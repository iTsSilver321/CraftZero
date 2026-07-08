package com.craftzero.graphics;

import com.craftzero.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared Release-era enchanted item glint selection and overlay geometry.
 */
public final class EnchantedItemVisuals {
    private static final float SOURCE_GLINT_TINT = 0.76f;
    private static final float[] GLINT_COLOR = {
            0.50f * SOURCE_GLINT_TINT,
            0.25f * SOURCE_GLINT_TINT,
            0.80f * SOURCE_GLINT_TINT,
            0.58f
    };
    private static final float[] GLINT_WASH = { 0.38f, 0.12f, 0.64f, 0.13f };
    private static final long GLINT_TICK_NANOS = 50_000_000L;
    private static final int GLINT_ANIMATION_PERIOD_TICKS = 60;
    private static final int GLINT_BAND_COUNT = 4;
    private static final float GLINT_BAND_WIDTH_RATIO = 0.22f;
    private static final float GLINT_BAND_SPACING_RATIO = 0.48f;
    private static final int GLINT_TEXTURE_PERIOD_TICKS = 60;
    private static final float GLINT_TEXTURE_REPEAT_PER_ICON = 3.0f;
    private static final float GLINT_TEXTURE_SCROLL_REPEATS = 2.0f;

    private EnchantedItemVisuals() {
    }

    public static boolean shouldDrawGlint(ItemStack stack) {
        return stack != null && !stack.isEmpty() && !stack.getEnchantments().isEmpty();
    }

    public static float[] glintColor() {
        return GLINT_COLOR.clone();
    }

    public static float[] glintWashColor() {
        return GLINT_WASH.clone();
    }

    public static long currentAnimationTick() {
        return System.nanoTime() / GLINT_TICK_NANOS;
    }

    public static float glintPhase() {
        long tick = currentAnimationTick();
        return Math.floorMod(tick, GLINT_TEXTURE_PERIOD_TICKS) / (float) GLINT_TEXTURE_PERIOD_TICKS;
    }

    public static List<Band> glintBands(int x, int y, int size) {
        return glintBands(x, y, size, currentAnimationTick());
    }

    public static List<Band> glintBands(int x, int y, int size, long animationTick) {
        if (size <= 0) {
            return List.of();
        }

        float phase = Math.floorMod(animationTick, GLINT_ANIMATION_PERIOD_TICKS)
                / (float) GLINT_ANIMATION_PERIOD_TICKS;
        float bandWidth = Math.max(2.0f, size * GLINT_BAND_WIDTH_RATIO);
        float spacing = Math.max(3.0f, size * GLINT_BAND_SPACING_RATIO);
        float travel = size + spacing;
        List<Band> bands = new ArrayList<>(GLINT_BAND_COUNT);
        for (int i = 0; i < GLINT_BAND_COUNT; i++) {
            float offset = ((phase * travel) + i * spacing) % travel - spacing;
            Band band = clippedDiagonalBand(x, y, size, offset, bandWidth);
            if (band != null) {
                bands.add(band);
            }
        }
        return List.copyOf(bands);
    }

    public static List<TexturePass> texturePasses(int x, int y, int size) {
        return texturePasses(x, y, size, currentAnimationTick());
    }

    public static List<TexturePass> texturePasses(int x, int y, int size, long animationTick) {
        if (size <= 0) {
            return List.of();
        }
        float phase = Math.floorMod(animationTick, GLINT_TEXTURE_PERIOD_TICKS)
                / (float) GLINT_TEXTURE_PERIOD_TICKS;
        float centerX = x + size * 0.5f;
        float centerY = y + size * 0.5f;
        float drawSize = size * 3.0f;
        float repeat = Math.max(1.0f, size / 16.0f * GLINT_TEXTURE_REPEAT_PER_ICON);
        return List.of(
                texturePass(centerX, centerY, drawSize, -50.0f, phase * GLINT_TEXTURE_SCROLL_REPEATS, repeat),
                texturePass(centerX, centerY, drawSize, 10.0f, -phase * GLINT_TEXTURE_SCROLL_REPEATS, repeat));
    }

