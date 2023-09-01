package v2.shared.measurements;

import v2.core.common.Subject;
import v2.core.context.Module;

public interface ExecutorInsights extends Module {
    Subject<Long> step();
}
