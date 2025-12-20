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
 * Renders a wireframe around the targeted face of a block.
 */
public class BlockHighlightRenderer {

    private ShaderProgram shader;
    private int vao;
    private int vbo;
    private Matrix4f modelMatrix;
    private FloatBuffer vertexBuffer;

    // Slightly larger than 1x1x1 to avoid z-fighting
    private static final float S = 1.002f;
    private static final float O = -0.001f;

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

        // Create dynamic VBO for face edges (4 edges = 8 vertices = 24 floats)
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 24 * Float.BYTES, GL_DYNAMIC_DRAW);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        vertexBuffer = MemoryUtil.memAllocFloat(24);
        modelMatrix = new Matrix4f();
    }

    /**
     * Get the 4 edge vertices for a specific face.
     * Each face has 4 edges, each edge has 2 vertices (8 vertices total, 24
     * floats).
     */
    private void getFaceVertices(int face, float[] vertices) {
        switch (face) {
            case 0: // Top (+Y)
                // Edge 1
                vertices[0] = O;
                vertices[1] = S;
                vertices[2] = O;
                vertices[3] = S;
                vertices[4] = S;
                vertices[5] = O;
                // Edge 2
                vertices[6] = S;
                vertices[7] = S;
                vertices[8] = O;
                vertices[9] = S;
                vertices[10] = S;
                vertices[11] = S;
                // Edge 3
                vertices[12] = S;
                vertices[13] = S;
                vertices[14] = S;
                vertices[15] = O;
                vertices[16] = S;
                vertices[17] = S;
                // Edge 4
                vertices[18] = O;
                vertices[19] = S;
                vertices[20] = S;
                vertices[21] = O;
                vertices[22] = S;
                vertices[23] = O;
                break;
            case 1: // Bottom (-Y)
                vertices[0] = O;
                vertices[1] = O;
                vertices[2] = O;
                vertices[3] = S;
                vertices[4] = O;
                vertices[5] = O;
                vertices[6] = S;
                vertices[7] = O;
                vertices[8] = O;
                vertices[9] = S;
                vertices[10] = O;
                vertices[11] = S;
                vertices[12] = S;
                vertices[13] = O;
                vertices[14] = S;
                vertices[15] = O;
                vertices[16] = O;
                vertices[17] = S;
                vertices[18] = O;
                vertices[19] = O;
                vertices[20] = S;
                vertices[21] = O;
                vertices[22] = O;
                vertices[23] = O;
                break;
            case 2: // North (-Z)
                vertices[0] = O;
                vertices[1] = O;
                vertices[2] = O;
                vertices[3] = S;
                vertices[4] = O;
                vertices[5] = O;
                vertices[6] = S;
                vertices[7] = O;
                vertices[8] = O;
                vertices[9] = S;
                vertices[10] = S;
                vertices[11] = O;
                vertices[12] = S;
                vertices[13] = S;
                vertices[14] = O;
                vertices[15] = O;
                vertices[16] = S;
                vertices[17] = O;
                vertices[18] = O;
                vertices[19] = S;
                vertices[20] = O;
                vertices[21] = O;
                vertices[22] = O;
                vertices[23] = O;
                break;
            case 3: // South (+Z)
                vertices[0] = O;
                vertices[1] = O;
                vertices[2] = S;
                vertices[3] = S;
                vertices[4] = O;
                vertices[5] = S;
                vertices[6] = S;
                vertices[7] = O;
                vertices[8] = S;
                vertices[9] = S;
                vertices[10] = S;
                vertices[11] = S;
                vertices[12] = S;
                vertices[13] = S;
                vertices[14] = S;
                vertices[15] = O;
                vertices[16] = S;
                vertices[17] = S;
                vertices[18] = O;
                vertices[19] = S;
                vertices[20] = S;
                vertices[21] = O;
                vertices[22] = O;
                vertices[23] = S;
                break;
            case 4: // East (+X)
                vertices[0] = S;
                vertices[1] = O;
                vertices[2] = O;
                vertices[3] = S;
                vertices[4] = O;
                vertices[5] = S;
                vertices[6] = S;
                vertices[7] = O;
                vertices[8] = S;
                vertices[9] = S;
                vertices[10] = S;
                vertices[11] = S;
                vertices[12] = S;
                vertices[13] = S;
                vertices[14] = S;
                vertices[15] = S;
                vertices[16] = S;
                vertices[17] = O;
                vertices[18] = S;
                vertices[19] = S;
                vertices[20] = O;
                vertices[21] = S;
                vertices[22] = O;
                vertices[23] = O;
                break;
            case 5: // West (-X)
                vertices[0] = O;
                vertices[1] = O;
                vertices[2] = O;
                vertices[3] = O;
                vertices[4] = O;
                vertices[5] = S;
                vertices[6] = O;
                vertices[7] = O;
                vertices[8] = S;
                vertices[9] = O;
                vertices[10] = S;
                vertices[11] = S;
                vertices[12] = O;
                vertices[13] = S;
                vertices[14] = S;
                vertices[15] = O;
                vertices[16] = S;
                vertices[17] = O;
                vertices[18] = O;
                vertices[19] = S;
                vertices[20] = O;
                vertices[21] = O;
                vertices[22] = O;
                vertices[23] = O;
                break;
        }
    }

    public void render(Camera camera, Raycast.RaycastResult target) {
        if (target == null || !target.hit || target.face < 0) {
            return;
        }

        Vector3i pos = target.blockPos;

        // Get vertices for the targeted face
        float[] vertices = new float[24];
        getFaceVertices(target.face, vertices);

        // Update VBO with face vertices
        vertexBuffer.clear();
        vertexBuffer.put(vertices).flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        // Enable blending for semi-transparent lines
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Thicker lines
        glLineWidth(2.0f);

        shader.bind();

        // Calculate MVP
        modelMatrix.identity().translate(pos.x, pos.y, pos.z);
        Matrix4f mvp = new Matrix4f();
        camera.getProjectionMatrix().mul(camera.getViewMatrix(), mvp);
        mvp.mul(modelMatrix);

        shader.setUniform("mvp", mvp);

        glBindVertexArray(vao);
        glDrawArrays(GL_LINES, 0, 8); // 4 edges = 8 vertices
        glBindVertexArray(0);

        shader.unbind();

        glLineWidth(1.0f);
        glDisable(GL_BLEND);
    }

    public void cleanup() {
        if (shader != null) {
            shader.cleanup();
        }
        if (vertexBuffer != null) {
            MemoryUtil.memFree(vertexBuffer);
        }
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
