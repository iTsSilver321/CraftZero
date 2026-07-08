package com.craftzero.ui.menu;

import com.craftzero.main.PlayerStatistics;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.AchievementTracker;
import com.craftzero.progression.AchievementType;
import com.craftzero.ui.menu.MenuScreenFactory.Content;
import com.craftzero.ui.menu.MenuScreenFactory.CreateWorldRequest;
import com.craftzero.ui.menu.MenuScreenFactory.GameMode;
import com.craftzero.ui.menu.MenuScreenFactory.WorldEntry;
import com.craftzero.world.BlockType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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
    @DisplayName("Pause screen should expose Release-era achievement and statistics routes")
    void pauseMenuExposesProgressionRoutes() {
        ScreenManager manager = new ScreenManager();
        MenuScreenFactory factory = new MenuScreenFactory(manager, new MenuScreenFactory.Callbacks() {
        });

        manager.push(factory.pause());
        assertEquals("Achievements", button(manager.current().orElseThrow(), "achievements").label());
        assertEquals("Statistics", button(manager.current().orElseThrow(), "statistics").label());

        button(manager.current().orElseThrow(), "achievements").click();
        assertEquals(MenuScreenIds.ACHIEVEMENTS, manager.current().orElseThrow().id());
        button(manager.current().orElseThrow(), "done").click();

        assertEquals(MenuScreenIds.PAUSE, manager.current().orElseThrow().id());
        button(manager.current().orElseThrow(), "statistics").click();
        assertEquals(MenuScreenIds.STATISTICS, manager.current().orElseThrow().id());
    }

    @Test
    @DisplayName("Factory achievement and statistics screens should use live progress")
    void achievementAndStatisticsFactoryScreensUseLiveProgress() {
        AchievementTracker tracker = new AchievementTracker();
        tracker.unlock(AchievementType.OPEN_INVENTORY);
        PlayerStatistics statistics = new PlayerStatistics();
        statistics.restore(2460, 1234, 7, 8, 3, 95, 42, 1, 6, 4, 11, 5, 9, 2);
        statistics.restoreFishCaught(3);
        statistics.restorePlayerKills(2);
        statistics.restoreItemsDropped(2);
        statistics.restoreGamesQuit(4);
        statistics.recordBlockMined(BlockType.STONE);
        statistics.recordItemUsed(ItemType.FISHING_ROD);
        statistics.recordItemDepleted(ItemType.FISHING_ROD);
        statistics.restoreItemsDroppedByType(Map.of(ItemType.DIAMOND, 2L));
        Content content = new Content()
                .setAchievementTracker(tracker)
                .setStatistics(statistics);
        MenuScreenFactory factory = new MenuScreenFactory(new ScreenManager(), new MenuScreenFactory.Callbacks() {
        }, content);

        Screen achievements = factory.achievements();
        assertEquals("Unlocked: 1 / " + AchievementType.values().length, label(achievements, "achievement-summary").text());
        assertEquals("Next: Getting Wood", label(achievements, "achievement-selected-title").text());

        Screen stats = factory.statistics();
        MenuList<?> general = list(stats, "statistics-general-list");
        assertTrue(general.items().contains("Games quit: 4"));
        assertTrue(general.items().contains("Minutes Played: 2 m 3 s"));
        assertTrue(general.items().contains("Distance Walked: 12.34 blocks"));
        assertTrue(general.items().contains("Mob Kills: 6"));
        assertTrue(general.items().contains("Player Kills: 2"));
        assertTrue(general.items().contains("Fish Caught: 3"));
        assertTrue(general.items().contains("Items Dropped: 2"));
        assertFalse(button(stats, "statistics-tab-general").isEnabled());

        button(stats, "statistics-tab-blocks").click();
        assertEquals("Blocks", label(stats, "statistics-summary").text());
        assertFalse(general.isVisible());
        assertTrue(list(stats, "statistics-block-list").isVisible());
        assertTrue(list(stats, "statistics-block-list").items().contains("Stone: 0 crafted, 0 used, 1 mined"));

        button(stats, "statistics-tab-items").click();
        assertEquals("Items", label(stats, "statistics-summary").text());
        assertTrue(list(stats, "statistics-item-list").items()
                .contains("Fishing Rod: 0 crafted, 1 used, 1 depleted"));
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

    @Test
    @DisplayName("Death screen should expose Release-era score text")
    void deathScreenShowsScore() {
        Content content = new Content().setDeathScore(1234);
        MenuScreenFactory factory = new MenuScreenFactory(new ScreenManager(), new MenuScreenFactory.Callbacks() {
        }, content);

        Screen screen = factory.death();

        MenuLabel score = label(screen, "score");
        assertEquals("Score: 1234", score.text());
        assertTrue(score.centered());
    }

    @Test
    @DisplayName("Death screen should expose Release-era Respawn and Title Menu buttons")
    void deathScreenShowsReleaseButtons() {
        MenuScreenFactory factory = new MenuScreenFactory(new ScreenManager(), new MenuScreenFactory.Callbacks() {
        });

        Screen screen = factory.death();

        assertEquals("Respawn", button(screen, "respawn").label());
        assertEquals("Title Menu", button(screen, "title").label());
    }

    @Test
    @DisplayName("Hardcore death screen should force Delete World instead of respawn")
    void hardcoreDeathScreenForcesDeleteWorld() {
        AtomicInteger deleted = new AtomicInteger();
        Content content = new Content()
                .setDeathScore(9001)
                .setHardcoreDeath(true);
        ScreenManager manager = new ScreenManager();
        MenuScreenFactory factory = new MenuScreenFactory(manager, new MenuScreenFactory.Callbacks() {
            @Override
            public void deleteHardcoreWorld() {
                deleted.incrementAndGet();
            }
        }, content);

        manager.push(factory.death());
        Screen screen = manager.current().orElseThrow();

        assertEquals("Game over!", screen.title());
        assertEquals("Score: 9001", label(screen, "score").text());
        assertEquals("You cannot respawn in hardcore mode!", label(screen, "hardcore-message").text());
        assertEquals("Delete World", button(screen, "delete-world").label());
        assertComponentMissing(screen, "respawn");
        assertComponentMissing(screen, "title");

        button(screen, "delete-world").click();

        assertEquals(1, deleted.get());
        assertEquals(MenuScreenIds.TITLE, manager.current().orElseThrow().id());
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

    private static MenuLabel label(Screen screen, String id) {
        return screen.components().stream()
                .filter(component -> component.id().equals(id))
                .filter(MenuLabel.class::isInstance)
                .map(MenuLabel.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static void assertComponentMissing(Screen screen, String id) {
        assertTrue(screen.components().stream().noneMatch(component -> component.id().equals(id)));
    }
}
