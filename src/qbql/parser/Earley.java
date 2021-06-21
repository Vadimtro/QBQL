package qbql.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import qbql.util.Array;
import qbql.util.Util;

/**
 * Generic Earley parser
 * Known subclasses: SQLEarley -- for unified SQL+PL/SQL grammar 
 * @author Dim
 */
public class Earley extends Parser  {
    public int identifier = -1; 
    protected int string_literal = -1; 
    protected int digits = -1;     
    
    public boolean isCaseSensitive = true;
    
    
    /**
     * Example usage for toy grammar of arithmetic expressions
     * @param args
     * @throws Exception
     */
	public static void main( String[] args ) throws Exception {
        /*Map<Integer, int[]> forward = new HashMap<Integer, int[]>();
        Map<Integer, int[]> backward = new HashMap<Integer, int[]>();
        addDependency(1, 2, forward, backward);
        addDependency(2, 3, forward, backward);
        addDependency(0, 1, forward, backward);
        addDependency(2, 4, forward, backward);
        System.out.println("forward ------------------");
        for( int from : forward.keySet() ) 
            for( int to : forward.get(from) )
                System.out.println(from+"->"+to);
        System.out.println("backward ------------------");
        for( int from : backward.keySet() ) 
            for( int to : backward.get(from) )
                System.out.println(from+"->"+to);*/

	    
		//Util.profile(10000, 50);

        String input = 
        	//"a lindy swings"
            //Util.readFile(Earley.class, "test.sql") //$NON-NLS-1$
                "1+2+3*4" 
        ;
        List<LexerToken> src =  new Lex().parse(input);
        System.out.println("src.size()="+src.size());
	
        Set<RuleTuple> wiki = new TreeSet<RuleTuple>();
        wiki.add(new RuleTuple("P",new String[]{"S"}));
        wiki.add(new RuleTuple("S",new String[]{"S","'+'","M"}));
        wiki.add(new RuleTuple("S",new String[]{"M"}));
        wiki.add(new RuleTuple("M",new String[]{"T","'*'","M"}));
        wiki.add(new RuleTuple("M",new String[]{"T"}));
        wiki.add(new RuleTuple("T",new String[]{"'('","P","')'"}));
        wiki.add(new RuleTuple("T",new String[]{"digits"}));
        wiki.add(new RuleTuple("T",new String[]{"identifier"}));
        wiki.add(new RuleTuple("T",new String[]{"string_literal"}));
        
        /*wiki.add(new RuleTuple("G",new String[]{"A","B","C"}));
        wiki.add(new RuleTuple("A",new String[]{"digits","digits",}));
        wiki.add(new RuleTuple("B",new String[]{"identifier"}));
        wiki.add(new RuleTuple("C",new String[]{"string_literal","string_literal","string_literal"}));*/

        
        Set<RuleTuple> rules = new TreeSet<RuleTuple>();
        /*final Parsed p = new Parsed(
                                             Util.readFile(Earley.class, "pereira.grammar"), //$NON-NLS-1$
                                             Grammar.bnfParser(),
                                             "grammar" //$NON-NLS-1$
                                     );
        //p.debug = true;
        Grammar.grammar(p.getRoot(), p.getSrc(), rules);
        */ 
        rules = wiki;
        Earley earley = new Earley(rules) {
            /*protected void initCell00( List<LexerToken> src, Matrix matrix ) {
                matrix.initCells(src.size());
                int S = symbolIndexes.get("S");
                initCell(matrix, new int[]{S},0);
            }*/            
        };
        earley.isCaseSensitive = true;
        Matrix matrix = new Matrix(earley);
        Visual visual = null;
        visual = new Visual(src, earley);

        long t1 = System.currentTimeMillis();
        earley.parse(src, matrix); 
        long t2 = System.currentTimeMillis();
        System.out.println("Earley parse time = "+(t2-t1)); // (authorized) //$NON-NLS-1$
        System.out.println("#tokens="+src.size());
        
        if( visual != null )
        	visual.draw(matrix);
        else
        	System.out.println(matrix.toString());
        ParseNode out = earley.forest(src, matrix);
        out.printTree();
	}

