package production;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Config {

    public static final Path fsRoot;

    public static final long serialId;
    private static final Map<String, String> configVariables;

    public static boolean missing(String key) {
        return !configVariables.containsKey(key);
    }

    public static String var(String key) {
        return var(key, "");
    }

    public static String var(String key, String defaultValue) {
        return configVariables.getOrDefault(key, defaultValue);
    }

    static {
        // might need to be set dynamically in the future
        fsRoot = Path.of("/home/pi/lora-mesh");

        configVariables = new HashMap<>();
        try {
            var lines = Files.readAllLines(fsRoot.resolve("config.txt"));
            for (String line : lines) {
                int breakIndex = line.indexOf("=");
                if (breakIndex == -1) continue;
                configVariables.put(line.substring(0, breakIndex), line.substring(breakIndex + 1));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (missing("sid")) throw new IllegalStateException("sid must be specified in config.txt");

        serialId = Long.parseLong(var("sid"));

    }
}
