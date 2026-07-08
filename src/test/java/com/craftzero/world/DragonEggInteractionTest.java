package com.craftzero.world;

import com.craftzero.entity.FallingBlockEntity;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.tile.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DragonEggInteractionTest {
    @Test
    @DisplayName("Dragon egg teleports using the Release 1.0 source-shaped random search")
    void dragonEggTeleportsWithSourceRandomSearch() {
        long seed = 122L;
        World world = new World(seed);
        try {
            BlockPos expected = firstDragonEggTarget(seed, 0, 70, 0);
            world.setBlock(expected.x(), expected.y(), expected.z(), BlockType.AIR, 0);
            world.setBlock(0, 70, 0, BlockType.DRAGON_EGG, 5);
            world.getRandom().setSeed(seed);

            BlockPos target = world.teleportDragonEgg(0, 70, 0);

            assertNotNull(target);
            assertEquals(expected, target);
            assertSame(BlockType.AIR, world.getBlock(0, 70, 0));
            assertSame(BlockType.DRAGON_EGG, world.getBlock(target.x(), target.y(), target.z()));
            assertEquals(5, world.getBlockMetadata(target.x(), target.y(), target.z()));
            assertTrue(Math.abs(target.x()) <= 15);
            assertTrue(Math.abs(target.z()) <= 15);
            assertTrue(Math.abs(target.y() - 70) <= 7);
            assertEquals(128, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.PORTAL)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dragon egg can still be broken and dropped when no teleport target exists")
    void dragonEggDropsWhenTeleportSearchFails() {
        World world = new World(123L);
        try {
            for (int x = -15; x <= 15; x++) {
                for (int y = 63; y <= 77; y++) {
                    for (int z = -15; z <= 15; z++) {
                        world.setBlock(x, y, z, BlockType.STONE, 0);
                    }
                }
            }
            world.setBlock(0, 70, 0, BlockType.DRAGON_EGG, 0);

            assertNull(world.teleportDragonEgg(0, 70, 0));
            assertSame(BlockType.DRAGON_EGG, world.getBlock(0, 70, 0));
            assertTrue(world.getParticles().isEmpty());

            assertTrue(world.breakBlock(0, 70, 0, true));
            assertSame(BlockType.AIR, world.getBlock(0, 70, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.DRAGON_EGG));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dragon egg uses Release 1.0 light, shape, and falling semantics")
    void dragonEggUsesReleaseOneBlockSemantics() {
        VoxelShape shape = BlockShape.collisionShape(BlockState.of(BlockType.DRAGON_EGG), emptyContext());

        assertEquals(1, BlockType.DRAGON_EGG.getLightEmission());
        assertTrue(BlockType.DRAGON_EGG.isFallingBlock());
        assertFalse(shape.isEmpty());
        assertFalse(shape.isFullCube());
        assertEquals(1, shape.boxes().size());
        BlockShape.Cuboid box = shape.boxes().get(0);
        assertEquals(1.0f / 16.0f, box.minX(), 0.0001f);
        assertEquals(15.0f / 16.0f, box.maxX(), 0.0001f);
        assertEquals(1.0f, box.maxY(), 0.0001f);
    }

    @Test
    @DisplayName("Unsupported dragon eggs spawn falling block entities and settle like sand")
    void dragonEggFallsWhenUnsupported() {
        World world = new World(124L);
        try {
            world.setBlock(0, 68, 0, BlockType.STONE);
            world.setBlock(0, 70, 0, BlockType.DRAGON_EGG);

            world.advanceBlockTicks(3);

            assertTrue(world.hasEntityOfType(FallingBlockEntity.class));
            runEntities(world, 80);
            assertSame(BlockType.AIR, world.getBlock(0, 70, 0));
            assertSame(BlockType.DRAGON_EGG, world.getBlock(0, 69, 0));
            assertTrue(world.getDroppedItems().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    private static BlockPos firstDragonEggTarget(long seed, int x, int y, int z) {
        Random random = new Random(seed);
        for (int attempt = 0; attempt < 1000; attempt++) {
            int nx = x + random.nextInt(16) - random.nextInt(16);
            int ny = y + random.nextInt(8) - random.nextInt(8);
            int nz = z + random.nextInt(16) - random.nextInt(16);
            if (nx != x || ny != y || nz != z) {
                return new BlockPos(nx, ny, nz);
            }
        }
        throw new AssertionError("Expected a non-origin dragon egg teleport target");
    }

    private static void runEntities(World world, int ticks) {
        for (int i = 0; i < ticks; i++) {
            world.updateEntities(1.0f / 20.0f);
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
