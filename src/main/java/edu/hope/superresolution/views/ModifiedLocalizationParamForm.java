/**
 * MainForm.java
 *
 * Form showing the UI controlling tracking of single molecules using
 * Gaussian Fitting
 *
 * The real work is done in class GaussianTrackThread
 *
 * Created on Sep 15, 2010, 9:29:05 PM
 */

package edu.hope.superresolution.views;

import edu.hope.superresolution.MMgaussianfitmods.datasubs.ExtendedGaussianInfo;
import edu.hope.superresolution.fitters.FindLocalMaxima;
import edu.hope.superresolution.fitters.GenericBaseGaussianFitThread;
import edu.hope.superresolution.models.GaussianFitParamModel;
import edu.hope.superresolution.models.ModelUpdateDispatcher;
import edu.hope.superresolution.models.ModelUpdateListener;
import edu.valelab.gaussianfit.FitAllThread;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import java.awt.Color;
import java.awt.Polygon;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import edu.valelab.gaussianfit.utils.NumberUtils;
import edu.valelab.gaussianfit.utils.ReportingUtils;
import java.text.ParseException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 *
 * @author nico
 */
public class ModifiedLocalizationParamForm extends javax.swing.JFrame implements ModelUpdateListener {
   private static final String NOISETOLERANCE = "NoiseTolerance";
   private static final String SNR = "SNR";
   private static final String INTENSITYTHRESHOLD = "IntensityThreshold";
   private static final String PCF = "PhotonConversionFactor";
   private static final String GAIN = "Gain";
   private static final String PIXELSIZE = "PixelSize";
   private static final String TIMEINTERVALMS = "TimeIntervalMs";
   private static final String ZSTEPSIZE = "ZStepSize";
   private static final String BACKGROUNDLEVEL = "BackgroundLevel";
   private static final String WIDTHMAX = "SigmaMax";
   private static final String WIDTHMIN = "SigmaMin";
   private static final String USEFILTER = "UseFilter";
   private static final String NRPHOTONSMIN = "NrPhotonsMin";
   private static final String NRPHOTONSMAX = "NrPhotonsMax";
   private static final String USENRPHOTONSFILTER = "UseNrPhotonsFilter";
   private static final String MAXITERATIONS = "MaxIterations";
   private static final String BOXSIZE = "BoxSize";
   private static final String FRAMEXPOS = "XPos";
   private static final String FRAMEYPOS = "YPos";
   private static final String FRAMEWIDTH = "Width";
   private static final String FRAMEHEIGHT = "Height";
   private static final String FITMODE = "FitMode";
   private static final String ENDTRACKBOOL = "EndTrackBoolean";
   private static final String ENDTRACKINT = "EndTrackAfterN";
   private static final String PREFILTER = "PreFilterType";
   private static final String MAXTRACKTRAVEL = "MaxTrackTravel";

   // we are a singleton with only one window
   public static boolean WINDOWOPEN = false;

   Preferences prefs_;

   // Store values of dropdown menus:
   private int shape_ = 1;
   private final int fitMode_ = 2;
   private FindLocalMaxima.FilterType preFilterType_ = FindLocalMaxima.FilterType.NONE;
   private GenericBaseGaussianFitThread.DataEnsureMode dataEnsureMode_ = GenericBaseGaussianFitThread.DataEnsureMode.none;
   private int maxTrackTravel_ = 10;
   private double minWidthnm_ = 105; //In nm
   private double maxWidthnm_ = 400; //In nm
   
   private FitAllThread ft_;
   
   public AtomicBoolean aStop_ = new AtomicBoolean(false);

   private int lastFrame_ = -1;
   
   // to keep track of front most window
   ImagePlus ip_ = null;
   
   //Permanently set showOverlay_ to on
   private boolean showOverlay_ = false;
   
   private GaussianFitParamModel gaussFitParamModel_;
   private final ExtendedGaussianInfo gaussianSettingsRef_ = new ExtendedGaussianInfo();

