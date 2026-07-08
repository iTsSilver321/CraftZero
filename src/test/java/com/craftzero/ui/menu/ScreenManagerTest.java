package com.craftzero.ui.menu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ScreenManagerTest {

    @Test
    @DisplayName("ESC should pop nested screens but leave a non-closing root")
    void escapePopsNestedScreens() {
        ScreenManager manager = new ScreenManager();
        manager.push(screen("root", false, () -> false));
        manager.push(screen("child", true, () -> false));

        assertTrue(manager.keyPressed(MenuKeys.ESCAPE));
        assertEquals(1, manager.depth());
        assertEquals("root", manager.current().orElseThrow().id());

        assertFalse(manager.keyPressed(MenuKeys.ESCAPE));
        assertEquals(1, manager.depth());
    }

    @Test
    @DisplayName("Screen back handlers can consume ESC without popping")
    void customBackHandlerConsumesEscape() {
        AtomicBoolean handled = new AtomicBoolean();
        ScreenManager manager = new ScreenManager();
        manager.push(screen("modal", false, () -> {
            handled.set(true);
            return true;
        }));

        assertTrue(manager.keyPressed(MenuKeys.ESCAPE));
        assertTrue(handled.get());
        assertEquals(1, manager.depth());
    }

    @Test
    @DisplayName("ScreenManager should route input only to the current screen")
    void routesInputToCurrentScreen() {
        AtomicInteger clicks = new AtomicInteger();
        MenuButton topButton = new MenuButton("top", "Top", new Rect(0, 0, 50, 20), clicks::incrementAndGet);

        ScreenManager manager = new ScreenManager();
        manager.push(new MenuScreen("root", "Root", List.of(new MenuButton("root", "Root", new Rect(0, 0, 50, 20),
                () -> fail("root should not receive top-screen click")))));
        manager.push(new MenuScreen("top", "Top", List.of(topButton)));

        assertTrue(manager.mousePressed(5, 5, MouseButton.LEFT));
        assertTrue(manager.mouseReleased(5, 5, MouseButton.LEFT));
        assertEquals(1, clicks.get());
    }

    @Test
    @DisplayName("ScreenManager should wire button click sounds for pushed screens")
    void wiresButtonClickSoundsForPushedScreens() {
        AtomicInteger clicks = new AtomicInteger();
        AtomicInteger sounds = new AtomicInteger();
        MenuButton button = new MenuButton("done", "Done", new Rect(0, 0, 50, 20), clicks::incrementAndGet);

        ScreenManager manager = new ScreenManager();
        manager.setButtonClickSound(sounds::incrementAndGet);
        manager.push(new MenuScreen("menu", "Menu", List.of(button)));

        assertTrue(manager.mousePressed(5, 5, MouseButton.LEFT));
        assertTrue(manager.mouseReleased(200, 200, MouseButton.LEFT));
        assertEquals(0, clicks.get());
        assertEquals(0, sounds.get());

        assertTrue(manager.mousePressed(5, 5, MouseButton.LEFT));
        assertTrue(manager.mouseReleased(5, 5, MouseButton.LEFT));
        assertEquals(1, clicks.get());
        assertEquals(1, sounds.get());
    }

    private static Screen screen(String id, boolean closeOnBack, java.util.function.BooleanSupplier backHandler) {
        return new MenuScreen(id, id, List.of(), true, closeOnBack, backHandler);
    }
}
