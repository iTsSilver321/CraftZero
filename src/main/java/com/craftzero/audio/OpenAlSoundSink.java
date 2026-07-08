package com.craftzero.audio;

import com.craftzero.resources.ResourcePackManager;
import com.craftzero.world.WorldSoundEvent;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_memory;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * Minimal OpenAL playback sink for transient world sound events.
 */
public final class OpenAlSoundSink implements WorldSoundDispatcher.SpatialSoundSink, AutoCloseable {
    private static final int MAX_ACTIVE_SOURCES = 32;

    private final long device;
    private final long context;
    private final Map<String, List<Integer>> buffersBySoundId = new HashMap<>();
    private final Set<String> missingSoundIds = new HashSet<>();
    private final List<ActiveSource> activeSources = new ArrayList<>();
    private final Random random = new Random();
    private float listenerX;
    private float listenerY;
    private float listenerZ;
    private boolean hasListener;
    private boolean closed;
    private long resourceRevision;

    private enum SourceKind {
        TRANSIENT,
        RECORD,
        MUSIC
    }

    private record ActiveSource(int source, SourceKind kind, int blockX, int blockY, int blockZ,
            float x, float y, float z, float rawVolume, float volumeScale) {
    }

    private OpenAlSoundSink(long device, long context) {
        this.device = device;
        this.context = context;
        this.resourceRevision = ResourcePackManager.activeResourceRevision();
    }

    public static OpenAlSoundSink create() {
        long device = alcOpenDevice((ByteBuffer) null);
        if (device == NULL) {
            throw new IllegalStateException("No OpenAL audio device is available");
        }

        ALCCapabilities deviceCapabilities = org.lwjgl.openal.ALC.createCapabilities(device);
        long context = alcCreateContext(device, (IntBuffer) null);
        if (context == NULL) {
            alcCloseDevice(device);
            throw new IllegalStateException("Failed to create OpenAL context");
        }

        if (!alcMakeContextCurrent(context)) {
            alcDestroyContext(context);
            alcCloseDevice(device);
            throw new IllegalStateException("Failed to activate OpenAL context");
        }
        AL.createCapabilities(deviceCapabilities);
        alDistanceModel(AL_NONE);
        alListener3f(AL_POSITION, 0.0f, 0.0f, 0.0f);
        alListener3f(AL_VELOCITY, 0.0f, 0.0f, 0.0f);
        alListenerfv(AL_ORIENTATION, new float[] { 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f });

        return new OpenAlSoundSink(device, context);
    }

    @Override
    public synchronized void setListener(float x, float y, float z) {
        if (closed || !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            return;
        }
        x = sanitizeCoordinate(x);
        y = sanitizeCoordinate(y);
        z = sanitizeCoordinate(z);
        rememberListener(x, y, z);
        alListener3f(AL_POSITION, x, y, z);
        updatePersistentSpatialGains();
    }

    @Override
    public synchronized void setListener(float x, float y, float z,
            float forwardX, float forwardY, float forwardZ,
            float upX, float upY, float upZ) {
        if (closed || !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            return;
        }
        x = sanitizeCoordinate(x);
        y = sanitizeCoordinate(y);
        z = sanitizeCoordinate(z);
        rememberListener(x, y, z);
        alListener3f(AL_POSITION, x, y, z);
        if (!isUsableVector(forwardX, forwardY, forwardZ) || !isUsableVector(upX, upY, upZ)) {
            updatePersistentSpatialGains();
            return;
        }
        float forwardLength = vectorLength(forwardX, forwardY, forwardZ);
        float upLength = vectorLength(upX, upY, upZ);
        alListenerfv(AL_ORIENTATION, new float[] {
                forwardX / forwardLength, forwardY / forwardLength, forwardZ / forwardLength,
                upX / upLength, upY / upLength, upZ / upLength
        });
        updatePersistentSpatialGains();
    }

