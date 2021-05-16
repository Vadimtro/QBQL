package qbql.induction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import qbql.apps.Run;
import qbql.lattice.Database;
import qbql.lattice.Program;
import qbql.parser.Earley;
import qbql.parser.Lex;
import qbql.parser.LexerToken;
import qbql.parser.Matrix;
import qbql.parser.ParseNode;
import qbql.parser.SyntaxError;
import qbql.util.Array;
import qbql.util.Profile;
import qbql.util.Util;

public class ExprGen {
    static boolean singleSolution = true;
    //static {singleSolution = false;}

    static String[] zilliaryOps;
    final static String[] unaryOps = new String[] {
        "<NOT>",
        //"<INV>",
        //"<EQ_CLOSE>",
        //"<CP_CLOSE>",
        //"<sq>",
    };
    static String[] binaryRelsOps;
    private static final String PARSE_ERROR_IN_ASSERTIONS_FILE = "*** Parse Error in assertions file ***";
    public static void main( String[] args ) throws Exception {
        final String goal = Util.readFile(ExprGen.class,"induction.prg");
        System.out.println("goal: "+Util.removeComments(goal));

        final String[] constants = new String[] {
                "R00",
                //"R11",
                //"S",
                //"R11",             
                //"Id", 
                //"R",
                //"A1A2",
                //"A1",
                //"A2",
        };

        final String[] binaryOps = new String[] {
                "^",
                "v", 
                //"(x)",
                //"<and>",
                //"<\"and\">",
                //"<OR>",
                //"/^",
                //"/>",
                //"/<",
                //"/=",
            //"/0",
            //"/1",
            //"/!",
        };
        final String[] binaryRels = new String[] {
                //"<",
                "=",
                //"!=",
                //"&",
                //"|"
        };
        //String skipTo = "((<NOT>(y) ^ <NOT>(y)) ^ (<NOT>(y) ^ <NOT>(y)))";
        String skipTo = "(((((((a ^ a) ^ a) ^ a) ^ a) ^ a) ^ a) ^ a)";
        skipTo = null;
        
        //Profile.sample(60000, 100);

        boolean includeUdf = true;
        //includeUdf = true;
        String quickFile = "FD.db";
        quickFile = "Figure1.db";
        String quickDbsrc = Util.readFile(ExprGen.class,quickFile);
        if( includeUdf )
        	quickDbsrc += "\n include udf.def;\n";
        //String fullDb = quickDb;
        String fullDbsrc = Util.readFile(Run.class,"db/Figure1.db");
        //fullDbsrc = Util.readFile(Run.class,"db/DependencyTheory.db");
        if( includeUdf )
        	fullDbsrc += "\n include udf.def;\n";

        final Lex lex = new Lex();
        List<LexerToken> src =  lex.parse(goal);
        Earley earley = Program.earley;
        Matrix matrix = new Matrix(earley);
        earley.parse(src, matrix); 
        SyntaxError err = SyntaxError.checkSyntax(goal, new String[]{"program"}, src, earley, matrix);      
        if( err != null ) {
            System.out.println(err.toString());
            throw new AssertionError(PARSE_ERROR_IN_ASSERTIONS_FILE);
        }
        ParseNode root = earley.forest(src, matrix);
        
        Map<String,long[]> assertions = new TreeMap<String,long[]>();
        listAssertions(root,src,goal,assertions);

        binaryRelsOps = new String[binaryOps.length];
        for( ParseNode subgoal : subgoals(root, src) )
        	if( subgoal.contains(Program.implication) )
        		binaryRelsOps = new String[binaryOps.length+binaryRels.length];
        for( int i = 0; i < binaryOps.length; i++ ) {
            binaryRelsOps[i] = binaryOps[i];
        }
        if( binaryRelsOps.length == binaryOps.length+binaryRels.length )
            for( int i = 0; i < binaryRels.length; i++ ) {
                binaryRelsOps[i+binaryOps.length] = binaryRels[i];
            }              

        int threads = 1;//Runtime.getRuntime().availableProcessors();
        if(  java.lang.management.ManagementFactory.getRuntimeMXBean(). getInputArguments().toString().contains("-agentlib:jdwp") )        	
        	threads = 1;
        System.out.println("Using " + threads + " threads");
        Verifier[] verifiers= new Verifier[threads];
        final Set<String> variables = extractVariables(root, src, new Program(new Database("qbql.lang")));
        variables.remove("expr");
variables.remove("R");
variables.remove("S");
variables.remove("A1");
variables.remove("A2");
variables.remove("A1A2");
        for( int i = 0; i < threads; i++ ) {
            Database quickDb = new Database("qbql.lang");
            final Program quick = new Program(quickDb);
            quick.outputVariables = false;
            quick.run(quickDbsrc);       
            Database fullDb = new Database("qbql.lang");
            final Program full = new Program(fullDb);
            full.outputVariables = false;
            full.run(fullDbsrc);       


            final Set<String> databaseOperations = new HashSet<String>();
            databaseOperations.addAll(full.database.operationNames());
            for( String op : databaseOperations )
                quick.database.addOperation(op, full.database.getOperation(op));
            verifiers[i] = new Verifier(assertions, lex, quick, full, databaseOperations, variables.toArray(new String[0]), binaryRels);
        }

        zilliaryOps = new String[variables.size()+constants.length];
        for( int i = 0; i < variables.size(); i++ ) {
            zilliaryOps[i] = variables.toArray(new String[0])[i];
        }
        for( int i = 0; i < constants.length; i++ ) {
            zilliaryOps[i+variables.size()] = constants[i];
        }

        ArrayList<TreeNode> l = new ArrayList<TreeNode>();
        l.add(Polish.leaf());
        l.add(TreeNode.one);

        for( Polish num = new Polish(l); ; num.next() ) {
            if( !num.wellBuilt() )
                continue;
            final TreeNode n = num.decode(); 
            if( n != null ) {
                //if( n.isRightSkewed() ) // !!!conflicts with other variable assignments!!!
                //continue;

                //System.out.println();
                try {
                    init(n);
                } catch( ArrayIndexOutOfBoundsException e ) { // no unary operations
                    continue;
                }
                if( singleSolution )
                    n.print();
                /*if( "((<NOT>(z) ^ z) ^ (z ^ z))".equals(n.toString()) ) {
            		System.out.println("*** reached (<NOT>((<NOT>(r) ^ r)) ^ (<NOT>(r) ^ r)) *** ");
            		System.out.println("Elapsed="+(System.currentTimeMillis()-ExprGen.startTime));
            		System.out.println("evalTime="+ExprGen.evalTime);
            		System.exit(0);                	
                }*/
				if( n.toString().equals(skipTo) )
					skipTo = null;
                if( skipTo != null )
                    continue;
                
                //System.out.println("-----------------");
                do {  
                    //if( n.toString().contains("((<NOT>(y) ^ <NOT>(x)) ^ (<INV>(y) ^ <INV>(x)))") ) {
                        //System.out.println("---->((<NOT>(y) ^ <NOT>(x)) ^ (<INV>(y) ^ <INV>(x)))");
                    //}
                    if( n.isRightSkewed() )
                        continue;
                    if( n.isAbsorpIdemp() )
                        continue;
                    if( n.isDoubleComplement() )
                        continue;
                    //if( !n.variables().containsAll(variables) )
                    	//continue;
                    //if( n.toString().startsWith("((((y ^ x) v r) ^") && n.toString().endsWith("v r)) < r)") )
                    	//n.print();
                    boolean launched = false;
                    do {
                        if( verifiers.length == 1 ) {
                            verifiers[0].exec(n);
                            break;
                        }
                        for( Verifier verifier : verifiers ) {
                            if( verifier.thread!=null && verifier.thread.isAlive() ) {
                                Thread.yield();
                                continue;
                            }
                            verifier.execThread(n);
                            launched = true;
                            break;
                        }
                    } while( !launched );
                } while( ExprGen.next(n) );
            } else {
                //System.out.print('.');
            }
        }
    }

