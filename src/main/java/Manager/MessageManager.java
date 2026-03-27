package Manager;

import bowshot.bowshot.Bowshot;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {
    private final Bowshot plugin;
    private FileConfiguration messages;
    private String language;

    public MessageManager(Bowshot plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.language = plugin.getConfigManager().getConfig().getString("language", "ko");
        String fileName = "messages_" + language + ".yml";
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            } else {
                plugin.saveResource("messages_ko.yml", false);
                file = new File(plugin.getDataFolder(), "messages_ko.yml");
            }
        }
        this.messages = YamlConfiguration.loadConfiguration(file);

        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            this.messages.setDefaults(defaults);
        }
    }

    public String get(String key) {
        String msg = messages.getString(key, key);
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public String get(String key, String... replacements) {
        String msg = get(key);
        if (replacements.length % 2 == 0) {
            for (int i = 0; i < replacements.length; i += 2) {
                msg = msg.replace(replacements[i], replacements[i + 1]);
            }
        }
        return msg;
    }

    public String getLanguage() {
        return language;
    }
}
