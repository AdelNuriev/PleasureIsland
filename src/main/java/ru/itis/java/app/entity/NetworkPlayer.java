package ru.itis.java.app.entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.shape.Rectangle;
import ru.itis.java.app.GamePanel;
import ru.itis.java.app.KeyHandler;
import ru.itis.java.app.network.SocketGameClient;
import ru.itis.java.app.network.protocol.*;
import java.util.*;

public class NetworkPlayer extends Entity {
    private GamePanel gamePanel;
    private KeyHandler keyHandler;
    private SocketGameClient gameClient;
    private final int screenX;
    private final int screenY;
    private int playerId;
    private int attackCounter = 0;
    private boolean attackAnimationPlaying = false;
    private boolean beingPushed = false;
    private int pushTargetX, pushTargetY;
    private final int pushSpeed = 12;
    private Map<Integer, RemotePlayerEntity> remotePlayers = new HashMap<>();
    private int lastSentX = -1, lastSentY = -1;
    private String lastSentDirection = "";
    private int lastSentSpriteNum = 1;
    private long lastSendTime = 0;
    private static final long SEND_INTERVAL = 33;
    private PlayerStats stats;
    private boolean isDead = false;
    private int deathTimer = 0;
    private final int DEATH_DURATION = 60;
    private boolean gameOver = false;
    private boolean handshakeReceived = false;
    private boolean showDeathScreen = false;
    private int deathAnimationFrame = 0;
    private int deathAnimationTimer = 0;
    private static final int DEATH_ANIMATION_FRAME_DURATION = 10;
    private static final int DEATH_ANIMATION_TOTAL_FRAMES = 5;

    public NetworkPlayer(GamePanel gamePanel, KeyHandler keyHandler) {
        this(gamePanel, keyHandler, null);
    }

    public NetworkPlayer(GamePanel gamePanel, KeyHandler keyHandler, SocketGameClient gameClient) {
        this.gamePanel = gamePanel;
        this.keyHandler = keyHandler;
        this.gameClient = gameClient;
        solidArea = new Rectangle();
        solidArea.setX(8);
        solidArea.setY(0);
        solidArea.setWidth(gamePanel.getTileSize() - 16);
        solidArea.setHeight(gamePanel.getTileSize() - 8);
        setDefaultValues();
        this.screenX = gamePanel.getScreenWidth() / 2 - (gamePanel.getTileSize() / 2);
        this.screenY = gamePanel.getScreenHeight() / 2 - (gamePanel.getTileSize() / 2);
        direction = "down";
        loadPlayerImages();
        this.playerId = 0;
        if (gameClient != null) {
            setupNetworkCallbacks();
        }
    }

    private void setDefaultValues() {
        worldX = gamePanel.getTileSize() * 20;
        worldY = gamePanel.getTileSize() * 7;
        speed = 4;
        attackDuration = 15;
        stats = new PlayerStats();
        isDead = false;
        showDeathScreen = false;
        deathAnimationFrame = 0;
        deathAnimationTimer = 0;
    }

