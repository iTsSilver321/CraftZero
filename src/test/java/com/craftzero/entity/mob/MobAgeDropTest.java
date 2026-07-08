package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.ExperienceOrbEntity;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobAgeDropTest {
    @Test
    @DisplayName("Release 1.0 baby animals should use half-size gameplay bounds")
    void babyAnimalsUseHalfSizeGameplayBounds() {
        Pig pig = new Pig();
        pig.setPosition(1.0f, 70.0f, 0.0f);
        float adultWidth = pig.getWidth();
        float adultHeight = pig.getHeight();

        pig.setGrowingAge(-1);

        assertTrue(pig.isBaby());
        assertEquals(adultWidth * 0.5f, pig.getWidth(), 0.0001f);
        assertEquals(adultHeight * 0.5f, pig.getHeight(), 0.0001f);
        assertEquals(adultWidth * 0.5f, pig.getBoundingBox().getWidth(), 0.0001f);
        assertEquals(adultHeight * 0.5f, pig.getBoundingBox().getHeight(), 0.0001f);

        pig.tickGrowingAge();

        assertFalse(pig.isBaby());
        assertEquals(adultWidth, pig.getWidth(), 0.0001f);
        assertEquals(adultHeight, pig.getHeight(), 0.0001f);
        assertEquals(adultWidth, pig.getBoundingBox().getWidth(), 0.0001f);
        assertEquals(adultHeight, pig.getBoundingBox().getHeight(), 0.0001f);
    }

    @Test
    @DisplayName("Release 1.0 baby animals should not drop adult loot")
    void babyAnimalsDoNotDropAdultLoot() {
        World world = new World(6280L);
        try {
            Pig pig = new Pig();
            pig.setGrowingAge(Mob.BABY_GROWING_AGE);
            pig.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(pig));

            pig.dropLoot();

            assertTrue(world.getDroppedItems().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 baby animals should not award player-kill XP")
    void babyAnimalsDoNotDropExperience() {
        World world = new World(6281L);
        try {
            Cow cow = new Cow();
            cow.setGrowingAge(Mob.BABY_GROWING_AGE);
            cow.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(cow));

            cow.damage(20.0f, DamageSource.point(DamageSource.Type.PLAYER_ATTACK,
                    0.0f, 70.0f, 0.0f, 0.0f, 0.0f));
            world.updateEntities(1.0f / 20.0f);
            world.updateEntities(1.0f / 20.0f);

            assertFalse(world.getEntities().stream().anyMatch(ExperienceOrbEntity.class::isInstance));
            assertTrue(world.getDroppedItems().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 baby sheep should reject shearing")
    void babySheepRejectShearing() {
        World world = new World(6282L);
        try {
            Sheep sheep = new Sheep();
            sheep.setGrowingAge(Mob.BABY_GROWING_AGE);
            sheep.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(sheep));

            assertFalse(sheep.shear());
            assertFalse(sheep.isSheared());
            assertTrue(world.getDroppedItems().stream().noneMatch(item -> item.getItemType() == ItemType.WHITE_WOOL));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Looting should widen Release 1.0 player-kill mob drop ranges")
    void lootingWidensPlayerKillMobDropRanges() {
        World plainWorld = new World(6283L);
        try {
            Zombie zombie = new Zombie();
            zombie.random = new FixedIntRandom(2);
            zombie.setPosition(1.0f, 70.0f, 0.0f);
            plainWorld.replaceEntities(List.of(zombie));

            zombie.damage(100.0f, DamageSource.playerAttack(0.0f, 70.0f, 0.0f, 0));
            zombie.tick();

            assertEquals(2, droppedCount(plainWorld, ItemType.ROTTEN_FLESH));
        } finally {
            plainWorld.cleanup();
        }

        World lootedWorld = new World(6284L);
        try {
            Zombie zombie = new Zombie();
            zombie.random = new FixedIntRandom(5);
            zombie.setPosition(1.0f, 70.0f, 0.0f);
            lootedWorld.replaceEntities(List.of(zombie));

            zombie.damage(100.0f, DamageSource.playerAttack(0.0f, 70.0f, 0.0f, 3));
            zombie.tick();

            assertEquals(5, droppedCount(lootedWorld, ItemType.ROTTEN_FLESH));
        } finally {
            lootedWorld.cleanup();
        }
    }

    @Test
    @DisplayName("Looting should use the Release-style separate base and enchantment rolls")
    void lootingUsesSeparateBaseAndEnchantmentRolls() {
        World world = new World(6285L);
        try {
            Zombie zombie = new Zombie();
            zombie.random = new SequenceRandom(1, 3);
            zombie.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(zombie));
            assertTrue(zombie.damage(1.0f, DamageSource.playerAttack(0.0f, 70.0f, 0.0f, 3)));

            zombie.dropLoot();

            assertEquals(4, droppedCount(world, ItemType.ROTTEN_FLESH));
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

    private static final class FixedIntRandom extends Random {
        private final int value;

        private FixedIntRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return Math.min(value, bound - 1);
        }

        @Override
        public float nextFloat() {
            return 0.5f;
        }
    }

    private static final class SequenceRandom extends Random {
        private final int[] values;
        private int index;

        private SequenceRandom(int... values) {
            this.values = values;
        }

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
    }
}
