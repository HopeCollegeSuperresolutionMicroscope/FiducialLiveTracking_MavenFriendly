/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

/**
 * 
 * A Container that holds a value and its uncertainty. This ensures 
 * that the value retrieved with Uncertainty is not overflown.
 *
 * @author Microscope
 */
public class NumberAndUncertaintyReporter {
    
    /**
     * Enumerated Values For the region associated with Value +/- Uncertainty 
     */
    public enum UncertaintyRegion {
        Upper, 
        Lower,
        Center
    }
    
    private double value_;
    private double uncertainty_;
    private boolean hasOverflow_;
    
    /**
     * Constructor
     * 
     * @param value
     * @param uncertainty - The uncertainty with the value, must be a positive number
     */
    public NumberAndUncertaintyReporter( double value, double uncertainty ) {
        
        value_ = value;
        uncertainty_ = uncertainty;
        checkForOverflow();
        
    }
    
    /**
     * Copy Constructor
     * 
     * @param copyObj 
     */
    public NumberAndUncertaintyReporter( NumberAndUncertaintyReporter copyObj ) {
        value_ = copyObj.value_;
        uncertainty_ = copyObj.uncertainty_;
        hasOverflow_ = copyObj.hasOverflow_;
    }
    
    /**
     * Whether or not the value and one of its Uncertainty Edges would breach overflow
     * 
     * @return 
     */
    public boolean hasOverflow() {
        return hasOverflow_;
    }
     
    /**
     * Sets the overflow flag if the number and its uncertainty will exceed max allowed values
     */
    private void checkForOverflow() {
        if (value_ > Double.MAX_VALUE - uncertainty_) {
            hasOverflow_ = true;
        } else if (value_ < Double.MIN_VALUE + uncertainty_) {
            hasOverflow_ = true;
        } else {
            hasOverflow_ = false;
        }
    }
    
    
     /**
     * Implemented to return the semantic value of the Previous Out of Focus Region
     * <p>
     * This accounts for the ability for something to overflow and will return the appropriate value
     * Double MAX in that case.
     * <p>
     * If you would like to know if the value is representative because it would overflow for real, use {@link #hasOverflow()}
     * 
     * @param region
     * @return 
     */    
    public double getValue( UncertaintyRegion region ) {


        switch( region ) {
            case Upper:
                if( value_ > Double.MAX_VALUE - uncertainty_ ) {
                    //redundant
                    hasOverflow_ = true;
                    return Double.MAX_VALUE;
                } else {
                    return value_ + uncertainty_;
                }
            case Lower:
                if( value_ < Double.MIN_VALUE + uncertainty_ ) {
                    hasOverflow_ = true;
                    return Double.MIN_VALUE;
                } else {
                    return value_ - uncertainty_;
                }
            case Center:
                return value_;
            default:
                throw new IllegalArgumentException( region.toString() + " is not a valid argument for ScoreRegion");
        }
    }
    
    /**
     * Gets the nominal value without uncertainties
     * 
     * @return 
     */
    public double getValue( ) {
       return value_; 
    }
    
    /**
     * Returns just the value of the uncertainty
     * @return 
     */
    public double getUncertainty( ) {
        return uncertainty_;
    }
    
    /**
     * Sets the Value and its uncertainty
     * 
     * @param value
     * @param uncertainty 
     */
    public void setValueAndUncertainty( double value, double uncertainty ) {
        value_ = value;
        uncertainty_ = uncertainty;
        checkForOverflow();
    }
    
    /**
     * Sets the Value and Uncertainty equal to a previous instance
     * @param copyObj 
     */
    public void setValueAndUncertainty( NumberAndUncertaintyReporter copyObj ) {
        value_ = copyObj.value_;
        uncertainty_ = copyObj.uncertainty_;
        hasOverflow_ = copyObj.hasOverflow_;
    }
    
}
