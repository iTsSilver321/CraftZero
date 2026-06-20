package com.craftzero.world;

import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.inventory.ItemType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

final class StructurePlanner {
    private static final int STRONGHOLD_COUNT = 3;
    private static final double STRONGHOLD_DISTANCE = 32.0D;
    private static final int FORTRESS_CELL = 16;

    List<StructureStart> startsForChunk(long seed, Dimension dimension, int chunkX, int chunkZ,
            ReleaseOneWorldGenerator generator) {
        ArrayList<StructureStart> starts = new ArrayList<>();
        if (dimension == Dimension.OVERWORLD) {
            for (int[] stronghold : strongholdChunks(seed, generator)) {
                StructureStart start = buildStronghold(seed, stronghold[0], stronghold[1]);
                if (start.intersectsChunk(chunkX, chunkZ)) {
                    starts.add(start);
                }
            }
            addLegacyMineshaft(seed, chunkX, chunkZ, starts);
            addLegacyVillage(seed, chunkX, chunkZ, generator, starts);
        } else if (dimension == Dimension.NETHER) {
            int minCellX = Math.floorDiv(chunkX - 8, FORTRESS_CELL);
            int maxCellX = Math.floorDiv(chunkX + 8, FORTRESS_CELL);
            int minCellZ = Math.floorDiv(chunkZ - 8, FORTRESS_CELL);
            int maxCellZ = Math.floorDiv(chunkZ + 8, FORTRESS_CELL);
            for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                    int[] origin = fortressOrigin(seed, cellX, cellZ);
                    if (origin == null) {
                        continue;
                    }
                    StructureStart start = buildNetherFortress(seed, origin[0], origin[1]);
                    if (start.intersectsChunk(chunkX, chunkZ)) {
                        starts.add(start);
                    }
                }
            }
        }
        return starts;
    }

    StructureGenerator.StructureLocation locate(long seed, Dimension dimension, StructureType type,
            int originX, int originZ, ReleaseOneWorldGenerator generator) {
        int originChunkX = Math.floorDiv(originX, Chunk.WIDTH);
        int originChunkZ = Math.floorDiv(originZ, Chunk.DEPTH);
        if (type == StructureType.STRONGHOLD && dimension == Dimension.OVERWORLD) {
            return java.util.Arrays.stream(strongholdChunks(seed, generator))
                    .map(pos -> buildStronghold(seed, pos[0], pos[1]))
                    .min(Comparator.comparingDouble(start -> distanceSq(start.bounds().centerX(), start.bounds().centerZ(),
                            originX, originZ)))
                    .map(start -> new StructureGenerator.StructureLocation(type, start.chunkX(), start.chunkZ(),
                            start.bounds().centerX(), start.bounds().centerY(), start.bounds().centerZ()))
                    .orElse(null);
        }
        if (type == StructureType.NETHER_FORTRESS && dimension == Dimension.NETHER) {
            StructureStart best = null;
            double bestDistance = Double.MAX_VALUE;
            int cellX = Math.floorDiv(originChunkX, FORTRESS_CELL);
            int cellZ = Math.floorDiv(originChunkZ, FORTRESS_CELL);
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    int[] pos = fortressOrigin(seed, cellX + dx, cellZ + dz);
                    if (pos == null) {
                        continue;
                    }
                    StructureStart start = buildNetherFortress(seed, pos[0], pos[1]);
                    double distance = distanceSq(start.bounds().centerX(), start.bounds().centerZ(), originX, originZ);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = start;
                    }
                }
            }
            return best == null ? null
                    : new StructureGenerator.StructureLocation(type, best.chunkX(), best.chunkZ(),
                            best.bounds().centerX(), best.bounds().centerY(), best.bounds().centerZ());
        }
        return null;
    }

    private int[][] strongholdChunks(long seed, ReleaseOneWorldGenerator generator) {
        int[][] result = new int[STRONGHOLD_COUNT][2];
        Random random = new Random(seed);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        int ring = 1;
        int spread = 3;
        for (int i = 0; i < STRONGHOLD_COUNT; i++) {
            double distance = (1.25D * ring + random.nextDouble()) * STRONGHOLD_DISTANCE * ring;
            int chunkX = (int) Math.round(Math.cos(angle) * distance);
            int chunkZ = (int) Math.round(Math.sin(angle) * distance);
            int[] adjusted = nearestStrongholdBiome(chunkX, chunkZ, generator);
            result[i][0] = adjusted[0];
            result[i][1] = adjusted[1];
            angle += Math.PI * 2.0D * ring / spread;
        }
        return result;
    }

    private int[] nearestStrongholdBiome(int chunkX, int chunkZ, ReleaseOneWorldGenerator generator) {
        if (generator == null) {
            return new int[] { chunkX, chunkZ };
        }
        int blockX = (chunkX << 4) + 8;
        int blockZ = (chunkZ << 4) + 8;
        if (isStrongholdBiome(generator.getBiome(blockX, blockZ))) {
            return new int[] { chunkX, chunkZ };
        }
        int bestX = chunkX;
        int bestZ = chunkZ;
        int bestDistance = Integer.MAX_VALUE;
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                int testX = chunkX + dx;
                int testZ = chunkZ + dz;
                if (!isStrongholdBiome(generator.getBiome((testX << 4) + 8, (testZ << 4) + 8))) {
                    continue;
                }
                int distance = dx * dx + dz * dz;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestX = testX;
                    bestZ = testZ;
                }
            }
        }
        return new int[] { bestX, bestZ };
    }

    private static boolean isStrongholdBiome(BiomeType biome) {
        return biome != null && !biome.isOceanic() && biome != BiomeType.RIVER && biome != BiomeType.FROZEN_RIVER
                && biome != BiomeType.BEACH;
    }

    private StructureStart buildStronghold(long seed, int chunkX, int chunkZ) {
        StructureStart start = new StructureStart(StructureType.STRONGHOLD, chunkX, chunkZ);
        Random random = new Random(seed ^ 0x51A7EF15L ^ chunkX * 341873128712L ^ chunkZ * 132897987541L);
        int x = chunkX * Chunk.WIDTH + 8;
        int z = chunkZ * Chunk.DEPTH + 8;
        int y = 24 + random.nextInt(9);

        start.addPiece(new StrongholdBoxPiece(x - 5, y, z - 5, x + 5, y + 6, z + 5, StrongholdRoom.START));
        start.addPiece(new StrongholdBoxPiece(x + 5, y + 1, z - 2, x + 30, y + 4, z + 2, StrongholdRoom.CORRIDOR));
        start.addPiece(new StrongholdBoxPiece(x + 26, y, z - 6, x + 38, y + 6, z + 6, StrongholdRoom.CROSSING));
        start.addPiece(new StrongholdBoxPiece(x + 30, y + 1, z - 34, x + 34, y + 4, z - 6, StrongholdRoom.CORRIDOR));
        start.addPiece(new StrongholdBoxPiece(x + 22, y, z - 48, x + 44, y + 7, z - 34, StrongholdRoom.LIBRARY));
        start.addPiece(new StrongholdBoxPiece(x + 38, y + 1, z - 2, x + 56, y + 4, z + 2, StrongholdRoom.CORRIDOR));
        start.addPiece(new StrongholdBoxPiece(x + 56, y - 1, z - 8, x + 72, y + 5, z + 8, StrongholdRoom.PORTAL));
        if (random.nextBoolean()) {
            start.addPiece(new StrongholdBoxPiece(x + 30, y + 1, z + 6, x + 34, y + 4, z + 30,
                    StrongholdRoom.CORRIDOR));
            start.addPiece(new StrongholdBoxPiece(x + 23, y, z + 30, x + 41, y + 5, z + 42,
                    StrongholdRoom.CHEST_CORRIDOR));
        }
        if (random.nextInt(3) != 0) {
            start.addPiece(new StrongholdBoxPiece(x - 28, y + 1, z - 2, x - 5, y + 4, z + 2,
                    StrongholdRoom.CORRIDOR));
            start.addPiece(new StrongholdBoxPiece(x - 42, y, z - 7, x - 28, y + 5, z + 7,
                    StrongholdRoom.PRISON));
        }
        return start;
    }

    private static int[] fortressOrigin(long seed, int cellX, int cellZ) {
        Random random = new Random((long) (cellX ^ cellZ << 4) ^ seed);
        random.nextInt();
        if (random.nextInt(3) != 0) {
            return null;
        }
        return new int[] {
                (cellX << 4) + 4 + random.nextInt(8),
                (cellZ << 4) + 4 + random.nextInt(8)
        };
    }

    private StructureStart buildNetherFortress(long seed, int chunkX, int chunkZ) {
        StructureStart start = new StructureStart(StructureType.NETHER_FORTRESS, chunkX, chunkZ);
        Random random = new Random(seed ^ 0xF047E55L ^ chunkX * 341873128712L ^ chunkZ * 132897987541L);
        int x = chunkX * Chunk.WIDTH + 8;
        int z = chunkZ * Chunk.DEPTH + 8;
        int y = 54 + random.nextInt(18);
        start.addPiece(new NetherFortressPiece(x - 4, y, z - 36, x + 4, y + 6, z + 36, FortressRoom.BRIDGE));
        start.addPiece(new NetherFortressPiece(x - 32, y, z - 4, x + 32, y + 6, z + 4, FortressRoom.BRIDGE));
        start.addPiece(new NetherFortressPiece(x - 8, y - 1, z - 8, x + 8, y + 7, z + 8, FortressRoom.CROSSING));
        start.addPiece(new NetherFortressPiece(x + 20, y, z - 12, x + 42, y + 8, z + 12, FortressRoom.BLAZE_PLATFORM));
        start.addPiece(new NetherFortressPiece(x - 42, y, z - 10, x - 20, y + 6, z + 10, FortressRoom.WART_ROOM));
        return start;
    }

    private void addLegacyMineshaft(long seed, int chunkX, int chunkZ, List<StructureStart> starts) {
        Random random = cellRandom(seed ^ 0x4D1AE5A7L, chunkX, chunkZ, 5);
        int cellX = Math.floorDiv(chunkX, 5);
        int cellZ = Math.floorDiv(chunkZ, 5);
        int originX = cellX * 5 + random.nextInt(5);
        int originZ = cellZ * 5 + random.nextInt(5);
        if (chunkX != originX || chunkZ != originZ || random.nextInt(3) != 0) {
            return;
        }
        StructureStart start = new StructureStart(StructureType.MINESHAFT, chunkX, chunkZ);
        int y = 22 + random.nextInt(22);
        int x = chunkX * Chunk.WIDTH + 8;
        int z = chunkZ * Chunk.DEPTH + 8;
        start.addPiece(new MineshaftPiece(x - 14, y, z - 2, x + 14, y + 3, z + 2, random.nextBoolean()));
        starts.add(start);
    }

    private void addLegacyVillage(long seed, int chunkX, int chunkZ, ReleaseOneWorldGenerator generator,
            List<StructureStart> starts) {
        Random random = cellRandom(seed ^ 0x7111A6E5L, chunkX, chunkZ, 12);
        int cellX = Math.floorDiv(chunkX, 12);
        int cellZ = Math.floorDiv(chunkZ, 12);
        int originX = cellX * 12 + random.nextInt(12);
        int originZ = cellZ * 12 + random.nextInt(12);
        if (chunkX != originX || chunkZ != originZ || random.nextInt(5) != 0 || generator == null) {
            return;
        }
        int worldCenterX = chunkX * Chunk.WIDTH + 8;
        int worldCenterZ = chunkZ * Chunk.DEPTH + 8;
        BiomeType biome = generator.getBiome(worldCenterX, worldCenterZ);
        if (biome != BiomeType.PLAINS && biome != BiomeType.DESERT && biome != BiomeType.DESERT_HILLS) {
            return;
        }
        StructureStart start = new StructureStart(StructureType.VILLAGE, chunkX, chunkZ);
        int y = Math.max(ReleaseOneWorldGenerator.SEA_LEVEL + 1, generator.terrainTopY(worldCenterX, worldCenterZ) + 1);
        start.addPiece(new VillagePiece(worldCenterX - 7, y, worldCenterZ - 7, worldCenterX + 7, y + 5,
                worldCenterZ + 7, biome == BiomeType.DESERT ? BlockType.SANDSTONE : BlockType.OAK_PLANKS));
        starts.add(start);
    }

    private static Random cellRandom(long seed, int chunkX, int chunkZ, int cellSize) {
        int cellX = Math.floorDiv(chunkX, cellSize);
        int cellZ = Math.floorDiv(chunkZ, cellSize);
        return new Random(seed ^ cellX * 341873128712L ^ cellZ * 132897987541L);
    }

    private static double distanceSq(int x, int z, int ox, int oz) {
        long dx = x - ox;
        long dz = z - oz;
        return dx * dx + dz * dz;
    }

    private enum StrongholdRoom {
        START, CORRIDOR, CROSSING, LIBRARY, CHEST_CORRIDOR, PRISON, PORTAL
    }

    private enum FortressRoom {
        BRIDGE, CROSSING, BLAZE_PLATFORM, WART_ROOM
    }

    private static class StrongholdBoxPiece extends BoxPiece {
        private final StrongholdRoom room;

        StrongholdBoxPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room) {
            super(minX, minY, minZ, maxX, maxY, maxZ);
            this.room = room;
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            Random random = random(seed, bounds().minX(), bounds().minY(), bounds().minZ());
            shell(chunk, chunkX, chunkZ, BlockType.STONE_BRICK, BlockType.AIR);
            if (room == StrongholdRoom.CORRIDOR) {
                clearEnds(chunk, chunkX, chunkZ);
                sprinkle(chunk, chunkX, chunkZ, random);
            } else if (room == StrongholdRoom.CROSSING || room == StrongholdRoom.START) {
                clearEnds(chunk, chunkX, chunkZ);
                pillar(chunk, chunkX, chunkZ, bounds().centerX(), bounds().minY() + 1, bounds().centerZ(),
                        BlockType.STONE_BRICK);
            } else if (room == StrongholdRoom.LIBRARY) {
                fillInterior(chunk, chunkX, chunkZ, BlockType.AIR);
                for (int z = bounds().minZ() + 2; z <= bounds().maxZ() - 2; z += 2) {
                    column(chunk, chunkX, chunkZ, bounds().minX() + 2, bounds().minY() + 1, z, 3, BlockType.BOOKSHELF);
                    column(chunk, chunkX, chunkZ, bounds().maxX() - 2, bounds().minY() + 1, z, 3, BlockType.BOOKSHELF);
                }
                StructureGenerator.placeLootChest(world, chunk, chunkX, chunkZ, bounds().centerX(), bounds().minY() + 1,
                        bounds().centerZ(), random, ItemType.BOOK, ItemType.PAPER, ItemType.ENDER_PEARL,
                        ItemType.IRON_INGOT, ItemType.REDSTONE);
            } else if (room == StrongholdRoom.CHEST_CORRIDOR) {
                clearEnds(chunk, chunkX, chunkZ);
                StructureGenerator.placeLootChest(world, chunk, chunkX, chunkZ, bounds().centerX(), bounds().minY() + 1,
                        bounds().centerZ(), random, ItemType.IRON_INGOT, ItemType.BREAD, ItemType.APPLE,
                        ItemType.REDSTONE, ItemType.ENDER_PEARL);
            } else if (room == StrongholdRoom.PRISON) {
                clearEnds(chunk, chunkX, chunkZ);
                for (int y = bounds().minY() + 1; y <= bounds().maxY() - 1; y++) {
                    for (int z = bounds().minZ() + 2; z <= bounds().maxZ() - 2; z += 2) {
                        set(chunk, chunkX, chunkZ, bounds().centerX(), y, z, BlockType.IRON_BARS);
                    }
                }
            } else if (room == StrongholdRoom.PORTAL) {
                buildPortalRoom(world, chunk, chunkX, chunkZ, random);
            }
        }

        private void buildPortalRoom(World world, Chunk chunk, int chunkX, int chunkZ, Random random) {
            clearEnds(chunk, chunkX, chunkZ);
            int cx = bounds().centerX();
            int cy = bounds().minY() + 2;
            int cz = bounds().centerZ();
            for (int x = cx - 1; x <= cx + 1; x++) {
                set(chunk, chunkX, chunkZ, x, cy - 1, cz - 2, BlockType.LAVA);
                set(chunk, chunkX, chunkZ, x, cy - 1, cz + 2, BlockType.LAVA);
                setFrame(chunk, chunkX, chunkZ, x, cy, cz - 2, 2, random);
                setFrame(chunk, chunkX, chunkZ, x, cy, cz + 2, 0, random);
            }
            for (int z = cz - 1; z <= cz + 1; z++) {
                set(chunk, chunkX, chunkZ, cx - 2, cy - 1, z, BlockType.LAVA);
                set(chunk, chunkX, chunkZ, cx + 2, cy - 1, z, BlockType.LAVA);
                setFrame(chunk, chunkX, chunkZ, cx - 2, cy, z, 1, random);
                setFrame(chunk, chunkX, chunkZ, cx + 2, cy, z, 3, random);
            }
            for (int x = cx - 1; x <= cx + 1; x++) {
                for (int z = cz - 1; z <= cz + 1; z++) {
                    set(chunk, chunkX, chunkZ, x, cy, z, BlockType.AIR);
                }
            }
            StructureGenerator.placeSpawner(world, chunk, chunkX, chunkZ, cx, cy, cz - 5,
                    MobDefinition.SILVERFISH, random);
        }

        private void setFrame(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z, int facing, Random random) {
            int metadata = facing | (random.nextFloat() < 0.10f ? 4 : 0);
            set(chunk, chunkX, chunkZ, x, y, z, BlockType.END_PORTAL_FRAME, metadata);
        }

        private void sprinkle(Chunk chunk, int chunkX, int chunkZ, Random random) {
            for (int x = bounds().minX(); x <= bounds().maxX(); x++) {
                for (int z = bounds().minZ(); z <= bounds().maxZ(); z++) {
                    if (random.nextInt(64) == 0) {
                        set(chunk, chunkX, chunkZ, x, bounds().minY(), z, BlockType.INFESTED_STONE);
                    }
                }
            }
        }
    }

    private static class NetherFortressPiece extends BoxPiece {
        private final FortressRoom room;

        NetherFortressPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, FortressRoom room) {
            super(minX, minY, minZ, maxX, maxY, maxZ);
            this.room = room;
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            Random random = random(seed, bounds().minX(), bounds().minY(), bounds().minZ());
            shell(chunk, chunkX, chunkZ, BlockType.NETHER_BRICK, BlockType.AIR);
            clearEnds(chunk, chunkX, chunkZ);
            if (room == FortressRoom.BRIDGE) {
                for (int x = bounds().minX(); x <= bounds().maxX(); x++) {
                    set(chunk, chunkX, chunkZ, x, bounds().minY() + 2, bounds().minZ(), BlockType.NETHER_BRICK_FENCE);
                    set(chunk, chunkX, chunkZ, x, bounds().minY() + 2, bounds().maxZ(), BlockType.NETHER_BRICK_FENCE);
                }
                for (int z = bounds().minZ(); z <= bounds().maxZ(); z++) {
                    set(chunk, chunkX, chunkZ, bounds().minX(), bounds().minY() + 2, z, BlockType.NETHER_BRICK_FENCE);
                    set(chunk, chunkX, chunkZ, bounds().maxX(), bounds().minY() + 2, z, BlockType.NETHER_BRICK_FENCE);
                }
            } else if (room == FortressRoom.BLAZE_PLATFORM) {
                StructureGenerator.placeSpawner(world, chunk, chunkX, chunkZ, bounds().centerX(), bounds().minY() + 1,
                        bounds().centerZ(), MobDefinition.BLAZE, random);
                StructureGenerator.placeLootChest(world, chunk, chunkX, chunkZ, bounds().maxX() - 3, bounds().minY() + 1,
                        bounds().centerZ(), random, ItemType.GOLD_INGOT, ItemType.GOLD_NUGGET, ItemType.SADDLE,
                        ItemType.BLAZE_ROD);
            } else if (room == FortressRoom.WART_ROOM) {
                for (int x = bounds().minX() + 4; x <= bounds().maxX() - 4; x++) {
                    set(chunk, chunkX, chunkZ, x, bounds().minY() + 1, bounds().centerZ() - 1, BlockType.SOUL_SAND);
                    set(chunk, chunkX, chunkZ, x, bounds().minY() + 2, bounds().centerZ() - 1, BlockType.NETHER_WART);
                    set(chunk, chunkX, chunkZ, x, bounds().minY() + 1, bounds().centerZ() + 1, BlockType.SOUL_SAND);
                    set(chunk, chunkX, chunkZ, x, bounds().minY() + 2, bounds().centerZ() + 1, BlockType.NETHER_WART);
                }
                StructureGenerator.placeLootChest(world, chunk, chunkX, chunkZ, bounds().centerX(), bounds().minY() + 1,
                        bounds().centerZ(), random, ItemType.NETHER_WART, ItemType.GOLD_INGOT, ItemType.IRON_INGOT);
            }
        }
    }

    private static class MineshaftPiece extends BoxPiece {
        private final boolean eastWest;

        MineshaftPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean eastWest) {
            super(minX, minY, minZ, maxX, maxY, maxZ);
            this.eastWest = eastWest;
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            Random random = random(seed, bounds().minX(), bounds().minY(), bounds().minZ());
            for (int x = bounds().minX(); x <= bounds().maxX(); x++) {
                for (int z = bounds().minZ(); z <= bounds().maxZ(); z++) {
                    set(chunk, chunkX, chunkZ, x, bounds().minY(), z, BlockType.OAK_PLANKS);
                    set(chunk, chunkX, chunkZ, x, bounds().minY() + 1, z, BlockType.AIR);
                    set(chunk, chunkX, chunkZ, x, bounds().minY() + 2, z, BlockType.AIR);
                    if ((eastWest ? x : z) % 5 == 0) {
                        set(chunk, chunkX, chunkZ, x, bounds().minY() + 1, z, BlockType.FENCE);
                        set(chunk, chunkX, chunkZ, x, bounds().minY() + 2, z, BlockType.OAK_PLANKS);
                    } else if (random.nextInt(14) == 0) {
                        set(chunk, chunkX, chunkZ, x, bounds().minY() + 2, z, BlockType.COBWEB);
                    } else {
                        set(chunk, chunkX, chunkZ, x, bounds().minY() + 1, z, BlockType.RAIL);
                    }
                }
            }
            if (random.nextBoolean()) {
                StructureGenerator.placeSpawner(world, chunk, chunkX, chunkZ, bounds().centerX(), bounds().minY() + 1,
                        bounds().centerZ(), MobDefinition.CAVE_SPIDER, random);
            }
        }
    }

    private static class VillagePiece extends BoxPiece {
        private final BlockType wall;

        VillagePiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockType wall) {
            super(minX, minY, minZ, maxX, maxY, maxZ);
            this.wall = wall;
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            shell(chunk, chunkX, chunkZ, wall, BlockType.AIR);
            for (int x = bounds().minX(); x <= bounds().maxX(); x++) {
                set(chunk, chunkX, chunkZ, x, bounds().minY() - 1, bounds().centerZ(), BlockType.GRAVEL);
            }
            for (int z = bounds().minZ(); z <= bounds().maxZ(); z++) {
                set(chunk, chunkX, chunkZ, bounds().centerX(), bounds().minY() - 1, z, BlockType.GRAVEL);
            }
            set(chunk, chunkX, chunkZ, bounds().centerX(), bounds().minY() + 1, bounds().minZ(), BlockType.AIR);
            set(chunk, chunkX, chunkZ, bounds().centerX(), bounds().minY() + 2, bounds().minZ(), BlockType.AIR);
        }
    }

    private abstract static class BoxPiece implements StructurePiece {
        private final StructureBoundingBox bounds;

        BoxPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.bounds = new StructureBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        }

        @Override
        public StructureBoundingBox bounds() {
            return bounds;
        }

        protected void shell(Chunk chunk, int chunkX, int chunkZ, BlockType shell, BlockType fill) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                        boolean edge = x == bounds.minX() || x == bounds.maxX()
                                || z == bounds.minZ() || z == bounds.maxZ()
                                || y == bounds.minY() || y == bounds.maxY();
                        set(chunk, chunkX, chunkZ, x, y, z, edge ? shell : fill);
                    }
                }
            }
        }

        protected void fillInterior(Chunk chunk, int chunkX, int chunkZ, BlockType block) {
            for (int y = bounds.minY() + 1; y <= bounds.maxY() - 1; y++) {
                for (int z = bounds.minZ() + 1; z <= bounds.maxZ() - 1; z++) {
                    for (int x = bounds.minX() + 1; x <= bounds.maxX() - 1; x++) {
                        set(chunk, chunkX, chunkZ, x, y, z, block);
                    }
                }
            }
        }

        protected void clearEnds(Chunk chunk, int chunkX, int chunkZ) {
            int cy = bounds.centerY();
            int cz = bounds.centerZ();
            int cx = bounds.centerX();
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    set(chunk, chunkX, chunkZ, bounds.minX(), cy + dy, cz + dz, BlockType.AIR);
                    set(chunk, chunkX, chunkZ, bounds.maxX(), cy + dy, cz + dz, BlockType.AIR);
                }
                for (int dx = -1; dx <= 1; dx++) {
                    set(chunk, chunkX, chunkZ, cx + dx, cy + dy, bounds.minZ(), BlockType.AIR);
                    set(chunk, chunkX, chunkZ, cx + dx, cy + dy, bounds.maxZ(), BlockType.AIR);
                }
            }
        }

        protected void column(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z, int height, BlockType block) {
            for (int dy = 0; dy < height; dy++) {
                set(chunk, chunkX, chunkZ, x, y + dy, z, block);
            }
        }

        protected void pillar(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z, BlockType block) {
            column(chunk, chunkX, chunkZ, x, y, z, Math.max(1, bounds.maxY() - bounds.minY() - 1), block);
        }

        protected void set(Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ, BlockType type) {
            StructureGenerator.writeBlock(chunk, chunkX, chunkZ, worldX, y, worldZ, type, 0);
        }

        protected void set(Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ, BlockType type,
                int metadata) {
            StructureGenerator.writeBlock(chunk, chunkX, chunkZ, worldX, y, worldZ, type, metadata);
        }

        protected static Random random(long seed, int x, int y, int z) {
            return new Random(seed ^ (long) x * 341873128712L ^ (long) y * 42317861L ^ (long) z * 132897987541L);
        }
    }
}
