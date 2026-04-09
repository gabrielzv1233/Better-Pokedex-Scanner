package com.joshiki.betterpokedexscanner.network

import net.minecraft.core.BlockPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceLocation

data class BeehiveDetailsResponsePayload(
    val blockPos: BlockPos,
    val occupantCount: Int,
    val maxOccupants: Int,
    val beeCount: Int,
    val combeeCount: Int,
    val unknownCount: Int,
    val femaleRareSpeciesId: String?,
    val shinyRareSpeciesId: String?,
    val sedated: Boolean,
    val hasSavedFlowerPos: Boolean
) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<BeehiveDetailsResponsePayload> = TYPE

    companion object {
        val ID = ResourceLocation.fromNamespaceAndPath("better_pokedex_scanner", "beehive_details_response")
        val TYPE = CustomPacketPayload.Type<BeehiveDetailsResponsePayload>(ID)

        val CODEC: StreamCodec<RegistryFriendlyByteBuf, BeehiveDetailsResponsePayload> =
            StreamCodec.of(
                { buf, payload ->
                    buf.writeBlockPos(payload.blockPos)
                    buf.writeInt(payload.occupantCount)
                    buf.writeInt(payload.maxOccupants)
                    buf.writeInt(payload.beeCount)
                    buf.writeInt(payload.combeeCount)
                    buf.writeInt(payload.unknownCount)
                    buf.writeNullable(payload.femaleRareSpeciesId) { innerBuf, value -> innerBuf.writeUtf(value) }
                    buf.writeNullable(payload.shinyRareSpeciesId) { innerBuf, value -> innerBuf.writeUtf(value) }
                    buf.writeBoolean(payload.sedated)
                    buf.writeBoolean(payload.hasSavedFlowerPos)
                },
                { buf ->
                    BeehiveDetailsResponsePayload(
                        blockPos = buf.readBlockPos(),
                        occupantCount = buf.readInt(),
                        maxOccupants = buf.readInt(),
                        beeCount = buf.readInt(),
                        combeeCount = buf.readInt(),
                        unknownCount = buf.readInt(),
                        femaleRareSpeciesId = buf.readNullable { innerBuf -> innerBuf.readUtf() },
                        shinyRareSpeciesId = buf.readNullable { innerBuf -> innerBuf.readUtf() },
                        sedated = buf.readBoolean(),
                        hasSavedFlowerPos = buf.readBoolean()
                    )
                }
            )
    }
}
