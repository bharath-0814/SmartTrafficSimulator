package com.traffic.ui;

import com.traffic.engine.SimulationEngine;
import com.traffic.engine.SimulationSnapshot;
import com.traffic.model.Direction;
import com.traffic.model.SignalColor;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.List;

/**
 * Custom 2D Swing canvas that renders the 4-way traffic intersection from a top-down view.
 *
 * <p>Thread Safety:
 * Renders strictly from immutable {@link SimulationSnapshot} objects.
 * Never holds locks or mutates domain objects directly.
 *
 * <p>Renders:
 * <ul>
 *   <li>The crossroads layout with North, East, South, and West approaching roads.</li>
 *   <li>Asphalt roads, yellow center lane dividers, and white stop lines.</li>
 *   <li>Traffic light housings with glowing RED, YELLOW, and GREEN signal indicators and timers.</li>
 *   <li>Vehicles queued on each approach with distinct visual representations:
 *       Cars (Blue), Buses (Orange), Bikes (Purple), and Ambulances (White with Red Cross).</li>
 *   <li>Live status cues such as Emergency Override indicator banners.</li>
 * </ul>
 */
public class IntersectionPanel extends JPanel {

    // Palette Colors
    private static final Color COLOR_GRASS = new Color(30, 34, 42);
    private static final Color COLOR_ROAD = new Color(52, 58, 68);
    private static final Color COLOR_INTERSECTION = new Color(60, 66, 78);
    private static final Color COLOR_DIVIDER = new Color(241, 196, 15);
    private static final Color COLOR_STOP_LINE = new Color(236, 240, 241);
    private static final Color COLOR_LIGHT_HOUSING = new Color(20, 22, 26);
    private static final Color COLOR_LIGHT_BORDER = new Color(80, 88, 100);

    // Signal Bulb Colors (Active vs Dimmed)
    private static final Color COLOR_RED_ON = new Color(231, 76, 60);
    private static final Color COLOR_RED_OFF = new Color(70, 25, 25);
    private static final Color COLOR_YELLOW_ON = new Color(241, 196, 15);
    private static final Color COLOR_YELLOW_OFF = new Color(75, 65, 20);
    private static final Color COLOR_GREEN_ON = new Color(46, 204, 113);
    private static final Color COLOR_GREEN_OFF = new Color(20, 65, 35);

    // Vehicle Colors
    private static final Color COLOR_CAR = new Color(41, 128, 185);
    private static final Color COLOR_BUS = new Color(211, 84, 0);
    private static final Color COLOR_BIKE = new Color(142, 68, 173);
    private static final Color COLOR_AMBULANCE = Color.WHITE;
    private static final Color COLOR_RED_CROSS = new Color(192, 57, 43);

    private final SimulationEngine engine;
    private volatile SimulationSnapshot currentSnapshot;

    /**
     * Constructs the visualization canvas with a reference to the simulation engine.
     *
     * @param engine The underlying simulation engine providing traffic state.
     */
    public IntersectionPanel(SimulationEngine engine) {
        if (engine == null) {
            throw new IllegalArgumentException("Simulation engine cannot be null.");
        }
        this.engine = engine;
        setBackground(COLOR_GRASS);
        setPreferredSize(new Dimension(600, 600));
        setMinimumSize(new Dimension(400, 400));
    }

    /**
     * Updates the local immutable snapshot used for painting.
     */
    public void setSnapshot(SimulationSnapshot snapshot) {
        this.currentSnapshot = snapshot;
    }

