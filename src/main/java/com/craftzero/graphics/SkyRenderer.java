package com.craftzero.graphics;

import com.craftzero.world.DayCycleManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL11.*;

/**
 * Renders sun and moon with additive blending to hide black background.
 */
public class SkyRenderer {

    private static final float SIZE = 30.0f;
    private static final float DISTANCE = 100.0f;
    private static final int MOON_PHASE_COUNT = 8;
    private static final int MOON_PHASE_COLUMNS = 4;
    private static final int MOON_PHASE_ROWS = 2;

    private Mesh sunMesh;
    private final Mesh[] moonPhaseMeshes = new Mesh[MOON_PHASE_COUNT];
    private Texture sunTexture;
    private Texture moonTexture;

    public void init() throws Exception {
        float[] vertices = new float[] {
                -SIZE, SIZE, 0,
                -SIZE, -SIZE, 0,
                SIZE, -SIZE, 0,
                SIZE, SIZE, 0
        };

        float[] texCoords = new float[] {
                0, 0,
                0, 1,
                1, 1,
                1, 0
        };

        int[] indices = new int[] { 0, 1, 3, 3, 1, 2 };

        float[] normals = new float[] {
                0, 0, 1,
                0, 0, 1,
                0, 0, 1,
                0, 0, 1
        };

        sunMesh = new Mesh(vertices, texCoords, normals, indices);
        for (int phase = 0; phase < moonPhaseMeshes.length; phase++) {
            moonPhaseMeshes[phase] = new Mesh(vertices, moonPhaseTexCoords(phase), normals, indices);
        }

        sunTexture = new Texture("/textures/terrain/sun.png");
        moonTexture = new Texture("/textures/terrain/moon_phases.png");
    }

    public void render(Renderer renderer, DayCycleManager dayCycle, Camera camera) {
        ShaderProgram shader = renderer.getShaderProgram();
        shader.bind();

        Vector3f camPos = camera.getPosition();

        // Setup for sky rendering
        glDepthMask(false);
        glDisable(GL_CULL_FACE);

        // ADDITIVE BLENDING - Makes black pixels invisible!
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);

        // Disable fog and set full brightness
        shader.setUniform("fogEnabled", false);
        shader.setUniform("ambientLight", 1.0f);

        // --- RENDER SUN ---
        // Sun is always at fixed DISTANCE from camera, but the angle (direction)
        // is based on time of day, not camera position. This ensures:
        // 1. Sun is never reachable (fixed distance from camera)
        // 2. Sun angle doesn't change when player flies up/down
        Vector3f sunDir = dayCycle.getSunDirection();
        Matrix4f sunMat = new Matrix4f();
        sunMat.translate(
                camPos.x + sunDir.x * DISTANCE,
                camPos.y + sunDir.y * DISTANCE,
                camPos.z + sunDir.z * DISTANCE);
        // Billboard facing - rotate to face camera
        sunMat.rotateY((float) Math.atan2(sunDir.x, sunDir.z));
        sunMat.rotateX((float) Math.asin(-sunDir.y));

        shader.setUniform("modelMatrix", sunMat);
        sunTexture.bind(0);
        sunMesh.render();

        // --- RENDER MOON ---
        Vector3f moonDir = dayCycle.getMoonDirection();
        Matrix4f moonMat = new Matrix4f();
        moonMat.translate(
                camPos.x + moonDir.x * DISTANCE,
                camPos.y + moonDir.y * DISTANCE,
                camPos.z + moonDir.z * DISTANCE);
        moonMat.rotateY((float) Math.atan2(moonDir.x, moonDir.z));
        moonMat.rotateX((float) Math.asin(-moonDir.y));
        moonMat.rotateZ((float) Math.toRadians(180));

        shader.setUniform("modelMatrix", moonMat);
        moonTexture.bind(0);
        moonPhaseMeshes[normalizedMoonPhase(dayCycle.getMoonPhase())].render();

        // Restore state - keep blend ENABLED for water transparency
        glDepthMask(true);
        glEnable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        // Note: We keep GL_BLEND enabled for water

        // Restore shader uniforms
        shader.setUniform("fogEnabled", true);
        shader.setUniform("ambientLight", dayCycle.getAmbientIntensity());

        shader.unbind();
    }

    public void cleanup() {
        if (sunMesh != null)
            sunMesh.cleanup();
        for (Mesh moonPhaseMesh : moonPhaseMeshes) {
            if (moonPhaseMesh != null)
                moonPhaseMesh.cleanup();
        }
        if (sunTexture != null)
            sunTexture.cleanup();
        if (moonTexture != null)
            moonTexture.cleanup();
    }

    static float[] moonPhaseTexCoords(int phase) {
        int normalized = normalizedMoonPhase(phase);
        int column = normalized % MOON_PHASE_COLUMNS;
        int row = normalized / MOON_PHASE_COLUMNS;
        float u0 = column / (float) MOON_PHASE_COLUMNS;
        float u1 = (column + 1) / (float) MOON_PHASE_COLUMNS;
        float v0 = row / (float) MOON_PHASE_ROWS;
        float v1 = (row + 1) / (float) MOON_PHASE_ROWS;
        return new float[] {
                u0, v0,
                u0, v1,
                u1, v1,
                u1, v0
        };
    }

    static int normalizedMoonPhase(int phase) {
        return Math.floorMod(phase, MOON_PHASE_COUNT);
    }
}
