/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author Justin Hanselman
 */
public class ThreadSafeSelectionList<E> {

    private final List<E> delegate = Collections.synchronizedList( new ArrayList<E>() );
    private final Map< Integer, ArrayList< ThreadListObjectCopyWrapper<E> > > registeredCopyWrappers_ = new TreeMap< Integer, ArrayList< ThreadListObjectCopyWrapper<E> > >();
    private int selectedIndex_ = -1;
    private final Object selectableLock_ = new Object();  //Lock to make sure selectedIndex is used
    
    private final ReentrantLock safeObjMethodLock_ = new ReentrantLock();
    
    public int size() {
        return delegate.size();
    }

    public Object get(int index) {
       return delegate.get(index);
    }
    
    //Manipulation Of Underlying Object May not Be Thread Safe
    public E getSelectedObjectRef( ) {
        synchronized( selectableLock_ ) {
            if( selectedIndex_ < 0 ) {
                return null;
            }
            return delegate.get(selectedIndex_);
        }
    }
    
    public ThreadListObjectCopyWrapper<E> getSelectedObjectCopy() {
        synchronized( selectableLock_ ) {
            if( selectedIndex_ < 0 ) {
                return null;
            }
            return new ThreadListObjectCopyWrapper( this, safeObjMethodLock_, delegate.get( selectedIndex_ ), selectedIndex_ );
        }
    }
    
    public void setSelectedObject( E obj ) {
        synchronized( selectableLock_ ) {
            int idx = delegate.indexOf( obj );
            selectedIndex_ = idx;
        }
    }
    
    public boolean add( E obj ) {
        boolean res = delegate.add( obj );
        //Probably Should be updating Map Size to spread out operation times
        return res;
    }

    //This Method is Used to Add a Reference to a ThreadList Object that previously Instantiated in the List
    //  This Applies to dispose() with multiple references or remove() operations
    private boolean ReAddSafeObject( ThreadListObjectCopyWrapper<E> safeObject ) {
        if( !safeObject.belongsToListOwner(this) ) {
            return false;
        }
        
        safeObjMethodLock_.lock(); // lock all Safe Object for this Thread From operation
        try {
            if (!registeredCopyWrappers_.containsKey(safeObject.getIndex())) {
                ArrayList< ThreadListObjectCopyWrapper<E>> list = new ArrayList< ThreadListObjectCopyWrapper<E>>();
                list.add(safeObject);
                registeredCopyWrappers_.put(safeObject.getIndex(), list);
                safeObjectIndexInc(safeObject.getIndex());
                return true;
            }
        } finally {
            safeObjMethodLock_.unlock();
        }
        //registeredCopyWrappers_.get( safeObject.getIndex() ).add(safeObject);
        //The Only Removal that would Occur, would be if the index was taken out
        return false;
    }
    
    private void safeObjectIndexInc( Integer startIndex ) {
        Set< Integer > keys = registeredCopyWrappers_.keySet();
        Integer lastIdxKey = -1;
        for( Integer safeObjIdxKey : keys ) {
            if( safeObjIdxKey >= startIndex ) {
                for( ThreadListObjectCopyWrapper<E> safeObj : registeredCopyWrappers_.get(safeObjIdxKey) ) {
                    safeObj.indexUpdated( 1 , this);
                }
                if( lastIdxKey >= 0 ) {   
                    registeredCopyWrappers_.put( lastIdxKey + 1, registeredCopyWrappers_.remove( lastIdxKey ) );
                }
                lastIdxKey = safeObjIdxKey;
            }
        }
        
        //Increment Final Last Idx As well
        if( lastIdxKey >= 0 ) {   
            registeredCopyWrappers_.put( lastIdxKey + 1, registeredCopyWrappers_.remove( lastIdxKey ) );
        }
        
    }
    
    private void safeObjectIndexDec( Integer startIndex ) {
        Set< Integer > keys = registeredCopyWrappers_.keySet();
        Integer lastIdxKey = -1;
        for( Integer safeObjIdxKey : keys ) {
            if( safeObjIdxKey >= startIndex ) {
                for( ThreadListObjectCopyWrapper<E> safeObj : registeredCopyWrappers_.get(safeObjIdxKey) ) {
                    safeObj.indexUpdated( -1, this);
                }
                if( lastIdxKey > 0 ) {
                    registeredCopyWrappers_.put( lastIdxKey - 1, registeredCopyWrappers_.remove( lastIdxKey ) );
                }
                lastIdxKey = safeObjIdxKey;
            }
        }
        //Increment Final Last Idx As well
        if( lastIdxKey > 0 ) {   
            registeredCopyWrappers_.put( lastIdxKey - 1, registeredCopyWrappers_.remove( lastIdxKey ) );
        }
    }
    
    public E remove( ThreadListObjectCopyWrapper<E> safeObject ) {
        if( !safeObject.belongsToListOwner(this) ) {
            return null;
        }
        
        safeObjMethodLock_.lock();
        try {

            //If Something Happened to the Index, Someone Abused indexUpdate or its an already Removed Object
            if (safeObject.getIndex() >= 0) {
                return remove(safeObject.getIndex());
            }
        } finally {
            safeObjMethodLock_.unlock();
        }

        return null;
        
    }
    
    public E remove( int idx ) {
        synchronized (selectableLock_) {
            if (idx <= selectedIndex_) {
                //Just Bump the Index Down One, even if removing the same
                --selectedIndex_;
            }
            unregisterAllSafeObjects( idx );
            return delegate.remove(idx);
        }
    }
    
    
    
    private void unregisterAllSafeObjects( Integer idxKey ) {
        safeObjMethodLock_.lock();
        try {
            for (ThreadListObjectCopyWrapper<E> safeObj : registeredCopyWrappers_.get(idxKey)) {
                safeObj.indexUpdated(-1 * (idxKey + 1), this);
            }
            registeredCopyWrappers_.remove(idxKey);
            safeObjectIndexDec(idxKey);
        } finally {
            safeObjMethodLock_.unlock();
        }
        
    }
    
    public void safeObjectDispose( ThreadListObjectCopyWrapper<E> safeObj ) {
        if( !safeObj.belongsToListOwner(this) ) {
            return;
        }
        safeObjMethodLock_.lock();
        try {
            registeredCopyWrappers_.get( safeObj.getIndex() ).remove(safeObj);
        } finally {
            safeObjMethodLock_.unlock();
        }
    }
    
}
