package com.craftzero.main;

import com.craftzero.entity.Entity;
import com.craftzero.entity.ChestMinecartEntity;
import com.craftzero.entity.FurnaceMinecartEntity;
import com.craftzero.entity.MinecartEntity;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.physics.AABB;
import com.craftzero.physics.Raycast;
import com.craftzero.progression.AchievementType;
import com.craftzero.world.BlockType;
import com.craftzero.world.RailShapeResolver;
import com.craftzero.world.World;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerMinecartInteractionTest {
    @Test
    @DisplayName("Player can mount and dismount a rideable minecart")
    void playerMountsAndDismountsRideableMinecart() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        MinecartEntity cart = new MinecartEntity(2.0f, 70.0f, 0.0f, MinecartEntity.CartKind.RIDEABLE);

        assertTrue(player.mountMinecart(cart));

        assertTrue(player.isRidingMinecart());
        assertSame(cart, player.getRidingMinecart());
        assertTrue(cart.hasPlayerPassenger());

        player.dismountMinecart();

        assertFalse(player.isRidingMinecart());
        assertFalse(cart.hasPlayerPassenger());
    }

    @Test
    @DisplayName("Minecart dismount should choose a clear side when the default side is blocked")
    void minecartDismountAvoidsBlockedDefaultSide() {
        World world = new World(6205L);
        try {
            world.setBlock(1, 70, 0, BlockType.STONE, 0);
            world.setBlock(1, 71, 0, BlockType.STONE, 0);
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.setWorld(world);
            MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);

            assertTrue(player.mountMinecart(cart));

            player.dismountMinecart();

            assertFalse(player.isRidingMinecart());
            assertFalse(cart.hasPlayerPassenger());
            assertTrue(player.getPosition().x < cart.getX(), "Expected west-side dismount when east side is blocked");
            assertClearOfLoadedCollision(world, player.getBoundingBox());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Removed mounted minecart clears player riding state on sync")
    void removedMinecartClearsRidingState() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        MinecartEntity cart = new MinecartEntity(2.0f, 70.0f, 0.0f, MinecartEntity.CartKind.RIDEABLE);

        assertTrue(player.mountMinecart(cart));
        cart.remove();
        player.syncRidingPosition();

        assertFalse(player.isRidingMinecart());
    }

    @Test
    @DisplayName("Release 1.0 On A Rail should unlock from a 1km mounted minecart trip")
    void onARailUnlocksFromLongMountedMinecartTrip() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        unlockAcquireIron(player);
        MinecartEntity cart = new MinecartEntity(0.0f, 70.0f, 0.0f, MinecartEntity.CartKind.RIDEABLE);

        assertTrue(player.mountMinecart(cart));
        cart.setPosition(999.99f, 70.0f, 0.0f);
        player.syncRidingPosition();
        assertFalse(player.getStats().getAchievements().isUnlocked(AchievementType.ON_A_RAIL));

        cart.setPosition(1000.0f, 70.0f, 0.0f);
        player.syncRidingPosition();

        assertTrue(player.getStats().getAchievements().isUnlocked(AchievementType.ON_A_RAIL));
    }

    @Test
    @DisplayName("World minecart collision pass should shove the local player")
    void worldMinecartCollisionPassShovesPlayer() {
        World world = new World(6206L);
        try {
            for (int x = 0; x <= 2; x++) {
                world.setBlock(x, 69, 0, BlockType.STONE, 0);
                world.setBlock(x, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            }
            Player player = new Player(1.1f, 70.0f, 0.5f);
            world.setPlayer(player);
            MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            cart.setMotion(0.2f, 0.0f, 0.0f);
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.025f, player.getVelocity().x, 0.0001f);
            assertEquals(0.0f, player.getVelocity().z, 0.0001f);
            assertEquals(0.092f, cart.getMotionX(), 0.0001f);
            assertEquals(0.0f, cart.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mounted minecart players should not be shoved by their own cart")
    void mountedMinecartPlayerSkipsWorldCollisionShove() {
        World world = new World(6207L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            Player player = new Player(0.5f, 70.2f, 0.5f);
            world.setPlayer(player);
            assertTrue(player.mountMinecart(cart));
            world.replaceEntities(java.util.List.of(cart));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.0f, player.getVelocity().x, 0.0001f);
            assertEquals(0.0f, player.getVelocity().z, 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Non-fuel furnace minecart use should redirect without adding fuel")
    void furnaceMinecartNonFuelUseRedirectsWithoutFuel() throws Exception {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        FurnaceMinecartEntity cart = new FurnaceMinecartEntity(2.0f, 70.0f, 0.0f);
        ItemStack diamond = new ItemStack(ItemType.DIAMOND, 1);

        assertTrue(handleEntityUse(player, diamond, cart));

        assertEquals(1, diamond.getCount());
        assertEquals(0, cart.getFuelTicks());
        assertEquals(2.0f, cart.getPushX(), 0.0001f);
        assertEquals(0.0f, cart.getPushZ(), 0.0001f);
        assertTrue(player.isUsingItem());
    }

    @Test
    @DisplayName("Empty-hand furnace minecart use should still redirect")
    void furnaceMinecartEmptyHandUseRedirects() throws Exception {
        Player player = new Player(0.0f, 70.0f, -1.0f);
        FurnaceMinecartEntity cart = new FurnaceMinecartEntity(2.0f, 70.0f, 1.0f);
        cart.setPush(-3.0f, 0.0f);

        assertTrue(handleEntityUse(player, null, cart));

        assertEquals(0, cart.getFuelTicks());
        assertEquals(2.0f, cart.getPushX(), 0.0001f);
        assertEquals(2.0f, cart.getPushZ(), 0.0001f);
        assertTrue(player.isUsingItem());
    }

    @Test
    @DisplayName("Coal player use fuels and redirects furnace minecarts")
    void coalPlayerUseFuelsAndRedirectsFurnaceMinecart() throws Exception {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        FurnaceMinecartEntity cart = new FurnaceMinecartEntity(2.0f, 70.0f, 0.0f);
        ItemStack coal = new ItemStack(ItemType.COAL, 1);
        player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = coal;

        assertTrue(handleEntityUse(player, coal, cart));

        assertNull(player.getInventory().getItemInHand());
        assertEquals(FurnaceMinecartEntity.FUEL_TICKS_PER_COAL, cart.getFuelTicks());
        assertEquals(2.0f, cart.getPushX(), 0.0001f);
        assertEquals(0.0f, cart.getPushZ(), 0.0001f);
        assertTrue(player.isUsingItem());
    }

    @Test
    @DisplayName("Chest minecart use should queue the storage screen")
    void chestMinecartUseQueuesStorageScreen() throws Exception {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        ChestMinecartEntity chestCart = new ChestMinecartEntity(2.0f, 70.0f, 0.0f);

        assertTrue(handleEntityUse(player, null, chestCart));

        assertSame(chestCart, player.getAndClearChestMinecartOpenRequest());
        assertTrue(player.isUsingItem());
    }

    @Test
    @DisplayName("Removed storage and furnace minecarts should reject player use")
    void removedMinecartVariantsRejectPlayerUse() throws Exception {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        ChestMinecartEntity chestCart = new ChestMinecartEntity(2.0f, 70.0f, 0.0f);
        FurnaceMinecartEntity furnaceCart = new FurnaceMinecartEntity(2.0f, 70.0f, 1.0f);
        ItemStack coal = new ItemStack(ItemType.COAL, 1);

        chestCart.remove();
        furnaceCart.remove();

        assertFalse(handleEntityUse(player, null, chestCart));
        assertNull(player.getAndClearChestMinecartOpenRequest());

        assertFalse(handleEntityUse(player, coal, furnaceCart));
        assertEquals(1, coal.getCount());
        assertEquals(0, furnaceCart.getFuelTicks());
        assertEquals(0.0f, furnaceCart.getPushX(), 0.0001f);
        assertEquals(0.0f, furnaceCart.getPushZ(), 0.0001f);
        assertFalse(player.isUsingItem());
    }

    private static boolean handleEntityUse(Player player, ItemStack stack, Entity entity) throws Exception {
        Method method = Player.class.getDeclaredMethod("handleEntityUse",
                ItemStack.class, Raycast.EntityRaycastResult.class, Raycast.RaycastResult.class);
        method.setAccessible(true);
        Raycast.EntityRaycastResult entityHit = new Raycast.EntityRaycastResult(true, entity, 1.0f,
                new Vector3f(entity.getX(), entity.getY(), entity.getZ()));
        return (boolean) method.invoke(player, stack, entityHit, Raycast.RaycastResult.miss());
    }

    private static void unlockAcquireIron(Player player) {
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.OPEN_INVENTORY));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.MINE_WOOD));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.BUILD_WORKBENCH));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.BUILD_PICKAXE));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.BUILD_FURNACE));
        assertTrue(player.getStats().getAchievements().unlock(AchievementType.ACQUIRE_IRON));
    }

    private static void assertClearOfLoadedCollision(World world, AABB box) {
        int minX = (int) Math.floor(box.getMin().x);
        int minY = (int) Math.floor(box.getMin().y);
        int minZ = (int) Math.floor(box.getMin().z);
        int maxX = (int) Math.floor(box.getMax().x - 0.0001f);
        int maxY = (int) Math.floor(box.getMax().y - 0.0001f);
        int maxZ = (int) Math.floor(box.getMax().z - 0.0001f);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (AABB collision : world.getCollisionBoxesIfLoaded(x, y, z)) {
                        assertFalse(box.intersects(collision), "Dismount box intersects loaded collision at "
                                + x + "," + y + "," + z);
                    }
                }
            }
        }
    }
}
