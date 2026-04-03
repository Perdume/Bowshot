package bowshot.match;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import bowshot.Bowshot;
import bowshot.debug.DebugLogger;
import bowshot.debug.ErrorCode;
import bowshot.lang.MessageKey;

public class MatchQueue {

    private final Bowshot plugin;
    private final Map<UUID, QueueEntry> entries = new LinkedHashMap<>();
    private BukkitRunnable matchmakingTask;
    private BukkitRunnable actionBarTask;

    public MatchQueue(Bowshot plugin) {
        this.plugin = plugin;
    }

    public boolean join(Player player) {
        if (isInQueue(player)) {
            player.sendMessage(plugin.getMessageManager().get(MessageKey.MATCH_ALREADY_IN));
            return false;
        }
        int mmr = plugin.getStatsManager().getMmr(player.getUniqueId());
        DebugLogger.log("Queue", player.getName() + " joined queue (mmr=" + mmr + ", queueSize=" + (entries.size() + 1) + ")");
        entries.put(player.getUniqueId(), new QueueEntry(player, mmr, System.currentTimeMillis()));
        player.sendMessage(plugin.getMessageManager().get(MessageKey.MATCH_JOINED));
        showQueueScoreboard(player);

        if (matchmakingTask == null) {
            startMatchmaking();
        }
        return true;
    }

    public boolean leave(Player player) {
        if (!isInQueue(player)) {
            player.sendMessage(plugin.getMessageManager().get(MessageKey.MATCH_NOT_IN));
            return false;
        }
        entries.remove(player.getUniqueId());
        clearQueueScoreboard(player);
        player.sendMessage(plugin.getMessageManager().get(MessageKey.MATCH_LEFT));

        if (entries.isEmpty()) {
            stopMatchmaking();
        }
        return true;
    }

    public boolean isInQueue(Player player) {
        return entries.containsKey(player.getUniqueId());
    }

    public int getQueueSize() {
        return entries.size();
    }

    public List<Player> getQueue() {
        List<Player> list = new ArrayList<>();
        for (QueueEntry e : entries.values()) {
            list.add(e.player());
        }
        return list;
    }

    public void clear() {
        for (QueueEntry e : entries.values()) {
            clearQueueScoreboard(e.player());
        }
        entries.clear();
        stopMatchmaking();
    }

