package com.craftzero.ui;

import java.util.ArrayList;
import java.util.List;

final class ContainerSlotOrder {
    private ContainerSlotOrder() {
    }

    static int[] range(int startInclusive, int endExclusive) {
        if (endExclusive <= startInclusive) {
            return new int[0];
        }
        int[] slots = new int[endExclusive - startInclusive];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = startInclusive + i;
        }
        return slots;
    }

    static int[] reverseRange(int startInclusive, int endExclusive) {
        if (endExclusive <= startInclusive) {
            return new int[0];
        }
        int[] slots = new int[endExclusive - startInclusive];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = endExclusive - 1 - i;
        }
        return slots;
    }

    static int[] playerInventoryReverse(int playerStartInclusive, int mainSize, int hotbarSize) {
        int hotbarStart = playerStartInclusive + mainSize;
        return concat(
                reverseRange(hotbarStart, hotbarStart + hotbarSize),
                reverseRange(playerStartInclusive, hotbarStart));
    }

    static int[] concat(int[]... ranges) {
        int size = 0;
        for (int[] range : ranges) {
            size += range == null ? 0 : range.length;
        }
        int[] slots = new int[size];
        int index = 0;
        for (int[] range : ranges) {
            if (range == null) {
                continue;
            }
            for (int slot : range) {
                slots[index++] = slot;
            }
        }
        return slots;
    }

    static int[] filteredRange(int startInclusive, int endExclusive, int... excludedSlots) {
        if (endExclusive <= startInclusive) {
            return new int[0];
        }
        List<Integer> slots = new ArrayList<>();
        for (int slot = startInclusive; slot < endExclusive; slot++) {
            if (!contains(excludedSlots, slot)) {
                slots.add(slot);
            }
        }
        int[] ordered = new int[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            ordered[i] = slots.get(i);
        }
        return ordered;
    }

    static int[] clickedGroupFirst(int clickedSlot, int groupStartInclusive, int groupEndExclusive,
            int[] primaryGroup, int[] secondaryGroup) {
        if (clickedSlot >= groupStartInclusive && clickedSlot < groupEndExclusive) {
            return concat(primaryGroup, secondaryGroup);
        }
        return concat(secondaryGroup, primaryGroup);
    }

    private static boolean contains(int[] values, int target) {
        if (values == null) {
            return false;
        }
        for (int value : values) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }
}