    /**
     * Creates new form for Parameters in Fitting
     * 
     */
    public ModifiedLocalizationParamForm( /*GaussianFitParamModel gaussFitParamModel*/) {
      
       gaussFitParamModel_ = null; //Store model
        
       initComponents();

       if (prefs_ == null)
            prefs_ = Preferences.userNodeForPackage(this.getClass());
       noiseToleranceTextField_.setText(Integer.toString(prefs_.getInt(NOISETOLERANCE,50)));
       IntensityThreshold.setText( Integer.toString( prefs_.getInt(INTENSITYTHRESHOLD, 800) ));
       SNRthreshold.setText( Integer.toString( prefs_.getInt( SNR, 4) ) );
       photonConversionTextField.setText(Double.toString(prefs_.getDouble(PCF, 3.57)));
       emGainTextField_.setText(Double.toString(prefs_.getDouble(GAIN, 1)));
       pixelSizeTextField_.setText(Double.toString(prefs_.getDouble(PIXELSIZE, 82.0)));
       baseLevelTextField.setText(Double.toString(prefs_.getDouble(BACKGROUNDLEVEL, 50)));
       timeIntervalTextField_.setText(Double.toString(prefs_.getDouble(TIMEINTERVALMS, 10)));
       zStepTextField_.setText(Double.toString(prefs_.getDouble(ZSTEPSIZE, 0)));                   
       pixelSizeTextField_.getDocument().addDocumentListener(new BackgroundCleaner(pixelSizeTextField_));
       emGainTextField_.getDocument().addDocumentListener(new BackgroundCleaner(emGainTextField_));      
       timeIntervalTextField_.getDocument().addDocumentListener(new BackgroundCleaner(timeIntervalTextField_));

       minWidthTextField.setText(Double.toString(prefs_.getDouble(WIDTHMIN, minWidthnm_)));
       maxWidthTextField.setText(Double.toString(prefs_.getDouble(WIDTHMAX, maxWidthnm_)));
       //filterDataCheckBoxNrPhotons.setSelected(prefs_.getBoolean(USENRPHOTONSFILTER, false));
       dataScalingMethodComboBox.setSelectedIndex(prefs_.getInt(FITMODE, 1));
       maxIterationsTextField.setText(Integer.toString(prefs_.getInt(MAXITERATIONS, 250)));
       boxSizeTextField.setText(Integer.toString(prefs_.getInt(BOXSIZE, 8)));
       //filterDataCheckBoxWidth.setSelected(prefs_.getBoolean(USEFILTER, false));
       preFilterComboBox_.setSelectedIndex(prefs_.getInt(PREFILTER, 0));
       endTrackCheckBox_.setSelected(prefs_.getBoolean(ENDTRACKBOOL, false));
       endTrackSpinner_.setValue(prefs_.getInt(ENDTRACKINT, 0));
       maxTrackTravelTextField_.setText( Integer.toString(prefs_.getInt(MAXTRACKTRAVEL, maxTrackTravel_ )) );
       maxTrackTravel_ = prefs_.getInt(MAXTRACKTRAVEL, maxTrackTravel_ );
       minWidthnm_ = prefs_.getDouble(WIDTHMIN, 80 );
       maxWidthnm_ = prefs_.getDouble(WIDTHMAX, 800 );
       
       DocumentListener updateNoiseOverlay = new DocumentListener() {

          @Override
          public void changedUpdate(DocumentEvent documentEvent) {
             updateDisplay();
          }

          @Override
          public void insertUpdate(DocumentEvent documentEvent) {
             updateDisplay();
          }

          @Override
          public void removeUpdate(DocumentEvent documentEvent) {
             updateDisplay();
          }

          private void updateDisplay() {
             if (WINDOWOPEN && showOverlay_ ) {
                showNoiseTolerance();
             }
          }
       };

       noiseToleranceTextField_.getDocument().addDocumentListener(updateNoiseOverlay);
       boxSizeTextField.getDocument().addDocumentListener(updateNoiseOverlay);
       
       this.getRootPane().setDefaultButton( saveButton );
          
       setTitle("Fitting Settings");
       
       // wdith on Mac should be 250, Windows 270
       setBounds(prefs_.getInt(FRAMEXPOS, 100), prefs_.getInt(FRAMEYPOS, 100), 270, 675);
       //ImagePlus.addImageListener(this);
       
    }

    @Override
    public void update(ModelUpdateDispatcher caller, int event) {
        if (caller instanceof GaussianFitParamModel) {
            switch (event) {
                case GaussianFitParamModel.SETTINGS_UPDATE:
                    break;
            }
        }
    }

    @Override
    public void guiEnable(boolean enable) {
        if( enable ) {
            assert( gaussFitParamModel_ != null );
        }
        setVisible( enable );
    }

    /**
     * Callback that anticipates a GaussianFitParamModel, and only stores the first one registered.
     * This is of course if that model has not been unregistered by conventional means.
     * 
     * @param registeredModel 
     */
    @Override
    public void onRegisteredToModel(ModelUpdateDispatcher registeredModel) {
        //only bonds to one gaussFitParamModelInstance
        if( registeredModel instanceof GaussianFitParamModel ) {
            if( gaussFitParamModel_ == null ) {
                gaussFitParamModel_ = (GaussianFitParamModel) registeredModel;
                updateValues( gaussianSettingsRef_ );
            } else {
                //unregister from the object possibly, but there is potential recursion here
                //This may be better as a standard exception to notify the calling context
            }
            
        }
    }

    /**
     * Callback that anticipates a GaussianFitParamModel being unregistered and if this is
     * the model registered, sets the internal model to null.
     * 
     * @param unregisteredModel 
     */
    @Override
    public void onUnregisteredToModel(ModelUpdateDispatcher unregisteredModel) {
        if( unregisteredModel != null && gaussFitParamModel_ == unregisteredModel ) {
            gaussFitParamModel_ = null;
        }
    }

