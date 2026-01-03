package com.vibey.imitari.item;

import com.vibey.imitari.util.CopyBlockContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class DebugToolItem extends Item {

    public DebugToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(net.minecraft.world.item.ItemStack stack) {
        return true; // Always show enchanted glint
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        BlockState state = context.getLevel().getBlockState(pos);

        // Get explosion resistance the way the game does it
        float explosionResistance = state.getExplosionResistance(
                context.getLevel(),
                pos,
                null  // null explosion
        );

        // Get destroy speed (hardness) the way the game does it
        float destroySpeed = state.getDestroySpeed(context.getLevel(), pos);

        // Send basic info to player
        context.getPlayer().sendSystemMessage(Component.literal("=== Block Debug Info ==="));
        context.getPlayer().sendSystemMessage(Component.literal("Block: " + state.getBlock().getName().getString()));
        context.getPlayer().sendSystemMessage(Component.literal("Explosion Resistance: " + explosionResistance));
        context.getPlayer().sendSystemMessage(Component.literal("Destroy Speed (hardness): " + destroySpeed));

        // Extra info for CopyBlockLayer
        if (state.hasProperty(com.vibey.imitari.block.CopyBlockLayer.LAYERS)) {
            int layers = state.getValue(com.vibey.imitari.block.CopyBlockLayer.LAYERS);
            context.getPlayer().sendSystemMessage(Component.literal("Layers: " + layers));
        }

        // Display all tags this block has (will use dynamic tag system if CopyBlock)
        context.getPlayer().sendSystemMessage(Component.literal("=== Tags ==="));

        // Note: Context is already set by getBlockState above, and our optimized
        // system allows multiple tag checks without popping context
        displayBlockTags(context, state);

        return InteractionResult.SUCCESS;
    }

    /**
     * Display all tags for a given block state
     */
    private void displayBlockTags(UseOnContext context, BlockState state) {
        List<String> tags = getAllBlockTags(context, state);

        if (tags.isEmpty()) {
            context.getPlayer().sendSystemMessage(Component.literal("No tags"));
        } else {
            context.getPlayer().sendSystemMessage(Component.literal("Total tags: " + tags.size()));

            // Group tags by namespace for better readability
            groupAndDisplayTags(context, tags);
        }
    }

    /**
     * Get all tags that this block state has
     */
    private List<String> getAllBlockTags(UseOnContext context, BlockState state) {
        List<String> tags = new ArrayList<>();

        try {
            // Get the block registry and iterate through all tag keys
            var blockRegistry = context.getLevel().registryAccess().registryOrThrow(Registries.BLOCK);

            // Get all tag keys from the registry
            blockRegistry.getTagNames().forEach(tagKey -> {
                // Check if this block state has this tag
                // NOTE: This will trigger our mixin if state is a CopyBlock
                // Our optimized system allows multiple checks without popping context!
                if (state.is(tagKey)) {
                    tags.add(tagKey.location().toString());
                }
            });
        } catch (Exception e) {
            context.getPlayer().sendSystemMessage(Component.literal("Error getting tags: " + e.getMessage()));
        }

        // Sort alphabetically for easier reading
        tags.sort(String::compareTo);

        return tags;
    }

    /**
     * Display tags grouped by namespace (minecraft, forge, etc.)
     */
    private void groupAndDisplayTags(UseOnContext context, List<String> tags) {
        String currentNamespace = "";
        int tagsInNamespace = 0;

        for (String tag : tags) {
            String namespace = tag.substring(0, tag.indexOf(':'));

            // New namespace - display header
            if (!namespace.equals(currentNamespace)) {
                if (!currentNamespace.isEmpty()) {
                    context.getPlayer().sendSystemMessage(
                            Component.literal("  (" + tagsInNamespace + " tags)")
                    );
                }

                currentNamespace = namespace;
                tagsInNamespace = 0;
                context.getPlayer().sendSystemMessage(
                        Component.literal("--- " + namespace + " ---")
                );
            }

            // Display tag (without namespace prefix for cleaner output)
            String tagPath = tag.substring(tag.indexOf(':') + 1);
            context.getPlayer().sendSystemMessage(
                    Component.literal("  - " + tagPath)
            );
            tagsInNamespace++;
        }

        // Display count for last namespace
        if (!currentNamespace.isEmpty()) {
            context.getPlayer().sendSystemMessage(
                    Component.literal("  (" + tagsInNamespace + " tags)")
            );
        }
    }
}