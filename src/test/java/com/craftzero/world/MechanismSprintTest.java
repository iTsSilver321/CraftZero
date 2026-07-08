package com.craftzero.world;

import com.craftzero.combat.DamageSource;
import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.BoatEntity;
import com.craftzero.entity.DroppedItem;
import com.craftzero.entity.EndCrystalEntity;
import com.craftzero.entity.MinecartEntity;
import com.craftzero.entity.PrimedTntEntity;
import com.craftzero.entity.SplashPotionEntity;
import com.craftzero.entity.ThrownItemEntity;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.CombatRules;
import com.craftzero.main.Player;
import com.craftzero.main.PlayerStats;
import com.craftzero.physics.AABB;
import com.craftzero.progression.PotionData;
import com.craftzero.progression.PotionType;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.world.tile.DispenserTileEntity;
import com.craftzero.world.tile.JukeboxTileEntity;
import com.craftzero.world.tile.NoteBlockTileEntity;
import com.craftzero.world.tile.BlockPos;
import com.craftzero.world.tile.SignTileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MechanismSprintTest {
    @Test
    @DisplayName("Redstone wire carries power into TNT without generating extra chunks")
    void redstoneWirePowersTnt() {
        World world = new World(5100L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(1, 99, 0, BlockType.STONE);
            world.setBlock(0, 100, 0, BlockType.REDSTONE_TORCH_ON, 5);
            world.setBlock(1, 100, 0, BlockType.REDSTONE_WIRE, 0);
            world.setBlock(1, 101, 0, BlockType.TNT, 0);

            world.advanceBlockTicks(8);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(world.getBlockMetadataIfLoaded(1, 100, 0, 0) > 0);
            assertSame(BlockType.AIR, world.getBlockIfLoaded(1, 101, 0, BlockType.BEDROCK));
            assertTrue(world.getEntities().stream().anyMatch(entity -> entity instanceof PrimedTntEntity));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Redstone dust power changes cascade through connected wire in one update tick")
    void redstoneWirePowerChangesCascadeThroughLine() {
        World world = new World(5151L);
        try {
            for (int x = 0; x <= 15; x++) {
                world.setBlock(x, 69, 0, BlockType.STONE, 0);
                world.setBlock(x, 70, 0, BlockType.REDSTONE_WIRE, 0);
            }
            world.setBlock(-1, 69, 0, BlockType.STONE, 0);
            world.setBlock(-1, 70, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(1);

            for (int x = 0; x <= 15; x++) {
                assertEquals(Math.max(15 - x, 0), world.getBlockMetadata(x, 70, 0) & 15,
                        "powered wire metadata at x=" + x);
            }

            world.setBlock(-1, 70, 0, BlockType.LEVER, 5);
            world.advanceBlockTicks(1);

            for (int x = 0; x <= 15; x++) {
                assertEquals(0, world.getBlockMetadata(x, 70, 0) & 15,
                        "cleared wire metadata at x=" + x);
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Weak-powered opaque blocks activate mechanisms without feeding dust")
    void weakPoweredOpaqueBlockActivatesMechanismsWithoutPoweringDust() {
        World world = new World(5152L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.REDSTONE_WIRE, 15);
            world.setBlock(1, 70, 0, BlockType.STONE, 0);
            world.setBlock(2, 70, 0, BlockType.TNT, 0);
            world.setBlock(1, 69, 1, BlockType.STONE, 0);
            world.setBlock(1, 70, 1, BlockType.REDSTONE_WIRE, 0);

            assertTrue(RedstoneEngine.isBlockPowered(world, 2, 70, 0));

            world.scheduleBlockTick(2, 70, 0, BlockType.TNT, 0);
            world.scheduleBlockTick(1, 70, 1, BlockType.REDSTONE_WIRE, 0);
            world.advanceBlockTicks(1);
            world.updateEntities(1.0f / 20.0f);

            assertSame(BlockType.AIR, world.getBlock(2, 70, 0));
            assertTrue(world.getEntities().stream().anyMatch(entity -> entity instanceof PrimedTntEntity));
            assertEquals(0, world.getBlockMetadata(1, 70, 1) & 15);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Repeaters accept weak-powered opaque blocks as Release-era input")
    void repeaterAcceptsWeakPoweredOpaqueInputBlock() {
        World world = new World(5155L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(2, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.REDSTONE_WIRE, 15);
            world.setBlock(1, 70, 0, BlockType.STONE, 0);
            world.setBlock(2, 70, 0, BlockType.REDSTONE_REPEATER_OFF, 3);

            world.scheduleBlockTick(2, 70, 0, BlockType.REDSTONE_REPEATER_OFF, 0);
            world.advanceBlockTicks(1);

            assertSame(BlockType.REDSTONE_REPEATER_ON, world.getBlock(2, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Repeaters accept powered rear dust even when that dust branches sideways")
    void repeaterAcceptsPoweredRearDustWithSideBranch() {
        World world = new World(5156L);
        try {
            world.setBlock(1, 69, 0, BlockType.STONE, 0);
            world.setBlock(2, 69, 0, BlockType.STONE, 0);
            world.setBlock(1, 69, 1, BlockType.STONE, 0);
            world.setBlock(1, 70, 0, BlockType.REDSTONE_WIRE, 15);
            world.setBlock(1, 70, 1, BlockType.REDSTONE_WIRE, 1);
            world.setBlock(2, 70, 0, BlockType.REDSTONE_REPEATER_OFF, 3);

            assertEquals(0, RedstoneEngine.getWeakPower(world, 1, 70, 0, Block.FACE_EAST),
                    "Branched dust should still keep ordinary weak-power side gating");

            world.scheduleBlockTick(2, 70, 0, BlockType.REDSTONE_REPEATER_OFF, 0);
            world.advanceBlockTicks(1);

            assertSame(BlockType.REDSTONE_REPEATER_ON, world.getBlock(2, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Redstone dust weak-powers upward and horizontal sides, but not downward")
    void redstoneWireDoesNotWeakPowerDownward() {
        World world = new World(5136L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            world.setBlock(0, 71, 0, BlockType.REDSTONE_WIRE, 15);

            assertEquals(15, RedstoneEngine.getWeakPower(world, 0, 71, 0, Block.FACE_TOP));
            assertEquals(15, RedstoneEngine.getWeakPower(world, 0, 71, 0, Block.FACE_NORTH));
            assertEquals(0, RedstoneEngine.getWeakPower(world, 0, 71, 0, Block.FACE_BOTTOM));
            assertFalse(RedstoneEngine.isBlockPowered(world, 0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Connected redstone dust weak-powers only along its straight line")
    void connectedRedstoneWireUsesDirectionalWeakPower() {
        World world = new World(5137L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            world.setBlock(1, 70, 0, BlockType.STONE, 0);
            world.setBlock(0, 71, 0, BlockType.REDSTONE_WIRE, 15);
            world.setBlock(1, 71, 0, BlockType.REDSTONE_WIRE, 1);

            assertEquals(15, RedstoneEngine.getWeakPower(world, 0, 71, 0, Block.FACE_EAST));
            assertEquals(0, RedstoneEngine.getWeakPower(world, 0, 71, 0, Block.FACE_WEST));
            assertEquals(0, RedstoneEngine.getWeakPower(world, 0, 71, 0, Block.FACE_NORTH));
            assertEquals(0, RedstoneEngine.getWeakPower(world, 0, 71, 0, Block.FACE_SOUTH));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Corner redstone dust should not weak-power through perpendicular branches")
    void cornerRedstoneWireDoesNotWeakPowerPerpendicularBranches() {
        World world = new World(5148L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            world.setBlock(1, 70, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, -1, BlockType.STONE, 0);
            world.setBlock(0, 71, 0, BlockType.REDSTONE_WIRE, 15);
            world.setBlock(1, 71, 0, BlockType.REDSTONE_WIRE, 1);
            world.setBlock(0, 71, -1, BlockType.REDSTONE_WIRE, 1);

            assertEquals(0, RedstoneEngine.getWeakPower(world, 0, 71, 0, Block.FACE_EAST));
            assertEquals(0, RedstoneEngine.getWeakPower(world, 0, 71, 0, Block.FACE_NORTH));
            assertEquals(0, RedstoneEngine.getWeakPower(world, 0, 71, 0, Block.FACE_WEST));
            assertEquals(0, RedstoneEngine.getWeakPower(world, 0, 71, 0, Block.FACE_SOUTH));

            world.setBlock(1, 71, 0, BlockType.AIR, 0);

            assertEquals(15, RedstoneEngine.getWeakPower(world, 0, 71, 0, Block.FACE_NORTH),
                    "Removing the perpendicular east branch leaves a straight north-only output");
            assertEquals(0, RedstoneEngine.getWeakPower(world, 0, 71, 0, Block.FACE_EAST));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Vertical redstone propagation should respect a blocked space above the lower dust")
    void verticalRedstoneWirePropagationRespectsCeiling() {
        World world = new World(5149L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            world.setBlock(0, 71, 0, BlockType.REDSTONE_WIRE, 0);
            world.setBlock(0, 72, 0, BlockType.STONE, 0);
            world.setBlock(1, 71, 0, BlockType.STONE, 0);
            world.setBlock(1, 72, 0, BlockType.REDSTONE_WIRE, 15);

            RedstoneEngine.updateRedstoneWire(world, 0, 71, 0);

            assertEquals(0, world.getBlockMetadata(0, 71, 0));

            world.setBlock(0, 72, 0, BlockType.AIR, 0);
            RedstoneEngine.updateRedstoneWire(world, 0, 71, 0);

            assertEquals(14, world.getBlockMetadata(0, 71, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Glowstone should let redstone climb up but not carry dust power downward")
    void glowstoneRedstoneSupportCarriesPowerUpwardOnly() {
        World world = new World(5150L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            world.setBlock(0, 71, 0, BlockType.REDSTONE_WIRE, 15);
            world.setBlock(1, 71, 0, BlockType.GLOWSTONE, 0);
            world.setBlock(1, 72, 0, BlockType.REDSTONE_WIRE, 0);

            RedstoneEngine.updateRedstoneWire(world, 1, 72, 0);

            assertEquals(14, world.getBlockMetadata(1, 72, 0));

            world.setBlock(10, 70, 0, BlockType.STONE, 0);
            world.setBlock(10, 71, 0, BlockType.REDSTONE_WIRE, 0);
            world.setBlock(11, 71, 0, BlockType.GLOWSTONE, 0);
            world.setBlock(11, 72, 0, BlockType.REDSTONE_WIRE, 15);

            RedstoneEngine.updateRedstoneWire(world, 10, 71, 0);

            assertEquals(0, world.getBlockMetadata(10, 71, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Floor redstone torches invert from and do not power their support block")
    void floorRedstoneTorchUsesSupportBlockBelow() {
        World world = new World(5121L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            world.setBlock(0, 71, 0, BlockType.REDSTONE_TORCH_ON, 5);
            world.advanceBlockTicks(2);

            assertFalse(RedstoneEngine.isBlockPowered(world, 0, 70, 0));
            assertEquals(0, RedstoneEngine.getWeakPower(world, 0, 71, 0, Block.FACE_BOTTOM));
            assertEquals(15, RedstoneEngine.getWeakPower(world, 0, 71, 0, Block.FACE_EAST));

            int leverMetadata = BlockShape.leverMetadataFromFace(Block.FACE_EAST);
            world.setBlock(1, 70, 0, BlockType.LEVER, leverMetadata | RedstoneEngine.POWERED_BIT);
            world.advanceBlockTicks(4);

            assertSame(BlockType.REDSTONE_TORCH_OFF, world.getBlock(0, 71, 0));

            world.setBlock(1, 70, 0, BlockType.LEVER, leverMetadata);
            world.advanceBlockTicks(4);

            assertSame(BlockType.REDSTONE_TORCH_ON, world.getBlock(0, 71, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Wall redstone torches should use Release 1.0 source metadata")
    void wallRedstoneTorchUsesSourceMetadataSupportFace() {
        World world = new World(5122L);
        try {
            world.setBlock(0, 70, 0, BlockType.STONE, 0);
            world.setBlock(1, 70, 0, BlockType.REDSTONE_TORCH_ON, 1);

            assertEquals(0, RedstoneEngine.getWeakPower(world, 1, 70, 0, Block.FACE_WEST));
            assertEquals(15, RedstoneEngine.getWeakPower(world, 1, 70, 0, Block.FACE_EAST));
            assertEquals(15, RedstoneEngine.getWeakPower(world, 1, 70, 0, Block.FACE_NORTH));

            world.breakBlock(0, 70, 0, false);

            assertSame(BlockType.AIR, world.getBlock(1, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Redstone torch burnout history should be scoped per world")
    void redstoneTorchBurnoutDoesNotLeakBetweenWorlds() {
        int leverMetadata = BlockShape.leverMetadataFromFace(Block.FACE_EAST);
        World burnedOutWorld = new World(5129L);
        try {
            burnedOutWorld.setBlock(0, 70, 0, BlockType.STONE, 0);
            burnedOutWorld.setBlock(0, 71, 0, BlockType.REDSTONE_TORCH_ON, 5);

            boolean burnedOut = false;
            for (int i = 0; i < 8; i++) {
                burnedOutWorld.setBlock(1, 70, 0, BlockType.LEVER,
                        leverMetadata | RedstoneEngine.POWERED_BIT);
                burnedOutWorld.advanceBlockTicks(4);
                if (!burnedOutWorld.getSoundEvents().isEmpty()) {
                    burnedOut = true;
                    break;
                }
                burnedOutWorld.setBlock(1, 70, 0, BlockType.LEVER, leverMetadata);
                burnedOutWorld.advanceBlockTicks(4);
            }

            assertTrue(burnedOut);
            assertSame(BlockType.REDSTONE_TORCH_OFF, burnedOutWorld.getBlock(0, 71, 0));
            var sounds = burnedOutWorld.drainSoundEvents();
            assertEquals(1, sounds.size());
            assertEquals(WorldSoundEvent.REDSTONE_TORCH_BURNOUT, sounds.get(0).soundId());
            assertEquals(0.5f, sounds.get(0).volume(), 0.0001f);
            assertTrue(sounds.get(0).pitch() >= 1.8f);
            assertTrue(sounds.get(0).pitch() <= 3.4f);
            assertEquals(5, burnedOutWorld.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SMOKE)
                    .count());

            burnedOutWorld.setBlock(1, 70, 0, BlockType.LEVER, leverMetadata);
            burnedOutWorld.advanceBlockTicks(RedstoneEngine.TORCH_BURNOUT_RECOVERY_TICKS
                    + RedstoneEngine.TORCH_DELAY_TICKS);
            assertSame(BlockType.REDSTONE_TORCH_ON, burnedOutWorld.getBlock(0, 71, 0));
            assertTrue(burnedOutWorld.drainSoundEvents().isEmpty());
        } finally {
            burnedOutWorld.cleanup();
        }

        World freshWorld = new World(5130L);
        try {
            freshWorld.setBlock(0, 70, 0, BlockType.STONE, 0);
            freshWorld.setBlock(0, 71, 0, BlockType.REDSTONE_TORCH_OFF, 5);

            freshWorld.advanceBlockTicks(4);

            assertSame(BlockType.REDSTONE_TORCH_ON, freshWorld.getBlock(0, 71, 0));
        } finally {
            freshWorld.cleanup();
        }
    }

    @Test
    @DisplayName("Note blocks should play once per redstone rising edge")
    void noteBlockPlaysOnRedstoneRisingEdges() {
        World world = new World(5122L);
        try {
            world.setBlock(0, 100, 0, BlockType.NOTE_BLOCK, 0);
            NoteBlockTileEntity note = (NoteBlockTileEntity) world.getTileEntity(0, 100, 0);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            assertEquals(1, note.getPlayTicks());
            assertTrue((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.POWERED_BIT) != 0);

            world.advanceBlockTicks(4);
            assertEquals(1, note.getPlayTicks());

            world.setBlock(-1, 100, 0, BlockType.LEVER, 5);
            assertEquals(0, world.getBlockMetadata(0, 100, 0) & RedstoneEngine.POWERED_BIT);

            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            assertEquals(2, note.getPlayTicks());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Repeaters should cycle four delay settings and use the selected delay")
    void repeaterCyclesAndUsesConfiguredDelay() {
        World world = new World(5153L);
        try {
            int baseMetadata = 3; // Output east, input west.
            world.setBlock(8, 99, 8, BlockType.STONE, 0);
            world.setBlock(7, 99, 8, BlockType.STONE, 0);
            world.setBlock(8, 100, 8, BlockType.REDSTONE_REPEATER_OFF, baseMetadata);

            assertEquals(2, RedstoneEngine.repeaterDelayTicks(world.getBlockMetadata(8, 100, 8)));
            assertTrue(world.toggleBlock(8, 100, 8));
            assertEquals(4, RedstoneEngine.repeaterDelayTicks(world.getBlockMetadata(8, 100, 8)));
            assertTrue(world.toggleBlock(8, 100, 8));
            assertEquals(6, RedstoneEngine.repeaterDelayTicks(world.getBlockMetadata(8, 100, 8)));
            assertTrue(world.toggleBlock(8, 100, 8));
            assertEquals(8, RedstoneEngine.repeaterDelayTicks(world.getBlockMetadata(8, 100, 8)));

            world.setBlock(7, 100, 8, BlockType.REDSTONE_TORCH_ON, Block.FACE_TOP);
            world.advanceBlockTicks(7);
            assertSame(BlockType.REDSTONE_REPEATER_OFF, world.getBlock(8, 100, 8));

            world.advanceBlockTicks(1);
            assertSame(BlockType.REDSTONE_REPEATER_ON, world.getBlock(8, 100, 8));
            assertEquals(baseMetadata | (3 << RedstoneEngine.REPEATER_DELAY_SHIFT),
                    world.getBlockMetadata(8, 100, 8));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 repeaters should ignore side power instead of locking")
    void repeaterIgnoresSidePowerBecauseLockingIsPostReleaseOne() {
        World world = new World(5154L);
        try {
            int metadata = 3; // Output east, input west.
            world.setBlock(8, 99, 8, BlockType.STONE, 0);
            world.setBlock(8, 99, 7, BlockType.STONE, 0);
            world.setBlock(7, 99, 8, BlockType.STONE, 0);
            world.setBlock(8, 100, 8, BlockType.REDSTONE_REPEATER_OFF, metadata);
            world.setBlock(8, 100, 7, BlockType.REDSTONE_TORCH_ON, Block.FACE_TOP);
            world.advanceBlockTicks(4);

            assertSame(BlockType.REDSTONE_REPEATER_OFF, world.getBlock(8, 100, 8));

            world.setBlock(7, 100, 8, BlockType.REDSTONE_TORCH_ON, Block.FACE_TOP);
            world.advanceBlockTicks(2);

            assertSame(BlockType.REDSTONE_REPEATER_ON, world.getBlock(8, 100, 8));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 jukeboxes should not be triggered by redstone")
    void jukeboxIgnoresRedstonePower() {
        World world = new World(5123L);
        try {
            world.setBlock(0, 100, 0, BlockType.JUKEBOX, 0);
            JukeboxTileEntity jukebox = (JukeboxTileEntity) world.getTileEntity(0, 100, 0);
            assertTrue(jukebox.insertRecord(new ItemStack(ItemType.RECORD_13, 1)));

            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);
            world.advanceBlockTicks(8);

            assertFalse(RedstoneEngine.isRedstoneTickable(BlockType.JUKEBOX));
            assertEquals(0, jukebox.getPlayTicks());
            assertEquals(0, world.getBlockMetadata(0, 100, 0) & RedstoneEngine.POWERED_BIT);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Touched redstone ore glows and then fades back")
    void redstoneOreGlowsWhenTouchedAndFades() {
        World world = new World(5118L);
        try {
            world.setBlock(0, 70, 0, BlockType.REDSTONE_ORE, 0);

            assertEquals(9, BlockType.GLOWING_REDSTONE_ORE.getLightEmission());
            assertTrue(world.activateRedstoneOre(0, 70, 0));
            assertSame(BlockType.GLOWING_REDSTONE_ORE, world.getBlock(0, 70, 0));

            world.advanceBlockTicks(29);
            assertSame(BlockType.GLOWING_REDSTONE_ORE, world.getBlock(0, 70, 0));

            world.advanceBlockTicks(1);
            assertSame(BlockType.REDSTONE_ORE, world.getBlock(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Redstone ore activation emits old exposed-face sparkles")
    void redstoneOreActivationEmitsExposedFaceSparkles() {
        World world = new World(5131L);
        try {
            world.setBlock(0, 70, 0, BlockType.REDSTONE_ORE, 0);

            assertTrue(world.activateRedstoneOre(0, 70, 0));

            assertEquals(6, redDustParticles(world));
            assertTrue(world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.RED_DUST)
                    .allMatch(particle -> particle.getData() == WorldParticle.RED_DUST_DEFAULT_COLOR_DATA
                            && (particle.getRenderX(0.0f) < 0.0f || particle.getRenderX(0.0f) > 1.0f
                                    || particle.getRenderY(0.0f) < 70.0f || particle.getRenderY(0.0f) > 71.0f
                                    || particle.getRenderZ(0.0f) < 0.0f || particle.getRenderZ(0.0f) > 1.0f)));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Redstone ore sparkles only leak through exposed faces")
    void redstoneOreSparklesRespectCoveredFaces() {
        World world = new World(5132L);
        try {
            world.setBlock(0, 70, 0, BlockType.REDSTONE_ORE, 0);
            world.setBlock(0, 71, 0, BlockType.AIR, 0);
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, -1, BlockType.STONE, 0);
            world.setBlock(0, 70, 1, BlockType.STONE, 0);
            world.setBlock(-1, 70, 0, BlockType.STONE, 0);
            world.setBlock(1, 70, 0, BlockType.STONE, 0);

            assertTrue(world.activateRedstoneOre(0, 70, 0));

            assertEquals(1, redDustParticles(world));
            WorldParticle sparkle = world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.RED_DUST)
                    .findFirst()
                    .orElseThrow();
            assertEquals(71.0625f, sparkle.getRenderY(0.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Players, mobs, and dropped items light redstone ore when stepping on it")
    void entitiesLightRedstoneOreWhenStepping() {
        World world = new World(5119L);
        try {
            world.setBlock(0, 99, 0, BlockType.REDSTONE_ORE, 0);
            Player player = new Player(0.5f, 100.0f, 0.5f);
            world.setPlayer(player);

            player.update(1.0f / 20.0f, world);

            assertSame(BlockType.GLOWING_REDSTONE_ORE, world.getBlock(0, 99, 0));

            world.setBlock(2, 99, 0, BlockType.REDSTONE_ORE, 0);
            Zombie zombie = new Zombie();
            zombie.setPosition(2.5f, 100.0f, 0.5f);
            world.replaceEntities(java.util.List.of(zombie));

            world.updateEntities(1.0f / 20.0f);

            assertSame(BlockType.GLOWING_REDSTONE_ORE, world.getBlock(2, 99, 0));

            world.setBlock(4, 99, 0, BlockType.REDSTONE_ORE, 0);
            world.spawnThrownStack(4.5f, 100.1f, 0.5f, new ItemStack(ItemType.DIRT, 1), 0.0f, 0.0f, 0.0f);

            world.updateDroppedItems(1.0f / 20.0f);

            assertSame(BlockType.GLOWING_REDSTONE_ORE, world.getBlock(4, 99, 0));
        } finally {
            world.cleanup();
        }
    }

    private static long redDustParticles(World world) {
        return world.getParticles().stream()
                .filter(particle -> particle.getType() == WorldParticle.Type.RED_DUST)
                .count();
    }

    @Test
    @DisplayName("Powered piston pushes one movable block and creates a piston head")
    void pistonPushesBlock() {
        World world = new World(5101L);
        try {
            world.setBlock(0, 100, 0, BlockType.PISTON, Block.FACE_EAST);
            for (int x = 1; x <= 13; x++) {
                world.setBlock(x, 100, 0, BlockType.AIR, 0);
            }
            world.setBlock(1, 100, 0, BlockType.STONE, 0);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(4);

            assertSame(BlockType.PISTON, world.getBlock(0, 100, 0));
            assertTrue((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT) != 0);
            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertSame(BlockType.STONE, world.getBlock(2, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Pistons should expose a moving piston phase before the head settles")
    void pistonExtensionUsesMovingPistonBeforeHeadSettles() {
        World world = new World(5146L);
        try {
            world.setBlock(0, 100, 0, BlockType.PISTON, Block.FACE_EAST);
            world.setBlock(1, 100, 0, BlockType.STONE, 0);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(1);

            assertTrue((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT) != 0);
            assertSame(BlockType.MOVING_PISTON, world.getBlock(1, 100, 0));
            assertEquals(Block.FACE_EAST, world.getBlockMetadata(1, 100, 0) & 7);
            World.MovingPistonState head = world.getMovingPistonState(1, 100, 0);
            assertNotNull(head);
            assertSame(BlockType.PISTON_HEAD, head.carriedType());
            assertSame(BlockType.PISTON_HEAD, head.finalType());
            assertEquals(0.0f, head.fromX(), 0.001f);
            assertEquals(1.0f, head.toX(), 0.001f);

            assertSame(BlockType.MOVING_PISTON, world.getBlock(2, 100, 0));
            World.MovingPistonState pushed = world.getMovingPistonState(2, 100, 0);
            assertNotNull(pushed);
            assertSame(BlockType.STONE, pushed.carriedType());
            assertSame(BlockType.STONE, pushed.finalType());
            assertEquals(1.0f, pushed.fromX(), 0.001f);
            assertEquals(2.0f, pushed.toX(), 0.001f);
            java.util.List<AABB> movingCollisions = world.getMovingPistonCollisionBoxes(
                    new AABB(1, 100, 0, 2, 101, 1));
            assertFalse(movingCollisions.isEmpty());
            assertEquals(1.0f, movingCollisions.get(0).getMin().x, 0.001f);
            assertEquals(2.0f, movingCollisions.get(0).getMax().x, 0.001f);

            world.advanceBlockTicks(RedstoneEngine.PISTON_MOVEMENT_TICKS);

            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertSame(BlockType.STONE, world.getBlock(2, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Moving pistons carry pushed block metadata until settling")
    void movingPistonStateCarriesPushedBlockMetadata() {
        World world = new World(5147L);
        try {
            world.setBlock(0, 100, 0, BlockType.PISTON, Block.FACE_EAST);
            world.setBlock(1, 100, 0, BlockType.WHITE_WOOL, 14);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(1);

            assertSame(BlockType.MOVING_PISTON, world.getBlock(2, 100, 0));
            World.MovingPistonState pushed = world.getMovingPistonState(2, 100, 0);
            assertNotNull(pushed);
            assertSame(BlockType.WHITE_WOOL, pushed.carriedType());
            assertEquals(14, pushed.carriedMetadata());
            assertSame(BlockType.WHITE_WOOL, pushed.finalType());
            assertEquals(14, pushed.finalMetadata());

            world.advanceBlockTicks(RedstoneEngine.PISTON_MOVEMENT_TICKS);

            assertSame(BlockType.WHITE_WOOL, world.getBlock(2, 100, 0));
            assertEquals(14, world.getBlockMetadata(2, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Sticky piston heads should carry sticky metadata while moving and after settling")
    void stickyPistonHeadCarriesStickyMetadata() {
        World world = new World(5160L);
        try {
            world.setBlock(0, 100, 0, BlockType.STICKY_PISTON, Block.FACE_EAST);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(1);

            World.MovingPistonState movingHead = world.getMovingPistonState(1, 100, 0);
            assertNotNull(movingHead);
            assertSame(BlockType.PISTON_HEAD, movingHead.carriedType());
            assertEquals(Block.FACE_EAST, movingHead.carriedMetadata() & 7);
            assertEquals(RedstoneEngine.PISTON_HEAD_STICKY_BIT,
                    movingHead.carriedMetadata() & RedstoneEngine.PISTON_HEAD_STICKY_BIT);
            assertEquals(RedstoneEngine.PISTON_HEAD_STICKY_BIT,
                    movingHead.finalMetadata() & RedstoneEngine.PISTON_HEAD_STICKY_BIT);

            world.advanceBlockTicks(RedstoneEngine.PISTON_MOVEMENT_TICKS);

            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertEquals(Block.FACE_EAST, world.getBlockMetadata(1, 100, 0) & 7);
            assertEquals(RedstoneEngine.PISTON_HEAD_STICKY_BIT,
                    world.getBlockMetadata(1, 100, 0) & RedstoneEngine.PISTON_HEAD_STICKY_BIT);

            world.setBlock(-1, 100, 0, BlockType.LEVER, 5);
            world.advanceBlockTicks(1);

            movingHead = world.getMovingPistonState(1, 100, 0);
            assertNotNull(movingHead);
            assertEquals(RedstoneEngine.PISTON_HEAD_STICKY_BIT,
                    movingHead.carriedMetadata() & RedstoneEngine.PISTON_HEAD_STICKY_BIT);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Pistons crush fragile blocks and move the solid chain into the vacated space")
    void pistonCrushesFragileBlockWhilePushing() {
        World world = new World(5104L);
        try {
            world.setBlock(0, 100, 0, BlockType.PISTON, Block.FACE_EAST);
            world.setBlock(1, 100, 0, BlockType.STONE, 0);
            world.setBlock(2, 99, 0, BlockType.DIRT, 0);
            world.setBlock(2, 100, 0, BlockType.YELLOW_FLOWER, 0);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(4);

            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertSame(BlockType.STONE, world.getBlock(2, 100, 0));
            assertNotSame(BlockType.YELLOW_FLOWER, world.getBlock(2, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Pistons drop dragon eggs instead of pushing them")
    void pistonDropsDragonEgg() {
        World world = new World(5117L);
        try {
            world.setBlock(0, 100, 0, BlockType.PISTON, Block.FACE_EAST);
            world.setBlock(1, 100, 0, BlockType.DRAGON_EGG, 0);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(4);

            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.DRAGON_EGG));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Pistons harvest pumpkins and jack o'lanterns instead of moving them")
    void pistonBreaksPumpkinBlocksIntoDrops() {
        assertPistonBreaksBlockIntoDrop(BlockType.PUMPKIN, ItemType.PUMPKIN);
        assertPistonBreaksBlockIntoDrop(BlockType.JACK_O_LANTERN, ItemType.JACK_O_LANTERN);
    }

    @Test
    @DisplayName("Pistons break melons into slices instead of pushing melon blocks")
    void pistonBreaksMelonsIntoSlices() {
        World world = new World(5124L);
        try {
            setupPoweredEastPiston(world);
            world.setBlock(1, 100, 0, BlockType.MELON, 0);

            world.advanceBlockTicks(4);

            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(2, 100, 0));
            assertTrue(world.getDroppedItems().stream().noneMatch(item -> item.getItemType() == ItemType.MELON_BLOCK));
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.MELON_SLICE
                            && item.getCount() >= 3
                            && item.getCount() <= 7));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Pistons move rails when the destination remains supported")
    void pistonMovesSupportedRails() {
        assertPistonMovesSupportedRail(BlockType.RAIL, ItemType.RAIL);
        assertPistonMovesSupportedRail(BlockType.POWERED_RAIL, ItemType.POWERED_RAIL);
        assertPistonMovesSupportedRail(BlockType.DETECTOR_RAIL, ItemType.DETECTOR_RAIL);
    }

    @Test
    @DisplayName("Pistons pop moved rails when the destination is unsupported")
    void pistonPopsUnsupportedMovedRails() {
        assertPistonPopsUnsupportedRail(BlockType.RAIL, ItemType.RAIL);
        assertPistonPopsUnsupportedRail(BlockType.POWERED_RAIL, ItemType.POWERED_RAIL);
        assertPistonPopsUnsupportedRail(BlockType.DETECTOR_RAIL, ItemType.DETECTOR_RAIL);
    }

    @Test
    @DisplayName("Ascending rails pop when their raised-side support disappears")
    void ascendingRailsPopWhenRaisedSideSupportDisappears() {
        assertAscendingRailPopsWithoutRaisedSupport(BlockType.RAIL, ItemType.RAIL);
        assertAscendingRailPopsWithoutRaisedSupport(BlockType.POWERED_RAIL, ItemType.POWERED_RAIL);
        assertAscendingRailPopsWithoutRaisedSupport(BlockType.DETECTOR_RAIL, ItemType.DETECTOR_RAIL);
    }

    @Test
    @DisplayName("Pistons break snow layers instead of moving them")
    void pistonBreaksSnowLayerIntoSnowball() {
        World world = new World(5142L);
        try {
            setupPoweredEastPiston(world);
            world.setBlock(1, 99, 0, BlockType.STONE, 0);
            world.setBlock(2, 99, 0, BlockType.STONE, 0);
            world.setBlock(1, 100, 0, BlockType.SNOW_LAYER, 0);

            world.advanceBlockTicks(4);

            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(2, 100, 0));
            assertEquals(1, droppedCount(world, ItemType.SNOWBALL));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Sticky pistons should not pull snow layers on retraction")
    void stickyPistonDoesNotPullSnowLayerOnRetraction() {
        World world = new World(5143L);
        try {
            world.setBlock(0, 100, 0, BlockType.STICKY_PISTON, Block.FACE_EAST);
            world.setBlock(2, 99, 0, BlockType.STONE, 0);
            world.setBlock(1, 100, 0, BlockType.AIR, 0);
            world.setBlock(2, 100, 0, BlockType.SNOW_LAYER, 0);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(4);

            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertSame(BlockType.SNOW_LAYER, world.getBlock(2, 100, 0));

            world.setBlock(-1, 100, 0, BlockType.LEVER, 5);
            world.advanceBlockTicks(4);

            assertSame(BlockType.AIR, world.getBlock(1, 100, 0));
            assertSame(BlockType.SNOW_LAYER, world.getBlock(2, 100, 0));
            assertEquals(0, droppedCount(world, ItemType.SNOWBALL));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Pistons do not extend when more than twelve blocks would move")
    void pistonRespectsTwelveBlockPushLimit() {
        World world = new World(5105L);
        try {
            world.setBlock(0, 100, 0, BlockType.PISTON, Block.FACE_EAST);
            for (int x = 1; x <= 13; x++) {
                world.setBlock(x, 100, 0, BlockType.STONE, 0);
            }
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(4);

            assertFalse((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT) != 0);
            assertSame(BlockType.STONE, world.getBlock(1, 100, 0));
            assertSame(BlockType.STONE, world.getBlock(13, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Pistons can extend into valid bottom and top world-height cells")
    void pistonsExtendIntoWorldHeightEdges() {
        World world = new World(5150L);
        try {
            world.setBlock(0, Chunk.HEIGHT - 2, 0, BlockType.PISTON, Block.FACE_TOP);
            world.setBlock(0, Chunk.HEIGHT - 1, 0, BlockType.AIR, 0);
            powerPistonFromWest(world, 0, Chunk.HEIGHT - 2, 0);

            world.setBlock(4, 1, 0, BlockType.PISTON, Block.FACE_BOTTOM);
            world.setBlock(4, 0, 0, BlockType.AIR, 0);
            powerPistonFromWest(world, 4, 1, 0);

            world.advanceBlockTicks(4);

            assertTrue((world.getBlockMetadata(0, Chunk.HEIGHT - 2, 0) & RedstoneEngine.PISTON_EXTENDED_BIT) != 0);
            assertSame(BlockType.PISTON_HEAD, world.getBlock(0, Chunk.HEIGHT - 1, 0));
            assertTrue((world.getBlockMetadata(4, 1, 0) & RedstoneEngine.PISTON_EXTENDED_BIT) != 0);
            assertSame(BlockType.PISTON_HEAD, world.getBlock(4, 0, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Pistons still refuse to push blocks out of the top of the world")
    void pistonsRefuseToPushPastTopWorldHeight() {
        World world = new World(5151L);
        try {
            world.setBlock(0, Chunk.HEIGHT - 2, 0, BlockType.PISTON, Block.FACE_TOP);
            world.setBlock(0, Chunk.HEIGHT - 1, 0, BlockType.STONE, 0);
            powerPistonFromWest(world, 0, Chunk.HEIGHT - 2, 0);

            world.advanceBlockTicks(4);

            assertEquals(0, world.getBlockMetadata(0, Chunk.HEIGHT - 2, 0) & RedstoneEngine.PISTON_EXTENDED_BIT);
            assertSame(BlockType.STONE, world.getBlock(0, Chunk.HEIGHT - 1, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Sticky pistons can pull movable blocks from y=0")
    void stickyPistonPullsBlockFromBottomWorldHeight() {
        World world = new World(5152L);
        try {
            world.setBlock(0, 2, 0, BlockType.STICKY_PISTON, Block.FACE_BOTTOM);
            world.setBlock(0, 1, 0, BlockType.AIR, 0);
            world.setBlock(0, 0, 0, BlockType.STONE, 0);
            powerPistonFromWest(world, 0, 2, 0);

            world.advanceBlockTicks(4);

            assertSame(BlockType.PISTON_HEAD, world.getBlock(0, 1, 0));
            assertSame(BlockType.STONE, world.getBlock(0, 0, 0));

            world.setBlock(-1, 2, 0, BlockType.LEVER,
                    BlockShape.leverMetadataFromFace(Block.FACE_WEST));
            world.advanceBlockTicks(4);

            assertEquals(0, world.getBlockMetadata(0, 2, 0) & RedstoneEngine.PISTON_EXTENDED_BIT);
            assertSame(BlockType.STONE, world.getBlock(0, 1, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 0, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Sticky pistons pull movable blocks back on retraction")
    void stickyPistonPullsMovableBlockOnRetraction() {
        World world = new World(5106L);
        try {
            world.setBlock(0, 100, 0, BlockType.STICKY_PISTON, Block.FACE_EAST);
            world.setBlock(1, 100, 0, BlockType.STONE, 0);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(4);

            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertSame(BlockType.STONE, world.getBlock(2, 100, 0));

            world.setBlock(-1, 100, 0, BlockType.LEVER, 5);
            world.advanceBlockTicks(4);

            assertFalse((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT) != 0);
            assertSame(BlockType.STONE, world.getBlock(1, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(2, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Short-pulsed sticky pistons should drop the pushed block")
    void shortPulsedStickyPistonDropsPushedBlock() {
        World world = new World(5128L);
        try {
            world.setBlock(0, 100, 0, BlockType.STICKY_PISTON, Block.FACE_EAST);
            world.setBlock(1, 100, 0, BlockType.STONE, 0);
            world.setBlock(2, 100, 0, BlockType.AIR, 0);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(1);
            assertSame(BlockType.MOVING_PISTON, world.getBlock(1, 100, 0));
            assertSame(BlockType.MOVING_PISTON, world.getBlock(2, 100, 0));
            World.MovingPistonState pushed = world.getMovingPistonState(2, 100, 0);
            assertNotNull(pushed);
            assertSame(BlockType.STONE, pushed.carriedType());

            world.setBlock(-1, 100, 0, BlockType.LEVER, 5);
            world.advanceBlockTicks(1);

            assertFalse((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT) != 0);
            assertSame(BlockType.MOVING_PISTON, world.getBlock(1, 100, 0));
            assertSame(BlockType.MOVING_PISTON, world.getBlock(2, 100, 0));

            world.advanceBlockTicks(RedstoneEngine.PISTON_MOVEMENT_TICKS);

            assertSame(BlockType.AIR, world.getBlock(1, 100, 0));
            assertSame(BlockType.STONE, world.getBlock(2, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Short-pulsed sticky pistons pull the skipped block when no block was pushed")
    void shortPulsedStickyPistonPullsSkippedBlockWhenFrontWasAir() {
        World world = new World(5129L);
        try {
            world.setBlock(0, 100, 0, BlockType.STICKY_PISTON, Block.FACE_EAST);
            world.setBlock(1, 100, 0, BlockType.AIR, 0);
            world.setBlock(2, 100, 0, BlockType.STONE, 0);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(1);
            assertSame(BlockType.MOVING_PISTON, world.getBlock(1, 100, 0));
            assertSame(BlockType.STONE, world.getBlock(2, 100, 0));

            world.setBlock(-1, 100, 0, BlockType.LEVER, 5);
            world.advanceBlockTicks(1);

            assertFalse((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT) != 0);
            assertSame(BlockType.MOVING_PISTON, world.getBlock(1, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(2, 100, 0));

            world.advanceBlockTicks(RedstoneEngine.PISTON_MOVEMENT_TICKS);

            assertSame(BlockType.STONE, world.getBlock(1, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(2, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Pistons refuse tile entities and obsidian")
    void pistonRefusesImmovableBlocks() {
        World world = new World(5107L);
        try {
            world.setBlock(0, 100, 0, BlockType.PISTON, Block.FACE_EAST);
            world.setBlock(1, 100, 0, BlockType.CHEST, 0);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(4);

            assertFalse((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT) != 0);
            assertSame(BlockType.CHEST, world.getBlock(1, 100, 0));

            world.setBlock(1, 100, 0, BlockType.OBSIDIAN, 0);
            world.advanceBlockTicks(4);

            assertFalse((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT) != 0);
            assertSame(BlockType.OBSIDIAN, world.getBlock(1, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Pistons refuse signs as source tile entity blocks")
    void pistonRefusesSignsAsTileEntityBlocks() {
        assertPistonRefusesSign(BlockType.STANDING_SIGN, 0);
        assertPistonRefusesSign(BlockType.WALL_SIGN, Block.FACE_WEST);
    }

    @Test
    @DisplayName("Pistons refuse enchanting tables as legacy tile entity blocks")
    void pistonRefusesEnchantingTable() {
        World world = new World(5145L);
        try {
            setupPoweredEastPiston(world);
            world.setBlock(1, 100, 0, BlockType.ENCHANTING_TABLE, 0);

            world.advanceBlockTicks(4);

            assertFalse((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT) != 0);
            assertSame(BlockType.ENCHANTING_TABLE, world.getBlock(1, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(2, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Pistons use Release 1.0 quasi-connectivity from the block space above")
    void pistonUsesQuasiConnectivity() {
        World world = new World(5108L);
        try {
            world.setBlock(-1, 101, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);
            world.setBlock(0, 100, 0, BlockType.PISTON, Block.FACE_EAST);
            world.setBlock(1, 100, 0, BlockType.STONE, 0);

            world.advanceBlockTicks(4);

            assertTrue((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT) != 0);
            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertSame(BlockType.STONE, world.getBlock(2, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Live quasi-connectivity power changes should wake already-placed pistons")
    void liveQuasiConnectivityPowerChangeSchedulesPiston() {
        World world = new World(5184L);
        try {
            world.setBlock(0, 100, 0, BlockType.PISTON, Block.FACE_EAST);
            world.setBlock(1, 100, 0, BlockType.STONE, 0);
            world.setBlock(-2, 101, 0, BlockType.STONE, 0);
            int leverMetadata = BlockShape.leverMetadataFromFace(Block.FACE_EAST);

            world.advanceBlockTicks(4);

            assertEquals(0, world.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT);

            world.setBlock(-1, 101, 0, BlockType.LEVER, leverMetadata | RedstoneEngine.POWERED_BIT);
            world.advanceBlockTicks(4);

            assertTrue((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT) != 0);
            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertSame(BlockType.STONE, world.getBlock(2, 100, 0));

            world.setBlock(-1, 101, 0, BlockType.LEVER, leverMetadata);
            world.advanceBlockTicks(4);

            assertEquals(0, world.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT);
            assertSame(BlockType.AIR, world.getBlock(1, 100, 0));
            assertSame(BlockType.STONE, world.getBlock(2, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Quasi-connectivity updates should include powered solid-block relays beside the above space")
    void quasiConnectivitySchedulesPistonFromPoweredSolidRelay() {
        World world = new World(5185L);
        try {
            world.setBlock(0, 100, 0, BlockType.PISTON, Block.FACE_EAST);
            world.setBlock(1, 100, 0, BlockType.STONE, 0);
            world.setBlock(-3, 101, 0, BlockType.STONE, 0);
            world.setBlock(-1, 101, 0, BlockType.STONE, 0);
            int leverMetadata = BlockShape.leverMetadataFromFace(Block.FACE_EAST);

            world.setBlock(-2, 101, 0, BlockType.LEVER, leverMetadata | RedstoneEngine.POWERED_BIT);
            world.advanceBlockTicks(4);

            assertTrue((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT) != 0);
            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertSame(BlockType.STONE, world.getBlock(2, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Breaking an extended piston head removes and drops the attached base")
    void breakingPistonHeadRemovesAttachedBase() {
        World world = new World(5138L);
        try {
            setupPoweredEastPiston(world);
            world.advanceBlockTicks(4);

            assertTrue(world.breakBlock(1, 100, 0, true));

            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(1, 100, 0));
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.PISTON && item.getCount() == 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Breaking an extended piston base clears its head and preserves sticky drops")
    void breakingExtendedPistonBaseRemovesHead() {
        World world = new World(5139L);
        try {
            world.setBlock(0, 100, 0, BlockType.STICKY_PISTON, Block.FACE_EAST);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);
            world.advanceBlockTicks(4);

            assertTrue(world.breakBlock(0, 100, 0, true));

            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(1, 100, 0));
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.STICKY_PISTON && item.getCount() == 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Piston heads should remove themselves when their attached base disappears")
    void orphanedPistonHeadBreaksWhenBaseDisappears() {
        World world = new World(5140L);
        try {
            setupPoweredEastPiston(world);
            world.advanceBlockTicks(4);

            assertTrue(world.isBlockSupported(1, 100, 0));

            world.setBlock(0, 100, 0, BlockType.AIR, 0);

            assertSame(BlockType.AIR, world.getBlock(0, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(1, 100, 0));
            assertTrue(world.getDroppedItems().stream()
                    .noneMatch(item -> item.getItemType() == ItemType.PISTON));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Extending piston heads push entities and players out of the front block")
    void pistonHeadPushesEntitiesOutOfFrontBlock() {
        World world = new World(5125L);
        try {
            setupPoweredEastPiston(world);
            Zombie zombie = new Zombie();
            zombie.setPosition(1.5f, 100.0f, 0.5f);
            world.replaceEntities(java.util.List.of(zombie));
            Player player = new Player(1.5f, 100.0f, 0.5f);
            world.setPlayer(player);

            world.advanceBlockTicks(4);

            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertEquals(2.5f, zombie.getX(), 0.001f);
            assertEquals(2.5f, player.getPosition().x, 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Pistons damage mobs instead of pushing them through blocked destinations")
    void pistonDamagesMobWhenPushDestinationIsBlocked() {
        World world = new World(5131L);
        try {
            setupPoweredEastPiston(world);
            world.setBlock(2, 100, 0, BlockType.STONE, 0);
            Zombie zombie = new Zombie();
            zombie.setPosition(1.5f, 100.0f, 0.5f);
            world.replaceEntities(java.util.List.of(zombie));
            float beforeHealth = zombie.getHealth();

            world.advanceBlockTicks(4);

            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertEquals(1.5f, zombie.getX(), 0.001f);
            assertEquals(beforeHealth - 1.0f, zombie.getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Pistons damage players instead of pushing them through blocked destinations")
    void pistonDamagesPlayerWhenPushDestinationIsBlocked() {
        World world = new World(5132L);
        try {
            setupPoweredEastPiston(world);
            world.setBlock(2, 100, 0, BlockType.STONE, 0);
            Player player = new Player(1.5f, 100.0f, 0.5f);
            world.setPlayer(player);
            player.getStats().restore(20.0f, 20.0f, 5.0f, 15.0f);
            float beforeHealth = player.getStats().getHealth();

            world.advanceBlockTicks(4);

            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertEquals(1.5f, player.getPosition().x, 0.001f);
            assertEquals(beforeHealth - 1.0f, player.getStats().getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Blocks pushed by pistons shove entities occupying their destination")
    void pistonMovedBlockPushesEntityAtDestination() {
        World world = new World(5126L);
        try {
            setupPoweredEastPiston(world);
            world.setBlock(1, 100, 0, BlockType.STONE, 0);
            Zombie zombie = new Zombie();
            zombie.setPosition(2.5f, 100.0f, 0.5f);
            world.replaceEntities(java.util.List.of(zombie));

            world.advanceBlockTicks(4);

            assertSame(BlockType.STONE, world.getBlock(2, 100, 0));
            assertEquals(3.5f, zombie.getX(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Moving piston blocks actively shove entities during travel")
    void movingPistonBlockShovesEntityDuringTravel() {
        World world = new World(5155L);
        try {
            setupPoweredEastPiston(world);
            world.setBlock(1, 100, 0, BlockType.STONE, 0);

            world.advanceBlockTicks(1);

            assertSame(BlockType.MOVING_PISTON, world.getBlock(2, 100, 0));
            Zombie zombie = new Zombie();
            zombie.setPosition(2.25f, 100.0f, 0.5f);
            world.replaceEntities(java.util.List.of(zombie));
            float beforeHealth = zombie.getHealth();

            world.advanceBlockTicks(1);

            assertTrue(zombie.getX() > 2.25f);
            assertTrue(zombie.getMotionX() > 0.0f);

            world.advanceBlockTicks(1);

            assertSame(BlockType.STONE, world.getBlock(2, 100, 0));
            assertTrue(zombie.getBoundingBox().getMin().x > 3.0f);
            assertEquals(beforeHealth, zombie.getHealth(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Pistons push dropped item entities out of the occupied block")
    void pistonPushesDroppedItems() {
        World world = new World(5127L);
        try {
            setupPoweredEastPiston(world);
            world.spawnThrownStack(1.5f, 100.5f, 0.5f, new ItemStack(ItemType.DIRT, 1),
                    0.0f, 0.0f, 0.0f);

            world.advanceBlockTicks(4);

            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertEquals(2.5f, world.getDroppedItems().get(0).getX(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("End crystals should maintain fire at their block without burning themselves")
    void endCrystalMaintainsFireWithoutSelfDamage() {
        World world = new World(5149L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            world.setBlock(0, 69, 0, BlockType.OBSIDIAN, 0);
            world.setBlock(0, 70, 0, BlockType.AIR, 0);
            EndCrystalEntity crystal = new EndCrystalEntity(0.5f, 70.0f, 0.5f);
            world.spawnEntity(crystal);

            world.updateEntities(1.0f / 20.0f);

            assertSame(BlockType.FIRE, world.getBlock(0, 70, 0));
            assertFalse(crystal.isOnFire());
            assertFalse(crystal.damage(1.0f, DamageSource.point(DamageSource.Type.FIRE,
                    0.5f, 70.0f, 0.5f, 0.0f, 0.0f)));
            assertFalse(crystal.isExploded());
            assertFalse(crystal.isRemoved());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("End crystals should replace their block with source-maintained End fire")
    void endCrystalMaintainedFireReplacesNonFireBlockInEnd() {
        World world = new World(5159L, WorldGenerator.RELEASE_ONE, Dimension.THE_END);
        try {
            world.setBlock(0, 70, 0, BlockType.BEDROCK, 0);
            EndCrystalEntity crystal = new EndCrystalEntity(0.5f, 70.0f, 0.5f);
            world.spawnEntity(crystal);

            world.updateEntities(1.0f / 20.0f);

            assertSame(BlockType.FIRE, world.getBlock(0, 70, 0),
                    "EntityEnderCrystal replaces any non-fire block at its floored position in The End");
            assertFalse(crystal.isExploded());
            assertFalse(crystal.isRemoved());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("End crystals should ignore living potion state and healing")
    void endCrystalIgnoresLivingPotionStateAndHealing() {
        EndCrystalEntity crystal = new EndCrystalEntity(0.0f, 70.0f, 0.0f);
        float health = crystal.getHealth();

        crystal.addEffect(new StatusEffectInstance(StatusEffectType.REGENERATION, 100, 0));
        crystal.setActiveEffects(java.util.List.of(new StatusEffectInstance(StatusEffectType.POISON, 100, 0)));
        crystal.setHealth(1.0f);
        crystal.heal(4.0f);

        assertTrue(crystal.getActiveEffects().isEmpty());
        assertFalse(crystal.hasEffect(StatusEffectType.REGENERATION));
        assertFalse(crystal.hasEffect(StatusEffectType.POISON));
        assertEquals(health, crystal.getHealth(), 0.001f);
    }

    @Test
    @DisplayName("End crystal damage should bypass living invulnerability frames")
    void endCrystalDamageBypassesLivingInvulnerabilityFrames() {
        World world = new World(5150L);
        try {
            InvulnerableEndCrystal crystal = new InvulnerableEndCrystal(0.0f, 70.0f, 0.0f);
            crystal.primeLivingInvulnerability(20, 10.0f);
            world.spawnEntity(crystal);

            assertTrue(crystal.damage(0.0f, DamageSource.generic()));

            assertTrue(crystal.isExploded());
            assertTrue(crystal.isRemoved());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("End crystals explode immediately with Release 1.0 blast strength")
    void endCrystalExplodesImmediatelyWithReleaseOneExplosionPower() {
        World world = new World(5109L);
        try {
            world.setBlock(2, 70, 0, BlockType.STONE, 0);
            EndCrystalEntity crystal = new EndCrystalEntity(0.0f, 70.0f, 0.0f);
            world.spawnEntity(crystal);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(crystal.damage(10.0f, DamageSource.generic()));
            assertTrue(crystal.isExploded());
            assertTrue(crystal.isRemoved());
            assertSame(BlockType.AIR, world.getBlock(2, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("End crystal explosions should not break obsidian or unbreakable End blocks")
    void endCrystalExplosionPreservesObsidianAndUnbreakableEndBlocks() {
        World world = new World(5130L);
        try {
            world.setBlock(1, 70, 0, BlockType.OBSIDIAN, 0);
            world.setBlock(2, 70, 0, BlockType.END_PORTAL_FRAME, 0);
            world.setBlock(3, 70, 0, BlockType.END_PORTAL, 0);
            world.setBlock(4, 70, 0, BlockType.WATER, 0);
            world.setBlock(0, 70, 2, BlockType.STONE, 0);
            EndCrystalEntity crystal = new EndCrystalEntity(0.0f, 70.0f, 0.0f);
            world.spawnEntity(crystal);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(crystal.damage(1.0f, DamageSource.generic()));

            assertSame(BlockType.OBSIDIAN, world.getBlock(1, 70, 0));
            assertSame(BlockType.END_PORTAL_FRAME, world.getBlock(2, 70, 0));
            assertSame(BlockType.END_PORTAL, world.getBlock(3, 70, 0));
            assertSame(BlockType.WATER, world.getBlock(4, 70, 0));
            assertSame(BlockType.AIR, world.getBlock(0, 70, 2));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Explosion rays should stop behind high-resistance blocks")
    void explosionRaysStopBehindHighResistanceBlocks() {
        World world = new World(5133L);
        try {
            for (int y = 68; y <= 72; y++) {
                for (int z = -2; z <= 2; z++) {
                    world.setBlock(1, y, z, BlockType.OBSIDIAN, 0);
                }
            }
            world.setBlock(2, 70, 0, BlockType.STONE, 0);
            EndCrystalEntity crystal = new EndCrystalEntity(0.0f, 70.0f, 0.0f);
            world.spawnEntity(crystal);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(crystal.damage(1.0f, DamageSource.generic()));

            assertSame(BlockType.STONE, world.getBlock(2, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("End crystal explosions should prime nearby TNT instead of deleting it")
    void endCrystalExplosionPrimesNearbyTnt() {
        World world = new World(5131L);
        try {
            world.setBlock(3, 70, 0, BlockType.TNT, 0);
            EndCrystalEntity crystal = new EndCrystalEntity(0.0f, 70.0f, 0.0f);
            world.spawnEntity(crystal);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(crystal.damage(1.0f, DamageSource.generic()));
            assertSame(BlockType.AIR, world.getBlock(3, 70, 0));

            world.updateEntities(1.0f / 20.0f);
            PrimedTntEntity tnt = (PrimedTntEntity) world.getEntities().stream()
                    .filter(entity -> entity instanceof PrimedTntEntity)
                    .findFirst()
                    .orElseThrow();
            assertTrue(tnt.getFuseTicks() >= RedstoneEngine.TNT_FUSE_TICKS / 8 - 1);
            assertTrue(tnt.getFuseTicks() < RedstoneEngine.TNT_FUSE_TICKS / 8 + RedstoneEngine.TNT_FUSE_TICKS / 4);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("End crystal explosions destroy nearby crystals without recursive crystal blasts")
    void endCrystalExplosionDestroysNearbyCrystalsWithoutRecursiveBlast() {
        World world = new World(5120L);
        try {
            world.setBlock(0, 70, 0, BlockType.AIR, 0);
            world.setBlock(3, 70, 0, BlockType.AIR, 0);
            EndCrystalEntity first = new EndCrystalEntity(0.0f, 70.0f, 0.0f);
            EndCrystalEntity second = new EndCrystalEntity(3.0f, 70.0f, 0.0f);
            world.spawnEntity(first);
            world.spawnEntity(second);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(first.damage(1.0f, DamageSource.generic()));

            assertTrue(first.isExploded());
            assertFalse(second.isExploded(),
                    "Explosion-sourced crystal damage should not create another crystal explosion");
            assertTrue(first.isRemoved());
            assertTrue(second.isRemoved());
            assertEquals(1, world.drainSoundEvents().stream()
                    .filter(sound -> WorldSoundEvent.EXPLOSION.equals(sound.soundId()))
                    .count());
            assertEquals(1, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.HUGE_EXPLOSION)
                    .count());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Explosion block drops should use Release 1.0 inverse blast-power chance")
    void explosionBlockDropsUseInverseBlastPowerChance() {
        assertEquals(1.0f, World.explosionBlockDropChance(0.5f), 0.0001f);
        assertEquals(1.0f / 3.0f, World.explosionBlockDropChance(3.0f), 0.0001f);
        assertEquals(0.25f, World.explosionBlockDropChance(4.0f), 0.0001f);
        assertEquals(0.2f, World.explosionBlockDropChance(World.BED_EXPLOSION_POWER), 0.0001f);
        assertEquals(1.0f / EndCrystalEntity.EXPLOSION_POWER,
                World.explosionBlockDropChance(EndCrystalEntity.EXPLOSION_POWER), 0.0001f);

        assertTrue(World.shouldDropBlockFromExplosion(new FixedFloatRandom(0.3333f), 3.0f));
        assertFalse(World.shouldDropBlockFromExplosion(new FixedFloatRandom(0.3334f), 3.0f));
        assertTrue(World.shouldDropBlockFromExplosion(new FixedFloatRandom(0.25f), 4.0f));
        assertFalse(World.shouldDropBlockFromExplosion(new FixedFloatRandom(0.2501f), 4.0f));
        assertTrue(World.shouldDropBlockFromExplosion(new FixedFloatRandom(0.2f), World.BED_EXPLOSION_POWER));
        assertFalse(World.shouldDropBlockFromExplosion(new FixedFloatRandom(0.2001f), World.BED_EXPLOSION_POWER));
    }

    @Test
    @DisplayName("Low-power explosions should keep destroyed block drops guaranteed")
    void lowPowerExplosionsKeepDestroyedBlockDropsGuaranteed() {
        World world = new World(5136L);
        try {
            for (int x = -2; x <= 2; x++) {
                for (int y = 68; y <= 72; y++) {
                    for (int z = -2; z <= 2; z++) {
                        world.setBlock(x, y, z, BlockType.AIR, 0);
                    }
                }
            }
            world.setBlock(0, 70, 0, BlockType.DIRT, 0);

            world.explode(0.5f, 70.5f, 0.5f, 1.0f);

            assertSame(BlockType.AIR, world.getBlock(0, 70, 0));
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.DIRT && item.getCount() >= 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Flaming explosions should only place fire above opaque full cubes")
    void flamingExplosionFiresRequireOpaqueSupport() {
        assertTrue(World.canPlaceExplosionFireOn(BlockType.STONE));
        assertFalse(World.canPlaceExplosionFireOn(BlockType.GLASS));
        assertFalse(World.canPlaceExplosionFireOn(BlockType.CHEST));
        assertFalse(World.canPlaceExplosionFireOn(BlockType.FENCE));

        World world = new RandomOverrideWorld(5137L, new ZeroIntRandom());
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.AIR, 0);
            world.setBlock(1, 69, 0, BlockType.GLASS, 0);
            world.setBlock(1, 70, 0, BlockType.AIR, 0);

            world.igniteExplosionFires(Set.of(
                    new BlockPos(0, 70, 0),
                    new BlockPos(1, 70, 0)));

            assertSame(BlockType.FIRE, world.getBlock(0, 70, 0));
            assertSame(BlockType.AIR, world.getBlock(1, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Explosion entity reach should use doubled explosion power radius")
    void explosionEntityReachUsesDoubledPowerRadius() {
        World world = new World(5134L);
        try {
            clearExplosionCorridor(world, 9);
            Zombie zombie = new Zombie();
            zombie.setPosition(8.25f, 70.0f, 0.0f);
            world.replaceEntities(java.util.List.of(zombie));
            float beforeHealth = zombie.getHealth();

            world.explode(0.0f, 70.0f, 0.0f, 4.0f);

            assertEquals(beforeHealth, zombie.getHealth(), 0.001f);
            assertEquals(0.0f, zombie.getMotionX(), 0.001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Explosion entity damage and push should use Release impact math")
    void explosionEntityDamageAndPushUsesReleaseImpactMath() {
        World world = new World(5135L);
        try {
            clearExplosionCorridor(world, 7);
            Zombie zombie = new Zombie();
            zombie.setPosition(7.0f, 70.0f, 0.0f);
            world.replaceEntities(java.util.List.of(zombie));
            Player player = new Player(7.0f, 70.0f, 0.0f);
            player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);
            world.setPlayer(player);

            world.explode(0.0f, 70.0f, 0.0f, 4.0f);

            float rawDamage = releaseExplosionDamage(4.0f, 7.0f, 1.0f);
            assertEquals(20.0f - rawDamage, zombie.getHealth(), 0.001f);
            assertEquals(20.0f - CombatRules.easyExplosionDamage(rawDamage), player.getStats().getHealth(), 0.001f);
            assertTrue(zombie.getMotionX() > 0.0f);
            assertEquals(0.0f, zombie.getMotionY(), 0.0001f);
            assertTrue(player.getVelocity().x > 0.0f);
            assertEquals(0.0f, player.getVelocity().y, 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Minecart placement on detector rail powers the detector")
    void detectorRailSeesMinecart() {
        World world = new World(5102L);
        try {
            world.setBlock(0, 99, 0, BlockType.STONE);
            world.setBlock(0, 100, 0, BlockType.DETECTOR_RAIL, 0);

            assertTrue(world.placeMinecartOnRail(0, 100, 0, ItemType.MINECART));
            world.updateEntities(1.0f / 20.0f);
            world.advanceBlockTicks(12);

            assertTrue(world.getEntities().stream().anyMatch(entity -> entity instanceof MinecartEntity));
            assertTrue((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.RAIL_POWERED_BIT) != 0);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Detector rails use the inset Release-style minecart sensing box")
    void detectorRailUsesInsetMinecartSensingBox() {
        World world = new World(5152L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.DETECTOR_RAIL, RailShapeResolver.EAST_WEST);
            MinecartEntity cart = new MinecartEntity(-0.37f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            world.replaceEntities(java.util.List.of(cart));

            world.scheduleBlockTick(0, 70, 0, BlockType.DETECTOR_RAIL, 0);
            world.advanceBlockTicks(1);

            assertEquals(0, world.getBlockMetadata(0, 70, 0) & RedstoneEngine.RAIL_POWERED_BIT);

            cart.setPosition(-0.36f, 70.1f, 0.5f);
            world.scheduleBlockTick(0, 70, 0, BlockType.DETECTOR_RAIL, 0);
            world.advanceBlockTicks(1);

            assertTrue((world.getBlockMetadata(0, 70, 0) & RedstoneEngine.RAIL_POWERED_BIT) != 0);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Detector rails should preserve resolved shape when powering")
    void detectorRailPoweringPreservesResolvedShape() {
        World world = new World(5153L);
        try {
            world.setBlock(-1, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(1, 69, 0, BlockType.STONE, 0);
            world.setBlock(-1, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            world.setBlock(0, 70, 0, BlockType.DETECTOR_RAIL, RailShapeResolver.NORTH_SOUTH);
            world.setBlock(1, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            world.replaceEntities(java.util.List.of(cart));

            world.scheduleBlockTick(0, 70, 0, BlockType.DETECTOR_RAIL, 0);
            world.advanceBlockTicks(1);

            assertEquals(RailShapeResolver.EAST_WEST, world.getBlockMetadata(0, 70, 0) & 7);
            assertTrue((world.getBlockMetadata(0, 70, 0) & RedstoneEngine.RAIL_POWERED_BIT) != 0);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ordinary rail curve metadata should not persist as a powered bit")
    void ordinaryRailCurveMetadataDoesNotPersistAsPoweredBit() {
        World world = new World(5155L);
        try {
            world.setBlock(-1, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(1, 69, 0, BlockType.STONE, 0);
            world.setBlock(-1, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            world.setBlock(0, 70, 0, BlockType.RAIL, RailShapeResolver.CURVE_NORTH_WEST);
            world.setBlock(1, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);

            world.scheduleBlockTick(0, 70, 0, BlockType.RAIL, 0);
            world.advanceBlockTicks(2);

            assertEquals(RailShapeResolver.EAST_WEST, world.getBlockMetadata(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Ordinary rail junctions should use the Release-era unpowered curve preference")
    void ordinaryRailJunctionUsesSourceUnpoweredCurvePreference() {
        World world = new World(5156L);
        try {
            setupFourWayRailJunction(world, 0, 70, 0);

            world.scheduleBlockTick(0, 70, 0, BlockType.RAIL, 0);
            world.advanceBlockTicks(1);

            assertEquals(RailShapeResolver.CURVE_SOUTH_EAST, world.getBlockMetadata(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Powered ordinary rail junctions should flip to the Release-era powered curve preference")
    void poweredOrdinaryRailJunctionUsesSourcePoweredCurvePreference() {
        World world = new World(5157L);
        try {
            setupFourWayRailJunction(world, 0, 70, 0);
            world.setBlock(0, 68, 0, BlockType.LEVER,
                    BlockShape.leverMetadataFromFace(Block.FACE_BOTTOM) | RedstoneEngine.POWERED_BIT);

            world.scheduleBlockTick(0, 70, 0, BlockType.RAIL, 0);
            world.advanceBlockTicks(1);

            assertEquals(RailShapeResolver.CURVE_NORTH_WEST, world.getBlockMetadata(0, 70, 0));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Rails should not connect sideways to a neighboring rail that already has two connections")
    void railShapeIgnoresFullNeighborThatCannotAcceptConnection() {
        World world = new World(5158L);
        try {
            setRailWithSupport(world, 0, 70, -1, RailShapeResolver.NORTH_SOUTH);
            setRailWithSupport(world, 0, 70, 0, RailShapeResolver.NORTH_SOUTH);
            setRailWithSupport(world, 0, 70, 1, RailShapeResolver.NORTH_SOUTH);
            setRailWithSupport(world, 1, 70, -1, RailShapeResolver.NORTH_SOUTH);
            setRailWithSupport(world, 1, 70, 0, RailShapeResolver.NORTH_SOUTH);
            setRailWithSupport(world, 1, 70, 1, RailShapeResolver.NORTH_SOUTH);

            assertEquals(RailShapeResolver.NORTH_SOUTH,
                    RailShapeResolver.resolveShape(world, 0, 70, 0, BlockType.RAIL));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Detector rails depower after the minecart leaves")
    void detectorRailDepowersAfterMinecartLeaves() {
        World world = new World(5151L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.DETECTOR_RAIL, RailShapeResolver.EAST_WEST);
            world.setBlock(1, 69, 0, BlockType.STONE, 0);
            world.setBlock(1, 70, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);

            MinecartEntity cart = new MinecartEntity(0.5f, 70.1f, 0.5f, MinecartEntity.CartKind.RIDEABLE);
            world.replaceEntities(java.util.List.of(cart));
            world.updateEntities(1.0f / 20.0f);
            world.advanceBlockTicks(1);

            assertTrue((world.getBlockMetadata(0, 70, 0) & RedstoneEngine.RAIL_POWERED_BIT) != 0);

            cart.setPosition(1.5f, 70.1f, 0.5f);
            world.updateEntities(1.0f / 20.0f);
            world.advanceBlockTicks(RedstoneEngine.DETECTOR_RAIL_DELAY_TICKS + 1);

            assertEquals(0, world.getBlockMetadata(0, 70, 0) & RedstoneEngine.RAIL_POWERED_BIT);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Powered rails propagate activation eight connected rails from a redstone source")
    void poweredRailsPropagateActivationEightRails() {
        World world = new World(5150L);
        try {
            for (int x = 0; x <= 9; x++) {
                world.setBlock(x, 69, 0, BlockType.STONE, 0);
                world.setBlock(x, 70, 0, BlockType.POWERED_RAIL, RailShapeResolver.EAST_WEST);
            }
            world.setBlock(0, 69, -1, BlockType.STONE, 0);
            world.setBlock(0, 70, -1, BlockType.REDSTONE_TORCH_ON, 5);

            world.advanceBlockTicks(24);

            for (int x = 0; x <= 8; x++) {
                assertTrue((world.getBlockMetadata(x, 70, 0) & RedstoneEngine.RAIL_POWERED_BIT) != 0,
                        "rail " + x + " should be within the eight-rail propagation range");
            }
            assertEquals(0, world.getBlockMetadata(9, 70, 0) & RedstoneEngine.RAIL_POWERED_BIT);

            world.setBlock(0, 70, -1, BlockType.AIR, 0);
            world.advanceBlockTicks(24);

            for (int x = 0; x <= 9; x++) {
                assertEquals(0, world.getBlockMetadata(x, 70, 0) & RedstoneEngine.RAIL_POWERED_BIT,
                        "rail " + x + " should unpower after the source is removed");
            }
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Powered rails propagate activation across ascending slopes")
    void poweredRailsPropagateAcrossAscendingSlopes() {
        World world = new World(5154L);
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(1, 69, 0, BlockType.STONE, 0);
            world.setBlock(2, 70, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, BlockType.POWERED_RAIL, RailShapeResolver.EAST_WEST);
            world.setBlock(1, 70, 0, BlockType.POWERED_RAIL, RailShapeResolver.ASCENDING_EAST);
            world.setBlock(2, 71, 0, BlockType.POWERED_RAIL, RailShapeResolver.EAST_WEST);
            world.setBlock(0, 69, -1, BlockType.STONE, 0);
            world.setBlock(0, 70, -1, BlockType.REDSTONE_TORCH_ON, 5);

            world.advanceBlockTicks(24);

            assertEquals(RailShapeResolver.EAST_WEST, world.getBlockMetadata(0, 70, 0) & 7);
            assertEquals(RailShapeResolver.ASCENDING_EAST, world.getBlockMetadata(1, 70, 0) & 7);
            assertEquals(RailShapeResolver.EAST_WEST, world.getBlockMetadata(2, 71, 0) & 7);
            assertTrue((world.getBlockMetadata(0, 70, 0) & RedstoneEngine.RAIL_POWERED_BIT) != 0);
            assertTrue((world.getBlockMetadata(1, 70, 0) & RedstoneEngine.RAIL_POWERED_BIT) != 0);
            assertTrue((world.getBlockMetadata(2, 71, 0) & RedstoneEngine.RAIL_POWERED_BIT) != 0);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Mobs wake stone pressure plates when stepping onto them")
    void mobWakesStonePressurePlate() {
        World world = new World(5114L);
        try {
            world.setBlock(8, 99, 8, BlockType.STONE, 0);
            world.setBlock(8, 100, 8, BlockType.STONE_PRESSURE_PLATE, 0);
            world.advanceBlockTicks(2);
            assertEquals(0, world.getBlockMetadata(8, 100, 8));

            Zombie zombie = new Zombie();
            zombie.setPosition(8.5f, 100.0f, 8.5f);
            world.replaceEntities(java.util.List.of(zombie));

            world.updateEntities(1.0f / 20.0f);
            world.advanceBlockTicks(1);

            assertEquals(1, world.getBlockMetadata(8, 100, 8));

            world.replaceEntities(java.util.List.of());
            world.advanceBlockTicks(RedstoneEngine.PRESSURE_PLATE_DELAY_TICKS + 1);

            assertEquals(0, world.getBlockMetadata(8, 100, 8));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Player movement wakes stone pressure plates")
    void playerWakesStonePressurePlate() {
        World world = new World(5115L);
        try {
            world.setBlock(8, 99, 8, BlockType.STONE, 0);
            world.setBlock(8, 100, 8, BlockType.STONE_PRESSURE_PLATE, 0);
            world.advanceBlockTicks(2);
            Player player = new Player(8.5f, 100.0f, 8.5f);
            world.setPlayer(player);

            player.update(1.0f / 20.0f, world);
            world.advanceBlockTicks(1);

            assertEquals(1, world.getBlockMetadata(8, 100, 8));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dropped items wake wooden pressure plates but not stone pressure plates")
    void droppedItemsWakeWoodenPressurePlateOnly() {
        World world = new World(5116L);
        try {
            world.setBlock(8, 99, 8, BlockType.STONE, 0);
            world.setBlock(8, 100, 8, BlockType.WOODEN_PRESSURE_PLATE, 0);
            world.setBlock(10, 99, 8, BlockType.STONE, 0);
            world.setBlock(10, 100, 8, BlockType.STONE_PRESSURE_PLATE, 0);
            world.advanceBlockTicks(2);

            world.spawnThrownStack(8.5f, 100.1f, 8.5f, new ItemStack(ItemType.DIRT, 1), 0.0f, 0.0f, 0.0f);
            world.spawnThrownStack(10.5f, 100.1f, 8.5f, new ItemStack(ItemType.DIRT, 1), 0.0f, 0.0f, 0.0f);

            world.updateDroppedItems(1.0f / 20.0f);
            world.advanceBlockTicks(1);

            assertEquals(1, world.getBlockMetadata(8, 100, 8));
            assertEquals(0, world.getBlockMetadata(10, 100, 8));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Pressure plates should use the old inset scan box and 20 tick release cadence")
    void pressurePlatesUseSourceScanBoxAndReleaseDelay() {
        World world = new World(5144L);
        try {
            world.setBlock(8, 99, 8, BlockType.STONE, 0);
            world.setBlock(8, 100, 8, BlockType.WOODEN_PRESSURE_PLATE, 0);
            world.replaceDroppedItems(java.util.List.of(new DroppedItem(
                    8.0f, 100.1f, 8.5f, new ItemStack(ItemType.DIRT, 1), 0.0f, 0.0f, 0.0f)));

            world.scheduleBlockTick(8, 100, 8, BlockType.WOODEN_PRESSURE_PLATE, 0);
            world.advanceBlockTicks(1);

            assertEquals(0, world.getBlockMetadata(8, 100, 8),
                    "The old 1/8 inset scan should ignore item boxes that only touch its boundary");

            world.replaceDroppedItems(java.util.List.of(new DroppedItem(
                    8.01f, 100.1f, 8.5f, new ItemStack(ItemType.DIRT, 1), 0.0f, 0.0f, 0.0f)));
            world.scheduleBlockTick(8, 100, 8, BlockType.WOODEN_PRESSURE_PLATE, 0);
            world.advanceBlockTicks(1);

            assertEquals(1, world.getBlockMetadata(8, 100, 8));

            world.replaceDroppedItems(java.util.List.of());
            world.advanceBlockTicks(RedstoneEngine.PRESSURE_PLATE_DELAY_TICKS - 1);
            assertEquals(1, world.getBlockMetadata(8, 100, 8),
                    "Release 1.0 plates should stay pressed until the 20 tick rescan");

            world.advanceBlockTicks(1);

            assertEquals(0, world.getBlockMetadata(8, 100, 8));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Arrows should wake wooden pressure plates but not stone pressure plates")
    void arrowsWakeWoodenPressurePlateOnly() {
        World world = new World(5141L);
        try {
            world.setBlock(8, 99, 8, BlockType.STONE, 0);
            world.setBlock(8, 100, 8, BlockType.WOODEN_PRESSURE_PLATE, 0);
            world.setBlock(10, 99, 8, BlockType.STONE, 0);
            world.setBlock(10, 100, 8, BlockType.STONE_PRESSURE_PLATE, 0);
            world.replaceEntities(java.util.List.of(
                    new ArrowEntity(8.5f, 100.0f, 8.5f, 0.1f, 0.0f, 0.0f, null, false, 2.0f),
                    new ArrowEntity(10.5f, 100.0f, 8.5f, 0.1f, 0.0f, 0.0f, null, false, 2.0f)));

            world.scheduleBlockTick(8, 100, 8, BlockType.WOODEN_PRESSURE_PLATE, 0);
            world.scheduleBlockTick(10, 100, 8, BlockType.STONE_PRESSURE_PLATE, 0);
            world.advanceBlockTicks(1);

            assertEquals(1, world.getBlockMetadata(8, 100, 8));
            assertEquals(0, world.getBlockMetadata(10, 100, 8));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dispenser consumes one slot item and fires arrows when powered")
    void dispenserFiresArrow() {
        World world = new World(5103L);
        try {
            world.setBlock(0, 100, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 100, 0);
            dispenser.getInventory()[0] = new ItemStack(ItemType.ARROW, 2);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(4);
            world.updateEntities(1.0f / 20.0f);

            assertEquals(1, dispenser.getInventory()[0].getCount());
            assertTrue(world.getEntities().stream().anyMatch(entity -> entity instanceof ArrowEntity));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dispensers use Release 1.0 quasi-connectivity from the block space above")
    void dispenserUsesQuasiConnectivityFromBlockAbove() {
        World world = new World(5144L);
        try {
            int leverMetadata = BlockShape.leverMetadataFromFace(Block.FACE_EAST)
                    | RedstoneEngine.POWERED_BIT;
            world.setBlock(-1, 101, 0, BlockType.LEVER, leverMetadata);
            world.setBlock(0, 100, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 100, 0);
            dispenser.getInventory()[0] = new ItemStack(ItemType.ARROW, 1);

            world.advanceBlockTicks(4);

            assertNull(dispenser.getInventory()[0]);
            assertTrue((world.getBlockMetadata(0, 100, 0) & RedstoneEngine.POWERED_BIT) != 0);
            assertTrue(world.hasEntityOfType(ArrowEntity.class));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Redstone-triggered dispensers should wait the old four tick activation delay")
    void dispenserRedstoneActivationUsesFourTickDelay() {
        World world = new World(5145L);
        try {
            world.setBlock(0, 100, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 100, 0);
            dispenser.getInventory()[0] = new ItemStack(ItemType.DIRT, 1);
            world.setBlock(-1, 99, 0, BlockType.STONE, 0);
            world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);

            world.advanceBlockTicks(RedstoneEngine.DISPENSER_DELAY_TICKS - 1);

            assertNotNull(dispenser.getInventory()[0]);
            assertTrue(world.getDroppedItems().isEmpty());

            world.advanceBlockTicks(1);

            assertNull(dispenser.getInventory()[0]);
            assertEquals(1, world.getDroppedItems().size());
            assertSame(ItemType.DIRT, world.getDroppedItems().get(0).getItemType());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Successful dispensers should emit the old directional smoke puff")
    void dispenserSuccessfulActivationEmitsDirectionalSmokePuff() {
        World world = new World(5147L);
        try {
            world.setBlock(0, 100, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 100, 0);
            dispenser.getInventory()[0] = new ItemStack(ItemType.DIRT, 1);

            dispenser.dispense(world);

            assertEquals(10, world.getParticles().stream()
                    .filter(particle -> particle.getType() == WorldParticle.Type.SMOKE)
                    .count());
            float totalOutwardMotion = 0.0f;
            for (WorldParticle particle : world.getParticles()) {
                if (particle.getType() != WorldParticle.Type.SMOKE) {
                    continue;
                }
                assertEquals(1.11f, particle.getRenderX(0.0f), 0.0001f);
                assertTrue(particle.getRenderY(0.0f) >= 100.25f);
                assertTrue(particle.getRenderY(0.0f) <= 100.75f);
                assertTrue(particle.getRenderZ(0.0f) >= 0.25f);
                assertTrue(particle.getRenderZ(0.0f) <= 0.75f);
                assertTrue(particle.getMotionY() < 0.01f);
                assertTrue(Math.abs(particle.getMotionZ()) < 0.05f);
                totalOutwardMotion += particle.getMotionX();
            }
            assertTrue(totalOutwardMotion > 0.4f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Empty dispensers should not emit the aux smoke puff")
    void emptyDispenserActivationDoesNotEmitSmokePuff() {
        World world = new World(5148L);
        try {
            world.setBlock(0, 100, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 100, 0);

            dispenser.dispense(world);

            assertTrue(world.getParticles().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dispenser throws eggs and snowballs as projectiles")
    void dispenserThrowsEggsAndSnowballs() {
        assertDispenserThrowsItemProjectile(ItemType.SNOWBALL, 5110L);
        assertDispenserThrowsItemProjectile(ItemType.EGG, 5111L);
    }

    private static void assertDispenserThrowsItemProjectile(ItemType itemType, long seed) {
        World world = new World(seed);
        try {
            world.setBlock(0, 120, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 120, 0);
            dispenser.getInventory()[0] = new ItemStack(itemType, 1);
            for (int x = 1; x <= 4; x++) {
                world.setBlock(x, 120, 0, BlockType.AIR, 0);
            }

            dispenser.dispense(world);
            assertTrue(world.hasEntityOfType(ThrownItemEntity.class));
            world.updateEntities(1.0f / 20.0f);

            ThrownItemEntity projectile = (ThrownItemEntity) world.getEntities().stream()
                    .filter(entity -> entity instanceof ThrownItemEntity)
                    .findFirst()
                    .orElseThrow();
            assertEquals(itemType, projectile.getItemType());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dispenser throws splash potions with potion data")
    void dispenserThrowsSplashPotion() {
        World world = new World(5112L);
        try {
            world.setBlock(0, 120, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 120, 0);
            ItemStack potion = new ItemStack(ItemType.POTION, 1);
            potion.setPotionData(new PotionData(PotionType.POISON, true, false, false));
            dispenser.getInventory()[0] = potion;
            for (int x = 1; x <= 4; x++) {
                world.setBlock(x, 120, 0, BlockType.AIR, 0);
            }

            dispenser.dispense(world);
            assertTrue(world.hasEntityOfType(SplashPotionEntity.class));
            world.updateEntities(1.0f / 20.0f);

            SplashPotionEntity projectile = (SplashPotionEntity) world.getEntities().stream()
                    .filter(entity -> entity instanceof SplashPotionEntity)
                    .findFirst()
                    .orElseThrow();
            assertEquals(PotionType.POISON, projectile.getPotionData().type());
            assertTrue(projectile.getPotionData().splash());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 dispenser ejects TNT as an item instead of priming it")
    void dispenserEjectsTntInsteadOfPriming() {
        World world = new World(5113L);
        try {
            world.setBlock(0, 100, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 100, 0);
            dispenser.getInventory()[0] = new ItemStack(ItemType.TNT, 1);

            dispenser.dispense(world);
            world.updateEntities(1.0f / 20.0f);

            assertTrue(world.getEntities().stream().noneMatch(entity -> entity instanceof PrimedTntEntity));
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.TNT && item.getCount() == 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 dispenser ejects minecart items even when rails are in front")
    void dispenserEjectsMinecartItemsEvenWhenRailIsInFront() {
        assertDispenserEjectsVehicleItem(ItemType.MINECART, 5117L);
        assertDispenserEjectsVehicleItem(ItemType.CHEST_MINECART, 5118L);
        assertDispenserEjectsVehicleItem(ItemType.FURNACE_MINECART, 5119L);
    }

    @Test
    @DisplayName("Release 1.0 dispenser ejects boat items even when water is in front")
    void dispenserEjectsBoatEvenWhenWaterIsInFront() {
        World world = new World(5121L);
        try {
            world.setBlock(0, 100, 0, BlockType.DISPENSER, Block.FACE_EAST);
            world.setBlock(1, 100, 0, BlockType.WATER, 0);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 100, 0);
            dispenser.getInventory()[0] = new ItemStack(ItemType.BOAT, 1);

            dispenser.dispense(world);
            world.updateEntities(1.0f / 20.0f);

            assertNull(dispenser.getInventory()[0]);
            assertTrue(world.getEntities().stream().noneMatch(BoatEntity.class::isInstance));
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == ItemType.BOAT && item.getCount() == 1));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 dispenser generic item ejection should use old offset and spread")
    void dispenserGenericItemEjectionUsesReleaseStyleOffsetAndSpread() {
        World world = new World(5114L);
        try {
            world.setBlock(0, 100, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 100, 0);
            dispenser.getInventory()[0] = new ItemStack(ItemType.DIRT, 1);

            dispenser.dispense(world);

            assertEquals(1, world.getDroppedItems().size());
            assertSame(ItemType.DIRT, world.getDroppedItems().get(0).getItemType());
            assertEquals(1.1f, world.getDroppedItems().get(0).getX(), 0.0001f);
            assertEquals(100.2f, world.getDroppedItems().get(0).getY(), 0.0001f);
            assertEquals(0.5f, world.getDroppedItems().get(0).getZ(), 0.0001f);

            float[] motion = RedstoneEngine.dispenserGenericItemMotion(Block.FACE_EAST, new Random(5114L));
            assertTrue(motion[0] > 0.15f && motion[0] < 0.35f);
            assertTrue(motion[1] > 0.05f && motion[1] < 0.35f);
            assertTrue(Math.abs(motion[2]) > 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dispenser slot choice and spread should come from the world seed")
    void dispenserSlotChoiceAndSpreadUseWorldSeed() {
        assertEquals(dispenserRandomSnapshot(5115L), dispenserRandomSnapshot(5115L));
        assertEquals(dispenserProjectileRandomSnapshot(5116L), dispenserProjectileRandomSnapshot(5116L));
    }

    @Test
    @DisplayName("Dispenser slot choice should consume Release-style reservoir RNG calls")
    void dispenserSlotChoiceUsesSourceReservoirSampling() {
        DispenserSelectionRandom random = new DispenserSelectionRandom();
        World world = new RandomOverrideWorld(5146L, random);
        try {
            world.setBlock(0, 100, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 100, 0);
            dispenser.getInventory()[0] = new ItemStack(ItemType.DIRT, 1);
            dispenser.getInventory()[4] = new ItemStack(ItemType.COBBLESTONE, 1);
            dispenser.getInventory()[8] = new ItemStack(ItemType.GRAVEL, 1);

            dispenser.dispense(world);

            assertEquals(3, random.nextIntCalls());
            assertEquals(1, random.boundAt(0));
            assertEquals(2, random.boundAt(1));
            assertEquals(3, random.boundAt(2));
            assertNotNull(dispenser.getInventory()[0]);
            assertNotNull(dispenser.getInventory()[4]);
            assertNull(dispenser.getInventory()[8]);
            assertEquals(1, world.getDroppedItems().size());
            assertSame(ItemType.GRAVEL, world.getDroppedItems().get(0).getItemType());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Release 1.0 dispenser output should stay horizontally faced")
    void dispenserOutputIgnoresVerticalMetadata() {
        assertEquals(Block.FACE_NORTH, RedstoneEngine.metadataToOutputFace(Block.FACE_TOP));
        assertEquals(Block.FACE_NORTH, RedstoneEngine.metadataToOutputFace(Block.FACE_BOTTOM));
        assertEquals(Block.FACE_NORTH,
                RedstoneEngine.metadataToOutputFace(Block.FACE_TOP | RedstoneEngine.POWERED_BIT));
        assertEquals(Block.FACE_SOUTH, RedstoneEngine.metadataToOutputFace(Block.FACE_SOUTH));
        assertEquals(Block.FACE_EAST, RedstoneEngine.metadataToOutputFace(Block.FACE_EAST));
        assertEquals(Block.FACE_WEST, RedstoneEngine.metadataToOutputFace(Block.FACE_WEST));
    }

    private static void assertDispenserEjectsVehicleItem(ItemType itemType, long seed) {
        World world = new World(seed);
        try {
            world.setBlock(0, 100, 0, BlockType.DISPENSER, Block.FACE_EAST);
            world.setBlock(1, 99, 0, BlockType.STONE, 0);
            world.setBlock(1, 100, 0, BlockType.RAIL, RailShapeResolver.EAST_WEST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 100, 0);
            dispenser.getInventory()[0] = new ItemStack(itemType, 1);

            dispenser.dispense(world);
            world.updateEntities(1.0f / 20.0f);

            assertNull(dispenser.getInventory()[0]);
            assertTrue(world.getEntities().stream().noneMatch(MinecartEntity.class::isInstance));
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == itemType && item.getCount() == 1));
        } finally {
            world.cleanup();
        }
    }

    private static DispenserRandomSnapshot dispenserRandomSnapshot(long seed) {
        World world = new World(seed);
        try {
            world.setBlock(0, 100, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 100, 0);
            for (int slot = 0; slot < DispenserTileEntity.SIZE; slot++) {
                dispenser.getInventory()[slot] = new ItemStack(ItemType.DIRT, 1);
            }

            dispenser.dispense(world);

            int emptiedSlot = -1;
            for (int slot = 0; slot < DispenserTileEntity.SIZE; slot++) {
                if (dispenser.getInventory()[slot] == null) {
                    emptiedSlot = slot;
                    break;
                }
            }
            assertTrue(emptiedSlot >= 0);
            assertEquals(1, world.getDroppedItems().size());
            DroppedItem dropped = world.getDroppedItems().get(0);
            return new DispenserRandomSnapshot(emptiedSlot,
                    dropped.getVelocityX(), dropped.getVelocityY(), dropped.getVelocityZ());
        } finally {
            world.cleanup();
        }
    }

    private static DispenserRandomSnapshot dispenserProjectileRandomSnapshot(long seed) {
        World world = new World(seed);
        try {
            world.setBlock(0, 100, 0, BlockType.DISPENSER, Block.FACE_EAST);
            DispenserTileEntity dispenser = (DispenserTileEntity) world.getTileEntity(0, 100, 0);
            for (int slot = 0; slot < DispenserTileEntity.SIZE; slot++) {
                dispenser.getInventory()[slot] = new ItemStack(ItemType.ARROW, 1);
            }

            dispenser.dispense(world);
            world.updateEntities(0.0f);

            int emptiedSlot = -1;
            for (int slot = 0; slot < DispenserTileEntity.SIZE; slot++) {
                if (dispenser.getInventory()[slot] == null) {
                    emptiedSlot = slot;
                    break;
                }
            }
            assertTrue(emptiedSlot >= 0);
            ArrowEntity arrow = (ArrowEntity) world.getEntities().stream()
                    .filter(entity -> entity instanceof ArrowEntity)
                    .findFirst()
                    .orElseThrow();
            return new DispenserRandomSnapshot(emptiedSlot,
                    arrow.getMotionX(), arrow.getMotionY(), arrow.getMotionZ());
        } finally {
            world.cleanup();
        }
    }

    private record DispenserRandomSnapshot(int emptiedSlot, float velocityX, float velocityY, float velocityZ) {
    }

    private static final class DispenserSelectionRandom extends Random {
        private final int[] bounds = new int[8];
        private int nextIntCalls;

        @Override
        public int nextInt(int bound) {
            bounds[nextIntCalls++] = bound;
            return 0;
        }

        @Override
        public double nextDouble() {
            return 0.5D;
        }

        @Override
        public double nextGaussian() {
            return 0.0D;
        }

        private int nextIntCalls() {
            return nextIntCalls;
        }

        private int boundAt(int index) {
            return bounds[index];
        }
    }

    private static final class FixedFloatRandom extends Random {
        private final float value;

        private FixedFloatRandom(float value) {
            this.value = value;
        }

        @Override
        public float nextFloat() {
            return value;
        }
    }

    private static final class ZeroIntRandom extends Random {
        @Override
        public int nextInt(int bound) {
            return 0;
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

    private static void assertPistonBreaksBlockIntoDrop(BlockType blockType, ItemType itemType) {
        assertPistonBreaksBlockIntoDrop(blockType, itemType, false);
    }

    private static void assertPistonBreaksBlockIntoDrop(BlockType blockType, ItemType itemType,
            boolean supportBelow) {
        World world = new World(5130L + blockType.getId());
        try {
            setupPoweredEastPiston(world);
            if (supportBelow) {
                world.setBlock(1, 99, 0, BlockType.STONE, 0);
            }
            world.setBlock(1, 100, 0, blockType, 0);

            world.advanceBlockTicks(4);

            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(2, 100, 0));
            assertTrue(world.getDroppedItems().stream()
                    .anyMatch(item -> item.getItemType() == itemType && item.getCount() >= 1));
        } finally {
            world.cleanup();
        }
    }

    private static void assertPistonRefusesSign(BlockType signType, int metadata) {
        World world = new World(5180L + signType.getId());
        try {
            setupPoweredEastPiston(world);
            if (signType == BlockType.WALL_SIGN) {
                world.setBlock(2, 100, 0, BlockType.STONE, 0);
            }
            world.setBlock(1, 100, 0, signType, metadata);
            assertInstanceOf(SignTileEntity.class, world.getTileEntity(1, 100, 0));

            world.advanceBlockTicks(4);

            assertEquals(0, world.getBlockMetadata(0, 100, 0) & RedstoneEngine.PISTON_EXTENDED_BIT);
            assertSame(signType, world.getBlock(1, 100, 0));
            assertInstanceOf(SignTileEntity.class, world.getTileEntity(1, 100, 0));
        } finally {
            world.cleanup();
        }
    }

    private static void assertPistonMovesSupportedRail(BlockType railType, ItemType itemType) {
        World world = new World(5160L + railType.getId());
        try {
            setupPoweredEastPiston(world);
            world.setBlock(1, 99, 0, BlockType.STONE, 0);
            world.setBlock(2, 99, 0, BlockType.STONE, 0);
            world.setBlock(1, 100, 0, railType, RailShapeResolver.EAST_WEST);

            world.advanceBlockTicks(4);

            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertSame(railType, world.getBlock(2, 100, 0));
            assertEquals(0, droppedCount(world, itemType));
        } finally {
            world.cleanup();
        }
    }

    private static void assertPistonPopsUnsupportedRail(BlockType railType, ItemType itemType) {
        World world = new World(5170L + railType.getId());
        try {
            setupPoweredEastPiston(world);
            world.setBlock(1, 99, 0, BlockType.STONE, 0);
            world.setBlock(2, 99, 0, BlockType.AIR, 0);
            world.setBlock(1, 100, 0, railType, RailShapeResolver.EAST_WEST);

            world.advanceBlockTicks(4);

            assertSame(BlockType.PISTON_HEAD, world.getBlock(1, 100, 0));
            assertSame(BlockType.AIR, world.getBlock(2, 100, 0));
            assertTrue(droppedCount(world, itemType) >= 1);
        } finally {
            world.cleanup();
        }
    }

    private static void assertAscendingRailPopsWithoutRaisedSupport(BlockType railType, ItemType itemType) {
        World world = new World(5190L + railType.getId());
        try {
            world.setBlock(0, 69, 0, BlockType.STONE, 0);
            world.setBlock(1, 70, 0, BlockType.STONE, 0);
            world.setBlock(0, 70, 0, railType, RailShapeResolver.ASCENDING_EAST);

            assertSame(railType, world.getBlock(0, 70, 0));

            world.setBlock(1, 70, 0, BlockType.AIR, 0);

            assertSame(BlockType.AIR, world.getBlock(0, 70, 0));
            assertTrue(droppedCount(world, itemType) >= 1);
        } finally {
            world.cleanup();
        }
    }

    private static void setupFourWayRailJunction(World world, int x, int y, int z) {
        setRailWithSupport(world, x, y, z, RailShapeResolver.NORTH_SOUTH);
        setRailWithSupport(world, x, y, z - 1, RailShapeResolver.NORTH_SOUTH);
        setRailWithSupport(world, x, y, z + 1, RailShapeResolver.NORTH_SOUTH);
        setRailWithSupport(world, x - 1, y, z, RailShapeResolver.EAST_WEST);
        setRailWithSupport(world, x + 1, y, z, RailShapeResolver.EAST_WEST);
    }

    private static void setRailWithSupport(World world, int x, int y, int z, int metadata) {
        world.setBlock(x, y - 1, z, BlockType.STONE, 0);
        world.setBlock(x, y, z, BlockType.RAIL, metadata);
    }

    private static int droppedCount(World world, ItemType type) {
        return world.getDroppedItems().stream()
                .filter(item -> item.getItemType() == type)
                .mapToInt(item -> item.getCount())
                .sum();
    }

    private static void clearExplosionCorridor(World world, int maxX) {
        for (int x = 0; x <= maxX; x++) {
            for (int y = 69; y <= 72; y++) {
                for (int z = -1; z <= 1; z++) {
                    world.setBlock(x, y, z, BlockType.AIR, 0);
                }
            }
        }
    }

    private static float releaseExplosionDamage(float power, float distance, float exposure) {
        float entityRadius = power * 2.0f;
        float impact = Math.max(0.0f, 1.0f - distance / entityRadius) * exposure;
        return ((impact * impact + impact) * 0.5f * 8.0f * entityRadius) + 1.0f;
    }

    private static void setupPoweredEastPiston(World world) {
        world.setBlock(0, 100, 0, BlockType.PISTON, Block.FACE_EAST);
        world.setBlock(2, 100, 0, BlockType.AIR, 0);
        world.setBlock(-1, 99, 0, BlockType.STONE, 0);
        world.setBlock(-1, 100, 0, BlockType.LEVER, 5 | RedstoneEngine.POWERED_BIT);
    }

    private static void powerPistonFromWest(World world, int x, int y, int z) {
        world.setBlock(x - 1, y, z, BlockType.LEVER,
                BlockShape.leverMetadataFromFace(Block.FACE_WEST) | RedstoneEngine.POWERED_BIT);
    }

    private static final class InvulnerableEndCrystal extends EndCrystalEntity {
        private InvulnerableEndCrystal(float x, float y, float z) {
            super(x, y, z);
        }

        private void primeLivingInvulnerability(int ticks, float lastDamage) {
            this.invulnerableTime = ticks;
            this.lastDamageAmount = lastDamage;
        }
    }
}
