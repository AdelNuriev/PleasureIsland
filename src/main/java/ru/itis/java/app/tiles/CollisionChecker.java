package ru.itis.java.app.tiles;

import ru.itis.java.app.GamePanel;
import ru.itis.java.app.entity.Entity;

public class CollisionChecker {

    private final GamePanel gamePanel;

    public CollisionChecker(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
    }

    public void checkTile(Entity entity) {

        int entityLeftWorldX = (int) (entity.getWorldX() + entity.getSolidArea().getX());
        int entityRightWorldX = (int) (entity.getWorldX() + entity.getSolidArea().getX() + entity.getSolidArea().getWidth());
        int entityTopWorldY = (int) (entity.getWorldY() + entity.getSolidArea().getY());
        int entityBottomWorldY = (int) (entity.getWorldY() + entity.getSolidArea().getY() + entity.getSolidArea().getHeight());

        int entityLeftColumn = entityLeftWorldX / gamePanel.getTileSize();
        int entityRightColumn = entityRightWorldX / gamePanel.getTileSize();
        int entityTopRow = entityTopWorldY / gamePanel.getTileSize();
        int entityBottomRow = entityBottomWorldY / gamePanel.getTileSize();

        int tileNum1, tileNum2;

        switch (entity.getDirection()) {
            case "up" :
                entityTopRow = (entityTopWorldY - entity.getSpeed()) / gamePanel.getTileSize();
                tileNum1 = gamePanel.getTileManager().getMapTileResource()[entityTopRow][entityLeftColumn];
                tileNum2 = gamePanel.getTileManager().getMapTileResource()[entityTopRow][entityRightColumn];
                if (gamePanel.getTileManager().getTile()[tileNum1].isCollision() ||
                        gamePanel.getTileManager().getTile()[tileNum2].isCollision()) {
                    entity.setCollisionOn(true);
                }
                break;
            case "down" :
                entityBottomRow = (entityBottomWorldY + entity.getSpeed()) / gamePanel.getTileSize();
                tileNum1 = gamePanel.getTileManager().getMapTileResource()[entityBottomRow][entityLeftColumn];
                tileNum2 = gamePanel.getTileManager().getMapTileResource()[entityBottomRow][entityRightColumn];
                if (gamePanel.getTileManager().getTile()[tileNum1].isCollision() ||
                        gamePanel.getTileManager().getTile()[tileNum2].isCollision()) {
                    entity.setCollisionOn(true);
                }
                break;
            case "left" :
                entityLeftColumn = (entityLeftWorldX - entity.getSpeed()) / gamePanel.getTileSize();
                tileNum1 = gamePanel.getTileManager().getMapTileResource()[entityTopRow][entityLeftColumn];
                tileNum2 = gamePanel.getTileManager().getMapTileResource()[entityBottomRow][entityLeftColumn];
                if (gamePanel.getTileManager().getTile()[tileNum1].isCollision() ||
                        gamePanel.getTileManager().getTile()[tileNum2].isCollision()) {
                    entity.setCollisionOn(true);
                }
                break;
            case "right" :
                entityRightColumn = (entityRightWorldX + entity.getSpeed()) / gamePanel.getTileSize();
                tileNum1 = gamePanel.getTileManager().getMapTileResource()[entityTopRow][entityRightColumn];
                tileNum2 = gamePanel.getTileManager().getMapTileResource()[entityBottomRow][entityRightColumn];
                if (gamePanel.getTileManager().getTile()[tileNum1].isCollision() ||
                        gamePanel.getTileManager().getTile()[tileNum2].isCollision()) {
                    entity.setCollisionOn(true);
                }
                break;
        }
    }
}