    public SimulationSnapshot getSnapshot() {
        return currentSnapshot;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        // Enable high quality anti-aliasing for smooth geometry and typography
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int centerX = width / 2;
        int centerY = height / 2;
        int roadWidth = Math.min(width, height) / 4;
        if (roadWidth < 100) {
            roadWidth = 100;
        }

        SimulationSnapshot snapshot = this.currentSnapshot;
        if (snapshot == null) {
            snapshot = engine.getSnapshot();
            this.currentSnapshot = snapshot;
        }

        // 1. Draw Crossroads & Road Geometry
        drawRoads(g2d, width, height, centerX, centerY, roadWidth);

        // 2. Draw Lane Markings & Stop Lines
        drawLaneMarkings(g2d, width, height, centerX, centerY, roadWidth);

        // 3. Draw Traffic Light Housings & Active Indicators
        drawTrafficSignals(g2d, snapshot, centerX, centerY, roadWidth);

        // 4. Draw Vehicles Queued on Approaches
        drawVehicles(g2d, snapshot, centerX, centerY, roadWidth);

        // 5. Draw Road Direction Banners & Compass
        drawBanners(g2d, snapshot, width, height, centerX, centerY, roadWidth);
    }

    /**
     * Draws the asphalt roads forming the four-way crossroads.
     */
    private void drawRoads(Graphics2D g2d, int width, int height, int centerX, int centerY, int roadWidth) {
        g2d.setColor(COLOR_ROAD);

        // Vertical Road (North - South)
        g2d.fillRect(centerX - roadWidth / 2, 0, roadWidth, height);

        // Horizontal Road (West - East)
        g2d.fillRect(0, centerY - roadWidth / 2, width, roadWidth);

        // Center Junction Box
        g2d.setColor(COLOR_INTERSECTION);
        g2d.fillRect(centerX - roadWidth / 2, centerY - roadWidth / 2, roadWidth, roadWidth);

        // Subtle outline around center junction box
        g2d.setColor(new Color(80, 88, 100, 100));
        g2d.drawRect(centerX - roadWidth / 2, centerY - roadWidth / 2, roadWidth, roadWidth);
    }

    /**
     * Draws the yellow dashed center dividing lines and solid white stop lines.
     */
    private void drawLaneMarkings(Graphics2D g2d, int width, int height, int centerX, int centerY, int roadWidth) {
        Stroke normalStroke = g2d.getStroke();

        // Dashed yellow center lines
        Stroke dashed = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{8, 8}, 0);
        g2d.setStroke(dashed);
        g2d.setColor(COLOR_DIVIDER);

        // North Center Line
        g2d.drawLine(centerX, 0, centerX, centerY - roadWidth / 2);
        // South Center Line
        g2d.drawLine(centerX, centerY + roadWidth / 2, centerX, height);
        // West Center Line
        g2d.drawLine(0, centerY, centerX - roadWidth / 2, centerY);
        // East Center Line
        g2d.drawLine(centerX + roadWidth / 2, centerY, width, centerY);

        // Solid White Stop Lines
        g2d.setStroke(new BasicStroke(3));
        g2d.setColor(COLOR_STOP_LINE);

        // North Stop Line (across left incoming lane)
        g2d.drawLine(centerX - roadWidth / 2, centerY - roadWidth / 2, centerX, centerY - roadWidth / 2);
        // South Stop Line (across right incoming lane)
        g2d.drawLine(centerX, centerY + roadWidth / 2, centerX + roadWidth / 2, centerY + roadWidth / 2);
        // West Stop Line (across bottom incoming lane)
        g2d.drawLine(centerX - roadWidth / 2, centerY, centerX - roadWidth / 2, centerY + roadWidth / 2);
        // East Stop Line (across top incoming lane)
        g2d.drawLine(centerX + roadWidth / 2, centerY - roadWidth / 2, centerX + roadWidth / 2, centerY);

