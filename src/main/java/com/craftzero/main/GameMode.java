package com.craftzero.main;

import java.util.Locale;

/**
 * Release 1.0-era game modes exposed by world creation and selection menus.
 */
public enum GameMode {
    SURVIVAL(0, "survival", "Survival", true, false, false),
    CREATIVE(1, "creative", "Creative", false, true, false),
    HARDCORE(2, "hardcore", "Hardcore", true, false, true);

    private final int id;
    private final String optionName;
    private final String displayName;
    private final boolean usesSurvivalRules;
    private final boolean allowsFlight;
    private final boolean deletesOnDeath;

    GameMode(int id, String optionName, String displayName, boolean usesSurvivalRules,
            boolean allowsFlight, boolean deletesOnDeath) {
        this.id = id;
        this.optionName = optionName;
        this.displayName = displayName;
        this.usesSurvivalRules = usesSurvivalRules;
        this.allowsFlight = allowsFlight;
        this.deletesOnDeath = deletesOnDeath;
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

    public boolean usesSurvivalRules() {
        return usesSurvivalRules;
    }

    public boolean allowsFlight() {
        return allowsFlight;
    }

    public boolean deletesOnDeath() {
        return deletesOnDeath;
    }

    public boolean isSurvivalLike() {
        return usesSurvivalRules;
    }

    public boolean isCreative() {
        return this == CREATIVE;
    }

    public static GameMode fromId(int id) {
        for (GameMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        return SURVIVAL;
    }

    public static GameMode fromName(String value) {
        if (value == null || value.isBlank()) {
            return SURVIVAL;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        for (GameMode mode : values()) {
            if (mode.optionName.equals(normalized)
                    || mode.name().toLowerCase(Locale.ROOT).equals(normalized)
                    || mode.displayName.toLowerCase(Locale.ROOT).replace(' ', '_').equals(normalized)) {
                return mode;
            }
        }

        try {
            return fromId(Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {
            return SURVIVAL;
        }
    }
}
