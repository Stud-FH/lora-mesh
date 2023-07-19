package simulation;

import javax.swing.*;
import java.awt.*;

public class GUI extends JFrame {

    public GUI() {
        super("Lora Mesh OpenFlow Simulator");
        setPreferredSize(new Dimension(1200, 800));
        setLayout(new BorderLayout());
        add(new GraphPanel(this), BorderLayout.CENTER);
        add(new ControlPanel(this), BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }
}
