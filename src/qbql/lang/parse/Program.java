package qbql.lang.parse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import qbql.parser.Earley;
import qbql.parser.Grammar;
import qbql.parser.Lex;
import qbql.parser.LexerToken;
import qbql.parser.ParseNode;
import qbql.parser.RuleTuple;
import qbql.util.Pair;
import qbql.util.Util;

public class Program {
    
    public static boolean debug = false;
    
    protected Earley parser;
	public Program( Earley parser ) {
		this.parser = parser;
	}
	

    private static int atomic_predicate = -1;
    private static int bind_var = -1;
    private static int backslash = -1;
    private static int closePar = -1;
    private static int closeBr = -1;
    private static int col = -1;
    private static int conj = -1;
    private static int disj = -1;
    private static int dot = -1;
    private static int eq = -1;
    private static int excl = -1;
    private static int identifier = -1;
    private static int header = -1;
    private static int lt = -1;
    private static int minus = -1;
    private static int node_parent = -1;
    private static int node_predecessor = -1;
    private static int node_position = -1;
    private static int node_successor = -1;
    private static int node = -1;
    private static int openBr = -1;
    private static int openPar = -1;
    private static int output = -1;
    private static int predicate = -1;
    private static int rule = -1;
    private static int srcPtr = -1;
    private static int semicol = -1;
    private static int slash = -1;
    private static int statement = -1;
    private static int string_literal = -1;
	public static Earley getArboriParser() throws IOException  {
		Set<RuleTuple> rules = getRules();
        //RuleTuple.printRules(rules);
        Earley testParser = new Earley(rules);
        atomic_predicate = testParser.symbolIndexes.get("atomic_predicate"); //$NON-NLS-1$
        bind_var = testParser.symbolIndexes.get("bind_var"); //$NON-NLS-1$
        backslash = testParser.symbolIndexes.get("'\\'"); //$NON-NLS-1$
        closePar = testParser.symbolIndexes.get("')'"); //$NON-NLS-1$
        closeBr = testParser.symbolIndexes.get("']'"); //$NON-NLS-1$
        col = testParser.symbolIndexes.get("':'"); //$NON-NLS-1$
        conj = testParser.symbolIndexes.get("'&'"); //$NON-NLS-1$
        disj = testParser.symbolIndexes.get("'|'"); //$NON-NLS-1$ 
        dot = testParser.symbolIndexes.get("'.'"); //$NON-NLS-1$
        eq = testParser.symbolIndexes.get("'='"); //$NON-NLS-1$
        excl = testParser.symbolIndexes.get("'!'"); //$NON-NLS-1$ 
        identifier = testParser.symbolIndexes.get("identifier"); //$NON-NLS-1$ 
        header = testParser.symbolIndexes.get("header"); //$NON-NLS-1$ 
        lt = testParser.symbolIndexes.get("'<'"); //$NON-NLS-1$ 
        minus = testParser.symbolIndexes.get("'-'"); //$NON-NLS-1$
        node_parent = testParser.symbolIndexes.get("node_parent"); //$NON-NLS-1$
        node_position = testParser.symbolIndexes.get("node_position"); //$NON-NLS-1$
        node_predecessor = testParser.symbolIndexes.get("node_predecessor"); //$NON-NLS-1$
        node_successor = testParser.symbolIndexes.get("node_successor"); //$NON-NLS-1$
        node = testParser.symbolIndexes.get("node"); //$NON-NLS-1$
        openBr = testParser.symbolIndexes.get("'['"); //$NON-NLS-1$
        openPar = testParser.symbolIndexes.get("'('"); //$NON-NLS-1$
        output = testParser.symbolIndexes.get("output"); //$NON-NLS-1$
        predicate = testParser.symbolIndexes.get("predicate"); //$NON-NLS-1$
        rule = testParser.symbolIndexes.get("rule"); //$NON-NLS-1$
        semicol = testParser.symbolIndexes.get("';'"); //$NON-NLS-1$
        slash = testParser.symbolIndexes.get("'/'"); //$NON-NLS-1$
        srcPtr = testParser.symbolIndexes.get("'?'"); //$NON-NLS-1$
        statement = testParser.symbolIndexes.get("statement"); //$NON-NLS-1$
        string_literal = testParser.symbolIndexes.get("string_literal"); //$NON-NLS-1$
		return testParser;
	}
	private static Set<RuleTuple> getRules() throws IOException  {
        String input = Util.readFile(Program.class, "arbori.grammar"); //$NON-NLS-1$
        List<LexerToken> src = new Lex(false,true,false,new HashMap()).parse(input); 
        ParseNode root = Grammar.parseGrammarFile(src, input);
        Set<RuleTuple> ret = new TreeSet<RuleTuple>();
        Grammar.grammar(root, src, ret);
        return ret;
    }

