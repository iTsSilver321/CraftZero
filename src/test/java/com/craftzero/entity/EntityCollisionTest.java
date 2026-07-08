package com.craftzero.entity;

import com.craftzero.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityCollisionTest {
    @Test
    @DisplayName("Living entity collision push should use the Release max-axis impulse")
    void livingEntityCollisionPushUsesReleaseMaxAxisImpulse() {
        assertCollisionPush(-0.03535534f, 0.5f, 2.0f);
        assertCollisionPush(-0.05f, 2.0f, 4.0f);
    }

    private static void assertCollisionPush(float expectedSubjectMotionX, float otherX, float otherWidth) {
        World world = new World(503L);
        try {
            TestLivingEntity subject = new TestLivingEntity(0.6f, 0.0f, 90.0f, 0.0f);
            TestLivingEntity other = new TestLivingEntity(otherWidth, otherX, 90.0f, 0.0f);
            world.replaceEntities(List.of(subject, other));

            subject.pushAgainstWorldEntities();

            assertEquals(expectedSubjectMotionX, subject.getMotionX(), 0.0001f);
            assertEquals(-expectedSubjectMotionX, other.getMotionX(), 0.0001f);
        } finally {
            world.cleanup();
        }
    }

    private static final class TestLivingEntity extends LivingEntity {
        private TestLivingEntity(float width, float x, float y, float z) {
            super(width, 1.8f, 20.0f);
            setPosition(x, y, z);
        }

        private void pushAgainstWorldEntities() {
            pushOutOfEntities();
        }
    }
}
