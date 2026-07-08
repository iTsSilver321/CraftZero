package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowGolemTest {
    @Test
    @DisplayName("Snow Golem snowball drops should ignore recent Looting credit")
    void snowGolemSnowballDropsIgnoreLooting() {
        World world = new World(6290L);
        try {
            SnowGolem golem = new SnowGolem();
            golem.random = new FixedIntRandom(15);
            golem.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(golem));
            assertTrue(golem.damage(1.0f, DamageSource.playerAttack(0.0f, 70.0f, 0.0f, 3)));

            golem.dropLoot();

            assertEquals(15, droppedCount(world, ItemType.SNOWBALL));
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
}
