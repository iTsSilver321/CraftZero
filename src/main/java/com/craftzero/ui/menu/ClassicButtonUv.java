package com.craftzero.ui.menu;

public enum ClassicButtonUv {
    NORMAL(0, 66, 200, 20),
    HOVERED(0, 86, 200, 20),
    DISABLED(0, 46, 200, 20);

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    ClassicButtonUv(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public float[] uv() {
        return new float[] {
                x / 256.0f,
                y / 256.0f,
                (x + width) / 256.0f,
                (y + height) / 256.0f
        };
    }
}
