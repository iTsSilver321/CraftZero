package com.craftzero.ui.menu;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ScreenManager implements MenuNavigation {

    public interface EscapeHandler {
        boolean onEscape();
    }

    private final Deque<Screen> stack = new ArrayDeque<>();
    private final Runnable onEmptyBack;
    private Runnable buttonClickSound = () -> {
    };

    public ScreenManager() {
        this(() -> {
        });
    }

    public ScreenManager(Runnable onEmptyBack) {
        this.onEmptyBack = Objects.requireNonNull(onEmptyBack, "onEmptyBack");
    }

    @Override
    public void push(Screen screen) {
        Screen next = Objects.requireNonNull(screen, "screen");
        wireButtonClickSounds(next);
        stack.addLast(next);
        next.onOpened();
    }

    public void setButtonClickSound(Runnable buttonClickSound) {
        this.buttonClickSound = buttonClickSound == null ? () -> {
        } : buttonClickSound;
        for (Screen screen : stack) {
            wireButtonClickSounds(screen);
        }
    }

    public void show(Screen screen) {
        clear();
        push(screen);
    }

    @Override
    public void replace(Screen screen) {
        pop();
        push(screen);
    }

    @Override
    public Optional<Screen> pop() {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        Screen removed = stack.removeLast();
        removed.onClosed();
        return Optional.of(removed);
    }

    @Override
    public void clear() {
        while (!stack.isEmpty()) {
            pop();
        }
    }

    @Override
    public boolean back() {
        return handleBack();
    }

    public boolean handleBack() {
        Optional<Screen> current = current();
        if (current.isEmpty()) {
            onEmptyBack.run();
            return false;
        }

        Screen screen = current.get();
        if (screen instanceof EscapeHandler escapeHandler && escapeHandler.onEscape()) {
            return true;
        }
        if (screen.handleBack()) {
            return true;
        }
        if (stack.size() > 1 || screen.shouldCloseOnBack()) {
            pop();
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode) {
        if (keyCode == MenuKeys.ESCAPE) {
            return handleBack();
        }
        return current().map(screen -> screen.keyPressed(keyCode)).orElse(false);
    }

    public boolean charTyped(char character) {
        return current().map(screen -> screen.charTyped(character)).orElse(false);
    }

    public boolean mouseMoved(int x, int y) {
        return current().map(screen -> screen.mouseMoved(x, y)).orElse(false);
    }

    public boolean mousePressed(int x, int y, MouseButton button) {
        return current().map(screen -> screen.mousePressed(x, y, button)).orElse(false);
    }

    public boolean mouseReleased(int x, int y, MouseButton button) {
        return current().map(screen -> screen.mouseReleased(x, y, button)).orElse(false);
    }

    public Optional<Screen> current() {
        return Optional.ofNullable(stack.peekLast());
    }

    public Screen currentScreen() {
        return stack.peekLast();
    }

    public boolean hasScreen() {
        return !stack.isEmpty();
    }

    public void update(MenuInput input) {
        if (input != null && input.keyPressed(MenuKeys.ESCAPE)) {
            handleBack();
            return;
        }
        current().ifPresent(screen -> screen.update(input));
    }

    public void render(MenuRenderer renderer, MenuInput input, float deltaTime) {
        current().ifPresent(screen -> screen.render(renderer, input, deltaTime));
    }

    public int depth() {
        return stack.size();
    }

    public List<Screen> stackSnapshot() {
        return List.copyOf(new ArrayList<>(stack));
    }

    private void wireButtonClickSounds(Screen screen) {
        if (screen == null) {
            return;
        }
        for (MenuComponent component : screen.components()) {
            if (component instanceof MenuButton button) {
                button.setClickSound(buttonClickSound);
            }
        }
    }
}
