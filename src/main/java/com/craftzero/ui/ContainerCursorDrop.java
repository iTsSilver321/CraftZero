package com.craftzero.ui;

import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;

import java.util.List;

final class ContainerCursorDrop {
    private ContainerCursorDrop() {
    }

    static boolean dropOutside(Inventory inventory, List<ItemStack> droppedItems, boolean rightClick) {
        if (inventory == null || droppedItems == null) {
            return false;
        }
        ItemStack cursor = inventory.getCursorItem();
        if (ItemStackOps.isEmpty(cursor)) {
            inventory.setCursorItem(null);
            return false;
        }
        if (!rightClick) {
            droppedItems.add(cursor);
            inventory.setCursorItem(null);
            return true;
        }

        ItemStack dropped = ItemStackOps.splitOne(cursor);
        if (ItemStackOps.isEmpty(dropped)) {
            return false;
        }
        droppedItems.add(dropped);
        if (cursor.isEmpty()) {
            inventory.setCursorItem(null);
        }
        return true;
    }
}
