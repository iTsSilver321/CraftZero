package com.craftzero.main;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.Entity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.mob.Wolf;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.physics.Raycast;
import com.craftzero.world.World;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerWolfInteractionTest {
    @Test
    @DisplayName("Bones tame wolves on the Release-era one-in-three success roll")
    void boneCanTameWolfAndConsumesOneBone() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        player.setPlayerName("Alex");
        Wolf wolf = new Wolf();
        ItemStack bones = new ItemStack(ItemType.BONE, 2);

        assertTrue(player.tameWolf(wolf, bones, fixedNextInt(0)));

        assertTrue(wolf.isTamed());
        assertFalse(wolf.isAngry());
        assertTrue(wolf.isSitting());
        assertEquals("Alex", wolf.getOwnerName());
        assertEquals(20.0f, wolf.getMaxHealth(), 0.001f);
        assertEquals(20.0f, wolf.getHealth(), 0.001f);
        assertEquals(1, bones.getCount());
        assertTrue(player.isUsingItem());
    }

    @Test
    @DisplayName("Failed wolf taming attempts still consume one bone")
    void failedBoneTamingAttemptConsumesBone() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        Wolf wolf = new Wolf();
        ItemStack bones = new ItemStack(ItemType.BONE, 2);

        assertTrue(player.tameWolf(wolf, bones, fixedNextInt(1)));

        assertFalse(wolf.isTamed());
        assertEquals(1, bones.getCount());
        assertTrue(player.isUsingItem());
    }

    @Test
    @DisplayName("Tamed or angry wolves reject bone taming attempts")
    void unavailableWolvesRejectBones() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        Wolf tamed = new Wolf();
        own(tamed, player);
        ItemStack tamedBones = new ItemStack(ItemType.BONE, 2);

        assertFalse(player.tameWolf(tamed, tamedBones, fixedNextInt(0)));
        assertEquals(2, tamedBones.getCount());

        Wolf angry = new Wolf();
        angry.setAngry(true);
        ItemStack angryBones = new ItemStack(ItemType.BONE, 2);

        assertFalse(player.tameWolf(angry, angryBones, fixedNextInt(0)));
        assertEquals(2, angryBones.getCount());
    }

    @Test
    @DisplayName("Meat heals damaged tamed wolves and consumes one item")
    void meatHealsTamedWolf() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        Wolf wolf = new Wolf();
        own(wolf, player);
        wolf.setHealth(10.0f);
        ItemStack steak = new ItemStack(ItemType.STEAK, 2);

        assertTrue(player.feedWolf(wolf, steak));

        assertEquals(18.0f, wolf.getHealth(), 0.001f);
        assertEquals(1, steak.getCount());
        assertTrue(player.isUsingItem());
    }

    @Test
    @DisplayName("Full-health tamed wolves reject meat healing without consuming it")
    void fullHealthWolfRejectsMeatHealing() {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        Wolf wolf = new Wolf();
        own(wolf, player);
        wolf.setHealth(wolf.getMaxHealth());
        ItemStack steak = new ItemStack(ItemType.STEAK, 2);

        assertFalse(player.feedWolf(wolf, steak));

        assertEquals(20.0f, wolf.getHealth(), 0.001f);
        assertEquals(2, steak.getCount());
        assertFalse(player.isUsingItem());
    }

    @Test
    @DisplayName("Right-clicking a tamed wolf toggles sitting")
    void rightClickTamedWolfTogglesSitting() throws Exception {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        Wolf wolf = new Wolf();
        own(wolf, player);
        wolf.setPosition(1.0f, 70.0f, 0.0f);

        assertTrue(handleEntityUse(player, null, wolf));
        assertTrue(wolf.isSitting());

        assertTrue(handleEntityUse(player, null, wolf));
        assertFalse(wolf.isSitting());
    }

    @Test
    @DisplayName("Non-owners cannot toggle tamed wolf sitting")
    void nonOwnerCannotToggleTamedWolfSitting() throws Exception {
        Player owner = new Player(0.0f, 70.0f, 0.0f);
        owner.setPlayerName("Alex");
        Player stranger = new Player(0.0f, 70.0f, 0.0f);
        stranger.setPlayerName("Steve");
        Wolf wolf = new Wolf();
        own(wolf, owner);
        wolf.setPosition(1.0f, 70.0f, 0.0f);

        assertFalse(handleEntityUse(stranger, null, wolf));

        assertFalse(wolf.isSitting());
    }

    @Test
    @DisplayName("Right-clicking a wolf with a bone routes through entity use")
    void handleEntityUseTamesWolfWithBone() throws Exception {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        Wolf wolf = new Wolf();
        wolf.setPosition(1.0f, 70.0f, 0.0f);
        ItemStack bones = new ItemStack(ItemType.BONE, 2);

        assertTrue(handleEntityUse(player, bones, wolf));

        assertEquals(1, bones.getCount());
    }

    @Test
    @DisplayName("Player attacks alert standing tamed wolves to the target")
    void playerAttackAlertsStandingTamedWolves() throws Exception {
        World world = new World(6264L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.setWorld(world);
            world.setPlayer(player);
            Wolf standing = new Wolf();
            own(standing, player);
            standing.setSitting(false);
            standing.setPosition(1.0f, 70.0f, 0.0f);
            Wolf sitting = new Wolf();
            own(sitting, player);
            sitting.setSitting(true);
            sitting.setPosition(1.0f, 70.0f, 1.0f);
            Wolf otherOwner = new Wolf();
            otherOwner.setTamed(true);
            otherOwner.setOwnerName("Steve");
            otherOwner.setPosition(1.0f, 70.0f, -1.0f);
            Zombie zombie = new Zombie();
            zombie.setPosition(2.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(standing, sitting, otherOwner, zombie));

            attackEntity(player, zombie);

            assertSame(zombie, standing.getAssistTarget());
            assertNull(sitting.getAssistTarget());
            assertNull(otherOwner.getAssistTarget());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player damage alerts standing tamed wolves to the attacker")
    void playerDamageAlertsStandingTamedWolves() {
        World world = new World(6265L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.setWorld(world);
            world.setPlayer(player);
            player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
            Wolf wolf = new Wolf();
            own(wolf, player);
            wolf.setSitting(false);
            wolf.setPosition(1.0f, 70.0f, 0.0f);
            Zombie zombie = new Zombie();
            zombie.setPosition(2.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(wolf, zombie));

            assertTrue(player.hurt(CombatRules.EASY_ZOMBIE_DAMAGE,
                    DamageSource.entity(DamageSource.Type.MOB_MELEE, zombie)));

            assertSame(zombie, wolf.getAssistTarget());
        } finally {
            world.cleanup();
        }
    }

    private static boolean handleEntityUse(Player player, ItemStack stack, Entity entity) throws Exception {
        Method method = Player.class.getDeclaredMethod("handleEntityUse",
                ItemStack.class, Raycast.EntityRaycastResult.class, Raycast.RaycastResult.class);
        method.setAccessible(true);
        Raycast.EntityRaycastResult entityHit = new Raycast.EntityRaycastResult(true, entity, 1.0f,
                new Vector3f(entity.getX(), entity.getY(), entity.getZ()));
        return (boolean) method.invoke(player, stack, entityHit, Raycast.RaycastResult.miss());
    }

    private static void attackEntity(Player player, LivingEntity target) throws Exception {
        Method method = Player.class.getDeclaredMethod("attackEntity", LivingEntity.class);
        method.setAccessible(true);
        method.invoke(player, target);
    }

    private static void own(Wolf wolf, Player player) {
        wolf.setTamed(true);
        wolf.setOwnerName(player.getPlayerName());
    }

    private static Random fixedNextInt(int value) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                if (value < 0 || value >= bound) {
                    throw new IllegalArgumentException("Fixed random value " + value + " outside bound " + bound);
                }
                return value;
            }
        };
    }
}
