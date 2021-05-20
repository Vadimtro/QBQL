package qbql.parser;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import qbql.parser.Cell;
import qbql.parser.LexerToken;
import qbql.parser.Matrix;
import qbql.parser.ParseNode;
import qbql.parser.RuleTuple;
import qbql.parser.Parser.Tuple;
import qbql.util.Array;

/**
 * Common stuff for both Earley and CYK parsers
 * @author Dim
 */
public abstract class Parser implements Serializable {
    
	/**
	 * bijective map from String to int encoding of grammar symbols
	 */
    public String[] allSymbols = new String[0];  
    public Map<String,Integer> symbolIndexes = new HashMap<String,Integer>();
    
    public Tuple[] rules;
    
    //int[] auxSymbols = new int[0];
            
    public Parser( Set<RuleTuple> originalRules ) {
        extractSymbols(originalRules);
		rules = new Tuple[originalRules.size()];
		int p = 0;
		for( RuleTuple t : originalRules ) {
			if( t.rhs.length == 0 )
				throw new AssertionError("empty production "+t.toString());
			int h = symbolIndexes.get(t.head);
			int[] rhs = new int[t.rhs.length];
			for( int i = 0; i < rhs.length; i++ ) {
				rhs[i] = symbolIndexes.get(t.rhs[i]);
			}
			rules[p++] = new Tuple(h,rhs);
		}
    }
    
    // speedier initialization for Yelrae 
    public Parser() {
    }

 	protected void extractSymbols( Set<RuleTuple> symbolicRules ) {
        Set<String> tmpSymbols = new TreeSet<String>();
        tmpSymbols.add("!nil"); //$NON-NLS-1$
        for( RuleTuple ct : symbolicRules ) {
            if( ct.head==null || ct.rhs.length==0 || ct.rhs[0]==null || ct.rhs.length>1 && ct.rhs[1]==null )
                throw new AssertionError("grammar has null symbols (or empty productions)"); //$NON-NLS-1$
            tmpSymbols.add(ct.head);
            for( String s : ct.rhs )            	
            	tmpSymbols.add(s);
        }

        // add grammar symbols according to some heuristic order
        // generally want to see "more complete" parse trees
        int k = allSymbols.length;
        allSymbols = Arrays.copyOf(allSymbols, allSymbols.length+tmpSymbols.size());
        for( String s : tmpSymbols ) {
        	if( symbolIndexes.containsKey(s) )
        		continue;
            symbolIndexes.put(s, k);
            allSymbols[k]=s;
            k++;
        }
	}
 	
	protected void extractSymbolsOld( Set<RuleTuple> originalRules ) {
        Set<String> tmpSymbols = new TreeSet<String>();
        tmpSymbols.add("!nil"); //$NON-NLS-1$
        for( RuleTuple ct : originalRules ) {
            if( ct.head==null || ct.rhs.length==0 || ct.rhs[0]==null || ct.rhs.length>1 && ct.rhs[1]==null )
                throw new AssertionError("grammar has null symbols (or empty productions)"); //$NON-NLS-1$
            tmpSymbols.add(ct.head);
            for( String s : ct.rhs )            	
            	tmpSymbols.add(s);
        }

        // add grammar symbols according to some heuristic order
        // generally want to see "more complete" parse trees
        allSymbols = new String[tmpSymbols.size()];
        symbolIndexes = new HashMap<String,Integer>();
        int k = 0;
        for( String s : tmpSymbols ) {
            symbolIndexes.put(s, k);
            allSymbols[k]=s;
            k++;
        }
	}

    
    public boolean isTerminal( int terminal ) {
    	return allSymbols[terminal].charAt(0)=='\'';
    }
    
    /**
     * Build a tree by backtracking derivations from the top matrix cell 
     * @param src -- output of lexical analysis
     * @param matrix -- output of recognition phase
     * @return
     */
    public ParseNode forest( List<LexerToken> src, Matrix matrix ) {
    	return forest(src, matrix, false);
    }

    public ParseNode forest( List<LexerToken> src, Matrix matrix, boolean full ) {
    	return forest(src, matrix, false, null);
    }

