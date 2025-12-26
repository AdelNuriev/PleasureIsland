package ru.itis.java.app.client;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;

import ru.itis.java.app.GamePanel;
import ru.itis.java.app.network.SocketGameClient;
import ru.itis.java.app.network.server.SocketGameServer;

public class GameClientWindow {
    private Stage stage;
    private VBox root;
    private Label statusLabel;
    private Button createRoomButton;
    private Button refreshRoomsButton;
    private ListView<String> roomsListView;
    private ObservableList<String> roomList;
    private Map<String, RoomInfo> roomInfoMap;
    private SocketGameServer currentServer;
    private ExecutorService executorService;
    private Stage notificationStage;

    private static final int START_PORT = 1234;
    private static final int MAX_PORT = 1300;
    private static final int MAX_PLAYERS = 100;

    public GameClientWindow() {
        executorService = Executors.newCachedThreadPool();
        roomList = FXCollections.observableArrayList();
        roomInfoMap = new HashMap<>();
        initializeUI();
    }

    private void initializeUI() {
        stage = new Stage();
        stage.setTitle("Pleasure Island - Game Client");
        stage.setWidth(500);
        stage.setHeight(500);

        String bgColor = "#2b2b2b";
        String textColor = "white";
        String buttonStyle = "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;";
        String listStyle = "-fx-background-color: #3c3c3c; -fx-text-fill: white;";

        root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + bgColor + ";");

        Label titleLabel = new Label("PLEASURE ISLAND MULTIPLAYER");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: " + textColor + "; -fx-font-weight: bold;");

        HBox statusPanel = new HBox(10);
        statusPanel.setAlignment(Pos.CENTER_LEFT);
        statusLabel = new Label("Статус: Готов");
        statusLabel.setStyle("-fx-text-fill: " + textColor + ";");
        statusPanel.getChildren().add(statusLabel);

        HBox controlPanel = new HBox(10);
        controlPanel.setAlignment(Pos.CENTER);

        createRoomButton = new Button("СОЗДАТЬ КОМНАТУ");
        createRoomButton.setStyle(buttonStyle);
        createRoomButton.setPrefSize(180, 40);
        createRoomButton.setOnAction(e -> createRoom());

        refreshRoomsButton = new Button("ОБНОВИТЬ");
        refreshRoomsButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold;");
        refreshRoomsButton.setPrefSize(120, 40);
        refreshRoomsButton.setOnAction(e -> discoverRooms());

        controlPanel.getChildren().addAll(createRoomButton, refreshRoomsButton);

        Label roomsTitle = new Label("ДОСТУПНЫЕ КОМНАТЫ:");
        roomsTitle.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 14px;");

        roomsListView = new ListView<>(roomList);
        roomsListView.setStyle(listStyle);
        roomsListView.setPrefHeight(250);

        HBox connectPanel = new HBox(10);
        connectPanel.setAlignment(Pos.CENTER);

        Button joinButton = new Button("ПОДКЛЮЧИТЬСЯ");
        joinButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
        joinButton.setPrefSize(150, 40);
        joinButton.setOnAction(e -> joinRoom());

        Button exitButton = new Button("ВЫХОД");
        exitButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
        exitButton.setPrefSize(100, 40);
        exitButton.setOnAction(e -> System.exit(0));

        connectPanel.getChildren().addAll(joinButton, exitButton);

        root.getChildren().addAll(
                titleLabel,
                new Separator(),
                statusPanel,
                new Separator(),
                controlPanel,
                new Separator(),
                roomsTitle,
                roomsListView,
                connectPanel
        );

