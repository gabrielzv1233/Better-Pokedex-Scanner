package com.joshiki.betterpokedexscanner.scanner

data class VariantLabelGroup(
    val headerLabel: String? = null,
    val headerTranslationKey: String? = null,
    val species: List<String> = emptyList(),
    val baseFormLabel: String? = null,
    val baseFormTranslationKey: String? = null,
    val formLabels: Map<String, String> = emptyMap(),
    val formTranslationKeys: Map<String, String> = emptyMap(),
    val aspectLabels: Map<String, String> = emptyMap(),
    val aspectTranslationKeys: Map<String, String> = emptyMap(),
    val missingAllAspects: List<String> = emptyList(),
    val missingAspectLabel: String? = null,
    val missingAspectTranslationKey: String? = null
)

data class VariantLabelFile(
    val groups: List<VariantLabelGroup> = emptyList()
)

data class VariantLabelMatch(
    val headerLabel: String? = null,
    val headerTranslationKey: String? = null,
    val label: String,
    val labelTranslationKey: String? = null
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
        return findMappedLabel(rawFormName, speciesId, VariantLabelGroup::formLabels, VariantLabelGroup::formTranslationKeys)
    }

    fun findBaseFormLabel(speciesId: String? = null): VariantLabelMatch? {
        val normalizedSpecies = speciesId?.let(::normalizeSpeciesId)

        return groups.firstNotNullOfOrNull { group ->
            if (!groupMatchesSpecies(group, normalizedSpecies)) return@firstNotNullOfOrNull null
            buildLabelMatch(group, group.baseFormLabel, group.baseFormTranslationKey)
        }
    }

    fun findAspectLabel(rawAspect: String, speciesId: String? = null): VariantLabelMatch? {
        return findMappedLabel(rawAspect, speciesId, VariantLabelGroup::aspectLabels, VariantLabelGroup::aspectTranslationKeys)
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
            buildLabelMatch(group, group.missingAspectLabel, group.missingAspectTranslationKey)
        }
    }

    private fun findMappedLabelMatch(
        group: VariantLabelGroup,
        labels: Map<String, String>,
        translationKeys: Map<String, String>,
        normalizedKey: String
    ): VariantLabelMatch? {
        labels.entries
            .firstOrNull { normalizeKey(it.key) == normalizedKey }
            ?.let { return buildLabelMatch(group, label = it.value, translationKey = null) }

        val translationKey = translationKeys.entries
            .firstOrNull { normalizeKey(it.key) == normalizedKey }
            ?.value

        return buildLabelMatch(group, label = null, translationKey = translationKey)
    }

    private fun findMappedLabel(
        rawKey: String,
        speciesId: String?,
        labels: (VariantLabelGroup) -> Map<String, String>,
        translationKeys: (VariantLabelGroup) -> Map<String, String>
    ): VariantLabelMatch? {
        val normalizedKey = normalizeKey(rawKey)
        val normalizedSpecies = speciesId?.let(::normalizeSpeciesId)

        return groups.firstNotNullOfOrNull { group ->
            if (!groupMatchesSpecies(group, normalizedSpecies)) return@firstNotNullOfOrNull null
            findMappedLabelMatch(group, labels(group), translationKeys(group), normalizedKey)
        }
    }

    private fun buildLabelMatch(
        group: VariantLabelGroup,
        label: String?,
        translationKey: String?
    ): VariantLabelMatch? {
        return when {
            !label.isNullOrBlank() -> {
                VariantLabelMatch(
                    headerLabel = group.headerLabel,
                    headerTranslationKey = group.headerTranslationKey,
                    label = label,
                    labelTranslationKey = null
                )
            }

            !translationKey.isNullOrBlank() -> {
                VariantLabelMatch(
                    headerLabel = group.headerLabel,
                    headerTranslationKey = group.headerTranslationKey,
                    label = translationKey,
                    labelTranslationKey = translationKey
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
