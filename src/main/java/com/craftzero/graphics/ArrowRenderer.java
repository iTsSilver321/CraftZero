package com.craftzero.graphics;

import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.EndCrystalEntity;
import com.craftzero.entity.Entity;
import com.craftzero.entity.ExperienceOrbEntity;
import com.craftzero.entity.EyeOfEnderEntity;
import com.craftzero.entity.FireballEntity;
import com.craftzero.entity.MinecartEntity;
import com.craftzero.entity.PrimedTntEntity;
import com.craftzero.entity.SplashPotionEntity;
import com.craftzero.inventory.ItemType;
import org.joml.Matrix4f;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Renders arrow projectiles with the Release 1.0 item sprite.
 */
public class ArrowRenderer {
    private static final float ARROW_RENDER_DISTANCE = 192.0f;
    private final Renderer renderer;
    private final ShaderProgram shader;
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
    private int crystalVao;
    private int crystalVbo;
    private int crystalEbo;
    private int xpOrbVao;
    private int xpOrbVbo;
    private int xpOrbEbo;
    private int potionVao;
    private int potionVbo;
    private int potionEbo;
    private int cartVao;
    private int cartVbo;
    private int cartEbo;
    private int tntVao;
    private int tntVbo;
    private int tntEbo;

    public ArrowRenderer(Renderer renderer) {
        this.renderer = renderer;
        this.shader = renderer.getShaderProgram();
    }

    public void init() {
        int[] arrowMesh = createSpriteMesh(ItemType.ARROW);
        arrowVao = arrowMesh[0];
        arrowVbo = arrowMesh[1];
        arrowEbo = arrowMesh[2];

        int[] fireballMesh = createSpriteMesh(ItemType.BLAZE_POWDER);
        fireballVao = fireballMesh[0];
        fireballVbo = fireballMesh[1];
        fireballEbo = fireballMesh[2];

        int[] eyeMesh = createSpriteMesh(ItemType.EYE_OF_ENDER);
        eyeVao = eyeMesh[0];
        eyeVbo = eyeMesh[1];
        eyeEbo = eyeMesh[2];

        int[] crystalMesh = createFullTextureSpriteMesh();
        crystalVao = crystalMesh[0];
        crystalVbo = crystalMesh[1];
        crystalEbo = crystalMesh[2];

        int[] xpOrbMesh = createFullTextureSpriteMesh();
        xpOrbVao = xpOrbMesh[0];
        xpOrbVbo = xpOrbMesh[1];
        xpOrbEbo = xpOrbMesh[2];

        int[] potionMesh = createSpriteMesh(ItemType.POTION);
        potionVao = potionMesh[0];
        potionVbo = potionMesh[1];
        potionEbo = potionMesh[2];

        int[] cartMesh = createFullTextureSpriteMesh();
        cartVao = cartMesh[0];
        cartVbo = cartMesh[1];
        cartEbo = cartMesh[2];

        int[] tntMesh = createSpriteMesh(ItemType.TNT);
        tntVao = tntMesh[0];
        tntVbo = tntMesh[1];
        tntEbo = tntMesh[2];
    }

    private int[] createSpriteMesh(ItemType itemType) {
        float[] uv = ItemTextureResolver.getUv(itemType);
        float[] vertices = {
                // x, y, z, u, v, nx, ny, nz, r, g, b
                -0.5f, 0.5f, 0.0f, uv[0], uv[1], 0, 0, 1, 1, 1, 1,
                -0.5f, -0.5f, 0.0f, uv[0], uv[3], 0, 0, 1, 1, 1, 1,
                0.5f, -0.5f, 0.0f, uv[2], uv[3], 0, 0, 1, 1, 1, 1,
                0.5f, 0.5f, 0.0f, uv[2], uv[1], 0, 0, 1, 1, 1, 1
        };
        int[] indices = { 0, 1, 2, 2, 3, 0 };

        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        int ebo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

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
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        glBindVertexArray(0);
        return new int[] { vao, vbo, ebo };
    }

