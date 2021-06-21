package qbql.lattice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import qbql.apps.Run;
import qbql.parser.Earley;
import qbql.parser.Grammar;
import qbql.parser.Lex;
import qbql.parser.LexerToken;
import qbql.parser.Matrix;
import qbql.parser.ParseNode;
import qbql.parser.RuleTuple;
import qbql.parser.SyntaxError;
import qbql.parser.Token;
import qbql.parser.Visual;
import qbql.parser.Parser.Tuple;
import qbql.util.Util;

public class Program {

    final boolean visualize = false;

    //// READ RULES

    public static Earley earley;
    
    public static int naturalJoin;
    public static int innerUnion;
    public static int count;
    static int userDefined;
    static int unaryUserDefined;
    static int userOper;
    static int unnamedJoin;
    static int unnamedMeet;
    static int setIX;
    static int setEQ;
    static int contains;
    static int transpCont;
    static int disjoint;
    static int almostDisj;
    static int big;
    public static int complement;
    static int CPclosure;
    static int EQclosure;
    public static int inverse;
    static int composition;
    static int restrict;
    static int project;
    static int rename;
    static int difference;
    static int directProduct;
    static int join;
    static int meet;
    static int equivalence;
    static int equality;
    public static int expr;
    static int num;
    static int minus;
    static int dot;
    static int partition;
    public static int parExpr;
    public static int openParen;
    public static int closeParen;
    public static int implication;
    public static int inductionFormula;
    static int proposition;
    static int oper;
    static int lt;
    static int gt;
    static int amp;
    static int bar;
    static int excl;
    public static int assertion;
    static int query;

    static int include;
    static int filename;

    public static int identifier;
    //public static int string_literal;


