package com.craftzero.engine;

/**
 * Timer class for calculating delta time between frames.
 * Provides frame-independent movement and timing utilities.
 */
public class Timer {
    
    private double lastLoopTime;
    private float deltaTime;
    private float accumulator;
    private int fps;
    private int fpsCount;
    private float fpsTimer;
    
    public void init() {
        lastLoopTime = getTime();
        deltaTime = 0;
        accumulator = 0;
        fps = 0;
        fpsCount = 0;
        fpsTimer = 0;
    }
    
    public double getTime() {
        return System.nanoTime() / 1_000_000_000.0;
    }
    
    public float getDeltaTime() {
        return deltaTime;
    }
    
    public void update() {
        double currentTime = getTime();
        deltaTime = (float) (currentTime - lastLoopTime);
        lastLoopTime = currentTime;
        
        // Clamp delta time to prevent spiral of death
        if (deltaTime > 0.25f) {
            deltaTime = 0.25f;
        }
        
        // FPS counter
        fpsTimer += deltaTime;
        fpsCount++;
        if (fpsTimer >= 1.0f) {
            fps = fpsCount;
            fpsCount = 0;
            fpsTimer = 0;
        }
    }
    
    public int getFps() {
        return fps;
    }
    
    public float getLastLoopTime() {
        return (float) lastLoopTime;
    }
    
    /**
     * For fixed timestep physics updates.
     * Accumulates time and returns how many fixed updates should run.
     */
    public int getFixedUpdates(float fixedDelta) {
        accumulator += deltaTime;
        int updates = 0;
        while (accumulator >= fixedDelta) {
            accumulator -= fixedDelta;
            updates++;
        }
        return updates;
    }
    
    /**
     * Returns interpolation alpha for rendering between physics states.
     */
    public float getAlpha(float fixedDelta) {
        return accumulator / fixedDelta;
    }
}
