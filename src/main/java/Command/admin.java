package Command;

import Debug.ThreadState;
import Game.Game;
import World.VisitToPlay;
import bowshot.bowshot.Bowshot;
import World.WorldManage;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class admin implements CommandExecutor {
    private final String PREFIX = ChatColor.GRAY + "[" + ChatColor.GOLD + "Bowshot" + ChatColor.GRAY + "]";
    private Bowshot bs = Bowshot.getPlugin(Bowshot.class);
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(args.length == 0) {
            sender.sendMessage(PREFIX + ChatColor.GOLD + "PVPGame by MisterMel");
            sender.sendMessage(PREFIX + ChatColor.GRAY + "Use /pvpgame help for a list of commands.");
            return true;
        }
        if(args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(PREFIX + ChatColor.GOLD + "PVPGame Command Usage");
            sender.sendMessage(PREFIX + ChatColor.GRAY + "/pvpgame - The main command");
            sender.sendMessage(PREFIX + ChatColor.GRAY + "/pvpgame help - Shows this message.");
            sender.sendMessage(PREFIX + ChatColor.GRAY + "/pvpgame add <Name> - Create an arena");
            sender.sendMessage(PREFIX + ChatColor.GRAY + "/pvpgame remove <Name> - Remove an arena");
            sender.sendMessage(PREFIX + ChatColor.GRAY + "/pvpgame setlobby <Name> - Set the lobby for an arena");
            sender.sendMessage(PREFIX + ChatColor.GRAY + "/pvpgame setspawn <Name> - Set the spawn for an arena");
            sender.sendMessage(PREFIX + ChatColor.GRAY + "/pvpgame setmainlobby - Set the main lobby");
            return true;
        }
        if(args[0].equalsIgnoreCase("add")) {
            if(args.length <= 2) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Use /pvpgame add <Name> <filename>");
                return true;
            }
            String name = args[1];
            if(bs.arenaManager.exists(name)) {
                sender.sendMessage(PREFIX + ChatColor.RED + "That arena already exists!");
                return true;
            }
            for (String wl: Wl()){
                if (Objects.equals(wl, args[2])){
                    bs.arenaManager.registerArena(name, wl);
                    sender.sendMessage(ChatColor.GOLD + "Arena created.");
                    return true;
                }
            }
            sender.sendMessage(PREFIX + ChatColor.RED + "No File!");
            return true;
        }
        if(args[0].equalsIgnoreCase("remove")) {
            if(args.length == 1) {
                sender.sendMessage(PREFIX + ChatColor.RED + "Use /pvpgame remove <Name>");
                return true;
            }
            String name = args[1];
            if(!bs.arenaManager.exists(name)) {
                sender.sendMessage(PREFIX + ChatColor.RED + "That arena doesn't exist!");
                return true;
            }
            bs.arenaManager.remove(name);
            sender.sendMessage(ChatColor.GOLD + "Arena removed.");
            return true;
        }
        if(args[0].equalsIgnoreCase("setspawn")) {
            if(!(sender instanceof Player)) {
                sender.sendMessage(PREFIX + ChatColor.RED + "This command can only be used by players.");
                return true;
            }
            VisitToPlay vtp = new VisitToPlay();
            Player p = (Player) sender;
            p.sendMessage(bs.vw.getVisitedWorldGeneration().get(p.getWorld().getName()) + ", " + args[1]);
            if (Objects.equals(bs.vw.getVisitedWorldGeneration().get(p.getWorld().getName()), bs.arenaManager.getArena(args[1]).getfl())){
                vtp.setSpawnLocation((Player) sender);
                return true;
            }
        }
        if(args[0].equalsIgnoreCase("setmainlobby")) {
            sender.sendMessage(ChatColor.GOLD + "Main lobby set.");
            return true;
        }
        if(args[0].equalsIgnoreCase("forcestart")) {
            bs.gamemanager.GameStart(bs.match.getMatchingPlayers());
            bs.match.getMatchingPlayers().clear();
            return true;
        }
        if(args[0].equalsIgnoreCase("list")) {
            if (bs.arenaManager.getArenas().isEmpty()){
                sender.sendMessage("Empty");
            }
            else{
                sender.sendMessage("Areas: " + bs.arenaManager.getArenas().toString());
            }
            return true;
        }
        if(args[0].equalsIgnoreCase("games")){
            sender.sendMessage("GAMETASK LIST:");
            for (Game g: bs.gamemanager.getGames()){
                sender.sendMessage(g.toString());
            }
            return true;
        }
        if(args[0].equalsIgnoreCase("worldload")){
            bs.wl.openInventory((HumanEntity) sender);
        }
        if(args[0].equalsIgnoreCase("worldedit")){
            bs.we.openInventory((HumanEntity) sender);
        }
        if(args[0].equalsIgnoreCase("worldsave")){
            Player p = (Player) sender;
            String s = p.getWorld().getName();
            bs.vw.getVisitedWorldList().remove(p.getWorld());
            for (Player pl: p.getWorld().getPlayers()) {
                pl.teleport((Location) bs.worldManager.getConfig().get("endloc"));
            }
            if (Bukkit.unloadWorld(s, true)) {
                Path releaseFolder = Paths.get(bs.getDataFolder().getAbsolutePath() + "\\WorldList\\" + bs.we.Worlds.get(s));
                String MixedName = s;
                Path toFolder = Paths.get(Bukkit.getWorldContainer().getAbsolutePath() + "\\" + MixedName);
                WorldManage wrma = new WorldManage();
                WorldManage.deleteFilesRecursively(releaseFolder.toFile());
                wrma.copyWorld(toFolder.toFile(), releaseFolder.toFile());
                p.sendMessage("SAVED");
            }
        }
        if(args[0].equalsIgnoreCase("WorldFiles")){
            File dir = new File(bs.getDataFolder().getAbsolutePath() + "\\WorldList");
            File[] dirList = dir.listFiles();
            if (dirList != null) {
                for(File f: dirList){
                    sender.sendMessage(f.getName());
                }
            }
            else{
                sender.sendMessage("No Folder");
            }
        }

        if (args[0].equalsIgnoreCase("DAW")){
            for (World w: bs.vw.getVisitedWorldList()){
                bs.vw.remove(w);
            }
            bs.vw.getVisitedWorldList().clear();
        }

        if (args[0].equalsIgnoreCase("EndLocation")){
            Player p = (Player) sender;
            bs.worldManager.getConfig().set("endloc", p.getLocation());
            bs.worldManager.saveconfig();
            sender.sendMessage("DONE");
        }
        if (args[0].equalsIgnoreCase("endworld")){
            for(Game g: bs.gamemanager.getGames()){
                bs.gamemanager.GameEnd(g.getArena());
            }
        }
        if (args[0].equalsIgnoreCase("GetThread")){
            List<Game> games = bs.gth.getGames();
            for (Game gm: games){
                List<ThreadState> thrt = bs.gth.getGameThread(gm);
                sender.sendMessage(games.toString() + ": " + thrt.toString());
            }
        }
        if (args[0].equalsIgnoreCase("setscore")){
            OfflinePlayer pl = Bukkit.getOfflinePlayer(args[1]);
            int tt = Integer.parseInt(args[2]);
            bs.scoremanager.getConfig().set(pl.getUniqueId().toString(), tt);
            bs.scoremanager.saveconfig();
        }
        sender.sendMessage(PREFIX + ChatColor.RED + "Unknown subcommand. Use /pvpgame help for a list of commands.");
        return true;
    }
    private List<String> Wl(){
        File dir = new File(bs.getDataFolder().getAbsolutePath() + "\\WorldList");
        List<String> RET = new ArrayList<>();
        File[] dirList = dir.listFiles();
        if (dirList != null) {
            for(File f: dirList){
                RET.add(f.getName());
            }
        }
        return RET;
    }
}
