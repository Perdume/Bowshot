package bowshot.player;

import bowshot.Bowshot;
import bowshot.arena.ArenaInstance;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerManager {

    private final List<GamePlayer> players = new ArrayList<>();

    public GamePlayer getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    public GamePlayer getPlayer(UUID uuid) {
        for (GamePlayer gp : players) {
            if (gp.getUniqueId().equals(uuid)) {
                return gp;
            }
        }
        GamePlayer gp = new GamePlayer(uuid);
        players.add(gp);
        return gp;
    }

    public void removePlayer(Player player) {
        players.removeIf(gp -> gp.getUniqueId().equals(player.getUniqueId()));
    }

    public void removePlayer(UUID uuid) {
        players.removeIf(gp -> gp.getUniqueId().equals(uuid));
    }

    public List<GamePlayer> getPlayersInInstance(ArenaInstance instance) {
        return instance.getPlayers();
    }
}
