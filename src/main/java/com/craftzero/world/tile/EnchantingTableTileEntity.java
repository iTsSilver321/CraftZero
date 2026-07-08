package com.craftzero.world.tile;

import com.craftzero.main.Player;
import com.craftzero.world.BlockType;
import com.craftzero.world.World;

import java.util.Random;

public class EnchantingTableTileEntity extends TileEntity {
    public static final float BOOK_SPREAD_STEP = 0.1f;
    public static final double PLAYER_RANGE = 3.0;
    private static final int GLYPH_PARTICLE_CHANCE = 4;

    private final Random random;
    private final Random particleRandom;
    private int tickCount;
    private float pageFlip;
    private float prevPageFlip;
    private float pageFlipTarget;
    private float pageFlipVelocity;
    private float bookSpread;
    private float prevBookSpread;
    private float bookRotation;
    private float bookRotation2;
    private float prevBookRotation;
    private float tickAccumulator;

    public EnchantingTableTileEntity(int x, int y, int z) {
        super(x, y, z);
        long seed = (((long) x) * 3129871L) ^ (((long) z) * 116129781L) ^ y;
        this.random = new Random(seed);
        this.particleRandom = new Random(seed ^ 0x5DEECE66DL);
    }

    @Override
    public String getTypeId() {
        return "enchanting_table";
    }

    @Override
    public void tick(World world, float deltaTime) {
        tickAccumulator += deltaTime * 20.0f;
        while (tickAccumulator >= 1.0f) {
            tickAccumulator -= 1.0f;
            tickOne(world);
        }
    }

    private void tickOne(World world) {
        prevBookSpread = bookSpread;
        prevBookRotation = bookRotation2;

        Player nearbyPlayer = getNearbyPlayer(world);
        if (nearbyPlayer != null) {
            double dx = nearbyPlayer.getPosition().x - (getPos().x() + 0.5);
            double dz = nearbyPlayer.getPosition().z - (getPos().z() + 0.5);
            bookRotation = (float) Math.atan2(dz, dx);
            bookSpread += BOOK_SPREAD_STEP;
            spawnBookshelfGlyphParticle(world);

            if (bookSpread < 0.5f || random.nextInt(40) == 0) {
                float previousTarget = pageFlipTarget;
                do {
                    pageFlipTarget += random.nextInt(4) - random.nextInt(4);
                } while (previousTarget == pageFlipTarget);
            }
        } else {
            bookRotation += 0.02f;
            bookSpread -= BOOK_SPREAD_STEP;
        }

        bookRotation2 = wrapRadians(bookRotation2);
        bookRotation = wrapRadians(bookRotation);
        bookRotation2 += wrapRadians(bookRotation - bookRotation2) * 0.4f;
        bookSpread = clamp(bookSpread, 0.0f, 1.0f);

        tickCount++;
        prevPageFlip = pageFlip;
        float pageDelta = clamp((pageFlipTarget - pageFlip) * 0.4f, -0.2f, 0.2f);
        pageFlipVelocity += (pageDelta - pageFlipVelocity) * 0.9f;
        pageFlip += pageFlipVelocity;
    }

