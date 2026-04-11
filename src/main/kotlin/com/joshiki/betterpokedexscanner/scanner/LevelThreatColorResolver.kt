package com.joshiki.betterpokedexscanner.scanner

import com.cobblemon.mod.common.client.CobblemonClient

object LevelThreatColorResolver {
    private const val DEFAULT_COLOUR = 0xFFFFFF
    private const val LOW_LEVEL_COLOUR = 0x78D86B
    private const val HIGH_LEVEL_COLOUR = 0xEF7374
    private const val HIGH_LEVEL_CONTAINED_COLOUR = 0x5CA8FF
    private const val THRESHOLD = 10

    fun resolveColour(level: Int?, containedInBall: Boolean = false): Int? {
        val targetLevel = level ?: return null
        val partyLevels =
            CobblemonClient.storage.party.slots
                .mapNotNull { pokemon -> pokemon?.level }

        if (partyLevels.isEmpty()) return null

        val minPartyLevel = partyLevels.minOrNull() ?: return null
        val maxPartyLevel = partyLevels.maxOrNull() ?: return null

        return when {
            targetLevel > maxPartyLevel -> {
                val progress = ((targetLevel - maxPartyLevel).coerceAtMost(THRESHOLD)).toFloat() / THRESHOLD.toFloat()
                interpolateColour(
                    DEFAULT_COLOUR,
                    if (containedInBall) HIGH_LEVEL_CONTAINED_COLOUR else HIGH_LEVEL_COLOUR,
                    progress
                )
            }
            targetLevel < minPartyLevel -> {
                val progress = ((minPartyLevel - targetLevel).coerceAtMost(THRESHOLD)).toFloat() / THRESHOLD.toFloat()
                interpolateColour(DEFAULT_COLOUR, LOW_LEVEL_COLOUR, progress)
            }
            else -> null
        }
    }

    private fun interpolateColour(start: Int, end: Int, progress: Float): Int {
        val clamped = progress.coerceIn(0f, 1f)
        val startRed = (start shr 16) and 0xFF
        val startGreen = (start shr 8) and 0xFF
        val startBlue = start and 0xFF
        val endRed = (end shr 16) and 0xFF
        val endGreen = (end shr 8) and 0xFF
        val endBlue = end and 0xFF

        val red = (startRed + ((endRed - startRed) * clamped)).toInt()
        val green = (startGreen + ((endGreen - startGreen) * clamped)).toInt()
        val blue = (startBlue + ((endBlue - startBlue) * clamped)).toInt()

        return (red shl 16) or (green shl 8) or blue
    }
}