    static int assignment;
    static int relation;
    static int table;
    /*static int tuples;
    static int tuple;*/
    static int header;
    static int content;
    static int value;
    //public static int attribute;
    //static int values;
    //static int namedValue;
    static int comma;
    static {        
        try {
            earley = new Earley(latticeRules()) {
    			@Override
    			protected boolean isScannedSymbol( int y, List<LexerToken> src, int pos, Tuple t, Integer suspect ) {
    				int symbol = t.content(pos);
    				LexerToken token = src.get(y);
    				if( symbol == digits && token.type == Token.DIGITS )
    					return true;
    				if( symbol == string_literal && token.type == Token.QUOTED_STRING )
    					return true;
    				if( symbol == identifier && token.type == Token.DQUOTED_STRING )
    					return true;
    				return suspect != null && suspect == symbol 
    					|| isIdentifier(y,src, symbol, suspect) && (suspect==null||notConfusedAsId(suspect,t.head,pos))
    				;
    			}
    			/*@Override
    		    protected boolean isIdentifier( int y, List<LexerToken> src, int symbol, Integer suspect ) {
    		        if( symbol != identifier )
    		            return false;
    				LexerToken token = src.get(y);
    				return symbol == identifier && token.type == Token.IDENTIFIER
    		          ||   symbol == identifier && token.type == Token.DQUOTED_STRING;
    		    }*/
            };
            naturalJoin = earley.symbolIndexes.get("join");
            userDefined = earley.symbolIndexes.get("userDefined");
            unaryUserDefined = earley.symbolIndexes.get("unaryUserDefined");
            userOper = earley.symbolIndexes.get("userOper");
            innerUnion = earley.symbolIndexes.get("innerUnion");
            count = earley.symbolIndexes.get("count");
            unnamedJoin = earley.symbolIndexes.get("unnamedJoin");
            unnamedMeet = earley.symbolIndexes.get("unnamedMeet");
            setIX = earley.symbolIndexes.get("setIX");
            setEQ = earley.symbolIndexes.get("setEQ");
            contains = earley.symbolIndexes.get("contains");
            transpCont = earley.symbolIndexes.get("transpCont");
            disjoint = earley.symbolIndexes.get("disjoint");
            almostDisj = earley.symbolIndexes.get("almostDisj");
            big = earley.symbolIndexes.get("big");
            complement = earley.symbolIndexes.get("complement");
            CPclosure = earley.symbolIndexes.get("CPclosure");
            EQclosure = earley.symbolIndexes.get("EQclosure");
            inverse = earley.symbolIndexes.get("inverse");
            composition = earley.symbolIndexes.get("composition");
            restrict = earley.symbolIndexes.get("restrict");
            project = earley.symbolIndexes.get("project");
            rename = earley.symbolIndexes.get("rename");
            difference = earley.symbolIndexes.get("difference");
            directProduct = earley.symbolIndexes.get("directProduct");
            
            join = earley.symbolIndexes.get("'v'");
            meet = earley.symbolIndexes.get("'^'");
            equivalence = earley.symbolIndexes.get("'~'");
            equality = earley.symbolIndexes.get("'='");
            minus = earley.symbolIndexes.get("'-'");
            dot = earley.symbolIndexes.get("'.'");
            lt = earley.symbolIndexes.get("'<'");
            gt = earley.symbolIndexes.get("'>'");
            amp = earley.symbolIndexes.get("'&'");
            num = earley.symbolIndexes.get("'#'");
            bar = earley.symbolIndexes.get("'|'");
            excl = earley.symbolIndexes.get("'!'");
            expr = earley.symbolIndexes.get("expr");
            partition = earley.symbolIndexes.get("partition");
            parExpr = earley.symbolIndexes.get("parExpr");
            openParen = earley.symbolIndexes.get("'('");
            closeParen = earley.symbolIndexes.get("')'");
            implication = earley.symbolIndexes.get("implication");
            inductionFormula = earley.symbolIndexes.get("inductionFormula");
            proposition = earley.symbolIndexes.get("proposition");
            oper = earley.symbolIndexes.get("oper");
            assertion = earley.symbolIndexes.get("assertion");
            query = earley.symbolIndexes.get("query");

            include = earley.symbolIndexes.get("include");
            filename = earley.symbolIndexes.get("filename");

            identifier = earley.symbolIndexes.get("identifier");
            //string_literal = earley.symbolIndexes.get("string_literal");

            assignment = earley.symbolIndexes.get("assignment");
            relation = earley.symbolIndexes.get("relation");
            table = earley.symbolIndexes.get("table");
            /*tuples = earley.symbolIndexes.get("tuples");
            tuple = earley.symbolIndexes.get("tuple");*/
            header = earley.symbolIndexes.get("header");
            content = earley.symbolIndexes.get("content");
            value = earley.symbolIndexes.get("value");
            //attribute = earley.symbolIndexes.get("attribute");
            //values = earley.symbolIndexes.get("values");
            //namedValue = earley.symbolIndexes.get("namedValue");
            //System.out.println(earley.allSymbols[20]);
            
        	prioritizeRules(earley);
        	//earley.isAsc = true;

        } catch( Exception e ) {
            e.printStackTrace(); 
        }
    }

	private static void prioritizeRules( Earley testParser ) {
		String rule2 = "proposition:  expr  '='  expr;";
		String rule3 = "proposition:  implication;";
		testParser.swapRules(rule2, rule3);	
	}
	
