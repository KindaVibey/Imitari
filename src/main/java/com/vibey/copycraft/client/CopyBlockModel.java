package com.vibey.copycraft.client;

import com.vibey.copycraft.blockentity.CopyBlockEntity;
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

import java.util.ArrayList;
import java.util.List;

public class CopyBlockModel implements BakedModel {

    public static final ModelProperty<BlockState> COPIED_STATE = new ModelProperty<>();
    public static final ModelProperty<Integer> VIRTUAL_ROTATION = new ModelProperty<>();

    private final BakedModel baseModel;

    public CopyBlockModel(BakedModel baseModel) {
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

        BlockState copiedState = data.get(COPIED_STATE);
        Integer virtualRotation = data.get(VIRTUAL_ROTATION);

        // Get the base model quads for our copy block
        List<BakedQuad> baseQuads = baseModel.getQuads(state, side, rand, ModelData.EMPTY, renderType);

        // If no copied state or it's AIR, return base quads (default texture)
        if (copiedState == null || copiedState.isAir()) {
            return baseQuads;
        }

        if (virtualRotation == null) {
            virtualRotation = 0;
        }

        // Get the copied block's model
        BakedModel copiedModel = Minecraft.getInstance()
                .getBlockRenderer()
                .getBlockModel(copiedState);

        // Apply log-style rotation to determine which face to get texture from
        Direction textureFace = applyLogRotation(side, virtualRotation);

        // Get the texture sprite for the rotated face from the copied block
        List<BakedQuad> copiedQuads = copiedModel.getQuads(copiedState, textureFace, rand, ModelData.EMPTY, renderType);

        if (copiedQuads.isEmpty() && textureFace != null) {
            // Try null side (for blocks without specific face quads)
            copiedQuads = copiedModel.getQuads(copiedState, null, rand, ModelData.EMPTY, renderType);
        }

        // Get the sprite to use
        TextureAtlasSprite sprite;
        BakedQuad sourceQuad = null;
        if (!copiedQuads.isEmpty()) {
            sourceQuad = copiedQuads.get(0);
            sprite = sourceQuad.getSprite();
        } else {
            sprite = copiedModel.getParticleIcon(ModelData.EMPTY);
        }

        // Remap UVs on our base quads to use the copied texture
        List<BakedQuad> remappedQuads = new ArrayList<>();
        for (BakedQuad quad : baseQuads) {
            remappedQuads.add(remapQuadTexture(quad, sprite, sourceQuad));
        }

        return remappedQuads;
    }

    /**
     * Simple log-style rotation: rotates the texture 90 degrees around different axes
     * rotation 0 = Y-axis (normal orientation, top/bottom stay top/bottom)
     * rotation 1 = Z-axis (rotated forward, was-top becomes front)
     * rotation 2 = X-axis (rotated sideways, was-top becomes right)
     */
    private Direction applyLogRotation(@Nullable Direction face, int rotation) {
        if (face == null || rotation == 0) {
            return face;
        }

        return switch (rotation) {
            case 1 -> // Z-axis orientation (like log facing north/south)
                    switch (face) {
                        case UP -> Direction.SOUTH;
                        case DOWN -> Direction.NORTH;
                        case NORTH -> Direction.DOWN;
                        case SOUTH -> Direction.UP;
                        case EAST, WEST -> face;
                    };
            case 2 -> // X-axis orientation (like log facing east/west)
                    switch (face) {
                        case UP -> Direction.EAST;
                        case DOWN -> Direction.WEST;
                        case EAST -> Direction.DOWN;
                        case WEST -> Direction.UP;
                        case NORTH, SOUTH -> face;
                    };
            default -> face;
        };
    }

    private BakedQuad remapQuadTexture(BakedQuad originalQuad, TextureAtlasSprite newSprite, @Nullable BakedQuad sourceQuad) {
        int[] vertexData = originalQuad.getVertices().clone();
        TextureAtlasSprite oldSprite = originalQuad.getSprite();

        // Process each vertex (4 vertices per quad)
        for (int i = 0; i < 4; i++) {
            int offset = i * 8; // 8 integers per vertex

            // Get original UV coordinates
            float u = Float.intBitsToFloat(vertexData[offset + 4]);
            float v = Float.intBitsToFloat(vertexData[offset + 5]);

            // Calculate relative position within old sprite (0.0 to 1.0)
            float relativeU = (u - oldSprite.getU0()) / (oldSprite.getU1() - oldSprite.getU0());
            float relativeV = (v - oldSprite.getV0()) / (oldSprite.getV1() - oldSprite.getV0());

            // Map to new sprite's UV space
            float newU = newSprite.getU0() + relativeU * (newSprite.getU1() - newSprite.getU0());
            float newV = newSprite.getV0() + relativeV * (newSprite.getV1() - newSprite.getV0());

            // Write back the new UV coordinates
            vertexData[offset + 4] = Float.floatToRawIntBits(newU);
            vertexData[offset + 5] = Float.floatToRawIntBits(newV);
        }

        // Use the source quad's tint index if available (for colored glass, etc.)
        int tintIndex = sourceQuad != null ? sourceQuad.getTintIndex() : originalQuad.getTintIndex();

        return new BakedQuad(
                vertexData,
                tintIndex,
                originalQuad.getDirection(),
                newSprite,
                originalQuad.isShade()
        );
    }

    @NotNull
    @Override
    public ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos,
                                  @NotNull BlockState state, @NotNull ModelData modelData) {

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CopyBlockEntity copyBE) {
            BlockState copiedState = copyBE.getCopiedBlock();
            int rotation = copyBE.getVirtualRotation();

            // ALWAYS build new ModelData - never return EMPTY or cached data
            // This ensures AIR states are properly represented
            return ModelData.builder()
                    .with(COPIED_STATE, copiedState != null ? copiedState : Blocks.AIR.defaultBlockState())
                    .with(VIRTUAL_ROTATION, rotation)
                    .build();
        }

        // Fallback: return AIR state rather than EMPTY
        return ModelData.builder()
                .with(COPIED_STATE, Blocks.AIR.defaultBlockState())
                .with(VIRTUAL_ROTATION, 0)
                .build();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return baseModel.getParticleIcon();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        BlockState copiedState = data.get(COPIED_STATE);
        if (copiedState != null && !copiedState.isAir()) {
            BakedModel copiedModel = Minecraft.getInstance()
                    .getBlockRenderer()
                    .getBlockModel(copiedState);
            return copiedModel.getParticleIcon(ModelData.EMPTY);
        }
        return baseModel.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return baseModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return baseModel.getOverrides();
    }

    @NotNull
    @Override
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.cutout());
    }
}