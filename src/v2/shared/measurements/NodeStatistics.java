package v2.shared.measurements;

import v2.core.common.*;
import v2.core.context.Context;
import v2.core.context.Module;
import v2.core.domain.message.Message;
import v2.core.domain.message.MessageType;
import v2.core.domain.node.Node;
import v2.core.domain.node.NodeStatus;

public class NodeStatistics implements Module {

    private long nodeId;
    private Subject<Long> step;

    private Observer.Ref statusRef;
    private Long setupTimestamp;
    private int emptyTriggers = 0;
    private int dataTriggers = 0;
    private int helloTriggers = 0;
    private int routingTriggers = 0;
    private int otherTriggers = 0;


    @Override
    public void build(Context ctx) {
        ctx.resolve(ResultsCollector.class).register(this);
        var node = ctx.resolve(Node.class);
        var executorInsights = ctx.resolve(ExecutorInsights.class);
        var loraInsights = ctx.resolve(LoraMeshModuleInsights.class);
//        var pceInsights = ctx.resolve(PceModuleInsights.class);

        nodeId = node.id();
        step = executorInsights.step();

        statusRef = node.status().subscribe(status -> {
            if (status == NodeStatus.Controller || status == NodeStatus.Node) {
                setupTimestamp = step.value();
                statusRef.unsubscribe();
            }
        });

        loraInsights.triggered().subscribe(this::countMessage);
    }

    private void countMessage(Message message) {
        if (message == null) {
            emptyTriggers++;
        } else if (MessageType.Data.matches(message)) {
            dataTriggers++;
        } else if (MessageType.Hello.matches(message)) {
            helloTriggers++;
        } else if (MessageType.Routing.matches(message)) {
            routingTriggers++;
        } else {
            otherTriggers++;
        }
    }

    public long getNodeId() {
        return nodeId;
    }

    public Long getSetupTimestamp() {
        return setupTimestamp;
    }

    public int getEmptyTriggers() {
        return emptyTriggers;
    }

    public int getDataTriggers() {
        return dataTriggers;
    }

    public int getHelloTriggers() {
        return helloTriggers;
    }

    public int getRoutingTriggers() {
        return routingTriggers;
    }

    public int getOtherTriggers() {
        return otherTriggers;
    }
}
