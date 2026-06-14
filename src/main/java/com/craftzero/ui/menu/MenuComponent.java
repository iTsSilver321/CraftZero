package com.craftzero.ui.menu;

public interface MenuComponent {

    String id();

    Rect bounds();

    boolean isVisible();

    boolean isEnabled();

    default boolean hitTest(int x, int y) {
        return isVisible() && bounds().contains(x, y);
    }

    default boolean mouseMoved(int x, int y) {
        return false;
    }

    default boolean mousePressed(int x, int y, MouseButton button) {
        return false;
    }

    default boolean mouseReleased(int x, int y, MouseButton button) {
        return false;
    }

    default boolean keyPressed(int keyCode) {
        return false;
    }

    default boolean charTyped(char character) {
        return false;
    }
}
