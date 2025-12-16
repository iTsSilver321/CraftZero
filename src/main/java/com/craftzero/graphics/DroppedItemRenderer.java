package com.craftzero.graphics;

import com.craftzero.entity.DroppedItem;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import org.joml.Matrix4f;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders dropped items as 3D spinning/bobbing blocks.
 * Creates a single cube mesh and reuses it for all items with different
 * transforms.
 */
public class DroppedItemRenderer {

    private ShaderProgram shader;
    private int vao, vbo, ebo;
    private int vertexCount;
    private Matrix4f modelMatrix;

    public void init() throws Exception {
        modelMatrix = new Matrix4f();

        // Use the same shader as the world renderer
        shader = new ShaderProgram();
        shader.createVertexShader(ShaderProgram.loadResource("/shaders/scene.vert"));
        shader.createFragmentShader(ShaderProgram.loadResource("/shaders/scene.frag"));
        shader.link();

        shader.createUniform("projectionMatrix");
        shader.createUniform("viewMatrix");
        shader.createUniform("modelMatrix");
        shader.createUniform("textureSampler");
        shader.createUniform("fogEnabled");
        shader.createUniform("fogDensity");
        shader.createUniform("fogColor");
        shader.createUniform("ambientLight");
        shader.createUniform("lightDirection");
        shader.createUniform("lightColor");

        // Create the cube mesh for rendering items
        createCubeMesh();
    }

    /**
     * Create a unit cube mesh (1x1x1) centered at origin.
     * This will be scaled and transformed for each dropped item.
     */
    private void createCubeMesh() {
        // We'll create mesh data dynamically per block type, but for now
        // just set up the VAO structure. The actual rendering will use
        // the Block class to get proper UV coordinates.

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        // Reserve buffer space - will be filled per item type
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, 1024 * Float.BYTES, GL_DYNAMIC_DRAW);

