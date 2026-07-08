package com.craftzero.ui.menu;

import com.craftzero.main.PlayerStatistics;
import com.craftzero.progression.AchievementTracker;
import com.craftzero.progression.AchievementType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds Release 1.0-style menu screen models with callbacks for later renderer
 * and game integration.
 */
public final class MenuScreenFactory {

    private static final GuiLayout LAYOUT = GuiLayout.logical(GuiLayout.BASE_WIDTH, GuiLayout.BASE_HEIGHT);
    private static final String DEFAULT_LANGUAGE_CODE = "en_US";
    private static final String DEFAULT_LANGUAGE_NAME = "English (US)";

    private final MenuNavigation navigation;
    private final Callbacks callbacks;
    private final Content content;

    public MenuScreenFactory(MenuNavigation navigation, Callbacks callbacks) {
        this(navigation, callbacks, new Content());
    }

    public MenuScreenFactory(MenuNavigation navigation, Callbacks callbacks, Content content) {
        this.navigation = Objects.requireNonNull(navigation, "navigation");
        this.callbacks = callbacks == null ? new Callbacks() {
        } : callbacks;
        this.content = content == null ? new Content() : content;
    }

    public Screen title() {
        List<Rect> rows = LAYOUT.centeredVerticalStack(200, 20, 5, 150, 4);
        List<MenuComponent> components = List.of(
                new MenuButton("singleplayer", "Singleplayer", rows.get(0), () -> navigation.push(worldSelect())),
                new MenuButton("multiplayer", "Multiplayer", rows.get(1), () -> navigation.push(multiplayer())),
                new MenuButton("texture-packs", "Texture Packs", rows.get(2), () -> navigation.push(texturePacks())),
                new MenuButton("options", "Options...", rows.get(3), () -> navigation.push(options())),
                new MenuButton("quit", "Quit Game", rows.get(4), callbacks::quitGame));
        return new MenuScreen(MenuScreenIds.TITLE, "Minecraft", components, false, false, () -> false,
                MenuScreen.Background.PANORAMA);
    }

    public Screen pause() {
        int centerX = GuiLayout.BASE_WIDTH / 2;
        int y = GuiLayout.BASE_HEIGHT / 4 + 48;
        List<MenuComponent> components = List.of(
                new MenuButton("resume", "Back to Game", new Rect(centerX - 100, y, 200, 20), this::resumeGame),
                new MenuButton("achievements", "Achievements", new Rect(centerX - 100, y + 24, 98, 20),
                        () -> navigation.push(achievements())),
                new MenuButton("statistics", "Statistics", new Rect(centerX + 2, y + 24, 98, 20),
                        () -> navigation.push(statistics())),
                new MenuButton("options", "Options...", new Rect(centerX - 100, y + 60, 200, 20),
                        () -> navigation.push(options())),
                new MenuButton("save-and-quit", "Save and Quit to Title", new Rect(centerX - 100, y + 96, 200, 20),
                        this::saveAndQuitToTitle));
        return new MenuScreen(MenuScreenIds.PAUSE, "Game menu", components, true, true, () -> {
            resumeGame();
            return true;
        }, MenuScreen.Background.NONE);
    }

    public Screen achievements() {
        int width = GuiLayout.BASE_WIDTH;
        int height = GuiLayout.BASE_HEIGHT;
        int centerX = width / 2;
        AchievementTracker tracker = content.achievementTracker();
        AchievementType[] achievements = AchievementType.values();
        int unlocked = tracker.unlockedAchievements().size();
        List<MenuComponent> components = new ArrayList<>();
        components.add(MenuLabel.centered("achievement-summary",
                "Unlocked: " + unlocked + " / " + achievements.length,
                centerX, 54, Math.min(320, Math.max(180, width - 40))).color(1.0f, 1.0f, 0.63f, 1.0f));

        int detailTitleY = Math.max(78, height - 72);
        int detailWidth = Math.max(100, width - 48);
        AchievementTreeComponent tree = new AchievementTreeComponent("achievement-tree",
                new Rect(24, 74, detailWidth, Math.max(64, detailTitleY - 84)), tracker);
        components.add(tree);

        AchievementType selected = tree.detailAchievement();
        MenuLabel selectedTitle = new MenuLabel("achievement-selected-title",
                tree.titleLine(selected), new Rect(24, detailTitleY, detailWidth, 10))
                .color(tree.colorFor(selected));
        MenuLabel selectedDescription = new MenuLabel("achievement-selected-description",
                MenuScreens.fitMenuText(tree.detailDescription(selected), detailWidth),
                new Rect(24, detailTitleY + 14, detailWidth, 10))
                .color(0.76f, 0.76f, 0.76f, 1.0f);
        tree.onDetailChanged(detail -> {
            selectedTitle.text(tree.titleLine(detail)).color(tree.colorFor(detail));
            selectedDescription.text(MenuScreens.fitMenuText(tree.detailDescription(detail), detailWidth));
        });
        components.add(selectedTitle);
        components.add(selectedDescription);
        components.add(new MenuButton("done", "Done", LAYOUT.centeredButton(height - 28), navigation::back));
        return new MenuScreen(MenuScreenIds.ACHIEVEMENTS, "Achievements", components);
    }

