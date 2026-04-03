package bowshot.replay;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ReplayData {

    private static final int FORMAT_VERSION = 2;

    private String arenaName;
    private long recordedAt;
    private int durationTicks;
    private final List<String> playerNames = new ArrayList<>();
    private final List<UUID> playerUuids = new ArrayList<>();
    private final List<ReplayEvent> events = new ArrayList<>();

    public ReplayData(String arenaName) {
        this.arenaName = arenaName;
        this.recordedAt = System.currentTimeMillis();
    }

    private ReplayData() {}

    public void addPlayer(UUID uuid, String name) {
        playerUuids.add(uuid);
        playerNames.add(name);
    }

    public int getPlayerIndex(UUID uuid) {
        return playerUuids.indexOf(uuid);
    }

    public void addEvent(ReplayEvent event) {
        events.add(event);
    }

    public void setDurationTicks(int ticks) {
        this.durationTicks = ticks;
    }

    // === Getters ===

    public String getArenaName() { return arenaName; }
    public long getRecordedAt() { return recordedAt; }
    public int getDurationTicks() { return durationTicks; }
    public List<String> getPlayerNames() { return Collections.unmodifiableList(playerNames); }
    public List<UUID> getPlayerUuids() { return Collections.unmodifiableList(playerUuids); }
    public List<ReplayEvent> getEvents() { return Collections.unmodifiableList(events); }
    public int getPlayerCount() { return playerNames.size(); }

    // === Save / Load ===

    public void save(File file) throws IOException {
        file.getParentFile().mkdirs();
        try (DataOutputStream out = new DataOutputStream(
                new GZIPOutputStream(new FileOutputStream(file)))) {
            out.writeInt(FORMAT_VERSION);
            out.writeUTF(arenaName);
            out.writeLong(recordedAt);
            out.writeInt(durationTicks);

            out.writeInt(playerNames.size());
            for (int i = 0; i < playerNames.size(); i++) {
                out.writeLong(playerUuids.get(i).getMostSignificantBits());
                out.writeLong(playerUuids.get(i).getLeastSignificantBits());
                out.writeUTF(playerNames.get(i));
            }

            out.writeInt(events.size());
            for (ReplayEvent event : events) {
                out.writeInt(event.getTick());
                out.writeByte(event.getType().ordinal());
                out.writeByte(event.getPlayerIndex());
                out.writeFloat(event.getX());
                out.writeFloat(event.getY());
                out.writeFloat(event.getZ());
                out.writeFloat(event.getYaw());
                out.writeFloat(event.getPitch());
                out.writeByte(event.getTargetIndex());
                out.writeByte(event.getPose());
                out.writeUTF(event.getMainHandItem());
            }
        }
    }

    public static ReplayData load(File file) throws IOException {
        ReplayData data = new ReplayData();
        try (DataInputStream in = new DataInputStream(
                new GZIPInputStream(new FileInputStream(file)))) {
            int version = in.readInt();
            if (version != 1 && version != FORMAT_VERSION) {
                throw new IOException("Unsupported replay format version: " + version);
            }

            data.arenaName = in.readUTF();
            data.recordedAt = in.readLong();
            data.durationTicks = in.readInt();

            int playerCount = in.readInt();
            for (int i = 0; i < playerCount; i++) {
                long msb = in.readLong();
                long lsb = in.readLong();
                String name = in.readUTF();
                data.playerUuids.add(new UUID(msb, lsb));
                data.playerNames.add(name);
            }

            int eventCount = in.readInt();
            for (int i = 0; i < eventCount; i++) {
                int tick = in.readInt();
                ReplayEventType type = ReplayEventType.values()[in.readByte()];
                int playerIndex = in.readByte();
                float x = in.readFloat();
                float y = in.readFloat();
                float z = in.readFloat();
                float yaw = in.readFloat();
                float pitch = in.readFloat();
                int targetIndex = in.readByte();
                byte pose = 0;
                String mainHandItem = "";
                if (version >= 2) {
                    pose = in.readByte();
                    mainHandItem = in.readUTF();
                }
                data.events.add(new ReplayEvent(tick, type, playerIndex, x, y, z, yaw, pitch, targetIndex, pose, mainHandItem));
            }
        }
        return data;
    }
}
