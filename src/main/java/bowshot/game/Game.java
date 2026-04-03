package bowshot.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import bowshot.Bowshot;
import bowshot.arena.ArenaInstance;
import bowshot.debug.DebugLogger;
import bowshot.debug.ErrorCode;
import bowshot.debug.TaskState;
import static bowshot.debug.TaskState.RUNNING;
import static bowshot.debug.TaskState.STOPPED;
import bowshot.lang.MessageKey;
import bowshot.player.GamePlayer;
import bowshot.replay.ReplayRecorder;
import bowshot.score.MmrCalculator;

public class Game {

    private final ArenaInstance arenaInstance;
    private final Bowshot plugin;
    private final GameTimer timer = new GameTimer();
    private final MmrCalculator mmrCalculator;
    private final Map<Player, Integer> killCounts = new HashMap<>();
    private final Map<Player, Double> killScores = new HashMap<>();
    private final Map<Player, Integer> bowKills = new HashMap<>();
    private final Map<Player, Integer> meleeKills = new HashMap<>();
    private final List<TaskState> taskStates = new ArrayList<>(Arrays.asList(RUNNING, RUNNING));
    private final List<Player> deathOrder = new ArrayList<>();
    private final List<Player> disconnectedPlayers = new ArrayList<>();
    private final Map<Player, List<Player>> killLog = new HashMap<>();
    private final Map<UUID, Integer> startingScores = new HashMap<>();

    private GameState state = GameState.WAITING;
    private boolean ended = false;
    private ReplayRecorder replayRecorder;

    private final ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
    private final Scoreboard scoreboard = scoreboardManager.getNewScoreboard();

    public Game(Bowshot plugin, ArenaInstance arenaInstance) {
        this.plugin = plugin;
        this.arenaInstance = arenaInstance;
        this.mmrCalculator = new MmrCalculator(plugin, this);
    }

    // === Kill Tracking ===

    public Map<Player, Integer> getKillCounts() {
        return killCounts;
    }

    public void addKill(Player killer) {
        killCounts.merge(killer, 1, Integer::sum);
    }

    public int getKillCount(Player player) {
        return killCounts.getOrDefault(player, 0);
    }

    public Map<Player, Double> getKillScores() {
        return killScores;
    }

    public double getKillScore(Player player) {
        return killScores.getOrDefault(player, 0.0);
    }

    public void setKillScore(Player player, double score) {
        killScores.put(player, score);
    }

    public void addBowKill(Player killer) {
        bowKills.merge(killer, 1, Integer::sum);
    }

    public void addMeleeKill(Player killer) {
        meleeKills.merge(killer, 1, Integer::sum);
    }

    public int getBowKills(Player player) {
        return bowKills.getOrDefault(player, 0);
    }

    public int getMeleeKills(Player player) {
        return meleeKills.getOrDefault(player, 0);
    }

    // === Death & Placement Tracking ===

    public void recordDeath(Player player) {
        if (!deathOrder.contains(player)) {
            deathOrder.add(player);
        }
    }

    public void recordDisconnect(Player player) {
        if (!disconnectedPlayers.contains(player)) {
            disconnectedPlayers.add(player);
        }
        recordDeath(player);
    }

    public boolean isDisconnected(Player player) {
        return disconnectedPlayers.contains(player);
    }

    public void addKillLog(Player killer, Player victim) {
        killLog.computeIfAbsent(killer, k -> new ArrayList<>()).add(victim);
    }

    public List<Player> getKillVictims(Player killer) {
        return killLog.getOrDefault(killer, List.of());
    }

    public int getStartingScore(UUID uuid) {
        return startingScores.getOrDefault(uuid, 0);
    }

