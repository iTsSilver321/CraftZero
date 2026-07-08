package com.craftzero.main;

import com.craftzero.entity.EnderPearlEntity;
import com.craftzero.entity.EyeOfEnderEntity;
import com.craftzero.entity.ThrownItemEntity;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.physics.Raycast;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerEnderPearlInteractionTest {
    @Test
    @DisplayName("Player throwing an ender pearl consumes one item and spawns the projectile")
    void playerThrowsEnderPearlFromHand() {
        World world = new World(6232L);
        try {
            Player player = new Player(0.0f, 120.0f, 0.0f);
            world.setPlayer(player);
            ItemStack stack = new ItemStack(ItemType.ENDER_PEARL, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = stack;

            assertTrue(player.throwEnderPearl(world, stack, new Vector3f(1.0f, 0.0f, 0.0f)));
            world.updateEntities(1.0f / 20.0f);

            assertNull(player.getInventory().getItemInHand());
            assertTrue(world.getEntities().stream().anyMatch(EnderPearlEntity.class::isInstance));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 ender pearls can be thrown back-to-back without item cooldown")
    void enderPearlsHaveNoReleaseOneCooldown() {
        World world = new World(6233L);
        try {
            Player player = new Player(0.0f, 120.0f, 0.0f);
            world.setPlayer(player);
            ItemStack stack = new ItemStack(ItemType.ENDER_PEARL, 2);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = stack;

            assertTrue(player.throwEnderPearl(world, stack, new Vector3f(1.0f, 0.0f, 0.0f)));
            assertTrue(player.throwEnderPearl(world, stack, new Vector3f(1.0f, 0.0f, 0.0f)));
            world.updateEntities(1.0f / 20.0f);

            assertNull(player.getInventory().getItemInHand());
            assertEquals(2L, world.getEntities().stream()
                    .filter(EnderPearlEntity.class::isInstance)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player throwing an eye of ender consumes one item and spawns the locator")
    void playerThrowsEyeOfEnderFromHand() throws Exception {
        World world = new World(6234L);
        try {
            Player player = new Player(0.0f, 120.0f, 0.0f);
            world.setPlayer(player);
            ItemStack stack = new ItemStack(ItemType.EYE_OF_ENDER, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = stack;

            assertTrue(throwEyeOfEnder(player, world, stack));
            world.updateEntities(1.0f / 20.0f);

            assertNull(player.getInventory().getItemInHand());
            assertTrue(world.getEntities().stream().anyMatch(EyeOfEnderEntity.class::isInstance));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Eye of Ender drop chance uses the player's deterministic random")
    void eyeOfEnderDropChanceUsesPlayerRandom() throws Exception {
        assertTrue(throwEyeAndReadDropFlag(0L));
        assertFalse(throwEyeAndReadDropFlag(4096L));
    }

    @Test
    @DisplayName("Eye insertion should rebuild the portal frame and activated End portal")
    void eyeInsertionRebuildsFrameAndPortalMeshes() throws Exception {
        RecordingWorld world = new RecordingWorld(6237L);
        try {
            int cx = 0;
            int y = 40;
            int cz = 0;
            buildEndPortalRing(world, cx, y, cz);
            world.setBlock(cx + 2, y, cz, BlockType.END_PORTAL_FRAME, 1);

            Player player = new Player(0.0f, 40.0f, 0.0f);
            ItemStack eye = new ItemStack(ItemType.EYE_OF_ENDER, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = eye;
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(cx + 2, y, cz),
                    new Vector3i(cx + 2, y + 1, cz),
                    Block.FACE_TOP,
                    1.0f));

            assertTrue(handleTargetedItemUse(player, world, eye, BlockType.END_PORTAL_FRAME));

            assertNull(player.getInventory().getItemInHand());
            assertTrue(player.isUsingItem());
            assertEquals(10, world.rebuildCount);
            assertTrue(world.rebuilt(2, y, 0));
            for (int x = cx - 1; x <= cx + 1; x++) {
                for (int z = cz - 1; z <= cz + 1; z++) {
                    assertSame(BlockType.END_PORTAL, world.getBlock(x, y, z));
                    assertTrue(world.rebuilt(x, y, z));
                }
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player can throw snowballs and eggs from hand with the old thrown item cue")
    void playerThrowsEggsAndSnowballsFromHand() {
        World world = new World(6236L);
        try {
            Player player = new Player(0.0f, 120.0f, 0.0f);
            world.setPlayer(player);
            ItemStack snowballs = new ItemStack(ItemType.SNOWBALL, 2);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = snowballs;

            assertTrue(player.throwThrownItemProjectile(world, snowballs, new Vector3f(1.0f, 0.0f, 0.0f)));
            assertTrue(player.throwThrownItemProjectile(world, snowballs, new Vector3f(1.0f, 0.0f, 0.0f)));

            assertNull(player.getInventory().getItemInHand());
            world.updateEntities(1.0f / 20.0f);
            List<ThrownItemEntity> thrownSnowballs = world.getEntities().stream()
                    .filter(ThrownItemEntity.class::isInstance)
                    .map(ThrownItemEntity.class::cast)
                    .filter(entity -> entity.getItemType() == ItemType.SNOWBALL)
                    .toList();
            assertEquals(2, thrownSnowballs.size());
            assertTrue(thrownSnowballs.stream().allMatch(ThrownItemEntity::isPlayerOwned));

            ItemStack egg = new ItemStack(ItemType.EGG, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = egg;
            assertTrue(player.throwThrownItemProjectile(world, egg, new Vector3f(1.0f, 0.0f, 0.0f)));
            assertNull(player.getInventory().getItemInHand());
            world.updateEntities(1.0f / 20.0f);
            assertTrue(world.getEntities().stream()
                    .filter(ThrownItemEntity.class::isInstance)
                    .map(ThrownItemEntity.class::cast)
                    .anyMatch(entity -> entity.getItemType() == ItemType.EGG && entity.isPlayerOwned()));

            var sounds = world.drainSoundEvents();
            assertEquals(3, sounds.size());
            sounds.forEach(PlayerEnderPearlInteractionTest::assertThrowSound);
        } finally {
            world.cleanup();
        }
    }

    private static boolean handleTargetedItemUse(Player player, World world, ItemStack stack,
            BlockType clickedBlock) throws Exception {
        Method method = Player.class.getDeclaredMethod("handleTargetedItemUse",
                World.class, ItemStack.class, BlockType.class);
        method.setAccessible(true);
        return (boolean) method.invoke(player, world, stack, clickedBlock);
    }

    private static boolean throwEyeOfEnder(Player player, World world, ItemStack stack) throws Exception {
        Method method = Player.class.getDeclaredMethod("throwEyeOfEnder", World.class, ItemStack.class);
        method.setAccessible(true);
        return (boolean) method.invoke(player, world, stack);
    }

    private static void setTargetBlock(Player player, Raycast.RaycastResult result) throws Exception {
        Field field = Player.class.getDeclaredField("targetBlock");
        field.setAccessible(true);
        field.set(player, result);
    }

    private static void buildEndPortalRing(World world, int centerX, int y, int centerZ) {
        for (int x = centerX - 1; x <= centerX + 1; x++) {
            world.setBlock(x, y, centerZ - 2, BlockType.END_PORTAL_FRAME,
                    0 | World.END_PORTAL_FRAME_EYE_BIT);
            world.setBlock(x, y, centerZ + 2, BlockType.END_PORTAL_FRAME,
                    2 | World.END_PORTAL_FRAME_EYE_BIT);
        }
        for (int z = centerZ - 1; z <= centerZ + 1; z++) {
            world.setBlock(centerX - 2, y, z, BlockType.END_PORTAL_FRAME,
                    3 | World.END_PORTAL_FRAME_EYE_BIT);
            world.setBlock(centerX + 2, y, z, BlockType.END_PORTAL_FRAME,
                    1 | World.END_PORTAL_FRAME_EYE_BIT);
        }
    }

    private static boolean throwEyeAndReadDropFlag(long randomSeed) throws Exception {
        World world = new World(6235L + randomSeed);
        try {
            Player player = new Player(0.0f, 120.0f, 0.0f);
            setPlayerRandomSeed(player, randomSeed);
            world.setPlayer(player);
            ItemStack stack = new ItemStack(ItemType.EYE_OF_ENDER, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = stack;

            assertTrue(throwEyeOfEnder(player, world, stack));
            world.updateEntities(1.0f / 20.0f);

            return world.getEntities().stream()
                    .filter(EyeOfEnderEntity.class::isInstance)
                    .map(EyeOfEnderEntity.class::cast)
                    .findFirst()
                    .orElseThrow()
                    .dropsItem();
        } finally {
            world.cleanup();
        }
    }

    private static void setPlayerRandomSeed(Player player, long seed) throws Exception {
        Field field = Player.class.getDeclaredField("random");
        field.setAccessible(true);
        ((Random) field.get(player)).setSeed(seed);
    }

    private static void assertThrowSound(WorldSoundEvent sound) {
        assertEquals(WorldSoundEvent.BOW, sound.soundId());
        assertEquals(0.5f, sound.volume(), 0.0001f);
        assertTrue(sound.pitch() >= 0.33f);
        assertTrue(sound.pitch() <= 0.5f);
    }

    private static final class RecordingWorld extends World {
        private int rebuildCount;
        private final Set<String> rebuilds = new HashSet<>();

        private RecordingWorld(long seed) {
            super(seed);
        }

        @Override
        public void rebuildBlockMeshesNow(int x, int y, int z) {
            rebuildCount++;
            rebuilds.add(key(x, y, z));
        }

        private boolean rebuilt(int x, int y, int z) {
            return rebuilds.contains(key(x, y, z));
        }

        private static String key(int x, int y, int z) {
            return x + "," + y + "," + z;
        }
    }
}
