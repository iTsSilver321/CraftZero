package com.craftzero.graphics;

import com.craftzero.world.DayCycleManager;
import com.craftzero.world.Dimension;
import org.joml.Vector3f;

/**
 * Dimension-specific render environment decisions that do not require an
 * OpenGL context.
 */
public final class DimensionRenderEnvironment {
    private static final Vector3f NETHER_FOG = new Vector3f(0.20f, 0.03f, 0.03f);
    private static final Vector3f END_CLEAR = new Vector3f(0.0f, 0.0f, 0.0f);
    private static final Vector3f END_FOG = new Vector3f(0.015f, 0.010f, 0.025f);
    private static final Vector3f LIGHTNING_FLASH = new Vector3f(0.92f, 0.96f, 1.0f);

    private DimensionRenderEnvironment() {
    }

    public enum CameraFluid {
        NONE,
        WATER,
        LAVA
    }

    public record Snapshot(
            Vector3f clearColor,
            Vector3f fogColor,
            float fogDensity,
            boolean distanceFog,
            boolean renderCelestialSky,
            boolean renderClouds,
            float ambientIntensity,
            float sunBrightness,
            float cloudBrightnessMultiplier) {
    }

    public static Snapshot snapshot(Dimension dimension, DayCycleManager dayCycle, CameraFluid fluid,
            float waterDepth, float rainStrength, float thunderStrength, float lightningFlashStrength) {
        Dimension resolvedDimension = dimension == null ? Dimension.OVERWORLD : dimension;
        DayCycleManager resolvedDayCycle = dayCycle == null ? new DayCycleManager() : dayCycle;
        CameraFluid resolvedFluid = fluid == null ? CameraFluid.NONE : fluid;

        if (resolvedFluid == CameraFluid.WATER) {
            Vector3f color = waterFogColor(waterDepth);
            return new Snapshot(color, new Vector3f(color),
                    0.12f + clamp01(waterDepth) * 0.18f,
                    false, false, false,
                    baseAmbientIntensity(resolvedDimension, resolvedDayCycle, 1.0f),
                    baseSunBrightness(resolvedDimension, resolvedDayCycle, 1.0f),
                    0.0f);
        }
        if (resolvedFluid == CameraFluid.LAVA) {
            return new Snapshot(
                    new Vector3f(0.55f, 0.16f, 0.02f),
                    new Vector3f(0.70f, 0.22f, 0.03f),
                    0.42f,
                    false, false, false,
                    baseAmbientIntensity(resolvedDimension, resolvedDayCycle, 1.0f),
                    baseSunBrightness(resolvedDimension, resolvedDayCycle, 1.0f),
                    0.0f);
        }

        if (resolvedDimension == Dimension.NETHER) {
            return new Snapshot(
                    new Vector3f(NETHER_FOG),
                    new Vector3f(NETHER_FOG),
                    0.055f,
                    false, false, false,
                    0.36f,
                    0.55f,
                    0.0f);
        }
        if (resolvedDimension == Dimension.THE_END) {
            return new Snapshot(
                    new Vector3f(END_CLEAR),
                    new Vector3f(END_FOG),
                    0.007f,
                    true, false, false,
                    0.34f,
                    0.42f,
                    0.0f);
        }

        float weatherBrightness = weatherBrightnessFactor(rainStrength, thunderStrength, lightningFlashStrength);
        return new Snapshot(
                weatherAdjustedColor(resolvedDayCycle.getSkyColor(), rainStrength, thunderStrength,
                        lightningFlashStrength),
                weatherAdjustedColor(resolvedDayCycle.getFogColor(), rainStrength, thunderStrength,
                        lightningFlashStrength),
                0.007f,
                true, true, true,
                baseAmbientIntensity(resolvedDimension, resolvedDayCycle, weatherBrightness),
                baseSunBrightness(resolvedDimension, resolvedDayCycle, weatherBrightness),
                weatherBrightness);
    }

    static float weatherBrightnessFactor(float rainStrength, float thunderStrength, float lightningFlashStrength) {
        float rain = clamp01(rainStrength);
        float thunder = clamp01(thunderStrength);
        float flash = clamp01(lightningFlashStrength);
        float weatherBrightness = (1.0f - rain * 0.5f) * (1.0f - thunder * 0.5f);
        return Math.min(1.0f, weatherBrightness + (1.0f - weatherBrightness) * flash);
    }

    private static Vector3f weatherAdjustedColor(Vector3f base, float rainStrength, float thunderStrength,
            float lightningFlashStrength) {
        float flash = clamp01(lightningFlashStrength);
        Vector3f adjusted = new Vector3f(base)
                .mul(weatherBrightnessFactor(rainStrength, thunderStrength, flash));
        return adjusted.lerp(new Vector3f(LIGHTNING_FLASH), flash * 0.65f);
    }

    private static float baseAmbientIntensity(Dimension dimension, DayCycleManager dayCycle, float weatherBrightness) {
        if (dimension == Dimension.OVERWORLD) {
            return dayCycle.getAmbientIntensity() * weatherBrightness;
        }
        if (dimension == Dimension.NETHER) {
            return 0.36f;
        }
        return 0.34f;
    }

    private static float baseSunBrightness(Dimension dimension, DayCycleManager dayCycle, float weatherBrightness) {
        if (dimension == Dimension.OVERWORLD) {
            return dayCycle.getSunBrightness() * weatherBrightness;
        }
        if (dimension == Dimension.NETHER) {
            return 0.55f;
        }
        return 0.42f;
    }

    private static Vector3f waterFogColor(float depthFactor) {
        float t = clamp01(depthFactor);
        float r = 0.10f * (1.0f - t) + 0.02f * t;
        float g = 0.40f * (1.0f - t) + 0.12f * t;
        float b = 0.82f * (1.0f - t) + 0.42f * t;
        return new Vector3f(r, g, b);
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