    public ParseNode forest( List<LexerToken> src, Matrix matrix, boolean full, String input ) {
        Map<Long,ParseNode> explored = new HashMap<Long,ParseNode>();
    	try {
    		int len = src.size();
    		if( len == 0 )
    			return new ParseNode(0,len, -1, this);

    		Cell cell = matrix.get(0,len);
    		if( cell != null && 0 < cell.size() ) {
    			ParseNode root = treeForACell(src, matrix, cell, 0, len/*, explored*/);
    			if( root != null )
    				return root;
    		}

    		ParseNode pseudoRoot = new ParseNode(0,len, -1, this);
    		int X = 0;
    		int Y = matrix.lastY();
    		for( int offset = 0; offset < Y; offset++ ) 
    			for( int t = 0; t < offset+1; t++ ) {
    				int x = X+t;
                    int y = Y+t-offset;
                    ParseNode coverX = pseudoRoot.coveredByOnTopLevel(x);
                    ParseNode coverY = pseudoRoot.coveredByOnTopLevel(y);
                    while( coverX != null || coverY != null ) {
    					int delta = 0;
    					if( coverX != null )
    						delta = coverX.to - y +1;
    					if( coverY != null )
    						delta = coverY.to - y +1;
    					if( delta <= 0 )
    						break;
						t += delta;
        				x = X+t;
                        y = Y+t-offset;
                        coverX = pseudoRoot.coveredByOnTopLevel(x);
                        coverY = pseudoRoot.coveredByOnTopLevel(y);
                    }
					if( offset+1 <= t )
						continue;
                    cell = matrix.get(x,y);
    				if( cell != null ) {
    				    explored = new HashMap<Long,ParseNode>();
    					ParseNode node = treeForACell(src, matrix, cell, x,y/*, explored*/);
    					if( node != null ) {
    						pseudoRoot.addTopLevel(node);
    						node.parent = pseudoRoot;
    						if( !full ) {
    							return pseudoRoot; 
    						}
    					}
    				}
    			}
    		//pseudoRoot.printTree();
    		if( full ) {
    			return pseudoRoot;
    		} else
    			return new ParseNode(0,1,-1,this);    		    
    	} catch( AssertionError e ) {
    		System.err.println("Parser.forest(): AssertionError "+e.getMessage());
    	}
        return null;        
    }
    

	public abstract ParseNode parse( List<LexerToken> src );
        
    abstract ParseNode treeForACell( List<LexerToken> src, Matrix m, Cell cell, int x, int y/*, Map<Long,ParseNode> explored*/ );

    abstract void parse( List<LexerToken> src, Matrix matrix );
        
    abstract void toHtml( int ruleNo, int pos, boolean selected, int x, int mid, int y, Matrix matrix, StringBuffer sb );

    
    public int getSymbol( String string ) {
        try {
			return symbolIndexes.get(string);
		} catch ( NullPointerException e ) { // Integer==null->int
 			// System.out.println("NPE: symbolIndexes.get("+string+")");  
			return -1;
		} catch ( Exception e ) {
 			e.printStackTrace();  // (authorized)
			return -1;
		}
    }
    
    /**
     * Output rules matching a symbol
     * Rules are printed 
     * This is alternative to to traditional way of having grammar text file, and leveraging editor search function  
     * @param earley
     * @param symbol      symbol of interest   
     *                    e.g.: search all rules that contain/or match exacly "query" in header or/and right hand side
     * @param headOnly    -- rules with header containing/or matching symbol 
     *                    e.g.: "query" substring   query_block: 'SELECT' ...
     * @param exactMatch  if false then "query" would match "query_block"
     *                    else rules with "query_block" wont be printed 
     */
    public void printOrderedRules( final String symbol, boolean headOnly, boolean exactMatch ) {
        //for( RuleTuple rule : origRules ) -- want rules in the order
        for( int i = 0; i < rules.length; i++ ) {  // Casting: this is abstract class with Earley only subclass.
                                                                  // This method is here because Earley is just too big already.
            Tuple tuple = rules[i];
            if(  (!exactMatch && allSymbols[tuple.head].contains(symbol) 
                           || exactMatch && allSymbols[tuple.head].equals(symbol)
                    ) ) 
                System.out.println(i+"     "+tuple.toString()); // (authorized)
            if( !headOnly ) {
                for( int rhsi : tuple.rhs ) {
                    if(  (!exactMatch && allSymbols[rhsi].contains(symbol) 
                            || exactMatch && allSymbols[rhsi].equals(symbol)
                     ) ) 
                        System.out.println(i+"     "+tuple.toString()); // (authorized)
                }
            }
        }
    }
    
    /**
     * To parse backwards
     * @return
     */
    public Tuple[] invertRules() {
    	Tuple[] ret = new Tuple[rules.length];
    	for ( int i = 0; i < rules.length; i++ ) {
			final Tuple rule = rules[i];
			int head = rule.head;
			int[] rhs = new int[rule.rhs.length];
			for( int j = 0; j < rhs.length; j++ ) {
				rhs[rhs.length-j-1] = rule.rhs[j];
			}
			ret[i] = new Tuple(head,rhs);
		}
		return ret;
    }    
    