   private class BackgroundCleaner implements DocumentListener {

      JTextField field_;

      public BackgroundCleaner(JTextField field) {
         field_ = field;
      }

      private void updateBackground() {
         field_.setBackground(Color.white);
      }

      @Override
      public void changedUpdate(DocumentEvent documentEvent) {
         updateBackground();
      }

      @Override
      public void insertUpdate(DocumentEvent documentEvent) {
         updateBackground();
      }

      @Override
      public void removeUpdate(DocumentEvent documentEvent) {
         updateBackground();
      }
   };
    

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        photonConversionTextField = new javax.swing.JTextField();
        emGainTextField_ = new javax.swing.JTextField();
        baseLevelTextField = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jSeparator4 = new javax.swing.JSeparator();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        jSeparator3 = new javax.swing.JSeparator();
        noiseToleranceTextField_ = new javax.swing.JTextField();
        pixelSizeTextField_ = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jSeparator5 = new javax.swing.JSeparator();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        preFilterComboBox_ = new javax.swing.JComboBox();
        fitDimensionsComboBox1 = new javax.swing.JComboBox();
        timeIntervalTextField_ = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        maxIterationsTextField = new javax.swing.JTextField();
        minWidthTextField = new javax.swing.JTextField();
        jLabel16 = new javax.swing.JLabel();
        maxWidthTextField = new javax.swing.JTextField();
        endTrackCheckBox_ = new javax.swing.JCheckBox();
        endTrackSpinner_ = new javax.swing.JSpinner();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        dataScalingMethodComboBox = new javax.swing.JComboBox();
        jLabel21 = new javax.swing.JLabel();
        zStepTextField_ = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        cancelButton = new javax.swing.JButton();
        applyButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        maxTrackTravelTextField_ = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        fitMethodComboBox2 = new javax.swing.JComboBox();
        jLabel24 = new javax.swing.JLabel();
        boxSizeTextField = new javax.swing.JTextField();
        SNRthreshold = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        IntensityThreshold = new javax.swing.JTextField();
        jLabel26 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jSeparator6 = new javax.swing.JSeparator();
        jLabel2 = new javax.swing.JLabel();
        MicroscopePropertiesButton_ = new javax.swing.JButton();

        jButton1.setText("jButton1");

        setBounds(new java.awt.Rectangle(0, 22, 250, 550));
        setMinimumSize(new java.awt.Dimension(250, 550));
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        getContentPane().setLayout(null);

        jLabel1.setText("Fit Parameters...");
        getContentPane().add(jLabel1);
        jLabel1.setBounds(20, 350, 82, 14);

        jLabel3.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel3.setText("Data Scaling Method");
        getContentPane().add(jLabel3);
        jLabel3.setBounds(20, 400, 110, 14);

        jLabel4.setText("Filter Data...");
        getContentPane().add(jLabel4);
        jLabel4.setBounds(20, 480, 87, 20);
        getContentPane().add(jLabel5);
        jLabel5.setBounds(0, 0, 0, 0);

        jLabel6.setText("Imaging parameters...");
        getContentPane().add(jLabel6);
        jLabel6.setBounds(20, 60, 142, 14);

        photonConversionTextField.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        photonConversionTextField.setText("10.41");
        getContentPane().add(photonConversionTextField);
        photonConversionTextField.setBounds(170, 80, 67, 20);

        emGainTextField_.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        emGainTextField_.setText("50");
        getContentPane().add(emGainTextField_);
        emGainTextField_.setBounds(170, 100, 67, 19);

