import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.text.DecimalFormat;

public class JuliaSet {
    public JFrame frame;
    public JPanel panel;
    public JPanel controlPanel1;
    public JPanel controlPanel2;
    public JPanel imagePanel;
    public JPanel borderPanel1;
    public JPanel borderPanel2;
    public JPanel borderPanel3;

    JTextField textFieldCY;
    JTextField textFieldIterations;
    JLabel labelZoom;
    JLabel labelColorMethod;
    JLabel imageLabel;
    JLabel hueSliderLabel;
    JLabel brightnessSliderLabel;
    JLabel blackThresholdLabel;
    JSlider sliderZoom;
    JSlider sliderColorHue;
    JSlider sliderColorBrightness;
    JSlider sliderBlackThreshold;

    BufferedImage juliaSetImage;

    int sliderFactor;
    int imageRectX;
    int imageRectY;
    int colorHueSliderFactor;
    double colorBrightnessSliderFactor;
    double blackThresholdSliderFactor;

    public int juliaIterations;
    public double juliaCX;
    public double juliaCY;
    public double juliaXAxisMin;
    public double juliaXAxisMax;
    public double juliaYAxisMin;
    public double juliaYAxisMax;

    public double distanceMinMaxX;
    public double distanceMinMaxY;
    public double offsetX;
    public double offsetY;
    public double zoom;
    public double stretchFactorX;
    public double stretchFactorY;
    public double mouseOffsetX;
    public double mouseOffsetY;

    // Rounding to two decimal points
    // Used for the slider
    static final DecimalFormat df = new DecimalFormat("0.00");

    // For the color picker
    public int selectedColorMethod;

    static ButtonGroup colorMethods;
    static JRadioButton colorMethod1;
    static JRadioButton colorMethod2;

    // for the resize event
    private javax.swing.Timer waitingTimer;

    JuliaSet() {
        initWindow();
        initGuiElements();
        showGUI();
        drawJuliaSet();
    }

