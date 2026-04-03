package bowshot.player;

import bowshot.Bowshot;
import bowshot.arena.ArenaInstance;
import bowshot.game.Game;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class GamePlayer {

    private final UUID uniqueId;
    private boolean spectator;

    public GamePlayer(UUID uniqueId) {
        this.uniqueId = uniqueId;
        this.spectator = false;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uniqueId);
    }

    public boolean isSpectator() {
        return spectator;
    }

    public void setSpectator(boolean spectator) {
        this.spectator = spectator;
    }

    public ArenaInstance getArenaInstance() {
        Bowshot plugin = Bowshot.getInstance();
        for (String name : plugin.getArenaManager().getInstanceNames()) {
            ArenaInstance instance = plugin.getArenaManager().getInstance(name);
            if (instance != null && instance.getPlayers().contains(this)) {
                return instance;
            }
        }
        return null;
    }

    public Game getGame() {
        Bowshot plugin = Bowshot.getInstance();
        for (Game game : plugin.getGameManager().getActiveGames()) {
            if (game.getPlayers().contains(this)) {
                return game;
            }
        }
        return null;
    }
}
