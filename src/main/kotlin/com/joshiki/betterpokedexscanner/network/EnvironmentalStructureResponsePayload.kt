package com.joshiki.betterpokedexscanner.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class EnvironmentalStructureResponsePayload(
    val sessionId: Int,
    val structureName: String?
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<EnvironmentalStructureResponsePayload> = TYPE

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath("better_pokedex_scanner", "environment_structure_response")
        val TYPE = CustomPacketPayload.Type<EnvironmentalStructureResponsePayload>(ID)

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, EnvironmentalStructureResponsePayload> =
            StreamCodec.of(
                { buf, payload ->
                    buf.writeInt(payload.sessionId)
                    buf.writeBoolean(payload.structureName != null)
                    if (payload.structureName != null) {
                        buf.writeUtf(payload.structureName)
                    }
                },
                { buf ->
                    EnvironmentalStructureResponsePayload(
                        sessionId = buf.readInt(),
                        structureName = if (buf.readBoolean()) buf.readUtf() else null
                    )
                }
            )
    }
}
