package Manager;


import Area.Arena;
import bowshot.bowshot.Bowshot;

import java.util.*;

// NOT THREAD SAFE!
public class ArenaManager{
    private Map<String, Arena> arenas = new HashMap<String, Arena>();
    private ArrayList<String> areaname = new ArrayList<>();
    private Bowshot bs = Bowshot.getPlugin(Bowshot.class);
    public void arealoader(){
        try {
            if (bs.worldManager.getConfig().get("Worldlist", areaname) == null){
                return;
            }
            areaname = (ArrayList<String>) bs.worldManager.getConfig().get("Worldlist", areaname);
            for (String s : areaname) {
                arenas.put(s, new Arena(s, (String) bs.worldManager.getConfig().get("World." + s + ".filename")));
            }
        }
        catch(Exception e){
            return;
        }
    }

    public void registerArena(String name, String filename) {
        bs.worldManager.getConfig().set("World."+name+".filename", filename);
        arenas.put(name, new Arena(name, filename));
        if(areaname != null){
            areaname.add(name);
            bs.worldManager.getConfig().set("Worldlist", areaname);
        }
        else{
            areaname = new ArrayList<>(Collections.singletonList(name));
            bs.worldManager.getConfig().set("Worldlist", areaname);
        }
        bs.worldManager.saveconfig();
    }

    public void remove(String name) {
        arenas.remove(name);
        areaname.remove(name);
        bs.worldManager.getConfig().set("Worldlist", areaname);
        bs.worldManager.getConfig().set("World."+name+".filename", null);
        bs.worldManager.saveconfig();
    }

    public Arena getArena(String name) {
        return arenas.get(name);
    }
    public List<String> getArenas() {
        return areaname;
    }

    public boolean exists(String name) {
        return arenas.get(name) != null;
    }

}
