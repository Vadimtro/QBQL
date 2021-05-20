package qbql.arbori;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import qbql.parser.Earley;
import qbql.parser.Grammar;
import qbql.parser.Lex;
import qbql.parser.LexerToken;
import qbql.parser.Matrix;
import qbql.parser.ParseNode;
import qbql.parser.Parsed;
import qbql.parser.RuleTuple;
import qbql.parser.Token;
import qbql.parser.Visual;
import qbql.parser.SyntaxError;
import qbql.util.Pair;
import qbql.util.Util;

/**
 * Arbori engine, see arbori.odt
 * 
 * Version 2.0
 * 
 * @author Dim
 *
 */
public class Program {
    
    public static boolean debug = false;
    public static boolean timing = false;
    
    
    protected Object struct = null; // a hook for callbacks, bind variables;
    public void setStruct( Object struct ) { this.struct = struct; }
    
    protected Earley parser;
    
    private int objRef;
    private static int objCnt = 0;    
    
	public Program( Earley parser ) {
		this(parser, "JS");
	}
	
	public Program( Earley parser, Object struct ) {
		this(parser, struct, null, null, true);
	}

	public Program( Earley parser, Object struct, ScriptEngine engine, GlobalMap globals, boolean allowJS ) {
		this.parser = parser;
		this.struct = struct;
		this.engine = engine;
		this.globals = globals;
		this.allowJS = allowJS;
        objRef = objCnt++;
	}

	public static void main( String[] args ) throws Exception {
	    ScriptEngineManager manager = new ScriptEngineManager();
	    List<ScriptEngineFactory> factories = manager.getEngineFactories();

	    for (ScriptEngineFactory factory : factories) {
	    	System.out.println("\nName : " + factory.getEngineName());
	    	System.out.println("Version : " + factory.getEngineVersion());
	    	System.out.println("Language name : " + factory.getLanguageName());
	    	System.out.println("Language version : " + factory.getLanguageVersion());
	    	System.out.println("Extensions : " + factory.getExtensions());
	    	System.out.println("Mime types : " + factory.getMimeTypes());
	    	System.out.println("Names : " + factory.getNames());
	    	ScriptEngine engine = manager.getEngineByName(factory.getNames().get(0));
	    	if (engine == null) {
	    		System.out.println("Impossible to find the engine with name " + factory.getEngineName()+"\n");
	    	}
	    }
		
    	final String arboriProgram = Util.readFile("test.arbori");
    	if( false ) { // test parsing
    		final Earley arboriParser = getArboriParser();
    		List<LexerToken> src = new Lex().parse(arboriProgram);
    		Matrix matrix = new Matrix(arboriParser);
    		matrix.visual = new Visual(src,arboriParser);
    		arboriParser.parse(src,matrix);
    		matrix.visual.draw(matrix);
    	    ParseNode root = arboriParser.forest(src, matrix);
    	    root.printTree();
    		return;
    	}
		
		
		long t1 = java.lang.System.currentTimeMillis();
        final String input = Util.readFile(Program.class, "test.sql");
        final List<LexerToken> src = new Lex().parse(input);
        
        /*Program r = new Program(arboriProgram) {
        };
        //r.debug = true;
        t1 = java.lang.System.currentTimeMillis();
        Map<String,MaterializedPredicate> output = r.run(                
               input, src , new HashMap<String,Object>() //"JS action"
        );*/
        //for( String p : output.keySet() )
            //System.out.println(p+"="+output.get(p).toString(p.length()+1));
        
		long t2 = java.lang.System.currentTimeMillis();
		System.out.println("Second time run="+(t2-t1));


	}