	protected void initCell00( List<LexerToken> src, Matrix matrix ) {
        long t1 = 0;
        if( matrix.visual != null )
            t1 = System.nanoTime();
        long[] content = null;
        for( int i = 0; i < rules.length; i++ ) {
            Tuple t = rules[i];
            String head = allSymbols[t.head];
            if( head.charAt(head.length()-1) != ')' )
                content = Array.insert(content,makeMatrixCellElem(i, 0, t));
        }
        matrix.initCells(src.size());
        matrix.put(0, 0, new EarleyCell(content));
        matrix.allXs = Array.insert(matrix.allXs,0);
        if( matrix.visual != null  ) {
            long t2 = System.nanoTime();
            matrix.visual.visited[0][0] += (int)(t2-t1);
        }
	}
	
	/**
	 * principal recognition phase combining essential Earley parsing steps: scan, complete, predict 
	 * @param src -- output of lexical analysis
	 * @param matrix -- parser output 
	 */
    public synchronized void parse( List<LexerToken> src, Matrix m ) {
    	Matrix matrix = (Matrix) m;
        try {
            initCell00(src, matrix);
            predict(matrix);
            while( true ) {
                //int before = matrix.size();
                if( !scan(matrix, src) )
                    break;
                complete(matrix, src.size());
                predict(matrix);
                //if( before == matrix.size() )
                //break;
            }
        } catch( Exception e ) {
            for( StackTraceElement elem : e.getStackTrace() ) {
                if( elem.toString().contains("UnitTest.assertion" ) 
                 || elem.toString().contains("SqlEarley.main" )      
                ) {
                    System.err.println(e.toString());
                    System.err.println("matrix.lastY="+matrix.lastY()+", src.size()="+src.size());
                    break;
                }
            }
        }
    }
	
    @Override
    public ParseNode parse( List<LexerToken> src ) {
        Matrix matrix = new Matrix(this);
        parse(src, matrix); 
        return forest(src, matrix);
        //return new TreeBuilder(matrix,this).build();
    }

    
	/**
	 * Instantiate parser
	 * @param originalRules -- grammar
	 */
    public Earley( Collection<RuleTuple> originalRules ) {
        this(originalRules, true);
    }
	public Earley( Collection<RuleTuple> originalRules, boolean precomputePredictions ) {
		super(originalRules);
        identifier = symbolIndexes.get("identifier");
        try { 
            string_literal = symbolIndexes.get("string_literal");
        } catch( NullPointerException e ) { /* symbols not present in some grammars */ }
        try { 
            digits = symbolIndexes.get("digits");
        } catch( NullPointerException e ) { /* symbols not present in some grammars */ }
        
	}
	
	/*
	 *  uniqueTerminalPredictions[symbolIndexes("group_by_clause)] = symbolIndexes("''GROUP''")
	 *  uniqueTerminalPredictions[symbolIndexes("select_list")] = null;
	 */
	protected PredictedTerminals[] terminalPredictions = null;

    // precomputed  symbol -> predicted rules
	public Map<Integer, long[]> predicts = null;
	
