package bowshot.score;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;

/**
 * Isolated FancyHolograms usage so the class is only loaded when the plugin is present.
 */
public class HologramHelper {

    public static void updateRanking(List<String> ranking) {
        if (!FancyHologramsPlugin.isEnabled()) return;
        try {
            HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
            java.util.Optional<Hologram> existing = manager.getHologram("BOWSHOT");

            if (existing.isPresent()) {
                Hologram hologram = existing.get();
                if (hologram.getData() instanceof TextHologramData textData) {
                    textData.setText(ranking);
                    hologram.queueUpdate();
                }
            } else {
                Location location = new Location(Bukkit.getWorld("world"), -22, 70, -9.5);
                TextHologramData data = new TextHologramData("BOWSHOT", location);
                data.setText(ranking);
                data.setPersistent(true);
                Hologram hologram = manager.create(data);
                manager.addHologram(hologram);
            }
        } catch (Exception ignored) {
        }
    }
}
