package qbql.parser;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import qbql.arbori.MaterializedPredicate;
import qbql.arbori.Program;
import qbql.parser.SyntaxError;
import qbql.util.Util;

/**
 * A helper class. Parser boilerplate code, i.e.
 * 
        String input = "select emp from empno;";
        List<LexerToken> src =  Lexer.parse(input);
        SqlEarley earley = SqlEarley.partialRecognizer();
        Matrix matrix = new Matrix(earley);
        earley.parse(src, matrix); 
        ParseNode root = earley.forest(src, matrix);
 * 
 * is wrapped into
 *
        Parsed parsed = new Parsed(
            "select emp from empno;",
            SqlEarley.getInstance(),
            "sql_statement"
        );
        ParseNode root = parsed.getRoot();
 * 
 * @author Dim
 *
 */
public class Parsed {
    public String input;
    protected List<LexerToken> src;
    protected ParseNode root;
    protected Earley earley;
    private String[] rootPayload = null;
    private SyntaxError err = null;
    
    //public Substitutions replacements = null;
    
    //public boolean debug = true;
    public boolean debug = false;
    
    public Parsed( String input, Earley parser, String legitimateSymbol ) {
        this.input = input;
        this.earley = parser;
        if( legitimateSymbol != null )
            this.rootPayload = new String[]{legitimateSymbol};
    }
    public Parsed( String input, List<LexerToken> src, Earley parser, String legitimateSymbol ) {
        this.input = input;
        this.earley = parser;
		this.src = src;
		if( legitimateSymbol != null )
		    this.rootPayload = new String[]{legitimateSymbol};
    }
    public Parsed( String input, List<LexerToken> src, Earley parser, String[] rootPayload ) {
        this.input = input;
        this.earley = parser;
        this.src = src;
        this.rootPayload = rootPayload;
    }
    public Parsed( String input, List<LexerToken> src, ParseNode root ) {
		this.input = input;
		this.src = src;
		this.root = root;
	}
	public String getInput() {
        return input;
    }
    public synchronized List<LexerToken> getSrc() {
        if( src == null )
            src =  new Lex().parse(input);
        return src;
    }
    protected Matrix matrix = null;
    public Matrix getMatrix() {
        getRoot();
        return matrix;
    }    
    public synchronized ParseNode getRoot() {
    	getSrc();
        if( root == null ) {
            //LexerToken.print(src);
            matrix = new Matrix(earley);
            if( debug ) {
            	boolean isCmdline = false;
                for( StackTraceElement elem : Thread.currentThread().getStackTrace() ) {
                	if( elem.toString().startsWith("qbql.cmdline") ) {
                		isCmdline = true;
                		break;
                	}
                }
                if( !isCmdline ) {
                	matrix.visual = new Visual(src, earley);
                	matrix.visual.matrix = matrix;
                }
            }
            earley.parse(src, matrix); 
            if( debug && matrix.visual != null )
            	matrix.visual.draw();            
            
            root = earley.forest(src, matrix, true, input);
            
            if( root.topLevel != null )
            	err = SyntaxError.checkSyntax(input, rootPayload, src, earley, matrix);

        }
        return root;
    }
    
    public SyntaxError getSyntaxError() {
    	if( err != null )
    		return err;
        getRoot();
        return err;
    }

    
    /* TODO: fix this arbori example
      public static void main(String[] args) throws Exception {
     
 		final Parsed xmlGrammar = new Parsed(
        		Service.readFile(Parsed.class, "xml.grammar"), //$NON-NLS-1$
        		Grammar.bnfParser(),
        		"grammar" //$NON-NLS-1$
        );
        //xmlGrammar.getRoot().printTree();    
        Set<RuleTuple> rules = new TreeSet<RuleTuple>();
        Grammar.grammar(xmlGrammar.getRoot(), xmlGrammar.getSrc(), rules);

        String input = Service.readFile(Parsed.class, "test.html");
        Earley htmlparser = new Earley(rules) {
			@Override
			protected boolean isIdentifier( int y, List<LexerToken> src, int symbol, Integer suspect ) {
				LexerToken token = src.get(y);
				return 
				symbol == identifier && token.type == Token.IDENTIFIER 
				||  symbol == identifier && token.type == Token.DQUOTED_STRING
				;
			}        			
		};
		final Parsed target = new Parsed(
        		input, 
        		htmlparser,
        		"nodes" //$NON-NLS-1$
        );
        //target.getRoot().printTree();   
        
 		final Parsed prog = new Parsed(
        		Service.readFile(Parsed.class, "htmltable.prg"), //$NON-NLS-1$
        		Program.getArboriParser(),
        		"program" //$NON-NLS-1$
        );
        if( false )
        	prog.getRoot().printTree();
        
        Program r = new Program(htmlparser) {
        	// define bind values here...
        };
        //r.debug = true;
        r.program(prog.getRoot(), prog.getSrc(), prog.getInput());

        target.getRoot(); // to get better idea of timing
        long t1 = System.currentTimeMillis();
		//Service.profile(500, 10, 5);
        Map<String,MaterializedPredicate> output = r.eval(target);
		for( String p : output.keySet() )
			System.out.println(p+"="+output.get(p).toString(p.length()+1));
        System.out.println("\n *********** eval time ="+(System.currentTimeMillis()-t1)+"\n");        

	}*/
}
