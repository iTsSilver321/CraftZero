package com.craftzero.ui;

final class ContainerDoubleClickTracker {
    private static final long DOUBLE_CLICK_NANOS = 350_000_000L;

    private int lastClickSlot = -1;
    private long lastClickNanos;
    private boolean lastClickRightClick;

    boolean isDoubleLeftClick(int slotIndex, boolean rightClick) {
        long now = System.nanoTime();
        boolean doubleClick = !rightClick && !lastClickRightClick && slotIndex == lastClickSlot
                && now - lastClickNanos <= DOUBLE_CLICK_NANOS;
        recordClick(slotIndex, rightClick, now);
        return doubleClick;
    }

    void recordClick(int slotIndex, boolean rightClick) {
        recordClick(slotIndex, rightClick, System.nanoTime());
    }

    void reset() {
        lastClickSlot = -1;
        lastClickNanos = 0L;
        lastClickRightClick = false;
    }

    private void recordClick(int slotIndex, boolean rightClick, long now) {
        lastClickSlot = slotIndex;
        lastClickNanos = now;
        lastClickRightClick = rightClick;
    }
}
