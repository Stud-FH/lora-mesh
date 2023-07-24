package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ApplicationContext {

    private final Map<Class<? extends Module>, Module> registry;
    private final Collection<Module> modules;

    public ApplicationContext(Map<Class<? extends Module>, Module> registry, Collection<Module> modules) {
        this.registry = registry;
        this.modules = modules;
    }

    public ApplicationContext deploy() {
        modules.forEach(Module::deploy);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T resolve(Class<T> moduleClass) {
        return (T) registry.get(moduleClass);
    }

    public static class Builder {
        private final Map<Class<? extends Module>, Module> registry = new HashMap<>();
        private final Collection<Module> modules = new ArrayList<>();

        public ApplicationContext.Builder register(Module m) {
            modules.add(m);
            m.providers().forEach(k -> registry.put(k, m));
            return this;
        }

        public ApplicationContext build() {
            modules.forEach(m -> m.dependencies().forEach(d -> {
                if (!registry.containsKey(d)) throw new IllegalStateException("unresolved dependency: "+d);
            }));

            ApplicationContext ctx = new ApplicationContext(registry, modules);
            modules.forEach(m -> m.useContext(ctx));
            return ctx;
        }
    }

}
