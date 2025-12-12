package com.vibey.imitari;

import com.mojang.logging.LogUtils;
import com.vibey.imitari.client.CopyBlockModelProvider;
import com.vibey.imitari.registry.ModBlockEntities;
import com.vibey.imitari.registry.ModBlocks;
import com.vibey.imitari.registry.ModItems;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Imitari.MODID)
public class Imitari {
    public static final String MODID = "imitari";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Imitari() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        System.out.println("Imitari constructor called!");

        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        // Manually register our blocks (safer than auto-register)
        CopyBlockModelProvider.registerBlock(new ResourceLocation(MODID, "copy_block"));
        CopyBlockModelProvider.registerBlock(new ResourceLocation(MODID, "copy_block_ghost"));
        CopyBlockModelProvider.registerBlock(new ResourceLocation(MODID, "copy_block_slab"));
        CopyBlockModelProvider.registerBlock(new ResourceLocation(MODID, "copy_block_stairs"));
        CopyBlockModelProvider.registerBlock(new ResourceLocation(MODID, "copy_block_layer"));

        // Register the new model provider system
        modEventBus.addListener(CopyBlockModelProvider::onModelBake);
        System.out.println("Registered CopyBlock model provider!");

        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Imitari common setup complete!");

        event.enqueueWork(() -> {
            try {
                com.vibey.imitari.vs2.VS2CopyBlockIntegration.register();
                LOGGER.info("Successfully registered VS2 CopyBlock mass provider!");
            } catch (Exception e) {
                LOGGER.error("Failed to register VS2 provider", e);
            }
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.COPY_BLOCK);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Imitari server starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Imitari client setup");

            event.enqueueWork(() -> {
                // FIX: Allow both cutout and translucent render types
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.COPY_BLOCK.get(),
                        rt -> rt == RenderType.cutout() || rt == RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.COPY_BLOCK_GHOST.get(),
                        rt -> rt == RenderType.cutout() || rt == RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.COPY_BLOCK_SLAB.get(),
                        rt -> rt == RenderType.cutout() || rt == RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.COPY_BLOCK_STAIRS.get(),
                        rt -> rt == RenderType.cutout() || rt == RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.COPY_BLOCK_LAYER.get(),
                        rt -> rt == RenderType.cutout() || rt == RenderType.translucent());
            });
        }
    }
}