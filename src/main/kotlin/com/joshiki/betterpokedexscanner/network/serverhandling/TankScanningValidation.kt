package com.joshiki.betterpokedexscanner.network.serverhandling

import com.cobblemon.mod.common.block.entity.RestorationTankBlockEntity
import com.cobblemon.mod.common.block.multiblock.FossilMultiblockStructure
import com.cobblemon.mod.common.pokemon.Pokemon
import com.joshiki.betterpokedexscanner.scanner.OccupiedPokeBallCompatibility
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

internal fun resolveScannedBlockPokemon(
    player: ServerPlayer,
    blockPos: BlockPos
): Pokemon? {
    val blockEntity = player.level().getBlockEntity(blockPos)
    val tankEntity = blockEntity as? RestorationTankBlockEntity
    if (tankEntity != null) {
        val structure = tankEntity.multiblockStructure as? FossilMultiblockStructure ?: return null
        val fossil = structure.resultingFossil ?: return null
        return try {
            fossil.result.create(player)
        } catch (_: Exception) {
            null
        }
    }

    return OccupiedPokeBallCompatibility.resolvePlacedBallPokemon(player.level(), blockPos)?.pokemon
}

internal fun resolveScannedBlockSpeciesId(
    player: ServerPlayer,
    blockPos: BlockPos
): ResourceLocation? {
    return resolveScannedBlockPokemon(player, blockPos)?.species?.resourceIdentifier
}
