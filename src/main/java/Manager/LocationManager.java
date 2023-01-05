package Manager;

import bowshot.bowshot.Bowshot;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

public class LocationManager {
    private Bowshot plugin;
    private FileConfiguration dataconfig = null;
    private File configfile = null;
    public LocationManager(Bowshot bw){
        plugin = bw;
        saveDefaultConfig();
    }
    public void reloadConfig(){
        if (this.configfile == null) {
            this.configfile = new File(this.plugin.getDataFolder(), "location.yml");
        }
        this.dataconfig = YamlConfiguration.loadConfiguration(this.configfile);

        InputStream defaultStream = this.plugin.getResource("location.yml");
        if(defaultStream != null){
            YamlConfiguration defaultconfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            this.dataconfig.setDefaults(defaultconfig);
        }
    }
    public FileConfiguration getConfig(){
        if (this.dataconfig == null){
            reloadConfig();
        }
        return this.dataconfig;
    }
    public void saveconfig(){
        if (this.dataconfig == null || this.configfile == null){
            return;

        }
        try {
            this.getConfig().save(this.configfile);
        }
        catch(IOException e){
            plugin.getLogger().log(Level.SEVERE, "Could not save config to " + this.configfile, e);
        }
    }
    public void saveDefaultConfig(){
        if(this.configfile == null){
            this.configfile = new File(this.plugin.getDataFolder(), "location.yml");
        }
        if (!this.configfile.exists()){
            this.plugin.saveResource("location.yml", false);
        }
    }
}
