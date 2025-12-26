package ru.itis.java.app;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import ru.itis.java.app.entity.Item;
import ru.itis.java.app.entity.NetworkPlayer;
import ru.itis.java.app.network.SocketGameClient;
import ru.itis.java.app.tiles.CollisionChecker;

public class GamePanel extends Pane {

    private final int originalTileSize = 16;
    private final int scale = 3;
    private final int tileSize = originalTileSize * scale;
    private final int maxScreenColumns = 16;
    private final int maxScreenRows = 12;
    private final int screenWidth = maxScreenColumns * tileSize;
    private final int screenHeight = maxScreenRows * tileSize;
    private final int maxMap = 10;

    private GameMap gameMap;
    private final CollisionChecker collisionChecker;
    private AssetSetter assetSetter;
    private final KeyHandler keyHandler = new KeyHandler();
    private final Item[] items = new Item[20];
    private NetworkPlayer player;

    private Sound sound;

    private final int FPS = 60;

    private final int maxWorldColumns = 100;
    private final int maxWorldRows = 75;
    private final int worldWidth = maxWorldColumns * tileSize;
    private final int worldHeight = maxWorldRows * tileSize;

    private final Canvas canvas;
    private final GraphicsContext gc;
    private AnimationTimer gameLoop;
    private SocketGameClient gameClient;

    public GamePanel() {
        this(null);
    }

    public GamePanel(SocketGameClient gameClient) {
        this.gameClient = gameClient;
        gameMap = new GameMap(this);
        collisionChecker = new CollisionChecker(this);
        assetSetter = new AssetSetter(this);
        sound = new Sound();
        canvas = new Canvas(screenWidth, screenHeight);
        gc = canvas.getGraphicsContext2D();
        this.getChildren().add(canvas);
        this.setPrefSize(screenWidth, screenHeight);
        this.setStyle("-fx-background-color: #4CAF50;");
        this.setFocusTraversable(true);
        player = new NetworkPlayer(this, keyHandler, gameClient);
        setupKeyHandlers();

        if (gameClient == null) {
            setupGame();
        }

        initializeGame();
    }
    public void setupNetworkGame(SocketGameClient gameClient) {
        this.gameClient = gameClient;
        clearItems();
        player = new NetworkPlayer(this, keyHandler, gameClient);
        setupKeyHandlers();
        initializeGame();
    }

    private void setupKeyHandlers() {
        this.setOnKeyPressed(e -> {
            keyHandler.keyPressed(e);
            e.consume();
        });
        this.setOnKeyReleased(e -> {
            keyHandler.keyReleased(e);
            e.consume();
        });
    }

    private void setupGame() {
        createInitialItems();
        playMusic(0);
    }

    public void createInitialItems() {
        assetSetter.setItem();
    }

    private void initializeGame() {
        player.draw(gc);
    }

    public void startGameThread() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;
            private final long NANOS_PER_FRAME = 1_000_000_000L / FPS;

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                }
                long elapsedNanos = now - lastUpdate;
                while (elapsedNanos >= NANOS_PER_FRAME) {
                    update();
                    elapsedNanos -= NANOS_PER_FRAME;
                    lastUpdate += NANOS_PER_FRAME;
                }
                draw();
            }
        };
        gameLoop.start();
    }

    public void stopGameThread() {
        if (gameLoop != null) {
            gameLoop.stop();
            gameLoop = null;
        }
        if (gameClient != null && gameClient.isConnected()) {
            gameClient.disconnect();
        }
        System.out.println("Игровой поток остановлен");
    }

    private void update() {
        player.update();
    }

    private void draw() {
        gc.setFill(Color.web("#3B8FCA"));
        gc.fillRect(0, 0, screenWidth, screenHeight);
        gameMap.draw(gc);

        for (Item item : items) {
            if (item != null && !item.isCollected()) {
                item.draw(gc, this);
            }
        }

        player.draw(gc);

        if (gameClient != null && gameClient.isConnected()) {
            if (player.isHandshakeReceived()) {
                if (!player.isDead() && !player.isShowDeathScreen()) {
                    int remotePlayerCount = player.getRemotePlayerCount();
                    gc.setFill(Color.WHITE);
                    gc.fillText("Players online: " + (remotePlayerCount + 1), tileSize * 14, 20);
                    gc.fillText("Player id: " + player.getPlayerId(), tileSize * 14, 40);
                    gc.fillText("HP: " + player.getHealth() + "/" + player.getMaxHealth(), tileSize * 14, 60);
                }
            } else {
                gc.setFill(Color.YELLOW);
                gc.fillText("Connecting to server...", tileSize * 14, 20);
                gc.fillText("Waiting for handshake...", tileSize * 14, 40);
            }
        }
    }

    public void playMusic(int index) {
        sound.setFile(index);
        sound.play();
        sound.loop();
    }

    public void stopMusic() {
        sound.stop();
    }

    public void playSoundEffect(int index) {
        sound.setFile(index);
        sound.play();
    }

    public void checkHandshakeReceived() {
        if (gameClient != null && gameClient.isConnected()) {
            if (player.getPlayerId() == 0 && gameClient.getPlayerId() > 0) {
                player.setPlayerId(gameClient.getPlayerId());
            }
        }
    }

    public void clearItems() {
        for (int i = 0; i < items.length; i++) {
            items[i] = null;
        }
    }

    public int getScreenWidth() { return screenWidth; }
    public int getScreenHeight() { return screenHeight; }
    public int getTileSize() { return tileSize; }
    public int getMaxScreenColumns() { return maxScreenColumns; }
    public int getMaxScreenRows() { return maxScreenRows; }
    public NetworkPlayer getPlayer() { return player; }
    public int getMaxWorldColumns() { return maxWorldColumns; }
    public int getMaxWorldRows() { return maxWorldRows; }
    public int getWorldWidth() { return worldWidth; }
    public int getWorldHeight() { return worldHeight; }
    public GameMap getGameMap() { return gameMap; }
    public CollisionChecker getCollisionChecker() { return collisionChecker; }
    public Item[] getItems() { return items; }
    public Sound getSound() { return sound; }
    public int getMaxMap() { return maxMap; }
    public GraphicsContext getGc() { return gc; }
    public AssetSetter getAssetSetter() { return assetSetter; }
}