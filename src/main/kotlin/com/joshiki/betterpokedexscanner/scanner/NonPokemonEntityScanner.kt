package com.joshiki.betterpokedexscanner.scanner

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokedex.scanner.PokemonScanner
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobCategory
import net.minecraft.world.entity.Saddleable
import net.minecraft.world.entity.animal.Cat
import net.minecraft.world.entity.animal.Fox
import net.minecraft.world.entity.animal.MushroomCow
import net.minecraft.world.entity.animal.Parrot
import net.minecraft.world.entity.animal.Pufferfish
import net.minecraft.world.entity.animal.Rabbit
import net.minecraft.world.entity.animal.Sheep
import net.minecraft.world.entity.animal.TropicalFish
import net.minecraft.world.entity.animal.Wolf
import net.minecraft.world.entity.animal.axolotl.Axolotl
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse
import net.minecraft.world.entity.animal.horse.AbstractHorse
import net.minecraft.world.entity.animal.horse.Horse
import net.minecraft.world.entity.animal.horse.Llama
import net.minecraft.world.entity.animal.Panda
import net.minecraft.world.entity.monster.Shulker
import net.minecraft.world.entity.monster.Phantom
import net.minecraft.world.entity.monster.Creeper
import net.minecraft.world.entity.monster.EnderMan
import net.minecraft.world.entity.monster.Slime
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.decoration.Painting
import net.minecraft.world.entity.boss.EnderDragonPart
import net.minecraft.world.entity.vehicle.Boat
import net.minecraft.world.phys.AABB

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
    val displays: List<VariantDisplay>,
    val anchorSlotIndex: Int? = null
)

object NonPokemonEntityScanner {
    private const val PAINTING_TARGET_TOLERANCE = 0.15

