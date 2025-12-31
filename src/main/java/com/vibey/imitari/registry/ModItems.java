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

    // Fence variants
    public static final RegistryObject<Item> COPY_BLOCK_FENCE = ITEMS.register("copy_block_fence",
            () -> new BlockItem(ModBlocks.COPY_BLOCK_FENCE.get(), new Item.Properties()));

    public static final RegistryObject<Item> COPY_BLOCK_FENCE_GATE = ITEMS.register("copy_block_fence_gate",
            () -> new BlockItem(ModBlocks.COPY_BLOCK_FENCE_GATE.get(), new Item.Properties()));

    public static final RegistryObject<Item> COPY_BLOCK_WALL = ITEMS.register("copy_block_wall",
            () -> new BlockItem(ModBlocks.COPY_BLOCK_WALL.get(), new Item.Properties()));

    // Door variants
    public static final RegistryObject<Item> COPY_BLOCK_DOOR = ITEMS.register("copy_block_door",
            () -> new BlockItem(ModBlocks.COPY_BLOCK_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> COPY_BLOCK_IRON_DOOR = ITEMS.register("copy_block_iron_door",
            () -> new BlockItem(ModBlocks.COPY_BLOCK_IRON_DOOR.get(), new Item.Properties()));

    // Trapdoor variants
    public static final RegistryObject<Item> COPY_BLOCK_TRAPDOOR = ITEMS.register("copy_block_trapdoor",
            () -> new BlockItem(ModBlocks.COPY_BLOCK_TRAPDOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> COPY_BLOCK_IRON_TRAPDOOR = ITEMS.register("copy_block_iron_trapdoor",
            () -> new BlockItem(ModBlocks.COPY_BLOCK_IRON_TRAPDOOR.get(), new Item.Properties()));

    // Pane
    public static final RegistryObject<Item> COPY_BLOCK_PANE = ITEMS.register("copy_block_pane",
            () -> new BlockItem(ModBlocks.COPY_BLOCK_PANE.get(), new Item.Properties()));

    // Button/Lever/Pressure Plate
    public static final RegistryObject<Item> COPY_BLOCK_BUTTON = ITEMS.register("copy_block_button",
            () -> new BlockItem(ModBlocks.COPY_BLOCK_BUTTON.get(), new Item.Properties()));

    public static final RegistryObject<Item> COPY_BLOCK_LEVER = ITEMS.register("copy_block_lever",
            () -> new BlockItem(ModBlocks.COPY_BLOCK_LEVER.get(), new Item.Properties()));

    public static final RegistryObject<Item> COPY_BLOCK_PRESSURE_PLATE = ITEMS.register("copy_block_pressure_plate",
            () -> new BlockItem(ModBlocks.COPY_BLOCK_PRESSURE_PLATE.get(), new Item.Properties()));

    // Ladder
    public static final RegistryObject<Item> COPY_BLOCK_LADDER = ITEMS.register("copy_block_ladder",
            () -> new BlockItem(ModBlocks.COPY_BLOCK_LADDER.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}