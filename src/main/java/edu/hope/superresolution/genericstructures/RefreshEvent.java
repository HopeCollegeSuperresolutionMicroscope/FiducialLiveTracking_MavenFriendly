/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

/**
 * Event Class Wrapper -- This class is used to store events that are normally
 * broadcast by the event bus on a regular, but not immediate basis.
 * <p>
 * The term refresh, refers to the explicit nature of a refresh operation, in
 * which the operation must be called. Thus, specific generics of this class
 * will provide a concatenated Event documenting details since the last time a RefreshEvent
 * was broadcast up to the current broadcast.
 * <p>
 * Example of intended Usage: A GUI View is listening to a model for every DataChangeEvent
 * as defined in the model class, so as to update what the user sees for preliminary confirmation.
 * However, a large amount of data will need to be reiterated once the user has finalized their decision.
 * Therefore, the data that is linked to this model is subscribed to a <code>RefreshEve &lt; model,DataChangeEven &gt;</code>
 * event type.  It is then the model's responsibility to dispatch this RefreshEvent at the appropriate time.
 * (i.e. when the user confirms their selction).
 * <p>
 * It is up to the documenter of these models to specify which events are RefreshEvents.
 * They May subclass these Events within the broadcasting class in order to produce explicit Types
 * that are more intuitive or informative to the subscriber.
 *
 * @param <OriginatorType> - The Type of the model that is expected to be generating these events.  This is used for instance checking
 *                            in the MasterSpecificEvent super class.
 * @param <EventType> - The Type of the Event
 *
 * @author Justin Hanselman
 */
public class RefreshEvent<OriginatorType, EventType extends MasterSpecificEvent> extends MasterSpecificEvent {

    //Other Classes have had final members.  May need to change this back.  However, it saved memory creation.
    private EventType evt_;

    /**
     * General Constructor -- Specifies the originator class that spawned this refresh Event and the Event Itself
     * 
     * @param originator -
     * @param evt - The event to be stored 
     */
    public RefreshEvent(OriginatorType originator, EventType evt) {
        super(originator);

        evt_ = evt;
    }

        /**
         * Originator Copy (superclass) Constructor
         * 
         * Takes An event and uses its Originator_ only.  
         * 
         * @param originatorSource - The Event that already has an Originator to copy into this instance
         * @param evt - The event to be stored (assumed to already be concatenated in the calling context)
         */
    public RefreshEvent(RefreshEvent<OriginatorType, EventType> originatorSource, EventType evt) {
        super(originatorSource);

        evt_ = evt;
    }

    /**
     * Get the Event associated with this RefreshEvent
     * 
     * @return 
     */
    public EventType getEvent() {
        return evt_;
    }
    
    /**
     * Appends an Event Instance to the currently stored previous event. In this
     * case, the appended Event Will be seen as the later occuring event and
     * will be concatenated with the current event
     *
     * @param evt
     */
    public void appendEvent(EventType evt) {
        evt_ = (EventType) evt_.concatenate(evt);
    }

    /**
     * Combine this RefreshEvent with the other. Will concatenate the Events within these the two
     * RefreshEvents.  Assumes the event instance is the previous and will concatenate with 
     * this instance's event added to the other.
     * <p>
     * Note: This event must be of the same type as the subclass.  
     *
     * @param event - the More Recent Event compared to this
     * @return New UpdateWait_SelectedFiducialChangeEvent object
     */
    @Override
    public MasterSpecificEvent concatenate(MasterSpecificEvent event) {
        //Make Sure This is the same Base Type of SelectedFiducialChangeEvent
        Class<RefreshEvent<OriginatorType, EventType>> type = (Class<RefreshEvent<OriginatorType, EventType>>) this.getClass();
        if (!(type.isInstance(event))) {
            throw new IllegalArgumentException("Event To Concatenate is instanceof " + this.getClass().getName());
        } else if (!sameOriginator(event)) {
            throw new IllegalArgumentException("Attempting to Concatenate Events from two Seperate Originators");
        }

        RefreshEvent<OriginatorType, EventType> upCast = (RefreshEvent<OriginatorType, EventType>) event;
        appendEvent(upCast.evt_);
        return this;//new RefreshEvent(upCast, (EventType) this.evt_.concatenate(event));
    }

}
