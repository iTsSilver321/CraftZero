package com.craftzero.world.tile;

import com.craftzero.inventory.ItemStack;
import com.craftzero.world.World;

public abstract class TileEntity {
    private final BlockPos pos;
    private boolean dirty;

    protected TileEntity(int x, int y, int z) {
        this.pos = new BlockPos(x, y, z);
        this.dirty = true;
    }

    public BlockPos getPos() {
        return pos;
    }

    public abstract String getTypeId();

    public void tick(World world, float deltaTime) {
    }

    public ItemStack[] getDrops() {
        return new ItemStack[0];
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public void clearDirty() {
        dirty = false;
    }
}
