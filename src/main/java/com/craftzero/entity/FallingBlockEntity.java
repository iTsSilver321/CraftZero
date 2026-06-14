package com.craftzero.entity;

import com.craftzero.inventory.ItemStack;
import com.craftzero.world.BlockType;

/**
 * Release-style falling sand/gravel entity.
 */
public class FallingBlockEntity extends Entity {
    private static final int MAX_AGE_TICKS = 600;

    private final BlockType blockType;
    private final int metadata;

    public FallingBlockEntity(BlockType blockType, int metadata) {
        super(0.98f, 0.98f);
        this.blockType = blockType;
        this.metadata = metadata;
    }

    @Override
    public void tick() {
        super.tick();
        if (ticksExisted > MAX_AGE_TICKS || y < -1.0f) {
            dropAsItem();
            remove();
        }
    }

    @Override
    public void updatePhysics(float deltaTime) {
        if (removed) {
            return;
        }
        super.updatePhysics(deltaTime);
        if (onGround || collidedVertically) {
            settle();
        }
    }

    @Override
    protected float getGravityPerTick() {
        return 0.04f;
    }

    @Override
    protected float getAirResistance() {
        return 0.98f;
    }

    private void settle() {
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y);
        int blockZ = (int) Math.floor(z);

        if (world != null && world.canPlaceFallingBlockAt(blockX, blockY, blockZ, blockType, metadata)) {
            world.setBlock(blockX, blockY, blockZ, blockType, metadata);
        } else {
            dropAsItem();
        }
        remove();
    }

    private void dropAsItem() {
        if (world == null) {
            return;
        }
        ItemStack drop = new ItemStack(blockType.getDroppedItem(), 1);
        world.spawnThrownStack(x, y + 0.25f, z, drop, 0.0f, 0.1f, 0.0f);
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public int getMetadata() {
        return metadata;
    }
}
