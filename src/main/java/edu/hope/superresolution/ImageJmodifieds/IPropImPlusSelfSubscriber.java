/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.ImageJmodifieds;

/**
 * Interface for a subscriber that will contains methods to subscribe and unsubscribe from a PropertyNotifyingImagePlus.
 * 
 * @author Desig
 */
interface IPropImPlusSelfSubscriber {
    
    /**
     * Sets up the registration.  Should include storing of reference to the Event caller
     * and any other environment variables to be set based on the subscription.
     * 
     * @param propImPlus - The non-null PropertyNotifyingImagePlus to which we will register
     */
    public void registerToPropertyNotifyingImagePlus( PropertyNotifyingImagePlus propImPlus );
    
    /**
     * This unregisters from the propImPlus events.  Implementation should assume that null will not
     * be passed to it from the calling context.
     * 
     * @param propImPlus - The non-null PropertyNotifyingImagePlus from which we assume to have already registered and therefore unregister
     */
    public void unregisterFromPropertyNotifyingImagePlus( PropertyNotifyingImagePlus propImPlus );
    
}
