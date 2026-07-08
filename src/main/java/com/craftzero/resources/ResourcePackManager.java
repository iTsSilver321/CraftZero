package com.craftzero.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Catalogs texture packs and resolves logical texture paths with default fallback.
 */
public final class ResourcePackManager {
    public static final Path DEFAULT_TEXTURE_PACKS_ROOT = Paths.get("texturepacks");
    public static final Path DEFAULT_RESOURCES_ROOT = Paths.get("src", "main", "resources");
    public static final Path DEFAULT_LEGACY_RESOURCES_ROOT = Paths.get("resources");
    public static final String DEFAULT_PACK_ID = "default";
    public static final String DEFAULT_PACK_NAME = "Default";

    private final Path texturePacksRoot;
    private final Path defaultResourcesRoot;
    private String selectedPackId = DEFAULT_PACK_ID;
    private static ResourcePackManager active;
    private static long activeResourceRevision;

    public ResourcePackManager() {
        this(DEFAULT_TEXTURE_PACKS_ROOT, DEFAULT_RESOURCES_ROOT);
    }

    public ResourcePackManager(Path texturePacksRoot) {
        this(texturePacksRoot, DEFAULT_RESOURCES_ROOT);
    }

    public ResourcePackManager(Path texturePacksRoot, Path defaultResourcesRoot) {
        if (texturePacksRoot == null || defaultResourcesRoot == null) {
            throw new IllegalArgumentException("Resource pack paths cannot be null");
        }
        this.texturePacksRoot = texturePacksRoot.toAbsolutePath().normalize();
        this.defaultResourcesRoot = defaultResourcesRoot.toAbsolutePath().normalize();
    }

    public Path getTexturePacksRoot() {
        return texturePacksRoot;
    }

    public Path getDefaultResourcesRoot() {
        return defaultResourcesRoot;
    }

    public String getSelectedPackId() {
        return selectedPackId;
    }

    public void setSelectedPackId(String selectedPackId) {
        String normalized = normalizePackId(selectedPackId);
        if (!this.selectedPackId.equals(normalized)) {
            this.selectedPackId = normalized;
            if (active == this) {
                activeResourceRevision++;
            }
        }
    }

    public static void setActive(ResourcePackManager manager) {
        if (active != manager) {
            active = manager;
            activeResourceRevision++;
        }
    }

    public static long activeResourceRevision() {
        return activeResourceRevision;
    }

