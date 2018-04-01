/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.Utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 *
 *  Static Utility Class that Provides Interaction with Copy-Constructor Patterned Objects
 * 
 * 
 * @author Justin Hanselman
 */
public class CopyUtils {
    
    
    //This Class Produces a copy of the full from an AbstractReference
    //  Requires that the Class Implement a Copy Constructor (i.e. SubClass( Subclass src ) )
    //  Throws All Exceptions Associated with an object that does not have the given constructor
    //  Recommended - That All Errors should Be Non-Recoverable, Indicative of Poor Programming
    static public <Abstr, Sub extends Abstr> Sub abstractCopy( Abstr abstrClassObj ) throws Exception {
        Class subClass = abstrClassObj.getClass();
        Sub subClassObj; 
        try {
            //There must be a copy constructor implemented for the Object
            Constructor constructor = subClass.getConstructor( subClass );
            subClassObj = (Sub) constructor.newInstance( abstrClassObj );
        } catch ( IllegalAccessException ex ) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (InstantiationException ex) {
            throw ex;
        } catch (NoSuchMethodException ex) {
            throw ex;
        } catch (SecurityException ex) {
            throw ex;
        } catch (InvocationTargetException ex) {
            throw ex;
        }

        return subClassObj;
        
    }
    
}
