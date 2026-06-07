package com.joshiki.betterpokedexscanner.scanner

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.DyeColor

object MinecraftColourLabelFormatter {
    private val colourMap =
        mapOf(
            "white" to 0xF9FFFE,
            "orange" to 0xF9801D,
            "magenta" to 0xC74EBD,
            "light_blue" to 0x3AB3DA,
            "pale_blue" to 0x9FD8F4,
            "yellow" to 0xFED83D,
            "lime" to 0x80C71F,
            "pink" to 0xF38BAA,
            "gray" to 0x474F52,
            "light_gray" to 0x9D9D97,
            "cyan" to 0x169C9C,
            "purple" to 0x8932B8,
            "blue" to 0x3C44AA,
            "brown" to 0x835432,
            "green" to 0x5E7C16,
            "red" to 0xB02E26,
            "black" to 0x1D1D21,
            "grey" to 0x474F52,
            "light_grey" to 0x9D9D97,
            "silver" to 0xC0C0C0,
            "dark_brown" to 0x4B3123,
            "pale_brown" to 0xC8A27A,
            "creamy" to 0xE6D7A8,
            "chestnut" to 0x9A5A32,
            "sandy" to 0xD8B47A,
            "tan" to 0xC19A6B,
            "pale_yellow" to 0xF4E3A1,
            "pale_pink" to 0xF2C6D8,
            "soft_blue" to 0xACAFFB,
            "dark_orange" to 0xD87820,
            "warm_orange" to 0xF1B245,
            "rabbit_brown" to 0xAB8361,
            "rabbit_gold" to 0xFFEFC1,
            "rabbit_salt_light" to 0xB0A489,
            "rabbit_salt_mid" to 0xCBC5BF,
            "rabbit_salt_dark" to 0x534A3B,
            "rabbit_splotch_mid" to 0xE0D9D9,
            "rabbit_evil" to 0xD6C3C3,
            "ragdoll_gray" to 0xDAD6D2,
            "persian_cream" to 0xFCE7C9,
            "siamese_brown" to 0x59473E,
            "siamese_dark" to 0x332922,
            "siamese_taupe" to 0xA49893,
            "siamese_cream" to 0xF5E7D2,
            "tabby_dark" to 0x5D412C,
            "tabby_mid" to 0x937155,
            "tabby_light" to 0xB2AAA0,
            "gray_tabby_dark" to 0x443E39,
            "gray_tabby_mid" to 0x7F7973,
            "gray_tabby_light" to 0xAFA9A4,
            "iron" to 0xD8D8D8,
            "gold" to 0xF6D04D,
            "diamond" to 0x7FE3E1,
            "netherite" to 0x5B4B4F
        )
    private val vanillaItemMaterials =
        mapOf(
            "leather_horse_armor" to "brown",
            "iron_horse_armor" to "iron",
            "golden_horse_armor" to "gold",
            "diamond_horse_armor" to "diamond",
            "leather_helmet" to "brown",
            "leather_chestplate" to "brown",
            "leather_leggings" to "brown",
            "leather_boots" to "brown",
            "chainmail_helmet" to "light_gray",
            "chainmail_chestplate" to "light_gray",
            "chainmail_leggings" to "light_gray",
            "chainmail_boots" to "light_gray",
            "iron_helmet" to "iron",
            "iron_chestplate" to "iron",
            "iron_leggings" to "iron",
            "iron_boots" to "iron",
            "golden_helmet" to "gold",
            "golden_chestplate" to "gold",
            "golden_leggings" to "gold",
            "golden_boots" to "gold",
            "diamond_helmet" to "diamond",
            "diamond_chestplate" to "diamond",
            "diamond_leggings" to "diamond",
            "diamond_boots" to "diamond",
            "netherite_helmet" to "netherite",
            "netherite_chestplate" to "netherite",
            "netherite_leggings" to "netherite",
            "netherite_boots" to "netherite",
            "wooden_sword" to "brown",
            "wooden_axe" to "brown",
            "wooden_pickaxe" to "brown",
            "wooden_shovel" to "brown",
            "wooden_hoe" to "brown",
            "stone_sword" to "gray",
            "stone_axe" to "gray",
            "stone_pickaxe" to "gray",
            "stone_shovel" to "gray",
            "stone_hoe" to "gray",
            "iron_sword" to "iron",
            "iron_axe" to "iron",
            "iron_pickaxe" to "iron",
            "iron_shovel" to "iron",
            "iron_hoe" to "iron",
            "golden_sword" to "gold",
            "golden_axe" to "gold",
            "golden_pickaxe" to "gold",
            "golden_shovel" to "gold",
            "golden_hoe" to "gold",
            "diamond_sword" to "diamond",
            "diamond_axe" to "diamond",
            "diamond_pickaxe" to "diamond",
            "diamond_shovel" to "diamond",
            "diamond_hoe" to "diamond",
            "netherite_sword" to "netherite",
            "netherite_axe" to "netherite",
            "netherite_pickaxe" to "netherite",
            "netherite_shovel" to "netherite",
            "netherite_hoe" to "netherite",
            "bow" to "brown",
            "crossbow" to "brown",
            "trident" to "diamond",
            "shield" to "brown",
            "fishing_rod" to "brown",
            "flint_and_steel" to "light_gray",
            "shears" to "iron"
        )

