package v2.simulation.gui;

import v2.core.context.Context;
import v2.core.context.Module;
import v2.core.log.Logger;
import v2.simulation.Simulation;
import v2.simulation.impl.VirtualTimeExecutor;
import v2.simulation.util.NodeHandle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.function.Consumer;

public class ControlPanel extends JPanel implements Module {

    private GUI parent;
    private VirtualTimeExecutor exec;
    private Simulation simulation;

    @Override
    public void build(Context ctx) {
        simulation = ctx.resolve(Simulation.class);
        parent = ctx.resolve(GUI.class);
        exec = ctx.resolve(VirtualTimeExecutor.class);
    }

    @Override
    public void deploy() {
        setPreferredSize(new Dimension(200, parent.getHeight()));

        add(settingsControl());
        add(timeControl());
        add(viewControl());
        add(nodeControl());
    }

    private JPanel settingsControl() {
        JPanel panel = new JPanel();

        var save = new JButton("save");
        save.addActionListener(a -> {
            File file = Simulation.root.resolve("latest.ser").toFile();
            try {
                FileOutputStream fs = new FileOutputStream(file);
                ObjectOutputStream os = new ObjectOutputStream(fs);
                os.writeObject(simulation);
                fs.close();
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        panel.add(save);

        var delete = new JButton("un-save");
        delete.addActionListener(a -> {
            File file = Simulation.root.resolve("latest.ser").toFile();
            try {
                if (!file.delete()) throw new Exception("could not delete file");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        panel.add(delete);

        return panel;
    }

    private JPanel timeControl() {
        JPanel panel = new JPanel();
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(new JLabel("time control"));

        var pauseButton = new JButton(exec.paused()? "run" : "pause");
        pauseButton.addActionListener(a -> {
            exec.pause(!exec.paused());
            pauseButton.setText(exec.paused()? "run" : "pause");
        });
        panel.add(pauseButton);

        var timeSlider = new JSlider(-3, 5, simulation.timeControl);
        timeSlider.addChangeListener(c -> simulation.timeControl = timeSlider.getValue());
        panel.add(timeSlider);
        return panel;
    }

    private JPanel viewControl() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        panel.add(new JLabel("view"));
        var group = new ButtonGroup();

        JRadioButton plain = new JRadioButton("plain");
        plain.addActionListener(a -> simulation.view = "plain");
        plain.setSelected(true);
        group.add(plain);
        panel.add(plain);

        JRadioButton upwards = new JRadioButton("upwards");
        upwards.addActionListener(a -> simulation.view = "upwards");
        group.add(upwards);
        panel.add(upwards);

        JRadioButton downwards = new JRadioButton("downwards");
        downwards.addActionListener(a -> simulation.view = "downwards");
        group.add(downwards);
        panel.add(downwards);

        return panel;
    }

    private JPanel nodeControl() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(200, 500));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel grid = new JPanel();
        grid.setLayout(new GridLayout(5, 1));
        grid.setPreferredSize(new Dimension(180, 100));


        JLabel header = new JLabel();
        panel.add(header);

        JTextField nameIn = new JTextField("", 10);
        nameIn.setPreferredSize(new Dimension(200, 20));
//        nameIn.addActionListener(a -> simulation.getSelected().label = nameIn.getText());
        panel.add(nameIn);

        panel.add(grid);

        JButton logLevel = new JButton("java.log level");
        logLevel.addActionListener(a -> {
            var simT = simulation.getSelected();
            Logger.Severity[] severities = Logger.Severity.values();
            simT.setLogLevel(severities[(simT.logLevel().ordinal() + 1) % severities.length]);
            logLevel.setText("java.log: " + simT.logLevel());
        });
        grid.add(logLevel);

        JButton kill = new JButton("kill");
        kill.addActionListener(a -> simulation.getSelected().kill());
        grid.add(kill);

        JButton promote = new JButton("toggle api");
        promote.addActionListener(a -> {
            var selected = simulation.getSelected();
            selected.setPceDisabled(!selected.pceDisabled());
            selected.setDataSinkDisabled(!selected.dataSinkDisabled());
        });
        grid.add(promote);

        JButton sendData = new JButton("send data");
        sendData.addActionListener(a -> {
            String input = JOptionPane.showInputDialog(
                    parent,
                    "Please enter the data to send:",
                    "Data Input",
                    JOptionPane.PLAIN_MESSAGE);
            if (input != null) simulation.getSelected().feedData(input.getBytes());
        });
        grid.add(sendData);

        JTable table = new JTable(new String[0][0], new String[] {"node", "send", "receive"});
        table.setPreferredSize(new Dimension(200, 200));
        ListSelectionModel select = table.getSelectionModel();
        select.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        select.addListSelectionListener(e -> {
            if (!simulation.hasSelected()) return;
            switch (table.getSelectedColumn()) {
                case 0: {}
                    break;
                case 1: openLinkDialog(simulation.getSelected(), simulation.get(table.getSelectedRow()));
                break;
                case 2: openLinkDialog(simulation.get(table.getSelectedRow()), simulation.getSelected());
                break;
            }
        });

        JScrollPane scroller = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setPreferredSize(new Dimension(200, Integer.MAX_VALUE));
        panel.add(scroller);

        Consumer<NodeHandle> callback = specs -> {
            header.setText(specs == null? "no node selected" : "modify node: ");
            nameIn.setText(specs == null? "-" : specs.label());
            nameIn.setEnabled(specs != null);
            logLevel.setText(specs == null? "java.log level" : "java.log: " + specs.logLevel());
            logLevel.setEnabled(specs != null);
            DefaultTableModel newModel = new DefaultTableModel(calculateTableData(), new String[]{"node", "send", "receive"});
            table.setModel(newModel);
            kill.setEnabled(specs != null);
            promote.setEnabled(specs != null);
            sendData.setEnabled(specs != null);
        };
        callback.accept(simulation.getSelected());
        simulation.addSelectionListener(callback);

        return panel;
    }

    private void openLinkDialog(NodeHandle source, NodeHandle target) {
        if (source == target) return;
//        String def = String.format("%.2f (default)", target.distanceBasedReception(source));
        String def = "auto";
        Object[] possibilities = {"auto", "1.0", "0.9", "0.7", "0.5", "0.3", "0.1", "0.0"};
        String input = (String)JOptionPane.showInputDialog(
                parent,
                String.format("current reliability: %.2f)", target.reception(source)),
                String.format("Transmission Reliability %s - %s", source.label(), target.label()),
                JOptionPane.PLAIN_MESSAGE,
                null,
                possibilities,
                null);
        if (input != null) {
            if (input.equals(def)) target.resetReception(source);
            else target.setReception(source, Double.parseDouble(input));
        }
    }

    private String[][] calculateTableData() {
        var selected = simulation.getSelected();
        if (selected == null) return new String[0][3];

        String[][] data = new String[simulation.all().size()][3];
        int i = 0;
        for (var other : simulation.all()) {
            String send = other == selected? "-" : String.format("%.2f", other.reception(selected));
            String receive = other == selected? "-" : String.format("%.2f", selected.reception(other));
            data[i++] = new String[] {other.label(), send, receive};
        }
        return data;
    }
}
