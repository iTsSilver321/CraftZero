package com.craftzero.main;

import com.craftzero.save.SafeFiles;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Backing model for Release 1.0-style options.txt settings.
 */
public final class GameSettings {
    public static final Path DEFAULT_OPTIONS_PATH = Paths.get("options.txt");
    public static final String DEFAULT_PLAYER_NAME = "Player";
    public static final String DEFAULT_TEXTURE_PACK = "Default";
    public static final String DEFAULT_LANGUAGE = "en_US";
    public static final int MIN_RENDER_DISTANCE_CHUNKS = 2;
    public static final int MAX_RENDER_DISTANCE_CHUNKS = 16;
    public static final int DEFAULT_RENDER_DISTANCE_CHUNKS = 8;

    private static final Map<String, KeyBinding> KEY_BINDINGS_BY_OPTION = createKeyBindingLookup();

    private String playerName = DEFAULT_PLAYER_NAME;
    private GameMode gameMode = GameMode.SURVIVAL;
    private Difficulty difficulty = Difficulty.NORMAL;
    private float musicVolume = 1.0f;
    private float soundVolume = 1.0f;
    private boolean invertYMouse;
    private float mouseSensitivity = 0.5f;
    private float fov;
    private float gamma;
    private int renderDistance = DEFAULT_RENDER_DISTANCE_CHUNKS;
    private int guiScale;
    private int particles;
    private int framerateLimit = 120;
    private boolean viewBobbing = true;
    private boolean fancyGraphics = true;
    private boolean smoothLighting = true;
    private boolean clouds = true;
    private boolean fullscreen;
    private boolean vsync;
    private boolean advancedOpenGl;
    private String selectedTexturePack = DEFAULT_TEXTURE_PACK;
    private String lastServer = "";
    private String language = DEFAULT_LANGUAGE;
    private final EnumMap<KeyBinding, Integer> keyBindings = new EnumMap<>(KeyBinding.class);
    private final LinkedHashMap<String, String> unknownOptions = new LinkedHashMap<>();

    private GameSettings() {
        resetKeyBindings();
    }

    public static GameSettings defaults() {
        return new GameSettings();
    }

    public static GameSettings load(Path optionsPath) throws IOException {
        GameSettings settings = defaults();
        if (optionsPath == null || !Files.isRegularFile(optionsPath)) {
            return settings;
        }

        try (Reader reader = Files.newBufferedReader(optionsPath)) {
            for (String rawLine : readLines(reader)) {
                settings.readOptionLine(rawLine);
            }
        }
        return settings;
    }

    public static GameSettings loadOrCreate(Path optionsPath) throws IOException {
        GameSettings settings = load(optionsPath);
        if (optionsPath != null && !Files.exists(optionsPath)) {
            settings.save(optionsPath);
        }
        return settings;
    }

    public void save(Path optionsPath) throws IOException {
        if (optionsPath == null) {
            throw new IllegalArgumentException("optionsPath cannot be null");
        }

        SafeFiles.writeAtomic(optionsPath, writer -> {
            for (String line : toOptionLines()) {
                writer.write(line);
                writer.write(System.lineSeparator());
            }
        }, SafeFiles.BackupPolicy.NONE);
    }

