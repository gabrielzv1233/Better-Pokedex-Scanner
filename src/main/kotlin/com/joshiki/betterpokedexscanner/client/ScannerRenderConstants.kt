package com.joshiki.betterpokedexscanner.client

import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.resources.ResourceLocation

object ScannerRenderConstants {
    val SHINY_ICON: ResourceLocation = cobblemonResource("textures/gui/summary/icon_shiny.png")
    val ENVIRONMENT_SCAN_MARK: ResourceLocation =
        ResourceLocation.fromNamespaceAndPath(
            "better_pokedex_scanner",
            "textures/gui/pokedex/scan/scan_environment.png"
        )
    const val ENVIRONMENT_SCAN_MARK_WIDTH = 46
    const val ENVIRONMENT_SCAN_MARK_HEIGHT = 46
    const val SHINY_ICON_SIZE = 16
    const val SHINY_ICON_SCALE = 0.5F
}
