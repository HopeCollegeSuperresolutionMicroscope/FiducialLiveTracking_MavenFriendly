/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.processors;

import edu.hope.superresolution.MMgaussianfitmods.datasubs.ExtendedGaussianInfo;
import edu.hope.superresolution.fitSettingsTest.ImageRegionFitThread;
import edu.hope.superresolution.fitters.FindLocalMaxima;
import edu.hope.superresolution.fitters.GaussianFitThread;
//import edu.hope.superresolution.fitters.ImageRegionFitThread;
import edu.hope.superresolution.genericstructures.ListCallback;
import edu.hope.superresolution.genericstructures.iZEstimator;
import edu.hope.superresolution.models.FiducialArea;
import edu.hope.superresolution.models.MicroscopeModel;
import edu.valelab.gaussianfit.utils.ReportingUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *   Super Class For All Fiducial Area Processors
 *    
 *    It is assumed that a Given Processor Instance is set up for a Given Microscope 
 *    Model.  Multiple Microscope Models or modes require different Processor 
 *    instances.  These Instances may be stored in tandem for multiple processes
 *    or for microscope characteristic changes in the same acquisitions.
 * 
 *   The Fiducial Processor Function is to update FiducialAreas in accordance with 
 *   changes to settings, as well as changes to FiducialAreas.  The Fiducial Processor
 *   In this instance, a FiducialProcessor instance is expected to adhere to a FiducialLocationModel
 *   or to a FiducialArea.  However, The FiducialProcessor is more explicitly meant 
 *   to be linked to a LocationAquisitionModel. 
 *
 * @author Microscope
 */
public abstract class FiducialAreaProcessor implements iFiducialAreaProcessor {
    
    private ExtendedGaussianInfo currentSettings_;
    private volatile boolean isAsyncProcessing_ = false;
    private MicroscopeModel microscopeModel_;
    private iZEstimator zEstimator_;
    
    //Lock Used to Indicate (In Advance) that a processor will be calling a method multiple times
    //  Necessary to avoid a settings update
    private final ReentrantLock settingsLock_ = new ReentrantLock();
    private final Object asyncSetLock_ = new Object();
    
    //Copies an ExtendedGaussianInfo Object with Settings,
    //  This is for Thread-safety and incremental setting changes if the camera has a problem
    public FiducialAreaProcessor( ExtendedGaussianInfo settings, MicroscopeModel microscopeModel, iZEstimator zEstimator ) {
        currentSettings_ = new ExtendedGaussianInfo( settings );  
        microscopeModel_ = new MicroscopeModel( microscopeModel );
        zEstimator_ = zEstimator;
        //For the Sake of succintness, update with the copy model.
        zEstimator_.setMicroscopeProperties(microscopeModel_);
    }

    @Override
    public void enableAsyncProcessing(boolean enable) {
        synchronized( asyncSetLock_ ) {
            isAsyncProcessing_ = enable;
        }
    }

    @Override
    public boolean isAsyncProcessEnabled() {
        synchronized( asyncSetLock_ ) {
            return isAsyncProcessing_;
        }
    }

    @Override
    public double getPixelSize() {
        
        lockSettings();
        double tempSize = 0;
        try {
            tempSize = currentSettings_.getPixelSize();
        } finally  {
            unlockSettings();
        }
        
        return tempSize;
    }
    
    //Convenient Methods to Lock settingsLock
    /**
     *  Permits External locking for calling contexts to operate on settings (ExtendedGaussianInfo).
     * <p>
     * Note:  Must Call unlockSettings().  Wrap in a try - finally
     */
    public void lockSettings() {
        settingsLock_.lock();
    }
    
    /**
     * Unlock settings (ExtendedGaussianInfo) lock from Calling context.
     * <p>
     * Note:  Must Be Called after lockSettings() was called. Wrap in a try - finally
     */
    public void unlockSettings() {
        settingsLock_.unlock();
    }
    
    /**
     * Updates the ExtendedGaussianInfo for this processor by copying it into the processor
     * once the settings lock is relinquished.
     * 
     * @param settings 
     */
    public void updateCurrentSettings( ExtendedGaussianInfo settings ) {
        lockSettings();
        try {
            currentSettings_ = new ExtendedGaussianInfo( settings );
        } finally {
            unlockSettings();
        }
    }
    
