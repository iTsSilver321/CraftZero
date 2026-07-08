package com.craftzero.graphics;

import com.craftzero.inventory.ItemType;
import com.craftzero.world.World;
import com.craftzero.world.WorldParticle;
import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

/**
 * Renders transient world particles as camera-facing textured quads.
 */
public class ParticleRenderer {
    private static final float PARTICLE_RENDER_DISTANCE = 96.0f;
    static final int FLAME_PARTICLE_ATLAS_INDEX = 48;
    static final int SPLASH_PARTICLE_ATLAS_INDEX = 20;
    static final int DRIP_FALL_PARTICLE_ATLAS_INDEX = 112;
    static final int DRIP_HANG_PARTICLE_ATLAS_INDEX = 113;
    static final int DRIP_LAVA_GROUND_PARTICLE_ATLAS_INDEX = 114;
    static final int CRIT_PARTICLE_ATLAS_INDEX = 65;
    private static final Vector3f WHITE_TINT = new Vector3f(1.0f, 1.0f, 1.0f);
    static final Vector3f PORTAL_TINT = new Vector3f(0.9f, 0.3f, 1.0f);
    static final Vector3f WATER_DRIP_TINT = new Vector3f(0.20f, 0.30f, 1.0f);
    static final Vector3f LAVA_TINT = new Vector3f(1.0f, 0.35f, 0.05f);
    static final Vector3f ENCHANTMENT_TINT = new Vector3f(0.90f, 0.90f, 1.0f);
    static final Vector3f SUSPENDED_TINT = new Vector3f(0.70f, 0.85f, 1.0f);
    static final Vector3f DEPTH_SUSPEND_TINT = new Vector3f(0.36f, 0.36f, 0.38f);
    static final Vector3f MAGIC_CRIT_TINT = new Vector3f(0.55f, 0.85f, 1.0f);
    static final Vector3f TOWN_AURA_TINT = new Vector3f(0.50f, 0.35f, 0.70f);

    private final Renderer renderer;
    private final ShaderProgram shader;
    private final Matrix4f modelMatrix = new Matrix4f();
    private Mesh heartMesh;
    private Mesh smokeMesh;
    private Mesh critMesh;
    private Mesh footstepMesh;
    private Mesh flameMesh;
    private Mesh splashMesh;
    private Mesh noteMesh;
    private Mesh bubbleMesh;
    private Mesh dripHangMesh;
    private Mesh dripFallMesh;
    private Mesh dripLavaGroundMesh;
    private Mesh lavaMesh;
    private Mesh suspendedMesh;
    private Mesh rainMesh;
    private Mesh snowMesh;
    private Mesh[] portalMeshes;
    private Mesh[] redDustMeshes;
    private Mesh[] smokeAnimationMeshes;
    private Mesh[] spellMeshes;
    private Mesh[] instantSpellMeshes;
    private Mesh[] explosionMeshes;
    private final Map<Integer, Mesh> blockParticleMeshes = new HashMap<>();
    private final Map<Integer, Mesh> itemParticleMeshes = new HashMap<>();
    private final Map<Integer, Mesh> glyphParticleMeshes = new HashMap<>();
    private Texture particleTexture;
    private Texture rainTexture;
    private Texture snowTexture;
    private Texture explosionTexture;
    private Texture glyphTexture;
    private Texture footstepTexture;

    public ParticleRenderer(Renderer renderer) {
        this.renderer = renderer;
        this.shader = renderer.getShaderProgram();
    }

