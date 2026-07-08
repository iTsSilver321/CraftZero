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
    private static final int MAX_ITERATIONS = 512;
    private static final int MAX_PATH_LENGTH = 64;
    private static final int RECALC_INTERVAL = 10;
    private static final float PARTIAL_PATH_MIN_PROGRESS = 1.0f;

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
        int newGoalX = (int) Math.floor(x);
        int newGoalY = (int) Math.floor(y);
        int newGoalZ = (int) Math.floor(z);
        int oldGoalX = (int) Math.floor(targetX);
        int oldGoalY = (int) Math.floor(targetY);
        int oldGoalZ = (int) Math.floor(targetZ);
        boolean sameBlockTarget = hasTarget
                && newGoalX == oldGoalX
                && newGoalY == oldGoalY
                && newGoalZ == oldGoalZ;

        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.hasTarget = true;

        if (!sameBlockTarget) {
            recalcCooldown = Math.min(recalcCooldown, 5);
        }
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

    public boolean hasTarget() {
        return hasTarget;
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
        if (recalcCooldown <= 0
                || currentPath == null
                || !currentPath.isValid()
                || currentPath.isTargetStale(targetX, targetY, targetZ)) {
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
        if (!evaluator.canUseForPath(startNode)) {
            return Path.empty();
        }

        openSet.add(startNode);
        nodeCache.put(nodeKey(startX, startY, startZ), startNode);

        int iterations = 0;
        PathNode bestNode = startNode;
        float bestNodeScore = partialPathScore(startNode, goalX, goalY, goalZ);

        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;

            // Get node with lowest fCost
            PathNode current = openSet.poll();

            if (current == null)
                break;
            current.inOpenSet = false;

            // Check if we've reached the goal
            if (current.x == goalX && current.z == goalZ && Math.abs(current.y - goalY) <= 1) {
                return reconstructPath(current, goalNode);
            }

            float currentScore = partialPathScore(current, goalX, goalY, goalZ);
            if (currentScore + PARTIAL_PATH_MIN_PROGRESS < bestNodeScore) {
                bestNode = current;
                bestNodeScore = currentScore;
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
                            key = cacheAdjustedNode(nodeCache, key, neighbor);
                        } else if (neighbor.x != nx || neighbor.y != ny || neighbor.z != nz) {
                            key = nodeKey(neighbor.x, neighbor.y, neighbor.z);
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
                            if (neighbor.inOpenSet) {
                                openSet.remove(neighbor);
                            }
                            neighbor.parent = current;
                            neighbor.gCost = tentativeG;
                            neighbor.calculateHeuristic(goalX, goalY, goalZ);
                            neighbor.calculateFCost();

                            neighbor.inOpenSet = true;
                            openSet.add(neighbor);
                        }
                    }
                }
            }
        }

        if (bestNode != null && bestNode != startNode && bestNode.parent != null) {
            return reconstructPath(bestNode, goalNode);
        }

        return Path.empty();
    }

    private long cacheAdjustedNode(Map<Long, PathNode> nodeCache, long originalKey, PathNode node) {
        long adjustedKey = nodeKey(node.x, node.y, node.z);
        if (adjustedKey != originalKey) {
            PathNode existing = nodeCache.get(adjustedKey);
            if (existing == null) {
                nodeCache.put(adjustedKey, node);
            }
        }
        return adjustedKey;
    }

    private static float partialPathScore(PathNode node, int goalX, int goalY, int goalZ) {
        int dx = node.x - goalX;
        int dy = node.y - goalY;
        int dz = node.z - goalZ;
        return dx * dx + dz * dz + Math.abs(dy) * 1.5f;
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
