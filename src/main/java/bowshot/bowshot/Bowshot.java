package bowshot.bowshot;

import Game.Eh;
import Area.ScoreHelper;
import Command.*;
import Debug.getThread;
import Game.Joinspect;
import Manager.*;
import Score.hologram;
import User.spectator;
import World.VisitWorld;
import World.WorldLoader;
import World.WorldEditor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Bowshot extends JavaPlugin implements Listener {

    public static final String PREFIX = ChatColor.GRAY + "[" + ChatColor.GOLD + "PVPGame" + ChatColor.GRAY + "]";
    public ArenaManager arenaManager;
    public SubArenaManager subarenaManager;
    public WorldManager worldManager;
    public UserManager usermanager;
    public GameManager gamemanager;
    public Match match;
    public VisitWorld vw;
    public WorldLoader wl;
    public LocationManager locationmanager;
    public Joinspect js;
    public spectator spe;
    public ScoreHelper sc;
    public WorldEditor we;
    public getThread gth;
    public ScoreManager scoremanager;
    public hologram holo;

    @Override
    public void onEnable() {
        // Plugin startup logic
        locationmanager = new LocationManager(this);
        vw = new VisitWorld(this);
        spe = new spectator();
        match = new Match(this);
        wl = new WorldLoader();
        js = new Joinspect();
        sc = new ScoreHelper(this);
        we = new WorldEditor();
        gth = new getThread(this);
        holo = new hologram(this);
        subarenaManager = new SubArenaManager();
        arenaManager = new ArenaManager();
        this.worldManager = new WorldManager(this);
        arenaManager.arealoader();
        this.usermanager = new UserManager(this);
        this.gamemanager = new GameManager(this);
        this.scoremanager = new ScoreManager(this);
        Bukkit.getServer().getPluginManager().registerEvents(new Eh(), this);
        Bukkit.getServer().getPluginManager().registerEvents(wl, this);
        Bukkit.getServer().getPluginManager().registerEvents(js, this);
        Bukkit.getServer().getPluginManager().registerEvents(spe, this);
        Bukkit.getServer().getPluginManager().registerEvents(we, this);
        this.getCommand("Bowshot").setExecutor(new user());
        this.getCommand("Bowshotadmin").setExecutor(new admin());
        this.getCommand("Bowshot").setTabCompleter(new UserTab());
        this.getCommand("Bowshotadmin").setTabCompleter(new AdminTab());
        gamemanager.autodeleteManager();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


}
