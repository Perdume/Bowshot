package bowshot.mission;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import bowshot.Bowshot;
import bowshot.data.DataFile;
import bowshot.debug.DebugLogger;
import bowshot.lang.MessageKey;
import bowshot.stats.PlayerStats;

public class MissionManager {

    private final Bowshot plugin;
    private final DataFile missionData;
    private final Random random = new Random();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int DAILY_MISSION_COUNT = 3;
    private static final int WEEKLY_MISSION_COUNT = 3;

    public MissionManager(Bowshot plugin) {
        this.plugin = plugin;
        this.missionData = new DataFile(plugin, "missions.yml");
    }

    // === 미션 할당 ===

    public List<MissionType> getActiveDailyMissions(UUID uuid) {
        String today = LocalDate.now().format(DATE_FMT);
        String path = "players." + uuid + ".daily";
        String storedDate = missionData.getConfig().getString(path + ".date", "");

        if (!today.equals(storedDate)) {
            assignDailyMissions(uuid);
        }

        return loadMissions(path + ".missions");
    }

    public List<MissionType> getActiveWeeklyMissions(UUID uuid) {
        String weekKey = getWeekKey();
        String path = "players." + uuid + ".weekly";
        String storedWeek = missionData.getConfig().getString(path + ".week", "");

        if (!weekKey.equals(storedWeek)) {
            assignWeeklyMissions(uuid);
        }

        return loadMissions(path + ".missions");
    }

    private void assignDailyMissions(UUID uuid) {
        String today = LocalDate.now().format(DATE_FMT);
        String path = "players." + uuid + ".daily";

        List<MissionType> dailyPool = new ArrayList<>();
        for (MissionType m : MissionType.values()) {
            if (m.getPeriod() == MissionPeriod.DAILY) dailyPool.add(m);
        }
        Collections.shuffle(dailyPool, random);

        List<String> assigned = new ArrayList<>();
        for (int i = 0; i < Math.min(DAILY_MISSION_COUNT, dailyPool.size()); i++) {
            assigned.add(dailyPool.get(i).name());
        }

        missionData.getConfig().set(path + ".date", today);
        missionData.getConfig().set(path + ".missions", assigned);

        // Reset progress
        for (String name : assigned) {
            missionData.getConfig().set(path + ".progress." + name, 0);
            missionData.getConfig().set(path + ".claimed." + name, false);
        }
        missionData.save();
        DebugLogger.log("Mission", "Assigned daily missions for " + uuid + ": " + assigned);
    }

    private void assignWeeklyMissions(UUID uuid) {
        String weekKey = getWeekKey();
        String path = "players." + uuid + ".weekly";

        List<MissionType> weeklyPool = new ArrayList<>();
        for (MissionType m : MissionType.values()) {
            if (m.getPeriod() == MissionPeriod.WEEKLY) weeklyPool.add(m);
        }
        Collections.shuffle(weeklyPool, random);

        List<String> assigned = new ArrayList<>();
        for (int i = 0; i < Math.min(WEEKLY_MISSION_COUNT, weeklyPool.size()); i++) {
            assigned.add(weeklyPool.get(i).name());
        }

        missionData.getConfig().set(path + ".week", weekKey);
        missionData.getConfig().set(path + ".missions", assigned);

        for (String name : assigned) {
            missionData.getConfig().set(path + ".progress." + name, 0);
            missionData.getConfig().set(path + ".claimed." + name, false);
        }
        missionData.save();
        DebugLogger.log("Mission", "Assigned weekly missions for " + uuid + ": " + assigned);
    }

    // === 진행도 업데이트 ===

