package com.craftzero.combat;

import com.craftzero.physics.AABB;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExplosionExposureTest {
    @Test
    @DisplayName("Explosion exposure should drop when a solid wall blocks the target bounds")
    void explosionExposureDropsBehindWall() {
        World world = new World(302L);
        try {
            prepareClearCorridor(world);
            AABB targetBounds = new AABB(4.2f, 100.0f, 0.2f, 4.8f, 101.8f, 0.8f);

            float exposed = ExplosionExposure.sample(world, 0.5f, 101.0f, 0.5f, targetBounds);

            for (int y = 99; y <= 103; y++) {
                world.setBlock(2, y, 0, BlockType.STONE);
            }
            float blocked = ExplosionExposure.sample(world, 0.5f, 101.0f, 0.5f, targetBounds);

            assertEquals(1.0f, exposed, 0.001f);
            assertTrue(blocked < exposed);
            assertEquals(0.0f, blocked, 0.001f);
        } finally {
            world.cleanup();
        }
    }

    private static void prepareClearCorridor(World world) {
        world.getChunkNow(0, 0);
        for (int x = 0; x <= 5; x++) {
            for (int y = 99; y <= 103; y++) {
                world.setBlock(x, y, 0, BlockType.AIR);
            }
        }
    }
}
