package com.vibey.imitari.api.registration;

import com.vibey.imitari.api.CopyBlockAPI;
import com.vibey.imitari.api.ICopyBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.jetbrains.annotations.ApiStatus;

public class CopyBlockRegistration {

    public static int registerForMod(String modId) {
        // Register with API (safe on both sides)
        int count = CopyBlockAPI.autoRegisterModBlocks(modId);

        // Register with model system (CLIENT ONLY)
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            com.vibey.imitari.client.CopyBlockModelProvider.autoRegisterForMod(modId);
        });

        return count;
    }

    public static void registerBlock(ResourceLocation blockId) {
        CopyBlockAPI.registerCopyBlock(blockId);

        // CLIENT ONLY
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            com.vibey.imitari.client.CopyBlockModelProvider.registerBlock(blockId);
        });
    }

    public static void registerBlock(Block block) {
        CopyBlockAPI.registerCopyBlock(block);

        // CLIENT ONLY
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            com.vibey.imitari.client.CopyBlockModelProvider.registerBlock(block);
        });
    }

    public static int registerBlocks(Block... blocks) {
        int count = 0;
        for (Block block : blocks) {
            if (block instanceof ICopyBlock) {
                registerBlock(block);
                count++;
            }
        }
        return count;
    }

    public static int registerBlocks(ResourceLocation... blockIds) {
        for (ResourceLocation id : blockIds) {
            registerBlock(id);
        }
        return blockIds.length;
    }

    @ApiStatus.Internal
    public static void initializeImitari() {
        registerForMod("imitari");
    }
}