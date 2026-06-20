package com.craftzero.progression;

import com.craftzero.world.BlockType;
import com.craftzero.world.World;

public final class BookshelfPower {
    private BookshelfPower() {
    }

    public static int count(World world, int tableX, int tableY, int tableZ) {
        if (world == null) {
            return 0;
        }
        int shelves = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) != 2 && Math.abs(dz) != 2) {
                    continue;
                }
                if (dx != 0 && dz != 0 && Math.abs(dx) != 2 && Math.abs(dz) != 2) {
                    continue;
                }
                int gapX = tableX + Integer.signum(dx);
                int gapZ = tableZ + Integer.signum(dz);
                if (world.getBlockIfLoaded(gapX, tableY, gapZ, BlockType.AIR) != BlockType.AIR
                        || world.getBlockIfLoaded(gapX, tableY + 1, gapZ, BlockType.AIR) != BlockType.AIR) {
                    continue;
                }
                for (int dy = 0; dy <= 1; dy++) {
                    if (world.getBlockIfLoaded(tableX + dx, tableY + dy, tableZ + dz, BlockType.AIR)
                            == BlockType.BOOKSHELF) {
                        shelves++;
                    }
                }
            }
        }
        return Math.min(30, shelves);
    }
}
