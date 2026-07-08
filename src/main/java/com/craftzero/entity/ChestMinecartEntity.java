package com.craftzero.entity;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;

import java.util.ArrayList;
import java.util.List;

public class ChestMinecartEntity extends MinecartEntity {
    public static final int SIZE = 27;
    private final ItemStack[] inventory = new ItemStack[SIZE];

    public ChestMinecartEntity() {
        super(CartKind.CHEST);
    }

    public ChestMinecartEntity(float x, float y, float z) {
        super(x, y, z, CartKind.CHEST);
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    @Override
    public void dropAsItem() {
        if (world != null) {
            for (ItemStack stack : getDrops()) {
                world.spawnThrownStack(getX(), getY() + 0.25f, getZ(), stack, 0.0f, 0.15f, 0.0f);
            }
            world.spawnThrownStack(getX(), getY() + 0.25f, getZ(),
                    new ItemStack(ItemType.CHEST, 1), 0.0f, 0.15f, 0.0f);
        }
        super.dropAsItem();
    }

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