	protected void precomputePredictions() {
	        Map<Integer, int[]> closure = new HashMap<Integer, int[]>();
	        Map<Integer, long[]> symbolHead2rules = new HashMap<Integer, long[]>(); 
	        
	        for( int i = 0; i < rules.length; i++ ) {
	            int[] tmp = closure.get(rules[i].head);
	            long[] tmp1 = symbolHead2rules.get(rules[i].head);
	            tmp = Array.insert(tmp, rules[i].rhs[0]);
	            tmp1 = Array.insert(tmp1, makeMatrixCellElem(i,0,rules[i]) );
	            closure.put(rules[i].head, tmp);
	            symbolHead2rules.put(rules[i].head, tmp1);
	        }
	//for( int k : symbolHead2rules.keySet() ) 
	//for( int n : symbolHead2rules.get(k) )
	//System.out.println(allSymbols[k]+" -> "+rules[n]);
	        
	        while( true ) {
	            int before = size(closure);
	            for( int k : closure.keySet() ) {
	                final int[] v = closure.get(k);
	                int[] tmp = Array.merge(v, new int[0]);
	                for( int n : v ) {
	                    tmp = Array.merge(tmp, closure.get(n));
	                }
	                closure.put(k, tmp);
	            }
	            if( before == size(closure) )
	                break;
	        }
	        
	//for( int k : closure.keySet() ) 
	//for( int n : closure.get(k) )
	//System.out.println(allSymbols[k]+"->"+allSymbols[n]);
	        terminalPredictions = new PredictedTerminals[allSymbols.length];
	        
	        for( int k : closure.keySet() ) {
	            long[] tmp = symbolHead2rules.get(k);
	            for( int n : closure.get(k) ) {
	                tmp = Array.merge(tmp, symbolHead2rules.get(n));
	                
	                String rhs0 = allSymbols[n];
	                if( rhs0.charAt(0)=='\'' ) {
	                    if( terminalPredictions[k] == null )
	                        terminalPredictions[k] = new PredictedTerminals();
	                    terminalPredictions[k].add(n);
	                    continue;
	                }
	                if( n == identifier 
	                 || n == digits
	                 || n == string_literal
	                ) {
	                    if( terminalPredictions[k] == null )
	                        terminalPredictions[k] = new PredictedTerminals();
	                    terminalPredictions[k].invalidate();
	                }

	            }
	            predicts.put(k, tmp);	            
	        }
	           

	    }
	    

	/**
	 * Incremental transitive closure:
	 * Inserting single ref->def would create multiple links between br and fd
	 *   __                  __
	 *  /  \    ref  def    /  \
	 * | br | ==> ---> ==> | fd |
	 *  \__/                \__/
	 *   
	 * @param ref
	 * @param def
	 * @param forward
	 * @param backward
	 */
    private static void addDependency( int ref, int def, Map<Integer, int[]> forward, Map<Integer, int[]> backward ) {
        int[] bd = backward.get(def);
        bd = Array.insert(bd, def);
        backward.put(def, bd);
        int[] fr = forward.get(ref);
        fr= Array.insert(fr,ref);        
        forward.put(ref, fr);
        int[] br = backward.get(ref);
        br= Array.insert(br,ref);        
        backward.put(ref, br);
        int[] fd = forward.get(def);
        fd= Array.insert(fd,def);        
        forward.put(def, fd);
        
        Map<Integer, int[]> newForward = new HashMap<Integer, int[]>();
        Map<Integer, int[]> newBackward = new HashMap<Integer, int[]>();
        for( int from : br )
            for( int to : fd ) {
                int[] dependents = newForward.get(from);
                dependents = Array.insert(dependents,to); 
                newForward.put(from, dependents);
                int[] referents = newBackward.get(to);
                referents = Array.insert(referents,from); 
                newBackward.put(to, referents);
            }
        for( int key : newForward.keySet() ) {
            int[] existing = forward.get(key);
            if( existing == null )
                forward.put(key, newForward.get(key));
            else {
                existing = Array.merge(existing, newForward.get(key));
                forward.put(key, existing);
            }
        }
        for( int key : newBackward.keySet() ) {
            int[] existing = backward.get(key);
            if( existing == null )
                backward.put(key, newBackward.get(key));
            else {
                existing = Array.merge(existing, newBackward.get(key)); 
                backward.put(key, existing);
            }
        }
    }

	
		
	private int size( Map<Integer, int[]> closure ) {
		int ret = 0;
		for( int[] tmp : closure.values() )
			ret += tmp.length;
		return ret;
	}


