package bowshot.game;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorChat {

    private static final Pattern HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");

    public static String colorize(String message) {
        if (message == null) return "";
        if (message.contains("<#")) {
            message = applyHexColors(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String applyHexColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        while (matcher.find()) {
            net.md_5.bungee.api.ChatColor hexColor = net.md_5.bungee.api.ChatColor.of(
                    matcher.group().substring(1, matcher.group().length() - 1));
            String before = message.substring(0, matcher.start());
            String after = message.substring(matcher.end());
            message = before + hexColor + after;
            matcher = HEX_PATTERN.matcher(message);
        }
        return message;
    }
}
