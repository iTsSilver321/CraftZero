package com.craftzero.entity;

import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoatEntityTest {
    @Test
    @DisplayName("Boat attacks use legacy damage accumulation and component drops")
    void attacksAccumulateAndBreakAtLegacyThreshold() {
        World world = new World(6200L);
        try {
            BoatEntity boat = new BoatEntity(0.5f, 70.0f, 0.5f);
            world.spawnEntity(boat);

            for (int i = 0; i < 4; i++) {
                assertTrue(boat.attack(1.0f, false));
            }
            assertFalse(boat.isRemoved());
            assertEquals(BoatEntity.BREAK_DAMAGE, boat.getDamage(), 0.0001f);

            assertTrue(boat.attack(1.0f, false));

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
    @DisplayName("Boat hit wobble and damage decay every tick")
    void hitStateDecaysEveryTick() {
        BoatEntity boat = new BoatEntity();

        boat.attack(1.0f, false);
        assertEquals(BoatEntity.HIT_ROLLING_TICKS, boat.getRollingAmplitude());
        assertEquals(10.0f, boat.getDamage(), 0.0001f);

        boat.tick();

        assertEquals(BoatEntity.HIT_ROLLING_TICKS - 1, boat.getRollingAmplitude());
        assertEquals(9.0f, boat.getDamage(), 0.0001f);
    }

    @Test
    @DisplayName("Boat crash drops planks and sticks instead of the boat item")
    void crashDropsComponents() {
        World world = new World(6201L);
        try {
            BoatEntity boat = new BoatEntity(0.5f, 70.0f, 0.5f);
            world.spawnEntity(boat);

            boat.breakAsCrashDrops();

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
    @DisplayName("Fast horizontal wall collisions break boats at the Release-era crash threshold")
    void fastWallCollisionBreaksBoatAtLegacyThreshold() {
        World world = new World(6213L);
        try {
            clearSpace(world, -2, 3, 69, 72, -2, 2);
            world.setBlock(1, 70, 0, BlockType.STONE, 0);
            BoatEntity boat = new BoatEntity(0.24f, 70.0f, 0.5f);
            boat.setMotion(0.16f, 0.0f, 0.0f);
            world.spawnEntity(boat);

            boat.updatePhysics(1.0f / 20.0f);

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
    @DisplayName("Slow horizontal wall collisions do not shatter boats")
    void slowWallCollisionDoesNotBreakBoat() {
        World world = new World(6214L);
        try {
            clearSpace(world, -2, 3, 69, 72, -2, 2);
            world.setBlock(1, 70, 0, BlockType.STONE, 0);
            BoatEntity boat = new BoatEntity(0.24f, 70.0f, 0.5f);
            boat.setMotion(BoatEntity.CRASH_BREAK_SPEED, 0.0f, 0.0f);
            world.spawnEntity(boat);

            boat.updatePhysics(1.0f / 20.0f);

            assertFalse(boat.isRemoved());
            assertTrue(boat.isCollidedHorizontally());
            assertTrue(world.getDroppedItems().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Explosions should destroy boats and keep their Release-style component drops")
    void explosionDestroysBoatAndKeepsLegacyDrops() {
        World world = new World(6208L);
        try {
            clearSpace(world, -1, 1, 69, 72, -1, 1);
            BoatEntity boat = new BoatEntity(0.5f, 70.0f, 0.5f);
            world.replaceEntities(java.util.List.of(boat));
            world.replaceDroppedItems(java.util.List.of(new DroppedItem(0.5f, 70.0f, 0.5f,
                    ItemType.DIRT, 1, 0.0f, 0.0f, 0.0f)));

            world.explode(0.5f, 70.3f, 0.5f, 4.0f);

            assertTrue(boat.isRemoved());
            assertDrop(world, ItemType.OAK_PLANKS, 3);
            assertDrop(world, ItemType.STICK, 2);
            assertFalse(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.BOAT));
            assertFalse(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.DIRT));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Lava contact destroys boats through legacy damage and component drops")
    void lavaContactDestroysBoatWithLegacyDrops() {
        World world = new World(6211L);
        try {
            clearSpace(world, -1, 1, 69, 72, -1, 1);
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.LAVA, 0);
            BoatEntity boat = new BoatEntity(0.5f, 70.0f, 0.5f);
            world.replaceEntities(java.util.List.of(boat));

            world.updateEntities(1.0f / 20.0f);

            assertFalse(boat.isRemoved());
            assertEquals(BoatEntity.BREAK_DAMAGE, boat.getDamage(), 0.0001f);

            world.updateEntities(1.0f / 20.0f);

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
    @DisplayName("Fire contact damages boats through the legacy hit threshold")
    void fireContactBreaksBoatAtLegacyThreshold() {
        World world = new World(6212L);
        try {
            clearSpace(world, -1, 1, 69, 72, -1, 1);
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.FIRE, 0);
            BoatEntity boat = new BoatEntity(0.5f, 70.0f, 0.5f);
            world.replaceEntities(java.util.List.of(boat));

            for (int i = 0; i < 4; i++) {
                world.updateEntities(1.0f / 20.0f);
                assertFalse(boat.isRemoved());
            }

            world.updateEntities(1.0f / 20.0f);

            assertTrue(boat.isRemoved());
            assertDrop(world, ItemType.OAK_PLANKS, 3);
            assertDrop(world, ItemType.STICK, 2);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Boats can hold the local player passenger and apply mounted input")
    void passengerStateAndInput() {
        World world = new World(6202L);
        try {
            BoatEntity boat = new BoatEntity(0.5f, 70.0f, 0.5f);
            world.spawnEntity(boat);

            assertTrue(boat.mountPlayer());
            assertTrue(boat.hasPlayerPassenger());
            assertFalse(boat.mountPlayer());

            boat.applyRiderInput(0.0f, 1.0f, 0.0f);
            boat.updatePhysics(1.0f / 20.0f);

            assertEquals(0.0f, boat.getMotionX(), 0.0001f);
            assertTrue(boat.getMotionZ() < 0.0f);

            boat.dismountPlayer();
            assertFalse(boat.hasPlayerPassenger());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Flowing water should carry boats along decay gradients")
    void flowingWaterCurrentCarriesBoats() {
        World world = new World(6215L);
        try {
            clearSpace(world, -1, 2, 69, 72, -1, 1);
            world.setBlock(0, 70, 0, BlockType.WATER, 0);
            world.setBlock(1, 70, 0, BlockType.FLOWING_WATER, 1);
            BoatEntity boat = new BoatEntity(1.5f, 70.25f, 0.5f);
            world.spawnEntity(boat);

            boat.updatePhysics(1.0f / 20.0f);

            assertTrue(boat.getMotionX() > 0.01f, () -> "motionX=" + boat.getMotionX());
            assertTrue(boat.getX() > 1.51f, () -> "x=" + boat.getX());
            assertEquals(0.0f, boat.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Boats clamp each horizontal motion axis to the Release-era speed cap")
    void boatMotionClampUsesReleasePerAxisCap() {
        World world = new World(6209L);
        try {
            BoatEntity boat = new BoatEntity(0.5f, 90.0f, 0.5f);
            boat.setMotion(0.8f, 0.0f, 0.8f);
            world.spawnEntity(boat);

            boat.updatePhysics(1.0f / 20.0f);

            assertEquals(BoatEntity.MAX_WATER_SPEED * 0.99f, boat.getMotionX(), 0.0001f);
            assertEquals(BoatEntity.MAX_WATER_SPEED * 0.99f, boat.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Boats turn toward travel by at most the Release-era yaw step")
    void boatYawTurnsTowardTravelWithSourceStepLimit() {
        World world = new World(6210L);
        try {
            BoatEntity boat = new BoatEntity(0.5f, 90.0f, 0.5f);
            boat.setYaw(0.0f);
            boat.setMotion(0.1f, 0.0f, 0.0f);
            world.spawnEntity(boat);

            boat.updatePhysics(1.0f / 20.0f);

            assertEquals(-BoatEntity.MAX_YAW_TURN_DEGREES, boat.getYaw(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Boats break lily pads as drops while clearing their path")
    void boatBreaksLilyPadsAsDrops() {
        World world = new World(6203L);
        try {
            world.setBlock(0, 70, 0, BlockType.WATER, 0);
            world.setBlock(0, 71, 0, BlockType.LILY_PAD, 0);
            BoatEntity boat = new BoatEntity(0.5f, 70.25f, 0.5f);
            world.spawnEntity(boat);

            boat.updatePhysics(1.0f / 20.0f);

            assertEquals(BlockType.AIR, world.getBlock(0, 71, 0));
            assertDrop(world, ItemType.LILY_PAD, 1);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Fast boats in water should emit Release-era splash wake particles")
    void fastBoatInWaterEmitsSplashWake() {
        World world = new World(6206L);
        try {
            fillWater(world, -2, 2, -2, 2, 70);
            BoatEntity boat = new BoatEntity(0.5f, 70.25f, 0.5f);
            boat.setMotion(0.2f, 0.0f, 0.0f);
            world.spawnEntity(boat);

            boat.updatePhysics(1.0f / 20.0f);

            assertEquals(13, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SPLASH)
                    .count());
            WorldParticle splash = world.getParticles().get(0);
            assertEquals(boat.getY() - 0.125f, splash.getRenderY(1.0f), 0.0001f);
            assertEquals(0.16f, splash.getScale(0.0f), 0.0001f);
            assertEquals(10.0f, splash.getLifetimeTicks(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Slow or dry boats should not emit splash wake particles")
    void slowOrDryBoatsDoNotEmitSplashWake() {
        World world = new World(6207L);
        try {
            fillWater(world, -2, 2, -2, 2, 70);
            BoatEntity slow = new BoatEntity(0.5f, 70.25f, 0.5f);
            slow.setMotion(0.14f, 0.0f, 0.0f);
            world.spawnEntity(slow);

            slow.updatePhysics(1.0f / 20.0f);

            BoatEntity dry = new BoatEntity(3.5f, 72.0f, 0.5f);
            dry.setMotion(0.25f, 0.0f, 0.0f);
            world.spawnEntity(dry);

            dry.updatePhysics(1.0f / 20.0f);

            assertEquals(0, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SPLASH)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("World collision pass lets boats shove living entities")
    void worldBoatCollisionPassShovesLivingEntities() {
        World world = new World(6204L);
        try {
            BoatEntity boat = new BoatEntity(0.5f, 70.0f, 0.5f);
            TestLivingEntity mob = new TestLivingEntity(1.0f, 70.0f, 0.5f);
            boat.setMotion(0.1f, 0.0f, 0.0f);
            world.replaceEntities(java.util.List.of(boat, mob));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(mob.getMotionX() > 0.03f);
            assertTrue(boat.getMotionX() < 0.1f);
            assertEquals(0.0f, mob.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("World collision pass lets overlapping boats transfer horizontal push")
    void worldBoatCollisionPassTransfersPushBetweenBoats() {
        World world = new World(6205L);
        try {
            BoatEntity moving = new BoatEntity(0.5f, 70.0f, 0.5f);
            BoatEntity bumped = new BoatEntity(1.0f, 70.0f, 0.5f);
            moving.setMotion(0.1f, 0.0f, 0.0f);
            world.replaceEntities(java.util.List.of(moving, bumped));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(bumped.getMotionX() > 0.01f);
            assertTrue(moving.getMotionX() < 0.09f);
            assertEquals(0.0f, bumped.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    private static void assertDrop(World world, ItemType type, int count) {
        assertTrue(world.getDroppedItems().stream()
                .anyMatch(item -> item.getItemType() == type && item.getCount() == count),
                () -> "Missing drop " + type + " x" + count);
    }

    private static void fillWater(World world, int minX, int maxX, int minZ, int maxZ, int y) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlock(x, y, z, BlockType.WATER, 0);
            }
        }
    }

    private static void clearSpace(World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    world.setBlock(x, y, z, BlockType.AIR, 0);
                }
            }
        }
    }

    private static final class TestLivingEntity extends LivingEntity {
        private TestLivingEntity(float x, float y, float z) {
            super(0.6f, 1.8f, 20.0f);
            setPosition(x, y, z);
        }
    }
}
