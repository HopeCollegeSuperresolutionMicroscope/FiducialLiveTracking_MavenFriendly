/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

/**
 *
 * @author Microscope
 */
public interface FitThreadCallback< T > extends ListCallback< T > {
    
    //Clean Up CallBack For When the Thread Ends
    public void onThreadEnd();
    
    //If the Thread is stopped early, this should be Invoked
    public void onThreadStop();
}
