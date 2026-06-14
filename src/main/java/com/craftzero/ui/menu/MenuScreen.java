package com.craftzero.ui.menu;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

public final class MenuScreen implements Screen {
    public enum Background {
        NONE,
        DIRT,
        PANORAMA
    }

    private final String id;
    private final String title;
    private final List<MenuComponent> components;
    private final boolean pausesGame;
    private final boolean closeOnBack;
    private final BooleanSupplier backHandler;
    private final Background background;
    private float renderTime;

    public MenuScreen(String id, String title, List<MenuComponent> components) {
        this(id, title, components, Background.DIRT);
    }

    public MenuScreen(String id, String title, List<MenuComponent> components, Background background) {
        this(id, title, components, true, true, () -> false, background);
    }

    public MenuScreen(String id, String title, List<MenuComponent> components, boolean pausesGame, boolean closeOnBack,
            BooleanSupplier backHandler) {
        this(id, title, components, pausesGame, closeOnBack, backHandler, Background.DIRT);
    }

    public MenuScreen(String id, String title, List<MenuComponent> components, boolean pausesGame, boolean closeOnBack,
            BooleanSupplier backHandler, Background background) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = Objects.requireNonNull(title, "title");
        this.components = List.copyOf(Objects.requireNonNull(components, "components"));
        this.pausesGame = pausesGame;
        this.closeOnBack = closeOnBack;
        this.backHandler = backHandler == null ? () -> false : backHandler;
        this.background = Objects.requireNonNull(background, "background");
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public List<MenuComponent> components() {
        return components;
    }

    @Override
    public boolean pausesGame() {
        return pausesGame;
    }

    @Override
    public boolean shouldCloseOnBack() {
        return closeOnBack;
    }

    @Override
    public boolean handleBack() {
        return backHandler.getAsBoolean();
    }

    public Background background() {
        return background;
    }

    @Override
    public void render(MenuRenderer renderer, MenuInput input, float deltaTime) {
        renderTime += Math.max(0.0f, deltaTime);
        switch (background) {
            case PANORAMA -> renderer.renderPanoramaBackground(renderTime);
            case DIRT -> renderer.renderDirtBackground();
            case NONE -> {
            }
        }
        if (!title.isEmpty()) {
            renderer.drawTitle(title, background == Background.PANORAMA ? 72 : 34,
                    background == Background.PANORAMA ? 3.0f : 2.0f);
        }
        for (MenuComponent component : components) {
            renderer.drawComponent(component);
        }
    }
}
