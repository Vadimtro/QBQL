package qbql.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import qbql.parser.Earley.Tuple;
import qbql.util.Array;
import qbql.util.Util;

/**
 * Main Data Structure for CYK method
 */
public class Matrix extends TreeMap<Integer,Cell> {
    private Parser parser;

    public Matrix( Parser cyk ) {
        this.parser = cyk;
    }

    public boolean contains( int x, int y, int symbol ) {
        Cell cell = get(Util.pair(x, y));
        if( cell == null )
            return false;
        for( int i = 0; i < cell.size() ; i++ ) {
            Earley e = (Earley) parser;
            Tuple tuple = e.rules[cell.getRule(i)];
            if( tuple.head != symbol )
                continue;
            if( tuple.rhs.length == cell.getPosition(i) )
                return true;
        }
        return false;
    }


    public String toString() throws RuntimeException {
        StringBuffer ret = new StringBuffer();
        for( int xy : keySet() ) {
            int x = Util.X(xy);
            int y = Util.Y(xy);
            ret.append("["+x+","+y+")");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            Cell output = get(Util.pair(x, y));
            if( output ==  null ) {
                throw new AssertionError("no value corresponding to the key ["+x+","+y+")");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            final int cutover = 5;
            for( int i = 0; i < output.size() && i < cutover+1 ; i++ ) {
            	if( i == cutover )
                    ret.append(" ...");
            	
                ((Earley)parser).toString(output.getRule(i), output.getPosition(i), ret);
				
			}

            ret.append('\n'); 
        }
        return ret.toString();
    }
    
    public List<Integer> getCykBackptrs( int x, int y, int symbol ) {
        int start = Util.pair(x,y);
        int end = Util.pair(0,y+1);
        List<Integer> ret = new ArrayList<Integer>();
        SortedMap<Integer,Cell> range = subMap(start, end);
//System.out.println("["+x+","+y+")  "+range.keySet());
        for( int key : range.keySet() ) {
            int mid = Util.X(key);
            Cell prefixes = get(Util.pair(x,mid));
            if( prefixes==null )
                continue;
            Cell suffixes = get(Util.pair(mid,y));
            if( suffixes==null )
                continue;

            for( int I : prefixes.getContent() )    // Not indexed Nested Loops
                for( int J : suffixes.getContent() ) {
                    int[] A = null; //Fix It, was: ((CYK)parser).doubleRhsRules.get(Util.pair(I, J));

                    if( A==null )
                        continue;
                    
                    int index = Array.indexOf(A, symbol);
                    if( A[index] == symbol )
                        if( !ret.contains(mid))
                            ret.add(mid);
                }
        }
        return ret;
    }
    
    public List<Integer> getEarleyBackptrs( int x, int y, Cell cell, int index ) {
        List<Integer> ret = new ArrayList<Integer>();
        if( x == y && x == 0 ) {
            ret.add(x);
            return ret;
        }
        int rule = cell.getRule(index);
        Tuple tuple = ((Earley)parser).rules[rule];
        int pos = cell.getPosition(index);
        
        // predict
        if( x == y ) {
            int start = Util.pair(0,y);
            int end = Util.pair(x,y);
            SortedMap<Integer,Cell> range = subMap(start, true, end, true);
            for( int key : range.keySet() ) {
                int mid = Util.X(key);
                Cell candidate = get(Util.pair(mid,x));
                if( candidate==null )
                    continue;
                for( int i = 0; i < candidate.size(); i++ ) {
                    Tuple candTuple = ((Earley)parser).rules[candidate.getRule(i)];
                    int candPos = candidate.getPosition(i);
					if( candPos < candTuple.rhs.length && candTuple.rhs[candPos]==tuple.head ) {
                        if( !ret.contains(mid) )
                            ret.add(mid);
                        break;
                    }
                }
            }
            return ret;
        }
        
        // scan
        Cell candidate = get(Util.pair(x,y-1));
        if( candidate!=null ) 
            for( int i = 0; i < candidate.size(); i++ ) {
                int candRule = candidate.getRule(i);
                if( rule != candRule )
                    continue;
                int candPos = candidate.getPosition(i);
                if( candPos + 1 != pos )
                    continue;
                Tuple candTuple = ((Earley)parser).rules[candRule];
                if( candTuple.rhs[candPos] == ((Earley)parser).identifier
                 || candTuple.rhs[candPos] == ((Earley)parser).string_literal    
                 || candTuple.rhs[candPos] == ((Earley)parser).digits 
                 || parser.allSymbols[candTuple.rhs[candPos]].charAt(0)=='\''   
                        ) {
                    ret.add(y+1);
                    break;
                }
            }
        
        // complete
        int start = Util.pair(x,y);
        int end = Util.pair(y,y);
        SortedMap<Integer,Cell> range = subMap(start, true, end, true);
        for( int key : range.keySet() ) {
            
    	    int mid = Util.X(key);
            Cell pre = get(Util.pair(x,mid));
            if( pre==null )
                continue;
            Cell post = get(Util.pair(mid,y));
            if( post==null )
                continue;
            nextCell:      
            for( int i = 0; i < pre.size(); i++ ) 
            	for( int j = 0; j < post.size(); j++ ) {
                    int rulePre = pre.getRule(i);
                    int rulePost = post.getRule(j); 
                    int dotPre = pre.getPosition(i);
                    int dotPost = post.getPosition(j);
                    Tuple tPre = ((Earley)parser).rules[rulePre];
                    Tuple tPost = ((Earley)parser).rules[rulePost];
                    if( tPost.size() == dotPost ) {
                        if( rulePre != rule )
                        	continue;
                    	if( dotPre+1 != pos )
                    		continue;
                        int symPre = tPre.content(dotPre);
                        if( symPre != tPost.head )
                            continue;
                        ret.add(mid);
                        break nextCell;
                    }
					
				}
            	
            
        }
        
        
        return ret;
    }
}
