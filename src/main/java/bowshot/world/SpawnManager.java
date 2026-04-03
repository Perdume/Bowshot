package bowshot.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import bowshot.Bowshot;

public class SpawnManager {

    private final Bowshot plugin;

    public SpawnManager(Bowshot plugin) {
        this.plugin = plugin;
    }

    // === Spawn (per-map, in-instance world) ===

    public void setSpawnLocation(Player player) {
        Location loc = player.getLocation();
        String originalName = plugin.getInstanceWorldManager().getOriginalName(loc.getWorld().getName());
        if (originalName == null) return;

        String path = "location." + originalName + ".spawn";
        plugin.getLocationData().getConfig().set(path + ".x", loc.getX());
        plugin.getLocationData().getConfig().set(path + ".y", loc.getY());
        plugin.getLocationData().getConfig().set(path + ".z", loc.getZ());
        plugin.getLocationData().getConfig().set(path + ".yaw", loc.getYaw());
        plugin.getLocationData().getConfig().set(path + ".pitch", loc.getPitch());
        plugin.getLocationData().save();
    }

    public Location getSpawnLocation(String templateFolderName, World world) {
        String path = "location." + templateFolderName + ".spawn";
        // Fallback: try old format without .spawn suffix
        if (!plugin.getLocationData().getConfig().contains(path + ".x")) {
            path = "location." + templateFolderName;
        }
        double x = plugin.getLocationData().getConfig().getDouble(path + ".x");
        double y = plugin.getLocationData().getConfig().getDouble(path + ".y");
        double z = plugin.getLocationData().getConfig().getDouble(path + ".z");
        float yaw = (float) plugin.getLocationData().getConfig().getDouble(path + ".yaw", 0);
        float pitch = (float) plugin.getLocationData().getConfig().getDouble(path + ".pitch", 0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    // === End (per-map, in-instance world) ===

    public void setEndLocation(Player player, String templateName) {
        Location loc = player.getLocation();
        String path = "location." + templateName + ".end";
        plugin.getLocationData().getConfig().set(path + ".x", loc.getX());
        plugin.getLocationData().getConfig().set(path + ".y", loc.getY());
        plugin.getLocationData().getConfig().set(path + ".z", loc.getZ());
        plugin.getLocationData().getConfig().set(path + ".yaw", loc.getYaw());
        plugin.getLocationData().getConfig().set(path + ".pitch", loc.getPitch());
        plugin.getLocationData().save();
    }

    public Location getEndLocation(String templateFolderName, World world) {
        String path = "location." + templateFolderName + ".end";
        if (!plugin.getLocationData().getConfig().contains(path + ".x")) {
            return getSpawnLocation(templateFolderName, world);
        }
        double x = plugin.getLocationData().getConfig().getDouble(path + ".x");
        double y = plugin.getLocationData().getConfig().getDouble(path + ".y");
        double z = plugin.getLocationData().getConfig().getDouble(path + ".z");
        float yaw = (float) plugin.getLocationData().getConfig().getDouble(path + ".yaw", 0);
        float pitch = (float) plugin.getLocationData().getConfig().getDouble(path + ".pitch", 0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    // === Lobby (global, main world) ===

    public void setLobbyLocation(Player player) {
        Location loc = player.getLocation();
        String path = "lobby";
        plugin.getLocationData().getConfig().set(path + ".x", loc.getX());
        plugin.getLocationData().getConfig().set(path + ".y", loc.getY());
        plugin.getLocationData().getConfig().set(path + ".z", loc.getZ());
        plugin.getLocationData().getConfig().set(path + ".yaw", loc.getYaw());
        plugin.getLocationData().getConfig().set(path + ".pitch", loc.getPitch());
        plugin.getLocationData().getConfig().set(path + ".world", loc.getWorld().getName());
        plugin.getLocationData().save();
    }

    public Location getLobbyLocation() {
        String path = "lobby";
        if (!plugin.getLocationData().getConfig().contains(path + ".world")) {
            // Fallback: try old global endloc from world.yml
            Object endloc = plugin.getWorldData().getConfig().get("endloc");
            if (endloc instanceof Location) return (Location) endloc;
            return null;
        }
        String worldName = plugin.getLocationData().getConfig().getString(path + ".world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        double x = plugin.getLocationData().getConfig().getDouble(path + ".x");
        double y = plugin.getLocationData().getConfig().getDouble(path + ".y");
        double z = plugin.getLocationData().getConfig().getDouble(path + ".z");
        float yaw = (float) plugin.getLocationData().getConfig().getDouble(path + ".yaw", 0);
        float pitch = (float) plugin.getLocationData().getConfig().getDouble(path + ".pitch", 0);
        return new Location(world, x, y, z, yaw, pitch);
    }
}