    public Screen statistics() {
        int width = GuiLayout.BASE_WIDTH;
        int height = GuiLayout.BASE_HEIGHT;
        int centerX = width / 2;
        PlayerStatistics stats = content.statistics();
        List<MenuComponent> components = new ArrayList<>();
        MenuLabel summary = MenuLabel.centered("statistics-summary", "General", centerX,
                MenuScreens.STATISTICS_SUMMARY_Y, MenuScreens.statisticsSummaryWidth(width))
                .color(1.0f, 1.0f, 0.63f, 1.0f);
        components.add(summary);

        MenuButton generalTab = new MenuButton("statistics-tab-general", "General",
                MenuScreens.statisticsTabBounds(centerX, 0), null);
        MenuButton blocksTab = new MenuButton("statistics-tab-blocks", "Blocks",
                MenuScreens.statisticsTabBounds(centerX, 1), null);
        MenuButton itemsTab = new MenuButton("statistics-tab-items", "Items",
                MenuScreens.statisticsTabBounds(centerX, 2), null);
        components.add(generalTab);
        components.add(blocksTab);
        components.add(itemsTab);

        Rect listBounds = MenuScreens.statisticsListBounds(width, height);
        int tableWidth = listBounds.width();
        MenuList<String> generalList = new MenuList<>("statistics-general-list",
                listBounds, MenuScreens.STATISTICS_ROW_HEIGHT,
                MenuScreens.statisticRows(stats, tableWidth),
                row -> row);
        MenuList<String> blockList = new MenuList<>("statistics-block-list",
                listBounds, MenuScreens.STATISTICS_ROW_HEIGHT,
                MenuScreens.blockStatisticRows(stats, tableWidth), row -> row);
        MenuList<String> itemList = new MenuList<>("statistics-item-list",
                listBounds, MenuScreens.STATISTICS_ROW_HEIGHT,
                MenuScreens.itemStatisticRows(stats, tableWidth), row -> row);
        blockList.setVisible(false);
        itemList.setVisible(false);
        components.add(generalList);
        components.add(blockList);
        components.add(itemList);

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

        components.add(new MenuButton("done", "Done", LAYOUT.centeredButton(height - 28), navigation::back));
        return new MenuScreen(MenuScreenIds.STATISTICS, "Statistics", components);
    }

    public Screen options() {
        SettingsModel settings = content.settings();
        MenuSlider music = new MenuSlider("music", "Music", LAYOUT.centeredButton(72), 0.0, 1.0,
                settings.musicVolume(), 0.01, value -> {
                    settings.setMusicVolume(value);
                    callbacks.settingsChanged(settings.copy());
                });
        MenuSlider sound = new MenuSlider("sound", "Sound", LAYOUT.centeredButton(96), 0.0, 1.0,
                settings.soundVolume(), 0.01, value -> {
                    settings.setSoundVolume(value);
                    callbacks.settingsChanged(settings.copy());
                });
        MenuButton invert = new MenuButton("invert-mouse", invertMouseLabel(settings), LAYOUT.centeredButton(120), () -> {
            settings.setInvertMouse(!settings.invertMouse());
            callbacks.settingsChanged(settings.copy());
        });
        invert.setAction(() -> {
            settings.setInvertMouse(!settings.invertMouse());
            invert.setLabel(invertMouseLabel(settings));
            callbacks.settingsChanged(settings.copy());
        });

        List<MenuComponent> components = List.of(
                music,
                sound,
                invert,
                new MenuButton("video", "Video Settings...", LAYOUT.centeredButton(148),
                        () -> navigation.push(videoOptions())),
                new MenuButton("controls", "Controls...", LAYOUT.centeredButton(172), () -> navigation.push(controls())),
                new MenuButton("language", "Language...", LAYOUT.centeredButton(196),
                        () -> navigation.push(language())),
                new MenuButton("done", "Done", LAYOUT.centeredButton(220), navigation::back));
        return new MenuScreen(MenuScreenIds.OPTIONS, "Options", components);
    }

