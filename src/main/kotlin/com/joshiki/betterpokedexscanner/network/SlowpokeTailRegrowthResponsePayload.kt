package com.joshiki.betterpokedexscanner.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class SlowpokeTailRegrowthResponsePayload(
    val entityId: Int,
    val percent: Int
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<SlowpokeTailRegrowthResponsePayload> = TYPE

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath("better_pokedex_scanner", "slowpoke_tail_regrowth_response")
        val TYPE = CustomPacketPayload.Type<SlowpokeTailRegrowthResponsePayload>(ID)

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SlowpokeTailRegrowthResponsePayload> =
            StreamCodec.of(
                { buf, payload ->
                    buf.writeInt(payload.entityId)
                    buf.writeInt(payload.percent)
                },
                { buf ->
                    SlowpokeTailRegrowthResponsePayload(
                        entityId = buf.readInt(),
                        percent = buf.readInt()
                    )
                }
            )
    }
}
