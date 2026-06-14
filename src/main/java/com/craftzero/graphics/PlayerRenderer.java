package com.craftzero.graphics;

import com.craftzero.graphics.model.ModelPart;
import com.craftzero.graphics.model.PlayerModel;
import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemRenderProfile;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

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

    // Scale factor (Minecraft model units are 1/16th of a block)
    private static final float MODEL_SCALE = 1.0f / 16.0f;

    // Player texture
    private Texture playerTexture;

    // Textures for held items
    private Texture atlas;
    private Texture itemsTexture;

    // Legacy dynamic held meshes kept only for cleanup compatibility.
    private int itemVao;
    private int itemVbo;
    private int blockVao;
    private int blockVbo;
    private int blockEbo;
    private final Map<ItemType, HeldMesh> heldItemMeshCache = new HashMap<>();
    private final Map<BlockType, HeldMesh> heldBlockMeshCache = new HashMap<>();
    private static final int HELD_VERTEX_FLOATS = 11;

    private record HeldMesh(int vao, int vbo, int ebo, int drawCount, boolean indexed) {
    }

    public PlayerRenderer(Renderer renderer) {
        this.renderer = renderer;
        this.shader = renderer.getShaderProgram();
        this.modelMatrix = new Matrix4f();
    }

    public void init() {
        playerModel = new PlayerModel();
        playerModel.buildMeshes();
        playerTexture = MobTexture.get("/textures/mob/char.png");
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
        return heldItemMeshCache.computeIfAbsent(type, this::createHeldItemMesh);
    }

    private HeldMesh getHeldBlockMesh(BlockType type) {
        return heldBlockMeshCache.computeIfAbsent(type, this::createHeldBlockMesh);
    }

    private HeldMesh createHeldItemMesh(ItemType type) {
        float[] uv = ItemTextureResolver.getUv(type);
        float[] vertices = {
                -0.5f, -0.5f, 0.0f, uv[0], uv[3], 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                0.5f, -0.5f, 0.0f, uv[2], uv[3], 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                0.5f, 0.5f, 0.0f, uv[2], uv[1], 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                -0.5f, 0.5f, 0.0f, uv[0], uv[1], 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f,
        };
        int[] indices = { 0, 1, 2, 0, 2, 3 };
        return uploadHeldMesh(vertices, indices);
    }

    private HeldMesh createHeldBlockMesh(BlockType type) {
        float s = 0.5f;
        float[] vertices = new float[24 * HELD_VERTEX_FLOATS];
        float[] topUV = type.getTextureCoords(0);
        float[] bottomUV = type.getTextureCoords(1);
        float[] sideUV = type.getTextureCoords(2);
        int i = 0;
        i = setColoredVert(vertices, i, -s, -s, s, sideUV[0], sideUV[3], 0, 0, 1);
        i = setColoredVert(vertices, i, s, -s, s, sideUV[2], sideUV[3], 0, 0, 1);
        i = setColoredVert(vertices, i, s, s, s, sideUV[2], sideUV[1], 0, 0, 1);
        i = setColoredVert(vertices, i, -s, s, s, sideUV[0], sideUV[1], 0, 0, 1);
        i = setColoredVert(vertices, i, s, -s, -s, sideUV[0], sideUV[3], 0, 0, -1);
        i = setColoredVert(vertices, i, -s, -s, -s, sideUV[2], sideUV[3], 0, 0, -1);
        i = setColoredVert(vertices, i, -s, s, -s, sideUV[2], sideUV[1], 0, 0, -1);
        i = setColoredVert(vertices, i, s, s, -s, sideUV[0], sideUV[1], 0, 0, -1);
        i = setColoredVert(vertices, i, -s, s, s, topUV[0], topUV[3], 0, 1, 0);
        i = setColoredVert(vertices, i, s, s, s, topUV[2], topUV[3], 0, 1, 0);
        i = setColoredVert(vertices, i, s, s, -s, topUV[2], topUV[1], 0, 1, 0);
        i = setColoredVert(vertices, i, -s, s, -s, topUV[0], topUV[1], 0, 1, 0);
        i = setColoredVert(vertices, i, -s, -s, -s, bottomUV[0], bottomUV[3], 0, -1, 0);
        i = setColoredVert(vertices, i, s, -s, -s, bottomUV[2], bottomUV[3], 0, -1, 0);
        i = setColoredVert(vertices, i, s, -s, s, bottomUV[2], bottomUV[1], 0, -1, 0);
        i = setColoredVert(vertices, i, -s, -s, s, bottomUV[0], bottomUV[1], 0, -1, 0);
        i = setColoredVert(vertices, i, s, -s, s, sideUV[0], sideUV[3], 1, 0, 0);
        i = setColoredVert(vertices, i, s, -s, -s, sideUV[2], sideUV[3], 1, 0, 0);
        i = setColoredVert(vertices, i, s, s, -s, sideUV[2], sideUV[1], 1, 0, 0);
        i = setColoredVert(vertices, i, s, s, s, sideUV[0], sideUV[1], 1, 0, 0);
        i = setColoredVert(vertices, i, -s, -s, -s, sideUV[0], sideUV[3], -1, 0, 0);
        i = setColoredVert(vertices, i, -s, -s, s, sideUV[2], sideUV[3], -1, 0, 0);
        i = setColoredVert(vertices, i, -s, s, s, sideUV[2], sideUV[1], -1, 0, 0);
        setColoredVert(vertices, i, -s, s, -s, sideUV[0], sideUV[1], -1, 0, 0);

        int[] indices = {
                0, 1, 2, 0, 2, 3,
                4, 5, 6, 4, 6, 7,
                8, 9, 10, 8, 10, 11,
                12, 13, 14, 12, 14, 15,
                16, 17, 18, 16, 18, 19,
                20, 21, 22, 20, 22, 23,
        };
        return uploadHeldMesh(vertices, indices);
    }

    private int setColoredVert(float[] v, int i, float x, float y, float z,
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

        // Get world light at player position for entity lighting
        float entityBrightness = 1.0f;
        if (player.getWorld() != null) {
            int lightLevel = player.getWorld().getSkyLight(
                    (int) Math.floor(pos.x),
                    (int) Math.floor(pos.y + 1), // Sample at eye level
                    (int) Math.floor(pos.z));
            // Convert light level (0-15) to brightness (0.08-1.0)
            float f = Math.max(0, Math.min(15, lightLevel)) / 15.0f;
            entityBrightness = Math.max(0.08f, f / (3.0f - 2.0f * f));
        }
        renderer.setEntityBrightness(entityBrightness);

        float bodyYaw = player.getRenderYawOffset(partialTick);

        float targetHeadYaw = camera.getYaw();
        if (cameraMode == 2) {
            targetHeadYaw += 180;
        }

        float headYaw = targetHeadYaw - bodyYaw;
        while (headYaw >= 180)
            headYaw -= 360;
        while (headYaw < -180)
            headYaw += 360;

        float prevDist = player.getPrevDistanceWalked();
        float currDist = player.getDistanceWalked();
        float limbSwing = prevDist + (currDist - prevDist) * partialTick;
        float ageInTicks = System.currentTimeMillis() / 50.0f;

        float limbSwingAmount = player.getLimbSwingAmount(partialTick);

        float headPitch = camera.getPitch();
        if (cameraMode == 2) {
            headPitch = -headPitch;
        }

        playerModel.animate(limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch,
                player.getSwingProgress(partialTick), player.isSneaking());

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

        modelMatrix.identity();
        modelMatrix.translate(renderX, renderY, renderZ);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.rotateZ(deathRotation);
        modelMatrix.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

        playerModel.root.calculateTransform(modelMatrix);

        // Apply hurt flash when dead (red tint)
        if (player.isDead()) {
            shader.setUniform("hurtFlash", 0.5f);
        }

        glDisable(GL_CULL_FACE);
        playerTexture.bind(0);
        renderModelPart(playerModel.root);
        playerTexture.unbind();

        // Reset hurt flash
        if (player.isDead()) {
            shader.setUniform("hurtFlash", 0.0f);
        }

        // Render held item in third person
        if (atlas != null || itemsTexture != null) {
            renderHeldItemThirdPerson(player, partialTick, entityBrightness);
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

    private void renderFirstPersonHand(Player player, Camera camera, float partialTick) {
        if (playerTexture == null)
            return;

        Vector3f pos = player.getPosition();
        float entityBrightness = 1.0f;
        if (player.getWorld() != null) {
            int lightLevel = player.getWorld().getSkyLight(
                    (int) Math.floor(pos.x),
                    (int) Math.floor(pos.y + 1),
                    (int) Math.floor(pos.z));
            float f = Math.max(0, Math.min(15, lightLevel)) / 15.0f;
            entityBrightness = Math.max(0.08f, f / (3.0f - 2.0f * f));
        }
        renderer.setEntityBrightness(entityBrightness);

        glClear(GL_DEPTH_BUFFER_BIT);
        glDisable(GL_CULL_FACE);

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

        // Get slot switch animation progress (0 = retracted, 1 = visible)
        float slotSwitchProgress = player.getSlotSwitchProgress(partialTick);
        // Ease-out for smooth animation
        float switchOffset = (1.0f - slotSwitchProgress);
        switchOffset = switchOffset * switchOffset; // Quadratic for smooth deceleration

        Vector3f camPos = camera.getPosition();
        modelMatrix.identity();
        modelMatrix.translate(camPos.x, camPos.y, camPos.z);
        modelMatrix.rotateY((float) Math.toRadians(-camera.getYaw()));
        modelMatrix.rotateX((float) Math.toRadians(-camera.getPitch()));

        // 1. Base Position - adjust when holding item
        float baseX = holdingItem ? 0.95f : 1.05f;
        float baseY = holdingItem ? -1.15f : -1.25f;
        float baseZ = holdingItem ? -1.2f : -1.1f;

        // Apply slot switch animation (retract down when switching)
        baseY -= switchOffset * 1.5f; // Move down/out of view when retracted

        modelMatrix.translate(baseX, baseY, baseZ);

        // --- SWING TRANSLATE ---
        if (swingProgress > 0) {
            float phase = swingProgress;
            float forwardExtend = (phase < 0.4f) ? (phase / 0.4f) : Math.max(0, 1.0f - (phase - 0.4f) / 0.6f);
            forwardExtend = (float) Math.sin(forwardExtend * Math.PI * 0.5f);
            modelMatrix.translate(0, forwardExtend * 0.2f, -forwardExtend * 0.1f);
        }

        // --- USE ANIMATION (block placing) ---
        if (useProgress > 0) {
            float phase = useProgress;
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
        float bobX = (float) Math.sin(walkDist * 0.6662f) * 0.02f;
        float bobY = (float) Math.cos(walkDist * 1.3324f) * 0.015f;
        modelMatrix.translate(bobX, bobY, 0);

        // 4. Swing Rotation
        if (swingProgress > 0) {
            float phase = swingProgress;
            float downSwing = (phase > 0.3f) ? (float) Math.sin((phase - 0.3f) / 0.7f * Math.PI) : 0;
            modelMatrix.rotateX((float) Math.toRadians(downSwing * 60.0f));
        }

        // 5. Use Rotation
        if (useProgress > 0) {
            float jabRotation = (float) Math.sin(useProgress * Math.PI) * 15.0f;
            modelMatrix.rotateX((float) Math.toRadians(jabRotation));
        }

        modelMatrix.scale(MODEL_SCALE * 1.55f, MODEL_SCALE * 1.55f, MODEL_SCALE * 1.55f);

        float origPX = playerModel.leftArm.getPivotX();
        float origPY = playerModel.leftArm.getPivotY();
        float origPZ = playerModel.leftArm.getPivotZ();

        playerModel.leftArm.setPivot(0, 0, 0);
        playerModel.leftArm.setRotation(0, 0, 0);
        playerModel.leftArm.calculateTransform(modelMatrix);

        playerTexture.bind(0);
        renderModelPart(playerModel.leftArm);

        // Render held item (block or tool)
        if (holdingItem && (atlas != null || itemsTexture != null)) {
            renderHeldItemFirstPerson(heldType, heldProfile, holdingBlock, holdingSpriteItem, camPos, camera,
                    swingProgress, useProgress, walkDist, entityBrightness, switchOffset);
        }

        playerModel.leftArm.setPivot(origPX, origPY, origPZ);
        glEnable(GL_CULL_FACE);
        renderer.setEntityBrightness(0.0f);
    }

    /**
     * Render held item in first person view.
     */
    private void renderHeldItemFirstPerson(ItemType type, ItemRenderProfile profile, boolean isBlock,
            boolean isSpriteItem,
            Vector3f camPos, Camera camera, float swingProgress, float useProgress,
            float walkDist, float brightness, float slotSwitchOffset) {

        // Set up item model matrix
        Matrix4f itemMatrix = new Matrix4f();
        itemMatrix.identity();
        itemMatrix.translate(camPos.x, camPos.y, camPos.z);
        itemMatrix.rotateY((float) Math.toRadians(-camera.getYaw()));
        itemMatrix.rotateX((float) Math.toRadians(-camera.getPitch()));

        itemMatrix.translate(
                profile.firstPersonOffsetX(),
                profile.firstPersonOffsetY() - slotSwitchOffset * profile.firstPersonEquipDrop(),
                profile.firstPersonOffsetZ());

        if (swingProgress > 0) {
            float sinSwing = (float) Math.sin(swingProgress * swingProgress * Math.PI);
            float sinSqrtSwing = (float) Math.sin(Math.sqrt(swingProgress) * Math.PI);
            itemMatrix.translate(-sinSqrtSwing * 0.28f, sinSwing * 0.08f, -sinSqrtSwing * 0.18f);
            itemMatrix.rotateY((float) Math.toRadians(-sinSwing * 20.0f));
            itemMatrix.rotateZ((float) Math.toRadians(-sinSqrtSwing * 20.0f));
            itemMatrix.rotateX((float) Math.toRadians(-sinSqrtSwing * 80.0f));
        }

        if (useProgress > 0) {
            float useSin = (float) Math.sin(useProgress * Math.PI);
            itemMatrix.translate(-useSin * 0.12f, useSin * 0.05f, -useSin * 0.10f);
            itemMatrix.rotateX((float) Math.toRadians(-useSin * 25.0f));
        }

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
                renderHeldMesh(getHeldBlockMesh(type.getPlacedBlock()));
            }
        } else if (isSpriteItem) {
            // Tool: render as flat 2D sprite angled
            itemMatrix.scale(profile.firstPersonScale());
            itemMatrix.rotateX((float) Math.toRadians(profile.firstPersonRotX()));
            itemMatrix.rotateY((float) Math.toRadians(profile.firstPersonRotY()));
            itemMatrix.rotateZ((float) Math.toRadians(profile.firstPersonRotZ()));

            Texture texToUse = null;
            if (ItemTextureResolver.usesItemsAtlas(type) && itemsTexture != null) {
                texToUse = itemsTexture;
            } else if (atlas != null) {
                texToUse = atlas;
            }

            if (texToUse != null) {
                texToUse.bind(0);
                shader.setUniform("alphaCutoff", 0.1f);
                shader.setUniform("modelMatrix", itemMatrix);
                renderHeldMesh(getHeldItemMesh(type));
                shader.setUniform("alphaCutoff", 0.0f);
            }
        }
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

        // Get the arm's world transform and position item at the "hand" end
        // NOTE: "leftArm" in PlayerModel is the VISUAL RIGHT ARM (pivot at X=6).
        Matrix4f armTransform = playerModel.leftArm.getWorldTransform();
        Matrix4f itemMatrix = new Matrix4f(armTransform);

        // Move to hand position (end of arm in model space)
        // Arm extends down from pivot (length is 12 units).
        // We translate in MODEL UNITS (pixels).
        // Hand is at Y = -10 (relative to pivot).
        // Refined 2: Slightly more front (Z=-3.0) and tiny bit left (X=-0.6).
        itemMatrix.translate(-1.0f, -10.8f, -2.2f);

        // Scale correction!
        // The armTransform includes 1/16 scale. Item needs to be ~0.375 blocks large.
        // 0.375 * 16 = 6.0 model units.
        float itemScale = profile.thirdPersonScale() * 16.0f;
        itemMatrix.scale(itemScale);

        if (isBlock) {
            // Block Rendering
            // Rotate so the player holds the corner/edge of the block
            itemMatrix.rotateX((float) Math.toRadians(profile.thirdPersonRotX()));
            itemMatrix.rotateY((float) Math.toRadians(profile.thirdPersonRotY()));
            itemMatrix.rotateZ((float) Math.toRadians(profile.thirdPersonRotZ()));

            // Translate so the hand (origin) is at the side of the block, not center
            // Removed X=0.5f offset to put center in palm as requested.
            itemMatrix.translate(profile.thirdPersonOffsetX(), profile.thirdPersonOffsetY(),
                    profile.thirdPersonOffsetZ());

            if (atlas != null) {
                atlas.bind(0);
                shader.setUniform("alphaCutoff", 0.0f);
                shader.setUniform("modelMatrix", itemMatrix);
                renderHeldMesh(getHeldBlockMesh(type.getPlacedBlock()));
            }
        } else {
            // Item Rendering (Tools, Sticks)
            // Rotate to look like a tool held in hand
            itemMatrix.rotateX((float) Math.toRadians(profile.thirdPersonRotX()));
            itemMatrix.rotateY((float) Math.toRadians(profile.thirdPersonRotY()));
            itemMatrix.rotateZ((float) Math.toRadians(profile.thirdPersonRotZ()));

            // Translate to hold by the handle (bottom-right of texture)
            // Adjusted X to 0.0 to match centered grip logic
            itemMatrix.translate(profile.thirdPersonOffsetX(), profile.thirdPersonOffsetY(),
                    profile.thirdPersonOffsetZ());

            Texture texToUse = ItemTextureResolver.usesItemsAtlas(type) && itemsTexture != null ? itemsTexture : atlas;
            if (texToUse != null) {
                texToUse.bind(0);
                shader.setUniform("alphaCutoff", 0.1f);
                shader.setUniform("modelMatrix", itemMatrix);
                renderHeldMesh(getHeldItemMesh(type));
                shader.setUniform("alphaCutoff", 0.0f);
            }
        }
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
    }

    private void deleteHeldMesh(HeldMesh mesh) {
        glDeleteVertexArrays(mesh.vao());
        glDeleteBuffers(mesh.vbo());
        if (mesh.ebo() != 0) {
            glDeleteBuffers(mesh.ebo());
        }
    }
}
