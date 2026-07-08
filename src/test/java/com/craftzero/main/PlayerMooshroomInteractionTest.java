package com.craftzero.main;

import com.craftzero.entity.mob.Cow;
import com.craftzero.entity.mob.Mooshroom;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerMooshroomInteractionTest {
    @Test
    @DisplayName("Mooshroom bowl use should replace one bowl with mushroom stew")
    void bowlUseOnMooshroomCreatesStew() {
        World world = new World(6260L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            Mooshroom mooshroom = new Mooshroom();
            mooshroom.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(mooshroom));
            ItemStack bowl = new ItemStack(ItemType.BOWL, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = bowl;

            assertTrue(player.fillBowlFromMooshroom(mooshroom, bowl));

            ItemStack held = player.getInventory().getItemInHand();
            assertSame(ItemType.MUSHROOM_STEW, held.getType());
            assertEquals(1, held.getCount());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Cow bucket use should replace one empty bucket with milk")
    void bucketUseOnCowCreatesMilkBucket() {
        World world = new World(6261L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            Cow cow = new Cow();
            cow.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(cow));
            ItemStack bucket = new ItemStack(ItemType.BUCKET, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = bucket;

            assertTrue(player.milkCow(cow, bucket));

            ItemStack held = player.getInventory().getItemInHand();
            assertSame(ItemType.MILK_BUCKET, held.getType());
            assertEquals(1, held.getCount());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mooshroom shearing should turn it into a cow and drop five red mushrooms")
    void shearingMooshroomConvertsToCowAndDropsMushrooms() {
        World world = new World(6262L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            Mooshroom mooshroom = new Mooshroom();
            mooshroom.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(mooshroom));
            ItemStack shears = new ItemStack(ItemType.SHEARS, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = shears;

            assertTrue(player.shearMooshroom(mooshroom, shears));
            world.updateEntities(0.0f);

            assertTrue(mooshroom.isRemoved());
            assertEquals(ItemType.SHEARS.getMaxDurability() - 1, shears.getDurability());
            assertEquals(5, droppedCount(world, ItemType.RED_MUSHROOM));
            assertTrue(world.getEntities().stream().anyMatch(entity -> entity instanceof Cow && !(entity instanceof Mooshroom)));
            WorldParticle conversion = world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.LARGE_EXPLOSION)
                    .findFirst()
                    .orElseThrow();
            assertEquals(1.0f, conversion.getRenderX(0.0f), 0.0001f);
            assertEquals(70.7f, conversion.getRenderY(0.0f), 0.0001f);
            assertEquals(0.0f, conversion.getRenderZ(0.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 baby cows and mooshrooms should reject milk, bowl, and shear interactions")
    void babyCowAndMooshroomRejectAdultContainerInteractions() {
        World world = new World(6263L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);

            Cow cow = new Cow();
            cow.setGrowingAge(Cow.BABY_GROWING_AGE);
            ItemStack bucket = new ItemStack(ItemType.BUCKET, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = bucket;

            assertFalse(player.milkCow(cow, bucket));
            assertSame(ItemType.BUCKET, player.getInventory().getItemInHand().getType());
            assertEquals(1, player.getInventory().getItemInHand().getCount());

            Mooshroom mooshroom = new Mooshroom();
            mooshroom.setGrowingAge(Mooshroom.BABY_GROWING_AGE);
            mooshroom.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(mooshroom));

            ItemStack bowl = new ItemStack(ItemType.BOWL, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = bowl;
            assertFalse(player.fillBowlFromMooshroom(mooshroom, bowl));
            assertSame(ItemType.BOWL, player.getInventory().getItemInHand().getType());
            assertEquals(1, player.getInventory().getItemInHand().getCount());

            ItemStack shears = new ItemStack(ItemType.SHEARS, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = shears;
            assertFalse(player.shearMooshroom(mooshroom, shears));
            assertFalse(mooshroom.isRemoved());
            assertEquals(ItemType.SHEARS.getMaxDurability(), shears.getDurability());
            assertEquals(0, droppedCount(world, ItemType.RED_MUSHROOM));
            assertEquals(1, world.getEntities().stream().filter(entity -> entity instanceof Mooshroom).count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Sheared mooshrooms should transfer their current health to the replacement cow")
    void shearedMooshroomTransfersCurrentHealthToCow() {
        World world = new World(6264L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            Mooshroom mooshroom = new Mooshroom();
            mooshroom.setPosition(1.0f, 70.0f, 0.0f);
            mooshroom.setHealth(4.0f);
            world.replaceEntities(List.of(mooshroom));
            ItemStack shears = new ItemStack(ItemType.SHEARS, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = shears;

            assertTrue(player.shearMooshroom(mooshroom, shears));
            world.updateEntities(0.0f);

            Cow cow = world.getEntities().stream()
                    .filter(entity -> entity instanceof Cow && !(entity instanceof Mooshroom))
                    .map(entity -> (Cow) entity)
                    .findFirst()
                    .orElseThrow();
            assertEquals(4.0f, cow.getHealth(), 0.001f);
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
}
