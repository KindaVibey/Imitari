package com.vibey.copycraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.vibey.copycraft.blockentity.CopyBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.List;

public class CopyBlockRenderer implements BlockEntityRenderer<CopyBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public CopyBlockRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(CopyBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        BlockState copiedState = blockEntity.getCopiedBlock();
        BlockState copyBlockState = blockEntity.getBlockState();

        poseStack.pushPose();

        try {
            // Get the Copy Block's model (YOUR custom UV mapping and geometry)
            BakedModel copyBlockModel = blockRenderer.getBlockModel(copyBlockState);

            // Get the texture sprite from the copied block
            TextureAtlasSprite textureSprite;
            if (blockEntity.hasCopiedBlock()) {
                BakedModel copiedModel = blockRenderer.getBlockModel(copiedState);
                textureSprite = copiedModel.getParticleIcon(ModelData.EMPTY);
            } else {
                // Default texture when nothing is copied
                BakedModel defaultModel = blockRenderer.getBlockModel(Blocks.OAK_PLANKS.defaultBlockState());
                textureSprite = defaultModel.getParticleIcon(ModelData.EMPTY);
            }

            // Render the Copy Block's model with the copied texture
            renderModelWithTexture(copyBlockModel, textureSprite, poseStack, bufferSource,
                    packedLight, packedOverlay, copyBlockState);

        } catch (Exception e) {
            // Fallback rendering
            e.printStackTrace();
        }

        poseStack.popPose();
    }

    private void renderModelWithTexture(BakedModel model, TextureAtlasSprite texture,
                                        PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay, BlockState state) {

        RandomSource random = RandomSource.create(42L);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());

        // Render each face direction + null (for non-culled quads)
        for (Direction direction : Direction.values()) {
            List<BakedQuad> quads = model.getQuads(state, direction, random, ModelData.EMPTY, RenderType.cutout());
            renderQuadsWithTexture(quads, texture, poseStack, consumer, packedLight, packedOverlay);
        }

        // Render non-culled quads
        List<BakedQuad> quads = model.getQuads(state, null, random, ModelData.EMPTY, RenderType.cutout());
        renderQuadsWithTexture(quads, texture, poseStack, consumer, packedLight, packedOverlay);
    }

    private void renderQuadsWithTexture(List<BakedQuad> quads, TextureAtlasSprite sprite,
                                        PoseStack poseStack, VertexConsumer consumer,
                                        int packedLight, int packedOverlay) {

        PoseStack.Pose pose = poseStack.last();

        for (BakedQuad quad : quads) {
            int[] vertexData = quad.getVertices();

            // Process 4 vertices
            for (int i = 0; i < 4; i++) {
                int idx = i * 8; // 8 ints per vertex

                // Position
                float x = Float.intBitsToFloat(vertexData[idx + 0]);
                float y = Float.intBitsToFloat(vertexData[idx + 1]);
                float z = Float.intBitsToFloat(vertexData[idx + 2]);

                // Color
                int color = vertexData[idx + 3];

                // UV - these come from the model and are already in sprite coordinates
                // But they reference the OLD sprite, we need to remap to the NEW sprite
                float oldU = Float.intBitsToFloat(vertexData[idx + 4]);
                float oldV = Float.intBitsToFloat(vertexData[idx + 5]);

                // Get the original sprite that this quad was referencing
                TextureAtlasSprite oldSprite = quad.getSprite();

                // Calculate the relative position within the old sprite (0-1)
                float relativeU = (oldU - oldSprite.getU0()) / (oldSprite.getU1() - oldSprite.getU0());
                float relativeV = (oldV - oldSprite.getV0()) / (oldSprite.getV1() - oldSprite.getV0());

                // Map to new sprite's coordinates
                float newU = sprite.getU0() + relativeU * (sprite.getU1() - sprite.getU0());
                float newV = sprite.getV0() + relativeV * (sprite.getV1() - sprite.getV0());

                // Lightmap
                int lightmap = vertexData[idx + 6];

                // Normal (packed as byte)
                int packedNormal = vertexData[idx + 7];
                byte nx = (byte) (packedNormal & 0xFF);
                byte ny = (byte) ((packedNormal >> 8) & 0xFF);
                byte nz = (byte) ((packedNormal >> 16) & 0xFF);

                consumer.vertex(pose.pose(), x, y, z)
                        .color(color)
                        .uv(newU, newV)
                        .overlayCoords(packedOverlay)
                        .uv2(packedLight)
                        .normal(pose.normal(), nx / 127.0f, ny / 127.0f, nz / 127.0f)
                        .endVertex();
            }
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