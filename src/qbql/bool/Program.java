package qbql.bool;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import qbql.parser.Earley;
import qbql.parser.Grammar;
import qbql.parser.Lex;
import qbql.parser.LexerToken;
import qbql.parser.Matrix;
import qbql.parser.ParseNode;
import qbql.parser.RuleTuple;
import qbql.parser.SyntaxError;
import qbql.util.Util;

public class Program {

    public static void main( String[] args ) throws Exception {
        Set<RuleTuple> rules = latticeRules();
        RuleTuple.printRules(rules);
    }

    //// READ RULES

    public static Earley earley;
    
    public static int naturalJoin;    
    public static int innerUnion;
    static int userDefined;
    static int unaryUserDefined;
    static int userOper;
    static int contains;
    public static int complement;
    public static int inverse;
    static int join;
    static int meet;
    static int equivalence;
    static int equality;
    public static int expr;
    static int minus;
    static int dot;
    public static int parExpr;
    public static int openParen;
    public static int closeParen;
    public static int implication;
    static int proposition;
    static int oper;
    static int lt;
    static int gt;
    static int amp;
    static int bar;
    static int excl;
    public static int assertion;
    static int query;

    public static int identifier;
    //public static int string_literal;


    static int assignment;
    static int comma;
    static {        
        try {
            earley = new Earley(latticeRules());
            naturalJoin = earley.symbolIndexes.get("join");
            userDefined = earley.symbolIndexes.get("userDefined");
            unaryUserDefined = earley.symbolIndexes.get("unaryUserDefined");
            userOper = earley.symbolIndexes.get("userOper");
            innerUnion = earley.symbolIndexes.get("innerUnion");
            contains = earley.symbolIndexes.get("contains");
            complement = earley.symbolIndexes.get("complement");
            inverse = earley.symbolIndexes.get("inverse");
            
            join = earley.symbolIndexes.get("'v'");
            meet = earley.symbolIndexes.get("'^'");
            equivalence = earley.symbolIndexes.get("'~'");
            equality = earley.symbolIndexes.get("'='");
            minus = earley.symbolIndexes.get("'-'");
            dot = earley.symbolIndexes.get("'.'");
            lt = earley.symbolIndexes.get("'<'");
            gt = earley.symbolIndexes.get("'>'");
            amp = earley.symbolIndexes.get("'&'");
            bar = earley.symbolIndexes.get("'|'");
            excl = earley.symbolIndexes.get("'!'");
            expr = earley.symbolIndexes.get("expr");
            parExpr = earley.symbolIndexes.get("parExpr");
            openParen = earley.symbolIndexes.get("'('");
            closeParen = earley.symbolIndexes.get("')'");
            implication = earley.symbolIndexes.get("implication");
            proposition = earley.symbolIndexes.get("proposition");
            oper = earley.symbolIndexes.get("oper");
            assertion = earley.symbolIndexes.get("assertion");
            query = earley.symbolIndexes.get("query");

            identifier = earley.symbolIndexes.get("identifier");
            //string_literal = earley.symbolIndexes.get("string_literal");

            assignment = earley.symbolIndexes.get("assignment");
        } catch( Exception e ) {
            e.printStackTrace(); 
        }
    }

    public static Set<RuleTuple> latticeRules() throws Exception  {
        String input;
        try {
            input = Util.readFile(Program.class, "embededLattice.grammar");
        } catch (Exception e) {
            throw new AssertionError("Failed to read lattice.grammar file");
        }
        HashMap<String, String> specialSymbols = new HashMap<String, String>();
        specialSymbols.put("qtSymbol", "'");
        List<LexerToken> src = new Lex(
                                       true, true, false,
                                       specialSymbols                 
                ).parse(input);
        //LexerToken.print(src);
        ParseNode root = Grammar.parseGrammarFile(src, input);
        return Grammar.grammar(root, src);
    }

    //--------------------------------------------------------------------------

    public UnaryOperator algebra;
    private long mask;
    public Program( UnaryOperator algebra, String[] variables, String[] constants ) {
        this.algebra = algebra;
        mask = (1 << algebra.dimension) - 1;
        this.variables = variables;
        this.constants = constants;
    }

    private boolean bool( ParseNode root, List<LexerToken> src ) {
        for( ParseNode child : root.children() ) 
            if( child.contains(expr)  )
                return atomicProposition(root,src);
            //else if( child.contains(inductionFormula) )
                //return inductionFormula(root,src);
            else 
                return logical(root,src);
        throw new AssertionError("Impossible exception, no children??"+root.content(src));
    }

