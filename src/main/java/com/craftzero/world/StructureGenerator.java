package com.craftzero.world;

import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.MonsterSpawnerTileEntity;

import java.util.Random;

public final class StructureGenerator {
    public void generate(World world, Chunk chunk, long seed, int chunkX, int chunkZ, Dimension dimension,
            ReleaseOneWorldGenerator generator) {
        if (dimension == Dimension.NETHER) {
            generateNetherFortress(world, chunk, seed, chunkX, chunkZ);
            return;
        }
        if (dimension != Dimension.OVERWORLD) {
            return;
        }
        generateMineshaft(world, chunk, seed, chunkX, chunkZ);
        generateStronghold(world, chunk, seed, chunkX, chunkZ);
        generateVillage(world, chunk, seed, chunkX, chunkZ, generator);
    }

    private void generateMineshaft(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
        Random random = cellRandom(seed ^ 0x4D1AE5A7L, chunkX, chunkZ, 5);
        int cellX = Math.floorDiv(chunkX, 5);
        int cellZ = Math.floorDiv(chunkZ, 5);
        int originX = cellX * 5 + random.nextInt(5);
        int originZ = cellZ * 5 + random.nextInt(5);
        if (chunkX != originX || chunkZ != originZ || random.nextInt(3) != 0) {
            return;
        }

        int y = 22 + random.nextInt(22);
        boolean eastWest = random.nextBoolean();
        for (int i = 1; i < 15; i++) {
            int x = eastWest ? i : 7;
            int z = eastWest ? 7 : i;
            carveTunnel(chunk, x, y, z);
            chunk.setBlock(x, y - 1, z, BlockType.OAK_PLANKS);
            chunk.setBlock(x, y, z, BlockType.RAIL);
            if (i % 4 == 0) {
                chunk.setBlock(x, y, Math.max(1, z - 2), BlockType.FENCE);
                chunk.setBlock(x, y + 1, Math.max(1, z - 2), BlockType.FENCE);
                chunk.setBlock(x, y, Math.min(14, z + 2), BlockType.FENCE);
                chunk.setBlock(x, y + 1, Math.min(14, z + 2), BlockType.FENCE);
                chunk.setBlock(x, y + 2, Math.max(1, z - 2), BlockType.OAK_PLANKS);
                chunk.setBlock(x, y + 2, Math.min(14, z + 2), BlockType.OAK_PLANKS);
            }
            if (random.nextInt(12) == 0) {
                chunk.setBlock(x, y + 1, z, BlockType.COBWEB);
            }
        }

        if (random.nextBoolean()) {
            placeSpawner(world, chunk, chunkX, chunkZ, 7, y, 7, MobDefinition.CAVE_SPIDER, random);
        }
        if (random.nextBoolean()) {
            placeLootChest(world, chunk, chunkX, chunkZ, eastWest ? 12 : 4, y, eastWest ? 4 : 12, random,
                    ItemType.RAIL, ItemType.IRON_INGOT, ItemType.BREAD, ItemType.TORCH, ItemType.REDSTONE);
        }
    }

    private void generateStronghold(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
        int strongX = strongholdChunk(seed, true);
        int strongZ = strongholdChunk(seed, false);
        if (chunkX != strongX || chunkZ != strongZ) {
            return;
        }
        int y0 = 24;
        box(chunk, 1, y0, 1, 14, y0 + 5, 14, BlockType.STONE_BRICK, BlockType.AIR);
        for (int z = 3; z <= 12; z++) {
            chunk.setBlock(3, y0 + 1, z, BlockType.BOOKSHELF);
            chunk.setBlock(12, y0 + 1, z, BlockType.BOOKSHELF);
            if (z % 3 == 0) {
                chunk.setBlock(3, y0 + 2, z, BlockType.BOOKSHELF);
                chunk.setBlock(12, y0 + 2, z, BlockType.BOOKSHELF);
            }
        }
        for (int x = 5; x <= 10; x++) {
            chunk.setBlock(x, y0 + 1, 6, BlockType.END_PORTAL_FRAME);
            chunk.setBlock(x, y0 + 1, 9, BlockType.END_PORTAL_FRAME);
        }
        for (int z = 7; z <= 8; z++) {
            chunk.setBlock(5, y0 + 1, z, BlockType.END_PORTAL_FRAME);
            chunk.setBlock(10, y0 + 1, z, BlockType.END_PORTAL_FRAME);
        }
        for (int z = 6; z <= 9; z += 3) {
            chunk.setBlock(7, y0 + 2, z, BlockType.IRON_BARS);
            chunk.setBlock(8, y0 + 2, z, BlockType.IRON_BARS);
        }
        placeSpawner(world, chunk, chunkX, chunkZ, 7, y0 + 1, 7, MobDefinition.SILVERFISH, new Random(seed ^ 0x51A7EF15L));
        placeLootChest(world, chunk, chunkX, chunkZ, 11, y0 + 1, 11, new Random(seed ^ 0xB00C5A11L),
                ItemType.BOOK, ItemType.PAPER, ItemType.ENDER_PEARL, ItemType.IRON_INGOT, ItemType.REDSTONE);
    }

