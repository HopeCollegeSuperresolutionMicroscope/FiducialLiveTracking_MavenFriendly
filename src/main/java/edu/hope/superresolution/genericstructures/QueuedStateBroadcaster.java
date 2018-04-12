/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SubClass of StateBroadcaster -- Represents an object that periodically notifies subscribers
 * of its state changes.  In this way, an Event Queue is populated with State Events by calling postToEventQueue()
 * and is then dispatched by calling dispatchEventQueue(). 
 * <p>
 * An event queue will only ever have 1 instance of a given sub-type of MasterSpecificEvent and
 * will concatenate each event to meet this requirement if multiple instances are posted.  Queues will
 * dispatch in the order of least recent to most recent.  (TODO: allow for specific ordering options).
 * <p>
 * Because a queue is indicative of a listening frequency, there may in fact be a need for separate queues
 * associated with separate notification schedules.  Because of this, queues are generated and 
 * produce queue handles that extending code may store and use according to these circumstances.  The 
 * lack of a removeQueueKey implies that queues should be setup once and then reused.  For this reason, 
 * it is encouraged to generate these queues in construction.
 * 
 * @author HanseltimeIndustries
 */
public abstract class QueuedStateBroadcaster extends StateBroadcaster {
   
    //Since we actually don't expect large numbers of different Queues, we will start at a low number of 4
    private final List<Map<String, MasterSpecificEvent>> queueList_ = Collections.synchronizedList(new ArrayList<Map<String, MasterSpecificEvent>>(4));

    /**
     * Generates a New Queue for use and returns the integer handle.  These Queues are not meant to be created for every time an event Queue is 
     * to be populated but for every situation that might require a unique Event Queue.
     * <p>
     * For Instance, if an Object has two methods, submitFinalizedAction1() and submitFinalizedAction2(),
     * the eventQueue for FinalizedAction1 may still be being populated by User actions, etc.  The subclass
     * would then
     * <code>
     * int action1Queue = generateNewQueue();
     * int action2Queue = generateNewQueue();
     * </code>
     * And would add different Events to different Queues as needed in the code.
     * 
     * @return The integer handle for the queue to be used in add, remove, and post Operations
     */
    protected final int generateNewQueue() {
        final Map<String, MasterSpecificEvent> newQueue = Collections.synchronizedMap(new LinkedHashMap<String, MasterSpecificEvent>(20, .75f, true));
        queueList_.add( newQueue );
        
        return queueList_.size() - 1;
    }
    
    /**
     * Posts an Event to The Event Queue.  
     * <p>
     * If an Event has already been queued, the new Event will be concatenated
     * with the old Event to produce only a single event.  See the corresponding Events for
     * details on their concatenation if curious.  Otherwise, know that only one Event of a type
     * will be present in the queue when it is dispatched.  Every new Event of the same type is significant 
     * to subscribers.
     * <p>
     * Note, posting and dispatching an event queue are synchronized to the eventQueues but does not use 
     * fairness.  In the event that posting and dispating are in such tight ordering, another solution may need to be provided.
     * TODO: Allow fairness Locks as an option for posting and dispatch synchronization.
     * 
     * @param evt - A MasterSpecific Event to Queue to this
     */
    protected final void postToEventQueue( int queueHandle, MasterSpecificEvent evt ) {

        Map<String, MasterSpecificEvent> eventQueue = queueList_.get(queueHandle);
        //Workaround for super.Constructors that call these methods as well
        if (eventQueue != null) {
            String queueKey = evt.getClass().getName();
            synchronized (eventQueue) {
                //Check to see if there is another corresponding Event and Update it.
                MasterSpecificEvent prevEvent = eventQueue.get(queueKey);
                if (prevEvent != null) {             
                    //Concatenate This Event into a new one, (prev calls on most current, see Doc)
                    evt = prevEvent.concatenate(evt);
                }
                eventQueue.put(queueKey, evt); //This Automatically Appends Order to the End for LinkedHashMap (most recent)
            }
        } else {
            throw new NullPointerException("Queue Handle " + queueHandle + " retrieves null Queue Object");
        }
        
    }
    
     /**
     * Test Method - For Retrieving Copy of EventQueue (Not Intended For Use outside of tests)
     * @return - Copy of the pendingEventQueue at the time it was called
     */
    protected final Map<String, MasterSpecificEvent> getCopyOfEventQueue( int queueHandle ) {
        Map<String, MasterSpecificEvent> eventQueue = queueList_.get(queueHandle);
        Map<String, MasterSpecificEvent> copy = new LinkedHashMap();
        //iterate through for a deep copy
        synchronized (eventQueue) {
            Iterator< Map.Entry<String, MasterSpecificEvent > > it = eventQueue.entrySet().iterator();
            Map.Entry< String, MasterSpecificEvent > entry;
            while( it.hasNext() ) {
                entry = it.next();
                copy.put( entry.getKey(), entry.getValue() );
            }

            return copy;
        }
    }
    
    /**
     * Dispatches all events in the EventQueue.  This is meant to be called only 
     * by endEntryContext() or any other functions that verify no other manipulation of the internal object.
     * <p>
     * Only 1 Type of Event is will be Dispatched thanks to Event concatenation in postToEventQueue().
     * Event Dispatch order is: least recent to most recent
     */
    protected final void dispatchEventQueue( int queueHandle ) {
        
        Map<String, MasterSpecificEvent> eventQueue = queueList_.get(queueHandle);
        synchronized (eventQueue) {
            //LinkedHashMap AccessOrder Guarantees first is the least recent Event, etc.
            for (MasterSpecificEvent e : eventQueue.values()) {
                bus_.post(e);
            }
            //Clear The pendingEventQueue_ (memory should depend on if Bus has sticky Posts)
            eventQueue.clear();
        }
    }
    
}
