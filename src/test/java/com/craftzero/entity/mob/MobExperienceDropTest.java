package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.ExperienceOrbEntity;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobExperienceDropTest {
    @Test
    @DisplayName("Player-killed passive animals should drop Release 1.0 random 1-3 XP")
    void passiveAnimalsDropRandomOneToThreeExperience() {
        assertPassiveExperienceRoll(0, 1);
        assertPassiveExperienceRoll(2, 3);
    }

    @Test
    @DisplayName("Player-killed water mobs should drop Release 1.0 random 1-3 XP")
    void waterMobsDropRandomOneToThreeExperience() {
        World world = new World(6283L);
        try {
            NoLootSquid squid = new NoLootSquid();
            squid.random = fixedIntRandom(1);
            squid.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(squid));

            assertTrue(squid.damage(squid.getMaxHealth(), DamageSource.playerAttack(0.0f, 70.0f, 0.0f, 0)));
            squid.tick();
            world.updateEntities(0.0f);

            assertEquals(2, spawnedExperienceValue(world));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player-killed hostile mobs should keep fixed Release 1.0 XP")
    void hostileMobsKeepFixedExperience() {
        World world = new World(6284L);
        try {
            NoLootZombie zombie = new NoLootZombie();
            zombie.random = fixedIntRandom(2);
            zombie.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(zombie));

            assertTrue(zombie.damage(zombie.getMaxHealth(), DamageSource.playerAttack(0.0f, 70.0f, 0.0f, 0)));
            zombie.tick();
            world.updateEntities(0.0f);

            assertEquals(5, spawnedExperienceValue(world));
        } finally {
            world.cleanup();
        }
    }

    private static void assertPassiveExperienceRoll(int randomValue, int expectedExperience) {
        World world = new World(6280L + randomValue);
        try {
            NoLootCow cow = new NoLootCow();
            cow.random = fixedIntRandom(randomValue);
            cow.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(cow));

            assertTrue(cow.damage(cow.getMaxHealth(), DamageSource.playerAttack(0.0f, 70.0f, 0.0f, 0)));
            cow.tick();
            world.updateEntities(0.0f);

            assertEquals(expectedExperience, spawnedExperienceValue(world));
        } finally {
            world.cleanup();
        }
    }

    private static int spawnedExperienceValue(World world) {
        return world.getEntities().stream()
                .filter(ExperienceOrbEntity.class::isInstance)
                .map(ExperienceOrbEntity.class::cast)
                .mapToInt(ExperienceOrbEntity::getValue)
                .sum();
    }

    private static Random fixedIntRandom(int value) {
        return new Random(0L) {
            @Override
            public int nextInt(int bound) {
                return Math.min(value, bound - 1);
            }

            @Override
            public float nextFloat() {
                return 0.5f;
            }
        };
    }

    private static final class NoLootCow extends Cow {
        @Override
        public void dropLoot() {
        }
    }

    private static final class NoLootSquid extends Squid {
        @Override
        public void dropLoot() {
        }
    }

    private static final class NoLootZombie extends Zombie {
        @Override
        public void dropLoot() {
        }
    }
}
