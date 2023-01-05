package Game;

import Area.SubArena;
import Game.Game;
import User.User;
import bowshot.bowshot.Bowshot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Objects;
import java.util.UUID;

public class Eh implements Listener {
    private Bowshot bs = Bowshot.getPlugin(Bowshot.class);

    @EventHandler
    public void onshot(EntityShootBowEvent e){
        if (e.getEntity() instanceof Player){
            Player pl = (Player) e.getEntity();
            if (bs.gamemanager.PlayerIsPlaying(pl)){
                e.getProjectile().setGravity(false);
            }
        }
    }
    @EventHandler
    public void onPlayerHurtPlayer(EntityDamageByEntityEvent event) {
        Entity entityDamaged = event.getEntity();
        if(entityDamaged instanceof Player) {
            if (bs.usermanager.getUser(entityDamaged.getUniqueId()).getArena() != null) {
                SubArena en = bs.usermanager.getUser(entityDamaged.getUniqueId()).getArena();
                UUID uid = entityDamaged.getUniqueId();
                if (en.getPlayers().contains(bs.usermanager.getUser(uid))) {
                    Player dng = (Player) entityDamaged;
                    User usr = bs.usermanager.getUser(dng);
                    if (usr.isSpectator()) {
                        dng.setCollidable(false);
                    } else {
                        if (event.getDamage() >= usr.getPlayer().getHealth()) {
                            event.setDamage(0);
                            usr.setSpectator(true);
                            dng.setCollidable(false);
                            dng.setAllowFlight(true);
                            dng.getInventory().clear();
                            dng.getInventory().addItem(getspectitem());
                            dng.setNoDamageTicks(0);
                            dng.setMaximumNoDamageTicks(0);
                            for (User u : en.getPlayers()) {
                                u.getPlayer().hidePlayer(dng);
                            }
                            if (event.getDamager() instanceof Arrow) {
                                Arrow a = (Arrow) event.getDamager();
                                Player player = (Player) a.getShooter();
                                bs.gamemanager.getGame(usr.getArena()).addkilllist(player);
                                bs.gamemanager.getGame(usr.getArena()).killscore().put(player, 1 + bs.gamemanager.getGame(usr.getArena()).getkillscore((Player) event.getEntity())*0.5);
                                bs.gamemanager.getGame(usr.getArena()).broadcast(ChatColor.RED.toString() + player.getName() + "->" + event.getEntity().getName());
                            }
                            else if (event.getDamager() instanceof Player){
                                Player player = (Player) event.getDamager();
                                bs.gamemanager.getGame(usr.getArena()).addkilllist(player);
                                bs.gamemanager.getGame(usr.getArena()).killscore().put(player, 1 + bs.gamemanager.getGame(usr.getArena()).getkillscore((Player) event.getEntity())*0.5);
                                bs.gamemanager.getGame(usr.getArena()).broadcast(ChatColor.RED.toString() + player.getName() + "->" + event.getEntity().getName());
                            }
                            int remainplayer = usr.getArena().getPlayers().size() - usr.getArena().getSpectators().size();
                            if (remainplayer == 1) {
                                bs.gamemanager.GameEnd(en);
                            }
                        }
                    }
                }
                if (event.getDamager() instanceof Player){
                    Player player = (Player) event.getDamager();
                    assert bs.usermanager.getUser(entityDamaged.getUniqueId()).getArena() != null;
                    SubArena ent = bs.usermanager.getUser(entityDamaged.getUniqueId()).getArena();
                    if(ent.getPlayers().contains(bs.usermanager.getUser(player))) {
                        if (bs.usermanager.getUser(player).isSpectator()){
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerHurt(EntityDamageEvent e){
        if (e.getEntity() instanceof Player) {
            Player dng = (Player) e.getEntity();
            if (bs.usermanager.getUser(dng.getUniqueId()).getArena() != null) {
                if (e.getCause() != EntityDamageEvent.DamageCause.PROJECTILE&&e.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                    User usr = bs.usermanager.getUser(dng);
                    if (e.getDamage() >= usr.getPlayer().getHealth()) {
                        SubArena en = bs.usermanager.getUser(dng.getUniqueId()).getArena();
                        UUID uid = dng.getUniqueId();
                        if (en.getPlayers().contains(bs.usermanager.getUser(uid))) {
                            if (!usr.isSpectator()) {
                                dng.getInventory().clear();
                                dng.getInventory().addItem(getspectitem());
                                dng.teleport(en.getSpawnLocation());
                                usr.setSpectator(true);
                                dng.setCollidable(false);
                                dng.setAllowFlight(true);
                                e.setDamage(0);
                                for (User u : en.getPlayers()) {
                                    u.getPlayer().hidePlayer(dng);
                                }
                                bs.gamemanager.getGame(usr.getArena()).broadcast(ChatColor.RED.toString() + dng.getName() + "-> ?");
                                int remainplayer = usr.getArena().getPlayers().size() - usr.getArena().getSpectators().size();

                                if (remainplayer == 1) {
                                    bs.gamemanager.GameEnd(en);
                                }
                            }
                        }
                    }
                    if (usr.isSpectator()) {
                        e.setCancelled(true);
                    }
                }
            }
        }
    }
    private ItemStack getspectitem(){
        ItemStack is = new ItemStack(Material.STICK);
        ItemMeta im = is.getItemMeta();
        im.setDisplayName("PLAYER TP");
        is.setItemMeta(im);
        return is;
    }
    @EventHandler
    public void shoot(EntityShootBowEvent e){
        if (e.getEntity() instanceof Player){
            Player dng = (Player) e.getEntity();
            User usr = bs.usermanager.getUser(dng);
            if (usr.isSpectator()){
                e.setCancelled(true);
            }
        }

    }
    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        for (Game g: bs.gamemanager.getGames()){
           if (g.isPlayerisPlaying(e.getPlayer())){
               bs.usermanager.getUser(e.getPlayer()).setSpectator(true);
               Scoreboard score = Bukkit.getScoreboardManager().getMainScoreboard();
               Team l = score.getTeam("nhide");
               l.removeEntry(e.getPlayer().getName());
               if (g.getArena().getPlayers().size() - g.getArena().getSpectators().size() == 1){
                   g.end();
               }
           }
        }
        if (bs.match.IsPlayerIn(e.getPlayer())){
            bs.match.getMatchingPlayers().remove(e.getPlayer());
        }
    }
    @EventHandler
    public void getOut(PlayerQuitEvent e){
        Bukkit.getScheduler().scheduleSyncDelayedTask(bs, new Runnable() {
            @Override
            public void run() {
                if (ETW(e.getPlayer().getWorld())){
                    Bukkit.broadcastMessage(ChatColor.GREEN + "TRUE");
                    bs.vw.remove(e.getPlayer().getWorld());
                }
            }
        }, 20);
    }
    private Boolean ETW(World w){
        if (bs.vw.getVisitedWorldList() != null){
            for (World wo: bs.vw.getVisitedWorldList()){
                if (wo == w){
                    return true;
                }
            }
        }
        return false;
    }
    @EventHandler
    public void ballFiring(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Action a = e.getAction();
        if (e.getItem() == null) return;
        if (e.getItem().getType() != Material.STICK) return;
        if (!Objects.requireNonNull(e.getItem().getItemMeta()).getDisplayName().equals("PLAYER TP")) return;
        bs.spe.openInventory(p);
    }
    @EventHandler
    public void asd(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (bs.gamemanager.PlayerIsPlaying(p)) {
            if (e.getClickedBlock().getType() != null) {
                if (e.getClickedBlock().getType() == Material.CHEST || e.getClickedBlock().getType() == Material.BARREL) {
                    e.setCancelled(true);
                }
            }
        }
    }
    @EventHandler
    public void drop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (bs.gamemanager.PlayerIsPlaying(p)) {
            e.setCancelled(true);
        }
    }

}
