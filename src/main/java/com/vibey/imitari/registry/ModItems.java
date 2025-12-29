package com.vibey.imitari.registry;

import com.vibey.imitari.Imitari;
import com.vibey.imitari.item.DebugToolItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Imitari.MODID);

    // Original copy block item
    public static final RegistryObject<Item> COPY_BLOCK = ITEMS.register("copy_block",
            () -> new BlockItem(ModBlocks.COPY_BLOCK.get(), new Item.Properties()));

    // New variant items
    public static final RegistryObject<Item> COPY_BLOCK_FULL = ITEMS.register("copy_block_ghost",
            () -> new BlockItem(ModBlocks.COPY_BLOCK_GHOST.get(), new Item.Properties()));

    public static final RegistryObject<Item> COPY_BLOCK_SLAB = ITEMS.register("copy_block_slab",
            () -> new BlockItem(ModBlocks.COPY_BLOCK_SLAB.get(), new Item.Properties()));

    public static final RegistryObject<Item> COPY_BLOCK_STAIRS = ITEMS.register("copy_block_stairs",
            () -> new BlockItem(ModBlocks.COPY_BLOCK_STAIRS.get(), new Item.Properties()));

    public static final RegistryObject<Item> COPY_BLOCK_LAYER = ITEMS.register("copy_block_layer",
            () -> new BlockItem(ModBlocks.COPY_BLOCK_LAYER.get(), new Item.Properties()));

    public static final RegistryObject<Item> DEBUG_TOOL = ITEMS.register("debug_tool",
            () -> new DebugToolItem(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}