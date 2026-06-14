package com.craftzero.main;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerCombatTest {
    @Test
    @DisplayName("Player hurt should apply damage, knockback, and 20-tick immunity")
    void playerHurtAppliesKnockbackAndImmunity() {
        Player player = new Player(0.0f, 64.0f, 0.0f);
        player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);

        boolean firstHit = player.hurt(CombatRules.EASY_ZOMBIE_DAMAGE,
                -1.0f, 64.0f, 0.0f,
                CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK,
                CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK);
        boolean secondHit = player.hurt(CombatRules.EASY_ZOMBIE_DAMAGE,
                -1.0f, 64.0f, 0.0f,
                CombatRules.MOB_MELEE_HORIZONTAL_KNOCKBACK,
                CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK);

        assertTrue(firstHit);
        assertFalse(secondHit);
        assertEquals(18.0f, player.getStats().getHealth(), 0.001f);
        assertTrue(player.getVelocity().x > 0.0f);
        assertEquals(CombatRules.MOB_MELEE_VERTICAL_KNOCKBACK, player.getVelocity().y, 0.001f);
    }

    @Test
    @DisplayName("Stronger damage should replace prior damage during hurt immunity")
    void strongerDamageReplacesPriorDamageDuringImmunity() {
        Player player = new Player(0.0f, 64.0f, 0.0f);
        player.getStats().restore(20.0f, 20.0f, 5.0f, PlayerStats.MAX_AIR_SECONDS);

        boolean firstHit = player.hurt(2.0f, -1.0f, 64.0f, 0.0f, 0.0f, 0.0f);
        boolean strongerHit = player.hurt(6.0f, -1.0f, 64.0f, 0.0f, 0.0f, 0.0f);
        boolean weakerHit = player.hurt(4.0f, -1.0f, 64.0f, 0.0f, 0.0f, 0.0f);

        assertTrue(firstHit);
        assertTrue(strongerHit);
        assertFalse(weakerHit);
        assertEquals(14.0f, player.getStats().getHealth(), 0.001f);
    }
}
