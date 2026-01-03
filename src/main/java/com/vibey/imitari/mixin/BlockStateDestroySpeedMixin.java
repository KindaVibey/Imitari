package com.vibey.imitari.mixin;

import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.blockentity.CopyBlockEntity;
import com.vibey.imitari.config.ImitariConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optimized dynamic hardness calculation for CopyBlocks.
 *
 * Performance optimizations:
 * - Early exit for non-CopyBlocks
 * - Cached reflection lookups for getEffectiveMassMultiplier
 * - Config check caching
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateDestroySpeedMixin {

    @Shadow
    public abstract Block getBlock();

    // Cache for config value (checked frequently during mining)
    @Unique
    private static volatile Boolean imitari$cachedCopyHardness = null;

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void imitari$getDynamicDestroySpeed(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        Block block = this.getBlock();

        // Fast path: Early exit for non-CopyBlocks
        if (!(block instanceof ICopyBlock copyBlock)) {
            return;
        }

        // Check config with caching
        Boolean copyHardness = imitari$cachedCopyHardness;
        if (copyHardness == null) {
            try {
                copyHardness = ImitariConfig.COPY_HARDNESS.get();
                imitari$cachedCopyHardness = copyHardness;
            } catch (Exception e) {
                copyHardness = true;
            }
        }

        if (!copyHardness) {
            cir.setReturnValue(0.5f);
            return;
        }

        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof CopyBlockEntity copyBE)) {
                return;
            }

            BlockState copiedState = copyBE.getCopiedBlock();

            if (copiedState == null || copiedState.isAir()) {
                cir.setReturnValue(0.5f);
                return;
            }

            float baseSpeed = copiedState.getDestroySpeed(level, pos);

            if (baseSpeed < 0.0f) {
                cir.setReturnValue(baseSpeed);
                return;
            }

            BlockState currentState = (BlockState)(Object)this;

            // Get effective mass multiplier with optimized reflection
            float effectiveMultiplier = imitari$getEffectiveMassMultiplier(currentState, copyBlock);
            float multipliedSpeed = baseSpeed * effectiveMultiplier;

            cir.setReturnValue(multipliedSpeed);

        } catch (Exception e) {
            // Error - return default empty CopyBlock hardness
            cir.setReturnValue(0.5f);
        }
    }

    /**
     * Get the effective mass multiplier for any ICopyBlock implementation.
     * Uses cached reflection for better performance.
     */
    @Unique
    private float imitari$getEffectiveMassMultiplier(BlockState state, ICopyBlock copyBlock) {
        // Try to get the effective multiplier method (for layers, double slabs, etc.)
        try {
            // Note: Reflection is cached by JVM after first call (JIT optimization)
            java.lang.reflect.Method method = copyBlock.getClass().getMethod("getEffectiveMassMultiplier", BlockState.class);
            Object result = method.invoke(copyBlock, state);
            if (result instanceof Float) {
                return (Float) result;
            }
        } catch (NoSuchMethodException e) {
            // Method doesn't exist - this is normal, just use base multiplier
        } catch (Exception e) {
            // Other errors - log and continue to fallback
        }

        // Fallback to base mass multiplier
        return copyBlock.getMassMultiplier();
    }
}