/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.Collections;
import java.util.ArrayList;

import java.lang.Math;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;
    
    /** Constructor */
    public Searcher( Index index ) {
        this.index = index;
    }

    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType ) { 

	//
	//  REPLACE THE STATEMENT BELOW WITH YOUR CODE
	//
        PostingsList postingsList = null;
        switch(queryType) {
            case INTERSECTION_QUERY:
                if(query.size() == 1) {
                    postingsList = singleWordSearch(query.queryterm.get(0).term);
                } else {
                    postingsList = intersectionSearch(query);
                }
                break;
            case PHRASE_QUERY:
                postingsList = phraseSearch(query);
                break;
            case RANKED_QUERY:
                break;
            default:
                break;
        }
        
	return postingsList;
    }
    
    /**
     * used for single word search
     */
    private PostingsList singleWordSearch (String term) {
        return index.getPostings(term);
    }
    
    /**
     * used for intersection search
     */
    private PostingsList intersectionSearch (Query query) {
        ArrayList<PostingsList> pLists = new ArrayList();
        
        for (int i = 0; i < query.size(); i++) {
            PostingsList temp = index.getPostings(query.queryterm.get(i).term);
            // if one of the terms does not exist, return null
            if (temp == null) {
                return null;
            } else {
                pLists.add(temp);
            }
        } 
        Collections.sort(pLists);
        
        PostingsList postingsList = new PostingsList();
        PostingsList pl1 = new PostingsList();
        PostingsList pl2 = null;
        int p1 = 0, p2 = 0;
        //copy the first posting list to pl1
        for (int i = 0; i < pLists.get(0).size(); i++) {
            pl1.addEntry(pLists.get(0).get(i));
        }
        
        for (int i = 0; i < pLists.size() - 1; i++) {
            p1 = 0;
            p2 = 0;
            pl2 = pLists.get(i + 1);
            postingsList.clear();
            
            while (true) {
                if (pl1.get(p1).equals(pl2.get(p2))) {
                    postingsList.addEntry(pl1.get(p1));
                    if (p1 < pl1.size() - 1) {
                        p1++;
                    }
                    if (p2 < pl2.size() - 1) {
                        p2++;
                    }
                } else if (pl1.get(p1).docID > pl2.get(p2).docID) {
                    if (p2 < pl2.size() - 1) {
                        p2++;
                    } else {
                        p1++;
                    }
                } else {
                    if (p1 < pl1.size() - 1) {
                        p1++;
                    } else {
                        p2++;
                    }
                }
                if (p1 >= (pl1.size() - 1) && p2 >= (pl2.size() - 1)) {
                    if (pl1.get(p1).equals(pl2.get(p2))) {
                        postingsList.addEntry(pl1.get(p1));
                    }
                    break;
                }
            }
            //the terms do not exist in the same document, stop searching
            if (postingsList.size() == 0) {
                break;
            }
            
            pl1.clear();
            for (int j = 0; j < postingsList.size(); j++) {
                pl1.addEntry(postingsList.get(j));
            }
        }
        
        return postingsList;
    }
    
    /**
     * used for phrase search
     * @param query
     * @return 
     */
    private PostingsList phraseSearch(Query query) {
        PostingsList postingList = new PostingsList();
        ArrayList<PostingsList> pLists = new ArrayList();
        
        int p1, p2, pp1, pp2;
        PostingsList pList1 = null;
        PostingsList pList2 = null;
        ArrayList<Integer> oList1 = null;
        ArrayList<Integer> oList2 = null;
        
        pList1 = index.getPostings(query.queryterm.get(0).term);
        //System.out.println("ready for query");
        // iteratively compare different terms
        for (int i = 0; i < query.size() - 1; i++) {
            postingList.clear();
            
            p1 = 0;
            p2 = 0;

            pList2 = index.getPostings(query.queryterm.get(i + 1).term);
            // compare two posting lists of two terms
            while ((p1 < pList1.size()) && (p2 < pList2.size())) {
                //System.out.println("pos1");
                if (pList1.get(p1).docID == pList2.get(p2).docID) {
                    
                    ArrayList<Integer> l = new ArrayList();
                    //initial point to the start position of two offset lists
                    pp1 = 0; 
                    pp2 = 0; 
                    
                    oList1 = pList1.get(p1).getOffsets();
                    oList2 = pList2.get(p2).getOffsets();
                    
                    // compare the two offset list 
                    while (pp1 < oList1.size()) {
                        //System.out.println("pos2");
                        pp2 = 0;
                        while (pp2 < oList2.size()) {
                            if ((oList2.get(pp2) - oList1.get(pp1)) == 1) {
                                //System.out.println("a result found");
                                l.add(oList2.get(pp2));
                                break;
                            }
                            pp2 ++;
                        }
                        pp1 ++;
                    }
                    //System.out.println("pos2");
                    
                    if(!l.isEmpty()) {
                        //System.out.println("a result found");
                        postingList.addEntry(new PostingsEntry(pList2.get(p2).docID, l));
                    }
        
                    p1 ++;
                    p2 ++;
                } else {
                    if (pList1.get(p1).docID < pList2.get(p2).docID) {
                        p1 ++;
                    } else {
                        p2 ++;
                    }
                }
            }
            pList1 = new PostingsList();
            for (int k = 0; k < postingList.size(); k++) {
                pList1.addEntry(postingList.get(k));
            }
            //System.out.println("pos3");
        }
        //System.out.println("pos4");
        
        return postingList;
    }
}