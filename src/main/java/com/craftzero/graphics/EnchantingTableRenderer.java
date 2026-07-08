package com.craftzero.graphics;

import com.craftzero.world.World;
import com.craftzero.world.tile.BlockPos;
import com.craftzero.world.tile.EnchantingTableTileEntity;
import com.craftzero.world.tile.TileEntity;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

public class EnchantingTableRenderer {
    private static final float RENDER_DISTANCE = 160.0f;
    private static final float HALF_WIDTH = 0.34f;
    private static final float BOOK_DEPTH = 0.52f;
    private static final float BOOK_THICKNESS = 0.035f;
    private static final float MIN_OPEN_ANGLE = 8.0f;
    private static final float MAX_OPEN_ANGLE = 58.0f;
    private static final float PAGE_WIDTH = 0.58f;
    private static final float PAGE_DEPTH = 0.43f;
    private static final float PAGE_LIFT = 0.026f;
    private static final float PAGE_OPEN_BIAS = 0.86f;

    private Mesh bookHalfMesh;
    private Mesh pageMesh;
    private Texture bookTexture;

    public void init(Texture bookTexture) {
        this.bookHalfMesh = Mesh.createCube(1.0f);
        this.pageMesh = createPageMesh();
        this.bookTexture = bookTexture;
    }

    public void render(Renderer renderer, Camera camera, World world, float partialTick) {
        if (bookHalfMesh == null || bookTexture == null) {
            return;
        }

        boolean drawing = false;
        for (TileEntity tile : world.getTileEntities()) {
            if (!(tile instanceof EnchantingTableTileEntity table)) {
                continue;
            }
            if (isTooFar(camera, table.getPos())) {
                continue;
            }
            if (!drawing) {
                glDisable(GL_CULL_FACE);
                renderer.beginRender(camera);
                bookTexture.bind(0);
                drawing = true;
            }

            renderBook(renderer, table, partialTick);
        }

        if (drawing) {
            bookTexture.unbind();
            renderer.endRender();
            glEnable(GL_CULL_FACE);
        }
    }

    private void renderBook(Renderer renderer, EnchantingTableTileEntity table, float partialTick) {
        renderer.renderMesh(bookHalfMesh, bookHalfModel(table, partialTick, true));
        renderer.renderMesh(bookHalfMesh, bookHalfModel(table, partialTick, false));
        if (pageMesh == null) {
            return;
        }
        float spread = table.getBookSpread(partialTick);
        if (spread <= 0.02f) {
            return;
        }
        renderer.renderMesh(pageMesh, bookPageModel(table, partialTick, -PAGE_OPEN_BIAS, 0.0f));
        renderer.renderMesh(pageMesh, bookPageModel(table, partialTick, PAGE_OPEN_BIAS, 0.0f));
        renderer.renderMesh(pageMesh, bookPageModel(table, partialTick, turningPageBias(table, partialTick, 0.00f),
                PAGE_LIFT));
        renderer.renderMesh(pageMesh, bookPageModel(table, partialTick, turningPageBias(table, partialTick, 0.37f),
                PAGE_LIFT * 1.8f));
    }

    static Matrix4f bookHalfModel(EnchantingTableTileEntity table, float partialTick, boolean left) {
        BlockPos pos = table.getPos();
        float spread = table.getBookSpread(partialTick);
        float rotation = table.getBookRotation(partialTick);
        float bob = bookBob(table, partialTick);
        float openAngle = (float) Math.toRadians(MIN_OPEN_ANGLE + (MAX_OPEN_ANGLE - MIN_OPEN_ANGLE) * spread);
        float hingeRotation = left ? openAngle : -openAngle;
        float halfOffset = left ? -HALF_WIDTH * 0.5f : HALF_WIDTH * 0.5f;

        return new Matrix4f()
                .translate(pos.x() + 0.5f, pos.y() + 0.93f + bob, pos.z() + 0.5f)
                .rotateY(rotation - (float) Math.PI * 0.5f)
                .rotateZ(hingeRotation)
                .translate(halfOffset, 0.0f, 0.0f)
                .scale(HALF_WIDTH, BOOK_THICKNESS, BOOK_DEPTH);
    }

    static Matrix4f bookPageModel(EnchantingTableTileEntity table, float partialTick,
            float sideBias, float lift) {
        BlockPos pos = table.getPos();
        float spread = table.getBookSpread(partialTick);
        float rotation = table.getBookRotation(partialTick);
        float bob = bookBob(table, partialTick);
        float openAngle = (float) Math.toRadians(MIN_OPEN_ANGLE + (MAX_OPEN_ANGLE - MIN_OPEN_ANGLE) * spread);
        float pageAngle = clamp(sideBias, -1.0f, 1.0f) * openAngle;
        float pageRise = (0.006f + lift) * spread;

        return new Matrix4f()
                .translate(pos.x() + 0.5f, pos.y() + 0.956f + bob + pageRise, pos.z() + 0.5f)
                .rotateY(rotation - (float) Math.PI * 0.5f)
                .rotateZ(pageAngle)
                .scale(PAGE_WIDTH, 1.0f, PAGE_DEPTH);
    }

    private static float turningPageBias(EnchantingTableTileEntity table, float partialTick, float offset) {
        float flip = table.getPageFlip(partialTick) + offset;
        float cycle = flip - (float) Math.floor(flip);
        if (cycle < 0.0f) {
            cycle += 1.0f;
        }
        float eased = cycle * cycle * (3.0f - 2.0f * cycle);
        return -PAGE_OPEN_BIAS + PAGE_OPEN_BIAS * 2.0f * eased;
    }

    private static float bookBob(EnchantingTableTileEntity table, float partialTick) {
        return (float) Math.sin((table.getTickCount() + partialTick) * 0.1f) * 0.017f;
    }

    private static Mesh createPageMesh() {
        float u1 = 28.0f / 64.0f;
        float v1 = 0.0f / 32.0f;
        float u2 = 62.0f / 64.0f;
        float v2 = 16.0f / 32.0f;
        float[] positions = {
                -0.5f, 0.0f, -0.5f,
                0.5f, 0.0f, -0.5f,
                0.5f, 0.0f, 0.5f,
                -0.5f, 0.0f, 0.5f
        };
        float[] texCoords = {
                u1, v1,
                u2, v1,
                u2, v2,
                u1, v2
        };
        float[] normals = {
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        };
        float[] colors = {
                1.0f, 0.96f, 0.82f,
                1.0f, 0.96f, 0.82f,
                0.92f, 0.84f, 0.66f,
                0.92f, 0.84f, 0.66f
        };
        int[] indices = { 0, 1, 2, 2, 3, 0 };
        return new Mesh(positions, texCoords, normals, colors, indices);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean isTooFar(Camera camera, BlockPos pos) {
        return RenderDistanceCulling.isBlockTooFar(camera, pos, RENDER_DISTANCE);
    }

    public void cleanup() {
        if (bookHalfMesh != null) {
            bookHalfMesh.cleanup();
            bookHalfMesh = null;
        }
        if (pageMesh != null) {
            pageMesh.cleanup();
            pageMesh = null;
        }
    }
}
