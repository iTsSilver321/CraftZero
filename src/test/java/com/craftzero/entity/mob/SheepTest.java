package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.ai.EatGrassGoal;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SheepTest {
    @Test
    @DisplayName("Release 1.0 sheep drops preserve wool color")
    void sheepDeathDropsStoredWoolColor() {
        World world = new World(6275L);
        try {
            Sheep sheep = new Sheep();
            sheep.setWoolColor(14);
            sheep.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(sheep));

            sheep.dropLoot();

            assertEquals(1, droppedCount(world, ItemType.RED_WOOL));
            assertEquals(0, droppedCount(world, ItemType.WHITE_WOOL));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 shearing preserves sheep wool color")
    void shearingDropsStoredWoolColor() {
        World world = new World(6276L);
        try {
            Sheep sheep = new Sheep();
            sheep.random = fixedNextInt(2);
            sheep.setWoolColor(11);
            sheep.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(sheep));

            assertTrue(sheep.shear());

            assertEquals(3, droppedCount(world, ItemType.BLUE_WOOL));
            assertEquals(0, droppedCount(world, ItemType.WHITE_WOOL));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 shearing drops should ignore recent Looting credit")
    void shearingDropsIgnoreRecentLootingCredit() {
        World world = new World(6277L);
        try {
            Sheep sheep = new Sheep();
            sheep.random = fixedNextInt(5);
            sheep.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(sheep));

            assertTrue(sheep.damage(1.0f, DamageSource.playerAttack(0.0f, 70.0f, 0.0f, 3)));
            assertTrue(sheep.shear());

            assertEquals(3, droppedCount(world, ItemType.WHITE_WOOL));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 sheep expose vanilla fleece tint colors")
    void sheepFleeceColorUsesStoredWoolColor() {
        Sheep sheep = new Sheep();
        sheep.setWoolColor(14);

        float[] color = sheep.getFleeceColor();

        assertEquals(0.8f, color[0], 0.0001f);
        assertEquals(0.3f, color[1], 0.0001f);
        assertEquals(0.3f, color[2], 0.0001f);
    }

    @Test
    @DisplayName("Release 1.0 sheep eat grass blocks and regrow wool")
    void sheepEatGrassBlocksAndRegrowWool() {
        World world = new World(6278L);
        try {
            Sheep sheep = new Sheep();
            sheep.random = fixedNextInt(0);
            sheep.setSheared(true);
            sheep.setPosition(0.5f, 100.0f, 0.5f);
            world.setBlock(0, 99, 0, BlockType.GRASS, 0);
            world.replaceEntities(List.of(sheep));

            tickSheep(sheep, EatGrassGoal.EATING_TICKS);

            assertEquals(BlockType.DIRT, world.getBlock(0, 99, 0));
            assertFalse(sheep.isSheared());
            assertEquals(0, sheep.getEatingGrassTimer());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 sheep eat tall grass before grass blocks")
    void sheepEatTallGrassBeforeGrassBlocks() {
        World world = new World(6279L);
        try {
            Sheep sheep = new Sheep();
            sheep.random = fixedNextInt(0);
            sheep.setSheared(true);
            sheep.setPosition(0.5f, 100.0f, 0.5f);
            world.setBlock(0, 99, 0, BlockType.GRASS, 0);
            world.setBlock(0, 100, 0, BlockType.TALL_GRASS, 1);
            world.replaceEntities(List.of(sheep));

            tickSheep(sheep, EatGrassGoal.EATING_TICKS);

            assertEquals(BlockType.AIR, world.getBlock(0, 100, 0));
            assertEquals(BlockType.GRASS, world.getBlock(0, 99, 0));
            assertFalse(sheep.isSheared());
            assertEquals(0, sheep.getEatingGrassTimer());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 baby sheep grow faster after eating grass")
    void babySheepGrowFasterAfterEatingGrass() {
        World world = new World(6280L);
        try {
            Sheep sheep = new Sheep();
            sheep.random = fixedNextInt(0);
            sheep.setGrowingAge(Mob.BABY_GROWING_AGE);
            sheep.setPosition(0.5f, 100.0f, 0.5f);
            world.setBlock(0, 99, 0, BlockType.GRASS, 0);
            world.replaceEntities(List.of(sheep));

            tickSheep(sheep, EatGrassGoal.EATING_TICKS);

            assertEquals(BlockType.DIRT, world.getBlock(0, 99, 0));
            assertTrue(sheep.getGrowingAge() > Mob.BABY_GROWING_AGE + EatGrassGoal.EATING_TICKS);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 sheep expose grass-eating head animation")
    void sheepExposeGrassEatingHeadAnimation() {
        Sheep sheep = new Sheep();

        sheep.setEatingGrassTimer(40);
        assertEquals(0.0f, sheep.getGrassEatingHeadOffsetScale(0.0f), 0.0001f);
        assertTrue(sheep.getGrassEatingHeadOffsetScale(1.0f) > 0.0f);

        sheep.setEatingGrassTimer(20);
        assertEquals(1.0f, sheep.getGrassEatingHeadOffsetScale(0.0f), 0.0001f);
        assertTrue(sheep.getGrassEatingHeadPitch(0.0f) > 0.4f);

        sheep.setEatingGrassTimer(0);
        assertEquals(0.0f, sheep.getGrassEatingHeadOffsetScale(0.0f), 0.0001f);
        assertEquals(0.0f, sheep.getGrassEatingHeadPitch(0.0f), 0.0001f);
    }

    @Test
    @DisplayName("Release 1.0 dyes map to inverse wool metadata colors")
    void dyeItemsMapToReleaseWoolColors() {
        Map<ItemType, Integer> expectedColors = Map.ofEntries(
                Map.entry(ItemType.INK_SAC, 15),
                Map.entry(ItemType.ROSE_RED, 14),
                Map.entry(ItemType.CACTUS_GREEN, 13),
                Map.entry(ItemType.COCOA_BEANS, 12),
                Map.entry(ItemType.LAPIS_LAZULI, 11),
                Map.entry(ItemType.PURPLE_DYE, 10),
                Map.entry(ItemType.CYAN_DYE, 9),
                Map.entry(ItemType.LIGHT_GRAY_DYE, 8),
                Map.entry(ItemType.GRAY_DYE, 7),
                Map.entry(ItemType.PINK_DYE, 6),
                Map.entry(ItemType.LIME_DYE, 5),
                Map.entry(ItemType.DANDELION_YELLOW, 4),
                Map.entry(ItemType.LIGHT_BLUE_DYE, 3),
                Map.entry(ItemType.MAGENTA_DYE, 2),
                Map.entry(ItemType.ORANGE_DYE, 1),
                Map.entry(ItemType.BONE_MEAL, 0));

        expectedColors.forEach((dye, color) -> assertEquals(color, Sheep.woolColorForDye(dye), dye.name()));
        assertEquals(-1, Sheep.woolColorForDye(ItemType.WHEAT));
    }

    private static int droppedCount(World world, ItemType type) {
        return world.getDroppedItems().stream()
                .filter(item -> item.getItemType() == type)
                .mapToInt(item -> item.getCount())
                .sum();
    }

    private static void tickSheep(Sheep sheep, int ticks) {
        for (int i = 0; i < ticks; i++) {
            sheep.tick();
        }
    }

    private static Random fixedNextInt(int value) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return value % bound;
            }
        };
    }
}
