package bowshot.debug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import bowshot.Bowshot;

public class DebugLogger {

    private static boolean enabled = false;
    private static final int MAX_RECENT = 100;
    private static final List<String> recentErrors = Collections.synchronizedList(new ArrayList<>());

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void log(String message) {
        if (enabled) {
            Bowshot.getInstance().getLogger().info("[DEBUG] " + message);
        }
    }

    public static void log(String category, String message) {
        if (enabled) {
            Bowshot.getInstance().getLogger().info("[DEBUG/" + category + "] " + message);
        }
    }

    public static void warn(String message) {
        if (enabled) {
            Bowshot.getInstance().getLogger().warning("[DEBUG] " + message);
        }
    }

    public static void error(String message, Throwable t) {
        Bowshot.getInstance().getLogger().log(Level.SEVERE, "[ERROR] " + message, t);
    }

    /**
     * 에러코드 기반 로깅. 심각도에 따라 자동 분류.
     * INFO → debug일 때만, WARN/ERROR → 항상 기록.
     */
    public static void report(ErrorCode code, String detail) {
        String msg = code.toString() + (detail != null ? " | " + detail : "");

        switch (code.getSeverity()) {
            case ERROR:
                Bowshot.getInstance().getLogger().severe(msg);
                addRecent(msg);
                break;
            case WARN:
                Bowshot.getInstance().getLogger().warning(msg);
                addRecent(msg);
                break;
            case INFO:
                if (enabled) {
                    Bowshot.getInstance().getLogger().info(msg);
                }
                addRecent(msg);
                break;
        }
    }

    public static void report(ErrorCode code) {
        report(code, null);
    }

    private static void addRecent(String msg) {
        recentErrors.add(msg);
        while (recentErrors.size() > MAX_RECENT) {
            recentErrors.remove(0);
        }
    }

    public static List<String> getRecentErrors() {
        return new ArrayList<>(recentErrors);
    }

    public static void clearRecentErrors() {
        recentErrors.clear();
    }
}
