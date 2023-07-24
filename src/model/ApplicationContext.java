package model;

import java.util.HashMap;
import java.util.Map;

public class ApplicationContext {

    private final Map<Class<? extends Module>, Module> registry = new HashMap<>();

    public void register(Module m) {
        m.providers().forEach(k -> registry.put(k, m));
    }

    public void validate() {
        registry.values().forEach(m -> {
            m.dependencies().forEach(d -> {
                if (!registry.containsKey(d)) throw new IllegalStateException("unresolved dependency: "+d);
            });
        });
    }

    public <T extends Module> T resolve(Class<T> moduleClass) {
        return (T) registry.get(moduleClass);
    }

}
