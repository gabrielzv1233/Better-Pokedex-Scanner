package com.joshiki.betterpokedexscanner.scanner

import net.minecraft.network.chat.Component

data class StyledLabelPart(
    val text: String? = null,
    val translationKey: String? = null,
    val color: String? = null
)

data class StyledLabelDefinition(
    val parts: List<StyledLabelPart> = emptyList(),
    val translationKey: String? = null,
    val args: List<StyledLabelPart> = emptyList()
)

object StyledLabelSupport {
    fun buildComponent(definition: StyledLabelDefinition?): Component? {
        if (definition == null) return null

        return when {
            !definition.translationKey.isNullOrBlank() -> {
                val args = definition.args.mapNotNull(::buildPartComponent).toTypedArray()
                Component.translatable(definition.translationKey, *args).withStyle { style -> style.withBold(true) }
            }

            definition.parts.isNotEmpty() -> {
                Component.empty().also { component ->
                    definition.parts.mapNotNull(::buildPartComponent).forEach(component::append)
                }
            }

            else -> null
        }
    }

    private fun buildPartComponent(part: StyledLabelPart): Component? {
        val baseText = when {
            !part.translationKey.isNullOrBlank() -> Component.translatable(part.translationKey)
            !part.text.isNullOrBlank() -> Component.literal(part.text)
            else -> null
        } ?: return null

        return baseText.withStyle { style ->
            val base = style.withBold(true)
            parseColour(part.color)?.let(base::withColor) ?: base
        }
    }

    private fun parseColour(value: String?): Int? {
        val normalized = value?.trim()?.removePrefix("#")?.takeIf { it.length == 6 } ?: return null
        return normalized.toIntOrNull(16)
    }
}
