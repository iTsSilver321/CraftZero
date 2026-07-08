package com.craftzero.graphics;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CloudRendererTest {

    @Test
    void releaseCloudLayerUsesClassicAssetHeightAndTileSize() {
        assertEquals("/textures/environment/clouds.png", CloudRenderer.CLOUD_TEXTURE_RESOURCE);
        assertEquals(108.0f, CloudRenderer.RELEASE_CLOUD_HEIGHT, 0.0001f);
        assertEquals(256.0f, CloudRenderer.CLOUD_TILE_SIZE, 0.0001f);
    }

    @Test
    void cloudTextureResourceIsBundledClassicSheet() throws Exception {
        try (InputStream stream = CloudRendererTest.class.getResourceAsStream(CloudRenderer.CLOUD_TEXTURE_RESOURCE)) {
            assertNotNull(stream);
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image);
            assertEquals(256, image.getWidth());
            assertEquals(256, image.getHeight());
        }
    }

    @Test
    void cloudBrightnessTracksSunAndDimensionMultiplier() {
        assertEquals(0.95f, CloudRenderer.cloudBrightness(1.0f, 1.0f), 0.0001f);
        assertEquals(0.475f, CloudRenderer.cloudBrightness(1.0f, 0.5f), 0.0001f);
        assertEquals(0.0f, CloudRenderer.cloudBrightness(-1.0f, 1.0f), 0.0001f);
        assertEquals(0.95f, CloudRenderer.cloudBrightness(2.0f, 3.0f), 0.0001f);
    }

    @Test
    void scrollOffsetWrapsByOneCloudTextureTile() {
        assertEquals(0.0f, CloudRenderer.normalizedScrollOffset(0.0f), 0.0001f);
        assertEquals(-4.0f, CloudRenderer.normalizedScrollOffset(-260.0f), 0.0001f);
        assertEquals(-252.0f, CloudRenderer.normalizedScrollOffset(4.0f), 0.0001f);
    }

    @Test
    void tileCentersStaySeamlessAroundCameraAndScrollOffset() {
        float center = CloudRenderer.cloudTileCenter(64.0f, -10.0f, 0);
        assertEquals(-10.0f, center, 0.0001f);
        assertEquals(center + CloudRenderer.CLOUD_TILE_SIZE,
                CloudRenderer.cloudTileCenter(64.0f, -10.0f, 1), 0.0001f);
        assertEquals(502.0f, CloudRenderer.cloudTileCenter(520.0f, -10.0f, 0), 0.0001f);
    }
}
