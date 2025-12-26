package ru.itis.java.app.network.server;

import ru.itis.java.app.entity.PlayerStats;
import ru.itis.java.app.entity.LevelSystem;
import ru.itis.java.app.network.protocol.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class SocketGameServer {
    private final int PORT;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private Map<Integer, PlayerSession> sessions = new ConcurrentHashMap<>();
    private Map<Integer, PlayerState> playerStates = new ConcurrentHashMap<>();
    private Map<Integer, ItemState> itemStates = new ConcurrentHashMap<>();
    private int nextPlayerId = 1;
    private PacketEncoder encoder = new PacketEncoder();
    private volatile boolean running = true;

    private static class Rectangle {
        private int x, y, width, height;

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

    public SocketGameServer(int port) {
        this.PORT = port;
        initializeItems();
    }

    public SocketGameServer() {
        this(1234);
    }

    private void initializeItems() {
        itemStates.put(1, new ItemState(1, "Sword", 24 * 48, 8 * 48, 100));
        itemStates.put(2, new ItemState(2, "Sword", 24 * 48, 10 * 48, 100));
        itemStates.put(3, new ItemState(3, "Key", 20 * 48, 15 * 48, 25));
        itemStates.put(4, new ItemState(4, "Door", 25 * 48, 15 * 48, 50));
        itemStates.put(5, new ItemState(5, "Shield", 30 * 48, 10 * 48, 100));
    }

    private PlayerSession createPlayerSession(Socket socket, PlayerState state) throws IOException {
        PlayerSession session = new PlayerSession(socket, state);
        byte[] handshakeData = encoder.encodeHandshake(state.getId(), state.getStats());
        session.getRawOut().write(handshakeData);
        session.getRawOut().flush();
        sendWorldState(session);
        sendInitialItems(session);
        broadcastPlayerJoin(state.getId(), state.getX(), state.getY(), state.getDirection(), state.getStats());
        return session;
    }

    private void sendWorldState(PlayerSession session) throws IOException {
        List<GamePacket.PlayerData> snapshot = new ArrayList<>();
        for (PlayerState ps : playerStates.values()) {
            GamePacket.PlayerData data = new GamePacket.PlayerData();
            data.setId(ps.getId());
            data.setX(ps.getX());
            data.setY(ps.getY());
            data.setDirection(GameProtocol.directionToByte(ps.getDirection()));
            if (ps.isDead()) {
                data.setHealth(0);
            } else {
                data.setHealth(ps.getStats().getHealth());
            }
            data.setMaxHealth(ps.getStats().getMaxHealth());
            data.setLevel(ps.getStats().getLevel());
            data.setDamage(ps.getStats().getDamage());
            data.setExperience(ps.getStats().getExperience());
            data.setExperienceToNextLevel(ps.getStats().getExperienceToNextLevel());
            data.setSpriteNum(ps.getLastSpriteNum());
            data.setDead(ps.isDead());
            snapshot.add(data);
        }
        byte[] worldData = encoder.encodeWorldState(snapshot);
        session.sendRaw(worldData);
    }

    private void sendInitialItems(PlayerSession session) throws IOException {
        for (ItemState item : itemStates.values()) {
            if (!item.isCollected()) {
                byte[] itemPacket = encoder.encodeItemPickup(
                        0,
                        item.getId(),
                        item.getType(),
                        item.getX(),
                        item.getY(),
                        item.getExperienceReward()
                );
                session.sendRaw(itemPacket);
            }
        }
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Сервер запущен на порту: " + PORT);
            threadPool = Executors.newCachedThreadPool();
            new Thread(this::snapshotBroadcastLoop).start();
            while (running) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(() -> handleClient(clientSocket));
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Ошибка сервера на порту " + PORT + ": " + e.getMessage());
            }
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
                session = createPlayerSession(socket, state);
                sessions.put(Integer.valueOf(playerId), session);
            }
            PacketDecoder decoder = new PacketDecoder();
            byte[] buffer = new byte[4096];
            while (session.isConnected()) {
                int bytesRead = session.getRawIn().read(buffer);
                if (bytesRead == -1) break;
                session.getMessageBuffer().write(buffer, 0, bytesRead);
                byte[] receivedData = session.getMessageBuffer().toByteArray();
                PacketDecoder.DecodeResult result = decoder.decode(receivedData, receivedData.length);
                for (GamePacket packet : result.packets()) {
                    processPacket(session, packet);
                }
                if (result.bytesProcessed() > 0) {
                    int remaining = receivedData.length - result.bytesProcessed();
                    if (remaining > 0) {
                        byte[] newBuffer = new byte[remaining];
                        System.arraycopy(receivedData, result.bytesProcessed(), newBuffer, 0, remaining);
                        session.getMessageBuffer().reset();
                        session.getMessageBuffer().write(newBuffer);
                    } else {
                        session.getMessageBuffer().reset();
                    }
                }
            }
        } catch (IOException e) {
        }  finally {
            if (session != null) {
                session.disconnect();
                if (state != null) {
                    sessions.remove(state.getId());
                    playerStates.remove(state.getId());
                    broadcastPlayerLeave(state.getId());
                }
            }
        }
    }

    private void broadcastPlayerJoin(int playerId, int x, int y, String direction, PlayerStats stats) {
        try {
            byte[] joinPacket = encoder.encodePlayerJoin(playerId, x, y, direction, stats);
            for (PlayerSession s : sessions.values()) {
                if (s.getState().getId() != playerId) {
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
        PlayerState state = session.getState();
        if (state.isDead() && packet.getType() == GameProtocol.TYPE_PLAYER_UPDATE) {
            return;
        }
        switch (packet.getType()) {
            case GameProtocol.TYPE_PLAYER_UPDATE:
                if (packet.hasFlag(GameProtocol.FLAG_POSITION)) {
                    int x = packet.getX();
                    int y = packet.getY();
                    if (GameProtocol.validateCoordinates(x, y)) {
                        state.setX(x);
                        state.setY(y);
                    }
                }
                if (packet.hasFlag(GameProtocol.FLAG_DIRECTION)) {
                    state.setDirection(GameProtocol.byteToDirection(packet.getDirection()));
                }
                if (packet.hasFlag(GameProtocol.FLAG_SPRITE_NUM)) {
                    state.setLastSpriteNum(packet.getSpriteNum());
                }
                state.setLastUpdateTime(System.currentTimeMillis());
                broadcastPlayerUpdate(state.getId(), state.getX(), state.getY(), state.getDirection(), state.getLastSpriteNum());
                break;
            case GameProtocol.TYPE_ATTACK:
                if (!state.isDead()) {
                    handleAttack(state, packet);
                }
                break;
            case GameProtocol.TYPE_ITEM_PICKUP:
                handleItemPickup(state, packet);
                break;
        }
    }

    private void broadcastPlayerUpdate(int playerId, int x, int y, String direction, byte spriteNum) {
        try {
            byte[] updatePacket = encoder.encodePlayerUpdate(playerId, x, y, direction, spriteNum);
            for (PlayerSession s : sessions.values()) {
                if (s.getState().getId() != playerId) {
                    s.sendRaw(updatePacket);
                }
            }
        } catch (IOException e) {
        }
    }

    private void handleItemPickup(PlayerState player, GamePacket packet) throws IOException {
        int itemId = packet.getItemId();
        String itemType = packet.getItemType();
        ItemState item = itemStates.get(itemId);

        if (item != null && !item.isCollected()) {
            boolean canPickup = true;

            if (item.getType().equals("Door") && player.getKeys() <= 0) {
                canPickup = false;
                System.out.println("Игрок " + player.getId() + " пытается открыть дверь без ключа");
                return;
            }

            if (!canPickup) {
                return;
            }

            item.setCollected(true);

            switch (item.getType()) {
                case "Sword":
                    player.setSwords(player.getSwords() + 1);
                    player.getStats().setDamage(player.getStats().getDamage() + 15);
                    break;
                case "Key":
                    player.setKeys(player.getKeys() + 1);
                    break;
                case "Door":
                    if (player.getKeys() > 0) {
                        player.setKeys(player.getKeys() - 1);
                    }
                    break;
                case "Shield":
                    player.setShields(player.getShields() + 1);
                    player.getStats().setHealth(player.getStats().getHealth() + 25);
                    player.getStats().setMaxHealth(player.getStats().getMaxHealth() + 25);
                    break;
            }

            player.getStats().addExperienceWithLevelCheck(item.getExperienceReward());

            byte[] itemRemovePacket = encoder.encodeItemRemove(itemId);

            byte[] itemPickupPacket = encoder.encodeItemPickup(
                    player.getId(), itemId, item.getType(), item.getX(), item.getY(), item.getExperienceReward()
            );

            byte[] experiencePacket = encoder.encodePlayerExperience(
                    player.getId(),
                    player.getStats().getExperience(),
                    player.getStats().getExperienceToNextLevel(),
                    player.getStats().getLevel()
            );

            for (PlayerSession session : sessions.values()) {
                if (session.isConnected()) {
                    session.sendRaw(itemRemovePacket);

                    if (session.getState().getId() == player.getId()) {
                        session.sendRaw(itemPickupPacket);
                        session.sendRaw(experiencePacket);
                    }
                }
            }

            System.out.println("Игрок " + player.getId() + " подобрал " + item.getType() +
                    " (ID: " + itemId + "), опыт: " + item.getExperienceReward());
        }
    }

    private void handleAttack(PlayerState attacker, GamePacket packet) throws IOException {
        byte[] attackPacket = encoder.encodeAttack(
                attacker.getId(),
                GameProtocol.byteToDirection(packet.getDirection()),
                attacker.getX(),
                attacker.getY()
        );

        for (PlayerSession session : sessions.values()) {
            if (session.isConnected() && session.getState() != null) {
                try {
                    session.sendRaw(attackPacket);
                } catch (IOException e) {
                }
            }
        }

        for (PlayerState target : playerStates.values()) {
            if (target.getId() == attacker.getId() || target.isDead() || !target.getStats().isAlive()) {
                continue;
            }

            int attackX = attacker.getX() + 24;
            int attackY = attacker.getY() + 24;
            Rectangle attackZone = calculateAttackZone(attackX, attackY,
                    GameProtocol.byteToDirection(packet.getDirection()));
            Rectangle targetBounds = new Rectangle(target.getX(), target.getY(), 48, 48);

            if (attackZone.intersects(targetBounds)) {
                int damage = attacker.getStats().getDamage();
                target.takeDamage(damage);

                byte[] damagePacket = encoder.encodePlayerDamage(
                        attacker.getId(),
                        target.getId(),
                        damage,
                        target.isDead() ? 0 : target.getStats().getHealth(),
                        target.getStats().getMaxHealth(),
                        target.getStats().getLevel()
                );

                for (PlayerSession session : sessions.values()) {
                    if (session.isConnected() && session.getState() != null) {
                        try {
                            session.sendRaw(damagePacket);
                        } catch (IOException e) {
                        }
                    }
                }

                if (!target.isAlive()) {
                    attacker.addExperienceForKill(target.getStats().getLevel());

                    byte[] deathPacket = encoder.encodePlayerDeath(target.getId(), attacker.getId());
                    for (PlayerSession session : sessions.values()) {
                        if (session.isConnected() && session.getState() != null) {
                            try {
                                session.sendRaw(deathPacket);
                            } catch (IOException e) {
                            }
                        }
                    }

                    sendWorldStateToAll();
                    handlePlayerDeath(target.getId(), attacker.getId());

                    byte[] experiencePacket = encoder.encodePlayerExperience(
                            attacker.getId(),
                            attacker.getStats().getExperience(),
                            attacker.getStats().getExperienceToNextLevel(),
                            attacker.getStats().getLevel()
                    );

                    for (PlayerSession session : sessions.values()) {
                        if (session.isConnected()) {
                            session.sendRaw(experiencePacket);
                        }
                    }
                }

                int[] push = calculatePush(target.getX(), target.getY(), attacker.getX(), attacker.getY(),
                        GameProtocol.byteToDirection(packet.getDirection()));
                target.setX(push[0]);
                target.setY(push[1]);
                target.setLastUpdateTime(System.currentTimeMillis());

                byte[] pushPacket = encoder.encodePlayerHit(
                        attacker.getId(),
                        target.getId(),
                        push[0],
                        push[1]
                );

                PlayerSession targetSession = sessions.get(target.getId());
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
                        if (deadSession.isConnected()) {
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
            data.setId(state.getId());
            data.setX(state.getX());
            data.setY(state.getY());
            data.setDirection(GameProtocol.directionToByte(state.getDirection()));
            if (state.isDead()) {
                data.setHealth(0);
            } else {
                data.setHealth(state.getStats().getHealth());
            }
            data.setMaxHealth(state.getStats().getMaxHealth());
            data.setLevel(state.getStats().getLevel());
            data.setDamage(state.getStats().getDamage());
            data.setExperience(state.getStats().getExperience());
            data.setExperienceToNextLevel(state.getStats().getExperienceToNextLevel());
            data.setSpriteNum(state.getLastSpriteNum());
            data.setDead(state.isDead());
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
        while (running) {
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

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (threadPool != null) {
            threadPool.shutdownNow();
        }

        System.out.println("Сервер на порту " + PORT + " остановлен");
    }
}