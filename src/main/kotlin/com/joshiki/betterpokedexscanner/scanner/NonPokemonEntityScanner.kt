package com.joshiki.betterpokedexscanner.scanner

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokedex.scanner.PokemonScanner
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Saddleable
import net.minecraft.world.entity.animal.Cat
import net.minecraft.world.entity.animal.Sheep
import net.minecraft.world.entity.animal.TropicalFish
import net.minecraft.world.entity.animal.Wolf
import net.minecraft.world.entity.animal.axolotl.Axolotl
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse
import net.minecraft.world.entity.animal.horse.AbstractHorse
import net.minecraft.world.entity.animal.horse.Horse
import net.minecraft.world.entity.animal.horse.Llama
import net.minecraft.world.entity.monster.Shulker
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.entity.monster.Slime
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.boss.EnderDragonPart
import net.minecraft.world.entity.vehicle.Boat

data class NonPokemonEntityScanSnapshot(
    val entityId: Int,
    val entityTypeId: String,
    val entityName: String,
    val frameDisplays: List<List<VariantDisplay>>,
    val mirroredDisplayGroups: List<MirroredDisplayGroup> = emptyList(),
    val useFixedSlotLayout: Boolean = false,
    val fixedVisibleSlots: List<Boolean> = emptyList()
)

data class MirroredDisplayGroup(
    val anchorDisplayIndex: Int,
    val displays: List<VariantDisplay>
)

object NonPokemonEntityScanner {

