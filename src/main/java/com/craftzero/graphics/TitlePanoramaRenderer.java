package com.craftzero.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * Lightweight title-screen panorama. It renders a fixed Minecraft-style voxel
 * scene with a slow orbiting camera, then re-renders it with tiny camera jitters
 * to imitate the softened classic menu panorama without loading a full world.
 */
public final class TitlePanoramaRenderer {
    private static final int ATLAS_CELLS = 16;
    private static final float CELL = 1.0f / ATLAS_CELLS;
    private static final float UV_INSET = 0.5f / 256.0f;

    private static final int AIR = 0;
    private static final int STONE = 1;
    private static final int DIRT = 2;
    private static final int GRASS = 3;
    private static final int SAND = 4;
    private static final int LOG = 5;
    private static final int LEAVES = 6;
    private static final int WATER = 7;

    private static final int TEX_GRASS_TOP = 0;
    private static final int TEX_STONE = 1;
    private static final int TEX_DIRT = 2;
    private static final int TEX_GRASS_SIDE = 3;
    private static final int TEX_SAND = 18;
    private static final int TEX_LOG_TOP = 21;
    private static final int TEX_LOG_SIDE = 20;
    private static final int TEX_LEAVES = 53;
    private static final int TEX_WATER = 205;

    private static final int SIZE_X = 52;
    private static final int SIZE_Y = 24;
    private static final int SIZE_Z = 52;
    private static final int OFFSET_X = SIZE_X / 2;
    private static final int OFFSET_Z = SIZE_Z / 2;
    private static final int WATER_LEVEL = 7;

    private ShaderProgram shader;
    private Texture terrainTexture;
    private Mesh sceneMesh;

    public void init() throws Exception {
        shader = new ShaderProgram();
        shader.createVertexShader("""
                #version 330 core
                layout (location = 0) in vec3 aPos;
                layout (location = 1) in vec2 aTexCoord;
                layout (location = 2) in vec3 aNormal;
                layout (location = 3) in vec3 aColor;
                out vec2 texCoord;
                out vec3 vertexColor;
                out float visibility;
                uniform mat4 projectionMatrix;
                uniform mat4 viewMatrix;
                uniform mat4 modelMatrix;
                uniform float fogDensity;
                void main() {
                    vec4 worldPosition = modelMatrix * vec4(aPos, 1.0);
                    vec4 cameraPosition = viewMatrix * worldPosition;
                    gl_Position = projectionMatrix * cameraPosition;
                    texCoord = aTexCoord;
                    vertexColor = aColor;
                    float distance = length(cameraPosition.xyz);
                    visibility = clamp(exp(-pow(distance * fogDensity, 2.0)), 0.0, 1.0);
                }
                """);
        shader.createFragmentShader("""
                #version 330 core
                in vec2 texCoord;
                in vec3 vertexColor;
                in float visibility;
                out vec4 fragColor;
                uniform sampler2D textureSampler;
                uniform vec4 tint;
                uniform vec3 fogColor;
                uniform float alphaCutoff;
                void main() {
                    vec4 textureColor = texture(textureSampler, texCoord);
                    if (textureColor.a <= alphaCutoff) {
                        discard;
                    }
                    vec3 color = mix(fogColor, textureColor.rgb * vertexColor, visibility);
                    fragColor = vec4(color, textureColor.a) * tint;
                }
                """);
        shader.link();
        shader.createUniform("projectionMatrix");
        shader.createUniform("viewMatrix");
        shader.createUniform("modelMatrix");
        shader.createUniform("textureSampler");
        shader.createUniform("tint");
        shader.createUniform("fogColor");
        shader.createUniform("fogDensity");
        shader.createUniform("alphaCutoff");

        terrainTexture = new Texture("/textures/terrain/Terrain.png");
        sceneMesh = buildSceneMesh();
    }

    public void render(int width, int height, float time) {
        if (shader == null || terrainTexture == null || sceneMesh == null) {
            return;
        }

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader.bind();
        terrainTexture.bind(0);
        shader.setUniform("textureSampler", 0);
        shader.setUniform("modelMatrix", new Matrix4f());
        shader.setUniform("fogColor", new Vector3f(0.63f, 0.69f, 0.61f));
        shader.setUniform("fogDensity", 0.022f);
        shader.setUniform("alphaCutoff", 0.08f);

        float aspect = width <= 0 || height <= 0 ? 16.0f / 9.0f : (float) width / (float) height;
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(70.0f), aspect, 0.1f, 140.0f);
        shader.setUniform("projectionMatrix", projection);

