package com.craftzero.main;

import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Persistent player counters for the Release-era Statistics screen.
 */
public final class PlayerStatistics {
    private long playTimeTicks;
    private long timesPlayed;
    private long gamesQuit;
    private long worldsLoaded;
    private long multiplayerJoins;
    private long worldsSaved;
    private long distanceWalkedCm;
    private long distanceSwumCm;
    private long distanceFallenCm;
    private long distanceClimbedCm;
    private long distanceFlownCm;
    private long distanceDoveCm;
    private long distanceByMinecartCm;
    private long distanceByBoatCm;
    private long distanceByPigCm;
    private long jumps;
    private long blocksMined;
    private long successfulAttacks;
    private long damageDealtTenths;
    private long damageTakenTenths;
    private long deaths;
    private long mobKills;
    private long monsterKills;
    private long playerKills;
    private long fishCaught;
    private long itemsPickedUp;
    private long itemsDropped;
    private long itemsCrafted;
    private long itemsUsed;
    private long itemsDepleted;
    private double playTickAccumulator;
    private final EnumMap<BlockType, Long> blocksMinedByType = new EnumMap<>(BlockType.class);
    private final EnumMap<ItemType, Long> itemsPickedUpByType = new EnumMap<>(ItemType.class);
    private final EnumMap<ItemType, Long> itemsDroppedByType = new EnumMap<>(ItemType.class);
    private final EnumMap<ItemType, Long> itemsCraftedByType = new EnumMap<>(ItemType.class);
    private final EnumMap<ItemType, Long> itemsUsedByType = new EnumMap<>(ItemType.class);
    private final EnumMap<ItemType, Long> itemsDepletedByType = new EnumMap<>(ItemType.class);

    public void recordPlayTime(float deltaTime) {
        if (deltaTime <= 0.0f) {
            return;
        }
        playTickAccumulator += deltaTime * 20.0;
        long ticks = (long) playTickAccumulator;
        if (ticks <= 0) {
            return;
        }
        playTimeTicks = saturatedAdd(playTimeTicks, ticks);
        playTickAccumulator -= ticks;
    }

    public void recordGameQuit() {
        gamesQuit = saturatedAdd(gamesQuit, 1);
    }

    public void recordWorldLoaded() {
        timesPlayed = saturatedAdd(timesPlayed, 1);
        worldsLoaded = saturatedAdd(worldsLoaded, 1);
    }

    public void recordMultiplayerJoin() {
        multiplayerJoins = saturatedAdd(multiplayerJoins, 1);
    }

    public void recordWorldSaved() {
        worldsSaved = saturatedAdd(worldsSaved, 1);
    }

    public void recordHorizontalDistance(float distanceBlocks) {
        recordDistanceWalked(distanceBlocks);
    }

    public void recordDistanceWalked(float distanceBlocks) {
        if (distanceBlocks <= 0.0f) {
            return;
        }
        distanceWalkedCm = saturatedAdd(distanceWalkedCm, centimeters(distanceBlocks));
    }

    public void recordDistanceSwum(float distanceBlocks) {
        distanceSwumCm = saturatedAdd(distanceSwumCm, centimeters(distanceBlocks));
    }

    public void recordDistanceFallen(float distanceBlocks) {
        distanceFallenCm = saturatedAdd(distanceFallenCm, centimeters(distanceBlocks));
    }

    public void recordDistanceClimbed(float distanceBlocks) {
        distanceClimbedCm = saturatedAdd(distanceClimbedCm, centimeters(distanceBlocks));
    }

    public void recordDistanceFlown(float distanceBlocks) {
        distanceFlownCm = saturatedAdd(distanceFlownCm, centimeters(distanceBlocks));
    }

    public void recordDistanceDove(float distanceBlocks) {
        distanceDoveCm = saturatedAdd(distanceDoveCm, centimeters(distanceBlocks));
    }

    public void recordDistanceByMinecart(float distanceBlocks) {
        distanceByMinecartCm = saturatedAdd(distanceByMinecartCm, centimeters(distanceBlocks));
    }

    public void recordDistanceByBoat(float distanceBlocks) {
        distanceByBoatCm = saturatedAdd(distanceByBoatCm, centimeters(distanceBlocks));
    }

    public void recordDistanceByPig(float distanceBlocks) {
        distanceByPigCm = saturatedAdd(distanceByPigCm, centimeters(distanceBlocks));
    }

    public void recordJump() {
        jumps = saturatedAdd(jumps, 1);
    }

