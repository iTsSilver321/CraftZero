package com.craftzero.ui;

import com.craftzero.ui.menu.MenuInput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_TAB;

class ChatOverlayTest {

    @Test
    @DisplayName("Tab should autocomplete and cycle chat command suggestions")
    void tabCompletesSuggestions() {
        ChatOverlay overlay = new ChatOverlay();
        overlay.open(true);

        overlay.update(input(List.of(), List.of('g')), value -> List.of());
        overlay.update(input(List.of(GLFW_KEY_TAB), List.of()), value -> List.of("/gamemode", "/give"));
        assertEquals("/gamemode", overlay.inputText());

        overlay.update(input(List.of(GLFW_KEY_TAB), List.of()), value -> List.of("/gamemode", "/give"));
        assertEquals("/give", overlay.inputText());
    }

    private static MenuInput input(List<Integer> keys, List<Character> chars) {
        return new MenuInput(320, 240, 0, 0, false, 0.0, keys, chars);
    }
}