    private void setupNetworkCallbacks() {
        gameClient.setPacketListener(new SocketGameClient.PacketListener() {
            @Override
            public void onHandshake(GamePacket packet) {
                int serverPlayerId = packet.getPlayerId();
                playerId = serverPlayerId;
                handshakeReceived = true;
                try {
                    java.lang.reflect.Field field = SocketGameClient.class.getDeclaredField("playerId");
                    field.setAccessible(true);
                    field.set(gameClient, serverPlayerId);
                } catch (Exception e) {
                }
                if (packet.hasFlag(GameProtocol.FLAG_HEALTH_EXTENDED)) {
                    stats = new PlayerStats(
                            packet.getLevel(),
                            packet.getHealth(),
                            packet.getMaxHealth(),
                            packet.getDamage(),
                            0
                    );
                }
            }

            @Override
            public void onPlayerUpdate(GamePacket packet) {
                if (!handshakeReceived) return;
                int otherPlayerId = packet.getPlayerId();
                if (otherPlayerId == playerId) {
                    return;
                }
                RemotePlayerEntity rpe = remotePlayers.get(otherPlayerId);
                if (rpe == null) {
                    rpe = new RemotePlayerEntity(otherPlayerId, gamePanel);
                    remotePlayers.put(otherPlayerId, rpe);
                }
                if (packet.hasFlag(GameProtocol.FLAG_POSITION)) {
                    rpe.setTargetPosition(packet.getX(), packet.getY());
                    rpe.setHasTarget(true);
                }
                if (packet.hasFlag(GameProtocol.FLAG_DIRECTION)) {
                    rpe.setDirection(GameProtocol.byteToDirection(packet.getDirection()));
                }
                if (packet.hasFlag(GameProtocol.FLAG_SPRITE_NUM)) {
                    rpe.setSpriteNum(packet.getSpriteNum());
                }
            }

            @Override
            public void onAttack(GamePacket packet) {
                if (!handshakeReceived) return;
                int attackerId = packet.getPlayerId();

                if (attackerId == playerId) {
                    if (!isDead && !attackAnimationPlaying) {
                        attackAnimationPlaying = true;
                        attackCounter = 0;
                        spriteNum = 1;
                        spriteCounter = 0;
                    }
                } else {
                    RemotePlayerEntity rpe = remotePlayers.get(attackerId);
                    if (rpe != null) {
                        rpe.playAttackAnimation();
                    }
                }
            }

            @Override
            public void onPlayerHit(GamePacket packet) {
                if (!handshakeReceived) return;
                if (packet.getTargetId() == playerId) {
                    onHit(packet.getPushX(), packet.getPushY());
                }
            }

            @Override
            public void onWorldState(GamePacket packet) {
                if (!handshakeReceived) return;
                List<GamePacket.PlayerData> playersData = packet.getPlayersData();
                if (playersData == null) return;
                Set<Integer> currentIds = new HashSet<>();
                for (GamePacket.PlayerData playerData : playersData) {
                    int otherPlayerId = playerData.getId();
                    if (otherPlayerId == playerId) continue;
                    currentIds.add(otherPlayerId);
                    RemotePlayerEntity rpe = remotePlayers.get(otherPlayerId);
                    if (rpe == null) {
                        rpe = new RemotePlayerEntity(otherPlayerId, gamePanel);
                        remotePlayers.put(otherPlayerId, rpe);
                    }
                    rpe.setTargetPosition(playerData.getX(), playerData.getY());
                    rpe.setDirection(playerData.getDirectionString());
                    rpe.setHasTarget(true);
                    rpe.setSpriteNum(playerData.getSpriteNum());
                    PlayerStats remoteStats = new PlayerStats(
                            playerData.getLevel(),
                            playerData.getHealth(),
                            playerData.getMaxHealth(),
                            playerData.getDamage(),
                            playerData.getExperience()
                    );
                    remoteStats.setExperienceToNextLevel(playerData.getExperienceToNextLevel());
                    rpe.setStats(remoteStats);
                    rpe.setDead(playerData.isDead());
                }
                Iterator<Integer> iterator = remotePlayers.keySet().iterator();
                while (iterator.hasNext()) {
                    int id = iterator.next();
                    if (!currentIds.contains(id) && id != playerId) {
                        iterator.remove();
                    }
                }
            }

            @Override
            public void onPlayerJoin(GamePacket packet) {
                if (!handshakeReceived) return;
                int joinedPlayerId = packet.getPlayerId();
                if (joinedPlayerId == playerId) {
                    return;
                }
                RemotePlayerEntity rpe = new RemotePlayerEntity(joinedPlayerId, gamePanel);
                rpe.setTargetPosition(packet.getX(), packet.getY());
                rpe.setDirection(GameProtocol.byteToDirection(packet.getDirection()));
                rpe.setHasTarget(true);
                remotePlayers.put(joinedPlayerId, rpe);
            }

            @Override
            public void onPlayerDamage(GamePacket packet) {
                if (!handshakeReceived) return;
                if (packet.getTargetId() == playerId) {
                    takeDamage(packet.getHealth(), packet.getMaxHealth(), packet.getLevel(), packet.getDamage());
                    if (stats.getHealth() <= 0 && !isDead) {
                        showDeathScreen = true;
                        isDead = true;
                        deathTimer = DEATH_DURATION;
                        deathAnimationFrame = 0;
                        deathAnimationTimer = 0;
                        keyHandler.upPressed = false;
                        keyHandler.downPressed = false;
                        keyHandler.leftPressed = false;
                        keyHandler.rightPressed = false;
                        keyHandler.attackPressed = false;
                    }
                }
                RemotePlayerEntity rpe = remotePlayers.get(packet.getTargetId());
                if (rpe != null) {
                    rpe.setStats(new PlayerStats(
                            packet.getLevel(),
                            packet.getHealth(),
                            packet.getMaxHealth(),
                            packet.getDamage(),
                            0
                    ));
                }
            }

            @Override
            public void onPlayerDeath(GamePacket packet) {
                if (!handshakeReceived) return;
                int deadPlayerId = packet.getPlayerId();

                if (deadPlayerId == playerId) {
                    showDeathScreen = true;
                    isDead = true;
                    stats.setHealth(0);
                    deathTimer = DEATH_DURATION;
                    deathAnimationFrame = 0;
                    deathAnimationTimer = 0;
                    keyHandler.upPressed = false;
                    keyHandler.downPressed = false;
                    keyHandler.leftPressed = false;
                    keyHandler.rightPressed = false;
                    keyHandler.attackPressed = false;
                } else {
                    RemotePlayerEntity rpe = remotePlayers.get(deadPlayerId);
                    if (rpe != null) {
                        rpe.die();
                    }
                }
            }

            @Override
            public void onPlayerLeave(GamePacket packet) {
                if (!handshakeReceived) return;
                int leftPlayerId = packet.getPlayerId();
                if (leftPlayerId == playerId) {
                    return;
                }
                remotePlayers.remove(leftPlayerId);
            }

            @Override
            public void onError(String message) {
            }

            @Override
            public void onDisconnect() {
                remotePlayers.clear();
            }
        });
    }

