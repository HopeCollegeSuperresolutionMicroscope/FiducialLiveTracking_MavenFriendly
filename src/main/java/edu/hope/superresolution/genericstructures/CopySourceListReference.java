/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import edu.hope.superresolution.Utils.CopyUtils;

/**
 * 
 *   Wrapping Class that provides some level of separation from a list in a Thread Context
 *   This Call Provides a copy of the reference, while allowing 
 *   the appropriate calling context to access the reference for use in List Updates
 * 
 * @author Justin Hanselman
 */
public class CopySourceListReference<T, K> {
    
    private final T obj_;
    private final T listObjRef_;
    
    private final K listContextInstance_;
    
    public CopySourceListReference( T objRef, K listContextInstance ) {
        listObjRef_ = objRef;
        try {
            //Copy Value Into obj_
            obj_ = CopyUtils.abstractCopy( objRef );
        } catch (Exception ex ) {
            throw new RuntimeException( ex );
        }
        listContextInstance_ = listContextInstance;
    }
    
    public T getObject( ) {
        return obj_;
    }
    
    public T getListObjRef( K listContextInstance ) throws IllegalAccessException {
        if( listContextInstance_ != listContextInstance ) {
            throw new IllegalAccessException( "Attempt to Access CopySource Object made by different Instance " +
                    listContextInstance_.getClass().getName() + " and accessed from Instance of " +
                    listContextInstance.getClass() );
        }
        return listObjRef_;
    }
    
    public boolean belongsToContextInstance( K listContextInstance ) {
        return (listContextInstance_ == listContextInstance);
    }
    
    
}