    fun findTarget(user: LocalPlayer, zoomLevel: Int): NonPokemonEntityScanSnapshot? {
        val detectedEntity = PokemonScanner.detectEntity(user, zoomLevel * 2) ?: return null
        val entity = normalizeScanTarget(detectedEntity)
        if (entity is PokemonEntity || shouldIgnoreEntity(entity)) return null

        val frameDisplays = buildFrameDisplays(entity)

        return NonPokemonEntityScanSnapshot(
            entityId = entity.id,
            entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type).toString(),
            entityName = entity.displayName?.string ?: entity.type.description.string,
            frameDisplays = frameDisplays,
            mirroredDisplayGroups = buildMirroredDisplayGroups(entity, frameDisplays),
            useFixedSlotLayout = entity is ArmorStand,
            fixedVisibleSlots = if (entity is ArmorStand) resolveArmorStandVisibleSlots(entity) else emptyList()
        )
    }

    private fun buildFrameDisplays(entity: Entity): List<List<VariantDisplay>> {
        if (entity is ArmorStand) {
            return buildArmorStandFrameDisplays(entity)
        }

        val displays = mutableListOf<List<VariantDisplay>>()
        val isItemEntity = entity is ItemEntity
        val entityTypeName = resolveEntityTypeLabel(entity)
        val baseEntityTypeName = entity.type.description.string
        val entityDisplayName = entity.displayName?.string ?: entityTypeName
        val villagerProfessionLabel = resolveVillagerProfessionLabel(entity)
        val hasDistinctName =
            entityDisplayName != baseEntityTypeName &&
                entityDisplayName != entityTypeName &&
                entityDisplayName != villagerProfessionLabel
        val detailDisplays = buildList {
            if (hasDistinctName) {
                add(
                    VariantDisplay(
                        headerTranslationKey = if (isItemEntity) {
                            "betterpokedexscanner.header.item_name"
                        } else {
                            "betterpokedexscanner.header.entity.name"
                        },
                        label = entityDisplayName
                    )
                )
            }

            resolveCollarColorDisplay(entity)?.let(::add)
            resolveWoolColorDisplay(entity)?.let(::add)
            resolveShellColorDisplay(entity)?.let(::add)
        }

        resolveHealthDisplay(entity)?.let { displays += listOf(it) }

        val categoryDisplay =
            if (isItemEntity) {
                null
            } else {
                listOf(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.entity.category",
                        label = resolveCategoryLabel(entity)
                    )
                )
            }

        val typeDisplay =
            listOf(
                VariantDisplay(
                    headerTranslationKey = "betterpokedexscanner.header.entity.type",
                    label = entityTypeName
                )
            )

        displays += typeDisplay
        if (detailDisplays.isNotEmpty()) {
            displays += detailDisplays
        }
        categoryDisplay?.let { displays += it }

        return displays
    }

    private fun buildMirroredDisplayGroups(
        entity: Entity,
        frameDisplays: List<List<VariantDisplay>>
    ): List<MirroredDisplayGroup> {
        if (entity is ArmorStand) {
            return buildArmorStandMirroredDisplayGroups(entity)
        }

        val groups = mutableListOf<MirroredDisplayGroup>()
        val hasConditionDisplay =
            frameDisplays.firstOrNull()?.firstOrNull()?.headerTranslationKey ==
                "betterpokedexscanner.header.entity.condition"

        val typeMirroredDisplays = resolveTypeMirroredDisplays(entity)
        if (typeMirroredDisplays.isNotEmpty()) {
            groups += MirroredDisplayGroup(
                anchorDisplayIndex = if (hasConditionDisplay) 1 else 0,
                displays = typeMirroredDisplays
            )
        }

        val conditionMirroredDisplays = resolveConditionMirroredDisplays(entity)
        if (conditionMirroredDisplays.isNotEmpty() && hasConditionDisplay) {
            groups += MirroredDisplayGroup(
                anchorDisplayIndex = 0,
                displays = conditionMirroredDisplays
            )
        }

        return groups
    }

    private fun buildArmorStandFrameDisplays(entity: ArmorStand): List<List<VariantDisplay>> {
        val baseEntityTypeName = entity.type.description.string
        val entityDisplayName = entity.displayName?.string ?: baseEntityTypeName
        val typeDisplays = buildList {
            add(
                VariantDisplay(
                    headerTranslationKey = "betterpokedexscanner.header.entity.type",
                    label = baseEntityTypeName
                )
            )

            if (entityDisplayName != baseEntityTypeName) {
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.entity.name",
                        label = entityDisplayName
                    )
                )
            }
        }

        val slotTwoDisplay =
            resolveEquipmentDisplay(entity, EquipmentSlot.CHEST, "betterpokedexscanner.header.armor_stand.chest")
                ?: resolveEquipmentDisplay(entity, EquipmentSlot.MAINHAND, "betterpokedexscanner.header.armor_stand.main_hand")

        val slotThreeDisplay =
            resolveEquipmentDisplay(entity, EquipmentSlot.LEGS, "betterpokedexscanner.header.armor_stand.legs")
                ?: resolveEquipmentDisplay(entity, EquipmentSlot.OFFHAND, "betterpokedexscanner.header.armor_stand.off_hand")

        val slotFourDisplay =
            resolveEquipmentDisplay(entity, EquipmentSlot.FEET, "betterpokedexscanner.header.armor_stand.feet")

        return listOf(
            typeDisplays,
            listOfNotNull(slotTwoDisplay),
            listOfNotNull(slotThreeDisplay),
            listOfNotNull(slotFourDisplay)
        )
    }

    private fun buildArmorStandMirroredDisplayGroups(entity: ArmorStand): List<MirroredDisplayGroup> {
        val groups = mutableListOf<MirroredDisplayGroup>()

        resolveEquipmentDisplay(entity, EquipmentSlot.HEAD, "betterpokedexscanner.header.armor_stand.head")?.let {
            groups += MirroredDisplayGroup(anchorDisplayIndex = 0, displays = listOf(it))
        }

        if (!entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty) {
            resolveEquipmentDisplay(entity, EquipmentSlot.MAINHAND, "betterpokedexscanner.header.armor_stand.main_hand")?.let {
                groups += MirroredDisplayGroup(anchorDisplayIndex = 1, displays = listOf(it))
            }
        }

        if (!entity.getItemBySlot(EquipmentSlot.LEGS).isEmpty) {
            resolveEquipmentDisplay(entity, EquipmentSlot.OFFHAND, "betterpokedexscanner.header.armor_stand.off_hand")?.let {
                groups += MirroredDisplayGroup(anchorDisplayIndex = 2, displays = listOf(it))
            }
        }

        return groups
    }

    private fun resolveArmorStandVisibleSlots(entity: ArmorStand): List<Boolean> {
        return listOf(
            true,
            !entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty || !entity.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty,
            !entity.getItemBySlot(EquipmentSlot.LEGS).isEmpty || !entity.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty,
            !entity.getItemBySlot(EquipmentSlot.FEET).isEmpty
        )
    }

    private fun resolveTypeMirroredDisplays(entity: Entity): List<VariantDisplay> {
        return buildList {
            resolveVillagerProfessionDisplay(entity)?.let(::add)

            if (entity is TropicalFish) {
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.base_color",
                        label = formatResourcePath(entity.baseColor.name)
                    )
                )
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.pattern",
                        label = entity.variant.displayName().string
                    )
                )
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.pattern_color",
                        label = formatResourcePath(entity.patternColor.name)
                    )
                )
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.size",
                        label = formatResourcePath(entity.variant.base().name)
                    )
                )
            } else {
                addAll(resolvePrimaryVariantDisplays(entity))
                resolveLifeStageDisplay(entity)?.let(::add)
                resolveSizeDisplay(entity)?.let(::add)
                resolveLlamaCarpetDisplay(entity)?.let(::add)
                resolveChestedDisplay(entity)?.let(::add)
            }
        }
    }

    private fun resolveConditionMirroredDisplays(entity: Entity): List<VariantDisplay> {
        return buildList {
            resolveSaddledDisplay(entity)?.let(::add)
            resolveArmoredDisplay(entity)?.let(::add)
            addAll(resolvePassengerDisplays(entity))
        }
    }

    private fun resolvePrimaryVariantDisplays(entity: Entity): List<VariantDisplay> {
        if (entity is Horse) {
            return buildList {
                formatVariantValue(entity.variant)?.let { colorLabel ->
                    add(
                        VariantDisplay(
                            headerTranslationKey = "betterpokedexscanner.header.entity.variant",
                            label = colorLabel
                        )
                    )
                }

                if (entity.markings.name != "NONE") {
                    add(
                        VariantDisplay(
                            headerTranslationKey = "betterpokedexscanner.header.pattern",
                            label = formatResourcePath(entity.markings.name)
                        )
                    )
                }
            }
        }

        val variantLabel = resolveVariantLabel(entity) ?: return emptyList()
        return listOf(
            VariantDisplay(
                headerTranslationKey = "betterpokedexscanner.header.entity.variant",
                label = variantLabel
            )
        )
    }

    private fun resolveVariantLabel(entity: Entity): String? {
        val variantValue =
            when (entity) {
                is Cat -> entity.variant
                is Shulker -> entity.variant.orElse(null)
                is Wolf -> entity.variant
                is Axolotl -> entity.variant
                else -> {
                    val method = entity.javaClass.methods.firstOrNull {
                        it.name == "getVariant" && it.parameterCount == 0
                    } ?: return null
                    runCatching { method.invoke(entity) }.getOrNull()
                }
            } ?: return null

        return formatVariantValue(variantValue)
    }

    private fun shouldIgnoreEntity(entity: Entity): Boolean {
        return when (entity) {
            is Boat -> true
            is ArmorStand -> isArmorStandEmpty(entity)
            else -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.type).path.contains("boat")
        }
    }

    private fun normalizeScanTarget(entity: Entity): Entity {
        return when (entity) {
            is EnderDragonPart -> entity.parentMob
            else -> entity
        }
    }

    private fun isArmorStandEmpty(entity: ArmorStand): Boolean {
        return entity.getItemBySlot(EquipmentSlot.HEAD).isEmpty &&
            entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty &&
            entity.getItemBySlot(EquipmentSlot.LEGS).isEmpty &&
            entity.getItemBySlot(EquipmentSlot.FEET).isEmpty
    }

    private fun resolveEntityTypeLabel(entity: Entity): String {
        val typeLabel = entity.type.description.string
        return if (isEntityTamed(entity)) {
            "Tamed $typeLabel"
        } else {
            typeLabel
        }
    }

    private fun isEntityTamed(entity: Entity): Boolean {
        return when (entity) {
            is AbstractHorse -> entity.isTamed
            else -> {
                entity.javaClass.methods.firstOrNull {
                    (it.name == "isTame" || it.name == "isTamed") && it.parameterCount == 0
                }?.let { method ->
                    runCatching { method.invoke(entity) as? Boolean }.getOrNull() == true
                } ?: false
            }
        }
    }

    private fun resolveVillagerProfessionDisplay(entity: Entity): VariantDisplay? {
        val villager = entity as? Villager ?: return null
        val professionLabel = resolveVillagerProfessionLabel(villager) ?: return null
        val levelLabel = resolveVillagerLevelLabel(villager)
        val label =
            if (levelLabel == null) {
                professionLabel
            } else {
                "$professionLabel ($levelLabel)"
            }

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.entity.profession",
            label = label
        )
    }

    private fun resolveVillagerProfessionLabel(entity: Entity): String? {
        val villager = entity as? Villager ?: return null
        val professionPath = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.villagerData.profession)?.path ?: return null
        return when (professionPath) {
            "none" -> "Unemployed"
            else -> formatResourcePath(professionPath)
        }
    }

    private fun resolveVillagerLevelLabel(entity: Entity): String? {
        val villager = entity as? Villager ?: return null
        return when (villager.villagerData.level) {
            1 -> "Novice"
            2 -> "Apprentice"
            3 -> "Journeyman"
            4 -> "Expert"
            5 -> "Master"
            else -> null
        }
    }

    private fun formatVariantValue(value: Any): String? {
        return when (value) {
            is Enum<*> -> {
                val serialized = runCatching {
                    value.javaClass.methods.firstOrNull {
                        it.name == "getSerializedName" && it.parameterCount == 0
                    }?.invoke(value) as? String
                }.getOrNull()
                formatResourcePath(serialized ?: value.name.lowercase())
            }

            is Holder<*> -> {
                val resourcePath = value.unwrapKey().orElse(null)?.location()?.path ?: return null
                formatResourcePath(resourcePath)
            }

            else -> {
                val serialized = runCatching {
                    value.javaClass.methods.firstOrNull {
                        it.name == "getSerializedName" && it.parameterCount == 0
                    }?.invoke(value) as? String
                }.getOrNull()
                if (!serialized.isNullOrBlank()) {
                    formatResourcePath(serialized)
                } else {
                    null
                }
            }
        }
    }

    private fun resolveCollarColorDisplay(entity: Entity): VariantDisplay? {
        val dyeColor =
            when (entity) {
                is Cat -> entity.collarColor.takeIf { entity.isTame }
                is Wolf -> entity.collarColor.takeIf { entity.isTame }
                else -> null
            } ?: return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.entity.collar_color",
            label = formatResourcePath(dyeColor.name)
        )
    }

    private fun resolveWoolColorDisplay(entity: Entity): VariantDisplay? {
        val dyeColor = (entity as? Sheep)?.color ?: return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.wool_color",
            label = formatResourcePath(dyeColor.name)
        )
    }

    private fun resolveShellColorDisplay(entity: Entity): VariantDisplay? {
        val dyeColor = (entity as? Shulker)?.color ?: return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.shell_color",
            label = formatResourcePath(dyeColor.name)
        )
    }

    private fun resolveLifeStageDisplay(entity: Entity): VariantDisplay? {
        val ageable = entity as? AgeableMob ?: return null
        if (!ageable.isBaby) return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.life_stage",
            label = "Baby"
        )
    }

    private fun resolveSizeDisplay(entity: Entity): VariantDisplay? {
        val sizeLabel =
            when (entity) {
                is Slime -> resolveSlimeSizeLabel(entity.size)
                is Phantom -> resolvePhantomSizeLabel(entity.phantomSize)
                else -> null
            } ?: return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.size",
            label = sizeLabel
        )
    }

    private fun resolveSlimeSizeLabel(size: Int): String {
        return when (size) {
            0, 1 -> "Small"
            2 -> "Medium"
            3 -> "Semi-Large"
            4 -> "Large"
            else -> size.toString()
        }
    }

    private fun resolvePhantomSizeLabel(size: Int): String {
        return when (size) {
            0, 1 -> "Small"
            2 -> "Medium"
            3 -> "Semi-Large"
            4 -> "Large"
            else -> size.toString()
        }
    }

    private fun resolveLlamaCarpetDisplay(entity: Entity): VariantDisplay? {
        val llama = entity as? Llama ?: return null
        if (!llama.isTamed) return null
        val dyeColor = llama.swag ?: return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.carpet_color",
            label = formatResourcePath(dyeColor.name)
        )
    }

    private fun resolveChestedDisplay(entity: Entity): VariantDisplay? {
        val chestedHorse = entity as? AbstractChestedHorse ?: return null
        if (!chestedHorse.hasChest()) return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.chest",
            label = "Attached"
        )
    }

    private fun resolveSaddledDisplay(entity: Entity): VariantDisplay? {
        val saddleable = entity as? Saddleable ?: return null
        if (!saddleable.isSaddled) return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.entity.saddle",
            label = "Equipped"
        )
    }

    private fun resolveArmoredDisplay(entity: Entity): VariantDisplay? {
        val living = entity as? LivingEntity ?: return null
        if (entity is Llama) return null

        val bodyItem = living.getItemBySlot(EquipmentSlot.BODY)
        if (bodyItem.isEmpty) return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.entity.armor",
            label = bodyItem.hoverName.string
        )
    }

    private fun resolveEquipmentDisplay(
        entity: LivingEntity,
        slot: EquipmentSlot,
        headerTranslationKey: String
    ): VariantDisplay? {
        val itemStack = entity.getItemBySlot(slot)
        if (itemStack.isEmpty) return null

        return VariantDisplay(
            headerTranslationKey = headerTranslationKey,
            label = itemStack.hoverName.string
        )
    }

    private fun resolvePassengerDisplays(entity: Entity): List<VariantDisplay> {
        return entity.passengers.map { passenger ->
            val typeName = passenger.type.description.string
            val displayName = passenger.displayName?.string ?: typeName
            VariantDisplay(
                headerTranslationKey = "betterpokedexscanner.header.entity.passenger",
                label = if (displayName == typeName) typeName else displayName
            )
        }
    }

    private fun resolveCategoryLabel(entity: Entity): String {
        val categoryName = entity.type.category.serializedName
        return formatResourcePath(categoryName)
    }

    private fun formatResourcePath(path: String): String {
        return path
            .split('_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase().replaceFirstChar { it.titlecase() }
            }
    }

    private fun resolveHealthDisplay(entity: Entity): VariantDisplay? {
        val living = entity as? LivingEntity ?: return null
        val maxHealth = living.maxHealth.takeIf { it > 0F } ?: return null
        val currentHealth = living.health.coerceIn(0F, maxHealth)
        val percent = ((currentHealth / maxHealth) * 100F).toInt().coerceIn(0, 100)

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.entity.condition",
            label = "$percent%",
            colour = when {
                percent >= 70 -> 0xA7F3D0
                percent >= 35 -> 0xFDE68A
                else -> 0xFCA5A5
            }
        )
    }
}
