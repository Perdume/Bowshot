package bowshot.lobby;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import bowshot.Bowshot;
import bowshot.mission.MissionManager;
import bowshot.mission.MissionType;
import bowshot.stats.PlayerStats;

public class LobbyScoreboard implements Listener {

    private final Bowshot plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private BukkitTask updateTask;

    public LobbyScoreboard(Bowshot plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // 5초마다 로비 스코어보드 갱신
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 100L, 100L);
    }

    public void stop() {
        if (updateTask != null) updateTask.cancel();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 로비 위치로 텔레포트
        org.bukkit.Location lobbyLoc = plugin.getSpawnManager().getLobbyLocation();
        if (lobbyLoc != null) {
            player.teleport(lobbyLoc);
        }
        // 로비 로케이터바 비활성화
        player.getWorld().setGameRule(org.bukkit.GameRule.LOCATOR_BAR, false);
        // 로비 버프 적용
        applyLobbyBuffs(player);
        // 1 tick 후 스코어보드 표시 (다른 플러그인과 충돌 방지)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !isInGameOrQueue(player)) {
                show(player);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        boards.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isInGameOrQueue(player) && !player.hasPermission("bowshot.admin")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!isInGameOrQueue(player) && !player.hasPermission("bowshot.admin")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isInGameOrQueue(player) && !player.hasPermission("bowshot.admin")) {
            // 물리적 상호작용(압력판 등)은 허용, 블럭 관련만 차단
            if (event.getAction().name().contains("BLOCK") && event.getClickedBlock() != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isInGameOrQueue(player)) {
            event.setCancelled(true);
        }
    }

    public void show(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("bslobby", "dummy",
                ChatColor.GOLD + "" + ChatColor.BOLD + "⚔ Bowshot");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        setLines(player, board, obj);
        player.setScoreboard(board);
        boards.put(player.getUniqueId(), board);
    }

    public void remove(Player player) {
        boards.remove(player.getUniqueId());
        if (player.isOnline()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
    }

    public boolean hasLobbyBoard(Player player) {
        return boards.containsKey(player.getUniqueId());
    }

    private void updateAll() {
        for (Map.Entry<UUID, Scoreboard> entry : new HashMap<>(boards).entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline() || isInGameOrQueue(player)) {
                boards.remove(entry.getKey());
                continue;
            }
            Scoreboard board = entry.getValue();
            Objective obj = board.getObjective("bslobby");
            if (obj == null) {
                show(player);
                continue;
            }
            // 기존 항목 제거 후 다시 그리기
            for (String e : board.getEntries()) {
                board.resetScores(e);
            }
            setLines(player, board, obj);
        }
    }

    private void setLines(Player player, Scoreboard board, Objective obj) {
        UUID uuid = player.getUniqueId();
        String lang = plugin.getSettings().getLanguage();
        PlayerStats stats = plugin.getStatsManager().getStats(uuid);

        // 칭호
        String titlePrefix = plugin.getTitleEffectManager().getDisplayPrefix(uuid);
        String displayName = titlePrefix.isEmpty() ? player.getName()
                : ChatColor.translateAlternateColorCodes('&', titlePrefix) + player.getName();

        int line = 15;

        obj.getScore(ChatColor.GRAY + "────────────").setScore(line--);
        obj.getScore(ChatColor.WHITE + displayName).setScore(line--);
        obj.getScore(" ").setScore(line--);

        // 통계 요약
        boolean isKo = "ko".equalsIgnoreCase(lang);
        obj.getScore(ChatColor.YELLOW + (isKo ? "승률: " : "WR: ") + ChatColor.WHITE +
                String.format("%.1f", stats.getWinRate()) + "%" +
                ChatColor.GRAY + " (" + stats.getWins() + "W " + stats.getLosses() + "L)").setScore(line--);
        obj.getScore(ChatColor.YELLOW + (isKo ? "킬/데스: " : "K/D: ") + ChatColor.WHITE +
                stats.getKills() + "/" + stats.getDeaths()).setScore(line--);
        obj.getScore("  ").setScore(line--);

        // 일일 미션
        MissionManager mm = plugin.getMissionManager();
        obj.getScore(ChatColor.GOLD + (isKo ? "▸ 일일 미션" : "▸ Daily Missions")).setScore(line--);

        List<MissionType> dailyMissions = mm.getActiveDailyMissions(uuid);
        for (MissionType m : dailyMissions) {
            int progress = mm.getProgress(uuid, m);
            boolean done = progress >= m.getGoal();
            String status;
            if (done) {
                if (mm.isClaimed(uuid, m)) {
                    status = ChatColor.DARK_GRAY + "✔ " + m.getDisplay(lang);
                } else {
                    status = ChatColor.GREEN + "✔ " + m.getDisplay(lang);
                }
            } else {
                status = ChatColor.GRAY + "  " + m.getDisplay(lang) + " " +
                        ChatColor.AQUA + progress + "/" + m.getGoal();
            }
            obj.getScore(status).setScore(line--);
        }

        obj.getScore("   ").setScore(line--);

        // 주간 미션
        obj.getScore(ChatColor.GOLD + (isKo ? "▸ 주간 미션" : "▸ Weekly Missions")).setScore(line--);

        List<MissionType> weeklyMissions = mm.getActiveWeeklyMissions(uuid);
        for (MissionType m : weeklyMissions) {
            int progress = mm.getProgress(uuid, m);
            boolean done = progress >= m.getGoal();
            String status;
            if (done) {
                if (mm.isClaimed(uuid, m)) {
                    status = ChatColor.DARK_GRAY + "✔ " + m.getDisplay(lang);
                } else {
                    status = ChatColor.GREEN + "✔ " + m.getDisplay(lang);
                }
            } else {
                status = ChatColor.GRAY + "  " + m.getDisplay(lang) + " " +
                        ChatColor.AQUA + progress + "/" + m.getGoal();
            }
            obj.getScore(status).setScore(line--);
        }

        obj.getScore(ChatColor.GRAY + "────────────" + ChatColor.RESET).setScore(line--);
    }

    private boolean isInGameOrQueue(Player player) {
        return plugin.getGameManager().isPlayerPlaying(player)
                || plugin.getMatchQueue().isInQueue(player)
                || plugin.getReplayManager().isViewing(player);
    }

    public void applyLobbyBuffs(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, Integer.MAX_VALUE, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
    }

    public void removeLobbyBuffs(Player player) {
        player.removePotionEffect(PotionEffectType.SATURATION);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
    }
}
