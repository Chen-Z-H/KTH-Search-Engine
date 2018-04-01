/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.nio.charset.*;
import java.io.*;


/**
 *  A class for representing a query as a list of words, each of which has
 *  an associated weight.
 */
public class Query {

    /**
     *  Help class to represent one query term, with its associated weight. 
     */
    class QueryTerm {
	String term;
	double weight;
	QueryTerm( String t, double w ) {
	    term = t;
	    weight = w;
	}
        
    }

    /** 
     *  Representation of the query as a list of terms with associated weights.
     *  In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**  
     *  Relevance feedback constant alpha (= weight of original query terms). 
     *  Should be between 0 and 1.
     *  (only used in assignment 3).
     */
    double alpha = 0.1;

    /**  
     *  Relevance feedback constant beta (= weight of query terms obtained by
     *  feedback from the user). 
     *  (only used in assignment 3).
     */
    double beta = 1 - alpha;
    
    
    /**
     *  Creates a new empty Query 
     */
    public Query() {
    }
    
    
    /**
     *  Creates a new Query from a string of words
     */
    public Query( String queryString  ) {
	StringTokenizer tok = new StringTokenizer( queryString );
	while ( tok.hasMoreTokens() ) {
	    queryterm.add( new QueryTerm(tok.nextToken(), 1.0) );
	}    
    }
    
    
    /**
     *  Returns the number of terms
     */
    public int size() {
	return queryterm.size();
    }
    
    
    /**
     *  Returns the Manhattan query length
     */
    public double length() {
	double len = 0;
	for ( QueryTerm t : queryterm ) {
	    len += t.weight; 
	}
	return len;
    }
    
    
    /**
     *  Returns a copy of the Query
     */
    public Query copy() {
	Query queryCopy = new Query();
	for ( QueryTerm t : queryterm ) {
	    queryCopy.queryterm.add( new QueryTerm(t.term, t.weight) );
	}
	return queryCopy;
    }

    
    /**
     *  Expands the Query using Relevance Feedback
     *
     *  @param results The results of the previous query.
     *  @param docIsRelevant A boolean array representing which query results the user deemed relevant.
     *  @param engine The search engine object
     */
    public void relevanceFeedback( PostingsList results, boolean[] docIsRelevant, Engine engine ) {
	//
	//  YOUR CODE HERE
	//
        
        // Notation: The lengths of the results and docIsRelevant are not equal.
        
        // Used to get idf and tf of documents
        Index hashedIndex = engine.index;
        // A array used to store all the relevant document vectors
        ArrayList<ArrayList<Double>> relDocs = new ArrayList();
        // compute the idfs of all the dicuments and store them in a array
        double[] idf = new double[size()];
        for (int i = 0; i < size(); i++) {
            PostingsList tmp_list = hashedIndex.getPostings(queryterm.get(i).term);
            idf[i] = (tmp_list==null)?0:tmp_list.size();
        }
        
        int index = 0;
        for (PostingsEntry tEntry: results.getList()) {
            if (docIsRelevant[index]) {
                // now we create a weight vector for each relevant document,
                // we take a differnet implementation compared to Searcher class
                ArrayList<Double> doc = new ArrayList();
                for (int i = 0; i < size(); i++) {
//                    System.out.println("DocID: " + tEntry.docID);
                    double tf = hashedIndex.getPostings(queryterm.get(i).term).getTermFrequency(tEntry.docID);
                    // The normalized tf-idf value
                    doc.add(tf * idf[i] / hashedIndex.docLengths.get(tEntry.docID));
                }
                relDocs.add(doc);
            }
            index++;
            if (index >= docIsRelevant.length) {
                break;
            }
        }
        
        // The user doesn't give any feedback
        if (relDocs.isEmpty()) {
            System.out.println("No feedback.");
            return;
        }
        
        // now update the weights of the terms
        index = 0;
        for (QueryTerm keyword: queryterm) {
            double sum = 0;
            for (int i = 0; i < relDocs.size(); i++) {
                sum += relDocs.get(i).get(index);
            }
            sum /= relDocs.size();
//            System.out.println("Sum: " + sum);
            
//            if (keyword.weight == 1) {
//                // We only consider the term frequency for query vector for now
//                keyword.weight = alpha * getTfInQuery(keyword.term) / queryterm.size() + beta * sum;
//            } else {
//                // Some problem may be with the following sentence, unreasonable to judge if it is the first query 
//                // by comparing its weight with 1
//                keyword.weight = alpha * keyword.weight / queryterm.size() + beta * sum;
//            }
            keyword.weight = alpha * getTfInQuery(keyword.term) / queryterm.size() + beta * sum;

            System.out.print(keyword.term + ": " + keyword.weight + " ");
            index++;
        }
        System.out.println();
    }
    
    /**
     * count the term frequency in a query
     * @param token
     * @return tf of the specified token in query
     */
    public int getTfInQuery(String token) {
        int count = 0;
        for (QueryTerm qTerm: queryterm) {
            if (qTerm.term.equals(token)) {
                count++;
            }
        }
        return count;
    }
}