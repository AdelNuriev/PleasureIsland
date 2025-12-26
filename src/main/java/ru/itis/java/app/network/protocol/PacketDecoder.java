package ru.itis.java.app.network.protocol;

import java.util.ArrayList;
import java.util.List;

public class PacketDecoder {
    private byte[] incompleteBuffer = new byte[0];

    public record DecodeResult(List<GamePacket> packets, int bytesProcessed, boolean hasMoreData) {
    }

    private static class ByteArrayOutputStream {
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
    }

    public DecodeResult decode(byte[] data, int length) {
        List<GamePacket> packets = new ArrayList<>();
        if (data == null || length <= 0) {
            return new DecodeResult(packets, 0, false);
        }
        byte[] combinedData;
        int combinedLength = length;
        if (incompleteBuffer.length > 0) {
            combinedData = new byte[incompleteBuffer.length + length];
            System.arraycopy(incompleteBuffer, 0, combinedData, 0, incompleteBuffer.length);
            System.arraycopy(data, 0, combinedData, incompleteBuffer.length, length);
            combinedLength = combinedData.length;
            data = combinedData;
        }
        int i = 0;
        int bytesProcessed = 0;
        boolean hasMoreData = false;
        while (i < combinedLength) {
            int packetStart = findPacketStart(data, i, combinedLength);
            if (packetStart == -1) {
                saveIncompleteData(data, i, combinedLength - i);
                hasMoreData = true;
                break;
            }
            i = packetStart;
            int packetEnd = findPacketEnd(data, i + 1, combinedLength);
            if (packetEnd == -1) {
                saveIncompleteData(data, i, combinedLength - i);
                hasMoreData = true;
                break;
            }
            int packetLength = packetEnd - i + 1;
            byte[] rawPacket = unescapePacket(data, i, packetLength);
            if (rawPacket != null && GameProtocol.isValidPacket(rawPacket, 0, rawPacket.length)) {
                GamePacket packet = parseRawPacket(rawPacket);
                if (packet != null) {
                    packets.add(packet);
                }
            }
            i = packetEnd + 1;
            bytesProcessed = i;
        }
        if (i >= combinedLength && !hasMoreData) {
            incompleteBuffer = new byte[0];
        } else if (bytesProcessed < combinedLength) {
            saveIncompleteData(data, bytesProcessed, combinedLength - bytesProcessed);
        }
        return new DecodeResult(packets, bytesProcessed, hasMoreData);
    }

    private int findPacketStart(byte[] data, int start, int length) {
        for (int i = start; i < length; i++) {
            if (data[i] == GameProtocol.PACKET_START) {
                return i;
            }
        }
        return -1;
    }

    private int findPacketEnd(byte[] data, int start, int length) {
        for (int i = start; i < length; i++) {
            if (data[i] == GameProtocol.PACKET_END) {
                if (i > start && data[i - 1] == GameProtocol.ESCAPE_CHAR) {
                    continue;
                }
                return i;
            }
        }
        return -1;
    }

    private byte[] unescapePacket(byte[] escapedData, int offset, int length) {
        byte[] result = new byte[length];
        System.arraycopy(escapedData, offset, result, 0, length);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean escaping = false;
        for (int i = offset; i < offset + length; i++) {
            byte b = escapedData[i];
            if (escaping) {
                if (b == GameProtocol.ESCAPED_START) {
                    output.write(GameProtocol.PACKET_START);
                } else if (b == GameProtocol.ESCAPED_END) {
                    output.write(GameProtocol.PACKET_END);
                } else if (b == GameProtocol.ESCAPED_ESCAPE) {
                    output.write(GameProtocol.ESCAPE_CHAR);
                } else {
                    output.write(GameProtocol.ESCAPE_CHAR);
                    output.write(b);
                }
                escaping = false;
            } else if (b == GameProtocol.ESCAPE_CHAR) {
                escaping = true;
            } else {
                output.write(b);
            }
        }
        if (escaping) {
            output.write(GameProtocol.ESCAPE_CHAR);
        }
        return output.toByteArray();
    }

    private void saveIncompleteData(byte[] data, int start, int length) {
        incompleteBuffer = new byte[length];
        System.arraycopy(data, start, incompleteBuffer, 0, length);
    }