    public static List<RuleTuple> latticeRules() throws Exception  {
        String input;
        try {
            input = Util.readFile(Program.class, "lattice.grammar");
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

    public Database database;
    public Program( Database db ) {
        this.database = db;
    }

    private boolean bool( ParseNode root, List<LexerToken> src ) {
        for( ParseNode child : root.children() ) 
            if( child.contains(expr) || child.contains(partition) )
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
            if( child.contains(partition) )
                return partitionProposition(root, src);
            else if( child.contains(expr) )
                return relationalProposition(root,src);
            break;
        }
        throw new AssertionError("VT: neither relational not partitional proposition");
    }
    private boolean partitionProposition( ParseNode root, List<LexerToken> src ) {
        int oper = -1;
        Partition left = null;
        Partition right = null;
        for( ParseNode child : root.children() ) {
            if( left == null )
                left = partition(child, src);
            else if( child.contains(lt) )
                oper = lt;
            else if( child.contains(gt) )
                oper = gt;
            else if( child.contains(equality) )
                oper = equality;
            else
                right = partition(child, src);
        }
        if( oper == lt )
            return Partition.le(left,right);
        if( oper == gt )
            return Partition.ge(left,right);
        if( oper == equality )
            return left.equals(right);

        throw new AssertionError("Impossible case");             
    }
    private boolean relationalProposition( ParseNode root, List<LexerToken> src ) {
        int oper = -1;
        Predicate left = null;
        Predicate right = null;
        boolean not = false;
        for( ParseNode child : root.children() ) {
            if( left == null )
                left = expr(child, src);
            else if( child.contains(excl) )
                not = true;
            else if( child.contains(equality) )
                oper = equality;
            else if( child.contains(lt) )
                oper = lt;
            else if( child.contains(gt) )
                oper = gt;
            else if( oper == lt && child.contains(equivalence) )
                oper = -lt;
            else if( oper == gt && child.contains(equivalence) )
                oper = -gt;
            else if( child.contains(equivalence) )
                oper = equivalence;
            else                            
                right = expr(child, src);
        }
        if( !(left instanceof Relation) )
            left = left.reEvaluateByUnnesting();
        if( !(right instanceof Relation) )
            right = right.reEvaluateByUnnesting();
        if( oper == lt )
            return Relation.le((Relation)left,(Relation)right);
        else if( oper == gt )
            return Relation.ge((Relation)left,(Relation)right);
        else if( oper == -lt )
            return database.submissive((Relation)left,(Relation)right);
        else if( oper == -gt )
            return database.submissive((Relation)right,(Relation)left);
        else if( oper == equivalence )
            return database.equivalent((Relation)left,(Relation)right);
        else if( oper == equality )
            return not ? !left.equals(right) : left.equals(right);

            throw new AssertionError("Impossible case");             
    }

    public boolean outputVariables = true;

    public ParseNode assertion( ParseNode root, List<LexerToken> src ) {
        ParseNode ret = null;
        boolean isNegation = false;
        for( ParseNode child : root.children() ) {
        	if( child.contains(excl) ) {
        		isNegation = true;
        		break;
        	}
        	
        	if( isDeclaration(child, src) )
                return null;
            String[] ops = operIneqArgs(child,src);
            if( ops == null )
                break;
            if( assertEqOp(ops[0], ops[1]) )
                return child;
            return null;
        }

        Set<String> variables = variables(root,src);

        String[] tables = database.relNames();
        int[] indexes = new int[variables.size()];
        for( int i = 0; i < indexes.length; i++ )
            indexes[i] = 0;
        boolean foundCounterExample = false;
        iterateThroughVariables:
        do {
            int var = 0;
            for( String variable : variables ) {
                //System.out.println(variable+"="+tables[indexes[var]]);
                database.addPredicate(variable, database.getPredicate(tables[indexes[var++]]));
            }

            for( ParseNode child : root.children() ) {
            	if( child.contains(implication) ) {
                    if( !bool(child, src) ) {
						foundCounterExample = true;
						if( isNegation )
							break iterateThroughVariables;
                        for( String variable : variables )
                            if( outputVariables )
                                System.out.println(variable+" = "
                                        +database.getPredicate(variable).toString(variable.length()+3)
                                        +";");
                        ret = child;
                        for( String variable : variables )
                            database.removePredicate(variable);
                        return ret;
                    } 
                    break;
                } else if( child.contains(excl) ) {
            		continue;
            	} else 
                    throw new AssertionError("Non boolean assertion???"); 
            }
        } while( Util.next(indexes,tables.length) );
        
        for( String variable : variables )
            database.removePredicate(variable);
        
        if( isNegation && !foundCounterExample )
        	return root;

        return ret;
    }
    private String[] operIneqArgs( ParseNode root, List<LexerToken> src )  {
        if( root.contains(proposition) ) {
            String[] ret = new String[2];
            boolean sawEq = false;
            boolean sawExcl = false;
            for( ParseNode child : root.children() ) {
                if( ret[0] == null ) 
                    if( !child.contains(oper) )
                        break;
                    else {
                        ret[0] = child.content(src);
                        continue;
                    }
                if( !sawExcl ) 
                    if( !child.contains(excl) )
                        throw new AssertionError("Expected '!'");
                    else {
                        sawExcl = true;
                        continue;
                    }
                if( !sawEq ) 
                    if( !child.contains(equality) )
                        throw new AssertionError("Expected '='");
                    else {
                        sawEq = true;
                        continue;
                    }
                if( ret[1] == null) {
                    ret[1] = child.content(src);
                    return ret;
                } else
                    throw new AssertionError("UnExpected case");
            }

        }
        return null;
    }
    private boolean assertEqOp( String op1, String op2 ) {
        String input = "x "+op1+" y = x "+op2+" y.";

        LinkedList<LexerToken> src = new Lex().parse(input);

        Matrix matrix = new Matrix(earley);
        earley.parse(src, matrix); 
        ParseNode root = Program.earley.forest(src, matrix);
        if( !root.contains(Program.earley.symbolIndexes.get("assertion") ) )
            throw new AssertionError("Parse Error");     

        return assertion(root,src)==null;
    }

    private boolean isDeclaration( ParseNode root, List<LexerToken> src ) throws AssertionError {
        if( root.contains(proposition) ) {
            String lft = null;
            String operation = null;
            String rgt = null;
            boolean sawEQ = false;
            for( ParseNode child : root.children() ) {
                if( !child.contains(userDefined) && operation == null )
                    break;
                if( operation == null ) {
                    for( ParseNode grandChild : child.children() ) {
                        if( lft == null && grandChild.contains(userOper) ) {
                            lft = "R00";
                            operation = grandChild.content(src);
                            try { 
                                database.getOperation(operation);
                                return false;
                            } catch( AssertionError e ) {} 
                        } else if( lft == null ) 
                            lft = grandChild.content(src);
                        else if( operation == null ) {
                            operation = grandChild.content(src);
                            try { 
                                database.getOperation(operation);
                                return false;
                            } catch( AssertionError e ) {} 
                        } else if( rgt == null ) 
                            rgt = grandChild.content(src);
                        else if( grandChild.contains(closeParen) ) 
                            ;
                        else
                            throw new AssertionError("Unexpected user defined expression");
                    }
                } else if( !sawEQ ) {
                    if( !child.contains(equality) ) 
                        throw new AssertionError("Unexpected user defined expression");
                    else 
                        sawEQ = true;
                } else {
                    database.addOperation(operation, Expr.convert(lft, rgt, child, src));
                    return true;
                }
            }
        }
        return false;
    }


    private Set<String> variables( ParseNode root, List<LexerToken> src, boolean notAssignedOnes ) {
        Set<String> variables = new HashSet<String>();
        for( ParseNode descendant : root.descendants() ) {
			if( descendant.from+1 == descendant.to 
                    && (descendant.contains(expr) || descendant.contains(identifier))
                    && !descendant.contains(value) ) {
	            String id = descendant.content(src);
	            ParseNode parent = root.parent(descendant.from, descendant.to);
				if( !parent.contains(header)
						&& !parent.contains(table)
						&& !parent.contains(userOper)
						&& !(parent.contains(inductionFormula)/*&&root.from==descendant.from*/)
						&& !id.startsWith("\"")
						&& (!notAssignedOnes || database.lookup(id) == null) ) 
	                variables.add(id);
			}
        }
        return variables;
    }
    public Set<String> variables( ParseNode root, List<LexerToken> src ) {
        return variables(root, src, true);
    }

    private ParseNode query( ParseNode root, List<LexerToken> src )  {
        for( ParseNode child : root.children() ) {
            if( child.contains(partition) ) {
                System.out.println(child.content(src)+"="+partition(child, src).toString()+";");
                return null;
            } else if( child.contains(expr) ) {
                Predicate expr2 = expr(child, src);

                try {
                    Predicate p = expr2.reEvaluateByUnnesting();
                    if( p instanceof Relation )
                        expr2 = p;
                } catch( AssertionError e ) {}

                System.out.println(child.content(src)+"="+expr2.toString(child.content(src).length()+1)+";");
                return null;
            } 
        }
        throw new AssertionError("No expr/partition in statement?");
    }


    /**
     * @param root
     * @param src
     * @return node that violates assertion
     * @
     */
    public ParseNode program( ParseNode root, List<LexerToken> src ) {
        if( root.contains(include) )
            return include(root, src);
        if( root.contains(assertion) )
            return assertion(root, src);
        if( root.contains(query) )
            return query(root, src);
        if( root.contains(assignment) ) {
            createPredicate(root, src);
            return null;
        }
        ParseNode ret = null;
        for( ParseNode child : root.children() ) {
            ret = program(child, src);       
            if( ret != null )
                return ret;
        }
        return ret;
    }


    private ParseNode include( ParseNode root, List<LexerToken> src ) {
        String prg = null;	
        String fname = null;
        try {
            StringBuilder fn = new StringBuilder();
            for( int i = root.from+1; i < root.to-1; i++ ) 
                fn.append(src.get(i).content);
            fname = fn.toString();
            if( fname.startsWith("\"") )
                prg = Util.readFile(fname.substring(1,fname.length()-1));
            else
                prg = Util.readFile(Run.class,fname);
            System.out.println("Processing "+fname+" ...");
        } catch ( Exception e ) {
            throw new RuntimeException("failed to read the file "+fname);
        }
        try {
            run(prg);
        } catch( AssertionError e ) {
            if( Program.PARSE_ERROR_IN_ASSERTIONS_FILE.equals(e.getMessage()) ) {
                System.out.println("^^^ Parse error in "+fname.toString());
            }
            throw e;
        }
        return null;
    }

    private void createPredicate( ParseNode root, List<LexerToken> src )  {
        String left = null;
        Predicate right = null;
        for( ParseNode child : root.children() ) {
            if( left == null )
                left = child.content(src);
            else if( child.contains(equality) )
                ;
            else {                          
                right = expr(child, src);
                //breaking test: right = right.reEvaluateByUnnesting();
                break;
            }
        }
        database.addPredicate(left, right);
    }
    private Predicate expr( ParseNode root, List<LexerToken> src ) {
        if( root.contains(identifier) || root.from+1 == root.to ) {
            LexerToken token = src.get(root.from);
            Predicate candidate = database.lookup(token.content);
            if( candidate == null )
                throw new AssertionError("Predicate/Table '"+token.content+"' not in the database");
            return candidate;
        } else       
            /*if( root.contains(relation) ) {
            for( ParseNode child : root.children() ) {
                if( child.contains(tuples) )
                    return tuples(child, src);
            }
        } else*/ 
            if( root.contains(table) ) {
                Relation ret = Database.R00;
                //String colX = null;
                for( ParseNode child : root.children() ) {
                    if( child.contains(header) )                    
                        ret = new Relation(values(child, src).toArray(new String[0]));
                    else if( child.contains(content) ) {
                        addContent(ret,child, src);
                        return ret;
                    } /*else if( child.contains(identifier) && colX == null ) 
                    colX = child.content(src);
                else if( child.contains(identifier) ) {
                    return new EqualityPredicate(colX, child.content(src));
                }*/
                } 
                return ret;
            } /*else if( root.contains(renamedRel) ) {
            String relName = null;
            List<String> columns = null;
            for( ParseNode child : root.children() ) {
                if( child.contains(header) )
                    columns = strings(child);
                else if( child.contains(identifier) ) 
                    relName = child.content(src);
            }
            Relation ret = Relation.join(lookup(relName),Database.R01); // clone
            if( columns.size() != ret.colNames.length )
                throw new RuntimeAssertionError();
            for( int i = 0; i < ret.colNames.length; i++ ) {
                ret.renameInPlace(ret.colNames[i], columns.get(i));
            }
            return ret;
        }*/ else if( root.contains(naturalJoin) ) 
            return binaryOper(root,src, naturalJoin);
        else if( root.contains(restrict) ) 
            return raOper(root,src, restrict);
        else if( root.contains(project) ) 
            return raOper(root,src, project);
        else if( root.contains(rename) ) 
            return raOper(root,src, rename);
        else if( root.contains(difference) ) 
            return binaryOper(root,src, difference);
        else if( root.contains(directProduct) ) 
            return binaryOper(root,src, directProduct);
        else if( root.contains(userDefined) || root.contains(unaryUserDefined) ) 
            return userDefined(root,src);
        else if( root.contains(innerUnion) ) 
            return binaryOper(root,src, innerUnion);
        else if( root.contains(count) ) 
            return binaryOper(root,src, count);
        else if( root.contains(unnamedJoin) ) 
            return binaryOper(root,src, unnamedJoin);
        else if( root.contains(unnamedMeet) ) 
            return binaryOper(root,src, unnamedMeet);
        else if( root.contains(complement) ) 
            return unaryOper(root,complement, src);
        else if( root.contains(CPclosure) ) 
            return unaryOper(root,CPclosure, src);
        else if( root.contains(EQclosure) ) 
            return unaryOper(root,EQclosure, src);
        else if( root.contains(inverse) ) 
            return unaryOper(root,inverse, src);
        else if( root.contains(setIX) ) 
            return binaryOper(root,src, setIX);
        else if( root.contains(setEQ) ) 
            return binaryOper(root,src, setEQ);
        else if( root.contains(contains) ) 
            return binaryOper(root,src, contains);
        else if( root.contains(transpCont) ) 
            return binaryOper(root,src, transpCont);
        else if( root.contains(disjoint) ) 
            return binaryOper(root,src, disjoint);
        else if( root.contains(almostDisj) ) 
            return binaryOper(root,src, almostDisj);
        else if( root.contains(big) ) 
            return binaryOper(root,src, big);

        else if( root.contains(parExpr) ) 
            return parExpr(root, src);

        throw new AssertionError("Unknown case");
    }


    private Predicate userDefined( ParseNode root, List<LexerToken> src )   {
        Predicate left = null;
        Predicate right = null;
        String oper = null;
        for( ParseNode child : root.children() ) {
            if( left == null && child.contains(userOper) ) { // e.g. ( <NOT> x )
                left = Database.R00; 
                oper = child.content(src);
            } else if( left == null && child.contains(expr) )
                left = expr(child, src);
            else if( oper != null ) {                           
                right = expr(child, src);
                break;  // in order not to step on closing parenthesis
            } else //if( oper == null )
                oper = child.content(src);
        }
        Predicate lft = database.getPredicate("?lft");
        Predicate rgt = database.getPredicate("?rgt");
        database.addPredicate("?lft",left);
        database.addPredicate("?rgt",right);
        Expr e = database.getOperation(oper);
        Predicate ret = e.eval(database);
        database.removePredicate("?lft");
        database.removePredicate("?rgt");
        if( lft != null )
            database.addPredicate("?lft",lft);
        if( rgt != null )
            database.addPredicate("?rgt",rgt);
        return ret;
    }

    private Predicate binaryOper( ParseNode root, List<LexerToken> src, int oper )   {
        Predicate left = null;
        Predicate right = null;
        for( ParseNode child : root.children() ) {
            if( left == null && child.contains(expr) )
                left = expr(child,src);
            else if( child.contains(expr) )                           
                right = expr(child,src);
        }
        if( oper == naturalJoin )
            return Predicate.join(left,right);
        else if( oper == innerUnion )
            return Predicate.union(left,right);
        else if( oper == difference )
            return Predicate.join(left,new ComplementPredicate(right));
        else if( oper == directProduct )
            return Relation.directProduct((Relation)left,(Relation)right);
        else if( oper == count )
            return Relation.count(left,right);
        else if( oper == unnamedMeet )
            return database.unnamedMeet((Relation)left,(Relation)right);
        else if( oper == setEQ ) {
            if( left instanceof Relation && right instanceof Relation )
                return database.quantifier((Relation)left,(Relation)right,setEQ);
            else
                return Predicate.setEQ(left,right);
        } else if( oper == setIX ) 
            return Predicate.setIX(left,right);
        else if( oper == contains ) 
            return database.quantifier((Relation)left,(Relation)right,contains);
        else if( oper == transpCont ) 
            return database.quantifier((Relation)left,(Relation)right,transpCont);
        else if( oper == disjoint ) 
            return database.quantifier((Relation)left,(Relation)right,disjoint);
        else if( oper == almostDisj ) 
            return database.quantifier((Relation)left,(Relation)right,almostDisj);
        else if( oper == big ) 
            return database.quantifier((Relation)left,(Relation)right,big);
        throw new AssertionError("Unknown case");
    }
    private Predicate unaryOper( ParseNode root, int oper, List<LexerToken> src )   {
        for( ParseNode child : root.children() ) {
            Predicate rel = expr(child,src);
            if( oper == complement )
                return database.complement(rel);
            else if( oper == inverse )
                return database.inverse((Relation)rel);
            else if( oper == CPclosure )
                return ((Relation)rel).CPclosure();
            else if( oper == EQclosure )
                return database.EQclosure(rel);

        }
        throw new AssertionError("Unknown case");
    }

    private Predicate raOper( ParseNode root, List<LexerToken> src, int oper )   {
        boolean sawOper = false;
        Predicate first = null;
        Predicate second = null;
        for( ParseNode child : root.children() ) {
            if( !sawOper ) {
                sawOper = true;
            } else if( first == null ) {
                if( oper == project ) {
                    List<String> attrs = strings(child,src);
                    first = new Relation(attrs.toArray(new String[0]));
                } else if( oper == restrict )
                    first = expr(child,src);
                else if( oper == rename ) {
                    List<String> attrs = strings(child,src);
                    first = new EqualityPredicate(attrs.get(0),attrs.get(1));
                }
            } else                            
                second = expr(child,src);
        }
        if( oper == restrict )
            return Predicate.join(first,second);
        else if( oper == project )
            return Predicate.union(first,second);
        else if( oper == rename) {
            Predicate ret = second.clone();
            ret.renameInPlace(first.colNames[0], first.colNames[1]);
            return ret;
        }
        throw new AssertionError("Unknown case");
    }
    
    private Predicate parExpr( ParseNode root, List<LexerToken> src )  {
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


    private Partition partition( ParseNode root, List<LexerToken> src ) {
        boolean parenthesis = false;
        for( ParseNode child : root.children() ) {
            if( child.contains(openParen) ) {
                parenthesis = true;
                continue;
            }
            if( parenthesis ) 
                return partition(child, src);
            if( child.contains(partition) )
                return partPart(root, src);
            else if( child.contains(expr) )
                return relPart(root, src);
            break;
        }
        throw new AssertionError("Unknown case");
    }
    private Partition partPart( ParseNode root, List<LexerToken> src ) {
        Partition left = null;
        Partition right = null;
        int oper = -1;
        for( ParseNode child : root.children() ) {
            if( left == null )
                left = partition(child, src);
            else if( child.contains(join) )
                oper = join;
            else if( child.contains(meet) )
                oper = meet;
            else if( child.contains(composition) )
                oper = composition;
            else if( child.contains(partition) )                           
                right = partition(child, src);
            else
                break;
        }
        if( oper == join )
            return Partition.union(left,right);
        if( oper == meet )
            return Partition.intersect(left,right);
        if( oper == composition )
            return Partition.composition(left,right);

        throw new AssertionError("Impossible case");             
    }
    private Partition relPart( ParseNode root, List<LexerToken> src )  {
        Relation left = null;
        Relation right = null;
        for( ParseNode child : root.children() ) {
            if( left == null )
                left = (Relation)expr(child,src);
            else if( child.contains(num) )
                ;
            else                            
                right = (Relation)expr(child,src);
        }
        return new Partition(left,right,database);
    }


    /*private Relation tuples( ParseNode root, List<LexerToken> src )  {
        Set<String> attrs = new TreeSet<String>();
        for( ParseNode descendant: root.descendants() )
            if( descendant.contains(attribute) )
                attrs.add(descendant.content(src));
        Relation ret = new Relation(attrs.toArray(new String[0]));

        addTuples(ret,root, src);            
        return ret;
    }
    private void addTuples( Relation ret, ParseNode root, List<LexerToken> src )  {
        if( root.contains(tuple) ) 
            ret.addTuple(tuple(root, src));
        else for( ParseNode child : root.children() )
            if( child.contains(tuple) )
                ret.addTuple(tuple(child, src));
            else if( child.contains(tuples) )
                addTuples(ret,child, src);
    }
    private Map<String,Object> tuple( ParseNode root, List<LexerToken> src )  {
        for( ParseNode child : root.children() ) {
            if( child.contains(values) ) {
                Map<String,Object> tuple = new TreeMap<String,Object>(); 
                values(tuple, child, src);
                return tuple;
            }
        }
        throw new AssertionError("Unknown case");
    }*/

    /*private void values( Map<String,Object> tuple, ParseNode root, List<LexerToken> src ) {
        if( root.contains(namedValue) )
            value(tuple,root, src);
        else for( ParseNode child : root.children() )
            if( child.contains(namedValue) )
                value(tuple,child, src);
            else if( child.contains(values) )
                values(tuple,child, src);
    }
    private void value( Map<String,Object> tuple, ParseNode root, List<LexerToken> src ) {
        String left = null;
        Object right = null;
        for( ParseNode child : root.children() ) {
            if( left == null )
                left = child.content(src);
            else if( child.contains(equality) )
                ;
            else { 
                String data = child.content(src);
                try {
                    right = Integer.parseInt(data);
                } catch( NumberFormatException e ) {
                    right = data;
                }
            }
        }
        tuple.put(left, right);
    }*/

    private List<String> strings( ParseNode root, List<LexerToken> src )  {
        List<String> ret = new LinkedList<String>();
        if( root.from + 1 == root.to && src.get(root.from).type == Token.IDENTIFIER )
            ret.add(root.content(src));
        else if( root.from + 1 == root.to && src.get(root.from).type == Token.DIGITS )
            throw new AssertionError("Got number while expected string");
        else
            for( ParseNode child : root.children() )
                ret.addAll(strings(child,src));
        return ret;
    }   
    private List<Object> values( ParseNode root, List<LexerToken> src ) {
        List<Object> ret = new LinkedList<Object>();
        if( root.contains(parExpr) )
            ret.add(parExpr(root, src));
        else if( root.from + 1 == root.to && src.get(root.from).type == Token.IDENTIFIER ) 
            ret.add(root.content(src));
        else if( root.from + 1 == root.to && src.get(root.from).type == Token.DIGITS  
            ||   root.from + 2 == root.to && "-".equals(src.get(root.from).content) && src.get(root.from+1).type == Token.DIGITS )
            ret.add(Integer.parseInt(root.content(src)));
        else if( root.from + 3 == root.to  
                && src.get(root.from).type == Token.DIGITS  
                && ".".equals(src.get(root.from+1).content) 
                && src.get(root.from+2).type == Token.DIGITS
        )
            ret.add(Double.parseDouble(root.content(src)));
        else if( root.from + 1 == root.to && src.get(root.from).type == Token.DQUOTED_STRING ) {
            String strValue = root.content(src).substring(1,root.content(src).length()-1);
            Object value = strValue;
            try {
                Integer i = Integer.parseInt(strValue);
                if( i != null )
                    value = i;
            } catch( Exception e ) {}
            try {
                Double d = Double.parseDouble(strValue);
                if( d != null )
                    value = d;
            } catch( Exception e ) {}
            ret.add(value);
        } else
            for( ParseNode child : root.children() )
                ret.addAll(values(child,src));
        return ret;
    }   
    private void addContent( Relation ret, ParseNode root, List<LexerToken> src ) {
        int i = 0;
        Object[] t = new Object[ret.colNames.length];
        for( Object elem : values(root, src) ) {
            t[i%ret.colNames.length] = elem;
            if( i%ret.colNames.length == ret.colNames.length-1 ) {
                ret.addTuple(t);
                t = new Object[ret.colNames.length];
            }
            i++;
        }
    }


    static final String FALSE_ASSERTION = "*** False Assertion ***";
    static final String PARSE_ERROR_IN_ASSERTIONS_FILE = "*** Parse Error in assertions file ***";
    public void run( String prg ) {
        List<LexerToken> src =  new Lex().parse(prg);
        
        Visual visual = null;
        if( visualize )
        	visual = new Visual(src, earley);
        //LexerToken.print(src);
        
        Matrix matrix = new Matrix(earley);
        if( visual != null )
        	matrix.visual = visual;
        earley.parse(src, matrix); 
        if( visual != null )
        	visual.draw(matrix);
        SyntaxError err = SyntaxError.checkSyntax(prg, new String[]{"program"}, src, earley, matrix);      
        if( err != null ) {
            System.out.println(err.toString());
            throw new AssertionError(PARSE_ERROR_IN_ASSERTIONS_FILE);
        }

        ParseNode root = earley.forest(src, matrix);

        long t1 = System.currentTimeMillis();
        ParseNode exception = program(root,src);
        long t2 = System.currentTimeMillis();
        System.out.println("Time = "+(t2-t1)); 
        if( exception != null ) {
            System.out.println(FALSE_ASSERTION);
            System.out.println(prg.substring(src.get(exception.from).begin, src.get(exception.to-1).end));
            throw new AssertionError(FALSE_ASSERTION);
        }
    }

}
