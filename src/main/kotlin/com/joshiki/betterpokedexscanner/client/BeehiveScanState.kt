package com.joshiki.betterpokedexscanner.client

import com.joshiki.betterpokedexscanner.network.BeehiveDetailsResponsePayload
import com.joshiki.betterpokedexscanner.scanner.BeehiveScanSnapshot
import net.minecraft.core.BlockPos

object BeehiveScanState {
    var active: Boolean = false
    var blockPos: BlockPos? = null
    var snapshot: BeehiveScanSnapshot? = null
    var scanProgress: Float = 0F
    var focusIntervals: Float = 0F
    var closing: Boolean = false
    var detailsRequestSent: Boolean = false

    fun clear() {
        active = false
        blockPos = null
        snapshot = null
        scanProgress = 0F
        focusIntervals = 0F
        closing = false
        detailsRequestSent = false
    }

    fun set(snapshot: BeehiveScanSnapshot) {
        active = true
        blockPos = snapshot.blockPos
        this.snapshot = snapshot
        scanProgress = 0F
        focusIntervals = 0F
        closing = false
        detailsRequestSent = false
    }

    fun beginClosing() {
        if (active) {
            closing = true
        }
    }

    fun markDetailsRequestSent() {
        detailsRequestSent = true
    }

    fun applyServerDetails(payload: BeehiveDetailsResponsePayload) {
        if (!active || blockPos != payload.blockPos) return
        val current = snapshot ?: return
        snapshot = current.withServerDetails(payload)
    }
}
