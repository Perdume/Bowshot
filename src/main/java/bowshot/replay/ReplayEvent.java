package bowshot.replay;

public class ReplayEvent {

    private final int tick;
    private final ReplayEventType type;
    private final int playerIndex;
    private final float x;
    private final float y;
    private final float z;
    private final float yaw;
    private final float pitch;
    private final int targetIndex;
    private final byte pose; // 0=STANDING, 1=SNEAKING
    private final String mainHandItem; // Material name or empty

    public ReplayEvent(int tick, ReplayEventType type, int playerIndex,
                       float x, float y, float z, float yaw, float pitch,
                       int targetIndex, byte pose, String mainHandItem) {
        this.tick = tick;
        this.type = type;
        this.playerIndex = playerIndex;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.targetIndex = targetIndex;
        this.pose = pose;
        this.mainHandItem = mainHandItem != null ? mainHandItem : "";
    }

    public static ReplayEvent position(int tick, int playerIndex, float x, float y, float z, float yaw, float pitch,
                                         byte pose, String mainHandItem) {
        return new ReplayEvent(tick, ReplayEventType.POSITION, playerIndex, x, y, z, yaw, pitch, -1, pose, mainHandItem);
    }

    public static ReplayEvent arrowShoot(int tick, int playerIndex, float x, float y, float z, float yaw, float pitch) {
        return new ReplayEvent(tick, ReplayEventType.ARROW_SHOOT, playerIndex, x, y, z, yaw, pitch, -1, (byte) 0, "");
    }

    public static ReplayEvent kill(int tick, int killerIndex, int victimIndex) {
        return new ReplayEvent(tick, ReplayEventType.KILL, killerIndex, 0, 0, 0, 0, 0, victimIndex, (byte) 0, "");
    }

    public static ReplayEvent death(int tick, int playerIndex) {
        return new ReplayEvent(tick, ReplayEventType.DEATH, playerIndex, 0, 0, 0, 0, 0, -1, (byte) 0, "");
    }

    public static ReplayEvent gameStart(int tick) {
        return new ReplayEvent(tick, ReplayEventType.GAME_START, -1, 0, 0, 0, 0, 0, -1, (byte) 0, "");
    }

    public static ReplayEvent gameEnd(int tick) {
        return new ReplayEvent(tick, ReplayEventType.GAME_END, -1, 0, 0, 0, 0, 0, -1, (byte) 0, "");
    }

    public int getTick() { return tick; }
    public ReplayEventType getType() { return type; }
    public int getPlayerIndex() { return playerIndex; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public int getTargetIndex() { return targetIndex; }
    public byte getPose() { return pose; }
    public String getMainHandItem() { return mainHandItem; }
}
