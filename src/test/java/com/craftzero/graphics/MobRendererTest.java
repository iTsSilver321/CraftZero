package com.craftzero.graphics;

import com.craftzero.entity.mob.Mob;
import com.craftzero.entity.mob.Creeper;
import com.craftzero.entity.mob.Enderman;
import com.craftzero.entity.mob.Pig;
import com.craftzero.entity.mob.Skeleton;
import com.craftzero.entity.mob.Silverfish;
import com.craftzero.entity.mob.Spider;
import com.craftzero.entity.mob.Zombie;
import com.craftzero.world.BlockType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobRendererTest {
    @Test
    @DisplayName("Baby mob fire overlays should follow the rendered baby scale")
    void babyFireOverlayUsesRenderScale() {
        Pig adult = new Pig();
        Pig baby = new Pig();
        baby.setGrowingAge(Mob.BABY_GROWING_AGE);

        MobRenderer.FireOverlayTransform adultOverlay = MobRenderer.fireOverlayTransform(adult);
        MobRenderer.FireOverlayTransform babyOverlay = MobRenderer.fireOverlayTransform(baby);

        assertEquals(1.0f, adult.getRenderScale(), 0.0001f);
        assertEquals(0.5f, baby.getRenderScale(), 0.0001f);
        assertEquals(adultOverlay.width() * 0.5f, babyOverlay.width(), 0.0001f);
        assertEquals(adultOverlay.height() * 0.5f, babyOverlay.height(), 0.0001f);
        assertEquals(adultOverlay.centerYOffset() * 0.5f, babyOverlay.centerYOffset(), 0.0001f);
        assertEquals(babyOverlay.width(), babyOverlay.depth(), 0.0001f);
    }

    @Test
    @DisplayName("Only saddled pigs should expose the Release-era saddle render layer")
    void saddledPigsExposeSaddleRenderLayer() {
        Pig plain = new Pig();
        Pig saddled = new Pig();
        saddled.setSaddled(true);

        MobRenderer.PigSaddleLayer plainLayer = MobRenderer.pigSaddleLayer(plain);
        MobRenderer.PigSaddleLayer saddledLayer = MobRenderer.pigSaddleLayer(saddled);

        assertFalse(plainLayer.visible());
        assertTrue(saddledLayer.visible());
        assertEquals("/textures/mob/saddle.png", saddledLayer.texturePath());
        assertEquals(0.5f, saddledLayer.modelInflate(), 0.0001f);
    }

    @Test
    @DisplayName("Mob spawner previews should scale large mobs into the cage")
    void spawnerPreviewScalesLargeMobs() {
        Zombie zombie = new Zombie();
        Silverfish silverfish = new Silverfish();

        assertEquals(MobSpawnerRenderer.BASE_PREVIEW_SCALE / zombie.getHeight(),
                MobSpawnerRenderer.previewScale(zombie), 0.0001f);
        assertEquals(MobSpawnerRenderer.BASE_PREVIEW_SCALE,
                MobSpawnerRenderer.previewScale(silverfish), 0.0001f);
    }

    @Test
    @DisplayName("Ignited creepers should expose Release-era swelling and white fuse flash")
    void creeperFuseVisualUsesReleaseSwellingAndBlink() {
        Creeper idle = new Creeper();
        MobRenderer.CreeperFuseVisual idleVisual = MobRenderer.creeperFuseVisual(idle);

        assertEquals(1.0f, idleVisual.horizontalScale(), 0.0001f);
        assertEquals(1.0f, idleVisual.verticalScale(), 0.0001f);
        assertEquals(0.0f, idleVisual.whiteFlash(), 0.0001f);

        Creeper flashing = new Creeper();
        flashing.setFuseState(15, true);
        MobRenderer.CreeperFuseVisual flashingVisual = MobRenderer.creeperFuseVisual(flashing);

        assertTrue(flashingVisual.horizontalScale() > 1.0f);
        assertTrue(flashingVisual.verticalScale() > 1.0f);
        assertEquals(0.1f, flashingVisual.whiteFlash(), 0.0001f);

        Creeper finalFuse = new Creeper();
        finalFuse.setFuseState(finalFuse.getMaxFuseTime(), true);
        MobRenderer.CreeperFuseVisual finalVisual = MobRenderer.creeperFuseVisual(finalFuse);

        assertTrue(finalVisual.horizontalScale() > 1.35f);
        assertTrue(finalVisual.verticalScale() > 1.08f);
        assertEquals(0.0f, finalVisual.whiteFlash(), 0.0001f);
    }

    @Test
    @DisplayName("Enderman carried block rendering should expose the carried block transform")
    void endermanCarriedBlockTransformUsesCarriedState() {
        Enderman empty = new Enderman();
        assertFalse(MobRenderer.endermanCarriedBlockTransform(empty).visible());

        Enderman carrier = new Enderman();
        carrier.setCarriedBlock(BlockType.PUMPKIN, 2);

        MobRenderer.EndermanCarriedBlockTransform transform =
                MobRenderer.endermanCarriedBlockTransform(carrier);

        assertTrue(transform.visible());
        assertEquals(BlockType.PUMPKIN, transform.block());
        assertEquals(2, transform.metadata());
        assertEquals(1.72f, transform.centerYOffset(), 0.0001f);
        assertEquals(-0.55f, transform.forwardOffset(), 0.0001f);
        assertEquals(0.5f, transform.scale(), 0.0001f);
    }

    @Test
    @DisplayName("Spiders should expose the Release-era additive eye glow layer")
    void spidersExposeEyeGlowLayer() {
        assertFalse(MobRenderer.spiderEyeLayer(new Zombie()).visible());

        MobRenderer.SpiderEyeLayer layer = MobRenderer.spiderEyeLayer(new Spider());

        assertTrue(layer.visible());
        assertEquals("/textures/mob/spider_eyes.png", layer.texturePath());
        assertEquals(1.0f, layer.brightness(), 0.0001f);
        assertEquals(0.1f, layer.alphaCutoff(), 0.0001f);
    }

    @Test
    @DisplayName("Skeleton bow pose should follow the active ranged attack goal")
    void skeletonBowPoseFollowsRangedAttackState() {
        Skeleton idle = new Skeleton();
        assertEquals(0.0f, MobRenderer.skeletonBowPoseProgress(idle), 0.0001f);

        Skeleton attacking = new Skeleton();
        attacking.getAI().setMoveTarget(4.0f, 0.0f);
        attacking.getAI().tick();

        assertTrue(attacking.isRangedAttackActive());
        assertEquals(1.0f, MobRenderer.skeletonBowPoseProgress(attacking), 0.0001f);
        assertEquals(0.0f, MobRenderer.skeletonBowPoseProgress(new Zombie()), 0.0001f);
    }
}