	private List<String> execOrder = new LinkedList<String>();
	Map<String, Predicate> namedPredicates = new HashMap<String, Predicate>();
	// namedPredicates is mutating, so restore it for subsequent executions from symbolicPredicates
    Map<String,Predicate> symbolicPredicates = new HashMap<String,Predicate>();
	
    private PredicateDependency dependency = new PredicateDependency();
    
    /**
     * Processes the top rule of the arbori syntax: 
    program:
        statement
      | program statement
    ;

     */
    public void program( ParseNode root, List<LexerToken> src, String input ) {
    	if( root.contains(statement) ) {
    		statement(root, src, input);
            copyPredicates();
    		return;
    	}
		for( ParseNode child : root.children() )
			program(child, src, input);
		
		copyPredicates();
	}

    private void copyPredicates() {
        for( String key : namedPredicates.keySet() )
		    symbolicPredicates.put(key, namedPredicates.get(key));
    }

    private void statement( ParseNode root, List<LexerToken> src, String input ) {
        if( root.contains(rule) )
            rule(root, src, input);
        else if( root.contains(output) )
            output(root, src, input);
    }
    /**
     * Processes arbori syntax: 
     * 
       rule:
        identifier ':' predicate ';'
       ;
     */
	private void rule( ParseNode root, List<LexerToken> src, String input ) {
		String first = null;
		boolean legitimateOper = false;
		//ParseNode second = null;
		for( ParseNode child : root.children() ) {
			if( first == null ) {
				if( child.from+1==child.to )
					first = child.content(src);
				else
					return;
				continue;
			}
			
			if( !legitimateOper ) {
				if( child.contains(col) )
					legitimateOper = true;
				else
					return;
				continue;
			}
			
	    	if( child.contains(predicate) ) {
	    		Predicate p = predicate(child, src, input);
	            /*if( debug ) {
	                System.out.println("***predicate***="+first); //$NON-NLS-1$
	        		final Set<String> allVariables = new HashSet<String>();
	        		p.variables(allVariables);
	                System.out.println("symbolTable="+allVariables.toString()); //$NON-NLS-1$
	                System.out.println("nodeFilter="+p.toString()); //$NON-NLS-1$
	            }*/
	    		Map<String, Boolean> dependencies = p.dependencies();
                for( String s : dependencies.keySet() )
	    		    dependency.addDependency(s, first, dependencies.get(s));
	            execOrder.add(first);
	            namedPredicates.put(first, p);

	    		return;
	    	}
		}
	}	

	private Set<String> outputRelations = new HashSet<String>();
    private void output( ParseNode root, List<LexerToken> src, String input ) {
        for( ParseNode child : root.children() ) {
            outputRelations.add(child.content(src));
        }
    }
    
	private Predicate predicate( ParseNode root, List<LexerToken> src, String input ) {
	    if( root.contains(identifier) ) 
            return new PredRef(root.content(src),this);
	    
        Predicate ret = isBrackets(root, src, input);
        if( ret != null )
            return ret;
		ret = isParenthesis(root, src, input);
		if( ret != null )
			return ret;
		ret = isConjunction(root, src, input);
		if( ret != null )
			return ret;
		ret = isDisjunction(root, src, input);
		if( ret != null ) 
			return ret;
        ret = isDifference(root, src, input);
        if( ret != null ) 
            return ret;
		ret = isAtomicPredicate(root, src, input);
		if( ret != null )
			return ret;
		throw new AssertionError("unexpected case for: "+root.content(src));
	}

