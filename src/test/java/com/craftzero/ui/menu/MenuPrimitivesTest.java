package com.craftzero.ui.menu;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MenuPrimitivesTest {

    @Test
    @DisplayName("MenuButton should hit test and fire only when released inside")
    void buttonFiresOnReleaseInside() {
        AtomicInteger clicks = new AtomicInteger();
        MenuButton button = new MenuButton("play", "Play", new Rect(10, 20, 100, 20), clicks::incrementAndGet);

        assertTrue(button.hitTest(10, 20));
        assertTrue(button.hitTest(109, 39));
        assertFalse(button.hitTest(110, 39));
        assertFalse(button.hitTest(109, 40));

        assertTrue(button.mouseMoved(15, 25));
        assertTrue(button.isHovered());
        assertTrue(button.mousePressed(15, 25, MouseButton.LEFT));
        assertTrue(button.isPressed());
        assertTrue(button.mouseReleased(200, 200, MouseButton.LEFT));
        assertEquals(0, clicks.get());

        assertTrue(button.mousePressed(15, 25, MouseButton.LEFT));
        assertTrue(button.mouseReleased(15, 25, MouseButton.LEFT));
        assertEquals(1, clicks.get());
    }

    @Test
    @DisplayName("MenuSlider should clamp, snap, and map mouse X to value")
    void sliderMathClampsAndSnaps() {
        AtomicReference<Double> lastChanged = new AtomicReference<>();
        MenuSlider slider = new MenuSlider("volume", "Volume", new Rect(10, 0, 100, 20), 0.0, 1.0, 0.0, 0.25,
                lastChanged::set);

        slider.setNormalizedValue(0.51);
        assertEquals(0.5, slider.value(), 0.0001);
        assertEquals(60, slider.thumbCenterX());
        assertEquals(0.5, lastChanged.get(), 0.0001);

        assertTrue(slider.mousePressed(109, 10, MouseButton.LEFT));
        assertEquals(1.0, slider.value(), 0.0001);
        assertTrue(slider.isDragging());

        assertTrue(slider.mouseMoved(-100, 10));
        assertEquals(0.0, slider.value(), 0.0001);
        assertTrue(slider.mouseReleased(35, 10, MouseButton.LEFT));
        assertEquals(0.25, slider.value(), 0.0001);
        assertFalse(slider.isDragging());
    }

    @Test
    @DisplayName("MenuList should select visible rows after scroll and activate selection")
    void menuListSelectsAndActivatesRows() {
        AtomicReference<String> selected = new AtomicReference<>();
        AtomicReference<String> activated = new AtomicReference<>();
        MenuList<String> list = new MenuList<>("worlds", new Rect(0, 0, 100, 40), 20,
                List.of("one", "two", "three", "four"), value -> value);
        list.setOnSelectionChanged(selected::set);
        list.setOnActivated(activated::set);

        assertEquals(2, list.visibleRowCount());
        list.scroll(1);
        assertEquals(1, list.scrollOffset());
        assertEquals(1, list.itemIndexAt(10, 10).orElseThrow());

        assertTrue(list.mousePressed(10, 10, MouseButton.LEFT));
        assertEquals(1, list.selectedIndex());
        assertEquals("two", selected.get());

        assertTrue(list.keyPressed(MenuKeys.DOWN));
        assertEquals(2, list.selectedIndex());
        assertEquals("three", selected.get());

        assertTrue(list.keyPressed(MenuKeys.ENTER));
        assertEquals("three", activated.get());
    }

    @Test
    @DisplayName("TextField should focus, edit around the cursor, and submit text")
    void textFieldEditsText() {
        AtomicReference<String> changed = new AtomicReference<>();
        AtomicReference<String> submitted = new AtomicReference<>();
        TextField field = new TextField("name", new Rect(10, 10, 120, 20), "abc", 5);
        field.setOnChanged(changed::set);
        field.setOnSubmitted(submitted::set);

        assertTrue(field.mousePressed(16, 15, MouseButton.LEFT));
        assertTrue(field.isFocused());
        assertEquals(1, field.cursorIndex());

        assertTrue(field.charTyped('Z'));
        assertEquals("aZbc", field.text());
        assertEquals("aZbc", changed.get());

        assertTrue(field.keyPressed(MenuKeys.BACKSPACE));
        assertEquals("abc", field.text());

        assertTrue(field.keyPressed(MenuKeys.END));
        assertTrue(field.charTyped('d'));
        assertTrue(field.charTyped('e'));
        assertFalse(field.charTyped('f'));
        assertEquals("abcde", field.text());

        assertTrue(field.keyPressed(MenuKeys.ENTER));
        assertEquals("abcde", submitted.get());

        assertFalse(field.mousePressed(200, 200, MouseButton.LEFT));
        assertFalse(field.isFocused());
    }

    @Test
    @DisplayName("Classic gui.png button UV helper should expose expected rows")
    void classicButtonUvRows() {
        UvRegion normal = ClassicGuiTexture.button(ClassicGuiTexture.ButtonState.NORMAL);
        UvRegion hovered = ClassicGuiTexture.button(ClassicGuiTexture.ButtonState.HOVERED);

        assertEquals(66 / 256f, normal.v1(), 0.0001f);
        assertEquals(86 / 256f, hovered.v1(), 0.0001f);
        assertEquals(200 / 256f, normal.u2(), 0.0001f);
    }

    @Test
    @DisplayName("Hidden controls should not react to direct update paths")
    void hiddenControlsIgnoreUpdates() {
        AtomicInteger clicks = new AtomicInteger();
        MenuInput clickingInput = new MenuInput(320, 240, 15, 15, true, -1.0, List.of(MenuKeys.DOWN), List.of('x'));

        MenuButton button = new MenuButton("hidden-button", "Hidden", new Rect(10, 10, 100, 20), clicks::incrementAndGet);
        button.setVisible(false);
        button.update(clickingInput);
        assertEquals(0, clicks.get());

        TextField field = new TextField("hidden-field", new Rect(10, 10, 100, 20), "", 10);
        field.setFocused(true);
        field.setVisible(false);
        field.update(clickingInput);
        assertEquals("", field.text());
        assertFalse(field.isFocused());

        MenuList<String> list = new MenuList<>("hidden-list", new Rect(10, 10, 100, 40), 20,
                List.of("one", "two", "three"), value -> value);
        list.setVisible(false);
        list.update(clickingInput);
        assertEquals(-1, list.selectedIndex());
        assertEquals(0, list.scrollOffset());
        assertFalse(list.keyPressed(MenuKeys.DOWN));
    }
}
