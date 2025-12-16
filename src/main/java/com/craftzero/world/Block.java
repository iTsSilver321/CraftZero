package com.craftzero.world;

/**
 * Block utility class for generating face vertices and UV coordinates.
 */
public class Block {

        // Face indices
        public static final int FACE_TOP = 0;
        public static final int FACE_BOTTOM = 1;
        public static final int FACE_NORTH = 2; // -Z
        public static final int FACE_SOUTH = 3; // +Z
        public static final int FACE_EAST = 4; // +X
        public static final int FACE_WEST = 5; // -X

        /**
         * Get vertex positions for a face at block position (x, y, z).
         * Returns 12 floats (4 vertices * 3 components).
         */
        public static float[] getFaceVertices(int face, int x, int y, int z) {
                float x0 = x;
                float y0 = y;
                float z0 = z;
                float x1 = x + 1;
                float y1 = y + 1;
                float z1 = z + 1;

                // Standard CCW Winding: TL -> BL -> BR -> TR
                return switch (face) {
                        case FACE_TOP -> new float[] {
                                        x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0
                        };
                        case FACE_BOTTOM -> new float[] {
                                        x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1
                        };
                        case FACE_NORTH -> new float[] {
                                        x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0
                        };
                        case FACE_SOUTH -> new float[] {
                                        x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1
                        };
                        case FACE_EAST -> new float[] {
                                        x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0
                        };
                        case FACE_WEST -> new float[] {
                                        x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1
                        };
                        default -> new float[12];
                };
        }

        /**
         * Get texture coordinates for a face.
         * Returns 8 floats (4 vertices * 2 components).
         */
        public static float[] getFaceTexCoords(BlockType type, int face) {
                float[] uv = type.getTextureCoords(face);
                float u1 = uv[0], v1 = uv[1], u2 = uv[2], v2 = uv[3];

                // UV layout matching TL -> BL -> BR -> TR
                return new float[] {
                                u1, v1, u1, v2, u2, v2, u2, v1
                };
        }

        /**
         * Get normal vector for a face.
         * Returns 12 floats (4 vertices * 3 components, same normal for all vertices).
         */
        public static float[] getFaceNormals(int face) {
                float nx = 0, ny = 0, nz = 0;

                switch (face) {
                        case FACE_TOP -> ny = 1;
                        case FACE_BOTTOM -> ny = -1;
                        case FACE_NORTH -> nz = -1;
                        case FACE_SOUTH -> nz = 1;
                        case FACE_EAST -> nx = 1;
                        case FACE_WEST -> nx = -1;
                }

                return new float[] {
                                nx, ny, nz, nx, ny, nz, nx, ny, nz, nx, ny, nz
                };
        }

        /**
         * Get indices for a face (2 triangles = 6 indices).
         * 
         * @param baseIndex The starting vertex index for this face.
         */
        public static int[] getFaceIndices(int baseIndex) {
                return new int[] {
                                baseIndex, baseIndex + 1, baseIndex + 2,
                                baseIndex + 2, baseIndex + 3, baseIndex
                };
        }
}
