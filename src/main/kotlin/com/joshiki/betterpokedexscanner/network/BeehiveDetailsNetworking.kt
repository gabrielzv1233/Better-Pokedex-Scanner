package com.joshiki.betterpokedexscanner.network

import com.joshiki.betterpokedexscanner.client.BeehiveScanState
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BeehiveBlockEntity
import java.lang.reflect.Field

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
    private fun resolveOccupants(blockEntity: BeehiveBlockEntity): List<Any> {
        val accessorNames = listOf("getBees", "getOccupants", "getStored")

        accessorNames.forEach { methodName ->
            runCatching {
                val method = blockEntity.javaClass.getDeclaredMethod(methodName)
                method.isAccessible = true
                (method.invoke(blockEntity) as? List<*>)?.filterNotNull()
            }.getOrNull()?.let { return it }
        }

        return blockEntity.javaClass.declaredFields
            .firstNotNullOfOrNull { field ->
                extractOccupantList(field, blockEntity)
            }
            ?: emptyList()
    }

    private fun extractOccupantList(field: Field, blockEntity: BeehiveBlockEntity): List<Any>? {
        return runCatching {
            if (!List::class.java.isAssignableFrom(field.type)) return@runCatching null
            field.isAccessible = true
            val value = field.get(blockEntity) as? List<*> ?: return@runCatching null
            value.filterNotNull().takeIf { occupants ->
                occupants.isNotEmpty() && occupants.first()::class.java.simpleName.contains("Bee", ignoreCase = true)
            }
        }.getOrNull()
    }

    private fun countOccupants(occupants: List<Any>): OccupantCounts {
        var beeCount = 0
        var combeeCount = 0
        var unknownCount = 0
        var femaleRareSpeciesId: String? = null
        var shinyRareSpeciesId: String? = null

        occupants.forEach { occupant ->
            val tag = extractOccupantEntityTag(occupant)
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

    private fun extractOccupantEntityTag(occupant: Any): CompoundTag? {
        resolveOccupantEntityDataObject(occupant)?.let { entityData ->
            extractEntityTag(entityData)?.let { return it }
        }

        return occupant.javaClass.declaredFields
            .firstNotNullOfOrNull { field ->
                extractEntityTagFromField(field, occupant)
            }
    }

    private fun resolveOccupantEntityDataObject(occupant: Any): Any? {
        val accessorNames = listOf("entityData", "getEntityData", "entityTag", "getEntityTag", "data", "getData")

        accessorNames.forEach { methodName ->
            runCatching {
                val method = occupant.javaClass.methods.firstOrNull { candidate ->
                    candidate.name == methodName && candidate.parameterCount == 0
                } ?: return@runCatching null
                method.invoke(occupant)
            }.getOrNull()?.let { return it }
        }

        return occupant.javaClass.methods
            .firstNotNullOfOrNull { method ->
                if (method.parameterCount != 0) return@firstNotNullOfOrNull null
                if (method.returnType == java.lang.Boolean.TYPE) return@firstNotNullOfOrNull null
                if (method.returnType == java.lang.Integer.TYPE) return@firstNotNullOfOrNull null
                if (method.returnType == java.lang.Void.TYPE) return@firstNotNullOfOrNull null
                if (method.declaringClass == Any::class.java || method.declaringClass == java.lang.Object::class.java) {
                    return@firstNotNullOfOrNull null
                }
                if (method.returnType == String::class.java) return@firstNotNullOfOrNull null
                if (method.returnType == Class::class.java) return@firstNotNullOfOrNull null
                runCatching {
                    method.invoke(occupant)
                }.getOrNull()
            }
            ?: occupant.javaClass.declaredFields.firstNotNullOfOrNull { field ->
                if (field.type == java.lang.Integer.TYPE) return@firstNotNullOfOrNull null
                if (field.type == String::class.java) return@firstNotNullOfOrNull null
                runCatching {
                    field.isAccessible = true
                    field.get(occupant)
                }.getOrNull()
            }
    }

    private fun extractEntityTagFromField(field: Field, target: Any): CompoundTag? {
        return runCatching {
            field.isAccessible = true
            extractEntityTag(field.get(target))
        }.getOrNull()
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
        return resolveStringValue(
            tag = tag,
            directKeys = listOf("species", "Species", "speciesId", "species_id"),
            nestedKeys = listOf("Pokemon", "pokemon", "Data", "data")
        ) ?: resolveNestedIdentifierValue(
            tag = tag,
            holderKeys = listOf("species", "Species", "speciesId", "species_id"),
            identifierKeys = listOf("resourceIdentifier", "identifier", "id", "value"),
            nestedKeys = listOf("Pokemon", "pokemon", "Data", "data")
        )
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
        return resolveStringValue(
            tag = tag,
            directKeys = listOf("gender", "Gender", "sex", "Sex"),
            nestedKeys = listOf("Pokemon", "pokemon", "Data", "data")
        )
    }

    private fun resolveStringValue(
        tag: CompoundTag,
        directKeys: List<String>,
        nestedKeys: List<String>
    ): String? {
        directKeys.forEach { key ->
            val value = tag.getString(key).takeIf { it.isNotBlank() }
            if (value != null) return value.lowercase()
        }

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

    private fun resolveNestedIdentifierValue(
        tag: CompoundTag,
        holderKeys: List<String>,
        identifierKeys: List<String>,
        nestedKeys: List<String>
    ): String? {
        holderKeys.forEach { key ->
            if (tag.contains(key, CompoundTag.TAG_COMPOUND.toInt())) {
                val nested = tag.getCompound(key)
                identifierKeys.forEach { identifierKey ->
                    val value = nested.getString(identifierKey).takeIf { it.isNotBlank() }
                    if (value != null) return value.lowercase()
                }
            }
        }

        nestedKeys.forEach { nestedKey ->
            if (!tag.contains(nestedKey, CompoundTag.TAG_COMPOUND.toInt())) return@forEach
            val nested = tag.getCompound(nestedKey)
            holderKeys.forEach { holderKey ->
                if (!nested.contains(holderKey, CompoundTag.TAG_COMPOUND.toInt())) return@forEach
                val holder = nested.getCompound(holderKey)
                identifierKeys.forEach { identifierKey ->
                    val value = holder.getString(identifierKey).takeIf { it.isNotBlank() }
                    if (value != null) return value.lowercase()
                }
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

    private fun extractEntityTag(entityData: Any?): CompoundTag? {
        return extractEntityTag(entityData, depth = 0)
    }

    private fun extractEntityTag(entityData: Any?, depth: Int): CompoundTag? {
        entityData ?: return null
        if (entityData is CompoundTag) return entityData
        if (depth >= 3) return null

        val preferredMethodNames = listOf(
            "copyTag",
            "copyNbt",
            "toTag",
            "toNbt",
            "getTag",
            "getNbt",
            "nbt",
            "copy",
            "copyData"
        )

        preferredMethodNames.forEach { methodName ->
            invokeCompoundTagMethod(entityData, methodName, depth)?.let { return it }
        }

        entityData.javaClass.methods
            .firstNotNullOfOrNull { method ->
                if (method.parameterCount != 0) return@firstNotNullOfOrNull null
                if (!CompoundTag::class.java.isAssignableFrom(method.returnType)) return@firstNotNullOfOrNull null
                runCatching {
                    method.invoke(entityData) as? CompoundTag
                }.getOrNull()
            }?.let { return it }

        val preferredFieldNames = listOf("tag", "nbt", "data", "customData")
        preferredFieldNames.forEach { fieldName ->
            val field = entityData.javaClass.declaredFields.firstOrNull { candidate ->
                candidate.name.equals(fieldName, ignoreCase = true)
            } ?: return@forEach
            extractEntityTagFromField(field, entityData)?.let { return it }
        }

        entityData.javaClass.methods
            .firstNotNullOfOrNull { method ->
                if (method.parameterCount != 0) return@firstNotNullOfOrNull null
                if (method.declaringClass == Any::class.java || method.declaringClass == java.lang.Object::class.java) {
                    return@firstNotNullOfOrNull null
                }
                if (method.returnType.isPrimitive) return@firstNotNullOfOrNull null
                if (method.returnType == String::class.java) return@firstNotNullOfOrNull null
                if (method.returnType == Class::class.java) return@firstNotNullOfOrNull null
                runCatching {
                    extractEntityTag(method.invoke(entityData), depth + 1)
                }.getOrNull()
            }?.let { return it }

        entityData.javaClass.declaredFields
            .firstNotNullOfOrNull { field ->
                if (java.lang.reflect.Modifier.isStatic(field.modifiers)) return@firstNotNullOfOrNull null
                if (field.type.isPrimitive) return@firstNotNullOfOrNull null
                if (field.type == String::class.java) return@firstNotNullOfOrNull null
                runCatching {
                    field.isAccessible = true
                    extractEntityTag(field.get(entityData), depth + 1)
                }.getOrNull()
            }?.let { return it }

        return null
    }

    private fun invokeCompoundTagMethod(target: Any, methodName: String, depth: Int): CompoundTag? {
        val method = target.javaClass.methods.firstOrNull { candidate ->
            candidate.name.equals(methodName, ignoreCase = true) &&
                candidate.parameterCount == 0
        } ?: return null

        return runCatching {
            extractEntityTag(method.invoke(target), depth + 1)
        }.getOrNull()
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
