package com.joshiki.betterpokedexscanner.scanner

import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.phys.BlockHitResult

object OccupiedPokeBallScanner {
    fun findTarget(user: LocalPlayer): RestorationTankScanData? {
        val hit = Minecraft.getInstance().hitResult as? BlockHitResult ?: return null
        val resolved = OccupiedPokeBallCompatibility.resolvePlacedBallPokemon(user.level(), hit.blockPos) ?: return null
        val pokemon = resolved.pokemon

        return RestorationTankScanData(
            blockPos = resolved.blockPos,
            speciesId = pokemon.species.resourceIdentifier,
            timeRemaining = 0,
            validSpecies = true,
            formName = pokemon.form.name,
            aspects = pokemon.aspects.toSet(),
            shiny = pokemon.shiny,
            scanKind = PokemonBlockScanKind.OCCUPIED_BALL,
            level = pokemon.level,
            containerName = resolved.ballDisplayName,
            gender = pokemon.gender,
            typeLabel = RestorationTankScanner.buildPokemonTypeLabel(pokemon),
            typeLabelComponent = RestorationTankScanner.buildPokemonTypeLabelComponent(pokemon),
            resolvedPokemon = pokemon,
            extraMirroredDisplays = listOfNotNull(
                RestorationTankScanner.buildAbilityDisplay(pokemon),
                RestorationTankScanner.buildIvDisplay(pokemon)
            )
        )
    }
}
