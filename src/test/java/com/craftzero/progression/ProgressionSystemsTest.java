package com.craftzero.progression;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.ExperienceOrbEntity;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.mob.Cow;
import com.craftzero.entity.mob.CaveSpider;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.main.PlayerStats;
import com.craftzero.world.BlockType;
import com.craftzero.world.Dimension;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;
import com.craftzero.world.tile.BrewingRecipeRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ProgressionSystemsTest {

    @Test
    @DisplayName("Experience orbs should use Release-style split values")
    void experienceOrbSplitValues() {
        assertEquals(2477, ExperienceOrbEntity.getOrbValue(5000));
        assertEquals(617, ExperienceOrbEntity.getOrbValue(800));
        assertEquals(17, ExperienceOrbEntity.getOrbValue(20));
        assertEquals(1, ExperienceOrbEntity.getOrbValue(1));
    }

    @Test
    @DisplayName("Experience orb launch randomness should be source-controllable")
    void experienceOrbLaunchRandomnessIsControllable() {
        ExperienceOrbEntity orb = new ExperienceOrbEntity(0.0f, 70.0f, 0.0f, 5,
                new SequenceRandom(0.25f, 0.50f, 0.75f, 1.0f));

        assertEquals(90.0f, orb.getYaw(), 0.001f);
        assertEquals(0.0f, orb.getMotionX(), 0.001f);
        assertEquals(0.3f, orb.getMotionY(), 0.001f);
        assertEquals(0.2f, orb.getMotionZ(), 0.001f);
    }

    @Test
    @DisplayName("World-spawned experience orbs should use the world RNG for launch and lava fizz")
    void worldSpawnedExperienceOrbUsesWorldRandom() {
        SequenceRandom random = new SequenceRandom(0.25f, 0.50f, 0.75f, 1.0f,
                0.50f, 0.50f, 0.50f, 0.50f, 0.25f);
        World world = new RandomOverrideWorld(6238L, random);
        try {
            world.setBlock(0, 70, 0, BlockType.LAVA, 0);
            ExperienceOrbEntity orb = new ExperienceOrbEntity(0.5f, 70.0f, 0.5f, 5);
            world.spawnEntity(orb);

            assertEquals(90.0f, orb.getYaw(), 0.001f);
            assertEquals(0.0f, orb.getMotionX(), 0.001f);
            assertEquals(0.3f, orb.getMotionY(), 0.001f);
            assertEquals(0.2f, orb.getMotionZ(), 0.001f);

            orb.setMotion(0.0f, 0.0f, 0.0f);
            world.updateEntities(1.0f / 20.0f);

            WorldSoundEvent sound = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.FIZZ, sound.soundId());
            assertEquals(2.1f, sound.pitch(), 0.0001f);
            assertEquals(9, random.nextFloatCalls());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Experience orb pickup should respect the player's short Release 1.0 cooldown")
    void experienceOrbPickupUsesPlayerCooldown() {
        World world = new World(6235L);
        try {
            Player player = new Player(0.0f, 100.0f, 0.0f);
            world.setPlayer(player);
            ExperienceOrbEntity first = readyOrb(0.0f, 101.0f, 0.0f, 5);
            ExperienceOrbEntity second = readyOrb(0.0f, 101.0f, 0.0f, 7);
            assertEquals(0, first.getPickupDelayTicks());
            assertEquals(0, second.getPickupDelayTicks());
            world.spawnEntity(first);
            world.spawnEntity(second);

            world.updateEntities(1.0f / 20.0f);

            assertEquals(1L, List.of(first, second).stream().filter(ExperienceOrbEntity::isRemoved).count());
            assertEquals(5, player.getStats().getProgression().getTotalExperience());
            assertExperiencePickupSound(world.drainSoundEvents().get(0));

            world.updateEntities(1.0f / 20.0f);
            assertEquals(5, player.getStats().getProgression().getTotalExperience());
            assertTrue(world.drainSoundEvents().isEmpty());

            world.updateEntities(1.0f / 20.0f);
            assertEquals(12, player.getStats().getProgression().getTotalExperience());
            assertExperiencePickupSound(world.drainSoundEvents().get(0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Experience orb pickup should play the Release level-up cue when crossing a level")
    void experienceOrbPickupPlaysLevelUpSoundOnLevelCrossing() {
        World world = new World(6236L);
        try {
            Player player = new Player(0.0f, 100.0f, 0.0f);
            world.setPlayer(player);
            ExperienceOrbEntity orb = readyOrb(0.0f, 101.0f, 0.0f,
                    PlayerProgression.experienceForLevel(1));
            world.spawnEntity(orb);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(orb.isRemoved());
            assertEquals(1, player.getStats().getProgression().getLevel());
            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(2, sounds.size());
            assertExperiencePickupSound(sounds.get(0));
            assertExperienceLevelUpSound(sounds.get(1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Experience orb attraction should aim toward the player eye height")
    void experienceOrbAttractionUsesPlayerEyeHeight() {
        World world = new World(6240L);
        try {
            Player player = new Player(0.0f, 100.0f, 0.0f);
            world.setPlayer(player);
            ExperienceOrbEntity orb = readyOrb(0.0f, 100.0f, 0.0f, 5);
            world.spawnEntity(orb);

            world.updateEntities(1.0f / 20.0f);

            assertFalse(orb.isRemoved());
            assertEquals(0.0329f, orb.getMotionY(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Experience orbs should keep the old ground bounce")
    void experienceOrbBouncesOnGround() {
        World world = new World(6236L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            ExperienceOrbEntity orb = new ExperienceOrbEntity(0.5f, 70.02f, 0.5f, 5);
            orb.setMotion(0.0f, -0.2f, 0.0f);
            world.replaceEntities(List.of(orb));

            world.updateEntities(1.0f / 20.0f);

            assertFalse(orb.isRemoved());
            assertTrue(orb.isOnGround());
            assertEquals(0.203f, orb.getMotionY(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Experience orbs should fizz while bobbing in lava")
    void experienceOrbFizzesInLava() {
        World world = new World(6237L);
        try {
            world.setBlock(0, 70, 0, BlockType.LAVA, 0);
            ExperienceOrbEntity orb = new ExperienceOrbEntity(0.5f, 70.0f, 0.5f, 5,
                    new SequenceRandom(0.5f, 0.5f, 0.5f, 0.5f,
                            0.5f, 0.5f, 0.5f, 0.5f, 0.25f));
            orb.setMotion(0.0f, 0.0f, 0.0f);
            world.replaceEntities(List.of(orb));

            world.updateEntities(1.0f / 20.0f);

            WorldSoundEvent sound = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.FIZZ, sound.soundId());
            assertEquals(0.4f, sound.volume(), 0.0001f);
            assertEquals(2.1f, sound.pitch(), 0.0001f);
            assertEquals(0.0f, orb.getMotionX(), 0.001f);
            assertEquals(0.0f, orb.getMotionZ(), 0.001f);
            assertEquals(0.196f, orb.getMotionY(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Experience orbs should use source health when damaged")
    void experienceOrbDamageUsesSourceHealth() {
        ExperienceOrbEntity orb = new ExperienceOrbEntity(0.0f, 70.0f, 0.0f, 5);

        assertTrue(orb.damage(2.0f, DamageSource.generic()));
        assertEquals(3, orb.getHealth());
        assertFalse(orb.isRemoved());

        assertTrue(orb.damage(3.0f, DamageSource.generic()));
        assertTrue(orb.isRemoved());
    }

    @Test
    @DisplayName("Explosion damage should destroy experience orbs")
    void explosionDamageDestroysExperienceOrbs() {
        World world = new World(6239L);
        try {
            ExperienceOrbEntity orb = new ExperienceOrbEntity(0.5f, 70.0f, 0.5f, 5);
            orb.setMotion(0.0f, 0.0f, 0.0f);
            world.replaceEntities(List.of(orb));

            world.explode(0.5f, 70.0f, 0.5f, 4.0f);

            assertTrue(orb.isRemoved());
            assertEquals(0, world.getEntities().stream()
                    .filter(ExperienceOrbEntity.class::isInstance)
                    .filter(entity -> !entity.isRemoved())
                    .count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player death should clear level-zero XP even when no death orbs spawn")
    void playerDeathClearsLevelZeroExperienceWithoutOrbs() {
        World world = new World(6241L);
        try {
            Player player = new Player(0.5f, 70.0f, 0.5f);
            player.setWorld(world);
            world.setPlayer(player);
            player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
            player.getStats().getProgression().addExperience(PlayerProgression.experienceForLevel(1) - 1);
            assertEquals(0, player.getStats().getProgression().getLevel());

            assertTrue(player.getStats().damage(100.0f));
            player.update(1.0f / 20.0f, world);
            world.updateEntities(1.0f / 20.0f);

            assertEquals(0, player.getStats().getProgression().getTotalExperience());
            assertEquals(0, spawnedExperienceValue(world));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player death should drop capped Release 1.0 XP and clear stored XP")
    void playerDeathDropsCappedExperienceAndClearsStoredExperience() {
        World world = new World(6242L);
        try {
            Player player = new Player(0.5f, 70.0f, 0.5f);
            player.setWorld(world);
            world.setPlayer(player);
            player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
            player.getStats().getProgression().restore(PlayerProgression.experienceForLevel(20), 1234);

            assertTrue(player.getStats().damage(100.0f));
            player.update(1.0f / 20.0f, world);
            world.updateEntities(1.0f / 20.0f);

            assertEquals(0, player.getStats().getProgression().getTotalExperience());
            assertEquals(1234, player.getStats().getProgression().getScore());
            assertEquals(100, spawnedExperienceValue(world));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player death should drop equipped armor instead of deleting it")
    void playerDeathDropsEquippedArmor() {
        World world = new World(6243L);
        try {
            Player player = new Player(0.5f, 70.0f, 0.5f);
            player.setWorld(world);
            world.setPlayer(player);
            player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
            player.getInventory().getArmor()[ArmorSlot.CHESTPLATE.getIndex()] =
                    new ItemStack(ItemType.IRON_CHESTPLATE, 1);

            assertTrue(player.getStats().damage(100.0f));
            player.update(1.0f / 20.0f, world);

            assertNull(player.getInventory().getArmor()[ArmorSlot.CHESTPLATE.getIndex()]);
            assertEquals(1, droppedItemCount(world, ItemType.IRON_CHESTPLATE));
        } finally {
            world.cleanup();
        }
    }

    private static ExperienceOrbEntity readyOrb(float x, float y, float z, int value) {
        ExperienceOrbEntity orb = new ExperienceOrbEntity(x, y, z, value);
        orb.setMotion(0.0f, 0.0f, 0.0f);
        return orb;
    }

    private static int spawnedExperienceValue(World world) {
        return world.getEntities().stream()
                .filter(ExperienceOrbEntity.class::isInstance)
                .map(ExperienceOrbEntity.class::cast)
                .mapToInt(ExperienceOrbEntity::getValue)
                .sum();
    }

    private static int droppedItemCount(World world, ItemType type) {
        return world.getDroppedItems().stream()
                .filter(item -> item.getItemType() == type)
                .mapToInt(item -> item.getCount())
                .sum();
    }

    private static void assertExperiencePickupSound(WorldSoundEvent sound) {
        assertEquals(WorldSoundEvent.XP_PICKUP, sound.soundId());
        assertEquals(0.1f, sound.volume(), 0.0001f);
        assertTrue(sound.pitch() >= 0.55f);
        assertTrue(sound.pitch() <= 1.25f);
    }

    private static void assertExperienceLevelUpSound(WorldSoundEvent sound) {
        assertEquals(WorldSoundEvent.XP_LEVEL_UP, sound.soundId());
        assertEquals(0.75f, sound.volume(), 0.0001f);
        assertEquals(1.0f, sound.pitch(), 0.0001f);
    }

    private static final class SequenceRandom extends Random {
        private final float[] values;
        private int index;

        private SequenceRandom(float... values) {
            this.values = values;
        }

        @Override
        public float nextFloat() {
            if (values.length == 0) {
                return 0.0f;
            }
            float value = values[index % values.length];
            index++;
            return value;
        }

        private int nextFloatCalls() {
            return index;
        }
    }

    private static final class FixedIntRandom extends Random {
        private final int value;

        private FixedIntRandom(int value) {
            this.value = value;
        }

        @Override
        public int nextInt(int bound) {
            return Math.min(value, bound - 1);
        }
    }

    private static final class RandomOverrideWorld extends World {
        private final Random random;

        private RandomOverrideWorld(long seed, Random random) {
            super(seed);
            this.random = random;
        }

        @Override
        public Random getRandom() {
            return random;
        }
    }

    @Test
    @DisplayName("Armor calculator should apply armor points and protection enchantments")
    void armorReductionIncludesProtection() {
        ItemStack chestplate = new ItemStack(ItemType.IRON_CHESTPLATE, 1);
        chestplate.addEnchantment(new EnchantmentInstance(EnchantmentType.PROTECTION, 4));
        ItemStack[] armor = new ItemStack[4];
        armor[ArmorSlot.CHESTPLATE.getIndex()] = chestplate;

        float reduced = ArmorCalculator.reduceDamage(10.0f, armor, DamageSource.generic());

        assertTrue(reduced < 7.6f, "Protection should reduce damage beyond the iron chestplate armor points");
        assertEquals(6, ArmorCalculator.armorPoints(armor));
    }

    @Test
    @DisplayName("Armor calculator should ignore armor pieces in the wrong slot")
    void armorCalculatorIgnoresWrongSlotArmor() {
        ItemStack chestplate = new ItemStack(ItemType.IRON_CHESTPLATE, 1);
        chestplate.addEnchantment(new EnchantmentInstance(EnchantmentType.PROTECTION, 4));
        ItemStack[] armor = new ItemStack[4];
        armor[ArmorSlot.HELMET.getIndex()] = chestplate;

        assertEquals(0, ArmorCalculator.armorPoints(armor));
        assertEquals(0, ArmorCalculator.protectionFactor(armor, DamageSource.generic()));
        assertEquals(10.0f, ArmorCalculator.reduceDamage(10.0f, armor, DamageSource.generic()), 0.0001f);
    }

    @Test
    @DisplayName("Feather Falling should reduce fall-source damage")
    void featherFallingReducesFallDamage() {
        ItemStack boots = new ItemStack(ItemType.IRON_BOOTS, 1);
        boots.addEnchantment(new EnchantmentInstance(EnchantmentType.FEATHER_FALLING, 4));
        ItemStack[] armor = new ItemStack[4];
        armor[ArmorSlot.BOOTS.getIndex()] = boots;

        float reduced = ArmorCalculator.reduceDamage(10.0f, armor,
                DamageSource.point(DamageSource.Type.FALL, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));

        assertTrue(reduced < 9.2f, "Feather Falling should reduce fall damage beyond boots armor points");
    }

    @Test
    @DisplayName("Potion resolver should expose Release 1.0 effect durations")
    void potionEffectsResolve() {
        PotionData speed = new PotionData(PotionType.SWIFTNESS, false, true, false);
        List<StatusEffectInstance> effects = PotionEffectResolver.effects(speed);

        assertEquals(1, effects.size());
        assertSame(StatusEffectType.SPEED, effects.get(0).type());
        assertEquals(9600, effects.get(0).durationTicks());
        assertEquals("Potion of Swiftness Extended", PotionEffectResolver.displayName(speed));
    }

    @Test
    @DisplayName("Splash potion duration should reject source-style edge effects below 21 ticks")
    void splashPotionDurationRejectsEdgeEffectsBelowTwentyOneTicks() {
        Cow cow = new Cow();

        PotionEffectResolver.applyToLiving(cow, new PotionData(PotionType.POISON, true, false, false),
                20.0f / 900.0f);
        assertTrue(cow.getActiveEffects().isEmpty());

        PotionEffectResolver.applyToLiving(cow, new PotionData(PotionType.POISON, true, false, false),
                21.0f / 900.0f);
        assertEquals(1, cow.getActiveEffects().size());
        assertSame(StatusEffectType.POISON, cow.getActiveEffects().get(0).type());
        assertEquals(21, cow.getActiveEffects().get(0).durationTicks());
    }

    @Test
    @DisplayName("Player attack potion modifiers should use Release 1.0 bit-shift values")
    void playerAttackPotionModifiersMatchReleaseOne() {
        PlayerStats stats = new PlayerStats();

        stats.addEffect(new StatusEffectInstance(StatusEffectType.STRENGTH, 200, 0));
        assertEquals(3.0f, stats.getAttackDamageBonus(), 0.0001f);

        stats.clearEffects();
        stats.addEffect(new StatusEffectInstance(StatusEffectType.STRENGTH, 200, 1));
        assertEquals(6.0f, stats.getAttackDamageBonus(), 0.0001f);

        stats.clearEffects();
        stats.addEffect(new StatusEffectInstance(StatusEffectType.WEAKNESS, 200, 0));
        assertEquals(-2.0f, stats.getAttackDamageBonus(), 0.0001f);

        stats.clearEffects();
        stats.addEffect(new StatusEffectInstance(StatusEffectType.WEAKNESS, 200, 1));
        assertEquals(-4.0f, stats.getAttackDamageBonus(), 0.0001f);
    }

    @Test
    @DisplayName("Living mob Speed and Slowness should modify AI movement")
    void livingMobMovementPotionModifiersAffectAiMotion() {
        float normalForwardMotion = forwardMotionMagnitude(new TestLivingEntity());

        TestLivingEntity speedy = new TestLivingEntity();
        speedy.addEffect(new StatusEffectInstance(StatusEffectType.SPEED, 200, 1));

        TestLivingEntity slowed = new TestLivingEntity();
        slowed.addEffect(new StatusEffectInstance(StatusEffectType.SLOWNESS, 200, 0));

        assertEquals(0.02f, normalForwardMotion, 0.0001f);
        assertEquals(normalForwardMotion * 1.4f, forwardMotionMagnitude(speedy), 0.0001f);
        assertEquals(normalForwardMotion * 0.85f, forwardMotionMagnitude(slowed), 0.0001f);
    }

    @Test
    @DisplayName("Living entity Jump Boost should add old per-level jump motion")
    void livingEntityJumpBoostAddsReleaseOneJumpMotion() {
        TestLivingEntity normal = new TestLivingEntity();
        normal.setOnGroundForTest(true);
        normal.jump();
        assertEquals(0.42f, normal.getMotionY(), 0.0001f);

        TestLivingEntity boosted = new TestLivingEntity();
        boosted.addEffect(new StatusEffectInstance(StatusEffectType.JUMP_BOOST, 200, 2));
        boosted.setOnGroundForTest(true);
        boosted.jump();
        assertEquals(0.72f, boosted.getMotionY(), 0.0001f);
    }

    @Test
    @DisplayName("Living entity Resistance should reduce incoming damage")
    void livingEntityResistanceReducesIncomingDamage() {
        TestLivingEntity living = new TestLivingEntity();
        living.addEffect(new StatusEffectInstance(StatusEffectType.RESISTANCE, 200, 2));

        assertTrue(living.damage(10.0f, DamageSource.generic()));

        assertEquals(16.0f, living.getHealth(), 0.0001f);
    }

    @Test
    @DisplayName("Status effect merging should preserve stronger Release 1.0 amplifiers")
    void statusEffectMergingPreservesStrongerAmplifiers() {
        PlayerStats stats = new PlayerStats();
        stats.addEffect(new StatusEffectInstance(StatusEffectType.SPEED, 100, 1));
        stats.addEffect(new StatusEffectInstance(StatusEffectType.SPEED, 400, 0));

        assertEquals(1, stats.getActiveEffects().size());
        assertEquals(100, stats.getActiveEffects().get(0).durationTicks());
        assertEquals(1, stats.getActiveEffects().get(0).amplifier());

        stats.addEffect(new StatusEffectInstance(StatusEffectType.SPEED, 120, 1));
        assertEquals(120, stats.getActiveEffects().get(0).durationTicks());
        assertEquals(1, stats.getActiveEffects().get(0).amplifier());

        Cow cow = new Cow();
        cow.addEffect(new StatusEffectInstance(StatusEffectType.POISON, 80, 1));
        cow.addEffect(new StatusEffectInstance(StatusEffectType.POISON, 300, 0));

        assertEquals(1, cow.getActiveEffects().size());
        assertEquals(80, cow.getActiveEffects().get(0).durationTicks());
        assertEquals(1, cow.getActiveEffects().get(0).amplifier());
    }

    @Test
    @DisplayName("Status effect visuals should blend active Release-era potion colors")
    void statusEffectVisualsBlendPotionColors() {
        List<StatusEffectInstance> effects = List.of(
                new StatusEffectInstance(StatusEffectType.POISON, 80, 0),
                new StatusEffectInstance(StatusEffectType.STRENGTH, 80, 1));

        assertEquals(0x7C4927, StatusEffectVisuals.mixedColor(effects));
    }

    @Test
    @DisplayName("Living entities with active effects should emit old mob spell particles")
    void livingEntitiesEmitStatusEffectParticles() {
        World world = new World(6263L);
        try {
            TestLivingEntity living = new TestLivingEntity();
            living.setPosition(1.0f, 70.0f, 2.0f);
            living.setWorld(world);
            living.addEffect(new StatusEffectInstance(StatusEffectType.POISON, 80, 0));

            for (int i = 0; i < 5; i++) {
                living.tick();
            }

            WorldParticle particle = world.getParticles().stream()
                    .filter(candidate -> candidate.getType() == WorldParticle.Type.MOB_SPELL)
                    .findFirst()
                    .orElseThrow();
            assertEquals(StatusEffectVisuals.color(StatusEffectType.POISON), (int) particle.getData());
            assertTrue(particle.getRenderY(0.0f) > living.getY());
            assertTrue(particle.getRenderY(0.0f) < living.getY() + living.getHeight());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Regeneration and poison should use separate Release 1.0 tick cadences")
    void timedPotionAmplifiersSpeedTickCadence() {
        PlayerStats stats = new PlayerStats();
        stats.restore(10.0f, 0.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
        stats.addEffect(new StatusEffectInstance(StatusEffectType.REGENERATION, 25, 0));

        stats.update(1.0f / 20.0f, false, false);

        assertEquals(10.0f, stats.getHealth(), 0.0001f);

        stats.clearEffects();
        stats.restore(10.0f, 0.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
        stats.addEffect(new StatusEffectInstance(StatusEffectType.REGENERATION, 50, 0));

        stats.update(1.0f / 20.0f, false, false);

        assertEquals(11.0f, stats.getHealth(), 0.0001f);

        stats.clearEffects();
        stats.restore(10.0f, 0.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
        stats.addEffect(new StatusEffectInstance(StatusEffectType.REGENERATION, 25, 1));

        stats.update(1.0f / 20.0f, false, false);

        assertEquals(11.0f, stats.getHealth(), 0.0001f);

        stats.clearEffects();
        stats.restore(10.0f, 0.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
        stats.addEffect(new StatusEffectInstance(StatusEffectType.POISON, 25, 0));

        stats.update(1.0f / 20.0f, false, false);

        assertEquals(9.0f, stats.getHealth(), 0.0001f);

        TestLivingEntity living = new TestLivingEntity();
        living.setHealth(10.0f);
        living.addEffect(new StatusEffectInstance(StatusEffectType.REGENERATION, 50, 0));

        living.tick();

        assertEquals(11.0f, living.getHealth(), 0.0001f);
    }

    @Test
    @DisplayName("Undead and spider-family mobs should reject Release 1.0 inapplicable timed effects")
    void mobPotionApplicabilityMatchesReleaseOne() {
        Zombie zombie = new Zombie();
        PotionEffectResolver.applyToLiving(zombie, new PotionData(PotionType.POISON, false, false, false), 1.0f);
        PotionEffectResolver.applyToLiving(zombie, new PotionData(PotionType.REGENERATION, false, false, false), 1.0f);

        assertTrue(zombie.getActiveEffects().isEmpty());

        zombie.addEffect(new StatusEffectInstance(StatusEffectType.POISON, 80, 0));
        zombie.addEffect(new StatusEffectInstance(StatusEffectType.REGENERATION, 80, 0));

        assertTrue(zombie.getActiveEffects().isEmpty());

        CaveSpider caveSpider = new CaveSpider();
        PotionEffectResolver.applyToLiving(caveSpider, new PotionData(PotionType.POISON, false, false, false), 1.0f);

        assertTrue(caveSpider.getActiveEffects().isEmpty());

        PotionEffectResolver.applyToLiving(caveSpider, new PotionData(PotionType.REGENERATION, false, false, false), 1.0f);

        assertEquals(1, caveSpider.getActiveEffects().size());
        assertSame(StatusEffectType.REGENERATION, caveSpider.getActiveEffects().get(0).type());
    }

    @Test
    @DisplayName("Respiration should use Release 1.0 air consumption rolls")
    void respirationUsesReleaseOneAirConsumptionRolls() {
        PlayerStats normal = new PlayerStats();
        normal.updateAir(true, 1.0f, 0, new FixedIntRandom(1));
        assertEquals(PlayerStats.MAX_AIR_SECONDS - 1.0f, normal.getCurrentAir(), 0.0001f);

        PlayerStats skipped = new PlayerStats();
        skipped.updateAir(true, 1.0f, 3, new FixedIntRandom(1));
        assertEquals(PlayerStats.MAX_AIR_SECONDS, skipped.getCurrentAir(), 0.0001f);

        PlayerStats consumed = new PlayerStats();
        consumed.updateAir(true, 1.0f, 3, new FixedIntRandom(0));
        assertEquals(PlayerStats.MAX_AIR_SECONDS - 1.0f, consumed.getCurrentAir(), 0.0001f);
    }

    @Test
    @DisplayName("Water Breathing should preserve player air while underwater")
    void waterBreathingPreservesPlayerAir() {
        PlayerStats stats = new PlayerStats();
        stats.restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS - 5.0f);
        stats.addEffect(new StatusEffectInstance(StatusEffectType.WATER_BREATHING, 200, 0));

        stats.updateAir(true, 5.0f, 0, new FixedIntRandom(0));

        assertEquals(PlayerStats.MAX_AIR_SECONDS, stats.getCurrentAir(), 0.0001f);
        assertEquals(20.0f, stats.getHealth(), 0.0001f);
    }

    @Test
    @DisplayName("Instant health and harming should use Release 1.0 source amounts")
    void instantPotionAmountsMatchReleaseOne() {
        Player player = new Player(0.0f, 80.0f, 0.0f);
        player.getStats().restore(10.0f, 0.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);

        PotionEffectResolver.applyToPlayer(player, new PotionData(PotionType.HEALING, false, false, false), 1.0f);
        assertEquals(14.0f, player.getStats().getHealth(), 0.0001f);

        TestLivingEntity harmed = new TestLivingEntity();
        PotionEffectResolver.applyToLiving(harmed, new PotionData(PotionType.HARMING, false, false, false), 1.0f);
        assertEquals(14.0f, harmed.getHealth(), 0.0001f);

        TestLivingEntity harmedTwo = new TestLivingEntity();
        PotionEffectResolver.applyToLiving(harmedTwo, new PotionData(PotionType.HARMING, false, false, true), 1.0f);
        assertEquals(8.0f, harmedTwo.getHealth(), 0.0001f);

        TestLivingEntity weakSplash = new TestLivingEntity();
        PotionEffectResolver.applyToLiving(weakSplash, new PotionData(PotionType.HARMING, true, false, false),
                0.49f);
        assertEquals(17.0f, weakSplash.getHealth(), 0.0001f);

        Zombie undead = new Zombie();
        PotionEffectResolver.applyToLiving(undead, new PotionData(PotionType.HEALING, false, false, false), 1.0f);
        assertEquals(14.0f, undead.getHealth(), 0.0001f);

        Zombie healedUndead = new Zombie();
        healedUndead.setHealth(10.0f);
        PotionEffectResolver.applyToLiving(healedUndead, new PotionData(PotionType.HARMING, false, false, false),
                1.0f);
        assertEquals(14.0f, healedUndead.getHealth(), 0.0001f);
    }

    @Test
    @DisplayName("Instant harming should use magic damage against players")
    void instantHarmingUsesMagicDamageAgainstPlayers() throws ReflectiveOperationException {
        Player player = new Player(0.0f, 80.0f, 0.0f);
        player.setDifficulty(com.craftzero.main.Difficulty.HARD);
        player.getStats().restore(20.0f, 0.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);

        ItemStack chestplate = new ItemStack(ItemType.DIAMOND_CHESTPLATE, 1);
        int durability = chestplate.getDurability();
        player.getInventory().getArmor()[ArmorSlot.CHESTPLATE.getIndex()] = chestplate;
        player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                new ItemStack(ItemType.DIAMOND_SWORD, 1);
        setBlockingItem(player, true);

        PotionEffectResolver.applyToPlayer(player, new PotionData(PotionType.HARMING, false, false, false), 1.0f);

        assertEquals(14.0f, player.getStats().getHealth(), 0.0001f);
        assertEquals(durability, chestplate.getDurability());
    }

    @Test
    @DisplayName("Potion catalog should stay within Release 1.0 potion identities")
    void potionCatalogExcludesPostReleaseOnePotionTypes() {
        assertFalse(Arrays.stream(PotionType.values()).map(Enum::name).anyMatch("NIGHT_VISION"::equals));
        assertTrue(Arrays.stream(StatusEffectType.values()).map(Enum::name).anyMatch("NIGHT_VISION"::equals));
        assertTrue(BrewingRecipeRegistry.creativePotions().stream()
                .noneMatch(potion -> "NIGHT_VISION".equals(potion.type().name())));
    }

    @Test
    @DisplayName("Enchanting bookshelf power should use the Release 1.0 two-block ring and cap")
    void enchantingBookshelfPowerUsesReleaseOneRingAndCap() {
        World world = new World(6260L);
        try {
            prepareEnchantingArea(world);
            placeFullBookshelfRing(world);

            assertEquals(30, BookshelfPower.count(world, 0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Enchanting bookshelf power should require clear lower and upper air gaps")
    void enchantingBookshelfPowerRequiresClearGap() {
        World world = new World(6261L);
        try {
            prepareEnchantingArea(world);
            world.setBlock(2, 70, 0, BlockType.BOOKSHELF, 0);
            world.setBlock(2, 71, 0, BlockType.BOOKSHELF, 0);

            assertEquals(2, BookshelfPower.count(world, 0, 70, 0));

            world.setBlock(1, 70, 0, BlockType.STONE, 0);
            assertEquals(0, BookshelfPower.count(world, 0, 70, 0));

            world.setBlock(1, 70, 0, BlockType.AIR, 0);
            world.setBlock(1, 71, 0, BlockType.STONE, 0);
            assertEquals(0, BookshelfPower.count(world, 0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Enchanting bookshelf power should count diagonal wing shelves through the diagonal gap")
    void enchantingBookshelfPowerCountsDiagonalWingShelves() {
        World world = new World(6262L);
        try {
            prepareEnchantingArea(world);
            for (int dy = 0; dy <= 1; dy++) {
                world.setBlock(2, 70 + dy, 2, BlockType.BOOKSHELF, 0);
                world.setBlock(2, 70 + dy, 1, BlockType.BOOKSHELF, 0);
                world.setBlock(1, 70 + dy, 2, BlockType.BOOKSHELF, 0);
            }

            assertEquals(6, BookshelfPower.count(world, 0, 70, 0));

            world.setBlock(1, 70, 1, BlockType.STONE, 0);
            assertEquals(0, BookshelfPower.count(world, 0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Enchanting offers should cap at old level 50 and ignore enchanted items")
    void enchantingOfferCapAndExistingEnchantments() {
        ItemStack sword = new ItemStack(ItemType.DIAMOND_SWORD, 1);
        int cost = EnchantmentResolver.offerCost(new Random(1L), 2, 30, sword);
        assertTrue(cost <= 50);

        sword.addEnchantment(new EnchantmentInstance(EnchantmentType.SHARPNESS, 1));
        assertFalse(EnchantmentResolver.isEnchantable(sword));
        assertEquals(0, EnchantmentResolver.offerCost(new Random(1L), 2, 30, sword));
    }

    @Test
    @DisplayName("Generated enchantments should use Release 1.0 weighted random selection")
    void generatedEnchantmentsUseReleaseOneWeights() {
        ItemStack sword = new ItemStack(ItemType.DIAMOND_SWORD, 1);

        List<EnchantmentInstance> enchantments = EnchantmentResolver.generate(
                new WeightedEnchantingRandom(14), sword, 4);

        assertEquals(1, enchantments.size());
        assertEquals(new EnchantmentInstance(EnchantmentType.SMITE, 1), enchantments.get(0));
    }

    @Test
    @DisplayName("Generated enchantment levels should use the Release 1.0 quarter-enchantability bonus")
    void generatedEnchantmentsUseReleaseOneAdjustedLevelFormula() {
        ItemStack goldSword = new ItemStack(ItemType.GOLD_SWORD, 1);
        ItemStack diamondSword = new ItemStack(ItemType.DIAMOND_SWORD, 1);

        assertEquals(21, EnchantmentResolver.adjustedEnchantmentLevel(
                new MaxEnchantingLevelRandom(), goldSword, 10));
        assertEquals(15, EnchantmentResolver.adjustedEnchantmentLevel(
                new MaxEnchantingLevelRandom(), diamondSword, 10));
    }

    @Test
    @DisplayName("Enchanting level bands should match Release 1.0 per-enchantment gates")
    void enchantingLevelBandsMatchReleaseOneGates() {
        assertTrue(EnchantmentResolver.canApplyAtAdjustedLevel(EnchantmentType.PROTECTION, 1, 21));
        assertFalse(EnchantmentResolver.canApplyAtAdjustedLevel(EnchantmentType.PROTECTION, 1, 22));
        assertFalse(EnchantmentResolver.canApplyAtAdjustedLevel(EnchantmentType.PROTECTION, 4, 33));
        assertTrue(EnchantmentResolver.canApplyAtAdjustedLevel(EnchantmentType.PROTECTION, 4, 34));

        assertFalse(EnchantmentResolver.canApplyAtAdjustedLevel(EnchantmentType.KNOCKBACK, 2, 24));
        assertTrue(EnchantmentResolver.canApplyAtAdjustedLevel(EnchantmentType.KNOCKBACK, 2, 25));
        assertFalse(EnchantmentResolver.canApplyAtAdjustedLevel(EnchantmentType.FIRE_ASPECT, 2, 29));
        assertTrue(EnchantmentResolver.canApplyAtAdjustedLevel(EnchantmentType.FIRE_ASPECT, 2, 30));

        assertFalse(EnchantmentResolver.canApplyAtAdjustedLevel(EnchantmentType.UNBREAKING, 3, 20));
        assertTrue(EnchantmentResolver.canApplyAtAdjustedLevel(EnchantmentType.UNBREAKING, 3, 21));
        assertFalse(EnchantmentResolver.canApplyAtAdjustedLevel(EnchantmentType.FORTUNE, 3, 32));
        assertTrue(EnchantmentResolver.canApplyAtAdjustedLevel(EnchantmentType.FORTUNE, 3, 33));
        assertFalse(EnchantmentResolver.canApplyAtAdjustedLevel(EnchantmentType.POWER, 1, 50));
    }

    @Test
    @DisplayName("Release 1.0 experience curve should keep level 50 at the old total")
    void releaseOneExperienceCurveKeepsLevelFiftyCost() {
        assertEquals(4625, PlayerProgression.experienceForLevel(50));
    }

    @Test
    @DisplayName("Enchanting level spending should preserve the old progress bar fraction")
    void enchantingLevelCostPreservesReleaseOneProgressBar() {
        PlayerProgression progression = new PlayerProgression();
        int currentLevel = 10;
        int spentLevels = 3;
        int currentBase = PlayerProgression.experienceForLevel(currentLevel);
        int currentSpan = PlayerProgression.experienceForLevel(currentLevel + 1) - currentBase;
        int intoCurrentLevel = currentSpan / 2;
        int originalScore = 1234;
        progression.restore(currentBase + intoCurrentLevel, originalScore);

        assertTrue(progression.consumeLevels(spentLevels));

        int newLevel = currentLevel - spentLevels;
        int newBase = PlayerProgression.experienceForLevel(newLevel);
        int newSpan = PlayerProgression.experienceForLevel(newLevel + 1) - newBase;
        int expectedIntoNewLevel = (int) (((long) intoCurrentLevel * newSpan) / currentSpan);
        assertEquals(newLevel, progression.getLevel());
        assertEquals(expectedIntoNewLevel, progression.getExperienceIntoLevel());
        assertEquals(newBase + expectedIntoNewLevel, progression.getTotalExperience());
        assertEquals(originalScore, progression.getScore());
    }

    @Test
    @DisplayName("Release 1.0 achievements should be parent-gated and queue HUD notifications")
    void releaseOneAchievementsAreParentGatedAndQueued() {
        AchievementTracker tracker = new AchievementTracker();

        assertFalse(tracker.recordCrafted(ItemType.CRAFTING_TABLE));
        assertFalse(tracker.recordDimensionTravel(Dimension.OVERWORLD, Dimension.NETHER));
        assertTrue(tracker.recordInventoryOpened());
        assertTrue(tracker.recordBlockBroken(BlockType.OAK_LOG));
        assertTrue(tracker.recordCrafted(ItemType.CRAFTING_TABLE));
        assertFalse(tracker.recordCrafted(ItemType.CRAFTING_TABLE));

        assertTrue(tracker.isUnlocked(AchievementType.OPEN_INVENTORY));
        assertTrue(tracker.isUnlocked(AchievementType.MINE_WOOD));
        assertTrue(tracker.isUnlocked(AchievementType.BUILD_WORKBENCH));
        assertEquals(List.of("openInventory", "mineWood", "buildWorkBench"), tracker.unlockedIds());
        assertEquals(3, tracker.queuedNotificationCount());

        tracker.updateNotifications(0.1f);
        assertSame(AchievementType.OPEN_INVENTORY, tracker.activeNotification());
        assertTrue(AchievementTracker.notificationAlpha(tracker.activeNotificationAge()) > 0.0f);

        tracker.updateNotifications(AchievementTracker.TOAST_TOTAL_SECONDS);
        assertSame(AchievementType.MINE_WOOD, tracker.activeNotification());

        tracker.restoreUnlocked(List.of("openInventory", "mineWood"));
        assertTrue(tracker.isUnlocked(AchievementType.MINE_WOOD));
        assertFalse(tracker.isUnlocked(AchievementType.BUILD_WORKBENCH));
        assertEquals(0, tracker.queuedNotificationCount());
    }

    @Test
    @DisplayName("Release 1.0 Nether and End achievements should unlock from dimension travel")
    void releaseOneDimensionAchievementsUnlockFromTravel() {
        AchievementTracker tracker = new AchievementTracker();

        assertFalse(tracker.recordDimensionTravel(Dimension.OVERWORLD, Dimension.NETHER));
        assertFalse(tracker.recordDimensionTravel(Dimension.OVERWORLD, Dimension.THE_END));

        assertTrue(tracker.unlock(AchievementType.OPEN_INVENTORY));
        assertTrue(tracker.unlock(AchievementType.MINE_WOOD));
        assertTrue(tracker.unlock(AchievementType.BUILD_WORKBENCH));
        assertTrue(tracker.unlock(AchievementType.BUILD_PICKAXE));
        assertTrue(tracker.unlock(AchievementType.BUILD_FURNACE));
        assertTrue(tracker.unlock(AchievementType.ACQUIRE_IRON));
        assertTrue(tracker.unlock(AchievementType.DIAMONDS));

        assertTrue(tracker.recordDimensionTravel(Dimension.OVERWORLD, Dimension.NETHER));
        assertTrue(tracker.isUnlocked(AchievementType.PORTAL));
        assertFalse(tracker.recordDimensionTravel(Dimension.OVERWORLD, Dimension.THE_END));

        assertTrue(tracker.recordCollectedItem(ItemType.BLAZE_ROD));
        assertTrue(tracker.isUnlocked(AchievementType.BLAZE_ROD));
        assertTrue(tracker.recordDimensionTravel(Dimension.OVERWORLD, Dimension.THE_END));
        assertTrue(tracker.isUnlocked(AchievementType.THE_END));
        assertTrue(tracker.recordDimensionTravel(Dimension.THE_END, Dimension.OVERWORLD));
        assertTrue(tracker.isUnlocked(AchievementType.THE_END2));
    }

    @Test
    @DisplayName("Release 1.0 Ghast and brewing achievements should keep old parent gates")
    void releaseOneCombatAndBrewingAchievementsKeepParentGates() {
        AchievementTracker tracker = new AchievementTracker();

        assertFalse(tracker.recordReturnedFireballKill());
        assertFalse(tracker.recordMonsterKilled());
        assertFalse(tracker.recordSkeletonSniped(50.0f));
        assertFalse(tracker.recordBrewedPotionTaken());

        assertTrue(tracker.unlock(AchievementType.OPEN_INVENTORY));
        assertTrue(tracker.unlock(AchievementType.MINE_WOOD));
        assertTrue(tracker.unlock(AchievementType.BUILD_WORKBENCH));
        assertTrue(tracker.unlock(AchievementType.BUILD_SWORD));
        assertTrue(tracker.unlock(AchievementType.BUILD_PICKAXE));
        assertTrue(tracker.unlock(AchievementType.BUILD_FURNACE));
        assertTrue(tracker.unlock(AchievementType.ACQUIRE_IRON));
        assertTrue(tracker.unlock(AchievementType.DIAMONDS));
        assertTrue(tracker.unlock(AchievementType.PORTAL));

        assertFalse(tracker.recordSkeletonSniped(50.0f));
        assertTrue(tracker.recordMonsterKilled());
        assertTrue(tracker.isUnlocked(AchievementType.KILL_ENEMY));
        assertFalse(tracker.recordSkeletonSniped(49.99f));
        assertTrue(tracker.recordSkeletonSniped(50.0f));
        assertTrue(tracker.isUnlocked(AchievementType.SNIPE_SKELETON));
        assertTrue(tracker.recordReturnedFireballKill());
        assertTrue(tracker.isUnlocked(AchievementType.RETURN_TO_SENDER));
        assertFalse(tracker.recordBrewedPotionTaken());

        assertTrue(tracker.recordCollectedItem(ItemType.BLAZE_ROD));
        assertTrue(tracker.recordBrewedPotionTaken());
        assertTrue(tracker.isUnlocked(AchievementType.LOCAL_BREWERY));
        assertEquals(List.of("openInventory", "mineWood", "buildWorkBench", "buildPickaxe",
                "buildFurnace", "acquireIron", "diamonds", "buildSword", "killEnemy", "snipeSkeleton",
                "portal", "ghast", "blazeRod", "potion"),
                tracker.unlockedIds());
    }

    @Test
    @DisplayName("Release 1.0 rail and pig achievements should keep old parent gates")
    void releaseOneRailAndPigAchievementsKeepParentGates() {
        AchievementTracker tracker = new AchievementTracker();

        assertFalse(tracker.recordMinecartRideDistance(1000.0f));
        assertFalse(tracker.recordPigFlew());

        assertTrue(tracker.unlock(AchievementType.OPEN_INVENTORY));
        assertTrue(tracker.unlock(AchievementType.MINE_WOOD));
        assertTrue(tracker.unlock(AchievementType.BUILD_WORKBENCH));
        assertTrue(tracker.unlock(AchievementType.BUILD_PICKAXE));
        assertTrue(tracker.unlock(AchievementType.BUILD_FURNACE));
        assertTrue(tracker.unlock(AchievementType.ACQUIRE_IRON));
        assertFalse(tracker.recordMinecartRideDistance(999.99f));
        assertTrue(tracker.recordMinecartRideDistance(1000.0f));
        assertTrue(tracker.isUnlocked(AchievementType.ON_A_RAIL));

        assertFalse(tracker.recordPigFlew());
        assertTrue(tracker.unlock(AchievementType.BUILD_SWORD));
        assertTrue(tracker.recordCollectedItem(ItemType.LEATHER));
        assertTrue(tracker.recordPigFlew());
        assertTrue(tracker.isUnlocked(AchievementType.FLY_PIG));
    }

    @Test
    @DisplayName("Release 1.0 enchanting branch achievements should keep old parent gates")
    void releaseOneEnchantingBranchAchievementsKeepParentGates() {
        AchievementTracker tracker = new AchievementTracker();

        assertFalse(tracker.recordCrafted(ItemType.ENCHANTING_TABLE));
        assertFalse(tracker.recordCrafted(ItemType.BOOKSHELF));
        assertFalse(tracker.recordOverkillHit(18.0f));

        assertTrue(tracker.unlock(AchievementType.OPEN_INVENTORY));
        assertTrue(tracker.unlock(AchievementType.MINE_WOOD));
        assertTrue(tracker.unlock(AchievementType.BUILD_WORKBENCH));
        assertTrue(tracker.unlock(AchievementType.BUILD_PICKAXE));
        assertTrue(tracker.unlock(AchievementType.BUILD_FURNACE));
        assertTrue(tracker.unlock(AchievementType.ACQUIRE_IRON));
        assertTrue(tracker.unlock(AchievementType.DIAMONDS));

        assertTrue(tracker.recordCrafted(ItemType.ENCHANTING_TABLE));
        assertTrue(tracker.isUnlocked(AchievementType.ENCHANTMENTS));
        assertTrue(tracker.recordCrafted(ItemType.BOOKSHELF));
        assertTrue(tracker.isUnlocked(AchievementType.BOOKCASE));
        assertFalse(tracker.recordOverkillHit(17.99f));
        assertTrue(tracker.recordOverkillHit(18.0f));
        assertTrue(tracker.isUnlocked(AchievementType.OVERKILL));
        assertEquals(List.of("openInventory", "mineWood", "buildWorkBench", "buildPickaxe",
                "buildFurnace", "acquireIron", "diamonds", "enchantments", "overkill", "bookcase"),
                tracker.unlockedIds());
    }

    @Test
    @DisplayName("Release 1.0 enchanting should reject hoes while accepting old table item classes")
    void enchantingEligibilityMatchesReleaseOneTableItems() {
        assertTrue(EnchantmentResolver.isEnchantable(new ItemStack(ItemType.DIAMOND_SWORD, 1)));
        assertTrue(EnchantmentResolver.isEnchantable(new ItemStack(ItemType.DIAMOND_PICKAXE, 1)));
        assertTrue(EnchantmentResolver.isEnchantable(new ItemStack(ItemType.IRON_CHESTPLATE, 1)));
        assertNull(ArmorMaterial.materialOf(ItemType.DIAMOND_SWORD));
        assertEquals(10, EnchantmentResolver.enchantability(ItemType.DIAMOND_SWORD));
        assertSame(ArmorMaterial.IRON, ArmorMaterial.materialOf(ItemType.IRON_CHESTPLATE));

        ItemStack bow = new ItemStack(ItemType.BOW, 1);
        assertFalse(EnchantmentResolver.isEnchantable(bow));
        assertEquals(0, EnchantmentResolver.enchantability(ItemType.BOW));
        assertEquals(0, EnchantmentResolver.offerCost(new Random(1L), 2, 30, bow));
        assertTrue(EnchantmentResolver.generate(new Random(1L), bow, 30).isEmpty());

        for (ItemType hoe : List.of(ItemType.WOODEN_HOE, ItemType.STONE_HOE, ItemType.IRON_HOE,
                ItemType.DIAMOND_HOE, ItemType.GOLD_HOE)) {
            ItemStack stack = new ItemStack(hoe, 1);
            assertNull(ArmorMaterial.materialOf(hoe), hoe.name());
            assertFalse(EnchantmentResolver.isEnchantable(stack), hoe.name());
            assertEquals(0, EnchantmentResolver.enchantability(hoe), hoe.name());
            assertEquals(0, EnchantmentResolver.offerCost(new Random(1L), 2, 30, stack), hoe.name());
        }
    }

    @Test
    @DisplayName("Unbreaking should preserve the old armor-specific durability gate")
    void unbreakingArmorUsesReleaseOneDurabilityGate() {
        ItemStack pickaxe = new ItemStack(ItemType.DIAMOND_PICKAXE, 1);
        pickaxe.addEnchantment(new EnchantmentInstance(EnchantmentType.UNBREAKING, 1));
        DurabilityRandom toolRandom = new DurabilityRandom(0.0f, 1);

        assertTrue(EnchantmentResolver.shouldPreventDurabilityLoss(pickaxe, toolRandom));
        assertEquals(0, toolRandom.nextFloatCalls());
        assertEquals(1, toolRandom.nextIntCalls());

        ItemStack chestplate = new ItemStack(ItemType.IRON_CHESTPLATE, 1);
        chestplate.addEnchantment(new EnchantmentInstance(EnchantmentType.UNBREAKING, 1));
        DurabilityRandom armorForcedDamage = new DurabilityRandom(0.59f, 1);

        assertFalse(EnchantmentResolver.shouldPreventDurabilityLoss(chestplate, armorForcedDamage));
        assertEquals(1, armorForcedDamage.nextFloatCalls());
        assertEquals(0, armorForcedDamage.nextIntCalls());

        DurabilityRandom armorPreventedDamage = new DurabilityRandom(0.6f, 1);
        assertTrue(EnchantmentResolver.shouldPreventDurabilityLoss(chestplate, armorPreventedDamage));
        assertEquals(1, armorPreventedDamage.nextFloatCalls());
        assertEquals(1, armorPreventedDamage.nextIntCalls());
    }

    @Test
    @DisplayName("Unbreaking should not prevent durability loss without an enchantment roll")
    void unbreakingDurabilityPreventionRequiresEnchantAndRandom() {
        ItemStack helmet = new ItemStack(ItemType.DIAMOND_HELMET, 1);
        ItemStack shovel = new ItemStack(ItemType.IRON_SHOVEL, 1);
        shovel.addEnchantment(new EnchantmentInstance(EnchantmentType.UNBREAKING, 2));

        assertFalse(EnchantmentResolver.shouldPreventDurabilityLoss(helmet, new DurabilityRandom(0.99f, 1)));
        assertFalse(EnchantmentResolver.shouldPreventDurabilityLoss(shovel, null));
    }

    private static void prepareEnchantingArea(World world) {
        for (int y = 70; y <= 71; y++) {
            for (int z = -2; z <= 2; z++) {
                for (int x = -2; x <= 2; x++) {
                    world.setBlock(x, y, z, BlockType.AIR, 0);
                }
            }
        }
        world.setBlock(0, 70, 0, BlockType.ENCHANTING_TABLE, 0);
    }

    private static void placeFullBookshelfRing(World world) {
        for (int z = -2; z <= 2; z++) {
            for (int x = -2; x <= 2; x++) {
                if (Math.abs(x) != 2 && Math.abs(z) != 2) {
                    continue;
                }
                world.setBlock(x, 70, z, BlockType.BOOKSHELF, 0);
                world.setBlock(x, 71, z, BlockType.BOOKSHELF, 0);
            }
        }
    }

    private static final class TestLivingEntity extends LivingEntity {
        private TestLivingEntity() {
            super(0.6f, 1.8f, 20.0f);
        }

        private void stepForwardForTest() {
            setMoveDirection(0.0f, 1.0f);
            updateAnimation();
        }

        private void setOnGroundForTest(boolean onGround) {
            this.onGround = onGround;
        }
    }

    private static float forwardMotionMagnitude(TestLivingEntity entity) {
        entity.stepForwardForTest();
        return -entity.getMotionZ();
    }

    private static final class WeightedEnchantingRandom extends Random {
        private final int weightedRoll;

        private WeightedEnchantingRandom(int weightedRoll) {
            this.weightedRoll = weightedRoll;
        }

        @Override
        public int nextInt(int bound) {
            if (bound == 6) {
                return 0;
            }
            if (bound == 30) {
                return weightedRoll;
            }
            if (bound == 50) {
                return 49;
            }
            return 0;
        }

        @Override
        public float nextFloat() {
            return 0.5f;
        }
    }

    private static final class MaxEnchantingLevelRandom extends Random {
        @Override
        public int nextInt(int bound) {
            return bound - 1;
        }

        @Override
        public float nextFloat() {
            return 0.5f;
        }
    }

    private static final class DurabilityRandom extends Random {
        private final float floatValue;
        private final int intValue;
        private int nextFloatCalls;
        private int nextIntCalls;

        private DurabilityRandom(float floatValue, int intValue) {
            this.floatValue = floatValue;
            this.intValue = intValue;
        }

        @Override
        public float nextFloat() {
            nextFloatCalls++;
            return floatValue;
        }

        @Override
        public int nextInt(int bound) {
            nextIntCalls++;
            return Math.floorMod(intValue, bound);
        }

        private int nextFloatCalls() {
            return nextFloatCalls;
        }

        private int nextIntCalls() {
            return nextIntCalls;
        }
    }

    private static void setBlockingItem(Player player, boolean blocking) throws ReflectiveOperationException {
        Field field = Player.class.getDeclaredField("isBlockingItem");
        field.setAccessible(true);
        field.setBoolean(player, blocking);
    }
}
