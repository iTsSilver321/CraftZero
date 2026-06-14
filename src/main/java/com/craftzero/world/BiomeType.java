package com.craftzero.world;

/**
 * Minecraft Java Release 1.0 biome ids and surface defaults.
 */
public enum BiomeType {
    OCEAN(0, BlockType.GRASS, BlockType.DIRT, false, false),
    PLAINS(1, BlockType.GRASS, BlockType.DIRT, false, false),
    DESERT(2, BlockType.SAND, BlockType.SAND, false, false),
    EXTREME_HILLS(3, BlockType.GRASS, BlockType.DIRT, false, false),
    FOREST(4, BlockType.GRASS, BlockType.DIRT, true, false),
    TAIGA(5, BlockType.GRASS, BlockType.DIRT, true, false),
    SWAMPLAND(6, BlockType.GRASS, BlockType.DIRT, true, false),
    RIVER(7, BlockType.GRASS, BlockType.DIRT, false, false),
    HELL(8, BlockType.NETHERRACK, BlockType.NETHERRACK, false, false),
    SKY(9, BlockType.END_STONE, BlockType.END_STONE, false, false),
    FROZEN_OCEAN(10, BlockType.GRASS, BlockType.DIRT, false, true),
    FROZEN_RIVER(11, BlockType.GRASS, BlockType.DIRT, false, true),
    ICE_PLAINS(12, BlockType.GRASS, BlockType.DIRT, false, true),
    ICE_MOUNTAINS(13, BlockType.GRASS, BlockType.DIRT, false, true),
    MUSHROOM_ISLAND(14, BlockType.MYCELIUM, BlockType.DIRT, true, false),
    MUSHROOM_ISLAND_SHORE(15, BlockType.MYCELIUM, BlockType.DIRT, true, false),
    BEACH(16, BlockType.SAND, BlockType.SAND, false, false),
    DESERT_HILLS(17, BlockType.SAND, BlockType.SAND, false, false),
    FOREST_HILLS(18, BlockType.GRASS, BlockType.DIRT, true, false),
    TAIGA_HILLS(19, BlockType.GRASS, BlockType.DIRT, true, false),
    EXTREME_HILLS_EDGE(20, BlockType.GRASS, BlockType.DIRT, false, false);

    private final int id;
    private final BlockType topBlock;
    private final BlockType fillerBlock;
    private final boolean treeBiome;
    private final boolean frozen;

    BiomeType(int id, BlockType topBlock, BlockType fillerBlock, boolean treeBiome, boolean frozen) {
        this.id = id;
        this.topBlock = topBlock;
        this.fillerBlock = fillerBlock;
        this.treeBiome = treeBiome;
        this.frozen = frozen;
    }

    public int getId() {
        return id;
    }

    public BlockType getTopBlock() {
        return topBlock;
    }

    public BlockType getFillerBlock() {
        return fillerBlock;
    }

    public boolean isTreeBiome() {
        return treeBiome;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public boolean isOceanic() {
        return this == OCEAN || this == FROZEN_OCEAN;
    }
}
