package ru.itis.java.app.network.protocol;

public class GameProtocol {
    public static final byte PACKET_START = (byte) 0xFF;
    public static final byte PACKET_END = (byte) 0x00;
    public static final byte ESCAPE_CHAR = (byte) 0x7D;
    public static final byte ESCAPED_START = (byte) 0xFE;
    public static final byte ESCAPED_END = (byte) 0x01;
    public static final byte ESCAPED_ESCAPE = (byte) 0x02;

    public static final byte TYPE_HANDSHAKE = 0x01;
    public static final byte TYPE_PLAYER_UPDATE = 0x02;
    public static final byte TYPE_ATTACK = 0x03;
    public static final byte TYPE_PLAYER_HIT = 0x04;
    public static final byte TYPE_WORLD_STATE = 0x05;
    public static final byte TYPE_PLAYER_JOIN = 0x06;
    public static final byte TYPE_PLAYER_LEAVE = 0x07;
    public static final byte TYPE_PLAYER_DAMAGE = 0x08;
    public static final byte TYPE_PLAYER_DEATH = 0x09;

    public static final byte FLAG_POSITION = 0x01;
    public static final byte FLAG_DIRECTION = 0x02;
    public static final byte FLAG_HEALTH_EXTENDED = 0x08;
    public static final byte FLAG_LEVEL = 0x10;
    public static final byte FLAG_MAX_HEALTH = 0x20;
    public static final byte FLAG_SPRITE_NUM = 0x40;

    public static final byte DIR_UP = 0;
    public static final byte DIR_DOWN = 1;
    public static final byte DIR_LEFT = 2;
    public static final byte DIR_RIGHT = 3;
    public static final int MAX_PLAYERS = 100;
    public static final int MAX_LEVEL = 100;

    public static final int HANDSHAKE_SIZE = 12;
    public static final int MIN_PLAYER_UPDATE_SIZE = 6;
    public static final int ATTACK_SIZE = 11;
    public static final int PLAYER_HIT_SIZE = 12;
    public static final int PLAYER_JOIN_SIZE = 17;
    public static final int PLAYER_LEAVE_SIZE = 6;
    public static final int PLAYER_DAMAGE_SIZE = 14;
    public static final int PLAYER_DEATH_SIZE = 6;
    public static final int ATTACK_RANGE = 58;

    public static final int MIN_X = 0;
    public static final int MAX_X = 4800 - 48;
    public static final int MIN_Y = 0;
    public static final int MAX_Y = 3600 - 48;

    public static byte directionToByte(String direction) {
        if (direction == null) return DIR_DOWN;
        switch (direction.toLowerCase()) {
            case "up": return DIR_UP;
            case "down": return DIR_DOWN;
            case "left": return DIR_LEFT;
            case "right": return DIR_RIGHT;
            default: return DIR_DOWN;
        }
    }

    public static String byteToDirection(byte directionByte) {
        switch (directionByte) {
            case DIR_UP: return "up";
            case DIR_DOWN: return "down";
            case DIR_LEFT: return "left";
            case DIR_RIGHT: return "right";
            default: return "down";
        }
    }

    public static boolean isValidPacket(byte[] data, int offset, int length) {
        if (data == null || offset < 0 || offset >= data.length) {
            return false;
        }
        if (length < 4) {
            return false;
        }
        if (data[offset] != PACKET_START) {
            return false;
        }
        if (data[offset + length - 1] != PACKET_END) {
            return false;
        }
        byte type = data[offset + 1];
        boolean typeValid = type >= TYPE_HANDSHAKE && type <= TYPE_PLAYER_DEATH;
        return typeValid;
    }

    public static boolean validateCoordinates(int x, int y) {
        return x >= MIN_X && x <= MAX_X && y >= MIN_Y && y <= MAX_Y;
    }
}