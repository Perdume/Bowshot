package bowshot.world;

import bowshot.Bowshot;
import bowshot.arena.Arena;
import bowshot.lang.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
import java.util.List;

public class VisitArenaGui implements Listener {

    private static final String GUI_TITLE = "\u00a76\u00a7l\ub9f5 \uad6c\uacbd";
    private final Bowshot plugin;

    public VisitArenaGui(Bowshot plugin) {
        this.plugin = plugin;
    }

    public void openInventory(HumanEntity player) {
        List<String> arenaNames = plugin.getArenaManager().getArenaNames();
        int size = Math.max(9, ((arenaNames.size() - 1) / 9 + 1) * 9);
        Inventory inventory = Bukkit.createInventory(null, size, GUI_TITLE);

        for (String name : arenaNames) {
            Arena arena = plugin.getArenaManager().getArena(name);
            if (arena != null) {
                inventory.addItem(createItem(Material.GRASS_BLOCK,
                        "\u00a7a" + name,
                        "\u00a77" + arena.getTemplateFolder()));
            }
        }
        player.openInventory(inventory);
    }

    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null) return;
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasLore()) return;

        Player player = (Player) event.getWhoClicked();
        player.closeInventory();

        String templateFolder = clicked.getItemMeta().getLore().get(0).replaceAll("\u00a7.", "");
        File templateFile = new File(plugin.getDataFolder(), "WorldList" + File.separator + templateFolder);

        if (!templateFile.exists() || !templateFile.isDirectory()) {
            player.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_NO_FILE));
            return;
        }

        player.sendMessage(plugin.getMessageManager().get(MessageKey.VISIT_LOADING));

        plugin.getInstanceWorldManager().createInstanceWorldAsync(templateFile, InstanceType.VISIT, world -> {
            if (world != null) {
                Location spawn = plugin.getSpawnManager().getSpawnLocation(templateFolder, world);
                player.teleport(spawn);
                player.sendMessage(plugin.getMessageManager().get(MessageKey.VISIT_TELEPORTED));
            } else {
                player.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_NO_FILE));
            }
        });
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle() != null && event.getView().getTitle().equals(GUI_TITLE)) {
            event.setCancelled(true);
        }
    }
}
