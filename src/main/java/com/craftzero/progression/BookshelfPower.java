package com.craftzero.progression;

import com.craftzero.world.BlockType;
import com.craftzero.world.World;

public final class BookshelfPower {
    private static final int MAX_POWER = 30;

    private BookshelfPower() {
    }

    public static int count(World world, int tableX, int tableY, int tableZ) {
        if (world == null) {
            return 0;
        }
        int shelves = 0;
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (!hasOpenGap(world, tableX + dx, tableY, tableZ + dz)) {
                    continue;
                }
                shelves += shelfPowerAt(world, tableX + dx * 2, tableY, tableZ + dz * 2);
                if (dx != 0 && dz != 0) {
                    shelves += shelfPowerAt(world, tableX + dx * 2, tableY, tableZ + dz);
                    shelves += shelfPowerAt(world, tableX + dx, tableY, tableZ + dz * 2);
                }
            }
        }
        return Math.min(MAX_POWER, shelves);
    }

    private static boolean hasOpenGap(World world, int x, int y, int z) {
        return world.getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.AIR
                && world.getBlockIfLoaded(x, y + 1, z, BlockType.AIR) == BlockType.AIR;
    }

    private static int shelfPowerAt(World world, int x, int y, int z) {
        int shelves = 0;
        for (int dy = 0; dy <= 1; dy++) {
            if (world.getBlockIfLoaded(x, y + dy, z, BlockType.AIR) == BlockType.BOOKSHELF) {
                shelves++;
            }
        }
        return shelves;
    }
}
