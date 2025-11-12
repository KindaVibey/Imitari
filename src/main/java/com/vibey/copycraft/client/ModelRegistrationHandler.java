package com.vibey.copycraft.client;

import com.vibey.copycraft.CopyCraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CopyCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModelRegistrationHandler {

    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        System.out.println("======== MODEL BAKING EVENT ========");

        // Try all possible model locations
        ResourceLocation blockId = new ResourceLocation(CopyCraft.MODID, "copy_block");

        // Try different model resource locations
        ModelResourceLocation[] locations = {
                new ModelResourceLocation(blockId, ""),
                new ModelResourceLocation(blockId, "inventory")
        };

        boolean found = false;
        for (ModelResourceLocation loc : locations) {
            BakedModel existingModel = event.getModels().get(loc);

            if (existingModel != null) {
                System.out.println("Found model at: " + loc);
                CopyBlockModel customModel = new CopyBlockModel(existingModel);
                event.getModels().put(loc, customModel);
                System.out.println("Replaced with CopyBlockModel!");
                found = true;
            } else {
                System.out.println("No model at: " + loc);
            }
        }

        if (!found) {
            System.out.println("WARNING: Could not find copy_block model!");
            System.out.println("Available models:");
            event.getModels().keySet().stream()
                    .filter(rl -> rl.toString().contains("copycraft"))
                    .forEach(rl -> System.out.println("  - " + rl));
        }
    }
}