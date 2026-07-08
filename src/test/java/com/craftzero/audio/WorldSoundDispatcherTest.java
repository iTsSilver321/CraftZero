package com.craftzero.audio;

import com.craftzero.world.World;
import com.craftzero.world.WorldSoundEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldSoundDispatcherTest {
    @Test
    @DisplayName("Dispatcher should drain world sound events into a volume-scaled sink")
    void dispatcherDrainsEventsIntoSink() {
        World world = new World(6290L);
        List<PlayedSound> played = new ArrayList<>();
        WorldSoundDispatcher dispatcher = new WorldSoundDispatcher((event, volume) ->
                played.add(new PlayedSound(event.soundId(), volume, event.pitch())));
        try {
            world.playSound(WorldSoundEvent.DOOR_OPEN, 1.0f, 2.0f, 3.0f, 0.8f, 1.25f);

            int count = dispatcher.dispatch(world, 0.5f);

            assertEquals(1, count);
            assertTrue(world.getSoundEvents().isEmpty());
            assertEquals(1, played.size());
            assertEquals(WorldSoundEvent.DOOR_OPEN, played.get(0).soundId());
            assertEquals(0.4f, played.get(0).volume(), 0.0001f);
            assertEquals(1.25f, played.get(0).pitch(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Dispatcher should play direct non-spatial UI sound events")
    void dispatcherPlaysDirectUiSoundEvents() {
        List<PlayedSound> played = new ArrayList<>();
        WorldSoundDispatcher dispatcher = new WorldSoundDispatcher((event, volume) ->
                played.add(new PlayedSound(event.soundId(), volume, event.pitch())));

        assertTrue(dispatcher.play(WorldSoundEvent.uiButtonClick(), 0.5f));
        assertFalse(dispatcher.play(WorldSoundEvent.uiButtonClick(), 0.0f));

        assertEquals(1, played.size());
        assertEquals(WorldSoundEvent.UI_BUTTON_CLICK, played.get(0).soundId());
        assertEquals(0.5f, played.get(0).volume(), 0.0001f);
        assertEquals(1.0f, played.get(0).pitch(), 0.0001f);
    }

    @Test
    @DisplayName("Muted dispatcher should still drain queued world sounds")
    void mutedDispatcherDrainsWithoutPlayback() {
        World world = new World(6291L);
        List<PlayedSound> played = new ArrayList<>();
        WorldSoundDispatcher dispatcher = new WorldSoundDispatcher((event, volume) ->
                played.add(new PlayedSound(event.soundId(), volume, event.pitch())));
        try {
            world.playSound(WorldSoundEvent.DOOR_CLOSE, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f);

            int count = dispatcher.dispatch(world, 0.0f);

            assertEquals(0, count);
            assertTrue(played.isEmpty());
            assertTrue(world.getSoundEvents().isEmpty());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spatial dispatcher should cull sounds outside the Release-style audible radius")
    void spatialDispatcherCullsDistantSounds() {
        World world = new World(6292L);
        List<PlayedSound> played = new ArrayList<>();
        WorldSoundDispatcher dispatcher = new WorldSoundDispatcher((event, volume) ->
                played.add(new PlayedSound(event.soundId(), volume, event.pitch())));
        try {
            world.playSound(WorldSoundEvent.DOOR_OPEN, 15.5f, 64.0f, 0.0f, 1.0f, 1.0f);
            world.playSound(WorldSoundEvent.DOOR_CLOSE, 16.0f, 64.0f, 0.0f, 1.0f, 1.0f);

            int count = dispatcher.dispatch(world, 1.0f, 0.0f, 64.0f, 0.0f);

            assertEquals(1, count);
            assertTrue(world.getSoundEvents().isEmpty());
            assertEquals(1, played.size());
            assertEquals(WorldSoundEvent.DOOR_OPEN, played.get(0).soundId());
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spatial dispatcher should fade sounds across the Release-style audible radius")
    void spatialDispatcherAppliesDistanceRolloff() {
        World world = new World(6296L);
        List<PlayedSound> played = new ArrayList<>();
        WorldSoundDispatcher dispatcher = new WorldSoundDispatcher((event, volume) ->
                played.add(new PlayedSound(event.soundId(), volume, event.pitch())));
        try {
            world.playSound(WorldSoundEvent.DOOR_OPEN, 8.0f, 64.0f, 0.0f, 1.0f, 1.0f);

            int count = dispatcher.dispatch(world, 1.0f, 0.0f, 64.0f, 0.0f);

            assertEquals(1, count);
            assertEquals(1, played.size());
            assertEquals(0.5f, played.get(0).volume(), 0.0001f);
            assertEquals(0.5f, WorldSoundDispatcher.effectiveVolume(
                    new WorldSoundEvent(WorldSoundEvent.DOOR_OPEN, 8.0f, 64.0f, 0.0f, 1.0f, 1.0f),
                    1.0f, 0.0f, 64.0f, 0.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Spatial dispatcher should extend audible range for loud Release-style sounds")
    void spatialDispatcherUsesVolumeScaledAudibleRange() {
        World world = new World(6293L);
        List<PlayedSound> played = new ArrayList<>();
        WorldSoundDispatcher dispatcher = new WorldSoundDispatcher((event, volume) ->
                played.add(new PlayedSound(event.soundId(), volume, event.pitch())));
        try {
            world.playSound(WorldSoundEvent.RECORD_CAT, 40.0f, 64.0f, 0.0f, 4.0f, 1.0f);

            int count = dispatcher.dispatch(world, 0.5f, 0.0f, 64.0f, 0.0f);

            assertEquals(1, count);
            assertEquals(1, played.size());
            assertEquals(WorldSoundEvent.RECORD_CAT, played.get(0).soundId());
            assertEquals(0.1875f, played.get(0).volume(), 0.0001f);
            assertEquals(64.0f, WorldSoundDispatcher.audibleRadius(4.0f), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    private record PlayedSound(String soundId, float volume, float pitch) {
    }
}
