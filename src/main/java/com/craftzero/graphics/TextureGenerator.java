package com.craftzero.graphics;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
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
        BufferedImage atlas = new BufferedImage(ATLAS_SIZE, ATLAS_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = atlas.createGraphics();

        // Define colors for blocks
        // Based on BlockType.java indices

        // 0: Grass Top (Green)
        drawBlock(g, 0, 0, new Color(100, 200, 100)); // Green

        // 1: Grass Side (Green top, dirt bottom)
        drawBlock(g, 1, 0, new Color(120, 180, 100), new Color(100, 60, 40));

        // 2: Dirt (Brown)
        drawBlock(g, 2, 0, new Color(120, 70, 40));

        // 3: Stone (Gray)
        drawBlock(g, 3, 0, new Color(120, 120, 120));

        // 4: Cobblestone (Rough Gray)
        drawBlockCheckered(g, 4, 0, new Color(100, 100, 100), new Color(130, 130, 130));

        // 5: Bedrock (Dark Gray)
        drawBlock(g, 5, 0, new Color(30, 30, 30));

        // 6: Sand (Yellow)
        drawBlock(g, 6, 0, new Color(220, 210, 150));

        // 7: Gravel (Light Gray)
        drawBlock(g, 7, 0, new Color(150, 150, 150));

        // 8: Oak Log Top (Light Wood)
        drawBlock(g, 8, 0, new Color(160, 120, 60));

        // 9: Oak Log Side (Bark)
        drawStriped(g, 9, 0, new Color(100, 70, 30), new Color(90, 60, 20));

        // 10: Planks (Wood)
        drawBlockCheckered(g, 10, 0, new Color(180, 140, 80), new Color(170, 130, 70));

        // 11: Leaves (Dark Green semi-transparent)
        drawBlock(g, 11, 0, new Color(60, 140, 60, 255)); // Solid for now

        // 12: Glass (Clear with frame and glint)
        // No fill -> Alpha 0 (transparent)
        drawFrame(g, 12, 0, Color.WHITE);
        // Add minimal "glint" pixels for visibility
        g.setColor(new Color(255, 255, 255, 200));
        g.fillRect(12 * BLOCK_SIZE + 4, 3, 2, 1);
        g.fillRect(12 * BLOCK_SIZE + 5, 4, 1, 2);
        g.fillRect(12 * BLOCK_SIZE + 10, 10, 2, 1);
        g.fillRect(12 * BLOCK_SIZE + 9, 11, 1, 2);

        // 13: Water (Lighter Blue, Translucent)
        drawBlock(g, 13, 0, new Color(40, 160, 255, 140));

        // 14: Brick (Red)
        drawBlockCheckered(g, 14, 0, new Color(180, 80, 60), new Color(150, 60, 50));

        // 15: Coal Ore
        drawBlockWithGems(g, 15, 0, new Color(120, 120, 120), Color.BLACK);

        // 16: Iron Ore
        drawBlockWithGems(g, 16, 0, new Color(120, 120, 120), new Color(220, 210, 200));

        // 17: Gold Ore
        drawBlockWithGems(g, 17, 0, new Color(120, 120, 120), Color.YELLOW);

        // 18: Diamond Ore
        drawBlockWithGems(g, 18, 0, new Color(120, 120, 120), Color.CYAN);

        // 19: Snow
        drawBlock(g, 3, 1, Color.WHITE); // Use row 1, col 3

        // 20: Snow Side
        drawBlock(g, 4, 1, Color.WHITE, new Color(120, 70, 40));

        // 21: Ice
        drawBlock(g, 5, 1, new Color(150, 200, 255, 200));

        // 22: Stick (brown stick on transparent)
        drawStick(g, 6, 1);

        // 23: Crafting Table Top (grid pattern)
        drawCraftingTableTop(g, 7, 1);

        // 24: Crafting Table Side (tools/planks)
        drawCraftingTableSide(g, 8, 1);

        g.dispose();

        File file = new File("src/main/resources/textures/atlas.png");
        file.getParentFile().mkdirs();
        ImageIO.write(atlas, "png", file);
        System.out.println("Generated atlas.png at " + file.getAbsolutePath());
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
        // Plank base
        g.setColor(new Color(180, 140, 80));
        g.fillRect(x, y, BLOCK_SIZE, BLOCK_SIZE);
        // Darker panel in center (tool pattern)
        g.setColor(new Color(140, 100, 60));
        g.fillRect(x + 3, y + 2, 10, 12);
        // Tool shapes (simplified)
        g.setColor(new Color(100, 100, 100)); // Metal gray
        g.fillRect(x + 5, y + 3, 2, 4); // Hammer head
        g.setColor(new Color(120, 80, 40)); // Wood
        g.fillRect(x + 5, y + 7, 2, 6); // Handle
        g.setColor(new Color(100, 100, 100));
        g.fillRect(x + 9, y + 3, 3, 2); // Saw blade
        g.fillRect(x + 9, y + 5, 1, 8);
    }
}
