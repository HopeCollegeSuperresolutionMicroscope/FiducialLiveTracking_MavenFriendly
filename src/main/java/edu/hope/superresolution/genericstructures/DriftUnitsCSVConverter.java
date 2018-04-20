/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import java.util.ResourceBundle;
import javafx.geometry.Point3D;

/**
 *
 * @author HanseltimeIndustries
 */
public class DriftUnitsCSVConverter <T> extends AbstractBeanField<T> {

    @Override
    protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
        return iDriftModel.DriftUnits.m;
    }
    
    @Override
    protected String convertToWrite( Object value ) throws CsvDataTypeMismatchException {
        
        String result = "";
        try {
          
            iDriftModel.DriftUnits drift = (iDriftModel.DriftUnits) value; 
            result = drift.toString();
            
        } catch(ClassCastException e) {
            CsvDataTypeMismatchException csve =
                    new CsvDataTypeMismatchException(ResourceBundle
                            .getBundle("convertGermanToBoolean", errorLocale)
                            .getString("field.not.boolean"));
            csve.initCause(e);
            throw csve;
        }
        
        return result;
    }
    
}
