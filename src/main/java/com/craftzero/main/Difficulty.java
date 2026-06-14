package com.craftzero.main;

import java.util.Locale;

/**
 * Difficulty values matching the Release 1.0 options ordering.
 */
public enum Difficulty {
    PEACEFUL(0, "peaceful", "Peaceful"),
    EASY(1, "easy", "Easy"),
    NORMAL(2, "normal", "Normal"),
    HARD(3, "hard", "Hard");

    private final int id;
    private final String optionName;
    private final String displayName;

    Difficulty(int id, String optionName, String displayName) {
        this.id = id;
        this.optionName = optionName;
        this.displayName = displayName;
    }

    public int id() {
        return id;
    }

    public String optionName() {
        return optionName;
    }

    public String displayName() {
        return displayName;
    }

    public float scaleIncomingDamage(float easyDamage) {
        return switch (this) {
            case PEACEFUL -> 0.0f;
            case EASY -> easyDamage;
            case NORMAL -> easyDamage * 1.5f;
            case HARD -> easyDamage * 2.0f;
        };
    }

    public boolean allowsHostileSpawns() {
        return this != PEACEFUL;
    }

    public Difficulty next() {
        Difficulty[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public Difficulty previous() {
        Difficulty[] values = values();
        return values[(ordinal() + values.length - 1) % values.length];
    }

    public static Difficulty fromId(int id) {
        for (Difficulty difficulty : values()) {
            if (difficulty.id == id) {
                return difficulty;
            }
        }
        return NORMAL;
    }

    public static Difficulty fromName(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        for (Difficulty difficulty : values()) {
            if (difficulty.optionName.equals(normalized)
                    || difficulty.name().toLowerCase(Locale.ROOT).equals(normalized)
                    || difficulty.displayName.toLowerCase(Locale.ROOT).replace(' ', '_').equals(normalized)) {
                return difficulty;
            }
        }

        try {
            return fromId(Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {
            return NORMAL;
        }
    }
}
