package com.joshiki.betterpokedexscanner.network

import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class FinishTankScanningPayload(
    val blockPos: BlockPos,
    val speciesId: ResourceLocation
) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
        val ID = ResourceLocation.fromNamespaceAndPath("better_pokedex_scanner", "finish_tank_scanning")
        val TYPE = CustomPacketPayload.Type<FinishTankScanningPayload>(ID)

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, FinishTankScanningPayload> =
            StreamCodec.of(
                { buf, payload ->
                    buf.writeBlockPos(payload.blockPos)
                    buf.writeResourceLocation(payload.speciesId)
                },
                { buf ->
                    FinishTankScanningPayload(
                        blockPos = buf.readBlockPos(),
                        speciesId = buf.readResourceLocation()
                    )
                }
            )
    }
}