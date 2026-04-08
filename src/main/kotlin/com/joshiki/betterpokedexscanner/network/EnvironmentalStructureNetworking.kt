package com.joshiki.betterpokedexscanner.network

import com.joshiki.betterpokedexscanner.client.EnvironmentalScanState
import com.joshiki.betterpokedexscanner.scanner.EnvironmentalScanner
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.core.BlockPos

object EnvironmentalStructureNetworking {

    fun registerCommon() {
        PayloadTypeRegistry.playC2S().register(
            EnvironmentalStructureRequestPayload.TYPE,
            EnvironmentalStructureRequestPayload.CODEC
        )
        PayloadTypeRegistry.playS2C().register(
            EnvironmentalStructureResponsePayload.TYPE,
            EnvironmentalStructureResponsePayload.CODEC
        )

        ServerPlayNetworking.registerGlobalReceiver(EnvironmentalStructureRequestPayload.TYPE) { payload, context ->
            val player = context.player()
            val position = BlockPos.containing(player.x, player.y, player.z)
            val structureResult = EnvironmentalScanner.resolveNearbyStructureResult(player.serverLevel(), position)

            ServerPlayNetworking.send(
                player,
                EnvironmentalStructureResponsePayload(
                    sessionId = payload.sessionId,
                    structureName = structureResult?.structureName
                )
            )
        }
    }

    fun registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(EnvironmentalStructureResponsePayload.TYPE) { payload, context ->
            context.client().execute {
                EnvironmentalScanState.applyStructureResult(payload.sessionId, payload.structureName)
            }
        }
    }
}
