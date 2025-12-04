package com.vibey.imitari.client;

import com.vibey.imitari.block.ICopyBlock;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Centralized model registration system for ICopyBlock implementations.
 * Addon mods can register their own blocks by calling registerBlock().
 */
public class CopyBlockModelProvider {

    private static final Set<ResourceLocation> REGISTERED_BLOCKS = new HashSet<>();
    private static boolean modelsWrapped = false; // prevent infinite wrapping/reload loops

    public static void registerBlock(ResourceLocation blockId) {
        REGISTERED_BLOCKS.add(blockId);
    }

    public static void registerBlock(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id != null) {
            registerBlock(id);
        }
    }

    public static void autoRegisterForMod(String modId) {
        for (Map.Entry<net.minecraft.resources.ResourceKey<Block>, Block> entry : ForgeRegistries.BLOCKS.getEntries()) {
            ResourceLocation id = entry.getKey().location();
            Block block = entry.getValue();

            if (id.getNamespace().equals(modId) && block instanceof ICopyBlock copyBlock && copyBlock.useDynamicModel()) {
                registerBlock(id);
            }
        }
    }

    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        // Prevent multiple wrapping that could cause reload loops
        if (modelsWrapped) return;
        modelsWrapped = true;

        Map<ResourceLocation, BakedModel> modelRegistry = event.getModels();

        for (Map.Entry<ResourceLocation, BakedModel> entry : modelRegistry.entrySet()) {
            ResourceLocation id = entry.getKey();

            for (ResourceLocation blockId : REGISTERED_BLOCKS) {
                if (!id.getNamespace().equals(blockId.getNamespace())) continue;

                String path = id.getPath();
                String blockName = blockId.getPath();

                if (path.equals(blockName) ||
                        path.equals(blockName + "_top") ||
                        path.equals(blockName + "_slab") ||
                        path.equals(blockName + "_stairs")) {

                    BakedModel existingModel = entry.getValue();
                    CopyBlockModel wrappedModel = new CopyBlockModel(existingModel);
                    modelRegistry.put(id, wrappedModel);
                }
            }
        }
    }

    public static Set<ResourceLocation> getRegisteredBlocks() {
        return new HashSet<>(REGISTERED_BLOCKS);
    }

    public static void clearRegistrations() {
        REGISTERED_BLOCKS.clear();
        modelsWrapped = false;
    }
}
