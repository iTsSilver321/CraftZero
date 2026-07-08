package com.craftzero.world;

import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.MonsterSpawnerTileEntity;

import java.util.List;
import java.util.Random;

/**
 * Release 1.0 structure facade. The heavy lifting lives in StructurePlanner so
 * location lookup and chunk population share exactly the same deterministic
 * starts.
 */
public final class StructureGenerator {
    private final StructurePlanner planner = new StructurePlanner();

    public void generate(World world, Chunk chunk, long seed, int chunkX, int chunkZ, Dimension dimension,
            ReleaseOneWorldGenerator generator) {
        generate(world, chunk, seed, chunkX, chunkZ, dimension, generator, null);
    }

    public void generate(World world, Chunk chunk, long seed, int chunkX, int chunkZ, Dimension dimension,
            ReleaseOneWorldGenerator generator, Random placementRandom) {
        if (generator != null && !generator.shouldGenerateStructures()) {
            return;
        }
        List<StructureStart> starts = planner.startsForChunk(seed, dimension, chunkX, chunkZ, generator);
        for (StructureStart start : starts) {
            start.place(world, chunk, seed, chunkX, chunkZ, placementRandom);
        }
    }

    void advancePlacementRandom(long seed, int chunkX, int chunkZ, Dimension dimension,
            ReleaseOneWorldGenerator generator, Random placementRandom) {
        if (placementRandom == null) {
            return;
        }
        generate(null, new Chunk(chunkX, chunkZ), seed, chunkX, chunkZ, dimension, generator, placementRandom);
    }

    public StructureLocation locate(long seed, Dimension dimension, StructureType type,
            int originX, int originZ, ReleaseOneWorldGenerator generator) {
        if (generator != null && !generator.shouldGenerateStructures()) {
            return null;
        }
        return planner.locate(seed, dimension, type, originX, originZ, generator);
    }

    boolean contains(long seed, Dimension dimension, StructureType type, int blockX, int y, int blockZ,
            ReleaseOneWorldGenerator generator) {
        if (generator != null && !generator.shouldGenerateStructures()) {
            return false;
        }
        return planner.contains(seed, dimension, type, blockX, y, blockZ, generator);
    }

    boolean suppressesOverworldLakes(long seed, int chunkX, int chunkZ, ReleaseOneWorldGenerator generator) {
        if (generator != null && !generator.shouldGenerateStructures()) {
            return false;
        }
        return planner.hasVillageStartForPopulationChunk(seed, chunkX, chunkZ, generator);
    }

    static void placeSpawner(World world, Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ,
            MobDefinition mob, Random random) {
        if (!writeBlock(chunk, chunkX, chunkZ, worldX, y, worldZ, BlockType.MOB_SPAWNER, 0)) {
            return;
        }
        if (world == null) {
            return;
        }
        MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(worldX, y, worldZ);
        spawner.setMobDefinition(mob);
        spawner.clearDirty();
        world.stageGeneratedTileEntity(spawner);
    }

    static void placeLootChest(World world, Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ,
            Random random, ItemType... loot) {
        if (!writeGeneratedChest(chunk, chunkX, chunkZ, worldX, y, worldZ)) {
            return;
        }
        ChestTileEntity chest = world == null ? null : new ChestTileEntity(worldX, y, worldZ);
        ItemStack[] inventory = chest == null ? null : chest.getInventory();
        int rolls = 3 + random.nextInt(5);
        for (int i = 0; i < rolls; i++) {
            ItemType item = loot[random.nextInt(loot.length)];
            int count = item.getMaxStackSize() == 1 ? 1 : 1 + random.nextInt(Math.min(5, item.getMaxStackSize()));
            int slot = random.nextInt(ChestTileEntity.SIZE);
            if (inventory != null) {
                inventory[slot] = new ItemStack(item, count);
            }
        }
        if (chest == null) {
            return;
        }
        chest.clearDirty();
        world.stageGeneratedTileEntity(chest);
    }

    static void placeWeightedLootChest(World world, Chunk chunk, int chunkX, int chunkZ, int worldX, int y,
            int worldZ, Random random, int rolls, LootEntry... loot) {
        if (!writeGeneratedChest(chunk, chunkX, chunkZ, worldX, y, worldZ)) {
            return;
        }
        ChestTileEntity chest = world == null ? null : new ChestTileEntity(worldX, y, worldZ);
        ItemStack[] inventory = chest == null ? null : chest.getInventory();
        for (int i = 0; i < rolls; i++) {
            LootEntry entry = weightedLoot(random, loot);
            int count = entry.minCount() >= entry.maxCount()
                    ? entry.minCount()
                    : random.nextInt(entry.maxCount() - entry.minCount() + 1) + entry.minCount();
            int slot = random.nextInt(ChestTileEntity.SIZE);
            if (inventory != null) {
                inventory[slot] = new ItemStack(entry.type(), count);
            }
        }
        if (chest == null) {
            return;
        }
        chest.clearDirty();
        world.stageGeneratedTileEntity(chest);
    }

    private static LootEntry weightedLoot(Random random, LootEntry[] loot) {
        int totalWeight = 0;
        for (LootEntry entry : loot) {
            totalWeight += entry.weight();
        }
        int choice = random.nextInt(totalWeight);
        for (LootEntry entry : loot) {
            choice -= entry.weight();
            if (choice < 0) {
                return entry;
            }
        }
        return loot[loot.length - 1];
    }

    private static boolean writeGeneratedChest(Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        int minX = chunkX * Chunk.WIDTH;
        int minZ = chunkZ * Chunk.DEPTH;
        int localX = worldX - minX;
        int localZ = worldZ - minZ;
        if (!Chunk.isInBounds(localX, y, localZ) || chunk.getBlock(localX, y, localZ) == BlockType.CHEST) {
            return false;
        }
        chunk.setBlock(localX, y, localZ, BlockType.CHEST, 0);
        return true;
    }

    static boolean writeBlock(Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ, BlockType type,
            int metadata) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return false;
        }
        int minX = chunkX * Chunk.WIDTH;
        int minZ = chunkZ * Chunk.DEPTH;
        int localX = worldX - minX;
        int localZ = worldZ - minZ;
        if (!Chunk.isInBounds(localX, y, localZ)) {
            return false;
        }
        chunk.setBlock(localX, y, localZ, type, metadata);
        return true;
    }

    public record StructureLocation(StructureType type, int chunkX, int chunkZ, int blockX, int blockY, int blockZ) {
    }

    record LootEntry(ItemType type, int minCount, int maxCount, int weight) {
    }
}
