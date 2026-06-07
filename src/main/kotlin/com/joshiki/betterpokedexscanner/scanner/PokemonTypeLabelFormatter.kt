package com.joshiki.betterpokedexscanner.scanner

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

object PokemonTypeLabelFormatter {
    data class TypeToken(
        val displayName: Component,
        val colourKey: String
    )

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
        return buildTypeTokenComponent(typeLabels.map { TypeToken(Component.literal(it), it) })
    }

    fun buildTypeTokenComponent(typeTokens: List<TypeToken>): MutableComponent {
        if (typeTokens.isEmpty()) return buildUnknownTypeComponent()

        return Component.translatable("cobblemon.type.suffix", buildTypeTokenNamesComponent(typeTokens))
            .withStyle { style -> style.withBold(true) }
    }

    fun buildTypeComponentFromTypes(types: Iterable<*>): MutableComponent {
        return buildTypeTokenComponent(toTypeTokens(types))
    }

    fun buildTypeNamesComponent(typeLabels: List<String>): MutableComponent {
        return buildTypeTokenNamesComponent(typeLabels.map { TypeToken(Component.literal(it), it) })
    }

    fun buildTypeTokenNamesComponent(typeTokens: List<TypeToken>): MutableComponent {
        if (typeTokens.isEmpty()) return colouredToken(Component.translatable("betterpokedexscanner.label.unknown"), "unknown")

        val component = Component.empty()
        typeTokens.forEachIndexed { index, token ->
            if (index > 0) {
                component.append(defaultToken("/"))
            }
            component.append(colouredToken(token.displayName, token.colourKey))
        }
        return component
    }

    fun buildUnknownTypeComponent(): MutableComponent {
        return Component.translatable(
            "cobblemon.type.suffix",
            colouredToken(Component.translatable("betterpokedexscanner.label.unknown"), "unknown")
        ).withStyle { style -> style.withBold(true) }
    }

    private fun colouredToken(text: String, typeKey: String): MutableComponent {
        return colouredToken(Component.literal(text), typeKey)
    }

    private fun colouredToken(component: Component, typeKey: String): MutableComponent {
        val colour = typeColours[typeKey.trim().lowercase()]
        return component.copy().withStyle { style ->
            val base = style.withBold(true)
            if (colour != null) base.withColor(colour) else base
        }
    }

    private fun defaultToken(text: String): MutableComponent {
        return Component.literal(text).withStyle { style -> style.withBold(true) }
    }

    private fun toTypeTokens(types: Iterable<*>): List<TypeToken> {
        return types.mapNotNull { typeObj ->
            typeObj ?: return@mapNotNull null
            val displayName = typeObj.javaClass.methods
                .firstOrNull { it.name == "getDisplayName" && it.parameterCount == 0 }
                ?.invoke(typeObj) as? Component
                ?: Component.literal(resolveTypeKey(typeObj))

            TypeToken(displayName, resolveTypeKey(typeObj))
        }
    }

    private fun resolveTypeKey(typeObj: Any): String {
        val directName = typeObj.javaClass.methods
            .firstOrNull { it.name == "getName" && it.parameterCount == 0 }
            ?.invoke(typeObj)
            ?.toString()
            ?.takeIf { it.isNotBlank() }
        if (directName != null) return directName

        val identifier = typeObj.javaClass.methods
            .firstOrNull { it.name == "getIdentifier" && it.parameterCount == 0 }
            ?.invoke(typeObj)
            ?.toString()
            ?.substringAfterLast(':')
            ?.takeIf { it.isNotBlank() }
        if (identifier != null) return identifier

        return typeObj.toString().substringAfterLast('.').substringAfterLast(':')
    }
}
