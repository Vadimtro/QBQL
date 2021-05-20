package qbql.parser;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import qbql.util.Util;

public class LexerToken {
    public String content;
    public int begin;
    public int end;
    public Token type;

    public LexerToken( CharSequence text, int from, int to, Token t ) {
        content = text.toString();
        begin = from;
        end = to;
        type = t;
    }

    public void print() {
        System.out.println(toString()); 
    }
    public String toString() {
        return "["+begin+","+end+") "+content+"   <"+type+">"; 
    }
    public static void print( List<LexerToken> src ) {
        int j = 0;
        for( LexerToken t: src ) {
            System.out.print(j+"    "); 
            t.print(); 
            j++;
        }
    }
    public static void print( List<LexerToken> src, int from, int to ) {		
        System.out.println(toString(src, from, to)); 
    }
    public static String toString( List<LexerToken> src, int from, int to ) {
        StringBuilder ret = new StringBuilder();
        for( int i = from; i < to; i++ ) {
            ret.append(" "+src.get(i).content);
        }
        return ret.toString(); 
    }
    
    /**
     * Helper method to render abbreviated content of parsed text covered by ParseNode
     * @param from  -- beginning of node interval
     * @param to -- end of node interval
     * @param src  -- lexed text
     * @return
     */
    public static String mnemonics( int from, int to, List<LexerToken> src ) {
        int _8 = 8;
        if( from + 1 == to)
            return Util.padln(_8+2<src.get(from).content.length() ? src.get(from).content.substring(0,_8+2) : src.get(from).content, _8+2);
        else {  // Node containing many tokens
                // Get _8 word's first letters with capitalized keywords 
                // e.g. "emp=10 and dept=emp" -> "e=1Ad=e"
            StringBuilder ret = new StringBuilder("\"");
            for( int i = from; i < to && i < from+_8 ; i++ ) {               
                String token = src.get(i).content.toUpperCase();
                String t = token.substring(0,1).toLowerCase();
                ret.append(t);
            }
            ret.append('\"');
            return Util.padln(ret.toString(),_8+2);
        }
    }

}
