package qbql.parser.json;

import java.io.IOException;

import qbql.arbori.Program;
import qbql.parser.Lex;
import qbql.parser.LexerToken;
import qbql.parser.Parsed;
import qbql.util.Util;


public class Interpreter  {
	
	public boolean queryEvents = false;

	public NamedValue ret  = null;

	Program program = null;

	public NamedValue eval( String input ) throws IOException  {
		//LexerToken.Companion.switchLineCommentSymbol("//")
		Parsed target = new Parsed(
				input,
				new Lex().parse(input),
				JsonEarley.jsonParser(),
				"top" //$NON-NLS-1$
				);
		if( program != null ) {
			program.eval(target);
			return ret;
		}

		//LexerToken.Companion.switchLineCommentSymbol("--")
		program = new Program(JsonEarley.jsonParser(), Interpreter.this) {
			//public override fun getBoolBindVar(name: String): Boolean {
			//    return true
			//}
		};
		if( target.getSyntaxError() != null )
			System.err.println(target.getSyntaxError());
		String prg = Util.readFile(Interpreter.class,"interpreter.arbori");
		program.compile(prg);
		program.eval(target);
		return ret;
	}

	/*override fun getIntBind(name: String): Int {
	        TODO("Not implemented")
	    }

	    override fun getBoolBind(name: String): Boolean {
	        TODO("Not implemented")
	    }

	    override val namedCallbacks: Map<String, (Parsed, Map<String, ParseNode>) -> Unit>
	        get() = TODO("Not implemented")
	 */    


	public static void main(String[] args) throws Exception {
		//LexerToken.switchLineCommentSymbol("//");

		String file = "call.json";
		//file = "flights.json"
		String input = 
				/* english history from https://github.com/jdorfman/awesome-json-datasets */
				//Service.readFile("C:\\Users\\dim\\Documents\\JSON/english history.json")
				Util.readFile("C:\\Users\\dim\\Documents\\JSON/init.json")
				;

		new Interpreter().eval(input);

	}
}
