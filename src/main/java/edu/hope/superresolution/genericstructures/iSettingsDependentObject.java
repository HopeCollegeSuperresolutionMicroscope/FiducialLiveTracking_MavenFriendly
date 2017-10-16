/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

/**
 *
 * Generic Structure for implementing Classes that Depend on an object for its properties.
 * This is intended to generalize Class Construction for processes so that feature changes
 * are the function of a new SettingsClass and an extension with that specific SettingsClass.
 * This interface is also intended to be extended with a concrete Settings Class.
 * In this way, the concrete Class becomes the settings for the extending class and is parsed
 * accordingly.
 * 
 * @author Desig
 * @param <SettingsClass> - The class expected to be used as a settings object.  This 
 *                          should be concrete in the implementation of this interface
 *                          so that implementors expect and handle their specific class.  
 *                          The use of a concrete generic will also allow for compile time checking
 *                          of mismatched information.
 */
public interface iSettingsDependentObject <SettingsClass extends iSettingsObject> {
    
    
    /**
     * Return a copy of the implementing Classes settings Class.
     * <p> 
     * Note, returning a copy is for the sake of ThreadSafety.
     * @return 
     */
    public SettingsClass getSettingsCopy();
    
    /**
     * Sets the Current settings from the settingsClass.
     * 
     * @param settingsObject The settings Object to set this too.
     */
    public void setSettings( SettingsClass settingsObject );
    
}
