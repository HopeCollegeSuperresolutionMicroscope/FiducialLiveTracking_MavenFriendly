/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

/**
 *  Event Class for Use with EventBus Instances specifically.  This must be extended
 *  and is not recommended to be the only type in a subscriber method.  
 *  This super class expects the specific creating instance in its constructor
 *  and provides the sameOriginator() method for subscribers to verify if the originator 
 *  of this Event is one that they have a reference to.
 *  <p>
 *  In hardware terms, this is analogous to having multiple slaves on a single bus.
 *  If a single EventBus is going to manage many instances calling the same events,
 *  then this should be used.  Likewise, if a subscriber is going to be registered
 *  to more than one EventBuses that can call the same Event (e.g. multiple instances of 
 *  a single class with seperate EventBuses) then this should be used to determine the calling object.
 *  <p>
 *  Note:  This class does not expose the reference to the instance originating the event to prevent intermediate alteration
 *  of the object posting Events.
 *
 * @author Justin Hanselman
 */
public abstract class MasterSpecificEvent {
    
    private final Object originator_;
    
    /**
     * Constructor
     * 
     * @param originator - The Originating Instance for the Event.  Should be same as the Calling context.
     */
    public MasterSpecificEvent( Object originator ) {
        originator_ = originator;
    }
    
    /**
     * Copy constructor - Ideally isolates originator tampering to simply creating a new Event.
     * <p>
     * This was designed with the idea of Concantenation in mind.  For that, there needs
     * to be a way to take two originating Events and create a new Event, which requires
     * a source for the originator.  Subclass Constructors of the type ( MasterSpecificEvent, subClass variables)
     * are the anticipated helpful format for extension.
     * 
     * @param source 
     */
    public MasterSpecificEvent( MasterSpecificEvent source ) {
        originator_ = source.originator_;
    }
    
    /**
     * Takes a reference to an instance and determines if it is the same originator.
     * @param testOrigin - The reference that is expected as the originator of this event
     * @return - <code>true</code> if references are the same <code>false</code> otherwise
     */
    public boolean sameOriginator( Object testOrigin ) {
        return testOrigin == originator_;
    }
    
    /**
     * Overload For sameOriginator().  Tests if two events are from the same Originator.
     * 
     * @param event - The event to compare to this one to see if they have the same orginator
     * @return  <code>true</code> if references are the same <code>false</code> otherwise
     */
    public boolean sameOriginator( MasterSpecificEvent event ) {
        return event.sameOriginator(originator_);
    }
    
    /**
     * Concatenates This Event with the other one.  This in effect Squishes the two
     * Events together Into a single Event that is returned.  
     * <p>
     * This was intended for multiple events that are rapidly fired that store previous
     * and current info, so that before sending all data, it might be concatenated.  In that format, 
     * the object doing the Concatenating, IS ASSUMED TO BE THE PREVIOUS OBJECT.
     * <p>
     * However, it might also be useful for related Events to be concatenated into 
     * another Event object that encapsulates their related Data.  It also might
     * be more advantageous for a concatenated Event to order by maximum and minimum of the 
     * two.  In unordered, Feel free to align them however you would like.
     * Feel free to document any interesting Concatenations.
     * 
     * @param event - The resulting concatenated Event.  
     * 
     * TODO:  Throw an exception in the case that the wrong Event type is given
     */
    public abstract MasterSpecificEvent concatenate( MasterSpecificEvent event );
    
}
