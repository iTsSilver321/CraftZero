package com.craftzero.ui;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class ContainerDragDistributor {
    private ContainerDragDistributor() {
    }

    interface Slots {
        ItemStack get(int slotIndex);

        void set(int slotIndex, ItemStack stack);

        boolean canPlace(int slotIndex, ItemStack stack);

        int maxStackSize(int slotIndex, ItemStack stack);
    }

    static boolean canDragInto(Slots slots, int slotIndex, ItemStack cursor) {
        if (slots == null || slotIndex < 0 || ItemStackOps.isEmpty(cursor)
                || !slots.canPlace(slotIndex, cursor)) {
            return false;
        }
        ItemStack target = slots.get(slotIndex);
        if (ItemStackOps.isEmpty(target)) {
            return true;
        }
        int slotMax = Math.min(target.getMaxStackSize(), slots.maxStackSize(slotIndex, cursor));
        return ItemStackOps.canMerge(target, cursor) && target.getCount() < slotMax;
    }

    static int distribute(Slots slots, Collection<Integer> slotIndices, ItemStack cursor, boolean rightClick) {
        if (slots == null || slotIndices == null || ItemStackOps.isEmpty(cursor)) {
            return 0;
        }
        List<Integer> eligible = new ArrayList<>();
        for (int slotIndex : slotIndices) {
            if (canDragInto(slots, slotIndex, cursor)) {
                eligible.add(slotIndex);
            }
        }
        int moved = 0;
        int perSlotLeftDragAmount = leftDragAmount(cursor, eligible.size());
        for (int slotIndex : eligible) {
            if (ItemStackOps.isEmpty(cursor)) {
                break;
            }
            int amount = rightClick ? 1 : perSlotLeftDragAmount;
            moved += placeAmount(slots, slotIndex, cursor, amount);
        }
        return moved;
    }

    private static int leftDragAmount(ItemStack cursor, int slotCount) {
        if (ItemStackOps.isEmpty(cursor) || slotCount <= 0) {
            return 0;
        }
        return Math.max(1, cursor.getCount() / slotCount);
    }

    private static int placeAmount(Slots slots, int slotIndex, ItemStack cursor, int amount) {
        if (amount <= 0 || !canDragInto(slots, slotIndex, cursor)) {
            return 0;
        }
        int slotMax = Math.max(0, slots.maxStackSize(slotIndex, cursor));
        if (slotMax <= 0) {
            return 0;
        }
        ItemStack target = slots.get(slotIndex);
        if (ItemStackOps.isEmpty(target)) {
            int moved = Math.min(Math.min(amount, cursor.getCount()), slotMax);
            slots.set(slotIndex, ItemStackOps.split(cursor, moved));
            return moved;
        }
        int space = Math.min(target.getMaxStackSize(), slotMax) - target.getCount();
        int moved = Math.min(Math.min(amount, cursor.getCount()), space);
        if (moved <= 0) {
            return 0;
        }
        target.add(moved);
        cursor.remove(moved);
        return moved;
    }

}
