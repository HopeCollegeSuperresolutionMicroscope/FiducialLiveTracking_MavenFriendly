/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.ImageJmodifieds;

/**
 * Interface for Returning the Results of Slice Selection.  This may be extended to 
 * Ranges of Slices, preferential Slices, etc.  It is meant to be used as a controller
 * passed in by a model or calling context to affect the context's variables based on the 
 * assumption of the slices collected.
 * 
 * @author Justin Hanselman
 */
public interface TwoSliceSelector {
    
    
    /**
     * Action to Be Performed Given the Two slices.
     * <p>
     * Note: Slices are 1-based index
     * 
     * @param slice1
     * @param slice2 
     */
    public void setSlices( int slice1, int slice2 );
    
}
