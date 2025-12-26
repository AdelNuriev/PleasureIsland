package ru.itis.java.app;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import ru.itis.java.app.entity.Item;
import ru.itis.java.app.entity.NetworkPlayer;
import ru.itis.java.app.tiles.TileManager;

public class GameMap extends TileManager {

    private final int miniMapSize = 200;
    private final int miniMapBorder = 5;
    private final int miniMapPositionX = 550;
    private final int miniMapPositionY = 360;
    private float miniMapScale;

    public GameMap(GamePanel gamePanel) {
        super(gamePanel);
        calculateMiniMapScale();
    }

    @Override
    public void draw(GraphicsContext gc) {
        super.draw(gc);
    }

    private void calculateMiniMapScale() {
        float worldWidth = gamePanel.getWorldWidth();
        float worldHeight = gamePanel.getWorldHeight();
        float worldMax = Math.max(worldWidth, worldHeight);
        miniMapScale = (float) miniMapSize / worldMax;
    }

    public void drawMiniMap(GraphicsContext gc) {
        if (gamePanel.getPlayer() == null) return;

        gc.setFill(new Color(0, 0, 0, 0.7));
        gc.fillRect(miniMapPositionX - miniMapBorder, miniMapPositionY - miniMapBorder,
                miniMapSize + miniMapBorder * 2, miniMapSize + miniMapBorder * 2);

        gc.setFill(Color.web("#3B8FCA"));
        gc.fillRect(miniMapPositionX, miniMapPositionY, miniMapSize, miniMapSize);

        NetworkPlayer player = gamePanel.getPlayer();
        int playerWorldX = player.getWorldX();
        int playerWorldY = player.getWorldY();

        int centerX = miniMapPositionX + miniMapSize / 2;
        int centerY = miniMapPositionY + miniMapSize / 2;


        for (int row = 0; row < gamePanel.getMaxWorldRows(); row++) {
            for (int col = 0; col < gamePanel.getMaxWorldColumns(); col++) {
                int tileNum = mapTileResource[row][col];
                if (tileNum >= 0 && tileNum < tile.length && tile[tileNum] != null) {

                    int worldTileX = col * gamePanel.getTileSize();
                    int worldTileY = row * gamePanel.getTileSize();

                    int mapX = centerX + (int)((worldTileX - playerWorldX) * miniMapScale);
                    int mapY = centerY + (int)((worldTileY - playerWorldY) * miniMapScale);

                    int tileSizeOnMiniMap = Math.max(2, (int)(gamePanel.getTileSize() * miniMapScale));

                    if (mapX >= miniMapPositionX - tileSizeOnMiniMap &&
                            mapX < miniMapPositionX + miniMapSize &&
                            mapY >= miniMapPositionY - tileSizeOnMiniMap &&
                            mapY < miniMapPositionY + miniMapSize) {

                        drawSimplifiedTile(gc, mapX, mapY, tileSizeOnMiniMap, tileNum);
                    }
                }
            }
        }

        gc.setFill(Color.RED);
        int playerSize = Math.max(4, (int)(gamePanel.getTileSize() * miniMapScale / 2));
        gc.fillOval(centerX - playerSize/2, centerY - playerSize/2, playerSize, playerSize);

        drawItemsOnMiniMap(gc, centerX, centerY, playerWorldX, playerWorldY);

        drawCompass(gc, 735, 385);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeRect(miniMapPositionX, miniMapPositionY, miniMapSize, miniMapSize);
    }

    private void drawSimplifiedTile(GraphicsContext gc, int x, int y, int size, int tileNum) {
        Color color = getTileColor(tileNum);
        gc.setFill(color);

        if (size < 2) {
            gc.fillRect(x, y, 2, 2);
        } else if (size < 4) {
            gc.fillRect(x, y, 4, 4);
        } else {
            gc.fillRect(x, y, size, size);
        }
    }

    private Color getTileColor(int tileNum) {
        if (tileNum >= 19 && tileNum <= 25) { // Вода
            return Color.web("#3B8FCA");
        } else if (tileNum >= 26 && tileNum <= 35) { // Песок
            return Color.web("#CA9E51");
        } else if (tileNum == 36) { // Дерево
            return Color.web("#2CA660");
        } else if (tileNum == 37) { // Дом
            return Color.web("#83582F");
        } else if (tileNum >= 0 && tileNum <= 18) { // Трава
            return Color.web("#67A55E");
        } else if (tileNum == 40) { // Камень
            return Color.web("#565656");
        } else if (tileNum == 41) { // Стена
            return Color.web("#939393");
        } else if (tileNum >= 44 && tileNum <= 45) { // Земля
            return Color.web("#755236");
        } else {
            return Color.web("#67A55E"); // По умолчанию - трава
        }
    }

    private void drawItemsOnMiniMap(GraphicsContext gc, int centerX, int centerY, int playerWorldX, int playerWorldY) {
        Item[] items = gamePanel.getItems();
        if (items == null) return;

        for (Item item : items) {
            if (item != null && !item.isCollected()) {
                int itemX = item.getWorldX();
                int itemY = item.getWorldY();

                int mapX = centerX + (int)((itemX - playerWorldX) * miniMapScale);
                int mapY = centerY + (int)((itemY - playerWorldY) * miniMapScale);

                if (mapX >= miniMapPositionX && mapX < miniMapPositionX + miniMapSize &&
                        mapY >= miniMapPositionY && mapY < miniMapPositionY + miniMapSize) {

                    String itemName = item.getName();
                    Color itemColor = Color.WHITE;

                    if (itemName != null) {
                        switch (itemName) {
                            case "Sword":
                                itemColor = Color.SILVER;
                                break;
                            case "Key":
                                itemColor = Color.GOLD;
                                break;
                            case "Door":
                                itemColor = Color.DARKRED;
                                break;
                        }
                    }

                    gc.setFill(itemColor);
                    int itemSize = Math.max(2, (int)(gamePanel.getTileSize() * miniMapScale / 4));
                    gc.fillOval(mapX - itemSize/2, mapY - itemSize/2, itemSize, itemSize);
                }
            }
        }
    }

    private void drawCompass(GraphicsContext gc, int x, int y) {
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);

        gc.strokeOval(x - 5, y - 5, 10, 10);

        gc.strokeLine(x, y - 5, x, y + 5);
        gc.strokeLine(x, y - 5, x - 2, y - 2);
        gc.strokeLine(x, y - 5, x + 2, y - 2);

        gc.setFill(Color.WHITE);
        gc.fillText("N", x - 4, y - 8);
    }
}