package com.craftzero.graphics;

import com.craftzero.world.DayCycleManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL11.*;

/**
 * Renders sun and moon with additive blending to hide black background.
 */
public class SkyRenderer {

    private Mesh sunMesh;
    private Mesh moonMesh;
    private Texture sunTexture;
    private Texture moonTexture;

    private static final float SIZE = 30.0f;
    private static final float DISTANCE = 100.0f;

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
        moonMesh = new Mesh(vertices, texCoords, normals, indices);

        sunTexture = new Texture("/textures/terrain/sun.png");
        moonTexture = new Texture("/textures/terrain/moon.png");
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
        moonMesh.render();

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
        if (moonMesh != null)
            moonMesh.cleanup();
        if (sunTexture != null)
            sunTexture.cleanup();
        if (moonTexture != null)
            moonTexture.cleanup();
    }
}
