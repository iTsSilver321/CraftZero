package com.craftzero.graphics;

import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.Dimension;
import com.craftzero.world.World;
import com.craftzero.world.tile.BlockPos;
import org.joml.Vector3f;

/**
 * Shared item texture lookup for HUD, inventory, dropped items, and held items.
 */
public final class ItemTextureResolver {
    private static final float ITEMS_SIZE = 256.0f;
    private static final float CELL_SIZE = 16.0f;
    private static final int BOW_BASE_COL = 5;
    private static final int BOW_BASE_ROW = 1;
    private static final float BOW_PULL_1_PROGRESS = 13.0f / 20.0f;
    private static final float BOW_PULL_2_PROGRESS = 18.0f / 20.0f;
    static final int DYNAMIC_ITEM_FRAME_COUNT = 64;
    private static final float TAU = (float) (Math.PI * 2.0);
    private static final float TICKS_PER_DAY = 24000.0f;

    private ItemTextureResolver() {
    }

    public record DynamicItemState(boolean active, float angleRadians, int frame) {
    }

    public static boolean usesItemsAtlas(ItemType type) {
        return type != null && type.usesItemTexture();
    }

    public static float[] getUv(ItemType type) {
        return getUv(type, 0);
    }

    public static float[] getUv(ItemType type, boolean drawingBow, float useProgress) {
        return getUv(type, bowDrawFrame(type, drawingBow, useProgress));
    }

    static int bowDrawFrame(ItemType type, boolean drawingBow, float useProgress) {
        if (type != ItemType.BOW || !drawingBow || useProgress <= 0.0f) {
            return 0;
        }
        if (useProgress >= BOW_PULL_2_PROGRESS) {
            return 3;
        }
        if (useProgress > BOW_PULL_1_PROGRESS) {
            return 2;
        }
        return 1;
    }

    public static DynamicItemState dynamicItemState(ItemType type, World world, Player player) {
        if (type == ItemType.CLOCK) {
            float angle = clockAngle(world);
            return new DynamicItemState(true, angle, frameForAngle(angle));
        }
        if (type == ItemType.COMPASS) {
            float angle = compassAngle(world, player);
            return new DynamicItemState(true, angle, frameForAngle(angle));
        }
        return new DynamicItemState(false, 0.0f, 0);
    }

    public static DynamicItemState dynamicItemState(ItemType type, World world,
            float viewerX, float viewerZ, float viewerYawDegrees) {
        if (type == ItemType.CLOCK) {
            float angle = clockAngle(world);
            return new DynamicItemState(true, angle, frameForAngle(angle));
        }
        if (type == ItemType.COMPASS) {
            float angle = compassAngle(world, viewerX, viewerZ, viewerYawDegrees);
            return new DynamicItemState(true, angle, frameForAngle(angle));
        }
        return new DynamicItemState(false, 0.0f, 0);
    }

    static float clockAngle(World world) {
        if (world == null || world.getDimension() != Dimension.OVERWORLD) {
            return invalidDimensionAngle(world);
        }
        DayCycleManager dayCycle = world.getDayCycleManager();
        float time = dayCycle == null ? 0.0f : dayCycle.getTime();
        return normalizeRadians((time / TICKS_PER_DAY) * TAU);
    }

    static float compassAngle(World world, Player player) {
        if (world == null || player == null || world.getDimension() != Dimension.OVERWORLD) {
            return invalidDimensionAngle(world);
        }
        BlockPos spawn = world.getWorldSpawn();
        Vector3f position = player.getPosition();
        float dx = spawn.x() + 0.5f - position.x;
        float dz = spawn.z() + 0.5f - position.z;
        if (dx * dx + dz * dz < 0.0001f) {
            return 0.0f;
        }
        float targetYaw = (float) Math.atan2(dx, -dz);
        float playerYaw = (float) Math.toRadians(player.getCamera().getYaw());
        return normalizeRadians(targetYaw - playerYaw);
    }

    private static float compassAngle(World world, float viewerX, float viewerZ, float viewerYawDegrees) {
        if (world == null || world.getDimension() != Dimension.OVERWORLD) {
            return invalidDimensionAngle(world);
        }
        BlockPos spawn = world.getWorldSpawn();
        float dx = spawn.x() + 0.5f - viewerX;
        float dz = spawn.z() + 0.5f - viewerZ;
        if (dx * dx + dz * dz < 0.0001f) {
            return 0.0f;
        }
        float targetYaw = (float) Math.atan2(dx, -dz);
        float viewerYaw = (float) Math.toRadians(viewerYawDegrees);
        return normalizeRadians(targetYaw - viewerYaw);
    }

    static int frameForAngle(float angleRadians) {
        float normalized = normalizeRadians(angleRadians);
        int frame = Math.round(normalized / TAU * DYNAMIC_ITEM_FRAME_COUNT);
        return Math.floorMod(frame, DYNAMIC_ITEM_FRAME_COUNT);
    }

    private static float invalidDimensionAngle(World world) {
        long tick = world == null ? 0L : world.getBlockTickClock();
        long seed = world == null ? 0L : world.getSeed();
        int frame = (int) Math.floorMod(seed + tick * 7L, DYNAMIC_ITEM_FRAME_COUNT);
        return frame * TAU / DYNAMIC_ITEM_FRAME_COUNT;
    }

    private static float normalizeRadians(float angleRadians) {
        float angle = angleRadians % TAU;
        return angle < 0.0f ? angle + TAU : angle;
    }

    static float[] getUv(ItemType type, int bowDrawFrame) {
        if (type == null) {
            return new float[] { 0, 0, 0, 0 };
        }
        if (type == ItemType.BOW && bowDrawFrame > 0) {
            int frame = Math.max(1, Math.min(3, bowDrawFrame));
            return getItemsUv(BOW_BASE_COL + frame, BOW_BASE_ROW);
        }
        if (usesItemsAtlas(type)) {
            int[] pos = type.getItemTexturePos();
            return getItemsUv(pos[0], pos[1]);
        }
        return type.getTextureCoords(com.craftzero.world.Block.FACE_SOUTH);
    }

    public static float[] getItemsUv(int col, int row) {
        float u1 = col * CELL_SIZE / ITEMS_SIZE;
        float v1 = row * CELL_SIZE / ITEMS_SIZE;
        float u2 = (col + 1) * CELL_SIZE / ITEMS_SIZE;
        float v2 = (row + 1) * CELL_SIZE / ITEMS_SIZE;
        return new float[] { u1, v1, u2, v2 };
    }
}
