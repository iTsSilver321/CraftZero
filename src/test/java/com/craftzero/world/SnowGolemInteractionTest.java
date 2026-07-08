package com.craftzero.world;

import com.craftzero.entity.ThrownItemEntity;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.entity.mob.SnowGolem;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.inventory.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SnowGolemInteractionTest {
    @Test
    @DisplayName("Pumpkin over two snow blocks should create a Snow Golem and consume the blocks")
    void pumpkinStackCreatesSnowGolem() {
        World world = new World(6101L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.SNOW, 0);
            world.setBlock(0, 71, 0, BlockType.SNOW, 0);
            world.setBlock(0, 72, 0, BlockType.PUMPKIN, 0);

            assertSame(BlockType.AIR, world.getBlock(0, 70, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 72, 0));

            world.updateEntities(1.0f / 20.0f);

            SnowGolem golem = world.getEntities().stream()
                    .filter(SnowGolem.class::isInstance)
                    .map(SnowGolem.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertSame(MobDefinition.SNOW_GOLEM, golem.getDefinition());
            assertEquals(0.5f, golem.getX(), 0.0001f);
            assertEquals(70.0f, golem.getY(), 0.0001f);
            assertEquals(0.5f, golem.getZ(), 0.0001f);
            assertEquals(120, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SNOW_SHOVEL)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Jack-o-lantern should use the same Release-era snow golem construction path")
    void jackOLanternStackCreatesSnowGolem() {
        World world = new World(6102L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.SNOW, 0);
            world.setBlock(0, 71, 0, BlockType.SNOW, 0);
            world.setBlock(0, 72, 0, BlockType.JACK_O_LANTERN, 0);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(world.getEntities().stream().anyMatch(SnowGolem.class::isInstance));
            assertSame(BlockType.AIR, world.getBlock(0, 72, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Snow Golems should throw snowballs at nearby hostile mobs")
    void snowGolemThrowsSnowballsAtHostiles() {
        World world = new World(6103L);
        try {
            for (int x = 0; x <= 6; x++) {
                world.setBlock(x, 69, 0, BlockType.STONE, 0);
                world.setBlock(x, 70, 0, BlockType.AIR, 0);
                world.setBlock(x, 71, 0, BlockType.AIR, 0);
                world.setBlock(x, 72, 0, BlockType.AIR, 0);
            }
            SnowGolem golem = new SnowGolem();
            golem.setPosition(0.5f, 70.0f, 0.5f);
            Zombie zombie = new Zombie();
            zombie.setPosition(5.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(golem, zombie));

            world.updateEntities(1.0f / 20.0f);
            world.updateEntities(1.0f / 20.0f);

            ThrownItemEntity snowball = world.getEntities().stream()
                    .filter(ThrownItemEntity.class::isInstance)
                    .map(ThrownItemEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertSame(ItemType.SNOWBALL, snowball.getItemType());
            assertTrue(snowball.getX() > 1.8f, "Snowball should use the faster Release-style throw speed");
            assertTrue(snowball.getY() > 71.1f, "Snowball should arc upward from the golem throw height");
            assertTrue(snowball.getMotionX() > 1.4f);
            assertTrue(snowball.getMotionY() > 0.05f);
            assertTrue(world.drainSoundEvents().stream()
                    .anyMatch(sound -> WorldSoundEvent.BOW.equals(sound.soundId())
                            && sound.volume() == 1.0f));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Snow Golems should leave snow layers only in cool enough biomes")
    void snowGolemLeavesSnowTrailInCoolBiomes() {
        World world = new World(424242L);
        try {
            assertEquals(BiomeType.FOREST, world.getReleaseBiome(0, 0));
            assertTrue(world.getReleaseBiome(0, 0).getTemperature() < 0.8f);
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            SnowGolem golem = new SnowGolem();
            golem.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(golem));

            world.updateEntities(1.0f / 20.0f);

            assertSame(BlockType.SNOW_LAYER, world.getBlock(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Snow Golems should take Release-style heat damage in hot biomes")
    void snowGolemTakesHeatDamageInDesertBiomes() {
        int x = -113 * Chunk.WIDTH + 10;
        int z = -31 * Chunk.DEPTH + 13;
        World world = new World(424242L);
        try {
            assertEquals(BiomeType.DESERT, world.getReleaseBiome(x, z));
            assertTrue(world.getReleaseBiome(x, z).getTemperature() > 1.0f);
            world.setBlock(x, 69, z, BlockType.STONE, 0);
            SnowGolem golem = new SnowGolem();
            golem.setPosition(x + 0.5f, 70.0f, z + 0.5f);
            world.replaceEntities(List.of(golem));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(3.0f, golem.getHealth(), 0.001f);
            assertSame(BlockType.AIR, world.getBlock(x, 70, z));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Snow Golems should take wet damage when touching water")
    void snowGolemTakesDamageWhenWet() {
        World world = new World(6104L);
        try {
            world.setBlock(0, 70, 0, BlockType.WATER, 0);
            SnowGolem golem = new SnowGolem();
            golem.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(golem));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(3.0f, golem.getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Snow Golems should take rain damage under open sky")
    void snowGolemTakesRainDamageUnderOpenSky() {
        World world = new World(6105L);
        try {
            world.setWeatherState("rain");
            int[] pos = findRainBiome(world);
            prepareOpenColumn(world, pos[0], pos[1]);

            SnowGolem golem = new SnowGolem();
            golem.setPosition(pos[0] + 0.5f, 100.0f, pos[1] + 0.5f);
            world.replaceEntities(List.of(golem));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(3.0f, golem.getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    private static int[] findRainBiome(World world) {
        for (int x = -64; x <= 64; x += 8) {
            for (int z = -64; z <= 64; z += 8) {
                BiomeType biome = world.getReleaseBiome(x, z);
                if (!biome.canFreezeWater() && biome.getTemperature() < 1.0f) {
                    return new int[] { x, z };
                }
            }
        }
        throw new AssertionError("No non-frozen rain biome found near origin");
    }

    private static void prepareOpenColumn(World world, int x, int z) {
        world.getChunkNow(Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH));
        world.setBlock(x, 99, z, BlockType.STONE, 0);
        for (int y = 100; y < 128; y++) {
            world.setBlock(x, y, z, BlockType.AIR, 0);
        }
    }
}
