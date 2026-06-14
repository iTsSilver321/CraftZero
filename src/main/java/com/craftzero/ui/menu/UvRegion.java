package com.craftzero.ui.menu;

/**
 * Normalized texture coordinates, top-left to bottom-right.
 */
public record UvRegion(float u1, float v1, float u2, float v2) {

    public static UvRegion fromPixels(int x, int y, int width, int height, int atlasWidth, int atlasHeight) {
        if (atlasWidth <= 0 || atlasHeight <= 0) {
            throw new IllegalArgumentException("atlas dimensions must be > 0");
        }
        return new UvRegion(
                x / (float) atlasWidth,
                y / (float) atlasHeight,
                (x + width) / (float) atlasWidth,
                (y + height) / (float) atlasHeight);
    }
}
