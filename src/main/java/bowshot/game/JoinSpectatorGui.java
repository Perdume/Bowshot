package bowshot.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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

import bowshot.Bowshot;
import bowshot.lang.MessageKey;
import bowshot.player.GamePlayer;

public class JoinSpectatorGui implements Listener {

    private final Bowshot plugin;
    private Inventory inventory;

    public JoinSpectatorGui(Bowshot plugin) {
        this.plugin = plugin;
    }

    public void openInventory(HumanEntity player) {
        if (!plugin.getSettings().isSpectatorAllowJoin()) {
            player.sendMessage(plugin.getMessageManager().get(MessageKey.SPECTATOR_JOIN_DISABLED));
            return;
        }
        inventory = Bukkit.createInventory(null, 9, "Game List");
        refreshItems();
        player.openInventory(inventory);
    }

    private void refreshItems() {
        inventory.clear();
        for (Game game : plugin.getGameManager().getActiveGames()) {
            List<String> playerNames = new ArrayList<>();
            for (GamePlayer gp : game.getPlayers()) {
                playerNames.add(gp.getPlayer().getDisplayName());
            }
            inventory.addItem(createItem(Material.NAME_TAG,
                    game.getArenaInstance().getWorldName(),
                    ChatColor.BLUE + "Players: " + playerNames));
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (inventory == null || !event.getInventory().equals(inventory)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        Player player = (Player) event.getWhoClicked();
        player.closeInventory();

        try {
            String arenaName = clicked.getItemMeta().getDisplayName();
            for (Game game : plugin.getGameManager().getActiveGames()) {
                if (game.getArenaInstance().getWorldName().equals(arenaName)) {
                    GamePlayer gp = plugin.getPlayerManager().getPlayer(player);
                    player.teleport(game.getArenaInstance().getSpawnLocation());

                    ItemStack tpItem = new ItemStack(Material.STICK);
                    ItemMeta meta = tpItem.getItemMeta();
                    meta.setDisplayName(plugin.getMessageManager().get(MessageKey.SPECTATOR_PLAYER_TP));
                    tpItem.setItemMeta(meta);
                    player.getInventory().addItem(tpItem);

                    player.setGameMode(GameMode.ADVENTURE);
                    gp.setSpectator(true);
                    player.setCollidable(false);
                    player.setAllowFlight(true);
                    player.setFlying(true);

                    float flySpeed = Math.max(0.0f, Math.min(1.0f, plugin.getSettings().getSpectatorFlySpeed() * 0.1f));
                    player.setFlySpeed(flySpeed);

                    if (plugin.getSettings().isSpectatorNightVision()) {
                        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
                    }

                    if (plugin.getSettings().isSpectatorInvisible()) {
                        // 투명 포션 적용 (hidePlayer 백업)
                        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
                        for (GamePlayer other : game.getPlayers()) {
                            other.getPlayer().hidePlayer(plugin, player);
                        }
                    }

                    // 관전자가 살아있는 플레이어를 볼 수 있도록 보장
                    for (GamePlayer other : game.getPlayers()) {
                        if (!other.isSpectator()) {
                            player.showPlayer(plugin, other.getPlayer());
                        }
                    }
                    game.getPlayers().add(gp);
                    return;
                }
            }
        } catch (Exception e) {
            player.sendMessage(plugin.getMessageManager().get(MessageKey.SPECTATOR_GAME_NOT_FOUND));
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (inventory != null && event.getInventory().equals(inventory)) {
            event.setCancelled(true);
        }
    }
}
