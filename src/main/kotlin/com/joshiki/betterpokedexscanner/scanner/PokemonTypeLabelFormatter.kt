package com.joshiki.betterpokedexscanner.scanner

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

object PokemonTypeLabelFormatter {
    private val typeColours =
        mapOf(
            "bug" to 0x91A119,
            "dark" to 0x624D4E,
            "dragon" to 0x5060E1,
            "electric" to 0xFAC000,
            "fairy" to 0xEF70EF,
            "fighting" to 0xFF8000,
            "fire" to 0xE62829,
            "flying" to 0x81B9EF,
            "ghost" to 0x704170,
            "grass" to 0x3FA129,
            "ground" to 0x915121,
            "ice" to 0x3DCEF3,
            "normal" to 0x9FA19F,
            "poison" to 0x9141CB,
            "psychic" to 0xEF4179,
            "rock" to 0xAFA981,
            "steel" to 0x60A1B8,
            "stellar" to 0x40B5A5,
            "water" to 0x2980EF,
            "unknown" to 0x68A090
        )

    fun buildTypeComponent(typeLabels: List<String>): MutableComponent {
        if (typeLabels.isEmpty()) {
            return colouredToken("Unknown", "unknown")
                .append(defaultToken(" Type"))
        }

        val component = buildTypeNamesComponent(typeLabels)
        component.append(defaultToken(" Type"))
        return component
    }

    fun buildTypeNamesComponent(typeLabels: List<String>): MutableComponent {
        if (typeLabels.isEmpty()) {
            return colouredToken("Unknown", "unknown")
        }

        val component = Component.empty()
        typeLabels.forEachIndexed { index, label ->
            if (index > 0) {
                component.append(defaultToken("/"))
            }
            component.append(colouredToken(label, label))
        }
        return component
    }

    fun buildUnknownTypeComponent(): MutableComponent {
        return colouredToken("Unknown", "unknown")
            .append(defaultToken(" Type"))
    }

    private fun colouredToken(text: String, typeKey: String): MutableComponent {
        val colour = typeColours[typeKey.trim().lowercase()]
        return Component.literal(text).withStyle { style ->
            val base = style.withBold(true)
            if (colour != null) base.withColor(colour) else base
        }
    }

    private fun defaultToken(text: String): MutableComponent {
        return Component.literal(text).withStyle { style -> style.withBold(true) }
    }
}