    public static Optional<InputStream> openActive(String resourcePath) {
        if (active == null) {
            return Optional.empty();
        }
        try {
            return active.resolveSelectedTexturePath(resourcePath).map(ResolvedTexture::openStream);
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static Optional<InputStream> openActiveResource(String resourcePath) {
        if (active == null) {
            return Optional.empty();
        }
        try {
            return active.resolveSelectedResourcePath(resourcePath).map(ResolvedTexture::openStream);
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public List<PackInfo> listPacks() throws IOException {
        List<PackInfo> packs = new ArrayList<>();
        packs.add(defaultPackInfo());

        if (Files.isDirectory(texturePacksRoot)) {
            try (var entries = Files.list(texturePacksRoot)) {
                for (Path entry : entries.filter(this::isPackCandidate).toList()) {
                    try {
                        packs.add(readPackInfo(entry));
                    } catch (IOException | RuntimeException e) {
                        System.err.println("Skipping unreadable texture pack " + entry + ": " + e.getMessage());
                    }
                }
            }
        }

        packs.sort(Comparator
                .comparing((PackInfo pack) -> !pack.isDefault())
                .thenComparing(PackInfo::displayName, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(packs);
    }

    public Optional<PackInfo> findPack(String packId) throws IOException {
        String id = normalizePackId(packId);
        for (PackInfo pack : listPacks()) {
            if (pack.id().equals(id)) {
                return Optional.of(pack);
            }
        }
        return Optional.empty();
    }

    public Optional<ResolvedTexture> resolveSelectedTexturePath(String texturePath) throws IOException {
        return resolveTexturePath(selectedPackId, texturePath);
    }

    public Optional<ResolvedTexture> resolveSelectedResourcePath(String resourcePath) throws IOException {
        return resolveResourcePath(selectedPackId, resourcePath);
    }

    public Optional<ResolvedTexture> resolveTexturePath(String packId, String texturePath) throws IOException {
        return resolveNormalizedPath(packId, normalizeTexturePath(texturePath));
    }

    public Optional<ResolvedTexture> resolveResourcePath(String packId, String resourcePath) throws IOException {
        return resolveNormalizedPath(packId, normalizeResourcePath(resourcePath));
    }

    private Optional<ResolvedTexture> resolveNormalizedPath(String packId, String logicalPath) throws IOException {
        String requestedPackId = normalizePackId(packId);
        boolean requestedDefault = DEFAULT_PACK_ID.equals(requestedPackId);
        PackInfo requestedPack = findPack(requestedPackId).orElse(defaultPackInfo());

        if (!requestedPack.isDefault()) {
            Optional<byte[]> packedBytes = readPackBytes(requestedPack, logicalPath);
            if (packedBytes.isPresent()) {
                return Optional.of(new ResolvedTexture(logicalPath, requestedPack, false, packedBytes.get()));
            }
        }

        Optional<byte[]> defaultBytes = readDefaultBytes(logicalPath);
        return defaultBytes.map(bytes -> new ResolvedTexture(logicalPath, defaultPackInfo(),
                !requestedDefault, bytes));
    }

    public boolean hasTexture(String packId, String texturePath) throws IOException {
        return resolveTexturePath(packId, texturePath).isPresent();
    }

    private PackInfo defaultPackInfo() {
        return new PackInfo(DEFAULT_PACK_ID, DEFAULT_PACK_NAME, PackSource.DEFAULT, null,
                new PackMetadata(DEFAULT_PACK_NAME, "Built-in CraftZero textures", null));
    }

    private boolean isPackCandidate(Path entry) {
        if (Files.isDirectory(entry)) {
            return true;
        }
        String name = entry.getFileName().toString().toLowerCase(Locale.ROOT);
        return Files.isRegularFile(entry) && (name.endsWith(".zip") || name.endsWith(".jar"));
    }

    private PackInfo readPackInfo(Path path) throws IOException {
        PackSource source = Files.isDirectory(path) ? PackSource.FOLDER : PackSource.ZIP;
        PackMetadata metadata = source == PackSource.FOLDER ? readFolderMetadata(path) : readZipMetadata(path);
        String fileName = path.getFileName().toString();
        String displayName = metadata != null && metadata.name() != null && !metadata.name().isBlank()
                ? metadata.name().trim()
                : stripArchiveExtension(fileName);
        return new PackInfo(fileName, displayName, source, path, metadata);
    }

    private Optional<byte[]> readPackBytes(PackInfo pack, String logicalPath) throws IOException {
        if (pack.source() == PackSource.FOLDER) {
            return readFolderBytes(pack.path().toAbsolutePath().normalize(), logicalPath);
        }

        if (pack.source() == PackSource.ZIP) {
            return readArchiveBytes(pack.path(), logicalPath);
        }

        return Optional.empty();
    }

    private Optional<byte[]> readDefaultBytes(String logicalPath) throws IOException {
        List<String> candidates = resourceReadCandidates(logicalPath);
        Optional<byte[]> defaultBytes = readFilesystemCandidates(defaultResourcesRoot, candidates);
        if (defaultBytes.isPresent()) {
            return defaultBytes;
        }

        Path builtInRoot = DEFAULT_RESOURCES_ROOT.toAbsolutePath().normalize();
        Path legacyRoot = DEFAULT_LEGACY_RESOURCES_ROOT.toAbsolutePath().normalize();
        if (defaultResourcesRoot.equals(builtInRoot) && !legacyRoot.equals(defaultResourcesRoot)) {
            Optional<byte[]> legacyBytes = readFilesystemCandidates(legacyRoot, candidates);
            if (legacyBytes.isPresent()) {
                return legacyBytes;
            }
        }

        return readClasspathCandidates(candidates);
    }

    private Optional<byte[]> readFolderBytes(Path packRoot, String logicalPath) throws IOException {
        List<String> candidates = resourceReadCandidates(logicalPath);
        Optional<byte[]> exact = readFilesystemCandidates(packRoot, candidates);
        if (exact.isPresent() || !Files.isDirectory(packRoot)) {
            return exact;
        }

        try (var entries = Files.list(packRoot)) {
            for (Path child : entries.filter(Files::isDirectory).toList()) {
                Optional<byte[]> wrapped = readFilesystemCandidates(child.toAbsolutePath().normalize(), candidates);
                if (wrapped.isPresent()) {
                    return wrapped;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<byte[]> readArchiveBytes(Path archivePath, String logicalPath) throws IOException {
        List<String> candidates = resourceReadCandidates(logicalPath);
        try (ZipFile zipFile = new ZipFile(archivePath.toFile())) {
            for (String candidate : candidates) {
                Optional<byte[]> exact = readZipEntryBytes(zipFile, candidate);
                if (exact.isPresent()) {
                    return exact;
                }
            }

            for (ZipEntry entry : zipFile.stream().toList()) {
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = normalizeArchiveEntryName(entry.getName());
                for (String candidate : candidates) {
                    if (isSingleRootWrappedResource(entryName, candidate)) {
                        return readZipEntryBytes(zipFile, entry);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<byte[]> readFilesystemCandidates(Path root, List<String> candidates) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        for (String candidatePath : candidates) {
            Path candidate = normalizedRoot.resolve(candidatePath).normalize();
            if (candidate.startsWith(normalizedRoot) && Files.isRegularFile(candidate)) {
                return Optional.of(Files.readAllBytes(candidate));
            }
        }
        return Optional.empty();
    }

    private Optional<byte[]> readClasspathCandidates(List<String> candidates) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ClassLoader fallbackLoader = ResourcePackManager.class.getClassLoader();
        for (String candidate : candidates) {
            InputStream resourceStream = loader == null ? null : loader.getResourceAsStream(candidate);
            if (resourceStream == null) {
                resourceStream = fallbackLoader == null ? null : fallbackLoader.getResourceAsStream(candidate);
            }
            try (InputStream stream = resourceStream) {
                if (stream != null) {
                    return Optional.of(stream.readAllBytes());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<byte[]> readZipEntryBytes(ZipFile zipFile, String entryName) throws IOException {
        ZipEntry entry = zipFile.getEntry(entryName);
        if (entry == null || entry.isDirectory()) {
            return Optional.empty();
        }
        return readZipEntryBytes(zipFile, entry);
    }

    private Optional<byte[]> readZipEntryBytes(ZipFile zipFile, ZipEntry entry) throws IOException {
        try (InputStream stream = zipFile.getInputStream(entry)) {
            return Optional.of(stream.readAllBytes());
        }
    }

    private static List<String> resourceReadCandidates(String logicalPath) {
        List<String> candidates = new ArrayList<>();
        addResourceCandidate(candidates, logicalPath);
        if (logicalPath.startsWith("resources/")) {
            addResourceCandidate(candidates, logicalPath.substring("resources/".length()));
        } else {
            addResourceCandidate(candidates, "resources/" + logicalPath);
        }
        if (logicalPath.startsWith("assets/minecraft/")) {
            addResourceCandidate(candidates, logicalPath.substring("assets/minecraft/".length()));
        } else {
            addResourceCandidate(candidates, "assets/minecraft/" + logicalPath);
        }
        return List.copyOf(candidates);
    }

    private static void addResourceCandidate(List<String> candidates, String logicalPath) {
        if (logicalPath != null && !logicalPath.isBlank() && !candidates.contains(logicalPath)) {
            candidates.add(logicalPath);
        }
    }

    private static String normalizeArchiveEntryName(String entryName) {
        String normalized = entryName == null ? "" : entryName.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static boolean isSingleRootWrappedResource(String entryName, String logicalPath) {
        int separator = entryName.indexOf('/');
        return separator > 0 && separator + 1 < entryName.length()
                && entryName.substring(separator + 1).equals(logicalPath);
    }

    private PackMetadata readFolderMetadata(Path packRoot) throws IOException {
        Path mcmeta = packRoot.resolve("pack.mcmeta");
        if (Files.isRegularFile(mcmeta)) {
            try (Reader reader = Files.newBufferedReader(mcmeta)) {
                return parseMcmeta(reader);
            }
        }

        Path packTxt = packRoot.resolve("pack.txt");
        if (Files.isRegularFile(packTxt)) {
            List<String> lines = Files.readAllLines(packTxt);
            return parsePackTxt(lines);
        }
        return null;
    }

    private PackMetadata readZipMetadata(Path zipPath) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            ZipEntry mcmeta = zipFile.getEntry("pack.mcmeta");
            if (mcmeta != null && !mcmeta.isDirectory()) {
                try (Reader reader = new java.io.InputStreamReader(zipFile.getInputStream(mcmeta))) {
                    return parseMcmeta(reader);
                }
            }

            ZipEntry packTxt = zipFile.getEntry("pack.txt");
            if (packTxt != null && !packTxt.isDirectory()) {
                try (InputStream stream = zipFile.getInputStream(packTxt)) {
                    return parsePackTxt(new String(stream.readAllBytes()).lines().toList());
                }
            }
        }
        return null;
    }

    private PackMetadata parseMcmeta(Reader reader) {
        JsonElement rootElement = JsonParser.parseReader(reader);
        if (!rootElement.isJsonObject()) {
            return null;
        }

        JsonObject root = rootElement.getAsJsonObject();
        JsonObject pack = root.has("pack") && root.get("pack").isJsonObject()
                ? root.getAsJsonObject("pack")
                : root;
        String name = stringValue(root.get("name"));
        String description = stringValue(pack.get("description"));
        Integer packFormat = pack.has("pack_format") && pack.get("pack_format").isJsonPrimitive()
                ? pack.get("pack_format").getAsInt()
                : null;
        return new PackMetadata(name, description, packFormat);
    }

    private PackMetadata parsePackTxt(List<String> lines) {
        String description = lines == null
                ? ""
                : String.join(" ", lines.stream().map(String::trim).filter(line -> !line.isEmpty()).toList());
        return new PackMetadata(null, description, null);
    }

    private static String stringValue(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return element.toString();
    }

    private static String normalizePackId(String packId) {
        String cleaned = packId == null ? "" : packId.trim();
        return cleaned.isEmpty()
                || DEFAULT_PACK_ID.equalsIgnoreCase(cleaned)
                || DEFAULT_PACK_NAME.equalsIgnoreCase(cleaned)
                ? DEFAULT_PACK_ID
                : cleaned;
    }

    private static String normalizeTexturePath(String texturePath) {
        String normalized = normalizeResourcePath(texturePath);
        return normalized.startsWith("textures/") ? normalized : "textures/" + normalized;
    }

    private static String normalizeResourcePath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalArgumentException("resourcePath cannot be blank");
        }

        String raw = resourcePath.trim().replace('\\', '/');
        while (raw.startsWith("/")) {
            raw = raw.substring(1);
        }

        List<String> parts = new ArrayList<>();
        for (String part : raw.split("/")) {
            if (part.isBlank() || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                throw new IllegalArgumentException("resourcePath cannot escape the resource root");
            }
            parts.add(part);
        }

        String normalized = String.join("/", parts);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("resourcePath cannot be blank");
        }
        return normalized;
    }

    private static String stripArchiveExtension(String fileName) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        return lowerName.endsWith(".zip") || lowerName.endsWith(".jar")
                ? fileName.substring(0, fileName.length() - 4)
                : fileName;
    }

    public enum PackSource {
        DEFAULT,
        FOLDER,
        ZIP
    }

    public record PackMetadata(String name, String description, Integer packFormat) {
        public boolean hasDescription() {
            return description != null && !description.isBlank();
        }
    }

    public record PackInfo(String id, String displayName, PackSource source, Path path, PackMetadata metadata) {
        public boolean isDefault() {
            return source == PackSource.DEFAULT;
        }

        public boolean hasMetadata() {
            return metadata != null;
        }
    }

    public record ResolvedTexture(String logicalPath, PackInfo pack, boolean defaultFallback, byte[] bytes) {
        public InputStream openStream() {
            return new ByteArrayInputStream(bytes);
        }

        public int size() {
            return bytes.length;
        }
    }
}
