package ru.itis.java.app;

import ru.itis.java.app.entity.Shield;
import ru.itis.java.app.entity.Sword;
import ru.itis.java.app.entity.Key;
import ru.itis.java.app.entity.Door;

public class AssetSetter {
    private GamePanel gamePanel;

    public AssetSetter(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    public void setItem() {
        gamePanel.getItems()[0] = new Sword();
        gamePanel.getItems()[0].setWorldX(24 * gamePanel.getTileSize());
        gamePanel.getItems()[0].setWorldY(8 * gamePanel.getTileSize());
        gamePanel.getItems()[0].setItemId(1);
        gamePanel.getItems()[0].setExperienceReward(100);

        gamePanel.getItems()[1] = new Sword();
        gamePanel.getItems()[1].setWorldX(24 * gamePanel.getTileSize());
        gamePanel.getItems()[1].setWorldY(10 * gamePanel.getTileSize());
        gamePanel.getItems()[1].setItemId(2);
        gamePanel.getItems()[1].setExperienceReward(100);

        gamePanel.getItems()[2] = new Key();
        gamePanel.getItems()[2].setWorldX(20 * gamePanel.getTileSize());
        gamePanel.getItems()[2].setWorldY(15 * gamePanel.getTileSize());
        gamePanel.getItems()[2].setItemId(3);
        gamePanel.getItems()[2].setExperienceReward(25);

        gamePanel.getItems()[3] = new Door();
        gamePanel.getItems()[3].setWorldX(25 * gamePanel.getTileSize());
        gamePanel.getItems()[3].setWorldY(15 * gamePanel.getTileSize());
        gamePanel.getItems()[3].setItemId(4);
        gamePanel.getItems()[3].setExperienceReward(50);

        gamePanel.getItems()[4] = new Shield();
        gamePanel.getItems()[4].setWorldX(30 * gamePanel.getTileSize());
        gamePanel.getItems()[4].setWorldY(10 * gamePanel.getTileSize());
        gamePanel.getItems()[4].setItemId(5);
        gamePanel.getItems()[4].setExperienceReward(100);
    }
}