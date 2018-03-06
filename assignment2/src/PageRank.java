import java.util.*;
import java.io.*;

public class PageRank {

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory;
     */
    final static int MAX_NUMBER_OF_DOCS = 1000;

    /**
     *   Mapping from document names to document numbers.
     */
    Hashtable<String,Integer> docNumber = new Hashtable<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    /**  
     *   The transition matrix. p[i][j] = the probability that the
     *   random surfer clicks from page i to page j.
     */
    double[][] p = new double[MAX_NUMBER_OF_DOCS][MAX_NUMBER_OF_DOCS];

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     *   The probability that the surfer will be bored, stop
     *   following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     *   In the initializaton phase, we use a negative number to represent 
     *   that there is a direct link from a document to another.
     */
    final static double LINK = -1.0;
    
    /**
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

    
    /* --------------------------------------------- */


    public PageRank( String filename ) {
	int noOfDocs = readDocs( filename );
	initiateProbabilityMatrix( noOfDocs );
	iterate( noOfDocs, 100 );
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and fills the data structures. When this method 
     *   finishes executing, <code>p[i][j] = LINK</code> if there is a direct
     *   link from i to j, and <code>p[i][j] = 0</code> otherwise.
     *   <p>
     *
     *   @return the number of documents read.
     */
    int readDocs( String filename ) {
	int fileIndex = 0;
	try {
	    System.err.print( "Reading file... " );
	    BufferedReader in = new BufferedReader( new FileReader( filename ));
	    String line;
	    while ((line = in.readLine()) != null && fileIndex<MAX_NUMBER_OF_DOCS ) {
		int index = line.indexOf( ";" );
		String title = line.substring( 0, index );
		Integer fromdoc = docNumber.get( title );
		//  Have we seen this document before?
		if ( fromdoc == null ) {	
		    // This is a previously unseen doc, so add it to the table.
		    fromdoc = fileIndex++;
		    docNumber.put( title, fromdoc );
		    docName[fromdoc] = title;
		}
		// Check all outlinks.
		StringTokenizer tok = new StringTokenizer( line.substring(index+1), "," );
		while ( tok.hasMoreTokens() && fileIndex<MAX_NUMBER_OF_DOCS ) {
		    String otherTitle = tok.nextToken();
		    Integer otherDoc = docNumber.get( otherTitle );
		    if ( otherDoc == null ) {
			// This is a previousy unseen doc, so add it to the table.
			otherDoc = fileIndex++;
			docNumber.put( otherTitle, otherDoc );
			docName[otherDoc] = otherTitle;
		    }
		    // Set the probability to LINK for now, to indicate that there is
		    // a link from d to otherDoc.
		    if ( p[fromdoc][otherDoc] >= 0 ) {
			p[fromdoc][otherDoc] = LINK;
			out[fromdoc]++;
		    }
		}
	    }
	    if ( fileIndex >= MAX_NUMBER_OF_DOCS ) {
		System.err.print( "stopped reading since documents table is full. " );
	    }
	    else {
		System.err.print( "done. " );
	    }
	}
	catch ( FileNotFoundException e ) {
	    System.err.println( "File " + filename + " not found!" );
	}
	catch ( IOException e ) {
	    System.err.println( "Error reading file " + filename );
	}
	System.err.println( "Read " + fileIndex + " number of documents" );
	return fileIndex;
    }




    /* --------------------------------------------- */


    /*
     *   Initiates the probability matrix. 
     */
    void initiateProbabilityMatrix( int numberOfDocs ) {

	// YOUR CODE HERE
        double numOfLinks = 0;
	for (int i = 0; i < numberOfDocs; i++) {
            numOfLinks = 0;
            for (int j = 0; j < numberOfDocs; j++) {
                numOfLinks += p[i][j];
            }
            numOfLinks = Math.abs(numOfLinks);
            for (int j = 0; j < numberOfDocs; j++) {
                if (numOfLinks == 0) {
                    p[i][j] += (double)1 / (double)numberOfDocs;
                } else {
                    if (p[i][j] == LINK) {
                        p[i][j] = (1 - BORED) * ((double)1 / (double)numOfLinks);
                    }
                    p[i][j] += BORED / (double)numberOfDocs;
                }
            }
        }
    }


    /* --------------------------------------------- */


    /*
     *   Chooses a probability vector a, and repeatedly computes
     *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
     */
    void iterate( int numberOfDocs, int maxIterations ) {

	// YOUR CODE HERE
        double a[] = new double[numberOfDocs];
        a[0] = 1;

        double aa[] = new double[numberOfDocs];
        
        int iteration = 0;
        while (iteration < maxIterations) {
            for (int i = 0; i < numberOfDocs; i++) {
                for (int j = 0; j < numberOfDocs; j++) {
                    aa[i] += a[j] * p[j][i];
                }
            }
            
            if (ManhattanDistance(a, aa, numberOfDocs) <= EPSILON) {
                break;
            }
            for (int i = 0; i < numberOfDocs; i++) {
                a[i] = aa[i];
                aa[i] = 0;
            }
            iteration++;
        }
        
        printRankedDocs(a, numberOfDocs);
    }
    
    private void printRankedDocs(double[] a, int numberOfDocs) {
        ArrayList<Rank> list = new ArrayList();
        for (int i = 0; i < numberOfDocs; i++) {
            list.add(new Rank(docName[i], a[i]));
            //System.out.println(docName[i]);
        }
        
        Collections.sort(list, new Comparator<Rank>(){
            public int compare(Rank rank1, Rank rank2) {
                if (rank1.pagerank < rank2.pagerank) {
                    return 1;
                } else if (rank1.pagerank > rank2.pagerank) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        
        for (int i = 0; i < 30; i++) {
            System.out.println(list.get(i).docName + ": " + list.get(i).pagerank);
        }
        
//        for (String key: docNumber.keySet()) {
//            System.out.println(key + ": " + docNumber.get(key));
//        }
    }
    
    private double EuclideanDistance(double[] a, double[] aa, int numberOfDocs) {
        double distance = 0;
        for (int i = 0; i < numberOfDocs; i++) {
            distance += Math.pow((aa[i] - a[i]), 2);
        }
        return Math.sqrt(distance);
    }
    
    private double ManhattanDistance(double a[], double aa[], int numberOfDocs) {
        double distance = 0;
        for (int i = 0; i < numberOfDocs; i++) {
            distance += Math.abs(aa[i] - a[i]);
        }
        return distance;
    }

    class Rank {
        String docName;
        double pagerank;
        Rank(String docName, double pagerank) {
            this.docName = docName;
            this.pagerank = pagerank;
        }
    }

    /* --------------------------------------------- */


    public static void main( String[] args ) {
	if ( args.length != 1 ) {
	    System.err.println( "Please give the name of the link file" );
	}
	else {
	    new PageRank( args[0] );
	}
    }
}