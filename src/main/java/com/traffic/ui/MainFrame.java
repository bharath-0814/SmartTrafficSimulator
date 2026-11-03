package com.traffic.ui;

import com.traffic.engine.SimulationController;
import com.traffic.engine.SimulationEngine;
import com.traffic.engine.SimulationSnapshot;
import com.traffic.model.Ambulance;
import com.traffic.model.Bike;
import com.traffic.model.Bus;
import com.traffic.model.Car;
import com.traffic.model.Direction;
import com.traffic.model.Intersection;
import com.traffic.model.Road;
import com.traffic.model.SignalColor;
import com.traffic.model.TrafficSignal;
import com.traffic.model.Vehicle;
import com.traffic.service.ReportException;
import com.traffic.service.ReportService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

/**
 * Main application window for the Smart Traffic Simulator.
 * Provides a dark-themed, modern traffic management dashboard.
 *
 * <p>Architecture & Responsibilities:
 * <ul>
 *   <li><b>Visualization:</b> Embeds {@link IntersectionPanel} to render top-down 2D graphics.</li>
 *   <li><b>Simulation Control:</b> Dispatches Start, Pause, Resume, Stop, and Step Once
 *       actions to {@link SimulationController}.</li>
 *   <li><b>Live Metrics:</b> Displays active approaches, signal states, served vehicle counters,
 *       and queue lengths refreshed on the Swing EDT.</li>
 *   <li><b>Interactive Vehicle Injection:</b> Allows the user to inject cars, buses, bikes,
 *       or emergency ambulances onto any incoming approach at runtime.</li>
 *   <li><b>Reporting:</b> Integrates with {@link ReportService} to generate and display
 *       simulation audit reports directly in a dialog window.</li>
 *   <li><b>Thread Safety:</b> Guarantees Swing components are updated strictly on the Event
 *       Dispatch Thread (EDT) via a {@link javax.swing.Timer}.</li>
 * </ul>
 */
public class MainFrame extends JFrame {

    // Palette Colors
    private static final Color BG_DARK = new Color(24, 26, 32);
    private static final Color BG_PANEL = new Color(34, 38, 48);
    private static final Color BG_CARD = new Color(44, 49, 60);
    private static final Color TEXT_PRIMARY = new Color(240, 243, 246);
    private static final Color TEXT_MUTED = new Color(160, 168, 180);
    private static final Color ACCENT_GREEN = new Color(46, 204, 113);
    private static final Color ACCENT_RED = new Color(231, 76, 60);
    private static final Color ACCENT_AMBER = new Color(241, 196, 15);
    private static final Color ACCENT_BLUE = new Color(52, 152, 219);
    private static final Color ACCENT_PURPLE = new Color(155, 89, 182);

    private final SimulationController controller;
    private final SimulationEngine engine;
    private final ReportService reportService;

    // UI Components
    private IntersectionPanel intersectionPanel;
    private Timer uiRefreshTimer;

    // Control Buttons
    private JButton btnStart;
    private JButton btnPause;
    private JButton btnResume;
    private JButton btnStop;
    private JButton btnStepOnce;
    private JSlider speedSlider;
    private JLabel lblSpeedValue;

    // Vehicle Injection Controls
    private JComboBox<String> comboApproach;
    private JComboBox<String> comboVehicleType;
    private JButton btnAddVehicle;
    private int customVehicleSeq = 1;

    // Live Statistics Labels
    private JLabel lblStatusBadge;
    private JLabel lblTicksElapsed;
    private JLabel lblActiveApproach;
    private JLabel lblSignalLight;
    private JLabel lblEmergencyOverride;
    private JLabel lblVehiclesServed;
    private JLabel lblEmergencyServed;
    private JLabel lblQueueNorth;
    private JLabel lblQueueEast;
    private JLabel lblQueueSouth;
    private JLabel lblQueueWest;
    private JLabel lblLastEvent;

    // Report Controls
    private JButton btnGenerateReport;
    private JButton btnViewReport;

    /**
     * Constructs the main dashboard window.
     *
     * @param controller The simulation controller governing the background thread.
     */
    public MainFrame(SimulationController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("Simulation controller cannot be null.");
        }
        this.controller = controller;
        this.engine = controller.getEngine();
        this.reportService = new ReportService();

