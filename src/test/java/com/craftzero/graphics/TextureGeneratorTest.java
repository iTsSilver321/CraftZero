package com.craftzero.graphics;

import com.craftzero.world.Block;
import com.craftzero.world.BlockType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextureGeneratorTest {
    private static final int CELL_SIZE = 16;
    private static final int CRAFTING_SIDE_X = 11 * CELL_SIZE;
    private static final int CRAFTING_SIDE_Y = 3 * CELL_SIZE;

    @Test
    @DisplayName("Generated fallback atlas should keep the classic 16 by 16 tile layout")
    void generatedAtlasKeepsExpectedTileLayout() {
        BufferedImage atlas = TextureGenerator.createAtlasImage();

        assertEquals(256, atlas.getWidth());
        assertEquals(256, atlas.getHeight());
    }

    @Test
    @DisplayName("Fallback crafting table side should show distinct panel and tool shapes")
    void craftingTableSideHasWorkbenchToolSilhouettes() {
        BufferedImage atlas = TextureGenerator.createAtlasImage();
        float[] craftingSideUv = BlockType.CRAFTING_TABLE.getTextureCoords(Block.FACE_NORTH);

        assertEquals(11, (int) (craftingSideUv[0] * 16.0f));
        assertEquals(3, (int) (craftingSideUv[1] * 16.0f));

        assertPixel(atlas, 4, 8, new Color(126, 82, 47), "inset panel");
        assertPixel(atlas, 9, 3, new Color(174, 174, 162), "saw blade metal");
        assertPixel(atlas, 3, 4, new Color(174, 174, 162), "hammer head metal");
        assertPixel(atlas, 6, 8, new Color(157, 98, 48), "hammer handle highlight");
        assertPixel(atlas, 12, 11, new Color(174, 174, 162), "saw tooth");
    }

    @Test
    @DisplayName("Generated terrain atlas should populate higher classic Terrain.png cells")
    void generatedAtlasPopulatesHigherTerrainCells() {
        BufferedImage atlas = TextureGenerator.createAtlasImage();

        assertEquals(new Color(220, 210, 200).getRGB(), atlas.getRGB(1 * CELL_SIZE + 4, 2 * CELL_SIZE + 4),
                "iron ore gem pixel");
    }

    @Test
    @DisplayName("Generated terrain fallback should encode as a readable PNG")
    void generatedTerrainFallbackEncodesAsPng() throws Exception {
        byte[] encoded = TextureGenerator.createAtlasPngBytes();

        assertTrue(encoded.length > 0);
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(encoded));
        assertNotNull(decoded);
        assertEquals(256, decoded.getWidth());
        assertEquals(256, decoded.getHeight());
    }

    private static void assertPixel(BufferedImage atlas, int localX, int localY, Color expected, String label) {
        assertEquals(expected.getRGB(),
                atlas.getRGB(CRAFTING_SIDE_X + localX, CRAFTING_SIDE_Y + localY),
                label);
    }
}
