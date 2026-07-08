package com.craftzero.ui;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;

final class ContainerDoubleClickCollector {
    private ContainerDoubleClickCollector() {
    }

    static boolean collectMatching(ContainerDragDistributor.Slots slots, int[] slotOrder, ItemStack cursor) {
        if (slots == null || slotOrder == null || ItemStackOps.isEmpty(cursor)) {
            return false;
        }
        int remainingSpace = cursor.getMaxStackSize() - cursor.getCount();
        if (remainingSpace <= 0) {
            return false;
        }

        boolean movedAny = false;
        for (int slotIndex : slotOrder) {
            if (remainingSpace <= 0) {
                break;
            }
            ItemStack source = slots.get(slotIndex);
            if (!slots.canPlace(slotIndex, cursor) || !ItemStackOps.canMerge(cursor, source)) {
                continue;
            }
            int moved = Math.min(remainingSpace, source.getCount());
            cursor.add(moved);
            source.remove(moved);
            remainingSpace -= moved;
            movedAny = true;
            if (source.isEmpty()) {
                slots.set(slotIndex, null);
            }
        }
        return movedAny;
    }
}
