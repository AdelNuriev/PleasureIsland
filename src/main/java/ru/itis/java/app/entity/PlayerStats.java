package ru.itis.java.app.entity;

import java.util.Random;

public class PlayerStats {
    private int health;
    private int maxHealth;
    private int damage;
    private int level;
    private int experience;
    private int experienceToNextLevel;

    private static final Random random = new Random();

    public PlayerStats() {
        this.level = 1;
        this.maxHealth = 80 + random.nextInt(41); // 80-120
        this.health = this.maxHealth;
        this.damage = 20 + random.nextInt(11); // 20-30
        this.experience = 0;
        this.experienceToNextLevel = LevelSystem.getExperienceForNextLevel(1);
    }

    public PlayerStats(int level, int health, int maxHealth, int damage, int experience) {
        this.level = level;
        this.health = health;
        this.maxHealth = maxHealth;
        this.damage = damage;
        this.experience = experience;
        this.experienceToNextLevel = LevelSystem.getExperienceForNextLevel(level);
    }

    public void addExperience(int amount) {
        this.experience += amount;
        while (LevelSystem.checkLevelUp(this)) {
            LevelSystem.applyLevelUp(this);
            this.experience -= this.experienceToNextLevel;
        }
    }

    public void addExperienceWithLevelCheck(int amount) {
        LevelSystem.addExperienceWithLevelCheck(this, amount);
    }

    public void takeDamage(int damage) {
        this.health -= damage;
        if (this.health < 0) {
            this.health = 0;
        }
    }

    public void heal(int amount) {
        this.health += amount;
        if (this.health > this.maxHealth) {
            this.health = this.maxHealth;
        }
    }

    public boolean isAlive() {
        return this.health > 0;
    }

    public float getHealthPercentage() {
        return (float) this.health / this.maxHealth;
    }

    public String getProgressInfo() {
        return LevelSystem.getProgressInfo(this);
    }

    public String getNextLevelPrediction() {
        return LevelSystem.getNextLevelPrediction(this);
    }

    public java.util.Map<String, Integer> getMilestoneBonuses() {
        return LevelSystem.getLevelMilestoneBonuses(this.level);
    }

    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }

    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }

    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }

    public int getLevel() { return level; }
    public void setLevel(int level) {
        this.level = level;
        this.experienceToNextLevel = LevelSystem.getExperienceForNextLevel(level);
    }

    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }

    public int getExperienceToNextLevel() { return experienceToNextLevel; }
    public void setExperienceToNextLevel(int experienceToNextLevel) {
        this.experienceToNextLevel = experienceToNextLevel;
    }

    @Override
    public String toString() {
        return String.format("Lvl %d: HP %d/%d, Dmg %d, XP %d/%d",
                level, health, maxHealth, damage, experience, experienceToNextLevel);
    }

    public String toDetailedString() {
        LevelSystem.LevelData levelData = LevelSystem.getLevelData(level);
        return String.format("Lvl %d (%s): HP %d/%d, Dmg %d, XP %d/%d (%.1f%%)",
                level, levelData.getTitle(), health, maxHealth, damage,
                experience, experienceToNextLevel,
                ((float)experience / experienceToNextLevel * 100));
    }

    public PlayerStats copy() {
        return new PlayerStats(level, health, maxHealth, damage, experience);
    }
}