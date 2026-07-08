package com.craftzero.ui;

import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.tile.BlockPos;
import com.craftzero.world.tile.TileEntity;
import org.joml.Vector3f;
import org.joml.Vector3i;

final class BlockContainerValidity {
    private static final double MAX_USE_DISTANCE_SQUARED = 64.0D;

    private BlockContainerValidity() {
    }

    static boolean sameTileWithinUseDistance(World world, TileEntity tile, Player player, BlockType... validBlocks) {
        if (world == null || tile == null || player == null) {
            return false;
        }
        BlockPos pos = tile.getPos();
        if (world.getTileEntity(pos.x(), pos.y(), pos.z()) != tile) {
            return false;
        }
        return hasValidBlock(world, pos.x(), pos.y(), pos.z(), validBlocks)
                && withinUseDistance(player, pos.x(), pos.y(), pos.z());
    }

    static boolean sameBlockWithinUseDistance(World world, Vector3i pos, Player player, BlockType... validBlocks) {
        if (world == null || pos == null || player == null) {
            return false;
        }
        return hasValidBlock(world, pos.x, pos.y, pos.z, validBlocks)
                && withinUseDistance(player, pos.x, pos.y, pos.z);
    }

    private static boolean hasValidBlock(World world, int x, int y, int z, BlockType... validBlocks) {
        BlockType current = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        for (BlockType validBlock : validBlocks) {
            if (current == validBlock) {
                return true;
            }
        }
        return false;
    }

    private static boolean withinUseDistance(Player player, int x, int y, int z) {
        Vector3f playerPos = player.getPosition();
        double dx = playerPos.x - (x + 0.5D);
        double dy = playerPos.y - (y + 0.5D);
        double dz = playerPos.z - (z + 0.5D);
        return dx * dx + dy * dy + dz * dz <= MAX_USE_DISTANCE_SQUARED;
    }
}