	protected boolean scan( Matrix matrix, List<LexerToken> src ) {
		int y = matrix.lastY();
		if( src.size() <= y  ) {
			return false;
		}
        
		LexerToken token = src.get(y);
        Integer suspect = symbolIndexes.get("'" + (isCaseSensitive?token.content:token.content.toUpperCase()) + "'");
        boolean ret = false;
        for( int i = matrix.allXs.length-1; 0 <= i; i-- ) {
        	int x = matrix.allXs[i]; 
            if( scan(matrix, y, src, x, suspect) )
                ret = true;
        }
        if( scan(matrix, y, src, y, suspect) )
            ret = true;
		return ret;
	}
	private boolean scan( Matrix matrix, int y, List<LexerToken> src, int x, Integer suspect ) {
        long t1 = 0;
        if( matrix.visual != null )
            t1 = System.nanoTime();
		long[] content = null;
		EarleyCell candidateRules = matrix.get(x,y);
		if( candidateRules == null )
			return false;
		for( int j = 0; j < candidateRules.size(); j++ ) {
			int pos = candidateRules.getPosition(j);
			int ruleNo = candidateRules.getRule(j);
			Tuple t = rules[ruleNo];
			
			if( t.size()-1 < pos )
				continue;
			if( isScannedSymbol(y,src, pos, t, suspect) ) {
                if( !lookaheadOK(t,pos+1,matrix) )
                    continue;
				content = Array.insert(content,makeMatrixCellElem(ruleNo, pos+1, t));
                if( t.rhs.length == pos+1 )
                    matrix.enqueue(Util.lPair(x, t.head));
			}
		}
        if( matrix.visual != null ) {
            long t2 = System.nanoTime();
            matrix.visual.visited[x][y+1] += (int)(t2-t1);
        }
		if( content == null )
			return false;
		matrix.put(x, y+1, new EarleyCell(content));
		matrix.allXs = Array.insert(matrix.allXs,x);	
		return true;
	}


	protected boolean isScannedSymbol( int y, List<LexerToken> src, int pos, Tuple t, Integer suspect ) {
		int symbol = t.content(pos);
		LexerToken token = src.get(y);
		if( symbol == digits && token.type == Token.DIGITS )
			return true;
		if( symbol == string_literal && token.type == Token.QUOTED_STRING )
			return true;
		return suspect != null && suspect == symbol 
			|| isIdentifier(y,src, symbol, suspect) && (suspect==null||notConfusedAsId(suspect,t.head,pos))
		;
	}


	// symbol @ pos within the rule with head, e.g.
	// begin dummy1 : = 'N'
	// doesn't scan to 
	// object_d_rhs: constrained_type default_expr_opt
    protected boolean notConfusedAsId(int symbol, int head, int pos) {
        return true;
    }
    protected boolean isIdentifier( int y, List<LexerToken> src, int symbol, Integer suspect ) {
        if( symbol != identifier )
            return false;
		LexerToken token = src.get(y);
        return symbol == identifier && token.type == Token.IDENTIFIER;
    }
	
    protected void predict( Matrix matrix ) {
    	if( predicts == null ) {
    		predicts = new HashMap<Integer, long[]>();
    		precomputePredictions();
    	}
        long t1 = 0;
        if( matrix.visual != null )
            t1 = System.nanoTime();
		int y = matrix.lastY();
		
		EarleyCell cell = matrix.get(y,y);
		long[] content = null;
		if( cell != null )
			content = cell.content;
		
		int[] symbols = new int[0];  // = null --> possible NPE in "for( int symbol : symbols )" 
        Map<Integer,EarleyCell> xRange = matrix.getXRange(y);
        //SortedMap<Long,EarleyCell> range = matrix.subMap(Util.lPair(0, y), true, last, true);
        for( int mid : xRange.keySet() ) {
        	EarleyCell candidateRules = matrix.get(mid,y);
        	for( int j = 0; j < candidateRules.size(); j++ ) {
        		int pos = candidateRules.getPosition(j);
        		int ruleNo = candidateRules.getRule(j);
    			Tuple t = rules[ruleNo];
    			if( t.size() <= pos )
    				continue;
                int symbol = t.content(pos);
                    
//if(y==5)
//if( t.toString().startsWith("percentile_cont") )
//y=5;
                
                symbols = Array.insert(symbols, symbol);
        	}
        }
        for( int symbol : symbols ) {
            if( matrix.LAsuspect!=null ) {
                PredictedTerminals terminal = terminalPredictions[symbol];
                if( terminal != null )
                    if( !terminal.matches(matrix.LAsuspect) )
                        continue;
            }
            content = Array.merge(content,predicts.get(symbol));
        }
        
        if( matrix.visual != null ) {
            long t2 = System.nanoTime();
            matrix.visual.visited[y][y] += (int)(t2-t1);
        }
        if( content != null && content.length > 0 ) 
        	matrix.put(y,y, new EarleyCell(content));
        else
        	return;
        
	}
	
