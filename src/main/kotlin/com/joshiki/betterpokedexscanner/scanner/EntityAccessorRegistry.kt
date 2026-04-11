package com.joshiki.betterpokedexscanner.scanner

import net.minecraft.network.chat.Component

data class EntityAccessorGroup(
    val accessorHeaderLabel: String? = null,
    val accessorHeaderTranslationKey: String? = null,
    val entities: List<String> = emptyList(),
    val accessor: String = "",
    val valueLabels: Map<String, String> = emptyMap(),
    val valueTranslationKeys: Map<String, String> = emptyMap(),
    val valueDisplays: Map<String, StyledLabelDefinition> = emptyMap()
)

data class EntityAccessorFile(
    val groups: List<EntityAccessorGroup> = emptyList()
)

data class EntityAccessorMatch(
    val headerLabel: String? = null,
    val headerTranslationKey: String? = null,
    val label: String,
    val labelTranslationKey: String? = null,
    val labelComponent: Component? = null
)

object EntityAccessorRegistry {
    private val groups = mutableListOf<EntityAccessorGroup>()

    fun clear() {
        groups.clear()
    }

    fun apply(file: EntityAccessorFile) {
        groups += file.groups.filter { it.accessor.isNotBlank() }
    }

    fun findGroup(entityTypeId: String): EntityAccessorGroup? {
        val normalizedEntityTypeId = normalizeEntityTypeId(entityTypeId)
        return groups.firstOrNull { group ->
            group.entities.any { normalizeEntityTypeId(it) == normalizedEntityTypeId }
        }
    }

    fun findValueLabel(group: EntityAccessorGroup, rawValue: String): EntityAccessorMatch? {
        val normalizedValue = normalizeKey(rawValue)

        group.valueDisplays.entries
            .firstOrNull { normalizeKey(it.key) == normalizedValue }
            ?.let {
                val labelComponent = StyledLabelSupport.buildComponent(it.value) ?: return@let
                return EntityAccessorMatch(
                    headerLabel = group.accessorHeaderLabel,
                    headerTranslationKey = group.accessorHeaderTranslationKey,
                    label = labelComponent.string,
                    labelComponent = labelComponent
                )
            }

        group.valueLabels.entries
            .firstOrNull { normalizeKey(it.key) == normalizedValue }
            ?.let {
                return EntityAccessorMatch(
                    headerLabel = group.accessorHeaderLabel,
                    headerTranslationKey = group.accessorHeaderTranslationKey,
                    label = it.value,
                    labelComponent = null
                )
            }

        val translationKey =
            group.valueTranslationKeys.entries
                .firstOrNull { normalizeKey(it.key) == normalizedValue }
                ?.value
                ?: return null

        return EntityAccessorMatch(
            headerLabel = group.accessorHeaderLabel,
            headerTranslationKey = group.accessorHeaderTranslationKey,
            label = translationKey,
            labelTranslationKey = translationKey,
            labelComponent = null
        )
    }

    private fun normalizeEntityTypeId(value: String): String {
        return value.trim().lowercase()
    }

    private fun normalizeKey(value: String): String {
        return value.trim().lowercase().replace('_', '-')
    }
}