    public void drawJuliaSet() {
        // Start timer
        long startTime = System.currentTimeMillis();

        // Because the JPanel with the image on it doesn't want to reveal its own size...
        int trueHeight = this.frame.getContentPane().getComponent(0).getHeight() - (this.controlPanel1.getHeight() + this.controlPanel2.getHeight() + 3 * 10);

        this.imageRectX = this.imagePanel.getBounds().width;
        this.imageRectY = trueHeight;

        // If the user switches the variables around
        double temp;
        if (juliaXAxisMin > juliaXAxisMax) {
            temp = juliaXAxisMax;
            juliaXAxisMax = juliaXAxisMin;
            juliaXAxisMin = temp;
        }
        if (juliaYAxisMin > juliaYAxisMax) {
            temp = juliaYAxisMax;
            juliaYAxisMax = juliaYAxisMin;
            juliaYAxisMin = temp;
        }

        // All the calculations are done beforehand
        // to save time. No need to calculate them
        // every single time...

        // zoom = slider value ^ 3
        zoom = Math.pow((double) sliderFactor / 10.0, 3);

        distanceMinMaxX = Math.abs(juliaXAxisMax - juliaXAxisMin);
        distanceMinMaxY = Math.abs(juliaYAxisMax - juliaYAxisMin);

        offsetX = (juliaXAxisMin + (Math.abs(distanceMinMaxX) / 2));
        offsetY = (juliaYAxisMin + (Math.abs(distanceMinMaxY) / 2));

        stretchFactorX = distanceMinMaxX / 2;
        stretchFactorY = distanceMinMaxY / 2;

        // Create image to draw on
        juliaSetImage = new BufferedImage(this.imageRectX, this.imageRectY, BufferedImage.TYPE_INT_RGB);

        int systemThreads = Runtime.getRuntime().availableProcessors();

        JuliaDrawThread[] jA = new JuliaDrawThread[systemThreads];

        for (int t = 0; t < systemThreads; t++) {
            // Make sure that the reference is empty
            jA[t] = null;
            jA[t] = new JuliaDrawThread(t, systemThreads, this);
        }

        // Create array of threads
        Thread[] tA = new Thread[systemThreads];

        // here's where we
        for (int t = 0; t < systemThreads; t++) {
            tA[t] = new Thread(jA[t]);
            tA[t].start();
        }

        BufferedImage[] partialImages = new BufferedImage[systemThreads];
        for (int t = 0; t < systemThreads; t++) {
            try {
                tA[t].join();
                partialImages[t] = jA[t].getPartialImage();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // At this point we've collected all the image data
        // Merge into one image
        int currentHeight = 0;
        Graphics2D g2d = juliaSetImage.createGraphics();
        for (BufferedImage pImage: partialImages) {
            g2d.drawImage(pImage, 0, currentHeight, null);
            currentHeight += pImage.getHeight();
        }

        g2d.dispose();

        // Draw the image here
        imageLabel.setIcon(new ImageIcon(juliaSetImage));

        // End timer
        long endTime = System.currentTimeMillis();
        System.out.printf("Time: %dms%n", endTime - startTime);
    }

    public void zoomSliderStateChanged(ChangeEvent e) {
        JSlider source = (JSlider) e.getSource();
        sliderFactor = source.getValue();

        // Rounding the number before writing it to the label
        labelZoom.setText(df.format((Math.pow((double) sliderFactor / 10, 3))));

        drawJuliaSet();
    }

    public void colorHueSliderStateChanged(ChangeEvent e) {
        JSlider source = (JSlider) e.getSource();
        colorHueSliderFactor = source.getValue();
        drawJuliaSet();
    }

    public void brightnessSliderStateChanged(ChangeEvent e) {
        JSlider source = (JSlider) e.getSource();
        colorBrightnessSliderFactor = (float) source.getValue();
        drawJuliaSet();
    }

    public void blackThresholdSliderStateChanged(ChangeEvent e) {
        JSlider source = (JSlider) e.getSource();
        blackThresholdSliderFactor = (float) source.getValue() / 250;
        drawJuliaSet();
    }

    // Universal text field changer
    // That way I don't have to re-write the float and integer parsing for every single text field
    // Using reflection we just look for a variable with a matching name

    // Help for key listener: https://kodejava.org/how-do-i-add-key-listener-event-handler-to-jtextfield/
    public void textFieldChanged(JTextField jt, String variableToBeChanged) {
        try {
            // Get variable of object by name
            Field f = this.getClass().getDeclaredField(variableToBeChanged);
            if (f.getType().toString().equals("double")) {
                try {
                    f.setDouble(this, Double.parseDouble(jt.getText()));
                    // Value changed successfully, redraw Julia set
                    drawJuliaSet();
                } catch (NumberFormatException ex) {
                    jt.setText(Double.toString(f.getDouble(this)));
                    JOptionPane.showConfirmDialog(null, "Please only enter floating point numbers", "Floats only", JOptionPane.DEFAULT_OPTION);
                } catch (IllegalAccessException e) {
                    // Just like the other exceptions these should never occur
                    // because the user doesn't have control over them
                    // Prints stack trace to the terminal, but the user can't see that *taps head*
                    e.printStackTrace();
                }
            }
            if (f.getType().toString().equals("int")) {
                try {
                    f.setInt(this, Integer.parseInt(jt.getText()));
                    drawJuliaSet();
                } catch (NumberFormatException ex) {
                    jt.setText(Integer.toString(f.getInt(this)));
                    JOptionPane.showConfirmDialog(null, "Please only enter integers", "Integers only", JOptionPane.DEFAULT_OPTION);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        // These are never supposed to be hit
        // The user doesn't have control over any of this
        // They're just here so IntelliJ doesn't complain
        catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    class ColorRadioButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent event) {
            JRadioButton button = (JRadioButton) event.getSource();

            if (button == colorMethod1) {
                selectedColorMethod = 1;
                hueSliderLabel.setVisible(false);
                brightnessSliderLabel.setVisible(false);
                sliderColorBrightness.setVisible(false);
                sliderColorHue.setVisible(false);
                blackThresholdLabel.setVisible(false);
                sliderBlackThreshold.setVisible(false);
            } else if (button == colorMethod2) {
                selectedColorMethod = 2;
                hueSliderLabel.setVisible(true);
                brightnessSliderLabel.setVisible(true);
                sliderColorBrightness.setVisible(true);
                sliderColorHue.setVisible(true);
                blackThresholdLabel.setVisible(true);
                sliderBlackThreshold.setVisible(true);
            }

            drawJuliaSet();
        }
    }

    public void initGuiElements() {
        this.panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Create panels to serve as borders
        borderPanel1 = new JPanel();
        borderPanel2 = new JPanel();
        borderPanel3 = new JPanel();

        borderPanel1.setMinimumSize(new Dimension(10, 10));
        borderPanel1.setPreferredSize(new Dimension(10, 10));
        borderPanel2.setMinimumSize(new Dimension(10, 10));
        borderPanel2.setPreferredSize(new Dimension(10, 10));
        borderPanel3.setMinimumSize(new Dimension(10, 10));
        borderPanel3.setPreferredSize(new Dimension(10, 10));

        borderPanel1.setLayout(new BoxLayout(borderPanel1, BoxLayout.X_AXIS));
        borderPanel2.setLayout(new BoxLayout(borderPanel2, BoxLayout.X_AXIS));
        borderPanel3.setLayout(new BoxLayout(borderPanel3, BoxLayout.X_AXIS));

        // Create JPanels for the controls at the top
        this.controlPanel1 = new JPanel();
        this.controlPanel2 = new JPanel();


        this.controlPanel1.setLayout(new BoxLayout(this.controlPanel1, BoxLayout.X_AXIS));
        this.controlPanel2.setLayout(new BoxLayout(this.controlPanel2, BoxLayout.X_AXIS));

        // Add space in-between control elements
        this.controlPanel1.add(Box.createRigidArea(new Dimension(7, 0)));
        JLabel labelParameterC = new JLabel("Parameter C:");
        this.controlPanel1.add(labelParameterC);
        this.controlPanel1.add(Box.createRigidArea(new Dimension(7, 0)));

        JLabel labelCX = new JLabel("X");
        this.controlPanel1.add(labelCX);
        this.controlPanel1.add(Box.createRigidArea(new Dimension(7, 0)));

        JTextField textFieldCX = new JTextField("0.0");
        textFieldCX.setMaximumSize(new Dimension(50, 30));
        juliaCX = 0.0 f;
        textFieldCX.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                textFieldChanged(textFieldCX, "juliaCX");
            }
        });
        textFieldCX.setPreferredSize(new Dimension(40, 30));
        textFieldCX.setMaximumSize(new Dimension(90, 30));
        this.controlPanel1.add(textFieldCX);

        this.controlPanel1.add(Box.createRigidArea(new Dimension(7, 0)));

        JLabel labelCY = new JLabel("Y");
        this.controlPanel1.add(labelCY);
        this.controlPanel1.add(Box.createRigidArea(new Dimension(7, 0)));


        textFieldCY = new JTextField("0.0");
        textFieldCY.setPreferredSize(new Dimension(40, 30));
        textFieldCY.setMaximumSize(new Dimension(90, 30));
        juliaCY = 0.0 f;
        textFieldCY.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                textFieldChanged(textFieldCY, "juliaCY");
            }
        });

        this.controlPanel1.add(textFieldCY);
        this.controlPanel1.add(Box.createRigidArea(new Dimension(7, 0)));

        JLabel labelIterations = new JLabel("Iterations");
        this.controlPanel1.add(labelIterations);

        textFieldIterations = new JTextField("300");
        textFieldIterations.setPreferredSize(new Dimension(40, 30));
        textFieldIterations.setMaximumSize(new Dimension(90, 30));
        juliaIterations = 300;
        textFieldIterations.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                textFieldChanged(textFieldIterations, "juliaIterations");
            }
        });
        this.controlPanel1.add(textFieldIterations);
        this.controlPanel1.add(Box.createRigidArea(new Dimension(7, 0)));

        // Initial values to constrain the first drawn julia set
        juliaXAxisMin = -2.0 f;
        juliaXAxisMax = 2.0 f;
        juliaYAxisMax = 2.0 f;
        juliaYAxisMin = -2.0 f;

        // Zoom label
        labelZoom = new JLabel("Zoom");
        this.controlPanel1.add(labelZoom);
        this.controlPanel1.add(Box.createRigidArea(new Dimension(7, 0)));

        // Zoom slider
        // unfortunately it only takes integers, so we'll have to divide by ten
        // to get the number we actually want
        sliderZoom = new JSlider(1, 1000, 10);

        // + 2 because the slider feels slightly off-center otherwise
        sliderZoom.addChangeListener(this::zoomSliderStateChanged);
        this.controlPanel1.add(sliderZoom);
        this.controlPanel1.add(Box.createRigidArea(new Dimension(7, 0)));

        // Text field right next to the slider
        // shows the zoom factor, is not writable
        labelZoom = new JLabel("1,0");
        sliderFactor = 10;
        this.controlPanel1.add(labelZoom);
        this.controlPanel1.add(Box.createRigidArea(new Dimension(7, 0)));
        labelZoom.setMinimumSize(new Dimension(50, 30));
        labelZoom.setPreferredSize(new Dimension(50, 30));

        labelColorMethod = new JLabel("View:");
        this.controlPanel2.add(Box.createRigidArea(new Dimension(7, 0)));
        this.controlPanel2.add(labelColorMethod);

        // Create buttons
        colorMethod1 = new JRadioButton();
        this.controlPanel2.add(colorMethod1);

        // Set this one to the default
        // We do this before we add the item listener
        // No need to draw the set twice just because
        // we clicked a button
        colorMethod1.doClick();
        selectedColorMethod = 1;

        colorMethod2 = new JRadioButton();
        this.controlPanel2.add(colorMethod2);

        // Create group for the radio buttons
        colorMethods = new ButtonGroup();
        colorMethods.add(colorMethod1);
        colorMethods.add(colorMethod2);

        // Add listener for the buttons
        ColorRadioButtonActionListener colorActionListener = new ColorRadioButtonActionListener();
        colorMethod1.addActionListener(colorActionListener);
        colorMethod2.addActionListener(colorActionListener);

        // "H" for the slider
        hueSliderLabel = new JLabel("H  ");
        this.controlPanel2.add(hueSliderLabel);
        hueSliderLabel.setVisible(false);

        // Hue slider
        sliderColorHue = new JSlider(0, 100, 0);
        // + 2 because the slider feels slightly off-center otherwise
        sliderColorHue.addChangeListener(this::colorHueSliderStateChanged);
        sliderColorHue.setVisible(false);
        this.controlPanel2.add(sliderColorHue);

        brightnessSliderLabel = new JLabel("B  ");
        this.controlPanel2.add(brightnessSliderLabel);
        brightnessSliderLabel.setVisible(false);

        // Brightness slider
        sliderColorBrightness = new JSlider(1, 100, 1);
        // + 2 because the slider feels slightly off-center otherwise
        sliderColorBrightness.addChangeListener(this::brightnessSliderStateChanged);
        sliderColorBrightness.setVisible(false);
        this.controlPanel2.add(sliderColorBrightness);
        colorBrightnessSliderFactor = 1;

        // Black threshold label
        blackThresholdLabel = new JLabel("Threshold  ");
        blackThresholdLabel.setVisible(false);
        this.controlPanel2.add(blackThresholdLabel);

        // Black threshold label
        sliderBlackThreshold = new JSlider(0, 100, 0);
        sliderBlackThreshold.addChangeListener(this::blackThresholdSliderStateChanged);
        sliderBlackThreshold.setVisible(false);
        this.controlPanel2.add(sliderBlackThreshold);

        // Draw image onto label
        // Every time setIcon() is called the image updates
        imagePanel = new JPanel();
        imageLabel = new JLabel();
        this.imagePanel.add(imageLabel);

        System.gc();
    }

    public void initWindow() {
        frame = new JFrame();
        frame.setTitle("Julia Set");

        // Windows UI
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }

        frame.setMinimumSize(new Dimension(800, 800));

        frame.setLocationRelativeTo(null);

        // Actually make sure that the window gets closed when we press the X button
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                we.getWindow().dispose(); // Throw away the window
                System.exit(0); // Exit JVM
            }
        });
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == this.waitingTimer) {
            //Stop timer, event has ended
            this.waitingTimer.stop();
            this.waitingTimer = null;

            this.imagePanel.setPreferredSize(new Dimension(this.imagePanel.getBounds().width, this.imagePanel.getBounds().height));
            drawJuliaSet();
        }
    }

    public void showGUI() {
        this.panel.add(borderPanel1);
        this.panel.add(controlPanel1);
        this.panel.add(borderPanel2);
        this.panel.add(controlPanel2);
        this.panel.add(borderPanel3);
        this.panel.add(imagePanel);
        this.frame.add(this.panel);
        this.frame.setVisible(true);

        // Implement timer to wait until the resize event is over
        this.frame.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent componentEvent) {
                if (waitingTimer == null) {
                    waitingTimer = new Timer(50, JuliaSet.this::actionPerformed);
                    waitingTimer.start();
                } else {
                    waitingTimer.restart();
                }
            }
        });

        MouseAdapter ma = new MouseAdapter() {
            private Point startPoint;

            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                mouseOffsetX += (p.x - startPoint.x) / zoom;
                mouseOffsetY += (p.y - startPoint.y) / zoom;
                startPoint = null;
                drawJuliaSet();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                sliderZoom.setValue(sliderZoom.getValue() - e.getWheelRotation());
            }
        };

        frame.addMouseListener(ma);
        frame.addMouseMotionListener(ma);
        frame.addMouseWheelListener(ma);
    }

    public static void main(String[] args) {
        new JuliaSet();
    }
}