	/**
	 * Processes arbori syntax: predicate & predicate
	 */
	private Predicate isConjunction( ParseNode root, List<LexerToken> src, String input ) {
		ParseNode first = null;
		boolean legitimateOper = false;
		//ParseNode second = null;
		for( ParseNode child : root.children() ) {
			if( first == null ) {
				first = child;
				continue;
			}
			
			if( !legitimateOper ) {
				if( child.contains(conj) )
					legitimateOper = true;
				else
					return null;
				continue;
			}
			
			Predicate lft = predicate(first, src, input);
			Predicate rgt = predicate(child, src, input);
			return new CompositeExpr(lft, rgt, Oper.CONJUNCTION);
		}
		return null;
	}
	
    /**
     * Processes arbori syntax: predicate | predicate
     */
	private Predicate isDisjunction( ParseNode root, List<LexerToken> src, String input) {
		ParseNode first = null;
		boolean legitimateOper = false;
		//ParseNode second = null;
		for( ParseNode child : root.children() ) {
			if( first == null ) {
				first = child;
				continue;
			}
			
			if( !legitimateOper ) {
				if( child.contains(disj) )
					legitimateOper = true;
				else
					return null;
				continue;
			}
			
			Predicate lft = predicate(first, src, input);
			Predicate rgt = predicate(child, src, input);
			return new CompositeExpr(lft, rgt, Oper.DISJUNCTION);
		}
		return null;
	}

	   private Predicate isDifference( ParseNode root, List<LexerToken> src, String input) {
	        ParseNode first = null;
	        boolean isLegit = false;
	        //ParseNode second = null;
	        for( ParseNode child : root.children() ) {
	            if( first == null ) {
	                first = child;
	                continue;
	            }
	            
	            if( !isLegit ) {
	                if( child.contains(Program.minus) )
	                    isLegit = true;
	                else
	                    return null;
	                continue;
	            }
	            
	            Predicate lft = predicate(first, src, input);
	            Predicate rgt = predicate(child, src, input);
	            return new CompositeExpr(lft, rgt, Oper.DIFFERENCE);
	        }
	        return null;
	    }
	   
   /**
     * Processes arbori syntax: ( predicate )
     */
	private Predicate isParenthesis( ParseNode root, List<LexerToken> src, String input) {
		boolean isOpenParen = false;
		for( ParseNode child : root.children() ) {
			if( !isOpenParen ) {
				if( child.contains(openPar ) ) 
					isOpenParen = true;
				else
					return null;
				continue;
			}
			
			if( isOpenParen && child.contains(predicate) ) {
				return predicate(child, src, input);
			} else
				return null;
		}
		return null;
	}

	   /**
     * Processes arbori syntax: ( predicate )
     */
    private Predicate isBrackets( ParseNode root, List<LexerToken> src, String input) {
        boolean isOpenBr = false;
        for( ParseNode child : root.children() ) {
            if( !isOpenBr ) {
                if( child.contains(openBr ) ) 
                    isOpenBr = true;
                else
                    return null;
                continue;
            }
            
            if( isOpenBr ) {
                if( child.contains(header) )
                    return header(child, src, input);
                else if( child.contains(closeBr) )
                    return new MaterializedPredicate(new ArrayList<String>(),src,"[]");
            } else
                return null;
        }
        return null;
    }

    private MaterializedPredicate header( ParseNode root, List<LexerToken> src, String input) {
        if( root.contains(identifier) ) {
            ArrayList<String> hdr = new ArrayList<String>();
            hdr.add(root.content(src));
            return new MaterializedPredicate(hdr,src,"["+root.content(src)+"]");
        }
        MaterializedPredicate ret = null;
        for( ParseNode child : root.children() ) {
            if( ret == null )
                ret = header(child, src, input);
            else
                ret = MaterializedPredicate.join(ret, header(child, src, input));
        }
        return ret;
    }
	
