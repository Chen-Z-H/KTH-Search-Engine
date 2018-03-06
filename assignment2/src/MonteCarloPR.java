import java.util.*;
import java.io.*;

public class MonteCarloPR {

    /**  
     *   Maximal number of documents. We're assuming here that we
     *   don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     *   Mapping from document names to document numbers.
     */
    private HashMap<String,Integer> docNumber = new HashMap<String,Integer>();

    /**
     *   Mapping from document numbers to document names
     */
    private String[] docName = new String[MAX_NUMBER_OF_DOCS];

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
    private HashMap<Integer,HashMap<Integer,Boolean>> link = new HashMap<Integer,HashMap<Integer,Boolean>>();

    /**
     *   The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

       
    /* --------------------------------------------- */


    public MonteCarloPR( String filename ) {
	int noOfDocs = readDocs( filename );
	MC1(noOfDocs);
//        MC2(noOfDocs);
//        MC4(noOfDocs);
//        MC5(noOfDocs);
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

    /**
     * The probability that current walk continue following the outlinks
     */
    private static final double c = 0.85;
    
    /**
     * The number of initiated walks
     */
    private static final double N = 100000;
    
    /**
     * The times that the random walk starts at each page
     */
    private static final double m = 5;
    
    /**
     * In mc4 and mc5, the walk may fall into an endless loop, 
     * we define a maximum walking steps to avoid this happening
     */
    private static final int MAXIMUM_STEP = 5;
    
    
    private void MC1(int numberOfDocs) {
//        int N = 20 * numberOfDocs;
        double[] pagerank = new double[numberOfDocs];
        for (int i = 0; i < N; i++) {
            int currentPage = new Random().nextInt(numberOfDocs);
            while(true) {
                HashMap<Integer,Boolean> tLink = link.get(currentPage);
                // if current page is s sink
                if (tLink == null) {
//                    pagerank[currentPage]++;
//                    break;
                    currentPage = new Random().nextInt(numberOfDocs);
                } else {
                    if (ifTerminate()) {
                        // if the walk should terminate here
                        pagerank[currentPage]++;
                        break;
                    } else {
                        // follow the next outlink
                        int index = new Random().nextInt(tLink.size());
                        currentPage = (int)tLink.keySet().toArray()[index];
                    }
                }
            }
        }
        
        for (int i = 0; i < numberOfDocs; i++) {
            pagerank[i] /= N;
        }
        printRankedDocs(pagerank, numberOfDocs);
    }
    
    private void MC2(int numberOfDocs) {
        double[] pagerank = new double[numberOfDocs];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < numberOfDocs; j++) {
                int currentPage = j;
                while(true) {
                    HashMap<Integer,Boolean> tLink = link.get(currentPage);
                    // if current page is s sink
                    if (tLink == null) {
//                        pagerank[currentPage]++;
//                        break;
                        currentPage = new Random().nextInt(numberOfDocs);
                    } else {
                        if (ifTerminate()) {
                            // if the walk should terminate here
                            pagerank[currentPage]++;
                            break;
                        } else {
                            // follow the next outlink
                            int index = new Random().nextInt(tLink.size());
                            currentPage = (int)tLink.keySet().toArray()[index];
                        }
                    }
                }
            }
        }
        
        for (int i = 0; i < numberOfDocs; i++) {
            pagerank[i] /= (m * numberOfDocs);
        }
        printRankedDocs(pagerank, numberOfDocs);
    }
    
    private void MC4(int numberOfDocs) {
        int numOfPageVisited = 0;
        double[] pagerank = new double[numberOfDocs];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < numberOfDocs; j++) {
                int currentPage = j;
                int step = 0;
                while(step < MAXIMUM_STEP) {
                    HashMap<Integer,Boolean> tLink = link.get(currentPage);
                    pagerank[currentPage]++;
                    numOfPageVisited++;
                
                    if (tLink == null) {
                        // if current page is s sink
                        break;
                    } else {
                        // follow the next outlink
                        int index = new Random().nextInt(tLink.size());
                        currentPage = (int)tLink.keySet().toArray()[index];
                    }
                    step++;
                }
            }
        }
        
        for (int i = 0; i < numberOfDocs; i++) {
            pagerank[i] /= numOfPageVisited;
        }
        
        printRankedDocs(pagerank, numberOfDocs);
    }
    
    private void MC5(int numberOfDocs) {
        int numOfPageVisited = 0;
        double[] pagerank = new double[numberOfDocs];
        for (int i = 0; i < N; i++) {
            int currentPage = new Random().nextInt(numberOfDocs);
            int step = 0;
            while(step < MAXIMUM_STEP) {
                HashMap<Integer,Boolean> tLink = link.get(currentPage);
                pagerank[currentPage]++;
                numOfPageVisited++;

                if (tLink == null) {
                    // if current page is s sink
                    break;
                } else {
                    // follow the next outlink
                    int index = new Random().nextInt(tLink.size());
                    currentPage = (int)tLink.keySet().toArray()[index];
                }
                step++;
            }
        }
        
        for (int i = 0; i < numberOfDocs; i++) {
            pagerank[i] /= numOfPageVisited;
        }
        printRankedDocs(pagerank, numberOfDocs);
    }

    private boolean ifTerminate() {
        double d = new Random().nextDouble();
        //System.out.println(d);
        if (d < c) {
            return false;
        } else {
            return true;
        }
    }
    
    private int getNextPageIndex(int outlinks) {
        return new Random().nextInt(outlinks);
    }
    
    /* --------------------------------------------- */
    
    /**
     * Mapping from document numbers to file names
     */
    HashMap<String, String> numToName = new HashMap<String, String>();
    
    
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
        
//        savePRToFile(list);
        savePRToFile("MC5", (int)(N), list);
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
    
    private void savePRToFile(String fileName, int N, ArrayList<Rank> list) {
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(new File(fileName));
            String item = null;
            for (int i = 0; i < 30; i++) {
                item = String.valueOf(N) + ";" + list.get(i).pagerank + "\r\n";
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
	    new MonteCarloPR( args[0] );
	}
    }
}