    public void recordBlockMined() {
        blocksMined = saturatedAdd(blocksMined, 1);
    }

    public void recordBlockMined(BlockType type) {
        recordBlockMined();
        if (type != null && type != BlockType.AIR) {
            increment(blocksMinedByType, type, 1);
        }
    }

    public void recordSuccessfulAttack(float damage) {
        successfulAttacks = saturatedAdd(successfulAttacks, 1);
        recordDamageDealt(damage);
    }

    public void recordDamageDealt(float damage) {
        long tenths = damageTenths(damage);
        if (tenths > 0) {
            damageDealtTenths = saturatedAdd(damageDealtTenths, tenths);
        }
    }

    public void recordDamageTaken(float damage) {
        long tenths = damageTenths(damage);
        if (tenths > 0) {
            damageTakenTenths = saturatedAdd(damageTakenTenths, tenths);
        }
    }

    public void recordDeath() {
        deaths = saturatedAdd(deaths, 1);
    }

    public void recordMobKill(boolean hostile) {
        mobKills = saturatedAdd(mobKills, 1);
        if (hostile) {
            monsterKills = saturatedAdd(monsterKills, 1);
        }
    }

    public void recordPlayerKill() {
        playerKills = saturatedAdd(playerKills, 1);
    }

    public void recordFishCaught() {
        fishCaught = saturatedAdd(fishCaught, 1);
    }

    public void recordItemPickup(int count) {
        if (count > 0) {
            itemsPickedUp = saturatedAdd(itemsPickedUp, count);
        }
    }

    public void recordItemPickup(ItemType type, int count) {
        recordItemPickup(count);
        if (type != null && count > 0) {
            increment(itemsPickedUpByType, type, count);
        }
    }

    public void recordItemDropped(int count) {
        if (count > 0) {
            itemsDropped = saturatedAdd(itemsDropped, count);
        }
    }

    public void recordItemDropped(ItemType type, int count) {
        recordItemDropped(count);
        if (type != null && count > 0) {
            increment(itemsDroppedByType, type, count);
        }
    }

    public void recordItemCrafted(int count) {
        if (count > 0) {
            itemsCrafted = saturatedAdd(itemsCrafted, count);
        }
    }

    public void recordItemCrafted(ItemType type, int count) {
        recordItemCrafted(count);
        if (type != null && count > 0) {
            increment(itemsCraftedByType, type, count);
        }
    }

    public void recordItemUsed(ItemType type) {
        if (type == null) {
            return;
        }
        itemsUsed = saturatedAdd(itemsUsed, 1);
        increment(itemsUsedByType, type, 1);
    }

    public void recordItemDepleted(ItemType type) {
        if (type == null) {
            return;
        }
        itemsDepleted = saturatedAdd(itemsDepleted, 1);
        increment(itemsDepletedByType, type, 1);
    }

    public void restore(long playTimeTicks, long distanceWalkedCm, long jumps, long blocksMined,
            long successfulAttacks, long damageDealtTenths, long damageTakenTenths, long deaths,
            long itemsPickedUp, long itemsCrafted) {
        restore(playTimeTicks, distanceWalkedCm, jumps, blocksMined, successfulAttacks, damageDealtTenths,
                damageTakenTenths, deaths, 0, 0, itemsPickedUp, itemsCrafted);
    }

    public void restore(long playTimeTicks, long distanceWalkedCm, long jumps, long blocksMined,
            long successfulAttacks, long damageDealtTenths, long damageTakenTenths, long deaths,
            long mobKills, long monsterKills, long itemsPickedUp, long itemsCrafted) {
        this.playTimeTicks = clampNonNegative(playTimeTicks);
        this.timesPlayed = 0;
        this.gamesQuit = 0;
        this.worldsLoaded = 0;
        this.multiplayerJoins = 0;
        this.worldsSaved = 0;
        this.distanceWalkedCm = clampNonNegative(distanceWalkedCm);
        this.distanceSwumCm = 0;
        this.distanceFallenCm = 0;
        this.distanceClimbedCm = 0;
        this.distanceFlownCm = 0;
        this.distanceDoveCm = 0;
        this.distanceByMinecartCm = 0;
        this.distanceByBoatCm = 0;
        this.distanceByPigCm = 0;
        this.jumps = clampNonNegative(jumps);
        this.blocksMined = clampNonNegative(blocksMined);
        this.successfulAttacks = clampNonNegative(successfulAttacks);
        this.damageDealtTenths = clampNonNegative(damageDealtTenths);
        this.damageTakenTenths = clampNonNegative(damageTakenTenths);
        this.deaths = clampNonNegative(deaths);
        this.mobKills = clampNonNegative(mobKills);
        this.monsterKills = clampNonNegative(monsterKills);
        this.playerKills = 0;
        this.fishCaught = 0;
        this.itemsPickedUp = clampNonNegative(itemsPickedUp);
        this.itemsDropped = 0;
        this.itemsCrafted = clampNonNegative(itemsCrafted);
        this.itemsUsed = 0;
        this.itemsDepleted = 0;
        this.playTickAccumulator = 0.0;
        this.blocksMinedByType.clear();
        this.itemsPickedUpByType.clear();
        this.itemsDroppedByType.clear();
        this.itemsCraftedByType.clear();
        this.itemsUsedByType.clear();
        this.itemsDepletedByType.clear();
    }