    private void takeDamage(int newHealth, int maxHealth, int newLevel, int newDamage) {
        if (isDead) return;
        stats.setHealth(newHealth);
        stats.setMaxHealth(maxHealth);
        stats.setLevel(newLevel);
        stats.setDamage(newDamage);
        beingPushed = true;
        pushTargetX = worldX;
        pushTargetY = worldY + 20;
        if (!stats.isAlive()) {
            die();
        }
    }

    private void die() {
        isDead = true;
        stats.setHealth(0);
        deathTimer = DEATH_DURATION;
        deathAnimationFrame = 0;
        deathAnimationTimer = 0;
        showDeathScreen = true;
        keyHandler.upPressed = false;
        keyHandler.downPressed = false;
        keyHandler.leftPressed = false;
        keyHandler.rightPressed = false;
        keyHandler.attackPressed = false;
    }

    private void updateMovement() {
        boolean moved = false;
        if (keyHandler.upPressed) {
            direction = "up";
        }
        if (keyHandler.downPressed) {
            direction = "down";
        }
        if (keyHandler.leftPressed) {
            direction = "left";
        }
        if (keyHandler.rightPressed) {
            direction = "right";
        }
        if (keyHandler.upPressed || keyHandler.downPressed ||
                keyHandler.leftPressed || keyHandler.rightPressed) {
            collisionOn = false;
            gamePanel.getCollisionChecker().checkTile(this);
            if (collisionOn == false) {
                switch (direction) {
                    case "up":
                        worldY -= speed;
                        moved = true;
                        break;
                    case "down":
                        worldY += speed;
                        moved = true;
                        break;
                    case "left":
                        worldX -= speed;
                        moved = true;
                        break;
                    case "right":
                        worldX += speed;
                        moved = true;
                        break;
                }
            }
        }
        if (keyHandler.attackPressed && !attackAnimationPlaying) {
            startAttack();
        }
        int worldWidth = gamePanel.getWorldWidth();
        int worldHeight = gamePanel.getWorldHeight();
        int tileSize = gamePanel.getTileSize();
        worldX = Math.max(0, Math.min(worldX, worldWidth - tileSize));
        worldY = Math.max(0, Math.min(worldY, worldHeight - tileSize));
        if (moved) {
            spriteCounter++;
            if (spriteCounter > 12) {
                spriteNum = (spriteNum == 1) ? 2 : 1;
                spriteCounter = 0;
            }
        } else {
            spriteNum = 1;
            spriteCounter = 0;
        }
    }

