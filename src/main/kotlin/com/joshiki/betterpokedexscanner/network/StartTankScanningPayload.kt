package com.joshiki.betterpokedexscanner.network

import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class StartTankScanningPayload(
    val blockPos: BlockPos,
    val speciesId: ResourceLocation
) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath("better_pokedex_scanner", "start_tank_scanning")
        val TYPE = CustomPacketPayload.Type<StartTankScanningPayload>(ID)

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, StartTankScanningPayload> =
            StreamCodec.of(
                { buf, payload ->
                    buf.writeBlockPos(payload.blockPos)
                    buf.writeResourceLocation(payload.speciesId)
                },
                { buf ->
                    StartTankScanningPayload(
                        blockPos = buf.readBlockPos(),
                        speciesId = buf.readResourceLocation()
                    )
                }
            )
    }
}
