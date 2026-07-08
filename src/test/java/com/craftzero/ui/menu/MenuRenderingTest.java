package com.craftzero.ui.menu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MenuRenderingTest {

    @Test
    @DisplayName("BaseMenuScreen should draw background, title, and only visible controls")
    void baseMenuScreenRendersVisibleControlsOnly() {
        BaseMenuScreen screen = new BaseMenuScreen("Options", true, false, null);
        MenuButton shownButton = new MenuButton("shown", "Shown", new Rect(10, 10, 100, 20), null);
        MenuSlider hiddenSlider = new MenuSlider("hidden-slider", "Hidden", new Rect(10, 40, 100, 20),
                0.0, 1.0, 0.5, 0.0, null);
        TextField hiddenField = new TextField("hidden-field", new Rect(10, 70, 100, 20), "", 10);
        MenuList<String> hiddenList = new MenuList<>("hidden-list", new Rect(10, 100, 100, 20), 20,
                List.of("world"), value -> value);
        MenuLabel shownLabel = MenuLabel.centered("score", "Score: 12", 160, 120, 200);
        MenuLabel hiddenLabel = MenuLabel.centered("hidden-label", "Hidden", 160, 132, 200).visible(false);
        hiddenSlider.setVisible(false);
        hiddenField.setVisible(false);
        hiddenList.setVisible(false);

        screen.add(hiddenLabel);
        screen.add(shownLabel);
        screen.add(hiddenList);
        screen.add(hiddenField);
        screen.add(hiddenSlider);
        screen.add(shownButton);

        RecordingMenuRenderer renderer = new RecordingMenuRenderer();
        screen.render(renderer, emptyInput(), 0.016f);

        assertEquals(List.of("dirt", "title:Options", "label:score:Score: 12", "button:shown"), renderer.calls);
        assertEquals(6, screen.components().size());
    }

    @Test
    @DisplayName("ScreenManager should render model MenuScreen components")
    void screenManagerRendersModelScreen() {
        ScreenManager manager = new ScreenManager();
        manager.push(new MenuScreen("title", "Minecraft", List.of(
                new MenuButton("singleplayer", "Singleplayer", new Rect(60, 120, 200, 20), null)),
                false, false, () -> false, MenuScreen.Background.PANORAMA));

        RecordingMenuRenderer renderer = new RecordingMenuRenderer();
        manager.render(renderer, emptyInput(), 0.25f);

        assertEquals(List.of("panorama", "title:Minecraft", "button:singleplayer"), renderer.calls);
    }

    private static MenuInput emptyInput() {
        return new MenuInput(320, 240, 0, 0, false, 0, List.of(), List.of());
    }

    private static final class RecordingMenuRenderer extends MenuRenderer {
        private final List<String> calls = new ArrayList<>();

        @Override
        public void renderPanoramaBackground(float time) {
            calls.add("panorama");
        }

        @Override
        public void renderDirtBackground() {
            calls.add("dirt");
        }

        @Override
        public void drawTitle(String text, int y, float scale) {
            calls.add("title:" + text);
        }

        @Override
        public void drawButton(MenuButton button) {
            calls.add("button:" + button.id());
        }

        @Override
        public void drawSlider(MenuSlider slider) {
            calls.add("slider:" + slider.id());
        }

        @Override
        public void drawTextField(TextField field) {
            calls.add("field:" + field.id());
        }

        @Override
        public <T> void drawList(MenuList<T> list) {
            calls.add("list:" + list.id());
        }

        @Override
        public void drawLabel(MenuLabel label) {
            if (label.visible()) {
                calls.add("label:" + label.id() + ":" + label.text());
            }
        }
    }
}
