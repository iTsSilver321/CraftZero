package com.craftzero.graphics;

import com.craftzero.world.World;
import com.craftzero.world.Block;
import com.craftzero.world.BlockShape;
import com.craftzero.world.tile.BlockPos;
import com.craftzero.world.tile.ChestTileEntity;
import com.craftzero.world.tile.TileEntity;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class ChestRenderer {
    private static final float CHEST_RENDER_DISTANCE = 160.0f;
    private static final float MAX_LID_ANGLE_RADIANS = (float) (Math.PI / 2.0);
    private static final float LATCH_WIDTH = 2.0f / 16.0f;
    private static final float LATCH_HEIGHT = 4.0f / 16.0f;
    private static final float LATCH_DEPTH = 1.0f / 16.0f;
    private static final ChestBoxSpec SINGLE_BODY_SPEC = new ChestBoxSpec(0, 19, 14, 10, 14, 64, 64);
    private static final ChestBoxSpec SINGLE_LID_SPEC = new ChestBoxSpec(0, 0, 14, 5, 14, 64, 64);
    private static final ChestBoxSpec SINGLE_LATCH_SPEC = new ChestBoxSpec(0, 0, 2, 4, 1, 64, 64);
    private static final ChestBoxSpec LARGE_BODY_SPEC = new ChestBoxSpec(0, 19, 30, 10, 14, 128, 64);
    private static final ChestBoxSpec LARGE_LID_SPEC = new ChestBoxSpec(0, 0, 30, 5, 14, 128, 64);
    private static final ChestBoxSpec LARGE_LATCH_SPEC = new ChestBoxSpec(0, 0, 2, 4, 1, 128, 64);

    private ChestMeshes singleChestMeshes;
    private ChestMeshes largeChestMeshes;
    private Texture chestTexture;
    private Texture largeChestTexture;

    public void init(Texture chestTexture, Texture largeChestTexture) {
        this.singleChestMeshes = new ChestMeshes(
                createChestMesh(singleBodyMeshData()),
                createChestMesh(singleLidMeshData()),
                createChestMesh(singleLatchMeshData()));
        this.largeChestMeshes = new ChestMeshes(
                createChestMesh(largeBodyMeshData()),
                createChestMesh(largeLidMeshData()),
                createChestMesh(largeLatchMeshData()));
        this.chestTexture = chestTexture;
        this.largeChestTexture = largeChestTexture;
    }

    public void render(Renderer renderer, Camera camera, World world, float partialTick) {
        if (singleChestMeshes == null || chestTexture == null) {
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
            ChestMeshes meshes = adjacent != null ? largeChestMeshes : singleChestMeshes;
            renderChest(renderer, world, chest, adjacent, partialTick, meshes);
            texture.unbind();
        }
        renderer.endRender();
        glEnable(GL_CULL_FACE);
    }

    private boolean isTooFar(Camera camera, BlockPos pos) {
        return RenderDistanceCulling.isBlockTooFar(camera, pos, CHEST_RENDER_DISTANCE);
    }

    private void renderChest(Renderer renderer, World world, ChestTileEntity chest, ChestTileEntity adjacent,
            float partialTick, ChestMeshes meshes) {
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
        renderer.renderMesh(meshes.body(), base);

        float lid = adjacent != null
                ? Math.max(chest.getLidAngle(partialTick), adjacent.getLidAngle(partialTick))
                : chest.getLidAngle(partialTick);
        float angle = lidRotationRadians(lid);
        int facing = world.getBlockMetadataIfLoaded(pos.x(), pos.y(), pos.z(), 0);
        Matrix4f lidModel = lidModelForFacing(facing, cx, pos.y() + 0.625f, cz, width, depth, angle);
        renderer.renderMesh(meshes.lid(), lidModel);

        Matrix4f latchModel = latchModelForFacing(facing, cx, pos.y() + 0.625f, cz, width, depth, angle);
        renderer.renderMesh(meshes.latch(), latchModel);
    }

    static float lidRotationRadians(float lidOpenAmount) {
        float clamped = Math.max(0.0f, Math.min(1.0f, lidOpenAmount));
        float closed = 1.0f - clamped;
        float eased = 1.0f - closed * closed * closed;
        return eased * MAX_LID_ANGLE_RADIANS;
    }

    private Matrix4f lidModelForFacing(int facing, float cx, float y, float cz, float width, float depth, float angle) {
        return switch (facing) {
            case Block.FACE_NORTH -> new Matrix4f()
                    .translate(cx, y, cz + depth * 0.5f)
                    .rotateX(angle)
                    .translate(0.0f, 0.125f, -depth * 0.5f)
                    .scale(width, 0.25f, depth);
            case Block.FACE_WEST -> new Matrix4f()
                    .translate(cx + width * 0.5f, y, cz)
                    .rotateZ(-angle)
                    .translate(-width * 0.5f, 0.125f, 0.0f)
                    .scale(width, 0.25f, depth);
            case Block.FACE_EAST -> new Matrix4f()
                    .translate(cx - width * 0.5f, y, cz)
                    .rotateZ(angle)
                    .translate(width * 0.5f, 0.125f, 0.0f)
                    .scale(width, 0.25f, depth);
            case Block.FACE_SOUTH -> new Matrix4f()
                    .translate(cx, y, cz - depth * 0.5f)
                    .rotateX(-angle)
                    .translate(0.0f, 0.125f, depth * 0.5f)
                    .scale(width, 0.25f, depth);
            default -> new Matrix4f()
                    .translate(cx, y, cz - depth * 0.5f)
                    .rotateX(-angle)
                    .translate(0.0f, 0.125f, depth * 0.5f)
                    .scale(width, 0.25f, depth);
        };
    }

    static Matrix4f latchModelForFacing(int facing, float cx, float y, float cz, float width, float depth, float angle) {
        return switch (facing) {
            case Block.FACE_NORTH -> new Matrix4f()
                    .translate(cx, y, cz + depth * 0.5f)
                    .rotateX(angle)
                    .translate(0.0f, 0.0f, -depth - LATCH_DEPTH * 0.5f)
                    .scale(LATCH_WIDTH, LATCH_HEIGHT, LATCH_DEPTH);
            case Block.FACE_WEST -> new Matrix4f()
                    .translate(cx + width * 0.5f, y, cz)
                    .rotateZ(-angle)
                    .translate(-width - LATCH_DEPTH * 0.5f, 0.0f, 0.0f)
                    .scale(LATCH_DEPTH, LATCH_HEIGHT, LATCH_WIDTH);
            case Block.FACE_EAST -> new Matrix4f()
                    .translate(cx - width * 0.5f, y, cz)
                    .rotateZ(angle)
                    .translate(width + LATCH_DEPTH * 0.5f, 0.0f, 0.0f)
                    .scale(LATCH_DEPTH, LATCH_HEIGHT, LATCH_WIDTH);
            case Block.FACE_SOUTH -> new Matrix4f()
                    .translate(cx, y, cz - depth * 0.5f)
                    .rotateX(-angle)
                    .translate(0.0f, 0.0f, depth + LATCH_DEPTH * 0.5f)
                    .scale(LATCH_WIDTH, LATCH_HEIGHT, LATCH_DEPTH);
            default -> new Matrix4f()
                    .translate(cx, y, cz - depth * 0.5f)
                    .rotateX(-angle)
                    .translate(0.0f, 0.0f, depth + LATCH_DEPTH * 0.5f)
                    .scale(LATCH_WIDTH, LATCH_HEIGHT, LATCH_DEPTH);
        };
    }

    private boolean comesBefore(ChestTileEntity a, ChestTileEntity b) {
        if (a.getPos().z() != b.getPos().z()) {
            return a.getPos().z() < b.getPos().z();
        }
        return a.getPos().x() < b.getPos().x();
    }

    static ChestBoxSpec singleBodyTextureBox() {
        return SINGLE_BODY_SPEC;
    }

    static ChestBoxSpec singleLidTextureBox() {
        return SINGLE_LID_SPEC;
    }

    static ChestBoxSpec singleLatchTextureBox() {
        return SINGLE_LATCH_SPEC;
    }

    static ChestBoxSpec largeBodyTextureBox() {
        return LARGE_BODY_SPEC;
    }

    static ChestBoxSpec largeLidTextureBox() {
        return LARGE_LID_SPEC;
    }

    static ChestBoxSpec largeLatchTextureBox() {
        return LARGE_LATCH_SPEC;
    }

    static ChestMeshData singleBodyMeshData() {
        return createChestMeshData(SINGLE_BODY_SPEC);
    }

    static ChestMeshData singleLidMeshData() {
        return createChestMeshData(SINGLE_LID_SPEC);
    }

    static ChestMeshData singleLatchMeshData() {
        return createChestMeshData(SINGLE_LATCH_SPEC);
    }

    static ChestMeshData largeBodyMeshData() {
        return createChestMeshData(LARGE_BODY_SPEC);
    }

    static ChestMeshData largeLidMeshData() {
        return createChestMeshData(LARGE_LID_SPEC);
    }

    static ChestMeshData largeLatchMeshData() {
        return createChestMeshData(LARGE_LATCH_SPEC);
    }

    private static Mesh createChestMesh(ChestMeshData data) {
        return new Mesh(data.positions(), data.texCoords(), data.normals(), data.colors(), data.indices());
    }

    private static ChestMeshData createChestMeshData(ChestBoxSpec spec) {
        ArrayList<Float> positions = new ArrayList<>();
        ArrayList<Float> texCoords = new ArrayList<>();
        ArrayList<Float> normals = new ArrayList<>();
        ArrayList<Float> colors = new ArrayList<>();
        ArrayList<Integer> indices = new ArrayList<>();
        BlockShape.Cuboid cube = new BlockShape.Cuboid(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);
        int vertexCount = 0;

        for (int face = 0; face < 6; face++) {
            float[] faceVertices = Block.getCuboidFaceVertices(face, 0.0f, 0.0f, 0.0f, cube);
            for (float vertex : faceVertices) {
                positions.add(vertex);
            }
            float[] uv = uvForFace(spec, face);
            for (float coord : uv) {
                texCoords.add(coord);
            }
            float[] faceNormals = Block.getFaceNormals(face);
            for (float normal : faceNormals) {
                normals.add(normal);
            }
            for (int i = 0; i < 4; i++) {
                colors.add(1.0f);
                colors.add(1.0f);
                colors.add(1.0f);
            }
            for (int index : Block.getFaceIndices(vertexCount)) {
                indices.add(index);
            }
            vertexCount += 4;
        }

        return new ChestMeshData(toFloatArray(positions), toFloatArray(texCoords), toFloatArray(normals),
                toFloatArray(colors), toIntArray(indices));
    }

    static float[] uvRectForFace(ChestBoxSpec spec, int face) {
        int u = spec.textureU();
        int v = spec.textureV();
        int width = spec.width();
        int height = spec.height();
        int depth = spec.depth();

        return switch (face) {
            case Block.FACE_TOP -> normalizeRect(u + depth, v, u + depth + width, v + depth, spec);
            case Block.FACE_BOTTOM -> normalizeRect(u + depth + width, v,
                    u + depth + width + width, v + depth, spec);
            case Block.FACE_NORTH -> normalizeRect(u + depth, v + depth,
                    u + depth + width, v + depth + height, spec);
            case Block.FACE_SOUTH -> normalizeRect(u + depth + width + depth, v + depth,
                    u + depth + width + depth + width, v + depth + height, spec);
            case Block.FACE_EAST -> normalizeRect(u + depth + width, v + depth,
                    u + depth + width + depth, v + depth + height, spec);
            case Block.FACE_WEST -> normalizeRect(u, v + depth, u + depth, v + depth + height, spec);
            default -> normalizeRect(u + depth, v + depth, u + depth + width, v + depth + height, spec);
        };
    }

    private static float[] uvForFace(ChestBoxSpec spec, int face) {
        float[] rect = uvRectForFace(spec, face);
        return new float[] {
                rect[0], rect[1],
                rect[0], rect[3],
                rect[2], rect[3],
                rect[2], rect[1]
        };
    }

    private static float[] normalizeRect(int x0, int y0, int x1, int y1, ChestBoxSpec spec) {
        return new float[] {
                x0 / (float) spec.textureWidth(),
                y0 / (float) spec.textureHeight(),
                x1 / (float) spec.textureWidth(),
                y1 / (float) spec.textureHeight()
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
        cleanup(singleChestMeshes);
        cleanup(largeChestMeshes);
        singleChestMeshes = null;
        largeChestMeshes = null;
    }

    private static void cleanup(ChestMeshes meshes) {
        if (meshes == null) {
            return;
        }
        meshes.body().cleanup();
        meshes.lid().cleanup();
        meshes.latch().cleanup();
    }

    record ChestBoxSpec(int textureU, int textureV, int width, int height, int depth,
            int textureWidth, int textureHeight) {
    }

    record ChestMeshData(float[] positions, float[] texCoords, float[] normals, float[] colors, int[] indices) {
    }

    private record ChestMeshes(Mesh body, Mesh lid, Mesh latch) {
    }
}
