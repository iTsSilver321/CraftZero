package com.craftzero.world.tile;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;

import java.util.ArrayList;
import java.util.List;

public class BrewingStandTileEntity extends TileEntity {
    public static final int SLOT_BOTTLE_0 = 0;
    public static final int SLOT_BOTTLE_1 = 1;
    public static final int SLOT_BOTTLE_2 = 2;
    public static final int SLOT_INGREDIENT = 3;
    public static final int SIZE = 4;
    public static final int BREW_TIME_TOTAL = 400;

    private final ItemStack[] inventory = new ItemStack[SIZE];
    private int brewTime;
    private ItemType brewingIngredientType;
    private int filledSlots;
    private float tickAccumulator;

    public BrewingStandTileEntity(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public String getTypeId() {
        return "brewing_stand";
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
        if (brewTime > 0) {
            brewTime--;
            markDirty();
            if (brewTime == 0) {
                brew();
            } else if (!canBrew() || brewingIngredientType != currentIngredientType()) {
                brewTime = 0;
                brewingIngredientType = null;
                markDirty();
            }
        } else if (canBrew()) {
            brewTime = BREW_TIME_TOTAL;
            brewingIngredientType = currentIngredientType();
            markDirty();
        }
        updateFilledSlots(world);
    }

    private ItemType currentIngredientType() {
        ItemStack ingredient = inventory[SLOT_INGREDIENT];
        return ingredient == null || ingredient.isEmpty() ? null : ingredient.getType();
    }

    private void updateFilledSlots(World world) {
        int slots = getFilledSlots();
        if (slots == filledSlots) {
            return;
        }
        filledSlots = slots;
        if (world != null) {
            BlockPos pos = getPos();
            if (world.getBlockIfLoaded(pos.x(), pos.y(), pos.z(), BlockType.AIR) == BlockType.BREWING_STAND) {
                world.setBlockPreservingTile(pos.x(), pos.y(), pos.z(), BlockType.BREWING_STAND, slots);
                world.rebuildBlockMeshesNow(pos.x(), pos.y(), pos.z());
            }
        }
        markDirty();
    }

    public boolean canBrew() {
        ItemStack ingredient = inventory[SLOT_INGREDIENT];
        if (!BrewingRecipeRegistry.isIngredient(ingredient)) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            if (BrewingRecipeRegistry.brew(inventory[i], ingredient) != null) {
                return true;
            }
        }
        return false;
    }

    private void brew() {
        ItemStack ingredient = inventory[SLOT_INGREDIENT];
        if (!BrewingRecipeRegistry.isIngredient(ingredient)) {
            return;
        }
        boolean changed = false;
        for (int i = 0; i < 3; i++) {
            ItemStack result = BrewingRecipeRegistry.brew(inventory[i], ingredient);
            if (result != null) {
                inventory[i] = result;
                changed = true;
            }
        }
        if (changed) {
            ingredient.remove(1);
            if (ingredient.isEmpty()) {
                inventory[SLOT_INGREDIENT] = null;
            }
            brewingIngredientType = null;
            markDirty();
        }
    }

    public int getFilledSlots() {
        int slots = 0;
        for (int i = 0; i < 3; i++) {
            ItemStack stack = inventory[i];
            if (BrewingRecipeRegistry.isBottleSlotItem(stack)) {
                slots |= 1 << i;
            }
        }
        return slots;
    }

    public int getBrewTime() {
        return brewTime;
    }

    public void setBrewTime(int brewTime) {
        this.brewTime = Math.max(0, Math.min(BREW_TIME_TOTAL, brewTime));
        this.brewingIngredientType = this.brewTime > 0 ? currentIngredientType() : null;
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
