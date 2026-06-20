package com.craftzero.world;

/**
 * Release 1.0-style fluid metadata helpers.
 *
 * Metadata 0 is a source, 1..7 are horizontal decay levels, and 8..15 are
 * falling fluids whose visible height is the almost-full falling sheet.
 */
public final class FluidState {
    private FluidState() {
    }

    public static int level(int metadata) {
        return metadata & 15;
    }

    public static int flowDecay(int metadata) {
        int level = level(metadata);
        return level >= 8 ? 0 : level;
    }

    public static boolean isSource(int metadata) {
        return level(metadata) == 0;
    }

    public static boolean isFalling(int metadata) {
        return level(metadata) >= 8;
    }

    public static float height(int metadata) {
        int level = level(metadata);
        if (level >= 8) {
            return 8.0f / 9.0f;
        }
        return (8.0f - level) / 9.0f;
    }

    public static boolean isStrongerOrEqual(int existingMetadata, int incomingMetadata) {
        int existing = flowDecay(existingMetadata);
        int incoming = flowDecay(incomingMetadata);
        if (existing == 0) {
            return true;
        }
        return existing <= incoming;
    }
}
