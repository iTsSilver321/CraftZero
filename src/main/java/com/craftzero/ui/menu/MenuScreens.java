package com.craftzero.ui.menu;

import com.craftzero.main.Difficulty;
import com.craftzero.main.GameMode;
import com.craftzero.main.GameSettings;
import com.craftzero.main.PlayerStatistics;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.AchievementTracker;
import com.craftzero.progression.AchievementType;
import com.craftzero.resources.ResourcePackManager;
import com.craftzero.save.WorldManager.WorldInfo;
import com.craftzero.world.BlockType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MenuScreens {
    private static final int CLASSIC_FULL_BUTTON_WIDTH = 200;
    private static final int CLASSIC_OPTION_WIDTH = 150;
    private static final int CLASSIC_OPTION_GAP = 10;
    private static final int CLASSIC_BUTTON_HEIGHT = 20;
    private static final int CLASSIC_ROW_STEP = 24;
    static final int STATISTICS_SUMMARY_Y = 52;
    static final int STATISTICS_TAB_Y = 68;
    static final int STATISTICS_LIST_Y = 94;
    static final int STATISTICS_ROW_HEIGHT = 12;
    private static final int STATISTICS_LIST_BOTTOM_PADDING = 50;
    private static final int STATISTICS_MIN_TABLE_WIDTH = 236;
    private static final int STATISTICS_MAX_TABLE_WIDTH = 360;
    private static final int STATISTICS_MIN_ROWS = 6;
    private static final int STATISTICS_MAX_ROWS = 15;
    private static final String[] TITLE_SPLASHES = {
            "Now with blocks!",
            "Old-school!",
            "Java Edition-ish!",
            "Dig, build, survive!",
            "Watch your step!",
            "Punching trees!",
            "So blocky!"
    };

    private MenuScreens() {
    }

    public static BaseMenuScreen title(int width, int height, Runnable singleplayer, Runnable multiplayer,
            Runnable texturePacks, Runnable options, Runnable quit) {
        int cx = width / 2;
        int y = height / 4 + 64;
        BaseMenuScreen screen = new BaseMenuScreen("CraftZero", false, true, null);
        int splashWidth = Math.min(150, Math.max(96, width / 2 - 28));
        int splashCenterX = Math.min(width - splashWidth / 2 - 4, cx + 92);
        int splashY = Math.max(84, y - 48);
        MenuLabel splash = MenuLabel.centered("title-splash", randomTitleSplash(), splashCenterX, splashY, splashWidth)
                .scale(1.1f)
                .rotationDegrees(-20.0f)
                .color(1.0f, 1.0f, 0.0f, 1.0f);
        long splashCreatedAt = System.nanoTime();
        screen.add(splash);
        screen.onTick(() -> {
            float seconds = (System.nanoTime() - splashCreatedAt) / 1_000_000_000.0f;
            splash.scale(1.08f + (float) Math.sin(seconds * 5.2f) * 0.075f);
        });
        int footerY = Math.max(4, height - 10);
        if (width >= 300) {
            screen.add(new MenuLabel("title-version", "CraftZero 1.0 parity", new Rect(2, footerY, 140, 10))
                    .color(0.86f, 0.86f, 0.86f, 1.0f));
            screen.add(MenuLabel.centered("title-footer", "Release-style sandbox", width - 86,
                    footerY, 170).color(0.86f, 0.86f, 0.86f, 1.0f));
        } else {
            screen.add(MenuLabel.centered("title-version", "CraftZero 1.0 parity", cx, footerY,
                    Math.max(120, width - 8)).color(0.86f, 0.86f, 0.86f, 1.0f));
        }
        screen.add(new MenuButton("Singleplayer", cx - 100, y, 200, 20, singleplayer));
        screen.add(new MenuButton("Multiplayer", cx - 100, y + 24, 200, 20, multiplayer));
        screen.add(new MenuButton("Texture Packs", cx - 100, y + 48, 200, 20, texturePacks));
        screen.add(new MenuButton("Options...", cx - 100, y + 84, 98, 20, options));
        screen.add(new MenuButton("Quit Game", cx + 2, y + 84, 98, 20, quit));
        return screen;
    }

    private static String randomTitleSplash() {
        int index = Math.floorMod((int) (System.nanoTime() >>> 20), TITLE_SPLASHES.length);
        return TITLE_SPLASHES[index];
    }

    public static BaseMenuScreen pause(int width, int height, Runnable back, Runnable options, Runnable saveQuit) {
        return pause(width, height, back, null, null, options, saveQuit);
    }

    public static BaseMenuScreen pause(int width, int height, Runnable back, Runnable achievements, Runnable options,
            Runnable saveQuit) {
        return pause(width, height, back, achievements, null, options, saveQuit);
    }

    public static BaseMenuScreen pause(int width, int height, Runnable back, Runnable achievements, Runnable statistics,
            Runnable options, Runnable saveQuit) {
        int cx = width / 2;
        int y = height / 4 + 48;
        BaseMenuScreen screen = new BaseMenuScreen("Game menu", false, false, back);
        screen.add(new MenuButton("Back to game", cx - 100, y, 200, 20, back));
        MenuButton achievementsButton = new MenuButton("Achievements", cx - 100, y + 24, 98, 20,
                achievements == null ? () -> {
                } : achievements);
        screen.add(achievementsButton.enabled(achievements != null));
        MenuButton statisticsButton = new MenuButton("Statistics", cx + 2, y + 24, 98, 20,
                statistics == null ? () -> {
                } : statistics);
        screen.add(statisticsButton.enabled(statistics != null));
        screen.add(new MenuButton("Options...", cx - 100, y + 60, 200, 20, options));
        screen.add(new MenuButton("Save and quit to title", cx - 100, y + 96, 200, 20, saveQuit));
        return screen;
    }

    public static BaseMenuScreen achievements(int width, int height, AchievementTracker tracker, Runnable done) {
        int cx = width / 2;
        BaseMenuScreen screen = new BaseMenuScreen("Achievements", true, false, done);
        AchievementType[] achievements = AchievementType.values();
        int unlocked = tracker == null ? 0 : tracker.unlockedAchievements().size();
        screen.add(MenuLabel.centered("achievement-summary",
                "Unlocked: " + unlocked + " / " + achievements.length,
                cx, 54, Math.min(320, Math.max(180, width - 40))).color(1.0f, 1.0f, 0.63f, 1.0f));

        int detailTitleY = Math.max(78, height - 72);
        AchievementTreeComponent tree = new AchievementTreeComponent("achievement-tree",
                new Rect(24, 74, Math.max(120, width - 48), Math.max(64, detailTitleY - 84)), tracker);
        screen.add(tree);

        AchievementType selected = tree.detailAchievement();
        MenuLabel selectedTitle = new MenuLabel("achievement-selected-title",
                tree.titleLine(selected),
                new Rect(24, detailTitleY, Math.max(100, width - 48), 10))
                .color(tree.colorFor(selected));
        MenuLabel selectedDescription = new MenuLabel("achievement-selected-description",
                fitMenuText(tree.detailDescription(selected), Math.max(100, width - 48)),
                new Rect(24, detailTitleY + 14, Math.max(100, width - 48), 10))
                .color(0.76f, 0.76f, 0.76f, 1.0f);
        tree.onDetailChanged(detail -> {
            selectedTitle.text(tree.titleLine(detail)).color(tree.colorFor(detail));
            selectedDescription.text(fitMenuText(tree.detailDescription(detail), Math.max(100, width - 48)));
        });
        screen.add(selectedTitle);
        screen.add(selectedDescription);

        screen.add(new MenuButton("Done", cx - 100, height - 28, 200, 20, done));
        return screen;
    }

    public static BaseMenuScreen statistics(int width, int height, PlayerStatistics statistics, Runnable done) {
        int cx = width / 2;
        BaseMenuScreen screen = new BaseMenuScreen("Statistics", true, false, done);
        PlayerStatistics stats = statistics == null ? new PlayerStatistics() : statistics;
        MenuLabel summary = MenuLabel.centered("statistics-summary", "General", cx, STATISTICS_SUMMARY_Y,
                statisticsSummaryWidth(width)).color(1.0f, 1.0f, 0.63f, 1.0f);
        screen.add(summary);

        MenuButton generalTab = new MenuButton("statistics-tab-general", "General",
                statisticsTabBounds(cx, 0), null);
        MenuButton blocksTab = new MenuButton("statistics-tab-blocks", "Blocks",
                statisticsTabBounds(cx, 1), null);
        MenuButton itemsTab = new MenuButton("statistics-tab-items", "Items",
                statisticsTabBounds(cx, 2), null);
        screen.add(generalTab);
        screen.add(blocksTab);
        screen.add(itemsTab);

        Rect listBounds = statisticsListBounds(width, height);
        int tableWidth = listBounds.width();
        MenuList<String> generalList = new MenuList<>("statistics-general-list",
                listBounds, STATISTICS_ROW_HEIGHT,
                statisticRows(stats, tableWidth), row -> row);
        MenuList<String> blockList = new MenuList<>("statistics-block-list",
                listBounds, STATISTICS_ROW_HEIGHT, blockStatisticRows(stats, tableWidth),
                row -> row);
        MenuList<String> itemList = new MenuList<>("statistics-item-list",
                listBounds, STATISTICS_ROW_HEIGHT, itemStatisticRows(stats, tableWidth),
                row -> row);
        blockList.setVisible(false);
        itemList.setVisible(false);
        screen.add(generalList);
        screen.add(blockList);
        screen.add(itemList);

        Runnable showGeneral = () -> {
            summary.text("General");
            generalList.setVisible(true);
            blockList.setVisible(false);
            itemList.setVisible(false);
            generalTab.setEnabled(false);
            blocksTab.setEnabled(true);
            itemsTab.setEnabled(true);
        };
        Runnable showBlocks = () -> {
            summary.text("Blocks");
            generalList.setVisible(false);
            blockList.setVisible(true);
            itemList.setVisible(false);
            generalTab.setEnabled(true);
            blocksTab.setEnabled(false);
            itemsTab.setEnabled(true);
        };
        Runnable showItems = () -> {
            summary.text("Items");
            generalList.setVisible(false);
            blockList.setVisible(false);
            itemList.setVisible(true);
            generalTab.setEnabled(true);
            blocksTab.setEnabled(true);
            itemsTab.setEnabled(false);
        };
        generalTab.setAction(showGeneral);
        blocksTab.setAction(showBlocks);
        itemsTab.setAction(showItems);
        showGeneral.run();

        screen.add(new MenuButton("Done", cx - 100, height - 28, 200, 20, done));
        return screen;
    }

    static int statisticsSummaryWidth(int width) {
        return Math.min(320, Math.max(180, width - 40));
    }

    static Rect statisticsTabBounds(int centerX, int tabIndex) {
        return new Rect(centerX - 153 + tabIndex * 104, STATISTICS_TAB_Y, 98, CLASSIC_BUTTON_HEIGHT);
    }

    static Rect statisticsListBounds(int width, int height) {
        int tableWidth = Math.min(STATISTICS_MAX_TABLE_WIDTH, Math.max(STATISTICS_MIN_TABLE_WIDTH, width - 64));
        int availableHeight = Math.max(STATISTICS_ROW_HEIGHT * STATISTICS_MIN_ROWS,
                height - STATISTICS_LIST_Y - STATISTICS_LIST_BOTTOM_PADDING);
        int visibleRows = Math.max(STATISTICS_MIN_ROWS,
                Math.min(STATISTICS_MAX_ROWS, availableHeight / STATISTICS_ROW_HEIGHT));
        return new Rect(width / 2 - tableWidth / 2, STATISTICS_LIST_Y,
                tableWidth, visibleRows * STATISTICS_ROW_HEIGHT);
    }

    public static BaseMenuScreen options(int width, int height, GameSettings settings, Runnable video,
            Runnable controls, Runnable done) {
        return options(width, height, settings, video, controls, () -> {
        }, done);
    }

    public static BaseMenuScreen options(int width, int height, GameSettings settings, Runnable video,
            Runnable controls, Runnable done, Runnable changed) {
        return options(width, height, settings, video, controls, () -> {
        }, done, changed);
    }

    public static BaseMenuScreen options(int width, int height, GameSettings settings, Runnable video,
            Runnable controls, Runnable language, Runnable done, Runnable changed) {
        int y = 60;
        int leftX = classicLeftColumnX(width);
        int rightX = classicRightColumnX(width);
        BaseMenuScreen screen = new BaseMenuScreen("Options", true, false, done);
        screen.add(new MenuSlider("Music", leftX, y, CLASSIC_OPTION_WIDTH, CLASSIC_BUTTON_HEIGHT,
                settings::getMusicVolume, value -> {
                    settings.setMusicVolume(value);
                    changed.run();
                }, MenuScreens::percent));
        screen.add(new MenuSlider("Sound", rightX, y, CLASSIC_OPTION_WIDTH, CLASSIC_BUTTON_HEIGHT,
                settings::getSoundVolume, value -> {
                    settings.setSoundVolume(value);
                    changed.run();
                }, MenuScreens::percent));
        MenuButton invert = new MenuButton("Invert Mouse: " + onOff(settings.isInvertYMouse()),
                leftX, y + CLASSIC_ROW_STEP, CLASSIC_OPTION_WIDTH, CLASSIC_BUTTON_HEIGHT, null);
        invert.setAction(() -> {
            settings.setInvertYMouse(!settings.isInvertYMouse());
            invert.setLabel("Invert Mouse: " + onOff(settings.isInvertYMouse()));
            changed.run();
        });
        screen.add(invert);
        screen.add(new MenuSlider("Sensitivity", rightX, y + CLASSIC_ROW_STEP, CLASSIC_OPTION_WIDTH,
                CLASSIC_BUTTON_HEIGHT,
                settings::getMouseSensitivity, value -> {
                    settings.setMouseSensitivity(value);
                    changed.run();
                }, MenuScreens::sensitivityLabel));
        screen.add(new MenuSlider("FOV", leftX, y + CLASSIC_ROW_STEP * 2, CLASSIC_OPTION_WIDTH, CLASSIC_BUTTON_HEIGHT,
                settings::getFov, value -> {
                    settings.setFov(value);
                    changed.run();
                }, MenuScreens::fovLabel));
        MenuButton difficulty = new MenuButton("Difficulty: " + difficultyLabel(settings.getDifficulty()),
                rightX, y + CLASSIC_ROW_STEP * 2, CLASSIC_OPTION_WIDTH, CLASSIC_BUTTON_HEIGHT, null);
        difficulty.setAction(() -> {
            settings.setDifficulty(next(settings.getDifficulty()));
            difficulty.setLabel("Difficulty: " + difficultyLabel(settings.getDifficulty()));
            changed.run();
        });
        screen.add(difficulty);
        screen.add(new MenuButton("Video Settings...", classicCenteredX(width, CLASSIC_FULL_BUTTON_WIDTH), y + 84,
                CLASSIC_FULL_BUTTON_WIDTH, CLASSIC_BUTTON_HEIGHT, video));
        screen.add(new MenuButton("Controls...", classicCenteredX(width, CLASSIC_FULL_BUTTON_WIDTH), y + 108,
                CLASSIC_FULL_BUTTON_WIDTH, CLASSIC_BUTTON_HEIGHT, controls));
        screen.add(new MenuButton("Language...", classicCenteredX(width, CLASSIC_FULL_BUTTON_WIDTH), y + 132,
                CLASSIC_FULL_BUTTON_WIDTH, CLASSIC_BUTTON_HEIGHT, language));
        screen.add(new MenuButton("Done", classicCenteredX(width, CLASSIC_FULL_BUTTON_WIDTH), height - 28,
                CLASSIC_FULL_BUTTON_WIDTH, CLASSIC_BUTTON_HEIGHT, done));
        return screen;
    }

    public static BaseMenuScreen video(int width, int height, GameSettings settings, Runnable done) {
        return video(width, height, settings, done, () -> {
        });
    }

    public static BaseMenuScreen video(int width, int height, GameSettings settings, Runnable done, Runnable changed) {
        int y = 54;
        int leftX = classicLeftColumnX(width);
        int rightX = classicRightColumnX(width);
        BaseMenuScreen screen = new BaseMenuScreen("Video Settings", true, false, done);
        MenuButton graphics = new MenuButton(graphicsLabel(settings), leftX, y, CLASSIC_OPTION_WIDTH,
                CLASSIC_BUTTON_HEIGHT, null);
        graphics.setAction(() -> {
            settings.setFancyGraphics(!settings.isFancyGraphics());
            graphics.setLabel(graphicsLabel(settings));
            changed.run();
        });
        screen.add(graphics);
        MenuButton renderDistance = new MenuButton("render-distance", renderDistanceLabel(settings),
                new Rect(rightX, y, CLASSIC_OPTION_WIDTH, CLASSIC_BUTTON_HEIGHT), null);
        renderDistance.setAction(() -> {
            settings.cycleRenderDistance();
            renderDistance.setLabel(renderDistanceLabel(settings));
            changed.run();
        });
        screen.add(renderDistance);
        MenuButton smooth = new MenuButton("Smooth Lighting: " + onOff(settings.isSmoothLighting()),
                leftX, y + CLASSIC_ROW_STEP, CLASSIC_OPTION_WIDTH, CLASSIC_BUTTON_HEIGHT, null);
        smooth.setAction(() -> {
            settings.setSmoothLighting(!settings.isSmoothLighting());
            smooth.setLabel("Smooth Lighting: " + onOff(settings.isSmoothLighting()));
            changed.run();
        });
        screen.add(smooth);
        MenuButton performance = new MenuButton(performanceLabel(settings), rightX, y + CLASSIC_ROW_STEP,
                CLASSIC_OPTION_WIDTH, CLASSIC_BUTTON_HEIGHT, null);
        performance.setAction(() -> {
            settings.setFramerateLimit(nextFrameLimit(settings.getFramerateLimit()));
            performance.setLabel(performanceLabel(settings));
            changed.run();
        });
        screen.add(performance);
        MenuButton anaglyph = new MenuButton("3D Anaglyph: " + onOff(settings.isAnaglyph3d()),
                leftX, y + CLASSIC_ROW_STEP * 2, CLASSIC_OPTION_WIDTH, CLASSIC_BUTTON_HEIGHT, null);
        anaglyph.setAction(() -> {
            settings.setAnaglyph3d(!settings.isAnaglyph3d());
            anaglyph.setLabel("3D Anaglyph: " + onOff(settings.isAnaglyph3d()));
            changed.run();
        });
        screen.add(anaglyph);
        MenuButton bobbing = new MenuButton("View Bobbing: " + onOff(settings.isViewBobbing()),
                rightX, y + CLASSIC_ROW_STEP * 2, CLASSIC_OPTION_WIDTH, CLASSIC_BUTTON_HEIGHT, null);
        bobbing.setAction(() -> {
            settings.setViewBobbing(!settings.isViewBobbing());
            bobbing.setLabel("View Bobbing: " + onOff(settings.isViewBobbing()));
            changed.run();
        });
        screen.add(bobbing);
        MenuButton guiScale = new MenuButton("GUI Scale: " + guiScaleLabel(settings.getGuiScale()),
                leftX, y + CLASSIC_ROW_STEP * 3, CLASSIC_OPTION_WIDTH, CLASSIC_BUTTON_HEIGHT, null);
        guiScale.setAction(() -> {
            settings.setGuiScale((settings.getGuiScale() + 1) % 4);
            guiScale.setLabel("GUI Scale: " + guiScaleLabel(settings.getGuiScale()));
            changed.run();
        });
        screen.add(guiScale);
        MenuButton advancedOpenGl = new MenuButton("Advanced OpenGL: " + onOff(settings.isAdvancedOpenGl()),
                rightX, y + CLASSIC_ROW_STEP * 3, CLASSIC_OPTION_WIDTH, CLASSIC_BUTTON_HEIGHT, null);
        advancedOpenGl.setAction(() -> {
            settings.setAdvancedOpenGl(!settings.isAdvancedOpenGl());
            advancedOpenGl.setLabel("Advanced OpenGL: " + onOff(settings.isAdvancedOpenGl()));
            changed.run();
        });
        screen.add(advancedOpenGl);
        screen.add(new MenuSlider("Brightness", leftX, y + CLASSIC_ROW_STEP * 4, CLASSIC_OPTION_WIDTH,
                CLASSIC_BUTTON_HEIGHT,
                settings::getGamma, value -> {
                    settings.setGamma(value);
                    changed.run();
                }, MenuScreens::brightnessLabel));
        MenuButton clouds = new MenuButton("Clouds: " + onOff(settings.isClouds()), rightX,
                y + CLASSIC_ROW_STEP * 4, CLASSIC_OPTION_WIDTH, CLASSIC_BUTTON_HEIGHT, null);
        clouds.setAction(() -> {
            settings.setClouds(!settings.isClouds());
            clouds.setLabel("Clouds: " + onOff(settings.isClouds()));
            changed.run();
        });
        screen.add(clouds);
        MenuButton particles = new MenuButton("Particles: " + particlesLabel(settings.getParticles()), leftX,
                y + CLASSIC_ROW_STEP * 5, CLASSIC_OPTION_WIDTH, CLASSIC_BUTTON_HEIGHT, null);
        particles.setAction(() -> {
            settings.setParticles((settings.getParticles() + 1) % 3);
            particles.setLabel("Particles: " + particlesLabel(settings.getParticles()));
            changed.run();
        });
        screen.add(particles);
        screen.add(new MenuButton("Done", classicCenteredX(width, CLASSIC_FULL_BUTTON_WIDTH), height - 28,
                CLASSIC_FULL_BUTTON_WIDTH, CLASSIC_BUTTON_HEIGHT, done));
        return screen;
    }

    private static int classicLeftColumnX(int width) {
        int pairWidth = CLASSIC_OPTION_WIDTH * 2 + CLASSIC_OPTION_GAP;
        return Math.max(4, width / 2 - pairWidth / 2);
    }

    private static int classicRightColumnX(int width) {
        return classicLeftColumnX(width) + CLASSIC_OPTION_WIDTH + CLASSIC_OPTION_GAP;
    }

    private static int classicCenteredX(int width, int buttonWidth) {
        return Math.max(4, width / 2 - buttonWidth / 2);
    }

    public static BaseMenuScreen worldSelect(int width, int height, Supplier<List<WorldInfo>> worlds,
            Consumer<WorldInfo> play, Runnable create, Consumer<WorldInfo> rename, Consumer<WorldInfo> delete,
            Runnable cancel) {
        int cx = width / 2;
        int listWidth = Math.min(440, Math.max(240, width - 48));
        int rowHeight = 22;
        int listY = 48;
        int visibleRows = Math.max(4, Math.min(8, (height - 124) / rowHeight));
        MenuList<WorldInfo> list = new MenuList<>(worlds.get(), WorldInfo::displayName,
                cx - listWidth / 2, listY, listWidth, rowHeight, visibleRows);
        list.setOnActivated(play);
        BaseMenuScreen screen = new BaseMenuScreen("Select World", true, false, cancel);
        screen.add(list);
        int y = height - 58;
        screen.add(new MenuButton("Play Selected", cx - 154, y, 150, 20, () -> play.accept(list.selected())));
        screen.add(new MenuButton("Create New World", cx + 4, y, 150, 20, create));
        screen.add(new MenuButton("Rename", cx - 154, y + CLASSIC_ROW_STEP, 72, 20,
                () -> rename.accept(list.selected())));
        screen.add(new MenuButton("Delete", cx - 78, y + CLASSIC_ROW_STEP, 72, 20,
                () -> delete.accept(list.selected())));
        screen.add(new MenuButton("Cancel", cx + 4, y + CLASSIC_ROW_STEP, 150, 20, cancel));
        return screen;
    }

    public static BaseMenuScreen createWorld(int width, int height, Consumer<CreateWorldRequest> create, Runnable cancel) {
        int cx = width / 2;
        int fieldWidth = Math.min(240, Math.max(180, width - 80));
        int fieldX = cx - fieldWidth / 2;
        int top = Math.max(54, height / 4);
        TextField name = new TextField("New World", 32, fieldX, top + 18, fieldWidth, 20);
        TextField seed = new TextField("", 64, fieldX, top + 64, fieldWidth, 20);
        final GameMode[] mode = { GameMode.SURVIVAL };
        Runnable createAction = () -> create.accept(new CreateWorldRequest(name.value(), seed.value(), mode[0]));
        name.onEnter(createAction);
        seed.onEnter(createAction);
        BaseMenuScreen screen = new BaseMenuScreen("Create New World", true, false, cancel);
        screen.add(MenuLabel.centered("world-name-label", "World Name", cx, top + 4, fieldWidth)
                .color(0.82f, 0.82f, 0.82f, 1.0f));
        screen.add(name);
        screen.add(MenuLabel.centered("world-seed-label", "Seed for the World Generator", cx, top + 50, fieldWidth)
                .color(0.82f, 0.82f, 0.82f, 1.0f));
        screen.add(seed);
        MenuButton modeButton = new MenuButton("Game Mode: Survival", cx - 100, top + 96, 200, 20, null);
        modeButton.setAction(() -> {
            mode[0] = nextMode(mode[0]);
            modeButton.setLabel("Game Mode: " + modeLabel(mode[0]));
        });
        screen.add(modeButton);
        screen.add(new MenuButton("Create New World", cx - 100, height - 56, 200, 20, createAction));
        screen.add(new MenuButton("Cancel", cx - 100, height - 28, 200, 20, cancel));
        return screen;
    }

    public static BaseMenuScreen texturePacks(int width, int height, ResourcePackManager packs, GameSettings settings,
            Runnable done) {
        int cx = width / 2;
        int listWidth = Math.min(340, Math.max(220, width - 48));
        int rowHeight = 22;
        MenuList<ResourcePackManager.PackInfo> list = new MenuList<>(safePacks(packs), ResourcePackManager.PackInfo::displayName,
                cx - listWidth / 2, 52, listWidth, rowHeight, Math.max(4, Math.min(7, (height - 116) / rowHeight)));
        BaseMenuScreen screen = new BaseMenuScreen("Texture Packs", true, false, done);
        screen.add(list);
        screen.add(new MenuButton("Select", cx - 100, height - 56, 98, 20, () -> {
            ResourcePackManager.PackInfo selected = list.selected();
            if (selected != null) {
                packs.setSelectedPackId(selected.id());
                settings.setSelectedTexturePack(selected.id());
            }
        }));
        screen.add(new MenuButton("Done", cx + 2, height - 56, 98, 20, done));
        return screen;
    }

    public static BaseMenuScreen multiplayer(int width, int height, Runnable directConnect, Runnable hostWorld,
            Runnable cancel) {
        int cx = width / 2;
        BaseMenuScreen screen = new BaseMenuScreen("Multiplayer", true, false, cancel);
        screen.add(new MenuButton("Direct Connect", cx - 100, height / 2 - 24, 200, 20, directConnect));
        screen.add(new MenuButton("Host World", cx - 100, height / 2 + 4, 200, 20, hostWorld));
        screen.add(new MenuButton("Cancel", cx - 100, height / 2 + 40, 200, 20, cancel));
        return screen;
    }

    public static BaseMenuScreen message(String title, String message, int width, int height, Runnable done) {
        BaseMenuScreen screen = new BaseMenuScreen(title, true, false, done);
        screen.add(new MenuButton("Done", width / 2 - 100, height - 48, 200, 20, done));
        screen.onTick(() -> {
        });
        return screen;
    }

    public static BaseMenuScreen endCredits(int width, int height, Runnable done) {
        int cx = width / 2;
        int top = Math.max(56, height / 4);
        int textWidth = Math.min(360, Math.max(180, width - 40));
        BaseMenuScreen screen = new BaseMenuScreen("The End.", true, false, done);
        screen.add(MenuLabel.centered("end-credits-line-1", "You wake from the End.", cx, top + 24, textWidth)
                .color(1.0f, 1.0f, 0.63f, 1.0f));
        screen.add(MenuLabel.centered("end-credits-line-2",
                fitMenuText("The dragon is gone, and the portal has carried you home.", textWidth),
                cx, top + 46, textWidth)
                .color(0.85f, 0.85f, 0.85f, 1.0f));
        screen.add(MenuLabel.centered("end-credits-line-3",
                fitMenuText("Your world waits at the spawn point.", textWidth),
                cx, top + 64, textWidth)
                .color(0.85f, 0.85f, 0.85f, 1.0f));
        screen.add(new MenuButton("Continue", cx - 100, height - 48, 200, 20, done));
        return screen;
    }

    static void setVisible(List<MenuLabel> labels, boolean visible) {
        for (MenuLabel label : labels) {
            label.visible(visible);
        }
    }

    static List<String> statisticRows(PlayerStatistics stats, int width) {
        List<StatisticRow> rows = generalStatisticRows(stats == null ? new PlayerStatistics() : stats);
        List<String> labels = new ArrayList<>(rows.size() + 1);
        labels.add(statisticHeader("Statistic", "Value"));
        for (StatisticRow row : rows) {
            labels.add(statisticTableRow(row.label(), row.value()));
        }
        return List.copyOf(labels);
    }

    private static List<StatisticRow> generalStatisticRows(PlayerStatistics stats) {
        List<StatisticRow> rows = new ArrayList<>();
        rows.add(new StatisticRow("statistics-timesPlayed", "Times Played", Long.toString(stats.getTimesPlayed())));
        rows.add(new StatisticRow("statistics-gamesQuit", "Games quit", Long.toString(stats.getGamesQuit())));
        rows.add(new StatisticRow("statistics-playTime", "Minutes Played", formatPlayTime(stats.getPlayTimeTicks())));

        rows.add(new StatisticRow("statistics-distance", "Distance Walked", formatBlocks(stats.getDistanceWalkedCm())));
        rows.add(new StatisticRow("statistics-distanceSwum", "Distance Swum", formatBlocks(stats.getDistanceSwumCm())));
        rows.add(new StatisticRow("statistics-distanceFallen", "Distance Fallen",
                formatBlocks(stats.getDistanceFallenCm())));
        rows.add(new StatisticRow("statistics-distanceClimbed", "Distance Climbed",
                formatBlocks(stats.getDistanceClimbedCm())));
        rows.add(new StatisticRow("statistics-distanceFlown", "Distance Flown",
                formatBlocks(stats.getDistanceFlownCm())));
        rows.add(new StatisticRow("statistics-distanceDove", "Distance Dove", formatBlocks(stats.getDistanceDoveCm())));
        rows.add(new StatisticRow("statistics-distanceMinecart", "Distance by Minecart",
                formatBlocks(stats.getDistanceByMinecartCm())));
        rows.add(new StatisticRow("statistics-distanceBoat", "Distance by Boat",
                formatBlocks(stats.getDistanceByBoatCm())));
        rows.add(new StatisticRow("statistics-distancePig", "Distance by Pig", formatBlocks(stats.getDistanceByPigCm())));

        rows.add(new StatisticRow("statistics-jumps", "Jumps", Long.toString(stats.getJumps())));
        rows.add(new StatisticRow("statistics-blocksMined", "Blocks Mined", Long.toString(stats.getBlocksMined())));
        rows.add(new StatisticRow("statistics-itemsPickedUp", "Items Picked Up",
                Long.toString(stats.getItemsPickedUp())));
        rows.add(new StatisticRow("statistics-itemsDropped", "Items Dropped", Long.toString(stats.getItemsDropped())));
        rows.add(new StatisticRow("statistics-itemsCrafted", "Items Crafted", Long.toString(stats.getItemsCrafted())));
        rows.add(new StatisticRow("statistics-itemsUsed", "Items Used", Long.toString(stats.getItemsUsed())));
        rows.add(new StatisticRow("statistics-itemsDepleted", "Items Depleted",
                Long.toString(stats.getItemsDepleted())));
        rows.add(new StatisticRow("statistics-fishCaught", "Fish Caught", Long.toString(stats.getFishCaught())));

        rows.add(new StatisticRow("statistics-attacks", "Attacks Landed", Long.toString(stats.getSuccessfulAttacks())));
        rows.add(new StatisticRow("statistics-damageDealt", "Damage Dealt", formatTenths(stats.getDamageDealtTenths())));
        rows.add(new StatisticRow("statistics-damageTaken", "Damage Taken", formatTenths(stats.getDamageTakenTenths())));
        rows.add(new StatisticRow("statistics-deaths", "Number of Deaths", Long.toString(stats.getDeaths())));
        rows.add(new StatisticRow("statistics-mobKills", "Mob Kills", Long.toString(stats.getMobKills())));
        rows.add(new StatisticRow("statistics-monsterKills", "Monster Kills", Long.toString(stats.getMonsterKills())));
        rows.add(new StatisticRow("statistics-playerKills", "Player Kills", Long.toString(stats.getPlayerKills())));

        rows.add(new StatisticRow("statistics-worldsLoaded", "Worlds loaded", Long.toString(stats.getWorldsLoaded())));
        rows.add(new StatisticRow("statistics-multiplayerJoins", "Multiplayer joins",
                Long.toString(stats.getMultiplayerJoins())));
        rows.add(new StatisticRow("statistics-worldsSaved", "Worlds saved", Long.toString(stats.getWorldsSaved())));
        return List.copyOf(rows);
    }

    static List<String> blockStatisticRows(PlayerStatistics stats, int width) {
        stats = stats == null ? new PlayerStatistics() : stats;
        List<String> rows = new ArrayList<>();
        rows.add(statisticHeader("Block", "Mined", "Craft", "Used", "Pick", "Drop"));
        List<BlockType> blockTypes = new ArrayList<>(List.of(BlockType.values()));
        blockTypes.sort(Comparator.comparingInt(BlockType::getId));
        for (BlockType type : blockTypes) {
            if (type == BlockType.AIR) {
                continue;
            }
            ItemType item = ItemType.fromBlock(type);
            long crafted = item == null ? 0 : stats.getItemsCrafted(item);
            long used = item == null ? 0 : stats.getItemsUsed(item);
            long pickedUp = item == null ? 0 : stats.getItemsPickedUp(item);
            long dropped = item == null ? 0 : stats.getItemsDropped(item);
            long mined = stats.getBlocksMined(type);
            if (crafted <= 0 && used <= 0 && mined <= 0 && pickedUp <= 0 && dropped <= 0) {
                continue;
            }
            rows.add(statisticTableRow(blockDisplayName(type),
                    statisticValue(mined),
                    statisticValue(crafted),
                    statisticValue(used),
                    statisticValue(pickedUp),
                    statisticValue(dropped)));
        }
        if (rows.size() <= 1) {
            return List.of("No block statistics yet");
        }
        return List.copyOf(rows);
    }

    static List<String> itemStatisticRows(PlayerStatistics stats, int width) {
        stats = stats == null ? new PlayerStatistics() : stats;
        List<PlayerStatistics.ItemStatistic> statistics = stats.itemStatistics();
        if (statistics.isEmpty()) {
            return List.of("No item statistics yet");
        }
        List<String> rows = new ArrayList<>();
        rows.add(statisticHeader("Item", "Pick", "Drop", "Craft", "Used", "Break"));
        for (PlayerStatistics.ItemStatistic row : statistics) {
            if (row.type().isBlockItem()) {
                continue;
            }
            if (row.pickedUp() <= 0 && row.dropped() <= 0 && row.crafted() <= 0
                    && row.used() <= 0 && row.depleted() <= 0) {
                continue;
            }
            rows.add(statisticTableRow(row.type().getDisplayName(),
                    statisticValue(row.pickedUp()),
                    statisticValue(row.dropped()),
                    statisticValue(row.crafted()),
                    statisticValue(row.used()),
                    statisticValue(row.depleted())));
        }
        if (rows.size() <= 1) {
            return List.of("No item statistics yet");
        }
        return List.copyOf(rows);
    }

    private static String statisticHeader(String... columns) {
        return "#" + statisticTableRow(columns);
    }

    private static String statisticTableRow(String... columns) {
        return String.join("\t", columns);
    }

    private static String statisticValue(long value) {
        if (value <= 0) {
            return "-";
        }
        return Long.toString(value);
    }

    private record StatisticRow(String id, String label, String value) {
        private String text() {
            return label + ": " + value;
        }
    }

    private static String blockDisplayName(BlockType type) {
        ItemType item = ItemType.fromBlock(type);
        return item == null ? titleCase(type.name()) : item.getDisplayName();
    }

    private static String titleCase(String enumName) {
        String[] parts = enumName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(part.charAt(0)).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    private static String percent(float value) {
        if (value <= 0.001f) {
            return "OFF";
        }
        return Math.round(value * 100.0f) + "%";
    }

    private static String sensitivityLabel(float value) {
        if (value <= 0.001f) {
            return "*yawn*";
        }
        if (value >= 0.999f) {
            return "HYPERSPEED!!!";
        }
        return Math.round(value * 200.0f) + "%";
    }

    static String fitMenuText(String text, int width) {
        int maxChars = Math.max(8, width / 6);
        if (text.length() <= maxChars) {
            return text;
        }
        if (maxChars <= 3) {
            return text.substring(0, maxChars);
        }
        return text.substring(0, maxChars - 3) + "...";
    }

    static String formatPlayTime(long ticks) {
        long seconds = Math.max(0, ticks) / 20;
        long minutes = seconds / 60;
        long remainderSeconds = seconds % 60;
        if (minutes <= 0) {
            return seconds + " s";
        }
        return minutes + " m " + remainderSeconds + " s";
    }

    static String formatBlocks(long centimeters) {
        double blocks = Math.max(0, centimeters) / 100.0;
        if (centimeters % 100 == 0) {
            return (centimeters / 100) + " blocks";
        }
        return String.format(Locale.ROOT, "%.2f blocks", blocks);
    }

    static String formatTenths(long tenths) {
        if (tenths % 10 == 0) {
            return Long.toString(tenths / 10);
        }
        return String.format(Locale.ROOT, "%.1f", tenths / 10.0);
    }

    private static String fovLabel(float value) {
        if (value <= 0.001f) {
            return "Normal";
        }
        if (value >= 0.999f) {
            return "Quake Pro";
        }
        return Integer.toString(Math.round(70.0f + value * 40.0f));
    }

    private static String difficultyLabel(Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL -> "Peaceful";
            case EASY -> "Easy";
            case NORMAL -> "Normal";
            case HARD -> "Hard";
        };
    }

    private static String graphicsLabel(GameSettings settings) {
        return "Graphics: " + (settings.isFancyGraphics() ? "Fancy" : "Fast");
    }

    private static String renderDistanceLabel(GameSettings settings) {
        return "Render Distance: " + GameSettings.renderDistanceDisplayName(settings.getRenderDistance());
    }

    private static String performanceLabel(GameSettings settings) {
        int limit = settings.getFramerateLimit();
        if (limit <= 0) {
            return "Performance: Max FPS";
        }
        if (limit <= 40) {
            return "Performance: Power Saver";
        }
        return "Performance: Balanced";
    }

    private static int nextFrameLimit(int limit) {
        if (limit == 120) {
            return 0;
        }
        if (limit <= 0) {
            return 40;
        }
        return 120;
    }

    private static String guiScaleLabel(int value) {
        return switch (value) {
            case 0 -> "Auto";
            case 1 -> "Small";
            case 2 -> "Normal";
            default -> "Large";
        };
    }

    private static String particlesLabel(int value) {
        return switch (value) {
            case 1 -> "Decreased";
            case 2 -> "Minimal";
            default -> "All";
        };
    }

    private static String brightnessLabel(float value) {
        if (value <= 0.001f) {
            return "Moody";
        }
        if (value >= 0.999f) {
            return "Bright";
        }
        return "+" + Math.round(value * 100.0f) + "%";
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static Difficulty next(Difficulty difficulty) {
        Difficulty[] values = Difficulty.values();
        return values[(difficulty.ordinal() + 1) % values.length];
    }

    private static GameMode nextMode(GameMode mode) {
        return switch (mode) {
            case SURVIVAL -> GameMode.CREATIVE;
            case CREATIVE -> GameMode.HARDCORE;
            case HARDCORE -> GameMode.SURVIVAL;
        };
    }

    private static String modeLabel(GameMode mode) {
        return switch (mode) {
            case SURVIVAL -> "Survival";
            case CREATIVE -> "Creative";
            case HARDCORE -> "Hardcore";
        };
    }

    private static List<ResourcePackManager.PackInfo> safePacks(ResourcePackManager packs) {
        try {
            return packs.listPacks();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public record CreateWorldRequest(String name, String seed, GameMode gameMode) {
    }
}
