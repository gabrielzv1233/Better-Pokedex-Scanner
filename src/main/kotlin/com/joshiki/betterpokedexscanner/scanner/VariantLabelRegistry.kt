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
        val normalized = normalizeKey(rawFormName)
        val normalizedSpecies = speciesId?.let(::normalizeSpeciesId)

        return groups.firstNotNullOfOrNull { group ->
            if (!groupMatchesSpecies(group, normalizedSpecies)) return@firstNotNullOfOrNull null

            group.formLabels.entries
                .firstOrNull { normalizeKey(it.key) == normalized }
                ?.let {
                    return@firstNotNullOfOrNull VariantLabelMatch(
                        headerLabel = group.headerLabel,
                        headerTranslationKey = group.headerTranslationKey,
                        label = it.value,
                        labelTranslationKey = null
                    )
                }

            group.formTranslationKeys.entries
                .firstOrNull { normalizeKey(it.key) == normalized }
                ?.let {
                    VariantLabelMatch(
                        headerLabel = group.headerLabel,
                        headerTranslationKey = group.headerTranslationKey,
                        label = it.value,
                        labelTranslationKey = it.value
                    )
                }
        }
    }

    fun findBaseFormLabel(speciesId: String? = null): VariantLabelMatch? {
        val normalizedSpecies = speciesId?.let(::normalizeSpeciesId)

        return groups.firstNotNullOfOrNull { group ->
            if (!groupMatchesSpecies(group, normalizedSpecies)) return@firstNotNullOfOrNull null

            when {
                !group.baseFormLabel.isNullOrBlank() -> {
                    VariantLabelMatch(
                        headerLabel = group.headerLabel,
                        headerTranslationKey = group.headerTranslationKey,
                        label = group.baseFormLabel,
                        labelTranslationKey = null
                    )
                }

                !group.baseFormTranslationKey.isNullOrBlank() -> {
                    VariantLabelMatch(
                        headerLabel = group.headerLabel,
                        headerTranslationKey = group.headerTranslationKey,
                        label = group.baseFormTranslationKey,
                        labelTranslationKey = group.baseFormTranslationKey
                    )
                }

                else -> null
            }
        }
    }

    fun findAspectLabel(rawAspect: String, speciesId: String? = null): VariantLabelMatch? {
        val normalized = normalizeKey(rawAspect)
        val normalizedSpecies = speciesId?.let(::normalizeSpeciesId)

        return groups.firstNotNullOfOrNull { group ->
            if (!groupMatchesSpecies(group, normalizedSpecies)) return@firstNotNullOfOrNull null

            group.aspectLabels.entries
                .firstOrNull { normalizeKey(it.key) == normalized }
                ?.let {
                    return@firstNotNullOfOrNull VariantLabelMatch(
                        headerLabel = group.headerLabel,
                        headerTranslationKey = group.headerTranslationKey,
                        label = it.value,
                        labelTranslationKey = null
                    )
                }

            group.aspectTranslationKeys.entries
                .firstOrNull { normalizeKey(it.key) == normalized }
                ?.let {
                    VariantLabelMatch(
                        headerLabel = group.headerLabel,
                        headerTranslationKey = group.headerTranslationKey,
                        label = it.value,
                        labelTranslationKey = it.value
                    )
                }
        }
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

            when {
                !group.missingAspectLabel.isNullOrBlank() -> {
                    VariantLabelMatch(
                        headerLabel = group.headerLabel,
                        headerTranslationKey = group.headerTranslationKey,
                        label = group.missingAspectLabel,
                        labelTranslationKey = null
                    )
                }

                !group.missingAspectTranslationKey.isNullOrBlank() -> {
                    VariantLabelMatch(
                        headerLabel = group.headerLabel,
                        headerTranslationKey = group.headerTranslationKey,
                        label = group.missingAspectTranslationKey,
                        labelTranslationKey = group.missingAspectTranslationKey
                    )
                }

                else -> null
            }
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