/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir;

import static ir.PersistentHashedIndex.TABLESIZE;
import java.io.File;
import java.io.RandomAccessFile;

/**
 *
 * @author Zehua
 */
public class MergingThread implements Runnable{
    
    private String dictionary_file_name = "";
    private String data_file_name = "";
    private String dictionary_fromfile_name = "";
    private String data_fromfile_name = "";
    private String dictionary_address_file_name = "";
    private String data_address_file_name = "";
    
    private String directory = "";
    
    RandomAccessFile dictionaryFileTo = null;
    RandomAccessFile dataFileTo = null;
    RandomAccessFile dictionaryFileFrom = null;
    RandomAccessFile dataFileFrom = null;
    RandomAccessFile dictionaryAddressFile = null;
    RandomAccessFile dataAddressFile = null;
    
    
    public MergingThread() {
        directory = "." + File.separator + "merge" + File.separator;
    }
    
    @Override
    public void run() {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        
        //String 
        try {  
            //dictionaryFile
            mergeDictionaryFile();
            mergeDataFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void mergeDictionaryFile() throws Exception {
        String str_dictionary_address = null;
        dictionaryFileTo = new RandomAccessFile(directory + dictionaryFileTo, "rw");
        dictionaryAddressFile = new RandomAccessFile(directory + dictionaryAddressFile, "rw");
        dictionaryFileFrom = new RandomAccessFile(directory + dictionaryAddressFile, "rw");
        
        while ((str_dictionary_address = dictionaryAddressFile.readLine()) != null) {
            dataFileTo.seek(Long.valueOf(str_dictionary_address));
            dataFileTo.writeLong(dataFileFrom.readLong());
        }
        
        dictionaryAddressFile.close();
        dictionaryFileTo.close();
    }
    
    private void mergeDataFile() throws Exception {
        String str_data_address = null;
        dataFileFrom = new RandomAccessFile(directory + dataFileFrom, "rw");
        dataFileTo = new RandomAccessFile(directory + dataFileTo, "rw");
        dataAddressFile = new RandomAccessFile(directory + dataAddressFile, "rw");
        
        while ((str_data_address = dataAddressFile.readLine()) != null) {
            dataFileTo.seek(Long.valueOf(str_data_address));
            dataFileTo.writeBytes(dataFileFrom.readLine());
        }
        
        dataFileTo.close();
        dataAddressFile.close();     
    }
    
    private int seed = 131313;
    
    private long getHashCode(String term) throws Exception{
        char[] term_string = term.toCharArray();
        long hashcode = 0;
        
        long hash = 0;
        for (int i = 0; i < term_string.length; i++) {
            hash = hash * seed + term_string[i];
        }
        
        hashcode = hash % TABLESIZE;
        
        return Math.abs(hashcode);
    }
    
    public String getDictionary_file_name() {
        return dictionary_file_name;
    }

    public void setDictionary_file_name(String dictionary_file_name) {
        this.dictionary_file_name = dictionary_file_name;
    }

    public String getData_file_name() {
        return data_file_name;
    }

    public void setData_file_name(String data_file_name) {
        this.data_file_name = data_file_name;
    }
    
    public String getDictionary_address_file_name() {
        return dictionary_address_file_name;
    }

    public void setDictionary_address_file_name(String dictionary_address_file_name) {
        this.dictionary_address_file_name = dictionary_address_file_name;
    }

    public String getData_address_file_name() {
        return data_address_file_name;
    }

    public void setData_address_file_name(String data_address_file_name) {
        this.data_address_file_name = data_address_file_name;
    }
    
    public String getDictionaryFrom() {
        return dictionary_fromfile_name;
    }

    public void setDictionaryFrom(String dictionary_fromfile_name) {
        this.dictionary_fromfile_name = dictionary_fromfile_name;
    }

    public String getDataFrom() {
        return data_fromfile_name;
    }

    public void setDataFrom(String data_fromfile_name) {
        this.data_fromfile_name = data_fromfile_name;
    }
}
