package com.vibey.copycraft.blockentity;

import com.vibey.copycraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
    }

    public boolean hasCopiedBlock() {
        return !copiedBlock.isAir();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("CopiedBlock")) {
            try {
                this.copiedBlock = NbtUtils.readBlockState(
                        this.level != null ? this.level.holderLookup(Registries.BLOCK) : null,
                        tag.getCompound("CopiedBlock")
                );
            } catch (Exception e) {
                this.copiedBlock = Blocks.AIR.defaultBlockState();
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