    /**
     * MMR 기반 매칭 — 가장 적합한 그룹을 찾아 반환.
     * 대기시간이 길수록 MMR 허용 범위가 넓어진다.
     */
    public List<Player> findMatch() {
        int minPlayers = plugin.getSettings().getMinPlayers();
        int maxPlayers = plugin.getSettings().getMaxPlayers();

        if (entries.size() < minPlayers) return null;

        int baseRange = plugin.getSettings().getMatchmakingMmrRange();
        int expandPerSec = plugin.getSettings().getMatchmakingMmrExpandPerSec();
        int maxWaitMs = plugin.getSettings().getMatchmakingMaxWaitSeconds() * 1000;
        long now = System.currentTimeMillis();

        // MMR 순으로 정렬
        List<QueueEntry> sorted = new ArrayList<>(entries.values());
        sorted.sort(Comparator.comparingInt(QueueEntry::mmr));

        // 슬라이딩 윈도우로 가장 큰 매칭 가능 그룹 찾기
        List<QueueEntry> bestGroup = null;

        for (int i = 0; i < sorted.size(); i++) {
            List<QueueEntry> group = new ArrayList<>();
            group.add(sorted.get(i));

            for (int j = i + 1; j < sorted.size(); j++) {
                QueueEntry a = sorted.get(i);
                QueueEntry b = sorted.get(j);

                // 두 플레이어 중 더 오래 기다린 쪽 기준으로 범위 확대
                long longestWait = Math.max(now - a.joinedAt(), now - b.joinedAt());
                int expandedRange = baseRange + (int) (longestWait / 1000) * expandPerSec;

                int mmrDiff = Math.abs(a.mmr() - b.mmr());

                if (mmrDiff <= expandedRange) {
                    group.add(b);
                } else {
                    break; // 정렬 상태이므로, 이후 더 클 수밖에 없음
                }

                if (group.size() >= maxPlayers) break;
            }

            if (group.size() >= minPlayers) {
                if (bestGroup == null || group.size() > bestGroup.size()) {
                    bestGroup = new ArrayList<>(group);
                }
            }
        }

        // 최대 대기 시간 초과 시 선착순 폴백
        if (bestGroup == null) {
            for (QueueEntry e : sorted) {
                if (now - e.joinedAt() >= maxWaitMs && entries.size() >= minPlayers) {
                    // 대기 시간 초과 — 아무나 매칭
                    bestGroup = new ArrayList<>(sorted.subList(0, Math.min(maxPlayers, sorted.size())));
                    DebugLogger.report(ErrorCode.MATCH_TIMEOUT_FALLBACK, "waited=" + (now - e.joinedAt()) + "ms");
                    break;
                }
            }
        }

        if (bestGroup == null) return null;

        // 최대 인원 제한
        if (bestGroup.size() > maxPlayers) {
            bestGroup = bestGroup.subList(0, maxPlayers);
        }

        // 큐에서 제거
        List<Player> matched = new ArrayList<>();
        int matchMinMmr = Integer.MAX_VALUE;
        int matchMaxMmr = Integer.MIN_VALUE;
        for (QueueEntry e : bestGroup) {
            entries.remove(e.player().getUniqueId());
            clearQueueScoreboard(e.player());
            matched.add(e.player());
            if (e.mmr() < matchMinMmr) matchMinMmr = e.mmr();
            if (e.mmr() > matchMaxMmr) matchMaxMmr = e.mmr();
        }

        // MMR 격차 경고
        int mmrGap = matchMaxMmr - matchMinMmr;
        if (mmrGap > baseRange * 2) {
            DebugLogger.report(ErrorCode.MATCH_MMR_GAP, "gap=" + mmrGap + " min=" + matchMinMmr + " max=" + matchMaxMmr);
        }

        if (entries.isEmpty()) {
            stopMatchmaking();
        }

        return matched;
    }

    // === 주기적 매칭 시도 ===

