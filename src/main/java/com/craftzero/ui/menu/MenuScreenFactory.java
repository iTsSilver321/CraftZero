package com.craftzero.ui.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds Release 1.0-style menu screen models with callbacks for later renderer
 * and game integration.
 */
public final class MenuScreenFactory {

    private static final GuiLayout LAYOUT = GuiLayout.logical(GuiLayout.BASE_WIDTH, GuiLayout.BASE_HEIGHT);

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
        List<Rect> rows = LAYOUT.centeredVerticalStack(200, 20, 4, 128, 4);
        List<MenuComponent> components = List.of(
                new MenuButton("resume", "Back to Game", rows.get(0), this::resumeGame),
                new MenuButton("options", "Options...", rows.get(1), () -> navigation.push(options())),
                new MenuButton("texture-packs", "Texture Packs", rows.get(2), () -> navigation.push(texturePacks())),
                new MenuButton("save-and-quit", "Save and Quit to Title", rows.get(3), this::saveAndQuitToTitle));
        return new MenuScreen(MenuScreenIds.PAUSE, "Game menu", components, true, true, () -> {
            resumeGame();
            return true;
        }, MenuScreen.Background.NONE);
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
                new MenuButton("done", "Done", LAYOUT.centeredButton(204), navigation::back));
        return new MenuScreen(MenuScreenIds.OPTIONS, "Options", components);
    }

    public Screen videoOptions() {
        VideoSettingsModel video = content.videoSettings();
        MenuButton graphics = new MenuButton("graphics", graphicsLabel(video), LAYOUT.centeredButton(72), null);
        graphics.setAction(() -> {
            video.setGraphicsMode(video.graphicsMode().next());
            graphics.setLabel(graphicsLabel(video));
            callbacks.videoSettingsChanged(video.copy());
        });

        MenuButton renderDistance = new MenuButton("render-distance", renderDistanceLabel(video), LAYOUT.centeredButton(96),
                null);
        renderDistance.setAction(() -> {
            video.setRenderDistance(video.renderDistance().next());
            renderDistance.setLabel(renderDistanceLabel(video));
            callbacks.videoSettingsChanged(video.copy());
        });

        MenuSlider guiScale = new MenuSlider("gui-scale", "GUI Scale", LAYOUT.centeredButton(120), 0.0, 4.0,
                video.guiScale(), 1.0, value -> {
                    video.setGuiScale((int) Math.round(value));
                    callbacks.videoSettingsChanged(video.copy());
                });
        guiScale.setFormatter(value -> ((int) Math.round(value)) == 0 ? "Auto" : Integer.toString((int) Math.round(value)));

        List<MenuComponent> components = List.of(
                graphics,
                renderDistance,
                guiScale,
                new MenuButton("done", "Done", LAYOUT.centeredButton(180), navigation::back));
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
        return death("You died!");
    }

    public Screen death(String message) {
        List<MenuComponent> components = List.of(
                new MenuButton("respawn", "Respawn", LAYOUT.centeredButton(124), () -> {
                    callbacks.respawn();
                    navigation.pop();
                }),
                new MenuButton("title", "Title Menu", LAYOUT.centeredButton(148), this::saveAndQuitToTitle));
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

    private static String invertMouseLabel(SettingsModel settings) {
        return "Invert Mouse: " + (settings.invertMouse() ? "ON" : "OFF");
    }

    private static String graphicsLabel(VideoSettingsModel video) {
        return "Graphics: " + video.graphicsMode().displayName();
    }

    private static String renderDistanceLabel(VideoSettingsModel video) {
        return "Render Distance: " + video.renderDistance().displayName();
    }

    private static String gameModeLabel(GameMode gameMode) {
        return "Game Mode: " + gameMode.displayName();
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
        private SettingsModel settings = new SettingsModel();
        private VideoSettingsModel videoSettings = new VideoSettingsModel();

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

        public SettingsModel() {
        }

        public SettingsModel(double musicVolume, double soundVolume, boolean invertMouse) {
            setMusicVolume(musicVolume);
            setSoundVolume(soundVolume);
            this.invertMouse = invertMouse;
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

        public SettingsModel copy() {
            return new SettingsModel(musicVolume, soundVolume, invertMouse);
        }
    }

    public static final class VideoSettingsModel {
        private GraphicsMode graphicsMode = GraphicsMode.FANCY;
        private RenderDistance renderDistance = RenderDistance.NORMAL;
        private int guiScale;

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

        public int guiScale() {
            return guiScale;
        }

        public void setGuiScale(int guiScale) {
            this.guiScale = Math.max(0, Math.min(4, guiScale));
        }

        public VideoSettingsModel copy() {
            VideoSettingsModel copy = new VideoSettingsModel();
            copy.setGraphicsMode(graphicsMode);
            copy.setRenderDistance(renderDistance);
            copy.setGuiScale(guiScale);
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
