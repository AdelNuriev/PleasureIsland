package ru.itis.java.app;

import javafx.application.Application;
import javafx.stage.Stage;
import ru.itis.java.app.client.GameClientWindow;

public class NetworkGameLauncher extends Application {

    @Override
    public void start(Stage primaryStage) {
        GameClientWindow clientWindow = new GameClientWindow();
        clientWindow.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}