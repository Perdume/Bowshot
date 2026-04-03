package bowshot.debug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import bowshot.Bowshot;
import bowshot.game.Game;
import bowshot.match.MatchQueue;
import bowshot.player.GamePlayer;
import bowshot.stats.PlayerStats;

/**
 * 운영 진단 도구.
 * /bsa status 명령어에서 호출.
 */
public class Diagnostics {

    private final Bowshot plugin;

    public Diagnostics(Bowshot plugin) {
        this.plugin = plugin;
    }

    public List<String> runFullDiagnostics() {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GOLD + "========== Bowshot 진단 ==========");

        // 1. 시스템 상태
        lines.add("");
        lines.add(ChatColor.YELLOW + "■ 시스템 상태");
        lines.add(gray("  플러그인 버전: ") + plugin.getDescription().getVersion());
        lines.add(gray("  디버그 모드: ") + (DebugLogger.isEnabled() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        lines.add(gray("  안티치트: ") + (plugin.getSettings().isAcEnabled() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        lines.add(gray("  리플레이: ") + (plugin.getSettings().isReplayEnabled() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));

        // 2. 아레나 상태
        lines.add("");
        lines.add(ChatColor.YELLOW + "■ 아레나");
        List<String> arenaNames = plugin.getArenaManager().getArenaNames();
        lines.add(gray("  등록 아레나: ") + ChatColor.WHITE + arenaNames.size() + "개");
        if (arenaNames.isEmpty()) {
            lines.add(ChatColor.RED + "  ⚠ " + ErrorCode.ARENA_NO_AVAILABLE);
        }
        List<World> instanceWorlds = new ArrayList<>(plugin.getInstanceWorldManager().getActiveWorlds());
        lines.add(gray("  활성 인스턴스 월드: ") + ChatColor.WHITE + instanceWorlds.size() + "개");

        // 로비 위치 확인
        if (plugin.getSpawnManager().getLobbyLocation() == null) {
            lines.add(ChatColor.RED + "  ⚠ " + ErrorCode.WORLD_LOBBY_NOT_SET);
        }

        // 3. 게임 상태
        lines.add("");
        lines.add(ChatColor.YELLOW + "■ 활성 게임");
        List<Game> games = plugin.getGameManager().getActiveGames();
        lines.add(gray("  진행 중: ") + ChatColor.WHITE + games.size() + "개");
        for (Game game : games) {
            int alive = game.getArenaInstance().getAliveCount();
            int total = game.getArenaInstance().getPlayers().size();
            int totalKills = 0;
            int maxKill = 0;
            int minKill = Integer.MAX_VALUE;
            for (Map.Entry<Player, Integer> entry : game.getKillCounts().entrySet()) {
                totalKills += entry.getValue();
                maxKill = Math.max(maxKill, entry.getValue());
                minKill = Math.min(minKill, entry.getValue());
            }
            if (minKill == Integer.MAX_VALUE) minKill = 0;

            lines.add(gray("  ├ ") + game.getArenaInstance().getWorldName()
                    + gray(" [생존 " + alive + "/" + total + "] [킬합계 " + totalKills + "]"));

            // 존버 감지: 시간 많이 지났는데 킬이 거의 없으면
            if (total >= 2 && totalKills == 0) {
                lines.add(ChatColor.RED + "  │  ⚠ " + ErrorCode.GAME_ZERO_KILLS.getCode() + " 전원 0킬 (존버 의심)");
            }

            // 일방적 게임 감지: 한 명이 킬 대부분을 차지
            if (maxKill > 0 && total >= 3 && maxKill >= totalKills * 0.7) {
                lines.add(ChatColor.RED + "  │  ⚠ " + ErrorCode.GAME_ONE_SIDED.getCode() + " 일방적 게임");
            }
        }

        // 4. 매칭 대기열
        lines.add("");
        lines.add(ChatColor.YELLOW + "■ 매칭 대기열");
        int queueSize = plugin.getMatchQueue().getQueueSize();
        lines.add(gray("  대기 인원: ") + ChatColor.WHITE + queueSize + "명");
        if (queueSize > 0) {
            long[] waitStats = plugin.getMatchQueue().getWaitTimeStats();
            if (waitStats != null) {
                lines.add(gray("  평균 대기: ") + ChatColor.WHITE + (waitStats[0] / 1000) + "초");
                lines.add(gray("  최대 대기: ") + ChatColor.WHITE + (waitStats[1] / 1000) + "초");
                if (waitStats[1] > plugin.getSettings().getMatchmakingMaxWaitSeconds() * 1000L) {
                    lines.add(ChatColor.RED + "  ⚠ " + ErrorCode.MATCH_LONG_WAIT.getCode() + " 장시간 대기자 있음");
                }
            }
            int[] mmrRange = plugin.getMatchQueue().getMmrRange();
            if (mmrRange != null) {
                lines.add(gray("  대기열 점수: ") + ChatColor.WHITE + mmrRange[0] + " ~ " + mmrRange[1]
                        + gray(" (차이 " + (mmrRange[1] - mmrRange[0]) + ")"));
            }
        }

        // 5. ELO 분포
        lines.add("");
        lines.add(ChatColor.YELLOW + "■ 점수 분포");
        MmrDistribution dist = getMmrDistribution();
        if (dist.count == 0) {
            lines.add(gray("  등록된 플레이어 없음"));
        } else {
            lines.add(gray("  전체 플레이어: ") + ChatColor.WHITE + dist.count + "명");
            lines.add(gray("  평균 점수: ") + ChatColor.WHITE + dist.average);
            lines.add(gray("  중앙값: ") + ChatColor.WHITE + dist.median);
            lines.add(gray("  최저/최고: ") + ChatColor.WHITE + dist.min + " / " + dist.max);
            lines.add(gray("  표준편차: ") + ChatColor.WHITE + String.format("%.1f", dist.stdDev));

            if (dist.max - dist.min > 1000) {
                lines.add(ChatColor.RED + "  ⚠ " + ErrorCode.SCORE_EXTREME_GAP.getCode() + " 점수 격차 " + (dist.max - dist.min));
            }
            if (dist.average > plugin.getSettings().getBaseMmr() + 300) {
                lines.add(ChatColor.GOLD + "  ⚠ " + ErrorCode.SCORE_INFLATION.getCode() + " 평균 점수가 기본값보다 " + (dist.average - plugin.getSettings().getBaseMmr()) + " 높음");
            }
            if (dist.average < plugin.getSettings().getBaseMmr() - 200 && dist.count > 5) {
                lines.add(ChatColor.GOLD + "  ⚠ " + ErrorCode.SCORE_DEFLATION.getCode() + " 평균 점수가 기본값보다 " + (plugin.getSettings().getBaseMmr() - dist.average) + " 낮음");
            }

            // 존버 의심: 게임수 대비 킬 비율 낮은 플레이어
            List<String> passivePlayers = getPassivePlayers();
            if (!passivePlayers.isEmpty()) {
                lines.add("");
                lines.add(ChatColor.YELLOW + "■ 존버 의심 플레이어 (킬/게임 < 0.3, 10게임 이상)");
                for (String info : passivePlayers) {
                    lines.add(gray("  ") + info);
                }
            }
        }

        // 6. 최근 에러
        lines.add("");
        lines.add(ChatColor.YELLOW + "■ 최근 에러/경고 (최대 10개)");
        List<String> recent = DebugLogger.getRecentErrors();
        if (recent.isEmpty()) {
            lines.add(gray("  없음"));
        } else {
            int start = Math.max(0, recent.size() - 10);
            for (int i = start; i < recent.size(); i++) {
                lines.add(gray("  ") + ChatColor.RED + recent.get(i));
            }
        }

        lines.add("");
        lines.add(ChatColor.GOLD + "===================================");
        return lines;
    }

    // === MMR 분포 계산 ===

    private MmrDistribution getMmrDistribution() {
        ConfigurationSection section = plugin.getStatsManager().getDataFile()
                .getConfig().getConfigurationSection("players");
        if (section == null) return new MmrDistribution();

        List<Integer> mmrs = new ArrayList<>();
        int baseMmr = plugin.getSettings().getBaseMmr();
        for (String uuid : section.getKeys(false)) {
            mmrs.add(section.getInt(uuid + ".mmr", baseMmr));
        }
        if (mmrs.isEmpty()) return new MmrDistribution();

        Collections.sort(mmrs);
        int sum = 0;
        for (int e : mmrs) sum += e;
        int avg = sum / mmrs.size();
        int median = mmrs.get(mmrs.size() / 2);
        int min = mmrs.get(0);
        int max = mmrs.get(mmrs.size() - 1);

        double variance = 0;
        for (int e : mmrs) {
            variance += (e - avg) * (e - avg);
        }
        variance /= mmrs.size();
        double stdDev = Math.sqrt(variance);

        MmrDistribution dist = new MmrDistribution();
        dist.count = mmrs.size();
        dist.average = avg;
        dist.median = median;
        dist.min = min;
        dist.max = max;
        dist.stdDev = stdDev;
        return dist;
    }

    // === 존버 플레이어 검출 ===

    private List<String> getPassivePlayers() {
        List<String> results = new ArrayList<>();
        ConfigurationSection section = plugin.getStatsManager().getDataFile()
                .getConfig().getConfigurationSection("players");
        if (section == null) return results;

        for (String uuidStr : section.getKeys(false)) {
            int gamesPlayed = section.getInt(uuidStr + ".games-played", 0);
            int kills = section.getInt(uuidStr + ".kills", 0);
            if (gamesPlayed < 10) continue;

            double killPerGame = (double) kills / gamesPlayed;
            if (killPerGame < 0.3) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String name = Bukkit.getOfflinePlayer(uuid).getName();
                    if (name == null) name = uuidStr.substring(0, 8);
                    results.add(ChatColor.WHITE + name + ChatColor.GRAY
                            + " (점수:" + section.getInt(uuidStr + ".elo", 0)
                            + " 게임:" + gamesPlayed
                            + " 킬:" + kills
                            + " 킬/게임:" + String.format("%.2f", killPerGame) + ")");
                } catch (Exception ignored) {}
            }
        }
        return results;
    }

    private String gray(String text) {
        return ChatColor.GRAY + text;
    }

    private static class MmrDistribution {
        int count = 0;
        int average = 0;
        int median = 0;
        int min = 0;
        int max = 0;
        double stdDev = 0;
    }
}
