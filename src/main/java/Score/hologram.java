package Score;

import bowshot.bowshot.Bowshot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static eu.decentsoftware.holograms.api.Settings.getConfig;

public class hologram {
    private Bowshot bs;
    public hologram(Bowshot bw){
        this.bs = bw;
    }
    public List<String> sort(){
        List<String> sorting = new ArrayList<>();
        sorting.add(0, ChatColor.GREEN + "RANKING");
        HashMap<String, Integer> players = new HashMap<String, Integer>();

        for(OfflinePlayer playerName: Bukkit.getOfflinePlayers()){

            players.put(playerName.getName(), bs.scoremanager.getConfig().getInt(playerName.getUniqueId().toString()));
        }


        String nextTop = "";
        Integer nextTopKills = 0;

        for(int i = 1; i < 11; i++){
            for(String playerName: players.keySet()){
                if(players.get(playerName) > nextTopKills){
                    nextTop = playerName;
                    nextTopKills = players.get(playerName);
                }
            }
            if (i == 1){
                sorting.add(ChatColor.GOLD.toString() + i + " " + nextTop + " : " + nextTopKills);
            }
            else if (i == 2){
                sorting.add(ChatColor.GRAY.toString() + i + " " + nextTop + " : " + nextTopKills);
            }
            else if (i == 3){
                sorting.add(ChatColor.DARK_PURPLE.toString() + i + " " + nextTop + " : " + nextTopKills);
            }
            else {
                sorting.add(i + " " + nextTop + " : " + nextTopKills);
            }
            players.remove(nextTop);
            nextTop = "";
            nextTopKills = 0;
        }
        return sorting;
    }
}
