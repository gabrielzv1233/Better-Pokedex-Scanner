package com.joshiki.betterpokedexscanner.mixin

import com.cobblemon.mod.common.api.gui.blitk
import com.cobblemon.mod.common.util.lang
import com.cobblemon.mod.common.api.text.bold
import com.cobblemon.mod.common.api.text.font
import com.cobblemon.mod.common.api.text.plus
import com.cobblemon.mod.common.api.text.text
import com.cobblemon.mod.common.client.CobblemonResources
import com.cobblemon.mod.common.client.gui.battle.BattleOverlay
import com.cobblemon.mod.common.client.pokedex.PokedexScannerRenderer
import com.cobblemon.mod.common.client.render.drawScaledText
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokedex.scanner.PokedexUsageContext
import com.cobblemon.mod.common.pokemon.Gender
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.Overwrite
import kotlin.math.ceil
import com.joshiki.betterpokedexscanner.scanner.VariantLabelResolver
import net.minecraft.resources.ResourceLocation
import com.joshiki.betterpokedexscanner.client.ScannerRenderConstants
import com.joshiki.betterpokedexscanner.scanner.VariantCycleAccess
import kotlin.math.roundToInt
import com.joshiki.betterpokedexscanner.client.SlowpokeTailRegrowthClientState
import com.joshiki.betterpokedexscanner.scanner.VariantDisplay
import com.joshiki.betterpokedexscanner.client.RestorationTankScanState
import com.joshiki.betterpokedexscanner.client.EnvironmentalScanState
import com.joshiki.betterpokedexscanner.client.BeehiveScanState
import com.joshiki.betterpokedexscanner.client.NonPokemonEntityScanState
import com.joshiki.betterpokedexscanner.client.RestorationTankRegistrationState
import com.cobblemon.mod.common.client.CobblemonClient
import com.mojang.blaze3d.systems.RenderSystem
import org.joml.Quaternionf
import org.spongepowered.asm.mixin.Shadow
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import kotlin.math.max
import kotlin.math.min
import kotlin.math.floor
import com.cobblemon.mod.common.api.pokedex.PokedexLearnedInformation
import com.joshiki.betterpokedexscanner.scanner.VariantRenderState
import com.joshiki.betterpokedexscanner.scanner.EnvironmentalScanSnapshot
import com.joshiki.betterpokedexscanner.scanner.BeehiveScanSnapshot
import com.joshiki.betterpokedexscanner.scanner.NonPokemonEntityScanSnapshot
import com.joshiki.betterpokedexscanner.scanner.PokemonBlockScanPresentation
import com.joshiki.betterpokedexscanner.scanner.PokemonBlockScanSnapshot


@Mixin(PokedexScannerRenderer::class)
abstract class PokedexScannerRendererMixin {

    @Shadow
    abstract fun getRegisterText(info: PokedexLearnedInformation): MutableComponent

    @Overwrite
    fun renderScanRings(
        poseStack: PoseStack,
        usageContext: PokedexUsageContext,
        centerX: Int,
        centerY: Int,
        opacity: Float
    ) {
        val rotation = usageContext.usageIntervals % 360

        val tankScanProgress =
            if (
                RestorationTankRegistrationState.active &&
                !RestorationTankRegistrationState.registerCompleted
            ) {
                RestorationTankRegistrationState.scanProgress
            } else {
                0F
            }

        val environmentScanProgress =
            if (
                EnvironmentalScanState.active &&
                !EnvironmentalScanState.completed
            ) {
                EnvironmentalScanState.scanProgress
            } else {
                0F
            }

        val beehiveScanProgress =
            if (BeehiveScanState.active) {
                BeehiveScanState.scanProgress
            } else {
                0F
            }

        val nonPokemonEntityScanProgress =
            if (NonPokemonEntityScanState.active && NonPokemonEntityScanState.focusIntervals <= 0F) {
                NonPokemonEntityScanState.scanProgress
            } else {
                0F
            }

        val effectiveScanningProgress = max(
            max(usageContext.scanningProgress, tankScanProgress),
            max(max(environmentScanProgress, beehiveScanProgress), nonPokemonEntityScanProgress)
        )

        poseStack.pushPose()
        poseStack.translate(centerX.toFloat(), centerY.toFloat(), 0.0f)

        poseStack.pushPose()
        poseStack.mulPose(Quaternionf().rotateZ(Math.toRadians((-rotation) * 0.5).toFloat()))
        blitk(
            matrixStack = poseStack,
            texture = PokedexScannerRenderer.SCAN_RING_OUTER,
            x = -(PokedexScannerRenderer.SCAN_RING_OUTER_DIAMETER / 2),
            y = -(PokedexScannerRenderer.SCAN_RING_OUTER_DIAMETER / 2),
            width = PokedexScannerRenderer.SCAN_RING_OUTER_DIAMETER,
            height = PokedexScannerRenderer.SCAN_RING_OUTER_DIAMETER,
            alpha = opacity
        )
        poseStack.popPose()

        var progressOpacity = opacity
        var segments = 40
        if (effectiveScanningProgress > 0) {
            if (effectiveScanningProgress < 20) {
                progressOpacity -= effectiveScanningProgress * 0.05F
            } else {
                if (progressOpacity != opacity) progressOpacity = opacity
                segments = floor((effectiveScanningProgress - 20.0) / 2.0).toInt()
            }
        }

        for (i in 0 until segments) {
            val rotationQuaternion =
                Quaternionf().rotateZ(Math.toRadians((i * 4.5) + (rotation * 0.5)).toFloat())
            poseStack.pushPose()
            poseStack.mulPose(rotationQuaternion)
            blitk(
                matrixStack = poseStack,
                texture = PokedexScannerRenderer.SCAN_RING_MIDDLE,
                x = -(PokedexScannerRenderer.SCAN_RING_MIDDLE_WIDTH / 2),
                y = -(PokedexScannerRenderer.SCAN_RING_MIDDLE_HEIGHT.toFloat() / 2F),
                width = PokedexScannerRenderer.SCAN_RING_MIDDLE_WIDTH,
                height = PokedexScannerRenderer.SCAN_RING_MIDDLE_HEIGHT,
                alpha = progressOpacity
            )
            poseStack.popPose()
        }

        poseStack.pushPose()
        poseStack.mulPose(Quaternionf().rotateZ(Math.toRadians(-usageContext.innerRingRotation.toDouble()).toFloat()))
        blitk(
            matrixStack = poseStack,
            texture = PokedexScannerRenderer.SCAN_RING_INNER,
            x = -(PokedexScannerRenderer.SCAN_RING_INNER_DIAMETER / 2),
            y = -(PokedexScannerRenderer.SCAN_RING_INNER_DIAMETER / 2),
            width = PokedexScannerRenderer.SCAN_RING_INNER_DIAMETER,
            height = PokedexScannerRenderer.SCAN_RING_INNER_DIAMETER,
            alpha = opacity
        )
        poseStack.popPose()

        poseStack.popPose()
    }

    @Suppress("UNUSED_PARAMETER")
    @Inject(method = ["renderScanOverlay"], at = [At("TAIL")])
    private fun renderTankRegistrationOverlay(
        graphics: GuiGraphics,
        tickDelta: Float,
        ci: CallbackInfo
    ) {
        val usageContext = CobblemonClient.pokedexUsageContext

        val showTankUnknown =
            RestorationTankRegistrationState.active &&
                    !RestorationTankRegistrationState.registerCompleted &&
                    RestorationTankRegistrationState.scanProgress > 0F

        val showTankRegister =
            RestorationTankRegistrationState.registerCompleted &&
                    RestorationTankRegistrationState.displayRegisterInfoIntervals > 0F

        val showEnvironmentUnknown =
            EnvironmentalScanState.active &&
                    !EnvironmentalScanState.completed &&
                    EnvironmentalScanState.scanProgress > 0F

        val showBeehiveUnknown =
            BeehiveScanState.active &&
                    BeehiveScanState.scanProgress > 0F &&
                    BeehiveScanState.focusIntervals <= 0F

        val showNonPokemonEntityUnknown =
            NonPokemonEntityScanState.active &&
                    NonPokemonEntityScanState.scanProgress > 0F &&
                    NonPokemonEntityScanState.focusIntervals <= 0F

        if (!showTankUnknown && !showTankRegister && !showEnvironmentUnknown && !showBeehiveUnknown && !showNonPokemonEntityUnknown) return

        val screenWidth = Minecraft.getInstance().window.guiScaledWidth
        val screenHeight = Minecraft.getInstance().window.guiScaledHeight
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        val matrices = graphics.pose()

        val opacity =
            if (usageContext.transitionIntervals >= 10) 1F
            else usageContext.transitionIntervals / 10F

        RenderSystem.enableBlend()

        if (showTankRegister) {
            blitk(
                matrixStack = matrices,
                texture = PokedexScannerRenderer.CENTER_INFO_FRAME,
                x = centerX - (PokedexScannerRenderer.CENTER_INFO_FRAME_WIDTH / 2),
                y = centerY - (PokedexScannerRenderer.CENTER_INFO_FRAME_HEIGHT / 2),
                width = PokedexScannerRenderer.CENTER_INFO_FRAME_WIDTH,
                height = PokedexScannerRenderer.CENTER_INFO_FRAME_HEIGHT,
                textureHeight = PokedexScannerRenderer.CENTER_INFO_FRAME_HEIGHT * 6,
                vOffset = min(
                    ceil(RestorationTankRegistrationState.displayRegisterInfoIntervals).toInt(),
                    PokedexUsageContext.CENTER_INFO_DISPLAY_INTERVALS.toInt()
                ) * PokedexScannerRenderer.CENTER_INFO_FRAME_HEIGHT,
                alpha = opacity
            )

            if (RestorationTankRegistrationState.displayRegisterInfoIntervals >=
                PokedexUsageContext.CENTER_INFO_DISPLAY_INTERVALS
            ) {
                drawScaledText(
                    context = graphics,
                    font = CobblemonResources.DEFAULT_LARGE,
                    text = getRegisterText(RestorationTankRegistrationState.newInformation),
                    x = centerX,
                    y = centerY - (PokedexScannerRenderer.CENTER_INFO_FRAME_HEIGHT / 2) + 4,
                    shadow = true,
                    centered = true
                )
            }
        } else {
            val progress =
                if (showTankUnknown) RestorationTankRegistrationState.scanProgress
                else if (showEnvironmentUnknown) EnvironmentalScanState.scanProgress
                else if (showBeehiveUnknown) BeehiveScanState.scanProgress
                else NonPokemonEntityScanState.scanProgress
            val unknownTexture =
                if (showEnvironmentUnknown) ScannerRenderConstants.ENVIRONMENT_SCAN_MARK
                else PokedexScannerRenderer.UNKNOWN_MARK
            val unknownWidth =
                if (showEnvironmentUnknown) ScannerRenderConstants.ENVIRONMENT_SCAN_MARK_WIDTH
                else PokedexScannerRenderer.UNKNOWN_MARK_WIDTH
            val unknownHeight =
                if (showEnvironmentUnknown) ScannerRenderConstants.ENVIRONMENT_SCAN_MARK_HEIGHT
                else PokedexScannerRenderer.UNKNOWN_MARK_HEIGHT

            val centerOpacity =
                if (progress > (PokedexUsageContext.MAX_SCAN_PROGRESS - 10))
                    (PokedexUsageContext.MAX_SCAN_PROGRESS - progress) * 0.1F
                else
                    progress * 0.1F

            blitk(
                matrixStack = matrices,
                texture = unknownTexture,
                x = centerX - (unknownWidth / 2),
                y = centerY - (unknownHeight / 2) + 2,
                width = unknownWidth,
                height = unknownHeight,
                alpha = max(0F, min(opacity, centerOpacity))
            )

            matrices.pushPose()
            matrices.translate(centerX.toFloat(), centerY.toFloat(), 0.0f)

            matrices.pushPose()
            matrices.mulPose(Quaternionf().rotateZ(Math.toRadians(usageContext.innerRingRotation * 0.5).toFloat()))
            blitk(
                matrixStack = matrices,
                texture = PokedexScannerRenderer.POINTER,
                x = -PokedexScannerRenderer.POINTER_WIDTH - PokedexScannerRenderer.POINTER_OFFSET,
                y = -(PokedexScannerRenderer.POINTER_HEIGHT / 2),
                width = PokedexScannerRenderer.POINTER_WIDTH,
                height = PokedexScannerRenderer.POINTER_HEIGHT,
                textureWidth = PokedexScannerRenderer.POINTER_WIDTH * 2,
                alpha = max(0F, min(opacity, centerOpacity))
            )

            blitk(
                matrixStack = matrices,
                texture = PokedexScannerRenderer.POINTER,
                x = PokedexScannerRenderer.POINTER_OFFSET,
                y = -(PokedexScannerRenderer.POINTER_HEIGHT / 2),
                width = PokedexScannerRenderer.POINTER_WIDTH,
                height = PokedexScannerRenderer.POINTER_HEIGHT,
                textureWidth = PokedexScannerRenderer.POINTER_WIDTH * 2,
                uOffset = PokedexScannerRenderer.POINTER_WIDTH,
                alpha = max(0F, min(opacity, centerOpacity))
            )
            matrices.popPose()
            matrices.popPose()
        }

        RenderSystem.disableBlend()
    }

