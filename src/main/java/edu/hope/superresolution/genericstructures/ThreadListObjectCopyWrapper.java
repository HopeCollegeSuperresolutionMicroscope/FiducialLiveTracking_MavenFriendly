/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import edu.hope.superresolution.Utils.CopyUtils;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 *  Wrapper Class for an Object Instance to be Passed Back from A ThreadList
 *    This Class Provided index Information as well as a separated Object from the List
 *    This Class may be used for Lists of Non-Thread Safe Objects
 * 
 * @author Microscope
 */
public class ThreadListObjectCopyWrapper<T> {
    
    private T obj_;
    private int index_;
    
    private final ThreadSafeSelectionList listOwnerRef_;
    private final ReentrantLock objectIdxLock_;
    
    public ThreadListObjectCopyWrapper( ThreadSafeSelectionList listOwner, ReentrantLock idxLock, T obj, int index ) {
        try {
            //Copy Value Into obj_
            obj_ = CopyUtils.abstractCopy( obj );
        } catch (Exception ex ) {
            throw new RuntimeException( ex );
        }
        listOwnerRef_ = listOwner;
        index_ = index;
        objectIdxLock_ = idxLock;
    }
    
    public synchronized void indexUpdated( int increment, ThreadSafeSelectionList callerRef ) {
        if( listOwnerRef_ == callerRef ) {
            objectIdxLock_.lock();
            try{
                index_ += increment;
            } finally {
                objectIdxLock_.unlock();
            }
        }
    }
    
    public T getObject() {
        return obj_;
    }
    
    public synchronized Integer getIndex() {
        Integer temp = -1;
        objectIdxLock_.lock();
        try {
            temp = index_;
        }
        finally {
            objectIdxLock_.lock();
        }
        return temp; 
    }
    
    public boolean belongsToListOwner( ThreadSafeSelectionList listTest ) {
        return (listOwnerRef_ == listTest);
    }
    
    //Function to Dereference the Object 
    //  This object Does Not Guarantee that other references to this object do not exist
    //  This will however, remove the pointer from the ThreadSafeSelectinList so it is open for garbageCollection
    // used like  ThreadListObjectCopyWrapper obj = obj.dispose( );
    public ThreadListObjectCopyWrapper<T> dispose() {
        listOwnerRef_.safeObjectDispose(this);
        return null;
    }
    
}
