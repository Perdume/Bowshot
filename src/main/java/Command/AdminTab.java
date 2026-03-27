package Command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminTab implements TabCompleter {
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return new ArrayList<>(Arrays.asList("help", "add", "remove", "setspawn", "setmainlobby", "forcestart", "list", "games", "worldload", "worldedit", "worldsave", "WorldFiles", "EndLocation", "endworld", "setscore", "reload"));
        }
        return null;
    }
}
