package com.vibey.imitari.registry;

import com.vibey.imitari.Imitari;
import com.vibey.imitari.block.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Imitari.MODID);

    // Original full-size copy block
    public static final RegistryObject<Block> COPY_BLOCK = BLOCKS.register("copy_block",
            () -> new CopyBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    // New variants with mass multipliers
    public static final RegistryObject<Block> COPY_BLOCK_GHOST = BLOCKS.register("copy_block_ghost",
            () -> new CopyBlockGhost(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noCollission()
                    .noOcclusion()));

    public static final RegistryObject<Block> COPY_BLOCK_SLAB = BLOCKS.register("copy_block_slab",
            () -> new CopyBlockSlab(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<Block> COPY_BLOCK_STAIRS = BLOCKS.register("copy_block_stairs",
            () -> new CopyBlockStairs(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<Block> COPY_BLOCK_LAYER = BLOCKS.register("copy_block_layer",
            () -> new CopyBlockLayer(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    // Fence variants
    public static final RegistryObject<Block> COPY_BLOCK_FENCE = BLOCKS.register("copy_block_fence",
            () -> new CopyBlockFence(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<Block> COPY_BLOCK_FENCE_GATE = BLOCKS.register("copy_block_fence_gate",
            () -> new CopyBlockFenceGate(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<Block> COPY_BLOCK_WALL = BLOCKS.register("copy_block_wall",
            () -> new CopyBlockWall(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    // Door variants
    public static final RegistryObject<Block> COPY_BLOCK_DOOR = BLOCKS.register("copy_block_door",
            () -> new CopyBlockDoor(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<Block> COPY_BLOCK_IRON_DOOR = BLOCKS.register("copy_block_iron_door",
            () -> new CopyBlockIronDoor(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    // Trapdoor variants
    public static final RegistryObject<Block> COPY_BLOCK_TRAPDOOR = BLOCKS.register("copy_block_trapdoor",
            () -> new CopyBlockTrapdoor(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<Block> COPY_BLOCK_IRON_TRAPDOOR = BLOCKS.register("copy_block_iron_trapdoor",
            () -> new CopyBlockIronTrapdoor(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    // Pane
    public static final RegistryObject<Block> COPY_BLOCK_PANE = BLOCKS.register("copy_block_pane",
            () -> new CopyBlockPane(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()));

    // Button/Lever/Pressure Plate
    public static final RegistryObject<Block> COPY_BLOCK_BUTTON = BLOCKS.register("copy_block_button",
            () -> new CopyBlockButton(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noCollission()
                    .noOcclusion()));

    public static final RegistryObject<Block> COPY_BLOCK_LEVER = BLOCKS.register("copy_block_lever",
            () -> new CopyBlockLever(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noCollission()
                    .noOcclusion()));

    public static final RegistryObject<Block> COPY_BLOCK_PRESSURE_PLATE = BLOCKS.register("copy_block_pressure_plate",
            () -> new CopyBlockPressurePlate(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noCollission()
                    .noOcclusion()));

    // Ladder
    public static final RegistryObject<Block> COPY_BLOCK_LADDER = BLOCKS.register("copy_block_ladder",
            () -> new CopyBlockLadder(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.LADDER)
                    .noCollission()
                    .noOcclusion()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}