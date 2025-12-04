package com.vibey.imitari.mixin;

import com.vibey.imitari.block.ICopyBlock;
import com.vibey.imitari.blockentity.CopyBlockEntity;
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
    private void imitari$getDynamicDestroySpeed(BlockGetter p_60801_, BlockPos p_60802_, CallbackInfoReturnable<Float> cir) {
        if (!(this.m_60734_() instanceof ICopyBlock)) {
            return;
        }

        try {
            BlockEntity be = p_60801_.getBlockEntity(p_60802_);
            if (!(be instanceof CopyBlockEntity copyBE)) {
                return;
            }

            BlockState copiedState = copyBE.getCopiedBlock();

            if (copiedState == null || copiedState.isAir()) {
                cir.setReturnValue(0.5f);
                return;
            }

            float baseSpeed = copiedState.getDestroySpeed(p_60801_, p_60802_);

            if (baseSpeed < 0.0f) {
                cir.setReturnValue(baseSpeed);
                return;
            }

            ICopyBlock copyBlock = (ICopyBlock) this.m_60734_();
            float multipliedSpeed = baseSpeed * copyBlock.getMassMultiplier();

            cir.setReturnValue(multipliedSpeed);

        } catch (Exception e) {
            System.err.println("[Imitari] ERROR in getDestroySpeed mixin:");
            e.printStackTrace();
        }
    }
}