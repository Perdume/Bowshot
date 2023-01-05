package Game;

import Area.ScoreHelper;
import User.User;
import bowshot.bowshot.Bowshot;
import org.bukkit.*;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class Joinspect implements Listener {
    private Bowshot bs = Bowshot.getPlugin(Bowshot.class);
    private final Inventory inv;
    public Joinspect() {
        // Create a new inventory, with no owner (as this isn't a real inventory), a size of nine, called example
        inv = Bukkit.createInventory(null, 9, "GameList");

        // Put the items into the inventory
    }
    public void initializeItems() {
        inv.clear();
        if (bs.gamemanager.getGames() != null) {
            for(Game g: bs.gamemanager.getGames()){
                List<String> pl = new ArrayList<>();
                for (User u: g.GetPlayers()){
                    pl.add(u.getPlayer().getDisplayName());
                }
                inv.addItem(createGuiItem(Material.NAME_TAG, g.getArena().getArenaName(), ChatColor.BLUE + "Players: " + pl));
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
        initializeItems();
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
        try{
            for (Game g: bs.gamemanager.getGames()){
                if (Objects.equals(g.getArena().getArenaName(), e.getCurrentItem().getItemMeta().getDisplayName())){
                    User u = bs.usermanager.getUser(p);
                    p.teleport(g.getArena().getSpawnLocation());
                    p.getInventory().addItem(getspectitem());
                    p.setGameMode(GameMode.ADVENTURE);
                    u.setSpectator(true);
                    u.setSpectator(true);
                    p.setCollidable(false);
                    p.setAllowFlight(true);
                    for (User ur : g.getArena().getPlayers()) {
                        ur.getPlayer().hidePlayer(u.getPlayer());
                    }
                    g.GetPlayers().add(u);
                }
            }
        }
        catch(Exception e2) {
            p.sendMessage(ChatColor.RED + "게임을 찾을 수 없습니다");
            e2.printStackTrace();
        }
    }
    private ItemStack getspectitem(){
        ItemStack is = new ItemStack(Material.STICK);
        ItemMeta im = is.getItemMeta();
        im.setDisplayName("PLAYER TP");
        is.setItemMeta(im);
        return is;
    }

    // Cancel dragging in our inventory
    @EventHandler
    public void onInventoryClick(final InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) {
            e.setCancelled(true);
        }
    }
}
