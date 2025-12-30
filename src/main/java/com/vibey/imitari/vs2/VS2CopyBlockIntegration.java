package com.vibey.imitari.vs2;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * Safe wrapper for VS2 integration that has NO direct VS2 dependencies.
 * All VS2-specific code is in VS2CopyBlockIntegrationImpl and loaded via reflection.
 *
 * This allows Imitari to work without VS2 installed.
 */
public class VS2CopyBlockIntegration {
    private static final Logger LOGGER = LogUtils.getLogger();
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
            LOGGER.info("Valkyrien Skies 2 detected - enabling physics integration");
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
            LOGGER.info("VS2 integration registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register VS2 integration", e);
            INTEGRATION_FAILED = true;
        }
    }

    /**
     * Notify VS2 that a CopyBlock's copied content has changed.
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
            LOGGER.error("Failed to update VS2 copy block mass", e);
        }
    }

    /**
     * Notify VS2 that a BlockEntity has loaded NBT data with copied content.
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
            LOGGER.error("Failed to notify VS2 of block entity data load", e);
        }
    }

    /**
     * Notify VS2 that a block's state has changed (e.g., layer count, slab type).
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
            LOGGER.error("Failed to notify VS2 of block state change", e);
        }
    }

    /**
     * Notify VS2 that a CopyBlock is being removed/broken.
     *
     * CRITICAL: Call this BEFORE the block is actually removed!
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
            LOGGER.error("Failed to notify VS2 of block removal", e);
        }
    }
}