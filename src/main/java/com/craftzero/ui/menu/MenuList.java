package com.craftzero.ui.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public final class MenuList<T> implements MenuComponent {

    private final String id;
    private Rect bounds;
    private final int rowHeight;
    private final Function<T, String> labelProvider;
    private List<T> items;
    private Consumer<T> onSelectionChanged = ignored -> {
    };
    private Consumer<T> onActivated = ignored -> {
    };
    private int selectedIndex = -1;
    private int hoveredIndex = -1;
    private int scrollOffset;
    private boolean visible = true;
    private boolean enabled = true;

    public MenuList(String id, Rect bounds, int rowHeight, Collection<T> items, Function<T, String> labelProvider) {
        if (rowHeight <= 0) {
            throw new IllegalArgumentException("rowHeight must be > 0");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.rowHeight = rowHeight;
        this.items = List.copyOf(Objects.requireNonNull(items, "items"));
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider");
    }

    public MenuList(Collection<T> items, Function<T, String> labelProvider, int x, int y, int width, int rowHeight,
            int visibleRows) {
        this("list", new Rect(x, y, width, rowHeight * Math.max(1, visibleRows)), rowHeight, items, labelProvider);
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

    public void setBounds(Rect bounds) {
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        clampScroll();
    }

    public int rowHeight() {
        return rowHeight;
    }

    public int visibleRowCount() {
        return Math.max(1, bounds.height() / rowHeight);
    }

    public int visibleRows() {
        return visibleRowCount();
    }

    public List<T> items() {
        return items;
    }

    public List<Row<T>> visibleRowEntries() {
        List<Row<T>> rows = new ArrayList<>();
        int count = Math.min(visibleRowCount(), Math.max(0, items.size() - scrollOffset));
        for (int row = 0; row < count; row++) {
            int index = scrollOffset + row;
            rows.add(new Row<>(index, items.get(index), rowBounds(row), labelProvider.apply(items.get(index)),
                    index == selectedIndex, index == hoveredIndex));
        }
        return List.copyOf(rows);
    }

    public List<T> visibleItems() {
        int count = Math.min(visibleRowCount(), Math.max(0, items.size() - scrollOffset));
        List<T> visibleItems = new ArrayList<>(count);
        for (int row = 0; row < count; row++) {
            visibleItems.add(items.get(scrollOffset + row));
        }
        return List.copyOf(visibleItems);
    }

    public String labelFor(T item) {
        return labelProvider.apply(item);
    }

    public void setItems(Collection<T> items) {
        this.items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (selectedIndex >= this.items.size()) {
            selectedIndex = -1;
        }
        clampScroll();
    }

    public Optional<T> selectedItem() {
        if (selectedIndex < 0 || selectedIndex >= items.size()) {
            return Optional.empty();
        }
        return Optional.of(items.get(selectedIndex));
    }

    public T selected() {
        return selectedItem().orElse(null);
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        if (selectedIndex < -1 || selectedIndex >= items.size()) {
            throw new IllegalArgumentException("selectedIndex out of range");
        }
        if (this.selectedIndex == selectedIndex) {
            return;
        }
        this.selectedIndex = selectedIndex;
        selectedItem().ifPresent(onSelectionChanged);
    }

    public int hoveredIndex() {
        return hoveredIndex;
    }

    public int scrollOffset() {
        return scrollOffset;
    }

    public void scroll(int rows) {
        scrollOffset += rows;
        clampScroll();
    }

    public Optional<Integer> itemIndexAt(int x, int y) {
        if (!hitTest(x, y)) {
            return Optional.empty();
        }
        int row = (y - bounds.y()) / rowHeight;
        int index = scrollOffset + row;
        if (index < 0 || index >= items.size()) {
            return Optional.empty();
        }
        return Optional.of(index);
    }

    public Rect rowBounds(int visibleRow) {
        return new Rect(bounds.x(), bounds.y() + visibleRow * rowHeight, bounds.width(), rowHeight);
    }

    public void activateSelected() {
        selectedItem().ifPresent(onActivated);
    }

    public void setOnSelectionChanged(Consumer<T> onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged == null ? ignored -> {
        } : onSelectionChanged;
    }

    public void setOnActivated(Consumer<T> onActivated) {
        this.onActivated = onActivated == null ? ignored -> {
        } : onActivated;
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
            hoveredIndex = -1;
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
            hoveredIndex = -1;
        }
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        if (!visible || !enabled) {
            boolean changed = hoveredIndex != -1;
            hoveredIndex = -1;
            return changed;
        }
        int nextHovered = itemIndexAt(x, y).orElse(-1);
        boolean changed = nextHovered != hoveredIndex;
        hoveredIndex = nextHovered;
        return changed;
    }

    @Override
    public boolean mousePressed(int x, int y, MouseButton button) {
        if (button != MouseButton.LEFT || !enabled) {
            return false;
        }
        Optional<Integer> index = itemIndexAt(x, y);
        if (index.isEmpty()) {
            return false;
        }
        setSelectedIndex(index.get());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode) {
        if (!visible || !enabled || items.isEmpty()) {
            return false;
        }
        if (keyCode == MenuKeys.UP) {
            moveSelection(-1);
            return true;
        }
        if (keyCode == MenuKeys.DOWN) {
            moveSelection(1);
            return true;
        }
        if (keyCode == MenuKeys.ENTER) {
            activateSelected();
            return true;
        }
        return false;
    }

    public void update(MenuInput input) {
        if (input == null || !visible) {
            return;
        }
        int mouseX = (int) Math.round(input.mouseX());
        int mouseY = (int) Math.round(input.mouseY());
        mouseMoved(mouseX, mouseY);
        if (input.scrollY() > 0.0) {
            scroll(-1);
        } else if (input.scrollY() < 0.0) {
            scroll(1);
        }
        if (input.leftPressed()) {
            mousePressed(mouseX, mouseY, MouseButton.LEFT);
        }
        if (input.pressedKeys() != null) {
            for (int key : input.pressedKeys()) {
                keyPressed(key);
            }
        }
    }

    private void moveSelection(int delta) {
        int next = selectedIndex < 0 ? 0 : Math.max(0, Math.min(items.size() - 1, selectedIndex + delta));
        setSelectedIndex(next);
        if (next < scrollOffset) {
            scrollOffset = next;
        } else if (next >= scrollOffset + visibleRowCount()) {
            scrollOffset = next - visibleRowCount() + 1;
        }
        clampScroll();
    }

    private void clampScroll() {
        int maxOffset = Math.max(0, items.size() - visibleRowCount());
        scrollOffset = Math.max(0, Math.min(maxOffset, scrollOffset));
    }

    public record Row<T>(int index, T item, Rect bounds, String label, boolean selected, boolean hovered) {
    }
}
