package com.craftzero.world;

import com.craftzero.physics.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * A block-local shape made from one or more cuboids.
 *
 * Keeping this object explicit lets rendering, ray selection, collision,
 * placement, support checks, and future block-state logic share one shape API.
 */
public final class VoxelShape {
    public static final VoxelShape EMPTY = new VoxelShape(List.of());
    public static final VoxelShape FULL = new VoxelShape(List.of(BlockShape.FULL));

    private final List<BlockShape.Cuboid> boxes;

    private VoxelShape(List<BlockShape.Cuboid> boxes) {
        this.boxes = List.copyOf(boxes);
    }

    public static VoxelShape of(List<BlockShape.Cuboid> boxes) {
        if (boxes == null || boxes.isEmpty()) {
            return EMPTY;
        }
        if (boxes.size() == 1 && boxes.get(0).isFullCube()) {
            return FULL;
        }
        return new VoxelShape(boxes);
    }

    public List<BlockShape.Cuboid> boxes() {
        return boxes;
    }

    public boolean isEmpty() {
        return boxes.isEmpty();
    }

    public boolean isFullCube() {
        return boxes.size() == 1 && boxes.get(0).isFullCube();
    }

    public List<AABB> toAabbs(int x, int y, int z) {
        if (boxes.isEmpty()) {
            return List.of();
        }
        List<AABB> result = new ArrayList<>(boxes.size());
        for (BlockShape.Cuboid box : boxes) {
            result.add(box.toAabb(x, y, z));
        }
        return result;
    }
}
