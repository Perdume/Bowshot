package bowshot.command;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class UserTabCompleter implements TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList("join", "leave", "visit", "spect", "stats", "mission", "achievement", "replay", "title", "effect");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String lower = args[0].toLowerCase();
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(lower))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("mission") || sub.equals("achievement")) {
                return Arrays.asList("claim").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (sub.equals("title") || sub.equals("effect")) {
                return Arrays.asList("equip", "unequip").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("mission") && args[1].equalsIgnoreCase("claim")) {
                return Arrays.stream(bowshot.mission.MissionType.values())
                        .map(m -> m.name().toLowerCase())
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (sub.equals("achievement") && args[1].equalsIgnoreCase("claim")) {
                return Arrays.stream(bowshot.mission.AchievementType.values())
                        .map(a -> a.name().toLowerCase())
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return Arrays.asList();
    }
}
