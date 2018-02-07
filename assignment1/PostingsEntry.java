/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {
    
    public int docID;
    public double score = 0;

    /**
     *  PostingsEntries are compared by their score (only relevant
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
	return Double.compare( other.score, score );
    }

    
    //
    // YOUR CODE HERE
    //
    
    // used for 1.4 phrase query
    public ArrayList<Integer> offsets = new ArrayList();
    
    public PostingsEntry(int docID) {
        this.docID = docID;
    }
    
    public PostingsEntry(int docID, ArrayList<Integer> offsets) {
        this.docID = docID;
        this.offsets = offsets;
    }
    
    public void addDocOff(int offset) {
        offsets.add(offset);
        //return true;
    }
    
    public ArrayList<Integer> getOffsets() {
        return offsets;
    }
    
    public int getOffset(int pos) {
        if(pos >= offsets.size()) {
            return -1;
        } else {
            return offsets.get(pos);
        }
    }
    
    /**
     * I overwrite this 'equals' method for ArrayList.contains to
     * make comparison between two PostingEntry objects, the comparison 
     * is made based on attribute 'docID'. For example, if an entry with 
     * docID 'a' has already exist, they will not be added to the posting list.
     * @param obj
     * @return 
     */
    @Override
    public boolean equals(Object obj) {
        if(this.docID == ((PostingsEntry)obj).docID) {
            return true;
        } else {
            return false;
        }
    }
}