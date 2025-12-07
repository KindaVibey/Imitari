package com.vibey.imitari.mixin.compat.modernfix;

import com.vibey.imitari.block.ICopyBlock;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.embeddedt.modernfix.duck.IModelHoldingBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlockModelShaper.class, priority = 900)
public class ExcludeImitariBlocksMixin {

    @Inject(method = { "<init>", "m_119409_" }, at = @At("RETURN"), remap = false)
    private void imitari$skipICopyBlockIteration(CallbackInfo ci) {
        System.out.println("[Imitari] Clearing model cache for non-ICopyBlock blocks only");

        for(Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof ICopyBlock) {
                System.out.println("[Imitari] SKIPPED: " + block.getClass().getSimpleName());
                continue;
            }

            for(BlockState state : block.getStateDefinition().getPossibleStates()) {
                if(state instanceof IModelHoldingBlockState modelHolder) {
                    modelHolder.mfix$setModel(null);
                }
            }
        }
    }
}