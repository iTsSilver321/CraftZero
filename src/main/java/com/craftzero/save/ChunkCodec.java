package com.craftzero.save;

import com.craftzero.world.Chunk;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Binary RLE codec for modified chunk block IDs and metadata.
 */
public final class ChunkCodec {
    private static final int MAGIC = 0x435A4331; // CZC1
    private static final int VERSION_BLOCK_IDS_ONLY = 1;
    private static final int VERSION_BLOCKS_WITH_METADATA = 2;

    private ChunkCodec() {
    }

    public static void write(Path path, Chunk chunk) throws IOException {
        write(path, chunk.copyBlockIds(), chunk.copyBlockMetadata());
    }

    public static void write(Path path, short[] blocks, byte[] metadata) throws IOException {
        if (blocks.length != Chunk.TOTAL_BLOCKS || metadata.length != Chunk.TOTAL_BLOCKS) {
            throw new IOException("Chunk dimensions do not match runtime");
        }

        SafeFiles.writeAtomicBytes(path, stream -> {
            DataOutputStream out = new DataOutputStream(stream);
            out.writeInt(MAGIC);
            out.writeInt(VERSION_BLOCKS_WITH_METADATA);
            out.writeInt(Chunk.WIDTH);
            out.writeInt(Chunk.HEIGHT);
            out.writeInt(Chunk.DEPTH);
            out.writeInt(blocks.length);

            int index = 0;
            while (index < blocks.length) {
                short id = blocks[index];
                byte meta = metadata[index];
                int count = 1;
                while (index + count < blocks.length
                        && blocks[index + count] == id
                        && metadata[index + count] == meta) {
                    count++;
                }
                out.writeShort(id);
                out.writeByte(meta);
                out.writeInt(count);
                index += count;
            }
            out.flush();
        }, SafeFiles.BackupPolicy.NONE);
    }

    public static ChunkData read(Path path) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new IOException("Invalid chunk magic: " + Integer.toHexString(magic));
            }

            int version = in.readInt();
            if (version != VERSION_BLOCK_IDS_ONLY && version != VERSION_BLOCKS_WITH_METADATA) {
                throw new IOException("Unsupported chunk version: " + version);
            }

            int width = in.readInt();
            int height = in.readInt();
            int depth = in.readInt();
            int total = in.readInt();
            if (width != Chunk.WIDTH || height != Chunk.HEIGHT || depth != Chunk.DEPTH
                    || total != Chunk.TOTAL_BLOCKS) {
                throw new IOException("Chunk dimensions do not match runtime");
            }

            short[] blocks = new short[total];
            byte[] metadata = new byte[total];
            int index = 0;
            while (index < total) {
                short id = in.readShort();
                byte meta = version == VERSION_BLOCKS_WITH_METADATA ? in.readByte() : 0;
                int count = in.readInt();
                if (count <= 0 || index + count > total) {
                    throw new IOException("Invalid chunk RLE run length: " + count);
                }
                for (int i = 0; i < count; i++) {
                    blocks[index] = id;
                    metadata[index] = meta;
                    index++;
                }
            }
            return new ChunkData(blocks, metadata);
        }
    }

    public static short[] readBlockIds(Path path) throws IOException {
        return read(path).blockIds();
    }

    public record ChunkData(short[] blockIds, byte[] metadata) {
    }
}