    private static int attribute = -1;
    private static int backslash = -1;
    private static int closePar = -1;
    private static int closeBr = -1;
    private static int col = -1;
    private static int conj = -1;
    private static int connect = -1;
    private static int digits = -1;
    private static int disj = -1;
    private static int dot = -1;
    private static int eq = -1;
    private static int excl = -1;
    private static int identifier = -1;
    private static int include = -1;
    private static int js_block = -1;
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
    private static int plus = -1;
    private static int predicate = -1;
    private static int program = -1;
    private static int referenced_node = -1;
    private static int rule = -1;
    private static int srcPtr = -1;
    private static int semicol = -1;
    private static int sharp = -1;
    private static int slash = -1;
    private static int statement = -1;
    private static int string_literal = -1;
	public static Earley getArboriParser() throws IOException  {
		Set<RuleTuple> rules = getRules();
		//RuleTransforms.eliminateEmptyProductions(rules);
        //RuleTuple.printRules(rules);
        Earley testParser = new Earley(rules) {
            @Override
            protected void initCell00(List<LexerToken> src, Matrix matrix) {
                long t1 = 0;
                if( matrix.visual != null )
                    t1 = System.nanoTime();
                matrix.initCells(src.size());
                initCell(matrix, new int[] {program},0);
                if( matrix.visual != null ) {
                    long t2 = System.nanoTime();
                    matrix.visual.visited[0][0] += (int)(t2-t1);
                }
                LexerToken LAtoken = src.get(0);
                matrix.LAsuspect = symbolIndexes.get("'" + LAtoken.content.toUpperCase() + "'");

            }
        };
        testParser.isCaseSensitive = true;
        //testParser.skipRanges = false;     // with optional trailing semicolons grammar is too ambitious
        attribute = testParser.symbolIndexes.get("attribute"); //$NON-NLS-1$
        backslash = testParser.symbolIndexes.get("'\\'"); //$NON-NLS-1$
        closePar = testParser.symbolIndexes.get("')'"); //$NON-NLS-1$
        closeBr = testParser.symbolIndexes.get("']'"); //$NON-NLS-1$
        col = testParser.symbolIndexes.get("':'"); //$NON-NLS-1$
        conj = testParser.symbolIndexes.get("'&'"); //$NON-NLS-1$
        connect = testParser.symbolIndexes.get("connect"); //$NON-NLS-1$
        digits = testParser.symbolIndexes.get("digits"); //$NON-NLS-1$
        disj = testParser.symbolIndexes.get("'|'"); //$NON-NLS-1$ 
        dot = testParser.symbolIndexes.get("'.'"); //$NON-NLS-1$
        eq = testParser.symbolIndexes.get("'='"); //$NON-NLS-1$
        excl = testParser.symbolIndexes.get("'!'"); //$NON-NLS-1$ 
        identifier = testParser.symbolIndexes.get("identifier"); //$NON-NLS-1$ 
        include = testParser.symbolIndexes.get("include"); //$NON-NLS-1$ 
        js_block = testParser.symbolIndexes.get("js_block"); //$NON-NLS-1$ 
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
        plus = testParser.symbolIndexes.get("'+'"); //$NON-NLS-1$
        predicate = testParser.symbolIndexes.get("predicate"); //$NON-NLS-1$
        program = testParser.symbolIndexes.get("program"); //$NON-NLS-1$
        referenced_node = testParser.symbolIndexes.get("referenced_node"); //$NON-NLS-1$
        rule = testParser.symbolIndexes.get("rule"); //$NON-NLS-1$
        semicol = testParser.symbolIndexes.get("';'"); //$NON-NLS-1$
        sharp = testParser.symbolIndexes.get("'#'"); //$NON-NLS-1$
        slash = testParser.symbolIndexes.get("'/'"); //$NON-NLS-1$
        srcPtr = testParser.symbolIndexes.get("'?'"); //$NON-NLS-1$
        statement = testParser.symbolIndexes.get("statement"); //$NON-NLS-1$
        string_literal = testParser.symbolIndexes.get("string_literal"); //$NON-NLS-1$
		prioritizeRules(testParser);
		testParser.isAsc = true;
		return testParser;
    }
    
    private static void prioritizeRules( Earley testParser ) {
		String rule2 = "program:  statement;";
		String rule3 = "program:  program  statement;";
		testParser.swapRules(rule2, rule3);	
	}
    
	private static Set<RuleTuple> getRules() throws IOException  {
        String input = Util.readFile(Program.class, "arbori.grammar"); //$NON-NLS-1$
        List<LexerToken> src = new Lex().parse(input); 
        ParseNode root = Grammar.parseGrammarFile(src, input);
        Set<RuleTuple> ret = new TreeSet<RuleTuple>();
        Grammar.grammar(root, src, ret);
        return ret;
    }

	private LinkedList<String> execOrder = new LinkedList<String>();
	/**
	 * @return List of predicates/queries in the order they are written in arbori program
	 *         Useful in situation when there is no callback marker identifying the latest query
	 */
	public LinkedList<String> querySequence() {
		return execOrder;
	}
	
	public Map<String, Predicate> namedPredicates = new HashMap<String, Predicate>();
	// namedPredicates is mutating, so restore it for subsequent executions from symbolicPredicates
    //Map<String,Predicate> symbolicPredicates = new HashMap<String,Predicate>();
	
