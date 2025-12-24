package ru.itis.java.app.entity;

import ru.itis.java.app.network.protocol.GameProtocol;

import java.util.*;

public class LevelSystem {
    private static final Map<Integer, LevelData> levelDataMap = new HashMap<>();
    private static final Random random = new Random();

    static {
        initializeLevelData();
    }

    public static class LevelData {
        private final int level;
        private final int minHealthGain;
        private final int maxHealthGain;
        private final int minDamageGain;
        private final int maxDamageGain;
        private final int experienceRequired;
        private final String title;

        public LevelData(int level, int minHealthGain, int maxHealthGain,
                         int minDamageGain, int maxDamageGain,
                         int experienceRequired, String title) {
            this.level = level;
            this.minHealthGain = minHealthGain;
            this.maxHealthGain = maxHealthGain;
            this.minDamageGain = minDamageGain;
            this.maxDamageGain = maxDamageGain;
            this.experienceRequired = experienceRequired;
            this.title = title;
        }

        public int getLevel() { return level; }
        public int getMinHealthGain() { return minHealthGain; }
        public int getMaxHealthGain() { return maxHealthGain; }
        public int getMinDamageGain() { return minDamageGain; }
        public int getMaxDamageGain() { return maxDamageGain; }
        public int getExperienceRequired() { return experienceRequired; }
        public String getTitle() { return title; }
    }

    private static void initializeLevelData() {
        levelDataMap.put(1, new LevelData(1, 20, 30, 5, 8, 0, "Новичок"));
        levelDataMap.put(2, new LevelData(2, 25, 35, 6, 9, 100, "Ученик"));
        levelDataMap.put(3, new LevelData(3, 30, 40, 7, 10, 200, "Боец"));
        levelDataMap.put(4, new LevelData(4, 35, 45, 8, 12, 350, "Опытный боец"));
        levelDataMap.put(5, new LevelData(5, 40, 50, 9, 14, 550, "Ветеран"));
        levelDataMap.put(6, new LevelData(6, 45, 55, 10, 16, 800, "Мастер"));
        levelDataMap.put(7, new LevelData(7, 50, 60, 11, 18, 1100, "Эксперт"));
        levelDataMap.put(8, new LevelData(8, 55, 65, 12, 20, 1450, "Чемпион"));
        levelDataMap.put(9, new LevelData(9, 60, 70, 13, 22, 1850, "Легенда"));
        levelDataMap.put(10, new LevelData(10, 65, 75, 14, 24, 2300, "Герой"));

        for (int i = 11; i <= 20; i++) {
            int baseXP = 2300 + (i - 10) * 500;
            levelDataMap.put(i, new LevelData(i, 70 + (i-10)*5, 80 + (i-10)*5,
                    15 + (i-10), 25 + (i-10), baseXP, "Воин " + (i-10)));
        }

        for (int i = 21; i <= 50; i++) {
            int tier = (i - 21) / 10 + 1;
            int baseXP = 4800 + (i - 20) * 800;
            levelDataMap.put(i, new LevelData(i, 120 + tier*20, 140 + tier*25,
                    30 + tier*5, 45 + tier*8, baseXP, "Элитный воин " + tier));
        }

        for (int i = 51; i <= 100; i++) {
            int tier = (i - 51) / 10 + 1;
            int baseXP = 28000 + (i - 50) * 1500;
            levelDataMap.put(i, new LevelData(i, 300 + tier*50, 350 + tier*70,
                    70 + tier*15, 100 + tier*25, baseXP, "Легенда " + tier));
        }
    }

    public static LevelData getLevelData(int level) {
        return levelDataMap.getOrDefault(level,
                new LevelData(level, 10, 20, 2, 5, 100, "Неизвестный"));
    }

    public static int getExperienceForNextLevel(int currentLevel) {
        LevelData data = getLevelData(currentLevel + 1);
        return data != null ? data.getExperienceRequired() : 0;
    }

    public static int getExperienceForCurrentLevel(int currentLevel) {
        LevelData data = getLevelData(currentLevel);
        return data != null ? data.getExperienceRequired() : 0;
    }

    public static int getRandomHealthGain(int level) {
        LevelData data = getLevelData(level);
        if (data != null) {
            return random.nextInt(data.getMaxHealthGain() - data.getMinHealthGain() + 1)
                    + data.getMinHealthGain();
        }
        return 10 + random.nextInt(11);
    }

