package com.craftzero.ui;

import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;

import java.util.List;
import java.util.function.BooleanSupplier;

final class ContainerKeyboardDrop {
    private ContainerKeyboardDrop() {
    }

    static DropResult dropOne(BooleanSupplier dropRequested, Inventory inventory, ContainerDragDistributor.Slots slots, int hoveredSlot,
            List<ItemStack> droppedItems) {
        if (!ContainerScreenControls.dropRequester(dropRequested).getAsBoolean()
                || inventory == null || droppedItems == null) {
            return DropResult.none();
        }

        ItemStack cursor = inventory.getCursorItem();
        if (!ItemStackOps.isEmpty(cursor)) {
            ItemStack dropped = ItemStackOps.splitOne(cursor);
            if (ItemStackOps.isEmpty(dropped)) {
                return DropResult.none();
            }
            droppedItems.add(dropped);
            if (cursor.isEmpty()) {
                inventory.setCursorItem(null);
            }
            return new DropResult(true, -1, true);
        }

        if (slots == null || hoveredSlot < 0) {
            return DropResult.none();
        }
        ItemStack source = slots.get(hoveredSlot);
        if (ItemStackOps.isEmpty(source) || !slots.canPlace(hoveredSlot, source)) {
            return DropResult.none();
        }

        ItemStack dropped = ItemStackOps.splitOne(source);
        if (ItemStackOps.isEmpty(dropped)) {
            return DropResult.none();
        }
        droppedItems.add(dropped);
        if (source.isEmpty()) {
            slots.set(hoveredSlot, null);
        }
        return new DropResult(true, hoveredSlot, false);
    }

    record DropResult(boolean dropped, int sourceSlot, boolean fromCursor) {
        static DropResult none() {
            return new DropResult(false, -1, false);
        }
    }
}
