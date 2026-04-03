package bowshot.lang;

import bowshot.Bowshot;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.EnumMap;
import java.util.Map;

public class MessageManager {

    private final Bowshot plugin;
    private final Map<MessageKey, String> messages = new EnumMap<>(MessageKey.class);

    public MessageManager(Bowshot plugin) {
        this.plugin = plugin;
    }

    public void load(String language) {
        String fileName = "lang/" + language + ".yml";

        File langFile = new File(plugin.getDataFolder(), fileName);
        if (!langFile.exists()) {
            plugin.saveResource(fileName, false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);

        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            config.setDefaults(defaults);
        }

        for (MessageKey key : MessageKey.values()) {
            String path = key.name().toLowerCase().replace('_', '.');
            String value = config.getString(path, "&cMissing: " + key.name());
            messages.put(key, ChatColor.translateAlternateColorCodes('&', value));
        }
    }

    public String get(MessageKey key) {
        return messages.getOrDefault(key, "&cMissing: " + key.name());
    }

    public String get(MessageKey key, Object... args) {
        String msg = get(key);
        for (int i = 0; i < args.length; i++) {
            msg = msg.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return msg;
    }
}
