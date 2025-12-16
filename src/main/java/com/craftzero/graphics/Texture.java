package com.craftzero.graphics;

import org.lwjgl.system.MemoryStack;

import java.io.IOException;
// import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

/**
 * Texture loading and management using STB Image.
 */
public class Texture {

    private int textureId;
    private int width;
    private int height;

    public Texture(String resourcePath) throws Exception {
        this(loadFromResource(resourcePath));
    }

    public Texture(ByteBuffer imageBuffer) throws Exception {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Load image
            ByteBuffer image = stbi_load_from_memory(imageBuffer, w, h, channels, 4);
            if (image == null) {
                throw new Exception("Failed to load texture: " + stbi_failure_reason());
            }

            width = w.get(0);
            height = h.get(0);

            createTexture(image);

            stbi_image_free(image);
        }
    }

    public Texture(int width, int height, int pixelFormat) {
        this.width = width;
        this.height = height;

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, pixelFormat, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }

    private void createTexture(ByteBuffer image) {
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        // Pixel-perfect for Minecraft style (nearest neighbor filtering)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        // Clamp to edge to prevent texture bleeding
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        // Upload texture data
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);

        // Generate mipmaps for distance rendering
        glGenerateMipmap(GL_TEXTURE_2D);
    }

    private static ByteBuffer loadFromResource(String resourcePath) throws Exception {
        // Use the new High-Fidelity Procedural Atlas to guarantee correct texture
        // mapping
        // and avoid "chaotic" generation from incompatible external atlases.
        return generateProceduralAtlas();
    }

    private static ByteBuffer generateProceduralAtlas() {
        // Generate a 256x256 atlas with "Real Game" style textures
        int size = 256;
        int blockSize = 16;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = image.createGraphics();

        // Fill background with TRANSPARENT, not Magenta, to allow alpha blending
        g.setColor(new java.awt.Color(0, 0, 0, 0));
        g.fillRect(0, 0, size, size);

        // Helper to draw noise
        java.util.Random rng = new java.util.Random(12345);

        for (int i = 0; i < 256; i++) {
            int bx = (i % 16) * blockSize;
            int by = (i / 16) * blockSize;

            // Generate base noise for everything first so it's not empty
            for (int px = 0; px < 16; px++) {
                for (int py = 0; py < 16; py++) {
                    // Default noise
                    // g.setColor(...)
                }
            }
        }

        // --- ROW 1 ---

        // 0: Grass Top (Noisy Green)
        drawNoiseBlock(g, 0, 0, new java.awt.Color(40, 180, 40), new java.awt.Color(60, 220, 60));

        // 1: Grass Side (Dirt + Green Top)
        drawNoiseBlock(g, 16, 0, new java.awt.Color(139, 69, 19), new java.awt.Color(160, 82, 45)); // Dirt
        g.setColor(new java.awt.Color(40, 180, 40));
        g.fillRect(16, 0, 16, 4); // Grass strip
        // Add noise to grass strip
        for (int i = 0; i < 20; i++) {
            g.setColor(new java.awt.Color(60, 220, 60));
            g.fillRect(16 + rng.nextInt(16), rng.nextInt(4), 1, 1);
        }

        // 2: Dirt (Noisy Brown)
        drawNoiseBlock(g, 32, 0, new java.awt.Color(139, 69, 19), new java.awt.Color(160, 82, 45));

        // 3: Stone (Smooth Gray Noise)
        drawNoiseBlock(g, 48, 0, new java.awt.Color(128, 128, 128), new java.awt.Color(140, 140, 140));

        // 4: Cobblestone (Contrast Gray Pattern)
        drawNoiseBlock(g, 64, 0, new java.awt.Color(100, 100, 100), new java.awt.Color(110, 110, 110));
        g.setColor(new java.awt.Color(50, 50, 50));
        g.drawRect(64, 0, 15, 15);
        g.drawRect(68, 4, 8, 8);

        // 5: Bedrock (Dark Contrast)
        drawNoiseBlock(g, 80, 0, new java.awt.Color(20, 20, 20), new java.awt.Color(50, 50, 50));

        // 6: Sand (Yellow Noise)
        drawNoiseBlock(g, 96, 0, new java.awt.Color(240, 240, 160), new java.awt.Color(230, 230, 140));

        // 7: Gravel (Grainy Gray)
        drawNoiseBlock(g, 112, 0, new java.awt.Color(130, 130, 130), new java.awt.Color(150, 150, 150));

        // 8: Log Top (Rings)
        g.setColor(new java.awt.Color(160, 82, 45));
        g.fillRect(128, 0, 16, 16);
        g.setColor(new java.awt.Color(139, 69, 19));
        g.drawRect(129, 1, 13, 13);
        g.drawRect(132, 4, 7, 7);

        // 9: Log Side (Bark lines)
        g.setColor(new java.awt.Color(100, 50, 20));
        g.fillRect(144, 0, 16, 16);
        g.setColor(new java.awt.Color(90, 40, 10));
        for (int i = 0; i < 16; i += 2)
            g.fillRect(144, i, 16, 1);

        // 10: Planks (Horizontal lines)
        g.setColor(new java.awt.Color(222, 184, 135));
        g.fillRect(160, 0, 16, 16);
        g.setColor(new java.awt.Color(180, 140, 100));
        g.drawRect(160, 0, 15, 15);
        g.drawLine(160, 4, 175, 4);
        g.drawLine(160, 8, 175, 8);
        g.drawLine(160, 12, 175, 12);

        // 11: Leaves (Green Noise)
        drawNoiseBlock(g, 176, 0, new java.awt.Color(34, 139, 34), new java.awt.Color(50, 160, 50));

        // 12: Glass (Clear with Frame)
        // No background fill = transparent
        g.setColor(java.awt.Color.WHITE);
        g.drawRect(192, 0, 15, 15);
        // Glint
        g.setColor(new java.awt.Color(255, 255, 255, 150));
        g.fillRect(192 + 4, 3, 2, 1);
        g.fillRect(192 + 5, 4, 1, 2);
        g.fillRect(192 + 10, 10, 2, 1);

        // 13: Water (Lighter Blue, Translucent)
        g.setColor(new java.awt.Color(40, 160, 255, 140));
        g.fillRect(208, 0, 16, 16);

        // 14: Brick (Red pattern)
        g.setColor(new java.awt.Color(150, 50, 50));
        g.fillRect(224, 0, 16, 16);
        g.setColor(new java.awt.Color(200, 200, 200)); // Grout
        g.drawRect(224, 0, 15, 15);
        g.drawLine(224, 8, 239, 8); // Middle line

        // 15: Coal Ore
        drawNoiseBlock(g, 240, 0, new java.awt.Color(128, 128, 128), new java.awt.Color(140, 140, 140));
        g.setColor(java.awt.Color.BLACK);
        g.fillRect(240 + 6, 6, 4, 4);
        g.fillRect(240 + 2, 10, 3, 3);
        g.fillRect(240 + 10, 2, 3, 3);

        // --- ROW 2 ---

        // 16: Iron Ore
        drawNoiseBlock(g, 0, 16, new java.awt.Color(128, 128, 128), new java.awt.Color(140, 140, 140));
        g.setColor(new java.awt.Color(210, 180, 140));
        g.fillRect(0 + 6, 16 + 6, 4, 4);
        g.fillRect(0 + 2, 16 + 10, 3, 3);

        // 17: Gold Ore
        drawNoiseBlock(g, 16, 16, new java.awt.Color(128, 128, 128), new java.awt.Color(140, 140, 140));
        g.setColor(java.awt.Color.YELLOW);
        g.fillRect(16 + 6, 16 + 6, 4, 4);
        g.fillRect(16 + 2, 16 + 10, 3, 3);

        // 18: Diamond Ore
        drawNoiseBlock(g, 32, 16, new java.awt.Color(128, 128, 128), new java.awt.Color(140, 140, 140));
        g.setColor(java.awt.Color.CYAN);
        g.fillRect(32 + 6, 16 + 6, 4, 4);
        g.fillRect(32 + 2, 16 + 10, 3, 3);

        // 19: Snow Top
        drawNoiseBlock(g, 48, 16, java.awt.Color.WHITE, new java.awt.Color(240, 240, 240));

        // 20: Snow Side
        drawNoiseBlock(g, 64, 16, new java.awt.Color(139, 69, 19), new java.awt.Color(160, 82, 45)); // Dirt
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(64, 16, 16, 4); // Snow cap

        // 21: Ice
        g.setColor(new java.awt.Color(180, 220, 255, 255)); // Opaque light blue
        g.fillRect(80, 16, 16, 16);

        // 22: STICK - TRANSPARENT background, diagonal stick only (THICKER)
        // NO background fill - transparent
        // DIAGONAL STICK - thicker 3x3 pixels
        g.setColor(new java.awt.Color(130, 85, 40)); // Main brown
        // Draw thick diagonal stick
        g.fillRect(96 + 1, 16 + 12, 3, 3);
        g.fillRect(96 + 2, 16 + 11, 3, 3);
        g.fillRect(96 + 3, 16 + 10, 3, 3);
        g.fillRect(96 + 4, 16 + 9, 3, 3);
        g.fillRect(96 + 5, 16 + 8, 3, 3);
        g.fillRect(96 + 6, 16 + 7, 3, 3);
        g.fillRect(96 + 7, 16 + 6, 3, 3);
        g.fillRect(96 + 8, 16 + 5, 3, 3);
        g.fillRect(96 + 9, 16 + 4, 3, 3);
        g.fillRect(96 + 10, 16 + 3, 3, 3);
        g.fillRect(96 + 11, 16 + 2, 3, 3);
        g.fillRect(96 + 12, 16 + 1, 3, 3);
        // Highlight edge (top-right of stick)
        g.setColor(new java.awt.Color(170, 120, 65));
        g.fillRect(96 + 3, 16 + 12, 1, 2);
        g.fillRect(96 + 4, 16 + 11, 1, 2);
        g.fillRect(96 + 5, 16 + 10, 1, 2);
        g.fillRect(96 + 6, 16 + 9, 1, 2);
        g.fillRect(96 + 7, 16 + 8, 1, 2);
        g.fillRect(96 + 8, 16 + 7, 1, 2);
        g.fillRect(96 + 9, 16 + 6, 1, 2);
        g.fillRect(96 + 10, 16 + 5, 1, 2);
        g.fillRect(96 + 11, 16 + 4, 1, 2);
        g.fillRect(96 + 12, 16 + 3, 1, 2);
        g.fillRect(96 + 13, 16 + 2, 1, 2);
        // Shadow edge (bottom-left of stick)
        g.setColor(new java.awt.Color(90, 55, 20));
        g.fillRect(96 + 1, 16 + 14, 2, 1);
        g.fillRect(96 + 2, 16 + 13, 2, 1);
        g.fillRect(96 + 3, 16 + 12, 2, 1);
        g.fillRect(96 + 4, 16 + 11, 2, 1);
        g.fillRect(96 + 5, 16 + 10, 2, 1);
        g.fillRect(96 + 6, 16 + 9, 2, 1);
        g.fillRect(96 + 7, 16 + 8, 2, 1);
        g.fillRect(96 + 8, 16 + 7, 2, 1);
        g.fillRect(96 + 9, 16 + 6, 2, 1);
        g.fillRect(96 + 10, 16 + 5, 2, 1);
        g.fillRect(96 + 11, 16 + 4, 2, 1);

        // 23: CRAFTING TABLE TOP - Exact Minecraft style 3x3 grid
        // Dark brown/black border frame
        g.setColor(new java.awt.Color(45, 30, 18));
        g.fillRect(112, 16, 16, 16);

        // 3x3 grid of tan/brown crafting cells
        // Cell size: 4x4 pixels, with 1px gaps
        // Grid starts at (1,1) inside the border
        java.awt.Color cellLight = new java.awt.Color(186, 139, 91); // Tan/light brown
        java.awt.Color cellMid = new java.awt.Color(166, 119, 71); // Medium brown
        java.awt.Color cellDark = new java.awt.Color(140, 100, 60); // Darker brown

        // Draw 9 cells (3x3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int cx = 113 + col * 5; // 5 = 4px cell + 1px gap
                int cy = 17 + row * 5;

                // Cell base
                g.setColor(cellMid);
                g.fillRect(cx, cy, 4, 4);

                // Lighter top-left highlight
                g.setColor(cellLight);
                g.fillRect(cx, cy, 2, 2);

                // Darker bottom-right shadow
                g.setColor(cellDark);
                g.fillRect(cx + 2, cy + 2, 2, 2);
            }
        }

        // 24: CRAFTING TABLE SIDE - Oak planks with saw and hammer (Minecraft style)
        // Oak plank base
        g.setColor(new java.awt.Color(186, 139, 91));
        g.fillRect(128, 16, 16, 16);
        // Horizontal plank separations
        g.setColor(new java.awt.Color(140, 100, 60));
        g.drawLine(128, 20, 143, 20);
        g.drawLine(128, 24, 143, 24);
        g.drawLine(128, 28, 143, 28);
        // Dark tool panel (like Minecraft)
        g.setColor(new java.awt.Color(105, 70, 40));
        g.fillRect(130, 18, 12, 11);

        // SAW (left side) - diagonal blade
        g.setColor(new java.awt.Color(190, 190, 195)); // Metal silver
        g.fillRect(131, 19, 1, 6); // Blade vertical part
        g.fillRect(132, 19, 1, 5);
        g.fillRect(133, 19, 1, 4);
        g.fillRect(134, 19, 1, 3);
        // Saw teeth
        g.setColor(new java.awt.Color(150, 150, 155));
        g.fillRect(131, 25, 1, 1);
        g.fillRect(133, 24, 1, 1);
        // Saw handle
        g.setColor(new java.awt.Color(130, 85, 45));
        g.fillRect(131, 26, 2, 2);

        // HAMMER (right side)
        // Iron hammer head (horizontal)
        g.setColor(new java.awt.Color(170, 170, 175));
        g.fillRect(137, 19, 4, 2);
        g.setColor(new java.awt.Color(130, 130, 135)); // Darker bottom
        g.fillRect(137, 21, 4, 1);
        // Wood handle (vertical)
        g.setColor(new java.awt.Color(130, 85, 45));
        g.fillRect(138, 22, 2, 5);
        g.setColor(new java.awt.Color(100, 65, 35)); // Handle shadow
        g.fillRect(138, 22, 1, 5);

        // Border
        g.setColor(new java.awt.Color(100, 65, 40));
        g.drawRect(128, 16, 15, 15);

        // ===== TOOLS (indices 25-36) =====
        // Colors for each tier - tuned for Minecraft accuracy
        java.awt.Color woodHead = new java.awt.Color(160, 115, 60); // Brown
        java.awt.Color stoneHead = new java.awt.Color(130, 130, 130); // Darker Grey
        java.awt.Color ironHead = new java.awt.Color(215, 215, 215); // White-ish
        java.awt.Color diamondHead = new java.awt.Color(50, 220, 210); // Cyan-Blue

        java.awt.Color stickColor = new java.awt.Color(105, 68, 30); // Darker Stick Brown
        java.awt.Color stickShadow = new java.awt.Color(70, 45, 20);

        // Helper arrays for tier colors and texture positions
        java.awt.Color[] tierColors = { woodHead, stoneHead, ironHead, diamondHead };

        // PICKAXES (indices 25-28): row 1, cols 9-12
        for (int tier = 0; tier < 4; tier++) {
            int tx = (9 + tier) * 16; // x positions: 144, 160, 176, 192
            int ty = 16; // row 1
            drawPickaxe(g, tx, ty, tierColors[tier], stickColor, stickShadow);
        }

        // SHOVELS (indices 29-32): row 1, cols 13-15 + row 2, col 0
        for (int tier = 0; tier < 4; tier++) {
            int col = 13 + tier;
            int tx = (col % 16) * 16;
            int ty = (col / 16 + 1) * 16; // row 1 or 2
            drawShovel(g, tx, ty, tierColors[tier], stickColor, stickShadow);
        }

        // AXES (indices 33-36): row 2, cols 1-4
        for (int tier = 0; tier < 4; tier++) {
            int tx = (1 + tier) * 16; // x positions: 16, 32, 48, 64
            int ty = 32; // row 2
            drawAxe(g, tx, ty, tierColors[tier], stickColor, stickShadow);
        }

        g.dispose();

        // Convert to PNG byte array
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try {
            javax.imageio.ImageIO.write(image, "png", baos);
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] bytes = baos.toByteArray();
        ByteBuffer out = org.lwjgl.BufferUtils.createByteBuffer(bytes.length);
        out.put(bytes).flip();
        return out;
    }

    private static void drawNoiseBlock(java.awt.Graphics2D g, int x, int y, java.awt.Color c1, java.awt.Color c2) {
        g.setColor(c1);
        g.fillRect(x, y, 16, 16);
        g.setColor(c2);
        java.util.Random rng = new java.util.Random(x * y + 123);
        for (int i = 0; i < 100; i++) {
            g.fillRect(x + rng.nextInt(16), y + rng.nextInt(16), 1, 1);
        }
    }

    /**
     * Helper to draw a single pixel in the 16x16 icon space.
     */
    /**
     * Draw a tool from a 16x16 char array template.
     * C = Color (Head Base)
     * L = Light (Head Highlight)
     * D = Dark (Head Shadow)
     * O = Outline (Head Outline)
     * S = Stick Base
     * H = Stick Highlight
     * I = Stick Shadow
     * K = Stick Dark Outline
     * . = Transparent
     */
    private static void drawTextureFromData(java.awt.Graphics2D g, int x, int y, String[] data,
            java.awt.Color headBase, java.awt.Color stickBase, java.awt.Color stickShadow) {

        java.awt.Color stickDark = new java.awt.Color(60, 40, 10);
        java.awt.Color stickLight = new java.awt.Color(160, 110, 60);

        java.awt.Color headLight = headBase.brighter();
        java.awt.Color headDark = headBase.darker();
        java.awt.Color headOutline = headDark.darker();

        for (int row = 0; row < 16; row++) {
            for (int col = 0; col < 16; col++) {
                char c = data[row].charAt(col);
                if (c == '.')
                    continue;

                java.awt.Color color = null;
                switch (c) {
                    case 'C':
                        color = headBase;
                        break;
                    case 'L':
                        color = headLight;
                        break;
                    case 'D':
                        color = headDark;
                        break;
                    case 'O':
                        color = headOutline;
                        break;
                    case 'S':
                        color = stickBase;
                        break;
                    case 'H':
                        color = stickLight;
                        break;
                    case 'I':
                        color = stickShadow;
                        break;
                    case 'K':
                        color = stickDark;
                        break;
                }

                if (color != null) {
                    g.setColor(color);
                    g.fillRect(x + col, y + row, 1, 1);
                }
            }
        }
    }

    private static void drawPickaxe(java.awt.Graphics2D g, int x, int y,
            java.awt.Color headBase, java.awt.Color stickBase, java.awt.Color stickShadow) {
        String[] data = {
                ".......OOO......", // 0
                ".....OOLCLOO....", // 1
                "....OLLCCCLDO...", // 2
                "...OLCCCCCDDKO..", // 3
                "...OCCCSHCDKO...", // 4
                "..OCDCSISDKO....", // 5
                "..OD.KIIKDO.....", // 6
                ".OO..KSHK.......", // 7
                ".O...KISK.......", // 8
                ".....KSHK.......", // 9
                ".....KISK.......", // 10
                "....KSHK........", // 11
                "....KISK........", // 12
                "...KSHK.........", // 13
                "...KIK..........", // 14
                "...KK..........." // 15
        };
        drawTextureFromData(g, x, y, data, headBase, stickBase, stickShadow);
    }

    private static void drawShovel(java.awt.Graphics2D g, int x, int y,
            java.awt.Color headBase, java.awt.Color stickBase, java.awt.Color stickShadow) {
        String[] data = {
                "................", // 0
                "............OOOO", // 1
                "...........OLLCO", // 2
                "..........OLCCDO", // 3
                "..........OCCDDO", // 4
                ".........OLLCDO.", // 5
                "........OLCCDO..", // 6
                ".......OLCCDO...", // 7
                "......OLCCDOK...", // 8
                "......OCCDO.K...", // 9
                ".......OOO.KSK..", // 10
                "..........KISK..", // 11
                ".........KSHK...", // 12
                "........KISK....", // 13
                ".......KSHK.....", // 14
                ".......KIK......" // 15
        };
        drawTextureFromData(g, x, y, data, headBase, stickBase, stickShadow);
    }

    private static void drawAxe(java.awt.Graphics2D g, int x, int y,
            java.awt.Color headBase, java.awt.Color stickBase, java.awt.Color stickShadow) {
        String[] data = {
                "...........OO...", // 0
                ".........OOLL...", // 1
                "........OLLCC...", // 2
                ".......OLCCCC...", // 3
                ".......OLCCCOO..", // 4
                ".......OLCCCDO..", // 5
                "......OKCCCDO...", // 6
                "......KSHCDO....", // 7
                "......KISDO.....", // 8
                ".....KSHKO......", // 9
                ".....KISK.......", // 10
                "....KSHK........", // 11
                "....KISK........", // 12
                "...KSHK.........", // 13
                "...KIK..........", // 14
                "...KK..........." // 15
        };
        drawTextureFromData(g, x, y, data, headBase, stickBase, stickShadow);
    }

    public void bind() {
        bind(0);
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getId() {
        return textureId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void cleanup() {
        glDeleteTextures(textureId);
    }
}
