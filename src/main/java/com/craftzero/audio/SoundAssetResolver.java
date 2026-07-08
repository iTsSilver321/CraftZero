package com.craftzero.audio;

import com.craftzero.resources.ResourcePackManager;
import org.lwjgl.BufferUtils;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves Release-era logical sound ids to old resource-pack OGG paths.
 */
final class SoundAssetResolver {
    private static final int SOUND_POOL_VARIANTS = 4;
    private static final int CAVE_SOUND_POOL_VARIANTS = 13;
    private static final int MAX_SOUND_ID_LENGTH = 128;
    private static final Set<String> NUMBERED_SOUND_POOLS = Set.of(
            "dig.stone",
            "dig.wood",
            "dig.gravel",
            "dig.grass",
            "dig.cloth",
            "dig.sand",
            "dig.snow",
            "dig.ladder",
            "step.stone",
            "step.wood",
            "step.gravel",
            "step.grass",
            "step.cloth",
            "step.sand",
            "step.snow",
            "step.ladder",
            "random.explode",
            "random.glass",
            "random.eat",
            "random.drink",
            "random.burp",
            "random.pop",
            "random.splash",
            "random.click",
            "random.fizz",
            "random.bow",
            "random.orb",
            "random.levelup",
            "random.fuse",
            "random.door_open",
            "random.door_close",
            "random.chestopen",
            "random.chestclosed",
            "fire.ignite",
            "portal.portal",
            "ambient.cave.cave",
            "ambient.weather.thunder",
            "ambient.weather.rain",
            "mob.zombiepig.zpig",
            "mob.zombiepig.zpighurt",
            "mob.zombiepig.zpigdeath",
            "mob.zombiepig.zpigangry",
            "mob.wolf.bark",
            "mob.wolf.growl",
            "mob.wolf.whine",
            "mob.wolf.panting",
            "mob.wolf.hurt",
            "mob.wolf.death",
            "mob.wolf.shake",
            "mob.magmacube.big",
            "mob.magmacube.small",
            "mob.magmacube.jump",
            "mob.blaze.breathe",
            "mob.blaze.hit",
            "mob.blaze.death",
            "mob.silverfish.say",
            "mob.silverfish.hit",
            "mob.silverfish.kill",
            "mob.endermen.idle",
            "mob.endermen.scream",
            "mob.endermen.stare",
            "mob.endermen.hit",
            "mob.endermen.death",
            "mob.ghast.moan",
            "mob.ghast.scream",
            "mob.ghast.death",
            "mob.ghast.charge",
            "mob.ghast.fireball",
            "mob.enderdragon.end");

    private SoundAssetResolver() {
    }

    static Optional<ByteBuffer> loadFirst(String soundId) {
        return loadAll(soundId).stream()
                .findFirst()
                .map(ResolvedSoundAsset::encoded);
    }

    static List<ResolvedSoundAsset> loadAll(String soundId) {
        List<ResolvedSoundAsset> assets = new ArrayList<>();
        for (String candidate : candidatesFor(soundId)) {
            Optional<byte[]> bytes = readBytes(candidate);
            bytes.ifPresent(value -> assets.add(new ResolvedSoundAsset(candidate, toByteBuffer(value))));
        }
        return List.copyOf(assets);
    }

