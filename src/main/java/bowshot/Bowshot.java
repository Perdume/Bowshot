package bowshot;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import bowshot.anticheat.AntiCheat;
import bowshot.arena.ArenaManager;
import bowshot.command.AdminCommand;
import bowshot.command.AdminTabCompleter;
import bowshot.command.UserCommand;
import bowshot.command.UserTabCompleter;
import bowshot.config.Settings;
import bowshot.cosmetic.TitleEffectManager;
import bowshot.data.DataFile;
import bowshot.debug.DebugLogger;
import bowshot.debug.ErrorCode;
import bowshot.debug.TaskDebugger;
import bowshot.game.GameListener;
import bowshot.game.GameManager;
import bowshot.game.JoinSpectatorGui;
import bowshot.game.ScoreboardHelper;
import bowshot.game.SpectatorGui;
import bowshot.lang.MessageManager;
import bowshot.lobby.LobbyScoreboard;
import bowshot.match.MatchQueue;
import bowshot.mission.AchievementManager;
import bowshot.mission.MissionManager;
import bowshot.player.PlayerManager;
import bowshot.replay.ReplayGui;
import bowshot.replay.ReplayManager;
import bowshot.score.RankingHologram;
import bowshot.stats.StatsManager;
import bowshot.world.InstanceWorldManager;
import bowshot.world.SpawnManager;
import bowshot.world.VisitArenaGui;
import bowshot.world.WorldEditorGui;
import bowshot.world.WorldLoaderGui;

public class Bowshot extends JavaPlugin {

    private static Bowshot instance;

    private Settings settings;
    private MessageManager messageManager;
    private DataFile locationData;
    private DataFile scoreData;
    private DataFile worldData;

    private PlayerManager playerManager;
    private ArenaManager arenaManager;
    private GameManager gameManager;
    private InstanceWorldManager instanceWorldManager;
    private SpawnManager spawnManager;
    private StatsManager statsManager;
    private TaskDebugger taskDebugger;
    private MatchQueue matchQueue;
    private RankingHologram rankingHologram;
    private MissionManager missionManager;
    private AchievementManager achievementManager;
    private TitleEffectManager titleEffectManager;
    private LobbyScoreboard lobbyScoreboard;

    private WorldLoaderGui worldLoaderGui;
    private WorldEditorGui worldEditorGui;
    private JoinSpectatorGui joinSpectatorGui;
    private ScoreboardHelper scoreboardHelper;
    private SpectatorGui spectatorGui;

    private ReplayManager replayManager;
    private ReplayGui replayGui;
    private VisitArenaGui visitArenaGui;
    private AntiCheat antiCheat;

