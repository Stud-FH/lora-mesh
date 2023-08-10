package v2.shared.api;

import v2.core.log.Logger;
import v2.shared.api.domain.LogEntry;
import v2.core.context.Context;
import v2.core.context.Module;
import v2.core.domain.node.Node;
import v2.shared.util.JsonUtil;

import java.util.Arrays;
import java.util.stream.Collectors;

public class HttpLogger implements Logger {

    private Http http;
    private Node node;

    @Override
    public void build(Context ctx) {
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
            http.postResponseVoid(String.format("/log/%d", node.sid()), JsonUtil.logEntry(data));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void exception(Exception e, Module module) {
        warn(e.toString() + "\n" + Arrays.stream(e.getStackTrace()).map(Object::toString).collect(Collectors.joining("\n")), module);
    }

    @Override
    public String info() {
        return "Http Logger";
    }
}