    private GamePacket parseRawPacket(byte[] rawPacket) {
        if (rawPacket.length < 4) {
            return null;
        }
        byte type = rawPacket[1];
        byte flags = rawPacket[2];
        GamePacket packet = new GamePacket(type);
        packet.setFlags(flags);
        try {
            int pos = 3;
            switch (type) {
                case GameProtocol.TYPE_HANDSHAKE:
                    if (rawPacket.length >= GameProtocol.HANDSHAKE_SIZE) {
                        int playerId = ((rawPacket[pos] & 0xFF) << 8) | (rawPacket[pos + 1] & 0xFF);
                        packet.setPlayerId(playerId);
                        pos += 2;
                        if ((flags & GameProtocol.FLAG_HEALTH_EXTENDED) != 0) {
                            int health = ((rawPacket[pos] & 0xFF) << 8) | (rawPacket[pos + 1] & 0xFF);
                            int maxHealth = ((rawPacket[pos + 2] & 0xFF) << 8) | (rawPacket[pos + 3] & 0xFF);
                            int damage = ((rawPacket[pos + 4] & 0xFF) << 8) | (rawPacket[pos + 5] & 0xFF);
                            packet.setHealth(health);
                            packet.setMaxHealth(maxHealth);
                            packet.setDamage(damage);
                            pos += 6;
                        }
                        if ((flags & GameProtocol.FLAG_LEVEL) != 0) {
                            packet.setLevel(rawPacket[pos] & 0xFF);
                            pos++;
                        }
                    }
                    break;
                case GameProtocol.TYPE_PLAYER_UPDATE:
                    if (rawPacket.length >= GameProtocol.MIN_PLAYER_UPDATE_SIZE) {
                        int playerId = ((rawPacket[pos] & 0xFF) << 8) | (rawPacket[pos + 1] & 0xFF);
                        packet.setPlayerId(playerId);
                        pos += 2;
                        if ((flags & GameProtocol.FLAG_POSITION) != 0 && pos + 3 < rawPacket.length - 1) {
                            int x = ((rawPacket[pos] & 0xFF) << 8) | (rawPacket[pos + 1] & 0xFF);
                            int y = ((rawPacket[pos + 2] & 0xFF) << 8) | (rawPacket[pos + 3] & 0xFF);
                            packet.setX(x);
                            packet.setY(y);
                            pos += 4;
                        }
                        if ((flags & GameProtocol.FLAG_DIRECTION) != 0 && pos < rawPacket.length - 1) {
                            packet.setDirection(rawPacket[pos]);
                            pos++;
                        }
                        if ((flags & GameProtocol.FLAG_SPRITE_NUM) != 0 && pos < rawPacket.length - 1) {
                            packet.setSpriteNum(rawPacket[pos]);
                        }
                    }
                    break;
                case GameProtocol.TYPE_ATTACK:
                    if (rawPacket.length >= GameProtocol.ATTACK_SIZE) {
                        int playerId = ((rawPacket[pos] & 0xFF) << 8) | (rawPacket[pos + 1] & 0xFF);
                        packet.setPlayerId(playerId);
                        packet.setDirection(rawPacket[pos + 2]);
                        packet.setAttackX(((rawPacket[pos + 3] & 0xFF) << 8) | (rawPacket[pos + 4] & 0xFF));
                        packet.setAttackY(((rawPacket[pos + 5] & 0xFF) << 8) | (rawPacket[pos + 6] & 0xFF));
                    }
                    break;
                case GameProtocol.TYPE_PLAYER_HIT:
                    if (rawPacket.length >= GameProtocol.PLAYER_HIT_SIZE) {
                        packet.setAttackerId(((rawPacket[pos] & 0xFF) << 8) | (rawPacket[pos + 1] & 0xFF));
                        packet.setTargetId(((rawPacket[pos + 2] & 0xFF) << 8) | (rawPacket[pos + 3] & 0xFF));
                        packet.setPushX(((rawPacket[pos + 4] & 0xFF) << 8) | (rawPacket[pos + 5] & 0xFF));
                        packet.setPushY(((rawPacket[pos + 6] & 0xFF) << 8) | (rawPacket[pos + 7] & 0xFF));
                    }
                    break;
                case GameProtocol.TYPE_PLAYER_DAMAGE:
                    if (rawPacket.length >= GameProtocol.PLAYER_DAMAGE_SIZE) {
                        packet.setAttackerId(((rawPacket[pos] & 0xFF) << 8) | (rawPacket[pos + 1] & 0xFF));
                        packet.setTargetId(((rawPacket[pos + 2] & 0xFF) << 8) | (rawPacket[pos + 3] & 0xFF));
                        if ((flags & GameProtocol.FLAG_HEALTH_EXTENDED) != 0) {
                            packet.setDamage(((rawPacket[pos + 4] & 0xFF) << 8) | (rawPacket[pos + 5] & 0xFF));
                            packet.setHealth(((rawPacket[pos + 6] & 0xFF) << 8) | (rawPacket[pos + 7] & 0xFF));
                            packet.setMaxHealth(((rawPacket[pos + 8] & 0xFF) << 8) | (rawPacket[pos + 9] & 0xFF));
                            packet.setLevel(rawPacket[pos + 10] & 0xFF);
                        } else {
                            packet.setDamage(rawPacket[pos + 4] & 0xFF);
                            packet.setHealth(((rawPacket[pos + 5] & 0xFF) << 8) | (rawPacket[pos + 6] & 0xFF));
                        }
                    }
                    break;
                case GameProtocol.TYPE_PLAYER_DEATH:
                    if (rawPacket.length >= GameProtocol.PLAYER_DEATH_SIZE) {
                        packet.setPlayerId(((rawPacket[pos] & 0xFF) << 8) | (rawPacket[pos + 1] & 0xFF));
                        packet.setAttackerId(((rawPacket[pos + 2] & 0xFF) << 8) | (rawPacket[pos + 3] & 0xFF));
                    }
                    break;
                case GameProtocol.TYPE_ITEM_PICKUP:
                    if (rawPacket.length >= GameProtocol.ITEM_PICKUP_SIZE) {
                        int playerId = ((rawPacket[pos] & 0xFF) << 8) | (rawPacket[pos + 1] & 0xFF);
                        int itemId = ((rawPacket[pos + 2] & 0xFF) << 8) | (rawPacket[pos + 3] & 0xFF);
                        packet.setPlayerId(playerId);
                        packet.setItemId(itemId);
                        pos += 4;

                        int typeLength = rawPacket[pos] & 0xFF;
                        pos++;
                        if (typeLength > 0 && pos + typeLength < rawPacket.length - 1) {
                            byte[] typeBytes = new byte[typeLength];
                            System.arraycopy(rawPacket, pos, typeBytes, 0, typeLength);
                            packet.setItemType(new String(typeBytes, "UTF-8"));
                            pos += typeLength;
                        }

                        if (pos + 4 < rawPacket.length - 1) {
                            packet.setItemX(((rawPacket[pos] & 0xFF) << 8) | (rawPacket[pos + 1] & 0xFF));
                            packet.setItemY(((rawPacket[pos + 2] & 0xFF) << 8) | (rawPacket[pos + 3] & 0xFF));
                            pos += 4;
                        }

                        if (pos + 1 < rawPacket.length - 1) {
                            packet.setExperienceGained(((rawPacket[pos] & 0xFF) << 8) | (rawPacket[pos + 1] & 0xFF));
                        }
                    }
                    break;
                case GameProtocol.TYPE_ITEM_REMOVE:
                    if (rawPacket.length >= GameProtocol.ITEM_REMOVE_SIZE) {
                        packet.setItemId(((rawPacket[pos] & 0xFF) << 8) | (rawPacket[pos + 1] & 0xFF));
                    }
                    break;
                case GameProtocol.TYPE_PLAYER_EXPERIENCE:
                    if (rawPacket.length >= GameProtocol.PLAYER_EXPERIENCE_SIZE) {
                        packet.setPlayerId(((rawPacket[pos] & 0xFF) << 8) | (rawPacket[pos + 1] & 0xFF));
                        packet.setExperience(((rawPacket[pos + 2] & 0xFF) << 8) | (rawPacket[pos + 3] & 0xFF));
                        packet.setTotalExperience(((rawPacket[pos + 4] & 0xFF) << 8) | (rawPacket[pos + 5] & 0xFF));
                        packet.setLevel(rawPacket[pos + 6] & 0xFF);
                    }
                    break;
                case GameProtocol.TYPE_WORLD_STATE:
                    if (pos < rawPacket.length - 1) {
                        int playerCount = Math.min(rawPacket[pos] & 0xFF, GameProtocol.MAX_PLAYERS);
                        pos++;
                        List<GamePacket.PlayerData> players = new ArrayList<>();
                        boolean hasExtendedData = (flags & GameProtocol.FLAG_HEALTH_EXTENDED) != 0;
                        boolean hasLevelData = (flags & GameProtocol.FLAG_LEVEL) != 0;
                        boolean hasSpriteNumData = (flags & GameProtocol.FLAG_SPRITE_NUM) != 0;
                        int playerDataSize;
                        if (hasExtendedData) {
                            playerDataSize = 18;
                        } else {
                            playerDataSize = 10;
                        }
                        for (int j = 0; j < playerCount; j++) {
                            if (pos + playerDataSize - 1 >= rawPacket.length - 1) break;
                            GamePacket.PlayerData playerData = new GamePacket.PlayerData();
                            playerData.setId(((rawPacket[pos] & 0xFF) << 8) | (rawPacket[pos + 1] & 0xFF));
                            playerData.setX(((rawPacket[pos + 2] & 0xFF) << 8) | (rawPacket[pos + 3] & 0xFF));
                            playerData.setY(((rawPacket[pos + 4] & 0xFF) << 8) | (rawPacket[pos + 5] & 0xFF));
                            playerData.setDirection(rawPacket[pos + 6]);
                            if (hasExtendedData) {
                                playerData.setHealth(((rawPacket[pos + 7] & 0xFF) << 8) | (rawPacket[pos + 8] & 0xFF));
                                playerData.setMaxHealth(((rawPacket[pos + 9] & 0xFF) << 8) | (rawPacket[pos + 10] & 0xFF));
                                playerData.setLevel(rawPacket[pos + 11] & 0xFF);
                                playerData.setDamage(((rawPacket[pos + 12] & 0xFF) << 8) | (rawPacket[pos + 13] & 0xFF));
                                playerData.setExperience(((rawPacket[pos + 14] & 0xFF) << 8) | (rawPacket[pos + 15] & 0xFF));
                                playerData.setExperienceToNextLevel(((rawPacket[pos + 16] & 0xFF) << 8) | (rawPacket[pos + 17] & 0xFF));
                                if (hasSpriteNumData) {
                                    playerData.setSpriteNum(rawPacket[pos + 18]);
                                    playerData.setDead(rawPacket[pos + 19] != 0);
                                    pos += 20;
                                } else {
                                    playerData.setSpriteNum((byte)1);
                                    playerData.setDead(rawPacket[pos + 18] != 0);
                                    pos += 19;
                                }
                            } else {
                                playerData.setHealth(rawPacket[pos + 7] & 0xFF);
                                playerData.setMaxHealth(100);
                                playerData.setLevel(1);
                                playerData.setDamage(25);
                                playerData.setExperience(0);
                                playerData.setExperienceToNextLevel(100);
                                if (hasSpriteNumData) {
                                    playerData.setSpriteNum(rawPacket[pos + 8]);
                                    playerData.setDead(rawPacket[pos + 9] != 0);
                                    pos += 10;
                                } else {
                                    playerData.setSpriteNum((byte)1);
                                    playerData.setDead(rawPacket[pos + 8] != 0);
                                    pos += 9;
                                }
                            }
                            players.add(playerData);
                        }
                        packet.setPlayersData(players);
                    }
                    break;
                case GameProtocol.TYPE_PLAYER_JOIN:
                    if (rawPacket.length >= GameProtocol.PLAYER_JOIN_SIZE) {
                        packet.setPlayerId(((rawPacket[pos] & 0xFF) << 8) | (rawPacket[pos + 1] & 0xFF));
                        packet.setX(((rawPacket[pos + 2] & 0xFF) << 8) | (rawPacket[pos + 3] & 0xFF));
                        packet.setY(((rawPacket[pos + 4] & 0xFF) << 8) | (rawPacket[pos + 5] & 0xFF));
                        packet.setDirection(rawPacket[pos + 6]);
                        if ((flags & GameProtocol.FLAG_HEALTH_EXTENDED) != 0) {
                            packet.setHealth(((rawPacket[pos + 7] & 0xFF) << 8) | (rawPacket[pos + 8] & 0xFF));
                            packet.setMaxHealth(((rawPacket[pos + 9] & 0xFF) << 8) | (rawPacket[pos + 10] & 0xFF));
                            packet.setDamage(((rawPacket[pos + 11] & 0xFF) << 8) | (rawPacket[pos + 12] & 0xFF));
                            packet.setLevel(rawPacket[pos + 13] & 0xFF);
                            packet.setExperience(((rawPacket[pos + 14] & 0xFF) << 8) | (rawPacket[pos + 15] & 0xFF));
                            packet.setExperienceToNextLevel(((rawPacket[pos + 16] & 0xFF) << 8) | (rawPacket[pos + 17] & 0xFF));
                        }
                    }
                    break;
                case GameProtocol.TYPE_PLAYER_LEAVE:
                    if (rawPacket.length >= GameProtocol.PLAYER_LEAVE_SIZE) {
                        packet.setPlayerId(((rawPacket[pos] & 0xFF) << 8) | (rawPacket[pos + 1] & 0xFF));
                    }
                    break;
                default:
                    return null;
            }
            return packet;
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public void reset() {
        incompleteBuffer = new byte[0];
    }
}