    public List<String> toOptionLines() {
        List<String> lines = new ArrayList<>();
        add(lines, "playerName", playerName);
        add(lines, "gameMode", gameMode.optionName());
        add(lines, "difficulty", difficulty.optionName());
        add(lines, "music", Float.toString(musicVolume));
        add(lines, "sound", Float.toString(soundVolume));
        add(lines, "invertYMouse", Boolean.toString(invertYMouse));
        add(lines, "mouseSensitivity", Float.toString(mouseSensitivity));
        add(lines, "fov", Float.toString(fov));
        add(lines, "gamma", Float.toString(gamma));
        add(lines, "renderDistance", Integer.toString(renderDistance));
        add(lines, "renderDistanceChunks", Integer.toString(renderDistance));
        add(lines, "guiScale", Integer.toString(guiScale));
        add(lines, "particles", Integer.toString(particles));
        add(lines, "framerateLimit", Integer.toString(framerateLimit));
        add(lines, "viewBobbing", Boolean.toString(viewBobbing));
        add(lines, "fancyGraphics", Boolean.toString(fancyGraphics));
        add(lines, "smoothLighting", Boolean.toString(smoothLighting));
        add(lines, "clouds", Boolean.toString(clouds));
        add(lines, "fullscreen", Boolean.toString(fullscreen));
        add(lines, "vsync", Boolean.toString(vsync));
        add(lines, "advancedOpenGL", Boolean.toString(advancedOpenGl));
        add(lines, "texturePack", selectedTexturePack);
        add(lines, "lastServer", lastServer);
        add(lines, "language", language);

        for (KeyBinding binding : KeyBinding.values()) {
            add(lines, binding.optionName(), Integer.toString(getKeyBinding(binding)));
        }

        for (Map.Entry<String, String> entry : unknownOptions.entrySet()) {
            add(lines, entry.getKey(), entry.getValue());
        }
        return lines;
    }

    private void readOptionLine(String rawLine) {
        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }

        int separator = line.indexOf(':');
        if (separator < 0) {
            unknownOptions.put(line, "");
            return;
        }

        String key = line.substring(0, separator).trim();
        String value = line.substring(separator + 1).trim();
        if (key.isEmpty()) {
            return;
        }

        KeyBinding binding = KEY_BINDINGS_BY_OPTION.get(key);
        if (binding != null) {
            setKeyBinding(binding, parseInt(value, getKeyBinding(binding)));
            return;
        }

