package com.vibey.imitari;

import com.mojang.logging.LogUtils;
import com.vibey.imitari.api.registration.CopyBlockRegistration;
import com.vibey.imitari.client.CopyBlockModelProvider;
import com.vibey.imitari.registry.ModBlockEntities;
import com.vibey.imitari.registry.ModBlocks;
import com.vibey.imitari.registry.ModItems;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Main mod class for Imitari.
 * Provides an API for creating blocks that copy and mimic other blocks.
 *
 * @see com.vibey.imitari.api.ICopyBlock
 * @see com.vibey.imitari.api.CopyBlockAPI
 */
@Mod(Imitari.MODID)
public class Imitari {
    public static final String MODID = "imitari";
    public static final String VERSION = "0.1.0";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Imitari() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        LOGGER.info("Initializing Imitari v{}", VERSION);

        // Setup event listeners
        modEventBus.addListener(this::commonSetup);

        // Register mod content
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        // Register model provider for client rendering
        modEventBus.addListener(CopyBlockModelProvider::onModelBake);
        LOGGER.info("Registered CopyBlock model provider");

        // Forge event bus
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Register all Imitari CopyBlocks with the API
            int count = CopyBlockRegistration.registerForMod(MODID);
            LOGGER.info("Registered {} CopyBlocks with Imitari API", count);

            // Initialize VS2 integration (if present)
            com.vibey.imitari.vs2.VS2CopyBlockIntegration.register();
        });

        LOGGER.info("Imitari common setup complete!");
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

    /**
     * Client-side setup events.
     */
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Imitari client setup");

            event.enqueueWork(() -> {
                // Allow both cutout and translucent render types for all CopyBlocks
                // This enables proper rendering of glass, water, etc. when copied
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

                LOGGER.info("Configured render layers for CopyBlocks");
            });
        }
    }
}