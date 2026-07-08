package com.craftzero.world.tile;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.RedstoneEngine;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DispenserTileEntity extends TileEntity {
    public static final int SIZE = 9;

    private final ItemStack[] inventory = new ItemStack[SIZE];

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
        if (world == null) {
            return;
        }
        int slot = chooseFilledSlot(world.getRandom());
        if (slot < 0) {
            playDispenseClick(world, 1.2f);
            return;
        }
        ItemStack one = ItemStackOps.splitOne(inventory[slot]);
        if (inventory[slot] != null && inventory[slot].isEmpty()) {
            inventory[slot] = null;
        }
        if (one == null || one.isEmpty()) {
            playDispenseClick(world, 1.2f);
            return;
        }
        playDispenseEffect(world, one);
        BlockPos pos = getPos();
        int metadata = world.getBlockMetadataIfLoaded(pos.x(), pos.y(), pos.z(), 0);
        RedstoneEngine.dropDispenserItem(world, pos.x(), pos.y(), pos.z(), metadata, one);
        world.spawnDispenserSmokeParticles(pos.x(), pos.y(), pos.z(), metadata);
        markDirty();
    }

    private void playDispenseEffect(World world, ItemStack stack) {
        if (usesProjectileDispenseEffect(stack)) {
            BlockPos pos = getPos();
            world.playSound(WorldSoundEvent.BOW,
                    pos.x() + 0.5f, pos.y() + 0.5f, pos.z() + 0.5f, 1.0f, 1.2f);
        } else {
            playDispenseClick(world, 1.0f);
        }
    }

    private boolean usesProjectileDispenseEffect(ItemStack stack) {
        ItemType type = stack.getType();
        return type == ItemType.ARROW
                || type == ItemType.EGG
                || type == ItemType.SNOWBALL
                || (type == ItemType.POTION && stack.getPotionData() != null && stack.getPotionData().splash());
    }

    private void playDispenseClick(World world, float pitch) {
        BlockPos pos = getPos();
        world.playSound(WorldSoundEvent.DISPENSER_CLICK,
                pos.x() + 0.5f, pos.y() + 0.5f, pos.z() + 0.5f, 1.0f, pitch);
    }

    private int chooseFilledSlot(Random random) {
        int slot = -1;
        int seen = 1;
        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] != null && !inventory[i].isEmpty()) {
                if (random.nextInt(seen++) == 0) {
                    slot = i;
                }
            }
        }
        return slot;
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
