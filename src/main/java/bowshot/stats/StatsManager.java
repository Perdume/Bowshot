package bowshot.stats;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;

import bowshot.Bowshot;
import bowshot.data.DataFile;

public class StatsManager {

    private final Bowshot plugin;
    private final DataFile statsData;

    public StatsManager(Bowshot plugin) {
        this.plugin = plugin;
        this.statsData = new DataFile(plugin, "stats.yml");
    }

    public PlayerStats getStats(UUID uuid) {
        String path = "players." + uuid.toString();
        int baseMmr = plugin.getSettings().getBaseMmr();

        int wins = statsData.getConfig().getInt(path + ".wins", 0);
        int losses = statsData.getConfig().getInt(path + ".losses", 0);
        int kills = statsData.getConfig().getInt(path + ".kills", 0);
        int deaths = statsData.getConfig().getInt(path + ".deaths", 0);
        int gamesPlayed = statsData.getConfig().getInt(path + ".games-played", 0);
        int mmr = statsData.getConfig().getInt(path + ".mmr", baseMmr);
        int winStreak = statsData.getConfig().getInt(path + ".win-streak", 0);
        int maxWinStreak = statsData.getConfig().getInt(path + ".max-win-streak", 0);

        return new PlayerStats(wins, losses, kills, deaths, gamesPlayed, mmr, winStreak, maxWinStreak);
    }

    public void saveStats(UUID uuid, PlayerStats stats) {
        String path = "players." + uuid.toString();
        statsData.getConfig().set(path + ".wins", stats.getWins());
        statsData.getConfig().set(path + ".losses", stats.getLosses());
        statsData.getConfig().set(path + ".kills", stats.getKills());
        statsData.getConfig().set(path + ".deaths", stats.getDeaths());
        statsData.getConfig().set(path + ".games-played", stats.getGamesPlayed());
        statsData.getConfig().set(path + ".mmr", stats.getMmr());
        statsData.getConfig().set(path + ".win-streak", stats.getWinStreak());
        statsData.getConfig().set(path + ".max-win-streak", stats.getMaxWinStreak());
        statsData.save();
    }

    public int getMmr(UUID uuid) {
        return getStats(uuid).getMmr();
    }

    public void setMmr(UUID uuid, int mmr) {
        PlayerStats stats = getStats(uuid);
        stats.setMmr(mmr);
        saveStats(uuid, stats);
    }

    public Map<String, Integer> getTopPlayers(int count) {
        Map<String, Integer> result = new LinkedHashMap<>();
        ConfigurationSection section = statsData.getConfig().getConfigurationSection("players");
        if (section == null) return result;

        List<Map.Entry<String, Integer>> entries = new ArrayList<>();
        for (String uuid : section.getKeys(false)) {
            int mmr = section.getInt(uuid + ".mmr", plugin.getSettings().getBaseMmr());
            entries.add(new AbstractMap.SimpleEntry<>(uuid, mmr));
        }

        entries.sort((a, b) -> b.getValue() - a.getValue());

        int limit = Math.min(count, entries.size());
        for (int i = 0; i < limit; i++) {
            entries.get(i);
            result.put(entries.get(i).getKey(), entries.get(i).getValue());
        }

        return result;
    }

    public DataFile getDataFile() {
        return statsData;
    }
}
