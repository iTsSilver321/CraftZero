package com.craftzero.world.tile;

import com.craftzero.entity.Entity;
import com.craftzero.entity.mob.Giant;
import com.craftzero.entity.mob.MagmaCube;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.entity.mob.Slime;
import com.craftzero.entity.mob.Squid;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.Chunk;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MonsterSpawnerTileEntityTest {
    @Test
    @DisplayName("Active mob spawners emit smoke and flame particles")
    void activeSpawnerEmitsSmokeAndFlameParticles() {
        World world = new World(611L);
        try {
            world.getChunkNow(0, 0);
            world.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(8, 70, 8);

            spawner.tick(world, 1.0f / 20.0f);

            assertEquals(2, world.getParticles().size());
            assertTrue(world.getParticles().stream()
                    .anyMatch(particle -> particle.getType() == WorldParticle.Type.SMOKE));
            assertTrue(world.getParticles().stream()
                    .anyMatch(particle -> particle.getType() == WorldParticle.Type.FLAME));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Active mob spawners advance the Release-style preview rotation")
    void activeSpawnerAdvancesPreviewRotation() {
        World world = new World(615L);
        try {
            world.getChunkNow(0, 0);
            world.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(8, 70, 8);
            spawner.setDelay(20);

            spawner.tick(world, 1.0f / 40.0f);
            assertEquals(0.0f, spawner.getRenderRotation(1.0f), 0.0001f);

            spawner.tick(world, 1.0f / 40.0f);

            float expectedStep = 1000.0f / 220.0f;
            assertEquals(0.0f, spawner.getRenderRotation(0.0f), 0.0001f);
            assertEquals(expectedStep * 0.5f, spawner.getRenderRotation(0.5f), 0.0001f);
            assertEquals(expectedStep, spawner.getRenderRotation(1.0f), 0.0001f);
            assertEquals(19, spawner.getDelay());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Inactive mob spawners do not emit particles")
    void inactiveSpawnerDoesNotEmitParticles() {
        World world = new World(612L);
        try {
            world.getChunkNow(0, 0);
            MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(8, 70, 8);

            spawner.tick(world, 1.0f / 20.0f);

            assertTrue(world.getParticles().isEmpty());
            assertEquals(0.0f, spawner.getRenderRotation(1.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("World-created spawner randomness should come from the world seed")
    void worldCreatedSpawnerRandomnessUsesWorldSeed() {
        assertEquals(worldSpawnerRandomSnapshot(614L), worldSpawnerRandomSnapshot(614L));
    }

    @Test
    @DisplayName("Successful mob spawner spawns emit a Release-style smoke and flame burst")
    void successfulSpawnerSpawnEmitsBurstParticles() {
        World world = new World(613L);
        try {
            world.getChunkNow(0, 0);
            prepareSpawnArea(world, 8, 69, 8, BlockType.GRASS);
            world.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(8, 70, 8, centeredSpawnerRandom());
            spawner.setMobDefinition(MobDefinition.PIG);
            spawner.setSpawnCount(1);
            spawner.setDelay(0);

            spawner.tick(world, 1.0f / 20.0f);

            List<WorldParticle> particles = world.getParticles();
            long smoke = world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SMOKE)
                    .count();
            long flame = world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.FLAME)
                    .count();
            assertEquals(42, particles.size());
            assertEquals(21, smoke);
            assertEquals(21, flame);
            for (int i = 2; i < particles.size(); i += 2) {
                WorldParticle smokeParticle = particles.get(i);
                WorldParticle flameParticle = particles.get(i + 1);
                assertSame(WorldParticle.Type.SMOKE, smokeParticle.getType());
                assertSame(WorldParticle.Type.FLAME, flameParticle.getType());
                assertSameSpawnerBurstPosition(smokeParticle, flameParticle);
                assertSpawnerBurstParticleInCube(smokeParticle, 8, 70, 8);
                assertEquals(0.0f, smokeParticle.getMotionX(), 0.0001f);
                assertEquals(0.0f, smokeParticle.getMotionY(), 0.0001f);
                assertEquals(0.0f, smokeParticle.getMotionZ(), 0.0001f);
                assertEquals(0.0f, flameParticle.getMotionX(), 0.0001f);
                assertEquals(0.0f, flameParticle.getMotionY(), 0.0001f);
                assertEquals(0.0f, flameParticle.getMotionZ(), 0.0001f);
            }
            world.updateEntities(1.0f / 20.0f);
            assertTrue(world.getEntities().stream()
                    .filter(Mob.class::isInstance)
                    .map(Mob.class::cast)
                    .anyMatch(mob -> mob.getDefinition() == MobDefinition.PIG));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Default mob spawners should use the Release 1.0 pig mob")
    void defaultSpawnerUsesReleasePigMob() {
        World world = new World(618L);
        try {
            world.getChunkNow(0, 0);
            prepareSpawnArea(world, 8, 69, 8, BlockType.GRASS);
            world.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(8, 70, 8, centeredSpawnerRandom());
            assertSame(MobDefinition.PIG, spawner.getMobDefinition());

            spawner.setSpawnCount(1);
            spawner.setDelay(0);
            spawner.tick(world, 1.0f / 20.0f);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(world.getEntities().stream()
                    .filter(Mob.class::isInstance)
                    .map(Mob.class::cast)
                    .anyMatch(mob -> mob.getDefinition() == MobDefinition.PIG));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spawner-spawned mobs should receive the Release-style random yaw")
    void successfulSpawnerSpawnAppliesRandomYaw() {
        CapturingSpawnWorld world = new CapturingSpawnWorld(617L);
        try {
            world.getChunkNow(0, 0);
            prepareSpawnArea(world, 8, 69, 8, BlockType.GRASS);
            world.setPlayer(new Player(8.5f, 70.0f, 23.0f));
            MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(8, 70, 8, yawSpawnerRandom(0.25f));
            spawner.setMobDefinition(MobDefinition.PIG);
            spawner.setSpawnCount(1);
            spawner.setDelay(0);

            spawner.tick(world, 1.0f / 20.0f);

            Mob pig = assertInstanceOf(Mob.class, world.lastSpawned());
            assertSame(MobDefinition.PIG, pig.getDefinition());
            assertEquals(90.0f, pig.getYaw(), 0.0001f);
            assertEquals(0.0f, pig.getPitch(), 0.0001f);

            world.updateEntities(0.0f);
            assertTrue(world.getEntities().contains(pig));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Slime mob spawners should use Release-style random sizes")
    void slimeSpawnerCreatesRandomSizedSlimes() {
        CapturingSpawnWorld world = new CapturingSpawnWorld(619L);
        try {
            world.getChunkNow(0, 0);
            setTime(world, 18000.0f);
            prepareSpawnArea(world, 8, 69, 8);
            world.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(8, 70, 8,
                    slimeSizeSpawnerRandom(0));
            spawner.setMobDefinition(MobDefinition.SLIME);
            spawner.setSpawnCount(1);
            spawner.setDelay(0);

            spawner.tick(world, 1.0f / 20.0f);

            Slime slime = assertInstanceOf(Slime.class, world.lastSpawned());
            assertEquals(1, slime.getSize());
            assertEquals(0.6f, slime.getWidth(), 0.0001f);
            assertEquals(0.6f, slime.getHeight(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Magma cube mob spawners should use Release-style random sizes")
    void magmaCubeSpawnerCreatesRandomSizedMagmaCubes() {
        CapturingSpawnWorld world = new CapturingSpawnWorld(620L);
        try {
            world.getChunkNow(0, 0);
            setTime(world, 18000.0f);
            prepareSpawnArea(world, 8, 69, 8);
            world.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(8, 70, 8,
                    slimeSizeSpawnerRandom(1));
            spawner.setMobDefinition(MobDefinition.MAGMA_CUBE);
            spawner.setSpawnCount(1);
            spawner.setDelay(0);

            spawner.tick(world, 1.0f / 20.0f);

            MagmaCube magmaCube = assertInstanceOf(MagmaCube.class, world.lastSpawned());
            assertEquals(2, magmaCube.getSize());
            assertEquals(1.2f, magmaCube.getWidth(), 0.0001f);
            assertEquals(1.2f, magmaCube.getHeight(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spawner delay should advance at 20Hz even when the game updates faster")
    void accumulatesDeltaTimeAtTwentyTicksPerSecond() {
        World world = new World(602L);
        try {
            world.getChunkNow(0, 0);
            Player player = new Player(8.5f, 70.0f, 12.5f);
            world.setPlayer(player);
            MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(8, 70, 8);
            spawner.setDelay(2);

            spawner.tick(world, 1.0f / 40.0f);
            assertEquals(2, spawner.getDelay());

            spawner.tick(world, 1.0f / 40.0f);
            assertEquals(1, spawner.getDelay());

            spawner.tick(world, 1.0f / 40.0f);
            assertEquals(1, spawner.getDelay());

            spawner.tick(world, 1.0f / 40.0f);
            assertEquals(0, spawner.getDelay());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spawner delay reset should use Release 1.0 exclusive upper bound")
    void delayResetUsesExclusiveUpperBound() {
        World world = new World(607L);
        try {
            world.getChunkNow(0, 0);
            world.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            Zombie existing = new Zombie();
            existing.setPosition(8.5f, 70.0f, 8.5f);
            world.replaceEntities(List.of(existing));

            MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(8, 70, 8, maxRollRandom());
            spawner.setMobDefinition(MobDefinition.ZOMBIE);
            spawner.setDelayRange(5, 8);
            spawner.setMaxNearbyEntities(1);
            spawner.setDelay(0);

            spawner.tick(world, 1.0f / 20.0f);

            assertEquals(7, spawner.getDelay());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Creature spawners should require bright grass support")
    void creatureSpawnerRequiresBrightGrassSupport() {
        World world = new World(608L);
        try {
            world.getChunkNow(0, 0);
            world.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            prepareSpawnArea(world, 8, 69, 8, BlockType.GRASS);
            MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(8, 70, 8, centeredSpawnerRandom());
            spawner.setMobDefinition(MobDefinition.PIG);
            spawner.setDelay(0);

            spawner.tick(world, 1.0f / 20.0f);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(world.getEntities().stream()
                    .filter(Mob.class::isInstance)
                    .map(Mob.class::cast)
                    .anyMatch(mob -> mob.getDefinition() == MobDefinition.PIG));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Creature spawners should reject non-grass support")
    void creatureSpawnerRejectsStoneSupport() {
        World world = new World(609L);
        try {
            world.getChunkNow(0, 0);
            world.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            prepareSpawnArea(world, 8, 69, 8, BlockType.STONE);
            MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(8, 70, 8, centeredSpawnerRandom());
            spawner.setMobDefinition(MobDefinition.PIG);
            spawner.setSpawnCount(20);
            spawner.setMaxNearbyEntities(20);
            spawner.setDelay(0);

            spawner.tick(world, 1.0f / 20.0f);
            world.updateEntities(1.0f / 20.0f);

            assertFalse(world.getEntities().stream()
                    .filter(Mob.class::isInstance)
                    .map(Mob.class::cast)
                    .anyMatch(mob -> mob.getDefinition() == MobDefinition.PIG));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mob spawners should retry every tick after failed spawn attempts")
    void failedSpawnerAttemptsKeepZeroDelayUntilSuccess() {
        World world = new World(616L);
        try {
            world.getChunkNow(0, 0);
            world.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            prepareSpawnArea(world, 8, 69, 8, BlockType.STONE);
            MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(8, 70, 8, centeredSpawnerRandom());
            spawner.setMobDefinition(MobDefinition.PIG);
            spawner.setSpawnCount(1);
            spawner.setDelayRange(5, 8);
            spawner.setDelay(0);

            spawner.tick(world, 1.0f / 20.0f);
            world.updateEntities(1.0f / 20.0f);

            assertEquals(0, spawner.getDelay(),
                    "Failed spawn attempts should not start a fresh spawner cooldown");
            assertFalse(world.getEntities().stream()
                    .filter(Mob.class::isInstance)
                    .map(Mob.class::cast)
                    .anyMatch(mob -> mob.getDefinition() == MobDefinition.PIG));

            prepareSpawnArea(world, 8, 69, 8, BlockType.GRASS);
            spawner.tick(world, 1.0f / 20.0f);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(spawner.getDelay() > 0);
            assertTrue(world.getEntities().stream()
                    .filter(Mob.class::isInstance)
                    .map(Mob.class::cast)
                    .anyMatch(mob -> mob.getDefinition() == MobDefinition.PIG));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spawner cap should include mobs spawned earlier in the same tick")
    void nearbyCapCountsSameTickSpawns() {
        World world = new World(604L);
        try {
            world.getChunkNow(0, 0);
            prepareWaterSpawnArea(world, 8, 60, 8);
            world.setPlayer(new Player(8.5f, 60.0f, 12.5f));
            world.setBlock(8, 60, 8, BlockType.MOB_SPAWNER);
            MonsterSpawnerTileEntity spawner = (MonsterSpawnerTileEntity) world.getTileEntity(8, 60, 8);
            spawner.setMobDefinition(MobDefinition.SQUID);
            spawner.setMaxNearbyEntities(1);
            spawner.setSpawnCount(50);
            spawner.setDelay(0);

            spawner.tick(world, 1.0f / 20.0f);
            world.updateEntities(1.0f / 20.0f);

            long squidCount = world.getEntities().stream()
                    .filter(Mob.class::isInstance)
                    .map(Mob.class::cast)
                    .filter(mob -> mob.getDefinition() == MobDefinition.SQUID)
                    .count();
            assertEquals(1, squidCount);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spawner nearby cap should use the Release 1.0 expanded box")
    void nearbyCapUsesExpandedBoxInsteadOfSphere() {
        World world = new World(603L);
        try {
            world.getChunkNow(0, 0);
            prepareWaterSpawnArea(world, 8, 60, 8);
            world.setPlayer(new Player(8.5f, 60.0f, 12.5f));
            world.setBlock(8, 60, 8, BlockType.MOB_SPAWNER);
            MonsterSpawnerTileEntity spawner = (MonsterSpawnerTileEntity) world.getTileEntity(8, 60, 8);
            spawner.setMobDefinition(MobDefinition.SQUID);
            spawner.setMaxNearbyEntities(1);
            spawner.setSpawnCount(50);
            spawner.setDelay(0);

            Squid highSquid = new Squid();
            highSquid.setPosition(8.5f, 67.0f, 8.5f);
            world.replaceEntities(List.of(highSquid));

            spawner.tick(world, 1.0f / 20.0f);
            world.updateEntities(1.0f / 20.0f);

            long squidCount = world.getEntities().stream()
                    .filter(Mob.class::isInstance)
                    .map(Mob.class::cast)
                    .filter(mob -> mob.getDefinition() == MobDefinition.SQUID)
                    .count();
            assertTrue(squidCount > 1);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Water creature spawners should stay within the Release 1.0 squid height band")
    void waterCreatureSpawnerRejectsOutOfBandWater() {
        World world = new World(610L);
        try {
            world.getChunkNow(0, 0);
            prepareWaterSpawnArea(world, 8, 70, 8);
            world.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            world.setBlock(8, 70, 8, BlockType.MOB_SPAWNER);
            MonsterSpawnerTileEntity spawner = (MonsterSpawnerTileEntity) world.getTileEntity(8, 70, 8);
            spawner.setMobDefinition(MobDefinition.SQUID);
            spawner.setSpawnCount(50);
            spawner.setMaxNearbyEntities(50);
            spawner.setDelay(0);

            spawner.tick(world, 1.0f / 20.0f);
            world.updateEntities(1.0f / 20.0f);

            assertFalse(world.getEntities().stream()
                    .filter(Mob.class::isInstance)
                    .map(Mob.class::cast)
                    .anyMatch(mob -> mob.getDefinition() == MobDefinition.SQUID));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spawner collision checks should honor tall mob height")
    void spawnerRejectsTooLowCeilingForTallMobs() {
        World world = new World(606L);
        try {
            world.getChunkNow(0, 0);
            setTime(world, 18000.0f);
            prepareSpawnArea(world, 8, 68, 8);
            fillArea(world, 8, 71, 8, BlockType.STONE);
            world.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            world.setBlock(8, 70, 8, BlockType.MOB_SPAWNER);
            MonsterSpawnerTileEntity spawner = (MonsterSpawnerTileEntity) world.getTileEntity(8, 70, 8);
            spawner.setMobDefinition(MobDefinition.ENDERMAN);
            spawner.setSpawnCount(50);
            spawner.setMaxNearbyEntities(50);

            for (int i = 0; i < 4; i++) {
                spawner.setDelay(0);
                spawner.tick(world, 1.0f / 20.0f);
                world.updateEntities(1.0f / 20.0f);
            }

            assertFalse(world.getEntities().stream()
                    .filter(Mob.class::isInstance)
                    .map(Mob.class::cast)
                    .anyMatch(mob -> mob.getDefinition() == MobDefinition.ENDERMAN));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Giant spawners should require a full Release-era Giant volume")
    void giantSpawnersRequireFullGiantVolume() {
        CapturingSpawnWorld clearWorld = new CapturingSpawnWorld(624L);
        CapturingSpawnWorld blockedWorld = new CapturingSpawnWorld(625L);
        try {
            clearWorld.getChunkNow(0, 0);
            setTime(clearWorld, 18000.0f);
            prepareTallSpawnArea(clearWorld, 8, 69, 8, BlockType.STONE, 14);
            clearWorld.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            MonsterSpawnerTileEntity clearSpawner = new MonsterSpawnerTileEntity(8, 70, 8,
                    centeredSpawnerRandom());
            clearSpawner.setMobDefinition(MobDefinition.GIANT);
            clearSpawner.setSpawnCount(1);
            clearSpawner.setDelay(0);

            clearSpawner.tick(clearWorld, 1.0f / 20.0f);

            Giant giant = assertInstanceOf(Giant.class, clearWorld.lastSpawned());
            assertSame(MobDefinition.GIANT, giant.getDefinition());
            assertEquals(3.6f, giant.getWidth(), 0.001f);
            assertEquals(10.8f, giant.getHeight(), 0.001f);

            blockedWorld.getChunkNow(0, 0);
            setTime(blockedWorld, 18000.0f);
            prepareTallSpawnArea(blockedWorld, 8, 69, 8, BlockType.STONE, 14);
            blockedWorld.setBlock(8, 80, 8, BlockType.STONE);
            blockedWorld.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            MonsterSpawnerTileEntity blockedSpawner = new MonsterSpawnerTileEntity(8, 70, 8,
                    centeredSpawnerRandom());
            blockedSpawner.setMobDefinition(MobDefinition.GIANT);
            blockedSpawner.setSpawnCount(1);
            blockedSpawner.setDelay(0);

            blockedSpawner.tick(blockedWorld, 1.0f / 20.0f);

            assertNull(blockedWorld.lastSpawned());
        } finally {
            clearWorld.cleanup();
            blockedWorld.cleanup();
        }
    }

    @Test
    @DisplayName("Hostile spawners should reject lit spawn positions")
    void hostileSpawnerRejectsLitSpawnPositions() {
        World world = new World(605L);
        try {
            world.getChunkNow(0, 0);
            setTime(world, 18000.0f);
            prepareSpawnArea(world, 8, 69, 8, BlockType.GLOWSTONE);
            world.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            world.setBlock(8, 70, 8, BlockType.MOB_SPAWNER);
            MonsterSpawnerTileEntity spawner = (MonsterSpawnerTileEntity) world.getTileEntity(8, 70, 8);
            spawner.setMobDefinition(MobDefinition.SKELETON);
            spawner.setSpawnCount(50);
            spawner.setMaxNearbyEntities(50);
            spawner.setDelay(0);

            for (int i = 0; i < 4; i++) {
                spawner.setDelay(0);
                spawner.tick(world, 1.0f / 20.0f);
                world.updateEntities(1.0f / 20.0f);
            }

            assertFalse(world.getEntities().stream()
                    .filter(Mob.class::isInstance)
                    .map(Mob.class::cast)
                    .anyMatch(mob -> mob.getDefinition() == MobDefinition.SKELETON));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Hostile mob spawners should not require floor support")
    void hostileSpawnerDoesNotRequireFloorSupport() {
        CapturingSpawnWorld world = new CapturingSpawnWorld(626L);
        try {
            world.getChunkNow(0, 0);
            setTime(world, 18000.0f);
            prepareAirSpawnArea(world, 8, 69, 8, 5);
            world.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(8, 70, 8,
                    centeredSpawnerRandom());
            spawner.setMobDefinition(MobDefinition.ZOMBIE);
            spawner.setSpawnCount(1);
            spawner.setDelay(0);

            spawner.tick(world, 1.0f / 20.0f);

            Mob zombie = assertInstanceOf(Mob.class, world.lastSpawned());
            assertSame(MobDefinition.ZOMBIE, zombie.getDefinition());
            assertEquals(70.0f, zombie.getY(), 0.0001f);
            assertSame(BlockType.AIR, world.getBlock(8, 69, 8),
                    "Release-era spawner mobs use entity volume checks, not a solid-floor predicate");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Hostile mob spawners should reject liquid inside the spawn volume")
    void hostileSpawnerRejectsLiquidSpawnVolume() {
        CapturingSpawnWorld world = new CapturingSpawnWorld(627L);
        try {
            world.getChunkNow(0, 0);
            setTime(world, 18000.0f);
            prepareSpawnArea(world, 8, 69, 8);
            world.setBlock(8, 70, 8, BlockType.WATER);
            world.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(8, 70, 8,
                    centeredSpawnerRandom());
            spawner.setMobDefinition(MobDefinition.ZOMBIE);
            spawner.setSpawnCount(1);
            spawner.setDelay(0);

            spawner.tick(world, 1.0f / 20.0f);

            assertNull(world.lastSpawned(),
                    "EntityLiving.getCanSpawnHere rejects liquid in the bounding box for non-water mobs");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spawner should spawn the configured mob when the player is nearby and space is clear")
    void spawnsConfiguredMobNearPlayer() {
        World world = new World(601L);
        try {
            world.getChunkNow(0, 0);
            setTime(world, 18000.0f);
            prepareSpawnArea(world, 8, 69, 8);
            Player player = new Player(8.5f, 70.0f, 12.5f);
            world.setPlayer(player);
            world.setBlock(8, 70, 8, BlockType.MOB_SPAWNER);
            MonsterSpawnerTileEntity spawner = (MonsterSpawnerTileEntity) world.getTileEntity(8, 70, 8);
            spawner.setMobDefinition(MobDefinition.SKELETON);
            spawner.setSpawnCount(20);
            spawner.setMaxNearbyEntities(20);

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
        prepareSpawnArea(world, centerX, groundY, centerZ, BlockType.STONE);
    }

    private static void prepareSpawnArea(World world, int centerX, int groundY, int centerZ, BlockType ground) {
        prepareTallSpawnArea(world, centerX, groundY, centerZ, ground, 4);
    }

    private static void prepareTallSpawnArea(World world, int centerX, int groundY, int centerZ, BlockType ground,
            int clearHeight) {
        for (int x = centerX - 5; x <= centerX + 5; x++) {
            for (int z = centerZ - 5; z <= centerZ + 5; z++) {
                assertTrue(world.isChunkGeneratedForBlock(x, z));
                world.setBlock(x, groundY, z, ground);
                for (int y = groundY + 1; y <= groundY + clearHeight; y++) {
                    world.setBlock(x, y, z, BlockType.AIR);
                }
            }
        }
    }

    private static void prepareAirSpawnArea(World world, int centerX, int minY, int centerZ, int clearHeight) {
        for (int x = centerX - 5; x <= centerX + 5; x++) {
            for (int z = centerZ - 5; z <= centerZ + 5; z++) {
                assertTrue(world.isChunkGeneratedForBlock(x, z));
                for (int y = minY; y <= minY + clearHeight; y++) {
                    world.setBlock(x, y, z, BlockType.AIR);
                }
            }
        }
    }

    private static void fillArea(World world, int centerX, int y, int centerZ, BlockType block) {
        for (int x = centerX - 5; x <= centerX + 5; x++) {
            for (int z = centerZ - 5; z <= centerZ + 5; z++) {
                assertTrue(world.isChunkGeneratedForBlock(x, z));
                world.setBlock(x, y, z, block);
            }
        }
    }

    private static void setTime(World world, float time) {
        DayCycleManager dayCycle = new DayCycleManager();
        dayCycle.setTime(time);
        world.setDayCycleManager(dayCycle);
    }

    private static void assertSameSpawnerBurstPosition(WorldParticle smoke, WorldParticle flame) {
        assertEquals(smoke.getRenderX(0.0f), flame.getRenderX(0.0f), 0.0001f);
        assertEquals(smoke.getRenderY(0.0f), flame.getRenderY(0.0f), 0.0001f);
        assertEquals(smoke.getRenderZ(0.0f), flame.getRenderZ(0.0f), 0.0001f);
    }

    private static void assertSpawnerBurstParticleInCube(WorldParticle particle, int x, int y, int z) {
        float particleX = particle.getRenderX(0.0f);
        float particleY = particle.getRenderY(0.0f);
        float particleZ = particle.getRenderZ(0.0f);
        assertTrue(particleX >= x - 0.5f && particleX <= x + 1.5f);
        assertTrue(particleY >= y - 0.5f && particleY <= y + 1.5f);
        assertTrue(particleZ >= z - 0.5f && particleZ <= z + 1.5f);
    }

    private static SpawnerRandomSnapshot worldSpawnerRandomSnapshot(long seed) {
        World world = new World(seed);
        try {
            world.getChunkNow(0, 0);
            world.setPlayer(new Player(8.5f, 70.0f, 12.5f));
            Zombie existing = new Zombie();
            existing.setPosition(8.5f, 70.0f, 8.5f);
            world.replaceEntities(List.of(existing));
            world.setBlock(8, 70, 8, BlockType.MOB_SPAWNER);
            MonsterSpawnerTileEntity spawner = (MonsterSpawnerTileEntity) world.getTileEntity(8, 70, 8);
            spawner.setDelayRange(5, 8);
            spawner.setMaxNearbyEntities(1);
            spawner.setDelay(0);

            spawner.tick(world, 1.0f / 20.0f);

            WorldParticle smoke = world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SMOKE)
                    .findFirst()
                    .orElseThrow();
            return new SpawnerRandomSnapshot(smoke.getRenderX(0.0f), smoke.getRenderY(0.0f),
                    smoke.getRenderZ(0.0f), smoke.getScale(0.0f), spawner.getDelay());
        } finally {
            world.cleanup();
        }
    }

    private record SpawnerRandomSnapshot(float particleX, float particleY, float particleZ,
            float particleScale, int delay) {
    }

    private static final class CapturingSpawnWorld extends World {
        private Entity lastSpawned;

        private CapturingSpawnWorld(long seed) {
            super(seed);
        }

        @Override
        public void spawnEntity(Entity entity) {
            lastSpawned = entity;
            super.spawnEntity(entity);
        }

        private Entity lastSpawned() {
            return lastSpawned;
        }
    }

    private static Random maxRollRandom() {
        return new Random(0L) {
            @Override
            public int nextInt(int bound) {
                return bound - 1;
            }
        };
    }

    private static Random centeredSpawnerRandom() {
        return new Random(0L) {
            @Override
            public double nextDouble() {
                return 0.5d;
            }

            @Override
            public int nextInt(int bound) {
                return Math.min(1, bound - 1);
            }
        };
    }

    private static Random yawSpawnerRandom(float yawFloat) {
        return new Random(0L) {
            @Override
            public double nextDouble() {
                return 0.5d;
            }

            @Override
            public int nextInt(int bound) {
                return Math.min(1, bound - 1);
            }

            @Override
            public float nextFloat() {
                return yawFloat;
            }
        };
    }

    private static Random slimeSizeSpawnerRandom(int sizeRoll) {
        return new Random(0L) {
            private int nextInt3Calls;

            @Override
            public double nextDouble() {
                return 0.5d;
            }

            @Override
            public int nextInt(int bound) {
                if (bound == 3) {
                    return nextInt3Calls++ == 0 ? 1 : Math.floorMod(sizeRoll, bound);
                }
                return Math.min(1, bound - 1);
            }

            @Override
            public float nextFloat() {
                return 0.25f;
            }
        };
    }

    private static void prepareWaterSpawnArea(World world, int centerX, int centerY, int centerZ) {
        for (int x = centerX - 5; x <= centerX + 5; x++) {
            for (int z = centerZ - 5; z <= centerZ + 5; z++) {
                assertTrue(world.isChunkGeneratedForBlock(x, z));
                for (int y = centerY - 1; y <= centerY + 2; y++) {
                    world.setBlock(x, y, z, BlockType.WATER);
                }
            }
        }
    }
}
