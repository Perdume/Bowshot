package World;

import org.bukkit.World;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class WorldManage {
    private static final String[] IGNORE_FILES = {"uid.dat", "session.dat"};

    public void copyWorld(File source, File target) {
        try {
            for (String ignored : IGNORE_FILES) {
                if (ignored.equals(source.getName())) {
                    return;
                }
            }
            if (source.isDirectory()) {
                if (!target.exists()) {
                    target.mkdirs();
                }
                String[] files = source.list();
                if (files != null) {
                    for (String file : files) {
                        File srcFile = new File(source, file);
                        File destFile = new File(target, file);
                        copyWorld(srcFile, destFile);
                    }
                }
            } else {
                try (InputStream in = new BufferedInputStream(new FileInputStream(source));
                     OutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static boolean deleteFilesRecursively(File rootFile) {
        File[] allFiles = rootFile.listFiles();
        if (allFiles != null) {
            for (File file : allFiles) {
                deleteFilesRecursively(file);
            }
        }
        return rootFile.delete();
    }
}
