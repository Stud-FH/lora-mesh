package v2.shared.measurements;

import v2.core.context.Context;
import v2.core.context.Module;
import v2.shared.integration.FileClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class ResultsCollector implements Module {
    private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");

    private FileClient fs;
    private final Collection<NodeStatistics> sources = new ArrayList<>();

    @Override
    public void build(Context ctx) {
        fs = ctx.resolve(FileClient.class);
    }

    public void register(NodeStatistics source) {
        sources.add(source);
    }

    @Override
    public void preDestroy() {
        StringBuilder sb = new StringBuilder("node, setup_time, t_empty, t_data, t_hello, t_routing, t_other\n");
        sources.forEach(s -> {
            var setupTime = s.getSetupTimestamp();
            String setup = setupTime == null? "-" : setupTime.toString();
            sb.append(String.format("%d, %s, %d, %d, %d, %d, %d\n", s.getNodeId(), setup, s.getEmptyTriggers(), s.getDataTriggers(), s.getHelloTriggers(), s.getRoutingTriggers(), s.getOtherTriggers()));
        });

        fs.write(String.format("simulation_results_%s.csv", df.format(new Date())), sb.toString());
    }
}