    public void swapRules( String rule2, String rule3 ) {
 		Tuple t2 = null;
     	int i2 = -1; 
     	Tuple t3 = null;
     	int i3 = -1;
     	for( int i = 0; i < rules.length; i++ ) {
     		Tuple t = rules[i];
 			if( rule2.equals(t.toString()) ) {
     			t2 = t;
     			i2 = i;
     		}
 			if( rule3.equals(t.toString()) ) {
     			t3 = t;
     			i3 = i;
     		}
     	}
     	rules[i2] = t3;	
     	rules[i3] = t2;
 	}

    
    /**
     * Same as RuleTuple but with more efficient encoding of grammar symbols as int rather than String
     */
    public class Tuple implements Comparable<Tuple>, Serializable {
        public int head;
        public int[] rhs;  
        public Tuple( int h, int[] r ) {
            head = h;
            rhs = r;
        }
        public int size() {
            return rhs.length; 
        }
        public int content( int i ) {
            return rhs[i];
        }
        
        public boolean equals(Object obj) {
            return (this == obj ) || ( obj instanceof Tuple &&  compareTo((Tuple)obj)==0);
        }
        public int hashCode() {
            throw new RuntimeException("hashCode inconsistent with equals"); //$NON-NLS-1$
        }              
        public int compareTo( Tuple src ) {
            if( head==0 || src.head==0 )
                throw new RuntimeException("head==0 || src.head==0"); //$NON-NLS-1$
            int cmp = head-src.head;
            if( cmp!=0 )
                return cmp;
            cmp = rhs.length-src.rhs.length;
            if( cmp!=0 )
                return cmp;
            for( int i = 0; i < rhs.length; i++ ) {				
                cmp = rhs[i]-src.rhs[i];
                if( cmp!=0 )
                    return cmp;                    
			}
            return  0;
        }
        public String toString() {
        	StringBuilder s = new StringBuilder(allSymbols[head]+":");
        	for( int i : rhs )
        		s.append("  "+allSymbols[i]);
        	s.append(";");
            return s.toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        public String toString( int pos ) {
        	StringBuilder s = new StringBuilder(allSymbols[head]+":");
        	for( int i = 0; i < rhs.length; i++ ) {
        		s.append(' ');
        		if( pos == i )
            		s.append('!');
        		s.append(allSymbols[rhs[i]]);
			}
    		if( pos == rhs.length )
        		s.append('!');
        	s.append(";");
            return s.toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }
    
    /**
     * @param rule
     * @param pos
     * @param t -- redundant, used to order/index cell context by the active rule variable
     * @return
     */
    protected static long makeMatrixCellElem( int rule, int pos, Tuple t ) {
        long activeVar = 0;
        if( pos < t.rhs.length )
            activeVar = (long)t.rhs[pos];
        return ((long)rule << 16) | (long)pos | (activeVar << 48);
    }   
    public static int ruleFromEarleyCell( long code ) {
        return (int)((code&0xffffffffffffL)>>16);
    }   
    public static int posFromEarleyCell( long code ) {
        return (int)(code&0xffffL);    // ffffL is too generous, rule lenght in practice is 10 symbols tops
    }    
    public static int activeVarFromEarleyCell( long code ) {
        return (int)(code>>48);
    }


    public class EarleyCell implements Cell {
    	long[] content = null;
    	public EarleyCell( long[] content ) {
    		this.content = content;
    	}

    	@Override
    	public int getRule( int index ) {
    		return ruleFromEarleyCell(content[index]);
    	}

        @Override
        public int getPosition( int index ) {
             return posFromEarleyCell(content[index]);
    	}
    	
        @Override
    	public int size() {
    	    if( content == null )
    	        return 0;
    		return content.length;
    	}

        @Override
    	public long[] getContent() {
    		return content;
    	}   	
        @Override
        public void insertContent( long cellElem ) {
            content = Array.insert(content, cellElem);
        }
        
        @Override
    	public String toString() {
    		StringBuilder sb = new StringBuilder("{ ");
    		for( int i = 0; i < content.length; i++ ) {
    			if( 0 < i ) 
    				sb.append(" , ");
    			Tuple t = rules[getRule(i)]; 
    			sb.append(t.toString(getPosition(i)));
    		}
    		sb.append(" }");
    		return sb.toString();
    	}
    }

}
