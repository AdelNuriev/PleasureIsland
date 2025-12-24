package ru.itis.java.app.network;

import ru.itis.java.app.network.protocol.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class SocketGameClient {
    private Socket socket;
    private DataInputStream rawIn;
    private DataOutputStream rawOut;
    private PacketEncoder encoder;
    private PacketDecoder decoder;
    private ByteArrayOutputStream receiveBuffer;
    private int playerId;
    private volatile boolean connected = false;
    private Thread receiveThread;
    private BlockingQueue<byte[]> sendQueue;
    private PacketListener packetListener;
    private final Object sendLock = new Object();

    public interface PacketListener {
        void onHandshake(GamePacket packet);
        void onPlayerUpdate(GamePacket packet);
        void onAttack(GamePacket packet);
        void onPlayerHit(GamePacket packet);
        void onPlayerDamage(GamePacket packet);
        void onPlayerDeath(GamePacket packet);
        void onWorldState(GamePacket packet);
        void onPlayerJoin(GamePacket packet);
        void onPlayerLeave(GamePacket packet);
        void onError(String message);
        void onDisconnect();
    }

    public SocketGameClient(String host, int port) throws IOException {
        try {
            this.socket = new Socket();
            this.socket.connect(new InetSocketAddress(host, port), 5000);
            this.socket.setTcpNoDelay(true);
            this.socket.setSoTimeout(30000);

            this.rawIn = new DataInputStream(socket.getInputStream());
            this.rawOut = new DataOutputStream(socket.getOutputStream());
            this.encoder = new PacketEncoder();
            this.decoder = new PacketDecoder();
            this.receiveBuffer = new ByteArrayOutputStream();
            this.sendQueue = new LinkedBlockingQueue<>(100);

            this.playerId = 0;

            startThreads();

            Thread.sleep(100);

        } catch (IOException e) {
            disconnect();
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void startThreads() {
        receiveThread = new Thread(() -> {
            System.out.println("[CLIENT] Receive thread started");
            byte[] buffer = new byte[1024];
            try {
                while (connected) {
                    int bytesRead;
                    try {
                        bytesRead = rawIn.read(buffer);
                        if (bytesRead == -1) {
                            System.out.println("[CLIENT] Connection closed by server (EOF)");
                            break;
                        }
                        System.out.println("[CLIENT] Received " + bytesRead + " raw bytes");

                        System.out.print("[CLIENT] Raw data (first 30 bytes hex): ");
                        for (int i = 0; i < Math.min(bytesRead, 30); i++) {
                            System.out.printf("%02X ", buffer[i] & 0xFF);
                        }
                        System.out.println();

                    } catch (SocketTimeoutException e) {
                        continue;
                    } catch (IOException e) {
                        if (connected) {
                            System.err.println("[CLIENT] Read error: " + e.getMessage());
                        }
                        break;
                    }

                    synchronized (receiveBuffer) {
                        receiveBuffer.write(buffer, 0, bytesRead);
                        byte[] allData = receiveBuffer.toByteArray();

                        System.out.println("[CLIENT] Total buffer size after write: " + allData.length);

                        boolean hasStartByte = false;
                        for (int i = 0; i < allData.length; i++) {
                            if ((allData[i] & 0xFF) == 0xFF) {
                                hasStartByte = true;
                                System.out.println("[CLIENT] Found PACKET_START at position " + i);
                                break;
                            }
                        }
                        if (!hasStartByte) {
                            System.out.println("[CLIENT] WARNING: No PACKET_START in buffer!");
                        }

                        PacketDecoder.DecodeResult result = decoder.decode(allData, allData.length);

                        for (int i = 0; i < result.packets().size(); i++) {
                            GamePacket packet = result.packets().get(i);
                            System.out.println("[CLIENT] Packet #" + (i+1) + ": " + packet);
                            System.out.println("  Type: " + packet.getType() +
                                    " (0x" + String.format("%02X", packet.getType()) + ")");
                            if (packet.isHandshake()) {
                                System.out.println("  THIS IS A HANDSHAKE PACKET!");
                                System.out.println("  Player ID in packet: " + packet.getPlayerId());

                                System.out.println("[CLIENT] MANUALLY calling handlePacket...");
                                handlePacket(packet);
                            } else {
                                handlePacket(packet);
                            }
                        }

                        System.out.println("[CLIENT] Decoder result:");
                        System.out.println("  - Packets found: " + result.packets().size());
                        System.out.println("  - Bytes processed: " + result.bytesProcessed());
                        System.out.println("  - Has more data: " + result.hasMoreData());

                        for (int i = 0; i < result.packets().size(); i++) {
                            GamePacket packet = result.packets().get(i);
                            System.out.println("[CLIENT] Packet #" + (i+1) + ": " + packet);
                            System.out.println("  Type: " + packet.getType() +
                                    " (0x" + String.format("%02X", packet.getType()) + ")");
                            if (packet.isHandshake()) {
                                System.out.println("  THIS IS A HANDSHAKE PACKET!");
                                System.out.println("  Player ID in packet: " + packet.getPlayerId());
                            }
                            handlePacket(packet);
                        }

                        if (result.bytesProcessed() > 0) {
                            int remaining = allData.length - result.bytesProcessed();
                            if (remaining > 0) {
                                byte[] newBuffer = new byte[remaining];
                                System.arraycopy(allData, result.bytesProcessed(), newBuffer, 0, remaining);
                                receiveBuffer.reset();
                                receiveBuffer.write(newBuffer);
                                System.out.println("[CLIENT] " + remaining + " bytes kept in buffer");

                                System.out.print("[CLIENT] Remaining buffer (hex): ");
                                for (int i = 0; i < Math.min(newBuffer.length, 20); i++) {
                                    System.out.printf("%02X ", newBuffer[i] & 0xFF);
                                }
                                System.out.println();
                            } else {
                                receiveBuffer.reset();
                                System.out.println("[CLIENT] Buffer cleared");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[CLIENT] Error in receive thread: " + e.getMessage());
                e.printStackTrace();
            } finally {
                System.out.println("[CLIENT] Receive thread ending");
                if (connected) {
                    disconnect();
                    if (packetListener != null) {
                        packetListener.onDisconnect();
                    }
                }
            }
        });

        Thread sendThread = new Thread(() -> {
            while (connected) {
                try {
                    byte[] data = sendQueue.poll(10, TimeUnit.MILLISECONDS);
                    if (data != null) {
                        System.out.println("[CLIENT] Sending " + data.length + " bytes");
                        synchronized (sendLock) {
                            rawOut.write(data);
                            rawOut.flush();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (connected) {
                        System.err.println("[CLIENT] Send error: " + e.getMessage());
                        disconnect();
                    }
                }
            }
        });

        receiveThread.setDaemon(true);
        sendThread.setDaemon(true);
        connected = true;
        receiveThread.start();
        sendThread.start();
    }

    private void handlePacket(GamePacket packet) {
        if (packet == null) {
            System.out.println("[CLIENT] ERROR: Packet is null!");
            return;
        }

        if (packet.isHandshake()) {
            int newPlayerId = packet.getPlayerId();
            this.playerId = newPlayerId;
        }

        try {
            byte packetType = packet.getType();

            switch (packetType) {
                case GameProtocol.TYPE_HANDSHAKE:
                    packetListener.onHandshake(packet);
                    break;
                case GameProtocol.TYPE_WORLD_STATE:
                    packetListener.onWorldState(packet);
                    break;
                case GameProtocol.TYPE_PLAYER_UPDATE:
                    packetListener.onPlayerUpdate(packet);
                    break;
                case GameProtocol.TYPE_PLAYER_HIT:
                    packetListener.onPlayerHit(packet);
                    break;
                case GameProtocol.TYPE_ATTACK:
                    packetListener.onAttack(packet);
                    break;
                case GameProtocol.TYPE_PLAYER_DAMAGE:
                    packetListener.onPlayerDamage(packet);
                    break;
                case GameProtocol.TYPE_PLAYER_DEATH:
                    packetListener.onPlayerDeath(packet);
                    break;
                case GameProtocol.TYPE_PLAYER_JOIN:
                    packetListener.onPlayerJoin(packet);
                    break;
                case GameProtocol.TYPE_PLAYER_LEAVE:
                    packetListener.onPlayerLeave(packet);
                    break;
                default:
                    packetListener.onError("Unknown packet type: " + packetType);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendPlayerUpdate(Integer x, Integer y, String direction, Byte spriteNum) {
        if (!connected) return;
        try {
            byte[] data = encoder.encodePlayerUpdate(playerId, x, y, direction, spriteNum);
            if (data != null) {
                sendQueue.offer(data);
            }
        } catch (Exception e) {
            System.err.println("Error encoding player update: " + e.getMessage());
        }
    }

    public void sendAttack(String direction, int x, int y) {
        if (!connected) return;
        try {
            byte[] data = encoder.encodeAttack(playerId, direction, x, y);
            if (data != null) {
                sendQueue.offer(data);
            }
        } catch (Exception e) {
            System.err.println("Error encoding attack: " + e.getMessage());
        }
    }

    public void sendFastPlayerUpdate(int x, int y, byte direction, byte spriteNum) {
        if (!connected) return;
        try {
            byte[] data = encoder.fastEncodePlayerUpdate(playerId, x, y, direction, spriteNum);
            if (data != null) {
                sendQueue.offer(data);
            }
        } catch (Exception e) {
            System.err.println("Error encoding fast update: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (!connected) return;
        connected = false;
        try {
            socket.close();
        } catch (IOException e) {
        }
        encoder.close();
        decoder.reset();
        System.out.println("Client disconnected");
    }

    public int getPlayerId() {
        return playerId;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed() && socket.isConnected();
    }

    public void setPacketListener(PacketListener listener) {
        this.packetListener = listener;
    }

    public PacketEncoder getEncoder() {
        return encoder;
    }

    public PacketDecoder getDecoder() {
        return decoder;
    }

    public InetAddress getServerAddress() {
        return socket != null ? socket.getInetAddress() : null;
    }

    public int getServerPort() {
        return socket != null ? socket.getPort() : -1;
    }
}