package com.craftzero.entity.mob;

import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SquidWaterMovementTest {
    @Test
    @DisplayName("Squid should swim underwater without generic surface bobbing")
    void squidStaysUnderwaterWithDedicatedSwimControl() {
        World world = new World(91L);
        try {
            for (int x = -16; x <= 16; x++) {
                for (int z = -16; z <= 16; z++) {
                    world.setBlock(x, 59, z, BlockType.STONE);
                    for (int y = 60; y <= 76; y++) {
                        world.setBlock(x, y, z, BlockType.WATER, 0);
                    }
                }
            }
            Squid squid = new Squid();
            squid.setPosition(0.5f, 62.0f, 0.5f);
            world.spawnEntity(squid);

            for (int i = 0; i < 80; i++) {
                world.updateEntities(1.0f / 20.0f);
            }

            assertTrue(squid.isInWater(), "Squid should remain in water instead of bobbing/flopping out");
            assertTrue(squid.getY() < 76.0f, "Squid should not be forced to the water surface by generic bobbing");
        } finally {
            world.cleanup();
        }
    }
}
