package ru.itis.java.app.network.server;

import ru.itis.java.app.entity.PlayerStats;
import ru.itis.java.app.entity.LevelSystem;
import ru.itis.java.app.network.protocol.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class SocketGameServer {
    private static final int PORT = 1234;
    private static final int BUFFER_SIZE = 4096;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private Map<Integer, PlayerSession> sessions = new ConcurrentHashMap<>();
    private Map<Integer, PlayerState> playerStates = new ConcurrentHashMap<>();
    private int nextPlayerId = 1;
    private PacketEncoder encoder = new PacketEncoder();

    private static class PlayerState {
        int id;
        int x = 100, y = 100;
        String direction = "down";
        byte lastSpriteNum = 1;
        PlayerStats stats;
        long lastUpdateTime;
        boolean isDead = false;
        int deathTimer = 0;
        private static final int DEATH_RESPAWN_TIME = 180;

        PlayerState(int id) {
            this.id = id;
            this.stats = new PlayerStats();
            this.lastUpdateTime = System.currentTimeMillis();
            this.isDead = false;
            this.deathTimer = 0;
        }

        void takeDamage(int damage) {
            if (!stats.isAlive() || isDead) return;
            stats.takeDamage(damage);
            if (!stats.isAlive() && !isDead) {
                die();
            }
        }

        void die() {
            isDead = true;
            deathTimer = DEATH_RESPAWN_TIME;
        }

        void update() {
            if (isDead && deathTimer > 0) {
                deathTimer--;
                if (deathTimer <= 0) {
                    respawn();
                }
            }
        }

        void respawn() {
            isDead = false;
            stats.setHealth(stats.getMaxHealth());
            int lostExperience = (int)(stats.getExperience() * 0.1);
            stats.setExperience(Math.max(0, stats.getExperience() - lostExperience));
            x = 100 + (id * 50) % 400;
            y = 100 + (id * 30) % 300;
        }

        boolean isAlive() {
            return stats.isAlive() && !isDead;
        }

        void addExperienceForKill(int victimLevel) {
            int experienceGained = LevelSystem.getExperienceForKill(stats.getLevel(), victimLevel);
            stats.addExperienceWithLevelCheck(experienceGained);
        }
    }

    private class PlayerSession {
        Socket socket;
        DataInputStream rawIn;
        DataOutputStream rawOut;
        PlayerState state;
        boolean connected = true;
        ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();

        PlayerSession(Socket socket, PlayerState state) throws IOException {
            this.socket = socket;
            this.state = state;
            this.rawIn = new DataInputStream(socket.getInputStream());
            this.rawOut = new DataOutputStream(socket.getOutputStream());
            byte[] handshakeData = encoder.encodeHandshake(state.id, state.stats);
            rawOut.write(handshakeData);
            rawOut.flush();
            sendWorldState();
            broadcastPlayerJoin(state.id, state.x, state.y, state.direction, state.stats);
        }

        void sendRaw(byte[] data) throws IOException {
            synchronized (rawOut) {
                rawOut.write(data);
                rawOut.flush();
            }
        }

        void sendWorldState() throws IOException {
            List<GamePacket.PlayerData> snapshot = new ArrayList<>();
            for (PlayerState ps : playerStates.values()) {
                GamePacket.PlayerData data = new GamePacket.PlayerData();
                data.setId(ps.id);
                data.setX(ps.x);
                data.setY(ps.y);
                data.setDirection(GameProtocol.directionToByte(ps.direction));
                if (ps.isDead) {
                    data.setHealth(0);
                } else {
                    data.setHealth(ps.stats.getHealth());
                }
                data.setMaxHealth(ps.stats.getMaxHealth());
                data.setLevel(ps.stats.getLevel());
                data.setDamage(ps.stats.getDamage());
                data.setExperience(ps.stats.getExperience());
                data.setExperienceToNextLevel(ps.stats.getExperienceToNextLevel());
                data.setSpriteNum(ps.lastSpriteNum);
                data.setDead(ps.isDead);
                snapshot.add(data);
            }
            byte[] worldData = encoder.encodeWorldState(snapshot);
            sendRaw(worldData);
        }

        void disconnect() {
            connected = false;
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            threadPool = Executors.newCachedThreadPool();
            new Thread(this::snapshotBroadcastLoop).start();
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
        }
    }

    private void handleClient(Socket socket) {
        PlayerState state = null;
        PlayerSession session = null;
        try {
            synchronized (this) {
                int playerId = nextPlayerId++;
                state = new PlayerState(playerId);
                playerStates.put(Integer.valueOf(playerId), state);
                session = new PlayerSession(socket, state);
                sessions.put(Integer.valueOf(playerId), session);
            }
            PacketDecoder decoder = new PacketDecoder();
            byte[] buffer = new byte[BUFFER_SIZE];
            while (session.connected) {
                int bytesRead = session.rawIn.read(buffer);
                if (bytesRead == -1) break;
                session.messageBuffer.write(buffer, 0, bytesRead);
                byte[] receivedData = session.messageBuffer.toByteArray();
                PacketDecoder.DecodeResult result = decoder.decode(receivedData, receivedData.length);
                for (GamePacket packet : result.packets()) {
                    processPacket(session, packet);
                }
                if (result.bytesProcessed() > 0) {
                    int remaining = receivedData.length - result.bytesProcessed();
                    if (remaining > 0) {
                        byte[] newBuffer = new byte[remaining];
                        System.arraycopy(receivedData, result.bytesProcessed(), newBuffer, 0, remaining);
                        session.messageBuffer.reset();
                        session.messageBuffer.write(newBuffer);
                    } else {
                        session.messageBuffer.reset();
                    }
                }
            }
        } catch (IOException e) {
        }  finally {
            if (session != null) {
                session.disconnect();
                if (state != null) {
                    sessions.remove(state.id);
                    playerStates.remove(state.id);
                    broadcastPlayerLeave(state.id);
                }
            }
        }
    }

    private void broadcastPlayerJoin(int playerId, int x, int y, String direction, PlayerStats stats) {
        try {
            byte[] joinPacket = encoder.encodePlayerJoin(playerId, x, y, direction, stats);
            for (PlayerSession s : sessions.values()) {
                if (s.state.id != playerId) {
                    s.sendRaw(joinPacket);
                }
            }
        } catch (IOException e) {
        }
    }

    private void broadcastPlayerLeave(int playerId) {
        try {
            byte[] leavePacket = encoder.encodePlayerLeave(playerId);
            for (PlayerSession s : sessions.values()) {
                s.sendRaw(leavePacket);
            }
        } catch (IOException e) {
        }
    }

    private void processPacket(PlayerSession session, GamePacket packet) throws IOException {
        PlayerState state = session.state;
        if (state.isDead && packet.getType() == GameProtocol.TYPE_PLAYER_UPDATE) {
            return;
        }
        switch (packet.getType()) {
            case GameProtocol.TYPE_PLAYER_UPDATE:
                if (packet.hasFlag(GameProtocol.FLAG_POSITION)) {
                    int x = packet.getX();
                    int y = packet.getY();
                    if (GameProtocol.validateCoordinates(x, y)) {
                        state.x = x;
                        state.y = y;
                    }
                }
                if (packet.hasFlag(GameProtocol.FLAG_DIRECTION)) {
                    state.direction = GameProtocol.byteToDirection(packet.getDirection());
                }
                if (packet.hasFlag(GameProtocol.FLAG_SPRITE_NUM)) {
                    state.lastSpriteNum = packet.getSpriteNum();
                }
                state.lastUpdateTime = System.currentTimeMillis();
                broadcastPlayerUpdate(state.id, state.x, state.y, state.direction, state.lastSpriteNum);
                break;
            case GameProtocol.TYPE_ATTACK:
                if (!state.isDead) {
                    handleAttack(state, packet);
                }
                break;
        }
    }

    private void broadcastPlayerUpdate(int playerId, int x, int y, String direction, byte spriteNum) {
        try {
            byte[] updatePacket = encoder.encodePlayerUpdate(playerId, x, y, direction, spriteNum);
            for (PlayerSession s : sessions.values()) {
                if (s.state.id != playerId) {
                    s.sendRaw(updatePacket);
                }
            }
        } catch (IOException e) {
        }
    }

    private void handleAttack(PlayerState attacker, GamePacket packet) throws IOException {
        byte[] attackPacket = encoder.encodeAttack(
                attacker.id,
                GameProtocol.byteToDirection(packet.getDirection()),
                attacker.x,
                attacker.y
        );

        for (PlayerSession session : sessions.values()) {
            if (session.connected && session.state != null) {
                try {
                    session.sendRaw(attackPacket);
                } catch (IOException e) {
                }
            }
        }

        for (PlayerState target : playerStates.values()) {
            if (target.id == attacker.id || target.isDead || !target.stats.isAlive()) {
                continue;
            }

            int attackX = attacker.x + 24;
            int attackY = attacker.y + 24;
            Rectangle attackZone = calculateAttackZone(attackX, attackY,
                    GameProtocol.byteToDirection(packet.getDirection()));
            Rectangle targetBounds = new Rectangle(target.x, target.y, 48, 48);

            if (attackZone.intersects(targetBounds)) {
                int damage = attacker.stats.getDamage();
                target.takeDamage(damage);

                byte[] damagePacket = encoder.encodePlayerDamage(
                        attacker.id,
                        target.id,
                        damage,
                        target.isDead ? 0 : target.stats.getHealth(),
                        target.stats.getMaxHealth(),
                        target.stats.getLevel()
                );

                for (PlayerSession session : sessions.values()) {
                    if (session.connected && session.state != null) {
                        try {
                            session.sendRaw(damagePacket);
                        } catch (IOException e) {
                        }
                    }
                }

                if (!target.isAlive()) {
                    attacker.addExperienceForKill(target.stats.getLevel());

                    byte[] deathPacket = encoder.encodePlayerDeath(target.id, attacker.id);
                    for (PlayerSession session : sessions.values()) {
                        if (session.connected && session.state != null) {
                            try {
                                session.sendRaw(deathPacket);
                            } catch (IOException e) {
                            }
                        }
                    }

                    sendWorldStateToAll();
                    handlePlayerDeath(target.id, attacker.id);
                }

                int[] push = calculatePush(target.x, target.y, attacker.x, attacker.y,
                        GameProtocol.byteToDirection(packet.getDirection()));
                target.x = push[0];
                target.y = push[1];
                target.lastUpdateTime = System.currentTimeMillis();

                byte[] pushPacket = encoder.encodePlayerHit(
                        attacker.id,
                        target.id,
                        push[0],
                        push[1]
                );

                PlayerSession targetSession = sessions.get(target.id);
                if (targetSession != null) {
                    try {
                        targetSession.sendRaw(pushPacket);
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    private void handlePlayerDeath(int deadPlayerId, int killerId) throws IOException {
        PlayerState deadPlayer = playerStates.get(deadPlayerId);
        if (deadPlayer != null) {
            deadPlayer.die();
        }
        PlayerSession deadSession = sessions.get(deadPlayerId);
        if (deadSession != null) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        if (deadSession.connected) {
                            deadSession.disconnect();
                            sessions.remove(deadPlayerId);
                            playerStates.remove(deadPlayerId);
                            broadcastPlayerLeave(deadPlayerId);
                        }
                    } catch (Exception e) {
                    }
                }
            }, 5000);
        }
    }

    private int[] calculatePush(int targetX, int targetY, int attackerX, int attackerY, String direction) {
        int pushDistance = 80;
        int newX = targetX;
        int newY = targetY;
        switch (direction) {
            case "up":
                newY -= pushDistance;
                break;
            case "down":
                newY += pushDistance;
                break;
            case "left":
                newX -= pushDistance;
                break;
            case "right":
                newX += pushDistance;
                break;
        }
        newX = Math.max(GameProtocol.MIN_X, Math.min(newX, GameProtocol.MAX_X));
        newY = Math.max(GameProtocol.MIN_Y, Math.min(newY, GameProtocol.MAX_Y));
        return new int[]{newX, newY};
    }

    private Rectangle calculateAttackZone(int x, int y, String direction) {
        int attackRange = GameProtocol.ATTACK_RANGE;
        int attackWidth = 30;
        switch (direction) {
            case "up":
                return new Rectangle(x - attackWidth/2, y - attackRange,
                        attackWidth, attackRange);
            case "down":
                return new Rectangle(x - attackWidth/2, y,
                        attackWidth, attackRange);
            case "left":
                return new Rectangle(x - attackRange, y - attackWidth/2,
                        attackRange, attackWidth);
            case "right":
                return new Rectangle(x, y - attackWidth/2,
                        attackRange, attackWidth);
            default:
                return new Rectangle(x - 24, y - 24, 48, 48);
        }
    }

    private void sendWorldStateToAll() throws IOException {
        if (playerStates.isEmpty()) return;
        List<GamePacket.PlayerData> snapshot = new ArrayList<>();
        for (PlayerState state : playerStates.values()) {
            GamePacket.PlayerData data = new GamePacket.PlayerData();
            data.setId(state.id);
            data.setX(state.x);
            data.setY(state.y);
            data.setDirection(GameProtocol.directionToByte(state.direction));
            if (state.isDead) {
                data.setHealth(0);
            } else {
                data.setHealth(state.stats.getHealth());
            }
            data.setMaxHealth(state.stats.getMaxHealth());
            data.setLevel(state.stats.getLevel());
            data.setDamage(state.stats.getDamage());
            data.setExperience(state.stats.getExperience());
            data.setExperienceToNextLevel(state.stats.getExperienceToNextLevel());
            data.setSpriteNum(state.lastSpriteNum);
            data.setDead(state.isDead);
            snapshot.add(data);
        }
        byte[] worldData = encoder.encodeWorldState(snapshot);
        for (PlayerSession session : sessions.values()) {
            try {
                session.sendRaw(worldData);
            } catch (IOException e) {
            }
        }
    }

    private void snapshotBroadcastLoop() {
        while (true) {
            try {
                Thread.sleep(33);
                for (PlayerState state : playerStates.values()) {
                    state.update();
                }
                sendWorldStateToAll();
            } catch (Exception e) {
            }
        }
    }

    private static class Rectangle {
        int x, y, width, height;

        Rectangle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean intersects(Rectangle other) {
            return x < other.x + other.width && x + width > other.x &&
                    y < other.y + other.height && y + height > other.y;
        }
    }

    public static void main(String[] args) {
        new SocketGameServer().start();
    }
}