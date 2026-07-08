package com.craftzero.world;

import com.craftzero.inventory.ItemType;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldParticleTest {
    @Test
    @DisplayName("Block destroy particles should use Release-style 4x4x4 textured fragments")
    void blockDestroyParticlesUseTexturedFragments() {
        World world = new World(11001L);
        try {
            world.spawnBlockDestroyParticles(3, 70, -2, BlockType.WHITE_WOOL, 14);

            assertEquals(64, world.getParticles().size());
            for (WorldParticle particle : world.getParticles()) {
                assertSame(WorldParticle.Type.BLOCK_CRACK, particle.getType());
                assertSame(BlockType.WHITE_WOOL, particle.getBlockParticleType());
                assertEquals(14, particle.getBlockParticleMetadata());
                assertEquals(Block.FACE_BOTTOM, particle.getBlockParticleFace());
                assertTrue(particle.getScale(0.0f) >= 0.10f);
                assertTrue(particle.getScale(0.0f) <= 0.20f);
                assertTrue(particle.getLifetimeTicks() >= 4.0f);
                assertTrue(particle.getLifetimeTicks() <= 40.0f);
                assertTrue(particle.getMotionY() > -0.12f);
                assertTrue(particle.getMotionY() < 0.35f);
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Block hit particles should preserve face texture data")
    void blockHitParticlesPreserveTextureData() {
        World world = new World(11002L);
        try {
            world.spawnBlockHitParticle(0, 70, 0, Block.FACE_EAST, BlockType.OAK_LOG, 2);

            assertEquals(1, world.getParticles().size());
            WorldParticle hit = world.getParticles().get(0);
            assertSame(WorldParticle.Type.BLOCK_CRACK, hit.getType());
            assertSame(BlockType.OAK_LOG, hit.getBlockParticleType());
            assertEquals(2, hit.getBlockParticleMetadata());
            assertEquals(Block.FACE_BOTTOM, hit.getBlockParticleFace());
            assertEquals(1.1f, hit.getRenderX(0.0f), 0.0001f);
            assertTrue(hit.getRenderY(0.0f) >= 70.1f);
            assertTrue(hit.getRenderY(0.0f) <= 70.9f);
            assertTrue(hit.getRenderZ(0.0f) >= 0.1f);
            assertTrue(hit.getRenderZ(0.0f) <= 0.9f);
            assertTrue(Math.abs(hit.getMotionX()) < 0.04f);
            assertTrue(hit.getMotionY() > 0.05f);
            assertTrue(hit.getMotionY() < 0.15f);
            assertTrue(Math.abs(hit.getMotionZ()) < 0.04f);
            assertTrue(hit.getScale(0.0f) >= 0.06f);
            assertTrue(hit.getScale(0.0f) <= 0.12f);
            assertTrue(hit.getLifetimeTicks() >= 4.0f);
            assertTrue(hit.getLifetimeTicks() <= 40.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Block hit particles should use non-full block render bounds")
    void blockHitParticlesUseNonFullRenderBounds() {
        World world = new World(11036L);
        try {
            world.spawnBlockHitParticle(2, 70, 3, Block.FACE_TOP, BlockType.SNOW_LAYER, 0);

            assertEquals(1, world.getParticles().size());
            WorldParticle hit = world.getParticles().get(0);
            assertSame(WorldParticle.Type.BLOCK_CRACK, hit.getType());
            assertSame(BlockType.SNOW_LAYER, hit.getBlockParticleType());
            assertEquals(Block.FACE_BOTTOM, hit.getBlockParticleFace());
            assertEquals(70.225f, hit.getRenderY(0.0f), 0.0001f);
            assertTrue(hit.getRenderX(0.0f) >= 2.1f);
            assertTrue(hit.getRenderX(0.0f) <= 2.9f);
            assertTrue(hit.getRenderZ(0.0f) >= 3.1f);
            assertTrue(hit.getRenderZ(0.0f) <= 3.9f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Water entry particles should emit splash and bubble bursts")
    void waterEntryParticlesEmitSplashAndBubbleBursts() {
        World world = new World(11003L);
        try {
            world.spawnEntityWaterEntryParticles(0.5f, 70.0f, 0.5f, 0.6f,
                    0.2f, -0.4f, 0.1f);

            assertEquals(26, world.getParticles().size());
            assertEquals(13, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.BUBBLE)
                    .count());
            assertEquals(13, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SPLASH)
                    .count());
            assertEquals(71.0f, world.getParticles().get(0).getRenderY(0.0f), 0.0001f);
            assertSame(WorldParticle.Type.SPLASH, world.getParticles().get(13).getType());
            assertEquals(71.0f, world.getParticles().get(13).getRenderY(0.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Bubble particles should use old water-only damping")
    void bubbleParticlesUseOldWaterOnlyDamping() {
        World waterWorld = new World(11017L);
        try {
            waterWorld.setBlock(0, 70, 0, BlockType.WATER, 0);
            WorldParticle bubble = new WorldParticle(WorldParticle.Type.BUBBLE,
                    0.5f, 70.5f, 0.5f,
                    1.0f, 0.0f, 0.0f,
                    0.055f, 16);
            waterWorld.getParticles().add(bubble);

            waterWorld.updateParticles(1.0f / 20.0f);
            assertEquals(0.55f, bubble.getRenderX(1.0f), 0.0001f);
            waterWorld.updateParticles(1.0f / 20.0f);
            assertEquals(0.5925f, bubble.getRenderX(1.0f), 0.0001f);
            assertTrue(bubble.getRenderY(1.0f) > 70.5f);
            assertEquals(1, waterWorld.getParticles().size());
        } finally {
            waterWorld.cleanup();
        }

        World airWorld = new World(11018L);
        try {
            airWorld.setBlock(0, 70, 0, BlockType.AIR, 0);
            airWorld.getParticles().add(new WorldParticle(WorldParticle.Type.BUBBLE,
                    0.5f, 70.5f, 0.5f,
                    0.0f, 0.0f, 0.0f,
                    0.055f, 16));

            airWorld.updateParticles(1.0f / 20.0f);

            assertTrue(airWorld.getParticles().isEmpty());
        } finally {
            airWorld.cleanup();
        }
    }

    @Test
    @DisplayName("Portal particles should use old source displacement curve and scale ramp")
    void portalParticlesUseOldSourceCurveAndScaleRamp() {
        WorldParticle particle = new WorldParticle(WorldParticle.Type.PORTAL,
                1.0f, 2.0f, 3.0f,
                2.0f, -0.5f, 1.0f,
                0.25f, 40, 3.0f);

        assertEquals(0.0f, particle.getScale(0.0f), 0.0001f);

        assertFalse(particle.update(1.0f / 20.0f));

        assertEquals(3.0f, particle.getRenderX(1.0f), 0.0001f);
        assertEquals(2.5f, particle.getRenderY(1.0f), 0.0001f);
        assertEquals(4.0f, particle.getRenderZ(1.0f), 0.0001f);
        assertEquals(0.01234375f, particle.getScale(0.0f), 0.0001f);

        for (int i = 0; i < 19; i++) {
            assertFalse(particle.update(1.0f / 20.0f));
        }

        assertEquals(3.0475f, particle.getRenderX(1.0f), 0.0001f);
        assertEquals(2.013125f, particle.getRenderY(1.0f), 0.0001f);
        assertEquals(4.02375f, particle.getRenderZ(1.0f), 0.0001f);
        assertEquals(0.1875f, particle.getScale(0.0f), 0.0001f);
    }

    @Test
    @DisplayName("World-spawned portal particles should get old random lifetime and texture frame")
    void worldSpawnedPortalParticlesUseOldRandomLifetimeAndFrame() {
        World world = new World(11019L);
        try {
            world.spawnParticle(WorldParticle.Type.PORTAL,
                    0.0f, 70.0f, 0.0f,
                    1.0f, 0.0f, 0.0f,
                    0.25f, 5);

            assertEquals(1, world.getParticles().size());
            WorldParticle particle = world.getParticles().get(0);
            assertTrue(particle.getLifetimeTicks() >= 40.0f && particle.getLifetimeTicks() <= 49.0f);
            assertTrue(particle.getData() >= 0.0f && particle.getData() <= 7.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Heart particles should use old upward pop, scale ramp, and lifetime")
    void heartParticlesUseOldMotionScaleAndLifetime() {
        WorldParticle heart = new WorldParticle(WorldParticle.Type.HEART,
                0.0f, 70.0f, 0.0f,
                4.0f, 0.0f, -4.0f,
                0.28f, 20);

        assertEquals(16.0f, heart.getLifetimeTicks(), 0.0001f);
        assertEquals(0.0f, heart.getScale(0.0f), 0.0001f);

        assertFalse(heart.update(1.0f / 20.0f));

        assertEquals(0.0f, heart.getRenderX(1.0f), 0.0001f);
        assertEquals(70.1f, heart.getRenderY(1.0f), 0.0001f);
        assertEquals(0.0f, heart.getRenderZ(1.0f), 0.0001f);
        assertEquals(0.28f, heart.getScale(0.0f), 0.0001f);
    }

    @Test
    @DisplayName("Note particles should use old short upward pop and scale ramp")
    void noteParticlesUseOldMotionScaleAndLifetime() {
        WorldParticle note = new WorldParticle(WorldParticle.Type.NOTE,
                0.5f, 71.2f, 0.5f,
                3.0f, 8.0f, 3.0f,
                0.30f, 40, 12.0f / 24.0f);

        assertEquals(6.0f, note.getLifetimeTicks(), 0.0001f);
        assertEquals(0.0f, note.getScale(0.0f), 0.0001f);

        assertFalse(note.update(1.0f / 20.0f));
        assertEquals(0.5f, note.getRenderX(1.0f), 0.0001f);
        assertEquals(71.4f, note.getRenderY(1.0f), 0.0001f);
        assertEquals(0.5f, note.getRenderZ(1.0f), 0.0001f);
        assertEquals(0.30f, note.getScale(0.0f), 0.0001f);

        assertFalse(note.update(1.0f / 20.0f));
        assertEquals(71.532f, note.getRenderY(1.0f), 0.0001f);
    }

    @Test
    @DisplayName("Ambient liquid particles should drip from blocks below water and lava")
    void ambientLiquidParticlesDripFromBlocksBelowFluids() {
        World world = new World(11004L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            world.setBlock(0, 71, 0, BlockType.WATER, 0);
            world.setBlock(0, 69, 0, BlockType.AIR, 0);
            world.setBlock(2, 70, 0, BlockType.STONE, 0);
            world.setBlock(2, 71, 0, BlockType.LAVA, 0);
            world.setBlock(2, 69, 0, BlockType.AIR, 0);

            world.tickAmbientBlockParticleAt(0, 70, 0, new Random(1L));
            world.tickAmbientBlockParticleAt(2, 70, 0, new Random(2L));

            assertSame(WorldParticle.Type.DRIP_WATER, world.getParticles().get(0).getType());
            assertSame(WorldParticle.Type.DRIP_LAVA, world.getParticles().get(1).getType());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ambient block particles should include Release-era lava particles and powered redstone dust")
    void ambientBlockParticlesIncludeLavaAndRedstoneDust() {
        World world = new World(11005L);
        try {
            world.setBlock(0, 70, 0, BlockType.LAVA, 0);
            world.setBlock(0, 71, 0, BlockType.AIR, 0);
            world.setBlock(2, 70, 0, BlockType.REDSTONE_WIRE, 12);

            world.tickAmbientBlockParticleAt(0, 70, 0, alwaysZeroRandom());
            world.tickAmbientBlockParticleAt(2, 70, 0, fixedFloatRandom(0.5f));

            assertSame(WorldParticle.Type.LAVA, world.getParticles().get(0).getType());
            WorldParticle redstone = world.getParticles().get(1);
            assertSame(WorldParticle.Type.RED_DUST, redstone.getType());
            assertEquals(2.5f, redstone.getRenderX(0.0f), 0.0001f);
            assertEquals(70.0625f, redstone.getRenderY(0.0f), 0.0001f);
            assertEquals(0.5f, redstone.getRenderZ(0.0f), 0.0001f);
            assertEquals(12.0f, redstone.getData(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ambient glowing redstone ore should emit old exposed-face sparkles")
    void ambientGlowingRedstoneOreEmitsExposedFaceSparkles() {
        World world = new World(11023L);
        try {
            world.setBlock(0, 70, 0, BlockType.GLOWING_REDSTONE_ORE, 0);

            assertTrue(world.tickAmbientBlockParticleAt(0, 70, 0, fixedFloatRandom(0.5f)));

            assertEquals(6, world.getParticles().size());
            for (WorldParticle sparkle : world.getParticles()) {
                assertSame(WorldParticle.Type.RED_DUST, sparkle.getType());
                assertEquals(WorldParticle.RED_DUST_DEFAULT_COLOR_DATA, sparkle.getData(), 0.0001f);
                assertTrue(sparkle.getRenderX(0.0f) < 0.0f || sparkle.getRenderX(0.0f) > 1.0f
                        || sparkle.getRenderY(0.0f) < 70.0f || sparkle.getRenderY(0.0f) > 71.0f
                        || sparkle.getRenderZ(0.0f) < 0.0f || sparkle.getRenderZ(0.0f) > 1.0f);
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ambient water blocks should emit suspended underwater particles")
    void ambientWaterBlocksEmitSuspendedParticles() {
        World world = new World(11010L);
        try {
            world.setBlock(0, 70, 0, BlockType.WATER, 0);

            assertTrue(world.tickAmbientBlockParticleAt(0, 70, 0, fixedWaterSuspendedRandom(0.5f)));

            assertEquals(1, world.getParticles().size());
            WorldParticle particle = world.getParticles().get(0);
            assertSame(WorldParticle.Type.SUSPENDED, particle.getType());
            assertTrue(particle.getLifetimeTicks() >= 16.0f && particle.getLifetimeTicks() <= 80.0f);
            assertEquals(0.5f, particle.getRenderX(0.0f), 0.0001f);
            assertEquals(70.5f, particle.getRenderY(0.0f), 0.0001f);
            assertEquals(0.5f, particle.getRenderZ(0.0f), 0.0001f);
            float x = particle.getRenderX(0.0f);
            float y = particle.getRenderY(0.0f);
            float z = particle.getRenderZ(0.0f);

            world.updateParticles(1.0f / 20.0f);

            assertEquals(x, particle.getRenderX(1.0f), 0.0001f);
            assertEquals(y, particle.getRenderY(1.0f), 0.0001f);
            assertEquals(z, particle.getRenderZ(1.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Flowing water should emit suspended particles only for Release falling metadata")
    void flowingWaterSuspendedParticlesFollowReleaseMetadataRules() {
        World world = new World(11024L);
        try {
            world.setBlock(0, 70, 0, BlockType.FLOWING_WATER, 3);
            world.setBlock(2, 70, 0, BlockType.FLOWING_WATER, 8);

            assertFalse(world.tickAmbientBlockParticleAt(0, 70, 0, fixedFloatRandom(0.5f)));
            assertTrue(world.tickAmbientBlockParticleAt(2, 70, 0, fixedWaterSuspendedRandom(0.5f)));

            assertEquals(1, world.getParticles().size());
            WorldParticle particle = world.getParticles().get(0);
            assertSame(WorldParticle.Type.SUSPENDED, particle.getType());
            assertEquals(2.5f, particle.getRenderX(0.0f), 0.0001f);
            assertEquals(70.5f, particle.getRenderY(0.0f), 0.0001f);
            assertEquals(0.5f, particle.getRenderZ(0.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Suspended water particles should die outside water")
    void suspendedWaterParticlesDieOutsideWater() {
        World world = new World(11020L);
        try {
            world.setBlock(0, 70, 0, BlockType.AIR, 0);
            world.getParticles().add(new WorldParticle(WorldParticle.Type.SUSPENDED,
                    0.5f, 70.5f, 0.5f,
                    1.0f, 1.0f, 1.0f,
                    0.04f, 40));

            world.updateParticles(1.0f / 20.0f);

            assertTrue(world.getParticles().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ambient mycelium should emit old town-aura spores")
    void ambientMyceliumEmitsTownAuraParticles() {
        World world = new World(11014L);
        try {
            world.setBlock(0, 70, 0, BlockType.MYCELIUM, 0);

            assertTrue(world.tickAmbientBlockParticleAt(0, 70, 0, alwaysZeroRandom()));

            assertEquals(1, world.getParticles().size());
            WorldParticle particle = world.getParticles().get(0);
            assertSame(WorldParticle.Type.TOWN_AURA, particle.getType());
            assertTrue(particle.getLifetimeTicks() >= 20.0f && particle.getLifetimeTicks() <= 100.0f);
            assertEquals(particle.getScale(0.0f), particle.getScale(10.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ambient fire should emit old large-smoke display particles")
    void ambientFireEmitsLargeSmokeParticles() {
        World world = new World(11015L);
        try {
            world.setBlock(0, 70, 0, BlockType.FIRE, 0);
            world.setBlock(0, 69, 0, BlockType.STONE, 0);

            assertTrue(world.tickAmbientBlockParticleAt(0, 70, 0, fixedFloatRandom(0.5f)));

            assertEquals(3, world.getParticles().size());
            for (WorldParticle particle : world.getParticles()) {
                assertSame(WorldParticle.Type.LARGE_SMOKE, particle.getType());
                assertEquals(0.5f, particle.getRenderX(0.0f), 0.0001f);
                assertEquals(70.75f, particle.getRenderY(0.0f), 0.0001f);
                assertEquals(0.5f, particle.getRenderZ(0.0f), 0.0001f);
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Side-supported fire should emit old edge large-smoke particles")
    void sideSupportedFireEmitsEdgeLargeSmokeParticles() {
        World world = new World(11023L);
        try {
            for (int x = -1; x <= 1; x++) {
                for (int y = 69; y <= 71; y++) {
                    for (int z = -1; z <= 1; z++) {
                        world.setBlock(x, y, z, BlockType.AIR, 0);
                    }
                }
            }
            world.setBlock(0, 70, 0, BlockType.FIRE, 0);
            world.setBlock(-1, 70, 0, BlockType.OAK_PLANKS, 0);

            assertTrue(world.tickAmbientBlockParticleAt(0, 70, 0, fixedFloatRandom(0.5f)));

            assertEquals(2, world.getParticles().size());
            for (WorldParticle particle : world.getParticles()) {
                assertSame(WorldParticle.Type.LARGE_SMOKE, particle.getType());
                assertEquals(0.05f, particle.getRenderX(0.0f), 0.0001f);
                assertEquals(70.5f, particle.getRenderY(0.0f), 0.0001f);
                assertEquals(0.5f, particle.getRenderZ(0.0f), 0.0001f);
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ambient torches should emit old smoke and flame display particles")
    void ambientTorchesEmitSmokeAndFlameParticles() {
        World world = new World(11016L);
        try {
            world.setBlock(10, 70, 5, BlockType.TORCH, 1);

            assertTrue(world.tickAmbientBlockParticleAt(10, 70, 5, fixedFloatRandom(0.5f)));

            assertEquals(2, world.getParticles().size());
            WorldParticle smoke = world.getParticles().get(0);
            WorldParticle flame = world.getParticles().get(1);
            assertSame(WorldParticle.Type.SMOKE, smoke.getType());
            assertSame(WorldParticle.Type.FLAME, flame.getType());
            assertEquals(10.5f - 0.2700000107f, smoke.getRenderX(0.0f), 0.0001f);
            assertEquals(70.7f + 0.22f, smoke.getRenderY(0.0f), 0.0001f);
            assertEquals(5.5f, smoke.getRenderZ(0.0f), 0.0001f);
            assertEquals(smoke.getRenderX(0.0f), flame.getRenderX(0.0f), 0.0001f);
            assertEquals(smoke.getRenderY(0.0f), flame.getRenderY(0.0f), 0.0001f);
            assertEquals(smoke.getRenderZ(0.0f), flame.getRenderZ(0.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ambient redstone torches should emit old active reddust only")
    void ambientRedstoneTorchesEmitActiveRedDustOnly() {
        World world = new World(11017L);
        try {
            world.setBlock(0, 70, 0, BlockType.REDSTONE_TORCH_ON, 5);
            world.setBlock(2, 70, 0, BlockType.REDSTONE_TORCH_OFF, 5);

            assertTrue(world.tickAmbientBlockParticleAt(0, 70, 0, fixedFloatRandom(0.5f)));
            assertFalse(world.tickAmbientBlockParticleAt(2, 70, 0, fixedFloatRandom(0.5f)));

            assertEquals(1, world.getParticles().size());
            WorldParticle dust = world.getParticles().get(0);
            assertSame(WorldParticle.Type.RED_DUST, dust.getType());
            assertEquals(0.5f, dust.getRenderX(0.0f), 0.0001f);
            assertEquals(70.7f, dust.getRenderY(0.0f), 0.0001f);
            assertEquals(0.5f, dust.getRenderZ(0.0f), 0.0001f);
            assertEquals(WorldParticle.RED_DUST_DEFAULT_COLOR_DATA, dust.getData(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Powered repeaters should emit old red-dust display particles")
    void poweredRepeatersEmitOldRedDustDisplayParticles() {
        World world = new World(11021L);
        try {
            world.setBlock(0, 70, 0, BlockType.REDSTONE_REPEATER_ON, 0);
            world.setBlock(2, 70, 0, BlockType.REDSTONE_REPEATER_OFF, 0);

            assertTrue(world.tickAmbientBlockParticleAt(0, 70, 0, fixedFloatRandom(0.5f)));
            assertFalse(world.tickAmbientBlockParticleAt(2, 70, 0, fixedFloatRandom(0.5f)));

            assertEquals(1, world.getParticles().size());
            WorldParticle dust = world.getParticles().get(0);
            assertSame(WorldParticle.Type.RED_DUST, dust.getType());
            assertEquals(0.5f, dust.getRenderX(0.0f), 0.0001f);
            assertEquals(70.4f, dust.getRenderY(0.0f), 0.0001f);
            assertEquals(0.5f - 0.3125f, dust.getRenderZ(0.0f), 0.0001f);
            assertEquals(WorldParticle.RED_DUST_DEFAULT_COLOR_DATA, dust.getData(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Powered repeater display particles should follow the delay torch offset")
    void poweredRepeaterDisplayParticlesFollowDelayTorchOffset() {
        World world = new World(11022L);
        try {
            int metadata = 3 | (3 << RedstoneEngine.REPEATER_DELAY_SHIFT);
            world.setBlock(8, 70, 5, BlockType.REDSTONE_REPEATER_ON, metadata);

            assertTrue(world.tickAmbientBlockParticleAt(8, 70, 5, fixedRepeaterDisplayRandom(0.5f, 1)));

            assertEquals(1, world.getParticles().size());
            WorldParticle dust = world.getParticles().get(0);
            assertSame(WorldParticle.Type.RED_DUST, dust.getType());
            assertEquals(8.5f + 0.3125f, dust.getRenderX(0.0f), 0.0001f);
            assertEquals(70.4f, dust.getRenderY(0.0f), 0.0001f);
            assertEquals(5.5f, dust.getRenderZ(0.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Brewing stands and End portals should emit old ambient smoke")
    void brewingStandsAndEndPortalsEmitAmbientSmokeParticles() {
        World world = new World(11018L);
        try {
            world.setBlock(0, 70, 0, BlockType.BREWING_STAND, 0);
            world.setBlock(2, 70, 0, BlockType.END_PORTAL, 0);

            assertTrue(world.tickAmbientBlockParticleAt(0, 70, 0, fixedFloatRandom(0.5f)));
            assertTrue(world.tickAmbientBlockParticleAt(2, 70, 0, fixedFloatRandom(0.5f)));

            assertEquals(2, world.getParticles().size());
            WorldParticle brewing = world.getParticles().get(0);
            WorldParticle portal = world.getParticles().get(1);
            assertSame(WorldParticle.Type.SMOKE, brewing.getType());
            assertEquals(0.5f, brewing.getRenderX(0.0f), 0.0001f);
            assertEquals(70.85f, brewing.getRenderY(0.0f), 0.0001f);
            assertEquals(0.5f, brewing.getRenderZ(0.0f), 0.0001f);
            assertSame(WorldParticle.Type.SMOKE, portal.getType());
            assertEquals(2.5f, portal.getRenderX(0.0f), 0.0001f);
            assertEquals(70.8f, portal.getRenderY(0.0f), 0.0001f);
            assertEquals(0.5f, portal.getRenderZ(0.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Low Overworld air should emit old depth-suspended void particles")
    void lowOverworldAirEmitsDepthSuspendedParticles() {
        World world = new World(11012L);
        try {
            world.setBlock(0, 10, 0, BlockType.AIR, 0);

            assertTrue(world.tickDepthSuspendAmbientAt(0, 10, 0, alwaysZeroRandom()));

            assertEquals(1, world.getParticles().size());
            WorldParticle particle = world.getParticles().get(0);
            assertSame(WorldParticle.Type.DEPTH_SUSPEND, particle.getType());
            assertTrue(particle.getLifetimeTicks() >= 20.0f && particle.getLifetimeTicks() <= 100.0f);
            assertEquals(particle.getScale(0.0f), particle.getScale(10.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Depth-suspended void particles should stay Overworld-only")
    void depthSuspendedParticlesStayOverworldOnly() {
        World world = new World(11013L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            world.setBlock(0, 10, 0, BlockType.AIR, 0);

            assertFalse(world.tickDepthSuspendAmbientAt(0, 10, 0, alwaysZeroRandom()));
            assertTrue(world.getParticles().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Item break particles should preserve item texture data")
    void itemBreakParticlesPreserveItemTextureData() {
        World world = new World(11006L);
        try {
            world.spawnItemBreakParticles(ItemType.SNOWBALL, 1.0f, 70.0f, 2.0f);

            assertEquals(8, world.getParticles().size());
            for (WorldParticle particle : world.getParticles()) {
                assertSame(WorldParticle.Type.ITEM_CRACK, particle.getType());
                assertSame(ItemType.SNOWBALL, particle.getItemParticleType());
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Fragment particles should use old falling shard motion")
    void fragmentParticlesUseOldFallingShardMotion() {
        WorldParticle poof = new WorldParticle(WorldParticle.Type.SNOWBALL_POOF,
                0.0f, 70.0f, 0.0f,
                0.02f, 0.0f, -0.02f,
                0.12f, 12);
        WorldParticle itemCrack = new WorldParticle(WorldParticle.Type.ITEM_CRACK,
                0.0f, 70.0f, 0.0f,
                0.02f, 0.0f, -0.02f,
                0.12f, 12,
                WorldParticle.itemParticleData(ItemType.EGG));
        WorldParticle blockCrack = new WorldParticle(WorldParticle.Type.BLOCK_CRACK,
                0.0f, 70.0f, 0.0f,
                0.02f, 0.0f, -0.02f,
                0.12f, 12,
                WorldParticle.blockParticleData(BlockType.STONE, 0, Block.FACE_TOP));
        WorldParticle blockDust = new WorldParticle(WorldParticle.Type.BLOCK_DUST,
                0.0f, 70.0f, 0.0f,
                0.02f, 0.0f, -0.02f,
                0.12f, 12,
                WorldParticle.blockParticleData(BlockType.GRASS, 0, Block.FACE_TOP));
        WorldParticle slime = new WorldParticle(WorldParticle.Type.SLIME,
                0.0f, 70.0f, 0.0f,
                0.02f, 0.0f, -0.02f,
                0.12f, 12);
        float scale = poof.getScale(0.0f);
        float itemScale = itemCrack.getScale(0.0f);
        float blockCrackScale = blockCrack.getScale(0.0f);
        float blockDustScale = blockDust.getScale(0.0f);
        float slimeScale = slime.getScale(0.0f);

        poof.update(1.0f / 20.0f);
        itemCrack.update(1.0f / 20.0f);
        blockCrack.update(1.0f / 20.0f);
        blockDust.update(1.0f / 20.0f);
        slime.update(1.0f / 20.0f);

        assertEquals(0.02f, poof.getRenderX(1.0f), 0.0001f);
        assertEquals(69.96f, poof.getRenderY(1.0f), 0.0001f);
        assertEquals(-0.02f, poof.getRenderZ(1.0f), 0.0001f);
        assertEquals(scale, poof.getScale(0.0f), 0.0001f);
        assertEquals(poof.getRenderX(1.0f), itemCrack.getRenderX(1.0f), 0.0001f);
        assertEquals(poof.getRenderY(1.0f), itemCrack.getRenderY(1.0f), 0.0001f);
        assertEquals(poof.getRenderZ(1.0f), itemCrack.getRenderZ(1.0f), 0.0001f);
        assertEquals(itemScale, itemCrack.getScale(0.0f), 0.0001f);
        assertFragmentMatchesPoof(poof, blockCrack, blockCrackScale);
        assertFragmentMatchesPoof(poof, blockDust, blockDustScale);
        assertFragmentMatchesPoof(poof, slime, slimeScale);
    }

    @Test
    @DisplayName("Item pickup particles should preserve the item texture and old squared pull")
    void itemPickupParticlesPreserveCollectedItemTextureAndSquaredPull() {
        World world = new World(11007L);
        try {
            world.spawnItemPickupParticle(ItemType.DIAMOND, 0.0f, 70.0f, 0.0f,
                    0.0f, 71.0f, 0.0f);

            assertEquals(1, world.getParticles().size());
            WorldParticle particle = world.getParticles().get(0);
            assertSame(WorldParticle.Type.ITEM_PICKUP, particle.getType());
            assertSame(ItemType.DIAMOND, particle.getItemParticleType());
            assertEquals(3.0f, particle.getLifetimeTicks(), 0.0001f);
            assertEquals(70.0f, particle.getRenderY(0.0f), 0.0001f);

            assertFalse(particle.update(1.0f / 20.0f));
            assertEquals(70.0f + 1.0f / 9.0f, particle.getRenderY(0.0f), 0.0001f);
            assertFalse(particle.update(1.0f / 20.0f));
            assertEquals(70.0f + 4.0f / 9.0f, particle.getRenderY(0.0f), 0.0001f);
            assertTrue(particle.update(1.0f / 20.0f));
            assertEquals(71.0f, particle.getRenderY(0.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Slime landing particles should use the old slime particle density")
    void slimeLandingParticlesUseSizeScaledSlimeballShards() {
        World world = new World(11008L);
        try {
            world.spawnSlimeLandingParticles(1.0f, 70.0f, 2.0f, 2.4f, 4);

            assertEquals(32, world.getParticles().size());
            for (WorldParticle particle : world.getParticles()) {
                assertSame(WorldParticle.Type.SLIME, particle.getType());
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Physical particles should stop against loaded block collision")
    void physicalParticlesStopAgainstLoadedBlockCollision() {
        World world = new World(11011L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.getParticles().add(new WorldParticle(WorldParticle.Type.ITEM_CRACK,
                    0.5f, 70.05f, 0.5f,
                    0.0f, -4.0f, 0.0f,
                    0.10f, 40,
                    WorldParticle.itemParticleData(ItemType.SLIMEBALL)));

            world.updateParticles(1.0f / 20.0f);

            WorldParticle particle = world.getParticles().get(0);
            assertTrue(particle.getRenderY(1.0f) >= 70.0f);
            assertTrue(particle.getRenderY(1.0f) < 70.02f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Footstep particles should shrink over their long lifetime")
    void footstepParticlesShrinkOverLifetime() {
        WorldParticle particle = new WorldParticle(WorldParticle.Type.FOOTSTEP,
                0.0f, 70.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.25f, 200);

        float initialScale = particle.getScale(0.0f);
        particle.update(5.0f);
        float midScale = particle.getScale(0.0f);
        particle.update(5.0f);
        float finalScale = particle.getScale(0.0f);

        assertTrue(midScale < initialScale);
        assertTrue(finalScale < midScale);
    }

    @Test
    @DisplayName("Red dust particles should grow quickly instead of fading like smoke")
    void redDustParticlesUseOldFastScaleRamp() {
        WorldParticle particle = new WorldParticle(WorldParticle.Type.RED_DUST,
                0.0f, 70.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.08f, 32, 15.0f);

        assertEquals(0.0f, particle.getScale(0.0f), 0.0001f);
        particle.update(1.0f / 20.0f);

        assertEquals(0.08f, particle.getScale(0.0f), 0.0001f);
        particle.update(15.0f / 20.0f);
        assertEquals(0.08f, particle.getScale(0.0f), 0.0001f);
    }

    @Test
    @DisplayName("Crit particles should use old fast scale ramp, drag, and gravity")
    void critParticlesUseOldMotionAndScale() {
        WorldParticle particle = new WorldParticle(WorldParticle.Type.CRIT,
                0.0f, 70.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                0.16f, 8, 0.8f);

        assertEquals(0.0f, particle.getScale(0.0f), 0.0001f);
        particle.update(1.0f / 20.0f);

        assertEquals(0.16f, particle.getScale(0.0f), 0.0001f);
        assertEquals(0.02f, particle.getRenderX(1.0f), 0.0001f);
        assertEquals(70.0f, particle.getRenderY(1.0f), 0.0001f);

        particle.update(1.0f / 20.0f);

        assertEquals(0.034f, particle.getRenderX(1.0f), 0.0001f);
        assertTrue(particle.getRenderY(1.0f) < 70.0f);
    }

    @Test
    @DisplayName("Lava particles should pop up then fall back under gravity")
    void lavaParticlesArcBackDownAfterInitialPop() {
        WorldParticle particle = new WorldParticle(WorldParticle.Type.LAVA,
                0.0f, 70.0f, 0.0f,
                0.0f, 0.18f, 0.0f,
                0.12f, 20);

        particle.update(1.0f / 20.0f);
        float firstY = particle.getRenderY(1.0f);
        for (int i = 0; i < 10; i++) {
            particle.update(1.0f / 20.0f);
        }
        float laterY = particle.getRenderY(1.0f);

        assertTrue(firstY > 70.0f);
        assertTrue(laterY < firstY);
    }

    @Test
    @DisplayName("Lava particles should shed old smoke while alive")
    void lavaParticlesShedSmokeWhileAlive() {
        World world = new World(11026L);
        try {
            world.getParticles().add(new WorldParticle(WorldParticle.Type.LAVA,
                    0.25f, 70.0f, 0.75f,
                    0.02f, 0.18f, -0.03f,
                    0.12f, 10000));

            world.updateParticles(1.0f / 20.0f);

            assertEquals(2, world.getParticles().size());
            WorldParticle smoke = world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SMOKE)
                    .findFirst()
                    .orElseThrow();
            assertEquals(0.25f, smoke.getRenderX(0.0f), 0.0001f);
            assertEquals(70.0f, smoke.getRenderY(0.0f), 0.0001f);
            assertEquals(0.75f, smoke.getRenderZ(0.0f), 0.0001f);
            assertEquals(0.02f, smoke.getMotionX(), 0.0001f);
            assertEquals(0.18f, smoke.getMotionY(), 0.0001f);
            assertEquals(-0.03f, smoke.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Expired lava particles should not leave phantom smoke")
    void expiredLavaParticlesDoNotLeavePhantomSmoke() {
        World world = new World(11027L);
        try {
            world.getParticles().add(new WorldParticle(WorldParticle.Type.LAVA,
                    0.25f, 70.0f, 0.75f,
                    0.02f, 0.18f, -0.03f,
                    0.12f, 1));

            world.updateParticles(1.0f / 20.0f);

            assertTrue(world.getParticles().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Drip particles should hang before falling and water should splash on impact")
    void dripParticlesHangThenWaterSplashesOnImpact() {
        WorldParticle drip = new WorldParticle(WorldParticle.Type.DRIP_WATER,
                0.0f, 70.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.045f, 80);
        assertTrue(drip.isDripBobPhase(0.0f));
        drip.update(40.0f / 20.0f);
        assertTrue(drip.isDripBobPhase(0.0f));
        drip.update(1.0f / 20.0f);
        assertFalse(drip.isDripBobPhase(0.0f));

        World world = new World(11016L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.getParticles().add(new WorldParticle(WorldParticle.Type.DRIP_WATER,
                    0.5f, 70.0f, 0.5f,
                    0.0f, 0.0f, 0.0f,
                    0.045f, 80));

            for (int i = 0; i < 60 && world.getParticles().stream()
                    .noneMatch(particle -> particle.getType() == WorldParticle.Type.SPLASH); i++) {
                world.updateParticles(1.0f / 20.0f);
            }

            assertTrue(world.getParticles().stream()
                    .anyMatch(particle -> particle.getType() == WorldParticle.Type.SPLASH));
            assertTrue(world.getParticles().stream()
                    .noneMatch(particle -> particle.getType() == WorldParticle.Type.DRIP_WATER));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Splash spell particles should drift upward under old spell acceleration")
    void spellParticlesDriftUpwardUnderOldAcceleration() {
        WorldParticle spell = new WorldParticle(WorldParticle.Type.SPELL,
                0.0f, 70.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.13f, 24, 0x336699);

        spell.update(1.0f / 20.0f);
        float firstY = spell.getRenderY(1.0f);
        spell.update(1.0f / 20.0f);
        float secondY = spell.getRenderY(1.0f);

        assertEquals(70.004f, firstY, 0.0001f);
        assertEquals(70.01184f, secondY, 0.0001f);
    }

    @Test
    @DisplayName("Splash-potion spell particles should use the old radial cloud shape")
    void splashPotionSpellParticlesUseOldRadialCloudShape() {
        World world = new World(11028L);
        try {
            world.spawnSplashPotionParticles(1.0f, 70.0f, 2.0f,
                    new PotionData(PotionType.POISON, true, false, false));

            int itemCrackCount = 0;
            float maxShardHorizontalMotion = 0.0f;
            int spellCount = 0;
            float maxHorizontalMotion = 0.0f;
            for (WorldParticle particle : world.getParticles()) {
                if (particle.getType() == WorldParticle.Type.ITEM_CRACK) {
                    itemCrackCount++;
                    assertSame(ItemType.POTION, particle.getItemParticleType());
                    assertEquals(1.0f, particle.getRenderX(0.0f), 0.0001f);
                    assertEquals(70.0f, particle.getRenderY(0.0f), 0.0001f);
                    assertEquals(2.0f, particle.getRenderZ(0.0f), 0.0001f);
                    assertTrue(particle.getMotionY() >= 0.0f);
                    assertTrue(particle.getMotionY() <= 0.2f);
                    maxShardHorizontalMotion = Math.max(maxShardHorizontalMotion,
                            (float) Math.sqrt(particle.getMotionX() * particle.getMotionX()
                                    + particle.getMotionZ() * particle.getMotionZ()));
                    continue;
                }
                if (particle.getType() != WorldParticle.Type.SPELL) {
                    continue;
                }
                spellCount++;
                float dx = particle.getRenderX(0.0f) - 1.0f;
                float dz = particle.getRenderZ(0.0f) - 2.0f;
                float spawnRadius = (float) Math.sqrt(dx * dx + dz * dz);
                float horizontalMotion = (float) Math.sqrt(
                        particle.getMotionX() * particle.getMotionX()
                                + particle.getMotionZ() * particle.getMotionZ());
                assertEquals(70.3f, particle.getRenderY(0.0f), 0.0001f);
                assertTrue(spawnRadius <= 0.4001f);
                assertEquals(dx * 4.0f, particle.getMotionX(), 0.0001f);
                assertEquals(dz * 4.0f, particle.getMotionZ(), 0.0001f);
                maxHorizontalMotion = Math.max(maxHorizontalMotion, horizontalMotion);
            }

            assertEquals(8, itemCrackCount);
            assertTrue(maxShardHorizontalMotion > 0.05f);
            assertEquals(100, spellCount);
            assertTrue(maxHorizontalMotion > 0.6f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mob spell particles should use old aura drift, scale, and lifetime")
    void mobSpellParticlesUseOldAuraDriftScaleAndLifetime() {
        WorldParticle mobAura = new WorldParticle(WorldParticle.Type.MOB_SPELL,
                0.0f, 70.0f, 0.0f,
                1.0f, 0.08f, -1.0f,
                0.13f, 24, 0x336699);
        float scale = mobAura.getScale(0.0f);

        mobAura.update(1.0f / 20.0f);
        assertEquals(0.02f, mobAura.getRenderX(1.0f), 0.0001f);
        assertEquals(70.0016f, mobAura.getRenderY(1.0f), 0.0001f);
        assertEquals(-0.02f, mobAura.getRenderZ(1.0f), 0.0001f);
        assertEquals(scale, mobAura.getScale(0.0f), 0.0001f);

        mobAura.update(1.0f / 20.0f);
        assertEquals(0.0398f, mobAura.getRenderX(1.0f), 0.0001f);

        World world = new World(11032L);
        try {
            world.spawnParticle(WorldParticle.Type.MOB_SPELL,
                    0.0f, 70.0f, 0.0f,
                    0.0f, 0.02f, 0.0f,
                    0.10f, 24, 0x336699);

            WorldParticle emitted = world.getParticles().get(0);
            assertEquals(0x336699, (int) emitted.getData());
            assertTrue(emitted.getLifetimeTicks() >= 20.0f);
            assertTrue(emitted.getLifetimeTicks() <= 100.0f);
            assertTrue(emitted.getScale(0.0f) >= 0.05f);
            assertTrue(emitted.getScale(0.0f) <= 0.11f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Smoke-family particles should use source-style scale ramp and vertical motion")
    void smokeFamilyParticlesUseOldScaleAndVerticalMotion() {
        WorldParticle smoke = new WorldParticle(WorldParticle.Type.SMOKE,
                0.0f, 70.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.22f, 32);
        WorldParticle explode = new WorldParticle(WorldParticle.Type.EXPLODE,
                0.0f, 70.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.22f, 32);
        WorldParticle snowShovel = new WorldParticle(WorldParticle.Type.SNOW_SHOVEL,
                0.0f, 70.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                0.13f, 32);

        assertEquals(0.0f, smoke.getScale(0.0f), 0.0001f);
        smoke.update(1.0f / 20.0f);
        explode.update(1.0f / 20.0f);
        snowShovel.update(1.0f / 20.0f);

        assertEquals(0.22f, smoke.getScale(0.0f), 0.0001f);
        assertEquals(70.004f, smoke.getRenderY(1.0f), 0.0001f);
        assertEquals(70.004f, explode.getRenderY(1.0f), 0.0001f);
        assertEquals(69.97f, snowShovel.getRenderY(1.0f), 0.0001f);
    }

    @Test
    @DisplayName("Splash particles should fall under old rain-particle gravity")
    void splashParticlesFallUnderOldRainGravity() {
        WorldParticle splash = new WorldParticle(WorldParticle.Type.SPLASH,
                0.0f, 70.0f, 0.0f,
                0.08f, 0.0f, 0.0f,
                0.18f, 16);

        splash.update(1.0f / 20.0f);

        assertEquals(69.96f, splash.getRenderY(1.0f), 0.0001f);
        assertEquals(0.08f, splash.getRenderX(1.0f), 0.0001f);
    }

    @Test
    @DisplayName("Rain particles should use old raindrop gravity and expire on ground")
    void rainParticlesUseOldDropGravityAndExpireOnGround() {
        WorldParticle rain = new WorldParticle(WorldParticle.Type.RAIN,
                0.0f, 70.0f, 0.0f,
                0.02f, 0.10f, -0.02f,
                0.16f, 20);

        assertFalse(rain.update(1.0f / 20.0f));
        assertEquals(0.02f, rain.getRenderX(1.0f), 0.0001f);
        assertEquals(70.04f, rain.getRenderY(1.0f), 0.0001f);
        assertEquals(-0.02f, rain.getRenderZ(1.0f), 0.0001f);

        World world = new World(11033L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.getParticles().add(new WorldParticle(WorldParticle.Type.RAIN,
                    0.5f, 70.02f, 0.5f,
                    0.0f, -0.02f, 0.0f,
                    0.16f, 20));

            world.updateParticles(1.0f / 20.0f);

            assertTrue(world.getParticles().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Flame particles should use the old quadratic scale shrink and drag")
    void flameParticlesUseOldScaleAndDrag() {
        WorldParticle flame = new WorldParticle(WorldParticle.Type.FLAME,
                0.0f, 70.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                0.20f, 20);

        assertEquals(0.20f, flame.getScale(0.0f), 0.0001f);
        flame.update(1.0f / 20.0f);
        assertEquals(1.0f, flame.getRenderX(1.0f), 0.0001f);
        flame.update(1.0f / 20.0f);
        assertEquals(1.96f, flame.getRenderX(1.0f), 0.0001f);
        flame.update(8.0f / 20.0f);

        assertEquals(0.175f, flame.getScale(0.0f), 0.0001f);
    }

    @Test
    @DisplayName("Enchantment-table particles should carry a glyph atlas index")
    void enchantmentTableParticlesCarryGlyphAtlasIndex() {
        World world = new World(11009L);
        try {
            world.spawnEnchantmentTableParticle(0, 70, 0, 2, 71, 0);

            assertEquals(1, world.getParticles().size());
            WorldParticle particle = world.getParticles().get(0);
            assertSame(WorldParticle.Type.ENCHANTMENT_TABLE, particle.getType());
            assertTrue(particle.getData() >= 1.0f && particle.getData() <= 26.0f);
            assertTrue(particle.getLifetimeTicks() >= 30.0f && particle.getLifetimeTicks() <= 39.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Enchantment-table particles should use the old target-to-table curve")
    void enchantmentTableParticlesUseOldTargetToTableCurve() {
        WorldParticle particle = new WorldParticle(WorldParticle.Type.ENCHANTMENT_TABLE,
                0.5f, 72.0f, 0.5f,
                0.0f, 0.0f, 0.0f,
                0.30f, 30, 5.0f,
                2.0f, 71.0f, 0.0f);

        assertEquals(2.0f, particle.getRenderX(0.0f), 0.0001f);
        assertEquals(71.0f, particle.getRenderY(0.0f), 0.0001f);
        assertEquals(0.0f, particle.getRenderZ(0.0f), 0.0001f);

        assertFalse(particle.update(15.0f / 20.0f));

        assertEquals(1.25f, particle.getRenderX(0.0f), 0.0001f);
        assertEquals(71.425f, particle.getRenderY(0.0f), 0.0001f);
        assertEquals(0.25f, particle.getRenderZ(0.0f), 0.0001f);
    }

    private static void assertFragmentMatchesPoof(WorldParticle poof, WorldParticle fragment, float initialScale) {
        assertEquals(poof.getRenderX(1.0f), fragment.getRenderX(1.0f), 0.0001f);
        assertEquals(poof.getRenderY(1.0f), fragment.getRenderY(1.0f), 0.0001f);
        assertEquals(poof.getRenderZ(1.0f), fragment.getRenderZ(1.0f), 0.0001f);
        assertEquals(initialScale, fragment.getScale(0.0f), 0.0001f);
    }

    private static Random alwaysZeroRandom() {
        return new Random(0L) {
            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }

    private static Random fixedFloatRandom(float value) {
        return new Random(0L) {
            @Override
            public float nextFloat() {
                return value;
            }

            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
    }

    private static Random fixedWaterSuspendedRandom(float value) {
        return new Random(0L) {
            @Override
            public float nextFloat() {
                return value;
            }

            @Override
            public int nextInt(int bound) {
                assertEquals(10, bound);
                return 0;
            }
        };
    }

    private static Random fixedRepeaterDisplayRandom(float value, int repeaterBranch) {
        return new Random(0L) {
            @Override
            public float nextFloat() {
                return value;
            }

            @Override
            public int nextInt(int bound) {
                if (bound == 2) {
                    return Math.max(0, Math.min(1, repeaterBranch));
                }
                return 0;
            }
        };
    }
}
