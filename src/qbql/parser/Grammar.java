package qbql.parser;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import qbql.parser.Parser.Tuple;

public class Grammar {
    private static Earley parser = bnfParser();
    public static Earley bnfParser() {
        Set<RuleTuple> rules = new TreeSet<RuleTuple>();
        rules.add(new RuleTuple("variable", new String[] {"identifier"}));                            //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        //rules.add(new RuleTuple("variable", new String[] {"identifier","'['","identifier","']'"}));                            //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        rules.add(new RuleTuple("variable", new String[] {"string_literal"}));                             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        rules.add(new RuleTuple("concat", new String[] {"variable"}));                             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        rules.add(new RuleTuple("concat", new String[] {"concat","variable"}));                             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        rules.add(new RuleTuple("disjunct", new String[] {"concat"}));                             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        rules.add(new RuleTuple("disjunct", new String[] {"'|'","concat"}));                             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        rules.add(new RuleTuple("disjunct", new String[] {"disjunct","'|'","concat"}));                             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        rules.add(new RuleTuple("disjunct", new String[] {"disjunct","'|'"}));                             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        rules.add(new RuleTuple("rule", new String[] {"variable","':'","disjunct","';'"}));                             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        rules.add(new RuleTuple("rule", new String[] {"variable","':'","';'"}));                             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        rules.add(new RuleTuple("rule", new String[] {"'-'","variable","';'"}));                             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        rules.add(new RuleTuple("grammar", new String[] {"rule"}));                             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        rules.add(new RuleTuple("grammar", new String[] {"grammar", "rule"}));                             //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        Set<RuleTuple> nonEmptyRules = rules; //RuleTransforms.eliminateEmptyProductions(rules); <--automatically transformed output is messy
        //RuleTuple.printRules(nonEmptyRules);
		parser = new Earley(nonEmptyRules) {
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
        rule = parser.symbolIndexes.get("rule");
        grammar = parser.symbolIndexes.get("grammar");
        variable = parser.symbolIndexes.get("variable");
        disjunct = parser.symbolIndexes.get("disjunct");
        concat = parser.symbolIndexes.get("concat");
        or = parser.symbolIndexes.get("'|'");
        minus = parser.symbolIndexes.get("'-'");
        return parser;
    }
    static int rule;
    static int grammar;
    static int variable;
    static int disjunct;
    static int concat;
    static int or;
    static int minus;

    public static void main( String[] args ) throws Exception {
    	String input = "x: a b c | A | B;";
        List<LexerToken> src = (new Lex()).parse(input); 
        ParseNode root = Grammar.parseGrammarFile(src, input);
        root.printTree();
        List<RuleTuple> rules = Grammar.grammar(root, src);
        RuleTuple.printRules(rules);
	}
    
    public static ParseNode parseGrammarFile( List<LexerToken> src, String input ) {
        Visual visual = null;
        //visual = new Visual(src, parser);
        //LexerToken.print(src);
        
        Matrix matrix = new Matrix(parser);
        if( visual != null )
        	matrix.visual = visual;
        parser.parse(src, matrix); 
        if( visual != null )
        	visual.draw(matrix);
        SyntaxError err = SyntaxError.checkSyntax(input, new String[]{"grammar"}, src, parser, matrix);      
        if( err != null ) {
            System.out.println(err.toString());
            if( visual != null )
                visual.draw(matrix);
            throw new AssertionError("*** Parse Error in assertions file ***");
        }
        ParseNode root = parser.forest(src, matrix);
        //root.printTree();
        return root;
    }
    
    public static void grammar( ParseNode root, List<LexerToken> src, List<RuleTuple> grammar ) {
        if( root.contains(rule) ) 
            rule(root, src, grammar); 
        else for( ParseNode child: root.children() ) 
            grammar(child, src, grammar);            
    }
    public static List<RuleTuple> grammar( ParseNode root, List<LexerToken> src ) {
    	List<RuleTuple> rules = new LinkedList<RuleTuple>();
        grammar(root, src, rules);
        return rules;
    }

	private static void rule( ParseNode node, List<LexerToken> src, List<RuleTuple> grammar ) {
        String header = null;
        Set<RuleTuple> disj = null;
        for( ParseNode child: node.children() ) {
            if( header==null && child.contains(minus) ) {
                delete(node, src, grammar);
                return;
            }
                
            if( header==null && child.contains(variable) ) {
                header = child.content(src); 
            } else if( child.contains(disjunct) ) {
                disj = disjunct(header, child, src);
				grammar.addAll(disj);
            } 
        }
        if( disj == null )
        	grammar.add(new RuleTuple(header, new String[]{}));
    }

    private static void delete( ParseNode node, List<LexerToken> src, List<RuleTuple> grammar ) {
        String var = null;
        for( ParseNode child: node.children() ) 
            if( var==null && child.contains(variable) ) {
                var = child.content(src);
                break;
            }
        Set<RuleTuple> deletions = new TreeSet<RuleTuple>();
        for( RuleTuple t : grammar )
            if( t.head.equals(var) ) 
                deletions.add(t);
            else for( String rhs : t.rhs )
                if( rhs.equals(var) ) {
                    deletions.add(t);
                    break;
                }
        
        grammar.removeAll(deletions);        
    }

    private static Set<RuleTuple> disjunct( String header, ParseNode node, List<LexerToken> src) {
        Set<RuleTuple> ret =  new TreeSet<RuleTuple>();
        if( node.contains(concat) || node.contains(disjunct) && node.from+1==node.to ) 
            ret.add(concat(header, node, src));
        else {
            int cnt = -1;
            int lastOrPos = -1;
            for( ParseNode child: node.children() )  {
                cnt++;
                if( child.contains(disjunct) ) 
                    ret.addAll(disjunct(header, child, src));
                else if( child.contains(concat) ) 
                    ret.add(concat(header, child, src));
                if( lastOrPos+1 == cnt && child.contains(or))
                    ret.add(new RuleTuple(header, new String[]{}));
                if( child.contains(or) ) {
                    lastOrPos = cnt;
                    if( cnt == node.children().size()-1 )
                    	 ret.add(new RuleTuple(header, new String[]{}));
                }
            }
        }
        return ret;
    }

    private static RuleTuple concat( String header, ParseNode node, List<LexerToken> src ) {
        List<String> payload =  new LinkedList<String>();
        for( int i = node.from; i < node.to; i++ )
            payload.add(src.get(i).content);
        return new RuleTuple(header,payload);
    }

}
