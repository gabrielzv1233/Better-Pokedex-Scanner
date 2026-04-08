package com.joshiki.betterpokedexscanner.client

import com.cobblemon.mod.common.api.pokedex.PokedexLearnedInformation
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation


object RestorationTankRegistrationState {
    var active: Boolean = false
    var blockPos: BlockPos? = null
    var speciesId: ResourceLocation? = null
    var scanProgress: Float = 0F
    var startSent: Boolean = false
    var awaitingServerConfirm: Boolean = false
    var registerCompleted: Boolean = false
    var newInformation: PokedexLearnedInformation = PokedexLearnedInformation.NONE
    var displayRegisterInfoIntervals: Float = 0F

    fun clear() {
        active = false
        blockPos = null
        speciesId = null
        scanProgress = 0F
        startSent = false
        awaitingServerConfirm = false
        registerCompleted = false
        newInformation = PokedexLearnedInformation.NONE
        displayRegisterInfoIntervals = 0F
    }

    fun setTarget(blockPos: BlockPos, speciesId: ResourceLocation) {
        active = true
        this.blockPos = blockPos
        this.speciesId = speciesId
        scanProgress = 0F
        startSent = false
        awaitingServerConfirm = false
        registerCompleted = false
        newInformation = PokedexLearnedInformation.NONE
        displayRegisterInfoIntervals = 0F
    }
}