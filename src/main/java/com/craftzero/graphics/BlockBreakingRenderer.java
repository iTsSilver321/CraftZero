package com.craftzero.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders block breaking crack overlay on the block being mined.
 * Uses actual crack textures from the terrain atlas (bottom row).
 */
public class BlockBreakingRenderer {

    private ShaderProgram shader;
    private int vao;
    private int vbo;
    private int ebo;
    private Matrix4f modelMatrix;
    private Texture terrainTexture;

    // 10 crack stages (0 = just started, 9 = almost broken)
    private static final int CRACK_STAGES = 10;

    // Atlas configuration: 16x16 grid
    private static final float TILE_SIZE = 1.0f / 16.0f;
    // Crack textures are in bottom row (row 15, columns 0-9)
    private static final int CRACK_ROW = 15;

    public void init() throws Exception {
        // Create textured shader for crack overlay
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
                        "uniform sampler2D crackTexture;\n" +
                        "void main() {\n" +
                        "    vec4 color = texture(crackTexture, texCoord);\n" +
                        "    // Only show the most opaque pixels (makes cracks thinner)\n" +
                        "    if (color.a < 0.9) discard;\n" +
                        "    // Use solid black for visible crack pixels\n" +
                        "    fragColor = vec4(0.0, 0.0, 0.0, 0.5);\n" +
                        "}");
        shader.link();
        shader.createUniform("mvp");
        shader.createUniform("crackTexture");

        // Load terrain texture for crack overlays
        terrainTexture = new Texture("/textures/terrain/Terrain.png");

        // Create cube mesh for overlay
        createCube();