        baseLevelTextField.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        baseLevelTextField.setText("100");
        baseLevelTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                baseLevelTextFieldActionPerformed(evt);
            }
        });
        getContentPane().add(baseLevelTextField);
        baseLevelTextField.setBounds(170, 180, 67, 20);

        jLabel7.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel7.setText("Photons/Single Count");
        getContentPane().add(jLabel7);
        jLabel7.setBounds(40, 80, 120, 14);

        jLabel8.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel8.setText("Linear (EM) Gain");
        getContentPane().add(jLabel8);
        jLabel8.setBounds(40, 100, 81, 14);

        jLabel9.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel9.setText("Background Offset Counts");
        getContentPane().add(jLabel9);
        jLabel9.setBounds(40, 180, 130, 14);
        getContentPane().add(jSeparator4);
        jSeparator4.setBounds(496, 191, 1, 9);
        getContentPane().add(jSeparator1);
        jSeparator1.setBounds(20, 340, 220, 10);
        getContentPane().add(jSeparator2);
        jSeparator2.setBounds(20, 50, 220, 10);
        getContentPane().add(jSeparator3);
        jSeparator3.setBounds(20, 480, 220, 10);

        noiseToleranceTextField_.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        noiseToleranceTextField_.setText("2000");
        noiseToleranceTextField_.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                noiseToleranceTextField_FocusLost(evt);
            }
        });
        noiseToleranceTextField_.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                noiseToleranceTextField_ActionPerformed(evt);
            }
        });
        noiseToleranceTextField_.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                noiseToleranceTextField_PropertyChange(evt);
            }
        });
        noiseToleranceTextField_.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                noiseToleranceTextField_KeyTyped(evt);
            }
        });
        getContentPane().add(noiseToleranceTextField_);
        noiseToleranceTextField_.setBounds(180, 250, 60, 20);

        pixelSizeTextField_.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        pixelSizeTextField_.setText("0.8");
        pixelSizeTextField_.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pixelSizeTextField_ActionPerformed(evt);
            }
        });
        getContentPane().add(pixelSizeTextField_);
        pixelSizeTextField_.setBounds(170, 120, 67, 20);

        jLabel13.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel13.setText("PixelSize (nm)");
        getContentPane().add(jLabel13);
        jLabel13.setBounds(40, 120, 122, 14);
        getContentPane().add(jSeparator5);
        jSeparator5.setBounds(20, 580, 220, 10);

        jLabel11.setText("Noise Characterization...");
        getContentPane().add(jLabel11);
        jLabel11.setBounds(20, 210, 130, 14);

        jLabel12.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel12.setText("Pre-Filter");
        getContentPane().add(jLabel12);
        jLabel12.setBounds(90, 230, 60, 14);

        jLabel14.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel14.setText("Dimensions");
        getContentPane().add(jLabel14);
        jLabel14.setBounds(80, 360, 54, 14);

        preFilterComboBox_.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        preFilterComboBox_.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None", "Gaussian1-5" }));
        preFilterComboBox_.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                preFilterComboBox_ActionPerformed(evt);
            }
        });
        getContentPane().add(preFilterComboBox_);
        preFilterComboBox_.setBounds(150, 230, 90, 20);

        fitDimensionsComboBox1.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        fitDimensionsComboBox1.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2" }));
        fitDimensionsComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fitDimensionsComboBox1ActionPerformed(evt);
            }
        });
        getContentPane().add(fitDimensionsComboBox1);
        fitDimensionsComboBox1.setBounds(150, 360, 90, 20);

        timeIntervalTextField_.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        timeIntervalTextField_.setText("0.8");
        getContentPane().add(timeIntervalTextField_);
        timeIntervalTextField_.setBounds(170, 140, 67, 20);

        jLabel15.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel15.setText("Time Interval (ms)");
        getContentPane().add(jLabel15);
        jLabel15.setBounds(40, 140, 122, 14);

        jLabel17.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel17.setText("Max Iterations");
        getContentPane().add(jLabel17);
        jLabel17.setBounds(50, 430, 90, 14);

        maxIterationsTextField.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        maxIterationsTextField.setText("250");
        getContentPane().add(maxIterationsTextField);
        maxIterationsTextField.setBounds(170, 430, 70, 20);

        minWidthTextField.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        minWidthTextField.setText("100");
        minWidthTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                minWidthTextFieldActionPerformed(evt);
            }
        });
        getContentPane().add(minWidthTextField);
        minWidthTextField.setBounds(20, 500, 50, 30);

        jLabel16.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel16.setText("    ( 2* sigma of fit)");
        getContentPane().add(jLabel16);
        jLabel16.setBounds(70, 510, 110, 20);

        maxWidthTextField.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        maxWidthTextField.setText("200");
        maxWidthTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                maxWidthTextFieldActionPerformed(evt);
            }
        });
        getContentPane().add(maxWidthTextField);
        maxWidthTextField.setBounds(180, 500, 60, 30);

        endTrackCheckBox_.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        endTrackCheckBox_.setText("End track when missing");
        endTrackCheckBox_.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                endTrackCheckBox_ActionPerformed(evt);
            }
        });
        getContentPane().add(endTrackCheckBox_);
        endTrackCheckBox_.setBounds(20, 530, 150, 23);

        endTrackSpinner_.setModel(new javax.swing.SpinnerNumberModel(1, 1, null, 1));
        getContentPane().add(endTrackSpinner_);
        endTrackSpinner_.setBounds(160, 530, 50, 20);

        jLabel19.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel19.setText("frames");
        getContentPane().add(jLabel19);
        jLabel19.setBounds(210, 530, 40, 30);

        jLabel20.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel20.setText("Intensity Threshold Counts");
        getContentPane().add(jLabel20);
        jLabel20.setBounds(50, 290, 130, 20);

        dataScalingMethodComboBox.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        dataScalingMethodComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "none", "Centered-Averages", "Directional-Edge-Test" }));
        dataScalingMethodComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dataScalingMethodComboBoxActionPerformed(evt);
            }
        });
        getContentPane().add(dataScalingMethodComboBox);
        dataScalingMethodComboBox.setBounds(150, 400, 90, 20);

        jLabel21.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel21.setText("Z-step (nm)");
        getContentPane().add(jLabel21);
        jLabel21.setBounds(40, 160, 70, 14);

        zStepTextField_.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        zStepTextField_.setText("50");
        getContentPane().add(zStepTextField_);
        zStepTextField_.setBounds(170, 160, 67, 20);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        applyButton.setText("Apply");
        applyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyButtonActionPerformed(evt);
            }
        });

        saveButton.setText("Save");
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(41, 41, 41)
                .addComponent(cancelButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(applyButton)
                .addGap(5, 5, 5)
                .addComponent(saveButton)
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {applyButton, cancelButton, saveButton});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(applyButton)
                    .addComponent(saveButton)
                    .addComponent(cancelButton)))
        );

        getContentPane().add(jPanel1);
        jPanel1.setBounds(10, 590, 250, 30);

        maxTrackTravelTextField_.setText("jTextField1");
        maxTrackTravelTextField_.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                maxTrackTravelTextField_ActionPerformed(evt);
            }
        });
        getContentPane().add(maxTrackTravelTextField_);
        maxTrackTravelTextField_.setBounds(180, 560, 59, 20);

        jLabel22.setText("Max Anticipated Travel (Pixels)");
        getContentPane().add(jLabel22);
        jLabel22.setBounds(20, 560, 150, 14);

        jLabel23.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel23.setText("Box Size (pixels)");
        getContentPane().add(jLabel23);
        jLabel23.setBounds(50, 450, 90, 10);

        fitMethodComboBox2.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        fitMethodComboBox2.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Simplex", "Levenberg-Marq", "Simplex-MLE", "Levenberg-Marq-Weighted" }));
        getContentPane().add(fitMethodComboBox2);
        fitMethodComboBox2.setBounds(150, 380, 90, 20);

        jLabel24.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel24.setText("Fitter");
        getContentPane().add(jLabel24);
        jLabel24.setBounds(110, 380, 24, 14);

        boxSizeTextField.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        boxSizeTextField.setText("16");
        boxSizeTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                boxSizeTextFieldActionPerformed(evt);
            }
        });
        getContentPane().add(boxSizeTextField);
        boxSizeTextField.setBounds(170, 450, 70, 20);

        SNRthreshold.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        SNRthreshold.setText("2000");
        SNRthreshold.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                SNRthresholdFocusLost(evt);
            }
        });
        SNRthreshold.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SNRthresholdActionPerformed(evt);
            }
        });
        SNRthreshold.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                SNRthresholdPropertyChange(evt);
            }
        });
        SNRthreshold.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                SNRthresholdKeyTyped(evt);
            }
        });
        getContentPane().add(SNRthreshold);
        SNRthreshold.setBounds(180, 270, 60, 20);

        jLabel25.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel25.setText("Noise Amplitude");
        getContentPane().add(jLabel25);
        jLabel25.setBounds(50, 250, 120, 20);

        IntensityThreshold.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        IntensityThreshold.setText("2000");
        IntensityThreshold.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                IntensityThresholdFocusLost(evt);
            }
        });
        IntensityThreshold.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                IntensityThresholdActionPerformed(evt);
            }
        });
        IntensityThreshold.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                IntensityThresholdPropertyChange(evt);
            }
        });
        IntensityThreshold.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                IntensityThresholdKeyTyped(evt);
            }
        });
        getContentPane().add(IntensityThreshold);
        IntensityThreshold.setBounds(180, 290, 60, 20);

        jLabel26.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel26.setText("Signal-To-Noise-Ratio");
        getContentPane().add(jLabel26);
        jLabel26.setBounds(50, 270, 130, 20);

        jLabel18.setFont(new java.awt.Font("Lucida Grande", 0, 10)); // NOI18N
        jLabel18.setText("  < width of fit (nm) <");
        getContentPane().add(jLabel18);
        jLabel18.setBounds(70, 500, 110, 20);
        getContentPane().add(jSeparator6);
        jSeparator6.setBounds(20, 200, 220, 2);

        jLabel2.setText("Microscope Properties");
        getContentPane().add(jLabel2);
        jLabel2.setBounds(20, 10, 120, 30);

        MicroscopePropertiesButton_.setText("Set");
        MicroscopePropertiesButton_.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MicroscopePropertiesButton_ActionPerformed(evt);
            }
        });
        getContentPane().add(MicroscopePropertiesButton_);
        MicroscopePropertiesButton_.setBounds(170, 20, 70, 23);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void noiseToleranceTextField_PropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_noiseToleranceTextField_PropertyChange
       
       
    }//GEN-LAST:event_noiseToleranceTextField_PropertyChange

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
       WINDOWOPEN = false;
    }//GEN-LAST:event_formWindowClosed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
       try {
       prefs_.put(NOISETOLERANCE, noiseToleranceTextField_.getText());
       prefs_.put(INTENSITYTHRESHOLD, IntensityThreshold.getText() );
       prefs_.put( SNR, SNRthreshold.getText() );
       prefs_.putDouble(PCF, NumberUtils.displayStringToDouble(photonConversionTextField.getText()));
       prefs_.putDouble(GAIN, NumberUtils.displayStringToDouble(emGainTextField_.getText()));
       prefs_.putDouble(PIXELSIZE, NumberUtils.displayStringToDouble(pixelSizeTextField_.getText()));      
       prefs_.putDouble(TIMEINTERVALMS, NumberUtils.displayStringToDouble(timeIntervalTextField_.getText()));
       prefs_.putDouble(ZSTEPSIZE, NumberUtils.displayStringToDouble(zStepTextField_.getText()));
       prefs_.putDouble(BACKGROUNDLEVEL, NumberUtils.displayStringToDouble(baseLevelTextField.getText()));
       prefs_.putBoolean(USEFILTER, false);
       prefs_.putDouble(WIDTHMIN, NumberUtils.displayStringToDouble(minWidthTextField.getText()) );
       prefs_.putDouble(WIDTHMAX, NumberUtils.displayStringToDouble(maxWidthTextField.getText()) );
       //prefs_.putBoolean(USENRPHOTONSFILTER, filterDataCheckBoxNrPhotons.isSelected());
       prefs_.putDouble(NRPHOTONSMIN, NumberUtils.displayStringToDouble(minWidthTextField.getText()));
       prefs_.putDouble(NRPHOTONSMAX, NumberUtils.displayStringToDouble(maxWidthTextField.getText()));
       prefs_.putInt(MAXITERATIONS, NumberUtils.displayStringToInt(maxIterationsTextField.getText()));
       prefs_.putInt(BOXSIZE, NumberUtils.displayStringToInt(boxSizeTextField.getText()));
       prefs_.putInt(PREFILTER, preFilterComboBox_.getSelectedIndex());
       prefs_.putInt(FRAMEXPOS, getX());
       prefs_.putInt(FRAMEYPOS, getY());
       prefs_.putInt(FRAMEWIDTH, getWidth());
       prefs_.putInt(FRAMEHEIGHT, this.getHeight());
       prefs_.putBoolean(ENDTRACKBOOL, endTrackCheckBox_.isSelected() );
       prefs_.putInt(ENDTRACKINT, (Integer) endTrackSpinner_.getValue() );
       prefs_.putInt(FITMODE, dataScalingMethodComboBox.getSelectedIndex() );
       prefs_.putInt(MAXTRACKTRAVEL, maxTrackTravel_ );
       } catch (ParseException ex) {
          ReportingUtils.logError(ex, "Error while closing Localization Microscopy plugin");
       }
       
       WINDOWOPEN = false;
       
       this.setVisible(false);
    }//GEN-LAST:event_formWindowClosing

    public void formWindowOpened() {
       WINDOWOPEN = true;
    }
    
   @Override
    public void dispose() {
       formWindowClosing(null);
    }

    private void preFilterComboBox_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_preFilterComboBox_ActionPerformed
       String item = (String) preFilterComboBox_.getSelectedItem();
       if (item.equals("None"))
          preFilterType_ = FindLocalMaxima.FilterType.NONE;
       if (item.equals("Gaussian1-5"))
          preFilterType_ = FindLocalMaxima.FilterType.GAUSSIAN1_5;
    }//GEN-LAST:event_preFilterComboBox_ActionPerformed

    private void fitDimensionsComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fitDimensionsComboBox1ActionPerformed
       shape_ = fitDimensionsComboBox1.getSelectedIndex() + 1;
    }//GEN-LAST:event_fitDimensionsComboBox1ActionPerformed

    private void baseLevelTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_baseLevelTextFieldActionPerformed
    }//GEN-LAST:event_baseLevelTextFieldActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        
       updateValues( gaussianSettingsRef_ );
       dispose();
       
    }//GEN-LAST:event_saveButtonActionPerformed

   private void endTrackCheckBox_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_endTrackCheckBox_ActionPerformed
      // TODO add your handling code here:
   }//GEN-LAST:event_endTrackCheckBox_ActionPerformed

   private void noiseToleranceTextField_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_noiseToleranceTextField_ActionPerformed
      //if (showOverlay_.isSelected())
      //   showNoiseTolerance();
   }//GEN-LAST:event_noiseToleranceTextField_ActionPerformed

   
   //Shows the Currently Found Gaussians for A Given Operation
   private void showNoiseTolerance() {
       ImagePlus siPlus;
       try {
           //Change This
          siPlus = IJ.getImage();
       } catch (Exception e) {
          return;
       }
       if (ip_ != siPlus)
          ip_ = siPlus;

       // Roi originalRoi = siPlus.getRoi();
       // Find maximum in Roi, might not be needed....
      try {
         int val = Integer.parseInt(noiseToleranceTextField_.getText());
         int halfSize = Integer.parseInt(boxSizeTextField.getText());
         int thresholdLevel =(int) NumberUtils.displayStringToInt(IntensityThreshold.getText());
         Polygon pol = FindLocalMaxima.FindMax(siPlus, halfSize, 6, val, thresholdLevel, preFilterType_);
         // pol = FindLocalMaxima.noiseFilter(siPlus.getProcessor(), pol, val);
         Overlay ov = new Overlay();
         for (int i = 0; i < pol.npoints; i++) {
            int x = pol.xpoints[i];
            int y = pol.ypoints[i];
            ov.add(new Roi(x - halfSize, y - halfSize, 2 * halfSize, 2 * halfSize));
         }
         siPlus.setOverlay(ov);
         siPlus.setHideOverlay(false);
      } catch (NumberFormatException nfEx) {
         // nothing to do
      } catch (ParseException ex) {
           Logger.getLogger(ModifiedLocalizationParamForm.class.getName()).log(Level.SEVERE, null, ex);
       }
   }

   private void noiseToleranceTextField_FocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_noiseToleranceTextField_FocusLost
   }//GEN-LAST:event_noiseToleranceTextField_FocusLost

   private void noiseToleranceTextField_KeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_noiseToleranceTextField_KeyTyped
   }//GEN-LAST:event_noiseToleranceTextField_KeyTyped

   private void pixelSizeTextField_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pixelSizeTextField_ActionPerformed
      // delete
   }//GEN-LAST:event_pixelSizeTextField_ActionPerformed

    private void applyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_applyButtonActionPerformed
        updateValues( gaussianSettingsRef_ );
    }//GEN-LAST:event_applyButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
       dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void maxTrackTravelTextField_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_maxTrackTravelTextField_ActionPerformed
        int temp = Integer.parseInt( maxTrackTravelTextField_.getText() );
        if( temp == 0 ) {
            //Reset to old on 0 width
            maxTrackTravelTextField_.setText( Integer.toString( maxTrackTravel_ ) );
            return;
        }
        if( temp < 0 ) {
            //make it an abs value
            temp = (int) Math.abs(temp);
            maxTrackTravelTextField_.setText( Integer.toString(temp) );
        }
        
        maxTrackTravel_ = temp;
    }//GEN-LAST:event_maxTrackTravelTextField_ActionPerformed

    private void dataScalingMethodComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dataScalingMethodComboBoxActionPerformed
       String scalingMethod = (String) dataScalingMethodComboBox.getSelectedItem();
       if( scalingMethod.equals( "Centered-Averages" ) ) {
           dataEnsureMode_ = GenericBaseGaussianFitThread.DataEnsureMode.averageCentered;
       } else if( scalingMethod.equals( "Directional-Edge-Test" ) ) {
           dataEnsureMode_ = GenericBaseGaussianFitThread.DataEnsureMode.directionalCentering;
       } else {
           dataEnsureMode_ = GenericBaseGaussianFitThread.DataEnsureMode.none;
       }
    }//GEN-LAST:event_dataScalingMethodComboBoxActionPerformed

    private void boxSizeTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_boxSizeTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_boxSizeTextFieldActionPerformed

    private void SNRthresholdFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_SNRthresholdFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_SNRthresholdFocusLost

    private void SNRthresholdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SNRthresholdActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_SNRthresholdActionPerformed

    private void SNRthresholdPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_SNRthresholdPropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_SNRthresholdPropertyChange

    private void SNRthresholdKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_SNRthresholdKeyTyped
        // TODO add your handling code here:
    }//GEN-LAST:event_SNRthresholdKeyTyped

    private void IntensityThresholdFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_IntensityThresholdFocusLost
        // TODO add your handling code here:
    }//GEN-LAST:event_IntensityThresholdFocusLost

    private void IntensityThresholdActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_IntensityThresholdActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_IntensityThresholdActionPerformed

    private void IntensityThresholdPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_IntensityThresholdPropertyChange
        // TODO add your handling code here:
    }//GEN-LAST:event_IntensityThresholdPropertyChange

    private void IntensityThresholdKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_IntensityThresholdKeyTyped
        // TODO add your handling code here:
    }//GEN-LAST:event_IntensityThresholdKeyTyped

    private void maxWidthTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_maxWidthTextFieldActionPerformed
        double tempMax = Double.parseDouble(maxWidthTextField.getText());
        if ( tempMax <= minWidthnm_ ) {
            maxWidthTextField.setText( Double.toString( maxWidthnm_));
        } else {
            maxWidthnm_ = tempMax;
        }
    }//GEN-LAST:event_maxWidthTextFieldActionPerformed

    private void minWidthTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_minWidthTextFieldActionPerformed
        
        double tempMin = Double.parseDouble(minWidthTextField.getText());
        if ( tempMin >= maxWidthnm_ ) {
            minWidthTextField.setText( Double.toString( minWidthnm_));
        } else if (tempMin < 0) {
            minWidthnm_ = Math.abs( tempMin );
            minWidthTextField.setText( Double.toString( minWidthnm_));
        } else {
            minWidthnm_ = tempMin;
        }
    }//GEN-LAST:event_minWidthTextFieldActionPerformed

    private void MicroscopePropertiesButton_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MicroscopePropertiesButton_ActionPerformed
        gaussFitParamModel_.getCurrentMicroscopeModel().enableAllListenerGUIs(true);
        
    }//GEN-LAST:event_MicroscopePropertiesButton_ActionPerformed

   //Update A Given GaussianInfoReference
   private void updateValues(ExtendedGaussianInfo tT) {

      try {
         tT.setNoiseTolerance(Integer.parseInt(noiseToleranceTextField_.getText()));
         tT.setPhotonConversionFactor(NumberUtils.displayStringToDouble(photonConversionTextField.getText()));
         tT.setGain(NumberUtils.displayStringToDouble(emGainTextField_.getText()));
         tT.setPixelSize((float) NumberUtils.displayStringToDouble(pixelSizeTextField_.getText()));
         tT.setZStackStepSize((float) NumberUtils.displayStringToDouble(zStepTextField_.getText()));
         tT.setTimeIntervalMs(NumberUtils.displayStringToDouble(timeIntervalTextField_.getText()));
         tT.setBaseLevel(NumberUtils.displayStringToDouble(baseLevelTextField.getText()));
         tT.setUseWidthFilter( false) ;//filterDataCheckBoxWidth.isSelected());
         tT.setWidthMin( Double.parseDouble( minWidthTextField.getText()) );
         tT.setWidthMax( Double.parseDouble( maxWidthTextField.getText()) );
         tT.setUseNrPhotonsFilter(false);
         tT.setNrPhotonsMin(NumberUtils.displayStringToDouble(minWidthTextField.getText()));
         tT.setNrPhotonsMax(NumberUtils.displayStringToDouble(maxWidthTextField.getText()));
         tT.setMaxIterations(Integer.parseInt(maxIterationsTextField.getText()));
         tT.setBoxSize(Integer.parseInt(boxSizeTextField.getText()) );  //AbbeLimit (Airy Radius) is Only half the box
         tT.setShape(fitDimensionsComboBox1.getSelectedIndex() + 1);
         tT.setFitMode(dataScalingMethodComboBox.getSelectedIndex() + 1);
         tT.setEndTrackBool(endTrackCheckBox_.isSelected());
         tT.setEndTrackAfterNFrames((Integer) endTrackSpinner_.getValue());
         tT.setPrefilterType( preFilterType_ );
         tT.setDataEnsureMode( dataEnsureMode_ );
         tT.setIntensityThreshold(NumberUtils.displayStringToInt( IntensityThreshold.getText() ));
         tT.setSNR( NumberUtils.displayStringToDouble(SNRthreshold.getText()) );
         tT.setMaxTrackTravel( maxTrackTravel_ );
      } catch (NumberFormatException ex) {
         JOptionPane.showMessageDialog(null, "Error interpreting input: " + ex.getMessage());
      } catch (ParseException ex) {
         JOptionPane.showMessageDialog(null, "Error interpreting input: " + ex.getMessage());
      }       
  
      int temp = Integer.parseInt( boxSizeTextField.getText() );
      gaussFitParamModel_.updateProcessorModelSettings( tT );
           
   }
   
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField IntensityThreshold;
    private javax.swing.JButton MicroscopePropertiesButton_;
    private javax.swing.JTextField SNRthreshold;
    private javax.swing.JButton applyButton;
    private javax.swing.JTextField baseLevelTextField;
    private javax.swing.JTextField boxSizeTextField;
    private javax.swing.JButton cancelButton;
    private javax.swing.JComboBox dataScalingMethodComboBox;
    private javax.swing.JTextField emGainTextField_;
    private javax.swing.JCheckBox endTrackCheckBox_;
    private javax.swing.JSpinner endTrackSpinner_;
    private javax.swing.JComboBox fitDimensionsComboBox1;
    private javax.swing.JComboBox fitMethodComboBox2;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JTextField maxIterationsTextField;
    private javax.swing.JTextField maxTrackTravelTextField_;
    private javax.swing.JTextField maxWidthTextField;
    private javax.swing.JTextField minWidthTextField;
    private javax.swing.JTextField noiseToleranceTextField_;
    private javax.swing.JTextField photonConversionTextField;
    private javax.swing.JTextField pixelSizeTextField_;
    private javax.swing.JComboBox preFilterComboBox_;
    private javax.swing.JButton saveButton;
    private javax.swing.JTextField timeIntervalTextField_;
    private javax.swing.JTextField zStepTextField_;
    // End of variables declaration//GEN-END:variables

}
