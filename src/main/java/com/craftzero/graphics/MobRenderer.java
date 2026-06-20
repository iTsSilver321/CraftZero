package com.craftzero.graphics;

import com.craftzero.entity.Entity;
import com.craftzero.entity.mob.*;
import com.craftzero.graphics.model.*; // Import all models
import com.craftzero.world.BlockType;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

public class MobRenderer {
    private static final float MOB_RENDER_DISTANCE = 160.0f;

    private final Renderer renderer;
    private final ShaderProgram shader;

    // Cached models
    private HumanoidModel humanoidModel;
    private SkeletonModel skeletonModel;
    private PigModel pigModel;
    private CowModel cowModel; // CHANGED: Use specific class
    private SheepModel sheepModel; // CHANGED: Use specific class
    private SheepFurModel sheepFurModel; // Wool layer for sheep
    private CreeperModel creeperModel;
    private SpiderModel spiderModel;
    private ChickenModel chickenModel;
    private SlimeModel slimeModel;
    private SquidModel squidModel;
    private SilverfishModel silverfishModel;
    private GhastModel ghastModel;
    private BlazeModel blazeModel;
    private DragonModel dragonModel;
    private Mesh fireOverlayMesh;

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

        skeletonModel = new SkeletonModel();
        skeletonModel.buildMeshes();

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

        creeperModel = new CreeperModel();
        creeperModel.buildMeshes();

        spiderModel = new SpiderModel();
        spiderModel.buildMeshes();

        chickenModel = new ChickenModel();
        chickenModel.buildMeshes();

        slimeModel = new SlimeModel();
        slimeModel.buildMeshes();

        squidModel = new SquidModel();
        squidModel.buildMeshes();

        silverfishModel = new SilverfishModel();
        silverfishModel.buildMeshes();

        ghastModel = new GhastModel();
        ghastModel.buildMeshes();

        blazeModel = new BlazeModel();
        blazeModel.buildMeshes();

        dragonModel = new DragonModel();
        dragonModel.buildMeshes();

        fireOverlayMesh = createFireOverlayMesh();

