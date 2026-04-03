package bowshot.api;

import bowshot.Bowshot;
import bowshot.arena.ArenaInstance;
import bowshot.game.Game;
import bowshot.stats.PlayerStats;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BowshotAPI {

    public static boolean isPlaying(Player player) {
        return Bowshot.getInstance().getGameManager().isPlayerPlaying(player);
    }

    public static Game getGame(Player player) {
        return Bowshot.getInstance().getGameManager().getGameByPlayer(player);
    }

    public static ArenaInstance getArenaInstance(Player player) {
        Game game = getGame(player);
        return game != null ? game.getArenaInstance() : null;
    }

    public static PlayerStats getPlayerStats(UUID uuid) {
        return Bowshot.getInstance().getStatsManager().getStats(uuid);
    }
}
