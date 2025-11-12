package com.vibey.copycraft.blockentity;

import com.vibey.copycraft.client.CopyBlockModel;
import com.vibey.copycraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CopyBlockEntity extends BlockEntity {
    private BlockState copiedBlock = Blocks.AIR.defaultBlockState();
    private int virtualRotation = 0; // 0 = normal, 1 = Z-axis, 2 = X-axis

    public CopyBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COPY_BLOCK_ENTITY.get(), pos, blockState);
    }

    public BlockState getCopiedBlock() {
        return copiedBlock;
    }

    public int getVirtualRotation() {
        return virtualRotation;
    }

    public void setCopiedBlock(BlockState newBlock) {
        System.out.println("setCopiedBlock called - Current: " + copiedBlock.getBlock().getName().getString() + ", New: " + newBlock.getBlock().getName().getString());

        // If it's the same block, rotate it
        if (!copiedBlock.isAir() && copiedBlock.getBlock() == newBlock.getBlock()) {
            rotateBlock();
            System.out.println("  Rotating! New rotation: " + virtualRotation);
        } else {
            // New block, reset rotation
            this.copiedBlock = newBlock;
            this.virtualRotation = 0;
            System.out.println("  New block copied!");
        }

        setChanged();

        if (level != null && !level.isClientSide) {
            // Server: sync to all clients with immediate render update
            System.out.println("  Server sending update");
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }

        // Always request model data update on both sides
        System.out.println("  Requesting model data update");
        requestModelDataUpdate();
    }

    private void rotateBlock() {
        // Check if block has AXIS property (logs, pillars, etc.)
        if (copiedBlock.hasProperty(BlockStateProperties.AXIS)) {
            Direction.Axis currentAxis = copiedBlock.getValue(BlockStateProperties.AXIS);
            Direction.Axis newAxis = switch (currentAxis) {
                case Y -> Direction.Axis.Z;
                case Z -> Direction.Axis.X;
                case X -> Direction.Axis.Y;
            };
            copiedBlock = copiedBlock.setValue(BlockStateProperties.AXIS, newAxis);
            virtualRotation = 0; // Reset virtual rotation when using real rotation
        }
        // Check if block has FACING property (furnaces, etc.)
        else if (copiedBlock.hasProperty(BlockStateProperties.FACING)) {
            Direction currentFacing = copiedBlock.getValue(BlockStateProperties.FACING);
            Direction newFacing = switch (currentFacing) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.UP;
                case UP -> Direction.DOWN;
                case DOWN -> Direction.NORTH;
            };
            copiedBlock = copiedBlock.setValue(BlockStateProperties.FACING, newFacing);
            virtualRotation = 0;
        }
        // Check if block has HORIZONTAL_FACING
        else if (copiedBlock.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction currentFacing = copiedBlock.getValue(BlockStateProperties.HORIZONTAL_FACING);
            Direction newFacing = currentFacing.getClockWise();
            copiedBlock = copiedBlock.setValue(BlockStateProperties.HORIZONTAL_FACING, newFacing);
            virtualRotation = 0;
        }
        // No rotation property - use virtual rotation
        else {
            virtualRotation = (virtualRotation + 1) % 3;
        }
    }

    public boolean hasCopiedBlock() {
        return !copiedBlock.isAir();
    }

    @NotNull
    @Override
    public ModelData getModelData() {
        return ModelData.builder()
                .with(CopyBlockModel.COPIED_STATE, copiedBlock)
                .with(CopyBlockModel.VIRTUAL_ROTATION, virtualRotation)
                .build();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains("CopiedBlockId")) {
            try {
                String blockId = tag.getString("CopiedBlockId");
                ResourceLocation loc = new ResourceLocation(blockId);
                this.copiedBlock = BuiltInRegistries.BLOCK.get(loc).defaultBlockState();

                // Load block state properties
                if (tag.contains("CopiedBlock")) {
                    try {
                        this.copiedBlock = NbtUtils.readBlockState(
                                level != null ? level.holderLookup(net.minecraft.core.registries.Registries.BLOCK) : null,
                                tag.getCompound("CopiedBlock")
                        );
                    } catch (Exception e) {
                        // Keep the default state from registry
                    }
                }
            } catch (Exception e) {
                this.copiedBlock = Blocks.AIR.defaultBlockState();
            }
        } else if (tag.contains("CopiedBlock")) {
            try {
                this.copiedBlock = NbtUtils.readBlockState(
                        level != null ? level.holderLookup(net.minecraft.core.registries.Registries.BLOCK) : null,
                        tag.getCompound("CopiedBlock")
                );
            } catch (Exception e) {
                this.copiedBlock = Blocks.AIR.defaultBlockState();
            }
        }

        this.virtualRotation = tag.getInt("VirtualRotation");

        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if (!copiedBlock.isAir()) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(copiedBlock.getBlock());
            tag.putString("CopiedBlockId", blockId.toString());
            tag.put("CopiedBlock", NbtUtils.writeBlockState(this.copiedBlock));
        }

        tag.putInt("VirtualRotation", virtualRotation);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}