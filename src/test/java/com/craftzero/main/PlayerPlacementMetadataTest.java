package com.craftzero.main;

import com.craftzero.engine.Input;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.physics.Raycast;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;
import com.craftzero.world.tile.JukeboxTileEntity;
import com.craftzero.world.tile.NoteBlockTileEntity;
import org.joml.Vector3i;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerPlacementMetadataTest {
    @Test
    @DisplayName("Piston placement should face up when the player places it from above")
    void pistonPlacementFacesUpFromAbove() {
        Player player = new Player(0.5f, 104.0f, 0.5f);

        assertEquals(Block.FACE_TOP, player.getPistonPlacementMetadata(new Vector3i(0, 100, 0)));
    }

    @Test
    @DisplayName("Piston placement should face down when the player places it from below")
    void pistonPlacementFacesDownFromBelow() {
        Player player = new Player(0.5f, 98.0f, 0.5f);

        assertEquals(Block.FACE_BOTTOM, player.getPistonPlacementMetadata(new Vector3i(0, 100, 0)));
    }

    @Test
    @DisplayName("Piston placement should keep horizontal facing when the player is level")
    void pistonPlacementKeepsHorizontalFacingWhenLevel() {
        Player player = new Player(0.5f, 100.0f, 0.5f);
        player.getCamera().setYaw(0.0f);

        assertEquals(Block.FACE_SOUTH, player.getPistonPlacementMetadata(new Vector3i(0, 100, 0)));
    }

    @Test
    @DisplayName("Vine placement should reject top and bottom faces")
    void vinePlacementRejectsVerticalFaces() throws Exception {
        World world = new World(6286L);
        try {
            world.setBlock(0, 100, 0, BlockType.STONE, 0);
            world.setBlock(1, 101, 0, BlockType.STONE, 0);
            Player player = new Player(5.0f, 100.0f, 5.0f);
            ItemStack vines = new ItemStack(ItemType.VINES, 1);
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 100, 0),
                    new Vector3i(0, 101, 0),
                    Block.FACE_TOP,
                    1.0f));

            assertFalse(tryPlaceHeldItem(player, world, BlockType.STONE, vines));

            assertSame(BlockType.AIR, world.getBlock(0, 101, 0));
            assertEquals(1, vines.getCount());
            assertFalse(player.isUsingItem());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Vine placement should allow horizontal supported faces")
    void vinePlacementAllowsHorizontalSupportedFaces() throws Exception {
        World world = new World(6287L);
        try {
            world.setBlock(1, 101, 0, BlockType.STONE, 0);
            Player player = new Player(5.0f, 100.0f, 5.0f);
            ItemStack vines = new ItemStack(ItemType.VINES, 1);
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(1, 101, 0),
                    new Vector3i(0, 101, 0),
                    Block.FACE_WEST,
                    1.0f));

            assertTrue(tryPlaceHeldItem(player, world, BlockType.STONE, vines));

            assertSame(BlockType.VINES, world.getBlock(0, 101, 0));
            assertEquals(8, world.getBlockMetadata(0, 101, 0));
            assertEquals(0, vines.getCount());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Torch placement should write Release 1.0 source metadata")
    void torchPlacementWritesReleaseOneSourceMetadata() throws Exception {
        World world = new World(6288L);
        try {
            world.setBlock(0, 101, 0, BlockType.STONE, 0);
            Player player = new Player(5.0f, 100.0f, 5.0f);
            ItemStack torch = new ItemStack(ItemType.TORCH, 1);
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 101, 0),
                    new Vector3i(1, 101, 0),
                    Block.FACE_EAST,
                    1.0f));

            assertTrue(tryPlaceHeldItem(player, world, BlockType.STONE, torch));

            assertSame(BlockType.TORCH, world.getBlock(1, 101, 0));
            assertEquals(1, world.getBlockMetadata(1, 101, 0));
            assertEquals(0, torch.getCount());
            assertTrue(player.isUsingItem());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Stone button placement should write Release 1.0 source metadata")
    void stoneButtonPlacementWritesReleaseOneSourceMetadata() throws Exception {
        World world = new World(6289L);
        try {
            world.setBlock(0, 101, 0, BlockType.STONE, 0);
            Player player = new Player(5.0f, 100.0f, 5.0f);
            ItemStack button = new ItemStack(ItemType.STONE_BUTTON, 1);
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 101, 0),
                    new Vector3i(1, 101, 0),
                    Block.FACE_EAST,
                    1.0f));

            assertTrue(tryPlaceHeldItem(player, world, BlockType.STONE, button));

            assertSame(BlockType.STONE_BUTTON, world.getBlock(1, 101, 0));
            assertEquals(1, world.getBlockMetadata(1, 101, 0));
            assertEquals(0, button.getCount());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ladder placement should write Release 1.0 source metadata")
    void ladderPlacementWritesReleaseOneSourceMetadata() throws Exception {
        World world = new World(6290L);
        try {
            world.setBlock(0, 101, 0, BlockType.STONE, 0);
            Player player = new Player(5.0f, 100.0f, 5.0f);
            ItemStack ladder = new ItemStack(ItemType.LADDER, 1);
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 101, 0),
                    new Vector3i(1, 101, 0),
                    Block.FACE_EAST,
                    1.0f));

            assertTrue(tryPlaceHeldItem(player, world, BlockType.STONE, ladder));

            assertSame(BlockType.LADDER, world.getBlock(1, 101, 0));
            assertEquals(5, world.getBlockMetadata(1, 101, 0));
            assertEquals(0, ladder.getCount());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Wall sign placement should write Release 1.0 source metadata")
    void wallSignPlacementWritesReleaseOneSourceMetadata() throws Exception {
        World world = new World(6291L);
        try {
            world.setBlock(0, 101, 0, BlockType.STONE, 0);
            Player player = new Player(5.0f, 100.0f, 5.0f);
            ItemStack sign = new ItemStack(ItemType.SIGN, 1);
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 101, 0),
                    new Vector3i(1, 101, 0),
                    Block.FACE_EAST,
                    1.0f));

            assertTrue(tryPlaceHeldItem(player, world, BlockType.STONE, sign));

            assertSame(BlockType.WALL_SIGN, world.getBlock(1, 101, 0));
            assertEquals(5, world.getBlockMetadata(1, 101, 0));
            assertEquals(0, sign.getCount());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Successful block placement should emit Release material sound feedback")
    void successfulBlockPlacementEmitsMaterialSoundFeedback() throws Exception {
        World world = new World(6301L);
        try {
            world.setBlock(0, 101, 0, BlockType.STONE, 0);
            Player player = new Player(5.0f, 100.0f, 5.0f);
            ItemStack planks = new ItemStack(ItemType.OAK_PLANKS, 2);
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 101, 0),
                    new Vector3i(1, 101, 0),
                    Block.FACE_EAST,
                    1.0f));

            assertTrue(tryPlaceHeldItem(player, world, BlockType.STONE, planks));

            assertSame(BlockType.OAK_PLANKS, world.getBlock(1, 101, 0));
            assertEquals(1, planks.getCount());
            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertEquals(WorldSoundEvent.DIG_WOOD, sounds.get(0).soundId());
            assertEquals(1.5f, sounds.get(0).x(), 0.0001f);
            assertEquals(101.5f, sounds.get(0).y(), 0.0001f);
            assertEquals(0.5f, sounds.get(0).z(), 0.0001f);
            assertEquals(0.8f, sounds.get(0).pitch(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Successful player block breaking should emit Release material sound feedback")
    void successfulPlayerBlockBreakingEmitsMaterialSoundFeedback() throws Exception {
        World world = new World(6302L);
        try {
            world.setBlock(0, 71, 0, BlockType.OAK_PLANKS, 0);
            Player player = new Player(0.5f, 70.0f, 3.5f);
            player.setGameMode(GameMode.CREATIVE);
            player.getCamera().setYaw(0.0f);
            player.getCamera().setPitch(0.0f);

            setAttackButtonPressed(true);
            player.handleBlockInteraction(world, 1.0f / 20.0f);

            assertSame(BlockType.AIR, world.getBlock(0, 71, 0));
            assertEquals(64, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.BLOCK_CRACK)
                    .count());
            WorldParticle particle = world.getParticles().stream()
                    .filter(candidate -> candidate.getType() == WorldParticle.Type.BLOCK_CRACK)
                    .findFirst()
                    .orElseThrow();
            assertSame(BlockType.OAK_PLANKS, particle.getBlockParticleType());
            assertEquals(Block.FACE_BOTTOM, particle.getBlockParticleFace());
            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertEquals(WorldSoundEvent.DIG_WOOD, sounds.get(0).soundId());
            assertEquals(0.5f, sounds.get(0).x(), 0.0001f);
            assertEquals(71.5f, sounds.get(0).y(), 0.0001f);
            assertEquals(0.5f, sounds.get(0).z(), 0.0001f);
            assertEquals(0.8f, sounds.get(0).pitch(), 0.0001f);
        } finally {
            setAttackButtonPressed(false);
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Initial survival block hits should emit textured crack particles before breaking")
    void survivalBlockHitEmitsTexturedCrackParticle() throws Exception {
        World world = new World(6303L);
        try {
            world.setBlock(0, 71, 0, BlockType.STONE, 0);
            Player player = new Player(0.5f, 70.0f, 3.5f);
            player.getCamera().setYaw(0.0f);
            player.getCamera().setPitch(0.0f);

            setAttackButtonPressed(true);
            player.handleBlockInteraction(world, 1.0f / 20.0f);

            assertSame(BlockType.STONE, world.getBlock(0, 71, 0));
            assertEquals(1, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.BLOCK_CRACK)
                    .count());
            WorldParticle particle = world.getParticles().get(0);
            assertSame(BlockType.STONE, particle.getBlockParticleType());
            assertEquals(0, particle.getBlockParticleMetadata());
            assertEquals(Block.FACE_BOTTOM, particle.getBlockParticleFace());
        } finally {
            setAttackButtonPressed(false);
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Sneaking with a placeable block should bypass clicked block interaction")
    void sneakingWithPlaceableBlockBypassesBlockUseBeforePlacement() throws Exception {
        Player player = new Player(5.0f, 100.0f, 5.0f);
        setTargetBlock(player, new Raycast.RaycastResult(true,
                new Vector3i(0, 100, 0),
                new Vector3i(1, 100, 0),
                Block.FACE_EAST,
                1.0f));
        setSneaking(player, true);

        assertFalse(shouldUseClickedBlockBeforePlacement(player, new ItemStack(ItemType.STONE, 1)));
    }

    @Test
    @DisplayName("Non-placeable or non-sneaking right-click should still use the clicked block first")
    void blockUseStillWinsWithoutSneakingPlaceableStack() throws Exception {
        Player player = new Player(5.0f, 100.0f, 5.0f);
        setTargetBlock(player, new Raycast.RaycastResult(true,
                new Vector3i(0, 100, 0),
                new Vector3i(1, 100, 0),
                Block.FACE_EAST,
                1.0f));

        assertTrue(shouldUseClickedBlockBeforePlacement(player, new ItemStack(ItemType.STONE, 1)));

        setSneaking(player, true);
        assertTrue(shouldUseClickedBlockBeforePlacement(player, null));
        assertTrue(shouldUseClickedBlockBeforePlacement(player, new ItemStack(ItemType.DIAMOND, 1)));
    }

    @Test
    @DisplayName("Right-clicking interactive blocks should start the hand-use animation")
    void rightClickingInteractiveBlockStartsUseAnimation() throws Exception {
        RecordingWorld world = new RecordingWorld(6292L);
        try {
            world.setBlock(0, 71, 0, BlockType.JUKEBOX);
            JukeboxTileEntity jukebox = (JukeboxTileEntity) world.getTileEntity(0, 71, 0);
            assertTrue(jukebox.insertRecord(world, new ItemStack(ItemType.RECORD_13, 1)));
            world.clearRebuilds();
            Player player = new Player(0.5f, 70.0f, 3.5f);
            player.getCamera().setYaw(0.0f);
            player.getCamera().setPitch(0.0f);

            setUseButtonPressed(true);
            player.handleBlockInteraction(world, 1.0f / 20.0f);

            assertFalse(jukebox.hasRecord());
            assertEquals(1, world.getDroppedItems().size());
            assertSame(ItemType.RECORD_13, world.getDroppedItems().get(0).getItemType());
            assertTrue(player.isUsingItem());
            assertEquals(1, world.rebuildCount);
            assertTrue(world.rebuilt(0, 71, 0));
        } finally {
            setUseButtonPressed(false);
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Right-clicking note blocks should tune through player interaction")
    void rightClickingNoteBlockTunesThroughPlayerInteraction() throws Exception {
        World world = new World(6300L);
        try {
            world.setBlock(0, 70, 0, BlockType.OAK_PLANKS);
            world.setBlock(0, 71, 0, BlockType.NOTE_BLOCK);
            NoteBlockTileEntity note = (NoteBlockTileEntity) world.getTileEntity(0, 71, 0);
            note.setPitch(5);
            Player player = new Player(0.5f, 70.0f, 3.5f);
            player.getCamera().setYaw(0.0f);
            player.getCamera().setPitch(0.0f);

            setUseButtonPressed(true);
            player.handleBlockInteraction(world, 1.0f / 20.0f);

            assertEquals(6, note.getPitch());
            assertEquals(1, note.getPlayTicks());
            assertTrue(player.isUsingItem());
            assertEquals(1, world.drainSoundEvents().size());
        } finally {
            setUseButtonPressed(false);
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Record insertion should rebuild jukebox metadata")
    void recordInsertionRebuildsJukeboxMetadata() throws Exception {
        RecordingWorld world = new RecordingWorld(6299L);
        try {
            world.setBlock(0, 71, 0, BlockType.JUKEBOX, 0);
            JukeboxTileEntity jukebox = (JukeboxTileEntity) world.getTileEntity(0, 71, 0);
            Player player = new Player(0.5f, 70.0f, 3.5f);
            ItemStack record = new ItemStack(ItemType.RECORD_CAT, 1);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] = record;
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 71, 0),
                    new Vector3i(0, 71, 1),
                    Block.FACE_SOUTH,
                    1.0f));

            assertTrue(handleTargetedItemUse(player, world, record, BlockType.JUKEBOX));

            assertTrue(jukebox.hasRecord());
            assertEquals(1, world.getBlockMetadata(0, 71, 0));
            assertNull(player.getInventory().getItemInHand());
            assertTrue(player.isUsingItem());
            assertEquals(1, world.rebuildCount);
            assertTrue(world.rebuilt(0, 71, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Record insertion should require an empty jukebox metadata state")
    void recordInsertionRequiresEmptyJukeboxMetadata() throws Exception {
        World world = new World(6293L);
        try {
            world.setBlock(0, 71, 0, BlockType.JUKEBOX, 1);
            JukeboxTileEntity jukebox = (JukeboxTileEntity) world.getTileEntity(0, 71, 0);
            Player player = new Player(0.5f, 70.0f, 3.5f);
            player.getCamera().setYaw(0.0f);
            player.getCamera().setPitch(0.0f);
            player.getInventory().getHotbar()[player.getInventory().getSelectedSlot()] =
                    new ItemStack(ItemType.RECORD_CAT, 1);

            setUseButtonPressed(true);
            player.handleBlockInteraction(world, 1.0f / 20.0f);

            assertFalse(jukebox.hasRecord());
            assertEquals(1, world.getBlockMetadata(0, 71, 0));
            assertEquals(1, player.getInventory().getItemInHand().getCount());
            assertSame(ItemType.RECORD_CAT, player.getInventory().getItemInHand().getType());
            assertTrue(player.isUsingItem());
            assertTrue(world.getDroppedItems().isEmpty());
        } finally {
            setUseButtonPressed(false);
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Right-clicking an upper wooden door should rebuild both door halves")
    void upperDoorToggleRebuildsBothDoorHalves() throws Exception {
        RecordingWorld world = new RecordingWorld(6298L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            assertTrue(world.placeDoor(0, 70, 0, BlockType.WOODEN_DOOR, 0, null));
            Player player = new Player(0.1f, 70.0f, 3.5f);
            player.getCamera().setYaw(0.0f);
            player.getCamera().setPitch(0.0f);

            setUseButtonPressed(true);
            player.handleBlockInteraction(world, 1.0f / 20.0f);

            assertEquals(4, world.getBlockMetadata(0, 70, 0) & 4);
            assertTrue(player.isUsingItem());
            assertEquals(2, world.rebuildCount);
            assertTrue(world.rebuilt(0, 70, 0));
            assertTrue(world.rebuilt(0, 71, 0));
        } finally {
            setUseButtonPressed(false);
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Seed planting should rebuild the planted crop mesh")
    void seedPlantingRebuildsPlantedCropMesh() throws Exception {
        assertSeedPlantingRebuilds(ItemType.SEEDS, BlockType.CROPS);
        assertSeedPlantingRebuilds(ItemType.PUMPKIN_SEEDS, BlockType.PUMPKIN_STEM);
        assertSeedPlantingRebuilds(ItemType.MELON_SEEDS, BlockType.MELON_STEM);
    }

    @Test
    @DisplayName("Hoeing dirt should rebuild the converted farmland mesh")
    void hoeingDirtRebuildsConvertedFarmlandMesh() throws Exception {
        RecordingWorld world = new RecordingWorld(6297L);
        try {
            world.setBlock(0, 70, 0, BlockType.DIRT, 0);
            Player player = new Player(0.5f, 70.0f, 3.5f);
            ItemStack hoe = new ItemStack(ItemType.WOODEN_HOE, 1);
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 70, 0),
                    new Vector3i(0, 71, 0),
                    Block.FACE_TOP,
                    1.0f));

            assertTrue(handleTargetedItemUse(player, world, hoe, BlockType.DIRT));

            assertSame(BlockType.FARMLAND, world.getBlock(0, 70, 0));
            assertTrue(player.isUsingItem());
            assertEquals(1, world.rebuildCount);
            assertEquals(0, world.lastRebuildX);
            assertEquals(70, world.lastRebuildY);
            assertEquals(0, world.lastRebuildZ);
        } finally {
            world.cleanup();
        }
    }

    private static void assertSeedPlantingRebuilds(ItemType seedType, BlockType plantedBlock) throws Exception {
        RecordingWorld world = new RecordingWorld(6294L + seedType.ordinal());
        try {
            world.setBlock(0, 70, 0, BlockType.FARMLAND, 7);
            Player player = new Player(0.5f, 70.0f, 3.5f);
            ItemStack seeds = new ItemStack(seedType, 2);
            setTargetBlock(player, new Raycast.RaycastResult(true,
                    new Vector3i(0, 70, 0),
                    new Vector3i(0, 71, 0),
                    Block.FACE_TOP,
                    1.0f));

            assertTrue(handleTargetedItemUse(player, world, seeds, BlockType.FARMLAND));

            assertSame(plantedBlock, world.getBlock(0, 71, 0));
            assertEquals(1, seeds.getCount());
            assertTrue(player.isUsingItem());
            assertEquals(1, world.rebuildCount);
            assertEquals(0, world.lastRebuildX);
            assertEquals(71, world.lastRebuildY);
            assertEquals(0, world.lastRebuildZ);
        } finally {
            world.cleanup();
        }
    }

    private static void setTargetBlock(Player player, Raycast.RaycastResult result) throws Exception {
        Field field = Player.class.getDeclaredField("targetBlock");
        field.setAccessible(true);
        field.set(player, result);
    }

    private static void setSneaking(Player player, boolean sneaking) throws Exception {
        Field field = Player.class.getDeclaredField("sneaking");
        field.setAccessible(true);
        field.set(player, sneaking);
    }

    private static boolean tryPlaceHeldItem(Player player, World world, BlockType clickedBlock,
            ItemStack stack) throws Exception {
        Method method = Player.class.getDeclaredMethod("tryPlaceHeldItem",
                World.class, BlockType.class, ItemStack.class);
        method.setAccessible(true);
        return (boolean) method.invoke(player, world, clickedBlock, stack);
    }

    private static boolean shouldUseClickedBlockBeforePlacement(Player player, ItemStack stack) throws Exception {
        Method method = Player.class.getDeclaredMethod("shouldUseClickedBlockBeforePlacement", ItemStack.class);
        method.setAccessible(true);
        return (boolean) method.invoke(player, stack);
    }

    private static boolean handleTargetedItemUse(Player player, World world, ItemStack stack,
            BlockType clickedBlock) throws Exception {
        Method method = Player.class.getDeclaredMethod("handleTargetedItemUse",
                World.class, ItemStack.class, BlockType.class);
        method.setAccessible(true);
        return (boolean) method.invoke(player, world, stack, clickedBlock);
    }

    private static void setUseButtonPressed(boolean pressed) throws Exception {
        int button = 1;
        setInputButtonArray("buttons", button, pressed);
        setInputButtonArray("buttonsPressed", button, pressed);
        setInputButtonArray("buttonsReleased", button, false);
    }

    private static void setAttackButtonPressed(boolean pressed) throws Exception {
        int button = 0;
        setInputButtonArray("buttons", button, pressed);
        setInputButtonArray("buttonsPressed", button, pressed);
        setInputButtonArray("buttonsReleased", button, false);
    }

    private static void setInputButtonArray(String fieldName, int button, boolean value) throws Exception {
        Field field = Input.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        boolean[] values = (boolean[]) field.get(null);
        values[button] = value;
    }

    private static final class RecordingWorld extends World {
        private int rebuildCount;
        private int lastRebuildX;
        private int lastRebuildY;
        private int lastRebuildZ;
        private final Set<String> rebuilds = new HashSet<>();

        private RecordingWorld(long seed) {
            super(seed);
        }

        @Override
        public void rebuildBlockMeshesNow(int x, int y, int z) {
            rebuildCount++;
            lastRebuildX = x;
            lastRebuildY = y;
            lastRebuildZ = z;
            rebuilds.add(key(x, y, z));
        }

        private boolean rebuilt(int x, int y, int z) {
            return rebuilds.contains(key(x, y, z));
        }

        private void clearRebuilds() {
            rebuildCount = 0;
            lastRebuildX = 0;
            lastRebuildY = 0;
            lastRebuildZ = 0;
            rebuilds.clear();
        }

        private static String key(int x, int y, int z) {
            return x + "," + y + "," + z;
        }
    }
}
