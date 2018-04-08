/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Zehua
 */
public class MultiWordCorrection implements Comparable<MultiWordCorrection> {
    
    // The current query combination
    private ArrayList<String> words = null;
    
    // The search results of the current search combination
    private ArrayList<PostingsEntry> entrys = null;

    public MultiWordCorrection(ArrayList<String> words, ArrayList<PostingsEntry> entrys) {
        this.words = words;
        this.entrys = entrys;
    }
    
    public MultiWordCorrection(ArrayList<String> words, 
            ArrayList<PostingsEntry> entrys1, ArrayList<PostingsEntry> entrys2) {
        this.words = words;
        this.entrys = merge(entrys1, entrys2);
    }
    
    /**
     * 
     * @param word The next query word.
     * @param tEntrys   The corresponding postingList of the next query word.
     * @return A new MultiWordCorrection object.
     */
    public MultiWordCorrection getNewCorrection(String word, ArrayList<PostingsEntry> tEntrys) {
        ArrayList<String> newWords = (ArrayList)words.clone();
        newWords.add(word);
        
        ArrayList<PostingsEntry> newEntrys = merge(entrys, tEntrys); // deep copy
        return new MultiWordCorrection(newWords, newEntrys);
    }
    
    /**
     * Get the merged list of the two incoming parameters.
     * @param p1
     * @param p2
     * @return The merged entrys list.
     */
    private ArrayList<PostingsEntry> merge(ArrayList<PostingsEntry> p1, ArrayList<PostingsEntry> p2) {
        if (p1 == null || p2 == null) {
            return new ArrayList();
        }
        if (p1.isEmpty()) {
            return p1;
        }
        if (p2.isEmpty()) {
            return p2;
        }
        
        int pp1 = 0, pp2 = 0;
        int p1_size = p1.size();
        int p2_size = p2.size();
        ArrayList<PostingsEntry> entrys = new ArrayList();
        
        while (true) {
            PostingsEntry t_entry1 = p1.get(pp1);
            PostingsEntry t_entry2 = p2.get(pp2);
            if (t_entry1.docID == t_entry2.docID) {
                entrys.add(p1.get(pp1));
                if (pp1 < p1_size - 1) {
                    pp1++;
                }
                if (pp2 < p2_size - 1) {
                    pp2++;
                }
                
            } else if (t_entry1.docID > t_entry2.docID) {
                if (pp2 < p2_size - 1) {
                    pp2++;
                } else {
                    break;
                }
                
            } else {
                if (pp1 < p1_size - 1) {
                    pp1++;
                } else {
                    break;
                }
            }
            
            if ((pp1 == p1_size - 1) && (pp2 == p2_size - 1)) {
                break;
            }
        }
        
        return entrys;
    }
    
    /**
     * Return the size of search results by the current query words
     * @return 
     */
    public double size() {
        return entrys.size();
    }
    
    /**
     * The sorting criteria is the number of documents that can be returned by the current search,
     * thus we need to make comparison between different corrections.
     */
    @Override
    public int compareTo( MultiWordCorrection other ) {
	return Double.compare( other.size(), size() );
    }
    
}
