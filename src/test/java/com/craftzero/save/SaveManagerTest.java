package com.craftzero.save;

import com.craftzero.entity.Entity;
import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.DroppedItem;
import com.craftzero.entity.EndCrystalEntity;
import com.craftzero.entity.EnderPearlEntity;
import com.craftzero.entity.ExperienceOrbEntity;
import com.craftzero.entity.FireballEntity;
import com.craftzero.entity.EyeOfEnderEntity;
import com.craftzero.entity.FallingBlockEntity;
import com.craftzero.entity.FishingHookEntity;
import com.craftzero.entity.FurnaceMinecartEntity;
import com.craftzero.entity.MinecartEntity;
import com.craftzero.entity.PaintingEntity;
import com.craftzero.entity.PrimedTntEntity;
import com.craftzero.entity.SplashPotionEntity;
import com.craftzero.entity.ThrownItemEntity;
import com.craftzero.entity.ai.MeleeAttackGoal;
import com.craftzero.entity.ai.PanicGoal;
import com.craftzero.entity.ai.RangedAttackGoal;
import com.craftzero.entity.ai.TargetNearestGoal;
import com.craftzero.entity.mob.Blaze;
import com.craftzero.entity.mob.Chicken;
import com.craftzero.entity.mob.Cow;
import com.craftzero.entity.mob.Creeper;
import com.craftzero.entity.mob.EnderDragon;
import com.craftzero.entity.mob.Enderman;
import com.craftzero.entity.mob.Ghast;
import com.craftzero.entity.mob.Giant;
import com.craftzero.entity.mob.MagmaCube;
import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.entity.mob.Pig;
import com.craftzero.entity.mob.Sheep;
import com.craftzero.entity.mob.Skeleton;
import com.craftzero.entity.mob.Slime;
import com.craftzero.entity.mob.SnowGolem;
import com.craftzero.entity.mob.Spider;
import com.craftzero.entity.mob.Squid;
import com.craftzero.entity.mob.Villager;
import com.craftzero.entity.mob.Wolf;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.progression.AchievementType;
import com.craftzero.progression.EnchantmentInstance;
import com.craftzero.progression.EnchantmentType;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionType;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import com.craftzero.world.Chunk;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.Dimension;
import com.craftzero.world.RedstoneEngine;
import com.craftzero.world.World;
import com.craftzero.world.WorldGenerator;
import com.craftzero.world.tile.BrewingStandTileEntity;
import com.craftzero.world.tile.BlockPos;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.DispenserTileEntity;
import com.craftzero.world.tile.EnchantingTableTileEntity;
import com.craftzero.world.tile.FurnaceTileEntity;
import com.craftzero.world.tile.JukeboxTileEntity;
import com.craftzero.world.tile.MonsterSpawnerTileEntity;
import com.craftzero.world.tile.NoteBlockTileEntity;
import com.craftzero.world.tile.SignTileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SaveManagerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Corrupt level.json should load the previous backup instead of falling back to a new world")
    void corruptLevelLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Files.writeString(worldDir.resolve("level.json"), "{ corrupt");

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Partial level.json should load the previous backup instead of losing player state")
    void partialLevelLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("partial-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Files.writeString(worldDir.resolve("level.json"), "{"
                    + "\"formatVersion\":" + SaveManager.FORMAT_VERSION + ","
                    + "\"targetVersion\":\"" + SaveManager.TARGET_VERSION + "\","
                    + "\"seed\":99,"
                    + "\"time\":300.0"
                    + "}");

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertNotNull(result.levelData().player);
            assertNotNull(result.levelData().inventory);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Partial level.json without backup should be reported as corrupt")
    void partialLevelWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("partial-no-backup-world");
        Files.createDirectories(worldDir);
        SaveManager manager = new SaveManager(worldDir);
        Files.writeString(worldDir.resolve("level.json"), "{"
                + "\"formatVersion\":" + SaveManager.FORMAT_VERSION + ","
                + "\"targetVersion\":\"" + SaveManager.TARGET_VERSION + "\","
                + "\"seed\":99,"
                + "\"time\":300.0"
                + "}");

        SaveManager.SaveLoadResult result = manager.loadLevel();
        assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
        assertNotNull(result.error());
        assertTrue(result.error().message().contains("missing player data"));
        assertNull(manager.loadLevelIfExists());
    }

    @Test
    @DisplayName("Invalid player stats without backup should be reported as corrupt")
    void invalidPlayerStatsWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-player-stats-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"player\": {", "\"health\": 20.0", "\"health\": 21.0"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid player stats"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid world time without backup should be reported as corrupt")
    void invalidWorldTimeWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-world-time-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            dayCycle.setTime(123.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteral(Files.readString(levelPath),
                    "\"time\": 123.0", "\"time\": -2.0"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid world time"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid weather countdowns without backup should be reported as corrupt")
    void invalidWeatherCountdownWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-weather-countdown-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.setWeatherState("rain", 321, 654);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteral(Files.readString(levelPath),
                    "\"weatherRainTime\": 321", "\"weatherRainTime\": 0"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid world weather timer"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid saved dimensions should load the previous backup")
    void invalidDimensionLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("invalid-dimension-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteral(Files.readString(levelPath),
                    "\"dimension\": \"overworld\"", "\"dimension\": \"moon\""));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(Dimension.OVERWORLD.getSaveName(), result.levelData().dimension);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid saved dimensions without backup should be reported as corrupt")
    void invalidDimensionWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-dimension-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteral(Files.readString(levelPath),
                    "\"dimension\": \"overworld\"", "\"dimension\": \"moon\""));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid world dimension"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid saved generator ids should load the previous backup")
    void invalidGeneratorIdLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("invalid-generator-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteral(Files.readString(levelPath),
                    "\"generatorId\": \"minecraft_java_1_0\"", "\"generatorId\": \"minecraft_java_1_0_nethre\""));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(WorldGenerator.RELEASE_ONE, result.levelData().generatorId);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid saved generator ids without backup should be reported as corrupt")
    void invalidGeneratorIdWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-generator-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteral(Files.readString(levelPath),
                    "\"generatorId\": \"minecraft_java_1_0\"", "\"generatorId\": \"minecraft_java_1_0_nethre\""));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid world generator"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Potion metadata on non-potion inventory items should load the previous backup")
    void nonPotionInventoryPotionDataLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("invalid-inventory-potion-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            player.getInventory().getHotbar()[0] = new ItemStack(ItemType.STICK, 5);
            DayCycleManager dayCycle = new DayCycleManager();

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteral(Files.readString(levelPath),
                    "\"itemId\": 280,", "\"itemId\": 280,"
                            + System.lineSeparator()
                            + "          \"potion\": {\"type\":\"POISON\",\"splash\":true,"
                            + "\"extended\":false,\"enhanced\":false},"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertNull(result.levelData().inventory.hotbar[0].potion);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Potion metadata on non-potion dropped items without backup should be reported as corrupt")
    void nonPotionDroppedItemPotionDataWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-dropped-potion-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.spawnThrownStack(1.0f, 80.0f, 1.0f, new ItemStack(ItemType.DIAMOND, 1), 0.0f, 0.0f, 0.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteral(Files.readString(levelPath),
                    "\"itemId\": 264,", "\"itemId\": 264,"
                            + System.lineSeparator()
                            + "      \"potion\": {\"type\":\"POISON\",\"splash\":true,"
                            + "\"extended\":false,\"enhanced\":false},"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid potion data in dropped item"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid saved potion combinations should load the previous backup")
    void invalidPotionCombinationLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("invalid-potion-combination-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            ItemStack potion = new ItemStack(ItemType.POTION, 1);
            potion.setPotionData(new PotionData(PotionType.SWIFTNESS, false, true, false));
            player.getInventory().getHotbar()[0] = potion;
            DayCycleManager dayCycle = new DayCycleManager();

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"itemId\": 373,", "\"enhanced\": false", "\"enhanced\": true"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(new PotionData(PotionType.SWIFTNESS, false, true, false),
                    result.levelData().inventory.hotbar[0].potion);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid active splash-potion data without backup should be reported as corrupt")
    void invalidSplashPotionEntityPotionWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-splash-potion-entity-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.replaceEntities(List.of(new SplashPotionEntity(1.0f, 80.0f, 1.0f,
                    0.0f, 0.1f, 0.0f, null,
                    new PotionData(PotionType.FIRE_RESISTANCE, true, false, false))));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"SPLASH_POTION\"", "\"enhanced\": false", "\"enhanced\": true"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid potion data in splash potion"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Over-max saved inventory stacks should load the previous backup")
    void overMaxInventoryStackCountLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("over-max-inventory-count-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            player.getInventory().getHotbar()[0] = new ItemStack(ItemType.STICK, 5);
            DayCycleManager dayCycle = new DayCycleManager();

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"itemId\": 280,", "\"count\": 5", "\"count\": 65"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(5, result.levelData().inventory.hotbar[0].count);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Over-max saved dropped-item stacks without backup should be reported as corrupt")
    void overMaxDroppedItemStackCountWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("over-max-dropped-count-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.spawnThrownStack(1.0f, 80.0f, 1.0f, new ItemStack(ItemType.DIAMOND, 1),
                    0.0f, 0.0f, 0.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"itemId\": 264,", "\"count\": 1", "\"count\": 65"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid item count in dropped item"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Over-max saved tool durability should load the previous backup")
    void overMaxInventoryDurabilityLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("over-max-inventory-durability-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            player.getInventory().getHotbar()[0] = new ItemStack(ItemType.STONE_SWORD, 1);
            DayCycleManager dayCycle = new DayCycleManager();

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            int maxDurability = ItemType.STONE_SWORD.getMaxDurability();
            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"itemId\": 272,",
                    "\"durability\": " + maxDurability,
                    "\"durability\": " + (maxDurability + 1)));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(maxDurability, result.levelData().inventory.hotbar[0].durability);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Non-damageable dropped-item durability without backup should be reported as corrupt")
    void nonDamageableDroppedItemDurabilityWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("non-damageable-dropped-durability-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.spawnThrownStack(1.0f, 80.0f, 1.0f, new ItemStack(ItemType.DIAMOND, 1),
                    0.0f, 0.0f, 0.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"itemId\": 264,", "\"durability\": -1", "\"durability\": 0"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid item durability in dropped item"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Wrong saved armor slot items should load the previous backup")
    void wrongArmorSlotItemLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("wrong-armor-slot-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            player.getInventory().getArmor()[0] = new ItemStack(ItemType.IRON_HELMET, 1);
            DayCycleManager dayCycle = new DayCycleManager();

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"armor\": [", "\"itemId\": 306", "\"itemId\": 309"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(306, result.levelData().inventory.armor[0].itemId);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Wrong saved armor slot items without backup should be reported as corrupt")
    void wrongArmorSlotItemWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("wrong-armor-slot-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            player.getInventory().getArmor()[0] = new ItemStack(ItemType.IRON_HELMET, 1);
            DayCycleManager dayCycle = new DayCycleManager();
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"armor\": [", "\"itemId\": 306", "\"itemId\": 309"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid armor slot item"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid thrown-item projectile items should load the previous backup")
    void invalidThrownItemProjectileLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("invalid-thrown-item-projectile-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.replaceEntities(List.of(new ThrownItemEntity(1.0f, 80.0f, 1.0f,
                    0.0f, 0.1f, 0.0f, ItemType.EGG, null)));

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"THROWN_ITEM\"", "\"projectileItemId\": 344", "\"projectileItemId\": 280"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(344, result.levelData().entities.get(0).projectileItemId);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid thrown-item projectile items without backup should be reported as corrupt")
    void invalidThrownItemProjectileWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-thrown-item-projectile-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.replaceEntities(List.of(new ThrownItemEntity(1.0f, 80.0f, 1.0f,
                    0.0f, 0.1f, 0.0f, ItemType.EGG, null)));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"THROWN_ITEM\"", "\"projectileItemId\": 344", "\"projectileItemId\": 280"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid thrown item projectile"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid falling block entities should load the previous backup")
    void invalidFallingBlockEntityLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("invalid-falling-block-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            FallingBlockEntity falling = new FallingBlockEntity(BlockType.SAND, 0);
            falling.setPosition(1.0f, 80.0f, 1.0f);
            world.replaceEntities(List.of(falling));

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"FALLING_BLOCK\"", "\"fallingBlockId\": 12", "\"fallingBlockId\": 1"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(12, result.levelData().entities.get(0).fallingBlockId);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid falling block entities without backup should be reported as corrupt")
    void invalidFallingBlockEntityWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-falling-block-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            FallingBlockEntity falling = new FallingBlockEntity(BlockType.SAND, 0);
            falling.setPosition(1.0f, 80.0f, 1.0f);
            world.replaceEntities(List.of(falling));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"FALLING_BLOCK\"", "\"fallingBlockId\": 12", "\"fallingBlockId\": 1"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid falling block entity"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid minecart kinds should load the previous backup")
    void invalidMinecartKindLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("invalid-minecart-kind-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            FurnaceMinecartEntity cart = new FurnaceMinecartEntity(1.0f, 80.0f, 1.0f);
            world.replaceEntities(List.of(cart));

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"MINECART\"", "\"cartKind\": \"FURNACE\"", "\"cartKind\": \"HOPPER\""));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals("FURNACE", result.levelData().entities.get(0).cartKind);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Hidden inventory on non-chest minecarts without backup should be reported as corrupt")
    void nonChestMinecartInventoryWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("non-chest-minecart-inventory-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.replaceEntities(List.of(new MinecartEntity(1.0f, 80.0f, 1.0f,
                    MinecartEntity.CartKind.RIDEABLE)));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"MINECART\"",
                    "\"cartKind\": \"RIDEABLE\"",
                    "\"cartKind\": \"RIDEABLE\","
                            + System.lineSeparator()
                            + "      \"inventory\": []"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid minecart inventory"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid painting art should load the previous backup")
    void invalidPaintingArtLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("invalid-painting-art-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.replaceEntities(List.of(PaintingEntity.fromSupport(1, 80, 1,
                    Block.FACE_EAST, PaintingEntity.Art.MATCH)));

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"PAINTING\"", "\"paintingArt\": \"Match\"", "\"paintingArt\": \"Wither\""));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals("Match", result.levelData().entities.get(0).paintingArt);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid painting facing without backup should be reported as corrupt")
    void invalidPaintingFacingWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-painting-facing-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.replaceEntities(List.of(PaintingEntity.fromSupport(1, 80, 1,
                    Block.FACE_EAST, PaintingEntity.Art.MATCH)));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"PAINTING\"", "\"paintingFacing\": 4", "\"paintingFacing\": 0"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid painting facing"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Null saved player status effects should load the previous backup")
    void nullPlayerStatusEffectLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("null-player-status-effect-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            player.getStats().addEffect(new StatusEffectInstance(StatusEffectType.POISON, 80, 0));
            DayCycleManager dayCycle = new DayCycleManager();

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, insertNullIntoJsonArray(Files.readString(levelPath), "\"activeEffects\": ["));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(1, result.levelData().player.activeEffects.size());
            assertSame(StatusEffectType.POISON, result.levelData().player.activeEffects.get(0).type());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Expired saved mob status effects without backup should be reported as corrupt")
    void expiredMobStatusEffectWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("expired-mob-status-effect-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Zombie zombie = new Zombie();
            zombie.setPosition(2.0f, 80.0f, 2.0f);
            zombie.addEffect(new StatusEffectInstance(StatusEffectType.STRENGTH, 80, 0));
            world.replaceEntities(List.of(zombie));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"ZOMBIE\"", "\"durationTicks\": 80", "\"durationTicks\": 0"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid status effect duration in ZOMBIE effects"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid experience orb values should load the previous backup")
    void invalidExperienceOrbValueLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("invalid-experience-orb-value-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            ExperienceOrbEntity orb = new ExperienceOrbEntity(1.0f, 80.0f, 1.0f, 17);
            world.replaceEntities(List.of(orb));

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"EXPERIENCE_ORB\"", "\"experienceValue\": 17", "\"experienceValue\": 0"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(17, result.levelData().entities.get(0).experienceValue);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Negative experience orb pickup delay without backup should be reported as corrupt")
    void negativeExperienceOrbPickupDelayWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("negative-experience-orb-delay-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            ExperienceOrbEntity orb = new ExperienceOrbEntity(1.0f, 80.0f, 1.0f, 17);
            orb.setPickupDelayTicks(6);
            world.replaceEntities(List.of(orb));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"EXPERIENCE_ORB\"", "\"pickupDelayTicks\": 6", "\"pickupDelayTicks\": -1"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid experience orb pickup delay"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Zero-health experience orb without backup should be reported as corrupt")
    void zeroHealthExperienceOrbWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("zero-health-experience-orb-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            ExperienceOrbEntity orb = new ExperienceOrbEntity(1.0f, 80.0f, 1.0f, 17);
            world.replaceEntities(List.of(orb));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"EXPERIENCE_ORB\"", "\"orbHealth\": 5", "\"orbHealth\": 0"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid experience orb health"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Overfull experience orb health without backup should be reported as corrupt")
    void overfullExperienceOrbHealthWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("overfull-health-experience-orb-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            ExperienceOrbEntity orb = new ExperienceOrbEntity(1.0f, 80.0f, 1.0f, 17);
            world.replaceEntities(List.of(orb));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"EXPERIENCE_ORB\"", "\"orbHealth\": 5",
                    "\"orbHealth\": " + (ExperienceOrbEntity.MAX_HEALTH + 1)));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid experience orb health"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Expired experience orb age without backup should be reported as corrupt")
    void expiredExperienceOrbAgeWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("expired-experience-orb-age-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            ExperienceOrbEntity orb = new ExperienceOrbEntity(1.0f, 80.0f, 1.0f, 17);
            orb.setTicksExisted(5999);
            world.replaceEntities(List.of(orb));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"EXPERIENCE_ORB\"", "\"age\": 5999", "\"age\": 6000"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid experience orb age"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Expired dropped item age without backup should be reported as corrupt")
    void expiredDroppedItemAgeWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("expired-dropped-item-age-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            DroppedItem item = new DroppedItem(1.0f, 80.0f, 1.0f, ItemType.DIRT, 3);
            item.setAge(DroppedItem.DESPAWN_TIME_SECONDS - 0.5f);
            world.replaceDroppedItems(List.of(item));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"itemId\": 3", "\"age\": 299.5", "\"age\": 300.0"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid dropped item age"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Overfull dropped item health without backup should be reported as corrupt")
    void overfullDroppedItemHealthWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("overfull-dropped-item-health-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.replaceDroppedItems(List.of(new DroppedItem(1.0f, 80.0f, 1.0f, ItemType.DIRT, 3)));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"itemId\": 3", "\"health\": 5", "\"health\": 6"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid dropped item health"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid arrow runtime fields should load the previous backup")
    void invalidArrowRuntimeFieldLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("invalid-arrow-runtime-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            ArrowEntity arrow = new ArrowEntity(1.0f, 80.0f, 1.0f,
                    0.0f, 0.1f, 0.0f, null, true, 4.0f);
            arrow.setFireTicksOnHit(60);
            world.replaceEntities(List.of(arrow));

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"ARROW\"", "\"fireTicksOnHit\": 60", "\"fireTicksOnHit\": -1"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(60, result.levelData().entities.get(0).fireTicksOnHit);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Expired stuck arrow ticks without backup should be reported as corrupt")
    void expiredStuckArrowTicksWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("expired-stuck-arrow-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            ArrowEntity arrow = new ArrowEntity(1.0f, 80.0f, 1.0f,
                    0.0f, 0.1f, 0.0f, null, true, 4.0f);
            arrow.setStuckInBlock(1, 80, 1, ArrowEntity.STUCK_DESPAWN_TICKS - 1);
            world.replaceEntities(List.of(arrow));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"ARROW\"", "\"stuckTicks\": 1199", "\"stuckTicks\": 1200"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid arrow stuck ticks"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Airborne arrow stuck ticks without backup should be reported as corrupt")
    void airborneArrowStuckTicksWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("airborne-arrow-stuck-ticks-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            ArrowEntity arrow = new ArrowEntity(1.0f, 80.0f, 1.0f,
                    0.0f, 0.1f, 0.0f, null, true, 4.0f);
            world.replaceEntities(List.of(arrow));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"ARROW\"", "\"stuckTicks\": 0", "\"stuckTicks\": 1"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid arrow stuck state"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Expired Eye of Ender age without backup should be reported as corrupt")
    void expiredEyeOfEnderAgeWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("expired-eye-of-ender-age-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            EyeOfEnderEntity eye = new EyeOfEnderEntity(1.0f, 80.0f, 1.0f,
                    16.0f, 88.0f, -8.0f, true);
            eye.setTicksExisted(EyeOfEnderEntity.LIFE_TICKS - 1);
            world.replaceEntities(List.of(eye));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"EYE_OF_ENDER\"", "\"age\": 79", "\"age\": 81"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid Eye of Ender age"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Expired Ender pearl age without backup should be reported as corrupt")
    void expiredEnderPearlAgeWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("expired-ender-pearl-age-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            EnderPearlEntity pearl = new EnderPearlEntity(1.0f, 80.0f, 1.0f,
                    0.0f, 0.1f, 0.0f, player);
            pearl.setTicksExisted(EnderPearlEntity.DESPAWN_TICKS - 1);
            world.replaceEntities(List.of(pearl));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"ENDER_PEARL\"", "\"age\": 1199", "\"age\": 1200"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid Ender pearl age"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ownerless Ender pearl without backup should be reported as corrupt")
    void ownerlessEnderPearlWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("ownerless-ender-pearl-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            EnderPearlEntity pearl = new EnderPearlEntity(1.0f, 80.0f, 1.0f,
                    0.0f, 0.1f, 0.0f, player);
            world.replaceEntities(List.of(pearl));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"ENDER_PEARL\"",
                    "\"ownerPlayer\": true",
                    "\"ownerPlayer\": false"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid Ender pearl owner"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Expired thrown item age without backup should be reported as corrupt")
    void expiredThrownItemAgeWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("expired-thrown-item-age-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            ThrownItemEntity egg = new ThrownItemEntity(1.0f, 80.0f, 1.0f,
                    0.0f, 0.1f, 0.0f, ItemType.EGG, null);
            egg.setTicksExisted(ThrownItemEntity.DESPAWN_TICKS - 1);
            world.replaceEntities(List.of(egg));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"THROWN_ITEM\"", "\"age\": 1199", "\"age\": 1200"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid thrown item age"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Expired splash potion age without backup should be reported as corrupt")
    void expiredSplashPotionAgeWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("expired-splash-potion-age-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            SplashPotionEntity potion = new SplashPotionEntity(1.0f, 80.0f, 1.0f,
                    0.0f, 0.1f, 0.0f, null,
                    new PotionData(PotionType.POISON, true, false, false));
            potion.setTicksExisted(SplashPotionEntity.DESPAWN_TICKS - 1);
            world.replaceEntities(List.of(potion));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"SPLASH_POTION\"", "\"age\": 1199", "\"age\": 1200"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid splash potion age"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Expired fireball age without backup should be reported as corrupt")
    void expiredFireballAgeWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("expired-fireball-age-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            FireballEntity fireball = new FireballEntity(1.0f, 80.0f, 1.0f,
                    0.0f, 0.1f, 0.0f, null, true);
            fireball.setTicksExisted(FireballEntity.DESPAWN_TICKS);
            world.replaceEntities(List.of(fireball));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"FIREBALL\"", "\"age\": 600", "\"age\": 601"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid fireball age"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid fishing hook catch windows without backup should be reported as corrupt")
    void invalidFishingHookCatchWindowWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-fishing-hook-window-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                    new ItemStack(ItemType.FISHING_ROD, 1);
            DayCycleManager dayCycle = new DayCycleManager();
            FishingHookEntity hook = new FishingHookEntity(1.0f, 80.0f, 1.0f,
                    0.0f, 0.1f, 0.0f, player);
            hook.setCatchableTicks(8);
            world.replaceEntities(List.of(hook));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"FISHING_HOOK\"",
                    "\"fishingCatchableTicks\": 8",
                    "\"fishingCatchableTicks\": 40"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid fishing hook catchable ticks"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Expired fishing hook age without backup should be reported as corrupt")
    void expiredFishingHookAgeWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("expired-fishing-hook-age-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                    new ItemStack(ItemType.FISHING_ROD, 1);
            DayCycleManager dayCycle = new DayCycleManager();
            FishingHookEntity hook = new FishingHookEntity(1.0f, 80.0f, 1.0f,
                    0.0f, 0.1f, 0.0f, player);
            hook.setTicksExisted(FishingHookEntity.DESPAWN_TICKS - 1);
            world.replaceEntities(List.of(hook));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"FISHING_HOOK\"",
                    "\"age\": " + (FishingHookEntity.DESPAWN_TICKS - 1),
                    "\"age\": " + FishingHookEntity.DESPAWN_TICKS));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid fishing hook age"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Impossible fishing hook phase without backup should be reported as corrupt")
    void impossibleFishingHookPhaseWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("impossible-fishing-hook-phase-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                    new ItemStack(ItemType.FISHING_ROD, 1);
            DayCycleManager dayCycle = new DayCycleManager();
            FishingHookEntity hook = new FishingHookEntity(1.0f, 80.0f, 1.0f,
                    0.0f, 0.1f, 0.0f, player);
            hook.restoreFishingState(37, 0, false);
            world.replaceEntities(List.of(hook));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"FISHING_HOOK\"",
                    "\"fishingCatchableTicks\": 0",
                    "\"fishingCatchableTicks\": 8"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid fishing hook phase"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ownerless fishing hook without backup should be reported as corrupt")
    void ownerlessFishingHookWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("ownerless-fishing-hook-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                    new ItemStack(ItemType.FISHING_ROD, 1);
            DayCycleManager dayCycle = new DayCycleManager();
            FishingHookEntity hook = new FishingHookEntity(1.0f, 80.0f, 1.0f,
                    0.0f, 0.1f, 0.0f, player);
            world.replaceEntities(List.of(hook));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"FISHING_HOOK\"",
                    "\"ownerPlayer\": true",
                    "\"ownerPlayer\": false"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid fishing hook owner"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Missing fishing hook target without backup should be reported as corrupt")
    void missingFishingHookTargetWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("missing-fishing-hook-target-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                    new ItemStack(ItemType.FISHING_ROD, 1);
            DayCycleManager dayCycle = new DayCycleManager();
            Zombie zombie = new Zombie();
            zombie.setPosition(4.0f, 80.0f, 1.0f);
            FishingHookEntity hook = new FishingHookEntity(4.0f, 81.4f, 1.0f,
                    0.0f, 0.0f, 0.0f, player);
            hook.restoreHookedEntity(zombie);
            world.replaceEntities(List.of(zombie, hook));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"FISHING_HOOK\"",
                    "\"fishingHookedEntitySaveId\": 1",
                    "\"fishingHookedEntitySaveId\": 99"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid fishing hook target"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Missing mob combat target without backup should be reported as corrupt")
    void missingMobCombatTargetWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("missing-mob-target-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Wolf wolf = new Wolf();
            wolf.setPosition(0.0f, 80.0f, 0.0f);
            wolf.setTamed(true);
            wolf.setOwnerName("Steve");
            Zombie zombie = new Zombie();
            zombie.setPosition(1.2f, 80.0f, 0.0f);
            world.replaceEntities(List.of(wolf, zombie));
            assertTrue(wolf.setAssistTarget(zombie));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"WOLF\"",
                    "\"mobTargetSaveId\": 2",
                    "\"mobTargetSaveId\": 99"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid mob target"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Missing spider jockey rider without backup should be reported as corrupt")
    void missingSpiderJockeyRiderWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("missing-spider-jockey-rider-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Spider spider = new Spider();
            spider.setPosition(0.0f, 80.0f, 0.0f);
            Skeleton skeleton = new Skeleton();
            skeleton.setPosition(0.0f, 81.0f, 0.0f);
            world.replaceEntities(List.of(spider, skeleton));
            assertTrue(spider.mountJockey(skeleton));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"SPIDER\"",
                    "\"spiderJockeyRiderSaveId\": 2",
                    "\"spiderJockeyRiderSaveId\": 99"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid spider jockey rider"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Duplicate saved entity reference ids without backup should be reported as corrupt")
    void duplicateEntityReferenceIdWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("duplicate-entity-reference-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Zombie first = new Zombie();
            first.setPosition(0.0f, 80.0f, 0.0f);
            Zombie second = new Zombie();
            second.setPosition(1.2f, 80.0f, 0.0f);
            world.replaceEntities(List.of(first, second));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteral(Files.readString(levelPath),
                    "\"entitySaveId\": 2",
                    "\"entitySaveId\": 1"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("duplicate entity reference id"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid primed TNT fuse values without backup should be reported as corrupt")
    void invalidPrimedTntFuseWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-tnt-fuse-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.replaceEntities(List.of(new PrimedTntEntity(1.0f, 80.0f, 1.0f, 10)));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"PRIMED_TNT\"", "\"fuseTicks\": 10", "\"fuseTicks\": -1"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid primed TNT fuse"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid minecart damage without backup should be reported as corrupt")
    void invalidMinecartDamageWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-minecart-damage-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.replaceEntities(List.of(new MinecartEntity(1.0f, 80.0f, 1.0f,
                    MinecartEntity.CartKind.RIDEABLE)));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"MINECART\"", "\"cartDamage\": 0.0", "\"cartDamage\": -1.0"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid minecart damage"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid mob health should load the previous backup")
    void invalidMobHealthLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("invalid-mob-health-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Zombie zombie = new Zombie();
            zombie.setPosition(1.0f, 80.0f, 1.0f);
            zombie.setHealth(12.0f);
            world.replaceEntities(List.of(zombie));

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"ZOMBIE\"", "\"health\": 12.0", "\"health\": 99.0"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(12.0f, result.levelData().entities.get(0).health, 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Zero-health non-dragon mob payload without backup should be reported as corrupt")
    void zeroHealthNonDragonMobWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("zero-health-mob-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Zombie zombie = new Zombie();
            zombie.setPosition(1.0f, 80.0f, 1.0f);
            zombie.setHealth(12.0f);
            world.replaceEntities(List.of(zombie));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"ZOMBIE\"", "\"health\": 12.0", "\"health\": 0.0"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid mob health"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid mob growing age without backup should be reported as corrupt")
    void invalidMobGrowingAgeWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-mob-growing-age-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Pig pig = new Pig();
            pig.setPosition(1.0f, 80.0f, 1.0f);
            pig.setGrowingAge(Mob.BABY_GROWING_AGE);
            world.replaceEntities(List.of(pig));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"PIG\"", "\"growingAge\": -24000", "\"growingAge\": -24001"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid mob growing age"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Impossible saved animal love state without backup should be reported as corrupt")
    void impossibleSavedAnimalLoveStateWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-mob-love-state-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Pig pig = new Pig();
            pig.setPosition(1.0f, 80.0f, 1.0f);
            pig.setGrowingAge(Mob.BABY_GROWING_AGE);
            world.replaceEntities(List.of(pig));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            String withLoveTicks = replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"PIG\"", "\"loveTicks\": 0", "\"loveTicks\": 1");
            Files.writeString(levelPath, withLoveTicks);

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid mob breeding state"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid End crystal health without backup should be reported as corrupt")
    void invalidEndCrystalHealthWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-end-crystal-health-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.replaceEntities(List.of(new EndCrystalEntity(1.0f, 80.0f, 1.0f)));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"END_CRYSTAL\"", "\"health\": 5.0", "\"health\": 6.0"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid end crystal health"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Zero-health End crystal save payload should be reported as corrupt")
    void zeroHealthEndCrystalWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("zero-health-end-crystal-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.replaceEntities(List.of(new EndCrystalEntity(1.0f, 80.0f, 1.0f)));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"END_CRYSTAL\"", "\"health\": 5.0", "\"health\": 0.0"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid end crystal health"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid Creeper fuse without backup should be reported as corrupt")
    void invalidCreeperFuseWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-creeper-fuse-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Creeper creeper = new Creeper();
            creeper.setPosition(1.0f, 80.0f, 1.0f);
            creeper.setFuseState(10, true);
            world.replaceEntities(List.of(creeper));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"CREEPER\"", "\"creeperFuseTicks\": 10", "\"creeperFuseTicks\": 31"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid creeper fuse"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid Slime sizes without backup should be reported as corrupt")
    void invalidSlimeSizeWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-slime-size-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Slime slime = new Slime(4);
            slime.setPosition(1.0f, 80.0f, 1.0f);
            world.replaceEntities(List.of(slime));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"SLIME\"", "\"slimeSize\": 4", "\"slimeSize\": 3"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid slime size"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid furnace progress without backup should be reported as corrupt")
    void invalidFurnaceProgressWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-furnace-progress-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.setBlock(1, 70, 1, BlockType.FURNACE);
            FurnaceTileEntity furnace = (FurnaceTileEntity) world.getTileEntity(1, 70, 1);
            furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT] = new ItemStack(ItemType.IRON_ORE, 1);
            furnace.setCookTime(80);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"type\": \"furnace\"", "\"cookTime\": 80", "\"cookTime\": -1"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid furnace cook time"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid scheduled block tick delays without backup should be reported as corrupt")
    void invalidScheduledBlockTickDelayWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-scheduled-tick-delay-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.setSaveManager(manager);
            world.setBlock(8, 70, 8, BlockType.GLOWING_REDSTONE_ORE);
            assertTrue(world.hasScheduledBlockTick(8, 70, 8, BlockType.GLOWING_REDSTONE_ORE));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"scheduledBlockTicks\": [", "\"delayTicks\": 30", "\"delayTicks\": -1"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid scheduled block tick delay"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Invalid moving piston snapshots without backup should be reported as corrupt")
    void invalidMovingPistonFacingWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("invalid-moving-piston-facing-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.setSaveManager(manager);
            world.setBlock(0, 100, 0, BlockType.PISTON, Block.FACE_EAST);
            world.setBlock(1, 100, 0, BlockType.STONE, 0);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);
            world.advanceBlockTicks(1);
            assertSame(BlockType.MOVING_PISTON, world.getBlock(1, 100, 0));
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"movingPistons\": [", "\"facing\": 4", "\"facing\": 99"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid moving piston facing"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Null saved enchantments on inventory items should load the previous backup")
    void nullInventoryEnchantmentLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("null-inventory-enchantment-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            player.getInventory().getHotbar()[0] = new ItemStack(ItemType.STONE_SWORD, 1);
            DayCycleManager dayCycle = new DayCycleManager();

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"itemId\": 272,", "\"enchantments\": []", "\"enchantments\": [null]"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertTrue(result.levelData().inventory.hotbar[0].enchantments.isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Null saved enchantments on dropped items without backup should be reported as corrupt")
    void nullDroppedItemEnchantmentWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("null-dropped-enchantment-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.spawnThrownStack(1.0f, 80.0f, 1.0f, new ItemStack(ItemType.DIAMOND_SWORD, 1),
                    0.0f, 0.0f, 0.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"itemId\": 276,", "\"enchantments\": []", "\"enchantments\": [null]"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid enchantment in dropped item"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Post-1.0 bow enchantments should load the previous backup")
    void bowEnchantmentLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("bow-enchantment-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            player.getInventory().getHotbar()[0] = new ItemStack(ItemType.BOW, 1);
            DayCycleManager dayCycle = new DayCycleManager();

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteralAfter(Files.readString(levelPath),
                    "\"itemId\": 261,",
                    "\"enchantments\": []",
                    "\"enchantments\": [{\"type\":\"POWER\",\"level\":2}]"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertTrue(result.levelData().inventory.hotbar[0].enchantments.isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Incompatible dropped-item enchantments without backup should be reported as corrupt")
    void incompatibleDroppedItemEnchantmentsWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("incompatible-dropped-enchantments-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            ItemStack pickaxe = new ItemStack(ItemType.DIAMOND_PICKAXE, 1);
            pickaxe.addEnchantment(new EnchantmentInstance(EnchantmentType.SILK_TOUCH, 1));
            pickaxe.addEnchantment(new EnchantmentInstance(EnchantmentType.FORTUNE, 3));
            world.spawnThrownStack(1.0f, 80.0f, 1.0f, pickaxe, 0.0f, 0.0f, 0.0f);
            manager.save(world, player, dayCycle);

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid enchantment combination in dropped item"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Null structured save entries should load the previous backup")
    void nullEntityListEntryLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("null-entity-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Zombie zombie = new Zombie();
            zombie.setPosition(2.0f, 80.0f, 2.0f);
            world.replaceEntities(List.of(zombie));

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, insertNullIntoJsonArray(Files.readString(levelPath), "\"entities\": ["));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(1, result.levelData().entities.size());
            assertEquals("ZOMBIE", result.levelData().entities.get(0).type);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Null structured save entries without backup should be reported as corrupt")
    void nullEntityListEntryWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("null-entity-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Zombie zombie = new Zombie();
            zombie.setPosition(2.0f, 80.0f, 2.0f);
            world.replaceEntities(List.of(zombie));

            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, insertNullIntoJsonArray(Files.readString(levelPath), "\"entities\": ["));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("invalid entity list"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Unknown saved entity types should load the previous backup")
    void unknownEntityTypeLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("unknown-entity-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Zombie zombie = new Zombie();
            zombie.setPosition(2.0f, 80.0f, 2.0f);
            world.replaceEntities(List.of(zombie));

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteral(Files.readString(levelPath),
                    "\"type\": \"ZOMBIE\"", "\"type\": \"NO_SUCH_ENTITY\""));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(1, result.levelData().entities.size());
            assertEquals("ZOMBIE", result.levelData().entities.get(0).type);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Unknown saved entity types without backup should be reported as corrupt")
    void unknownEntityTypeWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("unknown-entity-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Zombie zombie = new Zombie();
            zombie.setPosition(2.0f, 80.0f, 2.0f);
            world.replaceEntities(List.of(zombie));

            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteral(Files.readString(levelPath),
                    "\"type\": \"ZOMBIE\"", "\"type\": \"NO_SUCH_ENTITY\""));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("unknown entity type"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Unknown saved tile entity types should load the previous backup")
    void unknownTileEntityTypeLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("unknown-tile-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.setBlock(2, 70, 1, BlockType.CHEST, 4);

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteral(Files.readString(levelPath),
                    "\"type\": \"chest\"", "\"type\": \"mystery_tile\""));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(1, result.levelData().tileEntities.size());
            assertEquals("chest", result.levelData().tileEntities.get(0).type);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Malformed player inventory stacks should load the previous backup")
    void malformedInventoryStackLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("malformed-inventory-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            player.getInventory().getHotbar()[0] = new ItemStack(ItemType.DIAMOND, 3);
            DayCycleManager dayCycle = new DayCycleManager();

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteral(Files.readString(levelPath),
                    "\"itemId\": " + ItemType.DIAMOND.getId(), "\"itemId\": 99999"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(ItemType.DIAMOND.getId(), result.levelData().inventory.hotbar[0].itemId);
            assertEquals(3, result.levelData().inventory.hotbar[0].count);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Malformed player inventory stacks without backup should be reported as corrupt")
    void malformedInventoryStackWithoutBackupIsCorrupt() throws Exception {
        Path worldDir = tempDir.resolve("malformed-inventory-no-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            player.getInventory().getHotbar()[0] = new ItemStack(ItemType.DIAMOND, 3);
            DayCycleManager dayCycle = new DayCycleManager();

            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteral(Files.readString(levelPath),
                    "\"itemId\": " + ItemType.DIAMOND.getId(), "\"itemId\": 99999"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.CORRUPT, result.status());
            assertNotNull(result.error());
            assertTrue(result.error().message().contains("unknown item in hotbar inventory[0]"));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Malformed dropped item payloads should load the previous backup")
    void malformedDroppedItemPayloadLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("malformed-dropped-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.spawnDroppedItem(2.0f, 80.0f, 2.0f, ItemType.DIAMOND, 2);

            dayCycle.setTime(100.0f);
            manager.save(world, player, dayCycle);
            dayCycle.setTime(200.0f);
            manager.save(world, player, dayCycle);

            Path levelPath = worldDir.resolve("level.json");
            Files.writeString(levelPath, replaceFirstLiteral(Files.readString(levelPath),
                    "\"itemId\": " + ItemType.DIAMOND.getId(), "\"itemId\": 99999"));

            SaveManager.SaveLoadResult result = manager.loadLevel();
            assertEquals(SaveManager.SaveLoadStatus.LOADED, result.status());
            assertNotNull(result.levelData());
            assertEquals(100.0f, result.levelData().time, 0.001f);
            assertEquals(1, result.levelData().droppedItems.size());
            assertEquals(ItemType.DIAMOND.getId(), result.levelData().droppedItems.get(0).itemId);
            assertEquals(2, result.levelData().droppedItems.get(0).count);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Corrupt chunk files should load the previous chunk backup")
    void corruptChunkLoadsBackup() throws Exception {
        Path worldDir = tempDir.resolve("chunk-backup-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(99L);
        try {
            Player player = new Player(1.0f, 80.0f, 1.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.setBlock(1, 70, 1, BlockType.DIAMOND_ORE);
            manager.save(world, player, dayCycle);

            world.setBlock(1, 70, 1, BlockType.GOLD_ORE);
            manager.save(world, player, dayCycle);

            Path chunkPath = worldDir.resolve("chunks").resolve("c.0.0.bin");
            assertTrue(Files.exists(SafeFiles.backupPath(chunkPath)));
            Files.writeString(chunkPath, "bad chunk");

            Chunk restored = new Chunk(0, 0);
            assertTrue(manager.loadChunkIfExists(restored));
            assertSame(BlockType.DIAMOND_ORE, restored.getBlock(1, 70, 1));
            assertEquals(0, restored.getBlockMetadata(1, 70, 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Save manager should create, load, and restore the default world state")
    void saveManagerRoundTripsWorldState() throws Exception {
        Path worldDir = tempDir.resolve("default");
        SaveManager manager = new SaveManager(worldDir);
        assertFalse(manager.hasSave());
        assertNull(manager.loadLevelIfExists());

        World world = new World(123456789L);
        World restoredWorld = null;
        try {
            Player player = new Player(11.25f, 72.5f, -6.75f);
            player.getCamera().setYaw(135.0f);
            player.getCamera().setPitch(-12.5f);
            player.getInventory().setSelectedSlot(2);
            player.getInventory().getHotbar()[0] = damagedTool(ItemType.IRON_PICKAXE, 3);
            player.getInventory().getHotbar()[0].addEnchantment(new EnchantmentInstance(EnchantmentType.EFFICIENCY, 3));
            player.getInventory().getHotbar()[1] = damagedTool(ItemType.BOW, 2);
            player.getInventory().getHotbar()[1].putMetadata("test", "bow");
            player.getInventory().getMainInventory()[4] = new ItemStack(ItemType.CHARCOAL, 7);
            player.getInventory().getCraftingGrid()[0] = new ItemStack(ItemType.OAK_PLANKS, 2);
            player.getInventory().getArmor()[0] = damagedTool(ItemType.IRON_HELMET, 1);
            player.getInventory().setCursorItem(new ItemStack(ItemType.STICK, 5));
            player.getStats().restore(14.0f, 16.0f, 4.0f, 9.0f, 3.75f);
            player.getStats().getProgression().restore(120, 42);
            player.getStats().getStatistics().restore(2400, 9876, 12, 34, 5, 135, 80, 1, 6, 4, 44, 9, 7, 2,
                    Map.of(BlockType.STONE, 21L, BlockType.OAK_LOG, 13L),
                    Map.of(ItemType.OAK_LOG, 12L, ItemType.DIAMOND, 32L),
                    Map.of(ItemType.CRAFTING_TABLE, 4L, ItemType.STICK, 5L),
                    Map.of(ItemType.FISHING_ROD, 7L),
                    Map.of(ItemType.FISHING_ROD, 2L));
            player.getStats().getStatistics().restoreTravelDistances(123, 456, 789, 1011, 1213, 1415, 1617, 1819);
            player.getStats().getStatistics().restoreGamesQuit(4);
            player.getStats().getStatistics().restoreFishCaught(3);
            player.getStats().getStatistics().restorePlayerKills(2);
            player.getStats().getStatistics().restoreItemsDropped(5);
            player.getStats().getStatistics().restoreItemsDroppedByType(Map.of(ItemType.DIAMOND, 3L, ItemType.STICK, 2L));
            player.getStats().getAchievements().recordInventoryOpened();
            player.getStats().getAchievements().recordCollectedItem(ItemType.OAK_LOG);
            player.getStats().getAchievements().recordCrafted(ItemType.CRAFTING_TABLE);
            player.getStats().addEffect(new StatusEffectInstance(StatusEffectType.POISON, 80, 0));
            player.setFireTicks(37);
            player.setBedSpawnPosition(new BlockPos(30, 71, 30), 31.5f, 71.0f, 30.5f);

            DayCycleManager dayCycle = new DayCycleManager();
            dayCycle.setTime(18234.5f);

            world.setSaveManager(manager);
            world.setWeatherState("rain", 321, 654);
            world.setBlock(1, 70, 1, BlockType.BRICK);
            world.setBlock(2, 70, 1, BlockType.CHEST, 4);
            ChestTileEntity chest = (ChestTileEntity) world.getTileEntity(2, 70, 1);
            chest.getInventory()[0] = new ItemStack(ItemType.DIAMOND, 5);
            world.setBlock(3, 70, 1, BlockType.FURNACE, 5);
            FurnaceTileEntity furnace = (FurnaceTileEntity) world.getTileEntity(3, 70, 1);
            furnace.getInventory()[FurnaceTileEntity.SLOT_INPUT] = new ItemStack(ItemType.IRON_ORE, 2);
            furnace.getInventory()[FurnaceTileEntity.SLOT_FUEL] = new ItemStack(ItemType.CHARCOAL, 1);
            furnace.setBurnTime(1200);
            furnace.setCurrentFuelBurnTime(1600);
            furnace.setCookTime(80);
            furnace.setTickAccumulator(0.75f);
            world.setBlock(4, 69, 1, BlockType.STONE);
            world.setBlock(4, 70, 1, BlockType.STANDING_SIGN, 2);
            SignTileEntity sign = (SignTileEntity) world.getTileEntity(4, 70, 1);
            sign.setLine(0, "Release");
            sign.setLine(1, "One");
            world.setBlock(5, 70, 1, BlockType.DISPENSER, 2);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(5, 70, 1);
            dispenser.getInventory()[0] = new ItemStack(ItemType.ARROW, 12);
            dispenser.getInventory()[8] = new ItemStack(ItemType.SNOWBALL, 4);
            world.setBlock(6, 70, 1, BlockType.ENCHANTING_TABLE);
            world.setBlock(7, 70, 1, BlockType.BREWING_STAND, 1);
            BrewingStandTileEntity brewingStand = (BrewingStandTileEntity) world.getTileEntity(7, 70, 1);
            ItemStack waterBottle = new ItemStack(ItemType.POTION, 1);
            waterBottle.setPotionData(PotionData.water());
            brewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0] = waterBottle;
            brewingStand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT] = new ItemStack(ItemType.NETHER_WART, 1);
            brewingStand.setBrewTime(240);
            brewingStand.setTickAccumulator(0.5f);
            ItemStack thrownStack = damagedTool(ItemType.STONE_SWORD, 2);
            world.spawnThrownStack(3.5f, 73.0f, -2.5f, thrownStack, 0.25f, 0.5f, -0.25f);
            Zombie zombie = new Zombie();
            zombie.setPosition(8.5f, 71.0f, 8.5f);
            zombie.setMotion(0.05f, 0.0f, -0.02f);
            zombie.setYaw(90.0f);
            zombie.setPitch(-5.0f);
            zombie.setHealth(12.0f);
            zombie.setOnFire(40);
            world.replaceEntities(List.of(zombie));

            manager.save(world, player, dayCycle);

            assertTrue(manager.hasSave());
            assertTrue(Files.exists(worldDir.resolve("level.json")));
            assertTrue(Files.exists(worldDir.resolve("chunks").resolve("c.0.0.bin")));
            assertFalse(world.getChunkNow(0, 0).isModified());

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            assertEquals(SaveManager.FORMAT_VERSION, loaded.formatVersion);
            assertEquals(SaveManager.TARGET_VERSION, loaded.targetVersion);
            assertEquals(123456789L, loaded.seed);
            assertEquals(1, loaded.entities.size());
            assertEquals("rain", loaded.weatherState);
            assertEquals(321, loaded.weatherRainTime);
            assertEquals(654, loaded.weatherThunderTime);
            assertTrue(loaded.player.bedSpawnSet);
            assertEquals(30, loaded.player.bedSpawnX);
            assertEquals(71, loaded.player.bedSpawnY);
            assertEquals(30, loaded.player.bedSpawnZ);
            assertEquals(3.75f, loaded.player.exhaustion, 0.001f);
            assertEquals(37, loaded.player.fireTicks);
            assertEquals(4, loaded.player.statGamesQuit);
            assertEquals(3, loaded.player.statFishCaught);
            assertEquals(2, loaded.player.statPlayerKills);
            assertEquals(5, loaded.player.statItemsDropped);
            assertEquals(3L, loaded.player.statItemsDroppedByType.get(ItemType.DIAMOND));
            assertEquals(2L, loaded.player.statItemsDroppedByType.get(ItemType.STICK));
            assertEquals(List.of("openInventory", "mineWood", "buildWorkBench"), loaded.player.achievements);

            Player restoredPlayer = new Player(0, 64, 0);
            DayCycleManager restoredDayCycle = new DayCycleManager();
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);

            manager.applyLevel(loaded, restoredPlayer, restoredDayCycle, restoredWorld);

            assertEquals(11.25f, restoredPlayer.getPosition().x, 0.001f);
            assertEquals(72.5f, restoredPlayer.getPosition().y, 0.001f);
            assertEquals(-6.75f, restoredPlayer.getPosition().z, 0.001f);
            assertEquals(135.0f, restoredPlayer.getCamera().getYaw(), 0.001f);
            assertEquals(-12.5f, restoredPlayer.getCamera().getPitch(), 0.001f);
            assertEquals(14.0f, restoredPlayer.getStats().getHealth(), 0.001f);
            assertEquals(16.0f, restoredPlayer.getStats().getHunger(), 0.001f);
            assertEquals(4.0f, restoredPlayer.getStats().getSaturation(), 0.001f);
            assertEquals(3.75f, restoredPlayer.getStats().getExhaustion(), 0.001f);
            assertEquals(9.0f, restoredPlayer.getStats().getCurrentAir(), 0.001f);
            assertEquals(37, restoredPlayer.getFireTicks());
            assertEquals(18234.5f, restoredDayCycle.getTime(), 0.001f);
            assertEquals("rain", restoredWorld.getWeatherState());
            assertEquals(321, restoredWorld.getRainTime());
            assertEquals(654, restoredWorld.getThunderTime());
            assertEquals(120, restoredPlayer.getStats().getProgression().getTotalExperience());
            assertEquals(42, restoredPlayer.getStats().getProgression().getScore());
            assertEquals(2400, restoredPlayer.getStats().getStatistics().getPlayTimeTicks());
            assertEquals(9876, restoredPlayer.getStats().getStatistics().getDistanceWalkedCm());
            assertEquals(123, restoredPlayer.getStats().getStatistics().getDistanceSwumCm());
            assertEquals(456, restoredPlayer.getStats().getStatistics().getDistanceFallenCm());
            assertEquals(789, restoredPlayer.getStats().getStatistics().getDistanceClimbedCm());
            assertEquals(1011, restoredPlayer.getStats().getStatistics().getDistanceFlownCm());
            assertEquals(1213, restoredPlayer.getStats().getStatistics().getDistanceDoveCm());
            assertEquals(1415, restoredPlayer.getStats().getStatistics().getDistanceByMinecartCm());
            assertEquals(1617, restoredPlayer.getStats().getStatistics().getDistanceByBoatCm());
            assertEquals(1819, restoredPlayer.getStats().getStatistics().getDistanceByPigCm());
            assertEquals(4, restoredPlayer.getStats().getStatistics().getGamesQuit());
            assertEquals(12, restoredPlayer.getStats().getStatistics().getJumps());
            assertEquals(34, restoredPlayer.getStats().getStatistics().getBlocksMined());
            assertEquals(5, restoredPlayer.getStats().getStatistics().getSuccessfulAttacks());
            assertEquals(135, restoredPlayer.getStats().getStatistics().getDamageDealtTenths());
            assertEquals(80, restoredPlayer.getStats().getStatistics().getDamageTakenTenths());
            assertEquals(1, restoredPlayer.getStats().getStatistics().getDeaths());
            assertEquals(6, restoredPlayer.getStats().getStatistics().getMobKills());
            assertEquals(4, restoredPlayer.getStats().getStatistics().getMonsterKills());
            assertEquals(2, restoredPlayer.getStats().getStatistics().getPlayerKills());
            assertEquals(3, restoredPlayer.getStats().getStatistics().getFishCaught());
            assertEquals(44, restoredPlayer.getStats().getStatistics().getItemsPickedUp());
            assertEquals(5, restoredPlayer.getStats().getStatistics().getItemsDropped());
            assertEquals(9, restoredPlayer.getStats().getStatistics().getItemsCrafted());
            assertEquals(7, restoredPlayer.getStats().getStatistics().getItemsUsed());
            assertEquals(2, restoredPlayer.getStats().getStatistics().getItemsDepleted());
            assertEquals(21, restoredPlayer.getStats().getStatistics().getBlocksMined(BlockType.STONE));
            assertEquals(13, restoredPlayer.getStats().getStatistics().getBlocksMined(BlockType.OAK_LOG));
            assertEquals(12, restoredPlayer.getStats().getStatistics().getItemsPickedUp(ItemType.OAK_LOG));
            assertEquals(32, restoredPlayer.getStats().getStatistics().getItemsPickedUp(ItemType.DIAMOND));
            assertEquals(3, restoredPlayer.getStats().getStatistics().getItemsDropped(ItemType.DIAMOND));
            assertEquals(2, restoredPlayer.getStats().getStatistics().getItemsDropped(ItemType.STICK));
            assertEquals(4, restoredPlayer.getStats().getStatistics().getItemsCrafted(ItemType.CRAFTING_TABLE));
            assertEquals(5, restoredPlayer.getStats().getStatistics().getItemsCrafted(ItemType.STICK));
            assertEquals(7, restoredPlayer.getStats().getStatistics().getItemsUsed(ItemType.FISHING_ROD));
            assertEquals(2, restoredPlayer.getStats().getStatistics().getItemsDepleted(ItemType.FISHING_ROD));
            assertTrue(restoredPlayer.getStats().getAchievements().isUnlocked(AchievementType.OPEN_INVENTORY));
            assertTrue(restoredPlayer.getStats().getAchievements().isUnlocked(AchievementType.MINE_WOOD));
            assertTrue(restoredPlayer.getStats().getAchievements().isUnlocked(AchievementType.BUILD_WORKBENCH));
            assertEquals(1, restoredPlayer.getStats().getActiveEffects().size());
            assertSame(StatusEffectType.POISON, restoredPlayer.getStats().getActiveEffects().get(0).type());
            assertEquals(31.5f, restoredPlayer.getSpawnX(), 0.001f);
            assertEquals(71.0f, restoredPlayer.getSpawnY(), 0.001f);
            assertEquals(30.5f, restoredPlayer.getSpawnZ(), 0.001f);
            assertTrue(restoredPlayer.hasBedSpawn());
            assertEquals(new BlockPos(30, 71, 30), restoredPlayer.getBedSpawnPos());

            assertEquals(2, restoredPlayer.getInventory().getSelectedSlot());
            assertSame(ItemType.IRON_PICKAXE, restoredPlayer.getInventory().getHotbar()[0].getType());
            assertEquals(player.getInventory().getHotbar()[0].getDurability(),
                    restoredPlayer.getInventory().getHotbar()[0].getDurability());
            assertEquals(player.getInventory().getHotbar()[0].getEnchantments(),
                    restoredPlayer.getInventory().getHotbar()[0].getEnchantments());
            assertSame(ItemType.BOW, restoredPlayer.getInventory().getHotbar()[1].getType());
            assertEquals(player.getInventory().getHotbar()[1].getDurability(),
                    restoredPlayer.getInventory().getHotbar()[1].getDurability());
            assertTrue(restoredPlayer.getInventory().getHotbar()[1].getEnchantments().isEmpty());
            assertEquals("bow", restoredPlayer.getInventory().getHotbar()[1].getMetadata().get("test"));
            assertSame(ItemType.CHARCOAL, restoredPlayer.getInventory().getMainInventory()[4].getType());
            assertEquals(7, restoredPlayer.getInventory().getMainInventory()[4].getCount());
            assertSame(ItemType.OAK_PLANKS, restoredPlayer.getInventory().getCraftingGrid()[0].getType());
            assertSame(ItemType.IRON_HELMET, restoredPlayer.getInventory().getArmor()[0].getType());
            assertSame(ItemType.STICK, restoredPlayer.getInventory().getCursorItem().getType());

            assertEquals(1, restoredWorld.getDroppedItems().size());
            assertSame(ItemType.STONE_SWORD, restoredWorld.getDroppedItems().get(0).getItemType());
            assertEquals(thrownStack.getDurability(), restoredWorld.getDroppedItems().get(0).getDurability());
            assertEquals(0.25f, restoredWorld.getDroppedItems().get(0).getVelocityX(), 0.001f);
            assertEquals(0.5f, restoredWorld.getDroppedItems().get(0).getVelocityY(), 0.001f);
            assertEquals(-0.25f, restoredWorld.getDroppedItems().get(0).getVelocityZ(), 0.001f);
            assertEquals(DroppedItem.DEFAULT_PICKUP_DELAY_TICKS,
                    restoredWorld.getDroppedItems().get(0).getPickupDelayTicks());
            assertEquals(5, restoredWorld.getDroppedItems().get(0).getHealth());
            assertFalse(restoredWorld.getDroppedItems().get(0).isOnGround());

            assertSame(BlockType.BRICK, restoredWorld.getBlock(1, 70, 1));
            assertSame(BlockType.CHEST, restoredWorld.getBlock(2, 70, 1));
            assertEquals(4, restoredWorld.getBlockMetadata(2, 70, 1));
            assertInstanceOf(ChestTileEntity.class, restoredWorld.getTileEntity(2, 70, 1));
            ChestTileEntity restoredChest = (ChestTileEntity) restoredWorld.getTileEntity(2, 70, 1);
            assertSame(ItemType.DIAMOND, restoredChest.getInventory()[0].getType());
            assertEquals(5, restoredChest.getInventory()[0].getCount());

            assertSame(BlockType.FURNACE, restoredWorld.getBlock(3, 70, 1));
            assertEquals(5, restoredWorld.getBlockMetadata(3, 70, 1));
            assertInstanceOf(FurnaceTileEntity.class, restoredWorld.getTileEntity(3, 70, 1));
            FurnaceTileEntity restoredFurnace = (FurnaceTileEntity) restoredWorld.getTileEntity(3, 70, 1);
            assertSame(ItemType.IRON_ORE, restoredFurnace.getInventory()[FurnaceTileEntity.SLOT_INPUT].getType());
            assertSame(ItemType.CHARCOAL, restoredFurnace.getInventory()[FurnaceTileEntity.SLOT_FUEL].getType());
            assertEquals(1200, restoredFurnace.getBurnTime());
            assertEquals(1600, restoredFurnace.getCurrentFuelBurnTime());
            assertEquals(80, restoredFurnace.getCookTime());
            assertEquals(0.75f, restoredFurnace.getTickAccumulator(), 0.001f);

            assertSame(BlockType.STONE, restoredWorld.getBlock(4, 69, 1));
            assertSame(BlockType.STANDING_SIGN, restoredWorld.getBlock(4, 70, 1));
            assertEquals(2, restoredWorld.getBlockMetadata(4, 70, 1));
            assertInstanceOf(SignTileEntity.class, restoredWorld.getTileEntity(4, 70, 1));
            SignTileEntity restoredSign = (SignTileEntity) restoredWorld.getTileEntity(4, 70, 1);
            assertEquals("Release", restoredSign.getLines()[0]);
            assertEquals("One", restoredSign.getLines()[1]);

            assertSame(BlockType.DISPENSER, restoredWorld.getBlock(5, 70, 1));
            assertEquals(2, restoredWorld.getBlockMetadata(5, 70, 1));
            assertInstanceOf(DispenserTileEntity.class, restoredWorld.getTileEntity(5, 70, 1));
            DispenserTileEntity restoredDispenser = (DispenserTileEntity) restoredWorld.getTileEntity(5, 70, 1);
            assertSame(ItemType.ARROW, restoredDispenser.getInventory()[0].getType());
            assertEquals(12, restoredDispenser.getInventory()[0].getCount());
            assertSame(ItemType.SNOWBALL, restoredDispenser.getInventory()[8].getType());
            assertEquals(4, restoredDispenser.getInventory()[8].getCount());

            assertSame(BlockType.ENCHANTING_TABLE, restoredWorld.getBlock(6, 70, 1));
            assertInstanceOf(EnchantingTableTileEntity.class, restoredWorld.getTileEntity(6, 70, 1));

            assertSame(BlockType.BREWING_STAND, restoredWorld.getBlock(7, 70, 1));
            assertEquals(1, restoredWorld.getBlockMetadata(7, 70, 1));
            assertInstanceOf(BrewingStandTileEntity.class, restoredWorld.getTileEntity(7, 70, 1));
            BrewingStandTileEntity restoredBrewingStand = (BrewingStandTileEntity) restoredWorld.getTileEntity(7, 70, 1);
            assertSame(ItemType.POTION,
                    restoredBrewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0].getType());
            assertSame(PotionType.WATER,
                    restoredBrewingStand.getInventory()[BrewingStandTileEntity.SLOT_BOTTLE_0].getPotionData().type());
            assertSame(ItemType.NETHER_WART,
                    restoredBrewingStand.getInventory()[BrewingStandTileEntity.SLOT_INGREDIENT].getType());
            assertEquals(240, restoredBrewingStand.getBrewTime());
            assertEquals(0.5f, restoredBrewingStand.getTickAccumulator(), 0.001f);

            assertEquals(1, restoredWorld.getEntities().size());
            Entity restoredEntity = restoredWorld.getEntities().get(0);
            assertInstanceOf(Zombie.class, restoredEntity);
            Zombie restoredZombie = (Zombie) restoredEntity;
            assertEquals(8.5f, restoredZombie.getX(), 0.001f);
            assertEquals(12.0f, restoredZombie.getHealth(), 0.001f);
            assertEquals(90.0f, restoredZombie.getYaw(), 0.001f);
            assertTrue(restoredZombie.isOnFire());
            assertFalse(restoredWorld.getChunkNow(0, 0).isModified());
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Save manager should apply saved weather to world simulation")
    void saveManagerAppliesWeatherToWorld() {
        SaveManager manager = new SaveManager(tempDir.resolve("weather-world"));
        SaveManager.LevelData data = new SaveManager.LevelData();
        data.weatherState = "thunder";
        data.weatherRainTime = 400;
        data.weatherThunderTime = 200;

        World world = new World(9101L);
        try {
            manager.applyLevel(data, new Player(0.0f, 80.0f, 0.0f), new DayCycleManager(), world);

            assertEquals("thunder", world.getWeatherState());
            assertTrue(world.isThundering());
            assertEquals(400, world.getRainTime());
            assertEquals(200, world.getThunderTime());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Enchanting table animation state should round-trip through save/load")
    void enchantingTableAnimationStateRoundTrips() throws Exception {
        Path worldDir = tempDir.resolve("enchanting-animation-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(6268L);
        World restoredWorld = null;
        try {
            world.setBlock(0, 70, 0, BlockType.ENCHANTING_TABLE);
            EnchantingTableTileEntity table = (EnchantingTableTileEntity) world.getTileEntity(0, 70, 0);
            table.setAnimationState(17,
                    1.25f,
                    1.05f,
                    2.0f,
                    0.15f,
                    0.75f,
                    0.55f,
                    1.10f,
                    0.90f,
                    0.70f,
                    0.40f);

            manager.save(world, new Player(0.0f, 70.0f, 0.0f), new DayCycleManager());
            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);

            restoredWorld = new World(loaded.seed);
            manager.applyLevel(loaded, new Player(0.0f, 70.0f, 0.0f), new DayCycleManager(), restoredWorld);

            assertInstanceOf(EnchantingTableTileEntity.class, restoredWorld.getTileEntity(0, 70, 0));
            EnchantingTableTileEntity restored = (EnchantingTableTileEntity) restoredWorld.getTileEntity(0, 70, 0);
            assertEquals(17, restored.getTickCount());
            assertEquals(1.25f, restored.getPageFlip(), 0.001f);
            assertEquals(1.05f, restored.getPrevPageFlip(), 0.001f);
            assertEquals(2.0f, restored.getPageFlipTarget(), 0.001f);
            assertEquals(0.15f, restored.getPageFlipVelocity(), 0.001f);
            assertEquals(0.75f, restored.getBookSpread(), 0.001f);
            assertEquals(0.55f, restored.getPrevBookSpread(), 0.001f);
            assertEquals(1.10f, restored.getBookRotation(), 0.001f);
            assertEquals(0.90f, restored.getBookRotation2(), 0.001f);
            assertEquals(0.70f, restored.getPrevBookRotation(), 0.001f);
            assertEquals(0.40f, restored.getTickAccumulator(), 0.001f);
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Ageable mob growing age should round-trip separately from entity lifetime")
    void ageableMobGrowingAgeRoundTrips() throws Exception {
        Path worldDir = tempDir.resolve("ageable-entity-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(5151L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Chicken chicken = new Chicken();
            chicken.setPosition(4.5f, 71.0f, 4.5f);
            chicken.setTicksExisted(42);
            chicken.setGrowingAge(-1234);
            world.setSaveManager(manager);
            world.replaceEntities(List.of(chicken));

            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0, 64, 0), new DayCycleManager(), restoredWorld);

            assertEquals(1, restoredWorld.getEntities().size());
            Entity restoredEntity = restoredWorld.getEntities().get(0);
            assertInstanceOf(Chicken.class, restoredEntity);
            Chicken restoredChicken = (Chicken) restoredEntity;
            assertEquals(42, restoredChicken.getTicksExisted());
            assertEquals(-1234, restoredChicken.getGrowingAge());
            assertTrue(restoredChicken.isBaby());
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Villager profession should round-trip with generated village mobs")
    void villagerProfessionRoundTrips() throws Exception {
        Path worldDir = tempDir.resolve("villager-entity-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(8181L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Villager smith = new Villager(Villager.PROFESSION_SMITH);
            smith.setPosition(7.5f, 71.0f, 1.5f);
            world.setSaveManager(manager);
            world.replaceEntities(List.of(smith));

            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0, 64, 0), new DayCycleManager(), restoredWorld);

            assertEquals(1, restoredWorld.getEntities().size());
            Entity restoredEntity = restoredWorld.getEntities().get(0);
            assertInstanceOf(Villager.class, restoredEntity);
            Villager restoredVillager = (Villager) restoredEntity;
            assertEquals(Villager.PROFESSION_SMITH, restoredVillager.getProfession());
            assertEquals("/textures/mob/villager/smith.png", restoredVillager.getTexturePath());
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Mob-specific runtime state should round-trip")
    void mobSpecificRuntimeStateRoundTrips() throws Exception {
        Path worldDir = tempDir.resolve("mob-special-state-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(7373L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Sheep sheep = new Sheep();
            sheep.setPosition(1.5f, 71.0f, 1.5f);
            sheep.setSheared(true);
            sheep.setWoolColor(14);

            Wolf wolf = new Wolf();
            wolf.setPosition(2.5f, 71.0f, 1.5f);
            wolf.setAngry(true);
            wolf.setTamed(true);
            wolf.setOwnerName(player.getPlayerName());
            wolf.setSitting(true);
            wolf.setWetShakeState(true, true, 0.65f, 0.60f);

            Creeper creeper = new Creeper();
            creeper.setPosition(3.5f, 71.0f, 1.5f);
            creeper.setFuseState(12, true);
            creeper.setPowered(true);

            world.setSaveManager(manager);
            world.replaceEntities(List.of(sheep, wolf, creeper));

            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0, 64, 0), new DayCycleManager(), restoredWorld);

            Sheep restoredSheep = restoredWorld.getEntities().stream()
                    .filter(Sheep.class::isInstance)
                    .map(Sheep.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertTrue(restoredSheep.isSheared());
            assertEquals(14, restoredSheep.getWoolColor());

            Wolf restoredWolf = restoredWorld.getEntities().stream()
                    .filter(Wolf.class::isInstance)
                    .map(Wolf.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertTrue(restoredWolf.isAngry());
            assertTrue(restoredWolf.isTamed());
            assertTrue(restoredWolf.isSitting());
            assertEquals(player.getPlayerName(), restoredWolf.getOwnerName());
            assertEquals("/textures/mob/wolf_tame.png", restoredWolf.getTexturePath());
            assertTrue(restoredWolf.isWet());
            assertTrue(restoredWolf.isShaking());
            assertEquals(0.65f, restoredWolf.getShakeTime(), 0.001f);
            assertEquals(0.60f, restoredWolf.getPrevShakeTime(), 0.001f);

            Creeper restoredCreeper = restoredWorld.getEntities().stream()
                    .filter(Creeper.class::isInstance)
                    .map(Creeper.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertTrue(restoredCreeper.isIgnited());
            assertEquals(12, restoredCreeper.getFuseTime());
            assertTrue(restoredCreeper.isPowered());
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Giant entities should round-trip through save/load")
    void giantEntityRoundTrips() throws Exception {
        Path worldDir = tempDir.resolve("giant-entity-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(7472L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Giant giant = new Giant();
            giant.setPosition(3.5f, 70.0f, 4.5f);
            giant.setHealth(64.0f);

            world.setSaveManager(manager);
            world.replaceEntities(List.of(giant));

            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0, 64, 0), new DayCycleManager(), restoredWorld);

            Giant restoredGiant = restoredWorld.getEntities().stream()
                    .filter(Giant.class::isInstance)
                    .map(Giant.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertSame(MobDefinition.GIANT, restoredGiant.getDefinition());
            assertEquals(64.0f, restoredGiant.getHealth(), 0.001f);
            assertEquals(3.5f, restoredGiant.getX(), 0.001f);
            assertEquals(70.0f, restoredGiant.getY(), 0.001f);
            assertEquals(4.5f, restoredGiant.getZ(), 0.001f);
            assertEquals(3.6f, restoredGiant.getWidth(), 0.001f);
            assertEquals(10.8f, restoredGiant.getHeight(), 0.001f);
            assertEquals("/textures/mob/zombie.png", restoredGiant.getTexturePath());
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Mob AI timers and animation state should round-trip")
    void mobAiRuntimeStateRoundTrips() throws Exception {
        Path worldDir = tempDir.resolve("mob-ai-state-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(7474L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            Chicken chicken = new Chicken();
            chicken.setPosition(1.5f, 71.0f, 2.5f);
            chicken.setEggTimer(1234);

            Cow panickingCow = new Cow();
            panickingCow.setPosition(1.5f, 71.0f, 4.5f);
            PanicGoal cowPanic = panickingCow.getAI().getGoal(PanicGoal.class);
            assertNotNull(cowPanic);
            cowPanic.restoreState(new PanicGoal.State(true, 73, 12.5f, -4.25f));
            panickingCow.getAI().navigateTo(12.5f, 71.0f, -4.25f);

            Slime slime = new Slime(2);
            slime.setPosition(2.5f, 71.0f, 2.5f);
            slime.setJumpDelay(9);

            MagmaCube magmaCube = new MagmaCube(1);
            magmaCube.setPosition(3.5f, 71.0f, 2.5f);
            magmaCube.setJumpDelay(4);

            Blaze blaze = new Blaze();
            blaze.setPosition(4.5f, 71.0f, 2.5f);
            blaze.setAttackState(22, 2, 5);

            Ghast ghast = new Ghast();
            ghast.setPosition(5.5f, 71.0f, 2.5f);
            ghast.setFlightState(33, 12, 44, 10.0f, 76.0f, -5.0f);

            Squid squid = new Squid();
            squid.setPosition(6.5f, 71.0f, 2.5f);
            squid.setSwimState(12, -10, 0.1f, 0.2f, -0.3f,
                    15.0f, 14.0f, 25.0f, 24.0f, 1.5f, 1.4f, 0.8f, 0.7f);

            Enderman enderman = new Enderman();
            enderman.setPosition(7.5f, 71.0f, 2.5f);
            enderman.setCarriedBlock(BlockType.GRASS, 1);
            enderman.setAngry(true);
            enderman.setAttentionState(4, 28);

            Spider spider = new Spider();
            spider.setPosition(8.5f, 71.0f, 2.5f);
            spider.setProvoked(true);

            Skeleton skeleton = new Skeleton();
            skeleton.setPosition(9.5f, 71.0f, 2.5f);
            TargetNearestGoal skeletonTargeting = skeleton.getAI().getGoal(TargetNearestGoal.class);
            assertNotNull(skeletonTargeting);

            Zombie meleeCooldownZombie = new Zombie();
            meleeCooldownZombie.setPosition(10.5f, 71.0f, 2.5f);
            meleeCooldownZombie.performAttack();
            meleeCooldownZombie.getAI().setMoveTarget(-3.5f, 9.25f);
            MeleeAttackGoal zombieMelee = meleeCooldownZombie.getAI().getGoal(MeleeAttackGoal.class);
            assertNotNull(zombieMelee);
            zombieMelee.restoreState(new MeleeAttackGoal.State(14, 31, 10.0f, 2.0f), true);
            assertEquals(20, meleeCooldownZombie.getAttackCooldown());

            SnowGolem snowGolem = new SnowGolem();
            snowGolem.setPosition(11.5f, 71.0f, 2.5f);
            snowGolem.setSnowballAttackCooldown(13);

            world.setSaveManager(manager);
            world.setPlayer(player);
            world.replaceEntities(List.of(chicken, panickingCow, slime, magmaCube, blaze, ghast, squid, enderman, spider,
                    skeleton, meleeCooldownZombie, snowGolem));
            skeleton.getAI().setMoveTarget(player.getPosition().x, player.getPosition().z);
            skeleton.getAI().tick();
            skeleton.restoreRangedAttackState(new RangedAttackGoal.State(17, 9, true, 0.65f), true);
            skeletonTargeting.restoreState(new TargetNearestGoal.State(6, 21, 4));
            assertTrue(skeleton.isRangedAttackActive());

            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0, 64, 0), new DayCycleManager(), restoredWorld);

            Chicken restoredChicken = restoredWorld.getEntities().stream()
                    .filter(Chicken.class::isInstance)
                    .map(Chicken.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(1234, restoredChicken.getEggTimer());

            Cow restoredCow = restoredWorld.getEntities().stream()
                    .filter(Cow.class::isInstance)
                    .map(Cow.class::cast)
                    .findFirst()
                    .orElseThrow();
            PanicGoal restoredPanic = restoredCow.getAI().getGoal(PanicGoal.class);
            assertNotNull(restoredPanic);
            PanicGoal.State restoredPanicState = restoredPanic.getState();
            assertTrue(restoredPanicState.panicking());
            assertEquals(73, restoredPanicState.panicTime());
            assertEquals(12.5f, restoredPanicState.fleeX(), 0.001f);
            assertEquals(-4.25f, restoredPanicState.fleeZ(), 0.001f);
            assertTrue(restoredCow.getAI().hasMoveTarget());
            assertEquals(12.5f, restoredCow.getAI().getTargetX(), 0.001f);
            assertEquals(71.0f, restoredCow.getAI().getTargetY(), 0.001f);
            assertEquals(-4.25f, restoredCow.getAI().getTargetZ(), 0.001f);
            assertNull(restoredCow.getAI().getNavigator());
            restoredCow.getAI().tick();
            assertNotNull(restoredCow.getAI().getNavigator());
            assertTrue(restoredCow.getAI().getNavigator().hasTarget());

            Slime restoredSlime = restoredWorld.getEntities().stream()
                    .filter(entity -> entity instanceof Slime && !(entity instanceof MagmaCube))
                    .map(Slime.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(2, restoredSlime.getSize());
            assertEquals(9, restoredSlime.getJumpDelay());

            MagmaCube restoredMagmaCube = restoredWorld.getEntities().stream()
                    .filter(MagmaCube.class::isInstance)
                    .map(MagmaCube.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(1, restoredMagmaCube.getSize());
            assertEquals(4, restoredMagmaCube.getJumpDelay());

            Blaze restoredBlaze = restoredWorld.getEntities().stream()
                    .filter(Blaze.class::isInstance)
                    .map(Blaze.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(22, restoredBlaze.getAttackCooldown());
            assertEquals(2, restoredBlaze.getBurstShots());
            assertEquals(5, restoredBlaze.getBurstCooldown());

            Ghast restoredGhast = restoredWorld.getEntities().stream()
                    .filter(Ghast.class::isInstance)
                    .map(Ghast.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(33, restoredGhast.getFireCooldown());
            assertEquals(12, restoredGhast.getAttackCharge());
            assertEquals(44, restoredGhast.getWanderCooldown());
            assertEquals(10.0f, restoredGhast.getTargetX(), 0.001f);
            assertEquals(76.0f, restoredGhast.getTargetY(), 0.001f);
            assertEquals(-5.0f, restoredGhast.getTargetZ(), 0.001f);

            Squid restoredSquid = restoredWorld.getEntities().stream()
                    .filter(Squid.class::isInstance)
                    .map(Squid.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(12, restoredSquid.getSwimTimer());
            assertEquals(-10, restoredSquid.getAirTicks());
            assertEquals(0.1f, restoredSquid.getSwimX(), 0.001f);
            assertEquals(0.2f, restoredSquid.getSwimY(), 0.001f);
            assertEquals(-0.3f, restoredSquid.getSwimZ(), 0.001f);
            assertEquals(15.0f, restoredSquid.getSquidPitch(), 0.001f);
            assertEquals(24.0f, restoredSquid.getPrevSquidYaw(), 0.001f);
            assertEquals(1.5f, restoredSquid.getSquidRotation(), 0.001f);
            assertEquals(0.7f, restoredSquid.getPrevTentacleAngle(), 0.001f);

            Enderman restoredEnderman = restoredWorld.getEntities().stream()
                    .filter(Enderman.class::isInstance)
                    .map(Enderman.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertSame(BlockType.GRASS, restoredEnderman.getCarriedBlock());
            assertEquals(1, restoredEnderman.getCarriedMetadata());
            assertTrue(restoredEnderman.isAngry());
            assertEquals(4, restoredEnderman.getStareTicks());
            assertEquals(28, restoredEnderman.getTeleportCooldown());

            Spider restoredSpider = restoredWorld.getEntities().stream()
                    .filter(Spider.class::isInstance)
                    .map(Spider.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertTrue(restoredSpider.isProvoked());

            Skeleton restoredSkeleton = restoredWorld.getEntities().stream()
                    .filter(Skeleton.class::isInstance)
                    .map(Skeleton.class::cast)
                    .findFirst()
                    .orElseThrow();
            RangedAttackGoal.State restoredRangedState = restoredSkeleton.getRangedAttackState();
            assertTrue(restoredSkeleton.isRangedAttackActive());
            assertEquals(17, restoredRangedState.attackCooldown());
            assertEquals(9, restoredRangedState.strafeTime());
            assertTrue(restoredRangedState.strafingClockwise());
            assertEquals(0.65f, restoredRangedState.strafeSpeed(), 0.001f);
            TargetNearestGoal restoredTargeting = restoredSkeleton.getAI().getGoal(TargetNearestGoal.class);
            assertNotNull(restoredTargeting);
            TargetNearestGoal.State restoredTargetingState = restoredTargeting.getState();
            assertEquals(6, restoredTargetingState.checkCooldown());
            assertEquals(21, restoredTargetingState.sightLostTicks());
            assertEquals(4, restoredTargetingState.targetRefreshCooldown());

            Zombie restoredMeleeCooldownZombie = restoredWorld.getEntities().stream()
                    .filter(Zombie.class::isInstance)
                    .map(Zombie.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(20, restoredMeleeCooldownZombie.getAttackCooldown());
            assertFalse(restoredMeleeCooldownZombie.canAttack());
            assertTrue(restoredMeleeCooldownZombie.getAI().hasMoveTarget());
            assertEquals(-3.5f, restoredMeleeCooldownZombie.getAI().getTargetX(), 0.001f);
            assertEquals(71.0f, restoredMeleeCooldownZombie.getAI().getTargetY(), 0.001f);
            assertEquals(9.25f, restoredMeleeCooldownZombie.getAI().getTargetZ(), 0.001f);
            MeleeAttackGoal restoredMelee = restoredMeleeCooldownZombie.getAI().getGoal(MeleeAttackGoal.class);
            assertNotNull(restoredMelee);
            MeleeAttackGoal.State restoredMeleeState = restoredMelee.getState();
            assertEquals(14, restoredMeleeState.pathRecalcCooldown());
            assertEquals(31, restoredMeleeState.stuckTicks());
            assertEquals(10.0f, restoredMeleeState.lastX(), 0.001f);
            assertEquals(2.0f, restoredMeleeState.lastZ(), 0.001f);

            SnowGolem restoredSnowGolem = restoredWorld.getEntities().stream()
                    .filter(SnowGolem.class::isInstance)
                    .map(SnowGolem.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(13, restoredSnowGolem.getSnowballAttackCooldown());
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Mob combat target identity should round-trip")
    void mobCombatTargetIdentityRoundTrips() throws Exception {
        Path worldDir = tempDir.resolve("mob-target-state-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(7475L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.setPlayerName("Steve");
            DayCycleManager dayCycle = new DayCycleManager();

            Wolf wolf = new Wolf();
            wolf.setPosition(0.0f, 70.0f, 0.0f);
            wolf.setTamed(true);
            wolf.setOwnerName(player.getPlayerName());

            Zombie zombie = new Zombie();
            zombie.setPosition(1.2f, 70.0f, 0.0f);
            float zombieHealth = zombie.getHealth();

            world.setSaveManager(manager);
            world.replaceEntities(List.of(wolf, zombie));
            assertTrue(wolf.setAssistTarget(zombie));

            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            Player restoredPlayer = new Player(0, 64, 0);
            restoredPlayer.setPlayerName(player.getPlayerName());
            manager.applyLevel(loaded, restoredPlayer, new DayCycleManager(), restoredWorld);

            Wolf restoredWolf = restoredWorld.getEntities().stream()
                    .filter(Wolf.class::isInstance)
                    .map(Wolf.class::cast)
                    .findFirst()
                    .orElseThrow();
            Zombie restoredZombie = restoredWorld.getEntities().stream()
                    .filter(Zombie.class::isInstance)
                    .map(Zombie.class::cast)
                    .findFirst()
                    .orElseThrow();

            assertSame(restoredZombie, restoredWolf.getAssistTarget());

            for (int tick = 0; tick < 25 && restoredZombie.getHealth() == zombieHealth; tick++) {
                restoredWorld.updateEntities(1.0f / 20.0f);
            }

            assertEquals(zombieHealth - Wolf.TAMED_ATTACK_DAMAGE, restoredZombie.getHealth(), 0.001f);
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Minecart living passenger identity should round-trip")
    void minecartLivingPassengerRoundTrips() throws Exception {
        Path worldDir = tempDir.resolve("minecart-passenger-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(7476L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            DayCycleManager dayCycle = new DayCycleManager();

            MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            cart.setMotion(0.12f, 0.0f, 0.0f);
            Zombie zombie = new Zombie();
            zombie.setPosition(0.5f, 70.2f, 0.5f);
            world.setSaveManager(manager);
            world.replaceEntities(List.of(cart, zombie));
            assertTrue(cart.mountLivingEntity(zombie));

            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0, 64, 0), new DayCycleManager(), restoredWorld);

            MinecartEntity restoredCart = restoredWorld.getEntities().stream()
                    .filter(MinecartEntity.class::isInstance)
                    .map(MinecartEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            Zombie restoredZombie = restoredWorld.getEntities().stream()
                    .filter(Zombie.class::isInstance)
                    .map(Zombie.class::cast)
                    .findFirst()
                    .orElseThrow();

            assertSame(restoredZombie, restoredCart.getLivingPassenger());
            assertEquals(restoredCart.getX(), restoredZombie.getX(), 0.001f);
            assertEquals(restoredCart.getY() + 0.1f, restoredZombie.getY(), 0.001f);
            assertEquals(restoredCart.getZ(), restoredZombie.getZ(), 0.001f);

            restoredWorld.updateEntities(1.0f / 20.0f);

            assertSame(restoredZombie, restoredCart.getLivingPassenger());
            assertEquals(restoredCart.getX(), restoredZombie.getX(), 0.001f);
            assertEquals(restoredCart.getY() + 0.1f, restoredZombie.getY(), 0.001f);
            assertEquals(restoredCart.getZ(), restoredZombie.getZ(), 0.001f);
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Spider jockey rider identity should round-trip")
    void spiderJockeyRiderRoundTrips() throws Exception {
        Path worldDir = tempDir.resolve("spider-jockey-rider-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(7477L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            DayCycleManager dayCycle = new DayCycleManager();

            Spider spider = new Spider();
            spider.setPosition(0.5f, 70.0f, 0.5f);
            spider.setYaw(45.0f);
            Skeleton skeleton = new Skeleton();
            skeleton.setPosition(0.5f, 71.0f, 0.5f);
            world.setSaveManager(manager);
            world.replaceEntities(List.of(spider, skeleton));
            assertTrue(spider.mountJockey(skeleton));

            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            assertEquals(2, loaded.entities.size());
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0, 64, 0), new DayCycleManager(), restoredWorld);

            Spider restoredSpider = restoredWorld.getEntities().stream()
                    .filter(Spider.class::isInstance)
                    .map(Spider.class::cast)
                    .findFirst()
                    .orElseThrow();
            Skeleton restoredSkeleton = restoredWorld.getEntities().stream()
                    .filter(Skeleton.class::isInstance)
                    .map(Skeleton.class::cast)
                    .findFirst()
                    .orElseThrow();

            assertSame(restoredSkeleton, restoredSpider.getJockeyRider());
            assertSame(restoredSpider, restoredSkeleton.getRidingSpider());
            assertEquals(restoredSpider.getX(), restoredSkeleton.getX(), 0.001f);
            assertEquals(restoredSpider.getY() + restoredSpider.getHeight() * 0.75f,
                    restoredSkeleton.getY(), 0.001f);
            assertEquals(restoredSpider.getZ(), restoredSkeleton.getZ(), 0.001f);
            assertEquals(restoredSpider.getYaw(), restoredSkeleton.getYaw(), 0.001f);

            restoredSpider.setPosition(3.5f, 70.0f, -1.5f);
            restoredWorld.updateEntities(1.0f / 20.0f);

            assertSame(restoredSkeleton, restoredSpider.getJockeyRider());
            assertSame(restoredSpider, restoredSkeleton.getRidingSpider());
            assertEquals(restoredSpider.getX(), restoredSkeleton.getX(), 0.001f);
            assertEquals(restoredSpider.getY() + restoredSpider.getHeight() * 0.75f,
                    restoredSkeleton.getY(), 0.001f);
            assertEquals(restoredSpider.getZ(), restoredSkeleton.getZ(), 0.001f);
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Ender Dragon flight and death state should round-trip")
    void enderDragonRuntimeStateRoundTrips() throws Exception {
        Path worldDir = tempDir.resolve("dragon-runtime-state-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(7575L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            DayCycleManager dayCycle = new DayCycleManager();

            EnderDragon liveDragon = new EnderDragon();
            liveDragon.setPosition(-10.0f, 84.0f, 2.0f);
            liveDragon.setMotion(0.1f, 0.2f, -0.3f);
            liveDragon.setHealth(123.0f);
            liveDragon.setFlightState(33.0f, 88.0f, -44.0f, 77);

            EnderDragon dyingDragon = new EnderDragon();
            dyingDragon.setPosition(10.0f, 84.0f, 2.0f);
            dyingDragon.setFlightState(-20.0f, 90.0f, 20.0f, 55);
            dyingDragon.setDeathState(17, true);

            world.setSaveManager(manager);
            world.replaceEntities(List.of(liveDragon, dyingDragon));

            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0, 64, 0), new DayCycleManager(), restoredWorld);

            assertEquals(2, restoredWorld.getEntities().stream()
                    .filter(EnderDragon.class::isInstance)
                    .count());
            EnderDragon restoredLiveDragon = restoredWorld.getEntities().stream()
                    .filter(EnderDragon.class::isInstance)
                    .map(EnderDragon.class::cast)
                    .filter(dragon -> !dragon.isDead())
                    .findFirst()
                    .orElseThrow();
            assertEquals(123.0f, restoredLiveDragon.getHealth(), 0.001f);
            assertEquals(33.0f, restoredLiveDragon.getTargetX(), 0.001f);
            assertEquals(88.0f, restoredLiveDragon.getTargetY(), 0.001f);
            assertEquals(-44.0f, restoredLiveDragon.getTargetZ(), 0.001f);
            assertEquals(77, restoredLiveDragon.getTargetCooldown());
            assertEquals(0.1f, restoredLiveDragon.getMotionX(), 0.001f);

            EnderDragon restoredDyingDragon = restoredWorld.getEntities().stream()
                    .filter(EnderDragon.class::isInstance)
                    .map(EnderDragon.class::cast)
                    .filter(EnderDragon::isDead)
                    .findFirst()
                    .orElseThrow();
            assertEquals(0.0f, restoredDyingDragon.getHealth(), 0.001f);
            assertEquals(17, restoredDyingDragon.getDeathTicks());
            assertEquals(-20.0f, restoredDyingDragon.getTargetX(), 0.001f);
            assertEquals(55, restoredDyingDragon.getTargetCooldown());

            restoredWorld.updateEntities(1.0f / 20.0f);

            assertEquals(18, restoredDyingDragon.getDeathTicks());
            assertNotSame(BlockType.DRAGON_EGG, restoredWorld.getBlock(0, 66, 0));
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("End crystals should round-trip as fixed non-living End entities")
    void endCrystalRuntimeStateRoundTrips() throws Exception {
        Path worldDir = tempDir.resolve("end-crystal-runtime-state-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(7580L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            DayCycleManager dayCycle = new DayCycleManager();

            EndCrystalEntity crystal = new EndCrystalEntity(3.5f, 82.0f, -4.5f);
            crystal.setYaw(123.0f);
            crystal.setPitch(17.0f);
            crystal.setTicksExisted(44);
            crystal.addEffect(new StatusEffectInstance(StatusEffectType.REGENERATION, 100, 0));
            crystal.setHealth(1.0f);
            world.setSaveManager(manager);
            world.replaceEntities(List.of(crystal));

            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            restoredWorld = new World(loaded.seed, loaded.generatorId, Dimension.fromSaveName(loaded.dimension));
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0, 64, 0), new DayCycleManager(), restoredWorld);

            EndCrystalEntity restoredCrystal = restoredWorld.getEntities().stream()
                    .filter(EndCrystalEntity.class::isInstance)
                    .map(EndCrystalEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(3.5f, restoredCrystal.getX(), 0.001f);
            assertEquals(82.0f, restoredCrystal.getY(), 0.001f);
            assertEquals(-4.5f, restoredCrystal.getZ(), 0.001f);
            assertEquals(123.0f, restoredCrystal.getYaw(), 0.001f);
            assertEquals(17.0f, restoredCrystal.getPitch(), 0.001f);
            assertEquals(44, restoredCrystal.getTicksExisted());
            assertEquals(restoredCrystal.getMaxHealth(), restoredCrystal.getHealth(), 0.001f);
            assertTrue(restoredCrystal.getActiveEffects().isEmpty());

            restoredWorld.setBlock(3, 82, -5, BlockType.AIR, 0);
            restoredWorld.updateEntities(1.0f / 20.0f);

            assertSame(BlockType.FIRE, restoredWorld.getBlock(3, 82, -5));
            assertFalse(restoredCrystal.isOnFire());
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Note block and jukebox playback state should round-trip")
    void soundTileEntitiesRoundTripPlaybackState() throws Exception {
        Path worldDir = tempDir.resolve("sound-tile-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(4242L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                    new ItemStack(ItemType.FISHING_ROD, 1);
            DayCycleManager dayCycle = new DayCycleManager();
            world.setSaveManager(manager);
            world.setBlock(4, 69, 4, BlockType.GLASS);
            world.setBlock(4, 70, 4, BlockType.NOTE_BLOCK);
            world.setBlock(4, 71, 4, BlockType.AIR);
            NoteBlockTileEntity note = (NoteBlockTileEntity) world.getTileEntity(4, 70, 4);
            note.setPitch(12);
            assertTrue(note.play(world));
            note.setPlayTicks(7);

            world.setBlock(5, 70, 4, BlockType.JUKEBOX);
            JukeboxTileEntity jukebox = (JukeboxTileEntity) world.getTileEntity(5, 70, 4);
            assertTrue(jukebox.insertRecord(new ItemStack(ItemType.RECORD_CAT, 1)));
            jukebox.setPlayTicks(9);
            assertEquals(0, world.getBlockMetadata(5, 70, 4),
                    "Older CraftZero saves can contain record tile data with empty jukebox metadata");

            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0, 64, 0), new DayCycleManager(), restoredWorld);

            assertInstanceOf(NoteBlockTileEntity.class, restoredWorld.getTileEntity(4, 70, 4));
            NoteBlockTileEntity restoredNote = (NoteBlockTileEntity) restoredWorld.getTileEntity(4, 70, 4);
            assertEquals(12, restoredNote.getPitch());
            assertEquals(7, restoredNote.getPlayTicks());
            assertEquals(NoteBlockTileEntity.INSTRUMENT_STICKS, restoredNote.getLastInstrument());

            assertInstanceOf(JukeboxTileEntity.class, restoredWorld.getTileEntity(5, 70, 4));
            JukeboxTileEntity restoredJukebox = (JukeboxTileEntity) restoredWorld.getTileEntity(5, 70, 4);
            assertTrue(restoredJukebox.hasRecord());
            assertSame(ItemType.RECORD_CAT, restoredJukebox.getRecord().getType());
            assertEquals(9, restoredJukebox.getPlayTicks());
            assertEquals(1, restoredWorld.getBlockMetadata(5, 70, 4));
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Scheduled block ticks should round-trip with remaining delay")
    void scheduledBlockTicksRoundTripRemainingDelay() throws Exception {
        Path worldDir = tempDir.resolve("scheduled-tick-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(6262L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.setSaveManager(manager);
            world.setBlock(8, 70, 8, BlockType.GLOWING_REDSTONE_ORE, 0);
            assertTrue(world.hasScheduledBlockTick(8, 70, 8, BlockType.GLOWING_REDSTONE_ORE));

            world.advanceBlockTicks(10);
            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            assertNotNull(loaded.scheduledBlockTicks);
            assertEquals(1, loaded.scheduledBlockTicks.stream()
                    .filter(tick -> tick.blockId == BlockType.GLOWING_REDSTONE_ORE.getId())
                    .count());

            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0, 64, 0), new DayCycleManager(), restoredWorld);

            assertTrue(restoredWorld.hasScheduledBlockTick(8, 70, 8, BlockType.GLOWING_REDSTONE_ORE));
            assertSame(BlockType.GLOWING_REDSTONE_ORE, restoredWorld.getBlock(8, 70, 8));

            restoredWorld.advanceBlockTicks(19);
            assertSame(BlockType.GLOWING_REDSTONE_ORE, restoredWorld.getBlock(8, 70, 8));
            assertTrue(restoredWorld.hasScheduledBlockTick(8, 70, 8, BlockType.GLOWING_REDSTONE_ORE));

            restoredWorld.advanceBlockTicks(1);
            assertSame(BlockType.REDSTONE_ORE, restoredWorld.getBlock(8, 70, 8));
            assertFalse(restoredWorld.hasScheduledBlockTick(8, 70, 8, BlockType.GLOWING_REDSTONE_ORE));
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Moving piston state should round-trip and settle carried blocks")
    void movingPistonStateRoundTripsAndSettles() throws Exception {
        Path worldDir = tempDir.resolve("moving-piston-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(6363L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.setSaveManager(manager);
            world.setBlock(0, 100, 0, BlockType.PISTON, Block.FACE_EAST);
            world.setBlock(1, 100, 0, BlockType.STONE, 0);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(1);

            assertSame(BlockType.MOVING_PISTON, world.getBlock(1, 100, 0));
            assertSame(BlockType.MOVING_PISTON, world.getBlock(2, 100, 0));
            assertSame(BlockType.PISTON_HEAD, world.getMovingPistonState(1, 100, 0).carriedType());
            assertSame(BlockType.STONE, world.getMovingPistonState(2, 100, 0).carriedType());

            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            assertNotNull(loaded.movingPistons);
            assertEquals(2, loaded.movingPistons.size());

            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0, 64, 0), new DayCycleManager(), restoredWorld);

            assertEquals(RedstoneEngine.PISTON_MOVEMENT_TICKS,
                    scheduledDelay(restoredWorld, 1, 100, 0, BlockType.MOVING_PISTON));
            assertEquals(RedstoneEngine.PISTON_MOVEMENT_TICKS,
                    scheduledDelay(restoredWorld, 2, 100, 0, BlockType.MOVING_PISTON));
            assertTrue(restoredWorld.hasScheduledBlockTick(1, 100, 0, BlockType.MOVING_PISTON));
            assertTrue(restoredWorld.hasScheduledBlockTick(2, 100, 0, BlockType.MOVING_PISTON));
            assertSame(BlockType.MOVING_PISTON, restoredWorld.getBlock(1, 100, 0));
            assertSame(BlockType.MOVING_PISTON, restoredWorld.getBlock(2, 100, 0));
            assertEquals(RedstoneEngine.PISTON_EXTENDED_BIT,
                    restoredWorld.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT);
            assertEquals(RedstoneEngine.PISTON_MOVEMENT_TICKS,
                    scheduledDelay(restoredWorld, 1, 100, 0, BlockType.MOVING_PISTON));
            assertEquals(RedstoneEngine.PISTON_MOVEMENT_TICKS,
                    scheduledDelay(restoredWorld, 2, 100, 0, BlockType.MOVING_PISTON));
            assertSame(BlockType.PISTON_HEAD, restoredWorld.getMovingPistonState(1, 100, 0).carriedType());
            assertSame(BlockType.STONE, restoredWorld.getMovingPistonState(2, 100, 0).carriedType());

            restoredWorld.advanceBlockTicks(RedstoneEngine.PISTON_MOVEMENT_TICKS);

            assertNull(restoredWorld.getMovingPistonState(1, 100, 0));
            assertNull(restoredWorld.getMovingPistonState(2, 100, 0));
            assertSame(BlockType.PISTON_HEAD, restoredWorld.getBlock(1, 100, 0));
            assertSame(BlockType.STONE, restoredWorld.getBlock(2, 100, 0));
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Furnace minecart fuel and raw push vector should persist")
    void furnaceMinecartPushStateRoundTrips() throws Exception {
        Path worldDir = tempDir.resolve("furnace-minecart-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(555556L);
        World restoredWorld = null;
        try {
            FurnaceMinecartEntity cart = new FurnaceMinecartEntity(2.0f, 70.1f, 0.0f);
            cart.setFuelTicks(2400);
            cart.setPush(2.0f, -0.5f);
            world.replaceEntities(List.of(cart));

            manager.save(world, new Player(0.0f, 70.0f, 0.0f), new DayCycleManager());

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            assertEquals(1, loaded.entities.size());
            assertEquals(2.0f, loaded.entities.get(0).pushX, 0.0001f);
            assertEquals(-0.5f, loaded.entities.get(0).pushZ, 0.0001f);

            restoredWorld = new World(loaded.seed);
            manager.applyLevel(loaded, new Player(0.0f, 70.0f, 0.0f), new DayCycleManager(), restoredWorld);

            FurnaceMinecartEntity restored = restoredWorld.getEntities().stream()
                    .filter(FurnaceMinecartEntity.class::isInstance)
                    .map(FurnaceMinecartEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(2400, restored.getFuelTicks());
            assertEquals(2.0f, restored.getPushX(), 0.0001f);
            assertEquals(-0.5f, restored.getPushZ(), 0.0001f);
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Projectile entities should round-trip active runtime state")
    void projectileEntitiesRoundTripRuntimeState() throws Exception {
        Path worldDir = tempDir.resolve("projectile-entity-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(7171L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.setSaveManager(manager);
            world.setPlayer(player);

            ArrowEntity arrow = new ArrowEntity(1.5f, 81.0f, 1.5f,
                    0.25f, 0.05f, 0.0f, null, true, 4.0f);
            arrow.setTicksExisted(40);
            arrow.setKnockback(1.5f, 0.7f);
            arrow.setFireTicksOnHit(60);
            arrow.setCritical(true);
            arrow.setStuckInBlock(1, 80, 1, 37);

            FireballEntity fireball = new FireballEntity(2.5f, 81.0f, 1.5f,
                    0.0f, 0.0f, 0.45f, null, true);
            fireball.setTicksExisted(12);
            fireball.setDeflectedByPlayer(true);

            EnderPearlEntity pearl = new EnderPearlEntity(3.5f, 81.0f, 1.5f,
                    0.15f, 0.05f, 0.0f, player);
            pearl.setTicksExisted(8);

            ThrownItemEntity egg = new ThrownItemEntity(4.5f, 81.0f, 1.5f,
                    -0.15f, 0.05f, 0.0f, ItemType.EGG, null);
            egg.setTicksExisted(16);

            PotionData potionData = new PotionData(PotionType.POISON, true, false, false);
            SplashPotionEntity splashPotion = new SplashPotionEntity(5.5f, 81.0f, 1.5f,
                    0.0f, 0.1f, -0.15f, null, potionData);
            splashPotion.setTicksExisted(20);

            world.replaceEntities(List.of(arrow, fireball, pearl, egg, splashPotion));

            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            assertEquals(5, loaded.entities.size());

            Player restoredPlayer = new Player(0, 64, 0);
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, restoredPlayer, new DayCycleManager(), restoredWorld);

            assertEquals(5, restoredWorld.getEntities().size());
            ArrowEntity restoredArrow = restoredWorld.getEntities().stream()
                    .filter(ArrowEntity.class::isInstance)
                    .map(ArrowEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertTrue(restoredArrow.isInGround());
            assertTrue(restoredArrow.isPlayerOwned());
            assertEquals(4.0f, restoredArrow.getDamage(), 0.001f);
            assertEquals(1.5f, restoredArrow.getKnockbackHorizontal(), 0.001f);
            assertEquals(0.7f, restoredArrow.getKnockbackVertical(), 0.001f);
            assertEquals(60, restoredArrow.getFireTicksOnHit());
            assertTrue(restoredArrow.isCritical());
            assertEquals(37, restoredArrow.getStuckTicks());
            assertEquals(1, restoredArrow.getBlockX());
            assertEquals(80, restoredArrow.getBlockY());
            assertEquals(1, restoredArrow.getBlockZ());
            assertEquals(40, restoredArrow.getTicksExisted());

            FireballEntity restoredFireball = restoredWorld.getEntities().stream()
                    .filter(FireballEntity.class::isInstance)
                    .map(FireballEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertTrue(restoredFireball.isExplosive());
            assertTrue(restoredFireball.isDeflectedByPlayer());
            assertEquals(0.45f, restoredFireball.getMotionZ(), 0.001f);
            assertEquals(12, restoredFireball.getTicksExisted());

            EnderPearlEntity restoredPearl = restoredWorld.getEntities().stream()
                    .filter(EnderPearlEntity.class::isInstance)
                    .map(EnderPearlEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertSame(restoredPlayer, restoredPearl.getOwner());
            assertSame(restoredPlayer, restoredWorld.getPlayer());
            assertEquals(8, restoredPearl.getTicksExisted());

            ThrownItemEntity restoredEgg = restoredWorld.getEntities().stream()
                    .filter(ThrownItemEntity.class::isInstance)
                    .map(ThrownItemEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertSame(ItemType.EGG, restoredEgg.getItemType());
            assertEquals(16, restoredEgg.getTicksExisted());

            SplashPotionEntity restoredSplashPotion = restoredWorld.getEntities().stream()
                    .filter(SplashPotionEntity.class::isInstance)
                    .map(SplashPotionEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(PotionType.POISON, restoredSplashPotion.getPotionData().type());
            assertTrue(restoredSplashPotion.getPotionData().splash());
            assertEquals(20, restoredSplashPotion.getTicksExisted());
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Transient physics entities should round-trip runtime state")
    void transientPhysicsEntitiesRoundTripRuntimeState() throws Exception {
        Path worldDir = tempDir.resolve("transient-entity-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(7272L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                    new ItemStack(ItemType.FISHING_ROD, 1);
            DayCycleManager dayCycle = new DayCycleManager();
            world.setSaveManager(manager);

            FallingBlockEntity falling = new FallingBlockEntity(BlockType.SAND, 3);
            falling.setPosition(7.5f, 84.0f, 1.5f);
            falling.setMotion(0.05f, -0.2f, 0.01f);
            falling.setTicksExisted(25);

            EyeOfEnderEntity eye = new EyeOfEnderEntity(8.5f, 82.0f, 1.5f,
                    24.0f, 85.0f, -10.0f, true);
            eye.setMotion(0.02f, 0.04f, -0.03f);
            eye.setTicksExisted(12);

            FishingHookEntity hook = new FishingHookEntity(9.5f, 82.0f, 1.5f,
                    0.03f, -0.02f, 0.01f, player);
            hook.setTicksExisted(9);
            hook.setCatchableTicks(8);

            PrimedTntEntity tnt = new PrimedTntEntity(10.5f, 82.0f, 1.5f, 0);
            tnt.setMotion(0.02f, 0.16f, -0.01f);
            tnt.setTicksExisted(79);

            ExperienceOrbEntity orb = new ExperienceOrbEntity(11.5f, 82.0f, 1.5f, 17);
            orb.setMotion(-0.04f, 0.07f, 0.03f);
            orb.setTicksExisted(33);
            orb.setPickupDelayTicks(6);
            orb.setHealth(4);

            world.replaceEntities(List.of(falling, eye, hook, tnt, orb));

            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            assertEquals(5, loaded.entities.size());

            Player restoredPlayer = new Player(0, 64, 0);
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, restoredPlayer, new DayCycleManager(), restoredWorld);

            FallingBlockEntity restoredFalling = restoredWorld.getEntities().stream()
                    .filter(FallingBlockEntity.class::isInstance)
                    .map(FallingBlockEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertSame(BlockType.SAND, restoredFalling.getBlockType());
            assertEquals(3, restoredFalling.getMetadata());
            assertEquals(7.5f, restoredFalling.getX(), 0.001f);
            assertEquals(-0.2f, restoredFalling.getMotionY(), 0.001f);
            assertEquals(25, restoredFalling.getTicksExisted());

            EyeOfEnderEntity restoredEye = restoredWorld.getEntities().stream()
                    .filter(EyeOfEnderEntity.class::isInstance)
                    .map(EyeOfEnderEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(24.0f, restoredEye.getTargetX(), 0.001f);
            assertEquals(85.0f, restoredEye.getTargetY(), 0.001f);
            assertEquals(-10.0f, restoredEye.getTargetZ(), 0.001f);
            assertTrue(restoredEye.dropsItem());
            assertEquals(0.02f, restoredEye.getMotionX(), 0.001f);
            assertEquals(12, restoredEye.getTicksExisted());

            FishingHookEntity restoredHook = restoredWorld.getEntities().stream()
                    .filter(FishingHookEntity.class::isInstance)
                    .map(FishingHookEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertSame(restoredPlayer, restoredHook.getOwner());
            assertSame(restoredHook, restoredPlayer.getFishingHook());
            assertEquals(0, restoredHook.getWaitTicks());
            assertEquals(8, restoredHook.getCatchableTicks());
            assertEquals(0.03f, restoredHook.getMotionX(), 0.001f);
            assertEquals(9, restoredHook.getTicksExisted());

            PrimedTntEntity restoredTnt = restoredWorld.getEntities().stream()
                    .filter(PrimedTntEntity.class::isInstance)
                    .map(PrimedTntEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(0, restoredTnt.getFuseTicks());
            assertEquals(0.02f, restoredTnt.getMotionX(), 0.001f);
            assertEquals(0.16f, restoredTnt.getMotionY(), 0.001f);
            assertEquals(79, restoredTnt.getTicksExisted());

            ExperienceOrbEntity restoredOrb = restoredWorld.getEntities().stream()
                    .filter(ExperienceOrbEntity.class::isInstance)
                    .map(ExperienceOrbEntity.class::cast)
                    .findFirst()
                    .orElseThrow();
            assertEquals(17, restoredOrb.getValue());
            assertEquals(4, restoredOrb.getHealth());
            assertEquals(6, restoredOrb.getPickupDelayTicks());
            assertEquals(-0.04f, restoredOrb.getMotionX(), 0.001f);
            assertEquals(0.07f, restoredOrb.getMotionY(), 0.001f);
            assertEquals(0.03f, restoredOrb.getMotionZ(), 0.001f);
            assertEquals(33, restoredOrb.getTicksExisted());
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Fishing hook hooked-target identity should round-trip")
    void fishingHookedTargetRoundTrips() throws Exception {
        Path worldDir = tempDir.resolve("hooked-fishing-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(7273L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                    new ItemStack(ItemType.FISHING_ROD, 1);
            DayCycleManager dayCycle = new DayCycleManager();
            world.setSaveManager(manager);

            Zombie zombie = new Zombie();
            zombie.setPosition(4.0f, 70.0f, 0.0f);
            FishingHookEntity hook = new FishingHookEntity(4.0f, 71.4f, 0.0f,
                    0.0f, 0.0f, 0.0f, player);
            hook.restoreHookedEntity(zombie);
            hook.setTicksExisted(17);
            world.replaceEntities(List.of(zombie, hook));

            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            assertEquals(2, loaded.entities.size());

            Player restoredPlayer = new Player(0, 64, 0);
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, restoredPlayer, new DayCycleManager(), restoredWorld);

            Zombie restoredZombie = restoredWorld.getEntities().stream()
                    .filter(Zombie.class::isInstance)
                    .map(Zombie.class::cast)
                    .findFirst()
                    .orElseThrow();
            FishingHookEntity restoredHook = restoredWorld.getEntities().stream()
                    .filter(FishingHookEntity.class::isInstance)
                    .map(FishingHookEntity.class::cast)
                    .findFirst()
                    .orElseThrow();

            assertSame(restoredHook, restoredPlayer.getFishingHook());
            assertSame(restoredZombie, restoredHook.getHookedEntity());
            assertEquals(17, restoredHook.getTicksExisted());
            assertEquals(3, restoredHook.reelIn());
            assertTrue(restoredZombie.getMotionX() < 0.0f);
            assertTrue(restoredZombie.getMotionY() > 0.0f);
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    @Test
    @DisplayName("Mob spawner tile entities should round-trip mob type and timing state")
    void mobSpawnerTileEntityRoundTripsState() throws Exception {
        Path worldDir = tempDir.resolve("spawner-world");
        SaveManager manager = new SaveManager(worldDir);
        World world = new World(4141L);
        World restoredWorld = null;
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            DayCycleManager dayCycle = new DayCycleManager();
            world.setSaveManager(manager);
            world.setBlock(6, 40, 6, BlockType.MOB_SPAWNER);
            MonsterSpawnerTileEntity spawner = (MonsterSpawnerTileEntity) world.getTileEntity(6, 40, 6);
            spawner.setMobDefinition(MobDefinition.CAVE_SPIDER);
            spawner.setDelay(123);
            spawner.setDelayRange(40, 120);
            spawner.setSpawnCount(2);
            spawner.setMaxNearbyEntities(3);
            spawner.setTickAccumulator(0.65f);

            manager.save(world, player, dayCycle);

            SaveManager.LevelData loaded = manager.loadLevelIfExists();
            assertNotNull(loaded);
            restoredWorld = new World(loaded.seed);
            restoredWorld.setSaveManager(manager);
            manager.applyLevel(loaded, new Player(0, 64, 0), new DayCycleManager(), restoredWorld);

            assertSame(BlockType.MOB_SPAWNER, restoredWorld.getBlock(6, 40, 6));
            assertInstanceOf(MonsterSpawnerTileEntity.class, restoredWorld.getTileEntity(6, 40, 6));
            MonsterSpawnerTileEntity restored = (MonsterSpawnerTileEntity) restoredWorld.getTileEntity(6, 40, 6);
            assertSame(MobDefinition.CAVE_SPIDER, restored.getMobDefinition());
            assertEquals(123, restored.getDelay());
            assertEquals(40, restored.getMinDelay());
            assertEquals(120, restored.getMaxDelay());
            assertEquals(2, restored.getSpawnCount());
            assertEquals(3, restored.getMaxNearbyEntities());
            assertEquals(0.65f, restored.getTickAccumulator(), 0.001f);
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    private static int scheduledDelay(World world, int x, int y, int z, BlockType type) {
        return world.getScheduledBlockTickStates().stream()
                .filter(tick -> tick.x() == x && tick.y() == y && tick.z() == z && tick.type() == type)
                .findFirst()
                .map(World.ScheduledBlockTickState::delayTicks)
                .orElse(-1);
    }

    private static String insertNullIntoJsonArray(String json, String marker) {
        int markerIndex = json.indexOf(marker);
        assertTrue(markerIndex >= 0, "save JSON should contain " + marker);
        int insertionPoint = markerIndex + marker.length();
        return json.substring(0, insertionPoint)
                + System.lineSeparator()
                + "    null,"
                + json.substring(insertionPoint);
    }

    private static String replaceFirstLiteral(String text, String target, String replacement) {
        int index = text.indexOf(target);
        assertTrue(index >= 0, "text should contain " + target);
        return text.substring(0, index) + replacement + text.substring(index + target.length());
    }

    private static String replaceFirstLiteralAfter(String text, String marker, String target, String replacement) {
        int markerIndex = text.indexOf(marker);
        assertTrue(markerIndex >= 0, "text should contain " + marker);
        int index = text.indexOf(target, markerIndex + marker.length());
        assertTrue(index >= 0, "text should contain " + target + " after " + marker);
        return text.substring(0, index) + replacement + text.substring(index + target.length());
    }

    private static ItemStack damagedTool(ItemType type, int uses) {
        ItemStack stack = new ItemStack(type, 1);
        for (int i = 0; i < uses; i++) {
            stack.useDurability();
        }
        return stack;
    }
}
