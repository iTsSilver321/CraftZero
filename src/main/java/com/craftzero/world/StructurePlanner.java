package com.craftzero.world;

import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobFactory;
import com.craftzero.entity.mob.Villager;
import com.craftzero.inventory.ItemType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

final class StructurePlanner {
    private static final int STRONGHOLD_COUNT = 3;
    private static final double STRONGHOLD_DISTANCE = 32.0D;
    private static final int MAX_STRUCTURE_CACHE_ENTRIES = 32768;
    private static final int STRONGHOLD_BIOME_RADIUS = 112;
    private static final int STRUCTURE_RANGE = 8;
    private static final int FORTRESS_CELL = 16;
    private static final int FORTRESS_MIN_Y = 48;
    private static final int FORTRESS_MAX_Y = 70;
    private static final int FORTRESS_START_SIZE = 19;
    private static final int FORTRESS_START_HEIGHT = 10;
    private static final int FORTRESS_SOURCE_Y = 64;
    private static final int FORTRESS_MAX_DEPTH = 30;
    private static final int FORTRESS_MAX_BRANCH_DISTANCE = 112;
    private static final int VILLAGE_SPACING = 32;
    private static final int VILLAGE_SEPARATION = 8;
    private static final int VILLAGE_SALT = 0x9e7f70;
    private static final int VILLAGE_WELL_SOURCE_MIN_Y = 64;
    private static final int VILLAGE_WELL_SOURCE_MAX_Y = 78;
    private static final int SOURCE_STRUCTURE_WORLD_OCEAN_HEIGHT = ReleaseOneWorldGenerator.SEA_LEVEL;
    private static final int SOURCE_VILLAGE_FOUNDATION_MIN_Y = ReleaseOneWorldGenerator.SEA_LEVEL + 1;
    private static final int MARK_AVAILABLE_HEIGHT_MARGIN = 10;
    private static final int LOCATE_OVERWORLD_STRUCTURE_CHUNK_RADIUS = 128;
    private static final int MINESHAFT_MAX_DEPTH = 8;
    private static final int MINESHAFT_MAX_BRANCH_DISTANCE = 80;
    private static final int STRONGHOLD_BRANCH_MAX_DEPTH = 50;
    private static final int STRONGHOLD_BRANCH_MAX_DISTANCE = 112;
    private static final StructureGenerator.LootEntry[] STRONGHOLD_CHEST_CORRIDOR_LOOT = {
            new StructureGenerator.LootEntry(ItemType.ENDER_PEARL, 1, 1, 10),
            new StructureGenerator.LootEntry(ItemType.DIAMOND, 1, 3, 3),
            new StructureGenerator.LootEntry(ItemType.IRON_INGOT, 1, 5, 10),
            new StructureGenerator.LootEntry(ItemType.GOLD_INGOT, 1, 3, 5),
            new StructureGenerator.LootEntry(ItemType.REDSTONE, 4, 9, 5),
            new StructureGenerator.LootEntry(ItemType.BREAD, 1, 3, 15),
            new StructureGenerator.LootEntry(ItemType.APPLE, 1, 3, 15),
            new StructureGenerator.LootEntry(ItemType.IRON_PICKAXE, 1, 1, 5),
            new StructureGenerator.LootEntry(ItemType.IRON_SWORD, 1, 1, 5),
            new StructureGenerator.LootEntry(ItemType.IRON_CHESTPLATE, 1, 1, 5),
            new StructureGenerator.LootEntry(ItemType.IRON_HELMET, 1, 1, 5),
            new StructureGenerator.LootEntry(ItemType.IRON_LEGGINGS, 1, 1, 5),
            new StructureGenerator.LootEntry(ItemType.IRON_BOOTS, 1, 1, 5),
            new StructureGenerator.LootEntry(ItemType.GOLDEN_APPLE, 1, 1, 1)
    };
    private static final StructureGenerator.LootEntry[] STRONGHOLD_LIBRARY_LOOT = {
            new StructureGenerator.LootEntry(ItemType.BOOK, 1, 3, 20),
            new StructureGenerator.LootEntry(ItemType.PAPER, 2, 7, 20),
            new StructureGenerator.LootEntry(ItemType.MAP, 1, 1, 1),
            new StructureGenerator.LootEntry(ItemType.COMPASS, 1, 1, 1)
    };
    private static final StructureGenerator.LootEntry[] STRONGHOLD_ROOM_CROSSING_LOOT = {
            new StructureGenerator.LootEntry(ItemType.IRON_INGOT, 1, 5, 10),
            new StructureGenerator.LootEntry(ItemType.GOLD_INGOT, 1, 3, 5),
            new StructureGenerator.LootEntry(ItemType.REDSTONE, 4, 9, 5),
            new StructureGenerator.LootEntry(ItemType.COAL, 3, 8, 10),
            new StructureGenerator.LootEntry(ItemType.BREAD, 1, 3, 15),
            new StructureGenerator.LootEntry(ItemType.APPLE, 1, 3, 15),
            new StructureGenerator.LootEntry(ItemType.IRON_PICKAXE, 1, 1, 1)
    };
    private static final StructureGenerator.LootEntry[] MINESHAFT_CORRIDOR_LOOT = {
            new StructureGenerator.LootEntry(ItemType.IRON_INGOT, 1, 5, 10),
            new StructureGenerator.LootEntry(ItemType.GOLD_INGOT, 1, 3, 5),
            new StructureGenerator.LootEntry(ItemType.REDSTONE, 4, 9, 5),
            new StructureGenerator.LootEntry(ItemType.INK_SAC, 4, 9, 5),
            new StructureGenerator.LootEntry(ItemType.DIAMOND, 1, 2, 3),
            new StructureGenerator.LootEntry(ItemType.COAL, 3, 8, 10),
            new StructureGenerator.LootEntry(ItemType.BREAD, 1, 3, 15),
            new StructureGenerator.LootEntry(ItemType.IRON_PICKAXE, 1, 1, 1),
            new StructureGenerator.LootEntry(ItemType.RAIL, 4, 8, 1),
            new StructureGenerator.LootEntry(ItemType.MELON_SEEDS, 2, 4, 10),
            new StructureGenerator.LootEntry(ItemType.PUMPKIN_SEEDS, 2, 4, 10)
    };
    private final Map<StartsForChunkKey, List<StructureStart>> startsForChunkCache = new ConcurrentHashMap<>();
    private final Map<GeneratedStartKey, Optional<StructureStart>> generatedStartCache = new ConcurrentHashMap<>();
    private final Map<StrongholdChunksKey, int[][]> strongholdChunksCache = new ConcurrentHashMap<>();

    List<StructureStart> startsForChunk(long seed, Dimension dimension, int chunkX, int chunkZ,
            ReleaseOneWorldGenerator generator) {
        StartsForChunkKey key = new StartsForChunkKey(seed, dimension, chunkX, chunkZ, generatorKey(generator));
        List<StructureStart> cached = startsForChunkCache.get(key);
        if (cached != null) {
            return cached;
        }
        List<StructureStart> starts = List.copyOf(buildStartsForChunk(seed, dimension, chunkX, chunkZ, generator));
        clearCacheIfNeeded(startsForChunkCache);
        List<StructureStart> previous = startsForChunkCache.putIfAbsent(key, starts);
        return previous == null ? starts : previous;
    }

