package com.craftzero.entity.mob;

import com.craftzero.main.Difficulty;
import com.craftzero.main.Player;
import com.craftzero.progression.StatusEffectInstance;
import com.craftzero.progression.StatusEffectType;
import com.craftzero.world.World;

public class CaveSpider extends Spider {
    public CaveSpider() {
        super(MobDefinition.CAVE_SPIDER, MobDefinition.CAVE_SPIDER.width(), MobDefinition.CAVE_SPIDER.height(),
                MobDefinition.CAVE_SPIDER.maxHealth(), "/textures/mob/cavespider.png");
        this.moveSpeed = MobDefinition.CAVE_SPIDER.moveSpeed();
        this.experienceValue = MobDefinition.CAVE_SPIDER.experienceValue();
    }

    @Override
    public void onSuccessfulMeleeHit(Player player) {
        Difficulty difficulty = player == null ? Difficulty.EASY : player.getDifficulty();
        StatusEffectInstance poison = caveSpiderPoison(difficulty);
        if (poison != null) {
            player.getStats().addEffect(poison);
        }
    }

    @Override
    public void onSuccessfulRemoteMeleeHit(World.RemotePlayerTarget target) {
        if (world == null || target == null || !target.valid()) {
            return;
        }
        Player localPlayer = world.getPlayer();
        Difficulty difficulty = localPlayer == null ? Difficulty.EASY : localPlayer.getDifficulty();
        StatusEffectInstance poison = caveSpiderPoison(difficulty);
        if (poison != null) {
            world.applyRemotePlayerStatusEffect(target.playerId(), poison);
        }
    }

    private StatusEffectInstance caveSpiderPoison(Difficulty difficulty) {
        if (difficulty == Difficulty.NORMAL || difficulty == Difficulty.HARD) {
            return new StatusEffectInstance(StatusEffectType.POISON,
                    difficulty == Difficulty.HARD ? 15 * 20 : 7 * 20, 0);
        }
        return null;
    }

}
