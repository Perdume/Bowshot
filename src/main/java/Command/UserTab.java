package Command;

import bowshot.bowshot.Bowshot;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserTab implements TabCompleter {
    private Bowshot bs = Bowshot.getPlugin(Bowshot.class);
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length ==1){
            return new ArrayList<>(Arrays.asList("join", "leave", "spect"));
        }
        return null;
    }
}
