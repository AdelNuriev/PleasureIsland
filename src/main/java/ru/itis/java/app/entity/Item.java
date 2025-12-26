package ru.itis.java.app.entity;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.shape.Rectangle;
import ru.itis.java.app.GamePanel;

public class Item {
    protected Image image;
    protected String name;
    protected boolean collision = false;
    protected int worldX, worldY;
    protected final Rectangle solidArea = new Rectangle(0, 0, 48, 48);
    protected int solidAreaDefaultX = 0;
    protected int solidAreaDefaultY = 0;
    protected int itemId;
    protected int experienceReward = 0;
    protected boolean collected = false;

    public void draw(GraphicsContext gc, GamePanel gamePanel) {
        if (collected) return;

        int screenX = worldX - gamePanel.getPlayer().getWorldX() + gamePanel.getPlayer().getScreenX();
        int screenY = worldY - gamePanel.getPlayer().getWorldY() + gamePanel.getPlayer().getScreenY();

        if (worldX + gamePanel.getTileSize() > gamePanel.getPlayer().getWorldX() - gamePanel.getPlayer().getScreenX() &&
                worldX - gamePanel.getTileSize() < gamePanel.getPlayer().getWorldX() + gamePanel.getPlayer().getScreenX() &&
                worldY + gamePanel.getTileSize() > gamePanel.getPlayer().getWorldY() - gamePanel.getPlayer().getScreenY() &&
                worldY - gamePanel.getTileSize() < gamePanel.getPlayer().getWorldY() + gamePanel.getPlayer().getScreenY())
        {
            gc.drawImage(image, screenX, screenY, gamePanel.getTileSize(), gamePanel.getTileSize());
        }
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public int getExperienceReward() { return experienceReward; }
    public void setExperienceReward(int experienceReward) { this.experienceReward = experienceReward; }
    public boolean isCollected() { return collected; }
    public void setCollected(boolean collected) { this.collected = collected; }
    public Image getImage() { return image; }
    public void setImage(Image image) { this.image = image; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isCollision() { return collision; }
    public void setCollision(boolean collision) { this.collision = collision; }
    public int getWorldX() { return worldX; }
    public void setWorldX(int worldX) { this.worldX = worldX; }
    public int getWorldY() { return worldY; }
    public void setWorldY(int worldY) { this.worldY = worldY; }
    public Rectangle getSolidArea() { return solidArea; }
    public int getSolidAreaDefaultX() { return solidAreaDefaultX; }
    public int getSolidAreaDefaultY() { return solidAreaDefaultY; }
}