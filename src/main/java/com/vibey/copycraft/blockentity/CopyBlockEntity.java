package com.vibey.copycraft.blockentity;

import com.vibey.copycraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class CopyBlockEntity extends BlockEntity {
    private BlockState copiedBlock = Blocks.AIR.defaultBlockState();

    public CopyBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COPY_BLOCK_ENTITY.get(), pos, blockState);
    }

    public BlockState getCopiedBlock() {
        return copiedBlock;
    }

    public void setCopiedBlock(BlockState copiedBlock) {
        this.copiedBlock = copiedBlock;
        setChanged();

        // Force sync to client
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean hasCopiedBlock() {
        return !copiedBlock.isAir();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        // FIXED: Use a safer method that doesn't require HolderLookup
        if (tag.contains("CopiedBlockId")) {
            try {
                String blockId = tag.getString("CopiedBlockId");
                ResourceLocation loc = new ResourceLocation(blockId);
                this.copiedBlock = BuiltInRegistries.BLOCK.get(loc).defaultBlockState();
            } catch (Exception e) {
                System.err.println("[CopyCraft] Failed to load copied block: " + e.getMessage());
                this.copiedBlock = Blocks.AIR.defaultBlockState();
            }
        } else {
            // Fallback for old save format
            if (tag.contains("CopiedBlock")) {
                try {
                    this.copiedBlock = NbtUtils.readBlockState(
                            level != null ? level.holderLookup(net.minecraft.core.registries.Registries.BLOCK) : null,
                            tag.getCompound("CopiedBlock")
                    );
                } catch (Exception e) {
                    this.copiedBlock = Blocks.AIR.defaultBlockState();
                }
            }
        }
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        // FIXED: Save as ResourceLocation string instead of complex NBT
        if (!copiedBlock.isAir()) {
            ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(copiedBlock.getBlock());
            tag.putString("CopiedBlockId", blockId.toString());
        }

        // Also save the old way for compatibility
        tag.put("CopiedBlock", NbtUtils.writeBlockState(this.copiedBlock));
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