    private fun interpolateRedToGreen(percent: Int): Int {
        val clamped = percent.coerceIn(0, 100)
        val red = (255 * (100 - clamped) / 100)
        val green = (255 * clamped / 100)
        val blue = 0
        return (red shl 16) or (green shl 8) or blue
    }

    private fun interpolateCyanToGreen(percent: Int): Int {
        val clamped = percent.coerceIn(0, 100)

        // Start: pale cyan  (160, 255, 255)
        // End:   green      (  0, 255,   0)
        val red = 160 - (160 * clamped / 100)
        val green = 255
        val blue = 255 - (255 * clamped / 100)

        return (red shl 16) or (green shl 8) or blue
    }


    /**
     * Adds a 4th info payload for non-default forms.
     */
    @Overwrite
    fun renderInfoFrames(
        graphics: GuiGraphics,
        poseStack: PoseStack,
        usageContext: PokedexUsageContext,
        centerX: Int,
        centerY: Int,
        opacity: Float
    ) {
        val tankRegistrationActive =
            RestorationTankRegistrationState.active &&
                    RestorationTankRegistrationState.speciesId != null &&
                    !RestorationTankRegistrationState.registerCompleted

        val tankActive =
            usageContext.scannableEntityInFocus == null &&
                    RestorationTankScanState.active &&
                    RestorationTankScanState.speciesId != null &&
                    RestorationTankScanState.focusIntervals > 0F

        val beehiveActive =
            usageContext.scannableEntityInFocus == null &&
                    !tankRegistrationActive &&
                    !tankActive &&
                    BeehiveScanState.active &&
                    BeehiveScanState.snapshot != null &&
                    BeehiveScanState.focusIntervals > 0F

        val environmentActive =
            usageContext.scannableEntityInFocus == null &&
                    !tankRegistrationActive &&
                    !tankActive &&
                    !beehiveActive &&
                    EnvironmentalScanState.active &&
                    EnvironmentalScanState.snapshot != null &&
                    EnvironmentalScanState.focusIntervals > 0F

        val nonPokemonEntityActive =
            usageContext.scannableEntityInFocus == null &&
                    !tankRegistrationActive &&
                    !tankActive &&
                    !beehiveActive &&
                    !environmentActive &&
                    NonPokemonEntityScanState.active &&
                    NonPokemonEntityScanState.snapshot != null &&
                    NonPokemonEntityScanState.focusIntervals > 0F

        if (tankRegistrationActive && usageContext.scannableEntityInFocus == null) {
            return
        }

        val effectiveFocusIntervals =
            if (environmentActive) EnvironmentalScanState.focusIntervals
            else if (nonPokemonEntityActive) NonPokemonEntityScanState.focusIntervals
            else if (beehiveActive) BeehiveScanState.focusIntervals
            else if (tankActive) RestorationTankScanState.focusIntervals
            else usageContext.focusIntervals

        if (effectiveFocusIntervals <= 0) return

        val tankTypeVisible = tankActive && isTankTypingVisible()
        val pokemonBlockSnapshot =
            if (tankActive) PokemonBlockScanPresentation.build(tankTypeVisible) else null
        val tankPrimaryDisplayCounter = if (tankTypeVisible) 4 else 3
        val pokemonActive =
            usageContext.scannableEntityInFocus != null &&
                !tankRegistrationActive
        val pokemonTypeVisible = pokemonActive && usageContext.isPokemonInFocusOwned
        val pokemonPrimaryDisplayCounter = if (pokemonTypeVisible) 4 else 3

        var infoDisplayedCounter = 0


        usageContext.availableInfoFrames.forEachIndexed { index, isLeftSide ->
            if (isLeftSide == null) return@forEachIndexed


            infoDisplayedCounter++
            val pokemonBlockDisplayIndex =
                if (tankActive) {
                    pokemonBlockSnapshot?.populatedDisplayIndices?.getOrNull(infoDisplayedCounter - 1)
                        ?: return@forEachIndexed
                } else {
                    index
                }
            val beehiveDisplayIndex = if (beehiveActive) infoDisplayedCounter - 1 else index
            val nonPokemonEntityDisplayIndex = if (nonPokemonEntityActive) infoDisplayedCounter - 1 else index

            val isInnerFrame = index == 1 || index == 2
            val frameHeight =
                if (isInnerFrame) PokedexScannerRenderer.INNER_INFO_FRAME_HEIGHT else PokedexScannerRenderer.OUTER_INFO_FRAME_HEIGHT
            val xOffset = (if (isInnerFrame) -177 else -120) + if (isLeftSide) 0 else if (isInnerFrame) 234 else 148
            val yOffset = when (index) {
                0 -> -80
                1 -> -26
                2 -> 6
                3 -> 25
                else -> 0
            }

            val shouldUseAnimatedPrimaryFrame =
                effectiveFocusIntervals >= PokedexUsageContext.FOCUS_INTERVALS &&
                        run {
                            if (environmentActive) {
                                getEnvironmentalDisplaysForFrame(
                                    EnvironmentalScanState.snapshot,
                                    index
                                ).size > 1
                            } else if (nonPokemonEntityActive) {
                                getNonPokemonEntityDisplaysForFrame(
                                    NonPokemonEntityScanState.snapshot,
                                    nonPokemonEntityDisplayIndex
                                ).size > 1
                            } else if (beehiveActive) {
                                getBeehiveDisplaysForFrame(
                                    BeehiveScanState.snapshot,
                                    beehiveDisplayIndex
                                ).size > 1
                            } else if (tankActive && !tankRegistrationActive && infoDisplayedCounter == tankPrimaryDisplayCounter) {
                                buildTankPrimaryRenderState().primaryDisplays.size > 1
                            } else if (usageContext.scannableEntityInFocus != null && infoDisplayedCounter == pokemonPrimaryDisplayCounter) {
                                val scanData = usageContext.scannableEntityInFocus?.resolvePokemonScan()
                                val focusedEntity = usageContext.scannableEntityInFocus?.resolveEntityScan() as? PokemonEntity

                                if (scanData == null) {
                                    false
                                } else {
                                    buildPokemonPrimaryRenderState(scanData, focusedEntity).primaryDisplays.size > 1
                                }
                            } else {
                                false
                            }
                        }

            val shouldUseAnimatedBlockFrame =
                effectiveFocusIntervals >= PokedexUsageContext.FOCUS_INTERVALS &&
                    tankActive &&
                    !tankRegistrationActive &&
                    run {
                        val snapshot = pokemonBlockSnapshot ?: return@run false

                        if (snapshot.primaryDisplayIndex == pokemonBlockDisplayIndex) {
                            snapshot.primaryRenderState.primaryDisplays.size > 1
                        } else {
                            snapshot.frameDisplays.getOrNull(pokemonBlockDisplayIndex)?.size ?: 0 > 1
                        }
                    }

            if (!shouldUseAnimatedPrimaryFrame && !shouldUseAnimatedBlockFrame) {
                blitk(
                    matrixStack = poseStack,
                    texture = PokedexScannerRenderer.infoFrameResource(isLeftSide, index),
                    x = centerX + xOffset,
                    y = centerY + yOffset,
                    width = if (isInnerFrame) PokedexScannerRenderer.INNER_INFO_FRAME_WIDTH else PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH,
                    height = frameHeight,
                    textureHeight = frameHeight * 10,
                    vOffset = ceil(effectiveFocusIntervals) * frameHeight,
                    alpha = opacity
                )
            }

            val xOffsetText = if (isInnerFrame) {
                ((PokedexScannerRenderer.INNER_INFO_FRAME_WIDTH - PokedexScannerRenderer.INNER_INFO_FRAME_STEM_WIDTH) / 2) +
                        if (isLeftSide) 0 else PokedexScannerRenderer.INNER_INFO_FRAME_STEM_WIDTH
            } else {
                PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH / 2
            }

            val yOffsetText = when (index) {
                0 -> 5
                1 -> 4
                2 -> 8
                3 -> 42
                else -> 0
            }

            if (effectiveFocusIntervals >= PokedexUsageContext.FOCUS_INTERVALS &&
                (usageContext.scannableEntityInFocus != null || tankActive || beehiveActive || environmentActive || nonPokemonEntityActive)
            ) {
                val pokedexEntityData = usageContext.scannableEntityInFocus?.resolvePokemonScan()
                val environmentalSnapshot = EnvironmentalScanState.snapshot
                val beehiveSnapshot = BeehiveScanState.snapshot
                val nonPokemonEntitySnapshot = NonPokemonEntityScanState.snapshot

                if (beehiveActive) {
                    val snapshot = beehiveSnapshot ?: return@forEachIndexed
                    renderBeehiveInfoFrame(
                        graphics = graphics,
                        poseStack = poseStack,
                        usageContext = usageContext,
                        snapshot = snapshot,
                        displayIndex = beehiveDisplayIndex,
                        slotIndex = index,
                        isLeftSide = isLeftSide,
                        centerX = centerX,
                        centerY = centerY,
                        xOffset = xOffset,
                        yOffset = yOffset,
                        xOffsetText = xOffsetText,
                        frameHeight = frameHeight,
                        effectiveFocusIntervals = effectiveFocusIntervals,
                        opacity = opacity
                    )
                    return@forEachIndexed
                }

                if (nonPokemonEntityActive) {
                    val snapshot = nonPokemonEntitySnapshot ?: return@forEachIndexed
                    renderNonPokemonEntityInfoFrame(
                        graphics = graphics,
                        poseStack = poseStack,
                        usageContext = usageContext,
                        snapshot = snapshot,
                        displayIndex = nonPokemonEntityDisplayIndex,
                        slotIndex = index,
                        isLeftSide = isLeftSide,
                        centerX = centerX,
                        centerY = centerY,
                        xOffset = xOffset,
                        yOffset = yOffset,
                        xOffsetText = xOffsetText,
                        frameHeight = frameHeight,
                        effectiveFocusIntervals = effectiveFocusIntervals,
                        opacity = opacity
                    )

                    renderNonPokemonEntityMirroredFrames(
                        graphics = graphics,
                        poseStack = poseStack,
                        usageContext = usageContext,
                        snapshot = snapshot,
                        displayIndex = nonPokemonEntityDisplayIndex,
                        slotIndex = index,
                        isLeftSide = isLeftSide,
                        centerX = centerX,
                        centerY = centerY,
                        yOffset = yOffset,
                        frameHeight = frameHeight,
                        effectiveFocusIntervals = effectiveFocusIntervals,
                        opacity = opacity
                    )
                    return@forEachIndexed
                }

                if (infoDisplayedCounter == 1) {
                    if (environmentActive) {
                        val snapshot = environmentalSnapshot ?: return@forEachIndexed
                        renderEnvironmentalInfoFrame(
                            graphics = graphics,
                            poseStack = poseStack,
                            usageContext = usageContext,
                            snapshot = snapshot,
                            index = index,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            xOffset = xOffset,
                            yOffset = yOffset,
                            xOffsetText = xOffsetText,
                            frameHeight = frameHeight,
                            effectiveFocusIntervals = effectiveFocusIntervals,
                            opacity = opacity
                        )
                    } else if (tankActive && !tankRegistrationActive) {
                        val snapshot = pokemonBlockSnapshot ?: return@forEachIndexed
                        renderPokemonBlockInfoFrame(
                            graphics = graphics,
                            poseStack = poseStack,
                            usageContext = usageContext,
                            snapshot = snapshot,
                            displayIndex = pokemonBlockDisplayIndex,
                            slotIndex = index,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            xOffset = xOffset,
                            yOffset = yOffset,
                            xOffsetText = xOffsetText,
                            frameHeight = frameHeight,
                            effectiveFocusIntervals = effectiveFocusIntervals,
                            opacity = opacity
                        )
                        renderPokemonBlockMirroredFrames(
                            graphics = graphics,
                            poseStack = poseStack,
                            usageContext = usageContext,
                            snapshot = snapshot,
                            displayIndex = pokemonBlockDisplayIndex,
                            slotIndex = index,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            yOffset = yOffset,
                            frameHeight = frameHeight,
                            effectiveFocusIntervals = effectiveFocusIntervals,
                            opacity = opacity
                        )
                    } else {
                        val entityData = pokedexEntityData ?: return@forEachIndexed
                        drawScaledText(
                            context = graphics,
                            font = CobblemonResources.DEFAULT_LARGE,
                            text = lang("ui.lv.number", entityData.pokemon.level).bold(),
                            x = centerX + xOffset + xOffsetText,
                            y = centerY + yOffset + yOffsetText,
                            colour = com.joshiki.betterpokedexscanner.scanner.LevelThreatColorResolver.resolveColour(
                                level = entityData.pokemon.level,
                                containedInBall = false
                            ) ?: 0xFFFFFF,
                            shadow = true,
                            centered = true
                        )
                    }
                }

                if (infoDisplayedCounter == 2) {
                    if (environmentActive) {
                        val snapshot = environmentalSnapshot ?: return@forEachIndexed
                        renderEnvironmentalInfoFrame(
                            graphics = graphics,
                            poseStack = poseStack,
                            usageContext = usageContext,
                            snapshot = snapshot,
                            index = index,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            xOffset = xOffset,
                            yOffset = yOffset,
                            xOffsetText = xOffsetText,
                            frameHeight = frameHeight,
                            effectiveFocusIntervals = effectiveFocusIntervals,
                            opacity = opacity
                        )
                    } else if (tankActive && !tankRegistrationActive) {
                        val snapshot = pokemonBlockSnapshot ?: return@forEachIndexed
                        renderPokemonBlockInfoFrame(
                            graphics = graphics,
                            poseStack = poseStack,
                            usageContext = usageContext,
                            snapshot = snapshot,
                            displayIndex = pokemonBlockDisplayIndex,
                            slotIndex = index,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            xOffset = xOffset,
                            yOffset = yOffset,
                            xOffsetText = xOffsetText,
                            frameHeight = frameHeight,
                            effectiveFocusIntervals = effectiveFocusIntervals,
                            opacity = opacity
                        )
                        renderPokemonBlockMirroredFrames(
                            graphics = graphics,
                            poseStack = poseStack,
                            usageContext = usageContext,
                            snapshot = snapshot,
                            displayIndex = pokemonBlockDisplayIndex,
                            slotIndex = index,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            yOffset = yOffset,
                            frameHeight = frameHeight,
                            effectiveFocusIntervals = effectiveFocusIntervals,
                            opacity = opacity
                        )
                    } else {
                        val entityData = pokedexEntityData ?: return@forEachIndexed
                        val hasTrainer =
                            (usageContext.scannableEntityInFocus?.resolveEntityScan() as? PokemonEntity)?.ownerUUID != null
                        val speciesName = entityData.getApparentSpecies().translatedName.bold()
                        val yOffsetName = if (hasTrainer) 2 else 0

                        if (hasTrainer) {
                            drawScaledText(
                                context = graphics,
                                text = lang("ui.pokedex.scan.trainer_owned"),
                                x = centerX + xOffset + xOffsetText,
                                y = centerY + yOffset + yOffsetText - yOffsetName,
                                shadow = true,
                                centered = true,
                                scale = 0.5F
                            )
                        }

                        drawScaledText(
                            context = graphics,
                            font = CobblemonResources.DEFAULT_LARGE,
                            text = speciesName,
                            x = centerX + xOffset + xOffsetText,
                            y = centerY + yOffset + yOffsetText + yOffsetName,
                            shadow = true,
                            centered = true
                        )

                        val gender = entityData.pokemon.gender
                        val speciesNameWidth =
                            Minecraft.getInstance().font.width(speciesName.font(CobblemonResources.DEFAULT_LARGE))
                        if (gender != Gender.GENDERLESS) {
                            val isMale = gender == Gender.MALE
                            val textSymbol = if (isMale) "♂".text().bold() else "♀".text().bold()
                            drawScaledText(
                                context = graphics,
                                font = CobblemonResources.DEFAULT_LARGE,
                                text = textSymbol,
                                x = centerX + xOffset + xOffsetText + 2 + (speciesNameWidth / 2),
                                y = centerY + yOffset + yOffsetText + yOffsetName,
                                colour = if (isMale) 0x32CBFF else 0xFC5454,
                                shadow = true
                            )
                        }

                        if (usageContext.isPokemonInFocusOwned) {
                            blitk(
                                matrixStack = poseStack,
                                texture = BattleOverlay.caughtIndicator,
                                x = (centerX + xOffset + xOffsetText - 7 - (speciesNameWidth / 2)) / BattleOverlay.SCALE,
                                y = (centerY + yOffset + yOffsetText + yOffsetName + 2) / BattleOverlay.SCALE,
                                height = 10,
                                width = 10,
                                scale = BattleOverlay.SCALE
                            )
                        }
                    }
                }

                if (infoDisplayedCounter == 3) {
                    if (environmentActive) {
                        val snapshot = environmentalSnapshot ?: return@forEachIndexed
                        renderEnvironmentalInfoFrame(
                            graphics = graphics,
                            poseStack = poseStack,
                            usageContext = usageContext,
                            snapshot = snapshot,
                            index = index,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            xOffset = xOffset,
                            yOffset = yOffset,
                            xOffsetText = xOffsetText,
                            frameHeight = frameHeight,
                            effectiveFocusIntervals = effectiveFocusIntervals,
                            opacity = opacity
                        )
                    } else if (tankActive && !tankRegistrationActive) {
                        val snapshot = pokemonBlockSnapshot ?: return@forEachIndexed
                        renderPokemonBlockInfoFrame(
                            graphics = graphics,
                            poseStack = poseStack,
                            usageContext = usageContext,
                            snapshot = snapshot,
                            displayIndex = pokemonBlockDisplayIndex,
                            slotIndex = index,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            xOffset = xOffset,
                            yOffset = yOffset,
                            xOffsetText = xOffsetText,
                            frameHeight = frameHeight,
                            effectiveFocusIntervals = effectiveFocusIntervals,
                            opacity = opacity
                        )
                        renderPokemonBlockMirroredFrames(
                            graphics = graphics,
                            poseStack = poseStack,
                            usageContext = usageContext,
                            snapshot = snapshot,
                            displayIndex = pokemonBlockDisplayIndex,
                            slotIndex = index,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            yOffset = yOffset,
                            frameHeight = frameHeight,
                            effectiveFocusIntervals = effectiveFocusIntervals,
                            opacity = opacity
                        )
                    } else {
                        val entityData = pokedexEntityData ?: return@forEachIndexed
                        if (pokemonTypeVisible) {
                            val typeTokens = entityData.getApparentForm().types.map {
                                com.joshiki.betterpokedexscanner.scanner.PokemonTypeLabelFormatter.TypeToken(
                                    it.displayName,
                                    it.name
                                )
                            }
                            val typeText = com.joshiki.betterpokedexscanner.scanner.PokemonTypeLabelFormatter
                                .buildTypeTokenComponent(typeTokens)

                            val typeWidth =
                                Minecraft.getInstance().font.width(typeText.font(CobblemonResources.DEFAULT_LARGE))

                            if (typeWidth > (PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH - 8) &&
                                entityData.getApparentForm().secondaryType != null
                            ) {
                                val splitLines = typeTokens

                                drawScaledText(
                                    context = graphics,
                                    font = CobblemonResources.DEFAULT_LARGE,
                                    text = com.joshiki.betterpokedexscanner.scanner.PokemonTypeLabelFormatter
                                        .buildTypeTokenNamesComponent(listOf(splitLines[0]))
                                        .append(Component.literal("/").withStyle { style -> style.withBold(true) }),
                                    x = centerX + xOffset + xOffsetText,
                                    y = centerY + yOffset + yOffsetText - 4,
                                    shadow = true,
                                    centered = true
                                )

                                drawScaledText(
                                    context = graphics,
                                    font = CobblemonResources.DEFAULT_LARGE,
                                    text = com.joshiki.betterpokedexscanner.scanner.PokemonTypeLabelFormatter
                                        .buildTypeTokenComponent(listOf(splitLines[1])),
                                    x = centerX + xOffset + xOffsetText,
                                    y = centerY + yOffset + yOffsetText + 3,
                                    shadow = true,
                                    centered = true
                                )
                            } else {
                                drawScaledText(
                                    context = graphics,
                                    font = CobblemonResources.DEFAULT_LARGE,
                                    text = typeText,
                                    x = centerX + xOffset + xOffsetText,
                                    y = centerY + yOffset + yOffsetText,
                                    shadow = true,
                                    centered = true
                                )
                            }
                        } else {
                            val focusedEntity = usageContext.scannableEntityInFocus?.resolveEntityScan() as? PokemonEntity
                            renderPrimaryDisplayFrame(
                                graphics = graphics,
                                poseStack = poseStack,
                                usageContext = usageContext,
                                renderState = buildPokemonPrimaryRenderState(entityData, focusedEntity),
                                index = index,
                                isLeftSide = isLeftSide,
                                centerX = centerX,
                                centerY = centerY,
                                xOffset = xOffset,
                                yOffset = yOffset,
                                xOffsetText = xOffsetText,
                                frameHeight = frameHeight,
                                effectiveFocusIntervals = effectiveFocusIntervals,
                                opacity = opacity
                            )
                        }
                    }
                }

                if (infoDisplayedCounter == 4) {
                    if (environmentActive) {
                        val snapshot = environmentalSnapshot ?: return@forEachIndexed
                        renderEnvironmentalInfoFrame(
                            graphics = graphics,
                            poseStack = poseStack,
                            usageContext = usageContext,
                            snapshot = snapshot,
                            index = index,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            xOffset = xOffset,
                            yOffset = yOffset,
                            xOffsetText = xOffsetText,
                            frameHeight = frameHeight,
                            effectiveFocusIntervals = effectiveFocusIntervals,
                            opacity = opacity
                        )
                        renderEnvironmentalStructureTag(
                            graphics = graphics,
                            poseStack = poseStack,
                            snapshot = snapshot,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            opacity = opacity,
                            effectiveFocusIntervals = effectiveFocusIntervals
                        )
                    } else if (tankActive && !tankRegistrationActive) {
                        val snapshot = pokemonBlockSnapshot ?: return@forEachIndexed
                        renderPokemonBlockInfoFrame(
                            graphics = graphics,
                            poseStack = poseStack,
                            usageContext = usageContext,
                            snapshot = snapshot,
                            displayIndex = pokemonBlockDisplayIndex,
                            slotIndex = index,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            xOffset = xOffset,
                            yOffset = yOffset,
                            xOffsetText = xOffsetText,
                            frameHeight = frameHeight,
                            effectiveFocusIntervals = effectiveFocusIntervals,
                            opacity = opacity
                        )
                        renderPokemonBlockMirroredFrames(
                            graphics = graphics,
                            poseStack = poseStack,
                            usageContext = usageContext,
                            snapshot = snapshot,
                            displayIndex = pokemonBlockDisplayIndex,
                            slotIndex = index,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            yOffset = yOffset,
                            frameHeight = frameHeight,
                            effectiveFocusIntervals = effectiveFocusIntervals,
                            opacity = opacity
                        )
                    } else {
                        val entityData = pokedexEntityData ?: return@forEachIndexed
                        val focusedEntity = usageContext.scannableEntityInFocus?.resolveEntityScan() as? PokemonEntity
                        val liveAspects = focusedEntity?.aspects
                        val renderState = VariantLabelResolver.resolveRenderState(entityData, liveAspects)

                        val serverDisplays = buildList {
                            val focusedEntityId = focusedEntity?.id
                            val speciesId = focusedEntity?.pokemon?.species?.resourceIdentifier?.toString()
                            val percent = SlowpokeTailRegrowthClientState.percent

                            if (
                                speciesId == "cobblemon:slowpoke" &&
                                focusedEntityId != null &&
                                SlowpokeTailRegrowthClientState.activeEntityId == focusedEntityId &&
                                percent != null
                            ) {
                                add(
                                    when {
                                        percent <= 0 -> VariantDisplay(
                                            headerTranslationKey = "betterpokedexscanner.header.tail_health",
                                            label = "Sheared",
                                            labelTranslationKey = "betterpokedexscanner.variant.sheared",
                                            showShinyIcon = false,
                                            needsSeparateShinyTag = false,
                                            colour = interpolateRedToGreen(percent)
                                        )

                                        percent >= 100 -> VariantDisplay(
                                            headerTranslationKey = "betterpokedexscanner.header.tail_health",
                                            label = "Fully Grown",
                                            labelTranslationKey = "betterpokedexscanner.variant.fully_grown",
                                            showShinyIcon = false,
                                            needsSeparateShinyTag = false,
                                            colour = interpolateRedToGreen(percent)
                                        )

                                        else -> VariantDisplay(
                                            headerTranslationKey = "betterpokedexscanner.header.tail_regrowth",
                                            label = "$percent%",
                                            labelTranslationKey = null,
                                            showShinyIcon = false,
                                            needsSeparateShinyTag = false,
                                            colour = interpolateRedToGreen(percent)
                                        )
                                    }
                                )
                            }
                        }

                        val primaryDisplays = (renderState.primaryDisplays + serverDisplays).distinctBy {
                            Triple(it.headerLabel, it.headerTranslationKey, it.label)
                        }

                        if (primaryDisplays.isNotEmpty()) {
                            val variantCycleAccess = usageContext as VariantCycleAccess
                            val variantCycleTicks = variantCycleAccess.getVariantCycleTicksBetterPokedexScanner()

                            val holdTicks = 80f
                            val transitionTicks = 10f
                            val totalTicksPerPage = holdTicks + transitionTicks

                            val cyclePosition = variantCycleTicks % totalTicksPerPage
                            val basePageIndex = ((variantCycleTicks / totalTicksPerPage).toInt()) % primaryDisplays.size
                            val nextPageIndex = (basePageIndex + 1) % primaryDisplays.size

                            val inTransition = primaryDisplays.size > 1 && cyclePosition >= holdTicks
                            val transitionProgress =
                                if (inTransition) ((cyclePosition - holdTicks) / transitionTicks).coerceIn(0f, 1f)
                                else 0f

                            val baseFrameIndex = ceil(effectiveFocusIntervals).toInt().coerceIn(0, 9)

                            val animatedPrimaryFrameIndex =
                                if (index == 3 && primaryDisplays.size > 1 && inTransition) {
                                    val collapseAmount =
                                        if (transitionProgress < 0.5f) {
                                            transitionProgress / 0.5f
                                        } else {
                                            (1f - transitionProgress) / 0.5f
                                        }

                                    val collapseSteps = (collapseAmount * 2f).roundToInt()
                                    (baseFrameIndex - collapseSteps).coerceIn(0, 9)
                                } else {
                                    baseFrameIndex
                                }

                            val animatedPrimaryFrameVOffset = animatedPrimaryFrameIndex * frameHeight

                            val variantDisplay =
                                if (inTransition && transitionProgress >= 0.5f) primaryDisplays[nextPageIndex]
                                else primaryDisplays[basePageIndex]
                            val variantText =
                                variantDisplay.labelComponent?.copy()
                                    ?: if (!variantDisplay.labelTranslationKey.isNullOrBlank()) {
                                        Component.translatable(variantDisplay.labelTranslationKey).copy().bold()
                                    } else {
                                        Component.literal(variantDisplay.label).copy().bold()
                                    }
                            val headerY = centerY + yOffset + 40
                            val valueY = centerY + yOffset + 44
                            val noHeaderValueY = centerY + yOffset + 42
                            val isShinyOnlyDisplay =
                                variantDisplay.showShinyIcon &&
                                    !variantDisplay.needsSeparateShinyTag &&
                                    variantDisplay.labelTranslationKey == "betterpokedexscanner.label.shiny"
                            val primaryValueY =
                                if (isShinyOnlyDisplay)
                                    noHeaderValueY
                                else
                                    valueY
                            val maxWidth = PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH - 8
                            val headerComponent = if (!variantDisplay.headerTranslationKey.isNullOrBlank()) {
                                Component.translatable(variantDisplay.headerTranslationKey)
                            } else {
                                variantDisplay.headerLabel?.let(Component::literal)
                                    ?: Component.translatable("cobblemon.pokedex.variation.variant")
                            }

                            if (index == 3 && primaryDisplays.size > 1) {
                                blitk(
                                    matrixStack = poseStack,
                                    texture = PokedexScannerRenderer.infoFrameResource(isLeftSide, index),
                                    x = centerX + xOffset,
                                    y = centerY + yOffset,
                                    width = PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH,
                                    height = PokedexScannerRenderer.OUTER_INFO_FRAME_HEIGHT,
                                    textureHeight = PokedexScannerRenderer.OUTER_INFO_FRAME_HEIGHT * 10,
                                    vOffset = animatedPrimaryFrameVOffset,
                                    alpha = opacity
                                )
                            }

                            val headerScale =
                                if (primaryDisplays.size > 1) {
                                    if (!inTransition) {
                                        0.5f
                                    } else {
                                        when {
                                            transitionProgress < 0.20f -> {
                                                val local = transitionProgress / 0.20f
                                                0.5f * (1f - local)
                                            }

                                            transitionProgress < 0.8f -> {
                                                0f
                                            }

                                            else -> {
                                                val local = (transitionProgress - 0.8f) / 0.3f
                                                0.5f * local
                                            }
                                        }
                                    }
                                } else {
                                    0.5f
                                }

                            // Primary frame header - skip for shiny-only display
                            if (!isShinyOnlyDisplay) {
                                drawScaledText(
                                    context = graphics,
                                    text = headerComponent,
                                    x = centerX + xOffset + xOffsetText,
                                    y = headerY,
                                    shadow = true,
                                    centered = true,
                                    scale = headerScale
                                )
                            }

                            val baseTextWidth =
                                Minecraft.getInstance().font.width(variantText.font(CobblemonResources.DEFAULT_LARGE))
                            val useScaledText = baseTextWidth > maxWidth
                            val textScale = if (useScaledText) 0.75F else 1.0F
                            val animatedTextScale =
                                if (!inTransition) {
                                    textScale
                                } else {
                                    when {
                                        transitionProgress < 0.20f -> {
                                            val local = transitionProgress / 0.20f
                                            textScale * (1f - local)
                                        }

                                        transitionProgress < 0.8f -> {
                                            0f
                                        }

                                        else -> {
                                            val local = (transitionProgress - 0.8f) / 0.3f
                                            textScale * local
                                        }
                                    }
                                }


                            val scaledTextWidth = (baseTextWidth * textScale).toInt()

                            val textCenterX = centerX + xOffset + xOffsetText
                            val textLeftX = textCenterX - (scaledTextWidth / 2)

                            // Primary frame icon only for shiny-only case
                            if (variantDisplay.showShinyIcon) {
                                val iconScale = ScannerRenderConstants.SHINY_ICON_SCALE
                                val iconVisualWidth = (ScannerRenderConstants.SHINY_ICON_SIZE * iconScale).toInt()
                                val iconSpacing = 2

                                blitk(
                                    matrixStack = poseStack,
                                    texture = ScannerRenderConstants.SHINY_ICON,
                                    x = (textLeftX - iconSpacing - iconVisualWidth) / iconScale,
                                    y = primaryValueY / iconScale,
                                    width = ScannerRenderConstants.SHINY_ICON_SIZE,
                                    height = ScannerRenderConstants.SHINY_ICON_SIZE,
                                    scale = iconScale
                                )
                            }

                            if (variantDisplay.labelComponent != null) {
                                drawScaledStyledText(
                                    graphics = graphics,
                                    text = variantText,
                                    x = textCenterX,
                                    y = primaryValueY.toFloat(),
                                    scale = animatedTextScale,
                                    shadow = true,
                                    centered = true
                                )
                            } else {
                                drawScaledText(
                                    context = graphics,
                                    font = CobblemonResources.DEFAULT_LARGE,
                                    text = variantText,
                                    x = textCenterX,
                                    y = primaryValueY,
                                    colour = variantDisplay.colour ?: 0xFFFFFF,
                                    shadow = true,
                                    centered = true,
                                    scale = animatedTextScale
                                )
                            }

                            // Fake 5th slot: mirrored shiny tag on the opposite side of slot 4
                            if (index == 3 && renderState.showSeparateShinyTag) {
                                val shinySide = !isLeftSide
                                val shinyXOffset = -120 + if (shinySide) 0 else 148
                                val shinyYOffset = 25
                                val shinyText = Component.translatable("betterpokedexscanner.label.shiny").copy().bold()
                                val shinyValueY = centerY + shinyYOffset + 42
                                val shinyTextCenterX =
                                    centerX + shinyXOffset + (PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH / 2)
                                val shinyTextWidth =
                                    Minecraft.getInstance().font.width(shinyText.font(CobblemonResources.DEFAULT_LARGE))
                                val shinyUseScaledText = shinyTextWidth > maxWidth
                                val shinyTextScale = if (shinyUseScaledText) 0.75F else 1.0F
                                val shinyScaledTextWidth = (shinyTextWidth * shinyTextScale).toInt()
                                val shinyTextLeftX = shinyTextCenterX - (shinyScaledTextWidth / 2)

                                blitk(
                                    matrixStack = poseStack,
                                    texture = PokedexScannerRenderer.infoFrameResource(shinySide, 3),
                                    x = centerX + shinyXOffset,
                                    y = centerY + shinyYOffset,
                                    width = PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH,
                                    height = PokedexScannerRenderer.OUTER_INFO_FRAME_HEIGHT,
                                    textureHeight = PokedexScannerRenderer.OUTER_INFO_FRAME_HEIGHT * 10,
                                    vOffset = ceil(effectiveFocusIntervals) * PokedexScannerRenderer.OUTER_INFO_FRAME_HEIGHT,
                                    alpha = opacity
                                )

                                val shinyIconScale = ScannerRenderConstants.SHINY_ICON_SCALE
                                val shinyIconVisualWidth =
                                    (ScannerRenderConstants.SHINY_ICON_SIZE * shinyIconScale).toInt()
                                val shinyIconSpacing = 2

                                blitk(
                                    matrixStack = poseStack,
                                    texture = ScannerRenderConstants.SHINY_ICON,
                                    x = (shinyTextLeftX - shinyIconSpacing - shinyIconVisualWidth) / shinyIconScale,
                                    y = shinyValueY / shinyIconScale,
                                    width = ScannerRenderConstants.SHINY_ICON_SIZE,
                                    height = ScannerRenderConstants.SHINY_ICON_SIZE,
                                    scale = shinyIconScale
                                )

                                drawScaledText(
                                    context = graphics,
                                    font = CobblemonResources.DEFAULT_LARGE,
                                    text = shinyText,
                                    x = shinyTextCenterX,
                                    y = shinyValueY,
                                    shadow = true,
                                    centered = true,
                                    scale = shinyTextScale
                                )
                            }

                        }
                    }
                }
            }
        }
    }

