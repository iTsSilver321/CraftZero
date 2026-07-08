package com.craftzero.ui.menu;

import java.util.Objects;

public final class MenuLabel implements MenuComponent {
    private final String id;
    private Rect bounds;
    private String text;
    private float scale;
    private boolean centered;
    private boolean visible = true;
    private float rotationDegrees;
    private float red = 1.0f;
    private float green = 1.0f;
    private float blue = 1.0f;
    private float alpha = 1.0f;

    public MenuLabel(String id, String text, Rect bounds) {
        this.id = Objects.requireNonNull(id, "id");
        this.text = Objects.requireNonNull(text, "text");
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.scale = 1.0f;
    }

    public static MenuLabel centered(String id, String text, int centerX, int y, int width) {
        return new MenuLabel(id, text, new Rect(centerX - width / 2, y, width, 10)).centered(true);
    }

    @Override
    public String id() {
        return id;
    }

    public String text() {
        return text;
    }

    public MenuLabel text(String text) {
        this.text = Objects.requireNonNull(text, "text");
        return this;
    }

    public float scale() {
        return scale;
    }

    public MenuLabel scale(float scale) {
        this.scale = Math.max(0.1f, scale);
        return this;
    }

    public boolean centered() {
        return centered;
    }

    public MenuLabel centered(boolean centered) {
        this.centered = centered;
        return this;
    }

    public float rotationDegrees() {
        return rotationDegrees;
    }

    public MenuLabel rotationDegrees(float rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
        return this;
    }

    public float[] color() {
        return new float[] { red, green, blue, alpha };
    }

    public MenuLabel color(float red, float green, float blue, float alpha) {
        this.red = clamp01(red);
        this.green = clamp01(green);
        this.blue = clamp01(blue);
        this.alpha = clamp01(alpha);
        return this;
    }

    public MenuLabel color(float[] rgba) {
        if (rgba == null || rgba.length < 4) {
            return color(1.0f, 1.0f, 1.0f, 1.0f);
        }
        return color(rgba[0], rgba[1], rgba[2], rgba[3]);
    }

    @Override
    public Rect bounds() {
        return bounds;
    }

    public MenuLabel bounds(Rect bounds) {
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        return this;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    public boolean visible() {
        return visible;
    }

    public MenuLabel visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
