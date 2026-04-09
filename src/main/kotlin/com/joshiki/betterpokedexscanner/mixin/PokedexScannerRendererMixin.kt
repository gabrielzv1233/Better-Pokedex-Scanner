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
import com.cobblemon.mod.common.util.cobblemonResource
import net.minecraft.resources.ResourceLocation
import com.joshiki.betterpokedexscanner.client.ScannerRenderConstants
import com.joshiki.betterpokedexscanner.scanner.VariantCycleAccess
import kotlin.math.roundToInt
import com.joshiki.betterpokedexscanner.client.SlowpokeTailRegrowthClientState
import com.joshiki.betterpokedexscanner.scanner.VariantDisplay
import com.joshiki.betterpokedexscanner.client.RestorationTankScanState
import com.joshiki.betterpokedexscanner.client.EnvironmentalScanState
import com.joshiki.betterpokedexscanner.client.BeehiveScanState
import com.cobblemon.mod.common.block.multiblock.FossilMultiblockStructure
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
import com.joshiki.betterpokedexscanner.scanner.VariantLabelRegistry
import net.minecraft.client.resources.language.I18n
import com.joshiki.betterpokedexscanner.scanner.VariantRenderState
import com.joshiki.betterpokedexscanner.scanner.EnvironmentalScanSnapshot
import com.joshiki.betterpokedexscanner.scanner.BeehiveScanSnapshot


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

        val effectiveScanningProgress = max(
            max(usageContext.scanningProgress, tankScanProgress),
            max(environmentScanProgress, beehiveScanProgress)
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

        if (!showTankUnknown && !showTankRegister && !showEnvironmentUnknown && !showBeehiveUnknown) return

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
        } else if (showTankUnknown || showEnvironmentUnknown || showBeehiveUnknown) {
            val progress =
                if (showTankUnknown) RestorationTankRegistrationState.scanProgress
                else if (showEnvironmentUnknown) EnvironmentalScanState.scanProgress
                else BeehiveScanState.scanProgress
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

        if (tankRegistrationActive && usageContext.scannableEntityInFocus == null) {
            return
        }

        val effectiveFocusIntervals =
            if (environmentActive) EnvironmentalScanState.focusIntervals
            else if (beehiveActive) BeehiveScanState.focusIntervals
            else if (tankActive) RestorationTankScanState.focusIntervals
            else usageContext.focusIntervals

        if (effectiveFocusIntervals <= 0) return

        val tankTypeVisible = tankActive && !tankRegistrationActive && isTankTypingVisible()
        val tankPrimaryDisplayCounter = if (tankTypeVisible) 4 else 3
        val pokemonActive =
            usageContext.scannableEntityInFocus != null &&
                !tankRegistrationActive &&
                !tankActive &&
                !beehiveActive &&
                !environmentActive
        val pokemonTypeVisible = pokemonActive && usageContext.isPokemonInFocusOwned
        val pokemonPrimaryDisplayCounter = if (pokemonTypeVisible) 4 else 3

        var infoDisplayedCounter = 0


        usageContext.availableInfoFrames.forEachIndexed { index, isLeftSide ->
            if (isLeftSide == null) return@forEachIndexed


            infoDisplayedCounter++
            val beehiveDisplayIndex = if (beehiveActive) infoDisplayedCounter - 1 else index

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

            if (!shouldUseAnimatedPrimaryFrame) {
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
                (usageContext.scannableEntityInFocus != null || tankActive || beehiveActive || environmentActive)
            ) {
                val pokedexEntityData = usageContext.scannableEntityInFocus?.resolvePokemonScan()
                val environmentalSnapshot = EnvironmentalScanState.snapshot
                val beehiveSnapshot = BeehiveScanState.snapshot

                if (beehiveActive) {
                    renderBeehiveInfoFrame(
                        graphics = graphics,
                        poseStack = poseStack,
                        usageContext = usageContext,
                        snapshot = beehiveSnapshot ?: return@forEachIndexed,
                        displayIndex = beehiveDisplayIndex,
                        slotIndex = index,
                        isLeftSide = isLeftSide,
                        centerX = centerX,
                        centerY = centerY,
                        xOffset = xOffset,
                        yOffset = yOffset,
                        xOffsetText = xOffsetText,
                        yOffsetText = yOffsetText,
                        frameHeight = frameHeight,
                        effectiveFocusIntervals = effectiveFocusIntervals,
                        opacity = opacity
                    )
                    return@forEachIndexed
                }

                if (infoDisplayedCounter == 1) {
                    if (environmentActive) {
                        renderEnvironmentalInfoFrame(
                            graphics = graphics,
                            poseStack = poseStack,
                            usageContext = usageContext,
                            snapshot = environmentalSnapshot ?: return@forEachIndexed,
                            index = index,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            xOffset = xOffset,
                            yOffset = yOffset,
                            xOffsetText = xOffsetText,
                            yOffsetText = yOffsetText,
                            frameHeight = frameHeight,
                            effectiveFocusIntervals = effectiveFocusIntervals,
                            opacity = opacity
                        )
                    } else if (tankActive && !tankRegistrationActive) {
                        val progress = getTankProgressPercent(
                            RestorationTankScanState.timeRemaining ?: FossilMultiblockStructure.TIME_TO_TAKE
                        )

                        drawScaledText(
                            context = graphics,
                            font = CobblemonResources.DEFAULT_LARGE,
                            text = Component.literal("Lv.--").bold(),
                            x = centerX + xOffset + xOffsetText,
                            y = centerY + yOffset + yOffsetText,
                            shadow = true,
                            centered = true
                        )
                    } else {
                        val entityData = pokedexEntityData ?: return@forEachIndexed
                        drawScaledText(
                            context = graphics,
                            font = CobblemonResources.DEFAULT_LARGE,
                            text = lang("ui.lv.number", entityData.pokemon.level).bold(),
                            x = centerX + xOffset + xOffsetText,
                            y = centerY + yOffset + yOffsetText,
                            shadow = true,
                            centered = true
                        )
                    }
                }

                if (infoDisplayedCounter == 2) {
                    if (environmentActive) {
                        renderEnvironmentalInfoFrame(
                            graphics = graphics,
                            poseStack = poseStack,
                            usageContext = usageContext,
                            snapshot = environmentalSnapshot ?: return@forEachIndexed,
                            index = index,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            xOffset = xOffset,
                            yOffset = yOffset,
                            xOffsetText = xOffsetText,
                            yOffsetText = yOffsetText,
                            frameHeight = frameHeight,
                            effectiveFocusIntervals = effectiveFocusIntervals,
                            opacity = opacity
                        )
                    } else if (tankActive && !tankRegistrationActive) {
                        val speciesId = RestorationTankScanState.speciesId ?: return@forEachIndexed
                        val speciesName =
                            if (RestorationTankScanState.validSpecies) {
                                resolveSpeciesDisplayName(speciesId).copy().bold()
                            } else {
                                Component.literal("MissingNo.").bold()

                            }

                        drawScaledText(
                            context = graphics,
                            text = Component.literal("Restoration Subject"),
                            x = centerX + xOffset + xOffsetText,
                            y = centerY + yOffset + yOffsetText - 2,
                            shadow = true,
                            centered = true,
                            scale = 0.5F
                        )

                        drawScaledText(
                            context = graphics,
                            font = CobblemonResources.DEFAULT_LARGE,
                            text = speciesName,
                            x = centerX + xOffset + xOffsetText,
                            y = centerY + yOffset + yOffsetText + 2,
                            shadow = true,
                            centered = true
                        )
                    } else {
                        val entityData = pokedexEntityData ?: return@forEachIndexed
                        val hasTrainer =
                            (usageContext.scannableEntityInFocus?.resolveEntityScan() as? PokemonEntity)?.ownerUUID != null
                        val speciesName = entityData.getApparentSpecies().translatedName.bold()
                        var yOffsetName = if (hasTrainer) 2 else 0

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
                        renderEnvironmentalInfoFrame(
                            graphics = graphics,
                            poseStack = poseStack,
                            usageContext = usageContext,
                            snapshot = environmentalSnapshot ?: return@forEachIndexed,
                            index = index,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            xOffset = xOffset,
                            yOffset = yOffset,
                            xOffsetText = xOffsetText,
                            yOffsetText = yOffsetText,
                            frameHeight = frameHeight,
                            effectiveFocusIntervals = effectiveFocusIntervals,
                            opacity = opacity
                        )
                    } else if (tankActive && !tankRegistrationActive) {
                        if (tankTypeVisible) {
                            val speciesId = RestorationTankScanState.speciesId ?: return@forEachIndexed
                            val typeText =
                                if (RestorationTankScanState.validSpecies) {
                                    val baseTypeText = resolveSpeciesTypeText(speciesId)?.copy() ?: Component.literal("Unknown")
                                    Component.literal(baseTypeText.string + " Type").bold()
                                } else {
                                    Component.literal("Unknown Type").bold()
                                }

                            val typeWidth =
                                Minecraft.getInstance().font.width(typeText.font(CobblemonResources.DEFAULT_LARGE))

                            if (typeWidth > (PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH - 8) && typeText.string.contains(
                                    "/"
                                )
                            ) {
                                val splitLines = typeText.string.split("/")

                                drawScaledText(
                                    context = graphics,
                                    font = CobblemonResources.DEFAULT_LARGE,
                                    text = text(splitLines[0], "/").bold(),
                                    x = centerX + xOffset + xOffsetText,
                                    y = centerY + yOffset + yOffsetText - 4,
                                    shadow = true,
                                    centered = true
                                )

                                drawScaledText(
                                    context = graphics,
                                    font = CobblemonResources.DEFAULT_LARGE,
                                    text = text(splitLines[1]).bold(),
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
                            renderPrimaryDisplayFrame(
                                graphics = graphics,
                                poseStack = poseStack,
                                usageContext = usageContext,
                                renderState = buildTankPrimaryRenderState(),
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
                    } else {
                        val entityData = pokedexEntityData ?: return@forEachIndexed
                        if (pokemonTypeVisible) {
                            val typeText = lang(
                                "type.suffix",
                                entityData.getApparentForm().types
                                    .map { it.displayName.copy() }
                                    .reduce { acc, next -> acc.plus("/").plus(next) }
                            ).bold()

                            val typeWidth =
                                Minecraft.getInstance().font.width(typeText.font(CobblemonResources.DEFAULT_LARGE))

                            if (typeWidth > (PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH - 8) &&
                                entityData.getApparentForm().secondaryType != null
                            ) {
                                val splitLines = typeText.string.split("/")

                                drawScaledText(
                                    context = graphics,
                                    font = CobblemonResources.DEFAULT_LARGE,
                                    text = text(splitLines[0], "/").bold(),
                                    x = centerX + xOffset + xOffsetText,
                                    y = centerY + yOffset + yOffsetText - 4,
                                    shadow = true,
                                    centered = true
                                )

                                drawScaledText(
                                    context = graphics,
                                    font = CobblemonResources.DEFAULT_LARGE,
                                    text = text(splitLines[1]).bold(),
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
                        renderEnvironmentalInfoFrame(
                            graphics = graphics,
                            poseStack = poseStack,
                            usageContext = usageContext,
                            snapshot = environmentalSnapshot ?: return@forEachIndexed,
                            index = index,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            xOffset = xOffset,
                            yOffset = yOffset,
                            xOffsetText = xOffsetText,
                            yOffsetText = yOffsetText,
                            frameHeight = frameHeight,
                            effectiveFocusIntervals = effectiveFocusIntervals,
                            opacity = opacity
                        )
                        renderEnvironmentalStructureTag(
                            graphics = graphics,
                            poseStack = poseStack,
                            snapshot = environmentalSnapshot ?: return@forEachIndexed,
                            isLeftSide = isLeftSide,
                            centerX = centerX,
                            centerY = centerY,
                            opacity = opacity,
                            effectiveFocusIntervals = effectiveFocusIntervals
                        )
                    } else if (tankActive && !tankRegistrationActive) {
                        val progress = getTankProgressPercent(
                            RestorationTankScanState.timeRemaining ?: FossilMultiblockStructure.TIME_TO_TAKE
                        )

                        val tankRenderState = buildTankRenderState()

                        val progressDisplays = if (progress >= 100) {
                            listOf(
                                VariantDisplay(
                                    headerLabel = "Restoration Progress",
                                    headerTranslationKey = null,
                                    label = "Complete",
                                    labelTranslationKey = null,
                                    showShinyIcon = false,
                                    needsSeparateShinyTag = false,
                                    colour = interpolateCyanToGreen(progress)
                                )
                            )
                        } else {
                            listOf(
                                VariantDisplay(
                                    headerLabel = "Development State",
                                    headerTranslationKey = null,
                                    label = if (progress < 50) "Embryonic" else "Fetal",
                                    labelTranslationKey = null,
                                    showShinyIcon = false,
                                    needsSeparateShinyTag = false,
                                    colour = 0xFFFFFF
                                ),
                                VariantDisplay(
                                    headerLabel = "Restoration Progress",
                                    headerTranslationKey = null,
                                    label = "$progress%",
                                    labelTranslationKey = null,
                                    showShinyIcon = false,
                                    needsSeparateShinyTag = false,
                                    colour = interpolateCyanToGreen(progress)
                                )
                            )
                        }

                        val primaryDisplays = (tankRenderState.primaryDisplays + progressDisplays).distinctBy {
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

                            val variantText = Component.literal(variantDisplay.label).copy().bold()
                            val headerY = centerY + yOffset + 40
                            val valueY = centerY + yOffset + 44
                            val noHeaderValueY = centerY + yOffset + 42
                            val primaryValueY =
                                if (variantDisplay.showShinyIcon && !variantDisplay.needsSeparateShinyTag && variantDisplay.label == "Shiny")
                                    noHeaderValueY
                                else
                                    valueY

                            val maxWidth = PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH - 8
                            val headerComponent = if (!variantDisplay.headerTranslationKey.isNullOrBlank()) {
                                Component.translatable(variantDisplay.headerTranslationKey)
                            } else {
                                Component.literal(variantDisplay.headerLabel ?: "Variant")
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
                                            transitionProgress < 0.8f -> 0f
                                            else -> {
                                                val local = (transitionProgress - 0.8f) / 0.3f
                                                0.5f * local
                                            }
                                        }
                                    }
                                } else {
                                    0.5f
                                }

                            if (!(variantDisplay.showShinyIcon && !variantDisplay.needsSeparateShinyTag && variantDisplay.label == "Shiny")) {
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
                                        transitionProgress < 0.8f -> 0f
                                        else -> {
                                            val local = (transitionProgress - 0.8f) / 0.3f
                                            textScale * local
                                        }
                                    }
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
                                    y = primaryValueY / iconScale,
                                    width = ScannerRenderConstants.SHINY_ICON_SIZE,
                                    height = ScannerRenderConstants.SHINY_ICON_SIZE,
                                    scale = iconScale
                                )
                            }

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

                            if (index == 3 && tankRenderState.showSeparateShinyTag) {
                                val shinySide = !isLeftSide
                                val shinyXOffset = -120 + if (shinySide) 0 else 148
                                val shinyYOffset = 25
                                val shinyText = Component.literal("Shiny").copy().bold()
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
                                            headerLabel = "Tail Health",
                                            headerTranslationKey = null,
                                            label = "Sheared",
                                            labelTranslationKey = null,
                                            showShinyIcon = false,
                                            needsSeparateShinyTag = false,
                                            colour = interpolateRedToGreen(percent)
                                        )

                                        percent >= 100 -> VariantDisplay(
                                            headerLabel = "Tail Health",
                                            headerTranslationKey = null,
                                            label = "Fully Grown",
                                            labelTranslationKey = null,
                                            showShinyIcon = false,
                                            needsSeparateShinyTag = false,
                                            colour = interpolateRedToGreen(percent)
                                        )

                                        else -> VariantDisplay(
                                            headerLabel = "Tail Regrowth",
                                            headerTranslationKey = null,
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
                            val variantText = Component.literal(variantDisplay.label).copy().bold()
                            val headerY = centerY + yOffset + 40
                            val valueY = centerY + yOffset + 44
                            val noHeaderValueY = centerY + yOffset + 42
                            val primaryValueY =
                                if (variantDisplay.showShinyIcon && !variantDisplay.needsSeparateShinyTag && variantDisplay.label == "Shiny")
                                    noHeaderValueY
                                else
                                    valueY
                            val maxWidth = PokedexScannerRenderer.OUTER_INFO_FRAME_WIDTH - 8
                            val headerComponent = if (!variantDisplay.headerTranslationKey.isNullOrBlank()) {
                                Component.translatable(variantDisplay.headerTranslationKey)
                            } else {
                                Component.literal(variantDisplay.headerLabel ?: "Variant")
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
                            if (!(variantDisplay.showShinyIcon && !variantDisplay.needsSeparateShinyTag && variantDisplay.label == "Shiny")) {
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

                            // Fake 5th slot: mirrored shiny tag on the opposite side of slot 4
                            if (index == 3 && renderState.showSeparateShinyTag) {
                                val shinySide = !isLeftSide
                                val shinyXOffset = -120 + if (shinySide) 0 else 148
                                val shinyYOffset = 25
                                val shinyText = Component.literal("Shiny").copy().bold()
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
        yOffsetText: Int,
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

    private fun getEnvironmentalDisplaysForFrame(
        snapshot: EnvironmentalScanSnapshot?,
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
        yOffsetText: Int,
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
        return getBeehiveDisplaysForFrame(snapshot, index)
            .firstOrNull()
            ?.headerLabel == "Inhabitants"
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

    private fun isTankTypingVisible(): Boolean {
        val speciesId = RestorationTankScanState.speciesId ?: return false
        return CobblemonClient.clientPokedexData.getHighestKnowledgeForSpecies(speciesId) ==
            com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress.CAUGHT
    }

    private fun buildTankPrimaryRenderState(): VariantRenderState {
        val progress = getTankProgressPercent(
            RestorationTankScanState.timeRemaining ?: FossilMultiblockStructure.TIME_TO_TAKE
        )
        val progressDisplays = if (progress >= 100) {
            listOf(
                VariantDisplay(
                    headerLabel = "Restoration Progress",
                    headerTranslationKey = null,
                    label = "Complete",
                    labelTranslationKey = null,
                    showShinyIcon = false,
                    needsSeparateShinyTag = false,
                    colour = interpolateCyanToGreen(progress)
                )
            )
        } else {
            listOf(
                VariantDisplay(
                    headerLabel = "Development State",
                    headerTranslationKey = null,
                    label = if (progress < 50) "Embryonic" else "Fetal",
                    labelTranslationKey = null,
                    showShinyIcon = false,
                    needsSeparateShinyTag = false,
                    colour = 0xFFFFFF
                ),
                VariantDisplay(
                    headerLabel = "Restoration Progress",
                    headerTranslationKey = null,
                    label = "$progress%",
                    labelTranslationKey = null,
                    showShinyIcon = false,
                    needsSeparateShinyTag = false,
                    colour = interpolateCyanToGreen(progress)
                )
            )
        }

        val tankRenderState = buildTankRenderState()
        return VariantRenderState(
            primaryDisplays = (tankRenderState.primaryDisplays + progressDisplays).distinctBy {
                Triple(it.headerLabel, it.headerTranslationKey, it.label)
            },
            showSeparateShinyTag = tankRenderState.showSeparateShinyTag
        )
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
                            headerLabel = "Tail Health",
                            headerTranslationKey = null,
                            label = "Sheared",
                            labelTranslationKey = null,
                            showShinyIcon = false,
                            needsSeparateShinyTag = false,
                            colour = interpolateRedToGreen(percent)
                        )
                        percent >= 100 -> VariantDisplay(
                            headerLabel = "Tail Health",
                            headerTranslationKey = null,
                            label = "Fully Grown",
                            labelTranslationKey = null,
                            showShinyIcon = false,
                            needsSeparateShinyTag = false,
                            colour = interpolateRedToGreen(percent)
                        )
                        else -> VariantDisplay(
                            headerLabel = "Tail Regrowth",
                            headerTranslationKey = null,
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
        val variantText = Component.literal(variantDisplay.label).copy().bold()
        val headerComponent = if (!variantDisplay.headerTranslationKey.isNullOrBlank()) {
            Component.translatable(variantDisplay.headerTranslationKey)
        } else {
            Component.literal(variantDisplay.headerLabel ?: "Variant")
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
        val noHeaderValueY = if (index == 3) centerY + yOffset + 42 else valueY
        val primaryValueY =
            if (variantDisplay.showShinyIcon && !variantDisplay.needsSeparateShinyTag && variantDisplay.label == "Shiny")
                noHeaderValueY
            else
                valueY
        val isShinyOnlyDisplay =
            variantDisplay.showShinyIcon &&
                !variantDisplay.needsSeparateShinyTag &&
                variantDisplay.label == "Shiny"
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

        if (!(variantDisplay.showShinyIcon && !variantDisplay.needsSeparateShinyTag && variantDisplay.label == "Shiny")) {
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
                y = (if (isShinyOnlyDisplay) shinyValueY else primaryValueY.toFloat()) / iconScale,
                width = ScannerRenderConstants.SHINY_ICON_SIZE,
                height = ScannerRenderConstants.SHINY_ICON_SIZE,
                scale = iconScale
            )
        }

        drawScaledText(
            context = graphics,
            font = CobblemonResources.DEFAULT_LARGE,
            text = variantText,
            x = textCenterX,
            y = if (isShinyOnlyDisplay) shinyValueY else primaryValueY.toFloat(),
            colour = variantDisplay.colour ?: 0xFFFFFF,
            shadow = true,
            centered = true,
            scale = animatedTextScale
        )

        if (!renderState.showSeparateShinyTag) return

        val shinySide = !isLeftSide
        val shinyXOffsetText = getInfoFrameTextOffset(index, shinySide)
        val shinyXOffset =
            (if (isInnerFrame) -177 else -120) + if (shinySide) 0 else if (isInnerFrame) 234 else 148
        val shinyTextCenterX = centerX + shinyXOffset + shinyXOffsetText
        val shinyText = Component.literal("Shiny").copy().bold()
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
        val header = if (!display.headerTranslationKey.isNullOrBlank()) {
            Component.translatable(display.headerTranslationKey)
        } else {
            Component.literal(display.headerLabel ?: "Info")
        }
        val value = if (!display.labelTranslationKey.isNullOrBlank()) {
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
        val maxWidth = frameWidth - 8
        val baseTextWidth = Minecraft.getInstance().font.width(value.font(CobblemonResources.DEFAULT_LARGE))
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

        drawScaledText(
            context = graphics,
            text = header,
            x = centerX + xOffset + xOffsetText,
            y = headerY,
            shadow = true,
            centered = true,
            scale = headerScale
        )

        val scaledTextWidth = (baseTextWidth * textScale).toInt()
        val textCenterX = centerX + xOffset + xOffsetText
        val textLeftX = textCenterX - (scaledTextWidth / 2)

        if (display.showShinyIcon && animatedTextScale > 0F) {
            val iconScale = ScannerRenderConstants.SHINY_ICON_SCALE
            val iconVisualWidth = (ScannerRenderConstants.SHINY_ICON_SIZE * iconScale).toInt()
            val animatedIconScale = iconScale * animatedTextScale

            blitk(
                matrixStack = poseStack,
                texture = ScannerRenderConstants.SHINY_ICON,
                x = (textLeftX - 2 - iconVisualWidth) / animatedIconScale,
                y = valueY.toFloat() / animatedIconScale,
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
            drawScaledText(
                context = graphics,
                font = CobblemonResources.DEFAULT_LARGE,
                text = symbolText,
                x = textLeftX - 2 - symbolWidth,
                y = valueY.toFloat(),
                colour = if (isMale) 0x32CBFF else 0xFC5454,
                shadow = true,
                scale = animatedTextScale
            )
        }

        drawScaledText(
            context = graphics,
            font = CobblemonResources.DEFAULT_LARGE,
            text = value,
            x = textCenterX,
            y = valueY.toFloat(),
            colour = display.colour ?: 0xFFFFFF,
            shadow = true,
            centered = true,
            scale = animatedTextScale
        )
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

    private fun buildTankRenderState(): VariantRenderState {
        val speciesId = RestorationTankScanState.speciesId?.toString()
        val displays = mutableListOf<VariantDisplay>()

        if (RestorationTankScanState.validSpecies) {
            val formName = RestorationTankScanState.formName
            if (!formName.isNullOrBlank() && !formName.equals("normal", ignoreCase = true)) {
                VariantLabelRegistry.findFormLabel(formName, speciesId)?.let { match ->
                    displays += VariantDisplay(
                        headerLabel = match.headerLabel,
                        headerTranslationKey = match.headerTranslationKey,
                        label = if (!match.labelTranslationKey.isNullOrBlank()) {
                            I18n.get(match.labelTranslationKey)
                        } else {
                            match.label
                        },
                        labelTranslationKey = match.labelTranslationKey,
                        showShinyIcon = false,
                        needsSeparateShinyTag = false
                    )
                }
            }

            RestorationTankScanState.aspects
                .map { it.trim().lowercase().replace('_', '-') }
                .filter { it != "shiny" }
                .mapNotNull { VariantLabelRegistry.findAspectLabel(it, speciesId) }
                .distinctBy { Triple(it.headerLabel, it.headerTranslationKey, it.labelTranslationKey ?: it.label) }
                .forEach { match ->
                    displays += VariantDisplay(
                        headerLabel = match.headerLabel,
                        headerTranslationKey = match.headerTranslationKey,
                        label = if (!match.labelTranslationKey.isNullOrBlank()) {
                            I18n.get(match.labelTranslationKey)
                        } else {
                            match.label
                        },
                        labelTranslationKey = match.labelTranslationKey,
                        showShinyIcon = false,
                        needsSeparateShinyTag = false
                    )
                }
        }

        val distinctDisplays = displays.distinctBy {
            Triple(it.headerLabel, it.headerTranslationKey, it.label)
        }

        return VariantRenderState(
            primaryDisplays = distinctDisplays,
            showSeparateShinyTag = RestorationTankScanState.shiny == true
        )
    }


    private fun getTankProgressPercent(timeRemaining: Int): Int {
        val total = FossilMultiblockStructure.TIME_TO_TAKE
        return (((1f - (timeRemaining / total.toFloat())) * 100f).coerceIn(0f, 100f)).toInt()
    }

    private fun formatSpeciesIdName(speciesId: ResourceLocation): String {
        return speciesId.path
            .split('_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun resolveSpeciesDisplayName(speciesId: ResourceLocation): Component {
        return try {
            val speciesClass = Class.forName("com.cobblemon.mod.common.api.pokemon.PokemonSpecies")
            val species = speciesClass.methods
                .firstOrNull { it.name == "getByIdentifier" && it.parameterCount == 1 }
                ?.invoke(null, speciesId)

            val translatedName = species?.javaClass?.methods
                ?.firstOrNull { it.name == "getTranslatedName" && it.parameterCount == 0 }
                ?.invoke(species) as? Component

            translatedName?.copy() ?: Component.literal(formatSpeciesIdName(speciesId))
        } catch (_: Exception) {
            Component.literal(formatSpeciesIdName(speciesId))
        }
    }

    private fun resolveSpeciesTypeText(speciesId: ResourceLocation): Component? {
        return try {
            val speciesClass = Class.forName("com.cobblemon.mod.common.api.pokemon.PokemonSpecies")
            val species = speciesClass.methods
                .firstOrNull { it.name == "getByIdentifier" && it.parameterCount == 1 }
                ?.invoke(null, speciesId) ?: return null

            val standardForm = species.javaClass.methods
                .firstOrNull { it.name == "getStandardForm" && it.parameterCount == 0 }
                ?.invoke(species) ?: return null

            val types = standardForm.javaClass.methods
                .firstOrNull { it.name == "getTypes" && it.parameterCount == 0 }
                ?.invoke(standardForm) as? Iterable<*> ?: return null

            val typeComponents = types.mapNotNull { typeObj ->
                typeObj?.javaClass?.methods
                    ?.firstOrNull { it.name == "getDisplayName" && it.parameterCount == 0 }
                    ?.invoke(typeObj) as? Component
            }

            if (typeComponents.isEmpty()) return null

            typeComponents.drop(1).fold(typeComponents.first().copy()) { acc, next ->
                acc.plus("/").plus(next.copy())
            }
        } catch (_: Exception) {
            null
        }
    }

}
