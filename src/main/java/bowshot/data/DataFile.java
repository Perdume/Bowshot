package bowshot.data;

import bowshot.Bowshot;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class DataFile {

    private final Bowshot plugin;
    private final String fileName;
    private FileConfiguration config;
    private File file;

    public DataFile(Bowshot plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        saveDefault();
    }

    public void reload() {
        if (this.file == null) {
            this.file = new File(plugin.getDataFolder(), fileName);
        }
        this.config = YamlConfiguration.loadConfiguration(this.file);

        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            this.config.setDefaults(defaults);
        }
    }

    public FileConfiguration getConfig() {
        if (this.config == null) {
            reload();
        }
        return this.config;
    }

    public void save() {
        if (this.config == null || this.file == null) {
            return;
        }
        try {
            this.config.save(this.file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + fileName, e);
        }
    }

    private void saveDefault() {
        if (this.file == null) {
            this.file = new File(plugin.getDataFolder(), fileName);
        }
        if (!this.file.exists()) {
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            } else {
                try {
                    this.file.getParentFile().mkdirs();
                    this.file.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not create " + fileName, e);
                }
            }
        }
    }
}
