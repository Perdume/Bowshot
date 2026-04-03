package bowshot.world;

import bowshot.Bowshot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.Arrays;

public class WorldLoaderGui implements Listener {

    private final Bowshot plugin;
    private Inventory inventory;

    public WorldLoaderGui(Bowshot plugin) {
        this.plugin = plugin;
        refreshInventory();
    }

    public void refreshInventory() {
        this.inventory = Bukkit.createInventory(null, 9, "World List");
        File dir = new File(plugin.getDataFolder(), "WorldList");
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    inventory.addItem(createItem(Material.GRASS_BLOCK, f.getName()));
                }
            }
        }
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    public void openInventory(HumanEntity player) {
        refreshInventory();
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        Player player = (Player) event.getWhoClicked();
        player.closeInventory();
        player.sendMessage("Loading world...");

        String templateName = clicked.getItemMeta().getDisplayName();
        File templateFolder = new File(plugin.getDataFolder(), "WorldList" + File.separator + templateName);

        if (!templateFolder.exists() || !templateFolder.isDirectory()) {
            player.sendMessage("World folder not found.");
            return;
        }

        plugin.getInstanceWorldManager().createInstanceWorldAsync(templateFolder, InstanceType.VISIT, world -> {
            if (world != null) {
                player.teleport(new Location(world, 0, 64, 0));
            } else {
                player.sendMessage("Failed to load world.");
            }
        });
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().equals(inventory)) {
            event.setCancelled(true);
        }
    }
}
