package com.craftzero.progression;

public record PotionData(PotionType type, boolean splash, boolean extended, boolean enhanced) {
    public PotionData {
        if (type == null) {
            type = PotionType.WATER;
        }
    }

    public static PotionData water() {
        return new PotionData(PotionType.WATER, false, false, false);
    }
}
