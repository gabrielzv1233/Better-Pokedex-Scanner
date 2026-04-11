package com.joshiki.betterpokedexscanner.scanner

import net.minecraft.network.chat.Component

data class VariantLabelGroup(
    val headerLabel: String? = null,
    val headerTranslationKey: String? = null,
    val species: List<String> = emptyList(),
    val baseFormLabel: String? = null,
    val baseFormTranslationKey: String? = null,
    val baseFormDisplay: StyledLabelDefinition? = null,
    val formLabels: Map<String, String> = emptyMap(),
    val formTranslationKeys: Map<String, String> = emptyMap(),
    val formDisplays: Map<String, StyledLabelDefinition> = emptyMap(),
    val aspectLabels: Map<String, String> = emptyMap(),
    val aspectTranslationKeys: Map<String, String> = emptyMap(),
    val aspectDisplays: Map<String, StyledLabelDefinition> = emptyMap(),
    val missingAllAspects: List<String> = emptyList(),
    val missingAspectLabel: String? = null,
    val missingAspectTranslationKey: String? = null,
    val missingAspectDisplay: StyledLabelDefinition? = null
)

data class VariantLabelFile(
    val groups: List<VariantLabelGroup> = emptyList()
)

data class VariantLabelMatch(
    val headerLabel: String? = null,
    val headerTranslationKey: String? = null,
    val label: String,
    val labelTranslationKey: String? = null,
    val labelComponent: Component? = null
)

object VariantLabelRegistry {
    private val groups = mutableListOf<VariantLabelGroup>()

    fun clear() {
        groups.clear()
    }

    fun apply(file: VariantLabelFile) {
        groups += file.groups
    }

    fun findFormLabel(rawFormName: String, speciesId: String? = null): VariantLabelMatch? {
        return findMappedLabel(
            rawFormName,
            speciesId,
            VariantLabelGroup::formLabels,
            VariantLabelGroup::formTranslationKeys,
            VariantLabelGroup::formDisplays
        )
    }

    fun findBaseFormLabel(speciesId: String? = null): VariantLabelMatch? {
        val normalizedSpecies = speciesId?.let(::normalizeSpeciesId)

        return groups.firstNotNullOfOrNull { group ->
            if (!groupMatchesSpecies(group, normalizedSpecies)) return@firstNotNullOfOrNull null
            buildLabelMatch(group, group.baseFormLabel, group.baseFormTranslationKey, group.baseFormDisplay)
        }
    }

    fun findAspectLabel(rawAspect: String, speciesId: String? = null): VariantLabelMatch? {
        return findMappedLabel(
            rawAspect,
            speciesId,
            VariantLabelGroup::aspectLabels,
            VariantLabelGroup::aspectTranslationKeys,
            VariantLabelGroup::aspectDisplays
        )
    }

    fun findMissingAspectLabel(
        currentAspects: Iterable<String>,
        speciesId: String? = null
    ): VariantLabelMatch? {
        val normalizedSpecies = speciesId?.let(::normalizeSpeciesId)
        val normalizedCurrentAspects = currentAspects.map(::normalizeKey).toSet()

        return groups.firstNotNullOfOrNull { group ->
            if (!groupMatchesSpecies(group, normalizedSpecies)) return@firstNotNullOfOrNull null
            if (group.missingAllAspects.isEmpty()) return@firstNotNullOfOrNull null

            val normalizedMissingTargets = group.missingAllAspects.map(::normalizeKey)
            val nonePresent = normalizedMissingTargets.none { it in normalizedCurrentAspects }
            if (!nonePresent) return@firstNotNullOfOrNull null
            buildLabelMatch(
                group,
                group.missingAspectLabel,
                group.missingAspectTranslationKey,
                group.missingAspectDisplay
            )
        }
    }

    private fun findMappedLabelMatch(
        group: VariantLabelGroup,
        labels: Map<String, String>,
        translationKeys: Map<String, String>,
        displays: Map<String, StyledLabelDefinition>,
        normalizedKey: String
    ): VariantLabelMatch? {
        displays.entries
            .firstOrNull { normalizeKey(it.key) == normalizedKey }
            ?.let { return buildLabelMatch(group, label = null, translationKey = null, display = it.value) }

        labels.entries
            .firstOrNull { normalizeKey(it.key) == normalizedKey }
            ?.let { return buildLabelMatch(group, label = it.value, translationKey = null, display = null) }

        val translationKey = translationKeys.entries
            .firstOrNull { normalizeKey(it.key) == normalizedKey }
            ?.value

        return buildLabelMatch(group, label = null, translationKey = translationKey, display = null)
    }

    private fun findMappedLabel(
        rawKey: String,
        speciesId: String?,
        labels: (VariantLabelGroup) -> Map<String, String>,
        translationKeys: (VariantLabelGroup) -> Map<String, String>,
        displays: (VariantLabelGroup) -> Map<String, StyledLabelDefinition>
    ): VariantLabelMatch? {
        val normalizedKey = normalizeKey(rawKey)
        val normalizedSpecies = speciesId?.let(::normalizeSpeciesId)

        return groups.firstNotNullOfOrNull { group ->
            if (!groupMatchesSpecies(group, normalizedSpecies)) return@firstNotNullOfOrNull null
            findMappedLabelMatch(group, labels(group), translationKeys(group), displays(group), normalizedKey)
        }
    }

    private fun buildLabelMatch(
        group: VariantLabelGroup,
        label: String?,
        translationKey: String?,
        display: StyledLabelDefinition?
    ): VariantLabelMatch? {
        val labelComponent = StyledLabelSupport.buildComponent(display)

        return when {
            labelComponent != null -> {
                VariantLabelMatch(
                    headerLabel = group.headerLabel,
                    headerTranslationKey = group.headerTranslationKey,
                    label = labelComponent.string,
                    labelTranslationKey = null,
                    labelComponent = labelComponent
                )
            }

            !label.isNullOrBlank() -> {
                VariantLabelMatch(
                    headerLabel = group.headerLabel,
                    headerTranslationKey = group.headerTranslationKey,
                    label = label,
                    labelTranslationKey = null,
                    labelComponent = null
                )
            }

            !translationKey.isNullOrBlank() -> {
                VariantLabelMatch(
                    headerLabel = group.headerLabel,
                    headerTranslationKey = group.headerTranslationKey,
                    label = translationKey,
                    labelTranslationKey = translationKey,
                    labelComponent = null
                )
            }

            else -> null
        }
    }

    private fun groupMatchesSpecies(group: VariantLabelGroup, normalizedSpecies: String?): Boolean {
        if (group.species.isEmpty()) return true
        if (normalizedSpecies == null) return false

        return group.species.any { normalizeSpeciesId(it) == normalizedSpecies }
    }

    private fun normalizeSpeciesId(value: String): String {
        return value.trim().lowercase()
    }

    private fun normalizeKey(value: String): String {
        return value.trim().lowercase().replace('_', '-')
    }
}
