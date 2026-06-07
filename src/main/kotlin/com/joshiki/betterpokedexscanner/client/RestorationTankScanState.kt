package com.joshiki.betterpokedexscanner.client

import com.cobblemon.mod.common.pokemon.Gender
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import com.joshiki.betterpokedexscanner.scanner.PokemonBlockScanKind
import com.joshiki.betterpokedexscanner.scanner.VariantDisplay

object RestorationTankScanState {
    var active: Boolean = false
    var blockPos: BlockPos? = null
    var speciesId: ResourceLocation? = null
    var timeRemaining: Int? = null
    var focusIntervals: Float = 0F
    var closing: Boolean = false
    var validSpecies: Boolean = true
    var formName: String? = null
    var aspects: Set<String> = emptySet()
    var shiny: Boolean? = null
    var scanKind: PokemonBlockScanKind = PokemonBlockScanKind.RESTORATION_TANK
    var level: Int? = null
    var containerName: String? = null
    var containerNameComponent: MutableComponent? = null
    var gender: Gender? = null
    var typeLabel: String? = null
    var typeLabelComponent: MutableComponent? = null
    var extraMirroredDisplays: List<VariantDisplay> = emptyList()


    fun clear() {
        active = false
        blockPos = null
        speciesId = null
        timeRemaining = null
        focusIntervals = 0F
        closing = false
        validSpecies = true
        formName = null
        aspects = emptySet()
        shiny = null
        scanKind = PokemonBlockScanKind.RESTORATION_TANK
        level = null
        containerName = null
        containerNameComponent = null
        gender = null
        typeLabel = null
        typeLabelComponent = null
        extraMirroredDisplays = emptyList()
    }

    fun set(
        blockPos: BlockPos,
        speciesId: ResourceLocation,
        timeRemaining: Int,
        validSpecies: Boolean,
        formName: String?,
        aspects: Set<String>,
        shiny: Boolean?,
        scanKind: PokemonBlockScanKind,
        level: Int?,
        containerName: String?,
        containerNameComponent: MutableComponent?,
        gender: Gender?,
        typeLabel: String?,
        typeLabelComponent: MutableComponent?,
        extraMirroredDisplays: List<VariantDisplay>
    ) {
        active = true
        this.blockPos = blockPos
        this.speciesId = speciesId
        this.timeRemaining = timeRemaining
        this.validSpecies = validSpecies
        this.formName = formName
        this.aspects = aspects
        this.shiny = shiny
        this.scanKind = scanKind
        this.level = level
        this.containerName = containerName
        this.containerNameComponent = containerNameComponent
        this.gender = gender
        this.typeLabel = typeLabel
        this.typeLabelComponent = typeLabelComponent
        this.extraMirroredDisplays = extraMirroredDisplays
        this.focusIntervals = 0F
        this.closing = false
    }

    fun beginClosing() {
        if (active) {
            closing = true
        }
    }
}