    private PredicateDependency dependency = new PredicateDependency();
    
    private Map<String,String> outputActions = new HashMap<String,String>();  //relation -> javascript code, or "java callback"
    
    protected void copyState( Program source ) {
        //execOrder.clear();
        //execOrder.addAll(source.execOrder);
        execOrder = source.execOrder;
        namedPredicates.clear();
        //namedPredicates.putAll(source.namedPredicates);
        for( String key : source.namedPredicates.keySet() ) {
            Predicate value = source.namedPredicates.get(key);
            Predicate clone = value.copy(this);
            namedPredicates.put(key,clone);
        }
        /*symbolicPredicates.clear();
        //symbolicPredicates.putAll(source.symbolicPredicates);
        for( String key : source.symbolicPredicates.keySet() ) {
            Predicate value = source.symbolicPredicates.get(key);
            Predicate clone = value.copy(this);
            symbolicPredicates.put(key,clone);
        }*/
        dependency = source.dependency;
        outputActions = source.outputActions;
        parser = source.parser;
        bindsInstance = source.bindsInstance;
        included = source.included;
        //if( struct == null )
        	//struct = source.struct;  <-- most recent callback object must be provided explicitly
        engine = source.getEngine();
        globals = source.getGlobals(); 
        allowJS = source.allowJS;
        targetFileName= source.targetFileName;
    }
    
    private Program compiledInstance = null;
    static private HashMap<String,Program> compiledPrograms = new HashMap<String,Program>();
    private HashMap<String,Boolean> bindsInstance = null; // recompile the program every time bind changes
    /**
     * Compiles Arbori program.
     * Does it only once (for the same programText).
     * @param programText
     * @throws IOException
     */
    public void compile( String programText ) throws IOException {
    	compile(programText, null);
    }
    public void compile( String programText, Object struct ) throws IOException {
    	compile(programText, struct, false);
    }
    public void compile( String programText, Object struct, boolean force ) throws IOException {   	
    	included = new LinkedList<Program>();
        compiledInstance = compiledPrograms.get(programText);
        // recompile if boolean bind values changed
        if( !force && compiledInstance != null && compiledInstance.bindsInstance != null ) {
        	for( String bindVar : compiledInstance.bindsInstance.keySet() ) {
				Boolean bindValue = compiledInstance.bindsInstance.get(bindVar);
				if( !bindValue.equals(getBoolBindVar(bindVar)) ) {
					compiledInstance = null;
					break;
				}
			}
        } else {
        	compiledInstance = null;
        }        	
        if( compiledInstance == null ) {
            Parsed prg = new Parsed(
                         programText, //$NON-NLS-1$
                         getArboriParser(),
                         "program" //$NON-NLS-1$
                );
            //prg.debug = true;
            final SyntaxError error = prg.getSyntaxError();
            if( error != null )
            	throw error;
			program(prg.getRoot(), prg.getSrc(), prg.getInput());
            compiledInstance = new Program(parser, struct);
            compiledInstance.copyState(this);
            compiledInstance.bindsInstance = new HashMap<String,Boolean>();
            // override binds cache
			Set<String> bindVars = listBindVariables(prg.getRoot(), prg.getSrc());
			for( String bindVar : bindVars ) 
				compiledInstance.bindsInstance.put(bindVar, getBoolBindVar(bindVar));
			
			if( !force )
				compiledPrograms.put(programText,compiledInstance);
        } else 
            copyState(compiledInstance);
    }

	private void program( ParseNode root, List<LexerToken> src, String input ) {
    	//root.printTree();
    	if( root.contains(statement) ) {
    		statement(root, src, input);
    		return;
    	}
		for( ParseNode child : root.children() )
			program(child, src, input);		
	}

    private void statement( ParseNode root, List<LexerToken> src, String input ) {
        if( root.contains(rule) )
            rule(root, src, input);
        else if( root.contains(include) )
            include(root, src, input);
        else if( root.contains(connect) )
            connect(root, src, input);
        else if( root.contains(js_block) )
            jsBlock(root, src, input);
    }
    
