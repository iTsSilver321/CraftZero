package com.craftzero.ui.menu;

/**
 * Pixel and UV helpers for the Release-era {@code textures/gui/gui.png} atlas.
 */
public final class ClassicGuiTexture {

    public static final int ATLAS_WIDTH = 256;
    public static final int ATLAS_HEIGHT = 256;
    public static final int BUTTON_WIDTH = 200;
    public static final int BUTTON_HEIGHT = 20;
    public static final int BUTTON_X = 0;
    public static final int BUTTON_DISABLED_Y = 46;
    public static final int BUTTON_NORMAL_Y = 66;
    public static final int BUTTON_HOVERED_Y = 86;

    private ClassicGuiTexture() {
    }

    public enum ButtonState {
        DISABLED,
        NORMAL,
        HOVERED
    }

    public enum ButtonHalf {
        LEFT,
        RIGHT
    }

    public static UvRegion button(ButtonState state) {
        return UvRegion.fromPixels(BUTTON_X, buttonY(state), BUTTON_WIDTH, BUTTON_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    /**
     * Vanilla stretches buttons by drawing two halves. This helper exposes those
     * regions without baking in renderer details.
     */
    public static UvRegion buttonHalf(ButtonState state, ButtonHalf half) {
        int halfWidth = BUTTON_WIDTH / 2;
        int x = half == ButtonHalf.LEFT ? BUTTON_X : BUTTON_X + halfWidth;
        return UvRegion.fromPixels(x, buttonY(state), halfWidth, BUTTON_HEIGHT, ATLAS_WIDTH, ATLAS_HEIGHT);
    }

    public static int buttonY(ButtonState state) {
        return switch (state) {
            case DISABLED -> BUTTON_DISABLED_Y;
            case NORMAL -> BUTTON_NORMAL_Y;
            case HOVERED -> BUTTON_HOVERED_Y;
        };
    }
}
