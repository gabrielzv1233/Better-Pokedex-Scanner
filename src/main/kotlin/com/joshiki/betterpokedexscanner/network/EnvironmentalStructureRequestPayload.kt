package com.joshiki.betterpokedexscanner.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class EnvironmentalStructureRequestPayload(
    val sessionId: Int
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<EnvironmentalStructureRequestPayload> = TYPE

    companion object {
        val ID = ResourceLocation.fromNamespaceAndPath("better_pokedex_scanner", "environment_structure_request")
        val TYPE = CustomPacketPayload.Type<EnvironmentalStructureRequestPayload>(ID)

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, EnvironmentalStructureRequestPayload> =
            StreamCodec.of(
                { buf, payload -> buf.writeInt(payload.sessionId) },
                { buf -> EnvironmentalStructureRequestPayload(buf.readInt()) }
            )
    }
}
