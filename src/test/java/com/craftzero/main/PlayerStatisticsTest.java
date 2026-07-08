package com.craftzero.main;

import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerStatisticsTest {

    @Test
    @DisplayName("Typed statistics should track mined blocks and per-item counts")
    void typedStatisticsTrackBlockAndItemCounts() {
        PlayerStatistics statistics = new PlayerStatistics();

        statistics.recordBlockMined(BlockType.STONE);
        statistics.recordBlockMined(BlockType.STONE);
        statistics.recordBlockMined(BlockType.OAK_LOG);
        statistics.recordItemPickup(ItemType.OAK_LOG, 3);
        statistics.recordItemDropped(ItemType.DIAMOND, 2);
        statistics.recordItemCrafted(ItemType.CRAFTING_TABLE, 1);
        statistics.recordItemUsed(ItemType.FISHING_ROD);
        statistics.recordItemUsed(ItemType.FISHING_ROD);
        statistics.recordItemDepleted(ItemType.FISHING_ROD);

        assertEquals(3, statistics.getBlocksMined());
        assertEquals(2, statistics.getBlocksMined(BlockType.STONE));
        assertEquals(1, statistics.getBlocksMined(BlockType.OAK_LOG));
        assertEquals(3, statistics.getItemsPickedUp());
        assertEquals(3, statistics.getItemsPickedUp(ItemType.OAK_LOG));
        assertEquals(2, statistics.getItemsDropped());
        assertEquals(2, statistics.getItemsDropped(ItemType.DIAMOND));
        assertEquals(1, statistics.getItemsCrafted());
        assertEquals(1, statistics.getItemsCrafted(ItemType.CRAFTING_TABLE));
        assertEquals(2, statistics.getItemsUsed());
        assertEquals(2, statistics.getItemsUsed(ItemType.FISHING_ROD));
        assertEquals(1, statistics.getItemsDepleted());
        assertEquals(1, statistics.getItemsDepleted(ItemType.FISHING_ROD));

        List<PlayerStatistics.BlockStatistic> blockRows = statistics.blockStatistics();
        assertEquals(BlockType.STONE, blockRows.get(0).type());
        assertEquals(2, blockRows.get(0).mined());
        assertEquals(BlockType.OAK_LOG, blockRows.get(1).type());

        List<PlayerStatistics.ItemStatistic> itemRows = statistics.itemStatistics();
        PlayerStatistics.ItemStatistic oakLog = itemRow(itemRows, ItemType.OAK_LOG);
        assertEquals(3, oakLog.pickedUp());
        PlayerStatistics.ItemStatistic diamond = itemRow(itemRows, ItemType.DIAMOND);
        assertEquals(2, diamond.dropped());
        PlayerStatistics.ItemStatistic craftingTable = itemRow(itemRows, ItemType.CRAFTING_TABLE);
        assertEquals(1, craftingTable.crafted());
        PlayerStatistics.ItemStatistic fishingRod = itemRow(itemRows, ItemType.FISHING_ROD);
        assertEquals(2, fishingRod.used());
        assertEquals(1, fishingRod.depleted());
    }

    @Test
    @DisplayName("Combat statistics should track mob, monster, and player kills")
    void combatStatisticsTrackMobMonsterAndPlayerKills() {
        PlayerStatistics statistics = new PlayerStatistics();

        statistics.recordMobKill(false);
        statistics.recordMobKill(true);
        statistics.recordPlayerKill();
        statistics.recordPlayerKill();

        assertEquals(2, statistics.getMobKills());
        assertEquals(1, statistics.getMonsterKills());
        assertEquals(2, statistics.getPlayerKills());

        statistics.restorePlayerKills(5);

        assertEquals(5, statistics.getPlayerKills());
    }

    @Test
    @DisplayName("Fishing statistics should count successful fish catches")
    void fishingStatisticsTrackSuccessfulCatches() {
        PlayerStatistics statistics = new PlayerStatistics();

        statistics.recordFishCaught();
        statistics.recordFishCaught();

        assertEquals(2, statistics.getFishCaught());

        statistics.restoreFishCaught(7);

        assertEquals(7, statistics.getFishCaught());
    }

    @Test
    @DisplayName("Lifecycle statistics should count saved world quits")
    void lifecycleStatisticsTrackSavedWorldQuits() {
        PlayerStatistics statistics = new PlayerStatistics();

        statistics.recordGameQuit();
        statistics.recordGameQuit();

        assertEquals(2, statistics.getGamesQuit());

        statistics.restoreGamesQuit(5);

        assertEquals(5, statistics.getGamesQuit());
    }

    @Test
    @DisplayName("Dropped item statistics should count successful player drops")
    void droppedItemStatisticsTrackDrops() {
        PlayerStatistics statistics = new PlayerStatistics();

        statistics.recordItemDropped(2);
        statistics.recordItemDropped(ItemType.DIAMOND, 3);
        statistics.recordItemDropped(0);
        statistics.recordItemDropped(-1);

        assertEquals(5, statistics.getItemsDropped());
        assertEquals(3, statistics.getItemsDropped(ItemType.DIAMOND));

        statistics.restoreItemsDropped(7);
        statistics.restoreItemsDroppedByType(Map.of(ItemType.OAK_LOG, 4L));

        assertEquals(7, statistics.getItemsDropped());
        assertEquals(4, statistics.getItemsDropped(ItemType.OAK_LOG));
    }

    @Test
    @DisplayName("Release travel statistics should track movement modes separately")
    void releaseTravelStatisticsTrackMovementModesSeparately() {
        PlayerStatistics statistics = new PlayerStatistics();

        statistics.recordDistanceWalked(1.25f);
        statistics.recordDistanceSwum(2.0f);
        statistics.recordDistanceFallen(3.5f);
        statistics.recordDistanceClimbed(0.75f);
        statistics.recordDistanceFlown(4.0f);
        statistics.recordDistanceDove(1.5f);
        statistics.recordDistanceByMinecart(8.25f);
        statistics.recordDistanceByBoat(6.0f);
        statistics.recordDistanceByPig(2.25f);

        assertEquals(125, statistics.getDistanceWalkedCm());
        assertEquals(200, statistics.getDistanceSwumCm());
        assertEquals(350, statistics.getDistanceFallenCm());
        assertEquals(75, statistics.getDistanceClimbedCm());
        assertEquals(400, statistics.getDistanceFlownCm());
        assertEquals(150, statistics.getDistanceDoveCm());
        assertEquals(825, statistics.getDistanceByMinecartCm());
        assertEquals(600, statistics.getDistanceByBoatCm());
        assertEquals(225, statistics.getDistanceByPigCm());
    }

    @Test
    @DisplayName("Release travel statistics should restore persisted distances")
    void releaseTravelStatisticsRestorePersistedDistances() {
        PlayerStatistics statistics = new PlayerStatistics();

        statistics.recordDistanceSwum(12.0f);
        statistics.restore(20, 300, 1, 10, 0, 0, 0, 0, 6, 4, 4, 2, 3, 1);
        statistics.restoreTravelDistances(10, 20, 30, 40, 50, 60, 70, 80);

        assertEquals(300, statistics.getDistanceWalkedCm());
        assertEquals(10, statistics.getDistanceSwumCm());
        assertEquals(20, statistics.getDistanceFallenCm());
        assertEquals(30, statistics.getDistanceClimbedCm());
        assertEquals(40, statistics.getDistanceFlownCm());
        assertEquals(50, statistics.getDistanceDoveCm());
        assertEquals(60, statistics.getDistanceByMinecartCm());
        assertEquals(70, statistics.getDistanceByBoatCm());
        assertEquals(80, statistics.getDistanceByPigCm());
    }

    @Test
    @DisplayName("Typed statistics should restore from persisted enum maps")
    void typedStatisticsRestorePersistedMaps() {
        PlayerStatistics statistics = new PlayerStatistics();

        statistics.restore(20, 300, 1, 10, 0, 0, 0, 0, 6, 4, 4, 2, 3, 1,
                Map.of(BlockType.STONE, 7L, BlockType.OAK_LOG, 3L),
                Map.of(ItemType.OAK_LOG, 4L),
                Map.of(ItemType.CRAFTING_TABLE, 2L),
                Map.of(ItemType.FISHING_ROD, 3L),
                Map.of(ItemType.FISHING_ROD, 1L));
        statistics.restoreItemsDropped(5);
        statistics.restoreItemsDroppedByType(Map.of(ItemType.DIAMOND, 5L));

        assertEquals(10, statistics.getBlocksMined());
        assertEquals(6, statistics.getMobKills());
        assertEquals(4, statistics.getMonsterKills());
        assertEquals(5, statistics.getItemsDropped());
        assertEquals(3, statistics.getItemsUsed());
        assertEquals(1, statistics.getItemsDepleted());
        assertEquals(7, statistics.getBlocksMined(BlockType.STONE));
        assertEquals(3, statistics.getBlocksMined(BlockType.OAK_LOG));
        assertEquals(4, statistics.getItemsPickedUp(ItemType.OAK_LOG));
        assertEquals(5, statistics.getItemsDropped(ItemType.DIAMOND));
        assertEquals(2, statistics.getItemsCrafted(ItemType.CRAFTING_TABLE));
        assertEquals(3, statistics.getItemsUsed(ItemType.FISHING_ROD));
        assertEquals(1, statistics.getItemsDepleted(ItemType.FISHING_ROD));
    }

    private static PlayerStatistics.ItemStatistic itemRow(List<PlayerStatistics.ItemStatistic> rows, ItemType type) {
        return rows.stream()
                .filter(row -> row.type() == type)
                .findFirst()
                .orElseThrow();
    }
}