    private boolean logical( ParseNode root, List<LexerToken> src )  {
        Boolean left = null;
        int oper = -1;
        boolean impl = false;
        boolean bimpl = false;
        for( ParseNode child : root.children() ) {
            if( left == null ) {
                if( child.contains(minus) ) {
                    oper = minus;
                    left = true;
                } else if( child.contains(openParen) ) {
                    oper = openParen;
                    left = true;
                } else 
                    left = bool(child, src);
            } else if( child.contains(amp) ) 
                oper = amp;
            else if( child.contains(bar) ) {
                oper = bar;
            } else if( child.contains(gt)||child.contains(minus)||child.contains(lt) ) {
                if( child.contains(gt) )
                    impl = true;
                if( child.contains(lt) )
                    bimpl = true;
            } else {                               
                if( oper == amp ) {
                    // return left & bool(child) -- experienced function eval even when unnecessary
                    // therefore, optimized explicitly
                    if( !left )
                        return false;
                    else
                        return bool(child, src);
                } else if( oper == bar ) {
                    //return left | bool(child);
                    if( left )
                        return true;
                    else
                        return bool(child, src);
                } else if( oper == minus && !child.contains(openParen) )
                    return ! bool(child, src);
                else if( oper == openParen )
                    return bool(child, src);
                else if( impl && !bimpl ) { 
                    //return !left | bool(child);
                    if( !left )
                        return true;
                    else {
                        boolean ret = bool(child, src);
                        /*if( ret ) {
                            for( String variable : variables(root, false) )
                            	System.out.println(variable+" = "
                            			+database.predicate(variable).toString(variable.length()+3, false)
                            			+";");

                    	}*/
                        return ret;
                    }
                } else if( !impl && bimpl ) 
                    return left | !bool(child, src);
                else if( impl && bimpl ) 
                    return left == bool(child, src);
            }
        }
        throw new AssertionError("Unknown boolean operation "+oper);
    }

    private boolean atomicProposition( ParseNode root, List<LexerToken> src ) {
        for( ParseNode child : root.children() ) {
            if( child.contains(expr) )
                return booleanProposition(root,src);
            break;
        }
        throw new AssertionError("VT: unexpected proposition");
    }
    private boolean booleanProposition( ParseNode root, List<LexerToken> src ) {
        int oper = -1;
        long left = -1;
        long right = -1;
        boolean not = false;
        for( ParseNode child : root.children() ) {
            if( left == -1 )
                left = expr(child, src);
            else if( child.contains(excl) )
                not = true;
            else if( child.contains(equality) )
                oper = equality;
            else if( child.contains(lt) )
                oper = lt;
            else if( child.contains(gt) )
                oper = gt;
            else                            
                right = expr(child, src);
        }
        if( oper == lt )
            return /*left != right &&*/  (left & right) == left;
        else if( oper == gt )
            return (left & right) == right;
        else if( oper == equality )
            return not ? !(left==right) : left==right;

            throw new AssertionError("Impossible case");             
    }

    public boolean outputVariables = true;

    public boolean assertion( ParseNode root, List<LexerToken> src ) {
        boolean isNegation = false;
        for( ParseNode child : root.children() ) {
            if( isNegation ) 
                return !bool(child,src);
        	if( child.contains(excl) ) {
        		isNegation = true;
        		continue;
        	}
        	return bool(child,src);
        }
        throw new AssertionError("?");
    }
        


    static String[] variables( ParseNode root, List<LexerToken> src ) {
        Set<String> variables = new TreeSet<String>();
        for( ParseNode descendant : root.descendants() ) {
			if( descendant.from+1 == descendant.to 
                    && (descendant.contains(expr) || descendant.contains(identifier))
                     ) {
	            String id = descendant.content(src);
	            ParseNode parent = root.parent(descendant.from, descendant.to);
				if( !parent.contains(userOper)
						&& !id.startsWith("\"") ) 
	                variables.add(id);
			}
        }
        return variables.toArray(new String[0]);
    }
    static String[] constants( ParseNode root, List<LexerToken> src ) {
        Set<String> constants = new TreeSet<String>();
        for( ParseNode descendant : root.descendants() ) {
            if( descendant.from+1 == descendant.to 
                    && (descendant.contains(expr) || descendant.contains(identifier))
                     ) {
                String id = descendant.content(src);
                ParseNode parent = root.parent(descendant.from, descendant.to);
                if( !parent.contains(userOper)
                        && id.startsWith("\"") ) 
                    constants.add(id);
            }
        }
        return constants.toArray(new String[0]);
    }


