package com.craftzero.world;

import com.craftzero.entity.mob.Creeper;
import com.craftzero.entity.mob.Pig;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.entity.mob.ZombiePigman;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldWeatherTest {

    @Test
    @DisplayName("World weather should normalize and rain only in open Overworld sky")
    void weatherRainsOnlyInOpenOverworldSky() {
        World world = new World(9031L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            world.setWeatherState("THUNDER");
            int[] pos = findRainBiome(world);
            prepareOpenColumn(world, pos[0], pos[1]);

            assertEquals("thunder", world.getWeatherState());
            assertTrue(world.isRaining());
            assertTrue(world.isThundering());
            assertTrue(world.isRainingAt(pos[0], 100, pos[1]));

            world.setBlock(pos[0], 104, pos[1], BlockType.STONE, 0);

            assertFalse(world.isRainingAt(pos[0], 100, pos[1]));
        } finally {
            world.cleanup();
        }

        World nether = new World(9032L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            nether.setWeatherState("rain");

            assertEquals("rain", nether.getWeatherState());
            assertFalse(nether.isRaining());
            assertFalse(nether.isRainingAt(0, 100, 0));
        } finally {
            nether.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid weather states should fall back to clear")
    void invalidWeatherFallsBackToClear() {
        World world = new World(9033L);
        try {
            world.setWeatherState("drizzle");

            assertEquals("clear", world.getWeatherState());
            assertFalse(world.isRaining());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Weather timers should toggle rain and thunder like Release-era world weather")
    void weatherTimersToggleRainAndThunder() {
        World world = new World(9034L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            world.setWeatherState("clear", 1, 100);

            world.updateWeather(1.0f / 20.0f);

            assertEquals("rain", world.getWeatherState());
            assertTrue(world.isRaining());
            assertFalse(world.isThundering());
            assertTrue(world.getRainTime() >= 12000);

            world.setWeatherState("rain", 100, 1);
            world.updateWeather(1.0f / 20.0f);

            assertEquals("thunder", world.getWeatherState());
            assertTrue(world.isThundering());
            assertTrue(world.getThunderTime() >= 3600);

            world.setWeatherState("thunder", 1, 1);
            world.updateWeather(1.0f / 20.0f);

            assertEquals("clear", world.getWeatherState());
            assertFalse(world.isRaining());
            assertFalse(world.isThundering());
            assertTrue(world.getRainTime() >= 12000);
            assertTrue(world.getThunderTime() >= 12000);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Weather strength should ramp and interpolate for Release-style rendering")
    void weatherStrengthRampsAndInterpolates() {
        World world = new World(9035L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            world.setWeatherState("rain", 1000, 1000);

            assertEquals(0.0f, world.getRainStrength(1.0f), 0.0001f);

            world.updateWeather(1.0f / 20.0f);

            assertEquals(0.0f, world.getRainStrength(0.0f), 0.0001f);
            assertEquals(0.005f, world.getRainStrength(0.5f), 0.0001f);
            assertEquals(0.01f, world.getRainStrength(1.0f), 0.0001f);
            assertEquals(0.0f, world.getThunderStrength(1.0f), 0.0001f);

            world.updateWeather(99.0f / 20.0f);

            assertEquals(1.0f, world.getRainStrength(1.0f), 0.0001f);

            world.setWeatherState("clear", 1000, 1000);
            world.updateWeather(1.0f / 20.0f);

            assertEquals(0.995f, world.getRainStrength(0.5f), 0.0001f);
            assertEquals(0.99f, world.getRainStrength(1.0f), 0.0001f);

            world.setWeatherState("thunder", 1000, 1000);
            world.updateWeather(1.0f / 20.0f);

            assertEquals(0.005f, world.getThunderStrength(0.5f), 0.0001f);
            assertEquals(0.01f, world.getThunderStrength(1.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Lightning should strike only exposed thunderstorm columns and apply Release-style effects")
    void lightningStrikeRequiresThunderAndOpenSky() {
        World world = new World(9036L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] pos = findRainBiome(world);
            prepareOpenColumn(world, pos[0], pos[1]);
            Zombie zombie = new Zombie();
            zombie.setPosition(pos[0] + 0.5f, 100.0f, pos[1] + 0.5f);
            float beforeHealth = zombie.getHealth();
            world.replaceEntities(List.of(zombie));

            world.setWeatherState("rain");
            assertFalse(world.strikeLightningAt(pos[0], 100, pos[1]));
            assertTrue(world.drainSoundEvents().isEmpty());
            assertTrue(world.getLightningBolts().isEmpty());
            assertEquals(0.0f, world.getLightningFlashStrength(1.0f), 0.0001f);
            assertEquals(beforeHealth, zombie.getHealth(), 0.001f);
            assertFalse(zombie.isOnFire());

            world.setWeatherState("thunder");
            assertTrue(world.strikeLightningAt(pos[0], 100, pos[1]));

            assertEquals(1.0f, world.getLightningFlashStrength(1.0f), 0.0001f);
            assertEquals(1, world.getLightningBolts().size());
            WorldLightningBolt bolt = world.getLightningBolts().get(0);
            assertEquals(pos[0] + 0.5f, bolt.getX(), 0.0001f);
            assertEquals(100.0f, bolt.getY(), 0.0001f);
            assertEquals(pos[1] + 0.5f, bolt.getZ(), 0.0001f);
            assertTrue(bolt.getSegments().size() >= 11);
            assertTrue(bolt.getSegments().stream()
                    .anyMatch(segment -> Math.abs(segment.x2() - (pos[0] + 0.5f)) < 0.0001f
                            && Math.abs(segment.y2() - 100.0f) < 0.0001f
                            && Math.abs(segment.z2() - (pos[1] + 0.5f)) < 0.0001f));
            assertTrue(bolt.getSegments().stream().anyMatch(segment -> segment.y1() > segment.y2()));
            assertTrue(bolt.getAlpha(0.0f) > 0.0f);
            world.updateParticles(0.19f);
            assertEquals(1, world.getLightningBolts().size());
            assertTrue(world.getLightningFlashStrength(1.0f) > 0.0f);
            float remainingBoltLifetime = (bolt.getLifetimeTicks() + 1.0f - bolt.getAgeTicks()) / 20.0f;
            world.updateParticles(remainingBoltLifetime);
            assertTrue(world.getLightningBolts().isEmpty());
            assertEquals(0.0f, world.getLightningFlashStrength(1.0f), 0.0001f);

            assertEquals(BlockType.FIRE, world.getBlock(pos[0], 100, pos[1]));
            assertTrue(zombie.isOnFire());
            assertEquals(beforeHealth - 5.0f, zombie.getHealth(), 0.001f);

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertTrue(sounds.size() >= 2);
            assertEquals(WorldSoundEvent.WEATHER_THUNDER, sounds.get(0).soundId());
            assertEquals(WorldSoundEvent.EXPLOSION, sounds.get(1).soundId());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Lightning should reject covered columns")
    void lightningRejectsCoveredColumns() {
        World world = new World(9037L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] pos = findRainBiome(world);
            prepareOpenColumn(world, pos[0], pos[1]);
            world.setBlock(pos[0], 104, pos[1], BlockType.STONE, 0);
            world.setWeatherState("thunder");

            assertFalse(world.strikeLightningAt(pos[0], 100, pos[1]));
            assertEquals(BlockType.AIR, world.getBlock(pos[0], 100, pos[1]));
            assertTrue(world.drainSoundEvents().isEmpty());
            assertTrue(world.getLightningBolts().isEmpty());
            assertEquals(0.0f, world.getLightningFlashStrength(1.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Lightning should transform pigs and charge creepers")
    void lightningTransformsPigsAndChargesCreepers() {
        World world = new World(9036L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] pos = findRainBiome(world);
            prepareOpenColumn(world, pos[0], pos[1]);
            Pig pig = new Pig();
            pig.setPosition(pos[0] + 0.5f, 100.0f, pos[1] + 0.5f);
            Creeper creeper = new Creeper();
            creeper.setPosition(pos[0] + 1.5f, 100.0f, pos[1] + 0.5f);
            world.replaceEntities(List.of(pig, creeper));
            world.setWeatherState("thunder");

            assertTrue(world.strikeLightningAt(pos[0], 100, pos[1]));

            assertTrue(creeper.isPowered());
            assertEquals(6.0f, creeper.getExplosionPower(), 0.0001f);
            assertTrue(creeper.isOnFire());
            assertFalse(world.getEntities().stream().anyMatch(Pig.class::isInstance));
            ZombiePigman pigman = world.getEntities().stream()
                    .filter(ZombiePigman.class::isInstance)
                    .map(ZombiePigman.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(pos[0] + 0.5f, pigman.getX(), 0.0001f);
            assertEquals(100.0f, pigman.getY(), 0.0001f);
            assertEquals(pos[1] + 0.5f, pigman.getZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Natural lightning should roll generated active chunks during thunderstorms")
    void naturalLightningRollsGeneratedActiveChunks() {
        World world = new World(3954L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] chunkPos = findRainBiomeChunk(world);
            prepareOpenChunk(world, chunkPos[0], chunkPos[1]);
            Player player = new Player(chunkPos[0] * Chunk.WIDTH + 8.5f, 100.0f,
                    chunkPos[1] * Chunk.DEPTH + 8.5f);
            world.setPlayer(player);
            world.setWeatherState("thunder", 1000, 1000);

            world.updateWeather(1.0f / 20.0f);

            assertEquals(1, world.getLightningBolts().size());
            WorldLightningBolt bolt = world.getLightningBolts().get(0);
            int struckX = (int) Math.floor(bolt.getX());
            int struckZ = (int) Math.floor(bolt.getZ());
            assertTrue(struckX >= chunkPos[0] * Chunk.WIDTH);
            assertTrue(struckX < (chunkPos[0] + 1) * Chunk.WIDTH);
            assertTrue(struckZ >= chunkPos[1] * Chunk.DEPTH);
            assertTrue(struckZ < (chunkPos[1] + 1) * Chunk.DEPTH);
            assertEquals(100.0f, bolt.getY(), 0.0001f);
            assertSame(BlockType.FIRE, world.getBlock(struckX, 100, struckZ));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Active rain should emit precipitation particles and ambience near the player")
    void rainEmitsPrecipitationParticlesAndAmbience() {
        World world = new World(9039L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] pos = findRainBiomePatch(world);
            prepareOpenArea(world, pos[0], pos[1], 4);
            Player player = new Player(pos[0] + 0.5f, 100.0f, pos[1] + 0.5f);
            world.setPlayer(player);
            world.setWeatherState("rain", 1000, 1000);

            world.updateWeather(1.0f / 20.0f);

            WorldParticle particle = world.getParticles().stream()
                    .filter(candidate -> candidate.getType() == WorldParticle.Type.RAIN)
                    .findFirst()
                    .orElseThrow();
            assertEquals(100.10f, particle.getRenderY(0.0f), 0.0001f);
            assertTrue(particle.getLifetimeTicks() >= 8.0f);
            assertTrue(particle.getLifetimeTicks() <= 40.0f);
            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertTrue(sounds.stream()
                    .anyMatch(sound -> WorldSoundEvent.WEATHER_RAIN.equals(sound.soundId())));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Rain should not fill cauldrons in Release 1.0")
    void rainDoesNotFillCauldrons() {
        World world = new World(86420L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] pos = findRainBiomePatch(world);
            prepareOpenArea(world, pos[0], pos[1], 2);
            world.setWeatherState("rain", 1000, 1000);
            world.setBlock(pos[0], 100, pos[1], BlockType.CAULDRON, 0);

            assertFalse(world.tryFillCauldronFromRainAt(pos[0], 100, pos[1]));
            assertEquals(0, world.getCauldronLevel(pos[0], 100, pos[1]));

            world.setBlock(pos[0] + 1, 100, pos[1], BlockType.CAULDRON, 0);
            world.setBlock(pos[0] + 1, 104, pos[1], BlockType.STONE, 0);
            assertFalse(world.tryFillCauldronFromRainAt(pos[0] + 1, 100, pos[1]));
            assertEquals(0, world.getCauldronLevel(pos[0] + 1, 100, pos[1]));

            world.setBlock(pos[0] + 2, 100, pos[1], BlockType.CAULDRON, 0);
            world.setWeatherState("clear", 1000, 1000);
            assertFalse(world.tryFillCauldronFromRainAt(pos[0] + 2, 100, pos[1]));
            assertEquals(0, world.getCauldronLevel(pos[0] + 2, 100, pos[1]));

            int[] snowPos = findSnowBiome(world);
            prepareOpenColumn(world, snowPos[0], snowPos[1]);
            world.setBlock(snowPos[0], 100, snowPos[1], BlockType.CAULDRON, 0);
            world.setWeatherState("rain", 1000, 1000);

            assertTrue(world.isSnowingAt(snowPos[0], 101, snowPos[1]));
            assertFalse(world.tryFillCauldronFromRainAt(snowPos[0], 100, snowPos[1]));
            assertEquals(0, world.getCauldronLevel(snowPos[0], 100, snowPos[1]));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Active rain column ticks should leave cauldrons dry")
    void rainColumnTicksLeaveCauldronsDry() {
        World world = new World(9041L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] pos = findRainBiome(world);
            prepareOpenColumn(world, pos[0], pos[1]);
            world.setBlock(pos[0], 100, pos[1], BlockType.CAULDRON, 0);
            world.setWeatherState("rain", 1000, 1000);

            assertFalse(world.tickCauldronRainFillAtColumn(pos[0], pos[1], fixedIntRandom(0)));
            assertEquals(0, world.getCauldronLevel(pos[0], 100, pos[1]));

            assertFalse(world.tickCauldronRainFillAtColumn(pos[0], pos[1], fixedIntRandom(1)));
            assertEquals(0, world.getCauldronLevel(pos[0], 100, pos[1]));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Snowfall should place snow layers only in exposed frozen precipitation columns")
    void snowfallAccumulatesOnlyInOpenFrozenColumns() {
        World world = new World(86420L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] snowPos = findSnowBiome(world);
            prepareOpenColumn(world, snowPos[0], snowPos[1]);
            world.setWeatherState("rain", 1000, 1000);

            assertTrue(world.isSnowingAt(snowPos[0], 100, snowPos[1]));
            assertTrue(world.tryAccumulateSnowAt(snowPos[0], 100, snowPos[1]));
            assertSame(BlockType.SNOW_LAYER, world.getBlock(snowPos[0], 100, snowPos[1]));
            assertEquals(0, world.getBlockMetadata(snowPos[0], 100, snowPos[1]));
            assertTrue(world.hasScheduledBlockTick(snowPos[0], 100, snowPos[1], BlockType.SNOW_LAYER));
            assertFalse(world.tryAccumulateSnowAt(snowPos[0], 100, snowPos[1]));

            world.setBlock(snowPos[0], 100, snowPos[1], BlockType.AIR, 0);
            world.setBlock(snowPos[0], 104, snowPos[1], BlockType.STONE, 0);
            assertFalse(world.tryAccumulateSnowAt(snowPos[0], 100, snowPos[1]));

            world.setBlock(snowPos[0], 104, snowPos[1], BlockType.AIR, 0);
            world.setBlock(snowPos[0] + 1, 100, snowPos[1], BlockType.TORCH, Block.FACE_WEST);
            assertTrue(world.getBlockLight(snowPos[0], 100, snowPos[1]) >= 10);
            assertFalse(world.tryAccumulateSnowAt(snowPos[0], 100, snowPos[1]));

            int[] rainPos = findRainBiome(world);
            prepareOpenColumn(world, rainPos[0], rainPos[1]);
            assertFalse(world.isSnowingAt(rainPos[0], 100, rainPos[1]));
            assertFalse(world.tryAccumulateSnowAt(rainPos[0], 100, rainPos[1]));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Snowfall column accumulation should use the precipitation surface")
    void snowfallColumnAccumulationUsesPrecipitationSurface() {
        World world = new World(86420L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] snowPos = findSnowBiome(world);
            prepareOpenColumn(world, snowPos[0], snowPos[1]);
            world.setWeatherState("rain", 1000, 1000);

            assertTrue(world.tryAccumulateSnowAtColumn(snowPos[0], snowPos[1]));
            assertSame(BlockType.SNOW_LAYER, world.getBlock(snowPos[0], 100, snowPos[1]));
            assertFalse(world.tryAccumulateSnowAtColumn(snowPos[0], snowPos[1]));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Snowing weather should freeze only source water in exposed frozen columns")
    void snowingWeatherFreezesOnlySourceWaterInFrozenColumns() {
        World world = new World(86420L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] snowPos = findSnowBiome(world);
            prepareOpenWaterColumn(world, snowPos[0], snowPos[1]);
            world.setWeatherState("rain", 1000, 1000);

            assertTrue(world.isSnowingAt(snowPos[0], 100, snowPos[1]));
            assertTrue(world.tryFreezeWaterAt(snowPos[0], 100, snowPos[1]));
            assertSame(BlockType.ICE, world.getBlock(snowPos[0], 100, snowPos[1]));
            assertFalse(world.tryFreezeWaterAt(snowPos[0], 100, snowPos[1]));

            prepareOpenColumn(world, snowPos[0] + 1, snowPos[1]);
            world.setBlock(snowPos[0] + 1, 100, snowPos[1], BlockType.FLOWING_WATER, 3);
            assertFalse(world.tryFreezeWaterAt(snowPos[0] + 1, 100, snowPos[1]));
            assertSame(BlockType.FLOWING_WATER, world.getBlock(snowPos[0] + 1, 100, snowPos[1]));
            assertEquals(3, world.getBlockMetadata(snowPos[0] + 1, 100, snowPos[1]));

            int[] rainPos = findRainBiome(world);
            prepareOpenWaterColumn(world, rainPos[0], rainPos[1]);
            assertFalse(world.isSnowingAt(rainPos[0], 100, rainPos[1]));
            assertFalse(world.tryFreezeWaterAt(rainPos[0], 100, rainPos[1]));
            assertSame(BlockType.WATER, world.getBlock(rainPos[0], 100, rainPos[1]));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Snowing weather should honor block-light and lily-pad freeze edges")
    void snowingWeatherFreezeHonorsLightAndLilyPads() {
        World world = new World(86420L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] snowPos = findSnowBiome(world);
            prepareOpenWaterColumn(world, snowPos[0], snowPos[1]);
            world.setWeatherState("rain", 1000, 1000);

            world.setBlock(snowPos[0] + 1, 100, snowPos[1], BlockType.TORCH, Block.FACE_WEST);
            assertTrue(world.getBlockLight(snowPos[0], 100, snowPos[1]) >= 10);
            assertFalse(world.tryFreezeWaterAt(snowPos[0], 100, snowPos[1]));
            assertSame(BlockType.WATER, world.getBlock(snowPos[0], 100, snowPos[1]));

            prepareOpenWaterColumn(world, snowPos[0] + 2, snowPos[1]);
            world.setBlock(snowPos[0] + 2, 101, snowPos[1], BlockType.LILY_PAD, 0);
            assertTrue(world.tryFreezeWaterAtColumn(snowPos[0] + 2, snowPos[1]));
            assertSame(BlockType.ICE, world.getBlock(snowPos[0] + 2, 100, snowPos[1]));
            assertSame(BlockType.AIR, world.getBlock(snowPos[0] + 2, 101, snowPos[1]));
            assertEquals(0, world.getDroppedItems().stream()
                    .filter(item -> item.getItemType() == ItemType.LILY_PAD)
                    .mapToInt(item -> item.getCount())
                    .sum());
        } finally {
            world.cleanup();
        }
    }

    private static int[] findRainBiome(World world) {
        for (int x = -256; x <= 256; x += 8) {
            for (int z = -256; z <= 256; z += 8) {
                BiomeType biome = world.getReleaseBiome(x, z);
                if (isRainBiome(biome)) {
                    return new int[] { x, z };
                }
            }
        }
        throw new AssertionError("No non-frozen rain biome found near spawn search area");
    }

    private static int[] findRainBiomePatch(World world) {
        for (int x = -512; x <= 512; x += 8) {
            for (int z = -512; z <= 512; z += 8) {
                if (isRainPatch(world, x, z, 4)) {
                    return new int[] { x, z };
                }
            }
        }
        throw new AssertionError("No non-frozen rain biome patch found near spawn search area");
    }

    private static int[] findRainBiomeChunk(World world) {
        for (int chunkX = -32; chunkX <= 32; chunkX++) {
            for (int chunkZ = -32; chunkZ <= 32; chunkZ++) {
                if (isRainBiomeChunk(world, chunkX, chunkZ)) {
                    return new int[] { chunkX, chunkZ };
                }
            }
        }
        throw new AssertionError("No full non-frozen rain biome chunk found near spawn search area");
    }

    private static boolean isRainBiomeChunk(World world, int chunkX, int chunkZ) {
        int baseX = chunkX * Chunk.WIDTH;
        int baseZ = chunkZ * Chunk.DEPTH;
        for (int localX = 0; localX < Chunk.WIDTH; localX++) {
            for (int localZ = 0; localZ < Chunk.DEPTH; localZ++) {
                if (!isRainBiome(world.getReleaseBiome(baseX + localX, baseZ + localZ))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int[] findSnowBiome(World world) {
        for (int radius = 0; radius <= 2048; radius += 16) {
            int[] found = findSnowBiomeOnRing(world, radius);
            if (found != null) {
                return found;
            }
        }
        throw new AssertionError("No frozen precipitation biome found near spawn search area");
    }

    private static int[] findSnowBiomeOnRing(World world, int radius) {
        if (radius == 0) {
            return snowBiomeAt(world, 0, 0);
        }
        for (int x = -radius; x <= radius; x += 16) {
            int[] north = snowBiomeAt(world, x, -radius);
            if (north != null) {
                return north;
            }
            int[] south = snowBiomeAt(world, x, radius);
            if (south != null) {
                return south;
            }
        }
        for (int z = -radius + 16; z <= radius - 16; z += 16) {
            int[] west = snowBiomeAt(world, -radius, z);
            if (west != null) {
                return west;
            }
            int[] east = snowBiomeAt(world, radius, z);
            if (east != null) {
                return east;
            }
        }
        return null;
    }

    private static int[] snowBiomeAt(World world, int x, int z) {
        BiomeType biome = world.getReleaseBiome(x, z);
        return biome.hasPrecipitation() && biome.canFreezeWater() ? new int[] { x, z } : null;
    }

    private static boolean isRainPatch(World world, int x, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (!isRainBiome(world.getReleaseBiome(x + dx, z + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isRainBiome(BiomeType biome) {
        return biome.hasPrecipitation() && !biome.canFreezeWater() && biome.getTemperature() < 1.0f;
    }

    private static Random fixedIntRandom(int value) {
        return new Random(0L) {
            @Override
            public int nextInt(int bound) {
                return Math.max(0, Math.min(value, bound - 1));
            }
        };
    }

    private static void prepareOpenColumn(World world, int x, int z) {
        world.getChunkNow(Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH));
        world.setBlock(x, 99, z, BlockType.STONE, 0);
        for (int y = 100; y < 128; y++) {
            world.setBlock(x, y, z, BlockType.AIR, 0);
        }
    }

    private static void prepareOpenWaterColumn(World world, int x, int z) {
        prepareOpenColumn(world, x, z);
        world.setBlock(x, 100, z, BlockType.WATER, 0);
    }

    private static void prepareOpenArea(World world, int centerX, int centerZ, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                prepareOpenColumn(world, centerX + dx, centerZ + dz);
            }
        }
    }

    private static void prepareOpenChunk(World world, int chunkX, int chunkZ) {
        world.getChunkNow(chunkX, chunkZ);
        int baseX = chunkX * Chunk.WIDTH;
        int baseZ = chunkZ * Chunk.DEPTH;
        for (int localX = 0; localX < Chunk.WIDTH; localX++) {
            for (int localZ = 0; localZ < Chunk.DEPTH; localZ++) {
                prepareOpenColumn(world, baseX + localX, baseZ + localZ);
            }
        }
    }
}