    /**
     * Updates the MicroscopeModel for this processor by copying it into the processor
     * once the settings lock is relinquished.
     * 
     * @param microscopeModel - the new microscopeModel to copy
     */
    public void updateMicroscopeModel( MicroscopeModel microscopeModel ) {
        lockSettings();
        try {
            microscopeModel_ = new MicroscopeModel( microscopeModel );
        } finally {
            unlockSettings();
        }
    }
    
    /** 
     *  Exposes the Current Settings Object (non-Copy) to the calling context.
     * <p>
     *   Note: This exposure of a reference should be locked using lockSettings() and unlockSettings
     *   if modification is desired.
     * 
     * @return A Reference to the ExtendedGaussianInfo Object
     */
    public ExtendedGaussianInfo getCurrentSettings() {
        ExtendedGaussianInfo tempRef = null;
        lockSettings();
        try {
            tempRef = currentSettings_;
        }
        finally {
            unlockSettings();
        }
        
        return tempRef;
    }
    
    /**
     * Get the MicroscopeModel that this processor is tuned for.  This needs to 
     * be locked using lockSettings() and unlockSettings() if modification is intended.
     * 
     * @return 
     */
    public MicroscopeModel getMicroscopeModel() {
        return microscopeModel_;
    }
    
    public iZEstimator getZEstimator() {
        return zEstimator_;
    }
    
    /**
     * Sets the zEstimator to this one currently being used.
     * <p>
     * Note, the current processor MicroscopeModel will overwrite any of the zEstimator
     * microscopeModel properties from before.
     * 
     * @param zEstimator 
     */
    public void setZEstimator( iZEstimator zEstimator ) {
        lockSettings();
        try {
            zEstimator_ = zEstimator;
            zEstimator_.setMicroscopeProperties(microscopeModel_);
        } finally {
            unlockSettings();
        }
    }
    
    private final List< FiducialArea > updateAreas_ = Collections.synchronizedList( new ArrayList<FiducialArea>() ); 
    
    /**
     * Overload - Register FiducialArea 
     * 
     * @param fArea - The FiducialArea to Register
     */
    public void registerFiducialArea( FiducialArea fArea ) {
        registerFiducialArea(fArea, true );
    }
    
