package com.craftzero.world;

import com.craftzero.inventory.ItemType;
import com.craftzero.world.tile.NoteBlockTileEntity;

import java.util.Locale;
import java.util.Random;

/**
 * Transient Release-era sound cue emitted by world logic. Audio playback can
 * drain these events without coupling simulation code to a concrete mixer.
 */
public record WorldSoundEvent(String soundId, float x, float y, float z, float volume, float pitch) {
    public static final float MAX_SOURCE_COORDINATE = 30_000_000.0f;
    public static final float MAX_SOUND_VOLUME = 10_000.0f;
    public static final float MAX_SOUND_PITCH = 4.0f;
    public static final String RECORD_STOP = "craftzero.records.stop";
    public static final String DOOR_OPEN = "random.door_open";
    public static final String DOOR_CLOSE = "random.door_close";
    public static final String CHEST_OPEN = "random.chestopen";
    public static final String CHEST_CLOSE = "random.chestclosed";
    public static final String NOTE_HARP = "note.harp";
    public static final String NOTE_BASS_DRUM = "note.bd";
    public static final String NOTE_SNARE = "note.snare";
    public static final String NOTE_STICKS = "note.hat";
    public static final String NOTE_BASS = "note.bassattack";
    public static final String RECORD_13 = "records.13";
    public static final String RECORD_CAT = "records.cat";
    public static final String RECORD_BLOCKS = "records.blocks";
    public static final String RECORD_CHIRP = "records.chirp";
    public static final String RECORD_FAR = "records.far";
    public static final String RECORD_MALL = "records.mall";
    public static final String RECORD_MELLOHI = "records.mellohi";
    public static final String RECORD_STAL = "records.stal";
    public static final String RECORD_STRAD = "records.strad";
    public static final String RECORD_WARD = "records.ward";
    public static final String RECORD_11 = "records.11";
    public static final String RECORD_EJECT = "random.pop";
    public static final String ITEM_PICKUP = "random.pop";
    public static final String XP_PICKUP = "random.orb";
    public static final String XP_LEVEL_UP = "random.levelup";
    public static final String FISHING_SPLASH = "random.splash";
    public static final String PISTON_EXTEND = "tile.piston.out";
    public static final String PISTON_RETRACT = "tile.piston.in";
    public static final String REDSTONE_CLICK = "random.click";
    public static final String UI_BUTTON_CLICK = "random.click";
    public static final String FIZZ = "random.fizz";
    public static final String REDSTONE_TORCH_BURNOUT = FIZZ;
    public static final String DISPENSER_CLICK = "random.click";
    public static final String FUSE = "random.fuse";
    public static final String TNT_FUSE = FUSE;
    public static final String CREEPER_FUSE = FUSE;
    public static final String EXPLOSION = "random.explode";
    public static final String BOW = "random.bow";
    public static final String FIRE_IGNITE = "fire.ignite";
    public static final String DIG_STONE = "dig.stone";
    public static final String DIG_WOOD = "dig.wood";
    public static final String DIG_GRAVEL = "dig.gravel";
    public static final String DIG_GRASS = "dig.grass";
    public static final String DIG_CLOTH = "dig.cloth";
    public static final String DIG_SAND = "dig.sand";
    public static final String DIG_SNOW = "dig.snow";
    public static final String DIG_LADDER = "dig.ladder";
    public static final String DIG_METAL = "dig.stone";
    public static final String STEP_STONE = "step.stone";
    public static final String STEP_WOOD = "step.wood";
    public static final String STEP_GRAVEL = "step.gravel";
    public static final String STEP_GRASS = "step.grass";
    public static final String STEP_CLOTH = "step.cloth";
    public static final String STEP_SAND = "step.sand";
    public static final String STEP_SNOW = "step.snow";
    public static final String STEP_LADDER = "step.ladder";
    public static final String GLASS_BREAK = "random.glass";
    public static final String EAT = "random.eat";
    public static final String DRINK = "random.drink";
    public static final String BURP = "random.burp";
    public static final String PLAYER_HURT = "random.hurt";
    public static final String FALL_SMALL = "damage.fallsmall";
    public static final String FALL_BIG = "damage.fallbig";
    public static final String CHICKEN_PLOP = "mob.chicken.plop";
    public static final String CHICKEN_IDLE = "mob.chicken";
    public static final String COW_IDLE = "mob.cow";
    public static final String COW_HURT = "mob.cowhurt";
    public static final String COW_DEATH = COW_HURT;
    public static final String PIG_IDLE = "mob.pig";
    public static final String PIG_HURT = PIG_IDLE;
    public static final String PIG_DEATH = "mob.pigdeath";
    public static final String SHEEP_IDLE = "mob.sheep";
    public static final String SHEEP_HURT = SHEEP_IDLE;
    public static final String SHEEP_DEATH = SHEEP_IDLE;
    public static final String CHICKEN_HURT = "mob.chickenhurt";
    public static final String CHICKEN_DEATH = CHICKEN_HURT;
    public static final String ZOMBIE_IDLE = "mob.zombie";
    public static final String ZOMBIE_HURT = "mob.zombiehurt";
    public static final String ZOMBIE_DEATH = "mob.zombiedeath";
    public static final String ZOMBIE_PIGMAN_IDLE = "mob.zombiepig.zpig";
    public static final String ZOMBIE_PIGMAN_HURT = "mob.zombiepig.zpighurt";
    public static final String ZOMBIE_PIGMAN_DEATH = "mob.zombiepig.zpigdeath";
    public static final String ZOMBIE_PIGMAN_ANGRY = "mob.zombiepig.zpigangry";
    public static final String SKELETON_IDLE = "mob.skeleton";
    public static final String SKELETON_HURT = "mob.skeletonhurt";
    public static final String SKELETON_DEATH = SKELETON_HURT;
    public static final String CREEPER_HURT = "mob.creeper";
    public static final String CREEPER_DEATH = "mob.creeperdeath";
    public static final String SPIDER_IDLE = "mob.spider";
    public static final String SPIDER_HURT = "mob.spider";
    public static final String SPIDER_DEATH = "mob.spiderdeath";
    public static final String WOLF_BARK = "mob.wolf.bark";
    public static final String WOLF_GROWL = "mob.wolf.growl";
    public static final String WOLF_WHINE = "mob.wolf.whine";
    public static final String WOLF_PANTING = "mob.wolf.panting";
    public static final String WOLF_HURT = "mob.wolf.hurt";
    public static final String WOLF_DEATH = "mob.wolf.death";
    public static final String WOLF_SHAKE = "mob.wolf.shake";
    public static final String SLIME = "mob.slime";
    public static final String SLIME_ATTACK = "mob.slimeattack";
    public static final String MAGMA_CUBE_BIG = "mob.magmacube.big";
    public static final String MAGMA_CUBE_SMALL = "mob.magmacube.small";
    public static final String MAGMA_CUBE_JUMP = "mob.magmacube.jump";
    public static final String BLAZE_BREATHE = "mob.blaze.breathe";
    public static final String BLAZE_HURT = "mob.blaze.hit";
    public static final String BLAZE_DEATH = "mob.blaze.death";
    public static final String SILVERFISH_IDLE = "mob.silverfish.say";
    public static final String SILVERFISH_HURT = "mob.silverfish.hit";
    public static final String SILVERFISH_DEATH = "mob.silverfish.kill";
    public static final String WEATHER_THUNDER = "ambient.weather.thunder";
    public static final String WEATHER_RAIN = "ambient.weather.rain";
    public static final String AMBIENT_CAVE = "ambient.cave.cave";
    public static final String PORTAL_AMBIENT = "portal.portal";
    public static final String ENDER_DRAGON_DEATH = "mob.enderdragon.end";
    public static final String ENDERMAN_IDLE = "mob.endermen.idle";
    public static final String ENDERMAN_SCREAM = "mob.endermen.scream";
    public static final String ENDERMAN_STARE = "mob.endermen.stare";
    public static final String ENDERMAN_HURT = "mob.endermen.hit";
    public static final String ENDERMAN_DEATH = "mob.endermen.death";
    public static final String ENDERMAN_TELEPORT = "mob.endermen.portal";
    public static final String GHAST_IDLE = "mob.ghast.moan";
    public static final String GHAST_HURT = "mob.ghast.scream";
    public static final String GHAST_DEATH = "mob.ghast.death";
    public static final String GHAST_CHARGE = "mob.ghast.charge";
    public static final String GHAST_FIREBALL = "mob.ghast.fireball";
    public static final String MUSIC_CALM1 = "music.calm1";
    public static final String MUSIC_CALM2 = "music.calm2";
    public static final String MUSIC_CALM3 = "music.calm3";
    public static final String MUSIC_HAL1 = "music.hal1";
    public static final String MUSIC_HAL2 = "music.hal2";
    public static final String MUSIC_HAL3 = "music.hal3";
    public static final String MUSIC_HAL4 = "music.hal4";
    public static final String MUSIC_NUANCE1 = "music.nuance1";
    public static final String MUSIC_NUANCE2 = "music.nuance2";
    public static final String MUSIC_PIANO1 = "music.piano1";
    public static final String MUSIC_PIANO2 = "music.piano2";
    public static final String MUSIC_PIANO3 = "music.piano3";

