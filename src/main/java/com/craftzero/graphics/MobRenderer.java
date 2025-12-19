package com.craftzero.graphics;

import com.craftzero.entity.Entity;
import com.craftzero.entity.mob.*;
import com.craftzero.graphics.model.HumanoidModel;
import com.craftzero.graphics.model.ModelPart;
import com.craftzero.graphics.model.PigModel;
import com.craftzero.graphics.model.QuadrupedModel;
import com.craftzero.graphics.model.SheepModel;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

/**
 * Renders mobs with animated models.
 * Uses render interpolation for smooth 60fps display from 20hz physics.
 */
public class MobRenderer {

    private final Renderer renderer;
    private final ShaderProgram shader;

    // Cached models
    private HumanoidModel humanoidModel;
    private PigModel pigModel;
    private QuadrupedModel cowModel;
    private SheepModel sheepModel;

    // Model matrix
    private final Matrix4f modelMatrix;
    private final Matrix4f tempMatrix;

    // Scale factor (Minecraft model units are 1/16th of a block)
    private static final float MODEL_SCALE = 1.0f / 16.0f;

    public MobRenderer(Renderer renderer) {
        this.renderer = renderer;
        this.shader = renderer.getShaderProgram();
        this.modelMatrix = new Matrix4f();
        this.tempMatrix = new Matrix4f();
    }

    /**
     * Initialize models and textures.
     */
    public void init() {
        // Preload textures
        MobTexture.preload();

        // Create models
        humanoidModel = new HumanoidModel();
        humanoidModel.buildMeshes();

        pigModel = PigModel.create();
        pigModel.buildMeshes();

        cowModel = QuadrupedModel.createCow();
        cowModel.buildMeshes();

        sheepModel = SheepModel.create();
        sheepModel.buildMeshes();

        System.out.println("MobRenderer initialized");
    }

    /**
     * Render a mob entity.
     * 
     * @param entity      The mob to render
     * @param camera      Current camera
     * @param partialTick Interpolation factor (0-1)
     */
    public void render(Entity entity, Camera camera, float partialTick) {
        if (!(entity instanceof Mob mob))
            return;

        // Get interpolated position for smooth rendering
        float renderX = entity.getRenderX(partialTick);
        float renderY = entity.getRenderY(partialTick);
        float renderZ = entity.getRenderZ(partialTick);

        // Use BODY YAW for body rotation (not entity yaw)
        // This makes the body face movement direction while head can look around
        float renderBodyYaw = lerp(mob.getPrevBodyYaw(), mob.getBodyYaw(), partialTick);

        // Get texture
        Texture texture = MobTexture.get(mob.getTexturePath());
        if (texture == null)
            return;

        // Get animation parameters from LivingEntity (Minecraft-style)
        // limbSwing = cycle position, limbSwingAmount = amplitude based on speed
        float limbSwing = mob.getLimbSwing();
        float limbSwingAmount = lerp(mob.getPrevLimbSwingAmount(), mob.getLimbSwingAmount(), partialTick);
        float ageInTicks = entity.getTicksExisted() + partialTick;

        // Head rotation relative to body
        float headYaw = mob.getHeadYaw();

        // Select and animate model
        switch (mob.getModelType()) {
            case HUMANOID:
            case CREEPER:
                renderHumanoid(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, ageInTicks, partialTick, headYaw);
                break;
            case QUADRUPED:
                renderQuadruped(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, ageInTicks, headYaw);
                break;
            case SPIDER:
                // TODO: Spider model
                renderHumanoid(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, ageInTicks, partialTick, headYaw);
                break;
            case CHICKEN:
                // TODO: Chicken model
                renderQuadruped(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, ageInTicks, headYaw);
                break;
        }
    }

    /**
     * Linear interpolation for smooth animation.
     */
    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private void renderHumanoid(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float limbSwing, float limbSwingAmount, float ageInTicks,
            float partialTick, float headYaw) {
        // Animate with head independent of body
        float headPitch = mob.getRenderPitch(partialTick);
        humanoidModel.animate(limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch);

        // Death animation (fall over)
        float deathRotation = 0;
        if (mob.isDead()) {
            deathRotation = Math.min(mob.getDeathTime() * 0.1f, 1.5f);
        }

        // Creeper swelling
        float scale = 1.0f;
        if (mob instanceof Creeper creeper && creeper.isIgnited()) {
            float fuse = creeper.getFuseProgress();
            scale = 1.0f + fuse * 0.2f; // Swell up to 1.2x
        }

        // Hurt flash (red tint) - would need shader support
        // For now, we skip this

        // Set up transforms
        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.rotateZ(deathRotation);
        modelMatrix.scale(MODEL_SCALE * scale);

        // Calculate part transforms
        humanoidModel.root.calculateTransform(modelMatrix);

        // Render
        texture.bind(0);
        renderModelPart(humanoidModel.root);
        texture.unbind();
    }

    private void renderQuadruped(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw) {
        // Select model based on mob type
        QuadrupedModel model;
        float yOffset = 0; // Y offset to lift model out of ground

        if (mob instanceof Pig) {
            model = pigModel;
            yOffset = 0; // Pig model Y=0 is at ground
        } else if (mob instanceof Cow) {
            model = cowModel;
            yOffset = 0;
        } else if (mob instanceof Sheep) {
            model = sheepModel;
            yOffset = 0;
        } else {
            model = pigModel;
            yOffset = 0;
        }

        // Animate
        model.animate(limbSwing, limbSwingAmount, ageInTicks);

        // Set up transforms
        // Entity position is at bottom-center (feet level)
        // Model Y=0 is also ground level (after our QuadrupedModel fix)
        modelMatrix.identity();
        modelMatrix.translate(x, y + yOffset, z);

        // Rotate to face direction (yaw is degrees, convert to radians)
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));

        // Apply scale (model units to world units: 1/16)
        modelMatrix.scale(MODEL_SCALE);

        // Calculate part transforms
        model.root.calculateTransform(modelMatrix);

        // Render
        texture.bind(0);
        renderModelPart(model.root);
        texture.unbind();
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

    /**
     * Render all mobs in the world.
     */
    public void renderAll(java.util.List<Entity> entities, Camera camera, float partialTick) {
        if (entities.isEmpty())
            return;

        // Disable backface culling for mobs (some faces might be visible from inside)
        glDisable(GL_CULL_FACE);

        for (Entity entity : entities) {
            render(entity, camera, partialTick);
        }

        glEnable(GL_CULL_FACE);
    }

    /**
     * Cleanup resources.
     */
    public void cleanup() {
        if (humanoidModel != null)
            humanoidModel.cleanup();
        if (pigModel != null)
            pigModel.cleanup();
        if (cowModel != null)
            cowModel.cleanup();
        if (sheepModel != null)
            sheepModel.cleanup();

        MobTexture.cleanup();
    }
}
