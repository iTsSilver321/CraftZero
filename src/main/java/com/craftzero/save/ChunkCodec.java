package com.craftzero.save;

import com.craftzero.world.Chunk;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Binary RLE codec for modified chunk block IDs, metadata, and cached lighting.
 */
public final class ChunkCodec {
    private static final int MAGIC = 0x435A4331; // CZC1
    private static final int VERSION_BLOCK_IDS_ONLY = 1;
    private static final int VERSION_BLOCKS_WITH_METADATA = 2;
    private static final int VERSION_BLOCKS_WITH_LIGHTING = 3;

    private ChunkCodec() {
    }

    public static void write(Path path, Chunk chunk) throws IOException {
        chunk.calculateSkyLight();
        write(path, chunk.copyBlockIds(), chunk.copyBlockMetadata(), chunk.copySkyLight(),
                chunk.copyBlockLight(), chunk.copyHeightMap());
    }

    public static void write(Path path, short[] blocks, byte[] metadata) throws IOException {
        write(path, blocks, metadata, null, null, null);
    }

    public static void write(Path path, short[] blocks, byte[] metadata, byte[] skyLight,
            byte[] blockLight, int[] heightMap) throws IOException {
        if (blocks.length != Chunk.TOTAL_BLOCKS || metadata.length != Chunk.TOTAL_BLOCKS) {
            throw new IOException("Chunk dimensions do not match runtime");
        }
        boolean hasLighting = hasCompleteLightingData(skyLight, blockLight, heightMap);

        SafeFiles.writeAtomicBytes(path, stream -> {
            DataOutputStream out = new DataOutputStream(stream);
            out.writeInt(MAGIC);
            out.writeInt(hasLighting ? VERSION_BLOCKS_WITH_LIGHTING : VERSION_BLOCKS_WITH_METADATA);
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
            if (hasLighting) {
                out.writeInt(skyLight.length);
                out.write(skyLight);
                out.writeInt(blockLight.length);
                out.write(blockLight);
                out.writeInt(heightMap.length);
                for (int heightValue : heightMap) {
                    out.writeInt(heightValue);
                }
            }
            out.flush();
        }, SafeFiles.BackupPolicy.BAK);
    }

    public static ChunkData read(Path path) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new IOException("Invalid chunk magic: " + Integer.toHexString(magic));
            }

            int version = in.readInt();
            if (version != VERSION_BLOCK_IDS_ONLY
                    && version != VERSION_BLOCKS_WITH_METADATA
                    && version != VERSION_BLOCKS_WITH_LIGHTING) {
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
                byte meta = version == VERSION_BLOCK_IDS_ONLY ? 0 : in.readByte();
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

            if (version == VERSION_BLOCKS_WITH_LIGHTING) {
                byte[] skyLight = readByteArray(in, Chunk.LIGHT_DATA_BYTES, "sky light");
                byte[] blockLight = readByteArray(in, Chunk.LIGHT_DATA_BYTES, "block light");
                int[] heightMap = readHeightMap(in);
                return new ChunkData(blocks, metadata, skyLight, blockLight, heightMap);
            }
            return new ChunkData(blocks, metadata);
        }
    }

    public static short[] readBlockIds(Path path) throws IOException {
        return read(path).blockIds();
    }

    private static boolean hasCompleteLightingData(byte[] skyLight, byte[] blockLight, int[] heightMap)
            throws IOException {
        if (skyLight == null && blockLight == null && heightMap == null) {
            return false;
        }
        if (skyLight == null || blockLight == null || heightMap == null) {
            throw new IOException("Incomplete chunk lighting payload");
        }
        if (skyLight.length != Chunk.LIGHT_DATA_BYTES || blockLight.length != Chunk.LIGHT_DATA_BYTES
                || heightMap.length != Chunk.HEIGHT_MAP_SIZE) {
            throw new IOException("Chunk lighting dimensions do not match runtime");
        }
        return true;
    }

    private static byte[] readByteArray(DataInputStream in, int expectedLength, String label) throws IOException {
        int length = in.readInt();
        if (length != expectedLength) {
            throw new IOException("Invalid " + label + " length: " + length);
        }
        byte[] values = new byte[length];
        in.readFully(values);
        return values;
    }

    private static int[] readHeightMap(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length != Chunk.HEIGHT_MAP_SIZE) {
            throw new IOException("Invalid height map length: " + length);
        }
        int[] values = new int[length];
        for (int i = 0; i < values.length; i++) {
            values[i] = in.readInt();
        }
        return values;
    }

    public record ChunkData(short[] blockIds, byte[] metadata, byte[] skyLight, byte[] blockLight, int[] heightMap) {
        public ChunkData(short[] blockIds, byte[] metadata) {
            this(blockIds, metadata, null, null, null);
        }

        public boolean hasLightingData() {
            return skyLight != null && blockLight != null && heightMap != null;
        }
    }
}
