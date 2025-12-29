package com.vibey.imitari;

import com.vibey.imitari.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Imitari.MODID);

    public static final RegistryObject<CreativeModeTab> COPYCRAFT_TAB = CREATIVE_MODE_TABS.register("copycraft_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.COPY_BLOCK.get()))
                    .title(Component.translatable("itemGroup.imitari"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.COPY_BLOCK.get());
                        output.accept(ModItems.COPY_BLOCK_FULL.get());
                        output.accept(ModItems.COPY_BLOCK_SLAB.get());
                        output.accept(ModItems.COPY_BLOCK_STAIRS.get());
                        output.accept(ModItems.COPY_BLOCK_LAYER.get());
                        output.accept(ModItems.DEBUG_TOOL.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}