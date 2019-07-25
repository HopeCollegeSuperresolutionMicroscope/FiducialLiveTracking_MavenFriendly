/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import edu.hope.superresolution.autofocus.FiducialAutoFocus;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class For Implementing a non-selective Wake-Sleep Guard. This is currently
 * meant to augment wait processes in Plugins or Plugin-filters or AutoFocus,
 * where sleeping the waiting in order to wait for a response from a GUI is
 * beneficial
 *
 * @author Justin Hanselman
 */
public class SpuriousWakeGuard {

    private volatile boolean waitGuard_ = false;
    private final Object syncObj_ = new Object();

    /**
     * Sleeps the thread and waits for {@link #doNotify() } to be called. Note,
     * waking is not selective or ordered so if multiple threads use the same
     * instance of this guard, then doNofify must be called multiple times.
     * <p>
     * Todo: Extend this class and method to count Threads for a slightly higher
     * overhead
     */
    public void doWait() {
        synchronized (syncObj_) {
            waitGuard_ = true;
            while (waitGuard_) {
                try {
                    syncObj_.wait(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(FiducialAutoFocus.class.getName()).log(Level.SEVERE, null, ex);
                    Thread.interrupted();
                }
            }
        }
    }

    /**
     * Notifies the object waiting on this instance to wake up. This is
     * non-selective and must be called multiple times for multiple waiting
     * parties if the interaction is not 1 to 1.
     */
    public void doNotify() {
        synchronized (syncObj_) {
            waitGuard_ = false;
            syncObj_.notify();
        }
        //We reset this in the event that multiple threads have latched on and may all need waitGuarding still
        waitGuard_ = true;
    }

}
