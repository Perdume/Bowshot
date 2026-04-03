package bowshot.game;

public class GameTimer {

    private long startTime;
    private boolean started;

    public void start() {
        this.startTime = System.currentTimeMillis();
        this.started = true;
    }

    public void reset() {
        this.startTime = 0;
        this.started = false;
    }

    public long getStartTime() {
        if (!started) start();
        return startTime;
    }

    public boolean isStarted() {
        return started;
    }

    public String getFormattedTime(int totalGameTicks) {
        int elapsed = (int) (System.currentTimeMillis() - getStartTime());
        int remainingTicks = totalGameTicks - elapsed / 50;
        if (remainingTicks < 0) remainingTicks = 0;

        int tick = Math.floorMod(remainingTicks, 20);
        int sec = Math.floorMod(remainingTicks, 1200) / 20;
        int min = remainingTicks / 1200;

        return min + ":" + String.format("%02d", sec) + ":" + String.format("%02d", tick);
    }

    public int getRemainingTicks(int totalGameTicks) {
        int elapsed = (int) (System.currentTimeMillis() - getStartTime());
        return totalGameTicks - elapsed / 50;
    }
}