    private static final String[] BACKGROUND_MUSIC = {
            MUSIC_CALM1, MUSIC_CALM2, MUSIC_CALM3,
            MUSIC_HAL1, MUSIC_HAL2, MUSIC_HAL3, MUSIC_HAL4,
            MUSIC_NUANCE1, MUSIC_NUANCE2,
            MUSIC_PIANO1, MUSIC_PIANO2, MUSIC_PIANO3
    };

    public WorldSoundEvent {
        soundId = normalizeSoundId(soundId);
        x = sanitizeCoordinate(x);
        y = sanitizeCoordinate(y);
        z = sanitizeCoordinate(z);
        volume = clamp(volume, 0.0f, MAX_SOUND_VOLUME);
        pitch = clamp(pitch, 0.0f, MAX_SOUND_PITCH);
    }

    public boolean isPlayable() {
        return !soundId.isEmpty() && (isControlEvent() || (volume > 0.0f && pitch > 0.0f));
    }

    public static String openableSoundId(BlockType type, boolean open) {
        if (type == BlockType.WOODEN_DOOR || type == BlockType.IRON_DOOR
                || type == BlockType.TRAPDOOR || type == BlockType.FENCE_GATE) {
            return open ? DOOR_OPEN : DOOR_CLOSE;
        }
        return null;
    }

