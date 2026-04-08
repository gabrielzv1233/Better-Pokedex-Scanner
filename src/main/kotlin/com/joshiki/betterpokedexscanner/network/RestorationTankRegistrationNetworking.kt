package com.joshiki.betterpokedexscanner.network

import com.joshiki.betterpokedexscanner.network.serverhandling.FinishTankScanningHandler
import com.joshiki.betterpokedexscanner.network.serverhandling.StartTankScanningHandler
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import com.joshiki.betterpokedexscanner.client.RestorationTankRegistrationState
import com.joshiki.betterpokedexscanner.client.RestorationTankScanState
import com.cobblemon.mod.common.CobblemonSounds
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.Minecraft
import com.cobblemon.mod.common.api.pokedex.PokedexLearnedInformation

object RestorationTankRegistrationNetworking {

    fun registerCommon() {
        PayloadTypeRegistry.playC2S().register(StartTankScanningPayload.TYPE, StartTankScanningPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(FinishTankScanningPayload.TYPE, FinishTankScanningPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(TankScanConfirmedPayload.TYPE, TankScanConfirmedPayload.CODEC)

        ServerPlayNetworking.registerGlobalReceiver(StartTankScanningPayload.TYPE) { payload, context ->
            StartTankScanningHandler.handle(payload, context.server(), context.player())
        }

        ServerPlayNetworking.registerGlobalReceiver(FinishTankScanningPayload.TYPE) { payload, context ->
            FinishTankScanningHandler.handle(payload, context.server(), context.player())
        }
    }

    fun registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(TankScanConfirmedPayload.TYPE) { payload, context ->
            context.client().execute {
                RestorationTankRegistrationState.newInformation = payload.newInformation
                RestorationTankRegistrationState.registerCompleted = true
                RestorationTankRegistrationState.awaitingServerConfirm = false
                RestorationTankRegistrationState.scanProgress = 0F
                RestorationTankRegistrationState.displayRegisterInfoIntervals = 0F
                RestorationTankScanState.focusIntervals = 0F

                val soundEvent = if (payload.newInformation == PokedexLearnedInformation.SPECIES) {
                    CobblemonSounds.POKEDEX_SCAN_REGISTER_POKEMON
                } else {
                    CobblemonSounds.POKEDEX_SCAN_REGISTER_ASPECT
                }

                Minecraft.getInstance().soundManager.play(
                    SimpleSoundInstance.forUI(soundEvent, 1.0F)
                )
            }
        }
    }
}