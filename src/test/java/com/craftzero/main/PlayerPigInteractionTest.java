package com.craftzero.main;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.mob.Pig;
import com.craftzero.entity.mob.ZombiePigman;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.AchievementType;
import com.craftzero.world.BiomeType;
import com.craftzero.world.BlockType;
import com.craftzero.world.Chunk;
import com.craftzero.world.Dimension;
import com.craftzero.world.World;
import com.craftzero.world.WorldGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class PlayerPigInteractionTest {
    @Test
    @DisplayName("Player can saddle and mount a Release 1.0 pig")
    void playerSaddlesAndMountsPig() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        Pig pig = new Pig();
        ItemStack saddle = new ItemStack(ItemType.SADDLE, 1);
        player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = saddle;

        assertTrue(player.saddlePig(pig, saddle));
        assertTrue(pig.isSaddled());
        assertNull(player.getInventory().getItemInHand());
        assertTrue(player.isUsingItem());

        assertTrue(player.mountPig(pig));
        assertTrue(player.isRidingPig());
        assertSame(pig, player.getRidingPig());
        assertTrue(pig.hasPlayerPassenger());

        player.dismountPig();

        assertFalse(player.isRidingPig());
        assertFalse(pig.hasPlayerPassenger());
    }

    @Test
    @DisplayName("Unsaddled pigs should reject riding")
    void unsaddledPigsRejectRiding() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        Pig pig = new Pig();

        assertFalse(player.mountPig(pig));
    }

    @Test
    @DisplayName("Release 1.0 baby pigs can be saddled and mounted")
    void babyPigsCanBeSaddledAndMounted() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        Pig pig = new Pig();
        pig.setGrowingAge(Pig.BABY_GROWING_AGE);
        ItemStack saddle = new ItemStack(ItemType.SADDLE, 1);

        assertTrue(player.saddlePig(pig, saddle));
        assertTrue(pig.isSaddled());
        assertEquals(0, saddle.getCount());
        assertTrue(player.isUsingItem());

        assertTrue(player.mountPig(pig));
        assertTrue(player.isRidingPig());
        assertSame(pig, player.getRidingPig());
        assertTrue(pig.hasPlayerPassenger());
    }

    @Test
    @DisplayName("Removed mounted pig clears player riding state")
    void removedPigClearsRidingState() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        Pig pig = new Pig();
        pig.setSaddled(true);

        assertTrue(player.mountPig(pig));
        pig.remove();
        player.syncRidingPosition();

        assertFalse(player.isRidingPig());
    }

    @Test
    @DisplayName("Lightning transforming a ridden pig should dismount the player")
    void lightningTransformingRiddenPigDismountsPlayer() {
        World world = new World(6275L, WorldGenerator.RELEASE_ONE, Dimension.OVERWORLD);
        try {
            int[] pos = findRainBiome(world);
            prepareOpenArea(world, pos[0], pos[1], 2);
            Player player = new Player(pos[0] + 0.5f, 100.0f, pos[1] + 0.5f);
            player.setWorld(world);
            world.setPlayer(player);
            Pig pig = new Pig();
            pig.setSaddled(true);
            pig.setPosition(pos[0] + 0.5f, 100.0f, pos[1] + 0.5f);
            world.replaceEntities(List.of(pig));
            world.setWeatherState("thunder");

            assertTrue(player.mountPig(pig));
            assertTrue(world.strikeLightningAt(pos[0], 100, pos[1]));

            assertFalse(player.isRidingPig());
            assertFalse(pig.hasPlayerPassenger());
            assertTrue(pig.isRemoved());
            assertFalse(world.getEntities().stream().anyMatch(Pig.class::isInstance));
            assertTrue(world.getEntities().stream().anyMatch(ZombiePigman.class::isInstance));
            assertTrue(Math.abs(player.getPosition().x - pig.getX()) >= 0.5f
                    || Math.abs(player.getPosition().z - pig.getZ()) >= 0.5f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 When Pigs Fly should unlock when a ridden pig takes fall damage")
    void whenPigsFlyUnlocksWhenRiddenPigTakesFallDamage() {
        World world = new World(6274L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            unlockCowTipper(player);
            Pig pig = new Pig();
            pig.setPosition(1.0f, 70.0f, 0.0f);
            pig.setSaddled(true);
            world.replaceEntities(List.of(pig));

            assertTrue(player.mountPig(pig));
            assertTrue(pig.damage(4.0f, DamageSource.point(DamageSource.Type.FALL,
                    pig.getX(), pig.getY(), pig.getZ(), 0.0f, 0.0f)));

            assertTrue(player.getStats().getAchievements().isUnlocked(AchievementType.FLY_PIG));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 saddled pigs should not return saddles on death")
    void saddledPigDoesNotDropSaddle() {
        World world = new World(6270L);
        try {
            Pig pig = new Pig();
            pig.setPosition(1.0f, 70.0f, 0.0f);
            pig.setSaddled(true);
            world.replaceEntities(List.of(pig));

            pig.dropLoot();

            assertEquals(0, droppedCount(world, ItemType.SADDLE));
            int pork = droppedCount(world, ItemType.RAW_PORKCHOP);
            assertTrue(pork >= 0 && pork <= 2);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 pigs should drop the old zero-to-two porkchop range")
    void pigPorkchopDropRangeIsZeroToTwo() {
        World minWorld = new World(6272L);
        try {
            TestPig pig = new TestPig();
            pig.useRandom(new FixedRandom(0));
            pig.setPosition(1.0f, 70.0f, 0.0f);
            minWorld.replaceEntities(List.of(pig));

            pig.dropLoot();

            assertEquals(0, droppedCount(minWorld, ItemType.RAW_PORKCHOP));
        } finally {
            minWorld.cleanup();
        }

        World maxWorld = new World(6273L);
        try {
            TestPig pig = new TestPig();
            pig.useRandom(new FixedRandom(2));
            pig.setPosition(1.0f, 70.0f, 0.0f);
            maxWorld.replaceEntities(List.of(pig));

            pig.dropLoot();

            assertEquals(2, droppedCount(maxWorld, ItemType.RAW_PORKCHOP));
        } finally {
            maxWorld.cleanup();
        }
    }

    private static int droppedCount(World world, ItemType type) {
        return world.getDroppedItems().stream()
                .filter(item -> item.getItemType() == type)
                .mapToInt(item -> item.getCount())
                .sum();
    }

    private static int[] findRainBiome(World world) {
        for (int x = -256; x <= 256; x += 8) {
            for (int z = -256; z <= 256; z += 8) {
                BiomeType biome = world.getReleaseBiome(x, z);
                if (biome.hasPrecipitation() && !biome.canFreezeWater() && biome.getTemperature() < 1.0f) {
                    return new int[] { x, z };
                }
            }
        }
        throw new AssertionError("No non-frozen rain biome found near spawn search area");
    }

    private static void prepareOpenArea(World world, int centerX, int centerZ, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                prepareOpenColumn(world, centerX + dx, centerZ + dz);
            }
        }
    }

    private static void prepareOpenColumn(World world, int x, int z) {
        world.getChunkNow(Math.floorDiv(x, Chunk.WIDTH), Math.floorDiv(z, Chunk.DEPTH));
        world.setBlock(x, 99, z, BlockType.STONE, 0);
        for (int y = 100; y < 128; y++) {
            world.setBlock(x, y, z, BlockType.AIR, 0);
        }
    }

    private static void unlockCowTipper(Player player) {
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.OPEN_INVENTORY));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.MINE_WOOD));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.BUILD_WORKBENCH));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.BUILD_SWORD));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.KILL_COW));
    }

    private static final class TestPig extends Pig {
        private void useRandom(Random random) {
            this.random = random;
        }
    }

    private static final class FixedRandom extends Random {
        private final int intValue;

        private FixedRandom(int intValue) {
            this.intValue = intValue;
        }

        @Override
        public int nextInt(int bound) {
            return Math.min(intValue, bound - 1);
        }

        @Override
        public float nextFloat() {
            return 0.5f;
        }
    }
}
