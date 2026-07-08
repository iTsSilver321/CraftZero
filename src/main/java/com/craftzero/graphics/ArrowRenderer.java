package com.craftzero.graphics;

import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.BoatEntity;
import com.craftzero.entity.ChestMinecartEntity;
import com.craftzero.entity.EnderPearlEntity;
import com.craftzero.entity.EndCrystalEntity;
import com.craftzero.entity.Entity;
import com.craftzero.entity.ExperienceOrbEntity;
import com.craftzero.entity.EyeOfEnderEntity;
import com.craftzero.entity.FishingHookEntity;
import com.craftzero.entity.FireballEntity;
import com.craftzero.entity.FurnaceMinecartEntity;
import com.craftzero.entity.MinecartEntity;
import com.craftzero.entity.PaintingEntity;
import com.craftzero.entity.PrimedTntEntity;
import com.craftzero.entity.SplashPotionEntity;
import com.craftzero.entity.ThrownItemEntity;
import com.craftzero.entity.mob.EnderDragon;
import com.craftzero.inventory.ItemType;
import com.craftzero.main.Player;
import com.craftzero.world.Block;
import com.craftzero.world.BlockShape;
import com.craftzero.world.BlockType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders arrow projectiles with the Release 1.0 item sprite.
 */
public class ArrowRenderer {
    private static final float ARROW_RENDER_DISTANCE = 192.0f;
    private static final float FISHING_LINE_EYE_HEIGHT = 1.45f;
    private static final float FISHING_LINE_SNEAK_EYE_HEIGHT = 1.22f;
    private static final float FISHING_LINE_RIGHT_OFFSET = 0.35f;
    private static final float FISHING_LINE_FORWARD_OFFSET = 0.25f;
    private static final float DRAGON_BEAM_WIDTH = 3.0f;
    private static final Vector4f DRAGON_BEAM_COLOR = new Vector4f(0.82f, 0.22f, 1.0f, 1.0f);
    static final int DRAGON_BEAM_RING_COUNT = 12;
    private static final float DRAGON_BEAM_RADIUS = 0.22f;
    private final Renderer renderer;
    private final ShaderProgram shader;
    private ShaderProgram lineShader;
    private final Matrix4f modelMatrix = new Matrix4f();
    private int arrowVao;
    private int arrowVbo;
    private int arrowEbo;
    private int fireballVao;
    private int fireballVbo;
    private int fireballEbo;
    private int eyeVao;
    private int eyeVbo;
    private int eyeEbo;
    private int pearlVao;
    private int pearlVbo;
    private int pearlEbo;
    private int crystalVao;
    private int crystalVbo;
    private int crystalEbo;
    private int crystalIndexCount;
    private int xpOrbVao;
    private int xpOrbVbo;
    private int xpOrbEbo;
    private int potionVao;
    private int potionVbo;
    private int potionEbo;
    private int snowballVao;
    private int snowballVbo;
    private int snowballEbo;
    private int eggVao;
    private int eggVbo;
    private int eggEbo;
    private int fishingHookVao;
    private int fishingHookVbo;
    private int fishingHookEbo;
    private int fishingLineVao;
    private int fishingLineVbo;
    private int cartVao;
    private int cartVbo;
    private int cartEbo;
    private int cartIndexCount;
    private int chestMinecartPayloadVao;
    private int chestMinecartPayloadVbo;
    private int chestMinecartPayloadEbo;
    private int chestMinecartPayloadIndexCount;
    private int furnaceMinecartPayloadVao;
    private int furnaceMinecartPayloadVbo;
    private int furnaceMinecartPayloadEbo;
    private int furnaceMinecartPayloadIndexCount;
    private int litFurnaceMinecartPayloadVao;
    private int litFurnaceMinecartPayloadVbo;
    private int litFurnaceMinecartPayloadEbo;
    private int litFurnaceMinecartPayloadIndexCount;
    private int boatVao;
    private int boatVbo;
    private int boatEbo;
    private int boatIndexCount;
    private int tntVao;
    private int tntVbo;
    private int tntEbo;
    private final Map<PaintingEntity.Art, int[]> paintingMeshes = new EnumMap<>(PaintingEntity.Art.class);

    public ArrowRenderer(Renderer renderer) {
        this.renderer = renderer;
        this.shader = renderer.getShaderProgram();
    }

