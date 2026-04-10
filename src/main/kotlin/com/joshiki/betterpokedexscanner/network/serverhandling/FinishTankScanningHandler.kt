package com.joshiki.betterpokedexscanner.network.serverhandling

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.block.entity.RestorationTankBlockEntity
import com.cobblemon.mod.common.block.multiblock.FossilMultiblockStructure
import com.cobblemon.mod.common.pokedex.scanner.PokedexEntityData
import com.cobblemon.mod.common.pokedex.scanner.PokedexUsageContext
import com.joshiki.betterpokedexscanner.network.FinishTankScanningPayload
import com.joshiki.betterpokedexscanner.network.TankScanConfirmedPayload
import com.joshiki.betterpokedexscanner.network.server.PlayerTankScanningDetails
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

object FinishTankScanningHandler {
    fun handle(
        payload: FinishTankScanningPayload,
        server: MinecraftServer,
        player: ServerPlayer
    ) {
        val storedPos = PlayerTankScanningDetails.playerToBlockPos[player.uuid] ?: return
        val storedSpecies = PlayerTankScanningDetails.playerToSpeciesId[player.uuid] ?: return
        val startTick = PlayerTankScanningDetails.playerToTick[player.uuid] ?: return

        if (storedPos != payload.blockPos || storedSpecies != payload.speciesId) return

        val ticksScan = server.tickCount - startTick
        if (ticksScan < PokedexUsageContext.SUCCESS_SCAN_SERVER_TICKS) return

        val resolvedSpeciesId = resolveTankSpeciesId(player, payload.blockPos) ?: return
        if (resolvedSpeciesId != payload.speciesId) return

        val blockEntity = player.level().getBlockEntity(payload.blockPos) as? RestorationTankBlockEntity ?: return
        val struct = blockEntity.multiblockStructure as? FossilMultiblockStructure ?: return
        val fossil = struct.resultingFossil ?: return


        val tempPokemon = fossil.result.create(player)
        val scanData = PokedexEntityData(tempPokemon, null)
        val dex = Cobblemon.playerDataManager.getPokedexData(player)
        val newInformation = dex.getNewInformation(scanData)
        dex.encounter(scanData)

        ServerPlayNetworking.send(
            player,
            TankScanConfirmedPayload(payload.speciesId, newInformation)
        )
        PlayerTankScanningDetails.clear(player.uuid)
    }
}
