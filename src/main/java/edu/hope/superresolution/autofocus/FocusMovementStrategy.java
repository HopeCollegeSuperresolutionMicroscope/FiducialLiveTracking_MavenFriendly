/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.autofocus;

/**
 *  Interface for Movement Toward Focus.  The need for such an interface, allows 
 *   for the strategizing of other particular movement techniques.  This is particularly 
 *   important with low cost adapter solutions.  Such as qualitative upward focus with
 *   a limiting threshold.
 * 
 * @author Justin Hanselman
 */
public interface FocusMovementStrategy {
        
    /**
     *  This Function should perform the Hardware seek Sequence when a plane is discovered out of focus.
     * 
     * @return 
     */
    public boolean seekSequence( );
    
}
