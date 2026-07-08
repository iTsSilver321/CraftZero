package com.craftzero.progression;

import com.craftzero.inventory.ItemType;
import com.craftzero.world.BlockType;
import com.craftzero.world.Dimension;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Tracks Release-era achievements and the short HUD notifications they create.
 */
public class AchievementTracker {
    public static final float TOAST_FADE_IN_SECONDS = 0.25f;
    public static final float TOAST_STAY_SECONDS = 2.75f;
    public static final float TOAST_FADE_OUT_SECONDS = 0.75f;
    public static final float TOAST_TOTAL_SECONDS =
            TOAST_FADE_IN_SECONDS + TOAST_STAY_SECONDS + TOAST_FADE_OUT_SECONDS;

    private final EnumSet<AchievementType> unlocked = EnumSet.noneOf(AchievementType.class);
    private final ArrayDeque<AchievementType> pendingNotifications = new ArrayDeque<>();
    private AchievementType activeNotification;
    private float activeNotificationAge;

    public boolean unlock(AchievementType type) {
        if (type == null || unlocked.contains(type)) {
            return false;
        }
        AchievementType parent = type.parent();
        if (parent != null && !unlocked.contains(parent)) {
            return false;
        }
        unlocked.add(type);
        pendingNotifications.addLast(type);
        return true;
    }

    public boolean recordInventoryOpened() {
        return unlock(AchievementType.OPEN_INVENTORY);
    }

    public boolean recordBlockBroken(BlockType block) {
        return unlock(AchievementType.forBrokenBlock(block));
    }

    public boolean recordCollectedItem(ItemType item) {
        return unlock(AchievementType.forCollectedItem(item));
    }

    public boolean recordCrafted(ItemType item) {
        return unlock(AchievementType.forCraftedItem(item));
    }

    public boolean recordDimensionTravel(Dimension from, Dimension to) {
        return unlock(AchievementType.forDimensionTravel(from, to));
    }

    public boolean recordReturnedFireballKill() {
        return unlock(AchievementType.RETURN_TO_SENDER);
    }

    public boolean recordBrewedPotionTaken() {
        return unlock(AchievementType.LOCAL_BREWERY);
    }

    public boolean recordMinecartRideDistance(float distanceBlocks) {
        return distanceBlocks >= 1000.0f && unlock(AchievementType.ON_A_RAIL);
    }

    public boolean recordMonsterKilled() {
        return unlock(AchievementType.KILL_ENEMY);
    }

    public boolean recordPigFlew() {
        return unlock(AchievementType.FLY_PIG);
    }

    public boolean recordSkeletonSniped(float distanceBlocks) {
        return distanceBlocks >= 50.0f && unlock(AchievementType.SNIPE_SKELETON);
    }

    public boolean recordOverkillHit(float damage) {
        return damage >= 18.0f && unlock(AchievementType.OVERKILL);
    }

    public boolean isUnlocked(AchievementType type) {
        return type != null && unlocked.contains(type);
    }

    public Set<AchievementType> unlockedAchievements() {
        return Collections.unmodifiableSet(unlocked);
    }

    public List<String> unlockedIds() {
        List<String> ids = new ArrayList<>();
        for (AchievementType type : AchievementType.values()) {
            if (unlocked.contains(type)) {
                ids.add(type.id());
            }
        }
        return ids;
    }

    public void restoreUnlocked(Collection<String> ids) {
        unlocked.clear();
        pendingNotifications.clear();
        activeNotification = null;
        activeNotificationAge = 0.0f;
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            AchievementType type = AchievementType.fromId(id);
            if (type != null) {
                unlocked.add(type);
            }
        }
    }

    public int queuedNotificationCount() {
        return pendingNotifications.size() + (activeNotification == null ? 0 : 1);
    }

    public AchievementType activeNotification() {
        return activeNotification;
    }

    public float activeNotificationAge() {
        return activeNotificationAge;
    }

    public void updateNotifications(float deltaTime) {
        float safeDelta = Math.max(0.0f, deltaTime);
        if (activeNotification == null) {
            promoteNextNotification();
        }
        if (activeNotification == null) {
            return;
        }

        activeNotificationAge += safeDelta;
        if (activeNotificationAge >= TOAST_TOTAL_SECONDS) {
            promoteNextNotification();
        }
    }

    private void promoteNextNotification() {
        activeNotification = pendingNotifications.pollFirst();
        activeNotificationAge = 0.0f;
    }

    public static float notificationAlpha(float ageSeconds) {
        float age = Math.max(0.0f, ageSeconds);
        if (age < TOAST_FADE_IN_SECONDS) {
            return age / TOAST_FADE_IN_SECONDS;
        }
        if (age <= TOAST_FADE_IN_SECONDS + TOAST_STAY_SECONDS) {
            return 1.0f;
        }
        float fadeOutAge = age - TOAST_FADE_IN_SECONDS - TOAST_STAY_SECONDS;
        return Math.max(0.0f, 1.0f - fadeOutAge / TOAST_FADE_OUT_SECONDS);
    }
}
