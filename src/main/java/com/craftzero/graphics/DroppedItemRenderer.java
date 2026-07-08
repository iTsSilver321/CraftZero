package com.craftzero.graphics;

import com.craftzero.entity.DroppedItem;
import com.craftzero.inventory.ItemRenderProfile;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

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
    private static final float DYNAMIC_ITEM_OVERLAY_Z = 0.016f;
    static final int ITEM_VERTEX_FLOATS = 11;

    private ShaderProgram shader;
    private Matrix4f modelMatrix;
    private final Map<ItemType, CachedMesh> meshCache = new EnumMap<>(ItemType.class);
    private CachedMesh dynamicItemOverlayMesh;
    private boolean anaglyphColorCorrection;

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
        shader.createUniform("fogMode");
        shader.createUniform("fogStart");
        shader.createUniform("fogEnd");
        shader.createUniform("fogColor");
        shader.createUniform("ambientLight");
        shader.createUniform("lightDirection");
        shader.createUniform("lightColor");
        shader.createUniform("sunBrightness");
        shader.createUniform("alphaCutoff");
        shader.createUniform("glintSampler");
        shader.createUniform("glintMode");
        shader.createUniform("glintPass");
        shader.createUniform("glintPhase");
        shader.createUniform("glintColor");
        shader.createUniform("glintAlpha");
        shader.createUniform("anaglyphColorCorrection");
        shader.createUniform("solidColorMode");
        shader.createUniform("solidColor");

    }

    /**
     * Build mesh data for a specific block type.
     * Returns vertex data interleaved: pos(3) + uv(2) + normal(3) + color(3) = 11
     * floats per vertex
     */
    static float[] blockCubeVertices(ItemType type) {
        BlockType block = type.getPlacedBlock();
        int metadata = type.getPlacedBlockMetadata();
        float[] vertices = new float[6 * 4 * ITEM_VERTEX_FLOATS];
        int idx = 0;

        // Half size for centering at origin
        float h = 0.5f;

        for (int face = 0; face < 6; face++) {
            float[] faceVerts = Block.getFaceVertices(face, 0, 0, 0);
            float[] faceUVs = Block.getFaceTexCoords(block, face, metadata);
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
            com.craftzero.world.DayCycleManager dayCycle, World world) {
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
        shader.setUniform("glintSampler", 1);
        shader.setUniform("glintMode", false);
        shader.setUniform("glintPass", 0);
        shader.setUniform("glintPhase", 0.0f);
        shader.setUniform("anaglyphColorCorrection", anaglyphColorCorrection);
        shader.setUniform("solidColorMode", false);
        shader.setUniform("solidColor", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));

        // Fog settings from day cycle
        shader.setUniform("fogEnabled", true);
        shader.setUniform("fogDensity", 0.007f);
        shader.setUniform("fogMode", 1);
        float fogEnd = Math.max(32.0f, world.getRenderDistanceChunks() * 16.0f);
        shader.setUniform("fogStart", fogEnd * 0.70f);
        shader.setUniform("fogEnd", fogEnd);
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
            float alphaCutoff;
            if (profile.modelKind() == ItemRenderProfile.ModelKind.BLOCK) {
                alphaCutoff = type.getPlacedBlock().getRenderLayer() == com.craftzero.world.BlockRenderLayer.CUTOUT
                        ? 0.1f
                        : 0.0f;
            } else {
                alphaCutoff = 0.1f;
            }
            shader.setUniform("alphaCutoff", alphaCutoff);

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
            ItemStack stack = item.getStack();
            boolean enchanted = EnchantedItemVisuals.shouldDrawGlint(stack);

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
                if (enchanted) {
                    renderEnchantedGlint(mesh, currentTexture, alphaCutoff);
                }
                renderDynamicItemOverlay(type, world, camera, modelMatrix);
                glBindVertexArray(mesh.vao);
            }
        }

        glBindVertexArray(0);
        shader.unbind();
        if (currentTexture != null)
            currentTexture.unbind();
        glEnable(GL_CULL_FACE);
    }

    private void renderDynamicItemOverlay(ItemType type, World world, Camera camera, Matrix4f baseMatrix) {
        if (baseMatrix == null || camera == null) {
            return;
        }
        Vector3f viewer = camera.getPosition();
        ItemTextureResolver.DynamicItemState state = ItemTextureResolver.dynamicItemState(
                type, world, viewer.x, viewer.z, camera.getYaw());
        if (!state.active()) {
            return;
        }

        shader.setUniform("solidColorMode", true);
        shader.setUniform("alphaCutoff", 0.0f);
        if (type == ItemType.COMPASS) {
            renderDynamicOverlayNeedle(baseMatrix, state.angleRadians() + (float) Math.PI,
                    0.16f, 0.020f, new Vector4f(0.82f, 0.82f, 0.82f, 0.94f));
            renderDynamicOverlayNeedle(baseMatrix, state.angleRadians(),
                    0.34f, 0.030f, new Vector4f(0.92f, 0.06f, 0.04f, 0.98f));
        } else if (type == ItemType.CLOCK) {
            renderDynamicOverlayNeedle(baseMatrix, state.angleRadians(),
                    0.30f, 0.030f, new Vector4f(1.0f, 0.78f, 0.18f, 0.98f));
        }
        renderDynamicOverlayHub(baseMatrix);
        shader.setUniform("solidColorMode", false);
        shader.setUniform("solidColor", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
    }

    private void renderDynamicOverlayNeedle(Matrix4f baseMatrix, float angleRadians, float length, float width,
            Vector4f color) {
        Matrix4f needleMatrix = new Matrix4f(baseMatrix)
                .translate(0.0f, 0.0f, DYNAMIC_ITEM_OVERLAY_Z)
                .rotateZ(-angleRadians)
                .translate(0.0f, length * 0.5f, 0.0f)
                .scale(width, length, 1.0f);
        drawDynamicOverlayMesh(needleMatrix, color);
    }

    private void renderDynamicOverlayHub(Matrix4f baseMatrix) {
        Matrix4f hubMatrix = new Matrix4f(baseMatrix)
                .translate(0.0f, 0.0f, DYNAMIC_ITEM_OVERLAY_Z + 0.0015f)
                .scale(0.085f, 0.085f, 1.0f);
        drawDynamicOverlayMesh(hubMatrix, new Vector4f(0.08f, 0.06f, 0.04f, 0.92f));
    }

    private void drawDynamicOverlayMesh(Matrix4f overlayMatrix, Vector4f color) {
        CachedMesh overlay = dynamicItemOverlayMesh();
        shader.setUniform("solidColor", color);
        shader.setUniform("modelMatrix", overlayMatrix);
        glBindVertexArray(overlay.vao);
        glDrawElements(GL_TRIANGLES, overlay.vertexCount, GL_UNSIGNED_INT, 0);
    }

    private void renderEnchantedGlint(CachedMesh mesh, Texture baseTexture, float alphaCutoff) {
        Texture glint = GuiTexture.getGlintTexture();
        if (glint == null || mesh == null || baseTexture == null) {
            return;
        }

        glint.bind(1);
        shader.setUniform("glintMode", true);
        shader.setUniform("glintPhase", EnchantedItemVisuals.glintPhase());
        float[] color = EnchantedItemVisuals.glintColor();
        shader.setUniform("glintColor", new Vector3f(color[0], color[1], color[2]));
        shader.setUniform("glintAlpha", color.length >= 4 ? color[3] : 0.58f);
        shader.setUniform("alphaCutoff", alphaCutoff);

        glDepthMask(false);
        glDepthFunc(GL_EQUAL);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        for (int pass = 0; pass < 2; pass++) {
            shader.setUniform("glintPass", pass);
            glDrawElements(GL_TRIANGLES, mesh.vertexCount, GL_UNSIGNED_INT, 0);
        }
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthFunc(GL_LESS);
        glDepthMask(true);

        shader.setUniform("glintMode", false);
        shader.setUniform("glintPass", 0);
        shader.setUniform("alphaCutoff", alphaCutoff);
        glint.unbind();
        baseTexture.bind(0);
    }

    public void setAnaglyphColorCorrection(boolean enabled) {
        anaglyphColorCorrection = enabled;
    }

    private static boolean isTooFar(Camera camera, DroppedItem item) {
        if (item == null) {
            return true;
        }
        return RenderDistanceCulling.isPointTooFar(camera, item.getX(), item.getY(), item.getZ(),
                ITEM_RENDER_DISTANCE);
    }

    /**
     * Build a flat 2D sprite mesh for items (single plane).
     * Vertex format: pos(3) + uv(2) + normal(3) + color(3) = 11 floats per vertex
     */
    private float[] buildItemSpriteVertices(ItemType type) {
        float[] vertices = new float[4 * ITEM_VERTEX_FLOATS];
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

    private CachedMesh dynamicItemOverlayMesh() {
        if (dynamicItemOverlayMesh == null) {
            dynamicItemOverlayMesh = createDynamicOverlayMesh();
        }
        return dynamicItemOverlayMesh;
    }

    private CachedMesh createDynamicOverlayMesh() {
        float h = 0.5f;
        float[] vertices = {
                -h, h, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                -h, -h, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                h, -h, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                h, h, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f,
        };
        int[] indices = { 0, 1, 2, 2, 3, 0 };
        return uploadMesh(vertices, indices);
    }

    private CachedMesh createCachedMesh(ItemType type) {
        ItemRenderProfile profile = type.getRenderProfile();
        float[] vertices = profile.modelKind() == ItemRenderProfile.ModelKind.BLOCK
                ? blockCubeVertices(type)
                : buildItemSpriteVertices(type);
        int[] indices = profile.modelKind() == ItemRenderProfile.ModelKind.BLOCK
                ? buildCubeIndices()
                : new int[] { 0, 1, 2, 2, 3, 0 };

        return uploadMesh(vertices, indices);
    }

    private CachedMesh uploadMesh(float[] vertices, int[] indices) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        int ebo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        int stride = ITEM_VERTEX_FLOATS * Float.BYTES;
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
        if (dynamicItemOverlayMesh != null) {
            glDeleteBuffers(dynamicItemOverlayMesh.vbo);
            glDeleteBuffers(dynamicItemOverlayMesh.ebo);
            glDeleteVertexArrays(dynamicItemOverlayMesh.vao);
            dynamicItemOverlayMesh = null;
        }
    }
}
