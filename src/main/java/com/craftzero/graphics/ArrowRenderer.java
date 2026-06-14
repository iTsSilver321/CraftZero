package com.craftzero.graphics;

import com.craftzero.entity.ArrowEntity;
import com.craftzero.entity.Entity;
import com.craftzero.entity.FireballEntity;
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

    public void renderAll(List<Entity> entities, Camera camera, Texture itemsTexture, float partialTick) {
        if (itemsTexture == null || entities.isEmpty()) {
            return;
        }

        boolean drawing = false;
        int boundVao = 0;

        for (Entity entity : entities) {
            if (!(entity instanceof ArrowEntity) && !(entity instanceof FireballEntity)) {
                continue;
            }
            if (isTooFar(camera, entity)) {
                continue;
            }
            if (!drawing) {
                itemsTexture.bind(0);
                shader.setUniform("alphaCutoff", 0.1f);
                glDisable(GL_CULL_FACE);
                drawing = true;
            }

            int vao = entity instanceof FireballEntity ? fireballVao : arrowVao;
            if (vao != boundVao) {
                glBindVertexArray(vao);
                boundVao = vao;
            }
            float scale = entity instanceof FireballEntity fireball
                    ? (fireball.isExplosive() ? 0.9f : 0.55f)
                    : 0.55f;

            modelMatrix.identity()
                    .translate(entity.getRenderX(partialTick), entity.getRenderY(partialTick),
                            entity.getRenderZ(partialTick))
                    .rotateY((float) Math.toRadians(-entity.getRenderYaw(partialTick)))
                    .rotateX((float) Math.toRadians(entity.getRenderPitch(partialTick)))
                    .rotateZ((float) Math.toRadians(entity instanceof FireballEntity ? 0.0f : -45.0f))
                    .scale(scale, scale, scale);

            shader.setUniform("modelMatrix", modelMatrix);
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        }

        if (!drawing) {
            return;
        }
        glBindVertexArray(0);
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