    public List<Program> included = new LinkedList<Program>();
	private void include( ParseNode root, List<LexerToken> src, String input ) {
		for( ParseNode child : root.children() ) {
			if( child.contains("'include'") )
				continue;
			String filename = child.content(src);
			if( filename.startsWith("\"") )
				filename = filename.substring(1,filename.length()-1);
			String includeContent = getContent(filename);
			try {
				Program incPrg = new Program(parser, struct, getEngine(), getGlobals(), allowJS);
				incPrg.compile(includeContent,struct,true);
				included.add(incPrg);				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private String getContent( String filename ) throws AssertionError {
		String content = null;
		try {
			String sysSource = filename;
			String downOneLevel = "../";
			if( sysSource.startsWith(downOneLevel) )
				sysSource = "/qbql/"+sysSource.substring(downOneLevel.length());
			content = Util.readFile(Program.class, sysSource);
		} catch( IOException e ) {
			try {
				Properties pr = System.getProperties();
				String m = (String) pr.get("arbori.home");
				if( m != null && (m.endsWith("/") || m.endsWith("\\")) )
					m = m.substring(0,m.length()-1);
				if( m == null )
					content = Util.readFile(filename);
				else
					content = Util.readFile(m.toString()+"/"+filename);
			} catch( IOException e1 ) {
				try {
					content = Util.readFile(filename);
				} catch( IOException e2 ) {
					throw new AssertionError(filename+" not found");
				}
			}
		}
		return content;
	}
	
	private String targetFileName = null;
	private void connect( ParseNode root, List<LexerToken> src, String input ) {
		for( ParseNode child : root.children() ) {
			if( child.contains("'connect'") )
				continue;
			targetFileName = child.content(src);
			if( targetFileName.startsWith("\"") )
				targetFileName = targetFileName.substring(1,targetFileName.length()-1);
		}
	}
	
	static final String anonymousJsBlockNamePrefix = "anonymous JS Block #";
	int blockNo = 0;
	private void jsBlock( ParseNode root, List<LexerToken> src, String input ) {
		String tmpName = anonymousJsBlockNamePrefix + blockNo++;
        execOrder.add(tmpName);
        namedPredicates.put(tmpName, new True());
    	outputActions.put(tmpName, input.substring(src.get(root.from).begin, src.get(root.to-1).end) );
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
    	String name = null;

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
                if( namedPredicates.containsKey(first) )
                	throw new AssertionError("Duplicate defintion of predicate "+first);
	            execOrder.add(first);
	            namedPredicates.put(first, p);

	    		//return;
	            continue;
	    	}
	    	
	        if( child.contains(minus) ) { // trailing "->"
	        	name = first;
	            // do not return; might encounter JS code
	        } 
	        if( child.contains("javascript") ) { 
	        	outputActions.put(name, input.substring(src.get(child.from).begin, src.get(child.to-1).end) );
	            return;
	        } 
	        if( name != null )
	        	outputActions.put(name, javaCallbackCode);

		}
	}	
	
	static private String javaCallbackCode = "this is java callback (not JS)";

    
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

    private Header header( ParseNode root, List<LexerToken> src, String input) {
        if( root.contains(attribute) ) {
            ArrayList<String> hdr = new ArrayList<String>();
            hdr.add(root.content(src));
            return new Header(hdr);
        }
        Header ret = null;
        for( ParseNode child : root.children() ) {
            if( ret == null )
                ret = header(child, src, input);
            else {
            	Header tmp = header(child, src, input);
            	ret.attributes.addAll(tmp.attributes);
            }
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
		ret = isBoolBindVar(root, src, input);
		if( ret != null )
			return ret;
		ret = isJSFunc(root, src, input);
		if( ret != null )
			return ret;
		ret = isChildNumRelation(root, src, input);
		if( ret != null )
			return ret;
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
                 || child.contains(referenced_node) 
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
		if( symbol == null ) {
			//throw new AssertionError("Symbol '"+second+"' not found");
			System.err.println("Symbol '"+second+"' not found");
			return new False();
		}
		return new NodeContent(first, symbol);
	}

	public Boolean addWsDividers = null;  // customize it for your Program instance
	
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
					return new NodeMatchingSrc(first, child.content(src), addWsDividers);
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
		AncestorDescendantNodes.Type type = AncestorDescendantNodes.Type.CLOSEST;
		Pair<String,String> p = binaryPredicateNames(root, src, lt);
		if( p == null ) {
	        p = binaryPredicateNames(root, src, lt,eq);
	        type = AncestorDescendantNodes.Type.TRANSITIVE;
			if( p == null ) {
		        p = binaryPredicateNames(root, src, lt,lt);
		        type = AncestorDescendantNodes.Type.TRANSITIVE;
		        if( p == null )
		            return null; //investigate other relationships, do not throw new AssertionError("Unrecognized ancestor-descendant relationship");
			}
		}
		return new AncestorDescendantNodes(p.first(),p.second(),type);
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
                if( child.contains(node) ) {
                    first = child.content(src);                 
                    if( namedPredicates.containsKey(first) )
                        throw new AssertionError("Error: "+first+" is a predicate, not predicate attribute within binary operation");
                } else
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
                if( child.contains(node) ) {
                    second = child.content(src);                    
                    if( namedPredicates.containsKey(second) )
                        throw new AssertionError("Error: "+second+" is a predicate, not predicate attribute within binary operation");
                } else
                    return null;
                continue;
            }                
            
            if( !child.contains(semicol) )
                throw new AssertionError("unexpected case for: "+root.content(src));
        }
        return new Pair<String,String>(first,second);
    }   
		
	private Predicate isPositionalRelation( ParseNode root, List<LexerToken> src, String input ) {
	    //LexerToken.print(src, root.from, root.to);
	    Position first = null;
        boolean isGT = false;
		boolean isReflexive = false;
		Position second = null;
		for( ParseNode child : root.children() ) {
			if( first == null ) {				
				first = nodeRelativePosition(child,src,input);
				if( first == null )
					return null;
				continue;
			}
			
            if( child.contains(lt) ){
                isGT = true;           
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
		return new PositionalRelation(first, second, isReflexive, isGT, this);
	}
	private Position nodeRelativePosition( ParseNode root, List<LexerToken> src, String input ) {
		String name = null;
		Position t = null;
		int num = -1;
		for( ParseNode child : root.children() ) {			
			if( name == null && child.contains(attribute) || t instanceof BindVar ) {
				name = child.content(src);
				continue;
			} else if( child.contains(digits) ) {
                num = Integer.decode(child.content(src));
                if( t != null )
                    ((Composite)t).addendum = num;
                continue;
            } else if( t == null ) {
				if( child.contains(openBr) ){
					t = new Head(name);
					continue;
				} else if( child.contains(closePar) ) {
					t = new Tail(name);				
					continue;
				} else if( child.contains(col) ) {
					t = new BindVar(name);				
					continue;
				}  else if( child.contains(plus) ) {
                    continue;
                } else if( child.contains(node_position) ) {
                    Position tmp = nodeRelativePosition(child, src, input);
                    name = tmp.name; 
                    t = new Composite(tmp,num);
                    continue;
				} else
					throw new AssertionError();
			}
		}
		if( name == null )
			throw new AssertionError("name == null");
		if( t == null )
			throw new AssertionError("t == null");
		t.setName(name);
		return t;
	}

	private Predicate isChildNumRelation( ParseNode root, List<LexerToken> src, String input ) {
	    //LexerToken.print(src, root.from, root.to);
	    Integer first = null;
        boolean isGT = false;
		boolean isReflexive = false;
		String second = null;
		for( ParseNode child : root.children() ) {
			if( first == null ) {
				try {
					first = Integer.parseInt(src.get(child.from).content);
				} catch( Exception e ) {
					return null;
				}
				if( first == null )
					return null;
				continue;
			}
			
            if( child.contains(lt) ){
                isGT = true;           
                continue;
            }
            if( child.contains(eq) ) {
                isReflexive = true;           
                continue;
            }
            if( child.contains(sharp) ) {
                 continue;
            }

			if( second == null  ) {
				second = src.get(child.from).content;
				if( second == null )
					return null;
				continue;
			}
			
			throw new AssertionError("unexpected case");
		}
		return new ChildNumRelation(first, second, isReflexive, isGT, this);
	}
	
	public Boolean getBoolBindVar( String name ) {
		try {
			Field field = struct.getClass().getField(name);
			if( field.getType() == boolean.class )
				return field.getBoolean(struct);
		} catch( NoSuchFieldException e ) {
		} catch (IllegalArgumentException | IllegalAccessException e) {
			throw new AssertionError("field.getBoolean failed due to "+e.getMessage());
		}					
		
		try {
			Method method = struct.getClass().getMethod(name);
			if( method.getReturnType() == boolean.class )
				try {
					return (boolean)method.invoke(struct);
				} catch( IllegalAccessException | IllegalArgumentException | InvocationTargetException e ) {
					throw new AssertionError("method.invoke failed due to "+e.getMessage());
				}			
		} catch (NoSuchMethodException | SecurityException e1) {
		}

		throw new AssertionError("_getBoolBindVar("+name+") failed");
	}
	
    private Predicate isBoolBindVar( ParseNode root, List<LexerToken> src, String input ) {
		if( !root.contains("bind_var") )
			return null;
		String name = src.get(root.from+1).content;
		Boolean b = getBoolBindVar(name);
        if( b == null )
        	throw new AssertionError("Bind var '"+name+"' not found.");
        if( b )
        	return new True();
        else
        	return new False();
	}
    private Set<String> listBindVariables( ParseNode root, List<LexerToken> src ) {
    	Set<String> ret = new HashSet<String>();
    	if( root.contains("bind_var") 
    	 && root.contains("atomic_predicate") // boolean bind var
    	) {
    		ret.add(src.get(root.from+1).content);
    		return ret;
    	}
    	for( ParseNode child : root.children() )
    		ret.addAll(listBindVariables(child,src));
    	return ret;
    }

    private Predicate isJSFunc( ParseNode root, List<LexerToken> src, String input ) {
		if( !root.contains("js_condition") )
			return null;
		String name = src.get(root.from+1).content;
		LinkedList<String> args = new LinkedList<String>();
		for (int i = root.from+2; i < root.to; i++) {
			LexerToken t = src.get(i);
			if( t.type == Token.QUOTED_STRING )
				args.add(t.content.substring(1,t.content.length()-1));
		}
		return new JSFunc(name, this, args);
	}

	/*public Map<String,MaterializedPredicate> eval()  {
		if( targetFileName == null )
			throw new AssertionError("Not connected to parsed source file");
		String input = getContent(targetFileName);
		Parsed target = new Parsed(input,SqlEarley.partialRecognizer(),"sql_statement" );
		return eval(target);
	}*/
	
	/**
	 * @param target
	 * @return
	 */
	public Map<String,MaterializedPredicate> eval( Parsed target )  {
		return eval(target, struct);
	}
	/** Legacy API
	 * @param target
	 * @param action -- callback object
	 * @return
	 */
	public Map<String,MaterializedPredicate> eval( Parsed target, Object action )  {
		// Bug 30537651 struct is not copied with program state
		//if( struct != null && action != struct )
		//	throw new AssertionError("struct != null && action != struct");
		struct = action;
		
	    Map<String,MaterializedPredicate> ret = new HashMap<String,MaterializedPredicate>();
	    
		for( Program inc : included ) {
			Map<String, MaterializedPredicate> includedPredicates = inc.eval(target, action);
			ret.putAll(includedPredicates);
			namedPredicates.putAll(includedPredicates);
		}

		
	    final ParseNode root = target.getRoot();
	    if( timing ) {
	        System.out.println("tree depth ="+root.treeDepth());
	        System.out.println("#tokens ="+target.getSrc().size());
	    }
        long t1 = System.currentTimeMillis();
        
	    
	    for( String predVar : execOrder ) {
	        long t11 = System.currentTimeMillis();
	        if( debug )
	            System.out.println(">=================================<     "+predVar);
		    MaterializedPredicate unnamed = _eval(target, predVar);
		    //tmp.setContent(tmp);
			MaterializedPredicate table = new MaterializedPredicate(predVar,unnamed);
            if( timing ) {
                System.out.print(predVar+" eval time = "+(System.currentTimeMillis()-t11)); // (authorized) //$NON-NLS-1$
                System.out.println("       (cardinality="+table.cardinality()+")");         // (authorized) //$NON-NLS-1$
            }

            table.name = predVar; 
            ret.put(predVar,table);
		    namedPredicates.put(predVar, table);
		    table.trimAttributes();
	        if( debug /*|| outputRelations.contains(predVar)*/ )
	            System.out.println(predVar+"="+table);         
	        
	        if( action != null ) { // callback Java class or JS idicator (some string)
	        	String oa = outputActions.get(predVar);
	        	if( oa == null   )
	        		continue;
	        	long t2 = System.currentTimeMillis();
	        	if( javaCallbackCode.equals(oa) )
	        		javaCallback(target, action, ret, predVar);
	        	else 
	        		jsCallback(target, ret, predVar, oa);                
	        	if( debug || timing )
	        		System.out.println("callback time = "+(System.currentTimeMillis()-t2)); // (authorized) //$NON-NLS-1$
	        }	        

		}
	    
	    namedPredicates = /*symbolicPredicates.clone()*/ new HashMap<String,Predicate>();
        for( String key : compiledInstance.namedPredicates.keySet() )
            namedPredicates.put(key, compiledInstance.namedPredicates.get(key).copy(this) );
        
        long t2 = System.currentTimeMillis();
        if( debug || timing )
            System.out.println("eval time = "+(t2-t1)); // (authorized) //$NON-NLS-1$
               


		return ret;
	}

	public boolean allowJS = true;
	ScriptEngine engine = null;
	public ScriptEngine getEngine() {
		if( engine == null && allowJS ) {
	        System.setProperty("polyglot.js.nashorn-compat", "true");

	        ScriptEngineManager mgr = new ScriptEngineManager();
	        
	        engine = mgr.getEngineByExtension("js");
		}
		return engine;
	}
	GlobalMap globals = null;
	public GlobalMap getGlobals() {
		if( globals == null ) {
			ScriptEngine tmp = getEngine();
			if( tmp == null )
				return null;
			globals = new GlobalMap(tmp.createBindings());
			globals.put("polyglot.js.allowHostAccess", true);
			globals.put("polyglot.js.allowNativeAccess", true);
			globals.put("polyglot.js.allowCreateThread", true);
			globals.put("polyglot.js.allowIO", true);
			globals.put("polyglot.js.allowHostClassLoading",true);
			globals.put("polyglot.js.allowHostClassLookup", (java.util.function.Predicate<String>) s -> true);
			globals.put("polyglot.js.allowAllAccess", true);
			tmp.setBindings(globals, ScriptContext.ENGINE_SCOPE);
		}
		return globals;
	}
	private void jsCallback( Parsed target, Map<String, MaterializedPredicate> ret, String predVar, String oa ) {
		if( getEngine() == null )
			return;
   		GlobalMap _globals = getGlobals();
		if( _globals == null )
			return;
		MaterializedPredicate mp = ret.get(predVar); 
		for( Tuple t : mp.tuples ) {
		    Map<String,ParseNode> tuple = new HashMap<String,ParseNode>();
		    for ( int j = 0; j < mp.arity(); j++ ) {
		        String colName = mp.getAttribute(j);
		        ParseNode node = mp.getAttribute(t, colName); 
		        tuple.put(colName, node);
		    }
		   	try {
				_globals.put("target", target);
		   		_globals.put("tuple", tuple);
		   		_globals.put("struct", struct);
		   		_globals.put("program", this);
		   		engine.eval(oa, _globals);
		   		//getEngine().setBindings(_globals, ScriptContext.ENGINE_SCOPE);
		   		//getEngine().eval(oa, _globals);
			} catch( Exception e ) {
				System.out.println("predVar="+predVar);
				e.printStackTrace();
			} 
		}
	}
	
	private void javaCallback( Parsed target, Object action, Map<String, MaterializedPredicate> ret, String predVar ) {
		if( debug )
		    System.out.println("-------->>>   "+predVar);
		Class c = action.getClass();
		try {
			Method callback = null;
			try {
				callback = c.getDeclaredMethod(predVar, Parsed.class, Map.class);
			} catch ( NoSuchMethodException | SecurityException  | IllegalArgumentException e ) {
				callback = c.getMethod(predVar, Parsed.class, Map.class);
		    }
		    callback.setAccessible(true);
		    MaterializedPredicate mp = ret.get(predVar); 
		    for( Tuple t : mp.tuples ) {
		        Map<String,ParseNode> tuple = new HashMap<String,ParseNode>();
		        for ( int j = 0; j < mp.arity(); j++ ) {
		            String colName = mp.getAttribute(j);
		            ParseNode node = mp.getAttribute(t, colName); 
		            tuple.put(colName, node);
		        }
		        callback.invoke(action, target, tuple);
		    }
		} catch ( NoSuchMethodException | SecurityException | IllegalAccessException 
		        | IllegalArgumentException | InvocationTargetException e ) {
		    System.err.println(predVar +" callback: "+ e.getMessage());
		    e.printStackTrace();
		}
	}
	
	private static boolean isEvaluatedOnTupleLevel( Predicate predicate ) {
		if( predicate instanceof IdentedPredicate )
			return true;
		if( predicate instanceof CompositeExpr ) {
			CompositeExpr ce = (CompositeExpr) predicate;
			boolean ret = isEvaluatedOnTupleLevel(ce.lft);
			if( ret )
				return ret;
			if( ce.rgt != null )
				return isEvaluatedOnTupleLevel(ce.rgt);
		}
		return false;
	}
		
    // Map<String,MaterializedPredicate> materializedPredicates = new HashMap<String,MaterializedPredicate>();
	// Now Predicate mutate into MaterializedPredicate, so keep them in namedPredicates 
	private MaterializedPredicate _eval( Parsed target, String predVar )  {
		final Predicate evaluatedPredicate = namedPredicates.get(predVar);
		if( !isEvaluatedOnTupleLevel(evaluatedPredicate) )
			return evaluatedPredicate.eval(target);
				
        AttributeDefinitions varDefs = evalDimensions(predVar, target, debug);
        
        /////////////// Try evaluating better than through full dimensional cartesian product /////////////
        String firstDimension = null;
        for( String dim : varDefs.listDimensions() ) {
            if( firstDimension == null || varDefs.getDimensionContent(dim).cardinality() < varDefs.getDimensionContent(firstDimension).cardinality() ) {
                firstDimension = dim;
            }
        }
        Set<String> joined = new HashSet<String>();
        joined.add(firstDimension);
        Attribute firstAttr = varDefs.get(firstDimension); 
        MaterializedPredicate ret = ((IndependentAttribute)firstAttr).getContent();
        while( joined.size() < varDefs.listDimensions().size() ) {
            String current = varDefs.minimalRelatedDimension(joined, ret.cardinality());
            if( current == null ) // failed to find candidate for joining
                throw new AssertionError("Cartesian product evaluation: failed to find attribute joined to "+joined+
                		".\n Independent Attributes: "+varDefs.listDimensions());
            joined.add(current);
            Attribute second = varDefs.get(current);
            
            MaterializedPredicate pred2 = ((IndependentAttribute)second).getContent();
            Predicate filter = new True();
            if( joined.size() != varDefs.listDimensions().size() ) {
                for( String a : ret.attributes ) {
                    if( ret.name != null )
                        a = ret.name + "." + a;
                    for( String b : pred2.attributes ) {
                        if( pred2.name != null )
                            b = pred2.name + "." + b;
                        Predicate rel = evaluatedPredicate.isRelated(a, b, varDefs);
                        if( rel != null ) {
                            if( filter instanceof True )
                                filter = rel;
                            else {
                                if( filter != rel )
                                    filter = new CompositeExpr(filter, rel, Oper.CONJUNCTION);
                            }
                        }
                    }
                }
                if( filter instanceof True ) {
                	if( 1 < varDefs.getDimensionContent(current).cardinality()  
                	 &&	1 < ret.cardinality()	
                			)
                		throw new AssertionError("Cartesian product evaluation; check for missing binary predicates");
                }
            } else
                filter = evaluatedPredicate;

            ret = MaterializedPredicate.filteredCartesianProduct(ret, pred2,filter,varDefs,target.getRoot());
            if( debug )
                System.out.println("dim#"+joined.size()+",cardinality="+ret.cardinality());
        }
        /*if( varDefs.listDimensions().size() == 1 ) filteredCartesianProduct does not disambiguate
              // e.g.   "pc+fml":  pc.id+1=fml_part & [fml_part) fml_part ;
              // Here independent var "pc" is defined somewhere before this rule 
              // Need one more attribute "fml_part" to add to the result and couple predicates to evaluate
              // This happens because MaterializedPredicate.filteredCartesianProduct() is called only when joining
             */
            ret = MaterializedPredicate.filter(ret, evaluatedPredicate,varDefs,target.getRoot());        
        if( joined.size() == varDefs.listDimensions().size() )
            return ret;
        
        throw new AssertionError("Missing dyadic predicate in predvar "+predVar+"; won't evaluate cartesian product");        
	}

	public AttributeDefinitions evalDimensions( String predVar, Parsed target, boolean debug ) {
		AttributeDefinitions varDefs = new AttributeDefinitions(predVar, namedPredicates);
        varDefs.evalDimensions(target,false);
        //if( limits[0] == 0 )  redundant: if one dimension is 0, then it would be outer nested loop iterated 0 times
            //return new MaterializedPredicate(varDefs.getHeader(), target.getSrc(), null);
        boolean firstTime = true;
        if( debug ) {
        	System.out.print(predVar+" dimensions: ");
        	for( String varDef : varDefs.listDimensions() ) {
        		System.out.print(" "+varDef);
        	}
        	System.out.println();
        	System.out.print("Eval space = ");
        	for( String varDef : varDefs.listDimensions() ) {
				System.out.print((firstTime?"":"x")+varDefs.getDimensionContent(varDef).cardinality());
				firstTime = false;
			}
        	System.out.println();
        }
        firstTime = true;
		return varDefs;
	}

    public void logDimensions( String predVar,  Parsed target ) {
        evalDimensions(predVar, target, true);
    }

	@Override
	public String toString() {
		return getClass().getName()+"@"+objRef;
	}	

}
