package com.craftzero.graphics;

import com.craftzero.graphics.model.ModelPart;
import com.craftzero.graphics.model.PlayerModel;
import com.craftzero.inventory.ItemStack;
import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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

    // Held item rendering (simple VAO for a flat quad and cube)
    private int itemVao;
    private int itemVbo;
    private int blockVao;
    private int blockVbo;
    private int blockEbo;

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
        // Build a flat quad for item sprites (tools, sticks, etc.)
        // Vertex format: pos(3) + uv(2) + normal(3) = 8 floats per vertex
        float[] itemVertices = {
                // x, y, z, u, v, nx, ny, nz
                -0.5f, -0.5f, 0, 0, 1, 0, 0, 1,
                0.5f, -0.5f, 0, 1, 1, 0, 0, 1,
                0.5f, 0.5f, 0, 1, 0, 0, 0, 1,
                -0.5f, 0.5f, 0, 0, 0, 0, 0, 1,
        };

        itemVao = glGenVertexArrays();
        itemVbo = glGenBuffers();

        glBindVertexArray(itemVao);
        glBindBuffer(GL_ARRAY_BUFFER, itemVbo);
        glBufferData(GL_ARRAY_BUFFER, itemVertices, GL_STATIC_DRAW);

        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        // Build a small cube for block items
        buildBlockMesh();
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

    private void updateBlockUVs(BlockType type) {
        float s = 0.5f;
        float[] v = new float[24 * 8];

        // Get UVs from block type: top, bottom, side
        float[] topUV = type.getTextureCoords(0);
        float[] bottomUV = type.getTextureCoords(1);
        float[] sideUV = type.getTextureCoords(2);

        int i = 0;
        // Front - side
        i = setVert(v, i, -s, -s, s, sideUV[0], sideUV[3], 0, 0, 1);
        i = setVert(v, i, s, -s, s, sideUV[2], sideUV[3], 0, 0, 1);
        i = setVert(v, i, s, s, s, sideUV[2], sideUV[1], 0, 0, 1);
        i = setVert(v, i, -s, s, s, sideUV[0], sideUV[1], 0, 0, 1);
        // Back - side
        i = setVert(v, i, s, -s, -s, sideUV[0], sideUV[3], 0, 0, -1);
        i = setVert(v, i, -s, -s, -s, sideUV[2], sideUV[3], 0, 0, -1);
        i = setVert(v, i, -s, s, -s, sideUV[2], sideUV[1], 0, 0, -1);
        i = setVert(v, i, s, s, -s, sideUV[0], sideUV[1], 0, 0, -1);
        // Top
        i = setVert(v, i, -s, s, s, topUV[0], topUV[3], 0, 1, 0);
        i = setVert(v, i, s, s, s, topUV[2], topUV[3], 0, 1, 0);
        i = setVert(v, i, s, s, -s, topUV[2], topUV[1], 0, 1, 0);
        i = setVert(v, i, -s, s, -s, topUV[0], topUV[1], 0, 1, 0);
        // Bottom
        i = setVert(v, i, -s, -s, -s, bottomUV[0], bottomUV[3], 0, -1, 0);
        i = setVert(v, i, s, -s, -s, bottomUV[2], bottomUV[3], 0, -1, 0);
        i = setVert(v, i, s, -s, s, bottomUV[2], bottomUV[1], 0, -1, 0);
        i = setVert(v, i, -s, -s, s, bottomUV[0], bottomUV[1], 0, -1, 0);
        // Right - side
        i = setVert(v, i, s, -s, s, sideUV[0], sideUV[3], 1, 0, 0);
        i = setVert(v, i, s, -s, -s, sideUV[2], sideUV[3], 1, 0, 0);
        i = setVert(v, i, s, s, -s, sideUV[2], sideUV[1], 1, 0, 0);
        i = setVert(v, i, s, s, s, sideUV[0], sideUV[1], 1, 0, 0);
        // Left - side
        i = setVert(v, i, -s, -s, -s, sideUV[0], sideUV[3], -1, 0, 0);
        i = setVert(v, i, -s, -s, s, sideUV[2], sideUV[3], -1, 0, 0);
        i = setVert(v, i, -s, s, s, sideUV[2], sideUV[1], -1, 0, 0);
        setVert(v, i, -s, s, -s, sideUV[0], sideUV[1], -1, 0, 0);

        glBindBuffer(GL_ARRAY_BUFFER, blockVbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, v);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
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

        modelMatrix.identity();
        modelMatrix.translate(renderX, renderY, renderZ);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

        playerModel.root.calculateTransform(modelMatrix);

        glDisable(GL_CULL_FACE);
        playerTexture.bind(0);
        renderModelPart(playerModel.root);
        playerTexture.unbind();

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
        BlockType heldType = holdingItem ? heldItem.getType() : null;
        boolean holdingBlock = holdingItem && heldType != null && !heldType.isItem();
        boolean holdingTool = holdingItem && heldType != null && heldType.isItem();

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

        // Render arm ONLY when not holding a block (blocks replace the hand)
        // Tools still show with the arm
        if (!holdingBlock) {
            playerTexture.bind(0);
            renderModelPart(playerModel.leftArm);
        }

        // Render held item (block or tool)
        if (holdingItem && (atlas != null || itemsTexture != null)) {
            renderHeldItemFirstPerson(heldType, holdingBlock, holdingTool, camPos, camera,
                    swingProgress, useProgress, walkDist, entityBrightness, switchOffset);
        }

        playerModel.leftArm.setPivot(origPX, origPY, origPZ);
        glEnable(GL_CULL_FACE);
        renderer.setEntityBrightness(0.0f);
    }

    /**
     * Render held item in first person view.
     */
    private void renderHeldItemFirstPerson(BlockType type, boolean isBlock, boolean isTool,
            Vector3f camPos, Camera camera, float swingProgress, float useProgress,
            float walkDist, float brightness, float slotSwitchOffset) {

        // Set up item model matrix
        Matrix4f itemMatrix = new Matrix4f();
        itemMatrix.identity();
        itemMatrix.translate(camPos.x, camPos.y, camPos.z);
        itemMatrix.rotateY((float) Math.toRadians(-camera.getYaw()));
        itemMatrix.rotateX((float) Math.toRadians(-camera.getPitch()));

        // Position item in front of hand (screen space: right/bottom/forward)
        // Adjusted to match Minecraft positioning - more right, lower, further forward
        float itemX = 0.70f; // More to the right
        float itemY = -0.65f - slotSwitchOffset * 1.5f; // Lower on screen
        float itemZ = -0.95f; // Further forward (away from camera)
        itemMatrix.translate(itemX, itemY, itemZ);

        // Apply swing animation (swings forward, left, and down like Minecraft)
        if (swingProgress > 0) {
            float phase = swingProgress;
            float forwardExtend = (phase < 0.4f) ? (phase / 0.4f) : Math.max(0, 1.0f - (phase - 0.4f) / 0.6f);
            forwardExtend = (float) Math.sin(forwardExtend * Math.PI * 0.5f);
            // Move left (-X), UP (+Y toward crosshair), and forward (-Z)
            itemMatrix.translate(-forwardExtend * 0.3f, forwardExtend * 0.15f, -forwardExtend * 0.2f);

            float downSwing = (phase > 0.3f) ? (float) Math.sin((phase - 0.3f) / 0.7f * Math.PI) : 0;
            // Add downward motion in second half of swing
            itemMatrix.translate(0, -downSwing * 0.25f, 0);
            // Tilt forward (-X rotation) and left (+Z rotation)
            itemMatrix.rotateX((float) Math.toRadians(-downSwing * 80.0f));
            itemMatrix.rotateZ((float) Math.toRadians(downSwing * 20.0f));
        }

        // Apply use animation (same as swing - forward, left, and down)
        if (useProgress > 0) {
            float phase = useProgress;
            float forwardExtend = (phase < 0.4f) ? (phase / 0.4f) : Math.max(0, 1.0f - (phase - 0.4f) / 0.6f);
            forwardExtend = (float) Math.sin(forwardExtend * Math.PI * 0.5f);
            // Same motion as swing - up, left, forward toward crosshair
            itemMatrix.translate(-forwardExtend * 0.3f, forwardExtend * 0.15f, -forwardExtend * 0.2f);

            float downSwing = (phase > 0.3f) ? (float) Math.sin((phase - 0.3f) / 0.7f * Math.PI) : 0;
            // Add downward motion in second half of swing
            itemMatrix.translate(0, -downSwing * 0.25f, 0);
            // Same tilt as swing
            itemMatrix.rotateX((float) Math.toRadians(-downSwing * 80.0f));
            itemMatrix.rotateZ((float) Math.toRadians(downSwing * 20.0f));
        }

        // Bobbing
        float bobX = (float) Math.sin(walkDist * 0.6662f) * 0.015f;
        float bobY = (float) Math.cos(walkDist * 1.3324f) * 0.01f;
        itemMatrix.translate(bobX, bobY, 0);

        // Scale and orient the item
        if (isBlock) {
            // Block: render as 3D cube (replaces hand entirely)
            // Scale and orientation adjusted to match Minecraft first-person view
            itemMatrix.scale(0.46f); // Size
            itemMatrix.rotateY((float) Math.toRadians(42)); // Diamond orientation (corner in front)
            itemMatrix.rotateX((float) Math.toRadians(18)); // Forward tilt
            itemMatrix.rotateZ((float) Math.toRadians(10)); // More left tilt for visible top

            if (atlas != null) {
                // Update UVs for this specific block type
                updateBlockUVs(type);

                atlas.bind(0);
                shader.setUniform("modelMatrix", itemMatrix);

                glBindVertexArray(blockVao);
                glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);
                glBindVertexArray(0);
            }
        } else if (isTool) {
            // Tool: render as flat 2D sprite angled
            itemMatrix.scale(0.4f); // Slightly smaller as per user request
            itemMatrix.rotateZ((float) Math.toRadians(-45)); // Diagonal angle like Minecraft
            itemMatrix.rotateY((float) Math.toRadians(15)); // Slight turn toward camera

            Texture texToUse = null;
            if (type.usesItemTexture() && itemsTexture != null) {
                texToUse = itemsTexture;
            } else if (atlas != null) {
                texToUse = atlas;
            }

            if (texToUse != null) {
                texToUse.bind(0);
                shader.setUniform("modelMatrix", itemMatrix);

                glBindVertexArray(itemVao);
                glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
                glBindVertexArray(0);
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

        BlockType type = heldItem.getType();
        boolean isBlock = !type.isItem();

        // Get the arm's world transform and position item at the "hand" end
        // NOTE: "leftArm" in PlayerModel is the VISUAL RIGHT ARM (pivot at X=6).
        Matrix4f armTransform = playerModel.leftArm.getWorldTransform();
        Matrix4f itemMatrix = new Matrix4f(armTransform);

        // Move to hand position (end of arm in model space)
        // Arm extends down from pivot (length is 12 units).
        // We translate in MODEL UNITS (pixels).
        // Hand is at Y = -10 (relative to pivot).
        // Refined 2: Slightly more front (Z=-3.0) and tiny bit left (X=-0.6).
        itemMatrix.translate(-0.6f, -11.5f, -3.0f);

        // Scale correction!
        // The armTransform includes 1/16 scale. Item needs to be ~0.375 blocks large.
        // 0.375 * 16 = 6.0 model units.
        float itemScale = 0.375f * 16.0f;
        itemMatrix.scale(itemScale);

        if (isBlock) {
            // Block Rendering
            // Rotate so the player holds the corner/edge of the block
            itemMatrix.rotateY((float) Math.toRadians(-45));

            // Translate so the hand (origin) is at the side of the block, not center
            // Removed X=0.5f offset to put center in palm as requested.
            itemMatrix.translate(0.0f, 0.2f, 0.0f);

            if (atlas != null) {
                updateBlockUVs(type);
                atlas.bind(0);
                shader.setUniform("modelMatrix", itemMatrix);

                glBindVertexArray(blockVao);
                glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);
                glBindVertexArray(0);
            }
        } else {
            // Item Rendering (Tools, Sticks)
            // Rotate to look like a tool held in hand
            itemMatrix.rotateX((float) Math.toRadians(180)); // Flip upside down (origin is top-left usually)
            itemMatrix.rotateZ((float) Math.toRadians(45)); // Angle it forward

            // Translate to hold by the handle (bottom-right of texture)
            // Adjusted X to 0.0 to match centered grip logic
            itemMatrix.translate(0.0f, -0.5f, 0.0f);

            Texture texToUse = type.usesItemTexture() && itemsTexture != null ? itemsTexture : atlas;
            if (texToUse != null) {
                texToUse.bind(0);
                shader.setUniform("modelMatrix", itemMatrix);

                glBindVertexArray(itemVao);
                glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
                glBindVertexArray(0);
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
    }
}