	static long startTime = System.currentTimeMillis();
    static long evalTime = 0;




    private static Set<String> extractVariables( ParseNode root, List<LexerToken> src, Program p/*, int subgoal*/ ) {
        if( root.contains(Program.assertion) ) {
            for( ParseNode d : root.descendants() )
                 return p.variables(root,src);
        } 
        for( ParseNode child : root.children() ) {
            Set<String> ret = extractVariables(child, src, p);       
            if( ret != null )
                return ret;
        }
        return null;
    }

    private static List<ParseNode> subgoals( ParseNode root, List<LexerToken> src ) {
    	List<ParseNode> ret = new LinkedList<ParseNode>();
    	for( ParseNode p : root.descendants() )
    		if( p.from+1==p.to && p.contains(Program.expr) && p.content(src).equals(Program.earley.allSymbols[Program.expr]) )
                ret.add(p);
    		else if( p.contains(Program.inductionFormula) )
                ret.add(p);
    	//if( ret.size() == 0 )
    		//return null; //e.g. x <op> y = expr. ...  //throw new AssertionError("no subgoal?");
        return ret;
    }

    static void init( TreeNode node ) {
        if( node.left() == null ) 
            node.label = zilliaryOps[0];
        else {
            init(node.left());
            if( node.right() == null ) 
                node.label = unaryOps[0];
            else {
                init(node.right());
                node.label = binaryRelsOps[0];
            }
        }
    }
    static boolean next( TreeNode node ) {
        Boolean ok = false;
        if( node.left() != null ) {
            ok = next(node.left());
            if( ok )
                return true;
        }
        if( node.right() != null ) {
            init(node.left());
            ok = next(node.right());
            if( ok )
                return true;
        }

        if( node.left() == null ) {
            int index = index(node.label,zilliaryOps)+1;
            if( index == zilliaryOps.length )
                return false;
            else {
                init(node);
                node.label = zilliaryOps[index];
                return true;
            }
        } else {
            if( node.right() == null ) {
                int index = index(node.label,unaryOps)+1;
                if( index == unaryOps.length )
                    return false;
                else {
                    init(node);
                    node.label = unaryOps[index];
                    return true;
                }
            } else {
                int index = index(node.label,binaryRelsOps)+1;
                if( index == binaryRelsOps.length )
                    return false;
                else {
                    init(node);
                    node.label = binaryRelsOps[index];
                    return true;
                }
            }
        }
    }
    // for syntactically invalid nodes jump to next operation
    static boolean nextOp( TreeNode node ) {
        Boolean ok = false;
        if( node.left() != null ) {
            ok = nextOp(node.left());
            if( ok )
                return true;
        }
        if( node.right() != null ) {
            init(node.left());
            ok = nextOp(node.right());
            if( ok )
                return true;
        }

        if( node.left() == null ) {
        	node.label = zilliaryOps[zilliaryOps.length-1];
            return true;
        } else {
            if( node.right() == null ) {
                int index = index(node.label,unaryOps)+1;
                if( index == unaryOps.length )
                    return false;
                else {
                    init(node);
                    node.label = unaryOps[index];
                    return true;
                }
            } else {
                int index = index(node.label,binaryRelsOps)+1;
                if( index == binaryRelsOps.length )
                    return false;
                else {
                    init(node);
                    node.label = binaryRelsOps[index];
                    return true;
                }
            }
        }
    }
    private static int index( String s, String[] src ) {
        int ret = -1;
        for (int i = 0; i < src.length; i++) {
            if( s.equals(src[i]) )
                return i;
        }
        return ret;
    }

    private static void listAssertions( ParseNode root, List<LexerToken> src, String input, Map<String,long[]> ret ) {
		if( root.contains(Program.assertion) ) {
		    String prefix = "/*"; 
		    for( String assertion : ret.keySet() ) {
		        prefix = assertion.substring(0,assertion.indexOf("*/"));
		        break;
		    }
			prefix += "_*/";
			int offset = src.get(root.from).begin;
			String assertion = input.substring(offset,src.get(root.to-1).end);
			long[] entries = new long[0];
			for( ParseNode goal : subgoals(root, src) ) {
				entries = Array.insert(entries, Util.lPair(src.get(goal.from).begin-offset+prefix.length(), src.get(goal.to-1).end-offset+prefix.length()));
			}
			ret.put(prefix+assertion,entries);
			return;
		}
		for( ParseNode p : root.children() )
			listAssertions(p, src, input, ret);
	}

}