    public static String blockBreakSoundId(BlockType type) {
        return blockDigSoundId(type);
    }

    public static String blockPlaceSoundId(BlockType type) {
        return blockDigSoundId(type);
    }

    public static float blockInteractionVolume(BlockType type) {
        return blockDigSoundId(type) == null ? 0.0f : 1.0f;
    }

    public static float blockBreakPitch(BlockType type) {
        return blockSoundPitch(type) * 0.8f;
    }

    public static float blockPlacePitch(BlockType type) {
        return blockSoundPitch(type) * 0.8f;
    }

    public static String blockStepSoundId(BlockType type) {
        if (type == null || type == BlockType.AIR || type == BlockType.FIRE
                || type.isFluid() || type == BlockType.PORTAL || type == BlockType.END_PORTAL) {
            return null;
        }
        return switch (type) {
            case OAK_PLANKS, OAK_LOG, NOTE_BLOCK, CHEST, CRAFTING_TABLE, STANDING_SIGN,
                    WALL_SIGN, WOODEN_DOOR, WOODEN_PRESSURE_PLATE, JUKEBOX, OAK_STAIRS,
                    FENCE, PUMPKIN, JACK_O_LANTERN, LOCKED_CHEST, TRAPDOOR,
                    BROWN_MUSHROOM_BLOCK, RED_MUSHROOM_BLOCK, MELON, FENCE_GATE,
                    BOOKSHELF -> STEP_WOOD;
            case GRASS, LEAVES, SAPLING, TALL_GRASS, DEAD_BUSH, YELLOW_FLOWER,
                    RED_ROSE, BROWN_MUSHROOM, RED_MUSHROOM, TNT, CROPS, PUMPKIN_STEM,
                    MELON_STEM, VINES, MYCELIUM, LILY_PAD, NETHER_WART -> STEP_GRASS;
            case DIRT, GRAVEL, FARMLAND, CLAY -> STEP_GRAVEL;
            case SAND, SOUL_SAND -> STEP_SAND;
            case WHITE_WOOL, COBWEB, BED, CACTUS, SPONGE, CAKE -> STEP_CLOTH;
            case SNOW_LAYER, SNOW -> STEP_SNOW;
            case LADDER -> STEP_LADDER;
            default -> STEP_STONE;
        };
    }