    public Screen language() {
        SettingsModel settings = content.settings();
        List<LanguageEntry> languages = normalizedLanguages(content.languages());
        int centerX = GuiLayout.BASE_WIDTH / 2;
        MenuLabel current = MenuLabel.centered("language-current",
                currentLanguageLabel(settings, languages), centerX, 54, 240)
                .color(1.0f, 1.0f, 0.63f, 1.0f);
        MenuList<LanguageEntry> languageList = new MenuList<>("languages",
                new Rect(centerX - 120, 74, 240, 96), 16, languages,
                entry -> languageRowLabel(settings, entry));
        int selectedIndex = selectedLanguageIndex(languages, settings.language());
        if (selectedIndex >= 0) {
            languageList.setSelectedIndex(selectedIndex);
        }
        MenuButton select = new MenuButton("select-language", "Select", LAYOUT.centeredButton(180), null);
        select.setAction(() -> applyLanguageSelection(languageList, settings, current));
        languageList.setOnActivated(entry -> applyLanguageSelection(languageList, settings, current));

        List<MenuComponent> components = List.of(
                current,
                languageList,
                select,
                new MenuButton("done", "Done", LAYOUT.centeredButton(212), navigation::back));
        return new MenuScreen(MenuScreenIds.LANGUAGE, "Language", components);
    }

    public Screen videoOptions() {
        VideoSettingsModel video = content.videoSettings();
        int leftX = 10;
        int rightX = 160;
        int width = 150;
        int y = 42;
        int row = 24;
        MenuButton graphics = new MenuButton("graphics", graphicsLabel(video), new Rect(leftX, y, width, 20), null);
        graphics.setAction(() -> {
            video.setGraphicsMode(video.graphicsMode().next());
            graphics.setLabel(graphicsLabel(video));
            callbacks.videoSettingsChanged(video.copy());
        });

        MenuButton renderDistance = new MenuButton("render-distance", renderDistanceLabel(video),
                new Rect(rightX, y, width, 20),
                null);
        renderDistance.setAction(() -> {
            video.setRenderDistance(video.renderDistance().next());
            renderDistance.setLabel(renderDistanceLabel(video));
            callbacks.videoSettingsChanged(video.copy());
        });

        MenuButton smoothLighting = new MenuButton("smooth-lighting", smoothLightingLabel(video),
                new Rect(leftX, y + row, width, 20), null);
        smoothLighting.setAction(() -> {
            video.setSmoothLighting(!video.smoothLighting());
            smoothLighting.setLabel(smoothLightingLabel(video));
            callbacks.videoSettingsChanged(video.copy());
        });

        MenuButton performance = new MenuButton("performance", performanceLabel(video),
                new Rect(rightX, y + row, width, 20), null);
        performance.setAction(() -> {
            video.setFramerateLimit(nextFrameLimit(video.framerateLimit()));
            performance.setLabel(performanceLabel(video));
            callbacks.videoSettingsChanged(video.copy());
        });

        MenuButton anaglyph = new MenuButton("anaglyph", anaglyphLabel(video),
                new Rect(leftX, y + row * 2, width, 20), null);
        anaglyph.setAction(() -> {
            video.setAnaglyph3d(!video.anaglyph3d());
            anaglyph.setLabel(anaglyphLabel(video));
            callbacks.videoSettingsChanged(video.copy());
        });

        MenuButton bobbing = new MenuButton("view-bobbing", viewBobbingLabel(video),
                new Rect(rightX, y + row * 2, width, 20), null);
        bobbing.setAction(() -> {
            video.setViewBobbing(!video.viewBobbing());
            bobbing.setLabel(viewBobbingLabel(video));
            callbacks.videoSettingsChanged(video.copy());
        });

        MenuButton guiScale = new MenuButton("gui-scale", guiScaleButtonLabel(video),
                new Rect(leftX, y + row * 3, width, 20), null);
        guiScale.setAction(() -> {
            video.setGuiScale((video.guiScale() + 1) % 4);
            guiScale.setLabel(guiScaleButtonLabel(video));
            callbacks.videoSettingsChanged(video.copy());
        });

        MenuButton advancedOpenGl = new MenuButton("advanced-opengl", advancedOpenGlLabel(video),
                new Rect(rightX, y + row * 3, width, 20), null);
        advancedOpenGl.setAction(() -> {
            video.setAdvancedOpenGl(!video.advancedOpenGl());
            advancedOpenGl.setLabel(advancedOpenGlLabel(video));
            callbacks.videoSettingsChanged(video.copy());
        });

        MenuSlider brightness = new MenuSlider("brightness", "Brightness",
                new Rect(leftX, y + row * 4, width, 20), 0.0, 1.0,
                video.brightness(), 0.01, value -> {
                    video.setBrightness(value);
                    callbacks.videoSettingsChanged(video.copy());
                });
        brightness.setFormatter(MenuScreenFactory::brightnessLabel);

        MenuButton clouds = new MenuButton("clouds", cloudsLabel(video),
                new Rect(rightX, y + row * 4, width, 20), null);
        clouds.setAction(() -> {
            video.setClouds(!video.clouds());
            clouds.setLabel(cloudsLabel(video));
            callbacks.videoSettingsChanged(video.copy());
        });

        MenuButton particles = new MenuButton("particles", particlesLabel(video),
                new Rect(leftX, y + row * 5, width, 20), null);
        particles.setAction(() -> {
            video.setParticles((video.particles() + 1) % 3);
            particles.setLabel(particlesLabel(video));
            callbacks.videoSettingsChanged(video.copy());
        });

        List<MenuComponent> components = List.of(
                graphics,
                renderDistance,
                smoothLighting,
                performance,
                anaglyph,
                bobbing,
                guiScale,
                advancedOpenGl,
                brightness,
                clouds,
                particles,
                new MenuButton("done", "Done", LAYOUT.centeredButton(212), navigation::back));
        return new MenuScreen(MenuScreenIds.VIDEO_OPTIONS, "Video Settings", components);
    }

