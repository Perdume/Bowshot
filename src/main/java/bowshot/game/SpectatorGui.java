package bowshot.game;

import bowshot.Bowshot;
import bowshot.lang.MessageKey;
import bowshot.player.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class SpectatorGui implements Listener {

    private final Bowshot plugin;
    private Inventory inventory;

    public SpectatorGui(Bowshot plugin) {
        this.plugin = plugin;
    }

    public void openInventory(Player spectator) {
        inventory = Bukkit.createInventory(null, 54, "Player List");
        List<Player> worldPlayers = spectator.getWorld().getPlayers();

        for (Player player : worldPlayers) {
            GamePlayer gp = plugin.getPlayerManager().getPlayer(player);
            if (!gp.isSpectator()) {
                inventory.addItem(createHead(player));
            }
        }

        spectator.openInventory(inventory);
    }

    private ItemStack createHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skull = (SkullMeta) head.getItemMeta();
        skull.setOwningPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()));
        skull.setDisplayName(player.getName());
        head.setItemMeta(skull);
        return head;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (inventory == null || !event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;

        String targetName = clicked.getItemMeta().getDisplayName();
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) return;

        Player spectator = (Player) event.getWhoClicked();
        spectator.closeInventory();
        spectator.teleport(target.getLocation());
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (inventory != null && event.getInventory().equals(inventory)) {
            event.setCancelled(true);
        }
    }
}
