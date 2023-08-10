package v2.production.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigReader {

    private final long serialId;
    private final Map<String, String> configVariables = new HashMap<>();

    public ConfigReader(Path root) {
        try {
            var lines = Files.readAllLines(root.resolve("config.txt"));
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

    public long serialId() {
        return serialId;
    }

    public boolean missing(String key) {
        return !configVariables.containsKey(key);
    }

    public String var(String key) {
        return var(key, "");
    }

    public String var(String key, String defaultValue) {
        return configVariables.getOrDefault(key, defaultValue);
    }
}