    private fun renderEnvironmentalInfoFrame(
        graphics: GuiGraphics,
        poseStack: PoseStack,
        usageContext: PokedexUsageContext,
        snapshot: EnvironmentalScanSnapshot,
        index: Int,
        isLeftSide: Boolean,
        centerX: Int,
        centerY: Int,
        xOffset: Int,
        yOffset: Int,
        xOffsetText: Int,
        frameHeight: Int,
        effectiveFocusIntervals: Float,
        opacity: Float
    ) {
        val displays = getEnvironmentalDisplaysForFrame(snapshot, index)
        if (displays.isEmpty()) return
        renderCyclingInfoDisplay(
            graphics = graphics,
            poseStack = poseStack,
            usageContext = usageContext,
            displays = displays,
            index = index,
            schedulerIndex = index,
            isLeftSide = isLeftSide,
            centerX = centerX,
            centerY = centerY,
            xOffset = xOffset,
            yOffset = yOffset,
            xOffsetText = xOffsetText,
            frameHeight = frameHeight,
            effectiveFocusIntervals = effectiveFocusIntervals,
            opacity = opacity,
            forceDrawFrame = false
        )
    }

    private fun renderNonPokemonEntityInfoFrame(
        graphics: GuiGraphics,
        poseStack: PoseStack,
        usageContext: PokedexUsageContext,
        snapshot: NonPokemonEntityScanSnapshot,
        displayIndex: Int,
        slotIndex: Int,
        isLeftSide: Boolean,
        centerX: Int,
        centerY: Int,
        xOffset: Int,
        yOffset: Int,
        xOffsetText: Int,
        frameHeight: Int,
        effectiveFocusIntervals: Float,
        opacity: Float
    ) {
        val frameIndex = if (snapshot.useFixedSlotLayout) slotIndex else displayIndex
        val displays = getNonPokemonEntityDisplaysForFrame(snapshot, frameIndex)
        if (displays.isEmpty()) return
        renderCyclingInfoDisplay(
            graphics = graphics,
            poseStack = poseStack,
            usageContext = usageContext,
            displays = displays,
            index = slotIndex,
            schedulerIndex = frameIndex,
            isLeftSide = isLeftSide,
            centerX = centerX,
            centerY = centerY,
            xOffset = xOffset,
            yOffset = yOffset,
            xOffsetText = xOffsetText,
            frameHeight = frameHeight,
            effectiveFocusIntervals = effectiveFocusIntervals,
            opacity = opacity,
            forceDrawFrame = false
        )
    }