    @Override
    public synchronized void play(WorldSoundEvent event, float effectiveVolume) {
        if (closed || event == null || !event.isPlayable()) {
            return;
        }
        if (event.isRecordStop()) {
            stopRecordAt(event);
            return;
        }
        effectiveVolume = sanitizeGain(effectiveVolume);
        if (effectiveVolume <= 0.0f) {
            return;
        }
        refreshResourceCachesIfNeeded();
        purgeStoppedSources();
        String soundId = SoundAssetResolver.normalizeSoundId(event.soundId());
        int buffer = bufferFor(soundId);
        if (buffer == 0) {
            return;
        }
        while (activeSources.size() >= MAX_ACTIVE_SOURCES) {
            deleteSource(activeSources.remove(reclaimableSourceIndex()));
        }

        SourceKind kind = sourceKind(event);
        if (kind == SourceKind.RECORD) {
            stopRecordAt(event);
        } else if (kind == SourceKind.MUSIC) {
            stopMusicSources();
        }

        int source = alGenSources();
        if (source == 0) {
            return;
        }
        alSourcei(source, AL_BUFFER, buffer);
        float volumeScale = persistentVolumeScale(event, effectiveVolume);
        alSourcef(source, AL_GAIN, sourceGain(event, kind, effectiveVolume, volumeScale));
        alSourcef(source, AL_PITCH, sanitizePitch(event.pitch()));
        float sourceX = sanitizeCoordinate(event.x());
        float sourceY = sanitizeCoordinate(event.y());
        float sourceZ = sanitizeCoordinate(event.z());
        if (kind == SourceKind.MUSIC) {
            alSource3f(source, AL_POSITION, 0.0f, 0.0f, 0.0f);
            alSourcei(source, AL_SOURCE_RELATIVE, AL_TRUE);
        } else {
            alSource3f(source, AL_POSITION, sourceX, sourceY, sourceZ);
            alSourcei(source, AL_SOURCE_RELATIVE, AL_FALSE);
        }
        alSourcePlay(source);
        activeSources.add(new ActiveSource(source, kind,
                blockCoord(sourceX), blockCoord(sourceY), blockCoord(sourceZ),
                sourceX, sourceY, sourceZ, event.volume(), volumeScale));
    }

    private void rememberListener(float x, float y, float z) {
        listenerX = x;
        listenerY = y;
        listenerZ = z;
        hasListener = true;
    }

    private SourceKind sourceKind(WorldSoundEvent event) {
        if (event.isRecordSound()) {
            return SourceKind.RECORD;
        }
        if (event.isMusicSound()) {
            return SourceKind.MUSIC;
        }
        return SourceKind.TRANSIENT;
    }

    private float sourceGain(WorldSoundEvent event, SourceKind kind, float effectiveVolume, float volumeScale) {
        if (kind == SourceKind.RECORD) {
            return recordGain(event.x(), event.y(), event.z(), event.volume(), volumeScale);
        }
        return sanitizeGain(effectiveVolume);
    }

    private float persistentVolumeScale(WorldSoundEvent event, float effectiveVolume) {
        if (!event.isRecordSound()) {
            return 1.0f;
        }
        float rawGain = Math.min(1.0f, Math.max(0.0f, event.volume()));
        if (rawGain <= 0.0f) {
            return 0.0f;
        }
        float distanceFactor = recordDistanceFactor(event.x(), event.y(), event.z(), event.volume());
        if (distanceFactor <= 0.0f) {
            return 0.0f;
        }
        return Math.max(0.0f, effectiveVolume / (rawGain * distanceFactor));
    }

    private void updatePersistentSpatialGains() {
        if (closed || activeSources.isEmpty()) {
            return;
        }
        for (ActiveSource active : activeSources) {
            if (active.kind() == SourceKind.RECORD) {
                alSourcef(active.source(), AL_GAIN,
                        recordGain(active.x(), active.y(), active.z(), active.rawVolume(), active.volumeScale()));
            }
        }
    }

