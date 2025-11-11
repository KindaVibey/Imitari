package com.vibey.copycraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vibey.copycraft.blockentity.CopyBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

public class CopyBlockRenderer implements BlockEntityRenderer<CopyBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public CopyBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(CopyBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        if (blockEntity.getLevel() == null) {
            return;
        }

        BlockState copiedState = blockEntity.getCopiedBlock();
        BlockPos pos = blockEntity.getBlockPos();

        // If there's a copied block, render it using Minecraft's own rendering
        if (blockEntity.hasCopiedBlock()) {
            poseStack.pushPose();

            // Render the block as if it exists in the world at this position
            // This makes Minecraft calculate lighting based on surrounding blocks
            blockRenderer.renderBatched(
                    copiedState,
                    pos,
                    blockEntity.getLevel(),
                    poseStack,
                    bufferSource.getBuffer(RenderType.cutout()),
                    false,
                    blockEntity.getLevel().getRandom(),
                    ModelData.EMPTY,
                    RenderType.cutout()
            );

            poseStack.popPose();
        } else {
            // Render default oak planks if nothing is copied
            poseStack.pushPose();

            blockRenderer.renderBatched(
                    Blocks.OAK_PLANKS.defaultBlockState(),
                    pos,
                    blockEntity.getLevel(),
                    poseStack,
                    bufferSource.getBuffer(RenderType.cutout()),
                    false,
                    blockEntity.getLevel().getRandom(),
                    ModelData.EMPTY,
                    RenderType.cutout()
            );

            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(CopyBlockEntity blockEntity) {
        return false;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}