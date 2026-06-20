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
        List<StructureStart> starts = planner.startsForChunk(seed, dimension, chunkX, chunkZ, generator);
        for (StructureStart start : starts) {
            start.place(world, chunk, seed, chunkX, chunkZ);
        }
    }

    public StructureLocation locate(long seed, Dimension dimension, StructureType type,
            int originX, int originZ, ReleaseOneWorldGenerator generator) {
        return planner.locate(seed, dimension, type, originX, originZ, generator);
    }

    static void placeSpawner(World world, Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ,
            MobDefinition mob, Random random) {
        if (!writeBlock(chunk, chunkX, chunkZ, worldX, y, worldZ, BlockType.MOB_SPAWNER, 0)) {
            return;
        }
        MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(worldX, y, worldZ);
        spawner.setMobDefinition(mob);
        spawner.setDelay(20 + random.nextInt(120));
        spawner.clearDirty();
        world.stageGeneratedTileEntity(spawner);
    }

    static void placeLootChest(World world, Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ,
            Random random, ItemType... loot) {
        if (!writeBlock(chunk, chunkX, chunkZ, worldX, y, worldZ, BlockType.CHEST, 0)) {
            return;
        }
        ChestTileEntity chest = new ChestTileEntity(worldX, y, worldZ);
        ItemStack[] inventory = chest.getInventory();
        int rolls = 3 + random.nextInt(5);
        for (int i = 0; i < rolls; i++) {
            ItemType item = loot[random.nextInt(loot.length)];
            int count = item.getMaxStackSize() == 1 ? 1 : 1 + random.nextInt(Math.min(5, item.getMaxStackSize()));
            inventory[random.nextInt(inventory.length)] = new ItemStack(item, count);
        }
        chest.clearDirty();
        world.stageGeneratedTileEntity(chest);
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
}