	private Predicate isAtomicPredicate( ParseNode root, List<LexerToken> src, String input ) {
		Predicate ret = isExclamation(root, src, input);
		if( ret != null )
			return ret;
		ret = isNodeContent(root, src, input);
		if( ret != null )
			return ret;
		ret = isNodeMatchingSrc(root, src, input);
		if( ret != null )
			return ret;		
		/*ret = isNotCoveredByVectorNodes(root, src, input);
		if( ret != null )
			return ret;*/
		// 
		ret = isSameNode(root, src, input);
		if( ret != null )
			return ret;
		ret = isNodeAncestorDescendant(root, src, input);
		if( ret != null )
			return ret;
        ret = isAggregate(root, src, input);
        if( ret != null )
            return ret;
		//
		ret = isPositionalRelation(root, src, input);
		if( ret != null )
			return ret;
		return null;
	}

    private Predicate isAggregate(ParseNode root, List<LexerToken> src, String input) {
        Boolean slash1 = null;
        Boolean slash2 = null;
        ParseNode attribute = null;
        boolean seenOpenParen = false;
        ParseNode p = null;
        for( ParseNode child : root.children() ) {
            if( slash1 == null ) {
                if( child.contains(slash) ) 
                    slash1 = true;
                else if( child.contains(backslash) ) 
                    slash1 = false;
                else
                    return null;
                continue;
            }
            if( slash2 == null ) {
                if( child.contains(slash) ) 
                    slash2 = true;
                else if( child.contains(backslash) ) 
                    slash2 = false;
                else
                    return null;
                continue;
            }
            if( attribute == null ) {
                attribute = child;
                continue;
            }
            if( !seenOpenParen ) {
                if( child.contains(openPar) ) {
                    seenOpenParen = true;
                    continue;
                } else
                    throw new AssertionError("Syntax error not caught by parsing?");
            }
            p = child;
            break;
        }
        Predicate predicate = predicate(p, src, input);
        return new AggregatePredicate(attribute.content(src),predicate,slash1,slash2);
    }

    /**
     * Processes arbori syntax: ! predicate
     */
	private  Predicate isExclamation( ParseNode root,List<LexerToken> src, String input )  {
		boolean isExcl = false;
		for( ParseNode child : root.children() ) {
			if( !isExcl ) {
				if( child.contains(excl ) ) 
					isExcl = true;
				else
					return null;
				continue;
			}
			
			if( isExcl ) {
				return new CompositeExpr(predicate(child, src, input),null, Oper.NEGATION);
			} 
		}
		return null;
	}
	
    /**
     * Processes arbori syntax: [ node ) content
     */
	private Predicate isNodeContent( ParseNode root, List<LexerToken> src, String input ) {
		boolean openBrace = false;
		String first = null;
		boolean legitimateOper = false;
		String second = null;
		for( ParseNode child : root.children() ) {
			if( !openBrace ) {
				if( child.contains(openBr) )
					openBrace = true;
				else
					return null;
				continue;
			}
			
			if( first == null && child.contains(node) ) {
				if( child.from+1==child.to 
				 || child.contains(node_parent) 
				 || child.contains(node_predecessor) 
				 || child.contains(node_successor) 
				)
					first = child.content(src);
				else
					return null;
				continue;
			}
			
			if( !legitimateOper ) {
				if( child.contains(closePar) )
					legitimateOper = true;
				else
					return null;
				continue;
			}
			
			if( second == null  ) {
				second = child.content(src);
				continue;
			}
				
			throw new AssertionError("unexpected case");
		}
		Integer symbol = parser.symbolIndexes.get(second);
		if( symbol == null )
			throw new AssertionError("Symbol '"+second+"' not found");
		return new NodeContent(first, symbol);
	}

    /**
     * Processes arbori syntax: ?node = ?node
     */
	private Predicate isNodeMatchingSrc( ParseNode root, List<LexerToken> src, String input ) {
		boolean legitimateAt1 = false;
		String first = null;
		boolean legitimateOper = false;
		boolean legitimateAt2 = false;
		String second = null;
		for( ParseNode child : root.children() ) {
			if( !legitimateAt1 ) {
				if( child.contains(srcPtr) )
					legitimateAt1 = true;
				else
					return null;
				continue;
			}
			
			if( first == null ) {
				if( /*child.from+1==child.to &&*/ child.contains(node) )
					first = child.content(src);
				else
					return null;
				continue;
			}

			if( !legitimateOper ) {
				if( child.contains(eq) )
					legitimateOper = true;
				else
					return null;
				continue;
			}

			if( !legitimateAt2 ) {
				if( child.contains(srcPtr) )
					legitimateAt2 = true;
				else if( child.contains(string_literal) ) {
					return new NodeMatchingSrc(first, child.content(src));
				} else
					return null;
				continue;
			}
			
			if( second == null  ) {
				if( /*child.from+1==child.to ||*/ child.contains(node) )
					second = child.content(src);
				else
					return null;
				continue;
			}

			throw new AssertionError("unexpected case");
		}
		return new NodesWMatchingSrc(first, second);
	}
	

