package com.craftzero.ui.menu;

/**
 * Integer rectangle in logical GUI pixels.
 */
public record Rect(int x, int y, int width, int height) {

    public Rect {
        if (width < 0) {
            throw new IllegalArgumentException("width must be >= 0");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height must be >= 0");
        }
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }

    public boolean contains(int pointX, int pointY) {
        return pointX >= x && pointX < right() && pointY >= y && pointY < bottom();
    }

    public Rect movedTo(int newX, int newY) {
        return new Rect(newX, newY, width, height);
    }

    public Rect offset(int deltaX, int deltaY) {
        return new Rect(x + deltaX, y + deltaY, width, height);
    }

    public Rect inset(int amount) {
        return new Rect(x + amount, y + amount, Math.max(0, width - amount * 2), Math.max(0, height - amount * 2));
    }

    public Rect scaled(int scale) {
        if (scale <= 0) {
            throw new IllegalArgumentException("scale must be > 0");
        }
        return new Rect(x * scale, y * scale, width * scale, height * scale);
    }
}
