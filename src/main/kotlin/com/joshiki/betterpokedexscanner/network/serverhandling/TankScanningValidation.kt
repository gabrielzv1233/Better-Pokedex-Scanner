package com.joshiki.betterpokedexscanner.network.serverhandling

import com.cobblemon.mod.common.block.entity.RestorationTankBlockEntity
import com.cobblemon.mod.common.block.multiblock.FossilMultiblockStructure
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

internal fun resolveTankSpeciesId(
    player: ServerPlayer,
    blockPos: BlockPos
): ResourceLocation? {
    val blockEntity = player.level().getBlockEntity(blockPos) as? RestorationTankBlockEntity ?: return null
    val structure = blockEntity.multiblockStructure as? FossilMultiblockStructure ?: return null
    val fossil = structure.resultingFossil ?: return null
    val resolvedPokemon = try {
        fossil.result.create()
    } catch (_: Exception) {
        null
    } ?: return null

    return resolvedPokemon.species.resourceIdentifier
}
