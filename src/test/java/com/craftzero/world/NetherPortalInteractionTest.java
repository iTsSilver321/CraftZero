package com.craftzero.world;

import com.craftzero.world.tile.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class NetherPortalInteractionTest {
    @Test
    @DisplayName("Bottom-row fire should activate a Release 1.0 4x5 Nether portal")
    void bottomFireActivatesFixedNetherPortal() {
        World world = new World(9101L);
        try {
            buildFrameX(world, 0, 71, 0);

            world.setBlock(0, 71, 0, BlockType.FIRE, 0);

            assertPortalInteriorX(world, 0, 71, 0);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Nether portals should also activate in the perpendicular orientation")
    void activatesZAxisNetherPortal() {
        World world = new World(9102L);
        try {
            buildFrameZ(world, 0, 71, 0);

            world.setBlock(0, 71, 0, BlockType.FIRE, 0);

            for (int y = 71; y <= 73; y++) {
                for (int z = 0; z <= 1; z++) {
                    assertSame(BlockType.PORTAL, world.getBlock(0, y, z));
                    assertEquals(BlockShape.PORTAL_AXIS_Z, world.getBlockMetadata(0, y, z));
                }
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 Nether portals should not activate from upper interior fire")
    void upperInteriorFireDoesNotActivateReleaseOnePortal() {
        World world = new World(9103L);
        try {
            buildFrameX(world, 0, 71, 0);

            world.setBlock(0, 72, 0, BlockType.FIRE, 0);

            assertSame(BlockType.FIRE, world.getBlock(0, 72, 0));
            assertFalse(world.tryActivateNetherPortalFromFire(0, 72, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Incomplete obsidian frames should not activate")
    void incompleteFrameDoesNotActivate() {
        World world = new World(9104L);
        try {
            buildFrameX(world, 0, 71, 0);
            world.setBlock(2, 72, 0, BlockType.AIR, 0);

            world.setBlock(0, 71, 0, BlockType.FIRE, 0);

            assertSame(BlockType.FIRE, world.getBlock(0, 71, 0));
            assertSame(BlockType.AIR, world.getBlock(1, 71, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 Nether portals should require full obsidian corners")
    void missingCornerDoesNotActivateReleaseOnePortal() {
        World world = new World(9107L);
        try {
            buildFrameX(world, 0, 71, 0);
            world.setBlock(-1, 70, 0, BlockType.AIR, 0);

            world.setBlock(0, 71, 0, BlockType.FIRE, 0);

            assertSame(BlockType.FIRE, world.getBlock(0, 71, 0));
            assertSame(BlockType.AIR, world.getBlock(1, 71, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Breaking an obsidian side should collapse the whole Nether portal")
    void breakingFrameCollapsesNetherPortal() {
        World world = new World(9105L);
        try {
            buildFrameX(world, 0, 71, 0);
            world.setBlock(0, 71, 0, BlockType.FIRE, 0);
            assertPortalInteriorX(world, 0, 71, 0);

            world.breakBlock(-1, 72, 0, false);

            for (int y = 71; y <= 73; y++) {
                for (int x = 0; x <= 1; x++) {
                    assertSame(BlockType.AIR, world.getBlock(x, y, 0));
                }
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Replacing an active portal interior block should collapse the whole Nether portal")
    void replacingPortalInteriorCollapsesNetherPortal() {
        World world = new World(9112L);
        try {
            buildFrameX(world, 0, 71, 0);
            world.setBlock(0, 71, 0, BlockType.FIRE, 0);
            assertPortalInteriorX(world, 0, 71, 0);

            world.setBlock(0, 72, 0, BlockType.AIR, 0);

            for (int y = 71; y <= 73; y++) {
                for (int x = 0; x <= 1; x++) {
                    assertSame(BlockType.AIR, world.getBlock(x, y, 0));
                }
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Portal blocks should be light-emitting non-colliding planes")
    void portalBlocksUsePortalPhysics() {
        BlockShape.BlockContext context = emptyContext();
        VoxelShape xAxisRender = BlockShape.renderShape(
                new BlockState(BlockType.PORTAL, BlockShape.PORTAL_AXIS_X), context);
        VoxelShape zAxisRender = BlockShape.renderShape(
                new BlockState(BlockType.PORTAL, BlockShape.PORTAL_AXIS_Z), context);

        assertTrue(BlockShape.collisionShape(new BlockState(BlockType.PORTAL, BlockShape.PORTAL_AXIS_X), context).isEmpty());
        assertTrue(BlockShape.selectionShape(new BlockState(BlockType.PORTAL, BlockShape.PORTAL_AXIS_X), context).isEmpty());
        assertTrue(BlockShape.canFallThrough(BlockType.PORTAL));
        assertFalse(xAxisRender.isFullCube());
        assertEquals(6.0f / 16.0f, xAxisRender.boxes().get(0).minZ(), 0.0001f);
        assertEquals(10.0f / 16.0f, xAxisRender.boxes().get(0).maxZ(), 0.0001f);
        assertEquals(0.0f, xAxisRender.boxes().get(0).minX(), 0.0001f);
        assertEquals(1.0f, xAxisRender.boxes().get(0).maxX(), 0.0001f);
        assertFalse(zAxisRender.isFullCube());
        assertEquals(6.0f / 16.0f, zAxisRender.boxes().get(0).minX(), 0.0001f);
        assertEquals(10.0f / 16.0f, zAxisRender.boxes().get(0).maxX(), 0.0001f);
        assertEquals(0.0f, zAxisRender.boxes().get(0).minZ(), 0.0001f);
        assertEquals(1.0f, zAxisRender.boxes().get(0).maxZ(), 0.0001f);
        assertEquals(11, BlockType.PORTAL.getLightEmission());
    }

    @Test
    @DisplayName("Active Nether portal blocks should emit Release-style ambient particles and sound")
    void portalBlocksEmitAmbientFeedback() {
        World world = new World(9110L);
        try {
            buildFrameX(world, 0, 70, 0);
            world.setBlock(0, 70, 0, BlockType.FIRE, 0);
            assertPortalInteriorX(world, 0, 70, 0);

            assertTrue(world.tickNetherPortalAmbientAt(0, 70, 0,
                    new SequenceRandom(new int[] { 0, 1, 1, 1, 1 }, 0.5f)));

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertEquals(WorldSoundEvent.PORTAL_AMBIENT, sounds.get(0).soundId());
            assertEquals(0.5f, sounds.get(0).volume(), 0.0001f);
            assertEquals(1.0f, sounds.get(0).pitch(), 0.0001f);

            List<WorldParticle> particles = world.getParticles();
            assertEquals(4, particles.stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.PORTAL)
                    .count());
            assertTrue(particles.stream()
                    .allMatch(particle -> Math.abs(particle.getRenderZ(0.0f) - 0.75f) < 0.0001f));
            assertFalse(world.tickNetherPortalAmbientAt(8, 70, 0,
                    new SequenceRandom(new int[] { 0 }, 0.5f)));
            assertEquals(4, world.getParticles().size());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("World ambient display ticks should sample nearby portal blocks around the player")
    void ambientDisplayTicksSampleNearbyPortalBlocks() {
        World world = new World(9111L);
        try {
            com.craftzero.main.Player player = new com.craftzero.main.Player(0.5f, 70.0f, 0.5f);
            world.setPlayer(player);
            buildFrameX(world, 0, 70, 0);
            world.setBlock(0, 70, 0, BlockType.FIRE, 0);

            for (int i = 0; i < 20 && world.getParticles().isEmpty(); i++) {
                world.updateAmbientBlockEffects(1.0f / 20.0f);
            }

            assertTrue(world.getParticles().stream()
                    .anyMatch(particle -> particle.getType() == WorldParticle.Type.PORTAL));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Destination worlds should prepare a matching Nether portal around the transfer target")
    void destinationPortalIsPreparedAtTransferTarget() {
        World world = new World(9106L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            BlockPos portalPos = world.ensureNetherPortalAt(10.25f, 72.0f, -4.75f);

            assertEquals(new BlockPos(10, 72, -5), portalPos);
            assertPortalInteriorX(world, 10, 72, -5);
            for (int x = 9; x <= 12; x++) {
                assertSame(BlockType.OBSIDIAN, world.getBlock(x, 71, -5));
                assertSame(BlockType.OBSIDIAN, world.getBlock(x, 75, -5));
            }
            assertSame(BlockType.AIR, world.getBlock(10, 72, -6));
            assertSame(BlockType.AIR, world.getBlock(11, 72, -4));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Destination portal preparation should reuse an existing nearby portal")
    void destinationPortalReusesExistingNearbyPortal() {
        World world = new World(9108L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            buildFrameZ(world, 20, 70, -3);
            world.setBlock(20, 70, -3, BlockType.FIRE, 0);
            assertSame(BlockType.PORTAL, world.getBlock(20, 70, -3));
            world.setBlock(10, 72, -5, BlockType.DIAMOND_BLOCK, 0);

            BlockPos portalPos = world.ensureNetherPortalAt(10.25f, 72.0f, -4.75f);

            assertEquals(new BlockPos(20, 70, -3), portalPos);
            assertSame(BlockType.DIAMOND_BLOCK, world.getBlock(10, 72, -5));
            for (int y = 70; y <= 72; y++) {
                for (int z = -3; z <= -2; z++) {
                    assertSame(BlockType.PORTAL, world.getBlock(20, y, z));
                    assertEquals(BlockShape.PORTAL_AXIS_Z, world.getBlockMetadata(20, y, z));
                }
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Destination portal preparation should ignore incomplete nearby portals")
    void destinationPortalSkipsIncompleteExistingPortal() {
        World world = new World(9113L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            buildFrameZ(world, 20, 70, -3);
            world.setBlock(20, 70, -3, BlockType.FIRE, 0);
            assertSame(BlockType.PORTAL, world.getBlock(20, 70, -3));
            world.getChunk(Math.floorDiv(20, Chunk.WIDTH), Math.floorDiv(-3, Chunk.DEPTH))
                    .setBlock(Math.floorMod(20, Chunk.WIDTH), 71, Math.floorMod(-3, Chunk.DEPTH),
                            BlockType.AIR, 0);

            BlockPos portalPos = world.ensureNetherPortalAt(10.25f, 72.0f, -4.75f);

            assertNotEquals(new BlockPos(20, 70, -3), portalPos);
            assertSame(BlockType.AIR, world.getBlock(20, 71, -3));
            assertTrue(world.isNetherPortalAt(portalPos.x(), portalPos.y(), portalPos.z()));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Destination portal creation should prefer a nearby safe cavity before forced target overwrite")
    void destinationPortalCreationPrefersNearbySafeCavity() {
        World world = new World(9109L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        try {
            fillTargetChunkSolid(world, 10, -5, 2, 80);
            carveSafePortalCavityX(world, 14, 70, -5);
            world.setBlock(10, 72, -5, BlockType.DIAMOND_BLOCK, 0);

            BlockPos portalPos = world.ensureNetherPortalAt(10.25f, 72.0f, -4.75f);

            assertEquals(new BlockPos(14, 70, -5), portalPos);
            assertSame(BlockType.DIAMOND_BLOCK, world.getBlock(10, 72, -5));
            assertPortalInteriorX(world, 14, 70, -5);
            assertSame(BlockType.AIR, world.getBlock(14, 70, -6));
            assertSame(BlockType.AIR, world.getBlock(15, 72, -4));
        } finally {
            world.cleanup();
        }
    }

    private static void buildFrameX(World world, int minX, int minY, int z) {
        int bottomY = minY - 1;
        int topY = minY + 3;
        for (int x = minX - 1; x <= minX + 2; x++) {
            world.setBlock(x, bottomY, z, BlockType.OBSIDIAN, 0);
            world.setBlock(x, topY, z, BlockType.OBSIDIAN, 0);
        }
        for (int y = minY; y <= minY + 2; y++) {
            world.setBlock(minX - 1, y, z, BlockType.OBSIDIAN, 0);
            world.setBlock(minX + 2, y, z, BlockType.OBSIDIAN, 0);
            for (int x = minX; x <= minX + 1; x++) {
                world.setBlock(x, y, z, BlockType.AIR, 0);
            }
        }
    }

    private static void buildFrameZ(World world, int x, int minY, int minZ) {
        int bottomY = minY - 1;
        int topY = minY + 3;
        for (int z = minZ - 1; z <= minZ + 2; z++) {
            world.setBlock(x, bottomY, z, BlockType.OBSIDIAN, 0);
            world.setBlock(x, topY, z, BlockType.OBSIDIAN, 0);
        }
        for (int y = minY; y <= minY + 2; y++) {
            world.setBlock(x, y, minZ - 1, BlockType.OBSIDIAN, 0);
            world.setBlock(x, y, minZ + 2, BlockType.OBSIDIAN, 0);
            for (int z = minZ; z <= minZ + 1; z++) {
                world.setBlock(x, y, z, BlockType.AIR, 0);
            }
        }
    }

    private static void fillTargetChunkSolid(World world, int blockX, int blockZ, int minY, int maxY) {
        int minX = Math.floorDiv(blockX, Chunk.WIDTH) * Chunk.WIDTH;
        int minZ = Math.floorDiv(blockZ, Chunk.DEPTH) * Chunk.DEPTH;
        for (int x = minX; x < minX + Chunk.WIDTH; x++) {
            for (int z = minZ; z < minZ + Chunk.DEPTH; z++) {
                for (int y = minY; y <= maxY; y++) {
                    world.setBlock(x, y, z, BlockType.STONE, 0);
                }
            }
        }
    }

    private static void carveSafePortalCavityX(World world, int minX, int minY, int z) {
        for (int x = minX - 1; x <= minX + 2; x++) {
            world.setBlock(x, minY - 2, z, BlockType.STONE, 0);
        }
        for (int y = minY - 1; y <= minY + 3; y++) {
            for (int x = minX - 1; x <= minX + 2; x++) {
                world.setBlock(x, y, z, BlockType.AIR, 0);
            }
        }
        for (int y = minY; y <= minY + 2; y++) {
            for (int x = minX; x <= minX + 1; x++) {
                world.setBlock(x, y, z - 1, BlockType.AIR, 0);
                world.setBlock(x, y, z + 1, BlockType.AIR, 0);
            }
        }
    }

    private static void assertPortalInteriorX(World world, int minX, int minY, int z) {
        for (int y = minY; y <= minY + 2; y++) {
            for (int x = minX; x <= minX + 1; x++) {
                assertSame(BlockType.PORTAL, world.getBlock(x, y, z));
                assertEquals(BlockShape.PORTAL_AXIS_X, world.getBlockMetadata(x, y, z));
            }
        }
    }

    private static final class SequenceRandom extends Random {
        private final int[] ints;
        private final float fallbackFloat;
        private int intIndex;

        private SequenceRandom(int[] ints, float fallbackFloat) {
            this.ints = ints;
            this.fallbackFloat = fallbackFloat;
        }

        @Override
        public int nextInt(int bound) {
            int value = ints.length == 0 ? 0 : ints[Math.min(intIndex, ints.length - 1)];
            intIndex++;
            return Math.floorMod(value, bound);
        }

        @Override
        public float nextFloat() {
            return fallbackFloat;
        }
    }

    private static BlockShape.BlockContext emptyContext() {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return 0;
            }
        };
    }
}
