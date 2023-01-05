package Command;

import Game.Joinspect;
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
    private final String PREFIX = ChatColor.GRAY + "[" + ChatColor.GOLD + "Bowshot" + ChatColor.GRAY + "]";
    private Bowshot bs = Bowshot.getPlugin(Bowshot.class);
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player pl = (Player) sender;
        if (bs.gamemanager.PlayerIsPlaying(pl)){
            return true;
        }
        if(args.length == 0) {
            sender.sendMessage(PREFIX + ChatColor.GOLD + "Bowshot by Perdume");
            return true;
        }
        if(args[0].equalsIgnoreCase("help")) {
            return true;
        }
        if(args[0].equalsIgnoreCase("join")) {
            bs.match.join((Player) sender);
            Bukkit.broadcastMessage(ChatColor.GREEN + "Bowshot: " + bs.match.getMatchingPlayers().size());
            if (bs.match.getMatchingPlayers().size() == 2){
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
        for(Player p: bs.match.getMatchingPlayers()){
            p.sendMessage("20초 뒤에 게임이 시작됩니다");
        }
        new BukkitRunnable() {
            int i = 20;
            @Override
            public void run() {
                i--;
                if (bs.match.getMatchingPlayers().size() < 2){
                    for(Player p: bs.match.getMatchingPlayers()){
                        p.sendMessage("매칭이 취소되었습니다");
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