	public boolean skipRanges = true;
	
    protected void complete( Matrix matrix, int srcLength ) {
        Map<Integer,Integer> skipIntervals = new HashMap<Integer,Integer>();
        while( true ) {
            long completionCandidate = matrix.dequeue();
            if( completionCandidate == -1 )
                break;
            int symbol = Util.lY(completionCandidate);
            int mid = Util.lX(completionCandidate);
            
            int y = matrix.lastY();
//if(y==13 && mid == 10)
//System.out.println("y==2");

            int indexX = Array.indexOf(matrix.allXs,mid);
            if( matrix.allXs.length-1 < indexX )
                indexX = matrix.allXs.length-1;
            if( mid < matrix.allXs[indexX] )
                indexX--;
            //for( int x = mid; 0 <= x; x-- ) {
            for( int i = indexX; 0 <= i; i-- ) {
                int x = matrix.allXs[i]; 
                int skipTo = y;
                
                long t1 = 0;
                if( matrix.visual != null )
                    t1 = System.nanoTime();
                
                EarleyCell pres = matrix.get(x, mid);
                if( pres == null ) {
                    if( matrix.visual != null ) {
                        long t2 = System.nanoTime();
                        matrix.visual.visited[x][y]=Util.addlY(matrix.visual.visited[x][y],(int)(t2-t1));
                    }
                    continue;
                }
                
                long mask = ((long)symbol)<<48;
                int start = Array.indexOf(pres.getContent(), mask);
                int stop = Array.indexOf(pres.getContent(), mask|0xffffffffffffL)+1;
                
                EarleyCell content =  (EarleyCell) matrix.get(x, y);
                
                for( int ii = start; ii < stop && ii < pres.size(); ii++ ) {
                    int dotPre = pres.getPosition(ii);
                    int rulePre = pres.getRule(ii);
                    Tuple tPre = rules[rulePre];
                    if( tPre.size() == dotPre )
                        continue;
                    int symPre = tPre.content(dotPre);
                    if( symPre != symbol )
                        continue;
                    if( y < srcLength && !lookaheadOK(tPre,dotPre+1,matrix) )
                        continue;
                    if( content == null )
                        content = new EarleyCell(null);
                    long promotedRule = makeMatrixCellElem(rulePre, dotPre+1, tPre);
                    
                    int before = content.size();
                    content.insertContent(promotedRule);
                    int after = content.size();
                    
                    if( before < after ) {
                        matrix.put(x, y,content);
                        if( skipRanges && tPre.rhs.length==dotPre+1 
                                && mid < skipTo 
                                && allSymbols[tPre.head].charAt(0)!='"'
                                && allSymbols[symPre].charAt(0)!='"'
                        ) {
                            skipTo = mid;
//if( x+1==7 && skipTo==11 )
//System.out.println("----");
                        }
                    }

                    if( tPre.size() == dotPre+1 && before < after) {
                        matrix.enqueue(Util.lPair(x, tPre.head));
                        //System.out.println("x="+x+",y="+y+"  "+allSymbols[tPre.head]+"  "+matrix.completionQueue.size());
                    }
                }
                if( matrix.visual != null ) {
                    long t2 = System.nanoTime();
                    matrix.visual.visited[x][y]=Util.addlY(matrix.visual.visited[x][y],(int)(t2-t1));
                }

                //System.out.println("x="+x+",y="+y+", skipTo="+skipTo);
                if( skipRanges && x < skipTo && skipTo < y /*TEST#78 broken: && x+1 < skipTo*/ ) {
                    Integer predecessor = skipIntervals.get(x+1);
                    if( predecessor == null || skipTo < predecessor )
                    	/*if( x+1 < skipTo )*/ skipIntervals.put(x+1, skipTo);
                }
            }

        }
        for( int x : skipIntervals.keySet() ) {
            int y = skipIntervals.get(x);
            //System.out.println("skip from "+x+" to "+y);
            /*for( int k = x; k < y; k++ ) {
                matrix.allXs = Array.delete(matrix.allXs,k);
            }*/

            if( x < y )
            	matrix.allXs = Array.delete(matrix.allXs,x,y);
        }
    }

	
    // Lookahead a keyword and dismiss mismatching tuples
	protected boolean lookaheadOK( Tuple tPre, int pos, Matrix matrix ) {
		return true;
	}

