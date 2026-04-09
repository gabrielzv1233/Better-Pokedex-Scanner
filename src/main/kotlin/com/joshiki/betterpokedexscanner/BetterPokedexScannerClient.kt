package com.joshiki.betterpokedexscanner

import com.joshiki.betterpokedexscanner.client.SlowpokeTailRegrowthClientState
import com.joshiki.betterpokedexscanner.network.BeehiveDetailsNetworking
import com.joshiki.betterpokedexscanner.network.EnvironmentalStructureNetworking
import com.joshiki.betterpokedexscanner.network.RestorationTankRegistrationNetworking
import com.joshiki.betterpokedexscanner.network.SlowpokeTailRegrowthResponsePayload
import com.joshiki.betterpokedexscanner.scanner.VariantLabelLoader
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

class BetterPokedexScannerClient : ClientModInitializer {
    override fun onInitializeClient() {
        VariantLabelLoader.register()
        RestorationTankRegistrationNetworking.registerClient()
        EnvironmentalStructureNetworking.registerClient()
        BeehiveDetailsNetworking.registerClient()

        ClientPlayNetworking.registerGlobalReceiver(SlowpokeTailRegrowthResponsePayload.TYPE) { payload, context ->
            context.client().execute {
                SlowpokeTailRegrowthClientState.set(payload.entityId, payload.percent)
            }
        }
    }
}
