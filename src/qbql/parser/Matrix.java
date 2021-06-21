package qbql.parser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

import qbql.parser.Visual;
import qbql.parser.Parser.EarleyCell;
import qbql.parser.Parser.Tuple;

/**
 * Major data structure for Earley method
 * @author Dim
 */
public class Matrix {
	
    public Visual visual = null;
    Earley parser = null;
    
    public Queue<Long> completionQueue = new LinkedList<Long>();

	
    /**
     * If matrix cell at pos [x,y) contains a symbol
     * @param x
     * @param y
     * @param symbol
     * @return
     */
    public boolean contains( int x, int y, int symbol ) {
        EarleyCell cell = get(x, y);
        if( cell == null )
            return false;
        for( int i = 0; i < cell.size() ; i++ ) {
            Tuple tuple = parser.rules[cell.getRule(i)];
            if( tuple.head != symbol )
                continue;
            if( tuple.rhs.length == cell.getPosition(i) )
                return true;
        }
        return false;
    }
    
    public void enqueue( long candidate ) {
        completionQueue.add(candidate);
    }
    public long dequeue() {
        if( completionQueue.isEmpty() )
            return -1;
        return completionQueue.remove();        
    }
    
	public Visual getVisual() {
		return visual;
	}

	    
    private Map<Integer,EarleyCell>[] cells = null;
    private int lastY = 0;
        
    public int[] allXs = null;
    public Integer LAsuspect = null;

    public Matrix( Earley p ) {
        this.parser = p;
    }

    //former TreeMap.get(Service.lPair(x, y));
    public EarleyCell get( int x, int y ) {
        return cells[y].get(x);
    }
    public void put( int x, int y, EarleyCell content ) {
        if( lastY < y )
            lastY = y;
        cells[y].put(x,content);
    }
    
    public void initEarleyCells( int length ) {
        cells = new Map[length+1];
        for( int i = 0; i < cells.length; i++ ) 
            cells[i] = new TreeMap<Integer,EarleyCell>();        
    }
    
    public int lastY() {
        return lastY;
    }
    
    public Map<Integer, EarleyCell> getXRange( int y ) {
        return cells[y];
    }
    
    
    /**
     * Mostly for visualization as debugger variable
     * ASCII alternative to Visual GUI
     */
    public String toString() throws RuntimeException {
        StringBuffer ret = new StringBuffer();
        for( int y = 0; y < cells.length; y++ ) for( int x : cells[y].keySet() ) {
            EarleyCell output = get(x, y);
            if( output ==  null ) {
                System.out.println(".\n");
                continue;
                //throw new AssertionError("no value corresponding to the key ["+x+","+y+")");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            ret.append("["+x+","+y+")");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            final int cutover = 50;
            if( cutover < output.size() )
                ret.append(" ... "+output.size()+" more symbols");
            else
                for( int i = 0; i < output.size() && i < cutover+1 ; i++ ) {
                    ((Earley)parser).toString(output.getRule(i), output.getPosition(i), ret);
                    //if( i == cutover )
                        //ret.append(" ... "+output.size()+" more symbols");

                }

            ret.append("\n\n"); 
        }
        return ret.toString();
    }


    
    public List<Integer> getEarleyBackptrs( int x, int y, EarleyCell cell, int index ) {
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
            //long start = Service.lPair(0,y);
            //long end = Service.lPair(x,y);
            Map<Integer,EarleyCell> xRange = cells[y];
            //SortedMap<Long,EarleyCell> range = subMap(start, true, end, true);
            for( int mid : xRange.keySet() ) {
                //int mid = Service.lX(key);
                if( x < mid )
                    continue;
                EarleyCell candidate = get(mid,x);
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
        EarleyCell candidate = get(x,y-1);
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
        //long start = Service.lPair(x,y);
        //long end = Service.lPair(y,y);
        Map<Integer,EarleyCell> xRange = cells[y];
        //SortedMap<Long,EarleyCell> range = subMap(start, true, end, true);
        for( int mid : xRange.keySet() ) {
            if( mid < x || y < mid )
                continue;
            EarleyCell pre = get(x,mid);
            if( pre==null )
                continue;
            EarleyCell post = get(mid,y);
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

	public Matrix recalc() {
        Matrix matrix = new Matrix(parser);
        matrix.visual = visual;
        return matrix;
	}
	
	/**
	 * Does matrix cell ["x","y") contain grammar production "rule" recognized up to position "pos"? 
	 * @param x
	 * @param y
	 * @param rule
	 * @param pos
	 * @return
	 */
	public boolean recognized( int x, int y, int rule, int pos ) {
		EarleyCell cell = get(x,y);
		for( int i = 0; i < cell.size(); i++ ) {
			int r = cell.getRule(i);
			if( r != rule )
				continue;
			int p = cell.getPosition(i);
			if( p == pos )
				return true;
		}
		return false;
	}
	
    public void initCells( int length ) {
        cells = new Map[length+1];
        for( int i = 0; i < cells.length; i++ ) 
            cells[i] = new TreeMap<Integer,EarleyCell>();        
    }

}

