package com.joshiki.betterpokedexscanner.scanner

import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.block.RestorationTankBlock
import com.cobblemon.mod.common.block.entity.RestorationTankBlockEntity
import com.cobblemon.mod.common.block.multiblock.FossilMultiblockStructure
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.BlockHitResult

data class RestorationTankScanData(
    val blockPos: BlockPos,
    val speciesId: ResourceLocation,
    val timeRemaining: Int,
    val validSpecies: Boolean,
    val formName: String?,
    val aspects: Set<String>,
    val shiny: Boolean?
)

object RestorationTankScanner {

    private fun isValidSpeciesId(speciesId: ResourceLocation): Boolean {
        return try {
            val speciesClass = Class.forName("com.cobblemon.mod.common.api.pokemon.PokemonSpecies")
            val species = speciesClass.methods
                .firstOrNull { it.name == "getByIdentifier" && it.parameterCount == 1 }
                ?.invoke(null, speciesId)
            species != null
        } catch (_: Exception) {
            false
        }
    }

    fun findRestorationTankTarget(user: LocalPlayer): RestorationTankScanData? {
        val hit = net.minecraft.client.Minecraft.getInstance().hitResult as? BlockHitResult
        if (hit == null) {
            println("BPS tank scan: crosshair hit was not a BlockHitResult")
            return null
        }

        val level = user.level()
        var pos = hit.blockPos
        var state = level.getBlockState(pos)

        println("BPS tank scan: raw hit pos=$pos block=${state.block}")

        if (state.block != CobblemonBlocks.RESTORATION_TANK) {
            println("BPS tank scan: hit block was not restoration tank")
            return null
        }

        if (state.getValue(RestorationTankBlock.PART) == RestorationTankBlock.TankPart.TOP) {
            pos = pos.below()
            state = level.getBlockState(pos)
            println("BPS tank scan: normalized from TOP to BOTTOM at pos=$pos block=${state.block}")

            if (state.block != CobblemonBlocks.RESTORATION_TANK) {
                println("BPS tank scan: normalized bottom block was not restoration tank")
                return null
            }

            if (state.getValue(RestorationTankBlock.PART) != RestorationTankBlock.TankPart.BOTTOM) {
                println("BPS tank scan: normalized block was not bottom half")
                return null
            }
        }

        val blockEntity = level.getBlockEntity(pos)
        println("BPS tank scan: blockEntity=$blockEntity")

        val tankEntity = blockEntity as? RestorationTankBlockEntity
        if (tankEntity == null) {
            println("BPS tank scan: block entity was not RestorationTankBlockEntity")
            return null
        }

        val struct = tankEntity.multiblockStructure
        println("BPS tank scan: multiblockStructure=$struct")

        val fossilStruct = struct as? FossilMultiblockStructure
        if (fossilStruct == null) {
            println("BPS tank scan: structure was not FossilMultiblockStructure")
            return null
        }

        println(
            "BPS tank scan: running=${fossilStruct.isRunning()} created=${fossilStruct.hasCreatedPokemon} " +
                    "resultingFossil=${fossilStruct.resultingFossil} timeRemaining=${fossilStruct.timeRemaining}"
        )

        val fossil = fossilStruct.resultingFossil
        if (fossil == null) {
            println("BPS tank scan: resultingFossil was null")
            return null
        }

        if (!(fossilStruct.isRunning() || fossilStruct.hasCreatedPokemon)) {
            println("BPS tank scan: structure not running and no created pokemon")
            return null
        }

        val resultProps = fossil.result

        val declaredSpeciesString = resultProps.species
        if (declaredSpeciesString.isNullOrBlank()) {
            println("BPS tank scan: result species was null/blank")
            return RestorationTankScanData(
                blockPos = pos,
                speciesId = fossil.identifier,
                timeRemaining = fossilStruct.timeRemaining,
                validSpecies = false,
                formName = resultProps.form,
                aspects = resultProps.aspects,
                shiny = resultProps.shiny
            )
        }

        val declaredSpeciesId = try {
            if (declaredSpeciesString.contains(":")) {
                ResourceLocation.parse(declaredSpeciesString)
            } else {
                ResourceLocation.fromNamespaceAndPath("cobblemon", declaredSpeciesString)
            }
        } catch (_: Exception) {
            null
        }

        val validDeclaredSpecies = declaredSpeciesId?.let(::isValidSpeciesId) == true

        if (!validDeclaredSpecies) {
            println("BPS tank scan: invalid declared result species=$declaredSpeciesString")
            return RestorationTankScanData(
                blockPos = pos,
                speciesId = fossil.identifier,
                timeRemaining = fossilStruct.timeRemaining,
                validSpecies = false,
                formName = resultProps.form,
                aspects = resultProps.aspects,
                shiny = resultProps.shiny
            )
        }

        val resolvedPokemon = try {
            resultProps.create()
        } catch (_: Exception) {
            null
        }

        if (resolvedPokemon == null) {
            println("BPS tank scan: failed to create result pokemon from result properties")
            return RestorationTankScanData(
                blockPos = pos,
                speciesId = declaredSpeciesId,
                timeRemaining = fossilStruct.timeRemaining,
                validSpecies = true,
                formName = resultProps.form,
                aspects = resultProps.aspects,
                shiny = resultProps.shiny
            )
        }

        val resolvedSpeciesId = resolvedPokemon.species.resourceIdentifier
        val resolvedFormName = resolvedPokemon.form.name
        val resolvedAspects = resolvedPokemon.aspects.toSet()
        val resolvedShiny = resolvedPokemon.shiny

        println(
            "BPS tank scan: SUCCESS declared=$declaredSpeciesId resolved=$resolvedSpeciesId " +
                    "form=$resolvedFormName aspects=$resolvedAspects shiny=$resolvedShiny time=${fossilStruct.timeRemaining}"
        )

        return RestorationTankScanData(
            blockPos = pos,
            speciesId = resolvedSpeciesId,
            timeRemaining = fossilStruct.timeRemaining,
            validSpecies = true,
            formName = resolvedFormName,
            aspects = resolvedAspects,
            shiny = resolvedShiny
        )
    }
}