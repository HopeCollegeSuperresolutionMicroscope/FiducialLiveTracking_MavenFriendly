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
import ij.gui.ImageWindow;
import ij.gui.Toolbar;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Panel;
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
 *
 * General Class that Allows for docking extra panels Below an ImageWindow.  This provides better
 * UI and portability, by providing Docks to the East, West, and South of the main ImageWindow Canvas.
 * <p>
 * TODO: This should allow more than just underneath docking but the ImageLayout is very inflexible
 * 
 * @author Justin Hanselman
 */
public class UnderDockCapableImageWindow extends ImageWindow {
    
    public UnderDockCapableImageWindow( String title, ImPlusListenerDockablePanel dockSouth ) {
        super(title);
        
        dockAtBottom( dockSouth );

    }
       
    public UnderDockCapableImageWindow( PropertyNotifyingImagePlus imp, ImPlusListenerDockablePanel dockSouth ) {
        super( imp, null);
        
        dockAtBottom( dockSouth );
    }
       
    public UnderDockCapableImageWindow( PropertyNotifyingImagePlus imp, ImageCanvas ic, ImPlusListenerDockablePanel dockSouth ) {
        super(imp, ic);
        
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
