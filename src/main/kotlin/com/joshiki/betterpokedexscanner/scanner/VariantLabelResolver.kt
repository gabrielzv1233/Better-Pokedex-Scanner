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

    fun resolveAspectMatchesForSpecies(speciesId: String?, aspects: Iterable<String>): List<VariantLabelMatch> {
        val normalizedAspects = normalizeAspectStrings(aspects)
        val matches = mutableListOf<VariantLabelMatch>()

        buildSpindaAspectDisplays(speciesId, normalizedAspects).forEach { matches += it }

        val consumedAspects = consumedSpindaAspectKeys(speciesId, normalizedAspects)
        normalizedAspects
            .filter { it !in consumedAspects }
            .mapNotNull { VariantLabelRegistry.findAspectLabel(it, speciesId) }
            .forEach { matches += it }

        VariantLabelRegistry.findMissingAspectLabel(normalizedAspects, speciesId)?.let { matches += it }

        return matches.distinctBy { Triple(it.headerLabel, it.headerTranslationKey, it.labelTranslationKey ?: it.label) }
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
            val aspectStrings =
                normalizeAspectStrings(
                liveAspects ?: getPokemonAspects(scanData)
            )

            val speciesId = resolveSpeciesId(scanData)
            resolveAspectMatchesForSpecies(speciesId, aspectStrings)
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

    private fun buildSpindaAspectDisplays(speciesId: String?, aspects: List<String>): List<VariantLabelMatch> {
        if (speciesId != "cobblemon:spinda") return emptyList()

        val faceSpots = extractSpindaSpotValue(aspects, "face-spots-")
        val faceSpots2 = extractSpindaSpotValue(aspects, "face-spots2-")
        val leftEarSpots = extractSpindaSpotValue(aspects, "left-ear-spots-")
        val rightEarSpots = extractSpindaSpotValue(aspects, "right-ear-spots-")
        val patternParts = listOf(faceSpots, faceSpots2, leftEarSpots, rightEarSpots)

        val matches = mutableListOf<VariantLabelMatch>()
        if (patternParts.any { it != null }) {
            matches += VariantLabelMatch(
                headerLabel = "Spot Pattern",
                label = patternParts.joinToString("-") { it ?: "0" }
            )
        }

        val specialMark = when (extractSpindaSpotValue(aspects, "special-spots-")) {
            "1" -> "Heart"
            "2" -> "Broken Heart"
            "3" -> "Goatee"
            "4" -> "Star"
            else -> null
        }
        if (specialMark != null) {
            matches += VariantLabelMatch(
                headerLabel = "Special Mark",
                label = specialMark
            )
        }

        return matches
    }

    private fun consumedSpindaAspectKeys(speciesId: String?, aspects: List<String>): Set<String> {
        if (speciesId != "cobblemon:spinda") return emptySet()

        return aspects.filter {
            it.startsWith("face-spots-") ||
                it.startsWith("face-spots2-") ||
                it.startsWith("left-ear-spots-") ||
                it.startsWith("right-ear-spots-") ||
                it.startsWith("special-spots-")
        }.toSet()
    }

    private fun extractSpindaSpotValue(aspects: List<String>, prefix: String): String? {
        return aspects.firstOrNull { it.startsWith(prefix) }
            ?.removePrefix(prefix)
            ?.takeIf { it.isNotBlank() }
            ?.let { if (it == "none") null else it }
    }



}
