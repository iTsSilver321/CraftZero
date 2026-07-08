package com.craftzero.audio;

import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;

import java.util.List;

/**
 * Drains simulation sound events into a runtime playback sink.
 */
public final class WorldSoundDispatcher {
    public static final float BASE_AUDIBLE_RADIUS = 16.0f;

    @FunctionalInterface
    public interface SoundSink {
        void play(WorldSoundEvent event, float effectiveVolume);
    }

    public interface SpatialSoundSink extends SoundSink {
        void setListener(float x, float y, float z);

        default void setListener(float x, float y, float z,
                float forwardX, float forwardY, float forwardZ,
                float upX, float upY, float upZ) {
            setListener(x, y, z);
        }
    }

    private static final SoundSink NOOP_SINK = (event, effectiveVolume) -> {
    };

    private final SoundSink sink;

    public WorldSoundDispatcher(SoundSink sink) {
        this.sink = sink == null ? NOOP_SINK : sink;
    }

    public static WorldSoundDispatcher noop() {
        return new WorldSoundDispatcher(NOOP_SINK);
    }

    public boolean play(WorldSoundEvent event, float soundVolume) {
        if (!isDispatchable(event)) {
            return false;
        }
        if (event.isControlEvent()) {
            return playIntoSink(event, 1.0f);
        }
        float effectiveVolume = effectiveVolume(event, clamp01(soundVolume),
                Float.NaN, Float.NaN, Float.NaN, false);
        if (effectiveVolume <= 0.0f) {
            return false;
        }
        return playIntoSink(event, effectiveVolume);
    }

    public int dispatch(World world, float soundVolume) {
        return dispatchInternal(world, soundVolume, Float.NaN, Float.NaN, Float.NaN,
                0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f, false, false);
    }

    public int dispatchEvents(List<WorldSoundEvent> events, float soundVolume) {
        return dispatchEventsInternal(events, soundVolume, Float.NaN, Float.NaN, Float.NaN,
                0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f, false, false);
    }

    public int dispatch(World world, float soundVolume, float listenerX, float listenerY, float listenerZ) {
        return dispatchInternal(world, soundVolume, listenerX, listenerY, listenerZ,
                0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f, true, false);
    }

    public int dispatchEvents(List<WorldSoundEvent> events, float soundVolume,
            float listenerX, float listenerY, float listenerZ) {
        return dispatchEventsInternal(events, soundVolume, listenerX, listenerY, listenerZ,
                0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f, true, false);
    }

    public int dispatch(World world, float soundVolume,
            float listenerX, float listenerY, float listenerZ,
            float forwardX, float forwardY, float forwardZ,
            float upX, float upY, float upZ) {
        return dispatchInternal(world, soundVolume, listenerX, listenerY, listenerZ,
                forwardX, forwardY, forwardZ, upX, upY, upZ, true, true);
    }

    public int dispatchEvents(List<WorldSoundEvent> events, float soundVolume,
            float listenerX, float listenerY, float listenerZ,
            float forwardX, float forwardY, float forwardZ,
            float upX, float upY, float upZ) {
        return dispatchEventsInternal(events, soundVolume, listenerX, listenerY, listenerZ,
                forwardX, forwardY, forwardZ, upX, upY, upZ, true, true);
    }

    private int dispatchInternal(World world, float soundVolume,
            float listenerX, float listenerY, float listenerZ,
            float forwardX, float forwardY, float forwardZ,
            float upX, float upY, float upZ,
            boolean spatialCull, boolean listenerOrientation) {
        if (world == null) {
            return 0;
        }
        return dispatchEventsInternal(world.drainSoundEvents(), soundVolume, listenerX, listenerY, listenerZ,
                forwardX, forwardY, forwardZ, upX, upY, upZ, spatialCull, listenerOrientation);
    }