    public Screen controls() {
        MenuList<ControlBinding> bindings = new MenuList<>("bindings", new Rect(40, 48, 240, 96), 18,
                content.controlBindings(), binding -> binding.action() + ": " + binding.keyName());
        List<MenuComponent> components = List.of(
                bindings,
                new MenuButton("reset", "Reset Keys", LAYOUT.centeredButton(160), callbacks::resetControls),
                new MenuButton("done", "Done", LAYOUT.centeredButton(204), navigation::back));
        return new MenuScreen(MenuScreenIds.CONTROLS, "Controls", components);
    }

    public Screen worldSelect() {
        MenuList<WorldEntry> worlds = new MenuList<>("worlds", new Rect(32, 48, 256, 104), 20, content.worlds(),
                WorldEntry::displayName);
        MenuButton play = new MenuButton("play", "Play Selected World", LAYOUT.centeredButton(164),
                () -> worlds.selectedItem().ifPresent(callbacks::startWorld));
        play.setEnabled(false);
        MenuButton delete = new MenuButton("delete", "Delete", new Rect(162, 188, 98, 20),
                () -> worlds.selectedItem().ifPresent(callbacks::deleteWorld));
        delete.setEnabled(false);
        worlds.setOnSelectionChanged(world -> {
            play.setEnabled(true);
            delete.setEnabled(true);
        });
        worlds.setOnActivated(callbacks::startWorld);

        List<MenuComponent> components = List.of(
                worlds,
                play,
                new MenuButton("create", "Create New World", new Rect(60, 188, 98, 20),
                        () -> navigation.push(createWorld())),
                delete,
                new MenuButton("back", "Back", LAYOUT.centeredButton(212), navigation::back));
        return new MenuScreen(MenuScreenIds.WORLD_SELECT, "Select World", components);
    }

    public Screen createWorld() {
        CreateWorldModel create = new CreateWorldModel();
        TextField name = new TextField("world-name", new Rect(60, 66, 200, 20), create.name(), 32);
        TextField seed = new TextField("seed", new Rect(60, 112, 200, 20), create.seed(), 64);
        name.setOnChanged(create::setName);
        seed.setOnChanged(create::setSeed);

        MenuButton mode = new MenuButton("game-mode", gameModeLabel(create.gameMode()), LAYOUT.centeredButton(144), null);
        mode.setAction(() -> {
            create.setGameMode(create.gameMode().next());
            mode.setLabel(gameModeLabel(create.gameMode()));
        });

        List<MenuComponent> components = List.of(
                name,
                seed,
                mode,
                new MenuButton("create", "Create New World", LAYOUT.centeredButton(176), () -> {
                    CreateWorldRequest request = new CreateWorldRequest(name.text().isBlank() ? "New World" : name.text(),
                            seed.text(), create.gameMode());
                    callbacks.createWorld(request);
                }),
                new MenuButton("back", "Cancel", LAYOUT.centeredButton(204), navigation::back));
        return new MenuScreen(MenuScreenIds.CREATE_WORLD, "Create New World", components);
    }

    public Screen texturePacks() {
        MenuList<TexturePackEntry> packs = new MenuList<>("texture-packs", new Rect(32, 48, 256, 116), 20,
                content.texturePacks(), TexturePackEntry::displayName);
        MenuButton select = new MenuButton("select", "Select", new Rect(60, 184, 98, 20),
                () -> packs.selectedItem().ifPresent(callbacks::selectTexturePack));
        select.setEnabled(false);
        packs.setOnSelectionChanged(pack -> select.setEnabled(true));
        packs.setOnActivated(callbacks::selectTexturePack);
        List<MenuComponent> components = List.of(
                packs,
                select,
                new MenuButton("open-folder", "Open Folder", new Rect(162, 184, 98, 20), callbacks::openTexturePackFolder),
                new MenuButton("done", "Done", LAYOUT.centeredButton(212), navigation::back));
        return new MenuScreen(MenuScreenIds.TEXTURE_PACKS, "Texture Packs", components);
    }

