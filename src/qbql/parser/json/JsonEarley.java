package qbql.parser.json;

import qbql.parser.*;
import qbql.util.Util;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


public class JsonEarley {
	static Earley instance = null;

	static public Earley jsonParser() throws IOException {
	    if (instance != null) return instance;
	    Set<RuleTuple> rules = new TreeSet<RuleTuple>();
	    String input  = Util.readFile(JsonEarley.class, "json.grammar"); //$NON-NLS-1$
	    List<LexerToken> src = new Lex().parse(input, false);
	    //LexerToken.print(src);
	    ParseNode root = Grammar.parseGrammarFile(src, input);
	    //root.printTree();
	    Grammar.grammar(root, src, rules);
	    //RuleTransforms.eliminateEmptyProductions(rules);
	    instance = new Earley(rules);
	    //instance.isCaseSensitive = true;
	    return instance;
	}
	
	public static void main(String[] args) throws Exception {
	    //LexerToken.switchLineCommentSymbol("//");

	    String input = 
	        /* english history from https://github.com/jdorfman/awesome-json-datasets */
	    		//Util.readFile("C:\\Users\\dim\\Documents\\JSON/english history.json")
	    		//Util.readFile("C:\\Users\\dim\\Documents\\JSON/english history fragment.json")
	    		Util.readFile("C:\\Users\\dim\\Documents\\JSON/slow1.txt")
	    ;

	    //Program.debug = true
	    long t1 = System.nanoTime();
	    List<LexerToken> src = new Lex().parse(input);
	    long t2 = System.nanoTime();
	    //LexerToken.print(src);
	    System.out.println("Lex time = " + (t2 - t1)/1000000);  //$NON-NLS-1$

	    Earley earley = JsonEarley.jsonParser();
	    //Visual visual = new Visual(src,earley);
	    Matrix matrix = new Matrix(earley);
	    long t21 = System.nanoTime();
	    earley.parse(src, matrix);
	    //visual.draw(matrix);
	    long t22 = System.nanoTime();
	    System.out.println("parse time = " + (t22 - t21)/1000000);  //$NON-NLS-1$

	    t1 = System.nanoTime();
	    // the final parsing stage: building parse tree
	    ParseNode root = earley.forest(src, matrix);
	    if( src.size() < 1000 )
	        root.printTree();

	    t2 = System.nanoTime();
	    System.out.println("Reduction time = " + (t2 - t1)/1000000);  //$NON-NLS-1$	    


	}
}
