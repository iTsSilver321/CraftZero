package com.craftzero.graphics;

import com.craftzero.world.BlockType;
import com.craftzero.world.World;
import com.craftzero.world.tile.BlockPos;
import com.craftzero.world.tile.SignTileEntity;
import com.craftzero.world.tile.TileEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

/**
 * Renders Release-era sign tile text onto standing and wall signs in-world.
 */
public class SignTextRenderer {
    static final float GLYPH_WIDTH = 0.04f;
    static final float GLYPH_HEIGHT = 0.065f;
    static final float FIRST_LINE_Y = 0.125f;
    static final float LINE_SPACING = 0.085f;
    static final float STANDING_SIGN_TEXT_Y = 0.72f;
    static final float STANDING_SIGN_FACE_OFFSET = 1.0f / 16.0f + 0.006f;
    static final float WALL_SIGN_TEXT_Y = 0.53f;
    static final float WALL_SIGN_FACE_EPSILON = 0.006f;
    private static final int FONT_GRID = 16;
    private static final float MAX_RENDER_DISTANCE = 64.0f;
    private static final Vector3f TEXT_TINT = new Vector3f(0.0f, 0.0f, 0.0f);
    private static final Vector3f WHITE_TINT = new Vector3f(1.0f, 1.0f, 1.0f);

    private final Renderer renderer;
    private final Matrix4f modelMatrix = new Matrix4f();
    private Mesh[] glyphMeshes;
    private Texture fontTexture;

    public SignTextRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public void init() throws Exception {
        fontTexture = new Texture("/textures/font/default.png");
        glyphMeshes = new Mesh[256];
        for (int i = 0; i < glyphMeshes.length; i++) {
            glyphMeshes[i] = createGlyphMesh(i);
        }
    }

    public void render(World world, Camera camera) {
        if (world == null || camera == null || fontTexture == null || glyphMeshes == null) {
            return;
        }

        boolean began = false;
        for (TileEntity tile : world.getTileEntities()) {
            if (!(tile instanceof SignTileEntity sign) || !hasVisibleText(sign)) {
                continue;
            }
            BlockPos pos = sign.getPos();
            BlockType block = world.getBlockIfLoaded(pos.x(), pos.y(), pos.z(), BlockType.AIR);
            if (block != BlockType.STANDING_SIGN && block != BlockType.WALL_SIGN) {
                continue;
            }
            if (isTooFar(camera, pos)) {
                continue;
            }
            if (!began) {
                begin();
                began = true;
            }
            renderSign(sign, block, world.getBlockMetadataIfLoaded(pos.x(), pos.y(), pos.z(), 0));
        }

        if (began) {
            end();
        }
    }

    private void begin() {
        glDisable(GL_CULL_FACE);
        renderer.setAlphaCutoff(0.05f);
        renderer.setEntityBrightness(1.0f);
        renderer.setEntityTint(TEXT_TINT);
        fontTexture.bind(0);
    }

    private void end() {
        fontTexture.unbind();
        renderer.setEntityTint(WHITE_TINT);
        renderer.setEntityBrightness(0.0f);
        renderer.setAlphaCutoff(0.0f);
        glEnable(GL_CULL_FACE);
    }

    private void renderSign(SignTileEntity sign, BlockType block, int metadata) {
        Matrix4f base = block == BlockType.WALL_SIGN
                ? wallSignBaseMatrix(sign.getPos(), metadata)
                : standingSignBaseMatrix(sign.getPos(), metadata);
        String[] lines = sign.getLines();
        for (int line = 0; line < lines.length; line++) {
            renderLine(base, lines[line], line);
        }
    }

    private void renderLine(Matrix4f base, String text, int line) {
        if (text == null || text.isEmpty()) {
            return;
        }
        float startX = lineStartX(text);
        float y = lineY(line);
        for (int i = 0; i < text.length(); i++) {
            int glyph = text.charAt(i);
            if (glyph < 0 || glyph >= glyphMeshes.length) {
                continue;
            }
            modelMatrix.set(base)
                    .translate(startX + i * GLYPH_WIDTH, y, 0.0f)
                    .scale(GLYPH_WIDTH, GLYPH_HEIGHT, 1.0f);
            renderer.renderMesh(glyphMeshes[glyph], modelMatrix);
        }
    }

