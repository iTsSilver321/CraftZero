package com.craftzero.entity.mob;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.ExperienceOrbEntity;
import com.craftzero.entity.ai.FollowChildGoal;
import com.craftzero.entity.ai.FollowMateGoal;
import com.craftzero.entity.ai.FollowParentGoal;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MobBreedingTest {
    @Test
    @DisplayName("Release 1.0 wheat-fed pigs should court before breeding into a baby")
    void wheatFedPigsBreed() {
        World world = new World(6283L);
        try {
            Pig first = new Pig();
            Pig second = new Pig();
            first.setRenderBodyYaw(123.0f);
            first.setPitch(17.0f);
            first.setPosition(0.0f, 70.0f, 0.0f);
            second.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(first, second));

            assertTrue(first.feedBreedingItem(ItemType.WHEAT));
            assertTrue(second.feedBreedingItem(ItemType.WHEAT));

            updateEntities(world, Mob.BREEDING_SPAWN_DELAY_TICKS - 1);
            assertEquals(2, pigs(world).size());
            assertFalse(world.getEntities().stream().anyMatch(ExperienceOrbEntity.class::isInstance));

            long heartsBeforeBirth = heartParticleCount(world);
            updateEntities(world, 2);

            List<Pig> pigs = pigs(world);
            assertEquals(3, pigs.size());
            Pig child = pigs.stream()
                    .filter(Pig::isBaby)
                    .findFirst()
                    .orElseThrow();
            assertEquals(first.getX(), child.getX(), 0.0001f);
            assertEquals(first.getZ(), child.getZ(), 0.0001f);
            assertEquals(first.getYaw(), child.getYaw(), 0.0001f);
            assertEquals(first.getPitch(), child.getPitch(), 0.0001f);
            assertTrue(first.getGrowingAge() > 0);
            assertTrue(second.getGrowingAge() > 0);
            assertFalse(first.isInLove());
            assertFalse(second.isInLove());
            assertFalse(world.getEntities().stream().anyMatch(ExperienceOrbEntity.class::isInstance));
            assertTrue(heartParticleCount(world) - heartsBeforeBirth >= Mob.BREEDING_HEART_BURST_COUNT);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 wheat feeding should emit the immediate heart burst")
    void wheatFeedingEmitsImmediateHeartBurst() {
        World world = new World(6294L);
        try {
            Pig pig = new Pig();
            pig.setPosition(0.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(pig));

            assertTrue(pig.feedBreedingItem(ItemType.WHEAT));

            assertEquals(Mob.BREEDING_HEART_BURST_COUNT, heartParticleCount(world));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 repeated wheat use should refresh love mode and heart feedback")
    void repeatedWheatUseRefreshesLoveModeAndHeartFeedback() {
        World world = new World(6298L);
        try {
            Pig pig = new Pig();
            pig.setPosition(0.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(pig));

            assertTrue(pig.feedBreedingItem(ItemType.WHEAT));
            assertEquals(Mob.BREEDING_HEART_BURST_COUNT, heartParticleCount(world));

            pig.setLoveTicks(100);
            assertTrue(pig.feedBreedingItem(ItemType.WHEAT));

            assertEquals(Mob.LOVE_MODE_TICKS, pig.getLoveTicks());
            assertEquals(Mob.BREEDING_HEART_BURST_COUNT * 2L, heartParticleCount(world));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 breeding should reject babies and cooldown parents while refreshing adults")
    void breedingRejectsInvalidAgeStates() {
        Pig baby = new Pig();
        baby.setGrowingAge(Mob.BABY_GROWING_AGE);
        assertFalse(baby.feedBreedingItem(ItemType.WHEAT));

        Pig parent = new Pig();
        assertTrue(parent.feedBreedingItem(ItemType.WHEAT));
        parent.setLoveTicks(100);
        assertTrue(parent.feedBreedingItem(ItemType.WHEAT));
        assertEquals(Mob.LOVE_MODE_TICKS, parent.getLoveTicks());

        parent.setLoveTicks(0);
        parent.setGrowingAge(Mob.BREEDING_COOLDOWN_AGE);
        assertFalse(parent.feedBreedingItem(ItemType.WHEAT));
    }

    @Test
    @DisplayName("Release 1.0 accepted damage should cancel animal love mode")
    void acceptedDamageCancelsAnimalLoveMode() {
        World world = new World(6299L);
        try {
            Pig first = new Pig();
            Pig second = new Pig();
            first.setPosition(0.0f, 70.0f, 0.0f);
            second.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(first, second));

            assertTrue(first.feedBreedingItem(ItemType.WHEAT));
            assertTrue(second.feedBreedingItem(ItemType.WHEAT));
            updateEntities(world, Mob.BREEDING_SPAWN_DELAY_TICKS / 2);

            assertTrue(first.damage(1.0f, DamageSource.generic()));

            assertFalse(first.isInLove());
            updateEntities(world, Mob.BREEDING_SPAWN_DELAY_TICKS + 2);
            assertEquals(2, pigs(world).size());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 animals in love mode should emit heart particles")
    void loveModeSpawnsHeartParticles() {
        World world = new World(6285L);
        try {
            Pig pig = new Pig();
            pig.setPosition(0.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(pig));

            assertTrue(pig.feedBreedingItem(ItemType.WHEAT));
            world.getParticles().clear();
            for (int i = 0; i < 10; i++) {
                world.updateEntities(1.0f / 20.0f);
            }

            WorldParticle heart = world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.HEART)
                    .findFirst()
                    .orElseThrow();
            assertTrue(heart.getRenderX(0.0f) >= pig.getX() - pig.getWidth());
            assertTrue(heart.getRenderX(0.0f) <= pig.getX() + pig.getWidth());
            assertTrue(heart.getRenderY(0.0f) >= pig.getY() + 0.5f);
            assertTrue(heart.getRenderY(0.0f) <= pig.getY() + 0.5f + pig.getHeight());
            assertTrue(heart.getRenderZ(0.0f) >= pig.getZ() - pig.getWidth());
            assertTrue(heart.getRenderZ(0.0f) <= pig.getZ() + pig.getWidth());
            assertEquals(0.28f, heart.getScale(1.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 wheat-holding players should tempt nearby breeding animals")
    void animalsFollowPlayerHoldingWheat() {
        World world = new World(6286L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                    new ItemStack(ItemType.WHEAT, 1);
            world.setPlayer(player);

            Pig pig = new Pig();
            pig.setPosition(0.0f, 70.0f, 8.0f);
            world.replaceEntities(List.of(pig));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(pig.getZ() < 8.0f || pig.getMotionZ() < 0.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 love-mode animals should not keep following wheat")
    void loveModeAnimalsPrioritizeMatesOverWheatTemptation() {
        World world = new World(6295L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                    new ItemStack(ItemType.WHEAT, 1);
            world.setPlayer(player);

            InspectablePig pig = new InspectablePig();
            pig.random = new NoWanderRandom();
            pig.setPosition(0.0f, 70.0f, 8.0f);
            world.replaceEntities(List.of(pig));

            assertTrue(pig.feedBreedingItem(ItemType.WHEAT));
            world.updateEntities(1.0f / 20.0f);

            assertEquals(0.0f, pig.getForwardSpeedForTest(), 0.0001f);
            assertEquals(8.0f, pig.getZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 love-mode animals should seek nearby compatible mates")
    void loveModeAnimalsSeekCompatibleMates() {
        World world = new World(6287L);
        try {
            Pig first = new Pig();
            Pig second = new Pig();
            first.setPosition(0.0f, 70.0f, 7.5f);
            second.setPosition(0.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(first, second));

            assertTrue(first.feedBreedingItem(ItemType.WHEAT));
            assertTrue(second.feedBreedingItem(ItemType.WHEAT));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(first.getMotionZ() < 0.0f || first.getZ() < 7.5f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 mate seeking should continue through the full follow leash")
    void loveModeMateSeekingUsesFullContinuationRange() {
        World world = new World(6290L);
        try {
            Pig first = new Pig();
            Pig second = new Pig();
            first.setPosition(0.0f, 70.0f, 7.5f);
            second.setPosition(0.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(first, second));

            assertTrue(first.feedBreedingItem(ItemType.WHEAT));
            assertTrue(second.feedBreedingItem(ItemType.WHEAT));

            FollowMateGoal goal = first.getAI().getGoal(FollowMateGoal.class);
            assertNotNull(goal);
            assertTrue(goal.canUse());

            first.setPosition(0.0f, 70.0f, 9.75f);
            assertTrue(goal.canContinue());

            first.setPosition(0.0f, 70.0f, 10.25f);
            assertFalse(goal.canContinue());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 cooling-down parents should follow nearby babies")
    void coolingDownParentsFollowNearbyBabies() {
        World world = new World(6296L);
        try {
            InspectablePig parent = new InspectablePig();
            InspectablePig baby = new InspectablePig();
            parent.setGrowingAge(Mob.BREEDING_COOLDOWN_AGE);
            parent.setPosition(0.0f, 70.0f, 0.0f);
            parent.setRenderBodyYaw(180.0f);
            baby.setGrowingAge(Mob.BABY_GROWING_AGE);
            baby.setPosition(0.0f, 70.0f, 7.0f);
            world.replaceEntities(List.of(parent, baby));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(Mob.PARENT_FOLLOW_SPEED, parent.getForwardSpeedForTest(), 0.0001f);
            assertTrue(parent.getZ() > 0.0f || parent.getMotionZ() > 0.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 baby animals should follow nearby adult parents")
    void babyAnimalsFollowNearbyAdults() {
        World world = new World(6288L);
        try {
            Pig baby = new Pig();
            Pig parent = new Pig();
            baby.setGrowingAge(Mob.BABY_GROWING_AGE);
            baby.setPosition(0.0f, 70.0f, 7.0f);
            parent.setPosition(0.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(baby, parent));

            world.updateEntities(1.0f / 20.0f);

            assertTrue(baby.getZ() < 7.0f || baby.getMotionZ() < 0.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 grown babies should drop stale parent-follow movement immediately")
    void grownBabyDropsParentFollowMovementOnTransitionTick() {
        World world = new World(6300L);
        try {
            InspectablePig baby = new InspectablePig();
            InspectablePig parent = new InspectablePig();
            baby.setGrowingAge(-1);
            baby.setPosition(0.0f, 70.0f, 7.0f);
            parent.setPosition(0.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(baby, parent));

            world.updateEntities(1.0f / 20.0f);

            assertFalse(baby.isBaby());
            assertFalse(baby.getAI().isGoalActive(baby.getAI().getGoal(FollowParentGoal.class)));
            assertEquals(0, baby.getGrowingAge());
            assertEquals(0.0f, baby.getForwardSpeedForTest(), 0.0001f);
            assertEquals(0.0f, baby.getMotionX(), 0.0001f);
            assertEquals(0.0f, baby.getMotionZ(), 0.0001f);
            assertEquals(7.0f, baby.getZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 cooldown parents should drop stale child-follow movement immediately")
    void cooldownParentDropsChildFollowMovementOnTransitionTick() {
        World world = new World(6301L);
        try {
            InspectablePig parent = new InspectablePig();
            InspectablePig baby = new InspectablePig();
            parent.setGrowingAge(1);
            parent.setPosition(0.0f, 70.0f, 0.0f);
            baby.setGrowingAge(Mob.BABY_GROWING_AGE);
            baby.setPosition(0.0f, 70.0f, 7.0f);
            world.replaceEntities(List.of(parent, baby));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(0, parent.getGrowingAge());
            assertFalse(parent.getAI().isGoalActive(parent.getAI().getGoal(FollowChildGoal.class)));
            assertEquals(0.0f, parent.getForwardSpeedForTest(), 0.0001f);
            assertEquals(0.0f, parent.getMotionX(), 0.0001f);
            assertEquals(0.0f, parent.getMotionZ(), 0.0001f);
            assertEquals(0.0f, parent.getZ(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 parent following should require a baby far from an adult")
    void parentFollowingRequiresBabyFarFromAdult() {
        World world = new World(6289L);
        try {
            Pig baby = new Pig();
            Pig parent = new Pig();
            baby.setGrowingAge(Mob.BABY_GROWING_AGE);
            baby.setPosition(0.0f, 70.0f, 2.0f);
            parent.setPosition(0.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(baby, parent));

            FollowParentGoal goal = baby.ai.getGoal(FollowParentGoal.class);
            assertNotNull(goal);
            assertFalse(goal.canUse());

            baby.setPosition(0.0f, 70.0f, 7.0f);
            assertTrue(goal.canUse());

            baby.setGrowingAge(0);
            assertFalse(goal.canUse());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 animal follow goals should use source movement multipliers")
    void animalFollowGoalsUseReleaseMovementMultipliers() {
        assertWheatFollowSpeed();
        assertMateFollowSpeed();
        assertParentFollowSpeed();
        assertChildFollowSpeed();
    }

    @Test
    @DisplayName("Release 1.0 sheep babies inherit one parent wool color")
    void sheepBabyInheritsParentWoolColor() {
        World world = new World(6284L);
        try {
            Sheep red = new Sheep();
            Sheep blue = new Sheep();
            red.setWoolColor(14);
            blue.setWoolColor(11);
            red.setPosition(0.0f, 70.0f, 0.0f);
            blue.setPosition(1.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(red, blue));

            assertTrue(red.feedBreedingItem(ItemType.WHEAT));
            assertTrue(blue.feedBreedingItem(ItemType.WHEAT));
            updateEntities(world, Mob.BREEDING_SPAWN_DELAY_TICKS + 1);

            Sheep child = world.getEntities().stream()
                    .filter(Sheep.class::isInstance)
                    .map(Sheep.class::cast)
                    .filter(Sheep::isBaby)
                    .findFirst()
                    .orElseThrow();
            assertTrue(child.getWoolColor() == 14 || child.getWoolColor() == 11);
        } finally {
            world.cleanup();
        }
    }

    private static void updateEntities(World world, int ticks) {
        for (int i = 0; i < ticks; i++) {
            world.updateEntities(1.0f / 20.0f);
        }
    }

    private static List<Pig> pigs(World world) {
        return world.getEntities().stream()
                .filter(Pig.class::isInstance)
                .map(Pig.class::cast)
                .toList();
    }

    private static long heartParticleCount(World world) {
        return world.getParticles().stream()
                .filter(particle -> particle.getType() == WorldParticle.Type.HEART)
                .count();
    }

    private static void assertWheatFollowSpeed() {
        World world = new World(6291L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                    new ItemStack(ItemType.WHEAT, 1);
            world.setPlayer(player);

            InspectablePig pig = new InspectablePig();
            pig.setPosition(0.0f, 70.0f, 8.0f);
            world.replaceEntities(List.of(pig));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(Mob.BREEDING_ITEM_FOLLOW_SPEED, pig.getForwardSpeedForTest(), 0.0001f);
            assertTrue(pig.getZ() < 8.0f || pig.getMotionZ() < 0.0f);
        } finally {
            world.cleanup();
        }
    }

    private static void assertMateFollowSpeed() {
        World world = new World(6292L);
        try {
            InspectablePig first = new InspectablePig();
            InspectablePig second = new InspectablePig();
            first.setPosition(0.0f, 70.0f, 7.5f);
            second.setPosition(0.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(first, second));

            assertTrue(first.feedBreedingItem(ItemType.WHEAT));
            assertTrue(second.feedBreedingItem(ItemType.WHEAT));
            world.updateEntities(1.0f / 20.0f);

            assertEquals(Mob.BREEDING_MATE_FOLLOW_SPEED, first.getForwardSpeedForTest(), 0.0001f);
            assertTrue(first.getZ() < 7.5f || first.getMotionZ() < 0.0f);
        } finally {
            world.cleanup();
        }
    }

    private static void assertParentFollowSpeed() {
        World world = new World(6293L);
        try {
            InspectablePig baby = new InspectablePig();
            InspectablePig parent = new InspectablePig();
            baby.setGrowingAge(Mob.BABY_GROWING_AGE);
            baby.setPosition(0.0f, 70.0f, 7.0f);
            parent.setPosition(0.0f, 70.0f, 0.0f);
            world.replaceEntities(List.of(baby, parent));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(Mob.PARENT_FOLLOW_SPEED, baby.getForwardSpeedForTest(), 0.0001f);
            assertTrue(baby.getZ() < 7.0f || baby.getMotionZ() < 0.0f);
        } finally {
            world.cleanup();
        }
    }

    private static void assertChildFollowSpeed() {
        World world = new World(6297L);
        try {
            InspectablePig parent = new InspectablePig();
            InspectablePig baby = new InspectablePig();
            parent.setGrowingAge(Mob.BREEDING_COOLDOWN_AGE);
            parent.setPosition(0.0f, 70.0f, 0.0f);
            parent.setRenderBodyYaw(180.0f);
            baby.setGrowingAge(Mob.BABY_GROWING_AGE);
            baby.setPosition(0.0f, 70.0f, 7.0f);
            world.replaceEntities(List.of(parent, baby));

            world.updateEntities(1.0f / 20.0f);

            assertEquals(Mob.PARENT_FOLLOW_SPEED, parent.getForwardSpeedForTest(), 0.0001f);
            assertTrue(parent.getZ() > 0.0f || parent.getMotionZ() > 0.0f);
        } finally {
            world.cleanup();
        }
    }

    private static final class InspectablePig extends Pig {
        private float getForwardSpeedForTest() {
            return forwardSpeed;
        }
    }

    private static final class NoWanderRandom extends java.util.Random {
        @Override
        public float nextFloat() {
            return 1.0f;
        }

        @Override
        public double nextGaussian() {
            return 0.0D;
        }
    }
}
