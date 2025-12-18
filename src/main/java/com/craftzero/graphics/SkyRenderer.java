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
        float timePercent = dayCycle.getTime() / 24000.0f;
        float angle = (timePercent - 0.25f) * 360.0f;

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
        Matrix4f sunMat = new Matrix4f();
        sunMat.translate(camPos);
        sunMat.rotateZ((float) Math.toRadians(angle));
        sunMat.translate(0, DISTANCE, 0);
        sunMat.rotateX((float) Math.toRadians(90));

        shader.setUniform("modelMatrix", sunMat);
        sunTexture.bind(0);
        sunMesh.render();

        // --- RENDER MOON ---
        Matrix4f moonMat = new Matrix4f();
        moonMat.translate(camPos);
        moonMat.rotateZ((float) Math.toRadians(angle));
        moonMat.translate(0, -DISTANCE, 0);
        moonMat.rotateX((float) Math.toRadians(-90));
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
