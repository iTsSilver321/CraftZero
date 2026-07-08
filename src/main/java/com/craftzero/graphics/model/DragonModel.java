package com.craftzero.graphics.model;

import com.craftzero.entity.mob.EnderDragon;

public class DragonModel {
    private static final int NECK_SEGMENT_COUNT = 3;
    private static final int TAIL_SEGMENT_COUNT = 5;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    public final ModelPart root = new ModelPart();
    public final ModelPart body;
    private final ModelPart head;
    private final ModelPart[] neckSegments = new ModelPart[NECK_SEGMENT_COUNT];
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart[] tailSegments = new ModelPart[TAIL_SEGMENT_COUNT];

    public DragonModel() {
        body = new ModelPart()
                .setTextureSize(256, 256)
                .setTextureOffset(0, 0)
                .setPivot(0, 18, 0)
                .addBox(-12, -12, -32, 24, 24, 64);

        ModelPart neckParent = root;
        for (int i = 0; i < neckSegments.length; i++) {
            ModelPart neck = new ModelPart()
                    .setTextureSize(256, 256)
                    .setTextureOffset(112, 30)
                    .setPivot(0, i == 0 ? 16 : 0, i == 0 ? -32 : -14)
                    .addBox(-5, -5, -14, 10, 10, 14);
            neckSegments[i] = neck;
            neckParent.addChild(neck);
            neckParent = neck;
        }

        head = new ModelPart()
                .setTextureSize(256, 256)
                .setTextureOffset(0, 88)
                .setPivot(0, 0, -14)
                .addBox(-8, -8, -16, 16, 16, 16);
        leftWing = new ModelPart()
                .setTextureSize(256, 256)
                .setTextureOffset(112, 88)
                .setPivot(12, 12, -8)
                .addBox(0, -2, -8, 48, 4, 24);
        rightWing = new ModelPart()
                .setTextureSize(256, 256)
                .setTextureOffset(112, 88)
                .setPivot(-12, 12, -8)
                .addBox(-48, -2, -8, 48, 4, 24);

        ModelPart tailParent = root;
        for (int i = 0; i < tailSegments.length; i++) {
            ModelPart tail = new ModelPart()
                    .setTextureSize(256, 256)
                    .setTextureOffset(0, 120)
                    .setPivot(0, i == 0 ? 18 : 0, i == 0 ? 32 : 14)
                    .addBox(-5, -5, 0, 10, 10, 14);
            tailSegments[i] = tail;
            tailParent.addChild(tail);
            tailParent = tail;
        }

        root.addChild(body);
        neckParent.addChild(head);
        root.addChild(leftWing);
        root.addChild(rightWing);
    }

    public void buildMeshes() {
        root.buildMesh();
    }

    public void animate(float ageInTicks, float headYaw, float headPitch) {
        DragonArticulation articulation = fallbackArticulation(ageInTicks, headYaw, headPitch);
        applyArticulation(articulation);
    }

    public void animate(EnderDragon dragon, float ageInTicks, float partialTick, float headYaw) {
        applyArticulation(articulationFor(dragon, ageInTicks, partialTick, headYaw));
    }

    public void cleanup() {
        root.cleanup();
    }

    public ModelPart[] neckSegments() {
        return neckSegments.clone();
    }

    public ModelPart[] tailSegments() {
        return tailSegments.clone();
    }

    public ModelPart head() {
        return head;
    }

    public ModelPart leftWing() {
        return leftWing;
    }

    public ModelPart rightWing() {
        return rightWing;
    }

