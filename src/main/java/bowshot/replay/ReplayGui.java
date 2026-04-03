package bowshot.replay;

import bowshot.Bowshot;
import bowshot.lang.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.*;

public class ReplayGui implements Listener {

    private final Bowshot plugin;
    private final String guiTitle = "Replay List";

    public ReplayGui(Bowshot plugin) {
        this.plugin = plugin;
    }

    public void openInventory(Player player) {
        List<String> replays = plugin.getReplayManager().listReplays();
        int size = Math.max(9, Math.min(54, ((replays.size() / 9) + 1) * 9));
        Inventory inventory = Bukkit.createInventory(null, size, guiTitle);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (int i = 0; i < Math.min(replays.size(), 54); i++) {
            String filename = replays.get(i);
            ReplayData data = plugin.getReplayManager().loadReplay(filename);
            if (data == null) continue;

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + data.getArenaName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + sdf.format(new Date(data.getRecordedAt())));
            lore.add(ChatColor.GRAY + "Players: " + String.join(", ", data.getPlayerNames()));
            lore.add(ChatColor.GRAY + "Duration: " + (data.getDurationTicks() / 20) + "s");
            lore.add("");
            lore.add(ChatColor.YELLOW + filename);
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(i, item);
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null) return;
        if (!event.getView().getTitle().equals(guiTitle)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PAPER) return;
        if (clicked.getItemMeta() == null || clicked.getItemMeta().getLore() == null) return;

        List<String> lore = clicked.getItemMeta().getLore();
        String filename = ChatColor.stripColor(lore.get(lore.size() - 1));

        Player player = (Player) event.getWhoClicked();
        player.closeInventory();

        ReplayData data = plugin.getReplayManager().loadReplay(filename);
        if (data == null) {
            player.sendMessage(plugin.getMessageManager().get(MessageKey.REPLAY_NOT_FOUND));
            return;
        }

        plugin.getReplayManager().startViewing(player, data);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle() != null && event.getView().getTitle().equals(guiTitle)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onReplayControl(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getReplayManager().isViewing(player)) return;

        ItemStack item = event.getItem();
        if (item == null || item.getItemMeta() == null) return;

        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        ReplayViewer viewer = plugin.getReplayManager().getViewer(player);
        if (viewer == null) return;

        event.setCancelled(true);

        switch (name) {
            case "Pause/Resume":
                viewer.togglePause();
                break;
            case "Stop":
                viewer.stop();
                break;
            default:
                if (name.startsWith("Speed:")) {
                    viewer.cycleSpeed();
                }
                break;
        }
    }
}
