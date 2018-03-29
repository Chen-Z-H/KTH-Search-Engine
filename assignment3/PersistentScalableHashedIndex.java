/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir;

import java.io.File;
import java.io.RandomAccessFile;

/**
 *
 * @author Zehua
 */
public class PersistentScalableHashedIndex extends PersistentHashedIndex{
    private final int MAX_SIZE = 100000;
    private final long TABLESIZE = 3500000L;
    private int tokenCount = 0;
    
    private final String DIRECTORY = "." + File.separator + "merge" + File.separator;
    private final String DICTIONARY_NAME = "dictionary";
    private final String DATA_NAME = "data";
    
    RandomAccessFile dictionaryFile = null;
    RandomAccessFile dataFile = null;
    
    
    public PersistentScalableHashedIndex() {
        super();
        try {
            dictionaryFile = new RandomAccessFile(DIRECTORY + DICTIONARY_NAME, "rw");
            dataFile = new RandomAccessFile(DIRECTORY + DATA_NAME, "rw");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    @Override
    public void insert( String token, int docID, int offset ) {
        
        long filling = 0;
        
        try {
            for (int i = 0; i < TABLESIZE; i++) {
                dictionaryFile.writeLong(filling);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (index.size() == MAX_SIZE) {
            cleanup();
        }
        MergingThread merge = new MergingThread();
        new Thread(merge).start();
    }
    
    @Override
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        writeIndex();
        System.err.println( "done!" );
    }
}
