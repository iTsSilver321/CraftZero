package com.craftzero.ui.menu;

import java.util.ArrayList;
import java.util.List;

public class BaseMenuScreen implements Screen, ScreenManager.EscapeHandler {
    private final String title;
    private final boolean dirtBackground;
    private final boolean panoramaBackground;
    private final Runnable escapeAction;
    private final List<MenuButton> buttons = new ArrayList<>();
    private final List<MenuSlider> sliders = new ArrayList<>();
    private final List<TextField> textFields = new ArrayList<>();
    private final List<MenuList<?>> lists = new ArrayList<>();
    private Runnable tickAction;
    private float time;

    public BaseMenuScreen(String title, boolean dirtBackground, boolean panoramaBackground, Runnable escapeAction) {
        this.title = title;
        this.dirtBackground = dirtBackground;
        this.panoramaBackground = panoramaBackground;
        this.escapeAction = escapeAction;
    }

    public BaseMenuScreen add(MenuButton button) {
        buttons.add(button);
        return this;
    }

    public BaseMenuScreen add(MenuSlider slider) {
        sliders.add(slider);
        return this;
    }

    public BaseMenuScreen add(TextField field) {
        textFields.add(field);
        return this;
    }

    public BaseMenuScreen add(MenuList<?> list) {
        lists.add(list);
        return this;
    }

    public BaseMenuScreen onTick(Runnable tickAction) {
        this.tickAction = tickAction;
        return this;
    }

    @Override
    public void update(MenuInput input) {
        if (tickAction != null) {
            tickAction.run();
        }
        for (MenuList<?> list : lists) {
            if (list.visible()) {
                list.update(input);
            }
        }
        for (TextField field : textFields) {
            if (field.visible()) {
                field.update(input);
            }
        }
        for (MenuSlider slider : sliders) {
            if (slider.visible()) {
                slider.update(input);
            }
        }
        for (MenuButton button : buttons) {
            if (button.visible()) {
                button.update(input);
            }
        }
    }

    @Override
    public void render(MenuRenderer renderer, MenuInput input, float deltaTime) {
        time += deltaTime;
        if (panoramaBackground) {
            renderer.renderPanoramaBackground(time);
        } else if (dirtBackground) {
            renderer.renderDirtBackground();
        }
        if (title != null && !title.isEmpty()) {
            renderer.drawTitle(title, panoramaBackground ? 72 : 34, panoramaBackground ? 3.0f : 2.0f);
        }
        for (MenuList<?> list : lists) {
            renderer.drawComponent(list);
        }
        for (TextField field : textFields) {
            renderer.drawComponent(field);
        }
        for (MenuSlider slider : sliders) {
            renderer.drawComponent(slider);
        }
        for (MenuButton button : buttons) {
            renderer.drawComponent(button);
        }
    }

    @Override
    public boolean onEscape() {
        if (escapeAction != null) {
            escapeAction.run();
            return true;
        }
        return false;
    }

    public List<MenuButton> buttons() {
        return buttons;
    }

    public List<MenuSlider> sliders() {
        return sliders;
    }

    public List<TextField> textFields() {
        return textFields;
    }

    public List<MenuList<?>> lists() {
        return lists;
    }

    @Override
    public List<MenuComponent> components() {
        List<MenuComponent> components = new ArrayList<>(lists.size() + textFields.size() + sliders.size() + buttons.size());
        components.addAll(lists);
        components.addAll(textFields);
        components.addAll(sliders);
        components.addAll(buttons);
        return List.copyOf(components);
    }
}
