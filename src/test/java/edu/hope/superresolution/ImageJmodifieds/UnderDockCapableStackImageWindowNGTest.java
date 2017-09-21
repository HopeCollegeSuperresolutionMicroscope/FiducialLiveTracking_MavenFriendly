/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.ImageJmodifieds;

import edu.hope.superresolution.views.TestSliceInfoDisplayPanel;
import ij.ImageStack;
import ij.gui.StackWindow;
import java.awt.Dialog;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author Desig
 */
public class UnderDockCapableStackImageWindowNGTest {
    
    public UnderDockCapableStackImageWindowNGTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    @Test
    public void testSomeMethod() {
        System.out.println("Testing Displaying Slices In a Dock");
        final Object UIlock = new Object();
        ImageStack iStack = ImageStack.create( 400, 400, 6, 16 );
        PropertyNotifyingImagePlus ip = new PropertyNotifyingImagePlus( "This is the title", iStack );
        TestSliceInfoDisplayPanel displayPanel = new TestSliceInfoDisplayPanel( UIlock );
        StackWindow win = new UnderDockCapableStackImageWindow( ip, displayPanel );
        try {
            synchronized( UIlock ) {
                UIlock.wait();
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(UnderDockCapableStackImageWindowNGTest.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            
        }
    }
    
}
