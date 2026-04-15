package com.joshiki.betterpokedexscanner.mixin

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress
import com.cobblemon.mod.common.api.pokedex.PokedexLearnedInformation
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.pokedex.scanner.PokedexUsageContext
import com.cobblemon.mod.common.pokedex.scanner.PokemonScanner
import com.cobblemon.mod.common.pokedex.scanner.ScannableEntity
import com.joshiki.betterpokedexscanner.scanner.VariantCycleAccess
import com.joshiki.betterpokedexscanner.scanner.CarouselSlotState
import com.joshiki.betterpokedexscanner.scanner.VariantLabelResolver
import com.joshiki.betterpokedexscanner.scanner.PokemonBlockScanPresentation
import net.minecraft.client.player.LocalPlayer
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Overwrite
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.Unique
import kotlin.random.Random
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.joshiki.betterpokedexscanner.client.SlowpokeTailRegrowthClientState
import com.joshiki.betterpokedexscanner.network.SlowpokeTailRegrowthRequestPayload
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import com.joshiki.betterpokedexscanner.client.RestorationTankScanState
import com.joshiki.betterpokedexscanner.client.EnvironmentalScanState
import com.joshiki.betterpokedexscanner.client.BeehiveScanState
import com.joshiki.betterpokedexscanner.client.NonPokemonEntityScanState
import com.joshiki.betterpokedexscanner.scanner.PokemonBlockScanner
import com.joshiki.betterpokedexscanner.scanner.EnvironmentalScanner
import com.joshiki.betterpokedexscanner.scanner.BeehiveScanner
import com.joshiki.betterpokedexscanner.scanner.NonPokemonEntityScanner
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import com.joshiki.betterpokedexscanner.client.RestorationTankRegistrationState
import com.joshiki.betterpokedexscanner.network.StartTankScanningPayload
import com.joshiki.betterpokedexscanner.network.FinishTankScanningPayload
import com.joshiki.betterpokedexscanner.network.EnvironmentalStructureRequestPayload
import com.joshiki.betterpokedexscanner.network.BeehiveDetailsRequestPayload
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics



@Mixin(PokedexUsageContext::class)
abstract class PokedexUsageContextMixin : VariantCycleAccess {

    @Shadow var infoGuiOpen: Boolean = false
    @Shadow var scanningGuiOpen: Boolean = false
    @Shadow var isPokemonInFocusOwned: Boolean = false
    @Shadow var registerCompleted: Boolean = false
    @Shadow var scannedSpecies: ResourceLocation? = null
    @Shadow var scannableEntityInFocus: ScannableEntity? = null
    @Shadow var viewInfoTicks: Int = 0
    @Shadow var scanningProgress: Float = 0F
    @Shadow var displayRegisterInfoIntervals: Float = 0F
    @Shadow var transitionIntervals: Float = 0F
    @Shadow var usageIntervals: Float = 0F
    @Shadow var focusIntervals: Float = 0F
    @Shadow var innerRingRotation: Float = 0F
    @Shadow var zoomLevel: Float = 0F
    @Shadow var zoomModifier: Float = 1F
    @Shadow var newPokemonInfo: PokedexLearnedInformation = PokedexLearnedInformation.NONE
    @Shadow var availableInfoFrames: MutableList<Boolean?> = mutableListOf()

    @Unique
    private var variantCycleTicksBetterPokedexScanner: Float = 0F
    @Unique
    private var carouselSlotStates: MutableList<CarouselSlotState>? = null
    @Unique
    private var carouselNextFlipDelayTicks: Float = 50F

    @Shadow
    abstract fun resetFocusedPokemonState()

    @Shadow
    abstract fun playSound(sound: SoundEvent)

    override fun getVariantCycleTicksBetterPokedexScanner(): Float = variantCycleTicksBetterPokedexScanner


    override fun setVariantCycleTicksBetterPokedexScanner(value: Float) {
        variantCycleTicksBetterPokedexScanner = value
    }

