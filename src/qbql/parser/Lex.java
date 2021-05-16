package qbql.parser;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import qbql.util.Util;

public class Lex {

    public boolean isPercentLineComment = false; // Prover9 comments
    public boolean isQuotedString = false; // Prover9 unary '
    public boolean keepWSandCOMMENTS = false;
    Map<String, String> specialSymbols = new HashMap<String, String>();
    
    public Lex( boolean isPercentLineComment, 
                boolean isQuotedString, 
                boolean keepWSandCOMMENTS,
                Map<String, String> specialSymbols
    ) {
        this.isPercentLineComment = isPercentLineComment;
        this.isQuotedString = isQuotedString;
        this.keepWSandCOMMENTS = keepWSandCOMMENTS;
        this.specialSymbols = specialSymbols;
    }
    public Lex() {
    }
    
    public LinkedList<LexerToken> tokenize( String sourceExpr ) {

        LinkedList<LexerToken> ret = new LinkedList<LexerToken>();
        final String operation = "(){}[]^-~&|!?*+.\\/><='`\",;:#@";
        final String ws = " \n\r\t";
        StringTokenizer st = new StringTokenizer(sourceExpr,
                                                 operation + ws
                                                 ,true);
        int pos = 0;
        while( st.hasMoreTokens() ) {
            String token = st.nextToken();
            pos += token.length();
            // comments
            LexerToken last = null;
            if( ret.size() > 0 )
                last = ret.get(ret.size()-1);
            if( last != null && last.type == Token.COMMENT && (!last.content.endsWith("*/") || last.content.equals("/*/")) ) {
                if( "*".equals(token) || "/".equals(token) )
                    last.content = last.content + token;
                else
                    last.content = "/* ... "; // Speeds up really long comments.
                last.end = pos;
                // Fix the comment
                if( last != null && last.type == Token.COMMENT && last.content.endsWith("*/") && !last.content.equals("/*/") ) { 
                    last.content = sourceExpr.substring(last.begin,last.end);
                }
                continue;
            }
            if( last != null && last.type == Token.COMMENT && last.content.endsWith("*/") && !last.content.equals("/*/") ) {
                last.end = pos-token.length();
            }
            if( last != null && last.type == Token.LINE_COMMENT && !"\n".equals(token) ) {
                last.content = last.content + token;
                continue;
            }
            if( last != null && last.type == Token.LINE_COMMENT && "\n".equals(token) ) {
                last.end = pos-token.length();
                last.end = last.begin + last.content.length();
            }
            if( last != null && last.type == Token.CDATA && !"]".equals(token) ) {
                last.content = last.content + token;
                continue;
            }
            if( last != null && last.type == Token.CDATA && "]".equals(token) ) {
                last.end = pos-token.length();
                last.end = last.begin + last.content.length();
                token = " ";
            }
            if( isQuotedString && last != null && last.type == Token.QUOTED_STRING && !"'".equals(token)
                    && !(last.content.endsWith("'")&&last.content.length()>1)) {
                //last.content = last.content + token;
                //last.end = last.begin + last.content.length();
                continue;
            }
            if( isQuotedString && last != null && last.type == Token.QUOTED_STRING && "'".equals(token) 
                    && !(last.content.endsWith("'")&&last.content.length()>1)) {
                last.end = pos;
                last.content = sourceExpr.substring(last.begin,last.end);
                continue;
            }
            if( last != null && last.type == Token.DQUOTED_STRING && !"\"".equals(token) 
                    && !(last.content.endsWith("\"")&&last.content.length()>1)) {
                //last.content = last.content + token;
                //last.end = last.begin + last.content.length();
                continue;
            }
            if( last != null && last.type == Token.DQUOTED_STRING && "\"".equals(token) ) {
                last.end = pos;
                last.content = sourceExpr.substring(last.begin,last.end);
                continue;
            }

            if( "*".equals(token) && last != null && "/".equals(last.content) ) {
                last.content = last.content + token;
                last.end = last.begin + last.content.length();
                last.type = Token.COMMENT;
                continue;
            }
            if( "-".equals(token) && last != null && "-".equals(last.content) ) {
                last.content = last.content + token;
                last.type = Token.LINE_COMMENT;
                continue;
            }
            if( "[".equals(token) && last != null && "cdata".equalsIgnoreCase(last.content) ) {
                last.content = "";
                last.type = Token.CDATA;
                continue;
            }
            if( isPercentLineComment && "%".equals(token) ) {
                ret.add(new LexerToken(token, pos-1, -10, Token.LINE_COMMENT));
                continue;
            }
            if( isQuotedString && "'".equals(token) ) {  // start
                ret.add(new LexerToken(token, pos-1, -10, Token.QUOTED_STRING));
                continue;
            }
            if( "\"".equals(token) ) {
                ret.add(new LexerToken(token, pos-1, -11, Token.DQUOTED_STRING));
                continue;
            }
            if( operation.contains(token) ) {
                ret.add(new LexerToken(token, pos-1, pos, Token.OPERATION));
                continue;
            }
            if( ws.contains(token) ) {
                ret.add(new LexerToken(token, pos-1, pos, Token.WS));
                continue;
            }
            if( '0'<=token.charAt(0) && token.charAt(0)<='9' ) {
                // FIXME: "1e01" is treated as "digits", "1e+01" is treated as "digits '+' digits" 
                // This seems to be a minor bug -- the containing expressions are OK  
                ret.add(new LexerToken(token, pos-token.length(), pos, Token.DIGITS));
                continue;
            }
            ret.add(new LexerToken(token, pos-token.length(), pos, Token.IDENTIFIER));      

        }

        return ret;
    }
    public static void main(String[] args) throws Exception {
        String input = 
            //"a \"b\n"+ 
            //"'select ''Y'' from T '   ''''\n" +
            //" ' multiline \n literal' 123 a1$ x# \n" +
            //" /* 3s5d7 \n asdsa multiline comment*/ \n" +
            //"a 3a --a3 ss 5<7  \n" +
            //"  3.5 /* ** / -- ' ' \" \" */\n" +
            //"'/* */'\"/*--*/\"   abc\n" +
            //"((x /< \"[p<r]\") /^ [p=r]) ^ x; " +
            //"'Cannot find the key_id for key: \"'||p_key_name||'\" for table \"'||"+
            "aaa '['"
        	//Util.readFile("C:/Documents and Settings/Dim/Desktop/movies.list")
        	;

        List<LexerToken> out = new Lex().parse(input,true);
        
        int i = 0;
        for( LexerToken t : out) {
        	if( i > 1000 )
        		break;
        	System.out.println(t.content);
        }
        //System.out.print(out); // (authorized)
    }

    public LinkedList<LexerToken> parse( String input ) {
        return parse(input, false);
    }
    public LinkedList<LexerToken> parse( String input, boolean keepWSandCOMMENTS ) {
        //return parse((CharSequence)input);
        LinkedList<LexerToken> ret = new LinkedList<LexerToken>();
        LexerToken last = null;
        for( LexerToken token : tokenize(input) ) {
            if( token.type == Token.QUOTED_STRING ) {   // glue strings together
                if( last != null && last.type == Token.QUOTED_STRING ) {
                    last.content = last.content + token.content;
                    last.end = token.end;
                    continue;
                }
            }
            for( String key : specialSymbols.keySet() )
                token.content = token.content.replace(key, specialSymbols.get(key));
            if( keepWSandCOMMENTS || token.type != Token.WS && token.type != Token.COMMENT && token.type != Token.LINE_COMMENT )
                ret.add(token);
            last = token;
        }
        return ret;
    }

    public static int scanner2parserOffset( List<LexerToken> src, int start ) {
        int offset = -1;
        for( LexerToken t : src ) {
            offset++;
            if( t.end > start ) 
                break;
        }
        return offset;
    }
}