        modelMatrix = new Matrix4f();
    }

    private void createCube() {
        // Slightly larger than 1x1x1 to avoid z-fighting
        float s = 1.002f;
        float o = -0.001f;

        // 6 faces * 4 vertices = 24 vertices
        // Each vertex: x, y, z, u, v (UVs will be updated dynamically)
        float[] vertices = {
                // Front face (Z+) - vertices 0-3
                o, o, s, 0, 1,
                s, o, s, 1, 1,
                s, s, s, 1, 0,
                o, s, s, 0, 0,

                // Back face (Z-) - vertices 4-7
                s, o, o, 0, 1,
                o, o, o, 1, 1,
                o, s, o, 1, 0,
                s, s, o, 0, 0,

                // Top face (Y+) - vertices 8-11
                o, s, s, 0, 1,
                s, s, s, 1, 1,
                s, s, o, 1, 0,
                o, s, o, 0, 0,

                // Bottom face (Y-) - vertices 12-15
                o, o, o, 0, 1,
                s, o, o, 1, 1,
                s, o, s, 1, 0,
                o, o, s, 0, 0,

                // Right face (X+) - vertices 16-19
                s, o, s, 0, 1,
                s, o, o, 1, 1,
                s, s, o, 1, 0,
                s, s, s, 0, 0,

                // Left face (X-) - vertices 20-23
                o, o, o, 0, 1,
                o, o, s, 1, 1,
                o, s, s, 1, 0,
                o, s, o, 0, 0
        };

        // Indices for 6 faces (2 triangles each)
        int[] indices = {
                0, 1, 2, 2, 3, 0, // Front
                4, 5, 6, 6, 7, 4, // Back
                8, 9, 10, 10, 11, 8, // Top
                12, 13, 14, 14, 15, 12, // Bottom
                16, 17, 18, 18, 19, 16, // Right
                20, 21, 22, 22, 23, 20 // Left
        };

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW);

        IntBuffer indexBuffer = MemoryUtil.memAllocInt(indices.length);
        indexBuffer.put(indices).flip();

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        // Position attribute
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);

        // Texture coordinate attribute
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        MemoryUtil.memFree(vertexBuffer);
        MemoryUtil.memFree(indexBuffer);
    }

    /**
     * Update UVs for the crack stage in the terrain atlas.
     */
    private void updateUVs(int stage) {
        // Calculate UV coordinates for the crack texture (row 15, col = stage)
        float u1 = stage * TILE_SIZE;
        float v1 = CRACK_ROW * TILE_SIZE;
        float u2 = u1 + TILE_SIZE;
        float v2 = v1 + TILE_SIZE;

        // Update UVs for all 24 vertices (6 faces * 4 vertices)
        float[] uvs = new float[24 * 5]; // 24 vertices * 5 floats each

        // We need to update the full vertex data because UVs are interleaved
        float s = 1.002f;
        float o = -0.001f;

        int idx = 0;
        // Front face (Z+)
        uvs[idx++] = o;
        uvs[idx++] = o;
        uvs[idx++] = s;
        uvs[idx++] = u1;
        uvs[idx++] = v2;
        uvs[idx++] = s;
        uvs[idx++] = o;
        uvs[idx++] = s;
        uvs[idx++] = u2;
        uvs[idx++] = v2;
        uvs[idx++] = s;
        uvs[idx++] = s;
        uvs[idx++] = s;
        uvs[idx++] = u2;
        uvs[idx++] = v1;
        uvs[idx++] = o;
        uvs[idx++] = s;
        uvs[idx++] = s;
        uvs[idx++] = u1;
        uvs[idx++] = v1;

        // Back face (Z-)
        uvs[idx++] = s;
        uvs[idx++] = o;
        uvs[idx++] = o;
        uvs[idx++] = u1;
        uvs[idx++] = v2;
        uvs[idx++] = o;
        uvs[idx++] = o;
        uvs[idx++] = o;
        uvs[idx++] = u2;
        uvs[idx++] = v2;
        uvs[idx++] = o;
        uvs[idx++] = s;
        uvs[idx++] = o;
        uvs[idx++] = u2;
        uvs[idx++] = v1;
        uvs[idx++] = s;
        uvs[idx++] = s;
        uvs[idx++] = o;
        uvs[idx++] = u1;
        uvs[idx++] = v1;

        // Top face (Y+)
        uvs[idx++] = o;
        uvs[idx++] = s;
        uvs[idx++] = s;
        uvs[idx++] = u1;
        uvs[idx++] = v2;
        uvs[idx++] = s;
        uvs[idx++] = s;
        uvs[idx++] = s;
        uvs[idx++] = u2;
        uvs[idx++] = v2;
        uvs[idx++] = s;
        uvs[idx++] = s;
        uvs[idx++] = o;
        uvs[idx++] = u2;
        uvs[idx++] = v1;
        uvs[idx++] = o;
        uvs[idx++] = s;
        uvs[idx++] = o;
        uvs[idx++] = u1;
        uvs[idx++] = v1;

        // Bottom face (Y-)
        uvs[idx++] = o;
        uvs[idx++] = o;
        uvs[idx++] = o;
        uvs[idx++] = u1;
        uvs[idx++] = v2;
        uvs[idx++] = s;
        uvs[idx++] = o;
        uvs[idx++] = o;
        uvs[idx++] = u2;
        uvs[idx++] = v2;
        uvs[idx++] = s;
        uvs[idx++] = o;
        uvs[idx++] = s;
        uvs[idx++] = u2;
        uvs[idx++] = v1;
        uvs[idx++] = o;
        uvs[idx++] = o;
        uvs[idx++] = s;
        uvs[idx++] = u1;
        uvs[idx++] = v1;

        // Right face (X+)
        uvs[idx++] = s;
        uvs[idx++] = o;
        uvs[idx++] = s;
        uvs[idx++] = u1;
        uvs[idx++] = v2;
        uvs[idx++] = s;
        uvs[idx++] = o;
        uvs[idx++] = o;
        uvs[idx++] = u2;
        uvs[idx++] = v2;
        uvs[idx++] = s;
        uvs[idx++] = s;
        uvs[idx++] = o;
        uvs[idx++] = u2;
        uvs[idx++] = v1;
        uvs[idx++] = s;
        uvs[idx++] = s;
        uvs[idx++] = s;
        uvs[idx++] = u1;
        uvs[idx++] = v1;

        // Left face (X-)
        uvs[idx++] = o;
        uvs[idx++] = o;
        uvs[idx++] = o;
        uvs[idx++] = u1;
        uvs[idx++] = v2;
        uvs[idx++] = o;
        uvs[idx++] = o;
        uvs[idx++] = s;
        uvs[idx++] = u2;
        uvs[idx++] = v2;
        uvs[idx++] = o;
        uvs[idx++] = s;
        uvs[idx++] = s;
        uvs[idx++] = u2;
        uvs[idx++] = v1;
        uvs[idx++] = o;
        uvs[idx++] = s;
        uvs[idx++] = o;
        uvs[idx++] = u1;
        uvs[idx++] = v1;

        // Update VBO with new UVs
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer buffer = MemoryUtil.memAllocFloat(uvs.length);
        buffer.put(uvs).flip();
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
        MemoryUtil.memFree(buffer);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    /**
     * Render the crack overlay on the block being broken.
     * Only renders faces that are exposed (not covered by adjacent solid blocks).
     * 
     * @param camera   the camera
     * @param blockPos position of the block being broken
     * @param progress break progress from 0.0 to 1.0
     * @param world    the world to check for adjacent blocks
     */
    public void render(Camera camera, Vector3i blockPos, float progress, com.craftzero.world.World world) {
        if (blockPos == null || progress <= 0) {
            return;
        }

        // Calculate crack stage (0-9 based on progress)
        int stage = Math.min((int) (progress * CRACK_STAGES), CRACK_STAGES - 1);

        // Update UVs for current crack stage
        updateUVs(stage);

        // Enable blending for transparency
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Disable depth writing - overlay shouldn't affect depth buffer
        glDepthMask(false);

        // Keep backface culling enabled - only show cracks on visible faces
        // glDisable(GL_CULL_FACE);

        // Enable polygon offset to prevent z-fighting
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(-1.0f, -1.0f);

        shader.bind();

        // Bind terrain texture with nearest-neighbor filtering for sharp cracks
        terrainTexture.bind(0);
        // Use nearest filtering to keep thin crack lines sharp (no interpolation blur)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        shader.setUniform("crackTexture", 0);

        // Calculate MVP
        modelMatrix.identity().translate(blockPos.x, blockPos.y, blockPos.z);
        Matrix4f mvp = new Matrix4f();
        camera.getProjectionMatrix().mul(camera.getViewMatrix(), mvp);
        mvp.mul(modelMatrix);

        shader.setUniform("mvp", mvp);

        glBindVertexArray(vao);

        // Draw only exposed faces (6 indices per face)
        // Face order in EBO: Front, Back, Top, Bottom, Right, Left
        int bx = blockPos.x, by = blockPos.y, bz = blockPos.z;

        // Front face (Z+) - check if z+1 is air/transparent
        if (world == null || !world.getBlock(bx, by, bz + 1).isSolid()) {
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        }
        // Back face (Z-) - check if z-1 is air/transparent
        if (world == null || !world.getBlock(bx, by, bz - 1).isSolid()) {
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 6 * Integer.BYTES);
        }
        // Top face (Y+) - check if y+1 is air/transparent
        if (world == null || !world.getBlock(bx, by + 1, bz).isSolid()) {
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 12 * Integer.BYTES);
        }
        // Bottom face (Y-) - check if y-1 is air/transparent
        if (world == null || !world.getBlock(bx, by - 1, bz).isSolid()) {
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 18 * Integer.BYTES);
        }
        // Right face (X+) - check if x+1 is air/transparent
        if (world == null || !world.getBlock(bx + 1, by, bz).isSolid()) {
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 24 * Integer.BYTES);
        }
        // Left face (X-) - check if x-1 is air/transparent
        if (world == null || !world.getBlock(bx - 1, by, bz).isSolid()) {
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 30 * Integer.BYTES);
        }

        glBindVertexArray(0);

        terrainTexture.unbind();
        shader.unbind();

        glDisable(GL_POLYGON_OFFSET_FILL);
        // glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);
        glDepthMask(true); // Restore depth writing
    }

    public void cleanup() {
        if (shader != null) {
            shader.cleanup();
        }
        if (terrainTexture != null) {
            terrainTexture.cleanup();
        }
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }
}
