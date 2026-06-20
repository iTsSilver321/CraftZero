package com.craftzero.ui.menu;

import java.util.List;

public record MenuInput(
        int width,
        int height,
        double mouseX,
        double mouseY,
        boolean leftPressed,
        double scrollY,
        List<Integer> pressedKeys,
        List<Integer> downKeys,
        List<Character> typedCharacters) {

    public MenuInput(int width, int height, double mouseX, double mouseY, boolean leftPressed, double scrollY,
            List<Integer> pressedKeys, List<Character> typedCharacters) {
        this(width, height, mouseX, mouseY, leftPressed, scrollY, pressedKeys, List.of(), typedCharacters);
    }

    public boolean keyPressed(int keyCode) {
        return pressedKeys != null && pressedKeys.contains(keyCode);
    }

    public boolean keyDown(int keyCode) {
        return downKeys != null && downKeys.contains(keyCode);
    }
}
