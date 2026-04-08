package com.joshiki.betterpokedexscanner.client

object SlowpokeTailRegrowthClientState {
    var activeEntityId: Int? = null
    var percent: Int? = null
    var requestedEntityId: Int? = null

    fun clear() {
        activeEntityId = null
        percent = null
        requestedEntityId = null
    }

    fun set(entityId: Int, percent: Int) {
        activeEntityId = entityId
        this.percent = percent
    }
}