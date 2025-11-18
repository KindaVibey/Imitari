package com.vibey.copycraft;

import com.mojang.logging.LogUtils;
import com.vibey.copycraft.client.ModelRegistrationHandler;
import com.vibey.copycraft.registry.ModItems;
import com.vibey.copycraft.registry.ModBlocks;
import com.vibey.copycraft.registry.ModBlockEntities;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
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

@Mod(CopyCraft.MODID)
public class CopyCraft {
    public static final String MODID = "copycraft";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CopyCraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        System.out.println("CopyCraft constructor called!");

        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        // Register model event handler manually
        modEventBus.addListener(ModelRegistrationHandler::onModelBake);
        System.out.println("Registered model bake listener!");

        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("CopyCraft common setup complete!");

        // Register VS2 weights on the main thread
        event.enqueueWork(() -> {
            try {
                com.vibey.copycraft.vs2.CopyCraftWeights.register();
                LOGGER.info("Successfully registered CopyCraft VS2 dynamic mass system!");
            } catch (NoClassDefFoundError e) {
                LOGGER.info("Valkyrien Skies not installed - skipping VS2 integration");
            } catch (Exception e) {
                LOGGER.error("Failed to register VS2 weights", e);
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
        LOGGER.info("CopyCraft server starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("CopyCraft client setup");

            event.enqueueWork(() -> {
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.COPY_BLOCK.get(), RenderType.cutout());
            });
        }
    }
}