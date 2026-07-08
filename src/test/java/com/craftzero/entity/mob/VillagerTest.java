package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.world.BlockType;
import com.craftzero.world.RedstoneEngine;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VillagerTest {
    @Test
    @DisplayName("Villagers should use the dedicated Release-era model type")
    void villagersUseDedicatedModelType() {
        Villager villager = new Villager(Villager.PROFESSION_FARMER);

        assertEquals(Mob.MobModelType.VILLAGER, villager.getModelType());
    }

    @Test
    @DisplayName("Villagers should flee when hurt like Release-era passive mobs")
    void villagersPanicWhenHurt() {
        World world = new World(10010L);
        try {
            makeFloor(world, -8, 8, -14, 6, 69);
            Villager villager = new Villager(Villager.PROFESSION_PRIEST);
            villager.setPosition(0.5f, 70.0f, 0.5f);
            Zombie attacker = new Zombie();
            attacker.setPosition(0.5f, 70.0f, 5.5f);
            world.replaceEntities(List.of(villager));

            assertTrue(villager.damage(1.0f, DamageSource.entity(DamageSource.Type.GENERIC, attacker)));
            world.updateEntities(1.0f / 20.0f);

            float horizontalSpeed = Math.abs(villager.getMotionX()) + Math.abs(villager.getMotionZ());
            assertTrue(horizontalSpeed > 0.001f, "hurt villager should request panic movement");
            assertTrue(villager.getMotionZ() < 0.0f, "villager should flee away from the attacker");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Villagers should flee nearby zombies before being hit")
    void villagersAvoidNearbyZombiesBeforeContact() {
        World world = new World(10014L);
        try {
            makeFloor(world, -8, 8, -14, 8, 69);
            Villager villager = new Villager(Villager.PROFESSION_FARMER);
            villager.setPosition(0.5f, 70.0f, 0.5f);
            villager.setRenderBodyYaw(0.0f);
            Zombie zombie = new Zombie();
            zombie.setPosition(0.5f, 70.0f, 5.5f);
            world.replaceEntities(List.of(villager, zombie));

            world.updateEntities(1.0f / 20.0f);

            float horizontalSpeed = Math.abs(villager.getMotionX()) + Math.abs(villager.getMotionZ());
            assertEquals(0, villager.getHurtTime(), "avoidance should not depend on damage panic");
            assertTrue(horizontalSpeed > 0.001f, "nearby zombie should request flee movement");
            assertTrue(villager.getMotionZ() < 0.0f, "villager should flee away from the zombie");
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Villager zombie avoidance should be range gated")
    void villagersIgnoreDistantZombies() {
        World world = new World(10015L);
        try {
            makeFloor(world, -8, 8, -2, 18, 69);
            Villager villager = new Villager(Villager.PROFESSION_LIBRARIAN);
            villager.random = new NoWanderRandom();
            villager.setPosition(0.5f, 70.0f, 0.5f);
            Zombie zombie = new Zombie();
            zombie.setPosition(0.5f, 70.0f, 12.5f);
            world.replaceEntities(List.of(villager, zombie));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.0f, villager.getMotionX(), 0.0001f);
            assertEquals(0.0f, villager.getMotionZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Moving villagers should open and close wooden doors")
    void movingVillagersOpenAndCloseWoodenDoors() {
        World world = new World(10011L);
        try {
            makeFloor(world, -2, 4, -2, 2, 69);
            assertTrue(world.placeDoor(1, 70, 0, BlockType.WOODEN_DOOR, 0, null));

            Villager villager = new Villager(Villager.PROFESSION_FARMER);
            villager.setPosition(0.5f, 70.0f, 0.5f);
            villager.setYaw(90.0f);
            villager.setMotion(0.08f, 0.0f, 0.0f);
            world.replaceEntities(List.of(villager));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(RedstoneEngine.DOOR_OPEN_BIT,
                    world.getBlockMetadata(1, 70, 0) & RedstoneEngine.DOOR_OPEN_BIT);
            List<WorldSoundEvent> openSounds = world.drainSoundEvents();
            assertTrue(openSounds.stream().anyMatch(sound -> WorldSoundEvent.DOOR_OPEN.equals(sound.soundId())));

            villager.setPosition(2.5f, 70.0f, 0.5f);
            villager.setMotion(0.0f, 0.0f, 0.0f);
            for (int tick = 0; tick < 21; tick++) {
                world.updateEntities(1.0f / 20.0f);
            }

            assertEquals(0, world.getBlockMetadata(1, 70, 0) & RedstoneEngine.DOOR_OPEN_BIT);
            List<WorldSoundEvent> closeSounds = world.drainSoundEvents();
            assertTrue(closeSounds.stream().anyMatch(sound -> WorldSoundEvent.DOOR_CLOSE.equals(sound.soundId())));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Villagers should not open iron doors")
    void villagersDoNotOpenIronDoors() {
        World world = new World(10012L);
        try {
            makeFloor(world, -2, 4, -2, 2, 69);
            assertTrue(world.placeDoor(1, 70, 0, BlockType.IRON_DOOR, 0, null));

            Villager villager = new Villager(Villager.PROFESSION_SMITH);
            villager.setPosition(0.5f, 70.0f, 0.5f);
            villager.setYaw(90.0f);
            villager.setMotion(0.08f, 0.0f, 0.0f);
            world.replaceEntities(List.of(villager));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0, world.getBlockMetadata(1, 70, 0) & RedstoneEngine.DOOR_OPEN_BIT);
            assertTrue(world.drainSoundEvents().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Villagers should not close already-open wooden doors they did not open")
    void villagersDoNotClaimAlreadyOpenWoodenDoors() {
        World world = new World(10013L);
        try {
            makeFloor(world, -2, 4, -2, 2, 69);
            assertTrue(world.placeDoor(1, 70, 0, BlockType.WOODEN_DOOR, 0, null));
            assertTrue(world.setWoodenDoorOpen(1, 70, 0, true));
            world.drainSoundEvents();

            Villager villager = new Villager(Villager.PROFESSION_LIBRARIAN);
            villager.setPosition(0.5f, 70.0f, 0.5f);
            villager.setYaw(90.0f);
            villager.setMotion(0.08f, 0.0f, 0.0f);
            world.replaceEntities(List.of(villager));

            for (int tick = 0; tick < 30; tick++) {
                world.updateEntities(1.0f / 20.0f);
            }

            assertEquals(RedstoneEngine.DOOR_OPEN_BIT,
                    world.getBlockMetadata(1, 70, 0) & RedstoneEngine.DOOR_OPEN_BIT);
            assertFalse(world.drainSoundEvents().stream()
                    .anyMatch(sound -> WorldSoundEvent.DOOR_CLOSE.equals(sound.soundId())));
        } finally {
            world.cleanup();
        }
    }

    private static void makeFloor(World world, int minX, int maxX, int minZ, int maxZ, int y) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlock(x, y, z, BlockType.STONE, 0);
            }
        }
    }

    private static final class NoWanderRandom extends Random {
        @Override
        public float nextFloat() {
            return 0.99f;
        }
    }
}
