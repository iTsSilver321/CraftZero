package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.LivingEntity;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

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

    @Test
    @DisplayName("Squid ink drops should use the Release-style single Looting roll")
    void squidInkDropsUseSingleLootingRoll() {
        World world = new World(92L);
        try {
            Squid squid = new Squid();
            squid.random = new FixedIntRandom(5);
            squid.setPosition(0.5f, 62.0f, 0.5f);
            world.replaceEntities(List.of(squid));
            assertTrue(squid.damage(1.0f, DamageSource.playerAttack(0.0f, 62.0f, 0.0f, 3)));

            squid.dropLoot();

            assertEquals(6, droppedCount(world, ItemType.INK_SAC));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Stranded squid should use the Release water-mob land motion path")
    void strandedSquidUseReleaseLandMotionPath() {
        World world = new World(93L);
        try {
            Squid squid = new Squid();
            squid.setPosition(0.5f, 70.0f, 0.5f);
            squid.setMotion(0.35f, 0.0f, -0.25f);
            world.spawnEntity(squid);

            squid.tick();

            assertEquals(0.0f, squid.getMotionX(), 0.0001f,
                    "Out-of-water squid should stop horizontal movement instead of flopping sideways");
            assertEquals(0.0f, squid.getMotionZ(), 0.0001f);
            assertEquals(-1.8f, squid.getSquidPitch(), 0.0001f,
                    "Source squid land pitch eases toward -90 degrees at 2% per tick");
            assertEquals(Math.abs(Math.sin(0.12f)) * Math.PI * 0.25f, squid.getTentacleAngle(), 0.0001f);

            for (int i = 0; i < LivingEntity.MAX_AIR_TICKS - 1; i++) {
                squid.tick();
            }
            assertEquals(squid.getMaxHealth(), squid.getHealth(), 0.0001f);

            for (int i = 0; i < -LivingEntity.DROWN_DAMAGE_AIR_TICKS; i++) {
                squid.tick();
            }
            assertEquals(squid.getMaxHealth() - LivingEntity.DROWN_DAMAGE, squid.getHealth(), 0.0001f,
                    "Stranded Release-era squid should dry out after the old air counter expires");
            assertEquals(0, squid.getAirTicks());

            for (int i = 0; i < -LivingEntity.DROWN_DAMAGE_AIR_TICKS; i++) {
                squid.tick();
            }
            assertEquals(squid.getMaxHealth() - LivingEntity.DROWN_DAMAGE * 2.0f, squid.getHealth(), 0.0001f,
                    "Dry-out damage should pulse on the same old 20-tick cadence after the first hit");
        } finally {
            world.cleanup();
        }
    }

    private static int droppedCount(World world, ItemType type) {
        return world.getDroppedItems().stream()
                .filter(item -> item.getItemType() == type)
                .mapToInt(item -> item.getCount())
                .sum();
    }

    private static final class FixedIntRandom extends Random {
        private final int value;

        private FixedIntRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return Math.min(value, bound - 1);
        }

        @Override
        public float nextFloat() {
            return 0.5f;
        }
    }
}
