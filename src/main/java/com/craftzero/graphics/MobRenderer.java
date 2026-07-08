package com.craftzero.graphics;

import com.craftzero.entity.Entity;
import com.craftzero.entity.mob.*;
import com.craftzero.graphics.model.*; // Import all models
import com.craftzero.world.Block;
import com.craftzero.world.BlockShape;
import com.craftzero.world.BlockState;
import com.craftzero.world.BlockType;
import com.craftzero.world.VoxelShape;
import com.craftzero.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class MobRenderer {
    private static final float MOB_RENDER_DISTANCE = 160.0f;

    private final Renderer renderer;
    private final ShaderProgram shader;

    // Cached models
    private HumanoidModel humanoidModel;
    private SkeletonModel skeletonModel;
    private PigModel pigModel;
    private PigModel pigSaddleModel;
    private CowModel cowModel; // CHANGED: Use specific class
    private SheepModel sheepModel; // CHANGED: Use specific class
    private SheepFurModel sheepFurModel; // Wool layer for sheep
    private WolfModel wolfModel;
    private CreeperModel creeperModel;
    private SpiderModel spiderModel;
    private ChickenModel chickenModel;
    private SlimeModel slimeModel;
    private SquidModel squidModel;
    private SilverfishModel silverfishModel;
    private GhastModel ghastModel;
    private BlazeModel blazeModel;
    private SnowGolemModel snowGolemModel;
    private VillagerModel villagerModel;
    private DragonModel dragonModel;
    private Mesh fireOverlayMesh;
    private final Map<CarriedBlockMeshKey, Mesh> carriedBlockMeshes = new HashMap<>();
    private static final String CHARGED_CREEPER_TEXTURE = "/textures/armor/power.png";
    private static final String PIG_SADDLE_TEXTURE = "/textures/mob/saddle.png";
    private static final String SPIDER_EYES_TEXTURE = "/textures/mob/spider_eyes.png";
    private static final Vector3f HURT_FLASH_COLOR = new Vector3f(1.0f, 0.4f, 0.4f);
    private static final Vector3f CREEPER_FUSE_FLASH_COLOR = new Vector3f(1.0f, 1.0f, 1.0f);

    // Model matrix
    private final Matrix4f modelMatrix;
    private final Matrix4f tempMatrix;
    private static final float MODEL_SCALE = 1.0f / 16.0f;
    private float renderScaleMultiplier = 1.0f;

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

        pigSaddleModel = PigModel.createSaddleOverlay();
        pigSaddleModel.buildMeshes();

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

        wolfModel = new WolfModel();
        wolfModel.buildMeshes();

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

        snowGolemModel = new SnowGolemModel();
        snowGolemModel.buildMeshes();

        villagerModel = new VillagerModel();
        villagerModel.buildMeshes();

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
        renderer.setEntityBrightness(computeEntityBrightness(mob));

        // Calculate hurt flash intensity (1.0 = just hit, fades to 0)
        float hurtFlash = 0.0f;
        if (mob.getHurtTime() > 0) {
            hurtFlash = (float) mob.getHurtTime() / 10.0f; // 10 tick hurt duration
        }
        shader.setUniform("hurtFlashColor", HURT_FLASH_COLOR);
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
                        limbSwing, limbSwingAmount, partialTick, headYaw, hurtFlash);
                break;
            case SKELETON:
                renderSkeleton(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, ageInTicks, partialTick, headYaw);
                break;
            case QUADRUPED:
                renderQuadruped(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, ageInTicks, partialTick, headYaw);
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
                renderSlime(mob, texture, renderX, renderY, renderZ, renderBodyYaw, partialTick);
                break;
            case SQUID:
                renderSquid(mob, texture, renderX, renderY, renderZ, renderBodyYaw, ageInTicks, partialTick);
                break;
            case ENDERMAN:
                renderEnderman(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, ageInTicks, partialTick, headYaw, terrainAtlas);
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
            case WOLF:
                renderWolf(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, ageInTicks, partialTick, headYaw);
                break;
            case SNOW_GOLEM:
                renderSnowGolem(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, ageInTicks, partialTick, headYaw);
                break;
            case VILLAGER:
                renderVillager(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        limbSwing, limbSwingAmount, partialTick, headYaw);
                break;
            case DRAGON:
                renderDragon(mob, texture, renderX, renderY, renderZ, renderBodyYaw,
                        ageInTicks, partialTick, headYaw);
                break;
        }
        if (mob.isOnFire() && terrainAtlas != null) {
            renderFireOverlay(mob, terrainAtlas, renderX, renderY, renderZ, renderBodyYaw);
        }
        shader.setUniform("hurtFlash", 0.0f);
        shader.setUniform("hurtFlashColor", HURT_FLASH_COLOR);
        renderer.setEntityBrightness(0.0f);
    }

    public void renderScaled(Mob mob, Camera camera, float partialTick, Texture terrainAtlas, float scale) {
        if (mob == null || scale <= 0.0f) {
            return;
        }
        float previousScale = renderScaleMultiplier;
        renderScaleMultiplier = scale;
        try {
            render(mob, camera, partialTick, terrainAtlas);
        } finally {
            renderScaleMultiplier = previousScale;
        }
    }

    private float computeEntityBrightness(Mob mob) {
        World world = mob.getWorld();
        if (world == null) {
            return 1.0f;
        }
        int x = (int) Math.floor(mob.getX());
        int y = (int) Math.floor(mob.getY() + mob.getHeight() * 0.85f);
        int z = (int) Math.floor(mob.getZ());
        int sky = world.getSkyLight(x, y, z);
        if (world.getDayCycleManager() != null) {
            sky = Math.round(sky * world.getDayCycleManager().getSunBrightness());
        }
        int block = world.getBlockLightIfLoaded(x, y, z, 0);
        int lightLevel = Math.max(block, sky);
        float f = Math.max(0, Math.min(15, lightLevel)) / 15.0f;
        return Math.max(0.08f, f / (3.0f - 2.0f * f));
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
        modelMatrix.scale(MODEL_SCALE * renderScaleMultiplier);

        spiderModel.root.calculateTransform(modelMatrix);

        texture.bind(0);
        renderModelPart(spiderModel.root);
        texture.unbind();

        SpiderEyeLayer eyeLayer = spiderEyeLayer(mob);
        if (eyeLayer.visible()) {
            renderSpiderEyeLayer(mob, eyeLayer);
        }
    }

    private void renderSpiderEyeLayer(Mob mob, SpiderEyeLayer eyeLayer) {
        Texture eyeTexture = MobTexture.get(eyeLayer.texturePath());
        if (eyeTexture == null) {
            return;
        }
        float previousSunBrightness = renderer.getSunBrightness();
        shader.setUniform("hurtFlash", 0.0f);
        shader.setUniform("alphaCutoff", eyeLayer.alphaCutoff());
        renderer.setEntityBrightness(eyeLayer.brightness());
        renderer.setSunBrightness(1.0f);
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE);
        glDepthFunc(GL_LEQUAL);
        eyeTexture.bind(0);
        renderModelPart(spiderModel.root);
        eyeTexture.unbind();
        glDepthFunc(GL_LESS);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        renderer.setSunBrightness(previousSunBrightness);
        renderer.setEntityBrightness(computeEntityBrightness(mob));
        shader.setUniform("alphaCutoff", 0.0f);
    }

    private void renderSlime(Mob mob, Texture texture, float x, float y, float z, float bodyYaw, float partialTick) {
        float squishAmount = 0.0f;
        float scale = 1.0f;
        int size = 1;
        if (mob instanceof Slime slime) {
            size = slime.getSize();
            squishAmount = slime.getRenderSquishAmount(partialTick);
            scale = 0.6f * size;
        }
        slimeModel.animate(squishAmount, size);
        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.scale(MODEL_SCALE * scale * renderScaleMultiplier);
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
        modelMatrix.scale(MODEL_SCALE * renderScaleMultiplier);
        squidModel.root.calculateTransform(modelMatrix);
        texture.bind(0);
        renderModelPart(squidModel.root);
        texture.unbind();
    }

    private void renderEnderman(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float limbSwing, float limbSwingAmount, float ageInTicks,
            float partialTick, float headYaw, Texture terrainAtlas) {
        float headPitch = mob.getRenderPitch(partialTick);
        EndermanCarriedBlockTransform carried = mob instanceof Enderman enderman
                ? endermanCarriedBlockTransform(enderman)
                : EndermanCarriedBlockTransform.NONE;
        humanoidModel.animateEnderman(limbSwing, limbSwingAmount * 0.45f, ageInTicks, headYaw, headPitch,
                carried.visible());
        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.scale(MODEL_SCALE * 0.72f * renderScaleMultiplier,
                MODEL_SCALE * 1.45f * renderScaleMultiplier,
                MODEL_SCALE * 0.72f * renderScaleMultiplier);
        humanoidModel.root.calculateTransform(modelMatrix);
        texture.bind(0);
        renderModelPart(humanoidModel.root);
        texture.unbind();

        if (carried.visible() && terrainAtlas != null) {
            renderEndermanCarriedBlock(carried, terrainAtlas, x, y, z, bodyYaw);
        }
    }

    private void renderEndermanCarriedBlock(EndermanCarriedBlockTransform carried, Texture terrainAtlas,
            float x, float y, float z, float bodyYaw) {
        Mesh mesh = carriedBlockMeshes.computeIfAbsent(
                new CarriedBlockMeshKey(carried.block(), carried.metadata()),
                this::createCarriedBlockMesh);
        terrainAtlas.bind(0);
        shader.setUniform("alphaCutoff", 0.1f);
        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.translate(0.0f, carried.centerYOffset() * renderScaleMultiplier,
                carried.forwardOffset() * renderScaleMultiplier);
        modelMatrix.rotateX((float) Math.toRadians(20.0f));
        modelMatrix.rotateY((float) Math.toRadians(45.0f));
        modelMatrix.scale(carried.scale() * renderScaleMultiplier);
        shader.setUniform("modelMatrix", modelMatrix);
        mesh.render();
        shader.setUniform("alphaCutoff", 0.0f);
        terrainAtlas.unbind();
    }

    private void renderSilverfish(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw, float limbSwing, float limbSwingAmount) {
        silverfishModel.animate(limbSwing, limbSwingAmount);
        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.scale(MODEL_SCALE * 0.7f * renderScaleMultiplier);
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
        modelMatrix.scale(MODEL_SCALE * 2.0f * renderScaleMultiplier);
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
        modelMatrix.scale(MODEL_SCALE * renderScaleMultiplier);
        blazeModel.root.calculateTransform(modelMatrix);
        texture.bind(0);
        renderModelPart(blazeModel.root);
        texture.unbind();
    }

    private void renderWolf(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float limbSwing, float limbSwingAmount, float ageInTicks,
            float partialTick, float headYaw) {
        boolean sitting = false;
        boolean begging = false;
        boolean angry = false;
        boolean tamed = false;
        float health = mob.getHealth();
        float headShakeRoll = 0.0f;
        float bodyShakeRoll = 0.0f;
        float tailShakeRoll = 0.0f;
        if (mob instanceof Wolf wolf) {
            sitting = wolf.isSitting();
            begging = wolf.isBegging();
            angry = wolf.isAngry();
            tamed = wolf.isTamed();
            health = wolf.getHealth();
            headShakeRoll = wolf.getShakeAngle(partialTick, -0.08f);
            bodyShakeRoll = wolf.getShakeAngle(partialTick, -0.16f);
            tailShakeRoll = wolf.getShakeAngle(partialTick, -0.20f);
        }
        wolfModel.animate(limbSwing, limbSwingAmount, ageInTicks, headYaw, mob.getRenderPitch(partialTick),
                sitting, begging, angry, tamed, health, headShakeRoll, bodyShakeRoll, tailShakeRoll);

        float deathRotation = 0;
        if (mob.isDead()) {
            deathRotation = Math.min(mob.getDeathTime() * 0.1f, 1.5f);
        }

        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.rotateZ(deathRotation);
        modelMatrix.scale(MODEL_SCALE * mob.getRenderScale() * renderScaleMultiplier);

        wolfModel.root.calculateTransform(modelMatrix);
        texture.bind(0);
        renderModelPart(wolfModel.root);
        texture.unbind();
    }

    private void renderDragon(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float ageInTicks, float partialTick, float headYaw) {
        if (mob instanceof EnderDragon dragon) {
            dragonModel.animate(dragon, ageInTicks, partialTick, headYaw);
        } else {
            dragonModel.animate(ageInTicks, headYaw, mob.getRenderPitch(partialTick));
        }
        modelMatrix.identity();
        modelMatrix.translate(x, y + 1.5f, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.scale(MODEL_SCALE * 1.25f * renderScaleMultiplier);
        dragonModel.root.calculateTransform(modelMatrix);
        texture.bind(0);
        renderModelPart(dragonModel.root);
        texture.unbind();
    }

    private void renderSnowGolem(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float limbSwing, float limbSwingAmount, float ageInTicks,
            float partialTick, float headYaw) {
        snowGolemModel.animate(limbSwing, limbSwingAmount, ageInTicks, headYaw, mob.getRenderPitch(partialTick));

        float deathRotation = 0;
        if (mob.isDead()) {
            deathRotation = Math.min(mob.getDeathTime() * 0.1f, 1.5f);
        }

        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.rotateZ(deathRotation);
        modelMatrix.scale(MODEL_SCALE * renderScaleMultiplier);

        snowGolemModel.root.calculateTransform(modelMatrix);
        texture.bind(0);
        renderModelPart(snowGolemModel.root);
        texture.unbind();
    }

    private void renderVillager(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float limbSwing, float limbSwingAmount,
            float partialTick, float headYaw) {
        villagerModel.animate(limbSwing, limbSwingAmount, headYaw, mob.getRenderPitch(partialTick));

        float deathRotation = 0;
        if (mob.isDead()) {
            deathRotation = Math.min(mob.getDeathTime() * 0.1f, 1.5f);
        }

        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.rotateZ(deathRotation);
        modelMatrix.scale(MODEL_SCALE * mob.getRenderScale() * renderScaleMultiplier);

        villagerModel.root.calculateTransform(modelMatrix);
        texture.bind(0);
        renderModelPart(villagerModel.root);
        texture.unbind();
    }

    private void renderCreeper(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float limbSwing, float limbSwingAmount, float partialTick, float headYaw,
            float baseHurtFlash) {
        float headPitch = mob.getRenderPitch(partialTick);
        creeperModel.animate(limbSwing, limbSwingAmount, headYaw, headPitch);

        float deathRotation = 0;
        if (mob.isDead()) {
            deathRotation = Math.min(mob.getDeathTime() * 0.1f, 1.5f);
        }

        CreeperFuseVisual fuseVisual = mob instanceof Creeper creeper
                ? creeperFuseVisual(creeper)
                : CreeperFuseVisual.NONE;
        if (fuseVisual.whiteFlash() > 0.0f) {
            shader.setUniform("hurtFlashColor", CREEPER_FUSE_FLASH_COLOR);
            shader.setUniform("hurtFlash", Math.max(baseHurtFlash, fuseVisual.whiteFlash() * 2.0f));
        }

        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.rotateZ(deathRotation);
        modelMatrix.scale(MODEL_SCALE * fuseVisual.horizontalScale() * renderScaleMultiplier,
                MODEL_SCALE * fuseVisual.verticalScale() * renderScaleMultiplier,
                MODEL_SCALE * fuseVisual.horizontalScale() * renderScaleMultiplier);

        creeperModel.root.calculateTransform(modelMatrix);

        texture.bind(0);
        renderModelPart(creeperModel.root);
        texture.unbind();

        shader.setUniform("hurtFlashColor", HURT_FLASH_COLOR);
        shader.setUniform("hurtFlash", baseHurtFlash);

        if (mob instanceof Creeper creeper && creeper.isPowered()) {
            renderChargedCreeperOverlay();
        }
    }

    private void renderChargedCreeperOverlay() {
        Texture powerTexture = MobTexture.get(CHARGED_CREEPER_TEXTURE);
        if (powerTexture == null) {
            return;
        }
        shader.setUniform("hurtFlash", 0.0f);
        shader.setUniform("alphaCutoff", 0.1f);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        powerTexture.bind(0);
        renderModelPart(creeperModel.root);
        powerTexture.unbind();
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_BLEND);
        shader.setUniform("alphaCutoff", 0.0f);
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
        modelMatrix.scale(MODEL_SCALE * 0.75f * mob.getRenderScale() * renderScaleMultiplier);

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
        modelMatrix.scale(MODEL_SCALE * scale * renderScaleMultiplier);

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
        skeletonModel.setBowAnimation(skeletonBowPoseProgress(mob));

        float deathRotation = 0;
        if (mob.isDead()) {
            deathRotation = Math.min(mob.getDeathTime() * 0.1f, 1.5f);
        }

        // Set up transforms
        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.rotateZ(deathRotation);
        modelMatrix.scale(MODEL_SCALE * renderScaleMultiplier);

        skeletonModel.root.calculateTransform(modelMatrix);

        texture.bind(0);
        renderModelPart(skeletonModel.root);
        texture.unbind();
    }

    private void renderQuadruped(Mob mob, Texture texture,
            float x, float y, float z, float bodyYaw,
            float limbSwing, float limbSwingAmount, float ageInTicks,
            float partialTick, float headYaw) {

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

        if (mob instanceof Sheep sheep && model instanceof SheepModel sheepBodyModel) {
            sheepBodyModel.animate(limbSwing, limbSwingAmount, ageInTicks,
                    sheep.getGrassEatingHeadOffsetScale(partialTick),
                    sheep.getGrassEatingHeadPitch(partialTick));
        } else {
            model.animate(limbSwing, limbSwingAmount, ageInTicks);
        }

        // Death animation - fall over like humanoids
        float deathRotation = 0;
        if (mob.isDead()) {
            deathRotation = Math.min(mob.getDeathTime() * 0.1f, 1.5f);
        }

        modelMatrix.identity();
        modelMatrix.translate(x, y + yOffset, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.rotateZ(deathRotation);
        modelMatrix.scale(MODEL_SCALE * mob.getRenderScale() * renderScaleMultiplier);

        model.root.calculateTransform(modelMatrix);

        texture.bind(0);
        renderModelPart(model.root);
        texture.unbind();

        // Render sheep fur layer if not sheared
        if (mob instanceof Sheep sheep && !sheep.isSheared()) {
            sheepFurModel.animate(limbSwing, limbSwingAmount, ageInTicks,
                    sheep.getGrassEatingHeadOffsetScale(partialTick),
                    sheep.getGrassEatingHeadPitch(partialTick));
            sheepFurModel.root.calculateTransform(modelMatrix);

            Texture furTexture = MobTexture.get("/textures/mob/sheep_fur.png");
            if (furTexture != null) {
                float[] fleece = sheep.getFleeceColor();
                renderer.setEntityTint(new Vector3f(fleece[0], fleece[1], fleece[2]));
                furTexture.bind(0);
                renderModelPart(sheepFurModel.root);
                furTexture.unbind();
                renderer.setEntityTint(new Vector3f(1.0f, 1.0f, 1.0f));
            }
        }

        PigSaddleLayer saddleLayer = mob instanceof Pig pig
                ? pigSaddleLayer(pig)
                : PigSaddleLayer.NONE;
        if (saddleLayer.visible()) {
            renderPigSaddleLayer(saddleLayer, x, y + yOffset, z, bodyYaw,
                    limbSwing, limbSwingAmount, ageInTicks, mob.getRenderScale());
        }
    }

    private void renderPigSaddleLayer(PigSaddleLayer saddleLayer,
            float x, float y, float z, float bodyYaw,
            float limbSwing, float limbSwingAmount, float ageInTicks, float renderScale) {
        if (pigSaddleModel == null) {
            return;
        }
        Texture saddleTexture = MobTexture.get(saddleLayer.texturePath());
        if (saddleTexture == null) {
            return;
        }

        pigSaddleModel.animate(limbSwing, limbSwingAmount, ageInTicks);
        modelMatrix.identity();
        modelMatrix.translate(x, y, z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.scale(MODEL_SCALE * renderScale * renderScaleMultiplier);
        pigSaddleModel.root.calculateTransform(modelMatrix);

        shader.setUniform("alphaCutoff", 0.1f);
        saddleTexture.bind(0);
        renderModelPart(pigSaddleModel.root);
        saddleTexture.unbind();
        shader.setUniform("alphaCutoff", 0.0f);
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
        float padding = entity == null ? 0.0f : Math.max(entity.getWidth(), entity.getHeight());
        return RenderDistanceCulling.isEntityTooFar(camera, entity, distance + padding);
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
        FireOverlayTransform transform = fireOverlayTransform(mob);
        modelMatrix.translate(x, y + transform.centerYOffset(), z);
        modelMatrix.rotateY((float) Math.toRadians(-bodyYaw));
        modelMatrix.scale(transform.width(), transform.height(), transform.depth());
        shader.setUniform("modelMatrix", modelMatrix);
        fireOverlayMesh.render();
        shader.setUniform("alphaCutoff", 0.0f);
        terrainAtlas.unbind();
    }

    static FireOverlayTransform fireOverlayTransform(Mob mob) {
        float width = Math.max(mob.getWidth() * 1.35f, 0.7f * mob.getRenderScale());
        float height = mob.getHeight() * 1.08f;
        return new FireOverlayTransform(mob.getHeight() * 0.5f, width, height, width);
    }

    record FireOverlayTransform(float centerYOffset, float width, float height, float depth) {
    }

    static CreeperFuseVisual creeperFuseVisual(Creeper creeper) {
        if (creeper == null || !creeper.isIgnited()) {
            return CreeperFuseVisual.NONE;
        }
        return creeperFuseVisual(creeper.getFuseProgress());
    }

    static CreeperFuseVisual creeperFuseVisual(float fuseProgress) {
        float fuse = clamp01(fuseProgress);
        float pulse = 1.0f + (float) Math.sin(fuse * 100.0f) * fuse * 0.01f;
        float eased = fuse * fuse;
        eased *= eased;
        float horizontalScale = (1.0f + eased * 0.4f) * pulse;
        float verticalScale = (1.0f + eased * 0.1f) / pulse;
        float whiteFlash = (int) (fuse * 10.0f) % 2 == 0
                ? 0.0f
                : fuse * 0.2f;
        return new CreeperFuseVisual(horizontalScale, verticalScale, whiteFlash);
    }

    record CreeperFuseVisual(float horizontalScale, float verticalScale, float whiteFlash) {
        static final CreeperFuseVisual NONE = new CreeperFuseVisual(1.0f, 1.0f, 0.0f);
    }

    static PigSaddleLayer pigSaddleLayer(Pig pig) {
        if (pig == null || !pig.isSaddled()) {
            return PigSaddleLayer.NONE;
        }
        return new PigSaddleLayer(true, PIG_SADDLE_TEXTURE, PigModel.SADDLE_OVERLAY_INFLATE);
    }

    record PigSaddleLayer(boolean visible, String texturePath, float modelInflate) {
        static final PigSaddleLayer NONE = new PigSaddleLayer(false, "", 0.0f);
    }

    static SpiderEyeLayer spiderEyeLayer(Mob mob) {
        if (!(mob instanceof Spider)) {
            return SpiderEyeLayer.NONE;
        }
        return new SpiderEyeLayer(true, SPIDER_EYES_TEXTURE, 1.0f, 0.1f);
    }

    record SpiderEyeLayer(boolean visible, String texturePath, float brightness, float alphaCutoff) {
        static final SpiderEyeLayer NONE = new SpiderEyeLayer(false, "", 0.0f, 0.0f);
    }

    static EndermanCarriedBlockTransform endermanCarriedBlockTransform(Enderman enderman) {
        if (enderman == null || enderman.getCarriedBlock() == null
                || enderman.getCarriedBlock() == BlockType.AIR) {
            return EndermanCarriedBlockTransform.NONE;
        }
        int metadata = Math.max(0, Math.min(15, enderman.getCarriedMetadata()));
        return new EndermanCarriedBlockTransform(true, enderman.getCarriedBlock(), metadata,
                1.72f, -0.55f, 0.5f);
    }

    record EndermanCarriedBlockTransform(boolean visible, BlockType block, int metadata,
            float centerYOffset, float forwardOffset, float scale) {
        static final EndermanCarriedBlockTransform NONE = new EndermanCarriedBlockTransform(false,
                BlockType.AIR, 0, 0.0f, 0.0f, 0.0f);
    }

    static float skeletonBowPoseProgress(Mob mob) {
        return mob instanceof Skeleton skeleton && skeleton.isRangedAttackActive() ? 1.0f : 0.0f;
    }

    private static float clamp01(float value) {
        if (Float.isNaN(value)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
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

    private Mesh createCarriedBlockMesh(CarriedBlockMeshKey key) {
        java.util.ArrayList<Float> positions = new java.util.ArrayList<>();
        java.util.ArrayList<Float> texCoords = new java.util.ArrayList<>();
        java.util.ArrayList<Float> normals = new java.util.ArrayList<>();
        java.util.ArrayList<Float> colors = new java.util.ArrayList<>();
        java.util.ArrayList<Integer> indices = new java.util.ArrayList<>();
        int vertexCount = 0;
        VoxelShape shape = BlockShape.renderShape(
                new BlockState(key.type(), key.metadata()),
                emptyBlockContext());
        for (BlockShape.Cuboid box : shape.boxes()) {
            for (int face = 0; face < 6; face++) {
                float[] faceVerts = Block.getCuboidFaceVertices(face, -0.5f, -0.5f, -0.5f, box);
                for (float v : faceVerts) {
                    positions.add(v);
                }
                float[] uv = Block.getFaceTexCoords(key.type(), face, key.metadata());
                for (float t : uv) {
                    texCoords.add(t);
                }
                float[] faceNormals = Block.getFaceNormals(face);
                for (float n : faceNormals) {
                    normals.add(n);
                }
                float shade = faceShade(face);
                for (int i = 0; i < 4; i++) {
                    colors.add(shade);
                    colors.add(shade);
                    colors.add(shade);
                }
                for (int idx : Block.getFaceIndices(vertexCount)) {
                    indices.add(idx);
                }
                vertexCount += 4;
            }
        }
        return new Mesh(toFloatArray(positions), toFloatArray(texCoords), toFloatArray(normals),
                toFloatArray(colors), toIntArray(indices));
    }

    private static float faceShade(int face) {
        return switch (face) {
            case Block.FACE_BOTTOM -> 0.5f;
            case Block.FACE_NORTH, Block.FACE_SOUTH -> 0.8f;
            case Block.FACE_EAST, Block.FACE_WEST -> 0.6f;
            default -> 1.0f;
        };
    }

    private static BlockShape.BlockContext emptyBlockContext() {
        return new BlockShape.BlockContext() {
            @Override
            public BlockType getBlock(int dx, int dy, int dz) {
                return BlockType.AIR;
            }

            @Override
            public int getMetadata(int dx, int dy, int dz) {
                return 0;
            }
        };
    }

    private static float[] toFloatArray(List<Float> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    public void cleanup() {
        if (humanoidModel != null)
            humanoidModel.cleanup();
        if (skeletonModel != null)
            skeletonModel.cleanup();
        if (pigModel != null)
            pigModel.cleanup();
        if (pigSaddleModel != null)
            pigSaddleModel.cleanup();
        if (cowModel != null)
            cowModel.cleanup();
        if (sheepModel != null)
            sheepModel.cleanup();
        if (sheepFurModel != null)
            sheepFurModel.cleanup();
        if (wolfModel != null)
            wolfModel.cleanup();
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
        if (snowGolemModel != null)
            snowGolemModel.cleanup();
        if (villagerModel != null)
            villagerModel.cleanup();
        if (dragonModel != null)
            dragonModel.cleanup();
        if (fireOverlayMesh != null)
            fireOverlayMesh.cleanup();
        for (Mesh mesh : carriedBlockMeshes.values()) {
            mesh.cleanup();
        }
        carriedBlockMeshes.clear();

        MobTexture.cleanup();
    }

    private record CarriedBlockMeshKey(BlockType type, int metadata) {
    }
}
