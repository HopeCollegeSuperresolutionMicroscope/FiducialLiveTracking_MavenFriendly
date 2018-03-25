/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.exceptions;

/**
 *
 * @author Owner
 */
public class NoTrackException extends RuntimeException {
    
    private NoFiducialException[] noFidExceptions_ = null;
    
    public NoTrackException( ) {
        super();
    }
    
    /**
     * Constructor - Generic Message
     * 
     * @param msg - Error Message
     */
    public NoTrackException( String msg ) {
           super(msg);
    }
    
    /**
     * Constructor - NoFiducialException Caused Exception
     * 
     * @param msg - Error Message
     * @param noFidExceptions - No Fiducial Exceptions that caused this exception
     */
    public NoTrackException( String msg, NoFiducialException[] noFidExceptions ) {
        super(msg);
        noFidExceptions_ = noFidExceptions;
    }
    
    /**
     * Get any NoFiducialExceptions that triggered a NoTrackException
     * 
     * @return An array of NoFiducialExceptions or null if none triggered the exception 
     */
    public NoFiducialException[] getFiducialExceptions() {
        return noFidExceptions_;
    }
    
    
}
