package com.craftzero.entity.mob;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WolfVisualStateTest {
    @Test
    @DisplayName("Wild wolves beg for nearby held bones")
    void wildWolfBegsForHeldBone() {
        World world = new World(6270L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.getInventory().getHotbar()[0] = new ItemStack(ItemType.BONE, 1);
            world.setPlayer(player);
            Wolf wolf = new Wolf();
            wolf.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(wolf));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(wolf.isBegging());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Tamed wolves beg only for their owner holding meat")
    void tamedWolfBegsOnlyForOwnerHeldMeat() {
        World world = new World(6271L);
        try {
            Player owner = new Player(0.0f, 70.0f, 0.0f);
            owner.setPlayerName("Alex");
            owner.getInventory().getHotbar()[0] = new ItemStack(ItemType.STEAK, 1);
            Wolf wolf = new Wolf();
            wolf.setTamed(true);
            wolf.setOwnerName("Alex");
            wolf.setSitting(true);
            wolf.setPosition(1.0f, 70.0f, 0.0f);
            world.setPlayer(owner);
            world.replaceEntities(List.of(wolf));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(wolf.isBegging());

            Player stranger = new Player(0.0f, 70.0f, 0.0f);
            stranger.setPlayerName("Steve");
            stranger.getInventory().getHotbar()[0] = new ItemStack(ItemType.STEAK, 1);
            world.setPlayer(stranger);

            world.updateEntities(1.0f / 20.0f);

            assertFalse(wolf.isBegging());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Wolf tame attempts expose heart or smoke particle burst state")
    void tameAttemptsExposeParticleBurstState() {
        Wolf wolf = new Wolf();

        assertFalse(wolf.tryTameWithBone(fixedNextInt(1)));
        assertEquals(Wolf.TameParticle.SMOKE, wolf.getTameParticle());
        assertEquals(20, wolf.getTameParticleTicks());

        assertTrue(wolf.tryTameWithBone(fixedNextInt(0)));
        assertEquals(Wolf.TameParticle.HEART, wolf.getTameParticle());
        assertEquals(20, wolf.getTameParticleTicks());

        for (int i = 0; i < 20; i++) {
            wolf.tick();
        }

        assertEquals(Wolf.TameParticle.NONE, wolf.getTameParticle());
        assertEquals(0, wolf.getTameParticleTicks());
    }

    @Test
    @DisplayName("World-owned wolf tame attempts spawn heart or smoke particles")
    void tameAttemptsSpawnWorldParticles() {
        World world = new World(6272L);
        try {
            Wolf wolf = new Wolf();
            wolf.setPosition(1.0f, 70.0f, 1.0f);
            world.replaceEntities(List.of(wolf));

            assertTrue(wolf.tryTameWithBone(fixedNextInt(0)));

            assertEquals(7, world.getParticles().size());
            assertTrue(world.getParticles().stream()
                    .allMatch(particle -> particle.getType() == WorldParticle.Type.HEART));
            assertParticlesInSourceTameBox(world, wolf);

            world.getParticles().clear();
            Wolf wildWolf = new Wolf();
            wildWolf.setPosition(3.0f, 70.0f, 1.0f);
            world.replaceEntities(List.of(wildWolf));

            assertFalse(wildWolf.tryTameWithBone(fixedNextInt(1)));

            assertEquals(7, world.getParticles().size());
            assertTrue(world.getParticles().stream()
                    .allMatch(particle -> particle.getType() == WorldParticle.Type.SMOKE));
            assertParticlesInSourceTameBox(world, wildWolf);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Wet wolves shake dry with splash feedback after leaving water")
    void wetWolfShakesDryAfterLeavingWater() {
        World world = new World(6274L);
        try {
            world.setBlock(0, 69, 0, BlockType.DIRT, 0);
            world.setBlock(0, 70, 0, BlockType.WATER, 0);

            Wolf wolf = new Wolf();
            wolf.setTamed(true);
            wolf.setOwnerName("Alex");
            wolf.setSitting(true);
            wolf.setPosition(0.5f, 70.0f, 0.5f);
            world.replaceEntities(List.of(wolf));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(wolf.isWet());
            assertFalse(wolf.isShaking());

            world.setBlock(0, 70, 0, BlockType.AIR, 0);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(wolf.isWet());
            assertTrue(wolf.isShaking());
            assertEquals(0.05f, wolf.getShakeTime(), 0.0001f);
            assertTrue(world.getSoundEvents().stream()
                    .anyMatch(sound -> WorldSoundEvent.WOLF_SHAKE.equals(sound.soundId())));

            for (int i = 0; i < 10; i++) {
                world.updateEntities(1.0f / 20.0f);
            }

            assertTrue(world.getParticles().stream()
                    .anyMatch(particle -> particle.getType() == WorldParticle.Type.SPLASH));

            for (int i = 0; i < 35; i++) {
                world.updateEntities(1.0f / 20.0f);
            }

            assertFalse(wolf.isWet());
            assertFalse(wolf.isShaking());
            assertEquals(0.0f, wolf.getShakeTime(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("World particles expire after their lifetime")
    void worldParticlesExpireAfterLifetime() {
        World world = new World(6273L);
        try {
            world.spawnParticle(WorldParticle.Type.SMOKE, 0.0f, 70.0f, 0.0f,
                    0.0f, 0.0f, 0.0f, 0.2f, 2);

            world.updateParticles(1.0f / 20.0f);

            assertEquals(1, world.getParticles().size());

            world.updateParticles(1.0f / 20.0f);

            assertTrue(world.getParticles().isEmpty());
        } finally {
            world.cleanup();
        }
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

    private static void assertParticlesInSourceTameBox(World world, Wolf wolf) {
        for (WorldParticle particle : world.getParticles()) {
            assertTrue(particle.getRenderX(0.0f) >= wolf.getX() - wolf.getWidth());
            assertTrue(particle.getRenderX(0.0f) <= wolf.getX() + wolf.getWidth());
            assertTrue(particle.getRenderY(0.0f) >= wolf.getY() + 0.5f);
            assertTrue(particle.getRenderY(0.0f) <= wolf.getY() + 0.5f + wolf.getHeight());
            assertTrue(particle.getRenderZ(0.0f) >= wolf.getZ() - wolf.getWidth());
            assertTrue(particle.getRenderZ(0.0f) <= wolf.getZ() + wolf.getWidth());
        }
    }
}
