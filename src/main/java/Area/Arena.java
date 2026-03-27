package Area;

import Game.GameUtil;
import bowshot.bowshot.Bowshot;

import java.io.File;

public class Arena {
    private Bowshot bs = Bowshot.getPlugin(Bowshot.class);

    private String name;
    private String filename;


    public Arena(String nam, String filename) {
        this.name = nam;
        this.filename = filename;
    }
    public String getArenaName(){
        return name;
    }
    public File getWorldLoader(){
        return new File(GameUtil.buildPath(bs.getDataFolder().getAbsolutePath(), "WorldList", filename));
    }
    public String getfl(){
        return filename;
    }
}