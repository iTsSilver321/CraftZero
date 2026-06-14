package com.craftzero.graphics;

import com.craftzero.entity.DroppedItem;
import com.craftzero.inventory.ItemRenderProfile;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import org.joml.Matrix4f;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
    private static final float ITEM_RENDER_DISTANCE = 128.0f;

    private ShaderProgram shader;
    private Matrix4f modelMatrix;
    private final Map<ItemType, CachedMesh> meshCache = new EnumMap<>(ItemType.class);

    private static class CachedMesh {
        final int vao;
        final int vbo;
        final int ebo;
        final int vertexCount;

        CachedMesh(int vao, int vbo, int ebo, int vertexCount) {
            this.vao = vao;
            this.vbo = vbo;
            this.ebo = ebo;
            this.vertexCount = vertexCount;
        }
    }

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
        shader.createUniform("sunBrightness");
        shader.createUniform("alphaCutoff");

    }

    /**
     * Build mesh data for a specific block type.
     * Returns vertex data interleaved: pos(3) + uv(2) + normal(3) + color(3) = 11
     * floats per vertex
     */
    private float[] buildCubeVertices(BlockType type) {
        float[] vertices = new float[6 * 4 * 11]; // 6 faces * 4 verts * 11 floats
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

                // Color (white - no tinting for dropped items)
                vertices[idx++] = 1.0f;
                vertices[idx++] = 1.0f;
                vertices[idx++] = 1.0f;
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
    public void render(Camera camera, List<DroppedItem> items, Texture atlas, Texture itemsTexture,
            com.craftzero.world.DayCycleManager dayCycle, com.craftzero.world.World world) {
        if (items.isEmpty()) {
            return;
        }

        glDisable(GL_CULL_FACE); // Show all faces of small items

        atlas.bind(0);
        Texture currentTexture = atlas;

        shader.bind();

        // Set camera matrices
        shader.setUniform("projectionMatrix", camera.getProjectionMatrix());
        shader.setUniform("viewMatrix", camera.getViewMatrix());
        shader.setUniform("textureSampler", 0);

        // Fog settings from day cycle
        shader.setUniform("fogEnabled", true);
        shader.setUniform("fogDensity", 0.007f);
        shader.setUniform("fogColor", dayCycle.getFogColor());

        // Base lighting from day cycle
        shader.setUniform("ambientLight", dayCycle.getAmbientIntensity());
        shader.setUniform("lightDirection", dayCycle.getSunDirection());
        shader.setUniform("lightColor", dayCycle.getLightColor());

        // Render each item
        for (DroppedItem item : items) {
            if (isTooFar(camera, item)) {
                continue;
            }
            // Query sky light at item position
            int worldX = (int) Math.floor(item.getX());
            int worldY = (int) Math.floor(item.getY());
            int worldZ = (int) Math.floor(item.getZ());
            int skyLight = world.getSkyLight(worldX, worldY, worldZ);

            // Apply gamma curve and combine with day/night brightness
            float lightFactor = skyLight / 15.0f;
            float gammaLight = lightFactor / (3.0f - 2.0f * lightFactor);
            float finalBrightness = Math.max(0.08f, gammaLight) * dayCycle.getSunBrightness();
            shader.setUniform("sunBrightness", finalBrightness);

            ItemType type = item.getItemType();

            // Switch texture if needed
            Texture requiredTexture = (ItemTextureResolver.usesItemsAtlas(type) && itemsTexture != null)
                    ? itemsTexture
                    : atlas;
            if (requiredTexture != currentTexture) {
                requiredTexture.bind(0);
                currentTexture = requiredTexture;
            }

            ItemRenderProfile profile = type.getRenderProfile();
            CachedMesh mesh = meshCache.computeIfAbsent(type, this::createCachedMesh);
            if (profile.modelKind() == ItemRenderProfile.ModelKind.BLOCK) {
                shader.setUniform("alphaCutoff",
                        type.getPlacedBlock().getRenderLayer() == com.craftzero.world.BlockRenderLayer.CUTOUT
                                ? 0.1f
                                : 0.0f);
            } else {
                shader.setUniform("alphaCutoff", 0.1f);
            }

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
            float scale = item.getScale() * (profile.modelKind() == ItemRenderProfile.ModelKind.SPRITE
                    ? Math.max(0.8f, profile.thirdPersonScale() / 0.375f)
                    : 1.0f);

            glBindVertexArray(mesh.vao);

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
                glDrawElements(GL_TRIANGLES, mesh.vertexCount, GL_UNSIGNED_INT, 0);
            }
        }

        glBindVertexArray(0);
        shader.unbind();
        if (currentTexture != null)
            currentTexture.unbind();
        glEnable(GL_CULL_FACE);
    }

    private static boolean isTooFar(Camera camera, DroppedItem item) {
        float dx = item.getX() - camera.getPosition().x;
        float dy = item.getY() - camera.getPosition().y;
        float dz = item.getZ() - camera.getPosition().z;
        float max = Math.min(camera.getFarPlane(), ITEM_RENDER_DISTANCE);
        return dx * dx + dy * dy + dz * dz > max * max;
    }

    /**
     * Build a flat 2D sprite mesh for items (single plane).
     * Vertex format: pos(3) + uv(2) + normal(3) + color(3) = 11 floats per vertex
     */
    private float[] buildItemSpriteVertices(ItemType type) {
        float[] vertices = new float[4 * 11]; // 1 quad * 4 verts * 11 floats
        float h = 0.5f;

        // Get texture coordinates
        float[] uv;
        uv = ItemTextureResolver.getUv(type);

        float u0 = uv[0], v0 = uv[1], u1 = uv[2], v1 = uv[3];

        int idx = 0;

        // Single plane facing camera (will rotate with item rotation)
        // Vertex format: x, y, z, u, v, nx, ny, nz, r, g, b
        // Top-left
        vertices[idx++] = -h;
        vertices[idx++] = h;
        vertices[idx++] = 0;
        vertices[idx++] = u0;
        vertices[idx++] = v0;
        vertices[idx++] = 0;
        vertices[idx++] = 0;
        vertices[idx++] = 1;
        vertices[idx++] = 1.0f;
        vertices[idx++] = 1.0f;
        vertices[idx++] = 1.0f;
        // Bottom-left
        vertices[idx++] = -h;
        vertices[idx++] = -h;
        vertices[idx++] = 0;
        vertices[idx++] = u0;
        vertices[idx++] = v1;
        vertices[idx++] = 0;
        vertices[idx++] = 0;
        vertices[idx++] = 1;
        vertices[idx++] = 1.0f;
        vertices[idx++] = 1.0f;
        vertices[idx++] = 1.0f;
        // Bottom-right
        vertices[idx++] = h;
        vertices[idx++] = -h;
        vertices[idx++] = 0;
        vertices[idx++] = u1;
        vertices[idx++] = v1;
        vertices[idx++] = 0;
        vertices[idx++] = 0;
        vertices[idx++] = 1;
        vertices[idx++] = 1.0f;
        vertices[idx++] = 1.0f;
        vertices[idx++] = 1.0f;
        // Top-right
        vertices[idx++] = h;
        vertices[idx++] = h;
        vertices[idx++] = 0;
        vertices[idx++] = u1;
        vertices[idx++] = v0;
        vertices[idx++] = 0;
        vertices[idx++] = 0;
        vertices[idx++] = 1;
        vertices[idx++] = 1.0f;
        vertices[idx++] = 1.0f;
        vertices[idx++] = 1.0f;

        return vertices;
    }

    private CachedMesh createCachedMesh(ItemType type) {
        ItemRenderProfile profile = type.getRenderProfile();
        float[] vertices = profile.modelKind() == ItemRenderProfile.ModelKind.BLOCK
                ? buildCubeVertices(type.getPlacedBlock())
                : buildItemSpriteVertices(type);
        int[] indices = profile.modelKind() == ItemRenderProfile.ModelKind.BLOCK
                ? buildCubeIndices()
                : new int[] { 0, 1, 2, 2, 3, 0 };

        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        int ebo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        int stride = 11 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8 * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        glBindVertexArray(0);

        return new CachedMesh(vao, vbo, ebo, indices.length);
    }

    public void cleanup() {
        if (shader != null) {
            shader.cleanup();
        }
        for (CachedMesh mesh : meshCache.values()) {
            glDeleteBuffers(mesh.vbo);
            glDeleteBuffers(mesh.ebo);
            glDeleteVertexArrays(mesh.vao);
        }
        meshCache.clear();
    }
}
