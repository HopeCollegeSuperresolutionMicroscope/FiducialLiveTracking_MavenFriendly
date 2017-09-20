/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.exceptions;

/**
 *  Runtime Exception that is thrown if a Listener is a duplicate being registered to a class.
 *  This exception allows for programmers to check in multi-threaded situations if the
 *  registration is already done and determine if any relevant callbacks on registration
 *  were not called.  
 *  <p>
 *  Intended Specifically for ModelUpdateDispatcher registerModelUpdateListener().
 *
 * @see edu.hope.superresolution.models.ModelUpdateDispatcher#registerModelListener(edu.hope.superresolution.models.ModelUpdateListener) 
 * @author Desig
 */
public class DuplicateListenerException extends RuntimeException {
    
    /**
     * Constructs an <code>DuplicateListener</code> with no detail  message. 
     */
    public DuplicateListenerException() {
	super();
    }

    /**
     * Constructs an <code>DuplicateListenerException</code> with the 
     * specified detail message. 
     *
     * @param   s   the detail message.
     */
    public DuplicateListenerException(String s) {
	super(s);
    }
}
