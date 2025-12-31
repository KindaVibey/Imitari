package com.vibey.imitari.registry;

import com.vibey.imitari.Imitari;
import com.vibey.imitari.blockentity.CopyBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Imitari.MODID);

    public static final RegistryObject<BlockEntityType<CopyBlockEntity>> COPY_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("copy_block_entity", () ->
                    BlockEntityType.Builder.of(CopyBlockEntity::new,
                            ModBlocks.COPY_BLOCK.get(),
                            ModBlocks.COPY_BLOCK_GHOST.get(),
                            ModBlocks.COPY_BLOCK_SLAB.get(),
                            ModBlocks.COPY_BLOCK_STAIRS.get(),
                            ModBlocks.COPY_BLOCK_LAYER.get(),
                            ModBlocks.COPY_BLOCK_FENCE.get(),
                            ModBlocks.COPY_BLOCK_FENCE_GATE.get(),
                            ModBlocks.COPY_BLOCK_WALL.get(),
                            ModBlocks.COPY_BLOCK_DOOR.get(),
                            ModBlocks.COPY_BLOCK_IRON_DOOR.get(),
                            ModBlocks.COPY_BLOCK_BUTTON.get(),
                            ModBlocks.COPY_BLOCK_LEVER.get(),
                            ModBlocks.COPY_BLOCK_TRAPDOOR.get(),
                            ModBlocks.COPY_BLOCK_IRON_TRAPDOOR.get(),
                            ModBlocks.COPY_BLOCK_PANE.get(),
                            ModBlocks.COPY_BLOCK_PRESSURE_PLATE.get(),
                            ModBlocks.COPY_BLOCK_LADDER.get()
                    ).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}