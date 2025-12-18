package com.craftzero.world;

import org.joml.Vector3f;

/**
 * Manages the day/night cycle, including time, sun/moon position, and lighting
 * colors.
 * Standard Minecraft Cycle: 20 minutes = 24000 ticks.
 * Day: 0 - 12000
 * Sunset: 12000 - 13800
 * Night: 13800 - 22200
 * Sunrise: 22200 - 24000
 */
public class DayCycleManager {

    private static final int TICKS_PER_DAY = 24000;
    private static final float TICKS_PER_SECOND = 20.0f;

    // Time tracking
    private float time; // 0 to 24000
    private int days;
    private float timeScale = 1.0f; // Multiplier for debug speedup

    // Output values
    private Vector3f skyColor;
    private Vector3f fogColor;
    private Vector3f lightColor;
    private float ambientIntensity;
    private Vector3f sunDirection;
    private Vector3f moonDirection;
    private int moonPhase; // 0-7

    // Color constants
    private static final Vector3f DAY_SKY = new Vector3f(0.529f, 0.808f, 0.922f);
    private static final Vector3f SUNSET_SKY = new Vector3f(0.9f, 0.5f, 0.3f);
    private static final Vector3f NIGHT_SKY = new Vector3f(0.02f, 0.02f, 0.05f);

    public DayCycleManager() {
        this.time = 6000; // Start at noon
        this.days = 0;
        this.skyColor = new Vector3f();
        this.fogColor = new Vector3f();
        this.lightColor = new Vector3f();
        this.sunDirection = new Vector3f();
        this.moonDirection = new Vector3f();
    }

    public void update(float deltaTime) {
        // Increment time
        time += deltaTime * TICKS_PER_SECOND * timeScale;
        if (time >= TICKS_PER_DAY) {
            time -= TICKS_PER_DAY;
            days++;
            moonPhase = days % 8;
        }

        updateLighting();
    }

    private void updateLighting() {
        float timePercent = time / (float) TICKS_PER_DAY;

        // Calculate sun angle
        // At time 6000 (0.25): noon, sun overhead (Y=1)
        // At time 0 or 24000: sunrise, sun at horizon (Y=0)
        // At time 12000 (0.5): sunset, sun at horizon (Y=0)
        // At time 18000 (0.75): midnight, sun below (Y=-1)
        float angleRad = (float) ((timePercent - 0.25f) * Math.PI * 2.0f);

        sunDirection.x = -(float) Math.sin(angleRad);
        sunDirection.y = (float) Math.cos(angleRad);
        sunDirection.z = 0.2f;
        sunDirection.normalize();

        moonDirection.set(sunDirection).negate();

        // Get sun height for lighting calculations
        float sunHeight = sunDirection.y;

        // Calculate colors and intensity based on sun height
        // Key thresholds (delayed to match sun visual position):
        // sunHeight > 0.0: Day (sun above horizon)
        // sunHeight 0.0 to -0.35: Sunset/Sunrise transition
        // sunHeight < -0.35: Night (sun well below horizon)

        if (sunHeight > 0.0f) {
            // FULL DAY - sun above horizon
            skyColor.set(DAY_SKY);
            fogColor.set(0.6f, 0.7f, 0.8f);
            lightColor.set(1.0f, 1.0f, 0.95f);
            ambientIntensity = 0.4f;
        } else if (sunHeight > -0.35f) {
            // SUNSET/SUNRISE - sun at/near horizon
            // All blocks dim UNIFORMLY - no directional lighting
            float t = (sunHeight + 0.35f) / 0.35f;
            t = clamp(t, 0, 1);
            t = smoothstep(t);

            if (t > 0.5f) {
                // Upper half: day to sunset colors
                float t2 = (t - 0.5f) * 2.0f;
                skyColor.set(SUNSET_SKY).lerp(DAY_SKY, t2);
                // Uniform dimming - reduce ambient as sun sets
                ambientIntensity = 0.25f + 0.15f * t2;
            } else {
                // Lower half: sunset to night colors
                float t2 = t * 2.0f;
                skyColor.set(NIGHT_SKY).lerp(SUNSET_SKY, t2);
                ambientIntensity = 0.15f + 0.10f * t2;
            }

            fogColor.set(skyColor);
            // NO directional light during sunset - all faces dim uniformly
            lightColor.set(0.0f, 0.0f, 0.0f);
        } else {
            // NIGHT - sun well below horizon
            skyColor.set(NIGHT_SKY);
            fogColor.set(0.02f, 0.02f, 0.05f);
            lightColor.set(0.0f, 0.0f, 0.0f);
            ambientIntensity = 0.15f;
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float smoothstep(float t) {
        // Smooth transition curve (ease in/out)
        return t * t * (3 - 2 * t);
    }

    public Vector3f getSkyColor() {
        return skyColor;
    }

    public Vector3f getFogColor() {
        return fogColor;
    }

    public Vector3f getLightColor() {
        return lightColor;
    }

    public float getAmbientIntensity() {
        return ambientIntensity;
    }

    public Vector3f getSunDirection() {
        return sunDirection;
    }

    public Vector3f getMoonDirection() {
        return moonDirection;
    }

    public int getMoonPhase() {
        return moonPhase;
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    /**
     * Get sun brightness for shader (0.0-1.0).
     * Noon = 1.0, Night = 0.15 (moonlight minimum).
     */
    public float getSunBrightness() {
        float sunHeight = sunDirection.y;
        if (sunHeight > 0.0f) {
            // Day: full brightness
            return 1.0f;
        } else if (sunHeight > -0.35f) {
            // Sunset/Sunrise: fade from 1.0 to 0.15
            float t = (sunHeight + 0.35f) / 0.35f; // 0 at -0.35, 1 at 0.0
            return 0.15f + 0.85f * smoothstep(t);
        } else {
            // Night: minimum brightness (moonlight)
            return 0.15f;
        }
    }
}