    private void generateVillage(World world, Chunk chunk, long seed, int chunkX, int chunkZ,
            ReleaseOneWorldGenerator generator) {
        Random random = cellRandom(seed ^ 0x7111A6E5L, chunkX, chunkZ, 12);
        int cellX = Math.floorDiv(chunkX, 12);
        int cellZ = Math.floorDiv(chunkZ, 12);
        int originX = cellX * 12 + random.nextInt(12);
        int originZ = cellZ * 12 + random.nextInt(12);
        if (chunkX != originX || chunkZ != originZ || random.nextInt(5) != 0) {
            return;
        }

        int worldCenterX = chunkX * Chunk.WIDTH + 8;
        int worldCenterZ = chunkZ * Chunk.DEPTH + 8;
        BiomeType biome = generator.getBiome(worldCenterX, worldCenterZ);
        if (biome != BiomeType.PLAINS && biome != BiomeType.DESERT && biome != BiomeType.DESERT_HILLS) {
            return;
        }
        int y = highestSolid(chunk, 8, 8) + 1;
        if (y <= 4 || y >= Chunk.HEIGHT - 8) {
            return;
        }

        for (int x = 1; x < 15; x++) {
            chunk.setBlock(x, y - 1, 8, BlockType.GRAVEL);
        }
        for (int z = 1; z < 15; z++) {
            chunk.setBlock(8, y - 1, z, BlockType.GRAVEL);
        }
        placeSmallHouse(chunk, 2, y, 2, biome == BiomeType.DESERT ? BlockType.SANDSTONE : BlockType.OAK_PLANKS);
        placeFarm(chunk, 10, y, 2);
        placeLootChest(world, chunk, chunkX, chunkZ, 4, y + 1, 4, random,
                ItemType.BREAD, ItemType.WHEAT, ItemType.APPLE, ItemType.IRON_INGOT);
    }

    private void generateNetherFortress(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
        Random random = cellRandom(seed ^ 0xF047E55L, chunkX, chunkZ, 8);
        int cellX = Math.floorDiv(chunkX, 8);
        int cellZ = Math.floorDiv(chunkZ, 8);
        int originX = cellX * 8 + random.nextInt(8);
        int originZ = cellZ * 8 + random.nextInt(8);
        if (chunkX != originX || chunkZ != originZ || random.nextInt(3) != 0) {
            return;
        }

        int y = 54 + random.nextInt(18);
        box(chunk, 1, y, 5, 14, y + 5, 10, BlockType.NETHER_BRICK, BlockType.AIR);
        for (int x = 1; x < 15; x++) {
            chunk.setBlock(x, y + 1, 4, BlockType.NETHER_BRICK_FENCE);
            chunk.setBlock(x, y + 1, 11, BlockType.NETHER_BRICK_FENCE);
        }
        placeSpawner(world, chunk, chunkX, chunkZ, 8, y + 1, 8, MobDefinition.BLAZE, random);
        for (int x = 4; x <= 11; x++) {
            chunk.setBlock(x, y + 1, 6, BlockType.SOUL_SAND);
            chunk.setBlock(x, y + 2, 6, BlockType.NETHER_WART);
        }
        placeLootChest(world, chunk, chunkX, chunkZ, 12, y + 1, 8, random,
                ItemType.GOLD_INGOT, ItemType.GOLD_NUGGET, ItemType.BLAZE_ROD, ItemType.SADDLE, ItemType.NETHER_WART);
    }

