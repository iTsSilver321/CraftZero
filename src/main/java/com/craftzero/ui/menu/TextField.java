package com.craftzero.ui.menu;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntPredicate;

public final class TextField implements MenuComponent {

    private static final int DEFAULT_CHARACTER_WIDTH = 6;
    private static final long REPEAT_INITIAL_DELAY_NANOS = 350_000_000L;
    private static final long REPEAT_INTERVAL_NANOS = 45_000_000L;

    private final String id;
    private Rect bounds;
    private final StringBuilder text = new StringBuilder();
    private int maxLength;
    private int cursorIndex;
    private int characterWidth = DEFAULT_CHARACTER_WIDTH;
    private IntPredicate characterFilter = character -> character >= 32 && character != 127;
    private Consumer<String> onChanged = ignored -> {
    };
    private Consumer<String> onSubmitted = ignored -> {
    };
    private boolean visible = true;
    private boolean enabled = true;
    private boolean focused;
    private boolean mouseWasDown;
    private int heldRepeatKey = Integer.MIN_VALUE;
    private long nextRepeatNanos;

    public TextField(String id, Rect bounds, String initialText, int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("maxLength must be >= 0");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.maxLength = maxLength;
        setText(initialText == null ? "" : initialText);
    }

    public TextField(String initialText, int maxLength, int x, int y, int width, int height) {
        this("text-field", new Rect(x, y, width, height), initialText, maxLength);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Rect bounds() {
        return bounds;
    }

    public int x() {
        return bounds.x();
    }

    public int y() {
        return bounds.y();
    }

    public int width() {
        return bounds.width();
    }

    public int height() {
        return bounds.height();
    }

    public void setBounds(Rect bounds) {
        this.bounds = Objects.requireNonNull(bounds, "bounds");
    }

    public String text() {
        return text.toString();
    }

    public String value() {
        return text();
    }

    public void setText(String value) {
        String next = value == null ? "" : value;
        if (next.length() > maxLength) {
            next = next.substring(0, maxLength);
        }
        text.setLength(0);
        text.append(next);
        cursorIndex = text.length();
        onChanged.accept(text());
    }

    public int maxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("maxLength must be >= 0");
        }
        this.maxLength = maxLength;
        if (text.length() > maxLength) {
            text.setLength(maxLength);
        }
        cursorIndex = Math.min(cursorIndex, text.length());
    }

    public int cursorIndex() {
        return cursorIndex;
    }

    public void setCursorIndex(int cursorIndex) {
        this.cursorIndex = Math.max(0, Math.min(text.length(), cursorIndex));
    }

    public int characterWidth() {
        return characterWidth;
    }

    public void setCharacterWidth(int characterWidth) {
        if (characterWidth <= 0) {
            throw new IllegalArgumentException("characterWidth must be > 0");
        }
        this.characterWidth = characterWidth;
    }

    public boolean isFocused() {
        return focused;
    }

    public boolean focused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused && visible && enabled;
        if (!this.focused) {
            resetRepeat();
        }
    }

    public void setCharacterFilter(IntPredicate characterFilter) {
        this.characterFilter = Objects.requireNonNull(characterFilter, "characterFilter");
    }

    public void setOnChanged(Consumer<String> onChanged) {
        this.onChanged = onChanged == null ? ignored -> {
        } : onChanged;
    }

    public void setOnSubmitted(Consumer<String> onSubmitted) {
        this.onSubmitted = onSubmitted == null ? ignored -> {
        } : onSubmitted;
    }

    public TextField onEnter(Runnable action) {
        setOnSubmitted(ignored -> {
            if (action != null) {
                action.run();
            }
        });
        return this;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    public boolean visible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (!visible) {
            focused = false;
            resetRepeat();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            focused = false;
            resetRepeat();
        }
    }

    @Override
    public boolean mousePressed(int x, int y, MouseButton button) {
        if (button != MouseButton.LEFT || !visible || !enabled) {
            return false;
        }
        boolean hit = hitTest(x, y);
        focused = hit;
        if (hit) {
            setCursorIndex((x - bounds.x()) / characterWidth);
        }
        return hit;
    }

    @Override
    public boolean keyPressed(int keyCode) {
        if (!focused || !enabled) {
            return false;
        }
        switch (keyCode) {
            case MenuKeys.LEFT -> setCursorIndex(cursorIndex - 1);
            case MenuKeys.RIGHT -> setCursorIndex(cursorIndex + 1);
            case MenuKeys.HOME -> setCursorIndex(0);
            case MenuKeys.END -> setCursorIndex(text.length());
            case MenuKeys.BACKSPACE -> deleteBeforeCursor();
            case MenuKeys.DELETE -> deleteAtCursor();
            case MenuKeys.ENTER -> onSubmitted.accept(text());
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(char character) {
        if (!focused || !enabled || text.length() >= maxLength || !characterFilter.test(character)) {
            return false;
        }
        text.insert(cursorIndex, character);
        cursorIndex++;
        onChanged.accept(text());
        return true;
    }

    public void update(MenuInput input) {
        if (input == null || !visible) {
            return;
        }
        int mouseX = (int) Math.round(input.mouseX());
        int mouseY = (int) Math.round(input.mouseY());
        if (input.leftPressed() && !mouseWasDown) {
            mousePressed(mouseX, mouseY, MouseButton.LEFT);
        }
        mouseWasDown = input.leftPressed();

        long now = System.nanoTime();
        if (input.pressedKeys() != null) {
            for (int key : input.pressedKeys()) {
                if (isRepeatableEditingKey(key)) {
                    startRepeat(key, now);
                }
                keyPressed(key);
            }
        }
        repeatHeldKey(input, now);
        if (input.typedCharacters() != null) {
            for (char character : input.typedCharacters()) {
                charTyped(character);
            }
        }
    }

    private void deleteBeforeCursor() {
        if (cursorIndex == 0) {
            return;
        }
        text.deleteCharAt(cursorIndex - 1);
        cursorIndex--;
        onChanged.accept(text());
    }

    private void deleteAtCursor() {
        if (cursorIndex >= text.length()) {
            return;
        }
        text.deleteCharAt(cursorIndex);
        onChanged.accept(text());
    }

    private void repeatHeldKey(MenuInput input, long now) {
        if (!focused || !enabled) {
            resetRepeat();
            return;
        }
        int key = heldEditingKey(input);
        if (key == Integer.MIN_VALUE) {
            resetRepeat();
            return;
        }
        if (key != heldRepeatKey) {
            startRepeat(key, now);
            return;
        }
        if (now < nextRepeatNanos) {
            return;
        }
        keyPressed(key);
        nextRepeatNanos = now + REPEAT_INTERVAL_NANOS;
    }

    private int heldEditingKey(MenuInput input) {
        if (input == null) {
            return Integer.MIN_VALUE;
        }
        if (input.keyDown(MenuKeys.BACKSPACE)) {
            return MenuKeys.BACKSPACE;
        }
        if (input.keyDown(MenuKeys.DELETE)) {
            return MenuKeys.DELETE;
        }
        if (input.keyDown(MenuKeys.LEFT)) {
            return MenuKeys.LEFT;
        }
        if (input.keyDown(MenuKeys.RIGHT)) {
            return MenuKeys.RIGHT;
        }
        return Integer.MIN_VALUE;
    }

    private boolean isRepeatableEditingKey(int key) {
        return key == MenuKeys.BACKSPACE || key == MenuKeys.DELETE || key == MenuKeys.LEFT || key == MenuKeys.RIGHT;
    }

    private void startRepeat(int key, long now) {
        heldRepeatKey = key;
        nextRepeatNanos = now + REPEAT_INITIAL_DELAY_NANOS;
    }

    private void resetRepeat() {
        heldRepeatKey = Integer.MIN_VALUE;
        nextRepeatNanos = 0L;
    }
}
