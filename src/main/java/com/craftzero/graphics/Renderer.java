
package com.craftzero.graphics;

import com.craftzero.engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

/**
 * Master renderer that manages all rendering operations.
 * Handles shader setup, transformations, and rendering calls.
 */
public class Renderer {

    private ShaderProgram shaderProgram;
    private Matrix4f modelMatrix;

    // Fog settings for atmosphere
    private boolean fogEnabled = true;
    private float fogDensity = 0.007f;
    private Vector3f fogColor = new Vector3f(0.6f, 0.6f, 0.6f); // Grey fog

    // Ambient lighting
    private float ambientLight = 0.3f;
    private Vector3f lightDirection = new Vector3f(0.5f, 1.0f, 0.3f).normalize();
    private Vector3f lightColor = new Vector3f(1.0f, 1.0f, 0.9f);

    public void init() throws Exception {
        // Create shader program
        shaderProgram = new ShaderProgram();
        shaderProgram.createVertexShader(ShaderProgram.loadResource("/shaders/scene.vert"));
        shaderProgram.createFragmentShader(ShaderProgram.loadResource("/shaders/scene.frag"));
        shaderProgram.link();

        // Create uniforms
        shaderProgram.createUniform("projectionMatrix");
        shaderProgram.createUniform("viewMatrix");
        shaderProgram.createUniform("modelMatrix");
        shaderProgram.createUniform("textureSampler");

        // Fog uniforms
        shaderProgram.createUniform("fogEnabled");
        shaderProgram.createUniform("fogDensity");
        shaderProgram.createUniform("fogColor");

        // Lighting uniforms
        shaderProgram.createUniform("ambientLight");
        shaderProgram.createUniform("lightDirection");
        shaderProgram.createUniform("lightColor");

        // Sky Blue background
        glClearColor(0.529f, 0.808f, 0.922f, 1.0f);

        glEnable(GL_DEPTH_TEST);

        // Cull back faces (CCW)
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        // Enable Alpha Blending for transparent blocks (Glass, Water)
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        modelMatrix = new Matrix4f();

        System.out.println("Renderer initialized successfully");
    }

    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void render(Window window, Camera camera, Mesh mesh, Texture texture) {
        render(window, camera, mesh, texture, new Vector3f(0, 0, 0));
    }

    public void render(Window window, Camera camera, Mesh mesh, Texture texture, Vector3f position) {
        if (window.isResized()) {
            glViewport(0, 0, window.getWidth(), window.getHeight());
            camera.setAspectRatio(window.getWidth(), window.getHeight());
            window.setResized(false);
        }

        shaderProgram.bind();

        // Set matrices
        shaderProgram.setUniform("projectionMatrix", camera.getProjectionMatrix());
        shaderProgram.setUniform("viewMatrix", camera.getViewMatrix());

        // Model matrix with position
        modelMatrix.identity().translate(position);
        shaderProgram.setUniform("modelMatrix", modelMatrix);

        // Set texture
        shaderProgram.setUniform("textureSampler", 0);
        texture.bind(0);

        // Set fog
        shaderProgram.setUniform("fogEnabled", fogEnabled);
        shaderProgram.setUniform("fogDensity", fogDensity);
        shaderProgram.setUniform("fogColor", fogColor);

        // Set lighting
        shaderProgram.setUniform("ambientLight", ambientLight);
        shaderProgram.setUniform("lightDirection", lightDirection);
        shaderProgram.setUniform("lightColor", lightColor);

        // Render mesh
        mesh.render();

        texture.unbind();
        shaderProgram.unbind();
    }

    /**
     * Begin batch rendering (bind shader once).
     */
    public void beginRender(Camera camera) {
        shaderProgram.bind();

        shaderProgram.setUniform("projectionMatrix", camera.getProjectionMatrix());
        shaderProgram.setUniform("viewMatrix", camera.getViewMatrix());
        shaderProgram.setUniform("textureSampler", 0);

        // Set fog
        shaderProgram.setUniform("fogEnabled", fogEnabled);
        shaderProgram.setUniform("fogDensity", fogDensity);
        shaderProgram.setUniform("fogColor", fogColor);

        // Set lighting
        shaderProgram.setUniform("ambientLight", ambientLight);
        shaderProgram.setUniform("lightDirection", lightDirection);
        shaderProgram.setUniform("lightColor", lightColor);
    }

    /**
     * Render a mesh at a specific position (during batch).
     */
    public void renderMesh(Mesh mesh, Vector3f position) {
        modelMatrix.identity().translate(position);
        shaderProgram.setUniform("modelMatrix", modelMatrix);
        mesh.render();
    }

    /**
     * Render a mesh with identity model matrix (during batch).
     */
    public void renderMesh(Mesh mesh) {
        modelMatrix.identity();
        shaderProgram.setUniform("modelMatrix", modelMatrix);
        mesh.render();
    }

    /**
     * End batch rendering.
     */
    public void endRender() {
        shaderProgram.unbind();
    }

    public ShaderProgram getShaderProgram() {
        return shaderProgram;
    }

    public void setFogEnabled(boolean enabled) {
        this.fogEnabled = enabled;
    }

    public void setFogDensity(float density) {
        this.fogDensity = density;
    }

    public void setFogColor(Vector3f color) {
        this.fogColor = color;
    }

    public void setAmbientLight(float ambient) {
        this.ambientLight = ambient;
    }

    public void setDepthMask(boolean enabled) {
        glDepthMask(enabled);
    }

    public void setLightDirection(Vector3f direction) {
        this.lightDirection = direction.normalize();
    }

    public void cleanup() {
        if (shaderProgram != null) {
            shaderProgram.cleanup();
        }
    }
}
