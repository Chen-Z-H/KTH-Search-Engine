/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ir;

import java.util.Stack;
import java.util.ArrayList;

/**
 *
 * @author Zehua
 */
public class Combination {
    /**
     * 
     * @param <T>   The class of entries in the list
     * @param list  Contains the lists to be combinated
     * @param N     The number of lists to be combinated
     * @param stack A stack structrue used for combination algorithm
     * @param ret   All the combinations we get from this method
     */
    public static <T> void getCombination(ArrayList<ArrayList<T>> list, int N, 
            Stack<T> stack, ArrayList<ArrayList<T>> ret) {
        for (int i = 0; i < list.get(N).size(); i++) {
            stack.push(list.get(N).get(i));
            if (N < list.size() - 1) {
                getCombination(list, N + 1, stack, ret);
            } else {
                ArrayList<T> result = new ArrayList(stack);
                ret.add(result);
            }
            stack.pop();
        }
    }
}
