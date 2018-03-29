/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;
import java.math.*;


/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The dictionary file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;  // 50,000th prime number

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();
    
    //
    short[] hashTableFlags = new short[(int)TABLESIZE];


    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */ 
    public class Entry {
	//
	//  YOUR CODE HERE
	//
        private String key;
        private long address;

        public Entry(String key, long address) {
            this.key = key;
            this.address = address;
        }
    }


    // ==================================================================

    
    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created. 
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
        try {
            readDocInfo();
        }
        catch ( FileNotFoundException e ) {
        }
        catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */ 
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr ); 
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */ 
    String readData( long ptr, int size ) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[size];
            dataFile.readFully( data );
            return new String(data);
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file. 
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry( Entry entry, long ptr ) {
	//
	//  YOUR CODE HERE
	//
        int entry_size = 8;         //a type long variable has 8 bytes
        try {
            dictionaryFile.seek(free + ptr);
            dictionaryFile.writeLong(entry.address);
            System.out.println(entry.key + ": " + ptr);
        } catch (Exception e) {
            e.printStackTrace();
            //System.out.println(entry.key + " " + ptr);
        }
       
    }
    
    private int seed = 131;
    
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
    
    private long getStorePos(String term) {
        long hashcode = 0;
        try {
            hashcode = getHashCode(term);
            while (hashTableFlags[(int)hashcode] == 1) {
                collisions++;
                if (hashcode == (hashTableFlags.length - 1)) {
                    hashcode = 0;
                } else {
                    hashcode++;
                }       
            }
            hashTableFlags[(int)hashcode] = 1;
        } catch (Exception e) {
            System.out.println(term + " " + hashcode);
            e.printStackTrace();
        }
        return hashcode;
    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry( long ptr ) {   
	//
	//  REPLACE THE STATEMENT BELOW WITH YOUR CODE 
	//
        long dictionary_address = 0;
        Entry entry = null;
        
        try {
            dictionaryFile.seek(free + ptr);
            dictionary_address = dictionaryFile.readLong();
            entry = new Entry(getTokenFromDataFile(dictionary_address), dictionary_address);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
	return entry;
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        for (Map.Entry<Integer,String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
               String[] data = line.split(";");
               docNames.put(new Integer(data[0]), data[1]);
               docLengths.put(new Integer(data[0]), new Integer(data[2]));
            }
        }
        freader.close();
    }

    private int collisions = 0;
    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list
           
	    // 
	    //  YOUR CODE HERE
	    //
            long filePointer = 0;               //offset of file pointer
            //dataFile = new RandomAccessFile(DATA_FNAME, "rw");
            
            long filling = 0;
            for (int i = 0; i < TABLESIZE; i++) {
                dictionaryFile.writeLong(filling);
            }
            
            PostingsList postingList = null;
            
            /**
             * write all the posting lists into the data file, 
             * and build a hash map from the terms to their offsets in the data file 
             */
            for (String key: index.keySet()) {
                filePointer = dataFile.getFilePointer();
                postingList = index.get(key);
                dataFile.writeBytes(key + ";" + postingList.toString());
                writeEntry(new Entry(key, filePointer), getStorePos(key) * 8);
            }
            //System.out.println("Terms size: " + index.keySet().size());
            //dataFile.close();
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }

 
    // ==================================================================


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
	//
	//  REPLACE THE STATEMENT BELOW WITH YOUR CODE
	//
	PostingsList postingsList = null;
        long dic_address = 0, data_address = 0;
        long hashcode = 0;
        String t = "";
        try {
            hashcode = getHashCode(token);
            /**
             * given that only address are stored in dictionary file, 
             * when fetching a term form dictionary, we need to look up the 
             * data file to get the name of tokens, and compare them with the searching key words
             */
            while (!token.equals(t)) {
                dic_address = hashcode * 8;
                dictionaryFile.seek(free + dic_address);
                data_address = dictionaryFile.readLong();
                t = getTokenFromDataFile(data_address);
                hashcode += 1;
            }
            
            postingsList = getPostingFromDataFile(data_address);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
	return postingsList;
    }
    
    private PostingsList getPostingFromDataFile(long address) {
        PostingsList postingsList = null;
        
        try {
            dataFile.seek(free + address);
            String line = dataFile.readLine();
            //extract the toekn from the file
            String[] strings = line.split(";");
            if (strings.length > 0) {
                postingsList = new PostingsList();
                for (int i = 1; i < strings.length; i++) {
                    ArrayList<Integer> offsets = new ArrayList();
                    //extract docID and offsets from the file
                    String[] entry = strings[i].split("-");
                    for (int j = 2; j < entry.length; j++) {
                        offsets.add(Integer.valueOf(entry[j]));
                    }
                    postingsList.addEntry(new PostingsEntry(Integer.valueOf(entry[0]), offsets));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return postingsList;
    }
    
    private String getTokenFromDataFile(long address){
        String token = null;
        
        try {
            dataFile.seek(free + address);
            String line = dataFile.readLine();
            String[] strings = line.split(";");
            if (strings.length > 0) {
                token = strings[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return token;
    }
    

    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
	//
	//  YOUR CODE HERE
	//
        PostingsList postingList = index.get(token);
        if (postingList == null) {
            postingList = new PostingsList();
            postingList.addDocOff(docID, offset);
            index.put(token, postingList);
        } else {
            postingList.addDocOff(docID, offset);
        }
    }


    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        writeIndex();
        System.err.println( "done!" );
     }

}
