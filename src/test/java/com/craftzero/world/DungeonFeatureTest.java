package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DungeonFeatureTest {
    @Test
    @DisplayName("Dungeon generation should place a vanilla-height room shell and spawner when the envelope is valid")
    void dungeonPlacesRoomShellAndSpawner() {
        DungeonGenerator generator = new DungeonGenerator();
        World world = new World(701L);
        try {
            Chunk chunk = validDungeonChunk(8, 40, 8);

            assertTrue(generator.tryGenerateRoom(world, chunk, new DungeonTestRandom(), 0, 0, 8, 40, 8));

            assertSame(BlockType.MOB_SPAWNER, chunk.getBlock(8, 40, 8));
            assertSame(BlockType.AIR, chunk.getBlock(8, 43, 8),
                    "Release dungeons clear the top interior layer and rely on existing stone above as the ceiling");
            assertTrue(contains(chunk, BlockType.MOSSY_COBBLESTONE));
            assertTrue(contains(chunk, BlockType.COBBLESTONE));
            assertTrue(contains(chunk, BlockType.AIR));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dungeon generation should reject rooms without one to five side openings")
    void dungeonRejectsRoomsWithoutSideOpenings() {
        DungeonGenerator generator = new DungeonGenerator();
        World world = new World(702L);
        try {
            Chunk chunk = solidStoneChunk(0, 0);

            assertFalse(generator.tryGenerateRoom(world, chunk, new DungeonTestRandom(), 0, 0, 8, 40, 8));
            assertFalse(contains(chunk, BlockType.MOB_SPAWNER));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dungeon rooms should spill into a chunk from shifted neighboring origins")
    void dungeonSpillsAcrossChunkBorders() {
        DungeonGenerator generator = new DungeonGenerator();
        World world = new World(703L);
        try {
            Chunk chunk = solidStoneChunk(0, 0);
            chunk.setBlock(0, 40, 8, BlockType.AIR);
            chunk.setBlock(0, 41, 8, BlockType.AIR);

            assertTrue(generator.tryGenerateRoom(world, chunk, new DungeonTestRandom(), 0, 0, -3, 40, 8));

            assertSame(BlockType.MOSSY_COBBLESTONE, chunk.getBlock(0, 39, 8));
            assertSame(BlockType.AIR, chunk.getBlock(0, 40, 8),
                    "The opening on the chunk edge should remain air while the floor spills into this chunk");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dungeon validation should use the supplied off-chunk block reader")
    void dungeonValidationUsesSuppliedOffChunkBlockReader() {
        DungeonGenerator generator = new DungeonGenerator();
        World world = new World(704L);
        try {
            Chunk chunk = solidStoneChunk(0, 0);
            chunk.setBlock(0, 40, 8, BlockType.AIR);
            chunk.setBlock(0, 41, 8, BlockType.AIR);
            DungeonGenerator.BlockReader reader = (x, y, z) -> {
                if (x == -1 && y == 39 && z == 8) {
                    return BlockType.AIR;
                }
                if (y < 0 || y >= Chunk.HEIGHT) {
                    return BlockType.BEDROCK;
                }
                if (x >= 0 && x < Chunk.WIDTH && z >= 0 && z < Chunk.DEPTH) {
                    return chunk.getBlock(x, y, z);
                }
                return BlockType.STONE;
            };
            DungeonGenerator.BlockWriter writer = (x, y, z, block) -> {
                if (y < 0 || y >= Chunk.HEIGHT || x < 0 || x >= Chunk.WIDTH || z < 0 || z >= Chunk.DEPTH) {
                    return false;
                }
                chunk.setBlock(x, y, z, block);
                return true;
            };

            assertFalse(generator.tryGenerateRoom(world, chunk, new DungeonTestRandom(),
                    0, 0, -3, 40, 8, reader, writer));
            assertFalse(contains(chunk, BlockType.MOB_SPAWNER));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dungeon generation should not skip source-valid rooms outside the target chunk")
    void dungeonGenerationConsumesSourceRoomOutsideTargetChunk() {
        DungeonGenerator generator = new DungeonGenerator();
        Chunk target = solidStoneChunk(0, 0);
        Map<String, BlockType> overlay = new HashMap<>();
        DungeonGenerator.BlockReader reader = (x, y, z) -> {
            BlockType block = overlay.get(key(x, y, z));
            if (block != null) {
                return block;
            }
            if (x == 21 && (y == 40 || y == 41) && z == 8) {
                return BlockType.AIR;
            }
            if (y < 0 || y >= Chunk.HEIGHT) {
                return BlockType.BEDROCK;
            }
            return BlockType.STONE;
        };
        DungeonGenerator.BlockWriter writer = (x, y, z, block) -> {
            overlay.put(key(x, y, z), block);
            return x >= 0 && x < Chunk.WIDTH && z >= 0 && z < Chunk.DEPTH;
        };

        assertTrue(generator.tryGenerateRoom(null, target, new DungeonTestRandom(), 0, 0,
                24, 40, 8, reader, writer));

        assertSame(BlockType.MOB_SPAWNER, overlay.get(key(24, 40, 8)),
                "Release 1.0 still generates source-valid rooms that land wholly outside the current chunk");
        assertFalse(contains(target, BlockType.MOB_SPAWNER));
    }

    private static Chunk validDungeonChunk(int centerX, int centerY, int centerZ) {
        Chunk chunk = solidStoneChunk(0, 0);
        chunk.setBlock(centerX - 3, centerY, centerZ, BlockType.AIR);
        chunk.setBlock(centerX - 3, centerY + 1, centerZ, BlockType.AIR);
        return chunk;
    }

    private static Chunk solidStoneChunk(int chunkX, int chunkZ) {
        Chunk chunk = new Chunk(chunkX, chunkZ);
        for (int x = 0; x < Chunk.WIDTH; x++) {
            for (int z = 0; z < Chunk.DEPTH; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    chunk.setBlock(x, y, z, BlockType.STONE);
                }
            }
        }
        return chunk;
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

    private static String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    private static final class DungeonTestRandom extends Random {
        private int calls;

        @Override
        public int nextInt(int bound) {
            calls++;
            if (calls <= 2) {
                return 0;
            }
            if (bound == 4) {
                return 1;
            }
            return 0;
        }
    }
}
