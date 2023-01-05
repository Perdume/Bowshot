package World;

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
import java.util.Random;

public class WorldLoader implements Listener {
    private Bowshot bs = Bowshot.getPlugin(Bowshot.class);
    private final Inventory inv;
    public WorldLoader() {
        // Create a new inventory, with no owner (as this isn't a real inventory), a size of nine, called example
        inv = Bukkit.createInventory(null, 9, "WorldList");

        // Put the items into the inventory
        initializeItems();
    }
    public void initializeItems() {
        File dir = new File(bs.getDataFolder().getAbsolutePath() + "\\WorldList");
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

        // Set the name of the item
        meta.setDisplayName(name);

        // Set the lore of the item
        meta.setLore(Arrays.asList(lore));

        item.setItemMeta(meta);

        return item;
    }

    // You can open the inventory with this
    public void openInventory(final HumanEntity ent) {
        ent.openInventory(inv);
    }

    // Check for clicks on items
    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) throws IOException {
        if (!e.getInventory().equals(inv)) return;

        e.setCancelled(true);

        final ItemStack clickedItem = e.getCurrentItem();

        // verify current item is not null
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        final Player p = (Player) e.getWhoClicked();

        // Using slots click is a best option for your inventory click's
        p.sendMessage("Wait...");
        Path releaseFolder = Paths.get(bs.getDataFolder().getAbsolutePath() + "\\WorldList\\" + e.getCurrentItem().getItemMeta().getDisplayName());
        String MixedName = "Bowshot--VISIT--" + Randomname();
        Path toFolder = Paths.get(Bukkit.getWorldContainer().getAbsolutePath() + "\\" + MixedName);
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

    // Cancel dragging in our inventory
    @EventHandler
    public void onInventoryClick(final InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) {
            e.setCancelled(true);
        }
    }


    private String Randomname(){
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();

        String generatedString = random.ints(leftLimit,rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        return generatedString;
    }

}
