package com.craftzero.graphics.model;

/**
 * Humanoid mob model (Zombie, Skeleton, Player).
 * Has head, body, two arms, and two legs with animation support.
 */
public class HumanoidModel {

        // Body parts
        public final ModelPart head;
        public final ModelPart body;
        public final ModelPart leftArm;
        public final ModelPart rightArm;
        public final ModelPart leftLeg;
        public final ModelPart rightLeg;

        // Root for transforms
        public final ModelPart root;

        public HumanoidModel() {
                // Create root
                root = new ModelPart(0, 0, 0);

                // Head: 8x8x8, pivot at neck
                head = new ModelPart(8, 8, 8)
                                .setTextureOffset(0, 0)
                                .setOffset(0, 24, 0)
                                .setPivot(0, 0, 0);

                // Body: 8x12x4
                body = new ModelPart(8, 12, 4)
                                .setTextureOffset(16, 16)
                                .setOffset(0, 12, 0)
                                .setPivot(0, 0, 0);

                // Right Arm: 4x12x4, pivot at shoulder
                rightArm = new ModelPart(4, 12, 4)
                                .setTextureOffset(40, 16)
                                .setOffset(-6, 22, 0)
                                .setPivot(0, 10, 0); // Pivot at top (shoulder)

                // Left Arm: 4x12x4, pivot at shoulder
                leftArm = new ModelPart(4, 12, 4)
                                .setTextureOffset(40, 16)
                                .setOffset(6, 22, 0)
                                .setPivot(0, 10, 0);

                // Right Leg: 4x12x4, pivot at hip
                rightLeg = new ModelPart(4, 12, 4)
                                .setTextureOffset(0, 16)
                                .setOffset(-2, 12, 0)
                                .setPivot(0, 12, 0); // Pivot at top (hip)

                // Left Leg: 4x12x4, pivot at hip
                leftLeg = new ModelPart(4, 12, 4)
                                .setTextureOffset(0, 16)
                                .setOffset(2, 12, 0)
                                .setPivot(0, 12, 0);

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
         * 
         * @param limbSwing       How far the entity has walked (for phase)
         * @param limbSwingAmount How fast the entity is moving (for amplitude)
         * @param ageInTicks      Entity age for idle animations
         * @param headYaw         Head yaw rotation
         * @param headPitch       Head pitch rotation
         */
        public void animate(float limbSwing, float limbSwingAmount, float ageInTicks,
                        float headYaw, float headPitch) {
                // Head follows look direction
                head.setRotation(
                                (float) Math.toRadians(headPitch),
                                (float) Math.toRadians(headYaw),
                                0);

                // Leg swing animation
                float legSwing = (float) Math.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount;
                rightLeg.setRotation(legSwing, 0, 0);
                leftLeg.setRotation(-legSwing, 0, 0);

                // Arm swing (opposite of legs)
                float armSwing = (float) Math.cos(limbSwing * 0.6662f) * 1.0f * limbSwingAmount;
                rightArm.setRotation(-armSwing, 0, 0);
                leftArm.setRotation(armSwing, 0, 0);

                // Subtle idle breathing animation
                float breathe = (float) Math.sin(ageInTicks * 0.1f) * 0.02f;
                body.setRotation(breathe, 0, 0);
        }

        /**
         * Set attack animation (arm swing).
         * 
         * @param progress 0-1 attack progress
         */
        public void setAttackAnimation(float progress) {
                if (progress > 0) {
                        float swing = (float) Math.sin(progress * Math.PI) * 1.5f;
                        rightArm.setRotation(-swing, 0, 0);
                }
        }

        /**
         * Cleanup resources.
         */
        public void cleanup() {
                root.cleanup();
        }
}
