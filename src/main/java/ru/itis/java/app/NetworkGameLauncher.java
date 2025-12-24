package ru.itis.java.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import ru.itis.java.app.network.SocketGameClient;

public class NetworkGameLauncher extends Application {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 1234;

    @Override
    public void start(Stage primaryStage) {
        try {
            SocketGameClient gameClient = new SocketGameClient(SERVER_HOST, SERVER_PORT);
            if (!gameClient.isConnected()) {
                launchLocalGame(primaryStage);
                return;
            }
            boolean handshakeReceived = waitForHandshake(gameClient, 10000);
            if (!handshakeReceived) {
                launchLocalGame(primaryStage);
                return;
            }
            launchNetworkGame(primaryStage, gameClient);
        } catch (Exception e) {
            launchLocalGame(primaryStage);
        }
    }

    private void launchNetworkGame(Stage stage, SocketGameClient gameClient) {
        GamePanel panel = new GamePanel(gameClient);
        Scene scene = new Scene(panel);
        try {
            Image icon = new Image("Greed_Island_Icon.png");
            stage.getIcons().add(icon);
        } catch (Exception e) {
        }
        stage.setScene(scene);
        stage.setTitle("Greed Island - Multiplayer");
        stage.setOnShown(e -> {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    javafx.application.Platform.runLater(() -> {
                        panel.checkHandshakeReceived();
                        panel.startGameThread();
                    });
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
        stage.setOnCloseRequest(e -> {
            panel.stopGameThread();
            if (gameClient != null) {
                gameClient.disconnect();
            }
        });
        stage.show();
        panel.requestFocus();
    }

    private void launchLocalGame(Stage stage) {
        GamePanel panel = new GamePanel();
        Scene scene = new Scene(panel);
        try {
            Image icon = new Image("Greed_Island_Icon.png");
            stage.getIcons().add(icon);
        } catch (Exception e) {
        }
        stage.setScene(scene);
        stage.setTitle("Greed Island - Local");
        stage.setOnShown(e -> panel.startGameThread());
        stage.setOnCloseRequest(e -> panel.stopGameThread());
        stage.show();
        panel.requestFocus();
    }

    private boolean waitForHandshake(SocketGameClient client, int timeoutMs) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (client.getPlayerId() > 0) {
                return true;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return client.getPlayerId() > 0;
    }

    public static void main(String[] args) {
        launch(args);
    }
}