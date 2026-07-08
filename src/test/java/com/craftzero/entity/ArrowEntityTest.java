package com.craftzero.entity;

import com.craftzero.entity.mob.Skeleton;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.main.PlayerStats;
import com.craftzero.progression.AchievementType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArrowEntityTest {
    @Test
    @DisplayName("Arrow should stick in the first solid block it intersects")
    void arrowSticksInBlock() {
        World world = new World(11L);
        try {
            world.setBlock(3, 100, 0, BlockType.STONE);
            ArrowEntity arrow = world.spawnArrow(0.5f, 100.5f, 0.5f, 4.0f, 0.0f, 0.0f,
                    null, true, 4.0f);

            world.updateEntities(1.0f / 60.0f);

            assertFalse(arrow.isRemoved());
            assertTrue(arrow.isInGround());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Arrow should damage living entities on collision")
    void arrowDamagesMob() {
        World world = new World(12L);
        try {
            Zombie zombie = new Zombie();
            zombie.setPosition(3.0f, 100.0f, 0.5f);
            ArrowEntity arrow = world.spawnArrow(0.5f, 101.0f, 0.5f, 3.0f, 0.0f, 0.0f,
                    null, true, 5.0f);
            world.spawnEntity(zombie);

            world.updateEntities(1.0f / 60.0f);

            assertTrue(arrow.isRemoved());
            assertTrue(zombie.getHealth() < 20.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Fire arrows should ignite players after an accepted hit")
    void fireArrowsIgnitePlayers() {
        World world = new World(23L);
        try {
            clearAir(world, 0, 5, 99, 102, 0);
            Player player = new Player(3.0f, 100.0f, 0.5f);
            player.getStats().restore(20.0f, 17.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
            world.setPlayer(player);
            ArrowEntity arrow = new ArrowEntity(0.5f, 101.0f, 0.5f,
                    1.0f, 0.0f, 0.0f, null, false, 1.0f);
            arrow.setFireTicksOnHit(100);
            world.replaceEntities(java.util.List.of(arrow));

            for (int i = 0; i < 4 && !arrow.isRemoved(); i++) {
                world.updateEntities(1.0f / 20.0f);
            }

            assertTrue(arrow.isRemoved());
            assertTrue(player.isOnFire());
            assertEquals(100, player.getFireTicks());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Critical arrows should add the Release-style randomized hit damage")
    void criticalArrowsAddRandomizedDamageBonus() {
        World world = new World(18L);
        try {
            world.getRandom().setSeed(2L);
            Zombie zombie = new Zombie();
            zombie.setPosition(3.0f, 100.0f, 0.5f);
            ArrowEntity arrow = world.spawnArrow(0.5f, 101.0f, 0.5f, 3.0f, 0.0f, 0.0f,
                    null, true, 6.0f);
            arrow.setCritical(true);
            world.spawnEntity(zombie);

            world.updateEntities(1.0f / 60.0f);

            assertTrue(arrow.isRemoved());
            assertEquals(11.0f, zombie.getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player-owned arrows should unlock Sniper Duel after a 50-block skeleton kill")
    void playerOwnedArrowUnlocksSniperDuelAfterLongSkeletonKill() {
        World world = new World(22L);
        try {
            Player player = new Player(0.5f, 100.0f, 0.5f);
            world.setPlayer(player);
            unlockMonsterHunterParent(player);
            Skeleton skeleton = new Skeleton();
            skeleton.setPosition(51.5f, 100.0f, 0.5f);
            skeleton.setHealth(4.0f);
            world.spawnEntity(skeleton);
            ArrowEntity arrow = world.spawnArrow(0.5f, 101.2f, 0.5f, 60.0f, -0.2f, 0.0f,
                    null, true, 8.0f);

            world.updateEntities(1.0f / 20.0f);
            world.updateEntities(0.0f);

            assertTrue(arrow.isRemoved());
            assertTrue(player.getStats().getAchievements().isUnlocked(AchievementType.SNIPE_SKELETON));
            assertEquals(1, player.getStats().getStatistics().getMobKills());
            assertEquals(1, player.getStats().getStatistics().getMonsterKills());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Critical arrows should emit visible crit particles while flying")
    void criticalArrowsEmitTrailParticles() {
        World world = new World(19L);
        try {
            ArrowEntity arrow = world.spawnArrow(0.5f, 101.0f, 0.5f,
                    0.4f, 0.0f, 0.0f, null, true, 4.0f);
            arrow.setCritical(true);

            world.updateEntities(1.0f / 20.0f);

            assertEquals(4, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.CRIT)
                    .count());
            WorldParticle firstCrit = world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.CRIT)
                    .findFirst()
                    .orElseThrow();

            assertEquals(0.5f, firstCrit.getRenderX(0.0f), 0.0001f);
            assertEquals(101.0f, firstCrit.getRenderY(0.0f), 0.0001f);

            world.updateParticles(1.0f / 20.0f);

            assertEquals(0.492f, firstCrit.getRenderX(1.0f), 0.0001f);
            assertEquals(101.004f, firstCrit.getRenderY(1.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Underwater arrows should emit old bubble trails and use water drag")
    void underwaterArrowsEmitBubbleTrailsAndUseWaterDrag() {
        World world = new World(24L);
        try {
            for (int x = 0; x <= 3; x++) {
                world.setBlock(x, 80, 0, BlockType.WATER, 0);
            }
            ArrowEntity arrow = world.spawnArrow(0.5f, 80.5f, 0.5f,
                    0.5f, 0.0f, 0.0f, null, true, 4.0f);

            world.updateEntities(1.0f / 20.0f);

            assertFalse(arrow.isRemoved());
            assertEquals(4, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.BUBBLE)
                    .count());
            WorldParticle bubble = world.getParticles().get(0);
            assertEquals(0.875f, bubble.getRenderX(0.0f), 0.0001f);
            assertEquals(80.5f, bubble.getRenderY(0.0f), 0.0001f);
            assertEquals(0.5f, bubble.getRenderZ(0.0f), 0.0001f);
            assertEquals(0.4f, arrow.getMotionX(), 0.0001f);
            assertEquals(-0.05f, arrow.getMotionY(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Arrows should emit entry bursts only when crossing into water after their first tick")
    void arrowsEmitWaterEntryBurstAfterDryToWetTransition() {
        World world = new World(25L);
        try {
            ArrowEntity arrow = world.spawnArrow(0.5f, 80.5f, 0.5f,
                    0.5f, 0.0f, 0.0f, null, true, 4.0f);

            world.updateEntities(1.0f / 20.0f);
            world.setBlock(1, 80, 0, BlockType.WATER, 0);
            world.updateEntities(1.0f / 20.0f);

            assertFalse(arrow.isRemoved());
            assertEquals(10, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.BUBBLE)
                    .count());
            assertEquals(6, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SPLASH)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Arrows should destroy End crystals on direct contact")
    void arrowsDestroyEndCrystalsOnDirectContact() {
        World world = new World(15L);
        try {
            for (int x = 0; x <= 4; x++) {
                world.setBlock(x, 100, 0, BlockType.AIR, 0);
            }
            EndCrystalEntity crystal = new EndCrystalEntity(3.0f, 100.0f, 0.5f);
            world.spawnEntity(crystal);
            ArrowEntity arrow = world.spawnArrow(0.5f, 100.5f, 0.5f, 3.0f, 0.0f, 0.0f,
                    null, true, 4.0f);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(arrow.isRemoved());
            assertTrue(crystal.isExploded());
            assertTrue(crystal.isRemoved());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Arrows should break boats through the legacy vehicle attack path")
    void arrowsBreakBoatsOnDirectVehicleImpact() {
        World world = new World(21L);
        try {
            clearAir(world, 0, 5, 99, 102, 0);
            BoatEntity boat = new BoatEntity(3.0f, 100.0f, 0.5f);
            ArrowEntity arrow = new ArrowEntity(0.5f, 100.3f, 0.5f,
                    3.0f, 0.0f, 0.0f, null, true, 5.0f);
            world.replaceEntities(java.util.List.of(boat, arrow));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(arrow.isRemoved());
            assertTrue(boat.isRemoved());
            assertDrop(world, ItemType.OAK_PLANKS, 3);
            assertDrop(world, ItemType.STICK, 2);
            assertFalse(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.BOAT));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Arrows should break chest minecarts through variant drops")
    void arrowsBreakChestMinecartsOnDirectVehicleImpact() {
        World world = new World(22L);
        try {
            clearAir(world, 0, 5, 99, 102, 0);
            ChestMinecartEntity cart = new ChestMinecartEntity(3.0f, 100.0f, 0.5f);
            cart.getInventory()[0] = new ItemStack(ItemType.DIAMOND, 2);
            ArrowEntity arrow = new ArrowEntity(0.5f, 100.35f, 0.5f,
                    3.0f, 0.0f, 0.0f, null, true, 5.0f);
            world.replaceEntities(java.util.List.of(cart, arrow));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(arrow.isRemoved());
            assertTrue(cart.isRemoved());
            assertDrop(world, ItemType.DIAMOND, 2);
            assertDrop(world, ItemType.CHEST, 1);
            assertDrop(world, ItemType.MINECART, 1);
            assertFalse(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.CHEST_MINECART));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player-owned arrows should deflect fireballs on direct contact")
    void playerOwnedArrowsDeflectFireballs() {
        World world = new World(20L);
        try {
            for (int x = 0; x <= 5; x++) {
                world.setBlock(x, 100, 0, BlockType.AIR, 0);
            }
            ArrowEntity arrow = new ArrowEntity(0.5f, 100.5f, 0.5f,
                    3.0f, 0.0f, 0.0f, null, true, 4.0f);
            FireballEntity fireball = new FireballEntity(3.0f, 100.5f, 0.5f,
                    -0.55f, 0.0f, 0.0f, null, true);
            world.replaceEntities(java.util.List.of(arrow, fireball));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(arrow.isRemoved());
            assertFalse(fireball.isRemoved());
            assertTrue(fireball.isDeflectedByPlayer());
            assertTrue(fireball.getMotionX() > 0.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Stuck player arrows should release instead of vanishing when their block is removed")
    void stuckPlayerArrowReleasesWhenBlockIsRemoved() {
        World world = new World(14L);
        try {
            world.setBlock(3, 100, 0, BlockType.STONE);
            Player player = new Player(2.9f, 99.5f, 0.5f);
            world.setPlayer(player);
            ArrowEntity arrow = world.spawnArrow(0.5f, 100.5f, 0.5f, 4.0f, 0.0f, 0.0f,
                    null, true, 4.0f);

            world.updateEntities(1.0f / 20.0f);
            assertTrue(arrow.isInGround());

            world.setBlock(3, 100, 0, BlockType.AIR, 0);
            world.updateEntities(1.0f / 20.0f);

            assertFalse(arrow.isRemoved());
            assertFalse(arrow.isInGround());
            assertEquals(0, countInventory(player, ItemType.ARROW));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Picking up stuck player arrows should emit the Release-style pop sound")
    void stuckPlayerArrowPickupEmitsSound() {
        World world = new World(16L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            ArrowEntity arrow = new ArrowEntity(0.0f, 70.8f, 0.0f,
                    0.0f, 0.0f, 0.0f, null, true, 4.0f);
            arrow.setStuckInBlock(0, 70, 0, 0);
            world.replaceEntities(java.util.List.of(arrow));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(arrow.isRemoved());
            assertEquals(1, countInventory(player, ItemType.ARROW));
            WorldSoundEvent pickupSound = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.ITEM_PICKUP, pickupSound.soundId());
            assertEquals(0.2f, pickupSound.volume(), 0.0001f);
            assertTrue(pickupSound.pitch() >= 0.6f);
            assertTrue(pickupSound.pitch() <= 3.4f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Airborne arrows should not use the stuck-arrow despawn timer")
    void airborneArrowsDoNotUseStuckDespawnTimer() {
        ArrowEntity arrow = new ArrowEntity(0.0f, 100.0f, 0.0f,
                0.1f, 0.0f, 0.0f, null, true, 4.0f);

        for (int i = 0; i < 1201; i++) {
            arrow.tick();
        }

        assertFalse(arrow.isRemoved());
        assertFalse(arrow.isInGround());
    }

    @Test
    @DisplayName("Stuck arrows should still despawn after the old 1200-tick ground timer")
    void stuckArrowsDespawnAfterGroundTimer() {
        World world = new World(17L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE);
            ArrowEntity arrow = new ArrowEntity(0.0f, 70.8f, 0.0f,
                    0.0f, 0.0f, 0.0f, null, false, 4.0f);
            arrow.setStuckInBlock(0, 70, 0, 1199);
            world.replaceEntities(java.util.List.of(arrow));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(arrow.isRemoved());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Skeleton ranged attack should spawn a visible arrow entity")
    void skeletonSpawnsArrow() {
        World world = new World(13L);
        try {
            world.getChunkNow(0, 0);
            for (int x = 0; x <= 15; x++) {
                for (int z = 4; z <= 12; z++) {
                    world.setBlock(x, 99, z, BlockType.STONE);
                    for (int y = 100; y <= 103; y++) {
                        world.setBlock(x, y, z, BlockType.AIR);
                    }
                }
            }
            Player player = new Player(14.0f, 100.0f, 8.5f);
            player.setWorld(world);
            world.setPlayer(player);

            Skeleton skeleton = new Skeleton();
            skeleton.setPosition(0.5f, 100.0f, 8.5f);
            world.spawnEntity(skeleton);

            world.updateEntities(1.0f / 60.0f);
            assertTrue(skeleton.getAI().hasMoveTarget(), "skeleton should acquire the player");

            boolean sawArrow = false;
            for (int i = 0; i < 80; i++) {
                world.updateEntities(1.0f / 60.0f);
                if (world.hasEntityOfType(ArrowEntity.class)) {
                    sawArrow = true;
                    break;
                }
            }

            assertTrue(sawArrow,
                    () -> world.getEntities().stream()
                            .map(entity -> entity.getClass().getSimpleName())
                            .toList()
                            .toString());
            assertTrue(world.drainSoundEvents().stream()
                    .anyMatch(sound -> WorldSoundEvent.BOW.equals(sound.soundId())));
        } finally {
            world.cleanup();
        }
    }

    private static int countInventory(Player player, ItemType type) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getHotbar()) {
            if (stack != null && stack.getType() == type) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().getMainInventory()) {
            if (stack != null && stack.getType() == type) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void unlockMonsterHunterParent(Player player) {
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.OPEN_INVENTORY));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.MINE_WOOD));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.BUILD_WORKBENCH));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.BUILD_SWORD));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.KILL_ENEMY));
    }

    private static void assertDrop(World world, ItemType type, int count) {
        assertTrue(world.getDroppedItems().stream()
                .anyMatch(item -> item.getItemType() == type && item.getCount() == count),
                () -> "Missing drop " + type + " x" + count);
    }

    private static void clearAir(World world, int minX, int maxX, int minY, int maxY, int z) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                world.setBlock(x, y, z, BlockType.AIR, 0);
            }
        }
    }
}
