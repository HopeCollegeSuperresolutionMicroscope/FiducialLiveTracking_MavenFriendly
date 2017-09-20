package edu.hope.superresolution.livetrack;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import edu.hope.superresolution.models.LocationAcquisitionModel;
import edu.hope.superresolution.views.TestImageWindowCombinations;
import ij.ImagePlus;
import ij.process.ShortProcessor;
import java.util.ArrayList;
import org.micromanager.api.MMPlugin;
import org.micromanager.api.ScriptInterface;

/**
 *
 * @author Microscope
 */
public class LiveTracking implements org.micromanager.api.MMPlugin {

    //Static Access to menuName for MicroManager
    public static final String menuName = "Fiducial Tracking App";
    public static final String tooltipDescription =
       "Autofocusing Acquisition based on fiducial Gaussians";
    
    //Static Array List for Access to currently Running Plugins
    private static final ArrayList< MMPlugin > runningInstances_ = new ArrayList< MMPlugin >();
    
    // Current OverArching Location Acquision Model
    //  (Could Become a List for more window acquisitions in a single plugin)
    private LocationAcquisitionModel locAcqModel_;
    
    //Get the list of running Instances
    // returns - ArrayList of MMPlugin (Generic Interface) 
    public static ArrayList< MMPlugin > getInstances() {
        return runningInstances_;
    }
    
    //Adds an instance of this class to the runningInstances_ list
    private static void addRunningInstance( LiveTracking instance ) {
        runningInstances_.add( instance );
    }
    
    //removes a specified running Instance from the list (i.e. it closed)
    private static void removeRunningInstance( LiveTracking instance ) {
        runningInstances_.remove( instance );
    }

    //Return Given Location Acquisition Model for the Plugin
    public LocationAcquisitionModel getLocationAcqModel() {
        return locAcqModel_;
    }
    
    @Override
    public void dispose() {
        locAcqModel_.dispose();
        removeRunningInstance( this );
    }

    @Override
    public void setApp(ScriptInterface si) {
        //In the event that this autofocus is not installed via its own .jar, 
        // set the fiducialAutoFocus as available to the plugin
       /* FiducialAutoFocus fAutoFocus_ = null;
        //ReportingUtils.showMessage(si.installAutofocusPlugin( FiducialAutoFocus.class.getName() ) );
        //Temporary AutoFocus Instance For Testing
        AutofocusManager afMgr = si.getAutofocusManager();
        afMgr.setAFPluginClassName( FiducialAutoFocus.class.getName() );
        try {
            afMgr.refresh();
        } catch (MMException ex) {
            ReportingUtils.showError(ex);
        }
        final ScriptInterface siRun = si;
        //Notify User Of New Autofocus Options
        ReportingUtils.showMessage("New Autfocus Added: " + FiducialAutoFocus.DEVICE_NAME );
        try {
            afMgr.selectDevice( FiducialAutoFocus.DEVICE_NAME );
            //This is Stupidly Deprecated without a replacement for opening the Dialog
            // runAcquisition does not work well
            //si.getAcqDlg().setVisible(true);

        } catch (MMException ex) { 
           ReportingUtils.showError(ex);
           return;
        } */
        //fAutoFocus_ = (FiducialAutoFocus) afMgr.getDevice();
        TestImageWindowCombinations tester = new TestImageWindowCombinations(new ImagePlus( "testcase", new ShortProcessor(400, 400) ));
        
        //Cue Up the LiveWindow For Selection of Fiducials and Focus Plane
        si.enableLiveMode(true);
        
        //Should make this a Factory for other Image Processors
        
        locAcqModel_ = new LocationAcquisitionModel( si.getSnapLiveWin(), si, this );
        locAcqModel_.enableSelectedLocationModelGUIs( true );
        
        //Log That this has Been started Successfully
        addRunningInstance( this );
        
    }

    @Override
    public void show() {
        String ig = "This is ig";
    }

    @Override
    public String getDescription() {
        return tooltipDescription;
    }

    @Override
    public String getInfo() {
       return tooltipDescription;
    }

    @Override
    public String getVersion() {
        return ".1";
    }

    @Override
    public String getCopyright() {
        return "me";
    }


    
}