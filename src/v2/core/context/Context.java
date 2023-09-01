package v2.core.context;

import v2.core.common.BasicObservable;
import v2.core.common.Observable;

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
    private final BasicObservable<String> teardown = new BasicObservable<>();

    private Context(Map<Class<? extends Module>, Module> registry, Collection<Module> modules, Observable<String> parentTeardown) {
        this.registry = registry;
        this.modules = modules;
        if (parentTeardown != null) {
            parentTeardown.subscribe(this::destroy);
        }
    }

    public Status status() {
        return status;
    }

    public Observable<String> teardown() {
        return teardown;
    }

    public Context deploy() {
        status = Status.Deploying;
        modules.forEach(Module::deploy);
        status = Status.Deployed;
        modules.forEach(Module::postDeploy);
        return this;
    }

    public synchronized void destroy(String terminationMessage) {
        if (status.ordinal() >= Status.Destroying.ordinal()) return;
        modules.forEach(Module::preDestroy);
        status = Status.Destroying;
        teardown.next(null);
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
        private final Context parentContext;

        public Builder() {
            this(null);
        }

        public Builder(Context parentContext) {
            this.parentContext = parentContext;
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
            Context ctx = new Context(registry, modules, parentContext == null? null : parentContext.teardown);
            modules.forEach(m -> m.build(ctx));
            return ctx;
        }
    }
}
