package com.craftzero.graphics;

import com.craftzero.world.World;
import com.craftzero.world.tile.BlockPos;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.TileEntity;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

public class ChestRenderer {
    private static final float CHEST_RENDER_DISTANCE = 160.0f;

    private Mesh cubeMesh;
    private Texture chestTexture;
    private Texture largeChestTexture;

    public void init(Texture chestTexture, Texture largeChestTexture) {
        this.cubeMesh = Mesh.createCube(1.0f);
        this.chestTexture = chestTexture;
        this.largeChestTexture = largeChestTexture;
    }

    public void render(Renderer renderer, Camera camera, World world, float partialTick) {
        if (cubeMesh == null || chestTexture == null) {
            return;
        }

        glDisable(GL_CULL_FACE);
        renderer.beginRender(camera);
        for (TileEntity tile : world.getTileEntities()) {
            if (!(tile instanceof ChestTileEntity chest)) {
                continue;
            }
            if (isTooFar(camera, chest.getPos())) {
                continue;
            }
            ChestTileEntity adjacent = world.getAdjacentChest(chest);
            if (adjacent != null && comesBefore(adjacent, chest)) {
                continue;
            }

            Texture texture = adjacent != null && largeChestTexture != null ? largeChestTexture : chestTexture;
            texture.bind(0);
            renderChest(renderer, chest, adjacent, partialTick);
            texture.unbind();
        }
        renderer.endRender();
        glEnable(GL_CULL_FACE);
    }

    private boolean isTooFar(Camera camera, BlockPos pos) {
        float dx = pos.x() + 0.5f - camera.getPosition().x;
        float dy = pos.y() + 0.5f - camera.getPosition().y;
        float dz = pos.z() + 0.5f - camera.getPosition().z;
        float max = Math.min(camera.getFarPlane(), CHEST_RENDER_DISTANCE);
        return dx * dx + dy * dy + dz * dz > max * max;
    }

    private void renderChest(Renderer renderer, ChestTileEntity chest, ChestTileEntity adjacent, float partialTick) {
        BlockPos pos = chest.getPos();
        float width = 0.875f;
        float depth = 0.875f;
        float cx = pos.x() + 0.5f;
        float cz = pos.z() + 0.5f;

        if (adjacent != null) {
            int dx = adjacent.getPos().x() - pos.x();
            int dz = adjacent.getPos().z() - pos.z();
            if (dx != 0) {
                width = 1.875f;
                cx = Math.min(pos.x(), adjacent.getPos().x()) + 1.0f;
            } else if (dz != 0) {
                depth = 1.875f;
                cz = Math.min(pos.z(), adjacent.getPos().z()) + 1.0f;
            }
        }

        Matrix4f base = new Matrix4f()
                .translate(cx, pos.y() + 0.3125f, cz)
                .scale(width, 0.625f, depth);
        renderer.renderMesh(cubeMesh, base);

        float lid = adjacent != null
                ? Math.max(chest.getLidAngle(partialTick), adjacent.getLidAngle(partialTick))
                : chest.getLidAngle(partialTick);
        float angle = (float) Math.toRadians(-65.0f * lid);
        Matrix4f lidModel = new Matrix4f()
                .translate(cx, pos.y() + 0.625f, cz - depth * 0.5f)
                .rotateX(angle)
                .translate(0.0f, 0.125f, depth * 0.5f)
                .scale(width, 0.25f, depth);
        renderer.renderMesh(cubeMesh, lidModel);
    }

    private boolean comesBefore(ChestTileEntity a, ChestTileEntity b) {
        if (a.getPos().z() != b.getPos().z()) {
            return a.getPos().z() < b.getPos().z();
        }
        return a.getPos().x() < b.getPos().x();
    }

    public void cleanup() {
        if (cubeMesh != null) {
            cubeMesh.cleanup();
            cubeMesh = null;
        }
    }
}
