package com.craftzero.inventory;

/**
 * Shared stack movement primitives that preserve the full ItemStack identity.
 */
public final class ItemStackOps {
    private ItemStackOps() {
    }

    public static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.isEmpty();
    }

    public static boolean canMerge(ItemStack target, ItemStack source) {
        return !isEmpty(target) && !isEmpty(source) && target.canMergeWith(source);
    }

    public static ItemStack copyWithCount(ItemStack stack, int count) {
        if (isEmpty(stack) || count <= 0) {
            return null;
        }
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    public static ItemStack split(ItemStack source, int amount) {
        if (isEmpty(source) || amount <= 0) {
            return null;
        }
        int moved = Math.min(amount, source.getCount());
        ItemStack split = copyWithCount(source, moved);
        source.remove(moved);
        return split;
    }

    public static ItemStack splitOne(ItemStack source) {
        return split(source, 1);
    }

    public static ItemStack splitHalf(ItemStack source) {
        if (isEmpty(source)) {
            return null;
        }
        return split(source, (source.getCount() + 1) / 2);
    }

    public static int mergeInto(ItemStack target, ItemStack source) {
        return mergeAmountInto(target, source, Integer.MAX_VALUE);
    }

    public static int mergeAmountInto(ItemStack target, ItemStack source, int amount) {
        if (amount <= 0 || !canMerge(target, source)) {
            return 0;
        }
        int space = target.getMaxStackSize() - target.getCount();
        if (space <= 0) {
            return 0;
        }
        int moved = Math.min(Math.min(space, source.getCount()), amount);
        target.add(moved);
        source.remove(moved);
        return moved;
    }

    public static int moveIntoSlots(SlotAccess slots, ItemStack source) {
        int moved = mergeIntoSlots(slots, source);
        if (!isEmpty(source)) {
            moved += placeIntoEmptySlots(slots, source);
        }
        return moved;
    }

    public static int mergeIntoSlots(SlotAccess slots, ItemStack source) {
        if (slots == null || isEmpty(source)) {
            return 0;
        }
        int moved = 0;
        for (int i = 0; i < slots.size() && !isEmpty(source); i++) {
            moved += mergeInto(slots.get(i), source);
        }
        return moved;
    }

    public static int placeIntoEmptySlots(SlotAccess slots, ItemStack source) {
        if (slots == null || isEmpty(source)) {
            return 0;
        }
        int moved = 0;
        for (int i = 0; i < slots.size() && !isEmpty(source); i++) {
            if (isEmpty(slots.get(i))) {
                int toMove = Math.min(source.getMaxStackSize(), source.getCount());
                slots.set(i, split(source, toMove));
                moved += toMove;
            }
        }
        return moved;
    }

    public static boolean canFullyMoveInto(SlotAccess slots, ItemStack source) {
        return isEmpty(source) || countAddable(slots, source) >= source.getCount();
    }

    public static int countAddable(SlotAccess slots, ItemStack source) {
        if (slots == null || isEmpty(source)) {
            return 0;
        }
        int addable = 0;
        for (int i = 0; i < slots.size(); i++) {
            ItemStack slot = slots.get(i);
            if (isEmpty(slot)) {
                addable += source.getMaxStackSize();
            } else if (canMerge(slot, source)) {
                addable += Math.max(0, slot.getMaxStackSize() - slot.getCount());
            }
            if (addable >= source.getCount()) {
                return addable;
            }
        }
        return addable;
    }
}
