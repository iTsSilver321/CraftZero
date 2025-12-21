package com.craftzero.entity.ai.pathfinding;

import com.craftzero.entity.LivingEntity;
import com.craftzero.world.World;

import java.util.*;

/**
 * A* pathfinding navigator for mobs.
 * Calculates paths through the world and follows them.
 */
public class Navigator {

    private final LivingEntity entity;
    private final World world;
    private final PathNodeEvaluator evaluator;

    private Path currentPath;
    private float targetX, targetY, targetZ;
    private boolean hasTarget;

    // Pathfinding limits
    private static final int MAX_ITERATIONS = 500; // Max A* iterations
    private static final int MAX_PATH_LENGTH = 32; // Max nodes in path
    private static final int RECALC_INTERVAL = 20; // Ticks between recalculations

    private int recalcCooldown = 0;

    public Navigator(LivingEntity entity, World world) {
        this.entity = entity;
        this.world = world;
        this.evaluator = new PathNodeEvaluator(world, entity.getWidth(), entity.getHeight());
        this.currentPath = null;
        this.hasTarget = false;
    }

    /**
     * Set a new target to navigate to.
     */
    public void moveTo(float x, float y, float z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.hasTarget = true;

        // Force immediate recalculation
        recalcCooldown = 0;
    }

    /**
     * Clear the current path and target.
     */
    public void stop() {
        this.currentPath = null;
        this.hasTarget = false;
    }

    /**
     * Check if navigator has an active path.
     */
    public boolean isNavigating() {
        return hasTarget && currentPath != null && !currentPath.isDone();
    }

    /**
     * Check if we've reached the destination.
     */
    public boolean hasReachedTarget() {
        if (!hasTarget)
            return false;
        float dx = entity.getX() - targetX;
        float dy = entity.getY() - targetY;
        float dz = entity.getZ() - targetZ;
        return dx * dx + dz * dz < 1.5f && Math.abs(dy) < 2.0f;
    }

    /**
     * Get the current path (for debugging/rendering).
     */
    public Path getCurrentPath() {
        return currentPath;
    }

    /**
     * Update navigation each tick.
     * Returns the next position to move toward, or null if no path.
     */
    public PathNode tick() {
        if (!hasTarget)
            return null;

        // Recalculate path periodically or if needed
        recalcCooldown--;
        if (recalcCooldown <= 0 || currentPath == null || !currentPath.isValid()) {
            recalculatePath();
            recalcCooldown = RECALC_INTERVAL;
        }

        if (currentPath == null || !currentPath.isValid()) {
            return null;
        }

        // Check if we should advance to next node
        if (currentPath.shouldAdvance(entity.getX(), entity.getY(), entity.getZ())) {
            currentPath.advance();
        }

        // Return current target node
        return currentPath.getCurrentNode();
    }

    /**
     * Recalculate path to target using A*.
     */
    private void recalculatePath() {
        int startX = (int) Math.floor(entity.getX());
        int startY = (int) Math.floor(entity.getY());
        int startZ = (int) Math.floor(entity.getZ());

        int goalX = (int) Math.floor(targetX);
        int goalY = (int) Math.floor(targetY);
        int goalZ = (int) Math.floor(targetZ);

        currentPath = findPath(startX, startY, startZ, goalX, goalY, goalZ);
    }

    /**
     * A* pathfinding algorithm.
     */
    private Path findPath(int startX, int startY, int startZ, int goalX, int goalY, int goalZ) {
        // Early exit if already at goal
        if (startX == goalX && startY == goalY && startZ == goalZ) {
            return Path.empty();
        }

        // Create start and goal nodes
        PathNode startNode = new PathNode(startX, startY, startZ);
        PathNode goalNode = new PathNode(goalX, goalY, goalZ);

        // Open set (nodes to explore) - priority queue by fCost
        PriorityQueue<PathNode> openSet = new PriorityQueue<>();

        // Closed set (visited nodes)
        Set<PathNode> closedSet = new HashSet<>();

        // Node cache (for looking up existing nodes)
        Map<Long, PathNode> nodeCache = new HashMap<>();

        // Initialize start node
        startNode.gCost = 0;
        startNode.calculateHeuristic(goalX, goalY, goalZ);
        startNode.calculateFCost();
        evaluator.evaluateNode(startNode);

        openSet.add(startNode);
        nodeCache.put(nodeKey(startX, startY, startZ), startNode);

        int iterations = 0;

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;

            // Get node with lowest fCost
            PathNode current = openSet.poll();

            if (current == null)
                break;

            // Check if we've reached the goal
            if (current.x == goalX && current.z == goalZ && Math.abs(current.y - goalY) <= 1) {
                return reconstructPath(current, goalNode);
            }

            closedSet.add(current);

            // Explore neighbors (8 directions + up/down)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0)
                        continue;

                    // Check each possible Y level (same, up 1, down 1-3)
                    for (int dy = -3; dy <= 1; dy++) {
                        int nx = current.x + dx;
                        int ny = current.y + dy;
                        int nz = current.z + dz;

                        // Skip if too far from start (path length limit)
                        if (Math.abs(nx - startX) + Math.abs(nz - startZ) > MAX_PATH_LENGTH) {
                            continue;
                        }

                        long key = nodeKey(nx, ny, nz);
                        PathNode neighbor = nodeCache.get(key);

                        if (neighbor == null) {
                            neighbor = new PathNode(nx, ny, nz);
                            evaluator.evaluateNode(neighbor);
                            nodeCache.put(key, neighbor);
                        }

                        // Skip if already visited
                        if (closedSet.contains(neighbor))
                            continue;

                        // Skip if can't move there
                        if (!evaluator.canMoveBetween(current, neighbor))
                            continue;

                        // Calculate tentative gCost
                        float moveCost = evaluator.getMovementCost(current, neighbor);
                        float tentativeG = current.gCost + moveCost;

                        // Check if this is a better path
                        if (!neighbor.inOpenSet || tentativeG < neighbor.gCost) {
                            neighbor.parent = current;
                            neighbor.gCost = tentativeG;
                            neighbor.calculateHeuristic(goalX, goalY, goalZ);
                            neighbor.calculateFCost();

                            if (!neighbor.inOpenSet) {
                                neighbor.inOpenSet = true;
                                openSet.add(neighbor);
                            }
                        }
                    }
                }
            }
        }

        // No path found
        return Path.empty();
    }

    /**
     * Reconstruct path from goal back to start.
     */
    private Path reconstructPath(PathNode goalNode, PathNode target) {
        List<PathNode> nodes = new ArrayList<>();
        PathNode current = goalNode;

        while (current != null) {
            nodes.add(0, current); // Add to front
            current = current.parent;
        }

        // Skip the first node (we're already there)
        if (!nodes.isEmpty()) {
            nodes.remove(0);
        }

        return new Path(nodes, target);
    }

    /**
     * Create a unique key for a node position.
     */
    private long nodeKey(int x, int y, int z) {
        return ((long) x & 0xFFFFF) | (((long) y & 0xFF) << 20) | (((long) z & 0xFFFFF) << 28);
    }
}
