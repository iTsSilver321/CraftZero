package com.craftzero.world;

import com.craftzero.entity.EndCrystalEntity;
import com.craftzero.entity.DroppedItem;
import com.craftzero.entity.Entity;
import com.craftzero.entity.ExperienceOrbEntity;
import com.craftzero.entity.mob.EnderDragon;
import com.craftzero.combat.DamageSource;
import com.craftzero.main.Player;
import com.craftzero.main.PlayerStats;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

class EndProgressionTest {
    @Test
    @DisplayName("End portal frames should activate a 3x3 End portal when the last eye is inserted")
    void completeEndPortalActivates() {
        World world = new World(8001L);
        try {
            int cx = 0;
            int y = 40;
            int cz = 0;
            buildEndPortalRing(world, cx, y, cz);
            world.setBlock(cx + 2, y, cz, BlockType.END_PORTAL_FRAME, 1);

            assertTrue(world.addEyeToEndPortalFrame(cx + 2, y, cz));
            for (int x = cx - 1; x <= cx + 1; x++) {
                for (int z = cz - 1; z <= cz + 1; z++) {
                    assertSame(BlockType.END_PORTAL, world.getBlock(x, y, z));
                }
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Eye insertion should emit the Release-style smoke burst above the frame")
    void eyeInsertionEmitsSmokeBurstAboveFrame() {
        World world = new World(8009L);
        try {
            int x = 3;
            int y = 40;
            int z = -2;
            world.setBlock(x, y, z, BlockType.END_PORTAL_FRAME, 0);

            assertTrue(world.addEyeToEndPortalFrame(x, y, z));

            assertEquals(16, world.getParticles().size());
            for (WorldParticle particle : world.getParticles()) {
                assertSame(WorldParticle.Type.SMOKE, particle.getType());
                float particleX = particle.getRenderX(1.0f);
                float particleY = particle.getRenderY(1.0f);
                float particleZ = particle.getRenderZ(1.0f);
                assertTrue(particleX >= x + 5.0f / 16.0f && particleX <= x + 11.0f / 16.0f);
                assertEquals(y + 0.8125f, particleY, 0.0001f);
                assertTrue(particleZ >= z + 5.0f / 16.0f && particleZ <= z + 11.0f / 16.0f);
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("The End generator should create island, pillars, crystals, and one dragon")
    void endGeneratorCreatesProgressionEntities() throws Exception {
        World world = new World(8002L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            Chunk origin = world.getChunkNow(0, 0);
            assertTrue(contains(origin, BlockType.END_STONE));
            EnderDragon stagedDragon = stagedGeneratedEntities(world).stream()
                    .filter(EnderDragon.class::isInstance)
                    .map(EnderDragon.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(223.70831f, stagedDragon.getYaw(), 0.0001f,
                    "BiomeEndDecorator assigns the dragon yaw after the inherited ore RNG phase");
            Chunk spike = findChunkWithBlocks(world, 8, BlockType.OBSIDIAN, BlockType.BEDROCK);
            assertNotNull(spike, "The End decorator should place obsidian spikes with bedrock crystal caps");
            world.updateEntities(1.0f / 20.0f);

            assertEquals(1L, world.getEntities().stream().filter(EnderDragon.class::isInstance).count());
            assertTrue(world.getEntities().stream().anyMatch(EndCrystalEntity.class::isInstance));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("End portal frames should require inward-facing frame metadata")
    void endPortalRequiresInwardFacingFrames() {
        World world = new World(8011L);
        try {
            int cx = 0;
            int y = 40;
            int cz = 0;
            buildEndPortalRing(world, cx, y, cz);
            world.setBlock(cx, y, cz - 2, BlockType.END_PORTAL_FRAME,
                    1 | World.END_PORTAL_FRAME_EYE_BIT);
            world.setBlock(cx + 2, y, cz, BlockType.END_PORTAL_FRAME, 1);

            assertTrue(world.addEyeToEndPortalFrame(cx + 2, y, cz));
            assertFalse(world.isCompleteEndPortalFrame(cx, y, cz));
            for (int x = cx - 1; x <= cx + 1; x++) {
                for (int z = cz - 1; z <= cz + 1; z++) {
                    assertNotSame(BlockType.END_PORTAL, world.getBlock(x, y, z));
                }
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("End entry should rebuild the Release 1.0 obsidian spawn platform")
    void endEntryRebuildsReleaseOneObsidianPlatform() {
        World world = new World(8006L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            int cx = (int) Math.floor(DimensionTransferService.END_SPAWN_X);
            int cy = (int) DimensionTransferService.END_SPAWN_Y;
            int cz = (int) Math.floor(DimensionTransferService.END_SPAWN_Z);

            for (int x = cx - 2; x <= cx + 2; x++) {
                for (int z = cz - 2; z <= cz + 2; z++) {
                    world.setBlock(x, cy - 1, z, BlockType.END_STONE, 0);
                    for (int y = cy; y <= cy + 3; y++) {
                        world.setBlock(x, y, z, BlockType.STONE, 0);
                    }
                }
            }
            world.setBlock(cx + 3, cy - 1, cz, BlockType.STONE, 0);

            world.ensureEndSpawnPlatform();

            for (int x = cx - 2; x <= cx + 2; x++) {
                for (int z = cz - 2; z <= cz + 2; z++) {
                    assertSame(BlockType.OBSIDIAN, world.getBlock(x, cy - 1, z),
                            "The End entry platform should be a 5x5 obsidian layer at y=48");
                    for (int y = cy; y <= cy + 2; y++) {
                        assertSame(BlockType.AIR, world.getBlock(x, y, z),
                                "The End entry platform should clear three blocks of headroom");
                    }
                    assertSame(BlockType.STONE, world.getBlock(x, cy + 3, z),
                            "The Release-era platform clear should not erase a fourth air layer");
                }
            }
            assertSame(BlockType.STONE, world.getBlock(cx + 3, cy - 1, cz),
                    "The End entry platform should stay within the 5x5 footprint");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("The End terrain should form a finite floating island from the old density field")
    void endTerrainUsesFloatingIslandDensityField() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(8002L, Dimension.THE_END);
        Chunk origin = new Chunk(0, 0);
        Chunk distant = new Chunk(24, 24);

        generator.generateChunk(null, origin, 0, 0);
        generator.generateChunk(null, distant, 24, 24);

        assertTrue(contains(origin, BlockType.END_STONE));
        assertTrue(countBlocks(origin, BlockType.END_STONE) > 512);
        assertTrue(countBlocks(distant, BlockType.END_STONE) < 16,
                "Far End chunks should fade to empty space instead of continuing terrain");
    }

    @Test
    @DisplayName("The End density field should match deterministic Release 1.0 island fixtures")
    void endTerrainMatchesReleaseOneDensityFixtures() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(8002L, Dimension.THE_END);
        assertEndBaseChunk(generator, new EndBaseChunkExpectation(0, 0, 13065, 12, 63,
                8, 62, 8, 8, 63, 8));
        assertEndBaseChunk(generator, new EndBaseChunkExpectation(4, 0, 8751, 19, 59,
                8, 58, 8, 8, 59, 8));
        assertEndBaseChunk(generator, new EndBaseChunkExpectation(8, 8, 0, -1, -1,
                -1, -1, -1, 8, 48, 8));
        assertEndBaseChunk(generator, new EndBaseChunkExpectation(-4, 0, 10777, 14, 59,
                8, 48, 8, 8, 58, 8));
        assertEndBaseChunk(generator, new EndBaseChunkExpectation(4, 4, 1743, 27, 56,
                0, 48, 0, 8, 48, 8));

        ReleaseOneWorldGenerator alternateSeed = new ReleaseOneWorldGenerator(1234L, Dimension.THE_END);
        assertEndBaseChunk(alternateSeed, new EndBaseChunkExpectation(6, 0, 6831, 26, 58,
                8, 48, 8, 8, 58, 8));
    }

    @Test
    @DisplayName("The End raw density grid should match Release 1.0 source vectors")
    void endRawDensityMatchesReleaseOneSourceVectors() throws Exception {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(8002L, Dimension.THE_END);

        double[] origin = invokeEndDensities(generator, 0, 0);
        assertEndDensityVector(origin, 0, 0, 0, -45.164517275010200);
        assertEndDensityVector(origin, 0, 8, 0, 72.164800843053430);
        assertEndDensityVector(origin, 1, 12, 0, 74.638430859229960);
        assertEndDensityVector(origin, 2, 24, 2, -401.462150910646700);
        assertEquals(-924723426, Arrays.hashCode(origin));

        double[] falloff = invokeEndDensities(generator, 4, 0);
        assertEndDensityVector(falloff, 0, 8, 0, 36.441772575136640);
        assertEndDensityVector(falloff, 1, 16, 1, -74.260597264203530);
        assertEquals(-1418926504, Arrays.hashCode(falloff));

        double[] voidChunk = invokeEndDensities(generator, 8, 8);
        assertEndDensityVector(voidChunk, 0, 8, 0, -91.835609132373480);
        assertEndDensityVector(voidChunk, 1, 16, 1, -191.027145296168600);
        assertEquals(314764734, Arrays.hashCode(voidChunk));

        double[] negativeFalloff = invokeEndDensities(generator, -4, 0);
        assertEndDensityVector(negativeFalloff, 0, 8, 0, 25.320325146109990);
        assertEndDensityVector(negativeFalloff, 2, 8, 0, 42.523427385616700);
        assertEndDensityVector(negativeFalloff, 2, 24, 2, -433.557320363830600);
        assertEquals(-2060868023, Arrays.hashCode(negativeFalloff));

        double[] diagonalRim = invokeEndDensities(generator, 4, 4);
        assertEndDensityVector(diagonalRim, 0, 8, 0, 6.756780318837069);
        assertEndDensityVector(diagonalRim, 1, 12, 2, -1.996119041338741);
        assertEquals(1768565409, Arrays.hashCode(diagonalRim));

        ReleaseOneWorldGenerator alternateSeed = new ReleaseOneWorldGenerator(1234L, Dimension.THE_END);
        double[] alternateOrigin = invokeEndDensities(alternateSeed, 0, 0);
        assertEquals(2082441182, Arrays.hashCode(alternateOrigin));

        double[] alternateRim = invokeEndDensities(alternateSeed, 6, 0);
        assertEndDensityVector(alternateRim, 0, 8, 0, 12.444319178399638);
        assertEndDensityVector(alternateRim, 1, 12, 2, 5.424778128754838);
        assertEndDensityVector(alternateRim, 2, 16, 1, -78.000012074202670);
        assertEquals(-636917316, Arrays.hashCode(alternateRim));
    }

    @Test
    @DisplayName("End spikes should spill across chunk borders from shifted decorator origins")
    void endSpikeGenerationSpillsAcrossChunkBorders() {
        World world = new World(8002L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            Chunk spill = world.getChunkNow(-3, 1);

            assertSame(BlockType.OBSIDIAN, spill.getBlock(15, 63, 7),
                    "A spike centered in the eastern chunk should still write its western rim into this chunk");
            assertFalse(contains(spill, BlockType.BEDROCK),
                    "The spill chunk should receive rim obsidian without owning the crystal cap");

            Chunk source = world.getChunkNow(-2, 1);

            assertSame(BlockType.BEDROCK, source.getBlock(2, 97, 7));
            assertSame(BlockType.OBSIDIAN, source.getBlock(2, 63, 7));
            world.updateEntities(1.0f / 20.0f);
            EndCrystalEntity crystal = world.getEntities().stream()
                    .filter(EndCrystalEntity.class::isInstance)
                    .map(EndCrystalEntity.class::cast)
                    .filter(entity -> entity.getX() == -29.5f && entity.getY() == 97.0f
                            && entity.getZ() == 23.5f)
                    .findFirst()
                    .orElseThrow();
            assertEquals(131.8435f, crystal.getYaw(), 0.0001f,
                    "WorldGenSpikes assigns a random crystal yaw after height/radius draws");
            assertSame(BlockType.FIRE, world.getBlock(-30, 97, 23),
                    "Generated End crystals should maintain source-style fire at their cap block");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Null-world End generation should still include spike blocks")
    void nullWorldEndGenerationIncludesSpikeBlocks() {
        ReleaseOneWorldGenerator generator = new ReleaseOneWorldGenerator(8002L, Dimension.THE_END);

        Chunk spill = generatedEndChunk(generator, -3, 1);
        assertSame(BlockType.OBSIDIAN, spill.getBlock(15, 63, 7),
                "Null-world End chunks should receive spike rim obsidian");
        assertFalse(contains(spill, BlockType.BEDROCK),
                "The spill chunk should not receive the owning spike cap");

        Chunk source = generatedEndChunk(generator, -2, 1);
        assertSame(BlockType.BEDROCK, source.getBlock(2, 97, 7));
        assertSame(BlockType.OBSIDIAN, source.getBlock(2, 63, 7));
    }

    @Test
    @DisplayName("End crystals heal dragons from the nearest crystal inside a 32-block cuboid")
    void endCrystalHealingUsesNearestCrystalInCuboidRange() {
        World world = new World(8003L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            EnderDragon dragon = new EnderDragon();
            dragon.setPosition(0.0f, 80.0f, 0.0f);
            dragon.setHealth(100.0f);
            EndCrystalEntity near = new EndCrystalEntity(12.0f, 80.0f, 0.0f);
            EndCrystalEntity farther = new EndCrystalEntity(24.0f, 80.0f, 0.0f);
            world.replaceEntities(List.of(dragon, farther, near));

            advanceEntityTicks(world, 10);

            assertEquals(101.0f, dragon.getHealth(), 0.001f);
            assertTrue(dragon.isChargingFrom(near));
            assertFalse(dragon.isChargingFrom(farther));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("End crystals should link to dragons before the first healing pulse")
    void endCrystalLinksBeforeFirstHealingPulse() {
        World world = new World(8022L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            EnderDragon dragon = new EnderDragon();
            dragon.setPosition(0.0f, 80.0f, 0.0f);
            dragon.setHealth(100.0f);
            EndCrystalEntity crystal = new EndCrystalEntity(20.0f, 80.0f, 0.0f);
            world.replaceEntities(List.of(dragon, crystal));

            advanceEntityTicks(world, 1);

            float linkedHealth = dragon.getHealth();
            assertEquals(100.0f, linkedHealth, 0.001f,
                    "The first tick should establish the crystal link without applying the 10-tick heal yet");
            assertSame(crystal, dragon.getHealingCrystal());
            assertTrue(dragon.isChargingFrom(crystal));

            assertTrue(crystal.damage(1.0f, DamageSource.generic()));

            assertEquals(linkedHealth - 10.0f, dragon.getHealth(), 0.001f);
            assertFalse(dragon.isChargingFrom(crystal));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("End crystals outside the 32-block healing cuboid should not heal dragons")
    void endCrystalOutsideHealingCuboidDoesNotHealDragon() {
        World world = new World(8004L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            EnderDragon dragon = new EnderDragon();
            dragon.setPosition(0.0f, 80.0f, 0.0f);
            dragon.setHealth(100.0f);
            EndCrystalEntity outside = new EndCrystalEntity(40.0f, 80.0f, 0.0f);
            world.replaceEntities(List.of(dragon, outside));

            advanceEntityTicks(world, 10);

            assertEquals(100.0f, dragon.getHealth(), 0.001f);
            assertFalse(dragon.isChargingFrom(outside));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Destroying the crystal a dragon is charging from should damage the dragon")
    void destroyingActiveHealingCrystalDamagesDragon() {
        World world = new World(8005L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            EnderDragon dragon = new EnderDragon();
            dragon.setPosition(0.0f, 80.0f, 0.0f);
            dragon.setHealth(100.0f);
            EndCrystalEntity crystal = new EndCrystalEntity(20.0f, 80.0f, 0.0f);
            world.replaceEntities(List.of(dragon, crystal));
            advanceEntityTicks(world, 10);

            float healedHealth = dragon.getHealth();

            assertTrue(crystal.damage(1.0f, DamageSource.generic()));

            assertEquals(healedHealth - 10.0f, dragon.getHealth(), 0.001f);
            assertFalse(dragon.isChargingFrom(crystal));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Explosion-destroyed healing crystals should hurt the dragon without recursive blasts")
    void explosionDestroyedHealingCrystalDamagesDragonWithoutRecursiveBlast() {
        World world = new World(8021L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            EnderDragon dragon = new EnderDragon();
            dragon.setPosition(0.0f, 80.0f, 0.0f);
            dragon.setHealth(100.0f);
            EndCrystalEntity crystal = new EndCrystalEntity(20.0f, 80.0f, 0.0f);
            world.replaceEntities(List.of(dragon, crystal));
            advanceEntityTicks(world, 10);

            float healedHealth = dragon.getHealth();

            assertTrue(crystal.damage(5.0f, DamageSource.point(DamageSource.Type.EXPLOSION,
                    crystal.getX(), crystal.getY(), crystal.getZ(), 0.0f, 0.0f)));

            assertEquals(healedHealth - 10.0f, dragon.getHealth(), 0.001f);
            assertFalse(dragon.isChargingFrom(crystal));
            assertTrue(crystal.isRemoved());
            assertFalse(crystal.isExploded(),
                    "Explosion-sourced End crystal damage should not create a second crystal explosion");
            assertTrue(world.drainSoundEvents().isEmpty());
            assertEquals(0, hugeExplosionParticles(world));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("End crystals should maintain fire only in The End")
    void endCrystalMaintainsFireOnlyInEndDimension() {
        World end = new World(8010L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        World overworld = new World(8011L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            EndCrystalEntity endCrystal = new EndCrystalEntity(0.5f, 80.0f, 0.5f);
            EndCrystalEntity overworldCrystal = new EndCrystalEntity(0.5f, 80.0f, 0.5f);
            end.setBlock(0, 80, 0, BlockType.AIR, 0);
            overworld.setBlock(0, 80, 0, BlockType.AIR, 0);
            end.replaceEntities(List.of(endCrystal));
            overworld.replaceEntities(List.of(overworldCrystal));

            advanceEntityTicks(end, 1);
            advanceEntityTicks(overworld, 1);

            assertSame(BlockType.FIRE, end.getBlock(0, 80, 0));
            assertSame(BlockType.AIR, overworld.getBlock(0, 80, 0));
        } finally {
            end.cleanup();
            overworld.cleanup();
        }
    }

    @Test
    @DisplayName("Ender Dragon flight should destroy ordinary blocks but leave End pillars intact")
    void enderDragonFlightDestroysBreakableBlocksAndPreservesImmuneBlocks() {
        World world = new World(8013L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            int baseX = 32;
            int y = 82;
            int z = 0;
            world.setBlock(baseX - 6, y, z, BlockType.STONE, 0);
            world.setBlock(baseX, y, z, BlockType.STONE, 0);
            world.setBlock(baseX + 1, y, z, BlockType.CHEST, 0);
            world.setBlock(baseX + 2, y, z, BlockType.WATER, 0);
            world.setBlock(baseX + 3, y, z, BlockType.OBSIDIAN, 0);
            world.setBlock(baseX + 4, y, z, BlockType.END_STONE, 0);
            world.setBlock(baseX + 5, y, z, BlockType.BEDROCK, 0);
            world.setBlock(baseX + 6, y, z, BlockType.STONE, 0);

            EnderDragon dragon = eastboundDragon(baseX, z);
            world.replaceEntities(List.of(dragon));

            advanceEntityTicks(world, 1);

            assertSame(BlockType.AIR, world.getBlock(baseX, y, z));
            assertSame(BlockType.AIR, world.getBlock(baseX + 1, y, z));
            assertSame(BlockType.AIR, world.getBlock(baseX + 2, y, z));
            assertSame(BlockType.OBSIDIAN, world.getBlock(baseX + 3, y, z));
            assertSame(BlockType.END_STONE, world.getBlock(baseX + 4, y, z));
            assertSame(BlockType.BEDROCK, world.getBlock(baseX + 5, y, z));
            assertSame(BlockType.AIR, world.getBlock(baseX + 6, y, z));
            assertSame(BlockType.STONE, world.getBlock(baseX - 6, y, z),
                    "Dragon block destruction should use the source head/body part boxes, not the coarse whole-entity box");
            assertEquals(2, hugeExplosionParticles(world));
            assertFalse(world.getEntities().stream().anyMatch(DroppedItem.class::isInstance),
                    "Dragon-carved blocks should be deleted without ordinary item drops");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ender Dragon protected block contact should slow the next flight step")
    void enderDragonProtectedBlockContactSlowsNextFlightStep() {
        World clear = new World(8014L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        World blocked = new World(8015L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            int baseX = 64;
            int y = 82;
            int z = 0;
            clearDragonFlightCorridor(clear, baseX, y, z);
            clearDragonFlightCorridor(blocked, baseX, y, z);
            blocked.setBlock(baseX + 3, y, z, BlockType.OBSIDIAN, 0);

            EnderDragon clearDragon = eastboundDragon(baseX, z);
            EnderDragon blockedDragon = eastboundDragon(baseX, z);
            clear.replaceEntities(List.of(clearDragon));
            blocked.replaceEntities(List.of(blockedDragon));

            advanceEntityTicks(clear, 1);
            advanceEntityTicks(blocked, 1);

            assertEquals(clearDragon.getX(), blockedDragon.getX(), 0.0001f,
                    "Protected block contact should be detected after the current movement step");

            advanceEntityTicks(clear, 1);
            advanceEntityTicks(blocked, 1);

            assertTrue(blockedDragon.getX() < clearDragon.getX() - 0.001f,
                    "Protected End blocks should slow the dragon's next flight step");
            assertSame(BlockType.OBSIDIAN, blocked.getBlock(baseX + 3, y, z));
        } finally {
            clear.cleanup();
            blocked.cleanup();
        }
    }

    @Test
    @DisplayName("Ender Dragon flight should turn with old yaw inertia instead of snapping to the target")
    void enderDragonFlightUsesSourceStyleTurnInertia() {
        World world = new World(8019L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            EnderDragon dragon = new EnderDragon();
            dragon.setPosition(0.5f, 80.0f, 0.5f);
            dragon.setRenderBodyYaw(0.0f);
            dragon.setFlightState(64.0f, 80.0f, 0.5f, 40);
            world.replaceEntities(List.of(dragon));

            advanceEntityTicks(world, 1);

            assertTrue(dragon.getYaw() > 45.0f && dragon.getYaw() < 55.0f,
                    "The first eastbound turn should be clamped near the source 50-degree limit");
            assertTrue(dragon.getX() > 0.5f, "The dragon should begin moving east toward the target");
            assertTrue(dragon.getZ() < 0.5f,
                    "Turn-limited flight should arc forward instead of snapping directly sideways");

            double[] currentOffset = dragon.getMovementOffset(0, 1.0f);
            assertEquals(dragon.getYaw(), currentOffset[0], 0.0001);
            assertEquals(dragon.getY(), currentOffset[1], 0.0001);
            assertEquals(dragon.getPitch(), currentOffset[2], 0.0001);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ender Dragon should abandon far stale targets and retarget toward the active player")
    void enderDragonRetargetsWhenWaypointIsTooFar() {
        World world = new World(8020L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            Player player = healthyPlayer(12.5f, 82.0f, 4.5f);
            player.setWorld(world);
            world.setPlayer(player);

            EnderDragon dragon = new EnderDragon();
            dragon.setPosition(0.5f, 80.0f, 0.5f);
            dragon.setRenderBodyYaw(0.0f);
            dragon.setFlightState(220.0f, 90.0f, 0.5f, 40);
            dragon.getRandom().setSeed(4096L);
            world.replaceEntities(List.of(dragon));

            advanceEntityTicks(world, 1);

            assertEquals(player.getPosition().x, dragon.getTargetX(), 0.0001f);
            assertEquals(player.getPosition().y, dragon.getTargetY(), 0.0001f);
            assertEquals(player.getPosition().z, dragon.getTargetZ(), 0.0001f);
            assertTrue(dragon.getTargetCooldown() >= 80 && dragon.getTargetCooldown() < 160);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ender Dragon retargeting should sometimes choose the active player")
    void enderDragonRetargetsTowardActivePlayer() {
        World world = new World(8018L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            Player player = healthyPlayer(18.5f, 82.0f, -7.5f);
            player.setWorld(world);
            world.setPlayer(player);

            EnderDragon dragon = new EnderDragon();
            dragon.setPosition(0.5f, 80.0f, 0.5f);
            dragon.setFlightState(-40.0f, 90.0f, 40.0f, 0);
            dragon.getRandom().setSeed(4096L);
            world.replaceEntities(List.of(dragon));

            advanceEntityTicks(world, 1);

            assertEquals(player.getPosition().x, dragon.getTargetX(), 0.0001f);
            assertEquals(player.getPosition().y, dragon.getTargetY(), 0.0001f);
            assertEquals(player.getPosition().z, dragon.getTargetZ(), 0.0001f);
            assertTrue(dragon.getTargetCooldown() >= 80 && dragon.getTargetCooldown() < 160);
            assertTrue(dragon.getX() > 0.5f, "The first player-targeted flight step should head east");
            assertTrue(dragon.getZ() < 0.5f, "The first player-targeted flight step should head north");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ender Dragon contact damage should come from source-shaped part boxes")
    void enderDragonContactDamageUsesPartBoxes() {
        World world = new World(8016L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            EnderDragon dragon = eastboundDragon(0, 0);
            Player player = healthyPlayer(8.8f, 80.0f, 0.5f);
            world.setPlayer(player);
            world.replaceEntities(List.of(dragon));

            advanceEntityTicks(world, 10);

            assertTrue(player.getStats().getHealth() < PlayerStats.MAX_HEALTH,
                    "The dragon head box should damage players beyond the old coarse center-radius check");
            assertTrue(player.getVelocity().x > 0.0f,
                    "Contact knockback should push away from the dragon part that hit the player");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ender Dragon contact damage should not use an invisible center sphere")
    void enderDragonContactDamageIgnoresNearbyPlayersOutsideParts() {
        World world = new World(8017L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            EnderDragon dragon = eastboundDragon(0, 0);
            Player player = healthyPlayer(0.5f, 80.0f, 6.4f);
            world.setPlayer(player);
            world.replaceEntities(List.of(dragon));

            advanceEntityTicks(world, 10);

            assertEquals(PlayerStats.MAX_HEALTH, player.getStats().getHealth(), 0.001f,
                    "A player near the dragon center but outside the head/body boxes should not be hit");
            assertEquals(0.0f, player.getVelocity().lengthSquared(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ender Dragon death should create the Release 1.0 exit portal at the death location")
    void enderDragonDeathCreatesSourceShapedExitPortal() {
        World world = new World(8012L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            EnderDragon dragon = new EnderDragon();
            dragon.setPosition(10.75f, 80.0f, -3.25f);
            world.setBlock(13, 66, -4, BlockType.STONE, 0);
            world.setBlock(14, 66, -4, BlockType.STONE, 0);
            world.replaceEntities(List.of(dragon));

            assertTrue(dragon.damage(1000.0f, DamageSource.generic()));

            int cx = 10;
            int cy = 64;
            int cz = -4;
            float startingY = dragon.getY();
            advanceEntityTicks(world, 1);

            assertEquals(1, dragon.getDeathTicks());
            List<WorldSoundEvent> deathSounds = world.drainSoundEvents();
            assertEquals(1, deathSounds.size());
            assertEquals(WorldSoundEvent.ENDER_DRAGON_DEATH, deathSounds.get(0).soundId());
            assertEquals(5.0f, deathSounds.get(0).volume(), 0.0001f);

            advanceEntityTicks(world, 153);

            assertEquals(154, dragon.getDeathTicks());
            assertTrue(dragon.getY() > startingY);
            assertEquals(0, visibleExperience(world));
            assertEquals(0, hugeExplosionParticles(world));
            assertFalse(dragon.isRemoved());
            assertNotSame(BlockType.DRAGON_EGG, world.getBlock(cx, cy + 4, cz));
            assertNotSame(BlockType.END_PORTAL, world.getBlock(cx + 2, cy, cz));

            advanceEntityTicks(world, 2);

            assertEquals(156, dragon.getDeathTicks());
            assertEquals(1000, visibleExperience(world));

            advanceEntityTicks(world, EnderDragon.DEATH_SEQUENCE_TICKS - dragon.getDeathTicks() - 1);

            assertEquals(EnderDragon.DEATH_SEQUENCE_TICKS - 1, dragon.getDeathTicks());
            assertEquals(9000, visibleExperience(world));
            assertEquals(20, hugeExplosionParticles(world));
            assertFalse(dragon.isRemoved());
            assertNotSame(BlockType.DRAGON_EGG, world.getBlock(cx, cy + 4, cz));
            assertNotSame(BlockType.END_PORTAL, world.getBlock(cx + 2, cy, cz));

            advanceEntityTicks(world, 1);

            assertEquals(EnderDragon.DEATH_SEQUENCE_TICKS, dragon.getDeathTicks());
            assertTrue(dragon.isRemoved());
            assertEquals(9000, visibleExperience(world));
            assertEquals(21, hugeExplosionParticles(world));
            assertSame(BlockType.BEDROCK, world.getBlock(cx, cy, cz));
            assertSame(BlockType.END_PORTAL, world.getBlock(cx + 2, cy, cz));
            assertSame(BlockType.BEDROCK, world.getBlock(cx + 3, cy, cz));
            assertSame(BlockType.BEDROCK, world.getBlock(cx + 2, cy - 1, cz));
            assertSame(BlockType.AIR, world.getBlock(cx + 3, cy + 2, cz));
            assertSame(BlockType.STONE, world.getBlock(cx + 4, cy + 2, cz));

            assertSame(BlockType.BEDROCK, world.getBlock(cx, cy + 1, cz));
            assertSame(BlockType.BEDROCK, world.getBlock(cx, cy + 2, cz));
            assertSame(BlockType.BEDROCK, world.getBlock(cx, cy + 3, cz));
            assertSame(BlockType.DRAGON_EGG, world.getBlock(cx, cy + 4, cz));
            assertSame(BlockType.TORCH, world.getBlock(cx - 1, cy + 2, cz));
            assertEquals(BlockShape.torchMetadataFromFace(Block.FACE_WEST),
                    world.getBlockMetadata(cx - 1, cy + 2, cz));
            assertSame(BlockType.TORCH, world.getBlock(cx + 1, cy + 2, cz));
            assertEquals(BlockShape.torchMetadataFromFace(Block.FACE_EAST),
                    world.getBlockMetadata(cx + 1, cy + 2, cz));
            assertNotSame(BlockType.DRAGON_EGG, world.getBlock(0, 66, 0));

            advanceEntityTicks(world, 1);

            assertEquals(12000, visibleExperience(world));
        } finally {
            world.cleanup();
        }
    }

    private static int visibleExperience(World world) {
        return world.getEntities().stream()
                .filter(ExperienceOrbEntity.class::isInstance)
                .map(ExperienceOrbEntity.class::cast)
                .mapToInt(ExperienceOrbEntity::getValue)
                .sum();
    }

    private static long hugeExplosionParticles(World world) {
        return world.getParticles().stream()
                .filter(particle -> particle.getType() == WorldParticle.Type.HUGE_EXPLOSION)
                .count();
    }

    private static boolean contains(Chunk chunk, BlockType type) {
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (chunk.getBlock(x, y, z) == type) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int countBlocks(Chunk chunk, BlockType type) {
        int count = 0;
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (chunk.getBlock(x, y, z) == type) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static Chunk generatedEndChunk(ReleaseOneWorldGenerator generator, int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(chunkX, chunkZ);
        generator.generateChunk(null, chunk, chunkX, chunkZ);
        return chunk;
    }

    private static void assertEndBaseChunk(ReleaseOneWorldGenerator generator, EndBaseChunkExpectation expectation)
            throws Exception {
        Chunk chunk = generatedEndBaseChunk(generator, expectation.chunkX(), expectation.chunkZ());
        String label = "chunk (" + expectation.chunkX() + "," + expectation.chunkZ() + ")";
        assertEquals(expectation.endStoneCount(), countBlocks(chunk, BlockType.END_STONE),
                label + " End Stone count");
        assertEquals(expectation.minY(), minY(chunk, BlockType.END_STONE), label + " minimum End Stone y");
        assertEquals(expectation.maxY(), maxY(chunk, BlockType.END_STONE), label + " maximum End Stone y");
        if (expectation.solidX() >= 0) {
            assertSame(BlockType.END_STONE, chunk.getBlock(expectation.solidX(),
                    expectation.solidY(), expectation.solidZ()), label + " solid sample");
        }
        assertSame(BlockType.AIR, chunk.getBlock(expectation.airX(), expectation.airY(), expectation.airZ()),
                label + " air sample");
    }

    private static Chunk generatedEndBaseChunk(ReleaseOneWorldGenerator generator, int chunkX, int chunkZ)
            throws Exception {
        Chunk chunk = new Chunk(chunkX, chunkZ);
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("generateEndBaseChunk",
                Chunk.class, int.class, int.class);
        method.setAccessible(true);
        method.invoke(generator, chunk, chunkX, chunkZ);
        return chunk;
    }

    private record EndBaseChunkExpectation(int chunkX, int chunkZ, int endStoneCount, int minY, int maxY,
            int solidX, int solidY, int solidZ, int airX, int airY, int airZ) {
    }

    private static double[] invokeEndDensities(ReleaseOneWorldGenerator generator, int chunkX, int chunkZ)
            throws Exception {
        Method method = ReleaseOneWorldGenerator.class.getDeclaredMethod("endDensities",
                int.class, int.class, int.class, int.class, int.class, int.class);
        method.setAccessible(true);
        return (double[]) method.invoke(generator, chunkX * 2, 0, chunkZ * 2, 3, 33, 3);
    }

    private static void assertEndDensityVector(double[] densities, int gridX, int gridY, int gridZ,
            double expected) {
        int index = (gridX * 3 + gridZ) * 33 + gridY;
        assertEquals(expected, densities[index], 1.0E-9,
                "Unexpected End raw density at grid " + gridX + "," + gridY + "," + gridZ);
    }

    @SuppressWarnings("unchecked")
    private static List<Entity> stagedGeneratedEntities(World world) throws Exception {
        Field field = World.class.getDeclaredField("generatedEntities");
        field.setAccessible(true);
        return List.copyOf((Queue<Entity>) field.get(world));
    }

    private static int minY(Chunk chunk, BlockType type) {
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int x = 0; x < Chunk.WIDTH; x++) {
                for (int z = 0; z < Chunk.DEPTH; z++) {
                    if (chunk.getBlock(x, y, z) == type) {
                        return y;
                    }
                }
            }
        }
        return -1;
    }

    private static int maxY(Chunk chunk, BlockType type) {
        for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
            for (int x = 0; x < Chunk.WIDTH; x++) {
                for (int z = 0; z < Chunk.DEPTH; z++) {
                    if (chunk.getBlock(x, y, z) == type) {
                        return y;
                    }
                }
            }
        }
        return -1;
    }

    private static Chunk findChunkWithBlocks(World world, int chunkRadius, BlockType first, BlockType second) {
        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                Chunk chunk = world.getChunkNow(cx, cz);
                if (contains(chunk, first) && contains(chunk, second)) {
                    return chunk;
                }
            }
        }
        return null;
    }

    private static void advanceEntityTicks(World world, int ticks) {
        for (int i = 0; i < ticks; i++) {
            world.updateEntities(1.0f / 20.0f);
        }
    }

    private static EnderDragon eastboundDragon(int baseX, int z) {
        EnderDragon dragon = new EnderDragon();
        dragon.setPosition(baseX + 0.5f, 80.0f, z + 0.5f);
        dragon.setRenderBodyYaw(90.0f);
        dragon.setFlightState(baseX + 32.0f, 82.0f, z + 0.5f, 40);
        return dragon;
    }

    private static Player healthyPlayer(float x, float y, float z) {
        Player player = new Player(x, y, z);
        player.getStats().restore(PlayerStats.MAX_HEALTH, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
        return player;
    }

    private static void clearDragonFlightCorridor(World world, int baseX, int y, int z) {
        for (int x = baseX - 3; x <= baseX + 8; x++) {
            for (int by = y - 2; by <= y + 2; by++) {
                for (int bz = z - 3; bz <= z + 3; bz++) {
                    world.setBlock(x, by, bz, BlockType.AIR, 0);
                }
            }
        }
    }

    private static void buildEndPortalRing(World world, int centerX, int y, int centerZ) {
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            world.setBlock(x, y, centerZ - 2, BlockType.END_PORTAL_FRAME,
                    0 | World.END_PORTAL_FRAME_EYE_BIT);
            world.setBlock(x, y, centerZ + 2, BlockType.END_PORTAL_FRAME,
                    2 | World.END_PORTAL_FRAME_EYE_BIT);
        }
        for (int z = centerZ - 1; z <= centerZ + 1; z++) {
            world.setBlock(centerX - 2, y, z, BlockType.END_PORTAL_FRAME,
                    3 | World.END_PORTAL_FRAME_EYE_BIT);
            world.setBlock(centerX + 2, y, z, BlockType.END_PORTAL_FRAME,
                    1 | World.END_PORTAL_FRAME_EYE_BIT);
        }
    }
}
