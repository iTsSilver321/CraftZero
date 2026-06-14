package com.craftzero.ui.menu;

import com.craftzero.main.Difficulty;
import com.craftzero.main.GameMode;
import com.craftzero.main.GameSettings;
import com.craftzero.resources.ResourcePackManager;
import com.craftzero.save.WorldManager.WorldInfo;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MenuScreens {
    private MenuScreens() {
    }

    public static BaseMenuScreen title(int width, int height, Runnable singleplayer, Runnable multiplayer,
            Runnable texturePacks, Runnable options, Runnable quit) {
        int cx = width / 2;
        int y = height / 4 + 64;
        BaseMenuScreen screen = new BaseMenuScreen("CraftZero", false, true, null);
        screen.add(new MenuButton("Singleplayer", cx - 100, y, 200, 20, singleplayer));
        screen.add(new MenuButton("Multiplayer", cx - 100, y + 24, 200, 20, multiplayer));
        screen.add(new MenuButton("Texture Packs", cx - 100, y + 48, 200, 20, texturePacks));
        screen.add(new MenuButton("Options...", cx - 100, y + 84, 98, 20, options));
        screen.add(new MenuButton("Quit Game", cx + 2, y + 84, 98, 20, quit));
        return screen;
    }

    public static BaseMenuScreen pause(int width, int height, Runnable back, Runnable options, Runnable saveQuit) {
        int cx = width / 2;
        int y = height / 4 + 48;
        BaseMenuScreen screen = new BaseMenuScreen("Game menu", false, false, back);
        screen.add(new MenuButton("Back to game", cx - 100, y, 200, 20, back));
        screen.add(new MenuButton("Achievements", cx - 100, y + 24, 98, 20, () -> {
        }).enabled(false));
        screen.add(new MenuButton("Statistics", cx + 2, y + 24, 98, 20, () -> {
        }).enabled(false));
        screen.add(new MenuButton("Options...", cx - 100, y + 60, 200, 20, options));
        screen.add(new MenuButton("Save and quit to title", cx - 100, y + 96, 200, 20, saveQuit));
        return screen;
    }

    public static BaseMenuScreen options(int width, int height, GameSettings settings, Runnable video,
            Runnable controls, Runnable done) {
        return options(width, height, settings, video, controls, done, () -> {
        });
    }

    public static BaseMenuScreen options(int width, int height, GameSettings settings, Runnable video,
            Runnable controls, Runnable done, Runnable changed) {
        int cx = width / 2;
        int y = 60;
        BaseMenuScreen screen = new BaseMenuScreen("Options", true, false, done);
        screen.add(new MenuSlider("Music", cx - 205, y, 200, 20,
                settings::getMusicVolume, value -> {
                    settings.setMusicVolume(value);
                    changed.run();
                }, MenuScreens::percent));
        screen.add(new MenuSlider("Sound", cx + 5, y, 200, 20,
                settings::getSoundVolume, value -> {
                    settings.setSoundVolume(value);
                    changed.run();
                }, MenuScreens::percent));
        MenuButton invert = new MenuButton("Invert Mouse: " + onOff(settings.isInvertYMouse()), cx - 205, y + 24, 200, 20,
                null);
        invert.setAction(() -> {
            settings.setInvertYMouse(!settings.isInvertYMouse());
            invert.setLabel("Invert Mouse: " + onOff(settings.isInvertYMouse()));
            changed.run();
        });
        screen.add(invert);
        screen.add(new MenuSlider("Sensitivity", cx + 5, y + 24, 200, 20,
                settings::getMouseSensitivity, value -> {
                    settings.setMouseSensitivity(value);
                    changed.run();
                }, MenuScreens::percent));
        screen.add(new MenuSlider("FOV", cx - 205, y + 48, 200, 20,
                settings::getFov, value -> {
                    settings.setFov(value);
                    changed.run();
                }, MenuScreens::fovLabel));
        MenuButton difficulty = new MenuButton("Difficulty: " + difficultyLabel(settings.getDifficulty()), cx + 5, y + 48,
                200, 20, null);
        difficulty.setAction(() -> {
            settings.setDifficulty(next(settings.getDifficulty()));
            difficulty.setLabel("Difficulty: " + difficultyLabel(settings.getDifficulty()));
            changed.run();
        });
        screen.add(difficulty);
        screen.add(new MenuButton("Video Settings...", cx - 100, y + 84, 200, 20, video));
        screen.add(new MenuButton("Controls...", cx - 100, y + 108, 200, 20, controls));
        screen.add(new MenuButton("Done", cx - 100, height - 28, 200, 20, done));
        return screen;
    }

    public static BaseMenuScreen video(int width, int height, GameSettings settings, Runnable done) {
        return video(width, height, settings, done, () -> {
        });
    }

    public static BaseMenuScreen video(int width, int height, GameSettings settings, Runnable done, Runnable changed) {
        int cx = width / 2;
        int y = 54;
        BaseMenuScreen screen = new BaseMenuScreen("Video Settings", true, false, done);
        MenuButton graphics = new MenuButton(graphicsLabel(settings), cx - 205, y, 200, 20, null);
        graphics.setAction(() -> {
            settings.setFancyGraphics(!settings.isFancyGraphics());
            graphics.setLabel(graphicsLabel(settings));
            changed.run();
        });
        screen.add(graphics);
        MenuSlider renderDistance = new MenuSlider("render-distance", "Render Distance", new Rect(cx + 5, y, 200, 20),
                GameSettings.MIN_RENDER_DISTANCE_CHUNKS, GameSettings.MAX_RENDER_DISTANCE_CHUNKS,
                settings.getRenderDistance(), 1.0, value -> {
                    settings.setRenderDistance((int) Math.round(value));
                    changed.run();
                });
        renderDistance.setFormatter(value -> Math.round(value) + " chunks");
        screen.add(renderDistance);
        MenuButton smooth = new MenuButton("Smooth Lighting: " + onOff(settings.isSmoothLighting()), cx - 205, y + 24,
                200, 20, null);
        smooth.setAction(() -> {
            settings.setSmoothLighting(!settings.isSmoothLighting());
            smooth.setLabel("Smooth Lighting: " + onOff(settings.isSmoothLighting()));
            changed.run();
        });
        screen.add(smooth);
        MenuButton performance = new MenuButton(performanceLabel(settings), cx + 5, y + 24, 200, 20, null);
        performance.setAction(() -> {
            settings.setFramerateLimit(nextFrameLimit(settings.getFramerateLimit()));
            performance.setLabel(performanceLabel(settings));
            changed.run();
        });
        screen.add(performance);
        screen.add(new MenuButton("3D Anaglyph: OFF", cx - 205, y + 48, 200, 20, () -> {
        }).enabled(false));
        MenuButton bobbing = new MenuButton("View Bobbing: " + onOff(settings.isViewBobbing()), cx + 5, y + 48, 200, 20,
                null);
        bobbing.setAction(() -> {
            settings.setViewBobbing(!settings.isViewBobbing());
            bobbing.setLabel("View Bobbing: " + onOff(settings.isViewBobbing()));
            changed.run();
        });
        screen.add(bobbing);
        MenuButton guiScale = new MenuButton("GUI Scale: " + guiScaleLabel(settings.getGuiScale()), cx - 205, y + 72,
                200, 20, null);
        guiScale.setAction(() -> {
            settings.setGuiScale((settings.getGuiScale() + 1) % 5);
            guiScale.setLabel("GUI Scale: " + guiScaleLabel(settings.getGuiScale()));
            changed.run();
        });
        screen.add(guiScale);
        screen.add(new MenuSlider("Brightness", cx + 5, y + 72, 200, 20,
                settings::getGamma, value -> {
                    settings.setGamma(value);
                    changed.run();
                }, MenuScreens::percent));
        MenuButton fullscreen = new MenuButton("Fullscreen: " + onOff(settings.isFullscreen()), cx - 205, y + 96, 200, 20,
                null);
        fullscreen.setAction(() -> {
            settings.setFullscreen(!settings.isFullscreen());
            fullscreen.setLabel("Fullscreen: " + onOff(settings.isFullscreen()));
            changed.run();
        });
        screen.add(fullscreen);
        MenuButton vsync = new MenuButton("Use VSync: " + onOff(settings.isVsync()), cx + 5, y + 96, 200, 20, null);
        vsync.setAction(() -> {
            settings.setVsync(!settings.isVsync());
            vsync.setLabel("Use VSync: " + onOff(settings.isVsync()));
            changed.run();
        });
        screen.add(vsync);
        MenuButton clouds = new MenuButton("Clouds: " + onOff(settings.isClouds()), cx - 205, y + 120, 200, 20, null);
        clouds.setAction(() -> {
            settings.setClouds(!settings.isClouds());
            clouds.setLabel("Clouds: " + onOff(settings.isClouds()));
            changed.run();
        });
        screen.add(clouds);
        MenuButton advancedOpenGl = new MenuButton("Advanced OpenGL: " + onOff(settings.isAdvancedOpenGl()), cx + 5,
                y + 120, 200, 20, null);
        advancedOpenGl.setAction(() -> {
            settings.setAdvancedOpenGl(!settings.isAdvancedOpenGl());
            advancedOpenGl.setLabel("Advanced OpenGL: " + onOff(settings.isAdvancedOpenGl()));
            changed.run();
        });
        screen.add(advancedOpenGl);
        screen.add(new MenuButton("Done", cx - 100, height - 28, 200, 20, done));
        return screen;
    }

    public static BaseMenuScreen worldSelect(int width, int height, Supplier<List<WorldInfo>> worlds,
            Consumer<WorldInfo> play, Runnable create, Consumer<WorldInfo> rename, Consumer<WorldInfo> delete,
            Runnable cancel) {
        int listWidth = Math.min(520, Math.max(280, width - 40));
        int rowHeight = 24;
        int listY = 48;
        int visibleRows = Math.max(4, Math.min(7, (height - 116) / rowHeight));
        MenuList<WorldInfo> list = new MenuList<>(worlds.get(), WorldInfo::displayName,
                width / 2 - listWidth / 2, listY, listWidth, rowHeight, visibleRows);
        BaseMenuScreen screen = new BaseMenuScreen("Select World", true, false, cancel);
        screen.add(list);
        int y = height - 60;
        screen.add(new MenuButton("Play Selected", width / 2 - 204, y, 100, 20, () -> play.accept(list.selected())));
        screen.add(new MenuButton("Create New World", width / 2 - 100, y, 120, 20, create));
        screen.add(new MenuButton("Rename", width / 2 + 24, y, 80, 20, () -> rename.accept(list.selected())));
        screen.add(new MenuButton("Delete", width / 2 + 108, y, 96, 20, () -> delete.accept(list.selected())));
        screen.add(new MenuButton("Cancel", width / 2 - 100, y + 28, 200, 20, cancel));
        return screen;
    }

    public static BaseMenuScreen createWorld(int width, int height, Consumer<CreateWorldRequest> create, Runnable cancel) {
        int cx = width / 2;
        TextField name = new TextField("New World", 32, cx - 100, 62, 200, 20);
        TextField seed = new TextField("", 64, cx - 100, 110, 200, 20);
        final GameMode[] mode = { GameMode.SURVIVAL };
        Runnable createAction = () -> create.accept(new CreateWorldRequest(name.value(), seed.value(), mode[0]));
        name.onEnter(createAction);
        seed.onEnter(createAction);
        BaseMenuScreen screen = new BaseMenuScreen("Create New World", true, false, cancel);
        screen.add(name);
        screen.add(seed);
        MenuButton modeButton = new MenuButton("Game Mode: Survival", cx - 100, 148, 200, 20, null);
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
        int listWidth = Math.min(360, Math.max(260, width - 40));
        int rowHeight = 24;
        MenuList<ResourcePackManager.PackInfo> list = new MenuList<>(safePacks(packs), ResourcePackManager.PackInfo::displayName,
                width / 2 - listWidth / 2, 52, listWidth, rowHeight, Math.max(4, Math.min(7, (height - 116) / rowHeight)));
        BaseMenuScreen screen = new BaseMenuScreen("Texture Packs", true, false, done);
        screen.add(list);
        screen.add(new MenuButton("Select", width / 2 - 100, height - 56, 98, 20, () -> {
            ResourcePackManager.PackInfo selected = list.selected();
            if (selected != null) {
                packs.setSelectedPackId(selected.id());
                settings.setSelectedTexturePack(selected.id());
            }
        }));
        screen.add(new MenuButton("Done", width / 2 + 2, height - 56, 98, 20, done));
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

    private static String percent(float value) {
        return Math.round(value * 100.0f) + "%";
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
        return "Render Distance: " + switch (settings.getRenderDistance()) {
            case 0 -> "Far";
            case 1 -> "Normal";
            case 2 -> "Short";
            default -> "Tiny";
        };
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
        return value == 0 ? "Auto" : Integer.toString(value);
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