    private int[] createFullTextureSpriteMesh() {
        float[] vertices = {
                -0.5f, 0.5f, 0.0f, 0, 0, 0, 0, 1, 1, 1, 1,
                -0.5f, -0.5f, 0.0f, 0, 1, 0, 0, 1, 1, 1, 1,
                0.5f, -0.5f, 0.0f, 1, 1, 0, 0, 1, 1, 1, 1,
                0.5f, 0.5f, 0.0f, 1, 0, 0, 0, 1, 1, 1, 1
        };
        int[] indices = { 0, 1, 2, 2, 3, 0 };
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        int ebo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
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
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        glBindVertexArray(0);
        return new int[] { vao, vbo, ebo };
    }

    public void renderAll(List<Entity> entities, Camera camera, Texture itemsTexture, float partialTick) {
        if (itemsTexture == null || entities.isEmpty()) {
            return;
        }

        boolean drawing = false;
        int boundVao = 0;
        Texture boundTexture = null;

        for (Entity entity : entities) {
            if (!(entity instanceof ArrowEntity) && !(entity instanceof FireballEntity)
                    && !(entity instanceof EyeOfEnderEntity) && !(entity instanceof EndCrystalEntity)
                    && !(entity instanceof ExperienceOrbEntity) && !(entity instanceof SplashPotionEntity)
                    && !(entity instanceof MinecartEntity) && !(entity instanceof PrimedTntEntity)) {
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
                    : entity instanceof EndCrystalEntity ? crystalVao
                    : entity instanceof ExperienceOrbEntity ? xpOrbVao
                    : entity instanceof SplashPotionEntity ? potionVao
                    : entity instanceof MinecartEntity ? cartVao
                    : entity instanceof PrimedTntEntity ? tntVao : arrowVao;
            Texture texture = entity instanceof EndCrystalEntity
                    ? MobTexture.get("/textures/mob/enderdragon/crystal.png")
                    : entity instanceof ExperienceOrbEntity ? MobTexture.get("/textures/item/xporb.png")
                    : entity instanceof MinecartEntity ? MobTexture.get("/textures/item/cart.png")
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
                    : entity instanceof EndCrystalEntity ? 2.0f
                    : entity instanceof ExperienceOrbEntity ? 0.35f
                    : entity instanceof SplashPotionEntity ? 0.35f
                    : entity instanceof MinecartEntity ? 1.0f
                    : entity instanceof PrimedTntEntity ? 0.9f
                    : 0.55f;

            modelMatrix.identity()
                    .translate(entity.getRenderX(partialTick), entity.getRenderY(partialTick),
                            entity.getRenderZ(partialTick))
                    .rotateY((float) Math.toRadians(-entity.getRenderYaw(partialTick)))
                    .rotateX((float) Math.toRadians(entity.getRenderPitch(partialTick)))
                    .rotateZ((float) Math.toRadians(entity instanceof FireballEntity || entity instanceof EndCrystalEntity
                            || entity instanceof ExperienceOrbEntity || entity instanceof SplashPotionEntity
                            || entity instanceof MinecartEntity || entity instanceof PrimedTntEntity ? 0.0f : -45.0f))
                    .scale(scale, scale, scale);

            shader.setUniform("modelMatrix", modelMatrix);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        }

        if (!drawing) {
            return;
        }
        glBindVertexArray(0);
        if (boundTexture != null) {
            boundTexture.unbind();
        }
        glEnable(GL_CULL_FACE);
        shader.setUniform("alphaCutoff", 0.0f);
    }

    private static boolean isTooFar(Camera camera, Entity entity) {
        float dx = entity.getX() - camera.getPosition().x;
        float dy = entity.getY() - camera.getPosition().y;
        float dz = entity.getZ() - camera.getPosition().z;
        float max = Math.min(camera.getFarPlane(), ARROW_RENDER_DISTANCE);
        return dx * dx + dy * dy + dz * dz > max * max;
    }

    public void cleanup() {
        deleteMesh(arrowVao, arrowVbo, arrowEbo);
        deleteMesh(fireballVao, fireballVbo, fireballEbo);
        deleteMesh(eyeVao, eyeVbo, eyeEbo);
        deleteMesh(crystalVao, crystalVbo, crystalEbo);
        deleteMesh(xpOrbVao, xpOrbVbo, xpOrbEbo);
        deleteMesh(potionVao, potionVbo, potionEbo);
        deleteMesh(cartVao, cartVbo, cartEbo);
        deleteMesh(tntVao, tntVbo, tntEbo);
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
