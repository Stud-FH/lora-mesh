package simulation;

import model.Context;
import model.Executor;
import model.Module;
import model.message.MessageHeader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

public class GraphPanel extends JPanel implements Module {

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

    private GUI parent;
    private Executor exec;

    private final Simulation simulation;

    private NodeHandle highlighted;

    private int offsetX = Integer.MAX_VALUE, offsetY = Integer.MAX_VALUE;
    private double zoom = 100;
    private int xLast = Integer.MAX_VALUE, yLast = Integer.MAX_VALUE;

    public GraphPanel(Simulation simulation) {
        this.simulation = simulation;
    }

    @Override
    public void build(Context ctx) {
        parent = ctx.resolve(GUI.class);
        exec = ctx.resolve(Executor.class);
    }


    @Override
    public void deploy() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                super.mouseClicked(e);

                if (!SwingUtilities.isLeftMouseButton(e)) return;

                var closest = closest(e);
                if (screenDistance(closest, e) <= 10) {
                    if (simulation.hasSelected() && e.isShiftDown()) {
                        String def = String.format("%.2f (default)", simulation.getSelected().distanceBasedReception(closest));
                        Object[] possibilities = {def, "1.0", "0.9", "0.8", "0.7", "0.6", "0.5", "0.4", "0.3", "0.2", "0.1", "0.0"};
                        String s = (String)JOptionPane.showInputDialog(
                                parent,
                                String.format("current reception: %.2f)", simulation.getSelected().reception(closest)),
                                String.format("Transmission Reliability %s - %s", simulation.getSelected().label(), closest.label()),
                                JOptionPane.PLAIN_MESSAGE,
                                null,
                                possibilities,
                                null);
                        if (s != null) {
                            if (s.equals(def)) simulation.getSelected().resetReception(closest);
                            else simulation.getSelected().setReception(closest, Double.parseDouble(s));
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
                        simulation.add(label, xInv(e.getX()), yInv(e.getY()));
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
                var closest = closest(e);
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
                    simulation.getSelected().setX(xInv(e.getX()));
                    simulation.getSelected().setY(yInv(e.getY()));
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

        exec.schedulePeriodic(this::repaint, 15, 200);
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

        for (var node : simulation.all()) {
            g2.setColor(new Color(200, 200, 200));
            if (simulation.view.equals("upwards")) {
                for (var other : simulation.all()) {
                    if (node.getRoutingRegistry().contains(other.getNodeId())) {
                        g2.drawLine(x(node), y(node), x(other), y(other));
                    }
                }
            } else if (simulation.view.equals("downwards")) {
                for (var other : simulation.all())
                    if (node.getRoutingRegistry().contains((byte) (other.getNodeId() ^ (MessageHeader.DOWNWARDS_BIT >>> MessageHeader.ADDRESS_SHIFT)))) {
                    g2.drawLine(x(node), y(node), x(other), y(other));
                }
            }

            int mr = (int) Math.sqrt((now - node.lastSent()) * 4);
            int nr = node == simulation.getSelected()? 14 : 10;
            // message pulse
            g2.setColor(new Color(255, 255, 255, Math.max(50-mr, 0)));
            g2.fillOval(x(node) - mr, y(node) - mr, 2*mr+1, 2*mr+1);
            // hello pulse
            g2.setColor(new Color(255, 255, 255, (int) Math.max(node.lastHello() + 100L - now, 0)));
            g2.fillOval(x(node) - nr, y(node) - nr, 2*nr+1, 2*nr+1);
            // node status
            g2.setColor(getColor(node));
            g2.fillOval(x(node) - nr, y(node) - nr, 2*nr+1, 2*nr+1);
            // node position
            g2.setColor(Color.BLACK);
            g2.fillOval(x(node) - 5, y(node) - 5, 11, 11);
        }

        g2.dispose();
    }

    private Color getColor(NodeHandle node) {
        switch (node.getStatus()) {
            case Controller: return node == highlighted? CONTROLLER_H : CONTROLLER;
            case Node: return node == highlighted? NODE_H : NODE;
            case Joining: return node == highlighted? JOINING_H : JOINING;
            case Seeking: return node == highlighted? SEEKING_H : SEEKING;
            case Down: return node == highlighted? DOWN_H : DOWN;
            case Error: return node == highlighted? ERROR_H : ERROR;
            default: return null;
        }
    }

    /**
     * model to screen
     */
    private int x(NodeHandle node) {
        return (int) (node.x() * zoom + offsetX);
    }

    private int x(double x) {
        return (int) (x * zoom + offsetX);
    }

    /**
     * model to screen
     */
    private int y(NodeHandle node) {
        return (int) (node.y() * zoom + offsetY);
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

    private double screenDistance(NodeHandle specs, MouseEvent e) {
        double dx = x(specs) - e.getX();
        double dy = y(specs) - e.getY();
        return Math.sqrt(dx*dx + dy*dy);
    }

    private NodeHandle closest(MouseEvent e) {
        return simulation.all().stream().min(Comparator.comparingDouble(t -> t.distance(xInv(e.getX()), yInv(e.getY())))).orElse(null);
    }

    @Override
    public Collection<Class<? extends Module>> dependencies() {
        return Set.of(GUI.class, Executor.class);
    }

    @Override
    public String info() {
        return "Graph";
    }
}
