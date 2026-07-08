package com.craftzero.main;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.world.BlockType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerBlockBreakMathTest {
    private static final float TICK = 1.0f / 20.0f;
    private static final float EPSILON = 0.000001f;

    @Test
    @DisplayName("Common blocks should expose Release 1.0 mining hardness")
    void commonBlocksExposeReleaseOneMiningHardness() {
        assertBreakHardness(BlockType.STONE, 1.5f);
        assertBreakHardness(BlockType.GRASS, 0.6f);
        assertBreakHardness(BlockType.DIRT, 0.5f);
        assertBreakHardness(BlockType.COBBLESTONE, 2.0f);
        assertBreakHardness(BlockType.OAK_PLANKS, 2.0f);
        assertBreakHardness(BlockType.OAK_LOG, 2.0f);
        assertBreakHardness(BlockType.SAND, 0.5f);
        assertBreakHardness(BlockType.GRAVEL, 0.6f);
        assertBreakHardness(BlockType.IRON_ORE, 3.0f);
        assertBreakHardness(BlockType.COAL_ORE, 3.0f);
        assertBreakHardness(BlockType.DIAMOND_ORE, 3.0f);
        assertBreakHardness(BlockType.REDSTONE_ORE, 3.0f);
        assertBreakHardness(BlockType.GLASS, 0.3f);
        assertBreakHardness(BlockType.SANDSTONE, 0.8f);
        assertBreakHardness(BlockType.CHEST, 2.5f);
        assertBreakHardness(BlockType.BOOKSHELF, 1.5f);
        assertBreakHardness(BlockType.STONE_BRICK, 1.5f);
        assertBreakHardness(BlockType.NETHER_BRICK, 2.0f);
    }

    @Test
    @DisplayName("Common blocks should expose Release 1.0 explosion resistance")
    void commonBlocksExposeReleaseOneExplosionResistance() {
        assertExplosionResistance(BlockType.AIR, 0.0f);
        assertExplosionResistance(BlockType.WATER, 500.0f);
        assertExplosionResistance(BlockType.OBSIDIAN, 6000.0f);
        assertExplosionResistance(BlockType.END_PORTAL, 3600000.0f);
        assertExplosionResistance(BlockType.END_PORTAL_FRAME, 3600000.0f);
        assertExplosionResistance(BlockType.STONE, 6.0f);
        assertExplosionResistance(BlockType.COBBLESTONE, 6.0f);
        assertExplosionResistance(BlockType.STONE_BRICK, 6.0f);
        assertExplosionResistance(BlockType.NETHER_BRICK, 6.0f);
        assertExplosionResistance(BlockType.OAK_PLANKS, 3.0f);
        assertExplosionResistance(BlockType.OAK_LOG, 3.0f);
        assertExplosionResistance(BlockType.BOOKSHELF, 3.0f);
        assertExplosionResistance(BlockType.GRASS, 0.6f);
        assertExplosionResistance(BlockType.DIRT, 0.5f);
        assertExplosionResistance(BlockType.SAND, 0.5f);
        assertExplosionResistance(BlockType.GRAVEL, 0.6f);
        assertExplosionResistance(BlockType.COAL_ORE, 3.0f);
        assertExplosionResistance(BlockType.GLASS, 0.3f);
        assertExplosionResistance(BlockType.CHEST, 2.5f);
    }

    @Test
    @DisplayName("Special Release 1.0 cutting tools should use their old block speeds")
    void specialCuttingToolsUseReleaseOneBlockSpeeds() {
        assertEquals(15.0f,
                Player.computeBlockBreakSpeedMultiplier(BlockType.COBWEB,
                        new ItemStack(ItemType.WOODEN_SWORD, 1)),
                EPSILON);
        assertEquals(15.0f,
                Player.computeBlockBreakSpeedMultiplier(BlockType.COBWEB,
                        new ItemStack(ItemType.SHEARS, 1)),
                EPSILON);
        assertEquals(15.0f,
                Player.computeBlockBreakSpeedMultiplier(BlockType.LEAVES,
                        new ItemStack(ItemType.SHEARS, 1)),
                EPSILON);
        assertEquals(5.0f,
                Player.computeBlockBreakSpeedMultiplier(BlockType.WHITE_WOOL,
                        new ItemStack(ItemType.SHEARS, 1)),
                EPSILON);
        assertEquals(1.0f,
                Player.computeBlockBreakSpeedMultiplier(BlockType.STONE,
                        new ItemStack(ItemType.SHEARS, 1)),
                EPSILON);
    }

    @Test
    @DisplayName("Survival block breaking should use Release 1.0 block strength math")
    void survivalBlockBreakingUsesReleaseOneBlockStrengthMath() {
        assertEquals(1.0f / 5.0f / 100.0f,
                Player.computeSurvivalBlockBreakProgressIncrement(TICK, 5.0f, 1.0f, false, false, true, false),
                EPSILON);

        assertEquals(2.0f / 5.0f / 30.0f,
                Player.computeSurvivalBlockBreakProgressIncrement(TICK, 5.0f, 2.0f, true, false, true, false),
                EPSILON);

        assertEquals(2.0f / 5.0f / 30.0f / 25.0f,
                Player.computeSurvivalBlockBreakProgressIncrement(TICK, 5.0f, 2.0f, true, true, false, false),
                EPSILON);

        assertEquals(2.0f / 5.0f / 30.0f,
                Player.computeSurvivalBlockBreakProgressIncrement(TICK, 5.0f, 2.0f, true, true, true, false, true),
                EPSILON);

        assertEquals(2.0f / 5.0f / 30.0f / 5.0f,
                Player.computeSurvivalBlockBreakProgressIncrement(TICK, 5.0f, 2.0f, true, true, false, false, true),
                EPSILON);

        assertEquals(2.0f / 5.0f / 30.0f,
                Player.computeSurvivalBlockBreakProgressIncrement(TICK, 5.0f, 2.0f, true, true, false, true),
                EPSILON);
    }

    @Test
    @DisplayName("Airborne and head-underwater mining penalties should stack like Release 1.0")
    void airborneAndHeadUnderwaterMiningPenaltiesStack() {
        float normal = Player.computeSurvivalBlockBreakProgressIncrement(
                TICK, 5.0f, 2.0f, true, false, true, false);

        assertEquals(normal / 25.0f,
                Player.computeSurvivalBlockBreakProgressIncrement(TICK, 5.0f, 2.0f, true,
                        true, false, false),
                EPSILON);

        assertEquals(normal / 5.0f,
                Player.computeSurvivalBlockBreakProgressIncrement(TICK, 5.0f, 2.0f, true,
                        true, false, false, true),
                EPSILON);
    }

    @Test
    @DisplayName("Haste and Mining Fatigue should modify Release 1.0 mining speed")
    void hasteAndMiningFatigueModifyMiningSpeed() {
        PlayerStats stats = new PlayerStats();
        stats.addEffect(new StatusEffectInstance(StatusEffectType.HASTE, 200, 1));
        assertEquals(1.4f, stats.getMiningSpeedMultiplier(), EPSILON);

        stats.clearEffects();
        stats.addEffect(new StatusEffectInstance(StatusEffectType.MINING_FATIGUE, 200, 0));
        assertEquals(0.8f, stats.getMiningSpeedMultiplier(), EPSILON);

        stats.addEffect(new StatusEffectInstance(StatusEffectType.HASTE, 200, 0));
        assertEquals(0.96f, stats.getMiningSpeedMultiplier(), EPSILON);

        stats.clearEffects();
        stats.addEffect(new StatusEffectInstance(StatusEffectType.MINING_FATIGUE, 200, 4));
        assertEquals(0.0f, stats.getMiningSpeedMultiplier(), EPSILON);
    }

    @Test
    @DisplayName("Zero-hardness survival blocks should still break instantly")
    void zeroHardnessBlocksBreakInstantly() {
        assertEquals(1.0f,
                Player.computeSurvivalBlockBreakProgressIncrement(TICK, 0.0f, 1.0f, true, false, true, false),
                EPSILON);
    }

    private static void assertBreakHardness(BlockType blockType, float expected) {
        assertEquals(expected, blockType.getBreakHardness(), EPSILON, blockType.name());
    }

    private static void assertExplosionResistance(BlockType blockType, float expected) {
        assertEquals(expected, blockType.getExplosionResistance(), EPSILON, blockType.name());
    }
}
