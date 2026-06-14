package com.craftzero.entity.ai.pathfinding;

import com.craftzero.world.World;
import com.craftzero.world.BlockType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the A* pathfinding system.
 * Tests PathNode, Path, and basic navigation logic.
 */
class PathfindingTest {

    private PathNode nodeA;
    private PathNode nodeB;
    private PathNode nodeC;

    @BeforeEach
    void setUp() {
        nodeA = new PathNode(0, 64, 0);
        nodeB = new PathNode(5, 64, 5);
        nodeC = new PathNode(10, 64, 10);
    }

    // ==================== PathNode Tests ====================

    @Test
    @DisplayName("PathNode should calculate correct heuristic distance")
    void testHeuristicCalculation() {
        nodeA.calculateHeuristic(10, 64, 10);

        // Manhattan distance: |10-0| + |64-64|*1.5 + |10-0| = 20
        assertEquals(20.0f, nodeA.hCost, 0.01f);
    }

    @Test
    @DisplayName("PathNode should calculate correct fCost")
    void testFCostCalculation() {
        nodeA.gCost = 5.0f;
        nodeA.hCost = 10.0f;
        nodeA.penalty = 2.0f;
        nodeA.calculateFCost();

        assertEquals(17.0f, nodeA.fCost, 0.01f);
    }

    @Test
    @DisplayName("PathNode getCenterX/Z should return block center")
    void testBlockCenter() {
        PathNode node = new PathNode(5, 64, 10);

        assertEquals(5.5f, node.getCenterX(), 0.01f);
        assertEquals(10.5f, node.getCenterZ(), 0.01f);
    }

    @Test
    @DisplayName("PathNode comparison should prioritize lower fCost")
    void testNodeComparison() {
        nodeA.fCost = 10.0f;
        nodeB.fCost = 5.0f;

        assertTrue(nodeB.compareTo(nodeA) < 0); // B should come before A
        assertTrue(nodeA.compareTo(nodeB) > 0); // A should come after B
    }

    @Test
    @DisplayName("PathNodes at same position should be equal")
    void testNodeEquality() {
        PathNode node1 = new PathNode(5, 64, 10);
        PathNode node2 = new PathNode(5, 64, 10);

        assertEquals(node1, node2);
        assertEquals(node1.hashCode(), node2.hashCode());
    }

    // ==================== Path Tests ====================

    @Test
    @DisplayName("Empty path should be invalid")
    void testEmptyPath() {
        Path path = Path.empty();

        assertFalse(path.isValid());
        assertTrue(path.isDone());
        assertNull(path.getCurrentNode());
    }

    @Test
    @DisplayName("Path should track progress correctly")
    void testPathProgress() {
        java.util.List<PathNode> nodes = java.util.Arrays.asList(nodeA, nodeB, nodeC);
        Path path = new Path(nodes, nodeC);

        assertTrue(path.isValid());
        assertEquals(3, path.getLength());
        assertEquals(0.0f, path.getProgress(), 0.01f);

        path.advance();
        assertEquals(2, path.getRemainingNodes()); // 2 remaining after first advance

        path.advance();
        path.advance();
        assertTrue(path.isDone());
        assertEquals(1.0f, path.getProgress(), 0.01f);
    }

    @Test
    @DisplayName("Path shouldAdvance when close to current node")
    void testShouldAdvance() {
        java.util.List<PathNode> nodes = java.util.Arrays.asList(nodeA);
        Path path = new Path(nodes, nodeA);

        // Very close to node center (0.5, 64, 0.5)
        assertTrue(path.shouldAdvance(0.6f, 64.0f, 0.6f));

        // Too far from node
        assertFalse(path.shouldAdvance(5.0f, 64.0f, 5.0f));
    }

    @Test
    @DisplayName("Path should detect stale target")
    void testStaleTarget() {
        java.util.List<PathNode> nodes = java.util.Arrays.asList(nodeA, nodeC);
        Path path = new Path(nodes, nodeC);

        // Same position - not stale
        assertFalse(path.isTargetStale(10.5f, 64.0f, 10.5f));

        // Moved 5 blocks - stale
        assertTrue(path.isTargetStale(15.0f, 64.0f, 15.0f));
    }

    // ==================== Integration Tests ====================

    @Test
    @DisplayName("PathNode types should be initialized correctly")
    void testNodeTypes() {
        PathNode node = new PathNode(0, 64, 0);

        // Default type
        assertEquals(PathNode.NodeType.WALKABLE, node.type);

        // Can change type
        node.type = PathNode.NodeType.BLOCKED;
        assertEquals(PathNode.NodeType.BLOCKED, node.type);
    }

    @Test
    @DisplayName("Path evaluation should not generate chunks for unloaded positions")
    void pathEvaluationDoesNotGenerateChunks() {
        World world = new World(99L);
        try {
            PathNodeEvaluator evaluator = new PathNodeEvaluator(world, 0.6f, 1.8f);
            PathNode node = new PathNode(1000, 64, 1000);

            evaluator.evaluateNode(node);

            assertTrue(world.getLoadedChunks().isEmpty());
            assertEquals(PathNode.NodeType.BLOCKED, node.type);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Path evaluation should account for wide mob body clearance")
    void pathEvaluationUsesEntityWidthClearance() {
        World world = new World(100L);
        try {
            prepareFlatPatch(world);
            world.setBlock(1, 64, 0, BlockType.STONE);

            PathNodeEvaluator evaluator = new PathNodeEvaluator(world, 1.4f, 0.9f);
            PathNode node = new PathNode(0, 64, 0);
            evaluator.evaluateNode(node);

            assertEquals(PathNode.NodeType.BLOCKED, node.type);
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("Path evaluation should reject diagonal corner cutting through blocked sides")
    void diagonalMovementCannotCutBlockedCorners() {
        World world = new World(101L);
        try {
            prepareFlatPatch(world);
            world.setBlock(1, 64, 0, BlockType.STONE);

            PathNodeEvaluator evaluator = new PathNodeEvaluator(world, 0.6f, 1.8f);
            PathNode from = new PathNode(0, 64, 0);
            PathNode to = new PathNode(1, 64, 1);
            evaluator.evaluateNode(from);
            evaluator.evaluateNode(to);

            assertEquals(PathNode.NodeType.WALKABLE, from.type);
            assertEquals(PathNode.NodeType.WALKABLE, to.type);
            assertFalse(evaluator.canMoveBetween(from, to));
        } finally {
            world.cleanup();
        }
    }

    @Test
    @DisplayName("PathNode parent chain should reconstruct path")
    void testParentChain() {
        nodeB.parent = nodeA;
        nodeC.parent = nodeB;

        // Traverse back
        assertEquals(nodeB, nodeC.parent);
        assertEquals(nodeA, nodeC.parent.parent);
        assertNull(nodeC.parent.parent.parent);
    }

    private static void prepareFlatPatch(World world) {
        world.getChunkNow(0, 0);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 63; y <= 67; y++) {
                    world.setBlock(x, y, z, BlockType.AIR);
                }
                world.setBlock(x, 63, z, BlockType.STONE);
            }
        }
    }
}
