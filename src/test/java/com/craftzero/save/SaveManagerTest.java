package com.craftzero.save;

import com.craftzero.entity.Entity;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.progression.EnchantmentInstance;
import com.craftzero.progression.EnchantmentType;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.world.BlockType;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.World;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.FurnaceTileEntity;
import com.craftzero.world.tile.MonsterSpawnerTileEntity;
import com.craftzero.world.tile.SignTileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
            player.getInventory().getHotbar()[1] = damagedTool(ItemType.BOW, 2);
            player.getInventory().getHotbar()[1].addEnchantment(new EnchantmentInstance(EnchantmentType.POWER, 2));
            player.getInventory().getHotbar()[1].putMetadata("test", "bow");
            player.getInventory().getMainInventory()[4] = new ItemStack(ItemType.CHARCOAL, 7);
            player.getInventory().getCraftingGrid()[0] = new ItemStack(ItemType.OAK_PLANKS, 2);
            player.getInventory().getArmor()[0] = damagedTool(ItemType.IRON_HELMET, 1);
            player.getInventory().setCursorItem(new ItemStack(ItemType.STICK, 5));
            player.getStats().restore(14.0f, 16.0f, 4.0f, 9.0f);
            player.getStats().getProgression().restore(120, 42);
            player.getStats().addEffect(new StatusEffectInstance(StatusEffectType.POISON, 80, 0));

            DayCycleManager dayCycle = new DayCycleManager();
            dayCycle.setTime(18234.5f);

            world.setSaveManager(manager);
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
            world.setBlock(4, 70, 1, BlockType.STANDING_SIGN, 2);
            SignTileEntity sign = (SignTileEntity) world.getTileEntity(4, 70, 1);
            sign.setLine(0, "Release");
            sign.setLine(1, "One");
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
            assertEquals(9.0f, restoredPlayer.getStats().getCurrentAir(), 0.001f);
            assertEquals(18234.5f, restoredDayCycle.getTime(), 0.001f);
            assertEquals(120, restoredPlayer.getStats().getProgression().getTotalExperience());
            assertEquals(42, restoredPlayer.getStats().getProgression().getScore());
            assertEquals(1, restoredPlayer.getStats().getActiveEffects().size());
            assertSame(StatusEffectType.POISON, restoredPlayer.getStats().getActiveEffects().get(0).type());

            assertEquals(2, restoredPlayer.getInventory().getSelectedSlot());
            assertSame(ItemType.IRON_PICKAXE, restoredPlayer.getInventory().getHotbar()[0].getType());
            assertEquals(player.getInventory().getHotbar()[0].getDurability(),
                    restoredPlayer.getInventory().getHotbar()[0].getDurability());
            assertSame(ItemType.BOW, restoredPlayer.getInventory().getHotbar()[1].getType());
            assertEquals(player.getInventory().getHotbar()[1].getDurability(),
                    restoredPlayer.getInventory().getHotbar()[1].getDurability());
            assertEquals(player.getInventory().getHotbar()[1].getEnchantments(),
                    restoredPlayer.getInventory().getHotbar()[1].getEnchantments());
            assertEquals("bow", restoredPlayer.getInventory().getHotbar()[1].getMetadata().get("test"));
            assertSame(ItemType.CHARCOAL, restoredPlayer.getInventory().getMainInventory()[4].getType());
            assertEquals(7, restoredPlayer.getInventory().getMainInventory()[4].getCount());
            assertSame(ItemType.OAK_PLANKS, restoredPlayer.getInventory().getCraftingGrid()[0].getType());
            assertSame(ItemType.IRON_HELMET, restoredPlayer.getInventory().getArmor()[0].getType());
            assertSame(ItemType.STICK, restoredPlayer.getInventory().getCursorItem().getType());

            assertEquals(1, restoredWorld.getDroppedItems().size());
            assertSame(ItemType.STONE_SWORD, restoredWorld.getDroppedItems().get(0).getItemType());
            assertEquals(thrownStack.getDurability(), restoredWorld.getDroppedItems().get(0).getDurability());

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

            assertSame(BlockType.STANDING_SIGN, restoredWorld.getBlock(4, 70, 1));
            assertEquals(2, restoredWorld.getBlockMetadata(4, 70, 1));
            assertInstanceOf(SignTileEntity.class, restoredWorld.getTileEntity(4, 70, 1));
            SignTileEntity restoredSign = (SignTileEntity) restoredWorld.getTileEntity(4, 70, 1);
            assertEquals("Release", restoredSign.getLines()[0]);
            assertEquals("One", restoredSign.getLines()[1]);
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
        } finally {
            world.cleanup();
            if (restoredWorld != null) {
                restoredWorld.cleanup();
            }
        }
    }

    private static ItemStack damagedTool(ItemType type, int uses) {
        ItemStack stack = new ItemStack(type, 1);
        for (int i = 0; i < uses; i++) {
            stack.useDurability();
        }
        return stack;
    }
}
