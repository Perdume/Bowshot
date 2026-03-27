package Command;

import Game.Joinspect;
import Manager.MessageManager;
import bowshot.bowshot.Bowshot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public class user implements CommandExecutor {
    private Bowshot bs = Bowshot.getPlugin(Bowshot.class);
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player pl = (Player) sender;
        MessageManager msg = bs.getMessageManager();
        if (bs.gamemanager.PlayerIsPlaying(pl)){
            return true;
        }
        if(args.length == 0) {
            sender.sendMessage(msg.get("prefix") + ChatColor.GOLD + " Bowshot by Perdume");
            return true;
        }
        if(args[0].equalsIgnoreCase("help")) {
            return true;
        }
        if(args[0].equalsIgnoreCase("join")) {
            bs.match.join((Player) sender);
            int count = bs.match.getMatchingPlayers().size();
            Bukkit.broadcastMessage(msg.get("match.queue-count", "{count}", String.valueOf(count)));
            int minPlayers = bs.getConfigManager().getMinPlayers();
            if (count >= minPlayers){
                AutoStart();
            }
            return true;
        }
        if(args[0].equalsIgnoreCase("leave")) {
            bs.match.leave((Player) sender);
            return true;
        }
        if(args[0].equalsIgnoreCase("spect")) {
            bs.js.openInventory((HumanEntity) sender);
            return true;
        }
        return true;
    }
    private void AutoStart(){
        MessageManager msg = bs.getMessageManager();
        int countdown = bs.getConfigManager().getCountdown();
        for(Player p: bs.match.getMatchingPlayers()){
            p.sendMessage(msg.get("match.start-countdown", "{seconds}", String.valueOf(countdown)));
        }
        new BukkitRunnable() {
            int i = countdown;
            @Override
            public void run() {
                i--;
                int minPlayers = bs.getConfigManager().getMinPlayers();
                if (bs.match.getMatchingPlayers().size() < minPlayers){
                    for(Player p: bs.match.getMatchingPlayers()){
                        p.sendMessage(msg.get("match.cancelled"));
                        cancel();
                    }
                }
                if (i == 0){
                    bs.gamemanager.GameStart(bs.match.getMatchingPlayers());
                    bs.match.getMatchingPlayers().clear();
                    cancel();
                }
            }
        }.runTaskTimer(bs, 0, 20);
    }
}
