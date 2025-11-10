package com.vibey.copycraft;

import com.mojang.logging.LogUtils;
import com.vibey.copycraft.registry.ModItems;
import com.vibey.copycraft.registry.ModBlocks;
import com.vibey.copycraft.registry.ModBlockEntities;
import com.vibey.copycraft.client.CopyBlockRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
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

@Mod(CopyCraft.MODID)
public class CopyCraft {
    public static final String MODID = "copycraft";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CopyCraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        // Register our content
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("CopyCraft common setup complete!");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.COPY_BLOCK);
            // Add more copy block variants here as you create them:
            // event.accept(ModItems.COPY_COLUMN);
            // event.accept(ModItems.COPY_SLOPE);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("CopyCraft server starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("CopyCraft client setup");

            // Set render type to cutout for transparency support
            event.enqueueWork(() -> {
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.COPY_BLOCK.get(), RenderType.cutout());
                // Add more as you create them:
                // ItemBlockRenderTypes.setRenderLayer(ModBlocks.COPY_COLUMN.get(), RenderType.cutout());
                // ItemBlockRenderTypes.setRenderLayer(ModBlocks.COPY_SLOPE.get(), RenderType.cutout());
            });
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            // This single renderer handles ALL copy block types!
            event.registerBlockEntityRenderer(ModBlockEntities.COPY_BLOCK_ENTITY.get(),
                    CopyBlockRenderer::new);
        }
    }
}