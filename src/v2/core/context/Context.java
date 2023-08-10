package v2.core.context;

import java.util.*;

public class Context {

    public enum Status {
        Building,
        Deploying,
        Deployed,
        Destroying,
        Destroyed
    }

    private final Map<Class<? extends Module>, Module> registry;
    private final Collection<Module> modules;
    private Status status = Status.Building;

    private Context(Map<Class<? extends Module>, Module> registry, Collection<Module> modules) {
        this.registry = registry;
        this.modules = modules;
    }

    public Status status() {
        return status;
    }

    public Context deploy() {
        status = Status.Deploying;
        modules.forEach(Module::deploy);
        status = Status.Deployed;
        modules.forEach(Module::postDeploy);
        return this;
    }

    public synchronized void destroy() {
        if (status.ordinal() >= Status.Destroying.ordinal()) return;
        modules.forEach(Module::preDestroy);
        status = Status.Destroying;
        modules.forEach(Module::destroy);
        status = Status.Destroyed;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T resolve(Class<T> moduleClass) {
        if (!registry.containsKey(moduleClass)) {
            throw new IllegalStateException(String.format("unresolved dependency: %s", moduleClass));
        }
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

        @SuppressWarnings("unchecked")
        public Context.Builder register(Module m) {
            modules.add(m);
//            var c = m.getClass();
//            var in = c.getInterfaces();
//            for (var x : in) {
//                if (Module.class.isAssignableFrom())
//            }
            Arrays.stream(m.getClass().getInterfaces()).filter(Module.class::isAssignableFrom)
                    .forEach(k ->
                            registry.put((Class<? extends Module>) k, m));
            registry.put(m.getClass(), m);
            return this;
        }

        public Context build() {
            Context ctx = new Context(registry, modules);
            modules.forEach(m -> m.build(ctx));
            return ctx;
        }
    }
}
