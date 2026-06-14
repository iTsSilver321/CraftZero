package com.craftzero.main;

/**
 * High-level screens and runtime states used by menu/navigation code.
 */
public enum GameState {
    TITLE(true, false),
    WORLD_SELECT(true, false),
    PLAYING(false, true),
    DEATH(true, true),
    MULTIPLAYER(true, false),
    OPTIONS(true, false),
    MAIN_MENU(true, false),
    SINGLEPLAYER_MENU(true, false),
    WORLD_SELECTION(true, false),
    WORLD_CREATION(true, false),
    OPTIONS_MENU(true, false),
    VIDEO_SETTINGS(true, false),
    CONTROL_SETTINGS(true, false),
    TEXTURE_PACKS(true, false),
    MULTIPLAYER_MENU(true, false),
    CONNECTING(true, false),
    LOADING_WORLD(true, false),
    IN_GAME(false, true),
    PAUSED(true, true),
    DISCONNECTED(true, false),
    SHUTTING_DOWN(false, false);

    private final boolean menuVisible;
    private final boolean worldActive;

    GameState(boolean menuVisible, boolean worldActive) {
        this.menuVisible = menuVisible;
        this.worldActive = worldActive;
    }

    public boolean isMenuVisible() {
        return menuVisible;
    }

    public boolean isWorldActive() {
        return worldActive;
    }

    public boolean isGameplay() {
        return worldActive && !menuVisible;
    }

    public boolean showsWorldBehindMenu() {
        return menuVisible && worldActive;
    }
}