    private void updateAttackAnimation() {
        if (attackAnimationPlaying) {
            attackCounter++;
            spriteCounter++;
            if (spriteCounter > 8) {
                spriteNum = (spriteNum == 1) ? 2 : 1;
                spriteCounter = 0;
            }
            if (attackCounter >= attackDuration) {
                endAttack();
            }
        }
    }

    private void updatePushAnimation() {
        if (beingPushed) {
            int dx = pushTargetX - worldX;
            int dy = pushTargetY - worldY;
            int distance = (int) Math.sqrt(dx * dx + dy * dy);
            if (distance < pushSpeed) {
                worldX = pushTargetX;
                worldY = pushTargetY;
                beingPushed = false;
            } else {
                worldX += (dx * pushSpeed) / distance;
                worldY += (dy * pushSpeed) / distance;
            }
        }
    }

    @Override
    public void update() {
        if (gameOver) return;
        if (isDead) {
            updateDeathAnimation();
            return;
        }
        if (!handshakeReceived && gameClient != null && gameClient.isConnected()) {
            if (gameClient.getPlayerId() > 0) {
                playerId = gameClient.getPlayerId();
                handshakeReceived = true;
            }
            return;
        }
        updateAttackAnimation();
        updatePushAnimation();
        if (attackAnimationPlaying || beingPushed) {
            return;
        }
        updateMovement();
        if (gameClient != null && gameClient.isConnected() && handshakeReceived) {
            sendUpdatesToServer();
        }
        updateRemotePlayers();
    }

    private void updateDeathAnimation() {
        if (isDead && deathTimer > 0) {
            deathTimer--;
            deathAnimationTimer++;
            if (deathAnimationTimer >= DEATH_ANIMATION_FRAME_DURATION) {
                deathAnimationTimer = 0;
                deathAnimationFrame = (deathAnimationFrame + 1) % DEATH_ANIMATION_TOTAL_FRAMES;
            }
        }

        if (deathTimer <= 0) {
            showDeathScreen = false;
            gameOver = true;
            if (gameClient != null && gameClient.isConnected()) {
                gameClient.disconnect();
            }
        }
    }