    public static DragonArticulation articulationFor(EnderDragon dragon,
            float ageInTicks, float partialTick, float headYaw) {
        if (dragon == null) {
            return fallbackArticulation(ageInTicks, headYaw, 0.0f);
        }
        double[] current = dragon.getMovementOffset(0, partialTick);
        float baseYaw = (float) current[0];
        float baseY = (float) current[1];
        float basePitch = (float) current[2];

        DragonSegmentPose[] neck = new DragonSegmentPose[NECK_SEGMENT_COUNT];
        for (int i = 0; i < neck.length; i++) {
            double[] offset = dragon.getMovementOffset((i + 1) * 2, partialTick);
            float yaw = wrapDegrees((float) offset[0] - baseYaw) * DEG_TO_RAD * 0.25f;
            float pitch = ((float) offset[2] - basePitch) * DEG_TO_RAD * 0.18f;
            float vertical = ((float) offset[1] - baseY) * 0.035f;
            neck[i] = new DragonSegmentPose(yaw, pitch + vertical, 0.0f);
        }

        DragonSegmentPose[] tail = new DragonSegmentPose[TAIL_SEGMENT_COUNT];
        for (int i = 0; i < tail.length; i++) {
            double[] offset = dragon.getMovementOffset(10 + i * 5, partialTick);
            float yaw = wrapDegrees(baseYaw - (float) offset[0]) * DEG_TO_RAD * 0.35f;
            float pitch = (basePitch - (float) offset[2]) * DEG_TO_RAD * 0.12f
                    + (float) Math.sin(ageInTicks * 0.08f + i * 0.45f) * 0.045f;
            float roll = (float) Math.sin(ageInTicks * 0.10f + i * 0.7f) * 0.025f;
            tail[i] = new DragonSegmentPose(yaw, pitch, roll);
        }

        float flap = (float) Math.sin(ageInTicks * 0.18f) * 0.45f;
        float headPitch = basePitch * DEG_TO_RAD * 0.35f;
        float headYawRadians = headYaw * DEG_TO_RAD * 0.15f;
        return new DragonArticulation(
                basePitch * DEG_TO_RAD * 0.10f,
                headYawRadians,
                headPitch,
                neck,
                tail,
                -0.35f - flap,
                0.35f + flap);
    }

    private static DragonArticulation fallbackArticulation(float ageInTicks, float headYaw, float headPitch) {
        DragonSegmentPose[] neck = new DragonSegmentPose[NECK_SEGMENT_COUNT];
        DragonSegmentPose[] tail = new DragonSegmentPose[TAIL_SEGMENT_COUNT];
        for (int i = 0; i < neck.length; i++) {
            neck[i] = new DragonSegmentPose(0.0f, 0.0f, 0.0f);
        }
        for (int i = 0; i < tail.length; i++) {
            tail[i] = new DragonSegmentPose(0.0f,
                    (float) Math.sin(ageInTicks * 0.08f + i * 0.45f) * 0.045f,
                    (float) Math.sin(ageInTicks * 0.10f + i * 0.7f) * 0.025f);
        }
        float flap = (float) Math.sin(ageInTicks * 0.18f) * 0.45f;
        return new DragonArticulation(
                0.0f,
                headYaw * DEG_TO_RAD * 0.25f,
                headPitch * DEG_TO_RAD * 0.25f,
                neck,
                tail,
                -0.35f - flap,
                0.35f + flap);
    }

    private void applyArticulation(DragonArticulation articulation) {
        body.setRotation(articulation.bodyPitch(), 0.0f, 0.0f);
        DragonSegmentPose[] neck = articulation.neckSegments();
        for (int i = 0; i < neckSegments.length; i++) {
            DragonSegmentPose pose = i < neck.length ? neck[i] : DragonSegmentPose.ZERO;
            neckSegments[i].setRotation(pose.pitch(), pose.yaw(), pose.roll());
        }
        head.setRotation(articulation.headPitch(), articulation.headYaw(), 0.0f);
        DragonSegmentPose[] tail = articulation.tailSegments();
        for (int i = 0; i < tailSegments.length; i++) {
            DragonSegmentPose pose = i < tail.length ? tail[i] : DragonSegmentPose.ZERO;
            tailSegments[i].setRotation(pose.pitch(), pose.yaw(), pose.roll());
        }
        leftWing.setRotation(0.0f, 0.0f, articulation.leftWingRoll());
        rightWing.setRotation(0.0f, 0.0f, articulation.rightWingRoll());
    }

    private static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0f;
        if (wrapped >= 180.0f) {
            wrapped -= 360.0f;
        }
        if (wrapped < -180.0f) {
            wrapped += 360.0f;
        }
        return wrapped;
    }

    public record DragonSegmentPose(float yaw, float pitch, float roll) {
        static final DragonSegmentPose ZERO = new DragonSegmentPose(0.0f, 0.0f, 0.0f);
    }

    public record DragonArticulation(
            float bodyPitch,
            float headYaw,
            float headPitch,
            DragonSegmentPose[] neckSegments,
            DragonSegmentPose[] tailSegments,
            float leftWingRoll,
            float rightWingRoll) {
        public DragonArticulation {
            neckSegments = neckSegments == null ? new DragonSegmentPose[0] : neckSegments.clone();
            tailSegments = tailSegments == null ? new DragonSegmentPose[0] : tailSegments.clone();
        }

        @Override
        public DragonSegmentPose[] neckSegments() {
            return neckSegments.clone();
        }

        @Override
        public DragonSegmentPose[] tailSegments() {
            return tailSegments.clone();
        }
    }
}
