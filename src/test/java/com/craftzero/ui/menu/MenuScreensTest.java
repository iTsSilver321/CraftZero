package com.craftzero.ui.menu;

import com.craftzero.inventory.ItemType;
import com.craftzero.main.GameSettings;
import com.craftzero.main.PlayerStatistics;
import com.craftzero.progression.AchievementTracker;
import com.craftzero.progression.AchievementType;
import com.craftzero.world.BlockType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuScreensTest {

    @Test
    @DisplayName("Pause menu should open achievements while statistics stays disabled")
    void pauseMenuEnablesAchievementsWhenHandlerExists() {
        AtomicInteger opened = new AtomicInteger();

        BaseMenuScreen screen = MenuScreens.pause(854, 480, () -> {
        }, opened::incrementAndGet, () -> {
        }, () -> {
        });

        MenuButton achievements = button(screen, "Achievements");
        MenuButton statistics = button(screen, "Statistics");

        assertTrue(achievements.enabled());
        assertFalse(statistics.enabled());

        achievements.click();

        assertEquals(1, opened.get());
    }

    @Test
    @DisplayName("Pause menu should open statistics when real counters are provided")
    void pauseMenuEnablesStatisticsWhenHandlerExists() {
        AtomicInteger opened = new AtomicInteger();

        BaseMenuScreen screen = MenuScreens.pause(854, 480, () -> {
        }, () -> {
        }, opened::incrementAndGet, () -> {
        }, () -> {
        });

        MenuButton statistics = button(screen, "Statistics");

        assertTrue(statistics.enabled());

        statistics.click();

        assertEquals(1, opened.get());
    }

    @Test
    @DisplayName("Achievements screen should render the Release-style tree state")
    void achievementsScreenListsProgressionState() {
        AchievementTracker tracker = new AchievementTracker();
        tracker.unlock(AchievementType.OPEN_INVENTORY);
        AtomicInteger closed = new AtomicInteger();

        BaseMenuScreen screen = MenuScreens.achievements(854, 480, tracker, closed::incrementAndGet);

        assertEquals("Unlocked: 1 / " + AchievementType.values().length,
                label(screen, "achievement-summary").text());
        AchievementTreeComponent tree = component(screen, "achievement-tree", AchievementTreeComponent.class);
        assertEquals(AchievementTreeComponent.NodeState.UNLOCKED, tree.nodeState(AchievementType.OPEN_INVENTORY));
        assertEquals(AchievementTreeComponent.NodeState.AVAILABLE, tree.nodeState(AchievementType.MINE_WOOD));
        assertEquals(AchievementTreeComponent.NodeState.LOCKED, tree.nodeState(AchievementType.BUILD_WORKBENCH));
        assertEquals(ItemType.BOOK, tree.iconFor(AchievementType.OPEN_INVENTORY));
        assertTrue(tree.contentHeight() > tree.bounds().height());
        assertEquals("Next: Getting Wood", label(screen, "achievement-selected-title").text());
        assertEquals("Attack a tree until a block of wood pops out",
                label(screen, "achievement-selected-description").text());

        Rect workbench = tree.nodeScreenRect(AchievementType.BUILD_WORKBENCH);
        screen.update(new MenuInput(854, 480, workbench.centerX(), workbench.centerY(),
                false, 0, List.of(), List.of()));
        assertEquals(AchievementType.BUILD_WORKBENCH, tree.hoveredAchievement());
        assertEquals("Locked: Benchmarking", label(screen, "achievement-selected-title").text());
        assertEquals("Requires 'Getting Wood'", label(screen, "achievement-selected-description").text());

        int beforeScroll = tree.scrollY();
        screen.update(new MenuInput(854, 480, workbench.centerX(), workbench.centerY(),
                false, -1.0, List.of(), List.of()));
        assertTrue(tree.scrollY() > beforeScroll);

        button(screen, "Done").click();

        assertEquals(1, closed.get());
    }

    @Test
    @DisplayName("Achievements tree should support drag panning on compact screens")
    void achievementsTreeSupportsDragPanning() {
        BaseMenuScreen screen = MenuScreens.achievements(320, 240, new AchievementTracker(), () -> {
        });
        AchievementTreeComponent tree = component(screen, "achievement-tree", AchievementTreeComponent.class);
        int beforeX = tree.scrollX();
        int beforeY = tree.scrollY();
        int x = tree.bounds().centerX();
        int y = tree.bounds().centerY();

        screen.update(new MenuInput(320, 240, x, y, true, 0, List.of(), List.of()));
        screen.update(new MenuInput(320, 240, x - 48, y - 32, true, 0, List.of(), List.of()));
        screen.update(new MenuInput(320, 240, x - 48, y - 32, false, 0, List.of(), List.of()));

        assertTrue(tree.scrollX() > beforeX);
        assertTrue(tree.scrollY() > beforeY);
    }

    @Test
    @DisplayName("Achievement metadata should keep source display coordinates and special flags")
    void achievementTypesExposeSourceLayoutMetadata() {
        assertEquals(0, AchievementType.OPEN_INVENTORY.displayColumn());
        assertEquals(0, AchievementType.OPEN_INVENTORY.displayRow());
        assertEquals(4, AchievementType.BUILD_WORKBENCH.displayColumn());
        assertEquals(-1, AchievementType.BUILD_WORKBENCH.displayRow());
        assertEquals(-4, AchievementType.RETURN_TO_SENDER.displayColumn());
        assertEquals(8, AchievementType.RETURN_TO_SENDER.displayRow());
        assertTrue(AchievementType.ON_A_RAIL.special());
        assertFalse(AchievementType.DIAMONDS.special());
        assertEquals(ItemType.BOOK, AchievementType.OPEN_INVENTORY.icon());
        assertEquals(ItemType.GHAST_TEAR, AchievementType.RETURN_TO_SENDER.icon());
        assertEquals(ItemType.DRAGON_EGG, AchievementType.THE_END2.icon());
    }

    @Test
    @DisplayName("Statistics screen should list persisted player counters")
    void statisticsScreenListsPlayerCounters() {
        PlayerStatistics statistics = new PlayerStatistics();
        statistics.restore(2460, 1234, 7, 8, 3, 95, 42, 1, 6, 4, 11, 5, 9, 2);
        statistics.restoreTravelDistances(250, 900, 75, 1200, 160, 3200, 640, 480);
        statistics.restoreFishCaught(3);
        statistics.restorePlayerKills(2);
        statistics.restoreItemsDropped(2);
        statistics.restoreGamesQuit(4);
        AtomicInteger closed = new AtomicInteger();

        BaseMenuScreen screen = MenuScreens.statistics(854, 480, statistics, closed::incrementAndGet);

        assertEquals("General", label(screen, "statistics-summary").text());
        assertEquals("Games quit: 4", label(screen, "statistics-gamesQuit").text());
        assertEquals("Minutes Played: 2 m 3 s", label(screen, "statistics-playTime").text());
        assertEquals("Distance Walked: 12.34 blocks", label(screen, "statistics-distance").text());
        assertEquals("Distance Fallen: 9 blocks", label(screen, "statistics-distanceFallen").text());
        assertEquals("Distance Swum: 2.50 blocks", label(screen, "statistics-distanceSwum").text());
        assertEquals("Distance Climbed: 0.75 blocks", label(screen, "statistics-distanceClimbed").text());
        assertEquals("Distance Flown: 12 blocks", label(screen, "statistics-distanceFlown").text());
        assertEquals("Distance Dove: 1.60 blocks", label(screen, "statistics-distanceDove").text());
        assertEquals("Distance by Minecart: 32 blocks", label(screen, "statistics-distanceMinecart").text());
        assertEquals("Distance by Boat: 6.40 blocks", label(screen, "statistics-distanceBoat").text());
        assertEquals("Distance by Pig: 4.80 blocks", label(screen, "statistics-distancePig").text());
        assertEquals("Items Dropped: 2", label(screen, "statistics-itemsDropped").text());
        assertEquals("Damage Dealt: 9.5", label(screen, "statistics-damageDealt").text());
        assertEquals("Damage Taken: 4.2", label(screen, "statistics-damageTaken").text());
        assertEquals("Number of Deaths: 1", label(screen, "statistics-deaths").text());
        assertEquals("Mob Kills: 6", label(screen, "statistics-mobKills").text());
        assertEquals("Player Kills: 2", label(screen, "statistics-playerKills").text());
        assertEquals("Fish Caught: 3", label(screen, "statistics-fishCaught").text());

        button(screen, "Done").click();

        assertEquals(1, closed.get());
    }

    @Test
    @DisplayName("Statistics screen should switch between general, block, and item pages")
    void statisticsScreenSwitchesBetweenTypedPages() {
        PlayerStatistics statistics = new PlayerStatistics();
        statistics.recordBlockMined(BlockType.STONE);
        statistics.recordBlockMined(BlockType.STONE);
        statistics.recordItemPickup(ItemType.OAK_LOG, 3);
        statistics.recordItemDropped(ItemType.DIAMOND, 2);
        statistics.recordItemCrafted(ItemType.CRAFTING_TABLE, 1);
        statistics.recordItemUsed(ItemType.FISHING_ROD);
        statistics.recordItemDepleted(ItemType.FISHING_ROD);

        BaseMenuScreen screen = MenuScreens.statistics(854, 480, statistics, () -> {
        });

        MenuLabel playTime = label(screen, "statistics-playTime");
        MenuList<String> blockList = list(screen, "statistics-block-list");
        MenuList<String> itemList = list(screen, "statistics-item-list");

        assertEquals("General", label(screen, "statistics-summary").text());
        assertTrue(playTime.visible());
        assertFalse(blockList.visible());
        assertFalse(itemList.visible());

        button(screen, "statistics-tab-blocks").click();

        assertEquals("Blocks", label(screen, "statistics-summary").text());
        assertFalse(playTime.visible());
        assertTrue(blockList.visible());
        assertFalse(itemList.visible());
        assertTrue(blockList.items().contains("Stone: 0 crafted, 0 used, 2 mined"));
        assertTrue(blockList.items().contains("Crafting Table: 1 crafted, 0 used, 0 mined"));

        button(screen, "statistics-tab-items").click();

        assertEquals("Items", label(screen, "statistics-summary").text());
        assertFalse(blockList.visible());
        assertTrue(itemList.visible());
        assertFalse(itemList.items().contains("Oak Log: 0 crafted, 0 used, 0 depleted"));
        assertFalse(itemList.items().contains("Diamond: 0 crafted, 0 used, 0 depleted"));
        assertTrue(itemList.items().contains("Fishing Rod: 0 crafted, 1 used, 1 depleted"));
    }

    @Test
    @DisplayName("End credits screen should present completion text and continue to gameplay")
    void endCreditsScreenPresentsCompletionFlow() {
        AtomicInteger continued = new AtomicInteger();

        BaseMenuScreen screen = MenuScreens.endCredits(854, 480, continued::incrementAndGet);

        assertEquals("You wake from the End.", label(screen, "end-credits-line-1").text());
        assertEquals("The dragon is gone, and the portal has carried you home.",
                label(screen, "end-credits-line-2").text());
        assertEquals("Your world waits at the spawn point.", label(screen, "end-credits-line-3").text());

        button(screen, "Continue").click();

        assertEquals(1, continued.get());
    }

    @Test
    @DisplayName("Options screen should expose Release-style settings and language route")
    void optionsScreenUsesReleaseStyleLabelsAndRoutes() {
        GameSettings settings = GameSettings.defaults();
        settings.setMusicVolume(0.0f);
        settings.setMouseSensitivity(1.0f);
        AtomicInteger openedLanguage = new AtomicInteger();

        BaseMenuScreen screen = MenuScreens.options(854, 480, settings, () -> {
        }, () -> {
        }, openedLanguage::incrementAndGet, () -> {
        }, () -> {
        });

        assertEquals("Music: OFF", component(screen, "Music", MenuSlider.class).displayText());
        assertEquals("Sensitivity: HYPERSPEED!!!", component(screen, "Sensitivity", MenuSlider.class).displayText());
        button(screen, "Language...").click();

        assertEquals(1, openedLanguage.get());
    }

    @Test
    @DisplayName("Video settings should expose Release-style render distance presets")
    void videoSettingsCyclesRenderDistancePresets() {
        GameSettings settings = GameSettings.defaults();
        settings.setRenderDistance(8);
        AtomicInteger changed = new AtomicInteger();

        BaseMenuScreen screen = MenuScreens.video(854, 480, settings, () -> {
        }, changed::incrementAndGet);

        MenuButton renderDistance = button(screen, "render-distance");

        assertEquals("Render Distance: Normal", renderDistance.label());

        renderDistance.click();

        assertEquals(4, settings.getRenderDistance());
        assertEquals("Render Distance: Short", renderDistance.label());
        assertEquals(1, changed.get());
    }

    @Test
    @DisplayName("Video settings should expose Release-era toggles and labels")
    void videoSettingsExposeReleaseEraTogglesAndLabels() {
        GameSettings settings = GameSettings.defaults();
        settings.setGamma(0.0f);

        BaseMenuScreen screen = MenuScreens.video(854, 480, settings, () -> {
        });

        assertEquals("3D Anaglyph: OFF", button(screen, "3D Anaglyph: OFF").label());
        assertEquals("Particles: All", button(screen, "Particles: All").label());
        assertEquals("GUI Scale: Auto", button(screen, "GUI Scale: Auto").label());
        MenuSlider brightness = component(screen, "Brightness", MenuSlider.class);
        assertEquals("Brightness: Moody", brightness.displayText());
        brightness.setValue(0.5);
        assertEquals("Brightness: +50%", brightness.displayText());

        button(screen, "3D Anaglyph: OFF").click();
        button(screen, "Particles: All").click();
        button(screen, "GUI Scale: Auto").click();

        assertTrue(settings.isAnaglyph3d());
        assertEquals(1, settings.getParticles());
        assertEquals(1, settings.getGuiScale());

        assertFalse(screen.components().stream().anyMatch(component -> component.id().startsWith("Fullscreen")));
        assertFalse(screen.components().stream().anyMatch(component -> component.id().startsWith("Use VSync")));
    }

    private static MenuButton button(BaseMenuScreen screen, String id) {
        MenuComponent component = screen.components().stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElse(null);
        assertNotNull(component);
        return assertInstanceOf(MenuButton.class, component);
    }

    private static MenuLabel label(BaseMenuScreen screen, String id) {
        MenuComponent component = screen.components().stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElse(null);
        assertNotNull(component);
        return assertInstanceOf(MenuLabel.class, component);
    }

    private static <T extends MenuComponent> T component(BaseMenuScreen screen, String id, Class<T> type) {
        MenuComponent component = screen.components().stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElse(null);
        assertNotNull(component);
        return assertInstanceOf(type, component);
    }

    @SuppressWarnings("unchecked")
    private static MenuList<String> list(BaseMenuScreen screen, String id) {
        MenuComponent component = screen.components().stream()
                .filter(candidate -> candidate.id().equals(id))
                .findFirst()
                .orElse(null);
        assertNotNull(component);
        return (MenuList<String>) assertInstanceOf(MenuList.class, component);
    }
}
