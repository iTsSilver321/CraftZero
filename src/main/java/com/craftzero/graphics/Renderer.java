
package com.craftzero.graphics;

import com.craftzero.engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

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
    private int fogMode = 0;
    private float fogStart = 64.0f;
    private float fogEnd = 128.0f;
    private Vector3f fogColor = new Vector3f(0.6f, 0.6f, 0.6f); // Grey fog

    // Ambient lighting
    private float ambientLight = 0.3f;
    private Vector3f lightDirection = new Vector3f(0.5f, 1.0f, 0.3f).normalize();
    private Vector3f lightColor = new Vector3f(1.0f, 1.0f, 0.9f);
    private float sunBrightness = 1.0f; // Day/night sky light multiplier
    private boolean anaglyphColorCorrection;

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
        shaderProgram.createUniform("fogMode");
        shaderProgram.createUniform("fogStart");
        shaderProgram.createUniform("fogEnd");
        shaderProgram.createUniform("fogColor");

        // Lighting uniforms
        shaderProgram.createUniform("ambientLight");
        shaderProgram.createUniform("lightDirection");
        shaderProgram.createUniform("lightColor");
        shaderProgram.createUniform("sunBrightness"); // Day/night multiplier
        shaderProgram.createUniform("entityBrightness"); // Entity lighting override
        shaderProgram.createUniform("entityTint"); // Entity texture tint, used for colored sheep fleece
        shaderProgram.createUniform("hurtFlash"); // Red tint when entity takes damage
        shaderProgram.createUniform("hurtFlashColor"); // Flash overlay color for hurt/fuse effects
        shaderProgram.createUniform("alphaCutoff"); // Alpha-test threshold for cutout geometry
        shaderProgram.createUniform("glintSampler"); // Release-era enchanted item overlay texture
        shaderProgram.createUniform("glintMode"); // Draw animated item glint instead of base shading
        shaderProgram.createUniform("glintPass"); // Glint has two angled additive sweeps
        shaderProgram.createUniform("glintPhase"); // Shared animated scroll phase
        shaderProgram.createUniform("glintColor");
        shaderProgram.createUniform("glintAlpha");
        shaderProgram.createUniform("anaglyphColorCorrection");
        shaderProgram.createUniform("solidColorMode"); // Solid overlay primitives for dynamic item needles
        shaderProgram.createUniform("solidColor");

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
        if (window == null || camera == null || mesh == null || texture == null) {
            return;
        }
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
        modelMatrix.identity().translate(safeVector(position, new Vector3f()));
        shaderProgram.setUniform("modelMatrix", modelMatrix);

        // Set texture
        shaderProgram.setUniform("textureSampler", 0);
        texture.bind(0);

        // Set fog
        shaderProgram.setUniform("fogEnabled", fogEnabled);
        shaderProgram.setUniform("fogDensity", fogDensity);
        shaderProgram.setUniform("fogMode", fogMode);
        shaderProgram.setUniform("fogStart", fogStart);
        shaderProgram.setUniform("fogEnd", fogEnd);
        shaderProgram.setUniform("fogColor", fogColor);

        // Set lighting
        shaderProgram.setUniform("ambientLight", ambientLight);
        shaderProgram.setUniform("lightDirection", lightDirection);
        shaderProgram.setUniform("lightColor", lightColor);
        shaderProgram.setUniform("entityTint", new Vector3f(1.0f, 1.0f, 1.0f));
        shaderProgram.setUniform("hurtFlashColor", new Vector3f(1.0f, 0.4f, 0.4f));
        shaderProgram.setUniform("hurtFlash", 0.0f);
        shaderProgram.setUniform("glintSampler", 1);
        shaderProgram.setUniform("glintMode", false);
        shaderProgram.setUniform("glintPass", 0);
        shaderProgram.setUniform("glintPhase", 0.0f);
        float[] glintColor = EnchantedItemVisuals.glintColor();
        shaderProgram.setUniform("glintColor", new Vector3f(glintColor[0], glintColor[1], glintColor[2]));
        shaderProgram.setUniform("glintAlpha", glintColor.length >= 4 ? glintColor[3] : 0.58f);
        shaderProgram.setUniform("anaglyphColorCorrection", anaglyphColorCorrection);
        shaderProgram.setUniform("solidColorMode", false);
        shaderProgram.setUniform("solidColor", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));

        // Render mesh
        mesh.render();

        texture.unbind();
        shaderProgram.unbind();
    }

    /**
     * Begin batch rendering (bind shader once).
     */
    public void beginRender(Camera camera) {
        if (camera == null) {
            return;
        }
        shaderProgram.bind();

        shaderProgram.setUniform("projectionMatrix", camera.getProjectionMatrix());
        shaderProgram.setUniform("viewMatrix", camera.getViewMatrix());
        shaderProgram.setUniform("textureSampler", 0);

        // Set fog
        shaderProgram.setUniform("fogEnabled", fogEnabled);
        shaderProgram.setUniform("fogDensity", fogDensity);
        shaderProgram.setUniform("fogMode", fogMode);
        shaderProgram.setUniform("fogStart", fogStart);
        shaderProgram.setUniform("fogEnd", fogEnd);
        shaderProgram.setUniform("fogColor", fogColor);

        // Set lighting
        shaderProgram.setUniform("ambientLight", ambientLight);
        shaderProgram.setUniform("lightDirection", lightDirection);
        shaderProgram.setUniform("lightColor", lightColor);
        shaderProgram.setUniform("sunBrightness", sunBrightness);
        shaderProgram.setUniform("entityBrightness", 0.0f); // Default: use vertex colors
        shaderProgram.setUniform("entityTint", new Vector3f(1.0f, 1.0f, 1.0f));
        shaderProgram.setUniform("hurtFlashColor", new Vector3f(1.0f, 0.4f, 0.4f));
        shaderProgram.setUniform("hurtFlash", 0.0f);
        shaderProgram.setUniform("alphaCutoff", 0.0f);
        shaderProgram.setUniform("glintSampler", 1);
        shaderProgram.setUniform("glintMode", false);
        shaderProgram.setUniform("glintPass", 0);
        shaderProgram.setUniform("glintPhase", 0.0f);
        float[] glintColor = EnchantedItemVisuals.glintColor();
        shaderProgram.setUniform("glintColor", new Vector3f(glintColor[0], glintColor[1], glintColor[2]));
        shaderProgram.setUniform("glintAlpha", glintColor.length >= 4 ? glintColor[3] : 0.58f);
        shaderProgram.setUniform("anaglyphColorCorrection", anaglyphColorCorrection);
        shaderProgram.setUniform("solidColorMode", false);
        shaderProgram.setUniform("solidColor", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
    }

    /**
     * Render a mesh at a specific position (during batch).
     */
    public void renderMesh(Mesh mesh, Vector3f position) {
        if (mesh == null) {
            return;
        }
        modelMatrix.identity().translate(safeVector(position, new Vector3f()));
        shaderProgram.setUniform("modelMatrix", modelMatrix);
        mesh.render();
    }

    public void renderMesh(Mesh mesh, Matrix4f modelMatrix) {
        if (mesh == null || modelMatrix == null) {
            return;
        }
        shaderProgram.setUniform("modelMatrix", modelMatrix);
        mesh.render();
    }

    /**
     * Render a mesh with identity model matrix (during batch).
     */
    public void renderMesh(Mesh mesh) {
        if (mesh == null) {
            return;
        }
        modelMatrix.identity();
        shaderProgram.setUniform("modelMatrix", modelMatrix);
        mesh.render();
    }

    public void useIdentityModelMatrix() {
        modelMatrix.identity();
        shaderProgram.setUniform("modelMatrix", modelMatrix);
    }

    public void renderPreparedMesh(Mesh mesh) {
        if (mesh == null) {
            return;
        }
        mesh.renderBound();
    }

    public void endPreparedMeshBatch() {
        Mesh.unbind();
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
        this.fogDensity = Math.max(0.0f, finiteOrDefault(density, 0.0f));
        this.fogMode = 0;
    }

    public void setFogRange(float start, float end) {
        if (!Float.isFinite(start) || !Float.isFinite(end)) {
            start = 0.0f;
            end = 128.0f;
        }
        this.fogStart = Math.max(0.0f, Math.min(start, end - 0.001f));
        this.fogEnd = Math.max(this.fogStart + 0.001f, end);
        this.fogMode = 1;
    }

    public void setFogColor(Vector3f color) {
        this.fogColor = safeColor(color, this.fogColor);
    }

    public void setAmbientLight(float ambient) {
        this.ambientLight = clamp01(ambient);
    }

    public void setClearColor(float r, float g, float b, float a) {
        glClearColor(clamp01(r), clamp01(g), clamp01(b), clamp01(a));
    }

    public void setDepthMask(boolean enabled) {
        glDepthMask(enabled);
    }

    public void setLightDirection(Vector3f direction) {
        Vector3f safe = safeVector(direction, this.lightDirection);
        if (safe.lengthSquared() <= 0.000001f) {
            safe.set(0.5f, 1.0f, 0.3f);
        }
        this.lightDirection = safe.normalize();
    }

    public void setSunBrightness(float brightness) {
        this.sunBrightness = clamp01(brightness);
    }

    public float getSunBrightness() {
        return sunBrightness;
    }

    /**
     * Set entity brightness for rendering player/mobs.
     * Value > 0 overrides vertex color lighting.
     * Set to 0 after entity rendering to return to block rendering.
     */
    public void setEntityBrightness(float brightness) {
        shaderProgram.setUniform("entityBrightness", clamp01(brightness));
    }

    public void setEntityTint(Vector3f tint) {
        shaderProgram.setUniform("entityTint", safeColor(tint, new Vector3f(1.0f, 1.0f, 1.0f)));
    }

    public void setAlphaCutoff(float cutoff) {
        shaderProgram.setUniform("alphaCutoff", clamp01(cutoff));
    }

    public void setAnaglyphColorCorrection(boolean enabled) {
        anaglyphColorCorrection = enabled;
        if (shaderProgram == null) {
            return;
        }
        shaderProgram.bind();
        shaderProgram.setUniform("anaglyphColorCorrection", enabled);
        shaderProgram.unbind();
    }

    public void cleanup() {
        if (shaderProgram != null) {
            shaderProgram.cleanup();
        }
    }

    private static Vector3f safeVector(Vector3f value, Vector3f fallback) {
        if (value == null || !Float.isFinite(value.x) || !Float.isFinite(value.y) || !Float.isFinite(value.z)) {
            return new Vector3f(fallback == null ? new Vector3f() : fallback);
        }
        return new Vector3f(value);
    }

    private static Vector3f safeColor(Vector3f value, Vector3f fallback) {
        Vector3f safe = safeVector(value, fallback == null ? new Vector3f(1.0f, 1.0f, 1.0f) : fallback);
        safe.x = clamp01(safe.x);
        safe.y = clamp01(safe.y);
        safe.z = clamp01(safe.z);
        return safe;
    }

    private static float finiteOrDefault(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
