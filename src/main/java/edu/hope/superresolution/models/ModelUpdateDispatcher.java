/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.models;

import edu.hope.superresolution.exceptions.DuplicateListenerException;
import java.util.ArrayList;

/**
 * Super Class For Models That expect to use ModelUpdateListeners to notify their
 * View and other elements, dependent on its state changes.
 * <p>
 * This class provides methods to un/register listeners to the model, dispatch events
 * that are defined in the extending class to those listeners, and enable or disable 
 * the guiElements of the listeners.
 *
 * @author Microscope
 */
public abstract class ModelUpdateDispatcher {
    
    private final ArrayList< ModelUpdateListener > listeners_ = new ArrayList< ModelUpdateListener >();
    
    /**
     * Register a ModelUpdateListener to the extending model.  The onRegisterToModel() method 
     * will be called if this model is successfully added and passed the reference of the 
     * instance.
     * 
     * @param listener 
     * @throws DuplicateListenerException in the event that this listener was already called.  
     *         This exception is a runtime exception that allows the programmer the 
     *         flexibility of checking a registration failure and subsequent callback
     *         failure in applications where registration may occur in multiple thread instances.
     */
    public void registerModelListener( ModelUpdateListener listener ) {
        //Checks to ensure that instance is not already listening
        if( listener != null && !listeners_.contains(listener) ) {
            listeners_.add(listener);
            listener.onRegisteredToModel(this);
        } else {
            //Throw duplicate exception for calling code to handle
            throw new DuplicateListenerException( "ModelUpdateDispatcher already had "
                    + "listener registered to it without being unregistered.  No callback was called,"
                    + " due to this fact.");
        }
    }
    
    /**
     * Unregister a ModelUpdateListener from the extending model.  The onUnregisterToModel() method 
     * will be called if this model is successfully removed and passed the reference of the 
     * instance.
     * 
     * @param listener 
     */
    public void unregisterModelListener( ModelUpdateListener listener ) {
        if( listeners_.remove( listener ) ) {
            listener.onUnregisteredToModel(this);
        }
    }
    
    /**
     *  Enable/disable the Graphical Elements of a given ModelUpdateListner if it is registered 
     *  to the extending model.  This calls the guiEnable() method of the listener in question.
     * 
     * @param listener - The specific ModelUpdateListener to toggle their GUI elements
     * @param enable - <code>true</code> enables the gui elements associated with the listener
     *                 <code>false</code> disables the gui elements associated with the listener
     * @throws Exception - If the listener is not actually registered
     */
    public void enableListenerGUI( ModelUpdateListener listener, 
                                            boolean enable ) throws Exception {
        
        if( !listeners_.contains( listener ) ) {
            throw new Exception( "Listener not contained" );
        }
        
        listener.guiEnable( enable );
    }
    
    //Enable All registered View GUIs
    /**
     * Enables/disables all the GUI elements of the registered listeners.  This
     * effectively calls the guiEnable() methods of every registered listener.
     * 
     * @param enable - <code>true</code> enables the gui elements associated with the listener
     *                 <code>false</code> disables the gui elements associated with the listener
     */
    public void enableAllListenerGUIs( boolean enable ) {
        for( ModelUpdateListener listener : listeners_ ) {
            try {
                enableListenerGUI( listener, enable );
            } catch ( Exception ex ) { 
                //Exception is Guaranteeed not to throw
            }
        }
    }
    
    //Dispatch on a custom Event Integer that is saved in the extending class
    /**
     * Dispatch a custom event (defined as specific integers in the extending class)
     * to all registered listeners.  This calls the update() method of each listener.
     * 
     * @param event - integer defined events that are defined in the extending class
     */
    public void dispatch( int event) {
        for( ModelUpdateListener listener : listeners_ ) {
            listener.update( this, event );
        }
    }
    
}
