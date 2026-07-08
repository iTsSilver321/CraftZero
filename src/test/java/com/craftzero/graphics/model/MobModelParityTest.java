package com.craftzero.graphics.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobModelParityTest {
    @Test
    @DisplayName("Player walk animation should use the classic humanoid limb phase")
    void playerWalkAnimationUsesClassicHumanoidPhase() {
        PlayerModel model = new PlayerModel();

        model.animate(1.0f, 0.5f, 0.0f, 0.0f, 0.0f, 0.0f, false);

        float expectedLegSwing = (float) Math.cos(1.0f * 0.6662f) * 1.4f * 0.5f;
        float expectedArmSwing = (float) Math.cos(1.0f * 0.6662f) * 0.5f;
        assertEquals(-expectedLegSwing, model.rightLeg.getRotationX(), 0.001f);
        assertEquals(expectedLegSwing, model.leftLeg.getRotationX(), 0.001f);
        assertEquals(-expectedArmSwing, model.rightArm.getRotationX(), 0.001f);
        assertEquals(expectedArmSwing, model.leftArm.getRotationX(), 0.001f);
    }

    @Test
    @DisplayName("Spider model should expose classic body sections and eight legs")
    void spiderModelHasClassicParts() {
        SpiderModel model = new SpiderModel();

        assertEquals(11, model.root.getChildren().size());
        assertEquals(8, model.legs.length);
        assertSame(model.head, model.root.getChildren().get(0));
        assertSame(model.neck, model.root.getChildren().get(1));
        assertSame(model.body, model.root.getChildren().get(2));
    }

    @Test
    @DisplayName("Spider animation should mirror left and right leg phases")
    void spiderLegAnimationMirrorsSides() {
        SpiderModel model = new SpiderModel();

        model.animate(4.0f, 0.75f, 0.0f, 90.0f, -90.0f);

        assertEquals(Math.toRadians(60.0), model.head.getRotationY(), 0.001);
        assertEquals(Math.toRadians(-45.0), model.head.getRotationX(), 0.001);
        assertEquals(-model.leg2.getRotationY(), model.leg1.getRotationY(), 0.001f);
        assertEquals(-model.leg2.getRotationZ(), model.leg1.getRotationZ(), 0.001f);
        assertEquals(-model.leg4.getRotationY(), model.leg3.getRotationY(), 0.001f);
        assertEquals(-model.leg4.getRotationZ(), model.leg3.getRotationZ(), 0.001f);
        assertTrue(model.leg1.getRotationZ() > 0.0f);
        assertTrue(model.leg2.getRotationZ() < 0.0f);
    }

    @Test
    @DisplayName("Chicken model should expose head, bill, chin, body, legs, and wings")
    void chickenModelHasClassicParts() {
        ChickenModel model = new ChickenModel();

        assertEquals(8, model.root.getChildren().size());
        assertSame(model.head, model.root.getChildren().get(0));
        assertSame(model.bill, model.root.getChildren().get(1));
        assertSame(model.chin, model.root.getChildren().get(2));
        assertSame(model.body, model.root.getChildren().get(3));
    }

    @Test
    @DisplayName("Chicken animation should flap mirrored wings and copy head pose to bill and chin")
    void chickenAnimationMirrorsWingsAndHeadAttachments() {
        ChickenModel model = new ChickenModel();

        model.animate(1.0f, 0.8f, 3.0f, 90.0f, 60.0f, true);

        assertEquals(Math.toRadians(60.0), model.head.getRotationY(), 0.001);
        assertEquals(Math.toRadians(35.0), model.head.getRotationX(), 0.001);
        assertEquals(model.head.getRotationY(), model.bill.getRotationY(), 0.001f);
        assertEquals(model.head.getRotationX(), model.chin.getRotationX(), 0.001f);
        assertEquals(-model.leftWing.getRotationZ(), model.rightWing.getRotationZ(), 0.001f);
        assertEquals(-model.leftLeg.getRotationX(), model.rightLeg.getRotationX(), 0.001f);
    }

    @Test
    @DisplayName("Slime squish scale should stretch jumps and squash landings")
    void slimeSquishScaleStretchesAndSquashes() {
        SlimeModel.SquishScale jumping = SlimeModel.scaleFor(1.0f, 4);
        assertEquals(0.75f, jumping.horizontal(), 0.0001f);
        assertEquals(1.3333f, jumping.vertical(), 0.0001f);

        SlimeModel.SquishScale landing = SlimeModel.scaleFor(-0.5f, 4);
        assertEquals(1.2f, landing.horizontal(), 0.0001f);
        assertEquals(0.8333f, landing.vertical(), 0.0001f);
    }

    @Test
    @DisplayName("Pig saddle overlay should use the inflated Release-era render-pass model")
    void pigSaddleOverlayUsesInflatedRenderPassModel() {
        PigModel base = PigModel.create();
        PigModel saddle = PigModel.createSaddleOverlay();

        assertEquals(0.0f, base.getInflate(), 0.0001f);
        assertEquals(PigModel.SADDLE_OVERLAY_INFLATE, saddle.getInflate(), 0.0001f);
        assertEquals(base.root.getChildren().size(), saddle.root.getChildren().size());
        assertSame(saddle.snout, saddle.head.getChildren().get(0));
    }

    @Test
    @DisplayName("Villager model should expose head nose robe and folded arms")
    void villagerModelHasReleaseShapeParts() {
        VillagerModel model = new VillagerModel();

        assertEquals(5, model.root.getChildren().size());
        assertSame(model.head, model.root.getChildren().get(0));
        assertSame(model.nose, model.head.getChildren().get(0));
        assertSame(model.body, model.root.getChildren().get(1));
        assertSame(model.arms, model.root.getChildren().get(2));
        assertEquals(3, model.arms.getChildren().size());
        assertSame(model.foldedHands, model.arms.getChildren().get(2));
    }

    @Test
    @DisplayName("Villager animation should keep folded arms while walking")
    void villagerAnimationKeepsFoldedArmsAndMirrorsLegs() {
        VillagerModel model = new VillagerModel();

        model.animate(2.0f, 0.5f, 90.0f, 60.0f);

        assertEquals(Math.toRadians(60.0), model.head.getRotationY(), 0.001);
        assertEquals(Math.toRadians(45.0), model.head.getRotationX(), 0.001);
        assertEquals(0.75f, model.arms.getRotationX(), 0.001f);
        assertEquals(-model.leftLeg.getRotationX(), model.rightLeg.getRotationX(), 0.001f);
    }

    @Test
    @DisplayName("Enderman carried-block pose should raise both arms around the held block")
    void endermanCarriedBlockPoseRaisesArms() {
        HumanoidModel model = new HumanoidModel();

        model.animateEnderman(0.0f, 0.0f, 0.0f, 90.0f, 60.0f, true);

        assertEquals(Math.toRadians(60.0), model.head.getRotationY(), 0.001);
        assertEquals(Math.toRadians(45.0), model.head.getRotationX(), 0.001);
        assertEquals(0.5f, model.rightArm.getRotationX(), 0.001f);
        assertEquals(0.5f, model.leftArm.getRotationX(), 0.001f);
        assertEquals(0.05f, model.rightArm.getRotationZ(), 0.001f);
        assertEquals(-0.05f, model.leftArm.getRotationZ(), 0.001f);
    }

    @Test
    @DisplayName("Snow Golem model should use the source body yaw and stick arm pose")
    void snowGolemModelUsesSourceBodyYawAndStickArms() {
        SnowGolemModel model = new SnowGolemModel();

        model.animate(4.0f, 0.75f, 10.0f, 40.0f, 20.0f);

        float headYaw = (float) Math.toRadians(40.0f);
        float bodyYaw = headYaw * 0.25f;
        float armX = (float) Math.cos(bodyYaw) * 5.0f;
        float armZ = (float) Math.sin(bodyYaw) * 5.0f;

        assertEquals(Math.toRadians(20.0), model.head.getRotationX(), 0.001);
        assertEquals(headYaw, model.head.getRotationY(), 0.001f);
        assertEquals(bodyYaw, model.body.getRotationY(), 0.001f);
        assertEquals(0.0f, model.bottom.getRotationZ(), 0.001f);
        assertEquals(armX, model.leftArm.getPivotX(), 0.001f);
        assertEquals(-armZ, model.leftArm.getPivotZ(), 0.001f);
        assertEquals(-armX, model.rightArm.getPivotX(), 0.001f);
        assertEquals(armZ, model.rightArm.getPivotZ(), 0.001f);
        assertEquals(bodyYaw, model.leftArm.getRotationY(), 0.001f);
        assertEquals((float) Math.PI + bodyYaw, model.rightArm.getRotationY(), 0.001f);
        assertEquals(1.0f, model.leftArm.getRotationZ(), 0.001f);
        assertEquals(-1.0f, model.rightArm.getRotationZ(), 0.001f);
    }

    @Test
    @DisplayName("Skeleton ranged attack pose should raise both arms into a bow aim")
    void skeletonBowPoseRaisesAimingArmsAndResets() {
        SkeletonModel model = new SkeletonModel();

        model.animate(0.0f, 0.0f, 0.0f, 15.0f, 0.0f);
        model.setBowAnimation(1.0f);

        assertEquals(-1.5f, model.rightArm.getRotationX(), 0.001f);
        assertEquals(-0.5f, model.rightArm.getRotationY(), 0.001f);
        assertEquals(-1.5f, model.leftArm.getRotationX(), 0.001f);
        assertEquals(0.5f, model.leftArm.getRotationY(), 0.001f);

        model.animate(0.0f, 0.0f, 0.0f, 15.0f, 0.0f);

        assertEquals(0.0f, model.rightArm.getRotationX(), 0.001f);
        assertEquals(0.0f, model.rightArm.getRotationY(), 0.001f);
        assertEquals(0.0f, model.leftArm.getRotationX(), 0.001f);
        assertEquals(0.0f, model.leftArm.getRotationY(), 0.001f);
    }

    @Test
    @DisplayName("Blaze rods should use the Release-era three-ring orbit and bob")
    void blazeRodsUseReleaseOrbitAndBob() {
        BlazeModel model = new BlazeModel();

        model.animate(10.0f, 30.0f, -15.0f);

        assertEquals(Math.toRadians(30.0), model.head.getRotationY(), 0.001);
        assertEquals(Math.toRadians(-15.0), model.head.getRotationX(), 0.001);
        assertEquals(12, model.rods.length);

        assertRod(model.rods[0], 10.0f * (float) Math.PI * -0.1f,
                9.0f, -2.0f + (float) Math.cos((0.0f * 2.0f + 10.0f) * 0.25f));
        assertRod(model.rods[4], 0.7853982f + 10.0f * (float) Math.PI * 0.03f,
                7.0f, 2.0f + (float) Math.cos((4.0f * 2.0f + 10.0f) * 0.25f));
        assertRod(model.rods[8], 0.47123894f + 10.0f * (float) Math.PI * -0.05f,
                5.0f, 11.0f + (float) Math.cos((8.0f * 1.5f + 10.0f) * 0.5f));
    }

    @Test
    @DisplayName("Blaze rods should animate instead of staying on fixed-height rings")
    void blazeRodsAnimateAwayFromFixedRings() {
        BlazeModel model = new BlazeModel();

        model.animate(0.0f, 0.0f, 0.0f);
        float firstX = model.rods[0].getPivotX();
        float firstY = model.rods[0].getPivotY();
        float lowerZ = model.rods[8].getPivotZ();

        model.animate(7.0f, 0.0f, 0.0f);

        assertNotEquals(firstX, model.rods[0].getPivotX(), 0.001f);
        assertNotEquals(firstY, model.rods[0].getPivotY(), 0.001f);
        assertNotEquals(lowerZ, model.rods[8].getPivotZ(), 0.001f);
        assertEquals(0.0f, model.rods[0].getRotationY(), 0.001f);
    }

    private static void assertRod(ModelPart rod, float angle, float radius, float sourceY) {
        assertEquals((float) Math.cos(angle) * radius, rod.getPivotX(), 0.001f);
        assertEquals(18.0f - sourceY, rod.getPivotY(), 0.001f);
        assertEquals((float) Math.sin(angle) * radius, rod.getPivotZ(), 0.001f);
        assertEquals(0.0f, rod.getRotationX(), 0.001f);
        assertEquals(0.0f, rod.getRotationY(), 0.001f);
        assertEquals(0.0f, rod.getRotationZ(), 0.001f);
    }
}
