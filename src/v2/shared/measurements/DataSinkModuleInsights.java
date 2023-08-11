package v2.shared.measurements;

import v2.core.common.Observable;
import v2.core.context.Module;
import v2.core.domain.message.Message;

public interface DataSinkModuleInsights extends Module {
    Observable<Message> forwarded();
}
