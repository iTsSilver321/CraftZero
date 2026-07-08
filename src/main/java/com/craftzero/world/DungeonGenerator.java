package com.craftzero.world;

import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.MonsterSpawnerTileEntity;

import java.util.Random;

public final class DungeonGenerator {
    private static final int ROOM_HEIGHT = 3;
    private static final int DUNGEON_ATTEMPTS_PER_CHUNK = 8;

    @FunctionalInterface
    interface BlockReader {
        BlockType getBlock(int worldX, int y, int worldZ);
    }

    @FunctionalInterface
    interface BlockWriter {
        boolean setBlock(int worldX, int y, int worldZ, BlockType block);
    }

    public void generate(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
        for (int originChunkX = chunkX - 1; originChunkX <= chunkX; originChunkX++) {
            for (int originChunkZ = chunkZ - 1; originChunkZ <= chunkZ; originChunkZ++) {
                Random random = populationRandom(seed, originChunkX, originChunkZ);
                generateFromOrigin(world, chunk, random, chunkX, chunkZ, originChunkX, originChunkZ);
            }
        }
    }

    void generateFromOrigin(World world, Chunk chunk, Random random, int chunkX, int chunkZ,
            int originChunkX, int originChunkZ) {
        generateFromOrigin(world, chunk, random, chunkX, chunkZ, originChunkX, originChunkZ,
                defaultReader(chunk, chunkX, chunkZ));
    }

    void generateFromOrigin(World world, Chunk chunk, Random random, int chunkX, int chunkZ,
            int originChunkX, int originChunkZ, BlockReader blocks) {
        generateFromOrigin(world, chunk, random, chunkX, chunkZ, originChunkX, originChunkZ,
                blocks, defaultWriter(chunk, chunkX, chunkZ));
    }

    void generateFromOrigin(World world, Chunk chunk, Random random, int chunkX, int chunkZ,
            int originChunkX, int originChunkZ, BlockReader blocks, BlockWriter writer) {
        int originX = originChunkX * Chunk.WIDTH;
        int originZ = originChunkZ * Chunk.DEPTH;
        for (int attempt = 0; attempt < DUNGEON_ATTEMPTS_PER_CHUNK; attempt++) {
            int centerX = originX + random.nextInt(16) + 8;
            int centerY = random.nextInt(128);
            int centerZ = originZ + random.nextInt(16) + 8;
            tryGenerateRoom(world, chunk, random, chunkX, chunkZ, centerX, centerY, centerZ, blocks, writer);
        }
    }

    boolean tryGenerateRoom(World world, Chunk chunk, Random random, int chunkX, int chunkZ,
            int centerX, int centerY, int centerZ) {
        return tryGenerateRoom(world, chunk, random, chunkX, chunkZ, centerX, centerY, centerZ,
                defaultReader(chunk, chunkX, chunkZ), defaultWriter(chunk, chunkX, chunkZ));
    }

    boolean tryGenerateRoom(World world, Chunk chunk, Random random, int chunkX, int chunkZ,
            int centerX, int centerY, int centerZ, BlockReader blocks, BlockWriter writer) {
        int halfWidth = 2 + random.nextInt(2);
        int halfDepth = 2 + random.nextInt(2);

        int openings = countSideOpenings(blocks, centerX, centerY, centerZ, halfWidth, halfDepth);
        if (openings < 1 || openings > 5) {
            return false;
        }
        if (!hasValidEnvelope(blocks, centerX, centerY, centerZ, halfWidth, halfDepth)) {
            return false;
        }

        placeShell(blocks, writer, random, centerX, centerY, centerZ, halfWidth, halfDepth);
        placeChests(world, blocks, writer, random, centerX, centerY, centerZ, halfWidth, halfDepth);
        placeSpawner(world, writer, random, centerX, centerY, centerZ);
        return true;
    }

    private static int countSideOpenings(BlockReader blocks, int centerX, int centerY, int centerZ,
            int halfWidth, int halfDepth) {
        int openings = 0;
        int minX = centerX - halfWidth - 1;
        int maxX = centerX + halfWidth + 1;
        int minZ = centerZ - halfDepth - 1;
        int maxZ = centerZ + halfDepth + 1;
        for (int x = minX; x <= maxX; x++) {
            if (isTwoHighAir(blocks, x, centerY, minZ)) {
                openings++;
            }
            if (isTwoHighAir(blocks, x, centerY, maxZ)) {
                openings++;
            }
        }
        for (int z = minZ; z <= maxZ; z++) {
            if (isTwoHighAir(blocks, minX, centerY, z)) {
                openings++;
            }
            if (isTwoHighAir(blocks, maxX, centerY, z)) {
                openings++;
            }
        }
        return openings;
    }

