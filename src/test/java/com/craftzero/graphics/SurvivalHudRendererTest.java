package com.craftzero.graphics;

import com.craftzero.inventory.ItemType;
import com.craftzero.inventory.MapItemData;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurvivalHudRendererTest {
    @Test
    @DisplayName("Held map layout should use the full Release-era 128x128 map on normal windows")
    void heldMapLayoutUsesFullReleaseMapWhenSpaceAllows() {
        SurvivalHudRenderer.HeldMapLayout layout = SurvivalHudRenderer.heldMapLayout(1280, 720, 660);

        assertEquals(MapItemData.MAP_SIZE, layout.displayPixels());
        assertEquals(1, layout.sourceStep());
        assertTrue(layout.cellSize() >= 2);
        assertTrue(layout.mapSize() >= 256);
        assertTrue(layout.frameY() >= 18);
        assertTrue(layout.bottomY() < 660);
    }

    @Test
    @DisplayName("Held map layout should keep the complete map visible on short windows")
    void heldMapLayoutKeepsCompleteMapVisibleOnShortWindows() {
        SurvivalHudRenderer.HeldMapLayout layout = SurvivalHudRenderer.heldMapLayout(640, 360, 300);

        assertEquals(MapItemData.MAP_SIZE, layout.displayPixels());
        assertEquals(1, layout.sourceStep());
        assertEquals(1, layout.cellSize());
        assertTrue(layout.frameY() >= 18);
        assertTrue(layout.bottomY() < 300);
    }

    @Test
    @DisplayName("Held map player marker should point in the stored Release-era map direction")
    void heldMapMarkerVectorUsesStoredRotation() {
        SurvivalHudRenderer.MarkerVector north = SurvivalHudRenderer.markerVector(0, 6);
        SurvivalHudRenderer.MarkerVector east = SurvivalHudRenderer.markerVector(4, 6);
        SurvivalHudRenderer.MarkerVector south = SurvivalHudRenderer.markerVector(8, 6);
        SurvivalHudRenderer.MarkerVector west = SurvivalHudRenderer.markerVector(12, 6);

        assertEquals(0, north.tipX());
        assertTrue(north.tipY() < 0);
        assertTrue(east.tipX() > 0);
        assertEquals(0, east.tipY());
        assertEquals(0, south.tipX());
        assertTrue(south.tipY() > 0);
        assertTrue(west.tipX() < 0);
        assertEquals(0, west.tipY());
    }

    @Test
    @DisplayName("Held map player marker should clamp same-dimension off-map players to the map edge")
    void heldMapMarkerClampsOffMapPlayersToEdge() {
        SurvivalHudRenderer.HeldMapLayout layout = new SurvivalHudRenderer.HeldMapLayout(10, 20, 128, 1, 2, 8);
        MapItemData.View inside = mapView(64, 65, 4);
        MapItemData.View offMap = mapView(180, -20, 4);
        MapItemData.View hidden = mapView(-1, -1, -1);

        SurvivalHudRenderer.HeldMapMarker insideMarker = SurvivalHudRenderer.heldMapMarker(layout, inside);
        SurvivalHudRenderer.HeldMapMarker edgeMarker = SurvivalHudRenderer.heldMapMarker(layout, offMap);

        assertEquals(10 + 64 * 2, insideMarker.x());
        assertEquals(20 + 65 * 2, insideMarker.y());
        assertFalse(insideMarker.edgeClamped());
        assertEquals(10 + 127 * 2, edgeMarker.x());
        assertEquals(20, edgeMarker.y());
        assertTrue(edgeMarker.edgeClamped());
        assertNull(SurvivalHudRenderer.heldMapMarker(layout, hidden));
    }

    @Test
    @DisplayName("Held map hand pose should mirror angled Release-era grips around the map")
    void heldMapHandPoseMirrorsSourceStyleGrips() {
        SurvivalHudRenderer.HeldMapLayout layout = SurvivalHudRenderer.heldMapLayout(1280, 720, 660);
        SurvivalHudRenderer.HeldMapHandPose pose = SurvivalHudRenderer.heldMapHandPose(layout);

        float centerX = layout.mapX() + layout.mapSize() / 2.0f;

        assertTrue(pose.leftSleeve().minX() < layout.mapX());
        assertTrue(pose.leftSleeve().maxX() < centerX);
        assertTrue(pose.rightSleeve().maxX() > layout.mapX() + layout.mapSize());
        assertTrue(pose.rightSleeve().minX() > centerX);
        assertEquals(centerX - pose.leftSleeve().centerX(), pose.rightSleeve().centerX() - centerX, 0.001f);

        assertTrue(pose.leftSleeve().maxY() > layout.bottomY());
        assertTrue(pose.rightSleeve().maxY() > layout.bottomY());
        assertTrue(pose.leftHand().minY() < layout.bottomY());
        assertTrue(pose.leftHand().maxY() > layout.mapY() + layout.mapSize() - layout.frame());
        assertEquals(centerX - pose.leftHand().centerX(), pose.rightHand().centerX() - centerX, 0.001f);
    }

    @Test
    @DisplayName("First-person fire overlay should use mirrored lower-screen flame quads")
    void firstPersonFireOverlayUsesMirroredLowerScreenFlames() {
        List<SurvivalHudRenderer.HudQuad> quads = SurvivalHudRenderer.firstPersonFireOverlayQuads(1280, 720);
        assertEquals(2, quads.size());

        SurvivalHudRenderer.HudQuad left = quads.get(0);
        SurvivalHudRenderer.HudQuad right = quads.get(1);
        float centerX = 640.0f;

        assertTrue(left.centerX() < centerX);
        assertTrue(right.centerX() > centerX);
        assertEquals(centerX - left.centerX(), right.centerX() - centerX, 0.001f);
        assertEquals(centerX - left.minX(), right.maxX() - centerX, 0.001f);
        assertEquals(centerX - left.maxX(), right.minX() - centerX, 0.001f);
        assertTrue(left.minY() > 300.0f);
        assertTrue(left.maxY() > 720.0f);
        assertTrue(right.minY() > 300.0f);
        assertTrue(right.maxY() > 720.0f);
    }

    @Test
    @DisplayName("First-person fire overlay should stay visible on short windows")
    void firstPersonFireOverlayKeepsVisibleFlamesOnShortWindows() {
        List<SurvivalHudRenderer.HudQuad> quads = SurvivalHudRenderer.firstPersonFireOverlayQuads(320, 180);

        assertEquals(2, quads.size());
        for (SurvivalHudRenderer.HudQuad quad : quads) {
            assertTrue(quad.minY() < 180.0f);
            assertTrue(quad.maxY() > 180.0f);
            assertTrue(quad.maxX() > 0.0f);
            assertTrue(quad.minX() < 320.0f);
        }
    }

    @Test
    @DisplayName("Status-effect HUD entries should filter, sort, and expose player-visible labels")
    void statusEffectHudEntriesFilterSortAndExposeLabels() {
        List<SurvivalHudRenderer.StatusEffectHudEntry> entries = SurvivalHudRenderer.statusEffectHudEntries(List.of(
                new StatusEffectInstance(StatusEffectType.STRENGTH, 1234, 0),
                new StatusEffectInstance(StatusEffectType.POISON, 0, 0),
                new StatusEffectInstance(StatusEffectType.SPEED, 400, 1)), 320, 8);

        assertEquals(2, entries.size());

        SurvivalHudRenderer.StatusEffectHudEntry speed = entries.get(0);
        assertEquals(StatusEffectType.SPEED, speed.type());
        assertEquals(280, speed.x());
        assertEquals(8, speed.y());
        assertEquals("Speed", speed.displayName());
        assertEquals("0:20", speed.durationText());
        assertEquals("II", speed.amplifierText());
        assertEquals(ItemType.SUGAR, speed.iconItem());

        SurvivalHudRenderer.StatusEffectHudEntry strength = entries.get(1);
        assertEquals(StatusEffectType.STRENGTH, strength.type());
        assertEquals(280, strength.x());
        assertEquals(44, strength.y());
        assertEquals("Strength", strength.displayName());
        assertEquals("1:02", strength.durationText());
        assertEquals("", strength.amplifierText());
        assertEquals(ItemType.BLAZE_POWDER, strength.iconItem());
    }

    @Test
    @DisplayName("Status-effect HUD should warn and shrink duration bars for expiring effects")
    void statusEffectHudWarnsForExpiringEffects() {
        assertEquals(1.0f, SurvivalHudRenderer.statusEffectWarningAlpha(201), 0.001f);
        assertEquals(0.45f, SurvivalHudRenderer.statusEffectWarningAlpha(195), 0.001f);
        assertEquals(1.0f, SurvivalHudRenderer.statusEffectWarningAlpha(190), 0.001f);

        assertEquals(26, SurvivalHudRenderer.statusEffectDurationBarWidth(240, 26));
        assertEquals(13, SurvivalHudRenderer.statusEffectDurationBarWidth(100, 26));
        assertEquals(1, SurvivalHudRenderer.statusEffectDurationBarWidth(1, 26));
        assertEquals(0, SurvivalHudRenderer.statusEffectDurationBarWidth(0, 26));
    }

    private static MapItemData.View mapView(int playerPixelX, int playerPixelZ, int playerRotation) {
        return new MapItemData.View(true, 0, 0, MapItemData.DEFAULT_SCALE, 0,
                playerPixelX, playerPixelZ, playerRotation, new byte[MapItemData.MAP_SIZE * MapItemData.MAP_SIZE]);
    }
}
