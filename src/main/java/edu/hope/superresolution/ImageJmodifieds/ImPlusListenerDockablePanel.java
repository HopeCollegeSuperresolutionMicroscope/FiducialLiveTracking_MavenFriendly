/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.ImageJmodifieds;

import java.awt.Panel;

/**
 * This is an abstract awt.Panel Element that Listens to the Event Buses of a PropertyNotifyingImagePlus
 * and updates its associated classes with the data.  This is meant to be docked with the DockCapableImageWindow.
 *<p>
 * Currently Just Abstract Categorization For Differentiation of IPropImPlusSelfSubscriber and a Panel Implementing it.
 * 
 * @author Justin Hanselman
 */
public abstract class ImPlusListenerDockablePanel extends Panel implements IPropImPlusSelfSubscriber {
    
    //Nullary Constructor
    public ImPlusListenerDockablePanel() {}
    
}
