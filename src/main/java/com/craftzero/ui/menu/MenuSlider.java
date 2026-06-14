package com.craftzero.ui.menu;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public final class MenuSlider implements MenuComponent {

    private static final double EPSILON = 0.0000001;

    private final String id;
    private Rect bounds;
    private String label;
    private final double min;
    private final double max;
    private final double step;
    private double value;
    private DoubleConsumer onChanged;
    private DoubleFunction<String> formatter = value -> String.format("%.0f%%", value * 100.0);
    private Supplier<Float> legacyGetter;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean hovered;
    private boolean dragging;

    public MenuSlider(String id, String label, Rect bounds, double min, double max, double value, double step,
            DoubleConsumer onChanged) {
        if (max < min) {
            throw new IllegalArgumentException("max must be >= min");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.min = min;
        this.max = max;
        this.step = step;
        this.onChanged = onChanged == null ? ignored -> {
        } : onChanged;
        this.value = snapAndClamp(value);
    }

    public MenuSlider(String label, int x, int y, int width, int height, Supplier<Float> getter,
            Consumer<Float> setter, Function<Float, String> formatter) {
        this(label, label, new Rect(x, y, width, height), 0.0, 1.0,
                getter == null ? 0.0 : getter.get(), 0.0,
                value -> {
                    if (setter != null) {
                        setter.accept((float) value);
                    }
                });
        this.legacyGetter = getter;
        if (formatter != null) {
            setFormatter(value -> formatter.apply((float) value));
        }
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

    public String displayText() {
        return label + ": " + formatter.apply(value);
    }

    public void setFormatter(DoubleFunction<String> formatter) {
        this.formatter = Objects.requireNonNull(formatter, "formatter");
    }

    @Override
    public Rect bounds() {
        return bounds;
    }

    public void setBounds(Rect bounds) {
        this.bounds = Objects.requireNonNull(bounds, "bounds");
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public double step() {
        return step;
    }

    public double value() {
        return value;
    }

    public void setValue(double value) {
        double next = snapAndClamp(value);
        if (Math.abs(next - this.value) > EPSILON) {
            this.value = next;
            onChanged.accept(this.value);
        }
    }

    private void setValueSilently(double value) {
        this.value = snapAndClamp(value);
    }

    public double normalizedValue() {
        if (Math.abs(max - min) <= EPSILON) {
            return 0.0;
        }
        return (value - min) / (max - min);
    }

    public void setNormalizedValue(double normalizedValue) {
        setValue(min + clamp01(normalizedValue) * (max - min));
    }

    public double valueForMouseX(int mouseX) {
        if (bounds.width() == 0) {
            return min;
        }
        return min + clamp01((mouseX - bounds.x()) / (double) bounds.width()) * (max - min);
    }

    public int thumbCenterX() {
        return bounds.x() + (int) Math.round(normalizedValue() * bounds.width());
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean isDragging() {
        return dragging;
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
            dragging = false;
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
            hovered = false;
            dragging = false;
        }
    }

    public void setOnChanged(DoubleConsumer onChanged) {
        this.onChanged = onChanged == null ? ignored -> {
        } : onChanged;
    }

    public void update(MenuInput input) {
        if (input == null || !visible) {
            return;
        }
        if (legacyGetter != null && !dragging) {
            setValueSilently(legacyGetter.get());
        }
        int mouseX = (int) Math.round(input.mouseX());
        int mouseY = (int) Math.round(input.mouseY());
        mouseMoved(mouseX, mouseY);
        if (input.leftPressed()) {
            if (!dragging) {
                mousePressed(mouseX, mouseY, MouseButton.LEFT);
            } else {
                setValue(valueForMouseX(mouseX));
            }
        } else if (dragging) {
            mouseReleased(mouseX, mouseY, MouseButton.LEFT);
        }
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        boolean handled = false;
        boolean nextHovered = enabled && hitTest(x, y);
        if (nextHovered != hovered) {
            hovered = nextHovered;
            handled = true;
        }
        if (dragging) {
            setValue(valueForMouseX(x));
            handled = true;
        }
        return handled;
    }

    @Override
    public boolean mousePressed(int x, int y, MouseButton button) {
        if (button != MouseButton.LEFT || !enabled || !hitTest(x, y)) {
            return false;
        }
        dragging = true;
        setValue(valueForMouseX(x));
        return true;
    }

    @Override
    public boolean mouseReleased(int x, int y, MouseButton button) {
        if (button != MouseButton.LEFT || !dragging) {
            return false;
        }
        setValue(valueForMouseX(x));
        dragging = false;
        return true;
    }

    private double snapAndClamp(double raw) {
        double clamped = Math.max(min, Math.min(max, raw));
        if (step > EPSILON) {
            clamped = min + Math.round((clamped - min) / step) * step;
        }
        return Math.max(min, Math.min(max, clamped));
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
