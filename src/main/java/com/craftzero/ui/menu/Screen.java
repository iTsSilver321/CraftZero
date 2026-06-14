package com.craftzero.ui.menu;

import java.util.List;

public interface Screen {

    default String id() {
        return getClass().getSimpleName();
    }

    default String title() {
        return "";
    }

    default List<MenuComponent> components() {
        return List.of();
    }

    default boolean pausesGame() {
        return true;
    }

    default boolean shouldCloseOnBack() {
        return true;
    }

    default boolean consumesInput() {
        return true;
    }

    default void onOpened() {
    }

    default void onClosed() {
    }

    default boolean handleBack() {
        return false;
    }

    default void update(MenuInput input) {
    }

    default void render(MenuRenderer renderer, MenuInput input, float deltaTime) {
    }

    default boolean mouseMoved(int x, int y) {
        boolean handled = false;
        for (MenuComponent component : components()) {
            handled |= component.mouseMoved(x, y);
        }
        return handled;
    }

    default boolean mousePressed(int x, int y, MouseButton button) {
        List<MenuComponent> all = components();
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).mousePressed(x, y, button)) {
                return true;
            }
        }
        return false;
    }

    default boolean mouseReleased(int x, int y, MouseButton button) {
        List<MenuComponent> all = components();
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).mouseReleased(x, y, button)) {
                return true;
            }
        }
        return false;
    }

    default boolean keyPressed(int keyCode) {
        for (MenuComponent component : components()) {
            if (component.keyPressed(keyCode)) {
                return true;
            }
        }
        return false;
    }

    default boolean charTyped(char character) {
        for (MenuComponent component : components()) {
            if (component.charTyped(character)) {
                return true;
            }
        }
        return false;
    }
}
