/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

/**
 *
 * Interface to guarantee copy methods
 * 
 * @author Desig
 */
public interface iSettingsObject < SubClass extends iSettingsObject > {
    
    public SubClass copy( SubClass source );
    
}
