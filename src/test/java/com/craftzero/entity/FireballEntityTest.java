package com.craftzero.entity;

import com.craftzero.entity.mob.Ghast;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.progression.AchievementType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FireballEntityTest {
    @Test
    @DisplayName("Fireballs remain active while flying through open air")
    void fireballPersistsInOpenAir() {
        World world = new World(5116L);
        try {
            for (int x = 1; x <= 4; x++) {
                world.setBlock(x, 120, 0, BlockType.AIR, 0);
            }
            FireballEntity fireball = world.spawnFireball(1.2f, 120.5f, 0.5f,
                    1.1f, 0.1f, 0.0f, null, false);

            world.updateEntities(1.0f / 60.0f);

            assertFalse(fireball.isRemoved());
            assertTrue(world.getEntities().contains(fireball));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Flying fireballs should emit old per-tick smoke trails")
    void flyingFireballsEmitSmokeTrails() {
        World world = new World(5131L);
        try {
            for (int x = 0; x <= 3; x++) {
                world.setBlock(x, 80, 0, BlockType.AIR, 0);
            }
            FireballEntity fireball = world.spawnFireball(0.5f, 80.5f, 0.5f,
                    0.5f, 0.0f, 0.0f, null, false);

            world.updateEntities(1.0f / 20.0f);

            assertFalse(fireball.isRemoved());
            assertEquals(1, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SMOKE)
                    .count());
            WorldParticle smoke = world.getParticles().get(0);
            assertSame(WorldParticle.Type.SMOKE, smoke.getType());
            assertEquals(1.0f, smoke.getRenderX(0.0f), 0.0001f);
            assertEquals(81.0f, smoke.getRenderY(0.0f), 0.0001f);
            assertEquals(0.5f, smoke.getRenderZ(0.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Underwater fireballs should emit old bubble trails before smoke")
    void underwaterFireballsEmitBubbleTrailsBeforeSmoke() {
        World world = new World(5132L);
        try {
            for (int x = 0; x <= 3; x++) {
                world.setBlock(x, 80, 0, BlockType.WATER, 0);
            }
            FireballEntity fireball = world.spawnFireball(0.5f, 80.5f, 0.5f,
                    0.5f, 0.0f, 0.0f, null, false);

            world.updateEntities(1.0f / 20.0f);

            assertFalse(fireball.isRemoved());
            assertEquals(5, world.getParticles().size());
            assertEquals(4, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.BUBBLE)
                    .count());
            assertSame(WorldParticle.Type.SMOKE, world.getParticles().get(4).getType());
            WorldParticle bubble = world.getParticles().get(0);
            assertEquals(0.875f, bubble.getRenderX(0.0f), 0.0001f);
            assertEquals(80.5f, bubble.getRenderY(0.0f), 0.0001f);
            assertEquals(0.5f, bubble.getRenderZ(0.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player-deflected fireballs reverse toward the player's aim")
    void playerDeflectedFireballUsesAimDirection() {
        FireballEntity fireball = new FireballEntity(0.0f, 100.0f, 0.0f,
                -0.55f, 0.0f, 0.0f, null, true);

        boolean deflected = fireball.deflectFromPlayer(new Vector3f(1.0f, 0.0f, 0.0f));

        assertTrue(deflected);
        assertTrue(fireball.isDeflectedByPlayer());
        assertEquals(0.55f, fireball.getMotionX(), 0.001f);
        assertEquals(0.0f, fireball.getMotionY(), 0.001f);
        assertEquals(0.0f, fireball.getMotionZ(), 0.001f);
    }

    @Test
    @DisplayName("Player-deflected fireballs do not immediately collide with the player owner")
    void playerDeflectedFireballIgnoresPlayerOwner() {
        World world = new World(5117L);
        try {
            com.craftzero.main.Player player = new com.craftzero.main.Player(0.0f, 100.0f, 0.0f);
            world.setPlayer(player);
            FireballEntity fireball = world.spawnFireball(0.0f, 101.0f, 0.0f,
                    -0.55f, 0.0f, 0.0f, null, true);

            fireball.deflectFromPlayer(new Vector3f(1.0f, 0.0f, 0.0f));
            world.updateEntities(1.0f / 60.0f);

            assertFalse(fireball.isRemoved());
            assertEquals(20.0f, player.getStats().getHealth(), 0.001f);
            assertTrue(fireball.getX() > 0.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player-deflected explosive fireballs should damage Ghasts with player credit")
    void playerDeflectedFireballDamagesGhastWithPlayerCredit() {
        World world = new World(5124L);
        try {
            clearAir(world, -1, 8, 99, 105, 0);
            Ghast ghast = new Ghast();
            ghast.setPosition(4.0f, 100.0f, 0.5f);
            FireballEntity fireball = new FireballEntity(0.5f, 102.0f, 0.5f,
                    -3.0f, 0.0f, 0.0f, ghast, true);
            world.replaceEntities(List.of(ghast, fireball));

            assertTrue(fireball.deflectFromPlayer(new Vector3f(1.0f, 0.0f, 0.0f)));
            world.updateEntities(1.0f / 20.0f);

            assertTrue(fireball.isRemoved());
            assertTrue(ghast.getHealth() < ghast.getMaxHealth());
            assertTrue(ghast.hasRecentPlayerDamage());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player-returned Ghast fireball kills should unlock Return to Sender")
    void playerReturnedFireballKillUnlocksReturnToSender() {
        World world = new World(5129L);
        try {
            clearAir(world, -1, 8, 99, 105, 0);
            Player player = new Player(0.0f, 100.0f, -6.0f);
            unlockNetherPath(player);
            world.setPlayer(player);
            Ghast ghast = new Ghast();
            ghast.setPosition(4.0f, 100.0f, 0.5f);
            ghast.setHealth(6.0f);
            FireballEntity fireball = new FireballEntity(0.5f, 102.0f, 0.5f,
                    -3.0f, 0.0f, 0.0f, ghast, true);
            world.replaceEntities(List.of(ghast, fireball));

            assertTrue(fireball.deflectFromPlayer(new Vector3f(1.0f, 0.0f, 0.0f)));
            world.updateEntities(1.0f / 20.0f);

            assertTrue(fireball.isRemoved());
            assertTrue(ghast.getHealth() <= 0.0f);
            assertTrue(player.getStats().getAchievements().isUnlocked(AchievementType.RETURN_TO_SENDER));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Non-deflected fireball hits should preserve Ghast fire immunity")
    void nonDeflectedFireballPreservesGhastFireImmunity() {
        World world = new World(5125L);
        try {
            clearAir(world, -1, 8, 99, 105, 0);
            Ghast ghast = new Ghast();
            ghast.setPosition(4.0f, 100.0f, 0.5f);
            FireballEntity fireball = new FireballEntity(0.5f, 102.0f, 0.5f,
                    3.0f, 0.0f, 0.0f, null, false);
            world.replaceEntities(List.of(ghast, fireball));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(fireball.isRemoved());
            assertEquals(ghast.getMaxHealth(), ghast.getHealth(), 0.001f);
            assertFalse(ghast.hasRecentPlayerDamage());
            assertFalse(ghast.isOnFire());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Fireballs should resolve block impacts at the raycast hit point")
    void fireballBlockImpactUsesRaycastHitPoint() {
        World world = new World(5118L);
        try {
            for (int x = 0; x <= 5; x++) {
                world.setBlock(x, 100, 0, BlockType.AIR, 0);
            }
            world.setBlock(4, 100, 0, BlockType.STONE, 0);
            FireballEntity fireball = world.spawnFireball(0.5f, 100.5f, 0.5f,
                    5.0f, 0.0f, 0.0f, null, false);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(fireball.isRemoved());
            assertEquals(4.0f, fireball.getX(), 0.001f);
            assertEquals(100.5f, fireball.getY(), 0.001f);
            assertEquals(0.5f, fireball.getZ(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Small fireballs should place fire in the adjacent air block on block impact")
    void smallFireballBlockImpactPlacesFireInAdjacentAir() {
        World world = new World(5120L);
        try {
            for (int x = 0; x <= 5; x++) {
                world.setBlock(x, 100, 0, BlockType.AIR, 0);
            }
            world.setBlock(4, 100, 0, BlockType.STONE, 0);
            FireballEntity fireball = world.spawnFireball(0.5f, 100.5f, 0.5f,
                    5.0f, 0.0f, 0.0f, null, false);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(fireball.isRemoved());
            assertSame(BlockType.FIRE, world.getBlock(3, 100, 0));
            assertSame(BlockType.STONE, world.getBlock(4, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Explosive fireballs should explode on block impact instead of placing adjacent fire")
    void explosiveFireballBlockImpactDoesNotPlaceAdjacentFire() {
        World world = new World(5121L);
        try {
            for (int x = 0; x <= 5; x++) {
                world.setBlock(x, 100, 0, BlockType.AIR, 0);
            }
            world.setBlock(4, 100, 0, BlockType.STONE, 0);
            FireballEntity fireball = world.spawnFireball(0.5f, 100.5f, 0.5f,
                    5.0f, 0.0f, 0.0f, null, true);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(fireball.isRemoved());
            assertNotSame(BlockType.FIRE, world.getBlock(3, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Explosive fireballs should use flaming explosion aftermath")
    void explosiveFireballBlockImpactUsesFlamingExplosion() {
        ExplosiveFireballWorld world = new ExplosiveFireballWorld(5128L);
        try {
            for (int x = 0; x <= 5; x++) {
                world.setBlock(x, 99, 0, BlockType.STONE, 0);
                world.setBlock(x, 100, 0, BlockType.AIR, 0);
            }
            world.setBlock(4, 100, 0, BlockType.STONE, 0);
            FireballEntity fireball = world.spawnFireball(0.5f, 100.5f, 0.5f,
                    5.0f, 0.0f, 0.0f, null, true);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(fireball.isRemoved());
            assertTrue(world.exploded());
            assertTrue(world.lastExplosionFlaming());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Small fireballs should ignite mobs only when the hit damage lands")
    void smallFireballIgnitesMobOnlyWhenDamageLands() {
        World world = new World(5122L);
        try {
            for (int x = 0; x <= 4; x++) {
                world.setBlock(x, 100, 0, BlockType.AIR, 0);
            }
            Zombie zombie = new Zombie();
            zombie.setPosition(3.0f, 100.0f, 0.5f);
            FireballEntity fireball = new FireballEntity(0.5f, 100.5f, 0.5f,
                    3.0f, 0.0f, 0.0f, null, false);
            world.replaceEntities(List.of(zombie, fireball));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(fireball.isRemoved());
            assertTrue(zombie.isOnFire());

            zombie.extinguish();
            FireballEntity rejectedFireball = new FireballEntity(0.5f, 100.5f, 0.5f,
                    3.0f, 0.0f, 0.0f, null, false);
            world.spawnEntity(rejectedFireball);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(rejectedFireball.isRemoved());
            assertFalse(zombie.isOnFire());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Explosive fireballs should not apply the small-fireball burn on direct mob hits")
    void explosiveFireballDirectHitDoesNotApplySmallFireballBurn() {
        World world = new World(5123L);
        try {
            for (int x = 0; x <= 4; x++) {
                world.setBlock(x, 100, 0, BlockType.AIR, 0);
            }
            Zombie zombie = new Zombie();
            zombie.setPosition(3.0f, 100.0f, 0.5f);
            FireballEntity fireball = new FireballEntity(0.5f, 100.5f, 0.5f,
                    3.0f, 0.0f, 0.0f, null, true);
            world.replaceEntities(List.of(zombie, fireball));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(fireball.isRemoved());
            assertFalse(zombie.isOnFire());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Direct fireball contact should destroy End crystals even for non-explosive fireballs")
    void directFireballContactDestroysEndCrystal() {
        World world = new World(5119L);
        try {
            for (int x = 0; x <= 4; x++) {
                world.setBlock(x, 100, 0, BlockType.AIR, 0);
            }
            EndCrystalEntity crystal = new EndCrystalEntity(3.0f, 100.0f, 0.5f);
            FireballEntity fireball = new FireballEntity(0.5f, 100.5f, 0.5f,
                    3.0f, 0.0f, 0.0f, null, false);
            world.replaceEntities(List.of(crystal, fireball));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(fireball.isRemoved());
            assertTrue(crystal.isExploded());
            assertTrue(crystal.isRemoved());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Small fireballs should break boats through the vehicle attack path")
    void smallFireballsBreakBoatsOnDirectVehicleImpact() {
        World world = new World(5126L);
        try {
            clearAir(world, 0, 5, 99, 102, 0);
            BoatEntity boat = new BoatEntity(3.0f, 100.0f, 0.5f);
            FireballEntity fireball = new FireballEntity(0.5f, 100.3f, 0.5f,
                    3.0f, 0.0f, 0.0f, null, false);
            world.replaceEntities(List.of(boat, fireball));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(fireball.isRemoved());
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
    @DisplayName("Explosive fireballs should detonate on minecart vehicle impact")
    void explosiveFireballsDetonateOnDirectMinecartImpact() {
        World world = new World(5127L);
        try {
            clearAir(world, 0, 5, 99, 102, 0);
            MinecartEntity cart = new MinecartEntity(3.0f, 100.0f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            FireballEntity fireball = new FireballEntity(0.5f, 100.35f, 0.5f,
                    3.0f, 0.0f, 0.0f, null, true);
            world.replaceEntities(List.of(cart, fireball));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(fireball.isRemoved());
            assertTrue(cart.isRemoved());
            assertDrop(world, ItemType.MINECART, 1);
        } finally {
            world.cleanup();
        }
    }

    private static void clearAir(World world, int minX, int maxX, int minY, int maxY, int z) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                world.setBlock(x, y, z, BlockType.AIR, 0);
            }
        }
    }

    private static void unlockNetherPath(Player player) {
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.OPEN_INVENTORY));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.MINE_WOOD));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.BUILD_WORKBENCH));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.BUILD_PICKAXE));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.BUILD_FURNACE));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.ACQUIRE_IRON));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.DIAMONDS));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.PORTAL));
    }

    private static void assertDrop(World world, ItemType type, int count) {
        assertTrue(world.getDroppedItems().stream()
                .anyMatch(item -> item.getItemType() == type && item.getCount() == count),
                () -> "Missing drop " + type + " x" + count);
    }

    private static final class ExplosiveFireballWorld extends World {
        private boolean exploded;
        private boolean lastExplosionFlaming;

        private ExplosiveFireballWorld(long seed) {
            super(seed);
        }

        @Override
        public void explode(float x, float y, float z, float power, boolean flaming) {
            exploded = true;
            lastExplosionFlaming = flaming;
            super.explode(x, y, z, power, flaming);
        }

        private boolean exploded() {
            return exploded;
        }

        private boolean lastExplosionFlaming() {
            return lastExplosionFlaming;
        }
    }
}
