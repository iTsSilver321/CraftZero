package com.craftzero.entity;

import com.craftzero.entity.mob.Blaze;
import com.craftzero.entity.mob.Chicken;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.CombatRules;
import com.craftzero.world.BlockType;
import com.craftzero.world.Dimension;
import com.craftzero.world.World;
import com.craftzero.world.WorldGenerator;
import com.craftzero.world.WorldParticle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ThrownItemEntityTest {
    @Test
    @DisplayName("Thrown item projectile remains active while flying through open air")
    void projectilePersistsInOpenAir() {
        World world = new World(5115L);
        try {
            for (int x = 1; x <= 4; x++) {
                world.setBlock(x, 120, 0, com.craftzero.world.BlockType.AIR, 0);
            }
            ThrownItemEntity snowball = world.spawnThrownItemProjectile(1.2f, 120.5f, 0.5f,
                    1.1f, 0.1f, 0.0f, ItemType.SNOWBALL, null);

            world.updateEntities(1.0f / 60.0f);

            assertFalse(snowball.isRemoved());
            assertTrue(world.getEntities().contains(snowball));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Underwater thrown items should emit old bubble trails and use water drag")
    void underwaterThrownItemsEmitBubbleTrailsAndUseWaterDrag() {
        World world = new World(5126L);
        try {
            for (int x = 0; x <= 3; x++) {
                world.setBlock(x, 80, 0, BlockType.WATER, 0);
            }
            ThrownItemEntity snowball = world.spawnThrownItemProjectile(0.5f, 80.5f, 0.5f,
                    0.5f, 0.0f, 0.0f, ItemType.SNOWBALL, null);

            world.updateEntities(1.0f / 20.0f);

            assertFalse(snowball.isRemoved());
            assertEquals(4, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.BUBBLE)
                    .count());
            WorldParticle bubble = world.getParticles().get(0);
            assertEquals(0.875f, bubble.getRenderX(0.0f), 0.0001f);
            assertEquals(80.5f, bubble.getRenderY(0.0f), 0.0001f);
            assertEquals(0.5f, bubble.getRenderZ(0.0f), 0.0001f);
            assertEquals(0.4f, snowball.getMotionX(), 0.0001f);
            assertEquals(-0.03f, snowball.getMotionY(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Snowballs damage blazes on impact")
    void snowballDamagesBlaze() {
        World world = new World(5114L);
        try {
            Blaze blaze = new Blaze();
            blaze.setPosition(3.0f, 100.0f, 0.5f);
            float before = blaze.getHealth();
            ThrownItemEntity snowball = world.spawnThrownItemProjectile(0.5f, 100.9f, 0.5f,
                    3.0f, 0.0f, 0.0f, ItemType.SNOWBALL, null);
            world.spawnEntity(blaze);

            world.updateEntities(1.0f / 60.0f);

            assertTrue(snowball.isRemoved());
            assertEquals(before - 3.0f, blaze.getHealth(), 0.001f);
            assertFalse(blaze.hasRecentPlayerDamage());
            assertSame(snowball, blaze.getLastDamageSource());

            blaze.dropLoot();
            assertEquals(0, droppedCount(world, ItemType.BLAZE_ROD));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player-owned snowballs mark Blazes for player-credit drops")
    void playerOwnedSnowballCreditsBlazeDamage() {
        World world = new World(5121L);
        try {
            Blaze blaze = new AlwaysDropBlaze();
            blaze.setPosition(3.0f, 100.0f, 0.5f);
            ThrownItemEntity snowball = world.spawnThrownItemProjectile(0.5f, 100.9f, 0.5f,
                    3.0f, 0.0f, 0.0f, ItemType.SNOWBALL, null, true);
            world.spawnEntity(blaze);

            world.updateEntities(1.0f / 60.0f);

            assertTrue(snowball.isRemoved());
            assertTrue(blaze.hasRecentPlayerDamage());
            assertEquals(0, blaze.getRecentPlayerLootingLevel());
            assertSame(snowball, blaze.getLastDamageSource());

            blaze.dropLoot();
            assertEquals(1, droppedCount(world, ItemType.BLAZE_ROD));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player-owned snowballs should deflect fireballs on direct contact")
    void playerOwnedSnowballsDeflectFireballs() {
        World world = new World(5124L);
        try {
            for (int x = 0; x <= 5; x++) {
                world.setBlock(x, 100, 0, BlockType.AIR, 0);
            }
            ThrownItemEntity snowball = new ThrownItemEntity(0.5f, 100.5f, 0.5f,
                    3.0f, 0.0f, 0.0f, ItemType.SNOWBALL, null, true);
            FireballEntity fireball = new FireballEntity(3.0f, 100.5f, 0.5f,
                    -0.55f, 0.0f, 0.0f, null, true);
            world.replaceEntities(List.of(snowball, fireball));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(snowball.isRemoved());
            assertFalse(fireball.isRemoved());
            assertTrue(fireball.isDeflectedByPlayer());
            assertTrue(fireball.getMotionX() > 0.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Snowballs should apply a zero-damage hit to ordinary living entities")
    void snowballAppliesZeroDamageHitToOrdinaryMob() {
        World world = new World(5119L);
        try {
            Zombie zombie = new Zombie();
            zombie.setPosition(3.0f, 100.0f, 0.5f);
            float before = zombie.getHealth();
            ThrownItemEntity snowball = new ThrownItemEntity(0.5f, 100.5f, 0.5f,
                    3.0f, 0.0f, 0.0f, ItemType.SNOWBALL, null, fixedNextInts(1));
            world.replaceEntities(List.of(zombie, snowball));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(snowball.isRemoved());
            assertEquals(before, zombie.getHealth(), 0.001f);
            assertEquals(10, zombie.getHurtTime());
            assertSame(snowball, zombie.getLastDamageSource());
            assertEquals(CombatRules.ARROW_HORIZONTAL_KNOCKBACK, zombie.getMotionX(), 0.0001f);
            assertTrue(zombie.getMotionY() > 0.0f);
            assertEquals(0.0f, zombie.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Eggs should apply a zero-damage hit to ordinary living entities before hatching")
    void eggAppliesZeroDamageHitToOrdinaryMob() {
        World world = new World(5120L);
        try {
            Zombie zombie = new Zombie();
            zombie.setPosition(3.0f, 100.0f, 0.5f);
            float before = zombie.getHealth();
            ThrownItemEntity egg = new ThrownItemEntity(0.5f, 100.5f, 0.5f,
                    3.0f, 0.0f, 0.0f, ItemType.EGG, null, fixedNextInts(1));
            world.replaceEntities(List.of(zombie, egg));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(egg.isRemoved());
            assertEquals(before, zombie.getHealth(), 0.001f);
            assertEquals(10, zombie.getHurtTime());
            assertSame(egg, zombie.getLastDamageSource());
            assertEquals(CombatRules.ARROW_HORIZONTAL_KNOCKBACK, zombie.getMotionX(), 0.0001f);
            assertTrue(zombie.getMotionY() > 0.0f);
            assertEquals(0.0f, zombie.getMotionZ(), 0.0001f);
            assertFalse(world.hasEntityOfType(Chicken.class));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Egg projectiles hatch one baby chicken on the vanilla 1-in-8 success roll")
    void eggProjectileHatchesBabyChicken() {
        World world = new World(5113L);
        try {
            world.setBlock(2, 100, 0, BlockType.STONE, 0);
            ThrownItemEntity egg = new ThrownItemEntity(0.5f, 100.5f, 0.5f,
                    3.0f, 0.0f, 0.0f, ItemType.EGG, null, fixedNextInts(0, 1));
            world.spawnEntity(egg);

            world.updateEntities(1.0f / 60.0f);

            assertTrue(egg.isRemoved());
            assertTrue(world.hasEntityOfType(Chicken.class));

            world.updateEntities(1.0f / 60.0f);

            List<Chicken> chickens = world.getEntities().stream()
                    .filter(Chicken.class::isInstance)
                    .map(Chicken.class::cast)
                    .toList();
            assertEquals(1, chickens.size());
            assertTrue(chickens.get(0).isBaby());
            assertEquals(Mob.BABY_GROWING_AGE + 1, chickens.get(0).getGrowingAge());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Egg projectiles can hatch four baby chickens on the rare vanilla bonus roll")
    void eggProjectileCanHatchFourBabyChickens() {
        World world = new World(5112L);
        try {
            world.setBlock(2, 100, 0, BlockType.STONE, 0);
            ThrownItemEntity egg = new ThrownItemEntity(0.5f, 100.5f, 0.5f,
                    3.0f, 0.0f, 0.0f, ItemType.EGG, null, fixedNextInts(0, 0));
            world.spawnEntity(egg);

            world.updateEntities(1.0f / 60.0f);
            world.updateEntities(1.0f / 60.0f);

            long chickens = world.getEntities().stream()
                    .filter(Chicken.class::isInstance)
                    .count();
            assertEquals(4, chickens);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("World-spawned egg hatching should use the world RNG")
    void worldSpawnedEggHatchingUsesWorldRandom() {
        CountingIntRandom random = fixedCountingNextInts(0, 1);
        World world = new RandomOverrideWorld(5118L, random);
        try {
            world.setBlock(2, 100, 0, BlockType.STONE, 0);
            ThrownItemEntity egg = world.spawnThrownItemProjectile(0.5f, 100.5f, 0.5f,
                    3.0f, 0.0f, 0.0f, ItemType.EGG, null);

            world.updateEntities(1.0f / 60.0f);
            world.updateEntities(1.0f / 60.0f);

            assertTrue(egg.isRemoved());
            assertEquals(2, random.nextIntCalls());
            List<Chicken> chickens = world.getEntities().stream()
                    .filter(Chicken.class::isInstance)
                    .map(Chicken.class::cast)
                    .toList();
            assertEquals(1, chickens.size());
            assertTrue(chickens.get(0).isBaby());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Thrown item block impacts should resolve at the raycast hit point")
    void thrownItemBlockImpactUsesRaycastHitPoint() {
        World world = new World(5117L);
        try {
            for (int x = 0; x <= 5; x++) {
                world.setBlock(x, 100, 0, BlockType.AIR, 0);
            }
            world.setBlock(4, 100, 0, BlockType.STONE, 0);
            ThrownItemEntity snowball = new ThrownItemEntity(0.5f, 100.5f, 0.5f,
                    5.0f, 0.0f, 0.0f, ItemType.SNOWBALL, null, fixedNextInts(1));
            world.replaceEntities(List.of(snowball));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(snowball.isRemoved());
            assertEquals(4.0f, snowball.getX(), 0.001f);
            assertEquals(100.5f, snowball.getY(), 0.001f);
            assertEquals(0.5f, snowball.getZ(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Thrown snowballs and eggs should emit old snowball-poof particles on impact")
    void thrownItemsEmitSnowballPoofParticlesOnImpact() {
        assertThrownImpactParticles(ItemType.SNOWBALL);
        assertThrownImpactParticles(ItemType.EGG);
    }

    private static void assertThrownImpactParticles(ItemType itemType) {
        World world = new World(5127L + itemType.getId());
        try {
            for (int x = 0; x <= 5; x++) {
                world.setBlock(x, 100, 0, BlockType.AIR, 0);
            }
            world.setBlock(4, 100, 0, BlockType.STONE, 0);
            ThrownItemEntity projectile = new ThrownItemEntity(0.5f, 100.5f, 0.5f,
                    5.0f, 0.0f, 0.0f, itemType, null, fixedNextInts(1));
            world.replaceEntities(List.of(projectile));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(projectile.isRemoved());
            assertEquals(8, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SNOWBALL_POOF)
                    .count());
            assertEquals(0, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.ITEM_CRACK)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Snowballs should impact boats before backing blocks")
    void snowballsImpactBoatsBeforeBlocks() {
        World world = new World(5125L);
        try {
            world.setBlock(4, 100, 0, BlockType.STONE, 0);
            ThrownItemEntity snowball = new ThrownItemEntity(0.5f, 100.3f, 0.5f,
                    5.0f, 0.0f, 0.0f, ItemType.SNOWBALL, null, fixedNextInts(1));
            BoatEntity boat = new BoatEntity(3.0f, 100.0f, 0.5f);
            world.replaceEntities(List.of(snowball, boat));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(snowball.isRemoved());
            assertFalse(boat.isRemoved());
            assertEquals(0.0f, boat.getDamage(), 0.001f);
            assertEquals(2.15f, snowball.getX(), 0.001f);
            assertEquals(100.3f, snowball.getY(), 0.001f);
            assertEquals(0.5f, snowball.getZ(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Eggs should impact minecarts before backing blocks")
    void eggsImpactMinecartsBeforeBlocks() {
        World world = new World(5126L);
        try {
            world.setBlock(4, 100, 0, BlockType.STONE, 0);
            ThrownItemEntity egg = new ThrownItemEntity(0.5f, 100.3f, 0.5f,
                    5.0f, 0.0f, 0.0f, ItemType.EGG, null, fixedNextInts(1));
            MinecartEntity minecart = new MinecartEntity(3.0f, 100.0f, 0.5f,
                    MinecartEntity.CartKind.RIDEABLE);
            world.replaceEntities(List.of(egg, minecart));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(egg.isRemoved());
            assertFalse(minecart.isRemoved());
            assertEquals(0.0f, minecart.getDamage(), 0.001f);
            assertEquals(2.41f, egg.getX(), 0.001f);
            assertEquals(100.3f, egg.getY(), 0.001f);
            assertEquals(0.5f, egg.getZ(), 0.001f);
            assertFalse(world.hasEntityOfType(Chicken.class));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Thrown eggs and snowballs should destroy End crystals through zero-damage impacts")
    void thrownItemsDestroyEndCrystalsWithZeroDamageImpact() {
        assertThrownItemDestroysEndCrystal(ItemType.SNOWBALL);
        assertThrownItemDestroysEndCrystal(ItemType.EGG);
    }

    private static void assertThrownItemDestroysEndCrystal(ItemType itemType) {
        World world = new World(5116L + itemType.getId(), WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            for (int x = 0; x <= 4; x++) {
                world.setBlock(x, 100, 0, BlockType.AIR, 0);
            }
            EndCrystalEntity crystal = new EndCrystalEntity(3.0f, 100.0f, 0.5f);
            ThrownItemEntity projectile = new ThrownItemEntity(0.5f, 100.5f, 0.5f,
                    3.0f, 0.0f, 0.0f, itemType, null, fixedNextInts(1));
            world.replaceEntities(List.of(crystal, projectile));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(projectile.isRemoved(), itemType + " projectile should be consumed on impact");
            assertTrue(crystal.isExploded(), itemType + " should trigger the End crystal explosion path");
            assertTrue(crystal.isRemoved(), itemType + " should remove the destroyed End crystal");
        } finally {
            world.cleanup();
        }
    }

    private static Random fixedNextInts(int... values) {
        return fixedCountingNextInts(values);
    }

    private static CountingIntRandom fixedCountingNextInts(int... values) {
        return new CountingIntRandom(values);
    }

    private static int droppedCount(World world, ItemType type) {
        return world.getDroppedItems().stream()
                .filter(item -> item.getItemType() == type)
                .mapToInt(item -> item.getCount())
                .sum();
    }

    private static final class AlwaysDropBlaze extends Blaze {
        private AlwaysDropBlaze() {
            random = new Random() {
                @Override
                public int nextInt(int bound) {
                    return Math.min(1, bound - 1);
                }
            };
        }
    }

    private static final class CountingIntRandom extends Random {
        private final int[] values;
        private int index;

        private CountingIntRandom(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int bound) {
            int value = values[Math.min(index, values.length - 1)];
            index++;
            if (value < 0 || value >= bound) {
                throw new IllegalArgumentException("Fixed random value " + value + " outside bound " + bound);
            }
            return value;
        }

        private int nextIntCalls() {
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
