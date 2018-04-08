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

    /**
     *  Merging ranked candidate spelling corrections for all query terms available in
     *  <code>qCorrections</code> into one final merging of query phrases. Returns up
     *  to <code>limit</code> corrected phrases.
     */
    private List<KGramStat> mergeCorrections(List<List<KGramStat>> qCorrections, int limit) {
        //
        // YOUR CODE HERE
        //
        double returnedRate = 0.2;
        int numOfQueryTerms = qCorrections.size();
        
        for (int i = 0; i < numOfQueryTerms; i++) {
            
        }
        return null;
    }
}
