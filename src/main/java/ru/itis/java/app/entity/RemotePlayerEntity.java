package ru.itis.java.app.entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import ru.itis.java.app.GamePanel;

public class RemotePlayerEntity {
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

    private static Image up1, up2, down1, down2, left1, left2, right1, right2;
    private static Image attackRight, attackLeft, attackUp1, attackUp2, attackDown1, attackDown2;
    private static Image death1, death2, death3, death4, death5;

    public RemotePlayerEntity(int id, GamePanel gamePanel) {
        this.id = id;
        this.gamePanel = gamePanel;
        this.currentX = gamePanel.getTileSize() * 20;
        this.currentY = gamePanel.getTileSize() * 7;
        this.targetX = gamePanel.getTileSize() * 20;
        this.targetY = gamePanel.getTileSize() * 7;
        this.direction = "down";
        this.stats = new PlayerStats();
    }

    public void setTargetPosition(int x, int y) {
        this.targetX = x;
        this.targetY = y;
        this.hasTarget = true;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public void setHasTarget(boolean hasTarget) {
        this.hasTarget = hasTarget;
    }

    public PlayerStats getStats() { return stats; }

    public void setStats(PlayerStats stats) {
        this.stats = stats.copy();
    }

    public void setSpriteNum(byte spriteNum) {
        this.spriteNum = spriteNum;
    }

    public void setDead(boolean dead) {
        this.isDead = dead;
        if (dead) {
            deathAnimationCounter = 60;
            deathAnimationFrame = 0;
            deathAnimationTimer = 0;
        }
    }

    public void die() {
        isDead = true;
        deathAnimationCounter = 60;
        deathAnimationFrame = 0;
        deathAnimationTimer = 0;
    }

    public void playAttackAnimation() {
        attacking = true;
        attackAnimationCounter = 15;
    }

    public void update() {
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

    public void draw(GraphicsContext gc) {
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
            up1 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/person-up-1.png"));
            up2 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/person-up-2.png"));
            down1 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/person-down-1.png"));
            down2 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/person-down-2.png"));
            left1 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/person-left-1.png"));
            left2 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/person-left-2.png"));
            right1 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/person-right-1.png"));
            right2 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/person-right-2.png"));
            attackRight = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/person-attack-right.png"));
            attackLeft = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/person-attack-left.png"));
            attackUp1 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/person-attack-up-1.png"));
            attackUp2 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/person-attack-up-2.png"));
            attackDown1 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/person-attack-down-1.png"));
            attackDown2 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/person-attack-down-2.png"));
            death1 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/death-1.png"));
            death2 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/death-2.png"));
            death3 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/death-3.png"));
            death4 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/death-4.png"));
            death5 = new Image(RemotePlayerEntity.class.getResourceAsStream("/player/no_back/death-5.png"));
        } catch (Exception e) {
        }
    }

    public int getId() { return id; }
    public int getCurrentX() { return currentX; }
    public int getCurrentY() { return currentY; }
    public boolean isAttacking() { return attacking; }
    public boolean isRemoteDead() { return isDead; }
}