    private List<StructureStart> buildStartsForChunk(long seed, Dimension dimension, int chunkX, int chunkZ,
            ReleaseOneWorldGenerator generator) {
        ArrayList<StructureStart> starts = new ArrayList<>();
        if (dimension == Dimension.OVERWORLD) {
            for (int originX = chunkX - STRUCTURE_RANGE; originX <= chunkX + STRUCTURE_RANGE; originX++) {
                for (int originZ = chunkZ - STRUCTURE_RANGE; originZ <= chunkZ + STRUCTURE_RANGE; originZ++) {
                    addLegacyMineshaft(seed, originX, originZ, chunkX, chunkZ, starts);
                }
            }
            for (int originX = chunkX - STRUCTURE_RANGE; originX <= chunkX + STRUCTURE_RANGE; originX++) {
                for (int originZ = chunkZ - STRUCTURE_RANGE; originZ <= chunkZ + STRUCTURE_RANGE; originZ++) {
                    addLegacyVillage(seed, originX, originZ, chunkX, chunkZ, generator, starts);
                }
            }
            for (int[] stronghold : strongholdChunks(seed, generator)) {
                StructureStart start = cachedGeneratedStart(seed, StructureType.STRONGHOLD,
                        stronghold[0], stronghold[1], generator);
                if (start.intersectsChunk(chunkX, chunkZ)) {
                    starts.add(start);
                }
            }
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
                    StructureStart start = cachedGeneratedStart(seed, StructureType.NETHER_FORTRESS,
                            origin[0], origin[1], generator);
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
                    .map(pos -> cachedGeneratedStart(seed, StructureType.STRONGHOLD, pos[0], pos[1], generator))
                    .min(Comparator.comparingDouble(start -> {
                        StructureBoundingBox center = strongholdLocatorBox(start);
                        return distanceSq(center.centerX(), center.centerZ(), originX, originZ);
                    }))
                    .map(start -> {
                        StructureBoundingBox center = strongholdLocatorBox(start);
                        return new StructureGenerator.StructureLocation(type, start.chunkX(), start.chunkZ(),
                                center.centerX(), center.centerY(), center.centerZ());
                    })
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
                    StructureStart start = cachedGeneratedStart(seed, StructureType.NETHER_FORTRESS,
                            pos[0], pos[1], generator);
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
        if (dimension == Dimension.OVERWORLD
                && (type == StructureType.VILLAGE || type == StructureType.MINESHAFT)) {
            return locateOverworldGeneratedStart(seed, type, originChunkX, originChunkZ, originX, originZ,
                    generator);
        }
        return null;
    }

    private StructureGenerator.StructureLocation locateOverworldGeneratedStart(long seed, StructureType type,
            int originChunkX, int originChunkZ, int originX, int originZ, ReleaseOneWorldGenerator generator) {
        StructureStart best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int chunkX = originChunkX - LOCATE_OVERWORLD_STRUCTURE_CHUNK_RADIUS;
                chunkX <= originChunkX + LOCATE_OVERWORLD_STRUCTURE_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = originChunkZ - LOCATE_OVERWORLD_STRUCTURE_CHUNK_RADIUS;
                    chunkZ <= originChunkZ + LOCATE_OVERWORLD_STRUCTURE_CHUNK_RADIUS; chunkZ++) {
                StructureStart start = cachedGeneratedStart(seed, type, chunkX, chunkZ, generator);
                if (start == null) {
                    continue;
                }
                StructureBoundingBox bounds = start.bounds();
                double distance = distanceSq(bounds.centerX(), bounds.centerZ(), originX, originZ);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = start;
                }
            }
        }
        if (best == null) {
            return null;
        }
        StructureBoundingBox bounds = best.bounds();
        return new StructureGenerator.StructureLocation(type, best.chunkX(), best.chunkZ(),
                bounds.centerX(), bounds.centerY(), bounds.centerZ());
    }

    private StructureStart cachedGeneratedStart(long seed, StructureType type, int chunkX, int chunkZ,
            ReleaseOneWorldGenerator generator) {
        GeneratedStartKey key = new GeneratedStartKey(seed, type, chunkX, chunkZ, generatorKey(generator));
        Optional<StructureStart> cached = generatedStartCache.get(key);
        if (cached != null) {
            return cached.orElse(null);
        }
        StructureStart start = buildGeneratedStart(seed, type, chunkX, chunkZ, generator);
        clearCacheIfNeeded(generatedStartCache);
        Optional<StructureStart> previous = generatedStartCache.putIfAbsent(key, Optional.ofNullable(start));
        return previous == null ? start : previous.orElse(null);
    }

    private StructureStart buildGeneratedStart(long seed, StructureType type, int chunkX, int chunkZ,
            ReleaseOneWorldGenerator generator) {
        return switch (type) {
            case STRONGHOLD -> buildStronghold(seed, chunkX, chunkZ);
            case NETHER_FORTRESS -> buildNetherFortress(seed, chunkX, chunkZ);
            case VILLAGE -> buildVillageStart(seed, chunkX, chunkZ, generator);
            case MINESHAFT -> buildMineshaftStart(seed, chunkX, chunkZ);
            default -> null;
        };
    }

    private static StructureBoundingBox strongholdLocatorBox(StructureStart start) {
        for (StructurePiece piece : start.pieces()) {
            if (piece instanceof StrongholdBoxPiece strongholdPiece && strongholdPiece.room == StrongholdRoom.PORTAL) {
                return strongholdPiece.bounds();
            }
        }
        return start.bounds();
    }

    boolean hasVillageStartForPopulationChunk(long seed, int chunkX, int chunkZ,
            ReleaseOneWorldGenerator generator) {
        return cachedGeneratedStart(seed, StructureType.VILLAGE, chunkX, chunkZ, generator) != null;
    }

    boolean contains(long seed, Dimension dimension, StructureType type, int blockX, int y, int blockZ,
            ReleaseOneWorldGenerator generator) {
        int chunkX = Math.floorDiv(blockX, Chunk.WIDTH);
        int chunkZ = Math.floorDiv(blockZ, Chunk.DEPTH);
        for (StructureStart start : startsForChunk(seed, dimension, chunkX, chunkZ, generator)) {
            if (start.type() != type || !containsXZ(start.bounds(), blockX, blockZ)) {
                continue;
            }
            for (StructurePiece piece : start.pieces()) {
                if (pieceContains(piece, blockX, y, blockZ)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean containsXZ(StructureBoundingBox box, int blockX, int blockZ) {
        return box != null && blockX >= box.minX() && blockX <= box.maxX()
                && blockZ >= box.minZ() && blockZ <= box.maxZ();
    }

    private static boolean pieceContains(StructurePiece piece, int blockX, int y, int blockZ) {
        if (piece instanceof VillageWellPiece well) {
            return well.placementBounds().contains(blockX, y, blockZ);
        }
        if (piece instanceof VillageOrientedPiece village) {
            return village.placementBounds().contains(blockX, y, blockZ);
        }
        return piece.bounds().contains(blockX, y, blockZ);
    }

    private static int generatorKey(ReleaseOneWorldGenerator generator) {
        return generator == null ? 0 : System.identityHashCode(generator);
    }

    private static <K, V> void clearCacheIfNeeded(Map<K, V> cache) {
        if (cache.size() > MAX_STRUCTURE_CACHE_ENTRIES) {
            cache.clear();
        }
    }

    private record StartsForChunkKey(long seed, Dimension dimension, int chunkX, int chunkZ, int generatorKey) {
    }

    private record GeneratedStartKey(long seed, StructureType type, int chunkX, int chunkZ, int generatorKey) {
    }

    private record StrongholdChunksKey(long seed, int generatorKey) {
    }

    private int[][] strongholdChunks(long seed, ReleaseOneWorldGenerator generator) {
        StrongholdChunksKey key = new StrongholdChunksKey(seed, generatorKey(generator));
        int[][] cached = strongholdChunksCache.get(key);
        if (cached != null) {
            return cached;
        }
        int[][] result = buildStrongholdChunks(seed, generator);
        clearCacheIfNeeded(strongholdChunksCache);
        int[][] previous = strongholdChunksCache.putIfAbsent(key, result);
        return previous == null ? result : previous;
    }

    private int[][] buildStrongholdChunks(long seed, ReleaseOneWorldGenerator generator) {
        int[][] result = new int[STRONGHOLD_COUNT][2];
        Random random = new Random(seed);
        double angle = random.nextDouble() * Math.PI * 2.0D;
        for (int i = 0; i < STRONGHOLD_COUNT; i++) {
            double distance = (1.25D + random.nextDouble()) * STRONGHOLD_DISTANCE;
            int chunkX = (int) Math.round(Math.cos(angle) * distance);
            int chunkZ = (int) Math.round(Math.sin(angle) * distance);
            int[] adjusted = findStrongholdBiomePosition(chunkX, chunkZ, generator, random);
            result[i][0] = adjusted[0];
            result[i][1] = adjusted[1];
            angle += Math.PI * 2.0D / STRONGHOLD_COUNT;
        }
        return result;
    }

    private int[] findStrongholdBiomePosition(int chunkX, int chunkZ, ReleaseOneWorldGenerator generator,
            Random random) {
        if (generator == null) {
            return new int[] { chunkX, chunkZ };
        }
        int centerX = (chunkX << 4) + 8;
        int centerZ = (chunkZ << 4) + 8;
        int minLayerX = (centerX - STRONGHOLD_BIOME_RADIUS) >> 2;
        int minLayerZ = (centerZ - STRONGHOLD_BIOME_RADIUS) >> 2;
        int maxLayerX = (centerX + STRONGHOLD_BIOME_RADIUS) >> 2;
        int maxLayerZ = (centerZ + STRONGHOLD_BIOME_RADIUS) >> 2;
        int width = maxLayerX - minLayerX + 1;
        int height = maxLayerZ - minLayerZ + 1;
        int selectedBlockX = centerX;
        int selectedBlockZ = centerZ;
        int found = 0;
        boolean hasSelection = false;

        for (int index = 0; index < width * height; index++) {
            int layerX = minLayerX + index % width;
            int layerZ = minLayerZ + index / width;
            if (!isStrongholdBiome(generator.getBiomeForGenerationLayer(layerX, layerZ))) {
                continue;
            }
            if (!hasSelection || random.nextInt(found + 1) == 0) {
                selectedBlockX = layerX << 2;
                selectedBlockZ = layerZ << 2;
                hasSelection = true;
            }
            found++;
        }

        if (hasSelection) {
            return new int[] { selectedBlockX >> 4, selectedBlockZ >> 4 };
        }
        return new int[] { chunkX, chunkZ };
    }

    private static boolean isStrongholdBiome(BiomeType biome) {
        if (biome == null) {
            return false;
        }
        return switch (biome) {
            case DESERT, FOREST, EXTREME_HILLS, SWAMPLAND, TAIGA,
                    ICE_PLAINS, ICE_MOUNTAINS -> true;
            default -> false;
        };
    }

    private StructureStart buildStronghold(long seed, int chunkX, int chunkZ) {
        Random random = mapGenStructureRandom(seed, chunkX, chunkZ);
        random.nextInt();
        for (int attempt = 0; attempt < 1024; attempt++) {
            StructureStart start = buildStrongholdAttempt(random, chunkX, chunkZ);
            if (hasStrongholdPortalRoom(start)) {
                return start;
            }
        }
        throw new IllegalStateException("Unable to generate a stronghold portal room for chunk "
                + chunkX + "," + chunkZ);
    }

    private static StructureStart buildStrongholdAttempt(Random random, int chunkX, int chunkZ) {
        StructureStart start = new StructureStart(StructureType.STRONGHOLD, chunkX, chunkZ);
        int x = (chunkX << 4) + 2;
        int z = (chunkZ << 4) + 2;
        int y = 64;
        int rootMode = random.nextInt(4);

        ArrayList<StrongholdBoxPiece> pieces = new ArrayList<>();
        StructureBoundingBox rootBox = new StructureBoundingBox(x, y, z, x + 4, y + 10, z + 4);
        StrongholdBoxPiece root = orientedStrongholdPiece(x, y, z, rootMode, 0, 0, 0, 4, 10, 4,
                StrongholdRoom.START, 3);
        pieces.add(root);

        ArrayList<StrongholdBranchNode> pending = new ArrayList<>();
        StrongholdBoxPiece firstCrossing = createGeneratedStrongholdComponent(StrongholdRoom.CROSSING_HALL,
                pieces, random, strongholdNormalAccess(root, 1, 1), 1);
        if (firstCrossing != null) {
            firstCrossing = firstCrossing.withoutStrongholdWeightCount();
            pieces.add(firstCrossing);
            pending.add(new StrongholdBranchNode(firstCrossing, 1));
        }

        StrongholdGenerationContext context = new StrongholdGenerationContext(rootBox, pieces);
        while (!pending.isEmpty()) {
            StrongholdBranchNode node = pending.remove(random.nextInt(pending.size()));
            addStrongholdBranchChildren(context, pieces, pending, node.piece(), random, node.depth());
        }

        int yOffset = strongholdAvailableHeightOffset(random, pieces);
        for (StrongholdBoxPiece piece : pieces) {
            start.addPiece(piece.offsetY(yOffset));
        }
        return start;
    }

    private static boolean hasStrongholdPortalRoom(StructureStart start) {
        for (StructurePiece piece : start.pieces()) {
            if (piece instanceof StrongholdBoxPiece strongholdPiece
                    && strongholdPiece.room == StrongholdRoom.PORTAL) {
                return true;
            }
        }
        return false;
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
        Random random = fortressStartRandom(seed, chunkX, chunkZ);
        int startMode = random.nextInt(4);
        int x = (chunkX << 4) + 2;
        int z = (chunkZ << 4) + 2;

        ArrayList<NetherFortressPiece> pieces = new ArrayList<>();
        NetherFortressPiece startPiece = new NetherFortressPiece(x, FORTRESS_SOURCE_Y, z,
                x + FORTRESS_START_SIZE - 1, FORTRESS_SOURCE_Y + FORTRESS_START_HEIGHT - 1,
                z + FORTRESS_START_SIZE - 1, FortressRoom.CROSSING, startMode, 0);
        pieces.add(startPiece);

        FortressGenerationContext context = new FortressGenerationContext(startPiece.bounds());
        buildFortressPiece(startPiece, context, pieces, random);
        while (!context.pending.isEmpty()) {
            NetherFortressPiece piece = context.pending.remove(random.nextInt(context.pending.size()));
            buildFortressPiece(piece, context, pieces, random);
        }

        int yOffset = randomFortressYOffset(random, pieces);
        for (NetherFortressPiece piece : pieces) {
            start.addPiece(piece.offsetY(yOffset));
        }
        return start;
    }

    private static void buildFortressPiece(NetherFortressPiece piece, FortressGenerationContext context,
            List<NetherFortressPiece> pieces, Random random) {
        switch (piece.room) {
            case CROSSING -> {
                getNextFortressComponentNormal(piece, context, pieces, random, 8, 3, false);
                getNextFortressComponentX(piece, context, pieces, random, 3, 8, false);
                getNextFortressComponentZ(piece, context, pieces, random, 3, 8, false);
            }
            case BRIDGE -> getNextFortressComponentNormal(piece, context, pieces, random, 1, 3, false);
            case SMALL_CROSSING -> {
                getNextFortressComponentNormal(piece, context, pieces, random, 2, 0, false);
                getNextFortressComponentX(piece, context, pieces, random, 0, 2, false);
                getNextFortressComponentZ(piece, context, pieces, random, 0, 2, false);
            }
            case STAIRS -> getNextFortressComponentZ(piece, context, pieces, random, 6, 2, false);
            case ENTRANCE -> getNextFortressComponentNormal(piece, context, pieces, random, 5, 3, true);
            case CORRIDOR_5 -> getNextFortressComponentNormal(piece, context, pieces, random, 1, 0, true);
            case CROSSING_2 -> {
                getNextFortressComponentNormal(piece, context, pieces, random, 1, 0, true);
                getNextFortressComponentX(piece, context, pieces, random, 0, 1, true);
                getNextFortressComponentZ(piece, context, pieces, random, 0, 1, true);
            }
            case CORRIDOR_2 -> getNextFortressComponentZ(piece, context, pieces, random, 0, 1, true);
            case CORRIDOR -> getNextFortressComponentX(piece, context, pieces, random, 0, 1, true);
            case CORRIDOR_3 -> getNextFortressComponentNormal(piece, context, pieces, random, 1, 0, true);
            case CORRIDOR_4 -> {
                int sideOffset = piece.coordBaseMode == 1 || piece.coordBaseMode == 2 ? 5 : 1;
                getNextFortressComponentX(piece, context, pieces, random, 0, sideOffset, random.nextInt(8) > 0);
                getNextFortressComponentZ(piece, context, pieces, random, 0, sideOffset, random.nextInt(8) > 0);
            }
            case WART_ROOM -> {
                getNextFortressComponentNormal(piece, context, pieces, random, 5, 3, true);
                getNextFortressComponentNormal(piece, context, pieces, random, 5, 11, true);
            }
            case BLAZE_PLATFORM, END -> {
            }
        }
    }

    private static void getNextFortressComponentNormal(NetherFortressPiece piece, FortressGenerationContext context,
            List<NetherFortressPiece> pieces, Random random, int xOffset, int yOffset, boolean secondary) {
        switch (piece.coordBaseMode) {
            case 2 -> getNextFortressComponent(piece, context, pieces, random, piece.bounds().minX() + xOffset,
                    piece.bounds().minY() + yOffset, piece.bounds().minZ() - 1, piece.coordBaseMode, secondary);
            case 1 -> getNextFortressComponent(piece, context, pieces, random, piece.bounds().minX() - 1,
                    piece.bounds().minY() + yOffset, piece.bounds().minZ() + xOffset, piece.coordBaseMode, secondary);
            case 3 -> getNextFortressComponent(piece, context, pieces, random, piece.bounds().maxX() + 1,
                    piece.bounds().minY() + yOffset, piece.bounds().minZ() + xOffset, piece.coordBaseMode, secondary);
            default -> getNextFortressComponent(piece, context, pieces, random, piece.bounds().minX() + xOffset,
                    piece.bounds().minY() + yOffset, piece.bounds().maxZ() + 1, piece.coordBaseMode, secondary);
        }
    }

    private static void getNextFortressComponentX(NetherFortressPiece piece, FortressGenerationContext context,
            List<NetherFortressPiece> pieces, Random random, int yOffset, int zOffset, boolean secondary) {
        switch (piece.coordBaseMode) {
            case 1, 3 -> getNextFortressComponent(piece, context, pieces, random, piece.bounds().minX() + zOffset,
                    piece.bounds().minY() + yOffset, piece.bounds().minZ() - 1, 2, secondary);
            default -> getNextFortressComponent(piece, context, pieces, random, piece.bounds().minX() - 1,
                    piece.bounds().minY() + yOffset, piece.bounds().minZ() + zOffset, 1, secondary);
        }
    }

    private static void getNextFortressComponentZ(NetherFortressPiece piece, FortressGenerationContext context,
            List<NetherFortressPiece> pieces, Random random, int yOffset, int xOffset, boolean secondary) {
        switch (piece.coordBaseMode) {
            case 1, 3 -> getNextFortressComponent(piece, context, pieces, random, piece.bounds().minX() + xOffset,
                    piece.bounds().minY() + yOffset, piece.bounds().maxZ() + 1, 0, secondary);
            default -> getNextFortressComponent(piece, context, pieces, random, piece.bounds().maxX() + 1,
                    piece.bounds().minY() + yOffset, piece.bounds().minZ() + xOffset, 3, secondary);
        }
    }

    private static void getNextFortressComponent(NetherFortressPiece parent, FortressGenerationContext context,
            List<NetherFortressPiece> pieces, Random random, int x, int y, int z, int mode, boolean secondary) {
        NetherFortressPiece child;
        int parentDepth = parent.componentType;
        int depth = parentDepth + 1;
        if (Math.abs(x - context.startBox.minX()) > FORTRESS_MAX_BRANCH_DISTANCE
                || Math.abs(z - context.startBox.minZ()) > FORTRESS_MAX_BRANCH_DISTANCE) {
            child = createFortressEnd(pieces, random, x, y, z, mode, parentDepth);
        } else {
            child = chooseFortressComponent(context, secondary ? context.secondaryWeights : context.primaryWeights,
                    pieces, random, x, y, z, mode, depth);
        }
        if (child != null) {
            pieces.add(child);
            context.pending.add(child);
        }
    }

    private static NetherFortressPiece chooseFortressComponent(FortressGenerationContext context,
            List<FortressPieceWeight> weights, List<NetherFortressPiece> pieces, Random random,
            int x, int y, int z, int mode, int depth) {
        int totalWeight = totalFortressWeight(weights);
        boolean canChoose = totalWeight > 0 && depth <= FORTRESS_MAX_DEPTH;
        for (int attempt = 0; attempt < 5 && canChoose; attempt++) {
            int selected = random.nextInt(totalWeight);
            for (int index = 0; index < weights.size(); index++) {
                FortressPieceWeight weight = weights.get(index);
                selected -= weight.weight;
                if (selected >= 0) {
                    continue;
                }
                if (!weight.canSpawn() || weight == context.lastWeight && !weight.allowRepeat) {
                    break;
                }
                NetherFortressPiece piece = createFortressPiece(weight.room, pieces, random, x, y, z, mode, depth);
                if (piece == null) {
                    continue;
                }
                weight.used++;
                context.lastWeight = weight;
                if (!weight.canContinue()) {
                    weights.remove(index);
                }
                return piece;
            }
        }
        return createFortressEnd(pieces, random, x, y, z, mode, depth);
    }

    private static int totalFortressWeight(List<FortressPieceWeight> weights) {
        boolean hasLimitedAvailable = false;
        int total = 0;
        for (FortressPieceWeight weight : weights) {
            if (weight.maxCount > 0 && weight.used < weight.maxCount) {
                hasLimitedAvailable = true;
            }
            total += weight.weight;
        }
        return hasLimitedAvailable ? total : -1;
    }

    private static NetherFortressPiece createFortressPiece(FortressRoom room, List<NetherFortressPiece> pieces,
            Random random, int x, int y, int z, int mode, int depth) {
        StructureBoundingBox box = switch (room) {
            case BRIDGE -> componentBoundingBox(x, y, z, -1, -3, 0, 5, 10, 19, mode);
            case CROSSING -> componentBoundingBox(x, y, z, -8, -3, 0, 19, 10, 19, mode);
            case SMALL_CROSSING -> componentBoundingBox(x, y, z, -2, 0, 0, 7, 9, 7, mode);
            case STAIRS -> componentBoundingBox(x, y, z, -2, 0, 0, 7, 11, 7, mode);
            case BLAZE_PLATFORM -> componentBoundingBox(x, y, z, -2, 0, 0, 7, 8, 9, mode);
            case ENTRANCE, WART_ROOM -> componentBoundingBox(x, y, z, -5, -3, 0, 13, 14, 13, mode);
            case CORRIDOR_5, CROSSING_2, CORRIDOR_2, CORRIDOR ->
                    componentBoundingBox(x, y, z, -1, 0, 0, 5, 7, 5, mode);
            case CORRIDOR_3 -> componentBoundingBox(x, y, z, -1, -7, 0, 5, 14, 10, mode);
            case CORRIDOR_4 -> componentBoundingBox(x, y, z, -3, 0, 0, 9, 7, 9, mode);
            case END -> componentBoundingBox(x, y, z, -1, -3, 0, 5, 10, 8, mode);
        };
        if (!isFortressPieceAboveGround(box) || intersectsFortressPiece(pieces, box)) {
            return null;
        }
        int fillSeed = room == FortressRoom.END ? random.nextInt() : 0;
        return new NetherFortressPiece(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ(),
                room, mode, depth, fillSeed);
    }

    private static NetherFortressPiece createFortressEnd(List<NetherFortressPiece> pieces, Random random,
            int x, int y, int z, int mode, int depth) {
        return createFortressPiece(FortressRoom.END, pieces, random, x, y, z, mode, depth);
    }

    private static boolean isFortressPieceAboveGround(StructureBoundingBox box) {
        return box != null && box.minY() > 10;
    }

    private static boolean intersectsFortressPiece(List<NetherFortressPiece> pieces, StructureBoundingBox box) {
        for (NetherFortressPiece piece : pieces) {
            if (piece.bounds().intersects(box)) {
                return true;
            }
        }
        return false;
    }

    private static StructureBoundingBox componentBoundingBox(int x, int y, int z, int offsetX, int offsetY,
            int offsetZ, int sizeX, int sizeY, int sizeZ, int mode) {
        return switch (mode) {
            case 2 -> new StructureBoundingBox(x + offsetX, y + offsetY, (z - sizeZ) + 1 + offsetZ,
                    x + sizeX - 1 + offsetX, y + sizeY - 1 + offsetY, z + offsetZ);
            case 1 -> new StructureBoundingBox((x - sizeZ) + 1 + offsetZ, y + offsetY, z + offsetX,
                    x + offsetZ, y + sizeY - 1 + offsetY, z + sizeX - 1 + offsetX);
            case 3 -> new StructureBoundingBox(x + offsetZ, y + offsetY, z + offsetX,
                    x + sizeZ - 1 + offsetZ, y + sizeY - 1 + offsetY, z + sizeX - 1 + offsetX);
            default -> new StructureBoundingBox(x + offsetX, y + offsetY, z + offsetZ,
                    x + sizeX - 1 + offsetX, y + sizeY - 1 + offsetY, z + sizeZ - 1 + offsetZ);
        };
    }

    private static Random fortressStartRandom(long seed, int chunkX, int chunkZ) {
        int cellX = chunkX >> 4;
        int cellZ = chunkZ >> 4;
        Random random = new Random((long) (cellX ^ cellZ << 4) ^ seed);
        random.nextInt();
        random.nextInt(3);
        random.nextInt(8);
        random.nextInt(8);
        return random;
    }

    private static int randomFortressYOffset(Random random, List<NetherFortressPiece> pieces) {
        StructureBoundingBox bounds = StructureBoundingBox.union(pieces);
        int ySize = bounds.maxY() - bounds.minY() + 1;
        int range = ((FORTRESS_MAX_Y - FORTRESS_MIN_Y) + 1) - ySize;
        int targetMinY;
        if (range > 1) {
            targetMinY = FORTRESS_MIN_Y + random.nextInt(range);
        } else {
            targetMinY = FORTRESS_MIN_Y;
        }
        return targetMinY - bounds.minY();
    }

    private static int strongholdAvailableHeightOffset(Random random, List<StrongholdBoxPiece> pieces) {
        StructureBoundingBox bounds = StructureBoundingBox.union(pieces);
        int available = SOURCE_STRUCTURE_WORLD_OCEAN_HEIGHT - MARK_AVAILABLE_HEIGHT_MARGIN;
        int targetMaxY = bounds.maxY() - bounds.minY() + 2;
        if (targetMaxY < available) {
            targetMaxY += random.nextInt(available - targetMaxY);
        }
        return targetMaxY - bounds.maxY();
    }

    private static StrongholdDoor sourceStrongholdDoor(Random random) {
        return switch (random.nextInt(5)) {
            case 2 -> StrongholdDoor.WOOD_DOOR;
            case 3 -> StrongholdDoor.GRATES;
            case 4 -> StrongholdDoor.IRON_DOOR;
            default -> StrongholdDoor.OPENING;
        };
    }

    private static void addStrongholdBranchChildren(StrongholdGenerationContext context,
            List<StrongholdBoxPiece> pieces, List<StrongholdBranchNode> pending, StrongholdBoxPiece piece,
            Random random, int depth) {
        switch (piece.room) {
            case STRAIGHT -> {
                getNextGeneratedStrongholdComponent(context, pieces, pending, random,
                        strongholdNormalAccess(piece, 1, 1), depth);
                if (piece.expandsX) {
                    getNextGeneratedStrongholdComponent(context, pieces, pending, random,
                            strongholdXAccess(piece, 1, 2), depth);
                }
                if (piece.expandsZ) {
                    getNextGeneratedStrongholdComponent(context, pieces, pending, random,
                            strongholdZAccess(piece, 1, 2), depth);
                }
            }
            case STAIRS, STAIRS_STRAIGHT, CHEST_CORRIDOR, PRISON -> getNextGeneratedStrongholdComponent(context, pieces,
                    pending, random, strongholdNormalAccess(piece, 1, 1), depth);
            case LEFT_TURN, RIGHT_TURN -> {
                boolean orientedXSide = piece.coordBaseMode == 2 || piece.coordBaseMode == 3;
                boolean leftTurn = piece.room == StrongholdRoom.LEFT_TURN;
                boolean openLowX = (leftTurn && orientedXSide) || (!leftTurn && !orientedXSide);
                StrongholdAccessPoint access = openLowX
                        ? strongholdXAccess(piece, 1, 1)
                        : strongholdZAccess(piece, 1, 1);
                getNextGeneratedStrongholdComponent(context, pieces, pending, random, access, depth);
            }
            case CROSSING_HALL -> {
                getNextGeneratedStrongholdComponent(context, pieces, pending, random,
                        strongholdNormalAccess(piece, 5, 1), depth);
                addCrossingHallSideBranches(context, pieces, pending, piece, random, depth);
            }
            case CROSSING -> {
                getNextGeneratedStrongholdComponent(context, pieces, pending, random,
                        strongholdNormalAccess(piece, 4, 1), depth);
                getNextGeneratedStrongholdComponent(context, pieces, pending, random,
                        strongholdXAccess(piece, 1, 4), depth);
                getNextGeneratedStrongholdComponent(context, pieces, pending, random,
                        strongholdZAccess(piece, 1, 4), depth);
            }
            default -> {
            }
        }
    }

    private static void addCrossingHallSideBranches(StrongholdGenerationContext context,
            List<StrongholdBoxPiece> pieces, List<StrongholdBranchNode> pending, StrongholdBoxPiece crossing,
            Random random, int depth) {
        int lowerOffset = 3;
        int upperOffset = 5;
        if (crossing.coordBaseMode == 1 || crossing.coordBaseMode == 2) {
            lowerOffset = 5;
            upperOffset = 3;
        }
        if (crossing.crossingLowerLeft) {
            getNextGeneratedStrongholdComponent(context, pieces, pending, random,
                    strongholdXAccess(crossing, lowerOffset, 1), depth);
        }
        if (crossing.crossingUpperLeft) {
            getNextGeneratedStrongholdComponent(context, pieces, pending, random,
                    strongholdXAccess(crossing, upperOffset, 7), depth);
        }
        if (crossing.crossingLowerRight) {
            getNextGeneratedStrongholdComponent(context, pieces, pending, random,
                    strongholdZAccess(crossing, lowerOffset, 1), depth);
        }
        if (crossing.crossingUpperRight) {
            getNextGeneratedStrongholdComponent(context, pieces, pending, random,
                    strongholdZAccess(crossing, upperOffset, 7), depth);
        }
    }

    private static void getNextGeneratedStrongholdComponent(StrongholdGenerationContext context,
            List<StrongholdBoxPiece> pieces, List<StrongholdBranchNode> pending, Random random,
            StrongholdAccessPoint access, int parentDepth) {
        int depth = parentDepth + 1;
        if (depth > STRONGHOLD_BRANCH_MAX_DEPTH
                || Math.abs(access.x() - context.startBox.minX()) > STRONGHOLD_BRANCH_MAX_DISTANCE
                || Math.abs(access.z() - context.startBox.minZ()) > STRONGHOLD_BRANCH_MAX_DISTANCE) {
            return;
        }
        StrongholdBoxPiece child = chooseGeneratedStrongholdComponent(context, pieces, random, access, depth);
        if (child == null) {
            child = createFallbackStrongholdCorridor(pieces, access);
            if (child == null) {
                return;
            }
        }
        pieces.add(child);
        pending.add(new StrongholdBranchNode(child, depth));
    }

    private static StrongholdBoxPiece chooseGeneratedStrongholdComponent(StrongholdGenerationContext context,
            List<StrongholdBoxPiece> pieces, Random random, StrongholdAccessPoint access, int depth) {
        int totalWeight = totalStrongholdWeight(context.weights);
        for (int attempt = 0; attempt < 5 && totalWeight > 0; attempt++) {
            int selected = random.nextInt(totalWeight);
            for (StrongholdPieceWeight weight : context.weights) {
                selected -= weight.weight;
                if (selected >= 0) {
                    continue;
                }
                if (!weight.canSpawn()
                        || (weight == context.previousWeight && !weight.allowRepeat)
                        || !weight.canSpawnAtDepth(depth)) {
                    break;
                }
                StrongholdBoxPiece piece = createGeneratedStrongholdComponent(weight.room, pieces, random,
                        access, depth);
                if (piece == null) {
                    continue;
                }
                weight.used++;
                context.previousWeight = weight;
                return piece;
            }
        }
        return null;
    }

    private static int totalStrongholdWeight(List<StrongholdPieceWeight> weights) {
        boolean hasLimitedAvailable = false;
        int total = 0;
        for (StrongholdPieceWeight weight : weights) {
            if (weight.maxCount > 0 && weight.used < weight.maxCount) {
                hasLimitedAvailable = true;
            }
            total += weight.weight;
        }
        return hasLimitedAvailable ? total : -1;
    }

    private static StrongholdBoxPiece createGeneratedStrongholdComponent(StrongholdRoom room,
            List<StrongholdBoxPiece> pieces, Random random, StrongholdAccessPoint access, int depth) {
        if (room == StrongholdRoom.PORTAL) {
            StrongholdBoxPiece portal = depth > 5
                    ? sourceStrongholdPiece(access, -4, -1, 0, 11, 8, 16, room)
                    : null;
            if (portal == null || portal.bounds().minY() <= 10
                    || intersectsStrongholdPiece(pieces, portal.bounds())) {
                return null;
            }
            return portal;
        }
        if (room == StrongholdRoom.LIBRARY) {
            return createGeneratedStrongholdLibrary(pieces, random, access, depth);
        }
        StructureBoundingBox box = switch (room) {
            case STRAIGHT -> strongholdPieceBox(access, -1, -1, 0, 5, 5, 7);
            case PRISON -> strongholdPieceBox(access, -1, -1, 0, 9, 5, 11);
            case LEFT_TURN, RIGHT_TURN -> strongholdPieceBox(access, -1, -1, 0, 5, 5, 5);
            case CROSSING -> strongholdPieceBox(access, -4, -1, 0, 11, 7, 11);
            case CROSSING_HALL -> strongholdPieceBox(access, -4, -3, 0, 10, 9, 11);
            case STAIRS_STRAIGHT -> strongholdPieceBox(access, -1, -7, 0, 5, 11, 8);
            case STAIRS -> strongholdPieceBox(access, -1, -7, 0, 5, 11, 5);
            case CHEST_CORRIDOR -> strongholdPieceBox(access, -1, -1, 0, 5, 5, 7);
            default -> null;
        };
        if (!isValidStrongholdComponentBox(pieces, box)) {
            return null;
        }
        StrongholdDoor door = sourceStrongholdDoor(random);
        return switch (room) {
            case STRAIGHT -> sourceStrongholdPiece(access, -1, -1, 0, 5, 5, 7, room,
                    random.nextInt(2) == 0, random.nextInt(2) == 0, door);
            case PRISON -> sourceStrongholdPiece(access, -1, -1, 0, 9, 5, 11, room, door);
            case LEFT_TURN, RIGHT_TURN -> sourceStrongholdPiece(access, -1, -1, 0, 5, 5, 5, room, door);
            case CROSSING -> sourceStrongholdPiece(access, -4, -1, 0, 11, 7, 11, room, door,
                    random.nextInt(5));
            case CROSSING_HALL -> sourceStrongholdCrossingHall(access, door, random);
            case STAIRS_STRAIGHT -> sourceStrongholdPiece(access, -1, -7, 0, 5, 11, 8, room, door);
            case STAIRS -> sourceStrongholdPiece(access, -1, -7, 0, 5, 11, 5, room, door);
            case CHEST_CORRIDOR -> sourceStrongholdPiece(access, -1, -1, 0, 5, 5, 7, room, door);
            default -> null;
        };
    }

    private static StrongholdBoxPiece createGeneratedStrongholdLibrary(List<StrongholdBoxPiece> pieces,
            Random random, StrongholdAccessPoint access, int depth) {
        if (depth <= 4) {
            return null;
        }
        if (isValidStrongholdComponentBox(pieces, strongholdPieceBox(access, -4, -1, 0, 14, 11, 15))) {
            StrongholdDoor door = sourceStrongholdDoor(random);
            return sourceStrongholdPiece(access, -4, -1, 0, 14, 11, 15, StrongholdRoom.LIBRARY, door);
        }
        if (isValidStrongholdComponentBox(pieces, strongholdPieceBox(access, -4, -1, 0, 14, 6, 15))) {
            StrongholdDoor door = sourceStrongholdDoor(random);
            return sourceStrongholdPiece(access, -4, -1, 0, 14, 6, 15, StrongholdRoom.LIBRARY, door);
        }
        return null;
    }

    private static boolean isValidStrongholdComponentBox(List<StrongholdBoxPiece> pieces,
            StructureBoundingBox box) {
        return box != null && box.minY() > 10 && !intersectsStrongholdPiece(pieces, box);
    }

    private static StructureBoundingBox strongholdPieceBox(StrongholdAccessPoint access, int offsetX, int offsetY,
            int offsetZ, int sizeX, int sizeY, int sizeZ) {
        return componentToAddBoundingBox(access.x(), access.y(), access.z(), offsetX, offsetY,
                offsetZ, sizeX, sizeY, sizeZ, access.mode());
    }

    private static StrongholdBoxPiece createFallbackStrongholdCorridor(List<StrongholdBoxPiece> pieces,
            StrongholdAccessPoint access) {
        StructureBoundingBox probe = componentToAddBoundingBox(access.x(), access.y(), access.z(),
                -1, -1, 0, 5, 5, 4, access.mode());
        StrongholdBoxPiece blocking = findIntersectingStrongholdPiece(pieces, probe);
        if (blocking == null || blocking.bounds().minY() != probe.minY()) {
            return null;
        }
        for (int length = 3; length >= 1; length--) {
            if (!strongholdCorridorProbeIntersects(blocking.bounds(), access, length - 1)) {
                StructureBoundingBox box = componentToAddBoundingBox(access.x(), access.y(), access.z(),
                        -1, -1, 0, 5, 5, length, access.mode());
                if (box.minY() <= 1) {
                    return null;
                }
                return new StrongholdBoxPiece(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                        box.maxZ(), StrongholdRoom.CORRIDOR, access.mode());
            }
        }
        return null;
    }

    private static boolean strongholdCorridorProbeIntersects(StructureBoundingBox box,
            StrongholdAccessPoint access, int length) {
        if (length <= 0) {
            return false;
        }
        return box.intersects(componentToAddBoundingBox(access.x(), access.y(), access.z(),
                -1, -1, 0, 5, 5, length, access.mode()));
    }

    private static StrongholdBoxPiece sourceStrongholdCrossingHall(StrongholdAccessPoint access,
            StrongholdDoor doorType, Random random) {
        StructureBoundingBox box = componentToAddBoundingBox(access.x(), access.y(), access.z(),
                -4, -3, 0, 10, 9, 11, access.mode());
        return new StrongholdBoxPiece(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ(),
                StrongholdRoom.CROSSING_HALL, access.mode(), false, false,
                random.nextBoolean(), random.nextBoolean(), random.nextBoolean(), random.nextInt(3) > 0,
                doorType, -1);
    }

    private static boolean intersectsStrongholdPiece(List<StrongholdBoxPiece> pieces, StructureBoundingBox box) {
        return findIntersectingStrongholdPiece(pieces, box) != null;
    }

    private static StrongholdBoxPiece findIntersectingStrongholdPiece(List<StrongholdBoxPiece> pieces,
            StructureBoundingBox box) {
        for (StrongholdBoxPiece piece : pieces) {
            if (piece.bounds().intersects(box)) {
                return piece;
            }
        }
        return null;
    }

    private static StrongholdBoxPiece sourceStrongholdPiece(StrongholdAccessPoint access, int offsetX, int offsetY,
            int offsetZ, int sizeX, int sizeY, int sizeZ, StrongholdRoom room) {
        return sourceStrongholdPiece(access, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, room,
                false, false, StrongholdDoor.OPENING, -1);
    }

    private static StrongholdBoxPiece sourceStrongholdPiece(StrongholdAccessPoint access, int offsetX, int offsetY,
            int offsetZ, int sizeX, int sizeY, int sizeZ, StrongholdRoom room, StrongholdDoor doorType) {
        return sourceStrongholdPiece(access, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, room,
                false, false, doorType, -1);
    }

    private static StrongholdBoxPiece sourceStrongholdPiece(StrongholdAccessPoint access, int offsetX, int offsetY,
            int offsetZ, int sizeX, int sizeY, int sizeZ, StrongholdRoom room, StrongholdDoor doorType,
            int roomCrossingType) {
        return sourceStrongholdPiece(access, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, room,
                false, false, doorType, roomCrossingType);
    }

    private static StrongholdBoxPiece sourceStrongholdPiece(StrongholdAccessPoint access, int offsetX, int offsetY,
            int offsetZ, int sizeX, int sizeY, int sizeZ, StrongholdRoom room, boolean expandsX, boolean expandsZ,
            StrongholdDoor doorType) {
        return sourceStrongholdPiece(access, offsetX, offsetY, offsetZ, sizeX, sizeY, sizeZ, room,
                expandsX, expandsZ, doorType, -1);
    }

    private static StrongholdBoxPiece sourceStrongholdPiece(StrongholdAccessPoint access, int offsetX, int offsetY,
            int offsetZ, int sizeX, int sizeY, int sizeZ, StrongholdRoom room, boolean expandsX, boolean expandsZ,
            StrongholdDoor doorType, int roomCrossingType) {
        StructureBoundingBox box = componentToAddBoundingBox(access.x(), access.y(), access.z(), offsetX, offsetY,
                offsetZ, sizeX, sizeY, sizeZ, access.mode());
        return new StrongholdBoxPiece(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ(),
                room, access.mode(), expandsX, expandsZ, false, false, false, false, doorType, roomCrossingType);
    }

    private static StrongholdAccessPoint strongholdNormalAccess(StrongholdBoxPiece piece, int xOffset, int yOffset) {
        StructureBoundingBox box = piece.bounds();
        int y = box.minY() + yOffset;
        return switch (piece.coordBaseMode) {
            case 2 -> new StrongholdAccessPoint(box.minX() + xOffset, y, box.minZ() - 1, piece.coordBaseMode);
            case 1 -> new StrongholdAccessPoint(box.minX() - 1, y, box.minZ() + xOffset, piece.coordBaseMode);
            case 3 -> new StrongholdAccessPoint(box.maxX() + 1, y, box.minZ() + xOffset, piece.coordBaseMode);
            default -> new StrongholdAccessPoint(box.minX() + xOffset, y, box.maxZ() + 1, piece.coordBaseMode);
        };
    }

    private static StrongholdAccessPoint strongholdXAccess(StrongholdBoxPiece piece, int yOffset, int zOffset) {
        StructureBoundingBox box = piece.bounds();
        int y = box.minY() + yOffset;
        return switch (piece.coordBaseMode) {
            case 1, 3 -> new StrongholdAccessPoint(box.minX() + zOffset, y, box.minZ() - 1, 2);
            default -> new StrongholdAccessPoint(box.minX() - 1, y, box.minZ() + zOffset, 1);
        };
    }

    private static StrongholdAccessPoint strongholdZAccess(StrongholdBoxPiece piece, int yOffset, int xOffset) {
        StructureBoundingBox box = piece.bounds();
        int y = box.minY() + yOffset;
        return switch (piece.coordBaseMode) {
            case 1, 3 -> new StrongholdAccessPoint(box.minX() + xOffset, y, box.maxZ() + 1, 0);
            default -> new StrongholdAccessPoint(box.maxX() + 1, y, box.minZ() + xOffset, 3);
        };
    }

    private static StrongholdBoxPiece orientedStrongholdPiece(int rootX, int rootY, int rootZ, int rootMode,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room) {
        return orientedStrongholdPiece(rootX, rootY, rootZ, rootMode, minX, minY, minZ, maxX, maxY, maxZ, room, -1,
                false, false, StrongholdDoor.OPENING, -1);
    }

    private static StrongholdBoxPiece orientedStrongholdPiece(int rootX, int rootY, int rootZ, int rootMode,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room, int baseMode) {
        return orientedStrongholdPiece(rootX, rootY, rootZ, rootMode, minX, minY, minZ, maxX, maxY, maxZ, room,
                baseMode, false, false, StrongholdDoor.OPENING, -1);
    }

    private static StrongholdBoxPiece orientedStrongholdPiece(int rootX, int rootY, int rootZ, int rootMode,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room, int baseMode,
            StrongholdDoor doorType) {
        return orientedStrongholdPiece(rootX, rootY, rootZ, rootMode, minX, minY, minZ, maxX, maxY, maxZ, room,
                baseMode, false, false, doorType, -1);
    }

    private static StrongholdBoxPiece orientedStrongholdPiece(int rootX, int rootY, int rootZ, int rootMode,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room, int baseMode,
            StrongholdDoor doorType, int roomCrossingType) {
        return orientedStrongholdPiece(rootX, rootY, rootZ, rootMode, minX, minY, minZ, maxX, maxY, maxZ, room,
                baseMode, false, false, doorType, roomCrossingType);
    }

    private static StrongholdBoxPiece orientedStrongholdPiece(int rootX, int rootY, int rootZ, int rootMode,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room, int baseMode,
            boolean expandsX, boolean expandsZ, StrongholdDoor doorType) {
        return orientedStrongholdPiece(rootX, rootY, rootZ, rootMode, minX, minY, minZ, maxX, maxY, maxZ, room,
                baseMode, expandsX, expandsZ, doorType, -1);
    }

    private static StrongholdBoxPiece orientedStrongholdPiece(int rootX, int rootY, int rootZ, int rootMode,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room, int baseMode,
            boolean expandsX, boolean expandsZ, StrongholdDoor doorType, int roomCrossingType) {
        return orientedStrongholdPiece(rootX, rootY, rootZ, rootMode, minX, minY, minZ, maxX, maxY, maxZ, room,
                baseMode, expandsX, expandsZ, false, false, false, false, doorType, roomCrossingType);
    }

    private static StrongholdBoxPiece orientedStrongholdPiece(int rootX, int rootY, int rootZ, int rootMode,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room, int baseMode,
            boolean expandsX, boolean expandsZ, boolean crossingLowerLeft, boolean crossingUpperLeft,
            boolean crossingLowerRight, boolean crossingUpperRight, StrongholdDoor doorType, int roomCrossingType) {
        int[][] corners = {
                rotateStrongholdRelative(minX, minZ, rootMode),
                rotateStrongholdRelative(minX, maxZ, rootMode),
                rotateStrongholdRelative(maxX, minZ, rootMode),
                rotateStrongholdRelative(maxX, maxZ, rootMode)
        };
        int rotatedMinX = Integer.MAX_VALUE;
        int rotatedMinZ = Integer.MAX_VALUE;
        int rotatedMaxX = Integer.MIN_VALUE;
        int rotatedMaxZ = Integer.MIN_VALUE;
        for (int[] corner : corners) {
            rotatedMinX = Math.min(rotatedMinX, corner[0]);
            rotatedMinZ = Math.min(rotatedMinZ, corner[1]);
            rotatedMaxX = Math.max(rotatedMaxX, corner[0]);
            rotatedMaxZ = Math.max(rotatedMaxZ, corner[1]);
        }
        return new StrongholdBoxPiece(rootX + rotatedMinX, rootY + minY, rootZ + rotatedMinZ,
                rootX + rotatedMaxX, rootY + maxY, rootZ + rotatedMaxZ, room,
                rotateStrongholdMode(baseMode, rootMode), expandsX, expandsZ, crossingLowerLeft, crossingUpperLeft,
                crossingLowerRight, crossingUpperRight,
                doorType, roomCrossingType);
    }

    private static int[] rotateStrongholdRelative(int x, int z, int rootMode) {
        return switch (rootMode) {
            case 0 -> new int[] { 4 - z, x };
            case 1 -> new int[] { 4 - x, 4 - z };
            case 2 -> new int[] { z, 4 - x };
            default -> new int[] { x, z };
        };
    }

    private static int rotateStrongholdMode(int baseMode, int rootMode) {
        if (baseMode < 0) {
            return -1;
        }
        int[] vector = switch (baseMode) {
            case 0 -> new int[] { 0, 1 };
            case 1 -> new int[] { -1, 0 };
            case 2 -> new int[] { 0, -1 };
            default -> new int[] { 1, 0 };
        };
        int[] rotated = switch (rootMode) {
            case 0 -> new int[] { -vector[1], vector[0] };
            case 1 -> new int[] { -vector[0], -vector[1] };
            case 2 -> new int[] { vector[1], -vector[0] };
            default -> vector;
        };
        if (rotated[0] > 0) {
            return 3;
        }
        if (rotated[0] < 0) {
            return 1;
        }
        return rotated[1] > 0 ? 0 : 2;
    }

    private void addLegacyMineshaft(long seed, int originX, int originZ, int targetChunkX, int targetChunkZ,
            List<StructureStart> starts) {
        StructureStart start = cachedGeneratedStart(seed, StructureType.MINESHAFT, originX, originZ, null);
        if (start != null && start.intersectsChunk(targetChunkX, targetChunkZ)) {
            starts.add(start);
        }
    }

    private StructureStart buildMineshaftStart(long seed, int originX, int originZ) {
        Random random = mapGenStructureRandom(seed, originX, originZ);
        random.nextInt();
        if (random.nextInt(100) != 0 || random.nextInt(80) >= Math.max(Math.abs(originX), Math.abs(originZ))) {
            return null;
        }
        StructureStart start = new StructureStart(StructureType.MINESHAFT, originX, originZ);
        int roomMinX = (originX << 4) + 2;
        int roomMinY = 50;
        int roomMinZ = (originZ << 4) + 2;
        StructureBoundingBox roomBox = new StructureBoundingBox(roomMinX, roomMinY, roomMinZ,
                roomMinX + 7 + random.nextInt(6), 54 + random.nextInt(6), roomMinZ + 7 + random.nextInt(6));

        ArrayList<MineshaftDescriptor> pieces = new ArrayList<>();
        ArrayList<StructureBoundingBox> openings = new ArrayList<>();
        buildMineshaftRoomBranches(roomBox, pieces, openings, random);

        StructureBoundingBox[] boxes = new StructureBoundingBox[pieces.size() + 1];
        boxes[0] = roomBox;
        for (int i = 0; i < pieces.size(); i++) {
            boxes[i + 1] = pieces.get(i).bounds;
        }
        int yOffset = mineshaftAvailableHeightOffset(random, boxes);
        roomBox = offset(roomBox, 0, yOffset, 0);
        List<StructureBoundingBox> shiftedOpenings = openings.stream()
                .map(opening -> offset(opening, 0, yOffset, 0))
                .toList();

        start.addPiece(new MineshaftRoomPiece(roomBox, shiftedOpenings));
        for (MineshaftDescriptor descriptor : pieces) {
            start.addPiece(descriptor.toPiece(yOffset));
        }
        return start;
    }

    private static void buildMineshaftRoomBranches(StructureBoundingBox roomBox,
            List<MineshaftDescriptor> pieces, List<StructureBoundingBox> openings, Random random) {
        int depth = 0;
        int yRange = roomBox.maxY() - roomBox.minY() - 3;
        if (yRange <= 0) {
            yRange = 1;
        }

        for (int xOffset = 0; xOffset < width(roomBox); xOffset += 4) {
            xOffset += random.nextInt(width(roomBox));
            if (xOffset + 3 > width(roomBox)) {
                break;
            }
            MineshaftDescriptor child = getNextMineshaftComponent(roomBox, pieces, random,
                    roomBox.minX() + xOffset, roomBox.minY() + random.nextInt(yRange) + 1,
                    roomBox.minZ() - 1, 2, depth);
            if (child != null) {
                openings.add(new StructureBoundingBox(child.bounds.minX(), child.bounds.minY(), roomBox.minZ(),
                        child.bounds.maxX(), child.bounds.maxY(), roomBox.minZ() + 1));
            }
        }

        for (int xOffset = 0; xOffset < width(roomBox); xOffset += 4) {
            xOffset += random.nextInt(width(roomBox));
            if (xOffset + 3 > width(roomBox)) {
                break;
            }
            MineshaftDescriptor child = getNextMineshaftComponent(roomBox, pieces, random,
                    roomBox.minX() + xOffset, roomBox.minY() + random.nextInt(yRange) + 1,
                    roomBox.maxZ() + 1, 0, depth);
            if (child != null) {
                openings.add(new StructureBoundingBox(child.bounds.minX(), child.bounds.minY(), roomBox.maxZ() - 1,
                        child.bounds.maxX(), child.bounds.maxY(), roomBox.maxZ()));
            }
        }

        for (int zOffset = 0; zOffset < depth(roomBox); zOffset += 4) {
            zOffset += random.nextInt(depth(roomBox));
            if (zOffset + 3 > depth(roomBox)) {
                break;
            }
            MineshaftDescriptor child = getNextMineshaftComponent(roomBox, pieces, random,
                    roomBox.minX() - 1, roomBox.minY() + random.nextInt(yRange) + 1,
                    roomBox.minZ() + zOffset, 1, depth);
            if (child != null) {
                openings.add(new StructureBoundingBox(roomBox.minX(), child.bounds.minY(), child.bounds.minZ(),
                        roomBox.minX() + 1, child.bounds.maxY(), child.bounds.maxZ()));
            }
        }

        for (int zOffset = 0; zOffset < depth(roomBox); zOffset += 4) {
            zOffset += random.nextInt(depth(roomBox));
            if (zOffset + 3 > depth(roomBox)) {
                break;
            }
            MineshaftDescriptor child = getNextMineshaftComponent(roomBox, pieces, random,
                    roomBox.maxX() + 1, roomBox.minY() + random.nextInt(yRange) + 1,
                    roomBox.minZ() + zOffset, 3, depth);
            if (child != null) {
                openings.add(new StructureBoundingBox(roomBox.maxX() - 1, child.bounds.minY(), child.bounds.minZ(),
                        roomBox.maxX(), child.bounds.maxY(), child.bounds.maxZ()));
            }
        }
    }

    private static MineshaftDescriptor getNextMineshaftComponent(StructureBoundingBox root,
            List<MineshaftDescriptor> pieces, Random random, int x, int y, int z, int mode, int parentDepth) {
        if (parentDepth > MINESHAFT_MAX_DEPTH
                || Math.abs(x - root.minX()) > MINESHAFT_MAX_BRANCH_DISTANCE
                || Math.abs(z - root.minZ()) > MINESHAFT_MAX_BRANCH_DISTANCE) {
            return null;
        }
        MineshaftDescriptor descriptor = randomMineshaftComponent(root, pieces, random, x, y, z, mode,
                parentDepth + 1);
        if (descriptor == null) {
            return null;
        }
        pieces.add(descriptor);
        buildMineshaftComponent(root, pieces, random, descriptor);
        return descriptor;
    }

    private static MineshaftDescriptor randomMineshaftComponent(StructureBoundingBox root,
            List<MineshaftDescriptor> pieces, Random random, int x, int y, int z, int mode, int depth) {
        int choice = random.nextInt(100);
        if (choice >= 80) {
            boolean multipleFloors = random.nextInt(4) == 0;
            StructureBoundingBox box = mineshaftCrossBox(x, y, z, mode, multipleFloors);
            return intersectsMineshaft(root, pieces, box) ? null
                    : MineshaftDescriptor.cross(box, mode, depth, multipleFloors);
        }
        if (choice >= 70) {
            StructureBoundingBox box = mineshaftStairsBox(x, y, z, mode);
            return intersectsMineshaft(root, pieces, box) ? null
                    : MineshaftDescriptor.stairs(box, mode, depth);
        }

        int sections = random.nextInt(3) + 2;
        StructureBoundingBox box = null;
        while (sections > 0) {
            box = mineshaftCorridorBox(x, y, z, mode, sections);
            if (!intersectsMineshaft(root, pieces, box)) {
                break;
            }
            sections--;
        }
        if (sections <= 0 || box == null) {
            return null;
        }
        boolean hasRails = random.nextInt(3) == 0;
        boolean hasSpiders = !hasRails && random.nextInt(23) == 0;
        return MineshaftDescriptor.corridor(box, mode, depth, sections, hasRails, hasSpiders);
    }

    private static void buildMineshaftComponent(StructureBoundingBox root, List<MineshaftDescriptor> pieces,
            Random random, MineshaftDescriptor descriptor) {
        switch (descriptor.kind) {
            case CORRIDOR -> buildMineshaftCorridor(root, pieces, random, descriptor);
            case CROSS -> buildMineshaftCross(root, pieces, random, descriptor);
            case STAIRS -> buildMineshaftStairs(root, pieces, random, descriptor);
        }
    }

    private static StructureBoundingBox mineshaftCorridorBox(StructureBoundingBox room, int side, int y,
            int sections) {
        int length = sections * 5;
        return switch (side) {
            case 0 -> {
                int x = room.centerX() - 1;
                yield new StructureBoundingBox(x, y, room.minZ() - length, x + 2, y + 2, room.minZ() - 1);
            }
            case 1 -> {
                int x = room.centerX() - 1;
                yield new StructureBoundingBox(x, y, room.maxZ() + 1, x + 2, y + 2, room.maxZ() + length);
            }
            case 2 -> {
                int z = room.centerZ() - 1;
                yield new StructureBoundingBox(room.minX() - length, y, z, room.minX() - 1, y + 2, z + 2);
            }
            default -> {
                int z = room.centerZ() - 1;
                yield new StructureBoundingBox(room.maxX() + 1, y, z, room.maxX() + length, y + 2, z + 2);
            }
        };
    }

    private static void buildMineshaftCorridor(StructureBoundingBox root, List<MineshaftDescriptor> pieces,
            Random random, MineshaftDescriptor corridor) {
        int branch = random.nextInt(4);
        int y = corridor.bounds.minY() - 1 + random.nextInt(3);
        switch (corridor.mode) {
            case 2 -> {
                if (branch <= 1) {
                    getNextMineshaftComponent(root, pieces, random, corridor.bounds.minX(), y,
                            corridor.bounds.minZ() - 1, 2, corridor.depth);
                } else if (branch == 2) {
                    getNextMineshaftComponent(root, pieces, random, corridor.bounds.minX() - 1, y,
                            corridor.bounds.minZ(), 1, corridor.depth);
                } else {
                    getNextMineshaftComponent(root, pieces, random, corridor.bounds.maxX() + 1, y,
                            corridor.bounds.minZ(), 3, corridor.depth);
                }
            }
            case 0 -> {
                if (branch <= 1) {
                    getNextMineshaftComponent(root, pieces, random, corridor.bounds.minX(), y,
                            corridor.bounds.maxZ() + 1, 0, corridor.depth);
                } else if (branch == 2) {
                    getNextMineshaftComponent(root, pieces, random, corridor.bounds.minX() - 1, y,
                            corridor.bounds.maxZ() - 3, 1, corridor.depth);
                } else {
                    getNextMineshaftComponent(root, pieces, random, corridor.bounds.maxX() + 1, y,
                            corridor.bounds.maxZ() - 3, 3, corridor.depth);
                }
            }
            case 1 -> {
                if (branch <= 1) {
                    getNextMineshaftComponent(root, pieces, random, corridor.bounds.minX() - 1, y,
                            corridor.bounds.minZ(), 1, corridor.depth);
                } else if (branch == 2) {
                    getNextMineshaftComponent(root, pieces, random, corridor.bounds.minX(), y,
                            corridor.bounds.minZ() - 1, 2, corridor.depth);
                } else {
                    getNextMineshaftComponent(root, pieces, random, corridor.bounds.minX(), y,
                            corridor.bounds.maxZ() + 1, 0, corridor.depth);
                }
            }
            case 3 -> {
                if (branch <= 1) {
                    getNextMineshaftComponent(root, pieces, random, corridor.bounds.maxX() + 1, y,
                            corridor.bounds.minZ(), 3, corridor.depth);
                } else if (branch == 2) {
                    getNextMineshaftComponent(root, pieces, random, corridor.bounds.maxX() - 3, y,
                            corridor.bounds.minZ() - 1, 2, corridor.depth);
                } else {
                    getNextMineshaftComponent(root, pieces, random, corridor.bounds.maxX() - 3, y,
                            corridor.bounds.maxZ() + 1, 0, corridor.depth);
                }
            }
            default -> {
            }
        }

        if (corridor.depth >= MINESHAFT_MAX_DEPTH) {
            return;
        }
        if (corridor.mode == 2 || corridor.mode == 0) {
            for (int z = corridor.bounds.minZ() + 3; z + 3 <= corridor.bounds.maxZ(); z += 5) {
                int side = random.nextInt(5);
                if (side == 0) {
                    getNextMineshaftComponent(root, pieces, random, corridor.bounds.minX() - 1,
                            corridor.bounds.minY(), z, 1, corridor.depth + 1);
                } else if (side == 1) {
                    getNextMineshaftComponent(root, pieces, random, corridor.bounds.maxX() + 1,
                            corridor.bounds.minY(), z, 3, corridor.depth + 1);
                }
            }
        } else {
            for (int x = corridor.bounds.minX() + 3; x + 3 <= corridor.bounds.maxX(); x += 5) {
                int side = random.nextInt(5);
                if (side == 0) {
                    getNextMineshaftComponent(root, pieces, random, x, corridor.bounds.minY(),
                            corridor.bounds.minZ() - 1, 2, corridor.depth + 1);
                } else if (side == 1) {
                    getNextMineshaftComponent(root, pieces, random, x, corridor.bounds.minY(),
                            corridor.bounds.maxZ() + 1, 0, corridor.depth + 1);
                }
            }
        }
    }

    private static void buildMineshaftCross(StructureBoundingBox root, List<MineshaftDescriptor> pieces,
            Random random, MineshaftDescriptor cross) {
        switch (cross.mode) {
            case 2 -> {
                getNextMineshaftComponent(root, pieces, random, cross.bounds.minX() + 1, cross.bounds.minY(),
                        cross.bounds.minZ() - 1, 2, cross.depth);
                getNextMineshaftComponent(root, pieces, random, cross.bounds.minX() - 1, cross.bounds.minY(),
                        cross.bounds.minZ() + 1, 1, cross.depth);
                getNextMineshaftComponent(root, pieces, random, cross.bounds.maxX() + 1, cross.bounds.minY(),
                        cross.bounds.minZ() + 1, 3, cross.depth);
            }
            case 0 -> {
                getNextMineshaftComponent(root, pieces, random, cross.bounds.minX() + 1, cross.bounds.minY(),
                        cross.bounds.maxZ() + 1, 0, cross.depth);
                getNextMineshaftComponent(root, pieces, random, cross.bounds.minX() - 1, cross.bounds.minY(),
                        cross.bounds.minZ() + 1, 1, cross.depth);
                getNextMineshaftComponent(root, pieces, random, cross.bounds.maxX() + 1, cross.bounds.minY(),
                        cross.bounds.minZ() + 1, 3, cross.depth);
            }
            case 1 -> {
                getNextMineshaftComponent(root, pieces, random, cross.bounds.minX() + 1, cross.bounds.minY(),
                        cross.bounds.minZ() - 1, 2, cross.depth);
                getNextMineshaftComponent(root, pieces, random, cross.bounds.minX() + 1, cross.bounds.minY(),
                        cross.bounds.maxZ() + 1, 0, cross.depth);
                getNextMineshaftComponent(root, pieces, random, cross.bounds.minX() - 1, cross.bounds.minY(),
                        cross.bounds.minZ() + 1, 1, cross.depth);
            }
            case 3 -> {
                getNextMineshaftComponent(root, pieces, random, cross.bounds.minX() + 1, cross.bounds.minY(),
                        cross.bounds.minZ() - 1, 2, cross.depth);
                getNextMineshaftComponent(root, pieces, random, cross.bounds.minX() + 1, cross.bounds.minY(),
                        cross.bounds.maxZ() + 1, 0, cross.depth);
                getNextMineshaftComponent(root, pieces, random, cross.bounds.maxX() + 1, cross.bounds.minY(),
                        cross.bounds.minZ() + 1, 3, cross.depth);
            }
            default -> {
            }
        }

        if (cross.multipleFloors && random.nextBoolean()) {
            getNextMineshaftComponent(root, pieces, random, cross.bounds.minX() + 1, cross.bounds.minY() + 4,
                    cross.bounds.minZ() - 1, 2, cross.depth);
        }
        if (cross.multipleFloors && random.nextBoolean()) {
            getNextMineshaftComponent(root, pieces, random, cross.bounds.minX() - 1, cross.bounds.minY() + 4,
                    cross.bounds.minZ() + 1, 1, cross.depth);
        }
        if (cross.multipleFloors && random.nextBoolean()) {
            getNextMineshaftComponent(root, pieces, random, cross.bounds.maxX() + 1, cross.bounds.minY() + 4,
                    cross.bounds.minZ() + 1, 3, cross.depth);
        }
        if (cross.multipleFloors && random.nextBoolean()) {
            getNextMineshaftComponent(root, pieces, random, cross.bounds.minX() + 1, cross.bounds.minY() + 4,
                    cross.bounds.maxZ() + 1, 0, cross.depth);
        }
    }

    private static void buildMineshaftStairs(StructureBoundingBox root, List<MineshaftDescriptor> pieces,
            Random random, MineshaftDescriptor stairs) {
        switch (stairs.mode) {
            case 2 -> getNextMineshaftComponent(root, pieces, random, stairs.bounds.minX(), stairs.bounds.minY(),
                    stairs.bounds.minZ() - 1, 2, stairs.depth);
            case 0 -> getNextMineshaftComponent(root, pieces, random, stairs.bounds.minX(), stairs.bounds.minY(),
                    stairs.bounds.maxZ() + 1, 0, stairs.depth);
            case 1 -> getNextMineshaftComponent(root, pieces, random, stairs.bounds.minX() - 1, stairs.bounds.minY(),
                    stairs.bounds.minZ(), 1, stairs.depth);
            case 3 -> getNextMineshaftComponent(root, pieces, random, stairs.bounds.maxX() + 1, stairs.bounds.minY(),
                    stairs.bounds.minZ(), 3, stairs.depth);
            default -> {
            }
        }
    }

    private static StructureBoundingBox mineshaftCorridorBox(int x, int y, int z, int mode, int sections) {
        int length = sections * 5;
        return switch (mode) {
            case 2 -> new StructureBoundingBox(x, y, z - (length - 1), x + 2, y + 2, z);
            case 1 -> new StructureBoundingBox(x - (length - 1), y, z, x, y + 2, z + 2);
            case 3 -> new StructureBoundingBox(x, y, z, x + (length - 1), y + 2, z + 2);
            default -> new StructureBoundingBox(x, y, z, x + 2, y + 2, z + (length - 1));
        };
    }

    private static StructureBoundingBox mineshaftCrossBox(int x, int y, int z, int mode, boolean multipleFloors) {
        int maxY = y + 2 + (multipleFloors ? 4 : 0);
        return switch (mode) {
            case 2 -> new StructureBoundingBox(x - 1, y, z - 4, x + 3, maxY, z);
            case 1 -> new StructureBoundingBox(x - 4, y, z - 1, x, maxY, z + 3);
            case 3 -> new StructureBoundingBox(x, y, z - 1, x + 4, maxY, z + 3);
            default -> new StructureBoundingBox(x - 1, y, z, x + 3, maxY, z + 4);
        };
    }

    private static StructureBoundingBox mineshaftStairsBox(int x, int y, int z, int mode) {
        return switch (mode) {
            case 2 -> new StructureBoundingBox(x, y - 5, z - 8, x + 2, y + 2, z);
            case 1 -> new StructureBoundingBox(x - 8, y - 5, z, x, y + 2, z + 2);
            case 3 -> new StructureBoundingBox(x, y - 5, z, x + 8, y + 2, z + 2);
            default -> new StructureBoundingBox(x, y - 5, z, x + 2, y + 2, z + 8);
        };
    }

    private static boolean intersectsMineshaft(StructureBoundingBox root, List<MineshaftDescriptor> pieces,
            StructureBoundingBox box) {
        if (box == null || root.intersects(box)) {
            return true;
        }
        for (MineshaftDescriptor piece : pieces) {
            if (piece.bounds.intersects(box)) {
                return true;
            }
        }
        return false;
    }

    private static int width(StructureBoundingBox box) {
        return box.maxX() - box.minX() + 1;
    }

    private static int depth(StructureBoundingBox box) {
        return box.maxZ() - box.minZ() + 1;
    }

    private static int mineshaftAvailableHeightOffset(Random random, StructureBoundingBox... boxes) {
        StructureBoundingBox bounds = union(boxes);
        int available = SOURCE_STRUCTURE_WORLD_OCEAN_HEIGHT - MARK_AVAILABLE_HEIGHT_MARGIN;
        int targetMaxY = bounds.maxY() - bounds.minY() + 2;
        if (targetMaxY < available) {
            targetMaxY += random.nextInt(available - targetMaxY);
        }
        return targetMaxY - bounds.maxY();
    }

    private static StructureBoundingBox union(StructureBoundingBox... boxes) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (StructureBoundingBox box : boxes) {
            minX = Math.min(minX, box.minX());
            minY = Math.min(minY, box.minY());
            minZ = Math.min(minZ, box.minZ());
            maxX = Math.max(maxX, box.maxX());
            maxY = Math.max(maxY, box.maxY());
            maxZ = Math.max(maxZ, box.maxZ());
        }
        return new StructureBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static StructureBoundingBox offset(StructureBoundingBox box, int x, int y, int z) {
        return new StructureBoundingBox(box.minX() + x, box.minY() + y, box.minZ() + z,
                box.maxX() + x, box.maxY() + y, box.maxZ() + z);
    }

    private void addLegacyVillage(long seed, int originX, int originZ, int targetChunkX, int targetChunkZ,
            ReleaseOneWorldGenerator generator, List<StructureStart> starts) {
        StructureStart start = cachedGeneratedStart(seed, StructureType.VILLAGE, originX, originZ, generator);
        if (start != null && start.intersectsChunk(targetChunkX, targetChunkZ)) {
            starts.add(start);
        }
    }

    private StructureStart buildVillageStart(long seed, int originX, int originZ,
            ReleaseOneWorldGenerator generator) {
        if (villageBiome(seed, originX, originZ, generator) == null) {
            return null;
        }
        int wellMinX = originX * Chunk.WIDTH + 2;
        int wellMinZ = originZ * Chunk.DEPTH + 2;
        int wellPlacementMinY = villageWellGroundY(generator, wellMinX, wellMinZ) - 11;
        StructureStart start = new StructureStart(StructureType.VILLAGE, originX, originZ);
        StructureBoundingBox wellBox = new StructureBoundingBox(wellMinX, VILLAGE_WELL_SOURCE_MIN_Y, wellMinZ,
                wellMinX + 5, VILLAGE_WELL_SOURCE_MAX_Y, wellMinZ + 5);
        start.addPiece(new VillageWellPiece(wellMinX, VILLAGE_WELL_SOURCE_MIN_Y, wellMinZ, wellPlacementMinY));
        addVillageWellRoads(seed, originX, originZ, generator, start, wellBox);
        return isSizeableVillageStart(start) ? start : null;
    }

    private static boolean isSizeableVillageStart(StructureStart start) {
        int nonRoadPieces = 0;
        for (StructurePiece piece : start.pieces()) {
            if (piece instanceof VillagePathPiece) {
                continue;
            }
            nonRoadPieces++;
            if (nonRoadPieces > 2) {
                return true;
            }
        }
        return false;
    }

    private static void addVillageWellRoads(long seed, int chunkX, int chunkZ, ReleaseOneWorldGenerator generator,
            StructureStart start, StructureBoundingBox wellBox) {
        Random random = villageStartRandom(seed, chunkX, chunkZ);
        List<VillagePieceWeight> weights = villagePieceWeights(random, 0);
        random.nextInt(4);
        VillageGenerationContext context = new VillageGenerationContext(wellBox, weights);
        int roadY = wellBox.maxY() - 4;
        addVillagePath(context, start, random, generator, wellBox.minX() - 1, roadY, wellBox.minZ() + 1, 1, 0);
        addVillagePath(context, start, random, generator, wellBox.maxX() + 1, roadY, wellBox.minZ() + 1, 3, 0);
        addVillagePath(context, start, random, generator, wellBox.minX() + 1, roadY, wellBox.minZ() - 1, 2, 0);
        addVillagePath(context, start, random, generator, wellBox.minX() + 1, roadY, wellBox.maxZ() + 1, 0, 0);
        processVillageQueues(context, start, random, generator);
    }

    private static VillagePathPiece addVillagePath(VillageGenerationContext context, StructureStart start,
            Random random, ReleaseOneWorldGenerator generator, int x, int y, int z, int mode, int componentType) {
        if (componentType > 3 || tooFarFromVillageStart(context.startBox, x, z)) {
            return null;
        }
        StructureBoundingBox box = null;
        for (int length = 7 * randomIntegerInRange(random, 3, 5); length >= 7; length -= 7) {
            StructureBoundingBox candidate = componentToAddBoundingBox(x, y, z, 0, 0, 0, 3, 3, length, mode);
            if (intersectsAny(start.pieces(), candidate)) {
                continue;
            }
            box = candidate;
            break;
        }
        if (box == null || box.minY() <= 10 || !isVillageBiomeViable(generator, box)) {
            return null;
        }
        VillagePathPiece path = new VillagePathPiece(box, generator, mode, componentType);
        start.addPiece(path);
        context.pendingPaths.add(path);
        return path;
    }

    private static void processVillageQueues(VillageGenerationContext context, StructureStart start,
            Random random, ReleaseOneWorldGenerator generator) {
        while (!context.pendingBuildings.isEmpty() || !context.pendingPaths.isEmpty()) {
            if (!context.pendingPaths.isEmpty()) {
                VillagePathPiece path = context.pendingPaths.remove(random.nextInt(context.pendingPaths.size()));
                buildVillagePathChildren(context, start, path, random, generator);
            } else {
                context.pendingBuildings.remove(random.nextInt(context.pendingBuildings.size()));
            }
        }
    }

    private static void buildVillagePathChildren(VillageGenerationContext context, StructureStart start,
            VillagePathPiece path, Random random, ReleaseOneWorldGenerator generator) {
        boolean addedBuilding = false;
        int pathLength = Math.max(width(path.bounds()), depth(path.bounds()));
        for (int offset = random.nextInt(5); offset < pathLength - 8; offset += 2 + random.nextInt(5)) {
            StructurePiece child = addVillageComponentNN(context, start, path, random, generator, 0, offset);
            if (child != null) {
                offset += Math.max(width(child.bounds()), depth(child.bounds()));
                addedBuilding = true;
            }
        }
        for (int offset = random.nextInt(5); offset < pathLength - 8; offset += 2 + random.nextInt(5)) {
            StructurePiece child = addVillageComponentPP(context, start, path, random, generator, 0, offset);
            if (child != null) {
                offset += Math.max(width(child.bounds()), depth(child.bounds()));
                addedBuilding = true;
            }
        }
        if (addedBuilding && random.nextInt(3) > 0) {
            switch (path.mode) {
                case 2 -> addVillagePath(context, start, random, generator, path.bounds().minX() - 1,
                        path.bounds().minY(), path.bounds().minZ(), 1, path.componentType);
                case 3 -> addVillagePath(context, start, random, generator, path.bounds().maxX() - 2,
                        path.bounds().minY(), path.bounds().minZ() - 1, 2, path.componentType);
                case 1 -> addVillagePath(context, start, random, generator, path.bounds().minX(),
                        path.bounds().minY(), path.bounds().minZ() - 1, 2, path.componentType);
                default -> addVillagePath(context, start, random, generator, path.bounds().minX() - 1,
                        path.bounds().minY(), path.bounds().maxZ() - 2, 1, path.componentType);
            }
        }
        if (addedBuilding && random.nextInt(3) > 0) {
            switch (path.mode) {
                case 2 -> addVillagePath(context, start, random, generator, path.bounds().maxX() + 1,
                        path.bounds().minY(), path.bounds().minZ(), 3, path.componentType);
                case 3 -> addVillagePath(context, start, random, generator, path.bounds().maxX() - 2,
                        path.bounds().minY(), path.bounds().maxZ() + 1, 0, path.componentType);
                case 1 -> addVillagePath(context, start, random, generator, path.bounds().minX(),
                        path.bounds().minY(), path.bounds().maxZ() + 1, 0, path.componentType);
                default -> addVillagePath(context, start, random, generator, path.bounds().maxX() + 1,
                        path.bounds().minY(), path.bounds().maxZ() - 2, 3, path.componentType);
            }
        }
    }

    private static StructurePiece addVillageComponentNN(VillageGenerationContext context, StructureStart start,
            VillagePathPiece path, Random random, ReleaseOneWorldGenerator generator, int yOffset, int pathOffset) {
        return switch (path.mode) {
            case 1, 3 -> addNextVillageComponent(context, start, random, generator,
                    path.bounds().minX() + pathOffset, path.bounds().minY() + yOffset,
                    path.bounds().minZ() - 1, 2, path.componentType + 1);
            default -> addNextVillageComponent(context, start, random, generator,
                    path.bounds().minX() - 1, path.bounds().minY() + yOffset,
                    path.bounds().minZ() + pathOffset, 1, path.componentType + 1);
        };
    }

    private static StructurePiece addVillageComponentPP(VillageGenerationContext context, StructureStart start,
            VillagePathPiece path, Random random, ReleaseOneWorldGenerator generator, int yOffset, int pathOffset) {
        return switch (path.mode) {
            case 1, 3 -> addNextVillageComponent(context, start, random, generator,
                    path.bounds().minX() + pathOffset, path.bounds().minY() + yOffset,
                    path.bounds().maxZ() + 1, 0, path.componentType + 1);
            default -> addNextVillageComponent(context, start, random, generator,
                    path.bounds().maxX() + 1, path.bounds().minY() + yOffset,
                    path.bounds().minZ() + pathOffset, 3, path.componentType + 1);
        };
    }

    private static StructurePiece addNextVillageComponent(VillageGenerationContext context, StructureStart start,
            Random random, ReleaseOneWorldGenerator generator, int x, int y, int z, int mode, int componentType) {
        if (componentType > 50 || tooFarFromVillageStart(context.startBox, x, z)) {
            return null;
        }
        StructurePiece piece = chooseVillageComponent(context, start, random, generator, x, y, z, mode,
                componentType);
        if (piece == null) {
            return null;
        }
        if (!isVillageBiomeViable(generator, piece.bounds())) {
            return null;
        }
        start.addPiece(piece);
        context.pendingBuildings.add(piece);
        return piece;
    }

    private static StructurePiece chooseVillageComponent(VillageGenerationContext context, StructureStart start,
            Random random, ReleaseOneWorldGenerator generator, int x, int y, int z, int mode, int componentType) {
        int totalWeight = availableVillagePieceWeight(context.weights);
        if (totalWeight <= 0) {
            return null;
        }
        boolean skippedUnimplementedPiece = false;
        for (int attempts = 0; attempts < 5; attempts++) {
            int weight = random.nextInt(totalWeight);
            for (VillagePieceWeight pieceWeight : new ArrayList<>(context.weights)) {
                weight -= pieceWeight.weight;
                if (weight >= 0) {
                    continue;
                }
                if (!pieceWeight.canSpawnMore()
                        || pieceWeight == context.previousWeight && context.weights.size() > 1) {
                    break;
                }
                if (!isImplementedVillagePiece(pieceWeight.kind)) {
                    skippedUnimplementedPiece = true;
                    continue;
                }
                StructurePiece piece = createVillagePiece(pieceWeight.kind, start, random, generator, x, y, z,
                        mode);
                if (piece != null) {
                    pieceWeight.spawned++;
                    context.previousWeight = pieceWeight;
                    if (!pieceWeight.canSpawnMore()) {
                        context.weights.remove(pieceWeight);
                    }
                    return piece;
                }
            }
        }
        return skippedUnimplementedPiece ? null : createVillageTorch(start, generator, x, y, z, mode);
    }

    private static int availableVillagePieceWeight(List<VillagePieceWeight> weights) {
        int total = 0;
        boolean canSpawn = false;
        for (VillagePieceWeight weight : weights) {
            if (weight.canSpawnMore()) {
                canSpawn = true;
            }
            total += weight.weight;
        }
        return canSpawn ? total : -1;
    }

    private static StructurePiece createVillagePiece(VillagePieceKind kind, StructureStart start, Random random,
            ReleaseOneWorldGenerator generator, int x, int y, int z, int mode) {
        return switch (kind) {
            case WOOD_HUT -> createVillageWoodHut(start, random, generator, x, y, z, mode);
            case HOUSE_4_GARDEN -> createVillageHouse4Garden(start, random, generator, x, y, z, mode);
            case CHURCH -> createVillageChurch(start, generator, x, y, z, mode);
            case HOUSE_1 -> createVillageHouse1(start, generator, x, y, z, mode);
            case HOUSE_3 -> createVillageHouse3(start, generator, x, y, z, mode);
            case HOUSE_2 -> createVillageBlacksmith(start, generator, x, y, z, mode);
            case HALL -> createVillageHall(start, generator, x, y, z, mode);
            case FIELD -> createVillageFarm(start, generator, x, y, z, mode, true);
            case FIELD_2 -> createVillageFarm(start, generator, x, y, z, mode, false);
            default -> null;
        };
    }

    private static StructurePiece createVillageWoodHut(StructureStart start, Random random,
            ReleaseOneWorldGenerator generator, int x, int y, int z, int mode) {
        StructureBoundingBox box = componentToAddBoundingBox(x, y, z, 0, 0, 0, 4, 6, 5, mode);
        if (box.minY() <= 10 || intersectsAny(start.pieces(), box)) {
            return null;
        }
        int placementMinY = villageAverageGroundY(generator, box);
        return new VillageWoodHutPiece(box, mode, placementMinY, random.nextBoolean(), random.nextInt(3));
    }

    private static StructurePiece createVillageHall(StructureStart start, ReleaseOneWorldGenerator generator,
            int x, int y, int z, int mode) {
        StructureBoundingBox box = componentToAddBoundingBox(x, y, z, 0, 0, 0, 9, 7, 11, mode);
        if (box.minY() <= 10 || intersectsAny(start.pieces(), box)) {
            return null;
        }
        int placementMinY = villageAverageGroundY(generator, box);
        return new VillageHallPiece(box, mode, placementMinY);
    }

    private static StructurePiece createVillageHouse4Garden(StructureStart start, Random random,
            ReleaseOneWorldGenerator generator, int x, int y, int z, int mode) {
        StructureBoundingBox box = componentToAddBoundingBox(x, y, z, 0, 0, 0, 5, 6, 5, mode);
        if (intersectsAny(start.pieces(), box)) {
            return null;
        }
        int placementMinY = villageAverageGroundY(generator, box);
        return new VillageHouse4GardenPiece(box, mode, placementMinY, random.nextBoolean());
    }

    private static StructurePiece createVillageChurch(StructureStart start, ReleaseOneWorldGenerator generator,
            int x, int y, int z, int mode) {
        StructureBoundingBox box = componentToAddBoundingBox(x, y, z, 0, 0, 0, 5, 12, 9, mode);
        if (box.minY() <= 10 || intersectsAny(start.pieces(), box)) {
            return null;
        }
        int placementMinY = villageAverageGroundY(generator, box);
        return new VillageChurchPiece(box, mode, placementMinY);
    }

    private static StructurePiece createVillageHouse1(StructureStart start, ReleaseOneWorldGenerator generator,
            int x, int y, int z, int mode) {
        StructureBoundingBox box = componentToAddBoundingBox(x, y, z, 0, 0, 0, 9, 9, 6, mode);
        if (box.minY() <= 10 || intersectsAny(start.pieces(), box)) {
            return null;
        }
        int placementMinY = villageAverageGroundY(generator, box);
        return new VillageHouse1Piece(box, mode, placementMinY);
    }

    private static StructurePiece createVillageHouse3(StructureStart start, ReleaseOneWorldGenerator generator,
            int x, int y, int z, int mode) {
        StructureBoundingBox box = componentToAddBoundingBox(x, y, z, 0, 0, 0, 9, 7, 12, mode);
        if (box.minY() <= 10 || intersectsAny(start.pieces(), box)) {
            return null;
        }
        int placementMinY = villageAverageGroundY(generator, box);
        return new VillageHouse3Piece(box, mode, placementMinY);
    }

    private static StructurePiece createVillageBlacksmith(StructureStart start, ReleaseOneWorldGenerator generator,
            int x, int y, int z, int mode) {
        StructureBoundingBox box = componentToAddBoundingBox(x, y, z, 0, 0, 0, 10, 6, 7, mode);
        if (box.minY() <= 10 || intersectsAny(start.pieces(), box)) {
            return null;
        }
        int placementMinY = villageAverageGroundY(generator, box);
        return new VillageBlacksmithPiece(box, mode, placementMinY);
    }

    private static StructurePiece createVillageFarm(StructureStart start, ReleaseOneWorldGenerator generator,
            int x, int y, int z, int mode, boolean wide) {
        StructureBoundingBox box = componentToAddBoundingBox(x, y, z, 0, 0, 0, wide ? 13 : 7, 4, 9, mode);
        if (box.minY() <= 10 || intersectsAny(start.pieces(), box)) {
            return null;
        }
        int placementMinY = villageAverageGroundY(generator, box);
        return new VillageFarmPiece(box, mode, placementMinY, wide);
    }

    private static boolean isImplementedVillagePiece(VillagePieceKind kind) {
        return kind == VillagePieceKind.HOUSE_4_GARDEN || kind == VillagePieceKind.WOOD_HUT
                || kind == VillagePieceKind.CHURCH || kind == VillagePieceKind.HOUSE_1
                || kind == VillagePieceKind.HOUSE_2 || kind == VillagePieceKind.HOUSE_3 || kind == VillagePieceKind.HALL
                || kind == VillagePieceKind.FIELD || kind == VillagePieceKind.FIELD_2;
    }

    private static StructurePiece createVillageTorch(StructureStart start, ReleaseOneWorldGenerator generator,
            int x, int y, int z, int mode) {
        StructureBoundingBox box = componentToAddBoundingBox(x, y, z, 0, 0, 0, 3, 4, 2, mode);
        if (intersectsAny(start.pieces(), box)) {
            return null;
        }
        int placementMinY = villageAverageGroundY(generator, box);
        return new VillageTorchPiece(box, mode, placementMinY);
    }

    private static List<VillagePieceWeight> villagePieceWeights(Random random, int terrainType) {
        ArrayList<VillagePieceWeight> weights = new ArrayList<>();
        weights.add(new VillagePieceWeight(VillagePieceKind.HOUSE_4_GARDEN, 4,
                randomIntegerInRange(random, 2 + terrainType, 4 + terrainType * 2)));
        weights.add(new VillagePieceWeight(VillagePieceKind.CHURCH, 20,
                randomIntegerInRange(random, terrainType, 1 + terrainType)));
        weights.add(new VillagePieceWeight(VillagePieceKind.HOUSE_1, 20,
                randomIntegerInRange(random, terrainType, 2 + terrainType)));
        weights.add(new VillagePieceWeight(VillagePieceKind.WOOD_HUT, 3,
                randomIntegerInRange(random, 2 + terrainType, 5 + terrainType * 3)));
        weights.add(new VillagePieceWeight(VillagePieceKind.HALL, 15,
                randomIntegerInRange(random, terrainType, 2 + terrainType)));
        weights.add(new VillagePieceWeight(VillagePieceKind.FIELD, 3,
                randomIntegerInRange(random, 1 + terrainType, 4 + terrainType)));
        weights.add(new VillagePieceWeight(VillagePieceKind.FIELD_2, 3,
                randomIntegerInRange(random, 2 + terrainType, 4 + terrainType * 2)));
        weights.add(new VillagePieceWeight(VillagePieceKind.HOUSE_2, 15,
                randomIntegerInRange(random, 0, 1 + terrainType)));
        weights.add(new VillagePieceWeight(VillagePieceKind.HOUSE_3, 8,
                randomIntegerInRange(random, terrainType, 3 + terrainType * 2)));
        weights.removeIf(weight -> weight.limit == 0);
        return weights;
    }

    private static Random villageStartRandom(long seed, int chunkX, int chunkZ) {
        Random random = mapGenStructureRandom(seed, chunkX, chunkZ);
        random.nextInt();
        return random;
    }

    private static int randomIntegerInRange(Random random, int min, int max) {
        return min >= max ? min : random.nextInt(max - min + 1) + min;
    }

    private static StructureBoundingBox componentToAddBoundingBox(int x, int y, int z, int offX, int offY, int offZ,
            int sizeX, int sizeY, int sizeZ, int mode) {
        return switch (mode) {
            case 2 -> new StructureBoundingBox(x + offX, y + offY, z - sizeZ + 1 + offZ,
                    x + sizeX - 1 + offX, y + sizeY - 1 + offY, z + offZ);
            case 1 -> new StructureBoundingBox(x - sizeZ + 1 + offZ, y + offY, z + offX,
                    x + offZ, y + sizeY - 1 + offY, z + sizeX - 1 + offX);
            case 3 -> new StructureBoundingBox(x + offZ, y + offY, z + offX,
                    x + sizeZ - 1 + offZ, y + sizeY - 1 + offY, z + sizeX - 1 + offX);
            default -> new StructureBoundingBox(x + offX, y + offY, z + offZ,
                    x + sizeX - 1 + offX, y + sizeY - 1 + offY, z + sizeZ - 1 + offZ);
        };
    }

    private static boolean intersectsAny(List<StructurePiece> pieces, StructureBoundingBox box) {
        for (StructurePiece piece : pieces) {
            if (piece.bounds().intersects(box)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isVillageBiomeViable(ReleaseOneWorldGenerator generator, StructureBoundingBox box) {
        int radius = Math.max(box.maxX() - box.minX(), box.maxZ() - box.minZ()) / 2 + 4;
        int centerX = box.centerX();
        int centerZ = box.centerZ();
        int minLayerX = (centerX - radius) >> 2;
        int minLayerZ = (centerZ - radius) >> 2;
        int maxLayerX = (centerX + radius) >> 2;
        int maxLayerZ = (centerZ + radius) >> 2;
        for (int layerX = minLayerX; layerX <= maxLayerX; layerX++) {
            for (int layerZ = minLayerZ; layerZ <= maxLayerZ; layerZ++) {
                if (!isVillageBiome(generator.getBiomeForGenerationLayer(layerX, layerZ))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isVillageBiome(BiomeType biome) {
        return biome == BiomeType.PLAINS || biome == BiomeType.DESERT;
    }

    private static boolean tooFarFromVillageStart(StructureBoundingBox startBox, int x, int z) {
        return Math.abs(x - startBox.minX()) > 112 || Math.abs(z - startBox.minZ()) > 112;
    }

    private static int villageAverageGroundY(ReleaseOneWorldGenerator generator, StructureBoundingBox box) {
        int total = 0;
        int count = 0;
        for (int z = box.minZ(); z <= box.maxZ(); z++) {
            for (int x = box.minX(); x <= box.maxX(); x++) {
                total += sourceVillageFoundationY(generator, x, z);
                count++;
            }
        }
        return count == 0 ? SOURCE_VILLAGE_FOUNDATION_MIN_Y : total / count;
    }

    private static int villageWellGroundY(ReleaseOneWorldGenerator generator, int minX, int minZ) {
        int total = 0;
        int count = 0;
        for (int z = minZ; z <= minZ + 5; z++) {
            for (int x = minX; x <= minX + 5; x++) {
                total += sourceVillageFoundationY(generator, x, z);
                count++;
            }
        }
        return count == 0 ? SOURCE_VILLAGE_FOUNDATION_MIN_Y : total / count;
    }

    private static int sourceVillageFoundationY(ReleaseOneWorldGenerator generator, int x, int z) {
        return Math.max(sourceFindTopSolidBlock(generator, x, z), SOURCE_VILLAGE_FOUNDATION_MIN_Y);
    }

    private static int sourceFindTopSolidBlock(ReleaseOneWorldGenerator generator, int x, int z) {
        return generator.terrainTopY(x, z) + 1;
    }

    private BiomeType villageBiome(long seed, int chunkX, int chunkZ, ReleaseOneWorldGenerator generator) {
        if (generator == null || !isVillageOrigin(seed, chunkX, chunkZ)) {
            return null;
        }
        int worldCenterX = chunkX * Chunk.WIDTH + 8;
        int worldCenterZ = chunkZ * Chunk.DEPTH + 8;
        BiomeType biome = generator.getBiomeForGenerationLayer(worldCenterX >> 2, worldCenterZ >> 2);
        return biome == BiomeType.PLAINS || biome == BiomeType.DESERT ? biome : null;
    }

    private static boolean isVillageOrigin(long seed, int chunkX, int chunkZ) {
        int originalX = chunkX;
        int originalZ = chunkZ;
        int regionX = chunkX;
        int regionZ = chunkZ;
        if (regionX < 0) {
            regionX -= VILLAGE_SPACING - 1;
        }
        if (regionZ < 0) {
            regionZ -= VILLAGE_SPACING - 1;
        }
        regionX /= VILLAGE_SPACING;
        regionZ /= VILLAGE_SPACING;

        Random random = setRandomSeed(seed, regionX, regionZ, VILLAGE_SALT);
        int candidateX = regionX * VILLAGE_SPACING + random.nextInt(VILLAGE_SPACING - VILLAGE_SEPARATION);
        int candidateZ = regionZ * VILLAGE_SPACING + random.nextInt(VILLAGE_SPACING - VILLAGE_SEPARATION);
        return originalX == candidateX && originalZ == candidateZ;
    }

    private static Random mapGenStructureRandom(long seed, int chunkX, int chunkZ) {
        Random random = new Random(seed);
        long xSeed = random.nextLong();
        long zSeed = random.nextLong();
        random.setSeed((long) chunkX * xSeed ^ (long) chunkZ * zSeed ^ seed);
        return random;
    }

    private static Random setRandomSeed(long seed, int x, int z, int salt) {
        return new Random((long) x * 341873128712L + (long) z * 132897987541L + salt + seed);
    }

    private static double distanceSq(int x, int z, int ox, int oz) {
        long dx = x - ox;
        long dz = z - oz;
        return dx * dx + dz * dz;
    }

    private enum StrongholdRoom {
        START, STRAIGHT, STAIRS, STAIRS_STRAIGHT, LEFT_TURN, RIGHT_TURN, CROSSING_HALL, CORRIDOR, CROSSING, LIBRARY,
        CHEST_CORRIDOR, PRISON, PORTAL
    }

    private enum StrongholdDoor {
        OPENING, WOOD_DOOR, GRATES, IRON_DOOR
    }

    private record StrongholdAccessPoint(int x, int y, int z, int mode) {
    }

    private record StrongholdBranchNode(StrongholdBoxPiece piece, int depth) {
    }

    private static final class StrongholdGenerationContext {
        private final StructureBoundingBox startBox;
        private final ArrayList<StrongholdPieceWeight> weights = new ArrayList<>();
        private StrongholdPieceWeight previousWeight;

        private StrongholdGenerationContext(StructureBoundingBox startBox, List<StrongholdBoxPiece> existingPieces) {
            this.startBox = startBox;
            weights.add(new StrongholdPieceWeight(StrongholdRoom.STRAIGHT, 40, 0, false));
            weights.add(new StrongholdPieceWeight(StrongholdRoom.PRISON, 5, 5, false));
            weights.add(new StrongholdPieceWeight(StrongholdRoom.LEFT_TURN, 20, 0, false));
            weights.add(new StrongholdPieceWeight(StrongholdRoom.RIGHT_TURN, 20, 0, false));
            weights.add(new StrongholdPieceWeight(StrongholdRoom.CROSSING, 10, 6, false));
            weights.add(new StrongholdPieceWeight(StrongholdRoom.STAIRS_STRAIGHT, 5, 5, false));
            weights.add(new StrongholdPieceWeight(StrongholdRoom.STAIRS, 5, 5, false));
            weights.add(new StrongholdPieceWeight(StrongholdRoom.CROSSING_HALL, 5, 4, false));
            weights.add(new StrongholdPieceWeight(StrongholdRoom.CHEST_CORRIDOR, 5, 4, false));
            weights.add(new StrongholdPieceWeight(StrongholdRoom.LIBRARY, 10, 2, false, 5));
            weights.add(new StrongholdPieceWeight(StrongholdRoom.PORTAL, 20, 1, false, 6));

            for (StrongholdBoxPiece piece : existingPieces) {
                if (!piece.countsForWeight) {
                    continue;
                }
                for (StrongholdPieceWeight weight : weights) {
                    if (weight.room == piece.room) {
                        weight.used++;
                        break;
                    }
                }
            }
        }
    }

    private static final class StrongholdPieceWeight {
        private final StrongholdRoom room;
        private final int weight;
        private final int maxCount;
        private final boolean allowRepeat;
        private final int minimumDepth;
        private int used;

        private StrongholdPieceWeight(StrongholdRoom room, int weight, int maxCount, boolean allowRepeat) {
            this(room, weight, maxCount, allowRepeat, 0);
        }

        private StrongholdPieceWeight(StrongholdRoom room, int weight, int maxCount, boolean allowRepeat,
                int minimumDepth) {
            this.room = room;
            this.weight = weight;
            this.maxCount = maxCount;
            this.allowRepeat = allowRepeat;
            this.minimumDepth = minimumDepth;
        }

        private boolean canSpawn() {
            return maxCount == 0 || used < maxCount;
        }

        private boolean canSpawnAtDepth(int depth) {
            return depth >= minimumDepth;
        }
    }

    private enum FortressRoom {
        BRIDGE,
        CROSSING,
        SMALL_CROSSING,
        STAIRS,
        BLAZE_PLATFORM,
        ENTRANCE,
        CORRIDOR_5,
        CROSSING_2,
        CORRIDOR_2,
        CORRIDOR,
        CORRIDOR_3,
        CORRIDOR_4,
        WART_ROOM,
        END
    }

    private enum MineshaftKind {
        CORRIDOR, CROSS, STAIRS
    }

    private enum VillagePieceKind {
        HOUSE_4_GARDEN,
        CHURCH,
        HOUSE_1,
        WOOD_HUT,
        HALL,
        FIELD,
        FIELD_2,
        HOUSE_2,
        HOUSE_3
    }

    private static final class VillagePieceWeight {
        private final VillagePieceKind kind;
        private final int weight;
        private final int limit;
        private int spawned;

        private VillagePieceWeight(VillagePieceKind kind, int weight, int limit) {
            this.kind = kind;
            this.weight = weight;
            this.limit = limit;
        }

        private boolean canSpawnMore() {
            return limit == 0 || spawned < limit;
        }
    }

    private static final class VillageGenerationContext {
        private final StructureBoundingBox startBox;
        private final List<VillagePieceWeight> weights;
        private final ArrayList<VillagePathPiece> pendingPaths = new ArrayList<>();
        private final ArrayList<StructurePiece> pendingBuildings = new ArrayList<>();
        private VillagePieceWeight previousWeight;

        private VillageGenerationContext(StructureBoundingBox startBox, List<VillagePieceWeight> weights) {
            this.startBox = startBox;
            this.weights = weights;
        }
    }

    private record MineshaftDescriptor(MineshaftKind kind, StructureBoundingBox bounds, int mode, int depth,
            int sections, boolean hasRails, boolean hasSpiders, boolean multipleFloors) {
        static MineshaftDescriptor corridor(StructureBoundingBox bounds, int mode, int depth, int sections,
                boolean hasRails, boolean hasSpiders) {
            return new MineshaftDescriptor(MineshaftKind.CORRIDOR, bounds, mode, depth, sections, hasRails,
                    hasSpiders, false);
        }

        static MineshaftDescriptor cross(StructureBoundingBox bounds, int mode, int depth, boolean multipleFloors) {
            return new MineshaftDescriptor(MineshaftKind.CROSS, bounds, mode, depth, 0, false, false,
                    multipleFloors);
        }

        static MineshaftDescriptor stairs(StructureBoundingBox bounds, int mode, int depth) {
            return new MineshaftDescriptor(MineshaftKind.STAIRS, bounds, mode, depth, 0, false, false, false);
        }

        StructurePiece toPiece(int yOffset) {
            StructureBoundingBox shifted = offset(bounds, 0, yOffset, 0);
            return switch (kind) {
                case CORRIDOR -> new MineshaftPiece(shifted.minX(), shifted.minY(), shifted.minZ(),
                        shifted.maxX(), shifted.maxY(), shifted.maxZ(), mode, sections, hasRails, hasSpiders);
                case CROSS -> new MineshaftCrossPiece(shifted, multipleFloors);
                case STAIRS -> new MineshaftStairsPiece(shifted, mode);
            };
        }
    }

    private static final class FortressGenerationContext {
        private final StructureBoundingBox startBox;
        private final ArrayList<NetherFortressPiece> pending = new ArrayList<>();
        private final ArrayList<FortressPieceWeight> primaryWeights = new ArrayList<>();
        private final ArrayList<FortressPieceWeight> secondaryWeights = new ArrayList<>();
        private FortressPieceWeight lastWeight;

        FortressGenerationContext(StructureBoundingBox startBox) {
            this.startBox = startBox;
            primaryWeights.add(new FortressPieceWeight(FortressRoom.BRIDGE, 30, 0, true));
            primaryWeights.add(new FortressPieceWeight(FortressRoom.CROSSING, 10, 4, false));
            primaryWeights.add(new FortressPieceWeight(FortressRoom.SMALL_CROSSING, 10, 4, false));
            primaryWeights.add(new FortressPieceWeight(FortressRoom.STAIRS, 10, 3, false));
            primaryWeights.add(new FortressPieceWeight(FortressRoom.BLAZE_PLATFORM, 5, 2, false));
            primaryWeights.add(new FortressPieceWeight(FortressRoom.ENTRANCE, 5, 1, false));

            secondaryWeights.add(new FortressPieceWeight(FortressRoom.CORRIDOR_5, 25, 0, true));
            secondaryWeights.add(new FortressPieceWeight(FortressRoom.CROSSING_2, 15, 5, false));
            secondaryWeights.add(new FortressPieceWeight(FortressRoom.CORRIDOR_2, 5, 10, false));
            secondaryWeights.add(new FortressPieceWeight(FortressRoom.CORRIDOR, 5, 10, false));
            secondaryWeights.add(new FortressPieceWeight(FortressRoom.CORRIDOR_3, 10, 3, true));
            secondaryWeights.add(new FortressPieceWeight(FortressRoom.CORRIDOR_4, 7, 2, false));
            secondaryWeights.add(new FortressPieceWeight(FortressRoom.WART_ROOM, 5, 2, false));
        }
    }

    private static final class FortressPieceWeight {
        private final FortressRoom room;
        private final int weight;
        private final int maxCount;
        private final boolean allowRepeat;
        private int used;

        FortressPieceWeight(FortressRoom room, int weight, int maxCount, boolean allowRepeat) {
            this.room = room;
            this.weight = weight;
            this.maxCount = maxCount;
            this.allowRepeat = allowRepeat;
        }

        boolean canSpawn() {
            return maxCount == 0 || used < maxCount;
        }

        boolean canContinue() {
            return maxCount == 0 || used < maxCount;
        }
    }

    private static class StrongholdBoxPiece extends BoxPiece {
        private final StrongholdRoom room;
        private final int coordBaseMode;
        private final boolean expandsX;
        private final boolean expandsZ;
        private final boolean crossingLowerLeft;
        private final boolean crossingUpperLeft;
        private final boolean crossingLowerRight;
        private final boolean crossingUpperRight;
        private final StrongholdDoor doorType;
        private final int roomCrossingType;
        private final boolean countsForWeight;

        StrongholdBoxPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room) {
            this(minX, minY, minZ, maxX, maxY, maxZ, room, -1);
        }

        StrongholdBoxPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room,
                int coordBaseMode) {
            this(minX, minY, minZ, maxX, maxY, maxZ, room, coordBaseMode, false, false);
        }

        StrongholdBoxPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room,
                int coordBaseMode, StrongholdDoor doorType) {
            this(minX, minY, minZ, maxX, maxY, maxZ, room, coordBaseMode, false, false, doorType);
        }

        StrongholdBoxPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room,
                int coordBaseMode, StrongholdDoor doorType, int roomCrossingType) {
            this(minX, minY, minZ, maxX, maxY, maxZ, room, coordBaseMode, false, false, false, false, false,
                    false, doorType, roomCrossingType);
        }

        StrongholdBoxPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room,
                int coordBaseMode, boolean expandsX, boolean expandsZ) {
            this(minX, minY, minZ, maxX, maxY, maxZ, room, coordBaseMode, expandsX, expandsZ,
                    StrongholdDoor.OPENING);
        }

        StrongholdBoxPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room,
                int coordBaseMode, boolean expandsX, boolean expandsZ, StrongholdDoor doorType) {
            this(minX, minY, minZ, maxX, maxY, maxZ, room, coordBaseMode, expandsX, expandsZ, false, false, false,
                    false, doorType, -1);
        }

        StrongholdBoxPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room,
                int coordBaseMode, boolean expandsX, boolean expandsZ, boolean crossingLowerLeft,
                boolean crossingUpperLeft, boolean crossingLowerRight, boolean crossingUpperRight) {
            this(minX, minY, minZ, maxX, maxY, maxZ, room, coordBaseMode, expandsX, expandsZ, crossingLowerLeft,
                    crossingUpperLeft, crossingLowerRight, crossingUpperRight, StrongholdDoor.OPENING, -1);
        }

        StrongholdBoxPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room,
                int coordBaseMode, boolean expandsX, boolean expandsZ, boolean crossingLowerLeft,
                boolean crossingUpperLeft, boolean crossingLowerRight, boolean crossingUpperRight,
                StrongholdDoor doorType, int roomCrossingType) {
            this(minX, minY, minZ, maxX, maxY, maxZ, room, coordBaseMode, expandsX, expandsZ, crossingLowerLeft,
                    crossingUpperLeft, crossingLowerRight, crossingUpperRight, doorType, roomCrossingType, true);
        }

        StrongholdBoxPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StrongholdRoom room,
                int coordBaseMode, boolean expandsX, boolean expandsZ, boolean crossingLowerLeft,
                boolean crossingUpperLeft, boolean crossingLowerRight, boolean crossingUpperRight,
                StrongholdDoor doorType, int roomCrossingType, boolean countsForWeight) {
            super(minX, minY, minZ, maxX, maxY, maxZ);
            this.room = room;
            this.coordBaseMode = coordBaseMode;
            this.expandsX = expandsX;
            this.expandsZ = expandsZ;
            this.crossingLowerLeft = crossingLowerLeft;
            this.crossingUpperLeft = crossingUpperLeft;
            this.crossingLowerRight = crossingLowerRight;
            this.crossingUpperRight = crossingUpperRight;
            this.doorType = doorType == null ? StrongholdDoor.OPENING : doorType;
            this.roomCrossingType = roomCrossingType;
            this.countsForWeight = countsForWeight;
        }

        StrongholdBoxPiece offsetY(int yOffset) {
            return new StrongholdBoxPiece(bounds().minX(), bounds().minY() + yOffset, bounds().minZ(),
                    bounds().maxX(), bounds().maxY() + yOffset, bounds().maxZ(), room, coordBaseMode, expandsX,
                    expandsZ, crossingLowerLeft, crossingUpperLeft, crossingLowerRight, crossingUpperRight, doorType,
                    roomCrossingType, countsForWeight);
        }

        StrongholdBoxPiece withoutStrongholdWeightCount() {
            return new StrongholdBoxPiece(bounds().minX(), bounds().minY(), bounds().minZ(),
                    bounds().maxX(), bounds().maxY(), bounds().maxZ(), room, coordBaseMode, expandsX, expandsZ,
                    crossingLowerLeft, crossingUpperLeft, crossingLowerRight, crossingUpperRight, doorType,
                    roomCrossingType, false);
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            placeWithRandom(world, chunk, chunkX, chunkZ,
                    random(seed, bounds().minX(), bounds().minY(), bounds().minZ()));
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ, Random placementRandom) {
            Random random = placementRandom == null
                    ? random(seed, bounds().minX(), bounds().minY(), bounds().minZ())
                    : placementRandom;
            placeWithRandom(world, chunk, chunkX, chunkZ, random);
        }

        private void placeWithRandom(World world, Chunk chunk, int chunkX, int chunkZ, Random random) {
            if (room == StrongholdRoom.PORTAL) {
                buildPortalRoom(world, chunk, chunkX, chunkZ, random);
                return;
            }
            if (hasLiquidInChunkEnvelope(chunk, chunkX, chunkZ)) {
                return;
            }
            if (room == StrongholdRoom.CHEST_CORRIDOR) {
                buildChestCorridor(world, chunk, chunkX, chunkZ, random);
                return;
            }
            if (room == StrongholdRoom.PRISON) {
                buildPrisonRoom(chunk, chunkX, chunkZ, random);
                return;
            }
            if (room == StrongholdRoom.LIBRARY) {
                buildLibrary(world, chunk, chunkX, chunkZ, random);
                return;
            }
            if (room == StrongholdRoom.CROSSING) {
                buildRoomCrossing(world, chunk, chunkX, chunkZ, random);
                return;
            }
            if (room == StrongholdRoom.CORRIDOR) {
                buildCorridor(chunk, chunkX, chunkZ, random);
                return;
            }
            if (room == StrongholdRoom.START) {
                buildSourceStairs(chunk, chunkX, chunkZ, random, false);
                return;
            }
            if (room == StrongholdRoom.STAIRS) {
                buildSourceStairs(chunk, chunkX, chunkZ, random, true);
                return;
            }
            if (room == StrongholdRoom.STRAIGHT) {
                buildStraight(chunk, chunkX, chunkZ, random);
                return;
            }
            if (room == StrongholdRoom.STAIRS_STRAIGHT) {
                buildStairsStraight(chunk, chunkX, chunkZ, random);
                return;
            }
            if (room == StrongholdRoom.LEFT_TURN || room == StrongholdRoom.RIGHT_TURN) {
                buildTurn(chunk, chunkX, chunkZ, random);
                return;
            }
            if (room == StrongholdRoom.CROSSING_HALL) {
                buildCrossingHall(chunk, chunkX, chunkZ, random);
                return;
            }
            strongholdShell(chunk, chunkX, chunkZ, random);
        }

        private void buildPortalRoom(World world, Chunk chunk, int chunkX, int chunkZ, Random random) {
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 0, 0, 0, 10, 7, 15);
            placeGrateDoorLocal(chunk, chunkX, chunkZ, 4, 1, 0);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 1, 6, 1, 1, 6, 14);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 9, 6, 1, 9, 6, 14);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 2, 6, 1, 8, 6, 2);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 2, 6, 14, 8, 6, 14);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 1, 1, 1, 2, 1, 4);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 8, 1, 1, 9, 1, 4);
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 1, 1, 1, 3, BlockType.FLOWING_LAVA);
            fillLocal(chunk, chunkX, chunkZ, 9, 1, 1, 9, 1, 3, BlockType.FLOWING_LAVA);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 3, 1, 8, 7, 1, 12);
            fillLocal(chunk, chunkX, chunkZ, 4, 1, 9, 6, 1, 11, BlockType.FLOWING_LAVA);

            for (int z = 3; z < 14; z += 2) {
                fillLocal(chunk, chunkX, chunkZ, 0, 3, z, 0, 4, z, BlockType.IRON_BARS);
                fillLocal(chunk, chunkX, chunkZ, 10, 3, z, 10, 4, z, BlockType.IRON_BARS);
            }
            for (int x = 2; x < 9; x += 2) {
                fillLocal(chunk, chunkX, chunkZ, x, 3, 15, x, 4, 15, BlockType.IRON_BARS);
            }

            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 4, 1, 5, 6, 1, 7);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 4, 2, 6, 6, 2, 7);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 4, 3, 7, 6, 3, 7);
            int stairMetadata = strongholdStairMetadata(3);
            for (int x = 4; x <= 6; x++) {
                setLocal(chunk, chunkX, chunkZ, x, 1, 4, BlockType.STONE_BRICK_STAIRS, stairMetadata);
                setLocal(chunk, chunkX, chunkZ, x, 2, 5, BlockType.STONE_BRICK_STAIRS, stairMetadata);
                setLocal(chunk, chunkX, chunkZ, x, 3, 6, BlockType.STONE_BRICK_STAIRS, stairMetadata);
            }

            int[] frameMetadata = portalFrameMetadata();
            setFrameLocal(chunk, chunkX, chunkZ, 4, 3, 8, frameMetadata[0], random);
            setFrameLocal(chunk, chunkX, chunkZ, 5, 3, 8, frameMetadata[0], random);
            setFrameLocal(chunk, chunkX, chunkZ, 6, 3, 8, frameMetadata[0], random);
            setFrameLocal(chunk, chunkX, chunkZ, 4, 3, 12, frameMetadata[1], random);
            setFrameLocal(chunk, chunkX, chunkZ, 5, 3, 12, frameMetadata[1], random);
            setFrameLocal(chunk, chunkX, chunkZ, 6, 3, 12, frameMetadata[1], random);
            setFrameLocal(chunk, chunkX, chunkZ, 3, 3, 9, frameMetadata[2], random);
            setFrameLocal(chunk, chunkX, chunkZ, 3, 3, 10, frameMetadata[2], random);
            setFrameLocal(chunk, chunkX, chunkZ, 3, 3, 11, frameMetadata[2], random);
            setFrameLocal(chunk, chunkX, chunkZ, 7, 3, 9, frameMetadata[3], random);
            setFrameLocal(chunk, chunkX, chunkZ, 7, 3, 10, frameMetadata[3], random);
            setFrameLocal(chunk, chunkX, chunkZ, 7, 3, 11, frameMetadata[3], random);

            StructureGenerator.placeSpawner(world, chunk, chunkX, chunkZ,
                    localX(5, 6), localY(3), localZ(5, 6), MobDefinition.SILVERFISH, random);
        }

        private void buildChestCorridor(World world, Chunk chunk, int chunkX, int chunkZ, Random random) {
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 0, 0, 0, 4, 4, 6);
            placeSourceDoorLocal(chunk, chunkX, chunkZ, doorType, 1, 1, 0);
            placeOpeningLocal(chunk, chunkX, chunkZ, 1, 1, 6);
            fillLocal(chunk, chunkX, chunkZ, 3, 1, 2, 3, 1, 4, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 3, 1, 1, BlockType.STONE_SLAB, 5);
            setLocal(chunk, chunkX, chunkZ, 3, 1, 5, BlockType.STONE_SLAB, 5);
            setLocal(chunk, chunkX, chunkZ, 3, 2, 2, BlockType.STONE_SLAB, 5);
            setLocal(chunk, chunkX, chunkZ, 3, 2, 4, BlockType.STONE_SLAB, 5);
            for (int z = 2; z <= 4; z++) {
                setLocal(chunk, chunkX, chunkZ, 2, 1, z, BlockType.STONE_SLAB, 5);
            }
            StructureGenerator.placeWeightedLootChest(world, chunk, chunkX, chunkZ, localX(3, 3), localY(2),
                    localZ(3, 3), random, 2 + random.nextInt(2), STRONGHOLD_CHEST_CORRIDOR_LOOT);
        }

        private void buildPrisonRoom(Chunk chunk, int chunkX, int chunkZ, Random random) {
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 0, 0, 0, 8, 4, 10);
            placeSourceDoorLocal(chunk, chunkX, chunkZ, doorType, 1, 1, 0);
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 10, 3, 3, 10, BlockType.AIR);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 4, 1, 1, 4, 3, 1);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 4, 1, 3, 4, 3, 3);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 4, 1, 7, 4, 3, 7);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 4, 1, 9, 4, 3, 9);
            fillLocal(chunk, chunkX, chunkZ, 4, 1, 4, 4, 3, 6, BlockType.IRON_BARS);
            fillLocal(chunk, chunkX, chunkZ, 5, 1, 5, 7, 3, 5, BlockType.IRON_BARS);
            setLocal(chunk, chunkX, chunkZ, 4, 3, 2, BlockType.IRON_BARS);
            setLocal(chunk, chunkX, chunkZ, 4, 3, 8, BlockType.IRON_BARS);

            int doorMetadata = strongholdDoorMetadata(3);
            setLocal(chunk, chunkX, chunkZ, 4, 1, 2, BlockType.IRON_DOOR, doorMetadata);
            setLocal(chunk, chunkX, chunkZ, 4, 2, 2, BlockType.IRON_DOOR, doorMetadata + 8);
            setLocal(chunk, chunkX, chunkZ, 4, 1, 8, BlockType.IRON_DOOR, doorMetadata);
            setLocal(chunk, chunkX, chunkZ, 4, 2, 8, BlockType.IRON_DOOR, doorMetadata + 8);
        }

        private void buildLibrary(World world, Chunk chunk, int chunkX, int chunkZ, Random random) {
            boolean largeRoom = bounds().maxY() - bounds().minY() + 1 > 6;
            int maxY = largeRoom ? 10 : 5;
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 0, 0, 0, 13, maxY, 14);
            placeSourceDoorLocal(chunk, chunkX, chunkZ, doorType, 4, 1, 0);
            randomlyFillLocal(chunk, chunkX, chunkZ, random, 0.07f, 2, 1, 1, 11, 4, 13,
                    BlockType.COBWEB);

            for (int z = 1; z <= 13; z++) {
                if ((z - 1) % 4 == 0) {
                    fillLocal(chunk, chunkX, chunkZ, 1, 1, z, 1, 4, z, BlockType.OAK_PLANKS);
                    fillLocal(chunk, chunkX, chunkZ, 12, 1, z, 12, 4, z, BlockType.OAK_PLANKS);
                    setLocal(chunk, chunkX, chunkZ, 2, 3, z, BlockType.TORCH);
                    setLocal(chunk, chunkX, chunkZ, 11, 3, z, BlockType.TORCH);
                    if (largeRoom) {
                        fillLocal(chunk, chunkX, chunkZ, 1, 6, z, 1, 9, z, BlockType.OAK_PLANKS);
                        fillLocal(chunk, chunkX, chunkZ, 12, 6, z, 12, 9, z, BlockType.OAK_PLANKS);
                    }
                } else {
                    fillLocal(chunk, chunkX, chunkZ, 1, 1, z, 1, 4, z, BlockType.BOOKSHELF);
                    fillLocal(chunk, chunkX, chunkZ, 12, 1, z, 12, 4, z, BlockType.BOOKSHELF);
                    if (largeRoom) {
                        fillLocal(chunk, chunkX, chunkZ, 1, 6, z, 1, 9, z, BlockType.BOOKSHELF);
                        fillLocal(chunk, chunkX, chunkZ, 12, 6, z, 12, 9, z, BlockType.BOOKSHELF);
                    }
                }
            }

            for (int z = 3; z < 12; z += 2) {
                fillLocal(chunk, chunkX, chunkZ, 3, 1, z, 4, 3, z, BlockType.BOOKSHELF);
                fillLocal(chunk, chunkX, chunkZ, 6, 1, z, 7, 3, z, BlockType.BOOKSHELF);
                fillLocal(chunk, chunkX, chunkZ, 9, 1, z, 10, 3, z, BlockType.BOOKSHELF);
            }

            if (largeRoom) {
                fillLocal(chunk, chunkX, chunkZ, 1, 5, 1, 3, 5, 13, BlockType.OAK_PLANKS);
                fillLocal(chunk, chunkX, chunkZ, 10, 5, 1, 12, 5, 13, BlockType.OAK_PLANKS);
                fillLocal(chunk, chunkX, chunkZ, 4, 5, 1, 9, 5, 2, BlockType.OAK_PLANKS);
                fillLocal(chunk, chunkX, chunkZ, 4, 5, 12, 9, 5, 13, BlockType.OAK_PLANKS);
                setLocal(chunk, chunkX, chunkZ, 9, 5, 11, BlockType.OAK_PLANKS);
                setLocal(chunk, chunkX, chunkZ, 8, 5, 11, BlockType.OAK_PLANKS);
                setLocal(chunk, chunkX, chunkZ, 9, 5, 10, BlockType.OAK_PLANKS);
                fillLocal(chunk, chunkX, chunkZ, 3, 6, 2, 3, 6, 12, BlockType.FENCE);
                fillLocal(chunk, chunkX, chunkZ, 10, 6, 2, 10, 6, 10, BlockType.FENCE);
                fillLocal(chunk, chunkX, chunkZ, 4, 6, 2, 9, 6, 2, BlockType.FENCE);
                fillLocal(chunk, chunkX, chunkZ, 4, 6, 12, 8, 6, 12, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, 9, 6, 11, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, 8, 6, 11, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, 9, 6, 10, BlockType.FENCE);

                int ladderMetadata = strongholdLadderMetadata(3);
                for (int y = 1; y <= 7; y++) {
                    setLocal(chunk, chunkX, chunkZ, 10, y, 13, BlockType.LADDER, ladderMetadata);
                }

                int centerX = 7;
                int centerZ = 7;
                setLocal(chunk, chunkX, chunkZ, centerX - 1, 9, centerZ, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, centerX, 9, centerZ, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, centerX - 1, 8, centerZ, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, centerX, 8, centerZ, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, centerX - 1, 7, centerZ, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, centerX, 7, centerZ, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, centerX - 2, 7, centerZ, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, centerX + 1, 7, centerZ, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, centerX - 1, 7, centerZ - 1, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, centerX - 1, 7, centerZ + 1, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, centerX, 7, centerZ - 1, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, centerX, 7, centerZ + 1, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, centerX - 2, 8, centerZ, BlockType.TORCH);
                setLocal(chunk, chunkX, chunkZ, centerX + 1, 8, centerZ, BlockType.TORCH);
                setLocal(chunk, chunkX, chunkZ, centerX - 1, 8, centerZ - 1, BlockType.TORCH);
                setLocal(chunk, chunkX, chunkZ, centerX - 1, 8, centerZ + 1, BlockType.TORCH);
                setLocal(chunk, chunkX, chunkZ, centerX, 8, centerZ - 1, BlockType.TORCH);
                setLocal(chunk, chunkX, chunkZ, centerX, 8, centerZ + 1, BlockType.TORCH);
            }

            StructureGenerator.placeWeightedLootChest(world, chunk, chunkX, chunkZ, localX(3, 5), localY(3),
                    localZ(3, 5), random, 1 + random.nextInt(4), STRONGHOLD_LIBRARY_LOOT);
            if (largeRoom) {
                setLocal(chunk, chunkX, chunkZ, 12, 9, 1, BlockType.AIR);
                StructureGenerator.placeWeightedLootChest(world, chunk, chunkX, chunkZ, localX(12, 1), localY(8),
                        localZ(12, 1), random, 1 + random.nextInt(4), STRONGHOLD_LIBRARY_LOOT);
            }
        }

        private void buildRoomCrossing(World world, Chunk chunk, int chunkX, int chunkZ, Random random) {
            int roomType = roomCrossingType >= 0 ? roomCrossingType : random.nextInt(5);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 0, 0, 0, 10, 6, 10);
            placeSourceDoorLocal(chunk, chunkX, chunkZ, doorType, 4, 1, 0);
            fillLocal(chunk, chunkX, chunkZ, 4, 1, 10, 6, 3, 10, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 0, 1, 4, 0, 3, 6, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 10, 1, 4, 10, 3, 6, BlockType.AIR);

            switch (roomType) {
                case 0 -> buildRoomCrossingPillar(chunk, chunkX, chunkZ);
                case 1 -> buildRoomCrossingFountain(chunk, chunkX, chunkZ);
                case 2 -> buildRoomCrossingBalcony(world, chunk, chunkX, chunkZ, random);
                default -> {
                }
            }
        }

        private void buildRoomCrossingPillar(Chunk chunk, int chunkX, int chunkZ) {
            setLocal(chunk, chunkX, chunkZ, 5, 1, 5, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 5, 2, 5, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 5, 3, 5, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 4, 3, 5, BlockType.TORCH);
            setLocal(chunk, chunkX, chunkZ, 6, 3, 5, BlockType.TORCH);
            setLocal(chunk, chunkX, chunkZ, 5, 3, 4, BlockType.TORCH);
            setLocal(chunk, chunkX, chunkZ, 5, 3, 6, BlockType.TORCH);
            setLocal(chunk, chunkX, chunkZ, 4, 1, 4, BlockType.STONE_SLAB);
            setLocal(chunk, chunkX, chunkZ, 4, 1, 5, BlockType.STONE_SLAB);
            setLocal(chunk, chunkX, chunkZ, 4, 1, 6, BlockType.STONE_SLAB);
            setLocal(chunk, chunkX, chunkZ, 6, 1, 4, BlockType.STONE_SLAB);
            setLocal(chunk, chunkX, chunkZ, 6, 1, 5, BlockType.STONE_SLAB);
            setLocal(chunk, chunkX, chunkZ, 6, 1, 6, BlockType.STONE_SLAB);
            setLocal(chunk, chunkX, chunkZ, 5, 1, 4, BlockType.STONE_SLAB);
            setLocal(chunk, chunkX, chunkZ, 5, 1, 6, BlockType.STONE_SLAB);
        }

        private void buildRoomCrossingFountain(Chunk chunk, int chunkX, int chunkZ) {
            for (int i = 0; i < 5; i++) {
                setLocal(chunk, chunkX, chunkZ, 3, 1, 3 + i, BlockType.STONE_BRICK);
                setLocal(chunk, chunkX, chunkZ, 7, 1, 3 + i, BlockType.STONE_BRICK);
                setLocal(chunk, chunkX, chunkZ, 3 + i, 1, 3, BlockType.STONE_BRICK);
                setLocal(chunk, chunkX, chunkZ, 3 + i, 1, 7, BlockType.STONE_BRICK);
            }
            setLocal(chunk, chunkX, chunkZ, 5, 1, 5, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 5, 2, 5, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 5, 3, 5, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 5, 4, 5, BlockType.FLOWING_WATER);
        }

        private void buildRoomCrossingBalcony(World world, Chunk chunk, int chunkX, int chunkZ, Random random) {
            for (int z = 1; z <= 9; z++) {
                setLocal(chunk, chunkX, chunkZ, 1, 3, z, BlockType.COBBLESTONE);
                setLocal(chunk, chunkX, chunkZ, 9, 3, z, BlockType.COBBLESTONE);
            }
            for (int x = 1; x <= 9; x++) {
                setLocal(chunk, chunkX, chunkZ, x, 3, 1, BlockType.COBBLESTONE);
                setLocal(chunk, chunkX, chunkZ, x, 3, 9, BlockType.COBBLESTONE);
            }
            setLocal(chunk, chunkX, chunkZ, 5, 1, 4, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 5, 1, 6, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 5, 3, 4, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 5, 3, 6, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 4, 1, 5, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 6, 1, 5, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 4, 3, 5, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 6, 3, 5, BlockType.COBBLESTONE);
            for (int y = 1; y <= 3; y++) {
                setLocal(chunk, chunkX, chunkZ, 4, y, 4, BlockType.COBBLESTONE);
                setLocal(chunk, chunkX, chunkZ, 6, y, 4, BlockType.COBBLESTONE);
                setLocal(chunk, chunkX, chunkZ, 4, y, 6, BlockType.COBBLESTONE);
                setLocal(chunk, chunkX, chunkZ, 6, y, 6, BlockType.COBBLESTONE);
            }
            setLocal(chunk, chunkX, chunkZ, 5, 3, 5, BlockType.TORCH);

            for (int z = 2; z <= 8; z++) {
                setLocal(chunk, chunkX, chunkZ, 2, 3, z, BlockType.OAK_PLANKS);
                setLocal(chunk, chunkX, chunkZ, 3, 3, z, BlockType.OAK_PLANKS);
                if (z <= 3 || z >= 7) {
                    setLocal(chunk, chunkX, chunkZ, 4, 3, z, BlockType.OAK_PLANKS);
                    setLocal(chunk, chunkX, chunkZ, 5, 3, z, BlockType.OAK_PLANKS);
                    setLocal(chunk, chunkX, chunkZ, 6, 3, z, BlockType.OAK_PLANKS);
                }
                setLocal(chunk, chunkX, chunkZ, 7, 3, z, BlockType.OAK_PLANKS);
                setLocal(chunk, chunkX, chunkZ, 8, 3, z, BlockType.OAK_PLANKS);
            }

            int ladderMetadata = strongholdLadderMetadata(4);
            setLocal(chunk, chunkX, chunkZ, 9, 1, 3, BlockType.LADDER, ladderMetadata);
            setLocal(chunk, chunkX, chunkZ, 9, 2, 3, BlockType.LADDER, ladderMetadata);
            setLocal(chunk, chunkX, chunkZ, 9, 3, 3, BlockType.LADDER, ladderMetadata);
            StructureGenerator.placeWeightedLootChest(world, chunk, chunkX, chunkZ, localX(3, 8), localY(4),
                    localZ(3, 8), random, 1 + random.nextInt(4), STRONGHOLD_ROOM_CROSSING_LOOT);
        }

        private void buildStraight(Chunk chunk, int chunkX, int chunkZ, Random random) {
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 0, 0, 0, 4, 4, 6);
            placeSourceDoorLocal(chunk, chunkX, chunkZ, doorType, 1, 1, 0);
            placeOpeningLocal(chunk, chunkX, chunkZ, 1, 1, 6);
            randomlySetLocal(chunk, chunkX, chunkZ, random, 0.1f, 1, 2, 1, BlockType.TORCH);
            randomlySetLocal(chunk, chunkX, chunkZ, random, 0.1f, 3, 2, 1, BlockType.TORCH);
            randomlySetLocal(chunk, chunkX, chunkZ, random, 0.1f, 1, 2, 5, BlockType.TORCH);
            randomlySetLocal(chunk, chunkX, chunkZ, random, 0.1f, 3, 2, 5, BlockType.TORCH);
            if (expandsX) {
                fillLocal(chunk, chunkX, chunkZ, 0, 1, 2, 0, 3, 4, BlockType.AIR);
            }
            if (expandsZ) {
                fillLocal(chunk, chunkX, chunkZ, 4, 1, 2, 4, 3, 4, BlockType.AIR);
            }
        }

        private void buildTurn(Chunk chunk, int chunkX, int chunkZ, Random random) {
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 0, 0, 0, 4, 4, 4);
            placeSourceDoorLocal(chunk, chunkX, chunkZ, doorType, 1, 1, 0);
            boolean orientedXSide = coordBaseMode == 2 || coordBaseMode == 3;
            boolean leftTurn = room == StrongholdRoom.LEFT_TURN;
            boolean openLowX = (leftTurn && orientedXSide) || (!leftTurn && !orientedXSide);
            int sideX = openLowX ? 0 : 4;
            fillLocal(chunk, chunkX, chunkZ, sideX, 1, 1, sideX, 3, 3, BlockType.AIR);
        }

        private void buildStairsStraight(Chunk chunk, int chunkX, int chunkZ, Random random) {
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 0, 0, 0, 4, 10, 7);
            placeSourceDoorLocal(chunk, chunkX, chunkZ, doorType, 1, 7, 0);
            placeOpeningLocal(chunk, chunkX, chunkZ, 1, 1, 7);
            int stairMetadata = strongholdStairMetadata(2);
            for (int step = 0; step < 6; step++) {
                int y = 6 - step;
                int z = 1 + step;
                for (int x = 1; x <= 3; x++) {
                    setLocal(chunk, chunkX, chunkZ, x, y, z, BlockType.COBBLESTONE_STAIRS, stairMetadata);
                    if (step < 5) {
                        setLocal(chunk, chunkX, chunkZ, x, y - 1, z, BlockType.STONE_BRICK);
                    }
                }
            }
        }

        private void buildCrossingHall(Chunk chunk, int chunkX, int chunkZ, Random random) {
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 0, 0, 0, 9, 8, 10);
            placeSourceDoorLocal(chunk, chunkX, chunkZ, doorType, 4, 3, 0);
            if (crossingLowerLeft) {
                fillLocal(chunk, chunkX, chunkZ, 0, 3, 1, 0, 5, 3, BlockType.AIR);
            }
            if (crossingUpperLeft) {
                fillLocal(chunk, chunkX, chunkZ, 0, 5, 7, 0, 7, 9, BlockType.AIR);
            }
            if (crossingLowerRight) {
                fillLocal(chunk, chunkX, chunkZ, 9, 3, 1, 9, 5, 3, BlockType.AIR);
            }
            if (crossingUpperRight) {
                fillLocal(chunk, chunkX, chunkZ, 9, 5, 7, 9, 7, 9, BlockType.AIR);
            }

            fillLocal(chunk, chunkX, chunkZ, 5, 1, 10, 7, 3, 10, BlockType.AIR);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 1, 2, 1, 8, 2, 6);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 4, 1, 5, 4, 4, 9);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 8, 1, 5, 8, 4, 9);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 1, 4, 7, 3, 4, 9);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 1, 3, 5, 3, 3, 6);
            fillLocal(chunk, chunkX, chunkZ, 1, 3, 4, 3, 3, 4, BlockType.STONE_SLAB);
            fillLocal(chunk, chunkX, chunkZ, 1, 4, 6, 3, 4, 6, BlockType.STONE_SLAB);
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 5, 1, 7, 7, 1, 8);
            fillLocal(chunk, chunkX, chunkZ, 5, 1, 9, 7, 1, 9, BlockType.STONE_SLAB);
            fillLocal(chunk, chunkX, chunkZ, 5, 2, 7, 7, 2, 7, BlockType.STONE_SLAB);
            fillLocal(chunk, chunkX, chunkZ, 4, 5, 7, 4, 5, 9, BlockType.STONE_SLAB);
            fillLocal(chunk, chunkX, chunkZ, 8, 5, 7, 8, 5, 9, BlockType.STONE_SLAB);
            fillLocal(chunk, chunkX, chunkZ, 5, 5, 7, 7, 5, 9, BlockType.DOUBLE_STONE_SLAB);
            setLocal(chunk, chunkX, chunkZ, 6, 5, 6, BlockType.TORCH);
        }

        private void buildSourceStairs(Chunk chunk, int chunkX, int chunkZ, Random random, boolean doorway) {
            fillStrongholdLocal(chunk, chunkX, chunkZ, random, 0, 0, 0, 4, 10, 4);
            if (doorway) {
                placeSourceDoorLocal(chunk, chunkX, chunkZ, doorType, 1, 7, 0);
            } else {
                placeOpeningLocal(chunk, chunkX, chunkZ, 1, 7, 0);
            }
            placeOpeningLocal(chunk, chunkX, chunkZ, 1, 1, 4);

            setLocal(chunk, chunkX, chunkZ, 2, 6, 1, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 1, 5, 1, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 1, 6, 1, BlockType.STONE_SLAB, 0);
            setLocal(chunk, chunkX, chunkZ, 1, 5, 2, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 1, 4, 3, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 1, 5, 3, BlockType.STONE_SLAB, 0);
            setLocal(chunk, chunkX, chunkZ, 2, 4, 3, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 3, 3, 3, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 3, 4, 3, BlockType.STONE_SLAB, 0);
            setLocal(chunk, chunkX, chunkZ, 3, 3, 2, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 3, 2, 1, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 3, 3, 1, BlockType.STONE_SLAB, 0);
            setLocal(chunk, chunkX, chunkZ, 2, 2, 1, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 1, 1, 1, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 1, 2, 1, BlockType.STONE_SLAB, 0);
            setLocal(chunk, chunkX, chunkZ, 1, 1, 2, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, 1, 1, 3, BlockType.STONE_SLAB, 0);
        }

        private void buildCorridor(Chunk chunk, int chunkX, int chunkZ, Random random) {
            boolean alongX = bounds().maxX() - bounds().minX() >= bounds().maxZ() - bounds().minZ();
            if (alongX) {
                for (int x = bounds().minX(); x <= bounds().maxX(); x++) {
                    for (int y = bounds().minY(); y <= bounds().minY() + 4; y++) {
                        for (int z = bounds().minZ(); z <= bounds().maxZ(); z++) {
                            boolean edge = y == bounds().minY() || y == bounds().minY() + 4
                                    || z == bounds().minZ() || z == bounds().maxZ();
                            if (edge) {
                                BlockState state = strongholdStone(random, false);
                                set(chunk, chunkX, chunkZ, x, y, z, state.type(), state.metadata());
                            } else {
                                set(chunk, chunkX, chunkZ, x, y, z, BlockType.AIR);
                            }
                        }
                    }
                }
            } else {
                for (int z = bounds().minZ(); z <= bounds().maxZ(); z++) {
                    for (int y = bounds().minY(); y <= bounds().minY() + 4; y++) {
                        for (int x = bounds().minX(); x <= bounds().maxX(); x++) {
                            boolean edge = y == bounds().minY() || y == bounds().minY() + 4
                                    || x == bounds().minX() || x == bounds().maxX();
                            if (edge) {
                                BlockState state = strongholdStone(random, false);
                                set(chunk, chunkX, chunkZ, x, y, z, state.type(), state.metadata());
                            } else {
                                set(chunk, chunkX, chunkZ, x, y, z, BlockType.AIR);
                            }
                        }
                    }
                }
            }
        }

        private void setFrameLocal(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z, int facing,
                Random random) {
            int metadata = facing | (random.nextFloat() <= 0.9F ? 0 : 4);
            setLocal(chunk, chunkX, chunkZ, x, y, z, BlockType.END_PORTAL_FRAME, metadata);
        }

        private void strongholdShell(Chunk chunk, int chunkX, int chunkZ, Random random) {
            boolean allowMonsterEggs = room != StrongholdRoom.CORRIDOR;
            for (int y = bounds().minY(); y <= bounds().maxY(); y++) {
                for (int x = bounds().minX(); x <= bounds().maxX(); x++) {
                    for (int z = bounds().minZ(); z <= bounds().maxZ(); z++) {
                        boolean edge = x == bounds().minX() || x == bounds().maxX()
                                || z == bounds().minZ() || z == bounds().maxZ()
                                || y == bounds().minY() || y == bounds().maxY();
                        if (edge) {
                            BlockState state = strongholdStone(random, allowMonsterEggs);
                            set(chunk, chunkX, chunkZ, x, y, z, state.type(), state.metadata());
                        } else {
                            set(chunk, chunkX, chunkZ, x, y, z, BlockType.AIR);
                        }
                    }
                }
            }
        }

        private void fillStrongholdLocal(Chunk chunk, int chunkX, int chunkZ, Random random, int minX, int minY,
                int minZ, int maxX, int maxY, int maxZ) {
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        boolean edge = x == minX || x == maxX || y == minY || y == maxY
                                || z == minZ || z == maxZ;
                        if (edge) {
                            BlockState state = strongholdStone(random, true);
                            setLocal(chunk, chunkX, chunkZ, x, y, z, state.type(), state.metadata());
                        } else {
                            setLocal(chunk, chunkX, chunkZ, x, y, z, BlockType.AIR);
                        }
                    }
                }
            }
        }

        private void fillLocal(Chunk chunk, int chunkX, int chunkZ, int minX, int minY, int minZ,
                int maxX, int maxY, int maxZ, BlockType type) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        setLocal(chunk, chunkX, chunkZ, x, y, z, type);
                    }
                }
            }
        }

        private void randomlyFillLocal(Chunk chunk, int chunkX, int chunkZ, Random random, float chance,
                int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockType type) {
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (random.nextFloat() <= chance) {
                            setLocal(chunk, chunkX, chunkZ, x, y, z, type);
                        }
                    }
                }
            }
        }

        private void randomlySetLocal(Chunk chunk, int chunkX, int chunkZ, Random random, float chance, int x,
                int y, int z, BlockType type) {
            if (random.nextFloat() <= chance) {
                setLocal(chunk, chunkX, chunkZ, x, y, z, type);
            }
        }

        private void placeOpeningLocal(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z) {
            fillLocal(chunk, chunkX, chunkZ, x, y, z, x + 2, y + 2, z, BlockType.AIR);
        }

        private void placeSourceDoorLocal(Chunk chunk, int chunkX, int chunkZ, StrongholdDoor door, int x, int y,
                int z) {
            switch (door) {
                case WOOD_DOOR -> placeFramedDoorLocal(chunk, chunkX, chunkZ, x, y, z, BlockType.WOODEN_DOOR);
                case GRATES -> placeGrateDoorLocal(chunk, chunkX, chunkZ, x, y, z);
                case IRON_DOOR -> {
                    placeFramedDoorLocal(chunk, chunkX, chunkZ, x, y, z, BlockType.IRON_DOOR);
                    setLocal(chunk, chunkX, chunkZ, x + 2, y + 1, z + 1, BlockType.STONE_BUTTON,
                            strongholdButtonMetadata(4));
                    setLocal(chunk, chunkX, chunkZ, x + 2, y + 1, z - 1, BlockType.STONE_BUTTON,
                            strongholdButtonMetadata(3));
                }
                case OPENING -> placeOpeningLocal(chunk, chunkX, chunkZ, x, y, z);
            }
        }

        private void placeFramedDoorLocal(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z,
                BlockType door) {
            setLocal(chunk, chunkX, chunkZ, x, y, z, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, x, y + 1, z, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, x, y + 2, z, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, x + 1, y + 2, z, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, x + 2, y + 2, z, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, x + 2, y + 1, z, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, x + 2, y, z, BlockType.STONE_BRICK);
            setLocal(chunk, chunkX, chunkZ, x + 1, y, z, door, 0);
            setLocal(chunk, chunkX, chunkZ, x + 1, y + 1, z, door, 8);
        }

        private void placeGrateDoorLocal(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z) {
            setLocal(chunk, chunkX, chunkZ, x + 1, y, z, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, x + 1, y + 1, z, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, x, y, z, BlockType.IRON_BARS);
            setLocal(chunk, chunkX, chunkZ, x, y + 1, z, BlockType.IRON_BARS);
            setLocal(chunk, chunkX, chunkZ, x, y + 2, z, BlockType.IRON_BARS);
            setLocal(chunk, chunkX, chunkZ, x + 1, y + 2, z, BlockType.IRON_BARS);
            setLocal(chunk, chunkX, chunkZ, x + 2, y + 2, z, BlockType.IRON_BARS);
            setLocal(chunk, chunkX, chunkZ, x + 2, y + 1, z, BlockType.IRON_BARS);
            setLocal(chunk, chunkX, chunkZ, x + 2, y, z, BlockType.IRON_BARS);
        }

        private void setLocal(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z, BlockType type) {
            set(chunk, chunkX, chunkZ, localX(x, z), localY(y), localZ(x, z), type);
        }

        private void setLocal(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z, BlockType type,
                int metadata) {
            set(chunk, chunkX, chunkZ, localX(x, z), localY(y), localZ(x, z), type, metadata);
        }

        protected int localX(int x, int z) {
            return switch (coordBaseMode) {
                case 0, 2 -> bounds().minX() + x;
                case 1 -> bounds().maxX() - z;
                case 3 -> bounds().minX() + z;
                default -> bounds().minX() + x;
            };
        }

        private int localY(int y) {
            return bounds().minY() + y;
        }

        protected int localZ(int x, int z) {
            return switch (coordBaseMode) {
                case 2 -> bounds().maxZ() - z;
                case 0 -> bounds().minZ() + z;
                case 1, 3 -> bounds().minZ() + x;
                default -> bounds().minZ() + z;
            };
        }

        private int[] portalFrameMetadata() {
            return switch (coordBaseMode) {
                case 0 -> new int[] { 0, 2, 3, 1 };
                case 3 -> new int[] { 3, 1, 0, 2 };
                case 1 -> new int[] { 1, 3, 0, 2 };
                default -> new int[] { 2, 0, 3, 1 };
            };
        }

        private int strongholdStairMetadata(int metadata) {
            return switch (coordBaseMode) {
                case 0 -> metadata == 2 ? 3 : metadata == 3 ? 2 : metadata;
                case 1 -> switch (metadata) {
                    case 0 -> 2;
                    case 1 -> 3;
                    case 2 -> 0;
                    case 3 -> 1;
                    default -> metadata;
                };
                case 3 -> switch (metadata) {
                    case 0 -> 2;
                    case 1 -> 3;
                    case 2 -> 1;
                    case 3 -> 0;
                    default -> metadata;
                };
                default -> metadata;
            };
        }

        private int strongholdDoorMetadata(int metadata) {
            return switch (coordBaseMode) {
                case 0 -> metadata == 0 ? 2 : metadata == 2 ? 0 : metadata;
                case 1 -> metadata + 1 & 3;
                case 3 -> metadata + 3 & 3;
                default -> metadata;
            };
        }

        private int strongholdButtonMetadata(int metadata) {
            return switch (coordBaseMode) {
                case 0 -> metadata == 3 ? 4 : metadata == 4 ? 3 : metadata;
                case 1 -> switch (metadata) {
                    case 3 -> 1;
                    case 4 -> 2;
                    case 2 -> 3;
                    case 1 -> 4;
                    default -> metadata;
                };
                case 3 -> switch (metadata) {
                    case 3 -> 2;
                    case 4 -> 1;
                    case 2 -> 3;
                    case 1 -> 4;
                    default -> metadata;
                };
                default -> metadata;
            };
        }

        private int strongholdLadderMetadata(int metadata) {
            return switch (coordBaseMode) {
                case 0 -> metadata == 2 ? 3 : metadata == 3 ? 2 : metadata;
                case 1 -> switch (metadata) {
                    case 2 -> 4;
                    case 3 -> 5;
                    case 4 -> 2;
                    case 5 -> 3;
                    default -> metadata;
                };
                case 3 -> switch (metadata) {
                    case 2 -> 5;
                    case 3 -> 4;
                    case 4 -> 2;
                    case 5 -> 3;
                    default -> metadata;
                };
                default -> metadata;
            };
        }

        private static BlockState strongholdStone(Random random, boolean allowMonsterEggs) {
            float value = random.nextFloat();
            if (value < 0.20f) {
                return new BlockState(BlockType.STONE_BRICK, 2);
            }
            if (value < 0.50f) {
                return new BlockState(BlockType.STONE_BRICK, 1);
            }
            if (allowMonsterEggs && value < 0.55f) {
                return new BlockState(BlockType.INFESTED_STONE, 2);
            }
            return new BlockState(BlockType.STONE_BRICK, 0);
        }
    }

    private static class NetherFortressPiece extends BoxPiece {
        private final FortressRoom room;
        private final int coordBaseMode;
        private final int componentType;
        private final int fillSeed;

        NetherFortressPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, FortressRoom room) {
            this(minX, minY, minZ, maxX, maxY, maxZ, room, -1);
        }

        NetherFortressPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, FortressRoom room,
                int coordBaseMode) {
            this(minX, minY, minZ, maxX, maxY, maxZ, room, coordBaseMode, 0, 0);
        }

        NetherFortressPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, FortressRoom room,
                int coordBaseMode, int componentType) {
            this(minX, minY, minZ, maxX, maxY, maxZ, room, coordBaseMode, componentType, 0);
        }

        NetherFortressPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, FortressRoom room,
                int coordBaseMode, int componentType, int fillSeed) {
            super(minX, minY, minZ, maxX, maxY, maxZ);
            this.room = room;
            this.coordBaseMode = coordBaseMode;
            this.componentType = componentType;
            this.fillSeed = fillSeed;
        }

        NetherFortressPiece offsetY(int yOffset) {
            return new NetherFortressPiece(bounds().minX(), bounds().minY() + yOffset, bounds().minZ(),
                    bounds().maxX(), bounds().maxY() + yOffset, bounds().maxZ(), room, coordBaseMode,
                    componentType, fillSeed);
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            placeWithRandom(world, chunk, chunkX, chunkZ,
                    random(seed, bounds().minX(), bounds().minY(), bounds().minZ()));
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ, Random placementRandom) {
            Random random = placementRandom == null
                    ? random(seed, bounds().minX(), bounds().minY(), bounds().minZ())
                    : placementRandom;
            placeWithRandom(world, chunk, chunkX, chunkZ, random);
        }

        private void placeWithRandom(World world, Chunk chunk, int chunkX, int chunkZ, Random random) {
            if (room == FortressRoom.BRIDGE) {
                placeStraight(chunk, chunkX, chunkZ);
            } else if (room == FortressRoom.CROSSING) {
                placeCrossing3(chunk, chunkX, chunkZ);
            } else if (room == FortressRoom.SMALL_CROSSING) {
                placeSmallCrossing(chunk, chunkX, chunkZ);
            } else if (room == FortressRoom.STAIRS) {
                placeStairs(chunk, chunkX, chunkZ);
            } else if (room == FortressRoom.BLAZE_PLATFORM) {
                placeThrone(world, chunk, chunkX, chunkZ, random);
            } else if (room == FortressRoom.ENTRANCE) {
                placeEntrance(chunk, chunkX, chunkZ);
            } else if (room == FortressRoom.CORRIDOR_5) {
                placeCorridor5(chunk, chunkX, chunkZ);
            } else if (room == FortressRoom.CROSSING_2) {
                placeCrossing2(chunk, chunkX, chunkZ);
            } else if (room == FortressRoom.CORRIDOR_2) {
                placeCorridor2(chunk, chunkX, chunkZ);
            } else if (room == FortressRoom.CORRIDOR) {
                placeCorridor(chunk, chunkX, chunkZ);
            } else if (room == FortressRoom.CORRIDOR_3) {
                placeCorridor3(chunk, chunkX, chunkZ);
            } else if (room == FortressRoom.CORRIDOR_4) {
                placeCorridor4(chunk, chunkX, chunkZ);
            } else if (room == FortressRoom.WART_ROOM) {
                placeNetherStalkRoom(chunk, chunkX, chunkZ);
            } else if (room == FortressRoom.END) {
                placeEnd(chunk, chunkX, chunkZ);
            }
        }

        private void placeCrossing3(Chunk chunk, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 7, 3, 0, 11, 4, 18, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 3, 7, 18, 4, 11, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 8, 5, 0, 10, 7, 18, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 0, 5, 8, 18, 7, 10, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 7, 5, 0, 7, 5, 7, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 7, 5, 11, 7, 5, 18, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 11, 5, 0, 11, 5, 7, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 11, 5, 11, 11, 5, 18, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 5, 7, 7, 5, 7, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 11, 5, 7, 18, 5, 7, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 5, 11, 7, 5, 11, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 11, 5, 11, 18, 5, 11, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 7, 2, 0, 11, 2, 5, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 7, 2, 13, 11, 2, 18, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 7, 0, 0, 11, 1, 3, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 7, 0, 15, 11, 1, 18, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 7, 5, 2, 11, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 13, 2, 7, 18, 2, 11, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 7, 3, 1, 11, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 15, 0, 7, 18, 1, 11, BlockType.NETHER_BRICK);

            for (int x = 7; x <= 11; x++) {
                for (int z = 0; z <= 2; z++) {
                    fillDownLocal(chunk, chunkX, chunkZ, x, -1, z, BlockType.NETHER_BRICK);
                    fillDownLocal(chunk, chunkX, chunkZ, x, -1, 18 - z, BlockType.NETHER_BRICK);
                }
            }
            for (int x = 0; x <= 2; x++) {
                for (int z = 7; z <= 11; z++) {
                    fillDownLocal(chunk, chunkX, chunkZ, x, -1, z, BlockType.NETHER_BRICK);
                    fillDownLocal(chunk, chunkX, chunkZ, 18 - x, -1, z, BlockType.NETHER_BRICK);
                }
            }
        }

        private void placeStraight(Chunk chunk, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 0, 3, 0, 4, 4, 18, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 5, 0, 3, 7, 18, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 0, 5, 0, 0, 5, 18, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 4, 5, 0, 4, 5, 18, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 4, 2, 5, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 13, 4, 2, 18, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 4, 1, 3, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 15, 4, 1, 18, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 1, 1, 0, 4, 1, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 0, 3, 4, 0, 4, 4, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 0, 3, 14, 0, 4, 14, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 0, 1, 17, 0, 4, 17, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 4, 1, 1, 4, 4, 1, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 4, 3, 4, 4, 4, 4, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 4, 3, 14, 4, 4, 14, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 4, 1, 17, 4, 4, 17, BlockType.NETHER_BRICK_FENCE);

            for (int x = 0; x <= 4; x++) {
                for (int z = 0; z <= 2; z++) {
                    fillDownLocal(chunk, chunkX, chunkZ, x, -1, z, BlockType.NETHER_BRICK);
                    fillDownLocal(chunk, chunkX, chunkZ, x, -1, 18 - z, BlockType.NETHER_BRICK);
                }
            }
        }

        private void placeSmallCrossing(Chunk chunk, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 6, 1, 6, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 6, 7, 6, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 1, 6, 0, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 6, 1, 6, 6, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 5, 2, 0, 6, 6, 0, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 5, 2, 6, 6, 6, 6, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 0, 6, 1, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 5, 0, 6, 6, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 6, 2, 0, 6, 6, 1, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 6, 2, 5, 6, 6, 6, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 2, 6, 0, 4, 6, 0, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 2, 5, 0, 4, 5, 0, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 2, 6, 6, 4, 6, 6, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 2, 5, 6, 4, 5, 6, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 0, 6, 2, 0, 6, 4, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 5, 2, 0, 5, 4, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 6, 6, 2, 6, 6, 4, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 6, 5, 2, 6, 5, 4, BlockType.NETHER_BRICK_FENCE);
            fillDownArea(chunk, chunkX, chunkZ, 0, 6, 0, 6);
        }

        private void placeStairs(Chunk chunk, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 6, 1, 6, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 6, 10, 6, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 1, 8, 0, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 5, 2, 0, 6, 8, 0, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 1, 0, 8, 6, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 6, 2, 1, 6, 8, 6, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 2, 6, 5, 8, 6, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 3, 2, 0, 5, 4, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 6, 3, 2, 6, 5, 2, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 6, 3, 4, 6, 5, 4, BlockType.NETHER_BRICK_FENCE);
            setLocal(chunk, chunkX, chunkZ, 5, 2, 5, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 4, 2, 5, 4, 3, 5, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 3, 2, 5, 3, 4, 5, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 2, 2, 5, 2, 5, 5, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 2, 5, 1, 6, 5, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 7, 1, 5, 7, 4, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 6, 8, 2, 6, 8, 4, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 2, 6, 0, 4, 8, 0, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 2, 5, 0, 4, 5, 0, BlockType.NETHER_BRICK_FENCE);
            fillDownArea(chunk, chunkX, chunkZ, 0, 6, 0, 6);
        }

        private void placeThrone(World world, Chunk chunk, int chunkX, int chunkZ, Random random) {
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 6, 7, 7, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 1, 0, 0, 5, 1, 7, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 2, 1, 5, 2, 7, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 3, 2, 5, 3, 7, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 4, 3, 5, 4, 7, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 2, 0, 1, 4, 2, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 5, 2, 0, 5, 4, 2, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 5, 2, 1, 5, 3, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 5, 5, 2, 5, 5, 3, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 5, 3, 0, 5, 8, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 6, 5, 3, 6, 5, 8, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 5, 8, 5, 5, 8, BlockType.NETHER_BRICK);
            setLocal(chunk, chunkX, chunkZ, 1, 6, 3, BlockType.NETHER_BRICK_FENCE);
            setLocal(chunk, chunkX, chunkZ, 5, 6, 3, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 0, 6, 3, 0, 6, 8, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 6, 6, 3, 6, 6, 8, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 1, 6, 8, 5, 7, 8, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 2, 8, 8, 4, 8, 8, BlockType.NETHER_BRICK_FENCE);
            StructureGenerator.placeSpawner(world, chunk, chunkX, chunkZ,
                    localX(3, 5), localY(5), localZ(3, 5), MobDefinition.BLAZE, random);

            for (int x = 0; x <= 6; x++) {
                for (int z = 0; z <= 6; z++) {
                    fillDownLocal(chunk, chunkX, chunkZ, x, -1, z, BlockType.NETHER_BRICK);
                }
            }
        }

        private void placeEntrance(Chunk chunk, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 0, 3, 0, 12, 4, 12, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 5, 0, 12, 13, 12, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 0, 5, 0, 1, 12, 12, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 11, 5, 0, 12, 12, 12, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 2, 5, 11, 4, 12, 12, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 8, 5, 11, 10, 12, 12, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 5, 9, 11, 7, 12, 12, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 2, 5, 0, 4, 12, 1, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 8, 5, 0, 10, 12, 1, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 5, 9, 0, 7, 12, 1, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 2, 11, 2, 10, 12, 10, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 5, 8, 0, 7, 8, 0, BlockType.NETHER_BRICK_FENCE);
            placeRoofFences(chunk, chunkX, chunkZ, 12);
            fillLocal(chunk, chunkX, chunkZ, 4, 2, 0, 8, 2, 12, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 4, 12, 2, 8, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 4, 0, 0, 8, 1, 3, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 4, 0, 9, 8, 1, 12, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 4, 3, 1, 8, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 9, 0, 4, 12, 1, 8, BlockType.NETHER_BRICK);
            fillDownCrossSupports(chunk, chunkX, chunkZ, 12, 4, 8);
            fillLocal(chunk, chunkX, chunkZ, 5, 5, 5, 7, 5, 7, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 6, 1, 6, 6, 4, 6, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, 6, 0, 6, BlockType.NETHER_BRICK);
            placeEntranceLavaWell(chunk, chunkX, chunkZ);
        }

        private void placeEntranceLavaWell(Chunk chunk, int chunkX, int chunkZ) {
            setLocal(chunk, chunkX, chunkZ, 6, 5, 6, BlockType.FLOWING_LAVA, 0);
            for (int y = 4; y >= 1; y--) {
                setLocal(chunk, chunkX, chunkZ, 6, y, 6, BlockType.FLOWING_LAVA, 8);
            }
        }

        private void placeCorridor5(Chunk chunk, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 4, 1, 4, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 4, 5, 4, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 0, 5, 4, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 4, 2, 0, 4, 5, 4, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 3, 1, 0, 4, 1, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 0, 3, 3, 0, 4, 3, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 4, 3, 1, 4, 4, 1, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 4, 3, 3, 4, 4, 3, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 0, 6, 0, 4, 6, 4, BlockType.NETHER_BRICK);
            fillDownArea(chunk, chunkX, chunkZ, 0, 4, 0, 4);
        }

        private void placeCrossing2(Chunk chunk, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 4, 1, 4, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 4, 5, 4, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 0, 5, 0, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 4, 2, 0, 4, 5, 0, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 4, 0, 5, 4, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 4, 2, 4, 4, 5, 4, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 6, 0, 4, 6, 4, BlockType.NETHER_BRICK);
            fillDownArea(chunk, chunkX, chunkZ, 0, 4, 0, 4);
        }

        private void placeCorridor2(Chunk chunk, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 4, 1, 4, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 4, 5, 4, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 0, 5, 4, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 3, 1, 0, 4, 1, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 0, 3, 3, 0, 4, 3, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 4, 2, 0, 4, 5, 0, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 2, 4, 4, 5, 4, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 3, 4, 1, 4, 4, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 3, 3, 4, 3, 4, 4, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 0, 6, 0, 4, 6, 4, BlockType.NETHER_BRICK);
            fillDownArea(chunk, chunkX, chunkZ, 0, 4, 0, 4);
        }

        private void placeCorridor(Chunk chunk, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 4, 1, 4, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 4, 5, 4, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 4, 2, 0, 4, 5, 4, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 4, 3, 1, 4, 4, 1, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 4, 3, 3, 4, 4, 3, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 0, 5, 0, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 4, 3, 5, 4, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 3, 4, 1, 4, 4, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 3, 3, 4, 3, 4, 4, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 0, 6, 0, 4, 6, 4, BlockType.NETHER_BRICK);
            fillDownArea(chunk, chunkX, chunkZ, 0, 4, 0, 4);
        }

        private void placeCorridor3(Chunk chunk, int chunkX, int chunkZ) {
            int stairMetadata = 2;
            for (int z = 0; z <= 9; z++) {
                int floorY = Math.max(1, 7 - z);
                int roofY = Math.min(Math.max(floorY + 5, 14 - z), 13);
                fillLocal(chunk, chunkX, chunkZ, 0, 0, z, 4, floorY, z, BlockType.NETHER_BRICK);
                fillLocal(chunk, chunkX, chunkZ, 1, floorY + 1, z, 3, roofY - 1, z, BlockType.AIR);
                if (z <= 6) {
                    setLocal(chunk, chunkX, chunkZ, 1, floorY + 1, z, BlockType.NETHER_BRICK_STAIRS, stairMetadata);
                    setLocal(chunk, chunkX, chunkZ, 2, floorY + 1, z, BlockType.NETHER_BRICK_STAIRS, stairMetadata);
                    setLocal(chunk, chunkX, chunkZ, 3, floorY + 1, z, BlockType.NETHER_BRICK_STAIRS, stairMetadata);
                }
                fillLocal(chunk, chunkX, chunkZ, 0, roofY, z, 4, roofY, z, BlockType.NETHER_BRICK);
                fillLocal(chunk, chunkX, chunkZ, 0, floorY + 1, z, 0, roofY - 1, z, BlockType.NETHER_BRICK);
                fillLocal(chunk, chunkX, chunkZ, 4, floorY + 1, z, 4, roofY - 1, z, BlockType.NETHER_BRICK);
                if ((z & 1) == 0) {
                    fillLocal(chunk, chunkX, chunkZ, 0, floorY + 2, z, 0, floorY + 3, z,
                            BlockType.NETHER_BRICK_FENCE);
                    fillLocal(chunk, chunkX, chunkZ, 4, floorY + 2, z, 4, floorY + 3, z,
                            BlockType.NETHER_BRICK_FENCE);
                }
                for (int x = 0; x <= 4; x++) {
                    fillDownLocal(chunk, chunkX, chunkZ, x, -1, z, BlockType.NETHER_BRICK);
                }
            }
        }

        private void placeCorridor4(Chunk chunk, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 8, 1, 8, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 8, 5, 8, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 0, 6, 0, 8, 6, 5, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 2, 5, 0, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 6, 2, 0, 8, 5, 0, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 3, 0, 1, 4, 0, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 7, 3, 0, 7, 4, 0, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 4, 8, 2, 8, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 4, 2, 2, 4, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 6, 1, 4, 7, 2, 4, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 0, 3, 8, 8, 3, 8, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 0, 3, 6, 0, 3, 7, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 8, 3, 6, 8, 3, 7, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 0, 3, 4, 0, 5, 5, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 8, 3, 4, 8, 5, 5, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 3, 5, 2, 5, 5, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 6, 3, 5, 7, 5, 5, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 1, 4, 5, 1, 5, 5, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 7, 4, 5, 7, 5, 5, BlockType.NETHER_BRICK_FENCE);
            fillDownArea(chunk, chunkX, chunkZ, 0, 8, 0, 5);
        }

        private void placeNetherStalkRoom(Chunk chunk, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 0, 3, 0, 12, 4, 12, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 5, 0, 12, 13, 12, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 0, 5, 0, 1, 12, 12, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 11, 5, 0, 12, 12, 12, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 2, 5, 11, 4, 12, 12, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 8, 5, 11, 10, 12, 12, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 5, 9, 11, 7, 12, 12, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 2, 5, 0, 4, 12, 1, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 8, 5, 0, 10, 12, 1, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 5, 9, 0, 7, 12, 1, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 2, 11, 2, 10, 12, 10, BlockType.NETHER_BRICK);

            for (int i = 1; i <= 11; i += 2) {
                fillLocal(chunk, chunkX, chunkZ, i, 10, 0, i, 11, 0, BlockType.NETHER_BRICK_FENCE);
                fillLocal(chunk, chunkX, chunkZ, i, 10, 12, i, 11, 12, BlockType.NETHER_BRICK_FENCE);
                fillLocal(chunk, chunkX, chunkZ, 0, 10, i, 0, 11, i, BlockType.NETHER_BRICK_FENCE);
                fillLocal(chunk, chunkX, chunkZ, 12, 10, i, 12, 11, i, BlockType.NETHER_BRICK_FENCE);
                setLocal(chunk, chunkX, chunkZ, i, 13, 0, BlockType.NETHER_BRICK);
                setLocal(chunk, chunkX, chunkZ, i, 13, 12, BlockType.NETHER_BRICK);
                setLocal(chunk, chunkX, chunkZ, 0, 13, i, BlockType.NETHER_BRICK);
                setLocal(chunk, chunkX, chunkZ, 12, 13, i, BlockType.NETHER_BRICK);
                setLocal(chunk, chunkX, chunkZ, i + 1, 13, 0, BlockType.NETHER_BRICK_FENCE);
                setLocal(chunk, chunkX, chunkZ, i + 1, 13, 12, BlockType.NETHER_BRICK_FENCE);
                setLocal(chunk, chunkX, chunkZ, 0, 13, i + 1, BlockType.NETHER_BRICK_FENCE);
                setLocal(chunk, chunkX, chunkZ, 12, 13, i + 1, BlockType.NETHER_BRICK_FENCE);
            }

            setLocal(chunk, chunkX, chunkZ, 0, 13, 0, BlockType.NETHER_BRICK_FENCE);
            setLocal(chunk, chunkX, chunkZ, 0, 13, 12, BlockType.NETHER_BRICK_FENCE);
            setLocal(chunk, chunkX, chunkZ, 0, 13, 0, BlockType.NETHER_BRICK_FENCE);
            setLocal(chunk, chunkX, chunkZ, 12, 13, 0, BlockType.NETHER_BRICK_FENCE);

            for (int j = 3; j <= 9; j += 2) {
                fillLocal(chunk, chunkX, chunkZ, 1, 7, j, 1, 8, j, BlockType.NETHER_BRICK_FENCE);
                fillLocal(chunk, chunkX, chunkZ, 11, 7, j, 11, 8, j, BlockType.NETHER_BRICK_FENCE);
            }

            for (int l = 0; l <= 6; l++) {
                int z = l + 4;
                for (int x = 5; x <= 7; x++) {
                    setLocal(chunk, chunkX, chunkZ, x, 5 + l, z, BlockType.NETHER_BRICK_STAIRS, 3);
                }

                if (z >= 5 && z <= 8) {
                    fillLocal(chunk, chunkX, chunkZ, 5, 5, z, 7, l + 4, z, BlockType.NETHER_BRICK);
                } else if (z >= 9 && z <= 10) {
                    fillLocal(chunk, chunkX, chunkZ, 5, 8, z, 7, l + 4, z, BlockType.NETHER_BRICK);
                }

                if (l >= 1) {
                    fillLocal(chunk, chunkX, chunkZ, 5, 6 + l, z, 7, 9 + l, z, BlockType.AIR);
                }
            }

            for (int x = 5; x <= 7; x++) {
                setLocal(chunk, chunkX, chunkZ, x, 12, 11, BlockType.NETHER_BRICK_STAIRS, 3);
            }

            fillLocal(chunk, chunkX, chunkZ, 5, 6, 7, 5, 7, 7, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 7, 6, 7, 7, 7, 7, BlockType.NETHER_BRICK_FENCE);
            fillLocal(chunk, chunkX, chunkZ, 5, 13, 12, 7, 13, 12, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 2, 5, 2, 3, 5, 3, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 2, 5, 9, 3, 5, 10, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 2, 5, 4, 2, 5, 8, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 9, 5, 2, 10, 5, 3, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 9, 5, 9, 10, 5, 10, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 10, 5, 4, 10, 5, 8, BlockType.NETHER_BRICK);
            setLocal(chunk, chunkX, chunkZ, 4, 5, 2, BlockType.NETHER_BRICK_STAIRS, 1);
            setLocal(chunk, chunkX, chunkZ, 4, 5, 3, BlockType.NETHER_BRICK_STAIRS, 1);
            setLocal(chunk, chunkX, chunkZ, 4, 5, 9, BlockType.NETHER_BRICK_STAIRS, 1);
            setLocal(chunk, chunkX, chunkZ, 4, 5, 10, BlockType.NETHER_BRICK_STAIRS, 1);
            setLocal(chunk, chunkX, chunkZ, 8, 5, 2, BlockType.NETHER_BRICK_STAIRS, 0);
            setLocal(chunk, chunkX, chunkZ, 8, 5, 3, BlockType.NETHER_BRICK_STAIRS, 0);
            setLocal(chunk, chunkX, chunkZ, 8, 5, 9, BlockType.NETHER_BRICK_STAIRS, 0);
            setLocal(chunk, chunkX, chunkZ, 8, 5, 10, BlockType.NETHER_BRICK_STAIRS, 0);
            fillLocal(chunk, chunkX, chunkZ, 3, 4, 4, 4, 4, 8, BlockType.SOUL_SAND);
            fillLocal(chunk, chunkX, chunkZ, 8, 4, 4, 9, 4, 8, BlockType.SOUL_SAND);
            fillLocal(chunk, chunkX, chunkZ, 3, 5, 4, 4, 5, 8, BlockType.NETHER_WART);
            fillLocal(chunk, chunkX, chunkZ, 8, 5, 4, 9, 5, 8, BlockType.NETHER_WART);
            fillLocal(chunk, chunkX, chunkZ, 4, 2, 0, 8, 2, 12, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 4, 12, 2, 8, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 4, 0, 0, 8, 1, 3, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 4, 0, 9, 8, 1, 12, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 4, 3, 1, 8, BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 9, 0, 4, 12, 1, 8, BlockType.NETHER_BRICK);

            for (int x = 4; x <= 8; x++) {
                for (int z = 0; z <= 2; z++) {
                    fillDownLocal(chunk, chunkX, chunkZ, x, -1, z, BlockType.NETHER_BRICK);
                    fillDownLocal(chunk, chunkX, chunkZ, x, -1, 12 - z, BlockType.NETHER_BRICK);
                }
            }
            for (int x = 0; x <= 2; x++) {
                for (int z = 4; z <= 8; z++) {
                    fillDownLocal(chunk, chunkX, chunkZ, x, -1, z, BlockType.NETHER_BRICK);
                    fillDownLocal(chunk, chunkX, chunkZ, 12 - x, -1, z, BlockType.NETHER_BRICK);
                }
            }
        }

        private void placeEnd(Chunk chunk, int chunkX, int chunkZ) {
            Random random = new Random(fillSeed);
            for (int x = 0; x <= 4; x++) {
                for (int y = 3; y <= 4; y++) {
                    fillLocal(chunk, chunkX, chunkZ, x, y, 0, x, y, random.nextInt(8), BlockType.NETHER_BRICK);
                }
            }
            fillLocal(chunk, chunkX, chunkZ, 0, 5, 0, 0, 5, random.nextInt(8), BlockType.NETHER_BRICK);
            fillLocal(chunk, chunkX, chunkZ, 4, 5, 0, 4, 5, random.nextInt(8), BlockType.NETHER_BRICK);
            for (int x = 0; x <= 4; x++) {
                fillLocal(chunk, chunkX, chunkZ, x, 2, 0, x, 2, random.nextInt(5), BlockType.NETHER_BRICK);
            }
            for (int x = 0; x <= 4; x++) {
                for (int y = 0; y <= 1; y++) {
                    fillLocal(chunk, chunkX, chunkZ, x, y, 0, x, y, random.nextInt(3), BlockType.NETHER_BRICK);
                }
            }
        }

        private void placeRoofFences(Chunk chunk, int chunkX, int chunkZ, int max) {
            for (int i = 1; i <= max - 1; i += 2) {
                fillLocal(chunk, chunkX, chunkZ, i, 10, 0, i, 11, 0, BlockType.NETHER_BRICK_FENCE);
                fillLocal(chunk, chunkX, chunkZ, i, 10, max, i, 11, max, BlockType.NETHER_BRICK_FENCE);
                fillLocal(chunk, chunkX, chunkZ, 0, 10, i, 0, 11, i, BlockType.NETHER_BRICK_FENCE);
                fillLocal(chunk, chunkX, chunkZ, max, 10, i, max, 11, i, BlockType.NETHER_BRICK_FENCE);
                setLocal(chunk, chunkX, chunkZ, i, 13, 0, BlockType.NETHER_BRICK);
                setLocal(chunk, chunkX, chunkZ, i, 13, max, BlockType.NETHER_BRICK);
                setLocal(chunk, chunkX, chunkZ, 0, 13, i, BlockType.NETHER_BRICK);
                setLocal(chunk, chunkX, chunkZ, max, 13, i, BlockType.NETHER_BRICK);
                setLocal(chunk, chunkX, chunkZ, i + 1, 13, 0, BlockType.NETHER_BRICK_FENCE);
                setLocal(chunk, chunkX, chunkZ, i + 1, 13, max, BlockType.NETHER_BRICK_FENCE);
                setLocal(chunk, chunkX, chunkZ, 0, 13, i + 1, BlockType.NETHER_BRICK_FENCE);
                setLocal(chunk, chunkX, chunkZ, max, 13, i + 1, BlockType.NETHER_BRICK_FENCE);
            }
            setLocal(chunk, chunkX, chunkZ, 0, 13, 0, BlockType.NETHER_BRICK_FENCE);
            setLocal(chunk, chunkX, chunkZ, 0, 13, max, BlockType.NETHER_BRICK_FENCE);
            setLocal(chunk, chunkX, chunkZ, max, 13, 0, BlockType.NETHER_BRICK_FENCE);
        }

        private void fillDownCrossSupports(Chunk chunk, int chunkX, int chunkZ, int max, int min, int maxSupport) {
            for (int x = min; x <= maxSupport; x++) {
                for (int z = 0; z <= 2; z++) {
                    fillDownLocal(chunk, chunkX, chunkZ, x, -1, z, BlockType.NETHER_BRICK);
                    fillDownLocal(chunk, chunkX, chunkZ, x, -1, max - z, BlockType.NETHER_BRICK);
                }
            }
            for (int x = 0; x <= 2; x++) {
                for (int z = min; z <= maxSupport; z++) {
                    fillDownLocal(chunk, chunkX, chunkZ, x, -1, z, BlockType.NETHER_BRICK);
                    fillDownLocal(chunk, chunkX, chunkZ, max - x, -1, z, BlockType.NETHER_BRICK);
                }
            }
        }

        private void fillDownArea(Chunk chunk, int chunkX, int chunkZ, int minX, int maxX, int minZ, int maxZ) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    fillDownLocal(chunk, chunkX, chunkZ, x, -1, z, BlockType.NETHER_BRICK);
                }
            }
        }

        private void setLocal(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z, BlockType type) {
            set(chunk, chunkX, chunkZ, localX(x, z), localY(y), localZ(x, z), type);
        }

        private void setLocal(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z, BlockType type,
                int metadata) {
            set(chunk, chunkX, chunkZ, localX(x, z), localY(y), localZ(x, z), type,
                    metadataWithOffset(type, metadata));
        }

        private void fillLocal(Chunk chunk, int chunkX, int chunkZ, int minX, int minY, int minZ,
                int maxX, int maxY, int maxZ, BlockType type) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        setLocal(chunk, chunkX, chunkZ, x, y, z, type);
                    }
                }
            }
        }

        private void fillDownLocal(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z, BlockType type) {
            fillDownWorld(chunk, chunkX, chunkZ, localX(x, z), localY(y), localZ(x, z), type);
        }

        private void fillDownWorld(Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ,
                BlockType type) {
            if (Math.floorDiv(worldX, Chunk.WIDTH) != chunkX || Math.floorDiv(worldZ, Chunk.DEPTH) != chunkZ) {
                return;
            }
            int localX = Math.floorMod(worldX, Chunk.WIDTH);
            int localZ = Math.floorMod(worldZ, Chunk.DEPTH);
            for (int currentY = y; currentY > 1; currentY--) {
                BlockType current = chunk.getBlock(localX, currentY, localZ);
                if (!isAirOrLiquid(current)) {
                    return;
                }
                set(chunk, chunkX, chunkZ, worldX, currentY, worldZ, type);
            }
        }

        private static boolean isAirOrLiquid(BlockType type) {
            return type == BlockType.AIR || type == BlockType.WATER || type == BlockType.FLOWING_WATER
                    || type == BlockType.LAVA || type == BlockType.FLOWING_LAVA;
        }

        private int localX(int x, int z) {
            return switch (coordBaseMode) {
                case 0, 2 -> bounds().minX() + x;
                case 1 -> bounds().maxX() - z;
                case 3 -> bounds().minX() + z;
                default -> bounds().minX() + x;
            };
        }

        private int localY(int y) {
            return bounds().minY() + y;
        }

        private int localZ(int x, int z) {
            return switch (coordBaseMode) {
                case 2 -> bounds().maxZ() - z;
                case 0 -> bounds().minZ() + z;
                case 1, 3 -> bounds().minZ() + x;
                default -> bounds().minZ() + z;
            };
        }

        private int metadataWithOffset(BlockType type, int metadata) {
            if (!type.isStairs()) {
                return metadata;
            }
            return switch (coordBaseMode) {
                case 0 -> metadata == 2 ? 3 : metadata == 3 ? 2 : metadata;
                case 1 -> switch (metadata) {
                    case 0 -> 2;
                    case 1 -> 3;
                    case 2 -> 0;
                    case 3 -> 1;
                    default -> metadata;
                };
                case 3 -> switch (metadata) {
                    case 0 -> 2;
                    case 1 -> 3;
                    case 2 -> 1;
                    case 3 -> 0;
                    default -> metadata;
                };
                default -> metadata;
            };
        }
    }

    private static class MineshaftRoomPiece extends BoxPiece {
        private final List<StructureBoundingBox> childOpenings;

        MineshaftRoomPiece(StructureBoundingBox bounds, List<StructureBoundingBox> childOpenings) {
            super(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
            this.childOpenings = List.copyOf(childOpenings);
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            if (hasLiquidInChunkEnvelope(chunk, chunkX, chunkZ)) {
                return;
            }
            fillWorldNonAir(chunk, chunkX, chunkZ, bounds().minX(), bounds().minY(), bounds().minZ(),
                    bounds().maxX(), bounds().minY(), bounds().maxZ(), BlockType.DIRT);
            fillWorld(chunk, chunkX, chunkZ, bounds().minX(), bounds().minY() + 1, bounds().minZ(),
                    bounds().maxX(), Math.min(bounds().minY() + 3, bounds().maxY()), bounds().maxZ(),
                    BlockType.AIR);

            for (StructureBoundingBox opening : childOpenings) {
                fillWorld(chunk, chunkX, chunkZ, opening.minX(), opening.maxY() - 2, opening.minZ(),
                        opening.maxX(), opening.maxY(), opening.maxZ(), BlockType.AIR);
            }
            clearRareUpperPocket(chunk, chunkX, chunkZ);
        }

        private void clearRareUpperPocket(Chunk chunk, int chunkX, int chunkZ) {
            int minY = bounds().minY() + 4;
            if (minY > bounds().maxY()) {
                return;
            }
            float width = bounds().maxX() - bounds().minX() + 1;
            float height = bounds().maxY() - minY + 1;
            float depth = bounds().maxZ() - bounds().minZ() + 1;
            float centerX = bounds().minX() + width / 2.0f;
            float centerZ = bounds().minZ() + depth / 2.0f;
            for (int y = minY; y <= bounds().maxY(); y++) {
                float dy = (float) (y - minY) / height;
                for (int x = bounds().minX(); x <= bounds().maxX(); x++) {
                    float dx = (x - centerX) / (width * 0.5f);
                    for (int z = bounds().minZ(); z <= bounds().maxZ(); z++) {
                        float dz = (z - centerZ) / (depth * 0.5f);
                        if (dx * dx + dy * dy + dz * dz <= 1.05f) {
                            set(chunk, chunkX, chunkZ, x, y, z, BlockType.AIR);
                        }
                    }
                }
            }
        }

        private void fillWorld(Chunk chunk, int chunkX, int chunkZ, int minX, int minY, int minZ,
                int maxX, int maxY, int maxZ, BlockType type) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        set(chunk, chunkX, chunkZ, x, y, z, type);
                    }
                }
            }
        }

        private void fillWorldNonAir(Chunk chunk, int chunkX, int chunkZ, int minX, int minY, int minZ,
                int maxX, int maxY, int maxZ, BlockType type) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        if (blockWorld(chunk, chunkX, chunkZ, x, y, z) != BlockType.AIR) {
                            set(chunk, chunkX, chunkZ, x, y, z, type);
                        }
                    }
                }
            }
        }
    }

    private static class MineshaftPiece extends BoxPiece {
        private final boolean eastWest;
        private final int mode;
        private final int sectionCount;
        private final boolean hasRails;
        private final boolean hasSpiders;

        MineshaftPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean eastWest) {
            this(minX, minY, minZ, maxX, maxY, maxZ, eastWest,
                    ((eastWest ? maxX - minX : maxZ - minZ) + 2) / 5, true, false);
        }

        MineshaftPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean eastWest,
                int sectionCount, boolean hasRails, boolean hasSpiders) {
            this(minX, minY, minZ, maxX, maxY, maxZ, eastWest ? 3 : 0, sectionCount, hasRails, hasSpiders);
        }

        MineshaftPiece(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int mode,
                int sectionCount, boolean hasRails, boolean hasSpiders) {
            super(minX, minY, minZ, maxX, maxY, maxZ);
            this.mode = mode;
            this.eastWest = mode == 1 || mode == 3;
            this.sectionCount = sectionCount;
            this.hasRails = hasRails;
            this.hasSpiders = hasSpiders;
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            placeWithRandom(world, chunk, chunkX, chunkZ,
                    random(seed, bounds().minX(), bounds().minY(), bounds().minZ()));
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ, Random placementRandom) {
            Random random = placementRandom == null
                    ? random(seed, bounds().minX(), bounds().minY(), bounds().minZ())
                    : placementRandom;
            placeWithRandom(world, chunk, chunkX, chunkZ, random);
        }

        private void placeWithRandom(World world, Chunk chunk, int chunkX, int chunkZ, Random random) {
            if (hasLiquidInChunkEnvelope(chunk, chunkX, chunkZ)) {
                return;
            }
            int length = sectionCount * 5 - 1;
            for (int axis = 0; axis <= length; axis++) {
                for (int cross = 0; cross <= 2; cross++) {
                    setLocal(chunk, chunkX, chunkZ, cross, 0, axis, BlockType.AIR);
                    setLocal(chunk, chunkX, chunkZ, cross, 1, axis, BlockType.AIR);
                }
            }
            for (int cross = 0; cross <= 2; cross++) {
                for (int axis = 0; axis <= length; axis++) {
                    if (random.nextFloat() < 0.8f) {
                        setLocal(chunk, chunkX, chunkZ, cross, 2, axis, BlockType.AIR);
                    }
                }
            }
            if (hasSpiders) {
                for (int y = 0; y <= 1; y++) {
                    for (int cross = 0; cross <= 2; cross++) {
                        for (int axis = 0; axis <= length; axis++) {
                            randomlyPlaceLocal(chunk, chunkX, chunkZ, random, 0.6f,
                                    cross, y, axis, BlockType.COBWEB);
                        }
                    }
                }
            }

            boolean spawnerPlaced = false;
            for (int section = 0; section < sectionCount; section++) {
                int axis = 2 + section * 5;
                setLocal(chunk, chunkX, chunkZ, 0, 0, axis, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, 2, 0, axis, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, 0, 1, axis, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, 2, 1, axis, BlockType.FENCE);

                if (random.nextInt(4) != 0) {
                    fillLocal(chunk, chunkX, chunkZ, 0, 2, axis, 2, 2, axis, BlockType.OAK_PLANKS);
                } else {
                    setLocal(chunk, chunkX, chunkZ, 0, 2, axis, BlockType.OAK_PLANKS);
                    setLocal(chunk, chunkX, chunkZ, 2, 2, axis, BlockType.OAK_PLANKS);
                }

                randomlyPlaceLocal(chunk, chunkX, chunkZ, random, 0.1f, 0, 2, axis - 1, BlockType.COBWEB);
                randomlyPlaceLocal(chunk, chunkX, chunkZ, random, 0.1f, 2, 2, axis - 1, BlockType.COBWEB);
                randomlyPlaceLocal(chunk, chunkX, chunkZ, random, 0.1f, 0, 2, axis + 1, BlockType.COBWEB);
                randomlyPlaceLocal(chunk, chunkX, chunkZ, random, 0.1f, 2, 2, axis + 1, BlockType.COBWEB);
                randomlyPlaceLocal(chunk, chunkX, chunkZ, random, 0.05f, 0, 2, axis - 2, BlockType.COBWEB);
                randomlyPlaceLocal(chunk, chunkX, chunkZ, random, 0.05f, 2, 2, axis - 2, BlockType.COBWEB);
                randomlyPlaceLocal(chunk, chunkX, chunkZ, random, 0.05f, 0, 2, axis + 2, BlockType.COBWEB);
                randomlyPlaceLocal(chunk, chunkX, chunkZ, random, 0.05f, 2, 2, axis + 2, BlockType.COBWEB);
                randomlyPlaceLocal(chunk, chunkX, chunkZ, random, 0.05f, 1, 2, axis - 1, BlockType.TORCH);
                randomlyPlaceLocal(chunk, chunkX, chunkZ, random, 0.05f, 1, 2, axis + 1, BlockType.TORCH);

                if (random.nextInt(100) == 0) {
                    placeMineshaftChest(world, chunk, chunkX, chunkZ, random, 2, 0, axis - 1);
                }
                if (random.nextInt(100) == 0) {
                    placeMineshaftChest(world, chunk, chunkX, chunkZ, random, 0, 0, axis + 1);
                }

                if (hasSpiders && !spawnerPlaced) {
                    int spawnerAxis = (axis - 1) + random.nextInt(3);
                    int spawnerX = localX(1, spawnerAxis);
                    int spawnerY = localY(0);
                    int spawnerZ = localZ(1, spawnerAxis);
                    if (!isInsideChunk(spawnerX, spawnerY, spawnerZ, chunkX, chunkZ)) {
                        continue;
                    }
                    StructureGenerator.placeSpawner(world, chunk, chunkX, chunkZ, spawnerX, spawnerY, spawnerZ,
                            MobDefinition.CAVE_SPIDER, random);
                    spawnerPlaced = true;
                }
            }

            if (hasRails) {
                int metadata = eastWest ? RailShapeResolver.EAST_WEST : RailShapeResolver.NORTH_SOUTH;
                for (int axis = 0; axis <= length; axis++) {
                    if (blockLocal(chunk, chunkX, chunkZ, 1, -1, axis).isSolid()
                            && random.nextFloat() < 0.7f) {
                        setLocal(chunk, chunkX, chunkZ, 1, 0, axis, BlockType.RAIL, metadata);
                    }
                }
            }
        }

        private void placeMineshaftChest(World world, Chunk chunk, int chunkX, int chunkZ, Random random,
                int cross, int y, int axis) {
            StructureGenerator.placeWeightedLootChest(world, chunk, chunkX, chunkZ,
                    localX(cross, axis), localY(y), localZ(cross, axis), random, 3 + random.nextInt(4),
                    MINESHAFT_CORRIDOR_LOOT);
        }

        private void fillLocal(Chunk chunk, int chunkX, int chunkZ, int minCross, int minY, int minAxis,
                int maxCross, int maxY, int maxAxis, BlockType type) {
            for (int axis = minAxis; axis <= maxAxis; axis++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int cross = minCross; cross <= maxCross; cross++) {
                        setLocal(chunk, chunkX, chunkZ, cross, y, axis, type);
                    }
                }
            }
        }

        private void randomlyPlaceLocal(Chunk chunk, int chunkX, int chunkZ, Random random, float chance,
                int cross, int y, int axis, BlockType type) {
            if (random.nextFloat() < chance) {
                setLocal(chunk, chunkX, chunkZ, cross, y, axis, type);
            }
        }

        private void setLocal(Chunk chunk, int chunkX, int chunkZ, int cross, int y, int axis, BlockType type) {
            setLocal(chunk, chunkX, chunkZ, cross, y, axis, type, 0);
        }

        private void setLocal(Chunk chunk, int chunkX, int chunkZ, int cross, int y, int axis, BlockType type,
                int metadata) {
            StructureGenerator.writeBlock(chunk, chunkX, chunkZ, localX(cross, axis), localY(y),
                    localZ(cross, axis), type, metadata);
        }

        private BlockType blockLocal(Chunk chunk, int chunkX, int chunkZ, int cross, int y, int axis) {
            int worldX = localX(cross, axis);
            int worldY = localY(y);
            int worldZ = localZ(cross, axis);
            if (Math.floorDiv(worldX, Chunk.WIDTH) != chunkX || Math.floorDiv(worldZ, Chunk.DEPTH) != chunkZ
                    || worldY < 0 || worldY >= Chunk.HEIGHT) {
                return BlockType.AIR;
            }
            return chunk.getBlock(Math.floorMod(worldX, Chunk.WIDTH), worldY, Math.floorMod(worldZ, Chunk.DEPTH));
        }

        private int localX(int cross, int axis) {
            return switch (mode) {
                case 1 -> bounds().maxX() - axis;
                case 3 -> bounds().minX() + axis;
                default -> bounds().minX() + cross;
            };
        }

        private int localY(int y) {
            return bounds().minY() + y;
        }

        private int localZ(int cross, int axis) {
            return switch (mode) {
                case 2 -> bounds().maxZ() - axis;
                case 0 -> bounds().minZ() + axis;
                default -> bounds().minZ() + cross;
            };
        }
    }

    private static class MineshaftCrossPiece extends BoxPiece {
        private final boolean multipleFloors;

        MineshaftCrossPiece(StructureBoundingBox bounds, boolean multipleFloors) {
            super(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
            this.multipleFloors = multipleFloors;
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            if (hasLiquidInChunkEnvelope(chunk, chunkX, chunkZ)) {
                return;
            }
            if (multipleFloors) {
                fillWorld(chunk, chunkX, chunkZ, bounds().minX() + 1, bounds().minY(), bounds().minZ(),
                        bounds().maxX() - 1, bounds().minY() + 2, bounds().maxZ(), BlockType.AIR);
                fillWorld(chunk, chunkX, chunkZ, bounds().minX(), bounds().minY(), bounds().minZ() + 1,
                        bounds().maxX(), bounds().minY() + 2, bounds().maxZ() - 1, BlockType.AIR);
                fillWorld(chunk, chunkX, chunkZ, bounds().minX() + 1, bounds().maxY() - 2, bounds().minZ(),
                        bounds().maxX() - 1, bounds().maxY(), bounds().maxZ(), BlockType.AIR);
                fillWorld(chunk, chunkX, chunkZ, bounds().minX(), bounds().maxY() - 2, bounds().minZ() + 1,
                        bounds().maxX(), bounds().maxY(), bounds().maxZ() - 1, BlockType.AIR);
                fillWorld(chunk, chunkX, chunkZ, bounds().minX() + 1, bounds().minY() + 3, bounds().minZ() + 1,
                        bounds().maxX() - 1, bounds().minY() + 3, bounds().maxZ() - 1, BlockType.AIR);
            } else {
                fillWorld(chunk, chunkX, chunkZ, bounds().minX() + 1, bounds().minY(), bounds().minZ(),
                        bounds().maxX() - 1, bounds().maxY(), bounds().maxZ(), BlockType.AIR);
                fillWorld(chunk, chunkX, chunkZ, bounds().minX(), bounds().minY(), bounds().minZ() + 1,
                        bounds().maxX(), bounds().maxY(), bounds().maxZ() - 1, BlockType.AIR);
            }

            fillWorld(chunk, chunkX, chunkZ, bounds().minX() + 1, bounds().minY(), bounds().minZ() + 1,
                    bounds().minX() + 1, bounds().maxY(), bounds().minZ() + 1, BlockType.OAK_PLANKS);
            fillWorld(chunk, chunkX, chunkZ, bounds().minX() + 1, bounds().minY(), bounds().maxZ() - 1,
                    bounds().minX() + 1, bounds().maxY(), bounds().maxZ() - 1, BlockType.OAK_PLANKS);
            fillWorld(chunk, chunkX, chunkZ, bounds().maxX() - 1, bounds().minY(), bounds().minZ() + 1,
                    bounds().maxX() - 1, bounds().maxY(), bounds().minZ() + 1, BlockType.OAK_PLANKS);
            fillWorld(chunk, chunkX, chunkZ, bounds().maxX() - 1, bounds().minY(), bounds().maxZ() - 1,
                    bounds().maxX() - 1, bounds().maxY(), bounds().maxZ() - 1, BlockType.OAK_PLANKS);
        }

        private void fillWorld(Chunk chunk, int chunkX, int chunkZ, int minX, int minY, int minZ,
                int maxX, int maxY, int maxZ, BlockType type) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        set(chunk, chunkX, chunkZ, x, y, z, type);
                    }
                }
            }
        }

    }

    private static class MineshaftStairsPiece extends BoxPiece {
        private final int mode;

        MineshaftStairsPiece(StructureBoundingBox bounds, int mode) {
            super(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
            this.mode = mode;
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            if (hasLiquidInChunkEnvelope(chunk, chunkX, chunkZ)) {
                return;
            }
            fillLocal(chunk, chunkX, chunkZ, 0, 5, 0, 2, 7, 1, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 7, 2, 2, 8, BlockType.AIR);
            for (int i = 0; i < 5; i++) {
                int minY = 5 - i - (i >= 4 ? 0 : 1);
                fillLocal(chunk, chunkX, chunkZ, 0, minY, 2 + i, 2, 7 - i, 2 + i, BlockType.AIR);
            }
        }

        private void fillLocal(Chunk chunk, int chunkX, int chunkZ, int minCross, int minY, int minAxis,
                int maxCross, int maxY, int maxAxis, BlockType type) {
            for (int axis = minAxis; axis <= maxAxis; axis++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int cross = minCross; cross <= maxCross; cross++) {
                        set(chunk, chunkX, chunkZ, localX(cross, axis), localY(y), localZ(cross, axis), type);
                    }
                }
            }
        }

        private int localX(int cross, int axis) {
            return switch (mode) {
                case 1 -> bounds().maxX() - axis;
                case 3 -> bounds().minX() + axis;
                default -> bounds().minX() + cross;
            };
        }

        private int localY(int y) {
            return bounds().minY() + y;
        }

        private int localZ(int cross, int axis) {
            return switch (mode) {
                case 2 -> bounds().maxZ() - axis;
                case 0 -> bounds().minZ() + axis;
                default -> bounds().minZ() + cross;
            };
        }
    }

    private static class VillageWellPiece extends BoxPiece {
        private final int placementMinY;

        VillageWellPiece(int minX, int sourceMinY, int minZ, int placementMinY) {
            super(minX, sourceMinY, minZ, minX + 5, sourceMinY + 14, minZ + 5);
            this.placementMinY = placementMinY;
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            for (int y = 0; y <= 12; y++) {
                for (int z = 1; z <= 4; z++) {
                    for (int x = 1; x <= 4; x++) {
                        boolean border = x == 1 || x == 4 || z == 1 || z == 4 || y == 0 || y == 12;
                        setLocal(chunk, chunkX, chunkZ, x, y, z,
                                border ? BlockType.COBBLESTONE : BlockType.FLOWING_WATER);
                    }
                }
            }
            setLocal(chunk, chunkX, chunkZ, 2, 12, 2, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, 3, 12, 2, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, 2, 12, 3, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, 3, 12, 3, BlockType.AIR);

            setLocal(chunk, chunkX, chunkZ, 1, 13, 1, BlockType.FENCE);
            setLocal(chunk, chunkX, chunkZ, 1, 14, 1, BlockType.FENCE);
            setLocal(chunk, chunkX, chunkZ, 4, 13, 1, BlockType.FENCE);
            setLocal(chunk, chunkX, chunkZ, 4, 14, 1, BlockType.FENCE);
            setLocal(chunk, chunkX, chunkZ, 1, 13, 4, BlockType.FENCE);
            setLocal(chunk, chunkX, chunkZ, 1, 14, 4, BlockType.FENCE);
            setLocal(chunk, chunkX, chunkZ, 4, 13, 4, BlockType.FENCE);
            setLocal(chunk, chunkX, chunkZ, 4, 14, 4, BlockType.FENCE);

            for (int z = 1; z <= 4; z++) {
                for (int x = 1; x <= 4; x++) {
                    setLocal(chunk, chunkX, chunkZ, x, 15, z, BlockType.COBBLESTONE);
                }
            }
            for (int z = 0; z <= 5; z++) {
                for (int x = 0; x <= 5; x++) {
                    if (x == 0 || x == 5 || z == 0 || z == 5) {
                        setLocal(chunk, chunkX, chunkZ, x, 11, z, BlockType.GRAVEL);
                        clearLocalUpward(chunk, chunkX, chunkZ, x, 12, z);
                    }
                }
            }
        }

        private void setLocal(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z, BlockType type) {
            set(chunk, chunkX, chunkZ, bounds().minX() + x, placementMinY + y, bounds().minZ() + z, type);
        }

        private void clearLocalUpward(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z) {
            for (int currentY = placementMinY + y; currentY < Chunk.HEIGHT; currentY++) {
                set(chunk, chunkX, chunkZ, bounds().minX() + x, currentY, bounds().minZ() + z, BlockType.AIR);
            }
        }

        private StructureBoundingBox placementBounds() {
            return new StructureBoundingBox(bounds().minX(), placementMinY, bounds().minZ(),
                    bounds().maxX(), placementMinY + 15, bounds().maxZ());
        }
    }

    private abstract static class VillageOrientedPiece extends BoxPiece {
        protected final int mode;
        protected final int placementMinY;
        private int villagersSpawned;

        VillageOrientedPiece(StructureBoundingBox box, int mode) {
            this(box, mode, box.minY());
        }

        VillageOrientedPiece(StructureBoundingBox box, int mode, int placementMinY) {
            super(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
            this.mode = mode;
            this.placementMinY = placementMinY;
        }

        protected void fillLocal(Chunk chunk, int chunkX, int chunkZ, int minX, int minY, int minZ,
                int maxX, int maxY, int maxZ, BlockType type) {
            fillLocal(chunk, chunkX, chunkZ, minX, minY, minZ, maxX, maxY, maxZ, type, 0);
        }

        protected void fillLocal(Chunk chunk, int chunkX, int chunkZ, int minX, int minY, int minZ,
                int maxX, int maxY, int maxZ, BlockType type, int metadata) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        setLocal(chunk, chunkX, chunkZ, x, y, z, type, metadata);
                    }
                }
            }
        }

        protected void setLocal(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z, BlockType type) {
            setLocal(chunk, chunkX, chunkZ, x, y, z, type, 0);
        }

        protected void setLocal(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z, BlockType type,
                int metadata) {
            set(chunk, chunkX, chunkZ, localX(x, z), placementY(y), localZ(x, z), type, metadata);
        }

        protected void setTorchAttachedLocal(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z,
                int supportX, int supportZ) {
            int worldX = localX(x, z);
            int worldZ = localZ(x, z);
            int supportWorldX = localX(supportX, supportZ);
            int supportWorldZ = localZ(supportX, supportZ);
            int metadata = torchMetadataFromSupportOffset(supportWorldX - worldX, supportWorldZ - worldZ);
            set(chunk, chunkX, chunkZ, worldX, placementY(y), worldZ, BlockType.TORCH, metadata);
        }

        protected BlockType getLocal(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z) {
            int worldX = localX(x, z);
            int worldY = placementY(y);
            int worldZ = localZ(x, z);
            if (worldY < 0 || worldY >= Chunk.HEIGHT
                    || Math.floorDiv(worldX, Chunk.WIDTH) != chunkX
                    || Math.floorDiv(worldZ, Chunk.DEPTH) != chunkZ) {
                return BlockType.AIR;
            }
            return chunk.getBlock(Math.floorMod(worldX, Chunk.WIDTH), worldY, Math.floorMod(worldZ, Chunk.DEPTH));
        }

        protected void clearLocalUpward(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z) {
            int currentY = y;
            while (placementY(currentY) < Chunk.HEIGHT
                    && getLocal(chunk, chunkX, chunkZ, x, currentY, z) != BlockType.AIR) {
                setLocal(chunk, chunkX, chunkZ, x, currentY, z, BlockType.AIR);
                currentY++;
            }
        }

        protected void fillLocalDownward(Chunk chunk, int chunkX, int chunkZ, BlockType type, int x, int y, int z) {
            int currentY = y;
            while (placementY(currentY) > 1) {
                BlockType current = getLocal(chunk, chunkX, chunkZ, x, currentY, z);
                if (current != BlockType.AIR && !current.isFluid()) {
                    return;
                }
                setLocal(chunk, chunkX, chunkZ, x, currentY, z, type);
                currentY--;
            }
        }

        protected void placeDoorLocal(Chunk chunk, int chunkX, int chunkZ, int x, int y, int z, int metadata) {
            int worldX = localX(x, z);
            int worldY = placementY(y);
            int worldZ = localZ(x, z);
            int doorData = sourceDoorMetadata(chunk, chunkX, chunkZ, worldX, worldY, worldZ,
                    doorMetadata(metadata));
            set(chunk, chunkX, chunkZ, worldX, worldY, worldZ, BlockType.WOODEN_DOOR, doorData);
            set(chunk, chunkX, chunkZ, worldX, worldY + 1, worldZ, BlockType.WOODEN_DOOR, doorData + 8);
        }

        protected void spawnVillagerLocal(World world, int chunkX, int chunkZ, int x, int y, int z,
                int profession) {
            spawnVillagersLocal(world, chunkX, chunkZ, x, y, z, 1, profession);
        }

        protected void spawnVillagersLocal(World world, int chunkX, int chunkZ, int x, int y, int z,
                int count, int... professions) {
            if (world == null || !world.shouldSpawnNpcs() || villagersSpawned >= count) {
                return;
            }
            while (villagersSpawned < count) {
                int spawnX = x + villagersSpawned;
                int worldX = localX(spawnX, z);
                int worldY = placementY(y);
                int worldZ = localZ(spawnX, z);
                if (!isInsideChunk(worldX, worldY, worldZ, chunkX, chunkZ)) {
                    return;
                }
                Mob mob = MobFactory.create(MobDefinition.VILLAGER);
                if (!(mob instanceof Villager villager)) {
                    return;
                }
                int profession = villagersSpawned < professions.length
                        ? professions[villagersSpawned]
                        : Villager.PROFESSION_FARMER;
                villager.setProfession(profession);
                villager.setPosition(worldX + 0.5f, worldY, worldZ + 0.5f);
                world.stageGeneratedEntity(villager);
                villagersSpawned++;
            }
        }

        protected int stairMetadata(int metadata) {
            return switch (mode) {
                case 0 -> metadata == 2 ? 3 : metadata == 3 ? 2 : metadata;
                case 1 -> switch (metadata) {
                    case 0 -> 2;
                    case 1 -> 3;
                    case 2 -> 0;
                    case 3 -> 1;
                    default -> metadata;
                };
                case 3 -> switch (metadata) {
                    case 0 -> 2;
                    case 1 -> 3;
                    case 2 -> 1;
                    case 3 -> 0;
                    default -> metadata;
                };
                default -> metadata;
            };
        }

        protected int ladderMetadata(int metadata) {
            return switch (mode) {
                case 0 -> metadata == 2 ? 3 : metadata == 3 ? 2 : metadata;
                case 1 -> switch (metadata) {
                    case 2 -> 4;
                    case 3 -> 5;
                    case 4 -> 2;
                    case 5 -> 3;
                    default -> metadata;
                };
                case 3 -> switch (metadata) {
                    case 2 -> 5;
                    case 3 -> 4;
                    case 4 -> 2;
                    case 5 -> 3;
                    default -> metadata;
                };
                default -> metadata;
            };
        }

        private int doorMetadata(int metadata) {
            return switch (mode) {
                case 0 -> metadata == 0 ? 2 : metadata == 2 ? 0 : metadata;
                case 1 -> metadata + 1 & 3;
                case 3 -> metadata + 3 & 3;
                default -> metadata;
            };
        }

        private int sourceDoorMetadata(Chunk chunk, int chunkX, int chunkZ, int worldX, int worldY, int worldZ,
                int metadata) {
            int dx = 0;
            int dz = 0;
            if (metadata == 0) {
                dz = 1;
            } else if (metadata == 1) {
                dx = -1;
            } else if (metadata == 2) {
                dz = -1;
            } else if (metadata == 3) {
                dx = 1;
            }
            int negativeSupport = sourceDoorNormalCube(chunk, chunkX, chunkZ, worldX - dx, worldY, worldZ - dz)
                    + sourceDoorNormalCube(chunk, chunkX, chunkZ, worldX - dx, worldY + 1, worldZ - dz);
            int positiveSupport = sourceDoorNormalCube(chunk, chunkX, chunkZ, worldX + dx, worldY, worldZ + dz)
                    + sourceDoorNormalCube(chunk, chunkX, chunkZ, worldX + dx, worldY + 1, worldZ + dz);
            boolean negativeDoor = sourceDoorBlock(chunk, chunkX, chunkZ, worldX - dx, worldY, worldZ - dz)
                    || sourceDoorBlock(chunk, chunkX, chunkZ, worldX - dx, worldY + 1, worldZ - dz);
            boolean positiveDoor = sourceDoorBlock(chunk, chunkX, chunkZ, worldX + dx, worldY, worldZ + dz)
                    || sourceDoorBlock(chunk, chunkX, chunkZ, worldX + dx, worldY + 1, worldZ + dz);
            if ((negativeDoor && !positiveDoor) || positiveSupport > negativeSupport) {
                return (metadata - 1 & 3) + 4;
            }
            return metadata;
        }

        private int sourceDoorNormalCube(Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ) {
            return BlockShape.isOpaqueCube(blockWorld(chunk, chunkX, chunkZ, worldX, y, worldZ)) ? 1 : 0;
        }

        private boolean sourceDoorBlock(Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ) {
            return blockWorld(chunk, chunkX, chunkZ, worldX, y, worldZ) == BlockType.WOODEN_DOOR;
        }

        private int torchMetadataFromSupportOffset(int dx, int dz) {
            if (dx < 0) {
                return 1;
            }
            if (dx > 0) {
                return 2;
            }
            if (dz < 0) {
                return 3;
            }
            return 4;
        }

        protected int localX(int x, int z) {
            return switch (mode) {
                case 1 -> bounds().maxX() - z;
                case 3 -> bounds().minX() + z;
                default -> bounds().minX() + x;
            };
        }

        protected int placementY(int y) {
            return placementMinY + y;
        }

        private StructureBoundingBox placementBounds() {
            return new StructureBoundingBox(bounds().minX(), placementMinY, bounds().minZ(),
                    bounds().maxX(), placementMinY + bounds().maxY() - bounds().minY(), bounds().maxZ());
        }

        protected int localZ(int x, int z) {
            return switch (mode) {
                case 2 -> bounds().maxZ() - z;
                case 0 -> bounds().minZ() + z;
                default -> bounds().minZ() + x;
            };
        }
    }

    private static class VillageHouse4GardenPiece extends VillageOrientedPiece {
        private final boolean roofAccessible;

        VillageHouse4GardenPiece(StructureBoundingBox box, int mode, int placementMinY, boolean roofAccessible) {
            super(box, mode, placementMinY);
            this.roofAccessible = roofAccessible;
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 4, 0, 4, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 0, 4, 0, 4, 4, 4, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 1, 4, 1, 3, 4, 3, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 0, 1, 0, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 0, 2, 0, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 0, 3, 0, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 4, 1, 0, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 4, 2, 0, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 4, 3, 0, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 0, 1, 4, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 0, 2, 4, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 0, 3, 4, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 4, 1, 4, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 4, 2, 4, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 4, 3, 4, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 0, 1, 1, 0, 3, 3, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 4, 1, 1, 4, 3, 3, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 4, 3, 3, 4, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 0, 2, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 2, 2, 4, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 4, 2, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 1, 1, 0, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 1, 2, 0, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 1, 3, 0, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 2, 3, 0, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 3, 3, 0, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 3, 2, 0, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 3, 1, 0, BlockType.OAK_PLANKS);
            if (getLocal(chunk, chunkX, chunkZ, 2, 0, -1) == BlockType.AIR
                    && getLocal(chunk, chunkX, chunkZ, 2, -1, -1) != BlockType.AIR) {
                setLocal(chunk, chunkX, chunkZ, 2, 0, -1, BlockType.COBBLESTONE_STAIRS, stairMetadata(3));
            }
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 1, 3, 3, 3, BlockType.AIR);
            if (roofAccessible) {
                for (int x = 0; x <= 4; x++) {
                    setLocal(chunk, chunkX, chunkZ, x, 5, 0, BlockType.FENCE);
                    setLocal(chunk, chunkX, chunkZ, x, 5, 4, BlockType.FENCE);
                }
                for (int z = 1; z <= 3; z++) {
                    setLocal(chunk, chunkX, chunkZ, 4, 5, z, BlockType.FENCE);
                    setLocal(chunk, chunkX, chunkZ, 0, 5, z, BlockType.FENCE);
                }
                int metadata = ladderMetadata(3);
                setLocal(chunk, chunkX, chunkZ, 3, 1, 3, BlockType.LADDER, metadata);
                setLocal(chunk, chunkX, chunkZ, 3, 2, 3, BlockType.LADDER, metadata);
                setLocal(chunk, chunkX, chunkZ, 3, 3, 3, BlockType.LADDER, metadata);
                setLocal(chunk, chunkX, chunkZ, 3, 4, 3, BlockType.LADDER, metadata);
            }
            setLocal(chunk, chunkX, chunkZ, 2, 3, 1, BlockType.TORCH);
            for (int z = 0; z < 5; z++) {
                for (int x = 0; x < 5; x++) {
                    clearLocalUpward(chunk, chunkX, chunkZ, x, 6, z);
                    fillLocalDownward(chunk, chunkX, chunkZ, BlockType.COBBLESTONE, x, -1, z);
                }
            }
            spawnVillagerLocal(world, chunkX, chunkZ, 1, 1, 2, Villager.PROFESSION_FARMER);
        }
    }

    private static class VillageChurchPiece extends VillageOrientedPiece {
        VillageChurchPiece(StructureBoundingBox box, int mode, int placementMinY) {
            super(box, mode, placementMinY);
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 1, 3, 3, 7, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 1, 5, 1, 3, 9, 3, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 1, 0, 0, 3, 0, 8, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 0, 3, 10, 0, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 0, 1, 1, 0, 10, 3, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 4, 1, 1, 4, 10, 3, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 4, 0, 4, 7, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 4, 0, 4, 4, 4, 7, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 8, 3, 4, 8, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 1, 5, 4, 3, 10, 4, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 1, 5, 5, 3, 5, 7, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 0, 9, 0, 4, 9, 4, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 0, 4, 0, 4, 4, 4, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 0, 11, 2, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 4, 11, 2, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 2, 11, 0, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 2, 11, 4, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 1, 1, 6, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 1, 1, 7, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 2, 1, 7, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 3, 1, 6, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 3, 1, 7, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 1, 1, 5, BlockType.COBBLESTONE_STAIRS, stairMetadata(3));
            setLocal(chunk, chunkX, chunkZ, 2, 1, 6, BlockType.COBBLESTONE_STAIRS, stairMetadata(3));
            setLocal(chunk, chunkX, chunkZ, 3, 1, 5, BlockType.COBBLESTONE_STAIRS, stairMetadata(3));
            setLocal(chunk, chunkX, chunkZ, 1, 2, 7, BlockType.COBBLESTONE_STAIRS, stairMetadata(1));
            setLocal(chunk, chunkX, chunkZ, 3, 2, 7, BlockType.COBBLESTONE_STAIRS, stairMetadata(0));
            setLocal(chunk, chunkX, chunkZ, 0, 2, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 0, 3, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 4, 2, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 4, 3, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 0, 6, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 0, 7, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 4, 6, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 4, 7, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 2, 6, 0, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 2, 7, 0, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 2, 6, 4, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 2, 7, 4, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 0, 3, 6, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 4, 3, 6, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 2, 3, 8, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 2, 4, 7, BlockType.TORCH);
            setLocal(chunk, chunkX, chunkZ, 1, 4, 6, BlockType.TORCH);
            setLocal(chunk, chunkX, chunkZ, 3, 4, 6, BlockType.TORCH);
            setLocal(chunk, chunkX, chunkZ, 2, 4, 5, BlockType.TORCH);
            int ladderData = ladderMetadata(4);
            for (int y = 1; y <= 9; y++) {
                setLocal(chunk, chunkX, chunkZ, 3, y, 3, BlockType.LADDER, ladderData);
            }
            setLocal(chunk, chunkX, chunkZ, 2, 1, 0, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, 2, 2, 0, BlockType.AIR);
            placeDoorLocal(chunk, chunkX, chunkZ, 2, 1, 0, 1);
            if (getLocal(chunk, chunkX, chunkZ, 2, 0, -1) == BlockType.AIR
                    && getLocal(chunk, chunkX, chunkZ, 2, -1, -1) != BlockType.AIR) {
                setLocal(chunk, chunkX, chunkZ, 2, 0, -1, BlockType.COBBLESTONE_STAIRS, stairMetadata(3));
            }
            for (int z = 0; z < 9; z++) {
                for (int x = 0; x < 5; x++) {
                    clearLocalUpward(chunk, chunkX, chunkZ, x, 12, z);
                    fillLocalDownward(chunk, chunkX, chunkZ, BlockType.COBBLESTONE, x, -1, z);
                }
            }
            spawnVillagerLocal(world, chunkX, chunkZ, 2, 1, 2, Villager.PROFESSION_PRIEST);
        }
    }

    private static class VillageHouse1Piece extends VillageOrientedPiece {
        VillageHouse1Piece(StructureBoundingBox box, int mode, int placementMinY) {
            super(box, mode, placementMinY);
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 1, 7, 5, 4, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 8, 0, 5, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 0, 5, 0, 8, 5, 5, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 0, 6, 1, 8, 6, 4, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 0, 7, 2, 8, 7, 3, BlockType.COBBLESTONE);
            int westStair = stairMetadata(3);
            int eastStair = stairMetadata(2);
            for (int roof = -1; roof <= 2; roof++) {
                for (int x = 0; x <= 8; x++) {
                    setLocal(chunk, chunkX, chunkZ, x, 6 + roof, roof, BlockType.OAK_STAIRS, westStair);
                    setLocal(chunk, chunkX, chunkZ, x, 6 + roof, 5 - roof, BlockType.OAK_STAIRS, eastStair);
                }
            }
            fillLocal(chunk, chunkX, chunkZ, 0, 1, 0, 0, 1, 5, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 5, 8, 1, 5, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 8, 1, 0, 8, 1, 4, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 2, 1, 0, 7, 1, 0, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 0, 0, 4, 0, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 5, 0, 4, 5, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 8, 2, 5, 8, 4, 5, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 8, 2, 0, 8, 4, 0, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 0, 2, 1, 0, 4, 4, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 1, 2, 5, 7, 4, 5, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 8, 2, 1, 8, 4, 4, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 1, 2, 0, 7, 4, 0, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 4, 2, 0, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 5, 2, 0, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 6, 2, 0, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 4, 3, 0, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 5, 3, 0, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 6, 3, 0, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 0, 2, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 0, 2, 3, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 0, 3, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 0, 3, 3, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 8, 2, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 8, 2, 3, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 8, 3, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 8, 3, 3, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 2, 2, 5, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 3, 2, 5, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 5, 2, 5, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 6, 2, 5, BlockType.GLASS_PANE);
            fillLocal(chunk, chunkX, chunkZ, 1, 4, 1, 7, 4, 1, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 1, 4, 4, 7, 4, 4, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 1, 3, 4, 7, 3, 4, BlockType.BOOKSHELF);
            setLocal(chunk, chunkX, chunkZ, 7, 1, 4, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 7, 1, 3, BlockType.OAK_STAIRS, stairMetadata(0));
            int benchStair = stairMetadata(3);
            setLocal(chunk, chunkX, chunkZ, 6, 1, 4, BlockType.OAK_STAIRS, benchStair);
            setLocal(chunk, chunkX, chunkZ, 5, 1, 4, BlockType.OAK_STAIRS, benchStair);
            setLocal(chunk, chunkX, chunkZ, 4, 1, 4, BlockType.OAK_STAIRS, benchStair);
            setLocal(chunk, chunkX, chunkZ, 3, 1, 4, BlockType.OAK_STAIRS, benchStair);
            setLocal(chunk, chunkX, chunkZ, 6, 1, 3, BlockType.FENCE);
            setLocal(chunk, chunkX, chunkZ, 6, 2, 3, BlockType.WOODEN_PRESSURE_PLATE);
            setLocal(chunk, chunkX, chunkZ, 4, 1, 3, BlockType.FENCE);
            setLocal(chunk, chunkX, chunkZ, 4, 2, 3, BlockType.WOODEN_PRESSURE_PLATE);
            setLocal(chunk, chunkX, chunkZ, 7, 1, 1, BlockType.CRAFTING_TABLE);
            setLocal(chunk, chunkX, chunkZ, 1, 1, 0, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, 1, 2, 0, BlockType.AIR);
            placeDoorLocal(chunk, chunkX, chunkZ, 1, 1, 0, 1);
            if (getLocal(chunk, chunkX, chunkZ, 1, 0, -1) == BlockType.AIR
                    && getLocal(chunk, chunkX, chunkZ, 1, -1, -1) != BlockType.AIR) {
                setLocal(chunk, chunkX, chunkZ, 1, 0, -1, BlockType.COBBLESTONE_STAIRS, stairMetadata(3));
            }
            for (int z = 0; z < 6; z++) {
                for (int x = 0; x < 9; x++) {
                    clearLocalUpward(chunk, chunkX, chunkZ, x, 9, z);
                    fillLocalDownward(chunk, chunkX, chunkZ, BlockType.COBBLESTONE, x, -1, z);
                }
            }
            spawnVillagerLocal(world, chunkX, chunkZ, 2, 1, 2, Villager.PROFESSION_LIBRARIAN);
        }
    }

    private static class VillageHouse3Piece extends VillageOrientedPiece {
        VillageHouse3Piece(StructureBoundingBox box, int mode, int placementMinY) {
            super(box, mode, placementMinY);
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 1, 7, 4, 4, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 2, 1, 6, 8, 4, 10, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 2, 0, 5, 8, 0, 10, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 1, 0, 1, 7, 0, 4, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 0, 3, 5, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 8, 0, 0, 8, 3, 10, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 1, 0, 0, 7, 2, 0, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 1, 0, 5, 2, 1, 5, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 2, 0, 6, 2, 3, 10, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 3, 0, 10, 7, 3, 10, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 1, 2, 0, 7, 3, 0, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 1, 2, 5, 2, 3, 5, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 0, 4, 1, 8, 4, 1, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 0, 4, 4, 3, 4, 4, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 0, 5, 2, 8, 5, 3, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 0, 4, 2, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 0, 4, 3, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 8, 4, 2, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 8, 4, 3, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 8, 4, 4, BlockType.OAK_PLANKS);
            int westStair = stairMetadata(3);
            int eastStair = stairMetadata(2);
            for (int roof = -1; roof <= 2; roof++) {
                for (int x = 0; x <= 8; x++) {
                    setLocal(chunk, chunkX, chunkZ, x, 4 + roof, roof, BlockType.OAK_STAIRS, westStair);
                    if ((roof > -1 || x <= 1) && (roof > 0 || x <= 3)
                            && (roof > 1 || x <= 4 || x >= 6)) {
                        setLocal(chunk, chunkX, chunkZ, x, 4 + roof, 5 - roof, BlockType.OAK_STAIRS, eastStair);
                    }
                }
            }
            fillLocal(chunk, chunkX, chunkZ, 3, 4, 5, 3, 4, 10, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 7, 4, 2, 7, 4, 10, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 4, 5, 4, 4, 5, 10, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 6, 5, 4, 6, 5, 10, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 5, 6, 3, 5, 6, 10, BlockType.OAK_PLANKS);
            int northStair = stairMetadata(0);
            for (int x = 4; x >= 1; x--) {
                setLocal(chunk, chunkX, chunkZ, x, 2 + x, 7 - x, BlockType.OAK_PLANKS);
                for (int z = 8 - x; z <= 10; z++) {
                    setLocal(chunk, chunkX, chunkZ, x, 2 + x, z, BlockType.OAK_STAIRS, northStair);
                }
            }
            int southStair = stairMetadata(1);
            setLocal(chunk, chunkX, chunkZ, 6, 6, 3, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 7, 5, 4, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 6, 6, 4, BlockType.OAK_STAIRS, southStair);
            for (int x = 6; x <= 8; x++) {
                for (int z = 5; z <= 10; z++) {
                    setLocal(chunk, chunkX, chunkZ, x, 12 - x, z, BlockType.OAK_STAIRS, southStair);
                }
            }
            setLocal(chunk, chunkX, chunkZ, 0, 2, 1, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 0, 2, 4, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 0, 2, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 0, 2, 3, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 4, 2, 0, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 5, 2, 0, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 6, 2, 0, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 8, 2, 1, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 8, 2, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 8, 2, 3, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 8, 2, 4, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 8, 2, 5, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 8, 2, 6, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 8, 2, 7, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 8, 2, 8, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 8, 2, 9, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 2, 2, 6, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 2, 2, 7, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 2, 2, 8, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 2, 2, 9, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 4, 4, 10, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 5, 4, 10, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 6, 4, 10, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 5, 5, 10, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 2, 1, 0, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, 2, 2, 0, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, 2, 3, 1, BlockType.TORCH);
            placeDoorLocal(chunk, chunkX, chunkZ, 2, 1, 0, 1);
            fillLocal(chunk, chunkX, chunkZ, 1, 0, -1, 3, 2, -1, BlockType.AIR);
            if (getLocal(chunk, chunkX, chunkZ, 2, 0, -1) == BlockType.AIR
                    && getLocal(chunk, chunkX, chunkZ, 2, -1, -1) != BlockType.AIR) {
                setLocal(chunk, chunkX, chunkZ, 2, 0, -1, BlockType.COBBLESTONE_STAIRS, stairMetadata(3));
            }
            for (int z = 0; z < 5; z++) {
                for (int x = 0; x < 9; x++) {
                    clearLocalUpward(chunk, chunkX, chunkZ, x, 7, z);
                    fillLocalDownward(chunk, chunkX, chunkZ, BlockType.COBBLESTONE, x, -1, z);
                }
            }
            for (int z = 5; z < 11; z++) {
                for (int x = 2; x < 9; x++) {
                    clearLocalUpward(chunk, chunkX, chunkZ, x, 7, z);
                    fillLocalDownward(chunk, chunkX, chunkZ, BlockType.COBBLESTONE, x, -1, z);
                }
            }
            spawnVillagersLocal(world, chunkX, chunkZ, 4, 1, 2, 2);
        }
    }

    private static class VillageBlacksmithPiece extends VillageOrientedPiece {
        VillageBlacksmithPiece(StructureBoundingBox box, int mode, int placementMinY) {
            super(box, mode, placementMinY);
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 0, 1, 0, 9, 4, 6, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 9, 0, 6, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 0, 4, 0, 9, 4, 6, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 0, 5, 0, 9, 5, 6, BlockType.STONE_SLAB);
            fillLocal(chunk, chunkX, chunkZ, 1, 5, 1, 8, 5, 5, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 0, 2, 3, 0, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 0, 1, 0, 0, 4, 0, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 3, 1, 0, 3, 4, 0, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 0, 1, 6, 0, 4, 6, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 3, 3, 1, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 3, 1, 2, 3, 3, 2, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 4, 1, 3, 5, 3, 3, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 0, 1, 1, 0, 3, 5, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 6, 5, 3, 6, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 5, 1, 0, 5, 3, 0, BlockType.FENCE);
            fillLocal(chunk, chunkX, chunkZ, 9, 1, 0, 9, 3, 0, BlockType.FENCE);
            fillLocal(chunk, chunkX, chunkZ, 6, 1, 4, 9, 4, 6, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 7, 1, 5, BlockType.FLOWING_LAVA);
            setLocal(chunk, chunkX, chunkZ, 8, 1, 5, BlockType.FLOWING_LAVA);
            setLocal(chunk, chunkX, chunkZ, 9, 2, 5, BlockType.IRON_BARS);
            setLocal(chunk, chunkX, chunkZ, 9, 2, 4, BlockType.IRON_BARS);
            fillLocal(chunk, chunkX, chunkZ, 7, 2, 4, 8, 2, 5, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, 6, 1, 3, BlockType.COBBLESTONE);
            setLocal(chunk, chunkX, chunkZ, 6, 2, 3, BlockType.FURNACE);
            setLocal(chunk, chunkX, chunkZ, 6, 3, 3, BlockType.FURNACE);
            setLocal(chunk, chunkX, chunkZ, 8, 1, 1, BlockType.DOUBLE_STONE_SLAB);
            setLocal(chunk, chunkX, chunkZ, 0, 2, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 0, 2, 4, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 2, 2, 6, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 4, 2, 6, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 2, 1, 4, BlockType.FENCE);
            setLocal(chunk, chunkX, chunkZ, 2, 2, 4, BlockType.WOODEN_PRESSURE_PLATE);
            setLocal(chunk, chunkX, chunkZ, 1, 1, 5, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 2, 1, 5, BlockType.OAK_STAIRS, stairMetadata(3));
            setLocal(chunk, chunkX, chunkZ, 1, 1, 4, BlockType.OAK_STAIRS, stairMetadata(1));
            for (int x = 6; x <= 8; x++) {
                if (getLocal(chunk, chunkX, chunkZ, x, 0, -1) == BlockType.AIR
                        && getLocal(chunk, chunkX, chunkZ, x, -1, -1) != BlockType.AIR) {
                    setLocal(chunk, chunkX, chunkZ, x, 0, -1, BlockType.COBBLESTONE_STAIRS, stairMetadata(3));
                }
            }
            for (int z = 0; z < 7; z++) {
                for (int x = 0; x < 10; x++) {
                    clearLocalUpward(chunk, chunkX, chunkZ, x, 6, z);
                    fillLocalDownward(chunk, chunkX, chunkZ, BlockType.COBBLESTONE, x, -1, z);
                }
            }
            spawnVillagerLocal(world, chunkX, chunkZ, 7, 1, 1, Villager.PROFESSION_SMITH);
        }
    }

    private static class VillageWoodHutPiece extends VillageOrientedPiece {
        private final boolean tallHouse;
        private final int tablePosition;

        VillageWoodHutPiece(StructureBoundingBox box, int mode, int placementMinY,
                boolean tallHouse, int tablePosition) {
            super(box, mode, placementMinY);
            this.tallHouse = tallHouse;
            this.tablePosition = tablePosition;
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 1, 3, 5, 4, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 3, 0, 4, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 1, 0, 1, 2, 0, 3, BlockType.DIRT);
            if (tallHouse) {
                fillLocal(chunk, chunkX, chunkZ, 1, 4, 1, 2, 4, 3, BlockType.OAK_LOG);
            } else {
                fillLocal(chunk, chunkX, chunkZ, 1, 5, 1, 2, 5, 3, BlockType.OAK_LOG);
            }
            setLocal(chunk, chunkX, chunkZ, 1, 4, 0, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 2, 4, 0, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 1, 4, 4, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 2, 4, 4, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 0, 4, 1, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 0, 4, 2, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 0, 4, 3, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 3, 4, 1, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 3, 4, 2, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 3, 4, 3, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 0, 1, 0, 0, 3, 0, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 3, 1, 0, 3, 3, 0, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 0, 1, 4, 0, 3, 4, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 3, 1, 4, 3, 3, 4, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 0, 1, 1, 0, 3, 3, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 3, 1, 1, 3, 3, 3, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 0, 2, 3, 0, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 4, 2, 3, 4, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 0, 2, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 3, 2, 2, BlockType.GLASS_PANE);
            if (tablePosition > 0) {
                setLocal(chunk, chunkX, chunkZ, tablePosition, 1, 3, BlockType.FENCE);
                setLocal(chunk, chunkX, chunkZ, tablePosition, 2, 3, BlockType.WOODEN_PRESSURE_PLATE);
            }
            setLocal(chunk, chunkX, chunkZ, 1, 1, 0, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, 1, 2, 0, BlockType.AIR);
            placeDoorLocal(chunk, chunkX, chunkZ, 1, 1, 0, 1);
            if (getLocal(chunk, chunkX, chunkZ, 1, 0, -1) == BlockType.AIR
                    && getLocal(chunk, chunkX, chunkZ, 1, -1, -1) != BlockType.AIR) {
                setLocal(chunk, chunkX, chunkZ, 1, 0, -1, BlockType.COBBLESTONE_STAIRS, stairMetadata(3));
            }
            for (int z = 0; z < 5; z++) {
                for (int x = 0; x < 4; x++) {
                    clearLocalUpward(chunk, chunkX, chunkZ, x, 6, z);
                    fillLocalDownward(chunk, chunkX, chunkZ, BlockType.COBBLESTONE, x, -1, z);
                }
            }
            spawnVillagerLocal(world, chunkX, chunkZ, 1, 1, 2, Villager.PROFESSION_FARMER);
        }
    }

    private static class VillageTorchPiece extends VillageOrientedPiece {
        VillageTorchPiece(StructureBoundingBox box, int mode, int placementMinY) {
            super(box, mode, placementMinY);
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 2, 3, 1, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, 1, 0, 0, BlockType.FENCE);
            setLocal(chunk, chunkX, chunkZ, 1, 1, 0, BlockType.FENCE);
            setLocal(chunk, chunkX, chunkZ, 1, 2, 0, BlockType.FENCE);
            setLocal(chunk, chunkX, chunkZ, 1, 3, 0, BlockType.WHITE_WOOL, 15);
            setTorchAttachedLocal(chunk, chunkX, chunkZ, 0, 3, 0, 1, 0);
            setTorchAttachedLocal(chunk, chunkX, chunkZ, 1, 3, 1, 1, 0);
            setTorchAttachedLocal(chunk, chunkX, chunkZ, 2, 3, 0, 1, 0);
            setTorchAttachedLocal(chunk, chunkX, chunkZ, 1, 3, -1, 1, 0);
        }
    }

    private static class VillageHallPiece extends VillageOrientedPiece {
        VillageHallPiece(StructureBoundingBox box, int mode, int placementMinY) {
            super(box, mode, placementMinY);
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            fillLocal(chunk, chunkX, chunkZ, 1, 1, 1, 7, 4, 4, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 2, 1, 6, 8, 4, 10, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 2, 0, 6, 8, 0, 10, BlockType.DIRT);
            setLocal(chunk, chunkX, chunkZ, 6, 0, 6, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 2, 1, 6, 2, 1, 10, BlockType.FENCE);
            fillLocal(chunk, chunkX, chunkZ, 8, 1, 6, 8, 1, 10, BlockType.FENCE);
            fillLocal(chunk, chunkX, chunkZ, 3, 1, 10, 7, 1, 10, BlockType.FENCE);
            fillLocal(chunk, chunkX, chunkZ, 1, 0, 1, 7, 0, 4, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 0, 3, 5, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 8, 0, 0, 8, 3, 5, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 1, 0, 0, 7, 1, 0, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 1, 0, 5, 7, 1, 5, BlockType.COBBLESTONE);
            fillLocal(chunk, chunkX, chunkZ, 1, 2, 0, 7, 3, 0, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 1, 2, 5, 7, 3, 5, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 0, 4, 1, 8, 4, 1, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 0, 4, 4, 8, 4, 4, BlockType.OAK_PLANKS);
            fillLocal(chunk, chunkX, chunkZ, 0, 5, 2, 8, 5, 3, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 0, 4, 2, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 0, 4, 3, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 8, 4, 2, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 8, 4, 3, BlockType.OAK_PLANKS);
            int westStair = stairMetadata(3);
            int eastStair = stairMetadata(2);
            for (int roof = -1; roof <= 2; roof++) {
                for (int x = 0; x <= 8; x++) {
                    setLocal(chunk, chunkX, chunkZ, x, 4 + roof, roof, BlockType.OAK_STAIRS, westStair);
                    setLocal(chunk, chunkX, chunkZ, x, 4 + roof, 5 - roof, BlockType.OAK_STAIRS, eastStair);
                }
            }
            setLocal(chunk, chunkX, chunkZ, 0, 2, 1, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 0, 2, 4, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 8, 2, 1, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 8, 2, 4, BlockType.OAK_LOG);
            setLocal(chunk, chunkX, chunkZ, 0, 2, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 0, 2, 3, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 8, 2, 2, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 8, 2, 3, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 2, 2, 5, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 3, 2, 5, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 5, 2, 0, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 6, 2, 5, BlockType.GLASS_PANE);
            setLocal(chunk, chunkX, chunkZ, 2, 1, 3, BlockType.FENCE);
            setLocal(chunk, chunkX, chunkZ, 2, 2, 3, BlockType.WOODEN_PRESSURE_PLATE);
            setLocal(chunk, chunkX, chunkZ, 1, 1, 4, BlockType.OAK_PLANKS);
            setLocal(chunk, chunkX, chunkZ, 2, 1, 4, BlockType.OAK_STAIRS, stairMetadata(3));
            setLocal(chunk, chunkX, chunkZ, 1, 1, 3, BlockType.OAK_STAIRS, stairMetadata(1));
            fillLocal(chunk, chunkX, chunkZ, 5, 0, 1, 7, 0, 3, BlockType.DOUBLE_STONE_SLAB);
            setLocal(chunk, chunkX, chunkZ, 6, 1, 1, BlockType.DOUBLE_STONE_SLAB);
            setLocal(chunk, chunkX, chunkZ, 6, 1, 2, BlockType.DOUBLE_STONE_SLAB);
            setLocal(chunk, chunkX, chunkZ, 2, 1, 0, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, 2, 2, 0, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, 2, 3, 1, BlockType.TORCH);
            placeDoorLocal(chunk, chunkX, chunkZ, 2, 1, 0, 1);
            if (getLocal(chunk, chunkX, chunkZ, 2, 0, -1) == BlockType.AIR
                    && getLocal(chunk, chunkX, chunkZ, 2, -1, -1) != BlockType.AIR) {
                setLocal(chunk, chunkX, chunkZ, 2, 0, -1, BlockType.COBBLESTONE_STAIRS, stairMetadata(3));
            }
            setLocal(chunk, chunkX, chunkZ, 6, 1, 5, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, 6, 2, 5, BlockType.AIR);
            setLocal(chunk, chunkX, chunkZ, 6, 3, 4, BlockType.TORCH);
            placeDoorLocal(chunk, chunkX, chunkZ, 6, 1, 5, 1);
            for (int z = 0; z < 5; z++) {
                for (int x = 0; x < 9; x++) {
                    clearLocalUpward(chunk, chunkX, chunkZ, x, 7, z);
                    fillLocalDownward(chunk, chunkX, chunkZ, BlockType.COBBLESTONE, x, -1, z);
                }
            }
            spawnVillagersLocal(world, chunkX, chunkZ, 4, 1, 2, 2,
                    Villager.PROFESSION_BUTCHER, Villager.PROFESSION_FARMER);
        }
    }

    private static class VillageFarmPiece extends VillageOrientedPiece {
        private final boolean wide;

        VillageFarmPiece(StructureBoundingBox box, int mode, int placementMinY, boolean wide) {
            super(box, mode, placementMinY);
            this.wide = wide;
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            placeWithRandom(chunk, chunkX, chunkZ, farmRandom(seed));
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ, Random placementRandom) {
            placeWithRandom(chunk, chunkX, chunkZ, placementRandom);
        }

        private void placeWithRandom(Chunk chunk, int chunkX, int chunkZ, Random random) {
            if (wide) {
                placeWideFarm(chunk, chunkX, chunkZ, random);
            } else {
                placeNarrowFarm(chunk, chunkX, chunkZ, random);
            }
        }

        private void placeWideFarm(Chunk chunk, int chunkX, int chunkZ, Random random) {
            fillLocal(chunk, chunkX, chunkZ, 0, 1, 0, 12, 4, 8, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 1, 0, 1, 2, 0, 7, BlockType.FARMLAND);
            fillLocal(chunk, chunkX, chunkZ, 4, 0, 1, 5, 0, 7, BlockType.FARMLAND);
            fillLocal(chunk, chunkX, chunkZ, 7, 0, 1, 8, 0, 7, BlockType.FARMLAND);
            fillLocal(chunk, chunkX, chunkZ, 10, 0, 1, 11, 0, 7, BlockType.FARMLAND);
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 0, 0, 8, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 6, 0, 0, 6, 0, 8, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 12, 0, 0, 12, 0, 8, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 1, 0, 0, 11, 0, 0, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 1, 0, 8, 11, 0, 8, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 3, 0, 1, 3, 0, 7, BlockType.FLOWING_WATER);
            fillLocal(chunk, chunkX, chunkZ, 9, 0, 1, 9, 0, 7, BlockType.FLOWING_WATER);
            for (int z = 1; z <= 7; z++) {
                placeCropRow(random, chunk, chunkX, chunkZ, 1, z);
                placeCropRow(random, chunk, chunkX, chunkZ, 2, z);
                placeCropRow(random, chunk, chunkX, chunkZ, 4, z);
                placeCropRow(random, chunk, chunkX, chunkZ, 5, z);
                placeCropRow(random, chunk, chunkX, chunkZ, 7, z);
                placeCropRow(random, chunk, chunkX, chunkZ, 8, z);
                placeCropRow(random, chunk, chunkX, chunkZ, 10, z);
                placeCropRow(random, chunk, chunkX, chunkZ, 11, z);
            }
            clearAndSupport(chunk, chunkX, chunkZ, 13, 9);
        }

        private void placeNarrowFarm(Chunk chunk, int chunkX, int chunkZ, Random random) {
            fillLocal(chunk, chunkX, chunkZ, 0, 1, 0, 6, 4, 8, BlockType.AIR);
            fillLocal(chunk, chunkX, chunkZ, 1, 0, 1, 2, 0, 7, BlockType.FARMLAND);
            fillLocal(chunk, chunkX, chunkZ, 4, 0, 1, 5, 0, 7, BlockType.FARMLAND);
            fillLocal(chunk, chunkX, chunkZ, 0, 0, 0, 0, 0, 8, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 6, 0, 0, 6, 0, 8, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 1, 0, 0, 5, 0, 0, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 1, 0, 8, 5, 0, 8, BlockType.OAK_LOG);
            fillLocal(chunk, chunkX, chunkZ, 3, 0, 1, 3, 0, 7, BlockType.FLOWING_WATER);
            for (int z = 1; z <= 7; z++) {
                placeCropRow(random, chunk, chunkX, chunkZ, 1, z);
                placeCropRow(random, chunk, chunkX, chunkZ, 2, z);
                placeCropRow(random, chunk, chunkX, chunkZ, 4, z);
                placeCropRow(random, chunk, chunkX, chunkZ, 5, z);
            }
            clearAndSupport(chunk, chunkX, chunkZ, 7, 9);
        }

        private void placeCropRow(Random random, Chunk chunk, int chunkX, int chunkZ, int x, int z) {
            setLocal(chunk, chunkX, chunkZ, x, 1, z, BlockType.CROPS, randomIntegerInRange(random, 2, 7));
        }

        private void clearAndSupport(Chunk chunk, int chunkX, int chunkZ, int width, int depth) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    clearLocalUpward(chunk, chunkX, chunkZ, x, 4, z);
                    fillLocalDownward(chunk, chunkX, chunkZ, BlockType.DIRT, x, -1, z);
                }
            }
        }

        private Random farmRandom(long seed) {
            long mixed = seed ^ 0x9E3779B97F4A7C15L;
            mixed ^= (long) bounds().minX() * 341873128712L;
            mixed ^= (long) bounds().minZ() * 132897987541L;
            mixed ^= wide ? 0x46A5D1E1L : 0x46A5D1E2L;
            return new Random(mixed);
        }
    }

    private static class VillagePathPiece extends BoxPiece {
        private final ReleaseOneWorldGenerator generator;
        private final int mode;
        private final int componentType;

        VillagePathPiece(StructureBoundingBox box, ReleaseOneWorldGenerator generator, int mode, int componentType) {
            super(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
            this.generator = generator;
            this.mode = mode;
            this.componentType = componentType;
        }

        @Override
        public void place(World world, Chunk chunk, long seed, int chunkX, int chunkZ) {
            for (int x = bounds().minX(); x <= bounds().maxX(); x++) {
                for (int z = bounds().minZ(); z <= bounds().maxZ(); z++) {
                    int y = roadSurfaceY(chunk, chunkX, chunkZ, x, z);
                    set(chunk, chunkX, chunkZ, x, y, z, BlockType.GRAVEL);
                }
            }
        }

        private int roadSurfaceY(Chunk chunk, int chunkX, int chunkZ, int x, int z) {
            int localX = x - chunkX * Chunk.WIDTH;
            int localZ = z - chunkZ * Chunk.DEPTH;
            if (localX >= 0 && localX < Chunk.WIDTH && localZ >= 0 && localZ < Chunk.DEPTH) {
                int topY = topSourceRoadBlockY(chunk, localX, localZ);
                if (topY >= 0) {
                    return topY;
                }
            }
            return sourceFindTopSolidBlock(generator, x, z) - 1;
        }

        private static int topSourceRoadBlockY(Chunk chunk, int localX, int localZ) {
            for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
                BlockType block = chunk.getBlock(localX, y, localZ);
                if (isSourceRoadHeightBlock(block)) {
                    return y;
                }
            }
            return -1;
        }

        private static boolean isSourceRoadHeightBlock(BlockType block) {
            return block != BlockType.LEAVES && block.isSolid();
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

        protected boolean hasLiquidInChunkEnvelope(Chunk chunk, int chunkX, int chunkZ) {
            int chunkMinX = chunkX * Chunk.WIDTH;
            int chunkMinZ = chunkZ * Chunk.DEPTH;
            int minX = Math.max(bounds.minX() - 1, chunkMinX);
            int minY = Math.max(bounds.minY() - 1, 0);
            int minZ = Math.max(bounds.minZ() - 1, chunkMinZ);
            int maxX = Math.min(bounds.maxX() + 1, chunkMinX + Chunk.WIDTH - 1);
            int maxY = Math.min(bounds.maxY() + 1, Chunk.HEIGHT - 1);
            int maxZ = Math.min(bounds.maxZ() + 1, chunkMinZ + Chunk.DEPTH - 1);
            if (minX > maxX || minY > maxY || minZ > maxZ) {
                return false;
            }
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (isLiquid(blockWorld(chunk, chunkX, chunkZ, x, minY, z))
                            || isLiquid(blockWorld(chunk, chunkX, chunkZ, x, maxY, z))) {
                        return true;
                    }
                }
            }
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    if (isLiquid(blockWorld(chunk, chunkX, chunkZ, x, y, minZ))
                            || isLiquid(blockWorld(chunk, chunkX, chunkZ, x, y, maxZ))) {
                        return true;
                    }
                }
            }
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (isLiquid(blockWorld(chunk, chunkX, chunkZ, minX, y, z))
                            || isLiquid(blockWorld(chunk, chunkX, chunkZ, maxX, y, z))) {
                        return true;
                    }
                }
            }
            return false;
        }

        protected BlockType blockWorld(Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ) {
            if (!isInsideChunk(worldX, y, worldZ, chunkX, chunkZ)) {
                return BlockType.AIR;
            }
            return chunk.getBlock(Math.floorMod(worldX, Chunk.WIDTH), y, Math.floorMod(worldZ, Chunk.DEPTH));
        }

        protected static boolean isInsideChunk(int worldX, int y, int worldZ, int chunkX, int chunkZ) {
            return y >= 0 && y < Chunk.HEIGHT
                    && Math.floorDiv(worldX, Chunk.WIDTH) == chunkX
                    && Math.floorDiv(worldZ, Chunk.DEPTH) == chunkZ;
        }

        private static boolean isLiquid(BlockType type) {
            return type == BlockType.WATER || type == BlockType.FLOWING_WATER
                    || type == BlockType.LAVA || type == BlockType.FLOWING_LAVA;
        }

        protected static Random random(long seed, int x, int y, int z) {
            return new Random(seed ^ (long) x * 341873128712L ^ (long) y * 42317861L ^ (long) z * 132897987541L);
        }
    }
}
