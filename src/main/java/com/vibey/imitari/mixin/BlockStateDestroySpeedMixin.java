package com.vibey.imitari.mixin;

import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.blockentity.CopyBlockEntity;
import com.vibey.imitari.config.ImitariConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateDestroySpeedMixin {

    @Shadow
    public abstract net.minecraft.world.level.block.Block m_60734_();

    @Inject(method = "m_60800_", at = @At("HEAD"), cancellable = true)
    private void imitari$getDynamicDestroySpeed(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (!(this.m_60734_() instanceof ICopyBlock copyBlock)) {
            return;
        }

        // Check config
        if (!ImitariConfig.COPY_HARDNESS.get()) {
            cir.setReturnValue(0.5f); // Empty CopyBlock hardness
            return;
        }

        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof CopyBlockEntity copyBE)) {
                return;
            }

            BlockState copiedState = copyBE.getCopiedBlock();

            if (copiedState == null || copiedState.isAir()) {
                cir.setReturnValue(0.5f); // Empty CopyBlock hardness
                return;
            }

            float baseSpeed = copiedState.getDestroySpeed(level, pos);

            if (baseSpeed < 0.0f) {
                cir.setReturnValue(baseSpeed);
                return;
            }

            BlockState currentState = (BlockState)(Object)this;

            // Try to get effective mass multiplier via reflection (for addon blocks)
            float effectiveMultiplier = getEffectiveMassMultiplier(currentState, copyBlock);
            float multipliedSpeed = baseSpeed * effectiveMultiplier;

            cir.setReturnValue(multipliedSpeed);

        } catch (Exception e) {
            // Error - return default
        }
    }

    /**
     * Get the effective mass multiplier for any ICopyBlock implementation.
     * First tries to call getEffectiveMassMultiplier(BlockState) via reflection,
     * then falls back to getMassMultiplier().
     */
    private float getEffectiveMassMultiplier(BlockState state, ICopyBlock copyBlock) {
        try {
            java.lang.reflect.Method method = copyBlock.getClass().getMethod("getEffectiveMassMultiplier", BlockState.class);
            Object result = method.invoke(copyBlock, state);
            if (result instanceof Float) {
                return (Float) result;
            }
        } catch (NoSuchMethodException e) {
            // Method doesn't exist - this is normal
        } catch (Exception e) {
            // Other errors - continue
        }

        return copyBlock.getMassMultiplier();
    }
}