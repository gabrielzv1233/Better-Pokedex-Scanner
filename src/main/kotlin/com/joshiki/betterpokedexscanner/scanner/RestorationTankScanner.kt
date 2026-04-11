package com.joshiki.betterpokedexscanner.scanner

import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.block.RestorationTankBlock
import com.cobblemon.mod.common.block.entity.RestorationTankBlockEntity
import com.cobblemon.mod.common.block.multiblock.FossilMultiblockStructure
import com.cobblemon.mod.common.pokemon.Gender
import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.BlockHitResult

data class RestorationTankScanData(
    val blockPos: BlockPos,
    val speciesId: ResourceLocation,
    val timeRemaining: Int,
    val validSpecies: Boolean,
    val formName: String?,
    val aspects: Set<String>,
    val shiny: Boolean?,
    val scanKind: PokemonBlockScanKind = PokemonBlockScanKind.RESTORATION_TANK,
    val level: Int? = null,
    val containerName: String? = null,
    val gender: Gender? = null,
    val typeLabel: String? = null,
    val typeLabelComponent: MutableComponent? = null,
    val resolvedPokemon: Pokemon? = null,
    val extraMirroredDisplays: List<VariantDisplay> = emptyList()
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
                shiny = resultProps.shiny,
                typeLabel = null,
                typeLabelComponent = null,
                resolvedPokemon = null,
                extraMirroredDisplays = emptyList()
            )
        }

        val resolvedSpeciesId = resolvedPokemon.species.resourceIdentifier
        val resolvedFormName = resolvedPokemon.form.name
        val resolvedAspects = resolvedPokemon.aspects.toSet()
        val resolvedShiny = resolvedPokemon.shiny
        val resolvedTypeLabel = buildPokemonTypeLabel(resolvedPokemon)

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
            shiny = resolvedShiny,
            typeLabel = resolvedTypeLabel,
            typeLabelComponent = buildPokemonTypeLabelComponent(resolvedPokemon),
            resolvedPokemon = resolvedPokemon,
            extraMirroredDisplays = emptyList()
        )
    }

    internal fun buildPokemonTypeLabel(pokemon: Pokemon): String {
        val labels = pokemon.form.types.map { it.displayName.string }
        return if (labels.isEmpty()) "Unknown Type" else "${labels.joinToString("/")} Type"
    }

    internal fun buildPokemonTypeLabelComponent(pokemon: Pokemon): MutableComponent {
        val labels = pokemon.form.types.map { it.displayName.string }
        return if (labels.isEmpty()) {
            PokemonTypeLabelFormatter.buildUnknownTypeComponent()
        } else {
            PokemonTypeLabelFormatter.buildTypeComponent(labels)
        }
    }

    internal fun buildAbilityDisplay(pokemon: Pokemon): VariantDisplay? {
        val abilityTranslationKey = pokemon.ability.displayName.takeIf { it.isNotBlank() }
        val abilityLabel = abilityTranslationKey
            ?: pokemon.ability.name.takeIf { it.isNotBlank() }
            ?: return null
        return VariantDisplay(
            headerLabel = "Ability",
            label = abilityLabel,
            labelTranslationKey = abilityTranslationKey?.takeIf { it.contains('.') }
        )
    }

    internal fun buildIvDisplay(pokemon: Pokemon): VariantDisplay? {
        val ivs = resolveIvValues(pokemon)
        val raisedStat = pokemon.effectiveNature.increasedStat?.showdownId?.let(::normalizeNatureStatName)
        val loweredStat = pokemon.effectiveNature.decreasedStat?.showdownId?.let(::normalizeNatureStatName)

        val statOrder = listOf(
            Triple("hp", "HP", 0x9EE865),
            Triple("attack", "Attack", 0xF5DE69),
            Triple("defense", "Defense", 0xF09A65),
            Triple("special_attack", "Sp.Atk", 0x66D8F6),
            Triple("special_defense", "Sp.Def", 0x899EEA),
            Triple("speed", "Speed", 0xE46CCA)
        )

        val valueComponent = Component.empty()
        statOrder.forEachIndexed { index, (key, _, colour) ->
            if (index > 0) {
                valueComponent.append(
                    Component.literal("/").withStyle { style -> style.withBold(true) }
                )
            }

            val value = ivs[key] ?: return null
            var valueText = value.toString()
            if (raisedStat == key) valueText += "▲"
            if (loweredStat == key) valueText += "▼"

            valueComponent.append(
                Component.literal(valueText).withStyle { style ->
                    style.withColor(colour).withBold(true)
                }
            )
        }

        return VariantDisplay(
            headerLabel = "IVs",
            label = valueComponent.string,
            labelComponent = valueComponent
        )
    }

    private fun resolveIvValues(pokemon: Pokemon): Map<String, Int> {
        val ivs = pokemon.ivs
        return linkedMapOf(
            "hp" to ivs.getOrDefault(Stats.HP),
            "attack" to ivs.getOrDefault(Stats.ATTACK),
            "defense" to ivs.getOrDefault(Stats.DEFENCE),
            "special_attack" to ivs.getOrDefault(Stats.SPECIAL_ATTACK),
            "special_defense" to ivs.getOrDefault(Stats.SPECIAL_DEFENCE),
            "speed" to ivs.getOrDefault(Stats.SPEED)
        )
    }

    private fun normalizeNatureStatName(value: String): String? {
        return when (value.lowercase().replace("-", "_")) {
            "hp" -> "hp"
            "attack", "atk" -> "attack"
            "defense", "defence", "def" -> "defense"
            "special_attack", "sp_attack", "spatk", "sp_atk", "spa" -> "special_attack"
            "special_defense", "special_defence", "sp_defense", "sp_defence", "spdef", "sp_def", "spd" -> "special_defense"
            "speed", "spe" -> "speed"
            else -> null
        }
    }
}
