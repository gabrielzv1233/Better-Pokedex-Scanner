package com.joshiki.betterpokedexscanner.scanner

import com.joshiki.betterpokedexscanner.client.RestorationTankScanState
import net.minecraft.resources.ResourceLocation

data class PokemonBlockScanSnapshot(
    val frameDisplays: List<List<VariantDisplay>>,
    val mirroredDisplayGroups: List<MirroredDisplayGroup>,
    val primaryRenderState: VariantRenderState,
    val primaryDisplayIndex: Int?
) {
    val populatedDisplayIndices: List<Int>
        get() =
            frameDisplays.indices.filter { index ->
                when {
                    primaryDisplayIndex == index -> primaryRenderState.primaryDisplays.isNotEmpty()
                    else -> frameDisplays[index].isNotEmpty()
                }
            }

    val frameCount: Int
        get() = populatedDisplayIndices.size

    val pageCounts: List<Int>
        get() {
            val slotPageCounts =
                populatedDisplayIndices.map { displayIndex ->
                    if (primaryDisplayIndex == displayIndex) {
                        primaryRenderState.primaryDisplays.size.takeIf { it > 0 } ?: 1
                    } else {
                        frameDisplays.getOrNull(displayIndex)?.size?.takeIf { it > 0 } ?: 1
                    }
                }

            val mirroredPageCounts =
                mirroredDisplayGroups.mapNotNull { group ->
                    group.displays.size.takeIf { it > 0 }
                }

            return slotPageCounts + mirroredPageCounts
        }
}

object PokemonBlockScanPresentation {
    fun build(typeVisible: Boolean): PokemonBlockScanSnapshot? {
        val speciesId = RestorationTankScanState.speciesId ?: return null
        val primaryRenderState = buildPrimaryRenderState()

        val levelDisplays = buildList {
            add(
                VariantDisplay(
                    label = RestorationTankScanState.level?.let { "Lv.$it" } ?: "Lv.--",
                    colour = LevelThreatColorResolver.resolveColour(
                        level = RestorationTankScanState.level,
                        containedInBall = RestorationTankScanState.scanKind == PokemonBlockScanKind.OCCUPIED_BALL
                    )
                )
            )

            if (RestorationTankScanState.scanKind == PokemonBlockScanKind.OCCUPIED_BALL) {
                addAll(RestorationTankScanState.extraMirroredDisplays)
            }
        }

        val speciesDisplays = listOf(
            VariantDisplay(
                headerLabel =
                    if (RestorationTankScanState.scanKind == PokemonBlockScanKind.OCCUPIED_BALL) {
                        "Contained Pokemon"
                    } else {
                        "Restoration Subject"
                    },
                label = resolveSpeciesDisplayName(speciesId),
                showCaughtIcon = typeVisible,
                genderIcon = RestorationTankScanState.gender,
                trailingGenderIcon = true
            )
        )

        val typeDisplays =
            if (typeVisible) {
                listOf(
                    VariantDisplay(
                        label = RestorationTankScanState.typeLabel ?: buildTypeLabel(speciesId),
                        labelComponent = RestorationTankScanState.typeLabelComponent
                    )
                )
            } else {
                emptyList()
            }

        val primaryDisplayIndex =
            when {
                primaryRenderState.primaryDisplays.isEmpty() -> null
                typeVisible -> 3
                else -> 2
            }

        val frameDisplays = listOf(
            levelDisplays,
            speciesDisplays,
            if (typeVisible) typeDisplays else emptyList(),
            emptyList()
        )

        val mirroredDisplayGroups =
            if (RestorationTankScanState.scanKind == PokemonBlockScanKind.OCCUPIED_BALL) {
                listOfNotNull(
                    RestorationTankScanState.containerName
                        ?.takeIf { it.isNotBlank() }
                        ?.let { ballName ->
                            MirroredDisplayGroup(
                                anchorDisplayIndex = 0,
                                displays = listOf(
                                    VariantDisplay(
                                        label = ballName
                                    )
                                )
                            )
                        }
                )
            } else {
                emptyList()
            }

        return PokemonBlockScanSnapshot(
            frameDisplays = frameDisplays,
            mirroredDisplayGroups = mirroredDisplayGroups,
            primaryRenderState = primaryRenderState,
            primaryDisplayIndex = primaryDisplayIndex
        )
    }