	void toHtml( int ruleNo, int pos, boolean selected, 
			int x, int mid, int y, Matrix matrix, StringBuffer sb ) {
	    Tuple rule = rules[ruleNo];
	    String size = "+1";
	    if( selected ) {
	    	sb.append("<b>");
	    	size = "+2";
	    }
	    sb.append("<font size="+size+" color=blue>"+allSymbols[rule.head]+":</font> ");
	    final String greenish = "<font size="+size+" bgcolor=rgb(150,200,150))>";
	    final String bluish = "<font size="+size+" bgcolor=rgb(150,150,200))>";
	    sb.append(greenish);
	    for( int i = 0; i < rule.rhs.length; i++ ) {			
		    if( pos == i )
			    sb.append("</font>"+bluish);
		    sb.append(allSymbols[rule.rhs[i]]+" ");
		}
	    sb.append("</font>");		
	    if( selected ) 
	    	sb.append("</b>");
	    	    
        if( mid == -1 )
            return;
	    if( selected && x+y!=0) {
	    	if( mid < x || x == y ) {
		    	sb.append("<i> predict from </i>");
		    	EarleyCell bc = matrix.get(mid, y);
	        	for( int j = 0; j < bc.size(); j++ ) {
	        		int bp = bc.getPosition(j);
	        		int br = bc.getRule(j);
                    Tuple bt = rules[br];
                    if( bp < bt.rhs.length && bt.rhs[bp] == rule.head ) {
        		    	//sb.append("<font size="+size+" color = green>");
                    	//toString(br,bp,sb);
        		    	//sb.append("</font>");
        		    	toHtml(br,bp,false, -1,-1,-1, null, sb);
                    	return;
                    }
	        	}
	    	} else if( y < mid ) {
		    	sb.append("<i> scan from </i>");
		    	EarleyCell bc = matrix.get(x, y-1);
	        	for( int j = 0; j < bc.size(); j++ ) {
	        		int bp = bc.getPosition(j);
	        		int br = bc.getRule(j);
                    Tuple bt = rules[br];
                    if( br == ruleNo && bp+1 == pos ) {
        		    	//sb.append("<font size="+size+" color = green>");
                    	//toString(br,bp,sb);
        		    	//sb.append("</font>");
        		    	toHtml(br,bp,false, -1,-1,-1, null, sb);
                    	return;
                    }
	        	}
	    	} else {
		    	sb.append("<i> complete from </i>");
		    	boolean secondTime = false;
		    	EarleyCell pre = matrix.get(x, mid);
		    	EarleyCell post = matrix.get(mid, y);
	        	for( int i = 0; i < pre.size(); i++ ) 
	        		for( int j = 0; j < post.size(); j++ ) {
                        int dotPre = pre.getPosition(i);
                        int dotPost = post.getPosition(j);
                        int rulePre = pre.getRule(i);
                        int rulePost = post.getRule(j);                            
                        Tuple tPre = rules[rulePre];
                        Tuple tPost = rules[rulePost];
                        if( tPre.size()!=dotPre && tPost.size()!=dotPost )
                            continue;
                        if( tPost.size() == dotPost ) {
                            if( rulePre != ruleNo )
                            	continue;
                        	if( dotPre+1 != pos )
                        		continue;
                            int symPre = tPre.content(dotPre);
                            if( symPre != tPost.head )
                                continue;
                            if( secondTime )
                                sb.append("<b> or </b>");                                
            		    	toHtml(rulePre,dotPre,false, -1,-1,-1, null, sb);
            		    	sb.append("<i> and </i>");
            		    	toHtml(rulePost,dotPost,false, -1,-1,-1, null, sb);
                        	secondTime = true;
                        }	        		
	        	}
	    	}
	    }
	}
	void toString( int ruleNo, int pos, StringBuffer sb ) {
	    Tuple rule = rules[ruleNo];
	    sb.append(rule.toString(pos));		
	}
	
	

