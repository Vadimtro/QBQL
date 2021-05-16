package qbql.gui;

import java.util.List;
import java.util.TreeMap;

import javax.swing.JFrame;

import qbql.lattice.Database;
import qbql.lattice.Program;
import qbql.parser.Earley;
import qbql.parser.Lex;
import qbql.parser.LexerToken;
import qbql.parser.Matrix;
import qbql.parser.ParseNode;
import qbql.parser.SyntaxError;
import qbql.util.Util;


public class Graphics2D extends Database {
    public static void main( String[] args ) throws Exception {
        final String prg = Util.readFile(Graphics2D.class,"graphics.prg");
        run(prg);
        
        JFrame f = new JFrame("Graphics Database");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
        f.setVisible(true);

    }
    
    public Graphics2D( String pkg ) {
        super(pkg);
    }
    
	static final String PARSE_ERROR_IN_ASSERTIONS_FILE = "*** Parse Error in assertions file ***";
    public static Graphics2D run( String prg ) throws Exception {
        
        StackTraceElement[] stack = new Throwable().getStackTrace();
        String createdInClass = stack[0].getClassName();
        String pkg = createdInClass.substring(0,createdInClass.lastIndexOf('.'));
        Graphics2D db = new Graphics2D(pkg);
        
        // program
        List<LexerToken> src =  new Lex().parse(prg);
        Earley earley = new Earley(Gui.guiRules());
        Matrix matrix = new Matrix(earley);
        earley.parse(src, matrix); 
        SyntaxError err = SyntaxError.checkSyntax(prg, new String[]{"program"}, src, earley, matrix);      
        if( err != null ) {
            System.out.println(err.toString());
            throw new AssertionError(PARSE_ERROR_IN_ASSERTIONS_FILE);
        }
        ParseNode root = earley.forest(src, matrix);

        if( root.topLevel != null ) {
            System.out.println("*** Parse Error in assertions file ***");
            //CYK.printErrors(prg, src, root);
            return null;
        }
        System.out.println("-------------------------------------");

        Program program = new Program(db); 
        ParseNode exception = program.program(root,src);
        if( exception != null ) {
            System.out.println("*** False Assertion ***");
            System.out.println(prg.substring(src.get(exception.from).begin, src.get(exception.to-1).end));
        }
        
        //Relation result = (Relation)db.predicate("Result");
        return db;
    }

}