    private static boolean hasVisibleText(SignTileEntity sign) {
        for (String line : sign.getLines()) {
            if (line != null && !line.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTooFar(Camera camera, BlockPos pos) {
        return RenderDistanceCulling.isBlockTooFar(camera, pos, MAX_RENDER_DISTANCE);
    }

    static Matrix4f standingSignBaseMatrix(BlockPos pos, int metadata) {
        float angle = (metadata & 15) * (float) (Math.PI * 2.0 / 16.0);
        return new Matrix4f()
                .translate(pos.x() + 0.5f, pos.y() + STANDING_SIGN_TEXT_Y, pos.z() + 0.5f)
                .rotateY(angle)
                .translate(0.0f, 0.0f, -STANDING_SIGN_FACE_OFFSET);
    }

    static Matrix4f wallSignBaseMatrix(BlockPos pos, int metadata) {
        return switch (metadata & 7) {
            case 3 -> new Matrix4f()
                    .translate(pos.x() + 0.5f, pos.y() + WALL_SIGN_TEXT_Y,
                            pos.z() + 0.125f + WALL_SIGN_FACE_EPSILON);
            case 4 -> new Matrix4f()
                    .translate(pos.x() + 0.875f - WALL_SIGN_FACE_EPSILON,
                            pos.y() + WALL_SIGN_TEXT_Y, pos.z() + 0.5f)
                    .rotateY((float) Math.toRadians(-90.0));
            case 5 -> new Matrix4f()
                    .translate(pos.x() + 0.125f + WALL_SIGN_FACE_EPSILON,
                            pos.y() + WALL_SIGN_TEXT_Y, pos.z() + 0.5f)
                    .rotateY((float) Math.toRadians(90.0));
            case 2 -> new Matrix4f()
                    .translate(pos.x() + 0.5f, pos.y() + WALL_SIGN_TEXT_Y,
                            pos.z() + 0.875f - WALL_SIGN_FACE_EPSILON)
                    .rotateY((float) Math.toRadians(180.0));
            default -> new Matrix4f()
                    .translate(pos.x() + 0.5f, pos.y() + WALL_SIGN_TEXT_Y,
                            pos.z() + 0.875f - WALL_SIGN_FACE_EPSILON)
                    .rotateY((float) Math.toRadians(180.0));
        };
    }

    static float lineStartX(String text) {
        int length = text == null ? 0 : text.length();
        return -length * GLYPH_WIDTH * 0.5f;
    }

    static float lineY(int line) {
        return FIRST_LINE_Y - line * LINE_SPACING;
    }

    static float[] glyphUv(int glyph) {
        int index = Math.floorMod(glyph, 256);
        float cell = 1.0f / FONT_GRID;
        int x = index % FONT_GRID;
        int y = index / FONT_GRID;
        return new float[] {
                x * cell,
                y * cell,
                (x + 1) * cell,
                (y + 1) * cell
        };
    }

    private static Mesh createGlyphMesh(int glyph) {
        float[] uv = glyphUv(glyph);
        float[] positions = {
                0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f,
                1.0f, 0.0f, 0.0f,
                1.0f, 1.0f, 0.0f
        };
        float[] texCoords = {
                uv[0], uv[1],
                uv[0], uv[3],
                uv[2], uv[3],
                uv[2], uv[1]
        };
        int[] indices = { 0, 1, 2, 2, 3, 0 };
        return new Mesh(positions, texCoords, indices);
    }

    public void cleanup() {
        if (glyphMeshes != null) {
            for (Mesh mesh : glyphMeshes) {
                if (mesh != null) {
                    mesh.cleanup();
                }
            }
        }
        if (fontTexture != null) {
            fontTexture.cleanup();
        }
    }
}
