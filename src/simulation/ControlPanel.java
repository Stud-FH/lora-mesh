package simulation;

import model.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class ControlPanel extends JPanel {

    private final GUI parent;
    private final Simulation simulation = Simulation.INSTANCE;

    public ControlPanel(GUI parent) {
        this.parent = parent;
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
            try {
                File file = new File(Simulation.SAVE_PATH);
                boolean ignored = file.getParentFile().mkdirs();
                FileOutputStream fs = new FileOutputStream(file);
                ObjectOutputStream os = new ObjectOutputStream(fs);
                os.writeObject(Simulation.INSTANCE);
                fs.close();
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        panel.add(save);

        var delete = new JButton("un-save");
        delete.addActionListener(a -> {
            try {
                File file = new File(Simulation.SAVE_PATH);
                boolean ignored = file.delete();
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

        var pauseButton = new JButton(simulation.pause? "run" : "pause");
        pauseButton.addActionListener(a -> {
            simulation.pause = !simulation.pause;
            pauseButton.setText(simulation.pause? "run" : "pause");
        });
        panel.add(pauseButton);

        var timeSlider = new JSlider(1, 100, simulation.timeFactor);
        timeSlider.addChangeListener(c -> {
            simulation.timeFactor = timeSlider.getValue();
            for (SimulatedLoRaMeshClient s : simulation.all) s.refresh();
        });
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
        nameIn.addActionListener(a -> simulation.getSelected().name = nameIn.getText());
        panel.add(nameIn);

        panel.add(grid);

        JButton logLevel = new JButton("log level");
        logLevel.addActionListener(a -> {
            SimulatedLoRaMeshClient simT = simulation.getSelected();
            Logger.Severity[] severities = Logger.Severity.values();
            simT.logLevel = severities[(simT.logLevel.ordinal() + 1) % severities.length];
            logLevel.setText("log: " + simT.logLevel);
        });
        grid.add(logLevel);

        JButton kill = new JButton("kill");
        kill.addActionListener(a -> {
            simulation.getSelected().node.error("killed");
        });
        grid.add(kill);

        JButton promote = new JButton("toggle api");
        promote.addActionListener(a -> {
            simulation.getSelected().setController(!simulation.getSelected().isController());
        });
        grid.add(promote);

        JButton sendData = new JButton("send data");
        sendData.addActionListener(a -> {
            String input = JOptionPane.showInputDialog(
                    parent,
                    "Please enter the data to send:",
                    "Data Input",
                    JOptionPane.PLAIN_MESSAGE);
            if (input != null) simulation.getSelected().node.feedData(input.getBytes(StandardCharsets.UTF_8));
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
                case 1: openLinkDialog(simulation.getSelected(), simulation.all.get(table.getSelectedRow()));
                break;
                case 2: openLinkDialog(simulation.all.get(table.getSelectedRow()), simulation.getSelected());
                break;
            }
        });

        JScrollPane scroller = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setPreferredSize(new Dimension(200, Integer.MAX_VALUE));
        panel.add(scroller);

        Consumer<SimulatedLoRaMeshClient> callback = simT -> {
            header.setText(simT == null? "no node selected" : "modify node: ");
            nameIn.setText(simT == null? "-" : simT.name);
            nameIn.setEnabled(simT != null);
            logLevel.setText(simT == null? "log level" : "log: " + simT.logLevel);
            logLevel.setEnabled(simT != null);
            DefaultTableModel newModel = new DefaultTableModel(calculateTableData(), new String[]{"node", "send", "receive"});
            table.setModel(newModel);
            kill.setEnabled(simT != null);
            promote.setEnabled(simT != null);
            sendData.setEnabled(simT != null);
        };
        callback.accept(simulation.getSelected());
        simulation.addSelectionListener(callback);

        return panel;
    }

    private void openLinkDialog(SimulatedLoRaMeshClient source, SimulatedLoRaMeshClient target) {
        if (source == target) return;
        String def = String.format("%.2f (default)", target.naturalReception(source));
        Object[] possibilities = {def, "1.0", "0.9", "0.7", "0.5", "0.3", "0.1", "0.0"};
        String input = (String)JOptionPane.showInputDialog(
                parent,
                String.format("current reliability: %.2f)", target.reception(source)),
                String.format("Transmission Reliability %s - %s", source.name, target.name),
                JOptionPane.PLAIN_MESSAGE,
                null,
                possibilities,
                null);
        if (input != null) {
            if (input.equals(def)) target.reception.remove(source);
            else target.reception.put(source, Double.parseDouble(input));
        }
    }

    private String[][] calculateTableData() {
        SimulatedLoRaMeshClient simT = simulation.getSelected();
        if (simT == null) return new String[0][3];

        String[][] data = new String[simulation.all.size()][3];
        int i = 0;
        for (SimulatedLoRaMeshClient other : simulation.all) {
            String send = other == simT? "-" : String.format("%.2f", other.reception(simT));
            String receive = other == simT? "-" : String.format("%.2f", simT.reception(other));
            data[i++] = new String[] {other.name, send, receive};
        }
        return data;
    }
}
