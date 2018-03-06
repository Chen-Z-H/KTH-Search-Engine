import java.util.*;
import java.io.*;

public class PageRankSparse {

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     *   Mapping from document names to document numbers.
     */
    HashMap<String,Integer> docNumber = new HashMap<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];
    
    /**
     * Mapping from document numbers to file names
     */
    HashMap<String, String> numToName = new HashMap<String, String>();

    /**  
     *   A memory-efficient representation of the transition matrix.
     *   The outlinks are represented as a HashMap, whose keys are 
     *   the numbers of the documents linked from.<p>
     *
     *   The value corresponding to key i is a HashMap whose keys are 
     *   all the numbers of documents j that i links to.<p>
     *
     *   If there are no outlinks from i, then the value corresponding 
     *   key i is null.
     */
    HashMap<Integer,HashMap<Integer,Boolean>> link = new HashMap<Integer,HashMap<Integer,Boolean>>();

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
     *   Convergence criterion: Transition probabilities do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

       
    /* --------------------------------------------- */


    public PageRankSparse( String filename ) {
        readFileNames();
	int noOfDocs = readDocs( filename );
	iterate( noOfDocs, 1000 );
    }


    /* --------------------------------------------- */


    /**
     *   Reads the documents and fills the data structures. 
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
		    // Set the probability to 0 for now, to indicate that there is
		    // a link from fromdoc to otherDoc.
		    if ( link.get(fromdoc) == null ) {
			link.put(fromdoc, new HashMap<Integer,Boolean>());
		    }
		    if ( link.get(fromdoc).get(otherDoc) == null ) {
			link.get(fromdoc).put( otherDoc, true );
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
     *   Chooses a probability vector a, and repeatedly computes
     *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
     */
    void iterate( int numberOfDocs, int maxIterations ) {

	// YOUR CODE HERE
        double a[] = new double[numberOfDocs];
        a[0] = 1;
        
        double aa[] = new double[numberOfDocs];
        
        double ter_prob = (double)1 / (double)numberOfDocs;
        double bored_prob = BORED / (double)numberOfDocs;
        // trasfer the string docNames to Integer format
//        Integer[] iDocName = new Integer[numberOfDocs];
//        for (int i = 0; i < numberOfDocs; i++) {
//            iDocName[i] = Integer.valueOf(docName[i]);
//        }
        
        int iteration = 0;
        while (iteration < maxIterations) {

            for (int i = 0; i < numberOfDocs; i++) {
                HashMap<Integer,Boolean> tLink = link.get(i);
                if (tLink == null) {
                    double entry = a[i] * ter_prob;
                    for (int j = 0; j < numberOfDocs; j++) {
                        aa[j] += entry;
                    }
                } else {
                    double entry1 = a[i] * bored_prob;
                    double entry2 = a[i] * ((1 - BORED) * ((double)1 / (double)tLink.size()) + bored_prob);
                    for (int j = 0; j < numberOfDocs; j++) {
                        if (tLink.containsKey(j)) {
                            // if pij is zero
                            aa[j] += entry2;
                        } else {
                            // if pij is not zero
                            aa[j] += entry1;
                        }
                    }
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
            System.out.println(iteration);
        }
        
        printRankedDocs(a, numberOfDocs);
    }

    private void printRankedDocs(double[] a, int numberOfDocs) {
        ArrayList<Rank> list = new ArrayList();
        for (int i = 0; i < numberOfDocs; i++) {
            list.add(new Rank(docName[i], a[i]));
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
//            System.out.println(docName[Integer.valueOf(list.get(i).docName)] + ": " + list.get(i).pagerank);
            System.out.println(list.get(i).docName + ": " + list.get(i).pagerank);
        }
        
        savePRToFile(list);
    }
    
    private void savePRToFile(ArrayList<Rank> list) {
        String fileName = "pagerank";
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(new File(fileName));
            String item = null;
            for (int i = 0; i < list.size(); i++) {
                item = numToName.get(list.get(i).docName) + ";" + list.get(i).pagerank + "\r\n";
                output.write(item.getBytes());
            }
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * read the real file names and build the mapping from file numbers to names
     */
    private void readFileNames() {
        String fileName = "davisWikiArticleTitles.txt";
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] strs = line.split(";");
                numToName.put(strs[0], strs[1]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
	    new PageRankSparse( args[0] );
	}
    }
}