package com.joshiki.betterpokedexscanner.scanner

import com.joshiki.betterpokedexscanner.network.BeehiveDetailsResponsePayload
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.BeehiveBlock
import net.minecraft.world.level.block.entity.BeehiveBlockEntity
import net.minecraft.world.phys.BlockHitResult

data class BeehiveScanSnapshot(
    val blockPos: BlockPos,
    val honeyLevel: Int,
    val blockLabel: String,
    val occupantCount: Int? = null,
    val maxOccupants: Int? = null,
    val beeCount: Int? = null,
    val combeeCount: Int? = null,
    val unknownCount: Int? = null,
    val sedated: Boolean = false,
    val hasSavedFlowerPos: Boolean? = null
) {
    val frameDisplays: List<List<VariantDisplay>>
        get() = buildFrameDisplays()

    fun withServerDetails(payload: BeehiveDetailsResponsePayload): BeehiveScanSnapshot {
        return copy(
            occupantCount = payload.occupantCount,
            maxOccupants = payload.maxOccupants,
            beeCount = payload.beeCount,
            combeeCount = payload.combeeCount,
            unknownCount = payload.unknownCount,
            sedated = payload.sedated,
            hasSavedFlowerPos = payload.hasSavedFlowerPos
        )
    }

    private fun buildFrameDisplays(): List<List<VariantDisplay>> {
        val honeyPercent = ((honeyLevel / 5f) * 100f).toInt().coerceIn(0, 100)
        val honeyColour = when (honeyLevel) {
            5 -> 0xFFD35A
            in 3..4 -> 0xF6C453
            in 1..2 -> 0xD6A845
            else -> 0xB8B8B8
        }

        val occupancyLabel = when {
            occupantCount == null || maxOccupants == null -> "Reading..."
            occupantCount <= 0 -> "Empty"
            occupantCount >= maxOccupants -> "Full"
            else -> "$occupantCount / $maxOccupants"
        }

        val occupancyColour = when {
            occupantCount == null || maxOccupants == null -> 0xD1D5DB
            occupantCount <= 0 -> 0x9CA3AF
            occupantCount >= maxOccupants -> 0x6EE7B7
            else -> 0xFFFFFF
        }

        val inhabitantsDisplays = buildList {
            if ((occupantCount ?: 0) > 0 && combeeCount != null && beeCount != null && unknownCount != null) {
                if ((combeeCount ?: 0) > 0) {
                    add(
                        VariantDisplay(
                            headerLabel = "Inhabitants",
                            label = "Combee x${combeeCount ?: 0}",
                            colour = 0xF7D774
                        )
                    )
                }

                if ((beeCount ?: 0) > 0) {
                    add(
                        VariantDisplay(
                            headerLabel = "Inhabitants",
                            label = "Bee x${beeCount ?: 0}",
                            colour = 0xFFF4BF
                        )
                    )
                }

                if ((unknownCount ?: 0) > 0) {
                    add(
                        VariantDisplay(
                            headerLabel = "Inhabitants",
                            label = "Unknown x${unknownCount ?: 0}",
                            colour = 0xD1D5DB
                        )
                    )
                }
            }
        }

        val statusDisplays = buildList {
            if (sedated) {
                add(
                    VariantDisplay(
                        headerLabel = "Condition",
                        label = "Sedated",
                        colour = 0x9FE8B8
                    )
                )
            }

            add(
                VariantDisplay(
                    headerLabel = "Flowers",
                    label = when (hasSavedFlowerPos) {
                        true -> "Flowers Nearby"
                        false -> "No Flowers Nearby"
                        null -> "Reading..."
                    },
                    colour = when (hasSavedFlowerPos) {
                        true -> 0xA7F3D0
                        false -> 0xD1D5DB
                        null -> 0xD1D5DB
                    }
                )
            )
        }

        return buildList {
            add(
                listOf(
                    VariantDisplay(
                        headerLabel = "Honey Level",
                        label = "$honeyPercent%",
                        colour = honeyColour
                    )
                )
            )
            add(
                listOf(
                    VariantDisplay(
                        headerLabel = "Occupancy",
                        label = occupancyLabel,
                        colour = occupancyColour
                    )
                )
            )
            if (inhabitantsDisplays.isNotEmpty()) {
                add(inhabitantsDisplays)
            }
            add(
                statusDisplays.ifEmpty {
                    listOf(
                        VariantDisplay(
                            headerLabel = blockLabel,
                            label = "Stable",
                            colour = 0xFFFFFF
                        )
                    )
                }
            )
        }
    }
}

object BeehiveScanner {

    fun findBeehiveTarget(user: LocalPlayer): BeehiveScanSnapshot? {
        val hit = Minecraft.getInstance().hitResult as? BlockHitResult ?: return null
        val level = user.level()
        val pos = hit.blockPos
        val state = level.getBlockState(pos)
        val block = state.block as? BeehiveBlock ?: return null
        level.getBlockEntity(pos) as? BeehiveBlockEntity ?: return null

        val honeyLevel = state.getValue(BeehiveBlock.HONEY_LEVEL)

        return BeehiveScanSnapshot(
            blockPos = pos,
            honeyLevel = honeyLevel,
            blockLabel = if (block == net.minecraft.world.level.block.Blocks.BEE_NEST) "Bee Nest" else "Beehive"
        )
    }
}
