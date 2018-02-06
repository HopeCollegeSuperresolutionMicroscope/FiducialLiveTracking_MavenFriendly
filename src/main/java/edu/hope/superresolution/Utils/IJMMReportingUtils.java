/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.Utils;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.micromanager.MMStudio;
import org.micromanager.utils.ReportingUtils;

/**
 *  Wrapper Class Used for determining if Micro-manager Logging Utils should be used or ImageJ
 *  TODO: should be scaled to be comprehensive for ImageJ.
 * 
 * @author Owner
 */
public class IJMMReportingUtils {

    public static Logger LOGGER = Logger.getLogger(IJMMReportingUtils.class.getName());
    public static boolean asMMPlugin_ = true;
    
    //Detect if Micromanager is available or if this is just ImageJ
    static {

        try {
            Class<?> mmStudio = Class.forName("org.micromanager.MMStudio");
        } catch (ClassNotFoundException ex) {
            asMMPlugin_ = false;
        }

    }
    
    /**
     * Returns true is Micro-manager source code is available and there is a running
     * Micro-manager Instance.  Used to determine if Micro-manager Reporting is used instead of ImageJ
     * @return Whether or not Micro-manager has a running instance
     */
    public static boolean isMMRunning() {
        return asMMPlugin_ && (MMStudio.getInstance() != null);
    }
   
    public static void showMessage( String msg ) {
        if( isMMRunning() ) {
            ReportingUtils.showMessage(msg);
        } else {
            ij.IJ.showMessage(msg);
        }
    }
    
   public static void logError(Throwable e, String msg) {

      if( isMMRunning() ) {
          ReportingUtils.logError(e, msg);
      } else {
          if( e != null ) {
            ij.IJ.handleException(e);
          } else {
             ij.IJ.log(msg);
          }
      }
   }

   public static void logError(Throwable e) {
      logError(e, "");
   }

   public static void logError(String msg) {
      logError(null, msg);
   }

   public static void showError(Throwable e, String msg) {
      if(isMMRunning()) {
          ReportingUtils.showError(e, msg);
      } else {
          //Use the error logging already developed
        logError(e, msg);
        ij.IJ.error(msg);
      }
      
    }

   public static void showError(Throwable e) {
      showError(e, "");
   }

   public static void showError(String msg) {
      showError(null, msg);
   }
}
