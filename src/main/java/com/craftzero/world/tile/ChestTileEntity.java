package com.craftzero.world.tile;

import com.craftzero.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChestTileEntity extends TileEntity {
    public static final int SIZE = 27;

    private final ItemStack[] inventory = new ItemStack[SIZE];
    private int openCount;
    private float lidAngle;
    private float prevLidAngle;

    public ChestTileEntity(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public String getTypeId() {
        return "chest";
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    public void open() {
        openCount++;
    }

    public void close() {
        if (openCount > 0) {
            openCount--;
        }
    }

    @Override
    public void tick(com.craftzero.world.World world, float deltaTime) {
        prevLidAngle = lidAngle;
        float target = openCount > 0 ? 1.0f : 0.0f;
        float speed = deltaTime * 4.0f;
        if (lidAngle < target) {
            lidAngle = Math.min(target, lidAngle + speed);
        } else if (lidAngle > target) {
            lidAngle = Math.max(target, lidAngle - speed);
        }
    }

    public float getLidAngle(float partialTick) {
        return prevLidAngle + (lidAngle - prevLidAngle) * partialTick;
    }

    public float getLidAngle() {
        return lidAngle;
    }

    public void setLidAngle(float lidAngle) {
        this.lidAngle = Math.max(0.0f, Math.min(1.0f, lidAngle));
        this.prevLidAngle = this.lidAngle;
    }

    @Override
    public ItemStack[] getDrops() {
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack stack : inventory) {
            if (stack != null && !stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
        return drops.toArray(new ItemStack[0]);
    }
}
