package simulation;

import model.Context;
import model.Module;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Set;

public class GUI extends JFrame implements Module {

    GraphPanel graph;
    ControlPanel control;

    public GUI() {
        super("Lora Mesh OpenFlow Simulator");
    }

    @Override
    public void build(Context ctx) {
        graph = ctx.resolve(GraphPanel.class);
        control = ctx.resolve(ControlPanel.class);
    }

    @Override
    public void deploy() {
        setPreferredSize(new Dimension(1200, 800));
        setLayout(new BorderLayout());
        add(graph, BorderLayout.CENTER);
        add(control, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    @Override
    public String info() {
        return "GUI";
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of();
    }
}
