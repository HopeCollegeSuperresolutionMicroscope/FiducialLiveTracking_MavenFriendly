/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Basic ThreadSafe Instance of an object that stores and can refer to a settings object
 * to do meaningful tasks in its extending classes.
 *
 * @author Desig
 */
public class SettingsDependentObjectBasic <SettingsClass extends iSettingsObject> implements iSettingsDependentObject<SettingsClass> {
    
    private SettingsClass settingsObj_;
    private ReentrantLock settingsLock_;
    
    public SettingsDependentObjectBasic(){}
    
    /**
     * General constructor
     * @param settingsObj - Settings Object to Copy
     */
    public SettingsDependentObjectBasic( SettingsClass settingsObj ) {
        settingsObj_ = (SettingsClass) settingsObj.copy( settingsObj ); 
    }
    
    /**
     * Returns a copy of the settings stored at this moment.
     * @return 
     */
    @Override
    public SettingsClass getSettingsCopy() {
        lockSettings();
        try {
            return (SettingsClass) settingsObj_.copy(settingsObj_);
        } finally {
            unlockSettings();
        }
    }

    /**
     * Copies the settingsObject into the settings For use.
     * @param settingsObject 
     */
    @Override
    public void setSettings( SettingsClass settingsObject) {
        //jdk 1.6 use of instance interface due to inability to use static interface
        lockSettings();
        try {
            settingsObj_ = (SettingsClass) settingsObject.copy( settingsObject );  
        } finally {
            unlockSettings();
        }
    }
    
    //Convenient Methods to Lock settingsLock for outside altering of reference
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
     * Returns the Reference to the internal settings object of this class.
     * <p>
     * Any modifications to the reference should call lockSettings and unlockSettings.
     * 
     * @return 
     */
    protected SettingsClass getSettingsRef( ) {
        return settingsObj_;
    }
    
}