    public Map<Player, Integer> calculatePlacements() {
        Map<Player, Integer> placements = new HashMap<>();
        int totalPlayers = arenaInstance.getPlayers().size();

        // Dead players: first to die = worst rank (totalPlayers), last to die = deathOrder.size()
        for (int i = 0; i < deathOrder.size(); i++) {
            placements.put(deathOrder.get(i), totalPlayers - i);
        }

        // Alive players: ranked by kill score (best = rank 1)
        List<GamePlayer> alive = new ArrayList<>();
        for (GamePlayer gp : arenaInstance.getPlayers()) {
            if (!gp.isSpectator() && !placements.containsKey(gp.getPlayer())) {
                alive.add(gp);
            }
        }

        alive.sort((a, b) -> Double.compare(getKillScore(b.getPlayer()), getKillScore(a.getPlayer())));

        for (int i = 0; i < alive.size(); i++) {
            placements.put(alive.get(i).getPlayer(), i + 1);
        }

        return placements;
    }

    // === Game Lifecycle ===

    public void start(List<Player> playerList) {
        DebugLogger.log("Game", "Game.start() arena=" + arenaInstance.getWorldName() + " players=" + playerList.size());
        plugin.getGameManager().getActiveGames().add(this);
        state = GameState.PREPARING;

        // Disable coordinates display & locator bar BEFORE players enter the world
        arenaInstance.getSpawnLocation().getWorld().setGameRule(GameRule.REDUCED_DEBUG_INFO, true);
        arenaInstance.getSpawnLocation().getWorld().setGameRule(GameRule.LOCATOR_BAR, false);

        for (Player player : playerList) {
            arenaInstance.addPlayer(player);
        }

        timer.start();
        ended = false;

        // Store starting scores for kill quality calculation
        for (GamePlayer gp : arenaInstance.getPlayers()) {
            startingScores.put(gp.getUniqueId(), plugin.getStatsManager().getMmr(gp.getUniqueId()));
        }

        // Start replay recording
        if (plugin.getSettings().isReplayEnabled() && plugin.getSettings().isReplayAutoRecord()) {
            replayRecorder = new ReplayRecorder(plugin, this, arenaInstance.getWorldName());
            replayRecorder.start();
        }

        for (GamePlayer gp : arenaInstance.getPlayers()) {
            Player player = gp.getPlayer();
            player.setHealth(20);
            player.setFoodLevel(20);
            player.getInventory().clear();
            gp.setSpectator(false);
            player.teleport(arenaInstance.getSpawnLocation());
            player.setGameMode(GameMode.ADVENTURE);
            initializeScoreboard(player);

            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 10 * 20, 2, true));
            if (player.hasPermission("bowshot.admin")) {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.GAME_SCORE_DISPLAY,
                        plugin.getStatsManager().getMmr(gp.getUniqueId())));
            }
            plugin.getScoreboardHelper().joinTeam(player, scoreboard);
        }

        // Hide non-participating players (cosmetics isolation)
        hideNonParticipants();

        startPreparationPhase();
    }

    private void startPreparationPhase() {
        plugin.getTaskDebugger().register(this, taskStates);
        int prepTime = plugin.getSettings().getGamePreparationTime();

        broadcast(plugin.getMessageManager().get(MessageKey.GAME_PREPARING, prepTime));

        // Hide all players from each other
        for (GamePlayer gp1 : arenaInstance.getPlayers()) {
            for (GamePlayer gp2 : arenaInstance.getPlayers()) {
                if (gp1 != gp2) {
                    gp1.getPlayer().setCollidable(true);
                    gp1.getPlayer().hidePlayer(plugin, gp2.getPlayer());
                }
            }
        }

        // Time event task
        new BukkitRunnable() {
            @Override
            public void run() {
                if (ended) {
                    taskStates.set(1, STOPPED);
                    cancel();
                    return;
                }
                processTimeEvents();
            }
        }.runTaskTimer(plugin, 1L, 1L);

        // Scoreboard update task
        new BukkitRunnable() {
            @Override
            public void run() {
                if (ended) {
                    taskStates.set(0, STOPPED);
                    cancel();
                    return;
                }
                for (GamePlayer gp : arenaInstance.getPlayers()) {
                    updateScoreboard(gp.getPlayer());
                }
            }
        }.runTaskTimer(plugin, 4L, 4L);

        // Countdown task
        new BukkitRunnable() {
            int countdown = prepTime + 1;

            @Override
            public void run() {
                countdown--;
                if (ended) {
                    broadcast(plugin.getMessageManager().get(MessageKey.GAME_FORCE_ENDED));
                    cancel();
                    return;
                }
                if (countdown > 0 && countdown < 6) {
                    broadcast(ChatColor.RED + String.valueOf(countdown));
                }
                if (countdown == 0) {
                    state = GameState.PLAYING;
                    broadcast(plugin.getMessageManager().get(MessageKey.GAME_START));
                    showAllPlayers();
                    equipPlayers();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void hideNonParticipants() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (isPlayerPlaying(online)) continue;
            for (GamePlayer gp : arenaInstance.getPlayers()) {
                gp.getPlayer().hidePlayer(plugin, online);
                online.hidePlayer(plugin, gp.getPlayer());
            }
        }
    }

    private void showNonParticipants() {
        for (GamePlayer gp : arenaInstance.getPlayers()) {
            Player participant = gp.getPlayer();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (participant.equals(online)) continue;
                participant.showPlayer(plugin, online);
                Game otherGame = plugin.getGameManager().getGameByPlayer(online);
                if (otherGame == null || otherGame == this) {
                    online.showPlayer(plugin, participant);
                }
            }
        }
    }

    private void showAllPlayers() {
        for (GamePlayer gp1 : arenaInstance.getPlayers()) {
            for (GamePlayer gp2 : arenaInstance.getPlayers()) {
                if (gp1 != gp2 && !gp1.isSpectator() && !gp2.isSpectator()) {
                    gp1.getPlayer().showPlayer(plugin, gp2.getPlayer());
                }
            }
        }
    }

    private void equipPlayers() {
        for (GamePlayer gp : arenaInstance.getPlayers()) {
            if (!gp.isSpectator()) {
                Player player = gp.getPlayer();
                player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
                player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 64));
                player.getInventory().addItem(createBow());
                player.getInventory().addItem(createSword());
            }
        }
    }

    public void end() {
        if (ended) {
            DebugLogger.report(ErrorCode.GAME_END_ALREADY, arenaInstance.getWorldName());
            return;
        }
        ended = true;
        state = GameState.ENDING;

        // 존버 감지 로깅
        int totalKills = 0;
        for (int k : killCounts.values()) totalKills += k;
        if (totalKills == 0 && arenaInstance.getPlayers().size() >= 2) {
            DebugLogger.report(ErrorCode.GAME_ZERO_KILLS, arenaInstance.getWorldName());
        }

        // Stop and save replay
        if (replayRecorder != null && replayRecorder.isRecording()) {
            replayRecorder.stop();
            plugin.getReplayManager().saveReplay(replayRecorder.getData());
        }

        // Restore visibility of non-participating players
        showNonParticipants();

        Player winner = findLastAlive();

        // Teleport to per-map end location
        String templateName = plugin.getInstanceWorldManager().getOriginalName(arenaInstance.getWorldName());
        Location endLoc = (templateName != null)
                ? plugin.getSpawnManager().getEndLocation(templateName, arenaInstance.getSpawnLocation().getWorld())
                : arenaInstance.getSpawnLocation();
        for (GamePlayer gp : arenaInstance.getPlayers()) {
            gp.getPlayer().teleport(endLoc);
        }

        // Cleanup player states
        for (GamePlayer gp1 : arenaInstance.getPlayers()) {
            for (GamePlayer gp2 : arenaInstance.getPlayers()) {
                if (gp1 != gp2) {
                    plugin.getScoreboardHelper().leaveTeam(gp1.getPlayer(), scoreboard);
                    gp1.getPlayer().showPlayer(plugin, gp2.getPlayer());
                    gp1.getPlayer().setAllowFlight(false);
                    for (PotionEffect effect : gp1.getPlayer().getActivePotionEffects()) {
                        gp1.getPlayer().removePotionEffect(effect.getType());
                    }
                }
            }
        }

        String winnerName = winner != null ? winner.getName() : "X";
        Player mvp = findMvp();
        String mvpName = mvp != null ? mvp.getName() : null;

        for (GamePlayer gp : arenaInstance.getPlayers()) {
            Player player = gp.getPlayer();
            player.sendMessage(CenteredMessage.getCenteredMessage(
                    plugin.getMessageManager().get(MessageKey.GAME_SET_HEADER)));
            player.sendMessage(CenteredMessage.getCenteredMessage(
                    plugin.getMessageManager().get(MessageKey.GAME_SET_WINNER, winnerName)));
            if (mvpName != null) {
                player.sendMessage(CenteredMessage.getCenteredMessage(
                        plugin.getMessageManager().get(MessageKey.GAME_SET_MVP, mvpName)));
            } else {
                player.sendMessage(CenteredMessage.getCenteredMessage(
                        plugin.getMessageManager().get(MessageKey.GAME_SET_NO_MVP)));
            }
            player.sendMessage(CenteredMessage.getCenteredMessage(
                    plugin.getMessageManager().get(MessageKey.GAME_SET_FOOTER)));
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            player.getInventory().clear();
            timer.reset();
            player.sendMessage(plugin.getMessageManager().get(MessageKey.GAME_RETURN_COUNTDOWN));
        }

        // Calculate placements and apply BR scoring
        Map<Player, Integer> placements = calculatePlacements();
        int totalPlayers = arenaInstance.getPlayers().size();
        for (GamePlayer gp : arenaInstance.getPlayers()) {
            int placement = placements.getOrDefault(gp.getPlayer(), totalPlayers);
            boolean disconnected = isDisconnected(gp.getPlayer());
            mmrCalculator.calculateAndApply(gp, placement, totalPlayers, false, disconnected);
        }

        // Process missions and achievements
        processMissionsAndAchievements(placements, totalPlayers, mvp, false);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            for (Player pl : arenaInstance.getSpawnLocation().getWorld().getPlayers()) {
                GamePlayer gp = plugin.getPlayerManager().getPlayer(pl);
                gp.setSpectator(false);
                plugin.getPlayerManager().removePlayer(pl);
            }
            arenaInstance.clearPlayers();
            plugin.getGameManager().removeGame(arenaInstance);
            plugin.getGameManager().getActiveGames().remove(this);
            plugin.getInstanceWorldManager().removeWorld(
                    Bukkit.getWorld(arenaInstance.getWorldName()));

            // 로비 스코어보드 복원 (월드 이동 후)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!plugin.getGameManager().isPlayerPlaying(online)
                            && !plugin.getMatchQueue().isInQueue(online)
                            && !plugin.getReplayManager().isViewing(online)
                            && !plugin.getLobbyScoreboard().hasLobbyBoard(online)) {
                        plugin.getLobbyScoreboard().applyLobbyBuffs(online);
                        plugin.getLobbyScoreboard().show(online);
                    }
                }
            }, 5L);
        }, 60);
    }

    public void endByTimeout(Player mvp) {
        if (ended) return;
        ended = true;
        state = GameState.ENDING;

        // Stop and save replay
        if (replayRecorder != null && replayRecorder.isRecording()) {
            replayRecorder.stop();
            plugin.getReplayManager().saveReplay(replayRecorder.getData());
        }

        // Restore visibility of non-participating players
        showNonParticipants();

        Player winner = mvp;

        for (GamePlayer gp1 : arenaInstance.getPlayers()) {
            for (GamePlayer gp2 : arenaInstance.getPlayers()) {
                if (gp1 != gp2) {
                    plugin.getScoreboardHelper().leaveTeam(gp1.getPlayer(), scoreboard);
                    gp1.getPlayer().showPlayer(plugin, gp2.getPlayer());
                    gp1.getPlayer().setAllowFlight(false);
                    for (PotionEffect effect : gp1.getPlayer().getActivePotionEffects()) {
                        gp1.getPlayer().removePotionEffect(effect.getType());
                    }
                }
            }
        }

        String winnerName = winner != null ? winner.getName() : "X";
        String mvpName = winner != null ? winner.getName() : null;

        for (GamePlayer gp : arenaInstance.getPlayers()) {
            Player player = gp.getPlayer();
            if (winner != null) {
                player.sendMessage(ChatColor.GREEN + "winner: " + winnerName);
                player.sendMessage(ChatColor.GOLD + "MVP: " + mvpName);
            } else {
                player.sendMessage(ChatColor.GREEN + "winner: X");
                player.sendMessage(ChatColor.GOLD + "MVP: X");
            }
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            player.getInventory().clear();
            timer.reset();
            player.sendMessage(plugin.getMessageManager().get(MessageKey.GAME_RETURN_COUNTDOWN));
        }

        // Calculate placements and apply BR scoring (timeout)
        Map<Player, Integer> placements = calculatePlacements();
        int totalPlayers = arenaInstance.getPlayers().size();
        for (GamePlayer gp : arenaInstance.getPlayers()) {
            int placement = placements.getOrDefault(gp.getPlayer(), totalPlayers);
            boolean disconnected = isDisconnected(gp.getPlayer());
            mmrCalculator.calculateAndApply(gp, placement, totalPlayers, true, disconnected);
        }

        // Process missions and achievements
        processMissionsAndAchievements(placements, totalPlayers, mvp, true);

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            for (Player pl : arenaInstance.getSpawnLocation().getWorld().getPlayers()) {
                GamePlayer gp = plugin.getPlayerManager().getPlayer(pl);
                gp.setSpectator(false);
                plugin.getPlayerManager().removePlayer(pl);
            }
            arenaInstance.clearPlayers();
            plugin.getGameManager().removeGame(arenaInstance);
            plugin.getGameManager().getActiveGames().remove(this);
            plugin.getInstanceWorldManager().removeWorld(
                    Bukkit.getWorld(arenaInstance.getWorldName()));

            // 로비 스코어보드 복원 (월드 이동 후)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!plugin.getGameManager().isPlayerPlaying(online)
                            && !plugin.getMatchQueue().isInQueue(online)
                            && !plugin.getReplayManager().isViewing(online)
                            && !plugin.getLobbyScoreboard().hasLobbyBoard(online)) {
                        plugin.getLobbyScoreboard().applyLobbyBuffs(online);
                        plugin.getLobbyScoreboard().show(online);
                    }
                }
            }, 5L);
        }, 60);
    }

    // === Time Events ===

    private void processTimeEvents() {
        int gameDuration = plugin.getSettings().getGameDurationTicks();
        int remaining = timer.getRemainingTicks(gameDuration);
        int glowingTicks = plugin.getSettings().getGlowingTimeTicks();

        if (remaining == glowingTicks) {
            broadcast(plugin.getMessageManager().get(MessageKey.GAME_GLOWING_WARNING));
            for (GamePlayer gp : arenaInstance.getPlayers()) {
                if (!gp.isSpectator()) {
                    gp.getPlayer().addPotionEffect(
                            new PotionEffect(PotionEffectType.GLOWING, 120000, 2, false));
                }
            }
        }

        if (remaining <= 0) {
            broadcast(plugin.getMessageManager().get(MessageKey.GAME_TIME_OVER));
            DebugLogger.report(ErrorCode.GAME_TIMEOUT, arenaInstance.getWorldName());
            endByTimeout(findMvp());
        }
    }

    // === Scoreboard ===

    private void initializeScoreboard(Player player) {
        GamePlayer gp = plugin.getPlayerManager().getPlayer(player);
        int alive = arenaInstance.getAliveCount();
        String worldName = player.getWorld().getName();
        int gameDuration = plugin.getSettings().getGameDurationTicks();

        plugin.getScoreboardHelper().setTitle("&6&oBOWSHOT", worldName, scoreboard);
        plugin.getScoreboardHelper().setLine(4, "&7&m-------------------------", worldName, scoreboard);
        plugin.getScoreboardHelper().setLine(3,
                plugin.getMessageManager().get(MessageKey.SCOREBOARD_REMAINING, alive), worldName, scoreboard);
        plugin.getScoreboardHelper().setLine(2,
                plugin.getMessageManager().get(MessageKey.SCOREBOARD_TIME, timer.getFormattedTime(gameDuration)), worldName, scoreboard);
        plugin.getScoreboardHelper().setLine(1, "&7&m-------------------------", worldName, scoreboard);
    }

    private void updateScoreboard(Player player) {
        GamePlayer gp = plugin.getPlayerManager().getPlayer(player);
        int alive = arenaInstance.getAliveCount();
        int gameDuration = plugin.getSettings().getGameDurationTicks();

        plugin.getScoreboardHelper().updateLine(scoreboard,
                plugin.getMessageManager().get(MessageKey.SCOREBOARD_REMAINING, ""),
                plugin.getMessageManager().get(MessageKey.SCOREBOARD_REMAINING, alive), 3);
        plugin.getScoreboardHelper().updateLine(scoreboard,
                plugin.getMessageManager().get(MessageKey.SCOREBOARD_TIME, ""),
                plugin.getMessageManager().get(MessageKey.SCOREBOARD_TIME, timer.getFormattedTime(gameDuration)), 2);

        checkAliveCount();
    }

    // === Mission & Achievement Processing ===

    private void processMissionsAndAchievements(Map<Player, Integer> placements, int totalPlayers,
                                                 Player mvp, boolean isTimeout) {
        var missionMgr = plugin.getMissionManager();
        var achievementMgr = plugin.getAchievementManager();
        if (missionMgr == null || achievementMgr == null) return;

        for (GamePlayer gp : arenaInstance.getPlayers()) {
            Player player = gp.getPlayer();
            UUID uuid = player.getUniqueId();
            int placement = placements.getOrDefault(player, totalPlayers);
            int kills = getKillCount(player);
            boolean isMvp = (mvp != null && mvp.equals(player));
            boolean noDeath = !deathOrder.contains(player);
            int bKills = getBowKills(player);
            int mKills = getMeleeKills(player);

            // Stats already updated by EloCalculator, just read current state
            bowshot.stats.PlayerStats stats = plugin.getStatsManager().getStats(uuid);
            int winStreak = stats.getWinStreak();

            missionMgr.onGameEnd(uuid, placement, totalPlayers, kills, isMvp,
                    isTimeout, bKills, mKills, noDeath, winStreak);
            achievementMgr.onGameEnd(uuid, placement, totalPlayers, kills, isMvp,
                    noDeath, stats.getMmr(), winStreak, kills);
        }
    }

    // === Helpers ===

    private void checkAliveCount() {
        if (arenaInstance.getAliveCount() <= 1 && arenaInstance.getPlayers().size() > 1 && !ended) {
            end();
        }
    }

    private Player findLastAlive() {
        for (GamePlayer gp : arenaInstance.getPlayers()) {
            if (!gp.isSpectator()) {
                return gp.getPlayer();
            }
        }
        return null;
    }

    private Player findMvp() {
        double maxScore = 0.0;
        Player mvp = null;
        for (GamePlayer gp : arenaInstance.getPlayers()) {
            Double score = killScores.get(gp.getPlayer());
            if (score != null && score > maxScore) {
                maxScore = score;
                mvp = gp.getPlayer();
            }
        }
        return mvp;
    }

    public void broadcast(String message) {
        for (GamePlayer gp : arenaInstance.getPlayers()) {
            gp.getPlayer().sendMessage(message);
        }
    }

    private ItemStack createBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        Enchantment power = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("power"));
        if (power != null) bow.addUnsafeEnchantment(power, 6);
        return bow;
    }

    private ItemStack createSword() {
        ItemStack sword = new ItemStack(Material.WOODEN_SWORD);
        Enchantment knockback = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("knockback"));
        if (knockback != null) sword.addUnsafeEnchantment(knockback, 1);
        Enchantment sharpness = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("sharpness"));
        if (sharpness != null) sword.addUnsafeEnchantment(sharpness, 100);
        return sword;
    }

    // === Status ===

    public boolean isPlayerPlaying(Player player) {
        for (GamePlayer gp : arenaInstance.getPlayers()) {
            if (gp.getPlayer() == player) {
                return true;
            }
        }
        return false;
    }

    public boolean isEnded() { return ended; }
    public GameState getState() { return state; }
    public ArenaInstance getArenaInstance() { return arenaInstance; }
    public Scoreboard getScoreboard() { return scoreboard; }
    public ReplayRecorder getReplayRecorder() { return replayRecorder; }

    public List<GamePlayer> getPlayers() {
        return arenaInstance.getPlayers();
    }
}
