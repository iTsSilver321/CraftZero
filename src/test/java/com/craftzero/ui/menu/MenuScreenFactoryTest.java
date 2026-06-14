package com.craftzero.ui.menu;

import com.craftzero.ui.menu.MenuScreenFactory.Content;
import com.craftzero.ui.menu.MenuScreenFactory.CreateWorldRequest;
import com.craftzero.ui.menu.MenuScreenFactory.GameMode;
import com.craftzero.ui.menu.MenuScreenFactory.WorldEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MenuScreenFactoryTest {

    @Test
    @DisplayName("Title screen Singleplayer should navigate to world select")
    void titleNavigatesToWorldSelect() {
        ScreenManager manager = new ScreenManager();
        MenuScreenFactory factory = new MenuScreenFactory(manager, new MenuScreenFactory.Callbacks() {
        });

        manager.push(factory.title());
        button(manager.current().orElseThrow(), "singleplayer").click();

        assertEquals(MenuScreenIds.WORLD_SELECT, manager.current().orElseThrow().id());
        assertEquals(2, manager.depth());
    }

    @Test
    @DisplayName("World select should enable actions after selection and invoke callbacks")
    void worldSelectSelectsWorld() {
        AtomicReference<WorldEntry> started = new AtomicReference<>();
        Content content = new Content().setWorlds(List.of(
                new WorldEntry("w1", "World 1", "Today", "Release 1.0", false)));
        MenuScreenFactory factory = new MenuScreenFactory(new ScreenManager(), new MenuScreenFactory.Callbacks() {
            @Override
            public void startWorld(WorldEntry world) {
                started.set(world);
            }
        }, content);
        Screen screen = factory.worldSelect();

        MenuList<?> worlds = list(screen, "worlds");
        MenuButton play = button(screen, "play");
        assertFalse(play.isEnabled());

        assertTrue(worlds.mousePressed(40, 56, MouseButton.LEFT));
        assertTrue(play.isEnabled());
        play.click();

        assertEquals("w1", started.get().id());
    }

    @Test
    @DisplayName("Create-world screen should collect text and selected game mode")
    void createWorldCollectsInput() {
        AtomicReference<CreateWorldRequest> request = new AtomicReference<>();
        MenuScreenFactory factory = new MenuScreenFactory(new ScreenManager(), new MenuScreenFactory.Callbacks() {
            @Override
            public void createWorld(CreateWorldRequest createRequest) {
                request.set(createRequest);
            }
        });
        Screen screen = factory.createWorld();

        TextField name = textField(screen, "world-name");
        TextField seed = textField(screen, "seed");
        MenuButton gameMode = button(screen, "game-mode");

        name.setText("Ocean Base");
        seed.setText("12345");
        gameMode.click();
        button(screen, "create").click();

        assertEquals("Ocean Base", request.get().name());
        assertEquals("12345", request.get().seed());
        assertEquals(GameMode.CREATIVE, request.get().gameMode());
    }

    @Test
    @DisplayName("Pause screen ESC should resume and pop itself")
    void pauseEscapeResumesGame() {
        AtomicInteger resumeCount = new AtomicInteger();
        ScreenManager manager = new ScreenManager();
        MenuScreenFactory factory = new MenuScreenFactory(manager, new MenuScreenFactory.Callbacks() {
            @Override
            public void resumeGame() {
                resumeCount.incrementAndGet();
            }
        });

        manager.push(factory.pause());
        assertTrue(manager.keyPressed(MenuKeys.ESCAPE));

        assertEquals(1, resumeCount.get());
        assertEquals(0, manager.depth());
    }

    private static MenuButton button(Screen screen, String id) {
        return screen.components().stream()
                .filter(component -> component.id().equals(id))
                .filter(MenuButton.class::isInstance)
                .map(MenuButton.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static MenuList<?> list(Screen screen, String id) {
        return screen.components().stream()
                .filter(component -> component.id().equals(id))
                .filter(MenuList.class::isInstance)
                .map(MenuList.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static TextField textField(Screen screen, String id) {
        return screen.components().stream()
                .filter(component -> component.id().equals(id))
                .filter(TextField.class::isInstance)
                .map(TextField.class::cast)
                .findFirst()
                .orElseThrow();
    }
}
