package com.craftzero.graphics.model;

import com.craftzero.graphics.Mesh;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * A single box part of a mob model (head, body, limb, etc).
 * Supports hierarchical transforms with pivot points for proper joint rotation.
 * 
 * SCENE GRAPH APPROACH (Authentic Minecraft Beta 1.7.3):
 * - Pivot point defines the joint location (e.g., shoulder, hip)
 * - Box coordinates are defined RELATIVE to the pivot using addBox()
 * - Transform order: Translate to pivot -> Rotate (Z, Y, X) -> Scale
 * - NO "translate back" - box vertices are already offset from pivot
 */
public class ModelPart {

    // Dimensions (set via constructor or addBox)
    private float width, height, depth;

    // Box offset relative to pivot (set via addBox)
    private float boxOffsetX, boxOffsetY, boxOffsetZ;

    // Position offset from parent (legacy mode for old models)
    private float offsetX, offsetY, offsetZ;

    // Pivot point for rotation (joint location in parent space)
    private float pivotX, pivotY, pivotZ;

    // Current rotation (radians)
    private float rotationX, rotationY, rotationZ;

    // Scale (default 1.0)
    private float scaleX = 1.0f, scaleY = 1.0f, scaleZ = 1.0f;

    // UV texture coordinates
    private float u, v;
    private float textureWidth, textureHeight;

    // Children parts
    private final List<ModelPart> children;

    // Mesh for this part
    private Mesh mesh;

    // Transform matrix
    private final Matrix4f localTransform;
    private final Matrix4f worldTransform;

    // Flag to indicate if this is using the new addBox system
    private boolean usesBoxOffset = false;

    /**
     * Create an empty ModelPart (root node or use addBox to define geometry).
     */
    public ModelPart() {
        this(0, 0, 0);
    }

    /**
     * Create a ModelPart with dimensions (legacy centered box mode).
     * For new code, prefer using the empty constructor + addBox().
     */
    public ModelPart(float width, float height, float depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.children = new ArrayList<>();
        this.localTransform = new Matrix4f();
        this.worldTransform = new Matrix4f();
        this.textureWidth = 64.0f;
        this.textureHeight = 32.0f;
    }

    /**
     * Add a box with offset relative to the pivot point.
     * This is the authentic Minecraft Beta 1.7.3 approach.
     * 
     * @param x      Starting X offset from pivot
     * @param y      Starting Y offset from pivot
     * @param z      Starting Z offset from pivot
     * @param width  Box width (X size)
     * @param height Box height (Y size)
     * @param depth  Box depth (Z size)
     * @return this for chaining
     */
    public ModelPart addBox(float x, float y, float z, float width, float height, float depth) {
        this.boxOffsetX = x;
        this.boxOffsetY = y;
        this.boxOffsetZ = z;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.usesBoxOffset = true;
        return this;
    }

    /**
     * Set offset position from parent (legacy compatibility).
     * For new code, use addBox() with proper box offsets instead.
     */
    public ModelPart setOffset(float x, float y, float z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
        return this;
    }

    /**
     * Set pivot point for rotation (joint location).
     */
    public ModelPart setPivot(float x, float y, float z) {
        this.pivotX = x;
        this.pivotY = y;
        this.pivotZ = z;
        return this;
    }

