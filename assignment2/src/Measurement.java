/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Zehua
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.*;
import java.math.*;

public class Measurement {
    
    /**
     * The name the file contains pagerank derived by MC
     */
    private static final String MC_FILENAME = "MC5";
    
    /**
     * The name the file contains pagerank derived by Power Iteration
     */
    private static final String PI_FILENAME = "pagerank";
    
    /**
     * The number of iterations
     */
    private String N = "";
    
    /**
     * The number of top ranked files
     */
    private int numOfFiles = 30;
    
    
    public Measurement() {
        calculateDifference();
    }
    
    private void calculateDifference() {
        ArrayList<Double> mcPagerank = readMCPageRank();
        ArrayList<Double> piPagerank = readPIPageRank();
        
        double sum = 0;
        for (int i = 0; i < numOfFiles; i++) {
            sum += Math.pow(piPagerank.get(i) - mcPagerank.get(i), 2);
        }
        saveMeasure(sum);
    }
    
    private ArrayList<Double> readPIPageRank() {
        ArrayList<Double> list = new ArrayList();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(PI_FILENAME));
            for (int i = 0; i < numOfFiles; i++) {
                String line = reader.readLine();
                String[] strs = line.split(";");
                list.add(Double.valueOf(strs[1]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return list;
    }
    
    private ArrayList<Double> readMCPageRank() {
        ArrayList<Double> list = new ArrayList();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(MC_FILENAME));
            for (int i = 0; i < numOfFiles; i++) {
                String line = reader.readLine();
                String[] strs = line.split(";");
                list.add(Double.valueOf(strs[1]));
                N = strs[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return list;
    }
    
    private void saveMeasure(double sum) {
        String fileName = MC_FILENAME + "-measure";
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(new File(fileName), true);
            String content = N + ";" + String.valueOf(sum) + "\r\n";
            output.write(content.getBytes());
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main( String[] args ) {
        new Measurement();
    }
}
