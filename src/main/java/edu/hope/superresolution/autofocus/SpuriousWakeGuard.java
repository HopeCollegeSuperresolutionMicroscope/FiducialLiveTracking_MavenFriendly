/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.autofocus;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple Implementation of an Object that protects against spurious wakeups.  This 
 * Guard was intended for use with Micro-manager AutoFocus Plugins that need to wait on GUI
 * elements.  Ideally, Autofocus should have an init() method, but since it only uses fullfocus, 
 * detection of a new session requires waiting the fullfocus method on various GUI windows and their 
 * results.
 *
 * @author Desig
 */
public class SpuriousWakeGuard {
        
        private boolean waitGuard_ = false;
        private final Object syncObj_ = new Object();
        
        public void doWait() {
            synchronized (syncObj_) {
                waitGuard_ = true;
                while (waitGuard_) {
                    try {
                        super.wait(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(FiducialAutoFocus.class.getName()).log(Level.SEVERE, null, ex);
                        Thread.interrupted();
                    }
                }
            }
        }
        

        public void doNotify() {
            synchronized (syncObj_) {
                waitGuard_ = false;
                super.notify();
            }
        }
        
    }
