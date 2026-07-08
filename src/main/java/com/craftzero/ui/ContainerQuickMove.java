package com.craftzero.ui;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;

final class ContainerQuickMove {
    private ContainerQuickMove() {
    }

    static boolean moveSlot(ContainerDragDistributor.Slots slots, int sourceSlot, int[] destinationSlots) {
        if (slots == null || destinationSlots == null || sourceSlot < 0) {
            return false;
        }
        ItemStack source = slots.get(sourceSlot);
        int moved = moveStack(slots, source, sourceSlot, destinationSlots);
        if (moved > 0 && ItemStackOps.isEmpty(source)) {
            slots.set(sourceSlot, null);
        }
        return moved > 0;
    }

    static int moveStack(ContainerDragDistributor.Slots slots, ItemStack source, int sourceSlot,
            int[] destinationSlots) {
        if (slots == null || destinationSlots == null || ItemStackOps.isEmpty(source)) {
            return 0;
        }

        int moved = mergeIntoExistingStacks(slots, source, sourceSlot, destinationSlots);
        if (!ItemStackOps.isEmpty(source)) {
            moved += placeIntoEmptySlots(slots, source, sourceSlot, destinationSlots);
        }
        return moved;
    }

    private static int mergeIntoExistingStacks(ContainerDragDistributor.Slots slots, ItemStack source,
            int sourceSlot, int[] destinationSlots) {
        int moved = 0;
        for (int slotIndex : destinationSlots) {
            if (slotIndex == sourceSlot || ItemStackOps.isEmpty(source)
                    || !slots.canPlace(slotIndex, source)) {
                continue;
            }
            ItemStack target = slots.get(slotIndex);
            if (!ItemStackOps.canMerge(target, source)) {
                continue;
            }
            int max = Math.min(target.getMaxStackSize(), slots.maxStackSize(slotIndex, source));
            int space = max - target.getCount();
            if (space <= 0) {
                continue;
            }
            int amount = Math.min(space, source.getCount());
            target.add(amount);
            source.remove(amount);
            moved += amount;
        }
        return moved;
    }

    private static int placeIntoEmptySlots(ContainerDragDistributor.Slots slots, ItemStack source,
            int sourceSlot, int[] destinationSlots) {
        int moved = 0;
        for (int slotIndex : destinationSlots) {
            if (slotIndex == sourceSlot || ItemStackOps.isEmpty(source)
                    || !slots.canPlace(slotIndex, source) || !ItemStackOps.isEmpty(slots.get(slotIndex))) {
                continue;
            }
            int max = Math.min(source.getMaxStackSize(), slots.maxStackSize(slotIndex, source));
            if (max <= 0) {
                continue;
            }
            int amount = Math.min(max, source.getCount());
            slots.set(slotIndex, ItemStackOps.split(source, amount));
            moved += amount;
        }
        return moved;
    }
}
