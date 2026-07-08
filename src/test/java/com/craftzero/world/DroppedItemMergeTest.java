package com.craftzero.world;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.DroppedItem;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DroppedItemMergeTest {
    @Test
    @DisplayName("Dropped item visual randomness should be source-controllable")
    void droppedItemVisualRandomnessIsControllable() {
        DroppedItem item = new DroppedItem(0.0f, 70.0f, 0.0f, ItemType.DIRT, 1,
                new SequenceRandom(0.25f, 0.5f));

        assertEquals(-1.0f, item.getVelocityX(), 0.001f);
        assertEquals(4.0f, item.getVelocityY(), 0.001f);
        assertEquals(0.0f, item.getVelocityZ(), 0.001f);
        assertEquals(90.0f, item.getRotation(), 0.001f);
        assertEquals((float) Math.PI, item.getBobPhase(), 0.001f);
    }

    @Test
    @DisplayName("World-spawned dropped items should use the world RNG")
    void worldSpawnedDroppedItemsUseWorldRandom() {
        long seed = 6403L;
        World first = new World(seed);
        World second = new World(seed);
        try {
            first.spawnDroppedItem(0.0f, 70.0f, 0.0f, ItemType.DIRT, 1);
            second.spawnDroppedItem(0.0f, 70.0f, 0.0f, ItemType.DIRT, 1);

            DroppedItem firstItem = first.getDroppedItems().get(0);
            DroppedItem secondItem = second.getDroppedItems().get(0);

            assertEquals(firstItem.getRotation(), secondItem.getRotation(), 0.001f);
            assertEquals(firstItem.getBobPhase(), secondItem.getBobPhase(), 0.001f);
        } finally {
            first.cleanup();
            second.cleanup();
        }
    }

    @Test
    @DisplayName("World-attached dropped items should initialize deferred visuals from the world RNG")
    void worldAttachedDroppedItemsUseWorldRandom() {
        SequenceRandom random = new SequenceRandom(0.25f, 0.5f);
        World world = new RandomOverrideWorld(6404L, random);
        try {
            DroppedItem item = new DroppedItem(0.0f, 70.0f, 0.0f, ItemType.DIRT, 1);
            world.replaceDroppedItems(List.of(item));

            assertEquals(90.0f, item.getRotation(), 0.001f);
            assertEquals((float) Math.PI, item.getBobPhase(), 0.001f);
            assertEquals(-1.0f, item.getVelocityX(), 0.001f);
            assertEquals(4.0f, item.getVelocityY(), 0.001f);
            assertEquals(0.0f, item.getVelocityZ(), 0.001f);
            assertEquals(4, random.nextFloatCalls());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Restored dropped item animation should not be rerolled on world attach")
    void restoredDroppedItemAnimationIsNotRerolledOnAttach() {
        SequenceRandom random = new SequenceRandom(0.25f, 0.5f);
        World world = new RandomOverrideWorld(6405L, random);
        try {
            DroppedItem item = new DroppedItem(0.0f, 70.0f, 0.0f, ItemType.DIRT, 1,
                    0.0f, 0.0f, 0.0f);
            item.setAnimationState(33.0f, 1.25f);
            world.replaceDroppedItems(List.of(item));

            assertEquals(33.0f, item.getRotation(), 0.001f);
            assertEquals(1.25f, item.getBobPhase(), 0.001f);
            assertEquals(0, random.nextFloatCalls());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Oversized dropped item spawns split into valid item-entity stacks")
    void oversizedDroppedItemSpawnsSplitIntoValidStacks() {
        World world = new World(6400L);
        try {
            world.spawnDroppedItem(0.0f, 70.0f, 0.0f, ItemType.DIRT, 70);

            List<Integer> counts = sortedCounts(world);
            assertEquals(List.of(6, 64), counts);
            assertTrue(world.getDroppedItems().stream()
                    .allMatch(item -> item.getCount() <= item.getMaxStackSize()));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Nearby dropped items partially top off matching item-entity stacks")
    void nearbyDroppedItemsPartiallyTopOffMatchingStacks() {
        World world = new World(6401L);
        try {
            world.replaceDroppedItems(List.of(new DroppedItem(0.0f, 70.0f, 0.0f, ItemType.DIRT, 60)));

            world.spawnDroppedItem(0.5f, 70.0f, 0.0f, ItemType.DIRT, 10);

            assertEquals(List.of(6, 64), sortedCounts(world));
            assertEquals(70, world.getDroppedItems().stream()
                    .mapToInt(DroppedItem::getCount)
                    .sum());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Nearby dropped items should merge during item update cadence")
    void nearbyDroppedItemsMergeDuringUpdateCadence() {
        World world = new World(6412L);
        try {
            DroppedItem first = new DroppedItem(0.0f, 70.0f, 0.0f, ItemType.DIRT, 1,
                    0.0f, 0.0f, 0.0f);
            DroppedItem second = new DroppedItem(0.5f, 70.0f, 0.0f, ItemType.DIRT, 1,
                    0.0f, 0.0f, 0.0f);
            world.replaceDroppedItems(List.of(first, second));

            world.updateDroppedItems(1.0f / 20.0f);

            assertEquals(1, world.getDroppedItems().size());
            assertEquals(2, world.getDroppedItems().get(0).getCount());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dropped item pickup should be gated by remaining pickup-delay ticks")
    void droppedItemPickupDelayTicksGateCollection() {
        World world = new World(6406L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            DroppedItem item = new DroppedItem(0.0f, 70.9f, 0.0f, ItemType.DIRT, 1);
            world.replaceDroppedItems(List.of(item));

            assertEquals(DroppedItem.DEFAULT_PICKUP_DELAY_TICKS, item.getPickupDelayTicks());
            assertFalse(item.canPickup());
            assertTrue(world.collectNearbyItems(item.getX(), item.getY(), item.getZ(), 1.0f / 20.0f, player)
                    .isEmpty());

            world.updateDroppedItems(9.0f / 20.0f);
            assertEquals(1, item.getPickupDelayTicks());
            assertFalse(item.canPickup());

            world.updateDroppedItems(1.0f / 20.0f);
            assertTrue(item.canPickup());
            assertEquals(1, world.collectNearbyItems(item.getX(), item.getY(), item.getZ(), 1.0f / 20.0f, player)
                    .size());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Merged dropped items should keep the freshest pickup delay")
    void mergedDroppedItemsKeepFreshestPickupDelay() {
        World world = new World(6407L);
        try {
            DroppedItem oldItem = new DroppedItem(0.0f, 70.0f, 0.0f, ItemType.DIRT, 1);
            oldItem.setAge(2.0f);
            world.replaceDroppedItems(List.of(oldItem));

            world.spawnDroppedItem(0.0f, 70.0f, 0.0f, ItemType.DIRT, 1);

            assertEquals(1, world.getDroppedItems().size());
            DroppedItem merged = world.getDroppedItems().get(0);
            assertEquals(2, merged.getCount());
            assertEquals(DroppedItem.DEFAULT_PICKUP_DELAY_TICKS, merged.getPickupDelayTicks());
            assertFalse(merged.canPickup());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Thrown dropped stacks should accept the Release player-drop pickup delay")
    void thrownDroppedStacksCanUsePlayerPickupDelay() {
        World world = new World(6408L);
        try {
            world.spawnThrownStack(0.0f, 70.0f, 0.0f, new ItemStack(ItemType.DIRT, 1),
                    0.0f, 0.2f, 0.0f, DroppedItem.THROWN_PICKUP_DELAY_TICKS);

            assertEquals(1, world.getDroppedItems().size());
            assertEquals(DroppedItem.THROWN_PICKUP_DELAY_TICKS,
                    world.getDroppedItems().get(0).getPickupDelayTicks());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dropped item damage should use the source five-health item-entity path")
    void droppedItemDamageUsesSourceHealth() {
        DroppedItem item = new DroppedItem(0.0f, 70.0f, 0.0f, ItemType.DIRT, 1);

        assertEquals(5, item.getHealth());
        assertTrue(item.damage(2.0f, DamageSource.generic()));
        assertEquals(3, item.getHealth());
        assertFalse(item.isDestroyed());

        assertTrue(item.damage(3.0f, DamageSource.generic()));
        assertEquals(0, item.getHealth());
        assertTrue(item.isDestroyed());
        assertFalse(item.damage(1.0f, DamageSource.generic()));
    }

    @Test
    @DisplayName("Explosion damage should destroy dropped item entities")
    void explosionDamageDestroysDroppedItems() {
        World world = new World(6409L);
        try {
            DroppedItem item = new DroppedItem(0.5f, 90.0f, 0.5f, ItemType.DIRT, 1,
                    0.0f, 0.0f, 0.0f);
            world.replaceDroppedItems(List.of(item));

            world.explode(item.getX(), item.getY(), item.getZ(), 4.0f);

            assertTrue(world.getDroppedItems().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dropped items should apply Release-style gravity and vertical drag in air")
    void droppedItemAirMotionUsesReleaseGravityAndDrag() {
        World world = new World(6413L);
        try {
            DroppedItem item = new DroppedItem(0.5f, 120.0f, 0.5f, ItemType.DIRT, 1,
                    0.0f, 0.0f, 0.0f);
            world.replaceDroppedItems(List.of(item));

            world.updateDroppedItems(1.0f / 20.0f);

            assertEquals(119.96f, item.getY(), 0.0001f);
            assertEquals(-0.784f, item.getVelocityY(), 0.0001f);
            assertFalse(item.isOnGround());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dropped items should bounce and retain damped horizontal motion when landing")
    void droppedItemLandingBouncesWithFriction() {
        World world = new World(6410L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            DroppedItem item = new DroppedItem(0.5f, 71.2f, 0.5f, ItemType.DIRT, 1,
                    1.0f, -2.0f, 0.0f);
            world.replaceDroppedItems(List.of(item));

            world.updateDroppedItems(1.0f / 20.0f);

            assertEquals(1, world.getDroppedItems().size());
            assertEquals(71.1f, item.getY(), 0.001f);
            assertFalse(item.isOnGround());
            assertEquals(1.372f, item.getVelocityY(), 0.0001f);
            assertEquals(0.57624f, item.getVelocityX(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Grounded dropped items should keep sliding with friction instead of freezing")
    void groundedDroppedItemSlidesWithFriction() {
        World world = new World(6411L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            DroppedItem item = new DroppedItem(0.5f, 71.1f, 0.5f, ItemType.DIRT, 1,
                    1.0f, 0.0f, 0.0f);
            item.setOnGround(true);
            world.replaceDroppedItems(List.of(item));

            world.updateDroppedItems(1.0f / 20.0f);

            assertEquals(1, world.getDroppedItems().size());
            assertTrue(item.isOnGround());
            assertTrue(item.getX() > 0.5f);
            assertEquals(0.588f, item.getVelocityX(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dropped items should preserve more ground motion on ice")
    void droppedItemGroundFrictionUsesBlockSlipperiness() {
        float stoneMotion = droppedItemMotionAfterGroundTick(BlockType.STONE);
        float iceMotion = droppedItemMotionAfterGroundTick(BlockType.ICE);

        assertEquals(0.588f, stoneMotion, 0.0001f);
        assertEquals(0.9604f, iceMotion, 0.0001f);
        assertTrue(iceMotion > stoneMotion + 0.3f);
    }

    @Test
    @DisplayName("Flowing water should carry dropped item entities along decay gradients")
    void flowingWaterCurrentCarriesDroppedItems() {
        World world = new World(6413L);
        try {
            world.setBlock(0, 90, 0, BlockType.WATER, 0);
            world.setBlock(1, 90, 0, BlockType.FLOWING_WATER, 1);
            DroppedItem item = new DroppedItem(1.5f, 90.0f, 0.5f, ItemType.DIRT, 1,
                    0.0f, 0.0f, 0.0f);
            world.replaceDroppedItems(List.of(item));

            world.updateDroppedItems(1.0f / 20.0f);

            assertTrue(item.getVelocityX() > 0.25f, () -> "velocityX=" + item.getVelocityX());
            assertTrue(item.getX() > 1.51f, () -> "x=" + item.getX());
        } finally {
            world.cleanup();
        }
    }

    private static float droppedItemMotionAfterGroundTick(BlockType ground) {
        World world = new World(6412L + ground.getId());
        try {
            world.setBlock(0, 70, 0, ground, 0);
            DroppedItem item = new DroppedItem(0.5f, 71.1f, 0.5f, ItemType.DIRT, 1,
                    1.0f, 0.0f, 0.0f);
            item.setOnGround(true);
            world.replaceDroppedItems(List.of(item));

            world.updateDroppedItems(1.0f / 20.0f);

            return item.getVelocityX();
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dropped item pickup transfers partial stacks and leaves the remainder")
    void droppedItemPickupTransfersPartialStacksAndLeavesRemainder() {
        World world = new World(6402L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            fillInventory(player);
            player.getInventory().getHotbar()[0] = new ItemStack(ItemType.DIRT, 60);

            DroppedItem item = new DroppedItem(0.0f, 70.9f, 0.0f, ItemType.DIRT, 10);
            item.update(0.6f, world);
            world.replaceDroppedItems(List.of(item));

            List<DroppedItem> collected = world.collectNearbyItems(
                    item.getX(), item.getY(), item.getZ(), 1.0f / 20.0f, player);

            assertEquals(1, collected.size());
            assertEquals(4, collected.get(0).getCount());
            assertEquals(64, player.getInventory().getHotbar()[0].getCount());
            assertEquals(1, world.getDroppedItems().size());
            assertEquals(6, world.getDroppedItems().get(0).getCount());
            assertEquals(1, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.ITEM_PICKUP)
                    .filter(particle -> particle.getItemParticleType() == ItemType.DIRT)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    private static List<Integer> sortedCounts(World world) {
        return world.getDroppedItems().stream()
                .map(DroppedItem::getCount)
                .sorted()
                .toList();
    }

    private static void fillInventory(Player player) {
        for (int i = 0; i < player.getInventory().getHotbar().length; i++) {
            player.getInventory().getHotbar()[i] = new ItemStack(ItemType.COBBLESTONE, 64);
        }
        for (int i = 0; i < player.getInventory().getMainInventory().length; i++) {
            player.getInventory().getMainInventory()[i] = new ItemStack(ItemType.COBBLESTONE, 64);
        }
    }

    private static final class SequenceRandom extends Random {
        private final float[] values;
        private int index;

        private SequenceRandom(float... values) {
            this.values = values;
        }

        @Override
        public float nextFloat() {
            if (values.length == 0) {
                return 0.0f;
            }
            float value = values[index % values.length];
            index++;
            return value;
        }

        private int nextFloatCalls() {
            return index;
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
}
