package com.craftzero.main;

import com.craftzero.entity.EnderPearlEntity;
import com.craftzero.entity.SplashPotionEntity;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionType;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerConsumableUseSoundTest {
    @Test
    @DisplayName("Food use waits for the Release 1.0 held duration before feeding")
    void foodUseCompletesAfterHeldDuration() throws Exception {
        World world = new World(6273L);
        try {
            Player player = new Player(0.0f, 80.0f, 0.0f);
            player.getStats().restore(20.0f, 16.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
            ItemStack apples = new ItemStack(ItemType.APPLE, 2);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = apples;

            assertTrue(beginHeldConsumableUse(player, world, apples));
            tickHeldConsumableUse(player, world, 0.8f);

            assertEquals(2, apples.getCount());
            assertEquals(16.0f, player.getStats().getHunger(), 0.0001f);
            assertTrue(player.isUsingItem());

            tickHeldConsumableUse(player, world, 0.8f);

            assertFalse(player.isUsingItem());
            assertEquals(1, apples.getCount());
            assertEquals(PlayerStats.MAX_HUNGER, player.getStats().getHunger(), 0.0001f);
            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertTrue(sounds.stream().anyMatch(sound -> WorldSoundEvent.EAT.equals(sound.soundId())));
            assertConsumableSound(WorldSoundEvent.BURP, sounds.get(sounds.size() - 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Released food use cancels without feeding or consuming")
    void releasedFoodUseCancelsBeforeCompletion() throws Exception {
        World world = new World(6278L);
        try {
            Player player = new Player(0.0f, 80.0f, 0.0f);
            player.getStats().restore(20.0f, 16.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
            ItemStack apples = new ItemStack(ItemType.APPLE, 2);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = apples;

            assertTrue(beginHeldConsumableUse(player, world, apples));
            tickHeldConsumableUse(player, world, 0.4f);
            updateUse(player, world, 0.1f);

            assertFalse(player.isUsingItem());
            assertEquals(2, apples.getCount());
            assertEquals(16.0f, player.getStats().getHunger(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Food use should emit Release-style item icon crack crumbs")
    void foodUseEmitsItemIconCrackCrumbs() throws Exception {
        World world = new World(6283L);
        try {
            Player player = new Player(0.0f, 80.0f, 0.0f);
            player.getStats().restore(20.0f, 16.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
            ItemStack apples = new ItemStack(ItemType.APPLE, 2);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = apples;

            assertTrue(beginHeldConsumableUse(player, world, apples));
            assertEquals(1, itemCrackCount(world, ItemType.APPLE));

            tickHeldConsumableUse(player, world, 1.6f);

            assertEquals(17, itemCrackCount(world, ItemType.APPLE));
            assertTrue(world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.ITEM_CRACK)
                    .allMatch(particle -> particle.getItemParticleType() == ItemType.APPLE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Drinkable consumables should not emit food crumb particles")
    void drinkableConsumablesDoNotEmitFoodCrumbs() throws Exception {
        World world = new World(6284L);
        try {
            Player player = new Player(0.0f, 80.0f, 0.0f);
            ItemStack milk = new ItemStack(ItemType.MILK_BUCKET, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = milk;

            assertTrue(beginHeldConsumableUse(player, world, milk));
            tickHeldConsumableUse(player, world, 1.6f);

            assertEquals(0, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.ITEM_CRACK)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Held food use should not restart while already active")
    void heldFoodUseDoesNotRestartWhileActive() throws Exception {
        World world = new World(6282L);
        try {
            Player player = new Player(0.0f, 80.0f, 0.0f);
            player.getStats().restore(20.0f, 16.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
            ItemStack apples = new ItemStack(ItemType.APPLE, 2);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = apples;

            assertTrue(beginHeldConsumableUse(player, world, apples));
            tickHeldConsumableUse(player, world, 1.0f);
            assertFalse(beginHeldConsumableUse(player, world, apples));
            tickHeldConsumableUse(player, world, 0.6f);

            assertFalse(player.isUsingItem());
            assertEquals(1, apples.getCount());
            assertEquals(PlayerStats.MAX_HUNGER, player.getStats().getHunger(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 food values should include old saturation and side effects")
    void foodValuesIncludeReleaseOneSaturationAndSideEffects() throws Exception {
        World world = new World(6280L);
        try {
            Player cookiePlayer = foodReadyPlayer(ItemType.COOKIE, 10.0f, 0.0f);
            consumeHeldConsumable(cookiePlayer, world);
            assertEquals(12.0f, cookiePlayer.getStats().getHunger(), 0.0001f);
            assertEquals(0.4f, cookiePlayer.getStats().getSaturation(), 0.0001f);

            Player rawFishPlayer = foodReadyPlayer(ItemType.RAW_FISH, 10.0f, 0.0f);
            consumeHeldConsumable(rawFishPlayer, world);
            assertEquals(12.0f, rawFishPlayer.getStats().getHunger(), 0.0001f);
            assertEquals(0.4f, rawFishPlayer.getStats().getSaturation(), 0.0001f);

            Player spiderEyePlayer = foodReadyPlayer(ItemType.SPIDER_EYE, 10.0f, 0.0f);
            consumeHeldConsumable(spiderEyePlayer, world);
            assertEquals(12.0f, spiderEyePlayer.getStats().getHunger(), 0.0001f);
            assertEquals(3.2f, spiderEyePlayer.getStats().getSaturation(), 0.0001f);
            assertTrue(spiderEyePlayer.getStats().hasEffect(StatusEffectType.POISON));

            Player goldenApplePlayer = foodReadyPlayer(ItemType.GOLDEN_APPLE, PlayerStats.MAX_HUNGER, 0.0f);
            consumeHeldConsumable(goldenApplePlayer, world);
            assertEquals(PlayerStats.MAX_HUNGER, goldenApplePlayer.getStats().getHunger(), 0.0001f);
            assertEquals(PlayerStats.MAX_SATURATION, goldenApplePlayer.getStats().getSaturation(), 0.0001f);
            assertTrue(goldenApplePlayer.getStats().hasEffect(StatusEffectType.REGENERATION));

            Player rawChickenPlayer = foodReadyPlayer(ItemType.RAW_CHICKEN, 10.0f, 0.0f);
            setPlayerRandomSeed(rawChickenPlayer, 4096L);
            consumeHeldConsumable(rawChickenPlayer, world);
            assertEquals(12.0f, rawChickenPlayer.getStats().getHunger(), 0.0001f);
            assertEquals(1.2f, rawChickenPlayer.getStats().getSaturation(), 0.0001f);
            assertTrue(rawChickenPlayer.getStats().hasEffect(StatusEffectType.HUNGER));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mushroom stew should preserve stacked leftovers and return one bowl")
    void mushroomStewReturnsBowlWithoutDeletingStackedLeftovers() throws Exception {
        World world = new World(6281L);
        try {
            Player player = new Player(0.0f, 80.0f, 0.0f);
            player.getStats().restore(20.0f, 10.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                    new ItemStack(ItemType.MUSHROOM_STEW, 2);

            consumeHeldConsumable(player, world);

            assertSame(ItemType.MUSHROOM_STEW, player.getInventory().getItemInHand().getType());
            assertEquals(1, player.getInventory().getItemInHand().getCount());
            assertTrue(containsStack(player, ItemType.BOWL, 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Milk clears effects after held duration and leaves a bucket")
    void milkUseCompletesAfterHeldDuration() throws Exception {
        World world = new World(6274L);
        try {
            Player player = new Player(0.0f, 80.0f, 0.0f);
            player.getStats().addEffect(new StatusEffectInstance(StatusEffectType.POISON, 200, 0));
            ItemStack milk = new ItemStack(ItemType.MILK_BUCKET, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = milk;

            assertTrue(beginHeldConsumableUse(player, world, milk));
            tickHeldConsumableUse(player, world, 1.59f);

            assertTrue(player.getStats().hasEffect(StatusEffectType.POISON));
            assertSame(ItemType.MILK_BUCKET, player.getInventory().getItemInHand().getType());

            tickHeldConsumableUse(player, world, 0.01f);

            assertFalse(player.getStats().hasEffect(StatusEffectType.POISON));
            assertSame(ItemType.BUCKET, player.getInventory().getItemInHand().getType());
            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertTrue(sounds.stream().allMatch(sound -> WorldSoundEvent.DRINK.equals(sound.soundId())));
            assertConsumableSound(WorldSoundEvent.DRINK, sounds.get(sounds.size() - 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Drinkable potions complete after held duration and return the bottle")
    void drinkablePotionCompletesAfterHeldDuration() throws Exception {
        World world = new World(6275L);
        try {
            Player player = new Player(0.0f, 80.0f, 0.0f);
            ItemStack potion = new ItemStack(ItemType.POTION, 1);
            potion.setPotionData(PotionData.water());
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = potion;

            assertTrue(beginHeldConsumableUse(player, world, potion));
            tickHeldConsumableUse(player, world, 1.6f);

            assertSame(ItemType.GLASS_BOTTLE, player.getInventory().getItemInHand().getType());
            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertTrue(sounds.stream().allMatch(sound -> WorldSoundEvent.DRINK.equals(sound.soundId())));
            assertConsumableSound(WorldSoundEvent.DRINK, sounds.get(sounds.size() - 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Splash potions use the Release-era thrown item cue")
    void splashPotionEmitsThrowSound() throws Exception {
        World world = new World(6276L);
        try {
            Player player = new Player(0.0f, 80.0f, 0.0f);
            ItemStack potion = new ItemStack(ItemType.POTION, 1);
            potion.setPotionData(new PotionData(PotionType.POISON, true, false, false));
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = potion;

            assertTrue(useImmediate(player, world, potion));

            assertNull(player.getInventory().getItemInHand());
            assertTrue(world.hasEntityOfType(SplashPotionEntity.class));
            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertThrowSound(sounds.get(0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ender pearl throws use the Release-era thrown item cue")
    void enderPearlEmitsThrowSound() {
        World world = new World(6277L);
        try {
            Player player = new Player(0.0f, 80.0f, 0.0f);
            world.setPlayer(player);
            ItemStack pearl = new ItemStack(ItemType.ENDER_PEARL, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = pearl;

            assertTrue(player.throwEnderPearl(world, pearl, new Vector3f(1.0f, 0.0f, 0.0f)));

            assertNull(player.getInventory().getItemInHand());
            world.updateEntities(1.0f / 20.0f);
            assertTrue(world.getEntities().stream().anyMatch(EnderPearlEntity.class::isInstance));
            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertThrowSound(sounds.get(0));
        } finally {
            world.cleanup();
        }
    }

    private static boolean useImmediate(Player player, World world, ItemStack stack) throws Exception {
        Method method = Player.class.getDeclaredMethod("handleImmediateItemUse",
                World.class, ItemStack.class, Vector3f.class, Vector3f.class);
        method.setAccessible(true);
        return (boolean) method.invoke(player, world, stack,
                new Vector3f(0.0f, 0.0f, 0.0f),
                new Vector3f(1.0f, 0.0f, 0.0f));
    }

    private static boolean beginHeldConsumableUse(Player player, World world, ItemStack stack) throws Exception {
        Method method = Player.class.getDeclaredMethod("beginHeldConsumableUse", World.class, ItemStack.class);
        method.setAccessible(true);
        return (boolean) method.invoke(player, world, stack);
    }

    private static Player foodReadyPlayer(ItemType type, float hunger, float saturation) {
        Player player = new Player(0.0f, 80.0f, 0.0f);
        player.getStats().restore(20.0f, hunger, saturation, PlayerStats.MAX_AIR_SECONDS);
        player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = new ItemStack(type, 1);
        return player;
    }

    private static boolean containsStack(Player player, ItemType type, int count) {
        for (ItemStack stack : player.getInventory().getHotbar()) {
            if (stack != null && stack.getType() == type && stack.getCount() >= count) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().getMainInventory()) {
            if (stack != null && stack.getType() == type && stack.getCount() >= count) {
                return true;
            }
        }
        return false;
    }

    private static long itemCrackCount(World world, ItemType type) {
        return world.getParticles().stream()
                .filter(particle -> particle.getType() == WorldParticle.Type.ITEM_CRACK)
                .filter(particle -> particle.getItemParticleType() == type)
                .count();
    }

    private static void consumeHeldConsumable(Player player, World world) throws Exception {
        ItemStack stack = player.getInventory().getItemInHand();
        assertTrue(beginHeldConsumableUse(player, world, stack));
        tickHeldConsumableUse(player, world, 1.6f);
        world.drainSoundEvents();
    }

    private static void setPlayerRandomSeed(Player player, long seed) throws Exception {
        Field field = Player.class.getDeclaredField("random");
        field.setAccessible(true);
        ((java.util.Random) field.get(player)).setSeed(seed);
    }

    private static void tickHeldConsumableUse(Player player, World world, float seconds) throws Exception {
        setConsumableHeldThisFrame(player, true);
        updateUse(player, world, seconds);
    }

    private static void updateUse(Player player, World world, float seconds) throws Exception {
        Method method = Player.class.getDeclaredMethod("updateUse", float.class, World.class);
        method.setAccessible(true);
        method.invoke(player, seconds, world);
    }

    private static void setConsumableHeldThisFrame(Player player, boolean held) throws Exception {
        Field field = Player.class.getDeclaredField("consumableUseHeldThisFrame");
        field.setAccessible(true);
        field.setBoolean(player, held);
    }

    private static void assertConsumableSound(String soundId, WorldSoundEvent sound) {
        assertEquals(soundId, sound.soundId());
        assertEquals(0.5f, sound.volume(), 0.0001f);
        assertTrue(sound.pitch() >= 0.9f);
        assertTrue(sound.pitch() <= 1.0f);
    }

    private static void assertThrowSound(WorldSoundEvent sound) {
        assertEquals(WorldSoundEvent.BOW, sound.soundId());
        assertEquals(0.5f, sound.volume(), 0.0001f);
        assertTrue(sound.pitch() >= 0.33f);
        assertTrue(sound.pitch() <= 0.5f);
    }
}
