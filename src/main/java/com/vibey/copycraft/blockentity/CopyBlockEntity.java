package com.vibey.copycraft.blockentity;

import com.vibey.copycraft.client.ClientEventsHandler;
import com.vibey.copycraft.client.CopyBlockModel;
import com.vibey.copycraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CopyBlockEntity extends BlockEntity {

    private BlockState copiedBlock = Blocks.AIR.defaultBlockState();
    private int virtualRotation = 0; // 0 = Y-axis (normal), 1 = Z-axis, 2 = X-axis
    private boolean removedByCreative = false;

    public CopyBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COPY_BLOCK_ENTITY.get(), pos, blockState);
    }

    public BlockState getCopiedBlock() {
        return copiedBlock;
    }

    public int getVirtualRotation() {
        return virtualRotation;
    }

    public boolean wasRemovedByCreative() {
        return removedByCreative;
    }

    public void setRemovedByCreative(boolean value) {
        this.removedByCreative = value;
    }

    public void setCopiedBlock(BlockState newBlock) {
        BlockState oldBlock = this.copiedBlock;

        // If it's the same block, rotate it
        if (!copiedBlock.isAir() && !newBlock.isAir() && copiedBlock.getBlock() == newBlock.getBlock()) {
            rotateBlock();
        } else {
            // New block or clearing, reset rotation
            this.copiedBlock = newBlock;
            this.virtualRotation = 0;
        }

        setChanged();

        if (level != null && !level.isClientSide) {
            // Invalidate VS cache for this position
            try {
                com.vibey.copycraft.vs2.CopyCraftWeights.invalidateCache(worldPosition);
            } catch (NoClassDefFoundError e) {
                // VS not installed, ignore
            }

            // Send update packet to all clients
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    Block.UPDATE_ALL);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());

            // Log the change
            if (!oldBlock.is(newBlock.getBlock())) {
                System.out.println("[CopyCraft] Texture changed at " + worldPosition +
                        ": " + (oldBlock.isAir() ? "EMPTY" : oldBlock.getBlock().getName().getString()) +
                        " -> " + (newBlock.isAir() ? "EMPTY" : newBlock.getBlock().getName().getString()));
            }
        }
    }

    private void rotateBlock() {
        // Simple 3-axis rotation like logs: Y -> Z -> X -> Y
        virtualRotation = (virtualRotation + 1) % 3;
    }

    public boolean hasCopiedBlock() {
        return !copiedBlock.isAir();
    }

    @NotNull
    @Override
    public ModelData getModelData() {
        // Always return fresh data with current state
        return ModelData.builder()
                .with(CopyBlockModel.COPIED_STATE, copiedBlock)
                .with(CopyBlockModel.VIRTUAL_ROTATION, virtualRotation)
                .build();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        BlockState oldCopied = this.copiedBlock;

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
        } else {
            // No data means it should be AIR
            this.copiedBlock = Blocks.AIR.defaultBlockState();
        }

        this.virtualRotation = tag.getInt("VirtualRotation");

        if (level != null && level.isClientSide) {
            // Force invalidate and refresh model data
            requestModelDataUpdate();
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        BlockState oldState = this.copiedBlock;
        int oldRotation = this.virtualRotation;

        load(tag);

        // If data changed and we're on client, request render update
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();

            // Queue the block for render update
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    ClientEventsHandler.queueBlockUpdate(worldPosition)
            );
        }
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

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            // Store old values for comparison
            BlockState oldCopied = this.copiedBlock;

            handleUpdateTag(tag);

            // Force model data update and chunk re-render on client
            if (level != null && level.isClientSide) {
                // ALWAYS force update when receiving packet
                requestModelDataUpdate();

                // Queue the block for render update
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientEventsHandler.queueBlockUpdate(worldPosition)
                );

                // Mark the block for re-render with all flags
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                        Block.UPDATE_ALL_IMMEDIATE);

                // If we cleared the block (went from something to AIR), force extra update
                if (!oldCopied.isAir() && this.copiedBlock.isAir()) {
                    // Double-queue for AIR updates since they seem to be stubborn
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                        ClientEventsHandler.queueBlockUpdate(worldPosition);
                        ClientEventsHandler.queueBlockUpdate(worldPosition);
                    });
                }
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Clean up VS cache when block entity is removed
        if (level != null && !level.isClientSide) {
            try {
                com.vibey.copycraft.vs2.CopyCraftWeights.invalidateCache(worldPosition);
            } catch (NoClassDefFoundError e) {
                // VS not installed, ignore
            }
        }
    }
}