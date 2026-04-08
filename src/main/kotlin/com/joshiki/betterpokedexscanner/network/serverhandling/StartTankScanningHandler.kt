package com.joshiki.betterpokedexscanner.network.serverhandling

import com.cobblemon.mod.common.block.entity.RestorationTankBlockEntity
import com.cobblemon.mod.common.block.multiblock.FossilMultiblockStructure
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
        val blockEntity = player.level().getBlockEntity(payload.blockPos) as? RestorationTankBlockEntity ?: return
        val struct = blockEntity.multiblockStructure as? FossilMultiblockStructure ?: return
        val fossil = struct.resultingFossil ?: return
        val resolvedPokemon = try {
            fossil.result.create()
        } catch (_: Exception) {
            null
        } ?: return

        val resolvedSpeciesId = resolvedPokemon.species.resourceIdentifier
        if (resolvedSpeciesId != payload.speciesId) return

        PlayerTankScanningDetails.playerToBlockPos[player.uuid] = payload.blockPos
        PlayerTankScanningDetails.playerToSpeciesId[player.uuid] = payload.speciesId
        PlayerTankScanningDetails.playerToTick[player.uuid] = server.tickCount
    }
}