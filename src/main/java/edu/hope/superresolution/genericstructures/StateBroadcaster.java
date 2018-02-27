/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import com.google.common.eventbus.EventBus;

/**
 *
 * Base Class for more fine-grained control over Events than the Observer Class.
 * Uses an EventBus instance to dispatch to subscribers that are registered to listen for events.
 * Extending Classes must dispatch events in their code as seen fit, and make an effort to account for 
 * cyclic dispatches if they exist.
 * <br/>
 * It is the burden of the Extending Class to expose public events so that a registering class can build callback methods
 * for it.
 * <br/>
 * Note: Logging will report the Classname and a number related to chronology of creation from start of the program.
 * This will be accurate until overflow occurs, and then will iterate form INT_MIN to INT_MAX. Note this for any SUPER long runing
 * and SUPER instance heavy applications.
 * TODO: extending to AbstractClasses for Event Queue dispatching for better utility
 * 
 * @author HanseltimeIndustries
 */
public abstract class StateBroadcaster {

    EventBus bus_;
    static int busUniquenessIdx_ = 0;

    /**
     * Sets up the instance's Event Bus for State Event Broadcasting
     */
    public StateBroadcaster() {
        SetupBusInstance();
    }
    

    /**
     * Private Method where EventBus is setup. Due to the nature of the super
     * constructors We also, clear the pendingEventQueue_ for normal service.
     */
    private void SetupBusInstance() {
        bus_ = new EventBus(this.getClass().getName() + busUniquenessIdx_);
        //Increment the Uniqueness Index for the next EventBus
        busUniquenessIdx_++;
    }
    
    /**
     * Registers an Object to this object's separate EventBus instance.
     *
     * @param subscriber - an EventBus Subscriber to be registered to this
     * objects Event's
     */
    public void RegisterToStateEventBus(Object subscriber) {
        bus_.register(subscriber);
    }

    /**
     * Unregisters an Object from this instance's separate EventBus instance.
     *
     * @param subscriber - the EventBus Subscriber to be unregistered from this
     * objects Event's
     */
    public void UnregisterFromStateEventBus(Object subscriber) {
        bus_.unregister(subscriber);
    }

    /**
     * Dispatches an event, to be triggered in extending class
     * 
     * @param evt 
     */
    protected void dispatchEvent( MasterSpecificEvent evt ) {
        bus_.post(evt);
    }
    
}