	private Predicate isNodeAncestorDescendant( ParseNode root, List<LexerToken> src, String input ) {
	    boolean isStrict = true;
		Pair<String,String> p = binaryPredicateNames(root, src, lt);
		if( p == null ) {
	        p = binaryPredicateNames(root, src, lt,eq);
	        if( p == null )
	            return null;
	        isStrict = false;
		}
		return new AncestorDescendantNodes(p.first(),p.second(),isStrict);
	}
	private Predicate isSameNode( ParseNode root, List<LexerToken> src, String input ) {
		Pair<String,String> p = binaryPredicateNames(root, src, eq);
		if( p == null )
			return null;
		return new SameNodes(p.first(),p.second());
	}

	public Pair<String,String> binaryPredicateNames( ParseNode root, List<LexerToken> src, final int oper) throws AssertionError {
		return binaryPredicateNames(root, src, oper, -1);
	}	
    public Pair<String,String> binaryPredicateNames( ParseNode root, List<LexerToken> src, final int oper1, final int oper2) throws AssertionError {
        String first = null;
        boolean legitimateOper1 = false;
        boolean legitimateOper2 = (-1 == oper2);
        String second = null;
        for( ParseNode child : root.children() ) {
            if( first == null ) {
                if( child.contains(node) ) 
                    first = child.content(src);                 
                else
                    return null;
                continue;
            }
            
            if( !legitimateOper1 ) {
                if( child.contains(oper1) )
                    legitimateOper1 = true;
                else
                    return null;
                continue;
            }
            if( !legitimateOper2 ) {
                if( child.contains(oper2) )
                    legitimateOper2 = true;
                else
                    return null;
                continue;
            }
            
            if( second == null ) {
                if( child.contains(node) ) 
                    second = child.content(src);                    
                else
                    return null;
                continue;
            }
                
            if( !child.contains(semicol) )
                throw new AssertionError("unexpected case for: "+root.content(src));
        }
        return new Pair<String,String>(first,second);
    }   
		
	private Predicate isPositionalRelation( ParseNode root, List<LexerToken> src, String input ) {
		Pair<String,PosType> first = null;
		boolean legitimateOper = false;
		boolean isReflexive = false;
		Pair<String,PosType> second = null;
		for( ParseNode child : root.children() ) {
			if( first == null ) {				
				first = nodeRelativePosition(child,src,input);
				if( first == null )
					return null;
				continue;
			}
			
			if( !legitimateOper ) {
				if( child.contains(lt) )
					legitimateOper = true;
				else
					return null;
				continue;
			}
            if( child.contains(eq) ) {
                isReflexive = true;           
                continue;
            }

			if( second == null  ) {
				second = nodeRelativePosition(child,src,input);
				if( second == null )
					return null;
				continue;
			}
			
			throw new AssertionError("unexpected case");
		}
		return new PositionalRelation(first.first(), first.second(), second.first(), second.second(), isReflexive, this);
	}
	private Pair<String,PosType> nodeRelativePosition( ParseNode root, List<LexerToken> src, String input ) {
		String name = null;
		PosType t = null;
		for( ParseNode child : root.children() ) {			
			if( name == null && child.contains(node) || t == PosType.BINDVAR ) {
				name = child.content(src);
				continue;
			} else if( t == null ) {
				if( child.contains(openBr) ){
					t = PosType.HEAD;
					continue;
				} else if( child.contains(closePar) ) {
					t = PosType.TAIL;				
					continue;
				} else if( child.contains(col) ) {
					t = PosType.BINDVAR;				
					continue;
				} else
					throw new AssertionError();
			}
		}
		if( name == null )
			throw new AssertionError("name == null");
		if( t == null )
			throw new AssertionError("t == null");
		return new Pair<String,PosType>(name,t);
	}

