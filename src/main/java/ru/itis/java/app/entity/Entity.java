package ru.itis.java.app.entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.shape.Rectangle;

public abstract class Entity {

    protected int worldX, worldY;
    protected int speed;
    protected int attackDuration;

    protected static Image up1, up2, down1, down2, left1, left2, right1, right2;
    protected static Image attackRight, attackLeft, attackUp1, attackUp2, attackDown1, attackDown2;
    protected static Image death1, death2, death3, death4, death5;
    protected String direction;
    protected boolean attacking;

    protected Rectangle solidArea;
    protected int solidAreaDefaultX, solidAreaDefaultY;
    protected boolean collisionOn = false;

    protected int spriteCounter = 0;
    protected int spriteNum = 1;

    public abstract void update();
    public abstract void draw(GraphicsContext gc);

    protected void loadPlayerImages() {
        try {
            up1 = new Image(getClass().getResourceAsStream("/player/no_back/person-up-1.png"));
            up2 = new Image(getClass().getResourceAsStream("/player/no_back/person-up-2.png"));
            down1 = new Image(getClass().getResourceAsStream("/player/no_back/person-down-1.png"));
            down2 = new Image(getClass().getResourceAsStream("/player/no_back/person-down-2.png"));
            left1 = new Image(getClass().getResourceAsStream("/player/no_back/person-left-1.png"));
            left2 = new Image(getClass().getResourceAsStream("/player/no_back/person-left-2.png"));
            right1 = new Image(getClass().getResourceAsStream("/player/no_back/person-right-1.png"));
            right2 = new Image(getClass().getResourceAsStream("/player/no_back/person-right-2.png"));

            attackRight = new Image(getClass().getResourceAsStream("/player/no_back/person-attack-right.png"));
            attackLeft = new Image(getClass().getResourceAsStream("/player/no_back/person-attack-left.png"));
            attackUp1 = new Image(getClass().getResourceAsStream("/player/no_back/person-attack-up-1.png"));
            attackUp2 = new Image(getClass().getResourceAsStream("/player/no_back/person-attack-up-2.png"));
            attackDown1 = new Image(getClass().getResourceAsStream("/player/no_back/person-attack-down-1.png"));
            attackDown2 = new Image(getClass().getResourceAsStream("/player/no_back/person-attack-down-2.png"));

            death1 = new Image(getClass().getResourceAsStream("/player/no_back/person-death-1.png"));
            death2 = new Image(getClass().getResourceAsStream("/player/no_back/person-death-2.png"));
            death3 = new Image(getClass().getResourceAsStream("/player/no_back/person-death-3.png"));
            death4 = new Image(getClass().getResourceAsStream("/player/no_back/person-death-4.png"));
            death5 = new Image(getClass().getResourceAsStream("/player/no_back/person-death-5.png"));
        } catch (Exception e) {
            System.err.println("Ошибка загрузки изображений игрока: " + e.getMessage());
        }
    }

    public int getWorldX() { return worldX; }
    public void setWorldX(int worldX) { this.worldX = worldX; }
    public int getWorldY() { return worldY; }
    public void setWorldY(int worldY) { this.worldY = worldY; }
    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public boolean isAttacking() { return attacking; }
    public Rectangle getSolidArea() { return solidArea; }
    public void setSolidArea(Rectangle solidArea) { this.solidArea = solidArea; }
    public boolean isCollisionOn() { return collisionOn; }
    public void setCollisionOn(boolean collisionOn) { this.collisionOn = collisionOn; }
    public int getSolidAreaDefaultX() { return solidAreaDefaultX; }
    public int getSolidAreaDefaultY() { return solidAreaDefaultY; }
}
