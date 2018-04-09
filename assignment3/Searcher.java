/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import ir.Query.QueryTerm;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.Collections;
import java.util.ArrayList;

import java.lang.Math;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * In wildcard query, we need to generate all possible tokens according to the 
     * wildcard query specified by the user (look up the k-gram index).
     * @param query
     * @return The query after processed
     */
    private List<List<QueryTerm>> preprocessQuery(Query query) {
        KGramIndex kgIndex = ((HashedIndex)index).getKgIndex();
        int K = kgIndex.getK();
        // Store all the possible combinations of query words
        List<List<QueryTerm>> combinations = new ArrayList();
        // Each list in this contains all possible tokens for each wilfcard query word
        List<List<QueryTerm>> possible_tokens = new ArrayList();
        
        ArrayList<QueryTerm> qterm = query.queryterm;
        
        for (QueryTerm qt: qterm) {
            // Skip the term if it doesn't contain an asterisk *
            if (!qt.term.contains("*")) {
                ArrayList<QueryTerm> t = new ArrayList();
                t.add(qt);
                possible_tokens.add(t);
                continue;
            }
            //Store all tokens that can match the current query word
            List<KGramPostingsEntry> entrys = new ArrayList();
            // We maintain the weight of term for relevance feedback
            double weight = qt.weight;
            /**
             * Now we extract all the k-grams from the word
             * and get all possible tokens of it
             */
            String donoted_token = "^".concat(qt.term).concat("$"); // for the later matching by regex
            String[] strs = donoted_token.split("\\*");

            for (String str: strs) {
                /**
                 * In case the length of any part of the query word is smaller than K,
                 * we cannot get a k-gram from it.
                 */
                if (str.length() >= K) {
                    for (int i = 0; i < str.length() - K + 1; i++) {
                        String t_kgram = str.substring(i, i + K);
//                        System.out.println("K-gram: " + t_kgram);
                        if (entrys.isEmpty()) {
                            entrys = kgIndex.getPostings(t_kgram);
                        } else {
                            entrys = kgIndex.intersect(entrys, kgIndex.getPostings(t_kgram));
                        }
//                        System.out.println(entrys.size());
                    }
                }
            }
            // Filter the wrong tokens
            Pattern p = Pattern.compile(donoted_token.replace("*", ".*"));
            // Transform the KGramPostingsEntry to QueryTerm
            ArrayList<QueryTerm> disjunctions = new ArrayList();
            for (KGramPostingsEntry entry: entrys) {
                String term = kgIndex.getTermByID(entry.tokenID);
                if (p.matcher(term).matches()) {
                    disjunctions.add(query.new QueryTerm(term, weight));
                }
            }
            possible_tokens.add(disjunctions);
        }
        
        // Now try to figure out all the possible combinations
        Combination.getCombination(possible_tokens, 0, new Stack(), combinations);
        
        for (int i = 0; i < possible_tokens.size(); i++) {
            System.out.println(qterm.get(i).term + ": " + possible_tokens.get(i).size());
        }
        
        return combinations;
    }

    /**
     *  Searches the index for postings matching the query.
     *  @return A postings list representing the result of the query.
     */
    public PostingsList search( Query query, QueryType queryType, RankingType rankingType ) { 

	//
	//  REPLACE THE STATEMENT BELOW WITH YOUR CODE
	//
//        System.out.println("Starting preprocessing...");
//        long startTime = System.currentTimeMillis();
        List<List<QueryTerm>> query_combination = preprocessQuery(query);
//        System.out.println("Preprocessing is over.");
//        long elapsedTime = System.currentTimeMillis() - startTime;
//        System.out.println("All possible combinations: " + query_combination.size());
        
        PostingsList ret = null;
        for (int i = query_combination.size() - 1; i >= 0; i--) {
            query.queryterm = (ArrayList)query_combination.get(i);
            PostingsList postingsList = null;
            switch(queryType) {
                case INTERSECTION_QUERY:
                    if(query.size() == 1) {
                        postingsList = singleWordSearch(query);
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
            
            if (queryType == QueryType.INTERSECTION_QUERY || queryType == QueryType.PHRASE_QUERY) {
                // merge the current searching results with the previous
                if (ret == null) {
                    ret = postingsList;
                } else {
                    ret = mergePostingsList(ret, postingsList);
                }
            } else {
                // Merge the score for ranked search
                if (ret == null) {
                    ret = postingsList;
                } else {
                    ArrayList<PostingsEntry> entrylist1 = ret.getList();
                    ArrayList<PostingsEntry> entrylist2 = postingsList.getList();
                    for (int j = entrylist2.size() - 1; j >= 0 ; j--) {
                        PostingsEntry tempEntry = entrylist2.get(j);
                        // The result set returned by each query may have different length
                        int index = entrylist1.indexOf(tempEntry);
                        if (index != -1) {
                            entrylist1.get(index).score += tempEntry.score;
                        } else {
                            entrylist1.add(tempEntry);
                        }
                    }

                }
            }
            
        }

//        System.out.println("Time (hit): " + time1 / 1000 + ", Time (miss): " + time2 / 1000 + ", Miss: " + miss);
        
        if (queryType == QueryType.RANKED_QUERY) {
            Collections.sort(ret.getList());
        }
        
        return ret;
    }
    
    private PostingsList mergePostingsList(PostingsList plist1, PostingsList plist2) {
        ArrayList<PostingsEntry> list = plist2.getList();
        for (int i = list.size() - 1; i >= 0; i--) {
            PostingsEntry entry = list.get(i);
            if (!plist1.contains(entry)) {
                plist1.addEntry(entry);
            }
        }
        return plist1;
    }
    
    /**
     * used for single word search
     */
    private PostingsList singleWordSearch (Query query) {
        String term = query.queryterm.get(0).term;
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
                    } else if (p1 < pl1.size() - 1){
                        p1++;
                    }
                } else {
                    if (p1 < pl1.size() - 1) {
                        p1++;
                    } else if (p2 < pl2.size() - 1) {
                        p2++;
                    }
                }
                if (p1 >= (pl1.size() - 1) && p2 >= (pl2.size() - 1)) {
                    try {
                        if (pl1.get(p1).equals(pl2.get(p2))) {
                            postingsList.addEntry(pl1.get(p1));
                        }
                    } catch (Exception e) {
                        System.err.println("Size1: " + pl1.size() + ", index1: " + p1);
                        System.err.println("Size2: " + pl2.size() + ", index2: " + p2);
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
        
        if (postingsList.getList().isEmpty()) {
            return null;
        } else {
            return postingsList;
        }
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
        
        // iterate in terms of the key words in the query vector
        for (int i = 0; i < query.size(); i++) {
            /** 
             * sometimes a word may appear more than once, we need to ensure that 
             * these words just be counted once
             */
            String curToken = query.queryterm.get(i).term;
            double weightOfToken = query.queryterm.get(i).weight;
            if (uniqueToken.contains(curToken)) {
                continue;
            }
            tList = index.getPostings(curToken);
            
            // compute tf and idf of the query vector
//            int tf_query = query.getTfInQuery(curToken);
            // in the feedback version, we take the weight of the term into consideration
            double tf_query = /* query.getTfInQuery(curToken) * */ weightOfToken;
//            System.out.println(curToken + ": " + weightOfToken);
            double idf;
            if (tList == null) {
                // if the token does not exist, set idf equal to zero
                idf = 0;
            } else {
                // idfs of query vector and doc vector are same
                idf = Math.log(index.docNames.size() / tList.size());
            }
            
            //System.out.println(curToken + ": " + idf);
            
            for (int j = tList.size() - 1; j >= 0; j--) {
                PostingsEntry t_entry = tList.get(j);
                if (entrys.containsKey(t_entry.docID)) {
                    entrys.get(t_entry.docID).score += 
                            tf_query * idf * t_entry.getTermFrequency();
                } else {
                    t_entry.score += tf_query * idf * t_entry.getTermFrequency();
                    entrys.put(t_entry.docID, t_entry);
                }
            }
            uniqueToken.add(curToken);
        }
        
//        double eFactor = 0.001;
//        if (rankingType == RankingType.TF_IDF) {
//            eFactor = 1;
//        } else if (rankingType == RankingType.PAGERANK) {
//            eFactor = 0;
//        }
//        System.out.println("Factor: " + eFactor);
        for (int docID: entrys.keySet()) {
            // divided by the length of the document, this score is tf-idf score
            entrys.get(docID).score /= index.docLengths.get(docID);
            // calculate the combined score (tf-idf and pagerank)
//            entrys.get(docID).score = eFactor * (entrys.get(docID).score) + (1 - eFactor) * pageranks.get(docID);
        }
        
        //convert Hashmap to ArrayList and sort the documents based on the scores
        ArrayList<PostingsEntry> list = new ArrayList(entrys.values());
//        Collections.sort(list);
        postingsList.setEntrys(list);
        
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
    
    
    private PostingsList union(PostingsList p1, PostingsList p2) {
//        if (p1.size() == 0) {
//            return p2;
//        }
//        if (p2.size() == 0) {
//            return p1;
//        }
        PostingsList result = new PostingsList();
        int i = 0, j = 0;
        
        ArrayList<PostingsEntry> pp1 = p1.getList();
        ArrayList<PostingsEntry> pp2 = p2.getList();
        int p1_size = pp1.size();
        int p2_size = pp2.size();
        
        while(true){
            PostingsEntry entry1 = pp1.get(i);
            PostingsEntry entry2 = pp2.get(j);
            if (entry1.docID == entry2.docID) {                
                PostingsEntry entry = new PostingsEntry(entry1.docID, entry1.score + entry2.score);              
                result.addEntry(entry);  
                
                if (i < p1_size - 1) {
                    i++;
                }
                if (j < p2_size - 1) {
                    j++;
                }
                
            } else if (entry1.docID > entry2.docID){
                result.addEntry(entry2);      
                if (j < p2_size - 1) {
                    j++;
                } else {
                    break;
                }
            } else {
                result.addEntry(entry1);
                if (i < p1_size - 1) {
                    i++;
                } else {
                    break;
                }
            }
            
            if ((i == p1_size - 1) && (j == p2_size - 1)) {
                break;
            }

        }
//        System.out.println("com");

        return result;
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