    public void restoreTravelDistances(long distanceSwumCm, long distanceFallenCm, long distanceClimbedCm,
            long distanceFlownCm, long distanceDoveCm, long distanceByMinecartCm, long distanceByBoatCm,
            long distanceByPigCm) {
        this.distanceSwumCm = clampNonNegative(distanceSwumCm);
        this.distanceFallenCm = clampNonNegative(distanceFallenCm);
        this.distanceClimbedCm = clampNonNegative(distanceClimbedCm);
        this.distanceFlownCm = clampNonNegative(distanceFlownCm);
        this.distanceDoveCm = clampNonNegative(distanceDoveCm);
        this.distanceByMinecartCm = clampNonNegative(distanceByMinecartCm);
        this.distanceByBoatCm = clampNonNegative(distanceByBoatCm);
        this.distanceByPigCm = clampNonNegative(distanceByPigCm);
    }

    public void restoreFishCaught(long fishCaught) {
        this.fishCaught = clampNonNegative(fishCaught);
    }

    public void restorePlayerKills(long playerKills) {
        this.playerKills = clampNonNegative(playerKills);
    }

    public void restoreItemsDropped(long itemsDropped) {
        this.itemsDropped = clampNonNegative(itemsDropped);
    }

    public void restoreGamesQuit(long gamesQuit) {
        this.gamesQuit = clampNonNegative(gamesQuit);
    }

    public void restoreSessionCounters(long worldsLoaded, long multiplayerJoins, long worldsSaved) {
        restoreSessionCounters(0, worldsLoaded, multiplayerJoins, worldsSaved);
    }

    public void restoreSessionCounters(long timesPlayed, long worldsLoaded, long multiplayerJoins, long worldsSaved) {
        this.timesPlayed = clampNonNegative(timesPlayed);
        this.worldsLoaded = clampNonNegative(worldsLoaded);
        this.multiplayerJoins = clampNonNegative(multiplayerJoins);
        this.worldsSaved = clampNonNegative(worldsSaved);
    }

    public void restoreItemsDroppedByType(Map<ItemType, Long> itemsDroppedByType) {
        restoreCounts(this.itemsDroppedByType, itemsDroppedByType);
    }

    public void restore(long playTimeTicks, long distanceWalkedCm, long jumps, long blocksMined,
            long successfulAttacks, long damageDealtTenths, long damageTakenTenths, long deaths,
            long mobKills, long monsterKills, long itemsPickedUp, long itemsCrafted,
            long itemsUsed, long itemsDepleted) {
        restore(playTimeTicks, distanceWalkedCm, jumps, blocksMined, successfulAttacks, damageDealtTenths,
                damageTakenTenths, deaths, mobKills, monsterKills, itemsPickedUp, itemsCrafted);
        this.itemsUsed = clampNonNegative(itemsUsed);
        this.itemsDepleted = clampNonNegative(itemsDepleted);
    }

    public void restore(long playTimeTicks, long distanceWalkedCm, long jumps, long blocksMined,
            long successfulAttacks, long damageDealtTenths, long damageTakenTenths, long deaths,
            long itemsPickedUp, long itemsCrafted, Map<BlockType, Long> blocksMinedByType,
            Map<ItemType, Long> itemsPickedUpByType, Map<ItemType, Long> itemsCraftedByType) {
        restore(playTimeTicks, distanceWalkedCm, jumps, blocksMined, successfulAttacks, damageDealtTenths,
                damageTakenTenths, deaths, 0, 0, itemsPickedUp, itemsCrafted,
                blocksMinedByType, itemsPickedUpByType, itemsCraftedByType);
    }

