package Area;

import User.User;
import bowshot.bowshot.Bowshot;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SubArena {
    private Bowshot bs = Bowshot.getPlugin(Bowshot.class);

    private String name;
    private Location spawnLocation;

    private List<User> players = new ArrayList<>();

    public SubArena(String nam, Location spawnLocation) {
        this.name = nam;
        this.spawnLocation = spawnLocation;
    }

    public void join(Player p) {
        if (!players.contains(bs.usermanager.getUser(p.getUniqueId()))) {
            players.add(bs.usermanager.getUser(p.getUniqueId()));
        }
    }

    public void leave(Player p) {
        players.remove(bs.usermanager.getUser(p.getUniqueId()));
    }

    public List<User> getPlayers() {
        return players;
    }
    public void ClearPlayers() {
        players.clear();
    }
    public String getArenaName(){
        return name;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }


    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }


    public List<User> getSpectators(){
        List<User> spe = new ArrayList<>();
        for(User p: getPlayers()){
            if (p.isSpectator()){
                if (spe.isEmpty()) {
                    List<User> temp = new ArrayList<>(Arrays.asList(p));
                    spe = temp;
                }
                else{
                    spe.add(p);
                }
            }
        }
        return spe;
    }


}