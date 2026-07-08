package com.craftzero.graphics;

import com.craftzero.world.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Random;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

/**
 * Renders the continuous camera-centered rain/snow sheets used during active weather.
 */
public class PrecipitationRenderer {
    static final int MAX_CURTAIN_RADIUS = 10;
    static final float MIN_RENDER_STRENGTH = 0.02f;
    static final float RAIN_HALF_WIDTH = 0.09f;
    static final float RAIN_HALF_HEIGHT = 6.0f;
    static final float SNOW_HALF_WIDTH = 0.42f;
    static final float SNOW_HALF_HEIGHT = 5.0f;
    static final float RAIN_CENTER_Y_BIAS = 1.0f;
    static final float SNOW_CENTER_Y_BIAS = 0.6f;
    private static final float RAIN_FALL_BLOCKS_PER_TICK = 0.35f;
    private static final float SNOW_FALL_BLOCKS_PER_TICK = 0.08f;
    private static final float RAIN_PHASE_RANDOM_TICKS = 32.0f;
    private static final float SNOW_PHASE_RANDOM_TICKS = 64.0f;
    private static final PrecipitationColumnStyle NEUTRAL_COLUMN_STYLE =
            new PrecipitationColumnStyle(0.0f, 1.0f, 1.0f, 1.0f);
    private static final Vector3f WHITE_TINT = new Vector3f(1.0f, 1.0f, 1.0f);

    private final Renderer renderer;
    private final Matrix4f modelMatrix = new Matrix4f();
    private Texture rainTexture;
    private Texture snowTexture;
    private Mesh rainMesh;
    private Mesh snowMesh;

    public PrecipitationRenderer(Renderer renderer) {
        this.renderer = renderer;
    }

    public void init() throws Exception {
        rainTexture = new Texture("/textures/environment/rain.png");
        snowTexture = new Texture("/textures/environment/snow.png");
        rainMesh = createWeatherStrip(RAIN_HALF_WIDTH, RAIN_HALF_HEIGHT);
        snowMesh = createWeatherStrip(SNOW_HALF_WIDTH, SNOW_HALF_HEIGHT);
    }

