package User;

import World.WorldManage;
import bowshot.bowshot.Bowshot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldCreator;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class spectator implements Listener {
    private Bowshot bs = Bowshot.getPlugin(Bowshot.class);
    private final Inventory inv;
    public spectator() {
        // Create a new inventory, with no owner (as this isn't a real inventory), a size of nine, called example
        inv = Bukkit.createInventory(null, 54, "PlayerList");
    }
    public void initializeItems(Player p) {
        inv.clear();
        List<Player> Pll = p.getWorld().getPlayers();
        for(Player f: Pll){
            if (!bs.usermanager.getUser(f).isSpectator()) {
                inv.addItem(PLAYERHEAD(f));
            }
        }
    }
    protected ItemStack PLAYERHEAD(Player p) {
        final ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta skull = (SkullMeta) item.getItemMeta();
        skull.setOwner(p.getName());
        skull.setDisplayName(p.getName());
        item.setItemMeta(skull);
        return item;
    }

    // You can open the inventory with this
    public void openInventory(final HumanEntity ent) {
        initializeItems((Player) ent);
        ent.openInventory(inv);
    }

    // Check for clicks on items
    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e){
        if (!e.getInventory().equals(inv)) return;

        e.setCancelled(true);

        final ItemStack clickedItem = e.getCurrentItem();

        // verify current item is not null
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        final Player p = (Player) e.getWhoClicked();

        // Using slots click is a best option for your inventory click's
        Player tp = Bukkit.getPlayer(e.getCurrentItem().getItemMeta().getDisplayName());
        p.teleport(tp.getLocation());
    }

    // Cancel dragging in our inventory
    @EventHandler
    public void onInventoryClick(final InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) {
            e.setCancelled(true);
        }
    }
}
