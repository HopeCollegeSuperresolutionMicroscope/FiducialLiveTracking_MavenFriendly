/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.views;

import edu.hope.superresolution.Utils.IJMMReportingUtils;
import edu.hope.superresolution.livetrack.LiveTracking;
import edu.hope.superresolution.models.FiducialArea;
import edu.hope.superresolution.models.ModelUpdateListener;
import edu.hope.superresolution.models.FiducialLocationModel;
import edu.hope.superresolution.models.LocationAcquisitionModel;
import edu.hope.superresolution.models.ModelUpdateDispatcher;
import ij.gui.ImageWindow;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowListener;
import java.util.HashSet;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;
import org.micromanager.api.ScriptInterface;
import org.micromanager.utils.ReportingUtils;

/**
 *  Combination View/Controller For Interaction with a Given FiducialLocationModel
 * <p>
 *  Minimum Method Call Sequence: Constructor(), registerModelUpdateListener()
 * 
 * @author Microscope
 */
public class FiducialSelectForm extends javax.swing.JFrame implements ModelUpdateListener {

    private boolean WINDOW_OPEN = true;
    
    private final LiveTracking pluginInstance_;
    private FiducialLocationModel fLocationModel_ = null;  //Reference to fiducialLocationModel
    private LocationAcquisitionModel.AcquisitionSubmitAction subAction_;
    
    private DefaultListModel fiducialList_ = new DefaultListModel();
    private HashSet<Integer> selectedFiducials_ = new HashSet<Integer>();
    private Integer curEditIdx_ = -1;  //Current Index Being Edited (Since It's a single Selection Thing
    private ImageWindow selectWindow_;
    private int minimumNumFidForTracks_;
    private boolean refreshClicked_ = false;
    
    private JFrame paramForm_;
    
    private boolean showAllButtonOn_ = false;
    
    private fAreaDisplayBoxRadioHandler displayBoxRadioButtonListener_ = new fAreaDisplayBoxRadioHandler();
    
    //Base String to append Index values to
    String LIST_FIDUCIAL_BASE_STR = "Fiducial ";

    /**
     * Callback that vets any registrations that are not from the expected models.
     * Only registers a new FiducialLocation Model after the onUnregisteredCallback is called
     * through an unregistering of the previous FiducialLocationModel().
     * 
     * @param registeredModel 
     */
    @Override
    public void onRegisteredToModel(ModelUpdateDispatcher registeredModel) {
        //Make Sure the locationModel was set to null (in unregisterCallback) and the model matches if we set it
        if( fLocationModel_ == null && registeredModel instanceof FiducialLocationModel ) {
            fLocationModel_ = (FiducialLocationModel) registeredModel;
        }
    }

    /**
     * Callback that unregisters the calling model if already registered as fLocationModel_.
     * Sets fLocationModel_ to null.
     * 
     * @param unregisteredModel 
     */
    @Override
    public void onUnregisteredToModel(ModelUpdateDispatcher unregisteredModel) {
        if( fLocationModel_ != null && unregisteredModel == fLocationModel_ ) {
            fLocationModel_ = null;
        }
    }
        