    public Screen multiplayer() {
        TextField serverAddress = new TextField("server-address", new Rect(60, 68, 200, 20), "", 128);
        serverAddress.setOnSubmitted(address -> {
            if (!address.isBlank()) {
                callbacks.joinServer(address);
            }
        });

        MenuList<ServerEntry> servers = new MenuList<>("servers", new Rect(60, 100, 200, 64), 20, content.servers(),
                ServerEntry::displayName);
        servers.setOnSelectionChanged(server -> serverAddress.setText(server.address()));
        servers.setOnActivated(server -> callbacks.joinServer(server.address()));

        List<MenuComponent> components = List.of(
                serverAddress,
                servers,
                new MenuButton("join", "Join Server", new Rect(60, 176, 98, 20), () -> {
                    if (!serverAddress.text().isBlank()) {
                        callbacks.joinServer(serverAddress.text());
                    }
                }),
                new MenuButton("direct-connect", "Direct Connect", new Rect(162, 176, 98, 20),
                        () -> serverAddress.setFocused(true)),
                new MenuButton("back", "Back", LAYOUT.centeredButton(208), navigation::back));
        return new MenuScreen(MenuScreenIds.MULTIPLAYER, "Multiplayer", components);
    }

    public Screen death() {
        return death(content.hardcoreDeath() ? "Game over!" : "You died!");
    }

    public Screen death(String message) {
        List<MenuComponent> components = new ArrayList<>();
        components.add(MenuLabel.centered("score", com.craftzero.graphics.DeathScreen.scoreText(content.deathScore()),
                GuiLayout.BASE_WIDTH / 2, 96, 200)
                .color(0.85f, 0.85f, 0.85f, 1.0f));
        if (content.hardcoreDeath()) {
            components.add(MenuLabel.centered("hardcore-message", "You cannot respawn in hardcore mode!",
                    GuiLayout.BASE_WIDTH / 2, 122, 240));
            components.add(new MenuButton("delete-world", "Delete World", LAYOUT.centeredButton(148),
                    this::deleteHardcoreWorldAndReturnToTitle));
        } else {
            components.add(new MenuButton("respawn", "Respawn", LAYOUT.centeredButton(124), () -> {
                callbacks.respawn();
                navigation.pop();
            }));
            components.add(new MenuButton("title", "Title Menu", LAYOUT.centeredButton(148),
                    this::saveAndQuitToTitle));
        }
        return new MenuScreen(MenuScreenIds.DEATH, message, components, true, false, () -> true,
                MenuScreen.Background.NONE);
    }

    private void resumeGame() {
        callbacks.resumeGame();
        navigation.pop();
    }

    private void saveAndQuitToTitle() {
        callbacks.saveAndQuitToTitle();
        navigation.clear();
        navigation.push(title());
    }

    private void deleteHardcoreWorldAndReturnToTitle() {
        callbacks.deleteHardcoreWorld();
        navigation.clear();
        navigation.push(title());
    }

    private static String invertMouseLabel(SettingsModel settings) {
        return "Invert Mouse: " + (settings.invertMouse() ? "ON" : "OFF");
    }

    private static String graphicsLabel(VideoSettingsModel video) {
        return "Graphics: " + video.graphicsMode().displayName();
    }

    private static String renderDistanceLabel(VideoSettingsModel video) {
        return "Render Distance: " + video.renderDistance().displayName();
    }

    private static String guiScaleLabel(int value) {
        return switch (value) {
            case 0 -> "Auto";
            case 1 -> "Small";
            case 2 -> "Normal";
            default -> "Large";
        };
    }

    private static String guiScaleButtonLabel(VideoSettingsModel video) {
        return "GUI Scale: " + guiScaleLabel(video.guiScale());
    }

    private static String smoothLightingLabel(VideoSettingsModel video) {
        return "Smooth Lighting: " + onOff(video.smoothLighting());
    }

    private static String performanceLabel(VideoSettingsModel video) {
        return "Performance: " + switch (video.framerateLimit()) {
            case 0 -> "Max FPS";
            case 40 -> "Power saver";
            default -> "Balanced";
        };
    }

    private static String anaglyphLabel(VideoSettingsModel video) {
        return "3D Anaglyph: " + onOff(video.anaglyph3d());
    }

