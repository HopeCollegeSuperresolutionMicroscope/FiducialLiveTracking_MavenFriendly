/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.exceptions;

import ij.gui.Roi;

/**
 *
 * @author Microscope
 */
public class NoFiducialException extends RuntimeException {
    
    private Roi fidSearchArea_ = null;
    private int fidAreaIndex_ = -1;
    
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
    
    /**
     * Constructor - Caused by Fiducial Search Area failure to find a Fiducial
     * 
     * @param s - Error message
     * @param fidSearchArea - The Roi in which the failed search occurred
     */
    public NoFiducialException(String s, Roi fidSearchArea ) {
        super(s);
        fidSearchArea_ = fidSearchArea;
    }
    
    /**
     * Constructor - Caused By Fiducial Search Area failure in a FiducialLocationModel
     * 
     * @param s - Error Message
     * @param fidSearchArea - The roi in which the failed search occurred
     * @param fidAreaIndex - The index in the FiducialLocation model of the Fiducial Area
     */
    public NoFiducialException( String s, Roi fidSearchArea, int fidAreaIndex ) {
        super(s);
        fidSearchArea_ = fidSearchArea;
        fidAreaIndex_ = fidAreaIndex;
    }
    
    /**
     * Get the search area that failed to trigger this
     * 
     * @return An Roi if one was the cause of the trigger, or null
     */
    public Roi getSearchArea() {
        return fidSearchArea_;
    }
    
    /**
     * Get the index of the Location that no Fiducial was found
     * 
     * @return The zero-based index of the FiducialModel's FiducialAreas that corresponds to the failure, or -1 if not important
     *          Note: If this occurs during construction, this is simply a reference for the sake of tracking any previous
     *                FiducialLocationModels that the failed model was based off of.
     * 
     */
    public int getFiducialLocationModelIndex() {
        return fidAreaIndex_;
    }
    
}
