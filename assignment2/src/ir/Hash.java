/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir;

/**
 *
 * @author Zehua
 */
public class Hash {
    
    private static long TableSize = 99999999L;
    private static int seed = 131313;
    
    public static long getHashCode(String term) {
        char[] term_string = term.toCharArray();
        
        long hash = 0;
        for (int i = 0; i < term_string.length; i++) {
            hash = hash * seed + term_string[i];
        }
        
        return hash % TableSize;
    }

    public long getTableSize() {
        return TableSize;
    }

    public void setTableSize(long TableSize) {
        this.TableSize = TableSize;
    }
    
    
}
