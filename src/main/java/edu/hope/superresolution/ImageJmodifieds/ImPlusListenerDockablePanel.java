/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.ImageJmodifieds;

import java.awt.Panel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This is a awt.Panel Element that Listens to the Event Buses of a PropertyNotifyingImagePlus
 * and updates its associated classes with the data.  This is meant to be docked with the DockCapableImageWindow.
 *
 * @author Desig
 */
public abstract class ImPlusListenerDockablePanel extends Panel implements IPropImPlusSelfSubscriber {
    
    ImPlusListenerDockablePanel() {}
    
}
