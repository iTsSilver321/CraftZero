package com.craftzero.graphics;

import com.craftzero.graphics.DimensionRenderEnvironment.CameraFluid;
import com.craftzero.world.DayCycleManager;
import com.craftzero.world.Dimension;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DimensionRenderEnvironmentTest {
    @Test
    @DisplayName("Overworld environment should keep weathered celestial sky and clouds")
    void overworldKeepsWeatheredSkyAndClouds() {
        DayCycleManager dayCycle = new DayCycleManager();
        float weatherBrightness = 0.375f;

        DimensionRenderEnvironment.Snapshot snapshot = DimensionRenderEnvironment.snapshot(
                Dimension.OVERWORLD, dayCycle, CameraFluid.NONE,
                0.0f, 1.0f, 0.5f, 0.0f);

        assertTrue(snapshot.renderCelestialSky());
        assertTrue(snapshot.renderClouds());
        assertTrue(snapshot.distanceFog());
        assertEquals(0.007f, snapshot.fogDensity(), 0.0001f);
        assertEquals(dayCycle.getAmbientIntensity() * weatherBrightness, snapshot.ambientIntensity(), 0.0001f);
        assertEquals(dayCycle.getSunBrightness() * weatherBrightness, snapshot.sunBrightness(), 0.0001f);
        assertEquals(weatherBrightness, snapshot.cloudBrightnessMultiplier(), 0.0001f);
        assertVector(new Vector3f(dayCycle.getSkyColor()).mul(weatherBrightness), snapshot.clearColor());
        assertVector(new Vector3f(dayCycle.getFogColor()).mul(weatherBrightness), snapshot.fogColor());
    }

    @Test
    @DisplayName("Nether environment should use red dense fog without Overworld sky or weather")
    void netherUsesRedDenseFogWithoutSkyOrWeather() {
        DayCycleManager dayCycle = new DayCycleManager();
        DimensionRenderEnvironment.Snapshot noon = DimensionRenderEnvironment.snapshot(
                Dimension.NETHER, dayCycle, CameraFluid.NONE,
                0.0f, 1.0f, 1.0f, 1.0f);
        dayCycle.setTime(18000.0f);
        DimensionRenderEnvironment.Snapshot midnight = DimensionRenderEnvironment.snapshot(
                Dimension.NETHER, dayCycle, CameraFluid.NONE,
                0.0f, 0.0f, 0.0f, 0.0f);

        assertFalse(noon.renderCelestialSky());
        assertFalse(noon.renderClouds());
        assertFalse(noon.distanceFog());
        assertEquals(0.055f, noon.fogDensity(), 0.0001f);
        assertVector(new Vector3f(0.20f, 0.03f, 0.03f), noon.clearColor());
        assertVector(new Vector3f(0.20f, 0.03f, 0.03f), noon.fogColor());
        assertEquals(noon.ambientIntensity(), midnight.ambientIntensity(), 0.0001f);
        assertEquals(noon.sunBrightness(), midnight.sunBrightness(), 0.0001f);
        assertVector(noon.clearColor(), midnight.clearColor());
        assertVector(noon.fogColor(), midnight.fogColor());
    }

    @Test
    @DisplayName("End environment should stay dark and cloudless")
    void endStaysDarkAndCloudless() {
        DayCycleManager dayCycle = new DayCycleManager();

        DimensionRenderEnvironment.Snapshot snapshot = DimensionRenderEnvironment.snapshot(
                Dimension.THE_END, dayCycle, CameraFluid.NONE,
                0.0f, 1.0f, 1.0f, 1.0f);

        assertFalse(snapshot.renderCelestialSky());
        assertFalse(snapshot.renderClouds());
        assertTrue(snapshot.distanceFog());
        assertVector(new Vector3f(0.0f, 0.0f, 0.0f), snapshot.clearColor());
        assertVector(new Vector3f(0.015f, 0.010f, 0.025f), snapshot.fogColor());
        assertEquals(0.34f, snapshot.ambientIntensity(), 0.0001f);
        assertEquals(0.42f, snapshot.sunBrightness(), 0.0001f);
        assertEquals(0.0f, snapshot.cloudBrightnessMultiplier(), 0.0001f);
    }

    @Test
    @DisplayName("Fluid camera environment should suppress sky rendering")
    void fluidsSuppressSkyRendering() {
        DayCycleManager dayCycle = new DayCycleManager();

        DimensionRenderEnvironment.Snapshot water = DimensionRenderEnvironment.snapshot(
                Dimension.OVERWORLD, dayCycle, CameraFluid.WATER,
                1.0f, 0.0f, 0.0f, 0.0f);
        DimensionRenderEnvironment.Snapshot lava = DimensionRenderEnvironment.snapshot(
                Dimension.NETHER, dayCycle, CameraFluid.LAVA,
                0.0f, 0.0f, 0.0f, 0.0f);

        assertFalse(water.renderCelestialSky());
        assertFalse(water.renderClouds());
        assertFalse(water.distanceFog());
        assertEquals(0.30f, water.fogDensity(), 0.0001f);
        assertVector(new Vector3f(0.02f, 0.12f, 0.42f), water.clearColor());
        assertVector(water.clearColor(), water.fogColor());

        assertFalse(lava.renderCelestialSky());
        assertFalse(lava.renderClouds());
        assertFalse(lava.distanceFog());
        assertEquals(0.42f, lava.fogDensity(), 0.0001f);
        assertVector(new Vector3f(0.55f, 0.16f, 0.02f), lava.clearColor());
        assertVector(new Vector3f(0.70f, 0.22f, 0.03f), lava.fogColor());
    }

    private static void assertVector(Vector3f expected, Vector3f actual) {
        assertEquals(expected.x, actual.x, 0.0001f);
        assertEquals(expected.y, actual.y, 0.0001f);
        assertEquals(expected.z, actual.z, 0.0001f);
    }
}
