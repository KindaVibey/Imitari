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
        // Get the block model location - this MUST match your blockstate JSON
        ModelResourceLocation blockModelLocation = new ModelResourceLocation(
                new ResourceLocation(CopyCraft.MODID, "copy_block"),
                "inventory"
        );

        // Try to get the existing model
        BakedModel existingModel = event.getModels().get(blockModelLocation);

        if (existingModel != null) {
            // Wrap it in our custom model
            CopyBlockModel customModel = new CopyBlockModel(existingModel);
            event.getModels().put(blockModelLocation, customModel);
        }

        // Also register for the block state variant
        for (String variant : new String[]{"", "inventory"}) {
            ModelResourceLocation loc = new ModelResourceLocation(
                    new ResourceLocation(CopyCraft.MODID, "copy_block"),
                    variant
            );

            existingModel = event.getModels().get(loc);
            if (existingModel != null) {
                event.getModels().put(loc, new CopyBlockModel(existingModel));
            }
        }
    }
}