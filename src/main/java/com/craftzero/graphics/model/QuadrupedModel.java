package com.craftzero.graphics.model;

/**
 * Quadruped mob model (Pig, Cow, Sheep).
 * Has head, body, and four legs.
 * 
 * All dimensions are in MODEL UNITS (1 unit = 1/16 of a block).
 * ModelPart boxes are CENTERED around (0,0,0) when built.
 * We use offsets to position them so Y=0 is ground level for the entity.
 */
public class QuadrupedModel {

    public ModelPart head;
    public ModelPart body;
    public ModelPart frontLeftLeg;
    public ModelPart frontRightLeg;
    public ModelPart backLeftLeg;
    public ModelPart backRightLeg;

    public final ModelPart root;

    /**
     * Protected constructor for subclasses to define parts manually.
     */
    protected QuadrupedModel() {
        this.root = new ModelPart(0, 0, 0);
        this.head = null;
        this.body = null;
        this.frontLeftLeg = null;
        this.frontRightLeg = null;
        this.backLeftLeg = null;
        this.backRightLeg = null;
    }

    /**
     * Create a quadruped model.
     * Parts are positioned so that Y=0 in model space = ground level.
     * 
     * @param bodyWidth  Width of body (X axis)
     * @param bodyHeight Height of body (Y axis)
     * @param bodyLength Length of body (Z axis)
     * @param legWidth   Width of each leg (X and Z)
     * @param legHeight  Height/length of legs
     * @param headWidth  Width of head
     * @param headHeight Height of head
     * @param headDepth  Depth of head (Z)
     */
    public QuadrupedModel(float bodyWidth, float bodyHeight, float bodyLength,
            float legWidth, float legHeight,
            float headWidth, float headHeight, float headDepth) {

        root = new ModelPart(0, 0, 0);

        // Leg positioning:
        // - Legs are boxes centered at their offset position
        // - A leg of height 12 extends from -6 to +6 around its center
        // - To have feet at Y=0, leg center must be at Y = legHeight/2

        float legCenterY = legHeight / 2;
        float legOffsetX = (bodyWidth / 2) - (legWidth / 2);
        float legOffsetZ = (bodyLength / 2) - (legWidth / 2);

        frontRightLeg = new ModelPart(legWidth, legHeight, legWidth)
                .setTextureOffset(0, 16)
                .setOffset(-legOffsetX, legCenterY, -legOffsetZ)
                .setPivot(0, legHeight / 2, 0); // Pivot at top of leg (hip joint)

        frontLeftLeg = new ModelPart(legWidth, legHeight, legWidth)
                .setTextureOffset(0, 16)
                .setOffset(legOffsetX, legCenterY, -legOffsetZ)
                .setPivot(0, legHeight / 2, 0);

        backRightLeg = new ModelPart(legWidth, legHeight, legWidth)
                .setTextureOffset(0, 16)
                .setOffset(-legOffsetX, legCenterY, legOffsetZ)
                .setPivot(0, legHeight / 2, 0);

        backLeftLeg = new ModelPart(legWidth, legHeight, legWidth)
                .setTextureOffset(0, 16)
                .setOffset(legOffsetX, legCenterY, legOffsetZ)
                .setPivot(0, legHeight / 2, 0);

        // Body positioning:
        // - Body sits on top of legs
        // - Body center Y = legHeight + bodyHeight/2
        float bodyCenterY = legHeight + bodyHeight / 2;

        // Body UV offset: Must fit within 64x32
        // For a body of width W, height H, depth D:
        // Row 1 (top/bottom) needs: D + W + W + D = 2D + 2W pixels horizontally, D
        // pixels vertically
        // Row 2 (sides) needs: D + W + D + W = 2D + 2W pixels horizontally, H pixels
        // vertically
        // Total: (2D + 2W) x (D + H) pixels
        // For cow body 12x10x16: (32+24)x(16+10) = 56x26, fits starting at (0, 0) or
        // (0, 4)
        body = new ModelPart(bodyWidth, bodyHeight, bodyLength)
                .setTextureOffset(0, 4) // Start body at row 4 to avoid head overlap
                .setOffset(0, bodyCenterY, 0)
                .setPivot(0, 0, 0);

        // Head positioning:
        // - Head attaches to front of body, near top
        // - Head center Y ≈ legHeight + bodyHeight (at neck level)
        float headCenterY = legHeight + bodyHeight - headHeight / 4; // Slightly below top of body
        float headCenterZ = -bodyLength / 2 - headDepth / 2 + 2; // In front of body

        head = new ModelPart(headWidth, headHeight, headDepth)
                .setTextureOffset(0, 0)
                .setOffset(0, headCenterY, headCenterZ)
                .setPivot(0, 0, headDepth / 2); // Pivot at back of head (neck)

        root.addChild(frontRightLeg);
        root.addChild(frontLeftLeg);
        root.addChild(backRightLeg);
        root.addChild(backLeftLeg);
        root.addChild(body);
        root.addChild(head);
    }

    /**
     * Create standard cow model.
     * Cow: body 12x10x16, legs 4x12, head 8x8x6
     * UV space: head 22x14, body 56x26, legs 12x18 - all fit in 64x32
     */
    public static QuadrupedModel createCow() {
        return new QuadrupedModel(12, 10, 16, 4, 12, 8, 8, 6);
    }

    /**
     * Create standard pig model.
     * Pig: body 10x8x14, legs 4x6, head 8x8x8
     * Total height: 6 (legs) + 8 (body) = 14 model units = 0.875 blocks
     */
    public static QuadrupedModel createPig() {
        return new QuadrupedModel(10, 8, 14, 4, 6, 8, 8, 8);
    }

    /**
     * Create standard sheep model.
     * Sheep: body 10x9x16, legs 4x12, head 6x6x6
     */
    public static QuadrupedModel createSheep() {
        return new QuadrupedModel(10, 9, 16, 4, 12, 6, 6, 6);
    }

    public void buildMeshes() {
        root.buildMesh();
    }

    public void animate(float limbSwing, float limbSwingAmount, float ageInTicks) {
        // Head idle animation
        head.setRotation(
                (float) Math.sin(ageInTicks * 0.1f) * 0.05f,
                0,
                0);

        // Leg walk animation - diagonal pairs move together (like real quadrupeds)
        float swing = (float) Math.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;

        frontRightLeg.setRotation(swing, 0, 0);
        backLeftLeg.setRotation(swing, 0, 0);
        frontLeftLeg.setRotation(-swing, 0, 0);
        backRightLeg.setRotation(-swing, 0, 0);
    }

    public void cleanup() {
        root.cleanup();
    }
}
