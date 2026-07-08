package com.craftzero.graphics;

import com.craftzero.graphics.model.ModelPart;
import com.craftzero.graphics.model.PlayerModel;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.MapItemData;
import com.craftzero.inventory.ItemRenderProfile;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.progression.ArmorMaterial;
import com.craftzero.progression.ArmorSlot;
import com.craftzero.world.Block;
import com.craftzero.world.BlockShape;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders the player model in third-person view and first-person hand with held
 * items.
 */
public class PlayerRenderer {

    private final Renderer renderer;
    private final ShaderProgram shader;

    private PlayerModel playerModel;

    // Model matrix
    private final Matrix4f modelMatrix;
    private final Matrix4f firstPersonProjectionMatrix;

    // Scale factor (Minecraft model units are 1/16th of a block)
    private static final float MODEL_SCALE = 1.0f / 16.0f;
    private static final float FIRST_PERSON_BASE_FOV = 70.0f;
    private static final float FIRST_PERSON_MIN_FOV = 62.0f;
    private static final float FIRST_PERSON_MAX_FOV = 78.0f;
    private static final float FIRST_PERSON_FOV_RESPONSE = 0.22f;
    private static final float FIRST_PERSON_NEAR_PLANE = 0.05f;
    private static final float FIRST_PERSON_FAR_PLANE = 16.0f;
    private static final Vector3f HURT_FLASH_COLOR = new Vector3f(1.0f, 0.4f, 0.4f);
    private static final float RELEASE_CONSUME_USE_TICKS = 32.0f;
    private static final float RELEASE_CONSUME_PULL_POWER = 27.0f;
    private static final float RELEASE_CONSUME_BOB_TICK_PERIOD = 4.0f;
    private static final float RELEASE_BOW_DRAW_SECONDS = 1.0f;
    private static final float FIRST_PERSON_EMPTY_HAND_X = 1.05f;
    private static final float FIRST_PERSON_EMPTY_HAND_Y = -1.25f;
    private static final float FIRST_PERSON_EMPTY_HAND_Z = -1.10f;
    private static final float FIRST_PERSON_HELD_HAND_X = 0.95f;
    private static final float FIRST_PERSON_HELD_HAND_Y = -1.15f;
    private static final float FIRST_PERSON_HELD_HAND_Z = -1.20f;
    private static final float FIRST_PERSON_SPRITE_HAND_X = 0.82f;
    private static final float FIRST_PERSON_SPRITE_HAND_Y = -1.08f;
    private static final float FIRST_PERSON_SPRITE_HAND_Z = -1.05f;
    private static final float FIRST_PERSON_HAND_SCALE = 1.55f;
    private static final float FIRST_PERSON_SPRITE_HAND_SCALE = 1.42f;
    private static final float FIRST_PERSON_SPRITE_SWING_X = 0.30f;
    private static final float FIRST_PERSON_SPRITE_SWING_Y = 0.13f;
    private static final float FIRST_PERSON_SPRITE_SWING_Z = 0.12f;
    private static final float FIRST_PERSON_SPRITE_SWING_ROT_Y = 14.0f;
    private static final float FIRST_PERSON_SPRITE_SWING_ROT_Z = 13.0f;
    private static final float FIRST_PERSON_SPRITE_SWING_ROT_X = 52.0f;
    private static final float FIRST_PERSON_SPRITE_HAND_SWING_FOLLOW = 0.45f;

    // Player texture
    private Texture playerTexture;

    // Textures for held items
    private Texture atlas;
    private Texture itemsTexture;
    private Mesh fireOverlayMesh;

    // Legacy dynamic held meshes kept only for cleanup compatibility.
    private int itemVao;
    private int itemVbo;
    private int blockVao;
    private int blockVbo;
    private int blockEbo;
    private final Map<HeldItemMeshKey, HeldMesh> heldItemMeshCache = new HashMap<>();
    private final Map<HeldBlockMeshKey, HeldMesh> heldBlockMeshCache = new HashMap<>();
    private static final int HELD_VERTEX_FLOATS = 11;
    private static final int HELD_ITEM_SPRITE_PIXELS = 16;
    private static final float HELD_ITEM_THICKNESS = 1.0f / 16.0f;
    private static final float DYNAMIC_ITEM_OVERLAY_Z = HELD_ITEM_THICKNESS * 0.72f;
    private static final int FIRST_PERSON_MAP_TEXTURE_SIZE = 146;
    private static final int FIRST_PERSON_MAP_INSET = 9;
    private static final int FIRST_PERSON_MAP_SIZE = MapItemData.MAP_SIZE;
    private static final int FIRST_PERSON_MAP_BORDER_RGB = 0x8E7650;
    private static final int FIRST_PERSON_MAP_PAPER_RGB = 0xD3C08B;
    private static final int FIRST_PERSON_MAP_UNKNOWN_RGB = 0xC8B681;
    private static final int FIRST_PERSON_MAP_EDGE_DARK_RGB = 0x5C482D;
    private static final int FIRST_PERSON_MAP_CONTENT_SHADOW_RGB = 0x4A3923;
    private static final int FIRST_PERSON_MAP_CONTENT_HIGHLIGHT_RGB = 0xE8D6A5;
    private HeldMesh firstPersonMapMesh;
    private HeldMesh dynamicItemOverlayMesh;
    private Texture firstPersonMapTexture;
    private int firstPersonMapTextureHash = Integer.MIN_VALUE;

    private record HeldMesh(int vao, int vbo, int ebo, int drawCount, boolean indexed) {
    }

    private record HeldItemMeshKey(ItemType type, int bowDrawFrame) {
    }

    private record HeldBlockMeshKey(BlockType type, int metadata) {
        HeldBlockMeshKey {
            metadata &= 15;
        }
    }

    enum FirstPersonUsePose {
        NONE,
        GENERIC_USE,
        EAT_DRINK,
        BLOCK,
        BOW_DRAW
    }

    record FirstPersonUseTransform(
            FirstPersonUsePose pose,
            float translateX,
            float translateY,
            float translateZ,
            float rotateX,
            float rotateY,
            float rotateZ,
            float scaleX,
            float scaleY,
            float scaleZ) {
        static FirstPersonUseTransform identity() {
            return new FirstPersonUseTransform(FirstPersonUsePose.NONE,
                    0.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f,
                    1.0f, 1.0f, 1.0f);
        }
    }

    private record ThirdPersonHeldUseAdjustment(
            float handX,
            float handY,
            float handZ,
            float rotateX,
            float rotateY,
            float rotateZ,
            float offsetX,
            float offsetY,
            float offsetZ) {
        static ThirdPersonHeldUseAdjustment identity() {
            return new ThirdPersonHeldUseAdjustment(
                    0.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.0f);
        }
    }

    enum ArmorModelPart {
        HEAD,
        BODY,
        RIGHT_ARM,
        LEFT_ARM,
        RIGHT_LEG,
        LEFT_LEG
    }

    record ArmorRenderLayer(
            ArmorSlot slot,
            ArmorMaterial material,
            int textureLayer,
            String texturePath,
            Set<ArmorModelPart> parts) {
        ArmorRenderLayer {
            parts = parts == null ? Set.of() : Set.copyOf(parts);
        }

        boolean renders(ArmorModelPart part) {
            return parts.contains(part);
        }
    }

    public PlayerRenderer(Renderer renderer) {
        this.renderer = renderer;
        this.shader = renderer.getShaderProgram();
        this.modelMatrix = new Matrix4f();
        this.firstPersonProjectionMatrix = new Matrix4f();
    }

    public void init() {
        playerModel = new PlayerModel();
        playerModel.buildMeshes();
        playerTexture = MobTexture.get("/textures/mob/char.png");
        fireOverlayMesh = createFireOverlayMesh();
        buildHeldItemMeshes();
        System.out.println("PlayerRenderer initialized");
    }

    /**
     * Set textures for held item rendering.
     * Should be called after world is initialized.
     */
    public void setTextures(Texture atlas, Texture itemsTexture) {
        this.atlas = atlas;
        this.itemsTexture = itemsTexture;
    }

    /**
     * Build simple meshes for rendering held items (a flat quad for items, a cube
     * for blocks).
     */
    private void buildHeldItemMeshes() {
        // Held item meshes are created lazily per ItemType/BlockType so the atlas UVs
        // are stable and buffers are not rewritten during rendering.
    }

