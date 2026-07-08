package com.craftzero.audio;

import com.craftzero.world.WorldSoundEvent;

import java.util.Random;

/**
 * Release-era background music cue scheduler.
 */
public final class AmbientMusicScheduler {
    public static final float DEFAULT_INITIAL_MIN_DELAY_SECONDS = 120.0f;
    public static final float DEFAULT_INITIAL_RANDOM_DELAY_SECONDS = 120.0f;
    public static final float DEFAULT_NEXT_MIN_DELAY_SECONDS = 600.0f;
    public static final float DEFAULT_NEXT_RANDOM_DELAY_SECONDS = 600.0f;

    private static final WorldSoundDispatcher.SoundSink NOOP_SINK = (event, effectiveVolume) -> {
    };

    private final WorldSoundDispatcher.SoundSink sink;
    private final Random random;
    private final float initialMinDelaySeconds;
    private final float initialRandomDelaySeconds;
    private final float nextMinDelaySeconds;
    private final float nextRandomDelaySeconds;
    private float secondsUntilNextCue;

    public AmbientMusicScheduler(WorldSoundDispatcher.SoundSink sink) {
        this(sink, new Random(),
                DEFAULT_INITIAL_MIN_DELAY_SECONDS,
                DEFAULT_INITIAL_RANDOM_DELAY_SECONDS,
                DEFAULT_NEXT_MIN_DELAY_SECONDS,
                DEFAULT_NEXT_RANDOM_DELAY_SECONDS);
    }

    AmbientMusicScheduler(WorldSoundDispatcher.SoundSink sink, Random random,
            float initialMinDelaySeconds, float initialRandomDelaySeconds,
            float nextMinDelaySeconds, float nextRandomDelaySeconds) {
        this.sink = sink == null ? NOOP_SINK : sink;
        this.random = random == null ? new Random(0L) : random;
        this.initialMinDelaySeconds = positiveOrZero(initialMinDelaySeconds);
        this.initialRandomDelaySeconds = positiveOrZero(initialRandomDelaySeconds);
        this.nextMinDelaySeconds = positiveOrZero(nextMinDelaySeconds);
        this.nextRandomDelaySeconds = positiveOrZero(nextRandomDelaySeconds);
        reset();
    }

    public void reset() {
        secondsUntilNextCue = randomDelay(initialMinDelaySeconds, initialRandomDelaySeconds);
    }

    public boolean tick(float deltaSeconds, float musicVolume,
            float listenerX, float listenerY, float listenerZ) {
        if (clamp01(musicVolume) <= 0.0f) {
            return false;
        }
        secondsUntilNextCue -= Math.max(0.0f, finiteOrZero(deltaSeconds));
        if (secondsUntilNextCue > 0.0f) {
            return false;
        }

        String soundId = WorldSoundEvent.randomBackgroundMusicSoundId(random);
        boolean played = playIntoSink(new WorldSoundEvent(soundId,
                finiteOrZero(listenerX), finiteOrZero(listenerY), finiteOrZero(listenerZ),
                1.0f, 1.0f), clamp01(musicVolume));
        secondsUntilNextCue = randomDelay(nextMinDelaySeconds, nextRandomDelaySeconds);
        return played;
    }

    float secondsUntilNextCue() {
        return secondsUntilNextCue;
    }

    private float randomDelay(float minSeconds, float randomSeconds) {
        if (randomSeconds <= 0.0f) {
            return minSeconds;
        }
        return minSeconds + random.nextFloat() * randomSeconds;
    }

    private boolean playIntoSink(WorldSoundEvent event, float effectiveVolume) {
        if (event == null || !event.isPlayable() || !Float.isFinite(effectiveVolume) || effectiveVolume <= 0.0f) {
            return false;
        }
        try {
            sink.play(event, effectiveVolume);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static float positiveOrZero(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        return Math.max(0.0f, value);
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