    private fun buildPrimaryRenderState(): VariantRenderState {
        val speciesId = RestorationTankScanState.speciesId?.toString()
        val displays = mutableListOf<VariantDisplay>()

        if (RestorationTankScanState.validSpecies) {
            val formName = RestorationTankScanState.formName
            if (!formName.isNullOrBlank() && !formName.equals("normal", ignoreCase = true)) {
                VariantLabelRegistry.findFormLabel(formName, speciesId)?.let { match ->
                    displays += VariantDisplay(
                        headerLabel = match.headerLabel,
                        headerTranslationKey = match.headerTranslationKey,
                        label = if (match.labelComponent != null) {
                            match.labelComponent.string
                        } else if (!match.labelTranslationKey.isNullOrBlank()) {
                            net.minecraft.client.resources.language.I18n.get(match.labelTranslationKey)
                        } else {
                            match.label
                        },
                        labelComponent = match.labelComponent,
                        labelTranslationKey = match.labelTranslationKey
                    )
                }
            }

            RestorationTankScanState.aspects
                .filter { !it.equals("shiny", ignoreCase = true) }
                .let { VariantLabelResolver.resolveAspectMatchesForSpecies(speciesId, it) }
                .forEach { match ->
                    displays += VariantDisplay(
                        headerLabel = match.headerLabel,
                        headerTranslationKey = match.headerTranslationKey,
                        label = if (match.labelComponent != null) {
                            match.labelComponent.string
                        } else if (!match.labelTranslationKey.isNullOrBlank()) {
                            net.minecraft.client.resources.language.I18n.get(match.labelTranslationKey)
                        } else {
                            match.label
                        },
                        labelComponent = match.labelComponent,
                        labelTranslationKey = match.labelTranslationKey
                    )
                }
        }

        val distinctDisplays = displays.distinctBy { Triple(it.headerLabel, it.headerTranslationKey, it.label) }

        if (RestorationTankScanState.scanKind == PokemonBlockScanKind.OCCUPIED_BALL) {
            return when {
                distinctDisplays.isNotEmpty() && RestorationTankScanState.shiny == true -> VariantRenderState(
                    primaryDisplays = distinctDisplays,
                    showSeparateShinyTag = true
                )
                distinctDisplays.isNotEmpty() -> VariantRenderState(distinctDisplays, false)
                RestorationTankScanState.shiny == true -> VariantRenderState(
                    primaryDisplays = listOf(
                        VariantDisplay(
                            headerLabel = "Variant",
                            label = "Shiny",
                            showShinyIcon = true
                        )
                    ),
                    showSeparateShinyTag = false
                )
                else -> VariantRenderState(emptyList(), false)
            }
        }

        val progressDisplays =
            if (RestorationTankScanState.timeRemaining == null) {
                emptyList()
            } else {
                val progress = getTankProgressPercent(RestorationTankScanState.timeRemaining!!)
                if (progress >= 100) {
                    listOf(
                        VariantDisplay(
                            headerLabel = "Restoration Progress",
                            label = "Complete",
                            colour = interpolateCyanToGreen(progress)
                        )
                    )
                } else {
                    listOf(
                        VariantDisplay(
                            headerLabel = "Development State",
                            label = if (progress < 50) "Embryonic" else "Fetal"
                        ),
                        VariantDisplay(
                            headerLabel = "Restoration Progress",
                            label = "$progress%",
                            colour = interpolateCyanToGreen(progress)
                        )
                    )
                }
            }

        return VariantRenderState(
            primaryDisplays = (distinctDisplays + progressDisplays).distinctBy {
                Triple(it.headerLabel, it.headerTranslationKey, it.label)
            },
            showSeparateShinyTag = RestorationTankScanState.shiny == true
        )
    }

    private fun resolveSpeciesDisplayName(speciesId: ResourceLocation): String {
        return try {
            val speciesClass = Class.forName("com.cobblemon.mod.common.api.pokemon.PokemonSpecies")
            val species = speciesClass.methods
                .firstOrNull { it.name == "getByIdentifier" && it.parameterCount == 1 }
                ?.invoke(null, speciesId)

            val translatedName = species?.javaClass?.methods
                ?.firstOrNull { it.name == "getTranslatedName" && it.parameterCount == 0 }
                ?.invoke(species) as? net.minecraft.network.chat.Component

            translatedName?.string ?: formatSpeciesIdName(speciesId)
        } catch (_: Exception) {
            formatSpeciesIdName(speciesId)
        }
    }

    private fun buildTypeLabel(speciesId: ResourceLocation): String {
        return try {
            val speciesClass = Class.forName("com.cobblemon.mod.common.api.pokemon.PokemonSpecies")
            val species = speciesClass.methods
                .firstOrNull { it.name == "getByIdentifier" && it.parameterCount == 1 }
                ?.invoke(null, speciesId) ?: return "Unknown Type"

            val standardForm = species.javaClass.methods
                .firstOrNull { it.name == "getStandardForm" && it.parameterCount == 0 }
                ?.invoke(species) ?: return "Unknown Type"

            val types = standardForm.javaClass.methods
                .firstOrNull { it.name == "getTypes" && it.parameterCount == 0 }
                ?.invoke(standardForm) as? Iterable<*> ?: return "Unknown Type"

            val labels = types.mapNotNull { typeObj ->
                (typeObj?.javaClass?.methods
                    ?.firstOrNull { it.name == "getDisplayName" && it.parameterCount == 0 }
                    ?.invoke(typeObj) as? net.minecraft.network.chat.Component)?.string
            }

            if (labels.isEmpty()) "Unknown Type" else "${labels.joinToString("/")} Type"
        } catch (_: Exception) {
            "Unknown Type"
        }
    }

    private fun formatSpeciesIdName(speciesId: ResourceLocation): String {
        return speciesId.path
            .split('_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun getTankProgressPercent(timeRemaining: Int): Int {
        val total = com.cobblemon.mod.common.block.multiblock.FossilMultiblockStructure.TIME_TO_TAKE
        return (((1f - (timeRemaining / total.toFloat())) * 100f).coerceIn(0f, 100f)).toInt()
    }

    private fun interpolateCyanToGreen(percent: Int): Int {
        val clamped = percent.coerceIn(0, 100)
        val red = 160 - (160 * clamped / 100)
        val green = 255
        val blue = 255 - (255 * clamped / 100)
        return (red shl 16) or (green shl 8) or blue
    }
}
