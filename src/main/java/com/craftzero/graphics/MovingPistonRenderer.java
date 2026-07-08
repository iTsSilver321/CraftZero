package com.craftzero.graphics;

import com.craftzero.world.Block;
import com.craftzero.world.BlockShape;
import com.craftzero.world.BlockState;
import com.craftzero.world.BlockType;
import com.craftzero.world.RedstoneEngine;
import com.craftzero.world.VoxelShape;
import com.craftzero.world.World;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.glEnable;

/**
 * Renders piston block events while the real block state is MOVING_PISTON.
 */
public class MovingPistonRenderer {
    private static final float RENDER_DISTANCE = 192.0f;

    private final Renderer renderer;
    private final ShaderProgram shader;
    private final Matrix4f modelMatrix = new Matrix4f();
    private final Map<MeshKey, Mesh> meshes = new HashMap<>();

    public MovingPistonRenderer(Renderer renderer) {
        this.renderer = renderer;
        this.shader = renderer.getShaderProgram();
    }

    public void init() {
    }

    public void render(World world, Camera camera, Texture terrainTexture, float partialTick) {
        if (world == null || terrainTexture == null) {
            return;
        }
        List<World.MovingPistonState> states = world.getMovingPistonStates();
        if (states.isEmpty()) {
            return;
        }

        boolean drawing = false;
        for (World.MovingPistonState state : states) {
            BlockType carried = state.carriedType();
            if (carried == null || carried.isAir()) {
                continue;
            }
            float progress = movingProgress(world, state, partialTick);
            float renderX = lerp(state.fromX(), state.toX(), progress);
            float renderY = lerp(state.fromY(), state.toY(), progress);
            float renderZ = lerp(state.fromZ(), state.toZ(), progress);
            if (isTooFar(camera, renderX, renderY, renderZ)) {
                continue;
            }
            if (!drawing) {
                terrainTexture.bind(0);
                shader.setUniform("alphaCutoff", 0.0f);
                glEnable(GL_CULL_FACE);
                drawing = true;
            }
            int metadata = renderMetadata(state);
            Mesh mesh = meshes.computeIfAbsent(new MeshKey(carried, metadata), this::createBlockMesh);
            modelMatrix.identity().translate(renderX, renderY, renderZ);
            renderer.renderMesh(mesh, modelMatrix);
        }
    }

    private static float movingProgress(World world, World.MovingPistonState state, float partialTick) {
        float tick = world.getBlockTickClock() - state.startTick() + partialTick;
        return Math.min(1.0f, Math.max(0.0f, tick / RedstoneEngine.PISTON_MOVEMENT_TICKS));
    }

    static int renderMetadata(World.MovingPistonState state) {
        if (state == null) {
            return 0;
        }
        if (state.carriedType() != BlockType.PISTON_HEAD) {
            return state.carriedMetadata();
        }
        return (state.carriedMetadata() & ~7) | (state.facing() & 7);
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    private static boolean isTooFar(Camera camera, float x, float y, float z) {
        return RenderDistanceCulling.isPointTooFar(camera, x + 0.5f, y + 0.5f, z + 0.5f, RENDER_DISTANCE);
    }

    private Mesh createBlockMesh(MeshKey key) {
        java.util.ArrayList<Float> positions = new java.util.ArrayList<>();
        java.util.ArrayList<Float> texCoords = new java.util.ArrayList<>();
        java.util.ArrayList<Float> normals = new java.util.ArrayList<>();
        java.util.ArrayList<Float> colors = new java.util.ArrayList<>();
        java.util.ArrayList<Integer> indices = new java.util.ArrayList<>();
        int vertexCount = 0;
        VoxelShape shape = BlockShape.renderShape(
                new BlockState(key.type(), key.metadata()),
                emptyContext());
        for (BlockShape.Cuboid box : shape.boxes()) {
            for (int face = 0; face < 6; face++) {
                float[] faceVerts = Block.getCuboidFaceVertices(face, 0.0f, 0.0f, 0.0f, box);
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

    private static BlockShape.BlockContext emptyContext() {
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
        for (Mesh mesh : meshes.values()) {
            mesh.cleanup();
        }
        meshes.clear();
    }

    private record MeshKey(BlockType type, int metadata) {
    }
}