    public void init() {
        int[] arrowMesh = createSpriteMesh(ItemType.ARROW);
        arrowVao = arrowMesh[0];
        arrowVbo = arrowMesh[1];
        arrowEbo = arrowMesh[2];

        int[] fireballMesh = createTerrainSpriteMesh(BlockType.FIRE, Block.FACE_NORTH);
        fireballVao = fireballMesh[0];
        fireballVbo = fireballMesh[1];
        fireballEbo = fireballMesh[2];

        int[] eyeMesh = createSpriteMesh(ItemType.EYE_OF_ENDER);
        eyeVao = eyeMesh[0];
        eyeVbo = eyeMesh[1];
        eyeEbo = eyeMesh[2];

        int[] pearlMesh = createSpriteMesh(ItemType.ENDER_PEARL);
        pearlVao = pearlMesh[0];
        pearlVbo = pearlMesh[1];
        pearlEbo = pearlMesh[2];

        MeshData crystalData = crystalMeshData();
        int[] crystalMesh = createMesh(crystalData);
        crystalVao = crystalMesh[0];
        crystalVbo = crystalMesh[1];
        crystalEbo = crystalMesh[2];
        crystalIndexCount = crystalData.indices().length;

        int[] xpOrbMesh = createFullTextureSpriteMesh();
        xpOrbVao = xpOrbMesh[0];
        xpOrbVbo = xpOrbMesh[1];
        xpOrbEbo = xpOrbMesh[2];

        int[] potionMesh = createSpriteMesh(ItemType.POTION);
        potionVao = potionMesh[0];
        potionVbo = potionMesh[1];
        potionEbo = potionMesh[2];

        int[] snowballMesh = createSpriteMesh(ItemType.SNOWBALL);
        snowballVao = snowballMesh[0];
        snowballVbo = snowballMesh[1];
        snowballEbo = snowballMesh[2];

        int[] eggMesh = createSpriteMesh(ItemType.EGG);
        eggVao = eggMesh[0];
        eggVbo = eggMesh[1];
        eggEbo = eggMesh[2];

        int[] fishingHookMesh = createSpriteMesh(ItemType.FISHING_ROD);
        fishingHookVao = fishingHookMesh[0];
        fishingHookVbo = fishingHookMesh[1];
        fishingHookEbo = fishingHookMesh[2];

        MeshData cartData = minecartMeshData();
        int[] cartMesh = createMesh(cartData);
        cartVao = cartMesh[0];
        cartVbo = cartMesh[1];
        cartEbo = cartMesh[2];
        cartIndexCount = cartData.indices().length;

        MeshData chestPayloadData = chestMinecartPayloadMeshData();
        int[] chestPayloadMesh = createMesh(chestPayloadData);
        chestMinecartPayloadVao = chestPayloadMesh[0];
        chestMinecartPayloadVbo = chestPayloadMesh[1];
        chestMinecartPayloadEbo = chestPayloadMesh[2];
        chestMinecartPayloadIndexCount = chestPayloadData.indices().length;

        MeshData furnacePayloadData = furnaceMinecartPayloadMeshData(false);
        int[] furnacePayloadMesh = createMesh(furnacePayloadData);
        furnaceMinecartPayloadVao = furnacePayloadMesh[0];
        furnaceMinecartPayloadVbo = furnacePayloadMesh[1];
        furnaceMinecartPayloadEbo = furnacePayloadMesh[2];
        furnaceMinecartPayloadIndexCount = furnacePayloadData.indices().length;

        MeshData litFurnacePayloadData = furnaceMinecartPayloadMeshData(true);
        int[] litFurnacePayloadMesh = createMesh(litFurnacePayloadData);
        litFurnaceMinecartPayloadVao = litFurnacePayloadMesh[0];
        litFurnaceMinecartPayloadVbo = litFurnacePayloadMesh[1];
        litFurnaceMinecartPayloadEbo = litFurnacePayloadMesh[2];
        litFurnaceMinecartPayloadIndexCount = litFurnacePayloadData.indices().length;

        MeshData boatData = boatMeshData();
        int[] boatMesh = createMesh(boatData);
        boatVao = boatMesh[0];
        boatVbo = boatMesh[1];
        boatEbo = boatMesh[2];
        boatIndexCount = boatData.indices().length;

        int[] tntMesh = createSpriteMesh(ItemType.TNT);
        tntVao = tntMesh[0];
        tntVbo = tntMesh[1];
        tntEbo = tntMesh[2];

        for (PaintingEntity.Art art : PaintingEntity.Art.values()) {
            paintingMeshes.put(art, createPaintingMesh(art));
        }

        try {
            lineShader = new ShaderProgram();
            lineShader.createVertexShader(ShaderProgram.loadResource("/shaders/line.vert"));
            lineShader.createFragmentShader(ShaderProgram.loadResource("/shaders/line.frag"));
            lineShader.link();
            lineShader.createUniform("projectionMatrix");
            lineShader.createUniform("viewMatrix");
            lineShader.createUniform("lineColor");
            fishingLineVao = glGenVertexArrays();
            fishingLineVbo = glGenBuffers();
            glBindVertexArray(fishingLineVao);
            glBindBuffer(GL_ARRAY_BUFFER, fishingLineVbo);
            glBufferData(GL_ARRAY_BUFFER, 6L * Float.BYTES, GL_DYNAMIC_DRAW);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
            glEnableVertexAttribArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize fishing line renderer", e);
        }
    }

    private int[] createSpriteMesh(ItemType itemType) {
        float[] uv = ItemTextureResolver.getUv(itemType);
        return createMesh(new MeshData(new float[] {
                // x, y, z, u, v, nx, ny, nz, r, g, b
                -0.5f, 0.5f, 0.0f, uv[0], uv[1], 0, 0, 1, 1, 1, 1,
                -0.5f, -0.5f, 0.0f, uv[0], uv[3], 0, 0, 1, 1, 1, 1,
                0.5f, -0.5f, 0.0f, uv[2], uv[3], 0, 0, 1, 1, 1, 1,
                0.5f, 0.5f, 0.0f, uv[2], uv[1], 0, 0, 1, 1, 1, 1
        }, new int[] { 0, 1, 2, 2, 3, 0 }));
    }

