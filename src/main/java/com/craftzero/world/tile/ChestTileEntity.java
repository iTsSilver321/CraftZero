package com.craftzero.world.tile;

import com.craftzero.inventory.ItemStack;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;

import java.util.ArrayList;
import java.util.List;

public class ChestTileEntity extends TileEntity {
    public static final int SIZE = 27;
    public static final float LID_ANGLE_PER_TICK = 0.1f;

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

    public boolean open() {
        boolean wasClosed = openCount == 0;
        openCount++;
        return wasClosed;
    }

    public boolean close() {
        if (openCount > 0) {
            openCount--;
            return openCount == 0;
        }
        return false;
    }

    public int getOpenCount() {
        return openCount;
    }

    @Override
    public void tick(World world, float deltaTime) {
        prevLidAngle = lidAngle;
        float oldLidAngle = lidAngle;
        float target = openCount > 0 ? 1.0f : 0.0f;
        float speed = deltaTime * 20.0f * LID_ANGLE_PER_TICK;
        if (world != null && speed > 0.0f && openCount > 0 && lidAngle == 0.0f
                && shouldPlayLidSound(world)) {
            playLidSound(world, WorldSoundEvent.CHEST_OPEN);
        }
        if (lidAngle < target) {
            lidAngle = Math.min(target, lidAngle + speed);
        } else if (lidAngle > target) {
            lidAngle = Math.max(target, lidAngle - speed);
        }
        if (world != null && openCount == 0 && oldLidAngle >= 0.5f && lidAngle < 0.5f
                && shouldPlayLidSound(world)) {
            playLidSound(world, WorldSoundEvent.CHEST_CLOSE);
        }
    }

    private boolean shouldPlayLidSound(World world) {
        ChestTileEntity adjacent = world.getAdjacentChest(this);
        if (adjacent == null) {
            return true;
        }
        BlockPos pos = getPos();
        BlockPos adjacentPos = adjacent.getPos();
        return adjacentPos.z() > pos.z() || (adjacentPos.z() == pos.z() && adjacentPos.x() > pos.x());
    }

    private void playLidSound(World world, String soundId) {
        BlockPos pos = getPos();
        float soundX = pos.x() + 0.5f;
        float soundZ = pos.z() + 0.5f;
        ChestTileEntity adjacent = world.getAdjacentChest(this);
        if (adjacent != null) {
            BlockPos adjacentPos = adjacent.getPos();
            soundX += Math.signum(adjacentPos.x() - pos.x()) * 0.5f;
            soundZ += Math.signum(adjacentPos.z() - pos.z()) * 0.5f;
        }
        world.playChestSound(soundId, soundX, pos.y() + 0.5f, soundZ);
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
