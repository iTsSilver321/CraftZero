package com.craftzero.progression;

/**
 * Release 1.0 experience model. Old enchanting consumes whole levels and level
 * 50 requires 4625 total XP.
 */
public class PlayerProgression {
    private int totalExperience;
    private int score;

    public int getTotalExperience() {
        return totalExperience;
    }

    public int getScore() {
        return score;
    }

    public int getLevel() {
        int level = 0;
        while (experienceForLevel(level + 1) <= totalExperience) {
            level++;
        }
        return level;
    }

    public int getExperienceIntoLevel() {
        return totalExperience - experienceForLevel(getLevel());
    }

    public int getExperienceToNextLevel() {
        return experienceForLevel(getLevel() + 1) - experienceForLevel(getLevel());
    }

    public void addExperience(int amount) {
        if (amount <= 0) {
            return;
        }
        totalExperience += amount;
        score += amount;
    }

    public boolean consumeLevels(int levels) {
        if (levels <= 0) {
            return true;
        }
        int current = getLevel();
        if (current < levels) {
            return false;
        }
        totalExperience = experienceForLevel(current - levels);
        return true;
    }

    public void restore(int totalExperience, int score) {
        this.totalExperience = Math.max(0, totalExperience);
        this.score = Math.max(0, score);
    }

    public void clearExperience() {
        this.totalExperience = 0;
    }

    public int deathDropExperience() {
        return Math.min(100, getLevel() * 7);
    }

    public static int experienceForLevel(int level) {
        if (level <= 0) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < level; i++) {
            total += 7 + (i * 7) / 2;
        }
        return total;
    }
}