    private int[] createTerrainSpriteMesh(BlockType blockType, int face) {
        float[] uv = blockType.getTextureCoords(face);
        return createMesh(new MeshData(new float[] {
                -0.5f, 0.5f, 0.0f, uv[0], uv[1], 0, 0, 1, 1, 1, 1,
                -0.5f, -0.5f, 0.0f, uv[0], uv[3], 0, 0, 1, 1, 1, 1,
                0.5f, -0.5f, 0.0f, uv[2], uv[3], 0, 0, 1, 1, 1, 1,
                0.5f, 0.5f, 0.0f, uv[2], uv[1], 0, 0, 1, 1, 1, 1
        }, new int[] { 0, 1, 2, 2, 3, 0 }));
    }

    static float[] fireballSpriteUv() {
        return BlockType.FIRE.getTextureCoords(Block.FACE_NORTH);
    }

    private int[] createFullTextureSpriteMesh() {
        return createMesh(new MeshData(new float[] {
                -0.5f, 0.5f, 0.0f, 0, 0, 0, 0, 1, 1, 1, 1,
                -0.5f, -0.5f, 0.0f, 0, 1, 0, 0, 1, 1, 1, 1,
                0.5f, -0.5f, 0.0f, 1, 1, 0, 0, 1, 1, 1, 1,
                0.5f, 0.5f, 0.0f, 1, 0, 0, 0, 1, 1, 1, 1
        }, new int[] { 0, 1, 2, 2, 3, 0 }));
    }