    private fun renderNonPokemonEntityMirroredFrames(
        graphics: GuiGraphics,
        poseStack: PoseStack,
        usageContext: PokedexUsageContext,
        snapshot: NonPokemonEntityScanSnapshot,
        displayIndex: Int,
        slotIndex: Int,
        isLeftSide: Boolean,
        centerX: Int,
        centerY: Int,
        yOffset: Int,
        frameHeight: Int,
        effectiveFocusIntervals: Float,
        opacity: Float
    ) {
        val anchorIndex = if (snapshot.useFixedSlotLayout) slotIndex else displayIndex
        val mirroredSide = !isLeftSide
        val mirroredXOffset =
            (if (slotIndex == 1 || slotIndex == 2) -177 else -120) +
                if (mirroredSide) 0 else if (slotIndex == 1 || slotIndex == 2) 234 else 148

        snapshot.mirroredDisplayGroups.forEachIndexed { mirroredGroupIndex, mirroredGroup ->
            val isAnchoredHere =
                mirroredGroup.anchorSlotIndex?.let { it == slotIndex }
                    ?: (mirroredGroup.anchorDisplayIndex == anchorIndex)
            if (!isAnchoredHere) return@forEachIndexed
            if (mirroredGroup.displays.isEmpty()) return@forEachIndexed

            renderCyclingInfoDisplay(
                graphics = graphics,
                poseStack = poseStack,
                usageContext = usageContext,
                displays = mirroredGroup.displays,
                index = slotIndex,
                schedulerIndex = snapshot.frameDisplays.size + mirroredGroupIndex,
                isLeftSide = mirroredSide,
                centerX = centerX,
                centerY = centerY,
                xOffset = mirroredXOffset,
                yOffset = yOffset,
                xOffsetText = getInfoFrameTextOffset(slotIndex, mirroredSide),
                frameHeight = frameHeight,
                effectiveFocusIntervals = effectiveFocusIntervals,
                opacity = opacity,
                forceDrawFrame = true
            )
        }
    }