    public static int getRandomDamageGain(int level) {
        LevelData data = getLevelData(level);
        if (data != null) {
            return random.nextInt(data.getMaxDamageGain() - data.getMinDamageGain() + 1)
                    + data.getMinDamageGain();
        }
        return 2 + random.nextInt(4);
    }

    public static void applyLevelUp(PlayerStats stats) {
        int oldLevel = stats.getLevel();
        int newLevel = oldLevel + 1;

        if (newLevel > GameProtocol.MAX_LEVEL) {
            return;
        }

        LevelData levelData = getLevelData(newLevel);
        int healthGain = getRandomHealthGain(newLevel);
        int damageGain = getRandomDamageGain(newLevel);

        stats.setLevel(newLevel);
        stats.setMaxHealth(stats.getMaxHealth() + healthGain);
        stats.setHealth(stats.getMaxHealth());
        stats.setDamage(stats.getDamage() + damageGain);
        stats.setExperienceToNextLevel(levelData.getExperienceRequired());
    }

    public static boolean checkLevelUp(PlayerStats stats) {
        if (stats.getLevel() >= GameProtocol.MAX_LEVEL) {
            return false;
        }
        return stats.getExperience() >= stats.getExperienceToNextLevel();
    }

    public static void addExperienceWithLevelCheck(PlayerStats stats, int experience) {
        stats.addExperience(experience);
        while (checkLevelUp(stats)) {
            applyLevelUp(stats);
            stats.setExperience(stats.getExperience() - stats.getExperienceToNextLevel());
        }
    }

    public static int getExperienceForKill(int killerLevel, int victimLevel) {
        int baseExp = 50;
        int levelDiff = victimLevel - killerLevel;
        double multiplier = 1.0;

        if (levelDiff > 0) {
            multiplier = 1.0 + (levelDiff * 0.2);
        } else if (levelDiff < 0) {
            multiplier = Math.max(0.1, 1.0 + (levelDiff * 0.1));
        }

        int experience = (int)(baseExp * victimLevel * multiplier);
        return Math.max(10, experience);
    }

    public static Map<String, Integer> getLevelMilestoneBonuses(int level) {
        Map<String, Integer> bonuses = new HashMap<>();
        if (level >= 10) bonuses.put("Новый скин", 1);
        if (level >= 20) bonuses.put("Увеличенная скорость", 1);
        if (level >= 30) bonuses.put("Увеличенная дальность атаки", 2);
        if (level >= 40) bonuses.put("Критический удар", 5);
        if (level >= 50) bonuses.put("Регенерация здоровья", 3);
        if (level >= 75) bonuses.put("Невидимость на 3 секунды", 1);
        if (level >= 100) bonuses.put("Бессмертие на 5 секунд", 1);
        return bonuses;
    }

    public static String getProgressInfo(PlayerStats stats) {
        LevelData currentLevelData = getLevelData(stats.getLevel());
        LevelData nextLevelData = getLevelData(stats.getLevel() + 1);

        if (nextLevelData == null) {
            return String.format("Уровень %d (Максимальный) | %s",
                    stats.getLevel(), currentLevelData.getTitle());
        }

        float progress = (float) stats.getExperience() / stats.getExperienceToNextLevel() * 100;
        return String.format("Уровень %d | %s | Прогресс: %.1f%% | До след. уровня: %d XP",
                stats.getLevel(), currentLevelData.getTitle(),
                progress, stats.getExperienceToNextLevel() - stats.getExperience());
    }

    public static String getNextLevelPrediction(PlayerStats stats) {
        int nextLevel = stats.getLevel() + 1;
        if (nextLevel > GameProtocol.MAX_LEVEL) {
            return "Достигнут максимальный уровень!";
        }
        LevelData nextLevelData = getLevelData(nextLevel);
        return String.format("Следующий уровень (%d): HP +%d-%d, Урон +%d-%d",
                nextLevel,
                nextLevelData.getMinHealthGain(), nextLevelData.getMaxHealthGain(),
                nextLevelData.getMinDamageGain(), nextLevelData.getMaxDamageGain());
    }

    public static Map<Integer, LevelData> getAllLevelData() {
        return new HashMap<>(levelDataMap);
    }

    public static void resetToLevelOne(PlayerStats stats) {
        stats.setLevel(1);
        stats.setMaxHealth(80 + random.nextInt(41));
        stats.setHealth(stats.getMaxHealth());
        stats.setDamage(20 + random.nextInt(11));
        stats.setExperience(0);
        stats.setExperienceToNextLevel(getExperienceForNextLevel(1));
    }
}