package com.craftzero.world.tile;

import com.craftzero.inventory.ItemStack;
import com.craftzero.world.World;

public class JukeboxTileEntity extends TileEntity {
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
        markDirty();
        return true;
    }

    public ItemStack removeRecord() {
        ItemStack removed = getRecord();
        record = null;
        markDirty();
        return removed;
    }

    public int getPlayTicks() {
        return playTicks;
    }

    public void play(World world) {
        if (hasRecord()) {
            playTicks++;
            markDirty();
        }
    }

    @Override
    public ItemStack[] getDrops() {
        return hasRecord() ? new ItemStack[] { getRecord() } : new ItemStack[0];
    }
}
