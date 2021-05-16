package qbql.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Permutations {
    static int[] factorials = new int[] {1,1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
        
    public static int factorial( int n ) {
        if( factorials[n] > 0 )
            return factorials[n];
        factorials[n] = 1;
        for( int i = n; i > 1; i-- ) 
            factorials[n] = factorials[n]*i;
        return factorials[n];
    }

    public static int[] factoradic( int n, int len ) {
        int[] ret = new int[len];
        for( int i = len-1; i > 0; i-- ) {
            if( factorial(i) > n )
                continue;
            int times = n/factorial(i);
            ret[i] = times;
            n -= times*factorial(i);
        }
        return ret;
    }
    
    /**
     * factoradic vector -> permutation vector
     */
    public static int[] permutation( int[] fact ) {
        int[] ret = new int[fact.length];
        ArrayList<Integer> integers = new ArrayList<Integer>();
        for( int i = 0; i < fact.length; i++ )
            integers.add(i);
        for( int i = ret.length-1; i >= 0; i-- )
            ret[i] = position(integers,fact[i]);
        return ret;
    }
    private static int position( ArrayList<Integer> fact, int num ) {
        int ret = fact.get(num);
        fact.remove(num);
        return ret;
    }
    
    public static void print( int[] ints ) {
        for( int i = ints.length-1; i > 0; i-- )
            System.out.print(ints[i]+".");
        System.out.println(ints[0]);
    }
    
    public static List<Map<String,String>> permute( String[] src, String[] target ) {
        List<Map<String,String>> ret = new LinkedList<Map<String,String>>();
        List<String> srcMinusTarget = new ArrayList<String>(); 
        List<String> targetMinusSrc = new ArrayList<String>(); 
        for( String s : src ) {
            boolean isElem = false;
            for( String t : target )
                if( s.equals(t) ) {
                    isElem = true;
                    break;
                }
            if( !isElem )
                srcMinusTarget.add(s);
        }
        for( String t : target ) {
            boolean isElem = false;
            for( String s : src )
                if( s.equals(t) ) {
                    isElem = true;
                    break;
                }
            if( !isElem )
                targetMinusSrc.add(t);
        }
            
        for( int i = 0; i < factorial(srcMinusTarget.size()); i++ ) {
            int[] perm = permutation(factoradic(i,srcMinusTarget.size()));
            Map<String,String> tmp = new HashMap<String,String>();
            for( int j = 0; j < perm.length; j++ )
                tmp.put(srcMinusTarget.get(j),targetMinusSrc.get(perm[j]));
            ret.add(tmp);
        }
        return ret;    
    }

    public static void main( String[] args ) {
        /*int num = 3;
        for( int i = 0; i < factorial(num); i++ )
            print(permutation(factoradic(i,num)));
        */  
        String[] src = {"a", "b", "c", "d"};
        String[] target = {"d", "x", "y", "z"};
        List<Map<String,String>> tmp = permute(src,target);
        for( Map<String,String> m : tmp )
            System.out.println(m);
    }
}