    /**
     * Inner Class Handler for use with Radio Buttons to display selection boxes
     */
    public class fAreaDisplayBoxRadioHandler implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            JRadioButton but =  (JRadioButton) e.getItem();
            //check for null due to normal initialization in constructor
            if( but.isSelected() && fLocationModel_ != null ) {
                fLocationModel_.setDisplayBoxMode( but.getActionCommand() );
            }
        }
        
    }
       
    @Override
    public void guiEnable(boolean enable) {
        this.setVisible(true);
    }
    
    /**
     * Helper Class For List Elements, their correspondence to a listmodel for Fiducial Areas,
     * and their labels.
     */
    private class FiducialListElement {
        
        int idx_;
        String nameStr_;
        String base_;
       
        /**
         *  Creates a FiducialListElement with the default label (LIST_FIDUCIAL_BASE_STR)
         * 
         * @param idx - 0 based index of SelectionModel List
         * @see #LIST_FIDUCIAL_BASE_STR
         */
        FiducialListElement( int idx ) {

            base_ = LIST_FIDUCIAL_BASE_STR;
            idx_ = idx;
            setNameStr( );
        }
        
        /**
         * Creates a FiducialListElement with the label specified by baseNameStr with addition of idx + 1
         * 
         * @param baseNameStr - the start of the baseNameStr
         * @param idx - 0 based index of SelectionModel List (added to baseNameStr)
         */
        FiducialListElement( String baseNameStr, int idx) {
        
            base_ = baseNameStr;
            idx_ = idx;
            setNameStr( );
            //nameStr_ = baseNameStr + idx;

        }
        
        /**
         * Creates the full label of element ( base name + 1 based idx)
         */
        private void setNameStr( ) {
            nameStr_ = base_ + (int) (idx_ + 1);
        }
        
        /**
         *   Sets the list idx of the element and it's corresponding nameStr for the index
         * 
         * @param idx - the index (0 based) in the list model 
         * @see #setNameStr() 
         */
        public void setIdx( int idx ) {
            idx_ = idx;
            setNameStr( );
        }
        
        /**
         *  Gets the index of this current instance in correspondance to the list model
         * 
         * @return 
         */
        public int getIdx() {
            return idx_;
        }
        
        @Override
        public String toString() {
            return nameStr_;
        }
    }
    
    /**
     * Creates new form FiducialSelectForm.  For Versatility, the trackAction Paramaeter
     * is the Action Listener for the Track Button on the form.  May be customized based on
     * app status.
     * 
     * @param paramForm - The Parameter Form for Fitting that is used to locate Fiducials and is accessed through fittingParamBut
     * @param trackAction - The action to be implemented upon clicking the trackAction Button
     * @param selectWindow - the Window that may/or may not change it's imagePlus for use with selection
     * @param pluginInstance - The Instance of the LiveTracking Plugin That may be canceled on exit or cancel
     */
    public FiducialSelectForm( /*FiducialLocationModel fLocationModel,*/ JFrame paramForm,
                                LocationAcquisitionModel.AcquisitionSubmitAction trackAction,
                                ImageWindow selectWindow, LiveTracking pluginInstance ) {
        
        pluginInstance_ = pluginInstance;
        //fLocationModel_ = fLocationModel;
        selectWindow_ = selectWindow;
        
        paramForm_ = paramForm;
        subAction_ = trackAction;
        
        initComponents();
        
        minimumNumFidForTracks_ = Integer.parseInt( minFiducialTrackNumTextField_.getText() );
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jRadioButton1 = new javax.swing.JRadioButton();
        jButton1 = new javax.swing.JButton();
        buttonGroup1 = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        fittingParamBut = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        //JList of Object
        fiducialAreaList_ = new javax.swing.JList( fiducialList_ );
        addFiducialBut = new javax.swing.JButton();
        removeFiducialBut = new javax.swing.JButton();
        jToggleButton1 = new javax.swing.JToggleButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jRadioButton3 = new javax.swing.JRadioButton();
        jPanel3 = new javax.swing.JPanel();
        minFiducialTrackNumTextField_ = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        refreshButton_ = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        CancelButton_ = new javax.swing.JButton();

        jRadioButton1.setText("jRadioButton1");

        jButton1.setText("Show All");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel1.setText("Select Fiducials");

        fittingParamBut.setText("Fitting Parameters");
        fittingParamBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fittingParamButActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(fittingParamBut))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(fittingParamBut))
                .addContainerGap())
        );

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel2.setText("Fiducial List");

        fiducialAreaList_.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        fiducialAreaList_.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        fiducialAreaList_.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        fiducialAreaList_.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                fiducialAreaList_ValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(fiducialAreaList_);

        addFiducialBut.setText("Add");
        addFiducialBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addFiducialButActionPerformed(evt);
            }
        });

        removeFiducialBut.setText("Remove");
        removeFiducialBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeFiducialButActionPerformed(evt);
            }
        });

        jToggleButton1.setText("Show All");
        jToggleButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButton1ActionPerformed(evt);
            }
        });

        buttonGroup1.add(jRadioButton2);
        jRadioButton2.setText("Select Box");
        jRadioButton2.setActionCommand( FiducialLocationModel.SELECT_BOX_DISPLAY );
        jRadioButton2.addItemListener( displayBoxRadioButtonListener_ );
        jRadioButton2.setSelected(true);

        buttonGroup1.add(jRadioButton3);
        jRadioButton3.setText("Track Box");
        jRadioButton3.setActionCommand( FiducialLocationModel.TRACK_BOX_DISPLAY );
        jRadioButton3.addItemListener(displayBoxRadioButtonListener_);
        jRadioButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton3ActionPerformed(evt);
            }
        });

        minFiducialTrackNumTextField_.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        minFiducialTrackNumTextField_.setText("3");
        minFiducialTrackNumTextField_.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                minFiducialTrackNumTextField_ActionPerformed(evt);
            }
        });

        jLabel3.setText("Minimum Number of Fiducials For Track Analysis:");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(minFiducialTrackNumTextField_, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(minFiducialTrackNumTextField_, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        refreshButton_.setText("Refresh");
        refreshButton_.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButton_ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 285, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(addFiducialBut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(removeFiducialBut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jToggleButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jRadioButton2)
                                    .addComponent(jRadioButton3)
                                    .addComponent(refreshButton_)))))
                    .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        jPanel2Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addFiducialBut, jToggleButton1, removeFiducialBut});

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(addFiducialBut)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(removeFiducialBut)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(refreshButton_)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jRadioButton3)
                        .addGap(3, 3, 3)
                        .addComponent(jToggleButton1)
                        .addGap(27, 27, 27))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jButton4.setText("Track Acquisition");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        CancelButton_.setText("Cancel");
        CancelButton_.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CancelButton_ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(CancelButton_)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton4))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton4)
                    .addComponent(CancelButton_))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void addFiducialButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFiducialButActionPerformed
        //Modify List Model
        addModelFiducialElement();
       
    }//GEN-LAST:event_addFiducialButActionPerformed

    /**
     * When the list Selection Model changes selection states.  Used to Call Removals
     * 
     * @param evt 
     */
    private void fiducialAreaList_ValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_fiducialAreaList_ValueChanged

        //We are only interested in final valueChanges currently
        if( evt.getValueIsAdjusting() == false ) {
            //Get The SelectionModel
            ListSelectionModel lsm = fiducialAreaList_.getSelectionModel();

            //General Selection Procedure in case we swith to multiple selection later
            int min = lsm.getMinSelectionIndex();
            int max = lsm.getMaxSelectionIndex();

            //boolean since there's no guarantee of which index comes first
            boolean prevRoiCleared = false;
            
            for (int i = min; i <= max; i++) {
                //If selected and was not already selected
                if ( lsm.isSelectedIndex(i)  /*&& !selectedFiducials_.contains(i) */ ) {
            
                    //Display ROIs
                    //Enable Editing an area only if there's one
                    if (min == max ) {
                        //Select the new FiducialArea
                       fLocationModel_.setSelectedFiducialArea(i);
                    }
                    else {  //Display All Other Selected Elements
                        //For Later, Multi-Selection                  
                    }
                }
                else if ( selectedFiducials_.contains(i) && prevRoiCleared ) {
                    //Store the ROI as a value to be manipulated later and then clear it
                    //Should do a DeSelect Operation Once we're using Multiple
                    selectedFiducials_.remove(i);
                }
            }    
        }

    }//GEN-LAST:event_fiducialAreaList_ValueChanged

    /**
     * When the remove button is clicked, removes the selected FiducialArea from 
     *  list model.
     * 
     * @param evt 
     */
    private void removeFiducialButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeFiducialButActionPerformed

        ListSelectionModel lsm = fiducialAreaList_.getSelectionModel();
        
        int min = lsm.getMinSelectionIndex();
        int max = lsm.getMaxSelectionIndex();
        
        for( int i = min; i <= max; i++ ) {
            if( lsm.isSelectedIndex(i) ) {
                removeModelFiducialElement(i);
                selectedFiducials_.remove(i);
            }
        }
        //Clear All Selections Listeners should be aware of this and accoutn for it
        lsm.clearSelection();

    }//GEN-LAST:event_removeFiducialButActionPerformed

    /**
     * When the Fitting Parameters Button is Clicked, Shows the Form.
     * 
     * @param evt 
     */
    private void fittingParamButActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fittingParamButActionPerformed
        // TODO add your handling code here:
        
        //Should Check to See if there is a MainForm Already Created (top-level)
        if ( paramForm_.isVisible() == false ) {
            paramForm_.setVisible(true);
        }
    }//GEN-LAST:event_fittingParamButActionPerformed

    /**
     * When the Track Acquisition Button is Clicked, Performs the trackAction and 
     * extra validation or changes
     * 
     * @param evt 
     */
    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        if( fiducialList_.size() < 3 ) {
            IJMMReportingUtils.showMessage( "You must Select at least 3 Fiducials!");
            return;
        }
        fLocationModel_.setMinNumFiducialTracks( minimumNumFidForTracks_ );
        
        //MinimumNumFidForTracks_
        //Hide this and do any response
        //setVisible(false);
        subAction_.submitResponse();
                
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
     *  When the Toggle Button is clicked to show all Fiducial Areas or just the currently selected one
     * 
     * @param evt 
     */
    private void jToggleButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton1ActionPerformed
        showAllButtonOn_ = !(showAllButtonOn_);
        fLocationModel_.enableShowAll( showAllButtonOn_ );
    }//GEN-LAST:event_jToggleButton1ActionPerformed

    /**
     * When the Minimum Fiducial Track Number Text field is edited, performs verification 
     * and storage of the value that is validated.
     * 
     * @param evt 
     */
    private void minFiducialTrackNumTextField_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_minFiducialTrackNumTextField_ActionPerformed
        int temp = Integer.parseInt( minFiducialTrackNumTextField_.getText() );
        if( temp < 1 ) {
            temp = 1;
            minFiducialTrackNumTextField_.setText( Integer.toString(temp) );
        } else if( temp > fiducialList_.size() ) {        
            temp = fiducialList_.size();
            minFiducialTrackNumTextField_.setText( Integer.toString(temp) );
        }
        
        minimumNumFidForTracks_ = temp;
        
    }//GEN-LAST:event_minFiducialTrackNumTextField_ActionPerformed

    /**
     *  Refreshes All Fiducial Areas (Including New Fits) by adding the current ImagePlus if it is new
     * 
     * @param evt - Button was clicked
     */
    private void refreshButton_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_refreshButton_ActionPerformed
        refreshClicked_ = true;    
        refreshClicked_ = fLocationModel_.updateImagePlus( selectWindow_.getImagePlus() );  
    }//GEN-LAST:event_refreshButton_ActionPerformed

    private void CancelButton_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CancelButton_ActionPerformed
        dispose();
    }//GEN-LAST:event_CancelButton_ActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
       WINDOW_OPEN = false;
    }//GEN-LAST:event_formWindowClosed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        if( WINDOW_OPEN ) {
            WINDOW_OPEN = false;
            this.setVisible( false );
            //To Be changed later, but assumes other instances are passed
            if( pluginInstance_ != null ) {
                pluginInstance_.dispose();
            }
        }
    }//GEN-LAST:event_formWindowClosing

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
       WINDOW_OPEN = true;
    }//GEN-LAST:event_formWindowOpened

    private void jRadioButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton3ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jRadioButton3ActionPerformed

    /**
     * Add A FiducialArea to the LocationModel. Further interactions occur as response 
     * to a broadcast of an addition.
     * 
     * @see #onModelFiducialElementAdd() 
     */
    private void addModelFiducialElement() {
        fLocationModel_.addFiducialArea();
    }
    
    /**
     * Event Response:  FiducialLocationModel Fiducial Area was Added
     */
    private void onModelFiducialElementAdd() {
        final List< FiducialArea > list = fLocationModel_.getFiducialAreaList();
        refreshList();
        setFiducialSelection( list.size() - 1 );
    }
    
    /**
     * Event Response:  FiducialLocationModel broadcasts a Fiducial Area Was Selected
     */
    private void onModelSelectFiducialElement() {
        curEditIdx_ = fLocationModel_.getSelectedFiducialAreaIndex();
        setFiducialSelection( curEditIdx_ );
    }
    
     /**
     *  Removes an element from the corresponding FiducialLocationModel.  Will Handle
     *   the listmodel as a response through onModelFiducialElementRemove
     * 
     * @see #update(int) 
     * @see #onModelFiducialElementRemove() 
     */
    private void removeModelFiducialElement( int idx ) {
        fLocationModel_.removeFiducialArea( idx );
    }
    
    /**
     * Event Response:  FiducialLocationModel broadcasts an element removed
     */
    private void onModelFiducialElementRemove( ) {
       refreshList();
    }
    
    /**
     *  Response To A Broadcast from the FiducialLocationModel that A Fiducial Area has been selected.
     *   This only performs a response on refresh, by reevaluating and removing FiducialAreas from the
     *   listmodel that have no selectedFiducials due to a change.
     */
    private void onFiducialAreaDataChanged() {
        if (refreshClicked_) {
            ij.IJ.log("In Data Change");
            List<FiducialArea> fAreas = fLocationModel_.getFiducialAreaList();
            int count = 0;
            for (int i = 0; i < fAreas.size(); ++i) {
                FiducialArea fArea = fAreas.get(i);
                if (fArea.getAllRawPossibleSpots().size() == 0) {
                    removeModelFiducialElement(i);
                    selectedFiducials_.remove(i);
                    count++;
                }
            }
            if (count > 0) {
                IJMMReportingUtils.showMessage(count + " Fiducial Areas removed for no spots!");
            }
            refreshClicked_ = false;
        }
    }
    
    /**
     *  Refreshes the entire list and its holdings based on the fLocationModel
     *    Since this is just a semantic value, just update the number
     * 
     */
    private void refreshList(  ) {
        List<FiducialArea> list = fLocationModel_.getFiducialAreaList();
        int diff = fiducialList_.size() - list.size();
        if( diff >= 0 ) {
            for (; diff > 0; --diff) {
                //Remove from end, to guarantee order
                fiducialList_.removeElementAt(fiducialList_.size() - 1);
            }
        }
        else {
            for( int i = 0; i < diff*-1; i++ ) {
                fiducialList_.addElement( new FiducialListElement( fiducialList_.size() + i ) );
            }
        }
        
    }

    /**
     *  Sets A Fiducial Area as Selected based on its idx.
     * <p>
     * Note:  Fiducial Area List in LocationModel and the list model of this class are considered to share indices
     * 
     * @param idx 
     */
    private void setFiducialSelection( final Integer idx ) {
        
        ListSelectionModel lsm = fiducialAreaList_.getSelectionModel();
        lsm.setSelectionInterval(idx, idx);
        //Add to List of selected Fiducials
        selectedFiducials_.add(idx);
    }
    
    /**
     *  Model Update Listener Function- Called when FiducialLocationModel this is registered to
     *  broadcasts an update.
     * 
     * @param event
     */       
    @Override
    public void update(ModelUpdateDispatcher caller, int event) {
        if (caller instanceof FiducialLocationModel) {
            switch (event) {
                case FiducialLocationModel.EVENT_ELEMENT_ADDED:
                    onModelFiducialElementAdd();
                    break;
                case FiducialLocationModel.EVENT_ELEMENT_REMOVED:
                    onModelFiducialElementRemove();
                    break;
                case FiducialLocationModel.EVENT_ELEMENT_SELECTED:
                    onModelSelectFiducialElement();
                    break;
                case FiducialLocationModel.EVENT_FIDUCIAL_AREA_DATA_CHANGED:
                    onFiducialAreaDataChanged();
                    break;
                case FiducialLocationModel.EVENT_ELEMENT_SET:  //Setting an Element's Properties has no effect
                    break;
            }
        }
    }  
    
    @Override
    public void dispose() {
        formWindowClosing( null );
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton CancelButton_;
    private javax.swing.JButton addFiducialBut;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JList fiducialAreaList_;
    private javax.swing.JButton fittingParamBut;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton4;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToggleButton jToggleButton1;
    private javax.swing.JTextField minFiducialTrackNumTextField_;
    private javax.swing.JButton refreshButton_;
    private javax.swing.JButton removeFiducialBut;
    // End of variables declaration//GEN-END:variables
}
