package com.vibey.imitari.client;

import com.vibey.imitari.api.ICopyBlock;
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

    // Multipart sub-models that need special handling
    private static final Set<String> MULTIPART_MODELS = Set.of(
            "fence_post", "fence_side",
            "wall_post", "wall_side", "wall_side_tall",
            "pane_post", "pane_side", "pane_side_alt", "pane_noside", "pane_noside_alt"
    );

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
        Map<ResourceLocation, BakedModel> modelRegistry = event.getModels();
        int wrappedCount = 0;

        for (Map.Entry<ResourceLocation, BakedModel> entry : modelRegistry.entrySet()) {
            ResourceLocation modelId = entry.getKey();
            BakedModel existingModel = entry.getValue();

            // Skip if already wrapped
            if (existingModel instanceof CopyBlockModel || existingModel instanceof MultipartCopyBlockModel) {
                continue;
            }

            String path = modelId.getPath();
            boolean shouldWrap = false;
            boolean isMultipart = false;

            // Check if this is an Imitari copy_block model
            if (modelId.getNamespace().equals("imitari") && path.startsWith("block/copy_block")) {
                shouldWrap = true;

                // Check if it's a multipart sub-model
                for (String multipartSuffix : MULTIPART_MODELS) {
                    if (path.endsWith(multipartSuffix)) {
                        isMultipart = true;
                        break;
                    }
                }
            }

            // Also match item models
            if (modelId.getNamespace().equals("imitari") && path.startsWith("item/copy_block")) {
                shouldWrap = true;
            }

            // Check registered blocks for addon compatibility
            if (!shouldWrap) {
                for (ResourceLocation blockId : REGISTERED_BLOCKS) {
                    if (!modelId.getNamespace().equals(blockId.getNamespace())) {
                        continue;
                    }

                    String blockName = blockId.getPath();

                    if (path.equals("block/" + blockName) ||
                            path.equals(blockName) ||
                            path.startsWith("block/" + blockName + "_")) {
                        shouldWrap = true;

                        // Check if it's a multipart model
                        for (String multipartSuffix : MULTIPART_MODELS) {
                            if (path.endsWith(multipartSuffix)) {
                                isMultipart = true;
                                break;
                            }
                        }
                        break;
                    }
                }
            }

            if (shouldWrap) {
                BakedModel wrappedModel;
                if (isMultipart) {
                    // Use special multipart wrapper
                    wrappedModel = new MultipartCopyBlockModel(existingModel);
                } else {
                    // Use regular wrapper
                    wrappedModel = new CopyBlockModel(existingModel);
                }
                modelRegistry.put(modelId, wrappedModel);
                wrappedCount++;
            }
        }

        System.out.println("[Imitari] Wrapped " + wrappedCount + " CopyBlock models (including multipart sub-models)");
    }

    public static Set<ResourceLocation> getRegisteredBlocks() {
        return new HashSet<>(REGISTERED_BLOCKS);
    }

    public static void clearRegistrations() {
        REGISTERED_BLOCKS.clear();
    }
}