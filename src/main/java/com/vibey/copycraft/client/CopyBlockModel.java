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

        // Debug logging
        if (copiedState != null && !copiedState.isAir()) {
            System.out.println("CopyBlockModel rendering - Side: " + side + ", Copied: " + copiedState.getBlock().getName().getString() + ", Rotation: " + virtualRotation);
        }

        if (copiedState == null || copiedState.isAir()) {
            // Default to oak planks
            BlockState defaultState = Blocks.OAK_PLANKS.defaultBlockState();
            BakedModel defaultModel = Minecraft.getInstance()
                    .getBlockRenderer()
                    .getBlockModel(defaultState);
            return defaultModel.getQuads(defaultState, side, rand, ModelData.EMPTY, renderType);
        }

        if (virtualRotation == null) {
            virtualRotation = 0;
        }

        // Get the base model quads for our copy block
        List<BakedQuad> baseQuads = baseModel.getQuads(state, side, rand, ModelData.EMPTY, renderType);

        // Get the copied block's model
        BakedModel copiedModel = Minecraft.getInstance()
                .getBlockRenderer()
                .getBlockModel(copiedState);

        // Apply virtual rotation to determine which face to get texture from
        Direction textureFace = applyVirtualRotation(side, virtualRotation);

        System.out.println("  Original side: " + side + " -> Texture from: " + textureFace);

        // Get the texture sprite for the rotated face from the copied block
        List<BakedQuad> copiedQuads = copiedModel.getQuads(copiedState, textureFace, rand, ModelData.EMPTY, renderType);

        if (copiedQuads.isEmpty() && textureFace != null) {
            // Try null side (for blocks without specific face quads)
            copiedQuads = copiedModel.getQuads(copiedState, null, rand, ModelData.EMPTY, renderType);
        }

        // Get the sprite to use
        TextureAtlasSprite sprite;
        if (!copiedQuads.isEmpty()) {
            sprite = copiedQuads.get(0).getSprite();
        } else {
            sprite = copiedModel.getParticleIcon(ModelData.EMPTY);
        }

        // Remap UVs on our base quads to use the copied texture
        List<BakedQuad> remappedQuads = new ArrayList<>();
        for (BakedQuad quad : baseQuads) {
            remappedQuads.add(remapQuadTexture(quad, sprite));
        }

        return remappedQuads;
    }

    private Direction applyVirtualRotation(@Nullable Direction face, int rotation) {
        if (face == null || rotation == 0) {
            return face;
        }

        // Full 24-orientation rotation system
        // First digit (0-5): which face is "up"
        // Second digit (0-3): rotation around that axis

        int upFace = rotation / 4;  // 0-5: which original face becomes UP
        int spin = rotation % 4;     // 0-3: rotation around the up axis

        // Step 1: Determine which original face we need based on what's "up"
        Direction originalFace = face;

        // Map current face based on which face is rotated to UP
        originalFace = switch (upFace) {
            case 0 -> face; // Normal: UP is UP
            case 1 -> switch (face) { // DOWN is UP (flipped upside down)
                case UP -> Direction.DOWN;
                case DOWN -> Direction.UP;
                case NORTH -> Direction.SOUTH;
                case SOUTH -> Direction.NORTH;
                default -> face;
            };
            case 2 -> switch (face) { // NORTH is UP
                case UP -> Direction.SOUTH;
                case DOWN -> Direction.NORTH;
                case NORTH -> Direction.UP;
                case SOUTH -> Direction.DOWN;
                default -> face;
            };
            case 3 -> switch (face) { // SOUTH is UP
                case UP -> Direction.NORTH;
                case DOWN -> Direction.SOUTH;
                case NORTH -> Direction.DOWN;
                case SOUTH -> Direction.UP;
                default -> face;
            };
            case 4 -> switch (face) { // EAST is UP
                case UP -> Direction.WEST;
                case DOWN -> Direction.EAST;
                case EAST -> Direction.UP;
                case WEST -> Direction.DOWN;
                default -> face;
            };
            case 5 -> switch (face) { // WEST is UP
                case UP -> Direction.EAST;
                case DOWN -> Direction.WEST;
                case EAST -> Direction.DOWN;
                case WEST -> Direction.UP;
                default -> face;
            };
            default -> face;
        };

        // Step 2: Apply spin around the vertical axis
        if (spin > 0 && originalFace != Direction.UP && originalFace != Direction.DOWN) {
            for (int i = 0; i < spin; i++) {
                originalFace = switch (originalFace) {
                    case NORTH -> Direction.EAST;
                    case EAST -> Direction.SOUTH;
                    case SOUTH -> Direction.WEST;
                    case WEST -> Direction.NORTH;
                    default -> originalFace;
                };
            }
        }

        return originalFace;
    }

    private BakedQuad remapQuadTexture(BakedQuad originalQuad, TextureAtlasSprite newSprite) {
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

        return new BakedQuad(
                vertexData,
                originalQuad.getTintIndex(),
                originalQuad.getDirection(),
                newSprite,
                originalQuad.isShade()
        );
    }

    @NotNull
    @Override
    public ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos,
                                  @NotNull BlockState state, @NotNull ModelData modelData) {

        if (level.getBlockEntity(pos) instanceof CopyBlockEntity be) {
            System.out.println("getModelData called at " + pos + " - Has copied: " + be.hasCopiedBlock() + ", Rotation: " + be.getVirtualRotation());

            if (be.hasCopiedBlock()) {
                return ModelData.builder()
                        .with(COPIED_STATE, be.getCopiedBlock())
                        .with(VIRTUAL_ROTATION, be.getVirtualRotation())
                        .build();
            }
        }

        return ModelData.EMPTY;
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