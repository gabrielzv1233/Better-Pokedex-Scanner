package com.joshiki.betterpokedexscanner.scanner

import net.minecraft.client.player.LocalPlayer

object PokemonBlockScanner {
    fun findTarget(user: LocalPlayer): RestorationTankScanData? {
        return OccupiedPokeBallScanner.findTarget(user)
            ?: RestorationTankScanner.findRestorationTankTarget(user)
    }
}