    private static String viewBobbingLabel(VideoSettingsModel video) {
        return "View Bobbing: " + onOff(video.viewBobbing());
    }

    private static String advancedOpenGlLabel(VideoSettingsModel video) {
        return "Advanced OpenGL: " + onOff(video.advancedOpenGl());
    }

    private static String brightnessLabel(double value) {
        if (value <= 0.0) {
            return "Moody";
        }
        if (value >= 1.0) {
            return "Bright";
        }
        return Math.round(value * 100.0) + "%";
    }

    private static String cloudsLabel(VideoSettingsModel video) {
        return "Clouds: " + onOff(video.clouds());
    }

    private static String particlesLabel(VideoSettingsModel video) {
        return "Particles: " + switch (video.particles()) {
            case 0 -> "All";
            case 1 -> "Decreased";
            default -> "Minimal";
        };
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static int nextFrameLimit(int framerateLimit) {
        return framerateLimit == 120 ? 40 : framerateLimit == 40 ? 0 : 120;
    }

    private static String gameModeLabel(GameMode gameMode) {
        return "Game Mode: " + gameMode.displayName();
    }

    private void applyLanguageSelection(MenuList<LanguageEntry> languageList, SettingsModel settings,
            MenuLabel current) {
        languageList.selectedItem().ifPresent(entry -> {
            settings.setLanguage(entry.code());
            current.text(currentLanguageLabel(settings, languageList.items()));
            callbacks.settingsChanged(settings.copy());
        });
    }

    private static List<LanguageEntry> normalizedLanguages(List<LanguageEntry> languages) {
        return languages == null || languages.isEmpty()
                ? List.of(new LanguageEntry(DEFAULT_LANGUAGE_CODE, DEFAULT_LANGUAGE_NAME))
                : languages;
    }

    private static int selectedLanguageIndex(List<LanguageEntry> languages, String code) {
        String selected = normalizeLanguageCode(code);
        for (int i = 0; i < languages.size(); i++) {
            if (languages.get(i).code().equals(selected)) {
                return i;
            }
        }
        return languages.isEmpty() ? -1 : 0;
    }

    private static String currentLanguageLabel(SettingsModel settings, List<LanguageEntry> languages) {
        return "Language: " + languageName(languages, settings.language());
    }

    private static String languageRowLabel(SettingsModel settings, LanguageEntry entry) {
        return entry.displayName() + (entry.code().equals(settings.language()) ? " *" : "");
    }

    private static String languageName(List<LanguageEntry> languages, String code) {
        String selected = normalizeLanguageCode(code);
        for (LanguageEntry entry : languages) {
            if (entry.code().equals(selected)) {
                return entry.displayName();
            }
        }
        return selected;
    }

    private static String normalizeLanguageCode(String language) {
        String cleaned = language == null ? "" : language.trim();
        return cleaned.isEmpty() ? DEFAULT_LANGUAGE_CODE : cleaned;
    }

    public interface Callbacks {
        default void startWorld(WorldEntry world) {
        }

        default void deleteWorld(WorldEntry world) {
        }

        default void createWorld(CreateWorldRequest request) {
        }

        default void joinServer(String address) {
        }

        default void selectTexturePack(TexturePackEntry pack) {
        }

        default void openTexturePackFolder() {
        }

        default void resumeGame() {
        }

        default void saveAndQuitToTitle() {
        }

        default void deleteHardcoreWorld() {
        }

        default void quitGame() {
        }

        default void respawn() {
        }

        default void settingsChanged(SettingsModel settings) {
        }

        default void videoSettingsChanged(VideoSettingsModel videoSettings) {
        }

        default void resetControls() {
        }
    }

    public static final class Content {
        private final List<WorldEntry> worlds = new ArrayList<>();
        private final List<TexturePackEntry> texturePacks = new ArrayList<>();
        private final List<ServerEntry> servers = new ArrayList<>();
        private final List<ControlBinding> controlBindings = new ArrayList<>();
        private final List<LanguageEntry> languages = new ArrayList<>(
                List.of(new LanguageEntry(DEFAULT_LANGUAGE_CODE, DEFAULT_LANGUAGE_NAME)));
        private SettingsModel settings = new SettingsModel();
        private VideoSettingsModel videoSettings = new VideoSettingsModel();
        private AchievementTracker achievementTracker = new AchievementTracker();
        private PlayerStatistics statistics = new PlayerStatistics();
        private int deathScore;
        private boolean hardcoreDeath;

        public List<WorldEntry> worlds() {
            return List.copyOf(worlds);
        }

        public Content setWorlds(List<WorldEntry> worlds) {
            this.worlds.clear();
            this.worlds.addAll(Objects.requireNonNull(worlds, "worlds"));
            return this;
        }

        public List<TexturePackEntry> texturePacks() {
            return List.copyOf(texturePacks);
        }

        public Content setTexturePacks(List<TexturePackEntry> texturePacks) {
            this.texturePacks.clear();
            this.texturePacks.addAll(Objects.requireNonNull(texturePacks, "texturePacks"));
            return this;
        }

        public List<ServerEntry> servers() {
            return List.copyOf(servers);
        }

        public Content setServers(List<ServerEntry> servers) {
            this.servers.clear();
            this.servers.addAll(Objects.requireNonNull(servers, "servers"));
            return this;
        }

        public List<ControlBinding> controlBindings() {
            return List.copyOf(controlBindings);
        }

        public Content setControlBindings(List<ControlBinding> controlBindings) {
            this.controlBindings.clear();
            this.controlBindings.addAll(Objects.requireNonNull(controlBindings, "controlBindings"));
            return this;
        }

        public List<LanguageEntry> languages() {
            return List.copyOf(languages);
        }

        public Content setLanguages(List<LanguageEntry> languages) {
            this.languages.clear();
            this.languages.addAll(Objects.requireNonNull(languages, "languages"));
            return this;
        }

        public SettingsModel settings() {
            return settings;
        }

        public Content setSettings(SettingsModel settings) {
            this.settings = Objects.requireNonNull(settings, "settings");
            return this;
        }

        public VideoSettingsModel videoSettings() {
            return videoSettings;
        }

        public Content setVideoSettings(VideoSettingsModel videoSettings) {
            this.videoSettings = Objects.requireNonNull(videoSettings, "videoSettings");
            return this;
        }

        public AchievementTracker achievementTracker() {
            return achievementTracker;
        }

        public Content setAchievementTracker(AchievementTracker achievementTracker) {
            this.achievementTracker = Objects.requireNonNull(achievementTracker, "achievementTracker");
            return this;
        }

        public PlayerStatistics statistics() {
            return statistics;
        }

        public Content setStatistics(PlayerStatistics statistics) {
            this.statistics = Objects.requireNonNull(statistics, "statistics");
            return this;
        }

        public int deathScore() {
            return deathScore;
        }

        public Content setDeathScore(int deathScore) {
            this.deathScore = Math.max(0, deathScore);
            return this;
        }

        public boolean hardcoreDeath() {
            return hardcoreDeath;
        }

        public Content setHardcoreDeath(boolean hardcoreDeath) {
            this.hardcoreDeath = hardcoreDeath;
            return this;
        }
    }

    public record WorldEntry(String id, String displayName, String lastPlayed, String versionName, boolean locked) {
        public WorldEntry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(lastPlayed, "lastPlayed");
            Objects.requireNonNull(versionName, "versionName");
        }
    }