	/**
	 *  major runtime method 
	 */
	public Map<String,MaterializedPredicate> eval( Parsed target )  {
	    target.getRoot();
        long t1 = System.currentTimeMillis();
        
	    Map<String,MaterializedPredicate> ret = new HashMap<String,MaterializedPredicate>();
	    
	    boolean outputRelationsIsEmpty = outputRelations.size()==0; // not to break legacy
	    for( String predVar : execOrder ) {
	        long t11 = System.currentTimeMillis();
	        if( debug )
	            System.out.println(">=================================<     "+predVar);
		    MaterializedPredicate table = null;
            if( !outputRelationsIsEmpty && skipEmptyRelationEval(predVar) ) {
                final ArrayList<String> hdr = new ArrayList<String>();
                final Map<String,Attribute> varDefs = new HashMap<String,Attribute>();
                final ArrayList<String> independentVars = new ArrayList<String>(); 
                evalDimensions(predVar,varDefs,independentVars,hdr,target,true);
                table = new MaterializedPredicate(hdr, target.getSrc(), null);
            } else {
                table = new MaterializedPredicate(predVar,eval(target, predVar));
            }
		    //System.out.println(predVar+" eval time = "+(System.currentTimeMillis()-t11)); // (authorized) //$NON-NLS-1$

            table.name = predVar; 
            ret.put(predVar,table);
		    namedPredicates.put(predVar, table);
		    table.trimAttributes();
	        if( debug /*|| outputRelations.contains(predVar)*/ )
	            System.out.println(predVar+"="+table);         

		}
	    
	    namedPredicates = /*symbolicPredicates.clone()*/ new HashMap<String,Predicate>();
        for( String key : symbolicPredicates.keySet() )
            namedPredicates.put(key, symbolicPredicates.get(key));
        
        long t2 = System.currentTimeMillis();
        if( debug )
            System.out.println("eval time = "+(t2-t1)); // (authorized) //$NON-NLS-1$

		return ret;
	}
		
    // Map<String,MaterializedPredicate> materializedPredicates = new HashMap<String,MaterializedPredicate>();
	// Now Predicate mutate into MaterializedPredicate, so keep them in namedPredicates 
	private MaterializedPredicate eval( Parsed target, String predVar )  {
		final Predicate evaluatedPredicate = namedPredicates.get(predVar);
		MaterializedPredicate ret = evaluatedPredicate.eval(target);
		if( ret != null )
		    return ret;
				
        final Map<String,Attribute> varDefs = new HashMap<String,Attribute>();
        final ArrayList<String> independentVars = new ArrayList<String>(); // x,y,z,predicate
        final ArrayList<String> hdr = new ArrayList<String>();
        
        int[] limits = evalDimensions(predVar,varDefs,independentVars,hdr,target,false);
        if( limits[0] == 0 )
            return new MaterializedPredicate(hdr, target.getSrc(), null);
        boolean firstTime = true;
        if( debug ) {
        	System.out.print("Eval space = ");
        	for( int i = 0; i < limits.length; i++ ) {
				System.out.print((firstTime?"":"x")+limits[i]);
				firstTime = false;
			}
        	System.out.println();
        }
        firstTime = true;
        
        /////////////// Try evaluating better than through full dimensional cartesian product /////////////
        int firstDimension = 0;
        for( int i = 1; i < limits.length; i++ ) {
            if( limits[i] < limits[firstDimension] ) {
                firstDimension = i;
            }
        }
        Set<Integer> joined = new HashSet<Integer>();
        joined.add(firstDimension);
        String first = independentVars.get(firstDimension);
        ArrayList<String> attributes = new ArrayList<String>();
        attributes.add(first);
        Attribute firstAttr = varDefs.get(first); 
        ret = ((IndependentAttribute)firstAttr).getContent();
        while( joined.size() < limits.length ) {
            int current = minimalRelatedDimension(joined,limits, independentVars, varDefs, evaluatedPredicate);
            if( current == -1 ) // failed to find candidate for joining
                break; // 
            joined.add(current);
            Attribute second = varDefs.get(independentVars.get(current));
            
            MaterializedPredicate pred2 = ((IndependentAttribute)second).getContent();
            Predicate filter = new True();
            if( joined.size() != limits.length ) {
                for( String a : ret.attributes ) {
                    if( ret.name != null )
                        a = ret.name + "." + a;
                    for( String b : pred2.attributes ) {
                        if( pred2.name != null )
                            b = pred2.name + "." + b;
                        Predicate rel = evaluatedPredicate.isRelated(a, b, varDefs);
                        if( rel != null )
                            filter = new CompositeExpr(filter, rel, Oper.CONJUNCTION);
                    }
                }
                if( filter instanceof True )
                    throw new AssertionError("Cartesian product evaluation; check for missing binary predicates");
            } else
                filter = evaluatedPredicate;

            ret = MaterializedPredicate.filteredCartesianProduct(ret, pred2,filter,varDefs,target.getRoot());
            if( debug )
                System.out.println("dim#"+joined.size()+",cardinality="+ret.cardinality());
        }
        if( joined.size() == limits.length )
            return ret;
        
        throw new AssertionError("Missing dyadic predicate in predvar "+predVar+"; won't evaluate cartesian product");        
	}

