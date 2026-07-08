package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.inventory.Inventory;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemStackOps;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;

final class ContainerHotbarSwap {
    private static final int HOTBAR_KEYS = 9;

    private ContainerHotbarSwap() {
    }

    static boolean trySwapWithHotbar(Inventory inventory, ContainerDragDistributor.Slots slots, int hoveredSlot,
            int hotbarSlotStart) {
        int hotbarIndex = pressedHotbarIndex();
        if (hotbarIndex < 0 || hoveredSlot < 0 || inventory == null || slots == null
                || !ItemStackOps.isEmpty(inventory.getCursorItem())) {
            return false;
        }

        int hotbarSlot = hotbarSlotStart + hotbarIndex;
        if (hoveredSlot == hotbarSlot) {
            return false;
        }

        ItemStack hoveredStack = slots.get(hoveredSlot);
        ItemStack hotbarStack = slots.get(hotbarSlot);
        if (ItemStackOps.isEmpty(hoveredStack) && ItemStackOps.isEmpty(hotbarStack)) {
            return false;
        }
        if (!canLeaveInSlot(slots, hoveredSlot, hotbarStack, hoveredStack)) {
            return false;
        }

        slots.set(hoveredSlot, hotbarStack);
        slots.set(hotbarSlot, hoveredStack);
        return true;
    }

    private static int pressedHotbarIndex() {
        for (int i = 0; i < HOTBAR_KEYS; i++) {
            if (Input.isKeyPressed(GLFW_KEY_1 + i)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean canLeaveInSlot(ContainerDragDistributor.Slots slots, int slotIndex,
            ItemStack replacement, ItemStack current) {
        ItemStack candidate = ItemStackOps.isEmpty(replacement) ? current : replacement;
        if (ItemStackOps.isEmpty(candidate) || !slots.canPlace(slotIndex, candidate)) {
            return false;
        }
        return candidate.getCount() <= slots.maxStackSize(slotIndex, candidate);
    }
}
