package com.craftzero.graphics.model;

import com.craftzero.graphics.Mesh;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * A single box part of a mob model (head, body, limb, etc).
 * Supports hierarchical transforms with pivot points for proper joint rotation.
 * 
 * PIVOT POINT LOGIC:
 * When rotating, we: 1) Translate to pivot, 2) Rotate, 3) Translate back
 * This makes limbs rotate around their joints, not their corners.
 */
public class ModelPart {

    // Dimensions
    private final float width, height, depth;

    // Position offset from parent
    private float offsetX, offsetY, offsetZ;

    // Pivot point for rotation (relative to this part's origin)
    private float pivotX, pivotY, pivotZ;

    // Current rotation (radians)
    private float rotationX, rotationY, rotationZ;

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
     * Set offset position from parent.
     */
    public ModelPart setOffset(float x, float y, float z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
        return this;
    }

    /**
     * Set pivot point for rotation.
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
            // Empty root part
            for (ModelPart child : children) {
                child.buildMesh();
            }
            return;
        }

        float hw = width / 2;
        float hh = height / 2;
        float hd = depth / 2;

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
                -hw, -hh, -hd, hw, -hh, -hd, hw, hh, -hd, -hw, hh, -hd,
                uBase + du, vBase + dv, wu, hv, 0, 0, -1);

        // Back face (+Z direction)
        idx = addFace(positions, texCoords, normals, indices, idx,
                hw, -hh, hd, -hw, -hh, hd, -hw, hh, hd, hw, hh, hd,
                uBase + du + wu + du, vBase + dv, wu, hv, 0, 0, 1);

        // Right face (+X direction, looking at front)
        idx = addFace(positions, texCoords, normals, indices, idx,
                hw, -hh, -hd, hw, -hh, hd, hw, hh, hd, hw, hh, -hd,
                uBase + du + wu, vBase + dv, du, hv, 1, 0, 0);

        // Left face (-X direction, looking at front)
        idx = addFace(positions, texCoords, normals, indices, idx,
                -hw, -hh, hd, -hw, -hh, -hd, -hw, hh, -hd, -hw, hh, hd,
                uBase, vBase + dv, du, hv, -1, 0, 0);

        // Top face (+Y direction)
        idx = addFace(positions, texCoords, normals, indices, idx,
                -hw, hh, -hd, hw, hh, -hd, hw, hh, hd, -hw, hh, hd,
                uBase + du, vBase, wu, dv, 0, 1, 0);

        // Bottom face (-Y direction)
        idx = addFace(positions, texCoords, normals, indices, idx,
                -hw, -hh, hd, hw, -hh, hd, hw, -hh, -hd, -hw, -hh, -hd,
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
     * Calculate transform matrix with proper pivot point rotation.
     */
    public void calculateTransform(Matrix4f parentTransform) {
        localTransform.identity();

        // 1. Translate to position
        localTransform.translate(offsetX, offsetY, offsetZ);

        // 2. Translate to pivot point
        localTransform.translate(pivotX, pivotY, pivotZ);

        // 3. Apply rotation
        localTransform.rotateZ(rotationZ);
        localTransform.rotateY(rotationY);
        localTransform.rotateX(rotationX);

        // 4. Translate back from pivot
        localTransform.translate(-pivotX, -pivotY, -pivotZ);

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
}
