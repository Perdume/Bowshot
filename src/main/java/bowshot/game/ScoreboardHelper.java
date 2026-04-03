package bowshot.game;

import bowshot.Bowshot;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class ScoreboardHelper {

    private final Bowshot plugin;

    public ScoreboardHelper(Bowshot plugin) {
        this.plugin = plugin;
    }

    public void joinTeam(Player player, Scoreboard board) {
        getOrCreateTeam(board).addPlayer(player);
        player.setScoreboard(board);
    }

    public void leaveTeam(Player player, Scoreboard board) {
        getOrCreateTeam(board).removePlayer(player);
        player.setScoreboard(board);
    }

    private Team getOrCreateTeam(Scoreboard board) {
        Team team = board.getTeam("bowshot_hide");
        if (team == null) {
            team = board.registerNewTeam("bowshot_hide");
            team.setNameTagVisibility(NameTagVisibility.NEVER);
        }
        return team;
    }

    public Objective getOrCreateObjective(String id, Scoreboard board) {
        Objective obj = board.getObjective(id);
        if (obj == null) {
            obj = board.registerNewObjective(id, "dummy", "Bowshot");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            obj.setDisplayName("Bowshot");
        }
        return obj;
    }

    public void setTitle(String title, String objectiveId, Scoreboard board) {
        title = ChatColor.translateAlternateColorCodes('&', title);
        getOrCreateObjective(objectiveId, board).setDisplayName(title);
    }

    public void setLine(int score, String text, String objectiveId, Scoreboard board) {
        text = ChatColor.translateAlternateColorCodes('&', text);
        getOrCreateObjective(objectiveId, board).getScore(text).setScore(score);
    }

    public void setLines(String objectiveId, Scoreboard board, String... lines) {
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }
        int score = lines.length;
        for (String line : lines) {
            line = ChatColor.translateAlternateColorCodes('&', line);
            getOrCreateObjective(objectiveId, board).getScore(line).setScore(score);
            score--;
        }
    }

    public void updateLine(Scoreboard board, String oldLine, String newLine, int scoreSlot) {
        if (board == null) return;

        oldLine = ChatColor.translateAlternateColorCodes('&', oldLine);
        for (String entry : board.getEntries()) {
            if (entry.contains(oldLine) || entry.equals(oldLine)) {
                board.resetScores(entry);
            }
        }

        newLine = ChatColor.translateAlternateColorCodes('&', newLine);
        Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
        if (obj != null) {
            obj.getScore(newLine).setScore(scoreSlot);
        }
    }
}
