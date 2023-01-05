package Area;

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
        File fi = new File(bs.getDataFolder().getAbsolutePath() + "\\WorldList\\" + filename);
        return fi;
    }
    public String getfl(){
        return filename;
    }




}