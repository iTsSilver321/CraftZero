package com.craftzero.audio;

import com.craftzero.resources.ResourcePackManager;
import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundAssetResolverTest {
    @TempDir
    Path tempDir;

    @AfterEach
    void clearActiveResourcePack() {
        ResourcePackManager.setActive(null);
    }

    @Test
    @DisplayName("Sound ids should resolve to Release-era OGG resource-pack candidates")
    void soundIdsResolveToReleaseEraCandidates() {
        List<String> doorCandidates = SoundAssetResolver.candidatesFor(WorldSoundEvent.DOOR_OPEN);

        assertEquals("sounds/random/door_open.ogg", doorCandidates.get(0));
        assertTrue(doorCandidates.contains("sound/random/door_open.ogg"));
        assertTrue(doorCandidates.contains("newsound/random/door_open.ogg"));

        List<String> recordCandidates = SoundAssetResolver.candidatesFor(WorldSoundEvent.RECORD_CAT);

        assertEquals("streaming/cat.ogg", recordCandidates.get(0));
        assertTrue(recordCandidates.contains("records/cat.ogg"));
        assertTrue(recordCandidates.contains("newsound/records/cat.ogg"));

        List<String> dragonCandidates = SoundAssetResolver.candidatesFor(WorldSoundEvent.ENDER_DRAGON_DEATH);

        assertEquals("sounds/mob/enderdragon/end.ogg", dragonCandidates.get(0));
        assertTrue(dragonCandidates.contains("newsound/mob/enderdragon/end.ogg"));

        List<String> musicCandidates = SoundAssetResolver.candidatesFor(WorldSoundEvent.MUSIC_CALM1);

        assertEquals("music/calm1.ogg", musicCandidates.get(0));
        assertTrue(musicCandidates.contains("sounds/music/calm1.ogg"));
        assertTrue(musicCandidates.contains("sound/music/calm1.ogg"));
        assertTrue(musicCandidates.contains("newsound/music/calm1.ogg"));
    }

    @Test
    @DisplayName("Flat mob sounds should resolve numbered Release-era sound-pool variants")
    void flatMobSoundsResolveNumberedPoolVariants() {
        List<String> cowCandidates = SoundAssetResolver.candidatesFor(WorldSoundEvent.COW_IDLE);

        assertEquals("sounds/mob/cow.ogg", cowCandidates.get(0));
        assertTrue(cowCandidates.contains("sounds/mob/cow1.ogg"));
        assertTrue(cowCandidates.contains("sound/mob/cow4.ogg"));
        assertTrue(cowCandidates.contains("newsound/mob/cow4.ogg"));

        List<String> zombieHurtCandidates = SoundAssetResolver.candidatesFor(WorldSoundEvent.ZOMBIE_HURT);

        assertTrue(zombieHurtCandidates.contains("sounds/mob/zombiehurt1.ogg"));
        assertTrue(zombieHurtCandidates.contains("newsound/mob/zombiehurt4.ogg"));

        List<String> endermanCandidates = SoundAssetResolver.candidatesFor(WorldSoundEvent.ENDERMAN_TELEPORT);

        assertTrue(endermanCandidates.contains("sounds/mob/endermen/portal.ogg"));
        assertFalse(endermanCandidates.contains("sounds/mob/endermen/portal1.ogg"));
    }

    @Test
    @DisplayName("Release-era pooled sounds should resolve numbered block, explosion, and nested mob variants")
    void pooledReleaseSoundsResolveNumberedVariants() {
        List<String> stoneCandidates = SoundAssetResolver.candidatesFor(WorldSoundEvent.DIG_STONE);

        assertEquals("sounds/dig/stone.ogg", stoneCandidates.get(0));
        assertTrue(stoneCandidates.contains("sounds/dig/stone1.ogg"));
        assertTrue(stoneCandidates.contains("sound/dig/stone3.ogg"));
        assertTrue(stoneCandidates.contains("newsound/dig/stone4.ogg"));

        List<String> stepCandidates = SoundAssetResolver.candidatesFor(WorldSoundEvent.STEP_GRASS);

        assertEquals("sounds/step/grass.ogg", stepCandidates.get(0));
        assertTrue(stepCandidates.contains("sounds/step/grass1.ogg"));
        assertTrue(stepCandidates.contains("sound/step/grass3.ogg"));
        assertTrue(stepCandidates.contains("newsound/step/grass4.ogg"));

        List<String> explosionCandidates = SoundAssetResolver.candidatesFor(WorldSoundEvent.EXPLOSION);

        assertTrue(explosionCandidates.contains("sounds/random/explode1.ogg"));
        assertTrue(explosionCandidates.contains("newsound/random/explode4.ogg"));

        List<String> blazeHitCandidates = SoundAssetResolver.candidatesFor(WorldSoundEvent.BLAZE_HURT);

        assertEquals("sounds/mob/blaze/hit.ogg", blazeHitCandidates.get(0));
        assertTrue(blazeHitCandidates.contains("sounds/mob/blaze/hit1.ogg"));
        assertTrue(blazeHitCandidates.contains("newsound/mob/blaze/hit4.ogg"));

        List<String> chickenPlopCandidates = SoundAssetResolver.candidatesFor(WorldSoundEvent.CHICKEN_PLOP);

        assertTrue(chickenPlopCandidates.contains("sounds/mob/chicken/plop.ogg"));
        assertTrue(chickenPlopCandidates.contains("sounds/mob/chickenplop.ogg"));
    }

    @Test
    @DisplayName("Sound assets should load from raw active resource paths")
    void soundAssetsLoadFromRawActiveResourcePaths() throws Exception {
        Path packsRoot = tempDir.resolve("texturepacks");
        Path defaultRoot = tempDir.resolve("resources");
        Path cow1 = defaultRoot.resolve("sounds").resolve("mob").resolve("cow1.ogg");
        Path cow2 = defaultRoot.resolve("newsound").resolve("mob").resolve("cow2.ogg");
        Path packCow3 = packsRoot.resolve("Release Sounds").resolve("sound").resolve("mob").resolve("cow3.ogg");
        Files.createDirectories(cow1.getParent());
        Files.createDirectories(cow2.getParent());
        Files.createDirectories(packCow3.getParent());
        Files.writeString(packsRoot.resolve("Release Sounds").resolve("pack.txt"), "Release sound pack");
        Files.writeString(cow1, "cow-one");
        Files.writeString(cow2, "cow-two");
        Files.writeString(packCow3, "cow-three-pack");
        ResourcePackManager manager = new ResourcePackManager(packsRoot, defaultRoot);
        manager.setSelectedPackId("Release Sounds");
        ResourcePackManager.setActive(manager);

        List<SoundAssetResolver.ResolvedSoundAsset> assets = SoundAssetResolver.loadAll(WorldSoundEvent.COW_IDLE);

        assertEquals(List.of("sounds/mob/cow1.ogg", "newsound/mob/cow2.ogg", "sound/mob/cow3.ogg"),
                assets.stream().map(SoundAssetResolver.ResolvedSoundAsset::path).toList());
        assertEquals("cow-one", bufferText(assets.get(0).encoded()));
        assertEquals("cow-two", bufferText(assets.get(1).encoded()));
        assertEquals("cow-three-pack", bufferText(assets.get(2).encoded()));
    }

    @Test
    @DisplayName("Sound assets should load from wrapped, legacy-root, and namespaced resource layouts")
    void soundAssetsLoadFromWrappedLegacyAndNamespacedLayouts() throws Exception {
        Path packsRoot = tempDir.resolve("texturepacks-layouts");
        Path defaultRoot = tempDir.resolve("resources-layouts");
        Path legacyMusic = defaultRoot.resolve("resources").resolve("music").resolve("calm1.ogg");
        Files.createDirectories(legacyMusic.getParent());
        Files.writeString(legacyMusic, "legacy-root-music");
        createZipPack(packsRoot.resolve("Mixed Sounds.zip"), "Mixed Release sounds",
                "assets/minecraft/sounds/random/door_open.ogg", "namespaced-door",
                "Release Sounds/newsound/random/click.ogg", "wrapped-click");
        ResourcePackManager manager = new ResourcePackManager(packsRoot, defaultRoot);
        manager.setSelectedPackId("Mixed Sounds.zip");
        ResourcePackManager.setActive(manager);

        List<SoundAssetResolver.ResolvedSoundAsset> doorAssets =
                SoundAssetResolver.loadAll(WorldSoundEvent.DOOR_OPEN);
        List<SoundAssetResolver.ResolvedSoundAsset> clickAssets =
                SoundAssetResolver.loadAll(WorldSoundEvent.REDSTONE_CLICK);
        List<SoundAssetResolver.ResolvedSoundAsset> musicAssets =
                SoundAssetResolver.loadAll(WorldSoundEvent.MUSIC_CALM1);

        assertEquals(List.of("sounds/random/door_open.ogg"),
                doorAssets.stream().map(SoundAssetResolver.ResolvedSoundAsset::path).toList());
        assertEquals("namespaced-door", bufferText(doorAssets.get(0).encoded()));
        assertEquals(List.of("newsound/random/click.ogg"),
                clickAssets.stream().map(SoundAssetResolver.ResolvedSoundAsset::path).toList());
        assertEquals("wrapped-click", bufferText(clickAssets.get(0).encoded()));
        assertEquals(List.of("music/calm1.ogg"),
                musicAssets.stream().map(SoundAssetResolver.ResolvedSoundAsset::path).toList());
        assertEquals("legacy-root-music", bufferText(musicAssets.get(0).encoded()));
    }

    @Test
    @DisplayName("Pooled sound assets should load from numbered and legacy alias paths")
    void pooledSoundAssetsLoadFromNumberedAndLegacyAliasPaths() throws Exception {
        Path packsRoot = tempDir.resolve("texturepacks-pooled");
        Path defaultRoot = tempDir.resolve("resources-pooled");
        Path stone3 = defaultRoot.resolve("sound").resolve("dig").resolve("stone3.ogg");
        Path stepGrass4 = defaultRoot.resolve("newsound").resolve("step").resolve("grass4.ogg");
        Path blaze2 = defaultRoot.resolve("newsound").resolve("mob").resolve("blaze").resolve("hit2.ogg");
        Path chickenPlop = defaultRoot.resolve("sounds").resolve("mob").resolve("chickenplop.ogg");
        Path packExplosion2 = packsRoot.resolve("Pooled Sounds").resolve("newsound")
                .resolve("random").resolve("explode2.ogg");
        Files.createDirectories(stone3.getParent());
        Files.createDirectories(stepGrass4.getParent());
        Files.createDirectories(blaze2.getParent());
        Files.createDirectories(chickenPlop.getParent());
        Files.createDirectories(packExplosion2.getParent());
        Files.writeString(packsRoot.resolve("Pooled Sounds").resolve("pack.txt"), "Release pooled sounds");
        Files.writeString(stone3, "stone-three");
        Files.writeString(stepGrass4, "grass-step-four");
        Files.writeString(blaze2, "blaze-two");
        Files.writeString(chickenPlop, "chicken-plop-flat");
        Files.writeString(packExplosion2, "explosion-two-pack");
        ResourcePackManager manager = new ResourcePackManager(packsRoot, defaultRoot);
        manager.setSelectedPackId("Pooled Sounds");
        ResourcePackManager.setActive(manager);

        List<SoundAssetResolver.ResolvedSoundAsset> stoneAssets = SoundAssetResolver.loadAll(WorldSoundEvent.DIG_STONE);
        List<SoundAssetResolver.ResolvedSoundAsset> stepAssets =
                SoundAssetResolver.loadAll(WorldSoundEvent.STEP_GRASS);
        List<SoundAssetResolver.ResolvedSoundAsset> blazeAssets = SoundAssetResolver.loadAll(WorldSoundEvent.BLAZE_HURT);
        List<SoundAssetResolver.ResolvedSoundAsset> chickenAssets =
                SoundAssetResolver.loadAll(WorldSoundEvent.CHICKEN_PLOP);
        List<SoundAssetResolver.ResolvedSoundAsset> explosionAssets =
                SoundAssetResolver.loadAll(WorldSoundEvent.EXPLOSION);

        assertEquals(List.of("sound/dig/stone3.ogg"),
                stoneAssets.stream().map(SoundAssetResolver.ResolvedSoundAsset::path).toList());
        assertEquals("stone-three", bufferText(stoneAssets.get(0).encoded()));
        assertEquals(List.of("newsound/step/grass4.ogg"),
                stepAssets.stream().map(SoundAssetResolver.ResolvedSoundAsset::path).toList());
        assertEquals("grass-step-four", bufferText(stepAssets.get(0).encoded()));
        assertEquals(List.of("newsound/mob/blaze/hit2.ogg"),
                blazeAssets.stream().map(SoundAssetResolver.ResolvedSoundAsset::path).toList());
        assertEquals("blaze-two", bufferText(blazeAssets.get(0).encoded()));
        assertEquals(List.of("sounds/mob/chickenplop.ogg"),
                chickenAssets.stream().map(SoundAssetResolver.ResolvedSoundAsset::path).toList());
        assertEquals("chicken-plop-flat", bufferText(chickenAssets.get(0).encoded()));
        assertEquals(List.of("newsound/random/explode2.ogg"),
                explosionAssets.stream().map(SoundAssetResolver.ResolvedSoundAsset::path).toList());
        assertEquals("explosion-two-pack", bufferText(explosionAssets.get(0).encoded()));
    }

    @Test
    @DisplayName("Unsafe or empty sound ids should not produce asset paths")
    void unsafeSoundIdsDoNotResolve() {
        assertTrue(SoundAssetResolver.candidatesFor(null).isEmpty());
        assertTrue(SoundAssetResolver.candidatesFor("").isEmpty());
        assertTrue(SoundAssetResolver.candidatesFor("../random/click").isEmpty());
        assertTrue(SoundAssetResolver.candidatesFor("/random/click").isEmpty());
    }

    @Test
    @DisplayName("Spatial dispatcher should pass listener position to spatial sinks")
    void dispatcherUpdatesSpatialSinkListener() {
        World world = new World(6294L);
        RecordingSpatialSink sink = new RecordingSpatialSink();
        WorldSoundDispatcher dispatcher = new WorldSoundDispatcher(sink);
        try {
            world.playSound(WorldSoundEvent.DOOR_OPEN, 1.0f, 2.0f, 3.0f, 1.0f, 1.0f);

            int played = dispatcher.dispatch(world, 1.0f, 4.0f, 5.0f, 6.0f);

            assertEquals(1, played);
            assertEquals(4.0f, sink.listenerX, 0.0001f);
            assertEquals(5.0f, sink.listenerY, 0.0001f);
            assertEquals(6.0f, sink.listenerZ, 0.0001f);
            assertEquals(1, sink.played);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spatial dispatcher should pass listener orientation to orientation-aware sinks")
    void dispatcherUpdatesSpatialSinkListenerOrientation() {
        World world = new World(6295L);
        RecordingSpatialSink sink = new RecordingSpatialSink();
        WorldSoundDispatcher dispatcher = new WorldSoundDispatcher(sink);
        try {
            world.playSound(WorldSoundEvent.DOOR_CLOSE, 1.0f, 2.0f, 3.0f, 1.0f, 1.0f);

            int played = dispatcher.dispatch(world, 1.0f,
                    4.0f, 5.0f, 6.0f,
                    0.25f, -0.5f, -0.75f,
                    0.0f, 1.0f, 0.0f);

            assertEquals(1, played);
            assertEquals(4.0f, sink.listenerX, 0.0001f);
            assertEquals(5.0f, sink.listenerY, 0.0001f);
            assertEquals(6.0f, sink.listenerZ, 0.0001f);
            assertEquals(0.25f, sink.forwardX, 0.0001f);
            assertEquals(-0.5f, sink.forwardY, 0.0001f);
            assertEquals(-0.75f, sink.forwardZ, 0.0001f);
            assertEquals(0.0f, sink.upX, 0.0001f);
            assertEquals(1.0f, sink.upY, 0.0001f);
            assertEquals(0.0f, sink.upZ, 0.0001f);
            assertEquals(1, sink.played);
        } finally {
            world.cleanup();
        }
    }

    private static final class RecordingSpatialSink implements WorldSoundDispatcher.SpatialSoundSink {
        private float listenerX;
        private float listenerY;
        private float listenerZ;
        private float forwardX;
        private float forwardY;
        private float forwardZ;
        private float upX;
        private float upY;
        private float upZ;
        private int played;

        @Override
        public void setListener(float x, float y, float z) {
            listenerX = x;
            listenerY = y;
            listenerZ = z;
        }

        @Override
        public void setListener(float x, float y, float z,
                float forwardX, float forwardY, float forwardZ,
                float upX, float upY, float upZ) {
            setListener(x, y, z);
            this.forwardX = forwardX;
            this.forwardY = forwardY;
            this.forwardZ = forwardZ;
            this.upX = upX;
            this.upY = upY;
            this.upZ = upZ;
        }

        @Override
        public void play(WorldSoundEvent event, float effectiveVolume) {
            played++;
        }
    }

    private static String bufferText(ByteBuffer buffer) {
        ByteBuffer copy = buffer.asReadOnlyBuffer();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void createZipPack(Path zipPath, String description, String... resourcePairs)
            throws Exception {
        if (resourcePairs.length % 2 != 0) {
            throw new IllegalArgumentException("resourcePairs must contain path/content pairs");
        }
        Files.createDirectories(zipPath.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zip.putNextEntry(new ZipEntry("pack.txt"));
            zip.write(description.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            for (int i = 0; i < resourcePairs.length; i += 2) {
                zip.putNextEntry(new ZipEntry(resourcePairs[i]));
                zip.write(resourcePairs[i + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
    }
}
