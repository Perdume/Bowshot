package bowshot.world;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import bowshot.Bowshot;

public class InstanceWorldManager {

    private final Bowshot plugin;
    private final Set<World> activeWorlds = Collections.newSetFromMap(new LinkedHashMap<>());
    private final Map<String, String> worldNameMapping = new HashMap<>(); // generatedName -> originName
    private final Map<String, InstanceType> worldTypeMapping = new HashMap<>(); // generatedName -> type
    private final WorldFileManager fileManager = new WorldFileManager();

    public InstanceWorldManager(Bowshot plugin) {
        this.plugin = plugin;
    }

    /**
     * Synchronous world creation (used by GameManager for PLAY worlds).
     */
    public World createInstanceWorld(File templateFolder, InstanceType type) throws IOException {
        String randomName = type.createWorldName(generateRandomName());
        File targetFile = new File(Bukkit.getWorldContainer(), randomName);
        fileManager.copyWorld(templateFolder, targetFile);
        return loadWorldFromFolder(randomName, templateFolder.getName(), type);
    }

    /**
     * Async world creation: copies files off main thread, then loads world on main thread.
     * Calls callback with the World (or null on failure) on the main thread.
     */
    public void createInstanceWorldAsync(File templateFolder, InstanceType type, Consumer<World> callback) {
        String randomName = type.createWorldName(generateRandomName());
        File targetFile = new File(Bukkit.getWorldContainer(), randomName);
        String templateName = templateFolder.getName();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                fileManager.copyWorld(templateFolder, targetFile);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    World world = loadWorldFromFolder(randomName, templateName, type);
                    callback.accept(world);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    private World loadWorldFromFolder(String worldName, String templateName, InstanceType type) {
        WorldCreator creator = new WorldCreator(worldName);
        creator.generator("VoidGenerator");
        creator.generateStructures(false);
        World world = creator.createWorld();

        if (world != null) {
            world.setKeepSpawnInMemory(false);
            world.setAutoSave(type == InstanceType.EDIT);
            activeWorlds.add(world);
            worldNameMapping.put(worldName, templateName);
            worldTypeMapping.put(worldName, type);
        }
        return world;
    }

    public void removeWorld(World world) {
        if (world == null) return;

        Location lobbyLoc = plugin.getSpawnManager().getLobbyLocation();
        for (Player player : new ArrayList<>(world.getPlayers())) {
            if (lobbyLoc != null) {
                player.teleport(lobbyLoc);
            }
        }

        String worldName = world.getName();
        boolean isEdit = getInstanceType(worldName) == InstanceType.EDIT;
        worldNameMapping.remove(worldName);
        worldTypeMapping.remove(worldName);
        activeWorlds.remove(world);

        if (Bukkit.unloadWorld(world, isEdit)) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    WorldFileManager.deleteRecursively(world.getWorldFolder()));
        }
    }

    public Set<World> getActiveWorlds() {
        return activeWorlds;
    }

    public Map<String, String> getWorldNameMapping() {
        return worldNameMapping;
    }

    public String getOriginalName(String generatedName) {
        return worldNameMapping.get(generatedName);
    }

    public InstanceType getInstanceType(String worldName) {
        return worldTypeMapping.get(worldName);
    }

    public InstanceType getInstanceType(World world) {
        return worldTypeMapping.get(world.getName());
    }

    public boolean isInstanceWorld(World world) {
        return world != null && worldTypeMapping.containsKey(world.getName());
    }

    public void removeAllWorlds() {
        for (World w : new ArrayList<>(activeWorlds)) {
            removeWorld(w);
        }
    }

    private String generateRandomName() {
        int leftLimit = 48;
        int rightLimit = 122;
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(10)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