    public static float blockStepVolume(BlockType type) {
        return blockStepSoundId(type) == null ? 0.0f : 0.15f;
    }

    public static float blockStepPitch(BlockType type) {
        return blockSoundPitch(type);
    }

    private static String blockDigSoundId(BlockType type) {
        if (type == null || type == BlockType.AIR || type == BlockType.FIRE
                || type.isFluid() || type == BlockType.PORTAL || type == BlockType.END_PORTAL) {
            return null;
        }
        return switch (type) {
            case OAK_PLANKS, OAK_LOG, NOTE_BLOCK, CHEST, CRAFTING_TABLE, STANDING_SIGN,
                    WALL_SIGN, WOODEN_DOOR, WOODEN_PRESSURE_PLATE, JUKEBOX, OAK_STAIRS,
                    FENCE, PUMPKIN, JACK_O_LANTERN, LOCKED_CHEST, TRAPDOOR,
                    BROWN_MUSHROOM_BLOCK, RED_MUSHROOM_BLOCK, MELON, FENCE_GATE,
                    BOOKSHELF -> DIG_WOOD;
            case GRASS, LEAVES, SAPLING, TALL_GRASS, DEAD_BUSH, YELLOW_FLOWER,
                    RED_ROSE, BROWN_MUSHROOM, RED_MUSHROOM, TNT, CROPS, PUMPKIN_STEM,
                    MELON_STEM, VINES, MYCELIUM, LILY_PAD, NETHER_WART -> DIG_GRASS;
            case DIRT, GRAVEL, FARMLAND, CLAY -> DIG_GRAVEL;
            case SAND, SOUL_SAND -> DIG_SAND;
            case WHITE_WOOL, COBWEB, BED, CACTUS, SPONGE, CAKE -> DIG_CLOTH;
            case SNOW_LAYER, SNOW, ICE -> DIG_SNOW;
            case LADDER -> DIG_LADDER;
            case GLASS, GLASS_PANE, GLOWSTONE -> GLASS_BREAK;
            case GOLD_BLOCK, IRON_BLOCK, DIAMOND_BLOCK, IRON_DOOR, IRON_BARS,
                    RAIL, POWERED_RAIL, DETECTOR_RAIL, CAULDRON -> DIG_METAL;
            default -> DIG_STONE;
        };
    }

    private static float blockSoundPitch(BlockType type) {
        if (type == null) {
            return 1.0f;
        }
        return switch (type) {
            case GOLD_BLOCK, IRON_BLOCK, DIAMOND_BLOCK, IRON_DOOR, IRON_BARS,
                    RAIL, POWERED_RAIL, DETECTOR_RAIL, CAULDRON -> 1.5f;
            default -> 1.0f;
        };
    }

    public static String noteSoundId(int instrument) {
        return switch (instrument) {
            case NoteBlockTileEntity.INSTRUMENT_BASS_DRUM -> NOTE_BASS_DRUM;
            case NoteBlockTileEntity.INSTRUMENT_SNARE -> NOTE_SNARE;
            case NoteBlockTileEntity.INSTRUMENT_STICKS -> NOTE_STICKS;
            case NoteBlockTileEntity.INSTRUMENT_BASS -> NOTE_BASS;
            default -> NOTE_HARP;
        };
    }

