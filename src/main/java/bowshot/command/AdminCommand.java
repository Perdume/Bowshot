package bowshot.command;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import bowshot.Bowshot;
import bowshot.cosmetic.TitleEffectManager;
import bowshot.debug.DebugLogger;
import bowshot.debug.Diagnostics;
import bowshot.debug.TaskState;
import bowshot.game.Game;
import bowshot.lang.MessageKey;
import bowshot.mission.AchievementType;
import bowshot.mission.MissionPeriod;
import bowshot.world.WorldFileManager;

public class AdminCommand implements CommandExecutor {

    private final Bowshot plugin;

    public AdminCommand(Bowshot plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        DebugLogger.log("Command", "/bsa executed by " + sender.getName() + " args=" + java.util.Arrays.toString(args));
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelp(sender);
                break;
            case "add":
                handleAdd(sender, args);
                break;
            case "remove":
                handleRemove(sender, args);
                break;
            case "setspawn":
                handleSetSpawn(sender, args);
                break;
            case "forcestart":
                handleForceStart(sender);
                break;
            case "list":
                handleList(sender);
                break;
            case "games":
                handleGames(sender);
                break;
            case "worldload":
                handleWorldLoad(sender);
                break;
            case "worldedit":
                handleWorldEdit(sender);
                break;
            case "worldleave":
                handleWorldLeave(sender);
                break;
            case "worldsave":
                handleWorldSave(sender);
                break;
            case "worldfiles":
                handleWorldFiles(sender);
                break;
            case "daw":
                handleDeleteAllWorlds(sender);
                break;
            case "setend":
                handleSetEnd(sender, args);
                break;
            case "setlobby":
            case "endlocation":
                handleSetLobby(sender);
                break;
            case "endworld":
                handleEndWorld(sender);
                break;
            case "getthread":
                handleGetThread(sender);
                break;
            case "setscore":
                handleSetScore(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            case "debug":
                handleDebug(sender);
                break;
            case "status":
                handleStatus(sender);
                break;
            case "errors":
                handleErrors(sender);
                break;
            case "mission":
                handleMission(sender, args);
                break;
            case "achievement":
                handleAchievement(sender, args);
                break;
            case "title":
                handleTitle(sender, args);
                break;
            case "effect":
                handleEffect(sender, args);
                break;
            case "test":
                handleTest(sender);
                break;
            default:
                sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_UNKNOWN_COMMAND));
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        String prefix = plugin.getMessageManager().get(MessageKey.PREFIX);
        sender.sendMessage(prefix + ChatColor.GOLD + " Bowshot Admin Commands");
        sender.sendMessage(ChatColor.GRAY + "/bsa help - Show commands");
        sender.sendMessage(ChatColor.GRAY + "/bsa add <name> <filename> - Create arena");
        sender.sendMessage(ChatColor.GRAY + "/bsa remove <name> - Remove arena");
        sender.sendMessage(ChatColor.GRAY + "/bsa setspawn <name> - Set spawn location");
        sender.sendMessage(ChatColor.GRAY + "/bsa forcestart - Force start game");
        sender.sendMessage(ChatColor.GRAY + "/bsa test - Solo test game (1인 테스트)");
        sender.sendMessage(ChatColor.GRAY + "/bsa list - List arenas");
        sender.sendMessage(ChatColor.GRAY + "/bsa games - List active games");
        sender.sendMessage(ChatColor.GRAY + "/bsa worldload - Open world loader");
        sender.sendMessage(ChatColor.GRAY + "/bsa worldedit - Open world editor");
        sender.sendMessage(ChatColor.GRAY + "/bsa worldleave - Leave instance world");
        sender.sendMessage(ChatColor.GRAY + "/bsa worldsave - Save edited world");
        sender.sendMessage(ChatColor.GRAY + "/bsa worldfiles - List world files");
        sender.sendMessage(ChatColor.GRAY + "/bsa setend <name> - Set per-map end location");
        sender.sendMessage(ChatColor.GRAY + "/bsa setlobby - Set global lobby location");
        sender.sendMessage(ChatColor.GRAY + "/bsa setscore <player> <value> - Set ELO");
        sender.sendMessage(ChatColor.GRAY + "/bsa reload - Reload config");
        sender.sendMessage(ChatColor.GRAY + "/bsa debug - Toggle debug mode");
        sender.sendMessage(ChatColor.GRAY + "/bsa status - 운영 진단 (ELO 분포, 존버, 매칭 등)");
        sender.sendMessage(ChatColor.GRAY + "/bsa errors - 최근 에러/경고 목록");
        sender.sendMessage(ChatColor.GRAY + "/bsa mission reset <player> <daily|weekly>");
        sender.sendMessage(ChatColor.GRAY + "/bsa mission resetall <daily|weekly>");
        sender.sendMessage(ChatColor.GRAY + "/bsa achievement grant <player> <type>");
        sender.sendMessage(ChatColor.GRAY + "/bsa achievement reset <player>");
        sender.sendMessage(ChatColor.GRAY + "/bsa title create|remove|grant|revoke|list");
        sender.sendMessage(ChatColor.GRAY + "/bsa effect create|remove|grant|revoke|list");
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length <= 2) {
            sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_USAGE_ADD));
            return;
        }
        String name = args[1];
        if (plugin.getArenaManager().exists(name)) {
            sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_ARENA_EXISTS));
            return;
        }
        for (String worldFile : getWorldFileNames()) {
            if (worldFile.equals(args[2])) {
                plugin.getArenaManager().registerArena(name, worldFile);
                sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_ARENA_CREATED));
                return;
            }
        }
        sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_NO_FILE));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length == 1) {
            sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_USAGE_REMOVE));
            return;
        }
        if (!plugin.getArenaManager().exists(args[1])) {
            sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_ARENA_NOT_FOUND));
            return;
        }
        plugin.getArenaManager().removeArena(args[1]);
        sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_ARENA_REMOVED));
    }

    private void handleSetSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_PLAYERS_ONLY));
            return;
        }
        if (args.length < 2) return;

        Player player = (Player) sender;
        String originalName = plugin.getInstanceWorldManager().getOriginalName(player.getWorld().getName());
        if (originalName != null && plugin.getArenaManager().exists(args[1])) {
            String arenaTemplate = plugin.getArenaManager().getArena(args[1]).getTemplateFolder();
            if (Objects.equals(originalName, arenaTemplate)) {
                plugin.getSpawnManager().setSpawnLocation(player);
                sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_SPAWN_SET));
            }
        }
    }

    private void handleForceStart(CommandSender sender) {
        List<Player> players = new ArrayList<>(plugin.getMatchQueue().getQueue());
        if (players.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No players in queue.");
            return;
        }
        plugin.getGameManager().startGame(players);
        plugin.getMatchQueue().clear();
    }

    private void handleTest(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_PLAYERS_ONLY));
            return;
        }
        // 큐에 없으면 자동 추가
        if (!plugin.getMatchQueue().isInQueue(player)) {
            plugin.getMatchQueue().join(player);
        }
        // 바로 강제 시작 (1인 가능)
        List<Player> players = new ArrayList<>(plugin.getMatchQueue().getQueue());
        if (players.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Failed to join queue.");
            return;
        }
        plugin.getGameManager().startGame(players);
        plugin.getMatchQueue().clear();
        sender.sendMessage(ChatColor.GREEN + "Solo test game started.");
    }

    private void handleList(CommandSender sender) {
        List<String> arenas = plugin.getArenaManager().getArenaNames();
        if (arenas.isEmpty()) {
            sender.sendMessage("No arenas registered.");
        } else {
            sender.sendMessage("Arenas: " + arenas);
        }
    }

    private void handleGames(CommandSender sender) {
        sender.sendMessage("Active games:");
        for (Game game : plugin.getGameManager().getActiveGames()) {
            sender.sendMessage(" - " + game.getArenaInstance().getWorldName()
                    + " [" + game.getArenaInstance().getAliveCount() + " alive]");
        }
    }

    private void handleWorldLoad(CommandSender sender) {
        if (sender instanceof Player) {
            plugin.getWorldLoaderGui().openInventory((Player) sender);
        }
    }

    private void handleWorldEdit(CommandSender sender) {
        if (sender instanceof Player) {
            plugin.getWorldEditorGui().openInventory((Player) sender);
        }
    }

    private void handleWorldLeave(CommandSender sender) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        World world = player.getWorld();

        if (!plugin.getInstanceWorldManager().isInstanceWorld(world)) {
            sender.sendMessage(ChatColor.RED + "인스턴스 월드가 아닙니다.");
            return;
        }

        // Clean up edit mapping if exists
        plugin.getWorldEditorGui().getEditWorldMapping().remove(world.getName());

        Location lobbyLoc = plugin.getSpawnManager().getLobbyLocation();
        if (lobbyLoc != null) {
            player.teleport(lobbyLoc);
        }
        sender.sendMessage(plugin.getMessageManager().get(MessageKey.VISIT_LEFT));

        // Clean up world if empty
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (world.getPlayers().isEmpty()) {
                plugin.getInstanceWorldManager().removeWorld(world);
            }
        }, 5L);
    }

    private void handleWorldSave(CommandSender sender) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        String worldName = player.getWorld().getName();

        String templateName = plugin.getWorldEditorGui().getEditWorldMapping().get(worldName);
        if (templateName == null) {
            sender.sendMessage("This world is not being edited.");
            return;
        }

        plugin.getInstanceWorldManager().getActiveWorlds().remove(player.getWorld());

        Location lobbyLoc = plugin.getSpawnManager().getLobbyLocation();
        for (Player pl : player.getWorld().getPlayers()) {
            if (lobbyLoc != null) pl.teleport(lobbyLoc);
        }

        if (Bukkit.unloadWorld(worldName, true)) {
            Path templatePath = Paths.get(plugin.getDataFolder().getAbsolutePath(),
                    "WorldList", templateName);
            Path worldPath = Paths.get(Bukkit.getWorldContainer().getAbsolutePath(), worldName);

            WorldFileManager.deleteRecursively(templatePath.toFile());
            new WorldFileManager().copyWorld(worldPath.toFile(), templatePath.toFile());
            sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_WORLD_SAVED));
        }
    }

    private void handleWorldFiles(CommandSender sender) {
        for (String name : getWorldFileNames()) {
            sender.sendMessage(name);
        }
    }

    private void handleDeleteAllWorlds(CommandSender sender) {
        List<World> worlds = new ArrayList<>(plugin.getInstanceWorldManager().getActiveWorlds());
        for (World w : worlds) {
            plugin.getInstanceWorldManager().removeWorld(w);
        }
        sender.sendMessage("All instance worlds deleted.");
    }

    private void handleSetEnd(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_PLAYERS_ONLY));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bsa setend <arenaName>");
            return;
        }
        Player player = (Player) sender;
        String originalName = plugin.getInstanceWorldManager().getOriginalName(player.getWorld().getName());
        if (originalName != null && plugin.getArenaManager().exists(args[1])) {
            String arenaTemplate = plugin.getArenaManager().getArena(args[1]).getTemplateFolder();
            if (Objects.equals(originalName, arenaTemplate)) {
                plugin.getSpawnManager().setEndLocation(player, originalName);
                sender.sendMessage(ChatColor.GREEN + "End location set for " + args[1]);
            }
        }
    }

    private void handleSetLobby(CommandSender sender) {
        if (!(sender instanceof Player)) return;
        Player player = (Player) sender;
        plugin.getSpawnManager().setLobbyLocation(player);
        sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_END_LOCATION_SET));
    }

    private void handleEndWorld(CommandSender sender) {
        for (Game game : new ArrayList<>(plugin.getGameManager().getActiveGames())) {
            plugin.getGameManager().endGame(game.getArenaInstance());
        }
        sender.sendMessage("All games ended.");
    }

    private void handleGetThread(CommandSender sender) {
        for (Game game : plugin.getTaskDebugger().getActiveGames()) {
            List<TaskState> states = plugin.getTaskDebugger().getGameTasks(game);
            sender.sendMessage(game.getArenaInstance().getWorldName() + ": " + states);
        }
    }

    private void handleSetScore(CommandSender sender, String[] args) {
        if (args.length < 3) return;
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        try {
            int elo = Integer.parseInt(args[2]);
            plugin.getStatsManager().setMmr(target.getUniqueId(), elo);
            sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_SCORE_SET));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid number.");
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getSettings().load(plugin.getConfig());
        plugin.getMessageManager().load(plugin.getSettings().getLanguage());
        plugin.getTitleEffectManager().reload();
        sender.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
    }

    private void handleDebug(CommandSender sender) {
        boolean current = DebugLogger.isEnabled();
        DebugLogger.setEnabled(!current);
        plugin.getSettings().setDebug(!current);
        sender.sendMessage(ChatColor.GREEN + "Debug mode: " + (!current ? ChatColor.YELLOW + "ON" : ChatColor.RED + "OFF"));
    }

    private void handleStatus(CommandSender sender) {
        Diagnostics diag = new Diagnostics(plugin);
        for (String line : diag.runFullDiagnostics()) {
            sender.sendMessage(line);
        }
    }

    private void handleErrors(CommandSender sender) {
        List<String> errors = DebugLogger.getRecentErrors();
        if (errors.isEmpty()) {
            sender.sendMessage(ChatColor.GREEN + "최근 에러/경고 없음");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "========== 최근 에러 (" + errors.size() + "건) ==========");
        int start = Math.max(0, errors.size() - 20);
        for (int i = start; i < errors.size(); i++) {
            sender.sendMessage(ChatColor.GRAY + "[" + (i + 1) + "] " + ChatColor.RED + errors.get(i));
        }
        sender.sendMessage(ChatColor.GRAY + "'/bsa errors' 로 확인. 로그는 최대 100건 보관.");
    }

    private void handleMission(CommandSender sender, String[] args) {
        // /bsa mission reset <player> <daily|weekly>
        // /bsa mission resetall <daily|weekly>
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /bsa mission reset|resetall ...");
            return;
        }

        if (args[1].equalsIgnoreCase("resetall")) {
            MissionPeriod period;
            try {
                period = MissionPeriod.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "daily or weekly");
                return;
            }
            plugin.getMissionManager().resetAllMissions(period);
            sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_MISSION_RESETALL,
                    period.name().toLowerCase()));
            return;
        }

        if (args[1].equalsIgnoreCase("reset")) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Usage: /bsa mission reset <player> <daily|weekly>");
                return;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            MissionPeriod period;
            try {
                period = MissionPeriod.valueOf(args[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "daily or weekly");
                return;
            }
            plugin.getMissionManager().resetPlayerMissions(target.getUniqueId(), period);
            sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_MISSION_RESET,
                    target.getName(), period.name().toLowerCase()));
            return;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /bsa mission reset|resetall ...");
    }

    private void handleAchievement(CommandSender sender, String[] args) {
        // /bsa achievement grant <player> <type>
        // /bsa achievement reset <player>
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /bsa achievement grant|reset ...");
            return;
        }

        if (args[1].equalsIgnoreCase("grant")) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.RED + "Usage: /bsa achievement grant <player> <type>");
                return;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            AchievementType type;
            try {
                type = AchievementType.valueOf(args[3].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Unknown achievement: " + args[3]);
                return;
            }
            plugin.getAchievementManager().grantAchievement(target.getUniqueId(), type);
            sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_ACHIEVEMENT_GRANT,
                    target.getName(), type.name()));
            return;
        }

        if (args[1].equalsIgnoreCase("reset")) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            plugin.getAchievementManager().resetPlayer(target.getUniqueId());
            sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_ACHIEVEMENT_RESET,
                    target.getName()));
            return;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /bsa achievement grant|reset ...");
    }

    private void handleTitle(CommandSender sender, String[] args) {
        TitleEffectManager tem = plugin.getTitleEffectManager();
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bsa title create|remove|grant|revoke|list");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create": {
                // /bsa title create <id> <display...>
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bsa title create <id> <display...>");
                    return;
                }
                String id = args[2].toLowerCase();
                if (tem.getRegisteredTitles().containsKey(id)) {
                    sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_TITLE_EXISTS));
                    return;
                }
                StringBuilder display = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    if (i > 3) display.append(" ");
                    display.append(args[i]);
                }
                tem.createTitle(id, display.toString());
                sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_TITLE_CREATED, id));
                break;
            }
            case "remove": {
                // /bsa title remove <id>
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bsa title remove <id>");
                    return;
                }
                String id = args[2].toLowerCase();
                if (!tem.getRegisteredTitles().containsKey(id)) {
                    sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_TITLE_NOT_FOUND));
                    return;
                }
                tem.removeTitle(id);
                sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_TITLE_REMOVED, id));
                break;
            }
            case "grant": {
                // /bsa title grant <player> <id>
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bsa title grant <player> <id>");
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                String id = args[3].toLowerCase();
                if (!tem.getRegisteredTitles().containsKey(id)) {
                    sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_TITLE_NOT_FOUND));
                    return;
                }
                tem.grantTitle(target.getUniqueId(), id);
                sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_TITLE_GRANTED,
                        target.getName(), id));
                break;
            }
            case "revoke": {
                // /bsa title revoke <player> <id>
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bsa title revoke <player> <id>");
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                String id = args[3].toLowerCase();
                tem.revokeTitle(target.getUniqueId(), id);
                sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_TITLE_REVOKED,
                        target.getName(), id));
                break;
            }
            case "list": {
                sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_TITLE_LIST));
                for (var entry : tem.getRegisteredTitles().entrySet()) {
                    sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_TITLE_LISTENTRY,
                            entry.getKey(),
                            ChatColor.translateAlternateColorCodes('&', entry.getValue().display())));
                }
                break;
            }
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /bsa title create|remove|grant|revoke|list");
        }
    }

    private void handleEffect(CommandSender sender, String[] args) {
        TitleEffectManager tem = plugin.getTitleEffectManager();
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /bsa effect create|remove|grant|revoke|list");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create": {
                // /bsa effect create <id> <particle> [sound] [count]
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bsa effect create <id> <particle> [sound] [count]");
                    return;
                }
                String id = args[2].toLowerCase();
                if (tem.getRegisteredEffects().containsKey(id)) {
                    sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_EFFECT_EXISTS));
                    return;
                }
                Particle particle;
                try {
                    particle = Particle.valueOf(args[3].toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid particle: " + args[3]);
                    return;
                }
                Sound sound = null;
                int count = 10;
                if (args.length >= 5) {
                    try {
                        sound = Sound.valueOf(args[4].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid sound: " + args[4]);
                        return;
                    }
                }
                if (args.length >= 6) {
                    try {
                        count = Integer.parseInt(args[5]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid count: " + args[5]);
                        return;
                    }
                }
                tem.createEffect(id, particle, sound, count);
                sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_EFFECT_CREATED, id));
                break;
            }
            case "remove": {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bsa effect remove <id>");
                    return;
                }
                String id = args[2].toLowerCase();
                if (!tem.getRegisteredEffects().containsKey(id)) {
                    sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_EFFECT_NOT_FOUND));
                    return;
                }
                tem.removeEffect(id);
                sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_EFFECT_REMOVED, id));
                break;
            }
            case "grant": {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bsa effect grant <player> <id>");
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                String id = args[3].toLowerCase();
                if (!tem.getRegisteredEffects().containsKey(id)) {
                    sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_EFFECT_NOT_FOUND));
                    return;
                }
                tem.grantEffect(target.getUniqueId(), id);
                sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_EFFECT_GRANTED,
                        target.getName(), id));
                break;
            }
            case "revoke": {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /bsa effect revoke <player> <id>");
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                String id = args[3].toLowerCase();
                tem.revokeEffect(target.getUniqueId(), id);
                sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_EFFECT_REVOKED,
                        target.getName(), id));
                break;
            }
            case "list": {
                sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_EFFECT_LIST));
                for (var entry : tem.getRegisteredEffects().entrySet()) {
                    var def = entry.getValue();
                    sender.sendMessage(plugin.getMessageManager().get(MessageKey.ADMIN_EFFECT_LISTENTRY,
                            entry.getKey(), def.particle().name(),
                            def.sound() != null ? def.sound().name() : "none"));
                }
                break;
            }
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /bsa effect create|remove|grant|revoke|list");
        }
    }

    private List<String> getWorldFileNames() {
        List<String> names = new ArrayList<>();
        File dir = new File(plugin.getDataFolder(), "WorldList");
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    names.add(f.getName());
                }
            }
        }
        return names;
    }
}