    private float recordGain(float x, float y, float z, float rawVolume, float volumeScale) {
        float rawGain = Math.min(1.0f, Math.max(0.0f, rawVolume));
        return Math.max(0.0f, rawGain * Math.max(0.0f, volumeScale)
                * recordDistanceFactor(x, y, z, rawVolume));
    }

    private float recordDistanceFactor(float x, float y, float z, float rawVolume) {
        if (!hasListener) {
            return 1.0f;
        }
        float audibleRadius = WorldSoundDispatcher.audibleRadius(rawVolume);
        if (audibleRadius <= 0.0f) {
            return 0.0f;
        }
        double dx = sanitizeCoordinate(x) - listenerX;
        double dy = sanitizeCoordinate(y) - listenerY;
        double dz = sanitizeCoordinate(z) - listenerZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return distance >= audibleRadius ? 0.0f : (float) (1.0f - distance / audibleRadius);
    }

    private void stopRecordAt(WorldSoundEvent event) {
        int blockX = blockCoord(sanitizeCoordinate(event.x()));
        int blockY = blockCoord(sanitizeCoordinate(event.y()));
        int blockZ = blockCoord(sanitizeCoordinate(event.z()));
        for (int i = activeSources.size() - 1; i >= 0; i--) {
            ActiveSource active = activeSources.get(i);
            if (active.kind() == SourceKind.RECORD
                    && active.blockX() == blockX
                    && active.blockY() == blockY
                    && active.blockZ() == blockZ) {
                deleteSource(active);
                activeSources.remove(i);
            }
        }
    }

    private void stopMusicSources() {
        for (int i = activeSources.size() - 1; i >= 0; i--) {
            ActiveSource active = activeSources.get(i);
            if (active.kind() == SourceKind.MUSIC) {
                deleteSource(active);
                activeSources.remove(i);
            }
        }
    }

    private int reclaimableSourceIndex() {
        for (int i = 0; i < activeSources.size(); i++) {
            if (activeSources.get(i).kind() == SourceKind.TRANSIENT) {
                return i;
            }
        }
        return 0;
    }

    private static int blockCoord(float value) {
        return (int) Math.floor(value);
    }

    private int bufferFor(String soundId) {
        soundId = SoundAssetResolver.normalizeSoundId(soundId);
        if (soundId.isEmpty() || missingSoundIds.contains(soundId)) {
            return 0;
        }

        List<Integer> buffers = buffersBySoundId.get(soundId);
        if (buffers == null) {
            buffers = loadBuffers(soundId);
            if (buffers.isEmpty()) {
                missingSoundIds.add(soundId);
                return 0;
            }
            buffersBySoundId.put(soundId, buffers);
        }

        if (buffers.size() == 1) {
            return buffers.get(0);
        }
        return buffers.get(random.nextInt(buffers.size()));
    }

    private void refreshResourceCachesIfNeeded() {
        long currentRevision = ResourcePackManager.activeResourceRevision();
        if (currentRevision == resourceRevision) {
            return;
        }
        for (ActiveSource active : activeSources) {
            deleteSource(active);
        }
        activeSources.clear();
        deleteCachedBuffers();
        missingSoundIds.clear();
        resourceRevision = currentRevision;
    }

    private List<Integer> loadBuffers(String soundId) {
        List<Integer> buffers = new ArrayList<>();
        for (SoundAssetResolver.ResolvedSoundAsset asset : SoundAssetResolver.loadAll(soundId)) {
            if (asset == null || asset.encoded() == null || !asset.encoded().hasRemaining()) {
                continue;
            }
            Optional<Integer> buffer = decodeSoundAsset(asset.encoded());
            buffer.ifPresent(buffers::add);
        }
        if (buffers.isEmpty()) {
            buffers.addAll(loadProceduralBuffers(soundId));
        }
        return List.copyOf(buffers);
    }

