package com.craftzero.world.tile;

import com.craftzero.entity.DroppedItem;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;

import java.util.Random;

public class JukeboxTileEntity extends TileEntity {
    private static final float RECORD_EJECT_OFFSET_RANGE = 0.7f;
    private static final float RECORD_EJECT_HORIZONTAL_MARGIN = (1.0f - RECORD_EJECT_OFFSET_RANGE) * 0.5f;
    private static final float RECORD_EJECT_VERTICAL_MARGIN = (1.0f - RECORD_EJECT_OFFSET_RANGE) * 0.2f + 0.6f;
    private static final float RECORD_EJECT_VERTICAL_VELOCITY = 0.2f;
    private static final float RECORD_EJECT_HORIZONTAL_VELOCITY = 0.02f;

    private ItemStack record;
    private int playTicks;

    public JukeboxTileEntity(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public String getTypeId() {
        return "jukebox";
    }

    public boolean hasRecord() {
        return record != null && !record.isEmpty();
    }

    public ItemStack getRecord() {
        return record == null ? null : record.copy();
    }

    public boolean insertRecord(ItemStack stack) {
        if (hasRecord() || stack == null || stack.isEmpty() || !stack.getType().isRecord()) {
            return false;
        }
        record = stack.copy();
        record.setCount(1);
        playTicks = 0;
        markDirty();
        return true;
    }

    public boolean insertRecord(World world, ItemStack stack) {
        if (!insertRecord(stack)) {
            return false;
        }
        syncBlockMetadata(world, true);
        return true;
    }

    public ItemStack removeRecord() {
        ItemStack removed = getRecord();
        record = null;
        playTicks = 0;
        markDirty();
        return removed;
    }

    public ItemStack removeRecord(World world) {
        stopRecordSound(world);
        ItemStack removed = removeRecord();
        syncBlockMetadata(world, false);
        return removed;
    }

    public boolean ejectRecord(World world) {
        return ejectRecord(world, true);
    }

    public boolean ejectRecordOnRemoval(World world) {
        return ejectRecord(world, false);
    }

    private boolean ejectRecord(World world, boolean syncMetadata) {
        if (world == null || !hasRecord()) {
            return false;
        }
        BlockPos pos = getPos();
        stopRecordSound(world);
        ItemStack removed = removeRecord();
        if (syncMetadata) {
            syncBlockMetadata(world, false);
        }
        if (removed == null || removed.isEmpty()) {
            return false;
        }
        playEjectSound(world);
        Random random = world.getRandom();
        float dropX = pos.x() + random.nextFloat() * RECORD_EJECT_OFFSET_RANGE + RECORD_EJECT_HORIZONTAL_MARGIN;
        float dropY = pos.y() + random.nextFloat() * RECORD_EJECT_OFFSET_RANGE + RECORD_EJECT_VERTICAL_MARGIN;
        float dropZ = pos.z() + random.nextFloat() * RECORD_EJECT_OFFSET_RANGE + RECORD_EJECT_HORIZONTAL_MARGIN;
        world.spawnThrownStack(dropX, dropY, dropZ, removed,
                (float) random.nextGaussian() * RECORD_EJECT_HORIZONTAL_VELOCITY,
                RECORD_EJECT_VERTICAL_VELOCITY,
                (float) random.nextGaussian() * RECORD_EJECT_HORIZONTAL_VELOCITY,
                DroppedItem.DEFAULT_PICKUP_DELAY_TICKS);
        return true;
    }

    public int getPlayTicks() {
        return playTicks;
    }

    public void setPlayTicks(int playTicks) {
        this.playTicks = Math.max(0, playTicks);
        markDirty();
    }

    public void play(World world) {
        if (hasRecord()) {
            stopRecordSound(world);
            playTicks++;
            playRecordSound(world);
            markDirty();
        }
    }

    private void stopRecordSound(World world) {
        if (world != null) {
            BlockPos pos = getPos();
            world.stopRecordSound(pos.x() + 0.5f, pos.y() + 0.5f, pos.z() + 0.5f);
        }
    }

    private void playRecordSound(World world) {
        ItemType type = record == null ? null : record.getType();
        String soundId = WorldSoundEvent.recordSoundId(type);
        if (world != null && soundId != null) {
            BlockPos pos = getPos();
            world.playSound(soundId, pos.x() + 0.5f, pos.y() + 0.5f, pos.z() + 0.5f, 4.0f, 1.0f);
        }
    }

    private void playEjectSound(World world) {
        if (world != null) {
            BlockPos pos = getPos();
            world.playSound(WorldSoundEvent.RECORD_EJECT, pos.x() + 0.5f, pos.y() + 0.5f, pos.z() + 0.5f,
                    0.8f, 1.0f);
        }
    }

    private void syncBlockMetadata(World world, boolean inserted) {
        if (world == null) {
            return;
        }
        BlockPos pos = getPos();
        int metadata = inserted ? 1 : 0;
        if (world.getBlockIfLoaded(pos.x(), pos.y(), pos.z(), BlockType.AIR) == BlockType.JUKEBOX) {
            if (world.getBlockMetadataIfLoaded(pos.x(), pos.y(), pos.z(), 0) != metadata
                    && world.setBlockIfLoaded(pos.x(), pos.y(), pos.z(), BlockType.JUKEBOX, metadata)) {
                world.rebuildBlockMeshesNow(pos.x(), pos.y(), pos.z());
            }
        }
    }

    @Override
    public ItemStack[] getDrops() {
        return hasRecord() ? new ItemStack[] { getRecord() } : new ItemStack[0];
    }
}
