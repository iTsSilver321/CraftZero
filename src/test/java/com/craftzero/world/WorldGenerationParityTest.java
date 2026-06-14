package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class WorldGenerationParityTest {
    @Test
    @DisplayName("New worlds should default to the Release 1.0 overworld generator")
    void newWorldsUseReleaseOneGeneratorByDefault() {
        World world = new World(1234L);
        try {
            assertEquals(WorldGenerator.RELEASE_ONE, world.getGeneratorId());
            assertSame(Dimension.OVERWORLD, world.getDimension());
            assertNotSame(BiomeType.HELL, world.getReleaseBiome(0, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Saved dimensions should select matching Release 1.0 dimension generators")
    void explicitDimensionsSelectMatchingGenerators() {
        World nether = new World(1234L, WorldGenerator.RELEASE_ONE, Dimension.NETHER);
        World end = new World(1234L, WorldGenerators.generatorIdFor(Dimension.THE_END), null);
        try {
            assertEquals("minecraft_java_1_0_nether", nether.getGeneratorId());
            assertSame(Dimension.NETHER, nether.getDimension());
            assertSame(BiomeType.HELL, nether.getReleaseBiome(12, -4));

            assertEquals("minecraft_java_1_0_end", end.getGeneratorId());
            assertSame(Dimension.THE_END, end.getDimension());
            assertSame(BiomeType.SKY, end.getReleaseBiome(12, -4));
        } finally {
            nether.cleanup();
            end.cleanup();
        }
    }

    @Test
    @DisplayName("Base terrain block lookup should not duplicate ore generation")
    void baseTerrainLookupDoesNotInjectOres() throws Exception {
        World world = new World(1234L);
        try {
            Class<?> biomeType = Class.forName("com.craftzero.world.World$BiomeType");
            Object plains = enumConstant(biomeType, "PLAINS");
            Method getBlockType = World.class.getDeclaredMethod(
                    "getBlockType", int.class, int.class, biomeType, int.class, int.class);
            getBlockType.setAccessible(true);

            for (int x = -16; x <= 16; x += 4) {
                for (int z = -16; z <= 16; z += 4) {
                    for (int y = 1; y < 50; y++) {
                        BlockType type = (BlockType) getBlockType.invoke(world, y, 70, plains, x, z);
                        assertSame(BlockType.STONE, type,
                                "Base terrain should return stone below the dirt layer; ores belong to OreGenerator");
                    }
                }
            }
        } finally {
            world.cleanup();
        }
    }

    private static Object enumConstant(Class<?> enumType, String name) {
        for (Object value : enumType.getEnumConstants()) {
            if (((Enum<?>) value).name().equals(name)) {
                return value;
            }
        }
        fail("Missing enum constant " + name);
        return null;
    }
}
