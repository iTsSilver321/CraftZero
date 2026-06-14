package com.craftzero.inventory;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

/**
 * Small adapter for moving ItemStacks through inventory-like slot arrays.
 */
public final class SlotAccess {
    private final int size;
    private final IntFunction<ItemStack> getter;
    private final BiConsumer<Integer, ItemStack> setter;

    private SlotAccess(int size, IntFunction<ItemStack> getter, BiConsumer<Integer, ItemStack> setter) {
        if (size < 0) {
            throw new IllegalArgumentException("size must be non-negative");
        }
        this.size = size;
        this.getter = Objects.requireNonNull(getter, "getter");
        this.setter = Objects.requireNonNull(setter, "setter");
    }

    public static SlotAccess of(ItemStack[] slots) {
        Objects.requireNonNull(slots, "slots");
        return of(slots, 0, slots.length);
    }

    public static SlotAccess of(ItemStack[] slots, int offset, int length) {
        Objects.requireNonNull(slots, "slots");
        if (offset < 0 || length < 0 || offset + length > slots.length) {
            throw new IndexOutOfBoundsException("Invalid slot range");
        }
        return new SlotAccess(length, index -> slots[offset + index], (index, stack) -> slots[offset + index] = stack);
    }

    public static SlotAccess concat(SlotAccess... groups) {
        Objects.requireNonNull(groups, "groups");
        SlotAccess[] copy = Arrays.copyOf(groups, groups.length);
        int totalSize = 0;
        for (SlotAccess group : copy) {
            Objects.requireNonNull(group, "group");
            totalSize += group.size();
        }

        return new SlotAccess(totalSize, index -> {
            int localIndex = index;
            for (SlotAccess group : copy) {
                if (localIndex < group.size()) {
                    return group.get(localIndex);
                }
                localIndex -= group.size();
            }
            throw new IndexOutOfBoundsException(index);
        }, (index, stack) -> {
            int localIndex = index;
            for (SlotAccess group : copy) {
                if (localIndex < group.size()) {
                    group.set(localIndex, stack);
                    return;
                }
                localIndex -= group.size();
            }
            throw new IndexOutOfBoundsException(index);
        });
    }

    public int size() {
        return size;
    }

    public ItemStack get(int index) {
        checkIndex(index);
        return getter.apply(index);
    }

    public void set(int index, ItemStack stack) {
        checkIndex(index);
        setter.accept(index, stack);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index);
        }
    }
}
