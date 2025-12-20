package com.vibey.imitari.vs2;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;

/**
 * Safe wrapper for VS2 integration that has NO direct VS2 dependencies.
 * All VS2-specific code is in VS2CopyBlockIntegrationImpl and loaded via reflection.
 *
 * This allows Imitari to work without VS2 installed.
 */
public class VS2CopyBlockIntegration {
    private static boolean VS2_LOADED = false;
    private static boolean CHECKED = false;
    private static boolean INTEGRATION_FAILED = false;

    /**
     * Check if VS2 is available.
     * Safe to call from anywhere.
     *
     * @return true if VS2 is loaded and integration is working
     */
    public static boolean isAvailable() {
        if (!CHECKED) {
            checkVS2();
        }
        return VS2_LOADED && !INTEGRATION_FAILED;
    }

    private static void checkVS2() {
        if (CHECKED) return;

        VS2_LOADED = ModList.get().isLoaded("valkyrienskies");
        CHECKED = true;

        if (VS2_LOADED) {
            System.out.println("[Imitari] Valkyrienskies detected! Enabling CopyBlock physics integration.");
        } else {
            System.out.println("[Imitari] Valkyrienskies not detected. CopyBlock will work without ship physics.");
        }
    }

    /**
     * Register the CopyBlock mass provider with VS2.
     * Called during mod initialization.
     */
    public static void register() {
        if (!isAvailable()) {
            return;
        }

        try {
            Class<?> implClass = Class.forName("com.vibey.imitari.vs2.VS2CopyBlockIntegrationImpl");
            var registerMethod = implClass.getMethod("register");
            registerMethod.invoke(null);
            System.out.println("[Imitari] Successfully registered VS2 CopyBlock integration!");
        } catch (Exception e) {
            System.err.println("[Imitari] Failed to register VS2 integration: " + e.getMessage());
            INTEGRATION_FAILED = true;
            e.printStackTrace();
        }
    }

    /**
     * Notify VS2 that a CopyBlock's copied content has changed.
     *
     * @param level The world
     * @param pos The block position
     * @param copyBlockState The CopyBlock's state
     * @param oldCopiedBlock The previous copied block (for mass calculation)
     */
    public static void updateCopyBlockMass(Level level, BlockPos pos,
                                           BlockState copyBlockState,
                                           BlockState oldCopiedBlock) {
        if (!isAvailable()) {
            return;
        }

        try {
            Class<?> implClass = Class.forName("com.vibey.imitari.vs2.VS2CopyBlockIntegrationImpl");
            var method = implClass.getMethod("updateCopyBlockMass",
                    Level.class, BlockPos.class, BlockState.class, BlockState.class);
            method.invoke(null, level, pos, copyBlockState, oldCopiedBlock);
        } catch (Exception e) {
            System.err.println("[Imitari] Failed to update VS2 copy block mass: " + e.getMessage());
        }
    }

    /**
     * Notify VS2 that a BlockEntity has loaded NBT data with copied content.
     * Critical for ship assembly where VS2 queries mass before NBT is loaded.
     *
     * @param level The world
     * @param pos The block position
     * @param state The block state
     * @param copiedBlock The copied block from NBT
     */
    public static void onBlockEntityDataLoaded(Level level, BlockPos pos,
                                               BlockState state,
                                               BlockState copiedBlock) {
        if (!isAvailable()) {
            return;
        }

        try {
            Class<?> implClass = Class.forName("com.vibey.imitari.vs2.VS2CopyBlockIntegrationImpl");
            var method = implClass.getMethod("onBlockEntityDataLoaded",
                    Level.class, BlockPos.class, BlockState.class, BlockState.class);
            method.invoke(null, level, pos, state, copiedBlock);
        } catch (Exception e) {
            System.err.println("[Imitari] Failed to notify VS2 of block entity data load: " + e.getMessage());
        }
    }

    /**
     * Notify VS2 that a block's state has changed (e.g., layer count, slab type).
     * This updates the mass calculation when the mass multiplier changes.
     *
     * @param level The world
     * @param pos The block position
     * @param oldState The previous block state
     * @param newState The new block state
     */
    public static void onBlockStateChanged(Level level, BlockPos pos,
                                           BlockState oldState,
                                           BlockState newState) {
        if (!isAvailable()) {
            return;
        }

        try {
            Class<?> implClass = Class.forName("com.vibey.imitari.vs2.VS2CopyBlockIntegrationImpl");
            var method = implClass.getMethod("onBlockStateChanged",
                    Level.class, BlockPos.class, BlockState.class, BlockState.class);
            method.invoke(null, level, pos, oldState, newState);
        } catch (Exception e) {
            System.err.println("[Imitari] Failed to notify VS2 of block state change: " + e.getMessage());
        }
    }

    /**
     * Notify VS2 that a CopyBlock is being removed/broken.
     * This ensures the correct mass is subtracted.
     *
     * CRITICAL: Call this BEFORE the block is actually removed!
     *
     * @param level The world
     * @param pos The block position
     * @param state The CopyBlock state being removed
     * @param copiedBlock The copied block that was stored (for mass calculation)
     */
    public static void onBlockRemoved(Level level, BlockPos pos,
                                      BlockState state,
                                      BlockState copiedBlock) {
        if (!isAvailable()) {
            return;
        }

        try {
            Class<?> implClass = Class.forName("com.vibey.imitari.vs2.VS2CopyBlockIntegrationImpl");
            var method = implClass.getMethod("onBlockRemoved",
                    Level.class, BlockPos.class, BlockState.class, BlockState.class);
            method.invoke(null, level, pos, state, copiedBlock);
        } catch (Exception e) {
            System.err.println("[Imitari] Failed to notify VS2 of block removal: " + e.getMessage());
        }
    }
}