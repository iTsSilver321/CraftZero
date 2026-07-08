package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ChickenTest {

    @Test
    @DisplayName("Baby chickens grow toward adulthood and do not lay eggs")
    void babyChickenGrowsWithoutLayingEggs() {
        World world = new World(5111L);
        try {
            Chicken chicken = new Chicken();
            chicken.setGrowingAge(-2);
            world.replaceEntities(List.of(chicken));

            world.updateEntities(1.0f / 60.0f);

            assertEquals(-1, chicken.getGrowingAge());
            assertTrue(chicken.isBaby());
            assertTrue(world.getDroppedItems().isEmpty());

            world.updateEntities(1.0f / 60.0f);

            assertEquals(0, chicken.getGrowingAge());
            assertFalse(chicken.isBaby());
            assertTrue(world.getDroppedItems().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Adult chickens drop eggs when their vanilla timer expires")
    void adultChickenDropsEggWhenTimerExpires() throws Exception {
        World world = new World(5110L);
        try {
            Chicken chicken = new Chicken();
            Field eggTimer = Chicken.class.getDeclaredField("eggTimer");
            eggTimer.setAccessible(true);
            eggTimer.setInt(chicken, 1);
            world.replaceEntities(List.of(chicken));

            world.updateEntities(1.0f / 60.0f);

            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.EGG && item.getCount() == 1));
            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertEquals(WorldSoundEvent.CHICKEN_PLOP, sounds.get(0).soundId());
            assertEquals(1.0f, sounds.get(0).volume(), 0.0001f);
            assertTrue(sounds.get(0).pitch() > 0.8f && sounds.get(0).pitch() < 1.2f);
            int resetTimer = eggTimer.getInt(chicken);
            assertTrue(resetTimer >= 6000 && resetTimer < 12000);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Airborne chickens should damp downward velocity with the old wing-flap factor")
    void airborneChickenDampsDownwardVelocity() {
        World world = new World(5112L);
        try {
            Chicken chicken = new Chicken();
            chicken.setPosition(0.5f, 120.0f, 0.5f);
            world.replaceEntities(List.of(chicken));

            world.updateEntities(1.0f / 60.0f);

            assertEquals(-0.01764f, chicken.getMotionY(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Looting should widen chicken feathers but not the single meat drop")
    void lootingWidensFeathersButNotChickenMeat() {
        World world = new World(5113L);
        try {
            Chicken chicken = new Chicken();
            chicken.random = fixedNextInts(2, 2);
            chicken.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(chicken));
            assertTrue(chicken.damage(1.0f, DamageSource.playerAttack(0.0f, 70.0f, 0.0f, 3)));

            chicken.dropLoot();

            assertEquals(4, droppedCount(world, ItemType.FEATHER));
            assertEquals(1, droppedCount(world, ItemType.RAW_CHICKEN));
        } finally {
            world.cleanup();
        }
    }

    private static int droppedCount(World world, ItemType type) {
        return world.getDroppedItems().stream()
                .filter(item -> item.getItemType() == type)
                .mapToInt(item -> item.getCount())
                .sum();
    }

    private static Random fixedNextInts(int... values) {
        return new Random() {
            private int index;

            @Override
            public int nextInt(int bound) {
                if (index >= values.length) {
                    return 0;
                }
                return Math.floorMod(values[index++], bound);
            }

            @Override
            public float nextFloat() {
                return 0.5f;
            }
        };
    }
}
