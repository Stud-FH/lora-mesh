package simulation;

import model.message.MessageHeader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class GraphPanel extends JPanel {

    private static final Color CONTROLLER = new Color(255, 0, 255, 100);
    private static final Color CONTROLLER_H = new Color(255, 0, 255, 255);
    private static final Color NODE = new Color(0, 255, 0, 100);
    private static final Color NODE_H = new Color(0, 255, 0, 255);
    private static final Color JOINING = new Color(0, 160, 255, 100);
    private static final Color JOINING_H = new Color(0, 160, 255, 255);
    private static final Color SEEKING = new Color(255, 160, 0, 100);
    private static final Color SEEKING_H = new Color(255, 160, 0, 255);
    private static final Color DOWN = new Color(160, 160, 160, 100);
    private static final Color DOWN_H = new Color(160, 160, 160, 255);
    private static final Color ERROR = new Color(255, 0, 0, 100);
    private static final Color ERROR_H = new Color(255, 0, 0, 255);

    final GUI parent;

    final Simulation simulation = Simulation.INSTANCE;

    SimulatedTransmitter highlighted;

    int offsetX = Integer.MAX_VALUE, offsetY = Integer.MAX_VALUE;
    double zoom = 100;
    int xLast = Integer.MAX_VALUE, yLast = Integer.MAX_VALUE;


    public GraphPanel(GUI parent) {
        super();
        this.parent = parent;
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mouseClicked(e);

                if (!SwingUtilities.isLeftMouseButton(e)) return;

                SimulatedTransmitter closest = closest(e);
                if (screenDistance(closest, e) <= 10) {
                    if (simulation.hasSelected() && e.isShiftDown()) {
                        String def = String.format("%.2f (default)", simulation.getSelected().naturalReception(closest));
                        Object[] possibilities = {def, "1.0", "0.9", "0.8", "0.7", "0.6", "0.5", "0.4", "0.3", "0.2", "0.1", "0.0"};
                        String s = (String)JOptionPane.showInputDialog(
                                parent,
                                String.format("current reception: %.2f)", simulation.getSelected().reception(closest)),
                                String.format("Transmission Reliability %s - %s", simulation.getSelected().name, closest.name),
                                JOptionPane.PLAIN_MESSAGE,
                                null,
                                possibilities,
                                null);
                        if (s != null) {
                            if (s.equals(def)) simulation.getSelected().reception.remove(closest);
                            else simulation.getSelected().reception.put(closest, Double.parseDouble(s));
                        }

                    } else {
                        simulation.select(highlighted = closest);
                    }
                } else if (simulation.hasSelected()) {
                    if (screenDistance(simulation.getSelected(), e) > 14) {
                        simulation.unselect();
                        highlighted = screenDistance(closest, e) <= 10? closest : null;
                    }
                } else {
                    String label = JOptionPane.showInputDialog(
                            parent,
                            "Please enter a label for the new node:",
                            "Node Label",
                            JOptionPane.PLAIN_MESSAGE);

                    if (label != null && !label.isEmpty()) {
                        simulation.all.add(new SimulatedTransmitter(label, xInv(e.getX()), yInv(e.getY())).init(500));
                    }
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseDragged(e);
                xLast = e.getX();
                yLast = e.getY();
                if (simulation.hasSelected()) return;
                SimulatedTransmitter closest = closest(e);
                if (closest == null) return;
                highlighted = screenDistance(closest, e) <= 10? closest : null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                super.mouseDragged(e);
                if (SwingUtilities.isRightMouseButton(e) && xLast != Integer.MAX_VALUE && yLast != Integer.MAX_VALUE) {
                    offsetX += e.getX() - xLast;
                    offsetY += e.getY() - yLast;
                } else if (simulation.hasSelected()) {
                    simulation.getSelected().x = xInv(e.getX());
                    simulation.getSelected().y = yInv(e.getY());
                }
                xLast = e.getX();
                yLast = e.getY();
            }
        });

        addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                super.mouseWheelMoved(e);
                zoom *= Math.pow(0.9, e.getPreciseWheelRotation());
            }
        });

        parent.exec.scheduleAtFixedRate(this::repaint, 200, 15, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (offsetX == Integer.MAX_VALUE || offsetY == Integer.MAX_VALUE) {
            offsetX = getWidth() / 2;
            offsetY = getHeight() / 2;
        }

        long now = System.currentTimeMillis();

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(Color.GRAY);
        for (int i = (int) xInv(offsetX - getHeight()); i < xInv(offsetX + getHeight()); i++) {
            g2.drawLine(x(i), 0, x(i), getHeight());
        }
        for (int i = (int) yInv(offsetY - getWidth()); i < yInv(offsetY + getWidth()); i++) {
            g2.drawLine(0, y(i), getWidth(), y(i));
        }

        for (SimulatedTransmitter s : simulation.all) {
            g2.setColor(new Color(200, 200, 200));
            if (simulation.view.equals("upwards")) {
                for (SimulatedTransmitter other : simulation.all) if (s.node.getRoutingRegistry().contains(other.node.getId())) {
                    g2.drawLine(x(s), y(s), x(other), y(other));
                }
            } else if (simulation.view.equals("downwards")) {
                for (SimulatedTransmitter other : simulation.all)
                    if (s.node.getRoutingRegistry().contains((byte) (other.node.getId() ^ (MessageHeader.DOWNWARDS_BIT >>> MessageHeader.ADDRESS_SHIFT)))) {
                    g2.drawLine(x(s), y(s), x(other), y(other));
                }
            }

            int mr = (int) Math.sqrt((now - s.lastSent) * 4);
            int nr = s == simulation.getSelected()? 14 : 10;
            // message pulse
            g2.setColor(new Color(255, 255, 255, Math.max(50-mr, 0)));
            g2.fillOval(x(s) - mr, y(s) - mr, 2*mr+1, 2*mr+1);
            // hello pulse
            g2.setColor(new Color(255, 255, 255, (int) Math.max(s.lastHello + 100L - now, 0)));
            g2.fillOval(x(s) - nr, y(s) - nr, 2*nr+1, 2*nr+1);
            // node status
            g2.setColor(getColor(s));
            g2.fillOval(x(s) - nr, y(s) - nr, 2*nr+1, 2*nr+1);
            // node position
            g2.setColor(Color.BLACK);
            g2.fillOval(x(s) - 5, y(s) - 5, 11, 11);
        }

        g2.dispose();
    }

    private Color getColor(SimulatedTransmitter t) {
        return switch (t.node.getStatus()) {
            case Controller -> t == highlighted? CONTROLLER_H : CONTROLLER;
            case Node -> t == highlighted? NODE_H : NODE;
            case Joining -> t == highlighted? JOINING_H : JOINING;
            case Seeking -> t == highlighted? SEEKING_H : SEEKING;
            case Down -> t == highlighted? DOWN_H : DOWN;
            case Error -> t == highlighted? ERROR_H : ERROR;
        };
    }

    /**
     * model to screen
     */
    private int x(SimulatedTransmitter simT) {
        return (int) (simT.x * zoom + offsetX);
    }

    private int x(double x) {
        return (int) (x * zoom + offsetX);
    }

    /**
     * model to screen
     */
    private int y(SimulatedTransmitter simT) {
        return (int) (simT.y * zoom + offsetY);
    }

    private int y(double y) {
        return (int) (y * zoom + offsetY);
    }

    /**
     * screen to model
     */
    private double xInv(int x) {
        return (x - offsetX) / zoom;
    }

    /**
     * screen to model
     */
    private double yInv(int y) {
        return (y - offsetY) / zoom;
    }

    private double screenDistance(SimulatedTransmitter simT, MouseEvent e) {
        double dx = x(simT) - e.getX();
        double dy = y(simT) - e.getY();
        return Math.sqrt(dx*dx + dy*dy);
    }

    private SimulatedTransmitter closest(MouseEvent e) {
        return simulation.all.stream().min(Comparator.comparingDouble(t -> t.distance(xInv(e.getX()), yInv(e.getY())))).orElse(null);
    }
}
