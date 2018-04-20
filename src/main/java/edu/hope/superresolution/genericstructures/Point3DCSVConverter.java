/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.hope.superresolution.genericstructures;

import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvConstraintViolationException;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import java.util.List;
import java.util.ResourceBundle;
import javafx.geometry.Point3D;

/**
 *
 * @author HanseltimeIndustries
 */
public class Point3DCSVConverter<T> extends AbstractBeanField<T> {

    @Override
    protected Object convert(String value) throws CsvDataTypeMismatchException, CsvConstraintViolationException {
        String[] strs = value.split(",");
        return new Point3D(Integer.parseInt(strs[0]), Integer.parseInt(strs[1]), Integer.parseInt(strs[2]) );
    }
    
    @Override
    protected String convertToWrite( Object value ) throws CsvDataTypeMismatchException {
        
        String result = "";
        try {
          
            Point3D point = (Point3D) value; 
            result = point.getX() + "," + point.getY() + "," + point.getZ();
            
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
