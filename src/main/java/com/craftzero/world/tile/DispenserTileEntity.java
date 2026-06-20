package com.craftzero.world.tile;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;
import com.craftzero.world.RedstoneEngine;
import com.craftzero.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DispenserTileEntity extends TileEntity {
    public static final int SIZE = 9;

    private final ItemStack[] inventory = new ItemStack[SIZE];
    private final Random random = new Random();

    public DispenserTileEntity(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
    public String getTypeId() {
        return "dispenser";
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    public void dispense(World world) {
        int slot = chooseFilledSlot();
        if (slot < 0) {
            return;
        }
        ItemStack one = ItemStackOps.splitOne(inventory[slot]);
        if (inventory[slot] != null && inventory[slot].isEmpty()) {
            inventory[slot] = null;
        }
        if (one == null || one.isEmpty()) {
            return;
        }
        BlockPos pos = getPos();
        int metadata = world.getBlockMetadataIfLoaded(pos.x(), pos.y(), pos.z(), 0);
        RedstoneEngine.dropDispenserItem(world, pos.x(), pos.y(), pos.z(), metadata, one);
        markDirty();
    }

    private int chooseFilledSlot() {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] != null && !inventory[i].isEmpty()) {
                slots.add(i);
            }
        }
        if (slots.isEmpty()) {
            return -1;
        }
        return slots.get(random.nextInt(slots.size()));
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
