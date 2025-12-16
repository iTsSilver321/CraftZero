package com.craftzero.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders block breaking crack overlay on the block being mined.
 * Shows 10 stages of damage (0-9) based on break progress.
 */
public class BlockBreakingRenderer {

    private ShaderProgram shader;
    private int vao;
    private int vbo;
    private Matrix4f modelMatrix;

    // 10 crack stages (0 = just started, 9 = almost broken)
    private static final int CRACK_STAGES = 10;

    public void init() throws Exception {
        // Create textured block shader with crack overlay
        shader = new ShaderProgram();
        shader.createVertexShader(
                "#version 330 core\n" +
                        "layout (location = 0) in vec3 aPos;\n" +
                        "layout (location = 1) in vec2 aTexCoord;\n" +
                        "out vec2 texCoord;\n" +
                        "uniform mat4 mvp;\n" +
                        "void main() {\n" +
                        "    gl_Position = mvp * vec4(aPos, 1.0);\n" +
                        "    texCoord = aTexCoord;\n" +
                        "}");
        shader.createFragmentShader(
                "#version 330 core\n" +
                        "in vec2 texCoord;\n" +
                        "out vec4 fragColor;\n" +
                        "uniform int crackStage;\n" +
                        "void main() {\n" +
                        "    // Generate procedural crack pattern based on stage\n" +
                        "    float intensity = float(crackStage + 1) / 10.0;\n" +
                        "    \n" +
                        "    // Create a grid-like crack pattern\n" +
                        "    vec2 uv = texCoord * 4.0;\n" +
                        "    float pattern = 0.0;\n" +
                        "    \n" +
                        "    // Horizontal and vertical cracks\n" +
                        "    float hCrack = abs(fract(uv.y * 2.0) - 0.5);\n" +
                        "    float vCrack = abs(fract(uv.x * 2.0) - 0.5);\n" +
                        "    float diagCrack = abs(fract((uv.x + uv.y) * 1.5) - 0.5);\n" +
                        "    \n" +
                        "    // Combine cracks with intensity\n" +
                        "    float crackThreshold = 0.4 - intensity * 0.35;\n" +
                        "    if (hCrack < crackThreshold || vCrack < crackThreshold || diagCrack < crackThreshold * 0.7) {\n"
                        +
                        "        pattern = 1.0;\n" +
                        "    }\n" +
                        "    \n" +
                        "    // Add some noise-like variation based on position\n" +
                        "    float noise = fract(sin(dot(texCoord, vec2(12.9898, 78.233))) * 43758.5453);\n" +
                        "    if (noise < intensity * 0.3) pattern = 1.0;\n" +
                        "    \n" +
                        "    if (pattern < 0.5) discard;\n" +
                        "    fragColor = vec4(0.0, 0.0, 0.0, 0.5 * intensity);\n" +
                        "}");
        shader.link();
        shader.createUniform("mvp");
        shader.createUniform("crackStage");

        // Create cube mesh for overlay
        createCube();

        modelMatrix = new Matrix4f();
    }

    private void createCube() {
        // Slightly larger than 1x1x1 to avoid z-fighting
        float s = 1.002f;
        float o = -0.001f;

        // 6 faces * 6 vertices = 36 vertices
        // Each vertex: x, y, z, u, v
        float[] vertices = {
                // Front face (Z+)
                o, o, s, 0, 0,
                s, o, s, 1, 0,
                s, s, s, 1, 1,
                o, o, s, 0, 0,
                s, s, s, 1, 1,
                o, s, s, 0, 1,

                // Back face (Z-)
                s, o, o, 0, 0,
                o, o, o, 1, 0,
                o, s, o, 1, 1,
                s, o, o, 0, 0,
                o, s, o, 1, 1,
                s, s, o, 0, 1,

                // Top face (Y+)
                o, s, s, 0, 0,
                s, s, s, 1, 0,
                s, s, o, 1, 1,
                o, s, s, 0, 0,
                s, s, o, 1, 1,
                o, s, o, 0, 1,

                // Bottom face (Y-)
                o, o, o, 0, 0,
                s, o, o, 1, 0,
                s, o, s, 1, 1,
                o, o, o, 0, 0,
                s, o, s, 1, 1,
                o, o, s, 0, 1,

                // Right face (X+)
                s, o, s, 0, 0,
                s, o, o, 1, 0,
                s, s, o, 1, 1,
                s, o, s, 0, 0,
                s, s, o, 1, 1,
                s, s, s, 0, 1,

                // Left face (X-)
                o, o, o, 0, 0,
                o, o, s, 1, 0,
                o, s, s, 1, 1,
                o, o, o, 0, 0,
                o, s, s, 1, 1,
                o, s, o, 0, 1
        };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);

        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        buffer.put(vertices).flip();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        // Position attribute
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);

        // Texture coordinate attribute
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(buffer);
    }

    /**
     * Render the crack overlay on the block being broken.
     * 
     * @param camera   the camera
     * @param blockPos position of the block being broken
     * @param progress break progress from 0.0 to 1.0
     */
    public void render(Camera camera, Vector3i blockPos, float progress) {
        if (blockPos == null || progress <= 0) {
            return;
        }

        // Calculate crack stage (0-9 based on progress)
        int stage = Math.min((int) (progress * CRACK_STAGES), CRACK_STAGES - 1);

        // Enable blending for transparency
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Disable backface culling to see all faces
        glDisable(GL_CULL_FACE);

        // Enable polygon offset to prevent z-fighting
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(-1.0f, -1.0f);

        shader.bind();

        // Calculate MVP
        modelMatrix.identity().translate(blockPos.x, blockPos.y, blockPos.z);
        Matrix4f mvp = new Matrix4f();
        camera.getProjectionMatrix().mul(camera.getViewMatrix(), mvp);
        mvp.mul(modelMatrix);

        shader.setUniform("mvp", mvp);
        shader.setUniform("crackStage", stage);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 36);
        glBindVertexArray(0);

        shader.unbind();

        glDisable(GL_POLYGON_OFFSET_FILL);
        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);
    }

    public void cleanup() {
        if (shader != null) {
            shader.cleanup();
        }
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
