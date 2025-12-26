package ru.itis.java.app.entity;

import javafx.scene.image.Image;

public class Door extends Item {
    public Door() {
        this.name = "Door";
        this.collision = true;
        try {
            image = new Image(getClass().getResourceAsStream("/objects/door.png"));
        } catch (Exception e) {
            System.err.println("Ошибка загрузки изображения двери: " + e.getMessage());
        }
        collision = true;
    }
}