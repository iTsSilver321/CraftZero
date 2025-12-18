package com.craftzero.graphics;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Loads and samples biome colormaps from Minecraft texture assets.
 * Used to tint grass, foliage (leaves), and water with appropriate biome
 * colors.
 */
public class BiomeColormap {

    // Default colors sampled from colormap images (plains/forest biome area)
    private static float[] grassColor = { 0.486f, 0.741f, 0.318f }; // ~(124, 189, 81)
    private static float[] foliageColor = { 0.376f, 0.624f, 0.255f }; // ~(96, 159, 65)
    private static float[] waterColor = { 0.247f, 0.463f, 0.894f }; // ~(63, 118, 228) default blue

    static {
        loadColormaps();
    }

    private static void loadColormaps() {
        try {
            // Load grasscolor.png
            InputStream grassIs = BiomeColormap.class.getResourceAsStream("/textures/misc/grasscolor.png");
            if (grassIs != null) {
                BufferedImage grassImg = ImageIO.read(grassIs);
                grassIs.close();
                // Sample from middle-ish area (typical plains/forest climate)
                int rgb = grassImg.getRGB(128, 128);
                grassColor[0] = ((rgb >> 16) & 0xFF) / 255f;
                grassColor[1] = ((rgb >> 8) & 0xFF) / 255f;
                grassColor[2] = (rgb & 0xFF) / 255f;
                System.out
                        .println("Loaded grass color: " + grassColor[0] + ", " + grassColor[1] + ", " + grassColor[2]);
            }

            // Load foliagecolor.png
            InputStream foliageIs = BiomeColormap.class.getResourceAsStream("/textures/misc/foliagecolor.png");
            if (foliageIs != null) {
                BufferedImage foliageImg = ImageIO.read(foliageIs);
                foliageIs.close();
                int rgb = foliageImg.getRGB(128, 128);
                foliageColor[0] = ((rgb >> 16) & 0xFF) / 255f;
                foliageColor[1] = ((rgb >> 8) & 0xFF) / 255f;
                foliageColor[2] = (rgb & 0xFF) / 255f;
                System.out.println(
                        "Loaded foliage color: " + foliageColor[0] + ", " + foliageColor[1] + ", " + foliageColor[2]);
            }

            // Load watercolor.png
            InputStream waterIs = BiomeColormap.class.getResourceAsStream("/textures/misc/watercolor.png");
            if (waterIs != null) {
                BufferedImage waterImg = ImageIO.read(waterIs);
                waterIs.close();
                // Sample from middle area for typical water color
                int rgb = waterImg.getRGB(128, 128);
                waterColor[0] = ((rgb >> 16) & 0xFF) / 255f;
                waterColor[1] = ((rgb >> 8) & 0xFF) / 255f;
                waterColor[2] = (rgb & 0xFF) / 255f;
                System.out
                        .println("Loaded water color: " + waterColor[0] + ", " + waterColor[1] + ", " + waterColor[2]);
            }
        } catch (Exception e) {
            System.err.println("Failed to load biome colormaps, using defaults: " + e.getMessage());
        }
    }

    /**
     * Get grass tint color (for grass block top).
     */
    public static float[] getGrassColor() {
        return grassColor;
    }

    /**
     * Get foliage tint color (for leaves).
     */
    public static float[] getFoliageColor() {
        return foliageColor;
    }

    /**
     * Get water tint color (for water blocks).
     */
    public static float[] getWaterColor() {
        return waterColor;
    }
}