    private int[] createMesh(MeshData data) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        int ebo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data.vertices(), GL_STATIC_DRAW);
        int stride = 11 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8 * Float.BYTES);
        glEnableVertexAttribArray(3);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, data.indices(), GL_STATIC_DRAW);
        glBindVertexArray(0);
        return new int[] { vao, vbo, ebo };
    }

    private int[] createPaintingMesh(PaintingEntity.Art art) {
        float[] uv = art.uv();
        return createTexturedSpriteMesh(uv[0], uv[1], uv[2], uv[3]);
    }

    private int[] createTexturedSpriteMesh(float u0, float v0, float u1, float v1) {
        return createMesh(new MeshData(new float[] {
                -0.5f, 0.5f, 0.0f, u0, v0, 0, 0, 1, 1, 1, 1,
                -0.5f, -0.5f, 0.0f, u0, v1, 0, 0, 1, 1, 1, 1,
                0.5f, -0.5f, 0.0f, u1, v1, 0, 0, 1, 1, 1, 1,
                0.5f, 0.5f, 0.0f, u1, v0, 0, 0, 1, 1, 1, 1
        }, new int[] { 0, 1, 2, 2, 3, 0 }));
    }

    public void renderAll(List<Entity> entities, Camera camera, Texture itemsTexture, Texture terrainTexture,
            float partialTick) {
        if (entities.isEmpty() || (itemsTexture == null && terrainTexture == null)) {
            return;
        }

        boolean drawing = false;
        int boundVao = 0;
        Texture boundTexture = null;

        for (Entity entity : entities) {
            if (!(entity instanceof ArrowEntity) && !(entity instanceof FireballEntity)
                    && !(entity instanceof EyeOfEnderEntity) && !(entity instanceof EnderPearlEntity)
                    && !(entity instanceof EndCrystalEntity)
                    && !(entity instanceof ExperienceOrbEntity) && !(entity instanceof SplashPotionEntity)
                    && !(entity instanceof ThrownItemEntity)
                    && !(entity instanceof FishingHookEntity)
                    && !(entity instanceof MinecartEntity) && !(entity instanceof BoatEntity)
                    && !(entity instanceof PaintingEntity) && !(entity instanceof PrimedTntEntity)) {
                continue;
            }
            if (isTooFar(camera, entity)) {
                continue;
            }
            if (!drawing) {
                shader.setUniform("alphaCutoff", 0.1f);
                glDisable(GL_CULL_FACE);
                drawing = true;
            }

            int vao = entity instanceof FireballEntity ? fireballVao
                    : entity instanceof EyeOfEnderEntity ? eyeVao
                    : entity instanceof EnderPearlEntity ? pearlVao
                    : entity instanceof EndCrystalEntity ? crystalVao
                    : entity instanceof ExperienceOrbEntity ? xpOrbVao
                    : entity instanceof SplashPotionEntity ? potionVao
                    : entity instanceof ThrownItemEntity thrown
                            ? thrown.getItemType() == ItemType.EGG ? eggVao : snowballVao
                    : entity instanceof FishingHookEntity ? fishingHookVao
                    : entity instanceof MinecartEntity ? cartVao
                    : entity instanceof BoatEntity ? boatVao
                    : entity instanceof PaintingEntity painting ? paintingVao(painting)
                    : entity instanceof PrimedTntEntity ? tntVao : arrowVao;
            Texture texture = entity instanceof FireballEntity
                    ? terrainTexture
                    : entity instanceof EndCrystalEntity
                    ? MobTexture.get("/textures/mob/enderdragon/crystal.png")
                    : entity instanceof ExperienceOrbEntity ? MobTexture.get("/textures/item/xporb.png")
                    : entity instanceof MinecartEntity ? MobTexture.get("/textures/item/cart.png")
                    : entity instanceof BoatEntity ? MobTexture.get("/textures/item/boat.png")
                    : entity instanceof PaintingEntity ? MobTexture.get("/textures/art/kz.png")
                    : itemsTexture;
            if (texture == null) {
                continue;
            }
            if (texture != boundTexture) {
                texture.bind(0);
                boundTexture = texture;
            }
            if (vao != boundVao) {
                glBindVertexArray(vao);
                boundVao = vao;
            }
            float scale = entity instanceof FireballEntity fireball
                    ? (fireball.isExplosive() ? 0.9f : 0.55f)
                    : entity instanceof EyeOfEnderEntity ? 0.45f
                    : entity instanceof EnderPearlEntity ? 0.35f
                    : entity instanceof EndCrystalEntity ? 2.0f
                    : entity instanceof ExperienceOrbEntity ? 0.35f
                    : entity instanceof SplashPotionEntity ? 0.35f
                    : entity instanceof ThrownItemEntity ? 0.35f
                    : entity instanceof FishingHookEntity ? 0.25f
                    : entity instanceof MinecartEntity ? 1.0f
                    : entity instanceof BoatEntity ? 1.0f
                    : entity instanceof PaintingEntity ? 1.0f
                    : entity instanceof PrimedTntEntity ? 0.9f
                    : 0.55f;
            float scaleX = scale;
            float scaleY = scale;
            if (entity instanceof PaintingEntity painting) {
                scaleX = painting.getArt().blocksWide();
                scaleY = painting.getArt().blocksHigh();
            }

            if (entity instanceof EndCrystalEntity crystal) {
                modelMatrix.set(endCrystalModelMatrix(crystal, partialTick));
            } else {
                modelMatrix.identity()
                        .translate(entity.getRenderX(partialTick), entity.getRenderY(partialTick),
                                entity.getRenderZ(partialTick))
                        .rotateY((float) Math.toRadians(-entity.getRenderYaw(partialTick)))
                        .rotateX((float) Math.toRadians(entity.getRenderPitch(partialTick)))
                        .rotateZ((float) Math.toRadians(entity instanceof FireballEntity
                                || entity instanceof ExperienceOrbEntity || entity instanceof SplashPotionEntity
                                || entity instanceof EnderPearlEntity
                                || entity instanceof ThrownItemEntity
                                || entity instanceof MinecartEntity || entity instanceof BoatEntity
                                || entity instanceof PaintingEntity
                                || entity instanceof PrimedTntEntity ? 0.0f : -45.0f))
                        .scale(scaleX, scaleY, scale);
            }

            shader.setUniform("modelMatrix", modelMatrix);
            glDrawElements(GL_TRIANGLES, indexCountFor(entity), GL_UNSIGNED_INT, 0);

            if (entity instanceof MinecartEntity minecart) {
                MinecartPayload payload = minecartPayload(minecart, terrainTexture);
                if (payload != null && payload.texture() != null) {
                    if (payload.texture() != boundTexture) {
                        payload.texture().bind(0);
                        boundTexture = payload.texture();
                    }
                    if (payload.vao() != boundVao) {
                        glBindVertexArray(payload.vao());
                        boundVao = payload.vao();
                    }
                    shader.setUniform("modelMatrix", modelMatrix);
                    glDrawElements(GL_TRIANGLES, payload.indexCount(), GL_UNSIGNED_INT, 0);
                }
            }
        }

        if (drawing) {
            glBindVertexArray(0);
            if (boundTexture != null) {
                boundTexture.unbind();
            }
            glEnable(GL_CULL_FACE);
            shader.setUniform("alphaCutoff", 0.0f);
        }
        renderFishingLines(entities, camera, partialTick);
        renderDragonCrystalBeams(entities, camera, partialTick);
    }

    private void renderFishingLines(List<Entity> entities, Camera camera, float partialTick) {
        if (lineShader == null || fishingLineVao == 0 || fishingLineVbo == 0) {
            return;
        }
        boolean drawing = false;
        for (Entity entity : entities) {
            if (!(entity instanceof FishingHookEntity hook) || isTooFar(camera, hook)) {
                continue;
            }
            FishingLineSegment segment = fishingLineSegment(hook, partialTick);
            if (segment == null) {
                continue;
            }
            if (!drawing) {
                lineShader.bind();
                lineShader.setUniform("projectionMatrix", camera.getProjectionMatrix());
                lineShader.setUniform("viewMatrix", camera.getViewMatrix());
                lineShader.setUniform("lineColor", new Vector4f(0.08f, 0.07f, 0.06f, 1.0f));
                glDisable(GL_CULL_FACE);
                glLineWidth(1.0f);
                glBindVertexArray(fishingLineVao);
                drawing = true;
            }
            float[] vertices = {
                    segment.start().x, segment.start().y, segment.start().z,
                    segment.end().x, segment.end().y, segment.end().z
            };
            glBindBuffer(GL_ARRAY_BUFFER, fishingLineVbo);
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
            glDrawArrays(GL_LINES, 0, 2);
        }
        if (!drawing) {
            return;
        }
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        glEnable(GL_CULL_FACE);
        lineShader.unbind();
        shader.bind();
    }

    private void renderDragonCrystalBeams(List<Entity> entities, Camera camera, float partialTick) {
        if (lineShader == null || fishingLineVao == 0 || fishingLineVbo == 0) {
            return;
        }
        boolean drawing = false;
        for (Entity entity : entities) {
            if (!(entity instanceof EnderDragon dragon) || isTooFar(camera, dragon)) {
                continue;
            }
            List<DragonCrystalBeamSegment> segments = dragonCrystalBeamSegments(dragon, partialTick);
            if (segments.isEmpty()) {
                continue;
            }
            if (!drawing) {
                lineShader.bind();
                lineShader.setUniform("projectionMatrix", camera.getProjectionMatrix());
                lineShader.setUniform("viewMatrix", camera.getViewMatrix());
                lineShader.setUniform("lineColor", DRAGON_BEAM_COLOR);
                glDisable(GL_CULL_FACE);
                glLineWidth(DRAGON_BEAM_WIDTH);
                glBindVertexArray(fishingLineVao);
                drawing = true;
            }
            for (DragonCrystalBeamSegment segment : segments) {
                float[] vertices = {
                        segment.start().x, segment.start().y, segment.start().z,
                        segment.end().x, segment.end().y, segment.end().z
                };
                glBindBuffer(GL_ARRAY_BUFFER, fishingLineVbo);
                glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
                glDrawArrays(GL_LINES, 0, 2);
            }
        }
        if (!drawing) {
            return;
        }
        glLineWidth(1.0f);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        glEnable(GL_CULL_FACE);
        lineShader.unbind();
        shader.bind();
    }

    static FishingLineSegment fishingLineSegment(FishingHookEntity hook, float partialTick) {
        if (hook == null || hook.isRemoved()) {
            return null;
        }
        if (hook.getOwner() == null) {
            FishingHookEntity.OwnerSnapshot ownerSnapshot = hook.getOwnerSnapshot();
            if (ownerSnapshot == null || !ownerSnapshot.alive()) {
                return null;
            }
            return new FishingLineSegment(
                    fishingLineStart(ownerSnapshot),
                    new Vector3f(hook.getRenderX(partialTick), hook.getRenderY(partialTick),
                            hook.getRenderZ(partialTick)));
        }
        return new FishingLineSegment(
                fishingLineStart(hook.getOwner(), partialTick),
                new Vector3f(hook.getRenderX(partialTick), hook.getRenderY(partialTick), hook.getRenderZ(partialTick)));
    }

    static Vector3f fishingLineStart(FishingHookEntity.OwnerSnapshot owner) {
        float yaw = (float) Math.toRadians(owner.yaw());
        float forwardX = (float) Math.sin(yaw);
        float forwardZ = -(float) Math.cos(yaw);
        float rightX = (float) Math.cos(yaw);
        float rightZ = (float) Math.sin(yaw);
        float eye = owner.sneaking() ? FISHING_LINE_SNEAK_EYE_HEIGHT : FISHING_LINE_EYE_HEIGHT;
        return new Vector3f(
                owner.x() + rightX * FISHING_LINE_RIGHT_OFFSET + forwardX * FISHING_LINE_FORWARD_OFFSET,
                owner.y() + eye,
                owner.z() + rightZ * FISHING_LINE_RIGHT_OFFSET + forwardZ * FISHING_LINE_FORWARD_OFFSET);
    }

    static Vector3f fishingLineStart(Player owner, float partialTick) {
        Vector3f previous = owner.getPrevPosition();
        Vector3f current = owner.getPosition();
        float interpX = previous.x + (current.x - previous.x) * partialTick;
        float interpY = previous.y + (current.y - previous.y) * partialTick;
        float interpZ = previous.z + (current.z - previous.z) * partialTick;
        float yaw = (float) Math.toRadians(owner.getRenderYawOffset(partialTick));
        float forwardX = (float) Math.sin(yaw);
        float forwardZ = -(float) Math.cos(yaw);
        float rightX = (float) Math.cos(yaw);
        float rightZ = (float) Math.sin(yaw);
        float eye = owner.isSneaking() ? FISHING_LINE_SNEAK_EYE_HEIGHT : FISHING_LINE_EYE_HEIGHT;
        return new Vector3f(
                interpX + rightX * FISHING_LINE_RIGHT_OFFSET + forwardX * FISHING_LINE_FORWARD_OFFSET,
                interpY + eye,
                interpZ + rightZ * FISHING_LINE_RIGHT_OFFSET + forwardZ * FISHING_LINE_FORWARD_OFFSET);
    }

    static DragonCrystalBeamSegment dragonCrystalBeamSegment(EnderDragon dragon, float partialTick) {
        List<DragonCrystalBeamSegment> segments = dragonCrystalBeamSegments(dragon, partialTick);
        return segments.isEmpty() ? null : segments.get(0);
    }

    static List<DragonCrystalBeamSegment> dragonCrystalBeamSegments(EnderDragon dragon, float partialTick) {
        if (dragon == null || dragon.isRemoved() || dragon.isDead()) {
            return List.of();
        }
        EndCrystalEntity crystal = dragon.getHealingCrystal();
        if (crystal == null || crystal.isRemoved()) {
            return List.of();
        }
        Vector3f start = new Vector3f(
                crystal.getRenderX(partialTick),
                crystal.getRenderY(partialTick) + crystal.getHeight() * 0.5f,
                crystal.getRenderZ(partialTick));
        Vector3f end = new Vector3f(
                dragon.getRenderX(partialTick),
                dragon.getRenderY(partialTick) + dragon.getHeight() * 0.5f,
                dragon.getRenderZ(partialTick));
        List<DragonCrystalBeamSegment> segments = new ArrayList<>(
                1 + DRAGON_BEAM_RING_COUNT * 2 + DRAGON_BEAM_RING_COUNT / 2);
        segments.add(new DragonCrystalBeamSegment(start, end));

        Vector3f axis = new Vector3f(end).sub(start);
        float length = axis.length();
        if (length < 0.001f) {
            return segments;
        }
        axis.div(length);
        Vector3f reference = Math.abs(axis.y) > 0.92f
                ? new Vector3f(1.0f, 0.0f, 0.0f)
                : new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f side = new Vector3f(axis).cross(reference);
        if (side.lengthSquared() < 0.0001f) {
            side.set(1.0f, 0.0f, 0.0f);
        } else {
            side.normalize();
        }
        Vector3f up = new Vector3f(side).cross(axis).normalize();
        float phase = crystal.getRenderInnerRotation(partialTick) * 0.24f;
        Vector3f previousA = null;
        Vector3f previousB = null;
        for (int i = 0; i <= DRAGON_BEAM_RING_COUNT; i++) {
            float t = i / (float) DRAGON_BEAM_RING_COUNT;
            float angle = phase + t * (float) (Math.PI * 3.0);
            float radius = DRAGON_BEAM_RADIUS * (float) Math.sin(Math.PI * t);
            Vector3f pointA = dragonBeamPoint(start, end, side, up, t, angle, radius);
            Vector3f pointB = dragonBeamPoint(start, end, side, up, t, angle + (float) Math.PI, radius);
            if (previousA != null && previousB != null) {
                segments.add(new DragonCrystalBeamSegment(previousA, pointA));
                segments.add(new DragonCrystalBeamSegment(previousB, pointB));
            }
            if (i > 0 && i < DRAGON_BEAM_RING_COUNT && i % 2 == 0) {
                segments.add(new DragonCrystalBeamSegment(pointA, pointB));
            }
            previousA = pointA;
            previousB = pointB;
        }
        return segments;
    }

    private static Vector3f dragonBeamPoint(Vector3f start, Vector3f end, Vector3f side, Vector3f up,
                                            float t, float angle, float radius) {
        return new Vector3f(start)
                .lerp(end, t)
                .add(new Vector3f(side).mul((float) Math.cos(angle) * radius))
                .add(new Vector3f(up).mul((float) Math.sin(angle) * radius));
    }

    record FishingLineSegment(Vector3f start, Vector3f end) {
    }

    record DragonCrystalBeamSegment(Vector3f start, Vector3f end) {
    }

    private static boolean isTooFar(Camera camera, Entity entity) {
        return RenderDistanceCulling.isEntityTooFar(camera, entity, ARROW_RENDER_DISTANCE);
    }

    public void cleanup() {
        deleteMesh(arrowVao, arrowVbo, arrowEbo);
        deleteMesh(fireballVao, fireballVbo, fireballEbo);
        deleteMesh(eyeVao, eyeVbo, eyeEbo);
        deleteMesh(pearlVao, pearlVbo, pearlEbo);
        deleteMesh(crystalVao, crystalVbo, crystalEbo);
        deleteMesh(xpOrbVao, xpOrbVbo, xpOrbEbo);
        deleteMesh(potionVao, potionVbo, potionEbo);
        deleteMesh(snowballVao, snowballVbo, snowballEbo);
        deleteMesh(eggVao, eggVbo, eggEbo);
        deleteMesh(fishingHookVao, fishingHookVbo, fishingHookEbo);
        if (fishingLineVbo != 0) {
            glDeleteBuffers(fishingLineVbo);
        }
        if (fishingLineVao != 0) {
            glDeleteVertexArrays(fishingLineVao);
        }
        if (lineShader != null) {
            lineShader.cleanup();
        }
        deleteMesh(cartVao, cartVbo, cartEbo);
        deleteMesh(chestMinecartPayloadVao, chestMinecartPayloadVbo, chestMinecartPayloadEbo);
        deleteMesh(furnaceMinecartPayloadVao, furnaceMinecartPayloadVbo, furnaceMinecartPayloadEbo);
        deleteMesh(litFurnaceMinecartPayloadVao, litFurnaceMinecartPayloadVbo, litFurnaceMinecartPayloadEbo);
        deleteMesh(boatVao, boatVbo, boatEbo);
        deleteMesh(tntVao, tntVbo, tntEbo);
        for (int[] mesh : paintingMeshes.values()) {
            deleteMesh(mesh[0], mesh[1], mesh[2]);
        }
        paintingMeshes.clear();
    }

    private int paintingVao(PaintingEntity painting) {
        int[] mesh = paintingMeshes.get(painting.getArt());
        return mesh == null ? arrowVao : mesh[0];
    }

    private int indexCountFor(Entity entity) {
        if (entity instanceof EndCrystalEntity) {
            return crystalIndexCount;
        }
        if (entity instanceof MinecartEntity) {
            return cartIndexCount;
        }
        return entity instanceof BoatEntity ? boatIndexCount : 6;
    }

    private MinecartPayload minecartPayload(MinecartEntity minecart, Texture terrainTexture) {
        if (minecart instanceof ChestMinecartEntity) {
            return new MinecartPayload(chestMinecartPayloadVao, chestMinecartPayloadIndexCount,
                    MobTexture.get("/textures/item/chest.png"));
        }
        if (minecart instanceof FurnaceMinecartEntity furnace && terrainTexture != null) {
            boolean lit = furnace.getFuelTicks() > 0;
            return new MinecartPayload(
                    lit ? litFurnaceMinecartPayloadVao : furnaceMinecartPayloadVao,
                    lit ? litFurnaceMinecartPayloadIndexCount : furnaceMinecartPayloadIndexCount,
                    terrainTexture);
        }
        return null;
    }

    static MeshData minecartMeshData() {
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();
        addBox(vertices, indices, -0.46f, 0.00f, -0.46f, 0.46f, 0.12f, 0.46f, cartUv(0, 16, 20, 28));
        addBox(vertices, indices, -0.58f, 0.10f, -0.50f, -0.46f, 0.62f, 0.50f, cartUv(0, 0, 28, 10));
        addBox(vertices, indices, 0.46f, 0.10f, -0.50f, 0.58f, 0.62f, 0.50f, cartUv(0, 0, 28, 10));
        addBox(vertices, indices, -0.46f, 0.10f, -0.58f, 0.46f, 0.62f, -0.46f, cartUv(0, 10, 28, 16));
        addBox(vertices, indices, -0.46f, 0.10f, 0.46f, 0.46f, 0.62f, 0.58f, cartUv(0, 10, 28, 16));
        return new MeshData(toFloatArray(vertices), toIntArray(indices));
    }

    static MeshData chestMinecartPayloadMeshData() {
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();
        addBox(vertices, indices, -0.38f, 0.20f, -0.38f, 0.38f, 0.56f, 0.38f, fullUv());
        addBox(vertices, indices, -0.40f, 0.56f, -0.40f, 0.40f, 0.72f, 0.40f, fullUv());
        return new MeshData(toFloatArray(vertices), toIntArray(indices));
    }

    static MeshData furnaceMinecartPayloadMeshData(boolean lit) {
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();
        BlockType furnaceType = lit ? BlockType.LIT_FURNACE : BlockType.FURNACE;
        addBlockBox(vertices, indices, -0.38f, 0.18f, -0.38f, 0.38f, 0.74f, 0.38f,
                furnaceType, Block.FACE_SOUTH);
        return new MeshData(toFloatArray(vertices), toIntArray(indices));
    }

    static MeshData boatMeshData() {
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();
        addBox(vertices, indices, -0.50f, 0.00f, -0.88f, 0.50f, 0.12f, 0.88f, boatUv(0, 16, 28, 32));
        addBox(vertices, indices, -0.68f, 0.10f, -0.88f, -0.50f, 0.50f, 0.88f, boatUv(0, 0, 28, 8));
        addBox(vertices, indices, 0.50f, 0.10f, -0.88f, 0.68f, 0.50f, 0.88f, boatUv(0, 0, 28, 8));
        addBox(vertices, indices, -0.50f, 0.10f, -1.02f, 0.50f, 0.50f, -0.88f, boatUv(0, 8, 28, 16));
        addBox(vertices, indices, -0.50f, 0.10f, 0.88f, 0.50f, 0.50f, 1.02f, boatUv(0, 8, 28, 16));
        return new MeshData(toFloatArray(vertices), toIntArray(indices));
    }

    static MeshData crystalMeshData() {
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();
        addBox(vertices, indices, -0.45f, 0.00f, -0.45f, 0.45f, 0.18f, 0.45f, fullUv());
        addBox(vertices, indices, -0.52f, 0.46f, -0.52f, 0.52f, 1.50f, 0.52f, fullUv());
        addBox(vertices, indices, -0.36f, 0.62f, -0.36f, 0.36f, 1.34f, 0.36f, fullUv());
        return new MeshData(toFloatArray(vertices), toIntArray(indices));
    }

    static Matrix4f endCrystalModelMatrix(EndCrystalEntity crystal, float partialTick) {
        float animationTicks = crystal == null ? 0.0f : crystal.getRenderInnerRotation(partialTick);
        return new Matrix4f()
                .translate(
                        crystal == null ? 0.0f : crystal.getRenderX(partialTick),
                        (crystal == null ? 0.0f : crystal.getRenderY(partialTick))
                                + endCrystalBobOffset(animationTicks),
                        crystal == null ? 0.0f : crystal.getRenderZ(partialTick))
                .rotateY((float) Math.toRadians(endCrystalRotationDegrees(animationTicks)));
    }

    static float endCrystalRotationDegrees(float animationTicks) {
        return animationTicks * 3.0f;
    }

    static float endCrystalBobOffset(float animationTicks) {
        float wave = (float) Math.sin(animationTicks * 0.2f) * 0.5f + 0.5f;
        return (wave * wave + wave) * 0.2f;
    }

    private static void addBlockBox(ArrayList<Float> vertices, ArrayList<Integer> indices,
            float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
            BlockType type, int metadata) {
        BlockShape.Cuboid box = new BlockShape.Cuboid(0.0f, 0.0f, 0.0f,
                maxX - minX, maxY - minY, maxZ - minZ);
        for (int face = 0; face < 6; face++) {
            float[] faceVertices = Block.getCuboidFaceVertices(face, minX, minY, minZ, box);
            float[] uv = Block.getFaceTexCoords(type, face, metadata);
            float[] normals = Block.getFaceNormals(face);
            int base = vertices.size() / 11;
            for (int i = 0; i < 4; i++) {
                vertices.add(faceVertices[i * 3]);
                vertices.add(faceVertices[i * 3 + 1]);
                vertices.add(faceVertices[i * 3 + 2]);
                vertices.add(uv[i * 2]);
                vertices.add(uv[i * 2 + 1]);
                vertices.add(normals[i * 3]);
                vertices.add(normals[i * 3 + 1]);
                vertices.add(normals[i * 3 + 2]);
                vertices.add(1.0f);
                vertices.add(1.0f);
                vertices.add(1.0f);
            }
            for (int index : Block.getFaceIndices(base)) {
                indices.add(index);
            }
        }
    }

    private static void addBox(ArrayList<Float> vertices, ArrayList<Integer> indices,
            float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float[] uv) {
        addFace(vertices, indices,
                minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ,
                0.0f, 0.0f, 1.0f, uv);
        addFace(vertices, indices,
                maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ,
                0.0f, 0.0f, -1.0f, uv);
        addFace(vertices, indices,
                minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ,
                -1.0f, 0.0f, 0.0f, uv);
        addFace(vertices, indices,
                maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ,
                1.0f, 0.0f, 0.0f, uv);
        addFace(vertices, indices,
                minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ,
                0.0f, 1.0f, 0.0f, uv);
        addFace(vertices, indices,
                minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ,
                0.0f, -1.0f, 0.0f, uv);
    }

    private static void addFace(ArrayList<Float> vertices, ArrayList<Integer> indices,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3,
            float nx, float ny, float nz, float[] uv) {
        int base = vertices.size() / 11;
        addVertex(vertices, x0, y0, z0, uv[0], uv[3], nx, ny, nz);
        addVertex(vertices, x1, y1, z1, uv[2], uv[3], nx, ny, nz);
        addVertex(vertices, x2, y2, z2, uv[2], uv[1], nx, ny, nz);
        addVertex(vertices, x3, y3, z3, uv[0], uv[1], nx, ny, nz);
        indices.add(base);
        indices.add(base + 1);
        indices.add(base + 2);
        indices.add(base + 2);
        indices.add(base + 3);
        indices.add(base);
    }

    private static void addVertex(ArrayList<Float> vertices, float x, float y, float z, float u, float v,
            float nx, float ny, float nz) {
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

    private static float[] boatUv(float x0, float y0, float x1, float y1) {
        return new float[] { x0 / 64.0f, y0 / 32.0f, x1 / 64.0f, y1 / 32.0f };
    }

    private static float[] cartUv(float x0, float y0, float x1, float y1) {
        return new float[] { x0 / 64.0f, y0 / 32.0f, x1 / 64.0f, y1 / 32.0f };
    }

    private static float[] fullUv() {
        return new float[] { 0.0f, 0.0f, 1.0f, 1.0f };
    }

    private static float[] toFloatArray(ArrayList<Float> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    private static int[] toIntArray(ArrayList<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    record MeshData(float[] vertices, int[] indices) {
    }

    private record MinecartPayload(int vao, int indexCount, Texture texture) {
    }

    private void deleteMesh(int vao, int vbo, int ebo) {
        if (vbo != 0) {
            glDeleteBuffers(vbo);
        }
        if (ebo != 0) {
            glDeleteBuffers(ebo);
        }
        if (vao != 0) {
            glDeleteVertexArrays(vao);
        }
    }
}