        // Position attribute (location 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Texture coord attribute (location 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 8 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Normal attribute (location 2)
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 8 * Float.BYTES, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, 256 * Integer.BYTES, GL_DYNAMIC_DRAW);

        glBindVertexArray(0);
    }

    /**
     * Build mesh data for a specific block type.
     * Returns vertex data interleaved: pos(3) + uv(2) + normal(3) = 8 floats per
     * vertex
     */
    private float[] buildCubeVertices(BlockType type) {
        float[] vertices = new float[6 * 4 * 8]; // 6 faces * 4 verts * 8 floats
        int idx = 0;

        // Half size for centering at origin
        float h = 0.5f;

        for (int face = 0; face < 6; face++) {
            float[] faceVerts = Block.getFaceVertices(face, 0, 0, 0);
            float[] faceUVs = Block.getFaceTexCoords(type, face);
            float[] faceNormals = Block.getFaceNormals(face);

            for (int v = 0; v < 4; v++) {
                // Position (centered at origin)
                vertices[idx++] = faceVerts[v * 3] - h;
                vertices[idx++] = faceVerts[v * 3 + 1] - h;
                vertices[idx++] = faceVerts[v * 3 + 2] - h;

                // UV
                vertices[idx++] = faceUVs[v * 2];
                vertices[idx++] = faceUVs[v * 2 + 1];

                // Normal
                vertices[idx++] = faceNormals[v * 3];
                vertices[idx++] = faceNormals[v * 3 + 1];
                vertices[idx++] = faceNormals[v * 3 + 2];
            }
        }

        return vertices;
    }

    private int[] buildCubeIndices() {
        int[] indices = new int[6 * 6]; // 6 faces * 6 indices (2 triangles)
        int idx = 0;

        for (int face = 0; face < 6; face++) {
            int base = face * 4;
            indices[idx++] = base;
            indices[idx++] = base + 1;
            indices[idx++] = base + 2;
            indices[idx++] = base + 2;
            indices[idx++] = base + 3;
            indices[idx++] = base;
        }

        return indices;
    }

    /**
     * Render all dropped items in the world.
     */
    public void render(Camera camera, List<DroppedItem> items, Texture atlas) {
        if (items.isEmpty()) {
            return;
        }

        glDisable(GL_CULL_FACE); // Show all faces of small items

        atlas.bind(0);
        shader.bind();

        // Set camera matrices
        shader.setUniform("projectionMatrix", camera.getProjectionMatrix());
        shader.setUniform("viewMatrix", camera.getViewMatrix());
        shader.setUniform("textureSampler", 0);

        // Fog settings (match world renderer)
        shader.setUniform("fogEnabled", true);
        shader.setUniform("fogDensity", 0.007f);
        shader.setUniform("fogColor", new org.joml.Vector3f(0.6f, 0.6f, 0.6f));

        // Lighting
        shader.setUniform("ambientLight", 0.4f); // Slightly brighter for items
        shader.setUniform("lightDirection", new org.joml.Vector3f(0.5f, 1.0f, 0.3f).normalize());
        shader.setUniform("lightColor", new org.joml.Vector3f(1.0f, 1.0f, 0.9f));

        glBindVertexArray(vao);

        // Render each item
        for (DroppedItem item : items) {
            // Rebuild mesh for each item (different types need different meshes)
            BlockType type = item.getBlockType();
            float[] vertices;
            int[] indices;

            if (type.isItem()) {
                // Items render as flat 2D sprites (single plane)
                vertices = buildItemSpriteVertices(type);
                // Single quad indices
                indices = new int[] { 0, 1, 2, 2, 3, 0 };
            } else {
                // Blocks render as 3D cubes
                vertices = buildCubeVertices(type);
                indices = buildCubeIndices();
            }

            // Upload vertices
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);

            // Upload indices
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, indices);
            vertexCount = indices.length;

            // Determine how many overlapping blocks to show based on count
            // 1 = 1 block, 2-9 = 2 blocks, 10-31 = 3 blocks, 32+ = 4 blocks
            int count = item.getCount();
            int blocksToDraw;
            if (count >= 32) {
                blocksToDraw = 4;
            } else if (count >= 10) {
                blocksToDraw = 3;
            } else if (count >= 2) {
                blocksToDraw = 2;
            } else {
                blocksToDraw = 1;
            }

            float baseX = item.getX();
            float baseY = item.getVisualY();
            float baseZ = item.getZ();
            float rotation = item.getRotation();
            float scale = item.getScale();

            // Draw overlapping blocks with slight offsets
            for (int b = 0; b < blocksToDraw; b++) {
                // Small random-looking offset for each extra block
                float offsetX = (b % 2 == 0 ? 1 : -1) * b * 0.03f;
                float offsetY = b * 0.02f;
                float offsetZ = ((b + 1) % 2 == 0 ? 1 : -1) * b * 0.03f;
                float rotOffset = b * 15.0f; // Slight rotation offset

                modelMatrix.identity()
                        .translate(baseX + offsetX, baseY + offsetY, baseZ + offsetZ)
                        .rotateY((float) Math.toRadians(rotation + rotOffset))
                        .scale(scale);

                shader.setUniform("modelMatrix", modelMatrix);
                glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
            }
        }

        glBindVertexArray(0);
        shader.unbind();
        atlas.unbind();
        glEnable(GL_CULL_FACE);
    }

    /**
     * Build a flat 2D sprite mesh for items (single plane).
     */
    private float[] buildItemSpriteVertices(BlockType type) {
        float[] vertices = new float[4 * 8]; // 1 quad * 4 verts * 8 floats
        float h = 0.5f;

        // Get texture coordinates (use side texture for items)
        float[] uv = type.getTextureCoords(2);
        float u0 = uv[0], v0 = uv[1], u1 = uv[2], v1 = uv[3];

        int idx = 0;

        // Single plane facing camera (will rotate with item rotation)
        // Vertex format: x, y, z, u, v, nx, ny, nz
        // Top-left
        vertices[idx++] = -h;
        vertices[idx++] = h;
        vertices[idx++] = 0;
        vertices[idx++] = u0;
        vertices[idx++] = v0;
        vertices[idx++] = 0;
        vertices[idx++] = 0;
        vertices[idx++] = 1;
        // Bottom-left
        vertices[idx++] = -h;
        vertices[idx++] = -h;
        vertices[idx++] = 0;
        vertices[idx++] = u0;
        vertices[idx++] = v1;
        vertices[idx++] = 0;
        vertices[idx++] = 0;
        vertices[idx++] = 1;
        // Bottom-right
        vertices[idx++] = h;
        vertices[idx++] = -h;
        vertices[idx++] = 0;
        vertices[idx++] = u1;
        vertices[idx++] = v1;
        vertices[idx++] = 0;
        vertices[idx++] = 0;
        vertices[idx++] = 1;
        // Top-right
        vertices[idx++] = h;
        vertices[idx++] = h;
        vertices[idx++] = 0;
        vertices[idx++] = u1;
        vertices[idx++] = v0;
        vertices[idx++] = 0;
        vertices[idx++] = 0;
        vertices[idx++] = 1;

        return vertices;
    }

    public void cleanup() {
        if (shader != null) {
            shader.cleanup();
        }
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }
}
