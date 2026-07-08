package com.craftzero.world.tile;

import com.craftzero.world.World;
import com.craftzero.world.BlockType;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.WorldSoundEvent;

public class NoteBlockTileEntity extends TileEntity {
    public static final int INSTRUMENT_HARP = 0;
    public static final int INSTRUMENT_BASS_DRUM = 1;
    public static final int INSTRUMENT_SNARE = 2;
    public static final int INSTRUMENT_STICKS = 3;
    public static final int INSTRUMENT_BASS = 4;
    public static final float NOTE_PARTICLE_UPWARD_MOTION = 0.2f;
    public static final float NOTE_PARTICLE_SCALE = 0.3f;
    public static final int NOTE_PARTICLE_LIFETIME_TICKS = 6;

    private int pitch;
    private int playTicks;
    private int lastInstrument;

    public NoteBlockTileEntity(int x, int y, int z) {
        super(x, y, z);
        this.lastInstrument = INSTRUMENT_HARP;
    }

    @Override
    public String getTypeId() {
        return "note_block";
    }

    public int getPitch() {
        return pitch;
    }

    public void setPitch(int pitch) {
        this.pitch = Math.floorMod(pitch, 25);
        markDirty();
    }

    public int getPlayTicks() {
        return playTicks;
    }

    public void setPlayTicks(int playTicks) {
        this.playTicks = Math.max(0, playTicks);
        markDirty();
    }

    public int getLastInstrument() {
        return lastInstrument;
    }

    public void setLastInstrument(int lastInstrument) {
        this.lastInstrument = Math.max(INSTRUMENT_HARP, Math.min(INSTRUMENT_BASS, lastInstrument));
        markDirty();
    }

    public void cyclePitch() {
        setPitch(pitch + 1);
    }

    public boolean play(World world) {
        if (world == null) {
            return false;
        }
        BlockPos pos = getPos();
        if (!world.getBlockIfLoaded(pos.x(), pos.y() + 1, pos.z(), BlockType.AIR).isAir()) {
            return false;
        }
        lastInstrument = instrumentFor(world.getBlockIfLoaded(pos.x(), pos.y() - 1, pos.z(), BlockType.AIR));
        playTicks++;
        world.playSound(WorldSoundEvent.noteSoundId(lastInstrument),
                pos.x() + 0.5f, pos.y() + 0.5f, pos.z() + 0.5f,
                3.0f, WorldSoundEvent.notePitch(pitch));
        world.spawnParticle(WorldParticle.Type.NOTE,
                pos.x() + 0.5f, pos.y() + 1.2f, pos.z() + 0.5f,
                0.0f, NOTE_PARTICLE_UPWARD_MOTION, 0.0f,
                NOTE_PARTICLE_SCALE, NOTE_PARTICLE_LIFETIME_TICKS,
                pitch / 24.0f);
        markDirty();
        return true;
    }

    public static int instrumentFor(BlockType below) {
        if (isStoneMaterial(below)) {
            return INSTRUMENT_BASS_DRUM;
        }
        if (below == BlockType.SAND || below == BlockType.GRAVEL || below == BlockType.SOUL_SAND) {
            return INSTRUMENT_SNARE;
        }
        if (below == BlockType.GLASS || below == BlockType.GLASS_PANE || below == BlockType.GLOWSTONE) {
            return INSTRUMENT_STICKS;
        }
        if (isWoodMaterial(below)) {
            return INSTRUMENT_BASS;
        }
        return INSTRUMENT_HARP;
    }

    private static boolean isStoneMaterial(BlockType type) {
        return switch (type) {
            case STONE, COBBLESTONE, BEDROCK, GOLD_ORE, IRON_ORE, COAL_ORE, LAPIS_ORE,
                    DIAMOND_ORE, MOSSY_COBBLESTONE, OBSIDIAN,
                    DISPENSER, SANDSTONE, STONE_PRESSURE_PLATE,
                    DOUBLE_STONE_SLAB, STONE_SLAB, BRICK, MOB_SPAWNER, FURNACE, LIT_FURNACE,
                    COBBLESTONE_STAIRS, REDSTONE_ORE, GLOWING_REDSTONE_ORE, NETHERRACK,
                    STONE_BRICK, BRICK_STAIRS, STONE_BRICK_STAIRS,
                    NETHER_BRICK, NETHER_BRICK_FENCE, NETHER_BRICK_STAIRS, ENCHANTING_TABLE,
                    END_PORTAL_FRAME, END_STONE -> true;
            default -> false;
        };
    }

    private static boolean isWoodMaterial(BlockType type) {
        return switch (type) {
            case OAK_PLANKS, OAK_LOG, NOTE_BLOCK, BOOKSHELF, OAK_STAIRS, CHEST, CRAFTING_TABLE,
                    STANDING_SIGN, WALL_SIGN, WOODEN_DOOR, WOODEN_PRESSURE_PLATE, TRAPDOOR, JUKEBOX,
                    FENCE, FENCE_GATE,
                    BROWN_MUSHROOM_BLOCK, RED_MUSHROOM_BLOCK -> true;
            default -> false;
        };
    }
}