    /**
     * Set UV coordinates on texture atlas.
     */
    public ModelPart setTextureOffset(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    /**
     * Set texture size for UV calculation.
     */
    public ModelPart setTextureSize(float width, float height) {
        this.textureWidth = width;
        this.textureHeight = height;
        return this;
    }

    /**
     * Set uniform scale.
     */
    public ModelPart setScale(float scale) {
        this.scaleX = scale;
        this.scaleY = scale;
        this.scaleZ = scale;
        return this;
    }

    /**
     * Set non-uniform scale.
     */
    public ModelPart setScale(float x, float y, float z) {
        this.scaleX = x;
        this.scaleY = y;
        this.scaleZ = z;
        return this;
    }

    /**
     * Set rotation (radians).
     */
    public void setRotation(float x, float y, float z) {
        this.rotationX = x;
        this.rotationY = y;
        this.rotationZ = z;
    }

    /**
     * Add a child part.
     */
    public ModelPart addChild(ModelPart child) {
        children.add(child);
        return this;
    }

    /**
     * Build the mesh for this part.
     */
    public void buildMesh() {
        if (width <= 0 && height <= 0 && depth <= 0) {
            // Empty root part - just build children
            for (ModelPart child : children) {
                child.buildMesh();
            }
            return;
        }

        // Calculate box vertices based on offset mode
        float x1, y1, z1, x2, y2, z2;

        if (usesBoxOffset) {
            // New addBox mode: box starts at offset and extends by dimensions
            x1 = boxOffsetX;
            y1 = boxOffsetY;
            z1 = boxOffsetZ;
            x2 = boxOffsetX + width;
            y2 = boxOffsetY + height;
            z2 = boxOffsetZ + depth;
        } else {
            // Legacy centered mode: box centered at origin
            float hw = width / 2;
            float hh = height / 2;
            float hd = depth / 2;
            x1 = -hw;
            y1 = -hh;
            z1 = -hd;
            x2 = hw;
            y2 = hh;
            z2 = hd;
        }

        // Minecraft box texture UV layout:
        // The texture is laid out as an unwrapped box:
        //
        // [ top ][bottom] <- Row 1 (v to v+depth)
        // [left ][front ][right ][ back ] <- Row 2 (v+depth to v+depth+height)
        //
        // Where dimensions are: width (w), height (h), depth (d)
        // UV start at (u, v) on texture (in pixels)
        //
        // Face UV positions (in pixels from u,v):
        // - Top: (u+d, v, w, d)
        // - Bottom: (u+d+w, v, w, d)
        // - Left: (u, v+d, d, h)
        // - Front: (u+d, v+d, w, h) <- The FACE with eyes
        // - Right: (u+d+w, v+d, d, h)
        // - Back: (u+d+w+d, v+d, w, h)

        float tw = textureWidth;
        float th = textureHeight;

        // Normalized UV sizes
        float du = depth / tw; // depth in U
        float wu = width / tw; // width in U
        float dv = depth / th; // depth in V
        float hv = height / th; // height in V

        float uBase = u / tw;
        float vBase = v / th;

        List<Float> positions = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        int idx = 0;

        // Front face (-Z direction in model space = facing the mob)
        // This is where the FACE/EYES should be
        idx = addFace(positions, texCoords, normals, indices, idx,
                x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1,
                uBase + du, vBase + dv, wu, hv, 0, 0, -1);

        // Back face (+Z direction)
        idx = addFace(positions, texCoords, normals, indices, idx,
                x2, y1, z2, x1, y1, z2, x1, y2, z2, x2, y2, z2,
                uBase + du + wu + du, vBase + dv, wu, hv, 0, 0, 1);

        // Right face (+X direction, looking at front)
        idx = addFace(positions, texCoords, normals, indices, idx,
                x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1,
                uBase + du + wu, vBase + dv, du, hv, 1, 0, 0);

        // Left face (-X direction, looking at front)
        idx = addFace(positions, texCoords, normals, indices, idx,
                x1, y1, z2, x1, y1, z1, x1, y2, z1, x1, y2, z2,
                uBase, vBase + dv, du, hv, -1, 0, 0);

        // Top face (+Y direction)
        idx = addFace(positions, texCoords, normals, indices, idx,
                x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2,
                uBase + du, vBase, wu, dv, 0, 1, 0);

        // Bottom face (-Y direction)
        idx = addFace(positions, texCoords, normals, indices, idx,
                x1, y1, z2, x2, y1, z2, x2, y1, z1, x1, y1, z1,
                uBase + du + wu, vBase, wu, dv, 0, -1, 0);

        // Convert to arrays
        float[] posArray = new float[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            posArray[i] = positions.get(i);
        }

        float[] texArray = new float[texCoords.size()];
        for (int i = 0; i < texCoords.size(); i++) {
            texArray[i] = texCoords.get(i);
        }

        float[] normArray = new float[normals.size()];
        for (int i = 0; i < normals.size(); i++) {
            normArray[i] = normals.get(i);
        }

        int[] idxArray = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            idxArray[i] = indices.get(i);
        }

        mesh = new Mesh(posArray, texArray, normArray, idxArray);

        // Build children
        for (ModelPart child : children) {
            child.buildMesh();
        }
    }

