package bowshot.score;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;

import bowshot.Bowshot;

public class RankingHologram {

    private final Bowshot plugin;

    public RankingHologram(Bowshot plugin) {
        this.plugin = plugin;
    }

    public List<String> buildRankingLines() {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GREEN + "RANKING");

        Map<String, Integer> topPlayers = plugin.getStatsManager().getTopPlayers(10);
        int rank = 1;

        for (Map.Entry<String, Integer> entry : topPlayers.entrySet()) {
            String playerName = getPlayerName(entry.getKey());
            int elo = entry.getValue();
            String prefix;

            switch (rank) {
                case 1: prefix = ChatColor.GOLD.toString(); break;
                case 2: prefix = ChatColor.GRAY.toString(); break;
                case 3: prefix = ChatColor.DARK_PURPLE.toString(); break;
                default: prefix = ChatColor.WHITE.toString(); break;
            }

            lines.add(prefix + rank + " " + playerName);
            rank++;
        }

        return lines;
    }

    private String getPlayerName(String uuid) {
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            return player.getName() != null ? player.getName() : uuid;
        } catch (Exception e) {
            return uuid;
        }
    }
}