    private fun getEnvironmentalDisplaysForFrame(
        snapshot: EnvironmentalScanSnapshot?,
        index: Int
    ): List<VariantDisplay> {
        return snapshot?.frameDisplays?.getOrNull(index).orEmpty()
    }

    private fun getNonPokemonEntityDisplaysForFrame(
        snapshot: NonPokemonEntityScanSnapshot?,
        index: Int
    ): List<VariantDisplay> {
        return snapshot?.frameDisplays?.getOrNull(index).orEmpty()
    }

    private fun renderBeehiveInfoFrame(
        graphics: GuiGraphics,
        poseStack: PoseStack,
        usageContext: PokedexUsageContext,
        snapshot: BeehiveScanSnapshot,
        displayIndex: Int,
        slotIndex: Int,
        isLeftSide: Boolean,
        centerX: Int,
        centerY: Int,
        xOffset: Int,
        yOffset: Int,
        xOffsetText: Int,
        frameHeight: Int,
        effectiveFocusIntervals: Float,
        opacity: Float
    ) {
        val displays = getBeehiveDisplaysForFrame(snapshot, displayIndex)
        if (displays.isEmpty()) return
        renderCyclingInfoDisplay(
            graphics = graphics,
            poseStack = poseStack,
            usageContext = usageContext,
            displays = displays,
            index = slotIndex,
            schedulerIndex = displayIndex,
            isLeftSide = isLeftSide,
            centerX = centerX,
            centerY = centerY,
            xOffset = xOffset,
            yOffset = yOffset,
            xOffsetText = xOffsetText,
            frameHeight = frameHeight,
            effectiveFocusIntervals = effectiveFocusIntervals,
            opacity = opacity,
            forceDrawFrame = false
        )

        if (!isBeehiveInhabitantsFrame(snapshot, displayIndex) || snapshot.rareIndividualDisplays.isEmpty()) {
            return
        }

        val mirroredSide = !isLeftSide
        val mirroredXOffset =
            (if (slotIndex == 1 || slotIndex == 2) -177 else -120) +
                if (mirroredSide) 0 else if (slotIndex == 1 || slotIndex == 2) 234 else 148

        renderCyclingInfoDisplay(
            graphics = graphics,
            poseStack = poseStack,
            usageContext = usageContext,
            displays = snapshot.rareIndividualDisplays,
            index = slotIndex,
            schedulerIndex = 4 + displayIndex,
            isLeftSide = mirroredSide,
            centerX = centerX,
            centerY = centerY,
            xOffset = mirroredXOffset,
            yOffset = yOffset,
            xOffsetText = getInfoFrameTextOffset(slotIndex, mirroredSide),
            frameHeight = frameHeight,
            effectiveFocusIntervals = effectiveFocusIntervals,
            opacity = opacity,
            forceDrawFrame = true
        )
    }