    private static boolean isTwoHighAir(BlockReader blocks, int x, int y, int z) {
        return blocks.getBlock(x, y, z) == BlockType.AIR
                && blocks.getBlock(x, y + 1, z) == BlockType.AIR;
    }

    private static boolean hasValidEnvelope(BlockReader blocks, int centerX, int centerY, int centerZ,
            int halfWidth, int halfDepth) {
        int minX = centerX - halfWidth - 1;
        int maxX = centerX + halfWidth + 1;
        int minZ = centerZ - halfDepth - 1;
        int maxZ = centerZ + halfDepth + 1;
        for (int x = minX; x <= maxX; x++) {
            for (int y = centerY - 1; y <= centerY + ROOM_HEIGHT + 1; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockType block = blocks.getBlock(x, y, z);
                    if (y == centerY - 1 && !isSolidDungeonBlock(block)) {
                        return false;
                    }
                    if (y == centerY + ROOM_HEIGHT + 1 && !isSolidDungeonBlock(block)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void placeShell(BlockReader blocks, BlockWriter writer, Random random,
            int centerX, int centerY, int centerZ, int halfWidth, int halfDepth) {
        for (int x = centerX - halfWidth - 1; x <= centerX + halfWidth + 1; x++) {
            for (int y = centerY + ROOM_HEIGHT; y >= centerY - 1; y--) {
                for (int z = centerZ - halfDepth - 1; z <= centerZ + halfDepth + 1; z++) {
                    boolean boundary = x == centerX - halfWidth - 1
                            || x == centerX + halfWidth + 1
                            || z == centerZ - halfDepth - 1
                            || z == centerZ + halfDepth + 1
                            || y == centerY - 1;
                    if (boundary) {
                        if (y >= 0 && !isSolidDungeonBlock(blocks.getBlock(x, y - 1, z))) {
                            writer.setBlock(x, y, z, BlockType.AIR);
                            continue;
                        }
                        if (!isSolidDungeonBlock(blocks.getBlock(x, y, z))) {
                            continue;
                        }
                        if (y == centerY - 1 && random.nextInt(4) != 0) {
                            writer.setBlock(x, y, z, BlockType.MOSSY_COBBLESTONE);
                        } else {
                            writer.setBlock(x, y, z, BlockType.COBBLESTONE);
                        }
                    } else {
                        writer.setBlock(x, y, z, BlockType.AIR);
                    }
                }
            }
        }
    }

    private void placeChests(World world, BlockReader blocks, BlockWriter writer, Random random,
            int centerX, int centerY, int centerZ, int halfWidth, int halfDepth) {
        for (int chestIndex = 0; chestIndex < 2; chestIndex++) {
            for (int attempt = 0; attempt < 3; attempt++) {
                int x = centerX + random.nextInt(halfWidth * 2 + 1) - halfWidth;
                int z = centerZ + random.nextInt(halfDepth * 2 + 1) - halfDepth;
                if (blocks.getBlock(x, centerY, z) != BlockType.AIR
                        || countSolidHorizontalNeighbors(blocks, x, centerY, z) != 1) {
                    continue;
                }
                boolean inChunk = writer.setBlock(x, centerY, z, BlockType.CHEST);
                ChestTileEntity chest = inChunk ? new ChestTileEntity(x, centerY, z) : null;
                fillChest(chest, random);
                if (chest != null && world != null) {
                    chest.clearDirty();
                    world.stageGeneratedTileEntity(chest);
                }
                break;
            }
        }
    }

    private static int countSolidHorizontalNeighbors(BlockReader blocks, int x, int y, int z) {
        int count = 0;
        if (isSolidDungeonBlock(blocks.getBlock(x - 1, y, z))) {
            count++;
        }
        if (isSolidDungeonBlock(blocks.getBlock(x + 1, y, z))) {
            count++;
        }
        if (isSolidDungeonBlock(blocks.getBlock(x, y, z - 1))) {
            count++;
        }
        if (isSolidDungeonBlock(blocks.getBlock(x, y, z + 1))) {
            count++;
        }
        return count;
    }

    private static void placeSpawner(World world, BlockWriter writer, Random random,
            int centerX, int centerY, int centerZ) {
        if (!writer.setBlock(centerX, centerY, centerZ, BlockType.MOB_SPAWNER) || world == null) {
            pickMobSpawner(random);
            return;
        }
        MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(centerX, centerY, centerZ);
        spawner.setMobDefinition(pickMobSpawner(random));
        spawner.clearDirty();
        world.stageGeneratedTileEntity(spawner);
    }

    private static ItemStack pickChestLootItem(Random random) {
        int roll = random.nextInt(11);
        if (roll == 0) {
            return new ItemStack(ItemType.SADDLE, 1);
        }
        if (roll == 1) {
            return new ItemStack(ItemType.IRON_INGOT, random.nextInt(4) + 1);
        }
        if (roll == 2) {
            return new ItemStack(ItemType.BREAD, 1);
        }
        if (roll == 3) {
            return new ItemStack(ItemType.WHEAT, random.nextInt(4) + 1);
        }
        if (roll == 4) {
            return new ItemStack(ItemType.GUNPOWDER, random.nextInt(4) + 1);
        }
        if (roll == 5) {
            return new ItemStack(ItemType.STRING, random.nextInt(4) + 1);
        }
        if (roll == 6) {
            return new ItemStack(ItemType.BUCKET, 1);
        }
        if (roll == 7 && random.nextInt(100) == 0) {
            return new ItemStack(ItemType.GOLDEN_APPLE, 1);
        }
        if (roll == 8 && random.nextInt(2) == 0) {
            return new ItemStack(ItemType.REDSTONE, random.nextInt(4) + 1);
        }
        if (roll == 9 && random.nextInt(10) == 0) {
            return new ItemStack(random.nextInt(2) == 0 ? ItemType.RECORD_13 : ItemType.RECORD_CAT, 1);
        }
        if (roll == 10) {
            return new ItemStack(ItemType.COCOA_BEANS, 1);
        }
        return null;
    }

    private static MobDefinition pickMobSpawner(Random random) {
        return switch (random.nextInt(4)) {
            case 0 -> MobDefinition.SKELETON;
            case 1, 2 -> MobDefinition.ZOMBIE;
            case 3 -> MobDefinition.SPIDER;
            default -> MobDefinition.ZOMBIE;
        };
    }

    private static Random populationRandom(long seed, int chunkX, int chunkZ) {
        Random random = new Random(seed);
        long xSeed = (random.nextLong() / 2L) * 2L + 1L;
        long zSeed = (random.nextLong() / 2L) * 2L + 1L;
        random.setSeed((long) chunkX * xSeed + (long) chunkZ * zSeed ^ seed);
        return random;
    }

    private static BlockReader defaultReader(Chunk chunk, int chunkX, int chunkZ) {
        return (worldX, y, worldZ) -> blockAt(chunk, chunkX, chunkZ, worldX, y, worldZ);
    }

    private static BlockWriter defaultWriter(Chunk chunk, int chunkX, int chunkZ) {
        return (worldX, y, worldZ, block) -> setIfInChunk(chunk, chunkX, chunkZ, worldX, y, worldZ, block);
    }

    private static BlockType blockAt(Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return BlockType.BEDROCK;
        }
        if (containsBlock(chunkX, chunkZ, worldX, worldZ)) {
            return chunk.getBlock(worldX - chunkX * Chunk.WIDTH, y, worldZ - chunkZ * Chunk.DEPTH);
        }
        return BlockType.STONE;
    }

    private static boolean setIfInChunk(Chunk chunk, int chunkX, int chunkZ,
            int worldX, int y, int worldZ, BlockType block) {
        if (!containsBlock(chunkX, chunkZ, worldX, worldZ) || y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        chunk.setBlock(worldX - chunkX * Chunk.WIDTH, y, worldZ - chunkZ * Chunk.DEPTH, block);
        return true;
    }

    private static boolean containsBlock(int chunkX, int chunkZ, int worldX, int worldZ) {
        int minX = chunkX * Chunk.WIDTH;
        int minZ = chunkZ * Chunk.DEPTH;
        return worldX >= minX && worldX < minX + Chunk.WIDTH
                && worldZ >= minZ && worldZ < minZ + Chunk.DEPTH;
    }

    private static boolean isSolidDungeonBlock(BlockType block) {
        return block != null && block != BlockType.AIR && !block.isFluid() && block != BlockType.FIRE;
    }

    static void fillChest(ChestTileEntity chest, Random random) {
        for (int i = 0; i < 8; i++) {
            ItemStack item = pickChestLootItem(random);
            if (item != null) {
                int slot = random.nextInt(ChestTileEntity.SIZE);
                if (chest != null) {
                    chest.getInventory()[slot] = item;
                }
            }
        }
    }
}
