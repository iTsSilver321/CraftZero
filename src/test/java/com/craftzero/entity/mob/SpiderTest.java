package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Difficulty;
import com.craftzero.main.Player;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.world.World;
import com.craftzero.world.BlockType;
import com.craftzero.world.DayCycleManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiderTest {
    @Test
    @DisplayName("Release 1.0 spiders drop string but reserve spider eyes for rare player-credit drops")
    void spiderDropsStringAndGatesSpiderEyesBehindPlayerCredit() {
        World world = new World(6274L);
        try {
            Spider spider = new Spider();
            spider.random = fixedNextInts(2, 0);
            spider.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(spider));

            spider.dropLoot();

            assertEquals(2, droppedCount(world, ItemType.STRING));
            assertEquals(0, droppedCount(world, ItemType.SPIDER_EYE));

            Spider playerHitSpider = new Spider();
            playerHitSpider.random = fixedNextInts(1, 0);
            playerHitSpider.setPosition(3.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(playerHitSpider));
            assertTrue(playerHitSpider.damage(1.0f, DamageSource.playerAttack(0.0f, 70.0f, 0.0f, 0)));

            playerHitSpider.dropLoot();

            assertEquals(1, droppedCount(world, ItemType.SPIDER_EYE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Looting should widen the rare spider-eye player-kill chance")
    void lootingWidensRareSpiderEyeChance() {
        World world = new World(6277L);
        try {
            Spider spider = new Spider();
            spider.random = fixedNextInts(0, 0, 1, 1);
            spider.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(spider));
            assertTrue(spider.damage(1.0f, DamageSource.playerAttack(0.0f, 70.0f, 0.0f, 2)));

            spider.dropLoot();

            assertEquals(1, droppedCount(world, ItemType.SPIDER_EYE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Cave spiders should inherit the rare player-credit spider-eye drop gate")
    void caveSpidersUseRareSpiderEyeDropGate() {
        World world = new World(6278L);
        try {
            CaveSpider environmental = new CaveSpider();
            environmental.random = fixedNextInts(2, 0);
            environmental.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(environmental));

            environmental.dropLoot();

            assertEquals(2, droppedCount(world, ItemType.STRING));
            assertEquals(0, droppedCount(world, ItemType.SPIDER_EYE));

            CaveSpider playerHit = new CaveSpider();
            playerHit.random = fixedNextInts(1, 0);
            playerHit.setPosition(3.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(playerHit));
            assertTrue(playerHit.damage(1.0f, DamageSource.playerAttack(0.0f, 70.0f, 0.0f, 0)));

            playerHit.dropLoot();

            assertEquals(1, droppedCount(world, ItemType.SPIDER_EYE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Cave spider poison should only apply after accepted melee damage")
    void caveSpiderPoisonOnlyAppliesAfterAcceptedMeleeDamage() {
        World protectedWorld = fixedLightWorld(6284L, 0, 0);
        try {
            prepareBrightArea(protectedWorld);
            Player protectedPlayer = new Player(1.1f, 100.0f, 0.5f);
            protectedPlayer.setDifficulty(Difficulty.NORMAL);
            protectedWorld.setPlayer(protectedPlayer);
            CaveSpider protectedAttacker = meleeCaveSpider(protectedWorld);

            protectedWorld.updateEntities(1.0f / 20.0f);

            assertFalse(protectedPlayer.getStats().hasEffect(StatusEffectType.POISON),
                    "spawn-protected players should not receive cave-spider poison from a rejected hit");
            assertTrue(protectedAttacker.getAttackCooldown() > 0,
                    "fixture should prove the cave spider actually attempted its melee swing");
        } finally {
            protectedWorld.cleanup();
        }

        World vulnerableWorld = fixedLightWorld(6285L, 0, 0);
        try {
            prepareBrightArea(vulnerableWorld);
            Player vulnerablePlayer = new Player(1.1f, 100.0f, 0.5f);
            vulnerablePlayer.setDifficulty(Difficulty.NORMAL);
            vulnerablePlayer.getStats().update(5.1f, false, false, vulnerablePlayer.getDifficulty());
            vulnerableWorld.setPlayer(vulnerablePlayer);
            meleeCaveSpider(vulnerableWorld);

            vulnerableWorld.updateEntities(1.0f / 20.0f);

            assertTrue(vulnerablePlayer.getStats().hasEffect(StatusEffectType.POISON));
            StatusEffectInstance effect = vulnerablePlayer.getStats().getActiveEffects().get(0);
            assertEquals(StatusEffectType.POISON, effect.type());
            assertEquals(7 * 20, effect.durationTicks());
        } finally {
            vulnerableWorld.cleanup();
        }
    }

    @Test
    @DisplayName("Cave spider melee poison should use Release difficulty durations")
    void caveSpiderPoisonUsesReleaseDifficultyDurations() {
        World world = fixedLightWorld(6286L, 0, 0);
        try {
            prepareBrightArea(world);
            Player player = new Player(1.1f, 100.0f, 0.5f);
            player.setDifficulty(Difficulty.HARD);
            player.getStats().update(5.1f, false, false, player.getDifficulty());
            world.setPlayer(player);
            meleeCaveSpider(world);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(player.getStats().hasEffect(StatusEffectType.POISON));
            StatusEffectInstance effect = player.getStats().getActiveEffects().get(0);
            assertEquals(StatusEffectType.POISON, effect.type());
            assertEquals(15 * 20, effect.durationTicks());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Environmental damage should not permanently provoke bright-light spiders")
    void environmentalDamageDoesNotProvokeSpider() {
        Spider spider = new Spider();

        assertTrue(spider.damage(1.0f, DamageSource.generic()));

        assertFalse(spider.isProvoked());
        assertEquals(spider.getMaxHealth() - 1.0f, spider.getHealth(), 0.001f);
    }

    @Test
    @DisplayName("Entity damage should permanently provoke spiders")
    void entityDamageProvokesSpider() {
        Spider spider = new Spider();
        Entity attacker = new Zombie();

        assertTrue(spider.damage(1.0f, DamageSource.entity(DamageSource.Type.MOB_MELEE, attacker)));

        assertTrue(spider.isProvoked());
    }

    @Test
    @DisplayName("Moderate Release brightness should make unprovoked spiders ignore players")
    void moderateBrightnessSpidersDoNotAcquirePlayerTargets() {
        World world = fixedLightWorld(6281L, 0, 8);
        try {
            prepareBrightArea(world);
            world.setPlayer(new Player(2.5f, 100.0f, 0.5f));
            Spider spider = new Spider();
            spider.setPosition(0.5f, 100.0f, 0.5f);
            world.replaceEntities(List.of(spider));

            world.updateEntities(1.0f / 20.0f);

            assertFalse(spider.getAI().hasMoveTarget());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Darker light should still let unprovoked spiders target players")
    void darkSpidersStillAcquirePlayerTargets() {
        World world = fixedLightWorld(6282L, 0, 7);
        try {
            prepareBrightArea(world);
            world.setPlayer(new Player(2.5f, 100.0f, 0.5f));
            Spider spider = new Spider();
            spider.setPosition(0.5f, 100.0f, 0.5f);
            world.replaceEntities(List.of(spider));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(spider.getAI().hasMoveTarget());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spiders should use the old mid-range leap attack when the roll succeeds")
    void spidersLeapAtMidRangeWhenRollSucceeds() {
        World world = fixedLightWorld(6283L, 0, 0);
        try {
            prepareBrightArea(world);
            Player player = new Player(4.5f, 100.0f, 0.5f);
            world.setPlayer(player);
            Spider spider = new Spider();
            spider.random = fixedNextInts(0);
            spider.setPosition(0.5f, 100.0f, 0.5f);
            world.replaceEntities(List.of(spider));
            spider.getAI().setMoveTarget(player.getPosition().x, player.getPosition().z);

            world.updateEntities(1.0f / 20.0f);
            assertTrue(spider.isOnGround(), "fixture should settle the spider on the floor");

            float beforeX = spider.getX();
            world.updateEntities(1.0f / 20.0f);

            assertTrue(spider.getX() > beforeX);
            assertTrue(spider.getMotionX() > 0.2f, "leap should add forward motion");
            assertTrue(spider.getMotionY() > 0.2f, "leap should add upward motion");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Bright-light spiders should keep an existing chase when the old interest roll misses")
    void brightSpiderKeepsExistingTargetWhenInterestRollMisses() {
        World world = brightWorld(6275L);
        try {
            Spider spider = chaser(world, fixedNextInts(1, 1, 1));

            world.updateEntities(1.0f / 60.0f);

            assertTrue(spider.getAI().hasMoveTarget());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Bright-light spiders should drop an existing chase when the old interest roll succeeds")
    void brightSpiderDropsExistingTargetWhenInterestRollSucceeds() {
        World world = brightWorld(6276L);
        try {
            Spider spider = chaser(world, fixedNextInts(0));

            world.updateEntities(1.0f / 60.0f);

            assertFalse(spider.getAI().hasMoveTarget());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spiders should climb solid walls through the Release horizontal-collision ladder flag")
    void spidersClimbWallsFromHorizontalCollision() {
        World world = new World(6279L);
        try {
            prepareSpiderWall(world);
            Spider spider = new Spider();
            spider.setPosition(0.25f, 100.0f, 0.5f);
            spider.setMotion(1.0f, -1.0f, 0.0f);
            world.replaceEntities(List.of(spider));

            world.updateEntities(1.0f / 20.0f);
            assertTrue(spider.isCollidedHorizontally(), "first tick should establish wall contact");

            spider.setMotion(1.0f, -1.0f, 0.0f);
            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.0f, spider.getMotionX(), 0.0001f);
            assertEquals(0.2f, spider.getMotionY(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spider wall climbing should reset fall tracking like ladder physics")
    void spiderWallClimbingPreventsFallDamageAccumulation() {
        World world = new World(6280L);
        try {
            prepareSpiderWall(world);
            Spider spider = new Spider();
            spider.setPosition(0.25f, 110.0f, 0.5f);
            spider.setMotion(1.0f, -1.0f, 0.0f);
            world.replaceEntities(List.of(spider));

            world.updateEntities(1.0f / 20.0f);
            for (int i = 0; i < 24; i++) {
                spider.setMotion(1.0f, -0.4f, 0.0f);
                world.updateEntities(1.0f / 20.0f);
            }

            int shortLandingY = (int) Math.floor(spider.getY()) - 2;
            for (int x = -1; x <= 0; x++) {
                for (int z = -1; z <= 1; z++) {
                    world.setBlock(x, shortLandingY, z, BlockType.STONE);
                }
            }
            float healthBeforeLanding = spider.getHealth();
            spider.setMotion(0.0f, -1.0f, 0.0f);
            for (int i = 0; i < 20 && !spider.isOnGround(); i++) {
                world.updateEntities(1.0f / 20.0f);
            }

            assertTrue(spider.isOnGround(), "fixture should land the spider after leaving the wall");
            assertEquals(healthBeforeLanding, spider.getHealth(), 0.001f);
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

    private static World brightWorld(long seed) {
        World world = fixedLightWorld(seed, 15, 0);
        DayCycleManager dayCycle = new DayCycleManager();
        dayCycle.setTime(6000.0f);
        world.setDayCycleManager(dayCycle);
        prepareBrightArea(world);
        Player player = new Player(2.5f, 100.0f, 0.5f);
        world.setPlayer(player);
        return world;
    }

    private static World fixedLightWorld(long seed, int skyLight, int blockLight) {
        return new FixedLightWorld(seed, skyLight, blockLight);
    }

    private static Spider chaser(World world, Random random) {
        Spider spider = new Spider();
        spider.random = random;
        spider.setPosition(0.5f, 100.0f, 0.5f);
        world.replaceEntities(List.of(spider));
        spider.getAI().setMoveTarget(2.5f, 0.5f);
        return spider;
    }

    private static CaveSpider meleeCaveSpider(World world) {
        CaveSpider spider = new CaveSpider();
        spider.setPosition(0.5f, 100.0f, 0.5f);
        world.replaceEntities(List.of(spider));
        spider.getAI().setMoveTarget(1.1f, 0.5f);
        return spider;
    }

    private static void prepareSpiderWall(World world) {
        world.getChunkNow(0, 0);
        for (int y = 90; y <= 112; y++) {
            world.setBlock(1, y, 0, BlockType.STONE);
        }
        for (int x = -1; x <= 0; x++) {
            for (int z = -1; z <= 1; z++) {
                world.setBlock(x, 99, z, BlockType.STONE);
                for (int y = 100; y <= 112; y++) {
                    world.setBlock(x, y, z, BlockType.AIR);
                }
            }
        }
    }

    private static void prepareBrightArea(World world) {
        world.getChunkNow(0, 0);
        for (int x = 0; x <= 3; x++) {
            world.setBlock(x, 99, 0, BlockType.STONE);
            for (int y = 100; y < 128; y++) {
                world.setBlock(x, y, 0, BlockType.AIR);
            }
        }
    }

    private static Random fixedNextInts(int... values) {
        return new Random() {
            private int index;

            @Override
            public int nextInt(int bound) {
                if (index >= values.length) {
                    return 0;
                }
                return values[index++] % bound;
            }
        };
    }

    private static final class FixedLightWorld extends World {
        private final int skyLight;
        private final int blockLight;

        private FixedLightWorld(long seed, int skyLight, int blockLight) {
            super(seed);
            this.skyLight = skyLight;
            this.blockLight = blockLight;
        }

        @Override
        public int getSkyLight(int x, int y, int z) {
            return skyLight;
        }

        @Override
        public int getBlockLightIfLoaded(int x, int y, int z, int fallback) {
            return blockLight;
        }
    }
}
