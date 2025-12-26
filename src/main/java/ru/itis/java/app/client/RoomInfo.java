package ru.itis.java.app.client;

class RoomInfo {
    String address;
    int port;
    int playerCount;

    RoomInfo(String address, int port, int playerCount) {
        this.address = address;
        this.port = port;
        this.playerCount = playerCount;
    }
}