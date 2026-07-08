package com.craftzero.world;

import com.craftzero.entity.DroppedItem;
import com.craftzero.entity.PrimedTntEntity;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionType;
import com.craftzero.world.tile.DispenserTileEntity;
import com.craftzero.world.tile.JukeboxTileEntity;
import com.craftzero.world.tile.NoteBlockTileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldSoundEventTest {
    @Test
    @DisplayName("World sound events should drain as transient runtime feedback")
    void soundEventsDrainAsTransientFeedback() {
        World world = new World(6280L);
        try {
            world.playSound(WorldSoundEvent.DOOR_OPEN, 1.0f, 2.0f, 3.0f, 0.8f, 1.0f);

            assertEquals(1, world.getSoundEvents().size());
            List<WorldSoundEvent> drained = world.drainSoundEvents();

            assertEquals(1, drained.size());
            assertEquals(WorldSoundEvent.DOOR_OPEN, drained.get(0).soundId());
            assertTrue(world.getSoundEvents().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Block place and break helpers should emit Release material sound groups")
    void blockPlaceAndBreakHelpersEmitMaterialSoundGroups() {
        World world = new World(6294L);
        try {
            world.playBlockPlaceSound(BlockType.OAK_PLANKS, 1, 70, 2);
            WorldSoundEvent wood = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.DIG_WOOD, wood.soundId());
            assertEquals(1.5f, wood.x(), 0.0001f);
            assertEquals(70.5f, wood.y(), 0.0001f);
            assertEquals(2.5f, wood.z(), 0.0001f);
            assertEquals(1.0f, wood.volume(), 0.0001f);
            assertEquals(0.8f, wood.pitch(), 0.0001f);

            world.playBlockBreakSound(BlockType.GLASS, 2, 70, 2);
            WorldSoundEvent glass = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.GLASS_BREAK, glass.soundId());
            assertEquals(0.8f, glass.pitch(), 0.0001f);

            world.playBlockPlaceSound(BlockType.IRON_DOOR, 3, 70, 2);
            WorldSoundEvent metal = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.DIG_METAL, metal.soundId());
            assertEquals(1.2f, metal.pitch(), 0.0001f);

            world.playBlockBreakSound(BlockType.WATER, 4, 70, 2);
            assertTrue(world.drainSoundEvents().isEmpty());

            world.playBlockStepSound(BlockType.GRASS, 5.25f, 70.0f, 2.75f);
            WorldSoundEvent step = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.STEP_GRASS, step.soundId());
            assertEquals(5.25f, step.x(), 0.0001f);
            assertEquals(70.0f, step.y(), 0.0001f);
            assertEquals(2.75f, step.z(), 0.0001f);
            assertEquals(0.15f, step.volume(), 0.0001f);
            assertEquals(1.0f, step.pitch(), 0.0001f);

            world.playBlockStepSound(BlockType.IRON_BLOCK, 6.0f, 70.0f, 2.0f);
            WorldSoundEvent metalStep = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.STEP_STONE, metalStep.soundId());
            assertEquals(1.5f, metalStep.pitch(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Block sound ids should follow old StepSound material groups")
    void blockSoundIdsFollowOldStepSoundMaterialGroups() {
        assertEquals(WorldSoundEvent.DIG_GRASS, WorldSoundEvent.blockBreakSoundId(BlockType.GRASS));
        assertEquals(WorldSoundEvent.DIG_GRAVEL, WorldSoundEvent.blockBreakSoundId(BlockType.DIRT));
        assertEquals(WorldSoundEvent.DIG_SAND, WorldSoundEvent.blockBreakSoundId(BlockType.SAND));
        assertEquals(WorldSoundEvent.DIG_CLOTH, WorldSoundEvent.blockBreakSoundId(BlockType.WHITE_WOOL));
        assertEquals(WorldSoundEvent.DIG_SNOW, WorldSoundEvent.blockBreakSoundId(BlockType.SNOW_LAYER));
        assertEquals(WorldSoundEvent.DIG_LADDER, WorldSoundEvent.blockBreakSoundId(BlockType.LADDER));
        assertEquals(WorldSoundEvent.DIG_STONE, WorldSoundEvent.blockBreakSoundId(BlockType.STONE));
        assertEquals(WorldSoundEvent.GLASS_BREAK, WorldSoundEvent.blockBreakSoundId(BlockType.GLASS_PANE));
        assertEquals(null, WorldSoundEvent.blockBreakSoundId(BlockType.AIR));
        assertEquals(WorldSoundEvent.STEP_GRASS, WorldSoundEvent.blockStepSoundId(BlockType.GRASS));
        assertEquals(WorldSoundEvent.STEP_GRAVEL, WorldSoundEvent.blockStepSoundId(BlockType.DIRT));
        assertEquals(WorldSoundEvent.STEP_SAND, WorldSoundEvent.blockStepSoundId(BlockType.SAND));
        assertEquals(WorldSoundEvent.STEP_CLOTH, WorldSoundEvent.blockStepSoundId(BlockType.WHITE_WOOL));
        assertEquals(WorldSoundEvent.STEP_SNOW, WorldSoundEvent.blockStepSoundId(BlockType.SNOW_LAYER));
        assertEquals(WorldSoundEvent.STEP_LADDER, WorldSoundEvent.blockStepSoundId(BlockType.LADDER));
        assertEquals(WorldSoundEvent.STEP_STONE, WorldSoundEvent.blockStepSoundId(BlockType.GLASS_PANE));
        assertEquals(null, WorldSoundEvent.blockStepSoundId(BlockType.WATER));
        assertEquals(0.8f, WorldSoundEvent.blockPlacePitch(BlockType.STONE), 0.0001f);
        assertEquals(1.2f, WorldSoundEvent.blockPlacePitch(BlockType.IRON_BLOCK), 0.0001f);
        assertEquals(0.15f, WorldSoundEvent.blockStepVolume(BlockType.STONE), 0.0001f);
        assertEquals(1.5f, WorldSoundEvent.blockStepPitch(BlockType.IRON_BLOCK), 0.0001f);
    }

    @Test
    @DisplayName("Note blocks and jukebox records should emit Release-style sound ids")
    void noteBlocksAndJukeboxRecordsEmitSoundIds() {
        World world = new World(6281L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE);
            world.setBlock(0, 70, 0, BlockType.NOTE_BLOCK);
            NoteBlockTileEntity note = (NoteBlockTileEntity) world.getTileEntity(0, 70, 0);
            note.setPitch(12);

            assertTrue(note.play(world));
            WorldSoundEvent noteSound = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.NOTE_BASS_DRUM, noteSound.soundId());
            assertEquals(1.0f, noteSound.pitch(), 0.0001f);

            world.setBlock(2, 70, 0, BlockType.JUKEBOX);
            JukeboxTileEntity jukebox = (JukeboxTileEntity) world.getTileEntity(2, 70, 0);
            assertTrue(jukebox.insertRecord(new ItemStack(ItemType.RECORD_CAT, 1)));
            jukebox.play(world);

            WorldSoundEvent recordSound = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.RECORD_CAT, recordSound.soundId());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Every Java 1.0 music disc should resolve to its record sound id")
    void releaseOneRecordsResolveToSoundIds() {
        assertEquals(WorldSoundEvent.RECORD_13, WorldSoundEvent.recordSoundId(ItemType.RECORD_13));
        assertEquals(WorldSoundEvent.RECORD_CAT, WorldSoundEvent.recordSoundId(ItemType.RECORD_CAT));
        assertEquals(WorldSoundEvent.RECORD_BLOCKS, WorldSoundEvent.recordSoundId(ItemType.RECORD_BLOCKS));
        assertEquals(WorldSoundEvent.RECORD_CHIRP, WorldSoundEvent.recordSoundId(ItemType.RECORD_CHIRP));
        assertEquals(WorldSoundEvent.RECORD_FAR, WorldSoundEvent.recordSoundId(ItemType.RECORD_FAR));
        assertEquals(WorldSoundEvent.RECORD_MALL, WorldSoundEvent.recordSoundId(ItemType.RECORD_MALL));
        assertEquals(WorldSoundEvent.RECORD_MELLOHI, WorldSoundEvent.recordSoundId(ItemType.RECORD_MELLOHI));
        assertEquals(WorldSoundEvent.RECORD_STAL, WorldSoundEvent.recordSoundId(ItemType.RECORD_STAL));
        assertEquals(WorldSoundEvent.RECORD_STRAD, WorldSoundEvent.recordSoundId(ItemType.RECORD_STRAD));
        assertEquals(WorldSoundEvent.RECORD_WARD, WorldSoundEvent.recordSoundId(ItemType.RECORD_WARD));
        assertEquals(WorldSoundEvent.RECORD_11, WorldSoundEvent.recordSoundId(ItemType.RECORD_11));
        assertEquals(null, WorldSoundEvent.recordSoundId(ItemType.DIAMOND));
    }

    @Test
    @DisplayName("UI button clicks should use the Release-era random.click cue")
    void uiButtonClicksUseReleaseClickCue() {
        WorldSoundEvent click = WorldSoundEvent.uiButtonClick();

        assertEquals("random.click", WorldSoundEvent.UI_BUTTON_CLICK);
        assertEquals(WorldSoundEvent.UI_BUTTON_CLICK, click.soundId());
        assertEquals(1.0f, click.volume(), 0.0001f);
        assertEquals(1.0f, click.pitch(), 0.0001f);
    }

    @Test
    @DisplayName("Manual and redstone openable changes should emit door sounds")
    void openableChangesEmitDoorSounds() {
        World world = new World(6282L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE);
            assertTrue(world.placeDoor(0, 70, 0, BlockType.WOODEN_DOOR, 0, null));

            assertTrue(world.toggleBlock(0, 70, 0));
            assertOpenableSound(world.drainSoundEvents().get(0), WorldSoundEvent.DOOR_OPEN);

            assertTrue(world.toggleBlock(0, 70, 0));
            assertOpenableSound(world.drainSoundEvents().get(0), WorldSoundEvent.DOOR_CLOSE);

            world.setBlock(2, 70, 0, BlockType.TRAPDOOR, 3);
            assertTrue(world.toggleBlock(2, 70, 0));
            assertOpenableSound(world.drainSoundEvents().get(0), WorldSoundEvent.DOOR_OPEN);

            world.setBlock(4, 70, 0, BlockType.FENCE_GATE, 1);
            assertTrue(world.toggleBlock(4, 70, 0));
            assertOpenableSound(world.drainSoundEvents().get(0), WorldSoundEvent.DOOR_OPEN);

            world.setBlock(8, 69, 8, BlockType.STONE);
            assertTrue(world.placeDoor(8, 70, 8, BlockType.IRON_DOOR, 0, null));
            world.setBlock(9, 69, 8, BlockType.STONE);
            world.setBlock(9, 70, 8, BlockType.LEVER,
                    BlockShape.leverMetadataFromFace(Block.FACE_TOP) | RedstoneEngine.POWERED_BIT);
            world.advanceBlockTicks(2);

            assertOpenableSound(world.drainSoundEvents().get(0), WorldSoundEvent.DOOR_OPEN);
            assertSame(BlockType.IRON_DOOR, world.getBlock(8, 70, 8));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Jukebox ejection should emit a transient pop sound")
    void jukeboxEjectionEmitsPopSound() {
        World world = new World(6283L);
        try {
            world.setBlock(0, 70, 0, BlockType.JUKEBOX);
            JukeboxTileEntity jukebox = (JukeboxTileEntity) world.getTileEntity(0, 70, 0);
            assertTrue(jukebox.insertRecord(world, new ItemStack(ItemType.RECORD_13, 1)));
            assertEquals(1, world.getBlockMetadata(0, 70, 0));

            assertTrue(world.toggleBlock(0, 70, 0));

            assertFalse(jukebox.hasRecord());
            assertEquals(0, world.getBlockMetadata(0, 70, 0));
            assertEquals(WorldSoundEvent.RECORD_EJECT, world.drainSoundEvents().get(0).soundId());

            assertEquals(1, world.getDroppedItems().size());
            DroppedItem dropped = world.getDroppedItems().get(0);
            assertSame(ItemType.RECORD_13, dropped.getItemType());
            assertTrue(dropped.getX() >= 0.15f && dropped.getX() <= 0.85f);
            assertTrue(dropped.getY() >= 70.66f && dropped.getY() <= 71.36f);
            assertTrue(dropped.getZ() >= 0.15f && dropped.getZ() <= 0.85f);
            assertEquals(0.2f, dropped.getVelocityY(), 0.0001f);
            assertTrue(dropped.getAge() < 0.5f);
            assertEquals(DroppedItem.DEFAULT_PICKUP_DELAY_TICKS, dropped.getPickupDelayTicks());
            assertFalse(dropped.canPickup());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Piston extension and retraction should emit Release-style sound ids")
    void pistonMovementEmitsSoundIds() {
        World world = new World(6284L);
        try {
            world.setBlock(0, 100, 0, BlockType.PISTON, Block.FACE_EAST);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(1);

            List<WorldSoundEvent> extensionSounds = world.drainSoundEvents();
            assertEquals(1, extensionSounds.size());
            assertEquals(WorldSoundEvent.PISTON_EXTEND, extensionSounds.get(0).soundId());
            assertEquals(0.5f, extensionSounds.get(0).volume(), 0.0001f);
            assertTrue(extensionSounds.get(0).pitch() >= 0.6f);
            assertTrue(extensionSounds.get(0).pitch() < 0.85f);

            world.advanceBlockTicks(RedstoneEngine.PISTON_MOVEMENT_TICKS);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5);
            world.advanceBlockTicks(1);

            List<WorldSoundEvent> retractionSounds = world.drainSoundEvents();
            assertEquals(1, retractionSounds.size());
            assertEquals(WorldSoundEvent.PISTON_RETRACT, retractionSounds.get(0).soundId());
            assertEquals(0.5f, retractionSounds.get(0).volume(), 0.0001f);
            assertTrue(retractionSounds.get(0).pitch() >= 0.6f);
            assertTrue(retractionSounds.get(0).pitch() < 0.75f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mechanism sounds should use the old randomized pitch formulas")
    void pistonSoundPitchesUseReleaseFormulas() {
        assertEquals(0.95f, WorldSoundEvent.openablePitch(new FixedFloatRandom(0.5f)), 0.0001f);
        assertEquals(0.725f, WorldSoundEvent.pistonExtendPitch(new FixedFloatRandom(0.5f)), 0.0001f);
        assertEquals(0.675f, WorldSoundEvent.pistonRetractPitch(new FixedFloatRandom(0.5f)), 0.0001f);
        assertEquals(1.04f, WorldSoundEvent.chickenPlopPitch(new FixedFloatRandom(0.7f, 0.5f)), 0.0001f);
        assertEquals(1.0f, WorldSoundEvent.portalAmbientPitch(new FixedFloatRandom(0.5f)), 0.0001f);
    }

    @Test
    @DisplayName("Dispenser activation should emit the old click cue even when empty")
    void emptyDispenserActivationEmitsClickSound() {
        World world = new World(6289L);
        try {
            world.setBlock(0, 70, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 70, 0);

            dispenser.dispense(world);

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertDispenserSound(sounds.get(0), WorldSoundEvent.DISPENSER_CLICK, 1.2f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Successful generic dispenser ejection should emit the old click effect")
    void genericDispenserActivationEmitsSuccessfulClickEffect() {
        World world = new World(6291L);
        try {
            world.setBlock(0, 70, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 70, 0);
            dispenser.getInventory()[0] = new ItemStack(ItemType.DIRT, 1);

            dispenser.dispense(world);

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertDispenserSound(sounds.get(0), WorldSoundEvent.DISPENSER_CLICK, 1.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Projectile dispenser launches should emit the old bow effect")
    void projectileDispenserActivationEmitsBowEffect() {
        World world = new World(6292L);
        try {
            world.setBlock(0, 70, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 70, 0);
            dispenser.getInventory()[0] = new ItemStack(ItemType.ARROW, 1);

            dispenser.dispense(world);

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertDispenserSound(sounds.get(0), WorldSoundEvent.BOW, 1.2f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Splash potion dispenser launches should emit the old bow effect")
    void splashPotionDispenserActivationEmitsBowEffect() {
        World world = new World(6293L);
        try {
            world.setBlock(0, 70, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 70, 0);
            ItemStack potion = new ItemStack(ItemType.POTION, 1);
            potion.setPotionData(new PotionData(PotionType.POISON, true, false, false));
            dispenser.getInventory()[0] = potion;

            dispenser.dispense(world);

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertDispenserSound(sounds.get(0), WorldSoundEvent.BOW, 1.2f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Redstone controls should emit Release-style click sounds on state edges")
    void redstoneControlsEmitClickSounds() {
        World world = new World(6285L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.LEVER, BlockShape.leverMetadataFromFace(Block.FACE_TOP));

            assertTrue(world.toggleBlock(0, 70, 0));
            assertRedstoneClick(world.drainSoundEvents().get(0), true);

            assertTrue(world.toggleBlock(0, 70, 0));
            assertRedstoneClick(world.drainSoundEvents().get(0), false);

            world.setBlock(2, 70, 0, BlockType.STONE_BUTTON, Block.FACE_WEST);
            assertTrue(world.toggleBlock(2, 70, 0));
            assertRedstoneClick(world.drainSoundEvents().get(0), true);

            world.advanceBlockTicks(RedstoneEngine.BUTTON_DELAY_TICKS + 1);
            assertRedstoneClick(world.drainSoundEvents().get(0), false);

            world.setBlock(4, 69, 0, BlockType.STONE, 0);
            world.setBlock(4, 70, 0, BlockType.STONE_PRESSURE_PLATE, 0);
            world.advanceBlockTicks(2);

            Player player = new Player(4.5f, 70.0f, 0.5f);
            world.setPlayer(player);
            player.update(1.0f / 20.0f, world);
            world.advanceBlockTicks(1);
            WorldSoundEvent platePress = world.drainSoundEvents().get(0);
            assertRedstoneClick(platePress, true);
            assertEquals(4.5f, platePress.x(), 0.0001f);
            assertEquals(70.1f, platePress.y(), 0.0001f);
            assertEquals(0.5f, platePress.z(), 0.0001f);

            player.setPosition(6.5f, 70.0f, 0.5f);
            world.advanceBlockTicks(RedstoneEngine.PRESSURE_PLATE_DELAY_TICKS + 1);
            WorldSoundEvent plateRelease = world.drainSoundEvents().get(0);
            assertRedstoneClick(plateRelease, false);
            assertEquals(4.5f, plateRelease.x(), 0.0001f);
            assertEquals(70.1f, plateRelease.y(), 0.0001f);
            assertEquals(0.5f, plateRelease.z(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("TNT priming should emit the Release-style fuse sound")
    void tntPrimingEmitsFuseSound() {
        World world = new World(6286L);
        try {
            world.setBlock(0, 70, 0, BlockType.TNT, 0);

            assertNotNull(world.primeTnt(0, 70, 0, RedstoneEngine.TNT_FUSE_TICKS));

            WorldSoundEvent fuseSound = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.TNT_FUSE, fuseSound.soundId());
            assertEquals(1.0f, fuseSound.volume(), 0.0001f);
            assertEquals(1.0f, fuseSound.pitch(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Primed TNT should use the old circular launch impulse and smoke ticks")
    void primedTntUsesLaunchImpulseAndSmokeTicks() {
        World world = new World(6289L);
        try {
            world.setBlock(0, 70, 0, BlockType.TNT, 0);

            PrimedTntEntity tnt = world.primeTnt(0, 70, 0, RedstoneEngine.TNT_FUSE_TICKS);

            assertNotNull(tnt);
            float horizontalSpeed = (float) Math.sqrt(
                    tnt.getMotionX() * tnt.getMotionX() + tnt.getMotionZ() * tnt.getMotionZ());
            assertEquals(0.02f, horizontalSpeed, 0.0001f);
            assertEquals(0.2f, tnt.getMotionY(), 0.0001f);

            world.updateEntities(1.0f / 20.0f);

            assertTrue(world.getParticles().stream()
                    .anyMatch(particle -> particle.getType() == WorldParticle.Type.SMOKE));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Explosions should emit Release-style sound, burst, flash, and smoke debris")
    void explosionsEmitSoundAndLargeParticle() {
        World world = new World(6287L);
        try {
            world.setBlock(1, 100, 0, BlockType.STONE, 0);
            world.setBlock(0, 100, 1, BlockType.DIRT, 0);

            world.explode(0.5f, 100.0f, 0.5f, 4.0f);

            WorldSoundEvent explosionSound = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.EXPLOSION, explosionSound.soundId());
            assertEquals(4.0f, explosionSound.volume(), 0.0001f);
            assertTrue(explosionSound.pitch() >= 0.56f);
            assertTrue(explosionSound.pitch() <= 0.84f);

            WorldParticle explosionParticle = world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.HUGE_EXPLOSION)
                    .findFirst()
                    .orElseThrow();
            assertEquals(0.5f, explosionParticle.getRenderX(0.0f), 0.0001f);
            assertEquals(100.0f, explosionParticle.getRenderY(0.0f), 0.0001f);
            assertEquals(0.5f, explosionParticle.getRenderZ(0.0f), 0.0001f);
            assertEquals(2.0f, explosionParticle.getScale(0.0f), 0.0001f);
            assertEquals(16.0f, explosionParticle.getLifetimeTicks(), 0.0001f);

            List<WorldParticle> particles = world.getParticles();
            int flashIndex = -1;
            for (int i = 0; i < particles.size(); i++) {
                if (particles.get(i).getType() == WorldParticle.Type.EXPLODE) {
                    flashIndex = i;
                    break;
                }
            }
            assertTrue(flashIndex > 0);
            assertTrue(flashIndex + 1 < particles.size());
            WorldParticle flash = particles.get(flashIndex);
            WorldParticle smoke = particles.get(flashIndex + 1);
            assertSame(WorldParticle.Type.SMOKE, smoke.getType());
            assertEquals((smoke.getRenderX(0.0f) + 0.5f) * 0.5f, flash.getRenderX(0.0f), 0.0001f);
            assertEquals((smoke.getRenderY(0.0f) + 100.0f) * 0.5f, flash.getRenderY(0.0f), 0.0001f);
            assertEquals((smoke.getRenderZ(0.0f) + 0.5f) * 0.5f, flash.getRenderZ(0.0f), 0.0001f);
            assertTrue(flash.getLifetimeTicks() >= 14.0f);
            assertTrue(flash.getLifetimeTicks() <= 21.0f);
            assertTrue(smoke.getLifetimeTicks() >= 18.0f);
            assertTrue(smoke.getLifetimeTicks() <= 27.0f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Small explosions should use the smaller largeexplode center particle")
    void smallExplosionsUseLargeExplosionCenterParticle() {
        World world = new World(62871L);
        try {
            world.explode(0.5f, 100.0f, 0.5f, 1.5f);

            assertTrue(world.getParticles().stream()
                    .anyMatch(particle -> particle.getType() == WorldParticle.Type.LARGE_EXPLOSION));
            assertFalse(world.getParticles().stream()
                    .anyMatch(particle -> particle.getType() == WorldParticle.Type.HUGE_EXPLOSION));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Explosion-primed TNT should not emit the manual fuse sound")
    void explosionPrimedTntDoesNotEmitFuseSound() {
        World world = new World(6290L);
        try {
            world.setBlock(1, 70, 0, BlockType.TNT, 0);

            world.explode(0.5f, 70.0f, 0.5f, 4.0f);

            List<WorldSoundEvent> sounds = world.drainSoundEvents();
            assertTrue(sounds.stream().anyMatch(sound -> WorldSoundEvent.EXPLOSION.equals(sound.soundId())));
            assertFalse(sounds.stream().anyMatch(sound -> WorldSoundEvent.TNT_FUSE.equals(sound.soundId())));
            assertTrue(world.hasEntityOfType(PrimedTntEntity.class));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dropped item pickup should emit the Release-style pop sound")
    void droppedItemPickupEmitsSound() {
        World world = new World(6288L);
        try {
            Player player = new Player(0.0f, 70.0f, 0.0f);
            DroppedItem item = new DroppedItem(0.0f, 70.9f, 0.0f, new ItemStack(ItemType.DIRT, 1));
            item.update(0.6f, world);
            world.replaceDroppedItems(List.of(item));

            List<DroppedItem> collected = world.collectNearbyItems(
                    item.getX(), item.getY(), item.getZ(), 1.0f / 20.0f, player);

            assertEquals(1, collected.size());
            WorldSoundEvent pickupSound = world.drainSoundEvents().get(0);
            assertEquals(WorldSoundEvent.ITEM_PICKUP, pickupSound.soundId());
            assertEquals(0.2f, pickupSound.volume(), 0.0001f);
            assertTrue(pickupSound.pitch() >= 0.6f);
            assertTrue(pickupSound.pitch() <= 3.4f);
        } finally {
            world.cleanup();
        }
    }

    private static void assertRedstoneClick(WorldSoundEvent sound, boolean powered) {
        assertEquals(WorldSoundEvent.REDSTONE_CLICK, sound.soundId());
        assertEquals(0.3f, sound.volume(), 0.0001f);
        assertEquals(WorldSoundEvent.redstoneClickPitch(powered), sound.pitch(), 0.0001f);
    }

    private static void assertOpenableSound(WorldSoundEvent sound, String soundId) {
        assertEquals(soundId, sound.soundId());
        assertEquals(1.0f, sound.volume(), 0.0001f);
        assertTrue(sound.pitch() >= 0.9f);
        assertTrue(sound.pitch() < 1.0f);
    }

    private static void assertDispenserSound(WorldSoundEvent sound, String soundId, float pitch) {
        assertEquals(soundId, sound.soundId());
        assertEquals(1.0f, sound.volume(), 0.0001f);
        assertEquals(pitch, sound.pitch(), 0.0001f);
    }

    private static final class FixedFloatRandom extends Random {
        private final float[] values;
        private int index;

        private FixedFloatRandom(float... values) {
            this.values = values;
        }

        @Override
        public float nextFloat() {
            float value = values[Math.min(index, values.length - 1)];
            index++;
            return value;
        }
    }
}
