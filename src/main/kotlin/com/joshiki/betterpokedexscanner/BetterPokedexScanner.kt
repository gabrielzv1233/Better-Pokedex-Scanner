package com.joshiki.betterpokedexscanner

import com.joshiki.betterpokedexscanner.network.RestorationTankRegistrationNetworking
import com.joshiki.betterpokedexscanner.network.SlowpokeTailRegrowthNetworking
import net.fabricmc.api.ModInitializer

class BetterPokedexScanner : ModInitializer {
    override fun onInitialize() {
        SlowpokeTailRegrowthNetworking.registerCommon()
        RestorationTankRegistrationNetworking.registerCommon()
    }
}