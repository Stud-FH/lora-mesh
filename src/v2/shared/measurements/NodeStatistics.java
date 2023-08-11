package v2.shared.measurements;

import v2.core.common.Observer;
import v2.core.context.Context;
import v2.core.context.Module;
import v2.core.domain.message.Message;
import v2.core.domain.message.MessageType;
import v2.core.domain.node.Node;
import v2.core.domain.node.NodeStatus;

public class NodeStatistics implements Module {
    private ExecutorInsights executorInsights;

    private Observer.Ref statusRef;
    private Long setupTimestamp;
    private int emptyTriggers = 0;
    private int helloTriggers = 0;
    private int routingTriggers = 0;
    private int otherTriggers = 0;


    @Override
    public void build(Context ctx) {
        var node = ctx.resolve(Node.class);
        executorInsights = ctx.resolve(ExecutorInsights.class);
        var loraInsights = ctx.resolve(LoraMeshModuleInsights.class);
        var pceInsights = ctx.resolve(PceModuleInsights.class);

        statusRef = node.status().subscribe(status -> {
            if (status == NodeStatus.Controller || status == NodeStatus.Node) {
                setupTimestamp = executorInsights.taskCounter();
                statusRef.unsubscribe();
            }
        });

        loraInsights.triggered().subscribe(this::countMessage);
    }

    private void countMessage(Message message) {
        if (message == null) {
            emptyTriggers++;
        } else if (MessageType.Hello.matches(message)) {
            helloTriggers++;
        } else if (MessageType.Routing.matches(message)) {
            routingTriggers++;
        } else {
            otherTriggers++;
        }
    }
}
