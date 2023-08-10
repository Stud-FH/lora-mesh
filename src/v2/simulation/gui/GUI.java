package v2.simulation.gui;

import v2.core.context.Context;
import v2.core.context.Module;

import javax.swing.*;
import java.awt.*;

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
}
