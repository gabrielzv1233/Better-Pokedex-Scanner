package com.joshiki.betterpokedexscanner.scanner

import com.joshiki.betterpokedexscanner.network.BeehiveDetailsResponsePayload
import com.cobblemon.mod.common.pokemon.Gender
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.BeehiveBlock
import net.minecraft.world.level.block.entity.BeehiveBlockEntity
import net.minecraft.world.phys.BlockHitResult

data class BeehiveScanSnapshot(
    val blockPos: BlockPos,
    val honeyLevel: Int,
    val blockLabel: String,
    val occupantCount: Int? = null,
    val maxOccupants: Int? = null,
    val beeCount: Int? = null,
    val combeeCount: Int? = null,
    val unknownCount: Int? = null,
    val femaleRareSpeciesId: String? = null,
    val shinyRareSpeciesId: String? = null,
    val sedated: Boolean = false,
    val hasSavedFlowerPos: Boolean? = null
) {
    val frameDisplays: List<List<VariantDisplay>>
        get() = buildFrameDisplays()

    val rareIndividualDisplays: List<VariantDisplay>
        get() = buildRareIndividualDisplays()

    fun isInhabitantsFrame(index: Int): Boolean {
        val display = frameDisplays.getOrNull(index)?.firstOrNull() ?: return false
        return display.headerTranslationKey == INHABITANTS_HEADER_KEY ||
            display.headerLabel == "Inhabitants"
    }

    fun withServerDetails(payload: BeehiveDetailsResponsePayload): BeehiveScanSnapshot {
        return copy(
            occupantCount = payload.occupantCount,
            maxOccupants = payload.maxOccupants,
            beeCount = payload.beeCount,
            combeeCount = payload.combeeCount,
            unknownCount = payload.unknownCount,
            femaleRareSpeciesId = payload.femaleRareSpeciesId,
            shinyRareSpeciesId = payload.shinyRareSpeciesId,
            sedated = payload.sedated,
            hasSavedFlowerPos = payload.hasSavedFlowerPos
        )
    }

    private fun buildFrameDisplays(): List<List<VariantDisplay>> {
        val honeyPercent = ((honeyLevel / 5f) * 100f).toInt().coerceIn(0, 100)
        val honeyColour = when (honeyLevel) {
            5 -> 0xFFD35A
            in 3..4 -> 0xF6C453
            in 1..2 -> 0xD6A845
            else -> 0xB8B8B8
        }

        val occupancyLabel = when {
            occupantCount == null || maxOccupants == null -> "Reading..."
            occupantCount <= 0 -> "Empty"
            occupantCount >= maxOccupants -> "Full"
            else -> "$occupantCount / $maxOccupants"
        }
        val occupancyTranslationKey = when {
            occupantCount == null || maxOccupants == null -> "betterpokedexscanner.label.reading"
            occupantCount <= 0 -> "betterpokedexscanner.label.empty"
            occupantCount >= maxOccupants -> "betterpokedexscanner.label.full"
            else -> null
        }

        val occupancyColour = when {
            occupantCount == null || maxOccupants == null -> 0xD1D5DB
            occupantCount <= 0 -> 0x9CA3AF
            occupantCount >= maxOccupants -> 0x6EE7B7
            else -> 0xFFFFFF
        }

        val inhabitantsDisplays = buildList {
            if ((occupantCount ?: 0) > 0 && combeeCount != null && beeCount != null && unknownCount != null) {
                if (combeeCount > 0) {
                    add(
                        VariantDisplay(
                            headerTranslationKey = INHABITANTS_HEADER_KEY,
                            label = "Combee x$combeeCount",
                            labelComponent = Component.translatable(
                                "betterpokedexscanner.label.entity_count",
                                speciesNameComponent("cobblemon:combee"),
                                combeeCount
                            ).withStyle { style -> style.withBold(true) },
                            colour = 0xF7D774
                        )
                    )
                }

                if (beeCount > 0) {
                    add(
                        VariantDisplay(
                            headerTranslationKey = INHABITANTS_HEADER_KEY,
                            label = "Bee x$beeCount",
                            labelComponent = Component.translatable(
                                "betterpokedexscanner.label.entity_count",
                                Component.translatable("entity.minecraft.bee"),
                                beeCount
                            ).withStyle { style -> style.withBold(true) },
                            colour = 0xFFF4BF
                        )
                    )
                }

                if (unknownCount > 0) {
                    add(
                        VariantDisplay(
                            headerTranslationKey = INHABITANTS_HEADER_KEY,
                            label = "Unknown x$unknownCount",
                            labelComponent = Component.translatable(
                                "betterpokedexscanner.label.entity_count",
                                Component.translatable("betterpokedexscanner.label.unknown"),
                                unknownCount
                            ).withStyle { style -> style.withBold(true) },
                            colour = 0xD1D5DB
                        )
                    )
                }
            }
        }

        val statusDisplays = buildList {
            if (sedated) {
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.entity.condition",
                        label = "Sedated",
                        labelTranslationKey = "betterpokedexscanner.label.sedated",
                        colour = 0x9FE8B8
                    )
                )
            }

            add(
                VariantDisplay(
                    headerTranslationKey = "betterpokedexscanner.header.beehive.flowers",
                    label = when (hasSavedFlowerPos) {
                        true -> "Flowers Nearby"
                        false -> "No Flowers Nearby"
                        null -> "Reading..."
                    },
                    labelTranslationKey = when (hasSavedFlowerPos) {
                        true -> "betterpokedexscanner.label.flowers_nearby"
                        false -> "betterpokedexscanner.label.no_flowers_nearby"
                        null -> "betterpokedexscanner.label.reading"
                    },
                    colour = when (hasSavedFlowerPos) {
                        true -> 0xA7F3D0
                        false -> 0xD1D5DB
                        null -> 0xD1D5DB
                    }
                )
            )
        }

        return buildList {
            add(
                listOf(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.beehive.honey_level",
                        label = "$honeyPercent%",
                        colour = honeyColour
                    )
                )
            )
            add(
                listOf(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.beehive.occupancy",
                        label = occupancyLabel,
                        labelTranslationKey = occupancyTranslationKey,
                        colour = occupancyColour
                    )
                )
            )
            if (inhabitantsDisplays.isNotEmpty()) {
                add(inhabitantsDisplays)
            }
            add(
                statusDisplays.ifEmpty {
                    listOf(
                        VariantDisplay(
                            headerLabel = blockLabel,
                            label = "Stable",
                            labelTranslationKey = "betterpokedexscanner.label.stable",
                            colour = 0xFFFFFF
                        )
                    )
                }
            )
        }
    }

    private fun buildRareIndividualDisplays(): List<VariantDisplay> {
        if ((occupantCount ?: 0) <= 0) return emptyList()

        return buildList {
            shinyRareSpeciesId?.let { speciesId ->
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.beehive.rare_individual",
                        label = "Shiny ${formatSpeciesName(speciesId)}",
                        labelComponent = Component.translatable(
                            "betterpokedexscanner.label.shiny_species",
                            speciesNameComponent(speciesId)
                        ).withStyle { style -> style.withBold(true) },
                        showShinyIcon = true,
                        colour = 0xFFFFFF
                    )
                )
            }

            femaleRareSpeciesId?.let { speciesId ->
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.beehive.rare_individual",
                        label = "Female ${formatSpeciesName(speciesId)}",
                        labelComponent = Component.translatable(
                            "betterpokedexscanner.label.female_species",
                            speciesNameComponent(speciesId)
                        ).withStyle { style -> style.withBold(true) },
                        genderIcon = Gender.FEMALE,
                        colour = 0xFFFFFF
                    )
                )
            }
        }
    }

    private fun formatSpeciesName(speciesId: String): String {
        return speciesId
            .substringAfter(':')
            .split('_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun speciesNameComponent(speciesId: String): Component {
        val speciesPath = speciesId.substringAfter(':')
        return Component.translatable("cobblemon.species.$speciesPath.name")
    }

    private companion object {
        const val INHABITANTS_HEADER_KEY = "betterpokedexscanner.header.beehive.inhabitants"
    }
}

object BeehiveScanner {

    fun findBeehiveTarget(user: LocalPlayer): BeehiveScanSnapshot? {
        val hit = Minecraft.getInstance().hitResult as? BlockHitResult ?: return null
        val level = user.level()
        val pos = hit.blockPos
        val state = level.getBlockState(pos)
        val block = state.block as? BeehiveBlock ?: return null
        if (level.getBlockEntity(pos) !is BeehiveBlockEntity) return null

        val honeyLevel = state.getValue(BeehiveBlock.HONEY_LEVEL)

        return BeehiveScanSnapshot(
            blockPos = pos,
            honeyLevel = honeyLevel,
            blockLabel = if (block == net.minecraft.world.level.block.Blocks.BEE_NEST) "Bee Nest" else "Beehive"
        )
    }
}
