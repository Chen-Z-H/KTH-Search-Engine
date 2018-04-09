/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import ir.Query.QueryTerm;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;


public class SpellChecker {
    /** The regular inverted index to be used by the spell checker */
    Index index;

    /** K-gram index to be used by the spell checker */
    KGramIndex kgIndex;

    /** The auxiliary class for containing the value of your ranking function for a token */
    class KGramStat implements Comparable {
        double score;
        String token;

        KGramStat(String token, double score) {
            this.token = token;
            this.score = score;
        }

        public String getToken() {
            return token;
        }
        
        public void setScore(double score) {
            this.score = score;
        }

	public int compareTo(Object other) {
            if (this.score == ((KGramStat)other).score) return 0;
            return this.score < ((KGramStat)other).score ? 1 : -1;
        }

        public String toString() {
            return token + ";" + score;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (((KGramStat)obj).getToken().equals(token)) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * The threshold for Jaccard coefficient; a candidate spelling
     * correction should pass the threshold in order to be accepted
     */
    private static final double JACCARD_THRESHOLD = 0.4;


    /**
      * The threshold for edit distance for a candidate spelling
      * correction to be accepted.
      */
    private static final int MAX_EDIT_DISTANCE = 2;


    public SpellChecker(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }
    /**
     *  Computes the Jaccard coefficient for two sets A and B, where the size of set A is 
     *  <code>szA</code>, the size of set B is <code>szB</code> and the intersection 
     *  of the two sets contains <code>intersection</code> elements.
     */
    private double jaccard(int szA, int szB, int intersection) {
        //
        // YOUR CODE HERE
        //
        
	return (double)intersection / (double)(szA + szB - intersection);
    }

    /**
     * Computing Levenshtein edit distance using dynamic programming.
     * Allowed operations are:
     *      => insert (cost 1)
     *      => delete (cost 1)
     *      => substitute (cost 2)
     */
    private int editDistance(String s1, String s2) {
        //
        // YOUR CODE HERE
        //
        int s1_len = s1.length() + 1;
        int s2_len = s2.length() + 1;
        int m[][] = new int[s1_len][s2_len];
        
        for (int i = 0; i < s1_len; i++) {
            m[i][0] = i;
        }
        
        for (int i = 0; i < s2_len; i++) {
            m[0][i] = i;
        }
        
        for (int i = 1; i < s1_len; i++) {
            for (int j = 1; j < s2_len; j++) {
                int replacement_cost = (s1.charAt(i - 1) == s2.charAt(j - 1))?m[i-1][j-1]:(m[i-1][j-1] + 2);
                int insert_cost = m[i-1][j] + 1;
                int delete_cost = m[i][j-1] + 1;
                m[i][j] = Math.min(replacement_cost, Math.min(insert_cost, delete_cost));
            }
        }
        return m[s1_len-1][s2_len-1];
    }

    /**
     *  Checks spelling of all terms in <code>query</code> and returns up to
     *  <code>limit</code> ranked suggestions for spelling correction.
     */
    public String[] check(Query query, int limit) {
        //
        // YOUR CODE HERE
        //
        String[] ret = null;
        if (query.size() == 1) {
            ret = singleWordCheck(query, limit);
            System.err.println("Single-word spelling correction");
        } else {
            ret = multiWordCheck(query, limit);
            System.err.println("Multi-word spelling correction");
        }
        
        return ret;
    }
    
    private String[] singleWordCheck(Query query, int limit) {
        int K = kgIndex.getK();
        // For one-word query, we get the first term in queryterm directly
        String qterm = query.queryterm.get(0).term;
        if (qterm.contains("*")) {
            return null;
        }
        
        // All the survived corrections in ArrayList
        ArrayList<KGramStat> corrections = new ArrayList();
        // All k-grams in the query word
        ArrayList<String> query_kgrams = new ArrayList();
        // Prevent some terms be added more than once
        ArrayList<String> added_terms = new ArrayList();
        
        // Extract all the k-grams from the query
        String donoted_token = "^" + qterm + "$";
        int numOfQueryGram = donoted_token.length() - K + 1;
        for (int i = 0; i < numOfQueryGram; i++) {
            String t_kgram = donoted_token.substring(i, i + K);
            query_kgrams.add(t_kgram);
        }

        int sizeA = query_kgrams.size();
        for (int i = 0; i < numOfQueryGram; i++) {
            String t_kgram = query_kgrams.get(i);
            List<KGramPostingsEntry> kGramEntries = kgIndex.getPostings(t_kgram);
            if (kGramEntries != null) {
                // For each term that contains t_kgram
                for (int j = kGramEntries.size() - 1; j >= 0; j--) {
                    KGramPostingsEntry entry = kGramEntries.get(j);
                    String alter_term = kgIndex.getTermByID(entry.tokenID);
                    
                    if (added_terms.contains(alter_term)) {  // This term is recorded before
                        continue;
                    }
                    added_terms.add(alter_term);
                    
                    String denoted_alter_gram = "^" + alter_term + "$";
                    int intersection = 0;   // Size of A intersects B
                    for (int k = 0; k < numOfQueryGram; k++) {
                        String tt_kgram = query_kgrams.get(k);
                        if (denoted_alter_gram.contains(tt_kgram)) {
                            intersection++;
                        }
                    }

                    // Now filter the alternative terms with Jaccard coefficient
                    if (jaccard(sizeA, entry.numOfKGrams, intersection) >= JACCARD_THRESHOLD) {
                        // Now filter the survived terms with edit distance
                        if (editDistance(qterm, alter_term) <= MAX_EDIT_DISTANCE) {
                            // I use the number of documents that this term occurs in as its score
                            corrections.add(new KGramStat(alter_term, index.getPostings(alter_term).size()));
//                            System.out.println("Term: " + alter_term + ", Jaccard coefficient: " 
//                                + jaccard(sizeA, entry.numOfKGrams, intersection)
//                            + ", Edit distance: " + editDistance(qterm, alter_term)
//                            + ", Num of docs: " + index.getPostings(alter_term).size());
                        }
                    }
                }
            }
        }
//        System.out.println("Number of alternatives: " + corrections.size());
        Collections.sort(corrections);
        
        int numOfReturn = (corrections.size()<limit)?corrections.size():limit;
        String[] ret = new String[numOfReturn];
        for (int i = 0; i < numOfReturn; i++) {
            ret[i] = corrections.get(i).getToken();
        }
        
        return ret;
    }

    private String[] multiWordCheck(Query query, int limit) {
        int K = kgIndex.getK();
        List<List<KGramStat>> qCorrections = new ArrayList();
        // For each query word, get its candidate list, and try to merge them
        ArrayList<Query.QueryTerm> queryTerms = query.queryterm;
        for (Query.QueryTerm queryTerm: queryTerms) {
            String qterm = queryTerm.term;
            
            if (qterm.contains("*")) {
                return null;
            }
            
            // All the survived corrections in ArrayList
            ArrayList<KGramStat> corrections = new ArrayList();
            // All k-grams in the query word
            ArrayList<String> query_kgrams = new ArrayList();
            // Prevent some terms be added more than once
            ArrayList<String> added_terms = new ArrayList();

            // Extract all the k-grams from the query
            String donoted_token = "^" + qterm + "$";
            int numOfQueryGram = donoted_token.length() - K + 1;
            for (int i = 0; i < numOfQueryGram; i++) {
                String t_kgram = donoted_token.substring(i, i + K);
                query_kgrams.add(t_kgram);
            }

            int sizeA = query_kgrams.size();
            for (int i = 0; i < numOfQueryGram; i++) {
                String t_kgram = query_kgrams.get(i);
                List<KGramPostingsEntry> kGramEntries = kgIndex.getPostings(t_kgram);
                if (kGramEntries != null) {
                    // For each term that contains t_kgram
                    for (int j = kGramEntries.size() - 1; j >= 0; j--) {
                        KGramPostingsEntry entry = kGramEntries.get(j);
                        String alter_term = kgIndex.getTermByID(entry.tokenID);

                        if (added_terms.contains(alter_term)) {  // This term is recorded before
                            continue;
                        }
                        added_terms.add(alter_term);

                        String denoted_alter_gram = "^" + alter_term + "$";
                        int intersection = 0;   // Size of A intersects B
                        for (int k = 0; k < numOfQueryGram; k++) {
                            String tt_kgram = query_kgrams.get(k);
                            if (denoted_alter_gram.contains(tt_kgram)) {
                                intersection++;
                            }
                        }

                        // Now filter the alternative terms with Jaccard coefficient
                        if (jaccard(sizeA, entry.numOfKGrams, intersection) >= JACCARD_THRESHOLD) {
                            // Now filter the survived terms with edit distance
                            if (editDistance(qterm, alter_term) <= MAX_EDIT_DISTANCE) {
                                // I use the number of documents that this term occurs in as its score
                                corrections.add(new KGramStat(alter_term, index.getPostings(alter_term).size()));
                            }
                        }
                    }
                }
            }

//            Collections.sort(corrections);

            qCorrections.add(corrections);
        }
        
        List<KGramStat> retCorrections = mergeCorrections(qCorrections, limit);
        
        int numOfReturn = (retCorrections.size()<limit)?retCorrections.size():limit;
        String[] ret = new String[numOfReturn];
        for (int i = 0; i < numOfReturn; i++) {
            ret[i] = retCorrections.get(i).getToken();
        }
        
        return ret;
    }
    
    /**
     *  Merging ranked candidate spelling corrections for all query terms available in
     *  <code>qCorrections</code> into one final merging of query phrases. Returns up
     *  to <code>limit</code> corrected phrases.
     */
    private List<KGramStat> mergeCorrections(List<List<KGramStat>> qCorrections, int limit) {
        //
        // YOUR CODE HERE
        //
        int numOfQueryTerms = qCorrections.size();
        
        // Returned list contains all the possible phrases
        List<KGramStat> ret = new ArrayList();
        // Final corrections
        List<MultiWordCorrection> corrections = new ArrayList();
        
        // Candidates used for current search
        List<List<KGramStat>> candidates = qCorrections.subList(0, 2);
        
        // Get all the combinations of the first two query terms
        List<List<KGramStat>> combinations = new ArrayList();
        Combination.getCombination(candidates, 0, new Stack(), combinations);
        // For each combination, do a intersection query
        for (int i = combinations.size() - 1; i >= 0; i--) {
            List<KGramStat> tKGram = combinations.get(i);
            List<String> words = new ArrayList();
            words.add(tKGram.get(0).getToken());
            words.add(tKGram.get(1).getToken());
            PostingsList tList = intersectionSearch(tKGram);
            if (tList == null) {
                continue;
            }
            
            corrections.add(new MultiWordCorrection(words, tList.getList()));
        }
        
        Collections.sort(corrections);
        int retSize = (corrections.size()>limit)?limit:corrections.size();
        corrections = corrections.subList(0, retSize);
        
        ArrayList<MultiWordCorrection> tCorrections = null;
        // If the query has more than two words
        for (int i = 2; i < numOfQueryTerms; i++) {
            ArrayList<KGramStat> tGramList = (ArrayList)qCorrections.get(i);
            int newCorrectionsSize = tGramList.size();
            int preCorrectionsSize = corrections.size();
            tCorrections = new ArrayList();
            // For each in the previous corrections
            for (int j = 0; j < preCorrectionsSize; j++) {
                // The previously stored correction
                MultiWordCorrection tpCorrection = corrections.get(j);
                // For each correction of the new word
                for (int k = 0; k < newCorrectionsSize; k++) {
//                    ArrayList<String> tWords = tpCorrection.getWordsCopy();
//                    tWords.add(tGramList.get(k).getToken());
                    String tToken = tGramList.get(k).getToken();
                    tCorrections.add(tpCorrection.getNewCorrection(tToken, index.getPostings(tToken).getList()));
                }
                
            }
            Collections.sort(tCorrections);
            // Cut the list to save time
            retSize = (tCorrections.size()>limit)?limit:tCorrections.size();
            corrections = tCorrections.subList(0, retSize);
        }
        
        // Sort based on their scores
        Collections.sort(corrections);
        // Convert the MultiWordCorrection to Strings
        int ret_size = (corrections.size()>limit)?limit:corrections.size();
        for (int i = 0; i < ret_size; i++) {
            MultiWordCorrection cor = corrections.get(i);
            ret.add(new KGramStat(cor.toString(), cor.size()));
        }
        
        return ret;
    }
    
    // Convert the KGramStat list to String list
    private ArrayList<String> convertKGramToString(ArrayList<KGramStat> kGrams) {
        ArrayList<String> strs = new ArrayList();
        for (int i = 0; i < kGrams.size(); i++) {
            strs.add(kGrams.get(i).getToken());
        }
        return strs;
    }
    
    
    /**
     * used for intersection search
     */
    private PostingsList intersectionSearch (List<KGramStat> query) {
        ArrayList<PostingsList> pLists = new ArrayList();
        
        for (int i = 0; i < query.size(); i++) {
            PostingsList temp = index.getPostings(query.get(i).getToken());
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
        
        if (postingsList.getList().isEmpty()) {
            return null;
        } else {
            return postingsList;
        }
    }
}
