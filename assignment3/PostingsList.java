/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */
package ir;

import java.lang.Integer;
import java.util.ArrayList;
import java.util.Iterator;

public class PostingsList implements Comparable<PostingsList> {

    /**
     * The postings list
     */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();

    /**
     * Number of postings in this list.
     */
    public int size() {
        return list.size();
    }

    /**
     * Returns the ith posting.
     */
    public PostingsEntry get(int i) {
        return list.get(i);
    }

    // 
    //  YOUR CODE HERE
    //
    
    public boolean contains(PostingsEntry e) {
        if (list.contains(e)) {
            return true;
        } else {
            return false;
        }
    }
    
    public ArrayList<PostingsEntry> getList() {
        return list;
    }
    
    public void setEntrys(ArrayList<PostingsEntry> list) {
        this.list = list;
    }

    public void addEntry(PostingsEntry entry) {
        if (!list.contains(entry)) {
            list.add(entry);
        }
    }
    
    public void addDocOff(int docID, int offset) {
        int entry_index = getEntryIndex(docID);
        //PostingsEntry entry = getEntry(docID);
        if (entry_index == -1) {
            PostingsEntry entry = new PostingsEntry(docID);
            //entry.addDocOff(offset);
            entry.offsets.add(offset);
            list.add(entry);
        } else {
            list.get(entry_index).offsets.add(offset);
        }
    }
    
    /**
     * Get an entry form an existed posting list according to docID
     * @param docID
     * @return 
     */
    public int getEntryIndex(int docID) {
        int index = -1;
        
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).docID == docID) {
                index = i;
                return index;
            }
        }
        return index;
    }
    
    /**
     * Get the term frequency of a certain document based on docID
     */
    public double getTermFrequency(int docID) {
        for (PostingsEntry entry: list) {
            if (entry.docID == docID) {
                return (double)entry.getTermFrequency();
            }
        }
        // The document doesn't contain the specified term
        return 0;
    }

    public void clear() {
        list.clear();
    }

    /**
     * 
     * @param o
     * @return
     */
    @Override
    public int compareTo(PostingsList o) {
        if (this.size() < o.size()) {
            return -1;
        } else if (this.size() > o.size()) {
            return 1;
        } else {
            return 0;
        }
    }
    
    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        PostingsEntry entry = null;
        ArrayList<Integer> offsetList = null;
        //System.out.println("Size of entry: " + list.size());
        for (int i = 0; i < list.size(); i++) {
            entry = list.get(i);
            ret.append(String.valueOf(entry.docID)).append("-");
            offsetList = entry.getOffsets();
            for (int j = 0; j < offsetList.size(); j++) {
                ret.append("-");
                ret.append(offsetList.get(j));
            }
            ret.append(";");
        }
        ret.append("\r\n");
        return ret.toString();
    }
}
