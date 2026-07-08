package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.ArrowEntity;
import com.craftzero.main.Player;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZombiePigmanTest {

    @Test
    @DisplayName("Zombie Pigmen should not anger from incidental damage")
    void incidentalDamageDoesNotAngerPigmen() {
        ZombiePigman pigman = new ZombiePigman();
        float beforeHealth = pigman.getHealth();

        assertTrue(pigman.damage(1.0f, DamageSource.generic()));

        assertEquals(beforeHealth - 1.0f, pigman.getHealth(), 0.001f);
        assertEquals(0, pigman.getAngerTicks());
    }

    @Test
    @DisplayName("Zombie Pigmen should anger and alert nearby pigmen from player attacks")
    void playerDamageAngersNearbyPigmen() {
        World world = new World(9010L);
        try {
            ZombiePigman attacked = new ZombiePigman();
            attacked.setPosition(0.5f, 70.0f, 0.5f);
            ZombiePigman nearby = new ZombiePigman();
            nearby.setPosition(8.5f, 70.0f, 0.5f);
            ZombiePigman far = new ZombiePigman();
            far.setPosition(40.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(attacked, nearby, far));

            assertTrue(attacked.damage(1.0f, DamageSource.point(DamageSource.Type.PLAYER_ATTACK,
                    0.5f, 71.0f, -1.5f, 0.0f, 0.0f)));

            assertTrue(attacked.getAngerTicks() > 0);
            assertTrue(nearby.getAngerTicks() > 0);
            assertEquals(0, far.getAngerTicks());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player-provoked Zombie Pigmen should immediately chase the provoking player")
    void playerDamageImmediatelySeedsChaseTargetsForAlertedPigmen() {
        World world = new World(9015L);
        try {
            Player player = new Player(12.5f, 70.0f, -4.5f);
            world.setPlayer(player);
            ZombiePigman attacked = new ZombiePigman();
            attacked.setPosition(0.5f, 70.0f, 0.5f);
            ZombiePigman nearby = new ZombiePigman();
            nearby.setPosition(8.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(attacked, nearby));

            assertTrue(attacked.damage(1.0f, DamageSource.playerAttack(
                    player.getPosition().x, player.getPosition().y + 1.6f, player.getPosition().z, 0)));

            assertChasingPlayer(attacked, player);
            assertChasingPlayer(nearby, player);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Zombie Pigman anger should count down and play the delayed angry cue")
    void angerCountsDownAndPlaysDelayedAngrySound() {
        World world = new World(9020L);
        try {
            ZombiePigman pigman = new ZombiePigman();
            pigman.random = fixedRandom(new int[] { 0, 1, 999 }, 0.7f, 0.5f);
            pigman.setPosition(2.5f, 70.0f, 2.5f);
            world.replaceEntities(List.of(pigman));

            assertTrue(pigman.damage(1.0f, DamageSource.point(DamageSource.Type.PLAYER_ATTACK,
                    2.5f, 71.0f, 0.5f, 0.0f, 0.0f)));

            assertEquals(400, pigman.getAngerTicks());
            assertEquals(1, pigman.getAngerSoundDelay());
            assertTrue(world.drainSoundEvents().stream()
                    .anyMatch(sound -> WorldSoundEvent.ZOMBIE_PIGMAN_HURT.equals(sound.soundId())));

            world.updateEntities(1.0f / 20.0f);

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            WorldSoundEvent angry = sounds.get(0);
            assertEquals(WorldSoundEvent.ZOMBIE_PIGMAN_ANGRY, angry.soundId());
            assertEquals(2.0f, angry.volume(), 0.0001f);
            assertEquals(1.04f, angry.pitch(), 0.0001f);
            assertEquals(399, pigman.getAngerTicks(),
                    "Release-era Zombie Pigmen forgive after their finite anger timer expires");
            assertEquals(0, pigman.getAngerSoundDelay());

            for (int i = 0; i < 5; i++) {
                world.updateEntities(1.0f / 20.0f);
            }

            assertEquals(394, pigman.getAngerTicks());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Zombie Pigmen should calm down when anger countdown expires")
    void angerCountdownClearsChaseWhenExpired() {
        ZombiePigman pigman = new ZombiePigman();
        pigman.random = fixedRandom(new int[] { 999, 999 }, 0.5f);
        pigman.setAngerTicks(2);
        pigman.getAI().setMoveTarget(10.0f, -4.0f);

        pigman.tick();

        assertEquals(1, pigman.getAngerTicks());
        assertTrue(pigman.getAI().hasMoveTarget());

        pigman.tick();

        assertEquals(0, pigman.getAngerTicks());
        assertEquals(0, pigman.getAngerSoundDelay());
        assertFalse(pigman.getAI().hasMoveTarget());
    }

    @Test
    @DisplayName("Zombie Pigmen should anger from player-owned arrows only")
    void onlyPlayerOwnedArrowsAngerPigmen() {
        ZombiePigman playerShot = new ZombiePigman();
        ArrowEntity playerArrow = new ArrowEntity(0.0f, 70.0f, 0.0f,
                0.0f, 0.0f, 0.0f, null, true, 4.0f);
        assertTrue(playerShot.damage(1.0f, DamageSource.entity(DamageSource.Type.ARROW, playerArrow)));
        assertTrue(playerShot.getAngerTicks() > 0);

        ZombiePigman mobShot = new ZombiePigman();
        ArrowEntity mobArrow = new ArrowEntity(0.0f, 70.0f, 0.0f,
                0.0f, 0.0f, 0.0f, null, false, 4.0f);
        assertTrue(mobShot.damage(1.0f, DamageSource.entity(DamageSource.Type.ARROW, mobArrow)));
        assertEquals(0, mobShot.getAngerTicks());
    }

    private static Random fixedRandom(int[] ints, float... floats) {
        return new Random() {
            private int intIndex;
            private int floatIndex;

            @Override
            public int nextInt(int bound) {
                if (intIndex >= ints.length) {
                    return bound - 1;
                }
                return Math.floorMod(ints[intIndex++], bound);
            }

            @Override
            public float nextFloat() {
                if (floats.length == 0) {
                    return 0.5f;
                }
                return floats[floatIndex++ % floats.length];
            }
        };
    }

    private static void assertChasingPlayer(ZombiePigman pigman, Player player) {
        assertTrue(pigman.getAI().hasMoveTarget());
        assertEquals(player.getPosition().x, pigman.getAI().getTargetX(), 0.0001f);
        assertEquals(player.getPosition().z, pigman.getAI().getTargetZ(), 0.0001f);
    }
}
