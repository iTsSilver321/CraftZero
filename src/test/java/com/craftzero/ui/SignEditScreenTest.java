package com.craftzero.ui;

import com.craftzero.engine.Input;
import com.craftzero.world.tile.SignTileEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;

class SignEditScreenTest {

    @AfterEach
    void clearInputState() throws Exception {
        typedCharacters().clear();
        boolean[] keysPressed = keysPressed();
        for (int i = 0; i < keysPressed.length; i++) {
            keysPressed[i] = false;
        }
    }

    @Test
    @DisplayName("Sign editor should accept font-backed extended characters and reject controls/formatting")
    void signEditorFiltersTypedCharactersLikeReleaseSigns() throws Exception {
        SignTileEntity sign = new SignTileEntity(0, 70, 0);
        SignEditScreen screen = boundScreen(sign);
        char extendedFontChar = (char) 0xE9;

        typedCharacters().add('A');
        typedCharacters().add(extendedFontChar);
        typedCharacters().add((char) 0xC0);
        typedCharacters().add((char) 0xA7);
        typedCharacters().add((char) 0x7F);
        typedCharacters().add('\n');

        screen.update();

        assertEquals("A" + extendedFontChar, sign.getLines()[0]);
        assertTrue(SignEditScreen.isAllowedSignCharacter(extendedFontChar));
        assertFalse(SignEditScreen.isAllowedSignCharacter((char) 0xC0));
        assertFalse(SignEditScreen.isAllowedSignCharacter((char) 0xA7));
        assertFalse(SignEditScreen.isAllowedSignCharacter((char) 0x7F));
        assertFalse(SignEditScreen.isAllowedSignCharacter('\n'));
    }

    @Test
    @DisplayName("Sign editor should move lines before applying backspace")
    void signEditorMovesSelectionAndBackspacesSelectedLine() throws Exception {
        SignTileEntity sign = new SignTileEntity(0, 70, 0);
        sign.setLine(0, "One");
        sign.setLine(1, "Two");
        SignEditScreen screen = boundScreen(sign);

        keysPressed()[GLFW_KEY_DOWN] = true;
        screen.update();
        keysPressed()[GLFW_KEY_DOWN] = false;

        keysPressed()[GLFW_KEY_BACKSPACE] = true;
        screen.update();

        assertEquals(1, screen.getSelectedLine());
        assertEquals("One", sign.getLines()[0]);
        assertEquals("Tw", sign.getLines()[1]);
    }

    private static SignEditScreen boundScreen(SignTileEntity sign) throws Exception {
        SignEditScreen screen = new SignEditScreen();
        Field signField = SignEditScreen.class.getDeclaredField("sign");
        signField.setAccessible(true);
        signField.set(screen, sign);
        Field openField = SignEditScreen.class.getDeclaredField("open");
        openField.setAccessible(true);
        openField.set(screen, true);
        return screen;
    }

    @SuppressWarnings("unchecked")
    private static List<Character> typedCharacters() throws Exception {
        Field typedCharacters = Input.class.getDeclaredField("typedCharacters");
        typedCharacters.setAccessible(true);
        return (List<Character>) typedCharacters.get(null);
    }

    private static boolean[] keysPressed() throws Exception {
        Field keysPressed = Input.class.getDeclaredField("keysPressed");
        keysPressed.setAccessible(true);
        return (boolean[]) keysPressed.get(null);
    }
}
