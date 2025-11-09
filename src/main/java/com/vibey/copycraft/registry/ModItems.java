package com.vibey.copycraft.registry;

import com.vibey.copycraft.CopyCraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CopyCraft.MODID);

    // Register block items
    public static final RegistryObject<Item> COPY_BLOCK = ITEMS.register("copy_block",
            () -> new BlockItem(ModBlocks.COPY_BLOCK.get(), new Item.Properties())
    );
}