package edu.hope.superresolution.processors;


import edu.hope.superresolution.MMgaussianfitmods.datasubs.ExtendedGaussianInfo;
import edu.hope.superresolution.fitters.FindLocalMaxima;
import edu.hope.superresolution.fitters.GaussianFitThread;
import edu.hope.superresolution.fitters.GaussianWithSimpleDefocusFitThread;
import edu.hope.superresolution.fitters.GenericBaseGaussianFitThread;
import edu.hope.superresolution.genericstructures.FitThreadCallback;
import edu.hope.superresolution.genericstructures.ListCallback;
import edu.hope.superresolution.models.FiducialArea;
import edu.hope.superresolution.models.MicroscopeModel;
import edu.valelab.gaussianfit.data.SpotData;
import edu.valelab.gaussianfit.utils.ReportingUtils;
import ij.ImagePlus;
import ij.gui.Roi;
import java.util.List;
import javax.swing.JOptionPane;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * 
 * @author Microscope
 */
public class GaussianFitProcessor extends FiducialAreaProcessor {
    
    private final Integer MAX_WAIT_TIME = 10000;  //Max thread Wait Time for a Join
    private GaussianFitThread.DataEnsureMode DataEnsureMode_;
    
    //Current GaussianFitThread
    private volatile GenericBaseGaussianFitThread gft_;
    private final Object gftLock_ = new Object();  //Used to ensure only 1 thread manipulates any gft_ to run
    
    public GaussianFitProcessor( final ExtendedGaussianInfo settingsRef, 
                                   final MicroscopeModel microscopeModel ) {
        super( settingsRef, microscopeModel );
    }
    
    //Copy Constructor
    //Only Copies Settings
    public GaussianFitProcessor( GaussianFitProcessor sourceProcessor ) {
        super( sourceProcessor.getCurrentSettings(), sourceProcessor.getMicroscopeModel() );
    }

    /**
     * Fits all possible Fiducials to the Roi, and returns the results through a callback
     * 
     * @param ip The ImagePlut in which the fitting should occur
     * @param roi The Roi in which to apply fitting
     * @param callerCallback The callback to return the fitted fiducials after processing
     * @return true if a new fit is in process or a change occurs or is slated to occur
     */
    @Override
    public boolean fitRoiForImagePlus( ImagePlus ip, Roi roi, ListCallback callerCallback ) {
        
        synchronized (gftLock_) {
            //Check First to see if this is an asynchronous call
            if (gft_ != null && isAsyncProcessEnabled()) {
                //Block for the rest of thread to finish 
                //This should be changed to a Queue later if optimization or more processes are desired
                try {
                    while (gft_.isRunning()) {
                        gftLock_.wait();
                    }
                } catch (InterruptedException ex) {
                    ReportingUtils.logError(ex);
                }
            }
            //ij.IJ.log("Calling Specific FitThread");
            if (gft_ == null || !gft_.isRunning()) {
                //This is safe because settings are copied into Thread
                gft_ = new GaussianFitThread(ip, roi, 
                        new GaussianFitProcessor.GaussianFitListAction(callerCallback),
                        getCurrentSettings(), FindLocalMaxima.FilterType.NONE );
            }

            if (!gft_.init()) {
                return false;
            }
        }

        if (!isAsyncProcessEnabled()) {
            try {
                gft_.join(MAX_WAIT_TIME);
            } catch (InterruptedException ex) {
                JOptionPane.showMessageDialog(null, "Error process Interrupted: " + ex.getMessage());
            }
        }
   
        return true;
        
    }
    
    
    @Override
    public boolean fitFiducialAreaForImagePlus( ImagePlus ip, FiducialArea fArea, ListCallback resultListCallback) {
        return fitRoiForImagePlus( ip, fArea.getSelectionArea(), resultListCallback );
    }
    
    
    //local Callback Object for use with GaussianFitThread
    public class GaussianFitListAction implements FitThreadCallback<SpotData> {

        private ListCallback<SpotData> externCallback_ = null;
        
        //Constructor takees externalCallback from calling context
        //  This is currently the FiducialModel BoundedSpot
        GaussianFitListAction( ListCallback externCallback ) {
            externCallback_ = externCallback;
        }
        
        @Override
        public void onListFull(List<SpotData> list) {
            //Inner Updates
            
            //Propagate upward to external calling context like BoundedSpot
            if( externCallback_ != null ) {
                externCallback_.onListFull(list);
            }
        }

        @Override
        public void onListElementAdded(List<SpotData> list) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onListElementRemoved(List<SpotData> list) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onThreadEnd() {
           
            //Unblock Waiting Threads From Creating Another Thread And Running It
            //In the Future this may be better of being handled by an Asynchronous Manager
            //But currently, it only permits the given number of threads to spawn and run ( 1 )
            synchronized( gftLock_ ) {
                gftLock_.notify();
            }
            
        }

        @Override
        public void onThreadStop() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    
        
        
    }
    
    
}
