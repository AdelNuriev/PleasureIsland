package ru.itis.java.app.network.server;

public class ItemState {
    private int id;
    private String type;
    private int x, y;
    private boolean collected = false;
    private int experienceReward;

    public ItemState(int id, String type, int x, int y, int experienceReward) {
        this.id = id;
        this.type = type;
        this.x = x;
        this.y = y;
        this.experienceReward = experienceReward;
    }

    public int getId() { return id; }
    public String getType() { return type; }
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isCollected() { return collected; }
    public void setCollected(boolean collected) { this.collected = collected; }
    public int getExperienceReward() { return experienceReward; }
}