package ru.itis.java.app.network.server;

import java.io.*;
import java.net.*;

public class PlayerSession {
    private Socket socket;
    private DataInputStream rawIn;
    private DataOutputStream rawOut;
    private PlayerState state;
    private boolean connected = true;
    private ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();

    public PlayerSession(Socket socket, PlayerState state) throws IOException {
        this.socket = socket;
        this.state = state;
        this.rawIn = new DataInputStream(socket.getInputStream());
        this.rawOut = new DataOutputStream(socket.getOutputStream());
    }

    public void sendRaw(byte[] data) throws IOException {
        synchronized (rawOut) {
            rawOut.write(data);
            rawOut.flush();
        }
    }

    public void disconnect() {
        connected = false;
        try {
            socket.close();
        } catch (IOException e) {
        }
    }

    public Socket getSocket() { return socket; }
    public DataInputStream getRawIn() { return rawIn; }
    public DataOutputStream getRawOut() { return rawOut; }
    public PlayerState getState() { return state; }
    public boolean isConnected() { return connected; }
    public ByteArrayOutputStream getMessageBuffer() { return messageBuffer; }
}