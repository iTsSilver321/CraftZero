package com.craftzero.ui.menu;

import java.util.Objects;

public final class MenuButton implements MenuComponent {

    private final String id;
    private Rect bounds;
    private String label;
    private Runnable action;
    private Runnable clickSound = () -> {
    };
    private float[] textColor;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean hovered;
    private boolean pressed;

    public MenuButton(String id, String label, Rect bounds, Runnable action) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.action = action == null ? () -> {
        } : action;
    }

    public MenuButton(String label, int x, int y, int width, int height, Runnable action) {
        this(label, label, new Rect(x, y, width, height), action);
    }

    @Override
    public String id() {
        return id;
    }

    public String label() {
        return label;
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

    public void setLabel(String label) {
        this.label = Objects.requireNonNull(label, "label");
    }

    public float[] textColor() {
        return textColor == null ? null : textColor.clone();
    }

    public void setTextColor(float r, float g, float b, float a) {
        this.textColor = new float[] { r, g, b, a };
    }

    public void clearTextColor() {
        this.textColor = null;
    }

    @Override
    public Rect bounds() {
        return bounds;
    }

    public void setBounds(Rect bounds) {
        this.bounds = Objects.requireNonNull(bounds, "bounds");
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
            hovered = false;
            pressed = false;
        }
    }

    public MenuButton visible(boolean visible) {
        setVisible(visible);
        return this;
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
            hovered = false;
            pressed = false;
        }
    }

    public MenuButton enabled(boolean enabled) {
        setEnabled(enabled);
        return this;
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean hovered() {
        return hovered;
    }

    public boolean isPressed() {
        return pressed;
    }

    public void setAction(Runnable action) {
        this.action = action == null ? () -> {
        } : action;
    }

    public void setClickSound(Runnable clickSound) {
        this.clickSound = clickSound == null ? () -> {
        } : clickSound;
    }

    public MenuButton clickSound(Runnable clickSound) {
        setClickSound(clickSound);
        return this;
    }

    public void click() {
        if (visible && enabled) {
            clickSound.run();
            action.run();
        }
    }

    public void update(MenuInput input) {
        if (input == null || !visible) {
            return;
        }
        int mouseX = (int) Math.round(input.mouseX());
        int mouseY = (int) Math.round(input.mouseY());
        mouseMoved(mouseX, mouseY);
        if (input.leftPressed()) {
            if (!pressed) {
                mousePressed(mouseX, mouseY, MouseButton.LEFT);
            }
        } else if (pressed) {
            mouseReleased(mouseX, mouseY, MouseButton.LEFT);
        }
    }

    public ClassicGuiTexture.ButtonState visualState() {
        if (!enabled) {
            return ClassicGuiTexture.ButtonState.DISABLED;
        }
        return hovered ? ClassicGuiTexture.ButtonState.HOVERED : ClassicGuiTexture.ButtonState.NORMAL;
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        boolean nextHovered = enabled && hitTest(x, y);
        boolean changed = nextHovered != hovered;
        hovered = nextHovered;
        return changed;
    }

    @Override
    public boolean mousePressed(int x, int y, MouseButton button) {
        if (button != MouseButton.LEFT || !enabled || !hitTest(x, y)) {
            return false;
        }
        pressed = true;
        return true;
    }

    @Override
    public boolean mouseReleased(int x, int y, MouseButton button) {
        if (button != MouseButton.LEFT) {
            return false;
        }
        boolean wasPressed = pressed;
        pressed = false;
        if (wasPressed && enabled && hitTest(x, y)) {
            clickSound.run();
            action.run();
            return true;
        }
        return wasPressed;
    }
}
