package ru.itis.java.app.entity;

import javafx.scene.image.Image;

public class Key extends Item {
    public Key() {
        this.name = "Key";
        try {
            image = new Image(getClass().getResourceAsStream("/objects/key.png"));
        } catch (Exception e) {
            System.err.println("Ошибка загрузки изображения ключа: " + e.getMessage());
        }
    }
}