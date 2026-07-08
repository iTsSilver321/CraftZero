package com.craftzero.world.tile;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FurnaceTileEntity extends TileEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SIZE = 3;
    public static final int COOK_TIME_TOTAL = 200;
    private static final float AMBIENT_SIDE_OFFSET = 0.52f;
    private static final float AMBIENT_RANDOM_OFFSET_RANGE = 0.6f;
    private static final float AMBIENT_RANDOM_OFFSET_CENTER = 0.3f;
    private static final int AMBIENT_PARTICLE_LIFETIME_TICKS = 16;

    private final ItemStack[] inventory = new ItemStack[SIZE];
    private int burnTime;
    private int currentFuelBurnTime;
    private int cookTime;
    private float tickAccumulator;

    public FurnaceTileEntity(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public String getTypeId() {
        return "furnace";
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    @Override
    public void tick(World world, float deltaTime) {
        tickAccumulator += deltaTime * 20.0f;
        while (tickAccumulator >= 1.0f) {
            tickAccumulator -= 1.0f;
            tickOne(world);
        }
    }

    private void tickOne(World world) {
        if (burnTime > 0) {
            burnTime--;
            markDirty();
        }

        boolean hasInput = inventory[SLOT_INPUT] != null && !inventory[SLOT_INPUT].isEmpty();
        boolean hasFuel = inventory[SLOT_FUEL] != null && !inventory[SLOT_FUEL].isEmpty();
        boolean shouldProcess = burnTime > 0 || hasFuel && hasInput || cookTime > 0;
        if (shouldProcess) {
            if (!isBurning() && canSmelt()) {
                ItemStack fuel = inventory[SLOT_FUEL];
                int fuelBurnTime = fuel == null || fuel.isEmpty() ? 0 : FuelRegistry.getBurnTime(fuel);
                if (fuelBurnTime > 0) {
                    ItemType containerItem = fuel.getType().getContainerItem();
                    currentFuelBurnTime = fuelBurnTime;
                    burnTime = fuelBurnTime;
                    fuel.remove(1);
                    if (fuel.isEmpty()) {
                        inventory[SLOT_FUEL] = containerItem == null ? null : new ItemStack(containerItem, 1);
                    }
                    markDirty();
                }
            }

            if (isBurning() && canSmelt()) {
                cookTime++;
                if (cookTime >= COOK_TIME_TOTAL) {
                    cookTime = 0;
                    smeltItem();
                }
                markDirty();
            } else {
                resetCookProgress();
            }
        }

        boolean burning = isBurning();
        if (world != null) {
            syncBlockBurnState(world, burning);
            if (burning) {
                emitBurningAmbience(world);
            }
        }
    }

    private void resetCookProgress() {
        if (cookTime != 0) {
            cookTime = 0;
            markDirty();
        }
    }

    private void syncBlockBurnState(World world, boolean burning) {
        BlockPos pos = getPos();
        BlockType current = world.getBlockIfLoaded(pos.x(), pos.y(), pos.z(), BlockType.AIR);
        BlockType expected = burning ? BlockType.LIT_FURNACE : BlockType.FURNACE;
        if ((current == BlockType.FURNACE || current == BlockType.LIT_FURNACE) && current != expected) {
            world.setBlockPreservingTile(pos.x(), pos.y(), pos.z(),
                    expected,
                    world.getBlockMetadataIfLoaded(pos.x(), pos.y(), pos.z(), 0));
            world.rebuildBlockMeshesNow(pos.x(), pos.y(), pos.z());
        }
    }

    private void emitBurningAmbience(World world) {
        BlockPos pos = getPos();
        if (world.getBlockIfLoaded(pos.x(), pos.y(), pos.z(), BlockType.AIR) != BlockType.LIT_FURNACE) {
            return;
        }

        Random random = world.getRandom();
        int metadata = world.getBlockMetadataIfLoaded(pos.x(), pos.y(), pos.z(), 0);
        float particleX = pos.x() + 0.5f;
        float particleY = pos.y() + random.nextFloat() * 6.0f / 16.0f;
        float particleZ = pos.z() + 0.5f;
        float sideJitter = random.nextFloat() * AMBIENT_RANDOM_OFFSET_RANGE - AMBIENT_RANDOM_OFFSET_CENTER;
        switch (metadata) {
            case 4 -> {
                particleX -= AMBIENT_SIDE_OFFSET;
                particleZ += sideJitter;
            }
            case 5 -> {
                particleX += AMBIENT_SIDE_OFFSET;
                particleZ += sideJitter;
            }
            case 2 -> {
                particleX += sideJitter;
                particleZ -= AMBIENT_SIDE_OFFSET;
            }
            default -> {
                particleX += sideJitter;
                particleZ += AMBIENT_SIDE_OFFSET;
            }
        }
        world.spawnParticle(WorldParticle.Type.SMOKE, particleX, particleY, particleZ,
                0.0f, 0.015f, 0.0f, 0.20f, AMBIENT_PARTICLE_LIFETIME_TICKS);
        world.spawnParticle(WorldParticle.Type.FLAME, particleX, particleY, particleZ,
                0.0f, 0.0f, 0.0f, 0.16f, AMBIENT_PARTICLE_LIFETIME_TICKS / 2);
    }

    public boolean isBurning() {
        return burnTime > 0;
    }

    public boolean canSmelt() {
        ItemStack result = SmeltingRegistry.getResult(inventory[SLOT_INPUT]);
        if (result == null) {
            return false;
        }
        ItemStack output = inventory[SLOT_OUTPUT];
        if (output == null || output.isEmpty()) {
            return true;
        }
        if (output.getType() != result.getType() || output.getDurability() != result.getDurability()) {
            return false;
        }
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private void smeltItem() {
        if (!canSmelt()) {
            return;
        }
        ItemStack result = SmeltingRegistry.getResult(inventory[SLOT_INPUT]);
        if (inventory[SLOT_OUTPUT] == null || inventory[SLOT_OUTPUT].isEmpty()) {
            inventory[SLOT_OUTPUT] = result;
        } else {
            inventory[SLOT_OUTPUT].add(result.getCount());
        }
        inventory[SLOT_INPUT].remove(1);
        if (inventory[SLOT_INPUT].isEmpty()) {
            inventory[SLOT_INPUT] = null;
        }
        markDirty();
    }

    public int getBurnTime() {
        return burnTime;
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = Math.max(0, burnTime);
    }

    public int getCurrentFuelBurnTime() {
        return currentFuelBurnTime;
    }

    public void setCurrentFuelBurnTime(int currentFuelBurnTime) {
        this.currentFuelBurnTime = Math.max(0, currentFuelBurnTime);
    }

    public int getCookTime() {
        return cookTime;
    }

    public void setCookTime(int cookTime) {
        this.cookTime = Math.max(0, Math.min(COOK_TIME_TOTAL, cookTime));
    }

    public float getTickAccumulator() {
        return tickAccumulator;
    }

    public void setTickAccumulator(float tickAccumulator) {
        this.tickAccumulator = Math.max(0.0f, Math.min(1.0f, tickAccumulator));
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
