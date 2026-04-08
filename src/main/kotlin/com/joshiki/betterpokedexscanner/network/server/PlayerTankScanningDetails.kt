package com.joshiki.betterpokedexscanner.network.server

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import java.util.UUID

object PlayerTankScanningDetails {
    val playerToBlockPos = mutableMapOf<UUID, BlockPos>()
    val playerToSpeciesId = mutableMapOf<UUID, ResourceLocation>()
    val playerToTick = mutableMapOf<UUID, Int>()

    fun clear(playerId: UUID) {
        playerToBlockPos.remove(playerId)
        playerToSpeciesId.remove(playerId)
        playerToTick.remove(playerId)
    }
}