        g2d.setStroke(normalStroke);
    }

    /**
     * Draws the 4 traffic signal light boxes at each intersection corner.
     */
    private void drawTrafficSignals(Graphics2D g2d, SimulationSnapshot snapshot, int centerX, int centerY, int roadWidth) {
        int boxW = 30;
        int boxH = 68;

        // North Approach Signal (mounted at North-East corner facing incoming South-bound traffic)
        int northX = centerX + roadWidth / 2 + 8;
        int northY = centerY - roadWidth / 2 - boxH - 4;
        drawSignalBox(g2d, snapshot.getRoad(Direction.NORTH), northX, northY, boxW, boxH);

        // East Approach Signal (mounted at South-East corner facing incoming West-bound traffic)
        int eastX = centerX + roadWidth / 2 + 8;
        int eastY = centerY + roadWidth / 2 + 8;
        drawSignalBox(g2d, snapshot.getRoad(Direction.EAST), eastX, eastY, boxW, boxH);

        // South Approach Signal (mounted at South-West corner facing incoming North-bound traffic)
        int southX = centerX - roadWidth / 2 - boxW - 8;
        int southY = centerY + roadWidth / 2 + 8;
        drawSignalBox(g2d, snapshot.getRoad(Direction.SOUTH), southX, southY, boxW, boxH);

        // West Approach Signal (mounted at North-West corner facing incoming East-bound traffic)
        int westX = centerX - roadWidth / 2 - boxW - 8;
        int westY = centerY - roadWidth / 2 - boxH - 4;
        drawSignalBox(g2d, snapshot.getRoad(Direction.WEST), westX, westY, boxW, boxH);
    }

    /**
     * Renders a single vertical traffic signal box with RED, YELLOW, GREEN bulbs and timer.
     */
    private void drawSignalBox(Graphics2D g2d, SimulationSnapshot.RoadSnapshot road, int x, int y, int w, int h) {
        if (road == null) {
            return;
        }

        // Outer housing
        g2d.setColor(COLOR_LIGHT_HOUSING);
        g2d.fillRoundRect(x, y, w, h, 10, 10);
        g2d.setColor(COLOR_LIGHT_BORDER);
        g2d.drawRoundRect(x, y, w, h, 10, 10);

        int bulbDiameter = 14;
        int bulbX = x + (w - bulbDiameter) / 2;
        int redY = y + 6;
        int yellowY = y + 26;
        int greenY = y + 46;

        SignalColor color = road.getSignalColor();

        // RED Bulb
        if (color == SignalColor.RED) {
            g2d.setColor(new Color(231, 76, 60, 80));
            g2d.fillOval(bulbX - 2, redY - 2, bulbDiameter + 4, bulbDiameter + 4); // Glow
            g2d.setColor(COLOR_RED_ON);
        } else {
            g2d.setColor(COLOR_RED_OFF);
        }
        g2d.fillOval(bulbX, redY, bulbDiameter, bulbDiameter);

        // YELLOW Bulb
        if (color == SignalColor.YELLOW) {
            g2d.setColor(new Color(241, 196, 15, 80));
            g2d.fillOval(bulbX - 2, yellowY - 2, bulbDiameter + 4, bulbDiameter + 4); // Glow
            g2d.setColor(COLOR_YELLOW_ON);
        } else {
            g2d.setColor(COLOR_YELLOW_OFF);
        }
        g2d.fillOval(bulbX, yellowY, bulbDiameter, bulbDiameter);

        // GREEN Bulb
        if (color == SignalColor.GREEN) {
            g2d.setColor(new Color(46, 204, 113, 80));
            g2d.fillOval(bulbX - 2, greenY - 2, bulbDiameter + 4, bulbDiameter + 4); // Glow
            g2d.setColor(COLOR_GREEN_ON);
        } else {
            g2d.setColor(COLOR_GREEN_OFF);
        }
        g2d.fillOval(bulbX, greenY, bulbDiameter, bulbDiameter);

        // Draw timer text below housing if active
        if (color != SignalColor.RED) {
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
            String timerStr = road.getSignalTimer() + "s";
            FontMetrics fm = g2d.getFontMetrics();
            int tx = x + (w - fm.stringWidth(timerStr)) / 2;
            g2d.drawString(timerStr, tx, y + h + 12);
        }
    }

    /**
     * Draws vehicles queued on each of the 4 approaches.
     */
    private void drawVehicles(Graphics2D g2d, SimulationSnapshot snapshot, int centerX, int centerY, int roadWidth) {
        int laneOffset = roadWidth / 4;
        int vehicleSpacing = 42;

        // 1. North Approach (Vehicles heading SOUTH, queued in left lane: centerX - laneOffset)
        SimulationSnapshot.RoadSnapshot north = snapshot.getRoad(Direction.NORTH);
        if (north != null) {
            List<SimulationSnapshot.VehicleSnapshot> northVehicles = north.getVehicles();
            int northStopY = centerY - roadWidth / 2 - 32;
            for (int i = 0; i < northVehicles.size() && i < 6; i++) {
                SimulationSnapshot.VehicleSnapshot v = northVehicles.get(i);
                int vy = northStopY - (i * vehicleSpacing);
                drawVehicleBody(g2d, v, centerX - laneOffset, vy, 22, 34, true);
            }
        }

        // 2. South Approach (Vehicles heading NORTH, queued in right lane: centerX + laneOffset)
        SimulationSnapshot.RoadSnapshot south = snapshot.getRoad(Direction.SOUTH);
        if (south != null) {
            List<SimulationSnapshot.VehicleSnapshot> southVehicles = south.getVehicles();
            int southStopY = centerY + roadWidth / 2 + 10;
            for (int i = 0; i < southVehicles.size() && i < 6; i++) {
                SimulationSnapshot.VehicleSnapshot v = southVehicles.get(i);
                int vy = southStopY + (i * vehicleSpacing);
                drawVehicleBody(g2d, v, centerX + laneOffset, vy, 22, 34, true);
            }
        }

        // 3. West Approach (Vehicles heading EAST, queued in bottom lane: centerY + laneOffset)
        SimulationSnapshot.RoadSnapshot west = snapshot.getRoad(Direction.WEST);
        if (west != null) {
            List<SimulationSnapshot.VehicleSnapshot> westVehicles = west.getVehicles();
            int westStopX = centerX - roadWidth / 2 - 32;
            for (int i = 0; i < westVehicles.size() && i < 6; i++) {
                SimulationSnapshot.VehicleSnapshot v = westVehicles.get(i);
                int vx = westStopX - (i * vehicleSpacing);
                drawVehicleBody(g2d, v, vx, centerY + laneOffset, 34, 22, false);
            }
        }

        // 4. East Approach (Vehicles heading WEST, queued in top lane: centerY - laneOffset)
        SimulationSnapshot.RoadSnapshot east = snapshot.getRoad(Direction.EAST);
        if (east != null) {
            List<SimulationSnapshot.VehicleSnapshot> eastVehicles = east.getVehicles();
            int eastStopX = centerX + roadWidth / 2 + 10;
            for (int i = 0; i < eastVehicles.size() && i < 6; i++) {
                SimulationSnapshot.VehicleSnapshot v = eastVehicles.get(i);
                int vx = eastStopX + (i * vehicleSpacing);
                drawVehicleBody(g2d, v, vx, centerY - laneOffset, 34, 22, false);
            }
        }
    }

    /**
     * Renders an individual vehicle box with type-specific color and markings.
     */
    private void drawVehicleBody(Graphics2D g2d, SimulationSnapshot.VehicleSnapshot v, int x, int y, int w, int h, boolean vertical) {
        int drawX = x - (w / 2);
        int drawY = y - (h / 2);

        // Determine color based on vehicle type
        Color bodyColor = COLOR_CAR;
        if (v.isAmbulance()) {
            bodyColor = COLOR_AMBULANCE;
        } else if (v.isBus()) {
            bodyColor = COLOR_BUS;
        } else if (v.isBike()) {
            bodyColor = COLOR_BIKE;
        }

        // Fill vehicle body
        g2d.setColor(bodyColor);
        g2d.fillRoundRect(drawX, drawY, w, h, 6, 6);

        // Vehicle border
        if (v.isAmbulance()) {
            // Ambulance has bold red emergency border
            g2d.setColor(new Color(231, 76, 60));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(drawX, drawY, w, h, 6, 6);
            g2d.setStroke(new BasicStroke(1));

            // Draw Red Cross (+) in the center
            g2d.setColor(COLOR_RED_CROSS);
            int crossSize = 8;
            int cx = drawX + (w / 2);
            int cy = drawY + (h / 2);
            g2d.fillRect(cx - crossSize / 2, cy - 1, crossSize, 3);
            g2d.fillRect(cx - 1, cy - crossSize / 2, 3, crossSize);

        } else {
            g2d.setColor(new Color(20, 24, 30));
            g2d.drawRoundRect(drawX, drawY, w, h, 6, 6);
        }

        // Draw vehicle ID label
        g2d.setFont(new Font("SansSerif", Font.BOLD, 8));
        String idText = v.getId();
        if (idText.length() > 6) {
            idText = idText.substring(0, 6);
        }

        g2d.setColor(v.isAmbulance() ? Color.BLACK : Color.WHITE);
        FontMetrics fm = g2d.getFontMetrics();
        int tx = drawX + (w - fm.stringWidth(idText)) / 2;
        int ty = drawY + (h + fm.getAscent()) / 2 - 1;
        if (v.isAmbulance()) {
            ty = drawY + h - 2; // Position text below red cross
        }
        g2d.drawString(idText, tx, ty);
    }

    /**
     * Draws road approach banners and informational text.
     */
    private void drawBanners(Graphics2D g2d, SimulationSnapshot snapshot, int width, int height,
                             int centerX, int centerY, int roadWidth) {
        g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
        FontMetrics fm = g2d.getFontMetrics();

        // North Label
        SimulationSnapshot.RoadSnapshot north = snapshot.getRoad(Direction.NORTH);
        int northQueue = (north != null) ? north.getQueueLength() : 0;
        boolean northGreen = (north != null) && north.isGreen();
        String northStr = "NORTH APPROACH (" + northQueue + ")";
        drawBadge(g2d, northStr, centerX - fm.stringWidth(northStr) / 2, 22, northGreen);

        // South Label
        SimulationSnapshot.RoadSnapshot south = snapshot.getRoad(Direction.SOUTH);
        int southQueue = (south != null) ? south.getQueueLength() : 0;
        boolean southGreen = (south != null) && south.isGreen();
        String southStr = "SOUTH APPROACH (" + southQueue + ")";
        drawBadge(g2d, southStr, centerX - fm.stringWidth(southStr) / 2, height - 12, southGreen);

        // West Label
        SimulationSnapshot.RoadSnapshot west = snapshot.getRoad(Direction.WEST);
        int westQueue = (west != null) ? west.getQueueLength() : 0;
        boolean westGreen = (west != null) && west.isGreen();
        String westStr = "WEST (" + westQueue + ")";
        drawBadge(g2d, westStr, 15, centerY + 5, westGreen);

        // East Label
        SimulationSnapshot.RoadSnapshot east = snapshot.getRoad(Direction.EAST);
        int eastQueue = (east != null) ? east.getQueueLength() : 0;
        boolean eastGreen = (east != null) && east.isGreen();
        String eastStr = "EAST (" + eastQueue + ")";
        drawBadge(g2d, eastStr, width - fm.stringWidth(eastStr) - 25, centerY + 5, eastGreen);
    }

    private void drawBadge(Graphics2D g2d, String text, int x, int y, boolean isGreen) {
        FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth(text);
        int textH = fm.getHeight();

        g2d.setColor(isGreen ? new Color(39, 174, 96, 220) : new Color(44, 62, 80, 200));
        g2d.fillRoundRect(x - 6, y - textH + 4, textW + 12, textH + 2, 6, 6);

        g2d.setColor(Color.WHITE);
        g2d.drawString(text, x, y);
    }
}