    private static TexturePass texturePass(float centerX, float centerY, float size,
            float rotationDegrees, float vOffset, float repeat) {
        float half = size * 0.5f;
        float radians = (float) Math.toRadians(rotationDegrees);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float[] corners = {
                -half, -half,
                half, -half,
                half, half,
                -half, half
        };
        float[] vertices = new float[8];
        for (int i = 0; i < 4; i++) {
            float px = corners[i * 2];
            float py = corners[i * 2 + 1];
            vertices[i * 2] = centerX + px * cos - py * sin;
            vertices[i * 2 + 1] = centerY + px * sin + py * cos;
        }
        return new TexturePass(vertices, 0.0f, vOffset, repeat, vOffset + repeat);
    }

    private static Band clippedDiagonalBand(int x, int y, int size, float offset, float width) {
        float minX = x;
        float minY = y;
        float maxX = x + size;
        float maxY = y + size;
        List<Point> points = List.of(
                new Point(x + offset, y),
                new Point(x + offset + width, y),
                new Point(x + offset + width + size, y + size),
                new Point(x + offset + size, y + size));
        points = clip(points, Edge.LEFT, minX);
        points = clip(points, Edge.RIGHT, maxX);
        points = clip(points, Edge.TOP, minY);
        points = clip(points, Edge.BOTTOM, maxY);
        if (points.size() < 3) {
            return null;
        }

        float[] vertices = new float[points.size() * 2];
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            vertices[i * 2] = point.x();
            vertices[i * 2 + 1] = point.y();
        }
        return new Band(vertices, points.size());
    }

    private static List<Point> clip(List<Point> input, Edge edge, float boundary) {
        if (input.isEmpty()) {
            return input;
        }
        List<Point> output = new ArrayList<>(input.size() + 1);
        Point previous = input.get(input.size() - 1);
        boolean previousInside = inside(previous, edge, boundary);
        for (Point current : input) {
            boolean currentInside = inside(current, edge, boundary);
            if (currentInside) {
                if (!previousInside) {
                    output.add(intersection(previous, current, edge, boundary));
                }
                output.add(current);
            } else if (previousInside) {
                output.add(intersection(previous, current, edge, boundary));
            }
            previous = current;
            previousInside = currentInside;
        }
        return output;
    }

    private static boolean inside(Point point, Edge edge, float boundary) {
        return switch (edge) {
            case LEFT -> point.x() >= boundary;
            case RIGHT -> point.x() <= boundary;
            case TOP -> point.y() >= boundary;
            case BOTTOM -> point.y() <= boundary;
        };
    }

    private static Point intersection(Point from, Point to, Edge edge, float boundary) {
        float dx = to.x() - from.x();
        float dy = to.y() - from.y();
        if (edge == Edge.LEFT || edge == Edge.RIGHT) {
            if (Math.abs(dx) < 0.0001f) {
                return new Point(boundary, from.y());
            }
            float t = (boundary - from.x()) / dx;
            return new Point(boundary, from.y() + dy * t);
        }
        if (Math.abs(dy) < 0.0001f) {
            return new Point(from.x(), boundary);
        }
        float t = (boundary - from.y()) / dy;
        return new Point(from.x() + dx * t, boundary);
    }

    private enum Edge {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    private record Point(float x, float y) {
    }

    public record Band(float[] vertices, int vertexCount) {
        public Band {
            vertices = vertices == null ? new float[0] : vertices.clone();
            vertexCount = Math.max(0, Math.min(vertexCount, vertices.length / 2));
        }

        public float[] copyVertices() {
            return vertices.clone();
        }
    }

    public record TexturePass(float[] vertices, float u1, float v1, float u2, float v2) {
        public TexturePass {
            vertices = vertices == null ? new float[0] : vertices.clone();
        }

        public float[] copyVertices() {
            return vertices.clone();
        }
    }
}