        initWindow();
        initComponents();
        setupEventHandlers();
        startUiRefreshTimer();
    }

    /**
     * Initializes JFrame window properties.
     */
    private void initWindow() {
        setTitle("Smart Traffic Management & Emergency Vehicle Prioritization Simulator");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(980, 680));
        setPreferredSize(new Dimension(1120, 760));
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(8, 8));

        // Window listener to cleanly stop the worker thread upon closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (uiRefreshTimer != null) {
                    uiRefreshTimer.stop();
                }
                if (controller != null && controller.isRunning()) {
                    controller.stop();
                }
            }
        });
    }

    /**
     * Assembles the main dashboard panels.
     */
    private void initComponents() {
        // 1. Header Banner
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 2. Center: 2D Intersection Canvas
        intersectionPanel = new IntersectionPanel(engine);
        add(intersectionPanel, BorderLayout.CENTER);

        // 3. East: Control & Statistics Sidebar
        JPanel sidebarPanel = createSidebarPanel();
        add(sidebarPanel, BorderLayout.EAST);
    }

    /**
     * Creates the top header banner with application branding and current status.
     */
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_PANEL);
        header.setBorder(new EmptyBorder(12, 18, 12, 18));

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setBackground(BG_PANEL);

        JLabel lblTitle = new JLabel("SMART TRAFFIC MANAGEMENT DASHBOARD");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTitle.setForeground(TEXT_PRIMARY);

        JLabel lblSubtitle = new JLabel("CSC2104 Object Oriented Programming | 4-Way Emergency Vehicle Prioritization");
        lblSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSubtitle.setForeground(TEXT_MUTED);

        titleBox.add(lblTitle);
        titleBox.add(Box.createVerticalStrut(2));
        titleBox.add(lblSubtitle);

        lblStatusBadge = new JLabel("STOPPED");
        lblStatusBadge.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblStatusBadge.setForeground(TEXT_PRIMARY);
        lblStatusBadge.setOpaque(true);
        lblStatusBadge.setBackground(new Color(100, 110, 125));
        lblStatusBadge.setBorder(new EmptyBorder(6, 14, 6, 14));

        header.add(titleBox, BorderLayout.WEST);
        header.add(lblStatusBadge, BorderLayout.EAST);
        return header;
    }

    /**
     * Creates the sidebar containing Controls, Live Statistics, Vehicle Injection, and Reports.
     */
    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(BG_PANEL);
        sidebar.setPreferredSize(new Dimension(380, 0));
        sidebar.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Section 1: Execution Controls
        sidebar.add(createControlsCard());
        sidebar.add(Box.createVerticalStrut(10));

        // Section 2: Interactive Vehicle Injection
        sidebar.add(createVehicleInjectionCard());
        sidebar.add(Box.createVerticalStrut(10));

        // Section 3: Live Statistics Panel
        sidebar.add(createStatisticsCard());
        sidebar.add(Box.createVerticalStrut(10));

        // Section 4: Audit Reporting Card
        sidebar.add(createReportingCard());

        return sidebar;
    }

    /**
     * Creates the simulation playback controls (Start, Pause, Resume, Stop, Step, Speed).
     */
    private JPanel createControlsCard() {
        JPanel card = createStyledCard("Simulation Controls");
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Button Row 1: Start, Pause, Resume, Stop
        JPanel row1 = new JPanel(new GridLayout(1, 4, 6, 0));
        row1.setBackground(BG_CARD);

        btnStart = createStyledButton("Start", ACCENT_GREEN);
        btnPause = createStyledButton("Pause", ACCENT_AMBER);
        btnResume = createStyledButton("Resume", ACCENT_BLUE);
        btnStop = createStyledButton("Stop", ACCENT_RED);

        row1.add(btnStart);
        row1.add(btnPause);
        row1.add(btnResume);
        row1.add(btnStop);
        card.add(row1);
        card.add(Box.createVerticalStrut(8));

        // Button Row 2: Step Once
        JPanel row2 = new JPanel(new BorderLayout(6, 0));
        row2.setBackground(BG_CARD);
        btnStepOnce = createStyledButton("Step Once (When Paused)", new Color(108, 92, 231));
        row2.add(btnStepOnce, BorderLayout.CENTER);
        card.add(row2);
        card.add(Box.createVerticalStrut(8));

        // Speed / Delay Slider
        JPanel speedBox = new JPanel(new BorderLayout());
        speedBox.setBackground(BG_CARD);

        JLabel lblSpeed = new JLabel("Simulation Interval:");
        lblSpeed.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblSpeed.setForeground(TEXT_MUTED);

        lblSpeedValue = new JLabel(controller.getTickDelayMs() + " ms");
        lblSpeedValue.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblSpeedValue.setForeground(TEXT_PRIMARY);

        speedBox.add(lblSpeed, BorderLayout.WEST);
        speedBox.add(lblSpeedValue, BorderLayout.EAST);
        card.add(speedBox);

        int initialDelay = controller.getTickDelayMs();
        int minDelay = Math.min(25, initialDelay);
        int maxDelay = Math.max(2000, initialDelay);
        speedSlider = new JSlider(minDelay, maxDelay, initialDelay);
        speedSlider.setBackground(BG_CARD);
        speedSlider.setForeground(TEXT_PRIMARY);
        speedSlider.setMajorTickSpacing(500);
        speedSlider.setMinorTickSpacing(100);
        card.add(speedSlider);

        return card;
    }

    /**
     * Creates the interactive vehicle injection panel allowing the user to add cars or ambulances.
     */
    private JPanel createVehicleInjectionCard() {
        JPanel card = createStyledCard("Inject Traffic");
        card.setLayout(new GridLayout(3, 2, 6, 6));

        JLabel lblApproach = new JLabel("Approach Road:");
        lblApproach.setForeground(TEXT_MUTED);
        lblApproach.setFont(new Font("SansSerif", Font.PLAIN, 11));

        comboApproach = new JComboBox<>(new String[]{"North Approach", "East Approach", "South Approach", "West Approach"});
        comboApproach.setBackground(BG_DARK);
        comboApproach.setForeground(TEXT_PRIMARY);

        JLabel lblType = new JLabel("Vehicle Type:");
        lblType.setForeground(TEXT_MUTED);
        lblType.setFont(new Font("SansSerif", Font.PLAIN, 11));

        comboVehicleType = new JComboBox<>(new String[]{
                "Car (Normal)",
                "Ambulance (Urgency 5)",
                "Ambulance (Urgency 3)",
                "Bus (Public)",
                "Bike (Two-Wheeler)"
        });
        comboVehicleType.setBackground(BG_DARK);
        comboVehicleType.setForeground(TEXT_PRIMARY);

        btnAddVehicle = createStyledButton("+ Inject Vehicle", new Color(26, 188, 156));

        card.add(lblApproach);
        card.add(comboApproach);
        card.add(lblType);
        card.add(comboVehicleType);
        card.add(new JLabel("")); // spacer
        card.add(btnAddVehicle);

        return card;
    }

    /**
     * Creates the live statistics panel displaying active road, signals, and queues.
     */
    private JPanel createStatisticsCard() {
        JPanel card = createStyledCard("Live Intersection Metrics");
        card.setLayout(new GridLayout(8, 2, 4, 4));

        lblTicksElapsed = createStatLabel("0 s");
        lblActiveApproach = createStatLabel("North Approach");
        lblSignalLight = createStatLabel("[GREEN] (4s)");
        lblEmergencyOverride = createStatLabel("INACTIVE");
        lblVehiclesServed = createStatLabel("0");
        lblEmergencyServed = createStatLabel("0");
        lblQueueNorth = createStatLabel("0");
        lblQueueSouth = createStatLabel("0");

        addStatRow(card, "Ticks Elapsed:", lblTicksElapsed);
        addStatRow(card, "Active Approach:", lblActiveApproach);
        addStatRow(card, "Signal Light:", lblSignalLight);
        addStatRow(card, "Emergency Mode:", lblEmergencyOverride);
        addStatRow(card, "Total Served:", lblVehiclesServed);
        addStatRow(card, "Ambulances Served:", lblEmergencyServed);
        addStatRow(card, "North / East Queue:", lblQueueNorth);
        addStatRow(card, "South / West Queue:", lblQueueSouth);

        return card;
    }

    /**
     * Creates the report handling panel with Generate Report and View Report buttons.
     */
    private JPanel createReportingCard() {
        JPanel card = createStyledCard("Audit Reports (Phase 4)");
        card.setLayout(new GridLayout(1, 2, 6, 0));

        btnGenerateReport = createStyledButton("Generate Report", ACCENT_PURPLE);
        btnViewReport = createStyledButton("View Report", new Color(90, 100, 115));

        card.add(btnGenerateReport);
        card.add(btnViewReport);

        return card;
    }

    /**
     * Attaches action listeners to all controls.
     */
    private void setupEventHandlers() {
        // Start Button
        btnStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.start();
            }
        });

        // Pause Button
        btnPause.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.pause();
            }
        });

        // Resume Button
        btnResume.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.resume();
            }
        });

        // Stop Button
        btnStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                controller.stop();
            }
        });

        // Step Once Button
        btnStepOnce.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (controller.isPaused()) {
                    controller.stepOnce();
                } else {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Simulation must be paused to perform a single manual step.",
                            "Step Disabled", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        // Speed Slider Change Listener (Beginner-friendly anonymous inner class)
        speedSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int newDelay = speedSlider.getValue();
                controller.setTickDelayMs(newDelay);
                lblSpeedValue.setText(newDelay + " ms");
            }
        });

        // Inject Vehicle Button
        btnAddVehicle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                injectSelectedVehicle();
            }
        });

        // Generate Report Button
        btnGenerateReport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleGenerateReport();
            }
        });

        // View Report Button
        btnViewReport.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleViewReport();
            }
        });
    }

    /**
     * Injects a new vehicle onto the selected approach road safely (Thread-safe).
     */
    private void injectSelectedVehicle() {
        int approachIdx = comboApproach.getSelectedIndex();
        int typeIdx = comboVehicleType.getSelectedIndex();

        Vehicle vehicle;
        int seq = customVehicleSeq++;
        switch (typeIdx) {
            case 1: // Ambulance (Urgency 5)
                vehicle = new Ambulance("AMB-" + seq, 80, 5, "Trauma Center");
                break;
            case 2: // Ambulance (Urgency 3)
                vehicle = new Ambulance("AMB-" + seq, 70, 3, "General Hospital");
                break;
            case 3: // Bus
                vehicle = new Bus("BUS-" + seq, 35, 30);
                break;
            case 4: // Bike
                vehicle = new Bike("BIKE-" + seq, 25, true);
                break;
            default: // Car
                vehicle = new Car("CAR-" + seq, 45, 2);
                break;
        }

        // Thread-safe injection via engine method
        boolean added = engine.addVehicle(approachIdx, vehicle);
        if (!added) {
            JOptionPane.showMessageDialog(this,
                    "Could not add vehicle: Approach road is at maximum capacity.",
                    "Road Full", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Immediately update snapshot and repaint so the newly added vehicle shows up instantly
        intersectionPanel.setSnapshot(engine.getSnapshot());
        intersectionPanel.repaint();
    }

    /**
     * Generates a simulation report using ReportService.
     *
     * @param showDialog Whether to present a modal feedback dialog.
     * @return true if generation succeeded, false otherwise.
     */
    public boolean generateReportAction(boolean showDialog) {
        String reportFileName = "traffic_report.txt";
        try {
            reportService.generateReport(engine, reportFileName);
            File reportFile = new File(reportFileName);
            if (showDialog) {
                JOptionPane.showMessageDialog(this,
                        "Simulation report generated successfully!\nFile: " + reportFile.getAbsolutePath()
                                + "\nSize: " + reportFile.length() + " bytes",
                        "Report Generated", JOptionPane.INFORMATION_MESSAGE);
            }
            return true;
        } catch (ReportException ex) {
            if (showDialog) {
                JOptionPane.showMessageDialog(this,
                        "Failed to generate report: " + ex.getMessage(),
                        "Report Generation Error", JOptionPane.ERROR_MESSAGE);
            }
            return false;
        }
    }

    /**
     * Generates a simulation report using ReportService and notifies the user via dialog.
     */
    private void handleGenerateReport() {
        generateReportAction(true);
    }

    /**
     * Reads and displays the simulation report in a modal scrollable dialog.
     */
    private void handleViewReport() {
        String reportFileName = "traffic_report.txt";
        try {
            String reportContent = reportService.readReport(reportFileName);

            JTextArea textArea = new JTextArea(reportContent);
            textArea.setEditable(false);
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            textArea.setBackground(new Color(25, 28, 35));
            textArea.setForeground(new Color(220, 225, 235));
            textArea.setCaretPosition(0);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(620, 480));

            JDialog dialog = new JDialog(this, "Simulation Audit Report: " + reportFileName, true);
            dialog.getContentPane().add(scrollPane);
            dialog.pack();
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);

        } catch (ReportException ex) {
            JOptionPane.showMessageDialog(this,
                    "Unable to open report: " + ex.getMessage()
                            + "\nPlease click 'Generate Report' first.",
                    "Report Not Found", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Starts a javax.swing.Timer to refresh GUI state smoothly on the EDT at 20 Hz (50ms).
     */
    private void startUiRefreshTimer() {
        uiRefreshTimer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateDashboard();
            }
        });
        uiRefreshTimer.start();
    }

    /**
     * Periodic EDT update routine refreshing canvas and statistics.
     * Consumes an atomic, consistent SimulationSnapshot from the engine.
     */
    private void updateDashboard() {
        // 1. Capture atomic point-in-time snapshot
        SimulationSnapshot snapshot = engine.getSnapshot();

        // 2. Repaint 2D intersection using the snapshot
        intersectionPanel.setSnapshot(snapshot);
        intersectionPanel.repaint();

        // 3. Update status badge and button enabled states
        boolean isRunning = controller.isRunning();
        boolean isPaused = controller.isPaused();

        if (isRunning && !isPaused) {
            lblStatusBadge.setText("RUNNING");
            lblStatusBadge.setBackground(ACCENT_GREEN);
            btnStart.setEnabled(false);
            btnPause.setEnabled(true);
            btnResume.setEnabled(false);
            btnStop.setEnabled(true);
            btnStepOnce.setEnabled(false);
        } else if (isRunning && isPaused) {
            lblStatusBadge.setText("PAUSED");
            lblStatusBadge.setBackground(ACCENT_AMBER);
            btnStart.setEnabled(false);
            btnPause.setEnabled(false);
            btnResume.setEnabled(true);
            btnStop.setEnabled(true);
            btnStepOnce.setEnabled(true);
        } else {
            lblStatusBadge.setText("STOPPED");
            lblStatusBadge.setBackground(new Color(100, 110, 125));
            btnStart.setEnabled(true);
            btnPause.setEnabled(false);
            btnResume.setEnabled(false);
            btnStop.setEnabled(false);
            btnStepOnce.setEnabled(false);
        }

        // 4. Update live statistics from safe snapshot
        lblTicksElapsed.setText(snapshot.getTotalTicks() + " s");
        SimulationSnapshot.RoadSnapshot activeRoad = snapshot.getActiveRoad();
        if (activeRoad != null) {
            lblActiveApproach.setText(activeRoad.getName());

            String signalText = "[" + activeRoad.getSignalColor().name() + "] (" + activeRoad.getSignalTimer() + "s)";
            lblSignalLight.setText(signalText);
            if (activeRoad.isGreen()) {
                lblSignalLight.setForeground(ACCENT_GREEN);
            } else if (activeRoad.isYellow()) {
                lblSignalLight.setForeground(ACCENT_AMBER);
            } else {
                lblSignalLight.setForeground(ACCENT_RED);
            }

            if (activeRoad.isEmergencyOverride()) {
                lblEmergencyOverride.setText("EMERGENCY ACTIVE");
                lblEmergencyOverride.setForeground(ACCENT_RED);
            } else {
                lblEmergencyOverride.setText("NORMAL (INACTIVE)");
                lblEmergencyOverride.setForeground(TEXT_MUTED);
            }
        }

        lblVehiclesServed.setText(String.valueOf(snapshot.getTotalVehiclesServed()));
        lblEmergencyServed.setText(String.valueOf(snapshot.getTotalEmergencyVehiclesServed()));

        SimulationSnapshot.RoadSnapshot northRoad = snapshot.getRoad(Direction.NORTH);
        SimulationSnapshot.RoadSnapshot eastRoad = snapshot.getRoad(Direction.EAST);
        SimulationSnapshot.RoadSnapshot southRoad = snapshot.getRoad(Direction.SOUTH);
        SimulationSnapshot.RoadSnapshot westRoad = snapshot.getRoad(Direction.WEST);

        lblQueueNorth.setText("N: " + (northRoad != null ? northRoad.getQueueLength() : 0)
                + " | E: " + (eastRoad != null ? eastRoad.getQueueLength() : 0));
        lblQueueSouth.setText("S: " + (southRoad != null ? southRoad.getQueueLength() : 0)
                + " | W: " + (westRoad != null ? westRoad.getQueueLength() : 0));
    }

    // =========================================================================
    // UI Component Helpers
    // =========================================================================

    private JPanel createStyledCard(String title) {
        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(65, 72, 86), 1),
                title, TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12), TEXT_PRIMARY);
        card.setBorder(BorderFactory.createCompoundBorder(border, new EmptyBorder(8, 8, 8, 8)));
        return card;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(6, 10, 6, 10));
        return btn;
    }

    private JLabel createStatLabel(String initialText) {
        JLabel lbl = new JLabel(initialText);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(TEXT_PRIMARY);
        return lbl;
    }

    private void addStatRow(JPanel panel, String label, JLabel valueLabel) {
        JLabel titleLbl = new JLabel(label);
        titleLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        titleLbl.setForeground(TEXT_MUTED);
        panel.add(titleLbl);
        panel.add(valueLabel);
    }

    // =========================================================================
    // Getters for Automated UI Testing
    // =========================================================================

    public JButton getBtnStart() {
        return btnStart;
    }

    public JButton getBtnPause() {
        return btnPause;
    }

    public JButton getBtnResume() {
        return btnResume;
    }

    public JButton getBtnStop() {
        return btnStop;
    }

    public JButton getBtnStepOnce() {
        return btnStepOnce;
    }

    public JButton getBtnGenerateReport() {
        return btnGenerateReport;
    }

    public JButton getBtnViewReport() {
        return btnViewReport;
    }

    public SimulationController getController() {
        return controller;
    }

    public SimulationEngine getEngine() {
        return engine;
    }

    // =========================================================================
    // Standalone GUI Application Launcher
    // =========================================================================

    /**
     * Directly launches the Swing Graphical User Interface with initial vehicle queues.
     *
     * @param args Command line arguments (optional).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Intersection intersection = new Intersection();

                // Pre-populate intersection with starter commuter traffic and an emergency vehicle
                intersection.getRoad(Direction.NORTH).addVehicle(new Car("CAR-N1", 45, 2));
                intersection.getRoad(Direction.NORTH).addVehicle(new Bus("BUS-N1", 35, 30));
                intersection.getRoad(Direction.EAST).addVehicle(new Bike("BIKE-E1", 30, true));
                intersection.getRoad(Direction.EAST).addVehicle(new Car("CAR-E1", 50, 4));
                intersection.getRoad(Direction.SOUTH).addVehicle(new Car("CAR-S1", 40, 1));
                intersection.getRoad(Direction.SOUTH).addVehicle(new Ambulance("AMB-S1", 80, 5, "City Trauma Center"));
                intersection.getRoad(Direction.WEST).addVehicle(new Car("CAR-W1", 55, 3));
                intersection.getRoad(Direction.WEST).addVehicle(new Bike("BIKE-W1", 25, true));

                SimulationEngine engine = new SimulationEngine(intersection, 4, 2, 15);
                SimulationController controller = new SimulationController(engine, 1000);

                MainFrame frame = new MainFrame(controller);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }
}
