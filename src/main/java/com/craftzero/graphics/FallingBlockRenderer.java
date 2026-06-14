package com.craftzero.graphics;

import com.craftzero.entity.Entity;
import com.craftzero.entity.FallingBlockEntity;
import com.craftzero.world.Block;
import com.craftzero.world.BlockShape;
import com.craftzero.world.BlockType;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.glEnable;

/**
 * Renders falling sand/gravel as textured block cubes.
 */
public class FallingBlockRenderer {
    private static final float FALLING_BLOCK_RENDER_DISTANCE = 192.0f;

    private final Renderer renderer;
    private final ShaderProgram shader;
    private final Matrix4f modelMatrix = new Matrix4f();
    private final Map<BlockType, Mesh> meshes = new HashMap<>();

    public FallingBlockRenderer(Renderer renderer) {
        this.renderer = renderer;
        this.shader = renderer.getShaderProgram();
    }

    public void init() {
    }

    public void renderAll(List<Entity> entities, Camera camera, Texture terrainTexture, float partialTick) {
        if (terrainTexture == null || entities.isEmpty()) {
            return;
        }
        boolean drawing = false;

        for (Entity entity : entities) {
            if (!(entity instanceof FallingBlockEntity falling)) {
                continue;
            }
            if (isTooFar(camera, falling)) {
                continue;
            }
            if (!drawing) {
                terrainTexture.bind(0);
                shader.setUniform("alphaCutoff", 0.0f);
                glEnable(GL_CULL_FACE);
                drawing = true;
            }
            Mesh mesh = meshes.computeIfAbsent(falling.getBlockType(), this::createBlockMesh);
            modelMatrix.identity()
                    .translate(falling.getRenderX(partialTick), falling.getRenderY(partialTick),
                            falling.getRenderZ(partialTick));
            renderer.renderMesh(mesh, modelMatrix);
        }
    }

    private static boolean isTooFar(Camera camera, Entity entity) {
        float dx = entity.getX() - camera.getPosition().x;
        float dy = entity.getY() - camera.getPosition().y;
        float dz = entity.getZ() - camera.getPosition().z;
        float max = Math.min(camera.getFarPlane(), FALLING_BLOCK_RENDER_DISTANCE);
        return dx * dx + dy * dy + dz * dz > max * max;
    }

    private Mesh createBlockMesh(BlockType type) {
        java.util.ArrayList<Float> positions = new java.util.ArrayList<>();
        java.util.ArrayList<Float> texCoords = new java.util.ArrayList<>();
        java.util.ArrayList<Float> normals = new java.util.ArrayList<>();
        java.util.ArrayList<Float> colors = new java.util.ArrayList<>();
        java.util.ArrayList<Integer> indices = new java.util.ArrayList<>();
        BlockShape.Cuboid cube = new BlockShape.Cuboid(0, 0, 0, 1, 1, 1);
        int vertexCount = 0;
        for (int face = 0; face < 6; face++) {
            float[] faceVerts = Block.getCuboidFaceVertices(face, -0.5f, 0.0f, -0.5f, cube);
            for (float v : faceVerts) {
                positions.add(v);
            }
            float[] uv = Block.getFaceTexCoords(type, face, 0);
            for (float t : uv) {
                texCoords.add(t);
            }
            float[] faceNormals = Block.getFaceNormals(face);
            for (float n : faceNormals) {
                normals.add(n);
            }
            for (int i = 0; i < 4; i++) {
                colors.add(1.0f);
                colors.add(1.0f);
                colors.add(1.0f);
            }
            for (int idx : Block.getFaceIndices(vertexCount)) {
                indices.add(idx);
            }
            vertexCount += 4;
        }
        return new Mesh(toFloatArray(positions), toFloatArray(texCoords), toFloatArray(normals),
                toFloatArray(colors), toIntArray(indices));
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
}
