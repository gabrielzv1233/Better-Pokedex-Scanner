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
    val args: List<StyledLabelPart> = emptyList(),
    val colors: List<String> = emptyList()
)

object StyledLabelSupport {
    fun buildComponent(definition: StyledLabelDefinition?): Component? {
        if (definition == null) return null

        return when {
            !definition.translationKey.isNullOrBlank() -> {
                if (definition.args.isEmpty() && definition.colors.isNotEmpty()) {
                    return buildStripedTranslatedComponent(definition.translationKey, definition.colors)
                }

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

    private fun buildStripedTranslatedComponent(translationKey: String, colors: List<String>): Component {
        val translated = Component.translatable(translationKey).string
        val parsedColours = colors.mapNotNull(::parseColour)
        if (translated.isBlank() || parsedColours.isEmpty()) {
            return Component.translatable(translationKey).withStyle { style -> style.withBold(true) }
        }

        val codePoints = translated.codePoints().toArray()
        val chunkCount = minOf(parsedColours.size, codePoints.size)
        val component = Component.empty()

        for (index in 0 until chunkCount) {
            val start = (index * codePoints.size) / chunkCount
            val end = ((index + 1) * codePoints.size) / chunkCount
            val text = String(codePoints, start, end - start)
            component.append(
                Component.literal(text).withStyle { style ->
                    style.withBold(true).withColor(parsedColours[index])
                }
            )
        }

        return component
    }

    private fun parseColour(value: String?): Int? {
        val normalized = value?.trim()?.removePrefix("#")?.takeIf { it.length == 6 } ?: return null
        return normalized.toIntOrNull(16)
    }
}
