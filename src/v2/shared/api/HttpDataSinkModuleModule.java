package v2.shared.api;

import v2.core.common.BasicObservable;
import v2.core.common.Observable;
import v2.core.context.Context;
import v2.core.domain.DataSinkModule;
import v2.core.domain.message.Message;
import v2.shared.measurements.DataSinkModuleInsights;
import v2.shared.util.JsonUtil;

import java.util.Collection;

public class HttpDataSinkModuleModule implements DataSinkModule, DataSinkModuleInsights {

    private final BasicObservable<Message> forwarded = new BasicObservable<>();

    private Http http;

    @Override
    public void build(Context ctx) {
        http = ctx.resolve(Http.class);
    }

    @Override
    public boolean heartbeat() {
        var response = http.getResponseStringDisableLogging("/data");
        return "true".equals(response);
    }

    @Override
    public Collection<Integer> feed(Message message) {
        forwarded.next(message);
        var response = http.postResponseString("/data", JsonUtil.message(message));
        return JsonUtil.parseIntList(response);
    }

    @Override
    public Observable<Message> forwarded() {
        return forwarded;
    }

}
