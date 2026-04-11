package com.joshiki.betterpokedexscanner.scanner

import net.minecraft.client.resources.language.I18n
import com.cobblemon.mod.common.pokemon.Gender
import net.minecraft.network.chat.Component

data class VariantDisplay(
    val headerLabel: String? = null,
    val headerTranslationKey: String? = null,
    val label: String,
    val labelComponent: Component? = null,
    val labelTranslationKey: String? = null,
    val showShinyIcon: Boolean = false,
    val showCaughtIcon: Boolean = false,
    val genderIcon: Gender? = null,
    val trailingGenderIcon: Boolean = false,
    val needsSeparateShinyTag: Boolean = false,
    val colour: Int? = null
)

data class VariantRenderState(
    val primaryDisplays: List<VariantDisplay>,
    val showSeparateShinyTag: Boolean = false
)

object VariantLabelResolver {

    fun resolveRenderState(scanData: Any, liveAspects: Iterable<String>? = null): VariantRenderState {
        val displays = mutableListOf<VariantDisplay>()



        resolveFormLabel(scanData)?.let { match ->
            displays += VariantDisplay(
                headerLabel = match.headerLabel,
                headerTranslationKey = match.headerTranslationKey,
                label = resolveDisplayLabel(match),
                labelComponent = match.labelComponent,
                labelTranslationKey = match.labelTranslationKey,
                showShinyIcon = false,
                needsSeparateShinyTag = false
            )
        }

        resolveAspectLabels(scanData, liveAspects).forEach { match ->
            displays += VariantDisplay(
                headerLabel = match.headerLabel,
                headerTranslationKey = match.headerTranslationKey,
                label = resolveDisplayLabel(match),
                labelComponent = match.labelComponent,
                labelTranslationKey = match.labelTranslationKey,
                showShinyIcon = false,
                needsSeparateShinyTag = false
            )
        }

        val distinctDisplays = displays.distinctBy {
            Triple(it.headerLabel, it.headerTranslationKey, it.label)
        }

        val shiny = isShiny(scanData)

        return when {
            distinctDisplays.isNotEmpty() && shiny -> VariantRenderState(
                primaryDisplays = distinctDisplays,
                showSeparateShinyTag = true
            )
            distinctDisplays.isNotEmpty() -> VariantRenderState(
                primaryDisplays = distinctDisplays,
                showSeparateShinyTag = false
            )
            shiny -> VariantRenderState(
                primaryDisplays = listOf(
                    VariantDisplay(
                        headerLabel = "Variant",
                        headerTranslationKey = null,
                        label = "Shiny",
                        labelTranslationKey = null,
                        showShinyIcon = true,
                        needsSeparateShinyTag = false
                    )
                ),
                showSeparateShinyTag = false
            )
            else -> VariantRenderState(
                primaryDisplays = emptyList(),
                showSeparateShinyTag = false
            )
        }
    }

    fun hasDisplay(scanData: Any, liveAspects: Iterable<String>? = null): Boolean {
        return resolveRenderState(scanData, liveAspects).primaryDisplays.isNotEmpty()
    }

    private fun resolveDisplayLabel(match: VariantLabelMatch): String {
        return when {
            match.labelComponent != null -> match.labelComponent.string
            !match.labelTranslationKey.isNullOrBlank() -> {
            I18n.get(match.labelTranslationKey)
            }
            else -> match.label
        }
    }

    private fun isShiny(scanData: Any): Boolean {
        return try {
            val pokemon = scanData.javaClass.methods
                .firstOrNull { it.name == "getPokemon" }
                ?.invoke(scanData) ?: return false

            val shinyMethod = pokemon.javaClass.methods.firstOrNull {
                it.name == "getShiny" || it.name == "isShiny"
            } ?: return false

            shinyMethod.invoke(pokemon) as? Boolean ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun resolveSpeciesId(scanData: Any): String? {
        return try {
            val apparentSpecies = scanData.javaClass
                .getMethod("getApparentSpecies")
                .invoke(scanData) ?: return null

            val resourceIdentifier = apparentSpecies.javaClass.methods
                .firstOrNull { it.name == "getResourceIdentifier" }
                ?.invoke(apparentSpecies)
                ?: apparentSpecies.javaClass.fields
                    .firstOrNull { it.name == "resourceIdentifier" }
                    ?.get(apparentSpecies)

            resourceIdentifier?.toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveFormLabel(scanData: Any): VariantLabelMatch? {
        return try {
            val clazz = scanData.javaClass
            val apparentSpecies = clazz.getMethod("getApparentSpecies").invoke(scanData) ?: return null
            val apparentForm = clazz.getMethod("getApparentForm").invoke(scanData) ?: return null

            val standardFormMethod = apparentSpecies.javaClass.methods.firstOrNull {
                it.name == "getStandardForm"
            } ?: return null

            val standardForm = standardFormMethod.invoke(apparentSpecies) ?: return null
            val speciesId = resolveSpeciesId(scanData)
            if (apparentForm == standardForm) return VariantLabelRegistry.findBaseFormLabel(speciesId)

            val nameMethod = apparentForm.javaClass.methods.firstOrNull {
                it.name == "getName"
            } ?: return null

            val rawName = nameMethod.invoke(apparentForm)?.toString()?.trim() ?: return null
            if (rawName.isBlank() || rawName.equals("Normal", ignoreCase = true)) return null

            VariantLabelRegistry.findFormLabel(rawName, speciesId)
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveAspectLabels(scanData: Any, liveAspects: Iterable<String>? = null): List<VariantLabelMatch> {
        return try {
            val aspectStrings = normalizeAspectStrings(
                liveAspects ?: getPokemonAspects(scanData)
            )

            val speciesId = resolveSpeciesId(scanData)

            val matches = aspectStrings
                .mapNotNull { VariantLabelRegistry.findAspectLabel(it, speciesId) }
                .toMutableList()

            VariantLabelRegistry.findMissingAspectLabel(aspectStrings, speciesId)?.let { matches += it }

            matches.distinctBy { Triple(it.headerLabel, it.headerTranslationKey, it.labelTranslationKey ?: it.label) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getPokemonAspects(scanData: Any): Iterable<*> {
        val pokemon = scanData.javaClass.methods
            .firstOrNull { it.name == "getPokemon" }
            ?.invoke(scanData) ?: return emptyList<String>()

        return pokemon.javaClass.methods
            .firstOrNull { it.name == "getAspects" }
            ?.invoke(pokemon) as? Iterable<*> ?: emptyList<String>()
    }

    private fun normalizeAspectStrings(aspects: Iterable<*>): List<String> {
        return aspects.mapNotNull {
            it?.toString()?.trim()?.lowercase()?.replace('_', '-')
        }
    }



}