        if (!applyKnownOption(key, value)) {
            unknownOptions.put(key, value);
        }
    }

    private boolean applyKnownOption(String key, String value) {
        switch (key) {
            case "playerName", "username", "name" -> setPlayerName(value);
            case "gameMode", "gamemode" -> setGameMode(GameMode.fromName(value));
            case "difficulty" -> setDifficulty(Difficulty.fromName(value));
            case "music", "musicVolume" -> setMusicVolume(parseFloat(value, musicVolume));
            case "sound", "soundVolume" -> setSoundVolume(parseFloat(value, soundVolume));
            case "invertYMouse" -> setInvertYMouse(parseBoolean(value, invertYMouse));
            case "mouseSensitivity" -> setMouseSensitivity(parseFloat(value, mouseSensitivity));
            case "fov" -> setFov(parseFloat(value, fov));
            case "gamma" -> setGamma(parseFloat(value, gamma));
            case "renderDistance", "viewDistance" -> setRenderDistance(parseLegacyRenderDistance(value));
            case "renderDistanceChunks" -> setRenderDistance(parseInt(value, renderDistance));
            case "guiScale" -> setGuiScale(parseInt(value, guiScale));
            case "particles" -> setParticles(parseInt(value, particles));
            case "framerateLimit", "fpsLimit" -> setFramerateLimit(parseInt(value, framerateLimit));
            case "viewBobbing", "bobView" -> setViewBobbing(parseBoolean(value, viewBobbing));
            case "fancyGraphics" -> setFancyGraphics(parseBoolean(value, fancyGraphics));
            case "smoothLighting", "ao", "ambientOcclusion" -> setSmoothLighting(parseBoolean(value, smoothLighting));
            case "clouds" -> setClouds(parseBoolean(value, clouds));
            case "fullscreen" -> setFullscreen(parseBoolean(value, fullscreen));
            case "vsync" -> setVsync(parseBoolean(value, vsync));
            case "advancedOpenGL", "advancedOpengl" -> setAdvancedOpenGl(parseBoolean(value, advancedOpenGl));
            case "texturePack", "skin", "resourcePack" -> setSelectedTexturePack(value);
            case "lastServer" -> setLastServer(value);
            case "language", "lang" -> setLanguage(value);
            default -> {
                return false;
            }
        }
        return true;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        String cleaned = playerName == null ? "" : playerName.trim();
        this.playerName = cleaned.isEmpty() ? DEFAULT_PLAYER_NAME : cleaned;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(GameMode gameMode) {
        this.gameMode = gameMode == null ? GameMode.SURVIVAL : gameMode;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty == null ? Difficulty.NORMAL : difficulty;
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public void setMusicVolume(float musicVolume) {
        this.musicVolume = clamp01(musicVolume);
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public void setSoundVolume(float soundVolume) {
        this.soundVolume = clamp01(soundVolume);
    }

    public boolean isInvertYMouse() {
        return invertYMouse;
    }

    public void setInvertYMouse(boolean invertYMouse) {
        this.invertYMouse = invertYMouse;
    }

    public float getMouseSensitivity() {
        return mouseSensitivity;
    }

    public float mouseSensitivityMultiplier() {
        return 0.25f + mouseSensitivity * 1.75f;
    }

    public void setMouseSensitivity(float mouseSensitivity) {
        this.mouseSensitivity = clamp01(mouseSensitivity);
    }

    public float getFov() {
        return fov;
    }

    public void setFov(float fov) {
        this.fov = clamp01(fov);
    }

    public float getGamma() {
        return gamma;
    }

    public void setGamma(float gamma) {
        this.gamma = clamp01(gamma);
    }

    public int getRenderDistance() {
        return renderDistance;
    }

    public void setRenderDistance(int renderDistance) {
        this.renderDistance = clamp(renderDistance, MIN_RENDER_DISTANCE_CHUNKS, MAX_RENDER_DISTANCE_CHUNKS);
    }

    public int getGuiScale() {
        return guiScale;
    }

    public void setGuiScale(int guiScale) {
        this.guiScale = clamp(guiScale, 0, 4);
    }

    public int getParticles() {
        return particles;
    }

    public void setParticles(int particles) {
        this.particles = clamp(particles, 0, 2);
    }

    public int getFramerateLimit() {
        return framerateLimit;
    }

    public void setFramerateLimit(int framerateLimit) {
        this.framerateLimit = clamp(framerateLimit, 0, 1000);
    }

    public boolean isViewBobbing() {
        return viewBobbing;
    }

    public void setViewBobbing(boolean viewBobbing) {
        this.viewBobbing = viewBobbing;
    }

    public boolean isFancyGraphics() {
        return fancyGraphics;
    }

    public void setFancyGraphics(boolean fancyGraphics) {
        this.fancyGraphics = fancyGraphics;
    }

    public boolean isSmoothLighting() {
        return smoothLighting;
    }

    public void setSmoothLighting(boolean smoothLighting) {
        this.smoothLighting = smoothLighting;
    }

    public boolean isClouds() {
        return clouds;
    }

    public void setClouds(boolean clouds) {
        this.clouds = clouds;
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public boolean isVsync() {
        return vsync;
    }

    public void setVsync(boolean vsync) {
        this.vsync = vsync;
    }

    public boolean isAdvancedOpenGl() {
        return advancedOpenGl;
    }

    public void setAdvancedOpenGl(boolean advancedOpenGl) {
        this.advancedOpenGl = advancedOpenGl;
    }

    public String getSelectedTexturePack() {
        return selectedTexturePack;
    }

    public void setSelectedTexturePack(String selectedTexturePack) {
        String cleaned = selectedTexturePack == null ? "" : selectedTexturePack.trim();
        this.selectedTexturePack = cleaned.isEmpty() ? DEFAULT_TEXTURE_PACK : cleaned;
    }

    public String getLastServer() {
        return lastServer;
    }

    public void setLastServer(String lastServer) {
        this.lastServer = lastServer == null ? "" : lastServer.trim();
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        String cleaned = language == null ? "" : language.trim();
        this.language = cleaned.isEmpty() ? DEFAULT_LANGUAGE : cleaned;
    }

    public Map<KeyBinding, Integer> getKeyBindings() {
        return Collections.unmodifiableMap(new EnumMap<>(keyBindings));
    }

    public int getKeyBinding(KeyBinding binding) {
        if (binding == null) {
            throw new IllegalArgumentException("binding cannot be null");
        }
        return keyBindings.getOrDefault(binding, binding.defaultCode());
    }

    public void setKeyBinding(KeyBinding binding, int keyCode) {
        if (binding == null) {
            throw new IllegalArgumentException("binding cannot be null");
        }
        keyBindings.put(binding, keyCode);
    }

    public void resetKeyBindings() {
        keyBindings.clear();
        for (KeyBinding binding : KeyBinding.values()) {
            keyBindings.put(binding, binding.defaultCode());
        }
    }

    public Map<String, String> getUnknownOptions() {
        return Collections.unmodifiableMap(unknownOptions);
    }

    private static void add(List<String> lines, String key, String value) {
        lines.add(key + ":" + (value == null ? "" : value));
    }

    private static List<String> readLines(Reader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[1024];
        int read;
        while ((read = reader.read(buffer)) >= 0) {
            builder.append(buffer, 0, read);
        }
        return builder.toString().lines().toList();
    }

    private static Map<String, KeyBinding> createKeyBindingLookup() {
        Map<String, KeyBinding> bindings = new HashMap<>();
        for (KeyBinding binding : KeyBinding.values()) {
            bindings.put(binding.optionName(), binding);
        }
        return bindings;
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int parseLegacyRenderDistance(String value) {
        int parsed = parseInt(value, renderDistance);
        return switch (parsed) {
            case 0 -> 12; // Old Far option
            case 1 -> DEFAULT_RENDER_DISTANCE_CHUNKS; // Old Normal option
            case 2 -> 4; // Old Short option
            case 3 -> MIN_RENDER_DISTANCE_CHUNKS; // Old Tiny option
            default -> parsed;
        };
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
            return false;
        }
        return fallback;
    }

    private static float clamp01(float value) {
        if (Float.isNaN(value)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum KeyBinding {
        ATTACK("key_key.attack", "Attack", -100),
        USE("key_key.use", "Use Item", -99),
        PICK_BLOCK("key_key.pickItem", "Pick Block", -98),
        FORWARD("key_key.forward", "Forward", 87),
        LEFT("key_key.left", "Left", 65),
        BACK("key_key.back", "Back", 83),
        RIGHT("key_key.right", "Right", 68),
        JUMP("key_key.jump", "Jump", 32),
        SNEAK("key_key.sneak", "Sneak", 340),
        DROP("key_key.drop", "Drop", 81),
        INVENTORY("key_key.inventory", "Inventory", 69),
        CHAT("key_key.chat", "Chat", 84),
        PLAYER_LIST("key_key.playerlist", "Player List", 258),
        COMMAND("key_key.command", "Command", 47),
        SCREENSHOT("key_key.screenshot", "Screenshot", 291),
        TOGGLE_PERSPECTIVE("key_key.togglePerspective", "Toggle Perspective", 294),
        SMOOTH_CAMERA("key_key.smoothCamera", "Smooth Camera", 295);

        private final String optionName;
        private final String displayName;
        private final int defaultCode;

        KeyBinding(String optionName, String displayName, int defaultCode) {
            this.optionName = optionName;
            this.displayName = displayName;
            this.defaultCode = defaultCode;
        }

        public String optionName() {
            return optionName;
        }

        public String displayName() {
            return displayName;
        }

        public int defaultCode() {
            return defaultCode;
        }

        public static KeyBinding fromOptionName(String optionName) {
            if (optionName == null) {
                return null;
            }
            return KEY_BINDINGS_BY_OPTION.get(optionName.trim());
        }

        public static KeyBinding fromDisplayName(String displayName) {
            if (displayName == null) {
                return null;
            }
            String normalized = displayName.trim().toLowerCase(Locale.ROOT);
            for (KeyBinding binding : values()) {
                if (binding.displayName.toLowerCase(Locale.ROOT).equals(normalized)
                        || binding.name().toLowerCase(Locale.ROOT).equals(normalized.replace(' ', '_'))) {
                    return binding;
                }
            }
            return null;
        }
    }
}