    static List<String> candidatesFor(String soundId) {
        String normalized = normalizeSoundId(soundId);
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<String> candidates = new ArrayList<>();
        if (normalized.endsWith(".ogg")) {
            addExistingOggPath(candidates, normalized);
            return List.copyOf(candidates);
        }

        if (normalized.startsWith("records.")) {
            String recordName = normalized.substring("records.".length()).replace('.', '/');
            addOgg(candidates, recordName);
            addOgg(candidates, "streaming/" + recordName);
            addOgg(candidates, "records/" + recordName);
            addOgg(candidates, "music/records/" + recordName);
            addMinecraftRootedOgg(candidates, recordName);
            addMinecraftRootedOgg(candidates, "streaming/" + recordName);
            addMinecraftRootedOgg(candidates, "records/" + recordName);
            addMinecraftRootedOgg(candidates, "music/records/" + recordName);
            addSoundRootedOgg(candidates, "streaming/" + recordName);
            addSoundRootedOgg(candidates, "records/" + recordName);
            addOgg(candidates, "sounds/records/" + recordName);
            addOgg(candidates, "sound/records/" + recordName);
            addOgg(candidates, "newsound/records/" + recordName);
            addOgg(candidates, "sound3/records/" + recordName);
            return List.copyOf(candidates);
        }

        if (normalized.startsWith("music.")) {
            String musicName = normalized.substring("music.".length()).replace('.', '/');
            addOgg(candidates, "music/" + musicName);
            addOgg(candidates, "streaming/" + musicName);
            addOgg(candidates, "streaming/music/" + musicName);
            addMinecraftRootedOgg(candidates, "music/" + musicName);
            addMinecraftRootedOgg(candidates, "streaming/" + musicName);
            addMinecraftRootedOgg(candidates, "streaming/music/" + musicName);
            addSoundRootedOgg(candidates, "music/" + musicName);
            addSoundRootedOgg(candidates, "streaming/" + musicName);
        }

        for (String logicalPath : logicalPathsFor(normalized)) {
            addSoundRootedOgg(candidates, logicalPath);
            addOgg(candidates, logicalPath);
        }
        return List.copyOf(candidates);
    }

    private static List<String> logicalPathsFor(String normalized) {
        String logicalPath = normalized.replace('.', '/');
        List<String> paths = new ArrayList<>();
        addLogicalPath(paths, logicalPath);
        if (isFlatMobSoundPool(normalized) || NUMBERED_SOUND_POOLS.contains(normalized)) {
            addNumberedVariants(paths, logicalPath, numberedVariantCount(normalized));
        }

        for (LegacyAliasPath alias : legacyAliasPathsFor(normalized)) {
            addLogicalPath(paths, alias.logicalPath());
            if (alias.numberedPool()) {
                addNumberedVariants(paths, alias.logicalPath(), numberedVariantCount(normalized));
            }
        }

        return List.copyOf(paths);
    }

    private static void addNumberedVariants(List<String> paths, String logicalPath, int variants) {
        for (int variant = 1; variant <= variants; variant++) {
            addLogicalPath(paths, logicalPath + variant);
        }
    }

    private static int numberedVariantCount(String normalized) {
        return "ambient.cave.cave".equals(normalized) ? CAVE_SOUND_POOL_VARIANTS : SOUND_POOL_VARIANTS;
    }

    private static void addLogicalPath(List<String> paths, String logicalPath) {
        if (!paths.contains(logicalPath)) {
            paths.add(logicalPath);
        }
    }

