/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;


public class KGramIndex {

    /** Mapping from term ids to actual term strings */
    HashMap<Integer,String> id2term = new HashMap<Integer,String>();

    /** Mapping from term strings to term ids */
    HashMap<String,Integer> term2id = new HashMap<String,Integer>();

    /** Index from k-grams to list of term ids that contain the k-gram */
    HashMap<String,List<KGramPostingsEntry>> index = new HashMap<String,List<KGramPostingsEntry>>();

    /** The ID of the last processed term */
    int lastTermID = -1;

    /** Number of symbols to form a K-gram */
    int K = 3;

    public KGramIndex(int k) {
        K = k;
        if (k <= 0) {
            System.err.println("The K-gram index can't be constructed for a negative K value");
            System.exit(1);
        }
    }

    /** Generate the ID for an unknown term */
    private int generateTermID() {
        return ++lastTermID;
    }

    public int getK() {
        return K;
    }


    /**
     *  Get intersection of two postings lists
     */
    public List<KGramPostingsEntry> intersect(List<KGramPostingsEntry> p1, List<KGramPostingsEntry> p2) {
        // 
        // YOUR CODE HERE
        //
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
        List<KGramPostingsEntry> entrys = new ArrayList();
        
        while (true) {
            KGramPostingsEntry t_entry1 = p1.get(pp1);
            KGramPostingsEntry t_entry2 = p2.get(pp2);
            if (t_entry1.tokenID == t_entry2.tokenID) {
                entrys.add(p1.get(pp1));
                if (pp1 < p1_size - 1) {
                    pp1++;
                }
                if (pp2 < p2_size - 1) {
                    pp2++;
                }
                
            } else if (t_entry1.tokenID > t_entry2.tokenID) {
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


    /** Inserts all k-grams from a token into the index. */
    public void insert( String token ) {
        //
        // YOUR CODE HERE
        //
        
        // The token may appear before
        if (term2id.containsKey(token)) {
            return;
        }
        
        // Build the mapping from tokenID to token and the mapping from toekn to tokenID
        // each time we invoke generateTermID(), the id will plus one
        int id = generateTermID();
        id2term.put(id, token);
        term2id.put(token, id);
        
        KGramPostingsEntry newEntry = new KGramPostingsEntry(id);
        String donoted_token = "^".concat(token).concat("$");
//        StringBuilder donoted_token = new StringBuilder();
//        donoted_token.append("^");
//        donoted_token.append(token);
//        donoted_token.append("$");
//        String donoted_token = token;
        int numOfGrams = donoted_token.length() - K + 1;
        newEntry.numOfKGrams = numOfGrams;

        for (int i = 0; i < numOfGrams; i++) {
            String t_kgram = donoted_token.substring(i, i + K);
            List<KGramPostingsEntry> entrys = index.get(t_kgram);
            
            if (entrys != null) {
                // If this gram is already in the k-gram index
                // a k-gram may occur more than once in a token
                if (!entrys.contains(newEntry)) {
                    entrys.add(newEntry);
                }
            } else {
                // or if not
                entrys = new ArrayList();
                entrys.add(newEntry);
                index.put(t_kgram, entrys);
            }
        }
    }

    /** Get postings for the given k-gram */
    public List<KGramPostingsEntry> getPostings(String kgram) {
        //
        // YOUR CODE HERE
        //
        if (index.containsKey(kgram)) {
            return index.get(kgram);
        }
        return null;
    }

    /** Get id of a term */
    public Integer getIDByTerm(String term) {
        return term2id.get(term);
    }

    /** Get a term by the given id */
    public String getTermByID(Integer id) {
        return id2term.get(id);
    }

    private static HashMap<String,String> decodeArgs( String[] args ) {
        HashMap<String,String> decodedArgs = new HashMap<String,String>();
        int i=0, j=0;
        while ( i < args.length ) {
            if ( "-p".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("patterns_file", args[i++]);
                }
            }
            else if ( "-f".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("file", args[i++]);
                }
            }
            else if ( "-k".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("k", args[i++]);
                }
            }
            else if ( "-kg".equals( args[i] )) {
                i++;
                if ( i < args.length ) {
                    decodedArgs.put("kgram", args[i++]);
                }
            }
            else {
                System.err.println( "Unknown option: " + args[i] );
                break;
            }
        }
        return decodedArgs;
    }

    public static void main(String[] arguments) throws FileNotFoundException, IOException {
        HashMap<String,String> args = decodeArgs(arguments);

        int k = Integer.parseInt(args.getOrDefault("k", "3"));
        KGramIndex kgIndex = new KGramIndex(k);

        File f = new File(args.get("file"));
        Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
        Tokenizer tok = new Tokenizer( reader, true, false, true, args.get("patterns_file") );
        while ( tok.hasMoreTokens() ) {
            String token = tok.nextToken();
            kgIndex.insert(token);
        }

        String[] kgrams = args.get("kgram").split(" ");
        List<KGramPostingsEntry> postings = null;
        for (String kgram : kgrams) {
            if (kgram.length() != k) {
                System.err.println("Cannot search k-gram index: " + kgram.length() + "-gram provided instead of " + k + "-gram");
                System.exit(1);
            }

            if (postings == null) {
                postings = kgIndex.getPostings(kgram);
            } else {
                postings = kgIndex.intersect(postings, kgIndex.getPostings(kgram));
            }
        }
        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {
            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < resNum; i++) {
                System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }
        }
    }
}
