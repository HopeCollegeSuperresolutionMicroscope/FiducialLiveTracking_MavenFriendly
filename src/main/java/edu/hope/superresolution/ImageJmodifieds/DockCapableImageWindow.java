/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.ImageJmodifieds;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import java.awt.Panel;
import net.miginfocom.swing.MigLayout;

/**
 *
 * General Class that Allows for docking extra panels into an ImageWindow.  This provides bette
 * UI and portability, by providing Docks to the East, West, and South of the main ImageWindow Canvas.
 * 
 * @author Justin Hanselman
 */
public class DockCapableImageWindow extends ImageWindow {
    
    DockCapableImageWindow( String title, ImPlusListenerDockablePanel dockEast, ImPlusListenerDockablePanel dockWest, ImPlusListenerDockablePanel dockSouth ) {
        super(title);
        
        layoutDockables( dockEast, dockWest, dockSouth );

    }
       
    DockCapableImageWindow( ImagePlus imp, ImPlusListenerDockablePanel dockEast, ImPlusListenerDockablePanel dockWest, ImPlusListenerDockablePanel dockSouth ) {
        super(imp, null);
        
        //layoutDockables( dockEast, dockWest, dockSouth );
    }
       
    DockCapableImageWindow( ImagePlus imp, ImageCanvas ic, ImPlusListenerDockablePanel dockEast, ImPlusListenerDockablePanel dockWest, ImPlusListenerDockablePanel dockSouth ) {
        super(imp, ic);
        
        layoutDockables( dockEast, dockWest, dockSouth );
        
    }
    
    
    
    //Sets the layout of the Window and Docks the Panels.  This is in response to possible overwrites from the ImageWindow
    private final void layoutDockables( ImPlusListenerDockablePanel dockEast, ImPlusListenerDockablePanel dockWest, ImPlusListenerDockablePanel dockSouth ) {
               // Override the default layout with our own, so we can do more 
       // customized controls. 
       // This layout is intended to minimize distances between elements.
       setLayout(new MigLayout("insets 1, fillx, filly",
         "[grow, fill]", "[grow, fill]related[]"));

       if( dockEast != null ){
            add( dockEast, "dock east");
            /*if( imPlus != null ) {
                dockEast.registerToPropertyNotifyingImagePlus( imPlus );
            }*/
       }
       if( dockWest != null ) {
           add( dockWest, "dock west");           
       }
       if( dockSouth != null ) {
           add( dockSouth, "dock south");
       }
    }
    
}
