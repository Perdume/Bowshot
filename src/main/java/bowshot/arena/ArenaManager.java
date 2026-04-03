package bowshot.arena;

import bowshot.Bowshot;
import bowshot.data.DataFile;
import bowshot.debug.DebugLogger;
import bowshot.debug.ErrorCode;
import org.bukkit.Location;

import java.util.*;

public class ArenaManager {

    private final Bowshot plugin;
    private final Map<String, Arena> arenaTemplates = new HashMap<>();
    private final List<String> arenaNames = new ArrayList<>();
    private final Map<String, ArenaInstance> instances = new HashMap<>();

    public ArenaManager(Bowshot plugin) {
        this.plugin = plugin;
    }

    public void loadArenas() {
        DataFile worldData = plugin.getWorldData();
        try {
            List<String> names = (List<String>) worldData.getConfig().get("Worldlist");
            if (names == null) return;
            arenaNames.addAll(names);
            for (String name : arenaNames) {
                String folder = worldData.getConfig().getString("World." + name + ".filename");
                arenaTemplates.put(name, new Arena(name, folder));
            }
        } catch (Exception e) {
            DebugLogger.report(ErrorCode.INIT_ARENA_LOAD_FAIL, e.getMessage());
        }
    }

    public void registerArena(String name, String templateFolder) {
        DataFile worldData = plugin.getWorldData();
        arenaTemplates.put(name, new Arena(name, templateFolder));
        arenaNames.add(name);
        worldData.getConfig().set("World." + name + ".filename", templateFolder);
        worldData.getConfig().set("Worldlist", arenaNames);
        worldData.save();
    }

    public void removeArena(String name) {
        arenaTemplates.remove(name);
        arenaNames.remove(name);
        DataFile worldData = plugin.getWorldData();
        worldData.getConfig().set("World." + name + ".filename", null);
        worldData.getConfig().set("Worldlist", arenaNames);
        worldData.save();
    }

    public Arena getArena(String name) {
        return arenaTemplates.get(name);
    }

    public List<String> getArenaNames() {
        return arenaNames;
    }

    public boolean exists(String name) {
        return arenaTemplates.containsKey(name);
    }

    public Arena getRandomArena() {
        if (arenaNames.isEmpty()) return null;
        int index = new Random().nextInt(arenaNames.size());
        return arenaTemplates.get(arenaNames.get(index));
    }

    // Instance management
    public ArenaInstance createInstance(String worldName, Location spawnLocation) {
        ArenaInstance instance = new ArenaInstance(worldName, spawnLocation);
        instances.put(worldName, instance);
        return instance;
    }

    public ArenaInstance getInstance(String worldName) {
        return instances.get(worldName);
    }

    public void removeInstance(String worldName) {
        instances.remove(worldName);
    }

    public List<String> getInstanceNames() {
        return new ArrayList<>(instances.keySet());
    }

    public Arena getArenaByTemplate(String templateFolder) {
        for (Arena arena : arenaTemplates.values()) {
            if (arena.getTemplateFolder().equals(templateFolder)) {
                return arena;
            }
        }
        return null;
    }
}
