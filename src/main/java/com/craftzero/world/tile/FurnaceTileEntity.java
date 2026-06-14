package com.craftzero.world.tile;

import com.craftzero.inventory.ItemStack;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;

import java.util.ArrayList;
import java.util.List;

public class FurnaceTileEntity extends TileEntity {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_FUEL = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int SIZE = 3;
    public static final int COOK_TIME_TOTAL = 200;

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
        boolean wasBurning = isBurning();
        if (burnTime > 0) {
            burnTime--;
            markDirty();
        }

        if (!isBurning() && canSmelt()) {
            int fuelBurnTime = FuelRegistry.getBurnTime(inventory[SLOT_FUEL]);
            if (fuelBurnTime > 0) {
                currentFuelBurnTime = fuelBurnTime;
                burnTime = fuelBurnTime;
                inventory[SLOT_FUEL].remove(1);
                if (inventory[SLOT_FUEL].isEmpty()) {
                    inventory[SLOT_FUEL] = null;
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
            if (cookTime != 0) {
                cookTime = 0;
                markDirty();
            }
        }

        boolean burning = isBurning();
        if (burning != wasBurning) {
            BlockPos pos = getPos();
            BlockType current = world.getBlock(pos.x(), pos.y(), pos.z());
            if (current == BlockType.FURNACE || current == BlockType.LIT_FURNACE) {
                world.setBlockPreservingTile(pos.x(), pos.y(), pos.z(),
                        burning ? BlockType.LIT_FURNACE : BlockType.FURNACE,
                        world.getBlockMetadata(pos.x(), pos.y(), pos.z()));
            }
        }
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
