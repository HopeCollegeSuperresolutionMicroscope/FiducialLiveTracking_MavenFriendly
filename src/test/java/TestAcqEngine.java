
import java.util.logging.Level;
import java.util.logging.Logger;
import org.testng.annotations.Test;
import org.micromanager.AcquisitionEngine2010;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Microscope
 */
public class TestAcqEngine {
    
    @Test
    public void testPointer() {
        try {
            System.out.println("Testing If the AcquisitionEngine is found");
            AcquisitionEngine2010 testEng;
            
            Class.forName("org.micromanager.AcquisitionEngine2010");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(TestAcqEngine.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Didn't find class");
        }
    }
    
}
