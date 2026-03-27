package Game;

import java.io.File;
import java.util.Random;

public class GameUtil {
    private static final Random random = new Random();

    /**
     * Generate a random alphanumeric string for unique world names.
     */
    public static String randomName() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    /**
     * Build a file path using the platform-independent separator.
     */
    public static String buildPath(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            sb.append(parts[i]);
            if (i < parts.length - 1) {
                sb.append(File.separator);
            }
        }
        return sb.toString();
    }
}
