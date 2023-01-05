package Command;

import bowshot.bowshot.Bowshot;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Match {
    private List<Player> MatchingList = new ArrayList<>();
    Bowshot bs;
    public Match(Bowshot p){
        bs = p;
    }

    public Boolean IsPlayerIn(Player p){
        for (Player pl: MatchingList){
            if (pl == p){
                return true;
            }
        }
        return false;
    }

    public Boolean join(Player p){
        if (!IsPlayerIn(p)){
            MatchingList.add(p);
            return true;
        }
        else{
            return false;
        }
    }
    public Boolean leave(Player p){
        if (IsPlayerIn(p)){
            MatchingList.remove(p);
            return true;
        }
        else{
            return false;
        }
    }
    public List<Player> getMatchingPlayers(){
        return MatchingList;
    }

}