    private static List<LegacyAliasPath> legacyAliasPathsFor(String normalized) {
        return switch (normalized) {
            case "note.harp" -> List.of(alias("note/harp"));
            case "note.bd" -> List.of(alias("note/bd"), alias("note/bassdrum"));
            case "note.snare" -> List.of(alias("note/snare"));
            case "note.hat" -> List.of(alias("note/hat"), alias("note/sticks"));
            case "note.bass", "note.bassattack" -> List.of(alias("note/bassattack"),
                    alias("note/bass"));
            case "random.door_open" -> numberedAlias("random/dooropen");
            case "random.door_close" -> numberedAlias("random/doorclose");
            case "random.classic_hurt", "random.old_hurt" -> List.of(numberedAliasPath("damage/hurtflesh"),
                    alias("damage/hurt"), alias("damage/hit"), numberedAliasPath("random/hurt"));
            case "damage.hurt", "damage.hurtflesh", "damage.hit" -> List.of(numberedAliasPath("damage/hurtflesh"),
                    alias("damage/hurt"), alias("damage/hit"), numberedAliasPath("random/hurt"));
            case "random.chestopen" -> List.of(numberedAliasPath("random/chest_open"),
                    numberedAliasPath("random/chestopen"));
            case "random.chestclosed" -> List.of(numberedAliasPath("random/chest_close"),
                    numberedAliasPath("random/chestclosed"), numberedAliasPath("random/chestclose"));
            case "random.levelup" -> List.of(numberedAliasPath("random/level_up"),
                    numberedAliasPath("random/levelup"));
            case "random.hurt" -> List.of(numberedAliasPath("damage/hurtflesh"),
                    alias("damage/hurt"), alias("damage/hit"));
            case "damage.fallsmall" -> List.of(alias("damage/fall_small"));
            case "damage.fallbig" -> List.of(alias("damage/fall_big"));
            case "fire.ignite" -> List.of(numberedAliasPath("fire/fire"),
                    numberedAliasPath("random/ignite"));
            case "tile.piston.out" -> List.of(alias("tile/pistonout"),
                    alias("random/pistonout"));
            case "tile.piston.in" -> List.of(alias("tile/pistonin"),
                    alias("random/pistonin"));
            case "mob.zombiepig.zpig" -> numberedAlias("mob/pigzombie/zpig");
            case "mob.zombiepig.zpighurt" -> numberedAlias("mob/pigzombie/zpighurt");
            case "mob.zombiepig.zpigdeath" -> numberedAlias("mob/pigzombie/zpigdeath");
            case "mob.zombiepig.zpigangry" -> numberedAlias("mob/pigzombie/zpigangry");
            case "mob.chicken" -> numberedAlias("mob/chicken/say");
            case "mob.chickenhurt" -> numberedAlias("mob/chicken/hurt");
            case "mob.chicken.plop" -> List.of(alias("mob/chickenplop"), alias("mob/chicken/plop"));
            case "mob.cow" -> numberedAlias("mob/cow/say");
            case "mob.cowhurt" -> numberedAlias("mob/cow/hurt");
            case "mob.pig" -> numberedAlias("mob/pig/say");
            case "mob.pigdeath" -> numberedAlias("mob/pig/death");
            case "mob.sheep" -> numberedAlias("mob/sheep/say");
            case "mob.zombie" -> numberedAlias("mob/zombie/zombie");
            case "mob.zombiehurt" -> numberedAlias("mob/zombie/hurt");
            case "mob.zombiedeath" -> numberedAlias("mob/zombie/death");
            case "mob.skeleton" -> numberedAlias("mob/skeleton/say");
            case "mob.skeletonhurt" -> List.of(numberedAliasPath("mob/skeleton/hurt"),
                    numberedAliasPath("mob/skeleton/death"));
            case "mob.creeper" -> List.of(numberedAliasPath("mob/creeper/say"),
                    numberedAliasPath("mob/creeper/hurt"));
            case "mob.creeperdeath" -> numberedAlias("mob/creeper/death");
            case "mob.spider" -> List.of(numberedAliasPath("mob/spider/say"),
                    numberedAliasPath("mob/spider/hurt"));
            case "mob.spiderdeath" -> numberedAlias("mob/spider/death");
            case "mob.slime" -> List.of(numberedAliasPath("mob/slime/slime"),
                    numberedAliasPath("mob/slime/big"), numberedAliasPath("mob/slime/small"));
            case "mob.slimeattack" -> numberedAlias("mob/slime/attack");
            case "mob.wolf.panting" -> List.of(numberedAliasPath("mob/wolf/panting"),
                    numberedAliasPath("mob/wolf/pant"));
            case "mob.silverfish.kill" -> numberedAlias("mob/silverfish/death");
            case "mob.endermen.idle" -> numberedAlias("mob/enderman/idle");
            case "mob.endermen.scream" -> numberedAlias("mob/enderman/scream");
            case "mob.endermen.stare" -> numberedAlias("mob/enderman/stare");
            case "mob.endermen.hit" -> numberedAlias("mob/enderman/hit");
            case "mob.endermen.death" -> numberedAlias("mob/enderman/death");
            case "mob.endermen.portal" -> List.of(alias("mob/enderman/portal"),
                    alias("mob/endermen/portal"));
            case "mob.enderdragon.end" -> List.of(numberedAliasPath("mob/enderdragon/end"),
                    alias("mob/enderdragon/death"), alias("mob/dragon/death"),
                    alias("mob/enderdragon/growl"));
            default -> List.of();
        };
    }

    private static List<LegacyAliasPath> numberedAlias(String logicalPath) {
        return List.of(numberedAliasPath(logicalPath));
    }

    private static LegacyAliasPath numberedAliasPath(String logicalPath) {
        return new LegacyAliasPath(logicalPath, true);
    }

