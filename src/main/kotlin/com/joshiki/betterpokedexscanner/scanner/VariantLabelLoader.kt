package com.joshiki.betterpokedexscanner.scanner

import com.google.gson.Gson
import com.google.gson.JsonParseException
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.Resource
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import java.io.InputStreamReader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

object VariantLabelLoader : IdentifiableResourceReloadListener {
    private val gson = Gson()

    fun register() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(this)
    }

    override fun getFabricId(): ResourceLocation {
        return ResourceLocation.fromNamespaceAndPath("chimeric_shinies", "variant_label_loader")
    }

    override fun reload(
        synchronizer: net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier,
        manager: ResourceManager,
        preparationsProfiler: ProfilerFiller,
        reloadProfiler: ProfilerFiller,
        backgroundExecutor: Executor,
        gameExecutor: Executor
    ): CompletableFuture<Void> {
        return CompletableFuture
            .supplyAsync({
                val merged = mutableListOf<VariantLabelFile>()
                val resources: Map<ResourceLocation, List<Resource>> =
                    manager.listResourceStacks("variant_labels") { it.path.endsWith(".json") }

                for ((_, stack) in resources) {
                    for (resource in stack) {
                        resource.open().use { stream ->
                            InputStreamReader(stream).use { reader ->
                                try {
                                    merged += gson.fromJson(reader, VariantLabelFile::class.java)
                                } catch (_: JsonParseException) {
                                }
                            }
                        }
                    }
                }

                merged
            }, backgroundExecutor)
            .thenCompose(synchronizer::wait)
            .thenAcceptAsync({ files ->
                VariantLabelRegistry.clear()
                files.forEach { VariantLabelRegistry.apply(it) }
            }, gameExecutor)
    }
}