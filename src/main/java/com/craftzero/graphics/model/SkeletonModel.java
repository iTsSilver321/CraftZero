package com.craftzero.graphics.model;

/**
 * Skeleton mob model - humanoid with thin limbs.
 * Skeletons have 2-pixel wide arms and legs instead of 4.
 * Uses authentic Minecraft skeleton proportions.
 * 
 * Texture layout for skeleton (64x32):
 * - Head: 8x8x8 at (0,0)
 * - Body: 8x12x4 at (16,16)
 * - Arms: 2x12x2 at (40,16) - THIN
 * - Legs: 2x12x2 at (0,16) - THIN
 */
public class SkeletonModel {

    // Body parts
    public final ModelPart head;
    public final ModelPart body;
    public final ModelPart leftArm;
    public final ModelPart rightArm;
    public final ModelPart leftLeg;
    public final ModelPart rightLeg;

    // Root for transforms
    public final ModelPart root;

    public SkeletonModel() {
        // Create root at feet level
        root = new ModelPart();

        // Measurements (in model units, 16 units = 1 block):
        // Total height: 32 units (2 blocks) - same as zombie
        // - Legs: 12 units (hip at Y=12)
        // - Body: 12 units (shoulder at Y=24)
        // - Head: 8 units (top at Y=32)
        // BUT: arms and legs are only 2 units WIDE instead of 4

        // Head: 8x8x8 (same as zombie)
        // Pivot at neck (Y=24), head extends up
        head = new ModelPart()
                .setTextureOffset(0, 0)
                .setPivot(0, 24, 0)
                .addBox(-4, 0, -4, 8, 8, 8);

        // Body: 8x12x4 (same as zombie)
        // Pivot at waist (Y=12), body extends up to Y=24
        body = new ModelPart()
                .setTextureOffset(16, 16)
                .setPivot(0, 12, 0)
                .addBox(-4, 0, -2, 8, 12, 4);

        // Right Arm: 2x12x2 (THIN - skeleton specific)
        // Pivot at right shoulder (Y=22, X=-5)
        // Arm hangs from shoulder, box extends DOWN from pivot
        rightArm = new ModelPart()
                .setTextureOffset(40, 16)
                .setPivot(-5, 22, 0)
                .addBox(-1, -10, -1, 2, 12, 2);

        // Left Arm: 2x12x2 (THIN - skeleton specific)
        // Pivot at left shoulder (Y=22, X=5)
        leftArm = new ModelPart()
                .setTextureOffset(40, 16)
                .setPivot(5, 22, 0)
                .addBox(-1, -10, -1, 2, 12, 2);

        // Right Leg: 2x12x2 (THIN - skeleton specific)
        // Pivot at right hip (Y=12, X=-2)
        rightLeg = new ModelPart()
                .setTextureOffset(0, 16)
                .setPivot(-2, 12, 0)
                .addBox(-1, -12, -1, 2, 12, 2);

        // Left Leg: 2x12x2 (THIN - skeleton specific)
        // Pivot at left hip (Y=12, X=2)
        leftLeg = new ModelPart()
                .setTextureOffset(0, 16)
                .setPivot(2, 12, 0)
                .addBox(-1, -12, -1, 2, 12, 2);

        // Build hierarchy
        root.addChild(head);
        root.addChild(body);
        root.addChild(rightArm);
        root.addChild(leftArm);
        root.addChild(rightLeg);
        root.addChild(leftLeg);
    }

    /**
     * Build meshes for all parts.
     */
    public void buildMeshes() {
        root.buildMesh();
    }

    /**
     * Animate the model based on movement.
     * Skeletons have arms at rest (not raised like zombies).
     * 
     * @param limbSwing       How far the entity has walked (for phase)
     * @param limbSwingAmount How fast the entity is moving (for amplitude)
     * @param ageInTicks      Entity age for idle animations
     * @param headYaw         Head yaw rotation (relative to body)
     * @param headPitch       Head pitch rotation
     */
    public void animate(float limbSwing, float limbSwingAmount, float ageInTicks,
            float headYaw, float headPitch) {
        // Clamp head rotation
        headYaw = Math.max(-60, Math.min(60, headYaw));
        headPitch = Math.max(-45, Math.min(45, headPitch));

        // Head follows look direction
        head.setRotation(
                (float) Math.toRadians(headPitch),
                (float) Math.toRadians(headYaw),
                0);

        // Leg swing animation
        float legSwing = (float) Math.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
        rightLeg.setRotation(legSwing, 0, 0);
        leftLeg.setRotation(-legSwing, 0, 0);

        // Arm swing - skeletons hold arms more naturally (not zombie pose)
        // Arms swing opposite to legs during walking
        float armSwing = (float) Math.cos(limbSwing * 0.6662f) * 1.0f * limbSwingAmount;
        rightArm.setRotation(-armSwing, 0, 0);
        leftArm.setRotation(armSwing, 0, 0);

        // Subtle idle breathing animation
        float breathe = (float) Math.sin(ageInTicks * 0.1f) * 0.02f;
        body.setRotation(breathe, 0, 0);
    }

    /**
     * Set bow drawing animation (for ranged attack).
     * 
     * @param progress 0-1 bow draw progress
     */
    public void setBowAnimation(float progress) {
        if (progress > 0) {
            // Right arm pulls back bow
            rightArm.setRotation(-1.5f, -0.5f, 0);
            // Left arm holds bow forward
            leftArm.setRotation(-1.5f, 0.5f, 0);
        }
    }

    /**
     * Cleanup resources.
     */
    public void cleanup() {
        root.cleanup();
    }
}
