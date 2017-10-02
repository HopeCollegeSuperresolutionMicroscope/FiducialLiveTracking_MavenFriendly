/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.ImageJmodifieds;

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;
import ij.gui.ImageCanvas;
import ij.gui.StackWindow;
import ij.gui.Toolbar;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;
import net.miginfocom.swing.MigLayout;
import org.micromanager.MMStudio;
import org.micromanager.imagedisplay.MMCompositeImage;
import org.micromanager.utils.CanvasPaintPending;

/**
 * General Class that Allows for docking extra panels below into an StackWindow.  This provides better
 * UI and portability, by providing Docks to the East, West, and South of the main ImageWindow Canvas.
 * <p>
 * TODO: This should allow more than just underneath docking but the ImageLayout is very inflexible
 * 
 * @author Desig
 */
public class UnderDockCapableStackImageWindow extends StackWindow {

    public UnderDockCapableStackImageWindow(PropertyNotifyingImagePlus propImPlus, ImPlusListenerDockablePanel dockSouth) {
        super(propImPlus, null);
        
        dockAtBottom( dockSouth );
    }

    public UnderDockCapableStackImageWindow(PropertyNotifyingImagePlus propImPlus, ImageCanvas ic, ImPlusListenerDockablePanel dockSouth ) {
        super(propImPlus, ic);

        dockAtBottom( dockSouth );
    }
    
    //Sets the layout of the Window and Docks the Panels.  This is in response to possible overwrites from the ImageWindow
    final public void dockAtBottom( ImPlusListenerDockablePanel dockSouth ) {
        
       final ImagePlus imPlus = getImagePlus();
       
       if( dockSouth != null ) {
           add( dockSouth );
           if( imPlus != null && imPlus instanceof PropertyNotifyingImagePlus ) {
               dockSouth.registerToPropertyNotifyingImagePlus((PropertyNotifyingImagePlus)imPlus);
           }           
       }
       
       pack();
    }
    
}
