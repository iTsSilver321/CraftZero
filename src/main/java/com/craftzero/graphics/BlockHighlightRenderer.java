package com.craftzero.graphics;

import com.craftzero.physics.Raycast;

import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders a wireframe box around the targeted block.
 */
public class BlockHighlightRenderer {

    private ShaderProgram shader;
    private int vao;
    private int vbo;
    private Matrix4f modelMatrix;

    public void init() throws Exception {
        // Create simple line shader
        shader = new ShaderProgram();
        shader.createVertexShader(
                "#version 330 core\n" +
                        "layout (location = 0) in vec3 aPos;\n" +
                        "uniform mat4 mvp;\n" +
                        "void main() {\n" +
                        "    gl_Position = mvp * vec4(aPos, 1.0);\n" +
                        "}");
        shader.createFragmentShader(
                "#version 330 core\n" +
                        "out vec4 fragColor;\n" +
                        "void main() {\n" +
                        "    fragColor = vec4(0.0, 0.0, 0.0, 0.6);\n" +
                        "}");
        shader.link();
        shader.createUniform("mvp");

        // Create wireframe cube
        createWireframeCube();

        modelMatrix = new Matrix4f();
    }

    private void createWireframeCube() {
        // Slightly larger than 1x1x1 to avoid z-fighting
        float s = 1.002f;
        float o = -0.001f; // Offset

        // 12 edges = 24 vertices
        float[] vertices = {
                // Bottom face edges
                o, o, o, s, o, o,
                s, o, o, s, o, s,
                s, o, s, o, o, s,
                o, o, s, o, o, o,

                // Top face edges
                o, s, o, s, s, o,
                s, s, o, s, s, s,
                s, s, s, o, s, s,
                o, s, s, o, s, o,

                // Vertical edges
                o, o, o, o, s, o,
                s, o, o, s, s, o,
                s, o, s, s, s, s,
                o, o, s, o, s, s
        };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);

        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        buffer.put(vertices).flip();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(buffer);
    }

    public void render(Camera camera, Raycast.RaycastResult target) {
        if (target == null || !target.hit) {
            return;
        }

        Vector3i pos = target.blockPos;

        // Enable blending for semi-transparent lines
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Thicker lines
        glLineWidth(2.0f);

        // Disable depth test for always-visible lines (optional)
        // Or keep it enabled for proper occlusion

        shader.bind();

        // Calculate MVP
        modelMatrix.identity().translate(pos.x, pos.y, pos.z);
        Matrix4f mvp = new Matrix4f();
        camera.getProjectionMatrix().mul(camera.getViewMatrix(), mvp);
        mvp.mul(modelMatrix);

        shader.setUniform("mvp", mvp);

        glBindVertexArray(vao);
        glDrawArrays(GL_LINES, 0, 24);
        glBindVertexArray(0);

        shader.unbind();

        glLineWidth(1.0f);
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