    /**
     * Initialize Earley initial matrix cell with grammar symbols other than "sequence of statements"
     * e.g. when want to recognize SQL fragment such as group by clause
     * @param matrix
     * @param heads
     * @param pos
     */
	public void initCell( Matrix matrix, int[] heads, int pos ) {
		long[] content = null;
		for( int i = 0; i < rules.length; i++ ) {
			Tuple t = rules[i];
			for( int h : heads ) 
				if( t.head == h ) {
					content = Array.insert(content,makeMatrixCellElem(i, 0, t));
					break;
				}
		}
		matrix.put(pos, pos, new EarleyCell(content));
		matrix.allXs = Array.insert(matrix.allXs,pos);
	}


    @Override
    public ParseNode treeForACell( List<LexerToken> src, Matrix m, EarleyCell cell, int x, int y/*, Map<Long,ParseNode> explored*/ ) {
		//explored = new HashMap<Long,ParseNode>();
        int rule = -1;
        int pos = -1;
        for( int i = 0; i < cell.size(); i++ ) {
            rule = cell.getRule(i);
            pos = cell.getPosition(i);
            if( rules[rule].rhs.length == pos )
                return tree(src, m, x, y, rule,pos/*, explored*/);
        }
        if( rule != -1 && pos != -1 /*&& x+1 == y*/ )
            return tree(src, m, x, y, rule,pos/*, explored*/);
        return null;
    }