    public void addProgress(UUID uuid, MissionType type, int amount) {
        String periodPath = type.getPeriod() == MissionPeriod.DAILY ? "daily" : "weekly";
        String path = "players." + uuid + "." + periodPath;

        List<String> missions = missionData.getConfig().getStringList(path + ".missions");
        if (!missions.contains(type.name())) return;

        boolean claimed = missionData.getConfig().getBoolean(path + ".claimed." + type.name(), false);
        if (claimed) return;

        int current = missionData.getConfig().getInt(path + ".progress." + type.name(), 0);
        int newProgress = Math.min(current + amount, type.getGoal());
        missionData.getConfig().set(path + ".progress." + type.name(), newProgress);
        missionData.save();

        // Auto notify on completion
        if (current < type.getGoal() && newProgress >= type.getGoal()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                String lang = plugin.getSettings().getLanguage();
                player.sendMessage(plugin.getMessageManager().get(MessageKey.MISSION_COMPLETE,
                        type.getDisplay(lang)));
            }
        }
    }

    public int getProgress(UUID uuid, MissionType type) {
        String periodPath = type.getPeriod() == MissionPeriod.DAILY ? "daily" : "weekly";
        String path = "players." + uuid + "." + periodPath + ".progress." + type.name();
        return missionData.getConfig().getInt(path, 0);
    }

    public boolean isClaimed(UUID uuid, MissionType type) {
        String periodPath = type.getPeriod() == MissionPeriod.DAILY ? "daily" : "weekly";
        String path = "players." + uuid + "." + periodPath + ".claimed." + type.name();
        return missionData.getConfig().getBoolean(path, false);
    }

    // === 보상 수령 ===

    public boolean claimReward(UUID uuid, MissionType type) {
        int progress = getProgress(uuid, type);
        if (progress < type.getGoal()) return false;
        if (isClaimed(uuid, type)) return false;

        String periodPath = type.getPeriod() == MissionPeriod.DAILY ? "daily" : "weekly";
        String path = "players." + uuid + "." + periodPath + ".claimed." + type.name();
        missionData.getConfig().set(path, true);
        missionData.save();

        // 점수 보상 지급
        PlayerStats stats = plugin.getStatsManager().getStats(uuid);
        stats.addMmr(type.getReward());
        plugin.getStatsManager().saveStats(uuid, stats);

        DebugLogger.log("Mission", uuid + " claimed " + type.name() + " reward=" + type.getReward());
        return true;
    }

    // === 게임 결과에 따른 미션 진행 ===

    public void onGameEnd(UUID uuid, int placement, int totalPlayers, int kills,
                          boolean isMvp, boolean isTimeout, int bowKills, int meleeKills,
                          boolean noDeath, int winStreak) {
        // Ensure missions are loaded
        getActiveDailyMissions(uuid);
        getActiveWeeklyMissions(uuid);

        // 게임 참여
        addProgressAll(uuid, MissionType.DAILY_PLAY_GAMES, 1);
        addProgressAll(uuid, MissionType.WEEKLY_PLAY_GAMES, 1);

        // 승리
        if (placement == 1) {
            addProgressAll(uuid, MissionType.DAILY_WIN, 1);
            addProgressAll(uuid, MissionType.WEEKLY_WIN_5, 1);
            addProgressAll(uuid, MissionType.WEEKLY_WIN_10, 1);
        }

        // 1등
        if (placement == 1) {
            addProgressAll(uuid, MissionType.DAILY_FIRST_PLACE, 1);
            addProgressAll(uuid, MissionType.WEEKLY_FIRST_PLACE_3, 1);
        }

        // 3등 이내
        if (placement <= 3 && totalPlayers >= 3) {
            addProgressAll(uuid, MissionType.DAILY_TOP3, 1);
            addProgressAll(uuid, MissionType.WEEKLY_TOP3_5, 1);
        }

        // 킬
        if (kills >= 3) addProgressAll(uuid, MissionType.DAILY_KILL_3, kills);
        if (kills >= 5) addProgressAll(uuid, MissionType.DAILY_KILL_5, kills);
        addProgressAll(uuid, MissionType.WEEKLY_KILL_30, kills);
        addProgressAll(uuid, MissionType.WEEKLY_KILL_50, kills);

        // MVP
        if (isMvp) {
            addProgressAll(uuid, MissionType.DAILY_MVP, 1);
            addProgressAll(uuid, MissionType.WEEKLY_MVP_3, 1);
        }

        // 무피해 승리
        if (noDeath && placement == 1) {
            addProgressAll(uuid, MissionType.DAILY_NO_DEATH_WIN, 1);
        }

        // 활 킬
        if (bowKills > 0) {
            addProgressAll(uuid, MissionType.DAILY_BOW_KILL_3, bowKills);
            addProgressAll(uuid, MissionType.WEEKLY_BOW_KILL_20, bowKills);
        }

        // 근접 킬
        if (meleeKills > 0) {
            addProgressAll(uuid, MissionType.DAILY_MELEE_KILL, meleeKills);
        }

        // 연승
        if (winStreak >= 3) {
            addProgressAll(uuid, MissionType.WEEKLY_STREAK_3, 1);
        }
    }

    private void addProgressAll(UUID uuid, MissionType type, int amount) {
        addProgress(uuid, type, amount);
    }

    // === 관리자 기능 ===

    public void resetPlayerMissions(UUID uuid, MissionPeriod period) {
        String periodPath = period == MissionPeriod.DAILY ? "daily" : "weekly";
        missionData.getConfig().set("players." + uuid + "." + periodPath, null);
        missionData.save();
    }

    public void resetAllMissions(MissionPeriod period) {
        String periodPath = period == MissionPeriod.DAILY ? "daily" : "weekly";
        var section = missionData.getConfig().getConfigurationSection("players");
        if (section == null) return;
        for (String uuid : section.getKeys(false)) {
            missionData.getConfig().set("players." + uuid + "." + periodPath, null);
        }
        missionData.save();
    }

    // === Util ===

    private String getWeekKey() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        return monday.format(DATE_FMT);
    }

    private List<MissionType> loadMissions(String path) {
        List<String> names = missionData.getConfig().getStringList(path);
        List<MissionType> result = new ArrayList<>();
        for (String name : names) {
            try {
                result.add(MissionType.valueOf(name));
            } catch (IllegalArgumentException ignored) {}
        }
        return result;
    }

    public DataFile getDataFile() {
        return missionData;
    }
}
