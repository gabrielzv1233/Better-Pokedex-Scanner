package com.joshiki.betterpokedexscanner.network

import com.joshiki.betterpokedexscanner.client.BeehiveScanState
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.level.block.entity.BeehiveBlockEntity

object BeehiveDetailsNetworking {

    fun registerCommon() {
        PayloadTypeRegistry.playC2S().register(BeehiveDetailsRequestPayload.TYPE, BeehiveDetailsRequestPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(BeehiveDetailsResponsePayload.TYPE, BeehiveDetailsResponsePayload.CODEC)

        ServerPlayNetworking.registerGlobalReceiver(BeehiveDetailsRequestPayload.TYPE) { payload, context ->
            val player = context.player()
            val blockEntity = player.serverLevel().getBlockEntity(payload.blockPos) as? BeehiveBlockEntity ?: return@registerGlobalReceiver
            val occupants = resolveOccupants(blockEntity)
            val counts = countOccupants(occupants)

            ServerPlayNetworking.send(
                player,
                BeehiveDetailsResponsePayload(
                    blockPos = payload.blockPos,
                    occupantCount = blockEntity.occupantCount,
                    maxOccupants = BeehiveBlockEntity.MAX_OCCUPANTS,
                    beeCount = counts.beeCount,
                    combeeCount = counts.combeeCount,
                    unknownCount = counts.unknownCount,
                    femaleRareSpeciesId = counts.femaleRareSpeciesId,
                    shinyRareSpeciesId = counts.shinyRareSpeciesId,
                    sedated = blockEntity.isSedated,
                    hasSavedFlowerPos = hasSavedFlowerPos(blockEntity)
                )
            )
        }
    }

    fun registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(BeehiveDetailsResponsePayload.TYPE) { payload, context ->
            context.client().execute {
                BeehiveScanState.applyServerDetails(payload)
            }
        }
    }

    private data class OccupantCounts(
        val beeCount: Int,
        val combeeCount: Int,
        val unknownCount: Int,
        val femaleRareSpeciesId: String?,
        val shinyRareSpeciesId: String?
    )

    @Suppress("UNCHECKED_CAST")
    private fun resolveOccupants(blockEntity: BeehiveBlockEntity): List<BeehiveBlockEntity.Occupant> {
        return try {
            val method = blockEntity.javaClass.getDeclaredMethod("getBees")
            method.isAccessible = true
            method.invoke(blockEntity) as? List<BeehiveBlockEntity.Occupant> ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun countOccupants(occupants: List<BeehiveBlockEntity.Occupant>): OccupantCounts {
        var beeCount = 0
        var combeeCount = 0
        var unknownCount = 0
        var femaleRareSpeciesId: String? = null
        var shinyRareSpeciesId: String? = null

        occupants.forEach { occupant ->
            val tag = extractEntityTag(occupant.entityData())
            when (classifyOccupant(tag)) {
                "bee" -> beeCount += 1
                "combee" -> {
                    combeeCount += 1
                    if (tag != null) {
                        val speciesId = resolveSpeciesId(tag) ?: "combee"
                        if (femaleRareSpeciesId == null && isFemaleCombee(tag)) {
                            femaleRareSpeciesId = speciesId
                        }
                        if (shinyRareSpeciesId == null && isShinyCombee(tag)) {
                            shinyRareSpeciesId = speciesId
                        }
                    }
                }
                else -> unknownCount += 1
            }
        }

        return OccupantCounts(
            beeCount = beeCount,
            combeeCount = combeeCount,
            unknownCount = unknownCount,
            femaleRareSpeciesId = femaleRareSpeciesId,
            shinyRareSpeciesId = shinyRareSpeciesId
        )
    }

    private fun classifyOccupant(entityData: CustomData): String {
        return classifyOccupant(extractEntityTag(entityData))
    }

    private fun classifyOccupant(tag: CompoundTag?): String {
        tag ?: return "unknown"
        val entityId = tag.getString("id").lowercase()
        if (entityId == "minecraft:bee") {
            return "bee"
        }
        if (entityId == "cobblemon:combee") {
            return "combee"
        }

        val speciesId = resolveSpeciesId(tag)
        return when (speciesId) {
            "minecraft:bee", "bee" -> "bee"
            "cobblemon:combee", "combee" -> "combee"
            else -> "unknown"
        }
    }

    private fun resolveSpeciesId(tag: CompoundTag): String? {
        val directKeys = listOf("species", "Species", "speciesId", "species_id")
        directKeys.forEach { key ->
            val value = tag.getString(key).takeIf { it.isNotBlank() }
            if (value != null) return value.lowercase()
        }

        val nestedKeys = listOf("Pokemon", "pokemon", "Data", "data")
        nestedKeys.forEach { key ->
            if (!tag.contains(key, CompoundTag.TAG_COMPOUND.toInt())) return@forEach
            val nested = tag.getCompound(key)
            directKeys.forEach { nestedKey ->
                val value = nested.getString(nestedKey).takeIf { it.isNotBlank() }
                if (value != null) return value.lowercase()
            }
        }

        return null
    }

    private fun isFemaleCombee(tag: CompoundTag): Boolean {
        return resolveGenderValue(tag)?.contains("female") == true
    }

    private fun isShinyCombee(tag: CompoundTag): Boolean {
        return resolveBooleanValue(
            tag = tag,
            directKeys = listOf("shiny", "Shiny", "isShiny"),
            nestedKeys = listOf("Pokemon", "pokemon", "Data", "data")
        ) == true
    }

    private fun resolveGenderValue(tag: CompoundTag): String? {
        val directKeys = listOf("gender", "Gender", "sex", "Sex")
        directKeys.forEach { key ->
            val value = tag.getString(key).takeIf { it.isNotBlank() }
            if (value != null) return value.lowercase()
        }

        val nestedKeys = listOf("Pokemon", "pokemon", "Data", "data")
        nestedKeys.forEach { key ->
            if (!tag.contains(key, CompoundTag.TAG_COMPOUND.toInt())) return@forEach
            val nested = tag.getCompound(key)
            directKeys.forEach { nestedKey ->
                val value = nested.getString(nestedKey).takeIf { it.isNotBlank() }
                if (value != null) return value.lowercase()
            }
        }

        return null
    }

    private fun resolveBooleanValue(
        tag: CompoundTag,
        directKeys: List<String>,
        nestedKeys: List<String>
    ): Boolean? {
        directKeys.forEach { key ->
            if (tag.contains(key)) {
                return tag.getBoolean(key)
            }
        }

        nestedKeys.forEach { key ->
            if (!tag.contains(key, CompoundTag.TAG_COMPOUND.toInt())) return@forEach
            val nested = tag.getCompound(key)
            directKeys.forEach { nestedKey ->
                if (nested.contains(nestedKey)) {
                    return nested.getBoolean(nestedKey)
                }
            }
        }

        return null
    }

    private fun extractEntityTag(entityData: CustomData): CompoundTag? {
        return try {
            entityData.javaClass.methods
                .firstOrNull { method ->
                    method.parameterCount == 0 &&
                        CompoundTag::class.java.isAssignableFrom(method.returnType)
                }
                ?.invoke(entityData) as? CompoundTag
        } catch (_: Exception) {
            null
        }
    }

    private fun hasSavedFlowerPos(blockEntity: BeehiveBlockEntity): Boolean {
        return try {
            val method = blockEntity.javaClass.getDeclaredMethod("hasSavedFlowerPos")
            method.isAccessible = true
            method.invoke(blockEntity) as? Boolean ?: false
        } catch (_: Exception) {
            false
        }
    }
}
