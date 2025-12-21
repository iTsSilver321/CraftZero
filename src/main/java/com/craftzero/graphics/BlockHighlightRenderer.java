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
        shader.createVertexShader("#version 330 core\n" + "layout (location = 0) in vec3 aPos;\n"
                + "uniform mat4 mvp;\n" + "void main() {\n" + "    gl_Position = mvp * vec4(aPos, 1.0);\n" + "}");
        shader.createFragmentShader("#version 330 core\n" + "out vec4 fragColor;\n" + "void main() {\n"
                + "    fragColor = vec4(0.0, 0.0, 0.0, 0.6);\n" + "}");
        shader.link();
        shader.createUniform("mvp");

        // Create VBO for all 6 faces (6 faces * 4 edges * 2 vertices = 48 vertices =
        // 144 floats)
        vao =

                glGenVertexArrays();
        vbo =

                glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 144 * Float.BYTES, GL_DYNAMIC_DRAW);

        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        vertexBuffer = MemoryUtil.memAllocFloat(144);
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

    public void render(Camera camera, Raycast.RaycastResult target, com.craftzero.world.World world) {
        if (target == null || !target.hit || target.face < 0) {
            return;
        }

        Vector3i pos = target.blockPos;

        // Calculate which faces are visible to the player
        // A face is visible if:
        // 1. The camera is on the same side as the face normal (facing camera)
        // 2. The neighbor block on that face does not occlude it (exposed)
        float camX = camera.getPosition().x;
        float camY = camera.getPosition().y;
        float camZ = camera.getPosition().z;

        boolean[] visibleFaces = new boolean[6];

        // Check visibility against block face planes (Back-Face Culling for Wireframe)
        // A face is visible if the camera is strictly "in front" of the face plane.

        // Face 0: Top (+Y) -> Plane at Y+1. Camera must be > Y+1.
        visibleFaces[0] = (camY > pos.y + 1.0f) && !world.getBlock(pos.x, pos.y + 1, pos.z).occludesFace();

        // Face 1: Bottom (-Y) -> Plane at Y. Camera must be < Y.
        visibleFaces[1] = (camY < pos.y) && !world.getBlock(pos.x, pos.y - 1, pos.z).occludesFace();

        // Face 2: North (-Z) -> Plane at Z. Camera must be < Z.
        visibleFaces[2] = (camZ < pos.z) && !world.getBlock(pos.x, pos.y, pos.z - 1).occludesFace();

        // Face 3: South (+Z) -> Plane at Z+1. Camera must be > Z+1.
        visibleFaces[3] = (camZ > pos.z + 1.0f) && !world.getBlock(pos.x, pos.y, pos.z + 1).occludesFace();

        // Face 4: East (+X) -> Plane at X+1. Camera must be > X+1.
        visibleFaces[4] = (camX > pos.x + 1.0f) && !world.getBlock(pos.x + 1, pos.y, pos.z).occludesFace();

        // Face 5: West (-X) -> Plane at X. Camera must be < X.
        visibleFaces[5] = (camX < pos.x) && !world.getBlock(pos.x - 1, pos.y, pos.z).occludesFace();

        // Build vertices for visible faces only
        float[] faceVerts = new float[24];
        int vertexCount = 0;
        vertexBuffer.clear();
        for (int face = 0; face < 6; face++) {
            if (visibleFaces[face]) {
                getFaceVertices(face, faceVerts);
                vertexBuffer.put(faceVerts);
                vertexCount += 8; // 8 vertices per face
            }
        }
        vertexBuffer.flip();

        if (vertexCount == 0)
            return; // No visible faces

        // Update VBO with visible face vertices
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        // Enable blending for semi-transparent lines
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Enable depth testing so highlight is hidden behind blocks
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

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
        glDrawArrays(GL_LINES, 0, vertexCount); // Use computed vertex count
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
