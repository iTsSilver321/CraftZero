package com.craftzero.audio;

import com.craftzero.world.WorldSoundEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmbientMusicSchedulerTest {
    @Test
    @DisplayName("Ambient music should play through the music volume path")
    void ambientMusicUsesMusicVolumeAndListenerPosition() {
        List<PlayedMusic> played = new ArrayList<>();
        AmbientMusicScheduler scheduler = new AmbientMusicScheduler(
                (event, volume) -> played.add(new PlayedMusic(event, volume)),
                new ZeroRandom(), 0.0f, 0.0f, 30.0f, 0.0f);

        assertTrue(scheduler.tick(0.0f, 0.25f, 1.0f, 2.0f, 3.0f));

        assertEquals(1, played.size());
        assertEquals(WorldSoundEvent.MUSIC_CALM1, played.get(0).event().soundId());
        assertEquals(0.25f, played.get(0).effectiveVolume(), 0.0001f);
        assertEquals(1.0f, played.get(0).event().x(), 0.0001f);
        assertEquals(2.0f, played.get(0).event().y(), 0.0001f);
        assertEquals(3.0f, played.get(0).event().z(), 0.0001f);
        assertEquals(30.0f, scheduler.secondsUntilNextCue(), 0.0001f);
    }

    @Test
    @DisplayName("Muted music should not consume a due background cue")
    void mutedMusicDoesNotConsumeDueCue() {
        List<PlayedMusic> played = new ArrayList<>();
        AmbientMusicScheduler scheduler = new AmbientMusicScheduler(
                (event, volume) -> played.add(new PlayedMusic(event, volume)),
                new ZeroRandom(), 0.0f, 0.0f, 30.0f, 0.0f);

        assertFalse(scheduler.tick(1.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        assertTrue(played.isEmpty());
        assertEquals(0.0f, scheduler.secondsUntilNextCue(), 0.0001f);

        assertTrue(scheduler.tick(0.0f, 1.0f, 0.0f, 0.0f, 0.0f));
        assertEquals(1, played.size());
    }

    @Test
    @DisplayName("Ambient music should wait for its scheduled delay before playing")
    void ambientMusicWaitsForScheduledDelay() {
        List<PlayedMusic> played = new ArrayList<>();
        AmbientMusicScheduler scheduler = new AmbientMusicScheduler(
                (event, volume) -> played.add(new PlayedMusic(event, volume)),
                new ZeroRandom(), 5.0f, 0.0f, 30.0f, 0.0f);

        assertFalse(scheduler.tick(4.9f, 1.0f, 0.0f, 0.0f, 0.0f));
        assertTrue(played.isEmpty());

        assertTrue(scheduler.tick(0.1f, 1.0f, 0.0f, 0.0f, 0.0f));
        assertEquals(1, played.size());
    }

    private record PlayedMusic(WorldSoundEvent event, float effectiveVolume) {
    }

    private static final class ZeroRandom extends Random {
        @Override
        public int nextInt(int bound) {
            return 0;
        }

        @Override
        public float nextFloat() {
            return 0.0f;
        }
    }
}
