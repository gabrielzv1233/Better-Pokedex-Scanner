package com.joshiki.betterpokedexscanner.network.serverhandling

import com.joshiki.betterpokedexscanner.network.StartTankScanningPayload
import com.joshiki.betterpokedexscanner.network.server.PlayerTankScanningDetails
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object StartTankScanningHandler {
    fun handle(
        payload: StartTankScanningPayload,
        server: MinecraftServer,
        player: ServerPlayer
    ) {
        val resolvedSpeciesId = resolveTankSpeciesId(player, payload.blockPos) ?: return
        if (resolvedSpeciesId != payload.speciesId) return

        PlayerTankScanningDetails.playerToBlockPos[player.uuid] = payload.blockPos
        PlayerTankScanningDetails.playerToSpeciesId[player.uuid] = payload.speciesId
        PlayerTankScanningDetails.playerToTick[player.uuid] = server.tickCount
    }
}
