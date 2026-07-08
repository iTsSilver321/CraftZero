package com.craftzero.world;

/**
 * Minecraft Java Release 1.0 biome ids and surface defaults.
 */
public enum BiomeType {
    OCEAN(0, BlockType.GRASS, BlockType.DIRT, 0.5f, false, false),
    PLAINS(1, BlockType.GRASS, BlockType.DIRT, 0.8f, false, false),
    DESERT(2, BlockType.SAND, BlockType.SAND, 2.0f, false, false),
    EXTREME_HILLS(3, BlockType.GRASS, BlockType.DIRT, 0.2f, false, false),
    FOREST(4, BlockType.GRASS, BlockType.DIRT, 0.7f, true, false),
    TAIGA(5, BlockType.GRASS, BlockType.DIRT, 0.3f, true, false),
    SWAMPLAND(6, BlockType.GRASS, BlockType.DIRT, 0.8f, true, false),
    RIVER(7, BlockType.GRASS, BlockType.DIRT, 0.5f, false, false),
    HELL(8, BlockType.NETHERRACK, BlockType.NETHERRACK, 2.0f, false, false),
    SKY(9, BlockType.END_STONE, BlockType.END_STONE, 0.5f, false, false),
    FROZEN_OCEAN(10, BlockType.GRASS, BlockType.DIRT, 0.0f, false, true),
    FROZEN_RIVER(11, BlockType.GRASS, BlockType.DIRT, 0.0f, false, true),
    ICE_PLAINS(12, BlockType.GRASS, BlockType.DIRT, 0.0f, false, true),
    ICE_MOUNTAINS(13, BlockType.GRASS, BlockType.DIRT, 0.0f, false, true),
    MUSHROOM_ISLAND(14, BlockType.MYCELIUM, BlockType.DIRT, 0.9f, true, false),
    MUSHROOM_ISLAND_SHORE(15, BlockType.MYCELIUM, BlockType.DIRT, 0.9f, true, false),
    BEACH(16, BlockType.SAND, BlockType.SAND, 0.8f, false, false),
    DESERT_HILLS(17, BlockType.SAND, BlockType.SAND, 2.0f, false, false),
    FOREST_HILLS(18, BlockType.GRASS, BlockType.DIRT, 0.7f, true, false),
    TAIGA_HILLS(19, BlockType.GRASS, BlockType.DIRT, 0.3f, true, false),
    EXTREME_HILLS_EDGE(20, BlockType.GRASS, BlockType.DIRT, 0.2f, false, false);

    private final int id;
    private final BlockType topBlock;
    private final BlockType fillerBlock;
    private final float temperature;
    private final boolean treeBiome;
    private final boolean frozen;

    BiomeType(int id, BlockType topBlock, BlockType fillerBlock, float temperature, boolean treeBiome, boolean frozen) {
        this.id = id;
        this.topBlock = topBlock;
        this.fillerBlock = fillerBlock;
        this.temperature = temperature;
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

    public float getTemperature() {
        return temperature;
    }

    public boolean isTreeBiome() {
        return treeBiome;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public boolean canFreezeWater() {
        return this == FROZEN_OCEAN || this == FROZEN_RIVER || this == ICE_PLAINS || this == ICE_MOUNTAINS;
    }

    public boolean hasPrecipitation() {
        return this != DESERT && this != DESERT_HILLS && this != HELL && this != SKY;
    }

    public boolean isOceanic() {
        return this == OCEAN || this == FROZEN_OCEAN;
    }

    float minHeight() {
        return switch (this) {
            case OCEAN, FROZEN_OCEAN, MUSHROOM_ISLAND_SHORE -> -1.0F;
            case RIVER, FROZEN_RIVER -> -0.5F;
            case SWAMPLAND -> -0.2F;
            case BEACH -> 0.0F;
            case EXTREME_HILLS, ICE_MOUNTAINS, MUSHROOM_ISLAND,
                    DESERT_HILLS, FOREST_HILLS, TAIGA_HILLS, EXTREME_HILLS_EDGE -> 0.2F;
            default -> 0.1F;
        };
    }

    float maxHeight() {
        return switch (this) {
            case OCEAN -> 0.4F;
            case FROZEN_OCEAN -> 0.5F;
            case RIVER, FROZEN_RIVER -> 0.0F;
            case SWAMPLAND, BEACH, MUSHROOM_ISLAND_SHORE -> 0.1F;
            case EXTREME_HILLS, ICE_MOUNTAINS -> 1.8F;
            case MUSHROOM_ISLAND -> 1.0F;
            case DESERT_HILLS, TAIGA_HILLS -> 0.7F;
            case FOREST_HILLS -> 0.6F;
            case EXTREME_HILLS_EDGE -> 0.8F;
            case TAIGA -> 0.4F;
            case DESERT -> 0.2F;
            default -> 0.3F;
        };
    }
}
