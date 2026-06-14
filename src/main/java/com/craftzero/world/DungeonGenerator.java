package com.craftzero.world;

import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.MonsterSpawnerTileEntity;

import java.util.Random;

public final class DungeonGenerator {
    private static final ItemType[] LOOT = {
            ItemType.IRON_INGOT, ItemType.WHEAT, ItemType.BREAD, ItemType.STRING, ItemType.GUNPOWDER,
            ItemType.REDSTONE, ItemType.SADDLE, ItemType.BUCKET, ItemType.BONE, ItemType.RECORD_13,
            ItemType.RECORD_CAT
    };
    private static final MobDefinition[] SPAWNER_MOBS = {
            MobDefinition.ZOMBIE, MobDefinition.SKELETON, MobDefinition.SPIDER
    };

    public void generate(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
        Random random = new Random(seed ^ 0xD06E0A11L ^ chunkX * 341873128712L ^ chunkZ * 132897987541L);
        if (random.nextInt(18) != 0) {
            return;
        }

        int width = 6 + random.nextInt(3);
        int depth = 6 + random.nextInt(3);
        int x0 = 2 + random.nextInt(Math.max(1, Chunk.WIDTH - width - 3));
        int z0 = 2 + random.nextInt(Math.max(1, Chunk.DEPTH - depth - 3));
        int y0 = 12 + random.nextInt(44);
        int x1 = x0 + width - 1;
        int z1 = z0 + depth - 1;
        int y1 = y0 + 4;

        if (!hasStoneShell(chunk, x0, y0, z0, x1, y1, z1)) {
            return;
        }

        for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    boolean shell = x == x0 || x == x1 || z == z0 || z == z1 || y == y0 || y == y1;
                    if (shell) {
                        chunk.setBlock(x, y, z, random.nextInt(4) == 0 ? BlockType.MOSSY_COBBLESTONE : BlockType.COBBLESTONE);
                    } else {
                        chunk.setBlock(x, y, z, BlockType.AIR);
                    }
                }
            }
        }

        int centerX = (x0 + x1) / 2;
        int centerZ = (z0 + z1) / 2;
        int worldX = chunkX * Chunk.WIDTH + centerX;
        int worldZ = chunkZ * Chunk.DEPTH + centerZ;
        chunk.setBlock(centerX, y0 + 1, centerZ, BlockType.MOB_SPAWNER);
        MonsterSpawnerTileEntity spawner = new MonsterSpawnerTileEntity(worldX, y0 + 1, worldZ);
        spawner.setMobDefinition(SPAWNER_MOBS[random.nextInt(SPAWNER_MOBS.length)]);
        spawner.setDelay(20 + random.nextInt(80));
        spawner.clearDirty();
        world.stageGeneratedTileEntity(spawner);

        int chestCount = random.nextInt(3);
        for (int i = 0; i < chestCount; i++) {
            placeChest(world, chunk, random, chunkX, chunkZ, x0, y0 + 1, z0, x1, z1);
        }
    }

    private static boolean hasStoneShell(Chunk chunk, int x0, int y0, int z0, int x1, int y1, int z1) {
        int solidShell = 0;
        int shellBlocks = 0;
        for (int y = y0; y <= y1; y++) {
            for (int z = z0; z <= z1; z++) {
                for (int x = x0; x <= x1; x++) {
                    boolean shell = x == x0 || x == x1 || z == z0 || z == z1 || y == y0 || y == y1;
                    if (!shell) {
                        continue;
                    }
                    shellBlocks++;
                    BlockType block = chunk.getBlock(x, y, z);
                    if (block == BlockType.STONE || block == BlockType.DIRT || block == BlockType.GRAVEL
                            || block == BlockType.COBBLESTONE || block == BlockType.MOSSY_COBBLESTONE) {
                        solidShell++;
                    }
                }
            }
        }
        return solidShell >= shellBlocks * 3 / 4;
    }

    private static void placeChest(World world, Chunk chunk, Random random, int chunkX, int chunkZ,
            int x0, int y, int z0, int x1, int z1) {
        int side = random.nextInt(4);
        int x = side < 2 ? (side == 0 ? x0 + 1 : x1 - 1) : x0 + 2 + random.nextInt(Math.max(1, x1 - x0 - 3));
        int z = side >= 2 ? (side == 2 ? z0 + 1 : z1 - 1) : z0 + 2 + random.nextInt(Math.max(1, z1 - z0 - 3));
        if (chunk.getBlock(x, y, z) != BlockType.AIR || chunk.getBlock(x, y - 1, z) == BlockType.AIR) {
            return;
        }
        chunk.setBlock(x, y, z, BlockType.CHEST);
        ChestTileEntity chest = new ChestTileEntity(chunkX * Chunk.WIDTH + x, y, chunkZ * Chunk.DEPTH + z);
        fillChest(chest, random, 4 + random.nextInt(5));
        chest.clearDirty();
        world.stageGeneratedTileEntity(chest);
    }

    static void fillChest(ChestTileEntity chest, Random random, int rolls) {
        ItemStack[] inventory = chest.getInventory();
        for (int i = 0; i < rolls; i++) {
            ItemType item = LOOT[random.nextInt(LOOT.length)];
            int count = item.getMaxStackSize() == 1 ? 1 : 1 + random.nextInt(Math.min(6, item.getMaxStackSize()));
            inventory[random.nextInt(inventory.length)] = new ItemStack(item, count);
        }
    }
}
