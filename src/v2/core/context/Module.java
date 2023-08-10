package v2.core.context;

public interface Module {
    default void build(Context ctx) {

    }

    default void deploy() {

    }

    default void postDeploy() {

    }

    default void preDestroy() {

    }

    default void destroy() {

    }

    default String info() {
        return getClass().getName();
    }
}