    fun findTarget(user: LocalPlayer, zoomLevel: Int): NonPokemonEntityScanSnapshot? {
        val preciselyTargetedPainting = findPreciselyTargetedPainting(user, zoomLevel)
        val detectedEntity =
            preciselyTargetedPainting
                ?: PokemonScanner.detectEntity(user, zoomLevel * 2)
                ?: return null
        if (detectedEntity is Painting && preciselyTargetedPainting == null) return null

        val entity = normalizeScanTarget(detectedEntity)
        if (entity is PokemonEntity || shouldIgnoreEntity(entity)) return null

        val frameDisplays = buildFrameDisplays(entity)

        return NonPokemonEntityScanSnapshot(
            entityId = entity.id,
            entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type).toString(),
            entityName = entity.displayName?.string ?: entity.type.description.string,
            frameDisplays = frameDisplays,
            mirroredDisplayGroups = buildMirroredDisplayGroups(entity, frameDisplays),
            useFixedSlotLayout = entity is ArmorStand || entity is Painting,
            fixedVisibleSlots = when (entity) {
                is ArmorStand -> resolveArmorStandVisibleSlots(entity)
                is Painting -> listOf(true, true, true, false)
                else -> emptyList()
            }
        )
    }

    private fun buildFrameDisplays(entity: Entity): List<List<VariantDisplay>> {
        if (entity is ArmorStand) {
            return buildArmorStandFrameDisplays(entity)
        }
        if (entity is Painting) {
            return buildPaintingFrameDisplays(entity)
        }

        val displays = mutableListOf<List<VariantDisplay>>()
        val isItemEntity = entity is ItemEntity
        val entityTypeName = resolveEntityTypeLabel(entity)
        val entityTypeComponent = resolveEntityTypeComponent(entity)
        val baseEntityTypeName = entity.type.description.string
        val entityDisplayNameComponent = entity.displayName ?: entityTypeComponent
        val entityDisplayName = entityDisplayNameComponent.string
        val villagerProfessionLabel = resolveVillagerProfessionLabel(entity)
        val hasDistinctName =
            entityDisplayName != baseEntityTypeName &&
                entityDisplayName != entityTypeName &&
                entityDisplayName != villagerProfessionLabel
        val detailDisplays = buildList {
            resolvePaintingTitleDisplay(entity)?.let(::add)

            if (hasDistinctName && entity !is Painting) {
                add(
                    VariantDisplay(
                        headerTranslationKey = if (isItemEntity) {
                            "betterpokedexscanner.header.item_name"
                        } else {
                            "betterpokedexscanner.header.entity.name"
                        },
                        label = entityDisplayName,
                        labelComponent = entityDisplayNameComponent
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
                        label = resolveCategoryLabel(entity),
                        labelTranslationKey = resolveCategoryTranslationKey(entity)
                    )
                )
            }

        val typeDisplay =
            listOf(
                VariantDisplay(
                    headerTranslationKey = "betterpokedexscanner.header.entity.type",
                    label = entityTypeName,
                    labelComponent = entityTypeComponent
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
        if (entity is Painting) {
            return listOfNotNull(
                resolvePaintingTitleDisplay(entity)?.let { titleDisplay ->
                    MirroredDisplayGroup(anchorDisplayIndex = 0, displays = listOf(titleDisplay))
                }
            )
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

        resolveEndermanCarriedBlockDisplay(entity)?.let { carriedBlockDisplay ->
            groups += MirroredDisplayGroup(
                anchorDisplayIndex = -1,
                displays = listOf(carriedBlockDisplay),
                anchorSlotIndex = 3
            )
        }

        resolvePufferfishInflationDisplay(entity)?.let { inflationDisplay ->
            groups += MirroredDisplayGroup(
                anchorDisplayIndex = -1,
                displays = listOf(inflationDisplay),
                anchorSlotIndex = 3
            )
        }

        return groups
    }

    private fun buildPaintingFrameDisplays(entity: Painting): List<List<VariantDisplay>> {
        val typeDisplays = listOf(
            VariantDisplay(
                headerTranslationKey = "betterpokedexscanner.header.entity.type",
                label = entity.type.description.string,
                labelComponent = entity.type.description.copy().withStyle { style -> style.withBold(true) }
            )
        )
        val artistDisplays = listOfNotNull(resolvePaintingArtistDisplay(entity))
        val classificationDisplays = listOf(
            VariantDisplay(
                headerTranslationKey = "betterpokedexscanner.header.entity.category",
                label = resolveCategoryLabel(entity),
                labelTranslationKey = resolveCategoryTranslationKey(entity)
            )
        )

        return listOf(typeDisplays, artistDisplays, classificationDisplays, emptyList())
    }

    private fun findPreciselyTargetedPainting(user: LocalPlayer, zoomLevel: Int): Painting? {
        val start = user.eyePosition
        val scanRange = (zoomLevel * 2).toDouble()
        val end = start.add(user.lookAngle.scale(scanRange))
        val searchBox = AABB(start, end).inflate(PAINTING_TARGET_TOLERANCE)

        return user.level()
            .getEntitiesOfClass(Painting::class.java, searchBox)
            .mapNotNull { painting ->
                painting.boundingBox.inflate(PAINTING_TARGET_TOLERANCE).clip(start, end).orElse(null)?.let { hit ->
                    painting to start.distanceToSqr(hit)
                }
            }
            .minByOrNull { (_, distance) -> distance }
            ?.first
    }

    private fun buildArmorStandFrameDisplays(entity: ArmorStand): List<List<VariantDisplay>> {
        val baseEntityTypeName = entity.type.description.string
        val entityDisplayNameComponent = entity.displayName ?: entity.type.description
        val entityDisplayName = entityDisplayNameComponent.string
        val typeDisplays = buildList {
            add(
                VariantDisplay(
                    headerTranslationKey = "betterpokedexscanner.header.entity.type",
                    label = baseEntityTypeName,
                    labelComponent = entity.type.description
                )
            )

            if (entityDisplayName != baseEntityTypeName) {
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.entity.name",
                        label = entityDisplayName,
                        labelComponent = entityDisplayNameComponent
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
            resolvePaintingArtistDisplay(entity)?.let(::add)

            if (entity is TropicalFish) {
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.base_color",
                        label = formatResourcePath(entity.baseColor.name),
                        labelComponent = MinecraftColourLabelFormatter.buildDyeComponent(entity.baseColor)
                    )
                )
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.pattern",
                        label = entity.variant.displayName().string,
                        labelComponent = entity.variant.displayName()
                    )
                )
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.pattern_color",
                        label = formatResourcePath(entity.patternColor.name),
                        labelComponent = MinecraftColourLabelFormatter.buildDyeComponent(entity.patternColor)
                    )
                )
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.size",
                        label = formatResourcePath(entity.variant.base().name),
                        labelTranslationKey = resolveTropicalFishBaseTranslationKey(entity.variant.base().name)
                    )
                )
            } else {
                addAll(resolvePrimaryVariantDisplays(entity))
                addAll(resolveCreeperStatusDisplays(entity))
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
            addAll(resolveHostileEquipmentDisplays(entity))
            addAll(resolvePassengerDisplays(entity))
        }
    }

    private fun resolveEndermanCarriedBlockDisplay(entity: Entity): VariantDisplay? {
        val enderman = entity as? EnderMan ?: return null
        val blockName = enderman.carriedBlock?.block?.name ?: return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.carried_block",
            label = blockName.string,
            labelComponent = blockName.copy().withStyle { style -> style.withBold(true) }
        )
    }

    private fun resolvePufferfishInflationDisplay(entity: Entity): VariantDisplay? {
        val pufferfish = entity as? Pufferfish ?: return null
        val (label, translationKey) = when (pufferfish.puffState) {
            0 -> "Deflated" to "betterpokedexscanner.label.inflation.deflated"
            1 -> "Inflated" to "betterpokedexscanner.label.inflation.inflated"
            else -> "Fully Inflated" to "betterpokedexscanner.label.inflation.fully_inflated"
        }

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.inflation_state",
            label = label,
            labelTranslationKey = translationKey
        )
    }

    private fun resolvePrimaryVariantDisplays(entity: Entity): List<VariantDisplay> {
        resolveCustomAccessorDisplay(entity)?.let { customDisplay ->
            return listOf(customDisplay)
        }

        if (entity is Horse) {
            return buildList {
                formatVariantValue(entity.variant)?.let { colorLabel ->
                    val translationKey = resolveHorseVariantTranslationKey(colorLabel)
                    add(
                        VariantDisplay(
                            headerTranslationKey = "betterpokedexscanner.header.entity.variant",
                            label = colorLabel,
                            labelComponent = resolveBuiltInVariantComponent(entity, colorLabel, translationKey),
                            labelTranslationKey = translationKey
                        )
                    )
                }

                if (entity.markings.name != "NONE") {
                    add(
                        VariantDisplay(
                            headerTranslationKey = "betterpokedexscanner.header.pattern",
                            label = formatResourcePath(entity.markings.name),
                            labelComponent = resolveHorseMarkingsComponent(entity.markings.name)
                        )
                    )
                }
            }
        }

        resolveBuiltInVariantPresentation(entity)?.let { (variantLabel, variantComponent) ->
            return listOf(
                VariantDisplay(
                    headerTranslationKey = "betterpokedexscanner.header.entity.variant",
                    label = variantLabel,
                    labelComponent = variantComponent,
                    labelTranslationKey = resolveBuiltInVariantTranslationKey(entity, variantLabel)
                )
            )
        }
        return emptyList()
    }

    private fun resolveBuiltInVariantLabel(entity: Entity): String? {
        val variantValue =
            when (entity) {
                is Cat -> entity.variant
                is Fox -> entity.variant
                is MushroomCow -> entity.variant
                is Panda -> entity.variant
                is Parrot -> entity.variant
                is Rabbit -> entity.variant
                is Shulker -> entity.variant.orElse(null)
                is Wolf -> entity.variant
                is Axolotl -> entity.variant
                is Llama -> entity.variant
                else -> return null
            } ?: return null

        return formatVariantValue(variantValue)
    }

    private fun resolveBuiltInVariantPresentation(entity: Entity): Pair<String, Component?>? {
        val rawLabel = resolveBuiltInVariantLabel(entity) ?: return null
        val label = when {
            entity is Axolotl && rawLabel.equals("lucy", ignoreCase = true) -> "Leucistic"
            entity is Cat && rawLabel.equals("black", ignoreCase = true) -> "Tuxedo"
            entity is Cat && rawLabel.equals("all black", ignoreCase = true) -> "Black"
            else -> rawLabel
        }
        val translationKey = resolveBuiltInVariantTranslationKey(entity, label)

        if (entity is Cat) {
            val colours = when (label.lowercase()) {
                "tuxedo" -> listOf("black", "white")
                "calico" -> listOf("orange", "white", "dark_brown")
                "jellie" -> listOf("gray", "white")
                "red" -> listOf("dark_orange", "warm_orange", "white")
                "siamese" -> listOf("siamese_dark", "siamese_brown", "siamese_taupe", "siamese_cream")
                "tabby" -> listOf("tabby_dark", "tabby_mid", "tabby_light")
                else -> emptyList()
            }
            if (colours.isNotEmpty() && translationKey != null) {
                return label to MinecraftColourLabelFormatter.buildSplitComponent(
                    Component.translatable(translationKey),
                    colours
                )
            }
        }

        if (entity is Rabbit) {
            val colours = when (label.lowercase()) {
                "salt" -> listOf("rabbit_salt_light", "rabbit_salt_mid", "rabbit_salt_dark")
                "white splotched" -> listOf("black", "rabbit_splotch_mid", "white")
                else -> emptyList()
            }
            if (colours.isNotEmpty() && translationKey != null) {
                return label to MinecraftColourLabelFormatter.buildSplitComponent(
                    Component.translatable(translationKey),
                    colours
                )
            }
        }

        if (entity is Parrot) {
            val colours = when (label.lowercase()) {
                "red blue" -> listOf("red", "blue")
                "yellow blue" -> listOf("yellow", "blue")
                else -> emptyList()
            }
            if (colours.isNotEmpty()) {
                return label to buildColourListComponent(colours)
            }
        }

        return label to resolveBuiltInVariantComponent(entity, label, translationKey)
    }

    private fun buildColourListComponent(colours: List<String>): Component {
        return Component.empty().also { component ->
            colours.forEachIndexed { index, colour ->
                if (index > 0) component.append(Component.literal("/").withStyle { style -> style.withBold(true) })
                component.append(MinecraftColourLabelFormatter.buildComponent(
                    Component.translatable("color.minecraft.$colour"),
                    colour
                ))
            }
        }
    }

    private fun resolveBuiltInVariantComponent(
        entity: Entity,
        label: String,
        translationKey: String? = resolveBuiltInVariantTranslationKey(entity, label)
    ): Component? {
        val colourKey = when {
            entity is Axolotl -> when (label.lowercase()) {
                "leucistic" -> "pink"
                "wild", "brown" -> "chestnut"
                "gold" -> "gold"
                "cyan" -> "pale_blue"
                "blue" -> "soft_blue"
                else -> null
            }
            entity is Rabbit -> when (label.lowercase()) {
                "brown" -> "rabbit_brown"
                "gold" -> "rabbit_gold"
                "evil" -> "rabbit_evil"
                else -> null
            }
            entity is Cat -> when (label.lowercase()) {
                "black" -> "black"
                "british shorthair" -> "silver"
                "persian" -> "persian_cream"
                "ragdoll" -> "ragdoll_gray"
                "white" -> "white"
                else -> null
            }
            else -> when (label.lowercase()) {
                "white" -> "white"
                "creamy" -> "creamy"
                "chestnut" -> "chestnut"
                "brown" -> "brown"
                "black" -> "black"
                "gray" -> "gray"
                "dark brown" -> "dark_brown"
                "red" -> "red"
                "blue" -> "blue"
                "green" -> "green"
                "gold" -> "gold"
                else -> null
            }
        }

        return when {
            translationKey == null -> null
            colourKey != null -> MinecraftColourLabelFormatter.buildComponent(
                Component.translatable(translationKey),
                colourKey
            )
            else -> Component.translatable(translationKey).withStyle { style -> style.withBold(true) }
        }
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

    private fun resolveEntityTypeComponent(entity: Entity): MutableComponent {
        val typeComponent = entity.type.description.copy()
        return if (isEntityTamed(entity)) {
            Component.translatable("betterpokedexscanner.label.entity.tamed", typeComponent).withStyle { it.withBold(true) }
        } else {
            typeComponent.withStyle { it.withBold(true) }
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
        val professionPath = resolveVillagerProfessionPath(villager)
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
            label = label,
            labelComponent = buildVillagerProfessionComponent(professionPath, levelLabel)
        )
    }

    private fun resolveVillagerProfessionPath(villager: Villager): String {
        return BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.villagerData.profession).path
    }

    private fun resolveVillagerProfessionLabel(entity: Entity): String? {
        val villager = entity as? Villager ?: return null
        val professionPath = resolveVillagerProfessionPath(villager)
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

    private fun buildVillagerProfessionComponent(professionPath: String, levelLabel: String?): MutableComponent {
        val professionComponent = Component.translatable("entity.minecraft.villager.$professionPath")
        return if (levelLabel == null) {
            professionComponent.withStyle { it.withBold(true) }
        } else {
            Component.empty()
                .append(professionComponent)
                .append(Component.literal(" ("))
                .append(Component.translatable("merchant.level.${resolveVillagerLevelNumber(levelLabel)}"))
                .append(Component.literal(")"))
                .withStyle { it.withBold(true) }
        }
    }

    private fun resolveVillagerLevelNumber(levelLabel: String): Int {
        return when (levelLabel) {
            "Novice" -> 1
            "Apprentice" -> 2
            "Journeyman" -> 3
            "Expert" -> 4
            "Master" -> 5
            else -> 1
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
            label = formatResourcePath(dyeColor.name),
            labelComponent = MinecraftColourLabelFormatter.buildDyeComponent(dyeColor)
        )
    }

    private fun resolveWoolColorDisplay(entity: Entity): VariantDisplay? {
        val dyeColor = (entity as? Sheep)?.color ?: return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.wool_color",
            label = formatResourcePath(dyeColor.name),
            labelComponent = MinecraftColourLabelFormatter.buildDyeComponent(dyeColor)
        )
    }

    private fun resolveShellColorDisplay(entity: Entity): VariantDisplay? {
        val dyeColor = (entity as? Shulker)?.color ?: return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.shell_color",
            label = formatResourcePath(dyeColor.name),
            labelComponent = MinecraftColourLabelFormatter.buildDyeComponent(dyeColor)
        )
    }

    private fun resolveLifeStageDisplay(entity: Entity): VariantDisplay? {
        val ageable = entity as? AgeableMob ?: return null
        if (!ageable.isBaby) return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.life_stage",
            label = "Baby",
            labelTranslationKey = "betterpokedexscanner.label.baby"
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
            label = sizeLabel,
            labelTranslationKey = resolveSizeTranslationKey(sizeLabel)
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

    private fun resolveSizeTranslationKey(sizeLabel: String): String? {
        return when (sizeLabel) {
            "Small" -> "betterpokedexscanner.label.size.small"
            "Medium" -> "betterpokedexscanner.label.size.medium"
            "Semi-Large" -> "betterpokedexscanner.label.size.semi_large"
            "Large" -> "betterpokedexscanner.label.size.large"
            else -> null
        }
    }

    private fun resolveLlamaCarpetDisplay(entity: Entity): VariantDisplay? {
        val llama = entity as? Llama ?: return null
        if (!llama.isTamed) return null
        val dyeColor = llama.swag ?: return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.carpet_color",
            label = formatResourcePath(dyeColor.name),
            labelComponent = MinecraftColourLabelFormatter.buildDyeComponent(dyeColor)
        )
    }

    private fun resolveChestedDisplay(entity: Entity): VariantDisplay? {
        val chestedHorse = entity as? AbstractChestedHorse ?: return null
        if (!chestedHorse.hasChest()) return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.chest",
            label = "Attached",
            labelTranslationKey = "betterpokedexscanner.label.attached"
        )
    }

    private fun resolveSaddledDisplay(entity: Entity): VariantDisplay? {
        val saddleable = entity as? Saddleable ?: return null
        if (!saddleable.isSaddled) return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.entity.saddle",
            label = "Equipped",
            labelTranslationKey = "betterpokedexscanner.label.equipped"
        )
    }

    private fun resolveArmoredDisplay(entity: Entity): VariantDisplay? {
        val living = entity as? LivingEntity ?: return null
        if (entity is Llama) return null

        val bodyItem = living.getItemBySlot(EquipmentSlot.BODY)
        if (bodyItem.isEmpty) return null
        val itemPath = BuiltInRegistries.ITEM.getKey(bodyItem.item).path

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.entity.armor",
            label = bodyItem.hoverName.string,
            labelComponent = MinecraftColourLabelFormatter.buildVanillaMaterialComponent(bodyItem.hoverName, itemPath)
                ?: bodyItem.hoverName.copy().withStyle { it.withBold(true) }
        )
    }

    private fun resolveEquipmentDisplay(
        entity: LivingEntity,
        slot: EquipmentSlot,
        headerTranslationKey: String
    ): VariantDisplay? {
        val itemStack = entity.getItemBySlot(slot)
        if (itemStack.isEmpty) return null
        val itemPath = BuiltInRegistries.ITEM.getKey(itemStack.item).path

        return VariantDisplay(
            headerTranslationKey = headerTranslationKey,
            label = itemStack.hoverName.string,
            labelComponent = MinecraftColourLabelFormatter.buildVanillaMaterialComponent(itemStack.hoverName, itemPath)
                ?: itemStack.hoverName.copy().withStyle { it.withBold(true) }
        )
    }

    private fun resolveCustomAccessorDisplay(entity: Entity): VariantDisplay? {
        val entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.type).toString()
        val group = EntityAccessorRegistry.findGroup(entityTypeId) ?: return null
        val accessorValue = resolveAccessorValue(entity, group.accessor) ?: return null
        val rawValue = resolveAccessorRawValue(accessorValue) ?: return null
        val formattedValue = formatAccessorValue(accessorValue) ?: return null
        val match = EntityAccessorRegistry.findValueLabel(group, rawValue)
        val resolvedHeaderLabel = match?.headerLabel ?: group.accessorHeaderLabel
        val resolvedHeaderTranslationKey =
            match?.headerTranslationKey
                ?: group.accessorHeaderTranslationKey
                ?: if (resolvedHeaderLabel == null) "betterpokedexscanner.header.entity.variant" else null

        return VariantDisplay(
            headerLabel = resolvedHeaderLabel,
            headerTranslationKey = resolvedHeaderTranslationKey,
            label = match?.label ?: formattedValue,
            labelComponent = match?.labelComponent,
            labelTranslationKey = match?.labelTranslationKey
        )
    }

    private fun resolveAccessorValue(target: Any, accessorPath: String): Any? {
        var current: Any? = target

        for (segment in accessorPath.split('.').filter { it.isNotBlank() }) {
            current = current?.let { resolveAccessorSegment(it, segment) } ?: return null
        }

        return current
    }

    private fun resolveAccessorSegment(target: Any, segment: String): Any? {
        val normalizedSegment = segment.trim()
        val methods = target.javaClass.methods
        val candidateMethodNames =
            listOf(
                normalizedSegment,
                "get${normalizedSegment.replaceFirstChar { it.uppercase() }}",
                "is${normalizedSegment.replaceFirstChar { it.uppercase() }}"
            )

        methods.firstOrNull { it.parameterCount == 0 && it.name in candidateMethodNames }?.let { method ->
            return runCatching { method.invoke(target) }.getOrNull()
        }

        target.javaClass.fields.firstOrNull { it.name == normalizedSegment }?.let { field ->
            return runCatching { field.get(target) }.getOrNull()
        }

        return null
    }

    private fun resolveAccessorRawValue(value: Any): String? {
        return when (value) {
            is String -> value
            is Number, is Boolean -> value.toString()
            is Enum<*> -> {
                runCatching {
                    value.javaClass.methods.firstOrNull {
                        it.name == "getSerializedName" && it.parameterCount == 0
                    }?.invoke(value) as? String
                }.getOrNull() ?: value.name
            }

            is Holder<*> -> value.unwrapKey().orElse(null)?.location()?.path
            else -> {
                runCatching {
                    value.javaClass.methods.firstOrNull {
                        it.name == "getSerializedName" && it.parameterCount == 0
                    }?.invoke(value) as? String
                }.getOrNull() ?: value.toString()
            }
        }
    }

    private fun formatAccessorValue(value: Any): String? {
        return when (value) {
            is String -> formatResourcePath(value)
            is Number, is Boolean -> value.toString()
            else -> formatVariantValue(value)
        }
    }

    private fun resolvePassengerDisplays(entity: Entity): List<VariantDisplay> {
        return entity.passengers.map { passenger ->
            val typeName = passenger.type.description.string
            val displayNameComponent = passenger.displayName ?: passenger.type.description
            val displayName = displayNameComponent.string
            VariantDisplay(
                headerTranslationKey = "betterpokedexscanner.header.entity.passenger",
                label = if (displayName == typeName) typeName else displayName,
                labelComponent = displayNameComponent.copy().withStyle { it.withBold(true) }
            )
        }
    }

    private fun resolveCategoryLabel(entity: Entity): String {
        val categoryName = entity.type.category.serializedName
        return formatResourcePath(categoryName)
    }

    private fun resolveCategoryTranslationKey(entity: Entity): String {
        return "betterpokedexscanner.entity.category.${entity.type.category.serializedName.lowercase()}"
    }

    private fun resolveTropicalFishBaseTranslationKey(baseName: String): String? {
        return when (baseName.lowercase()) {
            "small" -> "betterpokedexscanner.label.size.small"
            "large" -> "betterpokedexscanner.label.size.large"
            else -> null
        }
    }

    private fun resolveHorseVariantTranslationKey(label: String): String? {
        val key = label.lowercase().replace(' ', '_')
        return when (key) {
            "white", "brown", "black", "gray" -> "color.minecraft.$key"
            "creamy", "chestnut", "dark_brown" -> "betterpokedexscanner.entity.variant.horse.$key"
            else -> null
        }
    }

    private fun resolveHorseMarkingsComponent(markingsName: String): Component? {
        return when (markingsName.lowercase()) {
            "white" -> MinecraftColourLabelFormatter.buildComponent(
                Component.translatable("color.minecraft.white"),
                "white"
            )
            "white_field" -> MinecraftColourLabelFormatter.buildComponent(
                Component.translatable("betterpokedexscanner.entity.horse.markings.white_field"),
                "white"
            )
            "white_dots" -> MinecraftColourLabelFormatter.buildComponent(
                Component.translatable("betterpokedexscanner.entity.horse.markings.white_spots"),
                "white"
            )
            "black_dots" -> MinecraftColourLabelFormatter.buildComponent(
                Component.translatable("betterpokedexscanner.entity.horse.markings.black_spots"),
                "black"
            )
            else -> null
        }
    }

    private fun resolvePaintingTitleDisplay(entity: Entity): VariantDisplay? {
        val painting = entity as? Painting ?: return null
        val variantId = painting.variant.unwrapKey().orElse(null)?.location() ?: return null
        val titleKey = "painting.${variantId.namespace}.${variantId.path}.title"
        val titleComponent =
            if (I18n.exists(titleKey)) Component.translatable(titleKey)
            else Component.literal(formatResourcePath(variantId.path))

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.painting_name",
            label = titleComponent.string,
            labelComponent = titleComponent.withStyle { style -> style.withBold(true) }
        )
    }

    private fun resolvePaintingArtistDisplay(entity: Entity): VariantDisplay? {
        val painting = entity as? Painting ?: return null
        val variantId = painting.variant.unwrapKey().orElse(null)?.location() ?: return null
        val authorKey = "painting.${variantId.namespace}.${variantId.path}.author"
        if (!I18n.exists(authorKey)) return null

        return VariantDisplay(
            headerTranslationKey = "betterpokedexscanner.header.artist",
            label = I18n.get(authorKey),
            labelComponent = Component.translatable(authorKey).withStyle { style -> style.withBold(true) }
        )
    }

    private fun resolveCreeperStatusDisplays(entity: Entity): List<VariantDisplay> {
        val creeper = entity as? Creeper ?: return emptyList()
        return buildList {
            if (creeper.isPowered) {
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.status",
                        label = "Charged",
                        labelTranslationKey = "betterpokedexscanner.label.charged",
                        colour = 0x7FE3E1
                    )
                )
            }
            if (creeper.isIgnited) {
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.status",
                        label = "Ignited",
                        labelTranslationKey = "betterpokedexscanner.label.ignited",
                        colour = 0xF9801D
                    )
                )
            }
        }
    }

    private fun resolveHostileEquipmentDisplays(entity: Entity): List<VariantDisplay> {
        val living = entity as? LivingEntity ?: return emptyList()
        if (entity.type.category != MobCategory.MONSTER) return emptyList()

        val slots = listOf(
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
        )
        return slots.mapNotNull { slot ->
            val headerKey = when (slot) {
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND -> "betterpokedexscanner.header.weapon"
                else -> "betterpokedexscanner.header.equipment"
            }
            resolveEquipmentDisplay(living, slot, headerKey)
        }
    }

    private fun resolveBuiltInVariantTranslationKey(entity: Entity, label: String): String? {
        val key = label.lowercase().replace(' ', '_')
        return when (entity) {
            is Cat -> when (key) {
                "black", "red", "white" -> "color.minecraft.$key"
                "tuxedo", "british_shorthair", "calico", "jellie", "persian", "ragdoll", "siamese", "tabby" ->
                    "betterpokedexscanner.entity.variant.cat.$key"
                else -> null
            }
            is Rabbit -> when (key) {
                "brown", "black", "white" -> "color.minecraft.$key"
                "evil" -> "entity.minecraft.killer_bunny"
                "gold" -> "betterpokedexscanner.variant.magikarp_jump_gold"
                "salt", "white_splotched" -> "betterpokedexscanner.entity.variant.rabbit.$key"
                else -> null
            }
            is Axolotl -> when (key) {
                "cyan", "blue", "brown" -> "color.minecraft.$key"
                "gold" -> "betterpokedexscanner.variant.magikarp_jump_gold"
                "leucistic", "wild" -> "betterpokedexscanner.entity.variant.axolotl.$key"
                else -> null
            }
            is Fox -> when (key) {
                "red" -> "color.minecraft.red"
                "snow" -> "block.minecraft.snow"
                else -> null
            }
            is MushroomCow -> when (key) {
                "red", "brown" -> "color.minecraft.$key"
                else -> null
            }
            is Panda -> when (key) {
                "brown" -> "color.minecraft.brown"
                "normal" -> "options.difficulty.normal"
                "lazy", "worried", "playful", "weak", "aggressive" ->
                    "betterpokedexscanner.entity.variant.panda.$key"
                else -> null
            }
            is Parrot -> when (key) {
                "blue", "green", "gray" -> "color.minecraft.$key"
                "red_blue", "yellow_blue" -> null
                else -> null
            }
            is Wolf -> when (key) {
                "black" -> "color.minecraft.black"
                "snowy" -> "betterpokedexscanner.variant.snowy_form"
                "chestnut" -> "betterpokedexscanner.entity.variant.horse.chestnut"
                "pale", "spotted", "ashen", "rusty", "woods", "striped" ->
                    "betterpokedexscanner.entity.variant.wolf.$key"
                else -> null
            }
            is Llama -> when (key) {
                "white", "brown", "gray" -> "color.minecraft.$key"
                "creamy" -> "betterpokedexscanner.entity.variant.horse.creamy"
                else -> null
            }
            is Shulker -> when (key) {
                "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray",
                "cyan", "purple", "blue", "brown", "green", "red", "black" -> "color.minecraft.$key"
                else -> null
            }
            else -> null
        }
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