    private void startMatchmaking() {
        if (matchmakingTask != null) return;

        matchmakingTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 오프라인 플레이어 제거
                entries.entrySet().removeIf(e -> !e.getValue().player().isOnline());

                if (entries.isEmpty()) {
                    stopMatchmaking();
                    return;
                }

                // 스코어보드 업데이트
                updateAllScoreboards();

                // 매칭 시도
                int minPlayers = plugin.getSettings().getMinPlayers();
                if (entries.size() >= minPlayers) {
                    List<Player> matched = findMatch();
                    if (matched != null && !matched.isEmpty()) {
                        startCountdown(matched);
                    }
                }
            }
        };
        matchmakingTask.runTaskTimer(plugin, 20L, 20L); // 1초마다

        // 액션바 업데이트 (더 자주)
        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (entries.isEmpty()) return;
                long now = System.currentTimeMillis();
                for (QueueEntry e : entries.values()) {
                    if (!e.player().isOnline()) continue;
                    int waitSec = (int) ((now - e.joinedAt()) / 1000);
                    String bar = ChatColor.YELLOW + "\u23F3 "
                            + plugin.getMessageManager().get(bowshot.lang.MessageKey.MATCH_QUEUE_ACTIONBAR,
                                    waitSec, entries.size());
                    e.player().spigot().sendMessage(
                            net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            net.md_5.bungee.api.chat.TextComponent.fromLegacy(bar));
                }
            }
        };
        actionBarTask.runTaskTimer(plugin, 0L, 20L); // 1초마다
    }

    private void stopMatchmaking() {
        if (matchmakingTask != null) {
            try { matchmakingTask.cancel(); } catch (IllegalStateException ignored) {}
            matchmakingTask = null;
        }
        if (actionBarTask != null) {
            try { actionBarTask.cancel(); } catch (IllegalStateException ignored) {}
            actionBarTask = null;
        }
    }

    private void startCountdown(List<Player> matched) {
        for (Player p : matched) {
            p.sendMessage(plugin.getMessageManager().get(MessageKey.MATCH_COUNTDOWN_START, 0));
        }
        plugin.getGameManager().startGame(matched);
    }

    // === 대기 중 스코어보드 ===

    private void showQueueScoreboard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("bsqueue", "dummy",
                ChatColor.GOLD + "⚔ Bowshot 매칭");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        QueueEntry entry = entries.get(player.getUniqueId());
        int mmr = entry != null ? entry.mmr() : 0;

        obj.getScore(ChatColor.YELLOW + plugin.getMessageManager().get(bowshot.lang.MessageKey.MATCH_QUEUE_WAITING, entries.size())).setScore(4);
        obj.getScore(" ").setScore(3);
        obj.getScore(ChatColor.GRAY + plugin.getMessageManager().get(bowshot.lang.MessageKey.MATCH_QUEUE_SEARCHING_1)).setScore(2);
        obj.getScore(ChatColor.GRAY + plugin.getMessageManager().get(bowshot.lang.MessageKey.MATCH_QUEUE_SEARCHING_2)).setScore(1);

        player.setScoreboard(board);
    }

    private void updateAllScoreboards() {
        long now = System.currentTimeMillis();
        for (QueueEntry e : entries.values()) {
            if (!e.player().isOnline()) continue;
            Scoreboard board = e.player().getScoreboard();
            Objective obj = board.getObjective("bsqueue");
            if (obj == null) {
                showQueueScoreboard(e.player());
                continue;
            }

            // 기존 항목 초기화
            for (String entry : board.getEntries()) {
                board.resetScores(entry);
            }

            int waitSec = (int) ((now - e.joinedAt()) / 1000);
            obj.getScore(ChatColor.YELLOW + plugin.getMessageManager().get(bowshot.lang.MessageKey.MATCH_QUEUE_WAITING, entries.size())).setScore(5);
            obj.getScore(ChatColor.YELLOW + plugin.getMessageManager().get(bowshot.lang.MessageKey.MATCH_QUEUE_ACTIONBAR, waitSec, entries.size())).setScore(4);
            obj.getScore(" ").setScore(3);
            obj.getScore(ChatColor.GRAY + plugin.getMessageManager().get(bowshot.lang.MessageKey.MATCH_QUEUE_SEARCHING_1)).setScore(2);
            obj.getScore(ChatColor.GRAY + plugin.getMessageManager().get(bowshot.lang.MessageKey.MATCH_QUEUE_SEARCHING_2)).setScore(1);
        }
    }

    private void clearQueueScoreboard(Player player) {
        if (player.isOnline()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            // 로비 스코어보드 복원
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && !plugin.getGameManager().isPlayerPlaying(player)
                        && !isInQueue(player)) {
                    plugin.getLobbyScoreboard().applyLobbyBuffs(player);
                    plugin.getLobbyScoreboard().show(player);
                }
            }, 3L);
        }
    }

    // === 큐 엔트리 ===

    public record QueueEntry(Player player, int mmr, long joinedAt) {}

    // === 진단용 ===

    /**
     * 대기 시간 통계. [0]=평균(ms), [1]=최대(ms). 비어있으면 null.
     */
    public long[] getWaitTimeStats() {
        if (entries.isEmpty()) return null;
        long now = System.currentTimeMillis();
        long total = 0;
        long max = 0;
        for (QueueEntry e : entries.values()) {
            long wait = now - e.joinedAt();
            total += wait;
            if (wait > max) max = wait;
        }
        return new long[]{total / entries.size(), max};
    }

    /**
     * 대기열 내 MMR 범위. [0]=최소, [1]=최대. 비어있으면 null.
     */
    public int[] getMmrRange() {
        if (entries.isEmpty()) return null;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (QueueEntry e : entries.values()) {
            if (e.mmr() < min) min = e.mmr();
            if (e.mmr() > max) max = e.mmr();
        }
        return new int[]{min, max};
    }
}
