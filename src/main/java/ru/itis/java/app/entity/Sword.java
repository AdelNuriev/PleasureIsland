package ru.itis.java.app.entity;

import javafx.scene.image.Image;

public class Sword extends Item {
    public Sword() {
        this.name = "Sword";
        try {
            image = new Image(getClass().getResourceAsStream("/objects/sword.png"));
        } catch (Exception e) {
            System.err.println("Ошибка загрузки изображений предмета: " + e.getMessage());
        }
    }
}