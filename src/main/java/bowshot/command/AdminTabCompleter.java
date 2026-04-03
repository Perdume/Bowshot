package bowshot.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import bowshot.Bowshot;

public class AdminTabCompleter implements TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList(
            "help", "add", "remove", "setspawn", "setend", "setlobby", "forcestart", "test", "list", "games",
            "worldload", "worldedit", "worldleave", "worldsave", "worldfiles", "daw",
            "endlocation", "endworld", "getthread", "setscore", "reload", "debug",
            "status", "errors", "mission", "achievement", "title", "effect"
    );

    private final Bowshot plugin;

    public AdminTabCompleter(Bowshot plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(SUB_COMMANDS, args[0]);
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "remove":
                case "setspawn":
                case "setend":
                    return filterStartsWith(plugin.getArenaManager().getArenaNames(), args[1]);
                case "add":
                    return filterStartsWith(getWorldFileNames(), args[1]);
                case "setscore":
                    return null; // player names
                case "mission":
                    return filterStartsWith(Arrays.asList("reset", "resetall"), args[1]);
                case "achievement":
                    return filterStartsWith(Arrays.asList("grant", "reset"), args[1]);
                case "title":
                    return filterStartsWith(Arrays.asList("create", "remove", "grant", "revoke", "list"), args[1]);
                case "effect":
                    return filterStartsWith(Arrays.asList("create", "remove", "grant", "revoke", "list"), args[1]);
            }
        }
        if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "add":
                    return filterStartsWith(getWorldFileNames(), args[2]);
                case "mission":
                    if (args[1].equalsIgnoreCase("reset")) return null; // player names
                    if (args[1].equalsIgnoreCase("resetall"))
                        return filterStartsWith(Arrays.asList("daily", "weekly"), args[2]);
                    break;
                case "achievement":
                    return null; // player names
                case "title":
                    if (args[1].equalsIgnoreCase("grant") || args[1].equalsIgnoreCase("revoke"))
                        return null; // player names
                    if (args[1].equalsIgnoreCase("remove")) {
                        return filterStartsWith(
                                new ArrayList<>(plugin.getTitleEffectManager().getRegisteredTitles().keySet()), args[2]);
                    }
                    break;
                case "effect":
                    if (args[1].equalsIgnoreCase("grant") || args[1].equalsIgnoreCase("revoke"))
                        return null; // player names
                    if (args[1].equalsIgnoreCase("remove")) {
                        return filterStartsWith(
                                new ArrayList<>(plugin.getTitleEffectManager().getRegisteredEffects().keySet()), args[2]);
                    }
                    if (args[1].equalsIgnoreCase("create")) {
                        // suggest particle types
                        List<String> particles = new ArrayList<>();
                        for (org.bukkit.Particle p : org.bukkit.Particle.values()) {
                            particles.add(p.name().toLowerCase());
                        }
                        return filterStartsWith(particles, args[2]);
                    }
                    break;
            }
        }
        if (args.length == 4) {
            switch (args[0].toLowerCase()) {
                case "mission":
                    if (args[1].equalsIgnoreCase("reset"))
                        return filterStartsWith(Arrays.asList("daily", "weekly"), args[3]);
                    break;
                case "achievement":
                    if (args[1].equalsIgnoreCase("grant")) {
                        List<String> types = new java.util.ArrayList<>();
                        for (bowshot.mission.AchievementType t : bowshot.mission.AchievementType.values()) {
                            types.add(t.name().toLowerCase());
                        }
                        return filterStartsWith(types, args[3]);
                    }
                    break;
                case "title":
                    if (args[1].equalsIgnoreCase("grant") || args[1].equalsIgnoreCase("revoke")) {
                        return filterStartsWith(
                                new ArrayList<>(plugin.getTitleEffectManager().getRegisteredTitles().keySet()), args[3]);
                    }
                    break;
                case "effect":
                    if (args[1].equalsIgnoreCase("grant") || args[1].equalsIgnoreCase("revoke")) {
                        return filterStartsWith(
                                new ArrayList<>(plugin.getTitleEffectManager().getRegisteredEffects().keySet()), args[3]);
                    }
                    break;
            }
        }
        return new ArrayList<>();
    }

    private List<String> filterStartsWith(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }

    private List<String> getWorldFileNames() {
        List<String> names = new ArrayList<>();
        File dir = new File(plugin.getDataFolder(), "WorldList");
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) names.add(f.getName());
            }
        }
        return names;
    }
}