    public void init() throws Exception {
        particleTexture = new Texture("/textures/gui/particles.png");
        heartMesh = createQuad(GuiTexture.getFullHeartUV());
        smokeMesh = createQuad(smokeUv());
        critMesh = createQuad(particleCellUv(CRIT_PARTICLE_ATLAS_INDEX));
        footstepMesh = createGroundQuad(fullTextureUv());
        flameMesh = createQuad(particleCellUv(FLAME_PARTICLE_ATLAS_INDEX));
        splashMesh = createQuad(particleCellUv(SPLASH_PARTICLE_ATLAS_INDEX));
        noteMesh = createQuad(noteUv());
        bubbleMesh = createQuad(bubbleUv());
        dripHangMesh = createQuad(particleCellUv(DRIP_HANG_PARTICLE_ATLAS_INDEX));
        dripFallMesh = createQuad(particleCellUv(DRIP_FALL_PARTICLE_ATLAS_INDEX));
        dripLavaGroundMesh = createQuad(particleCellUv(DRIP_LAVA_GROUND_PARTICLE_ATLAS_INDEX));
        lavaMesh = createQuad(particleCellUv(49));
        suspendedMesh = createQuad(suspendedUv());
        rainTexture = new Texture("/textures/environment/rain.png");
        snowTexture = new Texture("/textures/environment/snow.png");
        rainMesh = createWeatherQuad(0.08f, 0.80f, fullTextureUv());
        snowMesh = createWeatherQuad(0.45f, 0.75f, fullTextureUv());
        portalMeshes = createParticleCellMeshes(8);
        redDustMeshes = createParticleCellMeshes(8);
        smokeAnimationMeshes = createParticleCellMeshes(8);
        spellMeshes = createParticleCellMeshes(128, 8);
        instantSpellMeshes = createParticleCellMeshes(144, 8);
        explosionMeshes = createExplosionMeshes();
        explosionTexture = new Texture("/textures/misc/explosion.png");
        glyphTexture = new Texture("/textures/font/alternate.png");
        footstepTexture = new Texture("/textures/misc/footprint.png");
    }

    public void render(World world, Camera camera, float partialTick, Texture terrainTexture) {
        render(world, camera, partialTick, terrainTexture, 0);
    }

    public void render(World world, Camera camera, float partialTick, Texture terrainTexture, int particleSetting) {
        if (world == null || camera == null) {
            return;
        }
        float framePartialTick = sanitizePartialTick(partialTick);
        List<WorldParticle> particles = world.getParticles();
        if (particles.isEmpty()) {
            return;
        }

        glDisable(GL_CULL_FACE);
        shader.setUniform("alphaCutoff", 0.05f);
        float previousSunBrightness = renderer.getSunBrightness();
        shader.setUniform("sunBrightness", 1.0f);

        Texture currentTexture = null;
        int renderedIndex = 0;
        for (WorldParticle particle : particles) {
            if (particle == null || !shouldRenderParticle(renderedIndex++, particleSetting)) {
                continue;
            }
            float x = particle.getRenderX(framePartialTick);
            float y = particle.getRenderY(framePartialTick);
            float z = particle.getRenderZ(framePartialTick);
            if (!allFinite(x, y, z)) {
                continue;
            }
            if (isTooFar(camera, x, y, z)) {
                continue;
            }

            Texture requiredTexture = textureFor(particle, terrainTexture);
            Mesh mesh = meshFor(particle, framePartialTick);
            if (requiredTexture == null || mesh == null) {
                continue;
            }
            if (requiredTexture != currentTexture) {
                requiredTexture.bind(0);
                currentTexture = requiredTexture;
            }

            renderer.setEntityBrightness(computeParticleBrightness(world, particle, x, y, z));
            renderer.setEntityTint(tintFor(particle));
            float scale = particle.getScale(framePartialTick);
            if (!Float.isFinite(scale) || scale <= 0.0f) {
                continue;
            }
            modelMatrix.identity()
                    .translate(x, y, z);
            if (particle.getType() != WorldParticle.Type.FOOTSTEP) {
                modelMatrix
                        .rotateY((float) Math.toRadians(-camera.getYaw()))
                        .rotateX((float) Math.toRadians(camera.getPitch()));
            }
            modelMatrix.scale(scale);
            renderer.renderMesh(mesh, modelMatrix);
        }

        if (currentTexture != null) {
            currentTexture.unbind();
        }
        renderer.setEntityTint(WHITE_TINT);
        renderer.setEntityBrightness(0.0f);
        shader.setUniform("sunBrightness", previousSunBrightness);
        shader.setUniform("alphaCutoff", 0.0f);
        glEnable(GL_CULL_FACE);
    }

