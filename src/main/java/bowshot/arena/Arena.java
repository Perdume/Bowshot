package bowshot.arena;

import bowshot.Bowshot;

import java.io.File;

public class Arena {

    private final String name;
    private final String templateFolder;

    public Arena(String name, String templateFolder) {
        this.name = name;
        this.templateFolder = templateFolder;
    }

    public String getName() {
        return name;
    }

    public String getTemplateFolder() {
        return templateFolder;
    }

    public File getTemplateFile() {
        Bowshot plugin = Bowshot.getInstance();
        return new File(plugin.getDataFolder(), "WorldList" + File.separator + templateFolder);
    }
}
