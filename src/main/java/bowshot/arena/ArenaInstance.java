package bowshot.arena;

import bowshot.Bowshot;
import bowshot.player.GamePlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ArenaInstance {

    private final String worldName;
    private Location spawnLocation;
    private final List<GamePlayer> players = new ArrayList<>();

    public ArenaInstance(String worldName, Location spawnLocation) {
        this.worldName = worldName;
        this.spawnLocation = spawnLocation;
    }

    public String getWorldName() {
        return worldName;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public void addPlayer(Player player) {
        Bowshot plugin = Bowshot.getInstance();
        GamePlayer gp = plugin.getPlayerManager().getPlayer(player.getUniqueId());
        if (!players.contains(gp)) {
            players.add(gp);
        }
    }

    public void removePlayer(Player player) {
        Bowshot plugin = Bowshot.getInstance();
        players.remove(plugin.getPlayerManager().getPlayer(player.getUniqueId()));
    }

    public List<GamePlayer> getPlayers() {
        return players;
    }

    public void clearPlayers() {
        players.clear();
    }

    public List<GamePlayer> getSpectators() {
        return players.stream()
                .filter(GamePlayer::isSpectator)
                .collect(Collectors.toList());
    }

    public int getAliveCount() {
        return players.size() - getSpectators().size();
    }
}
