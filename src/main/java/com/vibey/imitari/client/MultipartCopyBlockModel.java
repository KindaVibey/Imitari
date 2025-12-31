package com.vibey.imitari.client;

import com.vibey.imitari.api.blockentity.ICopyBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Special wrapper for multipart blocks (fence, wall, pane) that FORCES ModelData through.
 * Multipart blockstates don't pass ModelData correctly, so we intercept at every level.
 */
public class MultipartCopyBlockModel implements BakedModel {

    public static final ModelProperty<BlockState> COPIED_STATE = new ModelProperty<>();

    private final BakedModel baseModel;
    private final ThreadLocal<BlockState> currentCopiedState = new ThreadLocal<>();

    public MultipartCopyBlockModel(BakedModel baseModel) {
        this.baseModel = baseModel;
    }

    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
        return baseModel.getQuads(state, side, rand);
    }

    @NotNull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                    @NotNull RandomSource rand, @NotNull ModelData data,
                                    @Nullable RenderType renderType) {

        // Get copied state from ModelData OR ThreadLocal
        BlockState copiedState = data.get(COPIED_STATE);
        if (copiedState == null) {
            copiedState = currentCopiedState.get();
        }

        if (copiedState == null || copiedState.isAir()) {
            return baseModel.getQuads(state, side, rand, ModelData.EMPTY, renderType);
        }

        // Get base quads from our model
        List<BakedQuad> baseQuads = baseModel.getQuads(state, side, rand, ModelData.EMPTY, renderType);
        if (baseQuads.isEmpty()) {
            return baseQuads;
        }

        // Get the copied block's model
        BakedModel copiedModel = Minecraft.getInstance()
                .getBlockRenderer()
                .getBlockModel(copiedState);

        // Get quads from copied block
        List<BakedQuad> copiedQuads = getCopiedQuads(copiedModel, copiedState, side, rand, renderType);
        if (copiedQuads.isEmpty()) {
            // Fallback to particle texture
            TextureAtlasSprite fallbackSprite = copiedModel.getParticleIcon(ModelData.EMPTY);
            return remapQuads(baseQuads, fallbackSprite);
        }

        // Remap each base quad to use copied textures
        return remapQuadsFromSource(baseQuads, copiedQuads);
    }

    private List<BakedQuad> getCopiedQuads(BakedModel copiedModel, BlockState copiedState,
                                           @Nullable Direction side, RandomSource rand,
                                           @Nullable RenderType renderType) {
        if (side == null) {
            return copiedModel.getQuads(copiedState, null, rand, ModelData.EMPTY, renderType);
        }

        List<BakedQuad> faceQuads = copiedModel.getQuads(copiedState, side, rand, ModelData.EMPTY, renderType);
        if (!faceQuads.isEmpty()) {
            return faceQuads;
        }

        // Try unculled quads
        return copiedModel.getQuads(copiedState, null, rand, ModelData.EMPTY, renderType);
    }

    private List<BakedQuad> remapQuadsFromSource(List<BakedQuad> baseQuads, List<BakedQuad> sourceQuads) {
        if (sourceQuads.isEmpty()) {
            return baseQuads;
        }

        // Use first source quad's sprite for all base quads
        BakedQuad firstSource = sourceQuads.get(0);
        return remapQuads(baseQuads, firstSource.getSprite(), firstSource.getTintIndex());
    }

    private List<BakedQuad> remapQuads(List<BakedQuad> quads, TextureAtlasSprite newSprite) {
        return remapQuads(quads, newSprite, -1);
    }

    private List<BakedQuad> remapQuads(List<BakedQuad> quads, TextureAtlasSprite newSprite, int tintIndex) {
        var remapped = new java.util.ArrayList<BakedQuad>();
        for (BakedQuad quad : quads) {
            remapped.add(remapQuadTexture(quad, newSprite, tintIndex >= 0 ? tintIndex : quad.getTintIndex()));
        }
        return remapped;
    }

    private BakedQuad remapQuadTexture(BakedQuad originalQuad, TextureAtlasSprite newSprite, int tintIndex) {
        int[] vertexData = originalQuad.getVertices().clone();
        TextureAtlasSprite oldSprite = originalQuad.getSprite();

        for (int i = 0; i < 4; i++) {
            int offset = i * 8;
            float u = Float.intBitsToFloat(vertexData[offset + 4]);
            float v = Float.intBitsToFloat(vertexData[offset + 5]);

            float relativeU = (u - oldSprite.getU0()) / (oldSprite.getU1() - oldSprite.getU0());
            float relativeV = (v - oldSprite.getV0()) / (oldSprite.getV1() - oldSprite.getV0());

            float newU = newSprite.getU0() + relativeU * (newSprite.getU1() - newSprite.getU0());
            float newV = newSprite.getV0() + relativeV * (newSprite.getV1() - newSprite.getV0());

            vertexData[offset + 4] = Float.floatToRawIntBits(newU);
            vertexData[offset + 5] = Float.floatToRawIntBits(newV);
        }

        return new BakedQuad(vertexData, tintIndex, originalQuad.getDirection(), newSprite, originalQuad.isShade());
    }

    @NotNull
    @Override
    public ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos,
                                  @NotNull BlockState state, @NotNull ModelData modelData) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ICopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            if (copiedState != null && !copiedState.isAir()) {
                // Store in ThreadLocal so sub-models can access it
                currentCopiedState.set(copiedState);

                return ModelData.builder()
                        .with(COPIED_STATE, copiedState)
                        .build();
            }
        }

        currentCopiedState.remove();
        return ModelData.builder()
                .with(COPIED_STATE, Blocks.AIR.defaultBlockState())
                .build();
    }

    @Override
    public boolean useAmbientOcclusion() { return true; }
    @Override
    public boolean isGui3d() { return true; }
    @Override
    public boolean usesBlockLight() { return true; }
    @Override
    public boolean isCustomRenderer() { return false; }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return baseModel.getParticleIcon();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        BlockState copiedState = data.get(COPIED_STATE);
        if (copiedState == null) {
            copiedState = currentCopiedState.get();
        }

        if (copiedState != null && !copiedState.isAir()) {
            BakedModel copiedModel = Minecraft.getInstance()
                    .getBlockRenderer()
                    .getBlockModel(copiedState);
            return copiedModel.getParticleIcon(ModelData.EMPTY);
        }
        return baseModel.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() { return baseModel.getTransforms(); }
    @Override
    public ItemOverrides getOverrides() { return baseModel.getOverrides(); }

    @NotNull
    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        BlockState copiedState = data.get(COPIED_STATE);
        if (copiedState != null && !copiedState.isAir()) {
            BakedModel copiedModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(copiedState);
            return copiedModel.getRenderTypes(copiedState, rand, ModelData.EMPTY);
        }
        return baseModel.getRenderTypes(state, rand, data);
    }
}