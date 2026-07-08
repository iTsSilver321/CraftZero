package com.craftzero.world;

import com.craftzero.entity.Entity;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.entity.mob.MobFactory;
import com.craftzero.entity.mob.Skeleton;
import com.craftzero.entity.mob.Sheep;
import com.craftzero.entity.mob.SnowGolem;
import com.craftzero.entity.mob.Spider;
import com.craftzero.entity.mob.Villager;
import com.craftzero.main.Difficulty;
import com.craftzero.main.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MobSpawnerTest {
    @Test
    @DisplayName("Hostile spawn rule should allow dark clear solid ground")
    void hostileSpawnAllowedInDarkClearSpace() {
        World world = new World(401L);
        try {
            setTime(world, 18000.0f);
            prepareSpawnColumn(world, 0, 70, 0, BlockType.STONE);

            MobSpawner spawner = new MobSpawner(world, new HostileLightGateRandom(31, 7));

            assertTrue(spawner.canSpawnHostileAt(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Hostile spawn rule should reject bright spaces")
    void hostileSpawnRejectedInBrightSpace() {
        World world = new World(402L);
        try {
            setTime(world, 6000.0f);
            prepareSpawnColumn(world, 0, 70, 0, BlockType.STONE);

            MobSpawner spawner = new MobSpawner(world, new HostileLightGateRandom(31, 7));

            assertFalse(spawner.canSpawnHostileAt(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Hostile spawn rule should reject torch-lit spaces at night")
    void hostileSpawnRejectedByBlockLight() {
        World world = new World(405L);
        try {
            setTime(world, 18000.0f);
            prepareSpawnColumn(world, 0, 70, 0, BlockType.STONE);
            world.setBlock(1, 71, 0, BlockType.TORCH);

            MobSpawner spawner = new MobSpawner(world, new HostileLightGateRandom(31, 7));

            assertTrue(world.getBlockLight(0, 71, 0) > 7);
            assertFalse(spawner.canSpawnHostileAt(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Hostile spawn light should use the Release 1.0 random block-light threshold")
    void hostileSpawnLightUsesReleaseRandomBlockThreshold() {
        FixedLightWorld world = new FixedLightWorld(434L, 0, 7);
        try {
            prepareSpawnColumn(world, 0, 70, 0, BlockType.STONE);

            assertFalse(new MobSpawner(world, new HostileLightGateRandom(31, 6))
                    .canSpawnHostileAt(0, 70, 0, MobDefinition.ZOMBIE));
            assertTrue(new MobSpawner(world, new HostileLightGateRandom(31, 7))
                    .canSpawnHostileAt(0, 70, 0, MobDefinition.ZOMBIE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Hostile spawn light should use the Release 1.0 raw sky-light precheck")
    void hostileSpawnLightUsesReleaseRandomSkyPrecheck() {
        FixedLightWorld world = new FixedLightWorld(435L, 15, 0);
        try {
            setTime(world, 18000.0f);
            prepareSpawnColumn(world, 0, 70, 0, BlockType.STONE);

            assertFalse(new MobSpawner(world, new HostileLightGateRandom(14, 7))
                    .canSpawnHostileAt(0, 70, 0, MobDefinition.ZOMBIE));
            assertTrue(new MobSpawner(world, new HostileLightGateRandom(15, 7))
                    .canSpawnHostileAt(0, 70, 0, MobDefinition.ZOMBIE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spawn rules should reject blocked feet or head space")
    void spawnRulesRejectBlockedSpace() {
        World world = new World(403L);
        try {
            setTime(world, 18000.0f);
            prepareSpawnColumn(world, 0, 70, 0, BlockType.STONE);
            world.setBlock(0, 72, 0, BlockType.STONE);

            MobSpawner spawner = new MobSpawner(world);

            assertFalse(spawner.canSpawnHostileAt(0, 70, 0));
            assertFalse(spawner.canSpawnPassiveAt(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Natural spawn rules should reject blocks inside the selected mob volume")
    void spawnRulesRejectSelectedMobVolumeObstructions() {
        World world = new World(411L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            prepareSpawnVolume(world, 0, 70, 0, BlockType.NETHERRACK, 4, 5);
            MobSpawner spawner = new MobSpawner(world);

            assertTrue(spawner.canSpawnHostileAt(0, 70, 0, MobDefinition.GHAST));

            world.setBlock(2, 72, 0, BlockType.NETHERRACK, 0);

            assertFalse(spawner.canSpawnHostileAt(0, 70, 0, MobDefinition.GHAST));
            assertTrue(spawner.canSpawnHostileAt(0, 70, 0, MobDefinition.ZOMBIE_PIGMAN));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ghast spawn checks should use flying volume rules instead of ground support")
    void ghastSpawnChecksUseFlyingVolumeRulesInsteadOfGroundSupport() {
        World world = new World(432L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            prepareAirSpawnVolume(world, 8, 70, 8, 3, 5);
            world.setBlock(9, 71, 8, BlockType.TORCH, 0);
            MobSpawner spawner = new MobSpawner(world);

            assertTrue(world.getBlockLight(8, 71, 8) > 7);
            assertTrue(spawner.canSpawnHostileAt(8, 70, 8, MobDefinition.GHAST));
            assertFalse(spawner.canSpawnHostileAt(8, 70, 8, MobDefinition.ZOMBIE_PIGMAN));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Natural ghast packs should roll the Release 1.0 rare spawn gate")
    void naturalGhastPacksRollReleaseOneRareSpawnGate() {
        SpawnRule ghastRule = new SpawnRule(MobDefinition.GHAST, 4, 32, Chunk.HEIGHT - 10, false);
        CapturingSpawnWorld world = new CapturingSpawnWorld(433L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            prepareAirSpawnVolume(world, 8, 70, 8, 3, 5);

            MobSpawner rejectedSpawner = new MobSpawner(world, new GhastGateRandom(1));
            assertEquals(0, rejectedSpawner.spawnHostilePack(ghastRule, 8, 70, 8, 1));
            world.updateEntities(0.0f);
            assertEquals(0, countMobs(world, MobDefinition.GHAST));

            MobSpawner acceptedSpawner = new MobSpawner(world, new GhastGateRandom(0));
            assertEquals(1, acceptedSpawner.spawnHostilePack(ghastRule, 8, 70, 8, 1));

            Mob ghast = assertInstanceOf(Mob.class, world.lastSpawned());
            assertEquals(8.5f, ghast.getX(), 0.0001f);
            assertEquals(71.0f, ghast.getY(), 0.0001f);
            assertEquals(8.5f, ghast.getZ(), 0.0001f);
            world.updateEntities(0.0f);
            assertEquals(1, countMobs(world, MobDefinition.GHAST));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Natural ground spawns should reject liquid inside the selected mob volume")
    void spawnRulesRejectLiquidInsideSelectedMobVolume() {
        World world = new World(412L);
        try {
            setTime(world, 18000.0f);
            prepareSpawnColumn(world, 0, 70, 0, BlockType.STONE);
            world.setBlock(0, 71, 0, BlockType.WATER, 0);
            MobSpawner spawner = new MobSpawner(world);

            assertFalse(spawner.canSpawnHostileAt(0, 70, 0, MobDefinition.ZOMBIE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Natural slime spawns should use Release 1.0 chunk, height, and light rules")
    void naturalSlimeSpawnRulesUseReleaseOneChunkHeightAndLightRules() {
        World world = new World(416L);
        try {
            setTime(world, 6000.0f);
            MobSpawner spawner = new MobSpawner(world, new Random(0L));
            int[] slimeColumn = findSlimeColumn(spawner, true);
            int[] ordinaryColumn = findSlimeColumn(spawner, false);

            prepareSpawnColumn(world, slimeColumn[0], 38, slimeColumn[1], BlockType.STONE);
            assertTrue(world.getSkyLight(slimeColumn[0], 39, slimeColumn[1]) > 7);
            assertTrue(spawner.canSpawnHostileAt(slimeColumn[0], 38, slimeColumn[1], MobDefinition.SLIME));

            prepareSpawnColumn(world, slimeColumn[0], 39, slimeColumn[1], BlockType.STONE);
            assertFalse(spawner.canSpawnHostileAt(slimeColumn[0], 39, slimeColumn[1], MobDefinition.SLIME));

            prepareSpawnColumn(world, ordinaryColumn[0], 38, ordinaryColumn[1], BlockType.STONE);
            assertFalse(spawner.canSpawnHostileAt(ordinaryColumn[0], 38, ordinaryColumn[1], MobDefinition.SLIME));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Explicit slime spawn packs should not bypass slime chunk gating")
    void explicitSlimeSpawnPacksRespectSlimeChunkGate() {
        World world = new World(417L);
        try {
            setTime(world, 6000.0f);
            MobSpawner spawner = new MobSpawner(world, new Random(1L));
            SpawnRule slimeRule = new SpawnRule(MobDefinition.SLIME, 4, 1, 38, false);
            int[] slimeColumn = findSlimeColumn(spawner, true);
            int[] ordinaryColumn = findSlimeColumn(spawner, false);

            prepareSpawnColumn(world, ordinaryColumn[0], 38, ordinaryColumn[1], BlockType.STONE);
            assertEquals(0, spawner.spawnHostilePack(slimeRule, ordinaryColumn[0], 38, ordinaryColumn[1], 1));

            prepareTallSpawnPatch(world, slimeColumn[0], 38, slimeColumn[1], BlockType.STONE, 3, 4);
            assertEquals(1, spawner.spawnHostilePack(slimeRule, slimeColumn[0], 38, slimeColumn[1], 1));
            world.updateEntities(0.0f);
            assertEquals(1, countMobs(world, MobDefinition.SLIME));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ground spawn packs should validate the actual created mob volume")
    void groundSpawnPacksValidateActualCreatedMobVolume() {
        World world = new World(418L);
        try {
            setTime(world, 6000.0f);
            MobSpawner spawner = new MobSpawner(world, new ZeroRandom());
            SpawnRule slimeRule = new SpawnRule(MobDefinition.SLIME, 4, 1, 38, false);
            int[] slimeColumn = findSlimeColumn(spawner, true);
            int x = slimeColumn[0];
            int z = slimeColumn[1];

            prepareSpawnColumn(world, x, 38, z, BlockType.STONE);
            world.setBlock(x + 1, 40, z, BlockType.STONE, 0);

            assertTrue(spawner.canSpawnHostileAt(x, 38, z, MobDefinition.SLIME),
                    "The definition-sized preflight check should still pass");
            assertEquals(0, spawner.spawnHostilePack(slimeRule, x, 38, z, 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Natural spawns should stay outside the Release 1.0 world-spawn exclusion")
    void naturalSpawnsRespectWorldSpawnExclusion() {
        assertNaturalHostileSpawnAtCandidate(-112, 71, 0, 0);
        assertNaturalHostileSpawnAtCandidate(1000, 70, 1000, 1);
    }

    @Test
    @DisplayName("Runtime natural spawning should skip the Release-style outer eligible chunk border")
    void runtimeNaturalSpawningSkipsOuterBorderChunks() {
        World world = new World(421L);
        try {
            setTime(world, 18000.0f);
            prepareSpawnColumn(world, -128, 70, 0, BlockType.STONE);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            player.setWorld(world);
            MobSpawner spawner = new MobSpawner(world, new EdgeChunkSpawnRandom());

            spawner.tick();
            world.updateEntities(0.0f);

            assertEquals(0, countMobs(world, MobDefinition.ZOMBIE),
                    "The outer border chunk should count toward caps but not receive spawn attempts");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Runtime natural spawning should sweep the Release-style inner eligible chunk area")
    void runtimeNaturalSpawningSweepsEligibleChunks() {
        World world = new World(421L);
        try {
            setTime(world, 18000.0f);
            prepareSpawnColumn(world, -112, 70, 0, BlockType.STONE);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            player.setWorld(world);
            MobSpawner spawner = new MobSpawner(world, new EdgeChunkSpawnRandom());

            spawner.tick();
            world.updateEntities(0.0f);

            assertEquals(1, countMobs(world, MobDefinition.ZOMBIE),
                    "The selected eligible chunk candidate is outside the old single radial probe");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Runtime natural ground spawning should use the selected chunk's random Y")
    void runtimeNaturalGroundSpawningUsesSelectedChunkRandomY() {
        World world = new World(421L);
        try {
            setTime(world, 18000.0f);
            world.setWorldSpawn(0, 80, 0);
            prepareTwoLevelSpawnColumn(world, -112, 20, 70, 0, BlockType.STONE);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            player.setWorld(world);
            MobSpawner spawner = new MobSpawner(world, new NaturalGroundYSpawnRandom(20));
            assertFalse(spawner.hostileRulesAt(-112, 20, 0).isEmpty());
            assertTrue(spawner.canSpawnHostileAt(-112, 20, 0, MobDefinition.ZOMBIE));

            spawner.tick();
            world.updateEntities(0.0f);

            Mob zombie = onlyMob(world, MobDefinition.ZOMBIE);
            assertEquals(-111.5f, zombie.getX(), 0.0001f);
            assertEquals(21.0f, zombie.getY(), 0.0001f);
            assertEquals(0.5f, zombie.getZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Runtime natural spawning should run three local group attempts per eligible chunk")
    void runtimeNaturalSpawningRunsThreeGroupsPerEligibleChunk() {
        World world = new World(423L);
        try {
            setTime(world, 18000.0f);
            prepareSpawnPatch(world, -110, 70, 0, BlockType.STONE, 8);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            player.setWorld(world);
            int hostileCap = MobSpawner.releaseOneMobCap(70);
            List<Mob> existing = new ArrayList<>();
            for (int i = 0; i < hostileCap - 3; i++) {
                Mob mob = MobFactory.create(MobDefinition.ZOMBIE);
                assertNotNull(mob);
                mob.setPosition(4.5f + i % 12, 70.0f, 4.5f + i / 12);
                existing.add(mob);
            }
            world.replaceEntities(existing);
            MobSpawner spawner = new MobSpawner(world, new ThreeGroupChunkSpawnRandom());

            spawner.tick();
            world.updateEntities(0.0f);

            assertEquals(hostileCap, countMobs(world, MobDefinition.ZOMBIE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Runtime passive spawning should not use an extra category chance gate")
    void runtimePassiveSpawningDoesNotUseCategoryChanceGate() {
        World world = new World(428L);
        try {
            prepareSpawnColumn(world, -112, 70, 0, BlockType.GRASS);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.setDifficulty(Difficulty.PEACEFUL);
            world.setPlayer(player);
            player.setWorld(world);
            MobSpawner spawner = new MobSpawner(world, new EdgeChunkSpawnRandom());

            spawner.tick();
            world.updateEntities(0.0f);

            assertEquals(1, countMobs(world, MobDefinition.SHEEP));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Villagers and utility mobs should not consume the natural creature cap")
    void nonCreatureMobsDoNotConsumeNaturalCreatureCap() {
        World world = new World(442L);
        try {
            prepareSpawnColumn(world, -112, 70, 0, BlockType.GRASS);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.setDifficulty(Difficulty.PEACEFUL);
            world.setPlayer(player);
            player.setWorld(world);

            List<Mob> existing = new ArrayList<>();
            int passiveCap = MobSpawner.releaseOneMobCap(10);
            for (int i = 0; i < passiveCap; i++) {
                Mob mob = i % 2 == 0
                        ? new Villager(Villager.PROFESSION_FARMER)
                        : new SnowGolem();
                mob.setPosition(4.5f + i * 0.01f, 70.0f, 4.5f);
                existing.add(mob);
            }
            world.replaceEntities(existing);
            MobSpawner spawner = new MobSpawner(world, new EdgeChunkSpawnRandom());

            spawner.tick();
            world.updateEntities(0.0f);

            assertEquals(1, countMobs(world, MobDefinition.SHEEP));
            assertEquals(passiveCap, countMobs(world, MobDefinition.VILLAGER)
                    + countMobs(world, MobDefinition.SNOW_GOLEM));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Runtime water-creature spawning should not use an extra category chance gate")
    void runtimeWaterCreatureSpawningDoesNotUseCategoryChanceGate() {
        World world = new World(429L);
        try {
            prepareWaterPatch(world, -112, 60, 0, 1);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.setDifficulty(Difficulty.PEACEFUL);
            world.setPlayer(player);
            player.setWorld(world);
            List<Mob> existing = new ArrayList<>();
            for (int i = 0; i < MobSpawner.releaseOneMobCap(10); i++) {
                Mob mob = MobFactory.create(MobDefinition.COW);
                assertNotNull(mob);
                mob.setPosition(4.5f + i * 0.01f, 70.0f, 4.5f);
                existing.add(mob);
            }
            world.replaceEntities(existing);
            MobSpawner spawner = new MobSpawner(world, new EdgeChunkSpawnRandom());

            spawner.tick();
            world.updateEntities(0.0f);

            assertEquals(1, countMobs(world, MobDefinition.SQUID));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Runtime hostile cap should scale by the Release-style eligible chunk count")
    void runtimeHostileCapScalesWithEligibleChunks() {
        World world = new World(422L);
        try {
            setTime(world, 18000.0f);
            prepareSpawnColumn(world, -112, 70, 0, BlockType.STONE);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            player.setWorld(world);
            List<Mob> existing = new ArrayList<>();
            for (int i = 0; i < 70; i++) {
                Mob mob = MobFactory.create(MobDefinition.ZOMBIE);
                assertNotNull(mob);
                mob.setPosition(4.5f + i * 0.01f, 70.0f, 4.5f);
                existing.add(mob);
            }
            world.replaceEntities(existing);
            MobSpawner spawner = new MobSpawner(world, new EdgeChunkSpawnRandom());

            spawner.tick();
            world.updateEntities(0.0f);

            assertEquals(79, MobSpawner.releaseOneMobCap(70));
            assertEquals(71, countMobs(world, MobDefinition.ZOMBIE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Passive spawn rule should require grass and bright light")
    void passiveSpawnRequiresGrassAndLight() {
        World world = new World(404L);
        try {
            prepareSpawnColumn(world, 0, 70, 0, BlockType.GRASS);
            MobSpawner spawner = new MobSpawner(world);

            assertTrue(spawner.canSpawnPassiveAt(0, 70, 0));

            world.setBlock(0, 70, 0, BlockType.DIRT);

            assertFalse(spawner.canSpawnPassiveAt(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Passive spawn rule should reject dark open night grass")
    void passiveSpawnRejectsDarkOpenNightGrass() {
        World world = new World(413L);
        try {
            setTime(world, 18000.0f);
            prepareSpawnColumn(world, 0, 70, 0, BlockType.GRASS);
            MobSpawner spawner = new MobSpawner(world);

            assertFalse(spawner.canSpawnPassiveAt(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Passive spawn rule should accept block-lit grass")
    void passiveSpawnAcceptsBlockLitGrass() {
        World world = new World(414L);
        try {
            setTime(world, 18000.0f);
            prepareSpawnColumn(world, 0, 70, 0, BlockType.GRASS);
            world.setBlock(0, 73, 0, BlockType.STONE, 0);
            world.setBlock(1, 71, 0, BlockType.TORCH, 5);
            MobSpawner spawner = new MobSpawner(world);

            assertTrue(world.getBlockLight(0, 71, 0) >= 9);
            assertTrue(spawner.canSpawnPassiveAt(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 Nether hostile rules should use a fortress-specific blaze list")
    void releaseOneNetherHostileRulesUseFortressSpecificBlazeList() {
        World world = new World(97531L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            StructureGenerator.StructureLocation fortress = world.locateStructure(StructureType.NETHER_FORTRESS, 0, 0);
            assertNotNull(fortress);
            ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(97531L, Dimension.NETHER);
            StructureStart start = new StructurePlanner()
                    .startsForChunk(97531L, Dimension.NETHER, fortress.chunkX(), fortress.chunkZ(), generator)
                    .stream()
                    .filter(candidate -> candidate.type() == StructureType.NETHER_FORTRESS)
                    .filter(candidate -> candidate.chunkX() == fortress.chunkX()
                            && candidate.chunkZ() == fortress.chunkZ())
                    .findFirst()
                    .orElseThrow();
            StructureBoundingBox crossing = start.pieces().get(0).bounds();
            int fortressX = crossing.centerX();
            int fortressY = crossing.centerY();
            int fortressZ = crossing.centerZ();
            assertTrue(world.isInsideStructure(StructureType.NETHER_FORTRESS,
                    fortressX, fortressY, fortressZ));

            MobSpawner spawner = new MobSpawner(world);
            List<SpawnRule> fortressRules = spawner.hostileRulesAt(fortressX, fortressY, fortressZ);
            assertTrue(hasRule(fortressRules, MobDefinition.BLAZE));
            assertTrue(hasRule(fortressRules, MobDefinition.ZOMBIE_PIGMAN));
            assertTrue(hasRule(fortressRules, MobDefinition.MAGMA_CUBE));
            assertFalse(hasRule(fortressRules, MobDefinition.GHAST),
                    "MapGenNetherBridge uses its own fortress list instead of the general Hell biome list");

            int[] outside = outsideFortress(world, fortressX, fortressY, fortressZ);
            List<SpawnRule> ordinaryRules = spawner.hostileRulesAt(outside[0], fortressY, outside[1]);
            assertFalse(hasRule(ordinaryRules, MobDefinition.BLAZE),
                    "Blazes should not be part of the general Nether cave/floor spawn list");
            assertTrue(hasRule(ordinaryRules, MobDefinition.GHAST));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Natural passive spawns should use Release 1.0 pack sizes")
    void naturalPassiveSpawnsUseReleaseOnePackSizes() {
        World world = new World(406L);
        try {
            prepareSpawnPatch(world, 0, 70, 0, BlockType.GRASS, 4);
            MobSpawner spawner = new MobSpawner(world, new Random(2L));
            SpawnRule pigRule = new SpawnRule(MobDefinition.PIG, 10, 1, Chunk.HEIGHT - 3, false);

            assertEquals(4, spawner.spawnPassivePack(pigRule, 0, 70, 0, 10));
            world.updateEntities(0.0f);

            assertEquals(4, countMobs(world, MobDefinition.PIG));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Natural sheep spawns should use Release 1.0 weighted fleece colors")
    void naturalSheepSpawnsUseReleaseOneWeightedFleeceColors() {
        World world = new World(438L);
        try {
            prepareSpawnPatch(world, 0, 70, 0, BlockType.GRASS, 4);
            MobSpawner spawner = new MobSpawner(world, new SheepColorRandom(4, 499));
            SpawnRule sheepRule = new SpawnRule(MobDefinition.SHEEP, 12, 1, Chunk.HEIGHT - 3, false);

            assertEquals(1, spawner.spawnPassivePack(sheepRule, 0, 70, 0, 1));
            world.updateEntities(0.0f);

            Sheep sheep = (Sheep) onlyMob(world, MobDefinition.SHEEP);
            assertEquals(15, sheep.getWoolColor());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Natural sheep spawns should keep the Release 1.0 rare pink roll")
    void naturalSheepSpawnsUseReleaseOneRarePinkFleeceRoll() {
        World world = new World(439L);
        try {
            prepareSpawnPatch(world, 0, 70, 0, BlockType.GRASS, 4);
            MobSpawner spawner = new MobSpawner(world, new SheepColorRandom(18, 0));
            SpawnRule sheepRule = new SpawnRule(MobDefinition.SHEEP, 12, 1, Chunk.HEIGHT - 3, false);

            assertEquals(1, spawner.spawnPassivePack(sheepRule, 0, 70, 0, 1));
            world.updateEntities(0.0f);

            Sheep sheep = (Sheep) onlyMob(world, MobDefinition.SHEEP);
            assertEquals(6, sheep.getWoolColor());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Natural spawn packs should be clamped by the remaining mob cap")
    void naturalSpawnPacksClampToRemainingMobCap() {
        World world = new World(407L);
        try {
            prepareSpawnPatch(world, 0, 70, 0, BlockType.GRASS, 4);
            MobSpawner spawner = new MobSpawner(world, new Random(2L));
            SpawnRule pigRule = new SpawnRule(MobDefinition.PIG, 10, 1, Chunk.HEIGHT - 3, false);

            assertEquals(2, spawner.spawnPassivePack(pigRule, 0, 70, 0, 2));
            world.updateEntities(0.0f);

            assertEquals(2, countMobs(world, MobDefinition.PIG));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Natural water creature spawns should use Release 1.0 pack sizes")
    void naturalWaterCreatureSpawnsUseReleaseOnePackSizes() {
        World world = new World(408L);
        try {
            prepareWaterPatch(world, 0, 60, 0, 4);
            MobSpawner spawner = new MobSpawner(world, new Random(2L));
            SpawnRule squidRule = new SpawnRule(MobDefinition.SQUID, 10, 45, 62, true);

            assertEquals(3, spawner.spawnWaterPack(squidRule, 0, 60, 0, 10));
            world.updateEntities(0.0f);

            assertEquals(3, countMobs(world, MobDefinition.SQUID));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Natural ground spawns should receive the Release-style random yaw")
    void naturalGroundSpawnsApplyRandomYaw() {
        CapturingSpawnWorld world = new CapturingSpawnWorld(430L);
        try {
            setTime(world, 18000.0f);
            prepareSpawnColumn(world, 0, 70, 0, BlockType.STONE);
            MobSpawner spawner = new MobSpawner(world, new SpawnYawRandom(0.25f));
            SpawnRule zombieRule = new SpawnRule(MobDefinition.ZOMBIE, 10, 1, Chunk.HEIGHT - 3, false);

            assertEquals(1, spawner.spawnHostilePack(zombieRule, 0, 70, 0, 1));

            Mob zombie = assertInstanceOf(Mob.class, world.lastSpawned());
            assertSame(MobDefinition.ZOMBIE, zombie.getDefinition());
            assertEquals(90.0f, zombie.getYaw(), 0.0001f);
            assertEquals(0.0f, zombie.getPitch(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Natural water spawns should receive the Release-style random yaw")
    void naturalWaterSpawnsApplyRandomYaw() {
        CapturingSpawnWorld world = new CapturingSpawnWorld(431L);
        try {
            prepareWaterPatch(world, 0, 60, 0, 1);
            MobSpawner spawner = new MobSpawner(world, new SpawnYawRandom(0.5f));
            SpawnRule squidRule = new SpawnRule(MobDefinition.SQUID, 10, 45, 62, true);

            assertEquals(1, spawner.spawnWaterPack(squidRule, 0, 60, 0, 1));

            Mob squid = assertInstanceOf(Mob.class, world.lastSpawned());
            assertSame(MobDefinition.SQUID, squid.getDefinition());
            assertEquals(180.0f, squid.getYaw(), 0.0001f);
            assertEquals(0.0f, squid.getPitch(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Water creature spawns should accept surface water with non-solid head space")
    void waterCreatureSpawnsAcceptSurfaceWaterWithNonSolidHeadSpace() {
        World world = new World(419L);
        try {
            world.getChunkNow(0, 0);
            world.setBlock(0, 60, 0, BlockType.WATER, 0);
            world.setBlock(0, 61, 0, BlockType.AIR, 0);
            world.setBlock(0, 62, 0, BlockType.AIR, 0);
            MobSpawner spawner = new MobSpawner(world, new Random(0L));
            SpawnRule squidRule = new SpawnRule(MobDefinition.SQUID, 10, 45, 62, true);

            assertTrue(squidRule.matches(world, 0, 60, 0));
            assertEquals(1, spawner.spawnWaterPack(squidRule, 0, 60, 0, 1));
            world.updateEntities(0.0f);
            assertEquals(1, countMobs(world, MobDefinition.SQUID));

            world.setBlock(4, 60, 0, BlockType.WATER, 0);
            world.setBlock(4, 61, 0, BlockType.STONE, 0);

            assertFalse(squidRule.matches(world, 4, 60, 0));
            assertEquals(0, spawner.spawnWaterPack(squidRule, 4, 60, 0, 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Runtime water-creature spawning should use surface water cells")
    void runtimeWaterCreatureSpawningUsesSurfaceWaterCells() {
        World world = new World(441L);
        try {
            prepareSurfaceWaterPatch(world, -112, 60, 0, 1);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.setDifficulty(Difficulty.PEACEFUL);
            world.setPlayer(player);
            player.setWorld(world);
            List<Mob> existing = new ArrayList<>();
            for (int i = 0; i < MobSpawner.releaseOneMobCap(10); i++) {
                Mob mob = MobFactory.create(MobDefinition.COW);
                assertNotNull(mob);
                mob.setPosition(4.5f + i * 0.01f, 70.0f, 4.5f);
                existing.add(mob);
            }
            world.replaceEntities(existing);
            MobSpawner spawner = new MobSpawner(world, new EdgeChunkSpawnRandom());

            spawner.tick();
            world.updateEntities(0.0f);

            assertEquals(1, countMobs(world, MobDefinition.SQUID));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Public natural mob spawner should use the owning world's RNG")
    void publicMobSpawnerUsesWorldRandom() {
        CountingIntRandom random = new CountingIntRandom(0);
        World world = new RandomOverrideWorld(415L, random);
        try {
            prepareWaterPatch(world, 0, 60, 0, 1);
            MobSpawner spawner = new MobSpawner(world);
            SpawnRule squidRule = new SpawnRule(MobDefinition.SQUID, 10, 45, 62, true);

            assertEquals(1, spawner.spawnWaterPack(squidRule, 0, 60, 0, 10));
            assertEquals(1, random.nextIntCalls());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Natural passive spawn rules should use Release 1.0 biome creature lists")
    void naturalPassiveSpawnRulesUseReleaseOneBiomeCreatureLists() {
        World world = new World(424242L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            MobSpawner spawner = new MobSpawner(world);

            assertSame(BiomeType.FOREST, world.getReleaseBiome(0, 0));
            assertEquals(5, ruleWeight(spawner.passiveRulesAt(0, 70, 0), MobDefinition.WOLF));

            assertSame(BiomeType.TAIGA, world.getReleaseBiome(-6496, -1216));
            assertEquals(8, ruleWeight(spawner.passiveRulesAt(-6496, 70, -1216), MobDefinition.WOLF));

            assertSame(BiomeType.DESERT, world.getReleaseBiome(-7328, -2896));
            assertTrue(spawner.passiveRulesAt(-7328, 70, -2896).isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mushroom islands should naturally spawn mooshrooms without hostile mobs")
    void mushroomIslandsUseMooshroomOnlyNaturalCreatureList() {
        int mushroomX = -1877 * Chunk.WIDTH + 16;
        int mushroomZ = -349 * Chunk.DEPTH + 16;
        World world = new World(5L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            MobSpawner spawner = new MobSpawner(world);

            assertSame(BiomeType.MUSHROOM_ISLAND, world.getReleaseBiome(mushroomX, mushroomZ));
            List<SpawnRule> passiveRules = spawner.passiveRulesAt(mushroomX, 70, mushroomZ);
            assertEquals(1, passiveRules.size());
            assertEquals(8, ruleWeight(passiveRules, MobDefinition.MOOSHROOM));
            assertTrue(spawner.hostileRulesAt(mushroomX, 70, mushroomZ).isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("The End hostile spawn rules should only expose Endermen")
    void endHostileSpawnRulesOnlyExposeEndermen() {
        World world = new World(410L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            MobSpawner spawner = new MobSpawner(world);
            List<SpawnRule> rules = spawner.hostileRulesAt(0, 70, 0);

            assertEquals(1, rules.size());
            assertEquals(MobDefinition.ENDERMAN, rules.get(0).definition());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Natural mooshroom packs should require mycelium support")
    void naturalMooshroomPacksRequireMyceliumSupport() {
        SpawnRule mooshroomRule = new SpawnRule(MobDefinition.MOOSHROOM, 8, 1, Chunk.HEIGHT - 3, false);
        World world = new World(409L);
        try {
            prepareSpawnColumn(world, 0, 70, 0, BlockType.GRASS);
            MobSpawner spawner = new MobSpawner(world, new Random(2L));
            assertEquals(0, spawner.spawnPassivePack(mooshroomRule, 0, 70, 0, 1));

            world.setBlock(0, 70, 0, BlockType.MYCELIUM);

            assertEquals(1, spawner.spawnPassivePack(mooshroomRule, 0, 70, 0, 1));
            world.updateEntities(0.0f);
            assertEquals(1, countMobs(world, MobDefinition.MOOSHROOM));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Natural spider spawns should roll Release 1.0 spider jockeys")
    void naturalSpiderSpawnsRollSpiderJockeys() {
        SpawnRule spiderRule = new SpawnRule(MobDefinition.SPIDER, 10, 1, Chunk.HEIGHT - 3, false);
        World world = new World(426L);
        try {
            setTime(world, 18000.0f);
            prepareSpawnVolume(world, 0, 70, 0, BlockType.STONE, 2, 4);
            MobSpawner spawner = new MobSpawner(world, new ZeroRandom());

            assertEquals(1, spawner.spawnHostilePack(spiderRule, 0, 70, 0, 1));
            world.updateEntities(0.0f);

            Spider spider = onlyMob(world, Spider.class);
            Skeleton skeleton = onlyMob(world, Skeleton.class);
            assertSame(skeleton, spider.getJockeyRider());
            assertSame(spider, skeleton.getRidingSpider());
            assertEquals(spider.getX(), skeleton.getX(), 0.0001f);
            assertEquals(spider.getY() + spider.getHeight() * 0.75f, skeleton.getY(), 0.0001f);
            assertEquals(spider.getZ(), skeleton.getZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spider jockey riders should stay pinned to their spider while mounted")
    void spiderJockeyRiderStaysPinnedToSpider() {
        World world = new World(427L);
        try {
            Spider spider = new Spider();
            spider.setPosition(0.5f, 71.0f, 0.5f);
            Skeleton skeleton = new Skeleton();
            assertTrue(spider.mountJockey(skeleton));
            world.replaceEntities(List.of(spider, skeleton));

            spider.setPosition(4.5f, 72.0f, -3.5f);
            world.updateEntities(0.0f);

            assertEquals(4.5f, skeleton.getX(), 0.0001f);
            assertEquals(spider.getY() + spider.getHeight() * 0.75f, skeleton.getY(), 0.0001f);
            assertEquals(-3.5f, skeleton.getZ(), 0.0001f);

            spider.remove();
            assertNull(skeleton.getRidingSpider());
        } finally {
            world.cleanup();
        }
    }

    private static boolean hasRule(List<SpawnRule> rules, MobDefinition definition) {
        return rules.stream().anyMatch(rule -> rule.definition() == definition);
    }

    private static int ruleWeight(List<SpawnRule> rules, MobDefinition definition) {
        return rules.stream()
                .filter(rule -> rule.definition() == definition)
                .mapToInt(SpawnRule::weight)
                .findFirst()
                .orElse(-1);
    }

    private static long countMobs(World world, MobDefinition definition) {
        return world.getEntities().stream()
                .filter(Mob.class::isInstance)
                .map(Mob.class::cast)
                .filter(mob -> mob.getDefinition() == definition)
                .count();
    }

    private static <T extends Mob> T onlyMob(World world, Class<T> type) {
        List<T> mobs = world.getEntities().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
        assertEquals(1, mobs.size());
        return mobs.get(0);
    }

    private static Mob onlyMob(World world, MobDefinition definition) {
        List<Mob> mobs = world.getEntities().stream()
                .filter(Mob.class::isInstance)
                .map(Mob.class::cast)
                .filter(mob -> mob.getDefinition() == definition)
                .toList();
        assertEquals(1, mobs.size(), "Mobs in world: " + describeMobs(world));
        return mobs.get(0);
    }

    private static List<String> describeMobs(World world) {
        return world.getEntities().stream()
                .filter(Mob.class::isInstance)
                .map(Mob.class::cast)
                .map(mob -> mob.getDefinition() + "@"
                        + mob.getX() + "," + mob.getY() + "," + mob.getZ())
                .toList();
    }

    private static int[] outsideFortress(World world, int originX, int y, int originZ) {
        for (int distance = 512; distance <= 4096; distance += 512) {
            int x = originX + distance;
            int z = originZ + distance;
            if (!world.isInsideStructure(StructureType.NETHER_FORTRESS, x, y, z)) {
                return new int[] { x, z };
            }
        }
        fail("Expected to find a non-fortress Nether coordinate near the fixture fortress");
        return new int[] { originX + 4096, originZ + 4096 };
    }

    private static int[] findSlimeColumn(MobSpawner spawner, boolean slimeChunk) {
        for (int chunkX = -32; chunkX <= 32; chunkX++) {
            for (int chunkZ = -32; chunkZ <= 32; chunkZ++) {
                int blockX = chunkX * Chunk.WIDTH + Chunk.WIDTH / 2;
                int blockZ = chunkZ * Chunk.DEPTH + Chunk.DEPTH / 2;
                if (spawner.isSlimeChunk(blockX, blockZ) == slimeChunk) {
                    return new int[] { blockX, blockZ };
                }
            }
        }
        fail("Expected to find a " + (slimeChunk ? "slime" : "non-slime") + " chunk near the origin");
        return new int[] { 0, 0 };
    }

    private static void setTime(World world, float time) {
        DayCycleManager dayCycle = new DayCycleManager();
        dayCycle.setTime(time);
        world.setDayCycleManager(dayCycle);
    }

    private static void prepareSpawnColumn(World world, int x, int y, int z, BlockType ground) {
        world.getChunkNow(Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH));
        world.setBlock(x, y, z, ground);
        world.setBlock(x, y + 1, z, BlockType.AIR);
        world.setBlock(x, y + 2, z, BlockType.AIR);
    }

    private static void prepareSpawnPatch(World world, int centerX, int y, int centerZ, BlockType ground, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                prepareSpawnColumn(world, x, y, z, ground);
            }
        }
    }

    private static void prepareTwoLevelSpawnColumn(World world, int x, int lowY, int highY, int z,
            BlockType ground) {
        world.getChunkNow(Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH));
        for (int y = lowY; y < Chunk.HEIGHT; y++) {
            world.setBlock(x, y, z, BlockType.AIR, 0);
        }
        world.setBlock(x, lowY, z, ground, 0);
        world.setBlock(x, highY, z, ground, 0);
    }

    private static void prepareTallSpawnPatch(World world, int centerX, int y, int centerZ,
            BlockType ground, int radius, int airHeight) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                world.getChunkNow(Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH));
                world.setBlock(x, y, z, ground);
                for (int dy = 1; dy <= airHeight; dy++) {
                    world.setBlock(x, y + dy, z, BlockType.AIR);
                }
            }
        }
    }

    private static void prepareSpawnVolume(World world, int centerX, int y, int centerZ,
            BlockType ground, int radius, int height) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                world.getChunkNow(Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH));
                world.setBlock(x, y, z, ground, 0);
                for (int dy = 1; dy <= height; dy++) {
                    world.setBlock(x, y + dy, z, BlockType.AIR, 0);
                }
            }
        }
    }

    private static void prepareAirSpawnVolume(World world, int centerX, int y, int centerZ,
            int radius, int height) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                world.getChunkNow(Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH));
                for (int dy = 0; dy <= height; dy++) {
                    world.setBlock(x, y + dy, z, BlockType.AIR, 0);
                }
            }
        }
    }

    private static void prepareWaterPatch(World world, int centerX, int y, int centerZ, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                world.getChunkNow(Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH));
                world.setBlock(x, y, z, BlockType.WATER);
                world.setBlock(x, y + 1, z, BlockType.WATER);
                world.setBlock(x, y + 2, z, BlockType.AIR);
            }
        }
    }

    private static void prepareSurfaceWaterPatch(World world, int centerX, int y, int centerZ, int radius) {
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                world.getChunkNow(Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH));
                world.setBlock(x, y, z, BlockType.WATER);
                world.setBlock(x, y + 1, z, BlockType.AIR);
            }
        }
    }

    private static void assertNaturalHostileSpawnAtCandidate(int worldSpawnX, int worldSpawnY, int worldSpawnZ,
            long expectedMobs) {
        World world = new World(420L);
        try {
            setTime(world, 18000.0f);
            world.setWorldSpawn(worldSpawnX, worldSpawnY, worldSpawnZ);
            prepareSpawnColumn(world, -112, 70, 0, BlockType.STONE);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            player.setWorld(world);
            MobSpawner spawner = new MobSpawner(world, new EdgeChunkSpawnRandom());

            spawner.tick();
            world.updateEntities(0.0f);

            assertEquals(expectedMobs, countMobs(world, MobDefinition.ZOMBIE));
        } finally {
            world.cleanup();
        }
    }

    private static final class CapturingSpawnWorld extends World {
        private Entity lastSpawned;

        private CapturingSpawnWorld(long seed) {
            super(seed);
        }

        private CapturingSpawnWorld(long seed, String generatorId, Dimension dimension) {
            super(seed, generatorId, dimension);
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

    private static final class SpawnYawRandom extends Random {
        private final float yawFloat;

        private SpawnYawRandom(float yawFloat) {
            this.yawFloat = yawFloat;
        }

        @Override
        public int nextInt(int bound) {
            if (bound == 32) {
                return 31;
            }
            if (bound == 8) {
                return 7;
            }
            return 0;
        }

        @Override
        public float nextFloat() {
            return yawFloat;
        }
    }

    private static final class SheepColorRandom extends Random {
        private final int colorRoll;
        private final int rarePinkRoll;

        private SheepColorRandom(int colorRoll, int rarePinkRoll) {
            this.colorRoll = colorRoll;
            this.rarePinkRoll = rarePinkRoll;
        }

        @Override
        public int nextInt(int bound) {
            if (bound == 100) {
                return colorRoll;
            }
            if (bound == 500) {
                return rarePinkRoll;
            }
            return 0;
        }

        @Override
        public float nextFloat() {
            return 0.25f;
        }
    }

    private static final class CountingIntRandom extends Random {
        private final int value;
        private int calls;

        private CountingIntRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            calls++;
            if (value < 0 || value >= bound) {
                throw new IllegalArgumentException("Fixed random value " + value + " outside bound " + bound);
            }
            return value;
        }

        private int nextIntCalls() {
            return calls;
        }
    }

    private static final class HostileLightGateRandom extends Random {
        private final int skyRoll;
        private final int blockRoll;

        private HostileLightGateRandom(int skyRoll, int blockRoll) {
            this.skyRoll = skyRoll;
            this.blockRoll = blockRoll;
        }

        @Override
        public int nextInt(int bound) {
            if (bound == 32) {
                return skyRoll;
            }
            if (bound == 8) {
                return blockRoll;
            }
            return 0;
        }
    }

    private static final class ZeroRandom extends Random {
        @Override
        public int nextInt(int bound) {
            if (bound == 32) {
                return 31;
            }
            if (bound == 8) {
                return 7;
            }
            return 0;
        }

        @Override
        public boolean nextBoolean() {
            return false;
        }
    }

    private static final class GhastGateRandom extends Random {
        private final int gateRoll;

        private GhastGateRandom(int gateRoll) {
            this.gateRoll = gateRoll;
        }

        @Override
        public int nextInt(int bound) {
            if (bound == 20) {
                return gateRoll;
            }
            return 0;
        }

        @Override
        public float nextFloat() {
            return 0.25f;
        }

        @Override
        public boolean nextBoolean() {
            return false;
        }
    }

    private static final class EdgeChunkSpawnRandom extends Random {
        private boolean selectedStartChunk;
        private boolean selectedGroundY;

        @Override
        public int nextInt(int bound) {
            if (bound == 32) {
                return 31;
            }
            if (bound == 8) {
                return 7;
            }
            if (!selectedStartChunk && bound == 17 * 17) {
                selectedStartChunk = true;
                return 8 * 17;
            }
            if (!selectedGroundY && bound > Chunk.WIDTH && bound <= Chunk.HEIGHT) {
                selectedGroundY = true;
                return Math.min(70, bound - 1);
            }
            return 0;
        }

        @Override
        public float nextFloat() {
            return 1.0f;
        }

        @Override
        public boolean nextBoolean() {
            return false;
        }
    }

    private static final class ThreeGroupChunkSpawnRandom extends Random {
        private int jitterCalls;
        private boolean selectedGroundY;

        @Override
        public int nextInt(int bound) {
            if (bound == 32) {
                return 31;
            }
            if (bound == 8) {
                return 7;
            }
            if (bound == 17 * 17) {
                return 8 * 17;
            }
            if (!selectedGroundY && bound > Chunk.WIDTH && bound <= Chunk.HEIGHT) {
                selectedGroundY = true;
                return Math.min(70, bound - 1);
            }
            if (bound == Chunk.WIDTH || bound == Chunk.DEPTH) {
                return 0;
            }
            if (bound == 6) {
                int[] values = {
                        0, 0, 0, 0,
                        2, 0, 0, 0,
                        4, 0, 0, 0
                };
                return jitterCalls < values.length ? values[jitterCalls++] : 0;
            }
            return 0;
        }

        @Override
        public float nextFloat() {
            return 1.0f;
        }

        @Override
        public boolean nextBoolean() {
            return false;
        }
    }

    private static final class NaturalGroundYSpawnRandom extends Random {
        private final int groundY;
        private boolean selectedStartChunk;
        private boolean selectedGroundY;

        private NaturalGroundYSpawnRandom(int groundY) {
            this.groundY = groundY;
        }

        @Override
        public int nextInt(int bound) {
            if (bound == 32) {
                return 31;
            }
            if (bound == 8) {
                return 7;
            }
            if (!selectedStartChunk && bound == 17 * 17) {
                selectedStartChunk = true;
                return 8 * 17 + 1;
            }
            if (!selectedGroundY && bound > Chunk.WIDTH && bound <= Chunk.HEIGHT) {
                selectedGroundY = true;
                return Math.min(groundY, bound - 1);
            }
            return 0;
        }

        @Override
        public float nextFloat() {
            return 1.0f;
        }

        @Override
        public boolean nextBoolean() {
            return false;
        }
    }

    private static final class RandomOverrideWorld extends World {
        private final Random random;

        private RandomOverrideWorld(long seed, Random random) {
            super(seed);
            this.random = random;
        }

        @Override
        public Random getRandom() {
            return random;
        }
    }

    private static final class FixedLightWorld extends World {
        private final int skyLight;
        private final int blockLight;

        private FixedLightWorld(long seed, int skyLight, int blockLight) {
            super(seed);
            this.skyLight = skyLight;
            this.blockLight = blockLight;
        }

        @Override
        public int getSkyLight(int x, int y, int z) {
            return skyLight;
        }

        @Override
        public int getBlockLight(int x, int y, int z) {
            return blockLight;
        }
    }
}
