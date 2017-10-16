/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.views;

import com.google.common.eventbus.Subscribe;
import edu.hope.superresolution.ImageJmodifieds.ImPlusListenerDockablePanel;
import edu.hope.superresolution.ImageJmodifieds.PropertyNotifyingImagePlus;
import edu.hope.superresolution.ImageJmodifieds.TwoSliceSelector;
import edu.hope.superresolution.models.FiducialLocationModel;
import edu.hope.superresolution.models.LocationAcquisitionModel;
import edu.hope.superresolution.models.ModelUpdateDispatcher;
import edu.hope.superresolution.models.ModelUpdateListener;
import java.awt.Button;
import java.awt.Color;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import net.miginfocom.swing.MigLayout;
import org.micromanager.api.IAcquisitionEngine2010;

/**
 *  An awt.Panel for Insertion into an ImageWindow that displays the Data from the corresponding 
 *  1-based index for Fiducial Models from a LocationAcquisitionModel
 * <p>
 * TODO: Adapt LocationAcquisitionModel to dispatch updates and register this view to it.
 * 
 * @author Microscope
 */
 public class FiducialModelFocusEdgesSelection extends ImPlusListenerDockablePanel {

    private PropertyNotifyingImagePlus curListener_ = null;
    private final LocationAcquisitionModel locAcq_;
    private int currentSlice_ = 0;
    private int inFocusSlice_ = 1;
    private int outFocusSlice_ = 2;

    /**
     * General Constructor
     * @param locAcq - The LocationAcquisition whose data will be displayed.
     * @param submitAction - The Action from the calling context that will be linked to the submit button.
     *                        Currently expected to modify the properties of the Autofocus for which this was designed.
     */
    public FiducialModelFocusEdgesSelection(/*final LocationAcquisitionModel locAcq,*/ final TwoSliceSelector submitAction) {

        setLayout(new MigLayout("insets 1, fill", "[grow,fill][][]", "[][][]"));

        //The in focus Selection
        inFocusSliceLabel_.setText("Set This Slice To Most In Focus: ");
        add(inFocusSliceLabel_, "align right");
        //Just set the first SliceNumber_ to the first slice
        inFocusSliceNumber_.setText(Integer.toString(inFocusSlice_));
        add(inFocusSliceNumber_, "align right");
        inFocusButton_.setLabel("Set");
        ActionListener inFocusButtonAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                inFocusSlice_ = currentSlice_;
                inFocusSliceNumber_.setText(Integer.toString(inFocusSlice_));
                if (inFocusSlice_ != outFocusSlice_) {
                    noError();
                } else {
                    sameOutAndInSelection();
                }

            }
        };
        inFocusButton_.addActionListener(inFocusButtonAction);
        add(inFocusButton_, "align center, wrap");

        //The out of focus Selection
        outFocusSliceLabel_.setText("Set This Slice To Least Out of Focus: ");
        add(outFocusSliceLabel_, "align right");
        //Just set the first SliceNumber_ to the first slice
        outFocusSliceNumber_.setText(Integer.toString(outFocusSlice_));
        add(outFocusSliceNumber_, "align right");
        outFocusButton_.setLabel("Set");
        ActionListener outFocusButtonAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outFocusSlice_ = currentSlice_;
                outFocusSliceNumber_.setText(Integer.toString(outFocusSlice_));
                if (outFocusSlice_ != inFocusSlice_) {
                    noError();
                } else {
                    sameOutAndInSelection();
                }

            }
        };
        outFocusButton_.addActionListener(outFocusButtonAction);
        add(outFocusButton_, "align center, wrap");
        //Add a Notification Label
        Notify_.setText("");
        Notify_.setForeground(Color.red);
        add(Notify_, "span 2, align center");
        //Submission Button
        submitButton_.setLabel("OK");
        submitButton_.setEnabled(true);
        ActionListener submitButAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Get Both Indices and pass back their focus information
                submitAction.setSlices(inFocusSlice_, outFocusSlice_);
            }

        };
        submitButton_.addActionListener(submitButAction);
        add(submitButton_, "align center");

    }

    //ImPlusListenerDockablePanel Overrides and Subscriber Methods
    @Override
    public void registerToPropertyNotifyingImagePlus(PropertyNotifyingImagePlus propImPlus) {
        propImPlus.RegisterToEventBus(this);
        curListener_ = propImPlus;
    }

    @Override
    public void unregisterFromPropertyNotifyingImagePlus(PropertyNotifyingImagePlus propImPlus) {
        propImPlus.UnregisterFromEventBus(this);
        curListener_ = propImPlus;
    }

    /**
     * On any slice change from the PropertyNotifyingImagePlus of the window,
     * correlate the Slice to a FiducialLocationModel
     */
    @Subscribe
    public void UpdateModelDetails(PropertyNotifyingImagePlus.StackPositionChangedEvent evt) {
        System.out.println("Recieiving Event with CurrentSlice as " + evt.getCurrentSlice());
        if (evt.sameOriginator(curListener_)) {
            //Compensate for 1-base Nature of slices
            if (evt.getCurrentSlice() != currentSlice_) {
                currentSlice_ = evt.getCurrentSlice();
            }

        }
    }

    @Override
    public void onWindowClosing() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * What to do when the Same Slice is used for in and Out of Focus (not
     * allowed)
     */
    private void sameOutAndInSelection() {
        Notify_.setText("In and OutOfFocus Cannot Be Same Slice");
        submitButton_.setEnabled(false);
    }

    /**
     * Modify User Elements for if there's no problems
     */
    private void noError() {
        Notify_.setText("");
        submitButton_.setEnabled(true);
    }

    //Awt Elements
    Label inFocusSliceLabel_ = new Label();
    Label inFocusSliceNumber_ = new Label();
    Label outFocusSliceLabel_ = new Label();
    Label outFocusSliceNumber_ = new Label();
    Button inFocusButton_ = new Button();
    Button outFocusButton_ = new Button();
    Label Notify_ = new Label();
    Button submitButton_ = new Button();

}
