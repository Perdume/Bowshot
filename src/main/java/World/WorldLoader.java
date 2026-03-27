package World;

import Game.GameUtil;
import bowshot.bowshot.Bowshot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldCreator;
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class WorldLoader implements Listener {
    private Bowshot bs = Bowshot.getPlugin(Bowshot.class);
    private final Inventory inv;
    public WorldLoader() {
        inv = Bukkit.createInventory(null, 9, "WorldList");
        initializeItems();
    }
    public void initializeItems() {
        File dir = new File(GameUtil.buildPath(bs.getDataFolder().getAbsolutePath(), "WorldList"));
        File[] dirList = dir.listFiles();
        if (dirList != null) {
            for(File f: dirList){
                inv.addItem(createGuiItem(Material.GRASS_BLOCK, f.getName()));
            }
        }
    }
    protected ItemStack createGuiItem(final Material material, final String name, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    public void openInventory(final HumanEntity ent) {
        ent.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) throws IOException {
        if (!e.getInventory().equals(inv)) return;

        e.setCancelled(true);

        final ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        final Player p = (Player) e.getWhoClicked();

        p.sendMessage(bs.getMessageManager().get("admin.wait"));
        Path releaseFolder = Paths.get(GameUtil.buildPath(bs.getDataFolder().getAbsolutePath(), "WorldList", e.getCurrentItem().getItemMeta().getDisplayName()));
        String MixedName = "Bowshot--VISIT--" + GameUtil.randomName();
        Path toFolder = Paths.get(GameUtil.buildPath(Bukkit.getWorldContainer().getAbsolutePath(), MixedName));
        p.sendMessage(toFolder.toFile().getAbsolutePath());
        WorldManage wrma = new WorldManage();
        wrma.copyWorld(releaseFolder.toFile(), toFolder.toFile());
        Files.move(toFolder, toFolder.resolveSibling(MixedName));
        WorldCreator wrm1 = new WorldCreator(MixedName);
        wrm1.generator("VoidGen");
        wrm1.createWorld();
        bs.vw.addVisitedWorldList(e.getCurrentItem().getItemMeta().getDisplayName(), Bukkit.getWorld(MixedName));
        p.teleport(new Location(Bukkit.getWorld(MixedName), 0, 64, 0));
    }

    @EventHandler
    public void onInventoryClick(final InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) {
            e.setCancelled(true);
        }
    }
}
