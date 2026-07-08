package com.craftzero.main;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.LivingEntity;
import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.FireballEntity;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.progression.ArmorSlot;
import com.craftzero.progression.AchievementTracker;
import com.craftzero.progression.AchievementType;
import com.craftzero.progression.EnchantmentInstance;
import com.craftzero.progression.EnchantmentType;
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

import static org.junit.jupiter.api.Assertions.*;

class PlayerCombatTest {
    @Test
    @DisplayName("Player hurt should apply damage, knockback, and 20-tick immunity")
    void playerHurtAppliesKnockbackAndImmunity() {
        Player player = new Player(0.0f, 64.0f, 0.0f);
        player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);

        boolean firstHit = player.hurt(CombatRules.EASY_ZOMBIE_DAMAGE,
                -1.0f, 64.0f, 0.0f,
                CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK,
                CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK);
        boolean secondHit = player.hurt(CombatRules.EASY_ZOMBIE_DAMAGE,
                -1.0f, 64.0f, 0.0f,
                CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK,
                CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK);

        assertTrue(firstHit);
        assertFalse(secondHit);
        assertEquals(18.0f, player.getStats().getHealth(), 0.001f);
        assertTrue(player.getVelocity().x > 0.0f);
        assertEquals(CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK, player.getVelocity().y, 0.001f);
    }

    @Test
    @DisplayName("Stronger damage should replace prior damage during hurt immunity")
    void strongerDamageReplacesPriorDamageDuringImmunity() {
        Player player = new Player(0.0f, 64.0f, 0.0f);
        player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);

        boolean firstHit = player.hurt(2.0f, -1.0f, 64.0f, 0.0f, 0.0f, 0.0f);
        boolean strongerHit = player.hurt(6.0f, -1.0f, 64.0f, 0.0f, 0.0f, 0.0f);
        boolean weakerHit = player.hurt(4.0f, -1.0f, 64.0f, 0.0f, 0.0f, 0.0f);

        assertTrue(firstHit);
        assertTrue(strongerHit);
        assertFalse(weakerHit);
        assertEquals(14.0f, player.getStats().getHealth(), 0.001f);
    }

    @Test
    @DisplayName("Fall damage should not scale with difficulty")
    void fallDamageDoesNotScaleWithDifficulty() {
        assertFallDamageOnDifficulty(Difficulty.PEACEFUL);
        assertFallDamageOnDifficulty(Difficulty.HARD);
    }

    @Test
    @DisplayName("Release 1.0 starvation should wait 80 ticks before damage")
    void starvationWaitsReleaseIntervalBeforeDamage() {
        PlayerStats stats = new PlayerStats();
        stats.restore(20.0f, 0.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);

        stats.update(3.95f, false, false, Difficulty.HARD);
        assertEquals(20.0f, stats.getHealth(), 0.001f);

        stats.update(0.05f, false, false, Difficulty.HARD);
        assertEquals(19.0f, stats.getHealth(), 0.001f);
    }

    @Test
    @DisplayName("Release 1.0 natural regeneration should wait 80 ticks per health")
    void naturalRegenerationWaitsReleaseInterval() {
        PlayerStats stats = new PlayerStats();
        stats.restore(18.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);

        stats.update(3.95f, false, false, Difficulty.NORMAL);
        assertEquals(18.0f, stats.getHealth(), 0.001f);
        assertEquals(20.0f, stats.getHunger(), 0.001f);
        assertEquals(5.0f, stats.getSaturation(), 0.001f);

        stats.update(0.05f, false, false, Difficulty.NORMAL);
        assertEquals(19.0f, stats.getHealth(), 0.001f);
        assertEquals(20.0f, stats.getHunger(), 0.001f);
        assertEquals(5.0f, stats.getSaturation(), 0.001f);
        assertEquals(3.0f, stats.getExhaustion(), 0.001f);

        stats.update(4.0f, false, false, Difficulty.NORMAL);
        assertEquals(20.0f, stats.getHealth(), 0.001f);
        assertEquals(6.0f, stats.getExhaustion(), 0.001f);

        stats.update(0.0f, false, false, Difficulty.NORMAL);
        assertEquals(4.0f, stats.getSaturation(), 0.001f);
        assertEquals(2.0f, stats.getExhaustion(), 0.001f);
    }

    @Test
    @DisplayName("Release 1.0 natural regeneration should require at least 18 food")
    void naturalRegenerationRequiresHighFood() {
        PlayerStats stats = new PlayerStats();
        stats.restore(18.0f, 17.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);

        stats.update(8.0f, false, false, Difficulty.NORMAL);

        assertEquals(18.0f, stats.getHealth(), 0.001f);
    }

    @Test
    @DisplayName("Release 1.0 starvation should obey difficulty health floors")
    void starvationObeysDifficultyHealthFloors() {
        PlayerStats easy = starvingStats(11.0f);
        easy.update(4.0f, false, false, Difficulty.EASY);
        assertEquals(10.0f, easy.getHealth(), 0.001f);
        easy.update(4.0f, false, false, Difficulty.EASY);
        assertEquals(10.0f, easy.getHealth(), 0.001f);
        assertFalse(easy.isDead());

        PlayerStats normal = starvingStats(2.0f);
        normal.update(4.0f, false, false, Difficulty.NORMAL);
        assertEquals(1.0f, normal.getHealth(), 0.001f);
        normal.update(4.0f, false, false, Difficulty.NORMAL);
        assertEquals(1.0f, normal.getHealth(), 0.001f);
        assertFalse(normal.isDead());

        PlayerStats hard = starvingStats(1.0f);
        hard.update(4.0f, false, false, Difficulty.HARD);
        assertEquals(0.0f, hard.getHealth(), 0.001f);
        assertTrue(hard.isDead());
    }

    @Test
    @DisplayName("Peaceful should heal without draining food or starving the player")
    void peacefulHealsWithoutDrainingFoodOrStarving() {
        PlayerStats stats = new PlayerStats();
        stats.restore(8.0f, 0.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);

        stats.update(0.95f, true, true, Difficulty.PEACEFUL);
        assertEquals(8.0f, stats.getHealth(), 0.001f);
        assertEquals(0.0f, stats.getHunger(), 0.001f);

        stats.update(0.05f, true, true, Difficulty.PEACEFUL);
        assertEquals(9.0f, stats.getHealth(), 0.001f);
        assertEquals(0.0f, stats.getHunger(), 0.001f);

        stats.update(7.0f, true, true, Difficulty.PEACEFUL);

        assertEquals(16.0f, stats.getHealth(), 0.001f);
        assertEquals(0.0f, stats.getHunger(), 0.001f);
        assertFalse(stats.isDead());

        PlayerStats moving = new PlayerStats();
        moving.restore(20.0f, 10.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
        moving.update(10.0f, true, true, Difficulty.PEACEFUL);

        assertEquals(10.0f, moving.getHunger(), 0.001f);
    }

    @Test
    @DisplayName("Release 1.0 walking should drain food only after exhaustion crosses threshold")
    void walkingDrainsFoodAfterExhaustionThreshold() {
        PlayerStats stats = new PlayerStats();
        stats.restore(20.0f, 10.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);

        stats.update(1.0f, false, true, Difficulty.NORMAL, 399.0f);

        assertEquals(10.0f, stats.getHunger(), 0.001f);
        assertEquals(0.0f, stats.getSaturation(), 0.001f);
        assertEquals(3.99f, stats.getExhaustion(), 0.001f);

        stats.update(1.0f, false, true, Difficulty.NORMAL, 2.0f);

        assertEquals(9.0f, stats.getHunger(), 0.001f);
        assertEquals(0.01f, stats.getExhaustion(), 0.001f);
    }

    @Test
    @DisplayName("Release 1.0 jumping should add exhaustion instead of immediate food chip damage")
    void jumpingUsesFoodStatsExhaustionThreshold() {
        PlayerStats stats = new PlayerStats();
        stats.restore(20.0f, 10.0f, 2.0f, PlayerStats.MAX_AIR_SECONDS);

        for (int i = 0; i < 20; i++) {
            stats.onJump();
        }
        stats.update(0.0f, false, false, Difficulty.NORMAL);

        assertEquals(10.0f, stats.getHunger(), 0.001f);
        assertEquals(2.0f, stats.getSaturation(), 0.001f);
        assertEquals(4.0f, stats.getExhaustion(), 0.001f);

        stats.onJump();
        stats.update(0.0f, false, false, Difficulty.NORMAL);

        assertEquals(10.0f, stats.getHunger(), 0.001f);
        assertEquals(1.0f, stats.getSaturation(), 0.001f);
        assertEquals(0.2f, stats.getExhaustion(), 0.001f);
    }

    @Test
    @DisplayName("Peaceful exhaustion should not reduce the visible food bar")
    void peacefulExhaustionDoesNotReduceFoodBar() {
        PlayerStats stats = new PlayerStats();
        stats.restore(20.0f, 10.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);

        stats.update(1.0f, true, true, Difficulty.PEACEFUL, 100.0f);

        assertEquals(10.0f, stats.getHunger(), 0.001f);
        assertTrue(stats.getExhaustion() <= 4.0f);
    }

    @Test
    @DisplayName("Hunger effect should add FoodStats exhaustion instead of direct food loss")
    void hungerEffectAddsExhaustion() {
        PlayerStats stats = new PlayerStats();
        stats.restore(20.0f, 10.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
        stats.addEffect(new StatusEffectInstance(StatusEffectType.HUNGER, 200, 0));

        stats.update(8.0f, false, false, Difficulty.NORMAL);

        assertEquals(10.0f, stats.getHunger(), 0.001f);
        assertEquals(4.0f, stats.getExhaustion(), 0.001f);

        stats.update(1.0f / 20.0f, false, false, Difficulty.NORMAL);

        assertEquals(9.0f, stats.getHunger(), 0.001f);
        assertEquals(0.025f, stats.getExhaustion(), 0.001f);
    }

    @Test
    @DisplayName("Release 1.0 feeding should cap saturation to current food level")
    void feedingCapsSaturationToCurrentFoodLevel() {
        PlayerStats stats = new PlayerStats();
        stats.restore(20.0f, 4.0f, 3.0f, PlayerStats.MAX_AIR_SECONDS);

        stats.feed(4.0f, 12.8f);

        assertEquals(8.0f, stats.getHunger(), 0.001f);
        assertEquals(8.0f, stats.getSaturation(), 0.001f);
    }

    @Test
    @DisplayName("Restored player stats should clamp saturation to food level")
    void restoredStatsClampSaturationToFoodLevel() {
        PlayerStats stats = new PlayerStats();

        stats.restore(20.0f, 5.0f, 12.0f, PlayerStats.MAX_AIR_SECONDS, 1.5f);

        assertEquals(5.0f, stats.getHunger(), 0.001f);
        assertEquals(5.0f, stats.getSaturation(), 0.001f);
        assertEquals(1.5f, stats.getExhaustion(), 0.001f);
    }

    @Test
    @DisplayName("Release 1.0 block breaking should add FoodStats exhaustion")
    void blockBreakingAddsExhaustion() {
        PlayerStats stats = new PlayerStats();
        stats.restore(20.0f, 10.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);

        stats.onBlockBreak();
        stats.update(0.0f, false, false, Difficulty.NORMAL);

        assertEquals(10.0f, stats.getHunger(), 0.001f);
        assertEquals(0.025f, stats.getExhaustion(), 0.001f);
    }

    @Test
    @DisplayName("Accepted player attacks should add Release 1.0 exhaustion")
    void acceptedPlayerAttacksAddExhaustion() throws Exception {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        player.getStats().restore(20.0f, 10.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
        Zombie zombie = new Zombie();
        zombie.setPosition(1.0f, 70.0f, 0.0f);

        attackEntity(player, zombie);

        assertEquals(0.3f, player.getStats().getExhaustion(), 0.001f);
    }

    @Test
    @DisplayName("Accepted player damage should add Release 1.0 exhaustion once")
    void acceptedPlayerDamageAddsExhaustionOnce() {
        Player player = new Player(0.0f, 64.0f, 0.0f);
        player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);

        assertTrue(player.hurt(2.0f, -1.0f, 64.0f, 0.0f, 0.0f, 0.0f));
        assertFalse(player.hurt(2.0f, -1.0f, 64.0f, 0.0f, 0.0f, 0.0f));

        assertEquals(0.3f, player.getStats().getExhaustion(), 0.001f);
    }

    @Test
    @DisplayName("Accepted player damage should wear armor by the Release 1.0 quarter-damage rule")
    void acceptedPlayerDamageWearsArmorByQuarterDamageRule() {
        Player player = new Player(0.0f, 64.0f, 0.0f);
        player.setDifficulty(Difficulty.EASY);
        player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
        ItemStack chestplate = new ItemStack(ItemType.IRON_CHESTPLATE, 1);
        player.getInventory().getArmor()[ArmorSlot.CHESTPLATE.getIndex()] = chestplate;
        int initialDurability = chestplate.getDurability();

        assertTrue(player.hurt(12.0f, DamageSource.generic()));

        assertEquals(initialDurability - 3, chestplate.getDurability());
    }

    @Test
    @DisplayName("Release 1.0 sword blocking should reduce blockable damage before armor")
    void swordBlockingReducesBlockableDamageBeforeArmor() throws Exception {
        Player player = new Player(0.0f, 64.0f, 0.0f);
        player.setDifficulty(Difficulty.EASY);
        player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
        ItemStack sword = new ItemStack(ItemType.IRON_SWORD, 1);
        ItemStack chestplate = new ItemStack(ItemType.IRON_CHESTPLATE, 1);
        player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = sword;
        player.getInventory().getArmor()[ArmorSlot.CHESTPLATE.getIndex()] = chestplate;
        int initialDurability = chestplate.getDurability();

        setSwordBlocking(player, true);

        assertTrue(player.hurt(5.0f, DamageSource.entity(DamageSource.Type.MOB_MELEE, null)));

        assertEquals(17.72f, player.getStats().getHealth(), 0.001f);
        assertEquals(initialDurability - 1, chestplate.getDurability());
    }

    @Test
    @DisplayName("Release 1.0 sword blocking should not reduce unblockable fall damage")
    void swordBlockingDoesNotReduceUnblockableFallDamage() throws Exception {
        Player player = new Player(0.0f, 64.0f, 0.0f);
        player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
        player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                new ItemStack(ItemType.IRON_SWORD, 1);

        setSwordBlocking(player, true);

        assertTrue(player.hurt(5.0f,
                DamageSource.point(DamageSource.Type.FALL, 0.0f, 64.0f, 0.0f, 0.0f, 0.0f)));

        assertEquals(15.0f, player.getStats().getHealth(), 0.001f);
    }

    @Test
    @DisplayName("Resistance should reduce accepted player damage by the old per-level amount")
    void resistanceReducesAcceptedPlayerDamage() {
        Player player = new Player(0.0f, 64.0f, 0.0f);
        player.setDifficulty(Difficulty.EASY);
        player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
        player.getStats().addEffect(new StatusEffectInstance(StatusEffectType.RESISTANCE, 200, 1));

        assertTrue(player.hurt(10.0f, DamageSource.generic()));

        assertEquals(14.0f, player.getStats().getHealth(), 0.001f);
    }

    @Test
    @DisplayName("Scaled armor durability loss should break equipped armor")
    void scaledArmorDurabilityLossBreaksEquippedArmor() {
        Player player = new Player(0.0f, 64.0f, 0.0f);
        player.setDifficulty(Difficulty.EASY);
        player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
        player.getInventory().getArmor()[ArmorSlot.CHESTPLATE.getIndex()] =
                new ItemStack(ItemType.IRON_CHESTPLATE, 1, 2);

        assertTrue(player.hurt(8.0f, DamageSource.generic()));

        assertNull(player.getInventory().getArmor()[ArmorSlot.CHESTPLATE.getIndex()]);
    }

    @Test
    @DisplayName("Critical player hits should emit the old three-tick crit particle emitter")
    void criticalPlayerHitsEmitCritParticles() throws Exception {
        World world = new World(6270L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.setWorld(world);
            world.setPlayer(player);
            player.getVelocity().y = -0.2f;
            Zombie zombie = new Zombie();
            zombie.setPosition(1.0f, 70.0f, 0.0f);

            attackEntity(player, zombie);

            long initial = countParticles(world, WorldParticle.Type.CRIT);
            world.updateParticles(1.0f / 20.0f);
            long secondTick = countParticles(world, WorldParticle.Type.CRIT);
            world.updateParticles(1.0f / 20.0f);
            long thirdTick = countParticles(world, WorldParticle.Type.CRIT);

            assertTrue(initial > 0);
            assertTrue(initial <= 16);
            assertTrue(secondTick > initial);
            assertTrue(thirdTick > secondTick);
            assertTrue(thirdTick <= 48);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Damage-enchanted player hits should emit the old three-tick magic crit emitter")
    void damageEnchantedPlayerHitsEmitMagicCritParticles() throws Exception {
        World world = new World(6271L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.setWorld(world);
            world.setPlayer(player);
            ItemStack sword = new ItemStack(ItemType.IRON_SWORD, 1);
            sword.setEnchantments(List.of(new EnchantmentInstance(EnchantmentType.SHARPNESS, 1)));
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = sword;
            Zombie zombie = new Zombie();
            zombie.setPosition(1.0f, 70.0f, 0.0f);

            attackEntity(player, zombie);

            long initial = countParticles(world, WorldParticle.Type.MAGIC_CRIT);
            world.updateParticles(1.0f / 20.0f);
            long secondTick = countParticles(world, WorldParticle.Type.MAGIC_CRIT);
            world.updateParticles(1.0f / 20.0f);
            long thirdTick = countParticles(world, WorldParticle.Type.MAGIC_CRIT);

            assertTrue(initial > 0);
            assertTrue(initial <= 16);
            assertTrue(secondTick > initial);
            assertTrue(thirdTick > secondTick);
            assertTrue(thirdTick <= 48);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 Overkill should unlock from one nine-heart player hit")
    void overkillUnlocksFromSingleHighDamagePlayerHit() throws Exception {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
        unlockEnchantingBranch(player.getStats().getAchievements());
        player.getVelocity().y = -0.2f;
        ItemStack sword = new ItemStack(ItemType.DIAMOND_SWORD, 1);
        sword.setEnchantments(List.of(new EnchantmentInstance(EnchantmentType.SHARPNESS, 5)));
        player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = sword;
        Zombie zombie = new Zombie();
        zombie.setPosition(1.0f, 70.0f, 0.0f);

        attackEntity(player, zombie);

        assertTrue(player.getStats().getAchievements().isUnlocked(AchievementType.OVERKILL));
    }

    @Test
    @DisplayName("Release 1.0 Monster Hunter should unlock from a real player monster kill")
    void monsterHunterUnlocksFromPlayerMonsterKill() throws Exception {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        unlockBuildSword(player.getStats().getAchievements());
        player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                new ItemStack(ItemType.WOODEN_SWORD, 1);
        Zombie zombie = new Zombie();
        zombie.setPosition(1.0f, 70.0f, 0.0f);
        zombie.setHealth(3.0f);

        attackEntity(player, zombie);

        assertTrue(player.getStats().getAchievements().isUnlocked(AchievementType.KILL_ENEMY));
    }

    @Test
    @DisplayName("Player mob kills should update statistics from real mob death")
    void playerMobKillsUpdateStatisticsFromMobDeath() throws Exception {
        World world = new World(6294L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.setWorld(world);
            world.setPlayer(player);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                    new ItemStack(ItemType.WOODEN_SWORD, 1);
            Zombie zombie = new Zombie();
            zombie.setPosition(1.0f, 70.0f, 0.0f);
            zombie.setHealth(3.0f);
            world.replaceEntities(List.of(zombie));

            attackEntity(player, zombie);
            world.updateEntities(0.0f);

            assertEquals(1, player.getStats().getStatistics().getMobKills());
            assertEquals(1, player.getStats().getStatistics().getMonsterKills());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player bow release should emit the Release-style bow sound")
    void playerBowReleaseEmitsSound() throws Exception {
        World world = new World(6271L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            ItemStack bow = new ItemStack(ItemType.BOW, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = bow;
            player.getInventory().getHotbar()[1] = new ItemStack(ItemType.ARROW, 1);

            fireBow(player, world, bow);

            WorldSoundEvent sound = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.BOW, sound.soundId());
            assertEquals(1.0f, sound.volume(), 0.0001f);
            assertTrue(sound.pitch() >= 1.0f / 1.2f);
            assertTrue(sound.pitch() <= 1.0f / 0.8f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Fully drawn Release 1.0 bows should create critical arrows")
    void fullyDrawnBowsCreateCriticalArrows() throws Exception {
        World world = new World(6273L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            ItemStack bow = new ItemStack(ItemType.BOW, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = bow;
            player.getInventory().getHotbar()[1] = new ItemStack(ItemType.ARROW, 1);

            fireBow(player, world, bow);
            world.updateEntities(0.0f);

            ArrowEntity arrow = world.getEntities().stream()
                    .filter(ArrowEntity.class::isInstance)
                    .map(ArrowEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertTrue(arrow.isCritical());

            world.updateEntities(1.0f / 20.0f);

            assertTrue(world.getParticles().stream()
                    .anyMatch(particle -> particle.getType() == WorldParticle.Type.CRIT));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 bows should ignore post-1.0 bow enchantment metadata")
    void bowEnchantmentsDoNotAffectReleaseOneArrows() throws Exception {
        World world = new World(6272L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            world.setPlayer(player);
            ItemStack bow = new ItemStack(ItemType.BOW, 1);
            bow.addEnchantment(new EnchantmentInstance(EnchantmentType.POWER, 5));
            bow.addEnchantment(new EnchantmentInstance(EnchantmentType.PUNCH, 2));
            bow.addEnchantment(new EnchantmentInstance(EnchantmentType.FLAME, 1));
            bow.addEnchantment(new EnchantmentInstance(EnchantmentType.INFINITY, 1));
            ItemStack arrows = new ItemStack(ItemType.ARROW, 2);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = bow;
            player.getInventory().getHotbar()[1] = arrows;

            fireBow(player, world, bow);
            world.updateEntities(0.0f);

            assertEquals(1, arrows.getCount(), "Infinity metadata should not prevent Release 1.0 arrow consumption");
            ArrowEntity arrow = world.getEntities().stream()
                    .filter(ArrowEntity.class::isInstance)
                    .map(ArrowEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(6.0f, arrow.getDamage(), 0.0001f);
            assertTrue(arrow.isCritical());
            assertEquals(CombatRules.ARROW_HORIZONTAL_KNOCKBACK, arrow.getKnockbackHorizontal(), 0.0001f);
            assertEquals(CombatRules.ARROW_VERTICAL_KNOCKBACK, arrow.getKnockbackVertical(), 0.0001f);
            assertEquals(0, arrow.getFireTicksOnHit());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player attacks deflect fireballs along the aim direction")
    void playerAttackDeflectsFireball() throws Exception {
        Player player = new Player(0.0f, 70.0f, 0.0f);
        FireballEntity fireball = new FireballEntity(1.0f, 71.0f, 0.0f,
                -0.45f, 0.0f, 0.0f, null, false);

        attackFireball(player, fireball, new Vector3f(1.0f, 0.0f, 0.0f));

        assertTrue(fireball.isDeflectedByPlayer());
        assertEquals(0.45f, fireball.getMotionX(), 0.001f);
        assertEquals(0.0f, fireball.getMotionY(), 0.001f);
        assertEquals(0.0f, fireball.getMotionZ(), 0.001f);
    }

    private static void setSwordBlocking(Player player, boolean blocking) throws Exception {
        Field field = Player.class.getDeclaredField("isBlockingItem");
        field.setAccessible(true);
        field.setBoolean(player, blocking);
    }

    private static void attackEntity(Player player, LivingEntity target) throws Exception {
        Method method = Player.class.getDeclaredMethod("attackEntity", LivingEntity.class);
        method.setAccessible(true);
        method.invoke(player, target);
    }

    private static long countParticles(World world, WorldParticle.Type type) {
        return world.getParticles().stream()
                .filter(particle -> particle.getType() == type)
                .count();
    }

    private static void unlockEnchantingBranch(AchievementTracker tracker) {
        assertTrue(tracker.unlock(AchievementType.OPEN_INVENTORY));
        assertTrue(tracker.unlock(AchievementType.MINE_WOOD));
        assertTrue(tracker.unlock(AchievementType.BUILD_WORKBENCH));
        assertTrue(tracker.unlock(AchievementType.BUILD_PICKAXE));
        assertTrue(tracker.unlock(AchievementType.BUILD_FURNACE));
        assertTrue(tracker.unlock(AchievementType.ACQUIRE_IRON));
        assertTrue(tracker.unlock(AchievementType.DIAMONDS));
        assertTrue(tracker.unlock(AchievementType.ENCHANTMENTS));
    }

    private static void unlockBuildSword(AchievementTracker tracker) {
        assertTrue(tracker.unlock(AchievementType.OPEN_INVENTORY));
        assertTrue(tracker.unlock(AchievementType.MINE_WOOD));
        assertTrue(tracker.unlock(AchievementType.BUILD_WORKBENCH));
        assertTrue(tracker.unlock(AchievementType.BUILD_SWORD));
    }

    private static void assertFallDamageOnDifficulty(Difficulty difficulty) {
        Player player = new Player(0.0f, 64.0f, 0.0f);
        player.setDifficulty(difficulty);
        player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);

        boolean hit = player.hurt(5.0f,
                DamageSource.point(DamageSource.Type.FALL, 0.0f, 64.0f, 0.0f, 0.0f, 0.0f));

        assertTrue(hit);
        assertEquals(15.0f, player.getStats().getHealth(), 0.001f);
    }

    private static PlayerStats starvingStats(float health) {
        PlayerStats stats = new PlayerStats();
        stats.restore(health, 0.0f, 0.0f, PlayerStats.MAX_AIR_SECONDS);
        return stats;
    }

    private static void attackFireball(Player player, FireballEntity fireball, Vector3f direction) throws Exception {
        Method method = Player.class.getDeclaredMethod("attackFireball", FireballEntity.class, Vector3f.class);
        method.setAccessible(true);
        method.invoke(player, fireball, direction);
    }

    private static void fireBow(Player player, World world, ItemStack bow) throws Exception {
        Method method = Player.class.getDeclaredMethod("fireBow",
                World.class, ItemStack.class, Vector3f.class, float.class);
        method.setAccessible(true);
        method.invoke(player, world, bow, new Vector3f(1.0f, 0.0f, 0.0f), 1.0f);
    }
}
