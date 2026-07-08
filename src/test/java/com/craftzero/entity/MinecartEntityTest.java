package com.craftzero.entity;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.RailShapeResolver;
import com.craftzero.world.RedstoneEngine;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecartEntityTest {
    private static final float ASCENDING_RAIL_ACCELERATION = 0.0078125f;
    private static final float EMPTY_RAIL_DRAG = 0.96f;

    @Test
    @DisplayName("Minecart attacks use legacy damage accumulation and break threshold")
    void attacksAccumulateAndBreakAtLegacyThreshold() {
        World world = new World(6100L);
        try {
            MinecartEntity cart = new MinecartEntity(0.5f, 70.0f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            world.spawnEntity(cart);

            for (int i = 0; i < 4; i++) {
                assertTrue(cart.attack(1.0f, false));
            }
            assertFalse(cart.isRemoved());
            assertEquals(MinecartEntity.BREAK_DAMAGE, cart.getDamage(), 0.0001f);

            assertTrue(cart.attack(1.0f, false));

            assertTrue(cart.isRemoved());
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.MINECART && item.getCount() == 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Minecart hit wobble and damage decay every tick")
    void hitStateDecaysEveryTick() {
        MinecartEntity cart = new MinecartEntity(MinecartEntity.CartKind.RIDEABLE);

        cart.attack(1.0f, false);
        assertEquals(MinecartEntity.HIT_ROLLING_TICKS, cart.getRollingAmplitude());
        assertEquals(10.0f, cart.getDamage(), 0.0001f);

        cart.tick();

        assertEquals(MinecartEntity.HIT_ROLLING_TICKS - 1, cart.getRollingAmplitude());
        assertEquals(9.0f, cart.getDamage(), 0.0001f);
    }

    @Test
    @DisplayName("Chest minecarts drop inventory, chest, and base minecart")
    void chestMinecartDropsComponentsAndContents() {
        World world = new World(6101L);
        try {
            ChestMinecartEntity cart = new ChestMinecartEntity(0.5f, 70.0f, 0.5f);
            cart.getInventory()[0] = new ItemStack(ItemType.DIAMOND, 3);
            world.spawnEntity(cart);

            cart.attack(5.0f, false);

            assertTrue(cart.isRemoved());
            assertDrop(world, ItemType.DIAMOND, 3);
            assertDrop(world, ItemType.CHEST, 1);
            assertDrop(world, ItemType.MINECART, 1);
            assertFalse(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.CHEST_MINECART));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Furnace minecarts drop furnace and base minecart")
    void furnaceMinecartDropsComponents() {
        World world = new World(6102L);
        try {
            FurnaceMinecartEntity cart = new FurnaceMinecartEntity(0.5f, 70.0f, 0.5f);
            world.spawnEntity(cart);

            cart.attack(5.0f, false);

            assertTrue(cart.isRemoved());
            assertDrop(world, ItemType.FURNACE, 1);
            assertDrop(world, ItemType.MINECART, 1);
            assertFalse(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.FURNACE_MINECART));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Explosions should destroy minecarts and keep their Release-style drops")
    void explosionDestroysMinecartAndKeepsLegacyDrops() {
        World world = new World(6122L);
        try {
            ChestMinecartEntity cart = new ChestMinecartEntity(0.5f, 70.0f, 0.5f);
            cart.getInventory()[0] = new ItemStack(ItemType.DIAMOND, 3);
            world.replaceEntities(java.util.List.of(cart));
            world.replaceDroppedItems(java.util.List.of(new DroppedItem(0.5f, 70.0f, 0.5f,
                    ItemType.DIRT, 1, 0.0f, 0.0f, 0.0f)));

            world.explode(0.5f, 70.0f, 0.5f, 4.0f);

            assertTrue(cart.isRemoved());
            assertDrop(world, ItemType.DIAMOND, 3);
            assertDrop(world, ItemType.CHEST, 1);
            assertDrop(world, ItemType.MINECART, 1);
            assertFalse(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.DIRT));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Lava contact destroys minecarts through legacy damage and variant drops")
    void lavaContactDestroysChestMinecartWithLegacyDrops() {
        World world = new World(6123L);
        try {
            clearSpace(world, -1, 1, 69, 72, -1, 1);
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.LAVA, 0);
            ChestMinecartEntity cart = new ChestMinecartEntity(0.5f, 70.0f, 0.5f);
            cart.getInventory()[0] = new ItemStack(ItemType.DIAMOND, 3);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            assertFalse(cart.isRemoved());
            assertEquals(MinecartEntity.BREAK_DAMAGE, cart.getDamage(), 0.0001f);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(cart.isRemoved());
            assertDrop(world, ItemType.DIAMOND, 3);
            assertDrop(world, ItemType.CHEST, 1);
            assertDrop(world, ItemType.MINECART, 1);
            assertFalse(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.CHEST_MINECART));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Only rideable minecarts can hold the local player passenger")
    void rideablePassengerState() {
        MinecartEntity rideable = new MinecartEntity(MinecartEntity.CartKind.RIDEABLE);
        ChestMinecartEntity chest = new ChestMinecartEntity();

        assertTrue(rideable.mountPlayer());
        assertTrue(rideable.hasPlayerPassenger());
        assertFalse(rideable.mountPlayer());

        rideable.dismountPlayer();
        assertFalse(rideable.hasPlayerPassenger());
        assertFalse(chest.mountPlayer());
    }

    @Test
    @DisplayName("Mounted player input can nudge a stopped rideable minecart")
    void mountedPlayerInputNudgesStoppedCart() {
        MinecartEntity cart = new MinecartEntity(MinecartEntity.CartKind.RIDEABLE);

        assertTrue(cart.mountPlayer());
        cart.applyRiderInput(0.0f);

        assertEquals(0.0f, cart.getMotionX(), 0.0001f);
        assertEquals(-0.1f, cart.getMotionZ(), 0.0001f);
    }

    @Test
    @DisplayName("Mounted minecarts should move 75 percent this step while keeping speed better than empty carts")
    void mountedMinecartUsesOccupiedRailDrag() {
        World world = new World(6107L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            world.setBlock(4, 69, 0, BlockType.STONE, 0);
            world.setBlock(4, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            world.setBlock(8, 69, 0, BlockType.STONE, 0);
            world.setBlock(8, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            MinecartEntity empty = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            MinecartEntity mounted = new MinecartEntity(4.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            MinecartEntity mobMounted = new MinecartEntity(8.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            TestLivingEntity mob = new TestLivingEntity(8.5f, 70.1f, 0.5f);
            empty.setMotion(0.2f, 0.0f, 0.0f);
            mounted.setMotion(0.2f, 0.0f, 0.0f);
            mobMounted.setMotion(0.2f, 0.0f, 0.0f);
            assertTrue(mounted.mountPlayer());
            assertTrue(mobMounted.mountLivingEntity(mob));
            world.replaceEntities(java.util.List.of(empty, mounted, mobMounted, mob));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.7f, empty.getX(), 0.0001f);
            assertEquals(4.65f, mounted.getX(), 0.0001f);
            assertEquals(8.65f, mobMounted.getX(), 0.0001f);
            assertEquals(0.192f, empty.getMotionX(), 0.0001f);
            assertEquals(0.1994f, mounted.getMotionX(), 0.0001f);
            assertEquals(0.1994f, mobMounted.getMotionX(), 0.0001f);
            assertEquals(mobMounted.getX(), mob.getX(), 0.0001f);
            assertTrue(mounted.getMotionX() > empty.getMotionX());
            assertTrue(mobMounted.getMotionX() > empty.getMotionX());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Powered rails should not launch stopped carts without a directional stopper block")
    void poweredRailDoesNotLaunchStoppedCartWithoutStopperBlock() {
        World world = new World(6103L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.POWERED_RAIL,
                    RailShapeResolver.EAST_WEST | RedstoneEngine.RAIL_POWERED_BIT);
            MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.5f, cart.getX(), 0.0001f);
            assertEquals(0.0f, cart.getMotionX(), 0.0001f);
            assertEquals(0.0f, cart.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Stopped minecarts should preserve yaw instead of rotating on a zero movement tick")
    void stoppedMinecartPreservesYawOnRailTick() {
        World world = new World(6119L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            cart.setYaw(42.0f);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(42.0f, cart.getYaw(), 0.0001f);
            assertEquals(0.5f, cart.getX(), 0.0001f);
            assertEquals(0.5f, cart.getZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Tiny rail nudges below the legacy movement threshold should preserve minecart yaw")
    void tinyRailMovementPreservesYaw() {
        World world = new World(6120L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            cart.setMotion(0.02f, 0.0f, 0.0f);
            cart.setYaw(42.0f);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(42.0f, cart.getYaw(), 0.0001f);
            assertEquals(0.52f, cart.getX(), 0.0001f);
            assertEquals(0.0192f, cart.getMotionX(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Powered rails should queue stopped-cart launch after the current movement step")
    void poweredRailLaunchesStoppedCartAfterMovementStep() {
        World world = new World(6104L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(-1, 70, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.POWERED_RAIL,
                    RailShapeResolver.EAST_WEST | RedstoneEngine.RAIL_POWERED_BIT);
            MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.5f, cart.getX(), 0.0001f);
            assertEquals(0.02f, cart.getMotionX(), 0.0001f);
            assertEquals(0.0f, cart.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Powered rails should boost moving carts after the current movement step")
    void poweredRailBoostsMovingCartAfterMovementStep() {
        World world = new World(6113L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.POWERED_RAIL,
                    RailShapeResolver.EAST_WEST | RedstoneEngine.RAIL_POWERED_BIT);
            MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            cart.setMotion(0.2f, 0.0f, 0.0f);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.7f, cart.getX(), 0.0001f);
            assertEquals(0.252f, cart.getMotionX(), 0.0001f);
            assertEquals(0.0f, cart.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Unpowered powered rails should stop very slow carts and brake faster carts")
    void unpoweredPoweredRailUsesReleaseStopThreshold() {
        World world = new World(6110L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.POWERED_RAIL, RailShapeResolver.EAST_WEST);
            world.setBlock(4, 69, 0, BlockType.STONE, 0);
            world.setBlock(4, 70, 0, BlockType.POWERED_RAIL, RailShapeResolver.EAST_WEST);
            MinecartEntity slow = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            MinecartEntity faster = new MinecartEntity(4.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            slow.setMotion(0.02f, 0.0f, 0.0f);
            faster.setMotion(0.04f, 0.0f, 0.0f);
            world.replaceEntities(java.util.List.of(slow, faster));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.5f, slow.getX(), 0.0001f);
            assertEquals(0.0f, slow.getMotionX(), 0.0001f);
            assertEquals(4.52f, faster.getX(), 0.0001f);
            assertEquals(0.0192f, faster.getMotionX(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Rider input should start carts on unpowered powered rails without braking the nudge")
    void riderInputBypassesUnpoweredPoweredRailBrake() {
        World world = new World(6111L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.POWERED_RAIL, RailShapeResolver.EAST_WEST);
            MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            assertTrue(cart.mountPlayer());
            world.replaceEntities(java.util.List.of(cart));

            cart.applyRiderInput(90.0f);
            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.575f, cart.getX(), 0.0001f);
            assertEquals(0.0997f, cart.getMotionX(), 0.0001f);
            assertEquals(0.0f, cart.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Off-rail minecarts should use legacy clamp, gravity, and airborne drag")
    void offRailMinecartsUseLegacyFallbackPhysics() {
        World world = new World(6118L);
        try {
            MinecartEntity cart = new MinecartEntity(0.5f, 80.0f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            cart.setMotion(0.6f, 0.0f, -0.6f);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.9f, cart.getX(), 0.0001f);
            assertEquals(79.96f, cart.getY(), 0.0001f);
            assertEquals(0.1f, cart.getZ(), 0.0001f);
            assertEquals(0.38f, cart.getMotionX(), 0.0001f);
            assertEquals(-0.038f, cart.getMotionY(), 0.0001f);
            assertEquals(-0.38f, cart.getMotionZ(), 0.0001f);
            assertFalse(cart.isOnGround());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Minecarts should not snap upward to unconnected raised rails")
    void minecartDoesNotSnapToUnconnectedRaisedRail() {
        World world = new World(6127L);
        try {
            clearSpace(world, -1, 1, 69, 72, -1, 1);
            world.setBlock(0, 71, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            cart.setMotion(0.2f, 0.0f, 0.0f);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.7f, cart.getX(), 0.0001f);
            assertEquals(70.06f, cart.getY(), 0.0001f);
            assertEquals(0.5f, cart.getZ(), 0.0001f);
            assertEquals(0.19f, cart.getMotionX(), 0.0001f);
            assertEquals(-0.038f, cart.getMotionY(), 0.0001f);
            assertEquals(0.0f, cart.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Minecart rail speed cap should clamp each horizontal axis independently")
    void railSpeedCapClampsEachHorizontalAxisIndependently() {
        World world = new World(6112L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.CURVE_SOUTH_EAST);
            MinecartEntity cart = new MinecartEntity(0.55f, 70.1f, 0.95f, MinecartEntity.CartKind.RIDEABLE);
            cart.setMotion(0.6f, 0.0f, 0.0f);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.95f, cart.getX(), 0.0001f);
            assertEquals(0.55f, cart.getZ(), 0.0001f);
            assertEquals(0.6f * 0.7071f * EMPTY_RAIL_DRAG, cart.getMotionX(), 0.0001f);
            assertEquals(-0.6f * 0.7071f * EMPTY_RAIL_DRAG, cart.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Curved rails should flip projection direction from incoming motion")
    void curvedRailsProjectMotionFromEitherDirection() {
        World world = new World(6114L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.CURVE_SOUTH_EAST);
            world.setBlock(4, 69, 0, BlockType.STONE, 0);
            world.setBlock(4, 70, 0, BlockType.RAIL, RailShapeResolver.CURVE_SOUTH_EAST);
            MinecartEntity forward = new MinecartEntity(0.55f, 70.1f, 0.95f, MinecartEntity.CartKind.RIDEABLE);
            MinecartEntity reverse = new MinecartEntity(4.95f, 70.1f, 0.55f, MinecartEntity.CartKind.RIDEABLE);
            forward.setMotion(0.2f, 0.0f, 0.0f);
            reverse.setMotion(-0.2f, 0.0f, 0.0f);
            world.replaceEntities(java.util.List.of(forward, reverse));

            world.updateEntities(1.0f / 20.0f);

            float projected = (float) (0.2f / Math.sqrt(2.0));
            assertEquals(0.55f + projected, forward.getX(), 0.0001f);
            assertEquals(0.95f - projected, forward.getZ(), 0.0001f);
            assertEquals(projected * EMPTY_RAIL_DRAG, forward.getMotionX(), 0.0001f);
            assertEquals(-projected * EMPTY_RAIL_DRAG, forward.getMotionZ(), 0.0001f);
            assertEquals(4.95f - projected, reverse.getX(), 0.0001f);
            assertEquals(0.55f + projected, reverse.getZ(), 0.0001f);
            assertEquals(-projected * EMPTY_RAIL_DRAG, reverse.getMotionX(), 0.0001f);
            assertEquals(projected * EMPTY_RAIL_DRAG, reverse.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Curved rails should project cart position onto the source rail path")
    void curvedRailsProjectPositionOntoRailPath() {
        World world = new World(6117L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.CURVE_SOUTH_EAST);
            MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.75f, cart.getX(), 0.0001f);
            assertEquals(0.75f, cart.getZ(), 0.0001f);
            assertEquals(0.0f, cart.getMotionX(), 0.0001f);
            assertEquals(0.0f, cart.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Minecarts should realign stored motion after crossing into a new rail cell")
    void minecartCrossingRailCellRealignsStoredMotion() {
        World world = new World(6115L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.CURVE_SOUTH_EAST);
            world.setBlock(1, 69, 0, BlockType.STONE, 0);
            world.setBlock(1, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            MinecartEntity cart = new MinecartEntity(0.8f, 70.1f, 0.7f, MinecartEntity.CartKind.RIDEABLE);
            cart.setMotion(0.4f, 0.0f, 0.0f);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            float projected = (float) (0.4f / Math.sqrt(2.0));
            float postDragSpeed = 0.4f * EMPTY_RAIL_DRAG;
            assertEquals(0.8f + projected, cart.getX(), 0.0001f);
            assertEquals(0.7f - projected, cart.getZ(), 0.0001f);
            assertEquals(postDragSpeed, cart.getMotionX(), 0.0001f);
            assertEquals(0.0f, cart.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Slope transitions should adjust stored speed from rail path height delta")
    void slopeTransitionAdjustsSpeedFromRailHeightDelta() {
        World world = new World(6116L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.ASCENDING_EAST);
            world.setBlock(1, 70, 0, BlockType.STONE, 0);
            world.setBlock(1, 71, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            MinecartEntity cart = new MinecartEntity(0.8f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            cart.setMotion(0.4f, 0.0f, 0.0f);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            float uphillMotion = 0.4f - ASCENDING_RAIL_ACCELERATION;
            float expectedMotion = uphillMotion * EMPTY_RAIL_DRAG - 0.01f;
            assertEquals(0.8f + uphillMotion, cart.getX(), 0.0001f);
            assertEquals(71.0625f, cart.getY(), 0.0001f);
            assertEquals(0.5f, cart.getZ(), 0.0001f);
            assertEquals(expectedMotion, cart.getMotionX(), 0.0001f);
            assertEquals(0.0f, cart.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ascending rails should apply the Release-style downhill acceleration")
    void ascendingRailsApplyReleaseStyleDownhillAcceleration() {
        assertAscendingRailMovesCart(RailShapeResolver.ASCENDING_EAST, -1, 0);
        assertAscendingRailMovesCart(RailShapeResolver.ASCENDING_WEST, 1, 0);
        assertAscendingRailMovesCart(RailShapeResolver.ASCENDING_NORTH, 0, 1);
        assertAscendingRailMovesCart(RailShapeResolver.ASCENDING_SOUTH, 0, -1);
    }

    @Test
    @DisplayName("World minecart placement should only accept Release 1.0 minecart items")
    void worldPlacementOnlyAcceptsMinecartItems() {
        World world = new World(6105L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            world.setBlock(1, 69, 0, BlockType.STONE, 0);
            world.setBlock(1, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            world.setBlock(2, 69, 0, BlockType.STONE, 0);
            world.setBlock(2, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);

            assertTrue(world.placeMinecartOnRail(0, 70, 0, ItemType.MINECART));
            assertTrue(world.placeMinecartOnRail(1, 70, 0, ItemType.CHEST_MINECART));
            assertTrue(world.placeMinecartOnRail(2, 70, 0, ItemType.FURNACE_MINECART));
            assertFalse(world.placeMinecartOnRail(0, 70, 0, ItemType.DIAMOND));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(1L, world.getEntities().stream()
                    .filter(entity -> entity instanceof MinecartEntity cart
                            && cart.getKind() == MinecartEntity.CartKind.RIDEABLE)
                    .count());
            assertEquals(1L, world.getEntities().stream().filter(ChestMinecartEntity.class::isInstance).count());
            assertEquals(1L, world.getEntities().stream().filter(FurnaceMinecartEntity.class::isInstance).count());
            assertEquals(3, world.getEntities().size());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Minecart item placement should lift carts on ascending rails")
    void minecartPlacementLiftsCartsOnAscendingRails() {
        World world = new World(6121L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            world.setBlock(2, 69, 0, BlockType.STONE, 0);
            world.setBlock(3, 70, 0, BlockType.STONE, 0);
            world.setBlock(2, 70, 0, BlockType.RAIL, RailShapeResolver.ASCENDING_EAST);

            assertTrue(world.placeMinecartOnRail(0, 70, 0, ItemType.MINECART));
            assertTrue(world.placeMinecartOnRail(2, 70, 0, ItemType.MINECART));

            assertFalse(world.hasMinecartIntersecting(0.0f, 71.0f, 0.0f, 1.0f, 71.25f, 1.0f),
                    "flat rail placement should not spawn the cart half a block too high");
            assertTrue(world.hasMinecartIntersecting(2.0f, 71.0f, 0.0f, 3.0f, 71.25f, 1.0f),
                    "ascending rail placement should apply the old half-block lift");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Minecarts transfer momentum when their hit boxes overlap on rails")
    void minecartsTransferMomentumOnCollision() {
        World world = new World(6106L);
        try {
            for (int x = 0; x <= 2; x++) {
                world.setBlock(x, 69, 0, BlockType.STONE, 0);
                world.setBlock(x, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            }
            MinecartEntity moving = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            MinecartEntity stopped = new MinecartEntity(1.1f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            moving.setMotion(0.2f, 0.0f, 0.0f);
            world.replaceEntities(java.util.List.of(moving, stopped));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(moving.getMotionX() < 0.16f);
            assertTrue(stopped.getMotionX() > 0.05f);
            assertEquals(0.0f, moving.getMotionZ(), 0.0001f);
            assertEquals(0.0f, stopped.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Minecart collision sweep should expand horizontally only")
    void worldMinecartCollisionSweepDoesNotExpandVertically() {
        World world = new World(6126L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            MinecartEntity lower = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            MinecartEntity higher = new MinecartEntity(1.1f, 70.9f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            lower.setYaw(90.0f);
            world.replaceEntities(java.util.List.of(lower, higher));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.0f, lower.getMotionX(), 0.0001f,
                    "Release minecart sweeps use X/Z padding but do not grow the collision query upward");
            assertEquals(0.0f, higher.getMotionX(), 0.0001f);
            assertFalse(lower.getBoundingBox().intersects(higher.getBoundingBox()));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Overlapping minecarts should still resolve collision from relative motion")
    void overlappingMinecartsResolveFromRelativeMotion() {
        MinecartEntity moving = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
        MinecartEntity stopped = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
        moving.setMotion(0.2f, 0.0f, 0.0f);

        moving.collideWithMinecart(stopped);

        assertTrue(moving.getMotionX() < 0.12f);
        assertTrue(stopped.getMotionX() > 0.09f);
        assertEquals(0.0f, moving.getMotionZ(), 0.0001f);
        assertEquals(0.0f, stopped.getMotionZ(), 0.0001f);
    }

    @Test
    @DisplayName("Minecart side contacts should be ignored when not aligned with travel axis")
    void minecartSideContactsIgnoreNonAlignedCollision() {
        MinecartEntity moving = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
        MinecartEntity side = new MinecartEntity(0.5f, 70.1f, 1.1f, MinecartEntity.CartKind.RIDEABLE);
        moving.setMotion(0.2f, 0.0f, 0.0f);
        moving.setYaw(90.0f);

        moving.collideWithMinecart(side);

        assertEquals(0.2f, moving.getMotionX(), 0.0001f);
        assertEquals(0.0f, moving.getMotionZ(), 0.0001f);
        assertEquals(0.0f, side.getMotionX(), 0.0001f);
        assertEquals(0.0f, side.getMotionZ(), 0.0001f);
    }

    @Test
    @DisplayName("Minecart collision impulse should weaken with center distance")
    void minecartCollisionImpulseScalesByDistance() {
        MinecartEntity moving = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
        MinecartEntity diagonal = new MinecartEntity(1.4f, 70.1f, 1.1f, MinecartEntity.CartKind.RIDEABLE);
        moving.setMotion(0.2f, 0.0f, 0.13333334f);
        moving.setYaw((float) Math.toDegrees(Math.atan2(0.2f, -0.13333334f)));

        moving.collideWithMinecart(diagonal);

        assertEquals(0.10153847f, moving.getMotionX(), 0.0001f);
        assertEquals(0.06769231f, moving.getMotionZ(), 0.0001f);
        assertEquals(0.13846155f, diagonal.getMotionX(), 0.0001f);
        assertEquals(0.0923077f, diagonal.getMotionZ(), 0.0001f);
    }

    @Test
    @DisplayName("Minecart collision alignment should use cart yaw rather than live motion")
    void minecartCollisionAlignmentUsesYawAxis() {
        MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
        MinecartEntity ahead = new MinecartEntity(1.1f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
        cart.setMotion(0.0f, 0.0f, 0.2f);
        cart.setYaw(90.0f);

        cart.collideWithMinecart(ahead);

        assertEquals(-0.05f, cart.getMotionX(), 0.0001f);
        assertEquals(0.14f, cart.getMotionZ(), 0.0001f);
        assertEquals(0.05f, ahead.getMotionX(), 0.0001f);
        assertEquals(0.1f, ahead.getMotionZ(), 0.0001f);
    }

    @Test
    @DisplayName("Storage minecarts shove living entities with the Release asymmetric impulse")
    void storageMinecartShovesLivingEntityWithReleaseImpulse() {
        ChestMinecartEntity cart = new ChestMinecartEntity(0.5f, 70.1f, 0.5f);
        TestLivingEntity mob = new TestLivingEntity(1.0f, 70.1f, 0.5f);
        cart.setMotion(0.2f, 0.0f, 0.0f);

        cart.collideWithLivingEntity(mob);

        assertEquals(0.1f, cart.getMotionX(), 0.0001f);
        assertEquals(0.025f, mob.getMotionX(), 0.0001f);
        assertEquals(0.0f, cart.getMotionZ(), 0.0001f);
        assertEquals(0.0f, mob.getMotionZ(), 0.0001f);
    }

    @Test
    @DisplayName("Exact minecart-mob overlaps should separate from cart motion")
    void exactMinecartMobOverlapResolvesFromCartMotion() {
        ChestMinecartEntity cart = new ChestMinecartEntity(0.5f, 70.1f, 0.5f);
        TestLivingEntity mob = new TestLivingEntity(0.5f, 70.1f, 0.5f);
        cart.setMotion(0.2f, 0.0f, 0.0f);

        cart.collideWithLivingEntity(mob);

        assertEquals(0.1f, cart.getMotionX(), 0.0001f);
        assertEquals(0.025f, mob.getMotionX(), 0.0001f);
        assertEquals(0.0f, cart.getMotionZ(), 0.0001f);
        assertEquals(0.0f, mob.getMotionZ(), 0.0001f);
    }

    @Test
    @DisplayName("Stationary exact minecart-mob overlaps should use a deterministic shove")
    void stationaryExactMinecartMobOverlapSeparatesDeterministically() {
        ChestMinecartEntity cart = new ChestMinecartEntity(0.5f, 70.1f, 0.5f);
        TestLivingEntity mob = new TestLivingEntity(0.5f, 70.1f, 0.5f);

        cart.collideWithLivingEntity(mob);

        assertEquals(-0.1f, cart.getMotionX(), 0.0001f);
        assertEquals(0.025f, mob.getMotionX(), 0.0001f);
        assertEquals(0.0f, cart.getMotionZ(), 0.0001f);
        assertEquals(0.0f, mob.getMotionZ(), 0.0001f);
    }

    @Test
    @DisplayName("Stationary rideable minecarts shove living entities instead of capturing them")
    void stationaryRideableMinecartShovesInsteadOfMountingLivingEntity() {
        MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
        TestLivingEntity mob = new TestLivingEntity(1.0f, 70.1f, 0.5f);

        cart.collideWithLivingEntity(mob);

        assertFalse(cart.hasLivingPassenger());
        assertEquals(-0.1f, cart.getMotionX(), 0.0001f);
        assertEquals(0.025f, mob.getMotionX(), 0.0001f);
        assertEquals(0.0f, cart.getMotionZ(), 0.0001f);
        assertEquals(0.0f, mob.getMotionZ(), 0.0001f);
    }

    @Test
    @DisplayName("Rideable minecarts pick up living entities they enter")
    void rideableMinecartMountsLivingEntityOnCollision() {
        World world = new World(6107L);
        try {
            for (int x = 0; x <= 2; x++) {
                world.setBlock(x, 69, 0, BlockType.STONE, 0);
                world.setBlock(x, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            }
            TestLivingEntity mob = new TestLivingEntity(1.5f, 70.0f, 0.5f);
            MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            cart.setMotion(0.4f, 0.0f, 0.0f);
            world.replaceEntities(java.util.List.of(mob, cart));

            world.updateEntities(1.0f / 20.0f);

            assertSame(mob, cart.getLivingPassenger());
            assertTrue(cart.hasLivingPassenger());
            assertEquals(cart.getX(), mob.getX(), 0.0001f);
            assertEquals(cart.getY() + 0.1f, mob.getY(), 0.0001f);
            assertEquals(cart.getZ(), mob.getZ(), 0.0001f);
            assertEquals(cart.getMotionX(), mob.getMotionX(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Rideable minecarts should not steal an already seated living passenger")
    void rideableMinecartDoesNotStealLivingPassenger() {
        World world = new World(6124L);
        try {
            TestLivingEntity mob = new TestLivingEntity(1.0f, 70.1f, 0.5f);
            MinecartEntity occupied = new MinecartEntity(1.0f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            MinecartEntity empty = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            world.replaceEntities(java.util.List.of(occupied, empty, mob));

            assertTrue(occupied.mountLivingEntity(mob));

            assertFalse(empty.mountLivingEntity(mob));
            assertSame(mob, occupied.getLivingPassenger());
            assertFalse(empty.hasLivingPassenger());

            empty.collideWithLivingEntity(mob);

            assertSame(mob, occupied.getLivingPassenger());
            assertFalse(empty.hasLivingPassenger());
            assertEquals(0.0f, empty.getMotionX(), 0.0001f);
            assertEquals(0.0f, empty.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("World minecart collision sweep preserves existing living passengers")
    void worldCollisionSweepDoesNotTransferLivingPassenger() {
        World world = new World(6125L);
        try {
            for (int x = 0; x <= 2; x++) {
                world.setBlock(x, 69, 0, BlockType.STONE, 0);
                world.setBlock(x, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            }
            TestLivingEntity mob = new TestLivingEntity(1.1f, 70.1f, 0.5f);
            MinecartEntity occupied = new MinecartEntity(1.1f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            MinecartEntity approaching = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            approaching.setMotion(0.4f, 0.0f, 0.0f);
            world.replaceEntities(java.util.List.of(occupied, approaching, mob));
            assertTrue(occupied.mountLivingEntity(mob));

            world.updateEntities(1.0f / 20.0f);

            assertSame(mob, occupied.getLivingPassenger());
            assertTrue(occupied.hasLivingPassenger());
            assertFalse(approaching.hasLivingPassenger());
            assertEquals(occupied.getX(), mob.getX(), 0.0001f);
            assertEquals(occupied.getY() + 0.1f, mob.getY(), 0.0001f);
            assertEquals(occupied.getZ(), mob.getZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("World collision pass lets storage minecarts shove mobs they enter")
    void worldStorageMinecartCollisionPassShovesLivingEntities() {
        World world = new World(6108L);
        try {
            for (int x = 0; x <= 2; x++) {
                world.setBlock(x, 69, 0, BlockType.STONE, 0);
                world.setBlock(x, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            }
            TestLivingEntity mob = new TestLivingEntity(1.5f, 70.0f, 0.5f);
            ChestMinecartEntity cart = new ChestMinecartEntity(0.5f, 70.1f, 0.5f);
            cart.setMotion(0.4f, 0.0f, 0.0f);
            world.replaceEntities(java.util.List.of(mob, cart));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.025f, mob.getMotionX(), 0.0001f);
            assertEquals(0.284f, cart.getMotionX(), 0.0001f);
            assertFalse(cart.hasLivingPassenger());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Furnace minecarts keep powered-cart momentum when shoving ordinary carts")
    void furnaceMinecartCollisionKeepsPoweredCartMomentum() {
        FurnaceMinecartEntity furnace = new FurnaceMinecartEntity(0.5f, 70.1f, 0.5f);
        MinecartEntity rideable = new MinecartEntity(1.1f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
        furnace.setMotion(0.2f, 0.0f, 0.0f);
        furnace.setYaw(90.0f);

        furnace.collideWithMinecart(rideable);

        assertEquals(0.19f, furnace.getMotionX(), 0.0001f);
        assertEquals(0.25f, rideable.getMotionX(), 0.0001f);
        assertEquals(0.0f, furnace.getMotionZ(), 0.0001f);
        assertEquals(0.0f, rideable.getMotionZ(), 0.0001f);
    }

    private static final class TestLivingEntity extends LivingEntity {
        private TestLivingEntity(float x, float y, float z) {
            super(0.6f, 1.8f, 20.0f);
            setPosition(x, y, z);
        }
    }

    private static void assertDrop(World world, ItemType type, int count) {
        assertTrue(world.getDroppedItems().stream()
                .anyMatch(item -> item.getItemType() == type && item.getCount() == count),
                () -> "Missing drop " + type + " x" + count);
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

    private static void assertAscendingRailMovesCart(int shape, int expectedXDirection, int expectedZDirection) {
        World world = new World(6200L + shape);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, shape);
            MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            float expectedMotion = ASCENDING_RAIL_ACCELERATION * (EMPTY_RAIL_DRAG + 0.05f);
            if (expectedXDirection < 0) {
                assertEquals(0.5f - ASCENDING_RAIL_ACCELERATION, cart.getX(), 0.0001f);
                assertEquals(-expectedMotion, cart.getMotionX(), 0.0001f);
                assertEquals(0.0f, cart.getMotionZ(), 0.0001f);
            } else if (expectedXDirection > 0) {
                assertEquals(0.5f + ASCENDING_RAIL_ACCELERATION, cart.getX(), 0.0001f);
                assertEquals(expectedMotion, cart.getMotionX(), 0.0001f);
                assertEquals(0.0f, cart.getMotionZ(), 0.0001f);
            }
            if (expectedZDirection < 0) {
                assertEquals(0.5f - ASCENDING_RAIL_ACCELERATION, cart.getZ(), 0.0001f);
                assertEquals(-expectedMotion, cart.getMotionZ(), 0.0001f);
                assertEquals(0.0f, cart.getMotionX(), 0.0001f);
            } else if (expectedZDirection > 0) {
                assertEquals(0.5f + ASCENDING_RAIL_ACCELERATION, cart.getZ(), 0.0001f);
                assertEquals(expectedMotion, cart.getMotionZ(), 0.0001f);
                assertEquals(0.0f, cart.getMotionX(), 0.0001f);
            }
        } finally {
            world.cleanup();
        }
    }
}