    private static boolean shouldRenderParticle(int index, int particleSetting) {
        if (index < 0) {
            return false;
        }
        if (particleSetting <= 0) {
            return true;
        }
        if (particleSetting == 1) {
            return index % 3 != 0;
        }
        return index % 10 == 0;
    }

    private static float computeParticleBrightness(World world, WorldParticle particle, float x, float y, float z) {
        if (world == null || particle == null || !allFinite(x, y, z) || isFullBrightParticle(particle.getType())) {
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
        float light = Math.max(0.0f, Math.min(15.0f, lightLevel)) / 15.0f;
        return Math.max(0.08f, light / (3.0f - 2.0f * light));
    }

    private static boolean isFullBrightParticle(WorldParticle.Type type) {
        return type == WorldParticle.Type.LARGE_EXPLOSION
                || type == WorldParticle.Type.HUGE_EXPLOSION
                || type == WorldParticle.Type.FLAME
                || type == WorldParticle.Type.LAVA
                || type == WorldParticle.Type.DRIP_LAVA;
    }

    private Texture textureFor(WorldParticle particle, Texture terrainTexture) {
        WorldParticle.Type type = particle.getType();
        if (type == WorldParticle.Type.HEART) {
            return GuiTexture.getIconsTexture();
        }
        if (type == WorldParticle.Type.BLOCK_CRACK || type == WorldParticle.Type.BLOCK_DUST) {
            return terrainTexture;
        }
        if (type == WorldParticle.Type.LARGE_EXPLOSION || type == WorldParticle.Type.HUGE_EXPLOSION) {
            return explosionTexture;
        }
        if (type == WorldParticle.Type.FOOTSTEP) {
            return footstepTexture;
        }
        if (type == WorldParticle.Type.RAIN) {
            return rainTexture;
        }
        if (type == WorldParticle.Type.SNOW) {
            return snowTexture;
        }
        if (type == WorldParticle.Type.ITEM_CRACK || type == WorldParticle.Type.ITEM_PICKUP
                || type == WorldParticle.Type.SLIME || type == WorldParticle.Type.SNOWBALL_POOF) {
            ItemType itemType = itemParticleTypeForRender(particle);
            return ItemTextureResolver.usesItemsAtlas(itemType) && GuiTexture.getItemsTexture() != null
                    ? GuiTexture.getItemsTexture()
                    : terrainTexture;
        }
        if (type == WorldParticle.Type.ENCHANTMENT_TABLE) {
            return glyphTexture;
        }
        return particleTexture;
    }

    private Mesh meshFor(WorldParticle particle, float partialTick) {
        return switch (particle.getType()) {
            case HEART -> heartMesh;
            case CRIT, MAGIC_CRIT -> critMesh;
            case FOOTSTEP -> footstepMesh;
            case FLAME -> flameMesh;
            case SPLASH -> splashMesh;
            case NOTE -> noteMesh;
            case PORTAL -> portalMeshFor(particle, partialTick);
            case BUBBLE -> bubbleMesh;
            case SUSPENDED, DEPTH_SUSPEND, TOWN_AURA -> suspendedMesh;
            case DRIP_WATER, DRIP_LAVA -> dripMeshFor(particle, partialTick);
            case LAVA -> lavaMesh;
            case RED_DUST -> redDustMeshFor(particle, partialTick);
            case SMOKE, LARGE_SMOKE, EXPLODE, SNOW_SHOVEL -> smokeAnimationMeshFor(particle, partialTick);
            case MOB_SPELL, SPELL, INSTANT_SPELL -> spellMeshFor(particle, partialTick);
            case RAIN -> rainMesh;
            case SNOW -> snowMesh;
            case BLOCK_CRACK, BLOCK_DUST -> blockParticleMeshFor(particle);
            case SLIME, SNOWBALL_POOF -> itemParticleMeshFor(itemParticleTypeForRender(particle));
            case ITEM_CRACK, ITEM_PICKUP -> itemParticleMeshFor(particle);
            case ENCHANTMENT_TABLE -> glyphParticleMeshFor(particle);
            case LARGE_EXPLOSION, HUGE_EXPLOSION -> explosionMeshFor(particle, partialTick);
            default -> smokeMesh;
        };
    }

    private Mesh blockParticleMeshFor(WorldParticle particle) {
        BlockType type = particle.getBlockParticleType();
        if (type == BlockType.AIR) {
            return smokeMesh;
        }
        int key = Math.max(0, Math.round(particle.getData()));
        return blockParticleMeshes.computeIfAbsent(key, ignored -> createQuad(faceUvBounds(
                type,
                particle.getBlockParticleFace(),
                particle.getBlockParticleMetadata())));
    }

    private Mesh itemParticleMeshFor(WorldParticle particle) {
        ItemType type = particle.getItemParticleType();
        return itemParticleMeshFor(type);
    }

    static ItemType itemParticleTypeForRender(WorldParticle particle) {
        if (particle == null) {
            return null;
        }
        if (particle.getType() == WorldParticle.Type.SNOWBALL_POOF) {
            return ItemType.SNOWBALL;
        }
        if (particle.getType() == WorldParticle.Type.SLIME) {
            return ItemType.SLIMEBALL;
        }
        return particle.getItemParticleType();
    }

    private Mesh itemParticleMeshFor(ItemType type) {
        if (type == null) {
            return smokeMesh;
        }
        int key = type.ordinal();
        return itemParticleMeshes.computeIfAbsent(key, ignored -> createQuad(ItemTextureResolver.getUv(type)));
    }

    private Mesh glyphParticleMeshFor(WorldParticle particle) {
        int glyph = Math.floorMod(Math.round(particle.getData()), 256);
        return glyphParticleMeshes.computeIfAbsent(glyph, ignored -> createQuad(glyphUv(glyph)));
    }

    private Mesh redDustMeshFor(WorldParticle particle, float partialTick) {
        if (redDustMeshes == null || redDustMeshes.length == 0) {
            return smokeMesh;
        }
        return redDustMeshes[redDustTextureFrame(particle, partialTick)];
    }

    private Mesh portalMeshFor(WorldParticle particle, float partialTick) {
        if (portalMeshes == null || portalMeshes.length == 0) {
            return smokeMesh;
        }
        return portalMeshes[portalTextureFrame(particle, partialTick)];
    }

    private Mesh smokeAnimationMeshFor(WorldParticle particle, float partialTick) {
        if (smokeAnimationMeshes == null || smokeAnimationMeshes.length == 0) {
            return smokeMesh;
        }
        return smokeAnimationMeshes[smokeTextureFrame(particle, partialTick)];
    }

    private Mesh spellMeshFor(WorldParticle particle, float partialTick) {
        Mesh[] meshes = particle.getType() == WorldParticle.Type.INSTANT_SPELL
                ? instantSpellMeshes
                : spellMeshes;
        if (meshes == null || meshes.length == 0) {
            return smokeMesh;
        }
        return meshes[spellTextureFrame(particle, partialTick)];
    }

    private Mesh dripMeshFor(WorldParticle particle, float partialTick) {
        return switch (dripTextureAtlasIndex(particle, partialTick)) {
            case DRIP_HANG_PARTICLE_ATLAS_INDEX -> dripHangMesh;
            case DRIP_LAVA_GROUND_PARTICLE_ATLAS_INDEX -> dripLavaGroundMesh;
            default -> dripFallMesh;
        };
    }

    private Mesh explosionMeshFor(WorldParticle particle, float partialTick) {
        if (explosionMeshes == null || explosionMeshes.length == 0) {
            return smokeMesh;
        }
        if (particle == null || particle.getLifetimeTicks() <= 0.0f) {
            return smokeMesh;
        }
        float age = Math.min(particle.getLifetimeTicks(), particle.getAgeTicks() + sanitizePartialTick(partialTick));
        int frame = Math.min(explosionMeshes.length - 1,
                (int) (age / particle.getLifetimeTicks() * explosionMeshes.length));
        return explosionMeshes[frame];
    }

    private static boolean isTooFar(Camera camera, float x, float y, float z) {
        return RenderDistanceCulling.isPointTooFar(camera, x, y, z, PARTICLE_RENDER_DISTANCE);
    }

    static int redDustTextureFrame(WorldParticle particle, float partialTick) {
        return animatedEightFrameIndex(particle, partialTick);
    }

    static int portalTextureFrame(WorldParticle particle, float partialTick) {
        if (particle == null) {
            return 0;
        }
        return Math.floorMod(Math.round(particle.getData()), 8);
    }

    static int smokeTextureFrame(WorldParticle particle, float partialTick) {
        return animatedEightFrameIndex(particle, partialTick);
    }

    static int spellTextureFrame(WorldParticle particle, float partialTick) {
        return animatedEightFrameIndex(particle, partialTick);
    }

    static int spellTextureAtlasIndex(WorldParticle particle, float partialTick) {
        int base = particle != null && particle.getType() == WorldParticle.Type.INSTANT_SPELL ? 144 : 128;
        return base + spellTextureFrame(particle, partialTick);
    }

    static int dripTextureAtlasIndex(WorldParticle particle, float partialTick) {
        if (particle == null) {
            return DRIP_HANG_PARTICLE_ATLAS_INDEX;
        }
        if (particle.getType() == WorldParticle.Type.DRIP_LAVA && particle.isOnGround()) {
            return DRIP_LAVA_GROUND_PARTICLE_ATLAS_INDEX;
        }
        return particle.isDripBobPhase(partialTick)
                ? DRIP_HANG_PARTICLE_ATLAS_INDEX
                : DRIP_FALL_PARTICLE_ATLAS_INDEX;
    }

    private static int animatedEightFrameIndex(WorldParticle particle, float partialTick) {
        if (particle == null || particle.getLifetimeTicks() <= 0.0f) {
            return 7;
        }
        float age = Math.min(particle.getLifetimeTicks(),
                particle.getAgeTicks() + sanitizePartialTick(partialTick));
        int frame = 7 - (int) (age * 8.0f / particle.getLifetimeTicks());
        return Math.max(0, Math.min(7, frame));
    }

    static Vector3f tintFor(WorldParticle particle) {
        if (particle == null) {
            return WHITE_TINT;
        }
        if (particle.getType() == WorldParticle.Type.PORTAL) {
            return PORTAL_TINT;
        }
        if (particle.getType() == WorldParticle.Type.MOB_SPELL
                || particle.getType() == WorldParticle.Type.SPELL
                || particle.getType() == WorldParticle.Type.INSTANT_SPELL) {
            return packedRgbTint(particle.getData());
        }
        if (particle.getType() == WorldParticle.Type.NOTE) {
            return noteTint(particle.getData());
        }
        if (particle.getType() == WorldParticle.Type.CRIT) {
            return critTint(particle);
        }
        if (particle.getType() == WorldParticle.Type.MAGIC_CRIT) {
            return magicCritTint(particle);
        }
        if (particle.getType() == WorldParticle.Type.TOWN_AURA) {
            return TOWN_AURA_TINT;
        }
        if (particle.getType() == WorldParticle.Type.DRIP_WATER) {
            return WATER_DRIP_TINT;
        }
        if (particle.getType() == WorldParticle.Type.SUSPENDED) {
            return SUSPENDED_TINT;
        }
        if (particle.getType() == WorldParticle.Type.DEPTH_SUSPEND) {
            return DEPTH_SUSPEND_TINT;
        }
        if (particle.getType() == WorldParticle.Type.DRIP_LAVA) {
            return lavaDripTint(particle);
        }
        if (particle.getType() == WorldParticle.Type.LAVA) {
            return LAVA_TINT;
        }
        if (particle.getType() == WorldParticle.Type.RED_DUST) {
            return redstoneTint(particle.getData());
        }
        if (particle.getType() == WorldParticle.Type.ENCHANTMENT_TABLE) {
            return ENCHANTMENT_TINT;
        }
        return WHITE_TINT;
    }

    static Vector3f critTint(WorldParticle particle) {
        float base = particle == null || particle.getData() <= 0.0f
                ? 0.75f
                : Math.max(0.0f, Math.min(1.0f, particle.getData()));
        float age = particle == null ? 0.0f : Math.max(0.0f, particle.getAgeTicks());
        return new Vector3f(base,
                base * (float) Math.pow(0.96f, age),
                base * (float) Math.pow(0.90f, age));
    }

    static Vector3f magicCritTint(WorldParticle particle) {
        float age = particle == null ? 0.0f : Math.max(0.0f, particle.getAgeTicks());
        return new Vector3f(MAGIC_CRIT_TINT.x,
                MAGIC_CRIT_TINT.y * (float) Math.pow(0.96f, age),
                MAGIC_CRIT_TINT.z * (float) Math.pow(0.90f, age));
    }

    static Vector3f lavaDripTint(WorldParticle particle) {
        float age = particle == null ? 0.0f : Math.max(0.0f,
                Math.min(WorldParticle.DRIP_BOB_TICKS, particle.getAgeTicks()));
        return new Vector3f(1.0f,
                16.0f / (age + 16.0f),
                4.0f / (age + 8.0f));
    }

    static Vector3f redstoneTint(float power) {
        if (!Float.isFinite(power)) {
            return new Vector3f(1.0f, 0.0f, 0.0f);
        }
        if (power < 0.0f) {
            return new Vector3f(1.0f, 0.0f, 0.0f);
        }
        float strength = Math.max(0.0f, Math.min(1.0f, power / 15.0f));
        float red = strength * 0.6f + 0.4f;
        float green = Math.max(0.0f, strength * strength * 0.7f - 0.5f);
        float blue = Math.max(0.0f, strength * strength * 0.6f - 0.7f);
        return new Vector3f(red, green, blue);
    }

    static Vector3f packedRgbTint(float packedRgb) {
        if (!Float.isFinite(packedRgb)) {
            return WHITE_TINT;
        }
        int color = Math.max(0, Math.min(0xFFFFFF, Math.round(packedRgb)));
        float red = ((color >> 16) & 0xFF) / 255.0f;
        float green = ((color >> 8) & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;
        return new Vector3f(red, green, blue);
    }

    static Vector3f noteTint(float note) {
        if (!Float.isFinite(note)) {
            note = 0.0f;
        }
        float red = noteColorChannel(note + 0.0f);
        float green = noteColorChannel(note + 1.0f / 3.0f);
        float blue = noteColorChannel(note + 2.0f / 3.0f);
        return new Vector3f(red, green, blue);
    }

    private static float noteColorChannel(float phase) {
        if (!Float.isFinite(phase)) {
            return 0.35f;
        }
        return (float) (Math.sin(phase * Math.PI * 2.0) * 0.65 + 0.35);
    }

    private static float sanitizePartialTick(float partialTick) {
        if (!Float.isFinite(partialTick)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, partialTick));
    }

    private static boolean allFinite(float... values) {
        for (float value : values) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    private static Mesh createQuad(float[] uv) {
        float h = 0.5f;
        float u0 = uv[0];
        float v0 = uv[1];
        float u1 = uv[2];
        float v1 = uv[3];
        float[] positions = {
                -h, h, 0.0f,
                -h, -h, 0.0f,
                h, -h, 0.0f,
                h, h, 0.0f
        };
        float[] texCoords = {
                u0, v0,
                u0, v1,
                u1, v1,
                u1, v0
        };
        int[] indices = { 0, 1, 2, 2, 3, 0 };
        return new Mesh(positions, texCoords, indices);
    }

    private static float[] faceUvBounds(BlockType type, int face, int metadata) {
        float[] faceCoords = Block.getFaceTexCoords(type, face, metadata);
        return new float[] { faceCoords[0], faceCoords[1], faceCoords[4], faceCoords[5] };
    }

    private static Mesh createWeatherQuad(float halfWidth, float halfHeight, float[] uv) {
        float u0 = uv[0];
        float v0 = uv[1];
        float u1 = uv[2];
        float v1 = uv[3];
        float[] positions = {
                -halfWidth, halfHeight, 0.0f,
                -halfWidth, -halfHeight, 0.0f,
                halfWidth, -halfHeight, 0.0f,
                halfWidth, halfHeight, 0.0f
        };
        float[] texCoords = {
                u0, v0,
                u0, v1,
                u1, v1,
                u1, v0
        };
        int[] indices = { 0, 1, 2, 2, 3, 0 };
        return new Mesh(positions, texCoords, indices);
    }

    private static Mesh createGroundQuad(float[] uv) {
        float h = 0.5f;
        float u0 = uv[0];
        float v0 = uv[1];
        float u1 = uv[2];
        float v1 = uv[3];
        float[] positions = {
                -h, 0.0f, -h,
                -h, 0.0f, h,
                h, 0.0f, h,
                h, 0.0f, -h
        };
        float[] texCoords = {
                u0, v0,
                u0, v1,
                u1, v1,
                u1, v0
        };
        int[] indices = { 0, 1, 2, 2, 3, 0 };
        return new Mesh(positions, texCoords, indices);
    }

    private static Mesh[] createExplosionMeshes() {
        Mesh[] meshes = new Mesh[16];
        float cell = 1.0f / 4.0f;
        for (int frame = 0; frame < meshes.length; frame++) {
            int x = frame % 4;
            int y = frame / 4;
            meshes[frame] = createQuad(new float[] {
                    x * cell, y * cell,
                    (x + 1) * cell, (y + 1) * cell
            });
        }
        return meshes;
    }

    private static Mesh[] createParticleCellMeshes(int count) {
        return createParticleCellMeshes(0, count);
    }

    private static Mesh[] createParticleCellMeshes(int baseIndex, int count) {
        Mesh[] meshes = new Mesh[count];
        for (int frame = 0; frame < count; frame++) {
            meshes[frame] = createQuad(particleCellUv(baseIndex + frame));
        }
        return meshes;
    }

    static float[] particleCellUv(int index) {
        float atlas = 256.0f;
        float cell = 8.0f;
        int clamped = Math.max(0, index);
        float x = (clamped % 16) * cell;
        float y = (clamped / 16) * cell;
        return new float[] { x / atlas, y / atlas, (x + cell) / atlas, (y + cell) / atlas };
    }

    private static float[] smokeUv() {
        return particleCellUv(0);
    }

    private static float[] fullTextureUv() {
        return new float[] { 0.0f, 0.0f, 1.0f, 1.0f };
    }

    private static float[] glyphUv(int glyph) {
        float atlas = 256.0f;
        float cell = 16.0f;
        int index = Math.floorMod(glyph, 256);
        float x = (index % 16) * cell;
        float y = (index / 16) * cell;
        return new float[] { x / atlas, y / atlas, (x + cell) / atlas, (y + cell) / atlas };
    }

    private static float[] noteUv() {
        float atlas = 256.0f;
        float cell = 16.0f;
        float x = 0.0f;
        float y = 64.0f;
        return new float[] { x / atlas, y / atlas, (x + cell) / atlas, (y + cell) / atlas };
    }

    private static float[] bubbleUv() {
        float atlas = 256.0f;
        return new float[] { 32.0f / atlas, 0.0f, 40.0f / atlas, 8.0f / atlas };
    }

    private static float[] suspendedUv() {
        float atlas = 256.0f;
        return new float[] { 24.0f / atlas, 0.0f, 32.0f / atlas, 8.0f / atlas };
    }

    public void cleanup() {
        if (heartMesh != null) {
            heartMesh.cleanup();
        }
        if (smokeMesh != null) {
            smokeMesh.cleanup();
        }
        if (critMesh != null) {
            critMesh.cleanup();
        }
        if (footstepMesh != null) {
            footstepMesh.cleanup();
        }
        if (flameMesh != null) {
            flameMesh.cleanup();
        }
        if (splashMesh != null) {
            splashMesh.cleanup();
        }
        if (noteMesh != null) {
            noteMesh.cleanup();
        }
        if (bubbleMesh != null) {
            bubbleMesh.cleanup();
        }
        if (dripHangMesh != null) {
            dripHangMesh.cleanup();
        }
        if (dripFallMesh != null) {
            dripFallMesh.cleanup();
        }
        if (dripLavaGroundMesh != null) {
            dripLavaGroundMesh.cleanup();
        }
        if (lavaMesh != null) {
            lavaMesh.cleanup();
        }
        if (suspendedMesh != null) {
            suspendedMesh.cleanup();
        }
        if (rainMesh != null) {
            rainMesh.cleanup();
        }
        if (snowMesh != null) {
            snowMesh.cleanup();
        }
        if (explosionMeshes != null) {
            for (Mesh mesh : explosionMeshes) {
                if (mesh != null) {
                    mesh.cleanup();
                }
            }
        }
        if (portalMeshes != null) {
            for (Mesh mesh : portalMeshes) {
                if (mesh != null) {
                    mesh.cleanup();
                }
            }
        }
        if (redDustMeshes != null) {
            for (Mesh mesh : redDustMeshes) {
                if (mesh != null) {
                    mesh.cleanup();
                }
            }
        }
        if (smokeAnimationMeshes != null) {
            for (Mesh mesh : smokeAnimationMeshes) {
                if (mesh != null) {
                    mesh.cleanup();
                }
            }
        }
        if (spellMeshes != null) {
            for (Mesh mesh : spellMeshes) {
                if (mesh != null) {
                    mesh.cleanup();
                }
            }
        }
        if (instantSpellMeshes != null) {
            for (Mesh mesh : instantSpellMeshes) {
                if (mesh != null) {
                    mesh.cleanup();
                }
            }
        }
        for (Mesh mesh : blockParticleMeshes.values()) {
            if (mesh != null) {
                mesh.cleanup();
            }
        }
        blockParticleMeshes.clear();
        for (Mesh mesh : itemParticleMeshes.values()) {
            if (mesh != null) {
                mesh.cleanup();
            }
        }
        itemParticleMeshes.clear();
        for (Mesh mesh : glyphParticleMeshes.values()) {
            if (mesh != null) {
                mesh.cleanup();
            }
        }
        glyphParticleMeshes.clear();
        if (particleTexture != null) {
            particleTexture.cleanup();
        }
        if (rainTexture != null) {
            rainTexture.cleanup();
        }
        if (snowTexture != null) {
            snowTexture.cleanup();
        }
        if (explosionTexture != null) {
            explosionTexture.cleanup();
        }
        if (glyphTexture != null) {
            glyphTexture.cleanup();
        }
        if (footstepTexture != null) {
            footstepTexture.cleanup();
        }
    }
}
