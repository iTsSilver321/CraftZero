package com.craftzero.world.tile;

import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.Chunk;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MonsterSpawnerTileEntityTest {
    @Test
    @DisplayName("Spawner should spawn the configured mob when the player is nearby and space is clear")
    void spawnsConfiguredMobNearPlayer() {
        World world = new World(601L);
        try {
            world.getChunkNow(0, 0);
            prepareSpawnArea(world, 8, 69, 8);
            Player player = new Player(8.5f, 70.0f, 12.5f);
            world.setPlayer(player);
            world.setBlock(8, 70, 8, BlockType.MOB_SPAWNER);
            MonsterSpawnerTileEntity spawner = (MonsterSpawnerTileEntity) world.getTileEntity(8, 70, 8);
            spawner.setMobDefinition(MobDefinition.SKELETON);

            for (int i = 0; i < 12 && world.getEntities().isEmpty(); i++) {
                spawner.setDelay(0);
                spawner.tick(world, 1.0f / 20.0f);
                world.updateEntities(1.0f / 20.0f);
            }

            assertTrue(world.getEntities().stream()
                    .filter(Mob.class::isInstance)
                    .map(Mob.class::cast)
                    .anyMatch(mob -> mob.getDefinition() == MobDefinition.SKELETON));
        } finally {
            world.cleanup();
        }
    }

    private static void prepareSpawnArea(World world, int centerX, int groundY, int centerZ) {
        for (int x = centerX - 5; x <= centerX + 5; x++) {
            for (int z = centerZ - 5; z <= centerZ + 5; z++) {
                assertTrue(world.isChunkGeneratedForBlock(x, z));
                world.setBlock(x, groundY, z, BlockType.STONE);
                for (int y = groundY + 1; y <= groundY + 4; y++) {
                    world.setBlock(x, y, z, BlockType.AIR);
                }
            }
        }
    }
}
