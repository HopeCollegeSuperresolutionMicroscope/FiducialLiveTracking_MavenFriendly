/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

/**
 *  Generic Interface meant to be Implemented for a given Blocking Queue.  This 
 *   interface allows for complex testing of any blocking queue object to be 
 *   encapsulated so that any using class, may switch BlockingQueues and only expect
 *   a new BlockingQueueEndConditionTest instance.
 * <p>
 *  This Class is used for construction of a super FitStackThread Class, so that 
 *  extensions have a standard (and consistent) way of stopping when operating on 
 *  BlockingQueue.
 * 
 * @param <QueueDataType> The DataType expected to be passed (should be the same datatype
 *                        of the BlockingQueue expected.
 * 
 * @see edu.hope.superresolution.fitters.FitStackThread
 * @see java.util.concurrent.BlockingQueue
 * 
 * @author Justin Hanselman
 */
public interface BlockingQueueEndConditionTest<QueueDataType> {
    
    /**
     *  Boolean Condition that is called for a given Queue Object. The pertinent
     *   Logic is performed within to determine whether the object is QueueEndCondition
     * <p>
     * Used In FitStackThread super Class to Determine Exit conditions
     * 
     * @param queueObj The Object that was pulled from a BlockingQueue of the given type
     * @return <code>true</code> if the data type is the expected end condition
     * 
     * @see edu.hope.superresolution.fitters.FitStackThread
     * @see edu.hope.superresolution.fitters.FitStackThread#run
     */
    public boolean isEndCondition( QueueDataType queueObj );
    
}
