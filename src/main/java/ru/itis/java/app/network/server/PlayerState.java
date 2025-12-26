package ru.itis.java.app.network.server;

import ru.itis.java.app.entity.PlayerStats;
import ru.itis.java.app.entity.LevelSystem;

public class PlayerState {
    private int id;
    private int x = 100, y = 100;
    private String direction = "down";
    private byte lastSpriteNum = 1;
    private PlayerStats stats;
    private long lastUpdateTime;
    private boolean isDead = false;
    private int deathTimer = 0;
    public static final int DEATH_RESPAWN_TIME = 180;
    private int keys = 0;
    private int swords = 0;
    private int shields = 0;

    public PlayerState(int id) {
        this.id = id;
        this.stats = new PlayerStats();
        this.lastUpdateTime = System.currentTimeMillis();
        this.isDead = false;
        this.deathTimer = 0;
    }

    public void takeDamage(int damage) {
        if (!stats.isAlive() || isDead) return;
        stats.takeDamage(damage);
        if (!stats.isAlive() && !isDead) {
            die();
        }
    }

    public void die() {
        isDead = true;
        deathTimer = DEATH_RESPAWN_TIME;
    }

    public void update() {
        if (isDead && deathTimer > 0) {
            deathTimer--;
            if (deathTimer <= 0) {
                respawn();
            }
        }
    }

    public void respawn() {
        isDead = false;
        stats.setHealth(stats.getMaxHealth());
        int lostExperience = (int)(stats.getExperience() * 0.1);
        stats.setExperience(Math.max(0, stats.getExperience() - lostExperience));
        x = 100 + (id * 50) % 400;
        y = 100 + (id * 30) % 300;
    }

    public boolean isAlive() {
        return stats.isAlive() && !isDead;
    }

    public void addExperienceForKill(int victimLevel) {
        int experienceGained = LevelSystem.getExperienceForKill(stats.getLevel(), victimLevel);
        stats.addExperienceWithLevelCheck(experienceGained);
    }

    public int getId() { return id; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public byte getLastSpriteNum() { return lastSpriteNum; }
    public void setLastSpriteNum(byte lastSpriteNum) { this.lastSpriteNum = lastSpriteNum; }
    public PlayerStats getStats() { return stats; }
    public long getLastUpdateTime() { return lastUpdateTime; }
    public void setLastUpdateTime(long lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
    public boolean isDead() { return isDead; }
    public int getDeathTimer() { return deathTimer; }
    public int getKeys() { return keys; }
    public void setKeys(int keys) { this.keys = keys; }
    public int getSwords() { return swords; }
    public void setSwords(int swords) { this.swords = swords; }
    public int getShields() { return shields; }
    public void setShields(int shields) { this.shields = shields; }
}