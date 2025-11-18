package com.vibey.copycraft.vs2;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;

@Pseudo
@Mixin(targets = "org.valkyrienskies.mod.common.assembly.ShipAssemblyKt", remap = false)
public class VSMassContextMixin {

    @Inject(method = "getBlockMass", at = @At("HEAD"), remap = false)
    private static void setMassContext(Level level, BlockPos pos, BlockState blockState,
                                       ShipObjectServerWorld ship, CallbackInfoReturnable<Double> cir) {
        CopyCraftWeights.setContext(level, pos);
    }

    @Inject(method = "getBlockMass", at = @At("RETURN"), remap = false)
    private static void clearMassContext(Level level, BlockPos pos, BlockState blockState,
                                         ShipObjectServerWorld ship, CallbackInfoReturnable<Double> cir) {
        CopyCraftWeights.clearContext();
    }
}