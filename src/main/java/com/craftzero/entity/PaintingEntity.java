package com.craftzero.entity;

import com.craftzero.inventory.ItemStack;
import com.craftzero.inventory.ItemType;
import com.craftzero.physics.AABB;
import com.craftzero.world.Block;
import com.craftzero.world.BlockShape;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PaintingEntity extends Entity {
    private static final float THICKNESS = 0.0625f;

    private final Art art;
    private final int facing;

    public PaintingEntity(float centerX, float centerY, float centerZ, int facing, Art art) {
        super(Math.max(1.0f, art == null ? 1.0f : art.blocksWide()), Math.max(1.0f, art == null ? 1.0f : art.blocksHigh()));
        this.art = art == null ? Art.KEBAB : art;
        this.facing = isHorizontalFace(facing) ? facing : Block.FACE_NORTH;
        setPosition(centerX, centerY, centerZ);
        setYaw(yawForFacing(this.facing));
    }

    public static PaintingEntity create(World world, int supportX, int supportY, int supportZ, int facing,
            Random random) {
        if (world == null || !isHorizontalFace(facing)) {
            return null;
        }
        List<Art> fitting = new ArrayList<>();
        for (Art candidate : Art.values()) {
            PaintingEntity painting = fromSupport(supportX, supportY, supportZ, facing, candidate);
            if (painting.canStay(world)) {
                fitting.add(candidate);
            }
        }
        if (fitting.isEmpty()) {
            return null;
        }
        Random rng = random == null ? world.getRandom() : random;
        return fromSupport(supportX, supportY, supportZ, facing, fitting.get(rng.nextInt(fitting.size())));
    }

    public static PaintingEntity fromSupport(int supportX, int supportY, int supportZ, int facing, Art art) {
        int width = art.blocksWide();
        int height = art.blocksHigh();
        int minY = supportY - (height - 1) / 2;
        float centerY = minY + height * 0.5f;
        if (facing == Block.FACE_NORTH || facing == Block.FACE_SOUTH) {
            int minX = supportX - (width - 1) / 2;
            float centerX = minX + width * 0.5f;
            float centerZ = facing == Block.FACE_NORTH ? supportZ - THICKNESS * 0.5f
                    : supportZ + 1.0f + THICKNESS * 0.5f;
            return new PaintingEntity(centerX, centerY, centerZ, facing, art);
        }
        int minZ = supportZ - (width - 1) / 2;
        float centerZ = minZ + width * 0.5f;
        float centerX = facing == Block.FACE_WEST ? supportX - THICKNESS * 0.5f
                : supportX + 1.0f + THICKNESS * 0.5f;
        return new PaintingEntity(centerX, centerY, centerZ, facing, art);
    }

    @Override
    public void updatePhysics(float deltaTime) {
        if (world == null || removed) {
            return;
        }
        if (!canStay(world)) {
            breakAsItem(false);
        }
    }

    @Override
    public AABB getBoundingBox() {
        int width = art.blocksWide();
        int height = art.blocksHigh();
        if (facing == Block.FACE_NORTH || facing == Block.FACE_SOUTH) {
            return new AABB(
                    x - width * 0.5f, y - height * 0.5f, z - THICKNESS * 0.5f,
                    x + width * 0.5f, y + height * 0.5f, z + THICKNESS * 0.5f);
        }
        return new AABB(
                x - THICKNESS * 0.5f, y - height * 0.5f, z - width * 0.5f,
                x + THICKNESS * 0.5f, y + height * 0.5f, z + width * 0.5f);
    }

    public boolean breakAsItem(boolean creative) {
        if (removed) {
            return false;
        }
        if (!creative && world != null) {
            world.spawnThrownStack(x, y, z, new ItemStack(ItemType.PAINTING, 1), 0.0f, 0.1f, 0.0f);
        }
        remove();
        return true;
    }

    public boolean canStay(World world) {
        if (world == null || !isHorizontalFace(facing)) {
            return false;
        }
        int width = art.blocksWide();
        int height = art.blocksHigh();
        int minY = (int) Math.floor(y - height * 0.5f + 0.0001f);
        if (facing == Block.FACE_NORTH || facing == Block.FACE_SOUTH) {
            int minX = (int) Math.floor(x - width * 0.5f + 0.0001f);
            int supportZ = facing == Block.FACE_NORTH ? (int) Math.floor(z + THICKNESS * 0.5f + 0.0001f)
                    : (int) Math.floor(z - THICKNESS * 0.5f - 0.0001f);
            for (int dx = 0; dx < width; dx++) {
                for (int dy = 0; dy < height; dy++) {
                    if (!canSupportPainting(world, minX + dx, minY + dy, supportZ)) {
                        return false;
                    }
                }
            }
        } else {
            int minZ = (int) Math.floor(z - width * 0.5f + 0.0001f);
            int supportX = facing == Block.FACE_WEST ? (int) Math.floor(x + THICKNESS * 0.5f + 0.0001f)
                    : (int) Math.floor(x - THICKNESS * 0.5f - 0.0001f);
            for (int dz = 0; dz < width; dz++) {
                for (int dy = 0; dy < height; dy++) {
                    if (!canSupportPainting(world, supportX, minY + dy, minZ + dz)) {
                        return false;
                    }
                }
            }
        }
        AABB bounds = getBoundingBox();
        return !intersectsBlockCollision(world, bounds)
                && !world.hasEntityIntersecting(bounds, this, true);
    }

    private static boolean canSupportPainting(World world, int x, int y, int z) {
        BlockType support = world.getBlockIfLoaded(x, y, z, BlockType.AIR);
        return BlockShape.canSupportAttached(support);
    }

    private static boolean intersectsBlockCollision(World world, AABB bounds) {
        int minX = (int) Math.floor(bounds.getMin().x);
        int minY = (int) Math.floor(bounds.getMin().y);
        int minZ = (int) Math.floor(bounds.getMin().z);
        int maxX = (int) Math.floor(bounds.getMax().x - 0.0001f);
        int maxY = (int) Math.floor(bounds.getMax().y - 0.0001f);
        int maxZ = (int) Math.floor(bounds.getMax().z - 0.0001f);
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    for (AABB collision : world.getCollisionBoxesIfLoaded(bx, by, bz)) {
                        if (bounds.intersects(collision)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isHorizontalFace(int face) {
        return face == Block.FACE_NORTH || face == Block.FACE_SOUTH
                || face == Block.FACE_EAST || face == Block.FACE_WEST;
    }

    private static float yawForFacing(int face) {
        return switch (face) {
            case Block.FACE_SOUTH -> 0.0f;
            case Block.FACE_NORTH -> 180.0f;
            case Block.FACE_EAST -> -90.0f;
            case Block.FACE_WEST -> 90.0f;
            default -> 0.0f;
        };
    }

    public Art getArt() {
        return art;
    }

    public int getFacing() {
        return facing;
    }

    public enum Art {
        KEBAB("Kebab", 16, 16, 0, 0),
        AZTEC("Aztec", 16, 16, 16, 0),
        ALBAN("Alban", 16, 16, 32, 0),
        AZTEC2("Aztec2", 16, 16, 48, 0),
        BOMB("Bomb", 16, 16, 64, 0),
        PLANT("Plant", 16, 16, 80, 0),
        WASTELAND("Wasteland", 16, 16, 96, 0),
        POOL("Pool", 32, 16, 0, 32),
        COURBET("Courbet", 32, 16, 32, 32),
        SEA("Sea", 32, 16, 64, 32),
        SUNSET("Sunset", 32, 16, 96, 32),
        CREEBET("Creebet", 32, 16, 128, 32),
        WANDERER("Wanderer", 16, 32, 0, 64),
        GRAHAM("Graham", 16, 32, 16, 64),
        MATCH("Match", 32, 32, 0, 128),
        BUST("Bust", 32, 32, 32, 128),
        STAGE("Stage", 32, 32, 64, 128),
        VOID("Void", 32, 32, 96, 128),
        SKULL_AND_ROSES("SkullAndRoses", 32, 32, 128, 128),
        FIGHTERS("Fighters", 64, 32, 0, 96),
        POINTER("Pointer", 64, 64, 0, 192),
        PIGSCENE("Pigscene", 64, 64, 64, 192),
        BURNING_SKULL("BurningSkull", 64, 64, 128, 192),
        SKELETON("Skeleton", 64, 48, 192, 64),
        DONKEY_KONG("DonkeyKong", 64, 48, 192, 112);

        private static final int ATLAS_SIZE = 256;

        private final String motive;
        private final int widthPixels;
        private final int heightPixels;
        private final int textureX;
        private final int textureY;

        Art(String motive, int widthPixels, int heightPixels, int textureX, int textureY) {
            this.motive = motive;
            this.widthPixels = widthPixels;
            this.heightPixels = heightPixels;
            this.textureX = textureX;
            this.textureY = textureY;
        }

        public String motive() {
            return motive;
        }

        public int widthPixels() {
            return widthPixels;
        }

        public int heightPixels() {
            return heightPixels;
        }

        public int blocksWide() {
            return Math.max(1, widthPixels / 16);
        }

        public int blocksHigh() {
            return Math.max(1, heightPixels / 16);
        }

        public float[] uv() {
            float u0 = textureX / (float) ATLAS_SIZE;
            float v0 = textureY / (float) ATLAS_SIZE;
            float u1 = (textureX + widthPixels) / (float) ATLAS_SIZE;
            float v1 = (textureY + heightPixels) / (float) ATLAS_SIZE;
            return new float[] { u0, v0, u1, v1 };
        }

        public static Art fromMotive(String motive) {
            if (motive == null || motive.isBlank()) {
                return KEBAB;
            }
            for (Art art : values()) {
                if (art.motive.equals(motive) || art.name().equalsIgnoreCase(motive)) {
                    return art;
                }
            }
            return KEBAB;
        }
    }
}
