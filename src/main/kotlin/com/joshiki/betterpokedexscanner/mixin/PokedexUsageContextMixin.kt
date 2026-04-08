package com.joshiki.betterpokedexscanner.mixin

import com.cobblemon.mod.common.CobblemonSounds
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress
import com.cobblemon.mod.common.api.pokedex.PokedexLearnedInformation
import com.cobblemon.mod.common.client.CobblemonClient
import com.cobblemon.mod.common.pokedex.scanner.PokedexUsageContext
import com.cobblemon.mod.common.pokedex.scanner.PokemonScanner
import com.cobblemon.mod.common.pokedex.scanner.ScannableEntity
import com.joshiki.betterpokedexscanner.scanner.VariantCycleAccess
import com.joshiki.betterpokedexscanner.scanner.VariantLabelResolver
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
import com.joshiki.betterpokedexscanner.scanner.RestorationTankScanner
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import com.joshiki.betterpokedexscanner.client.RestorationTankRegistrationState
import com.joshiki.betterpokedexscanner.network.StartTankScanningPayload
import com.joshiki.betterpokedexscanner.network.FinishTankScanningPayload
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
    private var variantCycleTicksChimericShinies: Float = 0F

    @Shadow
    abstract fun resetFocusedPokemonState()

    @Shadow
    abstract fun playSound(sound: SoundEvent)

    override fun getVariantCycleTicksChimericShinies(): Float = variantCycleTicksChimericShinies


    override fun setVariantCycleTicksChimericShinies(value: Float) {
        variantCycleTicksChimericShinies = value
    }

    @Inject(method = ["stopUsing"], at = [At("HEAD")])
    private fun clearCustomScanStateOnStop(ticks: Int, sound: ResourceLocation?, ci: CallbackInfo) {
        RestorationTankScanState.clear()
        SlowpokeTailRegrowthClientState.clear()
        variantCycleTicksChimericShinies = 0F
        RestorationTankRegistrationState.clear()
    }

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

        val targetScannableEntity = PokemonScanner.findScannableEntity(user, zoomLevel.toInt())


        if (targetScannableEntity != null) {

            if (targetScannableEntity != scannableEntityInFocus) {
                resetFocusedPokemonState()
                scannableEntityInFocus = targetScannableEntity
                variantCycleTicksChimericShinies = 0F

                RestorationTankScanState.beginClosing()

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

                availableInfoFrames = mutableListOf(null, null, null, null)

                val hasSlowpokeTailDisplay = focusedSpeciesId == "cobblemon:slowpoke"

                val frameCount = if (
                    hasSlowpokeTailDisplay || VariantLabelResolver.hasDisplay(resolvedPokemon, liveAspects)
                ) 4 else 3

                for (i in 0 until frameCount) {
                    var randomIndex = Random.nextInt(availableInfoFrames.size)
                    if (availableInfoFrames[randomIndex] != null) {
                        randomIndex = availableInfoFrames.indexOfFirst { it == null }
                    }
                    availableInfoFrames[randomIndex] = Random.nextBoolean()
                }

                newPokemonInfo = CobblemonClient.clientPokedexData.getNewInformation(resolvedPokemon)
                if (newPokemonInfo == PokedexLearnedInformation.NONE) {
                    playSound(CobblemonSounds.POKEDEX_SCAN_DETAIL)
                } else {
                    scannedSpecies = resolvedPokemon.getApparentSpecies().resourceIdentifier
                }
            } else {
                variantCycleTicksChimericShinies += 1F
            }
        } else {

            val hit = user.pick(zoomLevel.toDouble(), 0f, false)

            if (hit is net.minecraft.world.phys.BlockHitResult) {
                var debugPos = hit.blockPos
                var debugState = user.level().getBlockState(debugPos)

                if (debugState.block == com.cobblemon.mod.common.CobblemonBlocks.RESTORATION_TANK) {
                    if (debugState.getValue(com.cobblemon.mod.common.block.RestorationTankBlock.PART) ==
                        com.cobblemon.mod.common.block.RestorationTankBlock.TankPart.TOP
                    ) {
                        debugPos = debugPos.below()
                        debugState = user.level().getBlockState(debugPos)
                    }

                    println("BPS raw tank hit at $debugPos")
                    playSound(CobblemonSounds.POKEDEX_SCAN_DETAIL)
                }
            }

            val tankScan = RestorationTankScanner.findRestorationTankTarget(user)

            if (tankScan != null) {
                println("BPS tank scan resolved: ${tankScan.speciesId} at ${tankScan.blockPos} time=${tankScan.timeRemaining}")

                val isNewTankTarget =
                    !RestorationTankScanState.active ||
                            RestorationTankScanState.blockPos != tankScan.blockPos

                val knownKnowledge = CobblemonClient.clientPokedexData.getHighestKnowledgeForSpecies(tankScan.speciesId)
                val speciesUnknown = knownKnowledge == PokedexEntryProgress.NONE

                if (isNewTankTarget) {
                    resetFocusedPokemonState()
                    scannableEntityInFocus = null
                    variantCycleTicksChimericShinies = 0F

                    SlowpokeTailRegrowthClientState.clear()
                    RestorationTankRegistrationState.clear()

                    if (speciesUnknown && tankScan.validSpecies) {
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
                        shiny = tankScan.shiny
                    )

                    RestorationTankScanState.focusIntervals = 0F

                    isPokemonInFocusOwned = false
                    availableInfoFrames = mutableListOf(null, null, null, null)

                    val frameCount = 4
                    for (i in 0 until frameCount) {
                        var randomIndex = Random.nextInt(availableInfoFrames.size)
                        if (availableInfoFrames[randomIndex] != null) {
                            randomIndex = availableInfoFrames.indexOfFirst { it == null }
                        }
                        availableInfoFrames[randomIndex] = Random.nextBoolean()
                    }

                    newPokemonInfo = PokedexLearnedInformation.NONE

                    playSound(CobblemonSounds.POKEDEX_SCAN_DETAIL)
                } else {
                    variantCycleTicksChimericShinies += 1F

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

                            if (variantCycleTicksChimericShinies % 2F == 0F) {
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
                    }
                }
            } else {
                resetFocusedPokemonState()
                scannableEntityInFocus = null
                variantCycleTicksChimericShinies = 0F
                SlowpokeTailRegrowthClientState.clear()
                RestorationTankScanState.beginClosing()
                RestorationTankRegistrationState.clear()
            }
        }
    }
}