    override fun getCarouselSlotState(index: Int): CarouselSlotState {
        val states = getOrCreateCarouselSlotStates()
        return states.getOrElse(index) { CarouselSlotState() }
    }

    override fun tickCarouselScheduler(pageCounts: List<Int>) {
        if (pageCounts.isEmpty()) return
        val states = getOrCreateCarouselSlotStates()

        var anyTransitioning = false
        pageCounts.forEachIndexed { index, count ->
            val state = states[index]

            if (count <= 1) {
                state.currentPage = 0
                state.nextPage = 0
                state.transitionProgress = 0F
                state.cooldownTicks = 0F
                state.ticksSinceFlip = 0F
                return@forEachIndexed
            }

            state.currentPage %= count
            state.nextPage %= count
            state.cooldownTicks = (state.cooldownTicks - 1F).coerceAtLeast(0F)
            state.ticksSinceFlip += 1F

            if (state.transitionProgress > 0F) {
                anyTransitioning = true
                state.transitionProgress = (state.transitionProgress + 0.1F).coerceAtMost(1F)
                if (state.transitionProgress >= 1F) {
                    state.currentPage = state.nextPage
                    state.transitionProgress = 0F
                }
            }
        }

        if (anyTransitioning) return

        carouselNextFlipDelayTicks -= 1F
        if (carouselNextFlipDelayTicks > 0F) return

        val candidates = pageCounts.mapIndexedNotNull { index, count ->
            if (count <= 1) return@mapIndexedNotNull null

            val state = states[index]
            if (state.cooldownTicks > 0F) return@mapIndexedNotNull null

            val pageWeight = (count - 1).coerceAtMost(4).toFloat() * 0.35F
            val stalenessWeight = (state.ticksSinceFlip / 45F).coerceAtMost(6F)
            val jitter = Random.nextFloat() * 0.75F
            Triple(index, count, pageWeight + stalenessWeight + jitter)
        }

        if (candidates.isEmpty()) {
            carouselNextFlipDelayTicks = 14F
            return
        }

        val chosen = candidates.maxBy { it.third }
        val chosenState = states[chosen.first]
        chosenState.nextPage = (chosenState.currentPage + 1) % chosen.second
        chosenState.transitionProgress = 0.01F
        chosenState.cooldownTicks = 40F + Random.nextFloat() * 20F
        chosenState.ticksSinceFlip = 0F

        carouselNextFlipDelayTicks = 28F + Random.nextFloat() * 18F
    }

    override fun resetCarouselScheduler() {
        getOrCreateCarouselSlotStates().forEach {
            it.currentPage = 0
            it.nextPage = 0
            it.transitionProgress = 0F
            it.cooldownTicks = 0F
            it.ticksSinceFlip = 0F
        }
        carouselNextFlipDelayTicks = 50F
    }

    @Unique
    private fun assignRandomInfoFrames(frameCount: Int) {
        availableInfoFrames = mutableListOf(null, null, null, null)

        repeat(frameCount.coerceIn(0, availableInfoFrames.size)) {
            var randomIndex = Random.nextInt(availableInfoFrames.size)
            if (availableInfoFrames[randomIndex] != null) {
                randomIndex = availableInfoFrames.indexOfFirst { it == null }
            }
            availableInfoFrames[randomIndex] = Random.nextBoolean()
        }
    }

    @Unique
    private fun assignFixedInfoFrames(visibleSlots: List<Boolean>) {
        availableInfoFrames = mutableListOf(null, null, null, null)

        visibleSlots.take(availableInfoFrames.size).forEachIndexed { index, isVisible ->
            if (isVisible) {
                availableInfoFrames[index] = Random.nextBoolean()
            }
        }
    }

    @Unique
    private fun ensureInfoFrameCount(frameCount: Int) {
        if (availableInfoFrames.count { it != null } != frameCount.coerceIn(0, 4)) {
            assignRandomInfoFrames(frameCount)
            resetCarouselScheduler()
        }
    }

