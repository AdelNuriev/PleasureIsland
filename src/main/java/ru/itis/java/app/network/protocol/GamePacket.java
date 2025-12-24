package ru.itis.java.app.network.protocol;

import java.util.ArrayList;
import java.util.List;

public class GamePacket {
    private byte type;
    private byte flags;
    private int playerId;
    private int x;
    private int y;
    private byte direction;
    private int attackerId;
    private int targetId;
    private int attackX;
    private int attackY;
    private int pushX;
    private int pushY;
    private List<PlayerData> playersData;
    private int damage;
    private int health;
    private int maxHealth;
    private boolean isDead;
    private int level;
    private int experience;
    private int experienceToNextLevel;
    private byte spriteNum;

    public static class PlayerData {
        private int id;
        private int x;
        private int y;
        private byte direction;
        private int health;
        private int maxHealth;
        private boolean isDead;
        private int level;
        private int damage;
        private int experience;
        private int experienceToNextLevel;
        private byte spriteNum;

        public PlayerData() {}

        public PlayerData(int id, int x, int y, byte direction,
                          int health, int maxHealth, boolean isDead,
                          int level, int damage, int experience, int experienceToNextLevel, byte spriteNum) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.health = health;
            this.maxHealth = maxHealth;
            this.isDead = isDead;
            this.level = level;
            this.damage = damage;
            this.experience = experience;
            this.experienceToNextLevel = experienceToNextLevel;
            this.spriteNum = spriteNum;
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public int getX() { return x; }
        public void setX(int x) { this.x = x; }
        public int getY() { return y; }
        public void setY(int y) { this.y = y; }
        public byte getDirection() { return direction; }
        public void setDirection(byte direction) { this.direction = direction; }
        public String getDirectionString() {
            return GameProtocol.byteToDirection(direction);
        }
        public int getHealth() { return health; }
        public void setHealth(int health) { this.health = health; }
        public int getMaxHealth() { return maxHealth; }
        public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }
        public boolean isDead() { return isDead; }
        public void setDead(boolean dead) { isDead = dead; }
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public int getDamage() { return damage; }
        public void setDamage(int damage) { this.damage = damage; }
        public int getExperience() { return experience; }
        public void setExperience(int experience) { this.experience = experience; }
        public int getExperienceToNextLevel() { return experienceToNextLevel; }
        public void setExperienceToNextLevel(int exp) { this.experienceToNextLevel = exp; }
        public byte getSpriteNum() { return spriteNum; }
        public void setSpriteNum(byte spriteNum) { this.spriteNum = spriteNum; }
    }

    public GamePacket() {
        this.playersData = new ArrayList<>();
    }

    public GamePacket(byte type) {
        this();
        this.type = type;
    }

    public GamePacket(byte type, int playerId) {
        this(type);
        this.playerId = playerId;
    }

    public byte getType() { return type; }
    public void setType(byte type) { this.type = type; }
    public byte getFlags() { return flags; }
    public void setFlags(byte flags) { this.flags = flags; }
    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    public byte getDirection() { return direction; }
    public void setDirection(byte direction) { this.direction = direction; }
    public void setDirection(String direction) {
        this.direction = GameProtocol.directionToByte(direction);
    }
    public int getAttackerId() { return attackerId; }
    public void setAttackerId(int attackerId) { this.attackerId = attackerId; }
    public int getTargetId() { return targetId; }
    public void setTargetId(int targetId) { this.targetId = targetId; }
    public int getAttackX() { return attackX; }
    public void setAttackX(int attackX) { this.attackX = attackX; }
    public int getAttackY() { return attackY; }
    public void setAttackY(int attackY) { this.attackY = attackY; }
    public int getPushX() { return pushX; }
    public void setPushX(int pushX) { this.pushX = pushX; }
    public int getPushY() { return pushY; }
    public void setPushY(int pushY) { this.pushY = pushY; }
    public List<PlayerData> getPlayersData() { return playersData; }
    public void setPlayersData(List<PlayerData> playersData) {
        this.playersData = playersData;
    }
    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public int getMaxHealth() { return maxHealth; }
    public void setMaxHealth(int maxHealth) { this.maxHealth = maxHealth; }
    public boolean isDead() { return isDead; }
    public void setDead(boolean dead) { isDead = dead; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }
    public int getExperienceToNextLevel() { return experienceToNextLevel; }
    public void setExperienceToNextLevel(int exp) { this.experienceToNextLevel = exp; }
    public byte getSpriteNum() { return spriteNum; }
    public void setSpriteNum(byte spriteNum) { this.spriteNum = spriteNum; }

    public boolean hasFlag(byte flag) {
        return (flags & flag) != 0;
    }

    public void addFlag(byte flag) {
        flags |= flag;
    }

    public void removeFlag(byte flag) {
        flags &= ~flag;
    }

    public void clearFlags() {
        flags = 0;
    }

    public boolean isHandshake() { return type == GameProtocol.TYPE_HANDSHAKE; }
    public boolean isPlayerUpdate() { return type == GameProtocol.TYPE_PLAYER_UPDATE; }
    public boolean isAttack() { return type == GameProtocol.TYPE_ATTACK; }
    public boolean isPlayerHit() { return type == GameProtocol.TYPE_PLAYER_HIT; }
    public boolean isWorldState() { return type == GameProtocol.TYPE_WORLD_STATE; }
    public boolean isPlayerJoin() { return type == GameProtocol.TYPE_PLAYER_JOIN; }
    public boolean isPlayerLeave() { return type == GameProtocol.TYPE_PLAYER_LEAVE; }
    public boolean isPlayerDamage() { return type == GameProtocol.TYPE_PLAYER_DAMAGE; }
    public boolean isPlayerDeath() { return type == GameProtocol.TYPE_PLAYER_DEATH; }
}