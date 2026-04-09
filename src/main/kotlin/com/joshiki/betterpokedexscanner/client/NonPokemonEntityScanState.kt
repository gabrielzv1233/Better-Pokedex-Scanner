package com.joshiki.betterpokedexscanner.client

import com.joshiki.betterpokedexscanner.scanner.NonPokemonEntityScanSnapshot

object NonPokemonEntityScanState {
    var active: Boolean = false
    var scanProgress: Float = 0F
    var focusIntervals: Float = 0F
    var snapshot: NonPokemonEntityScanSnapshot? = null
    var cachedEntityTypeId: String? = null

    fun clear() {
        active = false
        scanProgress = 0F
        focusIntervals = 0F
        snapshot = null
        cachedEntityTypeId = null
    }

    fun clearActive() {
        active = false
        scanProgress = 0F
        focusIntervals = 0F
        snapshot = null
    }

    fun clearCached() {
        cachedEntityTypeId = null
    }

    fun set(snapshot: NonPokemonEntityScanSnapshot) {
        active = true
        scanProgress = 0F
        focusIntervals = 0F
        this.snapshot = snapshot
    }

    fun restoreCached(snapshot: NonPokemonEntityScanSnapshot, completedProgress: Float) {
        active = true
        scanProgress = completedProgress
        focusIntervals = 0F
        this.snapshot = snapshot
        cachedEntityTypeId = snapshot.entityTypeId
    }

    fun markCached() {
        cachedEntityTypeId = snapshot?.entityTypeId
    }
}