	private int[] evalDimensions( String predVar, Map<String,Attribute> varDefs, ArrayList<String> independentVars, ArrayList<String> hdr, Parsed target, boolean emptyContent) {
        final Predicate evaluatedPredicate = namedPredicates.get(predVar);
	    { //limit scope of allVariables: they contain temporary entries e.g. x=expr
	        final Set<String> allVariables = new HashSet<String>();  // x,y,z,x^,x+1,predicate.attr, 
	        evaluatedPredicate.variables(allVariables, true);       

	        for( String s : allVariables ) 
	            extractDependentAttributes(s, namedPredicates, varDefs, independentVars);
	    } //end of scope limit

	    if( debug )
	        System.out.println("Independent variables = "+independentVars);

	    int[] limits = new int[independentVars.size()];
	    for( int i = 0; i < limits.length; i++ ) {
	        String var = independentVars.get(i);
	        if( !emptyContent ) {
	            IndependentAttribute varDef = (IndependentAttribute)varDefs.get(var);
	            if( limits.length == 1 )
	                varDef.putFilter(evaluatedPredicate);
	            varDef.initContent(target.getRoot(),target.getSrc(),varDefs,predVar);
	            limits[i] = varDef.getLimits();
	        } else 
	            limits[i] = 0;
	        if( limits[i] == 0 ) { // dimension of size 0
	            for( String candidateAttr : varDefs.keySet() )
	                if( !(varDefs.get(candidateAttr) instanceof MaterializedPredicate) )
	                    hdr.add(candidateAttr);
	            limits = new int[independentVars.size()];
	            break;
	        }
	    }
	    return limits;
	}
    
    private boolean skipEmptyRelationEval( String predVar ) {
        for( String target : outputRelations )
            if( dependency.isDependent(predVar, target) ) {
                Map<String, Boolean> backRefs = dependency.backward.get(target);
                boolean hasNullifier = false;
                for( String src : backRefs.keySet() ) {
                    if( Boolean.FALSE == backRefs.get(src) )
                        continue;
                    Predicate sp = namedPredicates.get(src);
                    if( !(sp instanceof MaterializedPredicate) )
                        continue;
                    if( ((MaterializedPredicate)sp).tuples.size()==0 ) {
                        hasNullifier = true;
                        break;
                    }
                }
                if( !hasNullifier ) {
                    if( debug )
                        System.out.println("No nullifier for "+target);
                    return false;
                }
            }
        return true;
    }

