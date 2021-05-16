package qbql.bool;

import java.util.List;

import qbql.parser.Lex;
import qbql.parser.LexerToken;
import qbql.parser.ParseNode;
import qbql.parser.Token;
import qbql.util.Util;

/**
 * Mace4-inspired tool optimized and specialized for Boolean Algebra embeddings (e.g. BA with closure)
 * There is only one model of size 2^n (up to isomorphism) which cuts down model checking time significantly
 * One has to shuffle elements in the additional operators only 
 * @author Dim
 */
public class Model {
    
    public static void main( String[] args ) throws Exception {
        String input = null;
        //Profile.sample(10000, 30);
        if( args.length == 1 ) {
            input = Util.readFile(args[0]);
        } else {
            input = Util.readFile(Model.class,"lattice.embeding");     
        }
                
        List<LexerToken> src =  new Lex().parse(input);
        ParseNode root = Program.parse(input, src);
        
        ParseNode left = null;
        boolean sawEq = false;
        boolean sawGt = false;
        ParseNode right = null;
        for( ParseNode child : root.children() ) {
            if( left == null ) {
                left = child;
            } else if( !sawEq ) {
                sawEq = true;
            } else if( !sawGt ) {
                sawGt = true;
            } else if( right == null ) {
                right = child;
            } else 
                throw new AssertionError("Unexpected");            
        }
        
        StringBuilder preprocessed = new StringBuilder();
        preprocessed.append("!");
        for( LexerToken t : src ) {
            if( t.begin < src.get(right.from).begin )
                continue;
            //preprocessed.append(" ");
            if( t.type == Token.IDENTIFIER && t.content.charAt(0)!='"' )
                preprocessed.append('"'+t.content+'"');
            else
                preprocessed.append(t.content);
        }
        System.out.println(preprocessed.toString());

        input = input.substring(0, src.get(left.to).begin)+preprocessed.toString();
        src =  new Lex().parse(input);
        root = Program.parse(input, src);
        
        String[] variables = Program.variables(root, src);
        String[] constants = Program.constants(root, src);
        
        for( int dim = 1; dim < 10; dim++ ) {
            long t1 = System.currentTimeMillis();
            long size = 1L << dim;
            UnaryOperator oper = new UnaryOperator(dim);
            int count = 0;
            do {
                Program prg = new Program(oper,variables,constants);
                int[] model = prg.eval(root, src);
                if( model != null ) {
                    System.out.println(oper.toString());
                    for( String var : prg.assignments.keySet() ) {
                        System.out.print(var+"=");
                        long vector = prg.assignments.get(var);
                        System.out.println(Oper.toString(vector, dim));
                    }
                    return;
                }
                count++;
                oper = oper.next();
            } while( oper != null );
            System.out.print("models("+size+")="+count);
            System.out.println("      ("+(System.currentTimeMillis()-t1)+" ms)");
        }
    }
    
}