    private int addFace(List<Float> positions, List<Float> texCoords, List<Float> normals,
            List<Integer> indices, int startIdx,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float uStart, float vStart, float uSize, float vSize,
            float nx, float ny, float nz) {
        // Add positions
        addVertex3(positions, x1, y1, z1);
        addVertex3(positions, x2, y2, z2);
        addVertex3(positions, x3, y3, z3);
        addVertex3(positions, x4, y4, z4);

        // Add tex coords
        addVertex2(texCoords, uStart, vStart + vSize);
        addVertex2(texCoords, uStart + uSize, vStart + vSize);
        addVertex2(texCoords, uStart + uSize, vStart);
        addVertex2(texCoords, uStart, vStart);

        // Add normals
        addVertex3(normals, nx, ny, nz);
        addVertex3(normals, nx, ny, nz);
        addVertex3(normals, nx, ny, nz);
        addVertex3(normals, nx, ny, nz);

        // Two triangles
        indices.add(startIdx);
        indices.add(startIdx + 1);
        indices.add(startIdx + 2);

        indices.add(startIdx);
        indices.add(startIdx + 2);
        indices.add(startIdx + 3);

        return startIdx + 4;
    }

    private void addVertex3(List<Float> list, float x, float y, float z) {
        list.add(x);
        list.add(y);
        list.add(z);
    }

    private void addVertex2(List<Float> list, float x, float y) {
        list.add(x);
        list.add(y);
    }

    /**
     * Calculate transform matrix.
     * 
     * For LEGACY mode (uses setOffset + centered box):
     * 1. Translate to offset position
     * 2. Translate to pivot point
     * 3. Rotate (Z, Y, X)
     * 4. Translate back from pivot
     * 
     * For SCENE GRAPH mode (uses addBox):
     * 1. Translate to pivot point (joint location)
     * 2. Rotate (Z, Y, X)
     * 3. Scale
     * No translate back - box vertices are already offset via addBox().
     */
    public void calculateTransform(Matrix4f parentTransform) {
        localTransform.identity();

        if (usesBoxOffset) {
            // SCENE GRAPH MODE (new addBox approach)
            // 1. Translate to pivot point (joint location)
            localTransform.translate(pivotX, pivotY, pivotZ);

            // 2. Apply rotation (Z, Y, X order)
            localTransform.rotateZ(rotationZ);
            localTransform.rotateY(rotationY);
            localTransform.rotateX(rotationX);

            // 3. Apply scale
            localTransform.scale(scaleX, scaleY, scaleZ);

            // Note: NO translate back - box coordinates are relative to pivot
        } else {
            // LEGACY MODE (centered box with offset)
            // 1. Translate to offset position from parent
            localTransform.translate(offsetX, offsetY, offsetZ);

            // 2. Translate to pivot point
            localTransform.translate(pivotX, pivotY, pivotZ);

            // 3. Apply rotation
            localTransform.rotateZ(rotationZ);
            localTransform.rotateY(rotationY);
            localTransform.rotateX(rotationX);

            // 4. Translate back from pivot (legacy behavior)
            localTransform.translate(-pivotX, -pivotY, -pivotZ);
        }

        // Combine with parent
        parentTransform.mul(localTransform, worldTransform);

        // Update children
        for (ModelPart child : children) {
            child.calculateTransform(worldTransform);
        }
    }

    /**
     * Get the world transform matrix.
     */
    public Matrix4f getWorldTransform() {
        return worldTransform;
    }

    /**
     * Get the mesh for rendering.
     */
    public Mesh getMesh() {
        return mesh;
    }

    /**
     * Get children for rendering.
     */
    public List<ModelPart> getChildren() {
        return children;
    }

    /**
     * Cleanup resources.
     */
    public void cleanup() {
        if (mesh != null) {
            mesh.cleanup();
        }
        for (ModelPart child : children) {
            child.cleanup();
        }
    }

    // Getters
    public float getRotationX() {
        return rotationX;
    }

    public float getRotationY() {
        return rotationY;
    }

    public float getRotationZ() {
        return rotationZ;
    }

    public float getPivotX() {
        return pivotX;
    }

    public float getPivotY() {
        return pivotY;
    }

    public float getPivotZ() {
        return pivotZ;
    }
}
