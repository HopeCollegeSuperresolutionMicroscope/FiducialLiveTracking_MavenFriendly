/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.exceptions;

/**
 *
 * @author Microscope
 */
public class NoFiducialException extends RuntimeException {
    
    /**
     * Constructs an <code>NoFiducialException</code> with no detail  message. 
     */
    public NoFiducialException() {
	super();
    }

    /**
     * Constructs an <code>NoFiducialException</code> with the 
     * specified detail message. 
     *
     * @param   s   the detail message.
     */
    public NoFiducialException(String s) {
	super(s);
    }
    
}
