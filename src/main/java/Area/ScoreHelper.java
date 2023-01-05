package Area;

import bowshot.bowshot.Bowshot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ScoreHelper {
    Bowshot bs;
    public ScoreHelper(Bowshot bw){
        bs = bw;
    }
    public void jointeam(Player p, Scoreboard board){
        getTeam(board ).addPlayer(p);
        p.setScoreboard(board);
    }
    public void leaveteam(Player p, Scoreboard board){
        getTeam(board).removePlayer(p);
        p.setScoreboard(board);
    }
    public Team getTeam(Scoreboard board){
        Team t = board.getTeam("teamname");
        if (t == null) {
            Team team = board.registerNewTeam("teamname");
            team.setNameTagVisibility(NameTagVisibility.NEVER);
            return team;
        }
        else{
            return t;
        }
    }
    public Objective getScore(String w, Scoreboard board){
        Objective obj = board.getObjective(w);
        if (obj == null) {
            Objective objective = board.registerNewObjective(w, "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.setDisplayName("Name");
            return objective;
        }
        else{
            return obj;
        }
    }
    public void SetTitle(String str, String worldname, Scoreboard board){
        str = ChatColor.translateAlternateColorCodes('&', str);
        getScore(worldname, board).setDisplayName(str);
    }
    public void SetScore(int in, String str, String worldname, Scoreboard board){
        str = ChatColor.translateAlternateColorCodes('&', str);
        Score score = getScore(worldname, board).getScore(str);
        score.setScore(in);
    }
    public void SetScores(String worldname, Scoreboard board, String... strs){
        String name = board.getObjective(worldname).getDisplayName();
//        getScore(worldname, board).unregister();
//        getScore(worldname, board).setDisplayName(name);
        for (String s: board.getEntries()){
            board.resetScores(s);
        }
        int i = strs.length;
        for (String s: strs){
            s = ChatColor.translateAlternateColorCodes('&', s);
            Score score = getScore(worldname, board).getScore(s);
            score.setScore(i);
            i --;
        }
    }
    public void updatePerLine(Scoreboard board, String removeline, String addline, int scoreSlot) {
        if (board == null) {
            return;
        }
        removeline = ChatColor.translateAlternateColorCodes('&', removeline);
        for (String str : board.getEntries()) {
            if (str.contains(removeline) || str.equals(removeline)) {
                board.resetScores(str);
            }
        }
        addline = ChatColor.translateAlternateColorCodes('&', addline);
        board.getObjective(DisplaySlot.SIDEBAR).getScore(addline).setScore(scoreSlot);
    }
}