    /**
     * Register FiducialArea to be updated on this processorChanges.
     * <p>
     * Quietly Returns if the FiducialArea was already Registered, and does not process the Register
     * 
     * @param fArea - The FiducialArea To Register
     * @param processOnRegister - Whether or not to process the FiducialArea for fits upon registering
     * @return <code>true</code> If the area was registered <code>false</code> if the area was already registered and
     *         therefore nothing was done.
     */
    public boolean registerFiducialArea( FiducialArea fArea, boolean processOnRegister ) {
        if (!updateAreas_.contains(fArea)) {
            updateAreas_.add(fArea);
            if (processOnRegister) {
                processFiducialArea(fArea);
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * Unregister a FiducialArea from being updated on processor Changes
     * <p>
     * Quietly returns if the FiducialArea was not registered.
     * 
     */
    public void unregisterFiducialArea( FiducialArea fArea ) {
        if( updateAreas_.contains( fArea ) ) {
            updateAreas_.remove( fArea );
        }
    }
    
    /**
     *  Process All FiducialAreas that are registered to this Processor.
     *  Meant to Be Used Internally To Update on Any settings Changes.
     */
    protected void updateAllFiducialAreas( ) {
        synchronized( updateAreas_ ) {
            Iterator<FiducialArea> it = updateAreas_.iterator();
            while( it.hasNext() ) {
                FiducialArea fArea = it.next();
                processFiducialArea( fArea );
            }
        }
    }
    
    
    private ListCallback preListCallback_ = null;
    private ListCallback postListCallback_ = null;
    /**
     * Sets a ListCallback that will be implemented on each FiducialArea
     * processing before the defaultCallback that populates the FiducialArea
     * and as such may act as a pre-filter on the list being passed to it.
     * <p>
     * This Only Allows for one Pre-filter Callback to be implemented at a time.  
     * This is mainly for avoidance of ordering and modifications that may occur if the 
     * intention is to actually alter the list before it reaches the FiducialArea.
     * 
     * @param preCallback - The ListCallback to be Called before the PopulatingCallback on the FiducialArea
     * @return - The ListCallback that was previously registered as the preListCallback (null if none)
     */
    public ListCallback setPreListCallback( ListCallback preCallback ) {
        ListCallback ret = preListCallback_;
        preListCallback_ = preCallback;
        
        return ret;
    }
    
    /**
     * Sets a ListCallback that will be implemented on each FiducialArea
     * processing after the defaultCallback that populates the FiducialArea
     * and as such may act as a reporter on the list after being passed to the FiducialArea.
     * <p>
     * This Only Allows for one Post Reporter Callback to be implemented at a time.  
     * This is mainly for avoidance of ordering and modifications that may occur if the 
     * intention is to actually alter the list before it reaches the FiducialArea.
     * 
     * @param postCallback - The ListCallback to be Called after the PopulatingCallback on the FiducialArea
     * @return - The ListCallback that was previously registered as the post ListCallback (null if none)
     */
    public ListCallback setPostListCallback( ListCallback postCallback ) {
        ListCallback ret = postListCallback_;
        postListCallback_ = postCallback;
        
        return ret;
    }
    
    private ImageRegionFitThread processThread_;

    /**
     * Sets The RegionFitThread That will be used in processing.  Note, this will wait for 
     * any current Processing to finish and will then reprocess the FiducialAreas.
     * 
     * @param processThread New processThread Instance to use
     */
    public void setRegionFitThread( ImageRegionFitThread processThread ) {
        lockSettings();
        try {
            processThread_ = processThread;
        } finally { 
            unlockSettings();
        }
        
    }
    
    private ExecutorService FiducialAreaThreadPool_ = Executors.newCachedThreadPool();
    
    public void process( ) {
        
        lockSettings();
        try {
            //Since We can count on Fitting be more intensive than creating New Image Instances, 
            // We instantiate Each FiducialArea into its own Callable and Run them separately
                boolean first = true;
                ImageRegionFitThread processThread = processThread_;
                List<Future<?>> futureList = new ArrayList<Future<?>>( updateAreas_.size() );
                for( FiducialArea fArea : updateAreas_ ) {
                    if( !first ) {
                        //Use the first instance of the thread we have without copying
                        processThread = processThread_.copy();
                    }
                    //Selection Area should be the same as the TrackSearch Area if tracking
                    processThread.setRoi( fArea.getSelectionArea() );
                    //This should all be the same in a track but for other uses is not guaranteed
                    processThread.setImagePlus( fArea.getImagePlus() );
                    futureList.add( FiducialAreaThreadPool_.submit(processThread) );
                    //processThread init
                    //processThread.init();
                }

                
            //Wait for Worker Threads to finish
            boolean wasInterrupted = false;
            while (!futureList.isEmpty()) {
                Future<?> curFuture = futureList.get(0);
                if (curFuture != null) {
                    try {
                        List<  > curFuture.get();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ImageRegionFitThread.class.getName()).log(Level.SEVERE, null, ex);
                        wasInterrupted = true;
                    } catch (ExecutionException ex) {
                        Logger.getLogger(ImageRegionFitThread.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                //Remove the Future from the List
                futureList.remove(0);
            }

            //After letting all other threads close propagate Interruption
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
   
            
        } finally {
            unlockSettings();
        }
        
        
    }
    
    //Dealing With Executor Services here.
    //The FiducialAreaProcessor should have its own static ThreadPool, or should it have
    // the threadpool of the plugin?
    //Ideally, We have upfront Processes: The Processes that are at the forefront.
    //  We also have back processes - FiducialProcessors that are linked but are not pertinent just yet
    // FiducialProcessor ThreadPool = 3 threads max
    //  Run Asynchronous on all FiducialAreas Available at the moment.
    // Initialize all Other FiducialAreas to process as well.
    //  
    
    //Generalizing FiducialArea Processor
    //  A FiducialArea Processor has its own settings
    //  For instance, If my ImageRegionStackThread needs certain settings
    //  Those settings are dependent on the settingsObject
    //  So in reality, if we expect a settingsObject,
    
}
