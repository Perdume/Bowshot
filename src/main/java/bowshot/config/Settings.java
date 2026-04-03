package bowshot.config;

import org.bukkit.configuration.file.FileConfiguration;

import bowshot.Bowshot;

public class Settings {

    private int minPlayers;
    private int maxPlayers;
    private int matchCountdown;
    private int gamePreparationTime;
    private int gameDuration;
    private int glowingTime;
    private int baseMmr;
    private int placementBase;
    private int killBase;
    private int mvpBonus;
    private int entryRate;
    private int gainScaling;
    private double passivePenalty;
    private double smallGameFactor;
    private double timeoutFactor;
    private int disconnectPenalty;
    private int newbieThreshold;
    private double newbieLossReduction;
    private String language;

    // Spectator settings
    private boolean spectatorAllowJoin;
    private int spectatorFlySpeed;
    private boolean spectatorInvisible;
    private boolean spectatorShowItems;
    private boolean spectatorTeleportOnDeath;
    private boolean spectatorNightVision;

    // Replay settings
    private boolean replayEnabled;
    private boolean replayAutoRecord;
    private int replayMaxReplays;
    private int replayRecordingInterval;

    // Matchmaking settings
    private int matchmakingMmrRange;
    private int matchmakingMmrExpandPerSec;
    private int matchmakingMaxWaitSeconds;

    // Anti-cheat settings
    private boolean acEnabled;
    private double acMaxReach;
    private double acMaxSpeed;
    private int acMaxCps;
    private int acViolationThreshold;

    // Debug
    private boolean debug;

    public Settings(Bowshot plugin) {
        plugin.saveDefaultConfig();
        load(plugin.getConfig());
    }

    public void load(FileConfiguration config) {
        this.minPlayers = config.getInt("game.min-players", 2);
        this.maxPlayers = config.getInt("game.max-players", 8);
        this.matchCountdown = config.getInt("game.match-countdown", 20);
        this.gamePreparationTime = config.getInt("game.preparation-time", 15);
        this.gameDuration = config.getInt("game.duration", 315);
        this.glowingTime = config.getInt("game.glowing-time", 180);
        this.baseMmr = config.getInt("score.base-mmr", 0);
        this.placementBase = config.getInt("score.placement-base", 25);
        this.killBase = config.getInt("score.kill-base", 5);
        this.mvpBonus = config.getInt("score.mvp-bonus", 8);
        this.entryRate = config.getInt("score.entry-rate", 3);
        this.gainScaling = config.getInt("score.gain-scaling", 500);
        this.passivePenalty = config.getDouble("score.passive-penalty", 0.5);
        this.smallGameFactor = config.getDouble("score.small-game-factor", 0.7);
        this.timeoutFactor = config.getDouble("score.timeout-factor", 0.6);
        this.disconnectPenalty = config.getInt("score.disconnect-penalty", 15);
        this.newbieThreshold = config.getInt("score.newbie-threshold", 100);
        this.newbieLossReduction = config.getDouble("score.newbie-loss-reduction", 0.3);
        this.language = config.getString("language", "ko");

        // Spectator
        this.spectatorAllowJoin = config.getBoolean("spectator.allow-join", true);
        this.spectatorFlySpeed = config.getInt("spectator.fly-speed", 1);
        this.spectatorInvisible = config.getBoolean("spectator.invisible", true);
        this.spectatorShowItems = config.getBoolean("spectator.show-items", true);
        this.spectatorTeleportOnDeath = config.getBoolean("spectator.teleport-on-death", true);
        this.spectatorNightVision = config.getBoolean("spectator.night-vision", true);

        // Replay
        this.replayEnabled = config.getBoolean("replay.enabled", true);
        this.replayAutoRecord = config.getBoolean("replay.auto-record", true);
        this.replayMaxReplays = config.getInt("replay.max-replays", 50);
        this.replayRecordingInterval = config.getInt("replay.recording-interval", 4);

        // Matchmaking
        this.matchmakingMmrRange = config.getInt("matchmaking.mmr-range", 200);
        this.matchmakingMmrExpandPerSec = config.getInt("matchmaking.mmr-expand-per-sec", 5);
        this.matchmakingMaxWaitSeconds = config.getInt("matchmaking.max-wait-seconds", 60);

        // Anti-cheat
        this.acEnabled = config.getBoolean("anticheat.enabled", true);
        this.acMaxReach = config.getDouble("anticheat.max-reach", 4.5);
        this.acMaxSpeed = config.getDouble("anticheat.max-speed", 15.0);
        this.acMaxCps = config.getInt("anticheat.max-cps", 20);
        this.acViolationThreshold = config.getInt("anticheat.violation-threshold", 10);

        // Debug
        this.debug = config.getBoolean("debug", false);
        bowshot.debug.DebugLogger.setEnabled(this.debug);
    }

    public int getMinPlayers() { return minPlayers; }
    public int getMaxPlayers() { return maxPlayers; }
    public int getMatchCountdown() { return matchCountdown; }
    public int getGamePreparationTime() { return gamePreparationTime; }
    public int getGameDurationTicks() { return gameDuration * 20; }
    public int getGlowingTimeTicks() { return glowingTime * 20; }
    public int getBaseMmr() { return baseMmr; }
    public int getPlacementBase() { return placementBase; }
    public int getKillBase() { return killBase; }
    public int getMvpBonus() { return mvpBonus; }
    public int getEntryRate() { return entryRate; }
    public int getGainScaling() { return gainScaling; }
    public double getPassivePenalty() { return passivePenalty; }
    public double getSmallGameFactor() { return smallGameFactor; }
    public double getTimeoutFactor() { return timeoutFactor; }
    public int getDisconnectPenalty() { return disconnectPenalty; }
    public int getNewbieThreshold() { return newbieThreshold; }
    public double getNewbieLossReduction() { return newbieLossReduction; }
    public String getLanguage() { return language; }

    // Spectator getters
    public boolean isSpectatorAllowJoin() { return spectatorAllowJoin; }
    public int getSpectatorFlySpeed() { return spectatorFlySpeed; }
    public boolean isSpectatorInvisible() { return spectatorInvisible; }
    public boolean isSpectatorShowItems() { return spectatorShowItems; }
    public boolean isSpectatorTeleportOnDeath() { return spectatorTeleportOnDeath; }
    public boolean isSpectatorNightVision() { return spectatorNightVision; }

    // Replay getters
    public boolean isReplayEnabled() { return replayEnabled; }
    public boolean isReplayAutoRecord() { return replayAutoRecord; }
    public int getReplayMaxReplays() { return replayMaxReplays; }
    public int getReplayRecordingInterval() { return replayRecordingInterval; }

    // Matchmaking getters
    public int getMatchmakingMmrRange() { return matchmakingMmrRange; }
    public int getMatchmakingMmrExpandPerSec() { return matchmakingMmrExpandPerSec; }
    public int getMatchmakingMaxWaitSeconds() { return matchmakingMaxWaitSeconds; }

    // Anti-cheat getters
    public boolean isAcEnabled() { return acEnabled; }
    public double getAcMaxReach() { return acMaxReach; }
    public double getAcMaxSpeed() { return acMaxSpeed; }
    public int getAcMaxCps() { return acMaxCps; }
    public int getAcViolationThreshold() { return acViolationThreshold; }

    // Debug
    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }
}
