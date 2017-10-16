/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.autofocus;

/**
 * Abstract Implementation for Selector Actions that need to stall an autofocus
 * Thread.  This is because the limited UI of an Autofocus Plugin for Micromanager
 * does not allow for Graphical Elements.  As Such, the SpuriousWakeGaurdObject may be passed 
 * in object selection and then waited on after construction.  It is the burden of the 
 * extending classes to ensure all cases (closing a window, submitting, canceling) perform
 * signalWaitingObject();
 */
abstract public class FocusThreadLockSelector {

    private final SpuriousWakeGuard waitObject_;

    /**
     * General Constructor - assumes waitObject is only waiting on a single
     * thread
     * <p>
     * The thread passing this object, may call waitObject.wait() and can be
     * assured that the selectorAction will be called.
     *
     * @param waitObject
     */
    public FocusThreadLockSelector( SpuriousWakeGuard waitObject) {
        waitObject_ = waitObject;
    }

    /**
     * Call in subclasses to release the wait
     */
    protected void signalWaitingObject() {
        waitObject_.doNotify();
    }
}