    private static void carveTunnel(Chunk chunk, int cx, int y, int cz) {
        for (int dy = 0; dy <= 2; dy++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int x = cx + dx;
                    int z = cz + dz;
                    if (Chunk.isInBounds(x, y + dy, z)) {
                        chunk.setBlock(x, y + dy, z, BlockType.AIR);
                    }
                }
            }
        }
    }

    private static void placeSmallHouse(Chunk chunk, int x0, int y, int z0, BlockType wall) {
        box(chunk, x0, y, z0, x0 + 5, y + 4, z0 + 5, wall, BlockType.AIR);
        for (int x = x0 - 1; x <= x0 + 6; x++) {
            for (int z = z0 - 1; z <= z0 + 6; z++) {
                if (Chunk.isInBounds(x, y + 4, z)) {
                    chunk.setBlock(x, y + 4, z, BlockType.OAK_PLANKS);
                }
            }
        }
        chunk.setBlock(x0 + 2, y, z0, BlockType.AIR);
        chunk.setBlock(x0 + 2, y + 1, z0, BlockType.AIR);
        chunk.setBlock(x0 + 2, y + 2, z0, BlockType.GLASS_PANE);
    }

    private static void placeFarm(Chunk chunk, int x0, int y, int z0) {
        for (int x = x0; x < x0 + 4; x++) {
            for (int z = z0; z < z0 + 8; z++) {
                if (!Chunk.isInBounds(x, y - 1, z)) {
                    continue;
                }
                if (z == z0 + 3 || z == z0 + 4) {
                    chunk.setBlock(x, y - 1, z, BlockType.WATER);
                    chunk.setBlock(x, y, z, BlockType.AIR);
                } else {
                    chunk.setBlock(x, y - 1, z, BlockType.FARMLAND, 7);
                    chunk.setBlock(x, y, z, BlockType.CROPS, 7);
                }
            }
        }
    }

    private static void box(Chunk chunk, int x0, int y0, int z0, int x1, int y1, int z1,
            BlockType shell, BlockType fill) {
        for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    if (!Chunk.isInBounds(x, y, z)) {
                        continue;
                    }
                    boolean edge = x == x0 || x == x1 || z == z0 || z == z1 || y == y0 || y == y1;
                    chunk.setBlock(x, y, z, edge ? shell : fill);
                }
            }
        }
    }

    private static void placeSpawner(World world, Chunk chunk, int chunkX, int chunkZ, int x, int y, int z,
            MobDefinition mob, Random random) {
        if (!Chunk.isInBounds(x, y, z)) {
            return;
        }
        chunk.setBlock(x, y, z, BlockType.MOB_SPAWNER);
        MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(chunkX * Chunk.WIDTH + x, y,
                chunkZ * Chunk.DEPTH + z);
        spawner.setMobDefinition(mob);
        spawner.setDelay(20 + random.nextInt(120));
        spawner.clearDirty();
        world.stageGeneratedTileEntity(spawner);
    }

    private static void placeLootChest(World world, Chunk chunk, int chunkX, int chunkZ, int x, int y, int z,
            Random random, ItemType... loot) {
        if (!Chunk.isInBounds(x, y, z)) {
            return;
        }
        chunk.setBlock(x, y, z, BlockType.CHEST);
        ChestTileEntity chest = new ChestTileEntity(chunkX * Chunk.WIDTH + x, y, chunkZ * Chunk.DEPTH + z);
        ItemStack[] inventory = chest.getInventory();
        for (int i = 0; i < 3 + random.nextInt(5); i++) {
            ItemType item = loot[random.nextInt(loot.length)];
            int count = item.getMaxStackSize() == 1 ? 1 : 1 + random.nextInt(Math.min(5, item.getMaxStackSize()));
            inventory[random.nextInt(inventory.length)] = new ItemStack(item, count);
        }
        chest.clearDirty();
        world.stageGeneratedTileEntity(chest);
    }

    private static int highestSolid(Chunk chunk, int x, int z) {
        for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
            BlockType block = chunk.getBlock(x, y, z);
            if (block.isSolid() && block != BlockType.LEAVES) {
                return y;
            }
        }
        return -1;
    }

    private static int strongholdChunk(long seed, boolean xAxis) {
        Random random = new Random(seed ^ (xAxis ? 0x510006501DL : 0x570006501DL));
        int sign = random.nextBoolean() ? 1 : -1;
        return sign * (24 + random.nextInt(40));
    }

    private static Random cellRandom(long seed, int chunkX, int chunkZ, int cellSize) {
        int cellX = Math.floorDiv(chunkX, cellSize);
        int cellZ = Math.floorDiv(chunkZ, cellSize);
        return new Random(seed ^ cellX * 341873128712L ^ cellZ * 132897987541L);
    }
}