    private void buildBlockMesh() {
        // Larger cube for held blocks (fills more of screen like Minecraft)
        float s = 0.5f; // Half-size

        // Use GL_DYNAMIC_DRAW so we can update UVs per-block
        // 6 faces * 4 vertices * 8 floats = 192 floats
        float[] blockVertices = new float[24 * 8];
        buildBlockFaces(blockVertices, s, 0, 0, 1, 1); // Placeholder UVs

        int[] indices = {
                0, 1, 2, 0, 2, 3, // Front
                4, 5, 6, 4, 6, 7, // Back
                8, 9, 10, 8, 10, 11, // Top
                12, 13, 14, 12, 14, 15, // Bottom
                16, 17, 18, 16, 18, 19, // Right
                20, 21, 22, 20, 22, 23, // Left
        };

        blockVao = glGenVertexArrays();
        blockVbo = glGenBuffers();
        blockEbo = glGenBuffers();

        glBindVertexArray(blockVao);
        glBindBuffer(GL_ARRAY_BUFFER, blockVbo);
        glBufferData(GL_ARRAY_BUFFER, blockVertices, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, blockEbo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);
        glDisableVertexAttribArray(3);
        glVertexAttrib3f(3, 1.0f, 1.0f, 1.0f);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private void buildBlockFaces(float[] v, float s, float u1, float v1, float u2, float v2) {
        int i = 0;
        // Front (z = s)
        i = setVert(v, i, -s, -s, s, u1, v2, 0, 0, 1);
        i = setVert(v, i, s, -s, s, u2, v2, 0, 0, 1);
        i = setVert(v, i, s, s, s, u2, v1, 0, 0, 1);
        i = setVert(v, i, -s, s, s, u1, v1, 0, 0, 1);
        // Back (z = -s)
        i = setVert(v, i, s, -s, -s, u1, v2, 0, 0, -1);
        i = setVert(v, i, -s, -s, -s, u2, v2, 0, 0, -1);
        i = setVert(v, i, -s, s, -s, u2, v1, 0, 0, -1);
        i = setVert(v, i, s, s, -s, u1, v1, 0, 0, -1);
        // Top (y = s)
        i = setVert(v, i, -s, s, s, u1, v2, 0, 1, 0);
        i = setVert(v, i, s, s, s, u2, v2, 0, 1, 0);
        i = setVert(v, i, s, s, -s, u2, v1, 0, 1, 0);
        i = setVert(v, i, -s, s, -s, u1, v1, 0, 1, 0);
        // Bottom (y = -s)
        i = setVert(v, i, -s, -s, -s, u1, v2, 0, -1, 0);
        i = setVert(v, i, s, -s, -s, u2, v2, 0, -1, 0);
        i = setVert(v, i, s, -s, s, u2, v1, 0, -1, 0);
        i = setVert(v, i, -s, -s, s, u1, v1, 0, -1, 0);
        // Right (x = s)
        i = setVert(v, i, s, -s, s, u1, v2, 1, 0, 0);
        i = setVert(v, i, s, -s, -s, u2, v2, 1, 0, 0);
        i = setVert(v, i, s, s, -s, u2, v1, 1, 0, 0);
        i = setVert(v, i, s, s, s, u1, v1, 1, 0, 0);
        // Left (x = -s)
        i = setVert(v, i, -s, -s, -s, u1, v2, -1, 0, 0);
        i = setVert(v, i, -s, -s, s, u2, v2, -1, 0, 0);
        i = setVert(v, i, -s, s, s, u2, v1, -1, 0, 0);
        setVert(v, i, -s, s, -s, u1, v1, -1, 0, 0);
    }

    private int setVert(float[] v, int i, float x, float y, float z, float u, float vv, float nx, float ny, float nz) {
        v[i++] = x;
        v[i++] = y;
        v[i++] = z;
        v[i++] = u;
        v[i++] = vv;
        v[i++] = nx;
        v[i++] = ny;
        v[i++] = nz;
        return i;
    }

    private HeldMesh getHeldItemMesh(ItemType type) {
        return getHeldItemMesh(type, 0);
    }

    private HeldMesh getHeldItemMesh(ItemType type, int bowDrawFrame) {
        return heldItemMeshCache.computeIfAbsent(new HeldItemMeshKey(type, bowDrawFrame), this::createHeldItemMesh);
    }

    private HeldMesh getHeldBlockMesh(BlockType type, int metadata) {
        return heldBlockMeshCache.computeIfAbsent(new HeldBlockMeshKey(type, metadata), this::createHeldBlockMesh);
    }

    private HeldMesh getFirstPersonMapMesh() {
        if (firstPersonMapMesh == null) {
            firstPersonMapMesh = createFirstPersonMapMesh();
        }
        return firstPersonMapMesh;
    }

    private HeldMesh getDynamicItemOverlayMesh() {
        if (dynamicItemOverlayMesh == null) {
            dynamicItemOverlayMesh = createFlatOverlayMesh();
        }
        return dynamicItemOverlayMesh;
    }

    private HeldMesh createFirstPersonMapMesh() {
        return createFlatOverlayMesh();
    }

    private HeldMesh createFlatOverlayMesh() {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        float half = 0.5f;
        addHeldQuad(vertices, indices,
                -half, -half, 0.0f, 0.0f, 1.0f,
                half, -half, 0.0f, 1.0f, 1.0f,
                half, half, 0.0f, 1.0f, 0.0f,
                -half, half, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f);

        float[] vertexArray = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            vertexArray[i] = vertices.get(i);
        }
        int[] indexArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }
        return uploadHeldMesh(vertexArray, indexArray);
    }

    private HeldMesh createHeldItemMesh(HeldItemMeshKey key) {
        float[] uv = ItemTextureResolver.getUv(key.type(), key.bowDrawFrame());
        List<Float> vertexList = new ArrayList<>();
        List<Integer> indexList = new ArrayList<>();
        float frontZ = HELD_ITEM_THICKNESS * 0.5f;
        float backZ = -HELD_ITEM_THICKNESS * 0.5f;

        addHeldQuad(vertexList, indexList,
                -0.5f, -0.5f, frontZ, uv[0], uv[3],
                0.5f, -0.5f, frontZ, uv[2], uv[3],
                0.5f, 0.5f, frontZ, uv[2], uv[1],
                -0.5f, 0.5f, frontZ, uv[0], uv[1],
                0.0f, 0.0f, 1.0f);
        addHeldQuad(vertexList, indexList,
                -0.5f, 0.5f, backZ, uv[0], uv[1],
                0.5f, 0.5f, backZ, uv[2], uv[1],
                0.5f, -0.5f, backZ, uv[2], uv[3],
                -0.5f, -0.5f, backZ, uv[0], uv[3],
                0.0f, 0.0f, -1.0f);

        for (int pixel = 0; pixel < HELD_ITEM_SPRITE_PIXELS; pixel++) {
            float t0 = pixel / (float) HELD_ITEM_SPRITE_PIXELS;
            float t1 = (pixel + 1) / (float) HELD_ITEM_SPRITE_PIXELS;
            float x0 = -0.5f + t0;
            float x1 = -0.5f + t1;
            float y0 = -0.5f + t0;
            float y1 = -0.5f + t1;
            float u0 = lerp(uv[0], uv[2], t0);
            float u1 = lerp(uv[0], uv[2], t1);
            float v0 = lerp(uv[3], uv[1], t0);
            float v1 = lerp(uv[3], uv[1], t1);

            addHeldQuad(vertexList, indexList,
                    x0, -0.5f, backZ, u0, uv[3],
                    x0, -0.5f, frontZ, u0, uv[3],
                    x0, 0.5f, frontZ, u0, uv[1],
                    x0, 0.5f, backZ, u0, uv[1],
                    -1.0f, 0.0f, 0.0f);
            addHeldQuad(vertexList, indexList,
                    x1, -0.5f, frontZ, u1, uv[3],
                    x1, -0.5f, backZ, u1, uv[3],
                    x1, 0.5f, backZ, u1, uv[1],
                    x1, 0.5f, frontZ, u1, uv[1],
                    1.0f, 0.0f, 0.0f);
            addHeldQuad(vertexList, indexList,
                    -0.5f, y0, frontZ, uv[0], v0,
                    -0.5f, y0, backZ, uv[0], v0,
                    0.5f, y0, backZ, uv[2], v0,
                    0.5f, y0, frontZ, uv[2], v0,
                    0.0f, -1.0f, 0.0f);
            addHeldQuad(vertexList, indexList,
                    -0.5f, y1, backZ, uv[0], v1,
                    -0.5f, y1, frontZ, uv[0], v1,
                    0.5f, y1, frontZ, uv[2], v1,
                    0.5f, y1, backZ, uv[2], v1,
                    0.0f, 1.0f, 0.0f);
        }

        float[] vertices = new float[vertexList.size()];
        for (int i = 0; i < vertexList.size(); i++) {
            vertices[i] = vertexList.get(i);
        }
        int[] indices = new int[indexList.size()];
        for (int i = 0; i < indexList.size(); i++) {
            indices[i] = indexList.get(i);
        }
        return uploadHeldMesh(vertices, indices);
    }

    private void addHeldQuad(List<Float> vertices, List<Integer> indices,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3,
            float nx, float ny, float nz) {
        int base = vertices.size() / HELD_VERTEX_FLOATS;
        addHeldVertex(vertices, x0, y0, z0, u0, v0, nx, ny, nz);
        addHeldVertex(vertices, x1, y1, z1, u1, v1, nx, ny, nz);
        addHeldVertex(vertices, x2, y2, z2, u2, v2, nx, ny, nz);
        addHeldVertex(vertices, x3, y3, z3, u3, v3, nx, ny, nz);
        indices.add(base);
        indices.add(base + 1);
        indices.add(base + 2);
        indices.add(base);
        indices.add(base + 2);
        indices.add(base + 3);
    }

    private void addHeldVertex(List<Float> vertices, float x, float y, float z,
            float u, float v, float nx, float ny, float nz) {
        vertices.add(x);
        vertices.add(y);
        vertices.add(z);
        vertices.add(u);
        vertices.add(v);
        vertices.add(nx);
        vertices.add(ny);
        vertices.add(nz);
        vertices.add(1.0f);
        vertices.add(1.0f);
        vertices.add(1.0f);
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private HeldMesh createHeldBlockMesh(HeldBlockMeshKey key) {
        float[] vertices = heldBlockVertices(key.type(), key.metadata());
        int[] indices = heldBlockIndices();
        return uploadHeldMesh(vertices, indices);
    }

    static float[] heldBlockVertices(BlockType type, int metadata) {
        float s = 0.5f;
        BlockShape.Cuboid cube = new BlockShape.Cuboid(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
        float[] vertices = new float[24 * HELD_VERTEX_FLOATS];
        int i = 0;
        for (int face = 0; face < 6; face++) {
            float[] positions = Block.getCuboidFaceVertices(face, -s, -s, -s, cube);
            float[] uv = Block.getFaceTexCoords(type, face, metadata);
            float[] normals = Block.getFaceNormals(face);
            for (int vertex = 0; vertex < 4; vertex++) {
                i = setColoredVert(vertices, i,
                        positions[vertex * 3], positions[vertex * 3 + 1], positions[vertex * 3 + 2],
                        uv[vertex * 2], uv[vertex * 2 + 1],
                        normals[vertex * 3], normals[vertex * 3 + 1], normals[vertex * 3 + 2]);
            }
        }
        return vertices;
    }

    private static int[] heldBlockIndices() {
        int[] indices = new int[36];
        int i = 0;
        for (int face = 0; face < 6; face++) {
            for (int index : Block.getFaceIndices(face * 4)) {
                indices[i++] = index;
            }
        }
        return indices;
    }

    private static int setColoredVert(float[] v, int i, float x, float y, float z,
            float u, float vv, float nx, float ny, float nz) {
        v[i++] = x;
        v[i++] = y;
        v[i++] = z;
        v[i++] = u;
        v[i++] = vv;
        v[i++] = nx;
        v[i++] = ny;
        v[i++] = nz;
        v[i++] = 1.0f;
        v[i++] = 1.0f;
        v[i++] = 1.0f;
        return i;
    }

    private HeldMesh uploadHeldMesh(float[] vertices, int[] indices) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        int ebo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        int stride = HELD_VERTEX_FLOATS * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8 * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return new HeldMesh(vao, vbo, ebo, indices.length, true);
    }

    private void renderHeldMesh(HeldMesh mesh) {
        glBindVertexArray(mesh.vao());
        if (mesh.indexed()) {
            glDrawElements(GL_TRIANGLES, mesh.drawCount(), GL_UNSIGNED_INT, 0);
        } else {
            glDrawArrays(GL_TRIANGLES, 0, mesh.drawCount());
        }
        glBindVertexArray(0);
    }

    public void render(Player player, Camera camera, float partialTick, int cameraMode) {
        if (cameraMode == 0) {
            if (player.isSleeping()) {
                return;
            }
            renderFirstPersonHand(player, camera, partialTick);
            return;
        }

        if (playerTexture == null)
            return;

        Vector3f pos = player.getPosition();
        Vector3f prevPos = player.getPrevPosition();

        float renderX = prevPos.x + (pos.x - prevPos.x) * partialTick;
        float renderY = prevPos.y + (pos.y - prevPos.y) * partialTick;
        float renderZ = prevPos.z + (pos.z - prevPos.z) * partialTick;

        float entityBrightness = computeEntityBrightness(player, pos.x, pos.y + 1.0f, pos.z);
        renderer.setEntityBrightness(entityBrightness);

        boolean sleeping = player.isSleeping();
        float bodyYaw = sleeping ? player.getSleepingRenderYaw() : player.getRenderYawOffset(partialTick);

        float targetHeadYaw = camera.getYaw();
        if (cameraMode == 2) {
            targetHeadYaw += 180;
        }

        float headYaw = sleeping ? 0.0f : targetHeadYaw - bodyYaw;
        while (headYaw >= 180)
            headYaw -= 360;
        while (headYaw < -180)
            headYaw += 360;

        float prevDist = player.getPrevDistanceWalked();
        float currDist = player.getDistanceWalked();
        float limbSwing = prevDist + (currDist - prevDist) * partialTick;
        float ageInTicks = System.currentTimeMillis() / 50.0f;

        float limbSwingAmount = sleeping ? 0.0f : player.getLimbSwingAmount(partialTick);

        float headPitch = sleeping ? 0.0f : camera.getPitch();
        if (cameraMode == 2) {
            headPitch = -headPitch;
        }

        if (sleeping) {
            playerModel.animateSleeping();
        } else {
            boolean blocking = player.isBlockingItem();
            boolean drawingBow = player.isDrawingBow();
            boolean consuming = player.isUsingItem() && player.isEatingOrDrinkingItem()
                    && !blocking && !drawingBow;
            playerModel.animate(limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch,
                    player.getSwingProgress(partialTick), player.isSneaking(),
                    blocking, drawingBow, consuming, player.getUseProgress(partialTick));
        }

        if (cameraMode == 2 && player.getSwingProgress(partialTick) > 0) {
            float swing = player.getSwingProgress(partialTick);
            float sinSqrtSwing = (float) Math.sin(Math.sqrt(swing) * Math.PI);
            float swingRot = (float) Math.toRadians(80.0f) * sinSqrtSwing;
            swingRot += (float) Math.sin(swing * Math.PI) * 0.3f;
            // "Make it go forward" -> Remove negative sign from swingRot
            // Previously: -swingRot (Backwards?) -> Now: swingRot (Forward?)
            // Also, checking model, usually rightArm is main hand, but if code uses leftArm
            // here specifically for Mode 2 mirror or something,
            // we stick to flipping the rotation.
            playerModel.leftArm.setRotation(swingRot, sinSqrtSwing * 0.6f, sinSqrtSwing * 0.2f);
        }

        // Death animation - player falls over
        float deathRotation = 0;
        if (player.isDead()) {
            deathRotation = Math.min(player.getDeathTime() * 0.1f, 1.5f);
        }

        configurePlayerModelMatrix(modelMatrix, renderX, renderY, renderZ, bodyYaw, deathRotation,
                sleeping, MODEL_SCALE);

        playerModel.root.calculateTransform(modelMatrix);

        float hurtFlash = playerHurtFlash(player);
        setPlayerHurtFlash(hurtFlash);

        glDisable(GL_CULL_FACE);
        playerTexture.bind(0);
        renderModelPart(playerModel.root);
        playerTexture.unbind();

        clearPlayerHurtFlash();

        // Render held item in third person
        if (!sleeping && (atlas != null || itemsTexture != null)) {
            renderHeldItemThirdPerson(player, partialTick, entityBrightness);
        }
        setPlayerHurtFlash(hurtFlash);
        renderArmorLayers(player, renderX, renderY, renderZ, bodyYaw, deathRotation, sleeping);
        clearPlayerHurtFlash();

        if (!sleeping && player.isOnFire() && atlas != null) {
            renderFireOverlay(player, atlas, renderX, renderY, renderZ, bodyYaw);
        }

        glEnable(GL_CULL_FACE);

        // Reset entity brightness for block rendering
        renderer.setEntityBrightness(0.0f);
    }

    private void renderModelPart(ModelPart part) {
        if (part.getMesh() != null) {
            shader.setUniform("modelMatrix", part.getWorldTransform());
            part.getMesh().render();
        }
        for (ModelPart child : part.getChildren()) {
            renderModelPart(child);
        }
    }

    private void renderArmorLayers(Player player, float renderX, float renderY, float renderZ,
            float bodyYaw, float deathRotation, boolean sleeping) {
        ItemStack[] armor = player.getInventory().getArmor();
        List<ArmorRenderLayer> layers = armorRenderLayers(armor);
        if (layers.isEmpty()) {
            return;
        }
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(-1.0f, -1.0f);
        for (ArmorRenderLayer layer : layers) {
            renderArmorLayer(layer, renderX, renderY, renderZ, bodyYaw, deathRotation, sleeping);
        }
        glDisable(GL_POLYGON_OFFSET_FILL);
    }

    static List<ArmorRenderLayer> armorRenderLayers(ItemStack[] armor) {
        if (armor == null) {
            return List.of();
        }
        List<ArmorRenderLayer> layers = new ArrayList<>(ArmorSlot.values().length);
        for (ArmorSlot slot : ArmorSlot.values()) {
            int index = slot.getIndex();
            if (index < 0 || index >= armor.length) {
                continue;
            }
            ItemStack stack = armor[index];
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ArmorMaterial material = ArmorMaterial.materialOf(stack.getType());
            if (material != null) {
                int textureLayer = slot == ArmorSlot.LEGGINGS ? 2 : 1;
                layers.add(new ArmorRenderLayer(slot, material, textureLayer,
                        armorTexturePath(material, textureLayer),
                        armorPartsForSlot(slot)));
            }
        }
        return layers;
    }

    private static Set<ArmorModelPart> armorPartsForSlot(ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> EnumSet.of(ArmorModelPart.HEAD);
            case CHESTPLATE -> EnumSet.of(ArmorModelPart.BODY, ArmorModelPart.RIGHT_ARM, ArmorModelPart.LEFT_ARM);
            case LEGGINGS -> EnumSet.of(ArmorModelPart.BODY, ArmorModelPart.RIGHT_LEG, ArmorModelPart.LEFT_LEG);
            case BOOTS -> EnumSet.of(ArmorModelPart.RIGHT_LEG, ArmorModelPart.LEFT_LEG);
        };
    }

    private void renderArmorLayer(ArmorRenderLayer layer, float renderX, float renderY, float renderZ,
            float bodyYaw, float deathRotation, boolean sleeping) {
        Texture texture = MobTexture.get(layer.texturePath());
        if (texture == null) {
            return;
        }
        Matrix4f armorMatrix = configurePlayerModelMatrix(new Matrix4f(), renderX, renderY, renderZ,
                bodyYaw, deathRotation, sleeping, MODEL_SCALE * 1.025f);
        playerModel.root.calculateTransform(armorMatrix);
        shader.setUniform("alphaCutoff", 0.1f);
        texture.bind(0);
        renderArmorModelParts(layer);
        texture.unbind();
        shader.setUniform("alphaCutoff", 0.0f);
    }

    private void renderArmorModelParts(ArmorRenderLayer layer) {
        if (layer.renders(ArmorModelPart.HEAD)) {
            renderModelPart(playerModel.head);
        }
        if (layer.renders(ArmorModelPart.BODY)) {
            renderModelPart(playerModel.body);
        }
        if (layer.renders(ArmorModelPart.RIGHT_ARM)) {
            renderModelPart(playerModel.rightArm);
        }
        if (layer.renders(ArmorModelPart.LEFT_ARM)) {
            renderModelPart(playerModel.leftArm);
        }
        if (layer.renders(ArmorModelPart.RIGHT_LEG)) {
            renderModelPart(playerModel.rightLeg);
        }
        if (layer.renders(ArmorModelPart.LEFT_LEG)) {
            renderModelPart(playerModel.leftLeg);
        }
    }

    private void renderFireOverlay(Player player, Texture terrainAtlas,
            float x, float y, float z, float bodyYaw) {
        if (fireOverlayMesh == null) {
            return;
        }
        terrainAtlas.bind(0);
        shader.setUniform("alphaCutoff", 0.1f);
        shader.setUniform("hurtFlash", 0.0f);
        modelMatrix.identity();
        modelMatrix.translate(x, y + player.getHeight() * 0.5f, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        float width = Math.max(player.getWidth() * 1.35f, 0.7f);
        modelMatrix.scale(width, player.getHeight() * 1.08f, width);
        shader.setUniform("modelMatrix", modelMatrix);
        fireOverlayMesh.render();
        shader.setUniform("alphaCutoff", 0.0f);
        terrainAtlas.unbind();
    }

    private static float playerHurtFlash(Player player) {
        float hurtFlash = player == null ? 0.0f : player.getHurtFlash();
        if (player != null && player.isDead()) {
            hurtFlash = Math.max(hurtFlash, 0.5f);
        }
        return hurtFlash;
    }

    private void setPlayerHurtFlash(float hurtFlash) {
        shader.setUniform("hurtFlashColor", HURT_FLASH_COLOR);
        shader.setUniform("hurtFlash", Math.max(0.0f, Math.min(1.0f, hurtFlash)));
    }

    private void clearPlayerHurtFlash() {
        shader.setUniform("hurtFlash", 0.0f);
        shader.setUniform("hurtFlashColor", HURT_FLASH_COLOR);
    }

    private Matrix4f configurePlayerModelMatrix(Matrix4f matrix, float renderX, float renderY, float renderZ,
            float bodyYaw, float deathRotation, boolean sleeping, float scale) {
        matrix.identity();
        matrix.translate(renderX, renderY, renderZ);
        matrix.rotateY((float) Math.toRadians(-bodyYaw));
        if (sleeping) {
            matrix.rotateX((float) Math.toRadians(-90.0f));
            matrix.translate(0.0f, -0.2f, 0.0f);
        } else {
            matrix.rotateZ(deathRotation);
        }
        matrix.scale(scale, scale, scale);
        return matrix;
    }

    private static String armorTexturePath(ArmorMaterial material, int layer) {
        String prefix = switch (material) {
            case LEATHER -> "cloth";
            case CHAIN -> "chain";
            case IRON -> "iron";
            case DIAMOND -> "diamond";
            case GOLD -> "gold";
        };
        return "/textures/armor/" + prefix + "_" + layer + ".png";
    }

    private void renderFirstPersonHand(Player player, Camera camera, float partialTick) {
        if (playerTexture == null)
            return;

        Vector3f pos = player.getPosition();
        float entityBrightness = computeEntityBrightness(player, pos.x, pos.y + 1.0f, pos.z);
        renderer.setEntityBrightness(entityBrightness);

        glClear(GL_DEPTH_BUFFER_BIT);
        glDisable(GL_CULL_FACE);
        applyFirstPersonProjection(camera);

        float swingProgress = player.getSwingProgress(partialTick);
        float useProgress = player.getUseProgress(partialTick);
        float walkDist = player.getPrevDistanceWalked()
                + (player.getDistanceWalked() - player.getPrevDistanceWalked()) * partialTick;

        // Get held item
        ItemStack heldItem = player.getInventory().getItemInHand();
        boolean holdingItem = heldItem != null && !heldItem.isEmpty();
        ItemType heldType = holdingItem ? heldItem.getType() : null;
        ItemRenderProfile heldProfile = holdingItem && heldType != null ? heldType.getRenderProfile() : null;
        boolean holdingBlock = heldProfile != null && heldProfile.modelKind() == ItemRenderProfile.ModelKind.BLOCK;
        boolean holdingSpriteItem = heldProfile != null && heldProfile.modelKind() == ItemRenderProfile.ModelKind.SPRITE;
        boolean blocking = player.isBlockingItem();
        boolean drawingBow = holdingItem && heldType == ItemType.BOW && player.isDrawingBow();
        boolean eating = holdingItem && heldType != null
                && player.isUsingItem()
                && player.isEatingOrDrinkingItem()
                && !blocking
                && !drawingBow;
        boolean activeSpriteUsePose = holdingSpriteItem
                && ((drawingBow && heldType == ItemType.BOW) || blocking || eating);
        boolean holdingMap = holdingItem && heldType == ItemType.MAP;

        // Get slot switch animation progress (0 = retracted, 1 = visible)
        float slotSwitchProgress = player.getSlotSwitchProgress(partialTick);
        // Ease-out for smooth animation
        float switchOffsetLinear = 1.0f - slotSwitchProgress;
        float switchOffset = switchOffsetLinear * switchOffsetLinear; // Quadratic for smooth deceleration

        Vector3f camPos = camera.getPosition();
        modelMatrix.identity();
        modelMatrix.translate(camPos.x, camPos.y, camPos.z);
        modelMatrix.rotateY((float) Math.toRadians(-camera.getYaw()));
        modelMatrix.rotateX((float) Math.toRadians(-camera.getPitch()));

        // 1. Base Position - sprite items use their own hand anchor so the
        // visible grip lines up with the shallower lower-right item pose.
        float baseX = holdingSpriteItem
                ? FIRST_PERSON_SPRITE_HAND_X
                : holdingItem ? FIRST_PERSON_HELD_HAND_X : FIRST_PERSON_EMPTY_HAND_X;
        float baseY = holdingSpriteItem
                ? FIRST_PERSON_SPRITE_HAND_Y
                : holdingItem ? FIRST_PERSON_HELD_HAND_Y : FIRST_PERSON_EMPTY_HAND_Y;
        float baseZ = holdingSpriteItem
                ? FIRST_PERSON_SPRITE_HAND_Z
                : holdingItem ? FIRST_PERSON_HELD_HAND_Z : FIRST_PERSON_EMPTY_HAND_Z;

        // Apply slot switch animation (retract down when switching). Sprite
        // items use the old linear equip path, so the hand follows that same
        // cadence instead of drifting separately from the held item.
        float handSwitchOffset = holdingSpriteItem ? switchOffsetLinear : switchOffset;
        baseY -= handSwitchOffset * 1.5f; // Move down/out of view when retracted

        modelMatrix.translate(baseX, baseY, baseZ);
        float genericHandUseProgress = holdingSpriteItem ? 0.0f : useProgress;
        boolean spriteHandSwing = holdingSpriteItem && !activeSpriteUsePose;

        // --- SWING TRANSLATE ---
        if (spriteHandSwing) {
            applyFirstPersonSpriteHandSwingTranslate(modelMatrix, swingProgress);
        } else if (swingProgress > 0) {
            float phase = swingProgress;
            float forwardExtend = (phase < 0.4f) ? (phase / 0.4f) : Math.max(0, 1.0f - (phase - 0.4f) / 0.6f);
            forwardExtend = (float) Math.sin(forwardExtend * Math.PI * 0.5f);
            modelMatrix.translate(0, forwardExtend * 0.2f, -forwardExtend * 0.1f);
        }

        // --- USE ANIMATION (block placing) ---
        if (genericHandUseProgress > 0) {
            float phase = genericHandUseProgress;
            // Quick forward jab animation
            float forwardJab = (float) Math.sin(phase * Math.PI);
            modelMatrix.translate(0, forwardJab * 0.05f, -forwardJab * 0.15f);
        }

        // 2. Base Rotation
        modelMatrix.rotateY((float) Math.toRadians(180));
        modelMatrix.rotateX((float) Math.toRadians(-110));
        modelMatrix.rotateZ((float) Math.toRadians(-20));
        modelMatrix.rotateY((float) Math.toRadians(-40));

        // 3. Bobbing
        float handBobScale = holdingSpriteItem ? 0.0f : 1.0f;
        float bobX = (float) Math.sin(walkDist * 0.6662f) * 0.02f * handBobScale;
        float bobY = (float) Math.cos(walkDist * 1.3324f) * 0.015f * handBobScale;
        modelMatrix.translate(bobX, bobY, 0);

        // 4. Swing Rotation
        if (spriteHandSwing) {
            applyFirstPersonSpriteHandSwingRotate(modelMatrix, swingProgress);
        } else if (swingProgress > 0) {
            float phase = swingProgress;
            float downSwing = (phase > 0.3f) ? (float) Math.sin((phase - 0.3f) / 0.7f * Math.PI) : 0;
            modelMatrix.rotateX((float) Math.toRadians(downSwing * 60.0f));
        }

        // 5. Use Rotation
        if (genericHandUseProgress > 0) {
            float jabRotation = (float) Math.sin(genericHandUseProgress * Math.PI) * 15.0f;
            modelMatrix.rotateX((float) Math.toRadians(jabRotation));
        }

        if (activeSpriteUsePose) {
            applyFirstPersonSpriteHandUsePose(modelMatrix, heldType, useProgress, blocking, eating, drawingBow);
        }

        float handScale = holdingSpriteItem ? FIRST_PERSON_SPRITE_HAND_SCALE : FIRST_PERSON_HAND_SCALE;
        modelMatrix.scale(MODEL_SCALE * handScale, MODEL_SCALE * handScale, MODEL_SCALE * handScale);

        float origLeftPX = playerModel.leftArm.getPivotX();
        float origLeftPY = playerModel.leftArm.getPivotY();
        float origLeftPZ = playerModel.leftArm.getPivotZ();
        float origLeftRX = playerModel.leftArm.getRotationX();
        float origLeftRY = playerModel.leftArm.getRotationY();
        float origLeftRZ = playerModel.leftArm.getRotationZ();
        float origRightPX = playerModel.rightArm.getPivotX();
        float origRightPY = playerModel.rightArm.getPivotY();
        float origRightPZ = playerModel.rightArm.getPivotZ();
        float origRightRX = playerModel.rightArm.getRotationX();
        float origRightRY = playerModel.rightArm.getRotationY();
        float origRightRZ = playerModel.rightArm.getRotationZ();

        if (holdingMap) {
            renderFirstPersonMap(player, heldItem, camPos, camera, swingProgress, walkDist, switchOffset);
        } else if (!holdingItem || holdingSpriteItem) {
            playerModel.leftArm.setPivot(0, 0, 0);
            playerModel.leftArm.setRotation(0, 0, 0);
            playerModel.leftArm.calculateTransform(modelMatrix);

            playerTexture.bind(0);
            renderModelPart(playerModel.leftArm);
        }

        // Render held item (block or tool)
        if (!holdingMap && holdingItem && (atlas != null || itemsTexture != null)) {
            float heldItemSwitchOffset = holdingSpriteItem ? switchOffsetLinear : switchOffset;
            renderHeldItemFirstPerson(player, heldItem, heldType, heldProfile, holdingBlock, holdingSpriteItem, camPos,
                    camera, swingProgress, useProgress, walkDist, entityBrightness, heldItemSwitchOffset, blocking,
                    eating, drawingBow);
        }

        playerModel.leftArm.setPivot(origLeftPX, origLeftPY, origLeftPZ);
        playerModel.leftArm.setRotation(origLeftRX, origLeftRY, origLeftRZ);
        playerModel.rightArm.setPivot(origRightPX, origRightPY, origRightPZ);
        playerModel.rightArm.setRotation(origRightRX, origRightRY, origRightRZ);
        restoreWorldProjection(camera);
        glEnable(GL_CULL_FACE);
        renderer.setEntityBrightness(0.0f);
    }

    private void applyFirstPersonProjection(Camera camera) {
        float worldFov = Math.max(30.0f, Math.min(110.0f, camera.getFov()));
        float viewmodelFov = FIRST_PERSON_BASE_FOV + (worldFov - FIRST_PERSON_BASE_FOV) * FIRST_PERSON_FOV_RESPONSE;
        viewmodelFov = Math.max(FIRST_PERSON_MIN_FOV, Math.min(FIRST_PERSON_MAX_FOV, viewmodelFov));
        firstPersonProjectionMatrix.identity().perspective(
                (float) Math.toRadians(viewmodelFov),
                camera.getAspectRatio(),
                FIRST_PERSON_NEAR_PLANE,
                FIRST_PERSON_FAR_PLANE);
        shader.setUniform("projectionMatrix", firstPersonProjectionMatrix);
    }

    private void restoreWorldProjection(Camera camera) {
        shader.setUniform("projectionMatrix", camera.getProjectionMatrix());
    }

    /**
     * Render held item in first person view.
     */
    private void renderHeldItemFirstPerson(Player player, ItemStack stack, ItemType type, ItemRenderProfile profile,
            boolean isBlock, boolean isSpriteItem,
            Vector3f camPos, Camera camera, float swingProgress, float useProgress,
            float walkDist, float brightness, float slotSwitchOffset, boolean blocking, boolean eating,
            boolean drawingBow) {

        // Set up item model matrix
        Matrix4f itemMatrix = new Matrix4f();
        itemMatrix.identity();
        itemMatrix.translate(camPos.x, camPos.y, camPos.z);
        itemMatrix.rotateY((float) Math.toRadians(-camera.getYaw()));
        itemMatrix.rotateX((float) Math.toRadians(-camera.getPitch()));

        if (isSpriteItem) {
            renderHeldSpriteFirstPerson(player, stack, type, profile, itemMatrix, swingProgress, useProgress,
                    walkDist, slotSwitchOffset, blocking, eating, drawingBow);
            return;
        }

        itemMatrix.translate(
                profile.firstPersonOffsetX(),
                profile.firstPersonOffsetY() - slotSwitchOffset * profile.firstPersonEquipDrop(),
                profile.firstPersonOffsetZ());

        applyFirstPersonHeldSwing(itemMatrix, swingProgress);
        applyFirstPersonHeldUse(itemMatrix, type, useProgress, blocking, false, drawingBow);

        // Bobbing
        float bobX = (float) Math.sin(walkDist * 0.6662f) * 0.015f;
        float bobY = (float) Math.cos(walkDist * 1.3324f) * 0.01f;
        itemMatrix.translate(bobX, bobY, 0);

        // Scale and orient the item
        if (isBlock) {
            // Block: render as 3D cube (replaces hand entirely)
            // Scale and orientation adjusted to match Minecraft first-person view
            itemMatrix.scale(profile.firstPersonScale());
            itemMatrix.rotateY((float) Math.toRadians(profile.firstPersonRotY()));
            itemMatrix.rotateX((float) Math.toRadians(profile.firstPersonRotX()));
            itemMatrix.rotateZ((float) Math.toRadians(profile.firstPersonRotZ()));

            if (atlas != null) {
                atlas.bind(0);
                shader.setUniform("alphaCutoff", 0.0f);
                shader.setUniform("modelMatrix", itemMatrix);
                HeldMesh mesh = getHeldBlockMesh(type.getPlacedBlock(), type.getPlacedBlockMetadata());
                renderHeldMesh(mesh);
                renderHeldItemGlint(stack, mesh, atlas, itemMatrix, 0.0f);
            }
        }
    }

    private void renderHeldSpriteFirstPerson(Player player, ItemStack stack, ItemType type, ItemRenderProfile profile,
            Matrix4f itemMatrix,
            float swingProgress, float useProgress, float walkDist, float slotSwitchOffset,
            boolean blocking, boolean eating, boolean drawingBow) {
        boolean activeUsePose = (drawingBow && type == ItemType.BOW) || blocking || eating;
        boolean consumeUsePose = eating && useProgress > 0.0f && !blocking && !drawingBow;
        if (consumeUsePose) {
            applyFirstPersonConsumeTransform(itemMatrix, useProgress);
            applyFirstPersonSpriteBaseTransform(itemMatrix, profile, slotSwitchOffset, 0.0f);
        } else {
            if (!activeUsePose) {
                applyFirstPersonSpriteSwingTranslate(itemMatrix, swingProgress);
            }
            applyFirstPersonSpriteBaseTransform(itemMatrix, profile, slotSwitchOffset,
                    activeUsePose ? 0.0f : swingProgress);
        }

        if (activeUsePose && !consumeUsePose) {
            applyFirstPersonHeldUse(itemMatrix, type, useProgress, blocking, eating, drawingBow);
        } else if (!consumeUsePose) {
            applyFirstPersonSpriteHeldUse(itemMatrix, type, useProgress, blocking, eating, drawingBow);
        }
        itemMatrix.rotateZ((float) Math.toRadians(profile.firstPersonRotZ()));
        itemMatrix.rotateX((float) Math.toRadians(profile.firstPersonRotX()));

        Texture texToUse = ItemTextureResolver.usesItemsAtlas(type) && itemsTexture != null ? itemsTexture : atlas;
        if (texToUse != null) {
            texToUse.bind(0);
            shader.setUniform("alphaCutoff", 0.1f);
            shader.setUniform("modelMatrix", itemMatrix);
            int bowDrawFrame = ItemTextureResolver.bowDrawFrame(type, drawingBow, useProgress);
            HeldMesh mesh = getHeldItemMesh(type, bowDrawFrame);
            renderHeldMesh(mesh);
            renderDynamicHeldItemOverlay(type, player != null ? player.getWorld() : null, player, itemMatrix);
            renderHeldItemGlint(stack, mesh, texToUse, itemMatrix, 0.1f);
            shader.setUniform("alphaCutoff", 0.0f);
        }
    }

    private void applyFirstPersonSpriteBaseTransform(Matrix4f itemMatrix, ItemRenderProfile profile,
            float slotSwitchOffset, float swingProgress) {
        itemMatrix.translate(
                profile.firstPersonOffsetX(),
                profile.firstPersonOffsetY() - slotSwitchOffset * profile.firstPersonEquipDrop(),
                profile.firstPersonOffsetZ());
        itemMatrix.rotateY((float) Math.toRadians(profile.firstPersonRotY()));
        applyFirstPersonSpriteHeldSwing(itemMatrix, swingProgress);
        itemMatrix.scale(profile.firstPersonScale());
    }

    private void renderFirstPersonMap(Player player, ItemStack stack, Vector3f camPos, Camera camera,
            float swingProgress, float walkDist, float slotSwitchOffset) {
        updateFirstPersonMapTexture(player, stack);
        if (firstPersonMapTexture == null) {
            return;
        }

        float sinSwing = swingProgress > 0.0f ? (float) Math.sin(swingProgress * swingProgress * Math.PI) : 0.0f;
        float sinSqrtSwing = swingProgress > 0.0f ? (float) Math.sin(Math.sqrt(swingProgress) * Math.PI) : 0.0f;
        float bobX = (float) Math.sin(walkDist * 0.6662f) * 0.018f;
        float bobY = (float) Math.cos(walkDist * 1.3324f) * 0.012f;

        Matrix4f mapMatrix = firstPersonCameraSpace(camPos, camera);
        mapMatrix.translate(
                -sinSqrtSwing * 0.05f + bobX,
                -0.42f - slotSwitchOffset * 1.05f + sinSwing * 0.04f + bobY,
                -1.08f - sinSqrtSwing * 0.05f);
        mapMatrix.rotateX((float) Math.toRadians(8.0f + sinSqrtSwing * 5.0f));
        mapMatrix.rotateZ((float) Math.toRadians(sinSwing * 2.5f));
        mapMatrix.scale(1.35f, 1.35f, 1.0f);

        shader.setUniform("alphaCutoff", 0.0f);
        shader.setUniform("modelMatrix", mapMatrix);
        firstPersonMapTexture.bind(0);
        renderHeldMesh(getFirstPersonMapMesh());

        if (playerTexture != null) {
            playerTexture.bind(0);
            renderFirstPersonMapArm(playerModel.rightArm, camPos, camera, true, swingProgress, walkDist,
                    slotSwitchOffset);
            renderFirstPersonMapArm(playerModel.leftArm, camPos, camera, false, swingProgress, walkDist,
                    slotSwitchOffset);
        }
    }

    private Matrix4f firstPersonCameraSpace(Vector3f camPos, Camera camera) {
        return new Matrix4f()
                .identity()
                .translate(camPos.x, camPos.y, camPos.z)
                .rotateY((float) Math.toRadians(-camera.getYaw()))
                .rotateX((float) Math.toRadians(-camera.getPitch()));
    }

    private void renderFirstPersonMapArm(ModelPart arm, Vector3f camPos, Camera camera, boolean leftSide,
            float swingProgress, float walkDist, float slotSwitchOffset) {
        float side = leftSide ? -1.0f : 1.0f;
        float sinSwing = swingProgress > 0.0f ? (float) Math.sin(swingProgress * swingProgress * Math.PI) : 0.0f;
        float sinSqrtSwing = swingProgress > 0.0f ? (float) Math.sin(Math.sqrt(swingProgress) * Math.PI) : 0.0f;
        float bobX = (float) Math.sin(walkDist * 0.6662f) * 0.015f;
        float bobY = (float) Math.cos(walkDist * 1.3324f) * 0.010f;

        Matrix4f armMatrix = firstPersonCameraSpace(camPos, camera);
        armMatrix.translate(
                side * (0.43f + sinSqrtSwing * 0.03f) + side * bobX,
                -0.72f - slotSwitchOffset * 1.1f + sinSwing * 0.025f + bobY,
                -0.88f - sinSqrtSwing * 0.06f);
        armMatrix.rotateY((float) Math.toRadians(side * 14.0f));
        armMatrix.rotateZ((float) Math.toRadians(side * 12.0f));
        armMatrix.rotateX((float) Math.toRadians(-18.0f));
        armMatrix.scale(MODEL_SCALE * 1.42f);

        arm.setPivot(0, 0, 0);
        arm.setRotation(
                (float) Math.toRadians(64.0f + sinSqrtSwing * 8.0f),
                (float) Math.toRadians(side * 9.0f),
                (float) Math.toRadians(side * 10.0f));
        arm.calculateTransform(armMatrix);
        renderModelPart(arm);
    }

    private void updateFirstPersonMapTexture(Player player, ItemStack stack) {
        if (firstPersonMapTexture == null) {
            firstPersonMapTexture = new Texture(FIRST_PERSON_MAP_TEXTURE_SIZE, FIRST_PERSON_MAP_TEXTURE_SIZE, GL_RGBA);
        }
        MapItemData.View view = MapItemData.view(player != null ? player.getWorld() : null, stack);
        byte[] pixels = buildFirstPersonMapPixels(view);
        int hash = Arrays.hashCode(pixels);
        if (hash != firstPersonMapTextureHash) {
            firstPersonMapTexture.updateRgba(pixels);
            firstPersonMapTextureHash = hash;
        }
    }

    private byte[] buildFirstPersonMapPixels(MapItemData.View view) {
        byte[] pixels = new byte[FIRST_PERSON_MAP_TEXTURE_SIZE * FIRST_PERSON_MAP_TEXTURE_SIZE * 4];
        for (int y = 0; y < FIRST_PERSON_MAP_TEXTURE_SIZE; y++) {
            for (int x = 0; x < FIRST_PERSON_MAP_TEXTURE_SIZE; x++) {
                int distanceFromEdge = Math.min(Math.min(x, y),
                        Math.min(FIRST_PERSON_MAP_TEXTURE_SIZE - 1 - x, FIRST_PERSON_MAP_TEXTURE_SIZE - 1 - y));
                int rgb = distanceFromEdge < 2
                        ? FIRST_PERSON_MAP_EDGE_DARK_RGB
                        : distanceFromEdge < FIRST_PERSON_MAP_INSET
                                ? shadeMapPaper(FIRST_PERSON_MAP_BORDER_RGB, x, y, 14)
                                : shadeMapPaper(FIRST_PERSON_MAP_PAPER_RGB, x, y, 9);
                putRgba(pixels, x, y, rgb, 255);
            }
        }
        drawFirstPersonMapContentFrame(pixels);

        byte[] colors = view != null ? view.colors() : null;
        boolean initialized = view != null && view.initialized()
                && colors != null && colors.length == FIRST_PERSON_MAP_SIZE * FIRST_PERSON_MAP_SIZE;
        for (int z = 0; z < FIRST_PERSON_MAP_SIZE; z++) {
            for (int x = 0; x < FIRST_PERSON_MAP_SIZE; x++) {
                int rgb = initialized
                        ? MapItemData.rgbForPaletteIndex(colors[x + z * FIRST_PERSON_MAP_SIZE] & 0xFF)
                        : FIRST_PERSON_MAP_UNKNOWN_RGB;
                rgb = shadeMapPaper(rgb, x, z, 5);
                putRgba(pixels, FIRST_PERSON_MAP_INSET + x, FIRST_PERSON_MAP_INSET + z, rgb, 255);
            }
        }
        shadeFirstPersonMapContentEdges(pixels);

        if (view != null && view.playerRotation() >= 0) {
            int markerX = FIRST_PERSON_MAP_INSET + clampInt(view.playerPixelX(), 0, FIRST_PERSON_MAP_SIZE - 1);
            int markerY = FIRST_PERSON_MAP_INSET + clampInt(view.playerPixelZ(), 0, FIRST_PERSON_MAP_SIZE - 1);
            boolean edgeClamped = markerX != FIRST_PERSON_MAP_INSET + view.playerPixelX()
                    || markerY != FIRST_PERSON_MAP_INSET + view.playerPixelZ();
            drawFirstPersonMapMarker(pixels, markerX, markerY, view.playerRotation(), edgeClamped);
        }
        return pixels;
    }

    private void drawFirstPersonMapContentFrame(byte[] pixels) {
        int left = FIRST_PERSON_MAP_INSET - 2;
        int top = FIRST_PERSON_MAP_INSET - 2;
        int right = FIRST_PERSON_MAP_INSET + FIRST_PERSON_MAP_SIZE + 1;
        int bottom = FIRST_PERSON_MAP_INSET + FIRST_PERSON_MAP_SIZE + 1;
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                boolean outsideContent = x < FIRST_PERSON_MAP_INSET
                        || x >= FIRST_PERSON_MAP_INSET + FIRST_PERSON_MAP_SIZE
                        || y < FIRST_PERSON_MAP_INSET
                        || y >= FIRST_PERSON_MAP_INSET + FIRST_PERSON_MAP_SIZE;
                if (!outsideContent) {
                    continue;
                }
                int base = readRgb(pixels, x, y);
                boolean topLeftEdge = x == left || y == top;
                boolean bottomRightEdge = x == right || y == bottom;
                int overlay = topLeftEdge ? FIRST_PERSON_MAP_CONTENT_HIGHLIGHT_RGB
                        : bottomRightEdge ? FIRST_PERSON_MAP_EDGE_DARK_RGB
                                : FIRST_PERSON_MAP_CONTENT_SHADOW_RGB;
                float alpha = topLeftEdge ? 0.22f : bottomRightEdge ? 0.42f : 0.28f;
                putRgba(pixels, x, y, blendRgb(base, overlay, alpha), 255);
            }
        }
    }

    private void shadeFirstPersonMapContentEdges(byte[] pixels) {
        int left = FIRST_PERSON_MAP_INSET;
        int top = FIRST_PERSON_MAP_INSET;
        int right = FIRST_PERSON_MAP_INSET + FIRST_PERSON_MAP_SIZE - 1;
        int bottom = FIRST_PERSON_MAP_INSET + FIRST_PERSON_MAP_SIZE - 1;
        for (int i = 0; i < FIRST_PERSON_MAP_SIZE; i++) {
            blendPixel(pixels, left + i, top, FIRST_PERSON_MAP_CONTENT_HIGHLIGHT_RGB, 0.18f);
            blendPixel(pixels, left, top + i, FIRST_PERSON_MAP_CONTENT_HIGHLIGHT_RGB, 0.14f);
            blendPixel(pixels, left + i, bottom, FIRST_PERSON_MAP_CONTENT_SHADOW_RGB, 0.18f);
            blendPixel(pixels, right, top + i, FIRST_PERSON_MAP_CONTENT_SHADOW_RGB, 0.16f);
        }
    }

    private int shadeMapPaper(int rgb, int x, int y, int strength) {
        int grain = ((x * 31 + y * 17 + (x ^ y) * 7) & 15) - 7;
        int delta = grain * strength / 12;
        int r = clampInt(((rgb >> 16) & 0xFF) + delta, 0, 255);
        int g = clampInt(((rgb >> 8) & 0xFF) + delta, 0, 255);
        int b = clampInt((rgb & 0xFF) + delta, 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private void drawFirstPersonMapMarker(byte[] pixels, int centerX, int centerY, int rotation, boolean edgeClamped) {
        float angle = rotation * ((float) Math.PI * 2.0f / 16.0f);
        float forwardX = (float) Math.sin(angle);
        float forwardY = -(float) Math.cos(angle);
        float sideX = (float) Math.cos(angle);
        float sideY = (float) Math.sin(angle);
        int radius = edgeClamped ? 3 : 5;
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                float forward = x * forwardX + y * forwardY;
                float side = x * sideX + y * sideY;
                float width = Math.max(0.6f, (radius - forward) * 0.34f);
                boolean insideArrow = forward >= -radius * 0.24f
                        && forward <= radius
                        && Math.abs(side) <= width;
                if (insideArrow || (Math.abs(x) <= 1 && Math.abs(y) <= 1)) {
                    putRgba(pixels, centerX + x, centerY + y, 0x1A1712, 255);
                }
            }
        }
        int inner = Math.max(2, radius - 2);
        for (int y = -inner; y <= inner; y++) {
            for (int x = -inner; x <= inner; x++) {
                float forward = x * forwardX + y * forwardY;
                float side = x * sideX + y * sideY;
                float width = Math.max(0.38f, (inner - forward) * 0.30f);
                boolean insideArrow = forward >= -inner * 0.18f
                        && forward <= inner
                        && Math.abs(side) <= width;
                if (insideArrow || (Math.abs(x) == 0 && Math.abs(y) == 0)) {
                    putRgba(pixels, centerX + x, centerY + y, edgeClamped ? 0xD9CF9E : 0xF4F0E4, 255);
                }
            }
        }
    }

    private int readRgb(byte[] pixels, int x, int y) {
        if (x < 0 || y < 0 || x >= FIRST_PERSON_MAP_TEXTURE_SIZE || y >= FIRST_PERSON_MAP_TEXTURE_SIZE) {
            return FIRST_PERSON_MAP_PAPER_RGB;
        }
        int index = (x + y * FIRST_PERSON_MAP_TEXTURE_SIZE) * 4;
        int r = pixels[index] & 0xFF;
        int g = pixels[index + 1] & 0xFF;
        int b = pixels[index + 2] & 0xFF;
        return (r << 16) | (g << 8) | b;
    }

    private void blendPixel(byte[] pixels, int x, int y, int overlayRgb, float alpha) {
        putRgba(pixels, x, y, blendRgb(readRgb(pixels, x, y), overlayRgb, alpha), 255);
    }

    private int blendRgb(int baseRgb, int overlayRgb, float alpha) {
        float amount = Math.max(0.0f, Math.min(1.0f, alpha));
        int br = (baseRgb >> 16) & 0xFF;
        int bg = (baseRgb >> 8) & 0xFF;
        int bb = baseRgb & 0xFF;
        int or = (overlayRgb >> 16) & 0xFF;
        int og = (overlayRgb >> 8) & 0xFF;
        int ob = overlayRgb & 0xFF;
        int r = clampInt(Math.round(br + (or - br) * amount), 0, 255);
        int g = clampInt(Math.round(bg + (og - bg) * amount), 0, 255);
        int b = clampInt(Math.round(bb + (ob - bb) * amount), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private void putRgba(byte[] pixels, int x, int y, int rgb, int alpha) {
        if (x < 0 || y < 0 || x >= FIRST_PERSON_MAP_TEXTURE_SIZE || y >= FIRST_PERSON_MAP_TEXTURE_SIZE) {
            return;
        }
        int index = (x + y * FIRST_PERSON_MAP_TEXTURE_SIZE) * 4;
        pixels[index] = (byte) ((rgb >> 16) & 0xFF);
        pixels[index + 1] = (byte) ((rgb >> 8) & 0xFF);
        pixels[index + 2] = (byte) (rgb & 0xFF);
        pixels[index + 3] = (byte) clampInt(alpha, 0, 255);
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void applyFirstPersonHeldSwing(Matrix4f itemMatrix, float swingProgress) {
        if (swingProgress <= 0) {
            return;
        }
        float sinSwing = (float) Math.sin(swingProgress * swingProgress * Math.PI);
        float sinSqrtSwing = (float) Math.sin(Math.sqrt(swingProgress) * Math.PI);
        itemMatrix.translate(-sinSqrtSwing * 0.28f, sinSwing * 0.08f, -sinSqrtSwing * 0.18f);
        itemMatrix.rotateY((float) Math.toRadians(-sinSwing * 20.0f));
        itemMatrix.rotateZ((float) Math.toRadians(-sinSqrtSwing * 20.0f));
        itemMatrix.rotateX((float) Math.toRadians(-sinSqrtSwing * 80.0f));
    }

    private void applyFirstPersonSpriteSwingTranslate(Matrix4f itemMatrix, float swingProgress) {
        if (swingProgress <= 0) {
            return;
        }
        float sinSqrtSwing = (float) Math.sin(Math.sqrt(swingProgress) * Math.PI);
        float sinSqrtSwingDouble = (float) Math.sin(Math.sqrt(swingProgress) * Math.PI * 2.0f);
        float sinSwing = (float) Math.sin(swingProgress * Math.PI);
        itemMatrix.translate(-sinSqrtSwing * FIRST_PERSON_SPRITE_SWING_X,
                sinSqrtSwingDouble * FIRST_PERSON_SPRITE_SWING_Y,
                -sinSwing * FIRST_PERSON_SPRITE_SWING_Z);
    }

    private void applyFirstPersonSpriteHeldSwing(Matrix4f itemMatrix, float swingProgress) {
        if (swingProgress <= 0) {
            return;
        }
        float sinSwing = (float) Math.sin(swingProgress * swingProgress * Math.PI);
        float sinSqrtSwing = (float) Math.sin(Math.sqrt(swingProgress) * Math.PI);
        itemMatrix.rotateY((float) Math.toRadians(-sinSwing * FIRST_PERSON_SPRITE_SWING_ROT_Y));
        itemMatrix.rotateZ((float) Math.toRadians(-sinSqrtSwing * FIRST_PERSON_SPRITE_SWING_ROT_Z));
        itemMatrix.rotateX((float) Math.toRadians(-sinSqrtSwing * FIRST_PERSON_SPRITE_SWING_ROT_X));
    }

    private static void applyFirstPersonSpriteHandSwingTranslate(Matrix4f handMatrix, float swingProgress) {
        if (swingProgress <= 0) {
            return;
        }
        float sinSqrtSwing = (float) Math.sin(Math.sqrt(swingProgress) * Math.PI);
        float sinSqrtSwingDouble = (float) Math.sin(Math.sqrt(swingProgress) * Math.PI * 2.0f);
        float sinSwing = (float) Math.sin(swingProgress * Math.PI);
        handMatrix.translate(
                -sinSqrtSwing * FIRST_PERSON_SPRITE_SWING_X * FIRST_PERSON_SPRITE_HAND_SWING_FOLLOW,
                sinSqrtSwingDouble * FIRST_PERSON_SPRITE_SWING_Y * FIRST_PERSON_SPRITE_HAND_SWING_FOLLOW,
                -sinSwing * FIRST_PERSON_SPRITE_SWING_Z * FIRST_PERSON_SPRITE_HAND_SWING_FOLLOW);
    }

    private static void applyFirstPersonSpriteHandSwingRotate(Matrix4f handMatrix, float swingProgress) {
        if (swingProgress <= 0) {
            return;
        }
        float sinSwing = (float) Math.sin(swingProgress * swingProgress * Math.PI);
        float sinSqrtSwing = (float) Math.sin(Math.sqrt(swingProgress) * Math.PI);
        handMatrix.rotateY((float) Math.toRadians(
                -sinSwing * FIRST_PERSON_SPRITE_SWING_ROT_Y * FIRST_PERSON_SPRITE_HAND_SWING_FOLLOW));
        handMatrix.rotateZ((float) Math.toRadians(
                -sinSqrtSwing * FIRST_PERSON_SPRITE_SWING_ROT_Z * FIRST_PERSON_SPRITE_HAND_SWING_FOLLOW));
        handMatrix.rotateX((float) Math.toRadians(
                -sinSqrtSwing * FIRST_PERSON_SPRITE_SWING_ROT_X * FIRST_PERSON_SPRITE_HAND_SWING_FOLLOW));
    }

    private void applyFirstPersonHeldUse(Matrix4f itemMatrix, ItemType type, float useProgress,
            boolean blocking, boolean eating, boolean drawingBow) {
        if (drawingBow && type == ItemType.BOW) {
            applyFirstPersonBowDrawTransform(itemMatrix, useProgress);
            return;
        }
        if (blocking) {
            applyFirstPersonBlockingTransform(itemMatrix);
            return;
        }
        if (eating && useProgress > 0.0f) {
            applyFirstPersonConsumeTransform(itemMatrix, useProgress);
            return;
        }
        FirstPersonUseTransform transform = firstPersonUseTransform(type, blocking, eating, drawingBow, useProgress);
        if (transform.pose() == FirstPersonUsePose.NONE) {
            return;
        }
        itemMatrix.translate(transform.translateX(), transform.translateY(), transform.translateZ());
        itemMatrix.rotateY((float) Math.toRadians(transform.rotateY()));
        itemMatrix.rotateX((float) Math.toRadians(transform.rotateX()));
        itemMatrix.rotateZ((float) Math.toRadians(transform.rotateZ()));
        itemMatrix.scale(transform.scaleX(), transform.scaleY(), transform.scaleZ());
    }

    private void applyFirstPersonSpriteHeldUse(Matrix4f itemMatrix, ItemType type, float useProgress,
            boolean blocking, boolean eating, boolean drawingBow) {
        FirstPersonUseTransform transform = firstPersonUseTransform(type, blocking, eating, drawingBow, useProgress);
        if (transform.pose() == FirstPersonUsePose.GENERIC_USE) {
            return;
        }
        applyFirstPersonHeldUse(itemMatrix, type, useProgress, blocking, eating, drawingBow);
    }

    private static void applyFirstPersonSpriteHandUsePose(Matrix4f handMatrix, ItemType type, float useProgress,
            boolean blocking, boolean eating, boolean drawingBow) {
        if (drawingBow && type == ItemType.BOW) {
            float p = clamp01(useProgress);
            float pull = releaseBowPull(p * RELEASE_BOW_DRAW_SECONDS * 20.0f);
            handMatrix.translate(-0.10f, 0.06f + pull * 0.04f, -0.04f - pull * 0.05f);
            handMatrix.rotateZ((float) Math.toRadians(-8.0f));
            handMatrix.rotateY((float) Math.toRadians(-10.0f - pull * 8.0f));
            handMatrix.rotateX((float) Math.toRadians(-4.0f - pull * 6.0f));
            return;
        }
        if (blocking) {
            handMatrix.translate(-0.06f, 0.08f, -0.08f);
            handMatrix.rotateY((float) Math.toRadians(18.0f));
            handMatrix.rotateX((float) Math.toRadians(-18.0f));
            handMatrix.rotateZ((float) Math.toRadians(8.0f));
            return;
        }
        if (eating && useProgress > 0.0f) {
            FirstPersonUseTransform transform = firstPersonConsumeTransform(useProgress);
            handMatrix.translate(
                    transform.translateX() * 0.30f,
                    transform.translateY() * 0.30f,
                    -0.02f);
            handMatrix.rotateY((float) Math.toRadians(transform.rotateY() * 0.35f));
            handMatrix.rotateX((float) Math.toRadians(transform.rotateX() * 0.35f));
            handMatrix.rotateZ((float) Math.toRadians(transform.rotateZ() * 0.35f));
        }
    }

    private static void applyFirstPersonBowDrawTransform(Matrix4f itemMatrix, float useProgress) {
        float p = clamp01(useProgress);
        float useTicks = p * RELEASE_BOW_DRAW_SECONDS * 20.0f;
        float pull = releaseBowPull(useTicks);
        itemMatrix.rotateZ((float) Math.toRadians(-18.0f));
        itemMatrix.rotateY((float) Math.toRadians(-12.0f));
        itemMatrix.rotateX((float) Math.toRadians(-8.0f));
        itemMatrix.translate(-0.9f, 0.2f, 0.0f);
        if (pull > 0.1f) {
            itemMatrix.translate(0.0f,
                    (float) Math.sin((useTicks - 0.1f) * 1.3f) * 0.01f * (pull - 0.1f),
                    0.0f);
        }
        itemMatrix.translate(0.0f, 0.0f, pull * 0.1f);
        itemMatrix.rotateZ((float) Math.toRadians(-335.0f));
        itemMatrix.rotateY((float) Math.toRadians(-50.0f));
        itemMatrix.translate(0.0f, 0.5f, 0.0f);
        itemMatrix.scale(1.0f, 1.0f, 1.0f + pull * 0.2f);
        itemMatrix.translate(0.0f, -0.5f, 0.0f);
        itemMatrix.rotateY((float) Math.toRadians(50.0f));
        itemMatrix.rotateZ((float) Math.toRadians(335.0f));
    }

    private static void applyFirstPersonBlockingTransform(Matrix4f itemMatrix) {
        itemMatrix.translate(-0.5f, 0.2f, 0.0f);
        itemMatrix.rotateY((float) Math.toRadians(30.0f));
        itemMatrix.rotateX((float) Math.toRadians(-80.0f));
        itemMatrix.rotateY((float) Math.toRadians(60.0f));
    }

    private static void applyFirstPersonConsumeTransform(Matrix4f itemMatrix, float useProgress) {
        FirstPersonUseTransform transform = firstPersonConsumeTransform(useProgress);
        itemMatrix.translate(transform.translateX(), transform.translateY(), transform.translateZ());
        itemMatrix.rotateY((float) Math.toRadians(transform.rotateY()));
        itemMatrix.rotateX((float) Math.toRadians(transform.rotateX()));
        itemMatrix.rotateZ((float) Math.toRadians(transform.rotateZ()));
    }

    private static float releaseBowPull(float useTicks) {
        float draw = useTicks / 20.0f;
        return clamp01((draw * draw + draw * 2.0f) / 3.0f);
    }

    static FirstPersonUseTransform firstPersonUseTransform(ItemType type, boolean blocking, boolean eating,
            boolean drawingBow, float useProgress) {
        float p = clamp01(useProgress);
        if (drawingBow && type == ItemType.BOW) {
            float useTicks = p * RELEASE_BOW_DRAW_SECONDS * 20.0f;
            float pull = releaseBowPull(useTicks);
            float tremble = pull > 0.1f
                    ? (float) Math.sin((useTicks - 0.1f) * 1.3f) * 0.01f * (pull - 0.1f)
                    : 0.0f;
            return new FirstPersonUseTransform(FirstPersonUsePose.BOW_DRAW,
                    -0.9f,
                    0.2f + tremble,
                    pull * 0.1f,
                    -8.0f,
                    -12.0f,
                    -18.0f,
                    1.0f,
                    1.0f,
                    1.0f + pull * 0.2f);
        }
        if (blocking) {
            return new FirstPersonUseTransform(FirstPersonUsePose.BLOCK,
                    -0.5f, 0.2f, 0.0f,
                    -80.0f, 90.0f, 0.0f,
                    1.0f, 1.0f, 1.0f);
        }
        if (eating && p > 0.0f) {
            return firstPersonConsumeTransform(p);
        }
        if (p > 0.0f) {
            float useSin = (float) Math.sin(p * Math.PI);
            return new FirstPersonUseTransform(FirstPersonUsePose.GENERIC_USE,
                    -useSin * 0.12f,
                    useSin * 0.05f,
                    -useSin * 0.10f,
                    -useSin * 25.0f,
                    0.0f,
                    0.0f,
                    1.0f, 1.0f, 1.0f);
        }
        return FirstPersonUseTransform.identity();
    }

    private static FirstPersonUseTransform firstPersonConsumeTransform(float progress) {
        float p = clamp01(progress);
        float remaining = 1.0f - p;
        float pull = 1.0f - (float) Math.pow(remaining, RELEASE_CONSUME_PULL_POWER);
        float useTicksRemaining = remaining * RELEASE_CONSUME_USE_TICKS + 1.0f;
        float biteBob = remaining < 0.8f
                ? Math.abs((float) Math.cos(useTicksRemaining / RELEASE_CONSUME_BOB_TICK_PERIOD * Math.PI)) * 0.10f
                : 0.0f;
        return new FirstPersonUseTransform(FirstPersonUsePose.EAT_DRINK,
                0.60f * pull,
                -0.50f * pull + biteBob,
                0.0f,
                10.0f * pull,
                90.0f * pull,
                30.0f * pull,
                1.0f, 1.0f, 1.0f);
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private float computeEntityBrightness(Player player, float x, float y, float z) {
        World world = player.getWorld();
        if (world == null) {
            return 1.0f;
        }
        int blockX = (int) Math.floor(x);
        int blockY = (int) Math.floor(y);
        int blockZ = (int) Math.floor(z);
        int sky = world.getSkyLight(blockX, blockY, blockZ);
        if (world.getDayCycleManager() != null) {
            sky = Math.round(sky * world.getDayCycleManager().getSunBrightness());
        }
        int block = world.getBlockLightIfLoaded(blockX, blockY, blockZ, 0);
        int lightLevel = Math.max(block, sky);
        float f = Math.max(0, Math.min(15, lightLevel)) / 15.0f;
        return Math.max(0.08f, f / (3.0f - 2.0f * f));
    }

    /**
     * Render held item in third person view (attached to arm).
     */
    private void renderHeldItemThirdPerson(Player player, float partialTick, float brightness) {
        ItemStack heldItem = player.getInventory().getItemInHand();
        if (heldItem == null || heldItem.isEmpty()) {
            return;
        }

        ItemType type = heldItem.getType();
        ItemRenderProfile profile = type.getRenderProfile();
        boolean isBlock = profile.modelKind() == ItemRenderProfile.ModelKind.BLOCK;
        boolean drawingBow = type == ItemType.BOW && player.isDrawingBow();
        float useProgress = player.getUseProgress(partialTick);
        boolean blocking = player.isBlockingItem();
        boolean consuming = player.isUsingItem() && player.isEatingOrDrinkingItem()
                && !blocking && !drawingBow;
        ThirdPersonHeldUseAdjustment useAdjustment =
                thirdPersonHeldUseAdjustment(type, blocking, consuming, drawingBow, useProgress);

        // Get the arm's world transform and position item at the "hand" end
        // NOTE: "leftArm" in PlayerModel is the VISUAL RIGHT ARM (pivot at X=6).
        Matrix4f armTransform = playerModel.leftArm.getWorldTransform();
        Matrix4f itemMatrix = new Matrix4f(armTransform);

        // Move to hand position (end of arm in model space)
        // Arm extends down from pivot (length is 12 units).
        // We translate in MODEL UNITS (pixels).
        // Hand is at Y = -10 (relative to pivot).
        // Refined 2: Slightly more front (Z=-3.0) and tiny bit left (X=-0.6).
        itemMatrix.translate(
                -1.0f + useAdjustment.handX(),
                -10.8f + useAdjustment.handY(),
                -2.2f + useAdjustment.handZ());

        // Scale correction!
        // The armTransform includes 1/16 scale. Item needs to be ~0.375 blocks large.
        // 0.375 * 16 = 6.0 model units.
        float itemScale = profile.thirdPersonScale() * 16.0f;
        itemMatrix.scale(itemScale);

        if (isBlock) {
            // Block Rendering
            // Rotate so the player holds the corner/edge of the block
            itemMatrix.rotateX((float) Math.toRadians(profile.thirdPersonRotX() + useAdjustment.rotateX()));
            itemMatrix.rotateY((float) Math.toRadians(profile.thirdPersonRotY() + useAdjustment.rotateY()));
            itemMatrix.rotateZ((float) Math.toRadians(profile.thirdPersonRotZ() + useAdjustment.rotateZ()));

            // Translate so the hand (origin) is at the side of the block, not center
            // Removed X=0.5f offset to put center in palm as requested.
            itemMatrix.translate(
                    profile.thirdPersonOffsetX() + useAdjustment.offsetX(),
                    profile.thirdPersonOffsetY() + useAdjustment.offsetY(),
                    profile.thirdPersonOffsetZ() + useAdjustment.offsetZ());

            if (atlas != null) {
                atlas.bind(0);
                shader.setUniform("alphaCutoff", 0.0f);
                shader.setUniform("modelMatrix", itemMatrix);
                HeldMesh mesh = getHeldBlockMesh(type.getPlacedBlock(), type.getPlacedBlockMetadata());
                renderHeldMesh(mesh);
                renderHeldItemGlint(heldItem, mesh, atlas, itemMatrix, 0.0f);
            }
        } else {
            // Item Rendering (Tools, Sticks)
            // Rotate to look like a tool held in hand
            itemMatrix.rotateX((float) Math.toRadians(profile.thirdPersonRotX() + useAdjustment.rotateX()));
            itemMatrix.rotateY((float) Math.toRadians(profile.thirdPersonRotY() + useAdjustment.rotateY()));
            itemMatrix.rotateZ((float) Math.toRadians(profile.thirdPersonRotZ() + useAdjustment.rotateZ()));

            // Translate to hold by the handle (bottom-right of texture)
            // Adjusted X to 0.0 to match centered grip logic
            itemMatrix.translate(
                    profile.thirdPersonOffsetX() + useAdjustment.offsetX(),
                    profile.thirdPersonOffsetY() + useAdjustment.offsetY(),
                    profile.thirdPersonOffsetZ() + useAdjustment.offsetZ());

            Texture texToUse = ItemTextureResolver.usesItemsAtlas(type) && itemsTexture != null ? itemsTexture : atlas;
            if (texToUse != null) {
                texToUse.bind(0);
                shader.setUniform("alphaCutoff", 0.1f);
                shader.setUniform("modelMatrix", itemMatrix);
                int bowDrawFrame = ItemTextureResolver.bowDrawFrame(type, drawingBow, useProgress);
                HeldMesh mesh = getHeldItemMesh(type, bowDrawFrame);
                renderHeldMesh(mesh);
                renderDynamicHeldItemOverlay(type, player.getWorld(), player, itemMatrix);
                renderHeldItemGlint(heldItem, mesh, texToUse, itemMatrix, 0.1f);
                shader.setUniform("alphaCutoff", 0.0f);
            }
        }
    }

    private static ThirdPersonHeldUseAdjustment thirdPersonHeldUseAdjustment(ItemType type, boolean blocking,
            boolean consuming, boolean drawingBow, float useProgress) {
        float p = clamp01(useProgress);
        if (drawingBow && type == ItemType.BOW) {
            float pull = clamp01((p * p + p * 2.0f) / 3.0f);
            return new ThirdPersonHeldUseAdjustment(
                    0.10f, 0.18f + pull * 0.18f, -0.42f - pull * 0.16f,
                    -4.0f, -12.0f - pull * 16.0f, -18.0f + pull * 4.0f,
                    -0.08f, -0.10f, -0.12f);
        }
        if (blocking) {
            return new ThirdPersonHeldUseAdjustment(
                    -0.22f, 0.20f, -0.20f,
                    -6.0f, -34.0f, -24.0f,
                    -0.04f, -0.08f, -0.04f);
        }
        if (consuming) {
            float pull = 1.0f - (float) Math.pow(1.0f - p, 8.0f);
            float biteBob = p > 0.2f
                    ? Math.abs((float) Math.cos(p * RELEASE_CONSUME_USE_TICKS
                            / RELEASE_CONSUME_BOB_TICK_PERIOD * Math.PI)) * 0.08f
                    : 0.0f;
            return new ThirdPersonHeldUseAdjustment(
                    -0.18f * pull,
                    0.18f * pull + biteBob,
                    -0.18f * pull,
                    -20.0f * pull,
                    18.0f * pull,
                    10.0f * pull,
                    -0.04f * pull,
                    -0.10f * pull,
                    -0.06f * pull);
        }
        return ThirdPersonHeldUseAdjustment.identity();
    }

    private void renderDynamicHeldItemOverlay(ItemType type, World world, Player player, Matrix4f itemMatrix) {
        ItemTextureResolver.DynamicItemState state = ItemTextureResolver.dynamicItemState(type, world, player);
        if (!state.active() || itemMatrix == null) {
            return;
        }

        shader.setUniform("solidColorMode", true);
        shader.setUniform("alphaCutoff", 0.0f);
        if (type == ItemType.COMPASS) {
            renderDynamicItemNeedle(itemMatrix, state.angleRadians() + (float) Math.PI,
                    0.16f, 0.020f, new Vector4f(0.82f, 0.82f, 0.82f, 0.94f));
            renderDynamicItemNeedle(itemMatrix, state.angleRadians(),
                    0.34f, 0.030f, new Vector4f(0.92f, 0.06f, 0.04f, 0.98f));
        } else if (type == ItemType.CLOCK) {
            renderDynamicItemNeedle(itemMatrix, state.angleRadians(),
                    0.30f, 0.030f, new Vector4f(1.0f, 0.78f, 0.18f, 0.98f));
        }
        renderDynamicItemHub(itemMatrix);
        shader.setUniform("solidColorMode", false);
        shader.setUniform("solidColor", new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
    }

    private void renderDynamicItemNeedle(Matrix4f itemMatrix, float angleRadians, float length, float width,
            Vector4f color) {
        Matrix4f needleMatrix = new Matrix4f(itemMatrix)
                .translate(0.0f, 0.0f, DYNAMIC_ITEM_OVERLAY_Z)
                .rotateZ(-angleRadians)
                .translate(0.0f, length * 0.5f, 0.0f)
                .scale(width, length, 1.0f);
        shader.setUniform("solidColor", color);
        shader.setUniform("modelMatrix", needleMatrix);
        renderHeldMesh(getDynamicItemOverlayMesh());
    }

    private void renderDynamicItemHub(Matrix4f itemMatrix) {
        Matrix4f hubMatrix = new Matrix4f(itemMatrix)
                .translate(0.0f, 0.0f, DYNAMIC_ITEM_OVERLAY_Z + 0.0015f)
                .scale(0.085f, 0.085f, 1.0f);
        shader.setUniform("solidColor", new Vector4f(0.08f, 0.06f, 0.04f, 0.92f));
        shader.setUniform("modelMatrix", hubMatrix);
        renderHeldMesh(getDynamicItemOverlayMesh());
    }

    private void renderHeldItemGlint(ItemStack stack, HeldMesh mesh, Texture baseTexture,
            Matrix4f itemMatrix, float alphaCutoff) {
        Texture glint = GuiTexture.getGlintTexture();
        if (!EnchantedItemVisuals.shouldDrawGlint(stack) || mesh == null || glint == null || baseTexture == null) {
            return;
        }

        glint.bind(1);
        shader.setUniform("textureSampler", 0);
        shader.setUniform("glintSampler", 1);
        shader.setUniform("glintMode", true);
        shader.setUniform("glintPhase", EnchantedItemVisuals.glintPhase());
        float[] color = EnchantedItemVisuals.glintColor();
        shader.setUniform("glintColor", new Vector3f(color[0], color[1], color[2]));
        shader.setUniform("glintAlpha", color.length >= 4 ? color[3] : 0.58f);
        shader.setUniform("modelMatrix", itemMatrix);
        shader.setUniform("alphaCutoff", alphaCutoff);

        glDepthMask(false);
        glDepthFunc(GL_EQUAL);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        for (int pass = 0; pass < 2; pass++) {
            shader.setUniform("glintPass", pass);
            renderHeldMesh(mesh);
        }
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthFunc(GL_LESS);
        glDepthMask(true);

        shader.setUniform("glintMode", false);
        shader.setUniform("glintPass", 0);
        glint.unbind();
        baseTexture.bind(0);
    }

    private Mesh createFireOverlayMesh() {
        float[] uv = BlockType.FIRE.getTextureCoords(2);
        float[] positions = {
                -0.5f, -0.5f, 0.0f,
                0.5f, -0.5f, 0.0f,
                0.5f, 0.5f, 0.0f,
                -0.5f, 0.5f, 0.0f,
                0.0f, -0.5f, -0.5f,
                0.0f, -0.5f, 0.5f,
                0.0f, 0.5f, 0.5f,
                0.0f, 0.5f, -0.5f,
        };
        float[] texCoords = {
                uv[0], uv[3],
                uv[2], uv[3],
                uv[2], uv[1],
                uv[0], uv[1],
                uv[0], uv[3],
                uv[2], uv[3],
                uv[2], uv[1],
                uv[0], uv[1],
        };
        float[] normals = {
                0, 0, 1,
                0, 0, 1,
                0, 0, 1,
                0, 0, 1,
                1, 0, 0,
                1, 0, 0,
                1, 0, 0,
                1, 0, 0,
        };
        float[] colors = {
                1, 1, 1,
                1, 1, 1,
                1, 1, 1,
                1, 1, 1,
                1, 1, 1,
                1, 1, 1,
                1, 1, 1,
                1, 1, 1,
        };
        int[] indices = {
                0, 1, 2, 0, 2, 3,
                4, 5, 6, 4, 6, 7,
        };
        return new Mesh(positions, texCoords, normals, colors, indices);
    }

    public void cleanup() {
        if (playerModel != null)
            playerModel.cleanup();

        // Clean up held item meshes
        if (itemVao != 0) {
            glDeleteVertexArrays(itemVao);
            glDeleteBuffers(itemVbo);
        }
        if (blockVao != 0) {
            glDeleteVertexArrays(blockVao);
            glDeleteBuffers(blockVbo);
            glDeleteBuffers(blockEbo);
        }
        for (HeldMesh mesh : heldItemMeshCache.values()) {
            deleteHeldMesh(mesh);
        }
        heldItemMeshCache.clear();
        for (HeldMesh mesh : heldBlockMeshCache.values()) {
            deleteHeldMesh(mesh);
        }
        heldBlockMeshCache.clear();
        if (firstPersonMapMesh != null) {
            deleteHeldMesh(firstPersonMapMesh);
            firstPersonMapMesh = null;
        }
        if (dynamicItemOverlayMesh != null) {
            deleteHeldMesh(dynamicItemOverlayMesh);
            dynamicItemOverlayMesh = null;
        }
        if (firstPersonMapTexture != null) {
            firstPersonMapTexture.cleanup();
            firstPersonMapTexture = null;
        }
        if (fireOverlayMesh != null) {
            fireOverlayMesh.cleanup();
        }
    }

    private void deleteHeldMesh(HeldMesh mesh) {
        glDeleteVertexArrays(mesh.vao());
        glDeleteBuffers(mesh.vbo());
        if (mesh.ebo() != 0) {
            glDeleteBuffers(mesh.ebo());
        }
    }
}
