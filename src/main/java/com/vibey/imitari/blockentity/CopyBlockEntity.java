package com.vibey.imitari.blockentity;

import com.vibey.imitari.block.ICopyBlock;
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

public class CopyBlockEntity extends BlockEntity {

    private BlockState copiedBlock = Blocks.AIR.defaultBlockState();
    private int virtualRotation = 0;
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
        System.out.println("[Imitari DEBUG] setCopiedBlock called! isClientSide=" +
                (level != null ? level.isClientSide : "level is null"));

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
            System.out.println("[Imitari] ========== setCopiedBlock SERVER SIDE START ==========");
            System.out.println("[Imitari] Position: " + worldPosition);
            System.out.println("[Imitari] Old: " + (oldCopiedBlock.isAir() ? "AIR" : oldCopiedBlock.getBlock().getName().getString()));
            System.out.println("[Imitari] New: " + (newBlock.isAir() ? "AIR" : newBlock.getBlock().getName().getString()));

            // Call VS2 integration to update mass - PASS OLD COPIED BLOCK
            System.out.println("[Imitari] About to call VS2CopyBlockIntegration.updateCopyBlockMass...");
            com.vibey.imitari.vs2.VS2CopyBlockIntegration.updateCopyBlockMass(
                    level, worldPosition, getBlockState(), oldCopiedBlock
            );
            System.out.println("[Imitari] VS2 call completed");

            // Send update packet to all clients
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                    Block.UPDATE_ALL);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());

            System.out.println("[Imitari] ========== setCopiedBlock END ==========");
        } else {
            System.out.println("[Imitari DEBUG] Skipping VS2 - either client side or level is null");
        }


    }

    private void rotateBlock() {
        virtualRotation = (virtualRotation + 1) % 3;
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

    // Add this to your CopyBlockEntity.java load() method:

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        BlockState oldCopied = this.copiedBlock;

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
            System.out.println("[Imitari] BlockEntity.load() detected copied block: " +
                    this.copiedBlock.getBlock().getName().getString());
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
        BlockState oldState = this.copiedBlock;
        int oldRotation = this.virtualRotation;

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
            BlockState oldCopied = this.copiedBlock;

            handleUpdateTag(tag);

            if (level != null && level.isClientSide) {
                requestModelDataUpdate();
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        ClientEventsHandler.queueBlockUpdate(worldPosition)
                );

                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(),
                        Block.UPDATE_ALL_IMMEDIATE);

                if (!oldCopied.isAir() && this.copiedBlock.isAir()) {
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
    }
}