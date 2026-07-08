package com.craftzero.entity;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.Chunk;

/**
 * Release-style falling sand/gravel entity.
 */
public class FallingBlockEntity extends Entity {
    private static final int OUT_OF_WORLD_DROP_TICKS = 100;
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
        if (ticksExisted > MAX_AGE_TICKS
                || (ticksExisted > OUT_OF_WORLD_DROP_TICKS && isOutsideVerticalWorld())) {
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

    @Override
    protected float getWaterGravityPerTick() {
        return 0.035f;
    }

    @Override
    protected float getWaterHorizontalDrag() {
        return 0.72f;
    }

    @Override
    protected float getWaterVerticalDrag() {
        return 0.86f;
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
        ItemType droppedItem = ItemType.fromBlock(blockType, metadata);
        if (droppedItem != null) {
            world.spawnThrownStack(x, y + 0.25f, z, new ItemStack(droppedItem, 1), 0.0f, 0.1f, 0.0f);
        }
    }

    private boolean isOutsideVerticalWorld() {
        int blockY = (int) Math.floor(y);
        return blockY < 1 || blockY > Chunk.HEIGHT;
    }

    public BlockType getBlockType() {
        return blockType;
    }

    public int getMetadata() {
        return metadata;
    }
}
