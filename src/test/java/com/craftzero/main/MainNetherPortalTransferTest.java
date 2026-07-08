package com.craftzero.main;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainNetherPortalTransferTest {
    @Test
    @DisplayName("Nether portal transfer delay should match Release 1.0 game-mode rules")
    void netherPortalTransferDelayUsesGameMode() {
        assertEquals(4.0f, Main.netherPortalTransferTimeFor(GameMode.SURVIVAL), 0.0001f);
        assertEquals(4.0f, Main.netherPortalTransferTimeFor(GameMode.HARDCORE), 0.0001f);
        assertEquals(4.0f, Main.netherPortalTransferTimeFor(null), 0.0001f);
        assertEquals(0.0f, Main.netherPortalTransferTimeFor(GameMode.CREATIVE), 0.0001f);
    }
}