        System.out.println("MobRenderer initialized");
    }

    public void render(Entity entity, Camera camera, float partialTick, Texture terrainAtlas) {
        if (!(entity instanceof Mob mob))
            return;

        float renderX = entity.getRenderX(partialTick);
        float renderY = entity.getRenderY(partialTick);
        float renderZ = entity.getRenderZ(partialTick);

        float renderBodyYaw = lerp(mob.getPrevBodyYaw(), mob.getBodyYaw(), partialTick);

        Texture texture = MobTexture.get(mob.getTexturePath());
        if (texture == null)
            return;

        // Calculate hurt flash intensity (1.0 = just hit, fades to 0)
        float hurtFlash = 0.0f;
        if (mob.getHurtTime() > 0) {
            hurtFlash = (float) mob.getHurtTime() / 10.0f; // 10 tick hurt duration
        }
        shader.setUniform("hurtFlash", hurtFlash);

        float limbSwing = mob.getLimbSwing();
        float limbSwingAmount = lerp(mob.getPrevLimbSwingAmount(), mob.getLimbSwingAmount(), partialTick);
        float ageInTicks = entity.getTicksExisted() + partialTick;

        float headYaw = mob.getHeadYaw();

        switch (mob.getModelType()) {
            case HUMANOID:
                renderHumanoid(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, ageInTicks, partialTick, headYaw);
                break;
            case CREEPER:
                renderCreeper(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, partialTick, headYaw);
                break;
            case SKELETON:
                renderSkeleton(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, ageInTicks, partialTick, headYaw);
                break;
            case QUADRUPED:
                renderQuadruped(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, ageInTicks, headYaw);
                break;
            case SPIDER:
                renderSpider(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, ageInTicks, partialTick, headYaw);
                break;
            case CHICKEN:
                renderChicken(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, ageInTicks, partialTick, headYaw);
                break;
            case SLIME:
                renderSlime(mob, texture, renderX, renderY, renderZ, renderBodyYaw, ageInTicks);
                break;
            case SQUID:
                renderSquid(mob, texture, renderX, renderY, renderZ, renderBodyYaw, ageInTicks, partialTick);
                break;
            case ENDERMAN:
                renderEnderman(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, ageInTicks, partialTick, headYaw);
                break;
            case SILVERFISH:
                renderSilverfish(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount);
                break;
            case GHAST:
                renderGhast(mob, texture, renderX, renderY, renderZ, renderBodyYaw, ageInTicks);
                break;
            case BLAZE:
                renderBlaze(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        ageInTicks, partialTick, headYaw);
                break;
            case DRAGON:
                renderDragon(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        ageInTicks, partialTick, headYaw);
                break;
        }
        if (mob.isOnFire() && terrainAtlas != null) {
            renderFireOverlay(mob, terrainAtlas, renderX, renderY, renderZ, renderBodyYaw);
        }
    }

    private void renderSpider(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float limbSwing, float limbSwingAmount, float ageInTicks,
            float partialTick, float headYaw) {
        float headPitch = mob.getRenderPitch(partialTick);
        spiderModel.animate(limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch);

        float deathRotation = 0;
        if (mob.isDead()) {
            deathRotation = Math.min(mob.getDeathTime() * 0.16f, (float) Math.PI);
        }

        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.rotateZ(deathRotation);
        modelMatrix.scale(MODEL_SCALE);

        spiderModel.root.calculateTransform(modelMatrix);

        texture.bind(0);
        renderModelPart(spiderModel.root);
        texture.unbind();
    }

    private void renderSlime(Mob mob, Texture texture, float x, float y, float z, float bodyYaw, float ageInTicks) {
        slimeModel.animate(ageInTicks);
        float scale = 1.0f;
        if (mob instanceof Slime slime) {
            scale = 0.6f * slime.getSize();
        }
        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.scale(MODEL_SCALE * scale);
        slimeModel.root.calculateTransform(modelMatrix);
        texture.bind(0);
        renderModelPart(slimeModel.root);
        texture.unbind();
    }

    private void renderSquid(Mob mob, Texture texture, float x, float y, float z, float bodyYaw, float ageInTicks,
            float partialTick) {
        float squidYaw = bodyYaw;
        float squidPitch = mob.getRenderPitch(partialTick);
        float tentacleAngle = (float) Math.sin(ageInTicks * 0.18f) * 0.45f + 0.35f;
        if (mob instanceof Squid squid) {
            squidYaw = squid.getRenderSquidYaw(partialTick);
            squidPitch = squid.getRenderSquidPitch(partialTick);
            tentacleAngle = squid.getRenderTentacleAngle(partialTick);
        }
        squidModel.animate(ageInTicks, tentacleAngle);
        modelMatrix.identity();
        modelMatrix.translate(x, y + 0.25f, z);
        modelMatrix.rotateY((float) Math.toRadians(-squidYaw));
        modelMatrix.rotateX((float) Math.toRadians(squidPitch));
        modelMatrix.scale(MODEL_SCALE);
        squidModel.root.calculateTransform(modelMatrix);
        texture.bind(0);
        renderModelPart(squidModel.root);
        texture.unbind();
    }

    private void renderEnderman(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float limbSwing, float limbSwingAmount, float ageInTicks,
            float partialTick, float headYaw) {
        float headPitch = mob.getRenderPitch(partialTick);
        humanoidModel.animate(limbSwing, limbSwingAmount * 0.45f, ageInTicks, headYaw, headPitch);
        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.scale(MODEL_SCALE * 0.72f, MODEL_SCALE * 1.45f, MODEL_SCALE * 0.72f);
        humanoidModel.root.calculateTransform(modelMatrix);
        texture.bind(0);
        renderModelPart(humanoidModel.root);
        texture.unbind();
    }

    private void renderSilverfish(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw, float limbSwing, float limbSwingAmount) {
        silverfishModel.animate(limbSwing, limbSwingAmount);
        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.scale(MODEL_SCALE * 0.7f);
        silverfishModel.root.calculateTransform(modelMatrix);
        texture.bind(0);
        renderModelPart(silverfishModel.root);
        texture.unbind();
    }

    private void renderGhast(Mob mob, Texture texture, float x, float y, float z, float bodyYaw, float ageInTicks) {
        ghastModel.animate(ageInTicks);
        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.scale(MODEL_SCALE * 2.0f);
        ghastModel.root.calculateTransform(modelMatrix);
        texture.bind(0);
        renderModelPart(ghastModel.root);
        texture.unbind();
    }

    private void renderBlaze(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float ageInTicks, float partialTick, float headYaw) {
        blazeModel.animate(ageInTicks, headYaw, mob.getRenderPitch(partialTick));
        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.scale(MODEL_SCALE);
        blazeModel.root.calculateTransform(modelMatrix);
        texture.bind(0);
        renderModelPart(blazeModel.root);
        texture.unbind();
    }

    private void renderDragon(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float ageInTicks, float partialTick, float headYaw) {
        dragonModel.animate(ageInTicks, headYaw, mob.getRenderPitch(partialTick));
        modelMatrix.identity();
        modelMatrix.translate(x, y + 1.5f, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.scale(MODEL_SCALE * 1.25f);
        dragonModel.root.calculateTransform(modelMatrix);
        texture.bind(0);
        renderModelPart(dragonModel.root);
        texture.unbind();
    }

    private void renderCreeper(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float limbSwing, float limbSwingAmount, float partialTick, float headYaw) {
        float headPitch = mob.getRenderPitch(partialTick);
        creeperModel.animate(limbSwing, limbSwingAmount, headYaw, headPitch);

        float deathRotation = 0;
        if (mob.isDead()) {
            deathRotation = Math.min(mob.getDeathTime() * 0.1f, 1.5f);
        }

        float scale = 1.0f;
        if (mob instanceof Creeper creeper && creeper.isIgnited()) {
            float fuse = creeper.getFuseProgress();
            scale = 1.0f + fuse * 0.2f;
        }

        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.rotateZ(deathRotation);
        modelMatrix.scale(MODEL_SCALE * scale);

        creeperModel.root.calculateTransform(modelMatrix);

        texture.bind(0);
        renderModelPart(creeperModel.root);
        texture.unbind();
    }

    private void renderChicken(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float limbSwing, float limbSwingAmount, float ageInTicks,
            float partialTick, float headYaw) {
        float headPitch = mob.getRenderPitch(partialTick);
        chickenModel.animate(limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch, !mob.isOnGround());

        float deathRotation = 0;
        if (mob.isDead()) {
            deathRotation = Math.min(mob.getDeathTime() * 0.1f, 1.5f);
        }

        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.rotateZ(deathRotation);
        modelMatrix.scale(MODEL_SCALE * 0.75f);

        chickenModel.root.calculateTransform(modelMatrix);

        texture.bind(0);
        renderModelPart(chickenModel.root);
        texture.unbind();
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

    private void renderSkeleton(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float limbSwing, float limbSwingAmount, float ageInTicks,
            float partialTick, float headYaw) {
        float headPitch = mob.getRenderPitch(partialTick);
        skeletonModel.animate(limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch);

        float deathRotation = 0;
        if (mob.isDead()) {
            deathRotation = Math.min(mob.getDeathTime() * 0.1f, 1.5f);
        }

        // Set up transforms
        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.rotateZ(deathRotation);
        modelMatrix.scale(MODEL_SCALE);

        skeletonModel.root.calculateTransform(modelMatrix);

        texture.bind(0);
        renderModelPart(skeletonModel.root);
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

        // Death animation - fall over like humanoids
        float deathRotation = 0;
        if (mob.isDead()) {
            deathRotation = Math.min(mob.getDeathTime() * 0.1f, 1.5f);
        }

        modelMatrix.identity();
        modelMatrix.translate(x, y + yOffset, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.rotateZ(deathRotation);
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

    public void renderAll(java.util.List<Entity> entities, Camera camera, float partialTick, Texture terrainAtlas) {
        if (entities.isEmpty())
            return;

        glDisable(GL_CULL_FACE);

        for (Entity entity : entities) {
            if (isTooFar(camera, entity, MOB_RENDER_DISTANCE)) {
                continue;
            }
            render(entity, camera, partialTick, terrainAtlas);
        }

        // Reset hurt flash to avoid affecting other rendered objects
        shader.setUniform("hurtFlash", 0.0f);

        glEnable(GL_CULL_FACE);
    }

    private static boolean isTooFar(Camera camera, Entity entity, float distance) {
        float dx = entity.getX() - camera.getPosition().x;
        float dy = entity.getY() - camera.getPosition().y;
        float dz = entity.getZ() - camera.getPosition().z;
        float max = Math.min(camera.getFarPlane(), distance + Math.max(entity.getWidth(), entity.getHeight()));
        return dx * dx + dy * dy + dz * dz > max * max;
    }

    private void renderFireOverlay(Mob mob, Texture terrainAtlas,
            float x, float y, float z, float bodyYaw) {
        if (fireOverlayMesh == null) {
            return;
        }
        terrainAtlas.bind(0);
        shader.setUniform("alphaCutoff", 0.1f);
        shader.setUniform("hurtFlash", 0.0f);
        modelMatrix.identity();
        modelMatrix.translate(x, y + mob.getHeight() * 0.5f, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.scale(Math.max(mob.getWidth() * 1.35f, 0.7f), mob.getHeight() * 1.08f,
                Math.max(mob.getWidth() * 1.35f, 0.7f));
        shader.setUniform("modelMatrix", modelMatrix);
        fireOverlayMesh.render();
        shader.setUniform("alphaCutoff", 0.0f);
        terrainAtlas.unbind();
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
        if (humanoidModel != null)
            humanoidModel.cleanup();
        if (skeletonModel != null)
            skeletonModel.cleanup();
        if (pigModel != null)
            pigModel.cleanup();
        if (cowModel != null)
            cowModel.cleanup();
        if (sheepModel != null)
            sheepModel.cleanup();
        if (sheepFurModel != null)
            sheepFurModel.cleanup();
        if (creeperModel != null)
            creeperModel.cleanup();
        if (spiderModel != null)
            spiderModel.cleanup();
        if (chickenModel != null)
            chickenModel.cleanup();
        if (slimeModel != null)
            slimeModel.cleanup();
        if (squidModel != null)
            squidModel.cleanup();
        if (silverfishModel != null)
            silverfishModel.cleanup();
        if (ghastModel != null)
            ghastModel.cleanup();
        if (blazeModel != null)
            blazeModel.cleanup();
        if (dragonModel != null)
            dragonModel.cleanup();
        if (fireOverlayMesh != null)
            fireOverlayMesh.cleanup();

        MobTexture.cleanup();
    }
}