    private int dispatchEventsInternal(List<WorldSoundEvent> events, float soundVolume,
            float listenerX, float listenerY, float listenerZ,
            float forwardX, float forwardY, float forwardZ,
            float upX, float upY, float upZ,
            boolean spatialCull, boolean listenerOrientation) {
        if (events == null || events.isEmpty()) {
            return 0;
        }
        float volumeScale = clamp01(soundVolume);
        boolean hasUsableListener = spatialCull
                && Float.isFinite(listenerX) && Float.isFinite(listenerY) && Float.isFinite(listenerZ);
        if (!hasUsableListener) {
            listenerX = Float.NaN;
            listenerY = Float.NaN;
            listenerZ = Float.NaN;
        } else {
            listenerX = sanitizeCoordinate(listenerX);
            listenerY = sanitizeCoordinate(listenerY);
            listenerZ = sanitizeCoordinate(listenerZ);
        }
        if (spatialCull && sink instanceof SpatialSoundSink spatialSink) {
            if (listenerOrientation) {
                spatialSink.setListener(listenerX, listenerY, listenerZ,
                        forwardX, forwardY, forwardZ, upX, upY, upZ);
            } else {
                spatialSink.setListener(listenerX, listenerY, listenerZ);
            }
        }
        int played = 0;
        for (WorldSoundEvent event : events) {
            if (!isDispatchable(event)) {
                continue;
            }
            if (event.isControlEvent()) {
                if (playIntoSink(event, 1.0f)) {
                    played++;
                }
                continue;
            }
            if (hasUsableListener && !isAudible(event, listenerX, listenerY, listenerZ)) {
                continue;
            }
            float effectiveVolume = effectiveVolume(event, volumeScale,
                    listenerX, listenerY, listenerZ, hasUsableListener);
            if (effectiveVolume <= 0.0f) {
                continue;
            }
            if (playIntoSink(event, effectiveVolume)) {
                played++;
            }
        }
        return played;
    }

    private static boolean isAudible(WorldSoundEvent event, float listenerX, float listenerY, float listenerZ) {
        if (!Float.isFinite(listenerX) || !Float.isFinite(listenerY) || !Float.isFinite(listenerZ)) {
            return true;
        }
        float audibleRadius = audibleRadius(event.volume());
        double dx = event.x() - listenerX;
        double dy = event.y() - listenerY;
        double dz = event.z() - listenerZ;
        double radius = audibleRadius;
        return dx * dx + dy * dy + dz * dz < radius * radius;
    }

    public static float audibleRadius(float eventVolume) {
        float sanitizedVolume = Float.isFinite(eventVolume)
                ? Math.max(0.0f, Math.min(WorldSoundEvent.MAX_SOUND_VOLUME, eventVolume))
                : 0.0f;
        return BASE_AUDIBLE_RADIUS * Math.max(1.0f, sanitizedVolume);
    }

    public static float effectiveVolume(WorldSoundEvent event, float soundVolume,
            float listenerX, float listenerY, float listenerZ) {
        return effectiveVolume(event, clamp01(soundVolume), listenerX, listenerY, listenerZ, true);
    }

    private static float effectiveVolume(WorldSoundEvent event, float volumeScale,
            float listenerX, float listenerY, float listenerZ, boolean spatial) {
        if (!isDispatchable(event)) {
            return 0.0f;
        }
        float gain = Math.min(1.0f, Math.max(0.0f, event.volume())) * volumeScale;
        if (!spatial || gain <= 0.0f || !Float.isFinite(listenerX)
                || !Float.isFinite(listenerY) || !Float.isFinite(listenerZ)) {
            return gain;
        }
        float audibleRadius = audibleRadius(event.volume());
        if (audibleRadius <= 0.0f) {
            return 0.0f;
        }
        double dx = event.x() - listenerX;
        double dy = event.y() - listenerY;
        double dz = event.z() - listenerZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return distance >= audibleRadius ? 0.0f : (float) (gain * (1.0f - distance / audibleRadius));
    }

    private boolean playIntoSink(WorldSoundEvent event, float effectiveVolume) {
        if (!Float.isFinite(effectiveVolume) || effectiveVolume < 0.0f) {
            return false;
        }
        try {
            sink.play(event, effectiveVolume);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean isDispatchable(WorldSoundEvent event) {
        return event != null && event.isPlayable();
    }

    private static float sanitizeCoordinate(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        return Math.max(-WorldSoundEvent.MAX_SOURCE_COORDINATE,
                Math.min(WorldSoundEvent.MAX_SOURCE_COORDINATE, value));
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
