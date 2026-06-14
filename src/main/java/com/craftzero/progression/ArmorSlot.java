package com.craftzero.progression;

public enum ArmorSlot {
    HELMET(0),
    CHESTPLATE(1),
    LEGGINGS(2),
    BOOTS(3);

    private final int index;

    ArmorSlot(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
