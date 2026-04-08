package com.joshiki.betterpokedexscanner.scanner

import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.SectionPos
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructureStart

data class EnvironmentalScanSnapshot(
    val frameDisplays: List<List<VariantDisplay>>,
    val nearbyStructureName: String? = null
)

data class NearbyStructureDebugResult(
    val structureName: String,
    val structurePos: BlockPos,
    val structureMinY: Int? = null,
    val structureMaxY: Int? = null
)

object EnvironmentalScanner {
    private const val MAX_NEARBY_STRUCTURE_DISTANCE = 25.0
    private const val MAX_NEARBY_STRUCTURE_VERTICAL_DISTANCE = 25

    fun sample(player: LocalPlayer): EnvironmentalScanSnapshot {
        val level = player.level()
        val pos = BlockPos.containing(player.x, player.y, player.z)

        val biomeName = resolveBiomeName(level, pos)
        val timeLabel = "${resolveTimeBand(level.dayTime())} - ${formatTime24(level.dayTime())}"
        val weatherLabel = when {
            level.isThundering -> "Thunder"
            level.isRaining -> "Rain"
            else -> "Clear"
        }
        val terrainDisplays = resolveTerrainDisplays(player, pos)
        val canSeeSky = level.canSeeSky(pos)
        val skyLight = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos)
        val blockLight = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos)

        val frameDisplays = listOf(
            buildList {
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.environment.biome",
                        label = biomeName
                    )
                )
                addAll(terrainDisplays)
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.environment.dimension",
                        label = resolveDimensionName(level)
                    )
                )
            },
            buildList {
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.environment.time",
                        label = timeLabel
                    )
                )
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.environment.moon_phase",
                        label = "",
                        labelTranslationKey = "betterpokedexscanner.environment.moon_phase.${level.moonPhase}"
                    )
                )
            },
            buildList {
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.environment.weather",
                        label = weatherLabel
                    )
                )
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.environment.sky_visibility",
                        labelTranslationKey = if (canSeeSky) {
                            "betterpokedexscanner.environment.sky_visibility.visible"
                        } else {
                            "betterpokedexscanner.environment.sky_visibility.not_visible"
                        },
                        label = ""
                    )
                )
            },
            buildList {
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.environment.sky_light",
                        label = formatLightLabel(skyLight),
                        colour = resolveLightColour(skyLight)
                    )
                )
                add(
                    VariantDisplay(
                        headerTranslationKey = "betterpokedexscanner.header.environment.block_light",
                        label = formatLightLabel(blockLight),
                        colour = resolveLightColour(blockLight)
                    )
                )
            }
        )

        return EnvironmentalScanSnapshot(
            frameDisplays = frameDisplays.map { displays ->
                displays.distinctBy { Triple(it.headerLabel, it.headerTranslationKey, it.labelTranslationKey ?: it.label) }
            }
        )
    }

    private fun resolveBiomeName(level: net.minecraft.world.level.Level, pos: BlockPos): String {
        val biomeKey = level.getBiome(pos).unwrapKey().orElse(null)?.location()
        if (biomeKey != null) {
            val translated = Component.translatable("biome.${biomeKey.namespace}.${biomeKey.path}").string
            if (!translated.startsWith("biome.")) {
                return translated
            }
        }

        return biomeKey?.path
            ?.split('_', '-')
            ?.joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            ?: "Unknown"
    }

    private fun resolveDimensionName(level: net.minecraft.world.level.Level): String {
        val dimensionId = level.dimension().location()
        val translated = Component.translatable("dimension.${dimensionId.namespace}.${dimensionId.path}").string
        if (!translated.startsWith("dimension.")) {
            return translated
        }

        return dimensionId.path
            .split('_', '-')
            .joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    private fun resolveTimeBand(dayTime: Long): String {
        val hour = ((dayTime / 1000L + 6L) % 24L).toInt()
        return when (hour) {
            in 6..10 -> "Day"
            in 11..17 -> "Day"
            18, 19 -> "Dusk"
            in 20..22 -> "Night"
            23, 0, 1, 2, 3 -> "Night"
            else -> "Dawn"
        }
    }

    private fun formatTime24(dayTime: Long): String {
        val totalMinutes = (((dayTime % 24000L) * 1440L) / 24000L + 360L) % 1440L
        val hour = (totalMinutes / 60L).toInt()
        val minute = (totalMinutes % 60L).toInt()
        return "%02d:%02d".format(hour, minute)
    }

    private fun formatLightLabel(lightLevel: Int): String {
        return "${resolveLightDescriptor(lightLevel)} ($lightLevel)"
    }

    private fun resolveLightDescriptor(lightLevel: Int): String {
        return when {
            lightLevel >= 12 -> "Bright"
            lightLevel >= 7 -> "Dim"
            else -> "Dark"
        }
    }

    private fun resolveLightColour(lightLevel: Int): Int {
        val clamped = lightLevel.coerceIn(0, 15)
        return if (clamped <= 7) {
            interpolateColour(0x6B7280, 0xFFFFFF, clamped / 7f)
        } else {
            interpolateColour(0xFFFFFF, 0xFFD35A, (clamped - 7) / 8f)
        }
    }

    private fun interpolateColour(start: Int, end: Int, progress: Float): Int {
        val clamped = progress.coerceIn(0f, 1f)
        val startRed = (start shr 16) and 0xFF
        val startGreen = (start shr 8) and 0xFF
        val startBlue = start and 0xFF
        val endRed = (end shr 16) and 0xFF
        val endGreen = (end shr 8) and 0xFF
        val endBlue = end and 0xFF

        val red = (startRed + ((endRed - startRed) * clamped)).toInt()
        val green = (startGreen + ((endGreen - startGreen) * clamped)).toInt()
        val blue = (startBlue + ((endBlue - startBlue) * clamped)).toInt()

        return (red shl 16) or (green shl 8) or blue
    }

    private fun resolveTerrainDisplays(player: LocalPlayer, pos: BlockPos): List<VariantDisplay> {
        val level = player.level()
        val seaLevel = level.seaLevel
        val canSeeSky = level.canSeeSky(pos)
        val displays = mutableListOf<VariantDisplay>()

        if (player.isUnderWater) {
            displays += VariantDisplay(
                headerTranslationKey = "betterpokedexscanner.header.environment.terrain",
                label = "",
                labelTranslationKey = "betterpokedexscanner.environment.terrain.underwater"
            )
        } else if (player.isInWater) {
            displays += VariantDisplay(
                headerTranslationKey = "betterpokedexscanner.header.environment.terrain",
                label = "",
                labelTranslationKey = "betterpokedexscanner.environment.terrain.on_water"
            )
        }

        if (!canSeeSky && pos.y < seaLevel) {
            displays += VariantDisplay(
                headerTranslationKey = "betterpokedexscanner.header.environment.terrain",
                label = "",
                labelTranslationKey = "betterpokedexscanner.environment.terrain.underground"
            )
        } else if (canSeeSky) {
            displays += VariantDisplay(
                headerTranslationKey = "betterpokedexscanner.header.environment.terrain",
                label = "",
                labelTranslationKey = "betterpokedexscanner.environment.terrain.surface"
            )
        }

        if (!canSeeSky) {
            displays += VariantDisplay(
                headerTranslationKey = "betterpokedexscanner.header.environment.terrain",
                label = "",
                labelTranslationKey = "betterpokedexscanner.environment.terrain.in_cave"
            )
        }

        if (pos.y >= seaLevel + 25) {
            displays += VariantDisplay(
                headerTranslationKey = "betterpokedexscanner.header.environment.terrain",
                label = "",
                labelTranslationKey = "betterpokedexscanner.environment.terrain.high_altitude"
            )
        }

        return displays.distinctBy { it.labelTranslationKey ?: it.label }
    }

    fun resolveNearbyStructureResult(level: ServerLevel, pos: BlockPos): NearbyStructureDebugResult? {
        resolveStructureAtPosition(level, pos)?.let { structureName ->
            return NearbyStructureDebugResult(
                structureName = structureName,
                structurePos = pos
            )
        }
        return resolveNearestStructure(level, pos)
    }

    private fun resolveStructureAtPosition(level: ServerLevel, pos: BlockPos): String? {
        val start = level.structureManager().getStructureWithPieceAt(pos) { holder -> true }
        if (!start.isValid) return null
        return formatStructure(level, start.structure)
    }

    private fun resolveNearestStructure(level: ServerLevel, pos: BlockPos): NearbyStructureDebugResult? {
        val structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE)
        val structureHolders = structureRegistry.listElements().toList()
        if (structureHolders.isEmpty()) return null

        val nearest = level.chunkSource.generator.findNearestMapStructure(
            level,
            HolderSet.direct(structureHolders),
            pos,
            8,
            false
        ) ?: return null

        if (horizontalDistance(pos, nearest.first) > MAX_NEARBY_STRUCTURE_DISTANCE) {
            return null
        }

        val structureName = formatStructureHolder(level, nearest.second) ?: return null
        val structureStart = resolveNearbyStructureStart(level, nearest.first, nearest.second.value())
        val boundingBox = structureStart?.takeIf { it.isValid }?.boundingBox
        if (boundingBox != null && isOutsideVerticalRange(pos.y, boundingBox.minY(), boundingBox.maxY())) {
            return null
        }
        return NearbyStructureDebugResult(
            structureName = structureName,
            structurePos = nearest.first,
            structureMinY = boundingBox?.minY(),
            structureMaxY = boundingBox?.maxY()
        )
    }

    private fun isOutsideVerticalRange(playerY: Int, minY: Int, maxY: Int): Boolean {
        return playerY < (minY - MAX_NEARBY_STRUCTURE_VERTICAL_DISTANCE) ||
            playerY > (maxY + MAX_NEARBY_STRUCTURE_VERTICAL_DISTANCE)
    }

    private fun resolveNearbyStructureStart(
        level: ServerLevel,
        locatePos: BlockPos,
        structure: Structure
    ): StructureStart? {
        val originChunk = ChunkPos(locatePos)
        val candidateStarts = mutableListOf<StructureStart>()

        for (chunkX in (originChunk.x - 2)..(originChunk.x + 2)) {
            for (chunkZ in (originChunk.z - 2)..(originChunk.z + 2)) {
                val chunkPos = ChunkPos(chunkX, chunkZ)
                val chunk = level.getChunk(chunkX, chunkZ)
                val sectionPos = SectionPos.of(chunkPos, level.minSection)
                val start = level.structureManager().getStartForStructure(sectionPos, structure, chunk)
                if (start != null && start.isValid) {
                    candidateStarts.add(start)
                }
            }
        }

        return candidateStarts.minByOrNull { start ->
            val box = start.boundingBox
            val centerX = (box.minX() + box.maxX()) / 2.0
            val centerZ = (box.minZ() + box.maxZ()) / 2.0
            val deltaX = centerX - locatePos.x
            val deltaZ = centerZ - locatePos.z
            (deltaX * deltaX) + (deltaZ * deltaZ)
        }
    }

    private fun horizontalDistance(origin: BlockPos, target: BlockPos): Double {
        val deltaX = (origin.x - target.x).toDouble()
        val deltaZ = (origin.z - target.z).toDouble()
        return kotlin.math.sqrt((deltaX * deltaX) + (deltaZ * deltaZ))
    }

    private fun formatStructure(level: Level, structure: Structure): String? {
        val structureId = level.registryAccess()
            .lookupOrThrow(Registries.STRUCTURE)
            .listElements()
            .toList()
            .firstOrNull { it.value() == structure }
            ?.unwrapKey()
            ?.orElse(null)
            ?.location()
            ?: return null
        return formatResourcePath(structureId.path)
    }

    private fun formatStructureHolder(level: Level, holder: Holder<Structure>): String? {
        val structure = holder.value()
        return formatStructure(level, structure)
    }

    private fun formatResourcePath(path: String): String {
        return path
            .split('/', '_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
}