    public void restore(long playTimeTicks, long distanceWalkedCm, long jumps, long blocksMined,
            long successfulAttacks, long damageDealtTenths, long damageTakenTenths, long deaths,
            long mobKills, long monsterKills, long itemsPickedUp, long itemsCrafted,
            Map<BlockType, Long> blocksMinedByType, Map<ItemType, Long> itemsPickedUpByType,
            Map<ItemType, Long> itemsCraftedByType) {
        restore(playTimeTicks, distanceWalkedCm, jumps, blocksMined, successfulAttacks, damageDealtTenths,
                damageTakenTenths, deaths, mobKills, monsterKills, itemsPickedUp, itemsCrafted);
        restoreCounts(this.blocksMinedByType, blocksMinedByType);
        restoreCounts(this.itemsPickedUpByType, itemsPickedUpByType);
        restoreCounts(this.itemsCraftedByType, itemsCraftedByType);
    }

    public void restore(long playTimeTicks, long distanceWalkedCm, long jumps, long blocksMined,
            long successfulAttacks, long damageDealtTenths, long damageTakenTenths, long deaths,
            long mobKills, long monsterKills, long itemsPickedUp, long itemsCrafted,
            long itemsUsed, long itemsDepleted,
            Map<BlockType, Long> blocksMinedByType, Map<ItemType, Long> itemsPickedUpByType,
            Map<ItemType, Long> itemsCraftedByType, Map<ItemType, Long> itemsUsedByType,
            Map<ItemType, Long> itemsDepletedByType) {
        restore(playTimeTicks, distanceWalkedCm, jumps, blocksMined, successfulAttacks, damageDealtTenths,
                damageTakenTenths, deaths, mobKills, monsterKills, itemsPickedUp, itemsCrafted,
                itemsUsed, itemsDepleted);
        restoreCounts(this.blocksMinedByType, blocksMinedByType);
        restoreCounts(this.itemsPickedUpByType, itemsPickedUpByType);
        restoreCounts(this.itemsCraftedByType, itemsCraftedByType);
        restoreCounts(this.itemsUsedByType, itemsUsedByType);
        restoreCounts(this.itemsDepletedByType, itemsDepletedByType);
    }

    public long getPlayTimeTicks() {
        return playTimeTicks;
    }

    public long getGamesQuit() {
        return gamesQuit;
    }

    public long getTimesPlayed() {
        return timesPlayed;
    }

    public long getWorldsLoaded() {
        return worldsLoaded;
    }

    public long getMultiplayerJoins() {
        return multiplayerJoins;
    }

    public long getWorldsSaved() {
        return worldsSaved;
    }

    public long getDistanceWalkedCm() {
        return distanceWalkedCm;
    }

    public long getDistanceSwumCm() {
        return distanceSwumCm;
    }

    public long getDistanceFallenCm() {
        return distanceFallenCm;
    }

    public long getDistanceClimbedCm() {
        return distanceClimbedCm;
    }

    public long getDistanceFlownCm() {
        return distanceFlownCm;
    }

    public long getDistanceDoveCm() {
        return distanceDoveCm;
    }

    public long getDistanceByMinecartCm() {
        return distanceByMinecartCm;
    }

    public long getDistanceByBoatCm() {
        return distanceByBoatCm;
    }

    public long getDistanceByPigCm() {
        return distanceByPigCm;
    }

    public long getJumps() {
        return jumps;
    }

    public long getBlocksMined() {
        return blocksMined;
    }

    public long getSuccessfulAttacks() {
        return successfulAttacks;
    }

    public long getDamageDealtTenths() {
        return damageDealtTenths;
    }

    public long getDamageTakenTenths() {
        return damageTakenTenths;
    }

    public long getDeaths() {
        return deaths;
    }

    public long getMobKills() {
        return mobKills;
    }

    public long getMonsterKills() {
        return monsterKills;
    }

    public long getPlayerKills() {
        return playerKills;
    }

    public long getFishCaught() {
        return fishCaught;
    }

    public long getItemsPickedUp() {
        return itemsPickedUp;
    }

    public long getItemsDropped() {
        return itemsDropped;
    }

    public long getItemsCrafted() {
        return itemsCrafted;
    }

    public long getItemsUsed() {
        return itemsUsed;
    }

    public long getItemsDepleted() {
        return itemsDepleted;
    }

    public long getBlocksMined(BlockType type) {
        return blocksMinedByType.getOrDefault(type, 0L);
    }

    public long getItemsPickedUp(ItemType type) {
        return itemsPickedUpByType.getOrDefault(type, 0L);
    }

