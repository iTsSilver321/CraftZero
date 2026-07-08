package com.craftzero.world;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.entity.DroppedItem;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.main.Player;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.EnchantingTableTileEntity;
import com.craftzero.world.tile.JukeboxTileEntity;
import com.craftzero.world.tile.MonsterSpawnerTileEntity;
import com.craftzero.world.tile.NoteBlockTileEntity;
import com.craftzero.world.tile.SignTileEntity;
import com.craftzero.world.tile.TileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TileEntityTest {

    @Test
    @DisplayName("World should create and remove tile entities with their blocks")
    void createsAndRemovesTileEntities() {
        World world = new World(1L);
        try {
            world.setBlock(1, 70, 1, BlockType.CHEST, 3);

            TileEntity tile = world.getTileEntity(1, 70, 1);
            assertInstanceOf(ChestTileEntity.class, tile);
            assertEquals(3, world.getBlockMetadata(1, 70, 1));

            world.setBlock(1, 70, 1, BlockType.AIR);
            assertNull(world.getTileEntity(1, 70, 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Chest placement should allow doubles but reject triples")
    void chestPlacementRulesRejectTriples() {
        World world = new World(2L);
        try {
            world.setBlock(0, 70, 0, BlockType.AIR);
            world.setBlock(1, 70, 0, BlockType.AIR);
            world.setBlock(2, 70, 0, BlockType.AIR);
            world.setBlock(0, 70, 1, BlockType.AIR);

            assertTrue(world.canPlaceChestAt(0, 70, 0));
            assertTrue(world.canPlaceBlockAt(0, 70, 0, BlockType.CHEST, 0, null));
            world.setBlock(0, 70, 0, BlockType.CHEST);

            assertTrue(world.canPlaceChestAt(1, 70, 0));
            assertTrue(world.canPlaceBlockAt(1, 70, 0, BlockType.CHEST, 0, null));
            world.setBlock(1, 70, 0, BlockType.CHEST);

            assertFalse(world.canPlaceChestAt(2, 70, 0));
            assertFalse(world.canPlaceBlockAt(2, 70, 0, BlockType.CHEST, 0, null));
            assertFalse(world.canPlaceChestAt(0, 70, 1));
            assertFalse(world.canPlaceBlockAt(0, 70, 1, BlockType.CHEST, 0, null));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Chest opening should be blocked by opaque blocks above either chest half")
    void chestOpeningRequiresUnblockedLid() {
        World world = new World(22L);
        try {
            world.setBlock(0, 70, 0, BlockType.AIR);
            world.setBlock(0, 71, 0, BlockType.AIR);
            world.setBlock(0, 70, 0, BlockType.CHEST);
            ChestTileEntity chest = (ChestTileEntity) world.getTileEntity(0, 70, 0);
            assertTrue(world.canOpenChest(chest));

            world.setBlock(0, 71, 0, BlockType.STONE);
            assertFalse(world.canOpenChest(chest));

            world.setBlock(0, 71, 0, BlockType.AIR);
            world.setBlock(1, 70, 0, BlockType.AIR);
            world.setBlock(1, 71, 0, BlockType.AIR);
            world.setBlock(1, 70, 0, BlockType.CHEST);
            ChestTileEntity adjacent = (ChestTileEntity) world.getTileEntity(1, 70, 0);
            assertTrue(world.canOpenChest(chest));
            assertTrue(world.canOpenChest(adjacent));

            world.setBlock(1, 71, 0, BlockType.STONE);
            assertFalse(world.canOpenChest(chest));
            assertFalse(world.canOpenChest(adjacent));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Breaking a chest should drop the chest and its contents")
    void breakingChestDropsContents() {
        World world = new World(3L);
        try {
            world.setBlock(0, 70, 0, BlockType.CHEST);
            ChestTileEntity chest = (ChestTileEntity) world.getTileEntity(0, 70, 0);
            chest.getInventory()[0] = new ItemStack(ItemType.DIAMOND, 3);

            assertTrue(world.breakBlock(0, 70, 0, true));

            assertSame(BlockType.AIR, world.getBlock(0, 70, 0));
            assertNull(world.getTileEntity(0, 70, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.CHEST));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.DIAMOND
                    && item.getCount() == 3));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Chest lid animation should advance at Release-style tenth steps")
    void chestLidAnimationUsesReleaseStyleStepRate() {
        World world = new World(23L);
        try {
            world.setBlock(0, 70, 0, BlockType.CHEST);
            ChestTileEntity chest = (ChestTileEntity) world.getTileEntity(0, 70, 0);

            chest.open();
            chest.tick(world, 1.0f / 20.0f);
            assertEquals(ChestTileEntity.LID_ANGLE_PER_TICK, chest.getLidAngle(), 0.0001f);

            for (int i = 0; i < 9; i++) {
                chest.tick(world, 1.0f / 20.0f);
            }
            assertEquals(1.0f, chest.getLidAngle(), 0.0001f);

            chest.close();
            chest.tick(world, 1.0f / 20.0f);
            assertEquals(1.0f - ChestTileEntity.LID_ANGLE_PER_TICK, chest.getLidAngle(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Chest lid sounds should follow Release-style animation thresholds")
    void chestLidSoundsFollowReleaseStyleAnimationThresholds() {
        World world = new World(25L);
        try {
            world.setBlock(0, 70, 0, BlockType.CHEST);
            ChestTileEntity chest = (ChestTileEntity) world.getTileEntity(0, 70, 0);

            chest.open();
            chest.tick(world, 1.0f / 20.0f);

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertChestSound(sounds.get(0), WorldSoundEvent.CHEST_OPEN, 0.5f, 70.5f, 0.5f);

            chest.tick(world, 1.0f / 20.0f);
            assertTrue(world.drainSoundEvents().isEmpty());

            chest.setLidAngle(0.61f);
            chest.close();
            chest.tick(world, 0.025f);
            chest.tick(world, 0.025f);
            assertTrue(world.drainSoundEvents().isEmpty());

            chest.tick(world, 0.025f);

            sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertChestSound(sounds.get(0), WorldSoundEvent.CHEST_CLOSE, 0.5f, 70.5f, 0.5f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Double chest lid sounds should emit once from the shared chest center")
    void doubleChestLidSoundsEmitOnceFromSharedCenter() {
        World world = new World(26L);
        try {
            world.setBlock(0, 70, 0, BlockType.CHEST);
            world.setBlock(1, 70, 0, BlockType.CHEST);
            ChestTileEntity west = (ChestTileEntity) world.getTileEntity(0, 70, 0);
            ChestTileEntity east = (ChestTileEntity) world.getTileEntity(1, 70, 0);

            west.open();
            east.open();
            world.tickTileEntities(1.0f / 20.0f);

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertChestSound(sounds.get(0), WorldSoundEvent.CHEST_OPEN, 1.0f, 70.5f, 0.5f);

            west.setLidAngle(0.61f);
            east.setLidAngle(0.61f);
            west.close();
            east.close();
            for (int i = 0; i < 3; i++) {
                world.tickTileEntities(0.025f);
            }

            sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertChestSound(sounds.get(0), WorldSoundEvent.CHEST_CLOSE, 1.0f, 70.5f, 0.5f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Chest viewer count should report only open and close threshold transitions")
    void chestViewerCountReportsThresholdTransitions() {
        World world = new World(24L);
        try {
            world.setBlock(0, 70, 0, BlockType.CHEST);
            ChestTileEntity chest = (ChestTileEntity) world.getTileEntity(0, 70, 0);

            assertTrue(chest.open());
            assertEquals(1, chest.getOpenCount());
            assertFalse(chest.open());
            assertEquals(2, chest.getOpenCount());

            assertFalse(chest.close());
            assertEquals(1, chest.getOpenCount());
            assertTrue(chest.close());
            assertEquals(0, chest.getOpenCount());
            assertFalse(chest.close());
            assertEquals(0, chest.getOpenCount());
        } finally {
            world.cleanup();
        }
    }

    private static void assertChestSound(WorldSoundEvent sound, String soundId, float x, float y, float z) {
        assertEquals(soundId, sound.soundId());
        assertEquals(x, sound.x(), 0.0001f);
        assertEquals(y, sound.y(), 0.0001f);
        assertEquals(z, sound.z(), 0.0001f);
        assertEquals(0.5f, sound.volume(), 0.0001f);
        assertTrue(sound.pitch() >= 0.9f);
        assertTrue(sound.pitch() <= 1.0f);
    }

    @Test
    @DisplayName("Enchanting tables should create a book-animation tile entity")
    void enchantingTableCreatesBookAnimationTileEntity() {
        World world = new World(45L);
        try {
            world.setBlock(0, 70, 0, BlockType.ENCHANTING_TABLE);
            assertTrue(BlockType.ENCHANTING_TABLE.hasTileEntity());
            assertInstanceOf(EnchantingTableTileEntity.class, world.getTileEntity(0, 70, 0));

            EnchantingTableTileEntity table = (EnchantingTableTileEntity) world.getTileEntity(0, 70, 0);
            world.setPlayer(new Player(0.5f, 70.0f, 2.5f));
            table.tick(world, 1.0f / 20.0f);

            assertEquals(1, table.getTickCount());
            assertEquals(EnchantingTableTileEntity.BOOK_SPREAD_STEP, table.getBookSpread(), 0.0001f);
            assertEquals((float) Math.PI / 2.0f, table.getBookRotation(), 0.0001f);

            world.setPlayer(null);
            table.tick(world, 1.0f / 20.0f);

            assertEquals(0.0f, table.getBookSpread(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Enchanting tables should emit old bookshelf enchantmenttable particles when powered")
    void enchantingTablesEmitBookshelfEnchantmentTableParticlesWhenPowered() {
        World world = new World(46L);
        try {
            world.setBlock(0, 70, 0, BlockType.ENCHANTING_TABLE);
            world.setBlock(1, 70, 0, BlockType.AIR, 0);
            world.setBlock(1, 71, 0, BlockType.AIR, 0);
            world.setBlock(2, 70, 0, BlockType.BOOKSHELF, 0);
            world.setPlayer(new Player(0.5f, 70.0f, 2.5f));
            EnchantingTableTileEntity table = (EnchantingTableTileEntity) world.getTileEntity(0, 70, 0);

            for (int i = 0; i < 80 && world.getParticles().isEmpty(); i++) {
                table.tick(world, 1.0f / 20.0f);
            }

            assertTrue(world.getParticles().stream()
                    .anyMatch(particle -> particle.getType() == WorldParticle.Type.ENCHANTMENT_TABLE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Signs should create tile entities and preserve text in memory")
    void signsCreateTileEntities() {
        World world = new World(4L);
        try {
            world.setBlock(0, 70, 0, BlockType.STANDING_SIGN, 0);
            assertInstanceOf(SignTileEntity.class, world.getTileEntity(0, 70, 0));
            SignTileEntity sign = (SignTileEntity) world.getTileEntity(0, 70, 0);
            sign.setLine(0, "CraftZero");
            assertEquals("CraftZero", sign.getLines()[0]);
            sign.setLine(1, "12345678901234567890");
            assertEquals("123456789012345", sign.getLines()[1]);
            sign.setLine(2, "A\nB" + (char) 0xA7 + "C" + (char) 0x7F + "D" + (char) 0xE9
                    + (char) 0xC0 + "\u20AC");
            assertEquals("ABCD" + (char) 0xE9, sign.getLines()[2]);
            sign.setLine(3, null);
            assertEquals("", sign.getLines()[3]);

            String[] exposedLines = sign.getLines();
            exposedLines[0] = "bypassed";
            assertEquals("CraftZero", sign.getLines()[0]);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mob spawner block should create and remove a monster spawner tile entity")
    void mobSpawnerBlockCreatesMonsterSpawnerTile() {
        World world = new World(44L);
        try {
            world.setBlock(0, 70, 0, BlockType.MOB_SPAWNER);

            TileEntity tile = world.getTileEntity(0, 70, 0);
            assertInstanceOf(MonsterSpawnerTileEntity.class, tile);
            MonsterSpawnerTileEntity spawner = (MonsterSpawnerTileEntity) tile;
            assertSame(MobDefinition.PIG, spawner.getMobDefinition());

            world.setBlock(0, 70, 0, BlockType.AIR);
            assertNull(world.getTileEntity(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Note blocks should wrap pitch, require air above, and choose vanilla instruments")
    void noteBlockUsesVanillaPitchAndInstrumentRules() {
        World world = new World(45L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE);
            world.setBlock(0, 70, 0, BlockType.NOTE_BLOCK);
            NoteBlockTileEntity note = (NoteBlockTileEntity) world.getTileEntity(0, 70, 0);

            note.setPitch(24);
            note.cyclePitch();
            assertEquals(0, note.getPitch());
            assertTrue(note.play(world));
            assertEquals(1, note.getPlayTicks());
            assertEquals(NoteBlockTileEntity.INSTRUMENT_BASS_DRUM, note.getLastInstrument());
            assertEquals(1, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.NOTE)
                    .count());

            world.setBlock(0, 71, 0, BlockType.STONE);
            note.cyclePitch();
            assertEquals(1, note.getPitch());
            assertFalse(note.play(world));
            assertEquals(1, note.getPlayTicks());
            assertEquals(1, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.NOTE)
                    .count());

            assertEquals(NoteBlockTileEntity.INSTRUMENT_HARP, NoteBlockTileEntity.instrumentFor(BlockType.DIRT));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_SNARE, NoteBlockTileEntity.instrumentFor(BlockType.SAND));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_STICKS, NoteBlockTileEntity.instrumentFor(BlockType.GLASS));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_STICKS,
                    NoteBlockTileEntity.instrumentFor(BlockType.GLOWSTONE));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_BASS, NoteBlockTileEntity.instrumentFor(BlockType.OAK_PLANKS));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_BASS_DRUM,
                    NoteBlockTileEntity.instrumentFor(BlockType.STONE_PRESSURE_PLATE));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_BASS_DRUM,
                    NoteBlockTileEntity.instrumentFor(BlockType.DIAMOND_ORE));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_BASS_DRUM,
                    NoteBlockTileEntity.instrumentFor(BlockType.MOSSY_COBBLESTONE));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_BASS_DRUM,
                    NoteBlockTileEntity.instrumentFor(BlockType.OBSIDIAN));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_BASS,
                    NoteBlockTileEntity.instrumentFor(BlockType.WOODEN_PRESSURE_PLATE));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_BASS_DRUM,
                    NoteBlockTileEntity.instrumentFor(BlockType.ENCHANTING_TABLE));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_BASS_DRUM,
                    NoteBlockTileEntity.instrumentFor(BlockType.END_PORTAL_FRAME));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_HARP,
                    NoteBlockTileEntity.instrumentFor(BlockType.LAPIS_BLOCK));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_HARP,
                    NoteBlockTileEntity.instrumentFor(BlockType.IRON_BARS));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_HARP,
                    NoteBlockTileEntity.instrumentFor(BlockType.PISTON));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_HARP,
                    NoteBlockTileEntity.instrumentFor(BlockType.PISTON_HEAD));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_HARP,
                    NoteBlockTileEntity.instrumentFor(BlockType.MOVING_PISTON));
            assertEquals(NoteBlockTileEntity.INSTRUMENT_HARP,
                    NoteBlockTileEntity.instrumentFor(BlockType.INFESTED_STONE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Punched note blocks should play without changing pitch")
    void punchedNoteBlockPlaysWithoutCyclingPitch() {
        World world = new World(451L);
        try {
            world.setBlock(0, 69, 0, BlockType.OAK_PLANKS);
            world.setBlock(0, 70, 0, BlockType.NOTE_BLOCK);
            NoteBlockTileEntity note = (NoteBlockTileEntity) world.getTileEntity(0, 70, 0);
            note.setPitch(7);

            assertTrue(world.playNoteBlock(0, 70, 0));

            assertEquals(7, note.getPitch());
            assertEquals(1, note.getPlayTicks());
            assertEquals(NoteBlockTileEntity.INSTRUMENT_BASS, note.getLastInstrument());
            WorldSoundEvent sound = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.NOTE_BASS, sound.soundId());
            assertEquals(WorldSoundEvent.notePitch(7), sound.pitch(), 0.0001f);
            assertEquals(1, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.NOTE)
                    .count());
            WorldParticle noteParticle = world.getParticles().stream()
                    .filter(effect -> effect.getType() == WorldParticle.Type.NOTE)
                    .findFirst()
                    .orElseThrow();
            assertEquals(7.0f / 24.0f, noteParticle.getData(), 0.0001f);
            float startX = noteParticle.getRenderX(1.0f);
            float startY = noteParticle.getRenderY(1.0f);
            float startZ = noteParticle.getRenderZ(1.0f);

            assertFalse(noteParticle.update(1.0f / 20.0f));

            assertEquals(startX, noteParticle.getRenderX(1.0f), 0.0001f);
            assertTrue(noteParticle.getRenderY(1.0f) > startY);
            assertEquals(startZ, noteParticle.getRenderZ(1.0f), 0.0001f);

            world.setBlock(0, 71, 0, BlockType.STONE);
            assertFalse(world.playNoteBlock(0, 70, 0));
            assertEquals(7, note.getPitch());
            assertEquals(1, note.getPlayTicks());
            assertEquals(1, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.NOTE)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Right-clicked note blocks should cycle pitch then play the new note")
    void rightClickedNoteBlockCyclesPitchThenPlays() {
        World world = new World(452L);
        try {
            world.setBlock(0, 69, 0, BlockType.OAK_PLANKS);
            world.setBlock(0, 70, 0, BlockType.NOTE_BLOCK);
            NoteBlockTileEntity note = (NoteBlockTileEntity) world.getTileEntity(0, 70, 0);
            note.setPitch(24);

            assertTrue(world.toggleBlock(0, 70, 0));

            assertEquals(0, note.getPitch());
            assertEquals(1, note.getPlayTicks());
            assertEquals(NoteBlockTileEntity.INSTRUMENT_BASS, note.getLastInstrument());
            WorldSoundEvent sound = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.NOTE_BASS, sound.soundId());
            assertEquals(WorldSoundEvent.notePitch(0), sound.pitch(), 0.0001f);
            WorldParticle particle = world.getParticles().stream()
                    .filter(effect -> effect.getType() == WorldParticle.Type.NOTE)
                    .findFirst()
                    .orElseThrow();
            assertEquals(0.0f, particle.getData(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Blocked note blocks should still tune without playing")
    void blockedNoteBlockStillCyclesPitchWithoutPlayback() {
        World world = new World(453L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE);
            world.setBlock(0, 70, 0, BlockType.NOTE_BLOCK);
            world.setBlock(0, 71, 0, BlockType.STONE);
            NoteBlockTileEntity note = (NoteBlockTileEntity) world.getTileEntity(0, 70, 0);
            note.setPitch(3);

            assertTrue(world.toggleBlock(0, 70, 0));

            assertEquals(4, note.getPitch());
            assertEquals(0, note.getPlayTicks());
            assertTrue(world.drainSoundEvents().isEmpty());
            assertTrue(world.getParticles().stream()
                    .noneMatch(effect -> effect.getType() == WorldParticle.Type.NOTE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Jukeboxes should keep one record and track playback")
    void jukeboxKeepsRecordAndTracksPlayback() {
        World world = new World(46L);
        try {
            world.setBlock(0, 70, 0, BlockType.JUKEBOX);
            JukeboxTileEntity jukebox = (JukeboxTileEntity) world.getTileEntity(0, 70, 0);
            ItemStack stack = new ItemStack(ItemType.RECORD_13, 3);

            assertTrue(jukebox.insertRecord(world, stack));
            assertFalse(jukebox.insertRecord(new ItemStack(ItemType.RECORD_CAT, 1)));
            assertTrue(jukebox.hasRecord());
            assertEquals(1, world.getBlockMetadata(0, 70, 0));
            assertEquals(1, jukebox.getRecord().getCount());
            assertSame(ItemType.RECORD_13, jukebox.getRecord().getType());

            jukebox.play(world);
            assertEquals(1, jukebox.getPlayTicks());
            assertSame(ItemType.RECORD_13, jukebox.removeRecord(world).getType());
            assertFalse(jukebox.hasRecord());
            assertEquals(0, world.getBlockMetadata(0, 70, 0));
            assertEquals(0, jukebox.getPlayTicks());

            assertTrue(jukebox.insertRecord(world, new ItemStack(ItemType.RECORD_CAT, 1)));
            assertEquals(1, world.getBlockMetadata(0, 70, 0));
            assertEquals(0, jukebox.getPlayTicks());
            jukebox.play(world);
            assertEquals(1, jukebox.getPlayTicks());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Jukebox metadata sync should rebuild the jukebox block")
    void jukeboxMetadataSyncRebuildsBlock() {
        RecordingWorld world = new RecordingWorld(462L);
        try {
            world.setBlock(0, 70, 0, BlockType.JUKEBOX, 0);
            JukeboxTileEntity jukebox = (JukeboxTileEntity) world.getTileEntity(0, 70, 0);

            assertTrue(jukebox.insertRecord(world, new ItemStack(ItemType.RECORD_13, 1)));
            assertEquals(1, world.getBlockMetadata(0, 70, 0));
            assertEquals(1, world.rebuildCount);
            assertTrue(world.rebuilt(0, 70, 0));

            assertSame(ItemType.RECORD_13, jukebox.removeRecord(world).getType());
            assertEquals(0, world.getBlockMetadata(0, 70, 0));
            assertEquals(2, world.rebuildCount);
            assertTrue(world.rebuilt(0, 70, 0));

            assertNull(jukebox.removeRecord(world));
            assertEquals(2, world.rebuildCount);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Jukebox interaction should follow block metadata before tile contents")
    void jukeboxInteractionFollowsBlockMetadataGate() {
        World world = new World(461L);
        try {
            world.setBlock(0, 70, 0, BlockType.JUKEBOX, 0);
            JukeboxTileEntity jukebox = (JukeboxTileEntity) world.getTileEntity(0, 70, 0);
            assertTrue(jukebox.insertRecord(new ItemStack(ItemType.RECORD_13, 1)));

            assertFalse(world.toggleBlock(0, 70, 0));
            assertTrue(jukebox.hasRecord());
            assertEquals(0, world.getBlockMetadata(0, 70, 0));
            assertTrue(world.getDroppedItems().isEmpty());
            assertTrue(world.drainSoundEvents().isEmpty());

            world.setBlockPreservingTile(0, 70, 0, BlockType.JUKEBOX, 1);
            assertTrue(world.toggleBlock(0, 70, 0));

            assertFalse(jukebox.hasRecord());
            assertEquals(0, world.getBlockMetadata(0, 70, 0));
            assertEquals(1, world.getDroppedItems().size());
            assertSame(ItemType.RECORD_13, world.getDroppedItems().get(0).getItemType());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Breaking a recorded jukebox should eject the record through the source pop path")
    void breakingRecordedJukeboxEjectsRecordBeforeBlockDrop() {
        World world = new World(463L);
        try {
            world.setBlock(0, 70, 0, BlockType.JUKEBOX);
            JukeboxTileEntity jukebox = (JukeboxTileEntity) world.getTileEntity(0, 70, 0);
            assertTrue(jukebox.insertRecord(world, new ItemStack(ItemType.RECORD_CAT, 1)));

            assertTrue(world.breakBlock(0, 70, 0, true));

            assertSame(BlockType.AIR, world.getBlock(0, 70, 0));
            assertNull(world.getTileEntity(0, 70, 0));
            assertFalse(jukebox.hasRecord());

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertEquals(WorldSoundEvent.RECORD_EJECT, sounds.get(0).soundId());

            DroppedItem record = world.getDroppedItems().stream()
                    .filter(item -> item.getItemType() == ItemType.RECORD_CAT)
                    .findFirst()
                    .orElseThrow();
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.JUKEBOX));
            assertTrue(record.getX() >= 0.15f && record.getX() <= 0.85f);
            assertTrue(record.getY() >= 70.66f && record.getY() <= 71.36f);
            assertTrue(record.getZ() >= 0.15f && record.getZ() <= 0.85f);
            assertEquals(0.2f, record.getVelocityY(), 0.0001f);
            assertEquals(DroppedItem.DEFAULT_PICKUP_DELAY_TICKS, record.getPickupDelayTicks());
            assertFalse(record.canPickup());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Attachable blocks should break when their support is removed")
    void supportRemovalBreaksAttachables() {
        World world = new World(5L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE);
            world.setBlock(0, 71, 0, BlockType.TORCH, 5);
            assertSame(BlockType.TORCH, world.getBlock(0, 71, 0));

            world.breakBlock(0, 70, 0, false);

            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.TORCH));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Door and bed helpers should place and break paired blocks")
    void pairedBlocksPlaceAndBreakTogether() {
        World world = new World(6L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE);
            world.setBlock(0, 70, 0, BlockType.AIR);
            world.setBlock(0, 71, 0, BlockType.AIR);
            assertTrue(world.placeDoor(0, 70, 0, BlockType.WOODEN_DOOR, 0, null));
            assertSame(BlockType.WOODEN_DOOR, world.getBlock(0, 70, 0));
            assertSame(BlockType.WOODEN_DOOR, world.getBlock(0, 71, 0));

            world.breakBlock(0, 71, 0, true);
            assertSame(BlockType.AIR, world.getBlock(0, 70, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertEquals(1, world.getDroppedItems().stream()
                    .filter(item -> item.getItemType() == ItemType.WOODEN_DOOR)
                    .mapToInt(item -> item.getStack().getCount())
                    .sum());

            world.setBlock(1, 69, 0, BlockType.STONE);
            world.setBlock(1, 69, -1, BlockType.STONE);
            world.setBlock(1, 70, 0, BlockType.AIR);
            world.setBlock(1, 70, -1, BlockType.AIR);
            assertNotNull(world.placeBed(1, 70, 0, 0, null));
            assertSame(BlockType.BED, world.getBlock(1, 70, 0));
            assertSame(BlockType.BED, world.getBlock(1, 70, -1));

            world.breakBlock(1, 70, -1, true);
            assertSame(BlockType.AIR, world.getBlock(1, 70, 0));
            assertSame(BlockType.AIR, world.getBlock(1, 70, -1));
            assertTrue(world.getDroppedItems().stream().anyMatch(item -> item.getItemType() == ItemType.BED));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Orphaned upper doors should not drop an item or clear lower blocks")
    void orphanedUpperDoorBreakDoesNotDropItem() {
        World world = new World(665L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            world.setBlock(0, 71, 0, BlockType.WOODEN_DOOR, 8);

            assertTrue(world.breakBlock(0, 71, 0, true));

            assertSame(BlockType.STONE, world.getBlock(0, 70, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertTrue(world.getDroppedItems().stream()
                    .noneMatch(item -> item.getItemType() == ItemType.WOODEN_DOOR));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Door placement should write source upper-half hinge metadata")
    void doorPlacementWritesSourceUpperHingeMetadata() {
        World world = new World(664L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE);
            world.setBlock(0, 70, 1, BlockType.STONE);
            world.setBlock(0, 71, 1, BlockType.STONE);

            assertTrue(world.placeDoor(0, 70, 0, BlockType.WOODEN_DOOR, 0, null));
            assertEquals(9, world.getBlockMetadata(0, 71, 0));

            world.setBlock(2, 69, 0, BlockType.STONE);
            world.setBlock(2, 70, -1, BlockType.STONE);
            world.setBlock(2, 71, -1, BlockType.STONE);

            assertTrue(world.placeDoor(2, 70, 0, BlockType.WOODEN_DOOR, 0, null));
            assertEquals(8, world.getBlockMetadata(2, 71, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Openable blocks should follow Release 1.0 toggle rules")
    void openableBlocksToggleLikeReleaseOne() {
        World world = new World(66L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE);
            assertTrue(world.placeDoor(0, 70, 0, BlockType.WOODEN_DOOR, 0, null));
            assertTrue(world.toggleBlock(0, 71, 0));
            assertEquals(RedstoneEngine.DOOR_OPEN_BIT, world.getBlockMetadata(0, 70, 0) & RedstoneEngine.DOOR_OPEN_BIT);
            assertTrue(world.toggleBlock(0, 70, 0));
            assertEquals(0, world.getBlockMetadata(0, 70, 0) & RedstoneEngine.DOOR_OPEN_BIT);

            world.setBlock(1, 69, 0, BlockType.STONE);
            assertTrue(world.placeDoor(1, 70, 0, BlockType.IRON_DOOR, 0, null));
            assertFalse(world.toggleBlock(1, 70, 0));
            assertEquals(0, world.getBlockMetadata(1, 70, 0) & RedstoneEngine.DOOR_OPEN_BIT);

            world.setBlock(2, 70, 0, BlockType.STONE);
            world.setBlock(3, 70, 0, BlockType.TRAPDOOR, 3);
            assertTrue(world.toggleBlock(3, 70, 0));
            assertEquals(RedstoneEngine.DOOR_OPEN_BIT, world.getBlockMetadata(3, 70, 0) & RedstoneEngine.DOOR_OPEN_BIT);

            world.setBlock(4, 70, 0, BlockType.FENCE_GATE, 1);
            assertTrue(world.toggleBlock(4, 70, 0));
            assertEquals(RedstoneEngine.DOOR_OPEN_BIT, world.getBlockMetadata(4, 70, 0) & RedstoneEngine.DOOR_OPEN_BIT);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Orphaned upper wooden doors should not recreate missing lower halves")
    void orphanedUpperDoorToggleDoesNotCreateLowerDoor() {
        World world = new World(662L);
        try {
            world.setBlock(0, 70, 0, BlockType.AIR, 0);
            world.setBlock(0, 71, 0, BlockType.WOODEN_DOOR, 8);

            assertTrue(world.toggleBlock(0, 71, 0));

            assertSame(BlockType.AIR, world.getBlock(0, 70, 0));
            assertSame(BlockType.WOODEN_DOOR, world.getBlock(0, 71, 0));
            assertEquals(8, world.getBlockMetadata(0, 71, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Powered orphaned upper doors should not recreate missing lower halves")
    void poweredOrphanedUpperDoorTickDoesNotCreateLowerDoor() {
        World world = new World(663L);
        try {
            setPoweredLever(world, 1, 71, 0, true);
            world.setBlock(0, 70, 0, BlockType.AIR, 0);
            world.setBlock(0, 71, 0, BlockType.WOODEN_DOOR, 8);

            world.scheduleBlockTick(0, 71, 0, BlockType.WOODEN_DOOR, 0);
            world.advanceBlockTicks(1);

            assertSame(BlockType.AIR, world.getBlock(0, 70, 0));
            assertSame(BlockType.WOODEN_DOOR, world.getBlock(0, 71, 0));
            assertEquals(8, world.getBlockMetadata(0, 71, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Manual fence-gate opening should face the player when approached from behind")
    void manualFenceGateOpeningFacesPlayerFromBehind() {
        World world = new World(661L);
        try {
            world.setBlock(0, 70, 0, BlockType.FENCE_GATE, 0);

            assertTrue(world.toggleBlock(0, 70, 0, 2));
            assertEquals(2 | RedstoneEngine.DOOR_OPEN_BIT, world.getBlockMetadata(0, 70, 0));

            assertTrue(world.toggleBlock(0, 70, 0, 1));
            assertEquals(2, world.getBlockMetadata(0, 70, 0));

            world.setBlock(2, 70, 0, BlockType.FENCE_GATE, 1);
            assertTrue(world.toggleBlock(2, 70, 0, 2));
            assertEquals(1 | RedstoneEngine.DOOR_OPEN_BIT, world.getBlockMetadata(2, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Powered wooden doors keep manual state until the redstone signal changes")
    void poweredWoodenDoorManualToggleWaitsForSignalEdge() {
        World world = new World(67L);
        try {
            world.setBlock(8, 69, 8, BlockType.STONE);
            world.setBlock(8, 70, 8, BlockType.AIR);
            world.setBlock(8, 71, 8, BlockType.AIR);
            assertTrue(world.placeDoor(8, 70, 8, BlockType.WOODEN_DOOR, 0, null));

            setPoweredLever(world, 9, 70, 8, true);
            world.advanceBlockTicks(2);
            assertOpen(world, 8, 70, 8);

            assertTrue(world.toggleBlock(8, 70, 8));
            assertClosed(world, 8, 70, 8);
            world.advanceBlockTicks(2);
            assertClosed(world, 8, 70, 8);

            setPoweredLever(world, 9, 70, 8, false);
            world.advanceBlockTicks(2);
            assertClosed(world, 8, 70, 8);

            setPoweredLever(world, 9, 70, 8, true);
            world.advanceBlockTicks(2);
            assertOpen(world, 8, 70, 8);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Powered trapdoors keep manual state until the redstone signal changes")
    void poweredTrapdoorManualToggleWaitsForSignalEdge() {
        World world = new World(68L);
        try {
            world.setBlock(8, 70, 8, BlockType.STONE);
            assertTrue(world.placeTrapdoor(8, 70, 7, Block.FACE_NORTH, null));

            setPoweredLever(world, 9, 70, 7, true);
            world.advanceBlockTicks(2);
            assertOpen(world, 8, 70, 7);

            assertTrue(world.toggleBlock(8, 70, 7));
            assertClosed(world, 8, 70, 7);
            world.advanceBlockTicks(2);
            assertClosed(world, 8, 70, 7);

            setPoweredLever(world, 9, 70, 7, false);
            world.advanceBlockTicks(2);
            assertClosed(world, 8, 70, 7);

            setPoweredLever(world, 9, 70, 7, true);
            world.advanceBlockTicks(2);
            assertOpen(world, 8, 70, 7);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Powered fence gates keep manual state until the redstone signal changes")
    void poweredFenceGateManualToggleWaitsForSignalEdge() {
        World world = new World(69L);
        try {
            world.setBlock(8, 70, 8, BlockType.FENCE_GATE, 0);

            setPoweredLever(world, 9, 70, 8, true);
            world.advanceBlockTicks(2);
            assertOpen(world, 8, 70, 8);

            assertTrue(world.toggleBlock(8, 70, 8));
            assertClosed(world, 8, 70, 8);
            world.advanceBlockTicks(2);
            assertClosed(world, 8, 70, 8);

            setPoweredLever(world, 9, 70, 8, false);
            world.advanceBlockTicks(2);
            assertClosed(world, 8, 70, 8);

            setPoweredLever(world, 9, 70, 8, true);
            world.advanceBlockTicks(2);
            assertOpen(world, 8, 70, 8);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Redstone-driven openable changes should rebuild changed door, trapdoor, and gate meshes")
    void redstoneOpenableChangesRebuildMeshes() {
        RecordingWorld world = new RecordingWorld(690L);
        try {
            world.setBlock(8, 69, 8, BlockType.STONE);
            world.setBlock(8, 70, 8, BlockType.AIR);
            world.setBlock(8, 71, 8, BlockType.AIR);
            assertTrue(world.placeDoor(8, 70, 8, BlockType.WOODEN_DOOR, 0, null));
            setPoweredLever(world, 9, 70, 8, true);
            world.advanceBlockTicks(2);

            assertOpen(world, 8, 70, 8);
            assertEquals(2, world.rebuildCount);
            assertTrue(world.rebuilt(8, 70, 8));
            assertTrue(world.rebuilt(8, 71, 8));

            world.setBlock(12, 70, 8, BlockType.STONE);
            world.setBlock(12, 70, 7, BlockType.AIR);
            assertTrue(world.placeTrapdoor(12, 70, 7, Block.FACE_NORTH, null));
            setPoweredLever(world, 13, 70, 7, true);
            world.advanceBlockTicks(2);

            assertOpen(world, 12, 70, 7);
            assertEquals(3, world.rebuildCount);
            assertTrue(world.rebuilt(12, 70, 7));

            world.setBlock(16, 70, 8, BlockType.FENCE_GATE, 0);
            setPoweredLever(world, 17, 70, 8, true);
            world.advanceBlockTicks(2);

            assertOpen(world, 16, 70, 8);
            assertEquals(4, world.rebuildCount);
            assertTrue(world.rebuilt(16, 70, 8));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Open unpowered openables should survive the first redstone tick after metadata restore")
    void openUnpoweredOpenablesSurviveInitialRedstoneTick() {
        World world = new World(70L);
        try {
            world.setBlock(8, 69, 8, BlockType.STONE);
            assertTrue(world.placeDoor(8, 70, 8, BlockType.WOODEN_DOOR, 0, null));
            world.setBlock(8, 70, 8, BlockType.WOODEN_DOOR, RedstoneEngine.DOOR_OPEN_BIT);

            world.setBlock(10, 70, 8, BlockType.STONE);
            assertTrue(world.placeTrapdoor(10, 70, 7, Block.FACE_NORTH, null));
            world.setBlock(10, 70, 7, BlockType.TRAPDOOR,
                    world.getBlockMetadata(10, 70, 7) | RedstoneEngine.DOOR_OPEN_BIT);

            world.setBlock(12, 70, 8, BlockType.FENCE_GATE, RedstoneEngine.DOOR_OPEN_BIT);

            world.advanceBlockTicks(1);

            assertOpen(world, 8, 70, 8);
            assertOpen(world, 10, 70, 7);
            assertOpen(world, 12, 70, 8);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Closed powered openables should preserve restored metadata until the signal changes")
    void closedPoweredOpenablesSurviveInitialRedstoneTick() throws Exception {
        World world = new World(71L);
        try {
            world.setBlock(8, 69, 8, BlockType.STONE);
            world.setBlock(8, 70, 8, BlockType.WOODEN_DOOR, 0);
            world.setBlock(8, 71, 8, BlockType.WOODEN_DOOR, 8);
            setPoweredLever(world, 9, 70, 8, true);

            world.setBlock(10, 70, 8, BlockType.STONE);
            world.setBlock(10, 70, 7, BlockType.TRAPDOOR, 2);
            setPoweredLever(world, 11, 70, 7, true);

            world.setBlock(12, 70, 8, BlockType.FENCE_GATE, 0);
            setPoweredLever(world, 13, 70, 8, true);

            rescheduleLoadedChunkTicks(world);
            world.advanceBlockTicks(1);

            assertClosed(world, 8, 70, 8);
            assertClosed(world, 10, 70, 7);
            assertClosed(world, 12, 70, 8);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Block shapes should expose non-cube collision and selection")
    void nonCubeShapesExposeExpectedBoxes() {
        World world = new World(7L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE_SLAB);
            assertEquals(1, world.getCollisionBoxes(0, 70, 0).size());
            assertEquals(70.5f, world.getCollisionBoxes(0, 70, 0).get(0).getMax().y, 0.001f);

            world.setBlock(1, 70, 0, BlockType.LADDER, Block.FACE_NORTH);
            assertTrue(world.getCollisionBoxes(1, 70, 0).isEmpty());
            assertFalse(world.getSelectionBoxes(1, 70, 0).isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Slabs should merge into double slabs and drop two slab items")
    void slabsMergeAndDropTwoItems() {
        World world = new World(8L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE_SLAB);
            assertTrue(world.tryMergeSlab(0, 70, 0));
            assertSame(BlockType.DOUBLE_STONE_SLAB, world.getBlock(0, 70, 0));

            world.breakBlock(0, 70, 0, true);
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.STONE_SLAB && item.getCount() == 2));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Torch and lit furnace should emit block light")
    void lightEmittersProduceBlockLight() {
        World world = new World(9L);
        try {
            world.setBlock(0, 70, 0, BlockType.TORCH, 5);
            assertTrue(world.getBlockLight(0, 70, 0) >= 14);

            world.setBlock(1, 70, 0, BlockType.LIT_FURNACE, 2);
            assertTrue(world.getBlockLight(1, 70, 0) >= 13);
        } finally {
            world.cleanup();
        }
    }

    private static void setPoweredLever(World world, int x, int y, int z, boolean powered) {
        world.setBlock(x, y - 1, z, BlockType.STONE);
        int metadata = BlockShape.leverMetadataFromFace(Block.FACE_TOP);
        world.setBlock(x, y, z, BlockType.LEVER, powered ? metadata | RedstoneEngine.POWERED_BIT : metadata);
    }

    private static void rescheduleLoadedChunkTicks(World world) throws Exception {
        Method scheduler = World.class.getDeclaredMethod("scheduleTickableBlocksInChunk", Chunk.class);
        scheduler.setAccessible(true);
        scheduler.invoke(world, world.getLoadedChunk(0, 0));
    }

    private static void assertOpen(World world, int x, int y, int z) {
        assertEquals(RedstoneEngine.DOOR_OPEN_BIT,
                world.getBlockMetadata(x, y, z) & RedstoneEngine.DOOR_OPEN_BIT);
    }

    private static void assertClosed(World world, int x, int y, int z) {
        assertEquals(0, world.getBlockMetadata(x, y, z) & RedstoneEngine.DOOR_OPEN_BIT);
    }

    private static final class RecordingWorld extends World {
        private int rebuildCount;
        private final java.util.Set<String> rebuilds = new java.util.HashSet<>();

        private RecordingWorld(long seed) {
            super(seed);
        }

        @Override
        public void rebuildBlockMeshesNow(int x, int y, int z) {
            rebuildCount++;
            rebuilds.add(key(x, y, z));
        }

        private boolean rebuilt(int x, int y, int z) {
            return rebuilds.contains(key(x, y, z));
        }

        private static String key(int x, int y, int z) {
            return x + "," + y + "," + z;
        }
    }
}
