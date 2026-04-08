package com.joshiki.betterpokedexscanner.client

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation

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
    }

    fun set(
        blockPos: BlockPos,
        speciesId: ResourceLocation,
        timeRemaining: Int,
        validSpecies: Boolean,
        formName: String?,
        aspects: Set<String>,
        shiny: Boolean?
    ) {
        active = true
        this.blockPos = blockPos
        this.speciesId = speciesId
        this.timeRemaining = timeRemaining
        this.validSpecies = validSpecies
        this.formName = formName
        this.aspects = aspects
        this.shiny = shiny
        this.focusIntervals = 0F
        this.closing = false
    }

    fun beginClosing() {
        if (active) {
            closing = true
        }
    }
}