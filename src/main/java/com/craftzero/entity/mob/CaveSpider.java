package com.craftzero.entity.mob;

import com.craftzero.inventory.ItemType;
import com.craftzero.main.Difficulty;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;

public class CaveSpider extends Spider {
    public CaveSpider() {
        super(MobDefinition.CAVE_SPIDER, MobDefinition.CAVE_SPIDER.width(), MobDefinition.CAVE_SPIDER.height(),
                MobDefinition.CAVE_SPIDER.maxHealth(), "/textures/mob/cavespider.png");
        this.moveSpeed = MobDefinition.CAVE_SPIDER.moveSpeed();
        this.experienceValue = MobDefinition.CAVE_SPIDER.experienceValue();
    }

    @Override
    public void onSuccessfulMeleeHit(com.craftzero.main.Player player) {
        Difficulty difficulty = player.getDifficulty();
        if (difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD) {
            player.getStats().addEffect(new StatusEffectInstance(StatusEffectType.POISON,
                    difficulty == Difficulty.HARD ? 15 * 20 : 7 * 20, 0));
        }
    }

    @Override
    public void dropLoot() {
        dropItems(ItemType.STRING, 0, 2);
        dropItems(ItemType.SPIDER_EYE, 0, 1);
    }
}
