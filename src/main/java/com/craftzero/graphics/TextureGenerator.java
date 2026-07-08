package com.craftzero.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class TextureGenerator {

    // 16x16 grid of 16x16 pixel blocks
    private static final int BLOCK_SIZE = 16;
    private static final int GRID_SIZE = 16;
    private static final int ATLAS_SIZE = BLOCK_SIZE * GRID_SIZE; // 256x256

    public static void main(String[] args) {
        try {
            generateAtlas();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void generateAtlas() throws Exception {
        BufferedImage atlas = createAtlasImage();

        File file = new File("src/main/resources/textures/atlas.png");
        file.getParentFile().mkdirs();
        ImageIO.write(atlas, "png", file);
        System.out.println("Generated atlas.png at " + file.getAbsolutePath());
    }

    static byte[] createAtlasPngBytes() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(createAtlasImage(), "png", output);
        return output.toByteArray();
    }

    static BufferedImage createAtlasImage() {
        BufferedImage atlas = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = atlas.createGraphics();

        drawClassicTerrainCells(g);

        g.dispose();
        return atlas;
    }

    private static void drawClassicTerrainCells(Graphics2D g) {
        drawBlock(g, 0, new Color(100, 178, 80));
        drawBlock(g, 1, new Color(120, 120, 120));
        drawBlock(g, 2, new Color(120, 70, 40));
        drawBlock(g, 3, new Color(115, 176, 90), new Color(104, 63, 39));
        drawBlockCheckered(g, 4, new Color(180, 140, 80), new Color(160, 112, 60));
        drawBlock(g, 5, new Color(150, 150, 150), new Color(120, 120, 120));
        drawBlock(g, 6, new Color(172, 172, 172));
        drawBlockCheckered(g, 7, new Color(178, 76, 58), new Color(136, 50, 42));
        drawTntSide(g, 8);
        drawTntTop(g, 9);
        drawBlock(g, 10, new Color(97, 55, 36));
        drawWeb(g, 11);
        drawPlant(g, 12, new Color(190, 42, 48));
        drawPlant(g, 13, new Color(226, 205, 40));
        drawPortal(g, 14);
        drawSapling(g, 15);

        drawBlockCheckered(g, 16, new Color(95, 95, 95), new Color(132, 132, 132));
        drawBlockCheckered(g, 17, new Color(35, 35, 35), new Color(82, 82, 82));
        drawBlock(g, 18, new Color(220, 211, 148));
        drawBlockCheckered(g, 19, new Color(139, 139, 139), new Color(171, 171, 171));
        drawStriped(g, 20, new Color(100, 70, 30), new Color(74, 50, 23));
        drawLogTop(g, 21);
        drawBlock(g, 22, new Color(214, 212, 205));
        drawBlock(g, 23, new Color(226, 194, 65));
        drawBlock(g, 24, new Color(80, 208, 210));
        drawChestPanel(g, 25, false);
        drawChestPanel(g, 26, true);
        drawChestPanel(g, 27, true);
        drawPlant(g, 28, new Color(196, 42, 42));
        drawPlant(g, 29, new Color(124, 82, 50));

        drawBlockWithGems(g, 32, new Color(120, 120, 120), new Color(218, 180, 58));
        drawBlockWithGems(g, 33, new Color(120, 120, 120), new Color(220, 210, 200));
        drawBlockWithGems(g, 34, new Color(120, 120, 120), Color.BLACK);
        drawBookshelf(g, 35);
        drawBlockCheckered(g, 36, new Color(83, 101, 78), new Color(114, 133, 98));
        drawBlockCheckered(g, 37, new Color(22, 16, 38), new Color(47, 37, 72));
        drawPlant(g, 39, new Color(80, 160, 58));

        drawCraftingTableTop(g, 43);
        drawFurnaceFront(g, 44, false);
        drawBlockCheckered(g, 45, new Color(106, 106, 106), new Color(142, 142, 142));
        drawDispenserFront(g, 46);
        drawBlockCheckered(g, 48, new Color(170, 172, 67), new Color(126, 136, 58));
        drawGlass(g, 49);
        drawBlockWithGems(g, 50, new Color(120, 120, 120), new Color(72, 220, 222));
        drawBlockWithGems(g, 51, new Color(120, 120, 120), new Color(190, 26, 26));
        drawBlock(g, 53, new Color(54, 132, 54, 220));
        drawBlockCheckered(g, 54, new Color(118, 118, 118), new Color(151, 151, 151));
        drawPlant(g, 55, new Color(91, 67, 41));
        drawPlant(g, 56, new Color(56, 139, 55));
        drawCraftingTableSide(g, 59);
        drawFurnaceFront(g, 61, true);
        drawBlockCheckered(g, 62, new Color(112, 112, 112), new Color(150, 150, 150));

        drawBlock(g, 64, Color.WHITE);
        drawSpawner(g, 65);
        drawBlock(g, 66, new Color(238, 248, 255));
        drawBlock(g, 67, new Color(155, 205, 245, 190));
        drawCactus(g, 70);
        drawBlock(g, 72, new Color(154, 160, 164));
        drawReed(g, 73);
        drawJukebox(g, 74, false);
        drawJukebox(g, 75, true);
        drawLilyPad(g, 76);
        drawBlock(g, 77, new Color(116, 112, 122));
        drawBlock(g, 78, new Color(120, 70, 40), new Color(116, 112, 122));
        drawTorch(g, 80, true);
        drawDoor(g, 81, new Color(154, 96, 45));
        drawDoor(g, 82, new Color(184, 183, 176));
        drawLadder(g, 83);
        drawTrapdoor(g, 84);
        drawBars(g, 85);
        drawFarmland(g, 87);
        for (int stage = 0; stage < 8; stage++) {
            drawCropStage(g, 88 + stage, stage);
        }

        drawLever(g, 96);
        drawTorch(g, 99, false);
        drawBlockCheckered(g, 100, new Color(105, 124, 91), new Color(137, 151, 118));
        drawBlockCheckered(g, 101, new Color(103, 103, 103), new Color(142, 142, 142));
        drawPumpkin(g, 102, false);
        drawBlockCheckered(g, 103, new Color(122, 42, 36), new Color(93, 30, 32));
        drawBlock(g, 104, new Color(84, 63, 50));
        drawBlockCheckered(g, 105, new Color(210, 174, 80), new Color(250, 232, 124));
        drawPistonFace(g, 106, true);
        drawPistonFace(g, 107, false);
        drawBlockCheckered(g, 108, new Color(142, 128, 92), new Color(106, 96, 74));
        drawBlock(g, 109, new Color(118, 118, 118));
        drawPistonFace(g, 110, false);
        drawStem(g, 111);
        drawPumpkin(g, 119, true);

        drawRail(g, 128, false);
        drawRepeater(g, 131, false);
        drawBlock(g, 144, new Color(35, 70, 168));
        drawRepeater(g, 147, true);
        drawCauldron(g, 154);
        drawCauldron(g, 155);
        drawCauldron(g, 156);
        drawBrewingStand(g, 157);
        drawBlockWithGems(g, 160, new Color(120, 120, 120), new Color(36, 78, 190));
        drawEnchantmentTop(g, 166);
        drawBlock(g, 167, new Color(24, 16, 28));
        drawEndPortalFrame(g, 174);
        drawBlock(g, 175, new Color(219, 224, 168));
        drawBlock(g, 176, new Color(216, 201, 140));
        drawRail(g, 179, true);
        drawBlock(g, 192, new Color(228, 215, 158));
        drawRail(g, 195, false);
        drawBlock(g, 205, new Color(40, 160, 255, 150));
        drawBlockCheckered(g, 224, new Color(50, 24, 28), new Color(82, 32, 36));
        drawNetherWart(g, 226);
        drawBlock(g, 237, new Color(245, 94, 22, 210));
    }

    private static int col(int index) {
        return index % GRID_SIZE;
    }

    private static int row(int index) {
        return index / GRID_SIZE;
    }

    private static int pixelX(int index) {
        return col(index) * BLOCK_SIZE;
    }

    private static int pixelY(int index) {
        return row(index) * BLOCK_SIZE;
    }

    private static void drawBlock(Graphics2D g, int index, Color color) {
        drawBlock(g, col(index), row(index), color);
    }

    private static void drawBlock(Graphics2D g, int index, Color topColor, Color bottomColor) {
        drawBlock(g, col(index), row(index), topColor, bottomColor);
    }

    private static void drawBlockCheckered(Graphics2D g, int index, Color c1, Color c2) {
        drawBlockCheckered(g, col(index), row(index), c1, c2);
    }

    private static void drawStriped(Graphics2D g, int index, Color c1, Color c2) {
        drawStriped(g, col(index), row(index), c1, c2);
    }

    private static void drawBlockWithGems(Graphics2D g, int index, Color stone, Color gem) {
        drawBlockWithGems(g, col(index), row(index), stone, gem);
    }

    private static void drawFrame(Graphics2D g, int index, Color color) {
        drawFrame(g, col(index), row(index), color);
    }

    private static void drawCraftingTableTop(Graphics2D g, int index) {
        drawCraftingTableTop(g, col(index), row(index));
    }

    private static void drawCraftingTableSide(Graphics2D g, int index) {
        drawCraftingTableSide(g, col(index), row(index));
    }

    private static void drawTntSide(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, new Color(180, 42, 38));
        g.setColor(new Color(238, 222, 184));
        g.fillRect(x, y + 5, BLOCK_SIZE, 5);
        g.setColor(new Color(80, 38, 30));
        g.fillRect(x + 3, y + 6, 2, 3);
        g.fillRect(x + 7, y + 6, 2, 3);
        g.fillRect(x + 11, y + 6, 2, 3);
    }

    private static void drawTntTop(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlockCheckered(g, index, new Color(152, 43, 34), new Color(92, 43, 30));
        g.setColor(new Color(44, 38, 30));
        g.fillRect(x + 7, y + 2, 2, 12);
        g.fillRect(x + 2, y + 7, 12, 2);
    }

    private static void drawWeb(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        g.setColor(new Color(235, 235, 235, 210));
        g.drawLine(x + 1, y + 1, x + 14, y + 14);
        g.drawLine(x + 14, y + 1, x + 1, y + 14);
        g.drawLine(x + 8, y + 0, x + 8, y + 15);
        g.drawLine(x + 0, y + 8, x + 15, y + 8);
        g.setColor(new Color(180, 180, 180, 180));
        g.drawRect(x + 4, y + 4, 7, 7);
    }

    private static void drawPlant(Graphics2D g, int index, Color topColor) {
        int x = pixelX(index);
        int y = pixelY(index);
        g.setColor(new Color(54, 130, 46));
        g.fillRect(x + 7, y + 6, 2, 9);
        g.fillRect(x + 5, y + 9, 2, 4);
        g.fillRect(x + 9, y + 10, 2, 3);
        g.setColor(topColor);
        g.fillRect(x + 5, y + 3, 6, 4);
        g.fillRect(x + 4, y + 4, 8, 2);
    }

    private static void drawPortal(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, new Color(70, 22, 92, 170));
        g.setColor(new Color(160, 68, 220, 190));
        g.fillRect(x + 2, y + 3, 4, 9);
        g.fillRect(x + 9, y + 2, 5, 11);
        g.setColor(new Color(232, 120, 255, 220));
        g.fillRect(x + 5, y + 6, 6, 2);
        g.fillRect(x + 3, y + 11, 8, 1);
    }

    private static void drawSapling(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        g.setColor(new Color(92, 58, 30));
        g.fillRect(x + 7, y + 7, 2, 8);
        g.setColor(new Color(70, 150, 58));
        g.fillRect(x + 5, y + 3, 6, 5);
        g.fillRect(x + 3, y + 5, 10, 3);
    }

    private static void drawLogTop(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, new Color(165, 120, 60));
        g.setColor(new Color(110, 72, 34));
        g.drawRect(x + 3, y + 3, 9, 9);
        g.drawRect(x + 5, y + 5, 5, 5);
        g.setColor(new Color(202, 152, 78));
        g.fillRect(x + 7, y + 7, 2, 2);
    }

    private static void drawChestPanel(Graphics2D g, int index, boolean latch) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, new Color(156, 102, 42));
        g.setColor(new Color(92, 56, 24));
        g.drawRect(x + 1, y + 1, 13, 13);
        g.drawLine(x + 2, y + 7, x + 13, y + 7);
        if (latch) {
            g.setColor(new Color(210, 196, 126));
            g.fillRect(x + 7, y + 6, 2, 4);
        }
    }

    private static void drawBookshelf(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, new Color(118, 72, 34));
        Color[] books = {
                new Color(162, 38, 36), new Color(48, 84, 160), new Color(45, 128, 62), new Color(210, 178, 70)
        };
        for (int i = 0; i < 4; i++) {
            g.setColor(books[i]);
            g.fillRect(x + 2 + i * 3, y + 3, 2, 10);
        }
        g.setColor(new Color(84, 50, 24));
        g.drawLine(x + 1, y + 8, x + 14, y + 8);
    }

    private static void drawFurnaceFront(Graphics2D g, int index, boolean lit) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlockCheckered(g, index, new Color(104, 104, 104), new Color(140, 140, 140));
        g.setColor(new Color(44, 44, 44));
        g.fillRect(x + 4, y + 4, 8, 7);
        g.setColor(lit ? new Color(236, 118, 26) : new Color(78, 78, 78));
        g.fillRect(x + 5, y + 6, 6, 3);
        if (lit) {
            g.setColor(new Color(255, 210, 74));
            g.fillRect(x + 7, y + 5, 2, 4);
        }
    }

    private static void drawDispenserFront(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlockCheckered(g, index, new Color(96, 96, 96), new Color(134, 134, 134));
        g.setColor(new Color(36, 36, 36));
        g.fillRect(x + 5, y + 5, 6, 6);
        g.setColor(new Color(170, 170, 170));
        g.drawRect(x + 4, y + 4, 7, 7);
    }

    private static void drawGlass(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawFrame(g, index, new Color(220, 245, 255, 180));
        g.setColor(new Color(255, 255, 255, 180));
        g.fillRect(x + 4, y + 3, 2, 1);
        g.fillRect(x + 5, y + 4, 1, 2);
        g.fillRect(x + 10, y + 10, 2, 1);
        g.fillRect(x + 9, y + 11, 1, 2);
    }

    private static void drawSpawner(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, new Color(38, 54, 60, 170));
        g.setColor(new Color(80, 102, 112, 220));
        g.drawRect(x + 2, y + 2, 11, 11);
        g.drawLine(x + 2, y + 7, x + 13, y + 7);
        g.drawLine(x + 7, y + 2, x + 7, y + 13);
    }

    private static void drawCactus(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, new Color(42, 130, 58));
        g.setColor(new Color(28, 92, 40));
        g.drawLine(x + 3, y, x + 3, y + 15);
        g.drawLine(x + 12, y, x + 12, y + 15);
        g.setColor(new Color(216, 214, 166));
        g.fillRect(x + 5, y + 4, 1, 1);
        g.fillRect(x + 10, y + 9, 1, 1);
    }

    private static void drawReed(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        g.setColor(new Color(94, 178, 68));
        g.fillRect(x + 4, y + 2, 2, 13);
        g.fillRect(x + 7, y + 4, 2, 11);
        g.fillRect(x + 10, y + 1, 2, 14);
        g.setColor(new Color(198, 224, 86));
        g.fillRect(x + 5, y + 3, 1, 3);
        g.fillRect(x + 8, y + 5, 1, 3);
    }

    private static void drawJukebox(Graphics2D g, int index, boolean top) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, new Color(112, 58, 34));
        g.setColor(new Color(66, 34, 24));
        if (top) {
            g.drawRect(x + 3, y + 3, 9, 9);
            g.fillRect(x + 6, y + 6, 4, 4);
        } else {
            g.drawRect(x + 2, y + 2, 11, 11);
            g.drawLine(x + 4, y + 4, x + 11, y + 11);
        }
    }

    private static void drawLilyPad(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        g.setColor(new Color(50, 128, 58, 230));
        g.fillRect(x + 3, y + 3, 10, 9);
        g.fillRect(x + 5, y + 1, 7, 13);
        g.setColor(new Color(20, 82, 36, 230));
        g.fillRect(x + 8, y + 7, 6, 2);
    }

    private static void drawTorch(Graphics2D g, int index, boolean lit) {
        int x = pixelX(index);
        int y = pixelY(index);
        g.setColor(new Color(122, 72, 32));
        g.fillRect(x + 7, y + 5, 2, 10);
        g.setColor(lit ? new Color(255, 206, 70) : new Color(126, 24, 24));
        g.fillRect(x + 5, y + 1, 6, 5);
        if (lit) {
            g.setColor(new Color(236, 76, 26));
            g.fillRect(x + 7, y + 2, 2, 3);
        }
    }

    private static void drawDoor(Graphics2D g, int index, Color base) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, base);
        g.setColor(base.darker());
        g.drawRect(x + 2, y + 1, 11, 14);
        g.drawLine(x + 2, y + 7, x + 13, y + 7);
        g.drawLine(x + 7, y + 1, x + 7, y + 14);
        g.setColor(new Color(222, 192, 82));
        g.fillRect(x + 11, y + 7, 1, 2);
    }

    private static void drawLadder(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        g.setColor(new Color(132, 82, 36));
        g.fillRect(x + 4, y + 1, 2, 14);
        g.fillRect(x + 10, y + 1, 2, 14);
        for (int rung = 3; rung <= 12; rung += 3) {
            g.fillRect(x + 4, y + rung, 8, 1);
        }
    }

    private static void drawTrapdoor(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, new Color(150, 92, 42));
        g.setColor(new Color(82, 48, 22));
        g.drawRect(x + 2, y + 2, 11, 11);
        g.drawLine(x + 2, y + 7, x + 13, y + 7);
        g.drawLine(x + 7, y + 2, x + 7, y + 13);
    }

    private static void drawBars(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        g.setColor(new Color(174, 174, 168));
        g.fillRect(x + 3, y, 2, 16);
        g.fillRect(x + 8, y, 2, 16);
        g.fillRect(x + 13, y, 1, 16);
        g.fillRect(x + 2, y + 5, 12, 2);
        g.fillRect(x + 2, y + 10, 12, 2);
    }

    private static void drawFarmland(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, new Color(104, 62, 36));
        g.setColor(new Color(72, 42, 24));
        for (int line = 2; line < 15; line += 4) {
            g.drawLine(x, y + line, x + 15, y + line);
        }
    }

    private static void drawCropStage(Graphics2D g, int index, int stage) {
        int x = pixelX(index);
        int y = pixelY(index);
        int height = 3 + stage;
        g.setColor(new Color(64, 136 + stage * 8, 42));
        g.fillRect(x + 5, y + 15 - height, 2, height);
        g.fillRect(x + 9, y + 15 - height, 2, height);
        if (stage >= 5) {
            g.setColor(new Color(212, 178, 72));
            g.fillRect(x + 4, y + 9, 8, 3);
        }
    }

    private static void drawLever(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        g.setColor(new Color(94, 94, 94));
        g.fillRect(x + 4, y + 10, 8, 4);
        g.setColor(new Color(116, 72, 36));
        g.drawLine(x + 7, y + 9, x + 11, y + 3);
        g.drawLine(x + 8, y + 9, x + 12, y + 3);
    }

    private static void drawPumpkin(Graphics2D g, int index, boolean lit) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, new Color(194, 98, 28));
        g.setColor(new Color(132, 68, 22));
        g.drawLine(x + 5, y, x + 5, y + 15);
        g.drawLine(x + 10, y, x + 10, y + 15);
        g.setColor(lit ? new Color(255, 208, 72) : new Color(48, 34, 22));
        g.fillRect(x + 4, y + 5, 2, 2);
        g.fillRect(x + 10, y + 5, 2, 2);
        g.fillRect(x + 6, y + 10, 5, 2);
    }

    private static void drawPistonFace(Graphics2D g, int index, boolean sticky) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, new Color(146, 132, 88));
        g.setColor(new Color(92, 82, 58));
        g.drawRect(x + 2, y + 2, 11, 11);
        g.setColor(sticky ? new Color(72, 150, 62) : new Color(156, 156, 156));
        g.fillRect(x + 5, y + 5, 6, 6);
    }

    private static void drawStem(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        g.setColor(new Color(84, 154, 38));
        g.fillRect(x + 7, y + 6, 2, 9);
        g.fillRect(x + 8, y + 11, 5, 2);
        g.setColor(new Color(204, 180, 60));
        g.fillRect(x + 5, y + 4, 5, 3);
    }

    private static void drawRail(Graphics2D g, int index, boolean powered) {
        int x = pixelX(index);
        int y = pixelY(index);
        g.setColor(new Color(152, 152, 152));
        g.fillRect(x + 3, y, 2, 16);
        g.fillRect(x + 11, y, 2, 16);
        g.setColor(powered ? new Color(206, 64, 34) : new Color(112, 72, 36));
        for (int rung = 2; rung < 15; rung += 4) {
            g.fillRect(x + 4, y + rung, 8, 1);
        }
    }

    private static void drawRepeater(Graphics2D g, int index, boolean powered) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, new Color(172, 166, 158));
        g.setColor(powered ? new Color(224, 42, 34) : new Color(96, 28, 28));
        g.fillRect(x + 4, y + 5, 2, 6);
        g.fillRect(x + 10, y + 5, 2, 6);
        g.fillRect(x + 5, y + 7, 6, 1);
    }

    private static void drawCauldron(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, new Color(68, 68, 72));
        g.setColor(new Color(34, 34, 38));
        g.fillRect(x + 3, y + 3, 10, 8);
        g.setColor(new Color(112, 112, 118));
        g.drawRect(x + 2, y + 2, 11, 11);
    }

    private static void drawBrewingStand(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        g.setColor(new Color(132, 100, 48));
        g.fillRect(x + 7, y + 2, 2, 10);
        g.fillRect(x + 3, y + 12, 10, 2);
        g.setColor(new Color(178, 142, 68));
        g.fillRect(x + 5, y + 4, 6, 2);
    }

    private static void drawEnchantmentTop(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, new Color(80, 34, 42));
        g.setColor(new Color(32, 22, 28));
        g.fillRect(x + 2, y + 2, 12, 12);
        g.setColor(new Color(206, 182, 92));
        g.drawRect(x + 4, y + 4, 7, 7);
    }

    private static void drawEndPortalFrame(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        drawBlock(g, index, new Color(116, 126, 78));
        g.setColor(new Color(54, 96, 70));
        g.fillRect(x + 4, y + 4, 8, 8);
        g.setColor(new Color(180, 210, 130));
        g.drawRect(x + 2, y + 2, 11, 11);
    }

    private static void drawNetherWart(Graphics2D g, int index) {
        int x = pixelX(index);
        int y = pixelY(index);
        g.setColor(new Color(128, 20, 32));
        g.fillRect(x + 5, y + 8, 2, 7);
        g.fillRect(x + 9, y + 7, 2, 8);
        g.setColor(new Color(190, 32, 46));
        g.fillRect(x + 4, y + 5, 4, 4);
        g.fillRect(x + 8, y + 4, 5, 5);
    }

    private static void drawBlock(Graphics2D g, int col, int row, Color color) {
        g.setColor(color);
        g.fillRect(col * BLOCK_SIZE, row * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
        // Add subtle noise/border
        g.setColor(color.darker());
        g.drawRect(col * BLOCK_SIZE, row * BLOCK_SIZE, BLOCK_SIZE - 1, BLOCK_SIZE - 1);
    }

    private static void drawBlock(Graphics2D g, int col, int row, Color topColor, Color bottomColor) {
        g.setColor(topColor);
        g.fillRect(col * BLOCK_SIZE, row * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE / 4);
        g.setColor(bottomColor);
        g.fillRect(col * BLOCK_SIZE, row * BLOCK_SIZE + BLOCK_SIZE / 4, BLOCK_SIZE, BLOCK_SIZE * 3 / 4);
    }

    private static void drawBlockCheckered(Graphics2D g, int col, int row, Color c1, Color c2) {
        g.setColor(c1);
        g.fillRect(col * BLOCK_SIZE, row * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
        g.setColor(c2);
        g.fillRect(col * BLOCK_SIZE + 4, row * BLOCK_SIZE + 4, 8, 8);
    }

    private static void drawStriped(Graphics2D g, int col, int row, Color c1, Color c2) {
        g.setColor(c1);
        g.fillRect(col * BLOCK_SIZE, row * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
        g.setColor(c2);
        g.fillRect(col * BLOCK_SIZE + 4, row * BLOCK_SIZE, 4, BLOCK_SIZE);
        g.fillRect(col * BLOCK_SIZE + 12, row * BLOCK_SIZE, 2, BLOCK_SIZE);
    }

    private static void drawBlockWithGems(Graphics2D g, int col, int row, Color stone, Color gem) {
        drawBlock(g, col, row, stone);
        g.setColor(gem);
        g.fillRect(col * BLOCK_SIZE + 4, row * BLOCK_SIZE + 4, 4, 4);
        g.fillRect(col * BLOCK_SIZE + 10, row * BLOCK_SIZE + 8, 3, 3);
    }

    private static void drawFrame(Graphics2D g, int col, int row, Color color) {
        g.setColor(color);
        g.drawRect(col * BLOCK_SIZE, row * BLOCK_SIZE, BLOCK_SIZE - 1, BLOCK_SIZE - 1);
    }

    private static void drawStick(Graphics2D g, int col, int row) {
        int x = col * BLOCK_SIZE;
        int y = row * BLOCK_SIZE;
        // Transparent background (no fill)
        // Draw stick diagonal
        g.setColor(new Color(140, 90, 40));
        g.fillRect(x + 6, y + 2, 4, 12);
        g.setColor(new Color(120, 70, 30));
        g.fillRect(x + 6, y + 2, 1, 12);
    }

    private static void drawCraftingTableTop(Graphics2D g, int col, int row) {
        int x = col * BLOCK_SIZE;
        int y = row * BLOCK_SIZE;
        // Plank base
        g.setColor(new Color(180, 140, 80));
        g.fillRect(x, y, BLOCK_SIZE, BLOCK_SIZE);
        // Grid lines
        g.setColor(new Color(100, 70, 40));
        g.drawLine(x + 5, y + 1, x + 5, y + 14);
        g.drawLine(x + 10, y + 1, x + 10, y + 14);
        g.drawLine(x + 1, y + 5, x + 14, y + 5);
        g.drawLine(x + 1, y + 10, x + 14, y + 10);
        // Border
        g.setColor(new Color(140, 100, 60));
        g.drawRect(x, y, BLOCK_SIZE - 1, BLOCK_SIZE - 1);
    }

    private static void drawCraftingTableSide(Graphics2D g, int col, int row) {
        int x = col * BLOCK_SIZE;
        int y = row * BLOCK_SIZE;

        g.setColor(new Color(180, 140, 80));
        g.fillRect(x, y, BLOCK_SIZE, BLOCK_SIZE);

        g.setColor(new Color(199, 157, 91));
        g.drawLine(x + 1, y + 1, x + 14, y + 1);
        g.drawLine(x + 1, y + 6, x + 14, y + 6);
        g.setColor(new Color(116, 78, 44));
        g.drawRect(x, y, BLOCK_SIZE - 1, BLOCK_SIZE - 1);
        g.drawLine(x + 1, y + 7, x + 14, y + 7);
        g.drawLine(x + 1, y + 13, x + 14, y + 13);
        g.drawLine(x + 5, y + 2, x + 5, y + 5);
        g.drawLine(x + 10, y + 8, x + 10, y + 12);

        g.setColor(new Color(126, 82, 47));
        g.fillRect(x + 2, y + 2, 12, 11);
        g.setColor(new Color(84, 55, 34));
        g.drawRect(x + 2, y + 2, 11, 10);
        g.setColor(new Color(188, 139, 78));
        g.drawLine(x + 3, y + 3, x + 12, y + 3);
        g.drawLine(x + 3, y + 4, x + 3, y + 11);

        Color metal = new Color(174, 174, 162);
        Color metalShadow = new Color(93, 93, 88);
        Color handle = new Color(108, 70, 37);
        Color handleLight = new Color(157, 98, 48);

        g.setColor(metalShadow);
        g.fillRect(x + 3, y + 5, 5, 2);
        g.fillRect(x + 8, y + 3, 5, 1);
        g.fillRect(x + 11, y + 4, 2, 8);

        g.setColor(metal);
        g.fillRect(x + 3, y + 4, 5, 2);
        g.fillRect(x + 9, y + 3, 3, 8);
        g.fillRect(x + 8, y + 5, 1, 1);
        g.fillRect(x + 12, y + 6, 1, 1);
        g.fillRect(x + 8, y + 9, 1, 1);
        g.fillRect(x + 12, y + 11, 1, 1);

        g.setColor(handle);
        g.fillRect(x + 6, y + 6, 2, 6);
        g.fillRect(x + 5, y + 8, 1, 4);
        g.setColor(handleLight);
        g.fillRect(x + 6, y + 6, 1, 5);
        g.fillRect(x + 4, y + 11, 4, 1);
    }
}
