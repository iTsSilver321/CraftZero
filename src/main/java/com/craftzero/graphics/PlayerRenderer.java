package com.craftzero.graphics;

import com.craftzero.graphics.model.ModelPart;
import com.craftzero.graphics.model.PlayerModel;
import com.craftzero.main.Player;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renders the player model in third-person view.
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

    public PlayerRenderer(Renderer renderer) {
        this.renderer = renderer;
        this.shader = renderer.getShaderProgram();
        this.modelMatrix = new Matrix4f();
    }

    public void init() {
        playerModel = new PlayerModel();
        playerModel.buildMeshes();
        playerTexture = MobTexture.get("/textures/mob/char.png");
        System.out.println("PlayerRenderer initialized");
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

        float bodyYaw = player.getRenderYawOffset(partialTick);
        if (cameraMode == 2) {
            bodyYaw = camera.getYaw() + 180;
        }

        float headYaw = camera.getYaw() - bodyYaw;
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
            headYaw = 0;
        }

        playerModel.animate(limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch,
                player.getSwingProgress(partialTick), player.isSneaking());

        modelMatrix.identity();
        modelMatrix.translate(renderX, renderY, renderZ);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

        playerModel.root.calculateTransform(modelMatrix);

        glDisable(GL_CULL_FACE);
        playerTexture.bind(0);
        renderModelPart(playerModel.root);
        playerTexture.unbind();
        glEnable(GL_CULL_FACE);
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

        playerTexture.bind(0);
        glClear(GL_DEPTH_BUFFER_BIT);
        glDisable(GL_CULL_FACE);

        float swingProgress = player.getSwingProgress(partialTick);
        float walkDist = player.getPrevDistanceWalked()
                + (player.getDistanceWalked() - player.getPrevDistanceWalked()) * partialTick;

        Vector3f camPos = camera.getPosition();
        float camYaw = camera.getYaw();
        float camPitch = camera.getPitch();

        modelMatrix.identity();
        modelMatrix.translate(camPos.x, camPos.y, camPos.z);
        modelMatrix.rotateY((float) Math.toRadians(-camYaw));
        modelMatrix.rotateX((float) Math.toRadians(-camPitch));

        // Refined position: Pushing further down and right to take 20% less space while
        // remaining bulky
        modelMatrix.translate(1.05f, -1.25f, -1.0f);

        // Rotation: 90 degrees "straight up" look + 45 degree inward curve
        modelMatrix.rotateY((float) Math.toRadians(180));
        modelMatrix.rotateX((float) Math.toRadians(-125)); // More vertical alignment
        modelMatrix.rotateY((float) Math.toRadians(-45)); // 45-degree inward curve
        modelMatrix.rotateZ((float) Math.toRadians(0)); // Perfectly straight alignment

        // Bobbing - Smoothed out frequency to Minecraft's standard gait (about 0.6662)
        // This removes the "shake" by making the movement circular and rhythmic rather
        // than high-frequency jitter
        float bobX = (float) Math.sin(walkDist * 0.6662f) * 0.02f;
        float bobY = (float) Math.cos(walkDist * 1.3324f) * 0.015f;
        modelMatrix.translate(bobX, bobY, 0);

        // Swing
        if (swingProgress > 0) {
            float sqrtSwing = (float) Math.sqrt(swingProgress);
            float sinSqrt = (float) Math.sin(sqrtSwing * Math.PI);
            modelMatrix.rotateX((float) Math.toRadians(sinSqrt * 45.0f));
            modelMatrix.rotateY((float) Math.toRadians(-sinSqrt * 20.0f));
            modelMatrix.rotateZ((float) Math.toRadians(sinSqrt * 10.0f));
        }

        modelMatrix.scale(MODEL_SCALE * 1.55f, MODEL_SCALE * 1.55f, MODEL_SCALE * 1.55f);

        float origPX = playerModel.leftArm.getPivotX();
        float origPY = playerModel.leftArm.getPivotY();
        float origPZ = playerModel.leftArm.getPivotZ();

        playerModel.leftArm.setPivot(0, 0, 0);
        playerModel.leftArm.setRotation(0, 0, 0);
        playerModel.leftArm.calculateTransform(modelMatrix);

        renderModelPart(playerModel.leftArm);

        playerModel.leftArm.setPivot(origPX, origPY, origPZ);
        glEnable(GL_CULL_FACE);
    }

    public void cleanup() {
        if (playerModel != null)
            playerModel.cleanup();
    }
}