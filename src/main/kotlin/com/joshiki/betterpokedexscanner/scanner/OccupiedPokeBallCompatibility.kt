package com.joshiki.betterpokedexscanner.scanner

import com.cobblemon.mod.common.pokemon.Pokemon
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

data class OccupiedPokeBallPokemonData(
    val blockPos: BlockPos,
    val pokemon: Pokemon,
    val ballDisplayName: String
)

object OccupiedPokeBallCompatibility {
    private const val MOD_ID = "cobblemon_occupied_pokeballs"

    private val available by lazy {
        FabricLoader.getInstance().isModLoaded(MOD_ID)
    }

    private val placedBallBlockEntityClass by lazy {
        runCatching { Class.forName("com.pokecapsule.block.PlacedBallBlockEntity") }.getOrNull()
    }

    private val getBallStackMethod by lazy {
        placedBallBlockEntityClass?.methods?.firstOrNull { it.name == "getBallStack" && it.parameterCount == 0 }
    }

    private val ballNbtClass by lazy {
        runCatching { Class.forName("com.pokecapsule.util.BallNbt") }.getOrNull()
    }

    private val ballNbtInstance by lazy {
        ballNbtClass?.fields?.firstOrNull { it.name == "INSTANCE" }?.get(null)
    }

    private val hasPokemonMethod by lazy {
        ballNbtClass?.methods?.firstOrNull { it.name == "hasPokemon" && it.parameterCount == 1 }
    }

    private val readPokemonMethod by lazy {
        ballNbtClass?.methods?.firstOrNull { it.name == "readPokemon" && it.parameterCount == 2 }
    }

    fun resolvePlacedBallPokemon(level: Level, blockPos: BlockPos): OccupiedPokeBallPokemonData? {
        if (!available) return null

        val blockEntity = level.getBlockEntity(blockPos) ?: return null
        val blockEntityClass = placedBallBlockEntityClass ?: return null
        if (!blockEntityClass.isInstance(blockEntity)) return null

        val ballStack = runCatching {
            getBallStackMethod?.invoke(blockEntity) as? ItemStack
        }.getOrNull() ?: return null

        if (ballStack.isEmpty) return null

        val ballNbt = ballNbtInstance ?: return null
        val hasPokemon = runCatching {
            hasPokemonMethod?.invoke(ballNbt, ballStack) as? Boolean
        }.getOrNull() == true

        if (!hasPokemon) return null

        val pokemon = runCatching {
            readPokemonMethod?.invoke(ballNbt, ballStack, level.registryAccess()) as? Pokemon
        }.getOrNull() ?: return null

        return OccupiedPokeBallPokemonData(
            blockPos = blockPos,
            pokemon = pokemon,
            ballDisplayName = ballStack.item.description.string
        )
    }
}
