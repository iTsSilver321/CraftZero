package com.craftzero.graphics;

import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.MobDefinition;
import com.craftzero.entity.mob.MobFactory;
import com.craftzero.world.World;
import com.craftzero.world.tile.BlockPos;
import com.craftzero.world.tile.MonsterSpawnerTileEntity;
import com.craftzero.world.tile.TileEntity;

import java.util.EnumMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

public class MobSpawnerRenderer {
    static final float BASE_PREVIEW_SCALE = 0.4375f;
    private static final float RENDER_DISTANCE = 160.0f;
    private static final float PREVIEW_Y_OFFSET = 0.1f;

    private final MobRenderer mobRenderer;
    private final Map<MobDefinition, Mob> previewMobs = new EnumMap<>(MobDefinition.class);

    public MobSpawnerRenderer(MobRenderer mobRenderer) {
        this.mobRenderer = mobRenderer;
    }

    public void render(World world, Camera camera, float partialTick, Texture terrainAtlas) {
        if (world == null || mobRenderer == null) {
            return;
        }

        boolean drawing = false;
        for (TileEntity tile : world.getTileEntities()) {
            if (!(tile instanceof MonsterSpawnerTileEntity spawner)) {
                continue;
            }
            BlockPos pos = spawner.getPos();
            if (isTooFar(camera, pos)) {
                continue;
            }
            Mob preview = previewMobs.computeIfAbsent(spawner.getMobDefinition(), MobFactory::create);
            if (preview == null) {
                continue;
            }
            if (!drawing) {
                glDisable(GL_CULL_FACE);
                drawing = true;
            }

            preview.setWorld(world);
            preview.setPosition(pos.x() + 0.5f, pos.y() + PREVIEW_Y_OFFSET, pos.z() + 0.5f);
            preview.setRenderBodyYaw(spawner.getRenderRotation(partialTick));
            preview.setTicksExisted((int) (world.getBlockTickClock() & Integer.MAX_VALUE));
            mobRenderer.renderScaled(preview, camera, partialTick, terrainAtlas, previewScale(preview));
        }

        if (drawing) {
            glEnable(GL_CULL_FACE);
        }
    }

    static float previewScale(Mob mob) {
        if (mob == null) {
            return BASE_PREVIEW_SCALE;
        }
        float maxDimension = Math.max(mob.getWidth(), mob.getHeight());
        return maxDimension > 1.0f ? BASE_PREVIEW_SCALE / maxDimension : BASE_PREVIEW_SCALE;
    }

    private static boolean isTooFar(Camera camera, BlockPos pos) {
        return RenderDistanceCulling.isBlockTooFar(camera, pos, RENDER_DISTANCE);
    }

    public void cleanup() {
        previewMobs.clear();
    }
}
