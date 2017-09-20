/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.processors;

import edu.hope.superresolution.MMgaussianfitmods.datasubs.ExtendedGaussianInfo;
import edu.hope.superresolution.models.MicroscopeModel;
import java.util.concurrent.locks.ReentrantLock;

/**
 *   Super Class For All Fiducial Area Processors
 *    
 *    It is assumed that a Given Processor Instance is set up for a Given Microscope 
 *    Model.  Multiple Microscope Models or modes require different Processor 
 *    instances.  These Instances may be stored in tandem for multiple processes
 *    or for microscope characteristic changes in the same acquisitions.
 *
 * @author Microscope
 */
public abstract class FiducialAreaProcessor implements iFiducialAreaProcessor {
    
    private ExtendedGaussianInfo currentSettings_;
    private volatile boolean isAsyncProcessing_ = false;
    private MicroscopeModel microscopeModel_;
    
    //Lock Used to Indicate (In Advance) that a processor will be calling a method multiple times
    //  Necessary to avoid a settings update
    private final ReentrantLock settingsLock_ = new ReentrantLock();
    private final Object asyncSetLock_ = new Object();
    
    //Copies an ExtendedGaussianInfo Object with Settings,
    //  This is for Thread-safety and incremental setting changes if the camera has a problem
    FiducialAreaProcessor( ExtendedGaussianInfo settings, MicroscopeModel microscopeModel ) {
        currentSettings_ = new ExtendedGaussianInfo( settings );  
        microscopeModel_ = new MicroscopeModel( microscopeModel );
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
}
