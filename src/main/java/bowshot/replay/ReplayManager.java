package bowshot.replay;

import bowshot.Bowshot;
import bowshot.arena.Arena;
import bowshot.world.InstanceType;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class ReplayManager {

    private final Bowshot plugin;
    private final Map<UUID, ReplayViewer> activeViewers = new HashMap<>();

    public ReplayManager(Bowshot plugin) {
        this.plugin = plugin;
    }

    public void saveReplay(ReplayData data) {
        if (!plugin.getSettings().isReplayEnabled()) return;

        File replayDir = new File(plugin.getDataFolder(), "replays");
        replayDir.mkdirs();

        cleanOldReplays(replayDir);

        String filename = data.getRecordedAt() + "_" + sanitize(data.getArenaName()) + ".replay";
        File file = new File(replayDir, filename);

        try {
            data.save(file);
            plugin.getLogger().info("Saved replay: " + filename);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save replay", e);
        }
    }

    public ReplayData loadReplay(String filename) {
        File file = new File(plugin.getDataFolder(), "replays/" + filename);
        if (!file.exists()) return null;

        try {
            return ReplayData.load(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load replay: " + filename, e);
            return null;
        }
    }

    public List<String> listReplays() {
        List<String> names = new ArrayList<>();
        File replayDir = new File(plugin.getDataFolder(), "replays");
        if (!replayDir.exists()) return names;

        File[] files = replayDir.listFiles((dir, name) -> name.endsWith(".replay"));
        if (files == null) return names;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

        for (File f : files) {
            names.add(f.getName());
        }
        return names;
    }

    public void startViewing(Player viewer, ReplayData data) {
        if (activeViewers.containsKey(viewer.getUniqueId())) {
            stopViewing(viewer);
        }

        // Find the arena template to create world instance
        Arena arena = plugin.getArenaManager().getArenaByTemplate(data.getArenaName());
        if (arena == null) {
            // Try using arenaName directly as template folder
            arena = plugin.getArenaManager().getRandomArena();
        }

        if (arena == null) {
            viewer.sendMessage("No arena available for replay.");
            return;
        }

        try {
            File templateFile = arena.getTemplateFile();
            World world = plugin.getInstanceWorldManager().createInstanceWorld(templateFile, InstanceType.VISIT);
            if (world == null) {
                viewer.sendMessage("Failed to create replay world.");
                return;
            }

            ReplayViewer rv = new ReplayViewer(plugin, viewer, data, world);
            activeViewers.put(viewer.getUniqueId(), rv);
            rv.start();
        } catch (IOException e) {
            viewer.sendMessage("Failed to start replay.");
            plugin.getLogger().log(Level.WARNING, "Failed to start replay", e);
        }
    }

    public void stopViewing(Player viewer) {
        ReplayViewer rv = activeViewers.remove(viewer.getUniqueId());
        if (rv != null) {
            rv.stop();
        }
    }

    public void removeViewer(Player viewer) {
        activeViewers.remove(viewer.getUniqueId());
    }

    public boolean isViewing(Player player) {
        return activeViewers.containsKey(player.getUniqueId());
    }

    public ReplayViewer getViewer(Player player) {
        return activeViewers.get(player.getUniqueId());
    }

    public void stopAll() {
        for (ReplayViewer rv : new ArrayList<>(activeViewers.values())) {
            rv.stop();
        }
        activeViewers.clear();
    }

    private void cleanOldReplays(File replayDir) {
        int max = plugin.getSettings().getReplayMaxReplays();
        File[] files = replayDir.listFiles((dir, name) -> name.endsWith(".replay"));
        if (files == null || files.length < max) return;

        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        int toDelete = files.length - max + 1;
        for (int i = 0; i < toDelete; i++) {
            files[i].delete();
        }
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
