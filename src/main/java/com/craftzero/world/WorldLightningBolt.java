package com.craftzero.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Short-lived lightning bolt render state. Lightning gameplay effects happen
 * immediately in {@link World}; this object gives the renderer a transient
 * jagged bolt to draw for the same strike.
 */
public class WorldLightningBolt {
    private static final int MAIN_STEPS = 8;
    private static final int BRANCH_COUNT = 3;
    private static final float BRANCH_LENGTH_MIN = 2.5f;
    private static final float BRANCH_LENGTH_RANDOM = 4.0f;
    private static final int INITIAL_VISIBLE_TICKS = 3;
    private static final int MIN_REFLASH_DELAY_TICKS = 1;
    private static final int REFLASH_DELAY_RANDOM_TICKS = 4;
    private static final int MIN_REFLASH_DURATION_TICKS = 1;
    private static final int REFLASH_DURATION_RANDOM_TICKS = 2;
    private static final int LIFETIME_TAIL_TICKS = 1;
    private static final float MIN_VISIBLE_ALPHA = 0.2f;

    public record Segment(float x1, float y1, float z1, float x2, float y2, float z2) {
    }

    public record FlashWindow(float startTick, float endTick) {
        public FlashWindow {
            startTick = Math.max(0.0f, startTick);
            endTick = Math.max(startTick, endTick);
        }
    }

    private final float x;
    private final float y;
    private final float z;
    private final List<Segment> segments;
    private final List<FlashWindow> flashWindows;
    private final float lifetimeTicks;
    private float ageTicks;

    public WorldLightningBolt(float x, float y, float z, Random random) {
        Random source = random == null ? new Random(0L) : random;
        List<Segment> segments = createSegments(x, y, z, source);
        List<FlashWindow> flashWindows = createFlashWindows(source);
        this.x = x;
        this.y = y;
        this.z = z;
        this.segments = List.copyOf(segments);
        this.flashWindows = List.copyOf(flashWindows);
        this.lifetimeTicks = Math.max(1.0f, flashWindows.get(flashWindows.size() - 1).endTick() + LIFETIME_TAIL_TICKS);
    }

    WorldLightningBolt(float x, float y, float z, List<Segment> segments, int lifetimeTicks) {
        this(x, y, z, segments, List.of(new FlashWindow(0.0f, Math.max(1, lifetimeTicks))), lifetimeTicks);
    }

    WorldLightningBolt(float x, float y, float z, List<Segment> segments,
            List<FlashWindow> flashWindows, int lifetimeTicks) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.segments = List.copyOf(segments == null ? List.of() : segments);
        List<FlashWindow> safeWindows = flashWindows == null || flashWindows.isEmpty()
                ? List.of(new FlashWindow(0.0f, Math.max(1, lifetimeTicks)))
                : flashWindows;
        this.flashWindows = List.copyOf(safeWindows);
        float lastFlashEnd = this.flashWindows.get(this.flashWindows.size() - 1).endTick();
        this.lifetimeTicks = Math.max(Math.max(1, lifetimeTicks), lastFlashEnd);
    }

    public static WorldLightningBolt fromNetwork(float x, float y, float z, List<Segment> segments,
            List<FlashWindow> flashWindows, int lifetimeTicks) {
        return new WorldLightningBolt(x, y, z, segments, flashWindows, lifetimeTicks);
    }

    public boolean update(float deltaTime) {
        ageTicks += Math.max(0.0f, deltaTime) * 20.0f;
        return ageTicks >= lifetimeTicks;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public List<FlashWindow> getFlashWindows() {
        return flashWindows;
    }

    public float getAgeTicks() {
        return ageTicks;
    }

    public float getLifetimeTicks() {
        return lifetimeTicks;
    }

    public float getAlpha(float partialTick) {
        float age = Math.min(lifetimeTicks, ageTicks + Math.max(0.0f, partialTick));
        for (int i = 0; i < flashWindows.size(); i++) {
            FlashWindow window = flashWindows.get(i);
            if (age < window.startTick() || age >= window.endTick()) {
                continue;
            }
            float duration = Math.max(1.0f, window.endTick() - window.startTick());
            float progress = (age - window.startTick()) / duration;
            float pulseAlpha = 1.0f - progress * 0.55f;
            if (i > 0) {
                pulseAlpha *= 0.85f;
            }
            return Math.max(MIN_VISIBLE_ALPHA, pulseAlpha);
        }
        return 0.0f;
    }

    private static List<Segment> createSegments(float x, float y, float z, Random sourceRandom) {
        Random random = sourceRandom == null ? new Random(0L) : sourceRandom;
        float topY = Math.min(Chunk.HEIGHT - 0.5f, y + 24.0f + random.nextInt(16));
        float stepHeight = Math.max(1.0f, (topY - y) / MAIN_STEPS);
        float[] xs = new float[MAIN_STEPS + 1];
        float[] ys = new float[MAIN_STEPS + 1];
        float[] zs = new float[MAIN_STEPS + 1];

        xs[0] = x;
        ys[0] = topY;
        zs[0] = z;
        for (int i = 1; i <= MAIN_STEPS; i++) {
            float falloff = i / (float) MAIN_STEPS;
            xs[i] = x + (random.nextFloat() - 0.5f) * 2.2f * falloff;
            ys[i] = topY - stepHeight * i;
            zs[i] = z + (random.nextFloat() - 0.5f) * 2.2f * falloff;
        }
        xs[MAIN_STEPS] = x;
        ys[MAIN_STEPS] = y;
        zs[MAIN_STEPS] = z;

        ArrayList<Segment> segments = new ArrayList<>();
        for (int i = 0; i < MAIN_STEPS; i++) {
            segments.add(new Segment(xs[i], ys[i], zs[i], xs[i + 1], ys[i + 1], zs[i + 1]));
        }
        for (int i = 0; i < BRANCH_COUNT; i++) {
            int anchor = 1 + random.nextInt(MAIN_STEPS - 1);
            float length = BRANCH_LENGTH_MIN + random.nextFloat() * BRANCH_LENGTH_RANDOM;
            float angle = random.nextFloat() * (float) Math.PI * 2.0f;
            float endX = xs[anchor] + (float) Math.cos(angle) * length;
            float endY = ys[anchor] - random.nextFloat() * 3.0f;
            float endZ = zs[anchor] + (float) Math.sin(angle) * length;
            segments.add(new Segment(xs[anchor], ys[anchor], zs[anchor], endX, endY, endZ));
        }
        return segments;
    }

    private static List<FlashWindow> createFlashWindows(Random random) {
        ArrayList<FlashWindow> windows = new ArrayList<>();
        windows.add(new FlashWindow(0.0f, INITIAL_VISIBLE_TICKS));
        int repeatFlashes = random.nextInt(3) + 1;
        int cursor = INITIAL_VISIBLE_TICKS;
        for (int i = 0; i < repeatFlashes; i++) {
            cursor += MIN_REFLASH_DELAY_TICKS + random.nextInt(REFLASH_DELAY_RANDOM_TICKS);
            int duration = MIN_REFLASH_DURATION_TICKS + random.nextInt(REFLASH_DURATION_RANDOM_TICKS);
            windows.add(new FlashWindow(cursor, cursor + duration));
            cursor += duration;
        }
        return windows;
    }
}
