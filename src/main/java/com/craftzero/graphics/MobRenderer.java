package com.craftzero.graphics;

import com.craftzero.entity.Entity;
import com.craftzero.entity.mob.*;
import com.craftzero.graphics.model.*; // Import all models
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

public class MobRenderer {

    private final Renderer renderer;
    private final ShaderProgram shader;

    // Cached models
    private HumanoidModel humanoidModel;
    private PigModel pigModel;
    private CowModel cowModel; // CHANGED: Use specific class
    private SheepModel sheepModel; // CHANGED: Use specific class
    private SheepFurModel sheepFurModel; // Wool layer for sheep

    // Model matrix
    private final Matrix4f modelMatrix;
    private final Matrix4f tempMatrix;
    private static final float MODEL_SCALE = 1.0f / 16.0f;

    public MobRenderer(Renderer renderer) {
        this.renderer = renderer;
        this.shader = renderer.getShaderProgram();
        this.modelMatrix = new Matrix4f();
        this.tempMatrix = new Matrix4f();
    }

    public void init() {
        MobTexture.preload();

        humanoidModel = new HumanoidModel();
        humanoidModel.buildMeshes();

        pigModel = PigModel.create();
        pigModel.buildMeshes();

        // --- CRITICAL FIX ---
        // Use 'new CowModel()' to use the class with the fixes.
        // 'QuadrupedModel.createCow()' creates a generic, broken model.
        cowModel = new CowModel();
        cowModel.buildMeshes();

        // Use 'new SheepModel()'
        sheepModel = new SheepModel();
        sheepModel.buildMeshes();

        // Sheep fur (wool) layer
        sheepFurModel = SheepFurModel.create();
        sheepFurModel.buildMeshes();

        System.out.println("MobRenderer initialized");
    }

    public void render(Entity entity, Camera camera, float partialTick) {
        if (!(entity instanceof Mob mob))
            return;

        float renderX = entity.getRenderX(partialTick);
        float renderY = entity.getRenderY(partialTick);
        float renderZ = entity.getRenderZ(partialTick);

        float renderBodyYaw = lerp(mob.getPrevBodyYaw(), mob.getBodyYaw(), partialTick);

        Texture texture = MobTexture.get(mob.getTexturePath());
        if (texture == null)
            return;

        float limbSwing = mob.getLimbSwing();
        float limbSwingAmount = lerp(mob.getPrevLimbSwingAmount(), mob.getLimbSwingAmount(), partialTick);
        float ageInTicks = entity.getTicksExisted() + partialTick;

        float headYaw = mob.getHeadYaw();

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

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private void renderHumanoid(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float limbSwing, float limbSwingAmount, float ageInTicks,
            float partialTick, float headYaw) {
        float headPitch = mob.getRenderPitch(partialTick);
        humanoidModel.animate(limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch);

        float deathRotation = 0;
        if (mob.isDead()) {
            deathRotation = Math.min(mob.getDeathTime() * 0.1f, 1.5f);
        }

        float scale = 1.0f;
        if (mob instanceof Creeper creeper && creeper.isIgnited()) {
            float fuse = creeper.getFuseProgress();
            scale = 1.0f + fuse * 0.2f;
        }

        // Hurt flash (red tint) - would need shader support
        // For now, we skip this

        // Set up transforms
        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.rotateZ(deathRotation);
        modelMatrix.scale(MODEL_SCALE * scale);

        humanoidModel.root.calculateTransform(modelMatrix);

        texture.bind(0);
        renderModelPart(humanoidModel.root);
        texture.unbind();
    }

    private void renderQuadruped(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw) {

        QuadrupedModel model;
        float yOffset = 0;

        if (mob instanceof Pig) {
            model = pigModel;
        } else if (mob instanceof Cow) {
            model = cowModel;
        } else if (mob instanceof Sheep) {
            model = sheepModel;
        } else {
            model = pigModel;
        }

        model.animate(limbSwing, limbSwingAmount, ageInTicks);

        modelMatrix.identity();
        modelMatrix.translate(x, y + yOffset, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.scale(MODEL_SCALE);

        model.root.calculateTransform(modelMatrix);

        texture.bind(0);
        renderModelPart(model.root);
        texture.unbind();

        // Render sheep fur layer if not sheared
        if (mob instanceof Sheep sheep && !sheep.isSheared()) {
            sheepFurModel.animate(limbSwing, limbSwingAmount, ageInTicks);
            sheepFurModel.root.calculateTransform(modelMatrix);

            Texture furTexture = MobTexture.get("/textures/mob/sheep_fur.png");
            if (furTexture != null) {
                furTexture.bind(0);
                renderModelPart(sheepFurModel.root);
                furTexture.unbind();
            }
        }
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

    public void renderAll(java.util.List<Entity> entities, Camera camera, float partialTick) {
        if (entities.isEmpty())
            return;

        glDisable(GL_CULL_FACE);

        for (Entity entity : entities) {
            render(entity, camera, partialTick);
        }

        glEnable(GL_CULL_FACE);
    }

    public void cleanup() {
        if (humanoidModel != null)
            humanoidModel.cleanup();
        if (pigModel != null)
            pigModel.cleanup();
        if (cowModel != null)
            cowModel.cleanup();
        if (sheepModel != null)
            sheepModel.cleanup();
        if (sheepFurModel != null)
            sheepFurModel.cleanup();

        MobTexture.cleanup();
    }
}