    private String[] variables = null; //variables(root,src);
    private String[] constants = null; //variables(root,src);
    Map<String,Long> assignments = new TreeMap<String,Long>();
    /**
     * @param root
     * @param src
     * @return counterexample variable assignments
     * @
     */
    public int[] eval( ParseNode root, List<LexerToken> src ) {
        
        int[] indexes = new int[constants.length+variables.length];
        for( int i = 0; i < indexes.length; i++ )
            indexes[i] = 0;
        do {
            int var = 0;
            for( String variable : variables ) 
                assignments.put(variable, (long)indexes[var++]);
            for( String variable : constants ) 
                assignments.put(variable, (long)indexes[var++]);
            
            if( !verify(root, src) ) {
                for( int i = 0; i < variables.length; i++ )
                    indexes[i] = algebra.map.length-1; // 00000... is next
            } else {
                boolean exaustedAllVars = true;
                for( int i = 0; i < variables.length; i++ )
                    if( indexes[i] != algebra.map.length-1 ) {
                        exaustedAllVars = false;
                        break;
                    }
                if( exaustedAllVars )
                    return indexes;
            }
            
        } while( Util.next(indexes, algebra.map.length) );
        
        return null;
    }
    
    private boolean verify( ParseNode root, List<LexerToken> src ) {
        if( root.contains(assertion) )
            return assertion(root, src);
        /*if( root.contains(query) )
            return query(root, src);
        if( root.contains(assignment) ) {
            createPredicate(root, src);
            return null;
        }*/
        boolean ret = true;
        for( ParseNode child : root.children() ) {
            ret = verify(child, src);       
            if( !ret )
                return ret;
        }
        return ret;

    }

    private long expr( ParseNode root, List<LexerToken> src ) {
        if( root.contains(identifier) || root.from+1 == root.to ) {
            LexerToken token = src.get(root.from);
            Long candidate = assignments.get(token.content);
            if( candidate == null )
                throw new AssertionError(token.content+"== null ??");
            return candidate;
        } else       
            /*if( root.contains(relation) ) {
            for( ParseNode child : root.children() ) {
                if( child.contains(tuples) )
                    return tuples(child, src);
            }
        } else*/ 
        if( root.contains(naturalJoin) ) 
            return binaryOper(root,src, naturalJoin);
        else if( root.contains(complement) ) 
            return unaryOper(root,complement, src);
        else if( root.contains(inverse) ) 
            return unaryOper(root,inverse, src);
        else if( root.contains(parExpr) ) 
            return parExpr(root, src);

        throw new AssertionError("Unknown case");
    }


    private long binaryOper( ParseNode root, List<LexerToken> src, int oper )   {
        long left = -1;
        long right = -1;
        for( ParseNode child : root.children() ) {
            if( left == -1 && child.contains(expr) )
                left = expr(child,src);
            else if( child.contains(expr) )                           
                right = expr(child,src);
        }
        if( oper == naturalJoin )
            return left & right;
        //else user defined
        throw new AssertionError("Unknown case");
    }
    private long unaryOper( ParseNode root, int oper, List<LexerToken> src )   {
        for( ParseNode child : root.children() ) {
            if(child.from==0 && child.to==3)
                child.from=0;
            long rel = expr(child,src);
            if( oper == complement )
                return ~rel & mask;
            else if( oper == inverse )
                return algebra.map[(int) rel];
        }
        throw new AssertionError("Unknown case");
    }

    private long parExpr( ParseNode root, List<LexerToken> src )  {
        boolean parenthesis = false;
        for( ParseNode child : root.children() ) {
            if( child.contains(openParen) ) {
                parenthesis = true;
                continue;
            }
            if( parenthesis ) 
                return expr(child,src);           
        }
        throw new AssertionError("No parenthesis found");
    }


    static final String FALSE_ASSERTION = "*** False Assertion ***";
    static final String PARSE_ERROR_IN_ASSERTIONS_FILE = "*** Parse Error in assertions file ***";
    public static ParseNode parse( String prg, List<LexerToken> src ) {
        Matrix matrix = new Matrix(earley);
        earley.parse(src, matrix); 
        SyntaxError err = SyntaxError.checkSyntax(prg, new String[]{"theorem"}, src, earley, matrix);      
        if( err != null ) {
            System.out.println(err.toString());
            throw new AssertionError(PARSE_ERROR_IN_ASSERTIONS_FILE);
        }

        return earley.forest(src, matrix);
    }

}
