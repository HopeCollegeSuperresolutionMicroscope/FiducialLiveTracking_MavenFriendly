/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import java.util.List;

/**
 *  Callback Interface for Interacting with List Objects that are being filled
 * 
 * @author Microscope
 */
public interface ListCallback<T> {
    
    //Generic Function if the list is filled and no longer operated on
    void onListFull( List<T> list );
    
    //Generic Function if an element is Added
    void onListElementAdded( List<T> list );
    
    //Generic Function if an element is removed
    void onListElementRemoved( List<T> list );
    
}
