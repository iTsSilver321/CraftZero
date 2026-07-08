package com.craftzero.save;

import com.craftzero.world.BlockType;
import com.craftzero.world.Chunk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.io.DataOutputStream;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class ChunkCodecTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Chunk height should match Minecraft Release 1.0")
    void chunkHeightMatchesReleaseOne() {
        assertEquals(128, Chunk.HEIGHT);
        assertEquals(16 * 128 * 16, Chunk.TOTAL_BLOCKS);
    }

    @Test
    @DisplayName("Chunk RLE codec should round trip block IDs")
    void rleRoundTripsBlockIds() throws Exception {
        Chunk chunk = new Chunk(2, -3);
        chunk.setBlock(0, 0, 0, BlockType.BEDROCK);
        chunk.setBlock(1, 70, 1, BlockType.DIAMOND_ORE);
        chunk.setBlock(15, 127, 15, BlockType.SNOW);

        Path path = tempDir.resolve("c.2.-3.bin");
        ChunkCodec.write(path, chunk);

        ChunkCodec.ChunkData data = ChunkCodec.read(path);
        assertArrayEquals(chunk.copyBlockIds(), data.blockIds());
        assertArrayEquals(chunk.copyBlockMetadata(), data.metadata());
    }

    @Test
    @DisplayName("Chunk RLE codec should round trip metadata")
    void rleRoundTripsMetadata() throws Exception {
        Chunk chunk = new Chunk(0, 0);
        chunk.setBlock(3, 70, 4, BlockType.FURNACE, 5);
        chunk.setBlock(4, 70, 4, BlockType.CHEST, 2);

        Path path = tempDir.resolve("c.0.0.bin");
        ChunkCodec.write(path, chunk);

        ChunkCodec.ChunkData data = ChunkCodec.read(path);
        assertArrayEquals(chunk.copyBlockIds(), data.blockIds());
        assertArrayEquals(chunk.copyBlockMetadata(), data.metadata());
    }

    @Test
    @DisplayName("Chunk codec should keep a backup of the previous chunk file")
    void chunkWritesKeepPreviousBackup() throws Exception {
        Path path = tempDir.resolve("c.0.0.bin");
        Chunk first = new Chunk(0, 0);
        first.setBlock(1, 70, 1, BlockType.DIAMOND_ORE);
        Chunk second = new Chunk(0, 0);
        second.setBlock(1, 70, 1, BlockType.GOLD_ORE);

        ChunkCodec.write(path, first);
        ChunkCodec.write(path, second);

        assertTrue(Files.exists(SafeFiles.backupPath(path)));
        ChunkCodec.ChunkData current = ChunkCodec.read(path);
        ChunkCodec.ChunkData backup = ChunkCodec.read(SafeFiles.backupPath(path));
        assertEquals(BlockType.GOLD_ORE.getId(), current.blockIds()[Chunk.getIndex(1, 70, 1)]);
        assertEquals(BlockType.DIAMOND_ORE.getId(), backup.blockIds()[Chunk.getIndex(1, 70, 1)]);
    }

    @Test
    @DisplayName("Chunk codec should load v1 block-only chunks with zero metadata")
    void readsVersionOneChunks() throws Exception {
        Path path = tempDir.resolve("old.bin");
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(path))) {
            out.writeInt(0x435A4331);
            out.writeInt(1);
            out.writeInt(Chunk.WIDTH);
            out.writeInt(Chunk.HEIGHT);
            out.writeInt(Chunk.DEPTH);
            out.writeInt(Chunk.TOTAL_BLOCKS);
            out.writeShort(BlockType.STONE.getId());
            out.writeInt(Chunk.TOTAL_BLOCKS);
        }

        ChunkCodec.ChunkData data = ChunkCodec.read(path);
        assertEquals(BlockType.STONE.getId(), data.blockIds()[0]);
        for (byte metadata : data.metadata()) {
            assertEquals(0, metadata);
        }
    }

    @Test
    @DisplayName("Chunk modified tracking should distinguish generation from edits")
    void chunkModifiedTrackingWorks() {
        Chunk chunk = new Chunk(0, 0);
        assertFalse(chunk.isModified());

        chunk.setBlock(1, 2, 3, BlockType.STONE);
        assertTrue(chunk.isModified());

        chunk.clearModified();
        assertFalse(chunk.isModified());

        chunk.setBlock(1, 2, 3, BlockType.DIRT);
        assertTrue(chunk.isModified());
    }
}
