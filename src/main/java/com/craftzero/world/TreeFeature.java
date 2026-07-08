package com.craftzero.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class TreeFeature {
    private static final byte[] BIG_TREE_AXIS_PAIRS = { 2, 0, 0, 1, 2, 1 };
    static final int CROWN_RADIUS = 2;
    static final int MIN_SPACING = 6;
    static final int MIN_SPACING_SQUARED = MIN_SPACING * MIN_SPACING;

    interface BlockQuery {
        BlockType getBlock(int x, int y, int z);
    }

    private TreeFeature() {
    }

    enum Kind {
        NORMAL,
        BIG,
        SWAMP,
        TAIGA1,
        TAIGA2
    }

    record Candidate(int rootX, int rootY, int rootZ, int height, int priority, int treeMetadata, Kind kind,
            int dataA, int dataB, int dataC, int dataD) {
        Candidate(int rootX, int rootY, int rootZ, int height, int priority) {
            this(rootX, rootY, rootZ, height, priority, 0);
        }

        Candidate(int rootX, int rootY, int rootZ, int height, int priority, int treeMetadata) {
            this(rootX, rootY, rootZ, height, priority, treeMetadata, Kind.NORMAL);
        }

        Candidate(int rootX, int rootY, int rootZ, int height, int priority, int treeMetadata, Kind kind) {
            this(rootX, rootY, rootZ, height, priority, treeMetadata, kind, 0, 0, 0, 0);
        }

        Candidate {
            if (kind == null) {
                kind = Kind.NORMAL;
            }
        }

        boolean intersectsChunk(int chunkX, int chunkZ) {
            int minX = chunkX * Chunk.WIDTH;
            int minZ = chunkZ * Chunk.DEPTH;
            int maxX = minX + Chunk.WIDTH - 1;
            int maxZ = minZ + Chunk.DEPTH - 1;
            int radius = crownRadius();
            return rootX + radius >= minX && rootX - radius <= maxX
                    && rootZ + radius >= minZ && rootZ - radius <= maxZ;
        }

        private int crownRadius() {
            return switch (kind) {
                case BIG -> Math.max(4, height);
                case SWAMP -> 3;
                case TAIGA1, TAIGA2 -> Math.max(1, dataB);
                default -> CROWN_RADIUS;
            };
        }

        boolean conflictsWith(Candidate other) {
            int dx = rootX - other.rootX;
            int dz = rootZ - other.rootZ;
            return dx * dx + dz * dz < MIN_SPACING_SQUARED;
        }

        boolean canPlace(BlockQuery query) {
            if (kind == Kind.SWAMP) {
                return canPlaceSwamp(query);
            }
            if (kind == Kind.BIG) {
                return canPlaceBig(query);
            }
            if (kind == Kind.TAIGA1 || kind == Kind.TAIGA2) {
                return canPlaceTaiga(query);
            }
            return canPlaceNormal(query);
        }

        private boolean canPlaceNormal(BlockQuery query) {
            if (rootY < 1 || rootY + height + 1 > Chunk.HEIGHT) {
                return false;
            }
            for (int y = rootY; y <= rootY + height + 1; y++) {
                int radius = 1;
                if (y == rootY) {
                    radius = 0;
                }
                if (y >= rootY + height - 1) {
                    radius = CROWN_RADIUS;
                }
                for (int x = rootX - radius; x <= rootX + radius; x++) {
                    for (int z = rootZ - radius; z <= rootZ + radius; z++) {
                        if (y < 0 || y >= Chunk.HEIGHT
                                || !isReplaceableForLeaves(query.getBlock(x, y, z))) {
                            return false;
                        }
                    }
                }
            }
            BlockType support = query.getBlock(rootX, rootY - 1, rootZ);
            return (support == BlockType.GRASS || support == BlockType.DIRT)
                    && rootY < Chunk.HEIGHT - height - 1;
        }

        private boolean canPlaceTaiga(BlockQuery query) {
            if (rootY < 1 || rootY + height + 1 > Chunk.HEIGHT) {
                return false;
            }
            int clearStart = Math.max(0, dataA);
            int maxRadius = Math.max(1, dataB);
            for (int y = rootY; y <= rootY + height + 1; y++) {
                int radius = y - rootY < clearStart ? 0 : maxRadius;
                for (int x = rootX - radius; x <= rootX + radius; x++) {
                    for (int z = rootZ - radius; z <= rootZ + radius; z++) {
                        BlockType block = query.getBlock(x, y, z);
                        if (block != BlockType.AIR && block != BlockType.LEAVES) {
                            return false;
                        }
                    }
                }
            }
            BlockType support = query.getBlock(rootX, rootY - 1, rootZ);
            return (support == BlockType.GRASS || support == BlockType.DIRT)
                    && rootY < Chunk.HEIGHT - height - 1;
        }

        void placeInto(Chunk chunk, int chunkX, int chunkZ) {
            if (kind == Kind.SWAMP) {
                placeSwampInto(chunk, chunkX, chunkZ);
                return;
            }
            if (kind == Kind.BIG) {
                placeBigInto(chunk, chunkX, chunkZ);
                return;
            }
            if (kind == Kind.TAIGA1) {
                placeTaiga1Into(chunk, chunkX, chunkZ);
                return;
            }
            if (kind == Kind.TAIGA2) {
                placeTaiga2Into(chunk, chunkX, chunkZ);
                return;
            }
            placeNormalInto(chunk, chunkX, chunkZ);
        }

        void placeInto(World world) {
            if (kind == Kind.SWAMP) {
                placeSwampInto(world);
                return;
            }
            if (kind == Kind.BIG) {
                placeBigInto(world);
                return;
            }
            if (kind == Kind.TAIGA1) {
                placeTaiga1Into(world);
                return;
            }
            if (kind == Kind.TAIGA2) {
                placeTaiga2Into(world);
                return;
            }
            placeNormalInto(world);
        }

        private void placeNormalInto(Chunk chunk, int chunkX, int chunkZ) {
            int metadata = treeMetadata & 3;
            setGroundIfInChunk(chunk, chunkX, chunkZ, rootX, rootY - 1, rootZ);
            int cornerIndex = 0;
            for (int y = rootY - 3 + height; y <= rootY + height; y++) {
                int layerOffset = y - (rootY + height);
                int radius = 1 - layerOffset / 2;
                for (int x = rootX - radius; x <= rootX + radius; x++) {
                    int dx = x - rootX;
                    for (int z = rootZ - radius; z <= rootZ + radius; z++) {
                        int dz = z - rootZ;
                        boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
                        if (corner && (normalLeafCornerSkipped(cornerIndex++) || layerOffset == 0)) {
                            continue;
                        }
                        setIfInChunk(chunk, chunkX, chunkZ, x, y, z, BlockType.LEAVES, metadata, false);
                    }
                }
            }
            for (int i = 0; i < height; i++) {
                setIfInChunk(chunk, chunkX, chunkZ, rootX, rootY + i, rootZ, BlockType.OAK_LOG, metadata, true);
            }
        }

        private void placeNormalInto(World world) {
            int metadata = treeMetadata & 3;
            world.setBlock(rootX, rootY - 1, rootZ, BlockType.DIRT);
            int cornerIndex = 0;
            for (int y = rootY - 3 + height; y <= rootY + height; y++) {
                int layerOffset = y - (rootY + height);
                int radius = 1 - layerOffset / 2;
                for (int x = rootX - radius; x <= rootX + radius; x++) {
                    int dx = x - rootX;
                    for (int z = rootZ - radius; z <= rootZ + radius; z++) {
                        int dz = z - rootZ;
                        boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
                        if (corner && (normalLeafCornerSkipped(cornerIndex++) || layerOffset == 0)) {
                            continue;
                        }
                        setIfReplaceable(world, x, y, z, BlockType.LEAVES, metadata, false);
                    }
                }
            }
            for (int i = 0; i < height; i++) {
                setIfReplaceable(world, rootX, rootY + i, rootZ, BlockType.OAK_LOG, metadata, true);
            }
        }

        private boolean normalLeafCornerSkipped(int cornerIndex) {
            return (dataA & (1 << cornerIndex)) != 0;
        }

        private boolean canPlaceBig(BlockQuery query) {
            if (rootY < 1 || rootY + height + 1 > Chunk.HEIGHT) {
                return false;
            }
            BlockType support = query.getBlock(rootX, rootY - 1, rootZ);
            if (support != BlockType.GRASS && support != BlockType.DIRT) {
                return false;
            }
            int[] start = { rootX, rootY, rootZ };
            int[] end = { rootX, rootY + height - 1, rootZ };
            return checkBigBlockLine(start, end, query) == -1;
        }

        private void placeBigInto(Chunk chunk, int chunkX, int chunkZ) {
            int trunkHeight = bigTrunkHeight();
            List<BigLeafNode> nodes = bigLeafNodes();
            setGroundIfInChunk(chunk, chunkX, chunkZ, rootX, rootY - 1, rootZ);
            for (BigLeafNode node : nodes) {
                placeBigLeafNode(chunk, chunkX, chunkZ, node);
            }
            placeBigBlockLine(chunk, chunkX, chunkZ,
                    new int[] { rootX, rootY, rootZ },
                    new int[] { rootX, rootY + trunkHeight, rootZ });
            for (BigLeafNode node : nodes) {
                if (bigBranchCanGrow(node.branchBaseY() - rootY)) {
                    placeBigBlockLine(chunk, chunkX, chunkZ,
                            new int[] { rootX, node.branchBaseY(), rootZ },
                            new int[] { node.x(), node.y(), node.z() });
                }
            }
        }

        private void placeBigInto(World world) {
            int trunkHeight = bigTrunkHeight();
            List<BigLeafNode> nodes = bigLeafNodes();
            world.setBlock(rootX, rootY - 1, rootZ, BlockType.DIRT);
            for (BigLeafNode node : nodes) {
                placeBigLeafNode(world, node);
            }
            placeBigBlockLine(world,
                    new int[] { rootX, rootY, rootZ },
                    new int[] { rootX, rootY + trunkHeight, rootZ });
            for (BigLeafNode node : nodes) {
                if (bigBranchCanGrow(node.branchBaseY() - rootY)) {
                    placeBigBlockLine(world,
                            new int[] { rootX, node.branchBaseY(), rootZ },
                            new int[] { node.x(), node.y(), node.z() });
                }
            }
        }

        private List<BigLeafNode> bigLeafNodes() {
            Random random = bigTreeRandomAfterHeight();
            int trunkHeight = bigTrunkHeight();
            int nodesPerLayer = (int) (1.382D + Math.pow(height / 13.0D, 2.0D));
            if (nodesPerLayer < 1) {
                nodesPerLayer = 1;
            }

            List<BigLeafNode> nodes = new ArrayList<>();
            int leafY = rootY + height - 4;
            int trunkTopY = rootY + trunkHeight;
            int relativeY = leafY - rootY;
            nodes.add(new BigLeafNode(rootX, leafY, rootZ, trunkTopY));
            leafY--;
            relativeY--;

            while (relativeY >= 0) {
                float layerSize = bigLayerSize(relativeY);
                if (layerSize >= 0.0F) {
                    for (int i = 0; i < nodesPerLayer; i++) {
                        double distance = layerSize * (random.nextFloat() + 0.328D);
                        double angle = random.nextFloat() * 2.0D * Math.PI;
                        int nodeX = floor(distance * Math.sin(angle) + rootX + 0.5D);
                        int nodeZ = floor(distance * Math.cos(angle) + rootZ + 0.5D);
                        double horizontalDistance = Math.sqrt(Math.pow(Math.abs(rootX - nodeX), 2.0D)
                                + Math.pow(Math.abs(rootZ - nodeZ), 2.0D));
                        double branchDrop = horizontalDistance * 0.381D;
                        int branchBaseY = leafY - branchDrop > trunkTopY ? trunkTopY : (int) (leafY - branchDrop);
                        nodes.add(new BigLeafNode(nodeX, leafY, nodeZ, branchBaseY));
                    }
                }
                leafY--;
                relativeY--;
            }
            return nodes;
        }

        private int bigTrunkHeight() {
            int trunkHeight = (int) (height * 0.618D);
            return trunkHeight >= height ? height - 1 : trunkHeight;
        }

        private Random bigTreeRandomAfterHeight() {
            Random random = new Random(bigTreeSeed());
            random.nextInt(12);
            return random;
        }

        private long bigTreeSeed() {
            return ((long) dataA << 32) ^ (dataB & 0xffffffffL);
        }

        private float bigLayerSize(int relativeY) {
            if (relativeY < height * 0.3D) {
                return -1.618F;
            }
            float halfHeight = height / 2.0F;
            float offset = halfHeight - relativeY;
            float radius;
            if (offset == 0.0F) {
                radius = halfHeight;
            } else if (Math.abs(offset) >= halfHeight) {
                radius = 0.0F;
            } else {
                radius = (float) Math.sqrt(Math.pow(Math.abs(halfHeight), 2.0D)
                        - Math.pow(Math.abs(offset), 2.0D));
            }
            return radius * 0.5F;
        }

        private float bigLeafSize(int offset) {
            if (offset < 0 || offset >= 4) {
                return -1.0F;
            }
            return offset == 0 || offset == 3 ? 2.0F : 3.0F;
        }

        private boolean bigBranchCanGrow(int relativeY) {
            return relativeY >= height * 0.2D;
        }

        private void placeBigLeafNode(Chunk chunk, int chunkX, int chunkZ, BigLeafNode node) {
            for (int offset = 0; offset < 4; offset++) {
                float radius = bigLeafSize(offset);
                if (radius >= 0.0F) {
                    placeBigLeafLayer(chunk, chunkX, chunkZ, node.x(), node.y() + offset, node.z(), radius);
                }
            }
        }

        private void placeBigLeafNode(World world, BigLeafNode node) {
            for (int offset = 0; offset < 4; offset++) {
                float radius = bigLeafSize(offset);
                if (radius >= 0.0F) {
                    placeBigLeafLayer(world, node.x(), node.y() + offset, node.z(), radius);
                }
            }
        }

        private void placeBigLeafLayer(Chunk chunk, int chunkX, int chunkZ, int centerX, int y, int centerZ,
                float radius) {
            int blockRadius = (int) (radius + 0.618D);
            for (int dx = -blockRadius; dx <= blockRadius; dx++) {
                for (int dz = -blockRadius; dz <= blockRadius; dz++) {
                    double distance = Math.sqrt(Math.pow(Math.abs(dx) + 0.5D, 2.0D)
                            + Math.pow(Math.abs(dz) + 0.5D, 2.0D));
                    if (distance <= radius) {
                        setIfInChunk(chunk, chunkX, chunkZ, centerX + dx, y, centerZ + dz,
                                BlockType.LEAVES, 0, false);
                    }
                }
            }
        }

        private void placeBigLeafLayer(World world, int centerX, int y, int centerZ, float radius) {
            int blockRadius = (int) (radius + 0.618D);
            for (int dx = -blockRadius; dx <= blockRadius; dx++) {
                for (int dz = -blockRadius; dz <= blockRadius; dz++) {
                    double distance = Math.sqrt(Math.pow(Math.abs(dx) + 0.5D, 2.0D)
                            + Math.pow(Math.abs(dz) + 0.5D, 2.0D));
                    if (distance <= radius) {
                        setIfReplaceable(world, centerX + dx, y, centerZ + dz, BlockType.LEAVES, 0, false);
                    }
                }
            }
        }

        private void placeBigBlockLine(Chunk chunk, int chunkX, int chunkZ, int[] start, int[] end) {
            for (int[] point : bigBlockLine(start, end)) {
                setIfInChunk(chunk, chunkX, chunkZ, point[0], point[1], point[2], BlockType.OAK_LOG, 0, true);
            }
        }

        private void placeBigBlockLine(World world, int[] start, int[] end) {
            for (int[] point : bigBlockLine(start, end)) {
                setIfReplaceable(world, point[0], point[1], point[2], BlockType.OAK_LOG, 0, true);
            }
        }

        private List<int[]> bigBlockLine(int[] start, int[] end) {
            int[] delta = { end[0] - start[0], end[1] - start[1], end[2] - start[2] };
            int majorAxis = 0;
            for (int axis = 1; axis < 3; axis++) {
                if (Math.abs(delta[axis]) > Math.abs(delta[majorAxis])) {
                    majorAxis = axis;
                }
            }
            if (delta[majorAxis] == 0) {
                return List.of();
            }

            int axisA = BIG_TREE_AXIS_PAIRS[majorAxis];
            int axisB = BIG_TREE_AXIS_PAIRS[majorAxis + 3];
            int step = delta[majorAxis] > 0 ? 1 : -1;
            double slopeA = (double) delta[axisA] / delta[majorAxis];
            double slopeB = (double) delta[axisB] / delta[majorAxis];
            int stop = delta[majorAxis] + step;
            List<int[]> points = new ArrayList<>();
            for (int distance = 0; distance != stop; distance += step) {
                int[] point = { 0, 0, 0 };
                point[majorAxis] = floor(start[majorAxis] + distance + 0.5D);
                point[axisA] = floor(start[axisA] + distance * slopeA + 0.5D);
                point[axisB] = floor(start[axisB] + distance * slopeB + 0.5D);
                points.add(point);
            }
            return points;
        }

        private int checkBigBlockLine(int[] start, int[] end, BlockQuery query) {
            List<int[]> points = bigBlockLine(start, end);
            for (int i = 0; i < points.size(); i++) {
                int[] point = points.get(i);
                BlockType block = query.getBlock(point[0], point[1], point[2]);
                if (block != BlockType.AIR && block != BlockType.LEAVES) {
                    return i;
                }
            }
            return -1;
        }

        private boolean canPlaceSwamp(BlockQuery query) {
            int baseY = swampBaseY(query);
            if (baseY < 1 || baseY + height + 1 > Chunk.HEIGHT) {
                return false;
            }
            for (int y = baseY; y <= baseY + 1 + height; y++) {
                int radius = y == baseY ? 0 : 1;
                if (y >= (baseY + 1 + height) - 2) {
                    radius = 3;
                }
                for (int x = rootX - radius; x <= rootX + radius; x++) {
                    for (int z = rootZ - radius; z <= rootZ + radius; z++) {
                        BlockType block = query.getBlock(x, y, z);
                        if (block == BlockType.AIR || block == BlockType.LEAVES) {
                            continue;
                        }
                        if (block.isWater()) {
                            if (y > baseY) {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    }
                }
            }
            BlockType support = query.getBlock(rootX, baseY - 1, rootZ);
            return (support == BlockType.GRASS || support == BlockType.DIRT)
                    && baseY < Chunk.HEIGHT - height - 1;
        }

        private int swampBaseY(BlockQuery query) {
            int baseY = rootY;
            while (baseY > 1 && query.getBlock(rootX, baseY - 1, rootZ).isWater()) {
                baseY--;
            }
            return baseY;
        }

        private void placeTaiga1Into(Chunk chunk, int chunkX, int chunkZ) {
            int leafStart = dataA;
            int maxRadius = Math.max(1, dataB);
            setGroundIfInChunk(chunk, chunkX, chunkZ, rootX, rootY - 1, rootZ);

            int radius = 0;
            for (int y = rootY + height; y >= rootY + leafStart; y--) {
                for (int x = rootX - radius; x <= rootX + radius; x++) {
                    int dx = x - rootX;
                    for (int z = rootZ - radius; z <= rootZ + radius; z++) {
                        int dz = z - rootZ;
                        if ((Math.abs(dx) != radius || Math.abs(dz) != radius || radius <= 0)
                                && isReplaceableForLeaves(blockInChunk(chunk, chunkX, chunkZ, x, y, z))) {
                            setIfInChunk(chunk, chunkX, chunkZ, x, y, z, BlockType.LEAVES, 1, false);
                        }
                    }
                }

                if (radius >= 1 && y == rootY + leafStart + 1) {
                    radius--;
                } else if (radius < maxRadius) {
                    radius++;
                }
            }

            for (int i = 0; i < height - 1; i++) {
                setIfInChunk(chunk, chunkX, chunkZ, rootX, rootY + i, rootZ, BlockType.OAK_LOG, 1, true);
            }
        }

        private void placeTaiga1Into(World world) {
            int leafStart = dataA;
            int maxRadius = Math.max(1, dataB);
            world.setBlock(rootX, rootY - 1, rootZ, BlockType.DIRT);

            int radius = 0;
            for (int y = rootY + height; y >= rootY + leafStart; y--) {
                for (int x = rootX - radius; x <= rootX + radius; x++) {
                    int dx = x - rootX;
                    for (int z = rootZ - radius; z <= rootZ + radius; z++) {
                        int dz = z - rootZ;
                        if ((Math.abs(dx) != radius || Math.abs(dz) != radius || radius <= 0)
                                && isReplaceableForLeaves(world.getBlockIfLoaded(x, y, z, BlockType.BEDROCK))) {
                            setIfReplaceable(world, x, y, z, BlockType.LEAVES, 1, false);
                        }
                    }
                }

                if (radius >= 1 && y == rootY + leafStart + 1) {
                    radius--;
                } else if (radius < maxRadius) {
                    radius++;
                }
            }

            for (int i = 0; i < height - 1; i++) {
                setIfReplaceable(world, rootX, rootY + i, rootZ, BlockType.OAK_LOG, 1, true);
            }
        }

        private void placeTaiga2Into(Chunk chunk, int chunkX, int chunkZ) {
            int topOffset = dataA;
            int trunkHeight = height - topOffset;
            int maxRadius = Math.max(1, dataB);
            int radius = dataC;
            int trunkShorten = dataD;
            int nextRadiusLimit = 1;
            int resetRadius = 0;
            setGroundIfInChunk(chunk, chunkX, chunkZ, rootX, rootY - 1, rootZ);

            for (int layer = 0; layer <= trunkHeight; layer++) {
                int y = rootY + height - layer;
                for (int x = rootX - radius; x <= rootX + radius; x++) {
                    int dx = x - rootX;
                    for (int z = rootZ - radius; z <= rootZ + radius; z++) {
                        int dz = z - rootZ;
                        if ((Math.abs(dx) == radius && Math.abs(dz) == radius && radius > 0)
                                || !isReplaceableForLeaves(blockInChunk(chunk, chunkX, chunkZ, x, y, z))) {
                            continue;
                        }
                        setIfInChunk(chunk, chunkX, chunkZ, x, y, z, BlockType.LEAVES, 1, false);
                    }
                }

                if (radius >= nextRadiusLimit) {
                    radius = resetRadius;
                    resetRadius = 1;
                    nextRadiusLimit++;
                    if (nextRadiusLimit > maxRadius) {
                        nextRadiusLimit = maxRadius;
                    }
                } else {
                    radius++;
                }
            }

            for (int i = 0; i < height - trunkShorten; i++) {
                setIfInChunk(chunk, chunkX, chunkZ, rootX, rootY + i, rootZ, BlockType.OAK_LOG, 1, true);
            }
        }

        private void placeTaiga2Into(World world) {
            int topOffset = dataA;
            int trunkHeight = height - topOffset;
            int maxRadius = Math.max(1, dataB);
            int radius = dataC;
            int trunkShorten = dataD;
            int nextRadiusLimit = 1;
            int resetRadius = 0;
            world.setBlock(rootX, rootY - 1, rootZ, BlockType.DIRT);

            for (int layer = 0; layer <= trunkHeight; layer++) {
                int y = rootY + height - layer;
                for (int x = rootX - radius; x <= rootX + radius; x++) {
                    int dx = x - rootX;
                    for (int z = rootZ - radius; z <= rootZ + radius; z++) {
                        int dz = z - rootZ;
                        if ((Math.abs(dx) == radius && Math.abs(dz) == radius && radius > 0)
                                || !isReplaceableForLeaves(world.getBlockIfLoaded(x, y, z, BlockType.BEDROCK))) {
                            continue;
                        }
                        setIfReplaceable(world, x, y, z, BlockType.LEAVES, 1, false);
                    }
                }

                if (radius >= nextRadiusLimit) {
                    radius = resetRadius;
                    resetRadius = 1;
                    nextRadiusLimit++;
                    if (nextRadiusLimit > maxRadius) {
                        nextRadiusLimit = maxRadius;
                    }
                } else {
                    radius++;
                }
            }

            for (int i = 0; i < height - trunkShorten; i++) {
                setIfReplaceable(world, rootX, rootY + i, rootZ, BlockType.OAK_LOG, 1, true);
            }
        }

        private void placeSwampInto(Chunk chunk, int chunkX, int chunkZ) {
            int baseY = rootY;
            setIfInChunk(chunk, chunkX, chunkZ, rootX, baseY - 1, rootZ, BlockType.DIRT, 0, true);
            int cornerIndex = 0;
            for (int y = (baseY - 3) + height; y <= baseY + height; y++) {
                int layerOffset = y - (baseY + height);
                int radius = 2 - layerOffset / 2;
                for (int x = rootX - radius; x <= rootX + radius; x++) {
                    int dx = x - rootX;
                    for (int z = rootZ - radius; z <= rootZ + radius; z++) {
                        int dz = z - rootZ;
                        boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
                        if (swampLeafPositionPresent(corner, layerOffset, cornerIndex)) {
                            setIfInChunk(chunk, chunkX, chunkZ, x, y, z, BlockType.LEAVES, treeMetadata & 3, false);
                        }
                        if (corner) {
                            cornerIndex++;
                        }
                    }
                }
            }
            for (int i = 0; i < height; i++) {
                setIfInChunk(chunk, chunkX, chunkZ, rootX, baseY + i, rootZ, BlockType.OAK_LOG,
                        treeMetadata & 3, true);
            }
            placeSwampVines(chunk, chunkX, chunkZ, baseY);
        }

        private void placeSwampInto(World world) {
            int baseY = rootY;
            setIfReplaceable(world, rootX, baseY - 1, rootZ, BlockType.DIRT, 0, true);
            int cornerIndex = 0;
            for (int y = (baseY - 3) + height; y <= baseY + height; y++) {
                int layerOffset = y - (baseY + height);
                int radius = 2 - layerOffset / 2;
                for (int x = rootX - radius; x <= rootX + radius; x++) {
                    int dx = x - rootX;
                    for (int z = rootZ - radius; z <= rootZ + radius; z++) {
                        int dz = z - rootZ;
                        boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
                        if (swampLeafPositionPresent(corner, layerOffset, cornerIndex)) {
                            setIfReplaceable(world, x, y, z, BlockType.LEAVES, treeMetadata & 3, false);
                        }
                        if (corner) {
                            cornerIndex++;
                        }
                    }
                }
            }
            for (int i = 0; i < height; i++) {
                setIfReplaceable(world, rootX, baseY + i, rootZ, BlockType.OAK_LOG, treeMetadata & 3, true);
            }
            placeSwampVines(world, baseY);
        }

        private void placeSwampVines(Chunk chunk, int chunkX, int chunkZ, int baseY) {
            Random random = swampVineRandom();
            int cornerIndex = 0;
            for (int y = (baseY - 3) + height; y <= baseY + height; y++) {
                int layerOffset = y - (baseY + height);
                int radius = 2 - layerOffset / 2;
                for (int x = rootX - radius; x <= rootX + radius; x++) {
                    int dx = x - rootX;
                    for (int z = rootZ - radius; z <= rootZ + radius; z++) {
                        int dz = z - rootZ;
                        boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
                        boolean sourceLeaf = swampLeafPositionPresent(corner, layerOffset, cornerIndex);
                        if (corner) {
                            cornerIndex++;
                        }
                        if (!sourceLeaf) {
                            continue;
                        }
                        boolean west = random.nextInt(4) == 0;
                        boolean east = random.nextInt(4) == 0;
                        boolean north = random.nextInt(4) == 0;
                        boolean south = random.nextInt(4) == 0;
                        if (blockInChunk(chunk, chunkX, chunkZ, x, y, z) != BlockType.LEAVES) {
                            continue;
                        }
                        if (west) {
                            generateVines(chunk, chunkX, chunkZ, x - 1, y, z,
                                    BlockShape.vineMetadataFromFace(Block.FACE_WEST));
                        }
                        if (east) {
                            generateVines(chunk, chunkX, chunkZ, x + 1, y, z,
                                    BlockShape.vineMetadataFromFace(Block.FACE_EAST));
                        }
                        if (north) {
                            generateVines(chunk, chunkX, chunkZ, x, y, z - 1,
                                    BlockShape.vineMetadataFromFace(Block.FACE_NORTH));
                        }
                        if (south) {
                            generateVines(chunk, chunkX, chunkZ, x, y, z + 1,
                                    BlockShape.vineMetadataFromFace(Block.FACE_SOUTH));
                        }
                    }
                }
            }
        }

        private void placeSwampVines(World world, int baseY) {
            Random random = swampVineRandom();
            int cornerIndex = 0;
            for (int y = (baseY - 3) + height; y <= baseY + height; y++) {
                int layerOffset = y - (baseY + height);
                int radius = 2 - layerOffset / 2;
                for (int x = rootX - radius; x <= rootX + radius; x++) {
                    int dx = x - rootX;
                    for (int z = rootZ - radius; z <= rootZ + radius; z++) {
                        int dz = z - rootZ;
                        boolean corner = Math.abs(dx) == radius && Math.abs(dz) == radius;
                        boolean sourceLeaf = swampLeafPositionPresent(corner, layerOffset, cornerIndex);
                        if (corner) {
                            cornerIndex++;
                        }
                        if (!sourceLeaf) {
                            continue;
                        }
                        boolean west = random.nextInt(4) == 0;
                        boolean east = random.nextInt(4) == 0;
                        boolean north = random.nextInt(4) == 0;
                        boolean south = random.nextInt(4) == 0;
                        if (world.getBlockIfLoaded(x, y, z, BlockType.BEDROCK) != BlockType.LEAVES) {
                            continue;
                        }
                        if (west) {
                            generateVines(world, x - 1, y, z, BlockShape.vineMetadataFromFace(Block.FACE_WEST));
                        }
                        if (east) {
                            generateVines(world, x + 1, y, z, BlockShape.vineMetadataFromFace(Block.FACE_EAST));
                        }
                        if (north) {
                            generateVines(world, x, y, z - 1, BlockShape.vineMetadataFromFace(Block.FACE_NORTH));
                        }
                        if (south) {
                            generateVines(world, x, y, z + 1, BlockShape.vineMetadataFromFace(Block.FACE_SOUTH));
                        }
                    }
                }
            }
        }

        private boolean swampLeafPositionPresent(boolean corner, int layerOffset, int cornerIndex) {
            return !corner || (swampLeafCornerPresent(cornerIndex) && layerOffset != 0);
        }

        private boolean swampLeafCornerPresent(int cornerIndex) {
            return (dataA & (1 << cornerIndex)) != 0;
        }

        private Random swampVineRandom() {
            long seed = ((long) dataB << 32) ^ (dataC & 0xffffffffL);
            return new Random(seed);
        }
    }

    private record BigLeafNode(int x, int y, int z, int branchBaseY) {
    }

    static boolean isTreeSupport(BlockType block) {
        return block == BlockType.GRASS || block == BlockType.DIRT || block == BlockType.MYCELIUM;
    }

    static boolean isReplaceableForTrunk(BlockType block) {
        return block == BlockType.AIR || block == BlockType.LEAVES || block.isPlant()
                || block == BlockType.TALL_GRASS || block == BlockType.FIRE;
    }

    static boolean isReplaceableForLeaves(BlockType block) {
        return block == BlockType.AIR || block == BlockType.LEAVES || block.isPlant()
                || block == BlockType.TALL_GRASS || block == BlockType.FIRE;
    }

    private static void setIfInChunk(Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ,
            BlockType type, int metadata, boolean trunk) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return;
        }
        int localX = worldX - chunkX * Chunk.WIDTH;
        int localZ = worldZ - chunkZ * Chunk.DEPTH;
        if (!Chunk.isInBounds(localX, y, localZ)) {
            return;
        }
        BlockType current = chunk.getBlock(localX, y, localZ);
        if (trunk) {
            if (isReplaceableForTrunk(current)) {
                chunk.setBlock(localX, y, localZ, type, metadata);
            }
        } else if (isReplaceableForLeaves(current)) {
            chunk.setBlock(localX, y, localZ, type, metadata);
        }
    }

    private static void setGroundIfInChunk(Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return;
        }
        int localX = worldX - chunkX * Chunk.WIDTH;
        int localZ = worldZ - chunkZ * Chunk.DEPTH;
        if (Chunk.isInBounds(localX, y, localZ)) {
            chunk.setBlock(localX, y, localZ, BlockType.DIRT);
        }
    }

    private static void generateVines(Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ,
            int metadata) {
        for (int remaining = 5; remaining > 0 && y >= 0; remaining--, y--) {
            int localX = worldX - chunkX * Chunk.WIDTH;
            int localZ = worldZ - chunkZ * Chunk.DEPTH;
            if (!Chunk.isInBounds(localX, y, localZ)) {
                continue;
            }
            if (chunk.getBlock(localX, y, localZ) != BlockType.AIR) {
                return;
            }
            chunk.setBlock(localX, y, localZ, BlockType.VINES, metadata);
        }
    }

    private static void generateVines(World world, int x, int y, int z, int metadata) {
        for (int remaining = 5; remaining > 0 && y >= 0; remaining--, y--) {
            if (world.getBlockIfLoaded(x, y, z, BlockType.BEDROCK) != BlockType.AIR) {
                return;
            }
            world.setBlock(x, y, z, BlockType.VINES, metadata);
        }
    }

    private static BlockType blockInChunk(Chunk chunk, int chunkX, int chunkZ, int worldX, int y, int worldZ) {
        int localX = worldX - chunkX * Chunk.WIDTH;
        int localZ = worldZ - chunkZ * Chunk.DEPTH;
        if (!Chunk.isInBounds(localX, y, localZ)) {
            return BlockType.AIR;
        }
        return chunk.getBlock(localX, y, localZ);
    }

    private static void setIfReplaceable(World world, int x, int y, int z, BlockType type, int metadata,
            boolean trunk) {
        if (y < 0 || y >= Chunk.HEIGHT) {
            return;
        }
        BlockType current = world.getBlockIfLoaded(x, y, z, BlockType.BEDROCK);
        if (trunk) {
            if (isReplaceableForTrunk(current)) {
                world.setBlock(x, y, z, type, metadata);
            }
        } else if (isReplaceableForLeaves(current)) {
            world.setBlock(x, y, z, type, metadata);
        }
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }
}