        Scene scene = new Scene(root);
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> {
            e.consume();
            exitGame();
        });

        discoverRooms();
    }

    public void show() {
        stage.show();
    }

    private void exitGame() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Выход из игры");
        alert.setHeaderText("Вы уверены, что хотите выйти?");
        alert.setContentText("Все несохраненные данные будут потеряны.");

        if (alert.showAndWait().get() == ButtonType.OK) {
            System.exit(0);
        }
    }

    private int findAvailablePort() {
        for (int port = START_PORT; port <= MAX_PORT; port++) {
            try (ServerSocket testSocket = new ServerSocket(port)) {
                testSocket.close();
                return port;
            } catch (IOException ignored) {}
        }
        return -1;
    }

    private void createRoom() {
        createRoomButton.setDisable(true);
        statusLabel.setText("Создание комнаты...");

        executorService.submit(() -> {
            int port = findAvailablePort();
            if (port == -1) {
                Platform.runLater(() -> {
                    showError("Нет свободных портов!");
                    createRoomButton.setDisable(false);
                });
                return;
            }

            try {
                currentServer = new SocketGameServer(port);
                Thread serverThread = new Thread(() -> {
                    currentServer.start();
                });
                serverThread.setDaemon(true);
                serverThread.start();

                Thread.sleep(1000);

                Platform.runLater(() -> {
                    connectToServer("localhost", port, true);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Ошибка при создании комнаты: " + e.getMessage());
                    createRoomButton.setDisable(false);
                });
            }
        });
    }

    private void discoverRooms() {
        statusLabel.setText("Поиск комнат...");
        refreshRoomsButton.setDisable(true);
        roomList.clear();
        roomInfoMap.clear();

        executorService.submit(() -> {
            List<RoomInfo> discoveredRooms = new ArrayList<>();

            for (int port = START_PORT; port <= MAX_PORT; port++) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress("localhost", port), 200);
                    if (socket.isConnected()) {
                        discoveredRooms.add(new RoomInfo("localhost", port, 1));
                    }
                    socket.close();
                } catch (Exception ignored) {}
            }

            Platform.runLater(() -> {
                for (RoomInfo room : discoveredRooms) {
                    String roomString = String.format("Комната %d (localhost:%d) - 1/%d игроков",
                            room.port, room.port, MAX_PLAYERS);
                    roomList.add(roomString);
                    roomInfoMap.put(roomString, room);
                }

                statusLabel.setText("Найдено комнат: " + discoveredRooms.size());
                refreshRoomsButton.setDisable(false);
            });
        });
    }

    private void joinRoom() {
        String selected = roomsListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Выберите комнату из списка!");
            return;
        }

        RoomInfo room = roomInfoMap.get(selected);
        if (room == null) {
            showError("Не удалось получить информацию о комнате");
            return;
        }

        connectToServer(room.address, room.port, false);
    }

    private void connectToServer(String host, int port, boolean isHost) {
        statusLabel.setText("Подключение к " + host + ":" + port + "...");

        executorService.submit(() -> {
            try {
                SocketGameClient gameClient = new SocketGameClient(host, port);

                if (gameClient.isConnected()) {
                    Platform.runLater(() -> {
                        showGameInProgressNotification();
                        launchGame(gameClient, isHost);
                        stage.hide();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Ошибка подключения: " + e.getMessage());
                });
            }
        });
    }

    private void showGameInProgressNotification() {
        notificationStage = new Stage();
        notificationStage.initStyle(StageStyle.UTILITY);
        notificationStage.setTitle("Игра в процессе");
        notificationStage.setWidth(300);
        notificationStage.setHeight(150);
        notificationStage.setAlwaysOnTop(true);
        notificationStage.setResizable(false);

        VBox notificationBox = new VBox(20);
        notificationBox.setPadding(new Insets(20));
        notificationBox.setAlignment(Pos.CENTER);
        notificationBox.setStyle("-fx-background-color: #2b2b2b;");

        Label icon = new Label("⚔️");
        icon.setStyle("-fx-font-size: 32px;");

        Label message = new Label("GAME IN PROGRESS");
        message.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label info = new Label("Вернитесь в клиент после окончания игры");
        info.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 12px;");
        info.setAlignment(Pos.CENTER);

        notificationBox.getChildren().addAll(icon, message, info);

        Scene notificationScene = new Scene(notificationBox);
        notificationStage.setScene(notificationScene);
        notificationStage.show();
    }

    private void launchGame(SocketGameClient gameClient, boolean isHost) {
        Stage gameStage = new Stage();
        gameStage.setTitle("Pleasure Island" + (isHost ? " [HOST]" : ""));

        GamePanel panel = new GamePanel();

        panel.setupNetworkGame(gameClient);

        Scene scene = new Scene(panel);
        gameStage.setScene(scene);

        gameStage.setOnCloseRequest(e -> {
            panel.stopGameThread();

            if (notificationStage != null) {
                notificationStage.close();
            }

            stage.show();
            discoverRooms();
        });

        gameStage.setOnShown(e -> {
            panel.startGameThread();
        });

        gameStage.show();
        panel.requestFocus();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}