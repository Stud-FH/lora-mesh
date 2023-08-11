package v2.shared.measurements;

import v2.core.common.Observable;
import v2.core.context.Module;
import v2.core.domain.message.Message;

public interface LoraMeshModuleInsights extends Module {
    Observable<Message> triggered();
}