    fun buildLiteralComponent(label: String, colourKey: String): MutableComponent {
        val colour = colourMap[normalizeKey(colourKey)]
        return Component.literal(label).withStyle { style ->
            val base = style.withBold(true)
            if (colour != null) base.withColor(colour) else base
        }
    }

    fun buildComponent(label: Component, colourKey: String): MutableComponent {
        val colour = colourMap[normalizeKey(colourKey)]
        return label.copy().withStyle { style ->
            val base = style.withBold(true)
            if (colour != null) base.withColor(colour) else base
        }
    }

    fun buildSplitComponent(label: Component, colourKeys: List<String>): MutableComponent {
        return buildSplitLabelComponent(label.string, colourKeys)
    }

    fun buildDyeComponent(dyeColor: DyeColor): MutableComponent {
        val label = formatName(dyeColor.name)
        val colour = colourMap[normalizeKey(dyeColor.name)]
        return Component.translatable("color.minecraft.${dyeColor.name.lowercase()}").withStyle { style ->
            val base = style.withBold(true)
            if (colour != null) base.withColor(colour) else base
        }
    }

    fun buildNamedComponent(colourKey: String): MutableComponent {
        return buildLiteralComponent(formatName(colourKey), colourKey)
    }

    fun buildVanillaMaterialComponent(label: String, itemPath: String): MutableComponent? {
        val colourKey = vanillaItemMaterials[normalizeKey(itemPath)] ?: return null
        val firstSpace = label.indexOf(' ')
        if (firstSpace <= 0 || firstSpace >= label.lastIndex) {
            return buildLiteralComponent(label, colourKey)
        }

        val materialLabel = label.substring(0, firstSpace)
        val itemLabel = label.substring(firstSpace)

        return Component.empty()
            .append(buildLiteralComponent(materialLabel, colourKey))
            .append(Component.literal(itemLabel).withStyle { style -> style.withBold(true) })
    }

    fun buildVanillaMaterialComponent(label: Component, itemPath: String): MutableComponent? {
        val colourKey = vanillaItemMaterials[normalizeKey(itemPath)] ?: return null
        return buildComponent(label, colourKey)
    }

    fun buildSplitLabelComponent(label: String, colourKeys: List<String>): MutableComponent {
        val normalizedKeys = colourKeys.filter { it.isNotBlank() }
        if (normalizedKeys.isEmpty()) return Component.literal(label).withStyle { style -> style.withBold(true) }

        val chunks = splitIntoChunks(label, normalizedKeys.size)
        return Component.empty().also { component ->
            chunks.forEachIndexed { index, chunk ->
                component.append(buildLiteralComponent(chunk, normalizedKeys[index]))
            }
        }
    }

    private fun formatName(path: String): String {
        return path
            .split('_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase().replaceFirstChar { it.titlecase() }
            }
    }

    private fun normalizeKey(value: String): String {
        return value.trim().lowercase().replace('-', '_')
    }

    private fun splitIntoChunks(text: String, chunkCount: Int): List<String> {
        val trimmed = text
        if (chunkCount <= 1 || trimmed.length <= 1) return listOf(trimmed)

        val words = trimmed.split(' ')
        if (words.size == chunkCount) {
            return words.mapIndexed { index, word ->
                if (index < words.lastIndex) "$word " else word
            }
        }

        val chunks = mutableListOf<String>()
        var cursor = 0
        repeat(chunkCount) { index ->
            val remainingChars = trimmed.length - cursor
            val remainingChunks = chunkCount - index
            val nextSize = kotlin.math.ceil(remainingChars / remainingChunks.toDouble()).toInt().coerceAtLeast(1)
            val end = (cursor + nextSize).coerceAtMost(trimmed.length)
            chunks += trimmed.substring(cursor, end)
            cursor = end
        }
        return chunks
    }
}