    private void sendUpdatesToServer() {
        if (isDead || gameOver) return;
        if (gameClient != null && gameClient.isConnected() && playerId > 0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSendTime > SEND_INTERVAL) {
                boolean positionChanged = (worldX != lastSentX || worldY != lastSentY);
                boolean directionChanged = !direction.equals(lastSentDirection);
                boolean spriteChanged = (spriteNum != lastSentSpriteNum);

                if (positionChanged || directionChanged || spriteChanged) {
                    Integer sendX = positionChanged ? worldX : null;
                    Integer sendY = positionChanged ? worldY : null;
                    String sendDirection = directionChanged ? direction : null;
                    Byte sendSpriteNum = spriteChanged ? (byte)spriteNum : null;

                    gameClient.sendPlayerUpdate(sendX, sendY, sendDirection, sendSpriteNum);

                    lastSentX = worldX;
                    lastSentY = worldY;
                    lastSentDirection = direction;
                    lastSentSpriteNum = spriteNum;
                    lastSendTime = currentTime;
                }
            }
        }
    }

    public void startAttack() {
        if (isDead || gameOver || !handshakeReceived) return;
        attacking = true;
        attackAnimationPlaying = true;
        attackCounter = 0;
        spriteNum = 1;
        spriteCounter = 0;
        if (gameClient != null && gameClient.isConnected()) {
            gameClient.sendAttack(direction, worldX, worldY);
        }
    }

    private void onHit(int targetX, int targetY) {
        beingPushed = true;
        pushTargetX = targetX;
        pushTargetY = targetY;
    }

    private void endAttack() {
        attacking = false;
        attackAnimationPlaying = false;
        attackCounter = 0;
        spriteNum = 1;
        spriteCounter = 0;
    }

    private void updateRemotePlayers() {
        for (RemotePlayerEntity rpe : remotePlayers.values()) {
            rpe.update();
        }
    }

    @Override
    public void draw(GraphicsContext gc) {
        if (gameOver) {
            drawGameOverScreen(gc);
            return;
        }
        if (!handshakeReceived) {
            drawWaitingScreen(gc);
            return;
        }
        drawLocalPlayer(gc);
        for (RemotePlayerEntity rpe : remotePlayers.values()) {
            rpe.draw(gc);
        }
        drawHealthBar(gc);
    }

    private void drawGameOverScreen(GraphicsContext gc) {
        gc.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.9));
        gc.fillRect(0, 0, gamePanel.getScreenWidth(), gamePanel.getScreenHeight());
        gc.setFill(javafx.scene.paint.Color.RED);
        gc.fillText("ИГРА ОКОНЧЕНА",
                gamePanel.getScreenWidth() / 2 - 60,
                gamePanel.getScreenHeight() / 2 - 30);
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillText("Вы были убиты",
                gamePanel.getScreenWidth() / 2 - 50,
                gamePanel.getScreenHeight() / 2 + 10);
    }

    private void drawWaitingScreen(GraphicsContext gc) {
        gc.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 0, gamePanel.getScreenWidth(), gamePanel.getScreenHeight());
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillText("Ожидание подключения к серверу...",
                gamePanel.getScreenWidth() / 2 - 150,
                gamePanel.getScreenHeight() / 2 - 50);
        if (gameClient != null && !gameClient.isConnected()) {
            gc.setFill(javafx.scene.paint.Color.RED);
            gc.fillText("Нет подключения",
                    gamePanel.getScreenWidth() / 2 - 60,
                    gamePanel.getScreenHeight() / 2 + 20);
        }
    }

    private void drawLocalPlayer(GraphicsContext gc) {
        Image image = getCurrentImage();
        if (image != null) {
            int tileSize = gamePanel.getTileSize();
            gc.drawImage(image, screenX, screenY, tileSize, tileSize);
        }
    }

    private void drawHealthBar(GraphicsContext gc) {
        int barWidth = 150;
        int barHeight = 15;
        int x = 10;
        int y = 10;
        gc.setFill(javafx.scene.paint.Color.GRAY);
        gc.fillRect(x, y, barWidth, barHeight);
        double hpPercent = stats.getHealthPercentage();
        if (hpPercent > 0.5) {
            gc.setFill(javafx.scene.paint.Color.GREEN);
        } else if (hpPercent > 0.25) {
            gc.setFill(javafx.scene.paint.Color.YELLOW);
        } else {
            gc.setFill(javafx.scene.paint.Color.RED);
        }
        gc.fillRect(x, y, barWidth * hpPercent, barHeight);
        gc.setStroke(javafx.scene.paint.Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRect(x, y, barWidth, barHeight);
        gc.setFill(javafx.scene.paint.Color.WHITE);
        gc.fillText("Lvl " + stats.getLevel() + " HP: " + stats.getHealth() + "/" + stats.getMaxHealth(),
                x + 5, y + barHeight + 15);
        gc.fillText("Damage: " + stats.getDamage() + " XP: " + stats.getExperience() + "/" +
                stats.getExperienceToNextLevel(), x + 5, y + barHeight + 35);
        if (isDead) {
            gc.setFill(javafx.scene.paint.Color.RED);
            gc.fillText("DEAD!", x + 5, y + barHeight + 55);
        }
    }

    private Image getCurrentImage() {
        if (isDead && deathTimer > 0) {
            switch (deathAnimationFrame) {
                case 0: return death1 != null ? death1 : down1;
                case 1: return death2 != null ? death2 : down1;
                case 2: return death3 != null ? death3 : down1;
                case 3: return death4 != null ? death4 : down1;
                case 4: return death5 != null ? death5 : down1;
                default: return death1 != null ? death1 : down1;
            }
        }

        if (attackAnimationPlaying) {
            switch (direction) {
                case "up":
                    return (spriteNum == 1) ? attackUp1 : attackUp2;
                case "down":
                    return (spriteNum == 1) ? attackDown1 : attackDown2;
                case "left":
                    return attackLeft;
                case "right":
                    return attackRight;
            }
        } else {
            switch (direction) {
                case "up":
                    return (spriteNum == 1) ? up1 : up2;
                case "down":
                    return (spriteNum == 1) ? down1 : down2;
                case "left":
                    return (spriteNum == 1) ? left1 : left2;
                case "right":
                    return (spriteNum == 1) ? right1 : right2;
            }
        }
        return down1;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
        handshakeReceived = true;
    }

    public int getPlayerId() {
        return playerId;
    }

    public boolean isConnected() {
        return gameClient != null && gameClient.isConnected();
    }

    public boolean isHandshakeReceived() {
        return handshakeReceived;
    }

    public int getScreenX() { return screenX; }
    public int getScreenY() { return screenY; }

    private static class RemotePlayerEntity {
        private int id;
        private GamePanel gamePanel;
        private int currentX, currentY;
        private int targetX, targetY;
        private String direction;
        private int spriteNum = 1;
        private int spriteCounter = 0;
        private boolean attacking = false;
        private int attackAnimationCounter = 0;
        private PlayerStats stats;
        private boolean isDead = false;
        private int deathAnimationCounter = 0;
        private int deathAnimationFrame = 0;
        private int deathAnimationTimer = 0;
        private static final int DEATH_ANIMATION_FRAME_DURATION = 10;
        private static final int DEATH_ANIMATION_TOTAL_FRAMES = 5;
        private boolean hasTarget = false;
        private static final float LERP_FACTOR = 0.5f;

        RemotePlayerEntity(int id, GamePanel gamePanel) {
            this.id = id;
            this.gamePanel = gamePanel;
            this.currentX = gamePanel.getTileSize() * 20;
            this.currentY = gamePanel.getTileSize() * 7;
            this.targetX = gamePanel.getTileSize() * 20;
            this.targetY = gamePanel.getTileSize() * 7;
            this.direction = "down";
            this.stats = new PlayerStats();
        }

        void setTargetPosition(int x, int y) {
            this.targetX = x;
            this.targetY = y;
            this.hasTarget = true;
        }

        void setDirection(String direction) {
            this.direction = direction;
        }

        void setHasTarget(boolean hasTarget) {
            this.hasTarget = hasTarget;
        }

        void setStats(PlayerStats stats) {
            this.stats = stats;
        }

        void setSpriteNum(byte spriteNum) {
            this.spriteNum = spriteNum;
        }

        void setDead(boolean dead) {
            this.isDead = dead;
            if (dead) {
                deathAnimationCounter = 60;
                deathAnimationFrame = 0;
                deathAnimationTimer = 0;
            }
        }

        void die() {
            isDead = true;
            deathAnimationCounter = 60;
            deathAnimationFrame = 0;
            deathAnimationTimer = 0;
        }

        void playAttackAnimation() {
            attacking = true;
            attackAnimationCounter = 15;
        }

        void update() {
            if (isDead) {
                deathAnimationCounter--;
                if (deathAnimationCounter > 0) {
                    deathAnimationTimer++;
                    if (deathAnimationTimer >= DEATH_ANIMATION_FRAME_DURATION) {
                        deathAnimationTimer = 0;
                        deathAnimationFrame = (deathAnimationFrame + 1) % DEATH_ANIMATION_TOTAL_FRAMES;
                    }
                }
                return;
            }

            if (hasTarget) {
                int dx = targetX - currentX;
                int dy = targetY - currentY;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance > 2.0) {
                    currentX += (int) (dx * LERP_FACTOR);
                    currentY += (int) (dy * LERP_FACTOR);
                    spriteCounter++;
                    if (spriteCounter > 12) {
                        spriteNum = (spriteNum == 1) ? 2 : 1;
                        spriteCounter = 0;
                    }
                } else {
                    currentX = targetX;
                    currentY = targetY;
                    spriteNum = 1;
                    spriteCounter = 0;
                }
            } else {
                spriteNum = 1;
                spriteCounter = 0;
            }
            if (attacking) {
                attackAnimationCounter--;
                if (attackAnimationCounter <= 0) {
                    attacking = false;
                }
            }
        }

        void draw(GraphicsContext gc) {
            if (isDead && deathAnimationCounter <= 0) {
                return;
            }
            Image image = getImage();
            if (image != null) {
                int tileSize = gamePanel.getTileSize();
                int localPlayerWorldX = gamePanel.getPlayer().getWorldX();
                int localPlayerWorldY = gamePanel.getPlayer().getWorldY();
                int localPlayerScreenX = gamePanel.getPlayer().getScreenX();
                int localPlayerScreenY = gamePanel.getPlayer().getScreenY();
                int screenX = currentX - localPlayerWorldX + localPlayerScreenX;
                int screenY = currentY - localPlayerWorldY + localPlayerScreenY;
                if (screenX + tileSize > 0 && screenX < gamePanel.getScreenWidth() &&
                        screenY + tileSize > 0 && screenY < gamePanel.getScreenHeight()) {
                    gc.drawImage(image, screenX, screenY, tileSize, tileSize);
                    if (!isDead) {
                        drawRemoteInfo(gc, screenX, screenY);
                    }
                }
            }
        }

        private void drawRemoteInfo(GraphicsContext gc, int screenX, int screenY) {
            int tileSize = gamePanel.getTileSize();
            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.fillText("Player " + id, screenX, screenY - 20);
            int barWidth = tileSize;
            int barHeight = 5;
            int x = screenX;
            int y = screenY - 10;
            gc.setFill(javafx.scene.paint.Color.GRAY);
            gc.fillRect(x, y, barWidth, barHeight);
            double hpPercent = stats.getHealthPercentage();
            if (hpPercent > 0.5) {
                gc.setFill(javafx.scene.paint.Color.GREEN);
            } else if (hpPercent > 0.25) {
                gc.setFill(javafx.scene.paint.Color.YELLOW);
            } else {
                gc.setFill(javafx.scene.paint.Color.RED);
            }
            gc.fillRect(x, y, barWidth * hpPercent, barHeight);
        }

        private Image getImage() {
            try {
                if (isDead && deathAnimationCounter > 0) {
                    switch (deathAnimationFrame) {
                        case 0: return death1 != null ? death1 : down1;
                        case 1: return death2 != null ? death2 : down1;
                        case 2: return death3 != null ? death3 : down1;
                        case 3: return death4 != null ? death4 : down1;
                        case 4: return death5 != null ? death5 : down1;
                        default: return death1 != null ? death1 : down1;
                    }
                }

                if (attacking) {
                    switch (direction) {
                        case "up":
                            return attackUp1 != null ? attackUp1 :
                                    new Image(getClass().getResourceAsStream("/player/no_back/person-attack-up-1.png"));
                        case "down":
                            return attackDown1 != null ? attackDown1 :
                                    new Image(getClass().getResourceAsStream("/player/no_back/person-attack-down-1.png"));
                        case "left":
                            return attackLeft != null ? attackLeft :
                                    new Image(getClass().getResourceAsStream("/player/no_back/person-attack-left.png"));
                        case "right":
                            return attackRight != null ? attackRight :
                                    new Image(getClass().getResourceAsStream("/player/no_back/person-attack-right.png"));
                    }
                } else {
                    switch (direction) {
                        case "up":
                            return (spriteNum == 1) ? up1 : up2;
                        case "down":
                            return (spriteNum == 1) ? down1 : down2;
                        case "left":
                            return (spriteNum == 1) ? left1 : left2;
                        case "right":
                            return (spriteNum == 1) ? right1 : right2;
                    }
                }
            } catch (Exception e) {
            }
            return down1 != null ? down1 : null;
        }

        static {
            try {
                up1 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/person-up-1.png"));
                up2 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/person-up-2.png"));
                down1 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/person-down-1.png"));
                down2 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/person-down-2.png"));
                left1 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/person-left-1.png"));
                left2 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/person-left-2.png"));
                right1 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/person-right-1.png"));
                right2 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/person-right-2.png"));
                attackRight = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/person-attack-right.png"));
                attackLeft = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/person-attack-left.png"));
                attackUp1 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/person-attack-up-1.png"));
                attackUp2 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/person-attack-up-2.png"));
                attackDown1 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/person-attack-down-1.png"));
                attackDown2 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/person-attack-down-2.png"));
                death1 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/death-1.png"));
                death2 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/death-2.png"));
                death3 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/death-3.png"));
                death4 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/death-4.png"));
                death5 = new Image(NetworkPlayer.class.getResourceAsStream("/player/no_back/death-5.png"));
            } catch (Exception e) {
            }
        }
    }

    public int getRemotePlayerCount() {
        return remotePlayers.size();
    }

    public int getHealth() { return stats.getHealth(); }
    public int getMaxHealth() { return stats.getMaxHealth(); }
    public PlayerStats getStats() { return stats; }
    public boolean isDead() { return isDead || !stats.isAlive(); }
    public boolean isGameOver() { return gameOver; }
    public boolean isShowDeathScreen() { return showDeathScreen; }
}