package com.craftzero.entity;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishingHookEntityTest {
    @Test
    @DisplayName("Fishing hooks become catchable after the Release-era bite roll")
    void fishingHookBecomesCatchableInWater() {
        World world = new World(6238L);
        try {
            for (int y = 100; y <= 104; y++) {
                world.setBlock(0, y, 0, BlockType.WATER, 0);
            }
            Player player = new Player(0.5f, 100.0f, 0.5f);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                    new ItemStack(ItemType.FISHING_ROD, 1);
            world.setPlayer(player);
            FishingHookEntity hook = new FishingHookEntity(0.5f, 100.2f, 0.5f,
                    0.0f, 0.0f, 0.0f, player, fixedNextInt(0));
            world.spawnEntity(hook);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(hook.isCatchable(), "removed=" + hook.isRemoved()
                    + " wait=" + hook.getWaitTicks()
                    + " catchable=" + hook.getCatchableTicks()
                    + " y=" + hook.getY()
                    + " inWater=" + hook.isInWater());
            assertEquals(6, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.BUBBLE)
                    .count());
            assertEquals(6, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SPLASH)
                    .count());
            WorldParticle firstBubble = world.getParticles().get(0);
            assertSame(WorldParticle.Type.BUBBLE, firstBubble.getType());
            assertTrue(firstBubble.getRenderX(0.0f) >= 0.25f && firstBubble.getRenderX(0.0f) <= 0.75f);
            assertEquals(101.0f, firstBubble.getRenderY(0.0f), 0.0001f);
            assertTrue(firstBubble.getRenderZ(0.0f) >= 0.25f && firstBubble.getRenderZ(0.0f) <= 0.75f);
            WorldSoundEvent splash = world.getSoundEvents().stream()
                    .filter(sound -> sound.soundId().equals(WorldSoundEvent.FISHING_SPLASH))
                    .findFirst()
                    .orElseThrow();
            assertEquals(0.25f, splash.volume(), 0.0001f);
            assertTrue(splash.pitch() >= 0.6f && splash.pitch() <= 1.4f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Fishing hooks use Release-era bite chance and catchable RNG bounds")
    void fishingHookUsesReleaseBiteAndCatchableBounds() {
        World world = new World(6241L);
        try {
            fillFishingWater(world);
            Player player = fishingPlayer(world, 0.5f, 100.0f, 0.5f);
            FishingHookEntity hook = new FishingHookEntity(0.5f, 100.2f, 0.5f,
                    0.0f, 0.0f, 0.0f, player, sequenceNextInt(0, 29));
            world.spawnEntity(hook);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(hook.isCatchable());
            assertEquals(39, hook.getCatchableTicks());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("World-spawned fishing hooks should use the world RNG")
    void worldSpawnedFishingHookUsesWorldRandom() {
        CountingRandom random = new CountingRandom(new int[] { 0, 29 }, new float[] { 0.75f, 0.25f });
        World world = new RandomOverrideWorld(6243L, random);
        try {
            fillFishingWater(world);
            Player player = fishingPlayer(world, 0.5f, 100.0f, 0.5f);
            FishingHookEntity hook = new FishingHookEntity(0.5f, 100.2f, 0.5f,
                    0.0f, 0.0f, 0.0f, player);
            world.spawnEntity(hook);

            assertEquals(0, hook.getWaitTicks());
            assertEquals(0, random.nextIntCalls());

            world.updateEntities(1.0f / 20.0f);

            assertTrue(hook.isCatchable());
            assertEquals(39, hook.getCatchableTicks());
            assertEquals(2, random.nextIntCalls());
            assertEquals(500, random.boundAt(0));
            assertEquals(30, random.boundAt(1));
            assertEquals(32, random.nextFloatCalls());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Rain-exposed fishing hooks should use the old faster bite roll")
    void rainExposedFishingHookUsesRainBiteChance() {
        CountingRandom random = new CountingRandom(new int[] { 1 }, new float[0]);
        World world = new RainOverrideWorld(6245L, random, true);
        try {
            fillFishingWater(world);
            Player player = fishingPlayer(world, 0.5f, 100.0f, 0.5f);
            FishingHookEntity hook = new FishingHookEntity(0.5f, 100.2f, 0.5f,
                    0.0f, 0.0f, 0.0f, player);
            world.spawnEntity(hook);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(!hook.isCatchable());
            assertEquals(300, random.boundAt(0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Missed catch windows should return to the per-tick bite roll")
    void missedCatchWindowResetsWait() {
        World world = new World(6242L);
        try {
            fillFishingWater(world);
            Player player = fishingPlayer(world, 0.5f, 100.0f, 0.5f);
            FishingHookEntity hook = new FishingHookEntity(0.5f, 100.2f, 0.5f,
                    0.0f, 0.0f, 0.0f, player, fixedNextInt(0));
            hook.setCatchableTicks(1);
            world.spawnEntity(hook);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(!hook.isCatchable());
            assertEquals(0, hook.getWaitTicks());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Fishing hooks latch to entities and pull them toward the owner when reeled")
    void fishingHookPullsHookedEntityOnReel() {
        World world = new World(6239L);
        try {
            Player player = new Player(0.0f, 100.0f, 0.0f);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                    new ItemStack(ItemType.FISHING_ROD, 1);
            world.setPlayer(player);
            Zombie zombie = new Zombie();
            zombie.setPosition(3.0f, 100.0f, 0.0f);
            FishingHookEntity hook = new FishingHookEntity(3.0f, 100.8f, 0.0f,
                    0.0f, 0.0f, 0.0f, player, fixedNextInt(0));
            world.spawnEntity(zombie);
            world.spawnEntity(hook);

            world.updateEntities(1.0f / 20.0f);

            assertSame(zombie, hook.getHookedEntity());
            assertEquals(3, hook.reelIn());
            assertTrue(zombie.getMotionX() < 0.0f);
            assertTrue(zombie.getMotionY() > 0.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Caught fish should fly toward the owner with the Release-era reel vector")
    void fishingHookReelsCaughtFishTowardOwner() {
        World world = new World(6244L);
        try {
            Player player = fishingPlayer(world, 0.0f, 100.0f, 0.0f);
            FishingHookEntity hook = new FishingHookEntity(3.0f, 100.8f, 4.0f,
                    0.0f, 0.0f, 0.0f, player, fixedNextInt(0));
            hook.setCatchableTicks(20);
            world.spawnEntity(hook);

            assertEquals(1, hook.reelIn());

            assertEquals(1, world.getDroppedItems().size());
            DroppedItem fish = world.getDroppedItems().get(0);
            assertEquals(ItemType.RAW_FISH, fish.getItemType());
            assertEquals(1, fish.getCount());
            assertEquals(3.0f, fish.getX(), 0.0001f);
            assertEquals(100.95f, fish.getY(), 0.0001f);
            assertEquals(4.0f, fish.getZ(), 0.0001f);

            float dx = player.getPosition().x - hook.getX();
            float dy = player.getPosition().y - hook.getY();
            float dz = player.getPosition().z - hook.getZ();
            float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            assertEquals(dx * 0.1f, fish.getVelocityX(), 0.0001f);
            assertEquals(dy * 0.1f + (float) Math.sqrt(distance) * 0.08f,
                    fish.getVelocityY(), 0.0001f);
            assertEquals(dz * 0.1f, fish.getVelocityZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    private static Random fixedNextInt(int value) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                if (value < 0 || value >= bound) {
                    throw new IllegalArgumentException("Fixed random value " + value + " outside bound " + bound);
                }
                return value;
            }
        };
    }

    private static Random sequenceNextInt(int... values) {
        return new Random() {
            private int index;

            @Override
            public int nextInt(int bound) {
                if (index >= values.length) {
                    throw new IllegalStateException("No fixed random value left for bound " + bound);
                }
                int value = values[index++];
                if (value < 0 || value >= bound) {
                    throw new IllegalArgumentException("Fixed random value " + value + " outside bound " + bound);
                }
                return value;
            }
        };
    }

    private static final class CountingRandom extends Random {
        private final int[] ints;
        private final float[] floats;
        private final java.util.List<Integer> bounds = new java.util.ArrayList<>();
        private int intIndex;
        private int floatIndex;

        private CountingRandom(int[] ints, float[] floats) {
            this.ints = ints;
            this.floats = floats;
        }

        @Override
        public int nextInt(int bound) {
            bounds.add(bound);
            int value = ints[Math.min(intIndex, ints.length - 1)];
            intIndex++;
            if (value < 0 || value >= bound) {
                throw new IllegalArgumentException("Fixed random value " + value + " outside bound " + bound);
            }
            return value;
        }

        @Override
        public float nextFloat() {
            float value = floats[Math.min(floatIndex, floats.length - 1)];
            floatIndex++;
            return value;
        }

        private int nextIntCalls() {
            return intIndex;
        }

        private int boundAt(int index) {
            return bounds.get(index);
        }

        private int nextFloatCalls() {
            return floatIndex;
        }
    }

    private static final class RandomOverrideWorld extends World {
        private final Random random;

        private RandomOverrideWorld(long seed, Random random) {
            super(seed);
            this.random = random;
        }

        @Override
        public Random getRandom() {
            return random;
        }
    }

    private static final class RainOverrideWorld extends World {
        private final Random random;
        private final boolean rainingAtHook;

        private RainOverrideWorld(long seed, Random random, boolean rainingAtHook) {
            super(seed);
            this.random = random;
            this.rainingAtHook = rainingAtHook;
        }

        @Override
        public Random getRandom() {
            return random;
        }

        @Override
        public boolean isRainingAt(int x, int y, int z) {
            return rainingAtHook;
        }
    }

    private static void fillFishingWater(World world) {
        for (int y = 100; y <= 104; y++) {
            world.setBlock(0, y, 0, BlockType.WATER, 0);
        }
    }

    private static Player fishingPlayer(World world, float x, float y, float z) {
        Player player = new Player(x, y, z);
        player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                new ItemStack(ItemType.FISHING_ROD, 1);
        world.setPlayer(player);
        return player;
    }
}
