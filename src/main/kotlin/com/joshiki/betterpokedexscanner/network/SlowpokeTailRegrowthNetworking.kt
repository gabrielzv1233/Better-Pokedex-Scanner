package com.joshiki.betterpokedexscanner.network

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

object SlowpokeTailRegrowthNetworking {

    fun registerCommon() {
        PayloadTypeRegistry.playC2S().register(
            SlowpokeTailRegrowthRequestPayload.TYPE,
            SlowpokeTailRegrowthRequestPayload.CODEC
        )
        PayloadTypeRegistry.playS2C().register(
            SlowpokeTailRegrowthResponsePayload.TYPE,
            SlowpokeTailRegrowthResponsePayload.CODEC
        )

        ServerPlayNetworking.registerGlobalReceiver(SlowpokeTailRegrowthRequestPayload.TYPE) { payload, context ->
            val player = context.player()
            val entity = player.serverLevel().getEntity(payload.entityId) as? PokemonEntity ?: return@registerGlobalReceiver

            val speciesId = entity.pokemon.species.resourceIdentifier.toString()
            if (speciesId != "cobblemon:slowpoke") return@registerGlobalReceiver

            val feature = entity.pokemon.features.firstOrNull { it.name == "slowpoke_tail_regrowth" }
                ?: return@registerGlobalReceiver

            val getter = feature.javaClass.methods.firstOrNull { it.name == "getRegrowthSeconds" }
                ?: return@registerGlobalReceiver

            val regrowthSeconds = getter.invoke(feature) as? Int
                ?: return@registerGlobalReceiver

            val maxRegrowthSeconds = 1200
            val percent = (((maxRegrowthSeconds - regrowthSeconds).toFloat() / maxRegrowthSeconds.toFloat()) * 100f)
                .coerceIn(0f, 100f)
                .toInt()

            ServerPlayNetworking.send(
                player,
                SlowpokeTailRegrowthResponsePayload(entity.id, percent)
            )
        }
    }
}