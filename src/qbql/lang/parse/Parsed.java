package qbql.lang.parse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import qbql.parser.Earley;
import qbql.parser.Grammar;
import qbql.parser.Lex;
import qbql.parser.LexerToken;
import qbql.parser.Matrix;
import qbql.parser.ParseNode;
import qbql.parser.RuleTuple;
import qbql.parser.SyntaxError;
import qbql.parser.Visual;
import qbql.util.Util;

/**
 * A helper class. Parser boilerplate code, i.e.
 * 
        String input = "select emp from empno;";
        List<LexerToken> src =  LexerToken.parse(input);
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
    private final String input;
    private List<LexerToken> src;
    private ParseNode root;
    private Earley earley;
    private String rootSyntax;
    private SyntaxError err = null;
    
    //public boolean debug = true;
    public boolean debug = false;
    
    public Parsed( String input, Earley parser, String rootSyntax ) {
        this.input = input;
        this.earley = parser;
        this.rootSyntax = rootSyntax;
    }
    public Parsed( String input, List<LexerToken> src, Earley parser, String rootSyntax ) {
        this.input = input;
        this.earley = parser;
		this.src = src;
        this.rootSyntax = rootSyntax;
    }
    public Parsed( String input, List<LexerToken> src, ParseNode root ) {
		this.input = input;
		this.src = src;
		this.root = root;
	}
	public String getInput() {
        return input;
    }
	boolean isPercentLineComment = false;
    boolean isQuotedString = true;	
    public List<LexerToken> getSrc() {
        if( src == null ) {
            src =  new Lex(isPercentLineComment,isQuotedString,false,new HashMap()).parse(input);
        }
        return src;
    }
    public ParseNode getRoot() {
    	getSrc();
        if( root == null ) {
            //LexerToken.print(src);
            Visual visual = null;
            if( debug )
                visual = new Visual(src, earley);
            Matrix matrix = new Matrix(earley);
            earley.parse(src, matrix); 
            if( visual != null )
                visual.draw(matrix);
            //Cell top = matrix.get(0, src.size());
            //if( top == null ) // if input is syntactically recognized 
                //System.out.println("***** Syntactically Invalid code fragment *****"); //$NON-NLS-1$
            if( rootSyntax != null ) {
                err = SyntaxError.checkSyntax(input, new String[]{rootSyntax}, src, earley, matrix);
                if( err != null ) { //$NON-NLS-1$ //$NON-NLS-2$
                    throw err; //$NON-NLS-1$
                }
            }
            
            root = earley.forest(src, matrix);
        }
        return root;
    }
    
    public SyntaxError getSyntaxError() {
    	if( err != null )
    		return err;
        getRoot();
        return err;
    }

    
    public static void main(String[] args) throws Exception {
 		final Parsed xmlGrammar = new Parsed(
        		Util.readFile(Parsed.class, "xml.grammar"), //$NON-NLS-1$
        		Grammar.bnfParser(),
        		"grammar" //$NON-NLS-1$
        );
        //xmlGrammar.getRoot().printTree();    
        Set<RuleTuple> rules = new TreeSet<RuleTuple>();
        Grammar.grammar(xmlGrammar.getRoot(), xmlGrammar.getSrc(), rules);

        String input = Util.readFile(Parsed.class, "test.html");
        Earley htmlparser = new Earley(rules);
		final Parsed target = new Parsed(
        		input, 
        		htmlparser,
        		"nodes" //$NON-NLS-1$
        );
        //target.getRoot().printTree();   
        
 		final Parsed prog = new Parsed(
        		Util.readFile(Parsed.class, "htmltable.prg"), //$NON-NLS-1$
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

	}
}