    private void spawnBookshelfGlyphParticle(World world) {
        if (world == null || particleRandom.nextInt(GLYPH_PARTICLE_CHANCE) != 0) {
            return;
        }

        int selectedX = 0;
        int selectedY = 0;
        int selectedZ = 0;
        int seen = 0;
        int tableX = getPos().x();
        int tableY = getPos().y();
        int tableZ = getPos().z();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dz == 0 || !hasOpenGap(world, tableX + dx, tableY, tableZ + dz)) {
                    continue;
                }
                int[][] positions = dx != 0 && dz != 0
                        ? new int[][] {
                                { tableX + dx * 2, tableZ + dz * 2 },
                                { tableX + dx * 2, tableZ + dz },
                                { tableX + dx, tableZ + dz * 2 } }
                        : new int[][] { { tableX + dx * 2, tableZ + dz * 2 } };
                for (int[] position : positions) {
                    for (int dy = 0; dy <= 1; dy++) {
                        if (world.getBlockIfLoaded(position[0], tableY + dy, position[1], BlockType.AIR)
                                != BlockType.BOOKSHELF) {
                            continue;
                        }
                        seen++;
                        if (particleRandom.nextInt(seen) == 0) {
                            selectedX = position[0];
                            selectedY = tableY + dy;
                            selectedZ = position[1];
                        }
                    }
                }
            }
        }
        if (seen > 0) {
            world.spawnEnchantmentTableParticle(tableX, tableY, tableZ, selectedX, selectedY, selectedZ);
        }
    }

    private static boolean hasOpenGap(World world, int x, int y, int z) {
        return world.getBlockIfLoaded(x, y, z, BlockType.AIR) == BlockType.AIR
                && world.getBlockIfLoaded(x, y + 1, z, BlockType.AIR) == BlockType.AIR;
    }

    private Player getNearbyPlayer(World world) {
        Player player = world.getPlayer();
        if (player == null) {
            return null;
        }
        double dx = player.getPosition().x - (getPos().x() + 0.5);
        double dy = player.getPosition().y - (getPos().y() + 0.5);
        double dz = player.getPosition().z - (getPos().z() + 0.5);
        return dx * dx + dy * dy + dz * dz <= PLAYER_RANGE * PLAYER_RANGE ? player : null;
    }

    private static float wrapRadians(float value) {
        while (value >= Math.PI) {
            value -= (float) Math.PI * 2.0f;
        }
        while (value < -Math.PI) {
            value += (float) Math.PI * 2.0f;
        }
        return value;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public int getTickCount() {
        return tickCount;
    }

    public float getTickAccumulator() {
        return tickAccumulator;
    }

    public float getBookSpread() {
        return bookSpread;
    }

    public float getPrevBookSpread() {
        return prevBookSpread;
    }

    public float getBookSpread(float partialTick) {
        return prevBookSpread + (bookSpread - prevBookSpread) * partialTick;
    }

    public float getBookRotation() {
        return bookRotation;
    }

    public float getBookRotation2() {
        return bookRotation2;
    }

    public float getPrevBookRotation() {
        return prevBookRotation;
    }

    public float getBookRotation(float partialTick) {
        return prevBookRotation + (bookRotation2 - prevBookRotation) * partialTick;
    }

    public float getPageFlip() {
        return pageFlip;
    }

    public float getPrevPageFlip() {
        return prevPageFlip;
    }

    public float getPageFlipTarget() {
        return pageFlipTarget;
    }

    public float getPageFlipVelocity() {
        return pageFlipVelocity;
    }

    public float getPageFlip(float partialTick) {
        return prevPageFlip + (pageFlip - prevPageFlip) * partialTick;
    }

    public void setAnimationState(int tickCount,
            float pageFlip,
            float prevPageFlip,
            float pageFlipTarget,
            float pageFlipVelocity,
            float bookSpread,
            float prevBookSpread,
            float bookRotation,
            float bookRotation2,
            float prevBookRotation,
            float tickAccumulator) {
        this.tickCount = Math.max(0, tickCount);
        this.pageFlip = pageFlip;
        this.prevPageFlip = prevPageFlip;
        this.pageFlipTarget = pageFlipTarget;
        this.pageFlipVelocity = pageFlipVelocity;
        this.bookSpread = clamp(bookSpread, 0.0f, 1.0f);
        this.prevBookSpread = clamp(prevBookSpread, 0.0f, 1.0f);
        this.bookRotation = wrapRadians(bookRotation);
        this.bookRotation2 = wrapRadians(bookRotation2);
        this.prevBookRotation = wrapRadians(prevBookRotation);
        this.tickAccumulator = clamp(tickAccumulator, 0.0f, 1.0f);
    }
}
