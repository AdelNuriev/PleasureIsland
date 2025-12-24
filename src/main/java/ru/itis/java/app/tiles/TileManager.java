package ru.itis.java.app.tiles;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import ru.itis.java.app.GamePanel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TileManager {

    private static final String FILE_PATH = "/environment/map/map.txt";

    private final GamePanel gamePanel;
    private final Tile[] tile;
    private final int[][] mapTileResource;

    public TileManager(GamePanel gamePanel) {
        this.gamePanel = gamePanel;
        tile = new Tile[50];
        mapTileResource = new int[gamePanel.getMaxWorldRows()][gamePanel.getMaxWorldColumns()];
        getTileImages();
        loadMap();
    }

    private void loadMap() {

        try {
            InputStream imageStream = getClass().getResourceAsStream(FILE_PATH);
            BufferedReader reader = new BufferedReader(new InputStreamReader(imageStream));

            int col = 0;
            int row = 0;

            while (col < gamePanel.getMaxWorldColumns() && row < gamePanel.getMaxWorldRows()) {
                String line = reader.readLine();
                while (col < gamePanel.getMaxWorldColumns()) {
                    String[] tokens = line.split(" ");
                    int type = Integer.parseInt(tokens[col]);
                    mapTileResource[row][col] = type;
                    col++;
                }
                if (col == gamePanel.getMaxWorldColumns()) {
                    col = 0;
                    row++;
                }
            }
            reader.close();
        } catch (Exception e) {
            System.err.println("Ошибка загрузки отображения карты: " + e.getMessage());
        }
    }

    private void getTileImages() {
        try {
            tile[0] = new Tile();
            tile[0].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-common.png")));

            tile[1] = new Tile();
            tile[1].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass.png")));

            tile[2] = new Tile();
            tile[2].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-rare.png")));

            tile[3] = new Tile();
            tile[3].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-sand-top-left.png")));

            tile[4] = new Tile();
            tile[4].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-sand-top-right.png")));

            tile[5] = new Tile();
            tile[5].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-sand-bot-left.png")));

            tile[6] = new Tile();
            tile[6].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-sand-bot-right.png")));

            tile[7] = new Tile();
            tile[7].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-sand-top-left.png")));

            tile[8] = new Tile();
            tile[8].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-sand-top-right.png")));

            tile[9] = new Tile();
            tile[9].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-sand-bot-left.png")));

            tile[10] = new Tile();
            tile[10].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-sand-bot-right.png")));

            tile[11] = new Tile();
            tile[11].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-water-top-left.png")));

            tile[12] = new Tile();
            tile[12].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-water-top.png")));

            tile[13] = new Tile();
            tile[13].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-water-top-right.png")));

            tile[14] = new Tile();
            tile[14].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-water-left.png")));

            tile[15] = new Tile();
            tile[15].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-water-right.png")));

            tile[16] = new Tile();
            tile[16].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-water-bot-left.png")));

            tile[17] = new Tile();
            tile[17].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-water-bot.png")));

            tile[18] = new Tile();
            tile[18].setImage(new Image(getClass().getResourceAsStream("/environment/images/grass-water-bot-right.png")));

            tile[19] = new Tile();
            tile[19].setImage(new Image(getClass().getResourceAsStream("/environment/images/water.png")));
            tile[19].setCollision(true);

            tile[20] = new Tile();
            tile[20].setImage(new Image(getClass().getResourceAsStream("/environment/images/water-common.png")));
            tile[20].setCollision(true);

            tile[21] = new Tile();
            tile[21].setImage(new Image(getClass().getResourceAsStream("/environment/images/water-shining.png")));
            tile[21].setCollision(true);

            tile[22] = new Tile();
            tile[22].setImage(new Image(getClass().getResourceAsStream("/environment/images/water-grass-top-left.png")));
            tile[22].setCollision(true);

            tile[23] = new Tile();
            tile[23].setImage(new Image(getClass().getResourceAsStream("/environment/images/water-grass-top-right.png")));
            tile[23].setCollision(true);

            tile[24] = new Tile();
            tile[24].setImage(new Image(getClass().getResourceAsStream("/environment/images/water-grass-bot-left.png")));
            tile[24].setCollision(true);

            tile[25] = new Tile();
            tile[25].setImage(new Image(getClass().getResourceAsStream("/environment/images/water-grass-bot-right.png")));
            tile[25].setCollision(true);

            tile[26] = new Tile();
            tile[26].setImage(new Image(getClass().getResourceAsStream("/environment/images/sand.png")));

            tile[27] = new Tile();
            tile[27].setImage(new Image(getClass().getResourceAsStream("/environment/images/sand-common.png")));

            tile[28] = new Tile();
            tile[28].setImage(new Image(getClass().getResourceAsStream("/environment/images/sand-grass-top-left.png")));

            tile[29] = new Tile();
            tile[29].setImage(new Image(getClass().getResourceAsStream("/environment/images/sand-grass-top.png")));

            tile[30] = new Tile();
            tile[30].setImage(new Image(getClass().getResourceAsStream("/environment/images/sand-grass-top-right.png")));

            tile[31] = new Tile();
            tile[31].setImage(new Image(getClass().getResourceAsStream("/environment/images/sand-grass-left.png")));

            tile[32] = new Tile();
            tile[32].setImage(new Image(getClass().getResourceAsStream("/environment/images/sand-grass-right.png")));

            tile[33] = new Tile();
            tile[33].setImage(new Image(getClass().getResourceAsStream("/environment/images/sand-grass-bot-left.png")));

            tile[34] = new Tile();
            tile[34].setImage(new Image(getClass().getResourceAsStream("/environment/images/sand-grass-bot.png")));

            tile[35] = new Tile();
            tile[35].setImage(new Image(getClass().getResourceAsStream("/environment/images/sand-grass-bot-right.png")));

            tile[36] = new Tile();
            tile[36].setImage(new Image(getClass().getResourceAsStream("/environment/images/tree.png")));
            tile[36].setCollision(true);

            tile[37] = new Tile();
            tile[37].setImage(new Image(getClass().getResourceAsStream("/environment/images/house-common.png")));
            tile[37].setCollision(true);

            tile[38] = new Tile();
            tile[38].setImage(new Image(getClass().getResourceAsStream("/environment/images/ladder-up.png")));

            tile[39] = new Tile();
            tile[39].setImage(new Image(getClass().getResourceAsStream("/environment/images/ladder-down.png")));

            tile[40] = new Tile();
            tile[40].setImage(new Image(getClass().getResourceAsStream("/environment/images/stone.png")));

            tile[41] = new Tile();
            tile[41].setImage(new Image(getClass().getResourceAsStream("/environment/images/wall-common.png")));

            tile[42] = new Tile();
            tile[42].setImage(new Image(getClass().getResourceAsStream("/environment/images/table-wood.png")));

            tile[43] = new Tile();
            tile[43].setImage(new Image(getClass().getResourceAsStream("/environment/images/floor-wood.png")));

            tile[44] = new Tile();
            tile[44].setImage(new Image(getClass().getResourceAsStream("/environment/images/dirt.png")));

            tile[45] = new Tile();
            tile[45].setImage(new Image(getClass().getResourceAsStream("/environment/images/dirt-common.png")));
        } catch (Exception e) {
            System.err.println("Ошибка загрузки изображений окружения: " + e.getMessage());
        }
    }

    public void draw(GraphicsContext gc) {

        int worldCol = 0;
        int worldRow = 0;

        while (worldCol < gamePanel.getMaxWorldColumns() && worldRow < gamePanel.getMaxWorldRows()) {
            int tileType = mapTileResource[worldRow][worldCol];

            int worldX = worldCol * gamePanel.getTileSize();
            int worldY = worldRow * gamePanel.getTileSize();
            int screenX = worldX - gamePanel.getPlayer().getWorldX() + gamePanel.getPlayer().getScreenX();
            int screenY = worldY - gamePanel.getPlayer().getWorldY() + gamePanel.getPlayer().getScreenY();

            if (worldX + gamePanel.getTileSize() > gamePanel.getPlayer().getWorldX() - gamePanel.getPlayer().getScreenX() &&
                worldX - gamePanel.getTileSize() < gamePanel.getPlayer().getWorldX() + gamePanel.getPlayer().getScreenX() &&
                worldY + gamePanel.getTileSize() > gamePanel.getPlayer().getWorldY() - gamePanel.getPlayer().getScreenY() &&
                worldY - gamePanel.getTileSize() < gamePanel.getPlayer().getWorldY() + gamePanel.getPlayer().getScreenY())
            {
                gc.drawImage(tile[tileType].getImage(), screenX, screenY, gamePanel.getTileSize(), gamePanel.getTileSize());
            }

            worldCol++;


            if (worldCol == gamePanel.getMaxWorldColumns()) {
                worldCol = 0;
                worldRow++;
            }
        }
    }

    public Tile[] getTile() { return tile; }
    public int[][] getMapTileResource() { return mapTileResource; }
}
