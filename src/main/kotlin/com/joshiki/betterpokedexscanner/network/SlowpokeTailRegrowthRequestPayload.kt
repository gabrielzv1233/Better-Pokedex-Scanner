package com.joshiki.betterpokedexscanner.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class SlowpokeTailRegrowthRequestPayload(
    val entityId: Int
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<SlowpokeTailRegrowthRequestPayload> = TYPE

    companion object {
        val ID = ResourceLocation.fromNamespaceAndPath("better_pokedex_scanner", "slowpoke_tail_regrowth_request")
        val TYPE = CustomPacketPayload.Type<SlowpokeTailRegrowthRequestPayload>(ID)

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SlowpokeTailRegrowthRequestPayload> =
            StreamCodec.of(
                { buf, payload -> buf.writeInt(payload.entityId) },
                { buf -> SlowpokeTailRegrowthRequestPayload(buf.readInt()) }
            )
    }
}