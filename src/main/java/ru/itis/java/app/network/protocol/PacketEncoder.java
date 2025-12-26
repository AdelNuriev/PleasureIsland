package ru.itis.java.app.network.protocol;

import ru.itis.java.app.entity.PlayerStats;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

    public byte[] encodeItemPickup(int playerId, int itemId, String itemType, int itemX, int itemY, int experienceGained)
            throws IOException {
        resetStream();
        dataStream.writeByte(GameProtocol.PACKET_START);
        dataStream.writeByte(GameProtocol.TYPE_ITEM_PICKUP);
        dataStream.writeByte(GameProtocol.FLAG_ITEM_PICKUP);
        dataStream.writeShort(playerId);
        dataStream.writeShort(itemId);
        byte[] itemTypeBytes = itemType.getBytes("UTF-8");
        dataStream.writeByte(Math.min(itemTypeBytes.length, 20));
        dataStream.write(itemTypeBytes, 0, Math.min(itemTypeBytes.length, 20));
        dataStream.writeShort(itemX);
        dataStream.writeShort(itemY);
        dataStream.writeShort(experienceGained);
        dataStream.writeByte(GameProtocol.PACKET_END);
        byte[] rawPacket = byteStream.toByteArray();
        byte[] escapedData = escapePacketData(rawPacket);
        return escapedData;
    }

    public byte[] encodeItemRemove(int itemId) throws IOException {
        resetStream();
        dataStream.writeByte(GameProtocol.PACKET_START);
        dataStream.writeByte(GameProtocol.TYPE_ITEM_REMOVE);
        dataStream.writeByte(0);
        dataStream.writeShort(itemId);
        dataStream.writeByte(GameProtocol.PACKET_END);
        byte[] rawPacket = byteStream.toByteArray();
        byte[] escapedData = escapePacketData(rawPacket);
        return escapedData;
    }

    public byte[] encodePlayerExperience(int playerId, int experience, int totalExperience, int level)
            throws IOException {
        resetStream();
        dataStream.writeByte(GameProtocol.PACKET_START);
        dataStream.writeByte(GameProtocol.TYPE_PLAYER_EXPERIENCE);
        dataStream.writeByte(GameProtocol.FLAG_EXPERIENCE_UPDATE | GameProtocol.FLAG_LEVEL);
        dataStream.writeShort(playerId);
        dataStream.writeShort(experience);
        dataStream.writeShort(totalExperience);
        dataStream.writeByte(level);
        dataStream.writeByte(GameProtocol.PACKET_END);
        byte[] rawPacket = byteStream.toByteArray();
        byte[] escapedData = escapePacketData(rawPacket);
        return escapedData;
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

    private static class ByteArrayOutputStream extends OutputStream {
        private byte[] buffer;
        private int size;

        public ByteArrayOutputStream() {
            this(32);
        }

        public ByteArrayOutputStream(int initialCapacity) {
            buffer = new byte[initialCapacity];
            size = 0;
        }

        public void write(int b) {
            ensureCapacity(size + 1);
            buffer[size++] = (byte) b;
        }

        public void write(byte b) {
            ensureCapacity(size + 1);
            buffer[size++] = b;
        }

        private void ensureCapacity(int minCapacity) {
            if (minCapacity > buffer.length) {
                int newCapacity = Math.max(buffer.length * 2, minCapacity);
                byte[] newBuffer = new byte[newCapacity];
                System.arraycopy(buffer, 0, newBuffer, 0, size);
                buffer = newBuffer;
            }
        }

        public byte[] toByteArray() {
            byte[] result = new byte[size];
            System.arraycopy(buffer, 0, result, 0, size);
            return result;
        }

        public void reset() {
            size = 0;
        }
    }
}