    private fun getBeehiveDisplaysForFrame(
        snapshot: BeehiveScanSnapshot?,
        index: Int
    ): List<VariantDisplay> {
        return snapshot?.frameDisplays?.getOrNull(index).orEmpty()
    }

    private fun isBeehiveInhabitantsFrame(
        snapshot: BeehiveScanSnapshot?,
        index: Int
    ): Boolean {
        return snapshot?.isInhabitantsFrame(index) == true
    }

    private fun getInfoFrameTextOffset(index: Int, isLeftSide: Boolean): Int {
        val isInnerFrame = index == 1 || index == 2
        return if (isInnerFrame) {
            ((PokedexScannerRenderer.INNER_INFO_FRAME_WIDTH - PokedexScannerRenderer.INNER_INFO_FRAME_STEM_WIDTH) / 2) +
                if (isLeftSide) 0 else PokedexScannerRenderer.INNER_INFO_FRAME_STEM_WIDTH
        } else {
            PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH / 2
        }
    }

    private fun renderPokemonBlockInfoFrame(
        graphics: GuiGraphics,
        poseStack: PoseStack,
        usageContext: PokedexUsageContext,
        snapshot: PokemonBlockScanSnapshot,
        displayIndex: Int,
        slotIndex: Int,
        isLeftSide: Boolean,
        centerX: Int,
        centerY: Int,
        xOffset: Int,
        yOffset: Int,
        xOffsetText: Int,
        frameHeight: Int,
        effectiveFocusIntervals: Float,
        opacity: Float
    ) {
        if (snapshot.primaryDisplayIndex == displayIndex) {
            renderPrimaryDisplayFrame(
                graphics = graphics,
                poseStack = poseStack,
                usageContext = usageContext,
                renderState = snapshot.primaryRenderState,
                index = slotIndex,
                isLeftSide = isLeftSide,
                centerX = centerX,
                centerY = centerY,
                xOffset = xOffset,
                yOffset = yOffset,
                xOffsetText = xOffsetText,
                frameHeight = frameHeight,
                effectiveFocusIntervals = effectiveFocusIntervals,
                opacity = opacity
            )
            return
        }

        val displays = snapshot.frameDisplays.getOrNull(displayIndex).orEmpty()
        if (displays.isEmpty()) return

        renderCyclingInfoDisplay(
            graphics = graphics,
            poseStack = poseStack,
            usageContext = usageContext,
            displays = displays,
            index = slotIndex,
            schedulerIndex = displayIndex,
            isLeftSide = isLeftSide,
            centerX = centerX,
            centerY = centerY,
            xOffset = xOffset,
            yOffset = yOffset,
            xOffsetText = xOffsetText,
            frameHeight = frameHeight,
            effectiveFocusIntervals = effectiveFocusIntervals,
            opacity = opacity,
            forceDrawFrame = false
        )
    }

    private fun renderPokemonBlockMirroredFrames(
        graphics: GuiGraphics,
        poseStack: PoseStack,
        usageContext: PokedexUsageContext,
        snapshot: PokemonBlockScanSnapshot,
        displayIndex: Int,
        slotIndex: Int,
        isLeftSide: Boolean,
        centerX: Int,
        centerY: Int,
        yOffset: Int,
        frameHeight: Int,
        effectiveFocusIntervals: Float,
        opacity: Float
    ) {
        val mirroredSide = !isLeftSide
        val mirroredXOffset =
            (if (slotIndex == 1 || slotIndex == 2) -177 else -120) +
                if (mirroredSide) 0 else if (slotIndex == 1 || slotIndex == 2) 234 else 148

        snapshot.mirroredDisplayGroups.forEachIndexed { mirroredGroupIndex, mirroredGroup ->
            if (mirroredGroup.anchorDisplayIndex != displayIndex) return@forEachIndexed
            if (mirroredGroup.displays.isEmpty()) return@forEachIndexed

            renderCyclingInfoDisplay(
                graphics = graphics,
                poseStack = poseStack,
                usageContext = usageContext,
                displays = mirroredGroup.displays,
                index = slotIndex,
                schedulerIndex = snapshot.frameCount + mirroredGroupIndex,
                isLeftSide = mirroredSide,
                centerX = centerX,
                centerY = centerY,
                xOffset = mirroredXOffset,
                yOffset = yOffset,
                xOffsetText = getInfoFrameTextOffset(slotIndex, mirroredSide),
                frameHeight = frameHeight,
                effectiveFocusIntervals = effectiveFocusIntervals,
                opacity = opacity,
                forceDrawFrame = true
            )
        }
    }

    private fun isTankTypingVisible(): Boolean {
        val speciesId = RestorationTankScanState.speciesId ?: return false
        return CobblemonClient.clientPokedexData.getHighestKnowledgeForSpecies(speciesId) ==
            com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress.OWNED
    }

    private fun buildTankPrimaryRenderState(): VariantRenderState {
        return PokemonBlockScanPresentation.build(isTankTypingVisible())?.primaryRenderState
            ?: VariantRenderState(emptyList(), false)
    }

    private fun buildPokemonPrimaryRenderState(scanData: Any, focusedEntity: PokemonEntity?): VariantRenderState {
        val renderState = VariantLabelResolver.resolveRenderState(scanData, focusedEntity?.aspects)
        val serverDisplays = buildList {
            val focusedEntityId = focusedEntity?.id
            val speciesId = focusedEntity?.pokemon?.species?.resourceIdentifier?.toString()
            val percent = SlowpokeTailRegrowthClientState.percent

            if (
                speciesId == "cobblemon:slowpoke" &&
                focusedEntityId != null &&
                SlowpokeTailRegrowthClientState.activeEntityId == focusedEntityId &&
                percent != null
            ) {
                add(
                    when {
                        percent <= 0 -> VariantDisplay(
                            headerTranslationKey = "betterpokedexscanner.header.tail_health",
                            label = "Sheared",
                            labelTranslationKey = "betterpokedexscanner.variant.sheared",
                            showShinyIcon = false,
                            needsSeparateShinyTag = false,
                            colour = interpolateRedToGreen(percent)
                        )
                        percent >= 100 -> VariantDisplay(
                            headerTranslationKey = "betterpokedexscanner.header.tail_health",
                            label = "Fully Grown",
                            labelTranslationKey = "betterpokedexscanner.variant.fully_grown",
                            showShinyIcon = false,
                            needsSeparateShinyTag = false,
                            colour = interpolateRedToGreen(percent)
                        )
                        else -> VariantDisplay(
                            headerTranslationKey = "betterpokedexscanner.header.tail_regrowth",
                            label = "$percent%",
                            labelTranslationKey = null,
                            showShinyIcon = false,
                            needsSeparateShinyTag = false,
                            colour = interpolateRedToGreen(percent)
                        )
                    }
                )
            }
        }

        return VariantRenderState(
            primaryDisplays = (renderState.primaryDisplays + serverDisplays).distinctBy {
                Triple(it.headerLabel, it.headerTranslationKey, it.label)
            },
            showSeparateShinyTag = renderState.showSeparateShinyTag
        )
    }

    private fun drawScaledStyledText(
        graphics: GuiGraphics,
        text: Component,
        x: Int,
        y: Float,
        scale: Float,
        shadow: Boolean,
        centered: Boolean
    ) {
        if (scale <= 0F) return

        val styledText = text.copy().font(CobblemonResources.DEFAULT_LARGE)
        val font = Minecraft.getInstance().font
        val width = font.width(styledText)
        val drawX = if (centered) (-(width / 2F)).roundToInt() else 0

        graphics.pose().pushPose()
        graphics.pose().translate(x.toFloat(), y, 0F)
        graphics.pose().scale(scale, scale, 1F)
        graphics.drawString(font, styledText, drawX, 0, 0xFFFFFF, shadow)
        graphics.pose().popPose()
    }

