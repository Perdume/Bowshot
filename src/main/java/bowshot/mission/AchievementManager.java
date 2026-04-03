package bowshot.mission;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import bowshot.Bowshot;
import bowshot.data.DataFile;
import bowshot.debug.DebugLogger;
import bowshot.lang.MessageKey;
import bowshot.stats.PlayerStats;

public class AchievementManager {

    private final Bowshot plugin;
    private final DataFile achievementData;

    public AchievementManager(Bowshot plugin) {
        this.plugin = plugin;
        this.achievementData = new DataFile(plugin, "achievements.yml");
    }

    // === 달성 확인 ===

    public boolean isUnlocked(UUID uuid, AchievementType type) {
        return achievementData.getConfig().getBoolean("players." + uuid + ".unlocked." + type.name(), false);
    }

    public boolean isClaimed(UUID uuid, AchievementType type) {
        return achievementData.getConfig().getBoolean("players." + uuid + ".claimed." + type.name(), false);
    }

    public int getProgress(UUID uuid, AchievementType type) {
        return achievementData.getConfig().getInt("players." + uuid + ".progress." + type.name(), 0);
    }

    private void setProgress(UUID uuid, AchievementType type, int value) {
        int capped = Math.min(value, type.getGoal());
        achievementData.getConfig().set("players." + uuid + ".progress." + type.name(), capped);

        if (capped >= type.getGoal() && !isUnlocked(uuid, type)) {
            unlock(uuid, type);
        }
        achievementData.save();
    }

    private void unlock(UUID uuid, AchievementType type) {
        achievementData.getConfig().set("players." + uuid + ".unlocked." + type.name(), true);
        achievementData.save();

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            String lang = plugin.getSettings().getLanguage();
            player.sendMessage(plugin.getMessageManager().get(MessageKey.ACHIEVEMENT_UNLOCKED,
                    type.getName(lang)));
        }
        DebugLogger.log("Achievement", uuid + " unlocked " + type.name());
    }

    // === 보상 수령 ===

    public boolean claimReward(UUID uuid, AchievementType type) {
        if (!isUnlocked(uuid, type)) return false;
        if (isClaimed(uuid, type)) return false;

        achievementData.getConfig().set("players." + uuid + ".claimed." + type.name(), true);
        achievementData.save();

        PlayerStats stats = plugin.getStatsManager().getStats(uuid);
        stats.addMmr(type.getReward());
        plugin.getStatsManager().saveStats(uuid, stats);

        DebugLogger.log("Achievement", uuid + " claimed " + type.name() + " reward=" + type.getReward());
        return true;
    }

    // === 게임 결과에 따른 업적 진행 ===

    public void onGameEnd(UUID uuid, int placement, int totalPlayers, int kills,
                          boolean isMvp, boolean noDeath, int currentScore, int winStreak,
                          int singleGameKills) {
        PlayerStats stats = plugin.getStatsManager().getStats(uuid);

        // 누적 킬 업적
        int totalKills = stats.getKills();
        updateCumulative(uuid, AchievementType.KILL_10, totalKills);
        updateCumulative(uuid, AchievementType.KILL_50, totalKills);
        updateCumulative(uuid, AchievementType.KILL_100, totalKills);
        updateCumulative(uuid, AchievementType.KILL_500, totalKills);
        updateCumulative(uuid, AchievementType.KILL_1000, totalKills);

        // 누적 승리 업적
        int totalWins = stats.getWins();
        updateCumulative(uuid, AchievementType.WIN_1, totalWins);
        updateCumulative(uuid, AchievementType.WIN_10, totalWins);
        updateCumulative(uuid, AchievementType.WIN_50, totalWins);
        updateCumulative(uuid, AchievementType.WIN_100, totalWins);
        updateCumulative(uuid, AchievementType.WIN_500, totalWins);

        // 누적 게임수 업적
        int totalGames = stats.getGamesPlayed();
        updateCumulative(uuid, AchievementType.GAMES_10, totalGames);
        updateCumulative(uuid, AchievementType.GAMES_50, totalGames);
        updateCumulative(uuid, AchievementType.GAMES_100, totalGames);
        updateCumulative(uuid, AchievementType.GAMES_500, totalGames);
        updateCumulative(uuid, AchievementType.GAMES_1000, totalGames);

        // 점수 업적
        updateCumulative(uuid, AchievementType.SCORE_100, currentScore);
        updateCumulative(uuid, AchievementType.SCORE_300, currentScore);
        updateCumulative(uuid, AchievementType.SCORE_500, currentScore);
        updateCumulative(uuid, AchievementType.SCORE_1000, currentScore);
        updateCumulative(uuid, AchievementType.SCORE_2000, currentScore);

        // MVP 업적 (누적)
        if (isMvp) {
            int mvpProgress = getProgress(uuid, AchievementType.MVP_1) + 1;
            setProgress(uuid, AchievementType.MVP_1, mvpProgress);
            setProgress(uuid, AchievementType.MVP_10, mvpProgress);
            setProgress(uuid, AchievementType.MVP_50, mvpProgress);
        }

        // 연승 업적 (최대값 추적)
        if (winStreak > 0) {
            updateMax(uuid, AchievementType.STREAK_3, winStreak);
            updateMax(uuid, AchievementType.STREAK_5, winStreak);
            updateMax(uuid, AchievementType.STREAK_10, winStreak);
        }

        // 무피해 승리
        if (noDeath && placement == 1) {
            setProgress(uuid, AchievementType.PERFECT_WIN, 1);
        }

        // 한 게임 킬 스프리
        if (singleGameKills >= 5) {
            updateMax(uuid, AchievementType.KILL_SPREE_5, singleGameKills);
        }
        if (singleGameKills >= 8) {
            updateMax(uuid, AchievementType.KILL_SPREE_8, singleGameKills);
        }
    }

    private void updateCumulative(UUID uuid, AchievementType type, int totalValue) {
        if (isUnlocked(uuid, type)) return;
        setProgress(uuid, type, totalValue);
    }

    private void updateMax(UUID uuid, AchievementType type, int value) {
        if (isUnlocked(uuid, type)) return;
        int current = getProgress(uuid, type);
        if (value > current) {
            setProgress(uuid, type, value);
        }
    }

    // === 관리자 기능 ===

    public void resetPlayer(UUID uuid) {
        achievementData.getConfig().set("players." + uuid, null);
        achievementData.save();
    }

    public void grantAchievement(UUID uuid, AchievementType type) {
        setProgress(uuid, type, type.getGoal());
    }

    // === 통계 ===

    public int getUnlockedCount(UUID uuid) {
        int count = 0;
        for (AchievementType type : AchievementType.values()) {
            if (isUnlocked(uuid, type)) count++;
        }
        return count;
    }

    public int getTotalCount() {
        return AchievementType.values().length;
    }

    public List<AchievementType> getUnlockedList(UUID uuid) {
        List<AchievementType> list = new ArrayList<>();
        for (AchievementType type : AchievementType.values()) {
            if (isUnlocked(uuid, type)) list.add(type);
        }
        return list;
    }

    public DataFile getDataFile() {
        return achievementData;
    }
}
