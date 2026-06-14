package com.craftzero.world;

import com.craftzero.entity.mob.MobDefinition;

public record SpawnRule(MobDefinition definition, int weight, int minY, int maxY, boolean waterSpawn) {
    public boolean matches(World world, int x, int y, int z) {
        if (world == null || definition == null || !definition.canSpawnIn(world.getDimension())) {
            return false;
        }
        if (y < minY || y > maxY) {
            return false;
        }
        if (waterSpawn) {
            return world.getBlockIfLoaded(x, y + 1, z, BlockType.AIR).isWater();
        }
        return true;
    }
}
