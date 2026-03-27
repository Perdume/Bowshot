package Manager;

import Area.Arena;
import Area.SubArena;
import Game.Game;
import Game.GameUtil;
import World.VisitToPlay;
import World.WorldManage;
import bowshot.bowshot.Bowshot;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GameManager{
    private Bowshot bs;
    private Map<SubArena, Game> games = new HashMap<>();
    private List<Game> gamelist = new ArrayList<>();
    private Map<Arena, List<SubArena>> Arenalist = new HashMap<>();
    public GameManager(Bowshot plugin){
        bs = plugin;
    }
    public void StartGame(){

    }
    public Game getGame(SubArena area){
        return games.get(area);
    }
    public void registergame(SubArena area){
        games.put(area, new Game(area));

    }
    public List<Game> getGames(){
        return gamelist;
    }

    public void GameStart(List<Player> li){
        SubArena sb = GetRandomSubArena();
        Game gm = new Game(sb);
        games.put(sb, gm);
        gm.start(li);
    }
    public void GameEnd(SubArena area){
        games.get(area).end();
        games.remove(area);
    }
    public void removegame(SubArena area){
        games.remove(area);
    }

    private SubArena GetRandomSubArena(){
        try {
            List<String> Areas = bs.arenaManager.getArenas();
            if (Areas.isEmpty()) {
                return null;
            }
            int RandomInt = new Random().nextInt(Areas.size());
            Arena SelArena = bs.arenaManager.getArena(Areas.get(RandomInt));
            File fi = SelArena.getWorldLoader();
            String MixedName = "Bowshot--PLAY--" + GameUtil.randomName();
            Path toFolder = Paths.get(GameUtil.buildPath(Bukkit.getWorldContainer().getAbsolutePath(), MixedName));
            WorldManage wrma = new WorldManage();
            wrma.copyWorld(fi, toFolder.toFile());
            Files.move(toFolder, toFolder.resolveSibling(MixedName));
            WorldCreator wrm1 = new WorldCreator(MixedName);
            wrm1.generator("VoidGenerator");
            wrm1.createWorld();
            VisitToPlay vtp = new VisitToPlay();
            Location Loc = vtp.getLocation(fi.getName(), Bukkit.getWorld(MixedName));
            SubArena NewSubArena = bs.subarenaManager.registerArena(MixedName, Loc);
            return NewSubArena;
        }
        catch(IOException e){
            return null;
        }
    }
    public void autodeleteManager(){
        Bukkit.getScheduler().scheduleSyncRepeatingTask(bs, new Runnable() {
            @Override
            public void run() {
                for (World w: Bukkit.getWorlds()){
                    if (w.getName().contains("--PLAY--")){
                        if(w.getPlayers().isEmpty()){
                            bs.getLogger().info(bs.getMessageManager().get("admin.deleting-world", "{name}", w.getName()));
                            Bukkit.unloadWorld(w, false);
                            WorldManage.deleteFilesRecursively(w.getWorldFolder());
                        }
                    }
                    if (w.getName().contains("--VISIT--")){
                        if(w.getPlayers().isEmpty()){
                            Bukkit.unloadWorld(w, false);
                            WorldManage.deleteFilesRecursively(w.getWorldFolder());
                        }
                    }
                }
                File[] dataFiles = bs.getDataFolder().listFiles();
                if (dataFiles != null) {
                    for (File f : dataFiles) {
                        if (f.isDirectory()) {
                            if (f.getName().contains("--")) {
                                if (!ismatch(f)) {
                                    WorldManage.deleteFilesRecursively(f);
                                }
                            }
                        }
                    }
                }
                Hologram hologram;
                List<String> ranking = bs.holo.sort();
                if (DHAPI.getHologram("BOWSHOT") == null){
                    Location location = new Location(Bukkit.getWorld("world"), -22, 70, -9.5);
                    hologram = DHAPI.createHologram("BOWSHOT", location);
                }
                else{
                    hologram = DHAPI.getHologram("BOWSHOT");
                }
                DHAPI.setHologramLines(hologram, ranking);
            }
        }, 20, 1200);
    }
    public Boolean PlayerIsPlaying(Player p){
        if (gamelist == null) return false;
        for (Game g: gamelist){
            if (g.isPlayerisPlaying(p)){
                return true;
            }
        }
        return false;
    }
    private Boolean ismatch(File f){
        for (Game g: bs.gamemanager.getGames()){
            if (Objects.equals(g.getArena().getArenaName(), f.getName())){
                return true;
            }
        }
        return false;
    }
}