    @Unique
    private fun ensureFixedInfoFrames(visibleSlots: List<Boolean>) {
        val desiredSlots = visibleSlots.take(4)
        val currentSlots = availableInfoFrames.map { it != null }.take(4)
        if (currentSlots != desiredSlots) {
            assignFixedInfoFrames(desiredSlots)
            resetCarouselScheduler()
        }
    }

    @Unique
    private fun getOrCreateCarouselSlotStates(): MutableList<CarouselSlotState> {
        val existing = carouselSlotStates
        if (existing != null) {
            return existing
        }

        val created = mutableListOf(
            CarouselSlotState(),
            CarouselSlotState(),
            CarouselSlotState(),
            CarouselSlotState(),
            CarouselSlotState(),
            CarouselSlotState(),
            CarouselSlotState(),
            CarouselSlotState()
        )
        carouselSlotStates = created
        return created
    }

    @Suppress("UNUSED_PARAMETER")
    @Inject(method = ["stopUsing"], at = [At("HEAD")])
    private fun clearCustomScanStateOnStop(ticks: Int, sound: ResourceLocation?, ci: CallbackInfo) {
        RestorationTankScanState.clear()
        BeehiveScanState.clear()
        NonPokemonEntityScanState.clear()
        SlowpokeTailRegrowthClientState.clear()
        variantCycleTicksBetterPokedexScanner = 0F
        RestorationTankRegistrationState.clear()
        EnvironmentalScanState.clear()
        resetCarouselScheduler()
    }

    @Suppress("UNUSED_PARAMETER")
    @Inject(method = ["renderUpdate"], at = [At("TAIL")])
    private fun speedUpTankScanRotation(
        graphics: GuiGraphics,
        tickCounter: DeltaTracker,
        ci: CallbackInfo
    ) {
        if (
            RestorationTankRegistrationState.active &&
            !RestorationTankRegistrationState.registerCompleted &&
            RestorationTankRegistrationState.scanProgress > 0F
        ) {
            val tickDelta = tickCounter.realtimeDeltaTicks.takeIf { !Minecraft.getInstance().isPaused } ?: 0F
            val updateInterval = (tickDelta / 20F) * PokedexUsageContext.RENDER_UPDATES_PER_SECOND

            // Vanilla already added the "slow" +1x rotation.
            // Add another 9x so the total matches the normal scanned-entity speed (~10x).
            innerRingRotation = (innerRingRotation + (updateInterval * 9F)) % 360
        } else if (
            BeehiveScanState.active &&
            BeehiveScanState.scanProgress > 0F
        ) {
            val tickDelta = tickCounter.realtimeDeltaTicks.takeIf { !Minecraft.getInstance().isPaused } ?: 0F
            val updateInterval = (tickDelta / 20F) * PokedexUsageContext.RENDER_UPDATES_PER_SECOND
            innerRingRotation = (innerRingRotation + (updateInterval * 9F)) % 360
        } else if (
            EnvironmentalScanState.active &&
            !EnvironmentalScanState.completed &&
            EnvironmentalScanState.scanProgress > 0F
        ) {
            val tickDelta = tickCounter.realtimeDeltaTicks.takeIf { !Minecraft.getInstance().isPaused } ?: 0F
            val updateInterval = (tickDelta / 20F) * PokedexUsageContext.RENDER_UPDATES_PER_SECOND
            innerRingRotation = (innerRingRotation + (updateInterval * 9F)) % 360
        } else if (
            NonPokemonEntityScanState.active &&
            NonPokemonEntityScanState.scanProgress > 0F &&
            NonPokemonEntityScanState.focusIntervals <= 0F
        ) {
            val tickDelta = tickCounter.realtimeDeltaTicks.takeIf { !Minecraft.getInstance().isPaused } ?: 0F
            val updateInterval = (tickDelta / 20F) * PokedexUsageContext.RENDER_UPDATES_PER_SECOND
            innerRingRotation = (innerRingRotation + (updateInterval * 9F)) % 360
        }
    }