    private fun renderPrimaryDisplayFrame(
        graphics: GuiGraphics,
        poseStack: PoseStack,
        usageContext: PokedexUsageContext,
        renderState: VariantRenderState,
        index: Int,
        isLeftSide: Boolean,
        centerX: Int,
        centerY: Int,
        xOffset: Int,
        yOffset: Int,
        xOffsetText: Int,
        frameHeight: Int,
        effectiveFocusIntervals: Float,
        opacity: Float
    ) {
        val primaryDisplays = renderState.primaryDisplays
        if (primaryDisplays.isEmpty()) return

        val isInnerFrame = index == 1 || index == 2
        val frameWidth =
            if (isInnerFrame) PokedexScannerRenderer.INNER_INFO_FRAME_WIDTH
            else PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH
        val variantCycleAccess = usageContext as VariantCycleAccess
        val variantCycleTicks = variantCycleAccess.getVariantCycleTicksBetterPokedexScanner()
        val holdTicks = 80f
        val transitionTicks = 10f
        val totalTicksPerPage = holdTicks + transitionTicks
        val cyclePosition = variantCycleTicks % totalTicksPerPage
        val basePageIndex = ((variantCycleTicks / totalTicksPerPage).toInt()) % primaryDisplays.size
        val nextPageIndex = (basePageIndex + 1) % primaryDisplays.size
        val inTransition = primaryDisplays.size > 1 && cyclePosition >= holdTicks
        val transitionProgress =
            if (inTransition) ((cyclePosition - holdTicks) / transitionTicks).coerceIn(0f, 1f)
            else 0f
        val baseFrameIndex = ceil(effectiveFocusIntervals).toInt().coerceIn(0, 9)
        val animatedPrimaryFrameIndex =
            if (primaryDisplays.size > 1 && inTransition) {
                val collapseAmount =
                    if (transitionProgress < 0.5f) transitionProgress / 0.5f
                    else (1f - transitionProgress) / 0.5f
                val collapseStepRange = if (isInnerFrame) 4f else 2f
                val collapseSteps = (collapseAmount * collapseStepRange).roundToInt()
                (baseFrameIndex - collapseSteps).coerceIn(0, 9)
            } else {
                baseFrameIndex
            }

        if (primaryDisplays.size > 1) {
            blitk(
                matrixStack = poseStack,
                texture = PokedexScannerRenderer.infoFrameResource(isLeftSide, index),
                x = centerX + xOffset,
                y = centerY + yOffset,
                width = frameWidth,
                height = frameHeight,
                textureHeight = frameHeight * 10,
                vOffset = animatedPrimaryFrameIndex * frameHeight,
                alpha = opacity
            )
        }

        val variantDisplay =
            if (inTransition && transitionProgress >= 0.5f) primaryDisplays[nextPageIndex]
            else primaryDisplays[basePageIndex]
        val variantText = variantDisplay.labelComponent?.copy() ?: if (!variantDisplay.labelTranslationKey.isNullOrBlank()) {
            Component.translatable(variantDisplay.labelTranslationKey).copy().bold()
        } else {
            Component.literal(variantDisplay.label).copy().bold()
        }
        val headerComponent = if (!variantDisplay.headerTranslationKey.isNullOrBlank()) {
            Component.translatable(variantDisplay.headerTranslationKey)
        } else {
            variantDisplay.headerLabel?.let(Component::literal)
                ?: Component.translatable("cobblemon.pokedex.variation.variant")
        }
        val headerY = if (index == 3) centerY + yOffset + 40 else centerY + yOffset + when (index) {
            0 -> 3
            1 -> 2
            2 -> 6
            else -> 3
        }
        val valueY = if (index == 3) centerY + yOffset + 44 else centerY + yOffset + when (index) {
            0 -> 7
            1 -> 6
            2 -> 10
            else -> 7
        }
        val noHeaderValueY = if (index == 3) centerY + yOffset + 42f else valueY.toFloat()
        val isShinyOnlyDisplay =
            variantDisplay.showShinyIcon &&
                !variantDisplay.needsSeparateShinyTag &&
                variantDisplay.labelTranslationKey == "betterpokedexscanner.label.shiny"
        val primaryValueY =
            if (isShinyOnlyDisplay)
                noHeaderValueY
            else
                valueY.toFloat()
        val shinyValueY = when {
            index == 3 -> primaryValueY.toFloat() - 2f
            isInnerFrame -> primaryValueY.toFloat() - 2.4f
            else -> primaryValueY.toFloat()
        }
        val headerScale =
            if (primaryDisplays.size > 1 && inTransition) {
                when {
                    transitionProgress < 0.20f -> 0.5f * (1f - (transitionProgress / 0.20f))
                    transitionProgress < 0.8f -> 0f
                    else -> 0.5f * ((transitionProgress - 0.8f) / 0.3f)
                }
            } else {
                0.5f
            }
        val maxWidth = frameWidth - 8
        val baseTextWidth = Minecraft.getInstance().font.width(variantText.font(CobblemonResources.DEFAULT_LARGE))
        val textScale = if (baseTextWidth > maxWidth) 0.75F else 1.0F
        val animatedTextScale =
            if (!inTransition) {
                textScale
            } else {
                when {
                    transitionProgress < 0.20f -> textScale * (1f - (transitionProgress / 0.20f))
                    transitionProgress < 0.8f -> 0f
                    else -> textScale * ((transitionProgress - 0.8f) / 0.3f)
                }
            }

        if (!isShinyOnlyDisplay) {
            drawScaledText(
                context = graphics,
                text = headerComponent,
                x = centerX + xOffset + xOffsetText,
                y = headerY,
                shadow = true,
                centered = true,
                scale = headerScale
            )
        }

        val scaledTextWidth = (baseTextWidth * textScale).toInt()
        val textCenterX = centerX + xOffset + xOffsetText
        val textLeftX = textCenterX - (scaledTextWidth / 2)

        if (variantDisplay.showShinyIcon) {
            val iconScale = ScannerRenderConstants.SHINY_ICON_SCALE
            val iconVisualWidth = (ScannerRenderConstants.SHINY_ICON_SIZE * iconScale).toInt()
            val iconSpacing = 2

            blitk(
                matrixStack = poseStack,
                texture = ScannerRenderConstants.SHINY_ICON,
                x = (textLeftX - iconSpacing - iconVisualWidth) / iconScale,
                y = (if (isShinyOnlyDisplay) shinyValueY else primaryValueY) / iconScale,
                width = ScannerRenderConstants.SHINY_ICON_SIZE,
                height = ScannerRenderConstants.SHINY_ICON_SIZE,
                scale = iconScale
            )
        }

        if (variantDisplay.labelComponent != null) {
            drawScaledStyledText(
                graphics = graphics,
                text = variantText,
                x = textCenterX,
                y = if (isShinyOnlyDisplay) shinyValueY else primaryValueY,
                scale = animatedTextScale,
                shadow = true,
                centered = true
            )
        } else {
            drawScaledText(
                context = graphics,
                font = CobblemonResources.DEFAULT_LARGE,
                text = variantText,
                x = textCenterX,
                y = if (isShinyOnlyDisplay) shinyValueY else primaryValueY,
                colour = variantDisplay.colour ?: 0xFFFFFF,
                shadow = true,
                centered = true,
                scale = animatedTextScale
            )
        }

        if (!renderState.showSeparateShinyTag) return

        val shinySide = !isLeftSide
        val shinyXOffsetText = getInfoFrameTextOffset(index, shinySide)
        val shinyXOffset =
            (if (isInnerFrame) -177 else -120) + if (shinySide) 0 else if (isInnerFrame) 234 else 148
        val shinyTextCenterX = centerX + shinyXOffset + shinyXOffsetText
        val shinyText = Component.translatable("betterpokedexscanner.label.shiny").copy().bold()
        val shinyTextWidth = Minecraft.getInstance().font.width(shinyText.font(CobblemonResources.DEFAULT_LARGE))
        val shinyTextScale = if (shinyTextWidth > maxWidth) 0.75F else 1.0F
        val shinyScaledTextWidth = (shinyTextWidth * shinyTextScale).toInt()
        val shinyTextLeftX = shinyTextCenterX - (shinyScaledTextWidth / 2)

        blitk(
            matrixStack = poseStack,
            texture = PokedexScannerRenderer.infoFrameResource(shinySide, index),
            x = centerX + shinyXOffset,
            y = centerY + yOffset,
            width = frameWidth,
            height = frameHeight,
            textureHeight = frameHeight * 10,
            vOffset = ceil(effectiveFocusIntervals).toInt().coerceIn(0, 9) * frameHeight,
            alpha = opacity
        )

        val shinyIconScale = ScannerRenderConstants.SHINY_ICON_SCALE
        val shinyIconVisualWidth = (ScannerRenderConstants.SHINY_ICON_SIZE * shinyIconScale).toInt()

        blitk(
            matrixStack = poseStack,
            texture = ScannerRenderConstants.SHINY_ICON,
            x = (shinyTextLeftX - 2 - shinyIconVisualWidth) / shinyIconScale,
            y = shinyValueY / shinyIconScale,
            width = ScannerRenderConstants.SHINY_ICON_SIZE,
            height = ScannerRenderConstants.SHINY_ICON_SIZE,
            scale = shinyIconScale
        )

        drawScaledText(
            context = graphics,
            font = CobblemonResources.DEFAULT_LARGE,
            text = shinyText,
            x = shinyTextCenterX,
            y = shinyValueY,
            shadow = true,
            centered = true,
            scale = shinyTextScale
        )
    }