    public long getItemsDropped(ItemType type) {
        return itemsDroppedByType.getOrDefault(type, 0L);
    }

    public long getItemsCrafted(ItemType type) {
        return itemsCraftedByType.getOrDefault(type, 0L);
    }

    public long getItemsUsed(ItemType type) {
        return itemsUsedByType.getOrDefault(type, 0L);
    }

    public long getItemsDepleted(ItemType type) {
        return itemsDepletedByType.getOrDefault(type, 0L);
    }

    public Map<BlockType, Long> getBlocksMinedByType() {
        return Map.copyOf(blocksMinedByType);
    }

    public Map<ItemType, Long> getItemsPickedUpByType() {
        return Map.copyOf(itemsPickedUpByType);
    }

    public Map<ItemType, Long> getItemsDroppedByType() {
        return Map.copyOf(itemsDroppedByType);
    }

    public Map<ItemType, Long> getItemsCraftedByType() {
        return Map.copyOf(itemsCraftedByType);
    }

    public Map<ItemType, Long> getItemsUsedByType() {
        return Map.copyOf(itemsUsedByType);
    }

    public Map<ItemType, Long> getItemsDepletedByType() {
        return Map.copyOf(itemsDepletedByType);
    }

    public List<BlockStatistic> blockStatistics() {
        List<BlockStatistic> rows = new ArrayList<>();
        for (Map.Entry<BlockType, Long> entry : blocksMinedByType.entrySet()) {
            long count = clampNonNegative(entry.getValue());
            if (count > 0) {
                rows.add(new BlockStatistic(entry.getKey(), count));
            }
        }
        rows.sort(Comparator.comparingInt(row -> row.type().getId()));
        return List.copyOf(rows);
    }

    public List<ItemStatistic> itemStatistics() {
        EnumSet<ItemType> types = EnumSet.noneOf(ItemType.class);
        types.addAll(itemsPickedUpByType.keySet());
        types.addAll(itemsDroppedByType.keySet());
        types.addAll(itemsCraftedByType.keySet());
        types.addAll(itemsUsedByType.keySet());
        types.addAll(itemsDepletedByType.keySet());

        List<ItemStatistic> rows = new ArrayList<>();
        for (ItemType type : types) {
            long pickedUp = clampNonNegative(itemsPickedUpByType.getOrDefault(type, 0L));
            long dropped = clampNonNegative(itemsDroppedByType.getOrDefault(type, 0L));
            long crafted = clampNonNegative(itemsCraftedByType.getOrDefault(type, 0L));
            long used = clampNonNegative(itemsUsedByType.getOrDefault(type, 0L));
            long depleted = clampNonNegative(itemsDepletedByType.getOrDefault(type, 0L));
            if (pickedUp > 0 || dropped > 0 || crafted > 0 || used > 0 || depleted > 0) {
                rows.add(new ItemStatistic(type, pickedUp, dropped, crafted, used, depleted));
            }
        }
        rows.sort(Comparator
                .comparingInt((ItemStatistic row) -> row.type().getId())
                .thenComparingInt(row -> row.type().getDataValue()));
        return List.copyOf(rows);
    }

    private static long damageTenths(float damage) {
        if (damage <= 0.0f || !Float.isFinite(damage)) {
            return 0;
        }
        return Math.max(0, Math.round(damage * 10.0f));
    }

    private static long centimeters(float distanceBlocks) {
        if (distanceBlocks <= 0.0f || !Float.isFinite(distanceBlocks)) {
            return 0;
        }
        return Math.max(0, Math.round(distanceBlocks * 100.0f));
    }

    private static <E extends Enum<E>> void increment(EnumMap<E, Long> counts, E type, long amount) {
        if (type == null || amount <= 0) {
            return;
        }
        counts.put(type, saturatedAdd(counts.getOrDefault(type, 0L), amount));
    }

    private static <E extends Enum<E>> void restoreCounts(EnumMap<E, Long> target, Map<E, Long> source) {
        target.clear();
        if (source == null) {
            return;
        }
        for (Map.Entry<E, Long> entry : source.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static long saturatedAdd(long value, long amount) {
        if (amount <= 0) {
            return value;
        }
        long result = value + amount;
        return result < value ? Long.MAX_VALUE : result;
    }

    private static long clampNonNegative(long value) {
        return Math.max(0, value);
    }

    public record BlockStatistic(BlockType type, long mined) {
    }

    public record ItemStatistic(ItemType type, long pickedUp, long dropped, long crafted, long used, long depleted) {
    }
}
