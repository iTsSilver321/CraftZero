package com.craftzero.main;

import com.craftzero.entity.mob.Sheep;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSheepInteractionTest {
    @Test
    @DisplayName("Player shears drop sheep wool and damage shears once")
    void playerShearsSheep() {
        World world = new World(6250L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            ItemStack shears = new ItemStack(ItemType.SHEARS, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = shears;
            Sheep sheep = new Sheep();
            sheep.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(sheep));

            assertTrue(player.shearSheep(sheep, shears));

            assertTrue(sheep.isSheared());
            assertEquals(ItemType.SHEARS.getMaxDurability() - 1, shears.getDurability());
            int wool = droppedCount(world, ItemType.WHITE_WOOL);
            assertTrue(wool >= 1 && wool <= 3, () -> "Expected 1-3 wool, got " + wool);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Already-sheared sheep reject repeat shearing")
    void alreadyShearedSheepRejectsRepeatShearing() {
        World world = new World(6251L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            ItemStack shears = new ItemStack(ItemType.SHEARS, 1);
            Sheep sheep = new Sheep();
            sheep.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(sheep));

            assertTrue(player.shearSheep(sheep, shears));
            int durabilityAfterFirstUse = shears.getDurability();
            int dropsAfterFirstUse = droppedCount(world, ItemType.WHITE_WOOL);

            assertFalse(player.shearSheep(sheep, shears));

            assertEquals(durabilityAfterFirstUse, shears.getDurability());
            assertEquals(dropsAfterFirstUse, droppedCount(world, ItemType.WHITE_WOOL));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Creative shearing drops wool without damaging shears")
    void creativeShearingDoesNotDamageShears() {
        World world = new World(6252L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.setGameMode(GameMode.CREATIVE);
            ItemStack shears = new ItemStack(ItemType.SHEARS, 1);
            Sheep sheep = new Sheep();
            sheep.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(sheep));

            assertTrue(player.shearSheep(sheep, shears));

            assertEquals(ItemType.SHEARS.getMaxDurability(), shears.getDurability());
            assertTrue(droppedCount(world, ItemType.WHITE_WOOL) >= 1);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player dyes sheep and colored shearing drops the dyed wool")
    void playerDyesSheepAndShearsDyedWool() {
        World world = new World(6253L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            ItemStack dye = new ItemStack(ItemType.ROSE_RED, 2);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = dye;
            Sheep sheep = new Sheep();
            sheep.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(sheep));

            assertTrue(player.dyeSheep(sheep, dye));

            assertEquals(14, sheep.getWoolColor());
            assertEquals(1, dye.getCount());

            assertTrue(player.shearSheep(sheep, new ItemStack(ItemType.SHEARS, 1)));
            int redWool = droppedCount(world, ItemType.RED_WOOL);
            assertTrue(redWool >= 1 && redWool <= 3, () -> "Expected 1-3 red wool, got " + redWool);
            assertEquals(0, droppedCount(world, ItemType.WHITE_WOOL));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Sheared or already-matching sheep reject dye without consuming it")
    void shearedAndMatchingSheepRejectDye() {
        World world = new World(6254L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            Sheep sheared = new Sheep();
            sheared.setSheared(true);
            sheared.setPosition(1.0f, 70.0f, 0.0f);
            Sheep red = new Sheep();
            red.setWoolColor(14);
            red.setPosition(2.0f, 70.0f, 0.0f);
            ItemStack shearedDye = new ItemStack(ItemType.LAPIS_LAZULI, 1);
            ItemStack matchingDye = new ItemStack(ItemType.ROSE_RED, 1);
            world.replaceEntities(List.of(sheared, red));

            assertFalse(player.dyeSheep(sheared, shearedDye));
            assertFalse(player.dyeSheep(red, matchingDye));

            assertEquals(0, sheared.getWoolColor());
            assertEquals(1, shearedDye.getCount());
            assertEquals(14, red.getWoolColor());
            assertEquals(1, matchingDye.getCount());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Creative dyeing recolors sheep without consuming dye")
    void creativeDyeingDoesNotConsumeDye() {
        World world = new World(6255L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.setGameMode(GameMode.CREATIVE);
            ItemStack dye = new ItemStack(ItemType.LAPIS_LAZULI, 4);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = dye;
            Sheep sheep = new Sheep();
            sheep.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(sheep));

            assertTrue(player.dyeSheep(sheep, dye));

            assertEquals(11, sheep.getWoolColor());
            assertEquals(4, dye.getCount());
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
