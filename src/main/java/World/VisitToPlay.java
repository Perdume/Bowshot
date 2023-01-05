package World;

import bowshot.bowshot.Bowshot;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class VisitToPlay {
    private Bowshot bs=Bowshot.getPlugin(Bowshot.class);

    public void setSpawnLocation(Player p){
        Location loc = p.getLocation();
        String worldchangeName = bs.vw.getVisitedWorldGeneration().get(loc.getWorld().getName());
        bs.locationmanager.getConfig().set("location."+worldchangeName+".x", loc.getX());
        bs.locationmanager.getConfig().set("location."+worldchangeName+".y", loc.getY());
        bs.locationmanager.getConfig().set("location."+worldchangeName+".z", loc.getZ());
        p.sendMessage(ChatColor.GREEN + "Generated!");
        p.sendMessage(ChatColor.GREEN + worldchangeName + ", " + bs.locationmanager.getConfig().get("location."+worldchangeName+".x") + ", " + bs.locationmanager.getConfig().get("location."+worldchangeName + ".y") + ", " + bs.locationmanager.getConfig().get("location."+worldchangeName+".z"));
        bs.locationmanager.saveconfig();
    }
    public Location getLocation(String s, World w) {
        return (new Location(w, (double) bs.locationmanager.getConfig().get("location."+s+".x"), (double) bs.locationmanager.getConfig().get("location."+s+".y"), (double) bs.locationmanager.getConfig().get("location."+s+".z")));

    }
}
