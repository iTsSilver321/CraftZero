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
        List<Character> typedCharacters) {

    public boolean keyPressed(int keyCode) {
        return pressedKeys != null && pressedKeys.contains(keyCode);
    }
}
