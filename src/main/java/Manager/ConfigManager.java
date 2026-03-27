package Manager;

import bowshot.bowshot.Bowshot;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class ConfigManager {
    private final Bowshot plugin;
    private FileConfiguration config;
    private File configFile;

    public ConfigManager(Bowshot plugin) {
        this.plugin = plugin;
        saveDefaultConfig();
    }

    public void reloadConfig() {
        if (this.configFile == null) {
            this.configFile = new File(this.plugin.getDataFolder(), "config.yml");
        }
        this.config = YamlConfiguration.loadConfiguration(this.configFile);

        InputStream defaultStream = this.plugin.getResource("config.yml");
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            this.config.setDefaults(defaults);
        }
    }

    public FileConfiguration getConfig() {
        if (this.config == null) {
            reloadConfig();
        }
        return this.config;
    }

    public void saveConfig() {
        if (this.config == null || this.configFile == null) {
            return;
        }
        try {
            this.getConfig().save(this.configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + this.configFile, e);
        }
    }

    public void saveDefaultConfig() {
        if (this.configFile == null) {
            this.configFile = new File(this.plugin.getDataFolder(), "config.yml");
        }
        if (!this.configFile.exists()) {
            this.plugin.saveResource("config.yml", false);
        }
    }

    public int getMinPlayers() {
        return getConfig().getInt("game.min-players", 2);
    }

    public int getCountdown() {
        return getConfig().getInt("game.countdown", 20);
    }

    public int getPrepTime() {
        return getConfig().getInt("game.prep-time", 15);
    }

    public int getPlacementMatches() {
        return getConfig().getInt("ranked.placement-matches", 10);
    }

    public int getBaseElo() {
        return getConfig().getInt("ranked.base-elo", 1500);
    }
}