    @Overwrite
    fun tryScanPokemon(user: LocalPlayer) {

        if (RestorationTankScanState.closing) {
            RestorationTankScanState.focusIntervals =
                (RestorationTankScanState.focusIntervals - 2.5F).coerceAtLeast(0F)

            if (RestorationTankScanState.focusIntervals <= 0F) {
                RestorationTankScanState.clear()
                RestorationTankRegistrationState.clear()
            }
        }

        if (BeehiveScanState.closing) {
            BeehiveScanState.focusIntervals =
                (BeehiveScanState.focusIntervals - 2.5F).coerceAtLeast(0F)

            if (BeehiveScanState.focusIntervals <= 0F) {
                BeehiveScanState.clear()
            }
        }

        val targetScannableEntity = PokemonScanner.findScannableEntity(user, zoomLevel.toInt())
        val targetNonPokemonEntity = NonPokemonEntityScanner.findTarget(user, zoomLevel.toInt())

        if (targetNonPokemonEntity != null) {
            EnvironmentalScanState.suspendForTarget()
            resetFocusedPokemonState()
            scannableEntityInFocus = null

            val isNewEntityTarget =
                !NonPokemonEntityScanState.active ||
                    NonPokemonEntityScanState.snapshot?.entityId != targetNonPokemonEntity.entityId
            val isCachedEntityType =
                NonPokemonEntityScanState.cachedEntityTypeId == targetNonPokemonEntity.entityTypeId

            if (isNewEntityTarget) {
                variantCycleTicksBetterPokedexScanner = 0F
                resetCarouselScheduler()

                SlowpokeTailRegrowthClientState.clear()
                RestorationTankScanState.beginClosing()
                BeehiveScanState.beginClosing()
                RestorationTankRegistrationState.clear()

                if (isCachedEntityType) {
                    NonPokemonEntityScanState.restoreCached(
                        snapshot = targetNonPokemonEntity,
                        completedProgress = PokedexUsageContext.MAX_SCAN_PROGRESS.toFloat()
                    )
                } else {
                    NonPokemonEntityScanState.set(targetNonPokemonEntity)
                }

                isPokemonInFocusOwned = false
                if (targetNonPokemonEntity.useFixedSlotLayout) {
                    assignFixedInfoFrames(targetNonPokemonEntity.fixedVisibleSlots)
                } else {
                    assignRandomInfoFrames(targetNonPokemonEntity.frameDisplays.size)
                }

                newPokemonInfo = PokedexLearnedInformation.NONE

                playSound(CobblemonSounds.POKEDEX_SCAN_DETAIL)
            } else {
                variantCycleTicksBetterPokedexScanner += 1F
                    if (targetNonPokemonEntity.useFixedSlotLayout) {
                        ensureFixedInfoFrames(targetNonPokemonEntity.fixedVisibleSlots)
                    } else {
                        ensureInfoFrameCount(targetNonPokemonEntity.frameDisplays.size)
                    }

                    if (NonPokemonEntityScanState.scanProgress < PokedexUsageContext.MAX_SCAN_PROGRESS) {
                        NonPokemonEntityScanState.scanProgress =
                            (NonPokemonEntityScanState.scanProgress + 2.5F)
                                .coerceAtMost(PokedexUsageContext.MAX_SCAN_PROGRESS.toFloat())

                    if (variantCycleTicksBetterPokedexScanner % 2F == 0F) {
                        playSound(CobblemonSounds.POKEDEX_SCAN_LOOP)
                    }

                        if (NonPokemonEntityScanState.scanProgress >= PokedexUsageContext.MAX_SCAN_PROGRESS) {
                            NonPokemonEntityScanState.markCached()
                            playSound(CobblemonSounds.POKEDEX_SCAN_REGISTER_ASPECT)
                        }
                } else {
                    NonPokemonEntityScanState.focusIntervals =
                        (NonPokemonEntityScanState.focusIntervals + 2.5F)
                            .coerceAtMost(PokedexUsageContext.FOCUS_INTERVALS)

                    if (NonPokemonEntityScanState.focusIntervals >= PokedexUsageContext.FOCUS_INTERVALS) {
                        NonPokemonEntityScanState.focusIntervals = PokedexUsageContext.FOCUS_INTERVALS
                        val mirroredPageCounts =
                            targetNonPokemonEntity.mirroredDisplayGroups.map { it.displays.size }
                        tickCarouselScheduler(
                            targetNonPokemonEntity.frameDisplays.map { it.size } + mirroredPageCounts
                        )
                    }
                }
            }
        } else if (targetScannableEntity != null) {
            EnvironmentalScanState.suspendForTarget()
            if (targetScannableEntity != scannableEntityInFocus) {
                NonPokemonEntityScanState.clearActive()
                resetFocusedPokemonState()
                scannableEntityInFocus = targetScannableEntity
                variantCycleTicksBetterPokedexScanner = 0F
                resetCarouselScheduler()

                RestorationTankScanState.beginClosing()
                BeehiveScanState.beginClosing()

                SlowpokeTailRegrowthClientState.clear()
                RestorationTankRegistrationState.clear()

                val focusedEntity = scannableEntityInFocus?.resolveEntityScan() as? PokemonEntity
                val focusedSpeciesId = focusedEntity?.pokemon?.species?.resourceIdentifier?.toString()

                if (focusedEntity != null && focusedSpeciesId == "cobblemon:slowpoke") {
                    SlowpokeTailRegrowthClientState.requestedEntityId = focusedEntity.id
                    ClientPlayNetworking.send(SlowpokeTailRegrowthRequestPayload(focusedEntity.id))
                }


                val resolvedPokemon = scannableEntityInFocus?.resolvePokemonScan()
                if (resolvedPokemon == null) {
                    resetFocusedPokemonState()
                    return
                }

                val liveAspects = focusedEntity?.aspects

                isPokemonInFocusOwned =
                    CobblemonClient.clientPokedexData.getHighestKnowledgeForSpecies(
                        resolvedPokemon.getApparentSpecies().resourceIdentifier
                    ) == PokedexEntryProgress.CAUGHT

                val hasSlowpokeTailDisplay = focusedSpeciesId == "cobblemon:slowpoke"
                val hasPrimaryDisplay =
                    hasSlowpokeTailDisplay || VariantLabelResolver.hasDisplay(resolvedPokemon, liveAspects)
                val frameCount =
                    2 +
                        (if (isPokemonInFocusOwned) 1 else 0) +
                        (if (hasPrimaryDisplay) 1 else 0)

                assignRandomInfoFrames(frameCount)

                newPokemonInfo = CobblemonClient.clientPokedexData.getNewInformation(resolvedPokemon)
                if (newPokemonInfo == PokedexLearnedInformation.NONE) {
                    playSound(CobblemonSounds.POKEDEX_SCAN_DETAIL)
                } else {
                    scannedSpecies = resolvedPokemon.getApparentSpecies().resourceIdentifier
                }
            } else {
                NonPokemonEntityScanState.clearActive()
                variantCycleTicksBetterPokedexScanner += 1F
            }
        } else {
            val beehiveScan = BeehiveScanner.findBeehiveTarget(user)

            if (beehiveScan != null) {
                EnvironmentalScanState.suspendForTarget()

                val isNewBeehiveTarget =
                    !BeehiveScanState.active ||
                        BeehiveScanState.blockPos != beehiveScan.blockPos

                if (isNewBeehiveTarget) {
                    resetFocusedPokemonState()
                    scannableEntityInFocus = null
                    variantCycleTicksBetterPokedexScanner = 0F
                    resetCarouselScheduler()

                    SlowpokeTailRegrowthClientState.clear()
                    RestorationTankScanState.beginClosing()
                    RestorationTankRegistrationState.clear()
                    NonPokemonEntityScanState.clearActive()

                    BeehiveScanState.set(beehiveScan)
                    ClientPlayNetworking.send(BeehiveDetailsRequestPayload(beehiveScan.blockPos))
                    BeehiveScanState.markDetailsRequestSent()

                    isPokemonInFocusOwned = false
                    assignRandomInfoFrames(beehiveScan.frameDisplays.size)

                    newPokemonInfo = PokedexLearnedInformation.NONE

                    playSound(CobblemonSounds.POKEDEX_SCAN_DETAIL)
                } else {
                    variantCycleTicksBetterPokedexScanner += 1F
                    val activeBeehiveFrameCount =
                        (BeehiveScanState.snapshot ?: beehiveScan).frameDisplays.size
                    ensureInfoFrameCount(activeBeehiveFrameCount)

                    if (!BeehiveScanState.detailsRequestSent) {
                        ClientPlayNetworking.send(BeehiveDetailsRequestPayload(beehiveScan.blockPos))
                        BeehiveScanState.markDetailsRequestSent()
                    }

                    if (BeehiveScanState.scanProgress < PokedexUsageContext.MAX_SCAN_PROGRESS) {
                        BeehiveScanState.scanProgress =
                            (BeehiveScanState.scanProgress + 2.5F)
                                .coerceAtMost(PokedexUsageContext.MAX_SCAN_PROGRESS.toFloat())

                        if (variantCycleTicksBetterPokedexScanner % 2F == 0F) {
                            playSound(CobblemonSounds.POKEDEX_SCAN_LOOP)
                        }

                        if (BeehiveScanState.scanProgress >= PokedexUsageContext.MAX_SCAN_PROGRESS) {
                            playSound(CobblemonSounds.POKEDEX_SCAN_REGISTER_ASPECT)
                        }
                    } else {
                        BeehiveScanState.focusIntervals =
                            (BeehiveScanState.focusIntervals + 2.5F)
                                .coerceAtMost(PokedexUsageContext.FOCUS_INTERVALS)

                        if (BeehiveScanState.focusIntervals >= PokedexUsageContext.FOCUS_INTERVALS) {
                            BeehiveScanState.focusIntervals = PokedexUsageContext.FOCUS_INTERVALS
                            val snapshot = BeehiveScanState.snapshot
                            ensureInfoFrameCount(snapshot?.frameDisplays?.size ?: beehiveScan.frameDisplays.size)
                            val framePageCounts = snapshot?.frameDisplays?.map { it.size } ?: emptyList()
                            val rarePageCounts =
                                if (snapshot == null) {
                                    emptyList()
                                } else {
                                    List(4) { displayIndex ->
                                        if (
                                            snapshot.frameDisplays.getOrNull(displayIndex)
                                                ?.firstOrNull()
                                                ?.headerLabel == "Inhabitants"
                                        ) {
                                            snapshot.rareIndividualDisplays.size
                                        } else {
                                            0
                                        }
                                    }
                                }
                            tickCarouselScheduler(
                                framePageCounts + rarePageCounts
                            )
                        }
                    }
                }
            } else {
            val tankScan = PokemonBlockScanner.findTarget(user)

            if (tankScan != null) {
                EnvironmentalScanState.suspendForTarget()
                NonPokemonEntityScanState.clearActive()

                val isNewTankTarget =
                    !RestorationTankScanState.active ||
                            RestorationTankScanState.blockPos != tankScan.blockPos

                val knownKnowledge = CobblemonClient.clientPokedexData.getHighestKnowledgeForSpecies(tankScan.speciesId)
                val blockNewInformation =
                    tankScan.resolvedPokemon?.let {
                        CobblemonClient.clientPokedexData.getNewInformation(
                            com.cobblemon.mod.common.pokedex.scanner.PokedexEntityData(it, null)
                        )
                    }
                val needsRegistration =
                    when {
                        !tankScan.validSpecies -> false
                        blockNewInformation != null -> blockNewInformation != PokedexLearnedInformation.NONE
                        else -> knownKnowledge == PokedexEntryProgress.NONE
                    }
                val canShowTankTyping = knownKnowledge == PokedexEntryProgress.CAUGHT

                if (isNewTankTarget) {
                    resetFocusedPokemonState()
                    scannableEntityInFocus = null
                    variantCycleTicksBetterPokedexScanner = 0F
                    resetCarouselScheduler()

                    SlowpokeTailRegrowthClientState.clear()
                    BeehiveScanState.beginClosing()
                    RestorationTankRegistrationState.clear()

                    if (needsRegistration) {
                        RestorationTankRegistrationState.setTarget(tankScan.blockPos, tankScan.speciesId)
                    } else {
                        RestorationTankRegistrationState.clear()
                    }

                    RestorationTankScanState.set(
                        blockPos = tankScan.blockPos,
                        speciesId = tankScan.speciesId,
                        timeRemaining = tankScan.timeRemaining,
                        validSpecies = tankScan.validSpecies,
                        formName = tankScan.formName,
                        aspects = tankScan.aspects,
                        shiny = tankScan.shiny,
                        scanKind = tankScan.scanKind,
                        level = tankScan.level,
                        containerName = tankScan.containerName,
                        gender = tankScan.gender,
                        typeLabel = tankScan.typeLabel,
                        typeLabelComponent = tankScan.typeLabelComponent,
                        extraMirroredDisplays = tankScan.extraMirroredDisplays
                    )

                    RestorationTankScanState.focusIntervals = 0F

                    isPokemonInFocusOwned = false
                    val snapshot = PokemonBlockScanPresentation.build(canShowTankTyping)
                    if (snapshot != null) {
                        assignRandomInfoFrames(snapshot.frameCount)
                    } else {
                        assignRandomInfoFrames(if (canShowTankTyping) 4 else 3)
                    }

                    newPokemonInfo = PokedexLearnedInformation.NONE

                    playSound(CobblemonSounds.POKEDEX_SCAN_DETAIL)
                } else {
                    variantCycleTicksBetterPokedexScanner += 1F

                    if (!RestorationTankRegistrationState.active || RestorationTankRegistrationState.registerCompleted) {
                        RestorationTankScanState.focusIntervals =
                            (RestorationTankScanState.focusIntervals + 2.5F)
                                .coerceAtMost(PokedexUsageContext.FOCUS_INTERVALS)
                    }

                    if (RestorationTankRegistrationState.active &&
                        RestorationTankRegistrationState.blockPos == tankScan.blockPos &&
                        RestorationTankRegistrationState.speciesId == tankScan.speciesId
                    ) {
                        if (!RestorationTankRegistrationState.startSent) {
                            ClientPlayNetworking.send(StartTankScanningPayload(tankScan.blockPos, tankScan.speciesId))
                            RestorationTankRegistrationState.startSent = true
                        }

                        if (!RestorationTankRegistrationState.awaitingServerConfirm &&
                            !RestorationTankRegistrationState.registerCompleted
                        ) {
                            RestorationTankRegistrationState.scanProgress =
                                (RestorationTankRegistrationState.scanProgress + 2.5F)
                                    .coerceAtMost(PokedexUsageContext.MAX_SCAN_PROGRESS.toFloat())

                            if (variantCycleTicksBetterPokedexScanner % 2F == 0F) {
                                playSound(CobblemonSounds.POKEDEX_SCAN_LOOP)
                            }

                            if (RestorationTankRegistrationState.scanProgress >= PokedexUsageContext.MAX_SCAN_PROGRESS) {
                                ClientPlayNetworking.send(FinishTankScanningPayload(tankScan.blockPos, tankScan.speciesId))
                                RestorationTankRegistrationState.awaitingServerConfirm = true
                            }
                        }
                    }

                    if (RestorationTankRegistrationState.registerCompleted) {
                        RestorationTankRegistrationState.displayRegisterInfoIntervals =
                            (RestorationTankRegistrationState.displayRegisterInfoIntervals + 1F)
                                .coerceAtMost(
                                    PokedexUsageContext.CENTER_INFO_DISPLAY_INTERVALS +
                                            PokedexUsageContext.CENTER_INFO_LINGER_INTERVALS
                                )

                        if (RestorationTankRegistrationState.displayRegisterInfoIntervals >=
                            PokedexUsageContext.CENTER_INFO_DISPLAY_INTERVALS +
                            PokedexUsageContext.CENTER_INFO_LINGER_INTERVALS
                        ) {
                            RestorationTankRegistrationState.clear()
                        }
                    } else if (RestorationTankScanState.focusIntervals >= PokedexUsageContext.FOCUS_INTERVALS) {
                        val snapshot = PokemonBlockScanPresentation.build(canShowTankTyping)
                        if (snapshot != null) {
                            ensureInfoFrameCount(snapshot.frameCount)
                            tickCarouselScheduler(snapshot.pageCounts)
                        }
                    }
                }
            } else {
                resetFocusedPokemonState()
                scannableEntityInFocus = null
                SlowpokeTailRegrowthClientState.clear()
                RestorationTankScanState.beginClosing()
                BeehiveScanState.beginClosing()
                NonPokemonEntityScanState.clearActive()
                RestorationTankRegistrationState.clear()

                val activationThreshold =
                    if (EnvironmentalScanState.reacquirePending) {
                        EnvironmentalScanState.REACQUIRE_IDLE_TICKS
                    } else {
                        EnvironmentalScanState.INITIAL_IDLE_TICKS
                    }

                if (!EnvironmentalScanState.active) {
                    EnvironmentalScanState.idleTicks =
                        (EnvironmentalScanState.idleTicks + 1F).coerceAtMost(activationThreshold)

                    if (EnvironmentalScanState.idleTicks >= activationThreshold) {
                        val snapshot = EnvironmentalScanner.sample(user)
                        assignRandomInfoFrames(snapshot.frameDisplays.size)

                        newPokemonInfo = PokedexLearnedInformation.NONE
                        isPokemonInFocusOwned = false
                        variantCycleTicksBetterPokedexScanner = 0F
                        resetCarouselScheduler()

                        if (EnvironmentalScanState.reacquirePending) {
                            EnvironmentalScanState.restoreSnapshot(snapshot)
                        } else {
                            EnvironmentalScanState.beginFreshScan(snapshot)
                        }

                        ClientPlayNetworking.send(
                            EnvironmentalStructureRequestPayload(EnvironmentalScanState.sessionId)
                        )
                        EnvironmentalScanState.markStructureRequestSent()

                        playSound(CobblemonSounds.POKEDEX_SCAN_DETAIL)
                    } else {
                        variantCycleTicksBetterPokedexScanner = 0F
                    }
                } else {
                    variantCycleTicksBetterPokedexScanner += 1F

                    if (!EnvironmentalScanState.structureRequestSent) {
                        ClientPlayNetworking.send(
                            EnvironmentalStructureRequestPayload(EnvironmentalScanState.sessionId)
                        )
                        EnvironmentalScanState.markStructureRequestSent()
                    }

                    if (!EnvironmentalScanState.completed && EnvironmentalScanState.scanProgress < PokedexUsageContext.MAX_SCAN_PROGRESS) {
                        EnvironmentalScanState.scanProgress =
                            (EnvironmentalScanState.scanProgress + 2.5F)
                                .coerceAtMost(PokedexUsageContext.MAX_SCAN_PROGRESS.toFloat())

                        if (variantCycleTicksBetterPokedexScanner % 2F == 0F) {
                            playSound(CobblemonSounds.POKEDEX_SCAN_LOOP)
                        }

                        if (EnvironmentalScanState.scanProgress >= PokedexUsageContext.MAX_SCAN_PROGRESS) {
                            EnvironmentalScanState.completed = true
                            playSound(CobblemonSounds.POKEDEX_SCAN_REGISTER_ASPECT)
                        }
                    } else {
                        EnvironmentalScanState.focusIntervals =
                            (EnvironmentalScanState.focusIntervals + 2.5F)
                                .coerceAtMost(PokedexUsageContext.FOCUS_INTERVALS)

                        if (EnvironmentalScanState.focusIntervals >= PokedexUsageContext.FOCUS_INTERVALS) {
                            EnvironmentalScanState.focusIntervals = PokedexUsageContext.FOCUS_INTERVALS
                            tickCarouselScheduler(
                                EnvironmentalScanState.snapshot?.frameDisplays?.map { it.size } ?: emptyList()
                            )
                        }
                    }
                }
            }
            }
        }
    }
}
