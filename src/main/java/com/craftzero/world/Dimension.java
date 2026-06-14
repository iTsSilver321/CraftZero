package com.craftzero.world;

/**
 * Release 1.0 dimensions. The numeric ids match classic Java save data.
 */
public enum Dimension {
    NETHER(-1, "the_nether"),
    OVERWORLD(0, "overworld"),
    THE_END(1, "the_end");

    private final int id;
    private final String saveName;

    Dimension(int id, String saveName) {
        this.id = id;
        this.saveName = saveName;
    }

    public int getId() {
        return id;
    }

    public String getSaveName() {
        return saveName;
    }

    public static Dimension fromSaveName(String value) {
        if (value == null || value.isBlank()) {
            return OVERWORLD;
        }
        for (Dimension dimension : values()) {
            if (dimension.saveName.equalsIgnoreCase(value) || dimension.name().equalsIgnoreCase(value)) {
                return dimension;
            }
        }
        return OVERWORLD;
    }
}
