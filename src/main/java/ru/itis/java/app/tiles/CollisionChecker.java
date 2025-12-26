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
                tileNum1 = gamePanel.getGameMap().getMapTileResource()[entityTopRow][entityLeftColumn];
                tileNum2 = gamePanel.getGameMap().getMapTileResource()[entityTopRow][entityRightColumn];
                if (gamePanel.getGameMap().getTile()[tileNum1].isCollision() ||
                        gamePanel.getGameMap().getTile()[tileNum2].isCollision()) {
                    entity.setCollisionOn(true);
                }
                break;
            case "down" :
                entityBottomRow = (entityBottomWorldY + entity.getSpeed()) / gamePanel.getTileSize();
                tileNum1 = gamePanel.getGameMap().getMapTileResource()[entityBottomRow][entityLeftColumn];
                tileNum2 = gamePanel.getGameMap().getMapTileResource()[entityBottomRow][entityRightColumn];
                if (gamePanel.getGameMap().getTile()[tileNum1].isCollision() ||
                        gamePanel.getGameMap().getTile()[tileNum2].isCollision()) {
                    entity.setCollisionOn(true);
                }
                break;
            case "left" :
                entityLeftColumn = (entityLeftWorldX - entity.getSpeed()) / gamePanel.getTileSize();
                tileNum1 = gamePanel.getGameMap().getMapTileResource()[entityTopRow][entityLeftColumn];
                tileNum2 = gamePanel.getGameMap().getMapTileResource()[entityBottomRow][entityLeftColumn];
                if (gamePanel.getGameMap().getTile()[tileNum1].isCollision() ||
                        gamePanel.getGameMap().getTile()[tileNum2].isCollision()) {
                    entity.setCollisionOn(true);
                }
                break;
            case "right" :
                entityRightColumn = (entityRightWorldX + entity.getSpeed()) / gamePanel.getTileSize();
                tileNum1 = gamePanel.getGameMap().getMapTileResource()[entityTopRow][entityRightColumn];
                tileNum2 = gamePanel.getGameMap().getMapTileResource()[entityBottomRow][entityRightColumn];
                if (gamePanel.getGameMap().getTile()[tileNum1].isCollision() ||
                        gamePanel.getGameMap().getTile()[tileNum2].isCollision()) {
                    entity.setCollisionOn(true);
                }
                break;
        }
    }

    public int checkObject(Entity entity, boolean player) {
        int index = 999;
        for (int i = 0; i < gamePanel.getItems().length; i++) {
            if (gamePanel.getItems()[i] != null) {
                entity.getSolidArea().setX(entity.getWorldX() + entity.getSolidArea().getX());
                entity.getSolidArea().setY(entity.getWorldY() + entity.getSolidArea().getY());

                gamePanel.getItems()[i].getSolidArea().setX(gamePanel.getItems()[i].getWorldX() + gamePanel.getItems()[i].getSolidArea().getX());
                gamePanel.getItems()[i].getSolidArea().setY(gamePanel.getItems()[i].getWorldY() + gamePanel.getItems()[i].getSolidArea().getY());

                switch(entity.getDirection()) {
                    case "up" :
                        entity.getSolidArea().setY(entity.getSolidArea().getY() - entity.getSpeed());
                        if (entity.getSolidArea().getBoundsInParent().intersects(gamePanel.getItems()[i].getSolidArea().getBoundsInParent())) {
                            if (gamePanel.getItems()[i].isCollision()) { entity.setCollisionOn(true); }
                            if (player) { index = i; }
                        }
                        break;
                    case "down" :
                        entity.getSolidArea().setY(entity.getSolidArea().getY() + entity.getSpeed());
                        if (entity.getSolidArea().getBoundsInParent().intersects(gamePanel.getItems()[i].getSolidArea().getBoundsInParent())) {
                            if (gamePanel.getItems()[i].isCollision()) { entity.setCollisionOn(true); }
                            if (player) { index = i; }
                        }
                        break;
                    case "left" :
                        entity.getSolidArea().setX(entity.getSolidArea().getX() - entity.getSpeed());
                        if (entity.getSolidArea().getBoundsInParent().intersects(gamePanel.getItems()[i].getSolidArea().getBoundsInParent())) {
                            if (gamePanel.getItems()[i].isCollision()) { entity.setCollisionOn(true); }
                            if (player) { index = i; }
                        }
                        break;
                    case "right" :
                        entity.getSolidArea().setX(entity.getSolidArea().getX() + entity.getSpeed());
                        if (entity.getSolidArea().getBoundsInParent().intersects(gamePanel.getItems()[i].getSolidArea().getBoundsInParent())) {
                            if (gamePanel.getItems()[i].isCollision()) { entity.setCollisionOn(true); }
                            if (player) { index = i; }
                        }
                        break;
                }
                entity.getSolidArea().setX(entity.getSolidAreaDefaultX());
                entity.getSolidArea().setY(entity.getSolidAreaDefaultY());
                gamePanel.getItems()[i].getSolidArea().setX(gamePanel.getItems()[i].getSolidAreaDefaultX());
                gamePanel.getItems()[i].getSolidArea().setY(gamePanel.getItems()[i].getSolidAreaDefaultY());

            }
        }

        return index;
    }
}