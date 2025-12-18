package com.vibey.imitari.blockentity;

import com.vibey.imitari.api.ICopyBlock;
import com.vibey.imitari.api.blockentity.ICopyBlockEntity;
import com.vibey.imitari.client.ClientEventsHandler;
import com.vibey.imitari.client.CopyBlockModel;
import com.vibey.imitari.registry.ModBlockEntities;
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

// CRITICAL: Must implement ICopyBlockEntity for the API to work!
public class CopyBlockEntity extends BlockEntity implements ICopyBlockEntity {

    private BlockState copiedBlock = Blocks.AIR.defaultBlockState();
    private int virtualRotation = 0;
    private boolean removedByCreative = false;

    public CopyBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COPY_BLOCK_ENTITY.get(), pos, blockState);
    }

    // ==================== ICOPYBLOCKENTITY IMPLEMENTATION ====================

    @Override
    public BlockState getCopiedBlock() {
        return copiedBlock;
    }

    @Override
    public int getVirtualRotation() {
        return virtualRotation;
    }

    @Override
    public boolean wasRemovedByCreative() {
        return removedByCreative;
    }

    @Override
    public void setRemovedByCreative(boolean value) {
        this.removedByCreative = value;
    }

    @Override
    public boolean hasCopiedBlock() {
        return !copiedBlock.isAir();
    }

    @Override
    public void forceModelRefresh() {
        if (level != null) {
            requestModelDataUpdate();

            if (level.isClientSide) {
                // Client-side: force chunk re-render
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientEventsHandler.queueBlockUpdate(worldPosition)
                );

                // Also force immediate block update
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                        Block.UPDATE_ALL_IMMEDIATE);
            } else {
                // Server-side: notify all clients
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                        Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public void setCopiedBlock(BlockState newBlock) {
        BlockState oldCopiedBlock = this.copiedBlock;  // SAVE OLD COPIED BLOCK

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
            // Call VS2 integration to update mass - PASS OLD COPIED BLOCK
            com.vibey.imitari.vs2.VS2CopyBlockIntegration.updateCopyBlockMass(
                    level, worldPosition, getBlockState(), oldCopiedBlock
            );

            // Send update packet to all clients
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    Block.UPDATE_ALL);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    // ==================== INTERNAL METHODS ====================

    private void rotateBlock() {
        virtualRotation = (virtualRotation + 1) % 3;
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
            this.copiedBlock = Blocks.AIR.defaultBlockState();
        }

        this.virtualRotation = tag.getInt("VirtualRotation");

        // CRITICAL: If we just loaded NBT data with copied block content, notify VS2
        // This handles ship assembly where VS2 queries mass before BlockEntity NBT is loaded
        if (level != null && !level.isClientSide && !this.copiedBlock.isAir()) {
            com.vibey.imitari.vs2.VS2CopyBlockIntegration.onBlockEntityDataLoaded(
                    level, worldPosition, getBlockState(), this.copiedBlock
            );
        }

        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);

        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
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
            handleUpdateTag(tag);

            if (level != null && level.isClientSide) {
                requestModelDataUpdate();
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientEventsHandler.queueBlockUpdate(worldPosition)
                );

                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                        Block.UPDATE_ALL_IMMEDIATE);
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }
}