    private static LegacyAliasPath alias(String logicalPath) {
        return new LegacyAliasPath(logicalPath, false);
    }

    private static boolean isFlatMobSoundPool(String normalized) {
        return normalized.startsWith("mob.")
                && normalized.indexOf('.', "mob.".length()) < 0
                && normalized.indexOf('/') < 0;
    }

    private static void addOgg(List<String> candidates, String pathWithoutExtension) {
        addPath(candidates, pathWithoutExtension + ".ogg");
    }

    private static void addSoundRootedOgg(List<String> candidates, String logicalPath) {
        addOgg(candidates, "sounds/" + logicalPath);
        addOgg(candidates, "sound/" + logicalPath);
        addOgg(candidates, "newsound/" + logicalPath);
        addOgg(candidates, "sound3/" + logicalPath);
        addMinecraftRootedOgg(candidates, logicalPath);
    }

    private static void addMinecraftRootedOgg(List<String> candidates, String logicalPath) {
        addOgg(candidates, "assets/minecraft/sounds/" + logicalPath);
        addOgg(candidates, "assets/minecraft/sound/" + logicalPath);
        addOgg(candidates, "assets/minecraft/newsound/" + logicalPath);
        addOgg(candidates, "assets/minecraft/sound3/" + logicalPath);
    }

    private static void addExistingOggPath(List<String> candidates, String oggPath) {
        addPath(candidates, oggPath);
        if (!hasKnownSoundRoot(oggPath)) {
            addPath(candidates, "sounds/" + oggPath);
            addPath(candidates, "sound/" + oggPath);
            addPath(candidates, "newsound/" + oggPath);
            addPath(candidates, "sound3/" + oggPath);
            addPath(candidates, "assets/minecraft/sounds/" + oggPath);
            addPath(candidates, "assets/minecraft/sound/" + oggPath);
            addPath(candidates, "assets/minecraft/newsound/" + oggPath);
            addPath(candidates, "assets/minecraft/sound3/" + oggPath);
        }
    }

    private static void addPath(List<String> candidates, String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return;
        }
        candidate = candidate.replace('\\', '/');
        if (!candidates.contains(candidate)) {
            candidates.add(candidate);
        }
    }

    private static boolean hasKnownSoundRoot(String path) {
        return path.startsWith("sounds/")
                || path.startsWith("sound/")
                || path.startsWith("newsound/")
                || path.startsWith("sound3/")
                || path.startsWith("assets/minecraft/sounds/")
                || path.startsWith("assets/minecraft/sound/")
                || path.startsWith("assets/minecraft/newsound/")
                || path.startsWith("assets/minecraft/sound3/")
                || path.startsWith("records/")
                || path.startsWith("streaming/")
                || path.startsWith("music/");
    }

    static String normalizeSoundId(String soundId) {
        if (soundId == null) {
            return "";
        }
        String normalized = soundId.trim().toLowerCase(Locale.ROOT).replace('\\', '/');
        if (normalized.isEmpty()
                || normalized.length() > MAX_SOUND_ID_LENGTH
                || normalized.startsWith("/")
                || normalized.contains("..")
                || !normalized.matches("[a-z0-9_./-]+")) {
            return "";
        }
        return normalized;
    }

    private static Optional<byte[]> readBytes(String path) {
        try {
            Optional<InputStream> active = ResourcePackManager.openActiveResource(path);
            if (active.isPresent()) {
                try (InputStream stream = active.get()) {
                    return Optional.of(stream.readAllBytes());
                }
            }

            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream stream = loader == null ? null : loader.getResourceAsStream(path);
            if (stream == null) {
                ClassLoader fallback = SoundAssetResolver.class.getClassLoader();
                stream = fallback == null ? null : fallback.getResourceAsStream(path);
            }
            try (InputStream classpath = stream) {
                return classpath == null ? Optional.empty() : Optional.of(classpath.readAllBytes());
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static ByteBuffer toByteBuffer(byte[] bytes) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return buffer;
    }

    record ResolvedSoundAsset(String path, ByteBuffer encoded) {
    }

    private record LegacyAliasPath(String logicalPath, boolean numberedPool) {
    }
}
