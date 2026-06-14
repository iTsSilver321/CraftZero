package com.craftzero.graphics.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MobModelParityTest {
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
}