    public record TexturePackEntry(String id, String displayName, String description, boolean selected) {
        public TexturePackEntry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(description, "description");
        }
    }

    public record ServerEntry(String id, String displayName, String address) {
        public ServerEntry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(displayName, "displayName");
            Objects.requireNonNull(address, "address");
        }
    }

    public record ControlBinding(String action, String keyName) {
        public ControlBinding {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(keyName, "keyName");
        }
    }

    public record LanguageEntry(String code, String displayName) {
        public LanguageEntry {
            code = normalizeLanguageCode(code);
            Objects.requireNonNull(displayName, "displayName");
        }
    }

    public record CreateWorldRequest(String name, String seed, GameMode gameMode) {
        public CreateWorldRequest {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(seed, "seed");
            Objects.requireNonNull(gameMode, "gameMode");
        }
    }

    public static final class SettingsModel {
        private double musicVolume = 1.0;
        private double soundVolume = 1.0;
        private boolean invertMouse;
        private String language = DEFAULT_LANGUAGE_CODE;

        public SettingsModel() {
        }

        public SettingsModel(double musicVolume, double soundVolume, boolean invertMouse) {
            this(musicVolume, soundVolume, invertMouse, DEFAULT_LANGUAGE_CODE);
        }

        public SettingsModel(double musicVolume, double soundVolume, boolean invertMouse, String language) {
            setMusicVolume(musicVolume);
            setSoundVolume(soundVolume);
            this.invertMouse = invertMouse;
            setLanguage(language);
        }

        public double musicVolume() {
            return musicVolume;
        }

        public void setMusicVolume(double musicVolume) {
            this.musicVolume = clamp01(musicVolume);
        }

        public double soundVolume() {
            return soundVolume;
        }

        public void setSoundVolume(double soundVolume) {
            this.soundVolume = clamp01(soundVolume);
        }

        public boolean invertMouse() {
            return invertMouse;
        }

        public void setInvertMouse(boolean invertMouse) {
            this.invertMouse = invertMouse;
        }

        public String language() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = normalizeLanguageCode(language);
        }

        public SettingsModel copy() {
            return new SettingsModel(musicVolume, soundVolume, invertMouse, language);
        }
    }

    public static final class VideoSettingsModel {
        private GraphicsMode graphicsMode = GraphicsMode.FANCY;
        private RenderDistance renderDistance = RenderDistance.NORMAL;
        private boolean smoothLighting = true;
        private int framerateLimit = 120;
        private boolean anaglyph3d;
        private boolean viewBobbing = true;
        private int guiScale;
        private boolean advancedOpenGl;
        private double brightness;
        private boolean clouds = true;
        private int particles;

        public GraphicsMode graphicsMode() {
            return graphicsMode;
        }

        public void setGraphicsMode(GraphicsMode graphicsMode) {
            this.graphicsMode = Objects.requireNonNull(graphicsMode, "graphicsMode");
        }

        public RenderDistance renderDistance() {
            return renderDistance;
        }

        public void setRenderDistance(RenderDistance renderDistance) {
            this.renderDistance = Objects.requireNonNull(renderDistance, "renderDistance");
        }

        public boolean smoothLighting() {
            return smoothLighting;
        }

        public void setSmoothLighting(boolean smoothLighting) {
            this.smoothLighting = smoothLighting;
        }

        public int framerateLimit() {
            return framerateLimit;
        }

        public void setFramerateLimit(int framerateLimit) {
            this.framerateLimit = framerateLimit <= 0 ? 0 : framerateLimit <= 40 ? 40 : 120;
        }

        public boolean anaglyph3d() {
            return anaglyph3d;
        }

        public void setAnaglyph3d(boolean anaglyph3d) {
            this.anaglyph3d = anaglyph3d;
        }

        public boolean viewBobbing() {
            return viewBobbing;
        }

        public void setViewBobbing(boolean viewBobbing) {
            this.viewBobbing = viewBobbing;
        }

        public int guiScale() {
            return guiScale;
        }

        public void setGuiScale(int guiScale) {
            this.guiScale = Math.max(0, Math.min(3, guiScale));
        }

        public boolean advancedOpenGl() {
            return advancedOpenGl;
        }

        public void setAdvancedOpenGl(boolean advancedOpenGl) {
            this.advancedOpenGl = advancedOpenGl;
        }

        public double brightness() {
            return brightness;
        }

        public void setBrightness(double brightness) {
            this.brightness = clamp01(brightness);
        }

        public boolean clouds() {
            return clouds;
        }

        public void setClouds(boolean clouds) {
            this.clouds = clouds;
        }

        public int particles() {
            return particles;
        }

        public void setParticles(int particles) {
            this.particles = Math.max(0, Math.min(2, particles));
        }

        public VideoSettingsModel copy() {
            VideoSettingsModel copy = new VideoSettingsModel();
            copy.setGraphicsMode(graphicsMode);
            copy.setRenderDistance(renderDistance);
            copy.setSmoothLighting(smoothLighting);
            copy.setFramerateLimit(framerateLimit);
            copy.setAnaglyph3d(anaglyph3d);
            copy.setViewBobbing(viewBobbing);
            copy.setGuiScale(guiScale);
            copy.setAdvancedOpenGl(advancedOpenGl);
            copy.setBrightness(brightness);
            copy.setClouds(clouds);
            copy.setParticles(particles);
            return copy;
        }
    }

    public static final class CreateWorldModel {
        private String name = "New World";
        private String seed = "";
        private GameMode gameMode = GameMode.SURVIVAL;

        public String name() {
            return name;
        }

        public void setName(String name) {
            this.name = name == null ? "" : name;
        }

        public String seed() {
            return seed;
        }

        public void setSeed(String seed) {
            this.seed = seed == null ? "" : seed;
        }

        public GameMode gameMode() {
            return gameMode;
        }

        public void setGameMode(GameMode gameMode) {
            this.gameMode = Objects.requireNonNull(gameMode, "gameMode");
        }
    }

    public enum GameMode {
        SURVIVAL("Survival"),
        CREATIVE("Creative"),
        HARDCORE("Hardcore");

        private final String displayName;

        GameMode(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }

        public GameMode next() {
            GameMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public enum GraphicsMode {
        FAST("Fast"),
        FANCY("Fancy");

        private final String displayName;

        GraphicsMode(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }

        public GraphicsMode next() {
            return this == FAST ? FANCY : FAST;
        }
    }

    public enum RenderDistance {
        FAR("Far"),
        NORMAL("Normal"),
        SHORT("Short"),
        TINY("Tiny");

        private final String displayName;

        RenderDistance(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }

        public RenderDistance next() {
            RenderDistance[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