    private fun renderCyclingInfoDisplay(
        graphics: GuiGraphics,
        poseStack: PoseStack,
        usageContext: PokedexUsageContext,
        displays: List<VariantDisplay>,
        index: Int,
        schedulerIndex: Int,
        isLeftSide: Boolean,
        centerX: Int,
        centerY: Int,
        xOffset: Int,
        yOffset: Int,
        xOffsetText: Int,
        frameHeight: Int,
        effectiveFocusIntervals: Float,
        opacity: Float,
        forceDrawFrame: Boolean
    ) {
        val isInnerFrame = index == 1 || index == 2
        val frameWidth =
            if (isInnerFrame) PokedexScannerRenderer.INNER_INFO_FRAME_WIDTH
            else PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH
        val slotState = (usageContext as VariantCycleAccess).getCarouselSlotState(schedulerIndex)
        val transitionProgress = slotState.transitionProgress.coerceIn(0F, 1F)
        val inTransition = transitionProgress > 0F
        val basePageIndex = slotState.currentPage.coerceIn(0, max(0, displays.size - 1))
        val nextPageIndex = slotState.nextPage.coerceIn(0, max(0, displays.size - 1))

        val baseFrameIndex = ceil(effectiveFocusIntervals).toInt().coerceIn(0, 9)
        val animatedFrameIndex =
            if (displays.size > 1 && inTransition) {
                val collapseAmount =
                    if (transitionProgress < 0.5f) transitionProgress / 0.5f
                    else (1f - transitionProgress) / 0.5f
                val collapseStepRange =
                    if (isInnerFrame) 4f
                    else 2f
                val collapseSteps = (collapseAmount * collapseStepRange).roundToInt()
                (baseFrameIndex - collapseSteps).coerceIn(0, 9)
            } else {
                baseFrameIndex
            }

        if (forceDrawFrame || displays.size > 1) {
            blitk(
                matrixStack = poseStack,
                texture = PokedexScannerRenderer.infoFrameResource(isLeftSide, index),
                x = centerX + xOffset,
                y = centerY + yOffset,
                width = frameWidth,
                height = frameHeight,
                textureHeight = frameHeight * 10,
                vOffset = animatedFrameIndex * frameHeight,
                alpha = opacity
            )
        }

        val display =
            if (inTransition && transitionProgress >= 0.5f) displays[nextPageIndex] else displays[basePageIndex]
        val hasHeader = !display.headerTranslationKey.isNullOrBlank() || !display.headerLabel.isNullOrBlank()
        val header = if (!display.headerTranslationKey.isNullOrBlank()) {
            Component.translatable(display.headerTranslationKey)
        } else if (!display.headerLabel.isNullOrBlank()) {
            Component.literal(display.headerLabel ?: "Info")
        } else {
            Component.empty()
        }
        val value: MutableComponent = display.labelComponent?.copy() ?: if (!display.labelTranslationKey.isNullOrBlank()) {
            Component.translatable(display.labelTranslationKey).copy().bold()
        } else {
            Component.literal(display.label).copy().bold()
        }
        val headerScale =
            if (displays.size > 1 && inTransition) {
                when {
                    transitionProgress < 0.20f -> 0.5f * (1f - (transitionProgress / 0.20f))
                    transitionProgress < 0.8f -> 0f
                    else -> 0.5f * ((transitionProgress - 0.8f) / 0.3f)
                }
            } else {
                0.5f
            }
        val labelWidth =
            if (isInnerFrame) frameWidth - PokedexScannerRenderer.INNER_INFO_FRAME_STEM_WIDTH
            else frameWidth
        val maxWidth = labelWidth - 8
        val baseTextWidth = Minecraft.getInstance().font.width(value.font(CobblemonResources.DEFAULT_LARGE))
        val textScale =
            if (display.headerTranslationKey == "cobblemon.ui.stats.ivs" || display.headerLabel == "IVs") {
                1.0F
            } else {
                (maxWidth.toFloat() / baseTextWidth.coerceAtLeast(1)).coerceAtMost(1.0F)
            }
        val animatedTextScale =
            if (!inTransition) {
                textScale
            } else {
                when {
                    transitionProgress < 0.20f -> textScale * (1f - (transitionProgress / 0.20f))
                    transitionProgress < 0.8f -> 0f
                    else -> textScale * ((transitionProgress - 0.8f) / 0.3f)
                }
        }
        val headerY = if (index == 3) centerY + yOffset + 40 else centerY + yOffset + when (index) {
            0 -> 3
            1 -> 2
            2 -> 6
            else -> 3
        }
        val valueY = if (index == 3) centerY + yOffset + 44 else centerY + yOffset + when (index) {
            0 -> 7
            1 -> 6
            2 -> 10
            else -> 7
        }
        val noHeaderValueY = (headerY + valueY) / 2f

        if (hasHeader) {
            drawScaledText(
                context = graphics,
                text = header,
                x = centerX + xOffset + xOffsetText,
                y = headerY,
                shadow = true,
                centered = true,
                scale = headerScale
            )
        }

        val scaledTextWidth = (baseTextWidth * textScale).toInt()
        val textCenterX = centerX + xOffset + xOffsetText
        val textLeftX = textCenterX - (scaledTextWidth / 2)
        val textRightX = textCenterX + (scaledTextWidth / 2)
        val resolvedValueY = if (hasHeader) valueY.toFloat() else noHeaderValueY

        if (display.showShinyIcon && animatedTextScale > 0F) {
            val iconScale = ScannerRenderConstants.SHINY_ICON_SCALE
            val iconVisualWidth = (ScannerRenderConstants.SHINY_ICON_SIZE * iconScale).toInt()
            val animatedIconScale = iconScale * animatedTextScale

            blitk(
                matrixStack = poseStack,
                texture = ScannerRenderConstants.SHINY_ICON,
                x = (textLeftX - 2 - iconVisualWidth) / animatedIconScale,
                y = resolvedValueY / animatedIconScale,
                width = ScannerRenderConstants.SHINY_ICON_SIZE,
                height = ScannerRenderConstants.SHINY_ICON_SIZE,
                scale = animatedIconScale
            )
        }

        if (display.genderIcon != null && display.genderIcon != Gender.GENDERLESS && animatedTextScale > 0F) {
            val isMale = display.genderIcon == Gender.MALE
            val symbol = if (isMale) "♂" else "♀"
            val symbolText = Component.literal(symbol).copy().bold()
            val symbolWidth =
                (Minecraft.getInstance().font.width(symbolText.font(CobblemonResources.DEFAULT_LARGE)) * animatedTextScale).toInt()
            val symbolX =
                if (display.trailingGenderIcon) {
                    textRightX + 2
                } else {
                    textLeftX - 2 - symbolWidth
                }
            drawScaledText(
                context = graphics,
                font = CobblemonResources.DEFAULT_LARGE,
                text = symbolText,
                x = symbolX,
                y = resolvedValueY,
                colour = if (isMale) 0x32CBFF else 0xFC5454,
                shadow = true,
                scale = animatedTextScale
            )
        }

        if (display.showCaughtIcon && animatedTextScale > 0F) {
            val indicatorScale = BattleOverlay.SCALE * animatedTextScale
            val indicatorVisualWidth = (10 * indicatorScale).toInt()
            blitk(
                matrixStack = poseStack,
                texture = BattleOverlay.caughtIndicator,
                x = (textLeftX - 2 - indicatorVisualWidth) / indicatorScale,
                y = (resolvedValueY + 2F) / indicatorScale,
                height = 10,
                width = 10,
                scale = indicatorScale
            )
        }

        if (display.labelComponent != null) {
            drawScaledStyledText(
                graphics = graphics,
                text = value,
                x = textCenterX,
                y = resolvedValueY,
                scale = animatedTextScale,
                shadow = true,
                centered = true
            )
        } else {
            drawScaledText(
                context = graphics,
                font = CobblemonResources.DEFAULT_LARGE,
                text = value,
                x = textCenterX,
                y = resolvedValueY,
                colour = display.colour ?: 0xFFFFFF,
                shadow = true,
                centered = true,
                scale = animatedTextScale
            )
        }
    }

    private fun renderEnvironmentalStructureTag(
        graphics: GuiGraphics,
        poseStack: PoseStack,
        snapshot: EnvironmentalScanSnapshot,
        isLeftSide: Boolean,
        centerX: Int,
        centerY: Int,
        opacity: Float,
        effectiveFocusIntervals: Float
    ) {
        val structureName = snapshot.nearbyStructureName ?: return
        val mirroredSide = !isLeftSide
        val xOffset = -120 + if (mirroredSide) 0 else 148
        val yOffset = 25
        val centerTextX = centerX + xOffset + (PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH / 2)
        val maxWidth = PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH - 8
        val wrappedLines = wrapStructureName(structureName, maxWidth)
        val autoScale = wrappedLines.maxOfOrNull { line ->
            val width = Minecraft.getInstance().font.width(Component.literal(line).font(CobblemonResources.DEFAULT_LARGE))
            when {
                width <= maxWidth -> 1.0F
                width <= (maxWidth / 0.75F).toInt() -> 0.75F
                else -> 0.6F
            }
        } ?: 1.0F
        val valueScale = if (wrappedLines.size > 1) min(autoScale, 0.7F) else autoScale

        blitk(
            matrixStack = poseStack,
            texture = PokedexScannerRenderer.infoFrameResource(mirroredSide, 3),
            x = centerX + xOffset,
            y = centerY + yOffset,
            width = PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH,
            height = PokedexScannerRenderer.OUTER_INFO_FRAME_HEIGHT,
            textureHeight = PokedexScannerRenderer.OUTER_INFO_FRAME_HEIGHT * 10,
            vOffset = ceil(effectiveFocusIntervals) * PokedexScannerRenderer.OUTER_INFO_FRAME_HEIGHT,
            alpha = opacity
        )

        drawScaledText(
            context = graphics,
            text = Component.translatable("betterpokedexscanner.header.environment.nearby_structure"),
            x = centerTextX,
            y = centerY + yOffset + 39.5,
            shadow = true,
            centered = true,
            scale = 0.5F
        )

        val firstLineY = if (wrappedLines.size > 1) centerY + yOffset + 43 else centerY + yOffset + 44
        wrappedLines.forEachIndexed { index, line ->
            drawScaledText(
                context = graphics,
                font = CobblemonResources.DEFAULT_LARGE,
                text = Component.literal(line).copy().bold(),
                x = centerTextX,
                y = firstLineY + (index * 4),
                shadow = true,
                centered = true,
                scale = valueScale
            )
        }
    }

    private fun wrapStructureName(structureName: String, maxWidth: Int): List<String> {
        val words = structureName.split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return listOf(structureName)
        val font = Minecraft.getInstance().font
        val singleLineWidth = font.width(Component.literal(structureName).font(CobblemonResources.DEFAULT_LARGE))
        if (words.size == 1 || singleLineWidth <= maxWidth) {
            return listOf(structureName)
        }

        var bestSplit: List<String>? = null
        var bestScore = Int.MAX_VALUE

        for (splitIndex in 1 until words.size) {
            val firstLine = words.take(splitIndex).joinToString(" ")
            val secondLine = words.drop(splitIndex).joinToString(" ")
            val firstWidth = font.width(Component.literal(firstLine).font(CobblemonResources.DEFAULT_LARGE))
            val secondWidth = font.width(Component.literal(secondLine).font(CobblemonResources.DEFAULT_LARGE))
            val overflowPenalty = max(0, firstWidth - maxWidth) + max(0, secondWidth - maxWidth)
            val balancePenalty = kotlin.math.abs(firstWidth - secondWidth)
            val score = overflowPenalty * 4 + balancePenalty

            if (score < bestScore) {
                bestScore = score
                bestSplit = listOf(firstLine, secondLine)
            }
        }

        return bestSplit ?: listOf(structureName)
    }

}
