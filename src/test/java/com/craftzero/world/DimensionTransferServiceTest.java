package com.craftzero.world;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DimensionTransferServiceTest {
    @Test
    @DisplayName("End portal transfer should use the Release 1.0 fixed End entry target")
    void endPortalUsesReleaseOneEntryTarget() {
        DimensionTransferService.TransferTarget end = DimensionTransferService.fromEndPortal(
                Dimension.OVERWORLD, 12, 70, -4);

        assertSame(Dimension.THE_END, end.dimension());
        assertEquals(100.5f, end.x(), 0.0001f);
        assertEquals(49.0f, end.y(), 0.0001f);
        assertEquals(0.5f, end.z(), 0.0001f);
        assertFalse(end.prepareNetherPortal());

        DimensionTransferService.TransferTarget overworld = DimensionTransferService.fromEndPortal(
                Dimension.THE_END, 12, 70, -4);

        assertSame(Dimension.OVERWORLD, overworld.dimension());
        assertEquals(12.5f, overworld.x(), 0.0001f);
        assertEquals(70.0f, overworld.y(), 0.0001f);
        assertEquals(-3.5f, overworld.z(), 0.0001f);
    }

    @Test
    @DisplayName("Nether portal transfer should use the Release 1.0 8:1 coordinate ratio")
    void netherPortalUsesEightToOneCoordinateRatio() {
        DimensionTransferService.TransferTarget nether = DimensionTransferService.fromNetherPortal(
                Dimension.OVERWORLD, 80.5f, 72.0f, -16.0f);

        assertNotNull(nether);
        assertSame(Dimension.NETHER, nether.dimension());
        assertEquals(10.0625f, nether.x(), 0.0001f);
        assertEquals(72.0f, nether.y(), 0.0001f);
        assertEquals(-2.0f, nether.z(), 0.0001f);
        assertTrue(nether.prepareNetherPortal());

        DimensionTransferService.TransferTarget overworld = DimensionTransferService.fromNetherPortal(
                Dimension.NETHER, 10.0625f, 72.0f, -2.0f);

        assertNotNull(overworld);
        assertSame(Dimension.OVERWORLD, overworld.dimension());
        assertEquals(80.5f, overworld.x(), 0.0001f);
        assertEquals(-16.0f, overworld.z(), 0.0001f);
        assertTrue(overworld.prepareNetherPortal());
    }

    @Test
    @DisplayName("Nether portal targets should clamp vertical placement and ignore The End")
    void netherPortalTargetsClampVerticalPlacement() {
        DimensionTransferService.TransferTarget low = DimensionTransferService.fromNetherPortal(
                Dimension.OVERWORLD, 0.0f, -20.0f, 0.0f);
        DimensionTransferService.TransferTarget high = DimensionTransferService.fromNetherPortal(
                Dimension.NETHER, 0.0f, Chunk.HEIGHT + 20.0f, 0.0f);

        assertNotNull(low);
        assertEquals(4.0f, low.y(), 0.0001f);
        assertNotNull(high);
        assertEquals(Chunk.HEIGHT - 6.0f, high.y(), 0.0001f);
        assertNull(DimensionTransferService.fromNetherPortal(Dimension.THE_END, 0.0f, 72.0f, 0.0f));
    }
}
