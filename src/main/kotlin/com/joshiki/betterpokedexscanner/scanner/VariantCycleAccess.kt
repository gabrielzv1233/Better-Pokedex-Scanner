package com.joshiki.betterpokedexscanner.scanner

data class CarouselSlotState(
    var currentPage: Int = 0,
    var nextPage: Int = 0,
    var transitionProgress: Float = 0F,
    var cooldownTicks: Float = 0F,
    var ticksSinceFlip: Float = 0F
)

interface VariantCycleAccess {
    fun getVariantCycleTicksChimericShinies(): Float
    fun setVariantCycleTicksChimericShinies(value: Float)
    fun getCarouselSlotState(index: Int): CarouselSlotState
    fun tickCarouselScheduler(pageCounts: List<Int>)
    fun resetCarouselScheduler()
}