    public static float notePitch(int pitch) {
        return (float) Math.pow(2.0, (Math.floorMod(pitch, 25) - 12) / 12.0);
    }

    public static String fallSoundId(float fallDistance) {
        int damage = (int) Math.ceil(Math.max(0.0f, fallDistance) - 3.0f);
        if (damage <= 0) {
            return null;
        }
        return damage > 4 ? FALL_BIG : FALL_SMALL;
    }

    public static float redstoneClickPitch(boolean powered) {
        return powered ? 0.6f : 0.5f;
    }

    public static WorldSoundEvent uiButtonClick() {
        return new WorldSoundEvent(UI_BUTTON_CLICK, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f);
    }

    public static WorldSoundEvent stopRecord(float x, float y, float z) {
        return new WorldSoundEvent(RECORD_STOP, x, y, z, 1.0f, 1.0f);
    }

    public boolean isControlEvent() {
        return isRecordStop();
    }

    public boolean isRecordStop() {
        return RECORD_STOP.equals(soundId);
    }

    public boolean isRecordSound() {
        return soundId.startsWith("records.") && !isRecordStop();
    }

    public boolean isMusicSound() {
        return soundId.startsWith("music.");
    }

    public static float openablePitch(Random random) {
        Random source = random == null ? new Random(0L) : random;
        return source.nextFloat() * 0.1f + 0.9f;
    }

    public static float pistonExtendPitch(Random random) {
        Random source = random == null ? new Random(0L) : random;
        return source.nextFloat() * 0.25f + 0.6f;
    }

    public static float pistonRetractPitch(Random random) {
        Random source = random == null ? new Random(0L) : random;
        return source.nextFloat() * 0.15f + 0.6f;
    }

    public static float chickenPlopPitch(Random random) {
        Random source = random == null ? new Random(0L) : random;
        return (source.nextFloat() - source.nextFloat()) * 0.2f + 1.0f;
    }

    public static float portalAmbientPitch(Random random) {
        Random source = random == null ? new Random(0L) : random;
        return source.nextFloat() * 0.4f + 0.8f;
    }

    public static String recordSoundId(ItemType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case RECORD_13 -> RECORD_13;
            case RECORD_CAT -> RECORD_CAT;
            case RECORD_BLOCKS -> RECORD_BLOCKS;
            case RECORD_CHIRP -> RECORD_CHIRP;
            case RECORD_FAR -> RECORD_FAR;
            case RECORD_MALL -> RECORD_MALL;
            case RECORD_MELLOHI -> RECORD_MELLOHI;
            case RECORD_STAL -> RECORD_STAL;
            case RECORD_STRAD -> RECORD_STRAD;
            case RECORD_WARD -> RECORD_WARD;
            case RECORD_11 -> RECORD_11;
            default -> null;
        };
    }

    public static String randomBackgroundMusicSoundId(Random random) {
        Random source = random == null ? new Random(0L) : random;
        return BACKGROUND_MUSIC[source.nextInt(BACKGROUND_MUSIC.length)];
    }

    private static String normalizeSoundId(String soundId) {
        if (soundId == null) {
            return "";
        }
        String normalized = soundId.trim().toLowerCase(Locale.ROOT).replace('\\', '/');
        if (normalized.isEmpty()
                || normalized.length() > 128
                || normalized.startsWith("/")
                || normalized.contains("..")
                || !normalized.matches("[a-z0-9_./-]+")) {
            return "";
        }
        return normalized;
    }

    private static float sanitizeCoordinate(float value) {
        return clamp(value, -MAX_SOURCE_COORDINATE, MAX_SOURCE_COORDINATE);
    }

    private static float clamp(float value, float min, float max) {
        if (!Float.isFinite(value)) {
            return min <= 0.0f && max >= 0.0f ? 0.0f : min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