        float[] jitters = { 0.0f, 0.010f, -0.010f, 0.018f, -0.018f, 0.027f, -0.027f };
        float[] alphas = { 0.52f, 0.15f, 0.15f, 0.10f, 0.10f, 0.065f, 0.065f };
        for (int i = 0; i < jitters.length; i++) {
            glClear(GL_DEPTH_BUFFER_BIT);
            shader.setUniform("viewMatrix", viewMatrix(time, jitters[i]));
            shader.setUniform("tint", new Vector4f(1.0f, 1.0f, 1.0f, alphas[i]));
            sceneMesh.render();
        }

        terrainTexture.unbind();
        shader.unbind();

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
    }

    private Matrix4f viewMatrix(float time, float jitter) {
        float angle = time * 0.055f + jitter;
        float radius = 34.0f;
        float camX = (float) Math.sin(angle) * radius;
        float camZ = (float) Math.cos(angle) * radius;
        float camY = 13.2f + (float) Math.sin(time * 0.07f) * 0.7f;
        Vector3f eye = new Vector3f(camX, camY, camZ);
        Vector3f center = new Vector3f(
                (float) Math.sin(angle + 0.9f) * 4.5f,
                8.2f + (float) Math.sin(time * 0.045f) * 0.45f,
                (float) Math.cos(angle + 0.9f) * 4.5f);
        return new Matrix4f().lookAt(eye, center, new Vector3f(0, 1, 0));
    }

    private Mesh buildSceneMesh() {
        byte[][][] blocks = new byte[SIZE_X][SIZE_Y][SIZE_Z];
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                int wx = x - OFFSET_X;
                int wz = z - OFFSET_Z;
                int height = terrainHeight(wx, wz);
                boolean beach = height <= WATER_LEVEL + 1;
                for (int y = 0; y <= height && y < SIZE_Y; y++) {
                    if (beach) {
                        blocks[x][y][z] = (byte) (y >= height - 2 ? SAND : STONE);
                    } else if (y == height) {
                        blocks[x][y][z] = GRASS;
                    } else if (y >= height - 3) {
                        blocks[x][y][z] = DIRT;
                    } else {
                        blocks[x][y][z] = STONE;
                    }
                }
                if (height < WATER_LEVEL) {
                    for (int y = height + 1; y <= WATER_LEVEL && y < SIZE_Y; y++) {
                        blocks[x][y][z] = WATER;
                    }
                }
            }
        }

        placeTree(blocks, -15, 9);
        placeTree(blocks, -8, -5);
        placeTree(blocks, 4, 8);
        placeTree(blocks, 12, -8);
        placeTree(blocks, 18, 5);
        placeTree(blocks, -20, -13);

        MeshData mesh = new MeshData();
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = 0; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    int block = blocks[x][y][z] & 0xFF;
                    if (block == AIR) {
                        continue;
                    }
                    addVisibleFaces(mesh, blocks, x, y, z, block);
                }
            }
        }
        return mesh.toMesh();
    }

    private int terrainHeight(int x, int z) {
        double hill = Math.sin(x * 0.23) * 2.6
                + Math.cos(z * 0.27) * 2.1
                + Math.sin((x + z) * 0.13) * 2.8;
        double valley = Math.exp(-Math.pow((x + 15) / 10.0, 2.0) - Math.pow((z + 12) / 8.0, 2.0)) * 6.0;
        double shore = Math.exp(-Math.pow((x - 16) / 12.0, 2.0) - Math.pow((z - 7) / 10.0, 2.0)) * 4.0;
        int height = 8 + (int) Math.round(hill - valley - shore);
        return Math.max(4, Math.min(SIZE_Y - 7, height));
    }

    private void placeTree(byte[][][] blocks, int worldX, int worldZ) {
        int x = worldX + OFFSET_X;
        int z = worldZ + OFFSET_Z;
        if (x < 3 || x >= SIZE_X - 3 || z < 3 || z >= SIZE_Z - 3) {
            return;
        }
        int ground = highestSolid(blocks, x, z);
        if (ground < WATER_LEVEL || ground + 7 >= SIZE_Y) {
            return;
        }
        for (int y = ground + 1; y <= ground + 5; y++) {
            blocks[x][y][z] = LOG;
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 3; dy <= 6; dy++) {
                    int distance = Math.abs(dx) + Math.abs(dz);
                    if (dy == 6 && distance > 1) {
                        continue;
                    }
                    if (dy == 3 && Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                        continue;
                    }
                    int bx = x + dx;
                    int by = ground + dy;
                    int bz = z + dz;
                    if (blocks[bx][by][bz] == AIR) {
                        blocks[bx][by][bz] = LEAVES;
                    }
                }
            }
        }
    }

    private int highestSolid(byte[][][] blocks, int x, int z) {
        for (int y = SIZE_Y - 1; y >= 0; y--) {
            int block = blocks[x][y][z] & 0xFF;
            if (block != AIR && block != WATER && block != LEAVES) {
                return y;
            }
        }
        return -1;
    }

    private void addVisibleFaces(MeshData mesh, byte[][][] blocks, int x, int y, int z, int block) {
        float wx = x - OFFSET_X;
        float wy = y;
        float wz = z - OFFSET_Z;
        if (visibleNeighbor(blocks, x, y, z + 1, block)) {
            addFace(mesh, block, Face.SOUTH, wx, wy, wz, 0.80f);
        }
        if (visibleNeighbor(blocks, x, y, z - 1, block)) {
            addFace(mesh, block, Face.NORTH, wx, wy, wz, 0.66f);
        }
        if (visibleNeighbor(blocks, x + 1, y, z, block)) {
            addFace(mesh, block, Face.EAST, wx, wy, wz, 0.74f);
        }
        if (visibleNeighbor(blocks, x - 1, y, z, block)) {
            addFace(mesh, block, Face.WEST, wx, wy, wz, 0.74f);
        }
        if (visibleNeighbor(blocks, x, y + 1, z, block)) {
            addFace(mesh, block, Face.UP, wx, wy, wz, 0.98f);
        }
        if (visibleNeighbor(blocks, x, y - 1, z, block)) {
            addFace(mesh, block, Face.DOWN, wx, wy, wz, 0.52f);
        }
    }

    private boolean visibleNeighbor(byte[][][] blocks, int x, int y, int z, int current) {
        if (x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z) {
            return true;
        }
        int neighbor = blocks[x][y][z] & 0xFF;
        if (neighbor == AIR) {
            return true;
        }
        if (current == WATER) {
            return neighbor != WATER;
        }
        return current == LEAVES ? neighbor != LEAVES : neighbor == WATER || neighbor == LEAVES;
    }

    private void addFace(MeshData mesh, int block, Face face, float x, float y, float z, float shade) {
        int texture = textureFor(block, face);
        Uv uv = uv(texture);
        float tintR = shade;
        float tintG = shade;
        float tintB = shade;
        if (block == GRASS && face == Face.UP) {
            tintR *= 0.66f;
            tintG *= 0.90f;
            tintB *= 0.54f;
        } else if (block == LEAVES) {
            tintR *= 0.54f;
            tintG *= 0.78f;
            tintB *= 0.46f;
        } else if (block == WATER) {
            tintR *= 0.46f;
            tintG *= 0.66f;
            tintB *= 1.0f;
        }
        mesh.addQuad(face.vertices(x, y, z), face.normal, uv, tintR, tintG, tintB);
    }

    private int textureFor(int block, Face face) {
        return switch (block) {
            case STONE -> TEX_STONE;
            case DIRT -> TEX_DIRT;
            case GRASS -> face == Face.UP ? TEX_GRASS_TOP : face == Face.DOWN ? TEX_DIRT : TEX_GRASS_SIDE;
            case SAND -> TEX_SAND;
            case LOG -> face == Face.UP || face == Face.DOWN ? TEX_LOG_TOP : TEX_LOG_SIDE;
            case LEAVES -> TEX_LEAVES;
            case WATER -> TEX_WATER;
            default -> TEX_STONE;
        };
    }

    private Uv uv(int textureIndex) {
        int cellX = Math.floorMod(textureIndex, ATLAS_CELLS);
        int cellY = Math.floorDiv(textureIndex, ATLAS_CELLS);
        return new Uv(
                cellX * CELL + UV_INSET,
                cellY * CELL + UV_INSET,
                (cellX + 1) * CELL - UV_INSET,
                (cellY + 1) * CELL - UV_INSET);
    }

    public void cleanup() {
        if (sceneMesh != null) {
            sceneMesh.cleanup();
        }
        if (terrainTexture != null) {
            terrainTexture.cleanup();
        }
        if (shader != null) {
            shader.cleanup();
        }
    }

    private enum Face {
        SOUTH(new float[] { 0, 0, 1 }, new float[][] {
                { 0, 0, 1 }, { 1, 0, 1 }, { 1, 1, 1 }, { 0, 1, 1 }
        }),
        NORTH(new float[] { 0, 0, -1 }, new float[][] {
                { 1, 0, 0 }, { 0, 0, 0 }, { 0, 1, 0 }, { 1, 1, 0 }
        }),
        EAST(new float[] { 1, 0, 0 }, new float[][] {
                { 1, 0, 1 }, { 1, 0, 0 }, { 1, 1, 0 }, { 1, 1, 1 }
        }),
        WEST(new float[] { -1, 0, 0 }, new float[][] {
                { 0, 0, 0 }, { 0, 0, 1 }, { 0, 1, 1 }, { 0, 1, 0 }
        }),
        UP(new float[] { 0, 1, 0 }, new float[][] {
                { 0, 1, 1 }, { 1, 1, 1 }, { 1, 1, 0 }, { 0, 1, 0 }
        }),
        DOWN(new float[] { 0, -1, 0 }, new float[][] {
                { 0, 0, 0 }, { 1, 0, 0 }, { 1, 0, 1 }, { 0, 0, 1 }
        });

        private final float[] normal;
        private final float[][] corners;

        Face(float[] normal, float[][] corners) {
            this.normal = normal;
            this.corners = corners;
        }

        float[][] vertices(float x, float y, float z) {
            float[][] vertices = new float[4][3];
            for (int i = 0; i < 4; i++) {
                vertices[i][0] = x + corners[i][0];
                vertices[i][1] = y + corners[i][1];
                vertices[i][2] = z + corners[i][2];
            }
            return vertices;
        }
    }

    private record Uv(float u1, float v1, float u2, float v2) {
    }

    private static final class MeshData {
        private final List<Float> positions = new ArrayList<>();
        private final List<Float> texCoords = new ArrayList<>();
        private final List<Float> normals = new ArrayList<>();
        private final List<Float> colors = new ArrayList<>();
        private final List<Integer> indices = new ArrayList<>();

        void addQuad(float[][] vertices, float[] normal, Uv uv, float r, float g, float b) {
            int base = positions.size() / 3;
            float[][] uvs = {
                    { uv.u1, uv.v2 },
                    { uv.u2, uv.v2 },
                    { uv.u2, uv.v1 },
                    { uv.u1, uv.v1 }
            };
            for (int i = 0; i < 4; i++) {
                positions.add(vertices[i][0]);
                positions.add(vertices[i][1]);
                positions.add(vertices[i][2]);
                texCoords.add(uvs[i][0]);
                texCoords.add(uvs[i][1]);
                normals.add(normal[0]);
                normals.add(normal[1]);
                normals.add(normal[2]);
                colors.add(r);
                colors.add(g);
                colors.add(b);
            }
            indices.add(base);
            indices.add(base + 1);
            indices.add(base + 2);
            indices.add(base + 2);
            indices.add(base + 3);
            indices.add(base);
        }

        Mesh toMesh() {
            return new Mesh(toFloatArray(positions), toFloatArray(texCoords),
                    toFloatArray(normals), toFloatArray(colors), toIntArray(indices));
        }

        private static float[] toFloatArray(List<Float> values) {
            float[] array = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                array[i] = values.get(i);
            }
            return array;
        }

        private static int[] toIntArray(List<Integer> values) {
            int[] array = new int[values.size()];
            for (int i = 0; i < values.size(); i++) {
                array[i] = values.get(i);
            }
            return array;
        }
    }
}
