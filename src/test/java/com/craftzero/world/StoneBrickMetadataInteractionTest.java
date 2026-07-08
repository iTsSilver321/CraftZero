package com.craftzero.world;

import com.craftzero.entity.mob.Silverfish;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoneBrickMetadataInteractionTest {

    @Test
    @DisplayName("Stone brick and monster egg textures should honor Release 1.0 metadata")
    void stoneBrickFamilyTexturesHonorMetadata() {
        assertEquals(54, textureIndex(BlockType.STONE_BRICK, 0));
        assertEquals(100, textureIndex(BlockType.STONE_BRICK, 1));
        assertEquals(101, textureIndex(BlockType.STONE_BRICK, 2));
        assertEquals(213, textureIndex(BlockType.STONE_BRICK, 3));

        assertEquals(1, textureIndex(BlockType.INFESTED_STONE, 0));
        assertEquals(16, textureIndex(BlockType.INFESTED_STONE, 1));
        assertEquals(54, textureIndex(BlockType.INFESTED_STONE, 2));
    }

    @Test
    @DisplayName("Monster eggs should break instantly and spawn silverfish without drops")
    void monsterEggBreakSpawnsSilverfishWithoutDrops() {
        World world = new World(6100L);
        try {
            world.setBlock(0, 100, 0, BlockType.INFESTED_STONE, 2);

            assertEquals(0.0f, BlockType.INFESTED_STONE.getHardness(), 0.0001f);
            assertTrue(world.breakBlock(0, 100, 0, true));

            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertTrue(world.getDroppedItems().isEmpty());
            assertTrue(world.hasEntityOfType(Silverfish.class));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Hurt silverfish should wake nearby monster eggs without leaving stone behind")
    void hurtSilverfishWakesNearbyMonsterEggsIntoAir() {
        World world = new World(6101L);
        try {
            Silverfish silverfish = new Silverfish();
            silverfish.setPosition(0.5f, 100.0f, 0.5f);
            world.spawnEntity(silverfish);
            world.updateEntities(1.0f / 20.0f);
            world.setBlock(1, 100, 0, BlockType.INFESTED_STONE, 2);

            assertTrue(silverfish.damage(1.0f, com.craftzero.combat.DamageSource.generic()));
            world.updateEntities(1.0f / 20.0f);

            assertSame(BlockType.AIR, world.getBlock(1, 100, 0));
            assertEquals(2L, world.getEntities().stream()
                    .filter(Silverfish.class::isInstance)
                    .count());
            assertTrue(world.getDroppedItems().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Hurt silverfish should wake monster eggs in the old tall source search volume")
    void hurtSilverfishWakesTallSourceSearchVolume() {
        World world = new World(6102L);
        try {
            Silverfish silverfish = new Silverfish();
            silverfish.setPosition(0.5f, 100.0f, 0.5f);
            world.spawnEntity(silverfish);
            world.setBlock(0, 110, 0, BlockType.INFESTED_STONE, 2);
            world.setBlock(0, 90, 0, BlockType.INFESTED_STONE, 1);
            world.setBlock(5, 100, -5, BlockType.INFESTED_STONE, 0);
            world.setBlock(0, 111, 0, BlockType.INFESTED_STONE, 2);

            assertTrue(silverfish.damage(1.0f, com.craftzero.combat.DamageSource.generic()));
            world.updateEntities(1.0f / 20.0f);

            assertSame(BlockType.AIR, world.getBlock(0, 110, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 90, 0));
            assertSame(BlockType.AIR, world.getBlock(5, 100, -5));
            assertSame(BlockType.INFESTED_STONE, world.getBlock(0, 111, 0),
                    "Eggs outside the old +/-10 vertical wake range should stay hidden");
            assertEquals(4L, world.getEntities().stream()
                    .filter(Silverfish.class::isInstance)
                    .count());
            assertTrue(world.getDroppedItems().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    private static int textureIndex(BlockType type, int metadata) {
        float[] coords = type.getTextureCoords(Block.FACE_NORTH, metadata);
        int col = Math.round((coords[0] - 0.001f) / BlockType.TEXTURE_SIZE);
        int row = Math.round((coords[1] - 0.001f) / BlockType.TEXTURE_SIZE);
        return row * BlockType.ATLAS_SIZE + col;
    }
}
