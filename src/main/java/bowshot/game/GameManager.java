package bowshot.game;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import bowshot.Bowshot;
import bowshot.arena.Arena;
import bowshot.arena.ArenaInstance;
import bowshot.debug.DebugLogger;
import bowshot.debug.ErrorCode;
import bowshot.world.InstanceType;
import bowshot.world.WorldFileManager;

public class GameManager {

    private final Bowshot plugin;
    private final Map<ArenaInstance, Game> gameMap = new HashMap<>();
    private final List<Game> activeGames = new ArrayList<>();

    public GameManager(Bowshot plugin) {
        this.plugin = plugin;
    }

    public Game getGame(ArenaInstance instance) {
        return gameMap.get(instance);
    }

    public List<Game> getActiveGames() {
        return activeGames;
    }

    public void startGame(List<Player> players) {
        DebugLogger.log("Game", "startGame called with " + players.size() + " players");
        ArenaInstance instance = createRandomInstance();
        if (instance == null) {
            DebugLogger.report(ErrorCode.GAME_START_NO_ARENA, "players=" + players.size());
            for (Player p : players) {
                p.sendMessage("No arena available.");
            }
            return;
        }
        DebugLogger.log("Game", "Arena created: " + instance.getWorldName());
        Game game = new Game(plugin, instance);
        gameMap.put(instance, game);
        game.start(players);
    }

    public void endGame(ArenaInstance instance) {
        Game game = gameMap.get(instance);
        if (game != null) {
            DebugLogger.log("Game", "endGame: " + instance.getWorldName());
            game.end();
            gameMap.remove(instance);
        }
    }

    public void removeGame(ArenaInstance instance) {
        gameMap.remove(instance);
    }

    public boolean isPlayerPlaying(Player player) {
        for (Game game : activeGames) {
            if (game.isPlayerPlaying(player)) {
                return true;
            }
        }
        return false;
    }

    public Game getGameByPlayer(Player player) {
        for (Game game : activeGames) {
            if (game.isPlayerPlaying(player)) {
                return game;
            }
        }
        return null;
    }

    private ArenaInstance createRandomInstance() {
        Arena arena = plugin.getArenaManager().getRandomArena();
        if (arena == null) {
            DebugLogger.report(ErrorCode.ARENA_NO_AVAILABLE);
            return null;
        }

        try {
            File templateFile = arena.getTemplateFile();
            World world = plugin.getInstanceWorldManager().createInstanceWorld(templateFile, InstanceType.PLAY);
            if (world == null) {
                DebugLogger.report(ErrorCode.WORLD_CREATE_FAIL, "template=" + arena.getTemplateFolder());
                return null;
            }

            Location spawnLoc = plugin.getSpawnManager().getSpawnLocation(
                    arena.getTemplateFolder(), world);

            return plugin.getArenaManager().createInstance(world.getName(), spawnLoc);
        } catch (IOException e) {
            DebugLogger.report(ErrorCode.WORLD_CREATE_FAIL, e.getMessage());
            return null;
        }
    }

    public void startAutoCleanup() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            // Clean empty instance worlds (PLAY/VISIT only, not EDIT)
            for (World world : new ArrayList<>(plugin.getInstanceWorldManager().getActiveWorlds())) {
                if (world.getPlayers().isEmpty()) {
                    bowshot.world.InstanceType type = plugin.getInstanceWorldManager().getInstanceType(world);
                    if (type == bowshot.world.InstanceType.PLAY || type == bowshot.world.InstanceType.VISIT) {
                        plugin.getInstanceWorldManager().removeWorld(world);
                    }
                }
            }

            // Clean orphaned instance world folders from server root
            File worldContainer = Bukkit.getWorldContainer();
            File[] worldFolders = worldContainer.listFiles();
            if (worldFolders != null) {
                for (File f : worldFolders) {
                    if (f.isDirectory() && f.getName().startsWith("Bowshot--")) {
                        if (Bukkit.getWorld(f.getName()) == null) {
                            DebugLogger.report(ErrorCode.WORLD_ORPHANED, f.getName());
                            WorldFileManager.deleteRecursively(f);
                        }
                    }
                }
            }

            // Update ranking hologram
            updateRankingHologram();
        }, 20, 1200);
    }

    private boolean isActiveGameWorld(File folder) {
        for (Game game : activeGames) {
            if (game.getArenaInstance().getWorldName().equals(folder.getName())) {
                return true;
            }
        }
        return false;
    }

    private void updateRankingHologram() {
        if (Bukkit.getPluginManager().getPlugin("FancyHolograms") == null) return;
        try {
            List<String> ranking = plugin.getRankingHologram().buildRankingLines();
            bowshot.score.HologramHelper.updateRanking(ranking);
        } catch (Exception ignored) {
        }
    }
}
