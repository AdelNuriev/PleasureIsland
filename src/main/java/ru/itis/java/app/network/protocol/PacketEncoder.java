package ru.itis.java.app.network.protocol;

import ru.itis.java.app.entity.PlayerStats;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class PacketEncoder {
    private final ByteArrayOutputStream byteStream;
    private final DataOutputStream dataStream;

    public PacketEncoder() {
        this.byteStream = new ByteArrayOutputStream();
        this.dataStream = new DataOutputStream(byteStream);
    }

    public byte[] encodeHandshake(int playerId, PlayerStats stats) throws IOException {
        resetStream();
        dataStream.writeByte(GameProtocol.PACKET_START);
        dataStream.writeByte(GameProtocol.TYPE_HANDSHAKE);
        dataStream.writeByte(GameProtocol.FLAG_HEALTH_EXTENDED | GameProtocol.FLAG_LEVEL | GameProtocol.FLAG_MAX_HEALTH);
        dataStream.writeShort(playerId);
        dataStream.writeShort(stats.getHealth());
        dataStream.writeShort(stats.getMaxHealth());
        dataStream.writeShort(stats.getDamage());
        dataStream.writeByte(stats.getLevel());
        dataStream.writeShort(stats.getExperience());
        dataStream.writeShort(stats.getExperienceToNextLevel());
        dataStream.writeByte(GameProtocol.PACKET_END);
        byte[] rawPacket = byteStream.toByteArray();
        byte[] escapedData = escapePacketData(rawPacket);
        return escapedData;
    }

    public byte[] encodePlayerUpdate(int playerId, Integer x, Integer y, String direction, Byte spriteNum)
            throws IOException {
        resetStream();
        byte flags = 0;
        if (x != null && y != null) {
            flags |= GameProtocol.FLAG_POSITION;
            if (!GameProtocol.validateCoordinates(x, y)) {
                throw new IllegalArgumentException("Invalid coordinates: x=" + x + ", y=" + y);
            }
        }
        if (direction != null) {
            flags |= GameProtocol.FLAG_DIRECTION;
        }
        if (spriteNum != null && spriteNum > 0) {
            flags |= GameProtocol.FLAG_SPRITE_NUM;
        }
        dataStream.writeByte(GameProtocol.PACKET_START);
        dataStream.writeByte(GameProtocol.TYPE_PLAYER_UPDATE);
        dataStream.writeByte(flags);
        dataStream.writeShort(playerId);
        if ((flags & GameProtocol.FLAG_POSITION) != 0) {
            dataStream.writeShort(x);
            dataStream.writeShort(y);
        }
        if ((flags & GameProtocol.FLAG_DIRECTION) != 0) {
            dataStream.writeByte(GameProtocol.directionToByte(direction));
        }
        if ((flags & GameProtocol.FLAG_SPRITE_NUM) != 0) {
            dataStream.writeByte(spriteNum);
        }
        dataStream.writeByte(GameProtocol.PACKET_END);
        byte[] rawPacket = byteStream.toByteArray();
        byte[] escapedData = escapePacketData(rawPacket);
        return escapedData;
    }

    public byte[] encodeAttack(int playerId, String direction, int x, int y)
            throws IOException {
        resetStream();
        if (!GameProtocol.validateCoordinates(x, y)) {
            throw new IllegalArgumentException("Invalid attack coordinates: x=" + x + ", y=" + y);
        }
        dataStream.writeByte(GameProtocol.PACKET_START);
        dataStream.writeByte(GameProtocol.TYPE_ATTACK);
        dataStream.writeByte(0);
        dataStream.writeShort(playerId);
        dataStream.writeByte(GameProtocol.directionToByte(direction));
        dataStream.writeShort(x);
        dataStream.writeShort(y);
        dataStream.writeByte(GameProtocol.PACKET_END);
        byte[] rawPacket = byteStream.toByteArray();
        byte[] escapedData = escapePacketData(rawPacket);
        return escapedData;
    }

    public byte[] encodePlayerHit(int attackerId, int targetId, int pushX, int pushY)
            throws IOException {
        resetStream();
        if (!GameProtocol.validateCoordinates(pushX, pushY)) {
            throw new IllegalArgumentException("Invalid push coordinates: x=" + pushX + ", y=" + pushY);
        }
        dataStream.writeByte(GameProtocol.PACKET_START);
        dataStream.writeByte(GameProtocol.TYPE_PLAYER_HIT);
        dataStream.writeByte(0);
        dataStream.writeShort(attackerId);
        dataStream.writeShort(targetId);
        dataStream.writeShort(pushX);
        dataStream.writeShort(pushY);
        dataStream.writeByte(GameProtocol.PACKET_END);
        byte[] rawPacket = byteStream.toByteArray();
        byte[] escapedData = escapePacketData(rawPacket);
        return escapedData;
    }

    public byte[] encodePlayerDamage(int attackerId, int targetId, int damage, int targetHealth, int targetMaxHealth, int targetLevel)
            throws IOException {
        resetStream();
        dataStream.writeByte(GameProtocol.PACKET_START);
        dataStream.writeByte(GameProtocol.TYPE_PLAYER_DAMAGE);
        dataStream.writeByte(GameProtocol.FLAG_HEALTH_EXTENDED | GameProtocol.FLAG_MAX_HEALTH | GameProtocol.FLAG_LEVEL);
        dataStream.writeShort(attackerId);
        dataStream.writeShort(targetId);
        dataStream.writeShort(damage);
        dataStream.writeShort(targetHealth);
        dataStream.writeShort(targetMaxHealth);
        dataStream.writeByte(targetLevel);
        dataStream.writeByte(GameProtocol.PACKET_END);
        byte[] rawPacket = byteStream.toByteArray();
        byte[] escapedData = escapePacketData(rawPacket);
        return escapedData;
    }

    public byte[] encodePlayerDeath(int playerId, int killerId) throws IOException {
        resetStream();
        dataStream.writeByte(GameProtocol.PACKET_START);
        dataStream.writeByte(GameProtocol.TYPE_PLAYER_DEATH);
        dataStream.writeByte(0);
        dataStream.writeShort(playerId);
        dataStream.writeShort(killerId);
        dataStream.writeByte(GameProtocol.PACKET_END);
        byte[] rawPacket = byteStream.toByteArray();
        byte[] escapedData = escapePacketData(rawPacket);
        return escapedData;
    }

    public byte[] encodeWorldState(List<GamePacket.PlayerData> players) throws IOException {
        resetStream();
        int playerCount = Math.min(players.size(), GameProtocol.MAX_PLAYERS);
        byte flags = GameProtocol.FLAG_HEALTH_EXTENDED | GameProtocol.FLAG_LEVEL |
                GameProtocol.FLAG_MAX_HEALTH | GameProtocol.FLAG_SPRITE_NUM;
        dataStream.writeByte(GameProtocol.PACKET_START);
        dataStream.writeByte(GameProtocol.TYPE_WORLD_STATE);
        dataStream.writeByte(flags);
        dataStream.writeByte(playerCount);
        for (int i = 0; i < playerCount; i++) {
            GamePacket.PlayerData player = players.get(i);
            if (!GameProtocol.validateCoordinates(player.getX(), player.getY())) {
                throw new IllegalArgumentException("Invalid player coordinates for player " + player.getId());
            }
            dataStream.writeShort(player.getId());
            dataStream.writeShort(player.getX());
            dataStream.writeShort(player.getY());
            dataStream.writeByte(player.getDirection());
            dataStream.writeShort(player.getHealth());
            dataStream.writeShort(player.getMaxHealth());
            dataStream.writeByte(player.getLevel());
            dataStream.writeShort(player.getDamage());
            dataStream.writeShort(player.getExperience());
            dataStream.writeShort(player.getExperienceToNextLevel());
            dataStream.writeByte(player.getSpriteNum());
            dataStream.writeByte(player.isDead() ? 1 : 0);
        }
        dataStream.writeByte(GameProtocol.PACKET_END);
        byte[] rawPacket = byteStream.toByteArray();
        byte[] escapedData = escapePacketData(rawPacket);
        return escapedData;
    }

    public byte[] encodePlayerJoin(int playerId, int x, int y, String direction, PlayerStats stats)
            throws IOException {
        resetStream();
        if (!GameProtocol.validateCoordinates(x, y)) {
            throw new IllegalArgumentException("Invalid join coordinates: x=" + x + ", y=" + y);
        }
        dataStream.writeByte(GameProtocol.PACKET_START);
        dataStream.writeByte(GameProtocol.TYPE_PLAYER_JOIN);
        dataStream.writeByte(GameProtocol.FLAG_HEALTH_EXTENDED | GameProtocol.FLAG_LEVEL | GameProtocol.FLAG_MAX_HEALTH);
        dataStream.writeShort(playerId);
        dataStream.writeShort(x);
        dataStream.writeShort(y);
        dataStream.writeByte(GameProtocol.directionToByte(direction));
        dataStream.writeShort(stats.getHealth());
        dataStream.writeShort(stats.getMaxHealth());
        dataStream.writeShort(stats.getDamage());
        dataStream.writeByte(stats.getLevel());
        dataStream.writeShort(stats.getExperience());
        dataStream.writeShort(stats.getExperienceToNextLevel());
        dataStream.writeByte(GameProtocol.PACKET_END);
        byte[] rawPacket = byteStream.toByteArray();
        byte[] escapedData = escapePacketData(rawPacket);
        return escapedData;
    }

    public byte[] encodePlayerLeave(int playerId) throws IOException {
        resetStream();
        dataStream.writeByte(GameProtocol.PACKET_START);
        dataStream.writeByte(GameProtocol.TYPE_PLAYER_LEAVE);
        dataStream.writeByte(0);
        dataStream.writeShort(playerId);
        dataStream.writeByte(GameProtocol.PACKET_END);
        byte[] rawPacket = byteStream.toByteArray();
        byte[] escapedData = escapePacketData(rawPacket);
        return escapedData;
    }

    public byte[] fastEncodePlayerUpdate(int playerId, int x, int y, byte direction, byte spriteNum) {
        if (!GameProtocol.validateCoordinates(x, y)) {
            return null;
        }
        resetStream();
        try {
            byte flags = GameProtocol.FLAG_POSITION | GameProtocol.FLAG_DIRECTION | GameProtocol.FLAG_SPRITE_NUM;
            dataStream.writeByte(GameProtocol.PACKET_START);
            dataStream.writeByte(GameProtocol.TYPE_PLAYER_UPDATE);
            dataStream.writeByte(flags);
            dataStream.writeShort(playerId);
            dataStream.writeShort(x);
            dataStream.writeShort(y);
            dataStream.writeByte(direction);
            dataStream.writeByte(spriteNum);
            dataStream.writeByte(GameProtocol.PACKET_END);
        } catch (IOException e) {
            return null;
        }
        byte[] rawPacket = byteStream.toByteArray();
        byte[] escapedData = escapePacketData(rawPacket);
        return escapedData;
    }

    public byte[] encode(GamePacket packet) throws IOException {
        if (packet == null) {
            throw new IllegalArgumentException("Packet cannot be null");
        }
        byte type = packet.getType();
        if (type < GameProtocol.TYPE_HANDSHAKE || type > GameProtocol.TYPE_PLAYER_DEATH) {
            throw new IOException("Invalid packet type: " + type);
        }
        switch (type) {
            case GameProtocol.TYPE_HANDSHAKE:
                return encodeHandshake(packet.getPlayerId(),
                        new PlayerStats(packet.getLevel(), packet.getHealth(),
                                packet.getMaxHealth(), packet.getDamage(), 0));
            case GameProtocol.TYPE_PLAYER_UPDATE:
                Integer x = packet.hasFlag(GameProtocol.FLAG_POSITION) ? packet.getX() : null;
                Integer y = packet.hasFlag(GameProtocol.FLAG_POSITION) ? packet.getY() : null;
                String dir = packet.hasFlag(GameProtocol.FLAG_DIRECTION) ?
                        GameProtocol.byteToDirection(packet.getDirection()) : null;
                Byte sprite = packet.hasFlag(GameProtocol.FLAG_SPRITE_NUM) ? packet.getSpriteNum() : null;
                return encodePlayerUpdate(packet.getPlayerId(), x, y, dir, sprite);
            case GameProtocol.TYPE_ATTACK:
                return encodeAttack(
                        packet.getPlayerId(),
                        GameProtocol.byteToDirection(packet.getDirection()),
                        packet.getAttackX(),
                        packet.getAttackY()
                );
            case GameProtocol.TYPE_PLAYER_HIT:
                return encodePlayerHit(
                        packet.getAttackerId(),
                        packet.getTargetId(),
                        packet.getPushX(),
                        packet.getPushY()
                );
            case GameProtocol.TYPE_WORLD_STATE:
                return encodeWorldState(packet.getPlayersData());
            case GameProtocol.TYPE_PLAYER_JOIN:
                return encodePlayerJoin(
                        packet.getPlayerId(),
                        packet.getX(),
                        packet.getY(),
                        GameProtocol.byteToDirection(packet.getDirection()),
                        new PlayerStats(packet.getLevel(), packet.getHealth(),
                                packet.getMaxHealth(), packet.getDamage(), 0)
                );
            case GameProtocol.TYPE_PLAYER_LEAVE:
                return encodePlayerLeave(packet.getPlayerId());
            case GameProtocol.TYPE_PLAYER_DAMAGE:
                return encodePlayerDamage(
                        packet.getAttackerId(),
                        packet.getTargetId(),
                        packet.getDamage(),
                        packet.getHealth(),
                        packet.getMaxHealth(),
                        packet.getLevel()
                );
            case GameProtocol.TYPE_PLAYER_DEATH:
                return encodePlayerDeath(
                        packet.getPlayerId(),
                        packet.getAttackerId()
                );
            default:
                throw new IOException("Unknown packet type: " + packet.getType());
        }
    }

    private byte[] escapePacketData(byte[] rawPacket) {
        if (rawPacket.length < 2) {
            return rawPacket;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(rawPacket[0]);
        for (int i = 1; i < rawPacket.length - 1; i++) {
            byte b = rawPacket[i];
            if (b == GameProtocol.PACKET_START) {
                output.write(GameProtocol.ESCAPE_CHAR);
                output.write(GameProtocol.ESCAPED_START);
            } else if (b == GameProtocol.PACKET_END) {
                output.write(GameProtocol.ESCAPE_CHAR);
                output.write(GameProtocol.ESCAPED_END);
            } else if (b == GameProtocol.ESCAPE_CHAR) {
                output.write(GameProtocol.ESCAPE_CHAR);
                output.write(GameProtocol.ESCAPED_ESCAPE);
            } else {
                output.write(b);
            }
        }
        output.write(rawPacket[rawPacket.length - 1]);
        return output.toByteArray();
    }

    private void resetStream() {
        byteStream.reset();
    }

    public void close() {
        try {
            dataStream.close();
            byteStream.close();
        } catch (IOException e) {
        }
    }
}