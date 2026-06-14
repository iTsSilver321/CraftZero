package com.craftzero.ui.menu;

public final class GuiScale {
    private GuiScale() {
    }

    public static int compute(int configuredScale, int width, int height) {
        if (configuredScale > 0) {
            return Math.max(1, configuredScale);
        }
        int scale = 1;
        while (scale < 4 && width / (scale + 1) >= 320 && height / (scale + 1) >= 240) {
            scale++;
        }
        return scale;
    }
}