    public void render(World world, Camera camera, float partialTick) {
        if (world == null || camera == null || rainTexture == null || snowTexture == null) {
            return;
        }
        float rainStrength = clamp01(world.getRainStrength(partialTick));
        if (rainStrength < MIN_RENDER_STRENGTH) {
            return;
        }

        Vector3f cameraPosition = camera.getPosition();
        int radius = curtainRadius(rainStrength);
        int baseX = (int) Math.floor(cameraPosition.x);
        int baseY = (int) Math.floor(cameraPosition.y);
        int baseZ = (int) Math.floor(cameraPosition.z);
        float tick = Math.max(0.0f, world.getBlockTickClock() + clamp01(partialTick));

        boolean began = false;
        Texture currentTexture = null;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (!withinCurtainRadius(dx, dz, radius)) {
                    continue;
                }
                int x = baseX + dx;
                int z = baseZ + dz;
                PrecipitationType type = precipitationAt(world, x, baseY, z);
                if (type == PrecipitationType.NONE) {
                    continue;
                }

                if (!began) {
                    begin();
                    began = true;
                }

                Texture texture = type == PrecipitationType.SNOW ? snowTexture : rainTexture;
                if (texture != currentTexture) {
                    texture.bind(0);
                    currentTexture = texture;
                }
                Mesh mesh = type == PrecipitationType.SNOW ? snowMesh : rainMesh;
                PrecipitationColumnStyle style = columnStyle(x, z, type);
                float offset = type == PrecipitationType.SNOW
                        ? verticalOffset(tick, PrecipitationType.SNOW, style)
                        : verticalOffset(tick, PrecipitationType.RAIN, style);
                stripMatrix(x, z, cameraPosition, offset, type, style, modelMatrix);
                renderer.renderMesh(mesh, modelMatrix);
            }
        }

        if (began) {
            if (currentTexture != null) {
                currentTexture.unbind();
            }
            end();
        }
    }

    private void begin() {
        glDisable(GL_CULL_FACE);
        renderer.setAlphaCutoff(0.02f);
        renderer.setEntityBrightness(1.0f);
        renderer.setEntityTint(WHITE_TINT);
    }

    private void end() {
        renderer.setEntityTint(WHITE_TINT);
        renderer.setEntityBrightness(0.0f);
        renderer.setAlphaCutoff(0.0f);
        glEnable(GL_CULL_FACE);
    }

    static PrecipitationType precipitationAt(World world, int x, int y, int z) {
        if (world == null) {
            return PrecipitationType.NONE;
        }
        if (world.isSnowingAt(x, y, z)) {
            return PrecipitationType.SNOW;
        }
        if (world.isRainingAt(x, y, z)) {
            return PrecipitationType.RAIN;
        }
        return PrecipitationType.NONE;
    }

    static int curtainRadius(float rainStrength) {
        return Math.max(2, Math.round(MAX_CURTAIN_RADIUS * clamp01(rainStrength)));
    }

    static boolean withinCurtainRadius(int dx, int dz, int radius) {
        int clampedRadius = Math.max(0, radius);
        return dx * dx + dz * dz <= clampedRadius * clampedRadius;
    }

    static float verticalOffset(float tick, PrecipitationType type) {
        return verticalOffset(tick, type, NEUTRAL_COLUMN_STYLE);
    }

    static float verticalOffset(float tick, PrecipitationType type, PrecipitationColumnStyle style) {
        PrecipitationColumnStyle safeStyle = style == null ? NEUTRAL_COLUMN_STYLE : style;
        float speed = type == PrecipitationType.SNOW ? SNOW_FALL_BLOCKS_PER_TICK : RAIN_FALL_BLOCKS_PER_TICK;
        float phasedTick = Math.max(0.0f, tick + safeStyle.phaseTicks());
        return positiveModulo(phasedTick * speed * safeStyle.speedScale(), 1.0f);
    }

    static Matrix4f stripMatrix(int x, int z, Vector3f cameraPosition, float verticalOffset,
            PrecipitationType type) {
        return stripMatrix(x, z, cameraPosition, verticalOffset, type, NEUTRAL_COLUMN_STYLE, new Matrix4f());
    }

    static Matrix4f stripMatrix(int x, int z, Vector3f cameraPosition, float verticalOffset,
            PrecipitationType type, PrecipitationColumnStyle style) {
        return stripMatrix(x, z, cameraPosition, verticalOffset, type, style, new Matrix4f());
    }

    private static Matrix4f stripMatrix(int x, int z, Vector3f cameraPosition, float verticalOffset,
            PrecipitationType type, PrecipitationColumnStyle style, Matrix4f dest) {
        PrecipitationColumnStyle safeStyle = style == null ? NEUTRAL_COLUMN_STYLE : style;
        float centerX = x + 0.5f;
        float centerZ = z + 0.5f;
        float centerY = cameraPosition.y + centerYBias(type) - verticalOffset;
        return dest.identity()
                .translate(centerX, centerY, centerZ)
                .rotateY(columnYawRadians(centerX, centerZ, cameraPosition))
                .scale(safeStyle.widthScale(), safeStyle.heightScale(), 1.0f);
    }

    static float columnYawRadians(float centerX, float centerZ, Vector3f cameraPosition) {
        return (float) Math.atan2(cameraPosition.x - centerX, cameraPosition.z - centerZ);
    }

    private static float centerYBias(PrecipitationType type) {
        return type == PrecipitationType.SNOW ? SNOW_CENTER_Y_BIAS : RAIN_CENTER_Y_BIAS;
    }

    static PrecipitationColumnStyle columnStyle(int x, int z, PrecipitationType type) {
        Random random = new Random(precipitationColumnSeed(x, z));
        if (type == PrecipitationType.SNOW) {
            return new PrecipitationColumnStyle(
                    random.nextFloat() * SNOW_PHASE_RANDOM_TICKS,
                    0.72f + random.nextFloat() * 0.32f,
                    0.88f + random.nextFloat() * 0.26f,
                    0.90f + random.nextFloat() * 0.22f);
        }
        return new PrecipitationColumnStyle(
                random.nextFloat() * RAIN_PHASE_RANDOM_TICKS,
                0.84f + random.nextFloat() * 0.32f,
                0.72f + random.nextFloat() * 0.36f,
                0.86f + random.nextFloat() * 0.28f);
    }

    private static int precipitationColumnSeed(int x, int z) {
        return x * x * 3121 + x * 45238971 ^ z * z * 418711 + z * 13761;
    }

    private static Mesh createWeatherStrip(float halfWidth, float halfHeight) {
        float[] positions = {
                -halfWidth, halfHeight, 0.0f,
                -halfWidth, -halfHeight, 0.0f,
                halfWidth, -halfHeight, 0.0f,
                halfWidth, halfHeight, 0.0f
        };
        float[] texCoords = {
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f
        };
        int[] indices = { 0, 1, 2, 2, 3, 0 };
        return new Mesh(positions, texCoords, indices);
    }

    private static float positiveModulo(float value, float modulo) {
        float result = value % modulo;
        return result < 0.0f ? result + modulo : result;
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    public void cleanup() {
        if (rainMesh != null) {
            rainMesh.cleanup();
        }
        if (snowMesh != null) {
            snowMesh.cleanup();
        }
        if (rainTexture != null) {
            rainTexture.cleanup();
        }
        if (snowTexture != null) {
            snowTexture.cleanup();
        }
    }

    enum PrecipitationType {
        NONE,
        RAIN,
        SNOW
    }

    record PrecipitationColumnStyle(float phaseTicks, float speedScale, float widthScale, float heightScale) {
        PrecipitationColumnStyle {
            phaseTicks = Math.max(0.0f, phaseTicks);
            speedScale = Math.max(0.01f, speedScale);
            widthScale = Math.max(0.01f, widthScale);
            heightScale = Math.max(0.01f, heightScale);
        }
    }
}