    @Override
    public void onEnable() {
        instance = this;

        // Config & language
        settings = new Settings(this);
        messageManager = new MessageManager(this);
        messageManager.load(settings.getLanguage());
        DebugLogger.log("Init", "Config & language loaded (debug=" + settings.isDebug() + ", lang=" + settings.getLanguage() + ")");

        // Data files
        locationData = new DataFile(this, "location.yml");
        scoreData = new DataFile(this, "score.yml");
        worldData = new DataFile(this, "world.yml");
        DebugLogger.log("Init", "Data files loaded");

        // Managers
        playerManager = new PlayerManager();
        arenaManager = new ArenaManager(this);
        instanceWorldManager = new InstanceWorldManager(this);
        spawnManager = new SpawnManager(this);
        gameManager = new GameManager(this);
        statsManager = new StatsManager(this);
        missionManager = new MissionManager(this);
        achievementManager = new AchievementManager(this);
        titleEffectManager = new TitleEffectManager(this);
        lobbyScoreboard = new LobbyScoreboard(this);
        taskDebugger = new TaskDebugger();
        matchQueue = new MatchQueue(this);
        rankingHologram = new RankingHologram(this);
        DebugLogger.log("Init", "Managers initialized");

        // GUIs
        scoreboardHelper = new ScoreboardHelper(this);
        spectatorGui = new SpectatorGui(this);
        worldLoaderGui = new WorldLoaderGui(this);
        worldEditorGui = new WorldEditorGui(this);
        joinSpectatorGui = new JoinSpectatorGui(this);
        DebugLogger.log("Init", "GUIs initialized");

        // Replay
        replayManager = new ReplayManager(this);
        replayGui = new ReplayGui(this);
        visitArenaGui = new VisitArenaGui(this);
        DebugLogger.log("Init", "Replay system initialized");

        // Register events
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(spectatorGui, this);
        getServer().getPluginManager().registerEvents(worldLoaderGui, this);
        getServer().getPluginManager().registerEvents(worldEditorGui, this);
        getServer().getPluginManager().registerEvents(joinSpectatorGui, this);
        getServer().getPluginManager().registerEvents(replayGui, this);
        getServer().getPluginManager().registerEvents(visitArenaGui, this);
        getServer().getPluginManager().registerEvents(lobbyScoreboard, this);
        lobbyScoreboard.start();

        // Anti-cheat
        antiCheat = new AntiCheat(this);
        if (settings.isAcEnabled()) {
            getServer().getPluginManager().registerEvents(antiCheat, this);
            DebugLogger.log("Init", "AntiCheat enabled");
        }

        // Register commands
        PluginCommand bsCmd = getCommand("bs");
        if (bsCmd != null) {
            bsCmd.setExecutor(new UserCommand(this));
            bsCmd.setTabCompleter(new UserTabCompleter());
            DebugLogger.log("Init", "Registered /bs command");
        } else {
            DebugLogger.report(ErrorCode.INIT_COMMAND_REGISTER_FAIL, "/bs");
        }

        PluginCommand bsaCmd = getCommand("bsa");
        if (bsaCmd != null) {
            bsaCmd.setExecutor(new AdminCommand(this));
            bsaCmd.setTabCompleter(new AdminTabCompleter(this));
            DebugLogger.log("Init", "Registered /bsa command");
        } else {
            DebugLogger.report(ErrorCode.INIT_COMMAND_REGISTER_FAIL, "/bsa");
        }

        // Load arenas from data
        arenaManager.loadArenas();
        DebugLogger.log("Init", "Arenas loaded: " + arenaManager.getArenaNames());

        // Start auto cleanup
        gameManager.startAutoCleanup();

        getLogger().info("Bowshot enabled." + (settings.isDebug() ? " [DEBUG MODE]" : ""));
    }

    @Override
    public void onDisable() {
        // Stop lobby scoreboard
        if (lobbyScoreboard != null) {
            lobbyScoreboard.stop();
        }
        // Stop active replays
        if (replayManager != null) {
            replayManager.stopAll();
        }
        // Clean up instance worlds
        if (instanceWorldManager != null) {
            instanceWorldManager.removeAllWorlds();
        }
        getLogger().info("Bowshot disabled.");
    }

    public static Bowshot getInstance() {
        return instance;
    }

    public Settings getSettings() { return settings; }
    public MessageManager getMessageManager() { return messageManager; }
    public DataFile getLocationData() { return locationData; }
    public DataFile getScoreData() { return scoreData; }
    public DataFile getWorldData() { return worldData; }

    public PlayerManager getPlayerManager() { return playerManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public GameManager getGameManager() { return gameManager; }
    public InstanceWorldManager getInstanceWorldManager() { return instanceWorldManager; }
    public SpawnManager getSpawnManager() { return spawnManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public TaskDebugger getTaskDebugger() { return taskDebugger; }
    public MatchQueue getMatchQueue() { return matchQueue; }
    public RankingHologram getRankingHologram() { return rankingHologram; }
    public MissionManager getMissionManager() { return missionManager; }
    public AchievementManager getAchievementManager() { return achievementManager; }
    public TitleEffectManager getTitleEffectManager() { return titleEffectManager; }
    public LobbyScoreboard getLobbyScoreboard() { return lobbyScoreboard; }

    public WorldLoaderGui getWorldLoaderGui() { return worldLoaderGui; }
    public WorldEditorGui getWorldEditorGui() { return worldEditorGui; }
    public JoinSpectatorGui getJoinSpectatorGui() { return joinSpectatorGui; }
    public ScoreboardHelper getScoreboardHelper() { return scoreboardHelper; }
    public SpectatorGui getSpectatorGui() { return spectatorGui; }

    public ReplayManager getReplayManager() { return replayManager; }
    public ReplayGui getReplayGui() { return replayGui; }
    public VisitArenaGui getVisitArenaGui() { return visitArenaGui; }
    public AntiCheat getAntiCheat() { return antiCheat; }
}
