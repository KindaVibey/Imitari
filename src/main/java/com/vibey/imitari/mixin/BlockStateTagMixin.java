package com.vibey.imitari.mixin;

import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.config.ImitariConfig;
import com.vibey.imitari.util.CopyBlockContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Optimized dynamic tag checking for CopyBlocks.
 *
 * Performance optimizations:
 * - Early exit for non-CopyBlocks (99% of blocks)
 * - Cached blacklist check results
 * - No premature context cleanup (allows multiple tag checks)
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateTagMixin {

    @Shadow
    public abstract Block getBlock();

    // Cache for blacklist lookups (tag checks happen in hot loops)
    @Unique
    private static volatile List<? extends String> imitari$cachedBlacklist = null;

    /**
     * Inject at HEAD of is(TagKey) to check CopyBlock's copied block tags.
     */
    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true)
    private void imitari$checkCopiedTags(TagKey<Block> tag, CallbackInfoReturnable<Boolean> cir) {
        Block block = this.getBlock();

        // Fast path: Early exit for non-CopyBlocks (99% of cases)
        if (!(block instanceof ICopyBlock copyBlock)) {
            return;
        }

        // Check if dynamic tags are enabled for this block
        if (!copyBlock.useDynamicTags()) {
            return;
        }

        // Check if this tag is blacklisted
        if (imitari$isTagBlacklisted(tag)) {
            return; // Don't inherit blacklisted tags
        }

        // Try to get the copied block's tags using our optimized context system
        Boolean result = CopyBlockContext.checkCopiedBlockTag(tag);

        // If we got a result, use it. Otherwise, let vanilla behavior continue.
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Unique
    private boolean imitari$isTagBlacklisted(TagKey<Block> tag) {
        try {
            // Use cached blacklist to avoid repeated config lookups
            List<? extends String> blacklist = imitari$cachedBlacklist;
            if (blacklist == null) {
                blacklist = ImitariConfig.TAG_BLACKLIST.get();
                imitari$cachedBlacklist = blacklist;
            }

            if (blacklist.isEmpty()) {
                return false;
            }

            ResourceLocation tagLocation = tag.location();
            String tagString = tagLocation.toString();

            // Linear search is fine - blacklists are typically very small (< 10 entries)
            for (String blacklistedTag : blacklist) {
                if (tagString.equals(blacklistedTag)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Config not loaded yet or error - allow all tags
            imitari$cachedBlacklist = null;
        }

        return false;
    }
}