    private List<Integer> loadProceduralBuffers(String soundId) {
        List<Integer> buffers = new ArrayList<>();
        for (ShortBuffer pcm : ProceduralSoundBank.synthesize(soundId)) {
            if (pcm == null || !pcm.hasRemaining()) {
                continue;
            }
            int buffer = alGenBuffers();
            if (buffer == 0) {
                continue;
            }
            try {
                alBufferData(buffer, AL_FORMAT_MONO16, pcm, ProceduralSoundBank.SAMPLE_RATE);
                buffers.add(buffer);
            } catch (RuntimeException ex) {
                alDeleteBuffers(buffer);
            }
        }
        return List.copyOf(buffers);
    }

    private Optional<Integer> decodeSoundAsset(ByteBuffer encoded) {
        try {
            return Optional.of(decodeOggToBuffer(encoded));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private int decodeOggToBuffer(ByteBuffer encoded) {
        ShortBuffer pcm = null;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channels = stack.mallocInt(1);
            IntBuffer sampleRate = stack.mallocInt(1);
            pcm = stb_vorbis_decode_memory(encoded, channels, sampleRate);
            if (pcm == null) {
                throw new IllegalArgumentException("Invalid OGG sound data");
            }
            int channelCount = channels.get(0);
            int samplesPerSecond = sampleRate.get(0);
            if (samplesPerSecond <= 0) {
                throw new IllegalArgumentException("Invalid OGG sample rate: " + samplesPerSecond);
            }
            int format = switch (channelCount) {
                case 1 -> AL_FORMAT_MONO16;
                case 2 -> AL_FORMAT_STEREO16;
                default -> throw new IllegalArgumentException("Unsupported OGG channel count: " + channelCount);
            };
            int buffer = alGenBuffers();
            if (buffer == 0) {
                throw new IllegalStateException("Failed to allocate OpenAL buffer");
            }
            alBufferData(buffer, format, pcm, samplesPerSecond);
            return buffer;
        } finally {
            if (pcm != null) {
                memFree(pcm);
            }
        }
    }

    private void purgeStoppedSources() {
        for (int i = activeSources.size() - 1; i >= 0; i--) {
            ActiveSource active = activeSources.get(i);
            if (alGetSourcei(active.source(), AL_SOURCE_STATE) == AL_STOPPED) {
                deleteSource(active);
                activeSources.remove(i);
            }
        }
    }

    private static void deleteSource(ActiveSource active) {
        if (active != null) {
            deleteSource(active.source());
        }
    }

    private static void deleteSource(int source) {
        if (source == 0) {
            return;
        }
        alSourceStop(source);
        alDeleteSources(source);
    }

    private static boolean isUsableVector(float x, float y, float z) {
        double lengthSquared = vectorLengthSquared(x, y, z);
        return Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z)
                && Double.isFinite(lengthSquared)
                && lengthSquared > 0.000001d;
    }

    private static float vectorLength(float x, float y, float z) {
        return (float) Math.sqrt(vectorLengthSquared(x, y, z));
    }

    private static double vectorLengthSquared(float x, float y, float z) {
        return (double) x * x + (double) y * y + (double) z * z;
    }

    private static float sanitizeCoordinate(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        return Math.max(-WorldSoundEvent.MAX_SOURCE_COORDINATE,
                Math.min(WorldSoundEvent.MAX_SOURCE_COORDINATE, value));
    }

    private static float sanitizeGain(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float sanitizePitch(float value) {
        if (!Float.isFinite(value) || value <= 0.0f) {
            return 1.0f;
        }
        return Math.max(0.01f, Math.min(WorldSoundEvent.MAX_SOUND_PITCH, value));
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (ActiveSource active : activeSources) {
            deleteSource(active);
        }
        activeSources.clear();
        deleteCachedBuffers();
        missingSoundIds.clear();

        alcMakeContextCurrent(NULL);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }

    private void deleteCachedBuffers() {
        for (List<Integer> buffers : buffersBySoundId.values()) {
            for (int buffer : buffers) {
                if (buffer != 0) {
                    alDeleteBuffers(buffer);
                }
            }
        }
        buffersBySoundId.clear();
    }
}
