package com.vibey.imitari.mixin;

import com.vibey.imitari.block.ICopyBlock;
import com.vibey.imitari.blockentity.CopyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Injects dynamic destroy speed based on copied block.
 *
 * FIXED: Used proper method signature for getDestroySpeed
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateDestroySpeedMixin {

    @Shadow
    public abstract Block getBlock();

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void imitari$getDynamicDestroySpeed(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (!(this.getBlock() instanceof ICopyBlock)) {
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

            ICopyBlock copyBlock = (ICopyBlock) this.getBlock();
            float multipliedSpeed = baseSpeed * copyBlock.getMassMultiplier();

            cir.setReturnValue(multipliedSpeed);

        } catch (Exception e) {
            System.err.println("[Imitari] ERROR in getDestroySpeed mixin:");
            e.printStackTrace();
        }
    }
}