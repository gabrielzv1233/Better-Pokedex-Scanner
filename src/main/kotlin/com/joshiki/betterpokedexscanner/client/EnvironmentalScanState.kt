package com.joshiki.betterpokedexscanner.client

import com.joshiki.betterpokedexscanner.scanner.EnvironmentalScanSnapshot

object EnvironmentalScanState {
    const val INITIAL_IDLE_TICKS = 100F
    const val REACQUIRE_IDLE_TICKS = 60F

    var sessionId: Int = 0
    var active: Boolean = false
    var completed: Boolean = false
    var scanProgress: Float = 0F
    var focusIntervals: Float = 0F
    var idleTicks: Float = 0F
    var reacquirePending: Boolean = false
    var structureRequestSent: Boolean = false
    var snapshot: EnvironmentalScanSnapshot? = null

    fun clear() {
        sessionId = 0
        active = false
        completed = false
        scanProgress = 0F
        focusIntervals = 0F
        idleTicks = 0F
        reacquirePending = false
        structureRequestSent = false
        snapshot = null
    }

    fun beginFreshScan(snapshot: EnvironmentalScanSnapshot) {
        sessionId += 1
        active = true
        completed = false
        scanProgress = 0F
        focusIntervals = 0F
        idleTicks = 0F
        reacquirePending = false
        structureRequestSent = false
        this.snapshot = snapshot
    }

    fun restoreSnapshot(snapshot: EnvironmentalScanSnapshot) {
        sessionId += 1
        active = true
        completed = true
        scanProgress = 0F
        focusIntervals = 0F
        idleTicks = 0F
        reacquirePending = false
        structureRequestSent = false
        this.snapshot = snapshot
    }

    fun suspendForTarget() {
        reacquirePending = completed && snapshot != null
        active = false
        completed = false
        scanProgress = 0F
        focusIntervals = 0F
        idleTicks = 0F
        structureRequestSent = false
        snapshot = null
    }

    fun markStructureRequestSent() {
        structureRequestSent = true
    }

    fun applyStructureResult(sessionId: Int, structureName: String?) {
        if (this.sessionId != sessionId) return
        val currentSnapshot = snapshot ?: return
        snapshot = currentSnapshot.copy(nearbyStructureName = structureName)
    }
}
