package com.craftzero.ui.menu;

import com.craftzero.inventory.ItemType;
import com.craftzero.progression.AchievementTracker;
import com.craftzero.progression.AchievementType;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class AchievementTreeComponent implements MenuComponent {
    public enum NodeState {
        UNLOCKED,
        AVAILABLE,
        LOCKED
    }

    private static final int NODE_SIZE = 26;
    private static final int STEP_X = 48;
    private static final int STEP_Y = 32;
    private static final int CONTENT_PADDING = 24;
    private static final int WHEEL_STEP = 32;

    private final String id;
    private final Rect bounds;
    private final AchievementTracker tracker;
    private final List<AchievementType> achievements = List.copyOf(Arrays.asList(AchievementType.values()));
    private final int minColumn;
    private final int maxColumn;
    private final int minRow;
    private final int maxRow;
    private final int contentWidth;
    private final int contentHeight;

    private boolean visible = true;
    private boolean enabled = true;
    private int scrollX;
    private int scrollY;
    private int mouseX;
    private int mouseY;
    private boolean mouseWasDown;
    private boolean dragging;
    private int dragStartX;
    private int dragStartY;
    private int dragStartScrollX;
    private int dragStartScrollY;
    private AchievementType hoveredAchievement;
    private AchievementType selectedAchievement;
    private AchievementType lastDetailAchievement;
    private Consumer<AchievementType> detailChanged = ignored -> {
    };

    public AchievementTreeComponent(String id, Rect bounds, AchievementTracker tracker) {
        this.id = Objects.requireNonNull(id, "id");
        this.bounds = Objects.requireNonNull(bounds, "bounds");
        this.tracker = tracker;

        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        int minR = Integer.MAX_VALUE;
        int maxR = Integer.MIN_VALUE;
        for (AchievementType type : achievements) {
            minCol = Math.min(minCol, type.displayColumn());
            maxCol = Math.max(maxCol, type.displayColumn());
            minR = Math.min(minR, type.displayRow());
            maxR = Math.max(maxR, type.displayRow());
        }
        this.minColumn = minCol;
        this.maxColumn = maxCol;
        this.minRow = minR;
        this.maxRow = maxR;
        this.contentWidth = (maxColumn - minColumn) * STEP_X + NODE_SIZE + CONTENT_PADDING * 2;
        this.contentHeight = (maxRow - minRow) * STEP_Y + NODE_SIZE + CONTENT_PADDING * 2;
        this.selectedAchievement = defaultDetailAchievement();
        centerOn(selectedAchievement);
        this.lastDetailAchievement = detailAchievement();
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Rect bounds() {
        return bounds;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    public boolean visible() {
        return visible;
    }

    public AchievementTreeComponent visible(boolean visible) {
        this.visible = visible;
        if (!visible) {
            hoveredAchievement = null;
            dragging = false;
            mouseWasDown = false;
        }
        notifyDetailChangedIfNeeded();
        return this;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public AchievementTreeComponent enabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            hoveredAchievement = null;
            dragging = false;
            mouseWasDown = false;
        }
        notifyDetailChangedIfNeeded();
        return this;
    }

    public AchievementTreeComponent onDetailChanged(Consumer<AchievementType> detailChanged) {
        this.detailChanged = detailChanged == null ? ignored -> {
        } : detailChanged;
        this.detailChanged.accept(detailAchievement());
        return this;
    }

    @Override
    public void update(MenuInput input) {
        if (input == null || !visible || !enabled) {
            return;
        }
        mouseX = (int) Math.round(input.mouseX());
        mouseY = (int) Math.round(input.mouseY());
        boolean inside = bounds.contains(mouseX, mouseY);

        if (inside && input.scrollY() != 0.0) {
            scrollY += input.scrollY() > 0.0 ? -WHEEL_STEP : WHEEL_STEP;
            clampScroll();
        }

        if (input.leftPressed()) {
            if (!mouseWasDown) {
                dragging = inside;
                dragStartX = mouseX;
                dragStartY = mouseY;
                dragStartScrollX = scrollX;
                dragStartScrollY = scrollY;
                AchievementType clicked = achievementAt(mouseX, mouseY);
                if (clicked != null) {
                    selectedAchievement = clicked;
                }
            } else if (dragging) {
                scrollX = dragStartScrollX - (mouseX - dragStartX);
                scrollY = dragStartScrollY - (mouseY - dragStartY);
                clampScroll();
            }
        } else {
            dragging = false;
        }
        mouseWasDown = input.leftPressed();
        hoveredAchievement = inside ? achievementAt(mouseX, mouseY) : null;
        notifyDetailChangedIfNeeded();
    }

    @Override
    public boolean mouseMoved(int x, int y) {
        if (!visible || !enabled) {
            return false;
        }
        mouseX = x;
        mouseY = y;
        AchievementType next = bounds.contains(x, y) ? achievementAt(x, y) : null;
        boolean changed = next != hoveredAchievement;
        hoveredAchievement = next;
        notifyDetailChangedIfNeeded();
        return changed;
    }

    @Override
    public boolean mousePressed(int x, int y, MouseButton button) {
        if (button != MouseButton.LEFT || !visible || !enabled || !bounds.contains(x, y)) {
            return false;
        }
        mouseX = x;
        mouseY = y;
        AchievementType clicked = achievementAt(x, y);
        if (clicked != null) {
            selectedAchievement = clicked;
            hoveredAchievement = clicked;
        }
        dragging = true;
        dragStartX = x;
        dragStartY = y;
        dragStartScrollX = scrollX;
        dragStartScrollY = scrollY;
        notifyDetailChangedIfNeeded();
        return true;
    }

    @Override
    public boolean mouseReleased(int x, int y, MouseButton button) {
        if (button != MouseButton.LEFT) {
            return false;
        }
        boolean wasDragging = dragging;
        dragging = false;
        mouseWasDown = false;
        return wasDragging;
    }

    public List<AchievementType> achievements() {
        return achievements;
    }

    public int contentWidth() {
        return contentWidth;
    }

    public int contentHeight() {
        return contentHeight;
    }

    public int scrollX() {
        return scrollX;
    }

    public int scrollY() {
        return scrollY;
    }

    public int mouseX() {
        return mouseX;
    }

    public int mouseY() {
        return mouseY;
    }

    public AchievementType hoveredAchievement() {
        return hoveredAchievement;
    }

    public AchievementType selectedAchievement() {
        return selectedAchievement;
    }

    public AchievementType detailAchievement() {
        if (hoveredAchievement != null && hasDetails(hoveredAchievement)) {
            return hoveredAchievement;
        }
        if (selectedAchievement != null && hasDetails(selectedAchievement)) {
            return selectedAchievement;
        }
        return defaultDetailAchievement();
    }

    public NodeState nodeState(AchievementType type) {
        if (isUnlocked(type)) {
            return NodeState.UNLOCKED;
        }
        if (isAvailable(type)) {
            return NodeState.AVAILABLE;
        }
        return NodeState.LOCKED;
    }

    public boolean isUnlocked(AchievementType type) {
        return tracker != null && tracker.isUnlocked(type);
    }

    public boolean isAvailable(AchievementType type) {
        if (type == null || isUnlocked(type)) {
            return false;
        }
        AchievementType parent = type.parent();
        return parent == null || isUnlocked(parent);
    }

    public boolean isNodeVisible(AchievementType type) {
        return type != null && (isUnlocked(type) || isAvailable(type) || missingPrerequisiteDepth(type) <= 4);
    }

    public boolean hasDetails(AchievementType type) {
        return type != null && (isUnlocked(type) || isAvailable(type) || missingPrerequisiteDepth(type) <= 3);
    }

    public ItemType iconFor(AchievementType type) {
        return type == null ? null : type.icon();
    }

    public Rect nodeContentRect(AchievementType type) {
        return new Rect(
                CONTENT_PADDING + (type.displayColumn() - minColumn) * STEP_X,
                CONTENT_PADDING + (type.displayRow() - minRow) * STEP_Y,
                NODE_SIZE,
                NODE_SIZE);
    }

    public Rect nodeScreenRect(AchievementType type) {
        Rect content = nodeContentRect(type);
        return new Rect(bounds.x() + content.x() - scrollX, bounds.y() + content.y() - scrollY,
                content.width(), content.height());
    }

    public String titleLine(AchievementType type) {
        if (type == null) {
            return "";
        }
        return statusPrefix(type) + detailTitle(type);
    }

    public String detailTitle(AchievementType type) {
        if (type == null) {
            return "";
        }
        if (!isUnlocked(type) && !isAvailable(type) && missingPrerequisiteDepth(type) >= 3) {
            return "???";
        }
        return type.title();
    }

    public String detailDescription(AchievementType type) {
        if (type == null) {
            return "";
        }
        if (isUnlocked(type)) {
            return type.description();
        }
        if (isAvailable(type)) {
            return type.description();
        }
        if (!hasDetails(type)) {
            return "";
        }
        AchievementType parent = type.parent();
        return parent == null ? "" : "Requires '" + parent.title() + "'";
    }

    public float[] colorFor(AchievementType type) {
        NodeState state = nodeState(type);
        if (state == NodeState.UNLOCKED) {
            return type.special()
                    ? new float[] { 1.0f, 0.86f, 0.35f, 1.0f }
                    : new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
        }
        if (state == NodeState.AVAILABLE) {
            return type.special()
                    ? new float[] { 0.90f, 0.78f, 0.30f, 1.0f }
                    : new float[] { 0.64f, 0.90f, 0.64f, 1.0f };
        }
        return new float[] { 0.42f, 0.42f, 0.42f, 1.0f };
    }

    public void centerOn(AchievementType type) {
        if (type == null) {
            return;
        }
        Rect rect = nodeContentRect(type);
        scrollX = rect.centerX() - bounds.width() / 2;
        scrollY = rect.centerY() - bounds.height() / 2;
        clampScroll();
    }

    private AchievementType achievementAt(int x, int y) {
        for (int i = achievements.size() - 1; i >= 0; i--) {
            AchievementType type = achievements.get(i);
            if (isNodeVisible(type) && nodeScreenRect(type).contains(x, y)) {
                return type;
            }
        }
        return null;
    }

    private AchievementType defaultDetailAchievement() {
        AchievementType fallback = AchievementType.OPEN_INVENTORY;
        for (AchievementType type : achievements) {
            if (!isUnlocked(type) && isAvailable(type)) {
                return type;
            }
            if (!isUnlocked(type) && hasDetails(type)) {
                fallback = type;
            }
        }
        return fallback;
    }

    private int missingPrerequisiteDepth(AchievementType type) {
        int depth = 0;
        AchievementType cursor = type == null ? null : type.parent();
        while (cursor != null && !isUnlocked(cursor)) {
            depth++;
            cursor = cursor.parent();
        }
        return depth;
    }

    private String statusPrefix(AchievementType type) {
        if (isUnlocked(type)) {
            return "Taken: ";
        }
        if (isAvailable(type)) {
            return "Next: ";
        }
        return "Locked: ";
    }

    private void clampScroll() {
        scrollX = clamp(scrollX, 0, Math.max(0, contentWidth - bounds.width()));
        scrollY = clamp(scrollY, 0, Math.max(0, contentHeight - bounds.height()));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void notifyDetailChangedIfNeeded() {
        AchievementType detail = detailAchievement();
        if (detail != lastDetailAchievement) {
            lastDetailAchievement = detail;
            detailChanged.accept(detail);
        }
    }
}
