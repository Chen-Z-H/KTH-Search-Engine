/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  


package ir;

import java.util.HashMap;
import java.util.Iterator;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {


    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();
    
    public HashMap<Integer, HashMap<String, Boolean>> docIndex = new HashMap();


    private KGramIndex kgIndex = new KGramIndex(2);

    public KGramIndex getKgIndex() {
        return kgIndex;
    }
    
    /**
     *  Inserts this token in the hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        //
        // YOUR CODE HERE
        //
        PostingsList postingList = index.get(token);
        if (postingList == null) {
            postingList = new PostingsList();
            postingList.addDocOff(docID, offset);
            index.put(token, postingList);
            kgIndex.insert(token);
        } else {
            postingList.addDocOff(docID, offset);
        }
        
        if (docIndex.containsKey(docID)) {
            docIndex.get(docID).put(token, true);
        } else {
            HashMap<String, Boolean> tMap = new HashMap();
            tMap.put(token, true);
            docIndex.put(docID, tMap);
        }
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
	//
	// REPLACE THE STATEMENT BELOW WITH YOUR CODE
	//
        PostingsList postingsList = null;
        postingsList = index.get(token);
	return postingsList;
    }


    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
