package ru.itis.java.app.entity;

import javafx.scene.image.Image;

public class Shield extends Item {
    public Shield() {
        this.name = "Shield";
        try {
            image = new Image(getClass().getResourceAsStream("/objects/shield.png"));
        } catch (Exception e) {
            System.err.println("Ошибка загрузки изображений предмета: " + e.getMessage());
        }
    }
}
