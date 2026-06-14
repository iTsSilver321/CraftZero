package com.craftzero.ui.menu;

import java.util.ArrayList;
import java.util.List;

/**
 * Release-style logical GUI layout and scaling helper.
 */
public final class GuiLayout {

    public static final int BASE_WIDTH = 320;
    public static final int BASE_HEIGHT = 240;
    public static final int DEFAULT_BUTTON_WIDTH = 200;
    public static final int DEFAULT_BUTTON_HEIGHT = 20;
    public static final int DEFAULT_ROW_SPACING = 24;

    private final int screenWidth;
    private final int screenHeight;
    private final int scale;
    private final int scaledWidth;
    private final int scaledHeight;

    private GuiLayout(int screenWidth, int screenHeight, int scale) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            throw new IllegalArgumentException("screen dimensions must be > 0");
        }
        if (scale <= 0) {
            throw new IllegalArgumentException("scale must be > 0");
        }
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.scale = scale;
        this.scaledWidth = Math.max(1, (int) Math.ceil(screenWidth / (double) scale));
        this.scaledHeight = Math.max(1, (int) Math.ceil(screenHeight / (double) scale));
    }

    public static GuiLayout forScreen(int screenWidth, int screenHeight, int requestedScale) {
        return new GuiLayout(screenWidth, screenHeight, calculateScale(screenWidth, screenHeight, requestedScale));
    }

    public static GuiLayout logical(int width, int height) {
        return new GuiLayout(width, height, 1);
    }

    /**
     * Matches the classic "Auto" behavior: pick the largest scale that leaves at
     * least a 320x240 logical canvas, clamped to 4.
     */
    public static int calculateScale(int screenWidth, int screenHeight, int requestedScale) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            throw new IllegalArgumentException("screen dimensions must be > 0");
        }
        if (requestedScale > 0) {
            return Math.max(1, Math.min(4, requestedScale));
        }

        int scale = 1;
        while (scale < 4
                && screenWidth / (scale + 1) >= BASE_WIDTH
                && screenHeight / (scale + 1) >= BASE_HEIGHT) {
            scale++;
        }
        return scale;
    }

    public Rect centered(int width, int height) {
        return new Rect((scaledWidth - width) / 2, (scaledHeight - height) / 2, width, height);
    }

    public Rect centeredAtY(int width, int height, int y) {
        return new Rect((scaledWidth - width) / 2, y, width, height);
    }

    public Rect centeredButton(int y) {
        return centeredAtY(DEFAULT_BUTTON_WIDTH, DEFAULT_BUTTON_HEIGHT, y);
    }

    public List<Rect> centeredVerticalStack(int width, int height, int count, int centerY, int spacing) {
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0");
        }
        List<Rect> rects = new ArrayList<>(count);
        int totalHeight = count == 0 ? 0 : count * height + (count - 1) * spacing;
        int y = centerY - totalHeight / 2;
        for (int i = 0; i < count; i++) {
            rects.add(centeredAtY(width, height, y + i * (height + spacing)));
        }
        return List.copyOf(rects);
    }

    public int toLogicalX(int physicalX) {
        return physicalX / scale;
    }

    public int toLogicalY(int physicalY) {
        return physicalY / scale;
    }

    public int toPhysicalX(int logicalX) {
        return logicalX * scale;
    }

    public int toPhysicalY(int logicalY) {
        return logicalY * scale;
    }

    public Rect toPhysical(Rect logicalRect) {
        return logicalRect.scaled(scale);
    }

    public int screenWidth() {
        return screenWidth;
    }

    public int screenHeight() {
        return screenHeight;
    }

    public int scale() {
        return scale;
    }

    public int scaledWidth() {
        return scaledWidth;
    }

    public int scaledHeight() {
        return scaledHeight;
    }
}
