package com.craftzero.graphics.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WolfModelPoseTest {
    @Test
    @DisplayName("Wolf model exposes a distinct sitting pose")
    void sittingPoseFoldsBodyAndLegs() {
        WolfModel model = new WolfModel();

        model.animate(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, true, false, false);

        assertEquals(0.72f, model.body.getRotationX(), 0.001f);
        assertEquals(1.45f, model.backLeftLeg.getRotationX(), 0.001f);
        assertEquals(0.18f, model.frontLeftLeg.getRotationX(), 0.001f);
    }

    @Test
    @DisplayName("Wolf model rolls its head while begging")
    void beggingPoseRollsHead() {
        WolfModel model = new WolfModel();

        model.animate(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, false, true, false);

        assertTrue(model.head.getRotationZ() > 0.20f);
        assertEquals(model.head.getRotationZ(), model.snout.getRotationZ(), 0.001f);
    }

    @Test
    @DisplayName("Angry wolves hold the tail steady")
    void angryWolfTailDoesNotWag() {
        WolfModel model = new WolfModel();

        model.animate(0.0f, 1.0f, 10.0f, 0.0f, 0.0f, false, false, true);

        assertEquals(0.0f, model.tail.getRotationY(), 0.001f);
        assertEquals(1.5393804f, model.tail.getRotationX(), 0.001f);
    }

    @Test
    @DisplayName("Tamed wolf tail pitch should expose Release-era health feedback")
    void tamedWolfTailPitchFollowsHealth() {
        float fullHealth = WolfModel.releaseTailPitch(false, false, true, 20.0f);
        float halfHealth = WolfModel.releaseTailPitch(false, false, true, 10.0f);
        float sitting = WolfModel.releaseTailPitch(true, false, true, 1.0f);

        assertEquals(0.55f * (float) Math.PI, fullHealth, 0.0001f);
        assertEquals(0.35f * (float) Math.PI, halfHealth, 0.0001f);
        assertTrue(halfHealth < fullHealth);
        assertEquals(1.7278761f, sitting, 0.0001f);
    }

    @Test
    @DisplayName("Wolf model applies tamed health tail pitch during animation")
    void animatedTamedWolfUsesHealthTailPitch() {
        WolfModel model = new WolfModel();

        model.animate(0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                false, false, false, true, 10.0f, 0.0f, 0.0f, 0.0f);

        assertEquals(0.35f * (float) Math.PI, model.tail.getRotationX(), 0.0001f);
    }

    @Test
    @DisplayName("Wolf shake animation rolls head body and tail")
    void shakePoseRollsWolfParts() {
        WolfModel model = new WolfModel();

        model.animate(0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                false, false, false, 0.12f, -0.24f, 0.36f);

        assertEquals(0.12f, model.head.getRotationZ(), 0.001f);
        assertEquals(model.head.getRotationZ(), model.snout.getRotationZ(), 0.001f);
        assertEquals(-0.24f, model.body.getRotationZ(), 0.001f);
        assertEquals(-0.24f, model.mane.getRotationZ(), 0.001f);
        assertEquals(0.36f, model.tail.getRotationZ(), 0.001f);
    }
}
