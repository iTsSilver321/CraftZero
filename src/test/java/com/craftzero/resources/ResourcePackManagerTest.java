package com.craftzero.resources;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ResourcePackManagerTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Resource pack manager should list Default, folder packs, and zip packs")
    void listsDefaultFolderAndZipPacks() throws Exception {
        Path packsRoot = tempDir.resolve("texturepacks");
        Path defaultRoot = tempDir.resolve("resources");
        Files.createDirectories(packsRoot.resolve("Folder Pack"));
        Files.writeString(packsRoot.resolve("Folder Pack").resolve("pack.txt"), "Folder description");
        createZipPack(packsRoot.resolve("Zip Pack.zip"), "Zip description", "textures/item/items.png", "zip");

        ResourcePackManager manager = new ResourcePackManager(packsRoot, defaultRoot);
        List<ResourcePackManager.PackInfo> packs = manager.listPacks();

        assertEquals(3, packs.size());
        assertTrue(packs.get(0).isDefault());
        assertTrue(packs.stream().anyMatch(pack -> pack.id().equals("Folder Pack")
                && pack.source() == ResourcePackManager.PackSource.FOLDER
                && pack.metadata().description().equals("Folder description")));
        assertTrue(packs.stream().anyMatch(pack -> pack.id().equals("Zip Pack.zip")
                && pack.source() == ResourcePackManager.PackSource.ZIP
                && pack.metadata().description().equals("Zip description")));
    }

    @Test
    @DisplayName("Unreadable texture packs should not hide Default or valid packs")
    void listPacksSkipsUnreadablePackArchives() throws Exception {
        Path packsRoot = tempDir.resolve("texturepacks");
        Path defaultRoot = tempDir.resolve("resources");
        Files.createDirectories(packsRoot.resolve("Folder Pack"));
        Files.writeString(packsRoot.resolve("Folder Pack").resolve("pack.txt"), "Folder description");
        Files.writeString(packsRoot.resolve("Broken.zip"), "not a zip archive");

        ResourcePackManager manager = new ResourcePackManager(packsRoot, defaultRoot);
        List<ResourcePackManager.PackInfo> packs = manager.listPacks();

        assertEquals(2, packs.size());
        assertTrue(packs.get(0).isDefault());
        assertTrue(packs.stream().anyMatch(pack -> pack.id().equals("Folder Pack")));
    }

    @Test
    @DisplayName("Texture resolution should use selected packs and fall back to Default")
    void resolvesTexturesWithDefaultFallback() throws Exception {
        Path packsRoot = tempDir.resolve("texturepacks");
        Path defaultRoot = tempDir.resolve("resources");
        Path defaultGui = defaultRoot.resolve("textures").resolve("gui").resolve("gui.png");
        Path defaultTerrain = defaultRoot.resolve("textures").resolve("terrain").resolve("Terrain.png");
        Files.createDirectories(defaultGui.getParent());
        Files.createDirectories(defaultTerrain.getParent());
        Files.writeString(defaultGui, "default-gui");
        Files.writeString(defaultTerrain, "default-terrain");

        Path folderPackGui = packsRoot.resolve("Folder Pack").resolve("textures").resolve("gui").resolve("gui.png");
        Files.createDirectories(folderPackGui.getParent());
        Files.writeString(folderPackGui, "folder-gui");
        createZipPack(packsRoot.resolve("Zip Pack.zip"), "Zip description", "textures/item/items.png", "zip-items");

        ResourcePackManager manager = new ResourcePackManager(packsRoot, defaultRoot);
        manager.setSelectedPackId("Default");
        assertEquals(ResourcePackManager.DEFAULT_PACK_ID, manager.getSelectedPackId());

        ResourcePackManager.ResolvedTexture folderTexture =
                manager.resolveTexturePath("Folder Pack", "gui/gui.png").orElseThrow();
        assertEquals("textures/gui/gui.png", folderTexture.logicalPath());
        assertEquals("Folder Pack", folderTexture.pack().id());
        assertFalse(folderTexture.defaultFallback());
        assertArrayEquals("folder-gui".getBytes(), folderTexture.bytes());

        ResourcePackManager.ResolvedTexture fallbackTexture =
                manager.resolveTexturePath("Folder Pack", "terrain/Terrain.png").orElseThrow();
        assertEquals(ResourcePackManager.DEFAULT_PACK_ID, fallbackTexture.pack().id());
        assertTrue(fallbackTexture.defaultFallback());
        assertArrayEquals("default-terrain".getBytes(), fallbackTexture.bytes());

        Optional<ResourcePackManager.ResolvedTexture> zipTexture =
                manager.resolveTexturePath("Zip Pack.zip", "textures/item/items.png");
        assertTrue(zipTexture.isPresent());
        assertEquals("Zip Pack.zip", zipTexture.get().pack().id());
        assertArrayEquals("zip-items".getBytes(), zipTexture.get().bytes());
    }

    private static void createZipPack(Path zipPath, String description, String texturePath, String textureContent)
            throws Exception {
        Files.createDirectories(zipPath.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zip.putNextEntry(new ZipEntry("pack.txt"));
            zip.write(description.getBytes());
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry(texturePath));
            zip.write(textureContent.getBytes());
            zip.closeEntry();
        }
    }
}
