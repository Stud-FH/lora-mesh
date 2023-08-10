package model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Context {
    private final Map<Class<? extends Module>, Module> registry;
    private final Collection<Module> modules;
    private boolean ready = false;

    private Context(Map<Class<? extends Module>, Module> registry, Collection<Module> modules) {
        this.registry = registry;
        this.modules = modules;
    }

    public Context deploy() {
        modules.forEach(Module::deploy);
        ready = true;
        return this;
    }

    public void destroy() {
        if (!ready) return;
        ready = false;
        modules.forEach(Module::destroy);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T resolve(Class<T> moduleClass) {
        return (T) registry.get(moduleClass);
    }

    public static class Builder {
        private final Map<Class<? extends Module>, Module> registry;
        private final Collection<Module> modules = new ArrayList<>();

        public Builder() {
            this(null);
        }

        public Builder(Context parentContext) {
            registry = parentContext != null? new HashMap<>(parentContext.registry) : new HashMap<>();
        }

        public Context.Builder register(Module m) {
            modules.add(m);
            m.providers().forEach(k -> registry.put(k, m));
            return this;
        }

        public Context build() {
            modules.forEach(m -> m.dependencies().forEach(d -> {
                if (!registry.containsKey(d)) throw new IllegalStateException(String.format("unresolved dependency: %s (required by %s)", d, m.info()));
            }));

            Context ctx = new Context(registry, modules);
            modules.forEach(m -> m.build(ctx));
            return ctx;
        }
    }

}