    private void extractDependentAttributes( String name, Map<String, Predicate> db, 
            Map<String, Attribute> varDefs, ArrayList<String> independentVars ) {
        //if( "sc.id".equals(name) )
            //name = "sc.id";
        
        if( varDefs.containsKey(name) )
            return;
                       
        String ref = Attribute.referredTo(name);
        if( ref != null ) {
            if( 0 < name.indexOf('=') ) {
                int pos = name.indexOf('=');
                String prefix = name.substring(0,pos);
                String postfix = name.substring(pos+1);
                if( null == Attribute.referredTo(prefix) ) {
                    EqualExpr ee = new EqualExpr(prefix,postfix);
                    varDefs.put(ee.name, ee);
                    independentVars.remove(ee.name);   
                    ref = postfix;
                }  else if( null == Attribute.referredTo(postfix) ) {
                    EqualExpr ee = new EqualExpr(postfix,prefix);
                    varDefs.put(ee.name, ee);
                    independentVars.remove(ee.name);   
                    ref = prefix;
                } else {
                    extractDependentAttributes(prefix, db, varDefs, independentVars);
                    extractDependentAttributes(postfix, db, varDefs, independentVars);
                    return;
                }
 
            } else if( name.endsWith("^") )
                varDefs.put(name, new Parent(name));
            else if( name.endsWith("-1") )
                varDefs.put(name, new Predecessor(name));
            else if( name.endsWith("+1") )
                varDefs.put(name, new Successor(name));
            else if( 0 < name.indexOf('.') ) {
                String pred = name.substring(0,name.indexOf('.'));
                MaterializedPredicate refPred = (MaterializedPredicate)namedPredicates.get(pred);
                String postfix = name.substring(pred.length()+1);
                //if( !postfix.equals(ref)  )
                    //throw new AssertionError("!postfix.equals(ref)"); //$NON-NLS-1$
                if( refPred.getAttribute(postfix) == null  )
                    throw new AssertionError("Undefined variable "+postfix+" in "+pred);
                varDefs.put(name, new Column(name));
                ref = pred;
            } else
                throw new AssertionError("unexpected case"); //$NON-NLS-1$
            extractDependentAttributes(ref, db, varDefs, independentVars);
        } else {
            independentVars.add(name);
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if( /*!"extractDependentAttributes".equals*/ "eval".equals(stackTrace[2].getMethodName()) ) { // not a reference to MaterializedPredicate
                varDefs.put(name,new IndependentAttribute(name,db)); 
                return;
            }
            
            Predicate pred = namedPredicates.get(name);
            if( pred == null ) {
                varDefs.put(name,new IndependentAttribute(name,db));               
            } else {
                if( !(pred instanceof MaterializedPredicate) )
                    throw new AssertionError(" !("+name+" instanceof MaterializedPredicate)"); //$NON-NLS-1$
                MaterializedPredicate defVectors = (MaterializedPredicate) pred;
                varDefs.put(name,defVectors/*new MaterializedPredicate(defVectors)*/);
                defVectors.name = name;
            }            
        }            
    }

	
	/**
	 * Ad-hock greedy optimization
	 * @param joined
	 * @param limits
	 * @param independentVars
	 * @param varDefs 
	 * @param p
	 * @return -1 if no suiatble attribute to join found (e.g. the predicate may have disjunction in it, etc)
	 */
    private static int minimalRelatedDimension( Set<Integer> joined, int[] limits, 
            ArrayList<String> independentVars, Map<String, Attribute> varDefs, Predicate p ) {
        int ret = -1;
        for( int i = 0; i < limits.length; i++ ) {
            if( joined.contains(i) || !isConnected(joined,i,independentVars,varDefs,p) )
                continue;
            if( ret == -1 || limits[i] < limits[ret]  )
                ret = i;
        }
        return ret;
    }
    private static boolean isConnected( Set<Integer> joined, int i, 
            ArrayList<String> independentVars, Map<String, Attribute> varDefs, Predicate p ) {
        String name1 = independentVars.get(i);
        for( int j : joined ) {
            String name2 = independentVars.get(j);
            if( isRelated(name1, name2, varDefs, p) )
                return true;
        }
        return false;
    }
    private static boolean isRelated( String s1, String s2, Map<String, Attribute> varDefs, Predicate p ) {
        for( String d1 : functions(s1,varDefs) )
            for( String d2 : functions(s2,varDefs) )
                if( p.isRelated(d1, d2, varDefs) != null )
                    return true;            
        return false;
    }
    private static List<String> functions( String s, Map<String, Attribute> varDefs ) {
        List<String> ret = new LinkedList<String>();
        for( String vd : varDefs.keySet() )
            if( vd.startsWith(s) )
                ret.add(vd);
        return ret;
    }


}
