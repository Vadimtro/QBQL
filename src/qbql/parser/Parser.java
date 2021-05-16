package qbql.parser;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import qbql.util.Util;


public abstract class Parser {
    
    public String[] allSymbols;  
    public Map<String,Integer> symbolIndexes;
    
    // indexes
    public Set<Integer>[] singleRhsRules;
    
    public Parser( Set<RuleTuple> originalRules ) {
        extractSymbols(originalRules);
    }

    private int minQuoteIndex = Integer.MAX_VALUE;
    private int maxQuoteIndex = 0;
	protected void extractSymbols( Set<RuleTuple> originalRules ) {
        Set<String> tmpSymbols = new TreeSet<String>();
        for( RuleTuple ct : originalRules ) {
            if( ct.head==null || ct.rhs[0]==null || ct.rhs.length>1 && ct.rhs[1]==null )
                throw new RuntimeException("ct has null symbols"); //$NON-NLS-1$
            tmpSymbols.add(ct.head);
            for( String s : ct.rhs )            	
            	tmpSymbols.add(s);
        }

        // add grammar symbols according to some heuristic order
        // generally want to see "more complete" parse trees
        allSymbols = new String[tmpSymbols.size()];
        symbolIndexes = new TreeMap<String,Integer>();
        int k = 0;
        for( String s : tmpSymbols ) {
            symbolIndexes.put(s, k);
            allSymbols[k]=s;
            if( s.charAt(0)=='\'' ) {
            	if( k < minQuoteIndex )
            		minQuoteIndex = k;
            	if( maxQuoteIndex < k )
            		maxQuoteIndex = k;
            }
            	
            k++;
        }
	}
    
    protected boolean isQuoted( int terminal ) {
    	return minQuoteIndex <= terminal && terminal <= maxQuoteIndex;
    }
    
    public ParseNode forest( List<LexerToken> src, Matrix m ) {
    	int len = src.size();
    	if( len == 0 )
    		return new ParseNode(0,len, -1,-1, this);
    	
        Cell cell = m.get(Util.pair(0,len));
		if( cell != null && 0 < cell.size() ) {
            ParseNode root = treeForACell(src, m, cell, 0, len);
            if( root != null )
                return root;
        }
        
        ParseNode pseudoRoot = new ParseNode(0,len, -1,-1, this);
        for( int offset = len-1; offset > 0; offset-- ) 
            for( int x = 0; x < len-offset; x++ ) {
                int y = x+offset;
                cell = m.get(Util.pair(x,y));
        		if( cell != null ) {
        			ParseNode node = treeForACell(src, m, cell, x, y);
        			if( node != null ) {
        			    pseudoRoot.addTopLevel(node);
        			    return pseudoRoot; 
        			}
        		}
            }
        throw new AssertionError("VT: no suitable branches");        
    }
    
    abstract ParseNode treeForACell( List<LexerToken> src, Matrix m, Cell cell, int x, int y );


}
