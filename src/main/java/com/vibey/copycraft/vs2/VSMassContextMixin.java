package com.vibey.copycraft.vs2;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * CRITICAL: This mixin provides position context to our BlockStateInfoProvider.
 * Eureka doesn't need this because ballast mass is in BlockState (power level).
 * We need this because CopyBlock mass is in BlockEntity (copied block).
 */
@Pseudo
@Mixin(targets = "org.valkyrienskies.mod.common.assembly.ShipAssemblyKt", remap = false)
public class VSMassContextMixin {

    @Inject(
            method = "getBlockMass",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private static void setContextBeforeQuery(
            Level level,
            BlockPos pos,
            BlockState blockState,
            Object ship,
            CallbackInfoReturnable<Double> cir
    ) {
        System.out.println("[CopyCraft Mixin] getBlockMass HEAD: " + blockState + " at " + pos);
        CopyCraftWeights.setContext(level, pos);
    }

    @Inject(
            method = "getBlockMass",
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private static void clearContextAfterQuery(
            Level level,
            BlockPos pos,
            BlockState blockState,
            Object ship,
            CallbackInfoReturnable<Double> cir
    ) {
        System.out.println("[CopyCraft Mixin] getBlockMass RETURN: " + cir.getReturnValue() + " kg");
        CopyCraftWeights.clearContext();
    }
}