    public boolean isAsc = false;
	protected ParseNode tree( List<LexerToken> src, Matrix m, int x, int y, int rule, int pos/*, Map<Long,ParseNode> explored*/ ) {
//System.out.println("["+x+","+y+")");
//if(x==8&&y==31)
//x=8;
		// follow scan
		if( pos != 0 ) {
			EarleyCell pre = (EarleyCell) m.get(x,y-1);
			if( pre != null ) {
				long demotedRule = makeMatrixCellElem(rule,pos-1, rules[rule]);
				int indexOfDemotedRule = Array.indexOf(pre.content,demotedRule);
				long ruleAtTheIndex = pre.content[indexOfDemotedRule];
				if( ruleAtTheIndex == demotedRule ) {
					Tuple t = rules[rule];
					LexerToken token = src.get(y-1); 
					Integer suspect = symbolIndexes.get("'" + (isCaseSensitive?token.content:token.content.toUpperCase()) + "'");
					if( isScannedSymbol(y-1,src, pos-1, t,suspect) ) {
						ParseNode branch = new ParseNode(y-1,y, rules[rule].rhs[pos-1], this);
						if( x+1 == y ) {
							if( rules[rule].rhs.length == 1 )
								branch.addContent(rules[rule].head);
							return branch;
						}
						int head = rules[rule].head;
						if( pos != rules[rule].rhs.length ) {
							head = -1;
						}
						ParseNode ret = new ParseNode(x,y,head,head, this);
						ret.lft = tree(src,m, x,y-1,rule,pos-1/*, explored*/);
						ret.lft.parent = ret;
						ret.rgt = branch;
						ret.rgt.parent = ret;
						return ret;
					}
				}
			}    	
		}
    	
    	// follow complete
    	//System.out.println("try complete["+x+","+y+")");
		//System.out.println("rule#"+rule+"="+rules[rule].toString(pos));
		if( pos != 0 ) {
			long demotedRule = makeMatrixCellElem(rule,pos-1, rules[rule]);
			TreeMap<Integer, EarleyCell> cellsAtY = (TreeMap<Integer, EarleyCell>) m.getXRange(y);
			for( int mid : isAsc? cellsAtY.keySet(): cellsAtY.descendingKeySet() ) {
				//if( mid < x )
					//break;
			//for( int mid = y-1; x <= mid; mid-- ) {
				EarleyCell pre = (EarleyCell) m.get(x,mid);
				if( pre == null )
					continue;
				EarleyCell post =  m.get(mid,y);
				if( post == null )
					continue;

				if( pre.content[Array.indexOf(pre.content,demotedRule)] == demotedRule ) {
					//for( int j = post.size()-1; 0 <= j ; j-- ) {
					//for( int j = 0; j <= post.size()-1 ; j++ ) {
					for( long l : post.orderedContent() ) {
						int rJ = ruleFromEarleyCell(l);
						int pJ = posFromEarleyCell(l);
						//Tuple tPost = rules[rJ];
						if( rules[rJ].rhs.length != pJ )
							continue;
						if( rules[rJ].head != rules[rule].rhs[pos-1] )
							continue;

//System.out.println("rules["+rJ+"]="+rules[rJ].toString(pJ));
						if( x != mid ) {
							ParseNode ret = new ParseNode(x,y,rules[rule].rhs.length != pos ? -1 : rules[rule].head, this);
							ret.lft = tree(src,m, x,mid,rule,pos-1/*, explored*/);
							ret.lft.parent = ret;
							ret.rgt = tree(src,m, mid,y,rJ,pJ/*, explored*/);
							ret.rgt.parent = ret;
							return ret;
						} else if( rJ != rule || pJ != pos ) { //  StackOverflow
							//if(mid==8 && y==30) {
							    //System.out.println("mid="+mid+",y="+y+"   "+rules[rJ].toString(pJ));
							//}
							ParseNode ret = tree(src,m, mid,y,rJ,pJ/*, explored*/);
							if( rules[rule].rhs.length == pos )
								ret.addContent(rules[rule].head);
							return ret;
						}
					}
					//throw new AssertionError("VT: unexpected completion case "+rules[rule].toString(pos)+" @["+x+","+y+")");
				}    
			}       
		}
		throw new AssertionError("unwind "+rules[rule].toString(pos)+" @["+x+","+y+")");
    }
	
    public class PredictedTerminals implements Serializable {
        private boolean isValid = true;
        int[] symbols = null;
        
        void add( int sym ) {
            if( isValid  )
                symbols = Array.insert(symbols, sym);
            else 
                invalidate();
        }
        
        void invalidate() {
            symbols = null;
            isValid = false;
        }

        public boolean matches( Integer lookahead ) {
            if( !isValid )
                return true;
            if( symbols == null )
                return true;
            return lookahead!=null && symbols[Array.indexOf(symbols, lookahead)] == lookahead;
        }
        
        @Override
        public String toString() {
            if( !isValid )
                return "*invalid*";
            /*if( symbol == -1 )
                return "symbol == -1";
            return allSymbols[symbol];*/
            if( symbols == null )
                return "*symbols == null*";
            StringBuilder ret = new StringBuilder("{");
            for( int s : symbols ) {
                ret.append(allSymbols[s]);
                ret.append(',');
            }
            return ret.toString();
        }
    }

}




