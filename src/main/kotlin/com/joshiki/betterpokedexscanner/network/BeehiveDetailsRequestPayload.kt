package com.joshiki.betterpokedexscanner.network

import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class BeehiveDetailsRequestPayload(
    val blockPos: BlockPos
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<BeehiveDetailsRequestPayload> = TYPE

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath("better_pokedex_scanner", "beehive_details_request")
        val TYPE = CustomPacketPayload.Type<BeehiveDetailsRequestPayload>(ID)

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, BeehiveDetailsRequestPayload> =
            StreamCodec.of(
                { buf, payload -> buf.writeBlockPos(payload.blockPos) },
                { buf -> BeehiveDetailsRequestPayload(buf.readBlockPos()) }
            )
    }
}
