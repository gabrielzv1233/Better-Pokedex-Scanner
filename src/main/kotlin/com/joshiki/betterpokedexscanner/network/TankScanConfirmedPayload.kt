package com.joshiki.betterpokedexscanner.network

import com.cobblemon.mod.common.api.pokedex.PokedexLearnedInformation
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class TankScanConfirmedPayload(
    val speciesId: ResourceLocation,
    val newInformation: PokedexLearnedInformation
) : CustomPacketPayload {
    override fun type() = TYPE

    companion object {
        val ID = ResourceLocation.fromNamespaceAndPath("better_pokedex_scanner", "tank_scan_confirmed")
        val TYPE = CustomPacketPayload.Type<TankScanConfirmedPayload>(ID)

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, TankScanConfirmedPayload> =
            StreamCodec.of(
                { buf, payload ->
                    buf.writeResourceLocation(payload.speciesId)
                    buf.writeEnum(payload.newInformation)
                },
                { buf ->
                    TankScanConfirmedPayload(
                        speciesId = buf.readResourceLocation(),
                        newInformation = buf.readEnum(PokedexLearnedInformation::class.java)
                    )
                }
            )
    }
}