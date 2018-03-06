/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.Collections;
import java.util.ArrayList;

import java.lang.Math;
import java.util.HashMap;
import java.util.Map;

/**
 *  Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;
    
    /** The table that contains mappings from pagename to file named to pagerank*/
    
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
                postingsList = rankedSearch(query, rankingType);
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
    
    private PostingsList rankedSearch(Query query, RankingType rankingType) {
        PostingsList postingsList = new PostingsList();
        
        ArrayList<String> uniqueToken = new ArrayList();
        
        // all the docs to be ranked, I use haspmap to easily get an entry reference from the container
        HashMap<Integer, PostingsEntry> entrys = new HashMap();
        
        PostingsList tList = null;     
        
        for (int i = 0; i < query.size(); i++) {
            /** 
             * sometimes a word may appear more than once, we need to ensure that 
             * these words just be counted once
             */
            String curToken = query.queryterm.get(i).term;
            if (uniqueToken.contains(curToken)) {
                continue;
            }
            tList = index.getPostings(curToken);
            
            // compute tf and idf of the query vector
            int tf_query = query.getTfInQuery(curToken);
            double idf;
            if (tList == null) {
                // if the token does not exist, set idf equal to zero
                idf = 0;
            } else {
                // idfs of query vector and doc vector are same
                idf = Math.log(index.docNames.size() / tList.size());
            }
            
            //System.out.println(curToken + ": " + idf);
            
            for (int j = 0; j < tList.size(); j++) {

                if (entrys.containsKey(tList.get(j).docID)) {
                    entrys.get(tList.get(j).docID).score += 
                            tf_query * idf * tList.get(j).getTermFrequency();
                } else {
                    tList.get(j).score += tf_query * idf * tList.get(j).getTermFrequency();
                    entrys.put(tList.get(j).docID, tList.get(j));
                }
            }
            uniqueToken.add(curToken);
        }
        
        double eFactor = 0.001;
        if (rankingType == RankingType.TF_IDF) {
            eFactor = 1;
        } else if (rankingType == RankingType.PAGERANK) {
            eFactor = 0;
        }
        System.out.println("Factor: " + eFactor);
        
        for (int docID: entrys.keySet()) {
            // divided by the length of the document, this score is tf-idf score
            entrys.get(docID).score /= index.docLengths.get(docID);
            // calculate the combined score (tf-idf and pagerank)
//            entrys.get(docID).score = eFactor * (entrys.get(docID).score) + (1 - eFactor) * pageranks.get(docID);
        }
        //System.out.println(entrys.size());
        
        //convert Hashmap to ArrayList and sort the documents based on the scores
        ArrayList<PostingsEntry> list = new ArrayList(entrys.values());
        Collections.sort(list);
        postingsList.setEntrys(list);
        //System.out.println(postingsList.size());
        
        //save the top 50 ranked file names to files (for task 2.4)
        //saveFile(list);
        return postingsList;
    }
    
    /* 
     * calculate the similarity of query vector and document vector
     * @param query query vector
     * @param doc   document vector
     * @return cosine value of vector query and vector document
     */
    private double cosineScore(ArrayList<Double> query, ArrayList<Double> doc) {
        double cos = 0;
        
        double numerator = 0;
        double sum_q = 0, sum_d = 0;
        for (int i = 0; i < query.size(); i++) {
            numerator += query.get(i) * doc.get(i);
            sum_q += Math.pow(query.get(i), 2);
            sum_d += Math.pow(doc.get(i), 2);
        }
        cos = numerator / (Math.sqrt(sum_q) * Math.sqrt(sum_d));
        
        return cos;
    }
    
    
    
    /**
     * Mapping from file docID to pagerank
     */
    HashMap<Integer, Double> pageranks = new HashMap<Integer, Double>();
    
    private void readPageRank() {
        String fileName = "pagerank";
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] strs = line.split(";");
                pageranks.put(docIDs.get(strs[0]), Double.valueOf(strs[1]));
                //System.out.println("File name: " + strs[0] + ", docID: " + docIDs.get(strs[0]));
            }
            System.out.println("Number of pagerank: " + pageranks.size());
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }
    
    /**
     * build mapping from doc names to doc IDs
     */
    public HashMap<String, Integer> docIDs = new HashMap<String, Integer>();
    
    private void buildNameToID() {
        for (Map.Entry<Integer, String> entry: index.docNames.entrySet()) {
            // docNames contains the absolute paths of the files, 
            // I extract the file name from the path here
            //System.out.println("File: " + entry.getValue());
            String path = entry.getValue();
            int index = path.lastIndexOf("\\");
            docIDs.put(path.substring(index + 1, path.length()), entry.getKey());  
        }
        System.out.println("Length of docID: " + docIDs.size());
    }
    
    public void loadPageRankFile() {
        buildNameToID();
        readPageRank();
    }
    
    private void saveFile(ArrayList<PostingsEntry> list) {
        String fileName = "Zehua.txt";
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(new File(fileName));
            String item = null;
            for (int i = 0; i < 50; i++) {
                String path = index.docNames.get(list.get(i).docID);
                int index = path.lastIndexOf("\\");
                String line = "1\t" + path.substring(index + 1, path.length()) + "\r\n";
                output.write(line.getBytes());
            }
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 