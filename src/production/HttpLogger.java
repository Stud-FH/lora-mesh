package production;

import local.Node;
import model.Module;
import model.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class HttpLogger implements Logger {

    private Http http;
    private Node node;

    @Override
    public void useContext(ApplicationContext ctx) {
        http = ctx.resolve(Http.class);
        node = ctx.resolve(Node.class);
    }

    @Override
    public void log(Severity severity, String text, Module module) {
        var data = new LogEntry();
        data.severity = severity;
        data.moduleInfo = module.info();
        data.data = String.format("%s:\n%s", module.info(), text).getBytes();
        try {
            http.postResponseVoid(String.format("/log/%d", node.serialId), JsonUtil.logEntry(data));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void exception(Exception e, Module module) {
        warn(e.toString() + "\n" + Arrays.stream(e.getStackTrace()).map(Object::toString).collect(Collectors.joining("\n")), module);
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(Http.class, Node.class);
    }

    @Override
    public String info() {
        return "Http Logger";
    }
}
